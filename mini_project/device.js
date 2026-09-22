const api = require('../../utils/api')
const app = getApp()
const { pullToRefresh } = require('../../utils/refresh')

const TYPE_LABEL = {
  REPAIR: '故障报修',
  SUGGEST: '使用建议',
  OTHER: '其他反馈'
}

Page({
  data: {
    deviceId: api.deviceId(),
    apiBase: '',
    backendOk: false,
    loadingList: true,
    username: '',
    nickname: '',
    type: 'REPAIR',
    types: [
      { key: 'REPAIR', label: '故障报修' },
      { key: 'SUGGEST', label: '使用建议' },
      { key: 'OTHER', label: '其他反馈' }
    ],
    contact: '',
    content: '',
    submitting: false,
    items: []
  },

  onShow() {
    if (!app || !app.globalData || !app.globalData.token) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    const conf = app.globalData || {}
    this.setData({
      apiBase: conf.apiBase || 'http://127.0.0.1:8080',
      deviceId: api.deviceId(),
      username: conf.username || '',
      nickname: conf.nickname || conf.username || ''
    })
    this.refreshAll()
  },

  onPullDownRefresh() {
    pullToRefresh(this, () => this.refreshAll())
  },

  onLogout() {
    api.logout().catch(() => {})
    app.globalData.token = ''
    app.globalData.username = ''
    app.globalData.nickname = ''
    try {
      wx.removeStorageSync('env_token')
      wx.removeStorageSync('env_username')
      wx.removeStorageSync('env_nickname')
    } catch (e) {}
    wx.reLaunch({ url: '/pages/login/login' })
  },

  refreshAll() {
    return Promise.all([
      api.health().then(() => this.setData({ backendOk: true })).catch(() => this.setData({ backendOk: false })),
      this.loadFeedback()
    ])
  },

  loadFeedback() {
    this.setData({ loadingList: true })
    return api.listFeedback()
      .then(list => {
        const items = (list || []).map(f => ({
          id: f.id,
          type: f.type,
          typeLabel: TYPE_LABEL[f.type] || f.type,
          contact: f.contact || '未填',
          content: f.content,
          status: f.status === 'OPEN' ? '处理中' : '已完成',
          statusClass: f.status === 'OPEN' ? 'warn' : 'info',
          time: f.createdAt
        }))
        this.setData({ items, loadingList: false })
      })
      .catch(() => {
        this.setData({ items: [], loadingList: false })
      })
  },

  onPickType(e) {
    this.setData({ type: e.currentTarget.dataset.k })
  },

  onContact(e) {
    this.setData({ contact: e.detail.value })
  },

  onContent(e) {
    this.setData({ content: e.detail.value })
  },

  onSubmitFeedback() {
    if (this.data.submitting) return
    const content = (this.data.content || '').trim()
    if (!content) {
      wx.showToast({ title: '请填写反馈内容', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    api.submitFeedback({
      deviceId: this.data.deviceId,
      type: this.data.type,
      contact: (this.data.contact || '').trim(),
      content
    })
      .then(() => {
        wx.showToast({ title: '提交成功', icon: 'success' })
        this.setData({ content: '', submitting: false })
        this.loadFeedback()
      })
      .catch(err => {
        this.setData({ submitting: false })
        wx.showToast({ title: err.message || '提交失败', icon: 'none' })
      })
  }
})
