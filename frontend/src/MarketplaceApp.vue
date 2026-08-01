<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchProduct, fetchProducts } from './api'
import ChatWidget from './components/ChatWidget.vue'
import ProductCard from './components/ProductCard.vue'
import ProductDetailModal from './components/ProductDetailModal.vue'
import type { Product } from './types'

const pageSize = 12
const products = ref<Product[]>([])
const brands = ref<string[]>([])
const loading = ref(true)
const error = ref('')
const page = ref(1)
const hasNextPage = ref(false)
const selectedProduct = ref<Product | null>(null)
const detailLoading = ref(false)
const chatOpen = ref(false)
const chatContextProductId = ref<number | null>(null)
const filters = reactive({ keyword: '', category: '', brand: '', minPrice: '', maxPrice: '' })
const categories = ['办公/编程', '游戏', '设计/创作']
const resultDescription = computed(() => loading.value ? '正在加载商品' : `当前显示 ${products.value.length} 件在售商品`)

async function loadProducts(nextPage = page.value) {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchProducts({
      page: nextPage,
      pageSize,
      keyword: filters.keyword.trim(),
      category: filters.category,
      brand: filters.brand,
      minPrice: filters.minPrice ? Number(filters.minPrice) : undefined,
      maxPrice: filters.maxPrice ? Number(filters.maxPrice) : undefined
    })
    products.value = result
    page.value = nextPage
    hasNextPage.value = result.length === pageSize
    brands.value = [...new Set([...brands.value, ...result.map(product => product.brand)])].sort()
  } catch (reason) {
    products.value = []
    error.value = reason instanceof Error ? reason.message : '商品暂时无法加载，请稍后重试'
  } finally {
    loading.value = false
  }
}

function submitFilters() { void loadProducts(1) }
function chooseCategory(category: string) {
  filters.category = filters.category === category ? '' : category
  void loadProducts(1)
}
function clearFilters() {
  Object.assign(filters, { keyword: '', category: '', brand: '', minPrice: '', maxPrice: '' })
  void loadProducts(1)
}
async function openDetail(product: Product) {
  selectedProduct.value = product
  detailLoading.value = true
  try {
    selectedProduct.value = await fetchProduct(product.id)
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '商品详情暂时无法加载'
  } finally {
    detailLoading.value = false
  }
}
function previousPage() { if (page.value > 1) void loadProducts(page.value - 1) }
function nextPage() { if (hasNextPage.value) void loadProducts(page.value + 1) }
function openChat() { chatContextProductId.value = null; chatOpen.value = true }
function consultProduct(productId: number) {
  selectedProduct.value = null
  chatContextProductId.value = productId
  chatOpen.value = true
}

onMounted(() => { void loadProducts() })
</script>

<template>
  <div class="storefront-shell">
    <header class="store-header">
      <div class="store-header-inner">
        <a class="store-logo" href="#top" aria-label="PC Mall 首页">PC<span>MALL</span></a>
        <nav class="category-nav" aria-label="商品分类">
          <button type="button" :class="{ active: !filters.category }" :aria-pressed="!filters.category" @click="chooseCategory('')">全部商品</button>
          <button v-for="category in categories" :key="category" type="button" :class="{ active: filters.category === category }" :aria-pressed="filters.category === category" @click="chooseCategory(category)">{{ category }}</button>
        </nav>
        <form class="header-search" role="search" @submit.prevent="submitFilters">
          <label class="sr-only" for="product-search">搜索商品</label>
          <input id="product-search" v-model="filters.keyword" maxlength="64" placeholder="搜索商品、CPU 或显卡" />
          <button type="submit">搜索</button>
        </form>
      </div>
    </header>

    <main id="top" class="store-main">
      <section class="store-hero" aria-labelledby="store-title">
        <div>
          <p class="eyebrow">PC MALL · 电脑商城</p>
          <h1 id="store-title">挑一台适合你的电脑</h1>
          <p>商品信息来自商品库。按预算、品牌和用途筛选，快速比较真实配置。</p>
        </div>
        <div class="hero-categories" aria-label="快捷分类">
          <button v-for="category in categories" :key="category" type="button" :class="{ active: filters.category === category }" @click="chooseCategory(category)">{{ category }}</button>
        </div>
      </section>

      <section class="catalog-layout" aria-label="商品目录">
        <form class="filter-panel" @submit.prevent="submitFilters">
          <div class="filter-heading"><h2>筛选商品</h2><button type="button" @click="clearFilters">清除条件</button></div>
          <label>价格区间
            <span class="price-range"><input v-model="filters.minPrice" aria-label="最低价格" type="number" min="0" step="100" placeholder="最低" /><span>至</span><input v-model="filters.maxPrice" aria-label="最高价格" type="number" min="0" step="100" placeholder="最高" /></span>
          </label>
          <label>品牌<select v-model="filters.brand"><option value="">全部品牌</option><option v-for="brand in brands" :key="brand" :value="brand">{{ brand }}</option></select></label>
          <label>用途<select v-model="filters.category"><option value="">全部用途</option><option v-for="category in categories" :key="category" :value="category">{{ category }}</option></select></label>
          <button type="submit" class="primary-button">应用筛选</button>
        </form>

        <section class="catalog-content" aria-live="polite">
          <div class="catalog-heading"><div><p class="eyebrow">商品目录</p><h2>在售电脑</h2></div><p>{{ resultDescription }}</p></div>
          <p v-if="error" class="catalog-error" role="alert">{{ error }}</p>
          <div v-if="loading" class="product-grid loading-grid" aria-label="正在加载商品"><div v-for="index in 4" :key="index" class="product-skeleton" /></div>
          <div v-else-if="products.length" class="product-grid"><ProductCard v-for="product in products" :key="product.id" :product="product" @select="openDetail" /></div>
          <div v-else class="empty-state"><h2>没有找到匹配商品</h2><p>试试放宽价格范围，或清除部分筛选条件。</p><button type="button" class="outline-button" @click="clearFilters">查看全部商品</button></div>
          <nav class="pagination" aria-label="商品分页"><button type="button" :disabled="page === 1 || loading" @click="previousPage">上一页</button><span>第 {{ page }} 页</span><button type="button" :disabled="!hasNextPage || loading" @click="nextPage">下一页</button></nav>
        </section>
      </section>
    </main>
  </div>
  <ProductDetailModal :product="selectedProduct" :loading="detailLoading" @close="selectedProduct = null" @consult="consultProduct" />
  <ChatWidget :open="chatOpen" :context-product-id="chatContextProductId" @open="openChat" @close="chatOpen = false" />
</template>
