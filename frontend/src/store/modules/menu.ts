import { defineStore } from 'pinia'
import Constant from '@/config/constant'
import { MenuState } from '@/store/interface'

export const useMenuStore = defineStore({
  id: 'menu',
  state: (): MenuState => {
    return {
      isMenuCollapsed: JSON.parse(localStorage.getItem(Constant.LOCAL_MENU_COLLAPSED) || 'false')
    }
  },
  actions: {
    dispatchSetMenuCollapsed(value: boolean) {
      this.isMenuCollapsed = value
      localStorage.setItem(Constant.LOCAL_MENU_COLLAPSED, JSON.stringify(value))
    }
  }
})
