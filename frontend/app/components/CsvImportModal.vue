<script setup lang="ts">
const props = defineProps<{
  isOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'imported'): void
}>()

const { isImporting, downloadSampleTemplate, uploadProductsCsv } = useExportImport()
const { theme } = useTheme()

interface ParsedRow {
  lineNumber: number
  name: string
  sku: string
  category: string
  rawPrice: string
  rawStock: string
  price: number | null
  stock: number | null
  errors: string[]
  priceError: boolean
  stockError: boolean
  skuError: boolean
  nameError: boolean
  isValid: boolean
}

const selectedFile = ref<File | null>(null)
const parsedRows = ref<ParsedRow[]>([])
const importResult = ref<{
  created: number
  updated: number
  totalProcessed: number
  errors: string[]
} | null>(null)

const invalidRowsCount = computed(() => parsedRows.value.filter(r => !r.isValid).length)
const validRowsCount = computed(() => parsedRows.value.filter(r => r.isValid).length)
const hasValidationErrors = computed(() => parsedRows.value.length === 0 || invalidRowsCount.value > 0)

watch(() => props.isOpen, (open) => {
  if (open) {
    selectedFile.value = null
    parsedRows.value = []
    importResult.value = null
  }
})

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    processFile(target.files[0])
  }
}

function handleDrop(e: DragEvent) {
  if (e.dataTransfer?.files && e.dataTransfer.files.length > 0) {
    processFile(e.dataTransfer.files[0])
  }
}

function processFile(file: File) {
  selectedFile.value = file
  importResult.value = null
  parsedRows.value = []

  const reader = new FileReader()
  reader.onload = (event) => {
    const text = event.target?.result as string
    if (!text) return

    const lines = text.split(/\r\n|\n/).filter(line => line.trim().length > 0)
    if (lines.length <= 1) {
      alert('File CSV kosong atau hanya berisi baris header.')
      return
    }

    const rows: ParsedRow[] = []
    const seenSkus = new Set<string>()

    // Skip header line (index 0)
    for (let i = 1; i < lines.length; i++) {
      const lineNum = i + 1
      const rawLine = lines[i]

      // Simple RFC-compliant line parser handling basic quotes and commas
      const cols: string[] = []
      let curr = ''
      let inQuotes = false

      for (let c = 0; c < rawLine.length; c++) {
        const char = rawLine[c]
        if (char === '"') {
          inQuotes = !inQuotes
        } else if (char === ',' && !inQuotes) {
          cols.push(curr.trim())
          curr = ''
        } else {
          curr += char
        }
      }
      cols.push(curr.trim())

      const name = cols[0] || ''
      const sku = cols[1] || ''
      const category = cols[2] || ''
      const rawPrice = cols[3] || ''
      const rawStock = cols[4] || ''

      const rowErrors: string[] = []
      let nameError = false
      let skuError = false
      let priceError = false
      let stockError = false

      // Validate Name
      if (!name.trim()) {
        nameError = true
        rowErrors.push('Nama produk tidak boleh kosong')
      }

      // Validate SKU
      if (!sku.trim()) {
        skuError = true
        rowErrors.push('SKU tidak boleh kosong')
      } else {
        if (seenSkus.has(sku.trim().toUpperCase())) {
          skuError = true
          rowErrors.push(`Duplikat SKU "${sku}" dalam file`)
        } else {
          seenSkus.add(sku.trim().toUpperCase())
        }
      }

      // Validate Price
      const cleanPrice = rawPrice.replace(/[^0-9.-]+/g, '')
      const numPrice = Number(cleanPrice)
      if (!rawPrice.trim() || isNaN(numPrice) || numPrice <= 0) {
        priceError = true
        rowErrors.push('Harga harus > 0')
      }

      // Validate Stock (must be non-negative integer >= 0)
      const cleanStock = rawStock.trim()
      const numStock = Number(cleanStock)
      if (cleanStock === '' || isNaN(numStock) || !Number.isInteger(numStock) || numStock < 0) {
        stockError = true
        rowErrors.push('Stok tidak boleh negatif')
      }

      rows.push({
        lineNumber: lineNum,
        name: name || '-',
        sku: sku || '-',
        category: category || 'Electronics',
        rawPrice,
        rawStock,
        price: isNaN(numPrice) ? null : numPrice,
        stock: isNaN(numStock) ? null : numStock,
        errors: rowErrors,
        nameError,
        skuError,
        priceError,
        stockError,
        isValid: rowErrors.length === 0
      })
    }

    parsedRows.value = rows
  }
  reader.readAsText(file)
}

