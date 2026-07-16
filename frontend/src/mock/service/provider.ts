import Mock from 'mockjs'

const Random = Mock.Random
const providerList = [
  'silicon',
  'deepseek',
  'astraflow',
  'ollama',
  'qwenlm',
  'openai',
  'hunyuan',
  'doubao',
  'gemini'
]
const modelList = [
  'gpt-3.5-turbo',
  'gpt-4',
  'gpt-4-32k',
  'gpt-3.5-turbo-16k',
  'gpt-3.5-turbo-0613',
  'gpt-4-0613',
  'gpt-4-32k-0613',
  'gpt-3.5-turbo-16k-0613'
]

const getProviderList = () => {
  const dataList: any[] = []
  const length = providerList.length
  for (let i = 0; i < length; i++) {
    const provider = providerList[i]
    dataList.push({
      id: Random.string('number', 10),
      type: 'system',
      name: provider,
      url: 'https://' + provider + '.com',
      provider: provider,
      models: []
    })
    const moduleLength = Random.integer(1, 5)
    for (let m = 0; m < moduleLength; m++) {
      const modles: any = []
      dataList[i].models.push({
        id: Random.string('number', 10),
        type: providerList[Random.integer(1, 100) % providerList.length],
        name: '模型分组' + m,
        model: modles
      })
      const modelLength = Random.integer(1, 5)
      for (let n = 0; n < modelLength; n++) {
        modles.push({
          type: providerList[Random.integer(1, 100) % providerList.length],
          name: modelList[Random.integer(1, 100) % modelList.length]
        })
      }
    }
  }
  return Mock.mock({
    code: 0,
    result: dataList
  })
}

export default [
  {
    type: 'post',
    url: '/api/provider/list',
    response: () => {
      return getProviderList()
    }
  }
]
