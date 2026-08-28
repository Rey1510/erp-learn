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
  items: OrderItem[]
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
  items: OrderItemPayload[]
}
