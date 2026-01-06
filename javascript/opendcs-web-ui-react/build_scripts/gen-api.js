import { execSync } from "child_process";
import path from "path";
import os, { platform } from "os";

const inGradle = process.env.IN_GRADLE ? true : false;
const isShell = os.platform() !== "win32";

const workingDirectory = path.resolve(process.cwd(), "..", "..");
if (!inGradle) {
  // if we're in a gradle environment, it would've done this for us already
  try {
    execSync(
      `${isShell ? "./" : ""}gradlew :api-client-typescript:build --info -Pno.docs=true`,
      { cwd: workingDirectory, stdio: "inherit" },
    );
    process.exit(0);
  } catch (error) {
    console.error(`Command failed ${error.message}`);
    process.exit(1);
  }
} else {
  console.log("In gradle build, assuming gradle peformed this step for us.");
}
