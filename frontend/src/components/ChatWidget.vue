<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import LeadModal from './LeadModal.vue'
import { cleanAssistantText } from '../chat-text'
import type { BudgetBuildCandidate, BudgetFeasibilityResult, ChatMessage, Product, ProductSearchResult } from '../types'

interface StreamEvent { event: string; data: unknown }

const props = defineProps<{ open: boolean; contextProductId: number | null }>()
const emit = defineEmits<{ open: []; close: []; newConversation: []; applyBudgetBuild: [candidate: BudgetBuildCandidate] }>()
const buildSlotLabels: Record<string, string> = {
  CPU: 'CPU',
  MOTHERBOARD: '主板',
  GPU: '显卡',
  RAM: '内存套装',
  SSD: 'SSD',
  PSU: '电源',
  CASE: '机箱'
}
const defaultPanelSize = { width: 600, height: 720 }
const messages = ref<ChatMessage[]>([welcomeMessage()])
const draft = ref('')
const generating = ref(false)
const status = ref('')
const selectedProduct = ref<Product | null>(null)
const currentSessionId = ref(createSessionId())
const panelSize = ref({ ...defaultPanelSize })
const expanded = ref(false)
const resizing = ref(false)
const dragging = ref(false)
const panelElement = ref<HTMLElement | null>(null)
const panelOffset = ref({ x: 0, y: 0 })
let abortController: AbortController | null = null
let typingTimer: number | null = null
let typingQueue = ''
let pendingDone = false
type ResizeDirection = 'width' | 'height' | 'both'
let resizeOrigin: { x: number; y: number; width: number; height: number; direction: ResizeDirection } | null = null
let dragOrigin: { x: number; y: number; offsetX: number; offsetY: number } | null = null

const contextHint = computed(() => props.contextProductId === null ? '' : '正在咨询当前查看的商品')
const widgetStyle = computed(() => ({ '--widget-width': `${panelSize.value.width}px`, '--widget-height': `${panelSize.value.height}px` }))
const rootStyle = computed(() => ({ '--chat-offset-x': `${panelOffset.value.x}px`, '--chat-offset-y': `${panelOffset.value.y}px` }))

