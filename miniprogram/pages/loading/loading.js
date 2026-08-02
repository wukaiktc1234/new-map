Page({
  data: {},

  onLoad() {
    setTimeout(() => {
      wx.redirectTo({
        url: '/pages/index/index'
      })
    }, 2000)
  }
})
