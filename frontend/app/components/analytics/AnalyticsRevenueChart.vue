<script setup lang="ts">
import type { ChartPoint, TimelinePoint } from '~/types/analytics'

defineProps<{
  chartWidth: number
  chartHeight: number
  padding: { top: number; right: number; bottom: number; left: number }
  maxRevenue: number
  chartPoints: ChartPoint[]
  svgLinePath: string
  svgAreaPath: string
  formatRupiah: (val: number) => string
}>()

const { theme } = useTheme()
const hoveredPoint = ref<TimelinePoint | null>(null)
</script>

<template>
  <section 
    class="p-5 rounded-2xl border shadow-xl space-y-4 transition-colors"
    :class="theme === 'light' 
      ? 'bg-white border-slate-200 shadow-slate-200/50' 
      : 'bg-slate-900/70 border-slate-800/80'"
  >
    <!-- Header with Fixed Height -->
    <div class="flex items-center justify-between min-h-[44px]">
      <div>
        <h3 class="font-bold text-sm flex items-center gap-2" :class="theme === 'light' ? 'text-slate-900' : 'text-white'">
          <span>📈</span> Tren Omset Penjualan
        </h3>
        <p class="text-xs text-slate-400 mt-0.5">Grafik pendapatan dari transaksi berstatus PAID</p>
      </div>

      <!-- Stable Tooltip Badge Container -->
      <div class="h-11 flex items-center justify-end">
        <div 
          v-if="hoveredPoint" 
          class="text-right text-xs px-3 py-1.5 rounded-xl border shadow-lg animate-in fade-in duration-100"
          :class="theme === 'light' 
            ? 'bg-white border-indigo-200 shadow-indigo-100/50' 
            : 'bg-slate-950 border-indigo-500/40'"
        >
          <span class="text-slate-400">{{ hoveredPoint.label }}:</span>
          <span class="font-bold text-emerald-500 ml-1.5">{{ formatRupiah(hoveredPoint.revenue) }}</span>
          <span class="text-[10px] text-slate-400 block">({{ hoveredPoint.orderCount }} order lunas)</span>
        </div>
        <div v-else class="text-xs text-slate-400 hidden sm:block">
          Arahkan kursor ke titik grafik
        </div>
      </div>
    </div>

    <!-- SVG Interactive Chart -->
    <div class="relative w-full overflow-hidden pt-2" @mouseleave="hoveredPoint = null">
      <svg 
        :viewBox="`0 0 ${chartWidth} ${chartHeight}`" 
        class="w-full h-56 overflow-visible"
      >
        <defs>
          <linearGradient id="revenue-area-grad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#6366f1" :stop-opacity="theme === 'light' ? '0.35' : '0.45'" />
            <stop offset="70%" stop-color="#6366f1" :stop-opacity="theme === 'light' ? '0.05' : '0.08'" />
            <stop offset="100%" stop-color="#6366f1" stop-opacity="0" />
          </linearGradient>
          <linearGradient id="line-grad" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stop-color="#6366f1" />
            <stop offset="100%" stop-color="#10b981" />
          </linearGradient>
        </defs>

        <!-- Grid Horizontal Lines -->
        <g :stroke="theme === 'light' ? '#e2e8f0' : '#1e293b'" stroke-dasharray="3,3" stroke-width="1">
          <line :x1="padding.left" :y1="padding.top" :x2="chartWidth - padding.right" :y2="padding.top" />
          <line :x1="padding.left" :y1="padding.top + (chartHeight - padding.top - padding.bottom) / 2" :x2="chartWidth - padding.right" :y2="padding.top + (chartHeight - padding.top - padding.bottom) / 2" />
          <line :x1="padding.left" :y1="chartHeight - padding.bottom" :x2="chartWidth - padding.right" :y2="chartHeight - padding.bottom" />
        </g>

        <!-- Y-Axis Values Labels -->
        <text :x="padding.left - 8" :y="padding.top + 4" text-anchor="end" class="text-[10px] fill-slate-400 font-mono">
          {{ formatRupiah(maxRevenue) }}
        </text>
        <text :x="padding.left - 8" :y="padding.top + (chartHeight - padding.top - padding.bottom) / 2 + 4" text-anchor="end" class="text-[10px] fill-slate-400 font-mono">
          {{ formatRupiah(maxRevenue / 2) }}
        </text>
        <text :x="padding.left - 8" :y="chartHeight - padding.bottom + 4" text-anchor="end" class="text-[10px] fill-slate-400 font-mono">
          Rp 0
        </text>

        <!-- Area Fill -->
        <path 
          :d="svgAreaPath" 
          fill="url(#revenue-area-grad)" 
          class="transition-all duration-300 pointer-events-none"
        />

        <!-- Stroke Line Curve -->
        <path 
          :d="svgLinePath" 
          fill="none" 
          stroke="url(#line-grad)" 
          stroke-width="3" 
          stroke-linecap="round"
          stroke-linejoin="round"
          class="transition-all duration-300 pointer-events-none"
        />

        <!-- Interactive Points -->
        <g v-for="(p, i) in chartPoints" :key="i">
          <!-- X-Axis Label -->
          <text 
            :x="p.x" 
            :y="chartHeight - padding.bottom + 18" 
            text-anchor="middle" 
            class="text-[10px] fill-slate-400 font-medium select-none pointer-events-none"
          >
            {{ p.label }}
          </text>

          <!-- Hover vertical indicator dashed line -->
          <line 
            v-if="hoveredPoint?.date === p.date"
            :x1="p.x" 
            :y1="padding.top" 
            :x2="p.x" 
            :y2="chartHeight - padding.bottom" 
            stroke="#818cf8" 
            stroke-width="1.5" 
            stroke-dasharray="3,3"
            class="pointer-events-none"
          />

          <!-- Visual Dot -->
          <circle 
            :cx="p.x" 
            :cy="p.y" 
            :r="hoveredPoint?.date === p.date ? 7 : 4.5" 
            :fill="hoveredPoint?.date === p.date ? '#10b981' : '#6366f1'" 
            :stroke="theme === 'light' ? '#ffffff' : '#0f172a'" 
            stroke-width="2"
            class="transition-all duration-150 pointer-events-none"
          />

          <!-- Invisible Large Hitbox -->
          <circle 
            :cx="p.x" 
            :cy="p.y" 
            r="20" 
            fill="transparent"
            class="cursor-pointer"
            @mouseenter="hoveredPoint = p"
          />
        </g>
      </svg>
    </div>
  </section>
</template>
