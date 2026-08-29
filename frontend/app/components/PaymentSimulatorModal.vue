<script setup lang="ts">
import type { Order, Payment } from '~/types/order'

const props = defineProps<{
  isOpen: boolean
  order: Order | null
  formatRupiah: (val: number) => string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'settled', order: Order): void
  (e: 'cancelled', order: Order): void
}>()

const payment = ref<Payment | null>(null)
const isLoadingPayment = ref(false)
const isSimulating = ref(false)
const simulationMessage = ref('')
const countdownMinutes = ref(15)
const countdownSeconds = ref(0)
const isExpired = ref(false)
let timerInterval: any = null

const config = useRuntimeConfig()
const apiBase = config.public.apiBase || 'http://localhost:8080'

watch(() => props.isOpen, async (open) => {
  if (open && props.order) {
    simulationMessage.value = ''
    await fetchPaymentDetails()
    startTimer()
  } else {
    clearInterval(timerInterval)
  }
})

function updateCountdown() {
  if (!payment.value?.expiresAt) {
    countdownMinutes.value = 15
    countdownSeconds.value = 0
    isExpired.value = false
    return
  }

  const expireTime = new Date(payment.value.expiresAt).getTime()
  const now = Date.now()
  const diffMs = expireTime - now

  if (diffMs <= 0) {
    countdownMinutes.value = 0
    countdownSeconds.value = 0
    isExpired.value = true
    clearInterval(timerInterval)
    return
  }

  isExpired.value = false
  const totalSecs = Math.floor(diffMs / 1000)
  countdownMinutes.value = Math.floor(totalSecs / 60)
  countdownSeconds.value = totalSecs % 60
}

function startTimer() {
  clearInterval(timerInterval)
  updateCountdown()
  timerInterval = setInterval(() => {
    updateCountdown()
  }, 1000)
}

async function fetchPaymentDetails() {
  if (!props.order) return
  try {
    isLoadingPayment.value = true
    const res = await $fetch<Payment>(`${apiBase}/api/payments/order/${props.order.id}`, {
      headers: { 'bypass-tunnel-reminder': 'true' }
    })
    payment.value = res
    updateCountdown()
  } catch (err) {
    console.error('Failed to fetch payment', err)
  } finally {
    isLoadingPayment.value = false
  }
}

async function runSimulation(action: 'SETTLE' | 'EXPIRE' | 'FAIL') {
  if (!payment.value) return

  try {
    isSimulating.value = true
    simulationMessage.value = 'Mengirim sinyal simulasi webhook ke payment engine...'

    const updatedPayment = await $fetch<Payment>(`${apiBase}/api/payments/${payment.value.id}/simulate`, {
      method: 'POST',
      headers: { 'bypass-tunnel-reminder': 'true' },
      body: {
        action,
        notes: `Simulasi interaktif dijalankan oleh penguji (${action})`
      }
    })

    payment.value = updatedPayment

    // Fetch updated order
    const updatedOrder = await $fetch<Order>(`${apiBase}/api/orders/${props.order!.id}`, {
      headers: { 'bypass-tunnel-reminder': 'true' }
    })

    if (action === 'SETTLE') {
      simulationMessage.value = '✅ Pembayaran Berhasil! Mengupdate struk kasir...'
      setTimeout(() => {
        emit('settled', updatedOrder)
      }, 1000)
    } else if (action === 'EXPIRE') {
      simulationMessage.value = '⏱️ Transaksi Expired! Stok telah otomatis dikembalikan ke katalog.'
      setTimeout(() => {
        emit('cancelled', updatedOrder)
      }, 1200)
    } else {
      simulationMessage.value = '❌ Pembayaran Gagal/Ditolak Bank! Stok telah dikembalikan.'
      setTimeout(() => {
        emit('cancelled', updatedOrder)
      }, 1200)
    }
  } catch (err: any) {
    alert('Simulasi gagal: ' + (err.data?.error || err.message))
  } finally {
    isSimulating.value = false
  }
}
</script>

