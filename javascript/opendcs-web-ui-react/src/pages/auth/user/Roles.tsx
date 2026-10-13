import DataTable from "datatables.net-react";
import DT from "datatables.net-bs5";
import { AppDataTable } from "../../../components/data-table";
import type { OpenDcsApiRole } from "opendcs-api";
import type UserProperties from "./UserProperties";
import { useTranslation } from "react-i18next";
import { compareStrings } from "../../../util/sort";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export function Roles({ user }: Readonly<UserProperties>) {
  const { t } = useTranslation(["user-data"]);
  // Roles arrive keyed by organization in map order; sort so the profile
  // lists offices alphabetically like the login and org-switcher selects.
  // The default organization is the empty key, which sorts to the top.
  return Object.keys(user.roles || {})
    .sort(compareStrings)
    .map((org) => {
      const roles = user.roles![org];
      return (
        <div key={org}>
          For {org !== "" ? org : "Default"}
          <AppDataTable<OpenDcsApiRole, string, string>
            data={roles || []}
            columns={[
              { header: t("role"), data: "role" },
              { header: t("translation:description"), data: "description" },
            ]}
            getId={function (row: OpenDcsApiRole): string {
              return row.role!;
            }}
          />
        </div>
      );
    });
}
