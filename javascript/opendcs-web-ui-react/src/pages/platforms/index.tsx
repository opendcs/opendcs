import DataTable from "datatables.net-react";
import DT from "datatables.net-bs5";
import { Card } from "react-bootstrap";
import { useEffect, useState } from "react";
import { ApiPlatformRef, RESTDECODESPlatformRecordsApi } from "opendcs-api";
import { useApi } from "../../contexts/ApiContext";
import { useTranslation } from "react-i18next";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export const Platforms = () => {
  const api = useApi();
  const [t] = useTranslation();
  const [tableData, setTableData] = useState<ApiPlatformRef[]>([]);

  const columns = [
    { data: "platformId" },
    { data: "name" },
    { data: "agency", defaultContent: "" },
    { data: "transportmedium", defaultContent: "" },
    { data: "config", defaultContent: "" },
    /*{ data: 'expiration' },*/
    { data: "description", defaultContent: "" },
    /*{ data: 'Actions' }*/
  ];

  useEffect(() => {
    const platformApi = new RESTDECODESPlatformRecordsApi(api.conf);
    platformApi.getPlatformRefs("").then((refs) => {
      const data = Array.from(refs.values());
      setTableData(data);
    });
  }, [api.conf, setTableData]);

  return (
    <div className="content">
      <Card border="primary" className="large-padding">
        <DataTable
          id="platformTable"
          columns={columns}
          data={tableData}
          className="table-hover table-striped datatable-responsive tablerow-cursor w-100"
        >
          <thead>
            <tr>
              <th>{t("platforms:header.Id")}</th>
              <th>{t("platforms:header.Platform")}</th>
              <th>{t("platforms:header.Agency")}</th>
              <th>{t("platforms:header.TransportId")}</th>
              <th>{t("platforms:header.Config")}</th>
              {/*<th>{t("platforms:header.Expiration")}</th>*/}
              <th>{t("platforms:header.Description")}</th>
              {/*<th>{t("actions")}</th>*/}
            </tr>
          </thead>
        </DataTable>
      </Card>
    </div>
  );
};
