<template>
  <PageLayout title="停车场管理">
    <template #actions>
      <!--
        权限控制：只有超级管理员或普通管理员（且有编辑权限）才显示新增按钮
        editAllowed 来自 permission.js 的 canEdit()，判断当前角色是否有编辑权限
      -->
      <el-button
        v-if="(userRole === 'SUPER_ADMIN' || userRole === 'NORMAL_ADMIN') && editAllowed"
        type="primary"
        @click="handleAdd"
      >新增停车场</el-button>
    </template>

    <template #search>
      <el-input
        v-model="searchLotName"
        placeholder="请输入停车场名称查询"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </template>

    <!-- 统一表格容器 -->
    <div class="table-wrapper">
      <!--
        el-table：Element Plus 的表格组件
        :data="pagedData"：表格数据源（计算属性，当前页的数据切片）
        border：显示边框；stripe：斑马纹；v-loading：数据加载中显示 loading 遮罩
      -->
      <el-table :data="pagedData" border stripe style="width: 100%" v-loading="loading" scrollbar-always-on>
        <el-table-column prop="id"          label="ID"              width="60"  align="center" />
        <el-table-column prop="lotName"     label="停车场名称"      min-width="140" />
        <el-table-column prop="totalSpaces" label="总车位数"        width="90"  align="center" />
        <el-table-column prop="usedSpaces"  label="已用车位"        width="90"  align="center" />

        <!-- 自定义列：包月费用（null 时显示 — ） -->
        <el-table-column prop="monthlyFee" label="包月费用(元/月)" width="130" align="center">
          <template #default="scope">
            <!-- scope.row 是当前行的数据对象，相当于 tableData[i] -->
            {{ scope.row.monthlyFee != null ? scope.row.monthlyFee : '—' }}
          </template>
        </el-table-column>

        <el-table-column prop="freeMinutes" label="免费时长(分)" width="115" align="center">
          <template #default="scope">
            {{ scope.row.freeMinutes != null ? scope.row.freeMinutes : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="unitPrice" label="临停单价(元/时)" width="130" align="center" />
        <el-table-column prop="maxFee"    label="每日上限(元)"    width="110" align="center" />

        <!-- 续费优惠：把 JSON 字符串解析后用 el-tag 展示每条优惠规则 -->
        <el-table-column label="续费优惠" min-width="200">
          <template #default="scope">
            <template v-if="parseDiscounts(scope.row.discounts).length">
              <el-tag
                v-for="(d, i) in parseDiscounts(scope.row.discounts)"
                :key="i"
                type="success"
                size="small"
                style="margin: 2px 3px"
              >满{{ d.months }}月减{{ d.discount }}元</el-tag>
            </template>
            <span v-else style="color:#c0c4cc;font-size:13px">暂无优惠</span>
          </template>
        </el-table-column>

        <!-- 操作列：只有超级管理员且有编辑权限才显示；fixed="right" 固定在右侧，横向滚动时不消失 -->
        <el-table-column
          v-if="userRole === 'SUPER_ADMIN' && editAllowed"
          label="操作"
          width="150"
          align="center"
          fixed="right"
        >
          <template #default="scope">
            <el-button size="small" type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger"  link @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件：v-model:current-page 双向绑定当前页，:total 总条数 -->
      <div class="pagination-box">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          :total="tableData.length"
        />
      </div>
    </div>

    <!-- 底部统计图表：各停车场车位占用率 -->
    <div class="stats-panel">
      <div class="stats-title">
        <el-icon><TrendCharts /></el-icon>
        <span>各停车场车位占用率</span>
      </div>
      <div v-if="occupancyList.length" class="occupancy-list">
        <div v-for="item in occupancyList" :key="item.id" class="occupancy-row">
          <div class="occupancy-info">
            <span class="occupancy-name">{{ item.lotName }}</span>
            <span class="occupancy-detail">{{ item.usedSpaces }} / {{ item.totalSpaces }} 车位</span>
          </div>
          <div class="occupancy-bar">
            <el-progress
              :percentage="animated ? item.rate : 0"
              :color="getProgressColor(item.rate)"
              :stroke-width="14"
              :show-text="true"
            />
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无停车场数据" :image-size="80" />
    </div>

    <!-- 底部统计表：各停车场临时车入场数量（支持按月切换） -->
    <div class="stats-panel" v-loading="tempEntryLoading">
      <div class="stats-title stats-title--with-controls">
        <div class="stats-title-left">
          <el-icon><TrendCharts /></el-icon>
          <span>各停车场临时车进入数量</span>
        </div>
        <el-button-group>
          <el-button size="small" :icon="ArrowLeft" @click="prevStatMonth" />
          <el-button size="small" disabled style="min-width:110px;font-weight:600">
            {{ statYear }} 年 {{ statMonth }} 月
          </el-button>
          <el-button size="small" :icon="ArrowRight" @click="nextStatMonth" :disabled="isCurrentStatMonth" />
        </el-button-group>
      </div>
      <el-table :data="tempEntryList" border stripe style="width: 100%">
        <el-table-column prop="lotName" label="停车场名称" min-width="180" />
        <el-table-column prop="tempEntryCount" label="临时车进入数量" width="160" align="center">
          <template #default="scope">
            <el-tag type="warning" size="small">{{ scope.row.tempEntryCount }} 辆</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!tempEntryList.length && !tempEntryLoading" description="暂无数据" :image-size="80" />
    </div>

    <!-- 新增/编辑弹窗（同一个弹窗，通过 dialogTitle 区分） -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="115px">
        <el-form-item label="车场名称" prop="lotName"><el-input v-model="form.lotName" placeholder="请输入名称" /></el-form-item>
        <el-form-item label="总车位数" prop="totalSpaces"><el-input-number v-model="form.totalSpaces" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="包月费用(元/月)" prop="monthlyFee"><el-input-number v-model="form.monthlyFee" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="免费时长(分)" prop="freeMinutes"><el-input-number v-model="form.freeMinutes" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="计费单价(元)" prop="unitPrice"><el-input-number v-model="form.unitPrice" :precision="2" :step="0.5" style="width:100%" /></el-form-item>
        <el-form-item label="每日上限(元)" prop="maxFee"><el-input-number v-model="form.maxFee" :precision="2" :step="5" style="width:100%" /></el-form-item>

        <!-- 续费优惠规则配置（动态增减，最多 3 条） -->
        <el-divider content-position="left" style="margin:14px 0 10px">
          续费优惠设置
          <span style="font-size:12px;color:#909399;font-weight:normal">（最多 3 种，留空则不启用）</span>
        </el-divider>
        <div v-for="(rule, idx) in form.discountRules" :key="idx" class="discount-row">
          <span class="discount-label">优惠 {{ idx + 1 }}</span>
          <span class="discount-text">每满</span>
          <el-input-number v-model="rule.months"   :min="1" :max="60" style="width:110px" />
          <span class="discount-text">个月，减</span>
          <el-input-number v-model="rule.discount" :min="0" :precision="0" style="width:110px" />
          <span class="discount-text">元</span>
          <el-button type="danger" size="small" link @click="removeDiscount(idx)">删除</el-button>
        </div>
        <div style="margin-top:8px" v-if="form.discountRules.length < 3">
          <el-button size="small" type="primary" plain @click="addDiscount">+ 添加优惠规则</el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </PageLayout>
</template>

<script setup>
/**
 * 【停车场管理页】ParkLot.vue —— 停车场的增删改查
 *
 * 功能：
 *   1. 分页展示所有停车场信息（名称、车位、收费规则等）
 *   2. 新增/编辑停车场（支持设置续费优惠规则）
 *   3. 删除停车场（二次确认弹窗）
 *   4. 按名称模糊搜索
 *
 * 权限控制：
 *   - 超级管理员：可以新增、编辑、删除
 *   - 普通管理员（且有编辑权限）：可以新增，但不能编辑/删除
 *   - 只读用户：只能查看，按钮不显示
 *
 * 续费优惠存储：
 *   优惠规则是 JSON 数组存在数据库 discounts 字段里，
 *   格式：[{"months":3,"discount":50},{"months":6,"discount":120}]
 *   前端展示时用 parseDiscounts() 解析 JSON 字符串
 */

import { ref, computed, onMounted, nextTick } from 'vue'
import { Search, Refresh, TrendCharts, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { canEdit } from '../utils/permission'  // 权限判断工具
import PageLayout from '../components/PageLayout.vue'

// 从 localStorage 读取当前用户角色，用于控制按钮显示
const userRole    = localStorage.getItem('role')
// canEdit('park-lot')：判断当前用户对停车场管理是否有编辑权限
const editAllowed = canEdit('park-lot')

const tableData     = ref([])    // 完整的停车场列表（前端分页，全量加载）
const loading       = ref(false) // 表格 loading 状态
const dialogVisible = ref(false) // 新增/编辑弹窗显示状态
const dialogTitle   = ref('新增停车场')  // 弹窗标题（新增/编辑时动态变）
const searchLotName = ref('')    // 搜索框绑定值
const currentPage   = ref(1)    // 当前页码
const pageSize      = 5         // 每页显示条数（固定值，不需要响应式）
const animated      = ref(false) // 控制进度条入场增长动画

// ---- 各停车场临时车入场数量统计（按月切换） ----
const now = new Date()
const statYear  = ref(now.getFullYear())   // 统计年份
const statMonth = ref(now.getMonth() + 1)  // 统计月份
const tempEntryLoading = ref(false)
const tempEntryList    = ref([])           // 临时车入场数量列表

/**
 * 计算属性：当前页的数据切片
 * 前端分页：数据全量从后端加载，前端自己截取当前页的数据
 * 好处：数据量不大时，查询很快，换页不需要再请求后端
 * 坏处：数据量很大时，一次加载所有数据内存消耗大
 */
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return tableData.value.slice(start, start + pageSize)
})

/** 各停车场车位占用率（按占用率降序排列） */
const occupancyList = computed(() => {
  return tableData.value
    .filter(item => item.totalSpaces > 0)
    .map(item => ({
      ...item,
      rate: Math.round((item.usedSpaces / item.totalSpaces) * 100)
    }))
    .sort((a, b) => b.rate - a.rate)
})

/** 根据占用率返回进度条颜色 */
const getProgressColor = (rate) => {
  if (rate >= 90) return '#f56c6c'
  if (rate >= 70) return '#e6a23c'
  return '#409eff'
}

/** 临时车统计月份是否为当前月（用于禁用"下一月"按钮） */
const isCurrentStatMonth = computed(() =>
  statYear.value === now.getFullYear() && statMonth.value === (now.getMonth() + 1)
)

/** 切换到上一个统计月份 */
const prevStatMonth = () => {
  if (statMonth.value === 1) { statYear.value--; statMonth.value = 12 }
  else statMonth.value--
  loadTempEntryData()
}

/** 切换到下一个统计月份 */
const nextStatMonth = () => {
  if (isCurrentStatMonth.value) return
  if (statMonth.value === 12) { statYear.value++; statMonth.value = 1 }
  else statMonth.value++
  loadTempEntryData()
}

/** 加载各停车场临时车入场数量统计 */
const loadTempEntryData = async () => {
  tempEntryLoading.value = true
  try {
    const res = await request.get('/api/statistics/temp-entry-count', {
      params: { year: statYear.value, month: statMonth.value }
    })
    if (res.code === 200) tempEntryList.value = res.data || []
    else ElMessage.error(res.message || '查询失败')
  } catch (e) {
    console.error(e)
    ElMessage.error('获取数据失败')
  } finally {
    tempEntryLoading.value = false
  }
}

/** 表单默认值工厂函数（每次打开弹窗时重置为空表单） */
const defaultForm = () => ({
  id: null, lotName: '', totalSpaces: 100,
  freeMinutes: 30, unitMinutes: 60,
  unitPrice: 5.00, maxFee: 50.00, monthlyFee: 0,
  discountRules: []  // 前端用数组管理，提交时转成 JSON 字符串
})
const form = ref(defaultForm())
const formRef = ref(null)

// 表单校验规则
const formRules = {
  lotName:     [{ required: true, message: '请输入车场名称', trigger: 'blur' }],
  totalSpaces: [{ required: true, message: '请输入总车位数', trigger: 'change' }],
  unitPrice:   [{ required: true, message: '请输入计费单价', trigger: 'change' }],
  maxFee:      [{ required: true, message: '请输入每日上限', trigger: 'change' }]
}

// ---- 续费优惠工具方法 ----

/**
 * 解析优惠规则 JSON 字符串为数组
 * @param {string} str 如：'[{"months":3,"discount":50}]'
 * @returns {Array} 优惠规则数组，解析失败返回空数组
 */
const parseDiscounts = (str) => {
  if (!str) return []
  try {
    return JSON.parse(str).filter(d => d.months > 0 && d.discount > 0)
  } catch { return [] }
}

/** 添加一条优惠规则（最多 3 条） */
const addDiscount    = () => {
  if (form.value.discountRules.length < 3)
    form.value.discountRules.push({ months: null, discount: null })
}

/** 删除指定索引的优惠规则 */
const removeDiscount = (idx) => form.value.discountRules.splice(idx, 1)

// ---- 数据加载 ----

/** 从后端获取停车场列表（支持按名称筛选） */
const loadData = async (lotName = '') => {
  loading.value = true
  animated.value = false // 重置动画，先回到 0%
  try {
    const params = {}
    if (lotName && lotName.trim()) params.lotName = lotName.trim()
    const res = await request.get('/api/park-lot/list', { params })
    if (res.code === 200) tableData.value = res.data || []
    else ElMessage.error(res.message || '查询失败')
  } catch (e) {
    console.error(e)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
  await nextTick()
  animated.value = true // 数据渲染后再触发动画增长到实际占比
}

const handleSearch = () => loadData(searchLotName.value)
const handleReset  = () => { searchLotName.value = ''; loadData() }

// ---- 新增 / 编辑 ----

/** 点击"新增停车场"按钮：清空表单，打开弹窗 */
const handleAdd = () => {
  dialogTitle.value   = '新增停车场'
  form.value          = defaultForm()
  dialogVisible.value = true
}

/** 点击"编辑"按钮：把当前行数据填入表单，打开弹窗 */
const handleEdit = (row) => {
  dialogTitle.value = '编辑停车场'
  // 展开运算符 {...row}：浅拷贝一份数据，避免直接修改表格数据
  // discountRules：把数据库里的 JSON 字符串解析成前端用的数组
  form.value = { ...row, discountRules: parseDiscounts(row.discounts) }
  dialogVisible.value = true
}

/** 弹窗确定按钮：提交新增或编辑 */
const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 过滤掉 months/discount 为 0 的无效规则
  const validRules = form.value.discountRules.filter(d => d.months > 0 && d.discount > 0)
  // 把优惠数组序列化回 JSON 字符串，存入 discounts 字段
  const payload    = { ...form.value, discounts: validRules.length ? JSON.stringify(validRules) : null }
  delete payload.discountRules  // 删掉前端用的临时字段，不要发给后端

  try {
    const isEdit = !!payload.id  // id 存在 = 编辑，不存在 = 新增
    // 三元表达式动态选择 PUT 还是 POST
    const res = await request[isEdit ? 'put' : 'post'](
      isEdit ? '/api/park-lot/update' : '/api/park-lot/add', payload
    )
    if (res.code === 200) {
      ElMessage.success('操作成功')
      dialogVisible.value = false
      loadData(searchLotName.value)  // 刷新列表
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('操作失败')
  }
}

/** 删除（有二次确认弹窗） */
const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除这个停车场吗？', '系统警告', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    try {
      const res = await request.delete(`/api/park-lot/delete/${id}`)
      if (res.code === 200) { ElMessage.success('删除成功'); loadData(searchLotName.value) }
      else ElMessage.error(res.message || '删除失败')
    } catch (e) { console.error(e); ElMessage.error('删除失败') }
  }).catch(() => {})  // 点"取消"不做任何操作
}

