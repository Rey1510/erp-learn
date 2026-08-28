<script setup lang="ts">
const route = useRoute()
const { user, isAuthenticated, isAdmin, isCashier, logout } = useAuth()
const { t, locale, setLocale } = useI18n()
const { theme, toggleTheme, isDark } = useTheme()

withDefaults(defineProps<{
  hasError?: boolean
}>(), {
  hasError: false
})
</script>

<template>
  <header 
    v-if="route.path !== '/login'" 
    class="border-b sticky top-0 z-30 transition-colors duration-200 backdrop-blur-md"
    :class="theme === 'light' ? 'bg-white/80 border-slate-200 shadow-sm' : 'bg-slate-900/60 border-slate-800/80'"
  >
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
      <!-- Logo & Brand -->
      <div class="flex items-center gap-6">
        <NuxtLink to="/" class="flex items-center gap-3 group">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center font-bold text-white shadow-lg shadow-indigo-500/20 group-hover:scale-105 transition-transform">
            M
          </div>
          <div>
            <h1 
              class="font-bold text-base sm:text-lg leading-tight"
              :class="theme === 'light' ? 'text-slate-900' : 'bg-gradient-to-r from-white via-slate-200 to-indigo-200 bg-clip-text text-transparent'"
            >
              {{ t('nav.brand') }}
            </h1>
            <p class="text-[10px]" :class="theme === 'light' ? 'text-slate-500' : 'text-slate-400'">{{ t('nav.subBrand') }}</p>
          </div>
        </NuxtLink>

        <!-- Navigation Tabs -->
        <nav 
          class="hidden md:flex items-center gap-1 p-1 rounded-xl border transition-colors"
          :class="theme === 'light' ? 'bg-slate-100/80 border-slate-200' : 'bg-slate-950/60 border-slate-800'"
        >
          <NuxtLink 
            to="/" 
            class="px-3.5 py-1.5 rounded-lg text-xs font-medium transition"
            :class="route.path === '/' 
              ? 'bg-indigo-600 text-white shadow' 
              : (theme === 'light' ? 'text-slate-600 hover:text-slate-900 hover:bg-slate-200/60' : 'text-slate-400 hover:text-white hover:bg-slate-800/50')"
          >
            📦 {{ t('nav.products') }}
          </NuxtLink>

          <NuxtLink 
            to="/orders" 
            class="px-3.5 py-1.5 rounded-lg text-xs font-medium transition"
            :class="route.path === '/orders' 
              ? 'bg-indigo-600 text-white shadow' 
              : (theme === 'light' ? 'text-slate-600 hover:text-slate-900 hover:bg-slate-200/60' : 'text-slate-400 hover:text-white hover:bg-slate-800/50')"
          >
            🛒 {{ t('nav.transactions') }}
          </NuxtLink>

          <NuxtLink 
            v-if="isAdmin"
            to="/analytics" 
            class="px-3.5 py-1.5 rounded-lg text-xs font-medium transition"
            :class="route.path === '/analytics' 
              ? 'bg-indigo-600 text-white shadow' 
              : (theme === 'light' ? 'text-slate-600 hover:text-slate-900 hover:bg-slate-200/60' : 'text-slate-400 hover:text-white hover:bg-slate-800/50')"
          >
            📊 {{ t('nav.analytics') }}
          </NuxtLink>
        </nav>
      </div>

      <!-- Action Button, Language & Theme Switches, & User Profile -->
      <div class="flex items-center gap-2 sm:gap-3">
        <!-- Language Switcher Toggle -->
        <button
          @click="setLocale(locale === 'id' ? 'en' : 'id')"
          class="px-2.5 py-1.5 rounded-xl border text-xs font-semibold transition cursor-pointer flex items-center gap-1.5"
          :class="theme === 'light' 
            ? 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-300' 
            : 'bg-slate-800/80 hover:bg-slate-700 text-slate-200 border-slate-700'"
          :title="locale === 'id' ? 'Switch to English' : 'Ganti ke Bahasa Indonesia'"
        >
          <span>{{ locale === 'id' ? '🇮🇩 ID' : '🇬🇧 EN' }}</span>
        </button>

        <!-- Theme Switcher Toggle (Dark / Light) -->
        <button
          @click="toggleTheme"
          class="p-2 rounded-xl border text-xs font-semibold transition cursor-pointer flex items-center justify-center"
          :class="theme === 'light' 
            ? 'bg-slate-100 hover:bg-slate-200 text-amber-600 border-slate-300' 
            : 'bg-slate-800/80 hover:bg-slate-700 text-amber-400 border-slate-700'"
          :title="isDark ? 'Ganti ke Mode Terang (Light)' : 'Ganti ke Mode Gelap (Dark)'"
        >
          <span>{{ isDark ? '🌙' : '☀️' }}</span>
        </button>

        <!-- POS Terminal Quick Action Button -->
        <NuxtLink 
          to="/orders/create"
          class="px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-lg shadow-emerald-600/20 flex items-center gap-1.5 transition active:scale-95 shrink-0"
        >
          <span class="text-sm font-bold">+</span>
          <span class="hidden sm:inline">{{ t('nav.pos') }}</span>
        </NuxtLink>

        <!-- Authenticated User Profile Menu -->
        <div 
          v-if="isAuthenticated && user" 
          class="flex items-center gap-2 pl-2 border-l"
          :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'"
        >
          <div class="flex items-center gap-2">
            <!-- Avatar -->
            <div 
              class="w-8 h-8 rounded-lg flex items-center justify-center font-bold text-xs text-white shadow-sm"
              :class="isAdmin ? 'bg-indigo-600 border border-indigo-400/40' : 'bg-emerald-600 border border-emerald-400/40'"
            >
              {{ user.name.charAt(0).toUpperCase() }}
            </div>

            <!-- User Info & Role Badge -->
            <div class="hidden lg:flex flex-col text-left">
              <div class="flex items-center gap-1.5">
                <span 
                  class="text-xs font-bold"
                  :class="theme === 'light' ? 'text-slate-800' : 'text-slate-200'"
                >
                  {{ user.name }}
                </span>
                <span 
                  class="px-1.5 py-0.2 rounded text-[9px] font-extrabold uppercase tracking-wide border"
                  :class="isAdmin ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/30' : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'"
                >
                  {{ user.role }}
                </span>
              </div>
              <span class="text-[10px] text-slate-500 font-mono">{{ user.email }}</span>
            </div>
          </div>

          <!-- Logout Button -->
          <button 
            @click="logout"
            class="p-2 rounded-lg text-xs border transition cursor-pointer"
            :class="theme === 'light'
              ? 'bg-slate-100 hover:bg-rose-50 text-slate-600 hover:text-rose-600 border-slate-300 hover:border-rose-300'
              : 'bg-slate-800/80 hover:bg-rose-950/50 text-slate-400 hover:text-rose-400 border-slate-700 hover:border-rose-500/30'"
            :title="t('nav.logout')"
          >
            🚪 <span class="hidden sm:inline ml-1 font-medium">{{ t('nav.logout') }}</span>
          </button>
        </div>
      </div>
    </div>
  </header>
</template>
