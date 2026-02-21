"use client"

import { useI18n } from "@/lib/i18n/i18n-context"
import type { Locale } from "@/lib/i18n/translations"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Globe } from "lucide-react"

const languages: { code: Locale; label: string; flag: string }[] = [
  { code: "en", label: "English", flag: "EN" },
  { code: "pt", label: "Português", flag: "PT" },
  { code: "es", label: "Español", flag: "ES" },
]

interface LanguageSwitcherProps {
  variant?: "default" | "ghost" | "outline"
  className?: string
}

export function LanguageSwitcher({ variant = "outline", className }: LanguageSwitcherProps) {
  const { locale, setLocale } = useI18n()
  const current = languages.find((l) => l.code === locale)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={variant} size="sm" className={className}>
          <Globe className="size-4" />
          <span className="hidden sm:inline">{current?.flag}</span>
          <span className="sr-only">Switch language</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="min-w-[140px]">
        {languages.map((lang) => (
          <DropdownMenuItem
            key={lang.code}
            onClick={() => setLocale(lang.code)}
            className={locale === lang.code ? "bg-accent" : ""}
          >
            <span className="mr-2 font-mono text-xs text-muted-foreground">{lang.flag}</span>
            {lang.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
