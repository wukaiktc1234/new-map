const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    headerHeight: 0,
    orderNo: '',
    total: '0.00',
    countdown: '15:00',
    countdownSeconds: 900,
    paymentMethod: 'wechat',
    isLoggedIn: false,
    userBalance: '0.00',
    showSuccess: false,
    showFail: false,
    failReason: '网络超时',
    payTime: '',
    createTime: '',
    orderData: null,
    orderInfo: null
  },

  countdownTimer: null,

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync()
    
    let orderData = null
    if (options.data) {
      try {
        orderData = JSON.parse(decodeURIComponent(options.data))
      } catch (e) {
        console.error('解析订单数据失败', e)
      }
    }
    
    const now = new Date()
    const createTime = now.getFullYear() + '-' + 
      (now.getMonth() + 1).toString().padStart(2, '0') + '-' + 
      now.getDate().toString().padStart(2, '0') + ' ' +
      now.getHours().toString().padStart(2, '0') + ':' +
      now.getMinutes().toString().padStart(2, '0')
    
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      headerHeight: systemInfo.statusBarHeight * 2 + 72,
      orderNo: options.id || '',
      total: options.total || '0.00',
      orderData: orderData,
      createTime: createTime
    })
    
    this.checkLoginStatus()
    
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    if (currentPage) {
      wx.setNavigationBarColor({
        frontColor: '#000000',
        backgroundColor: '#FFFFFF'
      })
    }
    
    this.startCountdown()
  },

  onShow() {
    this.checkLoginStatus()
  },

  checkLoginStatus() {
    const isLoggedIn = app.globalData.isLoggedIn || false
    const userInfo = app.globalData.userInfo || {}
    
    this.setData({
      isLoggedIn,
      userBalance: isLoggedIn ? (userInfo.balance || '0.00') : '0.00'
    })
  },

  onUnload() {
    this.clearCountdownTimer()
  },

  clearCountdownTimer() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },

  startCountdown() {
    this.clearCountdownTimer()
    
    let remaining = 900
    
    const updateCountdown = () => {
      if (remaining <= 0) {
        this.clearCountdownTimer()
        this.setData({
          countdown: '00:00',
          countdownSeconds: 0,
          showFail: true,
          failReason: '支付超时'
        })
        return
      }
      
      remaining--
      const minutes = Math.floor(remaining / 60)
      const seconds = remaining % 60
      const countdown = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
      
      this.setData({
        countdown,
        countdownSeconds: remaining
      })
    }
    
    updateCountdown()
    this.countdownTimer = setInterval(updateCountdown, 1000)
  },

  selectPayment(e) {
    const method = e.currentTarget.dataset.method
    
    if (method === 'balance' && !this.data.isLoggedIn) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }
    
    this.setData({ paymentMethod: method })
  },

  cancelOrder() {
    wx.showModal({
      title: '取消订单',
      content: '确定要取消此订单吗？',
      success: (res) => {
        if (res.confirm) {
          this.clearCountdownTimer()
          wx.showToast({
            title: '订单已取消',
            icon: 'success'
          })
          
          const fromOrderList = this.data.orderData?.fromOrderList
          
          setTimeout(() => {
            if (fromOrderList) {
              wx.navigateBack({
                fail: () => {
                  wx.reLaunch({ url: '/pages/order-list/order-list' })
                }
              })
            } else {
              const orderType = this.data.orderData?.orderType || 'dinein'
              let targetRoute = 'menu-dinein'
              
              if (orderType === 'pickup') {
                targetRoute = 'menu-pickup'
              } else if (orderType === 'takeout') {
                targetRoute = 'menu-takeout'
              }
              
              const pages = getCurrentPages()
              const targetIndex = pages.findIndex(p => p.route && p.route.includes(targetRoute))
              
              if (targetIndex >= 0) {
                const delta = pages.length - 1 - targetIndex
                if (delta > 0) {
                  wx.navigateBack({ delta })
                  return
                }
              }
              
              let targetUrl = '/pages/menu-dinein/menu-dinein'
              if (orderType === 'pickup') {
                targetUrl = '/pages/menu-pickup/menu-pickup'
              } else if (orderType === 'takeout') {
                targetUrl = '/pages/menu-takeout/menu-takeout'
              }
              wx.reLaunch({ url: targetUrl })
            }
          }, 1000)
        }
      }
    })
  },

  async confirmPay() {
    if (this.data.paymentMethod === 'balance' && !this.data.isLoggedIn) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }
    
    if (this.data.paymentMethod === 'balance') {
      const balance = parseFloat(this.data.userBalance) || 0
      const total = parseFloat(this.data.total) || 0
      if (balance <= 0) {
        wx.showToast({
          title: '余额为0，请选择其他支付方式',
          icon: 'none',
          duration: 2000
        })
        return
      }
      if (balance < total) {
        wx.showModal({
          title: '余额不足',
          content: `当前余额: ${balance.toFixed(2)}元，订单金额: ${total.toFixed(2)}元，请选择其他支付方式或前往充值`,
          confirmText: '去充值',
          cancelText: '换方式',
          success: (res) => {
            if (res.confirm) {
              wx.navigateTo({ url: '/pages/recharge/recharge' })
            }
          }
        })
        return
      }
    }
    
    wx.showLoading({ title: '支付中...' })
    
    try {
      const openid = app.globalData.openid || await app.getOpenId()
      const orderData = this.data.orderData || {}
      const totalAmount = parseFloat(this.data.total) || 0
      
      const request = {
        orderId: this.data.orderNo,
        paymentMethod: this.data.paymentMethod === 'wechat' ? '微信支付' : 
                       this.data.paymentMethod === 'balance' ? '余额支付' : '现金',
        openid: openid,
        amount: totalAmount
      }
      
      console.log('发送支付请求:', request)
      const res = await app.request({
        url: '/pos/order/pay',
        method: 'POST',
        data: request
      })
      console.log('支付响应:', res)
      
      await new Promise(resolve => setTimeout(resolve, 800))
      
      wx.hideLoading()
      
      if (res.code === 0 && res.data) {
        this.clearCountdownTimer()
        
        const now = new Date()
        const payTime = now.getFullYear() + '-' + 
          (now.getMonth() + 1).toString().padStart(2, '0') + '-' + 
          now.getDate().toString().padStart(2, '0') + ' ' +
          now.getHours().toString().padStart(2, '0') + ':' +
          now.getMinutes().toString().padStart(2, '0') + ':' +
          now.getSeconds().toString().padStart(2, '0')
        
        const orderInfo = {
          id: this.data.orderNo,
          orderNumber: orderData.orderNumber || this.data.orderNo,
          type: orderData.orderType,
          typeText: this.getTypeText(orderData.orderType, orderData.tableNo),
          status: 'processing',
          statusText: '制作中',
          step: 2,
          statusTip: '您的餐品正在制作中，请耐心等待',
          pickupNumber: res.data.pickupNumber,
          pickupCode: res.data.pickupCode,
          storeName: '一碗好饭（天河店）',
          tableNo: orderData.tableNo || '',
          createTime: payTime,
          items: orderData.items,
          totalCount: orderData.totalCount || 0,
          subtotal: orderData.subtotal || 0,
          packFee: orderData.packFee || 0,
          deliveryFee: orderData.deliveryFee || 0,
          total: parseFloat(this.data.total),
          remark: orderData.remark || ''
        }
        
        app.saveOrder(orderInfo)
        
        if (this.data.paymentMethod === 'balance' && app.globalData.userInfo) {
          const newBalance = parseFloat(app.globalData.userInfo.balance || 0) - totalAmount
          app.globalData.userInfo.balance = newBalance.toFixed(2)
          wx.setStorageSync('userInfo', app.globalData.userInfo)
          this.setData({ userBalance: newBalance.toFixed(2) })
        }
        
        this.setData({
          showSuccess: true,
          payTime,
          orderInfo: orderInfo
        })
      } else {
        throw new Error(res.message || '支付失败')
      }
    } catch (e) {
      wx.hideLoading()
      console.error('支付失败', e)
      
      this.setData({
        showFail: true,
        failReason: e.message || '网络超时，请检查网络后重试'
      })
    }
  },

  formatTime(date) {
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hour = date.getHours().toString().padStart(2, '0')
    const minute = date.getMinutes().toString().padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}`
  },

  getTypeText(orderType, tableNumber) {
    const typeMap = {
      'dinein': '堂食' + (tableNumber ? ' · 桌号' + tableNumber : ''),
      'pickup': '自提',
      'takeout': '外卖配送'
    }
    return typeMap[orderType] || '堂食'
  },

  preventTouchMove() {},

  goToHome() {
    wx.reLaunch({
      url: '/pages/index/index'
    })
  },

  goToOrderDetail() {
    const orderInfo = this.data.orderInfo
    let url = '/pages/order-detail/order-detail?'
    if (orderInfo && orderInfo.orderNumber) {
      url += 'orderNumber=' + orderInfo.orderNumber
    } else {
      url += 'id=' + this.data.orderNo
    }
    wx.redirectTo({ url })
  },

  goToOrderList() {
    this.setData({ showFail: false })
    wx.reLaunch({
      url: '/pages/order-list/order-list'
    })
  },

  retryPay() {
    this.setData({ 
      showFail: false,
      failReason: ''
    })
    this.startCountdown()
  },

  goBack() {
    const fromOrderList = this.data.orderData?.fromOrderList
    
    wx.showModal({
      title: '提示',
      content: fromOrderList ? '确定要返回吗？' : '返回将取消当前订单，确定返回吗？',
      success: (res) => {
        if (res.confirm) {
          this.clearCountdownTimer()
          
          if (fromOrderList) {
            wx.navigateBack({
              fail: () => {
                wx.reLaunch({ url: '/pages/order-list/order-list' })
              }
            })
          } else {
            const orderType = this.data.orderData?.orderType || 'dinein'
            let targetUrl = '/pages/menu-dinein/menu-dinein'
            
            if (orderType === 'pickup') {
              targetUrl = '/pages/menu-pickup/menu-pickup'
            } else if (orderType === 'takeout') {
              targetUrl = '/pages/menu-takeout/menu-takeout'
            }
            
            wx.redirectTo({
              url: targetUrl,
              fail: () => {
                wx.reLaunch({ url: '/pages/index/index' })
              }
            })
          }
        }
      }
    })
  }
})
