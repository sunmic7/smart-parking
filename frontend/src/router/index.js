import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },

  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },

  {
    path: '/layout',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/layout/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'park-lot',
        name: 'ParkLot',
        component: () => import('../views/ParkLot.vue')
      },
      {
        path: 'monthly-car',
        name: 'MonthlyCar',
        component: () => import('../views/MonthlyCar.vue')
      },
      {
        path: 'park-lot-map',
        name: 'ParkLotMap',
        component: () => import('../views/ParkLotMap.vue')
      },
      {
        path: 'plate-recognize',
        name: 'PlateRecognize',
        component: () => import('../views/PlateRecognize.vue')
      },
      {
        path: 'record',
        name: 'Record',
        component: () => import('../views/ParkRecord.vue')
      },
      {
        path: 'payment',
        name: 'Payment',
        component: () => import('../views/ParkPayment.vue')
      },
      {
        path: 'user',
        name: 'SysUser',
        component: () => import('../views/SysUser.vue')
      },
      {
        path: 'role',
        name: 'SysRole',
        component: () => import('../views/SysRole.vue')
      },
      // 日志管理路由（新增）
      {
        path: 'log',
        name: 'SysLog',
        component: () => import('../views/SysLog.vue')
      }
    ]
  },

  // 404 兜底路由：匹配所有未定义路径
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录不能进入系统页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.path === '/login') {
    next()
    return
  }

  if (to.path.startsWith('/layout') && !token) {
    next('/login')
    return
  }

  next()
})

export default router