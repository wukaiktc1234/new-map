const store = require('./store')

const ERROR_MESSAGES = {
  'NETWORK_ERROR': '网络连接失败，请检查网络设置',
  'TIMEOUT': '请求超时，请稍后重试',
  'SERVER_ERROR': '服务器异常，请稍后重试',
  'UNAUTHORIZED': '登录已过期，请重新登录',
  'FORBIDDEN': '没有权限访问',
  'NOT_FOUND': '请求的资源不存在',
  'VALIDATION_ERROR': '数据验证失败',
  'UNKNOWN': '未知错误'
}

const request = (options) => {
  return new Promise((resolve, reject) => {
    const apiBase = 'http://localhost:8081/api/v1'
    const url = apiBase + options.url
    
    const showLoading = options.showLoading !== false
    const loadingText = options.loadingText || '加载中...'
    
    if (showLoading) {
      wx.showLoading({ title: loadingText, mask: true })
    }
    
    const timeout = options.timeout || 15000
    
    const requestTask = wx.request({
      url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...options.header
      },
      timeout,
      success: (res) => {
        if (showLoading) {
          wx.hideLoading()
        }
        
        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (res.data && res.data.code === 0) {
            resolve(res.data)
          } else if (res.data && res.data.code === 401) {
            handleUnauthorized()
            reject(new Error('登录已过期'))
          } else {
            const message = res.data?.message || ERROR_MESSAGES.UNKNOWN
            showError(message)
            reject(new Error(message))
          }
        } else if (res.statusCode === 401) {
          handleUnauthorized()
          reject(new Error('登录已过期'))
        } else if (res.statusCode >= 400 && res.statusCode < 500) {
          const message = ERROR_MESSAGES.VALIDATION_ERROR
          showError(message)
          reject(new Error(message))
        } else {
          showError(ERROR_MESSAGES.SERVER_ERROR)
          reject(new Error(ERROR_MESSAGES.SERVER_ERROR))
        }
      },
      fail: (err) => {
        if (showLoading) {
          wx.hideLoading()
        }
        
        let message = ERROR_MESSAGES.NETWORK_ERROR
        if (err.errMsg && err.errMsg.includes('timeout')) {
          message = ERROR_MESSAGES.TIMEOUT
        }
        
        showError(message)
        reject(new Error(message))
      }
    })
    
    if (options.timeout) {
      setTimeout(() => {
        requestTask.abort()
      }, options.timeout)
    }
  })
}

const showError = (message, duration = 2000) => {
  wx.showToast({
    title: message,
    icon: 'none',
    duration
  })
}

const showSuccess = (message, duration = 1500) => {
  wx.showToast({
    title: message,
    icon: 'success',
    duration
  })
}

const handleUnauthorized = () => {
  store.logout()
  showError('登录已过期，请重新登录')
  setTimeout(() => {
    wx.reLaunch({ url: '/pages/index/index' })
  }, 1500)
}

const showLoading = (title = '加载中...') => {
  wx.showLoading({ title, mask: true })
}

const hideLoading = () => {
  wx.hideLoading()
}

const showConfirm = (title, content) => {
  return new Promise((resolve) => {
    wx.showModal({
      title,
      content,
      success: (res) => {
        resolve(res.confirm)
      },
      fail: () => {
        resolve(false)
      }
    })
  })
}

module.exports = {
  request,
  showError,
  showSuccess,
  showLoading,
  hideLoading,
  showConfirm,
  ERROR_MESSAGES
}
