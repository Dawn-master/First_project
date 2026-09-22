// 全局配置
App({
  onLaunch() {
    try {
      const token = wx.getStorageSync('env_token')
      const username = wx.getStorageSync('env_username')
      const nickname = wx.getStorageSync('env_nickname')
      if (token) {
        this.globalData.token = token
        this.globalData.username = username || ''
        this.globalData.nickname = nickname || username || ''
      }
    } catch (e) {}
  },
  globalData: {
    // 模拟器：127.0.0.1；真机改为电脑局域网 IP
    apiBase: 'http://127.0.0.1:8080',
    deviceApiKey: 'env-monitor-2026',
    defaultDeviceId: 'sensor-001',
    token: '',
    username: '',
    nickname: ''
  }
})
