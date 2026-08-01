<script setup lang="ts">
import { ref, watch } from 'vue'

const fallback = '/assets/images/default-laptop.svg'
const props = defineProps<{ src?: string; alt: string }>()
const source = ref(props.src || fallback)

watch(() => props.src, value => {
  source.value = value || fallback
})

function useFallback() {
  if (source.value !== fallback) source.value = fallback
}
</script>

<template>
  <img :src="source" :alt="alt" @error="useFallback" />
</template>
