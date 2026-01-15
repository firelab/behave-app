// build.ts
await Bun.build({
  entrypoints: ['./src/persistent_sorted_set_js/index.js'],  // your TS entry file(s)
  naming: "[dir]/[name].min.[ext]",
  outdir: 'build',
  target: 'browser',  // or 'node', 'browser'
  format: 'esm',  // or 'cjs'
  // minify: true,
  // sourcemap: 'inline'
});

console.log('Build complete!');

