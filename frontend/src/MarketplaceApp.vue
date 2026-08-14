<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { checkBuild, fetchProducts, streamBuildAnalysis } from './api'
import { cleanAssistantText } from './chat-text'
import BuildListPanel from './components/BuildListPanel.vue'
import ChatWidget from './components/ChatWidget.vue'
import ProductCard from './components/ProductCard.vue'
import ProductDetailPage from './components/ProductDetailPage.vue'
import { BUILD_SLOTS, isBuildSlot, type BudgetBuildCandidate, type BuildCheckRequest, type BuildCheckResult, type BuildSlot, type Product, type ProductType } from './types'

const pageSize = 9
const products = ref<Product[]>([])
const brands = ref<string[]>([])
const loading = ref(true)
const error = ref('')
const page = ref(1)
const totalPages = ref(1)
const totalProducts = ref(0)
const hasNextPage = ref(false)
const chatOpen = ref(false)
const chatContextProductId = ref<number | null>(null)
const mobileBuildOpen = ref(false)
const buildNotice = ref('')
const buildCheckResult = ref<BuildCheckResult | null>(null)
const buildCheckError = ref('')
const checkingBuild = ref(false)
const buildAnalysisText = ref('')
const buildAnalysisProducts = ref<Product[]>([])
const buildAnalysisError = ref('')
const analyzingBuild = ref(false)
const routeProductId = ref<number | null>(productIdFromPath())
let buildAnalysisAbortController: AbortController | null = null
const filters = reactive<{ keyword: string; category: string; brand: string; minPrice: string; maxPrice: string; productType: ProductType | '' }>({ keyword: '', category: '', brand: '', minPrice: '', maxPrice: '', productType: '' })
const categories = ['办公/编程', '游戏', '设计/创作', 'DIY 装机']
const productTypes: ReadonlyArray<{ type: ProductType; label: string }> = [
  { type: 'LAPTOP', label: '笔记本' }, { type: 'CPU', label: 'CPU' }, { type: 'MOTHERBOARD', label: '主板' },
  { type: 'GPU', label: '显卡' }, { type: 'RAM', label: '内存' }, { type: 'SSD', label: 'SSD' },
  { type: 'PSU', label: '电源' }, { type: 'CASE', label: '机箱' }
]
const selectedBuild = reactive<Partial<Record<BuildSlot, Product>>>({})
const missingSlots = computed(() => BUILD_SLOTS.filter(slot => !selectedBuild[slot.type]).map(slot => slot.label))
const buildTotal = computed(() => Object.values(selectedBuild).reduce((sum, product) => sum + (product?.price || 0), 0))
const buildCheckRequest = computed<BuildCheckRequest>(() => ({
  cpuId: selectedBuild.CPU?.id ?? null,
  motherboardId: selectedBuild.MOTHERBOARD?.id ?? null,
  gpuId: selectedBuild.GPU?.id ?? null,
  ramId: selectedBuild.RAM?.id ?? null,
  ssdId: selectedBuild.SSD?.id ?? null,
  psuId: selectedBuild.PSU?.id ?? null,
  caseId: selectedBuild.CASE?.id ?? null
}))
const resultDescription = computed(() => loading.value ? '正在加载商品' : `当前显示 ${products.value.length} / 共 ${totalProducts.value} 件在售商品`)

watch(buildCheckRequest, () => {
  buildCheckResult.value = null
  buildCheckError.value = ''
  clearBuildAnalysis()
})

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
      productType: filters.productType || undefined,
      minPrice: filters.minPrice ? Number(filters.minPrice) : undefined,
      maxPrice: filters.maxPrice ? Number(filters.maxPrice) : undefined
    })
    products.value = result.products
    page.value = nextPage
    totalPages.value = result.totalPages
    totalProducts.value = result.total
    hasNextPage.value = nextPage < result.totalPages
    brands.value = [...new Set([...brands.value, ...result.products.map(product => product.brand)])].sort()
  } catch (reason) {
    products.value = []
    error.value = reason instanceof Error ? reason.message : '商品暂时无法加载，请稍后重试'
  } finally {
    loading.value = false
  }
}

