export type ProductSummary = {
  sku: string
  name: string
  priceCents: number
  currency: string
  imageUrl: string | null
  inStock: boolean
  categorySlug: string
}

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
  hasNext: boolean
}

export type CartLine = {
  sku: string
  name: string
  imageUrl: string | null
  quantity: number
  unitPriceCents: number
  lineTotalCents: number
  availableQty: number
}

export type CartView = {
  cartToken: string
  lines: CartLine[]
  totalQuantity: number
  subtotalCents: number
  shippingCents: number
  taxCents: number
  totalCents: number
  currency: string
}

export type ApiError = {
  code: string
  message: string
  fieldErrors: Record<string, string>
  timestamp: string
}

/**
 * Every failure the server can produce arrives in one shape, so there is exactly
 * one place that turns a response into an Error and exactly one thing the UI has
 * to render. `code` is what the UI branches on, never the HTTP status.
 */
export class StorefrontError extends Error {
  readonly code: string
  readonly fieldErrors: Record<string, string>

  constructor(payload: ApiError) {
    super(payload.message)
    this.code = payload.code
    this.fieldErrors = payload.fieldErrors ?? {}
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
  })

  if (!response.ok) {
    let payload: ApiError
    try {
      payload = (await response.json()) as ApiError
    } catch {
      payload = {
        code: 'NETWORK',
        message: `Request failed with status ${response.status}.`,
        fieldErrors: {},
        timestamp: new Date().toISOString(),
      }
    }
    throw new StorefrontError(payload)
  }

  return (await response.json()) as T
}

export type BrowseParams = {
  category?: string
  query?: string
  page?: number
  sort?: string
}

export function browseProducts(params: BrowseParams) {
  const search = new URLSearchParams()
  if (params.category) search.set('category', params.category)
  if (params.query) search.set('q', params.query)
  search.set('page', String(params.page ?? 0))
  search.set('size', '12')
  search.set('sort', params.sort ?? 'newest')
  return request<PageResponse<ProductSummary>>(`/api/catalog/products?${search}`)
}

export const createCart = () => request<CartView>('/api/cart', { method: 'POST' })

export const getCart = (token: string) => request<CartView>(`/api/cart/${token}`)

export const addToCart = (token: string, sku: string, quantity: number) =>
  request<CartView>(`/api/cart/${token}/items`, {
    method: 'POST',
    body: JSON.stringify({ sku, quantity }),
  })

export const setQuantity = (token: string, sku: string, quantity: number) =>
  request<CartView>(`/api/cart/${token}/items/${sku}`, {
    method: 'PATCH',
    body: JSON.stringify({ quantity }),
  })

export const removeFromCart = (token: string, sku: string) =>
  request<CartView>(`/api/cart/${token}/items/${sku}`, { method: 'DELETE' })
