<template>
  <div class="dashboard">

    <!-- 顶部标题 + 月份切换控制栏 -->
    <div class="dash-header">
      <div class="dash-title">
        <span class="title-bar"></span>
        <h2>停车场收入总览</h2>
      </div>
      <div class="dash-controls">
        <!--
          月份切换：左箭头往前翻、右箭头往后翻
          isCurrentMonth 为 true 时禁用右箭头（不能查"未来"月份）
        -->
        <el-button-group>
          <el-button size="small" :icon="ArrowLeft"  @click="prevMonth" />
          <el-button size="small" disabled style="min-width:110px;font-weight:600">
            {{ currentYear }} 年 {{ currentMonth }} 月
          </el-button>
          <el-button size="small" :icon="ArrowRight" @click="nextMonth" :disabled="isCurrentMonth" />
        </el-button-group>
        <el-button size="small" type="primary" style="margin-left:10px" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 4 张汇总卡片：总收入、包月收入、临停收入、停车场数量 -->
    <div class="summary-cards">
      <!-- totalIncome/totalMonthly/totalTemp 都是 computed，自动从 chartData 计算 -->
      <div class="s-card s-card--total">
        <div class="s-label">本月总收入</div>
        <div class="s-value">¥ {{ totalIncome.toFixed(2) }}</div>
      </div>
      <div class="s-card s-card--monthly">
        <div class="s-label">包月收入</div>
        <div class="s-value">¥ {{ totalMonthly.toFixed(2) }}</div>
      </div>
      <div class="s-card s-card--temp">
        <div class="s-label">临停收入</div>
        <div class="s-value">¥ {{ totalTemp.toFixed(2) }}</div>
      </div>
      <div class="s-card s-card--lots">
        <div class="s-label">停车场数量</div>
        <div class="s-value">{{ chartData.length }} 个</div>
      </div>
    </div>

    <!-- 柱状图区域（纯 CSS + HTML 实现，不依赖 ECharts 等图表库） -->
    <div class="chart-box" v-loading="loading">

      <!-- 无数据时显示空状态 -->
      <div v-if="chartData.length === 0 && !loading" class="chart-empty">
        <el-empty description="本月暂无收入数据" />
      </div>

      <div v-else class="bar-chart">

        <!--
          Y 轴刻度（yTicks 是 computed，自动计算 4 个均匀分布的刻度值）
          flex-direction: column-reverse 让刻度从下到上排列（小→大）
        -->
        <div class="y-axis">
          <span v-for="tick in yTicks" :key="tick" class="y-tick">¥{{ tick }}</span>
        </div>

        <!-- 图表主体：横向网格线 + 柱子组 -->
        <div class="chart-body">

          <!-- 网格线：position:absolute 覆盖在柱子后面，bottom 按百分比定位 -->
          <div class="grid-lines">
            <div v-for="tick in yTicks" :key="tick" class="grid-line"
                 :style="{ bottom: (tick / yMax * 100) + '%' }" />
          </div>

          <!--
            每个停车场对应一组柱子（pagedData = 当前页数据，每页最多 5 个停车场）
            每组有两根柱子：蓝色=包月收入，绿色=临停收入
          -->
          <div v-for="(item, idx) in pagedData" :key="item.lotId" class="bar-group">
            <div class="bars">

              <!-- 包月收入柱（蓝色）：高度由 barHeight(item.monthlyIncome) 计算 -->
              <div class="bar-wrap">
                <div class="bar bar--monthly"
                     :style="{ height: animated ? barHeight(item.monthlyIncome) : '0px', transitionDelay: animated ? idx * 60 + 'ms' : '0ms' }"
                     @mouseenter="showTip($event, item, 'monthly')"
                     @mouseleave="hideTip">
                  <!-- 柱子顶部显示数值（金额为 0 时不显示，避免遮挡） -->
                  <span v-if="item.monthlyIncome > 0 && animated" class="bar-val">{{ item.monthlyIncome }}</span>
                </div>
              </div>

              <!-- 临停收入柱（绿色） -->
              <div class="bar-wrap">
                <div class="bar bar--temp"
                     :style="{ height: animated ? barHeight(item.tempIncome) : '0px', transitionDelay: animated ? idx * 60 + 'ms' : '0ms' }"
                     @mouseenter="showTip($event, item, 'temp')"
                     @mouseleave="hideTip">
                  <span v-if="item.tempIncome > 0 && animated" class="bar-val">{{ item.tempIncome }}</span>
                </div>
              </div>

            </div>
            <!-- 柱子下方的停车场名称标签 -->
            <div class="bar-label" :title="item.lotName">{{ item.lotName }}</div>
          </div>

        </div>
      </div>

      <!-- 图表底部：图例 + 翻页（停车场多于 5 个时显示翻页） -->
      <div class="chart-footer">
        <div class="legend">
          <span class="legend-item">
            <span class="legend-dot dot--monthly"></span>包月收入
          </span>
          <span class="legend-item">
            <span class="legend-dot dot--temp"></span>临停收入
          </span>
        </div>
        <!-- 翻页：每次显示 5 个停车场，停车场 > 5 个时才出现 -->
        <div class="chart-pagination" v-if="totalPages > 1">
          <el-button size="small" :icon="ArrowLeft"  :disabled="currentPage === 1"         @click="prevPage" />
          <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
          <el-button size="small" :icon="ArrowRight" :disabled="currentPage === totalPages" @click="nextPage" />
        </div>
        <div v-else class="page-info-single" style="font-size:12px;color:#c0c4cc">
          共 {{ chartData.length }} 个停车场
        </div>
      </div>
    </div>

    <!--
      悬浮 Tooltip（鼠标悬停在柱子上时显示）
      v-if="tip.visible"：只有鼠标悬停时才渲染
      position:fixed + tip.x/tip.y：跟随鼠标位置显示
      pointer-events:none：Tooltip 不响应鼠标事件，不会遮挡柱子触发 mouseleave
    -->
    <div v-if="tip.visible" class="bar-tooltip"
         :style="{ left: tip.x + 'px', top: tip.y + 'px' }">
      <div class="tip-name">{{ tip.lotName }}</div>
      <div class="tip-row">
        <span class="tip-label">{{ tip.type === 'monthly' ? '包月收入' : '临停收入' }}</span>
        <span class="tip-val">¥ {{ tip.amount }}</span>
      </div>
      <div class="tip-row">
        <span class="tip-label">总收入</span>
        <span class="tip-val">¥ {{ tip.total }}</span>
      </div>
    </div>

  </div>
