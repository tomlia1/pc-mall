import type { LeadPayload, Product } from './types'

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface ProductSearchParams {
  page?: number
  pageSize?: number
  minPrice?: number
  maxPrice?: number
  category?: string
  brand?: string
  keyword?: string
}

async function readResult<T>(response: Response): Promise<T> {
  const result = await response.json() as ApiResult<T>
  if (!response.ok || result.code >= 400) throw new Error(result.message || '请求失败，请稍后重试')
  return result.data
}

export async function fetchProducts(params: ProductSearchParams = {}): Promise<Product[]> {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') query.set(key, String(value))
  })
  return readResult<Product[]>(await fetch(`/api/v1/products?${query.toString()}`))
}

export async function fetchProduct(id: number): Promise<Product> {
  return readResult<Product>(await fetch(`/api/v1/products/${id}`))
}

export async function submitLead(payload: LeadPayload): Promise<void> {
  const response = await fetch('/api/v1/leads', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  const result = await response.json() as ApiResult<unknown>
  if (!response.ok || result.code >= 400) throw new Error(result.message || '提交失败，请稍后再试')
}
