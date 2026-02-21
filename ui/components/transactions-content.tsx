"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import {
    Pagination,
    PaginationContent,
    PaginationEllipsis,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious,
} from "@/components/ui/pagination"
import {
    ArrowLeft,
    ArrowDownRight,
    ArrowUpRight,
    DollarSign,
    FileText,
    Loader2,
    LogOut,
    Upload,
    X,
} from "lucide-react"

const API_BASE = process.env.API_BASE_URL || "http://localhost:8080"

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

export function TransactionsContent() {
    const { user, logout } = useAuth()
    const { t } = useI18n()

    const [accounts, setAccounts] = useState<BankAccount[]>([])
    const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null)
    const [isLoadingAccounts, setIsLoadingAccounts] = useState(true)

    const [transactions, setTransactions] = useState<Transaction[]>([])
    const [isLoadingTransactions, setIsLoadingTransactions] = useState(false)
    const [currentPage, setCurrentPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)
    const [pageSize] = useState(10)

    const [file, setFile] = useState<File | null>(null)
    const [isDragging, setIsDragging] = useState(false)
    const [isUploading, setIsUploading] = useState(false)
    const [uploadError, setUploadError] = useState("")
    const [uploadSuccess, setUploadSuccess] = useState("")
    const [showUpload, setShowUpload] = useState(false)
    const fileInputRef = useRef<HTMLInputElement>(null)

    const [isLoggingOut, setIsLoggingOut] = useState(false)

    // Month and year filter - initialized with current month/year
    const [month, setMonth] = useState<number>(new Date().getMonth() + 1)
    const [year, setYear] = useState<number>(new Date().getFullYear())

    const displayName = user?.completeUsername || user?.username || "User"
    const initials = displayName
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)

    // Fetch all accounts for the selector
    const fetchAccounts = useCallback(async () => {
        setIsLoadingAccounts(true)
        try {
            const res = await fetch(`${API_BASE}/bank-accounts?page=0&itemsPerPage=100`, {
                credentials: "include"
            })
            if (res.ok) {
                const data: PageResponse = await res.json()
                setAccounts(data.content)
                if (data.content.length > 0 && !selectedAccountId) {
                    setSelectedAccountId(data.content[0].id)
                }
            }
        } catch {
            // silently fail
        } finally {
            setIsLoadingAccounts(false)
        }
    }, [selectedAccountId])

    // Fetch transactions for selected account
    const fetchTransactions = useCallback(async (accountId: number, page: number = 0, selectedMonth?: number, selectedYear?: number) => {
        setIsLoadingTransactions(true)
        try {
            const monthParam = selectedMonth ?? month
            const yearParam = selectedYear ?? year
            const res = await fetch(`${API_BASE}/transactions/${accountId}?page=${page}&size=${pageSize}&month=${monthParam}&year=${yearParam}`, {
                credentials: "include"
            })
            if (res.ok) {
                const data: TransactionsPageResponse = await res.json()
                setTransactions(data.content ?? [])
                setTotalPages(data.totalPages ?? 0)
                setTotalElements(data.totalElements ?? 0)
                setCurrentPage(data.number ?? 0)
            }
        } catch {
            // silently fail
        } finally {
            setIsLoadingTransactions(false)
        }
    }, [pageSize, month, year])

    useEffect(() => {
        fetchAccounts()
    }, [fetchAccounts])

    useEffect(() => {
        if (selectedAccountId) {
            setCurrentPage(0)
            fetchTransactions(selectedAccountId, 0)
        }
    }, [selectedAccountId, month, year, fetchTransactions])

    function handleDragOver(e: React.DragEvent) {
        e.preventDefault()
        setIsDragging(true)
    }

    function handleDragLeave(e: React.DragEvent) {
        e.preventDefault()
        setIsDragging(false)
    }

    function handleDrop(e: React.DragEvent) {
        e.preventDefault()
        setIsDragging(false)
        const droppedFile = e.dataTransfer.files[0]
        if (droppedFile && droppedFile.type === "application/pdf") {
            setFile(droppedFile)
            setUploadError("")
        }
    }

    function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
        const selectedFile = e.target.files?.[0]
        if (selectedFile && selectedFile.type === "application/pdf") {
            setFile(selectedFile)
            setUploadError("")
        }
    }

    async function handleUpload() {
        setUploadError("")
        setUploadSuccess("")

        if (!selectedAccountId) {
            setUploadError(t.noAccountSelected)
            return
        }
        if (!file) {
            setUploadError(t.noFileSelected)
            return
        }

        setIsUploading(true)
        try {
            const formData = new FormData()
            formData.append("file", file)

            const res = await fetch(`${API_BASE}/transactions/itau/${selectedAccountId}`, {
                method: "POST",
                body: formData,
                credentials: "include"
            })

            if (res.status === 202 || res.ok) {
                setUploadSuccess(t.uploadSuccess)
                setFile(null)
                if (fileInputRef.current) fileInputRef.current.value = ""
                setShowUpload(false)
                // Refresh transactions
                fetchTransactions(selectedAccountId)
            } else {
                setUploadError(t.uploadError)
            }
        } catch {
            setUploadError(t.uploadError)
        } finally {
            setIsUploading(false)
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

    function formatDate(dateStr: string) {
        try {
            return new Intl.DateTimeFormat("en-US", {
                month: "short",
                day: "numeric",
                year: "numeric",
            }).format(new Date(dateStr))
        } catch {
            return dateStr
        }
    }

    return (
        <div className="min-h-svh bg-background">
            {/* Header */}
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
                {/* Back link */}
                <div className="mb-6">
                    <Link
                        href="/accounts"
                        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
                    >
                        <ArrowLeft className="size-4" />
                        {t.backToAccounts}
                    </Link>
                </div>

                {/* Page heading + account selector + upload button */}
                <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                    <div className="flex flex-col gap-3">
                        <div>
                            <h1 className="text-2xl font-bold text-foreground tracking-tight">
                                {t.transactions}
                            </h1>
                            <p className="mt-1 text-muted-foreground">{t.transactionsDescription}</p>
                        </div>

                        {/* Account selector + Month/Year filters */}
                        {!isLoadingAccounts && accounts.length > 0 && (
                            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:gap-3">
                                <div className="flex flex-col gap-1.5">
                                    <Label htmlFor="account-select" className="text-xs text-muted-foreground">{t.selectAccount}</Label>
                                    <select
                                        id="account-select"
                                        value={selectedAccountId ?? ""}
                                        onChange={(e) => setSelectedAccountId(Number(e.target.value))}
                                        className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                                    >
                                        {accounts.map((acc) => (
                                            <option key={acc.id} value={acc.id}>
                                                {acc.name}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="flex flex-col gap-1.5">
                                    <Label htmlFor="month-select" className="text-xs text-muted-foreground">{t.month || 'Month'}</Label>
                                    <select
                                        id="month-select"
                                        value={month}
                                        onChange={(e) => setMonth(Number(e.target.value))}
                                        className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                                    >
                                        <option value={1}>January</option>
                                        <option value={2}>February</option>
                                        <option value={3}>March</option>
                                        <option value={4}>April</option>
                                        <option value={5}>May</option>
                                        <option value={6}>June</option>
                                        <option value={7}>July</option>
                                        <option value={8}>August</option>
                                        <option value={9}>September</option>
                                        <option value={10}>October</option>
                                        <option value={11}>November</option>
                                        <option value={12}>December</option>
                                    </select>
                                </div>

                                <div className="flex flex-col gap-1.5">
                                    <Label htmlFor="year-select" className="text-xs text-muted-foreground">{t.year || 'Year'}</Label>
                                    <select
                                        id="year-select"
                                        value={year}
                                        onChange={(e) => setYear(Number(e.target.value))}
                                        className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                                    >
                                        {[...Array(5)].map((_, i) => {
                                            const y = new Date().getFullYear() - i
                                            return (
                                                <option key={y} value={y}>
                                                    {y}
                                                </option>
                                            )
                                        })}
                                    </select>
                                </div>
                            </div>
                        )}
                    </div>

                    <Button
                        onClick={() => {
                            setShowUpload(!showUpload)
                            setUploadError("")
                            setUploadSuccess("")
                        }}
                        className="gap-2 self-start"
                    >
                        <Upload className="size-4" />
                        {t.uploadPdf}
                    </Button>
                </div>

                {/* Upload success message */}
                {uploadSuccess && (
                    <div className="mb-6 rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm text-primary">
                        {uploadSuccess}
                    </div>
                )}

                {/* Upload area */}
                {showUpload && (
                    <Card className="mb-8">
                        <CardHeader>
                            <CardTitle className="text-lg">{t.uploadPdf}</CardTitle>
                            <CardDescription>{t.uploadPdfDescription}</CardDescription>
                        </CardHeader>
                        <CardContent className="flex flex-col gap-4">
                            {/* Drop zone */}
                            <div
                                onDragOver={handleDragOver}
                                onDragLeave={handleDragLeave}
                                onDrop={handleDrop}
                                onClick={() => fileInputRef.current?.click()}
                                role="button"
                                tabIndex={0}
                                onKeyDown={(e) => {
                                    if (e.key === "Enter" || e.key === " ") fileInputRef.current?.click()
                                }}
                                className={`relative flex flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed px-6 py-12 transition-colors cursor-pointer ${
                                    isDragging
                                        ? "border-primary bg-primary/5"
                                        : "border-border hover:border-primary/50 hover:bg-muted/50"
                                }`}
                            >
                                <div className="flex size-14 items-center justify-center rounded-full bg-muted">
                                    <Upload className="size-6 text-muted-foreground" />
                                </div>
                                <div className="text-center">
                                    <p className="text-sm font-medium text-foreground">{t.dragAndDrop}</p>
                                    <p className="mt-1 text-xs text-muted-foreground">
                                        {t.dragAndDropOr}{" "}
                                        <span className="font-medium text-primary">{t.browseFiles}</span>
                                    </p>
                                    <p className="mt-2 text-xs text-muted-foreground">{t.pdfOnly}</p>
                                </div>
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept="application/pdf"
                                    onChange={handleFileSelect}
                                    className="sr-only"
                                    aria-label={t.browseFiles}
                                />
                            </div>

                            {/* Selected file display */}
                            {file && (
                                <div className="flex items-center justify-between rounded-lg border bg-muted/50 px-4 py-3">
                                    <div className="flex items-center gap-3">
                                        <FileText className="size-5 text-primary" />
                                        <div>
                                            <p className="text-sm font-medium text-foreground">{file.name}</p>
                                            <p className="text-xs text-muted-foreground">
                                                {(file.size / 1024).toFixed(1)} KB
                                            </p>
                                        </div>
                                    </div>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => {
                                            setFile(null)
                                            if (fileInputRef.current) fileInputRef.current.value = ""
                                        }}
                                        aria-label={t.removeFile}
                                    >
                                        <X className="size-4" />
                                    </Button>
                                </div>
                            )}

                            {uploadError && (
                                <p className="text-sm text-destructive">{uploadError}</p>
                            )}

                            <div className="flex justify-end">
                                <Button
                                    onClick={handleUpload}
                                    disabled={isUploading || !file || !selectedAccountId}
                                    className="gap-2"
                                >
                                    {isUploading ? (
                                        <Loader2 className="size-4 animate-spin" />
                                    ) : (
                                        <Upload className="size-4" />
                                    )}
                                    {isUploading ? t.uploading : t.uploadPdf}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                )}

                {/* Transactions list */}
                {isLoadingAccounts || isLoadingTransactions ? (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="size-8 animate-spin text-primary" />
                    </div>
                ) : accounts.length === 0 ? (
                    <Card>
                        <CardContent className="flex flex-col items-center justify-center py-16">
                            <div className="flex size-16 items-center justify-center rounded-full bg-muted mb-4">
                                <FileText className="size-8 text-muted-foreground" />
                            </div>
                            <h3 className="text-lg font-semibold text-foreground">{t.noAccounts}</h3>
                            <p className="mt-1 text-sm text-muted-foreground">{t.noAccountsDescription}</p>
                            <Button asChild className="mt-4">
                                <Link href="/accounts">{t.bankAccounts}</Link>
                            </Button>
                        </CardContent>
                    </Card>
                ) : transactions.length === 0 ? (
                    <Card>
                        <CardContent className="flex flex-col items-center justify-center py-16">
                            <div className="flex size-16 items-center justify-center rounded-full bg-muted mb-4">
                                <FileText className="size-8 text-muted-foreground" />
                            </div>
                            <h3 className="text-lg font-semibold text-foreground">{t.noTransactions}</h3>
                            <p className="mt-1 text-sm text-muted-foreground">{t.noTransactionsDescription}</p>
                        </CardContent>
                    </Card>
                ) : (
                    <Card>
                        <CardHeader>
                            <CardTitle>{t.transactions}</CardTitle>
                            <CardDescription>
                                {accounts.find((a) => a.id === selectedAccountId)?.name}
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            {/* Table header */}
                            <div className="hidden sm:grid sm:grid-cols-[1fr_2fr_1fr] gap-4 border-b pb-3 mb-2">
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{t.transactionDate}</p>
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{t.transactionDescription}</p>
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider text-right">{t.transactionAmount}</p>
                            </div>

                            <div className="flex flex-col gap-1">
                                {transactions.map((tx) => (
                                    <div
                                        key={tx.id}
                                        className="flex flex-col gap-1 rounded-lg border p-4 sm:grid sm:grid-cols-[1fr_2fr_1fr] sm:items-center sm:gap-4 sm:border-0 sm:p-3 sm:hover:bg-muted/50 sm:transition-colors"
                                    >
                                        <p className="text-sm text-muted-foreground">{formatDate(tx.transactionDate)}</p>
                                        <div className="flex items-center gap-2">
                                            <div className={`flex size-7 shrink-0 items-center justify-center rounded-full ${
                                                tx.amount >= 0
                                                    ? "bg-primary/10 text-primary"
                                                    : "bg-destructive/10 text-destructive"
                                            }`}>
                                                {tx.amount >= 0 ? (
                                                    <ArrowDownRight className="size-3.5" />
                                                ) : (
                                                    <ArrowUpRight className="size-3.5" />
                                                )}
                                            </div>
                                            <p className="text-sm font-medium text-foreground">{tx.description}</p>
                                        </div>
                                        <p className={`text-sm font-semibold text-right ${
                                            tx.amount >= 0 ? "text-primary" : "text-destructive"
                                        }`}>
                                            {tx.amount >= 0 ? "+" : ""}{formatCurrency(tx.amount)}
                                        </p>
                                    </div>
                                ))}
                            </div>

                            {/* Pagination */}
                            {totalPages > 1 && (
                                <div className="mt-6 flex items-center justify-between border-t pt-4">
                                    <p className="text-sm text-muted-foreground">
                                        {t.page} {currentPage + 1} {t.of} {totalPages} ({totalElements} total)
                                    </p>
                                    <Pagination>
                                        <PaginationContent>
                                            <PaginationItem>
                                                <PaginationPrevious
                                                    onClick={() => {
                                                        if (currentPage > 0 && selectedAccountId) {
                                                            fetchTransactions(selectedAccountId, currentPage - 1)
                                                        }
                                                    }}
                                                    className={currentPage === 0 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                                                />
                                            </PaginationItem>

                                            {/* First page */}
                                            {currentPage > 2 && (
                                                <>
                                                    <PaginationItem>
                                                        <PaginationLink
                                                            onClick={() => selectedAccountId && fetchTransactions(selectedAccountId, 0)}
                                                            className="cursor-pointer"
                                                        >
                                                            1
                                                        </PaginationLink>
                                                    </PaginationItem>
                                                    {currentPage > 3 && (
                                                        <PaginationItem>
                                                            <PaginationEllipsis />
                                                        </PaginationItem>
                                                    )}
                                                </>
                                            )}

                                            {/* Previous page */}
                                            {currentPage > 0 && (
                                                <PaginationItem>
                                                    <PaginationLink
                                                        onClick={() => selectedAccountId && fetchTransactions(selectedAccountId, currentPage - 1)}
                                                        className="cursor-pointer"
                                                    >
                                                        {currentPage}
                                                    </PaginationLink>
                                                </PaginationItem>
                                            )}

                                            {/* Current page */}
                                            <PaginationItem>
                                                <PaginationLink isActive className="cursor-default">
                                                    {currentPage + 1}
                                                </PaginationLink>
                                            </PaginationItem>

                                            {/* Next page */}
                                            {currentPage < totalPages - 1 && (
                                                <PaginationItem>
                                                    <PaginationLink
                                                        onClick={() => selectedAccountId && fetchTransactions(selectedAccountId, currentPage + 1)}
                                                        className="cursor-pointer"
                                                    >
                                                        {currentPage + 2}
                                                    </PaginationLink>
                                                </PaginationItem>
                                            )}

                                            {/* Last page */}
                                            {currentPage < totalPages - 3 && (
                                                <>
                                                    {currentPage < totalPages - 4 && (
                                                        <PaginationItem>
                                                            <PaginationEllipsis />
                                                        </PaginationItem>
                                                    )}
                                                    <PaginationItem>
                                                        <PaginationLink
                                                            onClick={() => selectedAccountId && fetchTransactions(selectedAccountId, totalPages - 1)}
                                                            className="cursor-pointer"
                                                        >
                                                            {totalPages}
                                                        </PaginationLink>
                                                    </PaginationItem>
                                                </>
                                            )}

                                            <PaginationItem>
                                                <PaginationNext
                                                    onClick={() => {
                                                        if (currentPage < totalPages - 1 && selectedAccountId) {
                                                            fetchTransactions(selectedAccountId, currentPage + 1)
                                                        }
                                                    }}
                                                    className={currentPage >= totalPages - 1 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                                                />
                                            </PaginationItem>
                                        </PaginationContent>
                                    </Pagination>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                )}
            </main>
        </div>
    )
}
