import test from "node:test";
import assert from "node:assert/strict";
import { win32, posix } from "node:path";
import { staticTarget } from "../server/static-path.mjs";

for (const [name, paths, root] of [
  ["Windows", win32, "C:\\Users\\Çağrı\\İzin 360\\app\\dist"],
  ["POSIX", posix, "/tmp/İzin 360/app/dist"],
]) {
  test(`${name}: page, JS, CSS and offline worker stay inside the package`, () => {
    for (const asset of ["/", "/index.html", "/assets/app.js", "/assets/app.css", "/sw.js", "/icon.svg"])
      assert.equal(staticTarget(root, asset, paths), paths.resolve(root, "." + asset));
  });
  test(`${name}: traversal and similarly named sibling directories remain blocked`, () => {
    for (const asset of ["/../connection.key", "/../dist-secret/key", "/assets/../../server/index.mjs"])
      assert.equal(staticTarget(root, asset, paths), null);
    if (name === "Windows") {
      assert.equal(staticTarget(root, "/..\\connection.key", paths), null);
      assert.equal(staticTarget(root, "/assets\\..\\..\\server/index.mjs", paths), null);
    }
  });
}
