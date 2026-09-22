function g() {
  return (typeof getApp === 'function' && getApp() && getApp().globalData) || {}
}

function request(path, options = {}) {
  const conf = g()
  const base = conf.apiBase || 'http://127.0.0.1:8080'
  const header = {
    'Content-Type': 'application/json',
    'X-Device-Key': conf.deviceApiKey || 'env-monitor-2026'
  }
  if (conf.token) {
    header['X-Token'] = conf.token
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url: base + path,
      method: options.method || 'GET',
      header,
      data: options.data,
      success(res) {
        const body = res.data
        if (body && body.code === 0) {
          resolve(body.data)
        } else {
          const err = new Error((body && body.message) || '接口返回异常')
          err.code = body && body.code
          reject(err)
        }
      },
      fail(err) {
        reject(new Error(err.errMsg || '网络请求失败'))
      }
    })
  })
}

function deviceId() {
  return g().defaultDeviceId || 'sensor-001'
}

function login(username, password) {
  return request('/api/auth/login', { method: 'POST', data: { username, password } })
}

function register(username, password, nickname) {
  return request('/api/auth/register', { method: 'POST', data: { username, password, nickname } })
}

function logout() {
  return request('/api/auth/logout', { method: 'POST' })
}

function fetchDashboard() {
  return request('/api/sensor/realtime?deviceId=' + deviceId())
}

function fetchLatest() {
  return request('/api/sensor/latest?deviceId=' + deviceId())
}

function fetchRealtime() {
  return request('/api/sensor/realtime?deviceId=' + deviceId())
}

function setLed(on, devId) {
  return request('/api/device/led', {
    method: 'POST',
    data: { deviceId: devId || deviceId(), on: !!on }
  })
}

function fetchHistory(hours) {
  return request('/api/sensor/history?deviceId=' + deviceId() + '&hours=' + (hours || 24))
}

/** 红外进出事件日志 */
function fetchEvents(hours) {
  const h = hours ? '&hours=' + hours : ''
  return request('/api/device/events?deviceId=' + deviceId() + h)
}

function fetchAlerts() {
  return request('/api/alerts?deviceId=' + deviceId())
}

function resolveAlert(id) {
  return request('/api/alerts/' + id + '/resolve', { method: 'POST' })
}

function submitFeedback(payload) {
  return request('/api/feedback', { method: 'POST', data: payload })
}

function listFeedback() {
  return request('/api/feedback?deviceId=' + deviceId())
}

function health() {
  return request('/api/health')
}

module.exports = {
  request,
  deviceId,
  login,
  register,
  logout,
  fetchDashboard,
  fetchLatest,
  fetchRealtime,
  setLed,
  fetchHistory,
  fetchEvents,
  fetchAlerts,
  resolveAlert,
  submitFeedback,
  listFeedback,
  health
}

module.exports.pullToRefresh = require('./refresh').pullToRefresh
