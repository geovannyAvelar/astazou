"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
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
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  CreditCard,
  DollarSign,
  Landmark,
  Loader2,
  LogOut,
  TrendingUp,
  Wallet,
} from "lucide-react"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

interface Balance {
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
  const [balance, setBalance] = useState<Balance | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [isLoadingTransactions, setIsLoadingTransactions] = useState(true)

  const fetchBalance = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/transactions/balance`, {
        credentials: "include"
      })
      if (res.ok) {
        const data: Balance = await res.json()
        setBalance(data)
      }
    } catch (error) {
      console.error("Failed to fetch balance:", error)
    }
  }, [])

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

  function formatCurrency(amount: number) {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 2,
    }).format(Math.abs(amount))
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
              <DollarSign className="size-5" />
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
          <h1 className="text-2xl font-bold text-foreground tracking-tight">
            {getGreeting()}, {displayName.split(" ")[0]}
          </h1>
          <p className="mt-1 text-muted-foreground">
            {t.dashboardOverview}
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <SummaryCard
            title={t.totalBalance}
            value={balance ? formatCurrency(balance.amount) : "$0.00"}
            icon={<Wallet className="size-5" />}
          />
          <SummaryCard
            title={t.monthlyIncome}
            value={balance ? formatCurrency(balance.income) : "$0.00"}
            icon={<TrendingUp className="size-5" />}
          />
          <SummaryCard
            title={t.monthlyExpenses}
            value={balance ? formatCurrency(balance.expenses) : "$0.00"}
            icon={<CreditCard className="size-5" />}
          />
        </div>

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
                        {tx.amount >= 0 ? "+" : "-"}{formatCurrency(tx.amount)}
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
}: {
  title: string
  value: string
  icon: React.ReactNode
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
          <p className="text-2xl font-bold text-foreground tracking-tight">{value}</p>
        </div>
      </CardContent>
    </Card>
  )
}

