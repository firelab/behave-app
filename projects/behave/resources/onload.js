function addScript( src, callback ) {
  var s = document.createElement( 'script' );
  s.setAttribute( 'src', src );
  s.onload=callback;
  document.body.appendChild( s );
};

// Backwards compatability
window.onWASMModuleLoaded = function() {
  addScript(window.onWASMModuleLoadedPath, window.onAppLoaded);
};

if (window.createModule) {
  createModule().then(instance => {
    window.Module = instance;
    addScript(window.onWASMModuleLoadedPath, window.onAppLoaded);
  });
}
