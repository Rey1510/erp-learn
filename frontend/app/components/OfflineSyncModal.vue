<script setup lang="ts">
import { useOfflineSync } from '~/composables/useOfflineSync'

const props = defineProps<{
  isOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'synced'): void
}>()

const {
  isOnline,
  isSimulatedOffline,
  effectiveOnline,
  outboxList,
  pendingSyncCount,
  syncedCount,
  isSyncing,
  syncMessage,
  syncOutboxQueue,
  clearSyncedHistory,
  removeOutboxItem,
  toggleSimulatedOffline
} = useOfflineSync()

const { formatRupiah, refresh: refreshOrders, reseedOrders } = useOrders()
const { refresh: refreshProducts } = useProducts()
const isReseeding = ref(false)

async function handleResetDemoData() {
  if (!confirm('Apakah Anda ingin me-reset seluruh database demo ke kondisi awal yang bersih?')) return
  isReseeding.value = true
  try {
    await reseedOrders()
    await Promise.all([refreshOrders?.(), refreshProducts?.()])
    await refreshNuxtData()
    alert('✅ Database demo berhasil di-reset ke kondisi awal!')
  } catch (err: any) {
    alert('Gagal mereset database: ' + (err.message || err))
  } finally {
    isReseeding.value = false
  }
}

async function handleSyncNow() {
  const result = await syncOutboxQueue()
  if (result.successCount > 0) {
    emit('synced')
    refreshOrders?.().catch(() => {})
    refreshProducts?.().catch(() => {})
    await refreshNuxtData()
  }
}

function formatDate(isoStr: string) {
  if (!isoStr) return '-'
  const d = new Date(isoStr)
  return new Intl.DateTimeFormat('id-ID', {
    dateStyle: 'short',
    timeStyle: 'medium'
  }).format(d)
}
</script>

