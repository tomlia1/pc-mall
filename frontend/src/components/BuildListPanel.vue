<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import ProductImage from './ProductImage.vue'
import { BUILD_SLOTS, type BuildCheckResult, type BuildSlot, type Product } from '../types'

const props = defineProps<{
  selected: Partial<Record<BuildSlot, Product>>
  totalPrice: number
  missingSlots: string[]
  checkResult: BuildCheckResult | null
  checking: boolean
  checkError: string
  analysisText: string
  analysisProducts: Product[]
  analysisError: string
  analyzing: boolean
  drawer?: boolean
}>()

const emit = defineEmits<{ remove: [type: BuildSlot]; check: []; analyze: []; stopAnalysis: []; replace: [product: Product]; close: [] }>()
const selectedCount = computed(() => BUILD_SLOTS.length - props.missingSlots.length)
const price = computed(() => `¥${props.totalPrice.toLocaleString('zh-CN')}`)
const slotLabels = Object.fromEntries(BUILD_SLOTS.map(slot => [slot.type, slot.label])) as Record<BuildSlot, string>
const issueLabels: Record<string, string> = {
  CPU_MOTHERBOARD_SOCKET: 'CPU 与主板接口',
  MOTHERBOARD_RAM_MEMORY_TYPE: '主板与内存类型',
  GPU_CASE_LENGTH: '显卡与机箱尺寸',
  MOTHERBOARD_CASE_FORM_FACTOR: '主板与机箱板型',
  SSD_MOTHERBOARD_INTERFACE: 'SSD 与主板接口',
  PSU_POWER_HEADROOM: '电源余量建议'
}
const resultTitle = computed(() => {
  if (!props.checkResult) return ''
  return ({ INCOMPLETE: '装机单未配齐', ERROR: '发现兼容性问题', WARNING: '存在建议项', PASS: '基础检查通过' })[props.checkResult.status]
})
const resultMissingSlots = computed(() => props.checkResult?.missingSlots.map(slot => slotLabels[slot as BuildSlot] ?? slot).join('、') ?? '')
function issueLabel(rule: string) { return issueLabels[rule] ?? '基础检查提示' }
const powerHeadroomIssue = computed(() => props.checkResult?.issues.find(issue => issue.rule === 'PSU_POWER_HEADROOM'))
const powerHeadroomPass = computed(() => Boolean(props.selected.PSU && props.checkResult?.recommendedPsuW !== null && !powerHeadroomIssue.value))
function productSpecs(product?: Product) {
  if (!product) return ''
  return [product.tags, product.cpu, product.gpu].filter(Boolean).join(' · ')
}
const scrollContainer = ref<HTMLElement | null>(null)
const exportStatus = ref('')

function scrollToLatestResult() {
  void nextTick(() => scrollContainer.value?.scrollTo({ top: scrollContainer.value.scrollHeight, behavior: 'smooth' }))
}
function requestCheck() { emit('check'); scrollToLatestResult() }
function requestAnalysis() { emit('analyze'); scrollToLatestResult() }

function buildListText() {
  const lines = [
    'PC Mall 装机清单',
    '说明：此清单仅供选配沟通，不是订单或购买凭证。',
    ''
  ]

  for (const slot of BUILD_SLOTS) {
    const product = props.selected[slot.type]
    lines.push(product
      ? `${slot.label}：${product.brand} ${product.name}（¥${Number(product.price).toLocaleString('zh-CN')}）`
      : `${slot.label}：待选择`)
  }

  lines.push('', `当前总价：¥${props.totalPrice.toLocaleString('zh-CN')}`)
  if (props.missingSlots.length) lines.push(`缺少槽位：${props.missingSlots.join('、')}`)

  if (!props.checkResult) {
    lines.push('基础检查：尚未运行')
  } else {
    lines.push(`基础检查：${resultTitle.value}`)
    if (props.checkResult.recommendedPsuW !== null) {
      lines.push(`估算功耗：${props.checkResult.estimatedPowerW}W；建议电源：${props.checkResult.recommendedPsuW}W 以上`)
    }
    for (const issue of props.checkResult.issues) lines.push(`${issueLabel(issue.rule)}：${issue.message}`)
  }

  return lines.join('\n')
}

async function copyBuildList() {
  const text = buildListText()
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.append(textarea)
      textarea.select()
      if (!document.execCommand('copy')) throw new Error('copy failed')
      textarea.remove()
    }
    exportStatus.value = '装机清单已复制，可粘贴发送给商家或朋友。'
  } catch {
    exportStatus.value = '复制失败，请使用“导出清单”保存文本文件。'
  }
}

