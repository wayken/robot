<template>
  <div class="a-head inline-flex-r-c-b">
    <div class="head--left inline-flex-r-n-n">
      <div class="icon" @click="handleMenuCollapsed">
        <a-svg-icon v-if="!isMenuCollapsed" icon-class="indent-left" size="28px" />
        <a-svg-icon v-else icon-class="indent-right" size="28px" />
      </div>
    </div>
    <div class="head--right inline-flex-r-c-n">
      <el-dropdown trigger="hover" size="large" :teleported="false">
        <div class="icon">
          <a-svg-icon icon-class="headset" size="22px" />
        </div>
        <template #dropdown>
          <div class="consule">
            <el-image src="https://waybuket01.oss-cn-beijing.aliyuncs.com/home/wechat_qrcode.jpg" />
            <div class="description">
              {{ $t('header.scan-consule') }}
            </div>
          </div>
        </template>
      </el-dropdown>
      <div class="icon">
        <a-svg-icon icon-class="bell" size="22px" />
      </div>
      <a-divider direction="vertical" :height="24"></a-divider>
      <el-dropdown trigger="click" size="large" :teleported="false"
        @visible-change="handleAccountDropdown"
      >
        <div class="account inline-flex-r-c-n"
          :class="{
            'is-actived': isAccountDropdown
          }"
        >
          <el-avatar :size="32" :icon="UserFilled" :src="account.avatar" />
          <div class="name">{{ account.name || '-'}}</div>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>
              <el-icon><Setting /></el-icon>
              <span>{{ $t('header.account-setting') }}</span>
            </el-dropdown-item>
            <el-dropdown-item>
              <el-icon><SwitchButton /></el-icon>
              <span>{{ $t('header.logout') }}</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Setting,
  ArrowDown,
  UserFilled,
  SwitchButton
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/modules/auth'
import { useMenuStore } from '@/store/modules/menu'

const {
  account
} = storeToRefs(useAuthStore())
const {
  isMenuCollapsed
} = storeToRefs(useMenuStore())
const isAccountDropdown = ref(false)

// 菜单折叠功能
const handleMenuCollapsed = () => {
  const menuStore = useMenuStore()
  menuStore.dispatchSetMenuCollapsed(!isMenuCollapsed.value)
}
// 账号下拉功能
const handleAccountDropdown = (value: boolean) => {
  isAccountDropdown.value = value
}
</script>
