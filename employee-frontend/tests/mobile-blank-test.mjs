/**
 * 移动端空白页面自动化测试
 * 模拟真机场景验证 router.isReady() 修复是否有效
 */

import { chromium } from 'playwright'

const DEV_URL = process.argv[2] || 'http://localhost:3005'

const DEVICES = [
  { name: 'iPhone 14 Pro', width: 390, height: 844, ua: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1' },
  { name: 'Android Pixel 7', width: 412, height: 915, ua: 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36' },
  { name: 'iPad Air', width: 820, height: 1180, ua: 'Mozilla/5.0 (iPad; CPU OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1' },
]

const results = []

async function test(browser, device, testName, testFn) {
  const context = await browser.newContext({
    viewport: { width: device.width, height: device.height },
    userAgent: device.ua,
    isMobile: true,
    hasTouch: true,
    deviceScaleFactor: 2,
  })
  const page = await context.newPage()
  const start = Date.now()
  try {
    const r = await testFn(page)
    results.push({ device: device.name, test: testName, passed: r.passed, detail: r.detail, ms: Date.now() - start })
  } catch (e) {
    results.push({ device: device.name, test: testName, passed: false, detail: `异常: ${e.message}`, ms: Date.now() - start })
  } finally {
    await context.close()
  }
}

async function main() {
  console.log('='.repeat(60))
  console.log('  移动端空白页面自动化测试')
  console.log('='.repeat(60))
  console.log(`  目标: ${DEV_URL}`)
  console.log(`  设备: ${DEVICES.map(d => d.name).join(', ')}`)
  console.log('='.repeat(60))

  let browser
  try {
    browser = await chromium.launch({ headless: true })
  } catch {
    console.error('无法启动 Chromium，请先运行: npx playwright install chromium')
    process.exit(1)
  }

  for (const device of DEVICES) {
    console.log(`\n📱 ${device.name} (${device.width}x${device.height})`)
    console.log('-'.repeat(40))

    // 测试1: 首次加载不刷新
    await test(browser, device, '首次加载-无刷新', async (page) => {
      const client = await page.context().newCDPSession(page)
      await client.send('Network.clearBrowserCache')
      await client.send('Network.clearBrowserCookies')

      await page.goto(DEV_URL, { waitUntil: 'domcontentloaded', timeout: 15000 })
      await page.waitForTimeout(3000)

      const info = await page.evaluate(() => {
        const app = document.getElementById('app')
        return {
          len: app?.innerHTML?.length || 0,
          hasBtn: !!app?.querySelector('button'),
          hasInput: !!app?.querySelector('input'),
          text: (app?.textContent || '').slice(0, 100),
        }
      })

      const ok = info.len > 100 && (info.hasBtn || info.hasInput)
      return {
        passed: ok,
        detail: ok
          ? `✅ #app长度=${info.len}, 有按钮=${info.hasBtn}, 有输入框=${info.hasInput}`
          : `❌ #app长度=${info.len}, 文本="${info.text}"`,
      }
    })

    // 测试2: 登录页渲染
    await test(browser, device, '登录页渲染', async (page) => {
      await page.goto(DEV_URL, { waitUntil: 'networkidle', timeout: 15000 })
      await page.waitForTimeout(2000)

      const info = await page.evaluate(() => {
        const app = document.getElementById('app')
        const html = app?.innerHTML || ''
        return {
          hasForm: html.includes('form') || html.includes('el-form') || html.includes('input'),
          hasBtn: html.includes('button') || html.includes('el-button'),
          hasLogin: html.includes('登录') || html.includes('员工'),
          len: html.length,
        }
      })

      const ok = info.hasForm && info.hasBtn
      return {
        passed: ok,
        detail: ok
          ? `✅ 表单=${info.hasForm}, 按钮=${info.hasBtn}, 登录文字=${info.hasLogin}`
          : `❌ 表单=${info.hasForm}, 按钮=${info.hasBtn}, 登录文字=${info.hasLogin}, 长度=${info.len}`,
      }
    })

    // 测试3: 模拟慢速加载（延迟JS执行）
    await test(browser, device, '延迟加载稳定性', async (page) => {
      // 注入延迟脚本，模拟慢速网络下JS延迟执行
      await page.route('**/*.js', async (route) => {
        await new Promise(r => setTimeout(r, 500)) // 每个JS文件延迟500ms
        route.continue()
      })

      await page.goto(DEV_URL, { waitUntil: 'domcontentloaded', timeout: 30000 })
      await page.waitForTimeout(5000)

      const info = await page.evaluate(() => {
        const app = document.getElementById('app')
        return { len: app?.innerHTML?.length || 0 }
      })

      const ok = info.len > 100
      return {
        passed: ok,
        detail: ok ? `✅ 延迟加载正常: #app长度=${info.len}` : `❌ 延迟加载空白: #app长度=${info.len}`,
      }
    })

    // 测试4: 连续5次刷新都不空白
    await test(browser, device, '5次加载稳定性', async (page) => {
      let failCount = 0
      for (let i = 0; i < 5; i++) {
        await page.goto(DEV_URL, { waitUntil: 'domcontentloaded', timeout: 15000 })
        await page.waitForTimeout(2000)
        const len = await page.evaluate(() => document.getElementById('app')?.innerHTML?.length || 0)
        if (len < 100) failCount++
      }
      const ok = failCount === 0
      return {
        passed: ok,
        detail: ok ? `✅ 5次加载全部正常` : `❌ ${failCount}/5次加载空白`,
      }
    })
  }

  await browser.close()

  // 输出报告
  console.log('\n' + '='.repeat(60))
  console.log('  测试报告')
  console.log('='.repeat(60))

  const passed = results.filter(r => r.passed).length
  const failed = results.filter(r => !r.passed).length

  for (const r of results) {
    const icon = r.passed ? '✅' : '❌'
    console.log(`${icon} [${r.device}] ${r.test} (${r.ms}ms)`)
    console.log(`   ${r.detail}`)
  }

  console.log('\n' + '-'.repeat(60))
  console.log(`  总计: ${results.length} | 通过: ${passed} | 失败: ${failed}`)
  console.log('-'.repeat(60))

  if (failed > 0) {
    console.log('\n❌ 存在失败项，空白问题未完全解决')
    process.exit(1)
  } else {
    console.log('\n✅ 所有测试通过，空白问题已修复！')
    process.exit(0)
  }
}

main().catch(e => { console.error('测试执行失败:', e); process.exit(1) })
