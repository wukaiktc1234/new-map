<template>
  <div class="menu-section">
    <!-- 分类栏 -->
    <div class="category-bar-wrapper">
      <div class="category-bar">
        <div
          v-for="cat in visibleCategories"
          :key="cat.categoryId"
          class="category-item"
          :class="{ active: activeCategory === cat.categoryId }"
          @click="$emit('selectCategory', cat.categoryId)"
        >
          <span>{{ cat.categoryName }}</span>
        </div>
      </div>
    </div>

    <!-- 加载失败 / 无权限：给"空状态插画 + 原因 + 下一步"，而不是一行红字 -->
    <div v-if="loadError && !loading" class="load-error" :class="{ 'is-forbidden': errorStatus === 403 }">
      <div class="load-error__art" aria-hidden="true">
        <!-- 403：锁；其它：断线 -->
        <svg v-if="errorStatus === 403" viewBox="0 0 120 120" fill="none">
          <rect x="26" y="52" width="68" height="50" rx="10" stroke="currentColor" stroke-width="3" />
          <path d="M42 52V38a18 18 0 0 1 36 0v14" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
          <circle cx="60" cy="74" r="6" fill="currentColor" />
          <path d="M60 80v8" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
        </svg>
        <svg v-else viewBox="0 0 120 120" fill="none">
          <path d="M28 74a22 22 0 0 1 6-43 26 26 0 0 1 50 8 18 18 0 0 1 4 35" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
          <path d="M40 92l8-8m24 8l-8-8" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
        </svg>
      </div>

      <div class="load-error__title">
        {{ errorStatus === 403 ? '账号暂无商品查看权限' : '菜单加载失败' }}
      </div>
      <div class="load-error__desc">
        <template v-if="errorStatus === 403">
          当前登录账号缺少商品查看权限（403）。请联系店长开通后重试；
          也可以先让店长在后台确认该角色的「商品 / 分类 / 套餐」查看权限。
        </template>
        <template v-else>
          {{ loadError }}
        </template>
      </div>

      <div class="load-error__actions">
        <el-button type="primary" round @click="$emit('retry')">
          <el-icon class="retry-icon"><Refresh /></el-icon>
          <span>重新加载</span>
        </el-button>
        <span class="load-error__hint" v-if="errorStatus === 403">权限变更后无需重启，点一次重新加载即可</span>
      </div>
    </div>

    <!-- 菜品网格 -->
    <div v-else-if="!loading" class="dish-grid">
      <div
        v-for="item in filteredItems"
        :key="item.id"
        class="dish-item"
        :class="{
          'combo-item': item.dishType === 'combo',
          'hot-item': item.isHot,
          'out-of-stock': isUnavailable(item)
        }"
        :aria-disabled="isUnavailable(item)"
        @click="handleAdd(item)"
      >
        <!-- 套餐标签 -->
        <span v-if="item.dishType === 'combo'" class="dish-badge">套餐</span>
        <!-- 热销标签 -->
        <span v-else-if="item.isHot" class="hot-badge">热销</span>
        <!-- 库存标签：只在低库存(<10)或售罄时出现，避免每个卡片都挂一个"库存99" -->
        <span
          v-if="item.stock < 10"
          class="stock-badge"
          :class="getStockClass(item.stock)"
        >
          {{ item.stock > 0 ? `仅剩 ${item.stock}` : '售罄' }}
        </span>
        <!-- 售罄遮罩 -->
        <div v-if="isUnavailable(item)" class="sold-out-overlay">
          <span>{{ unavailableReason(item) }}</span>
        </div>
        <!-- 图片或图标 -->
        <div class="dish-visual">
          <img
            v-if="item.imageUrl && getFullImageUrl(item.imageUrl)"
            :src="getFullImageUrl(item.imageUrl)"
            :alt="item.name"
            class="dish-image"
          />
          <el-icon v-else class="dish-icon">
            <component :is="getDishIcon(item)" />
          </el-icon>
        </div>
        <!-- 菜品信息 -->
        <div class="dish-content">
          <div class="dish-name">{{ item.name }}</div>
          <div class="dish-price">
            <span class="currency">&yen;</span>
            <span class="amount">{{ item.price.toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <!-- 空数据提示（无菜品且非加载/错误状态） -->
      <div v-if="filteredItems.length === 0" class="empty-menu">
        <el-icon class="empty-icon"><Bowl /></el-icon>
        <span>暂无可售菜品</span>
      </div>
    </div>

    <!-- 加载骨架屏 -->
    <div v-else class="dish-grid skeleton-grid">
      <div v-for="i in 8" :key="i" class="dish-item-skeleton">
        <div class="skeleton-visual pos-skeleton"></div>
        <div class="skeleton-content">
          <div class="skeleton-title pos-skeleton"></div>
          <div class="skeleton-price pos-skeleton"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getFullImageUrl } from '@/utils/image'
import {
  Food,
  Bowl,
  Burger,
  Chicken,
  Fries,
  Dessert,
  IceCream,
  CoffeeCup,
  ColdDrink,
  MilkTea,
  Mug,
  HotWater,
  Sugar,
  Apple,
  Cherry,
  Grape,
  Pear,
  Watermelon,
  Orange,
  KnifeFork,
  Dish,
  ForkSpoon,
  PieChart,
  Refresh,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

interface Category {
  categoryId: string
  categoryName: string
}

interface DishItem {
  id: string
  name: string
  price: number
  dishType?: string
  isHot?: boolean
  stock: number
  imageUrl?: string
  categoryId?: string
  items?: Array<{ foodId: string; foodName: string; quantity: number; price?: number }>
  /** 商品状态：后端可能给 status / saleStatus / soldOut / available，这里统一兜底 */
  status?: string | number
  saleStatus?: string | number
  soldOut?: boolean
  available?: boolean
  onSale?: boolean
}

interface ComboItem extends DishItem {
  dishType: 'combo'
}

interface Props {
  categories: Category[]
  items: DishItem[] | ComboItem[]
  activeCategory: string
  loading: boolean
  /** 加载错误信息（非空时显示错误提示与重试按钮） */
  loadError?: string | null
  /** 错误状态码：403 走"无权限"空态，其余走"加载失败" */
  errorStatus?: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'selectCategory', categoryId: string): void
  (e: 'addToCart', item: DishItem): void
  /** 重试加载菜单 */
  (e: 'retry'): void
}>()

// P1-COMBO-ORDER-001: 恢复套餐分类与套餐项展示
const visibleCategories = computed(() => props.categories)

const filteredItems = computed(() => {
  const base = props.items
  if (props.activeCategory === 'all') return base
  return base.filter(
    (item) => (item as DishItem).categoryId === props.activeCategory
  )
})

const getStockClass = (stock: number): string => {
  if (stock === 0) return 'out-of-stock'
  if (stock < 10) return 'low-stock'
  return ''
}

/**
 * 不可售判定（前端防呆，后端仍会拦）。
 * 覆盖字段差异：stock<=0 / soldOut / available=false / onSale=false /
 * status 或 saleStatus 命中售罄、下架、停售等取值（含数字 0）。
 */
const UNAVAILABLE_STATUS = ['0', 'soldout', 'sold_out', 'out_of_stock', 'off', 'offline', 'stopsale', 'stop', 'disabled', 'unavailable']

const unavailableReason = (item: DishItem): string => {
  if (item.soldOut === true) return '已售罄'
  if (item.available === false || item.onSale === false) return '已下架'
  if (typeof item.stock === 'number' && item.stock <= 0) return '已售罄'
  const rawStatus = item.saleStatus ?? item.status
  if (rawStatus !== undefined && rawStatus !== null && rawStatus !== '') {
    if (UNAVAILABLE_STATUS.includes(String(rawStatus).toLowerCase())) return '已下架'
  }
  return ''
}

const isUnavailable = (item: DishItem): boolean => unavailableReason(item) !== ''

const handleAdd = (item: DishItem): void => {
  if (isUnavailable(item)) return   // 按钮已置灰，这里再兜一层，防止键盘/误触
  emit('addToCart', item)
}

// 食物图标组件映射：根据菜品名称关键字匹配对应图标，未匹配则使用 Food 默认图标
// 与管理端保持同类风格，统一使用 Element Plus Icons 而非 emoji
const NAME_RULES: Array<{ keywords: string[]; icon: Component }> = [
  { keywords: ['面', '粉', '粥', '饭', '炒饭', '盖浇', '拌'], icon: Bowl },
  { keywords: ['汉堡', '卷', '三明治'], icon: Burger },
  { keywords: ['鸡', '鸭', '烤', '翅', '腿'], icon: Chicken },
  { keywords: ['薯', '炸', '小吃', '串', '烤肠'], icon: Fries },
  { keywords: ['甜点', '蛋糕', '布丁', '甜品'], icon: Dessert },
  { keywords: ['冰淇淋', '雪糕', '冰激凌'], icon: IceCream },
  { keywords: ['咖啡', '拿铁', '美式', '卡布奇诺'], icon: CoffeeCup },
  { keywords: ['奶茶', '红茶', '绿茶', '茶'], icon: MilkTea },
  { keywords: ['可乐', '雪碧', '汽水', '冷饮', '冰饮', '果汁', '汁'], icon: ColdDrink },
  { keywords: ['豆浆', '豆奶', '酸奶'], icon: Mug },
  { keywords: ['热水', '温水', '开水'], icon: HotWater },
  { keywords: ['糖', '糖果'], icon: Sugar },
  { keywords: ['苹果'], icon: Apple },
  { keywords: ['樱桃'], icon: Cherry },
  { keywords: ['葡萄'], icon: Grape },
  { keywords: ['梨'], icon: Pear },
  { keywords: ['西瓜'], icon: Watermelon },
  { keywords: ['橙', '橘子', '柑'], icon: Orange },
  { keywords: ['套餐', '组合', '拼盘'], icon: ForkSpoon },
  { keywords: ['汤', '煲'], icon: HotWater },
  { keywords: ['饼', '披萨', '派'], icon: PieChart },
  { keywords: ['牛排', '排'], icon: KnifeFork },
  { keywords: ['鱼', '虾', '蟹', '海鲜'], icon: Dish },
]

/**
 * 根据菜品名称返回最匹配的 Element Plus 图标组件
 * 套餐统一使用 ForkSpoon 图标，未匹配关键字时使用 Food 作为默认占位图标
 */
const getDishIcon = (item: DishItem): Component => {
  // 套餐使用专用图标
  if (item.dishType === 'combo') return ForkSpoon
  const name = item.name || ''
  for (const rule of NAME_RULES) {
    if (rule.keywords.some(kw => name.includes(kw))) {
      return rule.icon
    }
  }
  // 默认占位图标
  return Food
}
</script>

<style scoped>
.menu-section {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: var(--pos-bg-primary, #ffffff);
}

/* 分类栏 */
.category-bar-wrapper {
  flex-shrink: 0;
  background-color: var(--pos-bg-secondary, #f5f7fa);
  border-bottom: 1px solid var(--pos-border-color, #e4e7ed);
}

.category-bar {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  overflow-x: auto;
  scrollbar-width: thin;

  &::-webkit-scrollbar {
    height: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background-color: var(--pos-text-muted, rgba(0, 0, 0, 0.15));
    border-radius: 4px;
  }
}

/* 分类胶囊：两端全圆角（border-radius: 9999px），流线更顺 */
.category-item {
  padding: 9px 20px;
  border-radius: var(--pos-radius-full, 9999px);
  cursor: pointer;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--pos-text-secondary, #606266);
  background-color: transparent;
  transition: background-color var(--pos-transition-fast, 150ms ease),
    color var(--pos-transition-fast, 150ms ease),
    transform var(--pos-transition-fast, 150ms ease);
  user-select: none;

  &:hover {
    color: var(--pos-primary, #e3651c);
    background-color: var(--pos-primary-soft, #fdf1e7);
  }

  &:active {
    transform: scale(0.97);
  }

  &.active {
    background-color: var(--pos-primary, #e3651c);
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(227, 101, 28, 0.28);

    &:hover {
      background-color: var(--pos-primary, #e3651c);
      color: var(--pos-text-inverse, #ffffff);
    }
  }
}

/* ==================== 加载失败 / 无权限空态 ====================
 * 旧版只有一行红字 + 小按钮，收银员看到会以为"系统坏了"。
 * 现在用「插画 + 标题 + 原因 + 下一步」的空状态结构：
 * 403 用锁形插画 + 中性色（这是权限问题，不是故障），其它错误保留断线插画 + 危险色。
 */
.load-error {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--pos-spacing-xs, 8px);
  padding: 40px 24px;
  text-align: center;
  color: var(--pos-text-secondary, #606266);

  .load-error__art {
    width: 96px;
    height: 96px;
    margin-bottom: var(--pos-spacing-xs, 8px);
    color: var(--pos-danger, #c94b4b);
    opacity: 0.85;
  }

  .load-error__art svg {
    width: 100%;
    height: 100%;
  }

  .load-error__title {
    font-size: 16px;
    font-weight: 700;
    color: var(--pos-text-primary, #1e293b);
  }

  .load-error__desc {
    max-width: 420px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--pos-text-muted, #94a3b8);
  }

  .load-error__actions {
    display: flex;
    align-items: center;
    gap: var(--pos-spacing-xs, 8px);
    margin-top: var(--pos-spacing-xs, 8px);
  }

  .load-error__hint {
    font-size: 12px;
    color: var(--pos-text-muted, #94a3b8);
  }

  .retry-icon {
    margin-right: 4px;
  }

  /* 403 是"没权限"，不是"坏了"：换成中性/警示色，避免红色故障观感 */
  &.is-forbidden {
    .load-error__art {
      color: var(--pos-warning, #c2861f);
    }
  }
}

/* 菜品网格 */
.dish-grid {
  flex: 1;
  overflow-y: auto;
  display: grid;
  /* 自适应列数：随容器宽度自动增减，缩放/窄屏都不会挤成 11px 小字 */
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: var(--pos-spacing-xs, 8px);
  padding: var(--pos-spacing-sm, 16px);
  align-content: start;
  scrollbar-width: thin;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background-color: var(--pos-text-muted, rgba(0, 0, 0, 0.15));
    border-radius: 3px;

    &:hover {
      background-color: var(--pos-text-secondary, rgba(0, 0, 0, 0.25));
    }
  }
}

/* 空菜单提示 */
.empty-menu {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 0;
  color: var(--pos-text-muted, #909399);

  .empty-icon {
    font-size: 48px;
    opacity: 0.6;
  }
}

/* 菜品卡片 */
.dish-item {
  position: relative;
  overflow: hidden;
  background-color: var(--pos-bg-primary, #ffffff);
  border: 1px solid var(--pos-border-light, #f1f5f9);
  border-radius: var(--pos-radius-md, 12px);
  padding: 8px;
  box-shadow: var(--pos-shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.05));
  cursor: pointer;
  transition: background-color var(--pos-transition-fast, 150ms ease),
    transform var(--pos-transition-fast, 150ms ease),
    box-shadow var(--pos-transition-fast, 150ms ease);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--pos-shadow-md, 0 6px 16px rgba(0, 0, 0, 0.1));
    background-color: var(--pos-primary-soft, #fdf1e7);
    border-color: var(--pos-primary, #e3651c);
  }

  &:active {
    transform: scale(0.98);
  }

  &.out-of-stock {
    opacity: 0.6;
    pointer-events: none;
  }
}

/* 套餐/热销标签：软底 + 深字，比纯色实心块更耐看 */
.dish-badge,
.hot-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  z-index: 2;
  padding: 2px 7px;
  border-radius: var(--pos-radius-xs, 6px);
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.dish-badge {
  background-color: var(--pos-primary-soft, #fbf1ea);
  color: var(--pos-primary-dark, #a94f1f);
  box-shadow: inset 0 0 0 1px rgba(201, 98, 42, 0.2);
}

.hot-badge {
  background-color: var(--pos-danger-soft, #fbf0f0);
  color: var(--pos-danger, #c94b4b);
  box-shadow: inset 0 0 0 1px rgba(201, 75, 75, 0.18);
}

/* 库存标签：只在低库存/售罄时出现，作为"预警"而非"常态装饰" */
.stock-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 2;
  padding: 2px 7px;
  border-radius: var(--pos-radius-xs, 6px);
  font-size: 10.5px;
  font-weight: 600;
  background-color: var(--pos-warning-soft, #fbf5e8);
  color: #8a6114;
  box-shadow: inset 0 0 0 1px rgba(194, 134, 31, 0.22);

  &.low-stock {
    background-color: var(--pos-warning-soft, #fbf5e8);
    color: #8a6114;
  }

  &.out-of-stock {
    background-color: var(--pos-danger-soft, #fbf0f0);
    color: var(--pos-danger, #c94b4b);
    box-shadow: inset 0 0 0 1px rgba(201, 75, 75, 0.2);
  }
}

/* 售罄遮罩 */
.sold-out-overlay {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 固定用半透明黑：深浅两种主题下都能看清白色胶囊文案，
   * 之前用 --pos-text-inverse 在深色下变成深字叠深底 → 整张卡片看起来"空白" */
  background-color: rgba(12, 12, 14, 0.55);
  backdrop-filter: blur(2px);
  border-radius: var(--pos-radius-md, 12px);

  span {
    color: #ffffff;
    background: rgba(0, 0, 0, 0.45);
    padding: 3px 10px;
    border-radius: var(--pos-radius-full, 9999px);
    font-size: 12.5px;
    font-weight: 600;
    letter-spacing: 0.5px;
    white-space: nowrap;
  }
}

/* 菜品视觉区 */
.dish-visual {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
  background-color: var(--pos-bg-primary, #ffffff);
  border-radius: 5px;
  overflow: hidden;
}

.dish-image {
  max-height: 100%;
  max-width: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.dish-icon {
  font-size: 26px;
  color: var(--pos-primary, #e3651c);
}

/* 菜品内容 */
.dish-content {
  text-align: center;
}

.dish-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--pos-text-primary, #303133);
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-price {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 1px;
}

.currency {
  font-size: 11px;
  color: var(--pos-text-muted, #909399);
  font-weight: 500;
}

.amount {
  font-size: 15px;
  font-weight: 700;
  color: var(--pos-primary, #c9622a);
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-variant-numeric: tabular-nums;
}

/* 骨架屏 */
.skeleton-grid .dish-item-skeleton {
  background-color: var(--pos-bg-secondary, #f5f7fa);
  border-radius: 8px;
  padding: 6px;
}

.skeleton-visual {
  height: 50px;
  border-radius: 5px;
  margin-bottom: 4px;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.skeleton-title {
  height: 11px;
  width: 70%;
  border-radius: 3px;
}

.skeleton-price {
  height: 14px;
  width: 50%;
  border-radius: 3px;
  margin: 0 auto;
}

.pos-skeleton {
  background: linear-gradient(
    90deg,
    var(--pos-bg-secondary, #f0f2f5) 25%,
    var(--pos-bg-hover, #e8eaed) 50%,
    var(--pos-bg-secondary, #f0f2f5) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 4px;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
