"use strict";
const assert = require("yeoman-assert");
const helpers = require("yeoman-test");
const path = require("path");

const GENERATOR_PATH = path.join(__dirname, "../generators/reagent-component");

describe("generator-css-gardener:reagent-component", () => {
  it("creates a core.cljs file in the specified namespace.", async () => {
    await helpers.run(GENERATOR_PATH).withPrompts({
      "source-path": "src",
      "component-path": "css_gardener/sass_example/components",
      name: "hello_world",
      "stylesheet-type": "scss",
      scope: "hello-world"
    });

    assert.fileContent(
      "src/css_gardener/sass_example/components/hello_world/core.cljs",
      /css-gardener\.sass-example\.components\.hello-world\.core/
    );
  });

  it("creates a styles.scss file in the specified folder, with the scope populated", async () => {
    await helpers.run(GENERATOR_PATH).withPrompts({
      "source-path": "src",
      "component-path": "css_gardener/sass_example/components",
      name: "hello_world",
      "stylesheet-type": "scss",
      scope: "hello-world"
    });

    assert.fileContent(
      "src/css_gardener/sass_example/components/hello_world/styles.scss",
      /:css-gardener\/scope "hello-world"/
    );
  });

  it("creates a styles.css file in the specified folder, when the stylesheet-type is css", async () => {
    await helpers.run(GENERATOR_PATH).withPrompts({
      "source-path": "src",
      "component-path": "css_gardener/sass_example/components",
      name: "hello_world",
      "stylesheet-type": "css",
      scope: "hello-world"
    });

    assert.fileContent(
      "src/css_gardener/sass_example/components/hello_world/styles.css",
      /:css-gardener\/scope "hello-world"/
    );
  });
});
