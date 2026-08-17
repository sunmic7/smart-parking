<template>
  <PageLayout title="包月车辆管理">
    <template #actions>
      <!-- 只有有编辑权限的用户才显示"新增"按钮 -->
      <el-button v-if="editAllowed" type="primary" @click="handleAdd">新增包月车</el-button>
    </template>

    <template #search>
      <el-input v-model="query.lotName"     placeholder="停车场名称" clearable style="width:160px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.plateNumber" placeholder="车牌号"     clearable style="width:140px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.ownerName"   placeholder="车主姓名"   clearable style="width:120px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.phone"       placeholder="手机号"     clearable style="width:150px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.spaceNumber" placeholder="已购车位"   clearable style="width:120px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-button type="primary" :icon="Search"  @click="handleSearch">查询</el-button>
      <el-button            :icon="Refresh" @click="handleReset">重置</el-button>
    </template>

    <!-- 统一表格容器 -->
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" border stripe style="width:100%" scrollbar-always-on>
        <el-table-column prop="lotName"     label="停车场名称" width="160" />
        <el-table-column prop="plateNumber" label="车牌号"     width="115" />
        <el-table-column prop="ownerName"   label="车主姓名"   width="100" />
        <!-- 性别：数字→文字，1=男，2=女，其他=未知 -->
        <el-table-column prop="gender" label="性别" width="70" align="center">
          <template #default="scope">
            {{ scope.row.gender === 1 ? '男' : scope.row.gender === 2 ? '女' : '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="phone"       label="手机号"   width="130" />
        <el-table-column prop="spaceNumber" label="已购车位" width="100" />
        <el-table-column prop="startDate"  label="起始日"   width="110" />
        <el-table-column prop="expireDate" label="到期日"   width="110" />
        <!-- 状态：1=正常（绿色），0=过期（红色），由后端定时任务自动更新 -->
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '正常' : '过期' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 操作列：编辑、续费、删除 -->
        <el-table-column v-if="editAllowed" label="操作" width="190" fixed="right" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="warning" link @click="handleRenew(scope.row)">续费</el-button>
            <el-button size="small" type="danger"  link @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize"
          layout="total, prev, pager, next" :total="carList.length" />
      </div>
    </div>

    <!-- 底部统计面板：展示月租车辆核心指标 -->
    <div class="stats-panel">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon stat-icon-total">
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-title">月租车辆总数</div>
              <div class="stat-value">{{ totalCount }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon stat-icon-new">
              <el-icon><Plus /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-title">今日新增</div>
              <div class="stat-value">{{ todayAddedCount }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon stat-icon-expire">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-title">即将到期（7天内）</div>
              <div class="stat-value">{{ expiringSoonCount }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon stat-icon-renew">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-title">本月续费率</div>
              <div class="stat-value">{{ renewalRate }}%</div>
              <el-progress :percentage="renewalRate" :show-text="false" :stroke-width="6" color="#67c23a" />
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- ====== 新增/编辑弹窗 ====== -->
    <!-- isEdit 为 true 时显示"编辑"，false 时显示"新增" -->
    <el-dialog :title="isEdit ? '编辑包月车' : '新增包月车'" v-model="dialogVisible" width="520px">
      <!-- :rules="formRules" + ref="formRef"：配合 formRef.validate() 做表单校验 -->
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="停车场" prop="lotId">
          <el-select v-model="form.lotId" placeholder="请选择停车场" style="width:100%">
            <el-option v-for="lot in lotOptions" :key="lot.id" :label="lot.lotName" :value="lot.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="车牌号"   prop="plateNumber"><el-input v-model="form.plateNumber" /></el-form-item>
        <el-form-item label="车主姓名" prop="ownerName">  <el-input v-model="form.ownerName" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" style="width:100%">
            <el-option label="男" :value="1" /><el-option label="女" :value="2" /><el-option label="未知" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">  <el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="已购车位"><el-input v-model="form.spaceNumber" clearable /></el-form-item>
        <!-- 新增时显示起始日，提示需要续费激活 -->
        <el-form-item v-if="!isEdit" label="起始日" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="默认今天" />
          <div style="font-size:12px;color:#909399;margin-top:4px">新增后请通过"续费"按钮激活包月</div>
        </el-form-item>
        <!-- 编辑时显示到期日（允许手动修改） -->
        <el-form-item v-if="isEdit" label="到期日">
          <el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="请选择到期日" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="正常" :value="1" /><el-option label="过期" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- ====== 续费弹窗（最复杂，自动计算优惠） ====== -->
    <el-dialog title="包月续费" v-model="renewDialogVisible" width="440px">
      <el-form :model="renewForm" label-width="100px">
        <!-- 只读展示信息 -->
        <el-form-item label="停车场"><span class="renew-info">{{ renewLotName }}</span></el-form-item>
        <el-form-item label="车牌号"> <span class="renew-info">{{ renewForm.plateNumber }}</span></el-form-item>
        <el-form-item label="月单价">
          <span class="renew-info">
            {{ renewMonthlyFee != null ? renewMonthlyFee + ' 元/月' : '未设置' }}
          </span>
        </el-form-item>

        <!-- 续费月数：手动输入，或点击下方优惠档位快速填入 -->
        <el-form-item label="续费月数">
          <el-input-number v-model="renewForm.months" :min="1" :max="60" style="width:140px" />
          <span style="margin-left:8px;font-size:13px;color:#909399">月</span>
        </el-form-item>

        <!-- 优惠档位快捷选择：点击自动填入续费月数，高亮当前生效档位 -->
        <el-form-item v-if="renewDiscounts.length" label="优惠档位">
          <div class="discount-tags">
            <el-tag
              v-for="d in renewDiscounts" :key="d.months"
              :type="activeDiscount && activeDiscount.months === d.months ? 'success' : 'info'"
              style="cursor:pointer;margin:3px"
              @click="renewForm.months = d.months"
            >满{{ d.months }}月减{{ d.discount }}元
              <span v-if="activeDiscount && activeDiscount.months === d.months">（生效中）</span>
            </el-tag>
          </div>
        </el-form-item>

        <!-- 费用明细：原价 - 优惠 = 实付（computed 属性自动计算） -->
        <el-form-item label="费用明细">
          <div class="fee-detail">
            <div class="fee-row">
              <span>原价：</span>
              <span>{{ renewMonthlyFee != null
                ? `${renewMonthlyFee} × ${renewForm.months} = ${renewBaseAmount} 元` : '—' }}</span>
            </div>
            <div class="fee-row fee-discount" v-if="renewDiscountAmount > 0">
              <span>优惠减免：</span>
              <span>- {{ renewDiscountAmount }} 元
                <span style="font-size:12px;color:#909399" v-if="activeDiscount">
                  （每满{{ activeDiscount.months }}月减{{ activeDiscount.discount }}元
                  × {{ Math.floor(renewForm.months / activeDiscount.months) }}次）
                </span>
              </span>
            </div>
            <el-divider style="margin:8px 0" />
            <div class="fee-row fee-total">
              <span>实付金额：</span>
              <span class="total-amount">
                {{ renewMonthlyFee != null ? renewFinalAmount + ' 元' : '未设置月费，无法计算' }}
              </span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="renewPayLoading" @click="submitRenewAndPay">
          确认续费并支付 {{ renewMonthlyFee != null ? '¥' + renewFinalAmount : '' }}
        </el-button>
      </template>
    </el-dialog>
  </PageLayout>
</template>

<script setup>
/**
 * 【包月车管理页】MonthlyCar.vue
 *
 * 功能：
 *   1. 多条件查询包月车列表（按停车场、车牌、车主等）
 *   2. 新增/编辑包月车基本信息
 *   3. 删除包月车记录
 *   4. 续费弹窗：自动计算阶梯优惠（续满N月减X元），确认后同时
 *      调续费接口（更新到期日）+ 支付接口（记录缴费），两步合一
 *
 * 注意：续费优惠计算逻辑：
 *   比如设置"满3月减50元"，续12个月 → 12÷3=4次 → 减4×50=200元
 *   多个优惠档位时取对当前月数最优惠的那个档位
 */

import { ref, computed, onMounted } from 'vue'
import { Search, Refresh, OfficeBuilding, Plus, Clock, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { canEdit } from '../utils/permission'
import PageLayout from '../components/PageLayout.vue'

const editAllowed        = canEdit('monthly-car')  // 当前用户对车辆管理是否有编辑权限
const carList            = ref([])             // 完整列表（前端分页）
const loading            = ref(false)
const dialogVisible      = ref(false)          // 新增/编辑弹窗
const renewDialogVisible = ref(false)          // 续费弹窗
const isEdit             = ref(false)          // 区分新增还是编辑
const formRef            = ref(null)           // 表单 ref，用于调用 validate()
const lotOptions         = ref([])             // 停车场选项（下拉框数据）
const currentPage        = ref(1)
const pageSize           = 5

// 前端分页：从完整数组里截取当前页数据
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return carList.value.slice(start, start + pageSize)
})

// ---- 底部统计指标 ----
const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

/** 月租车辆总数 */
const totalCount = computed(() => carList.value.length)

/** 今日新增：按 startDate 统计 */
const todayAddedCount = computed(() =>
  carList.value.filter(item => item.startDate === todayStr.value).length
)

/** 即将到期：到期日在未来 7 天内（含今天） */
const expiringSoonCount = computed(() => {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const future = new Date(today); future.setDate(today.getDate() + 7); future.setHours(23, 59, 59, 999)
  return carList.value.filter(item => {
    if (!item.expireDate) return false
    const exp = new Date(item.expireDate)
    return exp >= today && exp <= future
  }).length
})

/**
 * 本月续费率：按"已续费至本月之后"的车辆占比估算
 * 即到期日 > 本月最后一天的车辆数 / 总数
 */
const renewalRate = computed(() => {
  const total = carList.value.length
  if (!total) return 0
  const now = new Date()
  const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999)
  const renewed = carList.value.filter(item => item.expireDate && new Date(item.expireDate) > endOfMonth).length
  return Math.round((renewed / total) * 100)
})

// 查询条件对象（多字段联合搜索）
const query = ref({ lotName: '', plateNumber: '', ownerName: '', phone: '', spaceNumber: '' })

// 表单数据（新增/编辑共用）
const form = ref({
  id: null, lotId: null, plateNumber: '', ownerName: '',
  gender: 1, phone: '', spaceNumber: '', startDate: '', months: 1, status: 1
})

// ---- 续费相关状态 ----
const renewForm       = ref({ id: null, plateNumber: '', months: 1 })
const renewLotName    = ref('')         // 显示用的停车场名称
const renewMonthlyFee = ref(null)       // 该停车场的包月单价
const renewDiscounts  = ref([])         // 优惠规则数组（从停车场数据解析）
const renewPayLoading = ref(false)

/**
 * 续费原价 = 单价 × 月数（computed，随 renewForm.months 变化自动重算）
 */
const renewBaseAmount = computed(() =>
  renewMonthlyFee.value != null ? +(renewMonthlyFee.value * renewForm.value.months).toFixed(2) : 0
)

/**
 * 优惠减免金额：
 * 遍历所有优惠档位，每个档位算出本次可减多少钱，取最大值
 * 算法：floor(月数 ÷ 档位月数) × 档位优惠金额
 */
const renewDiscountAmount = computed(() => {
  if (!renewDiscounts.value.length) return 0
  const best = renewDiscounts.value
    .map(d => Math.floor(renewForm.value.months / d.months) * d.discount)
    .filter(v => v > 0)
  return best.length ? Math.max(...best) : 0
})

/**
 * 当前生效的优惠档位（用于标签高亮显示"生效中"）
 * 找出算出最多优惠金额的那条规则
 */
const activeDiscount = computed(() => {
  if (!renewDiscounts.value.length) return null
  let bestRule = null, bestAmount = 0
  for (const d of renewDiscounts.value) {
    const amount = Math.floor(renewForm.value.months / d.months) * d.discount
    if (amount > bestAmount) { bestAmount = amount; bestRule = d }
  }
  return bestRule
})

// 实付金额 = 原价 - 优惠（不能为负）
const renewFinalAmount = computed(() => Math.max(0, renewBaseAmount.value - renewDiscountAmount.value))

/** 解析停车场优惠规则 JSON 字符串 */
const parseDiscounts = (str) => {
  if (!str) return []
  try { return JSON.parse(str).filter(d => d.months > 0 && d.discount > 0) }
  catch { return [] }
}

// 表单校验规则
const formRules = {
  lotId:       [{ required: true, message: '请选择停车场', trigger: 'change' }],
  plateNumber: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  ownerName:   [{ required: true, message: '请输入车主姓名', trigger: 'blur' }]
}

// ---- 数据加载 ----

/** 加载停车场下拉列表（同时包含 monthlyFee 和 discounts 字段） */
const fetchLotOptions = async () => {
  try {
    const res = await request.get('/api/park-lot/list')
    if (res.code === 200) lotOptions.value = res.data || []
  } catch (e) { console.error(e) }
}

/** 加载包月车列表（多条件筛选） */
const fetchData = async () => {
  loading.value = true
  try {
    // 把非空的查询条件拼成请求参数（空字符串不传，避免后端误判）
    const params = {}
    if (query.value.lotName.trim())     params.lotName     = query.value.lotName.trim()
    if (query.value.plateNumber.trim()) params.plateNumber = query.value.plateNumber.trim()
    if (query.value.ownerName.trim())   params.ownerName   = query.value.ownerName.trim()
    if (query.value.phone.trim())       params.phone       = query.value.phone.trim()
    if (query.value.spaceNumber.trim()) params.spaceNumber = query.value.spaceNumber.trim()
    const res = await request.get('/api/monthly-car/list', { params })
    if (res.code === 200) carList.value = res.data || []
    else ElMessage.error(res.message || '获取数据失败')
  } catch (e) { console.error(e); ElMessage.error('获取数据失败') }
  finally { loading.value = false }
}

const handleSearch = () => fetchData()
const handleReset  = () => {
  query.value = { lotName: '', plateNumber: '', ownerName: '', phone: '', spaceNumber: '' }
  fetchData()
}

// ---- 新增 / 编辑 ----

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: null, lotId: null, plateNumber: '', ownerName: '', gender: 1, phone: '', spaceNumber: '', startDate: '', status: 1 }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  // 用 ?? 运算符：值为 null/undefined 时才用默认值（比 || 更安全，0 不会被误判）
  form.value = {
    id: row.id, lotId: row.lotId,
    plateNumber: row.plateNumber || '', ownerName: row.ownerName || '',
    gender: row.gender ?? 1, phone: row.phone || '',
    spaceNumber: row.spaceNumber || '',
    startDate: row.startDate || '', expireDate: row.expireDate || '',
    status: row.status ?? 1
  }
  dialogVisible.value = true
}

/** 提交新增/编辑：先做前端表单校验，校验通过再请求后端 */
const submitForm = async () => {
  // formRef.value.validate() 返回 Promise，校验通过 resolve，失败 reject
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return  // 校验失败，Element Plus 会自动显示错误提示
  try {
    const res = await request.post('/api/monthly-car/save', form.value)
    if (res.code === 200) {
      ElMessage.success(res.message || '操作成功')
      dialogVisible.value = false
      fetchData()
    } else { ElMessage.error(res.message || '操作失败') }
  } catch (e) { console.error(e); ElMessage.error('保存失败') }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该包月记录吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        const res = await request.delete(`/api/monthly-car/delete/${id}`)
        if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
        else ElMessage.error(res.message || '删除失败')
      } catch (e) { console.error(e) }
    }).catch(() => {})
}

