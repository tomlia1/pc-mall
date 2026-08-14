import assert from 'node:assert/strict'
import test from 'node:test'

const { fetchProducts } = await import('../src/api.ts')

test('shows a friendly error when the API returns an empty error response', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => new Response(null, { status: 503 })

  try {
    await assert.rejects(fetchProducts(), { message: '服务暂时不可用，请稍后重试' })
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('shows a friendly error when the API cannot be reached', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => { throw new TypeError('Failed to fetch') }

  try {
    await assert.rejects(fetchProducts(), { message: '服务暂时不可用，请稍后重试' })
  } finally {
    globalThis.fetch = originalFetch
  }
})