async function handleImport() {
  if (!selectedFile.value || hasValidationErrors.value) return
  try {
    const result = await uploadProductsCsv(selectedFile.value)
    importResult.value = result
    emit('imported')
  } catch (err: any) {
    alert('Gagal mengimpor file: ' + (err.data?.error || err.message || err))
  }
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm" @click.self="emit('close')">
    <div 
      class="border w-full max-w-2xl rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-150 flex flex-col max-h-[90vh] transition-colors"
      :class="theme === 'light' ? 'bg-white border-slate-200 text-slate-800' : 'bg-slate-900 border-slate-800 text-slate-100'"
    >
      <!-- Header -->
      <div 
        class="p-5 border-b flex items-center justify-between shrink-0"
        :class="theme === 'light' ? 'bg-slate-50 border-slate-200' : 'bg-slate-950/40 border-slate-800'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-indigo-600/20 text-indigo-500 border border-indigo-500/30 flex items-center justify-center text-lg font-bold">
            📥
          </div>
          <div>
            <h3 class="font-bold text-base" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">Import Katalog Produk (.CSV)</h3>
            <p class="text-xs text-slate-400 mt-0.5">Tambah atau perbarui puluhan produk secara batch dengan validasi otomatis</p>
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

      <!-- Body -->
      <div class="p-6 overflow-y-auto space-y-5 flex-1">
        <!-- Template Download Section -->
        <div 
          class="p-4 rounded-xl border flex items-center justify-between gap-4 transition-colors"
          :class="theme === 'light' ? 'bg-slate-50 border-slate-200' : 'bg-slate-950/60 border-slate-800'"
        >
          <div>
            <p class="text-xs font-semibold" :class="theme === 'light' ? 'text-slate-800' : 'text-slate-200'">Belum punya format CSV yang sesuai?</p>
            <p class="text-[11px] text-slate-400 mt-0.5">Unduh file template resmi dengan contoh kolom Nama, SKU, Kategori, Harga, Stok.</p>
          </div>
          <button 
            @click="downloadSampleTemplate"
            class="shrink-0 px-3 py-1.5 rounded-lg text-xs font-medium border transition flex items-center gap-1.5 cursor-pointer"
            :class="theme === 'light' ? 'bg-white hover:bg-slate-100 text-slate-700 border-slate-300 shadow-sm' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'"
          >
            <span>📥</span> Download Template
          </button>
        </div>

        <!-- Drag & Drop Zone -->
        <div 
          @dragover.prevent 
          @drop="handleDrop"
          class="border-2 border-dashed rounded-2xl p-5 text-center transition cursor-pointer relative"
          :class="theme === 'light' 
            ? 'border-slate-300 hover:border-indigo-500 bg-slate-50/50' 
            : 'border-slate-700 hover:border-indigo-500 bg-slate-950/40'"
        >
          <input 
            type="file" 
            accept=".csv" 
            @change="handleFileChange"
            class="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
          />
          <div class="space-y-1.5">
            <div class="text-3xl">📄</div>
            <p v-if="!selectedFile" class="text-xs font-medium" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              Drag &amp; Drop file <strong class="text-indigo-500">.CSV</strong> ke sini, atau <span class="text-indigo-500 underline">Pilih File</span>
            </p>
            <p v-else class="text-xs font-semibold text-emerald-500 flex items-center justify-center gap-1.5">
              <span>✓ File Dipilih:</span> {{ selectedFile.name }} ({{ (selectedFile.size / 1024).toFixed(1) }} KB)
            </p>
            <p class="text-[11px] text-slate-400">Mendukung format UTF-8 CSV dan pemisah koma (,)</p>
          </div>
        </div>

        <!-- Validation Summary Alert -->
        <div v-if="parsedRows.length > 0 && !importResult">
          <!-- Error Alert Banner -->
          <div v-if="invalidRowsCount > 0" class="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2.5">
            <span class="text-base leading-none mt-0.5">⚠️</span>
            <div>
              <p class="font-bold text-rose-900">Ditemukan {{ invalidRowsCount }} dari {{ parsedRows.length }} baris yang tidak lolos validasi!</p>
              <p class="text-[11px] text-rose-700 mt-0.5">
                Tombol import dinonaktifkan. Silakan perbaiki baris yang ditandai merah (misal: stok negatif, harga &le; 0, atau SKU kosong) lalu upload ulang.
              </p>
            </div>
          </div>

          <!-- All Valid Success Banner -->
          <div v-else class="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2.5">
            <span class="text-base leading-none">✅</span>
            <div>
              <p class="font-bold text-emerald-900">Semua {{ parsedRows.length }} baris lolos validasi!</p>
              <p class="text-[11px] text-emerald-700 mt-0.5">Data siap diimpor dan disimpan ke database PostgreSQL.</p>
            </div>
          </div>
        </div>

        <!-- Preview Rows Table with Cell Highlighting -->
        <div v-if="parsedRows.length > 0 && !importResult" class="space-y-2">
          <div class="flex items-center justify-between text-xs">
            <span class="font-semibold flex items-center gap-1.5" :class="theme === 'light' ? 'text-slate-800' : 'text-slate-300'">
              <span>👁️</span> Pratinjau &amp; Hasil Validasi Data ({{ parsedRows.length }} Baris):
            </span>
            <span class="text-[11px] text-slate-400">
              Valid: <strong class="text-emerald-500">{{ validRowsCount }}</strong> | 
              Error: <strong class="text-rose-500">{{ invalidRowsCount }}</strong>
            </span>
          </div>

          <div class="overflow-x-auto max-h-60 rounded-xl border" :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'">
            <table class="w-full text-left text-[11px]" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              <thead 
                class="uppercase border-b sticky top-0 z-10 select-none"
                :class="theme === 'light' ? 'bg-slate-100 text-slate-600 border-slate-200' : 'bg-slate-950 text-slate-400 border-slate-800'"
              >
                <tr>
                  <th class="px-3 py-2 text-center w-12">Baris</th>
                  <th class="px-3 py-2">Nama Produk</th>
                  <th class="px-3 py-2">SKU</th>
                  <th class="px-3 py-2">Kategori</th>
                  <th class="px-3 py-2 text-right">Harga</th>
                  <th class="px-3 py-2 text-center">Stok</th>
                  <th class="px-3 py-2 text-center">Status Validasi</th>
                </tr>
              </thead>
              <tbody class="divide-y" :class="theme === 'light' ? 'divide-slate-200 bg-white' : 'divide-slate-800/60 bg-slate-900/50'">
                <tr 
                  v-for="row in parsedRows" 
                  :key="row.lineNumber"
                  :class="row.isValid ? (theme === 'light' ? 'hover:bg-slate-50' : 'hover:bg-slate-800/30') : 'bg-rose-50 hover:bg-rose-100/70'"
                >
                  <td class="px-3 py-2 text-center text-slate-400 font-mono">#{{ row.lineNumber }}</td>

                  <!-- Name -->
                  <td class="px-3 py-2 font-medium" :class="row.nameError ? 'text-rose-600 font-bold underline' : (theme === 'light' ? 'text-slate-900' : 'text-white')">
                    {{ row.name }}
                  </td>

                  <!-- SKU -->
                  <td class="px-3 py-2 font-mono" :class="row.skuError ? 'text-rose-600 font-bold bg-rose-100 rounded px-1' : 'text-indigo-500 font-semibold'">
                    {{ row.sku }}
                  </td>

                  <!-- Category -->
                  <td class="px-3 py-2 text-slate-500">{{ row.category }}</td>

                  <!-- Price -->
                  <td class="px-3 py-2 text-right font-mono" :class="row.priceError ? 'text-rose-600 font-bold bg-rose-100 rounded px-1' : (theme === 'light' ? 'text-slate-800' : 'text-slate-200')">
                    {{ row.price !== null ? row.price.toLocaleString('id-ID') : row.rawPrice }}
                  </td>

                  <!-- Stock -->
                  <td class="px-3 py-2 text-center font-mono" :class="row.stockError ? 'text-rose-600 font-bold bg-rose-100 border border-rose-300 rounded px-1' : (theme === 'light' ? 'text-slate-800' : 'text-slate-200')">
                    {{ row.rawStock }}
                  </td>

                  <!-- Validation Pill & Details -->
                  <td class="px-3 py-2 text-center whitespace-nowrap">
                    <span 
                      v-if="row.isValid" 
                      class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-500 border border-emerald-500/30"
                    >
                      ✓ Valid
                    </span>
                    <div v-else class="flex flex-col items-center gap-0.5">
                      <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-500 border border-rose-500/30">
                        ✕ Error
                      </span>
                      <span class="text-[9px] text-rose-500 max-w-[140px] truncate" :title="row.errors.join('; ')">
                        {{ row.errors[0] }}
                      </span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Import Success/Error Result Box -->
        <div v-if="importResult" class="p-4 rounded-xl border" :class="importResult.errors.length > 0 ? 'bg-amber-50 border-amber-200 text-amber-900' : 'bg-emerald-50 border-emerald-200 text-emerald-900'">
          <div class="flex items-center gap-2 font-bold text-sm">
            <span>{{ importResult.errors.length > 0 ? '⚠️' : '🎉' }}</span>
            <span>Hasil Import Selesai:</span>
          </div>
          <div class="grid grid-cols-3 gap-2 mt-3 text-xs">
            <div class="p-2 rounded-lg border text-center" :class="theme === 'light' ? 'bg-white border-slate-200' : 'bg-slate-900/80 border-slate-800'">
              <p class="text-[10px] text-slate-400">Produk Baru</p>
              <p class="text-base font-bold text-emerald-500 mt-0.5">+{{ importResult.created }}</p>
            </div>
            <div class="p-2 rounded-lg border text-center" :class="theme === 'light' ? 'bg-white border-slate-200' : 'bg-slate-900/80 border-slate-800'">
              <p class="text-[10px] text-slate-400">Produk Diupdate</p>
              <p class="text-base font-bold text-indigo-500 mt-0.5">{{ importResult.updated }}</p>
            </div>
            <div class="p-2 rounded-lg border text-center" :class="theme === 'light' ? 'bg-white border-slate-200' : 'bg-slate-900/80 border-slate-800'">
              <p class="text-[10px] text-slate-400">Total Diproses</p>
              <p class="text-base font-bold mt-0.5" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">{{ importResult.totalProcessed }}</p>
            </div>
          </div>

          <div v-if="importResult.errors.length > 0" class="mt-3 space-y-1 text-[11px] text-rose-600">
            <p class="font-semibold">Catatan / Peringatan:</p>
            <ul class="list-disc pl-4 space-y-0.5">
              <li v-for="(err, idx) in importResult.errors" :key="idx">{{ err }}</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div 
        class="p-4 border-t flex items-center justify-between shrink-0"
        :class="theme === 'light' ? 'border-slate-200 bg-slate-50/80' : 'border-slate-800 bg-slate-950/40'"
      >
        <button 
          @click="emit('close')" 
          class="px-4 py-2 rounded-xl text-xs font-medium border transition cursor-pointer"
          :class="theme === 'light' ? 'text-slate-600 hover:bg-slate-100 border-slate-300' : 'text-slate-400 hover:text-white hover:bg-slate-800 border-slate-700'"
        >
          {{ importResult ? 'Tutup' : 'Batal' }}
        </button>

        <button 
          v-if="!importResult"
          @click="handleImport"
          :disabled="!selectedFile || isImporting || hasValidationErrors"
          class="px-5 py-2.5 rounded-xl text-xs font-semibold text-white shadow-lg transition flex items-center gap-1.5 cursor-pointer active:scale-95"
          :class="hasValidationErrors ? 'bg-slate-300 text-slate-500 cursor-not-allowed opacity-60' : 'bg-indigo-600 hover:bg-indigo-500 shadow-indigo-600/30'"
        >
          <span v-if="isImporting">Mengimpor Data...</span>
          <span v-else-if="hasValidationErrors && parsedRows.length > 0">Perbaiki Error CSV untuk Melanjutkan</span>
          <span v-else-if="!selectedFile">Pilih File CSV Terlebih Dahulu</span>
          <span v-else>Mulai Import ({{ validRowsCount }} Produk Valid)</span>
        </button>
      </div>
    </div>
  </div>
</template>
