<script setup lang="ts">
import type { ProductRank } from '~/types/analytics'

defineProps<{
  products: ProductRank[]
  formatRupiah: (val: number) => string
}>()

const { theme } = useTheme()
</script>

<template>
  <section 
    class="p-5 rounded-2xl border shadow-xl space-y-4 transition-colors"
    :class="theme === 'light' 
      ? 'bg-white border-slate-200 shadow-slate-200/50' 
      : 'bg-slate-900/70 border-slate-800/80'"
  >
    <div class="flex items-center justify-between pb-3 border-b" :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'">
      <h3 class="font-bold text-sm flex items-center gap-2" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
        <span>🏆</span> Top 5 Produk Terlaris
      </h3>
      <span class="text-xs text-indigo-500 font-semibold">By Revenue</span>
    </div>

    <div class="space-y-3">
      <div 
        v-for="(prod, idx) in products" 
        :key="prod.name"
        class="p-3.5 rounded-xl border flex items-center justify-between gap-3 transition"
        :class="theme === 'light' 
          ? 'bg-slate-50 border-slate-200 hover:border-slate-300' 
          : 'bg-slate-950/80 border-slate-800/80 hover:border-slate-700'"
      >
        <div class="flex items-center gap-3 min-w-0">
          <!-- Rank Medal Badge -->
          <div 
            class="w-7 h-7 rounded-lg flex items-center justify-center font-bold text-xs shrink-0"
            :class="idx === 0 
              ? 'bg-amber-500/20 text-amber-500 border border-amber-500/40' 
              : idx === 1 
                ? (theme === 'light' ? 'bg-slate-200 text-slate-700 border border-slate-300' : 'bg-slate-700/40 text-slate-200 border border-slate-600') 
                : idx === 2 
                  ? 'bg-amber-800/30 text-amber-500 border border-amber-700/40' 
                  : (theme === 'light' ? 'bg-slate-100 text-slate-500 border border-slate-200' : 'bg-slate-900 text-slate-400 border border-slate-800')"
          >
            #{{ idx + 1 }}
          </div>

          <div class="min-w-0">
            <p class="font-semibold text-xs truncate" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">{{ prod.name }}</p>
            <p class="text-[11px] text-slate-400 flex items-center gap-1.5 mt-0.5">
              <span 
                class="px-1.5 py-0.2 rounded text-[10px] border"
                :class="theme === 'light' ? 'bg-slate-200 text-slate-700 border-slate-300' : 'bg-slate-800 text-slate-300 border-slate-700'"
              >
                {{ prod.category }}
              </span>
              <span>&middot;</span>
              <span class="text-indigo-500 font-medium">{{ prod.unitsSold }} unit terjual</span>
            </p>
          </div>
        </div>

        <!-- Revenue Contribution -->
        <div class="text-right shrink-0">
          <p class="font-bold text-xs text-emerald-500">{{ formatRupiah(prod.totalRevenue) }}</p>
          <p class="text-[10px] text-slate-400">{{ prod.percentOfTotal }}% total omset</p>
        </div>
      </div>

      <div v-if="products.length === 0" class="py-8 text-center text-slate-400 text-xs">
        Belum ada data produk terjual.
      </div>
    </div>
  </section>
</template>
