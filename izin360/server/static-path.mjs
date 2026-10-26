import path from "node:path";

// Keep package assets inside the root on both Windows and POSIX.
export function staticTarget(directory, pathname, paths = path) {
  const root = paths.resolve(directory);
  const target = paths.resolve(root, "." + pathname);
  const relative = paths.relative(root, target);
  if (relative === ".." || relative.startsWith(".." + paths.sep) || paths.isAbsolute(relative))
    return null;
  return target;
}
