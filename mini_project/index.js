const api = require('../../utils/api')
const { pullToRefresh } = require('../../utils/refresh')

function pad(n) { return n < 10 ? '0' + n : '' + n }
function formatClock() {
  const d = new Date()
  return pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
}

const FRESH_SEC = 15

Page({
  data: {
    clock: formatClock(),
    deviceId: api.deviceId(),
    hasData: false,
    statusText: '无数据',
    status: 'offline',
    temp: '无数据',
    humidity: '无数据',
    mq135: '无数据',
    pirText: '无数据',
    presence: false,
    score: '--',
    scoreLevel: '--',
    updatedAt: '--',
    ageText: '--',
    ledOn: false,
    ledBusy: false,
    alarm: false,
    alarms: [],
    alarmReasons: '',
    thrText: {
      temp: '超过 30℃ 报警',
      humidity: '超过 50% 报警',
      mq135: '超过 200 报警'
    },
    metricOk: {
      temp: true,
      humidity: true,
      mq135: true
    },
    tip: '仅硬件实时 · 模拟已禁用'
  },

  onLoad() {
    this.poll = null
    this.clockTimer = null
    // 报警弹窗去重：同一组原因只弹一次；恢复后再超标会再弹
    this._alarmKey = null
    this._alarmShowing = false
  },

  onShow() {
    const app = getApp()
    if (!app || !app.globalData || !app.globalData.token) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.clockTimer = setInterval(() => this.setData({ clock: formatClock() }), 1000)
    this.load()
    this.poll = setInterval(() => this.load(), 4000)
  },

  onHide() { this.clear() },
  onUnload() { this.clear() },

  clear() {
    if (this.clockTimer) clearInterval(this.clockTimer)
    if (this.poll) clearInterval(this.poll)
    this.clockTimer = null
    this.poll = null
  },

  onPullDownRefresh() {
    pullToRefresh(this, () => this.load())
  },

  calcAge(iso) {
    if (!iso) return -1
    let t = new Date(iso).getTime()
    if (Number.isNaN(t)) t = new Date(String(iso).replace(' ', 'T')).getTime()
    if (Number.isNaN(t)) return -1
    return Math.max(0, Math.round((Date.now() - t) / 1000))
  },

  load() {
    return api.request('/api/sensor/realtime?deviceId=' + api.deviceId())
      .then(data => {
        const latest = data.latest
        const thr = data.threshold || {}
        const led = !!data.led
        const age = this.calcAge(latest && latest.recordedAt)
        const live = !!(data.hasData && latest && age >= 0 && age <= FRESH_SEC)

        const thrText = {
          temp: (thr.thresholds && thr.thresholds.tempRule) || '超过 30℃ 报警',
          humidity: (thr.thresholds && thr.thresholds.humidityRule) || '超过 50% 报警',
          mq135: (thr.thresholds && thr.thresholds.mq135Rule) || '超过 200 报警'
        }

        const metricOk = { temp: true, humidity: true, mq135: true }
        if (live) {
          metricOk.temp = !!(thr.temperature && thr.temperature.ok)
          metricOk.humidity = !!(thr.humidity && thr.humidity.ok)
          metricOk.mq135 = !!(thr.mq135 && thr.mq135.ok)
        }

        const scoreObj = data.score
        const score = scoreObj ? scoreObj.score : '--'

        if (!live) {
          this._alarmKey = null
          this.setData({
            hasData: false,
            status: 'offline',
            statusText: '无数据',
            temp: '无数据',
            humidity: '无数据',
            mq135: '无数据',
            pirText: '无数据',
            presence: false,
            score: '--',
            scoreLevel: '--',
            updatedAt: '--',
            ageText: age >= 0 ? ('硬件已 ' + age + ' 秒未上报') : '--',
            ledOn: led,
            alarm: false,
            alarms: [],
            alarmReasons: '',
            thrText,
            metricOk: { temp: true, humidity: true, mq135: true },
            tip: '当前无硬件实时数据'
          })
          return
        }

        const alarms = (thr.alarms || []).slice()
        const alarm = !!thr.alarm || alarms.length > 0
        let status = 'good'
        let statusText = '正常'
        if (alarm) {
          status = 'alert'
          statusText = '阈值报警'
        }

        this.setData({
          hasData: true,
          status,
          statusText,
          temp: latest.temperature != null ? latest.temperature : '无数据',
          humidity: latest.humidity != null ? latest.humidity : '无数据',
          mq135: latest.mq135 != null ? latest.mq135 : '无数据',
          pirText: latest.presence ? '有人' : '无人',
          presence: !!latest.presence,
          score: score,
          scoreLevel: scoreObj ? scoreObj.level : '--',
          updatedAt: latest.recordedAt,
          ageText: age + ' 秒前',
          ledOn: led,
          alarm,
          alarms,
          alarmReasons: alarms.join('\n'),
          thrText,
          metricOk,
          tip: alarm ? '存在超阈值项，请查看报警原因' : '实时监测中'
        })

        // 标红之外：弹窗说明原因（仅在报警原因变化时）
        this.showAlarmModalIfNeeded(alarm, alarms)
      })
      .catch(err => {
        this.setData({
          hasData: false,
          status: 'offline',
          statusText: '无数据',
          temp: '无数据',
          humidity: '无数据',
          mq135: '无数据',
          pirText: '无数据',
          score: '--',
          alarm: false,
          alarms: [],
          alarmReasons: '',
          tip: (err.message || '无法连接后端')
        })
      })
  },

  /**
   * 数据超过阈值：前端卡片已标红；这里再弹警告框说明原因。
   * 同一组原因持续超标时只弹一次，恢复后再超标会再次弹出。
   */
  showAlarmModalIfNeeded(alarm, alarms) {
    if (!alarm || !alarms || !alarms.length) {
      this._alarmKey = null
      return
    }
    const key = alarms.slice().sort().join('||')
    if (this._alarmKey === key || this._alarmShowing) {
      return
    }
    this._alarmKey = key
    this._alarmShowing = true

    const lines = alarms.map((a, i) => '「' + (i + 1) + '」' + a)
    const content = lines.join('\n') + '\n\n请检查环境或硬件传感器。'

    try { wx.vibrateShort() } catch (e) {}

    wx.showModal({
      title: '环境阈值报警',
      content: content,
      showCancel: false,
      confirmText: '我知道了',
      confirmColor: '#D64545',
      success: () => { this._alarmShowing = false },
      fail: () => { this._alarmShowing = false }
    })
  },

  onToggleLed() {
    if (this.data.ledBusy) return
    const next = !this.data.ledOn
    this.setData({ ledBusy: true })
    api.request('/api/device/led', {
      method: 'POST',
      data: { deviceId: api.deviceId(), on: next }
    })
      .then(() => {
        this.setData({ ledOn: next, ledBusy: false })
        wx.showToast({
          title: next ? '已请求开灯（板端约1秒生效）' : '已请求关灯（板端约1秒生效）',
          icon: 'none',
          duration: 1500
        })
        // 稍后再刷新，确认后端 led 状态
        setTimeout(() => this.load(), 1200)
      })
      .catch(err => {
        this.setData({ ledBusy: false })
        wx.showToast({ title: err.message || 'LED 控制失败', icon: 'none' })
      })
  }
})
