/**
 * 【前端入口文件】main.js —— 整个 Vue 应用的"总开关"
 *
 * 这是前端项目启动时第一个执行的文件，负责：
 *   1. 创建 Vue 应用实例
 *   2. 注册全局插件（路由、UI 组件库）
 *   3. 把 Vue 应用挂载到 index.html 的 <div id="app"> 上
 *
 * 对比后端：就像后端的 Application.java 里的 main() 方法，是整个程序的入口。
 */

import { createApp } from 'vue'      // Vue3 的应用创建函数
import App from './App.vue'           // 根组件，所有页面的"外壳"
import router from './router'         // 路由配置（router/index.js）
import ElementPlus from 'element-plus'          // Element Plus UI 组件库
import 'element-plus/dist/index.css'            // Element Plus 的样式文件，必须引入否则样式丢失
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'  // Element Plus 中文语言包

// 第一步：用根组件 App.vue 创建 Vue 应用实例
const app = createApp(App)

// 第二步：注册路由插件
// 注册后，所有组件里都可以使用 useRouter()、<router-link>、<router-view> 等
app.use(router)

// 第三步：注册 Element Plus UI 组件库（使用中文语言包）
// 注册后，所有组件里都可以直接使用 <el-button>、<el-table>、<el-dialog> 等组件
// 以及 ElMessage、ElMessageBox 等弹窗工具
app.use(ElementPlus, { locale: zhCn })

// 第四步：把 Vue 应用挂载到 index.html 中 id="app" 的 div 元素上
// 挂载后，Vue 接管这个 div 里的所有内容，页面由 Vue 负责渲染
app.mount('#app')
