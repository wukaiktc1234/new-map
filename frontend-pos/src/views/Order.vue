<template>
  <div class="pos-order">
    <!-- 头部 - 玻璃拟态效果 -->
    <div class="header">
      <el-button @click="$router.back()" :icon="ArrowLeft" class="back-btn">返回</el-button>
      <div class="header-center">
        <h2>新建订单</h2>
        <span class="subtitle">Create New Order</span>
      </div>
      <div class="table-select">
        <span class="label">桌号</span>
        <el-select v-model="tableNumber" placeholder="选择" style="width: 120px" class="table-selector">
          <el-option v-for="i in 20" :key="i" :label="`${i}号桌`" :value="i" />
        </el-select>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="content">
      <!-- 菜单区域 -->
      <div class="menu-area">
        <!-- 分类导航 - 带滑动指示器 -->
        <div class="category-tabs-wrapper">
          <div class="category-tabs" ref="categoryTabsRef">
            <div class="category-indicator" :style="indicatorStyle"></div>
            <el-radio-group v-model="activeCategory" @change="filterDishes">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button 
                v-for="cat in categories" 
                :key="cat.categoryId" 
                :label="cat.categoryId"
                :ref="(el: any) => { if (el) categoryItems[cat.categoryId] = el.$el as HTMLElement }"
              >
                {{ cat.categoryName }}
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 类型选择 -->
        <div class="type-tabs">
          <el-radio-group v-model="activeType">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="single">单品</el-radio-button>
            <el-radio-button label="combo">套餐</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 菜品网格 - 带动画效果 -->
        <div class="dish-grid" v-if="!loading">
          <div
            v-for="(item, index) in filteredItems"
            :key="item.id"
            class="dish-card"
            :class="{ 'combo-card': item.dishType === 'combo', 'adding': addingItem === item.id }"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="addToCart(item)"
          >
            <!-- 套餐标识 -->
            <div class="combo-badge" v-if="item.dishType === 'combo'">
              <span class="badge-text">套餐</span>
              <div class="badge-glow"></div>
            </div>

            <!-- 菜品图片区域 -->
            <div class="dish-image">
              <div class="image-placeholder">
                <el-icon :size="48"><Bowl /></el-icon>
              </div>
              <div class="image-overlay"></div>
              <div class="hover-effect"></div>
            </div>

            <!-- 菜品信息 -->
            <div class="dish-info">
              <div class="dish-name">{{ item.name }}</div>
              <div class="dish-desc" v-if="item.description">{{ item.description }}</div>
              <div class="dish-footer">
                <div class="dish-price">
                  <span class="currency">¥</span>
                  <span class="amount">{{ item.price }}</span>
                </div>
                <div class="add-btn">
                  <el-icon><Plus /></el-icon>
                </div>
              </div>
              <!-- 套餐内容 -->
              <div class="combo-items" v-if="item.dishType === 'combo' && item.items">
                <span v-for="i in item.items.slice(0, 3)" :key="i.foodId" class="item-tag">
                  {{ i.foodName }}×{{ i.quantity }}
                </span>
                <span v-if="item.items.length > 3" class="more">+{{ item.items.length - 3 }}项</span>
              </div>
            </div>

            <!-- 添加动画 -->
            <div class="add-animation" v-if="addingItem === item.id">
              <div class="add-circle"></div>
              <span class="add-text">+1</span>
            </div>
          </div>
          <el-empty v-if="filteredItems.length === 0 && !loading" description="暂无菜品" />
        </div>

        <!-- 骨架屏 -->
        <div class="dish-grid" v-else>
          <div v-for="i in 9" :key="i" class="dish-card-skeleton">
            <div class="skeleton-image pos-skeleton"></div>
            <div class="skeleton-info">
              <div class="skeleton-title pos-skeleton pos-skeleton-text"></div>
              <div class="skeleton-desc pos-skeleton pos-skeleton-text" style="width: 80%"></div>
              <div class="skeleton-footer">
                <div class="skeleton-price pos-skeleton" style="width: 60px; height: 24px;"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 购物车区域 - 玻璃拟态效果 -->
      <div class="cart-area">
        <div class="cart-header">
          <div class="cart-title">
            <el-icon class="cart-icon"><ShoppingCart /></el-icon>
            <h3>购物车</h3>
            <span class="cart-count" v-if="cart.length > 0">{{ totalQuantity }}</span>
          </div>
          <el-button text type="danger" @click="clearCart" class="clear-btn" v-if="cart.length > 0">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
        </div>

        <!-- 购物车列表 -->
        <div class="cart-list">
          <transition-group name="cart-item" tag="div" class="cart-items">
            <div v-for="(item, index) in cart" :key="item.id" class="cart-item">
              <div class="item-content">
                <div class="item-info">
                  <span class="item-name">
                    {{ item.name }}
                    <el-tag v-if="item.dishType === 'combo'" type="warning" size="small" effect="dark">套餐</el-tag>
                  </span>
                  <span class="item-price">¥{{ item.price }}</span>
                </div>
                <div class="item-total">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
              </div>
              <div class="item-actions">
                <el-button-group>
                  <el-button size="small" @click="decreaseItem(index)" class="qty-btn minus">-</el-button>
                  <el-button size="small" class="qty-btn num">{{ item.quantity }}</el-button>
                  <el-button size="small" @click="increaseItem(index)" class="qty-btn plus">+</el-button>
                </el-button-group>
              </div>
            </div>
          </transition-group>
          <div v-if="cart.length === 0" class="empty-cart">
            <div class="empty-icon">
              <el-icon :size="56"><ShoppingCart /></el-icon>
            </div>
            <p class="empty-text">购物车为空</p>
            <p class="empty-hint">点击菜品添加到购物车</p>
          </div>
        </div>

        <!-- 购物车底部 -->
        <div class="cart-footer">
          <div class="total-section">
            <div class="total-row">
              <span class="label">商品数量</span>
              <span class="value">{{ totalQuantity }} 件</span>
            </div>
            <div class="total-row main">
              <span class="label">合计</span>
              <span class="amount">
                <span class="currency">¥</span>
                <span class="number">{{ totalAmount.toFixed(2) }}</span>
              </span>
            </div>
          </div>
          <el-button type="primary" size="large" :disabled="cart.length === 0" @click="goToPayment" class="checkout-btn">
            <span>去结算</span>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Bowl, Plus, ShoppingCart, Delete, ArrowRight } from '@element-plus/icons-vue'
