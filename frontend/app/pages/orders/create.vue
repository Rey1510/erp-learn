<script setup lang="ts">
import type { Product } from '~/types/product'
import type { CreateOrderPayload, Order } from '~/types/order'

const router = useRouter()
const { products, pending: productsPending, formatRupiah, refresh: refreshProducts } = useProducts()
const { createOrder, formatDate } = useOrders()
const { t } = useI18n()
const { theme } = useTheme()

// State Form Customer
const customerName = ref('')
const customerEmail = ref('')
const isSubmitting = ref(false)

// State Receipt Modal on Success
const createdOrder = ref<Order | null>(null)
const isReceiptModalOpen = ref(false)

// State Search Katalog
const searchProduct = ref('')

// State Cart (Keranjang Belanja)
interface CartItem {
  product: Product
  quantity: number
}

const cart = ref<CartItem[]>([])

const availableProducts = computed(() => {
  if (!products.value) return []
  return products.value.filter(p => {
    const match = p.name.toLowerCase().includes(searchProduct.value.toLowerCase()) ||
                  p.sku.toLowerCase().includes(searchProduct.value.toLowerCase())
    return match && p.stock > 0
  })
})

function addToCart(product: Product) {
  const existing = cart.value.find(item => item.product.id === product.id)
  if (existing) {
    if (existing.quantity < product.stock) {
      existing.quantity++
    } else {
      alert(`Maksimum stok untuk ${product.name} adalah ${product.stock}`)
    }
  } else {
    cart.value.push({ product, quantity: 1 })
  }
}

function updateQuantity(productId: number, delta: number) {
  const item = cart.value.find(i => i.product.id === productId)
  if (!item) return

  const newQty = item.quantity + delta
  if (newQty <= 0) {
    cart.value = cart.value.filter(i => i.product.id !== productId)
  } else if (newQty > item.product.stock) {
    alert(`Stok hanya tersisa ${item.product.stock}`)
  } else {
    item.quantity = newQty
  }
}

function removeFromCart(productId: number) {
  cart.value = cart.value.filter(i => i.product.id !== productId)
}

// Grand Total
const grandTotal = computed(() => {
  return cart.value.reduce((acc, item) => acc + (item.product.price * item.quantity), 0)
})

const totalItemsCount = computed(() => {
  return cart.value.reduce((acc, item) => acc + item.quantity, 0)
})

async function submitOrder() {
  if (!customerName.value.trim()) {
    alert('Mohon isi nama customer!')
    return
  }
  if (cart.value.length === 0) {
    alert('Keranjang belanja masih kosong!')
    return
  }

  try {
    isSubmitting.value = true
    const payload: CreateOrderPayload = {
      customerName: customerName.value,
      customerEmail: customerEmail.value || undefined,
      items: cart.value.map(i => ({
        productId: i.product.id,
        quantity: i.quantity
      }))
    }

    const orderRes = await createOrder(payload)
    await refreshProducts() // Update sisa stok di katalog
    createdOrder.value = orderRes
    isReceiptModalOpen.value = true
  } catch (err: any) {
    alert('Gagal membuat transaksi: ' + (err.data?.error || err.message || err))
  } finally {
    isSubmitting.value = false
  }
}

function handleReceiptClosed() {
  isReceiptModalOpen.value = false
  router.push('/orders')
}
</script>

