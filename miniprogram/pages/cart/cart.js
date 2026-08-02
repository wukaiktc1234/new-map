const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    headerHeight: 0,
    orderType: 'dinein',
    cartItems: [],
    recommendItems: [
      { id: 'r1', name: '酸梅汤', price: 6, image: '', desc: '清凉解腻' },
      { id: 'r2', name: '炸鸡翅', price: 12, image: '', desc: '外酥里嫩' },
      { id: 'r3', name: '蛋挞', price: 8, image: '', desc: '香甜可口' }
    ],
    subtotal: 0,
    packFee: 0,
    deliveryFee: 0,
    discount: 0,
    total: 0,
    totalCount: 0
  },

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      headerHeight: systemInfo.statusBarHeight * 2 + 72,
      orderType: options.type || 'dinein'
    })
  },

  onShow() {
    this.loadCartItems()
  },

  loadCartItems() {
    const cart = app.getCart()
    const subtotal = app.getCartTotal()
    const totalCount = app.getCartCount()
    
    let packFee = 0
    let deliveryFee = 0
    
    if (this.data.orderType === 'dinein') {
      packFee = totalCount > 0 ? 2 : 0
    } else if (this.data.orderType === 'takeout') {
      deliveryFee = 5
    }
    
    const total = subtotal + packFee + deliveryFee
    
    this.setData({
      cartItems: cart,
      subtotal: subtotal.toFixed(2),
      packFee: packFee.toFixed(2),
      deliveryFee: deliveryFee.toFixed(2),
      total: total.toFixed(2),
      totalCount
    })
  },

  increaseQuantity(e) {
    const id = e.currentTarget.dataset.id
    const item = this.data.cartItems.find(i => i.id === id)
    if (item) {
      app.addToCart(item)
      this.loadCartItems()
    }
  },

  decreaseQuantity(e) {
    const id = e.currentTarget.dataset.id
    app.removeFromCart(id)
    this.loadCartItems()
  },

  clearCart() {
    wx.showModal({
      title: '提示',
      content: '确定要清空购物车吗？',
      success: (res) => {
        if (res.confirm) {
          app.clearCart()
          this.loadCartItems()
        }
      }
    })
  },

  addToCart(e) {
    const item = e.currentTarget.dataset.item
    app.addToCart(item)
    this.loadCartItems()
    wx.showToast({
      title: '已添加',
      icon: 'success'
    })
  },

  goBack() {
    wx.navigateBack()
  },

  goToMenu() {
    wx.navigateBack()
  },

  goToConfirm() {
    if (this.data.cartItems.length === 0) {
      wx.showToast({
        title: '购物车为空',
        icon: 'none'
      })
      return
    }
    
    wx.navigateTo({
      url: '/pages/order-confirm/order-confirm?type=' + this.data.orderType
    })
  }
})
