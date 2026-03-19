<template>
  <div>
    <header class="topbar">
      <div class="brand">
        <img class="brand-logo" :src="logoUrl" alt="好美味 logo" />
        <span>好美味</span>
      </div>
      <nav>
        <RouterLink to="/">点餐</RouterLink>
      </nav>
    </header>
    <main class="container">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import logoUrl from './assets/haomei-logo.svg'
</script>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: #111827;
  color: #fff;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  letter-spacing: 1px;
}
.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}
nav a {
  color: #e5e7eb;
  margin-left: 14px;
  text-decoration: none;
}
nav a.router-link-active {
  color: #ffffff;
  border-bottom: 2px solid #60a5fa;
  padding-bottom: 4px;
}
.container {
  padding: 18px;
}
</style>