<template>
  <div class="space-y-6">
    <!-- Top Header -->
    <div class="flex items-center justify-between">
      <div>
        <NuxtLink to="/orders" class="text-xs text-indigo-500 hover:underline flex items-center gap-1 mb-1">
          &larr; {{ t('orders.title') }}
        </NuxtLink>
        <h2 class="text-xl font-bold" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
          {{ t('pos.title') }}
        </h2>
        <p class="text-xs" :class="theme === 'light' ? 'text-slate-600' : 'text-slate-400'">
          {{ t('pos.desc') }}
        </p>
      </div>
    </div>

    <!-- 2 Column Layout: Katalog Produk & Keranjang Checkout -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      <!-- Left: Katalog Produk (7 Cols) -->
      <div class="lg:col-span-7 space-y-4">
        <div 
          class="p-4 rounded-2xl border flex items-center justify-between gap-3 transition-colors"
          :class="theme === 'light' ? 'bg-white border-slate-200 shadow-sm' : 'bg-slate-900/60 border-slate-800'"
        >
          <input 
            v-model="searchProduct"
            type="text" 
            placeholder="Cari produk dari stok yang tersedia..."
            class="w-full border rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none transition"
            :class="theme === 'light' 
              ? 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400' 
              : 'bg-slate-950/80 border-slate-700/80 text-white placeholder-slate-500'"
          />
        </div>

        <div v-if="productsPending" class="p-12 text-center text-slate-400">
          Memuat katalog produk...
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div 
            v-for="p in availableProducts" 
            :key="p.id"
            class="p-4 rounded-2xl border transition group flex flex-col justify-between"
            :class="theme === 'light' 
              ? 'bg-white border-slate-200 hover:border-emerald-500 shadow-sm' 
              : 'bg-slate-900/80 border-slate-800 hover:border-emerald-500/40'"
          >
            <div>
              <div class="flex items-start justify-between gap-2">
                <span 
                  class="text-xs px-2 py-0.5 rounded-md border"
                  :class="theme === 'light' ? 'bg-slate-100 text-slate-700 border-slate-200' : 'bg-slate-800 text-slate-300 border-slate-700'"
                >
                  {{ p.category }}
                </span>
                <span class="text-xs font-medium text-emerald-500">
                  {{ t('pos.stockRemaining') }} {{ p.stock }}
                </span>
              </div>
              <h4 
                class="font-bold text-sm mt-2 leading-tight transition"
                :class="theme === 'light' ? 'text-slate-900 group-hover:text-emerald-600' : 'text-white group-hover:text-emerald-300'"
              >
                {{ p.name }}
              </h4>
              <p class="text-xs font-mono text-slate-400 mt-0.5">{{ p.sku }}</p>
            </div>

            <div 
              class="mt-4 pt-3 border-t flex items-center justify-between"
              :class="theme === 'light' ? 'border-slate-100' : 'border-slate-800/80'"
            >
              <span class="font-extrabold text-sm text-indigo-500">{{ formatRupiah(p.price) }}</span>
              <button 
                @click="addToCart(p)"
                class="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-md shadow-emerald-600/20 active:scale-95 transition cursor-pointer"
              >
                + Tambah
              </button>
            </div>
          </div>

          <div 
            v-if="availableProducts.length === 0" 
            class="col-span-2 p-8 text-center text-slate-400 rounded-2xl border"
            :class="theme === 'light' ? 'bg-white border-slate-200' : 'bg-slate-900/40 border-slate-800'"
          >
            Tidak ada produk yang cocok atau semua stok habis.
          </div>
        </div>
      </div>

      <!-- Right: Keranjang Checkout (5 Cols) -->
      <div 
        class="lg:col-span-5 border rounded-2xl p-5 shadow-2xl space-y-5 sticky top-20 transition-colors"
        :class="theme === 'light' ? 'bg-white border-slate-200 shadow-slate-200/50' : 'bg-slate-900/80 border-slate-800'"
      >
        <h3 
          class="font-bold text-base flex items-center justify-between pb-3 border-b"
          :class="theme === 'light' ? 'text-slate-900 border-slate-200' : 'text-white border-slate-800'"
        >
          <span>🛒 {{ t('pos.cartTitle') }}</span>
          <span class="text-xs font-normal text-emerald-500">{{ totalItemsCount }} Total Unit</span>
        </h3>

        <!-- Form Customer -->
        <div class="space-y-3">
          <div>
            <label class="block text-xs font-semibold mb-1" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              {{ t('pos.customerName') }} *
            </label>
            <input 
              v-model="customerName"
              type="text" 
              placeholder="Contoh: PT Jaya Abadi / Bpk. Budi"
              required
              class="w-full border rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none transition"
              :class="theme === 'light' 
                ? 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400' 
                : 'bg-slate-950 border-slate-700/80 text-white placeholder-slate-500'"
            />
          </div>
          <div>
            <label class="block text-xs font-semibold mb-1" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              {{ t('pos.customerEmail') }}
            </label>
            <input 
              v-model="customerEmail"
              type="email" 
              placeholder="budi@example.com"
              class="w-full border rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none transition"
              :class="theme === 'light' 
                ? 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400' 
                : 'bg-slate-950 border-slate-700/80 text-white placeholder-slate-500'"
            />
          </div>
        </div>

        <!-- List Items in Cart -->
        <div class="space-y-2 max-h-64 overflow-y-auto pr-1">
          <div 
            v-for="item in cart" 
            :key="item.product.id"
            class="p-3 rounded-xl border flex items-center justify-between gap-3 text-xs transition-colors"
            :class="theme === 'light' 
              ? 'bg-slate-50 border-slate-200' 
              : 'bg-slate-950/80 border-slate-800/80'"
          >
            <div class="min-w-0 flex-1">
              <p class="font-semibold truncate" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">{{ item.product.name }}</p>
              <p class="text-[11px] text-slate-400">{{ formatRupiah(item.product.price) }} / unit</p>
            </div>

            <!-- Qty Counter -->
            <div class="flex items-center gap-2">
              <button 
                @click="updateQuantity(item.product.id, -1)"
                class="w-6 h-6 rounded-lg font-bold flex items-center justify-center cursor-pointer border"
                :class="theme === 'light' ? 'bg-white hover:bg-slate-100 text-slate-800 border-slate-300' : 'bg-slate-800 hover:bg-slate-700 text-white border-slate-700'"
              >
                -
              </button>
              <span class="font-bold text-indigo-500 w-4 text-center">{{ item.quantity }}</span>
              <button 
                @click="updateQuantity(item.product.id, 1)"
                class="w-6 h-6 rounded-lg font-bold flex items-center justify-center cursor-pointer border"
                :class="theme === 'light' ? 'bg-white hover:bg-slate-100 text-slate-800 border-slate-300' : 'bg-slate-800 hover:bg-slate-700 text-white border-slate-700'"
              >
                +
              </button>
            </div>

            <div class="text-right">
              <p class="font-bold text-emerald-500">{{ formatRupiah(item.product.price * item.quantity) }}</p>
              <button 
                @click="removeFromCart(item.product.id)"
                class="text-[10px] text-rose-500 hover:underline cursor-pointer"
              >
                {{ t('home.delete') }}
              </button>
            </div>
          </div>

          <div 
            v-if="cart.length === 0" 
            class="py-8 text-center text-slate-400 border border-dashed rounded-xl"
            :class="theme === 'light' ? 'border-slate-300 bg-slate-50/50' : 'border-slate-800 bg-slate-950/40'"
          >
            {{ t('pos.emptyCart') }}
          </div>
        </div>

        <!-- Grand Total Summary -->
        <div 
          class="pt-4 border-t space-y-2"
          :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'"
        >
          <div class="flex items-center justify-between text-sm">
            <span class="text-slate-400">{{ t('pos.totalPayment') }}:</span>
            <span class="text-xl font-extrabold text-emerald-500">{{ formatRupiah(grandTotal) }}</span>
          </div>

          <button 
            @click="submitOrder"
            :disabled="isSubmitting || cart.length === 0"
            class="w-full py-3 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-500 hover:from-emerald-500 hover:to-teal-400 disabled:opacity-40 text-white font-bold text-sm shadow-lg shadow-emerald-600/30 transition cursor-pointer active:scale-95"
          >
            {{ isSubmitting ? 'Memproses...' : t('pos.checkoutBtn') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Receipt Modal on Success -->
    <OrderReceiptModal
      :is-open="isReceiptModalOpen"
      :order="createdOrder"
      :format-rupiah="formatRupiah"
      :format-date="formatDate"
      @close="handleReceiptClosed"
    />
  </div>
</template>
