import request from '@/api/request'

// 验证码接口
export default {
  loadCaptchaInfo: (params: any) => {
    return request.post('/captcha/info', params)
  },
  xhrCaptchaValidate: (params: any) => {
    return request.post('/captcha/validate', params)
  }
}
