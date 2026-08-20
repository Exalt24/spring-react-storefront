/**
 * Minor units in, formatted string out. The server is the only thing that does
 * arithmetic on money; this file only ever formats what it was given, which is
 * why it takes cents and never accepts a float.
 */
export function formatMoney(cents: number, currency = 'USD', locale = 'en-US'): string {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
  }).format(cents / 100)
}

export function formatMoneyOrFree(cents: number, currency = 'USD'): string {
  return cents === 0 ? 'Free' : formatMoney(cents, currency)
}
