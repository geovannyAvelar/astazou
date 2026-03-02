"use client"

import { useState, useEffect, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
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
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { AlertCircle, Clock, Eye, EyeOff, Loader2, Lock, User } from "lucide-react"

export function LoginForm() {
  const [clientId, setClientId] = useState("")
  const [clientSecret, setClientSecret] = useState("")
  const [showSecret, setShowSecret] = useState(false)
  const [error, setError] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const { login, logoutReason, clearLogoutReason } = useAuth()
  const { t } = useI18n()
  const router = useRouter()

  // Clear the session-expired banner once the user starts typing
  useEffect(() => {
    if (logoutReason) {
      const clear = () => clearLogoutReason()
      window.addEventListener("keydown", clear, { once: true })
      return () => window.removeEventListener("keydown", clear)
    }
  }, [logoutReason, clearLogoutReason])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError("")
    clearLogoutReason()

    if (!clientId.trim() || !clientSecret.trim()) {
      setError(t.loginErrorEmpty)
      return
    }

    setIsSubmitting(true)

    try {
      await login(clientId, clientSecret)
      router.push("/dashboard")
    } catch (err) {
      setError(err instanceof Error ? err.message : "An unexpected error occurred.")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card className="w-full max-w-md border-0 shadow-xl">
      <CardHeader className="text-center pb-2">
        <CardTitle className="text-2xl font-bold tracking-tight text-foreground">
          {t.loginTitle}
        </CardTitle>
        <CardDescription className="text-muted-foreground">
          {t.loginDescription}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          {logoutReason === "inactivity" && (
            <Alert variant="destructive">
              <Clock className="size-4" />
              <AlertTitle>{t.sessionExpiredTitle}</AlertTitle>
              <AlertDescription>{t.sessionExpiredMessage}</AlertDescription>
            </Alert>
          )}

          {error && (
            <div className="flex items-start gap-3 rounded-lg bg-destructive/10 p-3 text-sm text-destructive" role="alert">
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              <p>{error}</p>
            </div>
          )}

          <div className="flex flex-col gap-2">
            <Label htmlFor="username">{t.usernameLabel}</Label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
              <Input
                id="username"
                type="text"
                placeholder={t.usernamePlaceholder}
                value={clientId}
                onChange={(e) => setClientId(e.target.value)}
                className="pl-10 h-11"
                autoComplete="username"
                disabled={isSubmitting}
              />
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="password">{t.passwordLabel}</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
              <Input
                id="password"
                type={showSecret ? "text" : "password"}
                placeholder={t.passwordPlaceholder}
                value={clientSecret}
                onChange={(e) => setClientSecret(e.target.value)}
                className="pl-10 pr-10 h-11"
                autoComplete="current-password"
                disabled={isSubmitting}
              />
              <button
                type="button"
                onClick={() => setShowSecret(!showSecret)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                tabIndex={-1}
                aria-label={showSecret ? t.hidePassword : t.showPassword}
              >
                {showSecret ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
          </div>

          <Button
            type="submit"
            className="h-11 w-full text-base font-semibold mt-1 text-white green-dark"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                {t.loginSubmitting}
              </>
            ) : (
              t.loginSubmit
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
