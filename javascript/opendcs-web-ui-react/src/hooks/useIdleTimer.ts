import { useCallback, useEffect, useRef } from "react";

/**
 * Starts a two-stage inactivity timer.
 *
 * Call the returned reset() function whenever the user successfully
 * communicates with the server (e.g. from API response middleware) to restart
 * both timers. This keeps the client timer in sync with the server session
 * rather than relying on DOM activity events.
 *
 * - After warnAfterMs of no reset() calls → onWarn fires.
 * - After logoutAfterMs of no reset() calls → onLogout fires.
 * - When enabled becomes false (e.g. warning modal is visible) both timers
 *   are suspended and reset() becomes a no-op until re-enabled.
 */
export function useIdleTimer(
  onWarn: () => void,
  onLogout: () => void,
  warnAfterMs: number,
  logoutAfterMs: number,
  enabled: boolean,
): () => void {
  // Internal reset function exposed to the outside via the returned callback.
  const resetRef = useRef<() => void>(() => {});

  useEffect(() => {
    if (!enabled) {
      resetRef.current = () => {};
      return;
    }

    let warnTimer: ReturnType<typeof setTimeout>;
    let logoutTimer: ReturnType<typeof setTimeout>;

    const reset = () => {
      clearTimeout(warnTimer);
      clearTimeout(logoutTimer);
      warnTimer = setTimeout(onWarn, warnAfterMs);
      logoutTimer = setTimeout(onLogout, logoutAfterMs);
    };

    resetRef.current = reset;
    reset(); // start timers immediately when enabled

    return () => {
      clearTimeout(warnTimer);
      clearTimeout(logoutTimer);
      resetRef.current = () => {};
    };
  }, [onWarn, onLogout, warnAfterMs, logoutAfterMs, enabled]);

  // Stable callback that delegates to the current internal reset.
  return useCallback(() => resetRef.current(), []);
}
