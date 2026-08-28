export type Locale = 'id' | 'en'

const translations: Record<Locale, Record<string, string>> = {
  id: {
    // Nav
    'nav.brand': 'ERP Learn',
    'nav.subBrand': 'Sistem Retail & POS Terpadu',
    'nav.products': 'Master Produk',
    'nav.transactions': 'Transaksi Order',
    'nav.analytics': 'Sales Analytics',
    'nav.pos': 'Kasir POS',
    'nav.logout': 'Keluar',
    'nav.adminMode': 'ADMIN',
    'nav.cashierMode': 'KASIR',

    // Home / Catalog
    'home.badge': 'Point 6: 🔐 RBAC & Manajemen Sesi',
    'home.adminMode': '👑 Mode Administrator',
    'home.cashierMode': '🛒 Mode Kasir (Read-Only)',
    'home.title': 'Master Katalog Produk & Inventori',
    'home.adminDesc': 'Sebagai Administrator, Anda memiliki akses penuh untuk menambah, mengedit, menghapus, import/export CSV, serta mencatat restock mutasi inventori.',
    'home.cashierDesc': 'Sebagai Kasir, Anda dapat melihat informasi katalog dan ketersediaan stok untuk mendukung transaksi POS (akses modifikasi katalog dibatasi).',
    'home.exportCsv': 'Export CSV',
    'home.importCsv': 'Import CSV',
    'home.addProduct': 'Tambah Produk',
    'home.totalProducts': 'Total Produk',
    'home.categories': 'Kategori',
    'home.valuation': 'Total Valuasi Aset',
    'home.lowStock': 'Stok Menipis',
    'home.outOfStock': 'Stok Habis',
    'home.searchPlaceholder': 'Cari nama produk / SKU...',
    'home.categoryFilter': 'Kategori:',
    'home.statusFilter': 'Status:',
    'home.allCategories': 'Semua Kategori',
    'home.allStatus': 'Semua Status',
    'home.inStock': 'Tersedia',
    'home.actions': 'Aksi',
    'home.history': 'Riwayat',
    'home.edit': 'Edit',
    'home.delete': 'Hapus',
    'home.bulkSelected': 'produk dipilih untuk aksi massal',
    'home.cancelSelection': 'Batal Pilihan',
    'home.bulkDeleteBtn': 'Hapus Terpilih',
    'home.emptyTable': 'Tidak ada produk yang cocok dengan pencarian atau filter.',

    // POS / Orders
    'orders.title': 'Daftar Transaksi & Order POS',
    'orders.desc': 'Setiap transaksi menyimpan header dan detail dengan server-side paging, filter status realtime, dan pengurangan stok otomatis ke audit log.',
    'orders.newOrder': 'Buat Order Baru',
    'orders.totalOrders': 'Total Transaksi',
    'orders.totalRevenue': 'Total Pendapatan',
    'orders.pendingOrders': 'Menunggu Pembayaran',
    'orders.paidOrders': 'Lunas',
    'orders.orderNumber': 'No. Transaksi',
    'orders.customer': 'Pelanggan',
    'orders.date': 'Tanggal',
    'orders.total': 'Total Tagihan',
    'orders.status': 'Status',
    'orders.detail': 'Detail Item',
    'orders.receipt': 'Struk',
    'orders.setPaid': 'Set Lunas',
    'orders.cancel': 'Batal',

    // POS Create
    'pos.title': 'Terminal Kasir & POS Checkout',
    'pos.desc': 'Pilih produk dari katalog, masukkan detail pelanggan, dan proses transaksi penjualan langsung.',
    'pos.customerInfo': 'Informasi Pelanggan',
    'pos.customerName': 'Nama Pelanggan',
    'pos.customerEmail': 'Email Pelanggan (Opsional)',
    'pos.cartTitle': 'Keranjang Belanja',
    'pos.emptyCart': 'Keranjang masih kosong. Klik tombol "+" pada katalog produk.',
    'pos.subtotal': 'Subtotal',
    'pos.totalPayment': 'Total Pembayaran',
    'pos.checkoutBtn': 'Proses Pembayaran (Checkout)',
    'pos.stockRemaining': 'Sisa Stok:',

    // Analytics
    'analytics.title': 'Executive Dashboard & Sales Analytics',
    'analytics.desc': 'Visualisasi performa penjualan, revenue metrics, tren harian/bulanan, dan pangsa kategori produk secara realtime.',
    'analytics.revenueTrend': 'Tren Penjualan & Pendapatan',
    'analytics.categoryShare': 'Pangsa Penjualan per Kategori',
    'analytics.leaderboard': 'Leaderboard Produk Terlaris',
    'analytics.avgOrder': 'Rata-Rata per Order',

    // Auth & Login
    'auth.welcome': 'Selamat Datang Kembali',
    'auth.subWelcome': 'Silakan masuk dengan akun yang terdaftar',
    'auth.email': 'Alamat Email',
    'auth.password': 'Kata Sandi',
    'auth.loginBtn': 'Masuk ke Sistem',
    'auth.verifying': 'Memverifikasi...',
    'auth.demoTitle': 'Demo Quick Autofill',
    'auth.demoDesc': 'Klik tombol di bawah untuk mengisi kredensial demo secara instan:',
    'auth.adminDemo': 'Admin Demo',
    'auth.cashierDemo': 'Kasir Demo',
    'auth.adminRoleDesc': 'Akses Lengkap & Analytics',
    'auth.cashierRoleDesc': 'Mode POS & Transaksi',

    // Theme & Language
    'theme.dark': 'Gelap',
    'theme.light': 'Terang',
    'lang.id': 'Bahasa Indonesia',
    'lang.en': 'English'
  },
  en: {
    // Nav
    'nav.brand': 'ERP Learn',
    'nav.subBrand': 'Integrated Retail & POS System',
    'nav.products': 'Product Catalog',
    'nav.transactions': 'Transactions',
    'nav.analytics': 'Sales Analytics',
    'nav.pos': 'POS Terminal',
    'nav.logout': 'Sign Out',
    'nav.adminMode': 'ADMIN',
    'nav.cashierMode': 'CASHIER',

    // Home / Catalog
    'home.badge': 'Point 6: 🔐 RBAC & Session Management',
    'home.adminMode': '👑 Administrator Mode',
    'home.cashierMode': '🛒 Cashier Mode (Read-Only)',
    'home.title': 'Master Product Catalog & Inventory',
    'home.adminDesc': 'As an Administrator, you have full access to add, edit, delete, import/export CSV, and record warehouse restock movements.',
    'home.cashierDesc': 'As a Cashier, you can browse product details and live availability to assist with POS checkout (editing permissions restricted).',
    'home.exportCsv': 'Export CSV',
    'home.importCsv': 'Import CSV',
    'home.addProduct': 'Add Product',
    'home.totalProducts': 'Total Products',
    'home.categories': 'Categories',
    'home.valuation': 'Total Asset Valuation',
    'home.lowStock': 'Low Stock',
    'home.outOfStock': 'Out of Stock',
    'home.searchPlaceholder': 'Search product name / SKU...',
    'home.categoryFilter': 'Category:',
    'home.statusFilter': 'Status:',
    'home.allCategories': 'All Categories',
    'home.allStatus': 'All Statuses',
    'home.inStock': 'In Stock',
    'home.actions': 'Actions',
    'home.history': 'History',
    'home.edit': 'Edit',
    'home.delete': 'Delete',
    'home.bulkSelected': 'products selected for bulk action',
    'home.cancelSelection': 'Cancel',
    'home.bulkDeleteBtn': 'Delete Selected',
    'home.emptyTable': 'No products match your search or filter criteria.',

    // POS / Orders
    'orders.title': 'Transaction Orders & POS History',
    'orders.desc': 'Every transaction retains header-detail records with server-side paging, realtime status filters, and automatic stock ledger deductions.',
    'orders.newOrder': 'Create New Order',
    'orders.totalOrders': 'Total Orders',
    'orders.totalRevenue': 'Total Revenue',
    'orders.pendingOrders': 'Pending Orders',
    'orders.paidOrders': 'Completed (Paid)',
    'orders.orderNumber': 'Order #',
    'orders.customer': 'Customer',
    'orders.date': 'Date',
    'orders.total': 'Total Amount',
    'orders.status': 'Status',
    'orders.detail': 'Item Details',
    'orders.receipt': 'Receipt',
    'orders.setPaid': 'Mark Paid',
    'orders.cancel': 'Cancel',

    // POS Create
    'pos.title': 'POS Terminal & Cashier Checkout',
    'pos.desc': 'Select items from the catalog, enter customer details, and process instant retail transactions.',
    'pos.customerInfo': 'Customer Information',
    'pos.customerName': 'Customer Name',
    'pos.customerEmail': 'Customer Email (Optional)',
    'pos.cartTitle': 'Shopping Cart',
    'pos.emptyCart': 'Cart is empty. Click the "+" button on any catalog card.',
    'pos.subtotal': 'Subtotal',
    'pos.totalPayment': 'Total Payment',
    'pos.checkoutBtn': 'Process Payment (Checkout)',
    'pos.stockRemaining': 'Remaining Stock:',

    // Analytics
    'analytics.title': 'Executive Dashboard & Sales Analytics',
    'analytics.desc': 'Real-time visualization of sales performance, revenue metrics, monthly trends, and product category market share.',
    'analytics.revenueTrend': 'Revenue & Sales Trends',
    'analytics.categoryShare': 'Sales Share by Category',
    'analytics.leaderboard': 'Top Selling Products Leaderboard',
    'analytics.avgOrder': 'Avg. Order Value',

    // Auth & Login
    'auth.welcome': 'Welcome Back',
    'auth.subWelcome': 'Please sign in with your registered account',
    'auth.email': 'Email Address',
    'auth.password': 'Password',
    'auth.loginBtn': 'Sign In to System',
    'auth.verifying': 'Verifying...',
    'auth.demoTitle': 'Demo Quick Autofill',
    'auth.demoDesc': 'Click the buttons below to automatically populate demo credentials:',
    'auth.adminDemo': 'Admin Demo',
    'auth.cashierDemo': 'Cashier Demo',
    'auth.adminRoleDesc': 'Full Access & Analytics',
    'auth.cashierRoleDesc': 'POS Mode & Transactions',

    // Theme & Language
    'theme.dark': 'Dark',
    'theme.light': 'Light',
    'lang.id': 'Bahasa Indonesia',
    'lang.en': 'English'
  }
}

export function useI18n() {
  const localeCookie = useCookie<Locale>('erp_locale', {
    default: () => 'id',
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 365 // 1 year
  })

  const locale = useState<Locale>('app_locale', () => localeCookie.value || 'id')

  function setLocale(newLocale: Locale) {
    locale.value = newLocale
    localeCookie.value = newLocale
    if (import.meta.client) {
      localStorage.setItem('erp_locale', newLocale)
    }
  }

  function t(key: string, params?: Record<string, string | number>): string {
    const currentDict = translations[locale.value] || translations.id
    let text = currentDict[key] || translations.id[key] || key

    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        text = text.replace(new RegExp(`{${k}}`, 'g'), String(v))
      })
    }
    return text
  }

  return {
    locale,
    setLocale,
    t
  }
}
