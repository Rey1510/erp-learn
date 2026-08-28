export interface PageResponse<T> {
  content: T[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type SortDirection = 'asc' | 'desc'

export interface PaginationParams {
  page: number
  size: number
  sortBy: string
  direction: SortDirection
  search?: string
  category?: string
  status?: string
}
