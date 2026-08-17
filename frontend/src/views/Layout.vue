<template>
  <!--
    el-container：Element Plus 的布局容器，自动实现侧边栏 + 主内容区的经典布局
    height: 100vh 让整个布局铺满浏览器高度
  -->
  <el-container class="layout-container">

    <!-- ====== 左侧导航栏 ====== -->
    <!-- el-aside：Element Plus 的侧边栏组件，width="240px" 固定宽度 -->
    <el-aside width="240px" :class="['aside-box', { 'aside-box--mobile': isMobile, 'aside-box--open': sidebarOpen }]">

      <!-- 顶部 Logo 区 -->
      <div class="logo">
        <el-icon class="logo-icon"><OfficeBuilding /></el-icon>
        <span>智能停车管理系统</span>
      </div>

      <!-- 导航菜单列表 -->
      <div class="side-menu">

        <!-- 数据总览（所有角色都能看，不做权限控制） -->
        <div class="menu-group">
          <!--
            router-link：Vue Router 提供的导航组件，点击后跳转到指定路由（不刷新页面）
            custom + v-slot：自定义渲染，让我们用自己的 div 替代默认的 <a> 标签
            isActive：当前路由匹配时为 true，用于高亮当前菜单项
            navigate：调用它可以触发路由跳转（等效于点击 <a> 链接）
          -->
          <router-link to="/layout/dashboard" custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', 'menu-item--overview', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <el-icon style="font-size:15px"><DataLine /></el-icon>
              <span>数据总览</span>
            </div>
          </router-link>
        </div>

        <!--
          车场管理模块（停车场管理 + 包月车管理）
          v-if="hasPerm('park-lot') || hasPerm('monthly-car')"：
          只有当用户有这两个菜单之一的权限时，才渲染这整个分组
          hasPerm() 是下面 script 里定义的权限判断函数
        -->
        <div v-if="hasPerm('park-lot') || hasPerm('monthly-car') || hasPerm('park-lot-map')" class="menu-group">
          <div class="group-title">
            <el-icon><OfficeBuilding /></el-icon>
            <span>车场管理</span>
          </div>
          <!-- 每个子菜单单独判断权限，v-if 控制是否渲染 -->
          <router-link v-if="hasPerm('park-lot')"       to="/layout/park-lot"       custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <span class="item-dot"></span>停车场管理
            </div>
          </router-link>
          <router-link v-if="hasPerm('monthly-car')"    to="/layout/monthly-car"    custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <span class="item-dot"></span>车辆管理（包月）
            </div>
          </router-link>
          <router-link v-if="hasPerm('park-lot-map')"   to="/layout/park-lot-map"   custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <span class="item-dot"></span>停车场地图
            </div>
          </router-link>
        </div>

        <!-- 停车管理模块 -->
        <div v-if="hasPerm('plate-recognize') || hasPerm('record') || hasPerm('payment')" class="menu-group">
          <div class="group-title">
            <el-icon><List /></el-icon>
            <span>停车管理</span>
          </div>
          <router-link v-if="hasPerm('plate-recognize')" to="/layout/plate-recognize" custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <span class="item-dot"></span>车牌识别
            </div>
          </router-link>
          <router-link v-if="hasPerm('record')"          to="/layout/record"          custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <span class="item-dot"></span>停车记录
            </div>
          </router-link>
          <router-link v-if="hasPerm('payment')"         to="/layout/payment"         custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <span class="item-dot"></span>缴费记录
            </div>
          </router-link>
        </div>

        <!--
          系统管理模块（超级管理员始终可见，或有对应权限的普通管理员也可见）
          userRole === 'SUPER_ADMIN' 是额外的 OR 条件，超管不受 hasPerm 限制
        -->
        <div v-if="userRole === 'SUPER_ADMIN' || hasPerm('user') || hasPerm('role') || hasPerm('log')" class="menu-group">
          <div class="group-title">
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </div>
          <router-link v-if="userRole === 'SUPER_ADMIN' || hasPerm('user')" to="/layout/user" custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <span class="item-dot"></span>用户管理
            </div>
          </router-link>
          <router-link v-if="userRole === 'SUPER_ADMIN' || hasPerm('role')" to="/layout/role" custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <span class="item-dot"></span>角色管理
            </div>
          </router-link>
          <router-link v-if="userRole === 'SUPER_ADMIN' || hasPerm('log')" to="/layout/log" custom v-slot="{ navigate, isActive }">
            <div :class="['menu-item', isActive && 'menu-item--active']" @click="navigate(); closeSidebar()">
              <span class="item-dot"></span>日志管理
            </div>
          </router-link>
        </div>

      </div>
    </el-aside>

    <!-- ====== 右侧主区域（顶部 Header + 内容区） ====== -->
    <el-container>

      <!-- 顶部导航栏 -->
      <el-header class="main-header">
        <!-- 左侧：移动端菜单按钮 + 欢迎语 + 角色标签 -->
        <div class="header-left">
          <el-button v-if="isMobile" class="menu-toggle" text @click="toggleSidebar">
            <el-icon :size="22"><Expand v-if="!sidebarOpen" /><Fold v-else /></el-icon>
          </el-button>
          <!-- displayName 是计算属性：优先显示昵称，没有就用真实姓名，再没有才用账号 -->
          <span class="welcome-text">欢迎回来，{{ displayName }}</span>
          <!-- 超级管理员显示红色标签，普通管理员显示蓝色标签 -->
          <el-tag size="small" :type="userRole === 'SUPER_ADMIN' ? 'danger' : 'primary'" class="role-tag">
            {{ userRole === 'SUPER_ADMIN' ? '超级管理员' : '普通管理员' }}
          </el-tag>
        </div>

        <!-- 右侧：下拉菜单（修改密码 + 退出登录） -->
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              个人中心<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <!-- command 值会传给 handleCommand 函数，用来区分点了哪个选项 -->
                <el-dropdown-item command="editPwd">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!--
        内容区：<router-view /> 是路由出口
        子路由（Dashboard、ParkLot 等）的页面内容会渲染在这里
        相当于一个"内容插槽"，切换菜单时只替换这里，侧边栏和顶部不刷新
      -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <!-- 移动端侧边栏遮罩：点击关闭菜单 -->
    <div v-if="isMobile && sidebarOpen" class="sidebar-mask" @click="closeSidebar"></div>

    <!-- 修改密码弹窗（v-model="pwdVisible" 控制显示/隐藏） -->
    <el-dialog title="修改密码" v-model="pwdVisible" width="420px" destroy-on-close>
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px">
        <el-form-item label="当前账户">
          <el-input :model-value="username" disabled />
        </el-form-item>
        <el-form-item label="旧密码" prop="oldPwd">
          <el-input v-model="pwdForm.oldPwd" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="pwdForm.newPwd" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPwd">
          <el-input v-model="pwdForm.confirmPwd" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPwd">确定</el-button>
      </template>
    </el-dialog>

  </el-container>
