import type { StockMovement, RestockPayload, MovementType } from '~/types/audit'

export function useStockHistory() {
  const API_BASE = 'http://localhost:8080/api/stock-movements'

  const movements = ref<StockMovement[]>([])
  const pending = ref(false)
  const error = ref<string | null>(null)

  async function fetchMovements(productId?: number) {
    pending.value = true
    error.value = null
    try {
      const url = productId ? `${API_BASE}/product/${productId}` : API_BASE
      const data = await $fetch<StockMovement[]>(url)
      movements.value = data || []
    } catch (err: any) {
      error.value = err.message || 'Gagal memuat riwayat mutasi stok'
      movements.value = []
    } finally {
      pending.value = false
    }
  }

  async function restockProduct(payload: RestockPayload) {
    const res = await $fetch<StockMovement>(`${API_BASE}/restock`, {
      method: 'POST',
      body: payload
    })
    await fetchMovements(payload.productId)
    return res
  }

  function getMovementBadge(type: MovementType) {
    switch (type) {
      case 'RESTOCK':
        return {
          label: 'Restock Gudang',
          badgeClass: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
          icon: '📥'
        }
      case 'SALE':
        return {
          label: 'Penjualan POS',
          badgeClass: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
          icon: '🛒'
        }
      case 'CANCEL_RESTOCK':
        return {
          label: 'Batal Order (Restored)',
          badgeClass: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
          icon: '🔄'
        }
      case 'INITIAL':
        return {
          label: 'Setup Awal',
          badgeClass: 'bg-slate-500/10 text-slate-400 border-slate-500/20',
          icon: '📦'
        }
      default:
        return {
          label: 'Penyesuaian Manual',
          badgeClass: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
          icon: '⚙️'
        }
    }
  }

  function formatDate(dateStr: string) {
    if (!dateStr) return '-'
    const d = new Date(dateStr)
    return new Intl.DateTimeFormat('id-ID', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(d)
  }

  return {
    movements,
    pending,
    error,
    fetchMovements,
    restockProduct,
    getMovementBadge,
    formatDate
  }
}
