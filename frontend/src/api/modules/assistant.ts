import request from '@/api/request'

// 智能体接口
export default {
  loadAssistantInfomation: (params: any) => {
    return request.post('/api/assistant/infomation', params)
  },
  loadAssistantList: (params: any) => {
    return request.post('/api/assistant/list', params)
  },
  xhrAddAssistant: (params: any) => {
    return request.post('/api/assistant/add', params)
  },
  xhrUpdateAssistant: (params: any) => {
    return request.post('/api/assistant/update', params)
  },
  xhrDeleteAssistant: (params: any) => {
    return request.post('/api/assistant/delete', params)
  },
  xhrUpdateAssistantStatus: (params: any) => {
    return request.post('/api/assistant/status', params)
  }
}
