import { uuid } from '@/utils/dom'

export interface SocketOptions {
  reconnectOn?: boolean
  reconnectAttempts?: number
  reconnectInterval?: number
  requestTimeout?: number
  query?: Record<string, string>
}

/**
 * 系统指令类型，各指令类型说明如下：
 * 1. HANDSHAKE: 建立WebSocket连接后的握手数据传输，
 *  业务可通过socket.on('handshake', fn...)来实现建立连接过程中的降级、权限校验等
 * 2. COMMAND：业务自定义指令，包括指令消息数据
 * 3. DISCONNECT：主动断开连接指令，发送断开连接的数据包后客户端不会再主动重连
 * 4. ERROR：业务异常指令，一般指后端业务出现系统异常等
 */
export class SocketCommandType {
  public static readonly HANDSHAKE = 0
  public static readonly COMMAND = 1
  public static readonly DISCONNECT = 2
  public static readonly ERROR = 3
}

// 头部固定长度：VERSION(1) + COMMAND_TYPE(1) + STATUS(2) = 4 bytes
const DATA_HEADER_SIZE = 4
const PACKET_SEPARATOR = '#'

/**
 * WebSocket 自定义二进制通讯协议封装类
 */
export class Socket {
  // WebSocket 连接地址
  private url: string

  // 是否自动连接
  private reconnectOn: boolean

  // 指令监听器列表，采用Map结构，key为指令名称，value为指令处理函数数组，即[fn1, fn2, ...]
  // 系统指令包括：handshake、connect、disconnect、error
  private listeners: { [command: string]: ((...args: any[]) => any)[] } = {}

  // 任意指令监听器列表，采用数组结构，即[fn1, fn2, ...]
  private anyListeners: Array<(...args: any[]) => void> = []

  // 待响应的 request 请求，key 为 uuid，value 为 { resolve, reject, timeout }
  private pendingRequests: Map<string, {
    resolve: (value: any) => void;
    reject: (reason: any) => void;
    timeout?: ReturnType<typeof setTimeout>
  }> = new Map()

  // WebSocket 实例
  private socket: WebSocket | undefined

  // 当前协议版本号
  private version: number = 1

  // 待接收附件的消息队列，每个条目包含元数据、参数、状态码和已收集的附件
  private pendingAttachmentQueue: Array<{
    metadata: Record<string, any>
    parameters: any[]
    status: number
    attachments: ArrayBuffer[]
    total: number
  }> = []

  // 当前连接是否为主动发送数据包断开，如果是，则不再重连
  private disconnectable: boolean = false

  // request 请求的默认超时时间，单位为毫秒，-1 表示不超时
  private requestTimeout: number

  // 当连接因为网络异常断开时的重连次数，-1即表示不限制重连次数
  private defaultReconnectAttempts: number
  private reconnectAttempts: number

  // 每次重连之间的时间间隔，单位为毫秒
  private reconnectInterval: number

  constructor(url: string, opts?: SocketOptions) {
    const options: any = opts || {}
    // url地址修正，当url不以ws://或wss://开头时，自动添加ws://前缀
    // 当url以http://或https://开头时，自动替换为ws://或wss://
    if (/^wss?:\/\//.test(url)) {
      this.url = url
    } else if (/^https?:\/\//.test(url)) {
      this.url = url.replace(/^http:/, 'ws:').replace(/^https:/, 'wss:')
    } else {
      this.url = `ws://${url}`
    }
    // 将 query 参数拼接到 URL
    if (options.query && Object.keys(options.query).length > 0) {
      const params = new URLSearchParams(options.query).toString()
      this.url += (this.url.includes('?') ? '&' : '?') + params
    }
    this.reconnectOn = options.reconnectOn || true
    this.defaultReconnectAttempts = options.reconnectAttempts || -1
    this.reconnectAttempts = this.defaultReconnectAttempts
    this.reconnectInterval = options.reconnectInterval || 1000
    this.requestTimeout = options.requestTimeout ?? 30000
    if (this.reconnectOn) {
      this.open()
    }
  }

