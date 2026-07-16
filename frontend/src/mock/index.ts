import Mock from 'mockjs'
import assistant from './service/assistant'
import node from './service/node'
import provider from './service/provider'
import skill from './service/skill'
import { MockParams } from './typing'

const MOCK_TIMEOUT = import.meta.env.VITE_MOCK_TIMEOUT

if (import.meta.env.MODE == 'development') {
  let i: MockParams
  const mockList = [
    ...assistant,
    ...node,
    ...provider,
    ...skill
  ]
  for (i of mockList) {
    Mock.mock(new RegExp(i.url), i.type || 'post', i.response)
  }
  Mock.setup({
    timeout: MOCK_TIMEOUT
  })
}
