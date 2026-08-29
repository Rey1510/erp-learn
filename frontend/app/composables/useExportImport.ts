export function useExportImport() {
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase || 'http://localhost:8080'
  const API_BASE = `${apiBase}/api`

  const isExporting = ref(false)
  const isImporting = ref(false)

  function downloadProductsCsv() {
    isExporting.value = true
    try {
      window.location.href = `${API_BASE}/export/products.csv`
    } finally {
      setTimeout(() => { isExporting.value = false }, 1500)
    }
  }

  function downloadOrdersCsv() {
    isExporting.value = true
    try {
      window.location.href = `${API_BASE}/export/orders.csv`
    } finally {
      setTimeout(() => { isExporting.value = false }, 1500)
    }
  }

  function downloadSampleTemplate() {
    const csvContent = 
      '\uFEFF' +
      'Nama Produk,SKU,Kategori,Harga,Stok\n' +
      'Mechanical Keyboard RGB,KEY-RGB-99,Accessories,1450000,15\n' +
      'UltraWide Monitor 34 Inch,MON-UW-34,Electronics,9800000,5\n' +
      'Ergonomic Leather Desk Chair,FRN-CHR-88,Furniture,4200000,8\n'

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.setAttribute('href', url)
    link.setAttribute('download', 'template_import_produk.csv')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }

  async function uploadProductsCsv(file: File) {
    isImporting.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)

      const res = await $fetch<{
        created: number
        updated: number
        totalProcessed: number
        errors: string[]
      }>(`${API_BASE}/import/products`, {
        method: 'POST',
        body: formData
      })
      return res
    } finally {
      isImporting.value = false
    }
  }

  return {
    isExporting,
    isImporting,
    downloadProductsCsv,
    downloadOrdersCsv,
    downloadSampleTemplate,
    uploadProductsCsv
  }
}
