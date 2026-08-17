<template>
  <div class="map-page">
    <!-- 左侧停车场列表 -->
    <div :class="['lot-sidebar', sidebarCollapsed && 'lot-sidebar--collapsed']">
      <div class="sidebar-header">
        <template v-if="!sidebarCollapsed">
          <el-icon><OfficeBuilding /></el-icon>
          <span>停车场列表</span>
        </template>
        <el-button
          class="collapse-btn"
          link
          :icon="sidebarCollapsed ? Expand : Fold"
          @click="toggleSidebar"
          :title="sidebarCollapsed ? '展开' : '收起'"
        />
      </div>
      <div v-show="!sidebarCollapsed" class="sidebar-body">
        <el-input
          v-model="searchText"
          placeholder="搜索停车场"
          clearable
          size="small"
          :prefix-icon="Search"
          class="sidebar-search"
        />
        <div class="lot-list" v-loading="loading">
          <div
            v-for="lot in filteredLots"
            :key="lot.id"
            :class="['lot-item', selectedLot && selectedLot.id === lot.id && 'lot-item--active']"
            @click="selectLot(lot)"
          >
            <div class="lot-info">
              <div class="lot-name">{{ lot.lotName }}</div>
              <div class="lot-address">{{ lot.address || '暂无地址' }}</div>
            </div>
            <el-tag size="small" :type="hasLocation(lot) ? 'success' : 'info'">
              {{ hasLocation(lot) ? '已标注' : '未标注' }}
            </el-tag>
          </div>
          <el-empty v-if="!filteredLots.length" description="无数据" :image-size="60" />
        </div>
      </div>
    </div>

    <!-- 右侧地图区域 -->
    <div class="map-main">
      <!-- 地图搜索框 -->
      <div class="map-search-box">
        <el-input
          v-model="mapKeyword"
          placeholder="搜索地点，如：黄山学院"
          clearable
          size="small"
          @keyup.enter="handleMapSearch"
          @clear="searchResults = []"
        />
        <el-button type="primary" size="small" :icon="Search" :loading="searching" @click="handleMapSearch">搜索</el-button>
        <el-button title="定位到我的位置" size="small" :icon="Location" @click="locateMe">定位</el-button>

        <!-- 搜索结果下拉 -->
        <div v-if="searchResults.length" class="search-result-list">
          <div
            v-for="poi in searchResults"
            :key="poi.id"
            class="search-result-item"
            @click="goToSearchResult(poi)"
          >
            <div class="result-name">{{ poi.name }}</div>
            <div class="result-address">{{ poi.address || '暂无地址' }}</div>
          </div>
        </div>
      </div>

      <div id="amap-container" class="amap-container"></div>

      <div v-if="selectedLot" class="selected-card">
        <div class="selected-title">{{ selectedLot.lotName }}</div>
        <div class="selected-row">
          <span class="selected-label">地址</span>
          <span class="selected-value">{{ selectedLot.address || '—' }}</span>
        </div>
        <div class="selected-row">
          <span class="selected-label">经度</span>
          <span class="selected-value">{{ fmtCoord(selectedLot.longitude) }}</span>
        </div>
        <div class="selected-row">
          <span class="selected-label">纬度</span>
          <span class="selected-value">{{ fmtCoord(selectedLot.latitude) }}</span>
        </div>
        <div class="selected-actions">
          <el-button size="small" @click="clearSelected">取消</el-button>
          <el-button v-if="editAllowed" size="small" type="primary" :loading="saving" @click="saveLocation">保存坐标</el-button>
        </div>
        <div class="selected-tip">
          {{ editAllowed ? '在地图上点击即可重新标注该停车场位置' : '当前账号仅有查看权限，无法标注或修改坐标' }}
        </div>
      </div>

      <!-- 图例 -->
      <div class="map-legend">
        <div class="legend-item">
          <span class="legend-dot legend-dot--located"></span>
          <span>已标注</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot legend-dot--selected"></span>
          <span>当前选中</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 【停车场地图页】ParkLotMap.vue
 *
 * 功能：
 *   1. 在左侧展示所有停车场列表
 *   2. 右侧接入高德地图，显示已标注停车场的 Marker
 *   3. 选中停车场后，可在地图上点击进行标注/重新标注
 *   4. 保存经纬度到后端，不实现导航功能
 *
 * 依赖：
 *   - 高德地图 JSAPI 2.0（通过 script 动态加载）
 *   - 环境变量 VITE_AMAP_KEY 配置高德 Key
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Search, OfficeBuilding, Fold, Expand, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { canEdit } from '../utils/permission'

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY || ''

