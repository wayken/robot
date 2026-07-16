import request from '@/api/request'

// 节点接口
export default {
  loadNodeInfomation: (params: any) => {
    return request.post('/api/node/infomation', params)
  },
  loadNodeList: (params: any) => {
    return request.post('/api/node/list', params)
  },
  xhrAddNode: (params: any) => {
    return request.post('/api/node/add', params)
  },
  xhrUpdateNode: (params: any) => {
    return request.post('/api/node/update', params)
  },
  xhrDeleteNode: (params: any) => {
    return request.post('/api/node/delete', params)
  }
}
