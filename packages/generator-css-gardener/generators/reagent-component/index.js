"use strict";
const Generator = require("yeoman-generator");
const camelCase = require("lodash.camelcase");
const kebabCase = require("lodash.kebabcase");
const path = require("path");
const snakeCase = require("lodash.snakecase");

const STYLESHEET_TYPES = ["scss", "css"];

function stylesheetExtension(type) {
  return `.${type}`;
}

module.exports = class extends Generator {
  constructor(args, opts) {
    super(args, opts);

    this.option("source-path", { type: String });
    this.option("component-path", { type: String });
    this.option("name", { type: String });
    this.option("stylesheet-type", { type: String });
    this.option("scope", { type: String });
  }

  async prompting() {
    this.sourcePath = await this._getFromOptionAndPrompt({
      name: "source-path",
      type: "input",
      message:
        "What is the root source directory where you want to generate the " +
        "component?\n" +
        "Note: This should probably be one of the :source-paths in your " +
        "project configuration.",
      default: "src",
      saveAnswer: true
    });

    this._validateSourcePath();

    this.componentPath = await this._getFromOptionAndPrompt({
      name: "component-path",
      type: "input",
      message:
        "In what directory should the component be generated, " +
        "relative to the source directory?",
      saveAnswer: true
    });

    this._validateComponentPath();

    this.name = await this._getFromOptionAndPrompt({
      name: "name",
      type: "input",
      message:
        "What is the name of the component (in snake_case, no file extension)?"
    });

    this._validateName();

    this.stylesheetType = await this._getFromOptionAndPrompt({
      name: "stylesheet-type",
      type: "list",
      message:
        "What kind of stylesheet should be generated for this component?",
      choices: STYLESHEET_TYPES
    });

    this._validateStylesheetType();

    this.scope = await this._getFromOptionAndPrompt({
      name: "scope",
      type: "input",
      message: "What scope should be used for the stylesheet?",
      default: kebabCase(this.name)
    });

    this._validateScope();
  }

  async writing() {
    const styleName = `styles${stylesheetExtension(this.stylesheetType)}`;
    const nsPrefix = this.componentPath
      .split("/")
      .map(kebabCase)
      .join(".");
    const componentName = kebabCase(this.name);

    const componentDestinationPath = path.join(
      this.sourcePath,
      this.componentPath,
      this.name,
      "core.cljs"
    );

    const stylesheetDestinationPath = path.join(
      this.sourcePath,
      this.componentPath,
      this.name,
      styleName
    );

    this.fs.copyTpl(
      this.templatePath("core.cljs"),
      this.destinationPath(componentDestinationPath),
      { styleName, nsPrefix, componentName }
    );

    this.fs.copyTpl(
      this.templatePath(styleName),
      this.destinationPath(stylesheetDestinationPath),
      { scope: this.scope }
    );
  }

  async _getFromPrompt(args) {
    const answers = await this.prompt([args]);

    return answers[args.name];
  }

  async _getFromOptionAndPrompt({
    name,
    default: defaultValue,
    saveAnswer = false,
    ...args
  }) {
    const fromOptions = this.options[name];

    if (fromOptions) {
      return fromOptions;
    }

    const configName = camelCase(name);
    const fromConfig = this.config.get(configName);

    const answers = await this.prompt([
      {
        name: name,
        default: fromConfig || defaultValue,
        ...args
      }
    ]);

    const fromPrompt = answers[name];

    if (saveAnswer) {
      this.config.set(configName, fromPrompt);
    }

    return fromPrompt;
  }

  _validateSourcePath() {
    // Anything is valid for now
  }

  _validateComponentPath() {
    const snakeCased = this.componentPath
      .split("/")
      .map(snakeCase)
      .join("/");

    if (snakeCased !== this.componentPath) {
      throw new ValueError(
        "Component path must be a snake_cased relative path, received " +
          this.componentPath
      );
    }
  }

  _validateName() {
    if (!this.name === snakeCase(this.name)) {
      throw new ValueError(
        "Component name must be in snake_case, received " + this.name
      );
    }
  }

  _validateStylesheetType() {
    if (!STYLESHEET_TYPES.includes(this.stylesheetType)) {
      throw new ValueError(
        `Stylesheet type must be one of ${STYLESHEET_TYPES}, received ` +
          this.stylesheetType
      );
    }
  }

  _validateScope() {
    // Anything is a valid scope for now.
    // TODO(https://github.com/dking1286/css-gardener/issues/50):
    // Validate that the scope can be prepended to a css class selector
  }
};

class ValueError extends Error {}
