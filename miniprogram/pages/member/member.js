const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    isLoggedIn: false,
    userInfo: null,
    showLoginModal: false,
    vipInfo: {
      level: 'gold',
      levelName: '黄金会员',
      progress: 65,
      needAmount: 350,
      nextLevelName: '铂金会员'
    },
    tasks: [
      { id: 1, name: '完成一笔订单', reward: 50, icon: 'order', btnText: '去完成' },
      { id: 2, name: '分享给好友', reward: 30, icon: 'share', btnText: '去分享' },
      { id: 3, name: '每日签到', reward: 10, icon: 'signin', btnText: '去签到' }
    ]
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
      userInfo: userInfo
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

  goToPrivilege(e) {
    const type = e.currentTarget.dataset.type
    const privilegeNames = {
      discount: '专属折扣',
      birthday: '生日礼遇',
      points: '积分加倍',
      priority: '优先排队'
    }
    wx.showModal({
      title: privilegeNames[type] || '特权详情',
      content: type === 'discount' ? '享受全场菜品9折优惠' :
               type === 'birthday' ? '生日当天获赠免费套餐一份' :
               type === 'points' ? '消费积分双倍返还' :
               '高峰期优先排队等候',
      showCancel: false,
      confirmText: '我知道了'
    })
  },

  doTask(e) {
    const id = e.currentTarget.dataset.id
    const task = this.data.tasks.find(t => t.id === id)
    if (!task) return
    
    if (task.btnText === '已完成') {
      wx.showToast({
        title: '任务已完成',
        icon: 'none'
      })
      return
    }
    
    if (task.name.includes('签到')) {
      const tasks = this.data.tasks.map(t => 
        t.id === id ? { ...t, btnText: '已完成' } : t
      )
      this.setData({ tasks })
      wx.showToast({
        title: `+${task.reward}积分`,
        icon: 'success'
      })
    } else if (task.name.includes('订单')) {
      wx.switchTab({
        url: '/pages/index/index'
      })
    } else if (task.name.includes('分享')) {
      wx.showShareMenu({
        withShareTicket: true,
        menus: ['shareAppMessage', 'shareTimeline']
      })
    }
  },

  goBack() {
    wx.navigateBack()
  }
})
