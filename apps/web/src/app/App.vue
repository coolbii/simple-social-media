<script setup lang="ts">
import { useRouter, RouterView } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const router = useRouter();

async function handleLogout() {
  await auth.signOut();
  await router.push('/login');
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <a class="brand" href="/"> Simple Social Media </a>

      <div v-if="auth.isAuthenticated" class="topbar-user">
        <span class="user-name">{{ auth.currentUser?.userName }}</span>
        <button class="logout-btn" type="button" @click="handleLogout">登出</button>
      </div>
      <RouterLink v-else-if="auth.status === 'anonymous'" class="login-link" to="/login"> 登入 / 註冊 </RouterLink>
    </header>

    <main class="page-shell">
      <RouterView />
    </main>
  </div>
</template>

<style scoped lang="css">
.app-shell {
  min-height: 100vh;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 250, 245, 0.9);
  backdrop-filter: blur(18px);
}

.brand {
  color: #111827;
  font-size: clamp(1.1rem, 2vw, 1.4rem);
  font-weight: 700;
  text-decoration: none;
}

.topbar-user {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-name {
  font-weight: 600;
  color: #1e293b;
}

.logout-btn {
  padding: 0.4rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.4);
  border-radius: 999px;
  background: transparent;
  color: #475569;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.logout-btn:hover {
  background: #f1f5f9;
}

.login-link {
  padding: 0.4rem 1.1rem;
  border-radius: 999px;
  background: #1d4ed8;
  color: white;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  transition: background 0.15s;
}

.login-link:hover {
  background: #1e40af;
}

.page-shell {
  width: min(1120px, calc(100vw - 2rem));
  margin: 0 auto;
  padding: 2rem 0 4rem;
}
</style>
