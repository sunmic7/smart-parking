<template>
  <PageLayout title="停车记录管理">
    <template #actions>
      <el-button v-if="editAllowed" type="success" @click="openAddDialog">新增入场</el-button>
    </template>

    <template #search>
      <!-- 多条件组合查询：车牌号、停车场、日期 -->
      <el-input v-model="searchPlate" placeholder="请输入车牌号" clearable
        style="width: 180px" @keyup.enter="loadData" @clear="loadData" />
      <el-select v-model="searchLotId" placeholder="选择停车场" clearable
        style="width: 180px" @change="loadData">
        <el-option v-for="lot in lots" :key="lot.id" :label="lot.lotName" :value="lot.id" />
      </el-select>
      <el-date-picker v-model="searchDate" type="date" placeholder="选择日期"
        value-format="YYYY-MM-DD" style="width: 160px" @change="loadData" clearable />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </template>

    <!--
      停车记录表格：包含入场图片和出场图片预览
      el-image：支持点击放大预览（:preview-src-list 传图片数组）
    -->
    <el-table :data="pagedData" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="id"          label="ID"      width="70" />
      <el-table-column prop="lotName"     label="停车场"  width="160" />
      <el-table-column prop="plateNumber" label="车牌号"  width="140" />
      <!-- 车辆类型：1=包月（绿色），2=临时（橙色） -->
      <el-table-column label="车辆类型" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.carType === 1 ? 'success' : 'warning'">
            {{ scope.row.carType === 1 ? '包月车' : '临时车' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="entryTime" label="入场时间" width="180" />
      <!-- 入场图片列：有图片则显示缩略图，点击可放大预览 -->
      <el-table-column label="入场照片" width="120">
        <template #default="scope">
          <el-image v-if="scope.row.entryImgUrl" :src="scope.row.entryImgUrl"
            style="width: 60px; height: 60px" fit="cover"
            :preview-src-list="[scope.row.entryImgUrl]" />
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column prop="exitTime"  label="出场时间" width="180" />
      <!-- 出场图片列 -->
      <el-table-column label="出场照片" width="120">
        <template #default="scope">
          <el-image v-if="scope.row.exitImgUrl" :src="scope.row.exitImgUrl"
            style="width: 60px; height: 60px" fit="cover"
            :preview-src-list="[scope.row.exitImgUrl]" />
          <span v-else>无</span>
        </template>
      </el-table-column>
      <!-- 状态：0=场内（红色），1=已出场（绿色） -->
      <el-table-column label="状态" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.status === 0 ? 'danger' : 'success'">
            {{ scope.row.status === 0 ? '场内' : '已出场' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="parkingMinutes" label="停车时长(分钟)" width="130" />
      <el-table-column prop="payableAmount"  label="应收金额(元)"   width="120" />
      <!-- 操作列：在场的记录可以出场、调整时间，所有记录可删除 -->
      <el-table-column v-if="editAllowed" label="操作" width="300" fixed="right">
        <template #default="scope">
          <!-- v-if="scope.row.status === 0"：只有在场的记录才显示这两个按钮 -->
          <el-button v-if="scope.row.status === 0" type="primary" size="small"
            @click="openExitDialog(scope.row)">出场登记</el-button>
          <!-- 调整时间：演示用，可以把入场时间往前改，方便展示计费效果 -->
          <el-button v-if="scope.row.status === 0" type="warning" size="small"
            @click="openAdjustDialog(scope.row)">调整时间</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-box">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize"
        layout="total, prev, pager, next" :total="tableData.length" />
    </div>

    <!-- 新增入场弹窗：手动添加入场记录（备用方式，通常走车牌识别页） -->
    <el-dialog v-model="addDialogVisible" title="新增入场记录" width="500px">
      <el-form :model="addForm" :rules="addFormRules" ref="addFormRef" label-width="100px">
        <el-form-item label="停车场" prop="lotId">
          <el-select v-model="addForm.lotId" placeholder="请选择停车场" style="width: 100%">
            <el-option v-for="lot in lots" :key="lot.id" :label="lot.lotName" :value="lot.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="车牌号" prop="plateNumber">
          <el-input v-model="addForm.plateNumber" placeholder="请输入车牌号" />
        </el-form-item>
        <el-form-item label="车辆类型" prop="carType">
          <el-select v-model="addForm.carType" style="width: 100%">
            <el-option label="包月车" :value="1" /><el-option label="临时车" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="入场照片URL">
          <el-input v-model="addForm.entryImgUrl" placeholder="可填写图片地址或留空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 出场登记弹窗：填写出场照片 URL（可选），确认后调后端出场结算接口 -->
    <el-dialog v-model="exitDialogVisible" title="出场登记" width="500px">
      <el-form :model="exitForm" label-width="100px">
        <el-form-item label="记录ID">
          <!-- disabled：禁止修改，仅展示 -->
          <el-input v-model="exitForm.id" disabled />
        </el-form-item>
        <el-form-item label="车牌号">
          <el-input v-model="exitForm.plateNumber" disabled />
        </el-form-item>
        <el-form-item label="出场照片URL">
          <el-input v-model="exitForm.exitImgUrl" placeholder="可填写图片地址或留空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExit">确认出场</el-button>
      </template>
    </el-dialog>

    <!-- 调整入场时间弹窗：演示用，把入场时间往前推，出场时计费会按新时间算 -->
    <el-dialog v-model="adjustDialogVisible" title="调整入场时间（演示用）" width="480px">
      <!-- el-alert：黄色警告提示框 -->
      <el-alert title="此功能仅用于演示计费效果，将车辆入场时间修改为指定时间后，出场时将按新时间计算费用。"
        type="warning" :closable="false" style="margin-bottom: 18px" />
      <el-form :model="adjustForm" :rules="adjustFormRules" ref="adjustFormRef" label-width="100px">
        <el-form-item label="车牌号"><el-input v-model="adjustForm.plateNumber" disabled /></el-form-item>
        <el-form-item label="当前入场时间"><el-input v-model="adjustForm.originalTime" disabled /></el-form-item>
        <el-form-item label="新入场时间" prop="entryTime">
          <!-- el-date-picker type="datetime"：日期+时间选择器 -->
          <!-- value-format="YYYY-MM-DD HH:mm:ss"：统一输出格式 -->
          <el-date-picker v-model="adjustForm.entryTime" type="datetime"
            placeholder="选择新的入场时间"
            format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <!-- 快捷按钮：往前推 N 小时 -->
        <el-form-item label="快捷设置">
          <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">
            <el-button size="small" @click="setEntryTimeOffset(1)">往前1小时</el-button>
            <el-button size="small" @click="setEntryTimeOffset(2)">往前2小时</el-button>
            <el-button size="small" @click="setEntryTimeOffset(3)">往前3小时</el-button>
            <el-button size="small" @click="setEntryTimeOffset(5)">往前5小时</el-button>
            <el-button size="small" @click="setEntryTimeOffset(10)">往前10小时</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdjustTime">确认修改</el-button>
      </template>
    </el-dialog>
  </PageLayout>
</template>

<script setup>
/**
 * 【停车记录管理页】ParkRecord.vue
 *
 * 功能：
 *   1. 按车牌号查询停车记录（含入场图片、出场图片、时长、费用）
 *   2. 手动新增入场记录（通常用车牌识别页自动处理，这里作为备用）
 *   3. 手动出场登记（直接调后端出场结算接口）
 *   4. 调整入场时间（演示计费用，往前推N小时后出场可验证计费是否正确）
 *   5. 删除记录（有二次确认）
 */

import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { canEdit } from '../utils/permission'
import PageLayout from '../components/PageLayout.vue'

const editAllowed = canEdit('record')
const tableData   = ref([])
const loading     = ref(false)
const currentPage = ref(1)
const pageSize    = 5
// 前端分页：截取当前页数据
const pagedData   = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return tableData.value.slice(start, start + pageSize)
})
const searchPlate = ref('')  // 车牌号搜索关键词
const searchLotId = ref(null) // 停车场筛选
const searchDate  = ref('')   // 日期筛选
const lots        = ref([])  // 停车场列表（新增入场与查询共用）

/** 加载停车场列表（用于新增入场弹窗的下拉选择） */
const loadLots = async () => {
  try {
    const res = await request.get('/api/park-lot/list')
    lots.value = res.data || []
  } catch (e) { console.error(e) }
}

// 弹窗状态
const addDialogVisible    = ref(false)
const exitDialogVisible   = ref(false)
const adjustDialogVisible = ref(false)

// 新增入场表单
const addFormRef = ref(null)
const addForm    = ref({ lotId: 1, plateNumber: '', carType: 2, entryImgUrl: '' })
const addFormRules = {
  lotId:       [{ required: true, message: '请选择停车场', trigger: 'change' }],
  plateNumber: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  carType:     [{ required: true, message: '请选择车辆类型', trigger: 'change' }]
}

// 出场登记表单
const exitForm = ref({ id: null, plateNumber: '', exitImgUrl: '' })

// 调整入场时间表单
const adjustFormRef = ref(null)
const adjustForm    = ref({ id: null, plateNumber: '', originalTime: '', entryTime: '' })
const adjustFormRules = {
  entryTime: [{ required: true, message: '请选择新的入场时间', trigger: 'change' }]
}

/** 打开调整时间弹窗：把当前行的入场时间填入表单 */
const openAdjustDialog = (row) => {
  adjustForm.value = {
    id: row.id, plateNumber: row.plateNumber,
    originalTime: row.entryTime,  // 保存原始时间，快捷按钮以它为基准
    entryTime: row.entryTime
  }
  adjustDialogVisible.value = true
}

/**
 * 快捷往前推时间
 * 以原始入场时间（originalTime）为基准往前推 hours 小时
 * 目的：演示"停了3小时要收多少钱"，不用等真实时间
 *
 * @param {number} hours 往前推几小时
 */
const setEntryTimeOffset = (hours) => {
  const base = new Date(adjustForm.value.originalTime)
  if (isNaN(base)) return
  base.setHours(base.getHours() - hours)  // 往前推
  const pad = n => String(n).padStart(2, '0')
  adjustForm.value.entryTime =
    `${base.getFullYear()}-${pad(base.getMonth()+1)}-${pad(base.getDate())} ` +
    `${pad(base.getHours())}:${pad(base.getMinutes())}:${pad(base.getSeconds())}`
}

/** 提交调整入场时间 */
const handleAdjustTime = async () => {
  const valid = await adjustFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await request.put('/api/record/update-entry-time', {
      id: adjustForm.value.id, entryTime: adjustForm.value.entryTime
    })
    ElMessage.success('入场时间已更新')
    adjustDialogVisible.value = false
    loadData()
  } catch (error) { console.error(error) }
}

/** 重置查询条件并刷新列表 */
const handleReset = () => {
  searchPlate.value = ''
  searchLotId.value = null
  searchDate.value = ''
  loadData()
}

/** 加载停车记录列表（支持车牌号、停车场、日期组合过滤） */
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      plateNumber: searchPlate.value,
      lotId: searchLotId.value || undefined,
      date: searchDate.value || undefined
    }
    const res = await request.get('/api/record/list', { params })
    tableData.value = res.data || []
    currentPage.value = 1
  } catch (error) { console.error(error) }
  finally { loading.value = false }
}

const openAddDialog = () => {
  addForm.value = { lotId: 1, plateNumber: '', carType: 2, entryImgUrl: '' }
  addDialogVisible.value = true
}

/** 手动新增入场记录 */
const handleAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await request.post('/api/record/add', addForm.value)
    ElMessage.success('新增入场成功')
    addDialogVisible.value = false
    loadData()
  } catch (error) { console.error(error) }
}

const openExitDialog = (row) => {
  exitForm.value = { id: row.id, plateNumber: row.plateNumber, exitImgUrl: '' }
  exitDialogVisible.value = true
}

/** 手动出场登记（调后端出场结算接口，自动计费） */
const handleExit = async () => {
  try {
    await request.post('/api/record/exit', exitForm.value)
    ElMessage.success('出场登记成功')
    exitDialogVisible.value = false
    loadData()
  } catch (error) { console.error(error) }
}

/** 删除停车记录（二次确认） */
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条停车记录吗？', '提示', { type: 'warning' })
    await request.delete(`/api/record/delete/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) { console.error(error) }
}

onMounted(() => { loadLots(); loadData() })
</script>

<style scoped>
.pagination-box { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
