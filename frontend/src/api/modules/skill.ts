import request from '@/api/request'

// 技能接口
export default {
  loadInstallSkillList: (params: any) => {
    return request.post('/skill/install/list', params)
  },
  loadInstallMarketList: (params: any) => {
    return request.post('/skill/market/list', params)
  },
  switchSkill: (params: any) => {
    return request.post('/skill/switch', params)
  }
}
