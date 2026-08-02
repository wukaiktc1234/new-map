const app = getApp()
const store = require('../../utils/store.js')

Page({
  data: {
    statusBarHeight: 0,
    stores: [
      {
        id: 's1',
        name: '一碗好饭（天河店）',
        address: '广州市天河区天河路385号太古汇B1层',
        distance: 844,
        businessHours: '10:00-22:00',
        status: '营业中',
        isCurrent: true
      },
      {
        id: 's2',
        name: '一碗好饭（珠江新城店）',
        address: '广州市天河区珠江新城花城大道85号',
        distance: 1200,
        businessHours: '10:00-22:00',
        status: '营业中',
        isCurrent: false
      },
      {
        id: 's3',
        name: '一碗好饭（北京路店）',
        address: '广州市越秀区北京路168号',
        distance: 3500,
        businessHours: '09:00-23:00',
        status: '营业中',
        isCurrent: false
      },
      {
        id: 's4',
        name: '一碗好饭（番禺万达店）',
        address: '广州市番禺区汉溪大道东381号万达广场',
        distance: 5800,
        businessHours: '10:00-22:00',
        status: '休息中',
        isCurrent: false
      }
    ],
    selectMode: false
  },

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync()
    const currentStore = store.getCurrentStore()
    
    let stores = this.data.stores
    if (currentStore) {
      stores = stores.map(s => ({
        ...s,
        isCurrent: s.id === currentStore.id
      }))
    }
    
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      selectMode: options.select === 'true',
      stores
    })
  },

  selectStore(e) {
    const id = e.currentTarget.dataset.id
    const storeData = this.data.stores.find(s => s.id === id)
    
    if (storeData.status === '休息中') {
      wx.showToast({
        title: '该门店已休息',
        icon: 'none'
      })
      return
    }

    const stores = this.data.stores.map(s => ({
      ...s,
      isCurrent: s.id === id
    }))
    
    store.setCurrentStore(storeData)
    app.globalData.currentStore = storeData
    this.setData({ stores })
    
    wx.showToast({
      title: '已选择门店',
      icon: 'success'
    })
    
    if (this.data.selectMode) {
      setTimeout(() => {
        wx.navigateBack()
      }, 500)
    }
  },

  goBack() {
    wx.navigateBack()
  }
})
