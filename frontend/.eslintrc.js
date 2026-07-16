module.exports = {
  env: {
    browser: true,
    es2021: true,
    node: true
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-essential',
    'plugin:@typescript-eslint/recommended',
    './.eslintrc-auto-import.json',
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    ecmaVersion: 'latest',
    parser: '@typescript-eslint/parser',
    sourceType: 'module'
  },
  plugins: ['vue', '@typescript-eslint'],
  rules: {
    'indent': ['error', 2, { 'SwitchCase': 1 }],        // 每行缩进2个空格
    'semi': ['error', 'never'],                         // 禁止语句后加分号
    'quotes': ['error', 'single'],                      // 字符串采用单引号
    'comma-dangle': ['error', 'never'],                 // 禁止逗号结尾
    'eol-last': ['error', 'always'],                    // 文件的末尾至少执行一个换行符
    'no-empty': ['off'],                                // 允许空语句块
    'no-cond-assign': ['off'],                          // 允许条件语句中出现赋值操作符
    'no-useless-escape': ['off'],                       // 禁止不必要的转义字符
    'vue/multi-word-component-names': ['off'],          // 组件命名小写
    'vue/no-mutating-props': ['off'],                   // 允许动态修改Props属性
    '@typescript-eslint/ban-ts-comment': ['off'],       // 允许ts注释
    '@typescript-eslint/no-var-requires': ['off'],      // 允许声明require变量
    '@typescript-eslint/no-this-alias': ['off'],        // 允许声明this变量
    "@typescript-eslint/no-explicit-any": ['off'],      // 允许采用any类型
    '@typescript-eslint/no-empty-function': ['off'],    // 允许空函数
    '@typescript-eslint/no-inferrable-types': ['off'],  // 允许类型推断
    '@typescript-eslint/no-non-null-assertion': ['off'] // 允许非空断言
  }
}
