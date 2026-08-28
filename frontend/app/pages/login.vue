<script setup lang="ts">
definePageMeta({
  layout: false // Custom standalone login layout
})

const { login, isLoading, authError, isAuthenticated, user } = useAuth()
const { t, locale, setLocale } = useI18n()
const { theme, toggleTheme, isDark, initTheme } = useTheme()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const autofillNotice = ref('')

onMounted(() => {
  initTheme()
  if (isAuthenticated.value) {
    redirectBasedOnRole()
  }
})

function redirectBasedOnRole() {
  if (user.value?.role === 'CASHIER') {
    navigateTo('/orders/create')
  } else {
    navigateTo('/')
  }
}

function autofillAdmin() {
  email.value = 'admin@mail.com'
  password.value = 'admin123'
  autofillNotice.value = locale.value === 'id' 
    ? 'Kredensial Admin (admin@mail.com) berhasil diisikan ke form!' 
    : 'Admin credentials (admin@mail.com) filled into the form!'
  setTimeout(() => { autofillNotice.value = '' }, 3500)
}

function autofillCashier() {
  email.value = 'cashier@mail.com'
  password.value = 'cashier123'
  autofillNotice.value = locale.value === 'id'
    ? 'Kredensial Kasir (cashier@mail.com) berhasil diisikan ke form!'
    : 'Cashier credentials (cashier@mail.com) filled into the form!'
  setTimeout(() => { autofillNotice.value = '' }, 3500)
}

async function handleLogin() {
  if (!email.value || !password.value) return

  const success = await login(email.value, password.value)
  if (success) {
    redirectBasedOnRole()
  }
}
</script>

