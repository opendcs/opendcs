import { execSync } from "child_process";
import path from "path";
import os from "os";

const workingDirectory = path.resolve(process.cwd(), "..", "..");
const isShell = os.platform() !== "win32";

console.log(
  "Starting instance of web api from gradle. Please note that this process runs in this shell. you will need to open another shell to\n execute additional tasks such as `npm run dev`",
);
try {
  execSync(`${isShell ? "./" : ""}gradlew runApi --info -Pno.docs=true`, {
    cwd: workingDirectory,
    stdio: "inherit",
  });
  process.exit(0);
} catch (error) {
  console.error(`Command failed ${error.message}`);
  process.exit(1);
}
