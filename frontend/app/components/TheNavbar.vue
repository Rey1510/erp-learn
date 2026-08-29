<script setup lang="ts">
import { useOfflineSync } from '~/composables/useOfflineSync'

const route = useRoute()
const { user, isAuthenticated, isAdmin, isCashier, logout } = useAuth()
const { t, locale, setLocale } = useI18n()
const { theme, toggleTheme, isDark } = useTheme()

const {
  isOnline,
  isSimulatedOffline,
  effectiveOnline,
  pendingSyncCount,
  toggleSimulatedOffline
} = useOfflineSync()

const isSyncModalOpen = ref(false)

const { error: productsError } = useProducts()
const isBackendSleeping = computed(() => !!productsError.value)
const isBannerDismissed = ref(false)

withDefaults(defineProps<{
  hasError?: boolean
}>(), {
  hasError: false
})
</script>

<template>
  <div v-if="route.path !== '/login'">
    <!-- Standalone Offline Demo Notice Banner -->
    <div 
      v-if="isBackendSleeping && !isBannerDismissed" 
      class="bg-gradient-to-r from-amber-600 via-orange-600 to-amber-700 text-white px-4 py-2 text-xs flex items-center justify-between shadow-md relative z-40"
    >
      <div class="max-w-7xl mx-auto flex items-center gap-2 flex-wrap">
        <span class="text-sm">⚠️</span>
        <span class="font-bold tracking-wide">Demo Standalone Mode:</span>
        <span class="text-amber-100">
          Backend Spring Boot cloud saat ini offline. Anda tetap dapat menguji seluruh fitur dengan simulasi Mock Data interaktif!
        </span>
        <a 
          href="https://github.com/Rey1510/erp-learn#readme" 
          target="_blank" 
          rel="noopener noreferrer" 
          class="underline font-bold text-white hover:text-amber-200 ml-1 inline-flex items-center gap-1"
        >
          Jalankan Fullstack Lokal (README) ↗
        </a>
      </div>
      <button 
        @click="isBannerDismissed = true" 
        class="text-amber-200 hover:text-white font-bold ml-3 text-sm cursor-pointer p-1"
        title="Tutup pemberitahuan"
      >
        ✕
      </button>
    </div>

    <header 
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

      <!-- Action Buttons, Offline Switch, Utility Controls, & User Profile -->
      <div class="flex items-center gap-1.5 sm:gap-2.5">
        <!-- 1. Interactive Offline / Online Switcher Pill -->
        <div class="flex items-center gap-1">
          <button
            @click="toggleSimulatedOffline"
            class="px-2.5 py-1.5 rounded-xl border text-xs font-semibold transition cursor-pointer flex items-center gap-1.5 active:scale-95 shadow-sm"
            :class="effectiveOnline 
              ? (theme === 'light' ? 'bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border-emerald-200' : 'bg-emerald-950/40 hover:bg-emerald-900/40 text-emerald-400 border-emerald-800/60')
              : (theme === 'light' ? 'bg-rose-50 hover:bg-rose-100 text-rose-700 border-rose-200 animate-pulse' : 'bg-rose-950/50 hover:bg-rose-900/40 text-rose-400 border-rose-800/80 animate-pulse')"
            :title="effectiveOnline ? 'Status: ONLINE (Klik untuk beralih ke Mode Simulasi Offline)' : 'Status: OFFLINE (Klik untuk kembali ke Online)'"
          >
            <span class="w-2 h-2 rounded-full" :class="effectiveOnline ? 'bg-emerald-500' : 'bg-rose-500'"></span>
            <span class="text-[11px] font-bold">{{ effectiveOnline ? 'Online' : 'Offline Mode' }}</span>
          </button>

          <!-- Sync Queue Button (Opens Sync Center Modal) -->
          <button
            @click="isSyncModalOpen = true"
            class="p-1.5 rounded-xl border transition cursor-pointer flex items-center justify-center text-xs"
            :class="pendingSyncCount > 0 
              ? 'bg-amber-500 text-slate-950 border-amber-400 shadow-md shadow-amber-500/30 animate-pulse font-bold' 
              : (theme === 'light' ? 'bg-slate-100 hover:bg-slate-200 text-slate-600 border-slate-300' : 'bg-slate-800/80 hover:bg-slate-700 text-slate-300 border-slate-700')"
            title="Buka POS Offline Sync Center"
          >
            <span v-if="pendingSyncCount > 0" class="text-[10px] px-1 font-black">⚡ {{ pendingSyncCount }}</span>
            <span v-else class="text-xs">⚡</span>
          </button>
        </div>

        <!-- 2. Language Switcher -->
        <button
          @click="setLocale(locale === 'id' ? 'en' : 'id')"
          class="px-2 py-1.5 rounded-xl border text-xs font-semibold transition cursor-pointer flex items-center justify-center"
          :class="theme === 'light' 
            ? 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-300' 
            : 'bg-slate-800/80 hover:bg-slate-700 text-slate-200 border-slate-700'"
          :title="locale === 'id' ? 'Switch to English' : 'Ganti ke Bahasa Indonesia'"
        >
          <span>{{ locale === 'id' ? 'ID' : 'EN' }}</span>
        </button>

        <!-- 3. Theme Switcher (Dark / Light) -->
        <button
          @click="toggleTheme"
          class="p-1.5 rounded-xl border text-xs font-semibold transition cursor-pointer flex items-center justify-center"
          :class="theme === 'light' 
            ? 'bg-slate-100 hover:bg-slate-200 text-amber-600 border-slate-300' 
            : 'bg-slate-800/80 hover:bg-slate-700 text-amber-400 border-slate-700'"
          :title="isDark ? 'Ganti ke Mode Terang (Light)' : 'Ganti ke Mode Gelap (Dark)'"
        >
          <span>{{ isDark ? '🌙' : '☀️' }}</span>
        </button>

        <!-- 4. POS Terminal Quick Action Button -->
        <NuxtLink 
          to="/orders/create"
          class="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs shadow-md shadow-emerald-600/20 flex items-center gap-1 transition active:scale-95 shrink-0"
        >
          <span class="text-sm font-bold">+</span>
          <span>POS</span>
        </NuxtLink>

        <!-- 5. User Profile Badge & Sleek Vector Sign Out -->
        <div 
          v-if="isAuthenticated && user" 
          class="flex items-center gap-1.5 sm:gap-2 pl-2 border-l"
          :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'"
        >
          <!-- User Compact Badge -->
          <div class="flex items-center gap-1.5">
            <div 
              class="w-7 h-7 rounded-lg flex items-center justify-center font-bold text-xs text-white shadow-sm"
              :class="isAdmin ? 'bg-indigo-600 border border-indigo-400/40' : 'bg-emerald-600 border border-emerald-400/40'"
            >
              {{ user.name.charAt(0).toUpperCase() }}
            </div>
            <div class="hidden xl:flex items-center gap-1">
              <span 
                class="text-xs font-bold truncate max-w-[90px]"
                :class="theme === 'light' ? 'text-slate-800' : 'text-slate-200'"
              >
                {{ user.name }}
              </span>
              <span 
                class="px-1.5 py-0.2 rounded text-[8px] font-black uppercase tracking-wider border"
                :class="isAdmin ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/30' : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'"
              >
                {{ user.role }}
              </span>
            </div>
          </div>

          <!-- Clean Vector Logout Button -->
          <button 
            @click="logout"
            class="p-1.5 rounded-xl border transition cursor-pointer flex items-center justify-center text-slate-400 hover:text-rose-400 hover:bg-rose-950/30 border-transparent hover:border-rose-800/40"
            title="Keluar / Sign Out"
          >
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Offline Sync Center Modal -->
    <OfflineSyncModal 
      :is-open="isSyncModalOpen" 
      @close="isSyncModalOpen = false" 
      @synced="refreshNuxtData"
    />
  </header>
  </div>
</template>
