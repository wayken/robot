import axios, {
  AxiosInstance,
  AxiosError,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
  AxiosResponse
} from 'axios'
import i18n from '@/locale'
import JSONBig from 'json-bigint'
import { ElNotification } from 'element-plus'
import { ResultData } from '@/api/interface'
import { ResultEnum } from '@/api/enum'

const config = {
  // 默认地址请求地址，可在 .env.** 文件中修改
  baseURL: import.meta.env.VITE_API_URL as string,
  // 设置超时时间
  timeout: ResultEnum.TIMEOUT as number,
  transformResponse: (data: any) => {
    try {
      return JSONBig.parse(data)
    } catch (err) {
      console.log(err)
      return JSON.parse(data)
    }
  }
}
const { t } = i18n.global
const messagePosition = 'bottom-left'

export interface SystemAxiosRequestConfig extends InternalAxiosRequestConfig {
  loading?: boolean
}

export class HttpRequest {
  private service: AxiosInstance

  constructor(config: AxiosRequestConfig) {
    this.service = axios.create(config)

    /**
     * 请求拦截器
     * 客户端发送请求 -> [请求拦截器] -> 服务器
     * token校验(JWT) : 接受服务器返回的 token,存储到 vuex/pinia/本地储存当中
     */
    this.service.interceptors.request.use(
      (config: SystemAxiosRequestConfig) => {
        return config
      },
      (error: AxiosError) => {
        return Promise.reject(error)
      }
    )

    /**
     * @description 响应拦截器
     * 服务器换返回信息 -> [拦截统一处理] -> 客户端JS获取到信息
     */
    this.service.interceptors.response.use(
      (response: AxiosResponse) => {
        const { data } = response
        // 全局错误信息拦截（防止下载文件的时候返回数据流，没有 code 直接报错）
        if (data.code && data.code !== ResultEnum.SUCCESS) {
          const i18nKey = 'code.' + data.code
          let i18nValue = t('code.' + data.code)
          if (i18nKey === i18nValue) {
            i18nValue = t('code.unknow')
          }
          ElNotification.error({
            title: t('common.error'),
            message: i18nValue,
            position: messagePosition
          })
          return Promise.reject(data)
        }
        // 成功请求（在页面上除非特殊情况，否则不用处理失败逻辑）
        return data
      },
      async (error: AxiosError) => {
        if (error.response) {
          // 服务器返回的错误
          let i18nValue
          switch (error.response.status) {
            case 404:
              i18nValue = t('error.http-status-404')
              break
            default:
              i18nValue = t('error.http-status-500')
          }
          ElNotification.error({
            title: t('common.error'),
            message: i18nValue,
            position: messagePosition
          })
        } else {
          // 网络连接异常错误
          ElNotification.error({
            title: t('common.error'),
            message: t('error.http-status-400'),
            position: messagePosition
          })
        }
        return Promise.reject(error)
      }
    )
  }

  /**
   * 常用请求方法封装
   */
  get<T>(url: string, params?: object, _object = {}): Promise<ResultData<T>> {
    return this.service.get(url, { params, ..._object })
  }
  post<T>(url: string, params?: object | string, _object = {}): Promise<ResultData<T>> {
    return this.service.post(url, params, _object)
  }
}

export default new HttpRequest(config)
