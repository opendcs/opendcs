import { Form, FormCheck } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { UiRouting } from "./RoutingReducer";

export interface RoutingFlagsProps {
  routing: UiRouting;
  edit?: boolean;
  onChange: (field: keyof UiRouting, value: string | boolean) => void;
}

/**
 * The routing spec's DCP / GOES message-type flags: the boolean checkboxes plus
 * the GOES spacecraft (East/West) and parity (Good/Bad) selectors that are gated
 * on their companion checkbox. Mirrors the Swing SearchCriteria "platform types"
 * panel.
 */
export const RoutingFlags: React.FC<RoutingFlagsProps> = ({
  routing,
  edit = false,
  onChange,
}) => {
  const [t] = useTranslation(["routing"]);

  const flagCheck = (name: keyof UiRouting, label: string, extra?: React.ReactNode) => (
    <div className="d-flex align-items-center gap-2 mb-2">
      <FormCheck
        type="checkbox"
        id={`routing-${String(name)}`}
        label={label}
        disabled={!edit}
        checked={Boolean(routing[name])}
        onChange={(e) => onChange(name, e.currentTarget.checked)}
      />
      {extra}
    </div>
  );

  return (
    <>
      {flagCheck("ascendingTime", t("routing:ascending_time"))}
      {flagCheck("settlingTimeDelay", t("routing:settling_delay"))}
      {flagCheck("goesSelfTimed", t("routing:goes_self_timed"))}
      {flagCheck("goesRandom", t("routing:goes_random"))}
      {flagCheck("networkDCP", t("routing:network_dcp"))}
      {flagCheck("iridium", t("routing:iridium"))}
      {flagCheck("qualityNotifications", t("routing:quality_notifications"))}
      {flagCheck(
        "goesSpacecraftCheck",
        t("routing:spacecraft"),
        <Form.Select
          size="sm"
          aria-label={t("routing:spacecraft")}
          style={{ maxWidth: "7rem" }}
          value={routing.goesSpacecraftSelection ?? "East"}
          disabled={!edit || !routing.goesSpacecraftCheck}
          onChange={(e) => onChange("goesSpacecraftSelection", e.currentTarget.value)}
        >
          <option value="East">{t("routing:east")}</option>
          <option value="West">{t("routing:west")}</option>
        </Form.Select>,
      )}
      {flagCheck(
        "parityCheck",
        t("routing:parity"),
        <Form.Select
          size="sm"
          aria-label={t("routing:parity")}
          style={{ maxWidth: "7rem" }}
          value={routing.paritySelection ?? "Good"}
          disabled={!edit || !routing.parityCheck}
          onChange={(e) => onChange("paritySelection", e.currentTarget.value)}
        >
          <option value="Good">{t("routing:good")}</option>
          <option value="Bad">{t("routing:bad")}</option>
        </Form.Select>,
      )}
    </>
  );
};

export default RoutingFlags;
