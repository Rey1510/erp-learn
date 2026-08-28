export interface Product {
  id: number
  name: string
  sku: string
  category: string
  price: number
  stock: number
  status: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'
  createdAt?: string
  updatedAt?: string
}

export type ProductFormData = Omit<Product, 'id' | 'status' | 'createdAt' | 'updatedAt'>
