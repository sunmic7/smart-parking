<template>
  <!--
    登录页整体容器。
    用 CSS 让它铺满整个浏览器窗口（100vh × 100vw），
    作为登录页面的背景层。
  -->
  <div class="login-container">

    <!-- 动态网格背景：用 CSS animation 让网格缓慢移动，营造科技感 -->
    <div class="grid-bg"></div>

    <!-- 流光粒子效果：用 v-for 生成 15 个 li，每个都有不同的大小和动画延迟
         纯 CSS 实现的上升漂浮粒子，不依赖任何 JS 动画库 -->
    <ul class="particles">
      <li v-for="n in 15" :key="n"></li>
    </ul>

    <!-- 登录主体区域：左侧品牌介绍 + 右侧登录卡片 -->
    <div class="login-wrapper">

      <!-- ====== 左侧装饰区 ====== -->
      <div class="login-left">
        <!-- 品牌 Logo 和标题 -->
        <div class="brand">
          <div class="brand-icon">🅿</div>
          <h1 class="brand-title">智能停车管理</h1>
          <p class="brand-sub">Smart Parking Management System</p>
        </div>

        <!-- 系统功能特性列表
             features 数组在 <script> 里定义，v-for 循环渲染每一项 -->
        <div class="feature-list">
          <div class="feature-item" v-for="item in features" :key="item.icon">
            <span class="feature-icon">{{ item.icon }}</span>
            <span>{{ item.text }}</span>
          </div>
        </div>
      </div>

      <!-- ====== 右侧登录卡片 ====== -->
      <div class="login-card">
        <h2 class="card-title">管理员登录</h2>
        <p class="card-sub">ADMINISTRATOR LOGIN</p>

        <!--
          el-form：Element Plus 的表单组件
          :model="loginForm"：把表单绑定到 loginForm 响应式对象
          label-width="0"：不显示标签（用 placeholder 代替）
        -->
        <el-form :model="loginForm" class="login-form" label-width="0">

          <!-- 用户名输入框：v-model 双向绑定 loginForm.username，:prefix-icon 左侧图标，clearable 显示清空按钮 -->
          <el-form-item>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入管理员账号"
              :prefix-icon="User"
              clearable
              class="custom-input"
            />
          </el-form-item>

          <!-- 密码输入框 -->
          <el-form-item>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
              class="custom-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <!-- 验证码输入框 + 图片 -->
          <el-form-item>
            <div class="captcha-row">
              <el-input
                v-model="loginForm.captchaCode"
                placeholder="请输入验证码"
                clearable
                class="custom-input captcha-input"
                @keyup.enter="handleLogin"
              />
              <img
                v-if="captchaImage"
                :src="captchaImage"
                alt="验证码"
                class="captcha-img"
                @click="loadCaptcha"
                title="点击刷新验证码"
              />
              <div v-else class="captcha-img captcha-placeholder" @click="loadCaptcha">点击加载</div>
            </div>
          </el-form-item>

          <!-- 登录按钮：@click 触发 handleLogin，:loading 为 true 时按钮转圈防重复提交 -->
          <el-form-item>
            <el-button
              type="primary"
              class="login-btn"
              @click="handleLogin"
              :loading="loading"
            >
              <!-- 三元表达式：loading 时显示"登录中..."，否则显示"登 录" -->
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>

        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * =====================================================================
 * 【登录页】Login.vue —— 用户身份验证入口
 * =====================================================================
 *
 * 功能：展示登录表单，提交后调用后端 /api/auth/login 接口，
 * 登录成功后把 token 和用户信息存到 localStorage，再跳转到首页。
 *
 * 【Vue3 Composition API 说明】
 * 本项目用的是 Vue3 的 <script setup> 语法（组合式 API），
 * 和 Vue2 的 data()、methods: {} 写法不同：
 *   ref()：创建一个响应式的基本类型数据（数字、字符串、布尔值）
 *   reactive()：创建一个响应式的对象
 *   响应式 = 数据变化时，页面自动更新（不需要手动操作 DOM）
 */




import { reactive, ref, onMounted } from 'vue'        // Vue3 响应式工具函数
import { useRouter } from 'vue-router'     // 路由跳转 hook
import { ElMessage } from 'element-plus'   // 消息提示组件
import { User, Lock } from '@element-plus/icons-vue'  // 图标组件
import request from '../utils/request'    // 封装的 Axios 请求工具