// 页面挂载时加载数据
onMounted(() => {
  loadData()
  loadTempEntryData()
})
</script>

<style scoped>
/* 统一表格容器 */
.table-wrapper { width:100%; overflow-x:auto; }
/* 优惠规则一行：横向排列 */
.discount-row  { display:flex; align-items:center; gap:8px; margin-bottom:10px; flex-wrap:wrap; }
.discount-label{ font-size:13px; color:#606266; min-width:42px; }
.discount-text { font-size:13px; color:#303133; white-space:nowrap; }

/* 底部统计图表：停车场车位占用率 */
.stats-panel {
  margin-top: 24px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.stats-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}
.stats-title .el-icon { font-size: 18px; color: #409eff; }
.stats-title--with-controls {
  justify-content: space-between;
}
.stats-title-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.occupancy-list { display: flex; flex-direction: column; gap: 14px; }
.occupancy-row { display: flex; align-items: center; gap: 16px; }
.occupancy-info {
  display: flex;
  flex-direction: column;
  min-width: 160px;
  max-width: 220px;
}
.occupancy-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.occupancy-detail {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.occupancy-bar { flex: 1; min-width: 0; }

/* 进度条增长动效：每次进入页面从 0% 平滑增长到实际占比 */
.occupancy-bar :deep(.el-progress-bar__inner) {
  transition: width 1s ease-out;
}
</style>
