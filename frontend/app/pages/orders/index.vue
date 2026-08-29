<script setup lang="ts">
import type { Order } from '~/types/order'

const {
  orders,
  allOrders,
  filteredOrders,
  searchQuery,
  selectedStatus,
  pending,
  refresh,
  page,
  pageSize,
  sortBy,
  sortDirection,
  totalElements,
  totalPages,
  isFirst,
  isLast,
  totalRevenue,
  pendingOrdersCount,
  paidOrdersCount,
  setPage,
  nextPage,
  prevPage,
  setPageSize,
  toggleSort,
  updateOrderStatus,
  formatRupiah,
  formatDate
} = useOrders()

const { downloadOrdersCsv, isExporting } = useExportImport()
const { t } = useI18n()
const { theme } = useTheme()

const selectedOrder = ref<Order | null>(null)
const isDetailModalOpen = ref(false)

const selectedReceiptOrder = ref<Order | null>(null)
const isReceiptModalOpen = ref(false)

const selectedSimulatorOrder = ref<Order | null>(null)
const isSimulatorModalOpen = ref(false)

const activeDropdownOrderId = ref<number | null>(null)

const toastMessage = ref('')
const showToast = ref(false)

onMounted(() => {
  refresh()
  if (import.meta.client) {
    window.addEventListener('click', closeDropdown)
  }
})

onUnmounted(() => {
  if (import.meta.client) {
    window.removeEventListener('click', closeDropdown)
  }
})

function toggleDropdown(orderId: number) {
  if (activeDropdownOrderId.value === orderId) {
    activeDropdownOrderId.value = null
  } else {
    activeDropdownOrderId.value = orderId
  }
}

function closeDropdown() {
  activeDropdownOrderId.value = null
}

function triggerToast(msg: string) {
  toastMessage.value = msg
  showToast.value = true
  setTimeout(() => { showToast.value = false }, 3000)
}

function openDetail(order: Order) {
  selectedOrder.value = order
  isDetailModalOpen.value = true
}

function openReceipt(order: Order) {
  selectedReceiptOrder.value = order
  isReceiptModalOpen.value = true
}

function openSimulator(order: Order) {
  selectedSimulatorOrder.value = order
  isSimulatorModalOpen.value = true
  isDetailModalOpen.value = false
}

function handleSimulatorSettled(updatedOrder: Order) {
  isSimulatorModalOpen.value = false
  triggerToast('✅ Pembayaran berhasil diselesaikan via Sandbox Simulator!')
  refresh()
  openReceipt(updatedOrder)
}

function handleSimulatorCancelled(updatedOrder: Order) {
  isSimulatorModalOpen.value = false
  triggerToast('Transaksi dibatalkan. Stok produk otomatis dipulihkan ke katalog.')
  refresh()
}

async function handleStatusChange(orderId: number, newStatus: string) {
  try {
    await updateOrderStatus(orderId, newStatus)
    triggerToast(`Status order berhasil diubah menjadi "${newStatus}"`)
    if (selectedOrder.value && selectedOrder.value.id === orderId) {
      selectedOrder.value.status = newStatus
    }
  } catch (err: any) {
    alert('Gagal update status: ' + (err.message || err))
  }
}

function getSortIcon(field: string) {
  if (sortBy.value !== field) return '⇅'
  return sortDirection.value === 'asc' ? '▲' : '▼'
}
</script>

