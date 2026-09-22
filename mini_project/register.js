const api = require('../../utils/api')

Page({
  data: {
    username: '',
    password: '',
    password2: '',
    nickname: '',
    loading: false
  },

  onUsername(e) { this.setData({ username: e.detail.value }) },
  onPassword(e) { this.setData({ password: e.detail.value }) },
  onPassword2(e) { this.setData({ password2: e.detail.value }) },
  onNickname(e) { this.setData({ nickname: e.detail.value }) },

  onRegister() {
    if (this.data.loading) return
    const username = (this.data.username || '').trim()
    const password = this.data.password || ''
    const password2 = this.data.password2 || ''
    const nickname = (this.data.nickname || '').trim()

    if (!username || !password) {
      wx.showToast({ title: '请填写账号和密码', icon: 'none' })
      return
    }
    if (password.length < 6) {
      wx.showToast({ title: '密码至少 6 位', icon: 'none' })
      return
    }
    if (password !== password2) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    api.register(username, password, nickname)
      .then(() => {
        wx.showToast({ title: '注册成功，请登录', icon: 'success' })
        setTimeout(() => wx.navigateBack(), 600)
      })
      .catch(err => {
        wx.showToast({ title: err.message || '注册失败', icon: 'none' })
      })
      .finally(() => this.setData({ loading: false }))
  },

  goLogin() {
    wx.navigateBack()
  }
})
