import { useEffect, useRef, useCallback, useState } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { LiveUpdate } from '@/types/match'

interface UseLiveMatchOptions {
  matchId: string
  onUpdate: (update: LiveUpdate) => void
  enabled?: boolean
}

type ConnectionState = 'connecting' | 'connected' | 'disconnected' | 'error'

/**
 * Connects to commentary-service WebSocket (STOMP over SockJS) and subscribes
 * to /topic/match/{matchId}/live. Automatically reconnects on drop.
 */
export function useLiveMatch({ matchId, onUpdate, enabled = true }: UseLiveMatchOptions) {
  const [connectionState, setConnectionState] = useState<ConnectionState>('disconnected')
  const clientRef = useRef<Client | null>(null)
  const onUpdateRef = useRef(onUpdate)
  onUpdateRef.current = onUpdate  // always call latest version

  const connect = useCallback(() => {
    if (!enabled || !matchId) return

    setConnectionState('connecting')

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 25000,
      heartbeatOutgoing: 25000,

      onConnect: () => {
        setConnectionState('connected')

        client.subscribe(`/topic/match/${matchId}/live`, (message: IMessage) => {
          try {
            const update: LiveUpdate = JSON.parse(message.body)
            onUpdateRef.current(update)
          } catch {
            // Raw string from Redis (not JSON) — treat as commentary text
            onUpdateRef.current({ type: 'COMMENTARY', matchId, commentary: message.body })
          }
        })

        // Send ping so server confirms subscription
        client.publish({ destination: `/app/match/${matchId}/ping`, body: '{}' })
      },

      onDisconnect: () => setConnectionState('disconnected'),
      onStompError: () => setConnectionState('error'),
    })

    client.activate()
    clientRef.current = client
  }, [matchId, enabled])

  useEffect(() => {
    connect()
    return () => {
      clientRef.current?.deactivate()
      clientRef.current = null
    }
  }, [connect])

  return { connectionState }
}
