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

/**
 * A category glyph, drawn rather than photographed.
 *
 * The reference pass captured Nike, Allbirds and Everlane at both widths and all
 * three put a real product photograph in this slot. There is none to put here and
 * there honestly should not be: inventing product photography for a catalogue that
 * sells nothing would be the one dishonest thing on the page.
 *
 * So the tile commits to being an illustration instead of imitating a photo. A
 * gradient with only a SKU printed on it reads as a failed image load, which is
 * what the first screenshot looked like; a centred glyph with the SKU demoted to a
 * caption reads as a deliberate placeholder system, the way dev-tool storefronts
 * handle the same problem. Stock photography was considered and rejected: it would
 * imply these are real products.
 *
 * `aria-hidden` because the name, category and stock are already text on the tile,
 * so the glyph is decoration and a screen reader announcing it adds noise.
 */
function CategoryGlyph({ slug }: { slug: string }) {
  const common = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.5,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
    // Scales with the tile. A fixed 40px glyph is proportionate in a 2-up phone
    // card and looks lost in a ~300px-tall desktop tile, which the 1280px
    // screenshot showed plainly, so it steps up with the breakpoint.
    className: 'h-10 w-10 text-white/70 sm:h-14 sm:w-14 xl:h-16 xl:w-16',
  }

  if (slug === 'audio') {
    // Headphones: a band over two ear cups.
    return (
      <svg {...common}>
        <path d="M4 14v-2a8 8 0 0 1 16 0v2" />
        <rect x="2.5" y="13.5" width="4" height="7" rx="1.6" />
        <rect x="17.5" y="13.5" width="4" height="7" rx="1.6" />
      </svg>
    )
  }

  if (slug === 'keyboards') {
    // Keyboard: a body with two key rows and a spacebar.
    return (
      <svg {...common}>
        <rect x="2" y="6.5" width="20" height="11" rx="2" />
        <path d="M6 10h.01M9.5 10h.01M13 10h.01M16.5 10h.01M8 14h8" />
      </svg>
    )
  }

  // Desk: a work surface on two legs.
  return (
    <svg {...common}>
      <path d="M3 9h18M5.5 9v10M18.5 9v10M3 9l1.5-3h15L21 9" />
    </svg>
  )
}

export function ProductCard({ product, onAdd, pending }: Props) {
  const soldOut = !product.inStock
  const hue = hueFor(product.sku)

  return (
    <li className="flex flex-col overflow-hidden rounded-xl border border-slate-800 bg-slate-900/60 transition hover:border-slate-600">
      {/* 4:3 was ~268px tall once the card filled a 390px viewport, which is why
          only one product fitted on a phone. 5:4 at two columns puts the whole
          first row plus the start of the second above the fold, and the three
          reference grids all hold ONE ratio across every tile, so this one does
          too rather than changing per breakpoint. */}
      <div
        className="relative aspect-5/4 border-b border-slate-800"
        style={{
          backgroundImage: `linear-gradient(135deg, oklch(0.42 0.09 ${hue}), oklch(0.24 0.05 ${(hue + 40) % 360}))`,
        }}
      >
        <span className="absolute inset-0 grid place-items-center gap-1.5">
          <CategoryGlyph slug={product.categorySlug} />
        </span>
        {/* SKU demoted from the centre to a caption. Centred and alone it was the
            only mark on the tile and read as a broken image. */}
        <span className="absolute bottom-1.5 left-0 right-0 text-center font-mono text-[10px] tracking-[0.18em] text-white/45">
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

        {/* Always rendered, on every tile, per Baymard principle 1.
            Label and value both moved up one step (500 to 400, 400 to 300)
            after the reference pass called out the meta row as likely under
            4.5:1 on this near-black card. */}
        <dl className="flex flex-wrap gap-x-3 gap-y-1 text-[11px] text-slate-300">
          <div className="flex gap-1">
            <dt className="text-slate-400">Category</dt>
            <dd className="font-medium tracking-wide text-slate-200 uppercase">
              {product.categorySlug}
            </dd>
          </div>
          <div className="flex gap-1">
            <dt className="text-slate-400">Stock</dt>
            <dd className={soldOut ? 'font-medium text-amber-300' : 'font-medium text-emerald-300'}>
              {soldOut ? 'None' : 'Available'}
            </dd>
          </div>
        </dl>

        {/* Price then action. Side by side on one row is what the reference tiles
            do and it is kept from 400px up, but at two columns on a 390px phone
            the card is about 175px wide and "$420.00" beside "Add to cart" does
            not fit, so below that they stack and the button goes full width,
            which is also how Allbirds renders its card CTA. */}
        <div className="mt-auto flex flex-col gap-2 pt-1 min-[400px]:flex-row min-[400px]:items-center min-[400px]:justify-between min-[400px]:gap-3">
          <span className="text-base font-semibold tracking-tight text-slate-50 sm:text-lg">
            {formatMoney(product.priceCents, product.currency)}
          </span>
          <button
            type="button"
            onClick={() => onAdd(product.sku)}
            disabled={soldOut || pending}
            aria-label={`Add ${product.name} to cart`}
            className="min-h-11 w-full rounded-lg bg-sky-500 px-3 text-sm font-semibold whitespace-nowrap text-slate-950 transition hover:bg-sky-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-400 min-[400px]:w-auto min-[400px]:py-2"
          >
            {pending ? 'Adding' : soldOut ? 'Unavailable' : 'Add to cart'}
          </button>
        </div>
      </div>
    </li>
  )
}
