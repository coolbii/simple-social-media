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
  submit: [payload: { userName: string; email: string; password: string }];
}>();

const userName = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const error = ref('');
const isDirty = computed(
  () =>
    userName.value.trim().length > 0 ||
    email.value.trim().length > 0 ||
    password.value.length > 0 ||
    confirmPassword.value.length > 0
);

defineExpose({
  setError: (msg: string) => {
    error.value = msg;
  },
  isDirty: () => isDirty.value,
});

function handleSubmit() {
  if (password.value !== confirmPassword.value) {
    error.value = '兩次輸入的密碼不一致。';
    return;
  }
  error.value = '';
  if (props.loading) {
    return;
  }
  emit('submit', { userName: userName.value, email: email.value, password: password.value });
}
</script>

<template>
  <form class="auth-form" @submit.prevent="handleSubmit">
    <p class="step-hint">手機驗證成功！請設定您的帳號資料。</p>

    <label>
      <span>使用者名稱</span>
      <input
        v-model="userName"
        type="text"
        placeholder="例：Brian"
        autocomplete="username"
        :disabled="props.loading"
        required
      />
    </label>

    <label>
      <span>電子郵件 <span class="optional">（選填）</span></span>
      <input
        v-model="email"
        type="email"
        placeholder="you@example.com"
        autocomplete="email"
        :disabled="props.loading"
      />
    </label>

    <label>
      <span>密碼</span>
      <input
        v-model="password"
        type="password"
        autocomplete="new-password"
        minlength="8"
        :disabled="props.loading"
        required
      />
    </label>

    <label>
      <span>確認密碼</span>
      <input v-model="confirmPassword" type="password" autocomplete="new-password" :disabled="props.loading" required />
    </label>

    <p v-if="error" class="field-error">{{ error }}</p>

    <button class="submit-btn" :disabled="props.loading" type="submit">
      {{ props.loading ? '建立帳號中…' : '建立帳號' }}
    </button>
  </form>
</template>
