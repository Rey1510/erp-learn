<script setup lang="ts">
import type { Product, ProductFormData } from '~/types/product'

// Auth & Preferences
const { isAdmin, isCashier, user } = useAuth()
const { t } = useI18n()
const { theme } = useTheme()

// Composable Products
const {
  products,
  allProducts,
  pending,
  error,
  refresh,
  page,
  pageSize,
  sortBy,
  sortDirection,
  totalElements,
  totalPages,
  isFirst,
  isLast,
  searchQuery,
  selectedCategory,
  selectedStatus,
  categories,
  filteredProducts,
  totalValuation,
  lowStockCount,
  outOfStockCount,
  setPage,
  nextPage,
  prevPage,
  setPageSize,
  toggleSort,
  createProduct,
  updateProduct,
  deleteProduct,
  bulkDeleteProducts,
  formatRupiah
} = useProducts()

// Composable CSV Export / Import
const { downloadProductsCsv, isExporting } = useExportImport()

// State Modal Form
const isModalOpen = ref(false)
const isEditing = ref(false)
const isSubmitting = ref(false)
const editingProduct = ref<Product | null>(null)

// State Stock History Modal
const isHistoryModalOpen = ref(false)
const selectedProductForHistory = ref<Product | null>(null)

// State CSV Import Modal
const isImportModalOpen = ref(false)

// State Toast Notification
const toastMessage = ref('')
const showToast = ref(false)

function triggerToast(msg: string) {
  toastMessage.value = msg
  showToast.value = true
  setTimeout(() => { showToast.value = false }, 3000)
}

function handleAdd() {
  if (!isAdmin.value) return
  isEditing.value = false
  editingProduct.value = null
  isModalOpen.value = true
}

function handleEdit(product: Product) {
  if (!isAdmin.value) return
  isEditing.value = true
  editingProduct.value = product
  isModalOpen.value = true
}

function handleViewHistory(product: Product) {
  selectedProductForHistory.value = product
  isHistoryModalOpen.value = true
}

function handleRestocked() {
  refresh()
  triggerToast('Stok berhasil diperbarui & dicatat ke audit log!')
}

function handleImported() {
  refresh()
  triggerToast('Katalog produk berhasil diimport dari file CSV!')
}

async function handleSubmit(payload: ProductFormData) {
  try {
    isSubmitting.value = true
    if (isEditing.value && editingProduct.value) {
      await updateProduct(editingProduct.value.id, payload)
      triggerToast(`Produk "${payload.name}" berhasil diupdate!`)
    } else {
      await createProduct(payload)
      triggerToast(`Produk baru "${payload.name}" tersimpan di database!`)
    }
    isModalOpen.value = false
  } catch (err: any) {
    alert('Gagal menyimpan produk: ' + (err.message || err))
  } finally {
    isSubmitting.value = false
  }
}

async function handleDelete(id: number) {
  if (!isAdmin.value) return
  const target = products.value?.find(p => p.id === id)
  if (confirm(`Yakin ingin menghapus ${target?.name || 'produk ini'}?`)) {
    try {
      await deleteProduct(id)
      triggerToast('Produk berhasil dihapus.')
    } catch (err: any) {
      alert('Gagal menghapus produk: ' + (err.message || err))
    }
  }
}

async function handleBulkDelete(ids: number[]) {
  if (!isAdmin.value) return
  try {
    await bulkDeleteProducts(ids)
    triggerToast(`${ids.length} produk berhasil dihapus sekaligus!`)
  } catch (err: any) {
    alert('Gagal menghapus produk terpilih: ' + (err.message || err))
  }
}
</script>

