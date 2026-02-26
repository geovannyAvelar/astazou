"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
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
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import {
    ArrowLeft,
    DollarSign,
    Loader2,
    LogOut,
    TrendingDown,
    TrendingUp,
} from "lucide-react"
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

interface MonthlySummary {
    month: number
    income: number
    expenses: number
}

const MONTH_NAMES = [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
]

export function AnalyticsContent() {
    const { user, logout } = useAuth()
    const { t } = useI18n()
    const [isLoggingOut, setIsLoggingOut] = useState(false)
    const [summary, setSummary] = useState<MonthlySummary[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear())

    const displayName = user?.completeUsername || user?.username || "User"
    const initials = displayName
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)

    const fetchSummary = useCallback(async () => {
        setIsLoading(true)
        try {
            const res = await fetch(`${API_BASE}/transactions/summary?year=${selectedYear}`, {
                credentials: "include"
            })
            if (res.ok) {
                const data: MonthlySummary[] = await res.json()
                setSummary(data)
            }
        } catch (error) {
            console.error("Failed to fetch summary:", error)
        } finally {
            setIsLoading(false)
        }
    }, [selectedYear])

    useEffect(() => {
        fetchSummary()
    }, [fetchSummary])

    async function handleLogout() {
        setIsLoggingOut(true)
        try {
            await logout()
        } finally {
            setIsLoggingOut(false)
        }
    }

    function formatCurrency(value: number) {
        return new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: "USD",
        }).format(value)
    }

    const chartData = summary.map(item => ({
        month: MONTH_NAMES[item.month - 1],
        income: item.income,
        expenses: item.expenses,
    }))

    const totalIncome = summary.reduce((sum, item) => sum + item.income, 0)
    const totalExpenses = summary.reduce((sum, item) => sum + item.expenses, 0)
    const netSavings = totalIncome - totalExpenses

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
                <div className="mb-6">
                    <Link
                        href="/dashboard"
                        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
                    >
                        <ArrowLeft className="size-4" />
                        {t.backToDashboard}
                    </Link>
                </div>

                <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-foreground tracking-tight">
                            Financial Analytics
                        </h1>
                        <p className="mt-1 text-muted-foreground">Monthly income and expenses overview</p>
                    </div>

                    <Select
                        value={selectedYear.toString()}
                        onValueChange={(value) => setSelectedYear(parseInt(value))}
                    >
                        <SelectTrigger className="w-36">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {Array.from({ length: 10 }, (_, i) => new Date().getFullYear() - i).map((year) => (
                                <SelectItem key={year} value={year.toString()}>
                                    {year}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                {/* Summary Cards */}
                <div className="grid gap-4 sm:grid-cols-3 mb-8">
                    <Card>
                        <CardContent className="pt-6">
                            <div className="flex items-center justify-between">
                                <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                                    <TrendingUp className="size-5" />
                                </div>
                            </div>
                            <div className="mt-3">
                                <p className="text-sm text-muted-foreground">Total Income</p>
                                <p className="text-2xl font-bold tracking-tight text-foreground">
                                    {formatCurrency(totalIncome)}
                                </p>
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardContent className="pt-6">
                            <div className="flex items-center justify-between">
                                <div className="flex size-10 items-center justify-center rounded-lg bg-destructive/10 text-destructive">
                                    <TrendingDown className="size-5" />
                                </div>
                            </div>
                            <div className="mt-3">
                                <p className="text-sm text-muted-foreground">Total Expenses</p>
                                <p className="text-2xl font-bold tracking-tight text-foreground">
                                    {formatCurrency(totalExpenses)}
                                </p>
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardContent className="pt-6">
                            <div className="flex items-center justify-between">
                                <div className={`flex size-10 items-center justify-center rounded-lg ${
                                    netSavings >= 0 ? 'bg-primary/10 text-primary' : 'bg-destructive/10 text-destructive'
                                }`}>
                                    <DollarSign className="size-5" />
                                </div>
                            </div>
                            <div className="mt-3">
                                <p className="text-sm text-muted-foreground">Net Savings</p>
                                <p className={`text-2xl font-bold tracking-tight ${
                                    netSavings >= 0 ? 'text-primary' : 'text-destructive'
                                }`}>
                                    {formatCurrency(netSavings)}
                                </p>
                            </div>
                        </CardContent>
                    </Card>
                </div>

                {/* Chart */}
                <Card>
                    <CardHeader>
                        <CardTitle>Monthly Trends</CardTitle>
                        <CardDescription>Income vs Expenses throughout the year</CardDescription>
                    </CardHeader>
                    <CardContent>
                        {isLoading ? (
                            <div className="flex items-center justify-center py-16">
                                <Loader2 className="size-8 animate-spin text-primary" />
                            </div>
                        ) : (
                            <div className="h-96 w-full">
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                                        <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                                        <XAxis
                                            dataKey="month"
                                            className="text-xs"
                                            tick={{ fill: 'hsl(var(--muted-foreground))' }}
                                        />
                                        <YAxis
                                            className="text-xs"
                                            tick={{ fill: 'hsl(var(--muted-foreground))' }}
                                            tickFormatter={(value) => `$${(value / 1000).toFixed(0)}k`}
                                        />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: 'hsl(var(--card))',
                                                border: '1px solid hsl(var(--border))',
                                                borderRadius: '8px',
                                            }}
                                            labelStyle={{ color: 'hsl(var(--foreground))' }}
                                            formatter={(value: number) => formatCurrency(value)}
                                        />
                                        <Legend
                                            wrapperStyle={{ paddingTop: '20px' }}
                                            iconType="line"
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="income"
                                            stroke="hsl(var(--primary))"
                                            strokeWidth={2}
                                            name="Income"
                                            dot={{ fill: 'hsl(var(--primary))' }}
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="expenses"
                                            stroke="hsl(var(--destructive))"
                                            strokeWidth={2}
                                            name="Expenses"
                                            dot={{ fill: 'hsl(var(--destructive))' }}
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>
                        )}
                    </CardContent>
                </Card>
            </main>
        </div>
    )
}




