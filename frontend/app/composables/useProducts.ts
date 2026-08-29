import { MOCK_PRODUCTS } from '~/utils/mockData'

// Shared reactive client-side mock store when backend is unavailable
const localMockProducts = ref<Product[]>(JSON.parse(JSON.stringify(MOCK_PRODUCTS)))

export function useProducts() {
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase || 'http://localhost:8080'
  const API_BASE = `${apiBase}/api/products`

  // 1. Pagination & Sorting State
  const page = ref(0)
  const pageSize = ref(10)
  const sortBy = ref('id')
  const sortDirection = ref<SortDirection>('desc')
  const searchQuery = ref('')
  const selectedCategory = ref('ALL')
  const selectedStatus = ref('ALL')

  const categories = ['ALL', 'Electronics', 'Accessories', 'Furniture', 'Audio', 'Stationery']

  // 2. Fetch Server-Side Paginated Products
  const { data: pagedData, pending, error, refresh } = useFetch<PageResponse<Product>>(`${API_BASE}/paged`, {
    headers: {
      'bypass-tunnel-reminder': 'true'
    },
    query: computed(() => ({
      page: page.value,
      size: pageSize.value,
      sortBy: sortBy.value,
      direction: sortDirection.value,
      search: searchQuery.value.trim() || undefined,
      category: selectedCategory.value !== 'ALL' ? selectedCategory.value : undefined,
      status: selectedStatus.value !== 'ALL' ? selectedStatus.value : undefined
    })),
    watch: [page, pageSize, sortBy, sortDirection, searchQuery, selectedCategory, selectedStatus]
  })

  // Also fetch full unpaginated list for global metrics if needed
  const { data: serverAllProducts, refresh: refreshAll } = useFetch<Product[]>(API_BASE, {
    default: () => []
  })

  // Fallback computed mock products when backend is unreachable
  const isUsingMock = computed(() => !!error.value || !pagedData.value?.content)

  const computedMockFiltered = computed(() => {
    let list = [...localMockProducts.value]
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      list = list.filter(p => p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q))
    }
    if (selectedCategory.value !== 'ALL') {
      list = list.filter(p => p.category === selectedCategory.value)
    }
    if (selectedStatus.value !== 'ALL') {
      list = list.filter(p => p.status === selectedStatus.value)
    }
    list.sort((a, b) => {
      let valA = (a as any)[sortBy.value]
      let valB = (b as any)[sortBy.value]
      if (typeof valA === 'string') return sortDirection.value === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA)
      return sortDirection.value === 'asc' ? valA - valB : valB - valA
    })
    return list
  })

  // 3. Computed Product Lists & Pagination Metadata
  const allProducts = computed(() => {
    if (isUsingMock.value) return localMockProducts.value
    return serverAllProducts.value && serverAllProducts.value.length > 0 ? serverAllProducts.value : localMockProducts.value
  })

  const totalElements = computed(() => {
    if (isUsingMock.value) return computedMockFiltered.value.length
    return pagedData.value?.totalElements || computedMockFiltered.value.length
  })

  const totalPages = computed(() => {
    if (isUsingMock.value) return Math.ceil(totalElements.value / pageSize.value) || 1
    return pagedData.value?.totalPages || 1
  })

  const products = computed(() => {
    if (isUsingMock.value) {
      const start = page.value * pageSize.value
      return computedMockFiltered.value.slice(start, start + pageSize.value)
    }
    return pagedData.value?.content || []
  })

  const isFirst = computed(() => page.value === 0)
  const isLast = computed(() => page.value >= totalPages.value - 1)

  // 100% filtered products
  const filteredProducts = computed(() => products.value)

  // 4. Metrics Dashboard
  const totalValuation = computed(() => {
    return allProducts.value.reduce((acc, p) => acc + (p.price * p.stock), 0)
  })

  const lowStockCount = computed(() => {
    return allProducts.value.filter(p => p.stock > 0 && p.stock <= 5).length
  })

  const outOfStockCount = computed(() => {
    return allProducts.value.filter(p => p.stock === 0).length
  })

  // 5. Pagination & Sorting Actions
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
    page.value = 0 // Reset to first page
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

  // Reset page when search or category changes
  watch([searchQuery, selectedCategory, selectedStatus], () => {
    page.value = 0
  })

  // Smooth scroll to top of table when changing page
  watch(page, () => {
    if (import.meta.client) {
      const tableSection = document.getElementById('product-table-section')
      if (tableSection) {
        tableSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
      } else {
        window.scrollTo({ top: 0, behavior: 'smooth' })
      }
    }
  })

  // 6. CRUD Actions
  async function createProduct(payload: ProductFormData) {
    try {
      await $fetch(API_BASE, {
        method: 'POST',
        headers: { 'bypass-tunnel-reminder': 'true' },
        body: payload
      })
      await Promise.all([refresh(), refreshAll()])
    } catch (err) {
      console.warn('[Products] Backend offline, saving product to local mock state')
      const newId = (localMockProducts.value.length > 0 ? Math.max(...localMockProducts.value.map(p => p.id)) : 0) + 1
      const newProduct: Product = {
        id: newId,
        name: payload.name,
        sku: payload.sku || `PRD-MOCK-${newId}`,
        category: payload.category || 'General',
        price: Number(payload.price) || 0,
        stock: Number(payload.stock) || 0,
        status: Number(payload.stock) > 5 ? 'AVAILABLE' : Number(payload.stock) > 0 ? 'LOW_STOCK' : 'OUT_OF_STOCK',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
      localMockProducts.value.unshift(newProduct)
    }
  }

  async function updateProduct(id: number, payload: ProductFormData) {
    try {
      await $fetch(`${API_BASE}/${id}`, {
        method: 'PUT',
        headers: { 'bypass-tunnel-reminder': 'true' },
        body: payload
      })
      await Promise.all([refresh(), refreshAll()])
    } catch (err) {
      console.warn('[Products] Backend offline, updating product in local mock state')
      const idx = localMockProducts.value.findIndex(p => p.id === id)
      if (idx !== -1) {
        localMockProducts.value[idx] = {
          ...localMockProducts.value[idx],
          name: payload.name,
          sku: payload.sku,
          category: payload.category,
          price: Number(payload.price),
          stock: Number(payload.stock),
          status: Number(payload.stock) > 5 ? 'AVAILABLE' : Number(payload.stock) > 0 ? 'LOW_STOCK' : 'OUT_OF_STOCK',
          updatedAt: new Date().toISOString()
        }
      }
    }
  }

  async function deleteProduct(id: number) {
    try {
      await $fetch(`${API_BASE}/${id}`, {
        method: 'DELETE',
        headers: { 'bypass-tunnel-reminder': 'true' }
      })
      await Promise.all([refresh(), refreshAll()])
    } catch (err) {
      console.warn('[Products] Backend offline, removing from local mock state')
      localMockProducts.value = localMockProducts.value.filter(p => p.id !== id)
    }
  }

  async function bulkDeleteProducts(ids: number[]) {
    try {
      await $fetch(`${API_BASE}/bulk-delete`, {
        method: 'POST',
        headers: { 'bypass-tunnel-reminder': 'true' },
        body: { ids }
      })
      await Promise.all([refresh(), refreshAll()])
    } catch (err) {
      console.warn('[Products] Backend offline, bulk deleting from local mock state')
      localMockProducts.value = localMockProducts.value.filter(p => !ids.includes(p.id))
    }
  }

  function formatRupiah(val: number) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0
    }).format(val)
  }

  return {
    products,
    allProducts,
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
    searchQuery,
    selectedCategory,
    selectedStatus,
    categories,
    filteredProducts,
    totalValuation,
    lowStockCount,
    outOfStockCount,
    setPage,
    nextPage,
    prevPage,
    setPageSize,
    toggleSort,
    createProduct,
    updateProduct,
    deleteProduct,
    bulkDeleteProducts,
    formatRupiah
  }
}