function downloadBuildList() {
  const blob = new Blob([buildListText()], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `PC-Mall-装机清单-${new Date().toISOString().slice(0, 10)}.txt`
  document.body.append(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
  exportStatus.value = '装机清单已下载为文本文件。'
}

watch(() => [props.checkResult, props.analysisText, props.analysisProducts.length, props.analysisError], scrollToLatestResult)
</script>

<template>
  <aside class="build-list-panel" :class="{ 'build-list-drawer': drawer }" aria-label="我的装机单">
    <header class="build-list-heading">
      <div><p class="eyebrow">MY BUILD</p><h2>我的装机单</h2></div>
      <button v-if="drawer" type="button" class="icon-button" aria-label="关闭装机单" @click="emit('close')">×</button>
      <span v-else>{{ selectedCount }}/7</span>
    </header>
    <div ref="scrollContainer" class="build-list-scroll">
    <ol class="build-slot-list">
      <li v-for="slot in BUILD_SLOTS" :key="slot.type" :class="{ selected: selected[slot.type] }">
        <span class="build-slot-name">{{ slot.label }}</span>
        <template v-if="selected[slot.type]">
          <strong class="build-slot-product" :title="`${selected[slot.type]?.brand} · ¥${Number(selected[slot.type]?.price).toLocaleString('zh-CN')}\n${productSpecs(selected[slot.type])}`">{{ selected[slot.type]?.name }}</strong>
          <div class="build-slot-tooltip" aria-hidden="true">
            <div class="build-slot-tooltip-image">
              <ProductImage :src="selected[slot.type]?.coverImage" :alt="selected[slot.type]?.name ?? ''" />
            </div>
            <div class="build-slot-tooltip-copy">
            <b>{{ selected[slot.type]?.name }}</b>
            <span>{{ selected[slot.type]?.brand }} · ¥{{ Number(selected[slot.type]?.price).toLocaleString('zh-CN') }}</span>
            <small v-if="productSpecs(selected[slot.type])">{{ productSpecs(selected[slot.type]) }}</small>
            </div>
          </div>
          <button type="button" :aria-label="`移除 ${slot.label}`" @click="emit('remove', slot.type)">移除</button>
        </template>
        <span v-else class="build-slot-empty">待选择</span>
      </li>
    </ol>
    <div class="build-summary">
      <p><span>当前总价</span><strong>{{ price }}</strong></p>
      <p class="build-missing">{{ missingSlots.length ? `还缺：${missingSlots.join('、')}` : '七个槽位已选择' }}</p>
    </div>
    <div v-if="checkResult" class="build-check-result" :class="`build-check-${checkResult.status.toLowerCase()}`" aria-live="polite">
      <strong>{{ resultTitle }}</strong>
      <p v-if="checkResult.status === 'INCOMPLETE'">还缺：{{ resultMissingSlots }}</p>
      <p v-else-if="checkResult.status === 'PASS'">七个槽位已配齐，基础兼容性检查未发现问题。</p>
      <p v-if="checkResult.recommendedPsuW !== null">估算功耗 {{ checkResult.estimatedPowerW }}W，建议电源 {{ checkResult.recommendedPsuW }}W 以上。</p>
      <ul v-if="checkResult.issues.length" class="build-check-issues">
        <li v-for="issue in checkResult.issues" :key="`${issue.level}-${issue.rule}`" :class="`build-check-issue-${issue.level.toLowerCase()}`">
          <strong>{{ issueLabel(issue.rule) }}</strong><span>{{ issue.message }}</span>
        </li>
      </ul>
    </div>
    <p v-if="checkResult && checkResult.recommendedPsuW !== null" class="build-power-summary" :class="{ 'is-pass': powerHeadroomPass }">
      <template v-if="powerHeadroomPass"><strong>电源余量：通过。</strong>已选 {{ selected.PSU?.name }}，估算功耗 {{ checkResult?.estimatedPowerW }}W，建议电源 {{ checkResult?.recommendedPsuW }}W 以上。</template>
      <template v-else>估算功耗 {{ checkResult?.estimatedPowerW }}W，建议电源 {{ checkResult?.recommendedPsuW }}W 以上。</template>
    </p>
    <p v-else-if="checkError" class="build-check-error" role="alert">{{ checkError }}</p>
    <div v-else class="build-check-pending">
      <strong>完成选配后运行基础检查</strong>
      <span>由后端按在售商品事实重新查询并判断，不信任浏览器传入的规格。</span>
    </div>
    <section v-if="analysisText || analysisProducts.length || analysisError" class="build-analysis" aria-live="polite">
      <p class="eyebrow">AI 解读</p>
      <p class="build-analysis-caption">解读当前装机单的后端检查与预算取舍，不会改变基础检查结论。</p>
      <p v-if="analysisText" class="build-analysis-text">{{ analysisText }}</p>
      <p v-if="analysisError" class="build-analysis-error" role="alert">{{ analysisError }}</p>
      <div v-if="analysisProducts.length" class="build-analysis-products">
        <article v-for="product in analysisProducts" :key="product.id" class="build-analysis-product">
          <p>{{ product.brand }} · {{ product.productType }}</p>
          <strong>{{ product.name }}</strong>
          <span>¥{{ Number(product.price).toLocaleString('zh-CN') }}</span>
          <small>{{ product.tags }}</small>
          <button type="button" @click="emit('replace', product)">替换为此商品</button>
        </article>
      </div>
    </section>
    </div>
    <div class="build-actions">
    <div class="build-export-actions">
      <button type="button" class="build-export-button" @click="copyBuildList">复制清单</button>
      <button type="button" class="build-export-button" @click="downloadBuildList">导出清单</button>
    </div>
    <p v-if="exportStatus" class="build-export-status" role="status">{{ exportStatus }}</p>
    <button type="button" class="build-check-button" :disabled="checking" @click="requestCheck">{{ checking ? '检查中…' : '运行基础检查' }}</button>
    <button v-if="analyzing" type="button" class="build-analysis-stop" @click="emit('stopAnalysis')">停止解读</button>
    <button v-else type="button" class="build-ai-button" @click="requestAnalysis">解读当前装机单</button>
    <p>仅解释当前七件商品的后端检查结果与预算取舍。</p>
    </div>
  </aside>
</template>
