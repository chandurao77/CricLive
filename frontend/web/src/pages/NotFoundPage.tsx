import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      <h1 className="text-6xl font-bold text-slate-200 dark:text-slate-700">404</h1>
      <p className="mt-4 text-lg text-slate-500">Page not found.</p>
      <Link to="/" className="mt-6 px-4 py-2 bg-brand-600 text-white rounded-lg hover:bg-brand-700 transition-colors text-sm font-medium">
        Back to home
      </Link>
    </div>
  )
}
