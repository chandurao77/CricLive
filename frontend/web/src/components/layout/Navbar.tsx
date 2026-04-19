import { Link } from 'react-router-dom'
import { Moon, Sun, Wifi } from 'lucide-react'
import { useThemeStore } from '@/store/themeStore'
import { cn } from '@/lib/utils'

export default function Navbar() {
  const { dark, toggle } = useThemeStore()

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm">
      <div className="max-w-7xl mx-auto px-4 h-14 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 font-bold text-xl text-brand-600">
          <Wifi className="w-5 h-5" />
          CrickLive
        </Link>

        <nav className="hidden md:flex items-center gap-6 text-sm font-medium">
          <Link to="/" className="text-slate-600 dark:text-slate-300 hover:text-brand-600 transition-colors">
            Scores
          </Link>
          <a href="#series" className="text-slate-600 dark:text-slate-300 hover:text-brand-600 transition-colors">
            Series
          </a>
          <a href="#stats" className="text-slate-600 dark:text-slate-300 hover:text-brand-600 transition-colors">
            Stats
          </a>
          <a href="#news" className="text-slate-600 dark:text-slate-300 hover:text-brand-600 transition-colors">
            News
          </a>
        </nav>

        <button
          onClick={toggle}
          aria-label="Toggle dark mode"
          className={cn(
            'p-2 rounded-lg transition-colors',
            'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100',
            'hover:bg-slate-100 dark:hover:bg-slate-800',
          )}
        >
          {dark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
        </button>
      </div>
    </header>
  )
}
