import { dcpHandlers } from "./handlers/dcp";
import { sourceHandlers } from "./handlers/sources";
import { queryHandlers } from "./handlers/query";
import { summaryHandlers } from "./handlers/summary";

export const handlers = [
  ...dcpHandlers,
  ...sourceHandlers,
  ...queryHandlers,
  ...summaryHandlers
];

