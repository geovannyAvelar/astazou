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

/** Read the stored currency from localStorage (client-only, safe to call in lazy initializer). */
function readStoredCurrency(): string {
  if (typeof window === "undefined") return "BRL"
  return localStorage.getItem(STORAGE_KEY) ?? "BRL"
}

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const { user, isAuthenticated } = useAuth()

  // Lazy initializer runs once on the client and reads from localStorage immediately,
  // avoiding the need for a setState-in-effect pattern.
  const [preferredCurrency, setPreferredCurrencyState] = useState<string>(readStoredCurrency)

  // Sync whenever the server tells us the user's preferred currency changed
  // (e.g. after login, or after /token/validate resolves).
  useEffect(() => {
    if (isAuthenticated && user?.preferred_currency) {
        // eslint-disable-next-line react-hooks/set-state-in-effect
      setPreferredCurrencyState(user.preferred_currency)
      localStorage.setItem(STORAGE_KEY, user.preferred_currency)
    }
  }, [isAuthenticated, user?.preferred_currency])

  const setPreferredCurrency = useCallback(async (currency: string) => {
    setPreferredCurrencyState(currency)
    localStorage.setItem(STORAGE_KEY, currency)
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

