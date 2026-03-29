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
  submit: [phoneNumber: string, password: string];
}>();

const phoneNumber = ref('');
const password = ref('');
const error = ref('');
const isDirty = computed(() => phoneNumber.value.trim().length > 0 || password.value.length > 0);

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
  emit('submit', phoneNumber.value, password.value);
}
</script>

<template>
  <form class="auth-form" @submit.prevent="handleSubmit">
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

    <label>
      <span>密碼</span>
      <input v-model="password" type="password" autocomplete="current-password" :disabled="props.loading" required />
    </label>

    <p v-if="error" class="field-error">{{ error }}</p>

    <button class="submit-btn" :disabled="props.loading" type="submit">
      {{ props.loading ? '登入中…' : '登入' }}
    </button>
  </form>
</template>
