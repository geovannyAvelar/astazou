"use client"

import { createContext, useContext, useCallback, useEffect, useRef, useState, type ReactNode } from "react"
import type { AuthState, LoginResponse } from "@/lib/auth-types"
import { useIdleTimer } from "@/hooks/use-idle-timer"

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

/** How long without activity (ms) before the session is considered idle */
const IDLE_TIMEOUT_MS = Number(process.env.NEXT_PUBLIC_SESSION_IDLE_TIMEOUT_MS ?? 15 * 60 * 1000)

/** How often (ms) to proactively renew the session while the user is active */
const RENEW_INTERVAL_MS = 5 * 60 * 1000

interface AuthContextType extends AuthState {
  login: (clientId: string, clientSecret: string) => Promise<LoginResponse>
  logout: () => Promise<void>
  clearLogoutReason: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    isAuthenticated: false,
    isLoading: true,
    user: null,
    logoutReason: null,
  })

  const isAuthenticatedRef = useRef(false)

  // Keep ref in sync so callbacks don't capture stale state
  useEffect(() => {
    isAuthenticatedRef.current = state.isAuthenticated
  }, [state.isAuthenticated])

  const validate = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/token/validate`, {
        credentials: "include",
      })

      if (res.ok) {
        const user: LoginResponse = await res.json()
        setState((prev) => ({ ...prev, isAuthenticated: true, isLoading: false, user }))
      } else {
        setState((prev) => ({ ...prev, isAuthenticated: false, isLoading: false, user: null }))
      }
    } catch {
      setState((prev) => ({ ...prev, isAuthenticated: false, isLoading: false, user: null }))
    }
  }, [])

  useEffect(() => {
    validate()
  }, [validate])

  const login = useCallback(async (clientId: string, clientSecret: string): Promise<LoginResponse> => {
    const body = new URLSearchParams()
    body.append("client_id", clientId)
    body.append("client_secret", clientSecret)

    const res = await fetch(`${API_BASE}/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString(),
      credentials: "include",
    })

    if (!res.ok) {
      const status = res.status
      if (status === 401) {
        throw new Error("Invalid credentials. Please check your credentials")
      }
      throw new Error("Login failed. Please try again.")
    }

    const user: LoginResponse = await res.json()
    setState({ isAuthenticated: true, isLoading: false, user, logoutReason: null })
    return user
  }, [])

  const logout = useCallback(async (reason?: "inactivity") => {
    try {
      await fetch(`${API_BASE}/token/revoke`, {
        method: "POST",
        credentials: "include",
      })
    } finally {
      setState({ isAuthenticated: false, isLoading: false, user: null, logoutReason: reason ?? null })
    }
  }, [])

  const clearLogoutReason = useCallback(() => {
    setState((prev) => ({ ...prev, logoutReason: null }))
  }, [])

  /** Renew the session cookie on the backend (sliding window) */
  const renew = useCallback(async () => {
    if (!isAuthenticatedRef.current) return
    try {
      const res = await fetch(`${API_BASE}/token/renew`, {
        method: "POST",
        credentials: "include",
      })
      if (res.status === 401) {
        // Session already expired on the server – log out silently
        setState({ isAuthenticated: false, isLoading: false, user: null, logoutReason: "inactivity" })
      }
    } catch {
      // Network error – ignore, the idle timer will eventually log out
    }
  }, [])

  /** Periodic renewal while the user is active */
  useEffect(() => {
    if (!state.isAuthenticated) return
    const id = setInterval(renew, RENEW_INTERVAL_MS)
    return () => clearInterval(id)
  }, [state.isAuthenticated, renew])

  /** Idle detection – log out and set reason when user goes idle */
  const handleIdle = useCallback(() => {
    if (!isAuthenticatedRef.current) return
    logout("inactivity")
  }, [logout])

  /** When user comes back, renew immediately */
  const handleActive = useCallback(() => {
    renew()
  }, [renew])

  useIdleTimer({
    idleTimeout: IDLE_TIMEOUT_MS,
    onIdle: handleIdle,
    onActive: handleActive,
  })

  const publicLogout = useCallback(() => logout(), [logout])

  return (
    <AuthContext value={{ ...state, login, logout: publicLogout, clearLogoutReason }}>
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
