import type { StatusGroupSummary } from "opendcs-dds-api";

export const statusGroupSummary: StatusGroupSummary = {
  timestamp: new Date("2025-06-25T11:33:33-05:00"),
  durationHours: 24,
  counts: {
    complete: 515,
    partial: 15,
    parity: 14,
    missing: 5,
    unknown: 0,
  },
  dcpSummaries: {
    CE1F40D4: {
      identifiers: [
        { type: "Local", id: "NIMB" },
        { type: "SHEF", id: "NMBA4" },
      ],
      status: "complete",
      messageTotal: 23,
      expectedMessageTotal: 24,
      parityCount: 48,
      lowBattery: true,
    },
    CE1F2532: {
      identifiers: [
        { type: "Local", id: "BMOB" },
        { type: "SHEF", id: "BMRA4" },
      ],
      status: "complete",
      messageTotal: 24,
      expectedMessageTotal: 24,
      parityCount: 0,
      lowBattery: false,
    },
    CE000001: {
      identifiers: [
        { type: "Local", id: "TEST" },
        { type: "SHEF", id: "TSTA4" },
      ],
      status: "partial",
      messageTotal: 16,
      expectedMessageTotal: 24,
      parityCount: 2,
      lowBattery: false,
    },
  },
};
