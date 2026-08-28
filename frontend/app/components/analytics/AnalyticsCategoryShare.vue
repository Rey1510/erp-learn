<script setup lang="ts">
import type { CategorySales } from '~/types/analytics'

defineProps<{
  categories: CategorySales[]
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
    <div class="flex items-center justify-between">
      <h3 class="font-bold text-sm flex items-center gap-2" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
        <span>🏷️</span> Kontribusi Omset per Kategori
      </h3>
      <span class="text-xs text-slate-400">{{ categories.length }} Kategori Aktif</span>
    </div>

    <div class="space-y-3">
      <div 
        v-for="(cat, idx) in categories" 
        :key="cat.category"
        class="p-3 rounded-xl border space-y-1.5 transition-colors"
        :class="theme === 'light' ? 'bg-slate-50 border-slate-200' : 'bg-slate-950/60 border-slate-800'"
      >
        <div class="flex items-center justify-between text-xs">
          <span class="font-bold" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">{{ cat.category }}</span>
          <span class="font-mono text-emerald-500 font-semibold">{{ formatRupiah(cat.revenue) }} ({{ cat.percentage }}%)</span>
        </div>
        <!-- Progress Bar -->
        <div class="w-full h-2 rounded-full overflow-hidden" :class="theme === 'light' ? 'bg-slate-200' : 'bg-slate-800'">
          <div 
            class="h-full bg-gradient-to-r transition-all duration-500 rounded-full"
            :class="idx === 0 ? 'from-indigo-500 to-violet-500' : idx === 1 ? 'from-emerald-500 to-teal-500' : 'from-amber-500 to-orange-500'"
            :style="{ width: `${cat.percentage}%` }"
          ></div>
        </div>
      </div>

      <div v-if="categories.length === 0" class="py-6 text-center text-slate-400 text-xs">
        Belum ada data penjualan kategori.
      </div>
    </div>
  </section>
</template>
