import Mock from 'mockjs'

const Random = Mock.Random
const picList = [
  'https://757722.s21i.faiusr.com/4/ABUIABAEGAAgrtvOgwYogLCD7wIw3gI4hAI.png',
  'https://757722.s21i.faiusr.com/2/ABUIABACGAAgp9vOgwYo-7bj8QMw6Ac49wI.jpg',
  'https://757722.s21i.faiusr.com/2/ABUIABACGAAg_5XfgAYo_8P--QYw9AM4zQI.jpg'
]
const skillList = [
  '腾讯云智能体平台',
  '用于访问乐享知识库平台的专用 skill。当用户明确提到「乐享」「lexiang」「知识库」「知识」「文档」等关键词,或用户提供的链接 host 为 lexiangla.com,应优先调用本 skill。本 skill 支持:获取文档内容与元数据、搜索文档内容、查询知识库与目录结构、创建/编辑/移动文档、管理标签。该 Skill 来源于 SkillHub,暂不支持导出。如需在其他平台使用,请前往 SkillHub(https://skillhub.tencent.com/ )搜索「lexiang-mcp-skill」获取。',
  '基于 ADP 平台图片翻译 API 的多语言图片文字翻译工具。可自动识别图片中的文本内容并翻译成目标语言,支持中、英、日、韩、法、德、俄、西班牙、泰、越南等 13 种语言互译。支持本地图片文件和公网 URL 两种输入方式。适用于外文截图翻译、产品说明图翻译、旅行拍照翻译、海外资料快速阅读等场景。您可以点击右上角「导出」,将该 Skill 导出后在其他平台使用。',
  '基于混元大模型的文档翻译工具,通过 ADP 平台文档翻译 API 提供服务。支持 PDF 和 Word 文档的多语言翻译,并尽可能保留原文件格式样式。支持简体中文、繁体中文、粤语、英语、法语、葡萄牙语、西班牙语、日语、土耳其语共 9 种语言互译。适用于学术论文翻译、技术文档本地化、商务文档多语言版本生成等场景。您可以点击右上角「导出」,将该 Skill 导出后在其他平台使用。',
  '基于腾讯混元多模态大模型(hunyuan-turbos-vision)的图片理解工具,通过 ADP 平台 API 提供服务。提供两项核心能力:图片理解(自动分析图片内容并输出详细描述)和图文问答(基于图片回答用户问题)。当其他专业 Skill(如 OCR、翻译)能提供更好结果时,会主动建议切换。适用于图片内容分析、截图解读、图文互动问答等场景。您可以点击右上角「导出」,将该 Skill 导出后在其他平台使用。'
]

const getSkillList = () => {
  const dataList = []
  const amount = Random.natural(8, 24)
  for (let i = 0; i < amount; i++) {
    dataList.push({
      id: Random.id(),
      name: '技能' + i,
      enabled: Random.boolean(),
      image: picList[Random.integer(1, 100) % picList.length],
      description: skillList[Random.integer(1, 100) % skillList.length],
      version: '1.0.0'
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
    url: '/skill/install/list',
    response: () => {
      return getSkillList()
    }
  },
  {
    type: 'post',
    url: '/skill/market/list',
    response: () => {
      return getSkillList()
    }
  }
]
