<template>
  <div class="customer-order">
    <div class="header">
      <div class="header-left">
        <div class="shop-name">智慧餐厅</div>
        <div class="table-info" v-if="table">
          <span class="table-badge">{{ table.tableNumber }}</span>
          <span class="table-name">{{ table.tableName }}</span>
        </div>
      </div>
      <div class="header-right">
        <button class="my-orders-btn" @click="showMyOrders = true">
          <el-icon><List /></el-icon>
          我的订单
          <span class="order-badge" v-if="myOrders.length > 0">{{ myOrders.length }}</span>
        </button>
      </div>
    </div>

    <div class="main-content">
      <div class="category-sidebar">
        <div 
          v-for="cat in categories" 
          :key="cat.categoryId"
          class="category-item"
          :class="{ active: activeCategory === cat.categoryId }"
          @click="scrollToCategory(cat.categoryId)"
        >
          <span class="category-name">{{ cat.categoryName }}</span>
          <span class="category-count" v-if="getCategoryCartCount(cat.categoryId) > 0">
            {{ getCategoryCartCount(cat.categoryId) }}
          </span>
        </div>
      </div>

      <div class="dish-content" ref="dishContentRef">
        <div v-for="cat in categoriesWithDishes" :key="cat.categoryId" class="category-section" :ref="el => categoryRefs[cat.categoryId] = el">
          <div class="section-title">
            <span class="title-text">{{ cat.categoryName }}</span>
            <span class="title-count">({{ cat.dishes.length }})</span>
          </div>
          
          <div class="dish-list">
            <div v-for="item in cat.dishes" :key="item.id" class="dish-item">
              <div class="dish-image">
                <img v-if="item.image" :src="getImageUrl(item.image)" :alt="item.name" @error="handleImageError" />
                <div v-else class="no-image">
                  <el-icon :size="32"><Picture /></el-icon>
                </div>
              </div>
              <div class="dish-info">
                <div class="dish-name">{{ item.name }}</div>
                <div class="dish-desc">{{ item.description || '精选美味' }}</div>
                <div class="dish-sales">
                  <span class="sales-count">月售 {{ item.salesCount ?? '--' }}</span>
                  <span class="like-count">赞 {{ item.likeCount ?? '--' }}</span>
                </div>
                <div class="dish-bottom">
                  <div class="dish-price">
                    <span class="price-symbol">¥</span>
                    <span class="price-value">{{ item.price.toFixed(2) }}</span>
                  </div>
                  <div class="dish-actions">
                    <transition name="fade">
                      <div v-if="getCartCount(item.id) > 0" class="quantity-control">
                        <button class="btn-minus" @click="decreaseItem(item)">
                          <el-icon><Minus /></el-icon>
                        </button>
                        <span class="quantity">{{ getCartCount(item.id) }}</span>
                      </div>
                    </transition>
                    <button class="btn-plus" @click="addToCart(item)">
                      <el-icon><Plus /></el-icon>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="empty-state" v-if="!loading && dishes.length === 0">
          <el-icon :size="64"><Goods /></el-icon>
          <p>暂无菜品</p>
        </div>

        <div class="loading-state" v-if="loading">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
      </div>
    </div>

    <div class="float-cart" :class="{ expanded: cartTotalCount > 0 }" @click="cartTotalCount > 0 ? (showCart = true) : null">
      <div class="cart-icon-box">
        <el-icon :size="24"><ShoppingCart /></el-icon>
        <transition name="pop">
          <span class="cart-count" v-if="cartTotalCount > 0">{{ cartTotalCount }}</span>
        </transition>
      </div>
      <transition name="expand">
        <div class="cart-detail" v-if="cartTotalCount > 0">
          <div class="cart-price">
            <span class="price-symbol">¥</span>
            <span class="price-value">{{ cartTotalAmount.toFixed(2) }}</span>
          </div>
          <button class="checkout-btn" @click.stop="submitOrder">去结算</button>
        </div>
      </transition>
    </div>

    <el-drawer v-model="showCart" direction="btt" :size="cartExpanded ? '90%' : '50%'" :with-header="false">
      <div class="cart-drawer">
        <div class="cart-header">
          <span class="cart-title">已选商品 ({{ cartTotalCount }}件)</span>
          <div class="header-actions">
            <button class="expand-btn" @click="cartExpanded = !cartExpanded" v-if="cart.length > 3">
              <el-icon><FullScreen v-if="!cartExpanded" /><Close v-else /></el-icon>
              {{ cartExpanded ? '收起' : '展开' }}
            </button>
            <button class="clear-btn" @click="clearCart">
              <el-icon><Delete /></el-icon>
              清空
            </button>
          </div>
        </div>
        <div class="cart-list" :class="{ expanded: cartExpanded }">
          <div v-for="item in cart" :key="item.id" class="cart-item">
            <div class="item-info">
              <div class="item-name">{{ item.name }}</div>
              <div class="item-price">¥{{ item.price.toFixed(2) }}</div>
            </div>
            <div class="item-control">
              <button class="btn-minus" @click="decreaseItem(item)">
                <el-icon><Minus /></el-icon>
              </button>
              <span class="quantity">{{ item.quantity }}</span>
              <button class="btn-plus" @click="increaseItem(item)">
                <el-icon><Plus /></el-icon>
              </button>
            </div>
          </div>
        </div>
        <div class="cart-footer">
          <div class="total-info">
            <span class="total-label">合计</span>
            <span class="total-price">
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ cartTotalAmount.toFixed(2) }}</span>
            </span>
          </div>
          <button class="submit-btn" @click="submitOrder">提交订单</button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="showSuccess" width="300px" center :show-close="false" :close-on-click-modal="false" :lock-scroll="false">
      <div class="success-dialog">
        <div class="success-icon">
          <el-icon :size="48" color="#07c160"><CircleCheckFilled /></el-icon>
        </div>
        <div class="success-title">下单成功</div>
        <div class="success-order">订单号: {{ orderNumber }}</div>
        <div class="success-tip">请等待服务员送餐</div>
      </div>
      <template #footer>
        <el-button type="primary" round @click="continueOrder" style="width: 100%;">
          继续点餐
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="showMyOrders" direction="rtl" size="80%" title="我的订单">
      <div class="orders-drawer">
        <div class="orders-list" v-if="myOrders.length > 0">
          <div v-for="order in myOrders" :key="order.orderId" class="order-card">
            <div class="order-header">
              <span class="order-number">{{ order.orderNumber }}</span>
              <div class="order-status-group">
                <span class="order-status" :class="getStatusClass(order.status)">
                  {{ getKitchenStatusText(order.status) }}
                </span>
                <span class="order-pay-status" :class="getPayStatusClass(order.orderStatus)">
                  {{ getStatusText(order.status, order.orderStatus) }}
                </span>
              </div>
            </div>
            <div class="order-items">
              <div v-for="item in order.items" :key="item.name" class="order-item">
                <span class="item-name">{{ item.name }}</span>
                <span class="item-quantity">x{{ item.quantity }}</span>
                <span class="item-price">¥{{ item.price }}</span>
              </div>
            </div>
            <div class="order-footer">
              <span class="order-time">{{ formatTime(order.createTime) }}</span>
              <span class="order-total">合计: ¥{{ order.totalAmount }}</span>
            </div>
          </div>
        </div>
        <div class="empty-orders" v-else>
          <el-icon :size="48"><Document /></el-icon>
          <p>暂无订单</p>
        </div>
        <div class="orders-refresh">
          <el-button @click="loadMyOrders" :loading="loadingOrders">
            <el-icon><Refresh /></el-icon>
            刷新订单
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart, Minus, Plus, CircleCheckFilled, Loading, Goods, Picture, Delete, FullScreen, Close, List, Document, Refresh } from '@element-plus/icons-vue'
import request from '../api/request'

