const api = require('../../utils/api')
const { pullToRefresh } = require('../../utils/refresh')

const FRESH_SEC = 15

function calcAgeSec(iso) {
  if (!iso) return -1
  let t = new Date(iso).getTime()
  if (Number.isNaN(t)) t = new Date(String(iso).replace(' ', 'T')).getTime()
  if (Number.isNaN(t)) return -1
  return Math.max(0, Math.round((Date.now() - t) / 1000))
}

function fmtTime(iso) {
  if (!iso) return '--'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso)
  const p = n => (n < 10 ? '0' + n : '' + n)
  return p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
}

const EVENT_KEY = 'env_pir_events'

Page({
  data: {
    loading: true,
    hours: 24,
    hasData: false,
    count: 0,
    avgTemp: '无数据',
    avgHum: '无数据',
    avgMq: '无数据',
    maxMq: '无数据',
    presenceRate: '无数据',
    tip: '仅硬件上报 · 数据约每 5 秒自动刷新',
    categories: [],
    seriesTempHum: [],
    seriesMq: [],
    events: [],
    enterCount: 0,
    exitCount: 0,
    eventTip: '红外进出事件（进入/离开房间）',
    lastRefresh: '--'
  },

  onShow() {
    this.load()
    // 实时刷新：硬件上报后曲线/统计/事件会跟着变
    this._timer = setInterval(() => {
      this.load(true)
    }, 5000)
  },

  onHide() {
    if (this._timer) {
      clearInterval(this._timer)
      this._timer = null
    }
  },

  onUnload() {
    if (this._timer) {
      clearInterval(this._timer)
      this._timer = null
    }
  },

  onPullDownRefresh() {
    pullToRefresh(this, () => this.load())
  },

  onPickHours(e) {
    this.setData({ hours: Number(e.currentTarget.dataset.h) })
    this.load()
  },

  load(silent) {
    if (!silent) this.setData({ loading: true })
    return Promise.all([
      this.loadHistory(),
      this.loadEvents(),
      this.checkAlarmModal()
    ]).then(() => {
      const d = new Date()
      const p = n => (n < 10 ? '0' + n : '' + n)
      this.setData({
        loading: false,
        lastRefresh: p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /** 曲线页也做阈值弹窗提醒（与首页共用实时接口） */
  checkAlarmModal() {
    return api.fetchRealtime()
      .then(rt => {
        const thr = (rt && rt.threshold) || {}
        const alarms = (thr.alarms || []).slice()
        const alarm = !!(thr.alarm || alarms.length)
        if (!alarm || !alarms.length) return
        this._alarmKey = this._alarmKey || null
        const key = alarms.slice().sort().join('||')
        if (this._alarmKey === key) return
        this._alarmKey = key
        const lines = alarms.map((a, i) => '「' + (i + 1) + '」' + a)
        try { wx.vibrateShort() } catch (e) {}
        wx.showModal({
          title: '环境阈值报警',
          content: lines.join('\n') + '\n\n请检查环境或硬件传感器。',
          showCancel: false,
          confirmText: '我知道了',
          confirmColor: '#D64545'
        })
      })
      .catch(() => {})
  },

  loadHistory() {
    return api.fetchHistory(this.data.hours)
      .then(hist => {
        const points = (hist && hist.points) || []
        const stats = (hist && hist.stats) || null
        // 统计优先用后端区间聚合；没有 stats 时退回用曲线点估算
        const nChart = points.length
        const total = stats && stats.totalCount != null ? stats.totalCount : nChart
        if (!total) {
          this.setData({
            hasData: false, count: 0,
            avgTemp: '无数据', avgHum: '无数据', avgMq: '无数据',
            maxMq: '无数据', presenceRate: '无数据',
            categories: [], seriesTempHum: [], seriesMq: [],
            tip: '该区间暂无硬件历史数据'
          })
          return
        }

        let avgT, avgH, avgMq, maxMq, rate
        if (stats && stats.maxMq135 != null) {
          avgT = stats.avgTemperature
          avgH = stats.avgHumidity
          avgMq = stats.avgMq135
          maxMq = stats.maxMq135
          rate = stats.presenceRate
        } else {
          let st = 0, sh = 0, sm = 0, mm = -Infinity, pc = 0
          points.forEach(p => {
            st += p.temperature || 0
            sh += p.humidity || 0
            sm += p.mq135 || 0
            mm = Math.max(mm, p.mq135 || 0)
            if (p.pir === 0) pc++
          })
          avgT = st / nChart
          avgH = sh / nChart
          avgMq = sm / nChart
          maxMq = mm
          rate = pc / nChart
        }

        const fmt1 = v => (v == null || v === '' || Number.isNaN(Number(v))) ? '--' : Number(v).toFixed(1)

        const cats = points.map(p => {
          const d = new Date(p.time)
          if (Number.isNaN(d.getTime())) return ''
          const pad = x => (x < 10 ? '0' + x : '' + x)
          return pad(d.getHours()) + ':' + pad(d.getMinutes())
        })

        const seriesTempHum = [
          { name: '温度℃', color: '#3D7EA6', data: points.map(p => p.temperature) },
          { name: '湿度%', color: '#1F7A6E', data: points.map(p => p.humidity) }
        ]
        const seriesMq = [
          { name: 'MQ135', color: '#D4A017', data: points.map(p => p.mq135) }
        ]

        const tipText = stats && stats.totalCount && nChart < total
          ? ('区间全量 ' + total + ' 条 · 曲线取最新 ' + nChart + ' 点 · 统计含全区间峰值')
          : ('基于硬件采样 · 共 ' + total + ' 点 · 每 5 秒自动刷新')

        this.setData({
          hasData: true,
          tip: tipText,
          count: total,
          avgTemp: fmt1(avgT),
          avgHum: fmt1(avgH),
          avgMq: fmt1(avgMq),
          maxMq: fmt1(maxMq),
          presenceRate: (rate == null || Number.isNaN(rate)) ? '--' : Math.round(rate * 100) + '%',
          categories: cats,
          seriesTempHum,
          seriesMq
        })
      })
      .catch(err => {
        this.setData({
          hasData: false,
          tip: err.message || '历史加载失败',
          categories: [], seriesTempHum: [], seriesMq: []
        })
      })
  },

  loadEvents() {
    return api.fetchEvents(this.data.hours)
      .then(list => {
        const events = (list || []).map(e => ({
          id: e.id,
          type: e.eventType,
          label: e.label || (e.eventType === 'ENTER' ? '有人进入房间' : '离开房间'),
          cls: e.eventType === 'ENTER' ? 'enter' : 'exit',
          time: fmtTime(e.eventTime),
          temp: e.temperature,
          mq: e.mq135
        }))
        const enterCount = events.filter(e => e.type === 'ENTER').length
        const exitCount = events.filter(e => e.type === 'EXIT').length
        this.setData({ events, enterCount, exitCount })
        // 小程序本地缓存，便于离线查看
        try { wx.setStorageSync(EVENT_KEY, events) } catch (err) {}
      })
      .catch(err => {
        let local = []
        try { local = wx.getStorageSync(EVENT_KEY) || [] } catch (e) {}
        this.setData({
          events: local,
          enterCount: local.filter(e => e.type === 'ENTER').length,
          exitCount: local.filter(e => e.type === 'EXIT').length,
          eventTip: '接口暂不可用，展示本机缓存事件'
        })
      })
  },

  onClearLocalEvents() {
    try { wx.removeStorageSync(EVENT_KEY) } catch (e) {}
    wx.showToast({ title: '已清本机缓存', icon: 'none' })
  }
})
