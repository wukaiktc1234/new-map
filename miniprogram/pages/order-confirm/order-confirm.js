const app = getApp()
const store = require('../../utils/store.js')

Page({
  data: {
    statusBarHeight: 0,
    headerHeight: 0,
    orderType: 'dinein',
    tableNo: 'A01',
    pickupTime: '尽快取餐（约15分钟）',
    storeName: '',
    address: null,
    items: [],
    cartItems: [],
    totalCount: 0,
    subtotal: 0,
    packFee: 0,
    deliveryFee: 0,
    discount: 0,
    total: 0,
    remark: '',
    tempRemark: '',
    showRemark: false,
    loading: false,
    fromOrderList: false,
    orderId: null,
    recommendItems: [
      { id: 'r1', name: '酸梅汤', price: 6, image: '/images/default-dish.png' },
      { id: 'r2', name: '柠檬茶', price: 8, image: '/images/default-dish.png' },
      { id: 'r3', name: '炸鸡翅', price: 12, image: '/images/default-dish.png' },
      { id: 'r4', name: '薯条', price: 10, image: '/images/default-dish.png' },
      { id: 'r5', name: '蛋挞', price: 8, image: '/images/default-dish.png' }
    ]
  },

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync()
    const orderType = options.type || 'dinein'
    const fromOrderList = options.from === 'orderList'
    
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      headerHeight: systemInfo.statusBarHeight * 2 + 72,
      orderType: orderType,
      tableNo: options.tableNo || 'A01',
      pickupTime: decodeURIComponent(options.pickupTime || '尽快取餐（约15分钟）'),
      fromOrderList: fromOrderList,
      orderId: options.orderId || null
    })
    
    this.loadStoreInfo()
    
    if (fromOrderList && options.orderData) {
      try {
        const orderData = JSON.parse(decodeURIComponent(options.orderData))
        this.setData({
          items: orderData.items || [],
          cartItems: orderData.items || [],
          totalCount: orderData.totalCount || 0,
          subtotal: (orderData.subtotal || 0).toFixed(2),
          packFee: (orderData.packFee || 0).toFixed(2),
          deliveryFee: (orderData.deliveryFee || 0).toFixed(2),
          total: (orderData.total || orderData.subtotal || 0).toFixed(2),
          remark: orderData.remark || '',
          address: orderData.address || null
        })
      } catch (e) {
        console.error('解析订单数据失败', e)
        this.loadOrderData()
      }
    } else {
      this.loadOrderData()
    }
    
    if (orderType === 'takeout') {
      this.loadSelectedAddress()
    }
  },

  onShow() {
    this.loadStoreInfo()
    if (this.data.orderType === 'takeout') {
      this.loadSelectedAddress()
    }
  },

  loadStoreInfo() {
    const currentStore = store.getCurrentStore()
    if (currentStore) {
      this.setData({ storeName: currentStore.name })
    } else {
      this.setData({ storeName: '一碗好饭（天河店）' })
    }
  },

  selectStore() {
    wx.navigateTo({
      url: '/pages/store-select/store-select?select=true'
    })
  },

  loadSelectedAddress() {
    const isLoggedIn = store.state.isLoggedIn || app.globalData.isLoggedIn
    
    if (!isLoggedIn) {
      this.setData({ address: null })
      return
    }
    
    const selectedAddress = store.getSelectedAddress() || app.globalData.selectedAddress
    if (selectedAddress) {
      this.setData({ address: selectedAddress })
      return
    }
    
    let addresses = store.state.addresses || app.globalData.addresses
    if (!addresses || addresses.length === 0) {
      this.setData({ address: null })
      return
    }
    
    const defaultAddress = addresses.find(a => a.isDefault) || addresses[0]
    this.setData({ address: defaultAddress })
  },

  loadOrderData() {
    const orderType = this.data.orderType
    const cart = app.getCart(orderType)
    const subtotal = app.getCartTotal(orderType)
    const totalCount = app.getCartCount(orderType)
    
    let packFee = 0
    let deliveryFee = 0
    
    if (this.data.orderType === 'dinein') {
      packFee = totalCount > 0 ? 2 : 0
    } else if (this.data.orderType === 'takeout') {
      deliveryFee = 5
    }
    
    const total = subtotal + packFee + deliveryFee
    
    this.setData({
      items: cart,
      cartItems: cart,
      subtotal: subtotal.toFixed(2),
      packFee: packFee.toFixed(2),
      deliveryFee: deliveryFee.toFixed(2),
      total: total.toFixed(2),
      totalCount
    })
  },

  showRemarkModal() {
    this.setData({
      showRemark: true,
      tempRemark: this.data.remark
    })
  },

  hideRemarkModal() {
    this.setData({ showRemark: false })
  },

  onRemarkInput(e) {
    this.setData({ tempRemark: e.detail.value })
  },

  confirmRemark() {
    this.setData({
      remark: this.data.tempRemark,
      showRemark: false
    })
  },

  preventTouchMove() {},

  selectAddress() {
    wx.navigateTo({
      url: '/pages/address/address?select=true'
    })
  },

  submitOrder() {
    if (this.data.cartItems.length === 0) {
      wx.showToast({
        title: '购物车为空',
        icon: 'none'
      })
      return
    }
    
    if (this.data.orderType === 'takeout' && !this.data.address) {
      wx.showToast({
        title: '请选择收货地址',
        icon: 'none'
      })
      return
    }
    
    if (this.data.fromOrderList) {
      const orderData = {
        items: this.data.cartItems,
        totalCount: this.data.totalCount,
        subtotal: this.data.subtotal,
        packFee: this.data.packFee,
        deliveryFee: this.data.deliveryFee,
        orderType: this.data.orderType,
        tableNo: this.data.tableNo,
        remark: this.data.remark,
        address: this.data.address,
        fromOrderList: true,
        orderId: this.data.orderId
      }
      
      wx.navigateTo({
        url: '/pages/order-pay/order-pay?id=' + this.data.orderId + '&total=' + this.data.total + '&data=' + encodeURIComponent(JSON.stringify(orderData))
      })
    } else {
      this.createNewOrder()
    }
  },

  async createNewOrder() {
    wx.showLoading({ title: '提交中...' })
    
    try {
      const openid = app.globalData.openid || await app.getOpenId()
      
      const request = {
        items: this.data.cartItems.map(item => ({
          productId: item.id,
          productName: item.name,
          quantity: item.quantity,
          price: item.price
        })),
        orderType: this.data.orderType === 'dinein' ? 0 : this.data.orderType === 'takeout' ? 1 : 2,
        tableNumber: this.data.tableNo ? parseInt(this.data.tableNo.replace(/[^0-9]/g, '')) : null,
        totalAmount: parseFloat(this.data.total),
        paymentMethod: '待支付',
        openid: openid,
        remark: this.data.remark || null,
        contactName: this.data.address?.name || null,
        contactPhone: this.data.address?.phone || null,
        deliveryAddress: this.data.address ? 
          this.data.address.province + this.data.address.city + this.data.address.district + this.data.address.detail : null
      }
      
      const res = await app.request({
        url: '/pos/order',
        method: 'POST',
        data: request
      })
      
      wx.hideLoading()
      
      if (res.code === 0 && res.data) {
        app.clearCart(this.data.orderType)
        
        const orderData = {
          items: this.data.cartItems,
          totalCount: this.data.totalCount,
          subtotal: this.data.subtotal,
          packFee: this.data.packFee,
          deliveryFee: this.data.deliveryFee,
          orderType: this.data.orderType,
          tableNo: this.data.tableNo,
          remark: this.data.remark,
          address: this.data.address,
          fromOrderList: false,
          serverOrderId: res.data.orderId,
          orderNumber: res.data.orderNumber
        }
        
        const localOrder = {
          id: res.data.orderId,
          orderNumber: res.data.orderNumber,
          type: this.data.orderType,
          typeText: this.getTypeText(this.data.orderType, this.data.tableNo),
          status: 'pending',
          statusText: '待支付',
          step: 1,
          statusTip: '订单待支付，请尽快完成支付',
          storeName: '一碗好饭（天河店）',
          tableNo: this.data.tableNo || '',
          createTime: this.formatTime(new Date()),
          items: this.data.cartItems,
          totalCount: this.data.totalCount,
          subtotal: parseFloat(this.data.subtotal) || 0,
          packFee: parseFloat(this.data.packFee) || 0,
          deliveryFee: parseFloat(this.data.deliveryFee) || 0,
          total: parseFloat(this.data.total),
          remark: this.data.remark || ''
        }
        
        app.saveOrder(localOrder)
        
        wx.navigateTo({
          url: '/pages/order-pay/order-pay?id=' + res.data.orderId + '&total=' + this.data.total + '&data=' + encodeURIComponent(JSON.stringify(orderData))
        })
      } else {
        throw new Error(res.message || '订单创建失败')
      }
    } catch (e) {
      wx.hideLoading()
      console.error('创建订单失败', e)
      wx.showModal({
        title: '提交失败',
        content: e.message || '网络异常，请检查网络后重试',
        confirmText: '重试',
        success: (res) => {
          if (res.confirm) {
            this.createNewOrder()
          }
        }
      })
    }
  },

  formatTime(date) {
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hour = date.getHours().toString().padStart(2, '0')
    const minute = date.getMinutes().toString().padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}`
  },

  getTypeText(orderType, tableNo) {
    const typeMap = {
      'dinein': '堂食' + (tableNo ? ' · 桌号' + tableNo : ''),
      'pickup': '自提',
      'takeout': '外卖配送'
    }
    return typeMap[orderType] || '堂食'
  },

  addToCart(e) {
    const item = e.currentTarget.dataset.item
    const newItem = {
      ...item,
      quantity: 1
    }
    
    const items = [...this.data.items, newItem]
    const cartItems = [...this.data.cartItems, newItem]
    
    const totalCount = items.reduce((sum, i) => sum + i.quantity, 0)
    const subtotal = items.reduce((sum, i) => sum + i.price * i.quantity, 0)
    const packFee = this.data.orderType !== 'dinein' ? Math.ceil(totalCount * 0.5) : 0
    const deliveryFee = this.data.orderType === 'takeout' ? 5 : 0
    const total = subtotal + packFee + deliveryFee
    
    this.setData({
      items,
      cartItems,
      totalCount,
      subtotal,
      packFee,
      deliveryFee,
      total
    })
    
    wx.showToast({
      title: '已添加',
      icon: 'success',
      duration: 1000
    })
  },

  goBack() {
    if (this.data.loading) return
    
    if (this.data.fromOrderList) {
      wx.navigateBack({
        fail: () => {
          wx.reLaunch({ url: '/pages/order-list/order-list' })
        }
      })
      return
    }
    
    const pages = getCurrentPages()
    const orderType = this.data.orderType
    
    let targetRoute = 'menu-dinein'
    if (orderType === 'pickup') {
      targetRoute = 'menu-pickup'
    } else if (orderType === 'takeout') {
      targetRoute = 'menu-takeout'
    }
    
    const targetIndex = pages.findIndex(p => p.route && p.route.includes(targetRoute))
    
    if (targetIndex >= 0) {
      const delta = pages.length - 1 - targetIndex
      if (delta > 0) {
        wx.navigateBack({ delta })
        return
      }
    }
    
    this.setData({ loading: true })
    
    let targetUrl = '/pages/menu-dinein/menu-dinein'
    if (orderType === 'pickup') {
      targetUrl = '/pages/menu-pickup/menu-pickup'
    } else if (orderType === 'takeout') {
      targetUrl = '/pages/menu-takeout/menu-takeout'
    }
    
    wx.reLaunch({ 
      url: targetUrl,
      fail: () => {
        this.setData({ loading: false })
        wx.reLaunch({ url: '/pages/index/index' })
      }
    })
  }
})
