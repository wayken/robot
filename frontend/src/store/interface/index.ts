// 应用程序状态
export interface AppState {
  theme: string         // 应用程序主题
  macId: string         // 网卡地址，主要用于服务端安全校验
  deviceId: string      // 设备ID，主要用于客户端设备识别
  llmModel: any         // AI模型
  isNarrow: boolean     // 是否窄屏
  isAIMaking: boolean   // 是否正在生成AI消息
  providerList: any[]   // 模型服务提供商列表
  assistantList: any[]  // 助手列表
  partnerList: any[]    // 自定义搭档列表
  settingData: any      // 应用程序配置
  // 是否正在生成AI消息
}

// 路由参数状态传递
export interface RouteState {
  parameters: any
}

// 账户状态
export interface AccountState {
  // 登录用户信息，数据结构如下：
  account: {
    name: string;   // 用户名
    acct: string;   // 用户账号
    avatar: string; // 用户头像
  };
  session: string;  // 登录后的会话信息
}

// 菜单状态
export interface MenuState {
  isMenuCollapsed: boolean
}

// 智能体状态
export interface WorkerState {
  infomation: any
}
