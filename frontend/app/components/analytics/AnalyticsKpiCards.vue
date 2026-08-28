<script setup lang="ts">
import type { KpiSummary } from '~/types/analytics'

defineProps<{
  kpi: KpiSummary
  formatRupiah: (val: number) => string
}>()

const { theme } = useTheme()
</script>

<template>
  <section class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
    <!-- 1. Gross Revenue -->
    <div 
      class="p-5 rounded-2xl border shadow-sm relative overflow-hidden transition"
      :class="theme === 'light' 
        ? 'bg-white border-slate-200 hover:border-emerald-500/40 shadow-slate-200/50' 
        : 'bg-slate-900/80 border-slate-800 hover:border-emerald-500/30'"
    >
      <div class="flex items-center justify-between">
        <p class="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Omset Lunas</p>
        <span class="p-2 rounded-xl text-sm" :class="theme === 'light' ? 'bg-emerald-50 text-emerald-600' : 'bg-emerald-500/10 text-emerald-400'">💰</span>
      </div>
      <p class="text-2xl font-extrabold text-emerald-500 mt-2">
        {{ formatRupiah(kpi.totalGrossRevenue) }}
      </p>
      <p class="text-xs text-slate-500 mt-1 flex items-center gap-1">
        <span class="text-emerald-500 font-semibold">{{ kpi.paidOrdersCount }}</span> transaksi terselesaikan
      </p>
    </div>

    <!-- 2. Average Order Value (AOV) -->
    <div 
      class="p-5 rounded-2xl border shadow-sm relative overflow-hidden transition"
      :class="theme === 'light' 
        ? 'bg-white border-slate-200 hover:border-indigo-500/40 shadow-slate-200/50' 
        : 'bg-slate-900/80 border-slate-800 hover:border-indigo-500/30'"
    >
      <div class="flex items-center justify-between">
        <p class="text-xs font-semibold uppercase tracking-wider text-slate-400">Rata-rata Order (AOV)</p>
        <span class="p-2 rounded-xl text-sm" :class="theme === 'light' ? 'bg-indigo-50 text-indigo-600' : 'bg-indigo-500/10 text-indigo-400'">📈</span>
      </div>
      <p class="text-2xl font-extrabold text-indigo-500 mt-2">
        {{ formatRupiah(kpi.averageOrderValue) }}
      </p>
      <p class="text-xs text-slate-500 mt-1">Per transaksi lunas</p>
    </div>

    <!-- 3. Units Sold -->
    <div 
      class="p-5 rounded-2xl border shadow-sm relative overflow-hidden transition"
      :class="theme === 'light' 
        ? 'bg-white border-slate-200 hover:border-violet-500/40 shadow-slate-200/50' 
        : 'bg-slate-900/80 border-slate-800 hover:border-violet-500/30'"
    >
      <div class="flex items-center justify-between">
        <p class="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Item Terjual</p>
        <span class="p-2 rounded-xl text-sm" :class="theme === 'light' ? 'bg-violet-50 text-violet-600' : 'bg-violet-500/10 text-violet-400'">📦</span>
      </div>
      <p class="text-2xl font-extrabold text-violet-500 mt-2">
        {{ kpi.totalUnitsSold }} <span class="text-sm font-normal text-slate-400">Unit</span>
      </p>
      <p class="text-xs text-slate-500 mt-1">Volume produk terjual</p>
    </div>

    <!-- 4. Conversion Rate -->
    <div 
      class="p-5 rounded-2xl border shadow-sm relative overflow-hidden transition"
      :class="theme === 'light' 
        ? 'bg-white border-slate-200 hover:border-amber-500/40 shadow-slate-200/50' 
        : 'bg-slate-900/80 border-slate-800 hover:border-amber-500/30'"
    >
      <div class="flex items-center justify-between">
        <p class="text-xs font-semibold uppercase tracking-wider text-slate-400">Tingkat Sukses (PAID)</p>
        <span class="p-2 rounded-xl text-sm" :class="theme === 'light' ? 'bg-amber-50 text-amber-600' : 'bg-amber-500/10 text-amber-400'">🎯</span>
      </div>
      <p class="text-2xl font-extrabold text-amber-500 mt-2">
        {{ kpi.successRate }}%
      </p>
      <p class="text-xs text-slate-500 mt-1">
        {{ kpi.pendingCount }} Pending &middot; {{ kpi.cancelledCount }} Batal
      </p>
    </div>
  </section>
</template>
