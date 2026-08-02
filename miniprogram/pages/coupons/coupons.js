const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    headerHeight: 0,
    isLoggedIn: false,
    showLoginModal: false,
    currentTab: 'available',
    availableCount: 0,
    coupons: []
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      headerHeight: systemInfo.statusBarHeight * 2 + 128
    })
    this.checkLoginStatus()
  },

  onShow() {
    this.checkLoginStatus()
  },

  checkLoginStatus() {
    const userInfo = app.globalData.userInfo
    const isLoggedIn = !!userInfo
    
    if (!isLoggedIn) {
      this.setData({ showLoginModal: true })
      return
    }
    
    this.setData({ isLoggedIn: true })
    this.loadCoupons(this.data.currentTab)
  },

  onLoginSuccess() {
    this.setData({ showLoginModal: false })
    this.checkLoginStatus()
  },

  onCloseLoginModal() {
    this.setData({ showLoginModal: false })
    wx.navigateBack()
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ currentTab: tab })
    this.loadCoupons(tab)
  },

  checkExpiring(validUntil) {
    const today = new Date()
    const expireDate = new Date(validUntil)
    const diffDays = Math.ceil((expireDate - today) / (1000 * 60 * 60 * 24))
    return diffDays <= 7 && diffDays > 0
  },

  loadCoupons(tab) {
    let coupons = []
    
    if (tab === 'available') {
      coupons = [
        { id: 'c1', name: '新人专享券', value: 20, minSpend: 30, validUntil: '2024-06-30', type: 'newuser', expiring: false },
        { id: 'c2', name: '会员日优惠券', value: 5, minSpend: 20, validUntil: '2024-04-30', type: 'member', expiring: true },
        { id: 'c3', name: '满减优惠券', value: 15, minSpend: 50, validUntil: '2024-05-15', type: 'normal', expiring: false },
        { id: 'c4', name: '下午茶专享券', value: 8, minSpend: 25, validUntil: '2024-05-20', type: 'normal', expiring: false },
        { id: 'c5', name: '周末特惠券', value: 10, minSpend: 35, validUntil: '2024-05-31', type: 'normal', expiring: false },
        { id: 'c6', name: '午餐专享券', value: 6, minSpend: 20, validUntil: '2024-06-15', type: 'normal', expiring: false },
        { id: 'c7', name: '晚餐优惠券', value: 12, minSpend: 40, validUntil: '2024-06-20', type: 'normal', expiring: false },
        { id: 'c8', name: '外卖专享券', value: 8, minSpend: 30, validUntil: '2024-06-25', type: 'normal', expiring: false }
      ]
      
      coupons = coupons.map(c => ({
        ...c,
        expiring: this.checkExpiring(c.validUntil)
      }))
      
      const availableCount = coupons.length
      this.setData({ availableCount })
    } else if (tab === 'used') {
      coupons = [
        { id: 'c9', name: '春节优惠券', value: 8, minSpend: 25, validUntil: '2024-02-15', type: 'normal' },
        { id: 'c10', name: '元旦优惠券', value: 10, minSpend: 30, validUntil: '2024-01-01', type: 'normal' },
        { id: 'c11', name: '圣诞优惠券', value: 15, minSpend: 45, validUntil: '2023-12-25', type: 'normal' },
        { id: 'c12', name: '双十一优惠券', value: 20, minSpend: 60, validUntil: '2023-11-11', type: 'normal' }
      ]
    } else {
      coupons = [
        { id: 'c13', name: '中秋优惠券', value: 15, minSpend: 45, validUntil: '2023-09-29', type: 'normal' },
        { id: 'c14', name: '国庆优惠券', value: 18, minSpend: 50, validUntil: '2023-10-07', type: 'normal' },
        { id: 'c15', name: '教师节优惠券', value: 10, minSpend: 30, validUntil: '2023-09-10', type: 'normal' }
      ]
    }
    
    this.setData({ coupons })
  },

  useCoupon(e) {
    const id = e.currentTarget.dataset.id
    wx.reLaunch({
      url: '/pages/index/index'
    })
  },

  goBack() {
    wx.navigateBack()
  }
})
