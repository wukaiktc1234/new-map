const app = getApp()

Page({
  data: {
    isLoggedIn: false,
    userInfo: null,
    notificationEnabled: true,
    soundEnabled: true,
    cacheSize: '0KB'
  },

  onLoad() {
    this.checkLoginStatus()
    this.loadSettings()
    this.calculateCacheSize()
  },

  onShow() {
    this.checkLoginStatus()
  },

  checkLoginStatus() {
    const isLoggedIn = app.globalData.isLoggedIn || false
    const userInfo = app.globalData.userInfo || null
    this.setData({
      isLoggedIn,
      userInfo
    })
  },

  loadSettings() {
    const settings = wx.getStorageSync('appSettings') || {}
    this.setData({
      notificationEnabled: settings.notificationEnabled !== false,
      soundEnabled: settings.soundEnabled !== false
    })
  },

  saveSettings() {
    wx.setStorageSync('appSettings', {
      notificationEnabled: this.data.notificationEnabled,
      soundEnabled: this.data.soundEnabled
    })
  },

  calculateCacheSize() {
    try {
      const res = wx.getStorageInfoSync()
      const sizeKB = res.currentSize
      let sizeText = ''
      if (sizeKB < 1024) {
        sizeText = sizeKB + 'KB'
      } else {
        sizeText = (sizeKB / 1024).toFixed(2) + 'MB'
      }
      this.setData({ cacheSize: sizeText })
    } catch (e) {
      this.setData({ cacheSize: '0KB' })
    }
  },

  toggleNotification(e) {
    this.setData({
      notificationEnabled: e.detail.value
    })
    this.saveSettings()
    wx.showToast({
      title: e.detail.value ? '已开启通知' : '已关闭通知',
      icon: 'none'
    })
  },

  toggleSound(e) {
    this.setData({
      soundEnabled: e.detail.value
    })
    this.saveSettings()
    wx.showToast({
      title: e.detail.value ? '已开启声音' : '已关闭声音',
      icon: 'none'
    })
  },

  goToAccount() {
    if (!this.data.isLoggedIn) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }
    wx.navigateTo({
      url: '/pages/profile-edit/profile-edit'
    })
  },

  goToPassword() {
    if (!this.data.isLoggedIn) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }
    wx.showModal({
      title: '修改密码',
      editable: true,
      placeholderText: '请输入新密码',
      success: (res) => {
        if (res.confirm && res.content) {
          wx.showToast({
            title: '密码修改成功',
            icon: 'success'
          })
        }
      }
    })
  },

  goToAgreement() {
    wx.navigateTo({
      url: '/pages/agreement/agreement'
    })
  },

  clearCache() {
    wx.showModal({
      title: '清除缓存',
      content: '确定要清除所有缓存数据吗？这不会影响您的账号信息。',
      success: (res) => {
        if (res.confirm) {
          try {
            const keepKeys = ['userInfo', 'isLoggedIn', 'openid', 'token', 'appSettings']
            const allKeys = wx.getStorageInfoSync().keys
            
            allKeys.forEach(key => {
              if (!keepKeys.includes(key)) {
                wx.removeStorageSync(key)
              }
            })
            
            this.calculateCacheSize()
            
            wx.showToast({
              title: '缓存已清除',
              icon: 'success'
            })
          } catch (e) {
            wx.showToast({
              title: '清除失败',
              icon: 'none'
            })
          }
        }
      }
    })
  },

  goToAbout() {
    wx.showModal({
      title: '关于一碗好饭',
      content: '一碗好饭 v1.0.0\n\n致力于为您提供优质的餐饮服务体验。\n\n© 2026 一碗好饭团队',
      showCancel: false,
      confirmText: '我知道了'
    })
  },

  checkUpdate() {
    wx.showLoading({
      title: '检查中...',
    })
    
    setTimeout(() => {
      wx.hideLoading()
      wx.showModal({
        title: '检查更新',
        content: '当前已是最新版本',
        showCancel: false,
        confirmText: '确定'
      })
    }, 1000)
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      success: (res) => {
        if (res.confirm) {
          app.clearUserInfo()
          
          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          })
          
          setTimeout(() => {
            wx.switchTab({
              url: '/pages/index/index'
            })
          }, 1500)
        }
      }
    })
  }
})
