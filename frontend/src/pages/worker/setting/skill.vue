<template>
  <div class="a-setting-skill inline-flex-c-n-n" v-loading="progression.loading">
    <!-- 顶部操作栏 -->
    <div class="head inline-flex-r-c-b">
      <div class="title inline-flex-r-c-n">
        <div class="name">{{ $t('skill-setting.title') }}</div>
        <div class="count">{{ loadFilteredList.length }}</div>
      </div>
      <div class="operations inline-flex-r-c-n">
        <el-input v-model="loadSearchKeyword" class="search" clearable
          :prefix-icon="Search"
          :placeholder="$t('skill-setting.search-placeholder')"
        />
        <el-dropdown trigger="click" @command="handleImportCommand">
          <a-button type="primary" :icon="Plus" size="small">
            {{ $t('skill-setting.import') }}
          </a-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="folder">
                {{ $t('skill-setting.import-folder') }}
              </el-dropdown-item>
              <el-dropdown-item command="zip">
                {{ $t('skill-setting.import-zip') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <!-- 技能卡片列表 -->
    <div class="content">
      <div class="card">
        <div class="module" v-for="data in loadFilteredList" :key="data.name">
          <div class="header inline-flex-r-c-b">
            <div class="infomation inline-flex-r-c-n">
              <a-svg-icon icon-class="skill" size="28px" class="icon" />
              <div class="name">{{ data.name }}</div>
            </div>
            <a-button v-if="!data.enabled" type="success" size="small"
              @click="handleEnableSkill(data)"
            >
              {{ $t('extension.enable') }}
            </a-button>
            <a-button v-else type="primary" size="small">
              {{ $t('skill.use') }}
            </a-button>
          </div>
          <div class="body">
            <p class="description">{{ data.description || $t('skill-setting.no-description') }}</p>
          </div>
          <div class="footer inline-flex-r-c-b">
            <div class="marks inline-flex-r-c-n">
              <el-tag v-if="data.enabled" type="success">
                {{ $t('extension.enable') }}
              </el-tag>
              <el-tag v-else type="info">
                {{ $t('extension.disable') }}
              </el-tag>
              <el-tag v-if="data.always" type="warning">
                {{ $t('skill-setting.always') }}
              </el-tag>
            </div>
            <el-dropdown trigger="click" @command="(cmd: string) => handleMoreCommand(cmd, data)">
              <el-icon class="more"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" :icon="Edit">
                    {{ $t('common.edit') }}
                  </el-dropdown-item>
                  <el-dropdown-item command="download" :icon="Download">
                    {{ $t('common.download') }}
                  </el-dropdown-item>
                  <el-dropdown-item v-if="data.enabled" command="disable" :icon="TurnOff" divided>
                    {{ $t('extension.disable') }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" divided>
                    {{ $t('common.delete') }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
      <a-nodata v-if="loadFilteredList.length === 0 && !progression.loading"
        :loading="progression.loading"
        :success="progression.success"
      />
    </div>
    <!-- 编辑弹窗 -->
    <a-window ref="loadWindowRef" @submit="handleSkillSave" />
  </div>
</template>

<script setup lang="ts">
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import {
  Search,
  Plus,
  Edit,
  Delete,
  Download,
  TurnOff,
  MoreFilled
} from '@element-plus/icons-vue'
import useSocketIO from '@/hooks/useSocketIO'
import { useFileDialog } from '@/utils/dom'
import AWindow from '@/components/worker/setting/skill/window.vue'

interface SkillItem {
  name: string
  description: string
  always: boolean
  enabled: boolean
}

const i18n = useI18n()
const route = useRoute()
const { ioRequest, progression } = useSocketIO()

const dataList = ref<SkillItem[]>([])
const loadSearchKeyword = ref('')
const loadWindowRef = ref<InstanceType<typeof AWindow> | null>(null)

const useWid = () => route.params.id as string

const loadFilteredList = computed(() => {
  const keyword = loadSearchKeyword.value.trim().toLowerCase()
  if (!keyword) return dataList.value
  return dataList.value.filter(item =>
    item.name.toLowerCase().includes(keyword) ||
    (item.description && item.description.toLowerCase().includes(keyword))
  )
})

onMounted(() => {
  handleDataLoad()
})

const handleDataLoad = () => {
  ioRequest('skill.index', { wid: useWid() }).then((result: any) => {
    dataList.value = result[0] || []
  })
}

const handleEnableSkill = (data: SkillItem) => {
  ioRequest('skill.switch', {
    wid: useWid(),
    name: data.name,
    enabled: true
  }).then((result: any) => {
    const success = result[0]
    if (success) {
      data.enabled = true
      ElMessage.success(i18n.t('skill-setting.enabled-success'))
    } else {
      ElMessage.error(i18n.t('skill-setting.switch-failed'))
    }
  })
}

const handleDisableSkill = (data: SkillItem) => {
  ioRequest('skill.switch', {
    wid: useWid(),
    name: data.name,
    enabled: false
  }).then((result: any) => {
    const success = result[0]
    if (success) {
      data.enabled = false
      ElMessage.success(i18n.t('skill-setting.disabled-success'))
    } else {
      ElMessage.error(i18n.t('skill-setting.switch-failed'))
    }
  })
}

const handleMoreCommand = (command: string, data: SkillItem) => {
  switch (command) {
    case 'edit':
      handleEditSkill(data)
      break
    case 'download':
      handleDownloadSkill(data)
      break
    case 'disable':
      handleDisableSkill(data)
      break
    case 'delete':
      handleDeleteSkill(data)
      break
  }
}

const handleEditSkill = (data: SkillItem) => {
  ioRequest('skill.read', { wid: useWid(), name: data.name }).then((result: any) => {
    const content = result[0] || ''
    loadWindowRef.value?.open(data.name, content)
  })
}

const handleSkillSave = (name: string, content: string) => {
  loadWindowRef.value?.setSaving(true)
  ioRequest('skill.write', { wid: useWid(), name, content }).then((result: any) => {
    const success = result[0]
    if (success) {
      ElMessage.success(i18n.t('skill-setting.save-success'))
      loadWindowRef.value?.close()
      handleDataLoad()
    } else {
      ElMessage.error(i18n.t('skill-setting.save-failed'))
    }
  }).finally(() => {
    loadWindowRef.value?.setSaving(false)
  })
}

const handleDownloadSkill = (data: SkillItem) => {
  ioRequest('skill.download', { wid: useWid(), name: data.name }).then((result: any) => {
    const base64 = result[0]
    if (!base64) {
      ElMessage.error(i18n.t('skill-setting.download-failed'))
      return
    }
    // 将 base64 转为 Blob 下载
    const byteChars = atob(base64)
    const byteNumbers = new Array(byteChars.length)
    for (let i = 0; i < byteChars.length; i++) {
      byteNumbers[i] = byteChars.charCodeAt(i)
    }
    const byteArray = new Uint8Array(byteNumbers)
    const blob = new Blob([byteArray], { type: 'application/zip' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = data.name + '.zip'
    a.click()
    URL.revokeObjectURL(url)
  })
}

const handleDeleteSkill = (data: SkillItem) => {
  ElMessageBox.confirm(
    i18n.t('skill-setting.delete-confirm', { placeholder: data.name }),
    i18n.t('skill-setting.delete-title'),
    {
      type: 'warning',
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.delete')
    }
  ).then(() => {
    ioRequest('skill.delete', { wid: useWid(), name: data.name }).then((result: any) => {
      const success = result[0]
      if (success) {
        ElMessage.success(i18n.t('skill-setting.delete-success'))
        handleDataLoad()
      } else {
        ElMessage.error(i18n.t('skill-setting.delete-failed'))
      }
    })
  }).catch(() => {})
}

const handleImportCommand = async (command: string) => {
  if (command === 'zip') {
    const files = await useFileDialog({ accept: '.zip', multiple: false })
    const file = files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => {
      const arrayBuffer = reader.result as ArrayBuffer
      const base64 = btoa(
        new Uint8Array(arrayBuffer).reduce((data, byte) => data + String.fromCharCode(byte), '')
      )
      const skillName = file.name.replace(/\.zip$/i, '')
      ioRequest('skill.import', { wid: useWid(), name: skillName, content: base64 }).then((result: any) => {
        const success = result[0]
        if (success) {
          ElMessage.success(i18n.t('skill-setting.import-success'))
          handleDataLoad()
        } else {
          ElMessage.error(i18n.t('skill-setting.import-failed'))
        }
      })
    }
    reader.readAsArrayBuffer(file)
  } else if (command === 'folder') {
    const files = await useFileDialog({ directory: true })
    if (!files || files.length === 0) return
    // 从文件列表中推断技能名称（取第一层目录名）
    const firstPath = (files[0] as any).webkitRelativePath || files[0].name
    const skillName = firstPath.split('/')[0]
    // 读取所有文件
    const fileReads: Promise<{ path: string; data: ArrayBuffer }>[] = []
    for (let i = 0; i < files.length; i++) {
      const f = files[i]
      const relativePath = ((f as any).webkitRelativePath || f.name)
      const subPath = relativePath.split('/').slice(1).join('/')
      if (!subPath) continue
      fileReads.push(
        new Promise((resolve) => {
          const reader = new FileReader()
          reader.onload = () => resolve({ path: subPath, data: reader.result as ArrayBuffer })
          reader.readAsArrayBuffer(f)
        })
      )
    }
    const results = await Promise.all(fileReads)
    const filesParam: Record<string, string> = {}
    for (const r of results) {
      const decoder = new TextDecoder('utf-8')
      filesParam[r.path] = decoder.decode(r.data)
    }
    await ioRequest('skill.write', { wid: useWid(), name: skillName, content: '' })
    const writePromises = Object.entries(filesParam).map(([path, content]) => {
      if (path === 'SKILL.md') {
        return ioRequest('skill.write', { wid: useWid(), name: skillName, content })
      }
      return Promise.resolve()
    })
    await Promise.all(writePromises)
    ElMessage.success(i18n.t('skill-setting.import-success'))
    handleDataLoad()
  }
}
</script>
