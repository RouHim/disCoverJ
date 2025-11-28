import path from "path";
import { pathToFileURL } from "url";

const homeDir = process.env.HOME || process.env.USERPROFILE;
if (!homeDir) {
  throw new Error("HOME (or USERPROFILE) must be set to locate the global plugin");
}

const pluginPath = path.join(homeDir, ".npm-global/lib/node_modules/prettier-plugin-java/dist/index.js");
const plugin = await import(pathToFileURL(pluginPath));

export default plugin.default ?? plugin;
