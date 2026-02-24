"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import {
    Card,
    CardContent,
} from "@/components/ui/card"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { CreditCardUpload } from "@/components/credit-card-upload"
import {
    ArrowLeft,
    CreditCard,
    DollarSign,
    Loader2,
    LogOut,
    TrendingDown,
    TrendingUp,
} from "lucide-react"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

interface CreditCardData {
    id: number
    name: string
    cardNumber: string
    brand: string
}

interface CreditCardTransaction {
    id: number
    description: string
    amount: number
    transactionDate: string
    dueDate?: string
}

interface CardTransactionsContentProps {
    cardId: number
}

export function CreditCardTransactionsContent({ cardId }: CardTransactionsContentProps) {
    const { user, logout } = useAuth()
    const { t } = useI18n()
    const [card, setCard] = useState<CreditCardData | null>(null)
    const [transactions, setTransactions] = useState<CreditCardTransaction[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isLoadingTransactions, setIsLoadingTransactions] = useState(false)

    const [selectedMonth, setSelectedMonth] = useState<string>("")
    const [selectedYear, setSelectedYear] = useState<string>("")

    const [isLoggingOut, setIsLoggingOut] = useState(false)

    const displayName = user?.completeUsername || user?.username || "User"
    const initials = displayName
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)

    // Initialize month and year to current
    useEffect(() => {
        const now = new Date()
        const month = String(now.getMonth() + 1).padStart(2, '0')
        const year = String(now.getFullYear())
        setSelectedMonth(month)
        setSelectedYear(year)
    }, [])

    // Fetch card info
    useEffect(() => {
        const fetchCard = async () => {
            setIsLoading(true)
            try {
                const res = await fetch(`${API_BASE}/credit-cards/${cardId}`, {
                    credentials: "include"
                })
                if (res.ok) {
                    const data = await res.json()
                    setCard(data)
                }
            } catch {
                // silently fail
            } finally {
                setIsLoading(false)
            }
        }
        fetchCard()
    }, [cardId])

    // Fetch transactions when month/year changes
    useEffect(() => {
        if (!selectedMonth || !selectedYear) return

        const fetchTransactions = async () => {
            setIsLoadingTransactions(true)
            try {
                const month = parseInt(selectedMonth)
                const year = parseInt(selectedYear)
                const res = await fetch(
                    `${API_BASE}/credit-cards/transactions/${cardId}/${month}/${year}`,
                    { credentials: "include" }
                )
                if (res.ok) {
                    const data = await res.json()
                    // Handle both List and PageResponse formats
                    const transactionsList = Array.isArray(data) ? data : data.content || []
                    setTransactions(transactionsList)
                }
            } catch {
                // silently fail
            } finally {
                setIsLoadingTransactions(false)
            }
        }

        fetchTransactions()
    }, [selectedMonth, selectedYear, cardId])

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
        }).format(Math.abs(value))
    }

    function formatDate(dateString: string) {
        const date = new Date(dateString)
        return new Intl.DateTimeFormat("en-US", {
            month: "short",
            day: "numeric",
            year: "numeric",
        }).format(date)
    }

    const months = Array.from({ length: 12 }, (_, i) => ({
        value: String(i + 1).padStart(2, '0'),
        label: new Date(2000, i).toLocaleDateString("en-US", { month: "long" })
    }))

    const currentYear = new Date().getFullYear()
    const years = Array.from({ length: 5 }, (_, i) => ({
        value: String(currentYear - i),
        label: String(currentYear - i)
    }))

    const totalAmount = transactions.reduce((sum, t) => sum + t.amount, 0)

    const handleRefresh = () => {
        // Refetch transactions for current month/year
        if (selectedMonth && selectedYear) {
            const month = parseInt(selectedMonth)
            const year = parseInt(selectedYear)

            setIsLoadingTransactions(true)
            fetch(
                `${API_BASE}/credit-cards/transactions/${cardId}/${month}/${year}`,
                { credentials: "include" }
            )
                .then(res => res.ok ? res.json() : null)
                .then(data => {
                    const transactionsList = Array.isArray(data) ? data : data?.content || []
                    setTransactions(transactionsList)
                })
                .catch(err => console.error(err))
                .finally(() => setIsLoadingTransactions(false))
        }
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
                <div className="mb-6">
                    <Link
                        href="/credit-cards"
                        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
                    >
                        <ArrowLeft className="size-4" />
                        {t.backToDashboard}
                    </Link>
                </div>

                {isLoading ? (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="size-8 animate-spin text-primary" />
                    </div>
                ) : card ? (
                    <>
                        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                                <h1 className="text-2xl font-bold text-foreground tracking-tight">
                                    {card.name}
                                </h1>
                                <p className="mt-1 text-muted-foreground">{t.creditCardTransactionsDescription}</p>
                            </div>
                        </div>

                        {/* Upload Section */}
                        <div className="mb-8">
                            <CreditCardUpload
                                cardId={cardId}
                                cardName={card.name}
                                onUploadSuccess={handleRefresh}
                            />
                        </div>

                        {/* Month/Year Selection */}
                        <Card className="mb-8">
                            <CardContent className="pt-6">
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <div className="flex flex-col gap-2">
                                        <label className="text-sm font-medium text-foreground">{t.month}</label>
                                        <Select value={selectedMonth} onValueChange={setSelectedMonth}>
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                {months.map(m => (
                                                    <SelectItem key={m.value} value={m.value}>
                                                        {m.label}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </div>

                                    <div className="flex flex-col gap-2">
                                        <label className="text-sm font-medium text-foreground">{t.year}</label>
                                        <Select value={selectedYear} onValueChange={setSelectedYear}>
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                {years.map(y => (
                                                    <SelectItem key={y.value} value={y.value}>
                                                        {y.label}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Statement Summary */}
                        <Card className="mb-8 border-primary/20 bg-primary/5">
                            <CardContent className="pt-6">
                                <div className="flex items-baseline gap-2">
                                    <span className="text-sm text-muted-foreground">Total for Statement:</span>
                                    <span className={`text-2xl font-bold tracking-tight ${totalAmount >= 0 ? 'text-foreground' : 'text-destructive'}`}>
                                        {totalAmount >= 0 ? '+' : ''}{formatCurrency(totalAmount)}
                                    </span>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Transactions List */}
                        {isLoadingTransactions ? (
                            <div className="flex items-center justify-center py-16">
                                <Loader2 className="size-8 animate-spin text-primary" />
                            </div>
                        ) : transactions.length === 0 ? (
                            <Card>
                                <CardContent className="flex flex-col items-center justify-center py-16">
                                    <div className="flex size-16 items-center justify-center rounded-full bg-muted mb-4">
                                        <CreditCard className="size-8 text-muted-foreground" />
                                    </div>
                                    <h3 className="text-lg font-semibold text-foreground">{t.noTransactions}</h3>
                                    <p className="mt-1 text-sm text-muted-foreground">
                                        No transactions for the selected month
                                    </p>
                                </CardContent>
                            </Card>
                        ) : (
                            <div className="space-y-3">
                                {transactions.map((transaction) => (
                                    <Card key={transaction.id} className="group transition-shadow hover:shadow-md">
                                        <CardContent className="pt-6">
                                            <div className="flex items-start justify-between">
                                                <div className="flex items-start gap-3 flex-1">
                                                    <div className={`flex size-10 items-center justify-center rounded-lg ${transaction.amount >= 0 ? 'bg-primary/10 text-primary' : 'bg-destructive/10 text-destructive'}`}>
                                                        {transaction.amount >= 0 ? (
                                                            <TrendingUp className="size-5" />
                                                        ) : (
                                                            <TrendingDown className="size-5" />
                                                        )}
                                                    </div>
                                                    <div className="flex-1">
                                                        <p className="font-medium text-foreground">{transaction.description}</p>
                                                        <p className="text-xs text-muted-foreground mt-1">
                                                            {formatDate(transaction.transactionDate)}
                                                        </p>
                                                    </div>
                                                </div>
                                                <div className="text-right">
                                                    <p className={`text-lg font-semibold tracking-tight ${transaction.amount >= 0 ? 'text-primary' : 'text-destructive'}`}>
                                                        {transaction.amount >= 0 ? '+' : ''}{formatCurrency(transaction.amount)}
                                                    </p>
                                                </div>
                                            </div>
                                        </CardContent>
                                    </Card>
                                ))}
                            </div>
                        )}
                    </>
                ) : (
                    <Card>
                        <CardContent className="flex flex-col items-center justify-center py-16">
                            <div className="flex size-16 items-center justify-center rounded-full bg-muted mb-4">
                                <CreditCard className="size-8 text-muted-foreground" />
                            </div>
                            <h3 className="text-lg font-semibold text-foreground">Card not found</h3>
                        </CardContent>
                    </Card>
                )}
            </main>
        </div>
    )
}



