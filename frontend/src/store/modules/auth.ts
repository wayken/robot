import { defineStore } from 'pinia'
import { AccountState } from '@/store/interface'

export const useAuthStore = defineStore({
  id: 'auth',
  state: (): AccountState => {
    return {
      account: {
        name: '',
        acct: '',
        avatar: ''
      },
      session: ''
    }
  },
  actions: {
    dispatchSetAccount(value: any) {
      this.account = value
    },
    dispatchSetSession(value: string) {
      this.session = value
    }
  }
})
