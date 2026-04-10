export type ParmTypeOption = { value: string; label: string };

/** All parm types from RoleTypes.java */
export const PARM_TYPES: ParmTypeOption[] = [
  { value: "i", label: "i: Simple Input" },
  { value: "o", label: "o: Simple Output" },
  { value: "id", label: "id: Delta with Implicit Period" },
  { value: "idh", label: "idh: Hourly Delta" },
  { value: "idd", label: "idd: Daily Delta" },
  { value: "idld", label: "idld: Delta from end of last day" },
  { value: "idlm", label: "idlm: Delta from end of last month" },
  { value: "idly", label: "idly: Delta from end of last year" },
  { value: "idlwy", label: "idlwy: Delta from end of last water-year" },
  { value: "id5min", label: "id5min: Delta for last 5 minutes" },
  { value: "id10min", label: "id10min: Delta for last 10 minutes" },
  { value: "id15min", label: "id15min: Delta for last 15 minutes" },
  { value: "id20min", label: "id20min: Delta for last 20 minutes" },
  { value: "id30min", label: "id30min: Delta for last 30 minutes" },
];

export const parmTypeLabel = (type: string): string =>
  PARM_TYPES.find((pt) => pt.value === type)?.label ?? type;
