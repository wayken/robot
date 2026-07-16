import request from '@/api/request'

export default {
  loadProviderList: (params: any) => {
    return request.post('/api/provider/list', params)
  },
  xhrAddProvider: (params: any) => {
    return request.post('/api/provider/add', params)
  },
  xhrUpdateProvider: (params: any) => {
    return request.post('/api/provider/update', params)
  },
  xhrDeleteProvider: (params: any) => {
    return request.post('/api/provider/delete', params)
  }
}
