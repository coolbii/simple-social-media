<script setup lang="ts">
import { RouterView } from 'vue-router';
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">Nx + Vue 3 + Spring Boot</p>
        <a class="brand" href="/"> Simple Social Media </a>
      </div>
      <p class="tagline">Initial scaffold workspace</p>
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
  display: grid;
  gap: 1rem;
  align-items: center;
  grid-template-columns: 1fr;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 250, 245, 0.84);
  backdrop-filter: blur(18px);
}

.eyebrow {
  margin: 0 0 0.25rem;
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #b45309;
}

.brand {
  color: #111827;
  font-size: clamp(1.5rem, 2vw, 2rem);
  font-weight: 700;
  text-decoration: none;
}

.tagline {
  margin: 0;
  color: #475569;
  font-weight: 500;
}

.page-shell {
  width: min(1120px, calc(100vw - 2rem));
  margin: 0 auto;
  padding: 2rem 0 4rem;
}

@media (min-width: 900px) {
  .topbar {
    grid-template-columns: 1fr auto;
  }
}
</style>
