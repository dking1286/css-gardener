"use strict";
const Generator = require("yeoman-generator");

const STYLESHEET_TYPES = ["scss", "css"];

module.exports = class extends Generator {
  constructor(args, opts) {
    super(args, opts);

    this.option("source-path", { type: String, default: "src" });
    this.option("output-dir", { type: String, default: "public/css" });
    this.option("build-name", { type: String, default: "app" });
    this.option("module-name", { type: String, default: "main" });
    this.option("entry-ns", { type: String });
    this.option("stylesheet-type", { type: String, default: "scss" });
  }

  async initializing() {
    if (!this.options["entry-ns"]) {
      this.log("missing required --entry-ns option");
      this.abort = true;
    } else if (!STYLESHEET_TYPES.includes(this.options["stylesheet-type"])) {
      this.log(`--stylesheet-type must be one of: ${STYLESHEET_TYPES}`);
      this.abort = true;
    }
  }

  async writing() {
    if (this.abort) {
      return;
    }

    const templateFile = `css-gardener-${this.options["stylesheet-type"]}.edn`;

    this.fs.copyTpl(
      this.templatePath(templateFile),
      this.destinationPath("css-gardener.edn"),
      {
        sourcePath: this.options["source-path"],
        buildName: this.options["build-name"],
        outputDir: this.options["output-dir"],
        moduleName: this.options["module-name"],
        entryNs: this.options["entry-ns"]
      }
    );
  }
};