</template>

<script setup>
/**
 * =====================================================================
 * 【主框架】Layout.vue —— 系统的"壳"，包含侧边菜单、顶部栏、内容区
 * =====================================================================
 *
 * 这个页面在用户登录后始终显示，不会因为切换菜单而消失。
 * 它只负责：
 *   1. 渲染侧边导航菜单（根据权限动态显示/隐藏菜单项）
 *   2. 渲染顶部栏（欢迎语、角色标签、个人中心下拉）
 *   3. 提供 <router-view> 作为子页面的渲染容器
 *   4. 页面加载时拉取当前用户的菜单权限
 */

import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { OfficeBuilding, List, Setting, ArrowDown, DataLine, MapLocation, Fold, Expand } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()

// 从 localStorage 读取登录时存的用户信息
// ref() 让这些变量具有响应式，模板里用到它们时会自动更新
const username = ref(localStorage.getItem('username') || '未知用户')
const realName = ref(localStorage.getItem('realName') || '')
const nickname = ref(localStorage.getItem('nickname') || '')
const userRole = ref(localStorage.getItem('role') || 'ADMIN')

/**
 * 计算属性：显示名称
 * computed() 会缓存结果，只有依赖的响应式数据变化时才重新计算。
 * 优先级：昵称 > 真实姓名 > 用户名（账号），取最友好的那个显示在顶部"欢迎回来"
 */
const displayName = computed(() => nickname.value || realName.value || username.value)

