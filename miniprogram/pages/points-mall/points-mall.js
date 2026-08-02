const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    isLoggedIn: false,
    showLoginModal: false,
    points: 0,
    goods: []
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
      points: userInfo.points || 0
    })
    this.loadGoods()
  },

  onLoginSuccess() {
    this.setData({ showLoginModal: false })
    this.checkLoginStatus()
  },

  onCloseLoginModal() {
    this.setData({ showLoginModal: false })
    wx.navigateBack()
  },

  loadGoods() {
    const goods = [
      { id: 'g1', name: '满50减10优惠券', points: 500 },
      { id: 'g2', name: '满100减25优惠券', points: 1000 },
      { id: 'g3', name: '满200减50优惠券', points: 1800 },
      { id: 'g4', name: '品牌定制水杯', points: 2000 },
      { id: 'g5', name: '品牌帆布袋', points: 1500 },
      { id: 'g6', name: '免费饮品券', points: 800 },
      { id: 'g7', name: '新品试吃券', points: 600 },
      { id: 'g8', name: '生日专属蛋糕券', points: 2500 },
      { id: 'g9', name: '双人套餐优惠券', points: 1500 },
      { id: 'g10', name: '品牌T恤', points: 3000 }
    ]
    
    this.setData({ goods })
  },

  viewHistory() {
    wx.navigateTo({
      url: '/pages/exchange-history/exchange-history'
    })
  },

  exchangeGoods(e) {
    const item = e.currentTarget.dataset.item
    
    if (this.data.points < item.points) {
      wx.showToast({
        title: '积分不足',
        icon: 'none'
      })
      return
    }
    
    wx.showModal({
      title: '确认兑换',
      content: `确定使用${item.points}积分兑换${item.name}吗？`,
      success: (res) => {
        if (res.confirm) {
          const newPoints = this.data.points - item.points
          this.setData({ points: newPoints })
          
          wx.showToast({
            title: '兑换成功',
            icon: 'success'
          })
        }
      }
    })
  },

  goBack() {
    wx.navigateBack()
  }
})