import { posApi, type Category, type Dish, type Combo } from '@/api/posApi'

interface CartItem {
  id: string;
  name: string;
  price: number;
  quantity: number;
  dishType: string;
  items?: { foodId: string; foodName: string; quantity: number }[];
}

const router = useRouter()
const tableNumber = ref<number>()
const activeCategory = ref('all')
const activeType = ref('all')
const loading = ref(false)
const addingItem = ref<string | null>(null)

const categories = ref<Category[]>([])
const dishes = ref<Dish[]>([])
const combos = ref<Combo[]>([])
const cart = ref<CartItem[]>([])

// 分类指示器
const categoryTabsRef = ref<HTMLElement | null>(null)
const categoryItems = ref<Record<string, HTMLElement>>({})
const indicatorStyle = ref({})

const updateIndicator = () => {
  nextTick(() => {
    const activeEl = categoryItems.value[activeCategory.value]
    if (activeEl && categoryTabsRef.value) {
      const tabsEl = categoryTabsRef.value.querySelector('.el-radio-group') as HTMLElement
      if (tabsEl) {
        const radioBtn = activeEl.querySelector('.el-radio-button__inner') as HTMLElement
        if (radioBtn) {
          indicatorStyle.value = {
            left: `${radioBtn.offsetLeft}px`,
            width: `${radioBtn.offsetWidth}px`
          }
        }
      }
    }
  })
}

watch(activeCategory, updateIndicator)

const filteredItems = computed(() => {
  let items: any[] = []
  
  if (activeType.value === 'all' || activeType.value === 'single') {
    items = items.concat(dishes.value.map(d => ({
      id: d.dishId,
      name: d.dishName,
      price: d.price,
      description: d.description,
      categoryId: d.categoryId,
      dishType: 'single'
    })))
  }
  
  if (activeType.value === 'all' || activeType.value === 'combo') {
    items = items.concat(combos.value.map(c => ({
      id: c.comboId,
      name: c.comboName,
      price: c.price,
      description: c.description,
      categoryId: 'combo',
      dishType: 'combo',
      items: c.items
    })))
  }
  
  if (activeCategory.value !== 'all') {
    items = items.filter(item => item.categoryId === activeCategory.value)
  }
  
  return items
})

const totalQuantity = computed(() => cart.value.reduce((sum, item) => sum + item.quantity, 0))
const totalAmount = computed(() => cart.value.reduce((sum, item) => sum + item.price * item.quantity, 0))

const filterDishes = () => {
  updateIndicator()
}

