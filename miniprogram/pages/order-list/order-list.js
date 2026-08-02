const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    headerHeight: 0,
    currentTab: 'all',
    tabs: [
      { label: '全部', value: 'all' },
      { label: '待支付', value: 'pending' },
      { label: '进行中', value: 'processing' },
      { label: '已完成', value: 'completed' },
      { label: '退款/售后', value: 'refund' }
    ],
    orders: [],
    showPickupModal: false,
    currentOrder: {},
    loading: false
  },

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync()
    const type = options.type || 'all'
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      headerHeight: systemInfo.statusBarHeight * 2 + 128,
      currentTab: type
    })
    this.loadLocalOrders(type)
  },

  onShow() {
    this.loadLocalOrders(this.data.currentTab)
    this.loadOrders(this.data.currentTab)
  },

  loadLocalOrders(tab) {
    const localOrders = Object.values(app.globalData.orders || {})
    let orders = [...localOrders]
    
    if (tab !== 'all') {
      orders = orders.filter(order => order.status === tab)
    }
    
    orders.sort((a, b) => this.parseDate(b.createTime) - this.parseDate(a.createTime))
    this.setData({ orders })
  },

  switchTab(e) {
    const value = e.currentTarget.dataset.value
    this.setData({ currentTab: value })
    this.loadOrders(value)
  },

  async loadOrders(tab) {
    this.setData({ loading: true })
    
    try {
      const openid = app.globalData.openid || await app.getOpenId()
      
      const res = await app.request({
        url: '/pos/orders/byOpenid',
        method: 'GET',
        data: { openid: openid }
      })
      
      let orders = []
      
      if (res.code === 0 && res.data) {
        orders = res.data.map(order => this.transformOrder(order))
      }
      
      const localOrders = Object.values(app.globalData.orders || {})
      localOrders.forEach(localOrder => {
        if (!orders.find(o => o.orderNumber === localOrder.orderNumber)) {
          orders.push(localOrder)
        }
      })
      
      orders.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
      
      if (tab !== 'all') {
        orders = orders.filter(order => order.status === tab)
      }
      
      this.setData({ orders, loading: false })
    } catch (e) {
      console.error('加载订单失败', e)
      
      let orders = Object.values(app.globalData.orders || {})
      
      if (tab !== 'all') {
        orders = orders.filter(order => order.status === tab)
      }
      
      orders.sort((a, b) => this.parseDate(b.createTime) - this.parseDate(a.createTime))
      
      this.setData({ orders, loading: false })
    }
  },

  parseDate(dateStr) {
    if (!dateStr) return 0
    if (dateStr instanceof Date) return dateStr.getTime()
    const normalized = dateStr.replace(/-/g, '/').replace('T', ' ')
    return new Date(normalized).getTime() || 0
  },

  transformOrder(order) {
    const statusMap = {
      'pending': { status: 'pending', statusText: '待支付' },
      'paid': { status: 'processing', statusText: '已支付' },
      'making': { status: 'processing', statusText: '制作中' },
      'ready': { status: 'ready', statusText: '待取餐' },
      'delivering': { status: 'processing', statusText: '配送中' },
      'completed': { status: 'completed', statusText: '已完成' },
      'cancelled': { status: 'cancelled', statusText: '已取消' },
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
    
    let createTime = order.createTime || ''
    if (createTime) {
      createTime = this.formatTime(createTime)
    }
    
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
      createTime: createTime,
      items: (order.items || []).map(item => ({
        id: item.id || '',
        name: item.name,
        price: item.price,
        quantity: item.quantity,
        image: ''
      })),
      totalCount: (order.items || []).reduce((sum, item) => sum + item.quantity, 0),
      total: order.totalAmount ? order.totalAmount.toFixed(2) : '0.00'
    }
  },

  formatTime(timeStr) {
    if (!timeStr) return ''
    try {
      const date = new Date(timeStr)
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      const hours = date.getHours().toString().padStart(2, '0')
      const minutes = date.getMinutes().toString().padStart(2, '0')
      return `${month}-${day} ${hours}:${minutes}`
    } catch (e) {
      return timeStr
    }
  },

  goToOrderDetail(e) {
    const id = e.currentTarget.dataset.id
    const order = this.data.orders.find(o => o.id === id)
    if (order && order.orderNumber) {
      wx.navigateTo({
        url: '/pages/order-detail/order-detail?orderNumber=' + order.orderNumber
      })
    } else {
      wx.navigateTo({
        url: '/pages/order-detail/order-detail?id=' + id
      })
    }
  },

  showPickupCode(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showPickupModal: true,
      currentOrder: item
    })
  },

  hidePickupCode() {
    this.setData({ showPickupModal: false })
  },

  preventTouchMove() {},

  payOrder(e) {
    const id = e.currentTarget.dataset.id
    const order = this.data.orders.find(o => o.id === id)
    if (order) {
      const orderData = {
        items: order.items,
        totalCount: order.totalCount,
        subtotal: order.subtotal || order.total,
        packFee: order.packFee || 0,
        deliveryFee: order.deliveryFee || 0,
        orderType: order.type,
        tableNo: order.tableNo || '',
        remark: order.remark || '',
        address: order.address || null,
        total: order.total
      }
      wx.navigateTo({
        url: '/pages/order-confirm/order-confirm?type=' + order.type + '&from=orderList&orderId=' + id + '&orderData=' + encodeURIComponent(JSON.stringify(orderData))
      })
    }
  },

  reorder(e) {
    const id = e.currentTarget.dataset.id
    const order = this.data.orders.find(o => o.id === id)
    if (order) {
      app.clearCart(order.type)
      order.items.forEach(item => {
        for (let i = 0; i < item.quantity; i++) {
          app.addToCart({
            id: item.id,
            name: item.name,
            price: item.price,
            image: item.image
          }, order.type)
        }
      })
      wx.navigateTo({
        url: '/pages/order-confirm/order-confirm?type=' + order.type
      })
    }
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

  goToMenu() {
    wx.switchTab({
      url: '/pages/index/index'
    })
  }
})
