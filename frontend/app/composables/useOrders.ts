import { MOCK_ORDERS } from '~/utils/mockData'

// Shared reactive client-side mock store when backend is unavailable
const localMockOrders = ref<Order[]>(JSON.parse(JSON.stringify(MOCK_ORDERS)))

export function useOrders() {
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase || 'http://localhost:8080'
  const API_ORDERS = `${apiBase}/api/orders`

  // 1. Pagination & Sorting State
  const page = ref(0)
  const pageSize = ref(10)
  const sortBy = ref('createdAt')
  const sortDirection = ref<SortDirection>('desc')
  const searchQuery = ref('')
  const selectedStatus = ref('ALL')

  // 2. Fetch Server-Side Paginated Orders
  const { data: pagedData, pending, error, refresh } = useFetch<PageResponse<Order>>(`${API_ORDERS}/paged`, {
    headers: {
      'bypass-tunnel-reminder': 'true'
    },
    query: computed(() => ({
      page: page.value,
      size: pageSize.value,
      sortBy: sortBy.value,
      direction: sortDirection.value,
      search: searchQuery.value.trim() || undefined,
      status: selectedStatus.value !== 'ALL' ? selectedStatus.value : undefined
    })),
    watch: [page, pageSize, sortBy, sortDirection, searchQuery, selectedStatus]
  })

  // Also fetch all orders for global KPI aggregations
  const { data: serverAllOrders, refresh: refreshAll } = useFetch<Order[]>(API_ORDERS, {
    default: () => []
  })

  // Fallback computed mock orders when backend is unreachable
  const isUsingMock = computed(() => !!error.value || !pagedData.value?.content)

  const computedMockFiltered = computed(() => {
    let list = [...localMockOrders.value]
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      list = list.filter(o => 
        o.orderNumber.toLowerCase().includes(q) || 
        o.customerName.toLowerCase().includes(q) ||
        (o.customerEmail && o.customerEmail.toLowerCase().includes(q))
      )
    }
    if (selectedStatus.value !== 'ALL') {
      list = list.filter(o => o.status === selectedStatus.value)
    }
    list.sort((a, b) => {
      let valA = (a as any)[sortBy.value] || ''
      let valB = (b as any)[sortBy.value] || ''
      if (typeof valA === 'string') return sortDirection.value === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA)
      return sortDirection.value === 'asc' ? valA - valB : valB - valA
    })
    return list
  })

  // 3. Computed Order Lists & Pagination Metadata
  const allOrders = computed(() => {
    if (isUsingMock.value) return localMockOrders.value
    return serverAllOrders.value && serverAllOrders.value.length > 0 ? serverAllOrders.value : localMockOrders.value
  })

  const totalElements = computed(() => {
    if (isUsingMock.value) return computedMockFiltered.value.length
    return pagedData.value?.totalElements || computedMockFiltered.value.length
  })

  const totalPages = computed(() => {
    if (isUsingMock.value) return Math.ceil(totalElements.value / pageSize.value) || 1
    return pagedData.value?.totalPages || 1
  })

  const orders = computed(() => {
    if (isUsingMock.value) {
      const start = page.value * pageSize.value
      return computedMockFiltered.value.slice(start, start + pageSize.value)
    }
    return pagedData.value?.content || []
  })

  const isFirst = computed(() => page.value === 0)
  const isLast = computed(() => page.value >= totalPages.value - 1)

  // 100% Server-Side filtered orders
  const filteredOrders = computed(() => orders.value)

  // 4. Metrics Dashboard (Calculated from all orders)
  const totalRevenue = computed(() => {
    return allOrders.value
      .filter(o => o.status === 'PAID')
      .reduce((acc, o) => acc + o.totalAmount, 0)
  })

  const pendingOrdersCount = computed(() => {
    return allOrders.value.filter(o => o.status === 'PENDING').length
  })

  const paidOrdersCount = computed(() => {
    return allOrders.value.filter(o => o.status === 'PAID').length
  })

  // 5. Pagination & Sorting Handlers
  function setPage(n: number) {
    if (n >= 0 && n < totalPages.value) {
      page.value = n
    }
  }

  function nextPage() {
    if (!isLast.value) {
      page.value += 1
    }
  }

  function prevPage() {
    if (!isFirst.value) {
      page.value -= 1
    }
  }

  function setPageSize(size: number) {
    pageSize.value = size
    page.value = 0
  }

  function toggleSort(field: string) {
    if (sortBy.value === field) {
      sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
    } else {
      sortBy.value = field
      sortDirection.value = 'asc'
    }
    page.value = 0
  }

  // Reset page when filter changes
  watch([searchQuery, selectedStatus], () => {
    page.value = 0
  })

  // Smooth scroll to top of orders table on page change
  watch(page, () => {
    if (import.meta.client) {
      const el = document.getElementById('orders-table-section')
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      } else {
        window.scrollTo({ top: 0, behavior: 'smooth' })
      }
    }
  })

  // 6. Actions
  async function createOrder(payload: CreateOrderPayload) {
    try {
      const res = await $fetch<Order>(API_ORDERS, {
        method: 'POST',
        headers: {
          'bypass-tunnel-reminder': 'true'
        },
        body: payload
      })
      await Promise.all([refresh(), refreshAll()])
      return res
    } catch (err) {
      console.warn('[Orders] Backend offline, saving order to local mock store')
      const newId = (localMockOrders.value.length > 0 ? Math.max(...localMockOrders.value.map(o => o.id)) : 0) + 1
      const total = payload.items.reduce((acc, it) => acc + (it.quantity * (it.unitPrice || 0)), 0)
      const mockOrder: Order = {
        id: newId,
        orderNumber: `ORD-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${String(newId).padStart(4, '0')}`,
        customerName: payload.customerName || 'Pelanggan Umum',
        customerEmail: payload.customerEmail || 'demo@erp.com',
        totalAmount: total,
        status: payload.paymentMethod === 'CASH' ? 'PAID' : 'PENDING',
        paymentMethod: payload.paymentMethod || 'CASH',
        paymentRef: `DEMO-REF-${newId}`,
        createdAt: new Date().toISOString(),
        items: payload.items.map((it, idx) => ({
          id: idx + 1,
          productId: it.productId,
          productName: it.productName || `Produk #${it.productId}`,
          quantity: it.quantity,
          unitPrice: it.unitPrice || 0,
          subtotal: it.quantity * (it.unitPrice || 0)
        }))
      }
      localMockOrders.value.unshift(mockOrder)
      return mockOrder
    }
  }

  async function updateOrderStatus(orderId: number, status: 'PAID' | 'CANCELLED' | 'PENDING') {
    try {
      await $fetch(`${API_ORDERS}/${orderId}/status`, {
        method: 'PUT',
        headers: {
          'bypass-tunnel-reminder': 'true'
        },
        body: { status }
      })
      await Promise.all([refresh(), refreshAll()])
    } catch (err) {
      console.warn('[Orders] Backend offline, updating status in local mock store')
      const target = localMockOrders.value.find(o => o.id === orderId)
      if (target) {
        target.status = status
      }
    }
  }

  async function reseedOrders() {
    try {
      await $fetch(`${API_ORDERS}/reseed`, {
        method: 'POST',
        headers: {
          'bypass-tunnel-reminder': 'true'
        }
      })
      await Promise.all([refresh(), refreshAll()])
    } catch (err) {
      console.warn('[Orders] Backend offline, resetting local mock store')
      localMockOrders.value = JSON.parse(JSON.stringify(MOCK_ORDERS))
    }
  }

  function formatRupiah(val: number) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0
    }).format(val)
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
    orders,
    allOrders,
    filteredOrders,
    searchQuery,
    selectedStatus,
    pending,
    error,
    refresh: () => Promise.all([refresh(), refreshAll()]),
    page,
    pageSize,
    sortBy,
    sortDirection,
    totalElements,
    totalPages,
    isFirst,
    isLast,
    totalRevenue,
    pendingOrdersCount,
    paidOrdersCount,
    setPage,
    nextPage,
    prevPage,
    setPageSize,
    toggleSort,
    createOrder,
    updateOrderStatus,
    reseedOrders,
    formatRupiah,
    formatDate
  }
}
