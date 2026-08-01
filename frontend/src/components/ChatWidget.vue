<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import LeadModal from './LeadModal.vue'
import type { ChatMessage, Product } from '../types'

interface StreamEvent { event: string; data: unknown }

const props = defineProps<{ open: boolean; contextProductId: number | null }>()
const emit = defineEmits<{ open: []; close: [] }>()
const messages = ref<ChatMessage[]>([{ id: 'welcome', role: 'assistant', content: '你好，我可以按预算、用途和偏好为你推荐电脑。告诉我你想用电脑做什么吧。', products: [] }])
const draft = ref('')
const generating = ref(false)
const status = ref('')
const selectedProduct = ref<Product | null>(null)
let abortController: AbortController | null = null
let typingTimer: number | null = null
let typingQueue = ''
let pendingDone = false

const contextHint = computed(() => props.contextProductId === null ? '' : '正在咨询当前查看的商品')

function sessionId(): string {
  const key = 'pc-mall-chat-session-id'
  let id = localStorage.getItem(key)
  if (!id) { id = crypto.randomUUID(); localStorage.setItem(key, id) }
  return id
}
function newMessage(role: ChatMessage['role'], content: string): ChatMessage { return { id: crypto.randomUUID(), role, content, products: [] } }
function parseEvent(block: string): StreamEvent | null {
  const lines = block.split(/\r?\n/)
  const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim()
  const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).trim()).join('\n')
  return event && data ? { event, data: JSON.parse(data) } : null
}
function assistantMessage(): ChatMessage | undefined { return [...messages.value].reverse().find(message => message.role === 'assistant') }
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
    if (streamEvent.event === 'message') enqueueText((streamEvent.data as { text: string }).text)
    if (streamEvent.event === 'product') assistantMessage()?.products.push(streamEvent.data as Product)
    if (streamEvent.event === 'error') status.value = (streamEvent.data as { message: string }).message
    if (streamEvent.event === 'done') { pendingDone = true; finishWhenTyped() }
  } catch { status.value = '收到的推荐数据格式异常，请重试。' }
}
async function send() {
  const message = draft.value.trim()
  if (!message || generating.value) return
  draft.value = ''; status.value = ''; messages.value.push(newMessage('user', message), newMessage('assistant', ''))
  generating.value = true; pendingDone = false; abortController = new AbortController()
  try {
    const body: { sessionId: string; message: string; contextProductId?: number } = { sessionId: sessionId(), message }
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
    if (!(reason instanceof DOMException && reason.name === 'AbortError')) {
      status.value = reason instanceof Error ? reason.message : '推荐服务暂时不可用，请稍后再试'
      pendingDone = true; finishWhenTyped()
    }
  } finally { abortController = null }
}
function stop() { abortController?.abort(); generating.value = false; pendingDone = false }
function close() { if (!generating.value) emit('close') }
onBeforeUnmount(() => { abortController?.abort(); if (typingTimer !== null) window.clearTimeout(typingTimer) })
</script>

<template>
  <div class="chat-widget-root">
    <button type="button" class="chat-launcher" aria-controls="ai-shopping-guide" :aria-expanded="open" @click="emit('open')">AI 导购咨询</button>
    <Transition name="chat-panel">
      <aside v-if="open" id="ai-shopping-guide" class="chat-widget" role="dialog" aria-modal="false" aria-label="AI 导购咨询">
        <header class="widget-header"><div><p>AI 导购</p><strong>选电脑，问我就行</strong><span v-if="contextHint">{{ contextHint }}</span></div><button type="button" class="widget-close" aria-label="关闭 AI 导购" :disabled="generating" @click="close">×</button></header>
        <section class="widget-messages" aria-live="polite">
          <article v-for="message in messages" :key="message.id" class="widget-message" :class="message.role">
            <p class="widget-role">{{ message.role === 'user' ? '你' : '导购助手' }}</p>
            <p v-if="message.content" class="widget-message-text">{{ message.content }}<span v-if="generating && message === assistantMessage()" class="widget-cursor">|</span></p>
            <div v-if="message.products.length" class="widget-product-grid"><article v-for="product in message.products" :key="product.id" class="widget-product-card"><p>{{ product.brand }} · {{ product.category }}</p><h3>{{ product.name }}</h3><strong>¥{{ Number(product.price).toLocaleString('zh-CN') }}</strong><small>{{ [product.cpu, product.gpu].filter(Boolean).join(' · ') }}</small><button type="button" @click="selectedProduct = product">提交咨询</button></article></div>
          </article>
        </section>
        <p v-if="status" class="widget-status" role="alert">{{ status }}</p>
        <form class="widget-composer" @submit.prevent="send"><textarea v-model="draft" :disabled="generating" maxlength="500" rows="3" placeholder="例如：预算 6000，主要剪视频，偶尔玩 3A 游戏" /><div><span>{{ draft.length }}/500</span><button v-if="generating" type="button" class="widget-stop" @click="stop">停止生成</button><button v-else type="submit" :disabled="!draft.trim()">发送</button></div></form>
      </aside>
    </Transition>
  </div>
  <LeadModal :product="selectedProduct" @close="selectedProduct = null" @submitted="status = '已收到咨询，我们会尽快联系你。'" />
</template>

<style src="../chat-widget.css"></style>
