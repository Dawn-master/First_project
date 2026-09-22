Component({
  properties: {
    /** [{name, data:[]}] */
    series: {
      type: Array,
      value: []
    },
    /** x 轴标签 */
    categories: {
      type: Array,
      value: []
    },
    height: {
      type: Number,
      value: 260
    },
    title: {
      type: String,
      value: ''
    }
  },

  data: {
    canvasId: '',
    dpr: 1,
    nodeReady: false
  },

  lifetimes: {
    attached() {
      const id = 'c' + Date.now() + Math.floor(Math.random() * 1000)
      this.setData({ canvasId: id })
      this._dpr = wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : wx.getSystemInfoSync().pixelRatio
    },
    ready() {
      this.draw()
    }
  },

  observers: {
    'series, categories, height': function () {
      this.draw()
    }
  },

  methods: {
    draw() {
      const that = this
      const series = this.data.series || []
      const categories = this.data.categories || []
      const h = this.data.height || 260
      const dpr = this._dpr || 2

      const query = this.createSelectorQuery()
      query.select('#' + this.data.canvasId)
        .fields({ node: true, size: true })
        .exec(function (res) {
          if (!res || !res[0] || !res[0].node) {
            return
          }
          const canvas = res[0].node
          const width = res[0].width
          const height = res[0].height || h
          canvas.width = width * dpr
          canvas.height = height * dpr
          const ctx = canvas.getContext('2d')
          ctx.scale(dpr, dpr)
          that._paint(ctx, width, height, series, categories)
        })
    },

    _paint(ctx, W, H, series, categories) {
      const pad = { l: 36, r: 12, t: 28, b: 28 }
      const cw = W - pad.l - pad.r
      const ch = H - pad.t - pad.b

      ctx.clearRect(0, 0, W, H)
      ctx.fillStyle = '#F7FAF8'
      ctx.fillRect(0, 0, W, H)

      if (!series.length || !categories.length) {
        ctx.fillStyle = '#7A8B86'
        ctx.font = '12px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('暂无历史数据', W / 2, H / 2)
        return
      }

      let min = Infinity
      let max = -Infinity
      series.forEach(function (s) {
        (s.data || []).forEach(function (v) {
          if (typeof v === 'number' && !isNaN(v)) {
            if (v < min) min = v
            if (v > max) max = v
          }
        })
      })
      if (!isFinite(min)) { min = 0; max = 1 }
      if (min === max) { min -= 1; max += 1 }
      const padY = (max - min) * 0.1
      min -= padY
      max += padY

      const n = categories.length
      const colors = ['#3D7EA6', '#1F7A6E', '#D4A017', '#D64545']

      // grid
      ctx.strokeStyle = '#D5DDD8'
      ctx.lineWidth = 1
      ctx.fillStyle = '#7A8B86'
      ctx.font = '10px sans-serif'
      ctx.textAlign = 'right'
      const rows = 4
      for (let i = 0; i <= rows; i++) {
        const y = pad.t + (ch * i) / rows
        const val = max - ((max - min) * i) / rows
        ctx.beginPath()
        ctx.moveTo(pad.l, y)
        ctx.lineTo(pad.l + cw, y)
        ctx.stroke()
        ctx.fillText(val.toFixed(1), pad.l - 4, y + 3)
      }

      // x labels
      ctx.textAlign = 'center'
      const step = Math.max(1, Math.ceil(n / 6))
      for (let i = 0; i < n; i += step) {
        const x = pad.l + (cw * i) / Math.max(n - 1, 1)
        ctx.fillText(String(categories[i] || ''), x, H - 8)
      }

      // lines
      series.forEach(function (s, si) {
        const color = s.color || colors[si % colors.length]
        const data = s.data || []
        ctx.strokeStyle = color
        ctx.lineWidth = 2
        ctx.beginPath()
        let started = false
        data.forEach(function (v, i) {
          if (typeof v !== 'number' || isNaN(v)) return
          const x = pad.l + (cw * i) / Math.max(n - 1, 1)
          const y = pad.t + ch * (1 - (v - min) / (max - min))
          if (!started) { ctx.moveTo(x, y); started = true } else ctx.lineTo(x, y)
        })
        ctx.stroke()
      })

      // legend
      let lx = pad.l
      ctx.textAlign = 'left'
      series.forEach(function (s, si) {
        const color = s.color || colors[si % colors.length]
        ctx.fillStyle = color
        ctx.fillRect(lx, 8, 10, 10)
        ctx.fillStyle = '#0F1C24'
        ctx.font = '11px sans-serif'
        ctx.fillText(s.name || ('s' + si), lx + 14, 17)
        lx += 14 + ctx.measureText(s.name || '').width + 20
      })
    }
  }
})
