"use client"

import { useState } from "react"
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
  PiggyBank,
  TrendingUp,
  Wallet,
} from "lucide-react"

export function DashboardContent() {
  const { user, logout } = useAuth()
  const { t } = useI18n()
  const router = useRouter()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

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
            value="$24,563.00"
            change="+2.5%"
            trend="up"
            icon={<Wallet className="size-5" />}
          />
          <SummaryCard
            title={t.monthlyIncome}
            value="$8,350.00"
            change="+12.3%"
            trend="up"
            icon={<TrendingUp className="size-5" />}
          />
          <SummaryCard
            title={t.monthlyExpenses}
            value="$3,420.00"
            change="-4.1%"
            trend="down"
            icon={<CreditCard className="size-5" />}
          />
          <SummaryCard
            title={t.savings}
            value="$12,890.00"
            change="+8.7%"
            trend="up"
            icon={<PiggyBank className="size-5" />}
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
              <div className="flex flex-col gap-4">
                {mockTransactions.map((tx) => (
                  <div key={tx.id} className="flex items-center justify-between rounded-lg border p-4">
                    <div className="flex items-center gap-3">
                      <div className={`flex size-10 items-center justify-center rounded-full ${
                        tx.type === "income"
                          ? "bg-primary/10 text-primary"
                          : "bg-destructive/10 text-destructive"
                      }`}>
                        {tx.type === "income" ? (
                          <ArrowDownRight className="size-5" />
                        ) : (
                          <ArrowUpRight className="size-5" />
                        )}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-foreground">{tx.description}</p>
                        <p className="text-xs text-muted-foreground">{tx.date}</p>
                      </div>
                    </div>
                    <span className={`text-sm font-semibold ${
                      tx.type === "income" ? "text-primary" : "text-destructive"
                    }`}>
                      {tx.type === "income" ? "+" : "-"}${tx.amount}
                    </span>
                  </div>
                ))}
              </div>
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
  change,
  trend,
  icon,
}: {
  title: string
  value: string
  change: string
  trend: "up" | "down"
  icon: React.ReactNode
}) {
  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex items-center justify-between">
          <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
            {icon}
          </div>
          <span className={`text-xs font-medium ${
            trend === "up" ? "text-primary" : "text-destructive"
          }`}>
            {change}
          </span>
        </div>
        <div className="mt-3">
          <p className="text-sm text-muted-foreground">{title}</p>
          <p className="text-2xl font-bold text-foreground tracking-tight">{value}</p>
        </div>
      </CardContent>
    </Card>
  )
}

const mockTransactions = [
  { id: 1, description: "Salary Deposit", date: "Feb 20, 2026", amount: "5,250.00", type: "income" as const },
  { id: 2, description: "Grocery Store", date: "Feb 19, 2026", amount: "142.30", type: "expense" as const },
  { id: 3, description: "Electric Bill", date: "Feb 18, 2026", amount: "89.00", type: "expense" as const },
  { id: 4, description: "Freelance Payment", date: "Feb 17, 2026", amount: "1,200.00", type: "income" as const },
  { id: 5, description: "Restaurant", date: "Feb 16, 2026", amount: "67.50", type: "expense" as const },
]
