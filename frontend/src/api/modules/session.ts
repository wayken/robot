import request from '@/api/request'

// 会话管理模块
export default {
  // 获取文档信息
  loadSessionInfo: (token: string) => {
    return request.post('/session/info', token)
  },
  xhrSessionLogout: (params: any) => {
    return request.post('/session/logout', params)
  }
}
