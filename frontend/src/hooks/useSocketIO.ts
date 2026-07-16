import SocketIO from '@/api/socketio'

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
