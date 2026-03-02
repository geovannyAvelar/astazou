"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { LoginForm } from "@/components/login-form"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import Image from "next/image"
import { Loader2, Shield, TrendingUp, Wallet } from "lucide-react"

export default function LoginPage() {
  const { isAuthenticated, isLoading } = useAuth()
  const { t } = useI18n()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.replace("/dashboard")
    }
  }, [isLoading, isAuthenticated, router])

  if (isLoading) {
    return (
      <main className="flex min-h-svh items-center justify-center bg-background">
        <Loader2 className="size-8 animate-spin text-primary" />
      </main>
    )
  }

  if (isAuthenticated) {
    return null
  }

  return (
    <main className="flex min-h-svh">
      {/* Left panel - Branding */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-between bg-primary p-12 text-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Image src="/logo.png" alt="Astazou logo" width={60} height={60} className="object-contain" />
            <span className="text-2xl font-bold text-white tracking-tight">Astazou</span>
          </div>
        </div>

        <div className="flex flex-col gap-8">
          <h2 className="text-4xl font-bold text-white leading-tight text-balance">
            {t.brandTagline}
          </h2>
          <p className="text-lg text-white/80 leading-relaxed max-w-md">
            {t.brandDescription}
          </p>

          <div className="flex flex-col gap-4">
            <FeatureItem
              icon={<Wallet className="size-5" />}
              text={t.featureBalanceTracking}
            />
            <FeatureItem
              icon={<TrendingUp className="size-5" />}
              text={t.featureSpendingAnalytics}
            />
            <FeatureItem
              icon={<Shield className="size-5" />}
              text={t.featureSecurity}
            />
          </div>
        </div>

        <p className="text-sm text-white/60">
          {t.trustedBy}
        </p>
      </div>

      {/* Right panel - Login form */}
      <div className="flex w-full flex-col lg:w-1/2">
        <div className="flex justify-end gap-2 p-4">
          <ThemeToggle variant="ghost" />
          <LanguageSwitcher variant="ghost" />
        </div>

        <div className="flex flex-1 items-center justify-center px-6 pb-12">
          <div className="w-full max-w-md">
            {/* Mobile branding */}
            <div className="mb-8 flex flex-col items-center gap-3 lg:hidden">
              <Image src="/logo.png" alt="Astazou logo" width={72} height={72} className="object-contain" />
              <span className="text-2xl font-bold text-foreground tracking-tight">Astazou</span>
            </div>

            <LoginForm />

            <p className="mt-6 text-center text-xs text-muted-foreground">
              {t.loginFooterSecurity}
            </p>
          </div>
        </div>
      </div>
    </main>
  )
}

function FeatureItem({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="flex items-center gap-3 text-white/90">
      <div className="flex size-9 items-center justify-center rounded-lg bg-white/15">
        {icon}
      </div>
      <span className="text-sm font-medium">{text}</span>
    </div>
  )
}
