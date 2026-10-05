#!/usr/bin/env node
// Plays a synthetic limited-overs match ball by ball through the real APIs
// (match-service to start innings, scoring-service to record balls).
//
//   node scripts/simulate-match.mjs                 # demo T20I from the dev seed data
//   DELAY_MS=200 SEED=7 node scripts/simulate-match.mjs
//
// Env: MATCH_API, SCORING_API, JWT_SECRET, MATCH_ID, DELAY_MS (default 600), SEED (default random)
import { createHmac, randomUUID } from 'node:crypto'

const MATCH_API = process.env.MATCH_API ?? 'http://localhost:8082'
const SCORING_API = process.env.SCORING_API ?? 'http://localhost:8083'
const SECRET = process.env.JWT_SECRET ?? 'dev-only-jwt-secret-change-me-0123456789'
const MATCH_ID = process.env.MATCH_ID ?? 'd1000000-0000-0000-0000-000000000002'
const DELAY_MS = Number(process.env.DELAY_MS ?? 600)
const RUN_ID = randomUUID().slice(0, 8)

const b64url = (v) => Buffer.from(typeof v === 'string' ? v : JSON.stringify(v)).toString('base64url')
function mintToken() {
  const now = Math.floor(Date.now() / 1000)
  const head = b64url({ alg: 'HS256', typ: 'JWT' })
  const body = b64url({ sub: randomUUID(), roles: ['SCORER'], iat: now, exp: now + 3600 })
  const sig = createHmac('sha256', SECRET).update(`${head}.${body}`).digest('base64url')
  return `${head}.${body}.${sig}`
}
const TOKEN = mintToken()

function rng(seed) {
  let a = seed >>> 0
  return () => {
    a = (a + 0x6d2b79f5) >>> 0
    let t = a
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}
const random = rng(Number(process.env.SEED ?? Math.floor(Math.random() * 1e9)))
const pick = (arr) => arr[Math.floor(random() * arr.length)]
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function api(base, path, { method = 'GET', body } = {}) {
  const res = await fetch(`${base}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${TOKEN}` },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) throw new Error(`${method} ${path} -> ${res.status} ${await res.text()}`)
  return res.status === 204 ? null : res.json()
}

/** One random delivery, as the JSON body accepted by POST /api/v1/score/ball. */
function nextDelivery(fielders) {
  const r = random()
  if (r < 0.05) {
    const dismissalType = pick(['BOWLED', 'CAUGHT', 'CAUGHT', 'LBW', 'RUN_OUT', 'STUMPED'])
    return {
      runsScored: 0,
      wicket: true,
      dismissalType,
      fielderId: ['CAUGHT', 'RUN_OUT', 'STUMPED'].includes(dismissalType) ? pick(fielders).id : null,
    }
  }
  if (r < 0.09) return { runsScored: 0, wide: true, extraRuns: random() < 0.15 ? 5 : 1 }
  if (r < 0.11) return { runsScored: random() < 0.5 ? 0 : pick([1, 4, 6]), noBall: true, extraRuns: 1 }
  if (r < 0.12) return { runsScored: 0, bye: true, extraRuns: pick([1, 2]) }
  if (r < 0.13) return { runsScored: 0, legBye: true, extraRuns: 1 }
  const x = random()
  const runs = x < 0.34 ? 0 : x < 0.62 ? 1 : x < 0.72 ? 2 : x < 0.73 ? 3 : x < 0.85 ? 4 : 6
  return { runsScored: runs }
}

/** Scoring is asynchronous (Kafka); wait until match-service has closed the given innings. */
async function waitForInningsComplete(matchId, inningsNumber) {
  for (let i = 0; i < 60; i++) {
    const m = await api(MATCH_API, `/api/v1/matches/${matchId}`)
    if (m.innings.find((x) => x.inningsNumber === inningsNumber)?.status === 'COMPLETED') return
    await sleep(500)
  }
  throw new Error(`Innings ${inningsNumber} did not complete in time`)
}

