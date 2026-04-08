const fs = require('fs');

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
  "framework": {
    "name": "@storybook/react-webpack5",
    "options": {}
  },
  // shadow-cljs compiles stories to CJS using Object.defineProperty(module.exports, "default", ...)
  // Storybook 8's static CSF indexer expects ES module `export default` syntax, so we
  // provide a custom indexer that parses the compiled ClojureScript output directly.
  "indexers": async (existingIndexers) => {
    const cljsIndexer = {
      test: /behave\.stories\./,
      createIndex: async (fileName, { makeTitle }) => {
        const content = fs.readFileSync(fileName, 'utf8');

        const titleMatch = content.match(/"title":\s*"([^"]+)"/);
        const title = titleMatch ? titleMatch[1] : makeTitle(fileName);

        const exportPattern = /Object\.defineProperty\(module\.exports,\s*"([^"]+)"/g;
        const exportNames = [];
        let match;
        while ((match = exportPattern.exec(content)) !== null) {
          const name = match[1];
          if (name !== 'default' && name !== '__esModule') {
            exportNames.push(name);
          }
        }

        return exportNames.map(exportName => ({
          type: 'story',
          importPath: fileName,
          exportName,
          name: exportName,
          title,
          tags: [],
        }));
      }
    };
    return [cljsIndexer, ...existingIndexers];
  }
}
