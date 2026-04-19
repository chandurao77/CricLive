import type { InningsDetail } from '@/types/match'

interface ScorecardTableProps {
  innings: InningsDetail
}

export default function ScorecardTable({ innings }: ScorecardTableProps) {
  return (
    <div className="space-y-6">
      {/* Batting */}
      <div>
        <h3 className="font-semibold text-sm mb-2 text-slate-500 uppercase tracking-wider">Batting</h3>
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 text-xs uppercase">
              <tr>
                <th className="text-left px-3 py-2 w-full">Batter</th>
                <th className="px-3 py-2 text-right">R</th>
                <th className="px-3 py-2 text-right">B</th>
                <th className="px-3 py-2 text-right">4s</th>
                <th className="px-3 py-2 text-right">6s</th>
                <th className="px-3 py-2 text-right">SR</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
              {innings.batting.map((b) => (
                <tr key={b.playerId} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                  <td className="px-3 py-2">
                    <div className="font-medium">{b.playerName}</div>
                    {b.dismissalDescription && (
                      <div className="text-xs text-slate-400">{b.dismissalDescription}</div>
                    )}
                    {b.didNotBat && <div className="text-xs text-slate-400">did not bat</div>}
                  </td>
                  <td className="px-3 py-2 text-right font-bold">{b.didNotBat ? '-' : b.runs}</td>
                  <td className="px-3 py-2 text-right text-slate-500">{b.didNotBat ? '-' : b.ballsFaced}</td>
                  <td className="px-3 py-2 text-right text-slate-500">{b.didNotBat ? '-' : b.fours}</td>
                  <td className="px-3 py-2 text-right text-slate-500">{b.didNotBat ? '-' : b.sixes}</td>
                  <td className="px-3 py-2 text-right text-slate-500">{b.didNotBat ? '-' : b.strikeRate.toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot className="bg-slate-50 dark:bg-slate-800 text-xs text-slate-500">
              <tr>
                <td colSpan={6} className="px-3 py-1.5">
                  Extras: {innings.extras.total} (w {innings.extras.wides}, nb {innings.extras.noBalls}, b {innings.extras.byes}, lb {innings.extras.legByes})
                </td>
              </tr>
              <tr className="font-bold">
                <td className="px-3 py-1.5">Total</td>
                <td colSpan={5} className="px-3 py-1.5 text-right">
                  {innings.totalRuns}/{innings.wickets} ({innings.overs} ov)
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      {/* Bowling */}
      <div>
        <h3 className="font-semibold text-sm mb-2 text-slate-500 uppercase tracking-wider">Bowling</h3>
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 text-xs uppercase">
              <tr>
                <th className="text-left px-3 py-2 w-full">Bowler</th>
                <th className="px-3 py-2 text-right">O</th>
                <th className="px-3 py-2 text-right">M</th>
                <th className="px-3 py-2 text-right">R</th>
                <th className="px-3 py-2 text-right">W</th>
                <th className="px-3 py-2 text-right">Econ</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
              {innings.bowling.map((b) => (
                <tr key={b.playerId} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                  <td className="px-3 py-2 font-medium">{b.playerName}</td>
                  <td className="px-3 py-2 text-right text-slate-500">{b.overs}</td>
                  <td className="px-3 py-2 text-right text-slate-500">{b.maidens}</td>
                  <td className="px-3 py-2 text-right text-slate-500">{b.runs}</td>
                  <td className="px-3 py-2 text-right font-bold">{b.wickets}</td>
                  <td className="px-3 py-2 text-right text-slate-500">{b.economy.toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