<template>
  <div 
    class="min-h-screen flex flex-col justify-center items-center p-4 relative overflow-hidden selection:bg-indigo-500 selection:text-white transition-colors duration-200"
    :class="theme === 'light' ? 'bg-slate-100 text-slate-900' : 'bg-slate-950 text-slate-100'"
  >
    <!-- Top-Right Preferences Floating Bar (Language & Theme Switches) -->
    <div class="absolute top-6 right-6 z-20 flex items-center gap-2">
      <!-- Language Switch -->
      <button
        @click="setLocale(locale === 'id' ? 'en' : 'id')"
        class="px-3 py-1.5 rounded-xl border text-xs font-semibold shadow-sm transition cursor-pointer flex items-center gap-1.5 backdrop-blur-md"
        :class="theme === 'light'
          ? 'bg-white/90 hover:bg-slate-50 text-slate-700 border-slate-300'
          : 'bg-slate-900/80 hover:bg-slate-800 text-slate-200 border-slate-800'"
      >
        <span>{{ locale === 'id' ? '🇮🇩 ID' : '🇬🇧 EN' }}</span>
      </button>

      <!-- Theme Switch -->
      <button
        @click="toggleTheme"
        class="p-2 rounded-xl border text-xs font-semibold shadow-sm transition cursor-pointer flex items-center justify-center backdrop-blur-md"
        :class="theme === 'light'
          ? 'bg-white/90 hover:bg-slate-50 text-amber-600 border-slate-300'
          : 'bg-slate-900/80 hover:bg-slate-800 text-amber-400 border-slate-800'"
      >
        <span>{{ isDark ? '🌙' : '☀️' }}</span>
      </button>
    </div>

    <!-- Ambient Background Glows -->
    <div class="absolute top-[-15%] left-[-10%] w-[500px] h-[500px] rounded-full bg-indigo-600/15 blur-[120px] pointer-events-none"></div>
    <div class="absolute bottom-[-15%] right-[-10%] w-[500px] h-[500px] rounded-full bg-emerald-600/15 blur-[120px] pointer-events-none"></div>

    <div class="w-full max-w-md space-y-6 relative z-10 animate-in fade-in zoom-in-95 duration-200">
      <!-- App Brand Logo & Title -->
      <div class="text-center space-y-2">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500 to-emerald-500 text-white font-black text-2xl shadow-xl shadow-indigo-500/20 border border-white/20">
          E
        </div>
        <h1 
          class="text-2xl font-black tracking-tight mt-2"
          :class="theme === 'light' ? 'text-slate-900' : 'text-white'"
        >
          ENTERPRISE <span class="bg-gradient-to-r from-indigo-500 to-emerald-500 bg-clip-text text-transparent">ERP &amp; POS</span>
        </h1>
        <p class="text-xs" :class="theme === 'light' ? 'text-slate-500' : 'text-slate-400'">
          {{ t('nav.subBrand') }}
        </p>
      </div>

      <!-- Login Card -->
      <div 
        class="backdrop-blur-xl border rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6 transition-colors"
        :class="theme === 'light' ? 'bg-white border-slate-200 shadow-slate-200/50' : 'bg-slate-900/80 border-slate-800'"
      >
        <div>
          <h2 class="text-lg font-bold" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
            {{ t('auth.welcome') }}
          </h2>
          <p class="text-xs mt-0.5" :class="theme === 'light' ? 'text-slate-500' : 'text-slate-400'">
            {{ t('auth.subWelcome') }}
          </p>
        </div>

        <!-- Error Message Alert -->
        <div v-if="authError" class="p-3.5 rounded-xl bg-rose-950/40 border border-rose-500/40 text-rose-300 text-xs flex items-center gap-2.5 animate-in shake">
          <span>⚠️</span>
          <span>{{ authError }}</span>
        </div>

        <!-- Autofill Notification Toast -->
        <div 
          v-if="autofillNotice" 
          class="p-3 rounded-xl border text-xs flex items-center gap-2 animate-in fade-in"
          :class="theme === 'light' ? 'bg-indigo-50 border-indigo-200 text-indigo-700' : 'bg-indigo-950/50 border-indigo-500/40 text-indigo-300'"
        >
          <span>✨</span>
          <span>{{ autofillNotice }}</span>
        </div>

        <!-- Form -->
        <form @submit.prevent="handleLogin" class="space-y-4">
          <!-- Email Input -->
          <div class="space-y-1.5">
            <label class="text-xs font-semibold" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              {{ t('auth.email') }}
            </label>
            <div class="relative">
              <input
                v-model="email"
                type="email"
                required
                placeholder="contoh: admin@mail.com"
                class="w-full border rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition"
                :class="theme === 'light'
                  ? 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400'
                  : 'bg-slate-950/90 border-slate-800 text-white placeholder-slate-500'"
              />
              <span class="absolute right-3.5 top-2.5 text-slate-400 text-sm">✉️</span>
            </div>
          </div>

          <!-- Password Input -->
          <div class="space-y-1.5">
            <label class="text-xs font-semibold" :class="theme === 'light' ? 'text-slate-700' : 'text-slate-300'">
              {{ t('auth.password') }}
            </label>
            <div class="relative">
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                required
                placeholder="••••••••"
                class="w-full border rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition pr-10"
                :class="theme === 'light'
                  ? 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400'
                  : 'bg-slate-950/90 border-slate-800 text-white placeholder-slate-500'"
              />
              <button 
                type="button" 
                @click="showPassword = !showPassword" 
                class="absolute right-3.5 top-2.5 text-slate-400 hover:text-slate-600 transition cursor-pointer"
              >
                {{ showPassword ? '🙈' : '👁️' }}
              </button>
            </div>
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            :disabled="isLoading || !email || !password"
            class="w-full py-3 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 disabled:opacity-50 text-white font-semibold text-sm shadow-lg shadow-indigo-600/30 hover:shadow-indigo-500/50 transition cursor-pointer flex items-center justify-center gap-2 active:scale-98"
          >
            <span v-if="isLoading" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
            <span>{{ isLoading ? t('auth.verifying') : t('auth.loginBtn') }}</span>
          </button>
        </form>

        <!-- Divider -->
        <div class="relative flex items-center justify-center">
          <div class="border-t w-full" :class="theme === 'light' ? 'border-slate-200' : 'border-slate-800'"></div>
          <span 
            class="px-3 text-[11px] font-medium uppercase tracking-wider"
            :class="theme === 'light' ? 'bg-white text-slate-400' : 'bg-slate-900 text-slate-500'"
          >
            {{ t('auth.demoTitle') }}
          </span>
        </div>

        <!-- Autofill Demo Buttons Section -->
        <div class="space-y-2">
          <p class="text-[11px] text-center" :class="theme === 'light' ? 'text-slate-500' : 'text-slate-400'">
            {{ t('auth.demoDesc') }}
          </p>
          <div class="grid grid-cols-2 gap-2.5">
            <!-- Autofill Admin -->
            <button
              type="button"
              @click="autofillAdmin"
              class="p-3 rounded-xl border transition text-left cursor-pointer group"
              :class="theme === 'light'
                ? 'bg-slate-50 hover:bg-indigo-50/50 border-slate-200 hover:border-indigo-300'
                : 'bg-slate-950/80 hover:bg-slate-850 border-slate-800 hover:border-indigo-500/50'"
            >
              <div class="flex items-center gap-1.5">
                <span class="text-base">👑</span>
                <span 
                  class="text-xs font-bold transition group-hover:text-indigo-600"
                  :class="theme === 'light' ? 'text-slate-800' : 'text-white'"
                >
                  {{ t('auth.adminDemo') }}
                </span>
              </div>
              <p class="text-[10px] mt-1 font-mono text-slate-500">admin@mail.com</p>
              <span class="inline-block mt-1 text-[9px] font-semibold text-indigo-500 uppercase tracking-wide">
                {{ t('auth.adminRoleDesc') }}
              </span>
            </button>

            <!-- Autofill Cashier -->
            <button
              type="button"
              @click="autofillCashier"
              class="p-3 rounded-xl border transition text-left cursor-pointer group"
              :class="theme === 'light'
                ? 'bg-slate-50 hover:bg-emerald-50/50 border-slate-200 hover:border-emerald-300'
                : 'bg-slate-950/80 hover:bg-slate-850 border-slate-800 hover:border-emerald-500/50'"
            >
              <div class="flex items-center gap-1.5">
                <span class="text-base">🛒</span>
                <span 
                  class="text-xs font-bold transition group-hover:text-emerald-600"
                  :class="theme === 'light' ? 'text-slate-800' : 'text-white'"
                >
                  {{ t('auth.cashierDemo') }}
                </span>
              </div>
              <p class="text-[10px] mt-1 font-mono text-slate-500">cashier@mail.com</p>
              <span class="inline-block mt-1 text-[9px] font-semibold text-emerald-500 uppercase tracking-wide">
                {{ t('auth.cashierRoleDesc') }}
              </span>
            </button>
          </div>
        </div>
      </div>

      <!-- Security / Footer note -->
      <div class="text-center text-[11px] text-slate-500">
        Enterprise ERP Security System &middot; Role-Based Access Control (RBAC)
      </div>
    </div>
  </div>
</template>
