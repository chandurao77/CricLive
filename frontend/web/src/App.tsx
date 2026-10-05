import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Layout from '@/components/layout/Layout'
import HomePage from '@/pages/HomePage'
import MatchDetailPage from '@/pages/MatchDetailPage'
import ScorerPage from '@/pages/ScorerPage'
import NotFoundPage from '@/pages/NotFoundPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="match/:matchId" element={<MatchDetailPage />} />
          <Route path="score" element={<ScorerPage />} />
          <Route path="score/:matchId" element={<ScorerPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
