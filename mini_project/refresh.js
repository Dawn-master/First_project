/**
 * 页面下拉刷新公共处理：保证一定 stopPullDownRefresh
 */
function stopPullDownRefresh(toast) {
  try {
    wx.stopPullDownRefresh()
  } catch (e) {}
  if (toast) {
    wx.showToast({ title: '已刷新', icon: 'success', duration: 700 })
  }
}

function pullToRefresh(page, work, options) {
  const show = !options || options.toast !== false
  Promise.resolve()
    .then(() => (typeof work === 'function' ? work() : null))
    .catch(() => {})
    .then(() => stopPullDownRefresh(show))
}

module.exports = {
  stopPullDownRefresh,
  pullToRefresh
}
