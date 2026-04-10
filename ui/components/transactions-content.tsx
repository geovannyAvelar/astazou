"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import Link from "next/link"
import Image from "next/image"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { formatCurrency } from "@/lib/currency"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { Checkbox } from "@/components/ui/checkbox"
import { TransactionSearch } from "@/components/transaction-search"
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
    ArrowLeftRight,
    ArrowUpRight,
    FileDown,
    FileText,
    Loader2,
    LogOut,
    Pencil,
    Plus,
    Tag,
    Tags,
    Trash2,
    Upload,
    X,
} from "lucide-react"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

interface BankAccount {
    id: number
    name: string
    balance: number
    username: string
    currency: string
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
    tags: string[]
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

export function TransactionsContent({ preselectedAccountId }: { preselectedAccountId?: number }) {
    const { user, logout } = useAuth()
    const { t, locale } = useI18n()

    const [accounts, setAccounts] = useState<BankAccount[]>([])
    const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null)
    const [isLoadingAccounts, setIsLoadingAccounts] = useState(true)

    const [transactions, setTransactions] = useState<Transaction[]>([])
    const [isLoadingTransactions, setIsLoadingTransactions] = useState(false)
    const [currentPage, setCurrentPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)
    const [pageSize, setPageSize] = useState(10)

    const [file, setFile] = useState<File | null>(null)
    const [isDragging, setIsDragging] = useState(false)
    const [isUploading, setIsUploading] = useState(false)
    const [uploadError, setUploadError] = useState("")
    const [uploadSuccess, setUploadSuccess] = useState("")
    const [showUpload, setShowUpload] = useState(false)
    const [uploadUpdateAccount, setUploadUpdateAccount] = useState(false)
    const [uploadMode, setUploadMode] = useState<"pdf" | "ofx">("pdf")
    const fileInputRef = useRef<HTMLInputElement>(null)

    const [isLoggingOut, setIsLoggingOut] = useState(false)
    const [isGeneratingReport, setIsGeneratingReport] = useState(false)

    const [month, setMonth] = useState<number>(new Date().getMonth() + 1)
    const [year, setYear] = useState<number>(new Date().getFullYear())


    const [showCreateDialog, setShowCreateDialog] = useState(false)
    const [isCreating, setIsCreating] = useState(false)
    const [createError, setCreateError] = useState("")
    const [formData, setFormData] = useState({
        transactionDate: new Date().toISOString().split('T')[0],
        description: "",
        amount: "",
        type: "debit",
        bankAccountId: selectedAccountId || 0,
        updateAccount: true
    })


    const [showDeleteDialog, setShowDeleteDialog] = useState(false)
    const [transactionToDelete, setTransactionToDelete] = useState<Transaction | null>(null)
    const [isDeleting, setIsDeleting] = useState(false)
    const [deleteError, setDeleteError] = useState("")


    const [showTransformDialog, setShowTransformDialog] = useState(false)
    const [transactionToTransform, setTransactionToTransform] = useState<Transaction | null>(null)
    const [destinationAccountId, setDestinationAccountId] = useState<number | null>(null)
    const [isTransforming, setIsTransforming] = useState(false)
    const [transformError, setTransformError] = useState("")

    const [isSearching, setIsSearching] = useState(false)
    const [searchQuery, setSearchQuery] = useState("")
    const [searchStartDate, setSearchStartDate] = useState("")
    const [searchEndDate, setSearchEndDate] = useState("")

    const isSearchMode = Boolean(searchQuery || searchStartDate || searchEndDate)

    // Tag state
    const [showTagDialog, setShowTagDialog] = useState(false)
    const [transactionToTag, setTransactionToTag] = useState<Transaction | null>(null)
    const [tagInput, setTagInput] = useState("")
    const [editingTags, setEditingTags] = useState<string[]>([])
    const [isSavingTags, setIsSavingTags] = useState(false)
    const [tagError, setTagError] = useState("")
    const [allTags, setAllTags] = useState<string[]>([])
    const [activeTagFilter, setActiveTagFilter] = useState<string | null>(null)

