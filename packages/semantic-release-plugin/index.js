const { exec } = require("child_process");

module.exports = {
  prepare(unusedConfig, context) {
    const nextVersion = context.nextRelease.version;
    return new Promise((resolve, reject) => {
      exec(`npm run update_version ${nextVersion}`, (err) => {
        if (err) {
          reject(err);
        } else {
          resolve();
        }
      });
    });
  },
  publish() {
    return new Promise((resolve, reject) => {
      exec("npm run for_each_package -- 'npm run deploy'", (err) => {
        if (err) {
          reject(err);
        } else {
          resolve();
        }
      });
    });
  },
};
