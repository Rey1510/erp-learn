import type { Product } from './product'

export interface OrderItem {
  id: number
  product: Product
  productName: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface Order {
  id: number
  orderNumber: string
  customerName: string
  customerEmail?: string
  totalAmount: number
  status: 'PENDING' | 'PAID' | 'CANCELLED'
  paymentMethod?: 'CASH' | 'QRIS' | 'BANK_TRANSFER_VA' | 'CREDIT_CARD'
  paymentRef?: string
  items: OrderItem[]
  createdAt: string
  updatedAt: string
}

export interface Payment {
  id: number
  paymentNumber: string
  orderId: number
  orderNumber: string
  customerName: string
  amount: number
  method: 'CASH' | 'QRIS' | 'BANK_TRANSFER_VA' | 'CREDIT_CARD'
  status: 'PENDING' | 'SETTLED' | 'EXPIRED' | 'FAILED'
  referenceNumber: string
  notes?: string
  paidAt?: string
  expiresAt?: string
  createdAt: string
  updatedAt: string
}

export interface OrderItemPayload {
  productId: number
  quantity: number
}

export interface CreateOrderPayload {
  customerName: string
  customerEmail?: string
  paymentMethod?: 'CASH' | 'QRIS' | 'BANK_TRANSFER_VA' | 'CREDIT_CARD'
  idempotencyKey?: string
  items: OrderItemPayload[]
}

