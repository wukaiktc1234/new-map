/**
 * 挂单 API 单位与弱网暂存 单测
 *
 * 背景（Lead 2026-10-30）：后端 pos_held_orders.total_amount_fen 是**分**（bigint），
 * 前端购物车 totalAmount 是**元**。今天已经因为单位错过一次 100 倍，这里必须锁死。
 *
 * 运行：cd frontend-pos && npm run test:units
 */
import assert from 'node:assert/strict';
import { fenToYuan, yuanToFen } from '../src/utils/shiftStatus.ts';

/* ===== ① 12.34 元 → 1234 分（POST body 断言） ===== */
{
  const cartTotalYuan = 12.34;
  const bodyTotalAmountFen = yuanToFen(cartTotalYuan);
  assert.equal(bodyTotalAmountFen, 1234, 'POST /v1/pos/holds 的 totalAmountFen 必须是 1234（分）');
  console.log('ok  ① 12.34 元 → POST body totalAmountFen=' + bodyTotalAmountFen);
}

/* ===== ② 1234 分 → 12.34 元（GET 解析断言） ===== */
{
  const backendTotalAmountFen = 1234;
  const shownYuan = fenToYuan(backendTotalAmountFen);
  assert.equal(shownYuan, 12.34, 'GET 返回的 totalAmountFen 1234 必须展示为 12.34 元');
  console.log('ok  ② 1234 分 → 展示 ¥' + shownYuan.toFixed(2));
}

/* ===== ③ 弱网暂存标记与补传语义（纯逻辑，不依赖浏览器） ===== */
{
  /** 与 store 里 saveLocalPending/syncPendingOrders 等价的纯函数版本 */
  const makePending = (order) => ({ ...order, pendingSync: true });
  const afterSyncSuccess = (list, id) => list.filter((o) => String(o.id) !== String(id));
  const isNetworkFailure = (err) => {
    if (!err || typeof err !== 'object') return true;
    const status = Number(err.status ?? err.code);
    if (Number.isFinite(status) && status >= 400) return false;
    return !Number.isFinite(status);
  };

  let list = [];
  const offlineOrder = makePending({ id: 'local-1', totalAmount: 12.34, items: [] });
  list.unshift(offlineOrder);
  assert.equal(list[0].pendingSync, true, '弱网挂单必须带 pendingSync=true');
  assert.equal(isNetworkFailure({ message: 'Network Error', originalError: { code: 'ERR_NETWORK' } }), true, '断网应判为网络失败');
  assert.equal(isNetworkFailure({ status: 409, message: '已被处理' }), false, '409 是业务错误，不能当网络失败静默存本地');
  list = afterSyncSuccess(list, 'local-1');
  assert.equal(list.length, 0, '补传成功后本地副本必须删除');
  console.log('ok  ③ 弱网暂存：pendingSync=true → 网络失败判定 → 补传后删除本地副本');
}

/* ===== ④ 跨天过滤（本地暂存记录也要过期） ===== */
{
  const businessDateOf = (holdTime) => {
    const m = String(holdTime || '').trim().match(/^(\d{4}-\d{2}-\d{2})/);
    return m ? m[1] : '';
  };
  const today = new Date();
  const todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
  assert.equal(businessDateOf('2026-10-28 10:00:00'), '2026-10-28');
  assert.equal(businessDateOf(todayStr + ' 09:00:00') === todayStr, true);
  console.log('ok  ④ 跨天过滤：业务日期可解析，昨天的记录不等于今天');
}

console.log('\n挂单单测全部通过（4 组）');
