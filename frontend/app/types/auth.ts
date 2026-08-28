export type Role = 'ADMIN' | 'CASHIER'

export interface User {
  id: number
  name: string
  email: string
  role: Role
  token: string
}

export interface AuthResponse {
  id: number
  name: string
  email: string
  role: Role
  token: string
}
