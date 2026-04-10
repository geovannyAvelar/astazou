"use client"

import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react"
import { useAuth } from "@/lib/auth-context"

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
const STORAGE_KEY = "astazou-preferred-currency"

interface CurrencyContextValue {
  preferredCurrency: string
  setPreferredCurrency: (currency: string) => Promise<void>
}

const CurrencyContext = createContext<CurrencyContextValue | null>(null)

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const { user, isAuthenticated } = useAuth()

  const getInitial = (): string => {
    if (typeof window !== "undefined") {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) return stored
    }
    return user?.preferred_currency ?? "BRL"
  }

  const [preferredCurrency, setPreferredCurrencyState] = useState<string>("BRL")

  // Sync from server on auth state changes
  useEffect(() => {
    if (isAuthenticated && user?.preferred_currency) {
      setPreferredCurrencyState(user.preferred_currency)
      if (typeof window !== "undefined") {
        localStorage.setItem(STORAGE_KEY, user.preferred_currency)
      }
    } else if (!isAuthenticated) {
      // On logout restore from localStorage or fallback
      if (typeof window !== "undefined") {
        const stored = localStorage.getItem(STORAGE_KEY)
        if (stored) setPreferredCurrencyState(stored)
      }
    }
  }, [isAuthenticated, user?.preferred_currency])

  // On mount, read from localStorage before first auth response
  useEffect(() => {
    if (typeof window !== "undefined") {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) setPreferredCurrencyState(stored)
    }
  }, [])

  const setPreferredCurrency = useCallback(async (currency: string) => {
    setPreferredCurrencyState(currency)
    if (typeof window !== "undefined") {
      localStorage.setItem(STORAGE_KEY, currency)
    }
    try {
      await fetch(`${API_BASE}/users/me/preferences`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ preferredCurrency: currency }),
      })
    } catch {
      // persist locally even if the request fails
    }
  }, [])

  return (
    <CurrencyContext.Provider value={{ preferredCurrency, setPreferredCurrency }}>
      {children}
    </CurrencyContext.Provider>
  )
}

export function useCurrency() {
  const ctx = useContext(CurrencyContext)
  if (!ctx) throw new Error("useCurrency must be used within a CurrencyProvider")
  return ctx
}

