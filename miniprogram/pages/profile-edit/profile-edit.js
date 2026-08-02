const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    genderOptions: [
      { value: 0, label: '未设置' },
      { value: 1, label: '男' },
      { value: 2, label: '女' }
    ],
    userInfo: {
      name: '王木木',
      gender: 0,
      birthday: ''
    }
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight
    })
    
    const userInfo = app.globalData.userInfo
    if (userInfo) {
      this.setData({ userInfo: { ...this.data.userInfo, ...userInfo } })
    }
  },

  onNameInput(e) {
    this.setData({
      'userInfo.name': e.detail.value
    })
  },

  onGenderChange(e) {
    this.setData({
      'userInfo.gender': parseInt(e.detail.value)
    })
  },

  onBirthdayChange(e) {
    this.setData({
      'userInfo.birthday': e.detail.value
    })
  },

  saveProfile() {
    app.setUserInfo(this.data.userInfo)
    wx.showToast({
      title: '保存成功',
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
