import conversation from './conversation'
import node from './node'
import worker from './worker'
import toolkit from './toolkit'

const service: {
  [key: string]: any
} = {
  conversation,
  node,
  worker,
  toolkit
}
export default service
