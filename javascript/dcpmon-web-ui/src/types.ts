export type DcpStatus = "complete" | "partial" | "parity" | "missing";

export type StatusSummary = {
  completeCount: number;
  partialCount: number;
  parityCount: number;
  missingCount: number;
  reservoirCount: number;
};

export type DcpLocation = {
  locationCode: string;
  stationId: string;
  dcpAddress: string;
  status: DcpStatus;
  messagesTotal: number;
  parityCount: number;
};

export type StatusGroupSummary = {
  timestamp: string;
  group: string;
  durationHours: number;
  summary: StatusSummary;
  lowBatteryAddresses: string[];
  locations: DcpLocation[];
};

export type DcpMessage = {
  receiveTime: string;
  cType?: string;
  arm?: string;
  eirp?: string;
  frequency?: string;
  modulation?: string;
  quality?: string;
  channel?: string;
  data: string;
};

export type DcpMessageResponse = {
  total: number;
  messages: DcpMessage[];
};
