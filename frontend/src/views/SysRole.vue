<template>
  <PageLayout title="角色管理">
    <template #actions>
      <el-button type="success" @click="openDialog()">新增角色</el-button>
    </template>

    <template #search>
      <el-input v-model="searchRoleName" placeholder="请输入角色名称" clearable
        style="width: 220px" @keyup.enter="loadData" @clear="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </template>

    <el-table :data="pagedData" border stripe style="width:100%">
      <el-table-column prop="id"       label="ID"     width="70" />
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="roleCode" label="角色编码" width="150" />
      <el-table-column prop="remark"   label="角色说明" width="160" />



      <!-- 已授权菜单：把 permissions 字符串解析后展示为标签列表 -->
      <el-table-column label="已授权菜单" min-width="280">
        <template #default="scope">
          <template v-if="scope.row.roleCode === 'SUPER_ADMIN'">
            <el-tag type="danger" size="small">全部菜单</el-tag>
          </template>
          <template v-else>
            <template v-if="parseKeys(scope.row.permissions).length">
              <el-tag v-for="k in parseKeys(scope.row.permissions)" :key="k"
                type="primary" size="small" style="margin:2px 3px">
                <!-- PERM_LABEL 把 key 转成中文，如 'park-lot' → '停车场管理' -->
                {{ PERM_LABEL[k] || k }}
              </el-tag>
            </template>
            <span v-else style="color:#c0c4cc;font-size:13px">暂无权限</span>
          </template>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="创建时间" width="175" />

      <el-table-column label="操作" width="175" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="openDialog(scope.row)">编辑</el-button>
          <!-- id=1,2 是内置角色（超管和默认管理员），禁止删除 -->
          <el-button type="danger" size="small"
            :disabled="scope.row.id === 1 || scope.row.id === 2"
            @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-box">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize"
        layout="total, prev, pager, next" :total="tableData.length" />
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'"
      width="500px" @close="resetForm">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" placeholder="如：普通管理员" />
        </el-form-item>
        <el-form-item label="角色编码">
          <!-- 编辑时禁止修改编码，因为编码可能已被用户关联 -->
          <el-input v-model="form.roleCode" placeholder="如：ADMIN" :disabled="!!form.id" />
          <div class="form-tip" v-if="!form.id">编码保存后不可修改，建议全大写英文</div>
        </el-form-item>
        <el-form-item label="角色说明">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>

        <el-divider content-position="left" style="margin:14px 0 10px">权限配置</el-divider>

        <!-- 超管角色不需要配置权限（固定全部菜单） -->
        <template v-if="form.roleCode === 'SUPER_ADMIN'">
          <el-tag type="danger">超级管理员固定拥有全部菜单，且始终可编辑</el-tag>
        </template>
        <template v-else>
          <!-- 每个菜单单独配置：无权限 / 只读 / 可编辑 -->
          <el-form-item label="菜单权限">
            <div class="perm-box">
              <div class="perm-header">
                <span class="perm-header-name">菜单名称</span>
                <span class="perm-header-opts">
                  <span>无权限</span>
                  <span>只读</span>
                  <span>可编辑</span>
                </span>
              </div>
              <el-divider style="margin:8px 0" />
              <div
                v-for="item in PERM_OPTIONS"
                :key="item.value"
                class="perm-row"
              >
                <span class="perm-name">{{ item.label }}</span>
                <el-radio-group v-model="form.perms[item.value]" class="perm-radio-group">
                  <el-radio value="none">无权限</el-radio>
                  <el-radio value="read">只读</el-radio>
                  <el-radio value="edit">可编辑</el-radio>
                </el-radio-group>
              </div>
            </div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageLayout>
</template>

<script setup>
/**
 * 【角色管理页】SysRole.vue
 *
 * 管理系统角色及其权限配置，对应数据库 sys_role 表。
 *
 * 权限格式（permissions 字段存储格式）：
 *   新版："key1:mode,key2:mode,..."
 *     每个菜单单独配置：edit=可编辑 / read=只读
 *     未出现的菜单表示无权限
 *   旧版（兼容）："mode|key1,key2,..."
 *     全局 mode 应用到所有 key
 *
 * 前端展示时用 parsePerms() 解析成对象 { key: mode }，
 * 保存时用 serializePerms() 序列化回新版字符串。
 *
 * 菜单权限控制流程：
 *   超管配置角色权限 → 用户关联角色 →
 *   用户登录 → Layout.vue 加载权限 → 菜单按权限显示/隐藏
 */

import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import PageLayout from '../components/PageLayout.vue'

// 所有可配置的菜单项（key 与路由 path 的最后一段一致）
const PERM_OPTIONS = [
  { value: 'park-lot',        label: '停车场管理' },
  { value: 'park-lot-map',    label: '停车场地图' },
  { value: 'monthly-car',     label: '车辆管理'   },
  { value: 'plate-recognize', label: '车牌识别'   },
  { value: 'record',          label: '停车记录'   },
  { value: 'payment',         label: '缴费记录'   },
  { value: 'user',            label: '用户管理'   },
  { value: 'role',            label: '角色管理'   },
  { value: 'log',             label: '日志管理'   },
]
// key → 中文名 的映射（用于表格里展示）
const PERM_LABEL = Object.fromEntries(PERM_OPTIONS.map(p => [p.value, p.label]))
const ALL_VALUES = PERM_OPTIONS.map(p => p.value)  // 所有菜单 key 的数组

// ---- 权限字符串解析工具 ----