</template>

<script setup>
/**
 * 数据总览页Dashboard.vue —— 停车场收入统计柱状图
 * 功能：
 *   1. 展示指定年月内每个停车场的包月收入和临停收入（柱状图）
 *   2. 顶部 4 张汇总卡片（总收入、包月、临停、停车场数量）
 *   3. 可切换月份（前/后翻），不能查未来月份
 *   4. 停车场 > 5 个时图表支持翻页（每页 5 个）
 *   5. 鼠标悬停柱子显示 Tooltip（停车场名、当前类型收入、总收入）
 *
 * 技术亮点：
 *   柱状图完全用纯 HTML + CSS 实现，没有引入 ECharts / Chart.js 等图表库，
 *   减少依赖，加载更快。柱子高度通过 barHeight() 函数动态计算 px 值。
 *
 * 数据来源：GET /api/statistics/revenue?year=2025&month=6
 * 后端返回：[{ lotId, lotName, monthlyIncome, tempIncome, total }, ...]
 */

import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import request from '../utils/request'

// ================================================================
// 月份控制
// ================================================================
const now = new Date()
const currentYear  = ref(now.getFullYear())           // 当前查看的年份
const currentMonth = ref(now.getMonth() + 1)          // 当前查看的月份（1~12）

/**
 * 计算属性：是否是当前月（本月）
 * 用于禁用"下一月"按钮，防止查询未来数据
 */
