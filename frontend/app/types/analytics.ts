export type TimeRange = 'ALL' | '7D' | '30D'

export interface TimelinePoint {
  date: string
  label: string
  revenue: number
  orderCount: number
}

export interface ChartPoint extends TimelinePoint {
  x: number
  y: number
}

export interface ProductRank {
  name: string
  category: string
  unitsSold: number
  totalRevenue: number
  percentOfTotal: number
}

export interface CategorySales {
  category: string
  revenue: number
  percentage: number
}

export interface KpiSummary {
  totalGrossRevenue: number
  paidOrdersCount: number
  averageOrderValue: number
  totalUnitsSold: number
  successRate: number
  pendingCount: number
  cancelledCount: number
}
