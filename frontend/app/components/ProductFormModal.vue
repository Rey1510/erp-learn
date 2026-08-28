<script setup lang="ts">
import type { Product, ProductFormData } from '~/types/product'

const props = defineProps<{
  isOpen: boolean
  isEditing: boolean
  isSubmitting: boolean
  initialData?: Product | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit', payload: ProductFormData): void
}>()

const { theme } = useTheme()

const form = ref<ProductFormData>({
  name: '',
  sku: '',
  category: 'Electronics',
  price: 0,
  stock: 1
})

const categories = ['Electronics', 'Accessories', 'Furniture', 'Audio', 'Stationery']

watch(() => props.initialData, (newVal) => {
  if (newVal && props.isEditing) {
    form.value = {
      name: newVal.name,
      sku: newVal.sku,
      category: newVal.category,
      price: newVal.price,
      stock: newVal.stock
    }
  } else {
    form.value = {
      name: '',
      sku: `SKU-${Date.now().toString().slice(-4)}`,
      category: 'Electronics',
      price: 0,
      stock: 1
    }
  }
}, { immediate: true })

function handleSubmit() {
  if (!form.value.name.trim() || form.value.price < 0 || form.value.stock < 0) {
    alert('Mohon lengkapi formulir dengan data yang valid.')
    return
  }
  emit('submit', { ...form.value })
}
</script>

<template>
  <div 
    v-if="isOpen" 
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm"
    @click.self="emit('close')"
  >
    <div 
      class="border w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-150 transition-colors"
      :class="theme === 'light' ? 'bg-white border-slate-200 text-slate-800' : 'bg-slate-900 border-slate-800 text-slate-100'"
    >
      <div 
        class="px-6 py-4 border-b flex items-center justify-between"
        :class="theme === 'light' ? 'bg-slate-50 border-slate-200' : 'bg-slate-950/40 border-slate-800'"
      >
        <h3 class="font-bold text-base" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
          {{ isEditing ? 'Edit Data Master Produk' : 'Tambah Produk Baru (ke PostgreSQL)' }}
        </h3>
        <button 
          @click="emit('close')" 
          class="text-slate-400 hover:text-slate-600 text-xl font-bold cursor-pointer"
        >
          &times;
        </button>
      </div>

      <form @submit.prevent="handleSubmit" class="p-6 space-y-4">
        <!-- Nama Produk -->
        <div>
          <label class="block text-xs font-semibold mb-1.5" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
            Nama Produk
          </label>
          <input 
            v-model="form.name" 
            type="text" 
            placeholder="Contoh: MacBook Pro M3 Max"
            required
            class="w-full border rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none transition"
            :class="theme === 'light' ? 'bg-slate-50 border-slate-300 text-slate-900' : 'bg-slate-950 border-slate-700/80 text-white'"
          />
        </div>

        <!-- SKU & Kategori -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-semibold mb-1.5" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              SKU (Kode Barang)
            </label>
            <input 
              v-model="form.sku" 
              type="text" 
              required
              class="w-full border rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none transition"
              :class="theme === 'light' ? 'bg-slate-50 border-slate-300 text-slate-900' : 'bg-slate-950 border-slate-700/80 text-white'"
            />
          </div>
          <div>
            <label class="block text-xs font-semibold mb-1.5" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              Kategori
            </label>
            <select 
              v-model="form.category" 
              class="w-full border rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none transition cursor-pointer"
              :class="theme === 'light' ? 'bg-slate-50 border-slate-300 text-slate-900' : 'bg-slate-950 border-slate-700/80 text-white'"
            >
              <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
            </select>
          </div>
        </div>

        <!-- Harga Satuan & Stok (Stok hanya muncul saat Tambah Baru) -->
        <div class="grid" :class="isEditing ? 'grid-cols-1' : 'grid-cols-2 gap-4'">
          <div>
            <label class="block text-xs font-semibold mb-1.5" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              Harga Satuan (IDR)
            </label>
            <input 
              v-model.number="form.price" 
              type="number" 
              min="0"
              required
              class="w-full border rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none transition"
              :class="theme === 'light' ? 'bg-slate-50 border-slate-300 text-slate-900' : 'bg-slate-950 border-slate-700/80 text-white'"
            />
          </div>
          <div v-if="!isEditing">
            <label class="block text-xs font-semibold mb-1.5" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              Stok Awal
            </label>
            <input 
              v-model.number="form.stock" 
              type="number" 
              min="0"
              required
              class="w-full border rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none transition"
              :class="theme === 'light' ? 'bg-slate-50 border-slate-300 text-slate-900' : 'bg-slate-950 border-slate-700/80 text-white'"
            />
          </div>
        </div>

        <div v-if="isEditing" class="p-3 rounded-xl border text-xs flex items-center gap-2" :class="theme === 'light' ? 'bg-indigo-50 border-indigo-200 text-indigo-800' : 'bg-indigo-950/40 border-indigo-500/30 text-indigo-300'">
          <span>ℹ️</span>
          <span>Untuk menambah/mengurangi stok, gunakan menu <strong>Riwayat Mutasi</strong> di tabel.</span>
        </div>

        <div class="pt-4 flex justify-end gap-3 border-t" :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'">
          <button 
            type="button" 
            @click="emit('close')"
            class="px-4 py-2 rounded-xl text-xs font-medium border transition cursor-pointer"
            :class="theme === 'light' ? 'text-slate-600 hover:bg-slate-100 border-slate-300' : 'text-slate-400 hover:text-white border-slate-700'"
          >
            Batal
          </button>
          <button 
            type="submit" 
            :disabled="isSubmitting"
            class="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium shadow-lg shadow-indigo-600/30 transition disabled:opacity-50 flex items-center gap-2 cursor-pointer active:scale-95"
          >
            <span v-if="isSubmitting" class="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
            <span>{{ isSubmitting ? 'Menyimpan...' : 'Simpan Produk' }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