const isCurrentMonth = computed(() =>
  currentYear.value === now.getFullYear() && currentMonth.value === (now.getMonth() + 1)
)

/** 切换到上一个月（1月→12月，年份-1） */
const prevMonth = () => {
  if (currentMonth.value === 1) { currentYear.value--; currentMonth.value = 12 }
  else currentMonth.value--
  loadData()
}

/** 切换到下一个月（12月→1月，年份+1） */
const nextMonth = () => {
  if (isCurrentMonth.value) return  // 已经是本月，不能再往后
  if (currentMonth.value === 12) { currentYear.value++; currentMonth.value = 1 }
  else currentMonth.value++
  loadData()
}

// ================================================================
// 数据加载
// ================================================================
const loading   = ref(false)
const chartData = ref([])  // 后端返回的停车场收入数组
const animated  = ref(false) // 控制柱子入场动画

/**
 * computed：从 chartData 中汇总计算三个统计值
 * reduce() 是数组累加方法：(累计值, 当前项) => 新的累计值
 * 初始值为 0，遍历每个停车场加上它的对应收入
 */
const totalMonthly = computed(() => chartData.value.reduce((s, i) => s + Number(i.monthlyIncome), 0))
const totalTemp    = computed(() => chartData.value.reduce((s, i) => s + Number(i.tempIncome),    0))
const totalIncome  = computed(() => totalMonthly.value + totalTemp.value)

/** 加载收入统计数据 */
const loadData = async () => {
  loading.value = true
  animated.value = false
  try {
    const res = await request.get('/api/statistics/revenue', {
      params: { year: currentYear.value, month: currentMonth.value }
    })
    // Number() 强制转换：防止后端返回字符串类型导致加法变字符串拼接
    chartData.value = (res.data || []).map(d => ({
      ...d,
      monthlyIncome: Number(d.monthlyIncome) || 0,
      tempIncome:    Number(d.tempIncome)    || 0,
      total:         Number(d.total)         || 0,
    }))
  } catch (e) { console.error(e) }
  finally { loading.value = false }
  await nextTick()
  animated.value = true
}

// ================================================================
// 图表分页（每页显示 5 个停车场）
// ================================================================
const PAGE_SIZE   = 5
const currentPage = ref(1)

// 总页数 = 向上取整(停车场数 ÷ 每页数)，|| 1 防止 0 个停车场时显示 0 页
const totalPages = computed(() => Math.ceil(chartData.value.length / PAGE_SIZE) || 1)

// 当前页的数据切片（和列表分页同理）
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return chartData.value.slice(start, start + PAGE_SIZE)
})

const prevPage = () => { if (currentPage.value > 1)               currentPage.value-- }
const nextPage = () => { if (currentPage.value < totalPages.value) currentPage.value++ }

// watch：监听 chartData 变化，数据刷新后自动回到第 1 页
watch(chartData, () => { currentPage.value = 1 })

// 切换图表分页时重新播放柱子动画
watch(currentPage, async () => {
  animated.value = false
  await nextTick()
  animated.value = true
})

// ================================================================
// 柱子高度计算（Y 轴刻度 + 柱高）
// ================================================================
const CHART_H = 260  // 图表区域高度（px），与 CSS 保持一致

/**
 * Y 轴最大值（yMax）：
 * 找到当前页所有柱子中的最大值，然后向上取整到一个"好看的数"。
 * 比如最大值是 830：
 *   magnitude = 10^(floor(log10(830))) = 10^2 = 100
 *   yMax = ceil(830/100) * 100 = 9 * 100 = 900
 * 这样 Y 轴最大值是整百，刻度更整齐
 */
