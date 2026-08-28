<script setup lang="ts">
defineProps<{
  totalCount: number
  totalCategories: number
  totalValuationFormatted: string
  lowStockCount: number
  outOfStockCount: number
}>()``
</script>

<template>
  <section class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
    <div class="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-sm hover:border-slate-700 transition">
      <p class="text-xs font-medium text-slate-400 uppercase tracking-wider">Total Item</p>
      <p class="text-2xl font-extrabold text-white mt-2">{{ totalCount }} <span class="text-sm font-normal text-slate-400">SKU</span></p>
      <p class="text-xs text-slate-500 mt-1">{{ totalCategories }} Kategori Aktif</p>
    </div>

    <div class="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-sm hover:border-slate-700 transition">
      <p class="text-xs font-medium text-slate-400 uppercase tracking-wider">Total Nilai Stok</p>
      <p class="text-2xl font-extrabold text-indigo-300 mt-2">{{ totalValuationFormatted }}</p>
      <p class="text-xs text-slate-500 mt-1">Estimasi inventory aset</p>
    </div>

    <div class="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-sm hover:border-slate-700 transition">
      <p class="text-xs font-medium text-slate-400 uppercase tracking-wider">Stok Menipis (&le; 5)</p>
      <p class="text-2xl font-extrabold text-amber-400 mt-2">{{ lowStockCount }} <span class="text-sm font-normal text-slate-400">Item</span></p>
      <p class="text-xs text-amber-500/80 mt-1">Perlu restock segera</p>
    </div>

    <div class="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-sm hover:border-slate-700 transition">
      <p class="text-xs font-medium text-slate-400 uppercase tracking-wider">Stok Habis</p>
      <p class="text-2xl font-extrabold text-rose-400 mt-2">{{ outOfStockCount }} <span class="text-sm font-normal text-slate-400">Item</span></p>
      <p class="text-xs text-rose-500/80 mt-1">Status out of stock</p>
    </div>
  </section>
</template>