const editAllowed      = canEdit('park-lot-map')  // 是否有编辑（标注）权限
const loading          = ref(false)
const saving           = ref(false)
const lotList          = ref([])
const searchText       = ref('')
const selectedLot      = ref(null)
const sidebarCollapsed = ref(false)
const mapKeyword       = ref('')       // 地图 POI 搜索关键词
const searchResults    = ref([])       // 地图搜索结果
const searching        = ref(false)    // 搜索中状态

// 高德地图相关实例
let mapInstance   = null
let markerLayer   = []
let activeMarker  = null
let infoWindow    = null
let geocoder      = null
let placeSearch   = null
let geolocation   = null
let searchMarker  = null
let locMarker     = null
let mapClickEvent = null

const filteredLots = computed(() => {
  if (!searchText.value.trim()) return lotList.value
  const kw = searchText.value.trim().toLowerCase()
  return lotList.value.filter(l => l.lotName && l.lotName.toLowerCase().includes(kw))
})

const hasLocation = (lot) => lot && lot.longitude != null && lot.latitude != null

/** 收起/展开左侧列表 */
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  // 侧边栏动画过渡 300ms 后通知地图重新计算尺寸
  setTimeout(() => {
    mapInstance && mapInstance.resize()
  }, 300)
}

/** 地图 POI 搜索，无结果时回退到本地停车场名称搜索 */
const handleMapSearch = () => {
  const kw = mapKeyword.value.trim()
  if (!kw) return

  // 1. 先尝试本地停车场名称匹配
  const localMatch = lotList.value.find(l => l.lotName && l.lotName.includes(kw))
  if (localMatch && hasLocation(localMatch)) {
    selectLot(localMatch)
    ElMessage.success(`已定位到停车场：${localMatch.lotName}`)
    return
  }

  // 2. 再调用高德 POI 搜索
  if (!placeSearch) {
    ElMessage.warning('地图搜索插件尚未加载完成，请稍后再试')
    return
  }
  searching.value = true
  searchResults.value = []
  placeSearch.search(kw, (status, result) => {
    searching.value = false
    console.log('PlaceSearch status:', status, 'result:', result)
    if (status === 'complete' && result && result.info === 'OK' && result.poiList) {
      searchResults.value = result.poiList.pois || []
      if (!searchResults.value.length) {
        ElMessage.warning('未搜索到相关地点')
      }
    } else {
      searchResults.value = []
      const msg = (result && result.info) || status || '未知错误'
      ElMessage.warning('搜索失败：' + msg)
    }
  })
}

/** 点击搜索结果，地图飞到该位置 */
const goToSearchResult = (poi) => {
  if (!mapInstance || !window.AMap || !poi.location) return
  const lnglat = [poi.location.lng, poi.location.lat]
  mapInstance.setZoomAndCenter(16, lnglat)

  if (searchMarker) searchMarker.setMap(null)
  searchMarker = new window.AMap.Marker({
    position: lnglat,
    title: poi.name,
    label: { content: `<div class="amap-marker-label">${poi.name}</div>`, offset: new window.AMap.Pixel(0, -28) }
  })
  searchMarker.setMap(mapInstance)
  searchResults.value = []
  mapKeyword.value = poi.name
}

/** 定位到当前位置 */
const locateMe = () => {
  if (!geolocation) {
    ElMessage.warning('定位插件尚未加载完成，请稍后再试')
    return
  }
  geolocation.getCurrentPosition((status, result) => {
    if (status === 'complete') {
      const lnglat = [result.position.lng, result.position.lat]
      mapInstance.setZoomAndCenter(16, lnglat)

      if (locMarker) locMarker.setMap(null)
      locMarker = new window.AMap.Marker({
        position: lnglat,
        title: '当前位置',
        label: { content: '<div class="amap-marker-label">当前位置</div>', offset: new window.AMap.Pixel(0, -28) }
      })
      locMarker.setMap(mapInstance)
      ElMessage.success('已定位到当前位置')
    } else {
      ElMessage.error(result.message || '定位失败，请检查浏览器位置权限')
    }
  })
}

const fmtCoord = (val) => {
  if (val == null || val === '') return '—'
  return Number(val).toFixed(6)
}

/** 加载高德地图插件（Promise 封装） */
const loadAMapPlugins = (AMap, plugins) => {
  return new Promise((resolve, reject) => {
    AMap.plugin(plugins, (err) => {
      if (err) reject(new Error('高德插件加载失败：' + (err.message || err)))
      else resolve()
    })
  })
}