// 移动端侧边栏折叠控制
const isMobile    = ref(false)
const sidebarOpen = ref(false)
const checkScreen = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) sidebarOpen.value = false
}
const toggleSidebar = () => { sidebarOpen.value = !sidebarOpen.value }
const closeSidebar  = () => { if (isMobile.value) sidebarOpen.value = false }

// permKeys：当前用户有权限访问的菜单 key 列表
// 例如：['park-lot', 'monthly-car', 'record']
const permKeys = ref([])

/**
 * 权限判断函数：判断当前用户是否有某个菜单的访问权限
 * 模板里用 v-if="hasPerm('park-lot')" 来控制菜单项是否显示
 *
 * @param {string} key 菜单标识，如 'park-lot'、'record'
 * @returns {boolean} 有权限返回 true，否则 false
 */
const hasPerm = (key) => permKeys.value.includes(key)

/**
 * 从后端加载当前用户的权限列表
 *
 * 超级管理员：直接给全部权限，不需要请求后端
 * 普通管理员：请求 /api/role/permissions 接口，传入角色编码，
 *            后端返回该角色被授权的菜单 key 列表和每个菜单的权限模式（edit/read）
 */
const loadPermissions = async () => {
  const role = userRole.value

  // 超级管理员直接给全部菜单权限
  if (role === 'SUPER_ADMIN') {
    const allKeys = ['park-lot', 'monthly-car', 'park-lot-map', 'plate-recognize', 'record', 'payment', 'user', 'role']
    permKeys.value = allKeys
    const superPerms = Object.fromEntries(allKeys.map(k => [k, 'edit']))
    localStorage.setItem('permKeys', JSON.stringify(allKeys))
    localStorage.setItem('permMap', JSON.stringify(superPerms))
    return
  }

  // 普通管理员：从后端获取权限配置
  try {
    const res = await request.get('/api/role/permissions', { params: { roleCode: role } })
    if (res.code === 200 && res.data) {
      const data = res.data
      const keys = Array.isArray(data.keys) ? data.keys : []
      const perms = data.perms && typeof data.perms === 'object' ? data.perms : {}
      permKeys.value = keys
      localStorage.setItem('permKeys', JSON.stringify(keys))
      localStorage.setItem('permMap', JSON.stringify(perms))
    } else {
      permKeys.value = []
      localStorage.setItem('permKeys', '[]')
      localStorage.setItem('permMap', '{}')
    }
  } catch (e) {
    console.error('获取权限失败：', e)
    permKeys.value = []
    localStorage.setItem('permKeys', '[]')
    localStorage.setItem('permMap', '{}')
  }
}

