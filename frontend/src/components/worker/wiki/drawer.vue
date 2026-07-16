<template>
  <div class="drawer"
    :class="{
      'is-dragging': isDragging,
      'is-collapsed': isCollapsed
    }"
    :style="{
      width: loadMenuWidth + 'px'
    }"
  >
    <div class="wrapper inline-flex-c-n-n"
      :style="{
        width: loadMenuWidth + 'px'
      }"
    >
      <div class="head inline-flex-r-n-n">
        <div class="menu inline-flex-r-c-n">
          <a-svg-icon icon-class="disk" size="18px" />
          <div class="name">{{ $t('menu.wiki') }}</div>
        </div>
      </div>
      <div class="context">
        <div class="navigation">
          <div class="navigation-head inline-flex-r-c-n"
            :class="{
              'is-actived': loadOpenedMenu === 'mine'
            }"
            @click="handleMenuSwitch('mine')"
          >
            <el-icon class="arrow" :class="{
              'is-opened': isWikiMineTreeOpened
            }"
              @click.stop="isWikiMineTreeOpened = !isWikiMineTreeOpened"
            >
              <CaretRight />
            </el-icon>
            <a-svg-icon icon-class="disk" size="18px" style="color: #3370ff;" />
            <div class="name">{{ $t('wiki.mine-wiki') }}</div>
            <el-icon class="more" @click.stop="handleNavigationContextView($event)">
              <MoreFilled />
            </el-icon>
          </div>
          <div class="navigation-body" v-show="isWikiMineTreeOpened" v-loading="progression.loading">
            <el-tree ref="loadTreeRef" node-key="path" :data="dataList"
              :props="loadDefaultDataProps"
              :highlight-current="true"
              :expand-on-click-node="false"
              @node-click="handleNodeClick"
            >
              <template #default="{ data, node }">
                <div class="module" :title="data.name">
                  <div class="name">
                    <a-svg-icon icon-class="folder-user" />
                    <div v-if="loadEditNode.path == data.path" class="rename">
                      <input ref="loadRenameRef" v-model="loadEditNode.name" type="text"
                        @click.stop
                        @blur="handleXhrEdit(data)"
                        @keydown.escape="loadEditNode = {}"
                        @keyup.enter="(<HTMLElement> $event.target).blur()"
                      />
                    </div>
                    <div v-else class="text">{{ data.name }}</div>
                  </div>
                  <div class="operate inline-flex-r-c-n"
                    :class="{
                      'is-hidden': loadEditNode.path === data.path
                    }"
                  >
                    <el-icon @click.stop="handleContextView($event, node)"
                      :class="{
                        'is-actived': isContextmenuView && loadContextNode.data?.path === data.path
                      }"
                    >
                      <MoreFilled />
                    </el-icon>
                  </div>
                </div>
              </template>
            </el-tree>
          </div>
        </div>
        <div class="navigation">
          <div class="navigation-head inline-flex-r-c-n"
            :class="{
              'is-actived': loadOpenedMenu === 'remote'
            }"
            @click="handleMenuSwitch('remote')"
          >
            <el-icon class="arrow" :class="{
              'is-opened': loadOpenedMenu === 'remote'
            }">
              <CaretRight />
            </el-icon>
            <a-svg-icon icon-class="cloud" size="18px" style="color: #3ad1ff;" />
            <div class="name">{{ $t('wiki.remote-wiki') }}</div>
          </div>
        </div>
      </div>
      <div class="trash">
        <div class="wrapper inline-flex-r-c-n">
          <el-icon><Delete /></el-icon>
          <div class="name">{{ $t('disk.trash') }}</div>
        </div>
      </div>
    </div>
    <div class="flexible" @mousedown="handleResize"></div>
    <div class="caret" @click="handleCollapse">
      <el-icon v-if="!isCollapsed"><CaretLeft /></el-icon>
      <el-icon v-else><CaretRight /></el-icon>
    </div>
    <!-- 导航栏右键菜单 -->
    <a-contextmenu ref="loadNavContextmenuRef" :visible="isNavContextmenuView" mode="create"
      @add="handleNavContextAdd"
      @close="isNavContextmenuView = false"
    ></a-contextmenu>
    <!-- 目录/文档右键菜单 -->
    <a-contextmenu ref="loadContextmenuRef" :data="loadContextNode" :visible="isContextmenuView"
      @close="isContextmenuView = false"
      @add="handleContextAdd"
      @rename="handleContextRename"
      @moveto="handleContextMoveTo"
      @delete="handleContextDelete"
    ></a-contextmenu>
    <!-- 移动到弹窗 -->
    <a-window :title="$t('wiki.move-to')" :visible="isWindowView" :wid="useWid()"
      @close="isWindowView = false"
      @submit="handleMoveToSubmit"
    ></a-window>
  </div>
