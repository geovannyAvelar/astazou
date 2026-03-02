"use client"

import { useState, useCallback } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import { Search, X, Loader2 } from "lucide-react"
import { useI18n } from "@/lib/i18n/i18n-context"

interface TransactionSearchProps {
    onSearch: (query: string, startDate: string, endDate: string) => void
    isLoading: boolean
    defaultStartDate?: string
    defaultEndDate?: string
}

export function TransactionSearch({ onSearch, isLoading, defaultStartDate, defaultEndDate }: TransactionSearchProps) {
    const { t } = useI18n()

    const getDefaultDates = useCallback(() => {
        if (defaultStartDate && defaultEndDate) {
            return { startDateStr: defaultStartDate, endDateStr: defaultEndDate }
        }
        const today = new Date()
        const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1)
        const lastDayOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0)

        const endDateStr = lastDayOfMonth.toISOString().split('T')[0]
        const startDateStr = firstDayOfMonth.toISOString().split('T')[0]

        return { startDateStr, endDateStr }
    }, [defaultStartDate, defaultEndDate])

    const [query, setQuery] = useState("")
    const [startDate, setStartDate] = useState(() => getDefaultDates().startDateStr)
    const [endDate, setEndDate] = useState(() => getDefaultDates().endDateStr)

    function handleSearch(e?: React.FormEvent) {
        e?.preventDefault()
        onSearch(query, startDate, endDate)
    }

    function handleClear() {
        const defaults = getDefaultDates()
        setQuery("")
        setStartDate(defaults.startDateStr)
        setEndDate(defaults.endDateStr)
        onSearch("", "", "")
    }

    return (
        <Card className="mb-6">
            <CardHeader>
                <CardTitle className="flex items-center gap-2">
                    <Search className="size-5" />
                    {t.searchTransactions || "Search Transactions"}
                </CardTitle>
                <CardDescription>{t.search || "Search"} by description and date range</CardDescription>
            </CardHeader>
            <CardContent>
                <form onSubmit={handleSearch} className="space-y-4">
                    <div>
                        <Label htmlFor="search-query">{t.searchPlaceholder || "Search by description..."}</Label>
                        <div className="flex gap-2 mt-2">
                            <Input
                                id="search-query"
                                placeholder={t.searchPlaceholder || "Search by description..."}
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                                disabled={isLoading}
                            />
                            <Button
                                type="submit"
                                disabled={isLoading}
                                className="gap-2"
                            >
                                {isLoading ? (
                                    <>
                                        <Loader2 className="size-4 animate-spin" />
                                        {t.loading || "Loading..."}
                                    </>
                                ) : (
                                    <>
                                        <Search className="size-4" />
                                        {t.search || "Search"}
                                    </>
                                )}
                            </Button>
                            <Button
                                type="button"
                                variant="outline"
                                onClick={handleClear}
                                disabled={isLoading}
                                className="gap-2"
                            >
                                <X className="size-4" />
                                {t.cancel || "Clear"}
                            </Button>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <Label htmlFor="start-date">{t.from || "From"}</Label>
                            <Input
                                id="start-date"
                                type="date"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                disabled={isLoading}
                                className="mt-2"
                            />
                        </div>
                        <div>
                            <Label htmlFor="end-date">{t.to || "To"}</Label>
                            <Input
                                id="end-date"
                                type="date"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                disabled={isLoading}
                                className="mt-2"
                            />
                        </div>
                    </div>
                </form>
            </CardContent>
        </Card>
    )
}
