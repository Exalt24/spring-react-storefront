import { formatMoney, formatMoneyOrFree } from '../lib/money'
import type { CartView } from '../lib/api'

type Props = {
  cart: CartView | undefined
  loading: boolean
  onSetQuantity: (sku: string, quantity: number) => void
  onRemove: (sku: string) => void
  busySku: string | null
  error: string | null
}

/**
 * Modelled on a real Shopify order summary, driven with an actual line item and
 * read off the screenshot (2026-08-20), plus Baymard's cart findings:
 *
 *  - EVERY summary row renders, always. The reference showed a Shipping row with
 *    "Enter shipping address" where the number would go rather than dropping the
 *    row; 23% of US shoppers abandon without an upfront total and 49% of
 *    non-browsing abandonments blame surprise shipping or tax, so a row that
 *    disappears is worse than one that admits it does not know yet.
 *  - The line total sits on the same row as the name, so a quantity change and
 *    the money it moved are visible in one glance.
 *  - Totals refresh immediately on a quantity change, because every mutation
 *    returns the whole cart and replaces the cache. 86% of sites get this wrong.
 *  - Stepping down to 0 removes the line, which is the documented affordance
 *    rather than forcing a hunt for a separate control.
 *
 * No ARIA roles are invented: the reference uses plain buttons, so these are
 * plain buttons with labels, and the live quantity is announced with aria-live.
 */
export function CartPanel({ cart, loading, onSetQuantity, onRemove, busySku, error }: Props) {
  const currency = cart?.currency ?? 'USD'

  return (
    <aside
      aria-labelledby="cart-heading"
      className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 lg:sticky lg:top-6"
    >
      <div className="flex items-baseline justify-between">
        <h2
          id="cart-heading"
          className="text-sm font-semibold tracking-wide text-slate-200 uppercase"
        >
          Your cart
        </h2>
        <span className="text-xs text-slate-500" data-testid="cart-count">
          {cart ? `${cart.totalQuantity} item${cart.totalQuantity === 1 ? '' : 's'}` : ''}
        </span>
      </div>

      {error && (
        <p role="alert" className="mt-3 rounded-lg bg-amber-500/10 px-3 py-2 text-xs text-amber-300">
          {error}
        </p>
      )}

      {loading && !cart && <p className="mt-4 text-sm text-slate-500">Loading your cart.</p>}

      {cart && cart.lines.length === 0 && (
        <p className="mt-4 text-sm text-slate-400">
          Nothing here yet. Add something from the catalogue and the totals fill in below.
        </p>
      )}

      {cart && cart.lines.length > 0 && (
        <ul className="mt-4 flex flex-col divide-y divide-slate-800">
          {cart.lines.map((line) => (
            <li key={line.sku} className="py-3">
              <div className="flex items-start justify-between gap-3">
                <p className="min-w-0 flex-1 text-sm leading-snug text-slate-100">{line.name}</p>
                <span className="text-sm whitespace-nowrap text-slate-200">
                  {formatMoney(line.lineTotalCents, currency)}
                </span>
              </div>

              <p className="mt-0.5 font-mono text-[11px] text-slate-500">
                {line.sku} at {formatMoney(line.unitPriceCents, currency)}
              </p>

              <div className="mt-2 flex items-center gap-2">
                <button
                  type="button"
                  aria-label={`Decrease quantity of ${line.name}`}
                  disabled={busySku === line.sku}
                  onClick={() => onSetQuantity(line.sku, line.quantity - 1)}
                  className="size-6 rounded border border-slate-700 text-slate-300 disabled:opacity-40"
                >
                  -
                </button>
                <span aria-live="polite" className="w-6 text-center text-sm text-slate-200">
                  {line.quantity}
                </span>
                <button
                  type="button"
                  aria-label={`Increase quantity of ${line.name}`}
                  disabled={busySku === line.sku || line.availableQty === 0}
                  onClick={() => onSetQuantity(line.sku, line.quantity + 1)}
                  className="size-6 rounded border border-slate-700 text-slate-300 disabled:opacity-40"
                >
                  +
                </button>
                <button
                  type="button"
                  aria-label={`Remove ${line.name} from cart`}
                  disabled={busySku === line.sku}
                  onClick={() => onRemove(line.sku)}
                  className="ml-2 text-xs text-slate-500 underline hover:text-slate-300 disabled:opacity-40"
                >
                  Remove
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* Always rendered once the cart exists, even when empty. Every figure comes
          straight from the server; this component does no money arithmetic, which
          is why a cart total and a checkout total cannot drift apart. */}
      {cart && (
        <dl className="mt-4 space-y-1.5 border-t border-slate-800 pt-4 text-sm">
          <div className="flex justify-between text-slate-400">
            <dt>Subtotal</dt>
            <dd data-testid="subtotal">{formatMoney(cart.subtotalCents, currency)}</dd>
          </div>
          <div className="flex justify-between text-slate-400">
            <dt>Shipping</dt>
            <dd data-testid="shipping">
              {cart.lines.length === 0
                ? 'Add an item'
                : formatMoneyOrFree(cart.shippingCents, currency)}
            </dd>
          </div>
          <div className="flex justify-between text-slate-400">
            <dt>Tax</dt>
            <dd data-testid="tax">{formatMoney(cart.taxCents, currency)}</dd>
          </div>
          <div className="flex items-baseline justify-between pt-2 text-base font-semibold text-slate-50">
            <dt>Total</dt>
            <dd data-testid="total">
              <span className="mr-1 text-[11px] font-normal text-slate-500">{currency}</span>
              {formatMoney(cart.totalCents, currency)}
            </dd>
          </div>
        </dl>
      )}
    </aside>
  )
}