/** 解析权限字符串为对象 { key: 'edit'|'read'|'none' }，兼容新旧两种格式 */
const parsePerms = (str) => {
  const perms = {}
  ALL_VALUES.forEach(k => { perms[k] = 'none' })
  if (!str) return perms

  const raw = str.trim()

  // 旧版格式："mode|key1,key2,..."
  if (raw.includes('|')) {
    const [modePart, keysPart] = raw.split('|', 2)
    const mode = (modePart.trim() === 'read') ? 'read' : 'edit'
    if (keysPart) {
      keysPart.split(',').forEach(s => {
        const k = s.trim().replace(/:(edit|read)$/, '')
        if (k) perms[k] = mode
      })
    }
    return perms
  }

  // 新版格式："key1:mode,key2:mode,..."
  raw.split(',').forEach(s => {
    const item = s.trim()
    if (!item) return
    if (item.includes(':')) {
      const [k, m] = item.split(':', 2)
      const key = k.trim()
      const mode = m.trim()
      if (key && ALL_VALUES.includes(key)) {
        perms[key] = (mode === 'edit' || mode === 'read') ? mode : 'none'
      }
    } else {
      // 没有冒号，默认 edit
      if (ALL_VALUES.includes(item)) perms[item] = 'edit'
    }
  })
  return perms
}

/** 解析授权菜单 key 数组（用于表格标签展示） */
const parseKeys = (str) => {
  const perms = parsePerms(str)
  return Object.keys(perms).filter(k => perms[k] !== 'none')
}

/** 把权限对象序列化为新版字符串 "key1:mode,key2:mode,..." */
const serializePerms = (perms) => {
  return Object.entries(perms)
    .filter(([, mode]) => mode === 'edit' || mode === 'read')
    .map(([key, mode]) => `${key}:${mode}`)
    .join(',')
}

// ---- 表格状态 ----
const tableData      = ref([])
const currentPage    = ref(1)
const pageSize       = 5
const searchRoleName = ref('')
const pagedData   = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return tableData.value.slice(start, start + pageSize)
})

// 表单数据（新增/编辑共用）
const form = ref({
  id: null, roleName: '', roleCode: '', remark: '',
  perms: Object.fromEntries(ALL_VALUES.map(k => [k, 'none']))  // 每个菜单的权限：none/read/edit
})
const dialogVisible = ref(false)  // 控制新增/编辑弹窗显示

/** 重置查询条件并刷新列表 */
const handleReset = () => {
  searchRoleName.value = ''
  loadData()
}

const loadData = async () => {
  try {
    const res = await request.get('/api/role/list', {
      params: { roleName: searchRoleName.value }
    })
    tableData.value = res.data || []
    currentPage.value = 1
  } catch (e) { console.error(e) }
}

/** 打开弹窗：新增（row=null）或编辑（row=某行数据） */
const openDialog = (row = null) => {
  if (row) {
    // 编辑：把数据库里的权限字符串解析后填入表单
    form.value = {
      id: row.id, roleName: row.roleName, roleCode: row.roleCode, remark: row.remark || '',
      perms: parsePerms(row.permissions)
    }
  } else {
    resetForm()
  }
  dialogVisible.value = true
}

const resetForm = () => {
  form.value = {
    id: null, roleName: '', roleCode: '', remark: '',
    perms: Object.fromEntries(ALL_VALUES.map(k => [k, 'none']))
  }
}

/**
 * 提交表单：把 perms 对象序列化成权限字符串存入数据库
 * 格式："key1:mode,key2:mode,..."
 * 超管角色不配置权限（permissions=null），默认全部
 */
const handleSubmit = async () => {
  if (!form.value.roleName.trim()) return ElMessage.warning('请输入角色名称')
  if (!form.value.roleCode.trim()) return ElMessage.warning('请输入角色编码')

  const permStr = form.value.roleCode === 'SUPER_ADMIN'
    ? null
    : serializePerms(form.value.perms)

  const payload = {
    id:          form.value.id,
    roleName:    form.value.roleName.trim(),
    roleCode:    form.value.roleCode.trim().toUpperCase(),  // 编码统一大写
    remark:      form.value.remark,
    permissions: permStr
  }

  try {
    const url = form.value.id ? '/api/role/update' : '/api/role/add'
    const res = await request.post(url, payload)
    if (res.code === 200) {
      ElMessage.success(form.value.id ? '修改成功' : '新增成功')
      dialogVisible.value = false; loadData()
    } else { ElMessage.error(res.message || '操作失败') }
  } catch (e) { console.error(e) }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该角色吗？', '提示', { type: 'warning' })
    const res = await request.delete(`/api/role/delete/${id}`)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
    else ElMessage.error(res.message || '删除失败')
  } catch (e) { if (e !== 'cancel') console.error(e) }
}

onMounted(loadData)
</script>

<style scoped>
.pagination-box { display: flex; justify-content: flex-end; margin-top: 16px; }
/* 权限勾选区域 */
.perm-box          { width: 100%; background: #f8f9fa; border: 1px solid #e4e7ed; border-radius: 6px; padding: 12px 14px; }
.perm-header        { display: flex; justify-content: space-between; font-weight: 600; font-size: 13px; color: #606266; padding: 0 8px; }
.perm-header-name   { flex: 1; }
.perm-header-opts   { display: flex; gap: 46px; margin-right: 4px; }
.perm-row           { display: flex; align-items: center; justify-content: space-between; padding: 8px; border-radius: 4px; transition: background 0.15s; }
.perm-row:hover     { background: #eef1f6; }
.perm-name          { flex: 1; font-size: 14px; color: #303133; }
.perm-radio-group   { display: flex; gap: 24px; }
.perm-radio-group :deep(.el-radio__label) { padding-left: 4px; }
.mode-desc          { font-size: 12px; color: #909399; margin-left: 4px; }
.form-tip           { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
