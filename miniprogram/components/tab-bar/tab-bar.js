Component({
  properties: {
    active: {
      type: Number,
      value: 0
    }
  },

  methods: {
    switchTab(e) {
      const { index, url } = e.currentTarget.dataset
      if (this.properties.active === index) return
      
      wx.redirectTo({
        url: url,
        fail: () => {
          wx.reLaunch({ url: url })
        }
      })
    }
  }
})
