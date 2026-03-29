<script setup lang="ts">
import { computed, ref } from 'vue';

const props = withDefaults(
  defineProps<{
    loading?: boolean;
  }>(),
  {
    loading: false,
  }
);

const emit = defineEmits<{
  sent: [phoneNumber: string];
}>();

const phoneNumber = ref('');
const error = ref('');
const isDirty = computed(() => phoneNumber.value.trim().length > 0);

defineExpose({
  setError: (msg: string) => {
    error.value = msg;
  },
  isDirty: () => isDirty.value,
});

function handleSubmit() {
  error.value = '';
  if (props.loading) {
    return;
  }
  emit('sent', phoneNumber.value);
}
</script>

<template>
  <form class="auth-form" @submit.prevent="handleSubmit">
    <p class="step-hint">輸入手機號碼，我們將發送驗證碼給您。</p>

    <label>
      <span>手機號碼</span>
      <input
        v-model="phoneNumber"
        type="tel"
        placeholder="0912345678"
        autocomplete="tel"
        :disabled="props.loading"
        required
      />
    </label>

    <p v-if="error" class="field-error">{{ error }}</p>

    <button class="submit-btn" :disabled="props.loading" type="submit">
      {{ props.loading ? '發送中…' : '發送驗證碼' }}
    </button>
  </form>
</template>