  // 建立 WebSocket 连接
  public open() {
    // 创建 WebSocket 连接
    this.socket = new WebSocket(this.url)
    this.socket.onopen = () => {
      // 连接建立时，重置状态
      this.disconnectable = false
      this.reconnectAttempts = this.defaultReconnectAttempts
      // 调用所有指令处理函数
      const listeners = this.listeners.connect || []
      for (const listener of listeners) {
        listener()
      }
    }
    this.socket.onclose = () => {
      // 连接断开时，如果是主动发送数据包断开，则触发disconnect指令，否则触发重连
      if (this.disconnectable) {
        const listeners = this.listeners.disconnect || []
        for (const listener of listeners) {
          listener()
        }
        this.handleAllPendingRequestsReject(new Error('WebSocket connection closed'))
      } else {
        if (!this.reconnectOn || this.reconnectAttempts === 0) {
          const listeners = this.listeners.disconnect || []
          for (const listener of listeners) {
            listener()
          }
          this.handleAllPendingRequestsReject(new Error('WebSocket connection closed'))
          return
        }
        // 间隔一定时间后重连
        if (this.reconnectAttempts > 0) {
          this.reconnectAttempts--
        }
        // 为了保证后端服务的压力，重连时采用指数退避算法，每次重连时间间隔乘以随机因子
        const factor = 1 + Math.random()
        setTimeout(() => {
          this.open()
        }, this.reconnectInterval * factor)
      }
    }
    this.socket.onmessage = (command) => {
      // 解析数据
      const reader = new FileReader()
      reader.onload = () => {
        const buffer = reader.result as ArrayBuffer
        this.handleIncomingBuffer(buffer)
      }
      reader.readAsArrayBuffer(command.data)
    }
    this.socket.onerror = (error) => {
      // 调用所有指令处理函数
      const listeners = this.listeners.error || []
      for (const listener of listeners) {
        listener(error)
      }
    }
  }

  /**
   * 发送指令，不会传递指令ID，适用于不需要响应的单向指令
   * 
   * @param command    指令名称
   * @param parameters 指令参数
   */
  public emit(command: string, ...parameters: any): this {
    return this.commit(null, command, parameters)
  }

  /**
   * 检查 WebSocket 是否已连接
   * 
   * @returns 如果已连接返回 true，否则返回 false
   */
  public isConnected(): boolean {
    return !!(this.socket && this.socket.readyState === WebSocket.OPEN)
  }

