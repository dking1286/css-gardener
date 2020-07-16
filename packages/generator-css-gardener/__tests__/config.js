"use strict";
const assert = require("yeoman-assert");
const helpers = require("yeoman-test");
const path = require("path");

const GENERATOR_PATH = path.join(__dirname, "../generators/config");

describe("generator-css-gardener:config", () => {
  it("creates a css-gardener.edn file in the current working directory", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "output-dir": "public/css",
      "entry-ns": "css-gardener.sass-example.main",
      "stylesheet-type": "scss"
    });

    assert.file("css-gardener.edn");
  });

  it("populates the source path in the config", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "output-dir": "public/css",
      "entry-ns": "css-gardener.sass-example.main",
      "stylesheet-type": "scss"
    });

    assert.fileContent("css-gardener.edn", /:source-paths \["src"\]/);
  });

  it("defaults to 'src' if the --source-path option is missing", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "output-dir": "public/css",
      "entry-ns": "css-gardener.sass-example.main",
      "stylesheet-type": "scss"
    });

    assert.fileContent("css-gardener.edn", /:source-paths \["src"\]/);
  });

  it("populates the output dir in the config", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "output-dir": "public/css",
      "entry-ns": "css-gardener.sass-example.main",
      "stylesheet-type": "scss"
    });

    assert.fileContent("css-gardener.edn", /:output-dir "public\/css"/);
  });

  it("defaults to 'public/css' if the --output-dir option is missing", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "entry-ns": "css-gardener.sass-example.main",
      "stylesheet-type": "scss"
    });

    assert.fileContent("css-gardener.edn", /:output-dir "public\/css"/);
  });

  it("populates the entry ns in the config", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "output-dir": "public/css",
      "entry-ns": "css-gardener.sass-example.main",
      "stylesheet-type": "scss"
    });

    assert.fileContent(
      "css-gardener.edn",
      /:entries \[css-gardener\.sass-example\.main\]/
    );
  });

  it("does not create the file if --entry-ns is missing", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "output-dir": "public/css",
      "stylesheet-type": "scss"
    });

    assert.noFile("css-gardener.edn");
  });

  it("includes an .scss rule if the --stylesheet-type is scss", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "output-dir": "public/css",
      "entry-ns": "css-gardener.sass-example.main",
      "stylesheet-type": "scss"
    });

    assert.fileContent("css-gardener.edn", /:rules\s+\{"\.scss"/);
  });

  it("defaults to scss if no --stylesheet-type is provided", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "output-dir": "public/css",
      "entry-ns": "css-gardener.sass-example.main"
    });

    assert.fileContent("css-gardener.edn", /:rules\s+\{"\.scss"/);
  });

  it("includes a .css rule if --stylesheet-type is css", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "output-dir": "public/css",
      "entry-ns": "css-gardener.sass-example.main",
      "stylesheet-type": "css"
    });

    assert.fileContent("css-gardener.edn", /:rules\s+\{"\.css"/);
  });

  it("does not create the file if --stylesheet-type is not one of the recognized values", async () => {
    await helpers.run(GENERATOR_PATH).withOptions({
      "source-path": "src",
      "output-dir": "public/css",
      "entry-ns": "css-gardener.sass-example.main",
      "stylesheet-type": "wrong"
    });

    assert.noFile("css-gardener.edn");
  });
});
