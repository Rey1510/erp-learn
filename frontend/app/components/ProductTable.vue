<script setup lang="ts">
import type { Product } from '~/types/product'
import type { SortDirection } from '~/types/pagination'

const props = withDefaults(defineProps<{
  products: Product[]
  pending: boolean
  categories: string[]
  formatRupiah: (val: number) => string
  isAdmin?: boolean
  sortBy?: string
  sortDirection?: SortDirection
  page?: number
  pageSize?: number
  totalElements?: number
  totalPages?: number
  isFirst?: boolean
  isLast?: boolean
}>(), {
  isAdmin: true,
  sortBy: 'id',
  sortDirection: 'desc',
  page: 0,
  pageSize: 10,
  totalElements: 0,
  totalPages: 1,
  isFirst: true,
  isLast: true
})

const emit = defineEmits<{
  (e: 'edit', product: Product): void
  (e: 'delete', id: number): void
  (e: 'bulkDelete', ids: number[]): void
  (e: 'viewHistory', product: Product): void
  (e: 'sort', field: string): void
  (e: 'setPage', page: number): void
  (e: 'nextPage'): void
  (e: 'prevPage'): void
  (e: 'setPageSize', size: number): void
}>()

const { t } = useI18n()
const { theme } = useTheme()

const searchQuery = defineModel<string>('searchQuery', { default: '' })
const selectedCategory = defineModel<string>('selectedCategory', { default: 'ALL' })
const selectedStatus = defineModel<string>('selectedStatus', { default: 'ALL' })

// Multi-select state
const selectedIds = ref<number[]>([])

const isAllCurrentPageSelected = computed(() => {
  if (props.products.length === 0) return false
  return props.products.every(p => selectedIds.value.includes(p.id))
})

function toggleSelectAll() {
  if (isAllCurrentPageSelected.value) {
    const pageIds = props.products.map(p => p.id)
    selectedIds.value = selectedIds.value.filter(id => !pageIds.includes(id))
  } else {
    const pageIds = props.products.map(p => p.id)
    const newSelection = new Set([...selectedIds.value, ...pageIds])
    selectedIds.value = Array.from(newSelection)
  }
}

function toggleSelect(id: number) {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  } else {
    selectedIds.value.push(id)
  }
}

function handleBulkDeleteSubmit() {
  if (selectedIds.value.length === 0) return
  if (confirm(`Yakin ingin menghapus ${selectedIds.value.length} produk yang dipilih secara permanen?`)) {
    emit('bulkDelete', [...selectedIds.value])
    selectedIds.value = []
  }
}

function getSortIcon(field: string) {
  if (props.sortBy !== field) return '⇅'
  return props.sortDirection === 'asc' ? '▲' : '▼'
}
</script>

