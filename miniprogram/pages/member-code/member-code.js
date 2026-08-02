const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    isLoggedIn: false,
    showLoginModal: false,
    balance: 0,
    memberCode: '',
    showRecharge: false,
    rechargeAmount: 50,
    rechargeOptions: [30, 50, 100, 200]
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
  },

  checkLoginStatus() {
    const userInfo = app.globalData.userInfo
    const isLoggedIn = !!userInfo
    
    if (!isLoggedIn) {
      this.setData({ showLoginModal: true })
      return
    }
    
    this.setData({
      isLoggedIn: true,
      balance: userInfo.balance || 0,
      memberCode: userInfo.memberCode || 'MB' + Date.now().toString().slice(-8)
    })
  },

  onLoginSuccess() {
    this.setData({ showLoginModal: false })
    this.checkLoginStatus()
  },

  onCloseLoginModal() {
    this.setData({ showLoginModal: false })
    wx.navigateBack()
  },

  refreshCode() {
    const newCode = 'MB' + Date.now().toString().slice(-8)
    this.setData({ memberCode: newCode })
    wx.showToast({
      title: '已刷新',
      icon: 'success'
    })
  },

  showRechargeModal() {
    this.setData({ showRecharge: true })
  },

  hideRechargeModal() {
    this.setData({ showRecharge: false })
  },

  selectRechargeAmount(e) {
    const amount = e.currentTarget.dataset.amount
    this.setData({ rechargeAmount: parseInt(amount) })
  },

  confirmRecharge() {
    const amount = this.data.rechargeAmount
    this.hideRechargeModal()
    wx.navigateTo({
      url: `/pages/recharge/recharge?amount=${amount}`
    })
  },

  goToRecharge() {
    wx.navigateTo({
      url: '/pages/recharge/recharge'
    })
  },

  goBack() {
    wx.navigateBack()
  }
})
