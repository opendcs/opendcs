import { type HLJSApi } from "highlight.js";

/**
 * highlightjs.org language implementation for the DECODES language.
 * There's definitely more to do, if you want to please have fun doing so.
 * At the moment this is mostly a placeholder to get filled out more later.
 * @param hljs
 * @returns
 */
export default function (hljs: HLJSApi) {
  return {
    name: "DECODES",
    keywords: "f / x n shefprocessor",
    contains: [hljs.NUMBER_MODE, hljs.APOS_STRING_MODE],
  };
}
