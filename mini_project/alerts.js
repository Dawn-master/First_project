const api = require('../../utils/api')
const { pullToRefresh } = require('../../utils/refresh')

function pad(n) { return n < 10 ? '0' + n : '' + n }
function fmt(iso) {
  if (!iso) return '--'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso)
  return pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

Page({
  data: {
    loading: true,
    error: '',
    onlyOpen: false,
    items: []
  },

  onShow() {
    const app = getApp()
    if (!app || !app.globalData || !app.globalData.token) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.load()
  },

  onPullDownRefresh() {
    pullToRefresh(this, () => this.load())
  },

  onToggleOpen() {
    this.setData({ onlyOpen: !this.data.onlyOpen })
    this.load()
  },

  load() {
    this.setData({ loading: true, error: '' })
    return api.fetchAlerts()
      .then(list => {
        let items = (list || []).map(a => ({
          id: a.id,
          message: a.message,
          type: a.alertType,
          level: a.level,
          levelClass: a.level === 'DANGER' ? 'danger' : 'warn',
          levelText: a.level === 'DANGER' ? '危险' : '警告',
          resolved: !!a.resolved,
          time: fmt(a.createdAt)
        }))
        if (this.data.onlyOpen) {
          items = items.filter(x => !x.resolved)
        }
        this.setData({ items, loading: false })
      })
      .catch(err => {
        this.setData({ loading: false, error: err.message || '加载失败', items: [] })
      })
  },

  onMarkDone(e) {
    const id = e.currentTarget.dataset.id
    api.resolveAlert(id)
      .then(() => {
        wx.showToast({ title: '已处理', icon: 'success' })
        this.load()
      })
      .catch(err => {
        wx.showToast({ title: err.message || '操作失败', icon: 'none' })
      })
  }
})
