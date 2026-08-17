import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }

    // 将登录用户信息写入请求头，供后端 AOP 切面记录操作日志
    // username 通常是英文/数字，直接写入；realName 可能含中文，必须编码
    const username = localStorage.getItem('username') || ''
    const realName = localStorage.getItem('realName') || ''
    if (username) {
      config.headers['X-Username'] = username
    }
    if (realName) {
      // encodeURIComponent 将中文转为 %XX 格式，规避 ISO-8859-1 限制
      // 后端 AOP 用 URLDecoder.decode(realName, "UTF-8") 还原
      config.headers['X-RealName'] = encodeURIComponent(realName)
    }

    // FormData 上传文件时不设置 Content-Type，让浏览器自动补 multipart/form-data; boundary=...
    // 其他请求统一使用 JSON
    if (!(config.data instanceof FormData)) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }

    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data

    if (res.code !== 200) {
      if (res.code === 401 || res.code === 403) {
        ElMessage.error(res.msg || '登录已失效，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
      } else if (!response.config.silent) {
        ElMessage.error(res.msg || res.message || '请求失败')
      }
      return Promise.reject(res)
    }

    return res
  },
  error => {
    const silent = error.config?.silent
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
      } else if (!silent) {
        if (status === 404) {
          ElMessage.error('接口地址不存在（404）')
        } else if (status === 500) {
          ElMessage.error('后端服务器内部错误（500）')
        } else if (status === 403) {
          ElMessage.error('没有权限访问该接口（403）')
        } else {
          ElMessage.error(`请求失败：${status}`)
        }
      }
    } else if (error.request) {
      if (!silent) ElMessage.error('无法连接到后端，请确认 Spring Boot 已启动')
    } else {
      if (!silent) ElMessage.error('请求配置出错')
    }

    return Promise.reject(error)
  }
)

export default request