import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { CartPanel } from './CartPanel'
import type { CartView } from '../lib/api'

const cart: CartView = {
  cartToken: 'abc-123',
  lines: [
    {
      sku: 'KEY-3001',
      name: '65 Percent Mechanical Keyboard',
      imageUrl: null,
      quantity: 2,
      unitPriceCents: 14900,
      lineTotalCents: 29800,
      availableQty: 18,
    },
  ],
  totalQuantity: 2,
  subtotalCents: 29800,
  shippingCents: 0,
  taxCents: 3576,
  totalCents: 33376,
  currency: 'USD',
}

const emptyCart: CartView = {
  ...cart,
  lines: [],
  totalQuantity: 0,
  subtotalCents: 0,
  shippingCents: 0,
  taxCents: 0,
  totalCents: 0,
}

const noop = () => {}

describe('CartPanel', () => {
  it('renders every server-computed figure verbatim', () => {
    render(
      <CartPanel
        cart={cart}
        loading={false}
        onSetQuantity={noop}
        onRemove={noop}
        busySku={null}
        error={null}
      />,
    )

    expect(screen.getByTestId('subtotal')).toHaveTextContent('$298.00')
    expect(screen.getByTestId('tax')).toHaveTextContent('$35.76')
    expect(screen.getByTestId('total')).toHaveTextContent('$333.76')
    expect(screen.getByTestId('shipping')).toHaveTextContent('Free')
  })

  it('states the currency next to the total', () => {
    render(
      <CartPanel
        cart={cart}
        loading={false}
        onSetQuantity={noop}
        onRemove={noop}
        busySku={null}
        error={null}
      />,
    )

    expect(screen.getByTestId('total')).toHaveTextContent('USD')
  })

  /**
   * The transparency rule taken from the reference order summary: the rows stay
   * on screen when the cart is empty rather than vanishing, so the shopper can
   * always see what the total is made of.
   */
  it('keeps the summary rows visible for an empty cart', () => {
    render(
      <CartPanel
        cart={emptyCart}
        loading={false}
        onSetQuantity={noop}
        onRemove={noop}
        busySku={null}
        error={null}
      />,
    )

    expect(screen.getByTestId('subtotal')).toHaveTextContent('$0.00')
    expect(screen.getByTestId('total')).toHaveTextContent('$0.00')
    expect(screen.getByTestId('shipping')).toHaveTextContent('Add an item')
    expect(screen.getByText(/nothing here yet/i)).toBeInTheDocument()
  })

  it('does no arithmetic of its own, even when the server total looks wrong', () => {
    // A deliberately inconsistent payload. The panel must show what it was told,
    // because the server is the single source of truth for money.
    render(
      <CartPanel
        cart={{ ...cart, totalCents: 999 }}
        loading={false}
        onSetQuantity={noop}
        onRemove={noop}
        busySku={null}
        error={null}
      />,
    )

    expect(screen.getByTestId('total')).toHaveTextContent('$9.99')
  })

  it('steps quantity up and down through the callback', async () => {
    const onSetQuantity = vi.fn()
    render(
      <CartPanel
        cart={cart}
        loading={false}
        onSetQuantity={onSetQuantity}
        onRemove={noop}
        busySku={null}
        error={null}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: /increase quantity/i }))
    expect(onSetQuantity).toHaveBeenCalledWith('KEY-3001', 3)

    await userEvent.click(screen.getByRole('button', { name: /decrease quantity/i }))
    expect(onSetQuantity).toHaveBeenCalledWith('KEY-3001', 1)
  })

  /** Stepping to zero is the documented removal affordance. */
  it('steps down to zero from one, which the server treats as a removal', async () => {
    const onSetQuantity = vi.fn()
    render(
      <CartPanel
        cart={{ ...cart, lines: [{ ...cart.lines[0], quantity: 1 }] }}
        loading={false}
        onSetQuantity={onSetQuantity}
        onRemove={noop}
        busySku={null}
        error={null}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: /decrease quantity/i }))

    expect(onSetQuantity).toHaveBeenCalledWith('KEY-3001', 0)
  })

  it('cannot increase past what the server says is available', () => {
    render(
      <CartPanel
        cart={{ ...cart, lines: [{ ...cart.lines[0], availableQty: 0 }] }}
        loading={false}
        onSetQuantity={noop}
        onRemove={noop}
        busySku={null}
        error={null}
      />,
    )

    expect(screen.getByRole('button', { name: /increase quantity/i })).toBeDisabled()
  })

  it('removes a line through the callback', async () => {
    const onRemove = vi.fn()
    render(
      <CartPanel
        cart={cart}
        loading={false}
        onSetQuantity={noop}
        onRemove={onRemove}
        busySku={null}
        error={null}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: /remove 65 percent/i }))

    expect(onRemove).toHaveBeenCalledWith('KEY-3001')
  })

  it('locks the controls for the line currently being written', () => {
    render(
      <CartPanel
        cart={cart}
        loading={false}
        onSetQuantity={noop}
        onRemove={noop}
        busySku="KEY-3001"
        error={null}
      />,
    )

    expect(screen.getByRole('button', { name: /increase quantity/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /decrease quantity/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /remove 65 percent/i })).toBeDisabled()
  })

  it('surfaces a server refusal as an alert rather than swallowing it', () => {
    render(
      <CartPanel
        cart={cart}
        loading={false}
        onSetQuantity={noop}
        onRemove={noop}
        busySku={null}
        error="Only 0 left of DSK-2002, cannot reserve 1."
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Only 0 left of DSK-2002')
  })

  it('shows a loading line before the first cart arrives', () => {
    render(
      <CartPanel
        cart={undefined}
        loading
        onSetQuantity={noop}
        onRemove={noop}
        busySku={null}
        error={null}
      />,
    )

    expect(screen.getByText(/loading your cart/i)).toBeInTheDocument()
  })
})
