<script setup lang="ts">
import type { Product } from '~/types/product'

const props = defineProps<{
  isOpen: boolean
  product: Product | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'restocked'): void
}>()

const { movements, pending, fetchMovements, restockProduct, getMovementBadge, formatDate } = useStockHistory()
const { theme } = useTheme()

// Restock form state
const isRestocking = ref(false)
const showRestockForm = ref(false)
const restockQty = ref<number>(10)
const restockNotes = ref<string>('Restock dari supplier')

// Fetch movements when modal opens or product changes
watch(() => [props.isOpen, props.product], async ([open, prod]) => {
  if (open && prod && typeof prod === 'object' && 'id' in prod) {
    showRestockForm.value = false
    restockQty.value = 10
    restockNotes.value = 'Restock gudang'
    await fetchMovements((prod as Product).id)
  }
})

async function handleRestockSubmit() {
  if (!props.product || restockQty.value <= 0) return
  isRestocking.value = true
  try {
    await restockProduct({
      productId: props.product.id,
      quantity: restockQty.value,
      notes: restockNotes.value
    })
    showRestockForm.value = false
    emit('restocked')
  } catch (err: any) {
    alert('Gagal melakukan restock: ' + (err.message || err))
  } finally {
    isRestocking.value = false
  }
}
</script>

