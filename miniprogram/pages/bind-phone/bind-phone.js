Page({
  data: {
    statusBarHeight: 0,
    phone: '',
    code: '',
    countdown: 0
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight
    })
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  onCodeInput(e) {
    this.setData({ code: e.detail.value })
  },

  sendCode() {
    if (this.data.countdown > 0) return
    
    if (!this.data.phone || this.data.phone.length !== 11) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }
    
    this.setData({ countdown: 60 })
    
    const timer = setInterval(() => {
      if (this.data.countdown <= 1) {
        clearInterval(timer)
        this.setData({ countdown: 0 })
      } else {
        this.setData({ countdown: this.data.countdown - 1 })
      }
    }, 1000)
    
    wx.showToast({
      title: '验证码已发送',
      icon: 'success'
    })
  },

  bindPhone() {
    if (!this.data.phone || this.data.phone.length !== 11) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }
    
    if (!this.data.code || this.data.code.length !== 6) {
      wx.showToast({
        title: '请输入6位验证码',
        icon: 'none'
      })
      return
    }
    
    wx.showToast({
      title: '绑定成功',
      icon: 'success'
    })
    
    setTimeout(() => {
      wx.navigateBack()
    }, 1500)
  },

  goBack() {
    wx.navigateBack()
  }
})
