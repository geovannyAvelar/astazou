"use client"

import { useEffect, useRef, useCallback } from "react"

const ACTIVITY_EVENTS = [
  "mousemove",
  "mousedown",
  "keydown",
  "touchstart",
  "scroll",
  "visibilitychange",
] as const

interface UseIdleTimerOptions {
  /** Time in milliseconds of inactivity before onIdle fires */
  idleTimeout: number
  /** Called when user becomes idle */
  onIdle: () => void
  /** Called when user resumes activity after being idle */
  onActive?: () => void
  /** How often (ms) to check idleness. Defaults to 10s */
  checkInterval?: number
}

export function useIdleTimer({
  idleTimeout,
  onIdle,
  onActive,
  checkInterval = 10_000,
}: UseIdleTimerOptions) {
  const lastActivityRef = useRef<number>(0)
  const isIdleRef = useRef<boolean>(false)
  const onIdleRef = useRef(onIdle)
  const onActiveRef = useRef(onActive)

  // Keep refs up-to-date without restarting effects
  useEffect(() => {
    onIdleRef.current = onIdle
  }, [onIdle])

  useEffect(() => {
    onActiveRef.current = onActive
  }, [onActive])

  const resetActivity = useCallback(() => {
    lastActivityRef.current = Date.now()

    if (isIdleRef.current) {
      isIdleRef.current = false
      onActiveRef.current?.()
    }
  }, [])

  useEffect(() => {
    // Initialize so the timer doesn't fire immediately on mount
    lastActivityRef.current = Date.now()

    // Register activity listeners
    const events = ACTIVITY_EVENTS as unknown as string[]
    events.forEach((event) => window.addEventListener(event, resetActivity, { passive: true }))

    // Periodic check
    const interval = setInterval(() => {
      const elapsed = Date.now() - lastActivityRef.current
      if (!isIdleRef.current && elapsed >= idleTimeout) {
        isIdleRef.current = true
        onIdleRef.current()
      }
    }, checkInterval)

    return () => {
      events.forEach((event) => window.removeEventListener(event, resetActivity))
      clearInterval(interval)
    }
  }, [idleTimeout, checkInterval, resetActivity])
}



