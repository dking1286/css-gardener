'use strict';
const Generator = require('yeoman-generator');
const camelCase = require('lodash.camelcase');
const get = require('lodash.get');

module.exports = class extends Generator {
  constructor(args, opts) {
    super(args, opts);

    this.option('source-path', { type: String });
    this.option('component-path', { type: String });
    this.option('name', { type: String });
    this.option('scope', { type: String });
    this.option('stylesheet-type', { type: String });
  }

  async prompting() {
    this.sourcePath = await this._getFromOptionAndPrompt({
      name: 'source-path',
      type: 'input',
      message:
        'What is the root source directory where you want to generate the ' +
        'component?\n' +
        'Note: This should probably be one of the :source-paths in your ' +
        'project configuration.',
      default: 'src',
      saveAnswer: true,
    });

    this.componentPath = await this._getFromOptionAndPrompt({
      name: 'component-path',
      type: 'input',
      message:
        'In what directory should the component be generated, ' +
        'relative to the source directory?',
      saveAnswer: true,
    });

    this.name = await this._getFromOptionAndPrompt({
      name: 'name',
      type: 'input',
      message: 'What is the name of the component?',
    });

    this.stylesheetType = await this._getFromOptionAndPrompt({
      name: 'stylesheet-type',
      type: 'list',
      message: 'What kind of stylesheet do you want this component to have?',
      choices: ['scss', 'css']
    });

    this.scope = await this._getFromOptionAndPrompt({
      name: 'scope',
      type: 'input',
      message: 'What scope should be used for the stylesheet?',
      default: this.name,
    });
  }

  async _getFromPrompt(args) {
    const answers = await this.prompt([
      args,
    ]);

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
        ...args,
      },
    ]);

    const fromPrompt = answers[name];

    if (saveAnswer) {
      this.config.set(configName, fromPrompt);
    }

    return fromPrompt;
  }
}