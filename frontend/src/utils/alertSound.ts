export type AlertSoundLevel = 'TYPIQUE' | 'MOINS_CRITIQUE' | 'MOYEN' | 'CRITIQUE'

let audioContext: AudioContext | null = null
let alarmBufferPromise: Promise<AudioBuffer> | null = null
let activeSource: AudioScheduledSourceNode | null = null
let activeGain: GainNode | null = null
let durationTimer: number | null = null
let activeLoopKey: string | null = null
let activeDurationMs = 0
let activeGeneration = 0
let pendingLoopKey: string | null = null
let pendingGeneration = -1
let pendingLoopPromise: Promise<boolean> | null = null

function getAudioContext(): AudioContext | null {
  if (typeof window === 'undefined' || !window.AudioContext) return null
  if (!audioContext) {
    audioContext = new window.AudioContext()
    // Some browsers suspend a quiet AudioContext after the user has left the
    // tab. If an alarm is waiting, resume it as soon as the context is allowed
    // to run again (unlockAlertSound calls resume directly from a user gesture).
    audioContext.addEventListener('statechange', () => {
      if (audioContext?.state === 'running' && activeLoopKey && !activeSource) {
        void startLoop(activeLoopKey, activeDurationMs, activeGeneration)
      }
    })
  }
  return audioContext
}

function loadAlarmBuffer(context: AudioContext): Promise<AudioBuffer> {
  alarmBufferPromise ??= fetch('/sounds/alert-critical.mp3')
    .then((response) => {
      if (!response.ok) throw new Error(`Chargement du signal impossible (${response.status}).`)
      return response.arrayBuffer()
    })
    .then((data) => context.decodeAudioData(data))
    .catch((error: unknown) => {
      alarmBufferPromise = null
      throw error
    })
  return alarmBufferPromise
}

function startLoop(key: string, duration: number, generation: number): Promise<boolean> {
  const context = getAudioContext()
  if (!context || context.state !== 'running') return Promise.resolve(false)
  if (activeLoopKey !== key || activeGeneration !== generation) return Promise.resolve(false)
  if (activeSource) return Promise.resolve(true)
  if (pendingLoopKey === key && pendingGeneration === generation && pendingLoopPromise) return pendingLoopPromise

  const startPromise = (async () => {
    try {
      const buffer = await loadAlarmBuffer(context)
      // The alert can be acknowledged while the MP3 is loading.
      if (activeLoopKey !== key || activeGeneration !== generation || context.state !== 'running') return false

      const source = context.createBufferSource()
      const outputGain = context.createGain()
      source.buffer = buffer
      source.loop = true
      source.connect(outputGain)
      outputGain.connect(context.destination)
      source.start()
      activeSource = source
      activeGain = outputGain
      scheduleStop(key, duration)
      return true
    } catch {
      return false
    }
  })()
  pendingLoopKey = key
  pendingGeneration = generation
  pendingLoopPromise = startPromise
  void startPromise.finally(() => {
    if (pendingLoopPromise === startPromise) {
      pendingLoopKey = null
      pendingGeneration = -1
      pendingLoopPromise = null
    }
  })
  return startPromise
}

/** Plays the alarm continuously for the configured duration after the alert is signaled. */
export function startAlertSoundForDuration(level: AlertSoundLevel, durationMs: number): Promise<boolean> {
  const duration = Math.max(0, durationMs)
  if (duration === 0) {
    stopAlertSound()
    return Promise.resolve(false)
  }
  const key = `${level}:${duration}`
  if (activeLoopKey === key) {
    if (activeSource) return Promise.resolve(true)
    if (pendingLoopKey === key && pendingLoopPromise) return pendingLoopPromise
  }

  stopAlertSound()
  activeLoopKey = key
  activeDurationMs = duration
  return startLoop(key, duration, activeGeneration)
}

function scheduleStop(key: string, duration: number): void {
  if (activeLoopKey !== key || durationTimer !== null) return
  // Start the configured duration only after the MP3 source begins playback.
  durationTimer = window.setTimeout(stopAlertSound, duration)
}

/** Resume Web Audio directly from a real user gesture so later alert playback is allowed. */
export async function unlockAlertSound(): Promise<boolean> {
  const context = getAudioContext()
  if (!context) return false

  try {
    // Calling resume before the first await preserves the browser's user activation.
    const resumed = context.resume()
    await resumed
    if (context.state !== 'running') return false
    if (activeLoopKey && !activeSource) return startLoop(activeLoopKey, activeDurationMs, activeGeneration)
    return true
  } catch {
    return false
  }
}

export function stopAlertSound(): void {
  if (durationTimer !== null && typeof window !== 'undefined') window.clearTimeout(durationTimer)
  durationTimer = null
  activeGeneration += 1
  activeLoopKey = null
  activeDurationMs = 0
  pendingLoopKey = null
  pendingGeneration = -1
  pendingLoopPromise = null
  if (!activeSource) return
  try {
    activeSource.stop()
    activeSource.disconnect()
  } catch {
    // The source may already have ended or been stopped.
  }
  activeSource = null
  activeGain?.disconnect()
  activeGain = null
}

/** Play once for an explicit sound preview. */
export async function playAlertSound(_level: AlertSoundLevel): Promise<void> {
  if (activeLoopKey) return
  const context = getAudioContext()
  if (!context) return
  try {
    await context.resume()
    const buffer = await loadAlarmBuffer(context)
    if (context.state !== 'running' || activeLoopKey) return
    const source = context.createBufferSource()
    source.buffer = buffer
    source.connect(context.destination)
    source.start()
  } catch {
    // A preview failure should not interrupt alert handling.
  }
}
