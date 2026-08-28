<script setup lang="ts">
const props = defineProps<{
  page: number
  pageSize: number
  totalElements: number
  totalPages: number
  isFirst: boolean
  isLast: boolean
}>()

const emit = defineEmits<{
  (e: 'setPage', page: number): void
  (e: 'nextPage'): void
  (e: 'prevPage'): void
  (e: 'setPageSize', size: number): void
}>()

const { t, locale } = useI18n()
const { theme } = useTheme()

const startRecord = computed(() => {
  if (props.totalElements === 0) return 0
  return props.page * props.pageSize + 1
})

const endRecord = computed(() => {
  return Math.min((props.page + 1) * props.pageSize, props.totalElements)
})

// Generate visible page numbers with smart ellipsis
const visiblePages = computed(() => {
  const total = props.totalPages
  const current = props.page // 0-based
  const pages: (number | string)[] = []

  if (total <= 7) {
    for (let i = 0; i < total; i++) pages.push(i)
  } else {
    pages.push(0)
  }
  return pages.length > 0 ? (total <= 7 ? pages : buildEllipsisPages(current, total)) : []
})

function buildEllipsisPages(current: number, total: number): (number | string)[] {
  const pages: (number | string)[] = [0]
  if (current > 2) pages.push('...')

  const start = Math.max(1, current - 1)
  const end = Math.min(total - 2, current + 1)

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }

  if (current < total - 3) pages.push('...')
  pages.push(total - 1)
  return pages
}
</script>

<template>
  <div 
    class="px-5 py-4 border-t flex flex-col sm:flex-row items-center justify-between gap-4 text-xs transition-colors"
    :class="theme === 'light' 
      ? 'bg-slate-50/90 border-slate-200 text-slate-700' 
      : 'bg-slate-950/60 border-slate-800 text-slate-400'"
  >
    <!-- Left: Page Size Selector & Record Count -->
    <div class="flex items-center gap-3" :class="theme === 'light' ? 'text-slate-600' : 'text-slate-400'">
      <div class="flex items-center gap-1.5">
        <span>{{ locale === 'id' ? 'Baris per halaman:' : 'Rows per page:' }}</span>
        <select 
          :value="pageSize"
          @change="emit('setPageSize', Number(($event.target as HTMLSelectElement).value))"
          class="border rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer transition font-medium"
          :class="theme === 'light' 
            ? 'bg-white border-slate-300 text-slate-800 shadow-sm' 
            : 'bg-slate-900 border-slate-700 text-white'"
        >
          <option :value="5">5</option>
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </div>

      <span class="hidden sm:inline" :class="theme === 'light' ? 'text-slate-300' : 'text-slate-600'">&middot;</span>

      <span>
        {{ locale === 'id' ? 'Menampilkan' : 'Showing' }} 
        <strong :class="theme === 'light' ? 'text-slate-900 font-bold' : 'text-white font-medium'">{{ startRecord }}-{{ endRecord }}</strong> 
        {{ locale === 'id' ? 'dari' : 'of' }} 
        <strong :class="theme === 'light' ? 'text-slate-900 font-bold' : 'text-white font-medium'">{{ totalElements }}</strong> 
        items
      </span>
    </div>

    <!-- Right: Page Navigation Buttons -->
    <div class="flex items-center gap-1">
      <!-- First Page -->
      <button 
        @click="emit('setPage', 0)"
        :disabled="isFirst"
        class="p-1.5 rounded-lg border disabled:opacity-30 disabled:pointer-events-none transition cursor-pointer"
        :class="theme === 'light' 
          ? 'bg-white hover:bg-slate-100 text-slate-700 border-slate-300 shadow-sm' 
          : 'border-slate-800 hover:bg-slate-800 text-slate-300'"
        title="First Page"
      >
        &laquo;
      </button>

      <!-- Prev Page -->
      <button 
        @click="emit('prevPage')"
        :disabled="isFirst"
        class="px-2.5 py-1.5 rounded-lg border disabled:opacity-30 disabled:pointer-events-none transition cursor-pointer flex items-center gap-1"
        :class="theme === 'light' 
          ? 'bg-white hover:bg-slate-100 text-slate-700 border-slate-300 shadow-sm font-medium' 
          : 'border-slate-800 hover:bg-slate-800 text-slate-300'"
      >
        &lsaquo; <span class="hidden sm:inline">Prev</span>
      </button>

      <!-- Numbered Page Buttons -->
      <template v-for="(p, idx) in visiblePages" :key="idx">
        <span v-if="p === '...'" class="px-2 text-slate-400 select-none">...</span>
        <button 
          v-else
          @click="emit('setPage', p as number)"
          class="w-7 h-7 rounded-lg text-xs font-semibold transition cursor-pointer flex items-center justify-center"
          :class="page === p 
            ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30' 
            : (theme === 'light' 
                ? 'bg-white hover:bg-slate-100 text-slate-700 border border-slate-300 shadow-sm' 
                : 'text-slate-400 hover:text-white hover:bg-slate-800/80 border border-slate-800')"
        >
          {{ (p as number) + 1 }}
        </button>
      </template>

      <!-- Next Page -->
      <button 
        @click="emit('nextPage')"
        :disabled="isLast"
        class="px-2.5 py-1.5 rounded-lg border disabled:opacity-30 disabled:pointer-events-none transition cursor-pointer flex items-center gap-1"
        :class="theme === 'light' 
          ? 'bg-white hover:bg-slate-100 text-slate-700 border-slate-300 shadow-sm font-medium' 
          : 'border-slate-800 hover:bg-slate-800 text-slate-300'"
      >
        <span class="hidden sm:inline">Next</span> &rsaquo;
      </button>

      <!-- Last Page -->
      <button 
        @click="emit('setPage', totalPages - 1)"
        :disabled="isLast"
        class="p-1.5 rounded-lg border disabled:opacity-30 disabled:pointer-events-none transition cursor-pointer"
        :class="theme === 'light' 
          ? 'bg-white hover:bg-slate-100 text-slate-700 border-slate-300 shadow-sm' 
          : 'border-slate-800 hover:bg-slate-800 text-slate-300'"
        title="Last Page"
      >
        &raquo;
      </button>
    </div>
  </div>
</template>
