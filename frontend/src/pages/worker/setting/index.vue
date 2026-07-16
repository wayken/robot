<template>
  <div class="a-worker-setting inline-flex-r-n-n">
    <a-drawer></a-drawer>
    <router-view v-slot="{ Component, route }">
      <keep-alive v-if="route.meta.keepalive">
        <component :is="Component" :key="route.path" />
      </keep-alive>
      <component v-else :is="Component" :key="route.path" />
    </router-view>
  </div>
</template>

<script setup lang="ts">
import ADrawer from '@/components/worker/setting/drawer.vue'
</script>
