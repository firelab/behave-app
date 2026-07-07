function addScript( src, callback ) {
  var s = document.createElement( 'script' );
  s.setAttribute( 'src', src );
  s.onload=callback;
  document.body.appendChild( s );
};

// Startup timing marks (Phase 0 of STARTUP.org); read via behave.perf.
function bhpMark( name ) {
  if (window.performance) { performance.mark('bhp:' + name); }
};

bhpMark('wasm-glue-loaded');

// Backwards compatability
window.onWASMModuleLoaded = function() {
  addScript(window.onWASMModuleLoadedPath, window.onAppLoaded);
};

if (window.createModule) {
  createModule().then(instance => {
    bhpMark('wasm-module-loaded');
    window.Module = instance;
    addScript(window.onWASMModuleLoadedPath, function() {
      bhpMark('app-js-loaded');
      window.onAppLoaded();
    });
  });
}
