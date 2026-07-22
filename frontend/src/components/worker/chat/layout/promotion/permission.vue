<template>
  <el-dropdown trigger="click" @command="handlePermissionUpdate">
    <div class="permission inline-flex-r-c-n"
      :class="{
        'is-full-access': loadPermission === Constant.SECURITY_PERMISSION_FULL_ACCESS
      }"
    >
      <el-icon>
        <Unlock v-if="loadPermission === Constant.SECURITY_PERMISSION_FULL_ACCESS" />
        <Lock v-else />
      </el-icon>
      <div class="label">{{ usePermissionLabel(loadPermission) }}</div>
    </div>
    <template #dropdown>
      <el-dropdown-menu class="permission-menu">
        <el-dropdown-item :command="Constant.SECURITY_PERMISSION_REQUEST_APPROVAL">
          <div class="permission-unit">
            <div class="name inline-flex-r-c-n">
              <el-icon size="16px"><Lock /></el-icon>
              {{ $t('chat.security-request-approval') }}
            </div>
            <div class="description">{{ $t('chat.security-request-approval-description') }}</div>
          </div>
        </el-dropdown-item>
        <el-dropdown-item :command="Constant.SECURITY_PERMISSION_FULL_ACCESS">
          <div class="permission-unit">
            <div class="name inline-flex-r-c-n">
              <el-icon size="16px"><Unlock /></el-icon>
              {{ $t('chat.security-full-access') }}
            </div>
            <div class="description">{{ $t('chat.security-full-access-description') }}</div>
          </div>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import {
  Lock,
  Unlock
} from '@element-plus/icons-vue'
import Constant from '@/config/constant'
import useSocketIO from '@/hooks/useSocketIO'

const {
  ioOn,
  ioRequest
} = useSocketIO()
const i18n = useI18n()
const loadPermission = ref(Constant.SECURITY_PERMISSION_REQUEST_APPROVAL)

onMounted(() => {
  handlePermissionLoad()
})

ioOn('security.permission.broadcast', (data) => {
  const payload = data[0]
  if (payload?.permission) {
    loadPermission.value = payload.permission
  }
})

const usePermissionLabel = (permission: string) => {
  return permission === Constant.SECURITY_PERMISSION_FULL_ACCESS
    ? i18n.t('chat.security-full-access')
    : i18n.t('chat.security-request-approval')
}
const handlePermissionLoad = async () => {
  try {
    const result = await ioRequest('security.permission.index')
    const payload = result[0]
    if (payload?.permission) {
      loadPermission.value = payload.permission
    }
  } catch {
  }
}
const handlePermissionUpdate = async (permission: string) => {
  if (!permission || permission === loadPermission.value) return
  const result = await ioRequest('security.permission.update', { permission })
  const payload = result[0]
  loadPermission.value = payload?.permission || permission
}
</script>