const pwdVisible = ref(false)                                      // 控制弹窗显示/隐藏
const pwdFormRef = ref(null)
const pwdForm    = ref({ oldPwd: '', newPwd: '', confirmPwd: '' }) // 表单数据
const pwdRules = {
  oldPwd:    [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPwd:    [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPwd) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 处理顶部下拉菜单的点击命令
 * @param {string} command 'logout' 或 'editPwd'
 */
const handleCommand = (command) => {
  if (command === 'logout')  confirmLogout()
  if (command === 'editPwd') {
    pwdForm.value = { oldPwd: '', newPwd: '', confirmPwd: '' }  // 每次打开先清空表单
    pwdVisible.value = true                                      // 显示弹窗
  }
}

/** 退出登录（带二次确认弹窗）*/
const confirmLogout = () => {
  ElMessageBox.confirm('确定要退出系统吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    localStorage.clear()        // 清除所有本地存储（token、用户信息等全清）
    router.push('/login')       // 跳转回登录页
    ElMessage.success('已安全退出')
  }).catch(() => {})             // 点取消不做任何处理
}

/** 提交修改密码 */
const submitPwd = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const res = await request.post('/api/user/updatePassword', {
      username: username.value,
      oldPwd:   pwdForm.value.oldPwd,
      newPwd:   pwdForm.value.newPwd
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      pwdVisible.value = false
      localStorage.clear()    // 密码改了必须重新登录，清除 token
      router.push('/login')
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (e) { console.error(e) }
}

/**
 * onMounted：Vue 生命周期钩子，组件挂载到页面后执行。
 * 这里页面加载完成后立即去加载权限列表，确保菜单渲染正确。
 * （如果放在组件创建前执行，DOM 还没好，可能出问题）
 */
onMounted(() => {
  loadPermissions()
  checkScreen()
  window.addEventListener('resize', checkScreen)
})
</script>

<style scoped>
/* 整体布局铺满屏幕，overflow:hidden 防止出现滚动条 */
.layout-container { height: 100vh; overflow: hidden; }

/* 侧边栏：深蓝黑渐变背景 */
.aside-box { background: linear-gradient(180deg, #0d1b2a 0%, #1a2942 60%, #0d2137 100%); box-shadow: 2px 0 12px rgba(0, 0, 0, 0.5); display: flex; flex-direction: column; overflow: hidden; }
.aside-box--mobile { position: fixed; left: 0; top: 0; bottom: 0; z-index: 1001; transform: translateX(-100%); transition: transform 0.25s ease; }
.aside-box--mobile.aside-box--open { transform: translateX(0); }
.sidebar-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1000; }
.menu-toggle { color: #00d4ff; margin-right: 10px; padding: 4px; }

/* Logo 区：上边框高亮线 */
.logo { height: 64px; display: flex; align-items: center; justify-content: center; gap: 10px; color: #00d4ff; font-size: 16px; font-weight: bold; letter-spacing: 1px; background: rgba(0, 212, 255, 0.08); border-bottom: 1px solid rgba(0, 212, 255, 0.2); flex-shrink: 0; }
.logo-icon { font-size: 22px; }

/* 菜单滚动区 */
.side-menu { flex: 1; overflow-y: auto; padding: 10px 0 20px; }
.side-menu::-webkit-scrollbar { width: 4px; }
.side-menu::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.2); border-radius: 2px; }

/* 菜单分组 */
.menu-group { margin-bottom: 6px; }
.group-title { display: flex; align-items: center; gap: 8px; padding: 10px 20px 8px; font-size: 12px; font-weight: 600; color: rgba(0, 212, 255, 0.7); text-transform: uppercase; letter-spacing: 1.5px; cursor: default; user-select: none; }
.group-title .el-icon { font-size: 14px; }

/* 菜单项：左边框高亮 + 悬停效果 */
.menu-item { display: flex; align-items: center; gap: 10px; padding: 10px 20px 10px 36px; font-size: 14px; color: rgba(200, 220, 240, 0.75); cursor: pointer; transition: all 0.2s; border-left: 3px solid transparent; position: relative; }
.menu-item:hover { color: #fff; background: rgba(0, 212, 255, 0.08); border-left-color: rgba(0, 212, 255, 0.4); }
/* 当前激活的菜单项：左边框变亮蓝色 */
.menu-item--active { color: #00d4ff; background: rgba(0, 212, 255, 0.12); border-left-color: #00d4ff; font-weight: 600; }
.item-dot { width: 5px; height: 5px; border-radius: 50%; background: currentColor; flex-shrink: 0; opacity: 0.6; }
.menu-item--active .item-dot { opacity: 1; }
.menu-item--overview { font-size: 15px; font-weight: 600; color: rgba(0, 212, 255, 0.9); gap: 10px; padding-left: 24px; }
.menu-item--overview .el-icon { flex-shrink: 0; }
.menu-group + .menu-group { border-top: 1px solid rgba(0, 212, 255, 0.08); padding-top: 4px; }

/* 顶部 Header */
.main-header { background: linear-gradient(90deg, #0d1b2a 0%, #1a2942 100%); display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid rgba(0, 212, 255, 0.2); padding: 0 24px; height: 64px; box-sizing: border-box; box-shadow: 0 2px 8px rgba(0,0,0,0.3); }
.header-left  { display: flex; align-items: center; }
.welcome-text { font-size: 15px; color: rgba(200, 220, 240, 0.9); font-weight: 500; }
.role-tag     { margin-left: 12px; }
.header-right { cursor: pointer; }
.el-dropdown-link { display: flex; align-items: center; gap: 4px; color: #00d4ff; font-size: 14px; outline: none; }

/* 内容区：浅灰色背景，独立滚动 */
.main-content { background-color: #f0f4f8; padding: 20px; height: calc(100vh - 64px); overflow-y: auto; box-sizing: border-box; }
</style>
