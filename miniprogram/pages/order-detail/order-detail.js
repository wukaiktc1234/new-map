const app = getApp()

const REFRESH_COOLDOWN = 5000

Page({
  data: {
    statusBarHeight: 0,
    headerHeight: 0,
    showContact: false,
    order: {},
    countdown: '',
    countdownSeconds: 0,
    showExpiredTip: false,
    loading: false,
    refreshing: false,
    canRefresh: true,
    cooldownText: ''
  },

  countdownTimer: null,
  cooldownTimer: null,
  orderNumber: '',

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      headerHeight: systemInfo.statusBarHeight * 2 + 72
    })
    
    if (options.orderNumber) {
      this.orderNumber = options.orderNumber
      this.loadOrderFromBackend(options.orderNumber)
    } else if (options.id) {
      this.loadOrderDetail(options.id)
    }
  },

  onUnload() {
    this.clearCountdownTimer()
    this.clearCooldownTimer()
  },

  clearCountdownTimer() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },

  clearCooldownTimer() {
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer)
      this.cooldownTimer = null
    }
  },

  async loadOrderFromBackend(orderNumber) {
    this.setData({ loading: true })
    
    try {
      const res = await app.request({
        url: '/pos/order/' + orderNumber,
        method: 'GET'
      })
      
      if (res.code === 0 && res.data) {
        const order = this.transformOrder(res.data)
        this.setData({ order, loading: false })
        
        if (order.status === 'pending') {
          this.startCountdown()
        }
      } else {
        throw new Error((res && res.message) || '订单不存在')
      }
    } catch (e) {
      console.error('加载订单失败', e)
      
      const localOrders = app.globalData.orders || {}
      const localOrder = Object.values(localOrders).find(o => 
        o.orderNumber === orderNumber || o.id === orderNumber
      )
      
      if (localOrder) {
        this.setData({ order: localOrder, loading: false })
        if (localOrder.status === 'pending') {
          this.startCountdown()
        }
      } else {
        this.setData({ loading: false })
        wx.showModal({
          title: '订单不存在',
          content: '未找到该订单，请返回订单列表查看',
          showCancel: false,
          success: () => {
            wx.navigateBack({
              fail: () => {
                wx.reLaunch({
                  url: '/pages/index/index'
                })
              }
            })
          }
        })
      }
    }
  },

  transformOrder(order) {
    const statusMap = {
      'pending': { status: 'pending', statusText: '待支付' },
      'paid': { status: 'processing', statusText: '已支付' },
      'making': { status: 'processing', statusText: '制作中' },
      'delivering': { status: 'processing', statusText: '配送中' },
      'ready': { status: 'ready', statusText: '待取餐' },
      'completed': { status: 'completed', statusText: '已完成' },
      'cancelled': { status: 'cancelled', statusText: '已取消' },
      'refunding': { status: 'refund', statusText: '退款中' },
      'refunded': { status: 'refund', statusText: '已退款' }
    }
    
    const typeMap = {
      0: 'dinein',
      1: 'takeout',
      2: 'pickup'
    }
    
    const typeTextMap = {
      0: '堂食',
      1: '外卖配送',
      2: '自提'
    }
    
    const orderType = typeMap[order.orderType] || 'dinein'
    const statusInfo = statusMap[order.status] || statusMap[order.kitchenStatus] || { status: 'pending', statusText: '待处理' }
    
    return {
      id: order.orderId,
      orderNumber: order.orderNumber,
      type: orderType,
      typeText: typeTextMap[order.orderType] + (orderType === 'dinein' && order.tableNumber ? ' · 桌号' + order.tableNumber : ''),
      status: statusInfo.status,
      statusText: statusInfo.statusText,
      pickupNumber: order.pickupNumber || '',
      pickupCode: order.pickupCode || '',
      storeName: '一碗好饭（天河店）',
      tableNo: order.tableNumber || '',
      createTime: order.createTime || '',
      items: (order.items || []).map(item => ({
        id: item.id || '',
        name: item.name,
        price: item.price,
        quantity: item.quantity,
        image: ''
      })),
      totalCount: (order.items || []).reduce((sum, item) => sum + item.quantity, 0),
      total: order.totalAmount || 0,
      remark: order.remarks || ''
    }
  },

  loadOrderDetail(orderId) {
    let order = app.getOrder(orderId)
    
    if (!order) {
      wx.showModal({
        title: '订单不存在',
        content: '未找到该订单，请返回订单列表查看',
        showCancel: false,
        success: () => {
          wx.navigateBack({
            fail: () => {
              wx.reLaunch({
                url: '/pages/index/index'
              })
            }
          })
        }
      })
      return
    }
    
    this.setData({ order })
    
    if (order.status === 'pending') {
      this.startCountdown()
    }
  },

  async refreshOrderStatus() {
    if (!this.data.canRefresh || this.data.refreshing) {
      return
    }
    
    if (!this.orderNumber) {
      wx.showToast({
        title: '无法刷新',
        icon: 'none'
      })
      return
    }
    
    this.startCooldown()
    this.setData({ refreshing: true })
    
    try {
      const res = await app.request({
        url: '/pos/order/' + this.orderNumber,
        method: 'GET'
      })
      
      if (res && res.code === 0 && res.data) {
        const order = this.transformOrder(res.data)
        
        app.saveOrder(order)
        
        this.setData({ 
          order, 
          refreshing: false
        })
        
        if (order.status === 'ready') {
          wx.showToast({
            title: '餐品已准备好！',
            icon: 'success'
          })
        } else if (order.status === 'completed') {
          wx.showToast({
            title: '订单已完成',
            icon: 'success'
          })
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
        title: '刷新失败',
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

  startCountdown() {
    const expireMinutes = 15
    const createTime = this.parseDate(this.data.order.createTime)
    if (!createTime) {
      console.warn('无法解析创建时间:', this.data.order.createTime)
      return
    }
    const expireTime = createTime + expireMinutes * 60 * 1000
    
    const updateCountdown = () => {
      const now = Date.now()
      let remaining = Math.max(0, Math.floor((expireTime - now) / 1000))
      
      if (remaining <= 0) {
        this.clearCountdownTimer()
        this.setData({
          countdown: '00:00',
          countdownSeconds: 0,
          'order.status': 'cancelled',
          'order.statusText': '已取消',
          showExpiredTip: true
        })
        return
      }
      
      const minutes = Math.floor(remaining / 60)
      const seconds = remaining % 60
      const countdown = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
      
      this.setData({
        countdown,
        countdownSeconds: remaining
      })
    }
    
    updateCountdown()
    this.countdownTimer = setInterval(updateCountdown, 1000)
  },

  parseDate(dateStr) {
    if (!dateStr) return 0
    if (dateStr instanceof Date) return dateStr.getTime()
    const normalized = String(dateStr).replace(/-/g, '/').replace('T', ' ').split('.')[0]
    const date = new Date(normalized)
    if (isNaN(date.getTime())) {
      console.warn('日期解析失败:', dateStr)
      return 0
    }
    return date.getTime()
  },

  goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.reLaunch({
        url: '/pages/index/index'
      })
    }
  },

  callRider() {
    wx.makePhoneCall({
      phoneNumber: '13800138000',
      fail: () => {
        wx.showToast({
          title: '拨号失败',
          icon: 'none'
        })
      }
    })
  },

  confirmReceive() {
    wx.showModal({
      title: '确认收货',
      content: '确认已收到商品吗？',
      success: (res) => {
        if (res.confirm) {
          const order = { ...this.data.order, status: 'completed', statusText: '已完成' }
          app.saveOrder(order)
          this.setData({ order })
          wx.showToast({
            title: '已确认收货',
            icon: 'success'
          })
        }
      }
    })
  },

  preventTouchMove() {},

  contactStore() {
    this.setData({ showContact: true })
  },

  closeContact() {
    this.setData({ showContact: false })
  },

  callStore() {
    wx.makePhoneCall({
      phoneNumber: '020-12345678'
    })
  },

  copyOrderNumber() {
    wx.setClipboardData({
      data: this.data.order.orderNumber || this.data.order.id,
      success: () => {
        wx.showToast({
          title: '已复制订单号',
          icon: 'success'
        })
      }
    })
  },

  goToPay() {
    wx.redirectTo({
      url: `/pages/order-pay/order-pay?id=${this.data.order.orderNumber || this.data.order.id}`
    })
  },

  goToHome() {
    wx.reLaunch({
      url: '/pages/index/index'
    })
  },

  goToOrderList() {
    wx.redirectTo({
      url: '/pages/order-list/order-list'
    })
  }
})
