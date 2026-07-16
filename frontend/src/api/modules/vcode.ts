import request from '@/api/request'

// 短信、邮箱验证码接口
export default {
  xhrCodeSend: (params: any) => {
    return request.post('/vcode/send', params)
  },
  xhrCodeValid: (params: any) => {
    return request.post('/vcode/valid', params)
  }
}
