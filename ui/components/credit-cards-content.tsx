"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
import Image from "next/image"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import {
    ArrowLeft,
    ChevronLeft,
    ChevronRight,
    CreditCard,
    DollarSign,
    Loader2,
    LogOut,
    Plus,
} from "lucide-react"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

interface CreditCardData {
    id: number
    name: string
    cardNumber: string
    brand: string
    username: string
}

interface PageResponse {
    content: CreditCardData[]
    totalPages: number
    totalElements: number
    number: number
    size: number
    first: boolean
    last: boolean
}

export function CreditCardsContent() {
    const { user, logout } = useAuth()
    const { t } = useI18n()
    const [cards, setCards] = useState<CreditCardData[]>([])
    const [isLoadingCards, setIsLoadingCards] = useState(true)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [isFirst, setIsFirst] = useState(true)
    const [isLast, setIsLast] = useState(true)
    const [pageSize, setPageSize] = useState(10)

    // Form state
    const [name, setName] = useState("")
    const [cardNumber, setCardNumber] = useState("")
    const [brand, setBrand] = useState("")
    const [isCreating, setIsCreating] = useState(false)
    const [formError, setFormError] = useState("")
    const [formSuccess, setFormSuccess] = useState("")
    const [showForm, setShowForm] = useState(false)

    const [isLoggingOut, setIsLoggingOut] = useState(false)

    const displayName = user?.completeUsername || user?.username || "User"
    const initials = displayName
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)

    const fetchCards = useCallback(async (pageNum: number) => {
        setIsLoadingCards(true)
        try {
            const res = await fetch(`${API_BASE}/credit-cards?page=${pageNum}&itemsPerPage=${pageSize}`, {
                credentials: "include"
            })

            if (res.ok) {
                const data: PageResponse = await res.json()
                setCards(data.content)
                setTotalPages(data.totalPages)
                setIsFirst(data.first)
                setIsLast(data.last)
            }
        } catch {
            // silently fail
        } finally {
            setIsLoadingCards(false)
        }
    }, [pageSize])

    useEffect(() => {
        fetchCards(page)
    }, [page, fetchCards])

    useEffect(() => {
        setPage(0)
        fetchCards(0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pageSize])

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault()
        setFormError("")
        setFormSuccess("")

        if (!name.trim()) {
            setFormError(t.creditCardNameRequired)
            return
        }

        if (!cardNumber.trim() || cardNumber.length !== 4) {
            setFormError(t.creditCardNumber4Digits)
            return
        }

        setIsCreating(true)
        try {
            const res = await fetch(`${API_BASE}/credit-cards`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name: name.trim(),
                    cardNumber: cardNumber.trim(),
                    brand: brand.trim() || "Other",
                }),
                credentials: "include"
            })

            if (res.ok) {
                setFormSuccess(t.creditCardCreated)
                setName("")
                setCardNumber("")
                setBrand("")
                setShowForm(false)
                setPage(0)
                fetchCards(0)
            } else {
                setFormError(t.creditCardCreateError)
            }
        } catch {
            setFormError(t.creditCardCreateError)
        } finally {
            setIsCreating(false)
        }
    }

    async function handleLogout() {
        setIsLoggingOut(true)
        try {
            await logout()
        } finally {
            setIsLoggingOut(false)
        }
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
                            {t.creditCards}
                        </h1>
                        <p className="mt-1 text-muted-foreground">{t.creditCardsDescription}</p>
                    </div>
                    <Button
                        onClick={() => {
                            setShowForm(!showForm)
                            setFormError("")
                            setFormSuccess("")
                        }}
                        className="gap-2 self-start"
                    >
                        <Plus className="size-4" />
                        {t.createCreditCard}
                    </Button>
                </div>

                {formSuccess && (
                    <div className="mb-6 rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm text-primary">
                        {formSuccess}
                    </div>
                )}

                {showForm && (
                    <Card className="mb-8">
                        <CardHeader>
                            <CardTitle className="text-lg">{t.createCreditCard}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <form onSubmit={handleCreate} className="flex flex-col gap-4">
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <div className="flex flex-col gap-2">
                                        <Label htmlFor="card-name">{t.creditCardName}</Label>
                                        <Input
                                            id="card-name"
                                            type="text"
                                            placeholder={t.creditCardNamePlaceholder}
                                            value={name}
                                            onChange={(e) => setName(e.target.value)}
                                            autoFocus
                                        />
                                    </div>
                                    <div className="flex flex-col gap-2">
                                        <Label htmlFor="card-number">{t.creditCardNumber}</Label>
                                        <Input
                                            id="card-number"
                                            type="text"
                                            maxLength={4}
                                            placeholder={t.creditCardNumberPlaceholder}
                                            value={cardNumber}
                                            onChange={(e) => setCardNumber(e.target.value.replace(/\D/g, ''))}
                                        />
                                    </div>
                                    <div className="flex flex-col gap-2">
                                        <Label htmlFor="card-brand">{t.creditCardBrand}</Label>
                                        <Input
                                            id="card-brand"
                                            type="text"
                                            placeholder={t.creditCardBrandPlaceholder}
                                            value={brand}
                                            onChange={(e) => setBrand(e.target.value)}
                                        />
                                    </div>
                                </div>

                                {formError && (
                                    <p className="text-sm text-destructive">{formError}</p>
                                )}

                                <div className="flex justify-end">
                                    <Button type="submit" disabled={isCreating} className="gap-2">
                                        {isCreating ? (
                                            <Loader2 className="size-4 animate-spin" />
                                        ) : (
                                            <Plus className="size-4" />
                                        )}
                                        {isCreating ? t.creating : t.createCreditCard}
                                    </Button>
                                </div>
                            </form>
                        </CardContent>
                    </Card>
                )}

                {isLoadingCards ? (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="size-8 animate-spin text-primary" />
                    </div>
                ) : cards.length === 0 ? (
                    <Card>
                        <CardContent className="flex flex-col items-center justify-center py-16">
                            <div className="flex size-16 items-center justify-center rounded-full bg-muted mb-4">
                                <CreditCard className="size-8 text-muted-foreground" />
                            </div>
                            <h3 className="text-lg font-semibold text-foreground">{t.noCreditCards}</h3>
                            <p className="mt-1 text-sm text-muted-foreground">{t.noCreditCardsDescription}</p>
                        </CardContent>
                    </Card>
                ) : (
                    <>
                        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                            {cards.map((card) => (
                                <Card key={card.id} className="group transition-shadow hover:shadow-md">
                                    <CardContent className="pt-6">
                                        <div className="flex items-start justify-between">
                                            <div className="flex size-11 items-center justify-center rounded-lg bg-primary/10 text-primary">
                                                <CreditCard className="size-5" />
                                            </div>
                                        </div>
                                        <div className="mt-4">
                                            <p className="text-sm text-muted-foreground">{t.creditCardName}</p>
                                            <p className="text-base font-semibold text-foreground">{card.name}</p>
                                        </div>
                                        <div className="mt-3">
                                            <p className="text-xs text-muted-foreground">{t.creditCardBrand}</p>
                                            <p className="text-sm font-medium text-foreground">{card.brand}</p>
                                        </div>
                                        <div className="mt-3">
                                            <p className="text-xs text-muted-foreground">{t.creditCardNumber}</p>
                                            <p className="text-sm font-mono text-foreground">**** **** **** {card.cardNumber}</p>
                                        </div>
                                        <Link
                                            href={`/credit-cards/${card.id}/transactions`}
                                            className="mt-4 inline-flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
                                        >
                                            <CreditCard className="size-3.5" />
                                            {t.creditCardStatement}
                                        </Link>
                                    </CardContent>
                                </Card>
                            ))}
                        </div>

                        <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
                            <div className="flex items-center gap-2">
                                <span className="text-sm text-muted-foreground">{t.pageSize}:</span>
                                <select
                                    value={pageSize}
                                    onChange={(e) => setPageSize(Number(e.target.value))}
                                    className="h-8 rounded-md border border-input bg-background px-2 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                                >
                                    {[10, 20, 50, 100].map((s) => (
                                        <option key={s} value={s}>{s}</option>
                                    ))}
                                </select>
                            </div>
                            {totalPages > 1 && (
                                <>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        disabled={isFirst}
                                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                                        className="gap-1"
                                    >
                                        <ChevronLeft className="size-4" />
                                        {t.previous}
                                    </Button>
                                    <span className="text-sm text-muted-foreground">
                                        {t.page} {page + 1} {t.of} {totalPages}
                                    </span>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        disabled={isLast}
                                        onClick={() => setPage((p) => p + 1)}
                                        className="gap-1"
                                    >
                                        {t.next}
                                        <ChevronRight className="size-4" />
                                    </Button>
                                </>
                            )}
                        </div>
                    </>
                )}
            </main>
        </div>
    )
}

