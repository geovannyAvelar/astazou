"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { AccountsContent } from "@/components/accounts-content"
import { Loader2 } from "lucide-react"

export default function AccountsPage() {
  const { isAuthenticated, isLoading } = useAuth()
  const { t } = useI18n()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/")
    }
  }, [isLoading, isAuthenticated, router])

  if (isLoading) {
    return (
      <main className="flex min-h-svh items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="size-8 animate-spin text-primary" />
          <p className="text-sm text-muted-foreground">{t.loading}</p>
        </div>
      </main>
    )
  }

  if (!isAuthenticated) {
    return null
  }

  return <AccountsContent />
}
