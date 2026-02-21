export interface LoginResponse {
  authenticated: boolean
  username: string
  completeUsername?: string
  email?: string
  scope: string
  accessToken: string
  expiresAt: string
}

export interface AuthState {
  isAuthenticated: boolean
  isLoading: boolean
  user: LoginResponse | null
}
