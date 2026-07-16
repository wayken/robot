import focus from './focus'
import clickoutside from './clickoutside'

const directives: any = {
  focus,
  clickoutside
}

export default {
  install(app: any) {
    Object.keys(directives).forEach((key) => {
      app.directive(key, directives[key])
    })
  }
}
