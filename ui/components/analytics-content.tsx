"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
import Image from "next/image"
import { useTheme } from "next-themes"
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
    ArrowLeft,
    DollarSign,
    Loader2,
    LogOut,
    Tags,
    TrendingDown,
    TrendingUp,
} from "lucide-react"
import { LineChart, Line, BarChart, Bar, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

interface MonthlySummary {
    month: number
    income: number
    expenses: number
}

interface SpendingByTagItem {
    tag: string
    total: number
}

const BAR_COLORS = [
    "#6366f1", "#f59e0b", "#10b981", "#ef4444", "#3b82f6",
    "#ec4899", "#14b8a6", "#f97316", "#a855f7", "#84cc16",
    "#06b6d4", "#e11d48",
]

const MONTH_NAMES = [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
]

export function AnalyticsContent() {
    const { user, logout } = useAuth()
    const { t } = useI18n()
    const { resolvedTheme } = useTheme()
    const isDark = resolvedTheme === "dark"

    const chartColors = {
        tickFill: isDark ? "#a1a1aa" : "#71717a",
        gridStroke: isDark ? "#3f3f46" : "#e4e4e7",
        tooltipBg: isDark ? "#18181b" : "#ffffff",
        tooltipBorder: isDark ? "#3f3f46" : "#e4e4e7",
        tooltipFg: isDark ? "#fafafa" : "#09090b",
    }
    const [isLoggingOut, setIsLoggingOut] = useState(false)
    // Map of currency → 12 monthly summaries
    const [summaryByCurrency, setSummaryByCurrency] = useState<Record<string, MonthlySummary[]>>({})
    const [spendingByTag, setSpendingByTag] = useState<Record<string, SpendingByTagItem[]>>({})
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
            const [summaryRes, tagsRes] = await Promise.all([
                fetch(`${API_BASE}/transactions/summary?year=${selectedYear}`, { credentials: "include" }),
                fetch(`${API_BASE}/transactions/spending-by-tag?year=${selectedYear}`, { credentials: "include" }),
            ])
            if (summaryRes.ok) {
                const data: Record<string, MonthlySummary[]> = await summaryRes.json()
                setSummaryByCurrency(data)
            }
            if (tagsRes.ok) {
                const data: Record<string, SpendingByTagItem[]> = await tagsRes.json()
                setSpendingByTag(data)
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

    const now = new Date()
    const currentMonth = selectedYear === now.getFullYear() ? now.getMonth() + 1 : 12
    const currencyEntries = Object.entries(summaryByCurrency)

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

                        <Button variant="outline" size="sm" onClick={handleLogout} disabled={isLoggingOut} className="gap-2">
                            {isLoggingOut ? <Loader2 className="size-4 animate-spin" /> : <LogOut className="size-4" />}
                            <span className="hidden sm:inline">{t.signOut}</span>
                        </Button>
                    </div>
                </div>
            </header>

            <main className="mx-auto max-w-6xl px-4 py-8 lg:px-8">
                <div className="mb-6">
                    <Link href="/dashboard" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors">
                        <ArrowLeft className="size-4" />
                        {t.backToDashboard}
                    </Link>
                </div>

                <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-foreground tracking-tight">Financial Analytics</h1>
                        <p className="mt-1 text-muted-foreground">Monthly income and expenses overview</p>
                    </div>
                    <Select value={selectedYear.toString()} onValueChange={(v) => setSelectedYear(parseInt(v))}>
                        <SelectTrigger className="w-36"><SelectValue /></SelectTrigger>
                        <SelectContent>
                            {Array.from({ length: 10 }, (_, i) => now.getFullYear() - i).map((y) => (
                                <SelectItem key={y} value={y.toString()}>{y}</SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                {isLoading ? (
                    <div className="flex items-center justify-center py-24">
                        <Loader2 className="size-8 animate-spin text-primary" />
                    </div>
                ) : currencyEntries.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-24 text-center">
                        <p className="text-sm text-muted-foreground">No data available</p>
                        <p className="text-xs text-muted-foreground mt-1">Upload transactions to see your financial trends</p>
                    </div>
                ) : (
                    <div className="flex flex-col gap-10">
                        {currencyEntries.map(([currency, months]) => {
                            const filtered = months.filter(m => m.month <= currentMonth)
                            const chartData = filtered.map(m => ({
                                month: MONTH_NAMES[m.month - 1],
                                Income: Math.abs(m.income),
                                Expenses: Math.abs(m.expenses),
                            }))
                            const totalIncome = filtered.reduce((s, m) => s + m.income, 0)
                            const totalExpenses = filtered.reduce((s, m) => s + m.expenses, 0)
                            const netSavings = totalIncome - totalExpenses

                            return (
                                <div key={currency}>
                                    <p className="mb-4 text-xs font-semibold uppercase tracking-widest text-muted-foreground">{currency}</p>

                                    {/* Summary cards */}
                                    <div className="grid gap-4 sm:grid-cols-3 mb-6">
                                        <Card>
                                            <CardContent className="pt-6">
                                                <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary mb-3">
                                                    <TrendingUp className="size-5" />
                                                </div>
                                                <p className="text-sm text-muted-foreground">Total Income</p>
                                                <p className="text-2xl font-bold tracking-tight text-foreground">{formatCurrency(totalIncome, currency)}</p>
                                            </CardContent>
                                        </Card>
                                        <Card>
                                            <CardContent className="pt-6">
                                                <div className="flex size-10 items-center justify-center rounded-lg bg-destructive/10 text-destructive mb-3">
                                                    <TrendingDown className="size-5" />
                                                </div>
                                                <p className="text-sm text-muted-foreground">Total Expenses</p>
                                                <p className="text-2xl font-bold tracking-tight text-foreground">{formatCurrency(totalExpenses, currency)}</p>
                                            </CardContent>
                                        </Card>
                                        <Card>
                                            <CardContent className="pt-6">
                                                <div className={`flex size-10 items-center justify-center rounded-lg mb-3 ${netSavings >= 0 ? "bg-primary/10 text-primary" : "bg-destructive/10 text-destructive"}`}>
                                                    <DollarSign className="size-5" />
                                                </div>
                                                <p className="text-sm text-muted-foreground">Net Savings</p>
                                                <p className={`text-2xl font-bold tracking-tight ${netSavings >= 0 ? "text-primary" : "text-destructive"}`}>{formatCurrency(netSavings, currency)}</p>
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
                                            {chartData.length === 0 ? (
                                                <div className="flex flex-col items-center justify-center py-16 text-center">
                                                    <p className="text-sm text-muted-foreground">No transactions this year</p>
                                                </div>
                                            ) : (
                                                <div className="h-80 w-full">
                                                    <ResponsiveContainer width="100%" height="100%">
                                                        <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                                                            <CartesianGrid strokeDasharray="3 3" stroke={chartColors.gridStroke} />
                                                            <XAxis dataKey="month" tick={{ fill: chartColors.tickFill, fontSize: 12 }} axisLine={{ stroke: chartColors.gridStroke }} tickLine={{ stroke: chartColors.gridStroke }} />
                                                            <YAxis tick={{ fill: chartColors.tickFill, fontSize: 12 }} axisLine={{ stroke: chartColors.gridStroke }} tickLine={{ stroke: chartColors.gridStroke }} tickFormatter={(v) => formatCurrency(v, currency)} />
                                                            <Tooltip
                                                                contentStyle={{ backgroundColor: chartColors.tooltipBg, border: `1px solid ${chartColors.tooltipBorder}`, borderRadius: "8px", color: chartColors.tooltipFg }}
                                                                labelStyle={{ color: chartColors.tooltipFg }}
                                                                itemStyle={{ color: chartColors.tooltipFg }}
                                                                formatter={(v: number) => formatCurrency(v, currency)}
                                                            />
                                                            <Legend wrapperStyle={{ paddingTop: "20px", color: chartColors.tickFill }} iconType="line" />
                                                            <Line type="monotone" dataKey="Income" stroke="#22c55e" strokeWidth={2} dot={{ fill: "#22c55e" }} activeDot={{ r: 8 }} />
                                                            <Line type="monotone" dataKey="Expenses" stroke="#ef4444" strokeWidth={2} dot={{ fill: "#ef4444" }} activeDot={{ r: 8 }} />
                                                        </LineChart>
                                                    </ResponsiveContainer>
                                                </div>
                                            )}
                                        </CardContent>
                                    </Card>

                                    {/* Spending by category chart */}
                                    <Card>
                                        <CardHeader>
                                            <CardTitle>{t.spendingByCategory}</CardTitle>
                                            <CardDescription>{t.spendingByCategoryDescription}</CardDescription>
                                        </CardHeader>
                                        <CardContent>
                                            {!spendingByTag[currency] || spendingByTag[currency].length === 0 ? (
                                                <div className="flex flex-col items-center justify-center py-16 text-center">
                                                    <Tags className="size-8 text-muted-foreground mb-3" />
                                                    <p className="text-sm font-medium text-muted-foreground">{t.noTagsForChart}</p>
                                                    <p className="text-xs text-muted-foreground mt-1">{t.noTagsForChartDescription}</p>
                                                </div>
                                            ) : (
                                                <div className="h-80 w-full">
                                                    <ResponsiveContainer width="100%" height="100%">
                                                        <BarChart
                                                            data={spendingByTag[currency]}
                                                            margin={{ top: 5, right: 30, left: 20, bottom: 40 }}
                                                        >
                                                            <CartesianGrid strokeDasharray="3 3" stroke={chartColors.gridStroke} />
                                                            <XAxis
                                                                dataKey="tag"
                                                                tick={{ fill: chartColors.tickFill, fontSize: 12 }}
                                                                axisLine={{ stroke: chartColors.gridStroke }}
                                                                tickLine={{ stroke: chartColors.gridStroke }}
                                                                angle={-30}
                                                                textAnchor="end"
                                                                interval={0}
                                                            />
                                                            <YAxis
                                                                tick={{ fill: chartColors.tickFill, fontSize: 12 }}
                                                                axisLine={{ stroke: chartColors.gridStroke }}
                                                                tickLine={{ stroke: chartColors.gridStroke }}
                                                                tickFormatter={(v) => formatCurrency(v, currency)}
                                                            />
                                                            <Tooltip
                                                                contentStyle={{ backgroundColor: chartColors.tooltipBg, border: `1px solid ${chartColors.tooltipBorder}`, borderRadius: "8px", color: chartColors.tooltipFg }}
                                                                labelStyle={{ color: chartColors.tooltipFg }}
                                                                itemStyle={{ color: chartColors.tooltipFg }}
                                                                formatter={(v: number) => formatCurrency(v, currency)}
                                                            />
                                                            <Bar dataKey="total" radius={[4, 4, 0, 0]}>
                                                                {spendingByTag[currency].map((_, index) => (
                                                                    <Cell key={index} fill={BAR_COLORS[index % BAR_COLORS.length]} />
                                                                ))}
                                                            </Bar>
                                                        </BarChart>
                                                    </ResponsiveContainer>
                                                </div>
                                            )}
                                        </CardContent>
                                    </Card>
                                </div>
                            )
                        })}
                    </div>
                )}
            </main>
        </div>
    )
}
