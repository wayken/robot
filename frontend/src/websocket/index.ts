import { Socket, SocketOptions } from './socket'

function lookup(url: string, opts?: SocketOptions) {
  opts = opts || {}
  const socket = new Socket(url, opts)
  if (opts.reconnectOn) {
    socket.open()
  }
  return socket
}

export {
  Socket,
  lookup as io,
  lookup as connect,
  lookup as default
}
