const app = getApp()

Component({
  properties: {
    show: {
      type: Boolean,
      value: false
    }
  },

  data: {
    agreed: false
  },

  methods: {
    onMaskTap() {
      this.triggerEvent('close')
    },

    preventTouchMove() {},

    toggleAgree() {
      this.setData({
        agreed: !this.data.agreed
      })
    },

    viewAgreement(e) {
      const type = e.currentTarget.dataset.type
      wx.navigateTo({
        url: '/pages/agreement/agreement?type=' + type
      })
    },

    onGetPhoneNumber(e) {
      if (!this.data.agreed) {
        wx.showToast({
          title: '请先同意用户协议',
          icon: 'none'
        })
        return
      }

      if (e.detail.errMsg !== 'getPhoneNumber:ok') {
        wx.showToast({ 
          title: '授权失败，请重试', 
          icon: 'none'
        })
        return
      }

      this.doLogin(e.detail.code)
    },

    async doLogin(phoneCode) {
      wx.showLoading({ title: '登录中...' })
      
      try {
        const res = await app.request({
          url: '/mp/getOpenId',
          method: 'POST',
          data: { code: phoneCode }
        })
        
        if (res.code === 200) {
          const userInfo = {
            id: res.data.openid,
            name: '微信用户',
            phone: '',
            avatar: '',
            vipLevel: '普通会员',
            balance: 0,
            points: 0
          }
          
          app.setUserInfo(userInfo, res.data.openid)
          
          wx.hideLoading()
          wx.showToast({ title: '登录成功', icon: 'success' })
          
          this.setData({
            agreed: false
          })
          
          this.triggerEvent('success', { userInfo })
        } else {
          throw new Error(res.message || '登录失败')
        }
      } catch (err) {
        wx.hideLoading()
        wx.showToast({
          title: err.message || '登录失败',
          icon: 'none'
        })
      }
    },

    onClose() {
      this.triggerEvent('close')
    }
  }
})