    // Edit transaction state
    const [showEditDialog, setShowEditDialog] = useState(false)
    const [transactionToEdit, setTransactionToEdit] = useState<Transaction | null>(null)
    const [isEditing, setIsEditing] = useState(false)
    const [editError, setEditError] = useState("")
    const [editFormData, setEditFormData] = useState({
        transactionDate: "",
        description: "",
        amount: "",
        type: "debit",
    })

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
            const res = await fetch(`${API_BASE}/transactions/${accountId}?page=${page}&itemsPerPage=${pageSize}&month=${monthParam}&year=${yearParam}`, {
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

    const fetchSearchResults = useCallback(async (accountId: number, page: number = 0) => {
        setIsLoadingTransactions(true)
        try {
            const params = new URLSearchParams()
            if (searchQuery) params.append("query", searchQuery)
            if (searchStartDate) params.append("startDate", searchStartDate)
            if (searchEndDate) params.append("endDate", searchEndDate)
            params.append("page", String(page))
            params.append("itemsPerPage", String(pageSize))

            const res = await fetch(`${API_BASE}/transactions/search/${accountId}?${params.toString()}`, {
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
    }, [pageSize, searchQuery, searchStartDate, searchEndDate])

    useEffect(() => {
        fetchAccounts()
    }, [fetchAccounts])

    // Set preselected account when accounts are loaded
    useEffect(() => {
        if (preselectedAccountId && accounts.length > 0) {
            const accountExists = accounts.find(acc => acc.id === preselectedAccountId)
            if (accountExists && selectedAccountId !== preselectedAccountId) {
                setSelectedAccountId(preselectedAccountId)
            }
        }
    }, [preselectedAccountId, accounts, selectedAccountId])

    useEffect(() => {
        if (selectedAccountId) {
            setCurrentPage(0)
            if (isSearchMode) {
                fetchSearchResults(selectedAccountId, 0)
            } else {
                fetchTransactions(selectedAccountId, 0)
            }
        }
    }, [selectedAccountId, month, year, isSearchMode, fetchTransactions, fetchSearchResults])

    // Refetch when pageSize changes
    useEffect(() => {
        if (selectedAccountId) {
            setCurrentPage(0)
            if (isSearchMode) {
                fetchSearchResults(selectedAccountId, 0)
            } else {
                fetchTransactions(selectedAccountId, 0)
            }
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pageSize])

    useEffect(() => {
        if (selectedAccountId) {
            setFormData(prev => ({ ...prev, bankAccountId: selectedAccountId }))
        }
    }, [selectedAccountId])

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
        if (!droppedFile) return
        if (uploadMode === "pdf") {
            if (droppedFile.type === "application/pdf") {
                setFile(droppedFile)
                setUploadError("")
            }
        } else {
            const name = droppedFile.name.toLowerCase()
            if (name.endsWith(".ofx") || name.endsWith(".txt")) {
                setFile(droppedFile)
                setUploadError("")
            }
        }
    }

    function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
        const selectedFile = e.target.files?.[0]
        if (!selectedFile) return
        if (uploadMode === "pdf") {
            if (selectedFile.type === "application/pdf") {
                setFile(selectedFile)
                setUploadError("")
            }
        } else {
            const name = selectedFile.name.toLowerCase()
            if (name.endsWith(".ofx") || name.endsWith(".txt")) {
                setFile(selectedFile)
                setUploadError("")
            }
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

            const url = uploadMode === "ofx"
                ? `${API_BASE}/transactions/ofx/${selectedAccountId}?updateAccount=${uploadUpdateAccount}`
                : `${API_BASE}/transactions/itau/${selectedAccountId}?updateAccount=${uploadUpdateAccount}`

            const res = await fetch(url, {
                method: "POST",
                body: formData,
                credentials: "include"
            })

            if (res.status === 202 || res.ok) {
                setUploadSuccess(uploadMode === "ofx" ? t.uploadOfxSuccess : t.uploadSuccess)
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

    async function handleCreateTransaction() {
        setCreateError("")

        if (!formData.description.trim()) {
            setCreateError("Description is required")
            return
        }
        if (!formData.amount || Number(formData.amount) === 0) {
            setCreateError("Amount is required")
            return
        }
        if (!formData.bankAccountId) {
            setCreateError("Bank account is required")
            return
        }

        setIsCreating(true)
        try {
            const res = await fetch(`${API_BASE}/transactions`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    transactionDate: formData.transactionDate,
                    description: formData.description,
                    amount: Number(formData.amount),
                    type: formData.type,
                    bankAccountId: formData.bankAccountId,
                    updateAccount: formData.updateAccount
                }),
                credentials: "include"
            })

            if (res.ok) {
                setShowCreateDialog(false)
                setFormData({
                    transactionDate: new Date().toISOString().split('T')[0],
                    description: "",
                    amount: "",
                    type: "debit",
                    bankAccountId: selectedAccountId || 0,
                    updateAccount: true
                })
                // Refresh transactions
                if (selectedAccountId) {
                    fetchTransactions(selectedAccountId, currentPage)
                }
            } else {
                setCreateError("Failed to create transaction")
            }
        } catch {
            setCreateError("Failed to create transaction")
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

    async function handleGenerateReport() {
        if (!selectedAccountId) return
        setIsGeneratingReport(true)
        try {
            const res = await fetch(
                `${API_BASE}/transactions/report/${selectedAccountId}?month=${month}&year=${year}&lang=${locale}`,
                { credentials: "include" }
            )
            if (res.ok) {
                const blob = await res.blob()
                const url = URL.createObjectURL(blob)
                const a = document.createElement("a")
                a.href = url
                a.download = `transactions-${year}-${String(month).padStart(2, "0")}.pdf`
                a.click()
                URL.revokeObjectURL(url)
            }
        } catch {
            // silently fail
        } finally {
            setIsGeneratingReport(false)
        }
    }

    function openDeleteDialog(tx: Transaction) {
        setDeleteError("")
        setTransactionToDelete(tx)
        setShowDeleteDialog(true)
    }

    function openTransformDialog(tx: Transaction) {
        setTransformError("")
        setTransactionToTransform(tx)
        // Set default to first account that's different from transaction's account
        const defaultDestination = accounts.find(acc => acc.id !== tx.bankAccountId)
        setDestinationAccountId(defaultDestination?.id || null)
        setShowTransformDialog(true)
    }

    async function handleTransformToTransfer() {
        if (!transactionToTransform || !destinationAccountId) return

        if (destinationAccountId === transactionToTransform.bankAccountId) {
            setTransformError(t.cannotTransformSameAccount)
            return
        }

        setTransformError("")
        setIsTransforming(true)
        try {
            const res = await fetch(`${API_BASE}/transactions/${transactionToTransform.id}/transform-to-transfer`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    destinationAccountId: destinationAccountId
                }),
                credentials: "include"
            })

            if (res.ok) {
                setShowTransformDialog(false)
                setTransactionToTransform(null)
                setDestinationAccountId(null)
                if (selectedAccountId) {
                    if (isSearchMode) {
                        fetchSearchResults(selectedAccountId, currentPage)
                    } else {
                        fetchTransactions(selectedAccountId, currentPage)
                    }
                }
            } else {
                setTransformError(t.transformError)
            }
        } catch {
            setTransformError(t.transformError)
        } finally {
            setIsTransforming(false)
        }
    }

    async function handleDeleteTransaction() {
        if (!transactionToDelete) return

        setDeleteError("")
        setIsDeleting(true)
        try {
            const res = await fetch(`${API_BASE}/transactions/${transactionToDelete.id}`, {
                method: "DELETE",
                credentials: "include"
            })

            if (res.ok) {
                setShowDeleteDialog(false)
                setTransactionToDelete(null)
                if (selectedAccountId) {
                    const nextPage = transactions.length === 1 && currentPage > 0
                        ? currentPage - 1
                        : currentPage
                    if (isSearchMode) {
                        fetchSearchResults(selectedAccountId, nextPage)
                    } else {
                        fetchTransactions(selectedAccountId, nextPage)
                    }
                }
            } else {
                setDeleteError("Failed to delete transaction")
            }
        } catch {
            setDeleteError("Failed to delete transaction")
        } finally {
            setIsDeleting(false)
        }
    }

    async function handleSearch(query: string, startDate: string, endDate: string) {
        if (!selectedAccountId) return

        if (!query && !startDate && !endDate) {
            setSearchQuery("")
            setSearchStartDate("")
            setSearchEndDate("")
            setIsSearching(false)
            setCurrentPage(0)
            fetchTransactions(selectedAccountId, 0)
            return
        }

        setSearchQuery(query)
        setSearchStartDate(startDate)
        setSearchEndDate(endDate)
        setIsSearching(true)
        setCurrentPage(0)

        try {
            const params = new URLSearchParams()
            if (query) params.append("query", query)
            if (startDate) params.append("startDate", startDate)
            if (endDate) params.append("endDate", endDate)
            params.append("page", "0")
            params.append("itemsPerPage", String(pageSize))

            const res = await fetch(`${API_BASE}/transactions/search/${selectedAccountId}?${params.toString()}`, {
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
            setIsSearching(false)
        }
    }

    function openTagDialog(tx: Transaction) {
        setTagError("")
        setTransactionToTag(tx)
        setEditingTags(tx.tags ?? [])
        setTagInput("")
        setShowTagDialog(true)
    }

    function openEditDialog(tx: Transaction) {
        setEditError("")
        setTransactionToEdit(tx)
        setEditFormData({
            transactionDate: tx.transactionDate,
            description: tx.description,
            amount: String(Math.abs(tx.amount)),
            type: tx.type,
        })
        setShowEditDialog(true)
    }

    async function handleEditTransaction() {
        if (!transactionToEdit) return
        setEditError("")

        if (!editFormData.description.trim()) {
            setEditError("Description is required")
            return
        }
        if (!editFormData.amount || Number(editFormData.amount) === 0) {
            setEditError("Amount is required")
            return
        }

        setIsEditing(true)
        try {
            const res = await fetch(`${API_BASE}/transactions/${transactionToEdit.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({
                    transactionDate: editFormData.transactionDate,
                    description: editFormData.description,
                    amount: Number(editFormData.amount),
                    type: editFormData.type,
                }),
            })

            if (res.ok) {
                setShowEditDialog(false)
                setTransactionToEdit(null)
                if (selectedAccountId) {
                    if (isSearchMode) {
                        fetchSearchResults(selectedAccountId, currentPage)
                    } else {
                        fetchTransactions(selectedAccountId, currentPage)
                    }
                }
            } else {
                setEditError(t.editTransactionError)
            }
        } catch {
            setEditError(t.editTransactionError)
        } finally {
            setIsEditing(false)
        }
    }

    function handleTagInputKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
        if (e.key === "Enter" || e.key === ",") {
            e.preventDefault()
            addTagFromInput()
        }
    }

    function addTagFromInput() {
        const trimmed = tagInput.trim().toLowerCase()
        if (trimmed && !editingTags.includes(trimmed)) {
            setEditingTags(prev => [...prev, trimmed])
        }
        setTagInput("")
    }

    function removeEditingTag(tag: string) {
        setEditingTags(prev => prev.filter(t => t !== tag))
    }

    async function handleSaveTags() {
        if (!transactionToTag) return
        setTagError("")
        setIsSavingTags(true)
        try {
            const res = await fetch(`${API_BASE}/transactions/${transactionToTag.id}/tags`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ tags: editingTags })
            })
            if (res.ok) {
                setShowTagDialog(false)
                setTransactionToTag(null)
                // Update tags in local state
                setTransactions(prev => prev.map(tx =>
                    tx.id === transactionToTag.id ? { ...tx, tags: editingTags } : tx
                ))
                // Refresh all-tags list
                fetchAllTags()
            } else {
                setTagError("Failed to save tags")
            }
        } catch {
            setTagError("Failed to save tags")
        } finally {
            setIsSavingTags(false)
        }
    }

    const fetchAllTags = useCallback(async () => {
        try {
            const res = await fetch(`${API_BASE}/transactions/tags`, { credentials: "include" })
            if (res.ok) {
                const data: string[] = await res.json()
                setAllTags(data)
            }
        } catch {
            // silently fail
        }
    }, [])

    useEffect(() => {
        fetchAllTags()
    }, [fetchAllTags])

    function formatTxCurrency(value: number, account?: BankAccount | null) {
        return formatCurrency(value, account?.currency || "BRL")
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

                {/* Page heading + month/year filters + action buttons */}
                <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                    <div className="flex flex-col gap-3">
                        <div>
                            <h1 className="text-2xl font-bold text-foreground tracking-tight">
                                {t.transactions}
                            </h1>
                            <p className="mt-1 text-muted-foreground">{t.transactionsDescription}</p>
                        </div>

                        {/* Month/Year filters */}
                        {selectedAccountId && (
                            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:gap-3">

                                <div className="flex flex-col gap-1.5">
                                    <Label htmlFor="month-select" className="text-xs text-muted-foreground">{t.month || 'Month'}</Label>
                                    <select
                                        id="month-select"
                                        value={month}
                                        onChange={(e) => setMonth(Number(e.target.value))}
                                        className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                                    >
                                        {t.months.map((name, i) => (
                                            <option key={i + 1} value={i + 1}>{name}</option>
                                        ))}
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

                    <div className="flex gap-2 self-start">
                        <Button
                            onClick={() => setShowCreateDialog(true)}
                            disabled={!selectedAccountId}
                            className="gap-2"
                        >
                            <Plus className="size-4" />
                            {t.createTransaction || 'Create Transaction'}
                        </Button>
                        <Button
                            onClick={() => {
                                setShowUpload(!showUpload)
                                setUploadError("")
                                setUploadSuccess("")
                                setFile(null)
                                if (fileInputRef.current) fileInputRef.current.value = ""
                            }}
                            variant="outline"
                            className="gap-2"
                        >
                            <Upload className="size-4" />
                            {t.uploadPdf}
                        </Button>
                        <Button
                            onClick={handleGenerateReport}
                            variant="outline"
                            disabled={!selectedAccountId || isGeneratingReport}
                            className="gap-2"
                        >
                            {isGeneratingReport ? (
                                <Loader2 className="size-4 animate-spin" />
                            ) : (
                                <FileDown className="size-4" />
                            )}
                            {isGeneratingReport ? t.generatingReport : t.generateReport}
                        </Button>
                    </div>
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
                            <CardTitle className="text-lg">
                                {uploadMode === "ofx" ? t.uploadOfx : t.uploadPdf}
                            </CardTitle>
                            <CardDescription>
                                {uploadMode === "ofx" ? t.uploadOfxDescription : t.uploadPdfDescription}
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="flex flex-col gap-4">
                            {/* Mode toggle */}
                            <div className="flex flex-col gap-1.5">
                                <Label className="text-xs text-muted-foreground">{t.uploadMode}</Label>
                                <div className="flex gap-2">
                                    <Button
                                        type="button"
                                        size="sm"
                                        variant={uploadMode === "pdf" ? "default" : "outline"}
                                        onClick={() => {
                                            setUploadMode("pdf")
                                            setFile(null)
                                            setUploadError("")
                                            if (fileInputRef.current) fileInputRef.current.value = ""
                                        }}
                                    >
                                        {t.uploadModePdf}
                                    </Button>
                                    <Button
                                        type="button"
                                        size="sm"
                                        variant={uploadMode === "ofx" ? "default" : "outline"}
                                        onClick={() => {
                                            setUploadMode("ofx")
                                            setFile(null)
                                            setUploadError("")
                                            if (fileInputRef.current) fileInputRef.current.value = ""
                                        }}
                                    >
                                        {t.uploadModeOfx}
                                    </Button>
                                </div>
                            </div>

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
                                    <p className="mt-2 text-xs text-muted-foreground">
                                        {uploadMode === "ofx" ? t.ofxOnly : t.pdfOnly}
                                    </p>
                                </div>
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept={uploadMode === "ofx" ? ".ofx,.txt" : "application/pdf"}
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

                            <div className="flex items-center gap-2">
                                <Checkbox
                                    id="update-account-upload"
                                    checked={uploadUpdateAccount}
                                    onCheckedChange={(value) => setUploadUpdateAccount(value === true)}
                                />
                                <Label htmlFor="update-account-upload">
                                    {t.updateAccount || 'Update account balance'}
                                </Label>
                            </div>

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
                                    {isUploading ? t.uploading : (uploadMode === "ofx" ? t.uploadOfx : t.uploadPdf)}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                )}

                {/* Search */}
                {selectedAccountId && (
                    <TransactionSearch
                        key={`${year}-${month}`}
                        onSearch={handleSearch}
                        isLoading={isSearching || isLoadingTransactions}
                        defaultStartDate={new Date(year, month - 1, 1).toISOString().split('T')[0]}
                        defaultEndDate={new Date(year, month, 0).toISOString().split('T')[0]}
                    />
                )}

                {/* Tag filter bar */}
                {allTags.length > 0 && (
                    <div className="mb-4 flex flex-wrap items-center gap-2">
                        <span className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                            <Tags className="size-3.5" />
                            {t.filterByTag}:
                        </span>
                        {allTags.map(tag => (
                            <button
                                key={tag}
                                onClick={() => setActiveTagFilter(activeTagFilter === tag ? null : tag)}
                                className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium transition-colors border ${
                                    activeTagFilter === tag
                                        ? "bg-primary text-primary-foreground border-primary"
                                        : "bg-muted text-muted-foreground border-border hover:border-primary/50 hover:text-foreground"
                                }`}
                            >
                                <Tag className="size-3" />
                                {tag}
                            </button>
                        ))}
                        {activeTagFilter && (
                            <button
                                onClick={() => setActiveTagFilter(null)}
                                className="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors"
                            >
                                <X className="size-3" />
                                {t.clearTagFilter}
                            </button>
                        )}
                    </div>
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
                            <h3 className="text-lg font-semibold text-foreground">
                                {isSearchMode ? (t.noSearchResults || "No transactions found") : t.noTransactions}
                            </h3>
                            <p className="mt-1 text-sm text-muted-foreground">
                                {isSearchMode
                                    ? (t.noSearchResultsDescription || "Try adjusting your search terms or date range")
                                    : t.noTransactionsDescription}
                            </p>
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
                            <div className="hidden sm:grid sm:grid-cols-[1fr_2fr_1fr_auto] gap-4 border-b pb-3 mb-2">
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{t.transactionDate}</p>
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{t.transactionDescription}</p>
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider text-right">{t.transactionAmount}</p>
                                <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider text-right">{t.actions || 'Actions'}</span>
                            </div>

                            <div className="flex flex-col gap-1">
                                {transactions
                                    .filter(tx => !activeTagFilter || (tx.tags ?? []).includes(activeTagFilter))
                                    .map((tx) => (
                                    <div
                                        key={tx.id}
                                        className="flex flex-col gap-2 rounded-lg border p-4 sm:border-0 sm:p-3 sm:hover:bg-muted/50 sm:transition-colors"
                                    >
                                        <div className="sm:grid sm:grid-cols-[1fr_2fr_1fr_auto] sm:items-center sm:gap-4">
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
                                            <div className="flex items-center justify-between sm:justify-end gap-3 mt-2 sm:mt-0">
                                                <p className={`text-sm font-semibold ${
                                                    tx.amount >= 0 ? "text-primary" : "text-destructive"
                                                }`}>
                                                    {tx.amount >= 0 ? "+" : ""}{formatTxCurrency(tx.amount, accounts.find(a => a.id === selectedAccountId))}
                                                </p>
                                                <div className="flex gap-1">
                                                    <Button
                                                        variant="ghost"
                                                        size="icon-sm"
                                                        onClick={() => openEditDialog(tx)}
                                                        aria-label={t.editTransaction}
                                                        className="text-muted-foreground hover:text-primary"
                                                    >
                                                        <Pencil className="size-4" />
                                                    </Button>
                                                    <Button
                                                        variant="ghost"
                                                        size="icon-sm"
                                                        onClick={() => openTagDialog(tx)}
                                                        aria-label={t.editTags}
                                                        className="text-muted-foreground hover:text-primary"
                                                    >
                                                        <Tags className="size-4" />
                                                    </Button>
                                                    {(tx.type === "debit" || tx.type === "credit") && accounts.length > 1 && (
                                                        <Button
                                                            variant="ghost"
                                                            size="icon-sm"
                                                            onClick={() => openTransformDialog(tx)}
                                                            aria-label={t.transformToTransfer || "Transform to transfer"}
                                                            className="text-muted-foreground hover:text-primary"
                                                        >
                                                            <ArrowLeftRight className="size-4" />
                                                        </Button>
                                                    )}
                                                    <Button
                                                        variant="ghost"
                                                        size="icon-sm"
                                                        onClick={() => openDeleteDialog(tx)}
                                                        aria-label={t.deleteTransaction || "Delete transaction"}
                                                        className="text-muted-foreground hover:text-destructive"
                                                    >
                                                        <Trash2 className="size-4" />
                                                    </Button>
                                                </div>
                                            </div>
                                        </div>
                                        {/* Tag badges */}
                                        {(tx.tags ?? []).length > 0 && (
                                            <div className="flex flex-wrap gap-1 pl-0 sm:pl-9">
                                                {(tx.tags ?? []).map(tag => (
                                                    <span
                                                        key={tag}
                                                        onClick={() => setActiveTagFilter(activeTagFilter === tag ? null : tag)}
                                                        className={`inline-flex cursor-pointer items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium transition-colors border ${
                                                            activeTagFilter === tag
                                                                ? "bg-primary text-primary-foreground border-primary"
                                                                : "bg-muted text-muted-foreground border-border hover:border-primary/50 hover:text-foreground"
                                                        }`}
                                                    >
                                                        <Tag className="size-2.5" />
                                                        {tag}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>

                            {/* Pagination */}
                            {totalPages > 1 && (
                                <div className="mt-6 flex flex-wrap items-center justify-between gap-4 border-t pt-4">
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
                                    <p className="text-sm text-muted-foreground">
                                        {t.page} {currentPage + 1} {t.of} {totalPages} ({totalElements} total)
                                    </p>
                                    <Pagination>
                                        <PaginationContent>
                                            <PaginationItem>
                                                <PaginationPrevious
                                                    onClick={() => {
                                                        if (currentPage > 0 && selectedAccountId) {
                                                            if (isSearchMode) {
                                                                fetchSearchResults(selectedAccountId, currentPage - 1)
                                                            } else {
                                                                fetchTransactions(selectedAccountId, currentPage - 1)
                                                            }
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
                                                            onClick={() => selectedAccountId && (isSearchMode
                                                                ? fetchSearchResults(selectedAccountId, 0)
                                                                : fetchTransactions(selectedAccountId, 0))}
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
                                                        onClick={() => selectedAccountId && (isSearchMode
                                                            ? fetchSearchResults(selectedAccountId, currentPage - 1)
                                                            : fetchTransactions(selectedAccountId, currentPage - 1))}
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
                                                        onClick={() => selectedAccountId && (isSearchMode
                                                            ? fetchSearchResults(selectedAccountId, currentPage + 1)
                                                            : fetchTransactions(selectedAccountId, currentPage + 1))}
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
                                                            onClick={() => selectedAccountId && (isSearchMode
                                                                ? fetchSearchResults(selectedAccountId, totalPages - 1)
                                                                : fetchTransactions(selectedAccountId, totalPages - 1))}
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
                                                            if (isSearchMode) {
                                                                fetchSearchResults(selectedAccountId, currentPage + 1)
                                                            } else {
                                                                fetchTransactions(selectedAccountId, currentPage + 1)
                                                            }
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

            {/* Create Transaction Dialog */}
            <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{t.createTransaction || 'Create Transaction'}</DialogTitle>
                        <DialogDescription>
                            {t.createTransactionDescription || 'Add a new transaction manually'}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="flex flex-col gap-4 py-4">
                        <div className="flex flex-col gap-2">
                            <Label htmlFor="transaction-date">{t.transactionDate || 'Date'}</Label>
                            <Input
                                id="transaction-date"
                                type="date"
                                value={formData.transactionDate}
                                onChange={(e) => setFormData({ ...formData, transactionDate: e.target.value })}
                            />
                        </div>

                        <div className="flex flex-col gap-2">
                            <Label htmlFor="description">{t.transactionDescription || 'Description'}</Label>
                            <Input
                                id="description"
                                placeholder={t.transactionDescriptionPlaceholder || 'Enter description...'}
                                value={formData.description}
                                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                            />
                        </div>

                        <div className="flex flex-col gap-2">
                            <Label htmlFor="amount">{t.transactionAmount || 'Amount'}</Label>
                            <Input
                                id="amount"
                                type="number"
                                step="0.01"
                                placeholder="0.00"
                                value={formData.amount}
                                onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                            />
                        </div>

                        <div className="flex flex-col gap-2">
                            <Label htmlFor="type">{t.transactionType || 'Type'}</Label>
                            <select
                                id="type"
                                value={formData.type}
                                onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                                className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                            >
                                <option value="debit">{t.debit || 'Debit'}</option>
                                <option value="credit">{t.credit || 'Credit'}</option>
                            </select>
                        </div>

                        <div className="flex flex-col gap-2">
                            <Label htmlFor="account">{t.bankAccount || 'Bank Account'}</Label>
                            <select
                                id="account"
                                value={formData.bankAccountId}
                                onChange={(e) => setFormData({ ...formData, bankAccountId: Number(e.target.value) })}
                                className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                            >
                                {accounts.map((acc) => (
                                    <option key={acc.id} value={acc.id}>
                                        {acc.name}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="flex items-center gap-2">
                            <Checkbox
                                id="update-account"
                                checked={formData.updateAccount}
                                onCheckedChange={(value) => setFormData({
                                    ...formData,
                                    updateAccount: value === true
                                })}
                            />
                            <Label htmlFor="update-account">
                                {t.updateAccount || 'Update account balance'}
                            </Label>
                        </div>

                        {createError && (
                            <p className="text-sm text-destructive">{createError}</p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setShowCreateDialog(false)}
                            disabled={isCreating}
                        >
                            {t.cancel || 'Cancel'}
                        </Button>
                        <Button
                            onClick={handleCreateTransaction}
                            disabled={isCreating}
                            className="gap-2"
                        >
                            {isCreating ? (
                                <Loader2 className="size-4 animate-spin" />
                            ) : (
                                <Plus className="size-4" />
                            )}
                            {isCreating ? (t.creating || 'Creating...') : (t.create || 'Create')}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Delete Confirmation Dialog */}
            <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t.deleteTransaction || 'Delete transaction'}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t.deleteTransactionDescription || 'This action cannot be undone.'}
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    {deleteError && (
                        <p className="text-sm text-destructive">{deleteError}</p>
                    )}

                    <AlertDialogFooter>
                        <AlertDialogCancel disabled={isDeleting}>
                            {t.cancel || 'Cancel'}
                        </AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDeleteTransaction}
                            className="bg-destructive text-white hover:bg-destructive/90"
                            disabled={isDeleting}
                        >
                            {isDeleting ? (t.deleting || 'Deleting...') : (t.delete || 'Delete')}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {/* Transform to Transfer Dialog */}
            <Dialog open={showTransformDialog} onOpenChange={setShowTransformDialog}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{t.transformToTransfer || 'Transform to Transfer'}</DialogTitle>
                        <DialogDescription>
                            {transactionToTransform?.type === "credit"
                                ? (t.transformToTransferDescriptionCredit || 'Convert this credit transaction into a transfer to another account')
                                : (t.transformToTransferDescription || 'Convert this debit transaction into a transfer to another account')}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="flex flex-col gap-4 py-4">
                        {transactionToTransform && (
                            <div className="rounded-lg border bg-muted/50 p-4">
                                <p className="text-sm text-muted-foreground mb-1">Current Transaction</p>
                                <p className="text-sm font-medium">{transactionToTransform.description}</p>
                                <p className={`text-sm font-semibold mt-1 ${transactionToTransform.amount >= 0 ? "text-primary" : "text-destructive"}`}>
                                    {transactionToTransform.amount >= 0 ? "+" : ""}{formatTxCurrency(transactionToTransform.amount, accounts.find(a => a.id === selectedAccountId))}
                                </p>
                            </div>
                        )}

                        <div className="flex flex-col gap-2">
                            <Label htmlFor="destination-account">{t.selectDestinationAccount || 'Destination Account'}</Label>
                            <select
                                id="destination-account"
                                value={destinationAccountId || ""}
                                onChange={(e) => setDestinationAccountId(Number(e.target.value))}
                                className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                            >
                                <option value="" disabled>Select account...</option>
                                {accounts
                                    .filter(acc => acc.id !== transactionToTransform?.bankAccountId)
                                    .map((acc) => (
                                        <option key={acc.id} value={acc.id}>
                                            {acc.name}
                                        </option>
                                    ))}
                            </select>
                        </div>

                        {transformError && (
                            <p className="text-sm text-destructive">{transformError}</p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setShowTransformDialog(false)}
                            disabled={isTransforming}
                        >
                            {t.cancel || 'Cancel'}
                        </Button>
                        <Button
                            onClick={handleTransformToTransfer}
                            disabled={isTransforming || !destinationAccountId}
                            className="gap-2"
                        >
                            {isTransforming ? (
                                <Loader2 className="size-4 animate-spin" />
                            ) : (
                                <ArrowLeftRight className="size-4" />
                            )}
                            {isTransforming ? (t.transforming || 'Transforming...') : (t.transform || 'Transform')}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Edit Transaction Dialog */}
            <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2">
                            <Pencil className="size-4" />
                            {t.editTransaction}
                        </DialogTitle>
                        <DialogDescription>
                            {t.editTransactionDescription}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="flex flex-col gap-4 py-4">
                        <div className="flex flex-col gap-2">
                            <Label htmlFor="edit-transaction-date">{t.transactionDate}</Label>
                            <Input
                                id="edit-transaction-date"
                                type="date"
                                value={editFormData.transactionDate}
                                onChange={(e) => setEditFormData({ ...editFormData, transactionDate: e.target.value })}
                            />
                        </div>

                        <div className="flex flex-col gap-2">
                            <Label htmlFor="edit-description">{t.transactionDescription}</Label>
                            <Input
                                id="edit-description"
                                placeholder={t.transactionDescriptionPlaceholder}
                                value={editFormData.description}
                                onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                            />
                        </div>

                        <div className="flex flex-col gap-2">
                            <Label htmlFor="edit-amount">{t.transactionAmount}</Label>
                            <Input
                                id="edit-amount"
                                type="number"
                                step="0.01"
                                min="0"
                                placeholder="0.00"
                                value={editFormData.amount}
                                onChange={(e) => setEditFormData({ ...editFormData, amount: e.target.value })}
                            />
                        </div>

                        <div className="flex flex-col gap-2">
                            <Label htmlFor="edit-type">{t.transactionType}</Label>
                            <select
                                id="edit-type"
                                value={editFormData.type}
                                onChange={(e) => setEditFormData({ ...editFormData, type: e.target.value })}
                                className="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                            >
                                <option value="debit">{t.debit}</option>
                                <option value="credit">{t.credit}</option>
                            </select>
                        </div>

                        {editError && (
                            <p className="text-sm text-destructive">{editError}</p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setShowEditDialog(false)}
                            disabled={isEditing}
                        >
                            {t.cancel}
                        </Button>
                        <Button
                            onClick={handleEditTransaction}
                            disabled={isEditing}
                            className="gap-2"
                        >
                            {isEditing ? (
                                <Loader2 className="size-4 animate-spin" />
                            ) : (
                                <Pencil className="size-4" />
                            )}
                            {isEditing ? t.editing : t.edit}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Edit Tags Dialog */}
            <Dialog open={showTagDialog} onOpenChange={setShowTagDialog}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2">
                            <Tags className="size-4" />
                            {t.editTags}
                        </DialogTitle>
                        <DialogDescription>
                            {transactionToTag?.description}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="flex flex-col gap-4 py-4">
                        {/* Current tags */}
                        <div className="flex flex-wrap gap-1.5 min-h-[2rem]">
                            {editingTags.length === 0 ? (
                                <span className="text-sm text-muted-foreground">{t.noTags}</span>
                            ) : (
                                editingTags.map(tag => (
                                    <span
                                        key={tag}
                                        className="inline-flex items-center gap-1 rounded-full bg-primary/10 text-primary border border-primary/20 px-2.5 py-0.5 text-xs font-medium"
                                    >
                                        <Tag className="size-3" />
                                        {tag}
                                        <button
                                            onClick={() => removeEditingTag(tag)}
                                            className="ml-0.5 rounded-full hover:bg-primary/20 p-0.5 transition-colors"
                                            aria-label={`Remove ${tag}`}
                                        >
                                            <X className="size-2.5" />
                                        </button>
                                    </span>
                                ))
                            )}
                        </div>

                        {/* Tag input */}
                        <div className="flex gap-2">
                            <Input
                                placeholder={t.tagPlaceholder}
                                value={tagInput}
                                onChange={e => setTagInput(e.target.value)}
                                onKeyDown={handleTagInputKeyDown}
                                className="flex-1"
                            />
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={addTagFromInput}
                                disabled={!tagInput.trim()}
                                className="gap-1"
                            >
                                <Plus className="size-3.5" />
                                {t.addTag}
                            </Button>
                        </div>

                        {/* Suggestions from existing tags */}
                        {allTags.filter(t => !editingTags.includes(t)).length > 0 && (
                            <div className="flex flex-wrap gap-1.5">
                                <span className="text-xs text-muted-foreground w-full">{t.tags}:</span>
                                {allTags
                                    .filter(tag => !editingTags.includes(tag))
                                    .map(tag => (
                                        <button
                                            key={tag}
                                            type="button"
                                            onClick={() => setEditingTags(prev => [...prev, tag])}
                                            className="inline-flex items-center gap-1 rounded-full border border-dashed border-border px-2.5 py-0.5 text-xs text-muted-foreground hover:border-primary/50 hover:text-foreground transition-colors"
                                        >
                                            <Plus className="size-2.5" />
                                            {tag}
                                        </button>
                                    ))
                                }
                            </div>
                        )}

                        {tagError && (
                            <p className="text-sm text-destructive">{tagError}</p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setShowTagDialog(false)}
                            disabled={isSavingTags}
                        >
                            {t.cancel}
                        </Button>
                        <Button
                            onClick={handleSaveTags}
                            disabled={isSavingTags}
                            className="gap-2"
                        >
                            {isSavingTags ? (
                                <Loader2 className="size-4 animate-spin" />
                            ) : (
                                <Tags className="size-4" />
                            )}
                            {isSavingTags ? t.savingTags : t.saveTags}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    )
}