async function playInnings(match, battingTeamId, squads, maxOvers, target) {
  const started = await api(MATCH_API, `/api/v1/matches/${match.id}/innings`, { method: 'POST', body: { battingTeamId } })
  const innings = started.innings.at(-1)
  const batting = squads.find((s) => s.team.id === battingTeamId).players
  const bowling = squads.find((s) => s.team.id !== battingTeamId).players
  const bowlers = bowling.filter((p) => p.role === 'BOWLER' || p.role === 'ALL_ROUNDER')
  console.log(`\n=== Innings ${innings.inningsNumber}: ${innings.battingTeam.name} batting${target ? ` (target ${target})` : ''}`)

  let order = 2 // batters 0 and 1 open
  let striker = batting[0]
  let nonStriker = batting[1]
  let runs = 0
  let wickets = 0
  let legalBalls = 0
  let bowler = pick(bowlers)
  let n = 0

  while (wickets < 10 && legalBalls < maxOvers * 6 && !(target && runs >= target)) {
    const d = nextDelivery(bowling)
    const body = {
      matchId: match.id,
      inningsId: innings.id,
      batterId: striker.id,
      bowlerId: bowler.id,
      idempotencyKey: `${RUN_ID}-${innings.inningsNumber}-${++n}`,
      ...d,
    }
    const ack = await api(SCORING_API, '/api/v1/score/ball', { method: 'POST', body })
    const extras = d.extraRuns ?? 0
    runs += (d.runsScored ?? 0) + extras
    if (ack.legalBall) legalBalls++
    if (d.wicket) {
      wickets++
      if (order < batting.length) striker = batting[order++]
    } else if (ack.legalBall && ((d.runsScored ?? 0) + (d.bye || d.legBye ? extras : 0)) % 2 === 1) {
      ;[striker, nonStriker] = [nonStriker, striker]
    }
    if (ack.legalBall && legalBalls % 6 === 0) {
      ;[striker, nonStriker] = [nonStriker, striker]
      const previous = bowler
      do { bowler = pick(bowlers) } while (bowler.id === previous.id && bowlers.length > 1)
    }
    const label = d.wicket ? `W (${d.dismissalType})` : d.wide ? 'Wd' : d.noBall ? 'Nb' : d.bye ? 'B' : d.legBye ? 'Lb' : String(d.runsScored)
    console.log(`${ack.overNumber}.${ack.ballNumber}  ${label.padEnd(14)} ${runs}/${wickets}`)
    await sleep(DELAY_MS)
  }
  return runs
}

async function main() {
  let match = await api(MATCH_API, `/api/v1/matches/${MATCH_ID}`)
  const squads = await api(MATCH_API, `/api/v1/matches/${MATCH_ID}/squads`)
  const maxOvers = match.format === 'ODI' ? 50 : 20
  if (match.status === 'COMPLETED') throw new Error(`Match ${MATCH_ID} is already completed; restart with a fresh database`)
  if (match.innings.length > 0) throw new Error(`Match ${MATCH_ID} already has innings; restart with a fresh database`)
  console.log(`${match.homeTeam.name} v ${match.awayTeam.name} (${match.format}) at ${match.venue.name}`)
  console.log(`Open http://localhost:5173/match/${match.id} to watch live`)

  const firstTotal = await playInnings(match, match.homeTeam.id, squads, maxOvers, null)
  await waitForInningsComplete(match.id, 1)
  await playInnings(match, match.awayTeam.id, squads, maxOvers, firstTotal + 1)
  await waitForInningsComplete(match.id, 2)
  match = await api(MATCH_API, `/api/v1/matches/${MATCH_ID}`)
  console.log(`\nResult: ${match.status} - ${match.result ? `${match.result.winningTeamName ?? 'Tie'} ${match.result.margin ?? ''} ${match.result.marginUnit ?? ''}` : 'n/a'}`)
}

main().catch((err) => {
  console.error(err.message)
  process.exit(1)
})
