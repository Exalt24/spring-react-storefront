import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ProductCard } from './ProductCard'
import type { ProductSummary } from '../lib/api'

const product: ProductSummary = {
  sku: 'KEY-3001',
  name: '65 Percent Mechanical Keyboard',
  priceCents: 14900,
  currency: 'USD',
  imageUrl: '/img/key-3001.jpg',
  inStock: true,
  categorySlug: 'keyboards',
}

function renderCard(overrides: Partial<ProductSummary> = {}, pending = false) {
  const onAdd = vi.fn()
  render(
    <ul>
      <ProductCard product={{ ...product, ...overrides }} onAdd={onAdd} pending={pending} />
    </ul>,
  )
  return { onAdd }
}

describe('ProductCard', () => {
  it('shows name, price and category', () => {
    renderCard()

    expect(screen.getByText('65 Percent Mechanical Keyboard')).toBeInTheDocument()
    expect(screen.getByText('$149.00')).toBeInTheDocument()
    expect(screen.getByText('keyboards')).toBeInTheDocument()
  })

  /**
   * Baymard principle 1: the same attributes appear on every tile. An in-stock
   * and a sold-out card must expose the same labels, only different values,
   * otherwise neighbouring tiles stop being comparable.
   */
  it('renders the same attribute labels whether in stock or not', () => {
    const { unmount } = render(
      <ul>
        <ProductCard product={product} onAdd={() => {}} pending={false} />
      </ul>,
    )
    expect(screen.getByText('Category')).toBeInTheDocument()
    expect(screen.getByText('Stock')).toBeInTheDocument()
    unmount()

    render(
      <ul>
        <ProductCard product={{ ...product, inStock: false }} onAdd={() => {}} pending={false} />
      </ul>,
    )
    expect(screen.getByText('Category')).toBeInTheDocument()
    expect(screen.getByText('Stock')).toBeInTheDocument()
  })

  it('calls onAdd with the sku when the button is pressed', async () => {
    const { onAdd } = renderCard()

    await userEvent.click(screen.getByRole('button', { name: /add 65 percent/i }))

    expect(onAdd).toHaveBeenCalledWith('KEY-3001')
  })

  it('cannot be added when sold out', async () => {
    const { onAdd } = renderCard({ inStock: false })

    const button = screen.getByRole('button', { name: /add 65 percent/i })
    expect(button).toBeDisabled()
    await userEvent.click(button)

    expect(onAdd).not.toHaveBeenCalled()
  })

  it('marks a sold out product visibly and in its stock attribute', () => {
    renderCard({ inStock: false })

    expect(screen.getByText('Sold out')).toBeInTheDocument()
    expect(screen.getByText('None')).toBeInTheDocument()
  })

  it('disables the button and says so while a request is in flight', () => {
    renderCard({}, true)

    const button = screen.getByRole('button', { name: /add 65 percent/i })
    expect(button).toBeDisabled()
    expect(button).toHaveTextContent('Adding')
  })

  it('renders as a list item so a grid of these is a real list', () => {
    renderCard()

    expect(screen.getByRole('listitem')).toBeInTheDocument()
  })
})
