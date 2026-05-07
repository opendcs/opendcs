import DataTable from "datatables.net-react";
import DT from "datatables.net-bs5";
import { AppDataTable } from "../../../components/data-table";
import type { Role } from "opendcs-api";
import type UserProperties from "./UserProperties";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export function Roles({ user }: UserProperties) {
  return (
    <AppDataTable<Role, number, string>
      data={user.roles || []}
      columns={[
        {
          header: "Role",
          data: "name",
        },
        { header: "Description", data: "name" },
      ]}
      getId={function (row: Role): number {
        return row.id!.value!;
      }}
    ></AppDataTable>
  );
}
