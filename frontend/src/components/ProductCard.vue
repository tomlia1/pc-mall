<script setup lang="ts">
import ProductImage from './ProductImage.vue'
import type { Product } from '../types'

defineProps<{ product: Product }>()
defineEmits<{ select: [product: Product] }>()

function price(value: number) {
  return `￥${Number(value).toLocaleString('zh-CN')}`
}
</script>

<template>
  <article class="store-product-card">
    <div class="product-cover"><ProductImage :src="product.coverImage" :alt="product.name" /></div>
    <div class="product-card-body">
      <p class="product-meta">{{ product.brand }} · {{ product.category }}</p>
      <h2>{{ product.name }}</h2>
      <div class="product-specs"><p>{{ product.cpu }}</p><p>{{ product.gpu || '集成显卡' }}</p></div>
      <div class="product-card-footer">
        <strong>{{ price(product.price) }}</strong>
        <button type="button" class="outline-button" @click="$emit('select', product)">查看详情</button>
      </div>
    </div>
  </article>
</template>
