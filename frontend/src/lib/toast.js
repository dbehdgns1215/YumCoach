import { ref } from 'vue'

export const toasts = ref([])
let idCounter = 1

export function showToast(message, type = 'error', duration = 4000) {
  const id = idCounter++
  toasts.value.push({ id, message, type })
  if (duration > 0) {
    setTimeout(() => {
      removeToast(id)
    }, duration)
  }
}

export function removeToast(id) {
  toasts.value = toasts.value.filter(t => t.id !== id)
}