interface Category {
  categoryId: string
  categoryName: string
}

interface Dish {
  id: string
  name: string
  price: number
  description: string
  image: string
  categoryId: string
  dishType: string
  /** 销售数量 */
  salesCount?: number
  /** 点赞数量 */
  likeCount?: number
}

interface CartItem extends Dish {
  quantity: number
}

interface DiningTable {
  id: number
  tableNumber: string
  tableName: string
  qrCode: string
  status: string
}

const route = useRoute()
const loading = ref(true)
const table = ref<DiningTable | null>(null)
const categories = ref<Category[]>([])
const dishes = ref<Dish[]>([])
const activeCategory = ref('all')
const cart = ref<CartItem[]>([])
const showCart = ref(false)
const showSuccess = ref(false)
const orderNumber = ref('')
const categoryRefs = ref<Record<string, any>>({})
// 菜品内容区域引用（滚动定位使用）
const dishContentRef = ref<HTMLElement | null>(null)
const cartExpanded = ref(false)
const showMyOrders = ref(false)
const myOrders = ref<any[]>([])
const loadingOrders = ref(false)

const categoriesWithDishes = computed(() => {
  return categories.value
    .filter(cat => cat.categoryId !== 'all')
    .map(cat => ({
      ...cat,
      dishes: dishes.value.filter(d => d.categoryId === cat.categoryId)
    }))
    .filter(cat => cat.dishes.length > 0)
})

