export interface SupportedCurrency {
  code: string
  name: string
  symbol: string
}

export const SUPPORTED_CURRENCIES: SupportedCurrency[] = [
  { code: "ARS", name: "Argentine Peso",    symbol: "$" },
  { code: "AUD", name: "Australian Dollar", symbol: "A$" },
  { code: "BRL", name: "Brazilian Real",    symbol: "R$" },
  { code: "CAD", name: "Canadian Dollar",   symbol: "CA$" },
  { code: "CHF", name: "Swiss Franc",       symbol: "CHF" },
  { code: "CLP", name: "Chilean Peso",      symbol: "$" },
  { code: "EUR", name: "Euro",              symbol: "€" },
  { code: "GBP", name: "British Pound",     symbol: "£" },
  { code: "JPY", name: "Japanese Yen",      symbol: "¥" },
  { code: "MXN", name: "Mexican Peso",      symbol: "$" },
  { code: "PEN", name: "Peruvian Sol",      symbol: "S/" },
  { code: "USD", name: "US Dollar",         symbol: "$" },
  { code: "UYU", name: "Uruguayan Peso",    symbol: "$U" },
]

export const CURRENCY_LOCALE_MAP: Record<string, string> = {
  ARS: "es-AR",
  AUD: "en-AU",
  BRL: "pt-BR",
  CAD: "en-CA",
  CHF: "de-CH",
  CLP: "es-CL",
  EUR: "de-DE",
  GBP: "en-GB",
  JPY: "ja-JP",
  MXN: "es-MX",
  PEN: "es-PE",
  USD: "en-US",
  UYU: "es-UY",
}

export function formatCurrency(amount: number, currencyCode: string = "BRL", useAbsolute = false): string {
  const value = useAbsolute ? Math.abs(amount) : amount
  const locale = CURRENCY_LOCALE_MAP[currencyCode] ?? "en-US"
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency: currencyCode,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

