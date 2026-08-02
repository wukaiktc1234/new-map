const app = getApp()
const store = require('../../utils/store.js')

Component({
  properties: {
    show: {
      type: Boolean,
      value: false
    },
    orderType: {
      type: String,
      value: 'pickup'
    }
  },

  data: {
    activeTab: 'nearby',
    stores: [],
    currentStore: null,
    loading: false,
    locating: false,
    locationText: '',
    userLocation: null
  },

  lifetimes: {
    attached() {
      this.initData()
    }
  },

  observers: {
    'show': function(show) {
      if (show) {
        this.initData()
        this.autoLocate()
      }
    }
  },

  methods: {
    initData() {
      const currentStore = store.getCurrentStore()
      this.setData({ 
        currentStore,
        activeTab: 'nearby'
      })
      this.loadStores()
    },

    autoLocate() {
      if (!this.data.userLocation) {
        this.refreshLocation()
      }
    },

    preventTouchMove() {},

    onClose() {
      this.triggerEvent('close')
    },

    switchTab(e) {
      const tab = e.currentTarget.dataset.tab
      this.setData({ activeTab: tab })
      this.loadStores()
    },

    async refreshLocation() {
      if (this.data.locating) return
      
      this.setData({ locating: true })
      
      try {
        const res = await wx.getLocation({
          type: 'gcj02'
        })
        
        const userLocation = {
          latitude: res.latitude,
          longitude: res.longitude
        }
        
        this.setData({ 
          userLocation,
          locationText: '定位成功'
        })
        
        await this.reverseGeocode(userLocation)
        this.loadStores()
        
      } catch (err) {
        console.error('定位失败:', err)
        
        if (err.errMsg && err.errMsg.includes('auth deny')) {
          wx.showModal({
            title: '定位权限未开启',
            content: '请在设置中开启定位权限，以便为您推荐附近门店',
            confirmText: '去设置',
            success: (res) => {
              if (res.confirm) {
                wx.openSetting()
              }
            }
          })
        } else {
          wx.showToast({
            title: '定位失败，请检查权限设置',
            icon: 'none'
          })
        }
        
        this.setData({ 
          locationText: '定位失败',
          locating: false
        })
      }
    },

    async reverseGeocode(location) {
      try {
        const res = await wx.request({
          url: 'https://apis.map.qq.com/ws/geocoder/v1/',
          data: {
            location: `${location.latitude},${location.longitude}`,
            key: 'YOUR_TENCENT_MAP_KEY',
            get_poi: 0
          }
        })
        
        if (res.data && res.data.status === 0) {
          const address = res.data.result.address_component
          const locationText = address.street || address.district || '定位成功'
          this.setData({ locationText })
        }
      } catch (err) {
        console.error('逆地址解析失败:', err)
      }
      
      this.setData({ locating: false })
    },

    async loadStores() {
      this.setData({ loading: true })
      
      try {
        await new Promise(resolve => setTimeout(resolve, 500))
        
        let stores = this.getAllStores()
        
        if (this.data.userLocation) {
          stores = this.calculateDistances(stores, this.data.userLocation)
          stores.sort((a, b) => a.distance - b.distance)
        }
        
        if (this.data.activeTab === 'nearby') {
          stores = stores.slice(0, 5)
        }
        
        const currentStore = store.getCurrentStore()
        if (currentStore) {
          stores = stores.map(s => ({
            ...s,
            isCurrent: s.id === currentStore.id
          }))
        }
        
        this.setData({ stores, loading: false })
        
      } catch (err) {
        console.error('加载门店失败:', err)
        this.setData({ loading: false })
      }
    },

    getAllStores() {
      return [
        {
          id: 's1',
          name: '一碗好饭（天河店）',
          address: '广州市天河区天河路385号太古汇B1层',
          distance: 844,
          distanceText: '844m',
          businessHours: '10:00-22:00',
          status: '营业中',
          latitude: 23.1325,
          longitude: 113.3255
        },
        {
          id: 's2',
          name: '一碗好饭（珠江新城店）',
          address: '广州市天河区珠江新城花城大道85号',
          distance: 1200,
          distanceText: '1.2km',
          businessHours: '10:00-22:00',
          status: '营业中',
          latitude: 23.1256,
          longitude: 113.3206
        },
        {
          id: 's3',
          name: '一碗好饭（北京路店）',
          address: '广州市越秀区北京路168号',
          distance: 3500,
          distanceText: '3.5km',
          businessHours: '09:00-23:00',
          status: '营业中',
          latitude: 23.1252,
          longitude: 113.2635
        },
        {
          id: 's4',
          name: '一碗好饭（番禺万达店）',
          address: '广州市番禺区汉溪大道东381号万达广场',
          distance: 5800,
          distanceText: '5.8km',
          businessHours: '10:00-22:00',
          status: '休息中',
          latitude: 23.0523,
          longitude: 113.3421
        },
        {
          id: 's5',
          name: '一碗好饭（白云万达店）',
          address: '广州市白云区云城东路509号万达广场',
          distance: 7200,
          distanceText: '7.2km',
          businessHours: '10:00-22:00',
          status: '营业中',
          latitude: 23.1785,
          longitude: 113.2532
        },
        {
          id: 's6',
          name: '一碗好饭（海珠万达店）',
          address: '广州市海珠区广州大道南合生广场',
          distance: 4500,
          distanceText: '4.5km',
          businessHours: '10:00-22:00',
          status: '营业中',
          latitude: 23.0987,
          longitude: 113.3125
        }
      ]
    },

    calculateDistances(stores, location) {
      return stores.map(s => {
        const distance = this.getDistance(
          location.latitude,
          location.longitude,
          s.latitude,
          s.longitude
        )
        return {
          ...s,
          distance,
          distanceText: this.formatDistance(distance)
        }
      })
    },

    getDistance(lat1, lng1, lat2, lng2) {
      const rad = Math.PI / 180
      const earthRadius = 6371000
      
      const dLat = (lat2 - lat1) * rad
      const dLng = (lng2 - lng1) * rad
      
      const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1 * rad) * Math.cos(lat2 * rad) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
      
      const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
      
      return Math.round(earthRadius * c)
    },

    formatDistance(meters) {
      if (meters < 1000) {
        return meters + 'm'
      }
      return (meters / 1000).toFixed(1) + 'km'
    },

    selectStore(e) {
      const storeData = e.currentTarget.dataset.store
      
      if (storeData.status === '休息中') {
        wx.showToast({
          title: '该门店已休息',
          icon: 'none'
        })
        return
      }
      
      const stores = this.data.stores.map(s => ({
        ...s,
        isCurrent: s.id === storeData.id
      }))
      
      this.setData({ 
        currentStore: storeData,
        stores
      })
    },

    confirmSelect() {
      if (!this.data.currentStore) {
        wx.showToast({
          title: '请选择门店',
          icon: 'none'
        })
        return
      }
      
      store.setCurrentStore(this.data.currentStore)
      app.globalData.currentStore = this.data.currentStore
      
      this.triggerEvent('confirm', { 
        store: this.data.currentStore,
        orderType: this.data.orderType
      })
      
      this.onClose()
    }
  }
})
