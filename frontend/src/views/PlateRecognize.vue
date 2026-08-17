<template>
  <PageLayout title="车牌识别 — 入场 / 出场">
    <el-form label-width="110px" style="max-width: 720px">

      <!-- 停车场选择器：必须先选停车场，因为包月车是和停车场绑定的 -->
      <el-form-item label="选择停车场">
        <!-- @change="resetAll"：切换停车场时重置所有状态，避免车牌号和停车场对不上 -->
        <el-select v-model="lotId" placeholder="请选择停车场" style="width: 100%" @change="resetAll">
          <el-option v-for="item in lots" :key="item.id" :label="item.lotName" :value="item.id" />
        </el-select>
      </el-form-item>

      <!-- 图片来源切换：上传图片 或 调用摄像头（两种识别方式） -->
      <el-form-item label="图片来源">
        <el-radio-group v-model="imageSource" @change="onSourceChange">
          <el-radio-button value="upload">📁 上传图片</el-radio-button>
          <el-radio-button value="camera">📷 调用摄像头</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- ====== 上传模式区域（imageSource === 'upload' 时显示）====== -->
      <el-form-item v-if="imageSource === 'upload'" label="上传图片">
        <!--
          el-upload：Element Plus 的文件上传组件
          drag：支持拖拽上传
          action="#"：不自动上传到服务器（我们自己处理）
          :auto-upload="false"：关闭自动上传
          :on-change="handleImageChange"：选择文件后触发，在这里手动调用识别接口
          :limit="1"：最多上传 1 个文件
        -->
        <el-upload
          ref="uploadRef"
          drag
          action="#"
          :auto-upload="false"
          :on-change="handleImageChange"
          :on-remove="handleImageRemove"
          :limit="1"
          :show-file-list="true"
          accept="image/*"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 JPG / PNG，上传后自动识别车牌，也可手动修正</div>
          </template>
        </el-upload>
      </el-form-item>

      <!-- ====== 摄像头模式区域（imageSource === 'camera' 时显示）====== -->
      <el-form-item v-if="imageSource === 'camera'" label="摄像头">
        <div class="camera-box">
          <!-- 摄像头未开启时显示占位图 -->
          <div v-if="!cameraActive" class="camera-placeholder">
            <el-icon style="font-size:40px;color:#c0c4cc"><VideoCamera /></el-icon>
            <p>摄像头未开启</p>
          </div>

          <!--
            <video>：HTML5 原生视频标签，用于显示摄像头实时画面
            autoplay：获取到视频流后立即播放（不需要点播放按钮）
            playsinline：移动端不全屏播放
            muted：静音（摄像头通常不需要音频，静音也能避免浏览器安全限制）
            v-show：摄像头开启后才显示（v-show 只控制可见性，v-if 会销毁 DOM）
          -->
          <video
            v-show="cameraActive"
            ref="videoRef"
            autoplay
            playsinline
            muted
            class="camera-video"
          ></video>

          <!-- 扫描动画遮罩：摄像头开启但还没识别到车牌时，显示蓝色扫描线动画 -->
          <div v-if="cameraActive && !autoDetected" class="scan-overlay">
            <div class="scan-line"></div>
          </div>

          <!-- 识别成功遮罩：识别到车牌后显示绿色成功提示 -->
          <div v-if="cameraActive && autoDetected" class="detected-overlay">
            <el-icon style="font-size:36px;color:#67c23a"><CircleCheckFilled /></el-icon>
            <span>识别成功：{{ plateNumber }}</span>
          </div>

          <!--
            隐藏的 canvas（画布）：用于从 video 截取当前帧图像
            detectFrame() 方法里会把 video 的当前画面画到这个 canvas 上，
            再把 canvas 内容转成 Blob（二进制图片数据）发给后端识别
          -->
          <canvas ref="canvasRef" style="display:none"></canvas>
        </div>

        <!-- 摄像头控制按钮区域 -->
        <div class="camera-status">
          <template v-if="!cameraActive">
            <el-button type="primary" :icon="VideoPlay" @click="openCamera">开启摄像头</el-button>
          </template>
          <template v-else>
            <!-- 状态提示：检测中 / 已识别 -->
            <div class="status-badge" :class="autoDetecting ? 'status-scanning' : 'status-done'">
              <span v-if="!autoDetected">
                <el-icon class="is-loading"><Loading /></el-icon>
                自动检测中，请将车牌对准摄像头…
              </span>
              <span v-else>
                ✅ 已识别到车牌，可继续操作或
                <el-link type="primary" @click="restartDetect">重新检测</el-link>
              </span>
            </div>
            <el-button :icon="VideoPause" size="small" @click="closeCamera" style="margin-left:12px">关闭摄像头</el-button>
          </template>
        </div>
        <!-- 摄像头错误信息（权限被拒、设备不存在等） -->
        <div v-if="cameraError" class="camera-error">{{ cameraError }}</div>
      </el-form-item>

      <!-- 车牌号输入框：自动识别后填入，也可手动输入或修改 -->
      <el-form-item label="车牌号">
        <el-input
          v-model="plateNumber"
          placeholder="识别后自动填入，或手动输入车牌号"
          clearable
          style="width: 100%"
        >
          <!-- 输入框右侧附加的"重新识别"按钮 -->
          <template #append>
            <el-button @click="recognizeOnce" :loading="recognizing">
              {{ recognizing ? '识别中…' : '重新识别' }}
            </el-button>
          </template>
        </el-input>
        <!-- 车辆类型标签（识别后显示：包月车/临时车） -->
        <div v-if="carTypeText" class="plate-tag">
          <el-tag :type="carType === 1 ? 'success' : 'warning'" size="small">
            {{ carTypeText }}
          </el-tag>
        </div>
      </el-form-item>

      <!-- 手动操作按钮（适用于手动输入车牌号的场景） -->
      <el-form-item>
        <span style="font-size:13px;color:#909399;margin-right:12px">手动登记：</span>
        <el-button type="primary" size="large" :loading="entryLoading" @click="handleEntry">
          {{ entryLoading ? '处理中…' : '✅ 入场登记' }}
        </el-button>
        <el-button type="success" size="large" :loading="exitLoading" @click="handleExit">
          {{ exitLoading ? '处理中…' : '🚗 出场结算' }}
        </el-button>
        <el-button size="large" @click="resetAll">重置</el-button>
      </el-form-item>
    </el-form>

    <el-divider content-position="left">操作结果</el-divider>

    <!-- 操作结果展示区（入场/出场成功后显示） -->
    <div v-if="resultInfo" class="result-box">
      <!-- 结果头部：入场=蓝色，出场=绿色 -->
      <div class="result-header" :class="resultInfo.action === 'entry' ? 'header-entry' : 'header-exit'">
        <el-icon style="font-size:28px">
          <component :is="resultInfo.action === 'entry' ? 'Van' : 'CircleCheck'" />
        </el-icon>
        <span class="result-title">{{ resultInfo.title }}</span>
      </div>

      <!-- 详细信息表格：停车场、车牌、时间、费用等 -->
      <el-descriptions :column="2" border size="small" style="margin-top:14px">
        <el-descriptions-item label="停车场">{{ resultInfo.lotName || currentLotName }}</el-descriptions-item>
        <el-descriptions-item label="车牌号">{{ resultInfo.plateNumber }}</el-descriptions-item>
        <el-descriptions-item label="车辆类型">
          <el-tag :type="resultInfo.carType === 1 ? 'success' : 'warning'" size="small">
            {{ resultInfo.carType === 1 ? '包月车' : '临时车' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="resultInfo.action === 'entry' ? 'primary' : 'info'" size="small">
            {{ resultInfo.action === 'entry' ? '入场' : '出场' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="resultInfo.entryTime"       label="入场时间">{{ resultInfo.entryTime }}</el-descriptions-item>
        <el-descriptions-item v-if="resultInfo.exitTime"        label="出场时间">{{ resultInfo.exitTime }}</el-descriptions-item>
        <el-descriptions-item v-if="resultInfo.parkingMinutes != null" label="停车时长">{{ resultInfo.parkingMinutes }} 分钟</el-descriptions-item>
        <el-descriptions-item v-if="resultInfo.action === 'exit'" label="应缴金额">
          <span v-if="resultInfo.carType === 1" style="color:#67c23a;font-weight:bold">无需支付（包月车）</span>
          <span v-else-if="resultInfo.amount == 0" style="color:#67c23a;font-weight:bold">免费</span>
          <span v-else class="amount-text">¥ {{ resultInfo.amount }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 支付区域：出场 + 临时车 + 金额>0 才显示 -->
      <div v-if="resultInfo.action === 'exit' && resultInfo.carType !== 1 && resultInfo.amount > 0" class="pay-zone">
        <!-- 未支付状态 -->
        <div v-if="!resultInfo.paid" class="pay-unpaid">
          <span class="pay-tip">💳 请完成支付后点击确认</span>
          <el-button type="danger" size="large" :loading="payLoading" @click="handlePay">
            确认支付 ¥{{ resultInfo.amount }}
          </el-button>
        </div>
        <!-- 支付成功状态 -->
        <div v-else class="pay-done">
          <el-icon style="font-size:22px;color:#67c23a"><CircleCheckFilled /></el-icon>
          <span>支付成功，金额 ¥{{ resultInfo.amount }}</span>
        </div>
      </div>
    </div>

    <!-- 没有操作结果时显示空状态 -->
    <el-empty v-else description="暂无操作记录" />
  </PageLayout>
</template>

<script setup>
/**
 * 【车牌识别页】PlateRecognize.vue —— 系统最核心的操作页面
 *
 * 支持两种识别方式：
 *   1. 上传图片：手动选择本地图片文件，发给后端 OCR 识别
 *   2. 摄像头模式：调用浏览器 WebRTC API 打开摄像头，
 *      每隔 1.5 秒截一帧图像自动发给后端识别，识别到车牌后自动停止
 *
 * 识别后自动判断入场/出场：
 *   识别到车牌号后，先查后端该车是否在场（/api/record/check-in-lot）
 *   在场 → 自动触发出场结算；不在场 → 自动触发入场登记
 *   也可以手动点按钮手动操作
 */

import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import {
  UploadFilled, VideoCamera, VideoPlay, VideoPause, Loading, CircleCheckFilled, Van, CircleCheck
} from '@element-plus/icons-vue'
import request from '../utils/request'
import PageLayout from '../components/PageLayout.vue'

// ================================================================
// 基础响应式状态
// ================================================================
const lotId            = ref(null)   // 当前选中的停车场 ID
const lots             = ref([])     // 停车场列表（下拉框数据源）
const selectedFile     = ref(null)   // 上传模式：当前选中的图片文件
const plateNumber      = ref('')     // 识别出的车牌号（可手动修改）
const carType          = ref(null)   // 车辆类型：1=包月车，2=临时车
const carTypeText      = ref('')     // 车辆类型文字："包月车" 或 "临时车"
const recognizedImgUrl = ref('')     // 识别时保存的图片 URL（入/出场记录用）
const recognizing      = ref(false)  // 是否正在识别（控制"重新识别"按钮 loading）
const entryLoading     = ref(false)  // 入场按钮 loading 状态
const exitLoading      = ref(false)  // 出场按钮 loading 状态
const payLoading       = ref(false)  // 支付按钮 loading 状态
const resultInfo       = ref(null)   // 操作结果对象（null=无结果，有值则展示结果卡片）

// ================================================================
// 图片来源切换
// ================================================================
const imageSource = ref('upload')   // 'upload' 或 'camera'
const uploadRef   = ref(null)        // el-upload 组件的引用，用于调用 clearFiles()

/** 切换图片来源时清空所有状态 */
function onSourceChange() {
  selectedFile.value     = null
  plateNumber.value      = ''
  carType.value          = null
  carTypeText.value      = ''
  recognizedImgUrl.value = ''
  if (cameraActive.value) closeCamera()  // 切换时关闭摄像头
  uploadRef.value?.clearFiles()          // 清空上传列表（?.是可选链，uploadRef 为 null 时不报错）
}

// ================================================================
// 摄像头模式核心逻辑
// ================================================================
const videoRef      = ref(null)   // <video> 元素的 DOM 引用
const canvasRef     = ref(null)   // <canvas> 元素的 DOM 引用（截帧用）
const cameraActive  = ref(false)  // 摄像头是否开启
const cameraError   = ref('')     // 摄像头错误信息
const autoDetecting = ref(false)  // 是否正在轮询检测
const autoDetected  = ref(false)  // 是否已成功识别到车牌
let   mediaStream   = null        // 媒体流对象（关闭摄像头时需要 stop 它）
let   detectTimer   = null        // setInterval 返回的句柄（停止轮询时用 clearInterval）
const DETECT_INTERVAL = 1500      // 轮询间隔：每 1500ms（1.5秒）截帧识别一次

/**
 * 开启摄像头
 *
 * 使用浏览器的 WebRTC API（navigator.mediaDevices.getUserMedia）
 * 请求摄像头权限并获取视频流。
 * 获取成功后把视频流赋给 <video> 元素的 srcObject，视频画面就显示出来了。
 * 然后立即启动自动轮询检测（startAutoDetect）。
 */
async function openCamera() {
  cameraError.value = ''
  if (!navigator.mediaDevices?.getUserMedia) {
    cameraError.value = '您的浏览器不支持摄像头访问，请使用 Chrome / Edge 并确保页面在 HTTPS 或 localhost 下运行'
    return
  }
  if (!lotId.value) {
    ElMessage.warning('请先选择停车场，再开启摄像头')
    return
  }
  try {
    // 请求摄像头权限，偏好后置摄像头（移动端），分辨率 1280×720
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: false  // 不需要麦克风
    })
    videoRef.value.srcObject = mediaStream  // 把视频流绑定到 <video> 标签
    cameraActive.value  = true
    autoDetected.value  = false
    startAutoDetect()   // 开启自动识别轮询
  } catch (err) {
    // 不同错误类型给不同提示
    if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
      cameraError.value = '摄像头权限被拒绝，请在浏览器地址栏左侧允许摄像头访问后刷新页面'
    } else if (err.name === 'NotFoundError') {
      cameraError.value = '未检测到摄像头设备，请确认设备已连接'
    } else {
      cameraError.value = `摄像头开启失败：${err.message}`
    }
  }
}

/** 关闭摄像头，停止轮询，释放媒体流 */
function closeCamera() {
  stopAutoDetect()
  if (mediaStream) {
    // 停止所有媒体轨道（释放摄像头资源，摄像头指示灯会熄灭）
    mediaStream.getTracks().forEach(t => t.stop())
    mediaStream = null
  }
  if (videoRef.value) videoRef.value.srcObject = null
  cameraActive.value  = false
  autoDetected.value  = false
}

/** 启动轮询：每 1.5 秒调用一次 detectFrame */
function startAutoDetect() {
  if (detectTimer) return  // 防止重复启动
  autoDetecting.value = true
  detectTimer = setInterval(detectFrame, DETECT_INTERVAL)
}

/** 停止轮询 */
function stopAutoDetect() {
  if (detectTimer) {
    clearInterval(detectTimer)  // 清除定时器
    detectTimer = null
  }
  autoDetecting.value = false
}

/**
 * 截取视频当前帧并发送识别
 *
 * 原理：
 *   1. 把 <video> 当前画面用 canvas.getContext('2d').drawImage() 画到 canvas 上
 *   2. 用 canvas.toBlob() 把 canvas 内容转成 JPEG 格式的 Blob（二进制图片）
 *   3. 封装成 FormData 发给后端 /api/plate/recognize 接口
 *   4. 识别成功（返回车牌号）→ 停止轮询，自动触发入场/出场
 *   5. 未识别到 → 静默继续轮询（{ silent: true } 不弹错误提示）
 */
async function detectFrame() {
  const video  = videoRef.value
  const canvas = canvasRef.value
  if (!video || !canvas || !cameraActive.value || recognizing.value) return
  if (video.readyState < 2) return  // 视频流尚未就绪（readyState < 2 表示还没有足够数据）

  // 把 video 当前帧画到 canvas
  canvas.width  = video.videoWidth  || 640
  canvas.height = video.videoHeight || 480
  canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height)

  // canvas.toBlob() 是异步的，将画布内容转为 JPEG Blob
  canvas.toBlob(async (blob) => {
    if (!blob || recognizing.value) return
    const file = new File([blob], `auto_${Date.now()}.jpg`, { type: 'image/jpeg' })
    recognizing.value = true
    try {
      const formData = new FormData()
      formData.append('lotId', lotId.value)
      formData.append('file',  file)
      // { silent: true }：request.js 拦截器里，silent=true 时不弹错误提示
      // 因为轮询时未识别到车牌是正常情况，不应该弹错误
      const res  = await request.post('/api/plate/recognize', formData, { silent: true })
      const data = res.data || {}

      if (data.plateNumber) {
        // 识别成功！停止轮询，填充结果，自动判断入场/出场
        stopAutoDetect()
        selectedFile.value     = file
        plateNumber.value      = data.plateNumber
        carType.value          = data.carType
        carTypeText.value      = data.carTypeText || ''
        recognizedImgUrl.value = data.imgUrl || ''
        autoDetected.value     = true
        ElMessage.success(`自动识别成功：${data.plateNumber}（${data.carTypeText}）`)
        await autoEntryOrExit(data.plateNumber, data.carType, data.imgUrl)
      }
      // plateNumber 为空 = 画面中暂无车牌，静默继续下一次轮询
    } catch {
      // 网络错误等也静默继续，不打扰用户
    } finally {
      recognizing.value = false
    }
  }, 'image/jpeg', 0.92)  // 0.92 = JPEG 压缩质量 92%，在清晰度和文件大小之间平衡
}

/** 重新检测：清空之前的识别结果，重启轮询 */
function restartDetect() {
  plateNumber.value = ''; carType.value = null; carTypeText.value = ''
  recognizedImgUrl.value = ''; selectedFile.value = null; autoDetected.value = false
  startAutoDetect()
}

/** "重新识别"按钮点击：截当前帧识别一次（摄像头模式）或重新识别文件（上传模式） */
async function recognizeOnce() {
  if (imageSource.value === 'camera') {
    if (!cameraActive.value) return ElMessage.warning('请先开启摄像头')
    stopAutoDetect()
    await detectFrame()
    if (!autoDetected.value) startAutoDetect()  // 这次没识别到，继续轮询
  } else {
    await recognizeFile()
  }
}

// 页面卸载时（用户离开这个页面）自动关闭摄像头，释放资源
onBeforeUnmount(closeCamera)

// ================================================================
// 停车场列表加载
// ================================================================
/** currentLotName：计算属性，根据 lotId 从 lots 数组里找到对应停车场名称 */
const currentLotName = computed(() => {
  const lot = lots.value.find(item => item.id === lotId.value)
  return lot ? lot.lotName : '-'
})

onMounted(loadLots)  // 页面挂载后立即加载停车场列表

async function loadLots() {
  try {
    const res = await request.get('/api/park-lot/list')
    lots.value = res.data || []
  } catch {
    ElMessage.error('获取停车场列表失败')
  }
}

// ================================================================
// 上传模式：文件选择和识别
// ================================================================
/** 文件选择后触发（el-upload 的 on-change 回调） */
function handleImageChange(file) {
  selectedFile.value = file.raw  // .raw 是原始的 File 对象
  if (lotId.value) {
    recognizeFile()  // 已选停车场，直接识别
  } else {
    ElMessage.warning('请先选择停车场，再上传图片')
  }
}

/** 移除文件时清空相关状态 */
function handleImageRemove() {
  selectedFile.value = null; plateNumber.value = ''; carType.value = null
  carTypeText.value = ''; recognizedImgUrl.value = ''
}

/** 上传模式：把图片文件发给后端识别 */
async function recognizeFile() {
  if (!lotId.value)        return ElMessage.warning('请先选择停车场')
  if (!selectedFile.value) return ElMessage.warning('请先上传车辆图片')
  recognizing.value = true
  try {
    // FormData：浏览器内置的表单数据类，用于发送 multipart/form-data（文件上传格式）
    const formData = new FormData()
    formData.append('lotId', lotId.value)
    formData.append('file',  selectedFile.value)
    const res  = await request.post('/api/plate/recognize', formData)
    const data = res.data || {}
    plateNumber.value      = data.plateNumber || ''
    carType.value          = data.carType
    carTypeText.value      = data.carTypeText || ''
    recognizedImgUrl.value = data.imgUrl || ''
    ElMessage.success(`识别成功：${plateNumber.value}（${carTypeText.value}）`)
    await autoEntryOrExit(data.plateNumber, data.carType, data.imgUrl)
  } catch {
    plateNumber.value = ''; carType.value = null; carTypeText.value = ''
    ElMessage.info('未识别到车牌，请在"车牌号"输入框中手动输入后继续操作')
  } finally {
    recognizing.value = false
  }
}

// ================================================================
// 自动判断入场 / 出场
// ================================================================
/**
 * 识别到车牌后，查询该车是否在场，自动决定入场还是出场
 *
 * 接口：GET /api/record/check-in-lot?plateNumber=xxx&lotId=xxx
 * 返回：{ inLot: true/false, recordId: xxx }
 *   inLot=true：车在场 → 自动出场结算
 *   inLot=false：车不在场 → 自动入场登记
 */
async function autoEntryOrExit(plate, cType, imgUrl) {
  if (!lotId.value || !plate) return
  try {
    const res  = await request.get('/api/record/check-in-lot', {
      params: { plateNumber: plate, lotId: lotId.value }
    })
    const data = res.data || {}
    if (data.inLot) {
      await doExit(data.recordId, plate, imgUrl)   // 在场 → 出场
    } else {
      await doEntry(plate, cType, imgUrl)          // 不在场 → 入场
    }
  } catch {
    ElMessage.warning('自动判断入场/出场失败，请手动操作')
  }
}

/** 执行入场登记（内部方法，自动和手动都调这个） */
async function doEntry(plate, cType, imgUrl) {
  entryLoading.value = true
  try {
    const res  = await request.post('/api/record/add', {
      lotId: lotId.value, plateNumber: plate, carType: cType || 2, entryImgUrl: imgUrl || ''
    })
    const data = res.data || {}
    // 把入场结果存到 resultInfo，模板会自动渲染结果卡片
    resultInfo.value = {
      type: 'success', action: 'entry',
      title: res.message || '入场登记成功',
      desc:  `车牌：${plate}`,
      plateNumber: plate, carType: cType,
      lotName: currentLotName.value,
      entryTime: formatDate(data.entryTime || new Date())
    }
    ElMessage.success(res.message || '自动入场登记成功')
  } catch {
    // 错误已由 request.js 的响应拦截器统一弹出
  } finally {
    entryLoading.value = false
  }
}

/** 执行出场结算（内部方法） */
async function doExit(recordId, plate, imgUrl) {
  exitLoading.value = true
  try {
    const res  = await request.post('/api/record/exit', {
      id: recordId, plateNumber: plate, exitImgUrl: imgUrl || ''
    })
    const data = res.data || {}
    resultInfo.value = {
      type: 'success', action: 'exit',
      title: res.message || '出场结算成功',
      plateNumber: plate, carType: data.carType,
      lotName:        currentLotName.value,
      entryTime:      formatDate(data.entryTime),
      exitTime:       formatDate(data.exitTime),
      parkingMinutes: data.parkingMinutes,
      amount:         data.amount,
      paymentId:      data.paymentId || null,  // 待支付记录 ID
      paid:           false   // 初始未支付
    }
    ElMessage.success(res.message || '自动出场结算成功')
  } catch {
  } finally {
    exitLoading.value = false
  }
}

/** 确认支付按钮：调用后端把 payStatus 改为已支付 */
async function handlePay() {
  if (!resultInfo.value?.paymentId) {
    ElMessage.warning('无对应缴费记录，请前往缴费记录管理手动处理')
    return
  }
  payLoading.value = true
  try {
    await request.put(`/api/payment/pay/${resultInfo.value.paymentId}`)
    resultInfo.value.paid = true  // 标记为已支付，页面显示"支付成功"
    ElMessage.success('支付成功！')
  } catch {
  } finally {
    payLoading.value = false
  }
}

// ================================================================
// 手动操作（点按钮）
// ================================================================
/** 手动点"入场登记"按钮 */
async function handleEntry() {
  if (!validateBeforeSubmit()) return
  entryLoading.value = true
  try {
    const res = await request.post('/api/plate/entry', {
      lotId:       lotId.value,
      plateNumber: plateNumber.value.trim().toUpperCase(),  // 统一大写（车牌格式规范）
      carType:     carType.value || 2,
      entryImgUrl: recognizedImgUrl.value
    })
    const data = res.data || {}
    resultInfo.value = {
      type: 'success', action: 'entry',
      title: res.message || '入场登记成功',
      desc:  `车牌：${data.plateNumber}，类型：${data.carTypeText}`,
      plateNumber: data.plateNumber, carType: data.carType,
      lotName: currentLotName.value, entryTime: formatDate(data.entryTime)
    }
    ElMessage.success(res.message || '入场登记成功')
  } catch {
  } finally {
    entryLoading.value = false
  }
}

/** 手动点"出场结算"按钮 */
async function handleExit() {
  if (!validateBeforeSubmit()) return
  exitLoading.value = true
  try {
    // 先查该车是否在场（防止用户操作有误）
    const checkRes  = await request.get('/api/record/check-in-lot', {
      params: { plateNumber: plateNumber.value.trim().toUpperCase(), lotId: lotId.value }
    })
    const checkData = checkRes.data || {}
    if (!checkData.inLot) { ElMessage.warning('该车辆不在场内，无法出场'); return }
    await doExit(checkData.recordId, plateNumber.value.trim().toUpperCase(), recognizedImgUrl.value)
  } catch {
  } finally {
    exitLoading.value = false
  }
}

// ================================================================
// 工具函数
// ================================================================
/** 提交前校验：停车场和车牌号不能为空 */
function validateBeforeSubmit() {
  if (!lotId.value)                    { ElMessage.warning('请先选择停车场');       return false }
  if (!plateNumber.value.trim())       { ElMessage.warning('请输入或识别车牌号'); return false }
  return true
}

/** 重置所有状态 */
function resetAll() {
  selectedFile.value = null; plateNumber.value = ''; carType.value = null
  carTypeText.value = ''; recognizedImgUrl.value = ''; resultInfo.value = null
  cameraError.value = ''; autoDetected.value = false
  if (cameraActive.value) closeCamera()
  uploadRef.value?.clearFiles()
}

/**
 * 格式化日期为 yyyy-MM-dd HH:mm:ss
 * 后端返回的时间可能是 ISO 8601 字符串（如 "2025-06-01T15:30:00.000+08:00"）
 * 这里统一格式化成中文常见的显示格式
 */
function formatDate(val) {
  if (!val) return null
  const d = new Date(val)
  if (isNaN(d)) return val  // 如果不是有效日期，原样返回
  const pad = n => String(n).padStart(2, '0')  // 补零：5 → "05"
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
</script>

<style scoped>
/* 摄像头预览框 */
.camera-box { width: 100%; max-width: 520px; height: 300px; background: #1a1a2e; border-radius: 8px; overflow: hidden; display: flex; align-items: center; justify-content: center; position: relative; border: 1px solid #dcdfe6; }
.camera-placeholder { display: flex; flex-direction: column; align-items: center; color: #909399; gap: 10px; }
.camera-placeholder p { margin: 0; font-size: 14px; }
.camera-video { width: 100%; height: 100%; object-fit: cover; }

/* 扫描线动画：从上到下匀速移动，营造扫描感 */
.scan-overlay { position: absolute; inset: 0; pointer-events: none; border: 2px solid rgba(64, 158, 255, 0.6); border-radius: 8px; overflow: hidden; }
.scan-line { position: absolute; left: 0; right: 0; height: 3px; background: linear-gradient(90deg, transparent, #409eff, transparent); animation: scanMove 2s linear infinite; }
@keyframes scanMove { 0% { top: 0%; } 100% { top: 100%; } }

/* 识别成功时的绿色遮罩 */
.detected-overlay { position: absolute; inset: 0; background: rgba(0, 0, 0, 0.45); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: #fff; font-size: 16px; font-weight: bold; border: 2px solid #67c23a; border-radius: 8px; }

.camera-status { display: flex; align-items: center; margin-top: 12px; flex-wrap: wrap; gap: 8px; }
.status-badge { display: flex; align-items: center; gap: 6px; font-size: 13px; padding: 4px 10px; border-radius: 4px; }
.status-scanning { color: #409eff; background: #ecf5ff; }  /* 检测中：蓝色 */
.status-done     { color: #67c23a; background: #f0f9eb; }  /* 已识别：绿色 */
.camera-error { margin-top: 8px; color: #f56c6c; font-size: 13px; line-height: 1.6; }

.el-upload__tip { color: #909399; font-size: 12px; margin-top: 6px; }
.plate-tag  { margin-top: 6px; }
.result-box { margin-top: 8px; }

/* 结果卡片头部：入场蓝色，出场绿色 */
.result-header { display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-radius: 8px; font-size: 16px; font-weight: bold; color: #fff; }
.header-entry { background: linear-gradient(135deg, #409eff, #66b1ff); }
.header-exit  { background: linear-gradient(135deg, #67c23a, #85ce61); }
.result-title { font-size: 16px; }

/* 支付区域 */
.pay-zone { margin-top: 16px; padding: 16px 20px; border-radius: 8px; background: #fef9f0; border: 1px solid #f5dab1; }
.pay-unpaid { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.pay-tip  { color: #e6a23c; font-size: 14px; font-weight: 500; }
.pay-done { display: flex; align-items: center; gap: 8px; color: #67c23a; font-size: 15px; font-weight: bold; }
.amount-text { color: #f56c6c; font-weight: bold; font-size: 15px; }
</style>
