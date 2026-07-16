import request from '@/api'
import i18n from '@/locale'
import {
  Loading
} from '@element-plus/icons-vue'
import { ResultData } from '@/api/interface'
import { ElMessage, ElNotification } from 'element-plus'

export interface ILoadOption {
  onMessage?: any
}

export interface IPostOption {
  onMessage?: any
}

export const useRequest = () => {
  const progression = reactive({
    // 当前是否正在加载请求
    loading: false,
    // 当前是否正在批量加载请求，由业务定义
    batchLoading: true,
    // 当前是否正在发送加载请求
    sending: false,
    // 当前是否正在批量发送加载请求，由业务定义
    batchSending: false,
    // 当前加载请求是否成功
    success: true
  })

  const ioload = (module: string, key: string, params: any, option?: ILoadOption): Promise<any> => {
    const useRequest = request[module] ? request[module][key] : null
    if (!useRequest) {
      throw Error(`require ioload $request['${module}']['${key}']`)
    }
    progression.loading = true
    progression.success = true
    // onMessage: 业务定义是否显示请求信息，默认为空不显示
    const onMessage = option ? option.onMessage : null
    if (onMessage) {
      const loadingTips = onMessage.loadingTips
      if (loadingTips) {
        ElMessage({
          icon: Loading,
          showClose: true,
          message: loadingTips
        })
      }
    }
    return new Promise((resolve, reject) => {
      useRequest(params).then((result: ResultData) => {
        if (onMessage) {
          const { t } = i18n.global
          const successTips = onMessage.successTips ? onMessage.successTips : t('error.request-success-tips')
          ElNotification.success({
            title: t('error.system-tips'),
            message: successTips,
            position: 'bottom-left'
          })
        }
        resolve(result.result)
      }).catch((error: any) => {
        reject(error)
        progression.success = false
      }).finally(() => {
        progression.loading = false
      })
    })
  }

  const iopost = (module: string, key: string, params: any, option?: IPostOption): Promise<any> => {
    const useRequest = request[module] ? request[module][key] : null
    if (!useRequest) {
      throw Error(`require iopost $request['${module}']['${key}']`)
    }
    progression.sending = true
    progression.success = true
    const onMessage = option ? option.onMessage : null
    if (onMessage) {
      const sendingTips = onMessage.sendingTips
      if (sendingTips) {
        ElMessage({
          icon: Loading,
          showClose: true,
          message: sendingTips
        })
      }
    }
    return new Promise((resolve, reject) => {
      useRequest(params).then((result: ResultData) => {
        if (onMessage) {
          const { t } = i18n.global
          const successTips = onMessage.successTips ? onMessage.successTips : t('error.request-success-tips')
          ElNotification.success({
            title: t('error.system-tips'),
            message: successTips,
            position: 'bottom-left'
          })
        }
        resolve(result.result)
      }).catch((error: any) => {
        reject(error)
        progression.success = false
      }).finally(() => {
        progression.sending = false
      })
    })
  }

  return {
    ioload,
    iopost,
    progression
  }
}