const yMax = computed(() => {
  const max = Math.max(...pagedData.value.map(d => Math.max(d.monthlyIncome, d.tempIncome)), 10)
  const magnitude = Math.pow(10, Math.floor(Math.log10(max)))
  return Math.ceil(max / magnitude) * magnitude
})

/**
 * Y 轴刻度值（4 个均匀分布的刻度）：
 * 把 yMax 等分成 4 份，生成 4 个刻度：yMax/4, yMax/2, yMax*3/4, yMax
 * Math.round() 取整，避免小数刻度（如 ¥12.5）
 */
const yTicks = computed(() => {
  const step = yMax.value / 4
  return [step, step * 2, step * 3, yMax.value].map(v => Math.round(v))
})

/**
 * 根据数值计算柱子高度（px）
 * 公式：(当前值 / Y轴最大值) × 图表高度
 * 比如值=450，yMax=900，CHART_H=260：高度 = 450/900 × 260 = 130px
 *
 * @param {number} val 该停车场某类型的收入
 * @returns {string} CSS height 值，如 "130px"
 */
const barHeight = (val) => {
  if (!val || yMax.value === 0) return '0px'
  return (val / yMax.value * CHART_H) + 'px'
}

// ================================================================
// 鼠标悬浮 Tooltip
// ================================================================
const tip = ref({ visible: false, x: 0, y: 0, lotName: '', type: '', amount: 0, total: 0 })

/**
 * 显示 Tooltip
 * getBoundingClientRect()：获取柱子元素相对于视口的位置和尺寸
 * Tooltip 定位在柱子顶部中间偏上（- 90px）
 *
 * @param {MouseEvent} e   鼠标事件对象
 * @param {Object}     item 当前停车场数据
 * @param {string}     type 'monthly' 或 'temp'
 */
const showTip = (e, item, type) => {
  const rect = e.target.getBoundingClientRect()
  tip.value = {
    visible: true,
    x: rect.left + rect.width / 2,  // 水平居中
    y: rect.top - 90,               // 柱子顶部再往上 90px
    lotName: item.lotName,
    type,
    amount: type === 'monthly' ? item.monthlyIncome : item.tempIncome,
    total:  item.total
  }
}

/** 隐藏 Tooltip */
const hideTip = () => { tip.value.visible = false }

onMounted(loadData)
</script>

<style scoped>
.dashboard { padding: 24px; min-height: calc(100vh - 64px); box-sizing: border-box; position: relative; }

