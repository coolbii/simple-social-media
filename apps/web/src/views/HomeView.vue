<script setup lang="ts"></script>

<template>
  <section class="hero-card">
    <p class="section-label">Welcome</p>
    <h1>Simple Social Media Scaffold</h1>
    <p class="lead">
      Frontend and backend scaffolding are ready. Feature implementation will be added in follow-up commits.
    </p>
    <ul class="checklist">
      <li>Vue 3 + Vite + Pinia project setup</li>
      <li>Spring Boot API project setup</li>
      <li>Nx workspace layout and targets</li>
    </ul>
  </section>
</template>

<style scoped>
.hero-card {
  margin-bottom: 1.5rem;
  padding: 2rem;
  border-radius: 1.5rem;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 30px 60px rgba(148, 163, 184, 0.16);
  display: grid;
  gap: 1rem;
}

.section-label {
  margin: 0 0 0.5rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.75rem;
  color: #b45309;
}

h1 {
  margin: 0;
  color: #111827;
}

.lead {
  color: #475569;
  line-height: 1.7;
}

.checklist {
  margin: 0;
  padding-left: 1.25rem;
  color: #1f2937;
  line-height: 1.7;
}
</style>
