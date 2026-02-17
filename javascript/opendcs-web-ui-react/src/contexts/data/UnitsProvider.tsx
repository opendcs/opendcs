import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { UnitsContext, type UnitsContextType } from "./UnitsContext";
import { useApi } from "../app/ApiContext";
import { Record } from "react-bootstrap-icons";
import {
  ApiUnitConverter,
  RESTDataTypeMethodsApi,
  RESTEngineeringUnitMethodsApi,
  RESTReferenceListsApi,
  type ApiUnit,
} from "opendcs-api";

interface ProviderProps {
  children: ReactNode;
}

export const UnitsProvider = ({ children }: ProviderProps) => {
  const api = useApi();
  const [Units, setUnits] = useState<Record<number, ApiUnit>>({});
  const [conversions, setConversions] = useState<ApiUnitConverter[]>([]);
  const [unitsReady, setUnitsReady] = useState(false);
  const [conversionsReady, setConversionsReady] = useState(false);
  const UnitsRef = useRef(Units);
  const ConvsRef = useRef(conversions);

  useEffect(() => {
    const fetchUnits = async () => {
      const UnitsApi = new RESTEngineeringUnitMethodsApi(api.conf);
      const refs = await UnitsApi.getUnitList(api.org);
      setUnits(refs);
      setUnitsReady(true);
    };
    fetchUnits();
  }, [api.conf, api.org]);

  useEffect(() => {
    const fetchConversions = async () => {
      const UnitsApi = new RESTEngineeringUnitMethodsApi(api.conf);
      const conversions = await UnitsApi.getUnitConvList(api.org);
      setConversions(conversions);
      setConversionsReady(true);
    };
    fetchConversions();
  }, [api.conf, api.org]);

  useEffect(() => {
    UnitsRef.current = Units;
  }, [Units]);

  useEffect(() => {
    ConvsRef.current = conversions;
  }, [conversions]);

  const getConversion = useCallback(
    (id?: number, from?: string, to?: string): ApiUnitConverter | undefined => {
      return ConvsRef.current.find(
        (conv) => conv.ucId === id || (conv.fromAbbr === from && conv.toAbbr === to),
      );
    },
    [UnitsRef, ConvsRef],
  );

  const UnitsValue: UnitsContextType = {
    units: Units,
    conversions: {},
    getConversion: getConversion,
    ready: unitsReady && conversionsReady,
  };

  return <UnitsContext value={UnitsValue}>{children}</UnitsContext>;
};
