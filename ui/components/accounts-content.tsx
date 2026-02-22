"use client"

import { useState, useEffect, useCallback } from "react"
import Link from "next/link"
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
    DollarSign,
    FileText,
    Landmark,
    Loader2,
    LogOut,
    Pencil,
    Plus,
    Wallet,
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

interface BankAccount {
    id: number
    name: string
    balance: number
    username: string
}

interface PageResponse {
    content: BankAccount[]
    totalPages: number
    totalElements: number
    number: number
    size: number
    first: boolean
    last: boolean
}

export function AccountsContent() {
    const { user, logout } = useAuth()
    const { t } = useI18n()
    const [accounts, setAccounts] = useState<BankAccount[]>([])
    const [isLoadingAccounts, setIsLoadingAccounts] = useState(true)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [isFirst, setIsFirst] = useState(true)
    const [isLast, setIsLast] = useState(true)

    // Form state
    const [name, setName] = useState("")
    const [initialBalance, setInitialBalance] = useState("")
    const [isCreating, setIsCreating] = useState(false)
    const [formError, setFormError] = useState("")
    const [formSuccess, setFormSuccess] = useState("")
    const [showForm, setShowForm] = useState(false)

    // Edit state
    const [editingAccount, setEditingAccount] = useState<BankAccount | null>(null)
    const [editName, setEditName] = useState("")
    const [editBalance, setEditBalance] = useState("")
    const [isUpdating, setIsUpdating] = useState(false)
    const [editError, setEditError] = useState("")
    const [editSuccess, setEditSuccess] = useState("")

    const [isLoggingOut, setIsLoggingOut] = useState(false)

    const displayName = user?.completeUsername || user?.username || "User"
    const initials = displayName
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)

    const fetchAccounts = useCallback(async (pageNum: number) => {
        setIsLoadingAccounts(true)
        try {
            const res = await fetch(`${API_BASE}/bank-accounts?page=${pageNum}&itemsPerPage=10`, {
                credentials: "include"
            })
            if (res.ok) {
                const data: PageResponse = await res.json()
                setAccounts(data.content)
                setTotalPages(data.totalPages)
                setIsFirst(data.first)
                setIsLast(data.last)
            }
        } catch {
            // silently fail
        } finally {
            setIsLoadingAccounts(false)
        }
    }, [])

    useEffect(() => {
        fetchAccounts(page)
    }, [page, fetchAccounts])

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault()
        setFormError("")
        setFormSuccess("")

        if (!name.trim()) {
            setFormError(t.accountNameRequired)
            return
        }

        setIsCreating(true)
        try {
            const res = await fetch(`${API_BASE}/bank-accounts`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name: name.trim(),
                    initialBalance: initialBalance ? parseFloat(initialBalance) : 0,
                }),
                credentials: "include"
            })

            if (res.ok) {
                setFormSuccess(t.accountCreated)
                setName("")
                setInitialBalance("")
                setShowForm(false)
                setPage(0)
                fetchAccounts(0)
            } else {
                setFormError(t.accountCreateError)
            }
        } catch {
            setFormError(t.accountCreateError)
        } finally {
            setIsCreating(false)
        }
    }

    function openEditDialog(account: BankAccount) {
        setEditingAccount(account)
        setEditName(account.name)
        setEditBalance(account.balance.toString())
        setEditError("")
        setEditSuccess("")
    }

    function closeEditDialog() {
        setEditingAccount(null)
        setEditName("")
        setEditBalance("")
        setEditError("")
        setEditSuccess("")
    }

    async function handleUpdate(e: React.FormEvent) {
        e.preventDefault()
        if (!editingAccount) return

        setEditError("")
        setEditSuccess("")

        if (!editName.trim()) {
            setEditError(t.accountNameRequired)
            return
        }

        setIsUpdating(true)
        try {
            const res = await fetch(`${API_BASE}/bank-accounts/${editingAccount.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name: editName.trim(),
                    balance: editBalance ? parseFloat(editBalance) : 0,
                }),
                credentials: "include"
            })

            if (res.ok) {
                setEditSuccess(t.accountUpdated)
                fetchAccounts(page)
                setTimeout(() => {
                    closeEditDialog()
                }, 1000)
            } else {
                setEditError(t.accountUpdateError)
            }
        } catch {
            setEditError(t.accountUpdateError)
        } finally {
            setIsUpdating(false)
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

    function formatCurrency(value: number) {
        return new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: "USD",
        }).format(value)
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
                            {t.bankAccounts}
                        </h1>
                        <p className="mt-1 text-muted-foreground">{t.bankAccountsDescription}</p>
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
                        {t.createAccount}
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
                            <CardTitle className="text-lg">{t.createAccount}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <form onSubmit={handleCreate} className="flex flex-col gap-4">
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <div className="flex flex-col gap-2">
                                        <Label htmlFor="account-name">{t.accountName}</Label>
                                        <Input
                                            id="account-name"
                                            type="text"
                                            placeholder={t.accountNamePlaceholder}
                                            value={name}
                                            onChange={(e) => setName(e.target.value)}
                                            autoFocus
                                        />
                                    </div>
                                    <div className="flex flex-col gap-2">
                                        <Label htmlFor="initial-balance">{t.initialBalance}</Label>
                                        <div className="relative">
                                            <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
                                            <Input
                                                id="initial-balance"
                                                type="number"
                                                step="0.01"
                                                min="0"
                                                placeholder={t.initialBalancePlaceholder}
                                                value={initialBalance}
                                                onChange={(e) => setInitialBalance(e.target.value)}
                                                className="pl-9"
                                            />
                                        </div>
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
                                        {isCreating ? t.creating : t.createAccount}
                                    </Button>
                                </div>
                            </form>
                        </CardContent>
                    </Card>
                )}

                {isLoadingAccounts ? (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="size-8 animate-spin text-primary" />
                    </div>
                ) : accounts.length === 0 ? (
                    <Card>
                        <CardContent className="flex flex-col items-center justify-center py-16">
                            <div className="flex size-16 items-center justify-center rounded-full bg-muted mb-4">
                                <Landmark className="size-8 text-muted-foreground" />
                            </div>
                            <h3 className="text-lg font-semibold text-foreground">{t.noAccounts}</h3>
                            <p className="mt-1 text-sm text-muted-foreground">{t.noAccountsDescription}</p>
                        </CardContent>
                    </Card>
                ) : (
                    <>
                        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                            {accounts.map((account) => (
                                <Card key={account.id} className="group transition-shadow hover:shadow-md">
                                    <CardContent className="pt-6">
                                        <div className="flex items-start justify-between">
                                            <div className="flex size-11 items-center justify-center rounded-lg bg-primary/10 text-primary">
                                                <Wallet className="size-5" />
                                            </div>
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                className="size-8 text-muted-foreground hover:text-foreground"
                                                onClick={() => openEditDialog(account)}
                                            >
                                                <Pencil className="size-4" />
                                            </Button>
                                        </div>
                                        <div className="mt-4">
                                            <p className="text-sm text-muted-foreground">{t.accountName}</p>
                                            <p className="text-base font-semibold text-foreground">{account.name}</p>
                                        </div>
                                        <div className="mt-3 flex items-baseline gap-1">
                                            <p className="text-xs text-muted-foreground">{t.balance}</p>
                                            <p className="text-xl font-bold text-foreground tracking-tight">
                                                {formatCurrency(account.balance)}
                                            </p>
                                        </div>
                                        <Link
                                            href="/transactions"
                                            className="mt-4 inline-flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
                                        >
                                            <FileText className="size-3.5" />
                                            {t.transactions}
                                        </Link>
                                    </CardContent>
                                </Card>
                            ))}
                        </div>

                        {totalPages > 1 && (
                            <div className="mt-8 flex items-center justify-center gap-4">
                                <Button
                                    variant="outline"
                                    size="sm"
                                    disabled={isFirst}
                                    onClick={() => setPage((p) => Math.max(1, p - 1))}
                                    className="gap-1"
                                >
                                    <ChevronLeft className="size-4" />
                                    {t.previous}
                                </Button>
                                <span className="text-sm text-muted-foreground">
                  {t.page} {page} {t.of} {totalPages}
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
                            </div>
                        )}
                    </>
                )}
            </main>

            {/* Edit Account Dialog */}
            <Dialog open={!!editingAccount} onOpenChange={(open) => !open && closeEditDialog()}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{t.editAccount}</DialogTitle>
                        <DialogDescription>{t.editAccountDescription}</DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleUpdate}>
                        <div className="flex flex-col gap-4 py-4">
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="edit-account-name">{t.accountName}</Label>
                                <Input
                                    id="edit-account-name"
                                    type="text"
                                    placeholder={t.accountNamePlaceholder}
                                    value={editName}
                                    onChange={(e) => setEditName(e.target.value)}
                                    autoFocus
                                />
                            </div>
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="edit-balance">{t.currentBalance}</Label>
                                <div className="relative">
                                    <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
                                    <Input
                                        id="edit-balance"
                                        type="number"
                                        step="0.01"
                                        placeholder="0.00"
                                        value={editBalance}
                                        onChange={(e) => setEditBalance(e.target.value)}
                                        className="pl-9"
                                    />
                                </div>
                            </div>

                            {editError && (
                                <p className="text-sm text-destructive">{editError}</p>
                            )}

                            {editSuccess && (
                                <p className="text-sm text-primary">{editSuccess}</p>
                            )}
                        </div>
                        <DialogFooter>
                            <Button
                                type="button"
                                variant="outline"
                                onClick={closeEditDialog}
                                disabled={isUpdating}
                            >
                                {t.cancel}
                            </Button>
                            <Button type="submit" disabled={isUpdating} className="gap-2">
                                {isUpdating ? (
                                    <Loader2 className="size-4 animate-spin" />
                                ) : (
                                    <Pencil className="size-4" />
                                )}
                                {isUpdating ? t.saving : t.save}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>
        </div>
    )
}
