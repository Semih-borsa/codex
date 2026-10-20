import { mkdir, cp, rm } from "node:fs/promises";
import { resolve } from "node:path";
const dest = resolve("android/app/src/main/assets");
await rm(dest, { recursive: true, force: true });
await mkdir(dest, { recursive: true });
await cp(resolve("dist"), dest, { recursive: true });
console.log("Android assets copied from production build.");
