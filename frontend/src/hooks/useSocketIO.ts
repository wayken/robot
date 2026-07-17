import SocketIO from '@/api/socketio'
import { SocketError } from '@/websocket/index'
import { ElNotification } from 'element-plus'
import i18n from '@/locale'

const { t } = i18n.global
const messagePosition = 'bottom-left'

export default function useSocketIO() {
  const context: any = inject('context')
  const socketIO: SocketIO = context.socketIO

  const progression = reactive({
    success: true,
    loading: false
  })

  const ioRequest = <T = any>(command: string, ...parameters: any): Promise<T> => {
    progression.loading = true
    progression.success = true
    return socketIO.request<T>(command, ...parameters).then((result) => {
      return result
    }).catch((error) => {
      progression.success = false
      // 显示全局错误通知，与 HTTP 请求保持一致的错误提示体验
      if (error instanceof SocketError) {
        const i18nKey = 'code.' + error.code
        let i18nValue = t(i18nKey)
        if (i18nKey === i18nValue) {
          // 如果没有对应的i18n翻译，使用后端返回的错误信息
          i18nValue = error.message || t('code.unknow')
        }
        ElNotification.error({
          title: t('common.error'),
          message: i18nValue,
          position: messagePosition
        })
      } else {
        ElNotification.error({
          title: t('common.error'),
          message: error.message || t('error.network-error'),
          position: messagePosition
        })
      }
      throw error
    }).finally(() => {
      progression.loading = false
    })
  }

  const ioCommit = (command: string, ...parameters: any): void => {
    socketIO.commit(null, command, parameters)
  }

  const ioOn = (command: string, fn: (...args: any[]) => any): void => {
    socketIO.on(command, fn)
    onUnmounted(() => {
      socketIO.off(command, fn)
    })
  }

  return {
    ioOn,
    ioCommit,
    ioRequest,
    progression
  }
}