const cartTotalCount = computed(() => {
  return cart.value.reduce((sum, item) => sum + item.quantity, 0)
})

const cartTotalAmount = computed(() => {
  return cart.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const getImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/uploads')) {
    return `/api${url}`
  }
  return url.startsWith('/') ? url : `/${url}`
}

const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.style.display = 'none'
}

const getCartCount = (id: string) => {
  const item = cart.value.find(i => i.id === id)
  return item ? item.quantity : 0
}

const getCategoryCartCount = (categoryId: string) => {
  return cart.value
    .filter(item => item.categoryId === categoryId)
    .reduce((sum, item) => sum + item.quantity, 0)
}

const scrollToCategory = (categoryId: string) => {
  activeCategory.value = categoryId
  const el = categoryRefs.value[categoryId]
  if (el && dishContentRef.value) {
    // 确保在菜品内容区域内滚动
    const container = dishContentRef.value
    const top = el.offsetTop - container.offsetTop
    container.scrollTo({ top, behavior: 'smooth' })
  } else if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const loadTableInfo = async () => {
  const qrCode = route.query.table as string
  if (!qrCode) return
  
  try {
    const res = await request.get(`/v1/pos/tables/qrcode/${qrCode}`) as any
    table.value = res
  } catch (error) {
    console.error('获取桌台信息失败:', error)
  }
}

const loadMenu = async () => {
  try {
    const res = await request.get('/v1/pos/api/menu') as any
    const data = res || {}
    
    categories.value = [{ categoryId: 'all', categoryName: '推荐' }, ...(data.categories || [])]
    
    const dishList: Dish[] = []
    const dishData = data.dishes || []
    for (const dish of dishData) {
      dishList.push({
        id: dish.dishId || dish.id,
        name: dish.dishName || dish.name,
        price: dish.price || 0,
        description: dish.description || '',
        image: dish.imageUrl || dish.image || '',
        categoryId: dish.categoryId || '',
        dishType: dish.dishType || 'single'
      })
    }
    dishes.value = dishList
  } catch (error) {
    console.error('加载菜单失败:', error)
  } finally {
    loading.value = false
  }
}

const addToCart = (item: Dish) => {
  const existing = cart.value.find(i => i.id === item.id)
  if (existing) {
    existing.quantity++
  } else {
    cart.value.push({ ...item, quantity: 1 })
  }
}

const increaseItem = (item: CartItem) => {
  item.quantity++
}

const decreaseItem = (item: Dish | CartItem) => {
  const cartItem = cart.value.find(i => i.id === item.id)
  if (!cartItem) return
  
  if (cartItem.quantity > 1) {
    cartItem.quantity--
  } else {
    const index = cart.value.findIndex(i => i.id === item.id)
    if (index > -1) {
      cart.value.splice(index, 1)
    }
  }
}

const clearCart = () => {
  cart.value = []
  showCart.value = false
}

const submitOrder = async () => {
  if (cart.value.length === 0) {
    ElMessage.warning('请先选择菜品')
    return
  }
  
  if (!table.value) {
    ElMessage.error('桌台信息错误')
    return
  }
  
  try {
    const orderData = {
      tableId: table.value.id,
      tableNumber: table.value.tableNumber,
      items: cart.value.map(item => ({
        id: item.id,
        name: item.name,
        price: Number(item.price),
        quantity: Number(item.quantity)
      })),
      totalAmount: Number(cartTotalAmount.value)
    }
    
    const res = await request.post('/v1/pos/orders/order/table', orderData) as any
    orderNumber.value = res?.orderNumber || Date.now().toString().slice(-6)
    showSuccess.value = true
    showCart.value = false
    cart.value = []
  } catch (error: any) {
    console.error('下单失败:', error)
    ElMessage.error(error.response?.data?.message || error.message || '下单失败')
  }
}

const continueOrder = () => {
  showSuccess.value = false
  loadMyOrders()
}

const loadMyOrders = async () => {
  if (!table.value) return
  
  loadingOrders.value = true
  try {
    const res = await request.get(`/v1/pos/orders/table/${table.value.tableNumber}`) as any
    myOrders.value = res || []
  } catch (error) {
    console.error('加载订单失败:', error)
  } finally {
    loadingOrders.value = false
  }
}

const getStatusClass = (status: string) => {
  const statusMap: Record<string, string> = {
    'pending': 'status-pending',
    'received': 'status-received',
    'making': 'status-making',
    'completed': 'status-completed',
    'served': 'status-served',
    'cancelled': 'status-cancelled'
  }
  return statusMap[status] || 'status-pending'
}

const getPayStatusClass = (orderStatus?: number) => {
  if (orderStatus === 1) return 'pay-status-paid'
  if (orderStatus === 0) return 'pay-status-unpaid'
  return 'pay-status-other'
}

const getKitchenStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'pending': '⏳ 待制作',
    'received': '📋 已接单',
    'making': '👨‍🍳 制作中',
    'completed': '✅ 已完成',
    'served': '🍽️ 已上菜',
    'cancelled': '❌ 已取消'
  }
  return statusMap[status] || '待处理'
}

