export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface FilterRequest {
  page?: number
  size?: number
  sortBy?: string
  direction?: 'ASC' | 'DESC'
  search?: string
}
