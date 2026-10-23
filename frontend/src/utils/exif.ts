/**
 * JPEG EXIF 元数据解析（无依赖，纯 DataView 读取）
 * 提取：拍摄时间（DateTimeOriginal）、设备型号（Make+Model）、GPS（可选）
 * 元数据缺失时不报错，返回空对象（证据链标记"元数据缺失"由调用方处理）
 */

export interface ExifMeta {
  /** 拍摄时间（yyyy-MM-dd HH:mm:ss） */
  takenAt?: string
  /** 设备型号 */
  deviceModel?: string
  /** GPS 十进制坐标（纬度,经度） */
  gps?: string
}

const TAGS: Record<number, string> = {
  0x0110: 'make',
  0x0111: 'model',
  0x0132: 'dateTime',
  0x9003: 'dateTimeOriginal',
}

function readAscii(data: DataView, offset: number, length: number): string {
  let result = ''
  for (let i = 0; i < length; i++) {
    const code = data.getUint8(offset + i)
    if (code === 0) break
    result += String.fromCharCode(code)
  }
  return result.trim()
}

function readRational(data: DataView, offset: number, littleEndian: boolean): number {
  const numerator = data.getUint32(offset, littleEndian)
  const denominator = data.getUint32(offset + 4, littleEndian)
  return denominator === 0 ? 0 : numerator / denominator
}

function readGpsCoordinate(data: DataView, ifdOffset: number, littleEndian: boolean, isLat: boolean): string | null {
  const gpsTag = isLat ? 0x0002 : 0x0004
  const refTag = isLat ? 0x0001 : 0x0003
  const count = data.getUint16(ifdOffset, littleEndian)
  let degrees = 0
  let minutes = 0
  let seconds = 0
  let ref = ''
  for (let i = 0; i < count; i++) {
    const entry = ifdOffset + 2 + i * 12
    const tag = data.getUint16(entry, littleEndian)
    if (tag === gpsTag) {
      const type = data.getUint16(entry + 2, littleEndian)
      const valueOffset = data.getUint32(entry + 8, littleEndian)
      if (type === 5 && valueOffset + 24 <= data.byteLength) {
        degrees = readRational(data, valueOffset, littleEndian)
        minutes = readRational(data, valueOffset + 8, littleEndian)
        seconds = readRational(data, valueOffset + 16, littleEndian)
      }
    } else if (tag === refTag) {
      const type = data.getUint16(entry + 2, littleEndian)
      const value = data.getUint32(entry + 8, littleEndian)
      if (type === 2 && value < data.byteLength) {
        ref = readAscii(data, value, 2)
      }
    }
  }
  if (degrees === 0 && minutes === 0 && seconds === 0) return null
  const decimal = degrees + minutes / 60 + seconds / 3600
  if (ref === 'S' || ref === 'W') return `-${decimal.toFixed(6)}`
  return decimal.toFixed(6)
}

function parseTiff(data: DataView, tiffOffset: number): ExifMeta {
  const byteOrder = data.getUint16(tiffOffset, false)
  const littleEndian = byteOrder === 0x4949 // 'II'
  const ifdOffset = tiffOffset + data.getUint32(tiffOffset + 4, littleEndian)
  const meta: ExifMeta = {}
  const count = data.getUint16(ifdOffset, littleEndian)
  let gpsIfd = 0
  let make = ''
  let model = ''
  for (let i = 0; i < count; i++) {
    const entry = ifdOffset + 2 + i * 12
    const tag = data.getUint16(entry, littleEndian)
    const type = data.getUint16(entry + 2, littleEndian)
    const value = data.getUint32(entry + 8, littleEndian)
    const tagName = TAGS[tag]
    if (tag === 0x8825) {
      gpsIfd = value
    }
    if (!tagName) continue
    if (type === 2) {
      const length = data.getUint32(entry + 4, littleEndian)
      const strOffset = length > 4 ? tiffOffset + value : entry + 8
      const str = readAscii(data, strOffset, length)
      if (tagName === 'dateTimeOriginal' || tagName === 'dateTime') {
        meta.takenAt = str.replace(/^(\d{4}):(\d{2}):(\d{2})/, '$1-$2-$3')
      } else if (tagName === 'make') {
        make = str
      } else if (tagName === 'model') {
        model = str
      }
    }
  }
  const deviceParts = [make, model].filter(Boolean)
  if (deviceParts.length > 0) {
    meta.deviceModel = deviceParts.join(' ')
  }
  if (gpsIfd > 0 && gpsIfd + 2 <= data.byteLength) {
    const lat = readGpsCoordinate(data, tiffOffset + gpsIfd, littleEndian, true)
    const lng = readGpsCoordinate(data, tiffOffset + gpsIfd, littleEndian, false)
    if (lat && lng) {
      meta.gps = `${lat},${lng}`
    }
  }
  return meta
}

export async function readExif(file: Blob): Promise<ExifMeta> {
  try {
    const buffer = await file.arrayBuffer()
    const data = new DataView(buffer)
    if (data.byteLength < 4 || data.getUint16(0, false) !== 0xffd8) {
      return {}
    }
    let offset = 2
    while (offset + 4 <= data.byteLength) {
      const marker = data.getUint16(offset, false)
      if (marker === 0xffe1) {
        const length = data.getUint16(offset + 2, false)
        const exifMagic = readAscii(data, offset + 4, 6)
        if (exifMagic === 'Exif\u0000\u0000') {
          return parseTiff(data, offset + 10)
        }
        offset += 2 + length
      } else if (marker >= 0xffd0 && marker <= 0xffd9) {
        offset += 2
      } else {
        offset += 2 + data.getUint16(offset + 2, false)
      }
    }
  } catch {
    // 解析失败不抛错，元数据按缺失处理
  }
  return {}
}

/** 补充定位：浏览器地理定位授权（可选，失败静默，不阻塞流程） */
export function requestGeolocation(timeoutMs = 4000): Promise<string> {
  return new Promise((resolve) => {
    if (!('geolocation' in navigator)) {
      resolve('')
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve(`${pos.coords.latitude.toFixed(6)},${pos.coords.longitude.toFixed(6)}`),
      () => resolve(''),
      { enableHighAccuracy: false, timeout: timeoutMs, maximumAge: 60000 },
    )
  })
}
