<script setup lang="ts">
import ProductImage from './ProductImage.vue'
import { isBuildSlot, type Product } from '../types'

defineProps<{ product: Product }>()
defineEmits<{ select: [product: Product]; add: [product: Product] }>()

function price(value: number) {
  return `￥${Number(value).toLocaleString('zh-CN')}`
}
</script>

<template>
  <article class="store-product-card" tabindex="0" :aria-label="`查看 ${product.name} 的商品详情`" @click="$emit('select', product)" @keydown.enter="$emit('select', product)" @keydown.space.prevent="$emit('select', product)">
    <div class="product-cover">
      <ProductImage :src="product.coverImage" :alt="product.name" />
    </div>
    <div class="product-card-body">
      <p class="product-meta">{{ product.brand }} / {{ product.category }}</p>
      <h2>{{ product.name }}</h2>
      <div class="product-specs"><p>{{ product.productType === 'LAPTOP' ? product.cpu : product.tags }}</p><p v-if="product.productType === 'LAPTOP'">{{ product.gpu || '集成显卡' }}</p></div>
      <div class="product-card-footer">
        <strong>{{ price(product.price) }}</strong>
        <button v-if="isBuildSlot(product.productType)" type="button" class="add-build-button" @click.stop="$emit('add', product)">加入装机单</button>
        <button v-else type="button" class="outline-button" @click.stop="$emit('select', product)">查看配置</button>
      </div>
    </div>
  </article>
</template>
