"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { useI18n } from "@/lib/i18n/i18n-context"
import { ThemeToggle } from "@/components/theme-toggle"
import { LanguageSwitcher } from "@/components/language-switcher"
import Image from "next/image"
import { CheckCircle2, XCircle, Loader2, ArrowLeft, ShieldCheck, Calendar, User, CreditCard } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

interface ReportInfo {
  valid: boolean
  username: string
  accountName: string
  reportMonth: number
  reportYear: number
  generatedAt: string
}

export default function ValidateReportPage() {
  const { token } = useParams<{ token: string }>()
  const { t } = useI18n()
  const router = useRouter()

  const [status, setStatus] = useState<"loading" | "valid" | "invalid">("loading")
  const [reportInfo, setReportInfo] = useState<ReportInfo | null>(null)

  useEffect(() => {
    if (!token) return

    const controller = new AbortController()

    fetch(`${API_BASE}/reports/validate/${token}`, { signal: controller.signal })
      .then(async (res) => {
        if (!res.ok) {
          setStatus("invalid")
          return
        }
        const data: ReportInfo = await res.json()
        setReportInfo(data)
        setStatus("valid")
      })
      .catch(() => {
        setStatus("invalid")
      })

    return () => controller.abort()
  }, [token])

  const monthName = (month: number) => t.months[month - 1] ?? String(month)

  return (
    <main className="min-h-svh bg-background flex flex-col">
      {/* Top bar */}
      <header className="border-b bg-background/80 backdrop-blur-sm sticky top-0 z-10">
        <div className="max-w-2xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Image src="/logo.png" alt="Astazou logo" width={36} height={36} className="object-contain" />
            <span className="font-bold text-lg text-primary tracking-tight">Astazou</span>
          </div>
          <div className="flex items-center gap-2">
            <LanguageSwitcher />
            <ThemeToggle />
          </div>
        </div>
      </header>

      {/* Content */}
      <div className="flex-1 flex flex-col items-center justify-center px-4 py-12">
        <div className="w-full max-w-lg space-y-6">

          {/* Page heading */}
          <div className="text-center space-y-1">
            <div className="flex justify-center mb-3">
              <ShieldCheck className="size-10 text-primary" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight">{t.validateReportTitle}</h1>
            <p className="text-muted-foreground text-sm">{t.validateReportDescription}</p>
          </div>

          {/* Loading */}
          {status === "loading" && (
            <Card>
              <CardContent className="flex flex-col items-center gap-3 py-10">
                <Loader2 className="size-8 animate-spin text-primary" />
                <p className="text-muted-foreground text-sm">{t.validateReportLoading}</p>
              </CardContent>
            </Card>
          )}

          {/* Valid */}
          {status === "valid" && reportInfo && (
            <Card className="border-green-200 dark:border-green-900">
              <CardHeader className="pb-2">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="size-5 text-green-600 dark:text-green-400 shrink-0" />
                  <CardTitle className="text-green-700 dark:text-green-400 text-base">
                    {t.validateReportValid}
                  </CardTitle>
                  <Badge variant="outline" className="ml-auto border-green-400 text-green-700 dark:text-green-400">
                    Astazou
                  </Badge>
                </div>
                <CardDescription className="text-xs mt-1">
                  Token: <span className="font-mono break-all">{token}</span>
                </CardDescription>
              </CardHeader>

              <Separator />

              <CardContent className="pt-4 space-y-3">
                <div className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-3 text-sm">
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <CreditCard className="size-4 shrink-0" />
                    <span>{t.validateReportAccount}</span>
                  </div>
                  <span className="font-medium">{reportInfo.accountName}</span>

                  <div className="flex items-center gap-2 text-muted-foreground">
                    <Calendar className="size-4 shrink-0" />
                    <span>{t.validateReportPeriod}</span>
                  </div>
                  <span className="font-medium">
                    {monthName(reportInfo.reportMonth)} / {reportInfo.reportYear}
                  </span>

                  <div className="flex items-center gap-2 text-muted-foreground">
                    <User className="size-4 shrink-0" />
                    <span>{t.validateReportOwner}</span>
                  </div>
                  <span className="font-medium font-mono">{reportInfo.username}</span>

                  <div className="flex items-center gap-2 text-muted-foreground">
                    <ShieldCheck className="size-4 shrink-0" />
                    <span>{t.validateReportGeneratedAt}</span>
                  </div>
                  <span className="font-medium text-xs">
                    {new Date(reportInfo.generatedAt).toLocaleString()}
                  </span>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Invalid */}
          {status === "invalid" && (
            <Card className="border-red-200 dark:border-red-900">
              <CardHeader className="pb-2">
                <div className="flex items-center gap-2">
                  <XCircle className="size-5 text-red-600 dark:text-red-400 shrink-0" />
                  <CardTitle className="text-red-700 dark:text-red-400 text-base">
                    {t.validateReportInvalid}
                  </CardTitle>
                </div>
              </CardHeader>
              <Separator />
              <CardContent className="pt-4">
                <p className="text-sm text-muted-foreground">{t.validateReportInvalidDescription}</p>
              </CardContent>
            </Card>
          )}

          {/* Back button */}
          <div className="flex justify-center">
            <Button variant="ghost" onClick={() => router.push("/")} className="gap-2">
              <ArrowLeft className="size-4" />
              {t.validateReportBackHome}
            </Button>
          </div>
        </div>
      </div>
    </main>
  )
}

