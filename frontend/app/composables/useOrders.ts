import type { Order, CreateOrderPayload } from '~/types/order'
import type { PageResponse, SortDirection } from '~/types/pagination'

export function useOrders() {
  const API_ORDERS = 'http://localhost:8080/api/orders'

  // 1. Pagination & Sorting State
  const page = ref(0)
  const pageSize = ref(10)
  const sortBy = ref('createdAt')
  const sortDirection = ref<SortDirection>('desc')
  const searchQuery = ref('')
  const selectedStatus = ref('ALL')

  // 2. Fetch Server-Side Paginated Orders
  const { data: pagedData, pending, error, refresh } = useFetch<PageResponse<Order>>(`${API_ORDERS}/paged`, {
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
  const { data: allOrders, refresh: refreshAll } = useFetch<Order[]>(API_ORDERS, {
    default: () => []
  })

  // 3. Computed Order Lists & Pagination Metadata
  const orders = computed(() => pagedData.value?.content || [])
  const totalElements = computed(() => pagedData.value?.totalElements || 0)
  const totalPages = computed(() => pagedData.value?.totalPages || 1)
  const isFirst = computed(() => pagedData.value?.first ?? true)
  const isLast = computed(() => pagedData.value?.last ?? true)

  // 100% Server-Side filtered orders
  const filteredOrders = computed(() => orders.value)

  // 4. Metrics Dashboard (Calculated from all orders)
  const totalRevenue = computed(() => {
    if (!allOrders.value) return 0
    return allOrders.value
      .filter(o => o.status === 'PAID')
      .reduce((acc, o) => acc + o.totalAmount, 0)
  })

  const pendingOrdersCount = computed(() => {
    if (!allOrders.value) return 0
    return allOrders.value.filter(o => o.status === 'PENDING').length
  })

  const paidOrdersCount = computed(() => {
    if (!allOrders.value) return 0
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
    const res = await $fetch<Order>(API_ORDERS, {
      method: 'POST',
      body: payload
    })
    await Promise.all([refresh(), refreshAll()])
    return res
  }

  async function updateOrderStatus(orderId: number, status: 'PAID' | 'CANCELLED' | 'PENDING') {
    await $fetch(`${API_ORDERS}/${orderId}/status`, {
      method: 'PUT',
      body: { status }
    })
    await Promise.all([refresh(), refreshAll()])
  }

  async function reseedOrders() {
    await $fetch(`${API_ORDERS}/reseed`, {
      method: 'POST'
    })
    await Promise.all([refresh(), refreshAll()])
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
