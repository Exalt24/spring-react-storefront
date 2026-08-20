import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'

import {
  addToCart,
  createCart,
  getCart,
  removeFromCart,
  setQuantity,
  type CartView,
} from '../lib/api'

const TOKEN_KEY = 'storefront.cartToken'

/**
 * The cart token is the only piece of client state worth persisting. Keeping it
 * in localStorage means a refresh does not silently abandon a cart, and the
 * token is opaque so it reveals nothing about the row behind it.
 */
export function useCartToken() {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))

  useEffect(() => {
    if (token) return
    let cancelled = false
    createCart().then((cart) => {
      if (cancelled) return
      localStorage.setItem(TOKEN_KEY, cart.cartToken)
      setToken(cart.cartToken)
    })
    return () => {
      cancelled = true
    }
  }, [token])

  return token
}

export function useCart(token: string | null) {
  const queryClient = useQueryClient()
  const key = ['cart', token]

  const query = useQuery({
    queryKey: key,
    queryFn: () => getCart(token as string),
    enabled: Boolean(token),
  })

  /**
   * Every mutation returns the whole cart, so the response replaces the cache
   * outright. That is deliberate: recomputing totals on the client from a
   * partial response is how a cart total and a checkout total drift apart.
   */
  const add = useMutation({
    mutationFn: (vars: { sku: string; quantity: number }) =>
      addToCart(token as string, vars.sku, vars.quantity),
    onSuccess: (cart: CartView) => queryClient.setQueryData(key, cart),
  })

  const update = useMutation({
    mutationFn: (vars: { sku: string; quantity: number }) =>
      setQuantity(token as string, vars.sku, vars.quantity),
    onSuccess: (cart: CartView) => queryClient.setQueryData(key, cart),
  })

  const remove = useMutation({
    mutationFn: (vars: { sku: string }) => removeFromCart(token as string, vars.sku),
    onSuccess: (cart: CartView) => queryClient.setQueryData(key, cart),
  })

  return { query, add, update, remove }
}
