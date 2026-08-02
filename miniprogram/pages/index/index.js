const app = getApp()
const store = require('../../utils/store.js')

const REFRESH_COOLDOWN = 5000

Page({
  data: {
    statusBarHeight: 0,
    showLoginModal: false,
    showStoreSelector: false,
    pendingOrderType: '',
    isLoggedIn: false,
    userInfo: null,
    currentOrder: null,
    refreshing: false,
    canRefresh: true,
    cooldownText: ''
  },

  lastRefreshTime: 0,
  cooldownTimer: null,

  onLoad() {
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight
    })
    this.checkLoginStatus()
    this.loadCurrentOrder()
  },

  onShow() {
    this.checkLoginStatus()
    this.loadCurrentOrder()
  },

  onUnload() {
    this.clearCooldownTimer()
  },

  clearCooldownTimer() {
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer)
      this.cooldownTimer = null
    }
  },

  checkLoginStatus() {
    const isLoggedIn = app.globalData.isLoggedIn || false
    const userInfo = app.globalData.userInfo || null
    
    this.setData({ 
      isLoggedIn,
      userInfo
    })
  },

  async loadCurrentOrder() {
    try {
      const ordersObj = wx.getStorageSync('orders') || {}
      const orders = Object.values(ordersObj)
      const processingOrder = orders.find(o => 
        o.status === 'pending' || o.status === 'processing' || o.status === 'ready'
      )
      
      if (processingOrder) {
        const orderWithStep = this.addStepInfo(processingOrder)
        this.setData({ currentOrder: orderWithStep })
      } else {
        this.setData({ currentOrder: null })
      }
    } catch (e) {
      console.error('加载订单失败', e)
      this.setData({ currentOrder: null })
    }
  },

  addStepInfo(order) {
    const statusToStep = {
      'pending': 1,
      'processing': 2,
      'ready': 3,
      'completed': 4
    }
    const statusToTip = {
      'pending': '订单已提交，等待商家确认',
      'processing': '您的餐品正在制作中，请耐心等待',
      'ready': '您的餐品已制作完成，请尽快取餐',
      'completed': '订单已完成，感谢您的光临'
    }
    return {
      ...order,
      step: statusToStep[order.status] || 1,
      statusTip: statusToTip[order.status] || '您的订单正在处理中'
    }
  },

  async refreshOrderStatus() {
    if (!this.data.canRefresh || this.data.refreshing) {
      return
    }
    
    const order = this.data.currentOrder
    if (!order || !order.orderNumber) return
    
    this.lastRefreshTime = Date.now()
    this.startCooldown()
    
    this.setData({ refreshing: true })
    
    try {
      const res = await app.request({
        url: '/pos/order/' + order.orderNumber,
        method: 'GET'
      })
      
      if (res && res.code === 0 && res.data) {
        const serverOrder = res.data
        const statusMap = {
          'pending': 'pending',
          'paid': 'processing', 
          'making': 'processing',
          'delivering': 'processing',
          'ready': 'ready',
          'completed': 'completed',
          'cancelled': 'cancelled'
        }
        const statusTextMap = {
          'pending': '待支付',
          'paid': '已支付',
          'making': '制作中',
          'delivering': '配送中',
          'ready': '待取餐',
          'completed': '已完成',
          'cancelled': '已取消'
        }
        
        const newStatus = statusMap[serverOrder.status] || order.status
        const newStatusText = statusTextMap[serverOrder.status] || order.statusText
        
        const updatedOrder = {
          ...order,
          status: newStatus,
          statusText: newStatusText,
          pickupNumber: serverOrder.pickupNumber || order.pickupNumber
        }
        
        app.saveOrder(updatedOrder)
        
        this.setData({ 
          currentOrder: updatedOrder,
          refreshing: false
        })
        
        if (newStatus === 'ready') {
          wx.showToast({
            title: '餐品已准备好！',
            icon: 'success'
          })
        } else if (newStatus === 'completed' || newStatus === 'cancelled') {
          this.setData({ currentOrder: null })
        } else {
          wx.showToast({
            title: '状态已更新',
            icon: 'success'
          })
        }
      } else {
        throw new Error('查询失败')
      }
    } catch (e) {
      console.error('刷新订单状态失败', e)
      this.setData({ refreshing: false })
      wx.showToast({
        title: '刷新失败，请重试',
        icon: 'none'
      })
    }
  },

  startCooldown() {
    this.setData({ canRefresh: false })
    this.clearCooldownTimer()
    
    let remaining = Math.ceil(REFRESH_COOLDOWN / 1000)
    this.setData({ cooldownText: `${remaining}s` })
    
    this.cooldownTimer = setInterval(() => {
      remaining--
      if (remaining <= 0) {
        this.clearCooldownTimer()
        this.setData({ 
          canRefresh: true,
          cooldownText: ''
        })
      } else {
        this.setData({ cooldownText: `${remaining}s` })
      }
    }, 1000)
  },

  showLoginModal() {
    this.setData({ showLoginModal: true })
  },

  hideLoginModal() {
    this.setData({ showLoginModal: false })
  },

  handleCloseLoginModal() {
    this.setData({ showLoginModal: false })
  },

  onLoginSuccess(e) {
    const userInfo = e.detail.userInfo
    app.globalData.isLoggedIn = true
    app.globalData.userInfo = userInfo
    
    this.setData({ 
      isLoggedIn: true,
      userInfo: {
        name: userInfo.name || '美食爱好者',
        points: userInfo.points || 0,
        coupons: 0
      },
      showLoginModal: false 
    })
  },

  handlePhoneLogin(e) {
    const code = e.detail.code
    if (code) {
      this.doLogin(code)
    }
  },

  goToDinein() {
    wx.navigateTo({
      url: '/pages/menu-dinein/menu-dinein'
    })
  },

  goToPickup() {
    const currentStore = store.getCurrentStore()
    console.log('goToPickup currentStore:', currentStore)
    if (currentStore) {
      wx.navigateTo({
        url: '/pages/menu-pickup/menu-pickup'
      })
    } else {
      console.log('显示门店选择器')
      this.setData({ 
        showStoreSelector: true,
        pendingOrderType: 'pickup'
      })
    }
  },

  goToTakeout() {
    const currentStore = store.getCurrentStore()
    console.log('goToTakeout currentStore:', currentStore)
    if (currentStore) {
      wx.navigateTo({
        url: '/pages/menu-takeout/menu-takeout'
      })
    } else {
      console.log('显示门店选择器')
      this.setData({ 
        showStoreSelector: true,
        pendingOrderType: 'takeout'
      })
    }
  },

  onStoreConfirm(e) {
    const { orderType } = e.detail
    this.setData({ 
      showStoreSelector: false,
      pendingOrderType: ''
    })
    
    if (orderType === 'pickup') {
      wx.navigateTo({
        url: '/pages/menu-pickup/menu-pickup'
      })
    } else if (orderType === 'takeout') {
      wx.navigateTo({
        url: '/pages/menu-takeout/menu-takeout'
      })
    }
  },

  onStoreSelectorClose() {
    this.setData({ 
      showStoreSelector: false,
      pendingOrderType: ''
    })
  },

  goToOrderDetail() {
    const order = this.data.currentOrder
    if (order && order.orderNumber) {
      wx.navigateTo({
        url: `/pages/order-detail/order-detail?orderNumber=${order.orderNumber}`,
        fail: (err) => {
          console.error('跳转失败', err)
          wx.showToast({
            title: '页面跳转失败',
            icon: 'none'
          })
        }
      })
    } else {
      wx.showToast({
        title: '订单信息不完整',
        icon: 'none'
      })
    }
  },

  goToPay() {
    const order = this.data.currentOrder
    if (order && order.orderNumber) {
      wx.navigateTo({
        url: `/pages/order-detail/order-detail?orderNumber=${order.orderNumber}`,
        fail: (err) => {
          console.error('跳转失败', err)
          wx.showToast({
            title: '页面跳转失败',
            icon: 'none'
          })
        }
      })
    }
  },

  showMemberCode() {
    if (!this.data.isLoggedIn) {
      this.showLoginModal()
      return
    }
    wx.navigateTo({
      url: '/pages/member-code/member-code'
    })
  },

  goToPoints() {
    if (!this.data.isLoggedIn) {
      this.showLoginModal()
      return
    }
    wx.navigateTo({
      url: '/pages/points-mall/points-mall'
    })
  },

  goToCoupons() {
    if (!this.data.isLoggedIn) {
      this.showLoginModal()
      return
    }
    wx.navigateTo({
      url: '/pages/coupons/coupons'
    })
  },

  goToMember() {
    if (!this.data.isLoggedIn) {
      this.showLoginModal()
      return
    }
    wx.navigateTo({
      url: '/pages/member/member'
    })
  },

  goToRecharge() {
    if (!this.data.isLoggedIn) {
      this.showLoginModal()
      return
    }
    wx.navigateTo({
      url: '/pages/recharge/recharge'
    })
  },

  claimNewUserCoupon() {
    if (this.data.isLoggedIn) {
      wx.showToast({
        title: '您已领取过新人优惠券',
        icon: 'none'
      })
      return
    }
    
    this.showLoginModal()
  }
})
