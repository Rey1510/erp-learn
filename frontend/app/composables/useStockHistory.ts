import type { StockMovement, RestockPayload, MovementType } from '~/types/audit'

export function useStockHistory() {
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase || 'http://localhost:8080'
  const API_BASE = `${apiBase}/api/stock-movements`

  const movements = ref<StockMovement[]>([])
  const pending = ref(false)
  const error = ref<string | null>(null)

  const localMovements = ref<StockMovement[]>([
    {
      id: 1,
      productId: 1,
      productName: 'MacBook Pro M3 Max 16"',
      type: 'RESTOCK',
      quantityChange: 10,
      previousStock: 4,
      currentStock: 14,
      notes: 'Initial Demo Stock Inflow',
      createdAt: new Date().toISOString()
    }
  ])

  async function fetchMovements(productId?: number) {
    pending.value = true
    error.value = null
    try {
      const url = productId ? `${API_BASE}/product/${productId}` : API_BASE
      const data = await $fetch<StockMovement[]>(url, {
        headers: { 'bypass-tunnel-reminder': 'true' }
      })
      movements.value = data || []
    } catch (err: any) {
      // Fallback to local mock movements
      movements.value = productId 
        ? localMovements.value.filter(m => m.productId === productId)
        : localMovements.value
    } finally {
      pending.value = false
    }
  }

  async function restockProduct(payload: RestockPayload) {
    try {
      const res = await $fetch<StockMovement>(`${API_BASE}/restock`, {
        method: 'POST',
        headers: { 'bypass-tunnel-reminder': 'true' },
        body: payload
      })
      await fetchMovements(payload.productId)
      return res
    } catch (err) {
      console.warn('[StockHistory] Backend offline, recording restock to mock history')
      const mockMovement: StockMovement = {
        id: localMovements.value.length + 1,
        productId: payload.productId,
        productName: `Produk #${payload.productId}`,
        type: 'RESTOCK',
        quantityChange: payload.quantity,
        previousStock: 0,
        currentStock: payload.quantity,
        notes: payload.notes || 'Restock Standalone Demo',
        createdAt: new Date().toISOString()
      }
      localMovements.value.unshift(mockMovement)
      await fetchMovements(payload.productId)
      return mockMovement
    }
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
