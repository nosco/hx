// Use playwright browsers for cross-browser testing
process.env.CHROMIUM_BIN =
  process.env.CHROMIUM_BIN || require("playwright").chromium.executablePath();
process.env.FIREFOX_BIN =
  process.env.FIREFOX_BIN || require("playwright").firefox.executablePath();
process.env.WEBKIT_HEADLESS_BIN =
  process.env.WEBKIT_HEADLESS_BIN ||
  require("playwright").webkit.executablePath();

module.exports = function (config) {
  config.set({
    browsers: ["ChromiumHeadless", "FirefoxHeadless", "WebkitHeadless"],
    // The directory where the output file lives
    basePath: "target",
    // The file itself
    files: ["ci.js"],
    frameworks: ["cljs-test"],
    plugins: [
      "karma-cljs-test",
      "karma-chrome-launcher",
      "karma-firefox-launcher",
      "karma-webkit-launcher",
    ],
    colors: true,
    logLevel: config.LOG_INFO,
    client: {
      args: ["shadow.test.karma.init"],
      singleRun: true,
    },
  });
};
