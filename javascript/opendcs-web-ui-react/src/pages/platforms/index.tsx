import DataTable from "datatables.net-react";
import DT from "datatables.net-bs5";
import { Card } from "react-bootstrap";
import { useEffect, useState } from "react";
import { ApiPlatformRef, RESTDECODESPlatformRecordsApi } from "opendcs-api";
import { useApi } from "../../contexts/ApiContext";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export const Platforms = () => {
  const api = useApi();
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
              <th>Id</th>
              <th>Platform</th>
              <th>Agency</th>
              <th>Transport-ID</th>
              <th>Config</th>
              {/*<th>Expiration</th>*/}
              <th>Description</th>
              {/*<th>Actions</th>*/}
            </tr>
          </thead>
        </DataTable>
      </Card>
    </div>
  );
};
