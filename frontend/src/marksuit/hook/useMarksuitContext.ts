import type {
  MarksuitKind,
  MarksuitEditorOption
} from '../interface'
import {
  CompletionContext
} from '../interface'
import MarksuitRichEditor from '../core/index'
import MarksuitCodemirror from '../core/codemirror'

export interface MarksuitContext {
  useRichEditor: MarksuitRichEditor
  useCodemirror: MarksuitCodemirror
  useCurrentInstance: ReturnType<typeof getCurrentInstance>
  isFullscreen: Ref<boolean>
  isSourceMode: Ref<boolean>
  kind: MarksuitKind
  onFullscreenInvoke: () => void
  onSourceModeSwitch: () => void
  onImageUpload?: (file: File) => Promise<string>
}

const CONTEXT_KEY = 'useContext'

/**
 * 在父组件（index.vue）中调用：初始化编辑器并注入上下文
 */
export function provideMarksuitContext(
  options: MarksuitEditorOption,
  onUpdate: (content: string) => void,
  onImageUpload?: (file: File) => Promise<string>,
  onCompletion?: (context: CompletionContext) => void
): MarksuitContext {
  const useCodemirror = new MarksuitCodemirror()
  const useRichEditor = new MarksuitRichEditor({ ...options, onImageUpload })
  useRichEditor.on('update', onUpdate)
  if (onCompletion) {
    useRichEditor.on('completion', onCompletion)
  }
  const isFullscreen = ref(false)
  const isSourceMode = ref(false)
  const kind: MarksuitKind = options.kind ?? 'markdown'
  const onFullscreenInvoke = () => {
    isFullscreen.value = !isFullscreen.value
  }
  const onSourceModeSwitch = () => {
    isSourceMode.value = !isSourceMode.value
  }
  const context: MarksuitContext = {
    useRichEditor,
    useCodemirror,
    useCurrentInstance: getCurrentInstance(),
    isFullscreen,
    isSourceMode,
    kind,
    onFullscreenInvoke,
    onSourceModeSwitch,
    onImageUpload
  }
  provide(CONTEXT_KEY, context)
  return context
}

/**
 * 在子组件（menu.vue 等）中调用：获取父组件注入的上下文
 */
export function useMarksuitContext(): MarksuitContext {
  const context = inject<MarksuitContext>(CONTEXT_KEY)
  if (!context) {
    throw new Error('[Marksuit] useMarksuitContext must be used inside a Marksuit editor component')
  }
  return context
}