<template>
  <div class="space-y-8">
    <!-- Top Toast Notification -->
    <Transition
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="transform -translate-y-4 opacity-0"
      enter-to-class="transform translate-y-0 opacity-100"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="transform translate-y-0 opacity-100"
      leave-to-class="transform -translate-y-4 opacity-0"
    >
      <div 
        v-if="showToast" 
        class="fixed top-6 right-6 z-50 flex items-center gap-3 px-5 py-3 rounded-xl bg-indigo-600/90 backdrop-blur-md text-white shadow-2xl border border-indigo-400/30 text-sm font-medium"
      >
        <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
        {{ toastMessage }}
      </div>
    </Transition>

    <!-- Banner Info & Role Context Header -->
    <section 
      class="p-5 rounded-2xl border flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 transition-colors"
      :class="theme === 'light' 
        ? 'bg-gradient-to-r from-indigo-50/80 via-white to-white border-indigo-100 shadow-sm' 
        : 'bg-gradient-to-r from-indigo-950/70 via-slate-900 to-slate-900 border-indigo-500/20'"
    >
      <div>
        <div class="flex items-center gap-2">
          <span class="text-xs uppercase tracking-wider font-semibold text-emerald-500">
            {{ t('home.badge') }}
          </span>
          <span 
            class="px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wide border"
            :class="isAdmin ? 'bg-indigo-500/10 text-indigo-500 border-indigo-500/30' : 'bg-emerald-500/10 text-emerald-500 border-emerald-500/30'"
          >
            {{ isAdmin ? t('home.adminMode') : t('home.cashierMode') }}
          </span>
        </div>
        <h2 class="text-lg font-bold mt-1" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
          {{ t('home.title') }}
        </h2>
        <p class="text-xs mt-1 max-w-2xl" :class="theme === 'light' ? 'text-slate-600' : 'text-slate-400'">
          {{ isAdmin ? t('home.adminDesc') : t('home.cashierDesc') }}
        </p>
      </div>

      <!-- Action Buttons Bar -->
      <div class="flex flex-wrap items-center gap-2.5">
        <button 
          @click="downloadProductsCsv" 
          :disabled="isExporting"
          class="px-3.5 py-2 rounded-xl text-xs font-medium border transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          :class="theme === 'light'
            ? 'bg-white hover:bg-slate-50 text-slate-700 border-slate-300 shadow-sm'
            : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'"
          title="Unduh katalog produk dalam format CSV / Excel"
        >
          <span>📊</span>
          <span>{{ isExporting ? '...' : t('home.exportCsv') }}</span>
        </button>

        <template v-if="isAdmin">
          <button 
            @click="isImportModalOpen = true" 
            class="px-3.5 py-2 rounded-xl text-xs font-medium border transition flex items-center gap-1.5 cursor-pointer"
            :class="theme === 'light'
              ? 'bg-white hover:bg-slate-50 text-slate-700 border-slate-300 shadow-sm'
              : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'"
            title="Upload file CSV untuk import produk massal"
          >
            <span>📥</span> {{ t('home.importCsv') }}
          </button>
        </template>

        <button 
          @click="refresh()" 
          class="px-3 py-2 rounded-xl text-xs font-medium border transition cursor-pointer"
          :class="theme === 'light'
            ? 'bg-white hover:bg-slate-50 text-slate-700 border-slate-300 shadow-sm'
            : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'"
          title="Reload data"
        >
          🔄
        </button>

        <template v-if="isAdmin">
          <button 
            @click="handleAdd"
            class="shrink-0 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs sm:text-sm transition shadow-lg shadow-indigo-600/30 hover:shadow-indigo-500/50 flex items-center gap-1.5 cursor-pointer active:scale-95"
          >
            <span class="text-base leading-none">+</span> {{ t('home.addProduct') }}
          </button>
        </template>
      </div>
    </section>

    <!-- Metrics Cards Component -->
    <ProductStatCards
      :total-count="totalElements"
      :total-categories="categories.length - 1"
      :total-valuation-formatted="formatRupiah(totalValuation)"
      :low-stock-count="lowStockCount"
      :out-of-stock-count="outOfStockCount"
    />

    <!-- Table Component with Multi-Select Checkboxes & Server-Side Pagination -->
    <ProductTable
      v-model:search-query="searchQuery"
      v-model:selected-category="selectedCategory"
      v-model:selected-status="selectedStatus"
      :products="filteredProducts"
      :pending="pending"
      :categories="categories"
      :format-rupiah="formatRupiah"
      :is-admin="isAdmin"
      :sort-by="sortBy"
      :sort-direction="sortDirection"
      :page="page"
      :page-size="pageSize"
      :total-elements="totalElements"
      :total-pages="totalPages"
      :is-first="isFirst"
      :is-last="isLast"
      @sort="toggleSort"
      @set-page="setPage"
      @next-page="nextPage"
      @prev-page="prevPage"
      @set-page-size="setPageSize"
      @view-history="handleViewHistory"
      @edit="handleEdit"
      @delete="handleDelete"
      @bulk-delete="handleBulkDelete"
    />

    <!-- Modal Form Component (Admin only) -->
    <ProductFormModal
      v-if="isAdmin"
      :is-open="isModalOpen"
      :is-editing="isEditing"
      :is-submitting="isSubmitting"
      :initial-data="editingProduct"
      @close="isModalOpen = false"
      @submit="handleSubmit"
    />

    <!-- Stock Movement Audit Trail Modal -->
    <StockHistoryModal 
      :is-open="isHistoryModalOpen"
      :product="selectedProductForHistory"
      @close="isHistoryModalOpen = false"
      @restocked="handleRestocked"
    />

    <!-- CSV Bulk Import Modal (Admin only) -->
    <CsvImportModal
      v-if="isAdmin"
      :is-open="isImportModalOpen"
      @close="isImportModalOpen = false"
      @imported="handleImported"
    />
  </div>
</template>
