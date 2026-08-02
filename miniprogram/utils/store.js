const store = {
  state: {
    user: null,
    isLoggedIn: false,
    openid: null,
    cart: {
      dinein: [],
      pickup: [],
      takeout: []
    },
    currentOrderType: 'dinein',
    orders: {},
    addresses: [],
    selectedAddress: null,
    recentOrderIds: [],
    currentStore: null
  },

  listeners: [],

  subscribe(listener) {
    this.listeners.push(listener)
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener)
    }
  },

  notify() {
    this.listeners.forEach(listener => listener(this.state))
  },

  setState(updates) {
    this.state = { ...this.state, ...updates }
    this.notify()
    this.persist()
  },

  persist() {
    try {
      if (this.state.user) {
        wx.setStorageSync('userInfo', this.state.user)
      }
      wx.setStorageSync('isLoggedIn', this.state.isLoggedIn)
      if (this.state.openid) {
        wx.setStorageSync('openid', this.state.openid)
      }
      wx.setStorageSync('orders', this.state.orders)
      wx.setStorageSync('addresses', this.state.addresses)
      wx.setStorageSync('recentOrderIds', this.state.recentOrderIds)
      if (this.state.currentStore) {
        wx.setStorageSync('currentStore', this.state.currentStore)
      }
      if (this.state.selectedAddress) {
        wx.setStorageSync('selectedAddress', this.state.selectedAddress)
      }
    } catch (e) {
      console.error('状态持久化失败:', e)
    }
  },

  load() {
    try {
      const user = wx.getStorageSync('userInfo')
      const isLoggedIn = wx.getStorageSync('isLoggedIn')
      const openid = wx.getStorageSync('openid')
      const orders = wx.getStorageSync('orders')
      const addresses = wx.getStorageSync('addresses')
      const recentOrderIds = wx.getStorageSync('recentOrderIds')
      const currentStore = wx.getStorageSync('currentStore')
      const selectedAddress = wx.getStorageSync('selectedAddress')

      if (user) this.state.user = user
      if (isLoggedIn !== undefined) this.state.isLoggedIn = isLoggedIn
      if (openid) this.state.openid = openid
      if (orders) this.state.orders = orders
      if (addresses) this.state.addresses = addresses
      if (recentOrderIds) this.state.recentOrderIds = recentOrderIds
      if (currentStore) this.state.currentStore = currentStore
      if (selectedAddress) this.state.selectedAddress = selectedAddress
    } catch (e) {
      console.error('状态加载失败:', e)
    }
  },

  clear() {
    this.state = {
      user: null,
      isLoggedIn: false,
      openid: null,
      cart: {
        dinein: [],
        pickup: [],
        takeout: []
      },
      currentOrderType: 'dinein',
      orders: {},
      addresses: [],
      selectedAddress: null,
      recentOrderIds: []
    }
    wx.removeStorageSync('userInfo')
    wx.removeStorageSync('isLoggedIn')
    wx.removeStorageSync('openid')
    wx.removeStorageSync('orders')
    wx.removeStorageSync('addresses')
    wx.removeStorageSync('recentOrderIds')
    this.notify()
  },

  setUser(user) {
    this.setState({ user, isLoggedIn: true })
  },

  logout() {
    this.state.user = null
    this.state.isLoggedIn = false
    this.state.addresses = []
    this.state.selectedAddress = null
    this.state.currentStore = null
    this.state.orders = {}
    this.state.recentOrderIds = []
    this.state.openid = null
    
    wx.removeStorageSync('userInfo')
    wx.removeStorageSync('isLoggedIn')
    wx.removeStorageSync('addresses')
    wx.removeStorageSync('selectedAddress')
    wx.removeStorageSync('currentStore')
    wx.removeStorageSync('orders')
    wx.removeStorageSync('recentOrderIds')
    wx.removeStorageSync('openid')
    
    this.notify()
  },

  setOpenid(openid) {
    this.state.openid = openid
    wx.setStorageSync('openid', openid)
  },

  setOrderType(type) {
    this.state.currentOrderType = type
  },

  getCart(type) {
    const orderType = type || this.state.currentOrderType
    return this.state.cart[orderType] || []
  },

  addToCart(item, type) {
    const orderType = type || this.state.currentOrderType
    const cart = this.state.cart[orderType] || []
    const existIndex = cart.findIndex(i => i.id === item.id)
    if (existIndex > -1) {
      cart[existIndex].quantity += 1
    } else {
      cart.push({ ...item, quantity: 1 })
    }
    this.state.cart[orderType] = cart
    this.notify()
    return cart
  },

  removeFromCart(itemId, type) {
    const orderType = type || this.state.currentOrderType
    const cart = this.state.cart[orderType] || []
    const existIndex = cart.findIndex(i => i.id === itemId)
    if (existIndex > -1) {
      if (cart[existIndex].quantity > 1) {
        cart[existIndex].quantity -= 1
      } else {
        cart.splice(existIndex, 1)
      }
    }
    this.state.cart[orderType] = cart
    this.notify()
    return cart
  },

  clearCart(type) {
    const orderType = type || this.state.currentOrderType
    this.state.cart[orderType] = []
    this.notify()
  },

  getCartTotal(type) {
    const cart = this.getCart(type)
    return cart.reduce((total, item) => total + item.price * item.quantity, 0)
  },

  getCartCount(type) {
    const cart = this.getCart(type)
    return cart.reduce((count, item) => count + item.quantity, 0)
  },

  saveOrder(order) {
    this.state.orders[order.id] = order
    if (!this.state.recentOrderIds.includes(order.id)) {
      this.state.recentOrderIds.unshift(order.id)
      if (this.state.recentOrderIds.length > 20) {
        this.state.recentOrderIds = this.state.recentOrderIds.slice(0, 20)
      }
    }
    this.persist()
  },

  getOrder(orderId) {
    return this.state.orders[orderId]
  },

  getRecentOrders() {
    return this.state.recentOrderIds
      .map(id => this.state.orders[id])
      .filter(order => order)
  },

  setAddresses(addresses) {
    this.state.addresses = addresses
    this.persist()
  },

  setCurrentStore(storeData) {
    this.state.currentStore = storeData
    this.persist()
    this.notify()
  },

  getCurrentStore() {
    return this.state.currentStore
  },

  setSelectedAddress(address) {
    this.state.selectedAddress = address
    this.persist()
    this.notify()
  },

  getSelectedAddress() {
    return this.state.selectedAddress
  }
}

module.exports = store
