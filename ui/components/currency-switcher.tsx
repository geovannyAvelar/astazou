"use client"

import { useCurrency } from "@/lib/currency-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { SUPPORTED_CURRENCIES } from "@/lib/currency"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

interface CurrencySwitcherProps {
  variant?: "default" | "compact"
}

export function CurrencySwitcher({ variant = "default" }: CurrencySwitcherProps) {
  const { preferredCurrency, setPreferredCurrency } = useCurrency()
  const { t } = useI18n()

  return (
    <Select value={preferredCurrency} onValueChange={setPreferredCurrency}>
      <SelectTrigger className={variant === "compact" ? "h-8 w-[90px] text-xs" : "w-[160px]"}>
        <SelectValue placeholder={t.selectCurrency} />
      </SelectTrigger>
      <SelectContent>
        {SUPPORTED_CURRENCIES.map((c) => (
          <SelectItem key={c.code} value={c.code}>
            <span className="font-mono text-xs mr-1">{c.code}</span>
            {variant !== "compact" && (
              <span className="text-muted-foreground">{c.name}</span>
            )}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

