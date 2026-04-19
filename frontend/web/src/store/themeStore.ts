import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface ThemeState {
  dark: boolean
  toggle: () => void
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      dark: window.matchMedia('(prefers-color-scheme: dark)').matches,
      toggle: () =>
        set((s) => {
          const next = !s.dark
          document.documentElement.classList.toggle('dark', next)
          return { dark: next }
        }),
    }),
    { name: 'cricklive-theme' },
  ),
)

// Apply on load
const { dark } = useThemeStore.getState()
document.documentElement.classList.toggle('dark', dark)