const getStatusText = (status: string, orderStatus?: number) => {
  if (orderStatus !== undefined) {
    const orderStatusMap: Record<number, string> = {
      0: '待支付',
      1: '已支付',
      2: '制作中',
      3: '已退款',
      4: '已完成',
      5: '已上菜',
      6: '已取消'
    }
    return orderStatusMap[orderStatus] || '待处理'
  }
  
  const statusMap: Record<string, string> = {
    'pending': '待处理',
    'making': '制作中',
    'completed': '已完成',
    'served': '已上菜',
    'cancelled': '已取消'
  }
  return statusMap[status] || '待处理'
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadTableInfo()
  loadMenu()
})
</script>

<style scoped>
.customer-order {
  min-height: 100vh;
  height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  background: linear-gradient(135deg, #ffd54f 0%, #ffb300 100%);
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.shop-name {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.table-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-badge {
  background: #ff5722;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.table-name {
  font-size: 12px;
  color: #666;
}

.header-right {
  display: flex;
  align-items: center;
}

.my-orders-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  padding: 8px 12px;
  border-radius: 20px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  position: relative;
}

.my-orders-btn:active {
  background: rgba(255, 255, 255, 0.7);
}

.order-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ff5722;
  color: white;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.category-sidebar {
  width: 85px;
  background: #f8f8f8;
  overflow-y: auto;
  flex-shrink: 0;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

.category-item {
  padding: 14px 8px;
  font-size: 13px;
  color: #666;
  text-align: center;
  position: relative;
  cursor: pointer;
  transition: all 0.2s;
}

.category-item.active {
  background: white;
  color: #333;
  font-weight: 500;
}

.category-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: #ffd54f;
  border-radius: 0 2px 2px 0;
}

.category-count {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #ff5722;
  color: white;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dish-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 100px;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  touch-action: pan-y;
}

.category-section {
  background: white;
  margin-bottom: 10px;
}

.section-title {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
  background: white;
  z-index: 10;
}

.title-count {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

.dish-list {
  padding: 0 12px;
}

.dish-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.dish-item:last-child {
  border-bottom: none;
}

.dish-image {
  width: 90px;
  height: 90px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dish-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  color: #ccc;
}

.dish-info {
  flex: 1;
  margin-left: 12px;
  display: flex;
  flex-direction: column;
}

.dish-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.dish-desc {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-sales {
  font-size: 11px;
  color: #bbb;
  margin-bottom: 8px;
}

.dish-sales span {
  margin-right: 12px;
}

.dish-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.dish-price {
  color: #ff5722;
  font-weight: bold;
}

.price-symbol {
  font-size: 12px;
}

.price-value {
  font-size: 18px;
}

.dish-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-minus, .btn-plus {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-minus {
  background: #f0f0f0;
  color: #666;
}

.btn-plus {
  background: #ffd54f;
  color: #333;
}

.quantity {
  font-size: 14px;
  font-weight: 500;
  min-width: 20px;
  text-align: center;
}

.empty-state, .loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #999;
}

.float-cart {
  position: fixed;
  bottom: 20px;
  left: 16px;
  background: rgba(0, 0, 0, 0.75);
  border-radius: 50%;
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 100;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: all 0.3s ease;
}

.float-cart.expanded {
  border-radius: 30px;
  padding: 6px 6px 6px 10px;
  background: rgba(0, 0, 0, 0.85);
}

.cart-icon-box {
  position: relative;
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #ffd54f 0%, #ffb300 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.float-cart:hover .cart-icon-box {
  transform: scale(1.05);
}

.cart-count {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #ff5722;
  color: white;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  border: 2px solid rgba(0, 0, 0, 0.85);
}

.cart-detail {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.cart-price {
  color: white;
  font-weight: bold;
}

.cart-price .price-value {
  font-size: 18px;
}

.checkout-btn {
  background: linear-gradient(135deg, #ffd54f 0%, #ffb300 100%);
  color: #333;
  border: none;
  padding: 0 18px;
  height: 36px;
  border-radius: 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
}

.pop-enter-active, .pop-leave-active {
  transition: all 0.3s ease;
}

.pop-enter-from, .pop-leave-to {
  transform: scale(0);
  opacity: 0;
}

.expand-enter-active, .expand-leave-active {
  transition: all 0.3s ease;
}

.expand-enter-from, .expand-leave-to {
  opacity: 0;
  max-width: 0;
  margin-left: 0;
}

.expand-enter-to, .expand-leave-from {
  opacity: 1;
  max-width: 200px;
  margin-left: 0;
}

.cart-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.cart-title {
  font-size: 16px;
  font-weight: 500;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.expand-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f5f5f5;
  border: none;
  color: #666;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #999;
  font-size: 13px;
  cursor: pointer;
}

.cart-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
  max-height: 200px;
  transition: max-height 0.3s ease;
}

.cart-list.expanded {
  max-height: calc(90vh - 180px);
}

.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.cart-item .item-info {
  flex: 1;
}

.cart-item .item-name {
  font-size: 15px;
  color: #333;
  margin-bottom: 4px;
}

.cart-item .item-price {
  color: #ff5722;
  font-size: 14px;
  font-weight: 500;
}

.item-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.total-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.total-label {
  font-size: 14px;
  color: #666;
}

.total-price {
  color: #ff5722;
  font-weight: bold;
}

.total-price .price-value {
  font-size: 22px;
}

.submit-btn {
  background: linear-gradient(135deg, #ffd54f 0%, #ffb300 100%);
  color: #333;
  border: none;
  padding: 0 32px;
  height: 44px;
  border-radius: 22px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 179, 0, 0.3);
}

.total-price .price-value {
  font-size: 22px;
}

.success-dialog {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  margin-bottom: 16px;
}

.success-title {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 8px;
}

.success-order {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
  font-family: monospace;
}

.success-tip {
  font-size: 13px;
  color: #999;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active, .slide-up-leave-active {
  transition: all 0.3s;
}

.slide-up-enter-from, .slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

.orders-drawer {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.orders-list {
  flex: 1;
  overflow-y: auto;
}

.order-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.order-number {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.order-status-group {
  display: flex;
  gap: 6px;
  align-items: center;
}

.order-status {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.order-pay-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 400;
}

.status-pending {
  background: #fff3e0;
  color: #f57c00;
}

.status-received {
  background: #e8eaf6;
  color: #3f51b5;
}

.status-making {
  background: #e3f2fd;
  color: #1976d2;
}

.status-completed {
  background: #e8f5e9;
  color: #388e3c;
}

.status-served {
  background: #f3e5f5;
  color: #7b1fa2;
}

.status-cancelled {
  background: #ffebee;
  color: #d32f2f;
}

.pay-status-paid {
  background: #e8f5e9;
  color: #2e7d32;
}

.pay-status-unpaid {
  background: #fff8e1;
  color: #f9a825;
}

.pay-status-other {
  background: #f5f5f5;
  color: #757575;
}

.order-items {
  margin-bottom: 12px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
}

.order-item .item-name {
  flex: 1;
  color: #333;
}

.order-item .item-quantity {
  color: #999;
  margin: 0 12px;
}

.order-item .item-price {
  color: #666;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.order-time {
  font-size: 12px;
  color: #999;
}

.order-total {
  font-size: 14px;
  font-weight: 600;
  color: #ff5722;
}

.empty-orders {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.empty-orders p {
  margin-top: 12px;
  font-size: 14px;
}

.orders-refresh {
  padding: 16px 0;
  text-align: center;
}
</style>
