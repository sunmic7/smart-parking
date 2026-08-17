<template>
  <div class="page-layout">
    <!-- 页面标题栏：左侧标题，右侧操作区 -->
    <div class="page-header">
      <div class="page-title">
        <slot name="title">
          <h2>{{ title }}</h2>
        </slot>
      </div>
      <div class="page-actions" v-if="$slots.actions">
        <slot name="actions" />
      </div>
    </div>

    <!-- 搜索栏插槽 -->
    <div class="page-search" v-if="$slots.search">
      <slot name="search" />
    </div>

    <!-- 主内容区（表格等） -->
    <div class="page-body">
      <slot />
    </div>

    <!-- 底部扩展区（统计面板等） -->
    <div class="page-footer" v-if="$slots.footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
/**
 * 统一页面布局组件
 * 提供一致的标题栏、搜索区、内容区和底部扩展区样式
 */
defineProps({
  title: { type: String, default: '' }
})
</script>

<style scoped>
.page-layout {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.page-actions {
  display: flex;
  align-items: center;
}

.page-search {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.page-body {
  width: 100%;
}

.page-footer {
  margin-top: 24px;
}

/* 统一分页样式 */
.page-body :deep(.pagination-box) {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