/* 顶部标题 */
.dash-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.dash-title  { display: flex; align-items: center; gap: 10px; }
/* 标题左侧的蓝色竖条装饰 */
.title-bar   { width: 4px; height: 22px; border-radius: 2px; background: linear-gradient(180deg, #00d4ff, #0080ff); flex-shrink: 0; }
.dash-title h2 { margin: 0; font-size: 20px; font-weight: 700; color: #1a2942; letter-spacing: 0.5px; }
.dash-controls { display: flex; align-items: center; }

.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
@media (max-width: 1200px) { .summary-cards { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px)  { .summary-cards { grid-template-columns: 1fr; } }
.s-card { border-radius: 12px; padding: 20px 24px; color: #fff; box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.s-card--total   { background: linear-gradient(135deg, #0d1b2a, #1a3a5c); }
.s-card--monthly { background: linear-gradient(135deg, #0050a0, #0080d0); }
.s-card--temp    { background: linear-gradient(135deg, #00695c, #00897b); }
.s-card--lots    { background: linear-gradient(135deg, #4a148c, #7b1fa2); }
.s-label { font-size: 13px; opacity: 0.8; margin-bottom: 8px; }
.s-value { font-size: 26px; font-weight: 700; letter-spacing: 0.5px; }

/* 图表容器 */
.chart-box  { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); min-height: 360px; }
.chart-empty { display: flex; justify-content: center; align-items: center; height: 300px; }

/* 柱状图整体：Y 轴 + 图表主体 横向排列 */
.bar-chart { display: flex; align-items: stretch; height: 300px; }

/* Y 轴刻度：从下到上排列（column-reverse），靠右对齐 */
.y-axis   { display: flex; flex-direction: column-reverse; justify-content: space-between; width: 64px; flex-shrink: 0; padding-bottom: 40px; padding-right: 8px; }
.y-tick   { font-size: 11px; color: #909399; text-align: right; line-height: 1; }

/* 图表主体：相对定位（网格线绝对定位在它里面），底部留 40px 放标签 */
.chart-body { flex: 1; position: relative; padding-bottom: 40px; border-left: 1px solid #ebeef5; border-bottom: 1px solid #ebeef5; display: flex; align-items: flex-end; overflow-x: auto; }

/* 横向网格线：绝对定位，底部百分比定位（和 Y 轴刻度对齐） */
.grid-lines { position: absolute; inset: 0; pointer-events: none; }
.grid-line  { position: absolute; left: 0; right: 0; height: 1px; background: rgba(0,0,0,0.06); }

/* 每个停车场的柱子组 */
.bar-group { flex: 1; min-width: 80px; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; position: relative; padding: 0 6px; }
.bars      { display: flex; align-items: flex-end; gap: 4px; width: 100%; justify-content: center; }
.bar-wrap  { display: flex; align-items: flex-end; }

/* 柱子：transition 让高度变化有动画效果（0.6s 弹性曲线） */
.bar { width: 28px; border-radius: 4px 4px 0 0; transition: height 0.6s cubic-bezier(0.34, 1.56, 0.64, 1); position: relative; cursor: pointer; min-height: 0; }
.bar:hover { filter: brightness(1.1); }  /* 悬浮变亮 */
.bar--monthly { background: linear-gradient(180deg, #36a3f7, #0050a0); }  /* 蓝色渐变 */
.bar--temp    { background: linear-gradient(180deg, #2ecda7, #00695c); }  /* 绿色渐变 */

/* 柱子顶部的数值（position:absolute，垂直偏移到柱子上方） */
.bar-val { position: absolute; top: -18px; left: 50%; transform: translateX(-50%); font-size: 10px; color: #606266; white-space: nowrap; }

/* 停车场名称标签（绝对定位到柱子下方） */
.bar-label { position: absolute; bottom: -36px; font-size: 12px; color: #606266; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; text-align: center; padding: 0 4px; }

/* 图例 + 翻页行 */
.chart-footer     { display: flex; align-items: center; justify-content: space-between; margin-top: 20px; flex-wrap: wrap; gap: 10px; }
.legend           { display: flex; gap: 24px; }
.chart-pagination { display: flex; align-items: center; gap: 8px; }
.page-info        { font-size: 13px; color: #606266; min-width: 48px; text-align: center; }
.legend-item      { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #606266; }
.legend-dot       { width: 12px; height: 12px; border-radius: 2px; flex-shrink: 0; }
.dot--monthly     { background: linear-gradient(135deg, #36a3f7, #0050a0); }
.dot--temp        { background: linear-gradient(135deg, #2ecda7, #00695c); }

/* 悬浮 Tooltip：fixed 定位跟随鼠标，pointer-events:none 不拦截鼠标事件 */
.bar-tooltip { position: fixed; background: rgba(20, 30, 48, 0.92); color: #fff; border-radius: 8px; padding: 10px 14px; font-size: 13px; pointer-events: none; transform: translateX(-50%); z-index: 9999; box-shadow: 0 4px 16px rgba(0,0,0,0.3); min-width: 140px; }
.tip-name    { font-weight: 600; margin-bottom: 6px; color: #00d4ff; font-size: 13px; }
.tip-row     { display: flex; justify-content: space-between; gap: 16px; line-height: 1.8; }
.tip-label   { opacity: 0.7; }
.tip-val     { font-weight: 600; }
</style>