</template>

<script setup lang="ts">
import type {
  ElTree
} from 'element-plus'
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import {
  Delete,
  CaretLeft,
  CaretRight,
  MoreFilled
} from '@element-plus/icons-vue'
import {
  useDraggingResize
} from '@/hooks/useDraggingResize'
import useSocketIO from '@/hooks/useSocketIO'
import AContextmenu from './contextmenu.vue'
import AWindow from './window.vue'

const {
  ioRequest,
  progression
} = useSocketIO()
const i18n = useI18n()
const route = useRoute()
const router = useRouter()

const {
  width: loadMenuWidth,
  isDragging,
  handleResize
} = useDraggingResize({ initialWidth: 228, min: 158, max: 420 })

const loadDefaultDataProps = {
  label: 'name',
  isLeaf: 'isFile',
  children: 'children'
}
const isCollapsed = ref(false)
const dataList = ref<any[]>([])
const loadOpenedMenu = ref('mine')
const isWikiMineTreeOpened = ref(false)
const useWid = () => route.params.id as string
const loadTreeRef = ref<InstanceType<typeof ElTree>>()
// 当前编辑的节点，包括重命名、删除
const isContextmenuView = ref(false)
const loadEditNode = ref<any>({})
const loadContextNode = ref<any>({})
const loadRenameRef = ref<HTMLInputElement>()
const loadContextmenuRef = ref<InstanceType<typeof AContextmenu>>()
// 导航栏右键菜单
const isNavContextmenuView = ref(false)
const loadNavContextmenuRef = ref<InstanceType<typeof AContextmenu>>()
// 移动到弹窗状态
const isWindowView = ref(false)
const loadMoveNode = ref<any>(null)

