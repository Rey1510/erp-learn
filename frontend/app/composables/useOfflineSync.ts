import type { OutboxOrder } from '~/types/offline'
import type { CreateOrderPayload, Order } from '~/types/order'

const DB_NAME = 'ERP_OFFLINE_POS_DB'
const STORE_NAME = 'outbox_orders'
const DB_VERSION = 1

// Global singleton state across components
const isOnline = ref(true)
const isSimulatedOffline = ref(false)
const outboxList = ref<OutboxOrder[]>([])
const isSyncing = ref(false)
const syncMessage = ref('')
let isInitialized = false

export function useOfflineSync() {
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase || 'http://localhost:8080'

  const effectiveOnline = computed(() => isOnline.value && !isSimulatedOffline.value)

  const pendingSyncCount = computed(() => 
    outboxList.value.filter(o => o.status === 'PENDING_SYNC' || o.status === 'FAILED' || o.status === 'SYNCING').length
  )

  const syncedCount = computed(() => 
    outboxList.value.filter(o => o.status === 'SYNCED').length
  )

  // Helper to clone pure JSON without Vue 3 Reactive Proxies for IndexedDB
  function toPlainJSON<T>(data: T): T {
    return JSON.parse(JSON.stringify(data))
  }

  // 1. IndexedDB Helper Functions
  function openDB(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      if (!import.meta.client || !window.indexedDB) {
        return reject(new Error('IndexedDB not supported in current environment'))
      }

      const req = window.indexedDB.open(DB_NAME, DB_VERSION)

      req.onupgradeneeded = (e: any) => {
        const db = e.target.result as IDBDatabase
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          db.createObjectStore(STORE_NAME, { keyPath: 'id', autoIncrement: true })
        }
      }

      req.onsuccess = () => resolve(req.result)
      req.onerror = () => reject(req.error)
    })
  }

  async function loadOutbox(): Promise<void> {
    if (!import.meta.client) return
    try {
      const db = await openDB()
      const tx = db.transaction(STORE_NAME, 'readonly')
      const store = tx.objectStore(STORE_NAME)
      const req = store.getAll()

      req.onsuccess = () => {
        const raw = (req.result || []) as OutboxOrder[]
        outboxList.value = raw
          .map(item => {
            // Reset any hanging syncing status from previous browser session
            if (item.status === 'SYNCING') {
              item.status = 'PENDING_SYNC'
            }
            return item
          })
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      }
    } catch (err) {
      console.warn('[OfflineSync] Failed to load outbox from IndexedDB:', err)
    }
  }

  async function saveToDB(item: OutboxOrder): Promise<number> {
    const db = await openDB()
    const cleanItem = toPlainJSON(item)
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite')
      const store = tx.objectStore(STORE_NAME)
      const req = store.add(cleanItem)

      req.onsuccess = () => {
        const id = req.result as number
        resolve(id)
      }
      req.onerror = () => reject(req.error)
    })
  }

  async function updateInDB(item: OutboxOrder): Promise<void> {
    const db = await openDB()
    const cleanItem = toPlainJSON(item)
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite')
      const store = tx.objectStore(STORE_NAME)
      const req = store.put(cleanItem)

      req.onsuccess = () => resolve()
      req.onerror = () => reject(req.error)
    })
  }

  async function deleteFromDB(id: number): Promise<void> {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite')
      const store = tx.objectStore(STORE_NAME)
      const req = store.delete(id)

      req.onsuccess = () => resolve()
      req.onerror = () => reject(req.error)
    })
  }

  // 2. Queue Offline Order (When Cashier submits while offline)
  async function queueOfflineOrder(
    payload: CreateOrderPayload,
    itemsSummary: { productName: string; quantity: number; unitPrice: number; subtotal: number }[],
    customerName: string,
    customerEmail: string,
    paymentMethod: string,
    totalAmount: number
  ): Promise<OutboxOrder> {
    const now = new Date()
    const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '')
    const randCode = Math.random().toString(36).substring(2, 6).toUpperCase()
    const tempOrderNumber = `OFFLINE-ORD-${dateStr}-${randCode}`
    const idempotencyKey = `IDEM-OFFLINE-${Date.now()}-${randCode}`

    const newOutboxItem: OutboxOrder = {
      tempOrderNumber,
      payload: toPlainJSON(payload),
      itemsSummary: toPlainJSON(itemsSummary),
      customerName,
      customerEmail,
      paymentMethod,
      totalAmount,
      idempotencyKey,
      createdAt: now.toISOString(),
      status: 'PENDING_SYNC',
      syncAttempts: 0
    }

    const id = await saveToDB(newOutboxItem)
    newOutboxItem.id = id
    outboxList.value.unshift(newOutboxItem)

    return newOutboxItem
  }

  // 3. Background Sync Dispatcher
  async function syncOutboxQueue(): Promise<{ successCount: number; failCount: number }> {
    if (isSyncing.value) {
      return { successCount: 0, failCount: 0 }
    }

    const pendingItems = outboxList.value.filter(
      item => item.status === 'PENDING_SYNC' || item.status === 'FAILED' || item.status === 'SYNCING'
    )

    if (pendingItems.length === 0) {
      return { successCount: 0, failCount: 0 }
    }

    isSyncing.value = true
    syncMessage.value = `Menyinkronkan ${pendingItems.length} transaksi offline ke server...`
    let successCount = 0
    let failCount = 0

    try {
      for (const item of pendingItems) {
        item.status = 'SYNCING'
        item.syncAttempts++
        await updateInDB(item).catch(() => {})

        try {
          const syncedOrder = await $fetch<Order>(`${apiBase}/api/orders`, {
            method: 'POST',
            headers: {
              'X-Idempotency-Key': item.idempotencyKey,
              'Content-Type': 'application/json'
            },
            body: item.payload
          })

          item.status = 'SYNCED'
          item.syncedOrder = syncedOrder
          item.syncedAt = new Date().toISOString()
          item.lastError = undefined
          await updateInDB(item).catch(() => {})
          successCount++
        } catch (err: any) {
          console.error(`[OfflineSync] Failed to sync order ${item.tempOrderNumber}:`, err)
          item.status = 'FAILED'
          item.lastError = err.data?.error || err.data?.message || err.message || 'Koneksi gagal saat sync'
          await updateInDB(item).catch(() => {})
          failCount++
        }
      }

      syncMessage.value = successCount > 0 
        ? `✅ Berhasil sinkronisasi ${successCount} transaksi offline!` 
        : `⚠️ Sinkronisasi selesai dengan ${failCount} kegagalan.`

      if (successCount > 0) {
        // Automatically refresh all active page tables (Orders, Analytics, Products)
        await refreshNuxtData()
      }
    } catch (globalErr: any) {
      console.error('[OfflineSync] Global Sync Error:', globalErr)
      syncMessage.value = `⚠️ Terjadi kesalahan saat sinkronisasi: ${globalErr.message}`
    } finally {
      isSyncing.value = false
    }

    return { successCount, failCount }
  }

  // 4. Clean Synced History
  async function clearSyncedHistory(): Promise<void> {
    const syncedItems = outboxList.value.filter(o => o.status === 'SYNCED')
    for (const item of syncedItems) {
      if (item.id) {
        await deleteFromDB(item.id).catch(() => {})
      }
    }
    outboxList.value = outboxList.value.filter(o => o.status !== 'SYNCED')
  }

  async function removeOutboxItem(id: number): Promise<void> {
    await deleteFromDB(id).catch(() => {})
    outboxList.value = outboxList.value.filter(o => o.id !== id)
  }

  // 5. Toggle Simulation
  function toggleSimulatedOffline() {
    isSimulatedOffline.value = !isSimulatedOffline.value
    if (!isSimulatedOffline.value && isOnline.value) {
      // Returned online -> auto trigger sync!
      setTimeout(() => {
        syncOutboxQueue()
      }, 500)
    }
  }

  // 6. Setup Lifecycle Event Listeners
  if (import.meta.client && !isInitialized) {
    isInitialized = true
    isOnline.value = window.navigator.onLine

    window.addEventListener('online', () => {
      isOnline.value = true
      if (!isSimulatedOffline.value) {
        syncOutboxQueue()
      }
    })

    window.addEventListener('offline', () => {
      isOnline.value = false
    })

    loadOutbox()
  }

  return {
    isOnline,
    isSimulatedOffline,
    effectiveOnline,
    outboxList,
    pendingSyncCount,
    syncedCount,
    isSyncing,
    syncMessage,
    loadOutbox,
    queueOfflineOrder,
    syncOutboxQueue,
    clearSyncedHistory,
    removeOutboxItem,
    toggleSimulatedOffline
  }
}
