import { readdirSync, writeFile } from "node:fs";
import { exit } from "node:process";

const locales = readdirSync("public/locales");

let file = "";
file += `const availableLanguages: Array<string> = ${JSON.stringify(locales)};\n`;
file += "export default availableLanguages\n";
file += "export const dtLangs: Map<string, ConfigLanguage> = new Map();\n";

let imports = `import { type ConfigLanguage } from "datatables.net-bs5";\n`;
let foundAtLeastOne = false
for (const locale of locales) {
  const friendly = locale.replace("-", "_");
  const langOnly = locale.split("-")[0];
  let importName = null;
  try {
    console.log(`Attempting to locale data types language file for ${locale}`);
    await import(`datatables.net-plugins/i18n/${locale}.mjs`);
    console.log("Plugin found, using it.");
    importName = locale;
  } catch {
    // doesn't exist
    try {
      console.log(`attempting lang only ${langOnly}`);
      await import(`datatables.net-plugins/i18n/${langOnly}.mjs`);
      importName = langOnly;
    } catch {
      console.log(`No files found for ${locale} using default.`);
    }
  }

  if (importName) {
    imports += `// @ts-expect-error("no definitions")\nimport ${friendly} from "datatables.net-plugins/i18n/${importName}.mjs";\n`;
    file += `dtLangs.set("${locale}", ${friendly} as ConfigLanguage);\n`;
    foundAtLeastOne = true
  } else {
    file += `dtLangs.set("${locale}", {});\n`;
  }
}

writeFile("src/lang/index.ts", imports + file, (err) => {
  if (err) {
    console.log(err);
  }
});

if (!foundAtLeastOne) {
  console.log("No DataTable translations were found. Most likely you need to run 'npm install'");
  exit(1);
}
