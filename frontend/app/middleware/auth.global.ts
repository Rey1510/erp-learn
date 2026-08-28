export default defineNuxtRouteMiddleware((to) => {
  const { isAuthenticated, isCashier, checkStoredAuth } = useAuth()

  checkStoredAuth()

  // 1. Unauthenticated users -> Redirect to /login
  if (!isAuthenticated.value && to.path !== '/login') {
    return navigateTo('/login')
  }

  // 2. Authenticated users trying to visit /login -> Redirect to appropriate home
  if (isAuthenticated.value && to.path === '/login') {
    if (isCashier.value) {
      return navigateTo('/orders/create')
    }
    return navigateTo('/')
  }

  // 3. RBAC Guard: Cashier cannot access /analytics
  if (isCashier.value && to.path.startsWith('/analytics')) {
    return navigateTo('/orders/create')
  }
})
