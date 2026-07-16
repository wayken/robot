import Mock from 'mockjs'

const Random = Mock.Random
const picList = [
  'https://757722.s21i.faiusr.com/4/ABUIABAEGAAgrtvOgwYogLCD7wIw3gI4hAI.png',
  'https://757722.s21i.faiusr.com/2/ABUIABACGAAgp9vOgwYo-7bj8QMw6Ac49wI.jpg',
  'https://757722.s21i.faiusr.com/2/ABUIABACGAAg_5XfgAYo_8P--QYw9AM4zQI.jpg'
]
// 模型列表
const modelList = ['gpt-3.5-turbo', 'qwen2.5-coder:14b', 'qwen2.5-coder:7b', 'ollama-ollama68/deepseek-r1:14b']

const getAssistantInfomation = () => {
  const dataList = {
    id: Random.id(),
    name: '智能体' + Random.integer(0, 100),
    image: picList[Random.integer(1, 100) % picList.length],
    mode: Random.integer(0, 1),
    enabled: Random.boolean(),
    model: modelList[Random.integer(1, 100) % modelList.length],
    status: Random.integer(0, 1),
    node: '127.0.0.1:8162',
    workspace: 'C:/Users/Administrator/.worker/workspace',
    date: Random.date()
  }
  return Mock.mock({
    code: 0,
    result: dataList
  })
}

const getAssistantList = () => {
  const dataList = []
  const amount = Random.natural(1, 8)
  for (let i = 0; i < amount; i++) {
    dataList.push({
      id: '706823fb-4c1e-4eba-b329-1e517ad5ae63',
      name: '智能体' + i,
      image: picList[Random.integer(1, 100) % picList.length],
      mode: Random.integer(0, 1),
      enabled: Random.boolean(),
      model: modelList[Random.integer(1, 100) % modelList.length],
      status: Random.integer(0, 1),
      node: '127.0.0.1:8162',
      workspace: 'C:/Users/Administrator/.worker/workspace',
      date: Random.date()
    })
  }
  return Mock.mock({
    code: 0,
    result: dataList
  })
}

export default [
  {
    type: 'post',
    url: '/api/assistant/infomation',
    response: () => {
      return getAssistantInfomation()
    }
  },
  {
    type: 'post',
    url: '/api/assistant/list',
    response: () => {
      return getAssistantList()
    }
  }
]
