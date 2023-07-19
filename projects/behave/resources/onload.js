function addScript( src, callback ) {
  var s = document.createElement( 'script' );
  s.setAttribute( 'src', src );
  s.onload=callback;
  document.body.appendChild( s );
};

window.onWASMModuleLoaded = function() {
  addScript(window.onWASMModuleLoadedPath, window.onAppLoaded);
};
