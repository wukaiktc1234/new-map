const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    isLoggedIn: false,
    userInfo: null,
    defaultUserInfo: {
      name: '未登录',
      phone: '',
      avatar: '',
      vipLevel: ''
    },
    stats: {
      totalOrders: 0,
      pending: 0,
      processing: 0,
      coupons: 0
    },
    orderBadge: {
      pending: 0,
      processing: 0
    },
    showModal: false,
    modalTitle: '',
    modalContent: '',
    modalType: '',
    showLoginModal: false
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight
    })
    this.checkLoginStatus()
  },

  onShow() {
    this.checkLoginStatus()
    this.calculateOrderStats()
  },

  checkLoginStatus() {
    const isLoggedIn = app.globalData.isLoggedIn || false
    const userInfo = app.globalData.userInfo || null
    
    if (isLoggedIn && userInfo) {
      this.setData({
        isLoggedIn: true,
        userInfo: userInfo,
        stats: {
          totalOrders: userInfo.totalOrders || 0,
          pending: userInfo.pending || 0,
          processing: userInfo.processing || 0,
          coupons: userInfo.coupons || 0
        }
      })
      this.calculateOrderStats()
    } else {
      this.setData({
        isLoggedIn: false,
        userInfo: this.data.defaultUserInfo,
        stats: {
          totalOrders: 0,
          pending: 0,
          processing: 0,
          coupons: 0
        },
        orderBadge: {
          pending: 0,
          processing: 0
        }
      })
    }
  },

  calculateOrderStats() {
    const orders = Object.values(app.globalData.orders || {})
    let pending = 0
    let processing = 0
    let totalOrders = orders.length
    
    orders.forEach(order => {
      if (order.status === 'pending') {
        pending++
      } else if (order.status === 'processing' || order.status === 'ready') {
        processing++
      }
    })
    
    this.setData({
      'orderBadge.totalOrders': totalOrders,
      'orderBadge.pending': pending,
      'orderBadge.processing': processing,
      'stats.totalOrders': totalOrders,
      'stats.pending': pending,
      'stats.processing': processing
    })
  },

  goToLogin() {
    this.setData({ showLoginModal: true })
  },

  onLoginSuccess() {
    this.setData({ showLoginModal: false })
    this.checkLoginStatus()
  },

  onCloseLoginModal() {
    this.setData({ showLoginModal: false })
  },

  goToOrders(e) {
    if (!this.data.isLoggedIn) {
      this.goToLogin()
      return
    }
    const type = e.currentTarget.dataset.type
    wx.navigateTo({
      url: '/pages/order-list/order-list?type=' + type
    })
  },

  goToCoupons() {
    if (!this.data.isLoggedIn) {
      this.goToLogin()
      return
    }
    wx.navigateTo({
      url: '/pages/coupons/coupons'
    })
  },

  goToAddress() {
    if (!this.data.isLoggedIn) {
      this.goToLogin()
      return
    }
    wx.navigateTo({
      url: '/pages/address/address'
    })
  },

  goToMember() {
    if (!this.data.isLoggedIn) {
      this.goToLogin()
      return
    }
    wx.navigateTo({
      url: '/pages/member/member'
    })
  },

  goToPoints() {
    if (!this.data.isLoggedIn) {
      this.goToLogin()
      return
    }
    wx.navigateTo({
      url: '/pages/points-mall/points-mall'
    })
  },

  showFeedbackModal() {
    this.setData({
      showModal: true,
      modalTitle: '意见反馈',
      modalContent: '如有问题或建议，请联系客服：\n电话：400-888-8888\n邮箱：feedback@ywhf.com',
      modalType: 'feedback'
    })
  },

  goToSettings() {
    wx.navigateTo({
      url: '/pages/settings/settings'
    })
  },

  goToProfileEdit() {
    if (!this.data.isLoggedIn) {
      this.showLoginModal()
      return
    }
    wx.navigateTo({
      url: '/pages/profile-edit/profile-edit'
    })
  },

  goToBindPhone() {
    wx.navigateTo({
      url: '/pages/bind-phone/bind-phone'
    })
  },

  confirmModal() {
    this.setData({
      showModal: true,
      modalTitle: '退出登录',
      modalContent: '确定要退出登录吗？',
      modalType: 'logout'
    })
  },

  hideModal() {
    this.setData({ showModal: false })
  },

  confirmModal() {
    const type = this.data.modalType
    if (type === 'logout') {
      app.clearUserInfo()
      this.checkLoginStatus()
      wx.showToast({
        title: '已退出登录',
        icon: 'success'
      })
    }
    this.setData({ showModal: false })
  },

  preventTouchMove() {}
})
