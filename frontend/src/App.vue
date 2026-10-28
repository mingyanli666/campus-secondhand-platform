<template>
  <!-- ElementPlus 改为按需引入后，组件的全局配置（语言、尺寸、zIndex）
       要通过 el-config-provider 下发，替代原来的 app.use(ElementPlus, { locale }) -->
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
// 根组件只负责渲染路由出口，具体布局交给 layout 下的组件
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