  /**
   * 发送指令，并返回一个 Promise 对象，适用于需要响应的双向指令
   * 
   * @param command    指令名称
   * @param parameters 指令参数
   * @returns          Promise 对象，resolve 时传递响应数据，reject 时传递错误信息
   */
  public request<T = unknown>(command: string, ...parameters: any): Promise<T> {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return Promise.reject(new Error('WebSocket is not connected'))
    }
    const id = uuid()
    const promise = new Promise<T>((resolve, reject) => {
      let timeout: ReturnType<typeof setTimeout> | undefined
      if (this.requestTimeout > 0) {
        timeout = setTimeout(() => {
          if (this.pendingRequests.has(id)) {
            this.pendingRequests.delete(id)
            reject(new Error(`Request timeout after ${this.requestTimeout}ms: ${command}`))
          }
        }, this.requestTimeout)
      }
      this.pendingRequests.set(id, { resolve, reject, timeout })
    })
    this.commit(id, command, parameters)
    return promise
  }

  /**
   * 发送数据，
   * WebSocket 二进制数据包，数据包格式如下：
   * +---------+--------------+--------+----------+------+-----------+
   * | VERSION | COMMAND_TYPE | STATUS | METADATA |  -   | PARAMETER |
   * +---------+--------------+--------+----------+------+-----------+
   * | 8 bit   | 8 bit        | 16 bit | bits     | char | bits      |
   * +---------+--------------+--------+----------+------+-----------+
   * 如果后续有附件数据包，则在DATA部分后面会发送一个或多个附件数据包，附件数据包格式如下：
   * +-------------+
   * | Binary Data |
   * +-------------+
   * 头部 HEADER 数据格式（4个字节）如下：
   *     VERSION: 协议版本号，目前为1
   *     COMMAND_TYPE: 指令类型，详见PacketType
   *     STATUS: 状态码，表示处理状态，取值范围为[0, 65535]
   * 当事件类型为 PacketType.COMMAND时会扩展出 METADATA 和 PARAMETER 两个字段，分别表示指令的元数据和参数数据，其中 元数据 METADATA 数据格式如下：
   *   METADATA: JSON对象字符串，包含系统预定义的指令参数和用户自定义的业务参数，数据格式为{"_cmd": "xxx", "_id":xxx, "_num":xxx,...}
   *   其中：
   *     1. _cmd表示指令名称（字符串），示例："CommandChat"
   *     2. _id表示当次请求指令ID（字符串），主要服务于RPC通讯，可选项，示例："123e4567-e89b-12d3-a456-426614174000"
   *     3. _num表示有多少附件发送（整数），示例：2，当有附件时后续会发送_num个附件数据包
   *
   * 参数数据（PARAMETER）数据格式如下：
   *    PARAMETER: 数组格式的参数数据，数组内的数据格式由用户自定义，其中
   *    1. 参数格式为数据列表，示例：["Hello, World!, {"key": "value"}, 123]
   *    2. 参数数据类型由用户自定义，示例：字符串、JSON对象、整数等自由定义
   * NOTE：在元数据和参数数据中间是以"-"字符分隔的，主要是为了方便系统解析 关于附件数据说明：
   *    1. 当业务数据有附带附件时，其中的数据格式需要有点位符，示例：{"message": "Hello, World!", "file": {"_placeholder":true,"_num":0}}
   *    2. 其中_attachment_index表示附件数据包的索引，索引从0开始递增
   *
   * 业务数据格式由业务自定义，任意长度任意数据类型，具体格式由用户自定义， 完整的数据包示例：
   * 1000{"_cmd": "xxx", "_id": "xxx", "_num": 2}-[{"message": "Hello, World!", "file": {"_placeholder":true,"_num":0}}, "xxx", 123]
   *
   * @param id         指令ID，可选参数，如果有指令ID，则在指令扩展中设置相应的bit位，并将指令ID作为数据部分的前缀发送
   * @param command    指令名称
   * @param parameters 指令参数
   */
  public commit(id: string | null, command: string, parameters: any[]): this {
    // 如果 WebSocket 连接未建立，则先建立连接
    if (!this.isConnected()) {
      this.open()
    }

    // 收集参数中所有附件（ArrayBuffer | Blob），并将其替换为占位符
    const attachments: Array<ArrayBuffer | Blob> = []
    const replacedParams = this.handleAttachmentsReplace(parameters, attachments)

    // 构造 METADATA：{"_cmd":"xxx"[,"_id":"xxx"][,"_num":n]}
    const metadata: Record<string, any> = { _cmd: command }
    if (id !== null) metadata._id = id
    if (attachments.length > 0) metadata._num = attachments.length

    // 构造数据体：METADATA + "#" + PARAMETER
    const dataStr = JSON.stringify(metadata) + PACKET_SEPARATOR + JSON.stringify(replacedParams)
    const dataBytes = new TextEncoder().encode(dataStr)

    // 构造主数据包：4字节头部 + 数据体
    const buffer = new ArrayBuffer(DATA_HEADER_SIZE + dataBytes.byteLength)
    const view = new DataView(buffer)
    view.setUint8(0, this.version)
    view.setUint8(1, SocketCommandType.COMMAND)
    view.setUint16(2, 0, false)
    new Uint8Array(buffer, DATA_HEADER_SIZE).set(dataBytes)
    this.socket?.send(buffer)

    // 逐个发送附件二进制包
    for (const attachment of attachments) {
      this.socket?.send(attachment)
    }

    return this
  }

  /**
   * 监听指令
   *
   * @param command 指令名称
   * @param fn      指令处理函数
   */
  public on(command: string | string, fn: (...args: any[]) => any): this {
    const listeners = this.listeners[command] || []
    listeners.push(fn)
    this.listeners[command] = listeners
    return this
  }

  /**
   * 取消监听指令
   *
   * @param command 指令名称
   * @param fn      指令处理函数，如果不传，则删除该指令的所有处理函数
   */
  public off(command: string | string, fn?: (...args: any[]) => any): this {
    if (!fn) {
      delete this.listeners[command]
      return this
    }
    const listeners = this.listeners[command] || []
    const index = listeners.indexOf(fn)
    if (index !== -1) {
      listeners.splice(index, 1)
    }
    // 如果指令处理函数列表为空，则删除该指令
    if (listeners.length === 0) {
      delete this.listeners[command]
    }
    return this
  }

  /**
   * 监听任意指令
   *
   * @param listener 指令处理函数
   */
  public onAny(listener: (...args: any[]) => void): this {
    this.anyListeners.push(listener)
    return this
  }

  /**
   * 取消监听任意指令
   *
   * @param listener 指令处理函数
   */
  public offAny(listener?: (...args: any[]) => void): this {
    if (listener) {
      const index = this.anyListeners.indexOf(listener)
      if (index !== -1) {
        this.anyListeners.splice(index, 1)
      }
    } else {
      this.anyListeners = []
    }
    return this
  }

  /**
   * 断开 WebSocket 连接
   */
  public disconnect() {
    this.disconnectable = true
    if (this.socket) {
      this.socket.close()
    }
  }

  /**
   * reject 所有待响应的 pending 请求，并清空队列
   * 在连接断开时调用，防止内存泄露
   */
  private handleAllPendingRequestsReject(reason: Error) {
    for (const [, pending] of this.pendingRequests) {
      if (pending.timeout) clearTimeout(pending.timeout)
      pending.reject(reason)
    }
    this.pendingRequests.clear()
  }

  /**
   * 处理收到的二进制数据包
   */
  private handleIncomingBuffer(buffer: ArrayBuffer) {
    // 如果当前有待接收附件的消息，则将此包作为附件处理
    if (this.pendingAttachmentQueue.length > 0) {
      const pending = this.pendingAttachmentQueue[0]
      pending.attachments.push(buffer)
      if (pending.attachments.length >= pending.total) {
        // 附件已全部收齐，替换占位符后分发
        this.pendingAttachmentQueue.shift()
        const resolvedParams = this.handlePlaceholdersResolve(pending.parameters, pending.attachments)
        this.handleCommandDispatch(pending.metadata, resolvedParams, pending.status)
      }
      return
    }

    // 解析主数据包头部
    if (buffer.byteLength < DATA_HEADER_SIZE) {
      console.warn('Packet too short, expected at least ' + DATA_HEADER_SIZE + ' bytes, but got ' + buffer.byteLength)
      return
    }
    const view = new DataView(buffer)
    const version = view.getUint8(0)
    const commandType = view.getUint8(1)
    const status = view.getUint16(2, false)
    if (version !== this.version) {
      console.error(`Version mismatch, expected ${this.version}, got ${version}`)
      return
    }
    // 解析数据包数据部分
    const dataBytes = new Uint8Array(buffer, DATA_HEADER_SIZE)
    const dataValue = new TextDecoder('utf-8').decode(dataBytes)
    if (commandType === SocketCommandType.HANDSHAKE) {
      // 发送数据前的握手
      const data = JSON.parse(dataValue)
      // 如果握手数据中包含重试策略，则进行重试策略配置
      if (data.policy) {
        this.reconnectOn = data.policy.reconnect_on
        this.reconnectAttempts = data.policy.max_reconnect_attempts || this.defaultReconnectAttempts
        this.reconnectInterval = data.policy.reconnect_interval || this.reconnectInterval
      }
      // 调用所有指令处理函数
      const listeners = this.listeners.handshake || []
      for (const listener of listeners) {
        listener(data)
      }
    } else if (commandType === SocketCommandType.COMMAND) {
      // 业务自定义指令，数据体格式：METADATA-PARAMETER
      const separatorIndex = dataValue.indexOf(PACKET_SEPARATOR)
      if (separatorIndex === -1) {
        console.warn('Invalid command data, missing "-" separator: ' + dataValue)
        return
      }
      const metadataStr = dataValue.slice(0, separatorIndex)
      const parameterStr = dataValue.slice(separatorIndex + 1)

      let metadata: Record<string, any>
      let parameters: any[]
      try {
        metadata = JSON.parse(metadataStr)
        parameters = JSON.parse(parameterStr)
      } catch (e) {
        console.warn('Invalid command data, failed to parse JSON: ' + dataValue)
        return
      }

      if (!Array.isArray(parameters)) {
        console.warn('Invalid command data, PARAMETER must be an array: ' + parameterStr)
        return
      }

      const attachmentNum: number = metadata._num || 0
      if (attachmentNum > 0) {
        // 有附件，先入队等待后续附件包
        this.pendingAttachmentQueue.push({
          metadata,
          parameters,
          status,
          attachments: [],
          total: attachmentNum
        })
        return
      }

      this.handleCommandDispatch(metadata, parameters, status)
    }
  }

  /**
   * 分发已就绪的指令（附件已全部替换完毕）
   */
  private handleCommandDispatch(metadata: Record<string, any>, parameters: any[], status: number) {
    // 如果携带 _id，则表示是 WS RPC 请求
    const commandId = metadata._id
    if (commandId && this.pendingRequests.has(commandId)) {
      const pending = this.pendingRequests.get(commandId)!
      this.pendingRequests.delete(commandId)
      if (pending.timeout) clearTimeout(pending.timeout)
      if (status !== 0) {
        pending.reject(new Error(`Request failed with status ${status}`))
      } else {
        pending.resolve(parameters)
      }
      return
    }

    // 调用所有指令处理函数
    const commandName = metadata._cmd
    const listeners = this.listeners[commandName] || []
    for (const listener of listeners) {
      listener(parameters)
    }
    // 调用所有任意指令处理函数
    for (const listener of this.anyListeners) {
      listener(commandName, parameters)
    }
  }

  /**
   * 递归遍历参数，将占位符对象替换为对应的 ArrayBuffer 附件
   */
  private handlePlaceholdersResolve(value: any, attachments: ArrayBuffer[]): any {
    if (value !== null && typeof value === 'object' && value._placeholder === true && typeof value._num === 'number') {
      return attachments[value._num] ?? null
    }
    if (Array.isArray(value)) {
      return value.map((item) => this.handlePlaceholdersResolve(item, attachments))
    }
    if (value !== null && typeof value === 'object') {
      const result: Record<string, any> = {}
      for (const key of Object.keys(value)) {
        result[key] = this.handlePlaceholdersResolve(value[key], attachments)
      }
      return result
    }
    return value
  }

  /**
   * 递归遍历参数，将 ArrayBuffer 或 Blob 替换为占位符对象，并收集到 attachments 数组中
   */
  private handleAttachmentsReplace(value: any, attachments: Array<ArrayBuffer | Blob>): any {
    if (value instanceof ArrayBuffer || value instanceof Blob) {
      const num = attachments.length
      attachments.push(value)
      return { _placeholder: true, _num: num }
    }
    if (Array.isArray(value)) {
      return value.map((item) => this.handleAttachmentsReplace(item, attachments))
    }
    if (value !== null && typeof value === 'object') {
      const result: Record<string, any> = {}
      for (const key of Object.keys(value)) {
        result[key] = this.handleAttachmentsReplace(value[key], attachments)
      }
      return result
    }
    return value
  }
}
