import { useState } from 'react'
import { TOKEN_KEY } from '@/lib/api'

interface TokenGateProps {
  onSaved: () => void
}

/** Asks for a scorer JWT. Tokens come from the identity provider (locally: scripts/mint-token.sh). */
export default function TokenGate({ onSaved }: TokenGateProps) {
  const [value, setValue] = useState('')

  const save = () => {
    const token = value.trim()
    if (!token) return
    localStorage.setItem(TOKEN_KEY, token)
    onSaved()
  }

  return (
    <div className="max-w-xl space-y-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-5">
      <h2 className="font-bold">Scorer sign-in</h2>
      <p className="text-sm text-slate-500">
        Paste a JWT with the <code>SCORER</code> or <code>ADMIN</code> role. For local development run{' '}
        <code>./scripts/mint-token.sh SCORER</code>.
      </p>
      <textarea
        value={value}
        onChange={(e) => setValue(e.target.value)}
        rows={4}
        aria-label="Access token"
        className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-transparent p-2 font-mono text-xs"
      />
      <button
        onClick={save}
        className="px-4 py-2 rounded-lg bg-brand-600 text-white text-sm font-medium hover:opacity-90"
      >
        Save token
      </button>
    </div>
  )
}
