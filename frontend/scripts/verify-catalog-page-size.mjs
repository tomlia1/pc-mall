import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = await readFile(new URL('../src/MarketplaceApp.vue', import.meta.url), 'utf8')

assert.match(source, /const pageSize = 9\b/, '商城目录必须每页请求 9 件商品')
assert.match(source, /hasNextPage\.value = nextPage < result\.totalPages/, '下一页状态必须由后端总页数决定')
assert.match(source, /const totalPages = ref\(1\)/, '商城需要保存后端返回的总页数')
assert.match(source, /共 \{\{ totalPages \}\} 页/, '分页栏必须显示总页数')

console.log('catalog page size verification passed')
