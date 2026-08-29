import type { CreateOrderPayload, Order } from './order'

export type OutboxSyncStatus = 'PENDING_SYNC' | 'SYNCING' | 'SYNCED' | 'FAILED'

export interface OutboxOrder {
  id?: number // IndexedDB auto-increment ID
  tempOrderNumber: string // e.g. "OFFLINE-20260829-881A"
  payload: CreateOrderPayload
  itemsSummary: {
    productName: string
    quantity: number
    unitPrice: number
    subtotal: number
  }[]
  customerName: string
  customerEmail?: string
  paymentMethod: string
  totalAmount: number
  idempotencyKey: string
  createdAt: string
  status: OutboxSyncStatus
  syncAttempts: number
  lastError?: string
  syncedOrder?: Order
  syncedAt?: string
}
