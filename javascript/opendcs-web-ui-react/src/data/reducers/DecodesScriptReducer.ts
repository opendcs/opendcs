import type { ApiConfigScript, ApiScriptFormatStatement } from "opendcs-api";

export type DecodesScriptAction = {
  type: "add_statement";
  payload: { statement: ApiScriptFormatStatement };
};

export default function decodesScriptReducer(
  current: ApiConfigScript,
  action: DecodesScriptAction,
): ApiConfigScript {
  switch (action.type) {
    case "add_statement": {
      return {
        ...current,
        formatStatements: [...current.formatStatements!, action.payload.statement],
      };
    }
  }
  return current;
}
