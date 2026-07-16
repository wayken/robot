import session from './modules/session'
import captcha from './modules/captcha'
import user from './modules/user'
import vcode from './modules/vcode'
import assistant from './modules/assistant'
import node from './modules/node'
import provider from './modules/provider'
import skill from './modules/skill'

const service: {
  [key: string]: any
} = {
  session,
  captcha,
  user,
  vcode,
  assistant,
  node,
  provider,
  skill
}
export default service
