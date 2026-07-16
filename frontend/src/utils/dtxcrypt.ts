import CryptoJS from 'crypto-js'

/**
 * AES加密
 *
 * @param {String} data 原始数据
 * @param {String} key 密钥
 * @param {String} iv  加密向量
 */
function encrypt(data: string, key: string, iv: string) {
  return CryptoJS.AES.encrypt(data, CryptoJS.enc.Utf8.parse(key), {
    iv: CryptoJS.enc.Utf8.parse(iv),
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  }).toString()
}

/**
 * AES解密
 *
 * @param {String} data 原始数据
 * @param {String} key 密钥
 * @param {String} iv  加密向量
 */
function decrypt(data: string, key: string, iv: string) {
  const decrypted = CryptoJS.AES.decrypt(data, CryptoJS.enc.Utf8.parse(key), {
    iv: CryptoJS.enc.Utf8.parse(iv),
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypted.toString(CryptoJS.enc.Utf8)
}

/**
 * 生成随机字符串
 *
 * @param  length 随机字符串的长度
 * @return 随机字符串
 */
function doGetRandomChars(length: number) {
  length = length || 16
  let randomChars = ''
  const chars = [
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'm', 'o', 'p', 'q', 'r', 's', 't', 'i', 'v', 'w', 'x', 'y', 'z'
  ]
  for (let i = 0; i < length; i++) {
    const id = Math.ceil(Math.random() * (chars.length - 1))
    randomChars += chars[id]
  }
  return randomChars
}

/**
 * 大头虾算法加密，注意要对该源文件进行混淆再引入
 *
 * @param {String} data 原始数据
 */
export function dtxEncrypt(data: string) {
  const values = []
  const iv = doGetRandomChars(16)
  const key = doGetRandomChars(16)
  values.push(iv.split('').reverse().join(''))
  values.push(encrypt(data, key, iv))
  values.push(key)
  return values.join('')
}

/**
 * 大头虾算法解密，注意要对该源文件进行混淆再引入
 *
 * @param {String} data 原始数据
 */
export function dtxDecrypt(data: string) {
  if (!data || data.length < 36) {
    return null
  }
  const iv = data.substring(0, 16).split('').reverse().join('')
  const key = data.substring(data.length - 16, data.length)
  const sign = data.substring(16, data.length - 16)
  return decrypt(sign, key, iv)
}
