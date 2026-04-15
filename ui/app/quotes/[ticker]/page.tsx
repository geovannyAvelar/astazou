"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter, useParams } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { ArrowLeft, AlertCircle, Clock, Loader2, LogOut, RefreshCw, TrendingUp } from "lucide-react"
import Image from "next/image"
import Link from "next/link"

const API_BASE: string =
  (process.env.NEXT_PUBLIC_API_URL as string) ||
  (process.env.REACT_APP_API_BASE as string) ||
  (process.env.VITE_API_BASE as string) ||
  (process.env.API_BASE as string) ||
  "http://localhost:8080"

interface StockQuote {
  id: number
  symbol: string
  shortName: string
  longName: string
  currency: string
  price: number
  updatedAt: string
}

interface BrapiStock {
  id: number
  ticker: string
  name: string | null
  sector: string | null
  logoUrl: string | null
}

export default function StockDetailPage() {
  const { isAuthenticated, isLoading: authLoading, user, logout } = useAuth()
  const { t } = useI18n()
  const router = useRouter()
  const params = useParams()
  const ticker = ((params?.ticker as string) ?? "").toUpperCase()

  const [quote, setQuote] = useState<StockQuote | null>(null)
  const [stockInfo, setStockInfo] = useState<BrapiStock | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [error, setError] = useState<"not_found" | "error" | null>(null)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoError, setLogoError] = useState(false)

  // Auth guard
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.replace("/")
    }
  }, [authLoading, isAuthenticated, router])

  const fetchData = useCallback(async (showRefreshSpinner = false) => {
    if (!ticker) return
    if (showRefreshSpinner) {
      setIsRefreshing(true)
    } else {
      setIsLoading(true)
    }
    setError(null)

    try {
      const [quoteRes, stockRes] = await Promise.all([
        fetch(`${API_BASE}/quotes/${encodeURIComponent(ticker)}`, { credentials: "include" }),
        fetch(`${API_BASE}/stocks/${encodeURIComponent(ticker)}`, { credentials: "include" }),
      ])

      if (quoteRes.status === 404) {
        setError("not_found")
        return
      }
      if (!quoteRes.ok) {
        setError("error")
        return
      }

      const quoteData: StockQuote = await quoteRes.json()
      setQuote(quoteData)

      if (stockRes.ok) {
        const stockData: BrapiStock = await stockRes.json()
        setStockInfo(stockData)
        setLogoError(false)
      }
    } catch {
      setError("error")
    } finally {
      setIsLoading(false)
      setIsRefreshing(false)
    }
  }, [ticker])

  useEffect(() => {
    if (isAuthenticated && ticker) {
      fetchData()
    }
  }, [isAuthenticated, ticker, fetchData])

  async function handleLogout() {
    setIsLoggingOut(true)
    try {
      await logout()
      router.replace("/")
    } finally {
      setIsLoggingOut(false)
    }
  }

  function formatPrice(price: number, currency: string) {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: currency || "BRL",
      minimumFractionDigits: 2,
    }).format(price)
  }

  function formatDateTime(dateStr: string) {
    return new Intl.DateTimeFormat("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    }).format(new Date(dateStr))
  }

  function getStaleMinutes(dateStr: string): number {
    return Math.floor((Date.now() - new Date(dateStr).getTime()) / 60000)
  }

  const displayName = user?.completeUsername || user?.username || "User"
  const initials = displayName
    .split(" ")
    .map((n: string) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2)

  const logoSrc = stockInfo?.logoUrl ? `${API_BASE}${stockInfo.logoUrl}` : null
  const stockName = stockInfo?.name || quote?.longName || quote?.shortName || ticker

  // ── Loading skeleton ─────────────────────────────────────────────────────────
  if (authLoading || (isLoading && !quote)) {
    return (
      <main className="flex min-h-svh items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="size-8 animate-spin text-primary" />
          <p className="text-sm text-muted-foreground">{t.loading}</p>
        </div>
      </main>
    )
  }

  if (!isAuthenticated) return null

  return (
    <div className="min-h-svh bg-background">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b bg-card/80 backdrop-blur-sm">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary text-primary-foreground font-bold text-sm">
              <Image src="/logo.png" alt="Astazou logo" width={24} height={24} className="object-contain" />
            </div>
            <span className="text-lg font-bold text-foreground tracking-tight">Astazou</span>
          </div>

          <div className="flex items-center gap-3">
            <ThemeToggle variant="ghost" />
            <LanguageSwitcher variant="ghost" />
            <div className="hidden items-center gap-3 sm:flex">
              <div className="flex size-9 items-center justify-center rounded-full bg-secondary text-secondary-foreground text-sm font-semibold">
                {initials}
              </div>
              <div className="flex flex-col">
                <span className="text-sm font-medium text-foreground leading-tight">{displayName}</span>
                {user?.email && <span className="text-xs text-muted-foreground">{user.email}</span>}
              </div>
            </div>
            <Button variant="outline" size="sm" onClick={handleLogout} disabled={isLoggingOut} className="gap-2">
              {isLoggingOut ? <Loader2 className="size-4 animate-spin" /> : <LogOut className="size-4" />}
              <span className="hidden sm:inline">{t.signOut}</span>
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-4 py-8 lg:px-8">
        {/* Back navigation */}
        <div className="mb-8">
          <Link href="/quotes">
            <Button variant="ghost" size="sm" className="gap-2">
              <ArrowLeft className="size-4" />
              {t.backToQuotes}
            </Button>
          </Link>
        </div>

        {/* Error state */}
        {error && (
          <Card className="border-destructive/50 bg-destructive/5">
            <CardContent className="flex items-center gap-3 pt-6">
              <AlertCircle className="size-5 text-destructive shrink-0" />
              <div>
                <p className="font-mono font-medium text-destructive">{ticker}</p>
                <p className="text-sm text-muted-foreground">
                  {error === "not_found" ? t.quoteNotFound : t.quoteError}
                </p>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Stock detail card */}
        {quote && !error && (
          <Card>
            <CardHeader className="pb-4">
              <div className="flex items-start justify-between gap-4">
                {/* Logo + name block */}
                <div className="flex items-center gap-4">
                  {logoSrc && !logoError ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={logoSrc}
                      alt={`${ticker} logo`}
                      width={56}
                      height={56}
                      className="rounded-xl object-contain border bg-white p-1 shadow-sm"
                      onError={() => setLogoError(true)}
                    />
                  ) : (
                    <div className="flex size-14 items-center justify-center rounded-xl border bg-primary/10 text-primary shadow-sm">
                      <TrendingUp className="size-6" />
                    </div>
                  )}

                  <div>
                    <CardTitle className="flex items-center gap-2 text-2xl font-mono">
                      {quote.symbol}
                      <Badge variant="outline" className="font-sans text-xs font-normal">
                        {quote.currency}
                      </Badge>
                    </CardTitle>
                    <CardDescription className="mt-0.5 text-sm">
                      {stockName}
                    </CardDescription>
                    {stockInfo?.sector && (
                      <span className="mt-1 inline-block text-xs text-muted-foreground">
                        {t.stockSector}: {stockInfo.sector}
                      </span>
                    )}
                  </div>
                </div>

                {/* Refresh button */}
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => fetchData(true)}
                  disabled={isRefreshing}
                  title={t.refresh}
                  className="shrink-0"
                >
                  <RefreshCw className={`size-4 ${isRefreshing ? "animate-spin" : ""}`} />
                </Button>
              </div>
            </CardHeader>

            <CardContent>
              {/* Price */}
              <div className="flex items-baseline gap-2">
                <span className="text-4xl font-bold text-foreground tracking-tight">
                  {formatPrice(quote.price, quote.currency)}
                </span>
              </div>

              {/* Last updated */}
              {quote.updatedAt && (
                <div className="mt-4 flex items-center gap-1.5 text-xs text-muted-foreground">
                  <Clock className="size-3.5" />
                  <span>
                    {t.lastUpdated}: {formatDateTime(quote.updatedAt)}
                  </span>
                  {getStaleMinutes(quote.updatedAt) < 20 && (
                    <Badge variant="secondary" className="ml-1 text-xs">
                      {t.cached}
                    </Badge>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        )}
      </main>
    </div>
  )
}