const handleDataLoad = () => {
  const params = {
    wid: useWid()
  }
  return ioRequest('wiki.index', params).then((result) => {
    dataList.value = result[0]
  })
}
const handleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
const handleMenuSwitch = (menu: string) => {
  const wid = useWid()
  if (loadOpenedMenu.value === menu) {
    // 已经在当前菜单，点击则重置回根目录
    if (menu === 'mine') {
      // 清除树节点选中状态
      handleCurrentNodeSet(undefined as any)
      router.push({ path: `/worker/wiki/mine/${wid}`, query: { path: '/', refresh: String(Math.random()) } })
    }
    return
  }
  loadOpenedMenu.value = menu
  if (menu === 'mine') {
    router.push({ path: `/worker/wiki/mine/${wid}`, query: { path: '/' } })
  } else {
    // 切换到remote时清除树节点选中状态
    loadTreeRef.value?.setCurrentKey(undefined as any)
    router.push(`/worker/wiki/remote/${wid}`)
  }
}
const handleNodeClick = (data: any) => {
  const wid = useWid()
  if (route.name !== 'worker-wiki-mine') {
    loadOpenedMenu.value = 'mine'
    router.push({ path: `/worker/wiki/mine/${wid}`, query: { path: data.path } })
  } else {
    handleCurrentNodeSet(data.path, true)
  }
}
const handleCurrentNodeSet = (path: string, push = false) => {
  nextTick(() => {
    loadTreeRef.value?.setCurrentKey(path)
    if (path) isWikiMineTreeOpened.value = true
  })
  if (push) {
    router.push({
      query: { path: path }
    })
  }
}
const handleNavigationContextView = (event: MouseEvent) => {
  isNavContextmenuView.value = true
  nextTick(() => {
    const rect = (<HTMLElement> event.currentTarget).getBoundingClientRect()
    const maxHeight = 100
    let top = rect.top + 32
    let left = rect.left - 20
    if ((top + maxHeight) > document.body.clientHeight) {
      top = document.body.clientHeight - maxHeight
    }
    loadNavContextmenuRef.value?.handlePositionUpdate(left, top)
  })
}
const handleNavContextAdd = (_data: any, type: string) => {
  isNavContextmenuView.value = false
  const isMarkdown = type === 'markdown'
  const title = isMarkdown ? i18n.t('wiki.new-markdown') : i18n.t('wiki.new-folder')
  const defaultName = isMarkdown ? i18n.t('wiki.untitled-markdown') : i18n.t('wiki.untitled')
  ElMessageBox.prompt(
    '',
    title,
    {
      inputValue: defaultName,
      inputPattern: /\S+/,
      inputErrorMessage: i18n.t('common.tips'),
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(({ value }) => {
    const name = value.trim()
    handleXhrNavAddDocument(name, isMarkdown)
  }).catch(() => {})
}
const handleXhrNavAddDocument = async (name: string, isMarkdown: boolean) => {
  const command = isMarkdown ? 'wiki.create' : 'wiki.mkdir'
  const fileName = isMarkdown && !name.endsWith('.md') ? `${name}.md` : name
  const params = {
    wid: useWid(),
    path: '',
    name: fileName
  }
  const result = await ioRequest(command, params)
  const success = Array.isArray(result) ? result[0] : result
  if (!success) {
    ElMessage.error(i18n.t('wiki.add-document-failed'))
    return
  }
  ElMessage.success(i18n.t(isMarkdown ? 'wiki.new-markdown' : 'wiki.new-folder') + ' ✓')
  // 仅目录才插入节点到树中，文档不显示在目录树
  if (!isMarkdown) {
    const newNode = {
      name: fileName,
      path: fileName,
      isFile: false,
      children: []
    }
    dataList.value.push(newNode)
  }
  nextTick(() => {
    if (isMarkdown) {
      router.push({ query: { path: '/', file: fileName, refresh: String(Date.now()) } })
    } else {
      handleCurrentNodeSet(fileName)
      router.push({ query: { path: fileName, refresh: String(Date.now()) } })
    }
  })
}
const handleContextView = (event: MouseEvent, node?: any) => {
  isContextmenuView.value = true
  loadContextNode.value = node
  nextTick(() => {
    const rect = (<HTMLElement> event.currentTarget).getBoundingClientRect()
    const maxHeight = 200
    let posY = rect.top + 32
    let posX = rect.left - 20
    const contextmenu = loadContextmenuRef.value
    if ((posY + maxHeight) > document.body.clientHeight) {
      posY = document.body.clientHeight - maxHeight
    }
    contextmenu?.handlePositionUpdate(posX, posY)
  })
}
const handleXhrEdit = async (data: any) => {
  const editNode = loadEditNode.value
  const newName = editNode.name?.trim()
  // 重置编辑状态
  loadEditNode.value = {}
  // 名称未变或为空则跳过
  if (!newName || newName === data.name) {
    return
  }
  const wid = useWid()
  const params = {
    wid: wid,
    path: data.path,
    name: newName
  }
  const result = await ioRequest('wiki.rename', params)
  const success = Array.isArray(result) ? result[0] : result
  if (!success) {
    ElMessage.error(i18n.t('wiki.rename-failed'))
    return
  }
  // 计算新路径
  const oldPath = data.path
  const parts = oldPath.split('/')
  parts.pop()
  const parentPath = parts.join('/')
  const newPath = parentPath ? `${parentPath}/${newName}` : newName
  // 递归更新path前缀
  const updateNodePath = (node: any, oldPrefix: string, newPrefix: string) => {
    node.path = node.path.replace(oldPrefix, newPrefix)
    if (node.children) {
      for (const child of node.children) {
        updateNodePath(child, oldPrefix, newPrefix)
      }
    }
  }
  // 在数据源中找到并替换节点（通过重新赋值触发响应式更新，确保el-tree的node-key映射刷新）
  const findAndReplace = (list: any[], targetPath: string): boolean => {
    for (let i = 0; i < list.length; i++) {
      if (list[i].path === targetPath) {
        const node = { ...list[i], name: newName, path: newPath, children: list[i].children ? [...list[i].children] : [] }
        if (node.children.length > 0) {
          for (const child of node.children) {
            updateNodePath(child, oldPath, newPath)
          }
        }
        list.splice(i, 1, node)
        return true
      }
      if (list[i].children && findAndReplace(list[i].children, targetPath)) {
        return true
      }
    }
    return false
  }
  findAndReplace(dataList.value, oldPath)
  nextTick(() => {
    loadTreeRef.value?.setCurrentKey(newPath)
  })
  // 更新右侧workspace
  const currentPath = route.query.path as string
  if (currentPath === oldPath || currentPath?.startsWith(oldPath + '/')) {
    const updatedPath = currentPath.replace(oldPath, newPath)
    router.replace({ query: { ...route.query, path: updatedPath, refresh: String(Date.now()) } })
  } else {
    // 当前浏览的是被重命名节点的父目录，强制刷新文件列表
    router.replace({ query: { ...route.query, refresh: String(Date.now()) } })
  }
}
const handleContextRename = (node: any) => {
  isContextmenuView.value = false
  const data = node?.data
  if (!data) return
  loadEditNode.value = { path: data.path, name: data.name }
  nextTick(() => {
    unref(loadRenameRef)?.focus()
  })
}
const handleContextAdd = (node: any, type: string) => {
  isContextmenuView.value = false
  const parentPath = node?.data?.path || ''
  const isMarkdown = type === 'markdown'
  const title = isMarkdown ? i18n.t('wiki.new-markdown') : i18n.t('wiki.new-folder')
  const defaultName = isMarkdown ? i18n.t('wiki.untitled-markdown') : i18n.t('wiki.untitled')
  ElMessageBox.prompt(
    '',
    title,
    {
      inputValue: defaultName,
      inputPattern: /\S+/,
      inputErrorMessage: i18n.t('common.tips'),
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(({ value }) => {
    const name = value.trim()
    handleXhrAddDocument(name, isMarkdown, parentPath)
  }).catch(() => {})
}
const handleXhrAddDocument = async (name: string, isMarkdown: boolean, parentPath: string) => {
  const command = isMarkdown ? 'wiki.create' : 'wiki.mkdir'
  const fileName = isMarkdown && !name.endsWith('.md') ? `${name}.md` : name
  const params = {
    wid: useWid(),
    path: parentPath,
    name: fileName
  }
  const result = await ioRequest(command, params)
  const success = Array.isArray(result) ? result[0] : result
  if (!success) {
    ElMessage.error(i18n.t('wiki.add-document-failed'))
    return
  }
  ElMessage.success(i18n.t(isMarkdown ? 'wiki.new-markdown' : 'wiki.new-folder') + ' ✓')
  // 新节点的完整路径
  const newPath = parentPath ? `${parentPath}/${fileName}` : fileName
  // 仅目录才插入节点到树中，文档不显示在目录树
  if (!isMarkdown) {
    const newNode = {
      name: fileName,
      path: newPath,
      isFile: false,
      children: []
    }
    if (parentPath) {
      // 插入到父节点的children中
      const parentNode = loadTreeRef.value?.getNode(parentPath)
      if (parentNode) {
        if (!parentNode.data.children) {
          parentNode.data.children = []
        }
        parentNode.data.children.push(newNode)
        // 展开父节点
        parentNode.expanded = true
      }
    } else {
      // 无父路径，插入到根目录
      dataList.value.push(newNode)
    }
  }
  nextTick(() => {
    if (isMarkdown) {
      router.push({ query: { path: parentPath, file: fileName, refresh: String(Date.now()) } })
    } else {
      handleCurrentNodeSet(newPath)
      router.push({ query: { path: newPath, refresh: String(Date.now()) } })
    }
  })
}
const handleContextMoveTo = (node: any) => {
  isContextmenuView.value = false
  const data = node?.data
  if (!data) return
  loadMoveNode.value = node
  isWindowView.value = true
}
const handleMoveToSubmit = async (targetPath: string) => {
  isWindowView.value = false
  const node = loadMoveNode.value
  const data = node?.data
  if (!data) return
  const params = {
    wid: useWid(),
    path: data.path,
    target: targetPath || '/'
  }
  const result = await ioRequest('wiki.move', params)
  const success = Array.isArray(result) ? result[0] : result
  if (!success) {
    ElMessage.error(i18n.t('wiki.move-failed'))
    return
  }
  ElMessage.success(i18n.t('wiki.move-to') + ' ✓')
  // 从树中移除节点
  loadTreeRef.value?.remove(data.path)
  // 刷新右侧文件列表
  router.replace({ query: { ...route.query, refresh: String(Date.now()) } })
}
const handleContextDelete = (node: any) => {
  isContextmenuView.value = false
  const data = node?.data
  if (!data) return
  ElMessageBox.confirm(
    `${i18n.t('common.delete')}: ${data.name}?`,
    i18n.t('common.tips'),
    {
      type: 'warning',
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(() => {
    handleXhrDocumentRemove(data)
  }).catch(() => {})
}
const handleXhrDocumentRemove = (data: any) => {
  const wid = useWid()
  const params = {
    wid: wid,
    path: data.path
  }
  ioRequest('wiki.delete', params).then((result: any) => {
    const success = Array.isArray(result) ? result[0] : result
    if (success) {
      ElMessage.success(i18n.t('common.delete') + ' ✓')
      // 本地动态移除节点
      loadTreeRef.value?.remove(data.path)
      // 删除后跳转到父目录
      const parts = data.path.split('/')
      parts.pop()
      const parentPath = parts.join('/')
      router.push({ query: { path: parentPath || '/', refresh: String(Date.now()) } })
    }
  })
}

onMounted(() => {
  // 刷新时如果URL有path参数，自动展开目录树菜单
  const queryPath = route.query.path as string
  handleDataLoad().then(() => {
    // 数据加载完成后，根据当前path高亮并展开对应节点
    if (queryPath && queryPath !== '/' && route.name === 'worker-wiki-mine') {
      nextTick(() => {
        // 高亮当前节点
        handleCurrentNodeSet(queryPath)
      })
    }
  })
})

// 监听路由变化，高亮对应节点
watch(() => route.query.path, (path) => {
  if (loadOpenedMenu.value !== 'mine') return
  if (path && path !== '/') {
    nextTick(() => {
      handleCurrentNodeSet(path as string)
    })
  } else {
    nextTick(() => {
      handleCurrentNodeSet(undefined as any)
    })
  }
})
watch(() => route.name, (name) => {
  if (name === 'worker-wiki-remote') {
    loadOpenedMenu.value = 'remote'
  } else if (name === 'worker-wiki-mine') {
    loadOpenedMenu.value = 'mine'
  }
}, { immediate: true })
</script>
