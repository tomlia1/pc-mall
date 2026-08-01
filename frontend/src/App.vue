<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import LeadModal from './components/LeadModal.vue'
import type { ChatMessage, Product } from './types'

interface StreamEvent { event: string; data: unknown }

const messages = ref<ChatMessage[]>([{ id: 'welcome', role: 'assistant', content: '你好，我可以按预算、用途和偏好为你推荐电脑。比如：预算 6000，主要剪视频，也想偶尔玩游戏。', products: [] }])
const draft = ref('')
const generating = ref(false)
const status = ref('')
const selectedProduct = ref<Product | null>(null)
let abortController: AbortController | null = null
let typingTimer: number | null = null
let typingQueue = ''
let pendingDone = false

function sessionId(): string {
  const key = 'pc-mall-chat-session-id'
  let id = localStorage.getItem(key)
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem(key, id)
  }
  return id
}

function newMessage(role: ChatMessage['role'], content: string): ChatMessage {
  return { id: crypto.randomUUID(), role, content, products: [] }
}

function parseEvent(block: string): StreamEvent | null {
  const lines = block.split(/\r?\n/)
  const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim()
  const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).trim()).join('\n')
  if (!event || !data) return null
  return { event, data: JSON.parse(data) }
}

function assistantMessage(): ChatMessage | undefined {
  return [...messages.value].reverse().find(message => message.role === 'assistant')
}

function enqueueText(text: string) {
  typingQueue += text
  if (typingTimer !== null) return
  const typeNext = () => {
    const message = assistantMessage()
    if (!message || !typingQueue) {
      typingTimer = null
      finishWhenTyped()
      return
    }
    const [character, ...rest] = Array.from(typingQueue)
    message.content += character
    typingQueue = rest.join('')
    typingTimer = window.setTimeout(typeNext, 14)
  }
  typeNext()
}

function finishWhenTyped() {
  if (pendingDone && !typingQueue) {
    generating.value = false
    pendingDone = false
  }
}

async function send() {
  const message = draft.value.trim()
  if (!message || generating.value) return
  draft.value = ''
  status.value = ''
  messages.value.push(newMessage('user', message), newMessage('assistant', ''))
  generating.value = true
  pendingDone = false
  abortController = new AbortController()
  try {
    const response = await fetch('/api/v1/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
      body: JSON.stringify({ sessionId: sessionId(), message }),
      signal: abortController.signal
    })
    if (!response.ok || !response.body) throw new Error('推荐服务暂时不可用，请稍后再试')
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() ?? ''
      blocks.forEach(handleEventBlock)
    }
    if (buffer.trim()) handleEventBlock(buffer)
  } catch (reason) {
    if (reason instanceof DOMException && reason.name === 'AbortError') return
    status.value = reason instanceof Error ? reason.message : '推荐服务暂时不可用，请稍后再试'
    pendingDone = true
    finishWhenTyped()
  } finally {
    abortController = null
  }
}

function handleEventBlock(block: string) {
  try {
    const streamEvent = parseEvent(block)
    if (!streamEvent) return
    if (streamEvent.event === 'message') enqueueText((streamEvent.data as { text: string }).text)
    if (streamEvent.event === 'product') assistantMessage()?.products.push(streamEvent.data as Product)
    if (streamEvent.event === 'error') status.value = (streamEvent.data as { message: string }).message
    if (streamEvent.event === 'done') {
      pendingDone = true
      finishWhenTyped()
    }
  } catch {
    status.value = '收到的推荐数据格式异常，请重试。'
  }
}

function stop() {
  abortController?.abort()
  const message = assistantMessage()
  if (message) message.stopped = true
  generating.value = false
  pendingDone = false
  status.value = '已停止生成。'
}

function onLeadSubmitted() {
  status.value = '已收到咨询，我们会尽快联系你。'
}

onBeforeUnmount(() => {
  abortController?.abort()
  if (typingTimer !== null) window.clearTimeout(typingTimer)
})
</script>

<template>
  <main class="chat-shell">
    <header><div><p class="eyebrow">PC MALL</p><h1>智能电脑导购</h1></div><p>告诉我预算和用途，商品事实来自实时库存数据。</p></header>
    <section class="conversation" aria-live="polite">
      <article v-for="message in messages" :key="message.id" class="message" :class="message.role">
        <p class="role">{{ message.role === 'user' ? '你' : '导购助手' }}</p>
        <p v-if="message.content" class="message-text">{{ message.content }}<span v-if="generating && message === assistantMessage()" class="cursor">▋</span></p>
        <p v-if="message.stopped" class="stopped">已停止本次生成</p>
        <div v-if="message.products.length" class="product-grid">
          <article v-for="product in message.products" :key="product.id" class="product-card">
            <p class="brand">{{ product.brand }} · {{ product.category }}</p><h2>{{ product.name }}</h2><strong>¥{{ Number(product.price).toLocaleString('zh-CN') }}</strong>
            <p class="spec">{{ [product.cpu, product.gpu, product.memoryGb && `${product.memoryGb}GB 内存`, product.storageGb && `${product.storageGb}GB 存储`].filter(Boolean).join(' · ') }}</p>
            <p v-if="product.tags" class="tags">{{ product.tags }}</p>
            <button type="button" @click="selectedProduct = product">咨询这款</button>
          </article>
        </div>
      </article>
    </section>
    <p v-if="status" class="status">{{ status }}</p>
    <form class="composer" @submit.prevent="send"><textarea v-model="draft" :disabled="generating" maxlength="500" rows="3" placeholder="例如：预算 6000，主要剪视频，偶尔玩 3A 游戏"></textarea><div><span>{{ draft.length }}/500</span><button v-if="generating" class="stop" type="button" @click="stop">停止生成</button><button v-else class="send" type="submit" :disabled="!draft.trim()">发送</button></div></form>
  </main>
  <LeadModal :product="selectedProduct" @close="selectedProduct = null" @submitted="onLeadSubmitted" />
</template>
