<template>
  <div class="menu-section">
    <!-- 分类栏 -->
    <div class="category-bar-wrapper">
      <div class="category-bar">
        <div
          v-for="cat in categories"
          :key="cat.categoryId"
          class="category-item"
          :class="{ active: activeCategory === cat.categoryId }"
          @click="$emit('selectCategory', cat.categoryId)"
        >
          <span>{{ cat.categoryName }}</span>
        </div>
      </div>
    </div>

    <!-- 加载失败提示（带重试按钮） -->
    <div v-if="loadError && !loading" class="load-error">
      <el-icon class="error-icon"><WarningFilled /></el-icon>
      <div class="error-text">{{ loadError }}</div>
      <el-button type="primary" size="small" round @click="$emit('retry')">
        <el-icon class="retry-icon"><Refresh /></el-icon>
        <span>重试</span>
      </el-button>
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
          'out-of-stock': item.stock === 0
        }"
        @click="item.stock > 0 && $emit('addToCart', item)"
      >
        <!-- 套餐标签 -->
        <span v-if="item.dishType === 'combo'" class="dish-badge">套餐</span>
        <!-- 热销标签 -->
        <span v-else-if="item.isHot" class="hot-badge">热销</span>
        <!-- 库存标签 -->
        <span
          class="stock-badge"
          :class="getStockClass(item.stock)"
        >
          {{ item.stock > 0 ? `库存${item.stock}` : '售罄' }}
        </span>
        <!-- 售罄遮罩 -->
        <div v-if="item.stock === 0" class="sold-out-overlay">
          <span>暂不可选</span>
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
  WarningFilled,
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
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'selectCategory', categoryId: string): void
  (e: 'addToCart', item: DishItem): void
  /** 重试加载菜单 */
  (e: 'retry'): void
}>()

const filteredItems = computed(() => {
  if (props.activeCategory === 'all') return props.items
  return props.items.filter(
    (item) => (item as DishItem).categoryId === props.activeCategory
  )
})

const getStockClass = (stock: number): string => {
  if (stock === 0) return 'out-of-stock'
  if (stock < 10) return 'low-stock'
  return ''
}

const getFullImageUrl = (url: string): string => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `${import.meta.env.VITE_API_BASE_URL || ''}${url}`
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

.category-item {
  padding: 8px 20px;
  border-radius: 20px;
  cursor: pointer;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--pos-text-secondary, #606266);
  background-color: transparent;
  transition: all 0.25s ease;
  user-select: none;

  &:hover {
    color: var(--pos-primary, #ea580c);
    background-color: var(--pos-primary-light, rgba(234, 88, 12, 0.08));
  }

  &.active {
    background-color: var(--pos-primary, #ea580c);
    color: var(--pos-text-inverse, #ffffff);
    box-shadow: 0 2px 8px var(--pos-shadow-sm, rgba(234, 88, 12, 0.35));

    &:hover {
      background-color: var(--pos-primary, #ea580c);
      color: var(--pos-text-inverse, #ffffff);
    }
  }
}

/* 加载失败提示 */
.load-error {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 16px;
  color: var(--pos-text-secondary, #606266);

  .error-icon {
    font-size: 48px;
    color: var(--pos-danger, #f56c6c);
    opacity: 0.7;
  }

  .error-text {
    font-size: 14px;
    text-align: center;
    max-width: 360px;
    line-height: 1.6;
  }

  .retry-icon {
    margin-right: 4px;
  }
}

/* 菜品网格 */
.dish-grid {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  padding: 10px;
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
  background-color: var(--pos-bg-secondary, #f5f7fa);
  border-radius: 8px;
  padding: 6px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--pos-shadow-md, 0 6px 16px rgba(0, 0, 0, 0.1));
  }

  &:active {
    transform: translateY(0);
  }

  &.out-of-stock {
    opacity: 0.6;
    pointer-events: none;
  }
}

/* 套餐/热销标签 */
.dish-badge,
.hot-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  z-index: 2;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.dish-badge {
  background-color: var(--pos-primary, #ea580c);
  color: var(--pos-text-inverse, #ffffff);
}

.hot-badge {
  background-color: var(--pos-danger, #ef4444);
  color: var(--pos-text-inverse, #ffffff);
}

/* 库存标签 */
.stock-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 2;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 500;
  background-color: var(--pos-success, #10b981);
  color: var(--pos-text-inverse, #ffffff);

  &.low-stock {
    background-color: var(--pos-warning, #f59e0b);
  }

  &.out-of-stock {
    background-color: var(--pos-danger, #ef4444);
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
  background-color: var(--pos-glass-bg, rgba(0, 0, 0, 0.5));
  backdrop-filter: var(--pos-glass-blur, blur(2px));
  border-radius: 10px;

  span {
    color: var(--pos-text-inverse, #ffffff);
    font-size: 13px;
    font-weight: 600;
    letter-spacing: 1px;
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
  color: var(--pos-primary, #ea580c);
}

/* 菜品内容 */
.dish-content {
  text-align: center;
}

.dish-name {
  font-size: 11px;
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
  font-size: 9px;
  color: var(--pos-text-muted, #909399);
  font-weight: 500;
}

.amount {
  font-size: 13px;
  font-weight: 700;
  color: var(--pos-primary, #ea580c);
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
