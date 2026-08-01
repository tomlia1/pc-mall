<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { submitLead } from '../api'
import type { LeadPayload, Product } from '../types'

const props = defineProps<{ product: Product | null }>()
const emit = defineEmits<{ close: []; submitted: [] }>()

const contactType = ref<LeadPayload['contactType']>('phone')
const contactValue = ref('')
const userBudget = ref<number | undefined>()
const userRemark = ref('')
const privacyAgreed = ref(false)
const error = ref('')
const submitting = ref(false)
const visible = computed(() => props.product !== null)

watch(() => props.product, () => {
  contactValue.value = ''
  userBudget.value = undefined
  userRemark.value = ''
  privacyAgreed.value = false
  error.value = ''
})

async function submit() {
  if (!props.product || submitting.value) return
  if (!privacyAgreed.value) {
    error.value = '请先同意隐私说明。'
    return
  }
  submitting.value = true
  error.value = ''
  try {
    await submitLead({
      productId: props.product.id,
      contactType: contactType.value,
      contactValue: contactValue.value.trim(),
      privacyAgreed: privacyAgreed.value,
      userBudget: userBudget.value,
      userRemark: userRemark.value.trim() || undefined
    })
    emit('submitted')
    emit('close')
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '提交失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
    <section class="lead-modal" role="dialog" aria-modal="true" aria-labelledby="lead-title">
      <button class="close" type="button" aria-label="关闭" @click="emit('close')">×</button>
      <p class="eyebrow">预约咨询</p>
      <h2 id="lead-title">咨询 {{ product?.name }}</h2>
      <p class="subtle">留下联系方式，顾问会按你的需求提供购买建议。</p>
      <form @submit.prevent="submit">
        <label>联系方式类型
          <select v-model="contactType"><option value="phone">手机号</option><option value="weixin">微信号</option><option value="email">邮箱</option></select>
        </label>
        <label>联系方式
          <input v-model="contactValue" required maxlength="128" autocomplete="off" placeholder="请填写联系方式" />
        </label>
        <label>预算（可选）
          <input v-model.number="userBudget" type="number" min="0" step="100" placeholder="例如 6000" />
        </label>
        <label>补充需求（可选）
          <textarea v-model="userRemark" maxlength="500" rows="3" placeholder="例如主要玩哪些游戏"></textarea>
        </label>
        <label class="privacy"><input v-model="privacyAgreed" type="checkbox" /> 我已阅读并同意仅将联系方式用于本次咨询。</label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="primary" type="submit" :disabled="submitting">{{ submitting ? '提交中…' : '提交咨询' }}</button>
      </form>
    </section>
  </div>
</template>
