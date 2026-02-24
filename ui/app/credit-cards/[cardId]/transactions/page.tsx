"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { CreditCardTransactionsContent } from "@/components/credit-card-transactions-content"
import { Loader2 } from "lucide-react"

interface CreditCardTransactionsPageProps {
  params: Promise<{ cardId: string }>
}

export default function CreditCardTransactionsPage(props: CreditCardTransactionsPageProps) {
  const { isAuthenticated, isLoading } = useAuth()
  const { t } = useI18n()
  const router = useRouter()
  const [cardId, setCardId] = useState<number | null>(null)

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/")
    }
  }, [isLoading, isAuthenticated, router])

  useEffect(() => {
    props.params.then((params) => {
      const id = parseInt(params.cardId, 10)
      if (!isNaN(id)) {
        setCardId(id)
      }
    })
  }, [props.params])

  if (isLoading || cardId === null) {
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

  return (
    <CreditCardTransactionsContent cardId={cardId} />
  )
}

