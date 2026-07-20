/**
 * 全局 SSE 流式状态管理
 * 
 * 维护每个会话的流式传输状态和消息缓冲区，
 * 确保切换会话后消息不会丢失，切回时能恢复 loading 动画。
 */

interface StreamingSession {
  // 是否正在流式传输
  streaming: boolean
  // 离开会话后缓冲的消息
  buffer: any[]
}

const sessions = new Map<string, StreamingSession>()

/**
 * 标记会话开始流式传输
 * 
 * @param sid 会话ID
 */
export function useStreamingStart(sid: string): void {
  const session = sessions.get(sid)
  if (session) {
    session.streaming = true
  } else {
    sessions.set(sid, { streaming: true, buffer: [] })
  }
}

/**
 * 标记会话结束流式传输
 * 
 * @param sid 会话ID
 */
export function useStreamingStop(sid: string): void {
  const session = sessions.get(sid)
  if (session) {
    session.streaming = false
  }
}

/**
 * 检查会话是否正在流式传输
 * 
 * @param   sid 会话ID
 * @returns 是否正在流式传输
 */
export function isSessionStreaming(sid: string): boolean {
  return sessions.get(sid)?.streaming ?? false
}

/**
 * 向会话缓冲区推送消息（用于非当前激活会话收到的 SSE 消息）
 * 
 * @param sid     会话ID
 * @param message 消息内容
 */
export function useBufferPush(sid: string, message: any): void {
  const session = sessions.get(sid)
  if (session) {
    session.buffer.push(message)
  } else {
    sessions.set(sid, { streaming: true, buffer: [message] })
  }
}

/**
 * 消费并清空指定会话的消息缓冲区
 * 
 * @param   sid 会话ID
 * @returns 消费的消息数组
 */
export function useBufferConsume(sid: string): any[] {
  const session = sessions.get(sid)
  if (!session || session.buffer.length === 0) return []
  const messages = [...session.buffer]
  session.buffer = []
  return messages
}

/**
 * 清空指定会话的消息缓冲区（不改变流式状态）
 *
 * 当后台会话的流式传输结束后调用：此时消息已全部落库，
 * 缓冲区里的增量副本已无价值，保留反而会在切回会话时与
 * 数据库返回的持久化消息重复。
 *
 * @param sid 会话ID
 */
export function useBufferRemove(sid: string): void {
  const session = sessions.get(sid)
  if (session) {
    session.buffer = []
  }
}

/**
 * 清除会话的流式状态（会话删除时调用）
 * 
 * @param sid 会话ID
 */
export function useSessionRemove(sid: string): void {
  sessions.delete(sid)
}

export function useStreamingState() {
  return {
    useStreamingStart,
    useStreamingStop,
    isSessionStreaming,
    useBufferPush,
    useBufferConsume,
    useBufferRemove,
    useSessionRemove
  }
}
