<script setup lang="ts">
import type { Order } from '~/types/order'

const props = withDefaults(defineProps<{
  isOpen: boolean
  order: Order | null
  formatRupiah?: (val: number) => string
  formatDate?: (val: string) => string
}>(), {
  formatRupiah: (val: number) => 'Rp' + (val || 0).toLocaleString('id-ID'),
  formatDate: (val: string) => val ? new Date(val).toLocaleString('id-ID') : '-'
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'updateStatus', orderId: number, newStatus: string): void
}>()

const { t, locale } = useI18n()
const { theme } = useTheme()

function handleSetStatus(newStatus: string) {
  if (!props.order) return
  emit('updateStatus', props.order.id, newStatus)
}
</script>

<template>
  <Transition
    enter-active-class="transition duration-200 ease-out"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition duration-150 ease-in"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div 
      v-if="isOpen && order" 
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm overflow-y-auto"
      @click.self="emit('close')"
    >
      <div 
        class="border rounded-3xl p-6 sm:p-8 max-w-2xl w-full shadow-2xl space-y-6 animate-in zoom-in-95 duration-200 transition-colors"
        :class="theme === 'light' ? 'bg-white border-slate-200 shadow-slate-300/50 text-slate-800' : 'bg-slate-900 border-slate-800 text-slate-100'"
      >
        <!-- Modal Header -->
        <div class="flex items-start justify-between pb-4 border-b" :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'">
          <div>
            <div class="flex items-center gap-2">
              <span class="text-xs uppercase tracking-wider font-semibold text-emerald-500">
                {{ locale === 'id' ? 'Detail Transaksi' : 'Transaction Details' }}
              </span>
              <span 
                v-if="order.status === 'PAID'" 
                class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-500 border border-emerald-500/20"
              >
                LUNAS (PAID)
              </span>
              <span 
                v-else-if="order.status === 'PENDING'" 
                class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-500 border border-amber-500/20"
              >
                PENDING
              </span>
              <span 
                v-else 
                class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-500/10 text-rose-500 border border-rose-500/20"
              >
                DIBATALKAN
              </span>
            </div>
            <h3 class="text-xl font-mono font-bold mt-1" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
              {{ order.orderNumber }}
            </h3>
          </div>

          <button 
            @click="emit('close')"
            class="text-slate-400 hover:text-slate-600 transition text-2xl font-bold leading-none p-1 cursor-pointer"
          >
            &times;
          </button>
        </div>

        <!-- Customer & Order Metadata Grid -->
        <div 
          class="grid grid-cols-1 sm:grid-cols-2 gap-4 p-4 rounded-2xl border text-xs transition-colors"
          :class="theme === 'light' ? 'bg-slate-50 border-slate-200' : 'bg-slate-950/70 border-slate-800'"
        >
          <div>
            <p class="text-slate-400 uppercase tracking-wider font-semibold">{{ t('pos.customerName') }}</p>
            <p class="text-sm font-bold mt-0.5" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">{{ order.customerName }}</p>
          </div>
          <div>
            <p class="text-slate-400 uppercase tracking-wider font-semibold">{{ t('pos.customerEmail') }}</p>
            <p class="text-sm mt-0.5 font-mono" :class="theme === 'light' ? 'text-slate-800' : 'text-slate-300'">{{ order.customerEmail || '-' }}</p>
          </div>
          <div>
            <p class="text-slate-400 uppercase tracking-wider font-semibold">{{ t('orders.date') }}</p>
            <p class="text-sm mt-0.5" :class="theme === 'light' ? 'text-slate-800' : 'text-slate-300'">{{ formatDate(order.createdAt) }}</p>
          </div>
          <div>
            <p class="text-slate-400 uppercase tracking-wider font-semibold">{{ locale === 'id' ? 'Jumlah Item' : 'Total Items' }}</p>
            <p class="text-sm font-bold mt-0.5 text-indigo-500">{{ order.items?.length ?? 0 }} Macam Produk</p>
          </div>
        </div>

        <!-- Order Items Breakdown Table -->
        <div class="space-y-2">
          <h4 class="text-xs font-bold uppercase tracking-wider text-slate-400">
            {{ locale === 'id' ? 'Daftar Produk yang Dibeli' : 'Purchased Items List' }}
          </h4>
          <div class="border rounded-2xl overflow-hidden" :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'">
            <table class="w-full text-left text-xs" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              <thead 
                class="uppercase border-b"
                :class="theme === 'light' ? 'bg-slate-100 text-slate-600 border-slate-200' : 'bg-slate-950/80 text-slate-400 border-slate-800'"
              >
                <tr>
                  <th class="px-4 py-3 font-semibold">Nama Produk</th>
                  <th class="px-4 py-3 font-semibold text-right">Harga</th>
                  <th class="px-4 py-3 font-semibold text-center">Qty</th>
                  <th class="px-4 py-3 font-semibold text-right">Subtotal</th>
                </tr>
              </thead>
              <tbody class="divide-y" :class="theme === 'light' ? 'divide-slate-200 bg-white' : 'divide-slate-800/60 bg-slate-950/30'">
                <tr v-for="item in order.items" :key="item.id">
                  <td class="px-4 py-3 font-medium" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
                    {{ item.productName }}
                  </td>
                  <td class="px-4 py-3 text-right text-slate-500 font-mono">
                    {{ formatRupiah(item.unitPrice) }}
                  </td>
                  <td class="px-4 py-3 text-center font-bold text-indigo-500">
                    {{ item.quantity }}
                  </td>
                  <td class="px-4 py-3 text-right font-bold text-emerald-500 font-mono">
                    {{ formatRupiah(item.subtotal) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Grand Total Summary -->
        <div 
          class="p-4 rounded-2xl border flex items-center justify-between transition-colors"
          :class="theme === 'light' ? 'bg-emerald-50/70 border-emerald-200 text-emerald-950' : 'bg-emerald-950/30 border-emerald-500/30 text-white'"
        >
          <span class="text-sm font-semibold">{{ t('pos.totalPayment') }}:</span>
          <span class="text-2xl font-extrabold text-emerald-500 font-mono">
            {{ formatRupiah(order.totalAmount) }}
          </span>
        </div>

        <!-- Modal Footer Actions -->
        <div class="flex flex-wrap items-center justify-between gap-3 pt-2">
          <!-- Status Mutation Actions if Pending -->
          <div class="flex items-center gap-2">
            <template v-if="order.status === 'PENDING'">
              <button 
                @click="handleSetStatus('PAID')"
                class="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold shadow-md shadow-emerald-600/30 transition cursor-pointer active:scale-95"
              >
                ✓ {{ t('orders.setPaid') }}
              </button>
              <button 
                @click="handleSetStatus('CANCELLED')"
                class="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-bold shadow-md shadow-rose-600/30 transition cursor-pointer active:scale-95"
              >
                ✕ {{ t('orders.cancel') }}
              </button>
            </template>
          </div>

          <button 
            @click="emit('close')"
            class="px-5 py-2.5 rounded-xl border text-xs font-semibold transition cursor-pointer ml-auto"
            :class="theme === 'light'
              ? 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-300'
              : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'"
          >
            {{ locale === 'id' ? 'Tutup' : 'Close' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>
