<template>
  <PageLayout title="缴费记录管理">
    <template #actions>
      <el-button v-if="editAllowed" type="success" @click="openAddDialog">新增缴费</el-button>
    </template>

    <template #search>
      <!-- 多条件组合查询：车牌号、停车场、日期、支付状态 -->
      <el-input v-model="searchPlate" placeholder="请输入车牌号" clearable
        style="width: 160px" @keyup.enter="loadData" @clear="loadData" />
      <el-select v-model="searchLotId" placeholder="选择停车场" clearable
        style="width: 160px" @change="loadData">
        <el-option v-for="lot in lots" :key="lot.id" :label="lot.lotName" :value="lot.id" />
      </el-select>
      <el-date-picker v-model="searchDate" type="date" placeholder="选择日期"
        value-format="YYYY-MM-DD" style="width: 150px" @change="loadData" clearable />
      <el-select v-model="searchPayStatus" placeholder="支付状态" clearable
        style="width: 120px" @change="loadData">
        <el-option label="已支付" :value="1" />
        <el-option label="未支付" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </template>

    <el-table :data="pagedData" v-loading="loading" border stripe style="width: 100%" scrollbar-always-on>
      <el-table-column prop="id"          label="ID"         width="70" />
      <el-table-column prop="lotName"     label="停车场"     width="150" />
      <el-table-column prop="plateNumber" label="车牌号"     width="130" />
      <!-- 费用类型：1=包月续费，2=临停缴费 -->
      <el-table-column label="费用类型" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.carType === 1 ? 'success' : 'warning'" size="small">
            {{ scope.row.carType === 1 ? '包月续费' : '临停缴费' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="amount"  label="应缴金额(元)" width="120" />
      <el-table-column prop="payTime" label="支付时间"     width="180" />
      <!-- 支付状态：1=已支付（绿色），0=未支付（红色） -->
      <el-table-column label="支付状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.payStatus === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.payStatus === 1 ? '已支付' : '未支付' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="editAllowed" label="操作" width="160" fixed="right">
        <template #default="scope">
          <!-- 只有未支付的才显示"确认支付"，:loading 防止重复点击 -->
          <el-button v-if="scope.row.payStatus === 0" type="primary" size="small"
            :loading="payingId === scope.row.id" @click="handlePay(scope.row)">
            确认支付
          </el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-box">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize"
        layout="total, prev, pager, next" :total="tableData.length" />
    </div>

    <!-- 新增缴费弹窗（手动补录缴费记录） -->
    <el-dialog v-model="addDialogVisible" title="新增缴费记录" width="520px">
      <el-form :model="addForm" :rules="addFormRules" ref="addFormRef" label-width="100px">
        <el-form-item label="停车场" prop="lotId">
          <el-select v-model="addForm.lotId" placeholder="请选择停车场" style="width: 100%">
            <el-option v-for="lot in lots" :key="lot.id" :label="lot.lotName" :value="lot.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="车牌号" prop="plateNumber"><el-input v-model="addForm.plateNumber" placeholder="请输入车牌号" /></el-form-item>
        <el-form-item label="费用类型" prop="carType">
          <el-select v-model="addForm.carType" style="width: 100%">
            <el-option label="包月续费" :value="1" /><el-option label="临停缴费" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付金额" prop="amount"><el-input-number v-model="addForm.amount" :min="0" :precision="2" style="width: 100%" placeholder="请输入支付金额" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </PageLayout>
</template>

<script setup>
/**

 * 【缴费记录管理页】ParkPayment.vue
 *
 * 功能：
 *   1. 查询所有缴费记录（临停缴费 + 包月续费）
 *   2. 对未支付记录进行"确认支付"操作（有二次确认弹窗）
 *   3. 手动新增缴费记录（补录）
 *   4. 删除缴费记录
 *
 * 注意：停车场名称（lotName）数据库没有直接存，
 * 是前端加载数据后，根据 lotId 从 lots 数组里查找停车场名拼上去的
 */

import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { canEdit } from '../utils/permission'
import PageLayout from '../components/PageLayout.vue'

const editAllowed    = canEdit('payment')
const tableData      = ref([])
const loading        = ref(false)
const currentPage    = ref(1)
const pageSize       = 10
const pagedData      = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return tableData.value.slice(start, start + pageSize)
})
const searchPlate     = ref('')
const searchLotId     = ref(null)
const searchDate      = ref('')
const searchPayStatus = ref(null)
const addDialogVisible = ref(false)
const lots           = ref([])
// payingId：记录当前正在支付的记录 ID，让对应行的按钮显示 loading
// 防止多次点击（其他行不受影响）
const payingId       = ref(null)

