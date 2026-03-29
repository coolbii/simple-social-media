<script setup lang="ts">
import { ref, shallowRef } from 'vue';
import { useAuthStore } from '../../stores/auth';
import OtpStep from './OtpStep.vue';
import PhoneStep from './PhoneStep.vue';
import ProfileStep from './ProfileStep.vue';

type Step = 'phone' | 'otp' | 'profile';

const emit = defineEmits<{
  completed: [];
}>();

const auth = useAuthStore();

const step = ref<Step>('phone');
const phoneNumber = ref('');
const registrationToken = ref('');
const sendingCode = ref(false);
const verifyingCode = ref(false);
const creatingAccount = ref(false);

const phoneRef = shallowRef<InstanceType<typeof PhoneStep> | null>(null);
const otpRef = shallowRef<InstanceType<typeof OtpStep> | null>(null);
const profileRef = shallowRef<InstanceType<typeof ProfileStep> | null>(null);

function extractMessage(err: unknown, fallback: string): string {
  if (err instanceof Error) return err.message;
  return fallback;
}

async function onPhoneSent(phone: string) {
  sendingCode.value = true;
  try {
    await auth.sendVerificationCode(phone);
    phoneNumber.value = phone;
    step.value = 'otp';
  } catch (err) {
    phoneRef.value?.setError(extractMessage(err, '發送驗證碼失敗，請再試一次。'));
  } finally {
    sendingCode.value = false;
  }
}

async function onOtpVerified(code: string) {
  verifyingCode.value = true;
  try {
    const result = await auth.verifyPhone(phoneNumber.value, code);
    if (!result.registrationToken) throw new Error('伺服器未回傳驗證令牌。');
    registrationToken.value = result.registrationToken;
    step.value = 'profile';
  } catch (err) {
    otpRef.value?.setError(extractMessage(err, '驗證碼錯誤或已過期，請再試一次。'));
  } finally {
    verifyingCode.value = false;
  }
}

async function onProfileSubmit(payload: { userName: string; email: string; password: string }) {
  creatingAccount.value = true;
  try {
    await auth.signUpAndIn({
      registrationToken: registrationToken.value,
      phoneNumber: phoneNumber.value,
      userName: payload.userName,
      password: payload.password,
      email: payload.email || undefined,
    });
    emit('completed');
  } catch (err) {
    profileRef.value?.setError(extractMessage(err, '建立帳號失敗，請再試一次。'));
  } finally {
    creatingAccount.value = false;
  }
}

function isDirty() {
  return (
    phoneNumber.value.trim().length > 0 ||
    registrationToken.value.trim().length > 0 ||
    phoneRef.value?.isDirty?.() === true ||
    otpRef.value?.isDirty?.() === true ||
    profileRef.value?.isDirty?.() === true
  );
}

defineExpose({
  isDirty,
});
</script>

<template>
  <div>
    <div class="step-indicator">
      <span :class="['dot', { active: step === 'phone', done: step !== 'phone' }]">1</span>
      <span class="line" />
      <span :class="['dot', { active: step === 'otp', done: step === 'profile' }]">2</span>
      <span class="line" />
      <span :class="['dot', { active: step === 'profile' }]">3</span>
    </div>

    <PhoneStep v-if="step === 'phone'" ref="phoneRef" :loading="sendingCode" @sent="onPhoneSent" />
    <OtpStep
      v-else-if="step === 'otp'"
      ref="otpRef"
      :phone-number="phoneNumber"
      :loading="verifyingCode"
      @verified="onOtpVerified"
      @back="step = 'phone'"
    />
    <ProfileStep v-else ref="profileRef" :loading="creatingAccount" @submit="onProfileSubmit" />
  </div>
</template>

<style scoped>
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 1.75rem;
}

.dot {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.8rem;
  font-weight: 700;
  background: #e2e8f0;
  color: #94a3b8;
  transition:
    background 0.2s,
    color 0.2s;
}

.dot.active {
  background: #1d4ed8;
  color: white;
}

.dot.done {
  background: #16a34a;
  color: white;
}

.line {
  flex: 1;
  max-width: 3rem;
  height: 2px;
  background: #e2e8f0;
}
</style>
