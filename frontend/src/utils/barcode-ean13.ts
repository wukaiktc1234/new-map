/**
 * EAN-13 条码解码器（手写实现）
 *
 * 背景：zxing-js 对 1D 条码识别弱（竖拍/条码占比小易 NotFound），
 * 且部分浏览器无 BarcodeDetector。EAN-13 结构固定（95 模块），可精确解码。
 *
 * 解码流程：灰度 → 行二值化（分段自适应阈值）→ 黑白宽度序列 → 定位起始模式(101)
 * → 用「起始→结束」整体宽度重估单元（消除累积误差）→ 按模块中点采样读取 95 位
 * → 左右数字表匹配 + 首位奇偶表 + 校验和验证。
 * 多角度（0°±2°±4°±6°）+ 多行扫描，任一命中即返回。
 */

/** EAN-13 左侧奇数位编码 */
const LEFT_ODD = [
  '0001101', '0011001', '0010011', '0111101', '0100011',
  '0110001', '0101111', '0111011', '0110111', '0001011',
]

/** EAN-13 左侧偶数位编码（与右侧编码相同） */
const LEFT_EVEN = [
  '0100111', '0110011', '0011011', '0100001', '0011101',
  '0111001', '0000101', '0010001', '0001001', '0010111',
]

/** 首位数字 → 左 6 位奇偶模式（0=奇 1=偶） */
const PARITY_TABLE = [
  '000000', '001011', '001101', '001110', '010011',
  '011001', '010110', '011010', '011100', '010101',
]

/** 起始/结束模式与中间分隔符 */
const START_END = '101'
const MIDDLE = '01010'

/** 校验和（前 12 位，奇数位权重 1、偶数位权重 3） */
function checkSum(digits: number[]): boolean {
  if (digits.length !== 13) return false
  let sum = 0
  for (let i = 0; i < 12; i++) {
    sum += digits[i] * (i % 2 === 0 ? 1 : 3)
  }
  const check = (10 - (sum % 10)) % 10
  return check === digits[12]
}

/** 95 位模块序列 → 13 位数字（失败返回 null） */
export function decodeBits(bits: string): number[] | null {
  if (bits.length !== 95) return null
  if (bits.slice(0, 3) !== START_END) return null
  if (bits.slice(45, 50) !== MIDDLE) return null
  if (bits.slice(92, 95) !== START_END) return null

  const left = bits.slice(3, 45)
  const right = bits.slice(50, 92)

  const leftDigits: number[] = []
  const parity: number[] = []
  for (let i = 0; i < 6; i++) {
    const seg = left.slice(i * 7, i * 7 + 7)
    const oddIdx = LEFT_ODD.indexOf(seg)
    if (oddIdx >= 0) {
      leftDigits.push(oddIdx)
      parity.push(0)
      continue
    }
    const evenIdx = LEFT_EVEN.indexOf(seg)
    if (evenIdx >= 0) {
      leftDigits.push(evenIdx)
      parity.push(1)
      continue
    }
    return null
  }

  const firstDigit = PARITY_TABLE.indexOf(parity.join(''))
  if (firstDigit < 0) return null

  const rightDigits: number[] = []
  for (let i = 0; i < 6; i++) {
    const seg = right.slice(i * 7, i * 7 + 7)
    const idx = LEFT_EVEN.indexOf(seg)
    if (idx < 0) return null
    rightDigits.push(idx)
  }

  const digits = [firstDigit, ...leftDigits, ...rightDigits]
  return checkSum(digits) ? digits : null
}

/** 从黑白宽度序列中定位起始模式并按模块宽度读取 95 位（模块中点采样，避免边界抖动） */
export function readModules(runs: { black: boolean; width: number }[], startIdx: number, unitW: number): string {
  let bits = ''
  let pos = 0
  let runIdx = startIdx
  let consumedInRun = 0
  for (let m = 0; m < 95; m++) {
    const sample = (m + 0.5) * unitW // 模块中点
    while (pos < sample && runIdx < runs.length) {
      const remain = runs[runIdx].width - consumedInRun
      if (pos + remain >= sample) {
        consumedInRun += sample - pos
        pos = sample
        break
      }
      pos += remain
      runIdx++
      consumedInRun = 0
    }
    // run 序列耗尽（条码不完整）：返回空串，由调用方判失败
    if (runIdx >= runs.length) return ''
    bits += runs[runIdx].black ? '1' : '0'
  }
  return bits
}