<template>
  <div 
    v-if="isOpen && order" 
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200"
    @click.self="emit('close')"
  >
    <div class="bg-slate-900 border border-slate-700/80 w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden text-white">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
        <div class="flex items-center gap-2.5">
          <div class="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center font-bold text-sm">
            💳
          </div>
          <div>
            <h3 class="font-bold text-sm text-white">Mock Payment Gateway Simulator</h3>
            <p class="text-[11px] text-slate-400">Fintech Sandbox Engine • Portofolio Mode</p>
          </div>
        </div>
        <button 
          @click="emit('close')" 
          class="text-slate-400 hover:text-white text-xl font-bold transition cursor-pointer p-1"
        >
          &times;
        </button>
      </div>

      <!-- Content Body -->
      <div class="p-6 space-y-5 max-h-[75vh] overflow-y-auto">
        <!-- Order Brief Box -->
        <div class="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 flex items-center justify-between">
          <div>
            <span class="text-[10px] uppercase font-bold tracking-wider text-slate-400">Total Pembayaran</span>
            <div class="text-lg font-black text-emerald-400 tracking-tight">
              {{ formatRupiah(order.totalAmount) }}
            </div>
            <div class="text-[11px] text-slate-400 mt-0.5">
              Invoice: <span class="font-mono text-slate-200">{{ order.orderNumber }}</span>
            </div>
          </div>
          <div class="text-right">
            <span class="text-[10px] uppercase font-bold tracking-wider text-slate-400">Metode</span>
            <div class="text-xs font-bold text-indigo-300 mt-0.5">
              {{ order.paymentMethod }}
            </div>
            <div 
              class="text-[10px] font-mono mt-0.5 flex items-center justify-end gap-1 font-semibold"
              :class="isExpired ? 'text-rose-400' : 'text-amber-400'"
            >
              <span>{{ isExpired ? '⏱️' : '⏳' }}</span> 
              {{ isExpired ? 'Expired (00:00)' : `${String(countdownMinutes).padStart(2, '0')}:${String(countdownSeconds).padStart(2, '0')}` }}
            </div>
          </div>
        </div>

        <!-- Dynamic UI per Payment Method -->
        <!-- 1. QRIS Simulator View -->
        <div v-if="order.paymentMethod === 'QRIS'" class="text-center space-y-3 bg-white p-5 rounded-2xl text-slate-900 border border-slate-200">
          <div class="flex items-center justify-between pb-2 border-b border-slate-200">
            <span class="font-bold text-xs tracking-wider text-rose-600">QRIS STANDAR NASIONAL</span>
            <span class="text-[10px] bg-rose-50 text-rose-700 px-2 py-0.5 rounded font-bold border border-rose-200">NMID: ID1020092140</span>
          </div>

          <!-- Pure SVG Dynamic QR Matrix Pattern -->
          <div class="flex justify-center my-2">
            <div class="p-3 bg-white border-2 border-slate-900 rounded-xl shadow-inner inline-block">
              <svg class="w-44 h-44" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                <!-- QR Frame Corner Markers -->
                <rect x="5" y="5" width="26" height="26" rx="4" fill="#0f172a" />
                <rect x="9" y="9" width="18" height="18" rx="2" fill="#ffffff" />
                <rect x="13" y="13" width="10" height="10" rx="1" fill="#0f172a" />

                <rect x="69" y="5" width="26" height="26" rx="4" fill="#0f172a" />
                <rect x="73" y="9" width="18" height="18" rx="2" fill="#ffffff" />
                <rect x="77" y="13" width="10" height="10" rx="1" fill="#0f172a" />

                <rect x="5" y="69" width="26" height="26" rx="4" fill="#0f172a" />
                <rect x="9" y="73" width="18" height="18" rx="2" fill="#ffffff" />
                <rect x="13" y="77" width="10" height="10" rx="1" fill="#0f172a" />

                <!-- Data Matrix Dots Pattern -->
                <rect x="36" y="8" width="6" height="6" fill="#0f172a" />
                <rect x="46" y="8" width="6" height="6" fill="#0f172a" />
                <rect x="56" y="8" width="6" height="6" fill="#0f172a" />

                <rect x="8" y="36" width="6" height="6" fill="#0f172a" />
                <rect x="8" y="46" width="6" height="6" fill="#0f172a" />
                <rect x="8" y="56" width="6" height="6" fill="#0f172a" />

                <rect x="36" y="36" width="8" height="8" rx="1" fill="#4f46e5" />
                <rect x="48" y="36" width="6" height="6" fill="#0f172a" />
                <rect x="58" y="36" width="6" height="6" fill="#0f172a" />
                <rect x="68" y="36" width="8" height="8" rx="1" fill="#4f46e5" />

                <rect x="36" y="48" width="6" height="6" fill="#0f172a" />
                <rect x="46" y="48" width="8" height="8" rx="1" fill="#0f172a" />
                <rect x="58" y="48" width="6" height="6" fill="#0f172a" />
                <rect x="68" y="48" width="6" height="6" fill="#0f172a" />

                <rect x="36" y="60" width="8" height="8" rx="1" fill="#0f172a" />
                <rect x="48" y="60" width="6" height="6" fill="#0f172a" />
                <rect x="58" y="60" width="8" height="8" rx="1" fill="#4f46e5" />
                <rect x="68" y="60" width="6" height="6" fill="#0f172a" />

                <rect x="36" y="72" width="6" height="6" fill="#0f172a" />
                <rect x="46" y="72" width="6" height="6" fill="#0f172a" />
                <rect x="56" y="72" width="6" height="6" fill="#0f172a" />

                <rect x="80" y="72" width="12" height="12" rx="2" fill="#0f172a" />
                <rect x="68" y="84" width="8" height="8" rx="1" fill="#0f172a" />
              </svg>
            </div>
          </div>

          <div class="text-[11px] text-slate-500">
            Scan dengan GoPay, OVO, Dana, BCA Mobile, Livin', atau mobile banking lainnya.
          </div>
          <div class="text-[10px] font-mono bg-slate-100 p-1.5 rounded-lg border border-slate-200">
            Ref ID: <span class="font-bold text-slate-800">{{ payment?.referenceNumber || order.paymentRef || 'QRIS-REF-PENDING' }}</span>
          </div>
        </div>

        <!-- 2. Virtual Account Simulator View -->
        <div v-else-if="order.paymentMethod === 'BANK_TRANSFER_VA'" class="space-y-3 bg-slate-950 p-4 rounded-xl border border-slate-800">
          <div class="flex items-center justify-between pb-2 border-b border-slate-800">
            <span class="text-xs font-semibold text-slate-300">Bank Transfer / Virtual Account</span>
            <span class="text-[10px] bg-indigo-500/20 text-indigo-300 px-2 py-0.5 rounded font-bold">MANDIRI / BCA VA</span>
          </div>
          <div class="space-y-1">
            <div class="text-[11px] text-slate-400">Nomor Virtual Account:</div>
            <div class="p-3 bg-slate-900 border border-slate-700 rounded-lg flex items-center justify-between">
              <span class="font-mono text-base font-bold text-amber-400 tracking-wider">
                {{ payment?.referenceNumber || order.paymentRef || '88009-829104812' }}
              </span>
              <button 
                class="text-[10px] bg-slate-800 hover:bg-slate-700 px-2 py-1 rounded text-slate-300 transition cursor-pointer"
                @click="alert('Nomor VA disalin!')"
              >
                Copy
              </button>
            </div>
          </div>
          <div class="text-[10px] text-slate-400 leading-relaxed bg-slate-900/50 p-2.5 rounded-lg">
            Petunjuk: Transfer tepat sesuai nominal <span class="text-emerald-400 font-semibold">{{ formatRupiah(order.totalAmount) }}</span> melalui ATM atau Mobile Banking.
          </div>
        </div>

        <!-- 3. Credit Card Simulator View -->
        <div v-else-if="order.paymentMethod === 'CREDIT_CARD'" class="space-y-3">
          <!-- Stylized Card Visual -->
          <div class="p-5 rounded-2xl bg-gradient-to-tr from-slate-950 via-indigo-950 to-slate-900 border border-indigo-500/30 shadow-xl relative overflow-hidden text-white space-y-4">
            <div class="flex justify-between items-center">
              <span class="text-[11px] font-mono tracking-widest text-indigo-300">MOCK VISA / MASTERCARD</span>
              <span class="text-sm font-bold italic">VISA</span>
            </div>
            <div class="font-mono text-base tracking-widest text-slate-200">
              4000 •••• •••• 8821
            </div>
            <div class="flex justify-between items-end text-[10px]">
              <div>
                <div class="text-slate-400 uppercase tracking-wider text-[8px]">Cardholder</div>
                <div class="font-semibold text-slate-200">{{ order.customerName }}</div>
              </div>
              <div>
                <div class="text-slate-400 uppercase tracking-wider text-[8px]">Expires</div>
                <div class="font-semibold text-slate-200">12/28</div>
              </div>
            </div>
          </div>
          <div class="text-[11px] text-slate-400 text-center">
            Simulasi 3DS OTP Authentication siap dieksekusi melalui simulator panel di bawah.
          </div>
        </div>

        <!-- Interactive Sandbox Simulation Panel -->
        <div class="pt-3 border-t border-slate-800 space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-indigo-400 flex items-center gap-1.5">
              <span>⚡</span> Kontrol Simulasi Gateway:
            </span>
            <span class="text-[10px] text-slate-400">Klik untuk test flow</span>
          </div>

          <div v-if="simulationMessage" class="p-2.5 rounded-lg bg-indigo-950/60 border border-indigo-800 text-xs text-indigo-200 text-center animate-in fade-in duration-150">
            {{ simulationMessage }}
          </div>

          <div class="grid grid-cols-3 gap-2">
            <!-- 1. Simulate Success -->
            <button 
              :disabled="isSimulating"
              @click="runSimulation('SETTLE')"
              class="px-3 py-2.5 rounded-xl text-xs font-bold bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white transition shadow-lg shadow-emerald-600/20 flex flex-col items-center justify-center gap-1 cursor-pointer active:scale-95"
            >
              <span class="text-sm">🟢</span>
              <span>Simulate Pay</span>
              <span class="text-[9px] font-normal opacity-80">(Success 200)</span>
            </button>

            <!-- 2. Simulate Expire -->
            <button 
              :disabled="isSimulating"
              @click="runSimulation('EXPIRE')"
              class="px-3 py-2.5 rounded-xl text-xs font-bold bg-amber-600 hover:bg-amber-500 disabled:opacity-50 text-white transition shadow-lg shadow-amber-600/20 flex flex-col items-center justify-center gap-1 cursor-pointer active:scale-95"
            >
              <span class="text-sm">⏱️</span>
              <span>Simulate Expire</span>
              <span class="text-[9px] font-normal opacity-80">(Timeout 15m)</span>
            </button>

            <!-- 3. Simulate Fail -->
            <button 
              :disabled="isSimulating"
              @click="runSimulation('FAIL')"
              class="px-3 py-2.5 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white transition shadow-lg shadow-rose-600/20 flex flex-col items-center justify-center gap-1 cursor-pointer active:scale-95"
            >
              <span class="text-sm">🔴</span>
              <span>Simulate Fail</span>
              <span class="text-[9px] font-normal opacity-80">(Declined)</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="px-6 py-3 border-t border-slate-800 bg-slate-950/60 flex items-center justify-between text-xs text-slate-400">
        <span>Status Order: <strong class="text-amber-400">{{ order.status }}</strong></span>
        <button 
          @click="emit('close')" 
          class="hover:text-white underline cursor-pointer"
        >
          Tutup Simulator
        </button>
      </div>
    </div>
  </div>
</template>
