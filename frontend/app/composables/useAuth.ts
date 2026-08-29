import type { User, AuthResponse } from '~/types/auth'

export function useAuth() {
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase || 'http://localhost:8080'
  const API_BASE = `${apiBase}/api/auth`
  
  const authCookie = useCookie<User | null>('erp_auth_user', {
    default: () => null,
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 7 // 7 days
  })

  const user = useState<User | null>('auth_user', () => authCookie.value || null)

  const isLoading = ref(false)
  const authError = ref<string | null>(null)

  const isAuthenticated = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isCashier = computed(() => user.value?.role === 'CASHIER')
  const roleName = computed(() => {
    if (!user.value) return 'Guest'
    return user.value.role === 'ADMIN' ? 'Administrator' : 'Kasir'
  })

  async function login(email: string, password: string): Promise<boolean> {
    isLoading.value = true
    authError.value = null

    try {
      const res = await $fetch<AuthResponse>(`${API_BASE}/login`, {
        method: 'POST',
        headers: {
          'bypass-tunnel-reminder': 'true'
        },
        body: { email, password }
      })

      const authUser: User = {
        id: res.id,
        name: res.name,
        email: res.email,
        role: res.role,
        token: res.token
      }

      user.value = authUser
      authCookie.value = authUser

      if (import.meta.client) {
        localStorage.setItem('erp_auth_user', JSON.stringify(authUser))
      }

      return true
    } catch (err: any) {
      authError.value = err.data?.error || err.message || 'Email atau password salah.'
      return false
    } finally {
      isLoading.value = false
    }
  }

  function logout() {
    user.value = null
    authCookie.value = null
    if (import.meta.client) {
      localStorage.removeItem('erp_auth_user')
    }
    navigateTo('/login')
  }

  function checkStoredAuth() {
    if (!user.value) {
      if (authCookie.value) {
        user.value = authCookie.value
      } else if (import.meta.client) {
        try {
          const saved = localStorage.getItem('erp_auth_user')
          if (saved) {
            const parsed = JSON.parse(saved)
            user.value = parsed
            authCookie.value = parsed
          }
        } catch {
          user.value = null
        }
      }
    }
  }

  return {
    user,
    isAuthenticated,
    isAdmin,
    isCashier,
    roleName,
    isLoading,
    authError,
    login,
    logout,
    checkStoredAuth
  }
}