const addToCart = (item: any) => {
  addingItem.value = item.id
  setTimeout(() => {
    addingItem.value = null
  }, 400)

  const existing = cart.value.find(i => i.id === item.id)
  if (existing) {
    existing.quantity++
  } else {
    cart.value.push({
      id: item.id,
      name: item.name,
      price: item.price,
      quantity: 1,
      dishType: item.dishType,
      items: item.items
    })
  }
}

const increaseItem = (index: number) => {
  cart.value[index].quantity++
}

const decreaseItem = (index: number) => {
  if (cart.value[index].quantity > 1) {
    cart.value[index].quantity--
  } else {
    cart.value.splice(index, 1)
  }
}

const clearCart = () => {
  cart.value = []
}

const goToPayment = () => {
  router.push({
    path: '/payment',
    query: {
      table: tableNumber.value?.toString(),
      items: JSON.stringify(cart.value),
      total: totalAmount.value.toString()
    }
  })
}

const loadMenu = async () => {
  loading.value = true
  try {
    const menu = await posApi.getFullMenu() as any
    categories.value = menu.categories || []
    dishes.value = menu.dishes || []
    combos.value = menu.combos || []
    updateIndicator()
  } catch (error) {
    console.error('加载菜单失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMenu()
})
</script>

<style scoped>
/* ========== 主容器 ========== */
.pos-order {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

/* ========== 头部 ========== */
.header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.back-btn {
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.back-btn:hover {
  transform: translateX(-4px);
}

.header-center {
  flex: 1;
  text-align: center;
}

.header-center h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  background: var(--pos-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 11px;
  color: #9ca3af;
  letter-spacing: 1px;
}

.table-select {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 12px;
}

.table-select .label {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
}

.table-selector :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: none;
  border: 2px solid #e5e7eb;
  transition: all 0.3s ease;
}

.table-selector :deep(.el-input__wrapper:hover) {
  border-color: #ff6b35;
}

/* ========== 主内容区 ========== */
.content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
}

/* ========== 菜单区域 ========== */
.menu-area {
  flex: 2;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ========== 分类导航 ========== */
.category-tabs-wrapper {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  padding: 12px 16px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  position: relative;
}

.category-tabs {
  position: relative;
  overflow-x: auto;
}

.category-tabs::-webkit-scrollbar {
  height: 4px;
}

.category-tabs::-webkit-scrollbar-thumb {
  background: #e5e7eb;
  border-radius: 2px;
}

.category-tabs :deep(.el-radio-group) {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  position: relative;
}

.category-tabs :deep(.el-radio-button__inner) {
  border-radius: 10px !important;
  border: 2px solid #e5e7eb;
  padding: 8px 20px;
  font-weight: 500;
  transition: all 0.3s ease;
  background: #fff;
}

.category-tabs :deep(.el-radio-button__inner:hover) {
  border-color: #ff6b35;
  color: #ff6b35;
}

.category-tabs :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
}

.category-indicator {
  position: absolute;
  bottom: 0;
  height: 3px;
  background: linear-gradient(90deg, #ff6b35, #ff8a5c);
  border-radius: 3px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 2px 8px rgba(255, 107, 53, 0.4);
}

/* ========== 类型选择 ========== */
.type-tabs {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  padding: 8px 16px;
  border-radius: 12px;
}

.type-tabs :deep(.el-radio-button__inner) {
  border-radius: 8px !important;
  border: 2px solid #e5e7eb;
  padding: 6px 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.type-tabs :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--pos-accent-gradient);
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

/* ========== 菜品网格 ========== */
.dish-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  overflow-y: auto;
  padding: 4px;
}

/* ========== 菜品卡片 ========== */
.dish-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
  animation: pos-fade-in 0.4s ease-out backwards;
}

.dish-card:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  border-color: #ff6b35;
}

.dish-card:active {
  transform: translateY(-3px) scale(0.98);
}

.dish-card.combo-card {
  background: linear-gradient(135deg, #fff5e6 0%, #fff 100%);
  border-color: #ffc069;
}

.dish-card.combo-card:hover {
  border-color: #ff9c4a;
  box-shadow: 0 12px 32px rgba(255, 156, 74, 0.2);
}

.dish-card.adding {
  transform: scale(0.95);
}

/* 套餐标识 */
.combo-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
}

.badge-text {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
}

.badge-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(255, 107, 53, 0.3) 0%, transparent 70%);
  animation: pos-pulse 2s ease-in-out infinite;
}

/* 菜品图片 */
.dish-image {
  position: relative;
  width: 100%;
  height: 100px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  overflow: hidden;
}

