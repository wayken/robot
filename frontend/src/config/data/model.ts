import {
  ProviderTypeEnum
} from '@/types/index'
import { View, MagicStick, SetUp, Platform } from '@element-plus/icons-vue'
import IconSilicon from '@/assets/images/providers/silicon.png'
import IconDeepseek from '@/assets/images/providers/deepseek.png'
import IconAstraflow from '@/assets/images/providers/astraflow.png'
import IconOllama from '@/assets/images/providers/ollama.png'
import IconQwenlm from '@/assets/images/providers/qwenlm.png'
import IconOpenai from '@/assets/images/providers/openai.jpeg'
import IconAnthropic from '@/assets/images/providers/anthropic.png'
import IconHunyuan from '@/assets/images/providers/hunyuan.png'
import IconDoubao from '@/assets/images/providers/doubao.png'
import IconGemini from '@/assets/images/providers/gemini.png'
import IconLlama from '@/assets/images/providers/llama.png'
import IconBaiduCloud from '@/assets/images/providers/baidu-cloud.svg'
import IconUnknow from '@/assets/images/providers/unknow.png'

export const loadProviderIconMapping: any = {
  [ProviderTypeEnum.Silicon]: IconSilicon,
  [ProviderTypeEnum.Deepseek]: IconDeepseek,
  [ProviderTypeEnum.Astraflow]: IconAstraflow,
  [ProviderTypeEnum.Ollama]: IconOllama,
  [ProviderTypeEnum.Qwenlm]: IconQwenlm,
  [ProviderTypeEnum.Openai]: IconOpenai,
  [ProviderTypeEnum.Anthropic]: IconAnthropic,
  [ProviderTypeEnum.Hunyuan]: IconHunyuan,
  [ProviderTypeEnum.Doubao]: IconDoubao,
  [ProviderTypeEnum.Gemini]: IconGemini,
  [ProviderTypeEnum.Llama]: IconLlama,
  [ProviderTypeEnum.BaiduCloud]: IconBaiduCloud
} as const

export const loadProviderList = [
  {
    type: ProviderTypeEnum.Silicon,
    icon: IconSilicon
  },
  {
    type: ProviderTypeEnum.Deepseek,
    icon: IconDeepseek
  },
  {
    type: ProviderTypeEnum.Anthropic,
    icon: IconAnthropic
  },
  {
    type: ProviderTypeEnum.Ollama,
    icon: IconOllama
  },
  {
    type: ProviderTypeEnum.Qwenlm,
    icon: IconQwenlm
  },
  {
    type: ProviderTypeEnum.Openai,
    icon: IconOpenai
  },
  {
    type: ProviderTypeEnum.Hunyuan,
    icon: IconHunyuan
  },
  {
    type: ProviderTypeEnum.Doubao,
    icon: IconDoubao
  },
  {
    type: ProviderTypeEnum.Gemini,
    icon: IconGemini
  }
]

// 加载模型对应的图标
export function loadProviderIcon(type: string) {
  return loadProviderIconMapping[type] || IconUnknow
}

// 模型属性常量：1=视觉, 2=推理, 3=工具, 4=基岩

export const MODEL_PROPERTY_MAP: Record<number, { label: string; className: string; icon: any }> = {
  1: { label: 'vision', className: 'is-vision', icon: View },
  2: { label: 'inference', className: 'is-inference', icon: MagicStick },
  3: { label: 'tool', className: 'is-tool', icon: SetUp },
  4: { label: 'bedrock', className: 'is-bedrock', icon: Platform }
}

export function loadModelPropertyLabel(prop: number): string {
  return MODEL_PROPERTY_MAP[prop]?.label || 'unknow'
}

export function loadModelPropertyClass(prop: number): string {
  return MODEL_PROPERTY_MAP[prop]?.className || ''
}

export function loadModelPropertyIcon(prop: number): any {
  return MODEL_PROPERTY_MAP[prop]?.icon || View
}
