import type { Product } from '~/types/product'
import type { Order } from '~/types/order'
import type { StockMovement } from '~/types/audit'

export const MOCK_PRODUCTS: Product[] = [
  { id: 1, name: 'MacBook Pro M3 Max 16"', sku: 'LAP-MBP-01', category: 'Electronics', price: 38999000, stock: 14, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 2, name: 'Dell UltraSharp 27" 4K', sku: 'MON-DEL-03', category: 'Electronics', price: 8250000, stock: 8, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 3, name: 'iPad Pro 11" M4 OLED', sku: 'TAB-APP-06', category: 'Electronics', price: 17499000, stock: 12, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 4, name: 'ThinkPad X1 Carbon Gen 11', sku: 'LAP-LEN-07', category: 'Electronics', price: 29500000, stock: 6, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 5, name: 'Samsung Odyssey Neo G9 49"', sku: 'MON-SAM-08', category: 'Electronics', price: 21500000, stock: 3, status: 'LOW_STOCK', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 6, name: 'Logitech MX Master 3S', sku: 'ACC-LOG-02', category: 'Accessories', price: 1650000, stock: 25, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 7, name: 'Keychron Q1 Pro Wireless', sku: 'KEY-KCR-04', category: 'Accessories', price: 2890000, stock: 18, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 8, name: 'Anker 737 Power Bank 140W', sku: 'ACC-ANK-09', category: 'Accessories', price: 1850000, stock: 30, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 9, name: 'CalDigit TS4 Thunderbolt 4 Dock', sku: 'ACC-CDG-10', category: 'Accessories', price: 6200000, stock: 7, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 10, name: 'NuPhy Air75 V2 Low-Profile', sku: 'KEY-NUP-11', category: 'Accessories', price: 1950000, stock: 15, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 11, name: 'Ergonomic Standing Desk 160x80', sku: 'FRN-DSK-05', category: 'Furniture', price: 5400000, stock: 10, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 12, name: 'Herman Miller Aeron Chair', sku: 'FRN-HMA-12', category: 'Furniture', price: 22500000, stock: 5, status: 'LOW_STOCK', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 13, name: 'Steelcase Gesture Office Chair', sku: 'FRN-STC-13', category: 'Furniture', price: 18900000, stock: 4, status: 'LOW_STOCK', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 14, name: 'Dual Monitor Heavy Duty Arm', sku: 'FRN-ARM-14', category: 'Furniture', price: 1250000, stock: 20, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 15, name: 'Acoustic Felt Desk Partition', sku: 'FRN-PRT-15', category: 'Furniture', price: 850000, stock: 12, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 16, name: 'Sony WH-1000XM5 ANC Headphones', sku: 'AUD-SNY-16', category: 'Audio', price: 4999000, stock: 16, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 17, name: 'Shure SM7B Dynamic Microphone', sku: 'AUD-SHR-17', category: 'Audio', price: 6450000, stock: 9, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 18, name: 'Audioengine A2+ Wireless Speakers', sku: 'AUD-AEN-18', category: 'Audio', price: 4350000, stock: 8, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 19, name: 'Rodecaster Pro II Audio Console', sku: 'AUD-RDE-19', category: 'Audio', price: 10800000, stock: 4, status: 'LOW_STOCK', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 20, name: 'Sennheiser HD 660S2 Open-Back', sku: 'AUD-SNY-20', category: 'Audio', price: 7890000, stock: 5, status: 'LOW_STOCK', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 21, name: 'Rhode Leather Desk Mat 90x40', sku: 'STN-MAT-21', category: 'Stationery', price: 450000, stock: 40, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 22, name: 'Rotring 600 Mechanical Pencil', sku: 'STN-RTR-22', category: 'Stationery', price: 420000, stock: 50, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' },
  { id: 23, name: 'Leuchtturm1917 Hardcover Notebook', sku: 'STN-LCH-23', category: 'Stationery', price: 320000, stock: 35, status: 'AVAILABLE', createdAt: '2026-08-20T08:00:00Z', updatedAt: '2026-08-20T08:00:00Z' }
]

const now = new Date()
const isoDate = (daysAgo: number) => new Date(now.getTime() - daysAgo * 24 * 60 * 60 * 1000).toISOString()

export const MOCK_ORDERS: Order[] = [
  {
    id: 1,
    orderNumber: 'ORD-20260829-0001',
    customerName: 'Budi Santoso',
    customerEmail: 'budi.santoso@techcorp.id',
    totalAmount: 43998000,
    status: 'PAID',
    paymentMethod: 'QRIS',
    paymentRef: 'QRIS-NMID-0829-0001',
    createdAt: isoDate(0),
    items: [
      { id: 1, productId: 1, productName: 'MacBook Pro M3 Max 16"', quantity: 1, unitPrice: 38999000, subtotal: 38999000 },
      { id: 2, productId: 16, productName: 'Sony WH-1000XM5 ANC Headphones', quantity: 1, unitPrice: 4999000, subtotal: 4999000 }
    ]
  },
  {
    id: 2,
    orderNumber: 'ORD-20260828-0002',
    customerName: 'Siti Rahmawati',
    customerEmail: 'siti.rahma@fintech.co.id',
    totalAmount: 25390000,
    status: 'PAID',
    paymentMethod: 'BANK_TRANSFER_VA',
    paymentRef: '880098280002',
    createdAt: isoDate(1),
    items: [
      { id: 3, productId: 12, productName: 'Herman Miller Aeron Chair', quantity: 1, unitPrice: 22500000, subtotal: 22500000 },
      { id: 4, productId: 7, productName: 'Keychron Q1 Pro Wireless', quantity: 1, unitPrice: 2890000, subtotal: 2890000 }
    ]
  },
  {
    id: 3,
    orderNumber: 'ORD-20260826-0003',
    customerName: 'Ahmad Fauzi',
    customerEmail: 'fauzi@agency.com',
    totalAmount: 21500000,
    status: 'PAID',
    paymentMethod: 'CREDIT_CARD',
    paymentRef: 'AUTH-9826-0003',
    createdAt: isoDate(3),
    items: [
      { id: 5, productId: 5, productName: 'Samsung Odyssey Neo G9 49"', quantity: 1, unitPrice: 21500000, subtotal: 21500000 }
    ]
  },
  {
    id: 4,
    orderNumber: 'ORD-20260824-0004',
    customerName: 'Rian Hidayat',
    customerEmail: 'rian@hidayat.com',
    totalAmount: 38999000,
    status: 'CANCELLED',
    paymentMethod: 'BANK_TRANSFER_VA',
    paymentRef: '880098240004',
    createdAt: isoDate(5),
    items: [
      { id: 6, productId: 1, productName: 'MacBook Pro M3 Max 16"', quantity: 1, unitPrice: 38999000, subtotal: 38999000 }
    ]
  },
  {
    id: 5,
    orderNumber: 'ORD-20260822-0005',
    customerName: 'Dewi Lestari',
    customerEmail: 'dewi.lestari@creative.id',
    totalAmount: 14750000,
    status: 'PAID',
    paymentMethod: 'QRIS',
    paymentRef: 'QRIS-NMID-0822-0005',
    createdAt: isoDate(7),
    items: [
      { id: 7, productId: 2, productName: 'Dell UltraSharp 27" 4K', quantity: 1, unitPrice: 8250000, subtotal: 8250000 },
      { id: 8, productId: 17, productName: 'Shure SM7B Dynamic Microphone', quantity: 1, unitPrice: 6450000, subtotal: 6450000 }
    ]
  },
  {
    id: 6,
    orderNumber: 'ORD-20260815-0006',
    customerName: 'Hendro Wijaya',
    customerEmail: 'hendro@logistics.co.id',
    totalAmount: 64500000,
    status: 'PAID',
    paymentMethod: 'CASH',
    paymentRef: 'CASH-POS-0006',
    createdAt: isoDate(14),
    items: [
      { id: 9, productId: 4, productName: 'ThinkPad X1 Carbon Gen 11', quantity: 2, unitPrice: 29500000, subtotal: 59000000 },
      { id: 10, productId: 11, productName: 'Ergonomic Standing Desk 160x80', quantity: 1, unitPrice: 5400000, subtotal: 5400000 }
    ]
  }
]
