import OverlayTrigger from "react-bootstrap/OverlayTrigger";
import Tooltip from "react-bootstrap/Tooltip";
import type { DisplaySettings } from "../displaySettings";

type TimestampDisplayProps = {
  value: unknown;
  settings: DisplaySettings;
  className?: string;
};

function isGmt(timeZone: string) {
  return timeZone.toUpperCase() === "GMT" || timeZone.toUpperCase() === "UTC";
}

function formatTimestamp(date: Date, settings: DisplaySettings, timeZone: string) {
  const formatted = date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
    second: settings.showSeconds ? "2-digit" : undefined,
    hour12: settings.hourFormat === "12",
    timeZone,
    timeZoneName: "short",
  });
  return isGmt(timeZone)
    ? formatted.replace(/\b(?:UTC|GMT\+?0)\b/u, "GMT")
    : formatted;
}

export function TimestampDisplay({
  value,
  settings,
  className,
}: TimestampDisplayProps) {
  const date = value instanceof Date ? value : new Date(String(value ?? ""));
  const displayIsGmt = isGmt(settings.timeZone);
  const localTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || "Local";
  const alternateTimeZone = displayIsGmt ? localTimeZone : "GMT";
  const alternateLabel = displayIsGmt
    ? `Local time (${localTimeZone})`
    : "GMT";
  const dateTime = Number.isNaN(date.getTime()) ? undefined : date.toISOString();
  const displayText = dateTime
    ? formatTimestamp(date, settings, settings.timeZone)
    : "Invalid time";
  const alternateText = dateTime
    ? formatTimestamp(date, settings, alternateTimeZone)
    : "Invalid time";

  return (
    <OverlayTrigger
      placement="top"
      overlay={
        <Tooltip>
          <span className="d-block fw-semibold">{alternateLabel}</span>
          <span>{alternateText}</span>
        </Tooltip>
      }
    >
      <time
        className={`dcpmon-timestamp ${className ?? ""}`.trim()}
        dateTime={dateTime}
        tabIndex={0}
      >
        {displayText}
      </time>
    </OverlayTrigger>
  );
}
