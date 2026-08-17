<template>
  <PageLayout title="操作日志">
    <template #actions>
      <!-- 清空日志按钮：只有超级管理员才能看到 -->
      <el-button v-if="isSuperAdmin" type="danger" plain @click="handleClear">清空日志</el-button>
    </template>

    <template #search>
      <!-- 多条件筛选：账号、模块、操作类型、结果、时间范围 -->
      <el-input v-model="query.username" placeholder="账号" clearable style="width:140px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <!-- 模块和操作类型用下拉选择，选项与 @OperationLog 注解里的值保持一致 -->
      <el-select v-model="query.module" placeholder="功能模块" clearable style="width:140px" @change="handleSearch" @clear="handleSearch">
        <el-option v-for="m in moduleOptions" :key="m" :label="m" :value="m" />
      </el-select>
      <el-select v-model="query.action" placeholder="操作类型" clearable style="width:120px" @change="handleSearch" @clear="handleSearch">
        <el-option v-for="a in actionOptions" :key="a" :label="a" :value="a" />
      </el-select>
      <el-select v-model="query.status" placeholder="操作结果" clearable style="width:110px" @change="handleSearch" @clear="handleSearch">
        <el-option label="成功" :value="1" /><el-option label="失败" :value="0" />
      </el-select>
      <!-- 日期范围：配合后端按 createTime 区间筛选 -->
      <el-date-picker v-model="query.startTime" type="date" placeholder="开始日期"
        value-format="YYYY-MM-DD" style="width:140px" @change="handleSearch" />
      <el-date-picker v-model="query.endTime" type="date" placeholder="结束日期"
        value-format="YYYY-MM-DD" style="width:140px" @change="handleSearch" />
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </template>

    <!-- 日志表格：含操作人、模块、结果、耗时、IP 等信息 -->
    <el-table :data="tableData" border stripe style="width:100%" v-loading="loading">
      <el-table-column prop="id"          label="ID"     width="70"  align="center" />
      <el-table-column prop="username"    label="账号"   width="120" />
      <el-table-column prop="realName"    label="操作人" width="100" />
      <el-table-column prop="module"      label="功能模块" width="130" />
      <!-- 操作类型：不同类型不同颜色（见 actionTagType 函数） -->
      <el-table-column prop="action" label="操作类型" width="100" align="center">
        <template #default="scope">
          <el-tag :type="actionTagType(scope.row.action)" size="small">{{ scope.row.action }}</el-tag>
        </template>
      </el-table-column>
      <!-- show-overflow-tooltip：内容过长时鼠标悬停显示完整内容 -->
      <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="requestUrl"  label="请求URL"  width="220" show-overflow-tooltip />
      <el-table-column prop="requestIp"   label="IP地址"   width="130" />
      <!-- 操作结果：成功=绿色，失败=红色 -->
      <el-table-column label="结果" width="80" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <!-- 失败原因：有错误信息时显示红色，没有显示灰色横杠 -->
      <el-table-column prop="errorMsg" label="失败原因" width="160" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.errorMsg" style="color:#f56c6c;font-size:12px">{{ scope.row.errorMsg }}</span>
          <span v-else style="color:#c0c4cc">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="costTime"   label="耗时(ms)" width="90"  align="center" />
      <el-table-column prop="createTime" label="操作时间" width="175" />
      <!-- 删除按钮：只有超管可以删单条日志 -->
      <el-table-column v-if="isSuperAdmin" label="操作" width="80" align="center" fixed="right">
        <template #default="scope">
          <el-button type="danger" size="small" link @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!--
      后端分页（与其他页面的前端分页不同！）
      日志数据量大，必须后端分页，每次只查当前页的数据
      @size-change：每页条数改变时重新查；@current-change：翻页时重新查
    -->
    <div class="pagination-box">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10]"
        layout="total, prev, pager, next, jumper"
        :total="pagination.total"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </PageLayout>
</template>

