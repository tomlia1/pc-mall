import type { BuildCheckRequest, BuildCheckResult, LeadPayload, Product, ProductType } from './types'

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

const SERVICE_UNAVAILABLE_MESSAGE = '服务暂时不可用，请稍后重试'

export interface ProductSearchParams {
  page?: number
  pageSize?: number
  minPrice?: number
  maxPrice?: number
  category?: string
  brand?: string
  keyword?: string
  productType?: ProductType
}

export interface ProductCatalogPage {
  products: Product[]
  total: number
  totalPages: number
}

async function readResult<T>(response: Response): Promise<T> {
  const body = await response.text()
  let result: ApiResult<T>
  try {
    result = JSON.parse(body) as ApiResult<T>
  } catch {
    throw new Error(SERVICE_UNAVAILABLE_MESSAGE)
  }
  if (!response.ok || result.code >= 400) throw new Error(result.message || '请求失败，请稍后重试')
  return result.data
}

async function request(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(input, init)
  } catch {
    throw new Error(SERVICE_UNAVAILABLE_MESSAGE)
  }
}

export async function fetchProducts(params: ProductSearchParams = {}): Promise<ProductCatalogPage> {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') query.set(key, String(value))
  })
  const response = await request(`/api/v1/products?${query.toString()}`)
  const products = await readResult<Product[]>(response)
  const total = Number(response.headers.get('X-Total-Count'))
  const totalPages = Number(response.headers.get('X-Total-Pages'))
  const resolvedTotal = Number.isFinite(total) ? total : products.length
  const resolvedTotalPages = Number.isFinite(totalPages)
    ? Math.max(1, totalPages)
    : Math.max(1, Math.ceil(resolvedTotal / (params.pageSize ?? 12)))
  return { products, total: resolvedTotal, totalPages: resolvedTotalPages }
}

export async function fetchProduct(id: number): Promise<Product> {
  return readResult<Product>(await request(`/api/v1/products/${id}`))
}

export async function submitLead(payload: LeadPayload): Promise<void> {
  await readResult<unknown>(await request('/api/v1/leads', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }))
}

export async function checkBuild(payload: BuildCheckRequest): Promise<BuildCheckResult> {
  return readResult<BuildCheckResult>(await request('/api/v1/builds/check', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }))
}

export async function streamBuildAnalysis(payload: BuildCheckRequest, signal: AbortSignal): Promise<Response> {
  return request('/api/v1/builds/analyze/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(payload),
    signal
  })
}
