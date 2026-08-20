import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'

import { browseProducts, StorefrontError } from './lib/api'
import { useCart, useCartToken } from './hooks/useCart'
import { CartPanel } from './components/CartPanel'
import { ProductCard } from './components/ProductCard'

const CATEGORIES = [
  { slug: '', label: 'All' },
  { slug: 'audio', label: 'Audio' },
  { slug: 'desk', label: 'Desk' },
  { slug: 'keyboards', label: 'Keyboards' },
]

const SORTS = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price, low to high' },
  { value: 'price_desc', label: 'Price, high to low' },
  { value: 'name', label: 'Name' },
]

function SkeletonCard() {
  return (
    <li className="animate-pulse overflow-hidden rounded-xl border border-slate-800 bg-slate-900/60">
      <div className="aspect-4/3 bg-slate-800/60" />
      <div className="space-y-3 p-4">
        <div className="h-3 w-3/4 rounded bg-slate-800" />
        <div className="h-2 w-1/2 rounded bg-slate-800" />
        <div className="h-6 w-full rounded bg-slate-800" />
      </div>
    </li>
  )
}

export default function App() {
  const [category, setCategory] = useState('')
  const [sort, setSort] = useState('newest')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [cartError, setCartError] = useState<string | null>(null)
  const [busySku, setBusySku] = useState<string | null>(null)

  const token = useCartToken()
  const { query: cartQuery, add, update, remove } = useCart(token)

  const products = useQuery({
    queryKey: ['products', category, sort, query, page],
    queryFn: () => browseProducts({ category, sort, query, page }),
  })

  /**
   * A 409 from the server is the authoritative answer on stock, so it is shown
   * verbatim rather than translated into a guess. The catalogue is refetched
   * because whatever the server just told us means our copy is stale.
   */
  const handleFailure = (error: unknown) => {
    setCartError(error instanceof StorefrontError ? error.message : 'Something went wrong.')
    products.refetch()
  }

  const onAdd = (sku: string) => {
    setCartError(null)
    setBusySku(sku)
    add.mutate(
      { sku, quantity: 1 },
      {
        onError: handleFailure,
        onSuccess: () => products.refetch(),
        onSettled: () => setBusySku(null),
      },
    )
  }

  const onSetQuantity = (sku: string, quantity: number) => {
    setCartError(null)
    setBusySku(sku)
    update.mutate(
      { sku, quantity: Math.max(quantity, 0) },
      {
        onError: handleFailure,
        onSuccess: () => products.refetch(),
        onSettled: () => setBusySku(null),
      },
    )
  }

  const onRemove = (sku: string) => {
    setCartError(null)
    setBusySku(sku)
    remove.mutate(
      { sku },
      {
        onError: handleFailure,
        onSuccess: () => products.refetch(),
        onSettled: () => setBusySku(null),
      },
    )
  }

  const pageData = products.data

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4">
          <div>
            <h1 className="text-base font-semibold tracking-tight">Storefront</h1>
            <p className="text-xs text-slate-500">React and Spring Boot, one BFF</p>
          </div>
          <span className="rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-400">
            {pageData ? `${pageData.totalItems} products` : 'Loading'}
          </span>
        </div>
      </header>

      <main className="mx-auto grid max-w-6xl gap-6 px-4 py-6 lg:grid-cols-[1fr_20rem]">
        <section aria-labelledby="catalogue-heading">
          <h2 id="catalogue-heading" className="sr-only">
            Catalogue
          </h2>

          <div className="flex flex-wrap items-center gap-3">
            {/* Category chips, matching the reference storefront's row of
                category controls above the grid. */}
            <div className="flex flex-wrap gap-2">
              {CATEGORIES.map((c) => (
                <button
                  key={c.slug || 'all'}
                  type="button"
                  aria-pressed={category === c.slug}
                  onClick={() => {
                    setCategory(c.slug)
                    setPage(0)
                  }}
                  className={
                    category === c.slug
                      ? 'rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-950'
                      : 'rounded-full border border-slate-700 px-3 py-1.5 text-xs text-slate-300 hover:border-slate-500'
                  }
                >
                  {c.label}
                </button>
              ))}
            </div>

            <div className="ml-auto flex items-center gap-2">
              <label htmlFor="search" className="sr-only">
                Search products
              </label>
              <input
                id="search"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                  setPage(0)
                }}
                placeholder="Search"
                className="w-32 rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none sm:w-40"
              />
              {/* Labelled sort, as the reference does, rather than a bare select. */}
              <label htmlFor="sort" className="text-xs text-slate-500">
                Sort by
              </label>
              <select
                id="sort"
                value={sort}
                onChange={(e) => {
                  setSort(e.target.value)
                  setPage(0)
                }}
                className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-xs text-slate-100 focus:border-sky-500 focus:outline-none"
              >
                {SORTS.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {products.isError && (
            <p role="alert" className="mt-6 rounded-lg bg-amber-500/10 px-3 py-2 text-sm text-amber-300">
              The catalogue could not be loaded. Is the API running on port 8080?
            </p>
          )}

          <ul className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {products.isPending &&
              Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}

            {pageData?.items.map((product) => (
              <ProductCard
                key={product.sku}
                product={product}
                onAdd={onAdd}
                pending={busySku === product.sku}
              />
            ))}
          </ul>

          {pageData && pageData.items.length === 0 && (
            <p className="mt-6 text-sm text-slate-400">
              Nothing matches that. Try a different category or clear the search.
            </p>
          )}

          {pageData && pageData.totalPages > 1 && (
            <nav aria-label="Pagination" className="mt-6 flex items-center gap-3">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(p - 1, 0))}
                disabled={pageData.page === 0}
                className="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-300 disabled:opacity-40"
              >
                Previous
              </button>
              <span className="text-xs text-slate-500">
                Page {pageData.page + 1} of {pageData.totalPages}
              </span>
              <button
                type="button"
                onClick={() => setPage((p) => p + 1)}
                disabled={!pageData.hasNext}
                className="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-300 disabled:opacity-40"
              >
                Next
              </button>
            </nav>
          )}
        </section>

        <CartPanel
          cart={cartQuery.data}
          loading={cartQuery.isPending}
          onSetQuantity={onSetQuantity}
          onRemove={onRemove}
          busySku={busySku}
          error={cartError}
        />
      </main>
    </div>
  )
}
