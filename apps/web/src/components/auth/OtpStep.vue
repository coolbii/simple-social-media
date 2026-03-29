<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue';

const props = withDefaults(defineProps<{ phoneNumber: string; loading?: boolean }>(), {
  loading: false,
});
const emit = defineEmits<{
  verified: [registrationToken: string];
  back: [];
}>();

const code = ref('');
const error = ref('');
const countdown = ref(300);
const isDirty = computed(() => code.value.trim().length > 0);

defineExpose({
  setError: (msg: string) => {
    error.value = msg;
  },
  isDirty: () => isDirty.value,
});

const timer = setInterval(() => {
  if (countdown.value > 0) countdown.value--;
}, 1000);
onUnmounted(() => clearInterval(timer));

const formattedCountdown = () => {
  const m = Math.floor(countdown.value / 60)
    .toString()
    .padStart(2, '0');
  const s = (countdown.value % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
};

function handleSubmit() {
  error.value = '';
  if (props.loading) {
    return;
  }
  emit('verified', code.value);
}
</script>

<template>
  <form class="auth-form" @submit.prevent="handleSubmit">
    <p class="step-hint">
      驗證碼已發送至 <strong>{{ props.phoneNumber }}</strong
      >，請於 <span class="countdown">{{ formattedCountdown() }}</span> 內輸入。
    </p>

    <label>
      <span>驗證碼</span>
      <input
        v-model="code"
        type="text"
        inputmode="numeric"
        maxlength="6"
        placeholder="6 位數字"
        autocomplete="one-time-code"
        :disabled="props.loading"
        required
      />
    </label>

    <p v-if="error" class="field-error">{{ error }}</p>

    <button class="submit-btn" :disabled="props.loading || countdown === 0" type="submit">
      {{ props.loading ? '驗證中…' : '確認驗證碼' }}
    </button>

    <button class="link-btn" :disabled="props.loading" type="button" @click="emit('back')">← 重新輸入手機號碼</button>
  </form>
</template>
