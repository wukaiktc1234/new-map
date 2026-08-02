Component({
  properties: {
    show: {
      type: Boolean,
      value: false
    }
  },

  data: {
    phoneNumber: '',
    code: '',
    countdown: 0,
    sending: false
  },

  methods: {
    onPhoneInput(e) {
      this.setData({
        phoneNumber: e.detail.value
      })
    },

    onCodeInput(e) {
      this.setData({
        code: e.detail.value
      })
    },

    sendCode() {
      const phone = this.data.phoneNumber
      if (!/^1[3-9]\d{9}$/.test(phone)) {
        wx.showToast({
          title: '请输入正确的手机号',
          icon: 'none'
        })
        return
      }

      if (this.data.countdown > 0) return

      this.setData({ sending: true })

      // 模拟发送验证码
      setTimeout(() => {
        this.setData({ 
          sending: false,
          countdown: 60 
        })
        
        this.startCountdown()
        
        wx.showToast({
          title: '验证码已发送',
          icon: 'success'
        })
      }, 1000)
    },

    startCountdown() {
      if (this.data.countdown <= 0) return
      
      this.setData({
        countdown: this.data.countdown - 1
      })
      
      setTimeout(() => {
        this.startCountdown()
      }, 1000)
    },

    onGetPhoneNumber(e) {
      if (e.detail.code) {
        // 微信一键获取手机号
        this.triggerEvent('confirm', { 
          code: e.detail.code,
          type: 'wechat'
        })
      }
    },

    onConfirm() {
      const phone = this.data.phoneNumber
      const code = this.data.code

      if (!/^1[3-9]\d{9}$/.test(phone)) {
        wx.showToast({
          title: '请输入正确的手机号',
          icon: 'none'
        })
        return
      }

      if (!/^\d{4,6}$/.test(code)) {
        wx.showToast({
          title: '请输入验证码',
          icon: 'none'
        })
        return
      }

      this.triggerEvent('confirm', { 
        phone,
        code,
        type: 'sms'
      })
    },

    onClose() {
      this.triggerEvent('close')
    }
  }
})