<template>
  <section 
    id="product-table-section" 
    class="border rounded-2xl overflow-hidden shadow-xl scroll-mt-20 relative transition-colors"
    :class="theme === 'light' 
      ? 'bg-white border-slate-200 shadow-slate-200/50' 
      : 'bg-slate-900/60 border-slate-800/80'"
  >
    <!-- Filter Bar -->
    <div 
      class="p-4 sm:p-5 border-b flex flex-col md:flex-row gap-3 items-center justify-between transition-colors"
      :class="theme === 'light' ? 'bg-slate-50/70 border-slate-200' : 'bg-slate-900/40 border-slate-800/80'"
    >
      <!-- Search input -->
      <div class="relative w-full md:w-80">
        <input
          v-model="searchQuery"
          type="text"
          :placeholder="t('home.searchPlaceholder')"
          class="w-full border rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
          :class="theme === 'light' 
            ? 'bg-white border-slate-300 text-slate-900 placeholder-slate-400' 
            : 'bg-slate-950/80 border-slate-700/80 text-slate-200 placeholder-slate-500'"
        />
        <span v-if="searchQuery" @click="searchQuery = ''" class="absolute right-3 top-2.5 text-xs text-slate-400 hover:text-slate-600 cursor-pointer">&times;</span>
      </div>

      <!-- Dropdowns -->
      <div class="flex flex-wrap items-center gap-3 w-full md:w-auto">
        <div class="flex items-center gap-2 text-xs" :class="theme === 'light' ? 'text-slate-600' : 'text-slate-400'">
          <span>{{ t('home.categoryFilter') }}</span>
          <select
            v-model="selectedCategory"
            class="border rounded-lg px-3 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer"
            :class="theme === 'light' 
              ? 'bg-white border-slate-300 text-slate-800' 
              : 'bg-slate-950/80 border-slate-700/80 text-slate-200'"
          >
            <option v-for="cat in categories" :key="cat" :value="cat">{{ cat === 'ALL' ? t('home.allCategories') : cat }}</option>
          </select>
        </div>

        <div class="flex items-center gap-2 text-xs" :class="theme === 'light' ? 'text-slate-600' : 'text-slate-400'">
          <span>{{ t('home.statusFilter') }}</span>
          <select
            v-model="selectedStatus"
            class="border rounded-lg px-3 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer"
            :class="theme === 'light' 
              ? 'bg-white border-slate-300 text-slate-800' 
              : 'bg-slate-950/80 border-slate-700/80 text-slate-200'"
          >
            <option value="ALL">{{ t('home.allStatus') }}</option>
            <option value="IN_STOCK">{{ t('home.inStock') }}</option>
            <option value="LOW_STOCK">{{ t('home.lowStock') }}</option>
            <option value="OUT_OF_STOCK">{{ t('home.outOfStock') }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Bulk Action Floating Bar (Admin only) -->
    <Transition
      v-if="isAdmin"
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="transform -translate-y-2 opacity-0"
      enter-to-class="transform translate-y-0 opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="transform translate-y-0 opacity-100"
      leave-to-class="transform -translate-y-2 opacity-0"
    >
      <div 
        v-if="selectedIds.length > 0" 
        class="px-6 py-3 border-b flex items-center justify-between text-xs backdrop-blur-md transition-colors"
        :class="theme === 'light' 
          ? 'bg-indigo-50 border-indigo-200 text-indigo-950' 
          : 'bg-indigo-950/90 border-indigo-500/40 text-white'"
      >
        <div class="flex items-center gap-2 font-medium">
          <span class="w-2 h-2 rounded-full bg-indigo-500 animate-ping"></span>
          <span><strong>{{ selectedIds.length }}</strong> {{ t('home.bulkSelected') }}</span>
        </div>
        <div class="flex items-center gap-3">
          <button 
            @click="selectedIds = []" 
            class="text-slate-500 hover:text-slate-800 transition cursor-pointer"
          >
            {{ t('home.cancelSelection') }}
          </button>
          <button 
            @click="handleBulkDeleteSubmit"
            class="px-3.5 py-1.5 bg-rose-600 hover:bg-rose-500 text-white rounded-lg font-semibold shadow-md shadow-rose-600/30 transition cursor-pointer flex items-center gap-1.5 active:scale-95"
          >
            <span>🗑️</span> {{ t('home.bulkDeleteBtn') }} ({{ selectedIds.length }})
          </button>
        </div>
      </div>
    </Transition>

    <!-- Table View -->
    <div class="overflow-x-auto">
      <table class="w-full text-left text-sm" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
        <thead 
          class="text-xs uppercase border-b select-none transition-colors"
          :class="theme === 'light' ? 'bg-slate-100/90 text-slate-600 border-slate-200' : 'bg-slate-950/60 text-slate-400 border-slate-800'"
        >
          <tr>
            <!-- Checkbox Select All (Admin Only) -->
            <th v-if="isAdmin" scope="col" class="px-4 py-3.5 w-10 text-center whitespace-nowrap">
              <input 
                type="checkbox" 
                :checked="isAllCurrentPageSelected" 
                @change="toggleSelectAll"
                class="w-4 h-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer accent-indigo-600"
                title="Select All"
              />
            </th>

            <!-- Sortable: Name -->
            <th 
              scope="col" 
              @click="emit('sort', 'name')" 
              class="px-5 py-3.5 font-semibold cursor-pointer transition group whitespace-nowrap"
              :class="theme === 'light' ? 'hover:text-slate-900' : 'hover:text-white'"
            >
              <div class="flex items-center gap-1.5">
                <span>SKU &amp; {{ t('home.title').split('&')[0] }}</span>
                <span class="text-xs transition-colors" :class="sortBy === 'name' ? 'text-indigo-500 font-bold' : 'text-slate-400'">
                  {{ getSortIcon('name') }}
                </span>
              </div>
            </th>

            <!-- Sortable: Category -->
            <th 
              scope="col" 
              @click="emit('sort', 'category')" 
              class="px-5 py-3.5 font-semibold cursor-pointer transition group whitespace-nowrap"
              :class="theme === 'light' ? 'hover:text-slate-900' : 'hover:text-white'"
            >
              <div class="flex items-center gap-1.5">
                <span>{{ t('home.categories') }}</span>
                <span class="text-xs transition-colors" :class="sortBy === 'category' ? 'text-indigo-500 font-bold' : 'text-slate-400'">
                  {{ getSortIcon('category') }}
                </span>
              </div>
            </th>

            <!-- Sortable: Price -->
            <th 
              scope="col" 
              @click="emit('sort', 'price')" 
              class="px-5 py-3.5 font-semibold text-right cursor-pointer transition group whitespace-nowrap"
              :class="theme === 'light' ? 'hover:text-slate-900' : 'hover:text-white'"
            >
              <div class="flex items-center justify-end gap-1.5">
                <span>Harga</span>
                <span class="text-xs transition-colors" :class="sortBy === 'price' ? 'text-indigo-500 font-bold' : 'text-slate-400'">
                  {{ getSortIcon('price') }}
                </span>
              </div>
            </th>

            <!-- Sortable: Stock -->
            <th 
              scope="col" 
              @click="emit('sort', 'stock')" 
              class="px-5 py-3.5 font-semibold text-center cursor-pointer transition group whitespace-nowrap"
              :class="theme === 'light' ? 'hover:text-slate-900' : 'hover:text-white'"
            >
              <div class="flex items-center justify-center gap-1.5">
                <span>Stok</span>
                <span class="text-xs transition-colors" :class="sortBy === 'stock' ? 'text-indigo-500 font-bold' : 'text-slate-400'">
                  {{ getSortIcon('stock') }}
                </span>
              </div>
            </th>

            <th scope="col" class="px-5 py-3.5 font-semibold text-center whitespace-nowrap">Status</th>
            <th scope="col" class="px-5 py-3.5 font-semibold text-right whitespace-nowrap" :class="isAdmin ? 'min-w-[210px]' : 'min-w-[120px]'">
              {{ t('home.actions') }}
            </th>
          </tr>
        </thead>
        <tbody class="divide-y" :class="theme === 'light' ? 'divide-slate-200' : 'divide-slate-800/60'">
          <tr v-if="pending">
            <td :colspan="isAdmin ? 7 : 6" class="px-6 py-12 text-center text-slate-400">
              <div class="inline-flex items-center gap-2">
                <span class="w-4 h-4 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></span>
                Memuat data...
              </div>
            </td>
          </tr>

          <tr 
            v-else
            v-for="p in products" 
            :key="p.id" 
            class="transition-colors"
            :class="selectedIds.includes(p.id) 
              ? (theme === 'light' ? 'bg-indigo-50/80' : 'bg-indigo-950/20') 
              : (theme === 'light' ? 'hover:bg-slate-50' : 'hover:bg-slate-800/30')"
          >
            <!-- Row Checkbox (Admin Only) -->
            <td v-if="isAdmin" class="px-4 py-4 text-center whitespace-nowrap">
              <input 
                type="checkbox" 
                :checked="selectedIds.includes(p.id)" 
                @change="toggleSelect(p.id)"
                class="w-4 h-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer accent-indigo-600"
              />
            </td>

            <td class="px-5 py-4 whitespace-nowrap">
              <div class="font-semibold" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">{{ p.name }}</div>
              <div class="text-xs font-mono mt-0.5" :class="theme === 'light' ? 'text-indigo-600' : 'text-indigo-400/80'">{{ p.sku }}</div>
            </td>

            <td class="px-5 py-4 whitespace-nowrap">
              <span 
                class="inline-block px-2 py-0.5 rounded-md text-xs font-medium border"
                :class="theme === 'light' ? 'bg-slate-100 text-slate-700 border-slate-200' : 'bg-slate-800 text-slate-300 border-slate-700'"
              >
                {{ p.category }}
              </span>
            </td>

            <td class="px-5 py-4 text-right font-medium whitespace-nowrap" :class="theme === 'light' ? 'text-slate-900' : 'text-slate-200'">
              {{ formatRupiah(p.price) }}
            </td>

            <td class="px-5 py-4 text-center font-semibold whitespace-nowrap">
              <span :class="p.stock === 0 ? 'text-rose-500' : p.stock <= 5 ? 'text-amber-500' : (theme === 'light' ? 'text-slate-800' : 'text-slate-200')">
                {{ p.stock }}
              </span>
            </td>

            <td class="px-5 py-4 text-center whitespace-nowrap">
              <span 
                v-if="p.status === 'IN_STOCK'" 
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-500 border border-emerald-500/20 whitespace-nowrap"
              >
                {{ t('home.inStock') }}
              </span>
              <span 
                v-else-if="p.status === 'LOW_STOCK'" 
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-500/10 text-amber-500 border border-amber-500/20 whitespace-nowrap"
              >
                {{ t('home.lowStock') }}
              </span>
              <span 
                v-else 
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-500/10 text-rose-500 border border-rose-500/20 whitespace-nowrap"
              >
                {{ t('home.outOfStock') }}
              </span>
            </td>

            <td class="px-5 py-4 text-right whitespace-nowrap" :class="isAdmin ? 'min-w-[210px]' : 'min-w-[120px]'">
              <div class="inline-flex items-center gap-2 shrink-0 justify-end">
                <button 
                  @click="emit('viewHistory', p)"
                  class="px-2.5 py-1 text-xs font-medium text-emerald-500 hover:bg-emerald-50 rounded-lg border border-emerald-500/30 transition cursor-pointer flex items-center gap-1 shrink-0"
                  :title="isAdmin ? 'Lihat riwayat mutasi stok & restock' : 'Lihat riwayat mutasi stok'"
                >
                  <span>📜</span> {{ t('home.history') }}
                </button>
                <template v-if="isAdmin">
                  <button 
                    @click="emit('edit', p)"
                    class="px-2.5 py-1 text-xs font-medium text-indigo-500 hover:bg-indigo-50 rounded-lg border border-indigo-500/30 transition cursor-pointer shrink-0"
                  >
                    {{ t('home.edit') }}
                  </button>
                  <button 
                    @click="emit('delete', p.id)"
                    class="px-2.5 py-1 text-xs font-medium text-rose-500 hover:bg-rose-50 rounded-lg border border-rose-500/30 transition cursor-pointer shrink-0"
                  >
                    {{ t('home.delete') }}
                  </button>
                </template>
              </div>
            </td>
          </tr>

          <!-- Empty state -->
          <tr v-if="!pending && products.length === 0">
            <td :colspan="isAdmin ? 7 : 6" class="px-6 py-12 text-center text-slate-400">
              {{ t('home.emptyTable') }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Server-Side Pagination Bar -->
    <PaginationControls 
      :page="page"
      :page-size="pageSize"
      :total-elements="totalElements"
      :total-pages="totalPages"
      :is-first="isFirst"
      :is-last="isLast"
      @set-page="emit('setPage', $event)"
      @next-page="emit('nextPage')"
      @prev-page="emit('prevPage')"
      @set-page-size="emit('setPageSize', $event)"
    />
  </section>
</template>
