import { SocketIOOption } from './interface'
import { io, Socket } from '@/websocket/index'
import { ElNotification } from 'element-plus'
import i18n from '@/locale'

const { t } = i18n.global
const messagePosition = 'bottom-left'

class SocketIO {
  private socket: Socket | null = null

  private initPromise: Promise<void> | null = null

  constructor(options?: SocketIOOption) {
    this.initPromise = new Promise((resolve, reject) => {
      if (this.socket && this.socket.isConnected()) {
        return resolve()
      }
      if (!options?.remoteEndpoint) {
        reject(new Error('SocketIO: options.remoteEndpoint is required'))
        return
      }
      options.remoteEndpoint.then((socketUrl) => {
        const handshakePromise = options?.handshakeData || Promise.resolve({})
        handshakePromise.then((query) => {
          this.socket = io(socketUrl, {
            reconnectOn: options?.reconnectOn,
            reconnectAttempts: options?.reconnectAttempts,
            query
          })
          this.socket.on('connect', () => {
            resolve()
          })
          this.socket.on('disconnect', () => {
            this.initPromise = null
            ElNotification.error({
              title: t('common.error'),
              message: t('error.network-error'),
              position: messagePosition
            })
            reject(new Error('Socket connection failed after all reconnect attempts'))
          })
        }).catch((cause) => {
          this.initPromise = null
          reject(cause)
        })
      }).catch((cause) => {
        this.initPromise = null
        reject(cause)
      })
    })
  }

  /**
   * 发送指令，适用于单向指令发送和指令监听
   * 
   * @param id         指令 ID
   * @param command    指令名称
   * @param parameters 指令参数
   */
  public async commit(id: string | null, command: string, parameters: any[]): Promise<void> {
    await this.initPromise
    if (!this.socket) {
      throw new Error('Socket initialization failed')
    }
    this.socket.commit(id, command, parameters)
  }

  /**
   * 发送指令，并返回一个 Promise 对象，适用于需要响应的双向指令
   * 
   * @param   command    指令名称
   * @param   parameters 指令参数
   * @returns Promise 对象，resolve 时传递响应数据，reject 时传递错误信息
   */
  public async request<T = any>(command: string, ...parameters: any): Promise<T> {
    await this.initPromise
    if (!this.socket) {
      throw new Error('Socket initialization failed')
    }
    return this.socket!.request(command, ...parameters)
  }

  /**
   * 监听指令
   *
   * @param command 指令名称
   * @param fn      指令处理函数
   */
  public on(command: string, fn: (...args: any[]) => any): this {
    if (this.socket) {
      this.socket.on(command, fn)
    } else {
      this.initPromise?.then(() => this.socket?.on(command, fn))
    }
    return this
  }

  /**
   * 取消监听指令
   *
   * @param command 指令名称
   * @param fn      指令处理函数，如果不传，则删除该指令的所有处理函数
   */
  public off(command: string, fn?: (...args: any[]) => any): this {
    this.socket?.off(command, fn)
    return this
  }

  /**
   * 断开 Socket 连接
   */
  public disconnect(): void {
    this.socket?.disconnect()
    this.socket = null
    this.initPromise = null
  }

  /**
   * 等待连接就绪，可用于在调用其他方法前确保连接已建立
   */
  public ready(): Promise<void> {
    return this.initPromise ?? Promise.reject(new Error('SocketIO: not initialized'))
  }
}

export default SocketIO
