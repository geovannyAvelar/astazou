"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
import Image from "next/image"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { formatCurrency } from "@/lib/currency"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  BarChart2,
  Briefcase,
  Calendar,
  CreditCard,
  Landmark,
  Loader2,
  LogOut,
  TrendingUp,
  Wallet,
} from "lucide-react"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

interface BalanceByCurrency {
  currency: string
  income: number
  expenses: number
  amount: number
}

interface Transaction {
  id: number
  transactionDate: string
  description: string
  amount: number
  type: string
  page: number
  createdAt: string
  bankAccountId: number
  sequence: number
}

interface TransactionsPageResponse {
  content: Transaction[]
  totalPages: number
  totalElements: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
  numberOfElements: number
}


export function DashboardContent() {
  const { user, logout } = useAuth()
  const { t } = useI18n()
  const router = useRouter()
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [balances, setBalances] = useState<BalanceByCurrency[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [isLoadingTransactions, setIsLoadingTransactions] = useState(true)

  // Get current month and year
  const now = new Date()
  const [selectedMonth, setSelectedMonth] = useState<number>(now.getMonth() + 1) // 1-12
  const [selectedYear, setSelectedYear] = useState<number>(now.getFullYear())

  const fetchBalance = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/transactions/balance?month=${selectedMonth}&year=${selectedYear}`, {
        credentials: "include"
      })
      if (res.ok) {
        const data: BalanceByCurrency[] = await res.json()
        setBalances(data)
      }
    } catch (error) {
      console.error("Failed to fetch balance:", error)
    }
  }, [selectedMonth, selectedYear])

  const fetchLastTransactions = useCallback(async () => {
    setIsLoadingTransactions(true)
    try {
      const res = await fetch(`${API_BASE}/transactions/last`, {
        credentials: "include"
      })
      if (res.ok) {
        const data: TransactionsPageResponse = await res.json()
        setTransactions(data.content ?? [])
      }
    } catch (error) {
      console.error("Failed to fetch transactions:", error)
    } finally {
      setIsLoadingTransactions(false)
    }
  }, [])

  useEffect(() => {
    fetchBalance()
    fetchLastTransactions()
  }, [fetchBalance, fetchLastTransactions])

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
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2)

  function getGreeting() {
    const hour = new Date().getHours()
    if (hour < 12) return t.greetingMorning
    if (hour < 17) return t.greetingAfternoon
    return t.greetingEvening
  }

  function formatDate(dateString: string) {
    const date = new Date(dateString)
    return new Intl.DateTimeFormat("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    }).format(date)
  }

  return (
    <div className="min-h-svh bg-background">
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

      <main className="mx-auto max-w-6xl px-4 py-8 lg:px-8">
        <div className="mb-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h1 className="text-2xl font-bold text-foreground tracking-tight">
                {getGreeting()}, {displayName.split(" ")[0]}
              </h1>
              <p className="mt-1 text-muted-foreground">
                {t.dashboardOverview}
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Calendar className="size-4 text-muted-foreground" />
              <Select
                value={selectedMonth.toString()}
                onValueChange={(value) => setSelectedMonth(parseInt(value))}
              >
                <SelectTrigger className="w-[140px]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {t.months.map((name, i) => (
                    <SelectItem key={i + 1} value={(i + 1).toString()}>
                      {name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select
                value={selectedYear.toString()}
                onValueChange={(value) => setSelectedYear(parseInt(value))}
              >
                <SelectTrigger className="w-[100px]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Array.from({ length: 10 }, (_, i) => now.getFullYear() - i).map((year) => (
                    <SelectItem key={year} value={year.toString()}>
                      {year}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>

        {/* Summary cards per currency */}
        {balances.length === 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <SummaryCard title={t.totalBalance}   value={formatCurrency(0, "BRL")} icon={<Wallet className="size-5" />} />
            <SummaryCard title={t.monthlyIncome}  value={formatCurrency(0, "BRL")} icon={<TrendingUp className="size-5" />} />
            <SummaryCard title={t.monthlyExpenses} value={formatCurrency(0, "BRL")} icon={<CreditCard className="size-5" />} />
          </div>
        ) : (
          <div className="flex flex-col gap-6">
            {balances.map((b) => (
              <div key={b.currency}>
                <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-muted-foreground">{b.currency}</p>
                <div className="grid gap-4 sm:grid-cols-3">
                  <SummaryCard
                    title={t.totalBalance}
                    value={formatCurrency(b.amount, b.currency)}
                    icon={<Wallet className="size-5" />}
                    valueClassName={b.amount < 0 ? "text-destructive" : "text-foreground"}
                  />
                  <SummaryCard
                    title={t.monthlyIncome}
                    value={formatCurrency(b.income, b.currency)}
                    icon={<TrendingUp className="size-5" />}
                  />
                  <SummaryCard
                    title={t.monthlyExpenses}
                    value={formatCurrency(b.expenses, b.currency)}
                    icon={<CreditCard className="size-5" />}
                  />
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="mt-8">
          <Link href="/accounts" className="group">
            <Card className="transition-shadow hover:shadow-md">
              <CardContent className="flex items-center justify-between pt-6">
                <div className="flex items-center gap-4">
                  <div className="flex size-12 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Landmark className="size-6" />
                  </div>
                  <div>
                    <p className="text-base font-semibold text-foreground">{t.bankAccounts}</p>
                    <p className="text-sm text-muted-foreground">{t.bankAccountsDescription}</p>
                  </div>
                </div>
                <ArrowRight className="size-5 text-muted-foreground transition-transform group-hover:translate-x-1" />
              </CardContent>
            </Card>
          </Link>
        </div>

        <div className="mt-4">
          <Link href="/analytics" className="group">
            <Card className="transition-shadow hover:shadow-md">
              <CardContent className="flex items-center justify-between pt-6">
                <div className="flex items-center gap-4">
                  <div className="flex size-12 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <TrendingUp className="size-6" />
                  </div>
                  <div>
                    <p className="text-base font-semibold text-foreground">Analytics</p>
                    <p className="text-sm text-muted-foreground">View your financial trends and insights</p>
                  </div>
                </div>
                <ArrowRight className="size-5 text-muted-foreground transition-transform group-hover:translate-x-1" />
              </CardContent>
            </Card>
          </Link>
        </div>

        <div className="mt-4">
          <Link href="/credit-cards" className="group">
            <Card className="transition-shadow hover:shadow-md">
              <CardContent className="flex items-center justify-between pt-6">
                <div className="flex items-center gap-4">
                  <div className="flex size-12 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <CreditCard className="size-6" />
                  </div>
                  <div>
                    <p className="text-base font-semibold text-foreground">{t.creditCards}</p>
                    <p className="text-sm text-muted-foreground">{t.creditCardsDescription}</p>
                  </div>
                </div>
                <ArrowRight className="size-5 text-muted-foreground transition-transform group-hover:translate-x-1" />
              </CardContent>
            </Card>
          </Link>
        </div>

        <div className="mt-4">
          <Link href="/quotes" className="group">
            <Card className="transition-shadow hover:shadow-md">
              <CardContent className="flex items-center justify-between pt-6">
                <div className="flex items-center gap-4">
                  <div className="flex size-12 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <BarChart2 className="size-6" />
                  </div>
                  <div>
                    <p className="text-base font-semibold text-foreground">{t.stockQuotes}</p>
                    <p className="text-sm text-muted-foreground">{t.stockQuotesDescription}</p>
                  </div>
                </div>
                <ArrowRight className="size-5 text-muted-foreground transition-transform group-hover:translate-x-1" />
              </CardContent>
            </Card>
          </Link>
        </div>

        <div className="mt-4">
          <Link href="/investments" className="group">
            <Card className="transition-shadow hover:shadow-md">
              <CardContent className="flex items-center justify-between pt-6">
                <div className="flex items-center gap-4">
                  <div className="flex size-12 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Briefcase className="size-6" />
                  </div>
                  <div>
                    <p className="text-base font-semibold text-foreground">{t.investments}</p>
                    <p className="text-sm text-muted-foreground">{t.investmentsDescription}</p>
                  </div>
                </div>
                <ArrowRight className="size-5 text-muted-foreground transition-transform group-hover:translate-x-1" />
              </CardContent>
            </Card>
          </Link>
        </div>

        <div className="mt-8">
          <Card>
            <CardHeader>
              <CardTitle>{t.recentTransactions}</CardTitle>
              <CardDescription>{t.recentTransactionsDescription}</CardDescription>
            </CardHeader>
            <CardContent>
              {isLoadingTransactions ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="size-8 animate-spin text-primary" />
                </div>
              ) : transactions.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                  <p className="text-sm text-muted-foreground">No transactions yet</p>
                  <p className="text-xs text-muted-foreground mt-1">Upload a bank statement to get started</p>
                </div>
              ) : (
                <div className="flex flex-col gap-4">
                  {transactions.map((tx) => (
                    <div key={tx.id} className="flex items-center justify-between rounded-lg border p-4">
                      <div className="flex items-center gap-3">
                        <div className={`flex size-10 items-center justify-center rounded-full ${
                          tx.amount >= 0
                            ? "bg-primary/10 text-primary"
                            : "bg-destructive/10 text-destructive"
                        }`}>
                          {tx.amount >= 0 ? (
                            <ArrowDownRight className="size-5" />
                          ) : (
                            <ArrowUpRight className="size-5" />
                          )}
                        </div>
                        <div>
                          <p className="text-sm font-medium text-foreground">{tx.description}</p>
                          <p className="text-xs text-muted-foreground">{formatDate(tx.transactionDate)}</p>
                        </div>
                      </div>
                      <span className={`text-sm font-semibold ${
                        tx.amount >= 0 ? "text-primary" : "text-destructive"
                      }`}>
                        {tx.amount >= 0 ? "+" : ""}{tx.amount.toFixed(2)}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  )
}

function SummaryCard({
  title,
  value,
  icon,
  valueClassName,
}: {
  title: string
  value: string
  icon: React.ReactNode
  valueClassName?: string
}) {
  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex items-center justify-between">
          <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
            {icon}
          </div>
        </div>
        <div className="mt-3">
          <p className="text-sm text-muted-foreground">{title}</p>
          <p className={`text-2xl font-bold tracking-tight ${valueClassName || "text-foreground"}`}>{value}</p>
        </div>
      </CardContent>
    </Card>
  )
}
