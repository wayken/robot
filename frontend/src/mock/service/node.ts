import Mock from 'mockjs'

const Random = Mock.Random
// 模型列表
const oslList = ['Ubuntu 22.04', 'CentOS 7.9', 'Window 10']

const getNodeInfo = () => {
  const dataList = {
    address: '127.0.0.1',
    port: 8162
  }
  return Mock.mock({
    code: 0,
    result: dataList
  })
}

const getNodeList = () => {
  const dataList = []
  const amount = Random.natural(2, 8)
  for (let i = 0; i < amount; i++) {
    dataList.push({
      id: Random.id(),
      name: '节点' + i,
      hostname: 'WINDOW HOST' + i,
      address: '172.16.0.' + i,
      version: '1.0.0',
      port: Random.integer(1000, 2048),
      cpu: Random.integer(50, 99),
      os: oslList[Random.integer(1, 100) % oslList.length],
      memory: Random.integer(50, 99),
      enabled: Random.boolean(),
      assistants: Random.integer(0, 2),
      status: Random.integer(0, 2),
      region: 'us-west-1',
      uptime: Random.date()
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
    url: '/api/node/infomation',
    response: () => {
      return getNodeInfo()
    }
  },
  {
    type: 'post',
    url: '/api/node/list',
    response: () => {
      return getNodeList()
    }
  }
]
