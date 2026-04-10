"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
import Image from "next/image"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { useCurrency } from "@/lib/currency-context"
import { formatCurrency } from "@/lib/currency"
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
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import {
    ArrowLeft,
    ChevronLeft,
    ChevronRight,
    Loader2,
    LogOut,
    Pencil,
    Plus,
    TrendingUp,
    Trash2,
} from "lucide-react"

const API_BASE: string =
    (process.env.NEXT_PUBLIC_API_URL as string) ||
    (process.env.REACT_APP_API_BASE as string) ||
    (process.env.VITE_API_BASE as string) ||
    (process.env.API_BASE as string) ||
    "http://localhost:8080"

interface InvestmentContribution {
    id: number
    symbol: string
    purchaseDate: string
    quantity: number
    purchasePrice: number
    username: string
    createdAt: string
}

interface PageResponse {
    content: InvestmentContribution[]
    totalPages: number
    totalElements: number
    number: number
    size: number
    first: boolean
    last: boolean
}

export function InvestmentsContent() {
    const { user, logout } = useAuth()
    const { t } = useI18n()
    const { preferredCurrency } = useCurrency()

    const [contributions, setContributions] = useState<InvestmentContribution[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [isFirst, setIsFirst] = useState(true)
    const [isLast, setIsLast] = useState(true)
    const [pageSize, setPageSize] = useState(10)

    // Create form
    const [showForm, setShowForm] = useState(false)
    const [newSymbol, setNewSymbol] = useState("")
    const [newDate, setNewDate] = useState("")
    const [newQuantity, setNewQuantity] = useState("")
    const [newPrice, setNewPrice] = useState("")
    const [isCreating, setIsCreating] = useState(false)
    const [formError, setFormError] = useState("")

    // Edit dialog
    const [editItem, setEditItem] = useState<InvestmentContribution | null>(null)
    const [editSymbol, setEditSymbol] = useState("")
    const [editDate, setEditDate] = useState("")
    const [editQuantity, setEditQuantity] = useState("")
    const [editPrice, setEditPrice] = useState("")
    const [isUpdating, setIsUpdating] = useState(false)
    const [editError, setEditError] = useState("")

    // Delete dialog
    const [deleteItem, setDeleteItem] = useState<InvestmentContribution | null>(null)
    const [isDeleting, setIsDeleting] = useState(false)

    const [isLoggingOut, setIsLoggingOut] = useState(false)

    const displayName = user?.completeUsername || user?.username || "User"
    const initials = displayName
        .split(" ")
        .map((n: string) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)

    const fetchContributions = useCallback(
        async (pageNum: number) => {
            setIsLoading(true)
            try {
                const res = await fetch(
                    `${API_BASE}/investments?page=${pageNum}&itemsPerPage=${pageSize}`,
                    { credentials: "include" }
                )
                if (res.ok) {
                    const data: PageResponse = await res.json()
                    setContributions(data.content)
                    setTotalPages(data.totalPages)
                    setIsFirst(data.first)
                    setIsLast(data.last)
                }
            } catch {
                // silently fail
            } finally {
                setIsLoading(false)
            }
        },
        [pageSize]
    )

    useEffect(() => {
        fetchContributions(page)
    }, [page, fetchContributions])

    useEffect(() => {
        setPage(0)
        fetchContributions(0)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pageSize])

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault()
        setFormError("")

        if (!newSymbol.trim()) { setFormError(t.tickerRequired); return }
        if (!newDate) { setFormError(t.dateRequired); return }
        if (!newQuantity) { setFormError(t.quantityRequired); return }
        if (!newPrice) { setFormError(t.priceRequired); return }

        setIsCreating(true)
        try {
            const res = await fetch(`${API_BASE}/investments`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({
                    symbol: newSymbol.trim().toUpperCase(),
                    purchaseDate: newDate,
                    quantity: parseFloat(newQuantity),
                    purchasePrice: parseFloat(newPrice),
                }),
            })
            if (res.ok) {
                setNewSymbol("")
                setNewDate("")
                setNewQuantity("")
                setNewPrice("")
                setShowForm(false)
                setPage(0)
                fetchContributions(0)
            } else {
                setFormError(t.contributionAddError)
            }
        } catch {
            setFormError(t.contributionAddError)
        } finally {
            setIsCreating(false)
        }
    }

    function openEdit(item: InvestmentContribution) {
        setEditItem(item)
        setEditSymbol(item.symbol)
        setEditDate(item.purchaseDate)
        setEditQuantity(item.quantity.toString())
        setEditPrice(item.purchasePrice.toString())
        setEditError("")
    }

    function closeEdit() {
        setEditItem(null)
        setEditError("")
    }

    async function handleUpdate(e: React.FormEvent) {
        e.preventDefault()
        if (!editItem) return
        setEditError("")

        setIsUpdating(true)
        try {
            const res = await fetch(`${API_BASE}/investments/${editItem.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({
                    symbol: editSymbol.trim().toUpperCase(),
                    purchaseDate: editDate,
                    quantity: parseFloat(editQuantity),
                    purchasePrice: parseFloat(editPrice),
                }),
            })
            if (res.ok) {
                fetchContributions(page)
                setTimeout(() => closeEdit(), 600)
            } else {
                setEditError(t.contributionUpdateError)
            }
        } catch {
            setEditError(t.contributionUpdateError)
        } finally {
            setIsUpdating(false)
        }
    }

    async function handleDelete() {
        if (!deleteItem) return
        setIsDeleting(true)
        try {
            const res = await fetch(`${API_BASE}/investments/${deleteItem.id}`, {
                method: "DELETE",
                credentials: "include",
            })
            if (res.ok) {
                setDeleteItem(null)
                fetchContributions(page)
            }
        } catch {
            // silently fail
        } finally {
            setIsDeleting(false)
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

    function formatDate(date: string) {
        return new Date(date + "T00:00:00").toLocaleDateString()
    }

    function formatInvestmentCurrency(value: number) {
        return formatCurrency(value, preferredCurrency)
    }

    function formatQuantity(value: number) {
        return new Intl.NumberFormat("en-US", {
            maximumFractionDigits: 8,
        }).format(value)
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
                            {isLoggingOut ? <Loader2 className="size-4 animate-spin" /> : <LogOut className="size-4" />}
                            <span className="hidden sm:inline">{t.signOut}</span>
                        </Button>
                    </div>
                </div>
            </header>

            <main className="mx-auto max-w-6xl px-4 py-8 lg:px-8">
                {/* Back link */}
                <div className="mb-6">
                    <Link
                        href="/dashboard"
                        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
                    >
                        <ArrowLeft className="size-4" />
                        {t.backToDashboard}
                    </Link>
                </div>

                {/* Page title + add button */}
                <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-foreground tracking-tight">{t.investments}</h1>
                        <p className="mt-1 text-muted-foreground">{t.investmentsDescription}</p>
                    </div>
                    <Button
                        onClick={() => { setShowForm(!showForm); setFormError("") }}
                        className="gap-2 self-start"
                    >
                        <Plus className="size-4" />
                        {t.addContribution}
                    </Button>
                </div>

                {/* Create form */}
                {showForm && (
                    <Card className="mb-8">
                        <CardHeader>
                            <CardTitle className="text-lg">{t.addContribution}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <form onSubmit={handleCreate} className="flex flex-col gap-4">
                                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                                    <div className="flex flex-col gap-2">
                                        <Label htmlFor="new-symbol">{t.ticker}</Label>
                                        <Input
                                            id="new-symbol"
                                            type="text"
                                            placeholder="PETR4"
                                            value={newSymbol}
                                            onChange={(e) => setNewSymbol(e.target.value.toUpperCase())}
                                            autoFocus
                                        />
                                    </div>
                                    <div className="flex flex-col gap-2">
                                        <Label htmlFor="new-date">{t.purchaseDate}</Label>
                                        <Input
                                            id="new-date"
                                            type="date"
                                            value={newDate}
                                            onChange={(e) => setNewDate(e.target.value)}
                                        />
                                    </div>
                                    <div className="flex flex-col gap-2">
                                        <Label htmlFor="new-quantity">{t.quantity}</Label>
                                        <Input
                                            id="new-quantity"
                                            type="number"
                                            step="0.00000001"
                                            min="0"
                                            placeholder="100"
                                            value={newQuantity}
                                            onChange={(e) => setNewQuantity(e.target.value)}
                                        />
                                    </div>
                                    <div className="flex flex-col gap-2">
                                        <Label htmlFor="new-price">{t.purchasePrice}</Label>
                                        <Input
                                            id="new-price"
                                            type="number"
                                            step="0.01"
                                            min="0"
                                            placeholder="0.00"
                                            value={newPrice}
                                            onChange={(e) => setNewPrice(e.target.value)}
                                        />
                                    </div>
                                </div>

                                {formError && <p className="text-sm text-destructive">{formError}</p>}

                                <div className="flex justify-end">
                                    <Button type="submit" disabled={isCreating} className="gap-2">
                                        {isCreating ? (
                                            <Loader2 className="size-4 animate-spin" />
                                        ) : (
                                            <Plus className="size-4" />
                                        )}
                                        {isCreating ? t.creating : t.addContribution}
                                    </Button>
                                </div>
                            </form>
                        </CardContent>
                    </Card>
                )}

                {/* List */}
                {isLoading ? (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="size-8 animate-spin text-primary" />
                    </div>
                ) : contributions.length === 0 ? (
                    <Card>
                        <CardContent className="flex flex-col items-center justify-center py-16">
                            <div className="flex size-16 items-center justify-center rounded-full bg-muted mb-4">
                                <TrendingUp className="size-8 text-muted-foreground" />
                            </div>
                            <h3 className="text-lg font-semibold text-foreground">{t.noInvestments}</h3>
                            <p className="mt-1 text-sm text-muted-foreground">{t.noInvestmentsDescription}</p>
                        </CardContent>
                    </Card>
                ) : (
                    <>
                        <div className="overflow-x-auto rounded-lg border">
                            <table className="w-full text-sm">
                                <thead className="border-b bg-muted/50">
                                    <tr>
                                        <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t.ticker}</th>
                                        <th className="px-4 py-3 text-left font-medium text-muted-foreground">{t.purchaseDate}</th>
                                        <th className="px-4 py-3 text-right font-medium text-muted-foreground">{t.quantity}</th>
                                        <th className="px-4 py-3 text-right font-medium text-muted-foreground">{t.purchasePrice}</th>
                                        <th className="px-4 py-3 text-right font-medium text-muted-foreground">{t.totalInvested}</th>
                                        <th className="px-4 py-3 text-center font-medium text-muted-foreground">{t.actions}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {contributions.map((item, idx) => (
                                        <tr
                                            key={item.id}
                                            className={`border-b last:border-b-0 transition-colors hover:bg-muted/30 ${idx % 2 === 0 ? "" : "bg-muted/10"}`}
                                        >
                                            <td className="px-4 py-3">
                                                <span className="inline-flex items-center gap-2">
                                                    <div className="flex size-7 items-center justify-center rounded-md bg-primary/10 text-primary">
                                                        <TrendingUp className="size-3.5" />
                                                    </div>
                                                    <span className="font-semibold text-foreground">{item.symbol}</span>
                                                </span>
                                            </td>
                                            <td className="px-4 py-3 text-muted-foreground">{formatDate(item.purchaseDate)}</td>
                                            <td className="px-4 py-3 text-right font-medium text-foreground">{formatQuantity(item.quantity)}</td>
                                            <td className="px-4 py-3 text-right text-muted-foreground">{formatInvestmentCurrency(item.purchasePrice)}</td>
                                            <td className="px-4 py-3 text-right font-semibold text-foreground">
                                                {formatInvestmentCurrency(item.quantity * item.purchasePrice)}
                                            </td>
                                            <td className="px-4 py-3">
                                                <div className="flex items-center justify-center gap-1">
                                                    <Button
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8 text-muted-foreground hover:text-foreground"
                                                        onClick={() => openEdit(item)}
                                                    >
                                                        <Pencil className="size-4" />
                                                    </Button>
                                                    <Button
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8 text-muted-foreground hover:text-destructive"
                                                        onClick={() => setDeleteItem(item)}
                                                    >
                                                        <Trash2 className="size-4" />
                                                    </Button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {/* Pagination */}
                        <div className="mt-6 flex flex-wrap items-center justify-center gap-4">
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

            {/* Edit Dialog */}
            <Dialog open={!!editItem} onOpenChange={(open) => !open && closeEdit()}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{t.editContribution}</DialogTitle>
                        <DialogDescription>{t.editContributionDescription}</DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleUpdate}>
                        <div className="flex flex-col gap-4 py-4">
                            <div className="grid gap-4 sm:grid-cols-2">
                                <div className="flex flex-col gap-2">
                                    <Label htmlFor="edit-symbol">{t.ticker}</Label>
                                    <Input
                                        id="edit-symbol"
                                        type="text"
                                        value={editSymbol}
                                        onChange={(e) => setEditSymbol(e.target.value.toUpperCase())}
                                        autoFocus
                                    />
                                </div>
                                <div className="flex flex-col gap-2">
                                    <Label htmlFor="edit-date">{t.purchaseDate}</Label>
                                    <Input
                                        id="edit-date"
                                        type="date"
                                        value={editDate}
                                        onChange={(e) => setEditDate(e.target.value)}
                                    />
                                </div>
                                <div className="flex flex-col gap-2">
                                    <Label htmlFor="edit-quantity">{t.quantity}</Label>
                                    <Input
                                        id="edit-quantity"
                                        type="number"
                                        step="0.00000001"
                                        min="0"
                                        value={editQuantity}
                                        onChange={(e) => setEditQuantity(e.target.value)}
                                    />
                                </div>
                                <div className="flex flex-col gap-2">
                                    <Label htmlFor="edit-price">{t.purchasePrice}</Label>
                                    <Input
                                        id="edit-price"
                                        type="number"
                                        step="0.01"
                                        min="0"
                                        value={editPrice}
                                        onChange={(e) => setEditPrice(e.target.value)}
                                    />
                                </div>
                            </div>

                            {editError && <p className="text-sm text-destructive">{editError}</p>}
                        </div>
                        <DialogFooter>
                            <Button type="button" variant="outline" onClick={closeEdit} disabled={isUpdating}>
                                {t.cancel}
                            </Button>
                            <Button type="submit" disabled={isUpdating} className="gap-2">
                                {isUpdating ? <Loader2 className="size-4 animate-spin" /> : <Pencil className="size-4" />}
                                {isUpdating ? t.saving : t.save}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            {/* Delete Alert */}
            <AlertDialog open={!!deleteItem} onOpenChange={(open) => !open && setDeleteItem(null)}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t.deleteContribution}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t.deleteContributionDescription}
                            {deleteItem && (
                                <span className="mt-1 block font-medium text-foreground">
                                    {deleteItem.symbol} — {formatDate(deleteItem.purchaseDate)} — {formatQuantity(deleteItem.quantity)} × {formatInvestmentCurrency(deleteItem.purchasePrice)}
                                </span>
                            )}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel disabled={isDeleting}>{t.cancel}</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDelete}
                            disabled={isDeleting}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90 gap-2"
                        >
                            {isDeleting ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
                            {isDeleting ? t.deleting : t.delete}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    )
}

