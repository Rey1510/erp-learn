<script setup lang="ts">
const { initTheme, theme } = useTheme()

onMounted(() => {
  initTheme()
})
</script>

<template>
  <div 
    id="app-root"
    class="min-h-screen w-full font-sans antialiased selection:bg-indigo-500 selection:text-white transition-colors duration-200"
    :class="theme === 'light' ? 'bg-slate-100 text-slate-900' : 'bg-slate-950 text-slate-100'"
    :style="theme === 'light' ? 'background-color: #f1f5f9; min-height: 100vh;' : 'background-color: #020617; min-height: 100vh;'"
  >
    <!-- Navbar Component -->
    <TheNavbar />

    <!-- Main Container dengan Dynamic Page Router -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <NuxtPage />
    </main>
  </div>
</template>
