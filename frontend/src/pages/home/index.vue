<template>
  <div class="a-home inline-flex-r-n-n">
    <a-menu></a-menu>
    <div class="a-page inline-flex-c-n-n">
      <a-head></a-head>
      <router-view v-slot="{ Component, route }">
        <keep-alive v-if="route.meta.keepalive">
          <component :is="Component" :key="route.path" />
        </keep-alive>
        <component v-else :is="Component" :key="route.path" />
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import AMenu from '@/components/home/menu.vue'
import AHead from '@/components/home/head.vue'
</script>
