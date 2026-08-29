import type { Product, ProductFormData } from '~/types/product'
import type { PageResponse, SortDirection } from '~/types/pagination'

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
  const { data: allProducts, refresh: refreshAll } = useFetch<Product[]>(API_BASE, {
    default: () => []
  })

  // 3. Computed Product Lists & Pagination Metadata
  const products = computed(() => pagedData.value?.content || [])
  const totalElements = computed(() => pagedData.value?.totalElements || 0)
  const totalPages = computed(() => pagedData.value?.totalPages || 1)
  const isFirst = computed(() => pagedData.value?.first ?? true)
  const isLast = computed(() => pagedData.value?.last ?? true)

  // 100% Server-Side filtered products
  const filteredProducts = computed(() => products.value)

  // 4. Metrics Dashboard (Calculated from all products)
  const totalValuation = computed(() => {
    if (!allProducts.value) return 0
    return allProducts.value.reduce((acc, p) => acc + (p.price * p.stock), 0)
  })

  const lowStockCount = computed(() => {
    if (!allProducts.value) return 0
    return allProducts.value.filter(p => p.stock > 0 && p.stock <= 5).length
  })

  const outOfStockCount = computed(() => {
    if (!allProducts.value) return 0
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
    await $fetch(API_BASE, {
      method: 'POST',
      body: payload
    })
    await Promise.all([refresh(), refreshAll()])
  }

  async function updateProduct(id: number, payload: ProductFormData) {
    await $fetch(`${API_BASE}/${id}`, {
      method: 'PUT',
      body: payload
    })
    await Promise.all([refresh(), refreshAll()])
  }

  async function deleteProduct(id: number) {
    await $fetch(`${API_BASE}/${id}`, {
      method: 'DELETE'
    })
    await Promise.all([refresh(), refreshAll()])
  }

  async function bulkDeleteProducts(ids: number[]) {
    await $fetch(`${API_BASE}/bulk-delete`, {
      method: 'POST',
      body: { ids }
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
