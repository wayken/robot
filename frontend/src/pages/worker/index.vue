<template>
  <div class="a-worker inline-flex-c-n-n">
    <a-head></a-head>
    <router-view v-slot="{ Component, route }">
      <keep-alive v-if="route.meta.keepalive">
        <component :is="Component" :key="useRouteKey(route)" />
      </keep-alive>
      <component v-else :is="Component" :key="useRouteKey(route)" />
    </router-view>
  </div>
</template>

<script setup lang="ts">
import SocketIO from '@/api/socketio'
import { useRequest } from '@/hooks/useRequest'
import AHead from '@/components/worker/head.vue'

const {
  ioload
} = useRequest()
const router = useRouter()

const useRouteKey = (route: any) => {
  const matched = route.matched[1]
  if (matched) {
    return `${matched.name}-${route.params.id}`
  }
  return route.path
}

const assistantId = router.currentRoute.value.params.id
// 异步加载节点信息，包括：代理服务地址、验证签名等
const loadNodeInfo = ioload('assistant', 'loadAssistantInfomation', { id: assistantId })
  .then((assistant: any) => {
    return ioload('node', 'loadNodeInfomation', {
      id: assistant.node_id
    })
  })
const loadEndpoint = loadNodeInfo.then((node: any) => {
  return `${node.address}:${node.port}/worker.io`
})
const loadDiskEndpoint = loadNodeInfo.then((node: any) => {
  return `http://${node.address}:${node.port}`
})
const loadHandshakeData = loadNodeInfo.then((node: any) => {
  return {
    authorization: node.signature
  }
})

// 保存上下文供其他模块使用
const context = {
  // 当前实例
  instance: getCurrentInstance(),
  // 当前连接句柄
  socketIO: new SocketIO({
    reconnectAttempts: 4,
    remoteEndpoint: loadEndpoint,
    handshakeData: loadHandshakeData
  }),
  diskEndpoint: loadDiskEndpoint,
  diskAuthorization: loadHandshakeData.then((data: any) => data.authorization)
}
// 注入上下文，供其他模块使用
provide('context', context)
</script>
