<template>
  <div class="a-wiki inline-flex-c-n-n">
    <a-drawer></a-drawer>
    <a-nodata :loading="false" :success="true" />
  </div>
</template>

<script setup lang="ts">
import ADrawer from '@/components/home/wiki/drawer.vue'
</script>
