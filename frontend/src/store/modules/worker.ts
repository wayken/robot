import { defineStore } from 'pinia'
import { WorkerState } from '@/store/interface'

export const useWorkerStore = defineStore({
  id: 'worker',
  state: (): WorkerState => {
    return {
      infomation: {
      }
    }
  },
  actions: {
    dispatchSetInfomation(value: any) {
      this.infomation = value
    }
  }
})
