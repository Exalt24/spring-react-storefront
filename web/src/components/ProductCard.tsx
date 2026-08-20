import { formatMoney } from '../lib/money'
import type { ProductSummary } from '../lib/api'

type Props = {
  product: ProductSummary
  onAdd: (sku: string) => void
  pending: boolean
}

/**
 * Layout and content model taken from a real storefront grid (Keychron, captured
 * 2026-08-20) plus Baymard's two product-listing principles:
 *
 *  1. The same attributes appear on EVERY tile. The category and availability
 *     lines always render, and say so when a value is absent, rather than
 *     disappearing and leaving neighbouring tiles uncomparable.
 *  2. Each element is visually distinct by weight, size or colour, so the tile
 *     can be scanned rather than read.
 *
 * Renders as an <li> because the reference grid does, and because a list of
 * products is a list. No ARIA roles are invented; the reference tile carries
 * none either, and the only interactive element is a real button.
 */
/**
 * A stable hue per SKU. The seed data ships no real photography, and six
 * identical empty boxes read as six broken images rather than as a catalogue,
 * which is exactly how the first 1280px screenshot looked. A deterministic
 * gradient keyed off the SKU makes the grid look deliberate while claiming
 * nothing about what the product looks like.
 */
function hueFor(sku: string): number {
  let hash = 0
  for (let i = 0; i < sku.length; i += 1) {
    hash = (hash * 31 + sku.charCodeAt(i)) % 360
  }
  return hash
}

export function ProductCard({ product, onAdd, pending }: Props) {
  const soldOut = !product.inStock
  const hue = hueFor(product.sku)

  return (
    <li className="flex flex-col overflow-hidden rounded-xl border border-slate-800 bg-slate-900/60 transition hover:border-slate-600">
      <div
        className="relative aspect-4/3 border-b border-slate-800"
        style={{
          backgroundImage: `linear-gradient(135deg, oklch(0.42 0.09 ${hue}), oklch(0.24 0.05 ${(hue + 40) % 360}))`,
        }}
      >
        <span className="absolute inset-0 grid place-items-center font-mono text-xs tracking-[0.2em] text-white/70">
          {product.sku}
        </span>
        {soldOut && (
          <span className="absolute top-2 right-2 rounded-full bg-slate-950/90 px-2 py-1 text-[11px] font-medium text-amber-300">
            Sold out
          </span>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-3 p-4">
        {/* Name first and heaviest, matching the reference order. */}
        <h3 className="text-sm leading-snug font-medium text-slate-100">{product.name}</h3>

        {/* Always rendered, on every tile, per Baymard principle 1. */}
        <dl className="flex flex-wrap gap-x-4 gap-y-1 text-[11px] text-slate-400">
          <div className="flex gap-1">
            <dt className="text-slate-500">Category</dt>
            <dd className="font-medium tracking-wide text-slate-300 uppercase">
              {product.categorySlug}
            </dd>
          </div>
          <div className="flex gap-1">
            <dt className="text-slate-500">Stock</dt>
            <dd className={soldOut ? 'font-medium text-amber-300' : 'font-medium text-emerald-300'}>
              {soldOut ? 'None' : 'Available'}
            </dd>
          </div>
        </dl>

        {/* Price bottom-left, action bottom-right on one row, as the reference does. */}
        <div className="mt-auto flex items-center justify-between gap-3 pt-1">
          <span className="text-lg font-semibold tracking-tight text-slate-50">
            {formatMoney(product.priceCents, product.currency)}
          </span>
          <button
            type="button"
            onClick={() => onAdd(product.sku)}
            disabled={soldOut || pending}
            aria-label={`Add ${product.name} to cart`}
            className="rounded-lg bg-sky-500 px-3 py-2 text-sm font-semibold text-slate-950 transition hover:bg-sky-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-400"
          >
            {pending ? 'Adding' : soldOut ? 'Unavailable' : 'Add to cart'}
          </button>
        </div>
      </div>
    </li>
  )
}
