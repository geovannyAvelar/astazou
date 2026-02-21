"use client"

import { createContext, useContext, useCallback, useEffect, useState, type ReactNode } from "react"
import type { AuthState, LoginResponse } from "@/lib/auth-types"

const API_BASE = process.env.API_BASE_URL || "http://localhost:8080"

interface AuthContextType extends AuthState {
  login: (clientId: string, clientSecret: string) => Promise<LoginResponse>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    isAuthenticated: false,
    isLoading: true,
    user: null,
  })

  const validate = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/token/validate`, {
        credentials: "include",
      })

      if (res.ok) {
        const user: LoginResponse = await res.json()
        setState({ isAuthenticated: true, isLoading: false, user })
      } else {
        setState({ isAuthenticated: false, isLoading: false, user: null })
      }
    } catch {
      setState({ isAuthenticated: false, isLoading: false, user: null })
    }
  }, [])

  useEffect(() => {
    validate()
  }, [validate])

  const login = useCallback(async (clientId: string, clientSecret: string): Promise<LoginResponse> => {
    const body = new URLSearchParams()
    body.append("client_id", clientId)
    body.append("client_secret", clientSecret)

    const res = await fetch(`${API_BASE}/token` , {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString(),
      credentials: "include",
    })

    if (!res.ok) {
      const status = res.status
        console.log(status)
      if (status === 401) {
        throw new Error("Invalid credentials. Please check your credentials")
      }
      throw new Error("Login failed. Please try again.")
    }

    const user: LoginResponse = await res.json()
    setState({ isAuthenticated: true, isLoading: false, user })
    return user
  }, [])

  const logout = useCallback(async () => {
    try {
      await fetch(`${API_BASE}/token/revoke`, {
        method: "POST",
        credentials: "include",
      })
    } finally {
      setState({ isAuthenticated: false, isLoading: false, user: null })
    }
  }, [])

  return (
    <AuthContext value={{ ...state, login, logout }}>
      {children}
    </AuthContext>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return ctx
}
