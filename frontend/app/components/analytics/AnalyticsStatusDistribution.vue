<script setup lang="ts">
import type { Order } from '~/types/order'

const props = defineProps<{
  paidOrders: Order[]
  pendingOrders: Order[]
  cancelledOrders: Order[]
  totalOrdersCount: number
  successRate: number
}>()

const { theme } = useTheme()

const pendingRate = computed(() => {
  if (props.totalOrdersCount === 0) return 0
  return Math.round((props.pendingOrders.length / props.totalOrdersCount) * 100)
})

const cancelledRate = computed(() => {
  if (props.totalOrdersCount === 0) return 0
  return Math.round((props.cancelledOrders.length / props.totalOrdersCount) * 100)
})
</script>

<template>
  <section 
    class="p-5 rounded-2xl border shadow-xl space-y-4 transition-colors"
    :class="theme === 'light' 
      ? 'bg-white border-slate-200 shadow-slate-200/50' 
      : 'bg-slate-900/70 border-slate-800/80'"
  >
    <h3 class="font-bold text-sm flex items-center gap-2" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
      <span>🔄</span> Distribusi Status Transaksi
    </h3>

    <div class="grid grid-cols-3 gap-2.5 text-center">
      <!-- Paid -->
      <div 
        class="p-3 rounded-xl border transition-colors"
        :class="theme === 'light' ? 'bg-emerald-50/80 border-emerald-200 text-emerald-800' : 'bg-emerald-950/30 border-emerald-500/20'"
      >
        <p class="text-[11px] font-semibold uppercase" :class="theme === 'light' ? 'text-emerald-700' : 'text-emerald-400'">Lunas (PAID)</p>
        <p class="text-xl font-extrabold mt-1" :class="theme === 'light' ? 'text-emerald-950' : 'text-white'">{{ paidOrders.length }}</p>
        <p class="text-[10px]" :class="theme === 'light' ? 'text-emerald-600' : 'text-emerald-400/80'">{{ successRate }}%</p>
      </div>

      <!-- Pending -->
      <div 
        class="p-3 rounded-xl border transition-colors"
        :class="theme === 'light' ? 'bg-amber-50/80 border-amber-200 text-amber-800' : 'bg-amber-950/30 border-amber-500/20'"
      >
        <p class="text-[11px] font-semibold uppercase" :class="theme === 'light' ? 'text-amber-700' : 'text-amber-400'">Pending</p>
        <p class="text-xl font-extrabold mt-1" :class="theme === 'light' ? 'text-amber-950' : 'text-white'">{{ pendingOrders.length }}</p>
        <p class="text-[10px]" :class="theme === 'light' ? 'text-amber-600' : 'text-amber-400/80'">{{ pendingRate }}%</p>
      </div>

      <!-- Cancelled -->
      <div 
        class="p-3 rounded-xl border transition-colors"
        :class="theme === 'light' ? 'bg-rose-50/80 border-rose-200 text-rose-800' : 'bg-rose-950/30 border-rose-500/20'"
      >
        <p class="text-[11px] font-semibold uppercase" :class="theme === 'light' ? 'text-rose-700' : 'text-rose-400'">Batal</p>
        <p class="text-xl font-extrabold mt-1" :class="theme === 'light' ? 'text-rose-950' : 'text-white'">{{ cancelledOrders.length }}</p>
        <p class="text-[10px]" :class="theme === 'light' ? 'text-rose-600' : 'text-rose-400/80'">{{ cancelledRate }}%</p>
      </div>
    </div>
  </section>
</template>
