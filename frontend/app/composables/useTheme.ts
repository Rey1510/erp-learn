export type Theme = 'dark' | 'light'

export function useTheme() {
  const themeCookie = useCookie<Theme>('erp_theme', {
    default: () => 'dark',
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 365 // 1 year
  })

  const theme = useState<Theme>('app_theme', () => themeCookie.value || 'dark')

  function applyTheme(newTheme: Theme) {
    theme.value = newTheme
    themeCookie.value = newTheme

    if (import.meta.client) {
      const root = document.documentElement
      const body = document.body

      if (newTheme === 'dark') {
        root.classList.add('dark')
        root.classList.remove('light')
        root.style.backgroundColor = '#020617'
        root.style.colorScheme = 'dark'
        if (body) {
          body.style.backgroundColor = '#020617'
          body.style.color = '#f8fafc'
        }
      } else {
        root.classList.remove('dark')
        root.classList.add('light')
        root.style.backgroundColor = '#f1f5f9'
        root.style.colorScheme = 'light'
        if (body) {
          body.style.backgroundColor = '#f1f5f9'
          body.style.color = '#0f172a'
        }
      }
      localStorage.setItem('erp_theme', newTheme)
    }
  }

  function setTheme(newTheme: Theme) {
    applyTheme(newTheme)
  }

  function toggleTheme() {
    const next = theme.value === 'dark' ? 'light' : 'dark'
    applyTheme(next)
  }

  function initTheme() {
    if (import.meta.client) {
      try {
        const saved = (localStorage.getItem('erp_theme') as Theme) || themeCookie.value || 'dark'
        applyTheme(saved)
      } catch {
        applyTheme(themeCookie.value || 'dark')
      }
    }
  }

  return {
    theme,
    isDark: computed(() => theme.value === 'dark'),
    setTheme,
    toggleTheme,
    initTheme
  }
}