.image-placeholder {
  z-index: 1;
  opacity: 0.6;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(0, 0, 0, 0.05) 100%);
}

.hover-effect {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(255, 107, 53, 0.1) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.dish-card:hover .hover-effect {
  opacity: 1;
}

/* 菜品信息 */
.dish-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.dish-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 15px;
  margin-bottom: 4px;
  line-height: 1.3;
}

.dish-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.dish-price {
  color: #ef4444;
  font-weight: 700;
  font-size: 18px;
}

.dish-price .currency {
  font-size: 12px;
  margin-right: 2px;
}

.add-btn {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
  transition: all 0.3s ease;
}

.dish-card:hover .add-btn {
  transform: scale(1.1) rotate(90deg);
}

/* 套餐内容 */
.combo-items {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.item-tag {
  font-size: 10px;
  padding: 3px 8px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 6px;
  color: #6b7280;
}

.more {
  font-size: 10px;
  color: #ff6b35;
  font-weight: 500;
}

/* 添加动画 */
.add-animation {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  z-index: 20;
}

.add-circle {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(34, 197, 94, 0.2);
  animation: add-circle-expand 0.4s ease-out;
}

@keyframes add-circle-expand {
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

.add-text {
  font-size: 24px;
  font-weight: 700;
  color: #22c55e;
  animation: add-text-pop 0.4s ease-out;
}

@keyframes add-text-pop {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.3);
    opacity: 1;
  }
  100% {
    transform: scale(1);
    opacity: 0;
  }
}

/* ========== 骨架屏 ========== */
.dish-card-skeleton {
  background: #ffffff;
  border-radius: 16px;
  padding: 12px;
  border: 2px solid #f0f0f0;
}

.skeleton-image {
  width: 100%;
  height: 100px;
  border-radius: 12px;
  margin-bottom: 10px;
}

.skeleton-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

/* ========== 购物车区域 ========== */
.cart-area {
  flex: 1;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(180deg, #fafbfc 0%, #ffffff 100%);
}

.cart-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cart-icon {
  font-size: 20px;
  color: #ff6b35;
}

.cart-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.cart-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
}

.clear-btn {
  font-weight: 500;
}

/* ========== 购物车列表 ========== */
.cart-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  background: linear-gradient(135deg, #fafbfc 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.cart-item:hover {
  background: #fff7ed;
  border-color: #ff6b35;
}

.item-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-name {
  font-weight: 600;
  font-size: 14px;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-price {
  font-size: 12px;
  color: #9ca3af;
}

.item-total {
  font-weight: 600;
  color: #ef4444;
  font-size: 15px;
}

.item-actions {
  margin-left: 12px;
}

.qty-btn {
  min-width: 32px;
  font-weight: 600;
}

.qty-btn.minus, .qty-btn.plus {
  background: #f0f0f0;
  border-color: #e5e7eb;
}

.qty-btn.num {
  background: #fff;
  border-color: #e5e7eb;
  color: #1f2937;
  font-weight: 700;
}

/* 购物车动画 */
.cart-item-enter-active,
.cart-item-leave-active {
  transition: all 0.3s ease;
}

.cart-item-enter-from,
.cart-item-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* ========== 空购物车 ========== */
.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #9ca3af;
}

.empty-icon {
  opacity: 0.4;
  animation: pos-float 3s ease-in-out infinite;
}

.empty-text {
  font-size: 15px;
  font-weight: 500;
  margin-top: 16px;
  color: #6b7280;
}

.empty-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

/* ========== 购物车底部 ========== */
.cart-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: linear-gradient(180deg, #fafbfc 0%, #ffffff 100%);
}

.total-section {
  margin-bottom: 16px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.total-row .label {
  color: #6b7280;
}

.total-row .value {
  font-weight: 600;
  color: #1f2937;
}

.total-row.main {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 2px solid #f0f0f0;
}

.total-row.main .label {
  font-weight: 600;
  color: #1f2937;
}

.total-row .amount {
  font-size: 26px;
  font-weight: 700;
  color: #ef4444;
}

.total-row .currency {
  font-size: 16px;
  margin-right: 2px;
}

.checkout-btn {
  width: 100%;
  height: 52px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
  border: none;
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.3);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.checkout-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(255, 107, 53, 0.4);
}

.checkout-btn:active:not(:disabled) {
  transform: translateY(0);
}

.arrow-icon {
  font-size: 18px;
  transition: transform 0.3s ease;
}

.checkout-btn:hover .arrow-icon {
  transform: translateX(4px);
}
</style>
