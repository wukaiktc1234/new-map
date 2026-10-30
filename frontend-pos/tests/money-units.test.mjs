/**
 * 金额单位边界单测（元 ⇄ 分）
 *
 * 为什么必须锁死：今天第 3 次单位事故 —— 后端 ExceptionOrder.amount = 2200（**分**），
 * 前端当元用，卡片显示 ¥2200.00、发 amountFen=220000，后端直接 422。
 * 收银员看到 ¥2200 会不敢收钱，所以这条必须有回归测试。
 *
 * 运行：cd frontend-pos && npm run test:units
 * （用 node 的 --experimental-strip-types 直接跑 TS，不额外引入测试框架）
 */
import assert from 'node:assert/strict';
import { fenToYuan, yuanToFen } from '../src/utils/shiftStatus.ts';

const cases = [
  // [输入, 期望, 说明]
  [0, 0, '0 元 → 0 分'],
  [0.01, 1, '最小分'],
  [0.1, 10, '一角'],
  [22, 2200, '异常单真实金额（¥22 → 2200 分）'],
  [1234.56, 123456, '两位小数'],
  [999999.99, 99999999, '大额不丢精度'],
  [-12.5, -1250, '负数（退款方向）保留符号'],
  [null, 0, 'null → 0（不发 NaN 给后端）'],
  [undefined, 0, 'undefined → 0'],
  [Number.NaN, 0, 'NaN → 0'],
];

let pass = 0;
for (const [input, expected, label] of cases) {
  const got = yuanToFen(input);
  assert.equal(got, expected, `yuanToFen(${String(input)}) 期望 ${expected} 实际 ${got} —— ${label}`);
  console.log(`ok  yuanToFen(${String(input)}) = ${got}   ${label}`);
  pass += 1;
}

const backCases = [
  [0, 0],
  [1, 0.01],
  [10, 0.1],
  [2200, 22],
  [123456, 1234.56],
  [99999999, 999999.99],
  [-1250, -12.5],
  [null, 0],
  [undefined, 0],
];
for (const [input, expected] of backCases) {
  const got = fenToYuan(input);
  assert.equal(got, expected, `fenToYuan(${String(input)}) 期望 ${expected} 实际 ${got}`);
  console.log(`ok  fenToYuan(${String(input)}) = ${got}`);
  pass += 1;
}

// 往返不丢精度（元 → 分 → 元）
for (const yuan of [0, 0.01, 0.1, 22, 1234.56, 999999.99]) {
  const round = fenToYuan(yuanToFen(yuan));
  assert.equal(round, yuan, `往返 ${yuan} 元 得到 ${round}`);
  pass += 1;
}

// 关键反例：2200 分 绝不能被当成 2200 元
assert.equal(fenToYuan(2200), 22, '2200 分必须是 ¥22.00，不是 ¥2200.00');
assert.equal(yuanToFen(22), 2200, '¥22 必须发 2200 分，不是 220000 分');

console.log(`\n全部通过：${pass} 条断言 + 2 条反例`);