<script setup>
/**
 * 【操作日志页】SysLog.vue
 *
 * 展示后端 AOP 切面（OperationLogAspect）记录的所有操作日志。
 * 与其他列表页不同，这里使用【后端分页】而不是前端分页，
 * 原因：日志数据量大（每次操作都会写一条），一次全量加载太慢。
 *
 * 后端分页：每次查询传 page 和 pageSize，后端返回当前页数据和总记录数
 * 前端分页：一次查全部，前端 slice 切片显示
 */

import { ref, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import PageLayout from '../components/PageLayout.vue'

// 只有超级管理员才能删除日志和清空日志
const isSuperAdmin = localStorage.getItem('role') === 'SUPER_ADMIN'

// 查询条件（多字段联合过滤）
const query = ref({
  username: '', module: '', action: '',
  status: null,  // null 表示不过滤，1=成功，0=失败
  startTime: '', endTime: ''
})

// 模块选项（与后端 @OperationLog 注解的 module 字段值一致）
const moduleOptions = ['停车场管理', '车辆管理', '车牌识别', '停车记录', '缴费记录', '用户管理', '角色管理']
// 操作类型选项（与 @OperationLog 注解的 action 字段值一致）
const actionOptions = ['新增', '编辑', '删除', '入场', '出场', '登录', '续费', '清空']

/**
 * 操作类型标签颜色映射
 * 让不同操作用不同颜色区分：
 *   新增/入场 → 绿色（success）
 *   删除/清空 → 红色（danger）
 *   编辑/续费 → 橙色（warning）
 *   登录      → 蓝色（primary）
 *   出场      → 灰色（info）
 */
const actionTagType = (action) => {
  const map = {
    '新增': 'success', '入场': 'success',
    '删除': 'danger',  '出场': 'info',
    '编辑': 'warning', '续费': 'warning',
    '登录': 'primary', '清空': 'danger'
  }
  return map[action] || ''
}

const tableData = ref([])
const loading   = ref(false)

// 分页状态（后端分页，page 和 pageSize 要传给后端）
const pagination = ref({ page: 1, pageSize: 10, total: 0 })

/** 加载日志列表（后端分页，传 page + pageSize + 筛选条件） */
const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.value.page, pageSize: pagination.value.pageSize }
    // 把非空的查询条件加进去
    if (query.value.username)  params.username  = query.value.username
    if (query.value.module)    params.module    = query.value.module
    if (query.value.action)    params.action    = query.value.action
    if (query.value.status !== null && query.value.status !== '') params.status = query.value.status
    if (query.value.startTime) params.startTime = query.value.startTime
    if (query.value.endTime)   params.endTime   = query.value.endTime

    const res = await request.get('/api/log/list', { params })
    if (res.code === 200) {
      const data = res.data || {}
      // 后端返回分页对象：{ records: [...], total: 100, ... }
      tableData.value        = data.records || []
      pagination.value.total = data.total   || 0
    } else { ElMessage.error(res.message || '查询失败') }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

/** 查询：重置到第 1 页再搜索 */
const handleSearch = () => { pagination.value.page = 1; loadData() }

/** 重置：清空所有筛选条件，回第 1 页 */
const handleReset = () => {
  query.value = { username: '', module: '', action: '', status: null, startTime: '', endTime: '' }
  pagination.value.page = 1
  loadData()
}

/** 删除单条日志 */
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除这条日志吗？', '提示', { type: 'warning' })
    const res = await request.delete(`/api/log/delete/${id}`)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
    else ElMessage.error(res.message || '删除失败')
  } catch (e) { console.error(e) }
}

/** 清空全部日志（不可恢复，两次确认） */
const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清空全部操作日志吗？此操作不可恢复！', '警告',
      { type: 'warning', confirmButtonText: '确定清空', cancelButtonText: '取消' })
    const res = await request.delete('/api/log/clear')
    if (res.code === 200) { ElMessage.success('日志已清空'); loadData() }
    else ElMessage.error(res.message || '清空失败')
  } catch (e) { console.error(e) }
}

onMounted(loadData)
</script>

<style scoped>
.pagination-box { display: flex; justify-content: flex-end; margin-top: 18px; }
</style>
