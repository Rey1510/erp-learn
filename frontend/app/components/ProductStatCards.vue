<script setup lang="ts">
defineProps<{
  totalCount: number
  totalCategories: number
  totalValuationFormatted: string
  lowStockCount: number
  outOfStockCount: number
}>()

const { t } = useI18n()
const { theme } = useTheme()
</script>

<template>
  <section class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
    <!-- Total Products -->
    <div 
      class="p-5 rounded-2xl border shadow-sm transition"
      :class="theme === 'light' 
        ? 'bg-white border-slate-200 hover:border-slate-300' 
        : 'bg-slate-900/80 border-slate-800 hover:border-slate-700'"
    >
      <p class="text-xs font-medium uppercase tracking-wider text-slate-400">{{ t('home.totalProducts') }}</p>
      <p class="text-2xl font-extrabold mt-2" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
        {{ totalCount }} <span class="text-sm font-normal text-slate-400">SKU</span>
      </p>
      <p class="text-xs text-slate-500 mt-1">{{ totalCategories }} {{ t('home.categories') }}</p>
    </div>

    <!-- Valuation -->
    <div 
      class="p-5 rounded-2xl border shadow-sm transition"
      :class="theme === 'light' 
        ? 'bg-white border-slate-200 hover:border-slate-300' 
        : 'bg-slate-900/80 border-slate-800 hover:border-slate-700'"
    >
      <p class="text-xs font-medium uppercase tracking-wider text-slate-400">{{ t('home.valuation') }}</p>
      <p class="text-2xl font-extrabold text-indigo-500 mt-2">{{ totalValuationFormatted }}</p>
      <p class="text-xs text-slate-500 mt-1">Estimasi inventory aset</p>
    </div>

    <!-- Low Stock -->
    <div 
      class="p-5 rounded-2xl border shadow-sm transition"
      :class="theme === 'light' 
        ? 'bg-white border-slate-200 hover:border-slate-300' 
        : 'bg-slate-900/80 border-slate-800 hover:border-slate-700'"
    >
      <p class="text-xs font-medium uppercase tracking-wider text-slate-400">{{ t('home.lowStock') }} (&le; 5)</p>
      <p class="text-2xl font-extrabold text-amber-500 mt-2">
        {{ lowStockCount }} <span class="text-sm font-normal text-slate-400">Item</span>
      </p>
      <p class="text-xs text-amber-500/80 mt-1">Perlu restock segera</p>
    </div>

    <!-- Out of Stock -->
    <div 
      class="p-5 rounded-2xl border shadow-sm transition"
      :class="theme === 'light' 
        ? 'bg-white border-slate-200 hover:border-slate-300' 
        : 'bg-slate-900/80 border-slate-800 hover:border-slate-700'"
    >
      <p class="text-xs font-medium uppercase tracking-wider text-slate-400">{{ t('home.outOfStock') }}</p>
      <p class="text-2xl font-extrabold text-rose-500 mt-2">
        {{ outOfStockCount }} <span class="text-sm font-normal text-slate-400">Item</span>
      </p>
      <p class="text-xs text-rose-500/80 mt-1">Status 0 stock</p>
    </div>
  </section>
</template>
