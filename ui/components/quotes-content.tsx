"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { ArrowLeft, Loader2, LogOut, Search, TrendingUp } from "lucide-react"
import Image from "next/image"
import Link from "next/link"

export function QuotesContent() {
  const { user, logout } = useAuth()
  const { t } = useI18n()
  const router = useRouter()

  const [ticker, setTicker] = useState("")
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    const symbol = ticker.trim().toUpperCase()
    if (!symbol) return
    router.push(`/quotes/${encodeURIComponent(symbol)}`)
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
        <Card>
          <CardContent className="pt-6">
            <form onSubmit={handleSearch} className="flex gap-2">
              <Input
                value={ticker}
                onChange={(e) => setTicker(e.target.value.toUpperCase())}
                placeholder={t.tickerPlaceholder}
                className="flex-1 font-mono uppercase"
                maxLength={12}
                autoFocus
              />
              <Button type="submit" disabled={!ticker.trim()} className="gap-2">
                <Search className="size-4" />
                {t.search}
              </Button>
            </form>
            <p className="mt-2 text-xs text-muted-foreground">{t.quotesCacheNote}</p>
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
