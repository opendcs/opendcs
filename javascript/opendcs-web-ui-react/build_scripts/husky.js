import path from "path";

if (process.env.NODE_ENV === "production" || process.env.CI === "true") {
  process.exit(0);
}

process.chdir(path.resolve("../../"));
const husky = (await import("husky")).default;
console.log(husky("javascript/opendcs-web-ui-react/.husky"));
