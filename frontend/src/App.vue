<!--

  【根组件】App.vue —— 整个应用的"空壳子"

  所有页面的最外层容器。它本身不显示任何内容，
  只有一个 <router-view />，作为"舞台"，
  当前路由对应的页面组件（如 Login.vue、Dashboard.vue）
  会自动填充到这个位置显示出来。

  比喻：App.vue 就像一个相框，<router-view /> 是相框里的画槽，
  用户切换页面 = 换一张画，相框本身不变。

  <style> 里设置了全局基础样式：
  让整个页面铺满浏览器窗口，没有默认的外边距和内边距。
-->
<template>
  <!-- router-view：路由出口，当前路由匹配到的页面组件在这里显示 -->
  <!-- 比如访问 /login，这里就显示 Login.vue；访问 /layout，就显示 Layout.vue -->
  <router-view />
</template>

<style>
/* 全局基础样式重置：让 html、body、#app 铺满整个浏览器窗口 */
/* 不加这个的话页面四周会有白色边框（浏览器默认 margin） */
html, body, #app {
  margin: 0;      /* 去掉默认外边距 */
  padding: 0;     /* 去掉默认内边距 */
  height: 100%;   /* 高度撑满 */
  width: 100%;    /* 宽度撑满 */
}
</style>