// useRouter()：获取路由实例，用于编程式跳转页面（如 router.push('/layout'))
const router = useRouter()

// loading：控制登录按钮的加载状态
// ref(false) 初始值为 false（不加载）
// 用 .value 读写：loading.value = true
const loading = ref(false)

// loginForm：表单数据对象，双向绑定到输入框
// reactive() 让整个对象具有响应式，对象内任一属性变化都会触发页面更新
const loginForm = reactive({
  username: '',     // 用户名（绑定到用户名输入框）
  password: '',     // 密码（绑定到密码输入框）
  captchaKey: '',   // 验证码 key（后端缓存标识）
  captchaCode: ''   // 用户输入的验证码
})

// 验证码图片地址
const captchaImage = ref('')

// 加载验证码：从后端获取新的验证码 key 和图片地址
const loadCaptcha = async () => {
  try {
    const res = await request.get('/api/captcha')
    if (res.code === 200 && res.data) {
      loginForm.captchaKey = res.data.captchaKey || ''
      captchaImage.value = res.data.captchaImage || ''
      loginForm.captchaCode = ''
    }
  } catch (e) {
    console.error('验证码加载失败：', e)
    ElMessage.error('验证码加载失败，请刷新页面重试')
  }
}

// 页面加载时自动获取验证码
onMounted(loadCaptcha)

// features：左侧装饰区的功能列表数据
// 这是静态数据，不需要响应式，直接用普通数组即可
const features = [
  { icon: '🚗', text: '车辆进出智能识别' },
  { icon: '📊', text: '实时数据统计分析' },
  { icon: '🅿', text: '包月车位精细管理' },
  { icon: '💰', text: '停车费用自动结算' },
]

/**
 * 登录处理函数
 *
 * 执行步骤：
 *   1. 前端校验：用户名和密码不能为空（前端先做初步校验，减少无效请求）
 *   2. 设置 loading = true（按钮变灰，防止重复提交）
 *   3. 调用后端 POST /api/auth/login，传入用户名和密码
 *   4. 登录成功：把 token、用户信息存到 localStorage，跳转到 /layout
 *   5. 登录失败：显示错误信息
 *   6. 无论成功失败：finally 里恢复 loading = false
 *
 * async/await：异步函数写法，让异步代码看起来像同步代码，避免回调地狱。
 *   await request.post(...)：等待请求完成，得到结果后再继续执行。
 */
const handleLogin = async () => {
  // Step 1：前端基础校验
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('账号和密码不能为空！')
    return  // 直接返回，不发请求
  }
  if (!loginForm.captchaCode) {
    ElMessage.warning('请输入验证码！')
    return
  }

  loading.value = true  // 开启 loading，按钮显示转圈

  try {
    // Step 2：调用登录接口
    // request.js 里配置了 baseURL = http://localhost:8080
    // 实际请求：POST http://localhost:8080/api/auth/login
    // loginForm 会被自动序列化为 JSON 请求体：{"username":"admin","password":"123456"}
    const res = await request.post('/api/auth/login', loginForm)

    if (res.code === 200) {
      // Step 3：登录成功，提取返回的用户数据
      const userData = res.data || {}

      // 把所有用户信息存到 localStorage（浏览器本地持久化存储）
      // localStorage 刷新页面、关闭标签页后数据仍然存在，直到主动清除
      // 后续：
      //   token → 放到每次请求的 Authorization 头（request.js 拦截器处理）
      //   username、realName → 放到 X-Username、X-RealName 头（AOP 日志用）
      //   role → Layout.vue 用来判断显示哪些菜单
      localStorage.clear()  // 先清空旧数据，防止残留
      localStorage.setItem('token',    userData.token    || '')
      localStorage.setItem('userId',   userData.userId   || '')
      localStorage.setItem('username', userData.username || '')
      localStorage.setItem('realName', userData.realName || '')
      localStorage.setItem('role',     userData.role     || 'ADMIN')
      localStorage.setItem('nickname', userData.nickname || '')

      ElMessage.success(res.message || res.msg || '登录成功')

      // Step 4：跳转到主页（/layout 会自动重定向到 /layout/dashboard）
      router.push('/layout')

    } else {
      // 后端返回 code 不是 200，说明业务失败（账号不存在、密码错误、验证码错误等）
      ElMessage.error(res.message || res.msg || '登录失败')
      // 刷新验证码，防止暴力破解
      loadCaptcha()
    }

  } catch (error) {
    // 网络错误、后端 500 等异常（request.js 会弹出通用提示，这里做补充处理）
    console.error('登录失败：', error)
    if (error?.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error?.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('登录请求失败，请检查后端是否正常运行')
    }

  } finally {
    // 无论成功还是失败，最终都要关闭 loading
    loading.value = false
  }
}

