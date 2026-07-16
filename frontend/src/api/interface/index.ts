/**
 * 请求响应参数（不包含DATA）
 */
export interface Result {
  code: string
  timestamp: number
  message: string
}

/**
 * 请求响应参数（包含DATA）
 */
export interface ResultData<T = any> extends Result {
  result: T
}

/** 画布扩展选项 */
export interface SocketIOOption {
  reconnectOn?: boolean
  reconnectAttempts?: number
  remoteEndpoint?: Promise<string>
  handshakeData?: Promise<Record<string, string>>
}
