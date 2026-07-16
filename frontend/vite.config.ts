import {
  loadEnv,
  UserConfig,
  defineConfig
} from 'vite'
import {
  createHtmlPlugin
} from 'vite-plugin-html'
import {
  createSvgIconsPlugin
} from 'vite-plugin-svg-icons'
import { resolve } from 'path'
import vue from '@vitejs/plugin-vue'
import AutoImport from "unplugin-auto-import/vite"
import visualizer from 'rollup-plugin-visualizer'
import eslintPlugin from 'vite-plugin-eslint'

const loadConfig = ({ mode }: { mode: string }): UserConfig => {
  const {
    VITE_APP_TITLE,
    VITE_APP_PORT
  } = loadEnv(mode, process.cwd())
  return {
    base: './',
    plugins: [
      vue(),
      AutoImport ({
        imports: ['vue', 'pinia', 'vue-router', 'vue-i18n'],
        eslintrc: {
          enabled: true
        },
        dts: "src/auto-import.d.ts"
      }),
      createSvgIconsPlugin({
        iconDirs: [resolve(process.cwd(), 'src/assets/icons')],
        symbolId: 'icon-[dir]-[name]',
      }),
      eslintPlugin({
        include: [
          'src/**/*.js',
          'src/**/*.ts',
          'src/**/*.vue',
          'src/*.js',
          'src/*.vue'
        ],
        exclude: [
          '**/node_modules/**',
          '**/library/**'
        ]
      }),
      createHtmlPlugin({
        minify: true,
        inject: {
          data: {
            title: VITE_APP_TITLE
          }
        }
      }),
      // 只有在report命令下才配置打包分析插件
      mode === 'report' && visualizer({
        open: true,
        filename: './dist/report.html'
      })
    ],
    server: {
      host: false,
      port: Number(VITE_APP_PORT),
      open: false
    },
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    build: {
      target: 'es2015',
      outDir: resolve(__dirname, 'dist'),
      assetsDir: 'assets',
      assetsInlineLimit: 8192,
      emptyOutDir: true,
      chunkSizeWarningLimit: 1024,
      rollupOptions: {
        input: resolve(__dirname, 'index.html'),
        output: {
          chunkFileNames: 'js/[name].[hash].js',
          entryFileNames: 'js/[name].[hash].js',
          assetFileNames: '[ext]/[name].[hash].[ext]',
          manualChunks(id) {
            if (id.includes('node_modules')) {
              const modules = id.toString().split('node_modules/')[1].split('/')
              switch(modules[0]) {
                case '@vue':
                case 'vue-router':
                case 'vue-i18n':
                case 'axios':
                case 'mockjs':
                case 'element-plus':
                case 'dexie':
                case 'html2canvas':
                case 'jspdf':
                case 'katex':
                case 'canvg':
                case 'dagre-d3':
                case 'markdown-it':
                case 'markdown-it-katex':
                case 'prettier':
                case 'vuedraggable':
                case 'parse5':
                case 'lodash':
                case 'officeparser':
                case 'highlight.js':
                case 'emoji-mart-vue-fast':
                case 'monaco-editor':
                  return modules[0]
                case '@codemirror':
                case '@lezer':
                case 'codemirror':
                  return '@codemirror'
                default:
                  return 'vendor'
              }
            }
          }
        }
      }
    }
  }
}

// https://vitejs.dev/config/
export default defineConfig(loadConfig)
