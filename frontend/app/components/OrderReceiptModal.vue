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
}>()

function handlePrint() {
  if (!props.order) return

  // Create an isolated hidden iframe for clean 1-page printing
  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.right = '0'
  iframe.style.bottom = '0'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = '0'
  document.body.appendChild(iframe)

  const doc = iframe.contentWindow?.document
  if (!doc) return

  // Inject Print Styles via DOM to prevent Vite SFC parser collision
  const style = doc.createElement('style')
  style.textContent = `
    @page { size: auto; margin: 0; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    html, body {
      background: #ffffff;
      color: #000000;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 11px;
      width: 100%;
      margin: 0;
      padding: 0;
    }
    .receipt-wrapper {
      width: 80mm;
      max-width: 80mm;
      margin: 0 auto;
      padding: 12px 8px;
    }
    .text-center { text-align: center; }
    .text-right { text-align: right; }
    .text-slate-500 { color: #64748b; }
    .text-slate-600 { color: #475569; }
    .font-bold { font-weight: bold; }
    .font-semibold { font-weight: 600; }
    .border-b-dashed { border-bottom: 1px dashed #94a3b8; padding-bottom: 8px; margin-bottom: 8px; }
    .flex-between { display: flex; justify-content: space-between; margin-bottom: 4px; }
    table { width: 100%; border-collapse: collapse; }
    th { border-bottom: 1px solid #cbd5e1; padding-bottom: 4px; text-align: left; font-size: 11px; }
    td { padding: 4px 0; font-size: 11px; vertical-align: top; }
    .badge {
      display: inline-block;
      padding: 1px 6px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: bold;
      border: 1px solid #94a3b8;
    }
    .barcode {
      letter-spacing: 3px;
      font-size: 10px;
      color: #64748b;
      margin-top: 4px;
    }
  `
  doc.head.appendChild(style)

  // Inject Receipt Content
  const container = doc.createElement('div')
  container.className = 'receipt-wrapper'
  container.innerHTML = `
    <div class="text-center border-b-dashed">
      <h2 class="font-bold" style="font-size: 13px; letter-spacing: 1px;">ERP HUB POS</h2>
      <p style="font-size: 10px; color: #475569;">Jl. Gatot Subroto No. 36-38, Jakarta</p>
      <p style="font-size: 10px; color: #64748b;">Telp: (021) 5299-7777</p>
    </div>

    <div class="border-b-dashed">
      <div class="flex-between">
        <span class="text-slate-500">No. Order:</span>
        <span class="font-bold">${props.order.orderNumber}</span>
      </div>
      <div class="flex-between">
        <span class="text-slate-500">Tanggal:</span>
        <span>${props.formatDate(props.order.createdAt)}</span>
      </div>
      <div class="flex-between">
        <span class="text-slate-500">Customer:</span>
        <span class="font-semibold">${props.order.customerName}</span>
      </div>
      ${props.order.customerEmail ? `
      <div class="flex-between">
        <span class="text-slate-500">Email:</span>
        <span style="font-size: 10px;">${props.order.customerEmail}</span>
      </div>` : ''}
      <div class="flex-between">
        <span class="text-slate-500">Kasir:</span>
        <span>POS Terminal 01</span>
      </div>
    </div>

    <div class="border-b-dashed">
      <table>
        <thead>
          <tr>
            <th>Item</th>
            <th class="text-center" style="width: 30px;">Qty</th>
            <th class="text-right">Total</th>
          </tr>
        </thead>
        <tbody>
          ${props.order.items.map(item => `
            <tr>
              <td>
                <div class="font-semibold">${item.productName}</div>
                <div style="font-size: 10px; color: #64748b;">@ ${props.formatRupiah(item.unitPrice)}</div>
              </td>
              <td class="text-center font-bold">${item.quantity}</td>
              <td class="text-right font-bold">${props.formatRupiah(item.subtotal)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>

    <div class="border-b-dashed">
      <div class="flex-between">
        <span class="text-slate-600">Subtotal:</span>
        <span>${props.formatRupiah(props.order.totalAmount)}</span>
      </div>
      <div class="flex-between">
        <span class="text-slate-600">Pajak (0% PPn):</span>
        <span>Rp 0</span>
      </div>
      <div class="flex-between font-bold" style="font-size: 12px; margin-top: 4px; padding-top: 4px; border-top: 1px solid #cbd5e1;">
        <span>TOTAL:</span>
        <span>${props.formatRupiah(props.order.totalAmount)}</span>
      </div>
      <div class="flex-between" style="align-items: center; margin-top: 4px;">
        <span class="text-slate-600">Status:</span>
        <span class="badge">${props.order.status}</span>
      </div>
    </div>

    <div class="text-center" style="margin-top: 8px;">
      <p class="font-semibold" style="font-size: 10px;">*** TERIMA KASIH ***</p>
      <p style="font-size: 9px; color: #64748b; margin-top: 2px;">Barang yang sudah dibeli tidak dapat ditukar tanpa struk ini.</p>
      <div class="barcode">||||| | |||| ||| || |||| || ||||| |||||</div>
      <div style="font-size: 9px; color: #64748b; margin-top: 2px;">${props.order.orderNumber}</div>
    </div>
  `

  doc.body.appendChild(container)

  iframe.contentWindow?.focus()
  setTimeout(() => {
    iframe.contentWindow?.print()
    setTimeout(() => {
      if (document.body.contains(iframe)) {
        document.body.removeChild(iframe)
      }
    }, 1500)
  }, 250)
}
</script>

<template>
  <div 
    v-if="isOpen && order" 
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm"
    @click.self="emit('close')"
  >
    <!-- Modal Card (Screen View) -->
    <div class="bg-slate-900 border border-slate-800 w-full max-w-md rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
      <!-- Modal Header -->
      <div class="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
        <div class="flex items-center gap-2">
          <span class="text-lg">🧾</span>
          <h3 class="font-bold text-sm text-white">Struk Transaksi POS</h3>
        </div>
        <button 
          @click="emit('close')" 
          class="text-slate-400 hover:text-white text-lg font-bold transition cursor-pointer"
        >
          &times;
        </button>
      </div>

      <!-- Thermal Receipt Preview -->
      <div class="p-6 bg-slate-900 flex justify-center max-h-[70vh] overflow-y-auto">
        <div 
          id="receipt-print-area" 
          class="w-full max-w-[340px] bg-white text-slate-900 font-mono text-xs p-6 rounded-xl shadow-lg border border-slate-200"
        >
          <!-- Store Header -->
          <div class="text-center pb-4 border-b border-dashed border-slate-400">
            <div class="w-8 h-8 mx-auto mb-1 bg-slate-900 text-white rounded-lg flex items-center justify-center font-bold text-xs">
              EH
            </div>
            <h2 class="font-bold text-sm uppercase tracking-wider text-slate-950">ERP HUB POS</h2>
            <p class="text-[11px] text-slate-600">Jl. Gatot Subroto No. 36-38, Jakarta</p>
            <p class="text-[10px] text-slate-500">Telp: (021) 5299-7777</p>
          </div>

          <!-- Transaction Meta -->
          <div class="py-3 border-b border-dashed border-slate-400 space-y-1 text-[11px]">
            <div class="flex justify-between">
              <span class="text-slate-500">No. Order:</span>
              <span class="font-bold">{{ order.orderNumber }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">Tanggal:</span>
              <span>{{ formatDate(order.createdAt) }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">Customer:</span>
              <span class="font-semibold">{{ order.customerName }}</span>
            </div>
            <div v-if="order.customerEmail" class="flex justify-between">
              <span class="text-slate-500">Email:</span>
              <span class="text-[10px] text-slate-600 truncate max-w-[170px]">{{ order.customerEmail }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">Kasir:</span>
              <span>POS Terminal 01</span>
            </div>
          </div>

          <!-- Order Items Table -->
          <div class="py-3 border-b border-dashed border-slate-400">
            <table class="w-full text-left text-[11px]">
              <thead>
                <tr class="border-b border-slate-300 text-slate-600">
                  <th class="pb-1 font-semibold">Item</th>
                  <th class="pb-1 text-center font-semibold">Qty</th>
                  <th class="pb-1 text-right font-semibold">Total</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr v-for="item in order.items" :key="item.id">
                  <td class="py-1.5 pr-2">
                    <div class="font-semibold text-slate-900 leading-tight">{{ item.productName }}</div>
                    <div class="text-[10px] text-slate-500">@ {{ formatRupiah(item.unitPrice) }}</div>
                  </td>
                  <td class="py-1.5 text-center font-bold">{{ item.quantity }}</td>
                  <td class="py-1.5 text-right font-bold text-slate-900">{{ formatRupiah(item.subtotal) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Total Calculation -->
          <div class="py-3 border-b border-dashed border-slate-400 space-y-1.5 text-[11px]">
            <div class="flex justify-between">
              <span class="text-slate-600">Subtotal</span>
              <span>{{ formatRupiah(order.totalAmount) }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-600">Pajak (0% PPn)</span>
              <span>Rp 0</span>
            </div>
            <div class="flex justify-between text-xs font-extrabold text-slate-950 pt-1 border-t border-slate-300">
              <span>TOTAL</span>
              <span>{{ formatRupiah(order.totalAmount) }}</span>
            </div>
            <div class="flex justify-between items-center pt-1">
              <span class="text-slate-600">Status Pembayaran:</span>
              <span 
                class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider"
                :class="{
                  'bg-emerald-100 text-emerald-800 border border-emerald-300': order.status === 'PAID',
                  'bg-amber-100 text-amber-800 border border-amber-300': order.status === 'PENDING',
                  'bg-rose-100 text-rose-800 border border-rose-300': order.status === 'CANCELLED'
                }"
              >
                {{ order.status }}
              </span>
            </div>
          </div>

          <!-- Receipt Footer & Barcode Simulation -->
          <div class="pt-4 text-center space-y-2">
            <div class="text-[10px] text-slate-500 leading-tight">
              <p class="font-semibold text-slate-700">*** TERIMA KASIH ***</p>
              <p>Barang yang sudah dibeli tidak dapat ditukar atau dikembalikan tanpa struk ini.</p>
            </div>

            <!-- Visual Barcode Simulation -->
            <div class="pt-1 flex flex-col items-center">
              <div class="tracking-widest font-mono text-[9px] text-slate-400">
                ||||| | |||| ||| || |||| || ||||| |||||
              </div>
              <div class="text-[9px] text-slate-400 mt-0.5">
                {{ order.orderNumber }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Action Buttons Footer -->
      <div class="px-6 py-4 border-t border-slate-800 bg-slate-950/60 flex items-center justify-between gap-3">
        <button 
          @click="emit('close')" 
          class="px-4 py-2 rounded-xl text-xs font-medium bg-slate-800 hover:bg-slate-700 text-slate-300 transition cursor-pointer"
        >
          Tutup
        </button>
        <button 
          @click="handlePrint" 
          class="px-4 py-2 rounded-xl text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white transition shadow-lg shadow-emerald-600/30 flex items-center gap-1.5 cursor-pointer active:scale-95"
        >
          <span>🖨️</span> Cetak / Simpan PDF
        </button>
      </div>
    </div>
  </div>
</template>
