import DataTable from "datatables.net-react";
import DT from "datatables.net-bs5";
import { Card } from "react-bootstrap";
import { useEffect, useState } from "react";
import { ApiAlgorithmRef, RESTAlgorithmMethodsApi } from "opendcs-api";
import { useApi } from "../../../contexts/app/ApiContext";
import { useTranslation } from "react-i18next";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export const Algorithms = () => {
  const api = useApi();
  const [t] = useTranslation("algorithms");
  const [tableData, setTableData] = useState<ApiAlgorithmRef[]>([]);

  const columns = [
    { data: "algorithmId", className: "dt-left" },
    { data: "algorithmName", defaultContent: "" },
    { data: "execClass", defaultContent: "" },
    { data: "numCompsUsing", defaultContent: "", className: "dt-left" },
    { data: "description", defaultContent: "" },
  ];

  useEffect(() => {
    const algorithmApi = new RESTAlgorithmMethodsApi(api.conf);
    algorithmApi.getalgorithmrefs(api.org).then((refs) => {
      setTableData(refs);
    });
  }, [api.conf, api.org]);

  return (
    <div className="content">
      <Card border="primary" className="p-3">
        <DataTable
          id="algorithmTable"
          columns={columns}
          data={tableData}
          className="table-hover table-striped datatable-responsive tablerow-cursor w-100"
        >
          <thead>
            <tr>
              <th>{t("algorithms:header.Id")}</th>
              <th>{t("algorithms:header.Name")}</th>
              <th>{t("algorithms:header.ExecClass")}</th>
              <th>{t("algorithms:header.NumCompsUsing")}</th>
              <th>{t("algorithms:header.Description")}</th>
            </tr>
          </thead>
        </DataTable>
      </Card>
    </div>
  );
};