<template>
  <div v-if="isOpen && product" class="fixed inset-0 z-50 flex items-center justify-center p-4">
    <!-- Backdrop -->
    <div 
      class="fixed inset-0 bg-slate-950/80 backdrop-blur-sm transition-opacity"
      @click="emit('close')"
    ></div>

    <!-- Modal Card -->
    <div 
      class="relative border rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl z-10 flex flex-col max-h-[90vh] transition-colors"
      :class="theme === 'light' ? 'bg-white border-slate-200 text-slate-800' : 'bg-slate-900 border-slate-800 text-slate-100'"
    >
      <!-- Modal Header -->
      <div 
        class="p-5 border-b flex items-center justify-between shrink-0"
        :class="theme === 'light' ? 'bg-slate-50/80 border-slate-200' : 'bg-slate-950/40 border-slate-800'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-indigo-600/20 text-indigo-500 border border-indigo-500/30 flex items-center justify-center text-lg font-bold">
            📜
          </div>
          <div>
            <h3 class="font-bold text-base flex items-center gap-2" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
              Riwayat Mutasi Stok
              <span class="text-xs font-mono px-2 py-0.5 rounded border" :class="theme === 'light' ? 'bg-indigo-50 text-indigo-600 border-indigo-200' : 'bg-slate-800 text-indigo-400 border-slate-700'">
                {{ product.sku }}
              </span>
            </h3>
            <p class="text-xs text-slate-400 mt-0.5">
              {{ product.name }} &middot; Stok Saat Ini: <strong class="text-emerald-500">{{ product.stock }} unit</strong>
            </p>
          </div>
        </div>

        <button 
          @click="emit('close')"
          class="w-8 h-8 rounded-lg flex items-center justify-center transition cursor-pointer"
          :class="theme === 'light' ? 'bg-slate-100 hover:bg-slate-200 text-slate-500' : 'bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white'"
        >
          &times;
        </button>
      </div>

      <!-- Quick Restock Action Bar -->
      <div 
        class="p-4 border-b shrink-0"
        :class="theme === 'light' ? 'bg-slate-50 border-slate-200' : 'bg-slate-950/60 border-slate-800/80'"
      >
        <div v-if="!showRestockForm" class="flex items-center justify-between">
          <p class="text-xs text-slate-400">
            Perlu menambah stok masuk untuk produk ini?
          </p>
          <button 
            @click="showRestockForm = true"
            class="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-md shadow-emerald-600/20 flex items-center gap-1.5 transition active:scale-95 cursor-pointer"
          >
            <span>+</span> Tambah Stok (Restock)
          </button>
        </div>

        <!-- Inline Restock Form -->
        <div 
          v-else 
          class="space-y-3 p-3 rounded-xl border animate-in fade-in duration-150"
          :class="theme === 'light' ? 'bg-white border-emerald-300 shadow-sm' : 'bg-slate-900 border-emerald-500/30'"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold text-emerald-500 flex items-center gap-1.5">
              <span>📥</span> Form Restock Gudang Masuk
            </span>
            <button 
              @click="showRestockForm = false"
              class="text-xs text-slate-400 hover:text-slate-600 cursor-pointer"
            >
              Batal
            </button>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-12 gap-3">
            <div class="sm:col-span-4">
              <label class="block text-[11px] text-slate-400 mb-1 font-medium">Jumlah Tambahan Unit</label>
              <input 
                v-model.number="restockQty"
                type="number"
                min="1"
                class="w-full border rounded-lg px-3 py-1.5 text-xs focus:outline-none focus:border-emerald-500"
                :class="theme === 'light' ? 'bg-slate-50 border-slate-300 text-slate-900' : 'bg-slate-950 border-slate-700 text-white'"
              />
            </div>
            <div class="sm:col-span-8">
              <label class="block text-[11px] text-slate-400 mb-1 font-medium">Catatan / No. PO Supplier</label>
              <input 
                v-model="restockNotes"
                type="text"
                placeholder="Contoh: PO-SUPPLIER-882 atau Batch 02"
                class="w-full border rounded-lg px-3 py-1.5 text-xs focus:outline-none focus:border-emerald-500"
                :class="theme === 'light' ? 'bg-slate-50 border-slate-300 text-slate-900' : 'bg-slate-950 border-slate-700 text-white'"
              />
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-1">
            <button 
              @click="showRestockForm = false"
              class="px-3 py-1.5 rounded-lg text-xs border cursor-pointer"
              :class="theme === 'light' ? 'text-slate-600 hover:bg-slate-100 border-slate-300' : 'text-slate-400 hover:text-white border-slate-700'"
            >
              Tutup
            </button>
            <button 
              @click="handleRestockSubmit"
              :disabled="isRestocking || restockQty <= 0"
              class="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs transition cursor-pointer disabled:opacity-50 flex items-center gap-1.5 active:scale-95"
            >
              {{ isRestocking ? 'Menyimpan...' : 'Simpan Restock (+)' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Timeline Content Area -->
      <div class="p-5 overflow-y-auto space-y-3 flex-1">
        <div v-if="pending" class="py-12 text-center text-slate-400 text-xs">
          <span class="inline-block w-4 h-4 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin mr-2"></span>
          Memuat riwayat mutasi...
        </div>

        <div v-else-if="movements.length === 0" class="py-12 text-center text-slate-400 text-xs">
          Belum ada riwayat mutasi stok untuk produk ini.
        </div>

        <!-- Movements List -->
        <div v-else class="space-y-3">
          <div 
            v-for="m in movements" 
            :key="m.id"
            class="p-3.5 rounded-xl border transition flex flex-col sm:flex-row sm:items-center justify-between gap-3"
            :class="theme === 'light' 
              ? 'bg-slate-50 border-slate-200 hover:border-slate-300' 
              : 'bg-slate-950/60 border-slate-800/80 hover:border-slate-700'"
          >
            <!-- Left Info -->
            <div class="flex items-start gap-3">
              <div class="text-xl mt-0.5 shrink-0">
                {{ getMovementBadge(m.type).icon }}
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <span 
                    class="px-2 py-0.5 rounded-md text-[11px] font-semibold border"
                    :class="getMovementBadge(m.type).badgeClass"
                  >
                    {{ getMovementBadge(m.type).label }}
                  </span>
                  <span v-if="m.referenceNumber" class="text-xs font-mono text-indigo-500 font-semibold">
                    {{ m.referenceNumber }}
                  </span>
                </div>
                <p class="text-xs mt-1" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
                  {{ m.notes || 'Tanpa catatan' }}
                </p>
                <p class="text-[11px] text-slate-400 mt-0.5">
                  {{ formatDate(m.createdAt) }}
                </p>
              </div>
            </div>

            <!-- Right: Stock Quantity Delta -->
            <div 
              class="text-left sm:text-right shrink-0 border-t sm:border-t-0 pt-2 sm:pt-0"
              :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'"
            >
              <p 
                class="font-extrabold text-base"
                :class="m.quantityChange > 0 ? 'text-emerald-500' : 'text-rose-500'"
              >
                {{ m.quantityChange > 0 ? '+' : '' }}{{ m.quantityChange }} Unit
              </p>
              <p class="text-[11px] text-slate-400">
                Sisa Stok: <strong :class="theme === 'light' ? 'text-slate-900' : 'text-white'">{{ m.resultingStock }}</strong>
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Modal Footer -->
      <div 
        class="p-4 border-t flex items-center justify-between text-xs text-slate-400 shrink-0"
        :class="theme === 'light' ? 'border-slate-200 bg-slate-50/80' : 'border-slate-800 bg-slate-950/40'"
      >
        <span>Log mutasi tercatat otomatis oleh sistem.</span>
        <button 
          @click="emit('close')"
          class="px-4 py-2 rounded-xl font-medium transition cursor-pointer"
          :class="theme === 'light' ? 'bg-slate-200 hover:bg-slate-300 text-slate-800' : 'bg-slate-800 hover:bg-slate-700 text-white'"
        >
          Tutup
        </button>
      </div>
    </div>
  </div>
</template>