function createSessionId(): string { return crypto.randomUUID() }
function welcomeMessage(): ChatMessage { return { id: 'welcome', role: 'assistant', content: '你好，我可以按预算、用途和偏好为你推荐电脑。告诉我你想用电脑做什么吧。', products: [] } }
function newMessage(role: ChatMessage['role'], content: string): ChatMessage { return { id: crypto.randomUUID(), role, content, products: [] } }
function currency(value: number | null) { return value === null ? '暂未计算' : `¥${Number(value).toLocaleString('zh-CN')}` }
function buildSlotLabel(productType: string) { return buildSlotLabels[productType] ?? productType }
function parseEvent(block: string): StreamEvent | null {
  const lines = block.split(/\r?\n/)
  const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim()
  const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).trim()).join('\n')
  return event && data ? { event, data: JSON.parse(data) } : null
}
function assistantMessage(): ChatMessage | undefined { return [...messages.value].reverse().find(message => message.role === 'assistant') }
function removeEmptyAssistantMessage() {
  const message = assistantMessage()
  if (message && !message.content.trim() && !message.products.length) {
    messages.value = messages.value.filter(item => item.id !== message.id)
  }
}
function finishWhenTyped() { if (pendingDone && !typingQueue) { generating.value = false; pendingDone = false } }
function enqueueText(text: string) {
  typingQueue += text
  if (typingTimer !== null) return
  const typeNext = () => {
    const message = assistantMessage()
    if (!message || !typingQueue) { typingTimer = null; finishWhenTyped(); return }
    const [character, ...rest] = Array.from(typingQueue)
    message.content += character
    typingQueue = rest.join('')
    typingTimer = window.setTimeout(typeNext, 14)
  }
  typeNext()
}
function handleEventBlock(block: string) {
  try {
    const streamEvent = parseEvent(block)
    if (!streamEvent) return
    if (streamEvent.event === 'budget_result') assistantMessage()!.budgetResult = streamEvent.data as BudgetFeasibilityResult
    if (streamEvent.event === 'product_search_result') assistantMessage()!.productSearchResult = streamEvent.data as ProductSearchResult
    if (streamEvent.event === 'message') enqueueText(cleanAssistantText((streamEvent.data as { text: string }).text))
    if (streamEvent.event === 'product') assistantMessage()?.products.push(streamEvent.data as Product)
    if (streamEvent.event === 'error') { status.value = (streamEvent.data as { message: string }).message; removeEmptyAssistantMessage() }
    if (streamEvent.event === 'done') { pendingDone = true; finishWhenTyped() }
  } catch { status.value = '收到的推荐数据格式异常，请重试。' }
}
async function send() {
  const message = draft.value.trim()
  if (!message || generating.value) return
  draft.value = ''; status.value = ''; messages.value.push(newMessage('user', message), newMessage('assistant', ''))
  generating.value = true; pendingDone = false; abortController = new AbortController()
  let requestTimedOut = false
  const requestTimeout = window.setTimeout(() => {
    requestTimedOut = true
    abortController?.abort()
  }, 35_000)
  try {
    const body: { sessionId: string; message: string; contextProductId?: number } = { sessionId: currentSessionId.value, message }
    if (props.contextProductId !== null) body.contextProductId = props.contextProductId
    const response = await fetch('/api/v1/chat/stream', { method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' }, body: JSON.stringify(body), signal: abortController.signal })
    if (!response.ok || !response.body) throw new Error('推荐服务暂时不可用，请稍后再试')
    const reader = response.body.getReader(); const decoder = new TextDecoder(); let buffer = ''
    while (true) {
      const { value, done } = await reader.read(); if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split(/\r?\n\r?\n/); buffer = blocks.pop() ?? ''
      blocks.forEach(handleEventBlock)
    }
    if (buffer.trim()) handleEventBlock(buffer)
  } catch (reason) {
    if (!(reason instanceof DOMException && reason.name === 'AbortError') || requestTimedOut) {
      status.value = requestTimedOut ? 'AI 导购回复超时，请稍后重试。预算结论已保留。' : reason instanceof Error ? reason.message : '推荐服务暂时不可用，请稍后再试'
      removeEmptyAssistantMessage()
      pendingDone = true; finishWhenTyped()
    }
  } finally {
    window.clearTimeout(requestTimeout)
    abortController = null
    if (!pendingDone) { pendingDone = true; finishWhenTyped() }
  }
}
function sendOnEnter(event: KeyboardEvent) {
  if (event.shiftKey || event.isComposing) return
  event.preventDefault()
  void send()
}
function stop() { abortController?.abort(); generating.value = false; pendingDone = false }
function close() { if (!generating.value) emit('close') }
function newConversation() {
  stop()
  if (typingTimer !== null) window.clearTimeout(typingTimer)
  typingTimer = null
  typingQueue = ''
  pendingDone = false
  draft.value = ''
  status.value = ''
  selectedProduct.value = null
  currentSessionId.value = createSessionId()
  messages.value = [welcomeMessage()]
  emit('newConversation')
}
function clampPanelSize(width: number, height: number) {
  return {
    width: Math.max(320, Math.min(width, Math.min(600, window.innerWidth - 44))),
    height: Math.max(420, Math.min(height, Math.floor(window.innerHeight * .8)))
  }
}
function constrainPanelSize() {
  panelSize.value = clampPanelSize(panelSize.value.width, panelSize.value.height)
  requestAnimationFrame(constrainPanelPosition)
}
function startResize(event: PointerEvent, direction: ResizeDirection) {
  if (event.button !== 0) return
  event.preventDefault()
  resizeOrigin = { x: event.clientX, y: event.clientY, width: panelSize.value.width, height: panelSize.value.height, direction }
  resizing.value = true
  document.body.classList.add('is-resizing-chat-widget')
  document.body.classList.add(`is-resizing-chat-widget-${direction}`)
  window.addEventListener('pointermove', resizePanel)
  window.addEventListener('pointerup', stopResize, { once: true })
}
function resizePanel(event: PointerEvent) {
  if (!resizeOrigin) return
  const nextWidth = resizeOrigin.direction === 'height' ? resizeOrigin.width : resizeOrigin.width + resizeOrigin.x - event.clientX
  const nextHeight = resizeOrigin.direction === 'width' ? resizeOrigin.height : resizeOrigin.height + resizeOrigin.y - event.clientY
  panelSize.value = clampPanelSize(nextWidth, nextHeight)
  expanded.value = false
}
function stopResize() {
  resizeOrigin = null
  resizing.value = false
  document.body.classList.remove('is-resizing-chat-widget')
  document.body.classList.remove('is-resizing-chat-widget-width', 'is-resizing-chat-widget-height', 'is-resizing-chat-widget-both')
  window.removeEventListener('pointermove', resizePanel)
}
function startDrag(event: PointerEvent) {
  if (event.button !== 0 || (event.target as HTMLElement).closest('button')) return
  event.preventDefault()
  dragOrigin = { x: event.clientX, y: event.clientY, offsetX: panelOffset.value.x, offsetY: panelOffset.value.y }
  dragging.value = true
  document.body.classList.add('is-dragging-chat-widget')
  window.addEventListener('pointermove', dragPanel)
  window.addEventListener('pointerup', stopDrag, { once: true })
}
function dragPanel(event: PointerEvent) {
  if (!dragOrigin) return
  panelOffset.value = clampPanelOffset(
    dragOrigin.offsetX + event.clientX - dragOrigin.x,
    dragOrigin.offsetY + event.clientY - dragOrigin.y
  )
}
function clampPanelOffset(x: number, y: number) {
  const panel = panelElement.value
  if (!panel) return { x, y }
  const rect = panel.getBoundingClientRect()
  const baseLeft = rect.left - panelOffset.value.x
  const baseTop = rect.top - panelOffset.value.y
  return {
    x: Math.max(12 - baseLeft, Math.min(x, window.innerWidth - 12 - rect.width - baseLeft)),
    y: Math.max(12 - baseTop, Math.min(y, window.innerHeight - 12 - rect.height - baseTop))
  }
}
function constrainPanelPosition() { panelOffset.value = clampPanelOffset(panelOffset.value.x, panelOffset.value.y) }
function stopDrag() {
  dragOrigin = null
  dragging.value = false
  document.body.classList.remove('is-dragging-chat-widget')
  window.removeEventListener('pointermove', dragPanel)
}
function toggleExpanded() {
  if (expanded.value) {
    panelSize.value = { ...defaultPanelSize }
    expanded.value = false
    return
  }
  panelSize.value = clampPanelSize(600, Math.floor(window.innerHeight * .8))
  expanded.value = true
}
onMounted(() => window.addEventListener('resize', constrainPanelSize))
onBeforeUnmount(() => {
  abortController?.abort()
  if (typingTimer !== null) window.clearTimeout(typingTimer)
  window.removeEventListener('resize', constrainPanelSize)
  stopResize()
  stopDrag()
})
</script>

<template>
  <div class="chat-widget-root" :style="rootStyle">
    <button type="button" class="chat-launcher" aria-controls="ai-shopping-guide" :aria-expanded="open" @click="emit('open')">通用导购咨询</button>
    <Transition name="chat-panel">
      <aside v-if="open" ref="panelElement" id="ai-shopping-guide" class="chat-widget" :class="{ 'is-resizing': resizing, 'is-dragging': dragging }" :style="widgetStyle" role="dialog" aria-modal="false" aria-label="通用导购咨询">
        <header class="widget-header" @pointerdown="startDrag"><div><p>通用导购咨询</p><strong>预算、用途和商品都可以问</strong><span v-if="contextHint">{{ contextHint }}</span></div><div class="widget-actions"><button type="button" class="widget-header-button" :disabled="generating" @click="newConversation">新对话</button><button type="button" class="widget-header-button" :aria-label="expanded ? '恢复默认窗口大小' : '展开聊天窗口'" @click="toggleExpanded">{{ expanded ? '恢复' : '展开' }}</button><button type="button" class="widget-close" aria-label="关闭通用导购咨询" :disabled="generating" @click="close">×</button></div></header>
        <section class="widget-messages" aria-live="polite">
          <article v-for="message in messages" :key="message.id" class="widget-message" :class="message.role">
            <p v-if="message.productSearchResult?.matchType === 'ALTERNATIVE'" class="widget-match-note">相关备选：{{ message.productSearchResult.reason }}</p>
            <p v-else-if="message.productSearchResult?.matchType === 'EXACT'" class="widget-match-note">商品库匹配：{{ message.productSearchResult.reason }}</p>
            <p class="widget-role">{{ message.role === 'user' ? '你' : '导购助手' }}</p>
            <p v-if="message.content" class="widget-message-text">{{ message.content }}<span v-if="generating && message === assistantMessage()" class="widget-cursor">|</span></p>
            <div v-if="message.products.length" class="widget-product-grid"><article v-for="product in message.products" :key="product.id" class="widget-product-card"><p>{{ product.brand }} · {{ product.category }}</p><h3>{{ product.name }}</h3><strong>¥{{ Number(product.price).toLocaleString('zh-CN') }}</strong><small>{{ [product.cpu, product.gpu].filter(Boolean).join(' · ') }}</small><button type="button" @click="selectedProduct = product">提交咨询</button></article></div>
            <section v-if="message.budgetResult" class="budget-result-card" aria-label="预算可行性结论">
              <p class="budget-result-title">预算可行性</p>
              <dl>
                <div><dt>当前预算</dt><dd>{{ currency(message.budgetResult.budget) }}</dd></div>
                <div><dt>最低可行总价</dt><dd>{{ currency(message.budgetResult.minimumFeasibleTotal) }}</dd></div>
                <div v-if="!message.budgetResult.hasWithinBudgetPlan"><dt>还差</dt><dd>{{ currency(message.budgetResult.budgetGap) }}</dd></div>
              </dl>
              <p v-if="!message.budgetResult.hasWithinBudgetPlan" class="budget-result-note">当前商品库没有预算内的完整独显方案。</p>
              <template v-else-if="message.budgetResult.candidate">
                <p class="budget-result-note">当前预算内的最低价基础兼容独显方案，点击后才会填入装机单。</p>
                <ul class="budget-product-list">
                  <li v-for="product in message.budgetResult.candidate.products" :key="product.id"><span>{{ buildSlotLabel(product.productType) }}</span><b>{{ product.name }}</b><strong>{{ currency(product.price) }}</strong></li>
                </ul>
                <button type="button" class="budget-apply-button" @click="emit('applyBudgetBuild', message.budgetResult.candidate)">一键填入装机单</button>
              </template>
            </section>
          </article>
        </section>
        <p v-if="status" class="widget-status" role="alert">{{ status }}</p>
        <form class="widget-composer" @submit.prevent="send"><textarea v-model="draft" :disabled="generating" maxlength="500" rows="3" placeholder="例如：预算 6000，主要剪视频，偶尔玩 3A 游戏" @keydown.enter="sendOnEnter" /><div><span>{{ draft.length }}/500</span><button v-if="generating" type="button" class="widget-stop" @click="stop">停止生成</button><button v-else type="submit" :disabled="!draft.trim()">发送</button></div></form>
        <button type="button" class="widget-resize-handle widget-resize-width" aria-label="拖拽调整聊天窗口宽度" title="拖拽调整宽度" @pointerdown="startResize($event, 'width')" />
        <button type="button" class="widget-resize-handle widget-resize-height" aria-label="拖拽调整聊天窗口高度" title="拖拽调整高度" @pointerdown="startResize($event, 'height')" />
        <button type="button" class="widget-resize-handle widget-resize-corner" aria-label="拖拽调整聊天窗口大小" title="拖拽调整窗口大小" @pointerdown="startResize($event, 'both')" />
      </aside>
    </Transition>
  </div>
  <LeadModal :product="selectedProduct" @close="selectedProduct = null" @submitted="status = '已收到咨询，我们会尽快联系你。'" />
</template>

<style src="../chat-widget.css"></style>
