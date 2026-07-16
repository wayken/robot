export type MarksuitKind = 'markdown' | 'markup'

export interface MarksuitEditorOption {
  // 编辑器内容
  content?: string
  // 占位符
  placeholder?: string
  // 图片上传回调
  onImageUpload?: (file: File) => Promise<string>
  // 内容类型：markdown（默认）或 markup（富文本 HTML）
  kind?: MarksuitKind
  // 是否开启AI文本自动补全
  isAutoCompletion?: boolean
}

export interface MarksuitCodemirrorOption {
  // 编辑器内容
  content?: string
  workspace: HTMLElement
}

export interface MenuOption {
	name: string
  icon: string
  buildin?: boolean
  isActive?: () => boolean
  isDisabled?: () => boolean
	onClick?: () => void
	onDblclick?: () => void
}

export interface ToolTemplateProps {
  option: MenuOption
}

export interface CompletionContext {
  // 当前块从起始到光标的纯文本（提示词）
  prompt: string
  // 整个文档光标之前的纯文本
  before: string
  // 整个文档光标之后的纯文本
  after: string
  // 流式追加补全文本
  append: (chunk: string) => void
  // 整体替换补全文本
  replace: (text: string) => void
  // 标记流式输出结束（不会清空内容，等待用户 Tab 确认）
  finish: () => void
  // 取消本次补全（清空 ghost 文本并触发 onCancel 回调）
  cancel: () => void
  // 注册一个回调，当用户主动取消（按键、移动光标、修改内容等）时被触发
  onCancel: (callback: () => void) => void
}
