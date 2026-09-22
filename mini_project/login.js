const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    loading: false,
    tip: '环境监测系统 · 请登录'
  },

  onShow() {
    const t = app.globalData && app.globalData.token
    if (t) {
      wx.switchTab({ url: '/pages/index/index' })
    }
  },

  onUsername(e) { this.setData({ username: e.detail.value }) },
  onPassword(e) { this.setData({ password: e.detail.value }) },

  onLogin() {
    if (this.data.loading) return
    const username = (this.data.username || '').trim()
    const password = this.data.password || ''
    if (!username || !password) {
      wx.showToast({ title: '请输入账号和密码', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    api.login(username, password)
      .then(data => {
        app.globalData.token = data.token
        app.globalData.username = data.username
        app.globalData.nickname = data.nickname || data.username
        try {
          wx.setStorageSync('env_token', data.token)
          wx.setStorageSync('env_username', data.username)
          wx.setStorageSync('env_nickname', data.nickname || data.username)
        } catch (e) {}
        wx.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 400)
      })
      .catch(err => {
        wx.showToast({ title: err.message || '登录失败', icon: 'none' })
      })
      .finally(() => this.setData({ loading: false }))
  },

  goRegister() {
    wx.navigateTo({ url: '/pages/register/register' })
  }
})
