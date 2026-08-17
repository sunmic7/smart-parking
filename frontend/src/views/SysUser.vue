<template>
  <PageLayout title="用户管理">
    <template #actions>
      <!-- 新增按钮：此页面所有人都可以新增（超管页面，不做权限细分） -->
      <el-button type="success" @click="openAddDialog">新增管理员</el-button>
    </template>

    <template #search>
      <!-- 多条件组合查询：账号、联系人、联系电话、角色 -->
      <el-input v-model="searchUsername" placeholder="请输入账号" clearable
        style="width: 160px" @keyup.enter="loadData" @clear="loadData" />
      <el-input v-model="searchRealName" placeholder="请输入联系人" clearable
        style="width: 160px" @keyup.enter="loadData" @clear="loadData" />
      <el-input v-model="searchPhone" placeholder="请输入联系电话" clearable
        style="width: 160px" @keyup.enter="loadData" @clear="loadData" />
      <el-select v-model="searchRole" placeholder="选择角色" clearable
        style="width: 150px" @change="loadData">
        <el-option v-for="r in roleList" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </template>

    <el-table :data="pagedData" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="id"       label="ID"     width="70" />
      <el-table-column prop="username" label="账号"   width="140" />
      <el-table-column prop="realName" label="联系人" width="140" />
      <el-table-column prop="phone"    label="联系电话" width="150" />
      <!-- 角色列：roleMap 把 roleCode 转成中文名；超管显示红色，普通显示蓝色 -->
      <el-table-column label="角色" width="160">
        <template #default="scope">
          <el-tag :type="scope.row.role === 'SUPER_ADMIN' ? 'danger' : 'primary'">
            {{ roleMap[scope.row.role] || scope.row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <!-- 账号状态：1=正常，0=停用 -->
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
          <!-- 只有超管才能重置别人的密码 -->
          <el-button v-if="isSuperAdmin" type="warning" size="small"
            @click="openResetPwdDialog(scope.row)">重置密码</el-button>
          <!-- id=1 通常是超管自己，禁止删除 :disabled 防止误操作 -->
          <el-button type="danger" size="small" :disabled="scope.row.id === 1"
            @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-box">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize"
        layout="total, prev, pager, next" :total="tableData.length" />
    </div>

    <!-- 新增管理员弹窗 -->
    <el-dialog v-model="addDialogVisible" title="新增管理员" width="520px">
      <el-form :model="addForm" :rules="addFormRules" ref="addFormRef" label-width="100px">
        <el-form-item label="账号" prop="username"><el-input v-model="addForm.username" placeholder="请输入登录账号" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="addForm.password" show-password placeholder="请输入初始密码" /></el-form-item>
        <el-form-item label="联系人" prop="realName"><el-input v-model="addForm.realName" placeholder="选填" /></el-form-item>
        <el-form-item label="联系电话" prop="phone"><el-input v-model="addForm.phone" placeholder="选填" /></el-form-item>
        <!-- 角色从后端动态拉取，选择后同步写入 roleCode -->
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="addForm.roleId" placeholder="请选择角色" style="width:100%"
            @change="onRoleChange(addForm)">
            <el-option v-for="r in roleList" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑管理员弹窗：账号不可修改，可改联系人、电话、角色、状态 -->
    <el-dialog v-model="editDialogVisible" title="编辑管理员" width="520px">
      <el-form :model="editForm" :rules="editFormRules" ref="editFormRef" label-width="100px">
        <!-- 账号禁止修改（disabled），防止用户误改账号 -->
        <el-form-item label="账号"><el-input v-model="editForm.username" disabled /></el-form-item>
        <el-form-item label="联系人" prop="realName"><el-input v-model="editForm.realName" /></el-form-item>
        <el-form-item label="联系电话" prop="phone"><el-input v-model="editForm.phone" /></el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="editForm.roleId" placeholder="请选择角色" style="width:100%"
            @change="onRoleChange(editForm)">
            <el-option v-for="r in roleList" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">正常</el-radio>
            <!-- 停用后该用户登录时会被后端拒绝（LoginController 检查 status） -->
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 超管重置密码弹窗（不需要旧密码，直接强制修改） -->
    <el-dialog v-model="resetPwdDialogVisible" title="重置用户密码" width="420px">
      <el-form :model="resetPwdForm" :rules="resetPwdRules" ref="resetPwdFormRef" label-width="100px">
        <el-form-item label="账号">
          <span style="font-size:14px;font-weight:500;color:#303133">{{ resetPwdForm.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="resetPwdForm.newPwd" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="resetPwdForm.confirmPwd" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwd">确定重置</el-button>
      </template>
    </el-dialog>
  </PageLayout>
</template>

<script setup>
/**
 * 【用户管理页】SysUser.vue
 *
 * 管理系统登录账户（sys_user 表）：
 *   1. 查询所有管理员账号列表
 *   2. 新增管理员（含角色分配）
 *   3. 编辑管理员信息（真实姓名、角色、状态）
 *   4. 超管重置任意用户密码（不需要旧密码）
 *   5. 删除管理员（id=1 超管不可删）
 *
 * 角色是动态的（从 /api/user/roles 获取），不是写死的。
 * roleMap 是 computed 属性，把角色编码映射为中文名，用于表格展示。
 */

import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import PageLayout from '../components/PageLayout.vue'

const isSuperAdmin = localStorage.getItem('role') === 'SUPER_ADMIN'

// ---- 角色列表（动态拉取，用于下拉选择和表格展示） ----
const roleList = ref([])  // [{ id, roleName, roleCode, ... }]

/**
 * 计算属性：roleCode → 中文名 的映射对象
 * 例如：{ 'SUPER_ADMIN': '超级管理员', 'ADMIN': '普通管理员' }
 * 表格列里用 roleMap[scope.row.role] 取中文名
 */
const roleMap = computed(() => {
  const map = {}
  roleList.value.forEach(r => { map[r.roleCode] = r.roleName })
  return map
})

/**
 * 选择角色时同步把 roleCode 写到 form.role
 * 因为数据库里存的是 roleCode（字符串），
 * 但下拉框绑定的是 roleId（数字），需要手动同步
 */
const onRoleChange = (form) => {
  const found = roleList.value.find(r => r.id === form.roleId)
  if (found) form.role = found.roleCode
}

const loadRoles = async () => {
  try {
    const res = await request.get('/api/user/roles')
    if (res.code === 200) roleList.value = res.data || []
  } catch (e) { console.error('获取角色列表失败', e) }
}

// ---- 用户列表 ----
const tableData      = ref([])
const loading        = ref(false)
const currentPage    = ref(1)
const pageSize       = 5
const pagedData      = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return tableData.value.slice(start, start + pageSize)
})
const searchUsername = ref('')
const searchRealName = ref('')
const searchPhone    = ref('')
const searchRole     = ref('')

/** 重置查询条件并刷新列表 */
const handleReset = () => {
  searchUsername.value = ''
  searchRealName.value = ''
  searchPhone.value = ''
  searchRole.value = ''
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      username: searchUsername.value,
      realName: searchRealName.value,
      phone: searchPhone.value,
      role: searchRole.value || undefined
    }
    const res = await request.get('/api/user/list', { params })
    tableData.value = res.data || []
    currentPage.value = 1
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

// ---- 新增 ----
const addDialogVisible = ref(false)
const addFormRef = ref(null)
const addForm = ref({ username: '', password: '', realName: '', phone: '', role: 'ADMIN', roleId: 2 })
const addFormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  roleId:   [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const openAddDialog = () => {
  // 默认选第一个非超管角色
  const defaultRole = roleList.value.find(r => r.roleCode !== 'SUPER_ADMIN') || roleList.value[0]
  addForm.value = {
    username: '', password: '', realName: '', phone: '',
    roleId: defaultRole ? defaultRole.id       : 2,
    role:   defaultRole ? defaultRole.roleCode : 'ADMIN'
  }
  addDialogVisible.value = true
}

const handleAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const res = await request.post('/api/user/add', addForm.value)
    if (res.code === 200) {
      ElMessage.success('新增管理员成功')
      addDialogVisible.value = false; loadData()
    } else { ElMessage.error(res.message || '新增失败') }
  } catch (e) { console.error(e) }
}

// ---- 编辑 ----
const editDialogVisible = ref(false)
const editFormRef = ref(null)
const editForm = ref({ id: null, username: '', realName: '', phone: '', role: 'ADMIN', roleId: 2, status: 1 })
const editFormRules = {
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const openEditDialog = (row) => {
  editForm.value = {
    id: row.id, username: row.username,
    realName: row.realName || '', phone: row.phone || '',
    role: row.role, roleId: row.roleId, status: row.status
  }
  editDialogVisible.value = true
}

const handleUpdate = async () => {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const res = await request.post('/api/user/update', editForm.value)
    if (res.code === 200) {
      ElMessage.success('修改成功'); editDialogVisible.value = false; loadData()
    } else { ElMessage.error(res.message || '修改失败') }
  } catch (e) { console.error(e) }
}

// ---- 删除 ----
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该管理员吗？', '提示', { type: 'warning' })
    const res = await request.delete(`/api/user/delete/${id}`)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
    else ElMessage.error(res.message || '删除失败')
  } catch (e) { console.error(e) }
}

// ---- 超管重置密码 ----
const resetPwdDialogVisible = ref(false)
const resetPwdFormRef = ref(null)
const resetPwdForm = ref({ userId: null, username: '', newPwd: '', confirmPwd: '' })
const resetPwdRules = {
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetPwdForm.value.newPwd) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const openResetPwdDialog = (row) => {
  resetPwdForm.value = { userId: row.id, username: row.username, newPwd: '', confirmPwd: '' }
  resetPwdDialogVisible.value = true
}

const handleResetPwd = async () => {
  const valid = await resetPwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const { userId, newPwd } = resetPwdForm.value
    const res = await request.post('/api/user/resetPassword', { userId: String(userId), newPwd })
    if (res.code === 200) {
      ElMessage.success(`用户 ${resetPwdForm.value.username} 的密码已重置`)
      resetPwdDialogVisible.value = false
    } else { ElMessage.error(res.message || '重置失败') }
  } catch (e) { console.error(e) }
}

onMounted(() => { loadRoles(); loadData() })
</script>

<style scoped>
.pagination-box { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
