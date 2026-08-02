const app = getApp()
const store = require('../../utils/store.js')

Page({
  data: {
    statusBarHeight: 0,
    headerHeight: 0,
    isLoggedIn: false,
    showLoginModal: false,
    addresses: [],
    selectMode: false,
    editingAddress: null,
    showEditModal: false,
    editForm: {
      name: '',
      phone: '',
      province: '广东省',
      city: '广州市',
      district: '',
      detail: '',
      isDefault: false
    }
  },

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      headerHeight: systemInfo.statusBarHeight * 2 + 72,
      selectMode: options.select === 'true'
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
    
    this.setData({ isLoggedIn: true })
    this.loadAddresses()
  },

  onLoginSuccess() {
    this.setData({ showLoginModal: false })
    this.checkLoginStatus()
  },

  onCloseLoginModal() {
    this.setData({ showLoginModal: false })
    wx.navigateBack()
  },

  loadAddresses() {
    if (!app.globalData.addresses || app.globalData.addresses.length === 0) {
      app.globalData.addresses = [
        {
          id: 'a1',
          name: '张三',
          phone: '138****8888',
          province: '广东省',
          city: '广州市',
          district: '天河区',
          detail: '天河路385号太古汇商场B1层',
          isDefault: true
        },
        {
          id: 'a2',
          name: '李四',
          phone: '139****6666',
          province: '广东省',
          city: '广州市',
          district: '越秀区',
          detail: '中山五路219号中旅商业城',
          isDefault: false
        }
      ]
      wx.setStorageSync('addresses', app.globalData.addresses)
    }
    this.setData({ addresses: app.globalData.addresses })
  },

  selectAddress(e) {
    const id = e.currentTarget.dataset.id
    const address = this.data.addresses.find(a => a.id === id)
    
    if (this.data.selectMode && address) {
      store.setSelectedAddress(address)
      app.globalData.selectedAddress = address
      wx.showToast({
        title: '已选择',
        icon: 'success',
        duration: 500
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 500)
    } else {
      wx.showToast({
        title: '已选择地址',
        icon: 'success'
      })
    }
  },

  setDefault(e) {
    const id = e.currentTarget.dataset.id
    const addresses = this.data.addresses.map(addr => ({
      ...addr,
      isDefault: addr.id === id
    }))
    app.globalData.addresses = addresses
    wx.setStorageSync('addresses', addresses)
    this.setData({ addresses })
    wx.showToast({
      title: '已设为默认',
      icon: 'success'
    })
  },

  editAddress(e) {
    const id = e.currentTarget.dataset.id
    const address = this.data.addresses.find(a => a.id === id)
    if (address) {
      this.setData({
        editingAddress: address,
        showEditModal: true,
        editForm: {
          name: address.name,
          phone: address.phone,
          province: address.province,
          city: address.city,
          district: address.district,
          detail: address.detail,
          isDefault: address.isDefault
        }
      })
    }
  },

  deleteAddress(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个地址吗？',
      confirmText: '删除',
      confirmColor: '#E54D42',
      success: (res) => {
        if (res.confirm) {
          let addresses = this.data.addresses.filter(addr => addr.id !== id)
          if (addresses.length > 0 && !addresses.some(a => a.isDefault)) {
            addresses[0].isDefault = true
          }
          app.globalData.addresses = addresses
          wx.setStorageSync('addresses', addresses)
          this.setData({ addresses })
          wx.showToast({
            title: '删除成功',
            icon: 'success'
          })
        }
      }
    })
  },

  addAddress() {
    this.setData({
      editingAddress: null,
      showEditModal: true,
      editForm: {
        name: '',
        phone: '',
        province: '广东省',
        city: '广州市',
        district: '',
        detail: '',
        isDefault: false
      }
    })
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [`editForm.${field}`]: e.detail.value
    })
  },

  onRegionChange(e) {
    const values = e.detail.value
    this.setData({
      'editForm.province': values[0],
      'editForm.city': values[1],
      'editForm.district': values[2]
    })
  },

  toggleDefault() {
    this.setData({
      'editForm.isDefault': !this.data.editForm.isDefault
    })
  },

  saveAddress() {
    const { editForm, editingAddress, addresses } = this.data
    
    if (!editForm.name.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' })
      return
    }
    if (!editForm.phone.trim()) {
      wx.showToast({ title: '请输入电话', icon: 'none' })
      return
    }
    if (!editForm.detail.trim()) {
      wx.showToast({ title: '请输入详细地址', icon: 'none' })
      return
    }

    let newAddresses = [...addresses]
    
    if (editingAddress) {
      const index = newAddresses.findIndex(a => a.id === editingAddress.id)
      if (index > -1) {
        newAddresses[index] = {
          ...editingAddress,
          ...editForm
        }
      }
    } else {
      const newAddress = {
        id: 'a' + Date.now(),
        ...editForm
      }
      newAddresses.push(newAddress)
    }

    if (editForm.isDefault) {
      newAddresses = newAddresses.map(a => ({
        ...a,
        isDefault: a.id === (editingAddress ? editingAddress.id : 'a' + Date.now())
      }))
    }

    app.globalData.addresses = newAddresses
    wx.setStorageSync('addresses', newAddresses)
    this.setData({ 
      addresses: newAddresses,
      showEditModal: false 
    })
    wx.showToast({
      title: editingAddress ? '修改成功' : '添加成功',
      icon: 'success'
    })
  },

  closeEditModal() {
    this.setData({ showEditModal: false })
  },

  preventTouchMove() {},

  goBack() {
    wx.navigateBack()
  }
})
