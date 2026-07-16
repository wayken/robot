// 定义vue-i18n的扩展接口
declare module 'vue/types/vue' {
  interface Vue {
    $t: (key: string) => string;
  }
}
declare module 'markdown-it-katex'
declare module 'markdown-it-table-of-contents'
declare module 'markdown-it-task-lists'
declare module '@liradb2000/markdown-it-mermaid'
declare module 'emoji-mart-vue-fast/src'
declare module 'mockjs'
declare module 'virtual:svg-icons-register'
