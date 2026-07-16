import request from '@/api/request'

// 用户登录注册接口
export default {
  loadUserLoginCorpList: (params: any) => {
    return request.post('/security/login/corp/list', params)
  },
  loadUserSigninCorpList: (params: any) => {
    return request.post('/security/signin/corp/list', params)
  },
  xhrUserSignup: (params: any) => {
    return request.post('/security/signup', params)
  }
}
