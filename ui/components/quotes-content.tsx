"use client"

import { useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  ArrowLeft,
  Loader2,
  LogOut,
  Search,
  TrendingUp,
  RefreshCw,
  Clock,
  AlertCircle,
} from "lucide-react"
import Image from "next/image"
import Link from "next/link"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

interface StockQuote {
  id: number
  symbol: string
  shortName: string
  longName: string
  currency: string
  price: number
  updatedAt: string
}

export function QuotesContent() {
  const { user, logout } = useAuth()
  const { t } = useI18n()
  const router = useRouter()

  const [ticker, setTicker] = useState("")
  const [quote, setQuote] = useState<StockQuote | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [searchedTicker, setSearchedTicker] = useState<string | null>(null)

  const fetchQuote = useCallback(async (symbol: string) => {
    if (!symbol.trim()) return
    setIsLoading(true)
    setError(null)
    setQuote(null)
    setSearchedTicker(symbol.toUpperCase())
    try {
      const res = await fetch(`${API_BASE}/quotes/${encodeURIComponent(symbol.trim().toUpperCase())}`, {
          credentials: "include"
      })
      if (res.status === 404) {
        setError(t.quoteNotFound)
        return
      }
      if (!res.ok) {
        setError(t.quoteError)
        return
      }
      const data: StockQuote = await res.json()
      setQuote(data)
    } catch {
      setError(t.quoteError)
    } finally {
      setIsLoading(false)
    }
  }, [t])

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    await fetchQuote(ticker)
  }

  async function handleLogout() {
    setIsLoggingOut(true)
    try {
      await logout()
      router.replace("/")
    } finally {
      setIsLoggingOut(false)
    }
  }

  const displayName = user?.completeUsername || user?.username || "User"
  const initials = displayName
    .split(" ")
    .map((n: string) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2)

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
    const diff = Date.now() - new Date(dateStr).getTime()
    return Math.floor(diff / 60000)
  }

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
                {user?.email && (
                  <span className="text-xs text-muted-foreground">{user.email}</span>
                )}
              </div>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={handleLogout}
              disabled={isLoggingOut}
              className="gap-2"
            >
              {isLoggingOut ? (
                <Loader2 className="size-4 animate-spin" />
              ) : (
                <LogOut className="size-4" />
              )}
              <span className="hidden sm:inline">{t.signOut}</span>
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-4 py-8 lg:px-8">
        {/* Back + Title */}
        <div className="mb-8 flex items-center gap-3">
          <Link href="/dashboard">
            <Button variant="ghost" size="sm" className="gap-2">
              <ArrowLeft className="size-4" />
              {t.backToDashboard}
            </Button>
          </Link>
        </div>

        <div className="mb-6">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <TrendingUp className="size-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground tracking-tight">{t.stockQuotes}</h1>
              <p className="text-sm text-muted-foreground">{t.stockQuotesDescription}</p>
            </div>
          </div>
        </div>

        {/* Search form */}
        <Card className="mb-6">
          <CardContent className="pt-6">
            <form onSubmit={handleSearch} className="flex gap-2">
              <Input
                value={ticker}
                onChange={(e) => setTicker(e.target.value.toUpperCase())}
                placeholder={t.tickerPlaceholder}
                className="flex-1 font-mono uppercase"
                maxLength={12}
                disabled={isLoading}
              />
              <Button type="submit" disabled={isLoading || !ticker.trim()} className="gap-2">
                {isLoading ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <Search className="size-4" />
                )}
                {t.search}
              </Button>
            </form>
            <p className="mt-2 text-xs text-muted-foreground">{t.quotesCacheNote}</p>
          </CardContent>
        </Card>

        {/* Error state */}
        {error && (
          <Card className="border-destructive/50 bg-destructive/5">
            <CardContent className="flex items-center gap-3 pt-6">
              <AlertCircle className="size-5 text-destructive shrink-0" />
              <div>
                <p className="font-medium text-destructive">{searchedTicker}</p>
                <p className="text-sm text-muted-foreground">{error}</p>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Quote result */}
        {quote && !error && (
          <Card>
            <CardHeader className="pb-2">
              <div className="flex items-start justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2 text-xl font-mono">
                    {quote.symbol}
                    <Badge variant="outline" className="font-sans text-xs font-normal">
                      {quote.currency}
                    </Badge>
                  </CardTitle>
                  <CardDescription className="mt-1">
                    {quote.longName || quote.shortName}
                  </CardDescription>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => fetchQuote(quote.symbol)}
                  disabled={isLoading}
                  title={t.refresh}
                >
                  <RefreshCw className={`size-4 ${isLoading ? "animate-spin" : ""}`} />
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="mt-2 flex items-baseline gap-2">
                <span className="text-4xl font-bold text-foreground tracking-tight">
                  {formatPrice(quote.price, quote.currency)}
                </span>
              </div>

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

