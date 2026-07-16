export const MessageRoleEnum = {
  User: 'user',
  System: 'system',
  Assistant: 'assistant'
} as const

export type ModelModuleType = 'text' | 'vision' | 'embedding' | 'reasoning'
export type MessageStatus = 'pending' | 'sending' | 'success' | 'paused' | 'failed'

export type Model = {
  name: string
  type: string
  provider: string
  module?: ModelModuleType[]
}

export type Message = {
  id: string,
  role: MessageRole
  model?: Model
  status?: MessageStatus
  content: string
  files?: FileType[]
  metrics?: Metrics
  reasoning?: string
  updated: number
}

export interface FileType {
  id: string
  name: string
  path: string
  size: number
  type: string
  modified: number
}

export type Metrics = {
  completion_tokens?: number
  resoning_millsec_time?: number
}

export type Session = {
  id: string
  messages: Message[]
}

export type Document = {
  id: string
  path: string
  option?: any
}

export type Knowledge = {
  id: string
  name: string
  model: Model
  files: FileType[]
  updated: number
  description?: string
}

export type Trash = {
  id: string
  source: string
  destination: string
  type: string
  updated: Date
}

export const ProviderTypeEnum = {
  Silicon: 'silicon',
  Deepseek: 'deepseek',
  Astraflow: 'astraflow',
  Ollama: 'ollama',
  Qwenlm: 'qwenlm',
  Openai: 'openai',
  Anthropic: 'anthropic',
  Hunyuan: 'hunyuan',
  Doubao: 'doubao',
  Gemini: 'gemini',
  Llama: 'llama',
  BaiduCloud: 'baiducloud',
  BGE: 'bge'
} as const

export interface AIOption {
  key: string
  api: string
  model: string
  topP: number
  temperature: number
}

export type Assistant = {
  id: string
  type: string
  name: string
  prompt: string
  model: Model
  updated: number
  description?: string
}

export type MessageRole = (typeof MessageRoleEnum)[keyof typeof MessageRoleEnum]
export type ProviderType = (typeof ProviderTypeEnum)[keyof typeof ProviderTypeEnum]

export type ProcessingStatus = 'pending' | 'processing' | 'completed' | 'failed'
export type KnowledgeItemType = 'file' | 'url' | 'note' | 'sitemap' | 'directory'
export type KnowledgeItem = {
  id: string
  baseId?: string
  uniqueId?: string
  uniqueIds?: string[]
  type: KnowledgeItemType
  content: string | FileType
  remark?: string
  created_at: number
  updated_at: number
  processingStatus?: ProcessingStatus
  processingProgress?: number
  processingError?: string
  retryCount?: number
}

export type KnowledgeBaseParams = {
  id: string
  model: string
  dimensions: number
  apiKey: string
  apiVersion?: string
  baseURL: string
  chunkSize?: number
  chunkOverlap?: number
}

export type onMessageUpdate = (result: any) => void