<template>
  <Teleport to="body">
    <div 
      v-if="isOpen" 
      class="fixed inset-0 z-[999] flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200"
      @click.self="emit('close')"
    >
    <div class="bg-slate-900 border border-slate-700/80 w-full max-w-2xl rounded-2xl shadow-2xl overflow-hidden text-white flex flex-col max-h-[85vh]">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/70 flex-shrink-0">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30 flex items-center justify-center font-bold text-lg">
            ⚡
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-base text-white">POS Offline Sync Center</h3>
              <span 
                class="px-2 py-0.5 rounded-full text-[10px] font-bold border"
                :class="effectiveOnline 
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30' 
                  : 'bg-rose-500/10 text-rose-400 border-rose-500/30'"
              >
                {{ effectiveOnline ? '🟢 Connected' : '🔴 Offline Mode' }}
              </span>
            </div>
            <p class="text-[11px] text-slate-400">IndexedDB Outbox Queue Engine • Resilient Local Persistence</p>
          </div>
        </div>
        <button 
          @click="emit('close')" 
          class="text-slate-400 hover:text-white text-xl font-bold transition cursor-pointer p-1"
        >
          &times;
        </button>
      </div>

      <!-- Simulator Controls Bar -->
      <div class="px-6 py-3 bg-slate-950/40 border-b border-slate-800/80 flex items-center justify-between text-xs flex-shrink-0">
        <div class="flex items-center gap-2">
          <span class="text-slate-400">Status Simulasi:</span>
          <button 
            @click="toggleSimulatedOffline"
            class="px-3 py-1 rounded-lg font-semibold text-xs border transition cursor-pointer flex items-center gap-1.5"
            :class="isSimulatedOffline 
              ? 'bg-amber-600/30 text-amber-300 border-amber-500 hover:bg-amber-600/40 shadow-sm shadow-amber-500/20' 
              : 'bg-slate-800 text-slate-300 border-slate-700 hover:bg-slate-700'"
          >
            <span>{{ isSimulatedOffline ? '🔴 Mode Simulasi Offline AKTIF' : '🟢 Jaringan Normal (Online)' }}</span>
            <span class="text-[10px] underline opacity-80">(Klik untuk toggle)</span>
          </button>
        </div>

        <div class="flex items-center gap-2 text-[11px] text-slate-400">
          <span>Antrean: <strong class="text-amber-400">{{ pendingSyncCount }}</strong></span>
          <span>•</span>
          <span>Tersinkron: <strong class="text-emerald-400">{{ syncedCount }}</strong></span>
        </div>
      </div>

      <!-- Sync Feedback Banner -->
      <div v-if="syncMessage" class="px-6 py-2 bg-indigo-950/50 border-b border-indigo-800/60 text-xs text-indigo-300 flex items-center justify-between flex-shrink-0 animate-in fade-in">
        <span>{{ syncMessage }}</span>
      </div>

      <!-- Outbox List Table -->
      <div class="p-6 overflow-y-auto flex-1 space-y-4">
        <div v-if="outboxList.length === 0" class="text-center py-12 text-slate-500">
          <div class="text-3xl mb-2">📭</div>
          <div class="text-sm font-medium text-slate-400">Tidak ada antrean transaksi offline</div>
          <p class="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            Ketika internet offline, transaksi POS yang dibuat di kasir akan otomatis tersimpan sementara di IndexedDB ini.
          </p>
        </div>

        <div v-else class="space-y-3">
          <div 
            v-for="item in outboxList" 
            :key="item.id || item.tempOrderNumber"
            class="p-4 rounded-xl border transition flex flex-col md:flex-row md:items-center justify-between gap-3"
            :class="item.status === 'SYNCED'
              ? 'bg-slate-950/40 border-slate-800/80 text-slate-400'
              : (item.status === 'FAILED'
                  ? 'bg-rose-950/20 border-rose-800/50 text-slate-200'
                  : 'bg-slate-950/80 border-slate-700 text-slate-100')"
          >
            <!-- Order Details -->
            <div class="space-y-1">
              <div class="flex items-center gap-2">
                <span class="font-mono font-bold text-xs" :class="item.status === 'SYNCED' ? 'text-slate-400' : 'text-amber-400'">
                  {{ item.tempOrderNumber }}
                </span>
                
                <!-- Status Badge -->
                <span 
                  v-if="item.status === 'PENDING_SYNC'"
                  class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30 flex items-center gap-1"
                >
                  <span>⏳</span> Menunggu Sync
                </span>
                <span 
                  v-else-if="item.status === 'SYNCING'"
                  class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/30 flex items-center gap-1 animate-pulse"
                >
                  <span class="w-2.5 h-2.5 border-2 border-indigo-400 border-t-transparent rounded-full animate-spin"></span>
                  Syncing...
                </span>
                <span 
                  v-else-if="item.status === 'SYNCED'"
                  class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 flex items-center gap-1"
                >
                  <span>✅</span> Tersinkron Resmi
                </span>
                <span 
                  v-else
                  class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/30 flex items-center gap-1"
                >
                  <span>❌</span> Sync Gagal
                </span>
              </div>

              <div class="text-xs text-slate-300 font-semibold">
                {{ item.customerName }} • <span class="font-mono text-emerald-400">{{ formatRupiah(item.totalAmount) }}</span>
                <span class="text-slate-400 font-normal text-[11px] ml-1">({{ item.paymentMethod }})</span>
              </div>

              <!-- Item Breakdown Snippet -->
              <div class="text-[11px] text-slate-400">
                {{ item.itemsSummary.map(i => `${i.quantity}x ${i.productName}`).join(', ') }}
              </div>

              <div class="text-[10px] text-slate-500 flex items-center gap-2">
                <span>Dibuat: {{ formatDate(item.createdAt) }}</span>
                <span v-if="item.syncedAt">• Sync: {{ formatDate(item.syncedAt) }}</span>
              </div>

              <!-- Error Message if failed -->
              <div v-if="item.lastError" class="text-[10px] text-rose-400 bg-rose-950/40 px-2 py-1 rounded border border-rose-800/40 mt-1">
                ⚠️ Error: {{ item.lastError }}
              </div>
            </div>

            <!-- Item Action -->
            <div class="flex items-center gap-2 shrink-0 justify-end">
              <button 
                v-if="item.id"
                @click="removeOutboxItem(item.id)"
                class="px-2 py-1 text-[11px] text-slate-500 hover:text-rose-400 hover:bg-rose-950/30 rounded border border-transparent hover:border-rose-800 transition cursor-pointer"
                title="Hapus item antrean"
              >
                Hapus
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer Actions -->
      <div class="px-6 py-4 border-t border-slate-800 bg-slate-950/70 flex flex-col sm:flex-row sm:items-center justify-between gap-3 flex-shrink-0">
        <div class="flex items-center gap-3">
          <button 
            v-if="syncedCount > 0"
            @click="clearSyncedHistory"
            class="text-xs text-slate-400 hover:text-white underline transition cursor-pointer"
          >
            Bersihkan Tersinkron ({{ syncedCount }})
          </button>

          <button 
            @click="handleResetDemoData"
            :disabled="isReseeding"
            class="px-2.5 py-1 text-xs text-amber-400 hover:text-amber-300 hover:bg-amber-950/40 rounded-lg border border-amber-500/30 transition cursor-pointer flex items-center gap-1"
            title="Reset database PostgreSQL ke data awal demo yang bersih"
          >
            <span>{{ isReseeding ? '⏳ Mereset...' : '🔄 Reset Data Demo' }}</span>
          </button>
        </div>

        <div class="flex items-center gap-3 justify-end">
          <button 
            @click="emit('close')" 
            class="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition cursor-pointer"
          >
            Tutup
          </button>

          <button 
            :disabled="!effectiveOnline || pendingSyncCount === 0 || isSyncing"
            @click="handleSyncNow"
            class="px-4 py-2 rounded-xl text-xs font-bold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed text-white shadow-lg shadow-indigo-600/30 flex items-center gap-2 transition cursor-pointer active:scale-95"
          >
            <span v-if="isSyncing" class="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
            <span v-else>🔄</span>
            <span>{{ isSyncing ? 'Menyinkronkan...' : `Sinkronkan Semua (${pendingSyncCount})` }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
  </Teleport>
</template>
