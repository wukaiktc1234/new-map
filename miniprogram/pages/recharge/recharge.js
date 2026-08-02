const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    isLoggedIn: false,
    showLoginModal: false,
    balance: '0.00',
    selectedAmount: 50,
    isIOS: false,
    rechargeOptions: [],
    benefits: [
      { title: '充值优惠', desc: '充值越多，赠送越多' },
      { title: '会员专享', desc: '充值用户享受会员折扣' },
      { title: '快速支付', desc: '余额支付，无需等待' }
    ],
    showAgreement: false,
    agreedToTerms: false,
    maxSingleAmount: 300,
    maxDailyAmount: 500,
    maxMonthlyAmount: 1000,
    dailyRecharged: 0,
    monthlyRecharged: 0,
    configLoaded: false
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync()
    const isIOS = systemInfo.platform === 'ios'
    
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      isIOS
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
      balance: userInfo.balance ? userInfo.balance.toFixed(2) : '0.00'
    })
    
    this.loadConfig()
    this.loadRechargeLimits()
  },

  onLoginSuccess() {
    this.setData({ showLoginModal: false })
    this.checkLoginStatus()
  },

  onCloseLoginModal() {
    this.setData({ showLoginModal: false })
    wx.navigateBack()
  },

  loadConfig() {
    wx.request({
      url: app.globalData.apiUrl + '/api/recharge/config',
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.success) {
          const config = res.data.data
          this.setData({
            maxSingleAmount: config.maxSingleAmount || 300,
            maxDailyAmount: config.maxDailyAmount || 500,
            maxMonthlyAmount: config.maxMonthlyAmount || 1000,
            rechargeOptions: this.getRechargeOptions(this.data.isIOS, config.rechargeOptions),
            configLoaded: true
          })
        }
      },
      fail: () => {
        this.setData({
          rechargeOptions: this.getRechargeOptions(this.data.isIOS, null),
          configLoaded: true
        })
      }
    })
  },

  loadRechargeLimits() {
    wx.request({
      url: app.globalData.apiUrl + '/api/recharge/limits',
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.success) {
          this.setData({
            dailyRecharged: res.data.data.dailyRecharged || 0,
            monthlyRecharged: res.data.data.monthlyRecharged || 0
          })
        }
      }
    })
  },

  getRechargeOptions(isIOS, serverOptions) {
    const defaultOptions = [
      { amount: 30, bonus: 0, iosAmount: 38 },
      { amount: 50, bonus: 5, iosAmount: 68 },
      { amount: 100, bonus: 12, iosAmount: 128 },
      { amount: 200, bonus: 30, iosAmount: 248 },
      { amount: 300, bonus: 50, iosAmount: 368 }
    ]
    
    const baseOptions = serverOptions || defaultOptions
    
    if (isIOS) {
      return baseOptions.map(item => ({
        ...item,
        displayAmount: item.iosAmount,
        actualAmount: item.amount,
        bonus: item.bonus
      }))
    }
    
    return baseOptions.map(item => ({
      ...item,
      displayAmount: item.amount,
      actualAmount: item.amount
    }))
  },

  selectAmount(e) {
    const amount = e.currentTarget.dataset.amount
    this.setData({ selectedAmount: amount })
  },

  showAgreementModal() {
    this.setData({ showAgreement: true })
  },

  hideAgreementModal() {
    this.setData({ showAgreement: false })
  },

  agreeToTerms() {
    this.setData({ 
      agreedToTerms: true,
      showAgreement: false 
    })
  },

  checkRechargeLimits(displayAmount) {
    const { dailyRecharged, monthlyRecharged, maxDailyAmount, maxMonthlyAmount } = this.data
    
    if (displayAmount > this.data.maxSingleAmount) {
      return { valid: false, message: `单次充值金额不能超过¥${this.data.maxSingleAmount}` }
    }
    
    if (dailyRecharged + displayAmount > maxDailyAmount) {
      const remaining = maxDailyAmount - dailyRecharged
      return { valid: false, message: `今日剩余可充值¥${remaining > 0 ? remaining : 0}，请减少充值金额或明日再试` }
    }
    
    if (monthlyRecharged + displayAmount > maxMonthlyAmount) {
      const remaining = maxMonthlyAmount - monthlyRecharged
      return { valid: false, message: `本月剩余可充值¥${remaining > 0 ? remaining : 0}，请减少充值金额或下月再试` }
    }
    
    return { valid: true }
  },

  doRecharge() {
    if (!this.data.agreedToTerms) {
      wx.showModal({
        title: '温馨提示',
        content: '请先阅读并同意《用户充值协议》',
        confirmText: '去阅读',
        success: (res) => {
          if (res.confirm) {
            this.showAgreementModal()
          }
        }
      })
      return
    }
    
    const option = this.data.rechargeOptions.find(o => o.displayAmount === this.data.selectedAmount)
    if (!option) return
    
    const limitCheck = this.checkRechargeLimits(option.displayAmount)
    if (!limitCheck.valid) {
      wx.showToast({
        title: limitCheck.message,
        icon: 'none',
        duration: 3000
      })
      return
    }
    
    const totalAmount = option.actualAmount + (option.bonus || 0)
    
    let content = ''
    if (this.data.isIOS) {
      content = `由于苹果平台手续费，您支付¥${option.displayAmount}，实际到账¥${totalAmount}（含赠送¥${option.bonus || 0}）。\n\n是否确认充值？`
    } else {
      content = `确认充值¥${option.displayAmount}，到账¥${totalAmount}（含赠送¥${option.bonus || 0}）？`
    }
    
    wx.showModal({
      title: this.data.isIOS ? 'iOS充值说明' : '确认充值',
      content: content,
      success: (res) => {
        if (res.confirm) {
          this.processRecharge(totalAmount, option.displayAmount)
        }
      }
    })
  },

  processRecharge(totalAmount, displayAmount) {
    wx.showLoading({ title: '充值中...' })
    
    wx.request({
      url: app.globalData.apiUrl + '/api/recharge/create',
      method: 'POST',
      data: {
        amount: displayAmount,
        actualAmount: totalAmount
      },
      success: (res) => {
        wx.hideLoading()
        if (res.data && res.data.success) {
          const newBalance = (parseFloat(this.data.balance) + totalAmount).toFixed(2)
          const newDailyRecharged = this.data.dailyRecharged + displayAmount
          const newMonthlyRecharged = this.data.monthlyRecharged + displayAmount
          
          this.setData({ 
            balance: newBalance,
            dailyRecharged: newDailyRecharged,
            monthlyRecharged: newMonthlyRecharged
          })
          
          wx.showToast({
            title: '充值成功',
            icon: 'success'
          })
        } else {
          wx.showToast({
            title: res.data?.message || '充值失败',
            icon: 'none'
          })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        })
      }
    })
  },

  goBack() {
    wx.navigateBack()
  }
})
