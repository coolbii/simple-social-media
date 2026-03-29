<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { onBeforeRouteLeave, useRouter } from 'vue-router';
import LoginForm from '../components/auth/LoginForm.vue';
import RegisterFlow from '../components/auth/RegisterFlow.vue';
import { useAuthStore } from '../stores/auth';

type Tab = 'login' | 'register';

const tab = ref<Tab>('login');
const auth = useAuthStore();
const router = useRouter();
const leaveConfirmMessage = '目前表單尚未送出，離開後輸入資料會遺失。確定要離開嗎？';
const loginSubmitting = ref(false);
const allowDirectLeave = ref(false);

const loginFormRef = ref<InstanceType<typeof LoginForm> | null>(null);
const registerFlowRef = ref<InstanceType<typeof RegisterFlow> | null>(null);

function hasDirtyInput() {
  if (tab.value === 'login') {
    return loginFormRef.value?.isDirty?.() === true;
  }
  return registerFlowRef.value?.isDirty?.() === true;
}

function shouldAllowLeave() {
  if (allowDirectLeave.value) {
    return true;
  }
  if (!hasDirtyInput()) {
    return true;
  }
  return window.confirm(leaveConfirmMessage);
}

async function onLogin(loginId: string, password: string) {
  loginSubmitting.value = true;
  try {
    await auth.signIn(loginId, password);
    allowDirectLeave.value = true;
    await router.push('/');
  } catch {
    loginFormRef.value?.setError('手機號碼 / Email 或密碼錯誤，請再試一次。');
  } finally {
    loginSubmitting.value = false;
  }
}

async function onRegisterCompleted() {
  allowDirectLeave.value = true;
  await router.push('/');
}

function switchTab(nextTab: Tab) {
  if (tab.value === nextTab) {
    return;
  }
  if (!shouldAllowLeave()) {
    return;
  }
  tab.value = nextTab;
}

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (allowDirectLeave.value || !hasDirtyInput()) {
    return;
  }
  event.preventDefault();
  event.returnValue = '';
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload);
});

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
});

onBeforeRouteLeave(() => shouldAllowLeave());
</script>

<template>
  <section class="auth-shell">
    <div class="auth-card">
      <p class="auth-eyebrow">Simple Social Media</p>
      <h1 class="auth-title">{{ tab === 'login' ? '歡迎回來' : '建立帳號' }}</h1>

      <div class="tab-toggle" role="tablist">
        <button
          role="tab"
          :aria-selected="tab === 'login'"
          :class="{ active: tab === 'login' }"
          type="button"
          @click="switchTab('login')"
        >
          登入
        </button>
        <button
          role="tab"
          :aria-selected="tab === 'register'"
          :class="{ active: tab === 'register' }"
          type="button"
          @click="switchTab('register')"
        >
          註冊
        </button>
      </div>

      <LoginForm v-if="tab === 'login'" ref="loginFormRef" :loading="loginSubmitting" @submit="onLogin" />
      <RegisterFlow v-else ref="registerFlowRef" @completed="onRegisterCompleted" />
    </div>
  </section>
</template>

<style scoped>
.auth-shell {
  display: grid;
  place-items: center;
  min-height: 72vh;
}

.auth-card {
  width: min(100%, 30rem);
  padding: 2.25rem 2rem;
  border-radius: 1.75rem;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 32px 70px rgba(59, 130, 246, 0.11);
}

.auth-eyebrow {
  margin: 0 0 0.4rem;
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #2563eb;
}

.auth-title {
  margin: 0 0 1.5rem;
  font-size: 1.75rem;
  font-weight: 700;
  color: #0f172a;
}

.tab-toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-bottom: 1.75rem;
  padding: 0.3rem;
  border-radius: 999px;
  background: #f1f5f9;
}

.tab-toggle button {
  padding: 0.6rem 1rem;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #64748b;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition:
    background 0.18s,
    color 0.18s;
}

.tab-toggle button.active {
  background: white;
  color: #1d4ed8;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}
</style>
