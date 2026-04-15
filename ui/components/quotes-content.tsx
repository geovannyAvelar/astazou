"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { Popover, PopoverAnchor, PopoverContent } from "@/components/ui/popover"
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandList,
} from "@/components/ui/command"
import { ArrowLeft, Loader2, LogOut, Search, TrendingUp } from "lucide-react"
import Image from "next/image"
import Link from "next/link"

const API_BASE: string =
  (process.env.NEXT_PUBLIC_API_URL as string) ||
  (process.env.REACT_APP_API_BASE as string) ||
  (process.env.VITE_API_BASE as string) ||
  (process.env.API_BASE as string) ||
  "http://localhost:8080"

interface StockSuggestion {
  ticker: string
  name: string | null
  sector: string | null
  logoUrl: string | null
}

export function QuotesContent() {
  const { user, logout } = useAuth()
  const { t } = useI18n()
  const router = useRouter()

  const [query, setQuery] = useState("")
  const [suggestions, setSuggestions] = useState<StockSuggestion[]>([])
  const [open, setOpen] = useState(false)
  const [isSearching, setIsSearching] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  // Debounced autocomplete – fires 200 ms after the user stops typing
  useEffect(() => {
    const q = query.trim()
    if (!q) {
      setSuggestions([])
      setOpen(false)
      return
    }
    const timer = setTimeout(async () => {
      setIsSearching(true)
      try {
        const res = await fetch(
          `${API_BASE}/stocks/search?q=${encodeURIComponent(q)}`,
          { credentials: "include" }
        )
        if (res.ok) {
          const data: StockSuggestion[] = await res.json()
          setSuggestions(data)
          setOpen(data.length > 0)
        }
      } catch {
        // silently ignore network errors in autocomplete
      } finally {
        setIsSearching(false)
      }
    }, 200)

    return () => clearTimeout(timer)
  }, [query])

  function navigate(ticker: string) {
    setOpen(false)
    router.push(`/quotes/${encodeURIComponent(ticker.toUpperCase())}`)
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    const symbol = query.trim().toUpperCase()
    if (!symbol) return
    navigate(symbol)
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

        {/* Search card with autocomplete */}
        <Card>
          <CardContent className="pt-6">
            <Popover open={open} onOpenChange={setOpen}>
              <form onSubmit={handleSearch}>
                {/* PopoverAnchor wraps the input so the dropdown aligns to it */}
                <PopoverAnchor asChild>
                  <div className="flex gap-2">
                    <div className="relative flex-1">
                      <Input
                        value={query}
                        onChange={(e) => setQuery(e.target.value.toUpperCase())}
                        onKeyDown={(e) => {
                          if (e.key === "Escape") setOpen(false)
                        }}
                        placeholder={t.tickerPlaceholder}
                        className="font-mono uppercase pr-8"
                        maxLength={12}
                        autoFocus
                      />
                      {isSearching && (
                        <Loader2 className="pointer-events-none absolute right-2.5 top-1/2 size-3.5 -translate-y-1/2 animate-spin text-muted-foreground" />
                      )}
                    </div>
                    <Button type="submit" disabled={!query.trim()} className="gap-2">
                      <Search className="size-4" />
                      {t.search}
                    </Button>
                  </div>
                </PopoverAnchor>
              </form>

              <PopoverContent
                className="p-0"
                align="start"
                /* keep focus in the input, not the popover */
                onOpenAutoFocus={(e) => e.preventDefault()}
                style={{ width: "var(--radix-popper-anchor-width)" }}
              >
                <Command shouldFilter={false}>
                  <CommandList>
                    <CommandEmpty className="py-4 text-center text-sm text-muted-foreground">
                      {t.noStocksFound}
                    </CommandEmpty>
                    <CommandGroup>
                      {suggestions.map((s) => (
                        <CommandItem
                          key={s.ticker}
                          value={s.ticker}
                          onSelect={() => navigate(s.ticker)}
                          className="flex cursor-pointer items-center gap-3 px-3 py-2"
                        >
                          {/* Logo thumbnail */}
                          {s.logoUrl ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img
                              src={`${API_BASE}${s.logoUrl}`}
                              alt={s.ticker}
                              width={24}
                              height={24}
                              className="size-6 rounded object-contain"
                              onError={(e) => {
                                ;(e.currentTarget as HTMLImageElement).style.display = "none"
                              }}
                            />
                          ) : (
                            <div className="flex size-6 shrink-0 items-center justify-center rounded bg-primary/10 text-primary">
                              <TrendingUp className="size-3" />
                            </div>
                          )}

                          {/* Ticker */}
                          <span className="shrink-0 font-mono font-semibold text-sm">
                            {s.ticker}
                          </span>

                          {/* Name */}
                          {s.name && (
                            <span className="truncate text-sm text-muted-foreground">
                              {s.name}
                            </span>
                          )}
                        </CommandItem>
                      ))}
                    </CommandGroup>
                  </CommandList>
                </Command>
              </PopoverContent>
            </Popover>

            <p className="mt-2 text-xs text-muted-foreground">{t.quotesCacheNote}</p>
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
