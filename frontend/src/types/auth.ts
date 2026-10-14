export type UserScope = 'INSTITUTION' | 'GLOBAL' | 'ADMIN'

export interface AuthUser {
  id: number
  login: string
  email: string
  firstName: string
  lastName: string
  status: string
  languageCode: string
  institutionId: number | null
  institutionName: string | null
  scope: UserScope
  roles: string[]
  permissions: string[]
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: AuthUser
}

export interface LoginRequest {
  login: string
  password: string
}

export interface UserDto {
  id: number
  login: string
  email: string
  firstName: string
  lastName: string
  dateBirth?: string
  phone?: string
  status: string
  languageCode: string
  scope: UserScope
  passwordExpiresAt?: string
  accountExpiresAt?: string
  createdAt: string
  updatedAt: string
  institutionId?: number
  institutionName?: string
  roles: string[]
  permissions: string[]
}

export interface UserCreateRequest {
  login: string
  email: string
  password: string
  firstName: string
  lastName: string
  dateBirth?: string
  phone?: string
  languageCode?: string
  institutionId?: number
  scope?: UserScope
  roleIds?: number[]
}

export interface UserUpdateRequest {
  firstName: string
  lastName: string
  email: string
  phone?: string
  languageCode?: string
  institutionId?: number
  scope?: UserScope
  roleIds?: number[]
}
