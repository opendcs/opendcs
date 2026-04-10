import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { AuthContext, type AuthContextType } from "./AuthContext";
import { ApiContext, useApi } from "./ApiContext";
import {
  createConfiguration,
  RESTAuthenticationAndAuthorizationApi,
  ServerConfiguration,
  User,
} from "opendcs-api";
import { useNavigate } from "react-router-dom";
import { useIdleTimer } from "../../hooks/useIdleTimer";
import { SessionTimeoutModal } from "../../components/auth/SessionTimeoutModal";
import type { Scheme } from "../../util/login-providers/Scheme.types";
import { fromOpenApiUrl } from "../../util/login-providers";

// Inactivity thresholds — adjust to match the server-side session timeout.
const DEFAULT_WARN_AFTER_MS = 13.5 * 60 * 1000;
const DEFAULT_LOGOUT_AFTER_MS = 14.5 * 60 * 1000;

interface ProviderProps {
  children: ReactNode;
  /** Override the inactivity warn threshold (ms). Primarily for testing/stories. */
  warnAfterMs?: number;
  /** Override the inactivity logout threshold (ms). Must be > warnAfterMs. Primarily for testing/stories. */
  logoutAfterMs?: number;
}

export const AuthProvider = ({
  children,
  warnAfterMs = DEFAULT_WARN_AFTER_MS,
  logoutAfterMs = DEFAULT_LOGOUT_AFTER_MS,
}: ProviderProps) => {
  const warningDurationS = Math.round((logoutAfterMs - warnAfterMs) / 1000);
  const navigate = useNavigate();
  const [user, setUser] = useState<User | undefined>(undefined);
  const [schemes, setSchemes] = useState<Record<string, Scheme>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [showWarning, setShowWarning] = useState(false);
  const [countdown, setCountdown] = useState(warningDurationS);

  const apiContext = useApi();

  // Refs so the middleware closure (created once) always has the latest values
  // without needing to rebuild the Configuration object.
  const isAuthenticatedRef = useRef(false);
  const forceLogoutRef = useRef<() => void>(() => {});
  const resetIdleTimerRef = useRef<() => void>(() => {});

  useEffect(() => {
    isAuthenticatedRef.current = !!user;
  }, [user]);

  // Build the API config once with two middleware responsibilities:
  //   1. On any non-401 response → reset the idle timer (mirrors server-side
  //      session keep-alive, which is driven by actual requests, not mouse moves).
  //   2. On a 401 while the user is authenticated → session expired; force logout.
  const [enhancedConf] = useState(() =>
    createConfiguration({
      baseServer: new ServerConfiguration("/odcsapi", {}),
      promiseMiddleware: [
        {
          pre: (ctx) => Promise.resolve(ctx),
          post: (ctx) => {
            if (ctx.httpStatusCode === 401) {
              if (isAuthenticatedRef.current) {
                // Session died server-side — clear local state immediately.
                forceLogoutRef.current();
              }
              // Don't reset the timer on a 401.
            } else {
              // Any successful response means the server session is still alive.
              resetIdleTimerRef.current();
            }
            return Promise.resolve(ctx);
          },
        },
      ],
    }),
  );

  const enhancedApiContext = useMemo(
    () => ({ ...apiContext, conf: enhancedConf }),
    [apiContext, enhancedConf],
  );

  // Clears local auth state and redirects to login without hitting the API.
  // Used when the server session is already known to be dead.
  const forceLogout = useCallback(() => {
    setUser(undefined);
    setShowWarning(false);
    navigate("/login");
  }, [navigate]);

  useEffect(() => {
    forceLogoutRef.current = forceLogout;
  }, [forceLogout]);

  // Full logout — invalidates the server session then clears local state.
  const logout = useCallback(() => {
    if (!user) return;
    setShowWarning(false);
    const auth = new RESTAuthenticationAndAuthorizationApi(enhancedConf);
    auth
      .logout()
      .then(() => forceLogout())
      .catch((error: unknown) => {
        if ((error as { status: number }).status === 401) {
          forceLogout();
        } else {
          alert("Logout failed: " + String(error));
        }
      });
  }, [user, enhancedConf, forceLogout]);

  // Initial session check on mount.
  useEffect(() => {
    const auth = new RESTAuthenticationAndAuthorizationApi(enhancedConf);
    auth
      .checkSessionAuthorization()
      .then((value: User) => setUser(value))
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      .catch((_error: unknown) => setUser(undefined))
      .finally(() => setIsLoading(false));
  }, [enhancedConf]);

  // retrieve api schemes
  useEffect(() => {
    const fetchSchemes = async () => {
      const apiUrl = new URL(
        "./odcsapi/openapi.json",
        globalThis.window.location.toString(),
      );

      setSchemes(await fromOpenApiUrl(apiUrl));
    };
    console.log("retrieving OpenAPI Schemes");
    fetchSchemes();
  }, [apiContext.conf]);

  // Idle timer — paused while the warning modal is visible so the countdown
  // (not a new API call) controls what happens next.
  const handleWarn = useCallback(() => {
    setShowWarning(true);
    setCountdown(warningDurationS);
  }, [warningDurationS]);

  const resetIdleTimer = useIdleTimer(
    handleWarn,
    logout,
    warnAfterMs,
    logoutAfterMs,
    !!user && !showWarning,
  );

  useEffect(() => {
    resetIdleTimerRef.current = resetIdleTimer;
  }, [resetIdleTimer]);

  // Drive the countdown inside the modal.
  useEffect(() => {
    if (!showWarning) return;
    const interval = setInterval(() => {
      setCountdown((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => clearInterval(interval);
  }, [showWarning]);

  // When the countdown reaches 0 the user has had their warning and done
  // nothing — log them out. Uses the full logout() (not forceLogout()) so the
  // server session is invalidated. Without this, the cookie remains valid and
  // the login page may error or redirect back because the session is still alive.
  useEffect(() => {
    if (showWarning && countdown === 0) {
      logout();
    }
  }, [showWarning, countdown, logout]);

  // "Stay Logged In" — verify the session is still alive on the server.
  // If it is, the response resets the idle timer via middleware automatically.
  // If it is not, the middleware catches the 401 and calls forceLogout().
  const handleStay = useCallback(() => {
    const auth = new RESTAuthenticationAndAuthorizationApi(enhancedConf);
    auth
      .checkSessionAuthorization()
      .then((value: User) => {
        setUser(value);
        setShowWarning(false);
      })
      .catch(() => {
        // 401 → middleware already triggered forceLogout().
        // Any other error → close the modal and let the user keep working.
        setShowWarning(false);
      });
  }, [enhancedConf]);

  const authValue: AuthContextType = {
    user,
    isLoading,
    loginSchemes: schemes,
    setUser,
    setSchemes,
    logout,
  };

  return (
    <ApiContext value={enhancedApiContext}>
      <AuthContext value={authValue}>
        <SessionTimeoutModal
          show={showWarning}
          secondsRemaining={countdown}
          onStay={handleStay}
          onLogout={logout}
        />
        {children}
      </AuthContext>
    </ApiContext>
  );
};
