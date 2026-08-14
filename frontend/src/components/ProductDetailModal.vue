<script setup lang="ts">
import { computed } from 'vue'
import ProductImage from './ProductImage.vue'
import { isBuildSlot, type Product } from '../types'

const props = defineProps<{ product: Product | null; loading: boolean }>()
const emit = defineEmits<{ close: []; consult: [productId: number]; add: [product: Product] }>()

const price = computed(() => props.product ? `¥${Number(props.product.price).toLocaleString('zh-CN')}` : '')
const detailSpecs = computed(() => {
  const product = props.product
  if (!product) return []
  if (product.productType !== 'LAPTOP') return product.tags ? [{ label: '核心规格', value: product.tags }] : []
  return [
    { label: '处理器', value: product.cpu || '未提供' },
    { label: '显卡', value: product.gpu || '集成显卡' },
    { label: '内存', value: product.memoryGb ? `${product.memoryGb}GB` : '未提供' },
    { label: '存储', value: product.storageGb ? `${product.storageGb}GB` : '未提供' }
  ]
})
</script>

<template>
  <div v-if="product" class="detail-backdrop" @click.self="$emit('close')">
    <section class="detail-modal" role="dialog" aria-modal="true" :aria-label="`${product.name} 商品详情`">
      <button type="button" class="icon-button" aria-label="关闭商品详情" @click="$emit('close')">×</button>
      <div class="detail-cover"><ProductImage :src="product.coverImage" :alt="product.name" /></div>
      <div class="detail-content">
        <p class="product-meta">{{ product.brand }} · {{ product.category }}</p>
        <h2>{{ product.name }}</h2>
        <p class="detail-price">{{ price }}</p>
        <p v-if="loading" class="detail-loading">正在加载真实配置…</p>
        <dl v-else class="spec-list">
          <div v-for="spec in detailSpecs" :key="spec.label"><dt>{{ spec.label }}</dt><dd>{{ spec.value }}</dd></div>
          <div v-if="product.productType === 'LAPTOP' && product.tags"><dt>适用场景</dt><dd>{{ product.tags }}</dd></div>
        </dl>
        <button v-if="isBuildSlot(product.productType)" type="button" class="detail-consult" @click="emit('add', product)">加入装机单</button>
        <button v-else type="button" class="detail-consult" @click="emit('consult', product.id)">咨询 AI</button>
      </div>
    </section>
  </div>
</template>
