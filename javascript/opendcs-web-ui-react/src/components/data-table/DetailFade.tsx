import { useEffect, useState, type ReactNode } from "react";

export interface DetailFadeProps {
  skeleton: ReactNode;
  children: ReactNode;
}

/**
 * Two-phase entry animation for DataTable child-row detail components.
 *
 * On mount, the skeleton and real content render stacked: skeleton visible on
 * top, real content hidden beneath. After two animation frames — enough for
 * any inner DataTables to wrap their `<table>`s — the skeleton fades out, and
 * once its fade ends the real content fades in. The real content's enter
 * animation class is removed after its first play so that subsequent
 * DataTables redraws (which detach/reattach the child-row DOM) don't retrigger
 * the fade.
 */
export const DetailFade: React.FC<DetailFadeProps> = ({ skeleton, children }) => {
  const [phase, setPhase] = useState<"entering" | "fading" | "ready">("entering");
  const [hasEntered, setHasEntered] = useState(false);

  useEffect(() => {
    if (phase !== "entering") return;
    let frame2 = 0;
    const frame1 = requestAnimationFrame(() => {
      frame2 = requestAnimationFrame(() => setPhase("fading"));
    });
    return () => {
      cancelAnimationFrame(frame1);
      if (frame2) cancelAnimationFrame(frame2);
    };
  }, [phase]);

  return (
    <div className="detail-appear">
      {phase !== "ready" && (
        <div
          className={
            "detail-appear__layer detail-appear__layer--skeleton" +
            (phase === "fading" ? " detail-appear__layer--exit" : "")
          }
          onAnimationEnd={(e) => {
            if (e.target !== e.currentTarget) return;
            if (phase === "fading") setPhase("ready");
          }}
        >
          {skeleton}
        </div>
      )}
      <div
        className={
          "detail-appear__layer" +
          (phase !== "ready"
            ? " detail-appear__layer--hidden"
            : hasEntered
              ? ""
              : " detail-appear__layer--enter")
        }
        onAnimationEnd={(e) => {
          if (e.target !== e.currentTarget) return;
          if (phase === "ready" && !hasEntered) setHasEntered(true);
        }}
      >
        {children}
      </div>
    </div>
  );
};
