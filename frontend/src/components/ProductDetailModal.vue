<script setup lang="ts">
import { computed } from 'vue'
import ProductImage from './ProductImage.vue'
import type { Product } from '../types'

const props = defineProps<{ product: Product | null; loading: boolean }>()
const emit = defineEmits<{ close: []; consult: [productId: number] }>()

const price = computed(() => props.product ? `¥${Number(props.product.price).toLocaleString('zh-CN')}` : '')
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
          <div><dt>处理器</dt><dd>{{ product.cpu }}</dd></div>
          <div><dt>显卡</dt><dd>{{ product.gpu || '集成显卡' }}</dd></div>
          <div><dt>内存</dt><dd>{{ product.memoryGb }}GB</dd></div>
          <div><dt>存储</dt><dd>{{ product.storageGb }}GB</dd></div>
          <div v-if="product.tags"><dt>适用场景</dt><dd>{{ product.tags }}</dd></div>
        </dl>
        <button type="button" class="detail-consult" @click="emit('consult', product.id)">咨询 AI</button>
      </div>
    </section>
  </div>
</template>
