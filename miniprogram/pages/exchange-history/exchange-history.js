Page({
  data: {
    statusBarHeight: 0,
    records: []
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight
    })
    this.loadRecords()
  },

  loadRecords() {
    const records = [
      {
        id: 'r1',
        name: '满50减10优惠券',
        points: 500,
        status: '已使用',
        date: '2024-01-15 14:30'
      },
      {
        id: 'r2',
        name: '免费饮品券',
        points: 800,
        status: '未使用',
        date: '2024-01-10 10:20'
      },
      {
        id: 'r3',
        name: '新品试吃券',
        points: 600,
        status: '已过期',
        date: '2023-12-25 16:45'
      },
      {
        id: 'r4',
        name: '品牌帆布袋',
        points: 1500,
        status: '已领取',
        date: '2023-12-01 09:15'
      }
    ]
    
    this.setData({ records })
  },

  getStatusClass(status) {
    switch (status) {
      case '未使用':
        return 'status-unused'
      case '已使用':
        return 'status-used'
      case '已过期':
        return 'status-expired'
      case '已领取':
        return 'status-received'
      default:
        return ''
    }
  },

  goBack() {
    wx.navigateBack()
  }
})