/** 动态加载高德地图 JSAPI */
const loadAMapScript = () => {
  return new Promise((resolve, reject) => {
    if (window.AMap && window.AMap.Map) {
      resolve(window.AMap)
      return
    }
    if (!AMAP_KEY) {
      reject(new Error('未配置高德地图 Key，请在 .env 中设置 VITE_AMAP_KEY'))
      return
    }
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}`
    script.onerror = () => reject(new Error('高德地图脚本加载失败'))
    script.onload = () => {
      // 2.0 加载完成后 AMap 已挂载到 window
      if (window.AMap) resolve(window.AMap)
      else reject(new Error('高德地图初始化失败'))
    }
    document.head.appendChild(script)
  })
}

/** 初始化地图 */
const initMap = async () => {
  try {
    const AMap = await loadAMapScript()

    mapInstance = new AMap.Map('amap-container', {
      zoom: 12,
      center: [115.779, 33.844]  // 默认中心：亳州市
    })

    // 同时加载所需插件：逆地理编码、POI 搜索、浏览器定位
    try {
      await loadAMapPlugins(AMap, ['AMap.Geocoder', 'AMap.PlaceSearch', 'AMap.Geolocation'])
      geocoder = new AMap.Geocoder({ radius: 1000 })
      placeSearch = new AMap.PlaceSearch({ pageSize: 10, pageIndex: 1 })
      geolocation = new AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 10000,
        buttonOffset: new AMap.Pixel(10, 20),
        zoomToAccuracy: true
      })
    } catch (pluginErr) {
      console.error(pluginErr)
      ElMessage.warning('部分地图插件加载失败，搜索/定位功能可能不可用')
    }

    mapClickEvent = mapInstance.on('click', (e) => {
      if (!editAllowed) return
      if (!selectedLot.value) {
        ElMessage.info('请先从左侧选择要标注的停车场')
        return
      }
      const lng = e.lnglat.getLng()
      const lat = e.lnglat.getLat()
      updateSelectedLocation(lng, lat)
    })
  } catch (err) {
    ElMessage.error(err.message || '地图加载失败')
  }
}

/** 加载停车场列表 */
const fetchLots = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/park-lot/list')
    if (res.code === 200) {
      lotList.value = res.data || []
      renderMarkers()
    } else {
      ElMessage.error(res.message || '获取停车场失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取停车场失败')
  } finally {
    loading.value = false
  }
}

/** 渲染所有已标注停车场的 Marker */
const renderMarkers = () => {
  if (!mapInstance || !window.AMap) return

  // 清除旧 Marker
  markerLayer.forEach(m => m.setMap(null))
  markerLayer = []

  lotList.value.forEach(lot => {
    if (!hasLocation(lot)) return
    const marker = createMarker(lot, false)
    marker.setMap(mapInstance)
    markerLayer.push(marker)
  })
}

/** 创建 Marker */
const createMarker = (lot, isActive) => {
  const AMap = window.AMap
  const marker = new AMap.Marker({
    position: [Number(lot.longitude), Number(lot.latitude)],
    title: lot.lotName,
    animation: isActive ? 'AMAP_ANIMATION_BOUNCE' : 'AMAP_ANIMATION_NONE',
    label: {
      content: `<div class="amap-marker-label">${lot.lotName}</div>`,
      offset: new AMap.Pixel(0, -28)
    }
  })

  marker.on('click', () => {
    selectLot(lot)
  })

  return marker
}

/** 选中左侧某个停车场 */
const selectLot = (lot) => {
  selectedLot.value = { ...lot }  // 复制一份，避免直接修改原数据

  if (!mapInstance || !window.AMap) return

  // 移除之前的高亮 Marker
  if (activeMarker) {
    activeMarker.setMap(null)
    activeMarker = null
  }

  if (hasLocation(lot)) {
    const center = [Number(lot.longitude), Number(lot.latitude)]
    mapInstance.setCenter(center)
    mapInstance.setZoom(13)  // 选中停车场后保持市级视野，不要缩放到过大
    activeMarker = createMarker(lot, true)
    activeMarker.setMap(mapInstance)
    openInfoWindow(lot, center)
  } else {
    // 未标注时打开提示信息窗
    ElMessage.warning('该停车场尚未标注位置，点击地图可进行标注')
    infoWindow && infoWindow.close()
  }
}

/** 打开信息窗体 */
const openInfoWindow = (lot, position) => {
  const AMap = window.AMap
  if (!infoWindow) {
    infoWindow = new AMap.InfoWindow({
      offset: new AMap.Pixel(0, -30),
      closeWhenClickMap: true
    })
  }
  const rate = lot.totalSpaces > 0
    ? Math.round((lot.usedSpaces / lot.totalSpaces) * 100)
    : 0
  infoWindow.setContent(`
    <div style="padding:6px 8px;font-size:13px;min-width:140px">
      <div style="font-weight:600;margin-bottom:6px">${lot.lotName}</div>
      <div style="color:#666">${lot.address || '暂无地址'}</div>
      <div style="margin-top:6px">车位：${lot.usedSpaces || 0} / ${lot.totalSpaces || 0}（${rate}%）</div>
    </div>
  `)
  infoWindow.open(mapInstance, position)
}

/** 点击地图后更新当前选中停车场的坐标 */
const updateSelectedLocation = (lng, lat) => {
  if (!selectedLot.value) return
  selectedLot.value.longitude = lng
  selectedLot.value.latitude = lat

  if (!mapInstance || !window.AMap) return

  // 更新或创建高亮 Marker
  if (activeMarker) activeMarker.setMap(null)
  activeMarker = createMarker(selectedLot.value, true)
  activeMarker.setMap(mapInstance)
  mapInstance.setCenter([lng, lat])

  // 逆地理编码回填地址
  if (geocoder) {
    geocoder.getAddress([lng, lat], (status, result) => {
      if (status === 'complete' && result.regeocode) {
        selectedLot.value.address = result.regeocode.formattedAddress
      }
    })
  }
}

/** 保存坐标到后端 */
const saveLocation = async () => {
  if (!selectedLot.value) return
  const { id, longitude, latitude, address } = selectedLot.value
  if (longitude == null || latitude == null) {
    ElMessage.warning('请先在地图上点击标注位置')
    return
  }
  saving.value = true
  try {
    const res = await request.put('/api/park-lot/update-location', {
      id,
      longitude,
      latitude,
      address
    })
    if (res.code === 200) {
      ElMessage.success('坐标保存成功')
      // 更新本地列表数据
      const idx = lotList.value.findIndex(l => l.id === id)
      if (idx !== -1) {
        lotList.value[idx] = { ...selectedLot.value }
      }
      renderMarkers()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const clearSelected = () => {
  selectedLot.value = null
  if (activeMarker) {
    activeMarker.setMap(null)
    activeMarker = null
  }
  infoWindow && infoWindow.close()
  renderMarkers()
}

onMounted(() => {
  initMap()
  fetchLots()
})

onUnmounted(() => {
  if (mapInstance) {
    mapClickEvent && mapInstance.off('click', mapClickEvent)
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style scoped>
.map-page {
  display: flex;
  height: calc(100vh - 104px);  /* 减去顶部 header + 页面 padding */
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* 左侧列表 */
.lot-sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e4e7ed;
  transition: width 0.3s ease;
  overflow: hidden;
}
.lot-sidebar--collapsed {
  width: 48px;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
  white-space: nowrap;
}
.lot-sidebar--collapsed .sidebar-header {
  justify-content: center;
  padding: 12px 0;
}
.collapse-btn {
  font-size: 16px;
  color: #606266;
}
.collapse-btn:hover { color: #409eff; }
.sidebar-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-search {
  padding: 12px;
  flex-shrink: 0;
}
.lot-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 12px;
}
.lot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 6px;
}
.lot-item:hover { background: #f5f7fa; }
.lot-item--active { background: #ecf5ff; }
.lot-info { flex: 1; min-width: 0; }
.lot-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.lot-address {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 右侧地图 */
.map-main {
  flex: 1;
  position: relative;
  overflow: hidden;
}
.amap-container {
  width: 100%;
  height: 100%;
}

/* 地图搜索框 */
.map-search-box {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  width: 420px;
  max-width: calc(100% - 40px);
  display: flex;
  gap: 8px;
  z-index: 10;
  background: #fff;
  padding: 10px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
}
.map-search-box .el-input { flex: 1; }
.search-result-list {
  position: absolute;
  top: calc(100% + 6px);
  left: 10px;
  right: 10px;
  max-height: 260px;
  overflow-y: auto;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  border: 1px solid #e4e7ed;
}
.search-result-item {
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f2f5;
  transition: background 0.15s;
}
.search-result-item:last-child { border-bottom: none; }
.search-result-item:hover { background: #f5f7fa; }
.result-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.result-address {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 选中停车场操作卡片 */
.selected-card {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 260px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  padding: 16px;
  z-index: 10;
}
.selected-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.selected-row {
  display: flex;
  font-size: 13px;
  margin-bottom: 8px;
}
.selected-label {
  width: 40px;
  color: #909399;
  flex-shrink: 0;
}
.selected-value {
  flex: 1;
  color: #303133;
  word-break: break-all;
}
.selected-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}
.selected-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 10px;
  line-height: 1.4;
}

/* 图例 */
.map-legend {
  position: absolute;
  bottom: 16px;
  right: 16px;
  background: rgba(255, 255, 255, 0.95);
  padding: 10px 14px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 16px;
  z-index: 10;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.legend-dot--located { background: #409eff; }
.legend-dot--selected { background: #f56c6c; }
</style>

<style>
/* 高德地图 label 样式（非 scoped，需要全局生效） */
.amap-marker-label {
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #409eff;
  border-radius: 4px;
  font-size: 12px;
  color: #303133;
  white-space: nowrap;
}
</style>