// ---- 续费 ----

/**
 * 打开续费弹窗：从 lotOptions 里找到该车对应停车场的月单价和优惠规则
 * 填充到续费弹窗里，供用户选择续费月数
 */
const handleRenew = (row) => {
  const lot = lotOptions.value.find(l => l.id === row.lotId)
  renewLotName.value    = row.lotName || (lot ? lot.lotName : '')
  renewMonthlyFee.value = lot && lot.monthlyFee != null ? Number(lot.monthlyFee) : null
  renewDiscounts.value  = lot ? parseDiscounts(lot.discounts) : []
  renewForm.value       = { id: row.id, plateNumber: row.plateNumber, lotId: row.lotId, months: 1 }
  renewDialogVisible.value = true
}

/**
 * 确认续费并支付（两步操作合并成一个按钮）
 *
 * Step 1：调 /monthly-car/renew 接口，后端：
 *         - 把包月车的到期日延长 N 个月
 *         - 生成一条缴费记录（payStatus=0 未支付），返回 paymentId
 * Step 2：调 /api/payment/pay/{paymentId} 接口，把缴费记录标记为已支付
 *
 * 为什么分两步？和出场支付一样，保持数据流一致，
 * 支付记录可以在"缴费记录"页查询追溯。
 */
const submitRenewAndPay = async () => {
  if (!renewMonthlyFee.value && renewMonthlyFee.value !== 0) {
    ElMessage.warning('该停车场未设置月单价，无法续费'); return
  }
  renewPayLoading.value = true
  try {
    const res = await request.post('/api/monthly-car/renew', {
      id:     renewForm.value.id,
      months: renewForm.value.months,
      amount: renewFinalAmount.value   // 传实付金额（含优惠后）
    })
    if (res.code !== 200) { ElMessage.error(res.message || '续费失败'); return }

    const paymentId = res.data?.paymentId  // 后端返回的缴费记录 ID
    if (paymentId) {
      await request.put(`/api/payment/pay/${paymentId}`)  // 立即确认支付
    }
    ElMessage.success(`续费成功！已缴费 ¥${renewFinalAmount.value}，到期日已更新`)
    renewDialogVisible.value = false
    fetchData()  // 刷新列表，到期日会更新
  } catch (e) { console.error(e) }
  finally { renewPayLoading.value = false }
}

