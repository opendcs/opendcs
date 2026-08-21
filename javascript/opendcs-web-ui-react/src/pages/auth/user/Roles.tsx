import DataTable from "datatables.net-react";
import DT from "datatables.net-bs5";
import { AppDataTable } from "../../../components/data-table";
import type { Role } from "opendcs-api";
import type UserProperties from "./UserProperties";
import { useTranslation } from "react-i18next";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export function Roles({ user }: Readonly<UserProperties>) {
  const { t } = useTranslation(["user-data"]);
  return Object.keys(user.roles || {}).map((org) => {
    const roles = user.roles![org];
    return (
      <div key={org}>
        For {org !== "" ? org : "Default"}
        <AppDataTable<Role, number, string>
          data={roles || []}
          columns={[
            { header: t("role"), data: "name" },
            { header: t("translation:description"), data: "description" },
          ]}
          getId={function (row: Role): number {
            return row.id!.value!;
          }}
        />
      </div>
    );
  });
}
