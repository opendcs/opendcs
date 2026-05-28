import { useMemo } from "react";
import { Form } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { useRefList } from "../../../contexts/data/RefListContext";
import { useDataSourceRefsQuery } from "../../../queries/dataSources";
import { usePresentationRefsQuery } from "../../../queries/presentations";

interface BaseSelectProps {
  id?: string;
  value?: string;
  edit?: boolean;
  onChange: (value: string) => void;
  ariaLabel?: string;
}

// --- Generic reference-list backed select (DataConsumer, OutputFormat, ...) ---
interface RefListSelectProps extends BaseSelectProps {
  refListName: string;
  /** Prepend an empty option so the field can be cleared. */
  includeBlank?: boolean;
}

export const RefListSelect: React.FC<RefListSelectProps> = ({
  refListName,
  includeBlank = false,
  id,
  value,
  edit = true,
  onChange,
  ariaLabel,
}) => {
  const { refList, ready } = useRefList();
  const options = useMemo(() => {
    if (!ready) return [];
    const items = refList(refListName).items ?? {};
    return Object.values(items)
      .map((it) => it.value ?? "")
      .filter(Boolean)
      .sort((a, b) => a.localeCompare(b));
  }, [refList, refListName, ready]);
  // Keep the current value selectable even if it isn't in the list.
  const allOptions = value && !options.includes(value) ? [value, ...options] : options;

  return (
    <Form.Select
      id={id}
      aria-label={ariaLabel}
      value={value ?? ""}
      disabled={!edit}
      onChange={(e) => onChange(e.currentTarget.value)}
    >
      {includeBlank && <option value="" />}
      {allOptions.map((opt) => (
        <option key={opt} value={opt}>
          {opt}
        </option>
      ))}
    </Form.Select>
  );
};

// --- Data source select (by name) ---
export const DataSourceSelect: React.FC<BaseSelectProps> = ({
  id,
  value,
  edit = true,
  onChange,
  ariaLabel,
}) => {
  const { data: dataSources = [] } = useDataSourceRefsQuery();
  const names = useMemo(
    () =>
      dataSources
        .map((d) => d.name ?? "")
        .filter(Boolean)
        .sort((a, b) => a.localeCompare(b)),
    [dataSources],
  );
  const allNames = value && !names.includes(value) ? [value, ...names] : names;

  return (
    <Form.Select
      id={id}
      aria-label={ariaLabel}
      value={value ?? ""}
      disabled={!edit}
      onChange={(e) => onChange(e.currentTarget.value)}
    >
      <option value="" />
      {allNames.map((n) => (
        <option key={n} value={n}>
          {n}
        </option>
      ))}
    </Form.Select>
  );
};

// --- Presentation group select (by name; "(none)" clears it) ---
export const PresentationGroupSelect: React.FC<BaseSelectProps> = ({
  id,
  value,
  edit = true,
  onChange,
  ariaLabel,
}) => {
  const [t] = useTranslation(["routing"]);
  const { data: groups = [] } = usePresentationRefsQuery();
  const names = useMemo(
    () =>
      groups
        .map((g) => g.name ?? "")
        .filter(Boolean)
        .sort((a, b) => a.localeCompare(b)),
    [groups],
  );
  const allNames = value && !names.includes(value) ? [value, ...names] : names;

  return (
    <Form.Select
      id={id}
      aria-label={ariaLabel}
      value={value ?? ""}
      disabled={!edit}
      onChange={(e) => onChange(e.currentTarget.value)}
    >
      <option value="">{t("routing:none")}</option>
      {allNames.map((n) => (
        <option key={n} value={n}>
          {n}
        </option>
      ))}
    </Form.Select>
  );
};

// --- Timezone select (IANA names from Intl, with the record's value injected) ---
const FALLBACK_TZS = [
  "UTC",
  "GMT",
  "America/New_York",
  "America/Chicago",
  "America/Denver",
  "America/Los_Angeles",
  "America/Anchorage",
  "Pacific/Honolulu",
];

const getTimeZones = (): string[] => {
  const intl = Intl as unknown as { supportedValuesOf?: (k: string) => string[] };
  try {
    if (typeof intl.supportedValuesOf === "function") {
      return intl.supportedValuesOf("timeZone");
    }
  } catch {
    // fall through to fallback list
  }
  return FALLBACK_TZS;
};

export const TimezoneSelect: React.FC<BaseSelectProps> = ({
  id,
  value,
  edit = true,
  onChange,
  ariaLabel,
}) => {
  const zones = useMemo(() => getTimeZones(), []);
  const allZones = value && !zones.includes(value) ? [value, ...zones] : zones;

  return (
    <Form.Select
      id={id}
      aria-label={ariaLabel}
      value={value ?? ""}
      disabled={!edit}
      onChange={(e) => onChange(e.currentTarget.value)}
    >
      <option value="" />
      {allZones.map((z) => (
        <option key={z} value={z}>
          {z}
        </option>
      ))}
    </Form.Select>
  );
};
