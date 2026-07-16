import { io } from '@/websocket/index'

export default function useSocketIO(socketUrl: string): Promise<ReturnType<typeof io>> {
  return new Promise((resolve, reject) => {
    const socket = io(socketUrl, {
      reconnectOn: false,
      reconnectAttempts: 0
    })
    const onConnect = () => {
      onCleanup()
      resolve(socket)
    }
    const onError = () => {
      onCleanup()
      socket.disconnect()
      reject(new Error(`Socket ${socketUrl} connection failed after all reconnect attempts`))
    }
    const timer = window.setTimeout(() => {
      onCleanup()
      socket.disconnect()
      reject(new Error(`Socket ${socketUrl} connection timeout`))
    }, 10000)
    const onCleanup = () => {
      window.clearTimeout(timer)
      socket.off('error', onError)
      socket.off('connect', onConnect)
    }
    socket.on('error', onError)
    socket.on('connect', onConnect)
    socket.open()
  })
}
