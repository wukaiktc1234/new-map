const http = require('./utils/http.js')
const store = require('./utils/store.js')

const isDev = __wxConfig.envVersion === 'develop' || typeof __wxConfig.envVersion === 'undefined'

const API_BASE_URL = isDev 
  ? 'http://localhost:8081/api/v1' 
  : 'https://api.yiwanhaofan.com/api/v1'

App({
  globalData: {
    userInfo: null,
    openid: null,
    cart: {
      dinein: [],
      pickup: [],
      takeout: []
    },
    currentOrderType: 'dinein',
    currentOrder: null,
    orders: {},
    addresses: null,
    selectedAddress: null,
    currentStore: null,
    apiBase: API_BASE_URL,
    loading: false,
    isDev: isDev
  },

  onLaunch() {
    store.load()
    this.syncStoreToGlobalData()
    this.getOpenId()
  },

  syncStoreToGlobalData() {
    if (store.state.user) {
      this.globalData.userInfo = store.state.user
      this.globalData.isLoggedIn = store.state.isLoggedIn
    }
    if (store.state.openid) {
      this.globalData.openid = store.state.openid
    }
    if (store.state.orders) {
      this.globalData.orders = store.state.orders
    }
    if (store.state.addresses && store.state.addresses.length > 0) {
      this.globalData.addresses = store.state.addresses
    }
    if (store.state.selectedAddress) {
      this.globalData.selectedAddress = store.state.selectedAddress
    }
    if (store.state.currentStore) {
      this.globalData.currentStore = store.state.currentStore
    }
    this.globalData.cart = store.state.cart
  },

  parseDate(dateStr) {
    if (!dateStr) return null
    if (dateStr instanceof Date) return dateStr
    
    const normalized = dateStr.replace(/-/g, '/').replace('T', ' ')
    const date = new Date(normalized)
    if (isNaN(date.getTime())) {
      console.warn('日期解析失败:', dateStr)
      return null
    }
    return date
  },

  formatDate(date, format = 'YYYY-MM-DD HH:mm') {
    if (!date) return ''
    const d = date instanceof Date ? date : this.parseDate(date)
    if (!d) return ''
    
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hour = String(d.getHours()).padStart(2, '0')
    const minute = String(d.getMinutes()).padStart(2, '0')
    const second = String(d.getSeconds()).padStart(2, '0')
    
    return format
      .replace('YYYY', year)
      .replace('MM', month)
      .replace('DD', day)
      .replace('HH', hour)
      .replace('mm', minute)
      .replace('ss', second)
  },

  checkLoginStatus() {
    const userInfo = wx.getStorageSync('userInfo')
    const isLoggedIn = wx.getStorageSync('isLoggedIn')
    if (userInfo && isLoggedIn) {
      this.globalData.userInfo = userInfo
      this.globalData.isLoggedIn = true
    }
  },

  loadOrders() {
    const orders = wx.getStorageSync('orders')
    if (orders) {
      this.globalData.orders = orders
    }
  },

  loadAddresses() {
    const addresses = wx.getStorageSync('addresses')
    if (addresses && addresses.length > 0) {
      this.globalData.addresses = addresses
    }
  },

  async getOpenId() {
    try {
      const storedOpenid = wx.getStorageSync('openid')
      if (storedOpenid) {
        this.globalData.openid = storedOpenid
        return storedOpenid
      }

      const loginRes = await wx.login()
      if (loginRes.code) {
        const res = await this.request({
          url: '/mp/getOpenId',
          method: 'POST',
          data: { code: loginRes.code },
          showLoading: false
        })
        
        if (res.code === 200 && res.data) {
          this.globalData.openid = res.data.openid
          wx.setStorageSync('openid', res.data.openid)
          return res.data.openid
        }
      }
    } catch (e) {
      console.error('获取openid失败', e)
      const tempOpenid = 'temp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
      this.globalData.openid = tempOpenid
      wx.setStorageSync('openid', tempOpenid)
      return tempOpenid
    }
    return null
  },

  request(options) {
    return http.request(options)
  },

  showError(message) {
    http.showError(message)
  },

  showSuccess(message) {
    http.showSuccess(message)
  },

  showLoading(title = '加载中...') {
    this.globalData.loading = true
    http.showLoading(title)
  },

  hideLoading() {
    this.globalData.loading = false
    http.hideLoading()
  },

  saveOrder(order) {
    store.saveOrder(order)
    this.globalData.orders = store.state.orders
  },

  getOrder(orderId) {
    return store.getOrder(orderId)
  },

  setUserInfo(userInfo, token) {
    store.setUser(userInfo)
    this.globalData.isLoggedIn = true
    this.globalData.userInfo = userInfo
    if (token) {
      wx.setStorageSync('token', token)
    }
  },

  clearUserInfo() {
    store.logout()
    this.globalData.isLoggedIn = false
    this.globalData.userInfo = null
    this.globalData.addresses = null
    this.globalData.selectedAddress = null
    this.globalData.currentStore = null
    this.globalData.orders = {}
    this.globalData.openid = null
  },

  setCurrentOrderType(type) {
    store.setOrderType(type)
    this.globalData.currentOrderType = type
  },

  getCurrentOrderType() {
    return store.state.currentOrderType
  },

  addToCart(item, orderType) {
    store.addToCart(item, orderType)
    const type = orderType || this.globalData.currentOrderType
    this.globalData.cart[type] = store.getCart(type)
    return this.globalData.cart[type]
  },

  removeFromCart(itemId, orderType) {
    store.removeFromCart(itemId, orderType)
    const type = orderType || this.globalData.currentOrderType
    this.globalData.cart[type] = store.getCart(type)
    return this.globalData.cart[type]
  },

  getCart(orderType) {
    const type = orderType || this.globalData.currentOrderType
    return store.getCart(type)
  },

  getCartTotal(orderType) {
    return store.getCartTotal(orderType)
  },

  getCartCount(orderType) {
    return store.getCartCount(orderType)
  },

  clearCart(orderType) {
    store.clearCart(orderType)
    const type = orderType || this.globalData.currentOrderType
    this.globalData.cart[type] = []
  }
})