// 页面挂载时同时加载停车场列表和包月车列表
onMounted(() => { fetchLotOptions(); fetchData() })
</script>

<style scoped>
/* 统一表格容器 */
.table-wrapper { width:100%; overflow-x:auto; }
.renew-info    { font-size:14px; color:#303133; font-weight:500; }
.discount-tags { display:flex; flex-wrap:wrap; }
/* 费用明细卡片 */
.fee-detail    { background:#f8f9fa; border:1px solid #e4e7ed; border-radius:6px; padding:12px 16px; width:100%; }
.fee-row       { display:flex; justify-content:space-between; font-size:14px; color:#606266; line-height:2; }
.fee-discount  { color:#67c23a; font-weight:600; }  /* 优惠行：绿色 */
.fee-total     { font-size:15px; font-weight:600; color:#303133; }
.total-amount  { color:#f56c6c; font-size:16px; font-weight:700; }  /* 实付金额：红色醒目 */

/* 底部统计面板 */
.stats-panel { margin-top: 24px; }
.stat-card {
  display: flex;
  align-items: center;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 10px;
  margin-right: 14px;
  font-size: 24px;
  color: #fff;
}
.stat-icon-total  { background: linear-gradient(135deg, #409eff, #79bbff); }
.stat-icon-new    { background: linear-gradient(135deg, #67c23a, #95d475); }
.stat-icon-expire { background: linear-gradient(135deg, #e6a23c, #f3d19e); }
.stat-icon-renew  { background: linear-gradient(135deg, #909399, #bfc2c7); }
.stat-body { flex: 1; min-width: 0; }
.stat-title { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-value { font-size: 22px; font-weight: 700; color: #303133; margin-bottom: 8px; }
</style>
