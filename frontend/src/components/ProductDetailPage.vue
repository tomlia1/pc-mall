<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { fetchProduct } from '../api'
import { isBuildSlot, type Product } from '../types'
import ProductImage from './ProductImage.vue'

const props = defineProps<{ productId: number }>()
const emit = defineEmits<{ back: []; add: [product: Product]; consult: [productId: number] }>()
const product = ref<Product | null>(null)
const loading = ref(true)
const error = ref('')
const added = ref('')

const price = computed(() => product.value ? `¥${Number(product.value.price).toLocaleString('zh-CN')}` : '')
const detailSpecs = computed(() => {
  const item = product.value
  if (!item) return []
  if (item.productType !== 'LAPTOP') return item.tags ? [{ label: '核心规格', value: item.tags }] : []
  return [
    { label: '处理器', value: item.cpu || '未提供' },
    { label: '显卡', value: item.gpu || '集成显卡' },
    { label: '内存', value: item.memoryGb ? `${item.memoryGb}GB` : '未提供' },
    { label: '存储', value: item.storageGb ? `${item.storageGb}GB` : '未提供' },
    ...(item.tags ? [{ label: '适用场景', value: item.tags }] : [])
  ]
})

async function loadProduct() {
  loading.value = true
  error.value = ''
  added.value = ''
  try {
    product.value = await fetchProduct(props.productId)
  } catch (reason) {
    product.value = null
    error.value = reason instanceof Error ? reason.message : '商品详情暂时无法加载，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function addToBuild() {
  if (!product.value || !isBuildSlot(product.value.productType)) return
  emit('add', product.value)
  added.value = `已加入装机单：${product.value.name}`
}

watch(() => props.productId, loadProduct, { immediate: true })
</script>

<template>
  <main class="product-detail-page">
    <div class="product-detail-page-inner">
      <button type="button" class="detail-back-button" @click="emit('back')">← 返回商城</button>
      <p v-if="loading" class="detail-page-message">正在加载商品详情…</p>
      <section v-else-if="product" class="product-detail-layout" :aria-label="`${product.name} 商品详情`">
        <div class="product-detail-visual"><ProductImage :src="product.coverImage" :alt="product.name" /></div>
        <div class="product-detail-copy">
          <p class="product-meta">{{ product.brand }} / {{ product.category }}</p>
          <h1>{{ product.name }}</h1>
          <p class="detail-price">{{ price }}</p>
          <dl class="spec-list">
            <div v-for="spec in detailSpecs" :key="spec.label"><dt>{{ spec.label }}</dt><dd>{{ spec.value }}</dd></div>
          </dl>
          <p v-if="added" class="detail-page-success" role="status">{{ added }}</p>
          <div class="product-detail-actions">
            <button v-if="isBuildSlot(product.productType)" type="button" class="detail-consult" @click="addToBuild">加入装机单</button>
            <button v-else type="button" class="detail-consult" @click="emit('consult', product.id)">咨询通用导购</button>
          </div>
        </div>
      </section>
      <section v-else class="detail-page-message detail-page-error" role="alert">
        <h1>商品详情无法打开</h1><p>{{ error }}</p><button type="button" class="detail-back-button" @click="emit('back')">返回商城</button>
      </section>
    </div>
  </main>
</template>
