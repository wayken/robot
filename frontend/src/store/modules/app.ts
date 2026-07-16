import { defineStore } from 'pinia'
import { AppState } from '@/store/interface'

export const useAppStore = defineStore({
  id: 'app',
  state: (): AppState => {
    return {
      macId: '',
      deviceId: '',
      theme: 'blue',
      llmModel: {},
      isNarrow: false,
      isAIMaking: false,
      providerList: [],
      assistantList: [],
      partnerList: [],
      settingData: {}
    }
  },
  actions: {
    dispatchSetMacId(value: string) {
      this.macId = value
    },
    dispatchSetDeviceId(value: string) {
      this.deviceId = value
    },
    dispatchSetLLMModel(value: any) {
      this.llmModel = value
    },
    dispatchSetNarrow(value: boolean) {
      this.isNarrow = value
    },
    dispatchSetAIMaking(value: boolean) {
      this.isAIMaking = value
    },
    dispatchSetProviderList(value: any[]) {
      this.providerList = value
    },
    dispatchSetAssistantList(value: any[]) {
      this.assistantList = value
    },
    dispatchSetPartnerList(value: any[]) {
      this.partnerList = value
    },
    dispatchSetTheme(value: string) {
      this.theme = value
    },
    dispatchSetSettingData(value: any) {
      this.settingData = value
    }
  }
})
