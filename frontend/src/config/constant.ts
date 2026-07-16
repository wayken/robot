/**
 * 全局存储变量名称
 */
const LOCAL_LANG = 'Local-Lang'
const LOCAL_APPERANCE = 'Local-Appearance'
const LOCAL_THEME = 'Local-Theme'
const LOCAL_DEVICE_ID = 'Local-DeviceId'
const LOCAL_MENU_COLLAPSED = 'Local-Menu-Collapsed'
const LOCAL_PROVIDER_LIST = 'Local-ProviderList'
const LOCAL_ASSISTANT_LIST = 'Local-AssistantList'
const LOCAL_LLM = 'Local-LLM'
const LOCAL_NARROW = 'Local-Narrow'
const LOCAL_PARTNER_LIST = 'Local-PartnerList'
const LOCAL_LICENSE_INIT = 'Local-License-Init'
const LOCAL_LICENSE_TIMESTAMP = 'Local-Timestamp'
const LOCAL_LICENSE_LIMIT = 30
const LOCAL_ACTIVATION_CODE = 'Local-Activation-Code'
const LOCAL_LINK_LIST = 'Local-LinkList'
const X_Auth_Token = 'X-Auth-Token'

// 验证码业务ID
const VCODE_BIZ_LOGIN = 1
const VCODE_BIZ_SIGNUP = 2

export default {
  LOCAL_LANG,
  LOCAL_APPERANCE,
  LOCAL_THEME,
  LOCAL_DEVICE_ID,
  LOCAL_MENU_COLLAPSED,
  LOCAL_PROVIDER_LIST,
  LOCAL_ASSISTANT_LIST,
  LOCAL_LLM,
  LOCAL_NARROW,
  LOCAL_PARTNER_LIST,
  LOCAL_LICENSE_INIT,
  LOCAL_LICENSE_TIMESTAMP,
  LOCAL_LICENSE_LIMIT,
  LOCAL_ACTIVATION_CODE,
  LOCAL_LINK_LIST,
  X_Auth_Token,
  RESP_CODE: {
    NEED_LOGIN: 1004001
  },
  WEBSOCKET_STATUS: {
    NOT_CONNECT: 0,
    CONNECTING: 1,
    CONNECTED: 2,
    CONNECT_FAIL: 3
  },
  VCODE_BIZ_LOGIN,
  VCODE_BIZ_SIGNUP
}
