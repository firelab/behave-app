module.exports = {
  "stories": [{
    directory: "../resources/public/js/stories/",
    files: "behave.stories.*_stories.js",
    title: "BehavePlus"
  }],
  staticDirs: ['../resources/public'],
  "addons": [
    "@storybook/addon-links",
    "@storybook/addon-essentials",
    "@storybook/addon-interactions"
  ],
  "framework": "@storybook/react"
}
