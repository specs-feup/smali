import WeaverConfiguration from "lara-js/code/WeaverConfiguration.js";
import path from "path";
import { fileURLToPath } from "url";

export const weaverConfig: WeaverConfiguration = {
  weaverName: "alpakka",
  weaverPrettyName: "Alpakka",
  weaverFileName: "lara-js/code/Weaver.js",
  jarPath: path.join(
    path.dirname(path.dirname(fileURLToPath(import.meta.url))),
    "./java-binaries/",
  ),
  javaWeaverQualifiedName: "pt.up.fe.specs.alpakka.weaver.SmaliWeaver",
};