/** 供测试/调试使用的行解码入口：亮度行数组 → { code: 13 位数字或 null; startHit: 起始模式命中次数 } */
export function decodeLine(line: number[]): { code: string | null; startHit: number } {
  const min = Math.min(...line)
  const max = Math.max(...line)
  if (max - min < 24) return { code: null, startHit: 0 } // 对比度过低，无条码

  // 分段自适应阈值（抗光照渐变）：每段独立阈值，段内对比度低时回退全局阈值
  const SEGMENTS = 16
  const segLen = Math.max(1, Math.ceil(line.length / SEGMENTS))
  const thresholds: number[] = []
  for (let s = 0; s < SEGMENTS; s++) {
    const start = s * segLen
    const end = Math.min(line.length, start + segLen)
    let sMin = 255
    let sMax = 0
    for (let i = start; i < end; i++) {
      if (line[i] < sMin) sMin = line[i]
      if (line[i] > sMax) sMax = line[i]
    }
    thresholds.push(sMax - sMin >= 24 ? (sMin + sMax) / 2 : (min + max) / 2)
  }

  const runs: { black: boolean; width: number }[] = []
  for (let x = 0; x < line.length; x++) {
    const black = line[x] < thresholds[Math.min(SEGMENTS - 1, Math.floor(x / segLen))]
    const last = runs[runs.length - 1]
    if (last && last.black === black) {
      last.width++
    } else {
      runs.push({ black, width: 1 })
    }
  }

  // run 起点累计宽度（用于精确重估单元宽度）
  const runStart: number[] = new Array(runs.length + 1)
  runStart[0] = 0
  for (let i = 0; i < runs.length; i++) {
    runStart[i + 1] = runStart[i] + runs[i].width
  }

  const isStartPattern = (idx: number): { unit: number } | null => {
    if (idx < 0 || idx + 2 >= runs.length) return null
    if (!runs[idx].black || runs[idx + 1].black || !runs[idx + 2].black) return null
    const w0 = runs[idx].width
    const w1 = runs[idx + 1].width
    const w2 = runs[idx + 2].width
    const unit = (w0 + w1 + w2) / 3
    const tolerance = unit * 0.38
    if (Math.abs(w0 - unit) > tolerance || Math.abs(w1 - unit) > tolerance || Math.abs(w2 - unit) > tolerance) {
      return null
    }
    return { unit }
  }

  // 定位起始模式（黑-白-黑 1:1:1），并用「起始→结束」整体宽度重估单元（消除累积误差）
  let startHit = 0
  for (let i = 0; i < runs.length - 3; i++) {
    const start = isStartPattern(i)
    if (!start) continue
    startHit++

    // 单元重估：EAN-13 结束 guard(101) 首条与第 13 位数字末尾合并（2:1:1 非 1:1:1），
    // 无法用 1:1:1 定位条码末尾；直接取「起始到最后一个黑条终点」作为条码总宽（95 模块）重估单元
    let unitCalc = 0
    let lastBlackIdx = -1
    for (let j = i; j < runs.length; j++) {
      if (runs[j].black) lastBlackIdx = j
    }
    if (lastBlackIdx >= i) {
      const totalWidth = runStart[lastBlackIdx + 1] - runStart[i]
      const unitTest = totalWidth / 95
      // 与起始估计相差 30% 以内才视为有效（防背景假候选）
      if (Math.abs(unitTest - start.unit) < start.unit * 0.3) {
        unitCalc = unitTest
      }
    }

    // 双候选单元依次尝试：整体重估（抗累积误差）优先，起始估计兜底（抗重估误判）
    const unitCandidates = unitCalc > 0 ? [unitCalc, start.unit] : [start.unit]
    for (const candidate of unitCandidates) {
      const bits = readModules(runs, i, candidate)
      const digits = decodeBits(bits)
      if (digits) return { code: digits.join(''), startHit }
    }
  }
  return { code: null, startHit }
}

/** 旋转画布（白色填充背景），用于抗轻微倾斜扫描 */
function rotateCanvas(src: HTMLCanvasElement, angleDeg: number): HTMLCanvasElement {
  const rad = (angleDeg * Math.PI) / 180
  const w = src.width
  const h = src.height
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  if (!ctx) return src
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, w, h)
  ctx.translate(w / 2, h / 2)
  ctx.rotate(rad)
  ctx.drawImage(src, -w / 2, -h / 2)
  return canvas
}

/** 单角度扫描 */
function scanCanvasOnce(canvas: HTMLCanvasElement): { code: string | null; scans: number; starts: number } {
  const ctx = canvas.getContext('2d')
  if (!ctx) return { code: null, scans: 0, starts: 0 }
  const { width, height } = canvas
  const imageData = ctx.getImageData(0, 0, width, height)
  const data = imageData.data

  const luminanceAt = (x: number, y: number): number => {
    const i = (y * width + x) * 4
    return (data[i] * 299 + data[i + 1] * 587 + data[i + 2] * 114) / 1000
  }

  let scans = 0
  let startHit = 0
  const startY = Math.round(height * 0.25)
  const endY = Math.round(height * 0.95)
  for (let y = startY; y < endY; y += 2) {
    scans++
    const line: number[] = new Array(width)
    for (let x = 0; x < width; x++) {
      const y2 = Math.min(y + 1, height - 1)
      line[x] = (luminanceAt(x, y) + luminanceAt(x, y2)) / 2
    }
    const result = decodeLine(line)
    startHit += result.startHit
    if (result.code) {
      return { code: result.code, scans, starts: startHit }
    }
  }
  return { code: null, scans, starts: startHit }
}

/**
 * 从 canvas 解码 EAN-13
 * 多角度扫描（0°±2°±4°±6°，抗轻微倾斜）+ 多行扫描 + 分段自适应阈值
 * @returns { code: 13 位数字或 null; scans: 扫描行数; starts: 起始模式命中次数 }
 */
export function decodeEan13(canvas: HTMLCanvasElement): { code: string | null; scans: number; starts: number } {
  const angles = [0, 2, -2, 4, -4, 6, -6]
  let total: { code: string | null; scans: number; starts: number } = { code: null, scans: 0, starts: 0 }
  for (const angle of angles) {
    const src = angle === 0 ? canvas : rotateCanvas(canvas, angle)
    const result = scanCanvasOnce(src)
    total = { code: result.code, scans: total.scans + result.scans, starts: total.starts + result.starts }
    if (result.code) {
      return total
    }
  }
  return total
}
