<script setup lang="ts">
// 1. Fetch Orders & Products composables
const { allOrders, pending: ordersPending, refresh: refreshOrders, reseedOrders, formatRupiah } = useOrders()
const { refresh: refreshProducts } = useProducts()
const { t, locale } = useI18n()
const { theme } = useTheme()

onMounted(() => {
  refreshOrders()
})

// 2. Encapsulated Analytics Business Logic from useAnalytics composable
const {
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
} = useAnalytics(allOrders)

// 3. Reseed / Cleanse Handler
const isReseeding = ref(false)

async function handleReseed() {
  if (!confirm(locale.value === 'id' 
    ? 'Apakah Anda yakin ingin membersihkan (cleanse) seluruh data transaksi & stok, lalu mengembalikan database ke data awal yang bersih?' 
    : 'Are you sure you want to cleanse all test transactions and reset catalog inventory to clean initial demo data?')) {
    return
  }

  isReseeding.value = true
  try {
    await reseedOrders()
    await Promise.all([refreshOrders(), refreshProducts()])
    alert(locale.value === 'id' 
      ? '✅ Database berhasil dibersihkan & di-reset ke kondisi awal yang bersih!' 
      : '✅ Database successfully cleansed & reset to clean demo state!')
  } catch (err: any) {
    alert('Gagal membersihkan & reset data: ' + (err.message || err))
  } finally {
    isReseeding.value = false
  }
}
</script>

<template>
  <div class="space-y-8">
    <!-- Header Banner & Time Filter Controls -->
    <section 
      class="p-6 rounded-2xl border flex flex-col md:flex-row items-start md:items-center justify-between gap-4 transition-colors"
      :class="theme === 'light' 
        ? 'bg-white border-slate-200 shadow-sm' 
        : 'bg-gradient-to-r from-indigo-950/80 via-slate-900 to-slate-900 border-indigo-500/20'"
    >
      <div>
        <div class="flex items-center gap-2">
          <span 
            class="px-2.5 py-0.5 rounded-full text-xs font-semibold border"
            :class="theme === 'light' ? 'bg-indigo-50 text-indigo-600 border-indigo-200' : 'bg-indigo-500/20 text-indigo-300 border-indigo-500/30'"
          >
            📊 Executive BI Overview
          </span>
          <span class="text-xs text-slate-400">&middot; Clean Architecture Refactored</span>
        </div>
        <h2 class="text-xl font-extrabold mt-1.5" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
          {{ t('analytics.title') }}
        </h2>
        <p class="text-xs mt-1 max-w-2xl" :class="theme === 'light' ? 'text-slate-600' : 'text-slate-400'">
          {{ t('analytics.desc') }}
        </p>
      </div>

      <!-- Time Range & Action Buttons -->
      <div class="flex flex-wrap items-center gap-3">
        <!-- Time Range Selector -->
        <div 
          class="flex items-center p-1 rounded-xl border text-xs"
          :class="theme === 'light' ? 'bg-slate-100 border-slate-200' : 'bg-slate-950/80 border-slate-800'"
        >
          <button 
            @click="timeRange = 'ALL'" 
            class="px-3 py-1.5 rounded-lg font-medium transition cursor-pointer"
            :class="timeRange === 'ALL' 
              ? 'bg-indigo-600 text-white shadow' 
              : (theme === 'light' ? 'text-slate-600 hover:text-slate-900 hover:bg-slate-200/60' : 'text-slate-400 hover:text-white')"
          >
            {{ locale === 'id' ? 'Semua' : 'All Time' }}
          </button>
          <button 
            @click="timeRange = '30D'" 
            class="px-3 py-1.5 rounded-lg font-medium transition cursor-pointer"
            :class="timeRange === '30D' 
              ? 'bg-indigo-600 text-white shadow' 
              : (theme === 'light' ? 'text-slate-600 hover:text-slate-900 hover:bg-slate-200/60' : 'text-slate-400 hover:text-white')"
          >
            30 {{ locale === 'id' ? 'Hari' : 'Days' }}
          </button>
          <button 
            @click="timeRange = '7D'" 
            class="px-3 py-1.5 rounded-lg font-medium transition cursor-pointer"
            :class="timeRange === '7D' 
              ? 'bg-indigo-600 text-white shadow' 
              : (theme === 'light' ? 'text-slate-600 hover:text-slate-900 hover:bg-slate-200/60' : 'text-slate-400 hover:text-white')"
          >
            7 {{ locale === 'id' ? 'Hari' : 'Days' }}
          </button>
        </div>

        <button 
          @click="refreshOrders()" 
          class="px-3.5 py-2 rounded-xl text-xs font-medium border transition cursor-pointer"
          :class="theme === 'light'
            ? 'bg-white hover:bg-slate-50 text-slate-700 border-slate-300 shadow-sm'
            : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'"
        >
          🔄 Refresh
        </button>

        <button 
          @click="handleReseed" 
          :disabled="isReseeding"
          class="px-3.5 py-2 rounded-xl text-xs font-semibold border transition cursor-pointer disabled:opacity-50 flex items-center gap-1.5 active:scale-95"
          :class="theme === 'light'
            ? 'bg-rose-50 hover:bg-rose-100 text-rose-700 border-rose-200 shadow-sm'
            : 'bg-rose-950/40 hover:bg-rose-900/50 text-rose-300 border-rose-500/40'"
          title="Bersihkan seluruh data transaksi uji coba dan pulihkan stok katalog ke data demo awal yang bersih"
        >
          <span>🧹</span> {{ isReseeding ? (locale === 'id' ? 'Membersihkan...' : 'Cleansing...') : (locale === 'id' ? 'Cleanse & Reset Demo' : 'Cleanse & Reset Demo') }}
        </button>
      </div>
    </section>

    <!-- 1. KPI Metric Cards -->
    <AnalyticsKpiCards 
      :kpi="kpiSummary" 
      :format-rupiah="formatRupiah" 
    />

    <!-- 2. Main Analytics Content Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      <!-- Left Column (7 Cols): SVG Revenue Chart & Category Share -->
      <div class="lg:col-span-7 space-y-6">
        <AnalyticsRevenueChart 
          :chart-width="chartWidth"
          :chart-height="chartHeight"
          :padding="padding"
          :max-revenue="maxRevenue"
          :chart-points="chartPoints"
          :svg-line-path="svgLinePath"
          :svg-area-path="svgAreaPath"
          :format-rupiah="formatRupiah"
        />

        <AnalyticsCategoryShare 
          :categories="categorySales"
          :format-rupiah="formatRupiah"
        />
      </div>

      <!-- Right Column (5 Cols): Top Products Leaderboard & Status Breakdown -->
      <div class="lg:col-span-5 space-y-6">
        <AnalyticsTopProducts 
          :products="topSellingProducts"
          :format-rupiah="formatRupiah"
        />

        <AnalyticsStatusDistribution 
          :paid-orders="paidOrders"
          :pending-orders="pendingOrders"
          :cancelled-orders="cancelledOrders"
          :total-orders-count="rangeFilteredOrders.length"
          :success-rate="kpiSummary.successRate"
        />
      </div>
    </div>
  </div>
</template>