<template>
  <div class="space-y-8">
    <!-- Toast Notification -->
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

    <!-- Header Banner -->
    <section 
      class="p-5 rounded-2xl border flex flex-col md:flex-row items-start md:items-center justify-between gap-4 transition-colors"
      :class="theme === 'light' 
        ? 'bg-gradient-to-r from-emerald-50/80 via-white to-white border-emerald-100 shadow-sm' 
        : 'bg-gradient-to-r from-emerald-950/70 via-slate-900 to-slate-900 border-emerald-500/20'"
    >
      <div>
        <div class="flex items-center gap-2">
          <span class="text-xs uppercase tracking-wider font-semibold text-emerald-500">Header-Detail Transaction Management</span>
          <span class="text-xs text-slate-400">&middot; Server-Side Pagination</span>
        </div>
        <h2 class="text-lg font-bold mt-1" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
          {{ t('orders.title') }}
        </h2>
        <p class="text-xs mt-1 max-w-2xl" :class="theme === 'light' ? 'text-slate-600' : 'text-slate-400'">
          {{ t('orders.desc') }}
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-2.5">
        <button 
          @click="downloadOrdersCsv" 
          :disabled="isExporting"
          class="px-3.5 py-2.5 rounded-xl text-xs font-medium border transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          :class="theme === 'light'
            ? 'bg-white hover:bg-slate-50 text-slate-700 border-slate-300 shadow-sm'
            : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'"
          title="Unduh laporan penjualan dalam format CSV / Excel"
        >
          <span>📊</span>
          <span>{{ isExporting ? '...' : 'Export CSV' }}</span>
        </button>
        <button 
          @click="refresh()" 
          class="px-3.5 py-2.5 rounded-xl text-xs font-medium border transition cursor-pointer"
          :class="theme === 'light'
            ? 'bg-white hover:bg-slate-50 text-slate-700 border-slate-300 shadow-sm'
            : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'"
        >
          🔄 Refresh
        </button>
        <NuxtLink 
          to="/orders/create"
          class="shrink-0 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-sm transition shadow-lg shadow-emerald-600/30 hover:shadow-emerald-500/50 flex items-center gap-2 cursor-pointer active:scale-95"
        >
          <span class="text-lg leading-none">+</span> {{ t('orders.newOrder') }}
        </NuxtLink>
      </div>
    </section>

    <!-- Metrics Cards -->
    <section class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div 
        class="p-5 rounded-2xl border shadow-sm transition"
        :class="theme === 'light' ? 'bg-white border-slate-200' : 'bg-slate-900/80 border-slate-800'"
      >
        <p class="text-xs font-medium text-slate-400 uppercase tracking-wider">{{ t('orders.totalOrders') }}</p>
        <p class="text-2xl font-extrabold mt-2" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
          {{ totalElements }} <span class="text-sm font-normal text-slate-400">Order</span>
        </p>
        <p class="text-xs text-slate-500 mt-1">Tersimpan di database</p>
      </div>

      <div 
        class="p-5 rounded-2xl border shadow-sm transition"
        :class="theme === 'light' ? 'bg-white border-slate-200' : 'bg-slate-900/80 border-slate-800'"
      >
        <p class="text-xs font-medium text-slate-400 uppercase tracking-wider">{{ t('orders.totalRevenue') }}</p>
        <p class="text-2xl font-extrabold text-emerald-500 mt-2">{{ formatRupiah(totalRevenue) }}</p>
        <p class="text-xs text-emerald-500/80 mt-1">{{ paidOrdersCount }} {{ t('orders.paidOrders') }}</p>
      </div>

      <div 
        class="p-5 rounded-2xl border shadow-sm transition"
        :class="theme === 'light' ? 'bg-white border-slate-200' : 'bg-slate-900/80 border-slate-800'"
      >
        <p class="text-xs font-medium text-slate-400 uppercase tracking-wider">{{ t('orders.pendingOrders') }}</p>
        <p class="text-2xl font-extrabold text-amber-500 mt-2">{{ pendingOrdersCount }} <span class="text-sm font-normal text-slate-400">Order</span></p>
        <p class="text-xs text-amber-500/80 mt-1">Status PENDING</p>
      </div>

      <div 
        class="p-5 rounded-2xl border shadow-sm transition"
        :class="theme === 'light' ? 'bg-white border-slate-200' : 'bg-slate-900/80 border-slate-800'"
      >
        <p class="text-xs font-medium text-slate-400 uppercase tracking-wider">{{ t('orders.paidOrders') }}</p>
        <p class="text-2xl font-extrabold text-indigo-500 mt-2">{{ paidOrdersCount }} <span class="text-sm font-normal text-slate-400">Order</span></p>
        <p class="text-xs text-indigo-500/80 mt-1">Status PAID</p>
      </div>
    </section>

    <!-- Orders Table with Filter Bar & Server Pagination -->
    <section 
      id="orders-table-section" 
      class="border rounded-2xl overflow-hidden shadow-xl scroll-mt-20 transition-colors"
      :class="theme === 'light' ? 'bg-white border-slate-200 shadow-slate-200/50' : 'bg-slate-900/60 border-slate-800/80'"
    >
      <!-- Filter Bar Header -->
      <div 
        class="p-4 sm:p-5 border-b flex flex-col md:flex-row gap-3 items-center justify-between transition-colors"
        :class="theme === 'light' ? 'bg-slate-50/70 border-slate-200' : 'bg-slate-900/40 border-slate-800/80'"
      >
        <!-- Search input -->
        <div class="relative w-full md:w-96">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Cari No. Order / Customer / Email..."
            class="w-full border rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
            :class="theme === 'light' 
              ? 'bg-white border-slate-300 text-slate-900 placeholder-slate-400' 
              : 'bg-slate-950/80 border-slate-700/80 text-slate-200 placeholder-slate-500'"
          />
          <button 
            v-if="searchQuery" 
            @click="searchQuery = ''" 
            class="absolute right-3 top-2.5 text-xs text-slate-400 hover:text-slate-600 cursor-pointer"
          >
            &times;
          </button>
        </div>

        <!-- Status Filter Dropdown -->
        <div class="flex flex-wrap items-center gap-3 w-full md:w-auto justify-end">
          <div class="flex items-center gap-2 text-xs" :class="theme === 'light' ? 'text-slate-600' : 'text-slate-400'">
            <span>{{ t('home.statusFilter') }}</span>
            <select
              v-model="selectedStatus"
              class="border rounded-lg px-3 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500 cursor-pointer"
              :class="theme === 'light' 
                ? 'bg-white border-slate-300 text-slate-800' 
                : 'bg-slate-950/80 border-slate-700/80 text-slate-200'"
            >
              <option value="ALL">{{ t('home.allStatus') }}</option>
              <option value="PAID">Lunas (PAID)</option>
              <option value="PENDING">Menunggu (PENDING)</option>
              <option value="CANCELLED">Dibatalkan (CANCELLED)</option>
            </select>
          </div>

          <span v-if="searchQuery || selectedStatus !== 'ALL'" class="text-xs text-emerald-500 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-lg">
            {{ totalElements }} order
          </span>
        </div>
      </div>

      <!-- Table View -->
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
          <thead 
            class="text-xs uppercase border-b select-none transition-colors"
            :class="theme === 'light' ? 'bg-slate-100/90 text-slate-600 border-slate-200' : 'bg-slate-950/60 text-slate-400 border-slate-800'"
          >
            <tr>
              <th 
                scope="col" 
                @click="toggleSort('orderNumber')"
                class="px-5 py-3.5 font-semibold cursor-pointer transition select-none whitespace-nowrap"
                :class="theme === 'light' ? 'hover:text-slate-900' : 'hover:text-white'"
              >
                {{ t('orders.orderNumber') }} <span class="ml-1 text-[10px] text-slate-400">{{ getSortIcon('orderNumber') }}</span>
              </th>
              <th 
                scope="col" 
                @click="toggleSort('customerName')"
                class="px-5 py-3.5 font-semibold cursor-pointer transition select-none whitespace-nowrap"
                :class="theme === 'light' ? 'hover:text-slate-900' : 'hover:text-white'"
              >
                {{ t('orders.customer') }} <span class="ml-1 text-[10px] text-slate-400">{{ getSortIcon('customerName') }}</span>
              </th>
              <th 
                scope="col" 
                @click="toggleSort('createdAt')"
                class="px-5 py-3.5 font-semibold cursor-pointer transition select-none whitespace-nowrap"
                :class="theme === 'light' ? 'hover:text-slate-900' : 'hover:text-white'"
              >
                {{ t('orders.date') }} <span class="ml-1 text-[10px] text-slate-400">{{ getSortIcon('createdAt') }}</span>
              </th>
              <th scope="col" class="px-5 py-3.5 font-semibold text-center whitespace-nowrap">Items</th>
              <th 
                scope="col" 
                @click="toggleSort('totalAmount')"
                class="px-5 py-3.5 font-semibold text-right cursor-pointer transition select-none whitespace-nowrap"
                :class="theme === 'light' ? 'hover:text-slate-900' : 'hover:text-white'"
              >
                {{ t('orders.total') }} <span class="ml-1 text-[10px] text-slate-400">{{ getSortIcon('totalAmount') }}</span>
              </th>
              <th scope="col" class="px-5 py-3.5 font-semibold text-center whitespace-nowrap">{{ t('orders.status') }}</th>
              <th scope="col" class="px-5 py-3.5 font-semibold text-right whitespace-nowrap w-48">{{ t('home.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y" :class="theme === 'light' ? 'divide-slate-200' : 'divide-slate-800/60'">
            <tr v-if="pending">
              <td colspan="7" class="px-6 py-12 text-center text-slate-400">
                <div class="inline-flex items-center gap-2">
                  <span class="w-4 h-4 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin"></span>
                  Memuat data...
                </div>
              </td>
            </tr>

            <tr 
              v-else
              v-for="order in filteredOrders" 
              :key="order.id"
              class="transition-colors cursor-pointer group"
              :class="theme === 'light' ? 'hover:bg-slate-50' : 'hover:bg-slate-800/30'"
              @click="openDetail(order)"
            >
              <td class="px-5 py-4 font-mono font-bold whitespace-nowrap" :class="theme === 'light' ? 'text-indigo-600' : 'text-indigo-300'">
                {{ order.orderNumber }}
              </td>
              <td class="px-5 py-4 whitespace-nowrap">
                <div class="font-semibold" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">{{ order.customerName }}</div>
                <div class="text-xs text-slate-400">{{ order.customerEmail || '-' }}</div>
              </td>
              <td class="px-5 py-4 text-xs text-slate-400 whitespace-nowrap">
                {{ formatDate(order.createdAt) }}
              </td>
              <td class="px-5 py-4 text-center whitespace-nowrap">
                <span 
                  class="inline-block px-2.5 py-0.5 rounded-full text-xs font-semibold border"
                  :class="theme === 'light' ? 'bg-slate-100 text-slate-700 border-slate-200' : 'bg-slate-800 text-slate-200 border-slate-700'"
                >
                  {{ order.items?.length ?? 0 }} Produk
                </span>
              </td>
              <td class="px-5 py-4 text-right font-bold whitespace-nowrap" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
                {{ formatRupiah(order.totalAmount) }}
              </td>
              <td class="px-5 py-4 text-center whitespace-nowrap">
                <span 
                  v-if="order.status === 'PAID'" 
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-500 border border-emerald-500/20 whitespace-nowrap"
                >
                  Lunas (PAID)
                </span>
                <span 
                  v-else-if="order.status === 'PENDING'" 
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-500/10 text-amber-500 border border-amber-500/20 whitespace-nowrap"
                >
                  Pending
                </span>
                <span 
                  v-else 
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-500/10 text-rose-500 border border-rose-500/20 whitespace-nowrap"
                >
                  Dibatalkan
                </span>
              </td>
              <td class="px-5 py-4 text-right whitespace-nowrap w-48" @click.stop>
                <div class="relative inline-flex items-center justify-end gap-1.5">
                  <!-- Primary Action Hero Button -->
                  <template v-if="order.status === 'PENDING'">
                    <button 
                      @click="openSimulator(order)"
                      class="px-3 py-1.5 text-xs font-bold bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl transition shadow-md shadow-indigo-600/30 flex items-center gap-1.5 cursor-pointer active:scale-95 shrink-0"
                      title="Lanjutkan Pembayaran via Mock Payment Gateway Sandbox"
                    >
                      <span>💳</span> Bayar
                    </button>
                  </template>
                  <template v-else-if="order.status === 'PAID'">
                    <button 
                      @click="openReceipt(order)"
                      class="px-3 py-1.5 text-xs font-semibold rounded-xl border transition flex items-center gap-1.5 cursor-pointer active:scale-95 shrink-0"
                      :class="theme === 'light'
                        ? 'bg-slate-100 hover:bg-slate-200 text-slate-800 border-slate-300 shadow-sm'
                        : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'"
                      title="Cetak Struk Transaksi POS"
                    >
                      <span>🧾</span> Struk
                    </button>
                  </template>
                  <template v-else>
                    <button 
                      @click="openDetail(order)"
                      class="px-3 py-1.5 text-xs font-semibold rounded-xl border transition flex items-center gap-1.5 cursor-pointer active:scale-95 shrink-0"
                      :class="theme === 'light'
                        ? 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-300 shadow-sm'
                        : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'"
                    >
                      <span>👁️</span> Detail
                    </button>
                  </template>

                  <!-- Action Dropdown Trigger (•••) -->
                  <button 
                    @click="toggleDropdown(order.id)"
                    class="p-1.5 rounded-xl border transition cursor-pointer flex items-center justify-center shrink-0"
                    :class="[
                      activeDropdownOrderId === order.id
                        ? 'bg-indigo-600 text-white border-indigo-500 shadow-md shadow-indigo-600/30'
                        : (theme === 'light' 
                            ? 'bg-white hover:bg-slate-100 text-slate-600 border-slate-300 shadow-sm' 
                            : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700')
                    ]"
                    title="Menu Aksi Lainnya"
                  >
                    <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                      <circle cx="12" cy="5" r="2"/>
                      <circle cx="12" cy="12" r="2"/>
                      <circle cx="12" cy="19" r="2"/>
                    </svg>
                  </button>

                  <!-- Popover Dropdown Menu -->
                  <Transition
                    enter-active-class="transition duration-150 ease-out"
                    enter-from-class="transform scale-95 opacity-0 -translate-y-1"
                    enter-to-class="transform scale-100 opacity-100 translate-y-0"
                    leave-active-class="transition duration-100 ease-in"
                    leave-from-class="transform scale-100 opacity-100"
                    leave-to-class="transform scale-95 opacity-0 -translate-y-1"
                  >
                    <div 
                      v-if="activeDropdownOrderId === order.id"
                      class="absolute right-0 top-full mt-1.5 w-48 rounded-2xl border shadow-2xl z-30 py-1.5 text-left text-xs backdrop-blur-xl animate-in fade-in"
                      :class="theme === 'light'
                        ? 'bg-white/95 border-slate-200 text-slate-800 shadow-slate-300/50'
                        : 'bg-slate-900/95 border-slate-800 text-slate-200 shadow-black/80'"
                    >
                      <button 
                        @click="openDetail(order); closeDropdown()"
                        class="w-full px-3.5 py-2 flex items-center gap-2.5 hover:bg-indigo-500/10 hover:text-indigo-400 transition cursor-pointer"
                      >
                        <span>👁️</span> Lihat Detail Pesanan
                      </button>

                      <button 
                        @click="openReceipt(order); closeDropdown()"
                        class="w-full px-3.5 py-2 flex items-center gap-2.5 hover:bg-emerald-500/10 hover:text-emerald-400 transition cursor-pointer"
                      >
                        <span>🧾</span> Cetak Struk POS (PDF)
                      </button>

                      <template v-if="order.status === 'PENDING'">
                        <div class="my-1 border-t" :class="theme === 'light' ? 'border-slate-100' : 'border-slate-800'"></div>

                        <button 
                          @click="openSimulator(order); closeDropdown()"
                          class="w-full px-3.5 py-2 flex items-center gap-2.5 text-indigo-500 hover:bg-indigo-500/10 font-semibold transition cursor-pointer"
                        >
                          <span>💳</span> Bayar via Sandbox
                        </button>

                        <button 
                          @click="handleStatusChange(order.id, 'CANCELLED'); closeDropdown()"
                          class="w-full px-3.5 py-2 flex items-center gap-2.5 text-rose-500 hover:bg-rose-500/10 transition cursor-pointer"
                        >
                          <span>✕</span> Batalkan Transaksi
                        </button>
                      </template>
                    </div>
                  </Transition>
                </div>
              </td>
            </tr>

            <!-- Empty states -->
            <tr v-if="!pending && filteredOrders.length === 0">
              <td colspan="7" class="px-6 py-12 text-center text-slate-400">
                <div v-if="totalElements > 0">
                  Tidak ada transaksi yang cocok dengan filter.
                </div>
                <div v-else>
                  Belum ada transaksi. Klik <NuxtLink to="/orders/create" class="text-emerald-500 underline">Buat Order Baru</NuxtLink>.
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination Controls Bar -->
      <PaginationControls
        :page="page"
        :page-size="pageSize"
        :total-elements="totalElements"
        :total-pages="totalPages"
        :is-first="isFirst"
        :is-last="isLast"
        @set-page="setPage"
        @next-page="nextPage"
        @prev-page="prevPage"
        @set-page-size="setPageSize"
      />
    </section>

    <!-- Modals -->
    <OrderDetailModal
      :is-open="isDetailModalOpen"
      :order="selectedOrder"
      :format-rupiah="formatRupiah"
      :format-date="formatDate"
      @close="isDetailModalOpen = false"
      @update-status="handleStatusChange"
      @open-simulator="openSimulator"
    />

    <PaymentSimulatorModal
      :is-open="isSimulatorModalOpen"
      :order="selectedSimulatorOrder"
      :format-rupiah="formatRupiah"
      @close="isSimulatorModalOpen = false"
      @settled="handleSimulatorSettled"
      @cancelled="handleSimulatorCancelled"
    />

    <OrderReceiptModal
      :is-open="isReceiptModalOpen"
      :order="selectedReceiptOrder"
      :format-rupiah="formatRupiah"
      :format-date="formatDate"
      @close="isReceiptModalOpen = false"
    />
  </div>
</template>