const addFormRef = ref(null)
const addForm    = ref({ lotId: null, plateNumber: '', carType: 2, amount: null })
const addFormRules = {
  lotId:       [{ required: true, message: '请选择停车场', trigger: 'change' }],
  plateNumber: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  carType:     [{ required: true, message: '请选择费用类型', trigger: 'change' }],
  amount:      [{ required: true, message: '请输入支付金额', trigger: 'change' }]
}

const loadLots = async () => {
  try {
    const res = await request.get('/api/park-lot/list')
    lots.value = res.data || []
  } catch (e) { console.error(e) }
}

/** 重置查询条件并刷新列表 */
const handleReset = () => {
  searchPlate.value = ''
  searchLotId.value = null
  searchDate.value = ''
  searchPayStatus.value = null
  loadData()
}

/**
 * 加载缴费记录，并拼接停车场名称
 *
 * 支持车牌号、停车场、日期、支付状态组合查询。
 *
 * 数据库的 park_payment 表只存了 lot_id，没有 lot_name，
 * 前端用 lotCache（对象缓存）避免重复查找同一停车场：
 *   lotCache[lotId] = lotName
 */
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      plateNumber: searchPlate.value,
      lotId: searchLotId.value || undefined,
      date: searchDate.value || undefined,
      payStatus: searchPayStatus.value !== null && searchPayStatus.value !== ''
        ? searchPayStatus.value : undefined
    }
    const res = await request.get('/api/payment/list', { params })
    const records  = res.data || []
    const lotCache = {}  // 缓存：避免同一停车场 ID 重复查找
    for (const r of records) {
      if (!r.lotId) continue
      if (!lotCache[r.lotId]) {
        const lot = lots.value.find(l => l.id === r.lotId)
        lotCache[r.lotId] = lot ? lot.lotName : '（已删除停车场）'
      }
      r.lotName = lotCache[r.lotId]  // 直接给记录对象加一个 lotName 字段
    }
    tableData.value = records
    currentPage.value = 1
  } catch (error) { console.error(error) }
  finally { loading.value = false }
}

const openAddDialog = () => {
  addForm.value = { lotId: null, plateNumber: '', carType: 2, amount: null }
  addDialogVisible.value = true
}

const handleAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await request.post('/api/payment/add', addForm.value)
    ElMessage.success('新增缴费成功')
    addDialogVisible.value = false
    loadData()
  } catch (error) { console.error(error) }
}

/** 确认支付（二次确认弹窗，防止误操作） */
const handlePay = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认收取车牌 ${row.plateNumber} 的停车费 ¥${row.amount} 吗？`,
      '确认支付', { type: 'warning', confirmButtonText: '确认收款', cancelButtonText: '取消' }
    )
    payingId.value = row.id  // 设置当前支付中的 ID，该行按钮显示 loading
    await request.put(`/api/payment/pay/${row.id}`)  // 调后端接口把 payStatus 改为 1
    ElMessage.success('支付成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') console.error(error)  // 用户点取消不报错，其他错误正常打印
  } finally {
    payingId.value = null  // 支付结束（无论成功失败），清除 loading 状态
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条缴费记录吗？', '提示', { type: 'warning' })
    await request.delete(`/api/payment/delete/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) { console.error(error) }
}

// async：loadLots 必须先完成，才能在 loadData 里正确查到停车场名
onMounted(async () => { await loadLots(); loadData() })
</script>

<style scoped>
.pagination-box { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
