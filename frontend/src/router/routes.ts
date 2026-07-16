import type {
  RouteRecordRaw
} from 'vue-router'
import MainLayout from '@/pages/main.vue'
import HomeLayout from '@/pages/home/index.vue'
import WorkerLayout from '@/pages/worker/index.vue'

/**
 * 菜单路由配置，各配置项说明如下
 * meta:
 *   icon:   菜单图标
 *   lang:   菜单标题(多语言)
 *   hidden: 是否隐藏菜单
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/main',
    name: 'main',
    component: MainLayout
  },
  {
    path: '/',
    name: 'index',
    component: HomeLayout,
    redirect: '/assistant',
    children: [
      {
        name: 'dashboard',
        path: '/dashboard',
        meta: {
          keepalive: true,
          lang: 'menu.dashboard'
        },
        component: () => import('@/pages/home/dashboard/index.vue')
      },
      {
        name: 'logger',
        path: '/logger',
        meta: {
          keepalive: true,
          lang: 'menu.logger'
        },
        component: () => import('@/pages/home/logger/index.vue')
      },
      {
        name: 'provider',
        path: '/provider',
        meta: {
          keepalive: true,
          lang: 'menu.provider'
        },
        component: () => import('@/pages/home/provider/index.vue')
      },
      {
        name: 'assistant',
        path: '/assistant',
        meta: {
          keepalive: true,
          lang: 'menu.assistant'
        },
        component: () => import('@/pages/home/assistant/index.vue')
      },
      {
        name: 'node',
        path: '/node',
        meta: {
          keepalive: true,
          lang: 'menu.node'
        },
        component: () => import('@/pages/home/node/index.vue')
      },
      {
        name: 'channel',
        path: '/channel',
        meta: {
          keepalive: true,
          lang: 'menu.channel'
        },
        component: () => import('@/pages/home/channel/index.vue')
      },
      {
        name: 'authority',
        path: '/authority',
        meta: {
          keepalive: true,
          lang: 'menu.authority'
        },
        component: () => import('@/pages/home/authority/index.vue')
      },
      {
        name: 'wiki',
        path: '/wiki',
        meta: {
          keepalive: true,
          lang: 'menu.wiki'
        },
        component: () => import('@/pages/home/wiki/index.vue')
      },
      {
        name: 'summary',
        path: '/summary',
        meta: {
          keepalive: true,
          lang: 'menu.summary'
        },
        component: () => import('@/pages/home/summary/index.vue')
      },
      {
        path: '/skill',
        name: 'skill',
        meta: {
          keepalive: true,
          lang: 'menu.skill'
        },
        component: () => import('@/pages/home/skill/index.vue')
      },
      {
        path: '/setting',
        name: 'setting',
        meta: {
          keepalive: true,
          lang: 'menu.setting'
        },
        component: () => import('@/pages/home/setting/index.vue')
      }
    ]
  },
  {
    name: 'worker',
    path: '/worker/:id',
    component: WorkerLayout,
    redirect: to => {
      return `/worker/chat/${to.params.id}`
    },
    children: [
      {
        name: 'worker-chat',
        path: '/worker/chat/:id',
        meta: {
          keepalive: true,
          lang: 'menu.chat'
        },
        component: () => import('@/pages/worker/chat/index.vue')
      },
      {
        name: 'worker-disk',
        path: '/worker/disk/:id',
        meta: {
          keepalive: true,
          lang: 'menu.disk'
        },
        component: () => import('@/pages/worker/disk/index.vue'),
        redirect: to => {
          return `/worker/disk/mine/${to.params.id}`
        },
        children: [
          {
            path: '/worker/disk/mine/:id',
            name: 'worker-disk-mine',
            meta: {
              keepalive: true
            },
            component: () => import('@/pages/worker/disk/mine.vue')
          },
          {
            path: '/worker/disk/trash/:id',
            name: 'worker-disk-trash',
            meta: {
              keepalive: true
            },
            component: () => import('@/pages/worker/disk/trash.vue')
          }
        ]
      },
      {
        name: 'worker-wiki',
        path: '/worker/wiki/:id',
        meta: {
          keepalive: true,
          lang: 'menu.wiki'
        },
        component: () => import('@/pages/worker/wiki/index.vue'),
        redirect: to => {
          return `/worker/wiki/mine/${to.params.id}`
        },
        children: [
          {
            path: '/worker/wiki/mine/:id',
            name: 'worker-wiki-mine',
            meta: {
              keepalive: true
            },
            component: () => import('@/pages/worker/wiki/mine.vue')
          },
          {
            path: '/worker/wiki/remote/:id',
            name: 'worker-wiki-remote',
            meta: {
              keepalive: true
            },
            component: () => import('@/pages/worker/wiki/remote.vue')
          },
          {
            path: '/worker/wiki/trash/:id',
            name: 'worker-wiki-trash',
            meta: {
              keepalive: true
            },
            component: () => import('@/pages/worker/wiki/trash.vue')
          }
        ]
      },
      {
        name: 'worker-task',
        path: '/worker/task/:id',
        meta: {
          keepalive: true,
          lang: 'menu.task'
        },
        component: () => import('@/pages/worker/task/index.vue')
      },
      {
        name: 'worker-subworker',
        path: '/worker/subworker/:id',
        meta: {
          keepalive: true,
          lang: 'menu.subworker'
        },
        component: () => import('@/pages/worker/subworker/index.vue')
      },
      {
        name: 'worker-summary',
        path: '/worker/summary/:id',
        meta: {
          keepalive: true,
          lang: 'menu.summary'
        },
        component: () => import('@/pages/worker/summary/index.vue')
      },
      {
        name: 'worker-setting',
        path: '/worker/setting/:id',
        meta: {
          keepalive: true,
          lang: 'menu.worker'
        },
        component: () => import('@/pages/worker/setting/index.vue'),
        redirect: to => {
          return `/worker/setting/toolkit/${to.params.id}`
        },
        children: [
          {
            path: '/worker/setting/skill/:id',
            name: 'worker-setting-skill',
            component: () => import('@/pages/worker/setting/skill.vue')
          },
          {
            path: '/worker/setting/toolkit/:id',
            name: 'worker-setting-toolkit',
            component: () => import('@/pages/worker/setting/toolkit.vue')
          },
          {
            path: '/worker/setting/rule/:id',
            name: 'worker-setting-rule',
            component: () => import('@/pages/worker/setting/rule.vue')
          },
          {
            path: '/worker/setting/memory/:id',
            name: 'worker-setting-memory',
            component: () => import('@/pages/worker/setting/memory.vue')
          },
          {
            path: '/worker/setting/plugin/:id',
            name: 'worker-setting-plugin',
            component: () => import('@/pages/worker/setting/plugin.vue')
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/error/404.html'
  },
  {
    path: '/error/404.html',
    component: () => import('@/layouts/error.vue')
  },
  {
    path: '/error/400.html',
    component: () => import('@/layouts/400.vue')
  }
]

export default routes