function submitFilters() { void loadProducts(1) }
function chooseType(type: ProductType | '') {
  filters.productType = filters.productType === type ? '' : type
  void loadProducts(1)
}
function clearFilters() {
  Object.assign(filters, { keyword: '', category: '', brand: '', minPrice: '', maxPrice: '', productType: '' })
  void loadProducts(1)
}
function productIdFromPath(): number | null {
  const match = window.location.pathname.match(/^\/products\/(\d+)$/)
  const id = match ? Number(match[1]) : NaN
  return Number.isSafeInteger(id) && id > 0 ? id : null
}
function openDetail(product: Product) {
  window.history.pushState({}, '', `/products/${product.id}`)
  routeProductId.value = product.id
  window.scrollTo(0, 0)
}
function returnToCatalog() {
  window.history.pushState({}, '', '/')
  routeProductId.value = null
  window.scrollTo(0, 0)
}
function syncRoute() { routeProductId.value = productIdFromPath() }
function previousPage() { if (page.value > 1) void loadProducts(page.value - 1) }
function nextPage() { if (hasNextPage.value) void loadProducts(page.value + 1) }
function openChat() {
  if (routeProductId.value !== null) chatContextProductId.value = routeProductId.value
  else chatContextProductId.value = null
  chatOpen.value = true
}
function closeChat() { chatOpen.value = false; chatContextProductId.value = null }
function startNewConversation() { chatContextProductId.value = null }
function consultProduct(productId: number) {
  chatContextProductId.value = productId
  chatOpen.value = true
}
function addToBuild(product: Product) {
  if (!isBuildSlot(product.productType)) return
  const previous = selectedBuild[product.productType]
  selectedBuild[product.productType] = product
  buildNotice.value = previous && previous.id !== product.id ? `已将 ${previous.name} 替换为 ${product.name}` : `已加入 ${product.name}`
}
function removeFromBuild(type: BuildSlot) {
  delete selectedBuild[type]
  buildNotice.value = '已从装机单移除商品'
}
async function runBuildCheck() {
  checkingBuild.value = true
  buildCheckError.value = ''
  try {
    buildCheckResult.value = await checkBuild(buildCheckRequest.value)
  } catch (reason) {
    buildCheckResult.value = null
    buildCheckError.value = reason instanceof Error ? reason.message : '兼容性检查暂时无法完成，请稍后重试'
  } finally {
    checkingBuild.value = false
  }
}
function clearBuildAnalysis() {
  buildAnalysisAbortController?.abort()
  buildAnalysisAbortController = null
  analyzingBuild.value = false
  buildAnalysisText.value = ''
  buildAnalysisProducts.value = []
  buildAnalysisError.value = ''
}
function parseStreamEvent(block: string): { event: string; data: unknown } | null {
  const lines = block.split(/\r?\n/)
  const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim()
  const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).trim()).join('\n')
  return event && data ? { event, data: JSON.parse(data) } : null
}
function handleBuildAnalysisEvent(block: string) {
  try {
    const streamEvent = parseStreamEvent(block)
    if (!streamEvent) return
    if (streamEvent.event === 'check') buildCheckResult.value = streamEvent.data as BuildCheckResult
    if (streamEvent.event === 'message') buildAnalysisText.value += cleanAssistantText((streamEvent.data as { text: string }).text)
    if (streamEvent.event === 'product') {
      const product = streamEvent.data as Product
      if (!buildAnalysisProducts.value.some(item => item.id === product.id)) buildAnalysisProducts.value.push(product)
    }
    if (streamEvent.event === 'error') buildAnalysisError.value = (streamEvent.data as { message: string }).message
  } catch { buildAnalysisError.value = 'AI 解读暂时不可用，请稍后重试' }
}
async function analyzeBuild() {
  clearBuildAnalysis()
  analyzingBuild.value = true
  buildAnalysisAbortController = new AbortController()
  try {
    const response = await streamBuildAnalysis(buildCheckRequest.value, buildAnalysisAbortController.signal)
    if (!response.ok || !response.body) throw new Error('AI 解读暂时不可用，请稍后重试')
    const reader = response.body.getReader(); const decoder = new TextDecoder(); let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split(/\r?\n\r?\n/); buffer = blocks.pop() ?? ''
      blocks.forEach(handleBuildAnalysisEvent)
    }
    if (buffer.trim()) handleBuildAnalysisEvent(buffer)
  } catch (reason) {
    if (!(reason instanceof DOMException && reason.name === 'AbortError')) {
      buildAnalysisError.value = reason instanceof Error ? reason.message : 'AI 解读暂时不可用，请稍后重试'
    }
  } finally {
    analyzingBuild.value = false
    buildAnalysisAbortController = null
  }
}
function stopBuildAnalysis() { buildAnalysisAbortController?.abort() }
function replaceWithCandidate(product: Product) {
  if (!isBuildSlot(product.productType)) return
  addToBuild(product)
  buildNotice.value = `已替换为 ${product.name}，请重新运行基础检查或再次请求 AI 解读。`
}
function applyBudgetBuild(candidate: BudgetBuildCandidate) {
  for (const slot of BUILD_SLOTS) delete selectedBuild[slot.type]
  candidate.products.forEach(product => {
    if (isBuildSlot(product.productType)) selectedBuild[product.productType] = product
  })
  buildNotice.value = '已按后端预算方案填入装机单，正在运行基础检查。'
  void runBuildCheck()
}

onMounted(() => {
  window.addEventListener('popstate', syncRoute)
  void loadProducts()
})
onBeforeUnmount(() => window.removeEventListener('popstate', syncRoute))
</script>

