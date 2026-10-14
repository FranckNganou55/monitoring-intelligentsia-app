import { useEffect, useRef, useCallback, useState } from 'react'

const ACTIVITY_EVENTS = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart', 'click'] as const

interface Options {
  timeoutMs: number
  warningMs: number
  onLogout: () => void
}

export function useIdleTimeout({ timeoutMs, warningMs, onLogout }: Options) {
  const [showWarning, setShowWarning]     = useState(false)
  const [secondsLeft, setSecondsLeft]     = useState(0)
  const logoutTimerRef  = useRef<ReturnType<typeof setTimeout> | null>(null)
  const warningTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const countdownRef    = useRef<ReturnType<typeof setInterval> | null>(null)

  const clearAll = useCallback(() => {
    if (logoutTimerRef.current)  clearTimeout(logoutTimerRef.current)
    if (warningTimerRef.current) clearTimeout(warningTimerRef.current)
    if (countdownRef.current)    clearInterval(countdownRef.current)
  }, [])

  const startCountdown = useCallback((seconds: number) => {
    setSecondsLeft(seconds)
    if (countdownRef.current) clearInterval(countdownRef.current)
    countdownRef.current = setInterval(() => {
      setSecondsLeft(s => {
        if (s <= 1) {
          clearInterval(countdownRef.current!)
          return 0
        }
        return s - 1
      })
    }, 1_000)
  }, [])

  const reset = useCallback(() => {
    clearAll()
    setShowWarning(false)

    warningTimerRef.current = setTimeout(() => {
      setShowWarning(true)
      startCountdown(Math.round(warningMs / 1_000))
    }, timeoutMs - warningMs)

    logoutTimerRef.current = setTimeout(() => {
      onLogout()
    }, timeoutMs)
  }, [timeoutMs, warningMs, onLogout, clearAll, startCountdown])

  // "Rester connecté" button handler
  const stayConnected = useCallback(() => {
    reset()
  }, [reset])

  useEffect(() => {
    const handleActivity = () => {
      // Only reset if the warning isn't shown yet — once warning is visible, user must click the button
      if (!showWarning) reset()
    }

    ACTIVITY_EVENTS.forEach(e => document.addEventListener(e, handleActivity, { passive: true }))
    reset()

    return () => {
      ACTIVITY_EVENTS.forEach(e => document.removeEventListener(e, handleActivity))
      clearAll()
    }
  }, [showWarning, reset, clearAll])

  return { showWarning, secondsLeft, stayConnected }
}