// 加载验证码函数已在上方定义

</script>

<style scoped>
/*
  scoped：样式只作用于当前组件，不会影响其他页面的同名 class。
  Vue 会自动给本组件的 DOM 元素加一个唯一属性（如 data-v-xxxxxx），
  CSS 选择器也会自动加上这个属性限定范围。
*/

/* 全屏背景容器 */
.login-container {
  height: 100vh; width: 100vw;             /* 铺满视口 */
  display: flex; justify-content: center; align-items: center; /* 垂直水平居中 */
  background: #020b18;                     /* 深夜蓝黑色背景 */
  overflow: hidden;                        /* 粒子超出不显示滚动条 */
  position: relative;                      /* 让粒子的 position:absolute 相对于它定位 */
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 动态网格背景：用 CSS 渐变画出横竖网格线，animation 让它缓慢移动 */
.grid-bg {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(0,200,255,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,200,255,0.04) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: gridMove 20s linear infinite;  /* 无限循环，每 20 秒移动一格 */
}
@keyframes gridMove {
  0%   { background-position: 0 0; }
  100% { background-position: 50px 50px; }  /* 移动一个格子的距离后循环，看起来无限滚动 */
}

/* 粒子容器 */
.particles { position: absolute; inset: 0; list-style: none; margin: 0; padding: 0; pointer-events: none; }
/* 每个粒子：小圆点，用 animation: float 让它从底部浮到顶部 */
.particles li { position: absolute; display: block; background: rgba(0,200,255,0.6); border-radius: 50%; animation: float linear infinite; }
/* 每个粒子不同大小、位置、速度（nth-child 选择器精确控制每一个） */
.particles li:nth-child(1)  { width:4px; height:4px; left:10%; animation-duration:18s; animation-delay:0s; }
.particles li:nth-child(2)  { width:6px; height:6px; left:20%; animation-duration:22s; animation-delay:2s; }
.particles li:nth-child(3)  { width:3px; height:3px; left:30%; animation-duration:15s; animation-delay:4s; }
.particles li:nth-child(4)  { width:5px; height:5px; left:40%; animation-duration:20s; animation-delay:1s; }
.particles li:nth-child(5)  { width:4px; height:4px; left:50%; animation-duration:25s; animation-delay:3s; }
.particles li:nth-child(6)  { width:3px; height:3px; left:60%; animation-duration:17s; animation-delay:5s; }
.particles li:nth-child(7)  { width:6px; height:6px; left:70%; animation-duration:21s; animation-delay:0s; }
.particles li:nth-child(8)  { width:4px; height:4px; left:80%; animation-duration:19s; animation-delay:2s; }
.particles li:nth-child(9)  { width:5px; height:5px; left:90%; animation-duration:23s; animation-delay:4s; }
.particles li:nth-child(10) { width:3px; height:3px; left:15%; animation-duration:16s; animation-delay:6s; }
.particles li:nth-child(11) { width:4px; height:4px; left:25%; animation-duration:24s; animation-delay:1s; }
.particles li:nth-child(12) { width:6px; height:6px; left:35%; animation-duration:18s; animation-delay:3s; }
.particles li:nth-child(13) { width:3px; height:3px; left:55%; animation-duration:20s; animation-delay:5s; }
.particles li:nth-child(14) { width:5px; height:5px; left:75%; animation-duration:22s; animation-delay:2s; }
.particles li:nth-child(15) { width:4px; height:4px; left:85%; animation-duration:26s; animation-delay:0s; }

/* 粒子上升动画：从底部 bottom:-10px 浮到 bottom:110%（超出顶部），同时淡入淡出 */
@keyframes float {
  0%   { bottom:-10px; opacity:0; transform:translateX(0); }
  10%  { opacity:1; }      /* 快速淡入 */
  90%  { opacity:1; }      /* 保持可见 */
  100% { bottom:110%; opacity:0; transform:translateX(30px); }  /* 飘出顶部，稍微偏右 */
}

/* 登录主体：左右两块拼在一起 */
.login-wrapper {
  position: relative; z-index: 10;  /* 在粒子和背景上层显示 */
  display: flex; align-items: center; gap: 0;
  border-radius: 16px; overflow: hidden;
  box-shadow: 0 0 60px rgba(0,200,255,0.15), 0 0 0 1px rgba(0,200,255,0.1);  /* 发光边框效果 */
}

/* 左侧品牌区样式（省略，主要是颜色和布局） */
.login-left { width: 340px; padding: 48px 36px; background: linear-gradient(160deg, #0a1f3c 0%, #061428 100%); border-right: 1px solid rgba(0,200,255,0.15); display: flex; flex-direction: column; gap: 40px; }
.brand { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; }
.brand-icon { font-size: 40px; width: 64px; height: 64px; background: linear-gradient(135deg, #00c8ff, #0066ff); border-radius: 16px; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: bold; box-shadow: 0 4px 20px rgba(0,200,255,0.4); margin-bottom: 6px; }
.brand-title { font-size: 22px; font-weight: 700; color: #e8f4ff; margin: 0; letter-spacing: 2px; }
.brand-sub   { font-size: 11px; color: rgba(0,200,255,0.6); margin: 0; letter-spacing: 1px; }
.feature-list { display: flex; flex-direction: column; gap: 16px; }
.feature-item { display: flex; align-items: center; gap: 12px; color: rgba(200,230,255,0.7); font-size: 14px; }
.feature-icon { font-size: 18px; width: 36px; height: 36px; background: rgba(0,200,255,0.08); border: 1px solid rgba(0,200,255,0.2); border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

/* 右侧登录卡片 */
.login-card { width: 380px; padding: 48px 40px 40px; background: rgba(8,20,40,0.95); }
.card-title { font-size: 26px; font-weight: 700; color: #e8f4ff; margin: 0 0 4px; letter-spacing: 2px; }
.card-sub   { font-size: 11px; color: rgba(0,200,255,0.5); letter-spacing: 3px; margin: 0 0 36px; }

/* 输入框样式覆盖 Element Plus 默认样式
   :deep() 是 Vue scoped 穿透选择器，能修改子组件内部的样式
   因为 el-input 内部的 DOM 是 Element Plus 组件内的，scoped 无法直接影响，需要用 :deep() */
.login-form :deep(.el-input__wrapper) { background: rgba(255,255,255,0.04) !important; border: 1px solid rgba(0,200,255,0.2) !important; border-radius: 8px !important; box-shadow: none !important; height: 46px; transition: border-color 0.3s; }
.login-form :deep(.el-input__wrapper:hover), .login-form :deep(.el-input__wrapper.is-focus) { border-color: rgba(0,200,255,0.6) !important; box-shadow: 0 0 12px rgba(0,200,255,0.15) !important; }
.login-form :deep(.el-input__inner)              { color: #c8e6ff !important; background: transparent !important; font-size: 14px; }
.login-form :deep(.el-input__inner::placeholder) { color: rgba(100,160,200,0.5) !important; }
.login-form :deep(.el-input__prefix-inner .el-icon){ color: rgba(0,200,255,0.6) !important; }
.login-form :deep(.el-input__suffix-inner .el-icon){ color: rgba(0,200,255,0.4) !important; }

/* 登录按钮：渐变色 + 悬停上移效果 */
.login-btn { width: 100%; height: 46px; font-size: 16px; letter-spacing: 4px; border-radius: 8px; background: linear-gradient(90deg, #0066ff, #00c8ff) !important; border: none !important; box-shadow: 0 4px 20px rgba(0,150,255,0.4) !important; transition: all 0.3s !important; margin-top: 4px; }
.login-btn:hover  { transform: translateY(-1px); box-shadow: 0 6px 28px rgba(0,150,255,0.6) !important; }  /* 悬停时上移1px，产生"悬浮"感 */
.login-btn:active { transform: translateY(0); }  /* 点击时回到原位，产生"按下"感 */
.login-form .el-form-item { margin-bottom: 20px; }

/* 验证码行：输入框 + 图片并排 */
.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.captcha-input {
  flex: 1;
}
.captcha-img {
  width: 120px;
  height: 46px;
  border-radius: 8px;
  border: 1px solid rgba(0, 200, 255, 0.3);
  cursor: pointer;
  object-fit: cover;
  background: #fff;
}
.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(200, 230, 255, 0.7);
  font-size: 13px;
  background: rgba(255, 255, 255, 0.06);
}
</style>
