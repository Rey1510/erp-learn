export type MovementType = 'INITIAL' | 'SALE' | 'RESTOCK' | 'CANCEL_RESTOCK' | 'MANUAL_ADJUSTMENT'

export interface StockMovement {
  id: number
  productId: number
  productName: string
  sku: string
  type: MovementType
  quantityChange: number
  resultingStock: number
  referenceNumber?: string
  notes?: string
  createdAt: string
}

export interface RestockPayload {
  productId: number
  quantity: number
  notes?: string
}