<template>
  <ProductDetailPage v-if="routeProductId !== null" :product-id="routeProductId" @back="returnToCatalog" @add="addToBuild" @consult="consultProduct" />
  <template v-else>
  <div class="storefront-shell">
    <header class="store-header">
      <div class="store-header-inner">
        <a class="store-logo" href="#top" aria-label="PC Mall 首页">PC<span>MALL</span></a>
        <nav class="category-nav" aria-label="商品类型">
          <button type="button" :class="{ active: !filters.productType }" :aria-pressed="!filters.productType" @click="chooseType('')">全部商品</button>
          <button v-for="item in productTypes" :key="item.type" type="button" :class="{ active: filters.productType === item.type }" :aria-pressed="filters.productType === item.type" @click="chooseType(item.type)">{{ item.label }}</button>
        </nav>
        <form class="header-search" role="search" @submit.prevent="submitFilters">
          <label class="sr-only" for="product-search">搜索商品</label>
          <input id="product-search" v-model="filters.keyword" maxlength="64" placeholder="搜索商品、CPU 或显卡" />
          <button type="submit">搜索</button>
        </form>
      </div>
    </header>

    <main id="top" class="store-main">
      <section class="store-intro" aria-labelledby="store-title">
        <div class="intro-copy">
          <p class="eyebrow">PC MALL / 2026 CATALOG</p>
          <h1 id="store-title">把预算放进每一件真正需要的硬件里。</h1>
          <p>商品信息来自商品库。先选部件，再逐步搭出属于你的电脑。</p>
        </div>
        <div class="intro-categories" aria-label="快捷分类">
          <span>从一个槽位开始</span>
          <button v-for="item in productTypes.slice(1, 5)" :key="item.type" type="button" :class="{ active: filters.productType === item.type }" @click="chooseType(item.type)">{{ item.label }}</button>
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
          <div class="catalog-heading"><div><p class="eyebrow">商品目录</p><h2>在售硬件与电脑</h2></div><p>{{ resultDescription }}</p></div>
          <p v-if="error" class="catalog-error" role="alert">{{ error }}</p>
          <p v-if="buildNotice" class="build-notice" role="status">{{ buildNotice }}</p>
          <template v-if="loading"><div class="product-grid loading-grid" aria-label="正在加载商品"><div v-for="index in 4" :key="index" class="product-skeleton" /></div></template>
          <div v-else-if="products.length" class="product-grid"><ProductCard v-for="product in products" :key="product.id" :product="product" @select="openDetail" @add="addToBuild" /></div>
          <div v-else class="empty-state"><h2>没有找到匹配商品</h2><p>试试放宽价格范围，或清除部分筛选条件。</p><button type="button" class="outline-button" @click="clearFilters">查看全部商品</button></div>
          <nav class="pagination" aria-label="商品分页"><button type="button" :disabled="page === 1 || loading" @click="previousPage">上一页</button><span>第 {{ page }} / 共 {{ totalPages }} 页</span><button type="button" :disabled="!hasNextPage || loading" @click="nextPage">下一页</button></nav>
        </section>
        <BuildListPanel class="build-desktop" :selected="selectedBuild" :total-price="buildTotal" :missing-slots="missingSlots" :check-result="buildCheckResult" :checking="checkingBuild" :check-error="buildCheckError" :analysis-text="buildAnalysisText" :analysis-products="buildAnalysisProducts" :analysis-error="buildAnalysisError" :analyzing="analyzingBuild" @remove="removeFromBuild" @check="runBuildCheck" @analyze="analyzeBuild" @stop-analysis="stopBuildAnalysis" @replace="replaceWithCandidate" @close="mobileBuildOpen = false" />
      </section>
    </main>
  </div>
  <button type="button" class="build-mobile-summary" @click="mobileBuildOpen = true"><span>我的装机单 {{ 7 - missingSlots.length }}/7</span><strong>¥{{ buildTotal.toLocaleString('zh-CN') }}</strong></button>
  <div v-if="mobileBuildOpen" class="build-drawer-backdrop" @click.self="mobileBuildOpen = false"><BuildListPanel drawer :selected="selectedBuild" :total-price="buildTotal" :missing-slots="missingSlots" :check-result="buildCheckResult" :checking="checkingBuild" :check-error="buildCheckError" :analysis-text="buildAnalysisText" :analysis-products="buildAnalysisProducts" :analysis-error="buildAnalysisError" :analyzing="analyzingBuild" @remove="removeFromBuild" @check="runBuildCheck" @analyze="analyzeBuild" @stop-analysis="stopBuildAnalysis" @replace="replaceWithCandidate" @close="mobileBuildOpen = false" /></div>
  </template>
  <ChatWidget :open="chatOpen" :context-product-id="chatContextProductId" @open="openChat" @close="closeChat" @new-conversation="startNewConversation" @apply-budget-build="applyBudgetBuild" />
</template>
