import type { Ref } from 'vue'
import type { Order } from '~/types/order'
import type { 
  TimeRange, 
  TimelinePoint, 
  ChartPoint, 
  ProductRank, 
  CategorySales, 
  KpiSummary 
} from '~/types/analytics'

export function useAnalytics(orders: Ref<Order[] | null | undefined>) {
  const timeRange = ref<TimeRange>('ALL')

  // 1. Filter orders based on time range
  const rangeFilteredOrders = computed(() => {
    if (!orders.value) return []
    if (timeRange.value === 'ALL') return orders.value

    const now = new Date().getTime()
    const days = timeRange.value === '7D' ? 7 : 30
    const cutoff = now - days * 24 * 60 * 60 * 1000

    return orders.value.filter(o => new Date(o.createdAt).getTime() >= cutoff)
  })

  // 2. Status Segregations
  const paidOrders = computed(() => {
    return rangeFilteredOrders.value.filter(o => o.status === 'PAID')
  })

  const pendingOrders = computed(() => {
    return rangeFilteredOrders.value.filter(o => o.status === 'PENDING')
  })

  const cancelledOrders = computed(() => {
    return rangeFilteredOrders.value.filter(o => o.status === 'CANCELLED')
  })

  // 3. KPI Metrics
  const kpiSummary = computed<KpiSummary>(() => {
    const gross = paidOrders.value.reduce((acc, o) => acc + o.totalAmount, 0)
    const paidCount = paidOrders.value.length
    const aov = paidCount > 0 ? gross / paidCount : 0
    const units = paidOrders.value.reduce((tot, o) => {
      return tot + (o.items?.reduce((s, i) => s + i.quantity, 0) ?? 0)
    }, 0)
    const total = rangeFilteredOrders.value.length
    const rate = total > 0 ? Math.round((paidCount / total) * 100) : 0

    return {
      totalGrossRevenue: gross,
      paidOrdersCount: paidCount,
      averageOrderValue: aov,
      totalUnitsSold: units,
      successRate: rate,
      pendingCount: pendingOrders.value.length,
      cancelledCount: cancelledOrders.value.length
    }
  })

  // 4. Sales Timeline Data & Math for SVG Chart
  const chartWidth = 640
  const chartHeight = 220
  const padding = { top: 20, right: 30, bottom: 40, left: 60 }

  const salesTimeline = computed<TimelinePoint[]>(() => {
    const map = new Map<string, { revenue: number; count: number }>()

    const sorted = [...paidOrders.value].sort(
      (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    )

    sorted.forEach(order => {
      const d = new Date(order.createdAt)
      const dateKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      
      const existing = map.get(dateKey) || { revenue: 0, count: 0 }
      existing.revenue += order.totalAmount
      existing.count += 1
      map.set(dateKey, existing)
    })

    if (map.size === 0) {
      return [{ date: 'Today', label: 'Hari Ini', revenue: 0, orderCount: 0 }]
    }

    return Array.from(map.entries()).map(([dateKey, data]) => {
      const [y, m, d] = dateKey.split('-')
      const formatted = new Intl.DateTimeFormat('id-ID', { day: 'numeric', month: 'short' }).format(new Date(+y, +m - 1, +d))
      return {
        date: dateKey,
        label: formatted,
        revenue: data.revenue,
        orderCount: data.count
      }
    })
  })

  const maxRevenue = computed(() => {
    const max = Math.max(...salesTimeline.value.map(p => p.revenue), 1000000)
    return max * 1.15 // 15% headroom
  })

  const chartPoints = computed<ChartPoint[]>(() => {
    const data = salesTimeline.value
    const innerWidth = chartWidth - padding.left - padding.right
    const innerHeight = chartHeight - padding.top - padding.bottom

    return data.map((point, index) => {
      const x = data.length === 1 
        ? padding.left + innerWidth / 2 
        : padding.left + (index / (data.length - 1)) * innerWidth
      const y = padding.top + innerHeight - (point.revenue / maxRevenue.value) * innerHeight
      return { ...point, x, y }
    })
  })

  const svgLinePath = computed(() => {
    const pts = chartPoints.value
    if (pts.length === 0) return ''
    if (pts.length === 1) return `M ${padding.left} ${pts[0].y} L ${chartWidth - padding.right} ${pts[0].y}`
    
    return pts.reduce((acc, curr, idx, arr) => {
      if (idx === 0) return `M ${curr.x} ${curr.y}`
      const prev = arr[idx - 1]
      const cp1x = prev.x + (curr.x - prev.x) / 2
      const cp1y = prev.y
      const cp2x = prev.x + (curr.x - prev.x) / 2
      const cp2y = curr.y
      return `${acc} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${curr.x} ${curr.y}`
    }, '')
  })

  const svgAreaPath = computed(() => {
    const pts = chartPoints.value
    if (pts.length === 0) return ''
    const bottomY = chartHeight - padding.bottom
    const line = svgLinePath.value
    
    if (pts.length === 1) {
      return `M ${padding.left} ${bottomY} L ${padding.left} ${pts[0].y} L ${chartWidth - padding.right} ${pts[0].y} L ${chartWidth - padding.right} ${bottomY} Z`
    }
    
    return `${line} L ${pts[pts.length - 1].x} ${bottomY} L ${pts[0].x} ${bottomY} Z`
  })

  // 5. Top Best-Selling Products Leaderboard
  const topSellingProducts = computed<ProductRank[]>(() => {
    const map = new Map<string, { category: string; units: number; revenue: number }>()

    paidOrders.value.forEach(order => {
      order.items?.forEach(item => {
        const existing = map.get(item.productName) || {
          category: item.product?.category || 'General',
          units: 0,
          revenue: 0
        }
        existing.units += item.quantity
        existing.revenue += item.subtotal
        map.set(item.productName, existing)
      })
    })

    const totalRev = kpiSummary.value.totalGrossRevenue || 1

    return Array.from(map.entries())
      .map(([name, data]) => ({
        name,
        category: data.category,
        unitsSold: data.units,
        totalRevenue: data.revenue,
        percentOfTotal: Math.min(100, Math.round((data.revenue / totalRev) * 100))
      }))
      .sort((a, b) => b.totalRevenue - a.totalRevenue)
      .slice(0, 5)
  })

  // 6. Category Revenue Share
  const categorySales = computed<CategorySales[]>(() => {
    const map = new Map<string, number>()

    paidOrders.value.forEach(order => {
      order.items?.forEach(item => {
        const cat = item.product?.category || 'Uncategorized'
        const current = map.get(cat) || 0
        map.set(cat, current + item.subtotal)
      })
    })

    const total = kpiSummary.value.totalGrossRevenue || 1
    return Array.from(map.entries()).map(([category, revenue]) => ({
      category,
      revenue,
      percentage: Math.round((revenue / total) * 100)
    })).sort((a, b) => b.revenue - a.revenue)
  })

  return {
    timeRange,
    rangeFilteredOrders,
    paidOrders,
    pendingOrders,
    cancelledOrders,
    kpiSummary,
    chartWidth,
    chartHeight,
    padding,
    maxRevenue,
    chartPoints,
    svgLinePath,
    svgAreaPath,
    topSellingProducts,
    categorySales
  }
}
