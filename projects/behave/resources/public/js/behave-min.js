// include: shell.js
// The Module object: Our interface to the outside world. We import
// and export values on it. There are various ways Module can be used:
// 1. Not defined. We create it here
// 2. A function parameter, function(Module) { ..generated code.. }
// 3. pre-run appended it, var Module = {}; ..generated code..
// 4. External script tag defines var Module.
// We need to check if Module already exists (e.g. case 3 above).
// Substitution will be replaced with actual code on later stage of the build,
// this way Closure Compiler will not mangle it (e.g. case 4. above).
// Note that if you want to run closure, and also to use Module
// after the generated code, you will need to define   var Module = {};
// before the code. Then that object will be used in the code, and you
// can continue to use Module afterwards as well.
var Module = typeof Module != 'undefined' ? Module : {};

// --pre-jses are emitted after the Module integration code, so that they can
// refer to Module (if they choose; they can also define Module)


// Sometimes an existing Module object exists with properties
// meant to overwrite the default module functionality. Here
// we collect those properties and reapply _after_ we configure
// the current environment's defaults to avoid having to be so
// defensive during initialization.
var moduleOverrides = Object.assign({}, Module);

var arguments_ = [];
var thisProgram = './this.program';
var quit_ = (status, toThrow) => {
  throw toThrow;
};

// Determine the runtime environment we are in. You can customize this by
// setting the ENVIRONMENT setting at compile time (see settings.js).

// Attempt to auto-detect the environment
var ENVIRONMENT_IS_WEB = typeof window == 'object';
var ENVIRONMENT_IS_WORKER = typeof importScripts == 'function';
// N.b. Electron.js environment is simultaneously a NODE-environment, but
// also a web environment.
var ENVIRONMENT_IS_NODE = typeof process == 'object' && typeof process.versions == 'object' && typeof process.versions.node == 'string';
var ENVIRONMENT_IS_SHELL = !ENVIRONMENT_IS_WEB && !ENVIRONMENT_IS_NODE && !ENVIRONMENT_IS_WORKER;

if (Module['ENVIRONMENT']) {
  throw new Error('Module.ENVIRONMENT has been deprecated. To force the environment, use the ENVIRONMENT compile-time option (for example, -sENVIRONMENT=web or -sENVIRONMENT=node)');
}

// `/` should be present at the end if `scriptDirectory` is not empty
var scriptDirectory = '';
function locateFile(path) {
  if (Module['locateFile']) {
    return Module['locateFile'](path, scriptDirectory);
  }
  return scriptDirectory + path;
}

// Hooks that are implemented differently in different runtime environments.
var read_,
    readAsync,
    readBinary,
    setWindowTitle;

if (ENVIRONMENT_IS_NODE) {
  if (typeof process == 'undefined' || !process.release || process.release.name !== 'node') throw new Error('not compiled for this environment (did you build to HTML and try to run it not on the web, or set ENVIRONMENT to something - like node - and run it someplace else - like on the web?)');

  var nodeVersion = process.versions.node;
  var numericVersion = nodeVersion.split('.').slice(0, 3);
  numericVersion = (numericVersion[0] * 10000) + (numericVersion[1] * 100) + numericVersion[2] * 1;
  var minVersion = 101900;
  if (numericVersion < 101900) {
    throw new Error('This emscripten-generated code requires node v10.19.19.0 (detected v' + nodeVersion + ')');
  }

  // `require()` is no-op in an ESM module, use `createRequire()` to construct
  // the require()` function.  This is only necessary for multi-environment
  // builds, `-sENVIRONMENT=node` emits a static import declaration instead.
  // TODO: Swap all `require()`'s with `import()`'s?
  // These modules will usually be used on Node.js. Load them eagerly to avoid
  // the complexity of lazy-loading.
  var fs = require('fs');
  var nodePath = require('path');

  if (ENVIRONMENT_IS_WORKER) {
    scriptDirectory = nodePath.dirname(scriptDirectory) + '/';
  } else {
    scriptDirectory = __dirname + '/';
  }

// include: node_shell_read.js
read_ = (filename, binary) => {
  // We need to re-wrap `file://` strings to URLs. Normalizing isn't
  // necessary in that case, the path should already be absolute.
  filename = isFileURI(filename) ? new URL(filename) : nodePath.normalize(filename);
  return fs.readFileSync(filename, binary ? undefined : 'utf8');
};

readBinary = (filename) => {
  var ret = read_(filename, true);
  if (!ret.buffer) {
    ret = new Uint8Array(ret);
  }
  assert(ret.buffer);
  return ret;
};

readAsync = (filename, onload, onerror) => {
  // See the comment in the `read_` function.
  filename = isFileURI(filename) ? new URL(filename) : nodePath.normalize(filename);
  fs.readFile(filename, function(err, data) {
    if (err) onerror(err);
    else onload(data.buffer);
  });
};

// end include: node_shell_read.js
  if (process.argv.length > 1) {
    thisProgram = process.argv[1].replace(/\\/g, '/');
  }

  arguments_ = process.argv.slice(2);

  if (typeof module != 'undefined') {
    module['exports'] = Module;
  }

  process.on('uncaughtException', function(ex) {
    // suppress ExitStatus exceptions from showing an error
    if (ex !== 'unwind' && !(ex instanceof ExitStatus) && !(ex.context instanceof ExitStatus)) {
      throw ex;
    }
  });

  // Without this older versions of node (< v15) will log unhandled rejections
  // but return 0, which is not normally the desired behaviour.  This is
  // not be needed with node v15 and about because it is now the default
  // behaviour:
  // See https://nodejs.org/api/cli.html#cli_unhandled_rejections_mode
  var nodeMajor = process.versions.node.split(".")[0];
  if (nodeMajor < 15) {
    process.on('unhandledRejection', function(reason) { throw reason; });
  }

  quit_ = (status, toThrow) => {
    process.exitCode = status;
    throw toThrow;
  };

  Module['inspect'] = function () { return '[Emscripten Module object]'; };

} else
if (ENVIRONMENT_IS_SHELL) {

  if ((typeof process == 'object' && typeof require === 'function') || typeof window == 'object' || typeof importScripts == 'function') throw new Error('not compiled for this environment (did you build to HTML and try to run it not on the web, or set ENVIRONMENT to something - like node - and run it someplace else - like on the web?)');

  if (typeof read != 'undefined') {
    read_ = function shell_read(f) {
      return read(f);
    };
  }

  readBinary = function readBinary(f) {
    let data;
    if (typeof readbuffer == 'function') {
      return new Uint8Array(readbuffer(f));
    }
    data = read(f, 'binary');
    assert(typeof data == 'object');
    return data;
  };

  readAsync = function readAsync(f, onload, onerror) {
    setTimeout(() => onload(readBinary(f)), 0);
  };

  if (typeof clearTimeout == 'undefined') {
    globalThis.clearTimeout = (id) => {};
  }

  if (typeof scriptArgs != 'undefined') {
    arguments_ = scriptArgs;
  } else if (typeof arguments != 'undefined') {
    arguments_ = arguments;
  }

  if (typeof quit == 'function') {
    quit_ = (status, toThrow) => {
      // Unlike node which has process.exitCode, d8 has no such mechanism. So we
      // have no way to set the exit code and then let the program exit with
      // that code when it naturally stops running (say, when all setTimeouts
      // have completed). For that reason, we must call `quit` - the only way to
      // set the exit code - but quit also halts immediately.  To increase
      // consistency with node (and the web) we schedule the actual quit call
      // using a setTimeout to give the current stack and any exception handlers
      // a chance to run.  This enables features such as addOnPostRun (which
      // expected to be able to run code after main returns).
      setTimeout(() => {
        if (!(toThrow instanceof ExitStatus)) {
          let toLog = toThrow;
          if (toThrow && typeof toThrow == 'object' && toThrow.stack) {
            toLog = [toThrow, toThrow.stack];
          }
          err('exiting due to exception: ' + toLog);
        }
        quit(status);
      });
      throw toThrow;
    };
  }

  if (typeof print != 'undefined') {
    // Prefer to use print/printErr where they exist, as they usually work better.
    if (typeof console == 'undefined') console = /** @type{!Console} */({});
    console.log = /** @type{!function(this:Console, ...*): undefined} */ (print);
    console.warn = console.error = /** @type{!function(this:Console, ...*): undefined} */ (typeof printErr != 'undefined' ? printErr : print);
  }

} else

// Note that this includes Node.js workers when relevant (pthreads is enabled).
// Node.js workers are detected as a combination of ENVIRONMENT_IS_WORKER and
// ENVIRONMENT_IS_NODE.
if (ENVIRONMENT_IS_WEB || ENVIRONMENT_IS_WORKER) {
  if (ENVIRONMENT_IS_WORKER) { // Check worker, not web, since window could be polyfilled
    scriptDirectory = self.location.href;
  } else if (typeof document != 'undefined' && document.currentScript) { // web
    scriptDirectory = document.currentScript.src;
  }
  // blob urls look like blob:http://site.com/etc/etc and we cannot infer anything from them.
  // otherwise, slice off the final part of the url to find the script directory.
  // if scriptDirectory does not contain a slash, lastIndexOf will return -1,
  // and scriptDirectory will correctly be replaced with an empty string.
  // If scriptDirectory contains a query (starting with ?) or a fragment (starting with #),
  // they are removed because they could contain a slash.
  if (scriptDirectory.indexOf('blob:') !== 0) {
    scriptDirectory = scriptDirectory.substr(0, scriptDirectory.replace(/[?#].*/, "").lastIndexOf('/')+1);
  } else {
    scriptDirectory = '';
  }

  if (!(typeof window == 'object' || typeof importScripts == 'function')) throw new Error('not compiled for this environment (did you build to HTML and try to run it not on the web, or set ENVIRONMENT to something - like node - and run it someplace else - like on the web?)');

  // Differentiate the Web Worker from the Node Worker case, as reading must
  // be done differently.
  {
// include: web_or_worker_shell_read.js
read_ = (url) => {
      var xhr = new XMLHttpRequest();
      xhr.open('GET', url, false);
      xhr.send(null);
      return xhr.responseText;
  }

  if (ENVIRONMENT_IS_WORKER) {
    readBinary = (url) => {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', url, false);
        xhr.responseType = 'arraybuffer';
        xhr.send(null);
        return new Uint8Array(/** @type{!ArrayBuffer} */(xhr.response));
    };
  }

  readAsync = (url, onload, onerror) => {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.responseType = 'arraybuffer';
    xhr.onload = () => {
      if (xhr.status == 200 || (xhr.status == 0 && xhr.response)) { // file URLs can return 0
        onload(xhr.response);
        return;
      }
      onerror();
    };
    xhr.onerror = onerror;
    xhr.send(null);
  }

// end include: web_or_worker_shell_read.js
  }

  setWindowTitle = (title) => document.title = title;
} else
{
  throw new Error('environment detection error');
}

var out = Module['print'] || console.log.bind(console);
var err = Module['printErr'] || console.warn.bind(console);

// Merge back in the overrides
Object.assign(Module, moduleOverrides);
// Free the object hierarchy contained in the overrides, this lets the GC
// reclaim data used e.g. in memoryInitializerRequest, which is a large typed array.
moduleOverrides = null;
checkIncomingModuleAPI();

// Emit code to handle expected values on the Module object. This applies Module.x
// to the proper local x. This has two benefits: first, we only emit it if it is
// expected to arrive, and second, by using a local everywhere else that can be
// minified.

if (Module['arguments']) arguments_ = Module['arguments'];legacyModuleProp('arguments', 'arguments_');

if (Module['thisProgram']) thisProgram = Module['thisProgram'];legacyModuleProp('thisProgram', 'thisProgram');

if (Module['quit']) quit_ = Module['quit'];legacyModuleProp('quit', 'quit_');

// perform assertions in shell.js after we set up out() and err(), as otherwise if an assertion fails it cannot print the message
// Assertions on removed incoming Module JS APIs.
assert(typeof Module['memoryInitializerPrefixURL'] == 'undefined', 'Module.memoryInitializerPrefixURL option was removed, use Module.locateFile instead');
assert(typeof Module['pthreadMainPrefixURL'] == 'undefined', 'Module.pthreadMainPrefixURL option was removed, use Module.locateFile instead');
assert(typeof Module['cdInitializerPrefixURL'] == 'undefined', 'Module.cdInitializerPrefixURL option was removed, use Module.locateFile instead');
assert(typeof Module['filePackagePrefixURL'] == 'undefined', 'Module.filePackagePrefixURL option was removed, use Module.locateFile instead');
assert(typeof Module['read'] == 'undefined', 'Module.read option was removed (modify read_ in JS)');
assert(typeof Module['readAsync'] == 'undefined', 'Module.readAsync option was removed (modify readAsync in JS)');
assert(typeof Module['readBinary'] == 'undefined', 'Module.readBinary option was removed (modify readBinary in JS)');
assert(typeof Module['setWindowTitle'] == 'undefined', 'Module.setWindowTitle option was removed (modify setWindowTitle in JS)');
assert(typeof Module['TOTAL_MEMORY'] == 'undefined', 'Module.TOTAL_MEMORY has been renamed Module.INITIAL_MEMORY');
legacyModuleProp('read', 'read_');
legacyModuleProp('readAsync', 'readAsync');
legacyModuleProp('readBinary', 'readBinary');
legacyModuleProp('setWindowTitle', 'setWindowTitle');
var IDBFS = 'IDBFS is no longer included by default; build with -lidbfs.js';
var PROXYFS = 'PROXYFS is no longer included by default; build with -lproxyfs.js';
var WORKERFS = 'WORKERFS is no longer included by default; build with -lworkerfs.js';
var NODEFS = 'NODEFS is no longer included by default; build with -lnodefs.js';

assert(!ENVIRONMENT_IS_SHELL, "shell environment detected but not enabled at build time.  Add 'shell' to `-sENVIRONMENT` to enable.");


// end include: shell.js
// include: preamble.js
// === Preamble library stuff ===

// Documentation for the public APIs defined in this file must be updated in:
//    site/source/docs/api_reference/preamble.js.rst
// A prebuilt local version of the documentation is available at:
//    site/build/text/docs/api_reference/preamble.js.txt
// You can also build docs locally as HTML or other formats in site/
// An online HTML version (which may be of a different version of Emscripten)
//    is up at http://kripken.github.io/emscripten-site/docs/api_reference/preamble.js.html

var wasmBinary;
if (Module['wasmBinary']) wasmBinary = Module['wasmBinary'];legacyModuleProp('wasmBinary', 'wasmBinary');
var noExitRuntime = Module['noExitRuntime'] || true;legacyModuleProp('noExitRuntime', 'noExitRuntime');

if (typeof WebAssembly != 'object') {
  abort('no native wasm support detected');
}

// Wasm globals

var wasmMemory;

//========================================
// Runtime essentials
//========================================

// whether we are quitting the application. no code should run after this.
// set in exit() and abort()
var ABORT = false;

// set by exit() and abort().  Passed to 'onExit' handler.
// NOTE: This is also used as the process return code code in shell environments
// but only when noExitRuntime is false.
var EXITSTATUS;

/** @type {function(*, string=)} */
function assert(condition, text) {
  if (!condition) {
    abort('Assertion failed' + (text ? ': ' + text : ''));
  }
}

// We used to include malloc/free by default in the past. Show a helpful error in
// builds with assertions.

// include: runtime_strings.js
// runtime_strings.js: String related runtime functions that are part of both
// MINIMAL_RUNTIME and regular runtime.

var UTF8Decoder = typeof TextDecoder != 'undefined' ? new TextDecoder('utf8') : undefined;

/**
 * Given a pointer 'idx' to a null-terminated UTF8-encoded string in the given
 * array that contains uint8 values, returns a copy of that string as a
 * Javascript String object.
 * heapOrArray is either a regular array, or a JavaScript typed array view.
 * @param {number} idx
 * @param {number=} maxBytesToRead
 * @return {string}
 */
function UTF8ArrayToString(heapOrArray, idx, maxBytesToRead) {
  var endIdx = idx + maxBytesToRead;
  var endPtr = idx;
  // TextDecoder needs to know the byte length in advance, it doesn't stop on
  // null terminator by itself.  Also, use the length info to avoid running tiny
  // strings through TextDecoder, since .subarray() allocates garbage.
  // (As a tiny code save trick, compare endPtr against endIdx using a negation,
  // so that undefined means Infinity)
  while (heapOrArray[endPtr] && !(endPtr >= endIdx)) ++endPtr;

  if (endPtr - idx > 16 && heapOrArray.buffer && UTF8Decoder) {
    return UTF8Decoder.decode(heapOrArray.subarray(idx, endPtr));
  }
  var str = '';
  // If building with TextDecoder, we have already computed the string length
  // above, so test loop end condition against that
  while (idx < endPtr) {
    // For UTF8 byte structure, see:
    // http://en.wikipedia.org/wiki/UTF-8#Description
    // https://www.ietf.org/rfc/rfc2279.txt
    // https://tools.ietf.org/html/rfc3629
    var u0 = heapOrArray[idx++];
    if (!(u0 & 0x80)) { str += String.fromCharCode(u0); continue; }
    var u1 = heapOrArray[idx++] & 63;
    if ((u0 & 0xE0) == 0xC0) { str += String.fromCharCode(((u0 & 31) << 6) | u1); continue; }
    var u2 = heapOrArray[idx++] & 63;
    if ((u0 & 0xF0) == 0xE0) {
      u0 = ((u0 & 15) << 12) | (u1 << 6) | u2;
    } else {
      if ((u0 & 0xF8) != 0xF0) warnOnce('Invalid UTF-8 leading byte ' + ptrToString(u0) + ' encountered when deserializing a UTF-8 string in wasm memory to a JS string!');
      u0 = ((u0 & 7) << 18) | (u1 << 12) | (u2 << 6) | (heapOrArray[idx++] & 63);
    }

    if (u0 < 0x10000) {
      str += String.fromCharCode(u0);
    } else {
      var ch = u0 - 0x10000;
      str += String.fromCharCode(0xD800 | (ch >> 10), 0xDC00 | (ch & 0x3FF));
    }
  }
  return str;
}

/**
 * Given a pointer 'ptr' to a null-terminated UTF8-encoded string in the
 * emscripten HEAP, returns a copy of that string as a Javascript String object.
 *
 * @param {number} ptr
 * @param {number=} maxBytesToRead - An optional length that specifies the
 *   maximum number of bytes to read. You can omit this parameter to scan the
 *   string until the first \0 byte. If maxBytesToRead is passed, and the string
 *   at [ptr, ptr+maxBytesToReadr[ contains a null byte in the middle, then the
 *   string will cut short at that byte index (i.e. maxBytesToRead will not
 *   produce a string of exact length [ptr, ptr+maxBytesToRead[) N.B. mixing
 *   frequent uses of UTF8ToString() with and without maxBytesToRead may throw
 *   JS JIT optimizations off, so it is worth to consider consistently using one
 * @return {string}
 */
function UTF8ToString(ptr, maxBytesToRead) {
  assert(typeof ptr == 'number');
  return ptr ? UTF8ArrayToString(HEAPU8, ptr, maxBytesToRead) : '';
}

/**
 * Copies the given Javascript String object 'str' to the given byte array at
 * address 'outIdx', encoded in UTF8 form and null-terminated. The copy will
 * require at most str.length*4+1 bytes of space in the HEAP.  Use the function
 * lengthBytesUTF8 to compute the exact number of bytes (excluding null
 * terminator) that this function will write.
 *
 * @param {string} str - The Javascript string to copy.
 * @param {ArrayBufferView|Array<number>} heap - The array to copy to. Each
 *                                               index in this array is assumed
 *                                               to be one 8-byte element.
 * @param {number} outIdx - The starting offset in the array to begin the copying.
 * @param {number} maxBytesToWrite - The maximum number of bytes this function
 *                                   can write to the array.  This count should
 *                                   include the null terminator, i.e. if
 *                                   maxBytesToWrite=1, only the null terminator
 *                                   will be written and nothing else.
 *                                   maxBytesToWrite=0 does not write any bytes
 *                                   to the output, not even the null
 *                                   terminator.
 * @return {number} The number of bytes written, EXCLUDING the null terminator.
 */
function stringToUTF8Array(str, heap, outIdx, maxBytesToWrite) {
  // Parameter maxBytesToWrite is not optional. Negative values, 0, null,
  // undefined and false each don't write out any bytes.
  if (!(maxBytesToWrite > 0))
    return 0;

  var startIdx = outIdx;
  var endIdx = outIdx + maxBytesToWrite - 1; // -1 for string null terminator.
  for (var i = 0; i < str.length; ++i) {
    // Gotcha: charCodeAt returns a 16-bit word that is a UTF-16 encoded code
    // unit, not a Unicode code point of the character! So decode
    // UTF16->UTF32->UTF8.
    // See http://unicode.org/faq/utf_bom.html#utf16-3
    // For UTF8 byte structure, see http://en.wikipedia.org/wiki/UTF-8#Description
    // and https://www.ietf.org/rfc/rfc2279.txt
    // and https://tools.ietf.org/html/rfc3629
    var u = str.charCodeAt(i); // possibly a lead surrogate
    if (u >= 0xD800 && u <= 0xDFFF) {
      var u1 = str.charCodeAt(++i);
      u = 0x10000 + ((u & 0x3FF) << 10) | (u1 & 0x3FF);
    }
    if (u <= 0x7F) {
      if (outIdx >= endIdx) break;
      heap[outIdx++] = u;
    } else if (u <= 0x7FF) {
      if (outIdx + 1 >= endIdx) break;
      heap[outIdx++] = 0xC0 | (u >> 6);
      heap[outIdx++] = 0x80 | (u & 63);
    } else if (u <= 0xFFFF) {
      if (outIdx + 2 >= endIdx) break;
      heap[outIdx++] = 0xE0 | (u >> 12);
      heap[outIdx++] = 0x80 | ((u >> 6) & 63);
      heap[outIdx++] = 0x80 | (u & 63);
    } else {
      if (outIdx + 3 >= endIdx) break;
      if (u > 0x10FFFF) warnOnce('Invalid Unicode code point ' + ptrToString(u) + ' encountered when serializing a JS string to a UTF-8 string in wasm memory! (Valid unicode code points should be in range 0-0x10FFFF).');
      heap[outIdx++] = 0xF0 | (u >> 18);
      heap[outIdx++] = 0x80 | ((u >> 12) & 63);
      heap[outIdx++] = 0x80 | ((u >> 6) & 63);
      heap[outIdx++] = 0x80 | (u & 63);
    }
  }
  // Null-terminate the pointer to the buffer.
  heap[outIdx] = 0;
  return outIdx - startIdx;
}

/**
 * Copies the given Javascript String object 'str' to the emscripten HEAP at
 * address 'outPtr', null-terminated and encoded in UTF8 form. The copy will
 * require at most str.length*4+1 bytes of space in the HEAP.
 * Use the function lengthBytesUTF8 to compute the exact number of bytes
 * (excluding null terminator) that this function will write.
 *
 * @return {number} The number of bytes written, EXCLUDING the null terminator.
 */
function stringToUTF8(str, outPtr, maxBytesToWrite) {
  assert(typeof maxBytesToWrite == 'number', 'stringToUTF8(str, outPtr, maxBytesToWrite) is missing the third parameter that specifies the length of the output buffer!');
  return stringToUTF8Array(str, HEAPU8,outPtr, maxBytesToWrite);
}

/**
 * Returns the number of bytes the given Javascript string takes if encoded as a
 * UTF8 byte array, EXCLUDING the null terminator byte.
 *
 * @param {string} str - JavaScript string to operator on
 * @return {number} Length, in bytes, of the UTF8 encoded string.
 */
function lengthBytesUTF8(str) {
  var len = 0;
  for (var i = 0; i < str.length; ++i) {
    // Gotcha: charCodeAt returns a 16-bit word that is a UTF-16 encoded code
    // unit, not a Unicode code point of the character! So decode
    // UTF16->UTF32->UTF8.
    // See http://unicode.org/faq/utf_bom.html#utf16-3
    var c = str.charCodeAt(i); // possibly a lead surrogate
    if (c <= 0x7F) {
      len++;
    } else if (c <= 0x7FF) {
      len += 2;
    } else if (c >= 0xD800 && c <= 0xDFFF) {
      len += 4; ++i;
    } else {
      len += 3;
    }
  }
  return len;
}

// end include: runtime_strings.js
// Memory management

var HEAP,
/** @type {!Int8Array} */
  HEAP8,
/** @type {!Uint8Array} */
  HEAPU8,
/** @type {!Int16Array} */
  HEAP16,
/** @type {!Uint16Array} */
  HEAPU16,
/** @type {!Int32Array} */
  HEAP32,
/** @type {!Uint32Array} */
  HEAPU32,
/** @type {!Float32Array} */
  HEAPF32,
/** @type {!Float64Array} */
  HEAPF64;

function updateMemoryViews() {
  var b = wasmMemory.buffer;
  Module['HEAP8'] = HEAP8 = new Int8Array(b);
  Module['HEAP16'] = HEAP16 = new Int16Array(b);
  Module['HEAP32'] = HEAP32 = new Int32Array(b);
  Module['HEAPU8'] = HEAPU8 = new Uint8Array(b);
  Module['HEAPU16'] = HEAPU16 = new Uint16Array(b);
  Module['HEAPU32'] = HEAPU32 = new Uint32Array(b);
  Module['HEAPF32'] = HEAPF32 = new Float32Array(b);
  Module['HEAPF64'] = HEAPF64 = new Float64Array(b);
}

assert(!Module['STACK_SIZE'], 'STACK_SIZE can no longer be set at runtime.  Use -sSTACK_SIZE at link time')

assert(typeof Int32Array != 'undefined' && typeof Float64Array !== 'undefined' && Int32Array.prototype.subarray != undefined && Int32Array.prototype.set != undefined,
       'JS engine does not provide full typed array support');

// If memory is defined in wasm, the user can't provide it, or set INITIAL_MEMORY
assert(!Module['wasmMemory'], 'Use of `wasmMemory` detected.  Use -sIMPORTED_MEMORY to define wasmMemory externally');
assert(!Module['INITIAL_MEMORY'], 'Detected runtime INITIAL_MEMORY setting.  Use -sIMPORTED_MEMORY to define wasmMemory dynamically');

// include: runtime_init_table.js
// In regular non-RELOCATABLE mode the table is exported
// from the wasm module and this will be assigned once
// the exports are available.
var wasmTable;

// end include: runtime_init_table.js
// include: runtime_stack_check.js
// Initializes the stack cookie. Called at the startup of main and at the startup of each thread in pthreads mode.
function writeStackCookie() {
  var max = _emscripten_stack_get_end();
  assert((max & 3) == 0);
  // If the stack ends at address zero we write our cookies 4 bytes into the
  // stack.  This prevents interference with the (separate) address-zero check
  // below.
  if (max == 0) {
    max += 4;
  }
  // The stack grow downwards towards _emscripten_stack_get_end.
  // We write cookies to the final two words in the stack and detect if they are
  // ever overwritten.
  HEAPU32[((max)>>2)] = 0x02135467;
  HEAPU32[(((max)+(4))>>2)] = 0x89BACDFE;
  // Also test the global address 0 for integrity.
  HEAPU32[0] = 0x63736d65; /* 'emsc' */
}

function checkStackCookie() {
  if (ABORT) return;
  var max = _emscripten_stack_get_end();
  // See writeStackCookie().
  if (max == 0) {
    max += 4;
  }
  var cookie1 = HEAPU32[((max)>>2)];
  var cookie2 = HEAPU32[(((max)+(4))>>2)];
  if (cookie1 != 0x02135467 || cookie2 != 0x89BACDFE) {
    abort('Stack overflow! Stack cookie has been overwritten at ' + ptrToString(max) + ', expected hex dwords 0x89BACDFE and 0x2135467, but received ' + ptrToString(cookie2) + ' ' + ptrToString(cookie1));
  }
  // Also test the global address 0 for integrity.
  if (HEAPU32[0] !== 0x63736d65 /* 'emsc' */) {
    abort('Runtime error: The application has corrupted its heap memory area (address zero)!');
  }
}

// end include: runtime_stack_check.js
// include: runtime_assertions.js
// Endianness check
(function() {
  var h16 = new Int16Array(1);
  var h8 = new Int8Array(h16.buffer);
  h16[0] = 0x6373;
  if (h8[0] !== 0x73 || h8[1] !== 0x63) throw 'Runtime error: expected the system to be little-endian! (Run with -sSUPPORT_BIG_ENDIAN to bypass)';
})();

// end include: runtime_assertions.js
var __ATPRERUN__  = []; // functions called before the runtime is initialized
var __ATINIT__    = []; // functions called during startup
var __ATEXIT__    = []; // functions called during shutdown
var __ATPOSTRUN__ = []; // functions called after the main() is called

var runtimeInitialized = false;

var runtimeKeepaliveCounter = 0;

function keepRuntimeAlive() {
  return noExitRuntime || runtimeKeepaliveCounter > 0;
}

function preRun() {
  if (Module['preRun']) {
    if (typeof Module['preRun'] == 'function') Module['preRun'] = [Module['preRun']];
    while (Module['preRun'].length) {
      addOnPreRun(Module['preRun'].shift());
    }
  }
  callRuntimeCallbacks(__ATPRERUN__);
}

function initRuntime() {
  assert(!runtimeInitialized);
  runtimeInitialized = true;

  checkStackCookie();

  
  callRuntimeCallbacks(__ATINIT__);
}

function postRun() {
  checkStackCookie();

  if (Module['postRun']) {
    if (typeof Module['postRun'] == 'function') Module['postRun'] = [Module['postRun']];
    while (Module['postRun'].length) {
      addOnPostRun(Module['postRun'].shift());
    }
  }

  callRuntimeCallbacks(__ATPOSTRUN__);
}

function addOnPreRun(cb) {
  __ATPRERUN__.unshift(cb);
}

function addOnInit(cb) {
  __ATINIT__.unshift(cb);
}

function addOnExit(cb) {
}

function addOnPostRun(cb) {
  __ATPOSTRUN__.unshift(cb);
}

// include: runtime_math.js
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/imul

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/fround

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/clz32

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/trunc

assert(Math.imul, 'This browser does not support Math.imul(), build with LEGACY_VM_SUPPORT or POLYFILL_OLD_MATH_FUNCTIONS to add in a polyfill');
assert(Math.fround, 'This browser does not support Math.fround(), build with LEGACY_VM_SUPPORT or POLYFILL_OLD_MATH_FUNCTIONS to add in a polyfill');
assert(Math.clz32, 'This browser does not support Math.clz32(), build with LEGACY_VM_SUPPORT or POLYFILL_OLD_MATH_FUNCTIONS to add in a polyfill');
assert(Math.trunc, 'This browser does not support Math.trunc(), build with LEGACY_VM_SUPPORT or POLYFILL_OLD_MATH_FUNCTIONS to add in a polyfill');

// end include: runtime_math.js
// A counter of dependencies for calling run(). If we need to
// do asynchronous work before running, increment this and
// decrement it. Incrementing must happen in a place like
// Module.preRun (used by emcc to add file preloading).
// Note that you can add dependencies in preRun, even though
// it happens right before run - run will be postponed until
// the dependencies are met.
var runDependencies = 0;
var runDependencyWatcher = null;
var dependenciesFulfilled = null; // overridden to take different actions when all run dependencies are fulfilled
var runDependencyTracking = {};

function getUniqueRunDependency(id) {
  var orig = id;
  while (1) {
    if (!runDependencyTracking[id]) return id;
    id = orig + Math.random();
  }
}

function addRunDependency(id) {
  runDependencies++;

  if (Module['monitorRunDependencies']) {
    Module['monitorRunDependencies'](runDependencies);
  }

  if (id) {
    assert(!runDependencyTracking[id]);
    runDependencyTracking[id] = 1;
    if (runDependencyWatcher === null && typeof setInterval != 'undefined') {
      // Check for missing dependencies every few seconds
      runDependencyWatcher = setInterval(function() {
        if (ABORT) {
          clearInterval(runDependencyWatcher);
          runDependencyWatcher = null;
          return;
        }
        var shown = false;
        for (var dep in runDependencyTracking) {
          if (!shown) {
            shown = true;
            err('still waiting on run dependencies:');
          }
          err('dependency: ' + dep);
        }
        if (shown) {
          err('(end of list)');
        }
      }, 10000);
    }
  } else {
    err('warning: run dependency added without ID');
  }
}

function removeRunDependency(id) {
  runDependencies--;

  if (Module['monitorRunDependencies']) {
    Module['monitorRunDependencies'](runDependencies);
  }

  if (id) {
    assert(runDependencyTracking[id]);
    delete runDependencyTracking[id];
  } else {
    err('warning: run dependency removed without ID');
  }
  if (runDependencies == 0) {
    if (runDependencyWatcher !== null) {
      clearInterval(runDependencyWatcher);
      runDependencyWatcher = null;
    }
    if (dependenciesFulfilled) {
      var callback = dependenciesFulfilled;
      dependenciesFulfilled = null;
      callback(); // can add another dependenciesFulfilled
    }
  }
}

/** @param {string|number=} what */
function abort(what) {
  if (Module['onAbort']) {
    Module['onAbort'](what);
  }

  what = 'Aborted(' + what + ')';
  // TODO(sbc): Should we remove printing and leave it up to whoever
  // catches the exception?
  err(what);

  ABORT = true;
  EXITSTATUS = 1;

  // Use a wasm runtime error, because a JS error might be seen as a foreign
  // exception, which means we'd run destructors on it. We need the error to
  // simply make the program stop.
  // FIXME This approach does not work in Wasm EH because it currently does not assume
  // all RuntimeErrors are from traps; it decides whether a RuntimeError is from
  // a trap or not based on a hidden field within the object. So at the moment
  // we don't have a way of throwing a wasm trap from JS. TODO Make a JS API that
  // allows this in the wasm spec.

  // Suppress closure compiler warning here. Closure compiler's builtin extern
  // defintion for WebAssembly.RuntimeError claims it takes no arguments even
  // though it can.
  // TODO(https://github.com/google/closure-compiler/pull/3913): Remove if/when upstream closure gets fixed.
  /** @suppress {checkTypes} */
  var e = new WebAssembly.RuntimeError(what);

  // Throw the error whether or not MODULARIZE is set because abort is used
  // in code paths apart from instantiation where an exception is expected
  // to be thrown when abort is called.
  throw e;
}

// include: memoryprofiler.js
// end include: memoryprofiler.js
// show errors on likely calls to FS when it was not included
var FS = {
  error: function() {
    abort('Filesystem support (FS) was not included. The problem is that you are using files from JS, but files were not used from C/C++, so filesystem support was not auto-included. You can force-include filesystem support with -sFORCE_FILESYSTEM');
  },
  init: function() { FS.error() },
  createDataFile: function() { FS.error() },
  createPreloadedFile: function() { FS.error() },
  createLazyFile: function() { FS.error() },
  open: function() { FS.error() },
  mkdev: function() { FS.error() },
  registerDevice: function() { FS.error() },
  analyzePath: function() { FS.error() },
  loadFilesFromDB: function() { FS.error() },

  ErrnoError: function ErrnoError() { FS.error() },
};
Module['FS_createDataFile'] = FS.createDataFile;
Module['FS_createPreloadedFile'] = FS.createPreloadedFile;

// include: URIUtils.js
// Prefix of data URIs emitted by SINGLE_FILE and related options.
var dataURIPrefix = 'data:application/octet-stream;base64,';

// Indicates whether filename is a base64 data URI.
function isDataURI(filename) {
  // Prefix of data URIs emitted by SINGLE_FILE and related options.
  return filename.startsWith(dataURIPrefix);
}

// Indicates whether filename is delivered via file protocol (as opposed to http/https)
function isFileURI(filename) {
  return filename.startsWith('file://');
}

// end include: URIUtils.js
/** @param {boolean=} fixedasm */
function createExportWrapper(name, fixedasm) {
  return function() {
    var displayName = name;
    var asm = fixedasm;
    if (!fixedasm) {
      asm = Module['asm'];
    }
    assert(runtimeInitialized, 'native function `' + displayName + '` called before runtime initialization');
    if (!asm[name]) {
      assert(asm[name], 'exported native function `' + displayName + '` not found');
    }
    return asm[name].apply(null, arguments);
  };
}

// include: runtime_exceptions.js
// Base Emscripten EH error class
class EmscriptenEH extends Error {}

class EmscriptenSjLj extends EmscriptenEH {}

class CppException extends EmscriptenEH {
  constructor(excPtr) {
    super(excPtr);
    const excInfo = getExceptionMessage(excPtr);
    this.name = excInfo[0];
    this.message = excInfo[1];
  }
}

// end include: runtime_exceptions.js
var wasmBinaryFile;
  wasmBinaryFile = 'behave-min.wasm';
  if (!isDataURI(wasmBinaryFile)) {
    wasmBinaryFile = locateFile(wasmBinaryFile);
  }

function getBinary(file) {
  try {
    if (file == wasmBinaryFile && wasmBinary) {
      return new Uint8Array(wasmBinary);
    }
    if (readBinary) {
      return readBinary(file);
    }
    throw "both async and sync fetching of the wasm failed";
  }
  catch (err) {
    abort(err);
  }
}

function getBinaryPromise(binaryFile) {
  // If we don't have the binary yet, try to to load it asynchronously.
  // Fetch has some additional restrictions over XHR, like it can't be used on a file:// url.
  // See https://github.com/github/fetch/pull/92#issuecomment-140665932
  // Cordova or Electron apps are typically loaded from a file:// url.
  // So use fetch if it is available and the url is not a file, otherwise fall back to XHR.
  if (!wasmBinary && (ENVIRONMENT_IS_WEB || ENVIRONMENT_IS_WORKER)) {
    if (typeof fetch == 'function'
      && !isFileURI(binaryFile)
    ) {
      return fetch(binaryFile, { credentials: 'same-origin' }).then(function(response) {
        if (!response['ok']) {
          throw "failed to load wasm binary file at '" + binaryFile + "'";
        }
        return response['arrayBuffer']();
      }).catch(function () {
          return getBinary(binaryFile);
      });
    }
    else {
      if (readAsync) {
        // fetch is not available or url is file => try XHR (readAsync uses XHR internally)
        return new Promise(function(resolve, reject) {
          readAsync(binaryFile, function(response) { resolve(new Uint8Array(/** @type{!ArrayBuffer} */(response))) }, reject)
        });
      }
    }
  }

  // Otherwise, getBinary should be able to get it synchronously
  return Promise.resolve().then(function() { return getBinary(binaryFile); });
}

function instantiateArrayBuffer(binaryFile, imports, receiver) {
  return getBinaryPromise(binaryFile).then(function(binary) {
    return WebAssembly.instantiate(binary, imports);
  }).then(function (instance) {
    return instance;
  }).then(receiver, function(reason) {
    err('failed to asynchronously prepare wasm: ' + reason);

    // Warn on some common problems.
    if (isFileURI(wasmBinaryFile)) {
      err('warning: Loading from a file URI (' + wasmBinaryFile + ') is not supported in most browsers. See https://emscripten.org/docs/getting_started/FAQ.html#how-do-i-run-a-local-webserver-for-testing-why-does-my-program-stall-in-downloading-or-preparing');
    }
    abort(reason);
  });
}

function instantiateAsync(binary, binaryFile, imports, callback) {
  if (!binary &&
      typeof WebAssembly.instantiateStreaming == 'function' &&
      !isDataURI(binaryFile) &&
      // Don't use streaming for file:// delivered objects in a webview, fetch them synchronously.
      !isFileURI(binaryFile) &&
      // Avoid instantiateStreaming() on Node.js environment for now, as while
      // Node.js v18.1.0 implements it, it does not have a full fetch()
      // implementation yet.
      //
      // Reference:
      //   https://github.com/emscripten-core/emscripten/pull/16917
      !ENVIRONMENT_IS_NODE &&
      typeof fetch == 'function') {
    return fetch(binaryFile, { credentials: 'same-origin' }).then(function(response) {
      // Suppress closure warning here since the upstream definition for
      // instantiateStreaming only allows Promise<Repsponse> rather than
      // an actual Response.
      // TODO(https://github.com/google/closure-compiler/pull/3913): Remove if/when upstream closure is fixed.
      /** @suppress {checkTypes} */
      var result = WebAssembly.instantiateStreaming(response, imports);

      return result.then(
        callback,
        function(reason) {
          // We expect the most common failure cause to be a bad MIME type for the binary,
          // in which case falling back to ArrayBuffer instantiation should work.
          err('wasm streaming compile failed: ' + reason);
          err('falling back to ArrayBuffer instantiation');
          return instantiateArrayBuffer(binaryFile, imports, callback);
        });
    });
  } else {
    return instantiateArrayBuffer(binaryFile, imports, callback);
  }
}

// Create the wasm instance.
// Receives the wasm imports, returns the exports.
function createWasm() {
  // prepare imports
  var info = {
    'env': wasmImports,
    'wasi_snapshot_preview1': wasmImports,
  };
  // Load the wasm module and create an instance of using native support in the JS engine.
  // handle a generated wasm instance, receiving its exports and
  // performing other necessary setup
  /** @param {WebAssembly.Module=} module*/
  function receiveInstance(instance, module) {
    var exports = instance.exports;

    Module['asm'] = exports;

    wasmMemory = Module['asm']['memory'];
    assert(wasmMemory, "memory not found in wasm exports");
    // This assertion doesn't hold when emscripten is run in --post-link
    // mode.
    // TODO(sbc): Read INITIAL_MEMORY out of the wasm file in post-link mode.
    //assert(wasmMemory.buffer.byteLength === 16777216);
    updateMemoryViews();

    wasmTable = Module['asm']['__indirect_function_table'];
    assert(wasmTable, "table not found in wasm exports");

    addOnInit(Module['asm']['__wasm_call_ctors']);

    removeRunDependency('wasm-instantiate');

    return exports;
  }
  // wait for the pthread pool (if any)
  addRunDependency('wasm-instantiate');

  // Prefer streaming instantiation if available.
  // Async compilation can be confusing when an error on the page overwrites Module
  // (for example, if the order of elements is wrong, and the one defining Module is
  // later), so we save Module and check it later.
  var trueModule = Module;
  function receiveInstantiationResult(result) {
    // 'result' is a ResultObject object which has both the module and instance.
    // receiveInstance() will swap in the exports (to Module.asm) so they can be called
    assert(Module === trueModule, 'the Module object should not be replaced during async compilation - perhaps the order of HTML elements is wrong?');
    trueModule = null;
    // TODO: Due to Closure regression https://github.com/google/closure-compiler/issues/3193, the above line no longer optimizes out down to the following line.
    // When the regression is fixed, can restore the above PTHREADS-enabled path.
    receiveInstance(result['instance']);
  }

  // User shell pages can write their own Module.instantiateWasm = function(imports, successCallback) callback
  // to manually instantiate the Wasm module themselves. This allows pages to run the instantiation parallel
  // to any other async startup actions they are performing.
  // Also pthreads and wasm workers initialize the wasm instance through this path.
  if (Module['instantiateWasm']) {
    try {
      return Module['instantiateWasm'](info, receiveInstance);
    } catch(e) {
      err('Module.instantiateWasm callback failed with error: ' + e);
        return false;
    }
  }

  instantiateAsync(wasmBinary, wasmBinaryFile, info, receiveInstantiationResult);
  return {}; // no exports yet; we'll fill them in later
}

// Globals used by JS i64 conversions (see makeSetValue)
var tempDouble;
var tempI64;

// include: runtime_debug.js
function legacyModuleProp(prop, newName) {
  if (!Object.getOwnPropertyDescriptor(Module, prop)) {
    Object.defineProperty(Module, prop, {
      configurable: true,
      get: function() {
        abort('Module.' + prop + ' has been replaced with plain ' + newName + ' (the initial value can be provided on Module, but after startup the value is only looked for on a local variable of that name)');
      }
    });
  }
}

function ignoredModuleProp(prop) {
  if (Object.getOwnPropertyDescriptor(Module, prop)) {
    abort('`Module.' + prop + '` was supplied but `' + prop + '` not included in INCOMING_MODULE_JS_API');
  }
}

// forcing the filesystem exports a few things by default
function isExportedByForceFilesystem(name) {
  return name === 'FS_createPath' ||
         name === 'FS_createDataFile' ||
         name === 'FS_createPreloadedFile' ||
         name === 'FS_unlink' ||
         name === 'addRunDependency' ||
         // The old FS has some functionality that WasmFS lacks.
         name === 'FS_createLazyFile' ||
         name === 'FS_createDevice' ||
         name === 'removeRunDependency';
}

function missingGlobal(sym, msg) {
  if (typeof globalThis !== 'undefined') {
    Object.defineProperty(globalThis, sym, {
      configurable: true,
      get: function() {
        warnOnce('`' + sym + '` is not longer defined by emscripten. ' + msg);
        return undefined;
      }
    });
  }
}

missingGlobal('buffer', 'Please use HEAP8.buffer or wasmMemory.buffer');

function missingLibrarySymbol(sym) {
  if (typeof globalThis !== 'undefined' && !Object.getOwnPropertyDescriptor(globalThis, sym)) {
    Object.defineProperty(globalThis, sym, {
      configurable: true,
      get: function() {
        // Can't `abort()` here because it would break code that does runtime
        // checks.  e.g. `if (typeof SDL === 'undefined')`.
        var msg = '`' + sym + '` is a library symbol and not included by default; add it to your library.js __deps or to DEFAULT_LIBRARY_FUNCS_TO_INCLUDE on the command line';
        // DEFAULT_LIBRARY_FUNCS_TO_INCLUDE requires the name as it appears in
        // library.js, which means $name for a JS name with no prefix, or name
        // for a JS name like _name.
        var librarySymbol = sym;
        if (!librarySymbol.startsWith('_')) {
          librarySymbol = '$' + sym;
        }
        msg += " (e.g. -sDEFAULT_LIBRARY_FUNCS_TO_INCLUDE=" + librarySymbol + ")";
        if (isExportedByForceFilesystem(sym)) {
          msg += '. Alternatively, forcing filesystem support (-sFORCE_FILESYSTEM) can export this for you';
        }
        warnOnce(msg);
        return undefined;
      }
    });
  }
  // Any symbol that is not included from the JS libary is also (by definition)
  // not exported on the Module object.
  unexportedRuntimeSymbol(sym);
}

function unexportedRuntimeSymbol(sym) {
  if (!Object.getOwnPropertyDescriptor(Module, sym)) {
    Object.defineProperty(Module, sym, {
      configurable: true,
      get: function() {
        var msg = "'" + sym + "' was not exported. add it to EXPORTED_RUNTIME_METHODS (see the FAQ)";
        if (isExportedByForceFilesystem(sym)) {
          msg += '. Alternatively, forcing filesystem support (-sFORCE_FILESYSTEM) can export this for you';
        }
        abort(msg);
      }
    });
  }
}

// Used by XXXXX_DEBUG settings to output debug messages.
function dbg(text) {
  // TODO(sbc): Make this configurable somehow.  Its not always convenient for
  // logging to show up as errors.
  console.error(text);
}

// end include: runtime_debug.js
// === Body ===

function array_bounds_check_error(idx,size) { throw 'Array index ' + idx + ' out of bounds: [0,' + size + ')'; }



// end include: preamble.js

  /** @constructor */
  function ExitStatus(status) {
      this.name = 'ExitStatus';
      this.message = 'Program terminated with exit(' + status + ')';
      this.status = status;
    }

  function callRuntimeCallbacks(callbacks) {
      while (callbacks.length > 0) {
        // Pass the module as the first argument.
        callbacks.shift()(Module);
      }
    }

  
  var wasmTableMirror = [];
  
  function getWasmTableEntry(funcPtr) {
      var func = wasmTableMirror[funcPtr];
      if (!func) {
        if (funcPtr >= wasmTableMirror.length) wasmTableMirror.length = funcPtr + 1;
        wasmTableMirror[funcPtr] = func = wasmTable.get(funcPtr);
      }
      assert(wasmTable.get(funcPtr) == func, "JavaScript-side Wasm function table mirror is out of date!");
      return func;
    }
  function exception_decRef(info) {
      // A rethrown exception can reach refcount 0; it must not be discarded
      // Its next handler will clear the rethrown flag and addRef it, prior to
      // final decRef and destruction here
      if (info.release_ref() && !info.get_rethrown()) {
        var destructor = info.get_destructor();
        if (destructor) {
          // In Wasm, destructors return 'this' as in ARM
          getWasmTableEntry(destructor)(info.excPtr);
        }
        ___cxa_free_exception(info.excPtr);
      }
    }
  
  /** @constructor */
  function ExceptionInfo(excPtr) {
      this.excPtr = excPtr;
      this.ptr = excPtr - 24;
  
      this.set_type = function(type) {
        HEAPU32[(((this.ptr)+(4))>>2)] = type;
      };
  
      this.get_type = function() {
        return HEAPU32[(((this.ptr)+(4))>>2)];
      };
  
      this.set_destructor = function(destructor) {
        HEAPU32[(((this.ptr)+(8))>>2)] = destructor;
      };
  
      this.get_destructor = function() {
        return HEAPU32[(((this.ptr)+(8))>>2)];
      };
  
      this.set_refcount = function(refcount) {
        HEAP32[((this.ptr)>>2)] = refcount;
      };
  
      this.set_caught = function (caught) {
        caught = caught ? 1 : 0;
        HEAP8[(((this.ptr)+(12))>>0)] = caught;
      };
  
      this.get_caught = function () {
        return HEAP8[(((this.ptr)+(12))>>0)] != 0;
      };
  
      this.set_rethrown = function (rethrown) {
        rethrown = rethrown ? 1 : 0;
        HEAP8[(((this.ptr)+(13))>>0)] = rethrown;
      };
  
      this.get_rethrown = function () {
        return HEAP8[(((this.ptr)+(13))>>0)] != 0;
      };
  
      // Initialize native structure fields. Should be called once after allocated.
      this.init = function(type, destructor) {
        this.set_adjusted_ptr(0);
        this.set_type(type);
        this.set_destructor(destructor);
        this.set_refcount(0);
        this.set_caught(false);
        this.set_rethrown(false);
      }
  
      this.add_ref = function() {
        var value = HEAP32[((this.ptr)>>2)];
        HEAP32[((this.ptr)>>2)] = value + 1;
      };
  
      // Returns true if last reference released.
      this.release_ref = function() {
        var prev = HEAP32[((this.ptr)>>2)];
        HEAP32[((this.ptr)>>2)] = prev - 1;
        assert(prev > 0);
        return prev === 1;
      };
  
      this.set_adjusted_ptr = function(adjustedPtr) {
        HEAPU32[(((this.ptr)+(16))>>2)] = adjustedPtr;
      };
  
      this.get_adjusted_ptr = function() {
        return HEAPU32[(((this.ptr)+(16))>>2)];
      };
  
      // Get pointer which is expected to be received by catch clause in C++ code. It may be adjusted
      // when the pointer is casted to some of the exception object base classes (e.g. when virtual
      // inheritance is used). When a pointer is thrown this method should return the thrown pointer
      // itself.
      this.get_exception_ptr = function() {
        // Work around a fastcomp bug, this code is still included for some reason in a build without
        // exceptions support.
        var isPointer = ___cxa_is_pointer_type(this.get_type());
        if (isPointer) {
          return HEAPU32[((this.excPtr)>>2)];
        }
        var adjusted = this.get_adjusted_ptr();
        if (adjusted !== 0) return adjusted;
        return this.excPtr;
      };
    }
  function ___cxa_decrement_exception_refcount(ptr) {
      if (!ptr) return;
      exception_decRef(new ExceptionInfo(ptr));
    }
  function decrementExceptionRefcount(ptr) {
      ___cxa_decrement_exception_refcount(ptr);
    }

  
  
  function withStackSave(f) {
      var stack = stackSave();
      var ret = f();
      stackRestore(stack);
      return ret;
    }
  function getExceptionMessageCommon(ptr) {
      return withStackSave(function() {
        var type_addr_addr = stackAlloc(4);
        var message_addr_addr = stackAlloc(4);
        ___get_exception_message(ptr, type_addr_addr, message_addr_addr);
        var type_addr = HEAPU32[((type_addr_addr)>>2)];
        var message_addr = HEAPU32[((message_addr_addr)>>2)];
        var type = UTF8ToString(type_addr);
        _free(type_addr);
        var message;
        if (message_addr) {
          message = UTF8ToString(message_addr);
          _free(message_addr);
        }
        return [type, message];
      });
    }
  function getExceptionMessage(ptr) {
      return getExceptionMessageCommon(ptr);
    }
  Module["getExceptionMessage"] = getExceptionMessage;

  
    /**
     * @param {number} ptr
     * @param {string} type
     */
  function getValue(ptr, type = 'i8') {
    if (type.endsWith('*')) type = '*';
    switch (type) {
      case 'i1': return HEAP8[((ptr)>>0)];
      case 'i8': return HEAP8[((ptr)>>0)];
      case 'i16': return HEAP16[((ptr)>>1)];
      case 'i32': return HEAP32[((ptr)>>2)];
      case 'i64': return HEAP32[((ptr)>>2)];
      case 'float': return HEAPF32[((ptr)>>2)];
      case 'double': return HEAPF64[((ptr)>>3)];
      case '*': return HEAPU32[((ptr)>>2)];
      default: abort('invalid type for getValue: ' + type);
    }
  }

  function exception_addRef(info) {
      info.add_ref();
    }
  
  function ___cxa_increment_exception_refcount(ptr) {
      if (!ptr) return;
      exception_addRef(new ExceptionInfo(ptr));
    }
  function incrementExceptionRefcount(ptr) {
      ___cxa_increment_exception_refcount(ptr);
    }

  function ptrToString(ptr) {
      assert(typeof ptr === 'number');
      return '0x' + ptr.toString(16).padStart(8, '0');
    }

  
    /**
     * @param {number} ptr
     * @param {number} value
     * @param {string} type
     */
  function setValue(ptr, value, type = 'i8') {
    if (type.endsWith('*')) type = '*';
    switch (type) {
      case 'i1': HEAP8[((ptr)>>0)] = value; break;
      case 'i8': HEAP8[((ptr)>>0)] = value; break;
      case 'i16': HEAP16[((ptr)>>1)] = value; break;
      case 'i32': HEAP32[((ptr)>>2)] = value; break;
      case 'i64': (tempI64 = [value>>>0,(tempDouble=value,(+(Math.abs(tempDouble))) >= 1.0 ? (tempDouble > 0.0 ? ((Math.min((+(Math.floor((tempDouble)/4294967296.0))), 4294967295.0))|0)>>>0 : (~~((+(Math.ceil((tempDouble - +(((~~(tempDouble)))>>>0))/4294967296.0)))))>>>0) : 0)],HEAP32[((ptr)>>2)] = tempI64[0],HEAP32[(((ptr)+(4))>>2)] = tempI64[1]); break;
      case 'float': HEAPF32[((ptr)>>2)] = value; break;
      case 'double': HEAPF64[((ptr)>>3)] = value; break;
      case '*': HEAPU32[((ptr)>>2)] = value; break;
      default: abort('invalid type for setValue: ' + type);
    }
  }

  function warnOnce(text) {
      if (!warnOnce.shown) warnOnce.shown = {};
      if (!warnOnce.shown[text]) {
        warnOnce.shown[text] = 1;
        if (ENVIRONMENT_IS_NODE) text = 'warning: ' + text;
        err(text);
      }
    }

  function ___assert_fail(condition, filename, line, func) {
      abort('Assertion failed: ' + UTF8ToString(condition) + ', at: ' + [filename ? UTF8ToString(filename) : 'unknown filename', line, func ? UTF8ToString(func) : 'unknown function']);
    }

  var exceptionCaught =  [];
  
  
  var uncaughtExceptionCount = 0;
  function ___cxa_begin_catch(ptr) {
      var info = new ExceptionInfo(ptr);
      if (!info.get_caught()) {
        info.set_caught(true);
        uncaughtExceptionCount--;
      }
      info.set_rethrown(false);
      exceptionCaught.push(info);
      exception_addRef(info);
      return info.get_exception_ptr();
    }

  
  var exceptionLast = 0;
  
  function ___cxa_end_catch() {
      // Clear state flag.
      _setThrew(0);
      assert(exceptionCaught.length > 0);
      // Call destructor if one is registered then clear it.
      var info = exceptionCaught.pop();
  
      exception_decRef(info);
      exceptionLast = 0; // XXX in decRef?
    }

  
  
  function ___resumeException(ptr) {
      if (!exceptionLast) { exceptionLast = ptr; }
      throw new CppException(ptr);
    }
  
  
  function ___cxa_find_matching_catch() {
      var thrown = exceptionLast;
      if (!thrown) {
        // just pass through the null ptr
        setTempRet0(0);
        return 0;
      }
      var info = new ExceptionInfo(thrown);
      info.set_adjusted_ptr(thrown);
      var thrownType = info.get_type();
      if (!thrownType) {
        // just pass through the thrown ptr
        setTempRet0(0);
        return thrown;
      }
  
      // can_catch receives a **, add indirection
      // The different catch blocks are denoted by different types.
      // Due to inheritance, those types may not precisely match the
      // type of the thrown object. Find one which matches, and
      // return the type of the catch block which should be called.
      for (var i = 0; i < arguments.length; i++) {
        var caughtType = arguments[i];
        if (caughtType === 0 || caughtType === thrownType) {
          // Catch all clause matched or exactly the same type is caught
          break;
        }
        var adjusted_ptr_addr = info.ptr + 16;
        if (___cxa_can_catch(caughtType, thrownType, adjusted_ptr_addr)) {
          setTempRet0(caughtType);
          return thrown;
        }
      }
      setTempRet0(thrownType);
      return thrown;
    }
  var ___cxa_find_matching_catch_2 = ___cxa_find_matching_catch;

  var ___cxa_find_matching_catch_3 = ___cxa_find_matching_catch;

  
  
  function ___cxa_rethrow() {
      var info = exceptionCaught.pop();
      if (!info) {
        abort('no exception to throw');
      }
      var ptr = info.excPtr;
      if (!info.get_rethrown()) {
        // Only pop if the corresponding push was through rethrow_primary_exception
        exceptionCaught.push(info);
        info.set_rethrown(true);
        info.set_caught(false);
        uncaughtExceptionCount++;
      }
      exceptionLast = ptr;
      throw new CppException(ptr);
    }

  
  
  function ___cxa_throw(ptr, type, destructor) {
      var info = new ExceptionInfo(ptr);
      // Initialize ExceptionInfo content after it was allocated in __cxa_allocate_exception.
      info.init(type, destructor);
      exceptionLast = ptr;
      uncaughtExceptionCount++;
      throw new CppException(ptr);
    }


  function _abort() {
      abort('native code called abort()');
    }

  function _emscripten_memcpy_big(dest, src, num) {
      HEAPU8.copyWithin(dest, src, src + num);
    }

  function getHeapMax() {
      // Stay one Wasm page short of 4GB: while e.g. Chrome is able to allocate
      // full 4GB Wasm memories, the size will wrap back to 0 bytes in Wasm side
      // for any code that deals with heap sizes, which would require special
      // casing all heap size related code to treat 0 specially.
      return 2147483648;
    }
  
  function emscripten_realloc_buffer(size) {
      var b = wasmMemory.buffer;
      try {
        // round size grow request up to wasm page size (fixed 64KB per spec)
        wasmMemory.grow((size - b.byteLength + 65535) >>> 16); // .grow() takes a delta compared to the previous size
        updateMemoryViews();
        return 1 /*success*/;
      } catch(e) {
        err('emscripten_realloc_buffer: Attempted to grow heap from ' + b.byteLength  + ' bytes to ' + size + ' bytes, but got error: ' + e);
      }
      // implicit 0 return to save code size (caller will cast "undefined" into 0
      // anyhow)
    }
  function _emscripten_resize_heap(requestedSize) {
      var oldSize = HEAPU8.length;
      requestedSize = requestedSize >>> 0;
      // With multithreaded builds, races can happen (another thread might increase the size
      // in between), so return a failure, and let the caller retry.
      assert(requestedSize > oldSize);
  
      // Memory resize rules:
      // 1.  Always increase heap size to at least the requested size, rounded up
      //     to next page multiple.
      // 2a. If MEMORY_GROWTH_LINEAR_STEP == -1, excessively resize the heap
      //     geometrically: increase the heap size according to
      //     MEMORY_GROWTH_GEOMETRIC_STEP factor (default +20%), At most
      //     overreserve by MEMORY_GROWTH_GEOMETRIC_CAP bytes (default 96MB).
      // 2b. If MEMORY_GROWTH_LINEAR_STEP != -1, excessively resize the heap
      //     linearly: increase the heap size by at least
      //     MEMORY_GROWTH_LINEAR_STEP bytes.
      // 3.  Max size for the heap is capped at 2048MB-WASM_PAGE_SIZE, or by
      //     MAXIMUM_MEMORY, or by ASAN limit, depending on which is smallest
      // 4.  If we were unable to allocate as much memory, it may be due to
      //     over-eager decision to excessively reserve due to (3) above.
      //     Hence if an allocation fails, cut down on the amount of excess
      //     growth, in an attempt to succeed to perform a smaller allocation.
  
      // A limit is set for how much we can grow. We should not exceed that
      // (the wasm binary specifies it, so if we tried, we'd fail anyhow).
      var maxHeapSize = getHeapMax();
      if (requestedSize > maxHeapSize) {
        err('Cannot enlarge memory, asked to go up to ' + requestedSize + ' bytes, but the limit is ' + maxHeapSize + ' bytes!');
        return false;
      }
  
      let alignUp = (x, multiple) => x + (multiple - x % multiple) % multiple;
  
      // Loop through potential heap size increases. If we attempt a too eager
      // reservation that fails, cut down on the attempted size and reserve a
      // smaller bump instead. (max 3 times, chosen somewhat arbitrarily)
      for (var cutDown = 1; cutDown <= 4; cutDown *= 2) {
        var overGrownHeapSize = oldSize * (1 + 0.2 / cutDown); // ensure geometric growth
        // but limit overreserving (default to capping at +96MB overgrowth at most)
        overGrownHeapSize = Math.min(overGrownHeapSize, requestedSize + 100663296 );
  
        var newSize = Math.min(maxHeapSize, alignUp(Math.max(requestedSize, overGrownHeapSize), 65536));
  
        var replacement = emscripten_realloc_buffer(newSize);
        if (replacement) {
  
          return true;
        }
      }
      err('Failed to grow the heap from ' + oldSize + ' bytes to ' + newSize + ' bytes, not enough memory!');
      return false;
    }

  var SYSCALLS = {varargs:undefined,get:function() {
        assert(SYSCALLS.varargs != undefined);
        SYSCALLS.varargs += 4;
        var ret = HEAP32[(((SYSCALLS.varargs)-(4))>>2)];
        return ret;
      },getStr:function(ptr) {
        var ret = UTF8ToString(ptr);
        return ret;
      }};
  function _fd_close(fd) {
      abort('fd_close called without SYSCALLS_REQUIRE_FILESYSTEM');
    }

  function convertI32PairToI53Checked(lo, hi) {
      assert(lo == (lo >>> 0) || lo == (lo|0)); // lo should either be a i32 or a u32
      assert(hi === (hi|0));                    // hi should be a i32
      return ((hi + 0x200000) >>> 0 < 0x400001 - !!lo) ? (lo >>> 0) + hi * 4294967296 : NaN;
    }
  
  
  
  
  function _fd_seek(fd, offset_low, offset_high, whence, newOffset) {
      return 70;
    }

  var printCharBuffers = [null,[],[]];
  function printChar(stream, curr) {
      var buffer = printCharBuffers[stream];
      assert(buffer);
      if (curr === 0 || curr === 10) {
        (stream === 1 ? out : err)(UTF8ArrayToString(buffer, 0));
        buffer.length = 0;
      } else {
        buffer.push(curr);
      }
    }
  
  function flush_NO_FILESYSTEM() {
      // flush anything remaining in the buffers during shutdown
      _fflush(0);
      if (printCharBuffers[1].length) printChar(1, 10);
      if (printCharBuffers[2].length) printChar(2, 10);
    }
  
  
  function _fd_write(fd, iov, iovcnt, pnum) {
      // hack to support printf in SYSCALLS_REQUIRE_FILESYSTEM=0
      var num = 0;
      for (var i = 0; i < iovcnt; i++) {
        var ptr = HEAPU32[((iov)>>2)];
        var len = HEAPU32[(((iov)+(4))>>2)];
        iov += 8;
        for (var j = 0; j < len; j++) {
          printChar(fd, HEAPU8[ptr+j]);
        }
        num += len;
      }
      HEAPU32[((pnum)>>2)] = num;
      return 0;
    }

  /** @type {function(string, boolean=, number=)} */
  function intArrayFromString(stringy, dontAddNull, length) {
    var len = length > 0 ? length : lengthBytesUTF8(stringy)+1;
    var u8array = new Array(len);
    var numBytesWritten = stringToUTF8Array(stringy, u8array, 0, u8array.length);
    if (dontAddNull) u8array.length = numBytesWritten;
    return u8array;
  }


  function allocateUTF8(str) {
      var size = lengthBytesUTF8(str) + 1;
      var ret = _malloc(size);
      if (ret) stringToUTF8Array(str, HEAP8, ret, size);
      return ret;
    }

  function uleb128Encode(n, target) {
      assert(n < 16384);
      if (n < 128) {
        target.push(n);
      } else {
        target.push((n % 128) | 128, n >> 7);
      }
    }
  
  function sigToWasmTypes(sig) {
      var typeNames = {
        'i': 'i32',
        // i64 values will be split into two i32s.
        'j': 'i32',
        'f': 'f32',
        'd': 'f64',
        'p': 'i32',
      };
      var type = {
        parameters: [],
        results: sig[0] == 'v' ? [] : [typeNames[sig[0]]]
      };
      for (var i = 1; i < sig.length; ++i) {
        assert(sig[i] in typeNames, 'invalid signature char: ' + sig[i]);
        type.parameters.push(typeNames[sig[i]]);
        if (sig[i] === 'j') {
          type.parameters.push('i32');
        }
      }
      return type;
    }
  
  function generateFuncType(sig, target){
      var sigRet = sig.slice(0, 1);
      var sigParam = sig.slice(1);
      var typeCodes = {
        'i': 0x7f, // i32
        'p': 0x7f, // i32
        'j': 0x7e, // i64
        'f': 0x7d, // f32
        'd': 0x7c, // f64
      };
    
      // Parameters, length + signatures
      target.push(0x60 /* form: func */);
      uleb128Encode(sigParam.length, target);
      for (var i = 0; i < sigParam.length; ++i) {
        assert(sigParam[i] in typeCodes, 'invalid signature char: ' + sigParam[i]);
    target.push(typeCodes[sigParam[i]]);
      }
    
      // Return values, length + signatures
      // With no multi-return in MVP, either 0 (void) or 1 (anything else)
      if (sigRet == 'v') {
        target.push(0x00);
      } else {
        target.push(0x01, typeCodes[sigRet]);
      }
    }
  function convertJsFunctionToWasm(func, sig) {
  
      // If the type reflection proposal is available, use the new
      // "WebAssembly.Function" constructor.
      // Otherwise, construct a minimal wasm module importing the JS function and
      // re-exporting it.
      if (typeof WebAssembly.Function == "function") {
        return new WebAssembly.Function(sigToWasmTypes(sig), func);
      }
  
      // The module is static, with the exception of the type section, which is
      // generated based on the signature passed in.
      var typeSectionBody = [
        0x01, // count: 1
      ];
      generateFuncType(sig, typeSectionBody);
  
      // Rest of the module is static
      var bytes = [
        0x00, 0x61, 0x73, 0x6d, // magic ("\0asm")
        0x01, 0x00, 0x00, 0x00, // version: 1
        0x01, // Type section code
      ];
      // Write the overall length of the type section followed by the body
      uleb128Encode(typeSectionBody.length, bytes);
      bytes.push.apply(bytes, typeSectionBody);
  
      // The rest of the module is static
      bytes.push(
        0x02, 0x07, // import section
          // (import "e" "f" (func 0 (type 0)))
          0x01, 0x01, 0x65, 0x01, 0x66, 0x00, 0x00,
        0x07, 0x05, // export section
          // (export "f" (func 0 (type 0)))
          0x01, 0x01, 0x66, 0x00, 0x00,
      );
  
      // We can compile this wasm module synchronously because it is very small.
      // This accepts an import (at "e.f"), that it reroutes to an export (at "f")
      var module = new WebAssembly.Module(new Uint8Array(bytes));
      var instance = new WebAssembly.Instance(module, { 'e': { 'f': func } });
      var wrappedFunc = instance.exports['f'];
      return wrappedFunc;
    }
  
  
  function updateTableMap(offset, count) {
      if (functionsInTableMap) {
        for (var i = offset; i < offset + count; i++) {
          var item = getWasmTableEntry(i);
          // Ignore null values.
          if (item) {
            functionsInTableMap.set(item, i);
          }
        }
      }
    }
  
  var functionsInTableMap = undefined;
  function getFunctionAddress(func) {
      // First, create the map if this is the first use.
      if (!functionsInTableMap) {
        functionsInTableMap = new WeakMap();
        updateTableMap(0, wasmTable.length);
      }
      return functionsInTableMap.get(func) || 0;
    }
  
  
  var freeTableIndexes = [];
  function getEmptyTableSlot() {
      // Reuse a free index if there is one, otherwise grow.
      if (freeTableIndexes.length) {
        return freeTableIndexes.pop();
      }
      // Grow the table
      try {
        wasmTable.grow(1);
      } catch (err) {
        if (!(err instanceof RangeError)) {
          throw err;
        }
        throw 'Unable to grow wasm table. Set ALLOW_TABLE_GROWTH.';
      }
      return wasmTable.length - 1;
    }
  
  
  function setWasmTableEntry(idx, func) {
      wasmTable.set(idx, func);
      // With ABORT_ON_WASM_EXCEPTIONS wasmTable.get is overriden to return wrapped
      // functions so we need to call it here to retrieve the potential wrapper correctly
      // instead of just storing 'func' directly into wasmTableMirror
      wasmTableMirror[idx] = wasmTable.get(idx);
    }
  /** @param {string=} sig */
  function addFunction(func, sig) {
      assert(typeof func != 'undefined');
      // Check if the function is already in the table, to ensure each function
      // gets a unique index.
      var rtn = getFunctionAddress(func);
      if (rtn) {
        return rtn;
      }
  
      // It's not in the table, add it now.
  
      var ret = getEmptyTableSlot();
  
      // Set the new value.
      try {
        // Attempting to call this with JS function will cause of table.set() to fail
        setWasmTableEntry(ret, func);
      } catch (err) {
        if (!(err instanceof TypeError)) {
          throw err;
        }
        assert(typeof sig != 'undefined', 'Missing signature argument to addFunction: ' + func);
        var wrapped = convertJsFunctionToWasm(func, sig);
        setWasmTableEntry(ret, wrapped);
      }
  
      functionsInTableMap.set(func, ret);
  
      return ret;
    }
function checkIncomingModuleAPI() {
  ignoredModuleProp('fetchSettings');
}
var wasmImports = {
  "__assert_fail": ___assert_fail,
  "__cxa_begin_catch": ___cxa_begin_catch,
  "__cxa_end_catch": ___cxa_end_catch,
  "__cxa_find_matching_catch_2": ___cxa_find_matching_catch_2,
  "__cxa_find_matching_catch_3": ___cxa_find_matching_catch_3,
  "__cxa_rethrow": ___cxa_rethrow,
  "__cxa_throw": ___cxa_throw,
  "__resumeException": ___resumeException,
  "abort": _abort,
  "emscripten_memcpy_big": _emscripten_memcpy_big,
  "emscripten_resize_heap": _emscripten_resize_heap,
  "fd_close": _fd_close,
  "fd_seek": _fd_seek,
  "fd_write": _fd_write,
  "invoke_diiidiiiii": invoke_diiidiiiii,
  "invoke_ii": invoke_ii,
  "invoke_iidddiidd": invoke_iidddiidd,
  "invoke_iiddiiddiidid": invoke_iiddiiddiidid,
  "invoke_iiddiidiidiiiii": invoke_iiddiidiidiiiii,
  "invoke_iii": invoke_iii,
  "invoke_iiii": invoke_iiii,
  "invoke_iiiii": invoke_iiiii,
  "invoke_iiiiidididdidddddidddii": invoke_iiiiidididdidddddidddii,
  "invoke_iiiiii": invoke_iiiiii,
  "invoke_v": invoke_v,
  "invoke_vi": invoke_vi,
  "invoke_vididi": invoke_vididi,
  "invoke_vii": invoke_vii,
  "invoke_viii": invoke_viii,
  "invoke_viiii": invoke_viiii,
  "invoke_viiiiddddddddddddii": invoke_viiiiddddddddddddii,
  "invoke_viiiii": invoke_viiiii,
  "invoke_viiiiiiiiiiiii": invoke_viiiiiiiiiiiii
};
var asm = createWasm();
/** @type {function(...*):?} */
var ___wasm_call_ctors = createExportWrapper("__wasm_call_ctors");
/** @type {function(...*):?} */
var getTempRet0 = createExportWrapper("getTempRet0");
/** @type {function(...*):?} */
var _fflush = Module["_fflush"] = createExportWrapper("fflush");
/** @type {function(...*):?} */
var _emscripten_bind_VoidPtr___destroy___0 = Module["_emscripten_bind_VoidPtr___destroy___0"] = createExportWrapper("emscripten_bind_VoidPtr___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_DoublePtr___destroy___0 = Module["_emscripten_bind_DoublePtr___destroy___0"] = createExportWrapper("emscripten_bind_DoublePtr___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_BoolVector_BoolVector_0 = Module["_emscripten_bind_BoolVector_BoolVector_0"] = createExportWrapper("emscripten_bind_BoolVector_BoolVector_0");
/** @type {function(...*):?} */
var _emscripten_bind_BoolVector_BoolVector_1 = Module["_emscripten_bind_BoolVector_BoolVector_1"] = createExportWrapper("emscripten_bind_BoolVector_BoolVector_1");
/** @type {function(...*):?} */
var _emscripten_bind_BoolVector_resize_1 = Module["_emscripten_bind_BoolVector_resize_1"] = createExportWrapper("emscripten_bind_BoolVector_resize_1");
/** @type {function(...*):?} */
var _emscripten_bind_BoolVector_get_1 = Module["_emscripten_bind_BoolVector_get_1"] = createExportWrapper("emscripten_bind_BoolVector_get_1");
/** @type {function(...*):?} */
var _emscripten_bind_BoolVector_set_2 = Module["_emscripten_bind_BoolVector_set_2"] = createExportWrapper("emscripten_bind_BoolVector_set_2");
/** @type {function(...*):?} */
var _emscripten_bind_BoolVector_size_0 = Module["_emscripten_bind_BoolVector_size_0"] = createExportWrapper("emscripten_bind_BoolVector_size_0");
/** @type {function(...*):?} */
var _emscripten_bind_BoolVector___destroy___0 = Module["_emscripten_bind_BoolVector___destroy___0"] = createExportWrapper("emscripten_bind_BoolVector___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_CharVector_CharVector_0 = Module["_emscripten_bind_CharVector_CharVector_0"] = createExportWrapper("emscripten_bind_CharVector_CharVector_0");
/** @type {function(...*):?} */
var _emscripten_bind_CharVector_CharVector_1 = Module["_emscripten_bind_CharVector_CharVector_1"] = createExportWrapper("emscripten_bind_CharVector_CharVector_1");
/** @type {function(...*):?} */
var _emscripten_bind_CharVector_resize_1 = Module["_emscripten_bind_CharVector_resize_1"] = createExportWrapper("emscripten_bind_CharVector_resize_1");
/** @type {function(...*):?} */
var _emscripten_bind_CharVector_get_1 = Module["_emscripten_bind_CharVector_get_1"] = createExportWrapper("emscripten_bind_CharVector_get_1");
/** @type {function(...*):?} */
var _emscripten_bind_CharVector_set_2 = Module["_emscripten_bind_CharVector_set_2"] = createExportWrapper("emscripten_bind_CharVector_set_2");
/** @type {function(...*):?} */
var _emscripten_bind_CharVector_size_0 = Module["_emscripten_bind_CharVector_size_0"] = createExportWrapper("emscripten_bind_CharVector_size_0");
/** @type {function(...*):?} */
var _emscripten_bind_CharVector___destroy___0 = Module["_emscripten_bind_CharVector___destroy___0"] = createExportWrapper("emscripten_bind_CharVector___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_IntVector_IntVector_0 = Module["_emscripten_bind_IntVector_IntVector_0"] = createExportWrapper("emscripten_bind_IntVector_IntVector_0");
/** @type {function(...*):?} */
var _emscripten_bind_IntVector_IntVector_1 = Module["_emscripten_bind_IntVector_IntVector_1"] = createExportWrapper("emscripten_bind_IntVector_IntVector_1");
/** @type {function(...*):?} */
var _emscripten_bind_IntVector_resize_1 = Module["_emscripten_bind_IntVector_resize_1"] = createExportWrapper("emscripten_bind_IntVector_resize_1");
/** @type {function(...*):?} */
var _emscripten_bind_IntVector_get_1 = Module["_emscripten_bind_IntVector_get_1"] = createExportWrapper("emscripten_bind_IntVector_get_1");
/** @type {function(...*):?} */
var _emscripten_bind_IntVector_set_2 = Module["_emscripten_bind_IntVector_set_2"] = createExportWrapper("emscripten_bind_IntVector_set_2");
/** @type {function(...*):?} */
var _emscripten_bind_IntVector_size_0 = Module["_emscripten_bind_IntVector_size_0"] = createExportWrapper("emscripten_bind_IntVector_size_0");
/** @type {function(...*):?} */
var _emscripten_bind_IntVector___destroy___0 = Module["_emscripten_bind_IntVector___destroy___0"] = createExportWrapper("emscripten_bind_IntVector___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_DoubleVector_DoubleVector_0 = Module["_emscripten_bind_DoubleVector_DoubleVector_0"] = createExportWrapper("emscripten_bind_DoubleVector_DoubleVector_0");
/** @type {function(...*):?} */
var _emscripten_bind_DoubleVector_DoubleVector_1 = Module["_emscripten_bind_DoubleVector_DoubleVector_1"] = createExportWrapper("emscripten_bind_DoubleVector_DoubleVector_1");
/** @type {function(...*):?} */
var _emscripten_bind_DoubleVector_resize_1 = Module["_emscripten_bind_DoubleVector_resize_1"] = createExportWrapper("emscripten_bind_DoubleVector_resize_1");
/** @type {function(...*):?} */
var _emscripten_bind_DoubleVector_get_1 = Module["_emscripten_bind_DoubleVector_get_1"] = createExportWrapper("emscripten_bind_DoubleVector_get_1");
/** @type {function(...*):?} */
var _emscripten_bind_DoubleVector_set_2 = Module["_emscripten_bind_DoubleVector_set_2"] = createExportWrapper("emscripten_bind_DoubleVector_set_2");
/** @type {function(...*):?} */
var _emscripten_bind_DoubleVector_size_0 = Module["_emscripten_bind_DoubleVector_size_0"] = createExportWrapper("emscripten_bind_DoubleVector_size_0");
/** @type {function(...*):?} */
var _emscripten_bind_DoubleVector___destroy___0 = Module["_emscripten_bind_DoubleVector___destroy___0"] = createExportWrapper("emscripten_bind_DoubleVector___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_0 = Module["_emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_0"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_0");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_1 = Module["_emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_1"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_1");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecordVector_resize_1 = Module["_emscripten_bind_SpeciesMasterTableRecordVector_resize_1"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecordVector_resize_1");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecordVector_get_1 = Module["_emscripten_bind_SpeciesMasterTableRecordVector_get_1"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecordVector_get_1");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecordVector_set_2 = Module["_emscripten_bind_SpeciesMasterTableRecordVector_set_2"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecordVector_set_2");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecordVector_size_0 = Module["_emscripten_bind_SpeciesMasterTableRecordVector_size_0"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecordVector_size_0");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecordVector___destroy___0 = Module["_emscripten_bind_SpeciesMasterTableRecordVector___destroy___0"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecordVector___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_FireSize_0 = Module["_emscripten_bind_FireSize_FireSize_0"] = createExportWrapper("emscripten_bind_FireSize_FireSize_0");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_calculateFireBasicDimensions_4 = Module["_emscripten_bind_FireSize_calculateFireBasicDimensions_4"] = createExportWrapper("emscripten_bind_FireSize_calculateFireBasicDimensions_4");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getFireLengthToWidthRatio_0 = Module["_emscripten_bind_FireSize_getFireLengthToWidthRatio_0"] = createExportWrapper("emscripten_bind_FireSize_getFireLengthToWidthRatio_0");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getEccentricity_0 = Module["_emscripten_bind_FireSize_getEccentricity_0"] = createExportWrapper("emscripten_bind_FireSize_getEccentricity_0");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getBackingSpreadRate_1 = Module["_emscripten_bind_FireSize_getBackingSpreadRate_1"] = createExportWrapper("emscripten_bind_FireSize_getBackingSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getEllipticalA_3 = Module["_emscripten_bind_FireSize_getEllipticalA_3"] = createExportWrapper("emscripten_bind_FireSize_getEllipticalA_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getEllipticalB_3 = Module["_emscripten_bind_FireSize_getEllipticalB_3"] = createExportWrapper("emscripten_bind_FireSize_getEllipticalB_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getEllipticalC_3 = Module["_emscripten_bind_FireSize_getEllipticalC_3"] = createExportWrapper("emscripten_bind_FireSize_getEllipticalC_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getFireLength_3 = Module["_emscripten_bind_FireSize_getFireLength_3"] = createExportWrapper("emscripten_bind_FireSize_getFireLength_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getMaxFireWidth_3 = Module["_emscripten_bind_FireSize_getMaxFireWidth_3"] = createExportWrapper("emscripten_bind_FireSize_getMaxFireWidth_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getFirePerimeter_3 = Module["_emscripten_bind_FireSize_getFirePerimeter_3"] = createExportWrapper("emscripten_bind_FireSize_getFirePerimeter_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getFireArea_3 = Module["_emscripten_bind_FireSize_getFireArea_3"] = createExportWrapper("emscripten_bind_FireSize_getFireArea_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize___destroy___0 = Module["_emscripten_bind_FireSize___destroy___0"] = createExportWrapper("emscripten_bind_FireSize___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_SIGContainResource_0 = Module["_emscripten_bind_SIGContainResource_SIGContainResource_0"] = createExportWrapper("emscripten_bind_SIGContainResource_SIGContainResource_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_SIGContainResource_7 = Module["_emscripten_bind_SIGContainResource_SIGContainResource_7"] = createExportWrapper("emscripten_bind_SIGContainResource_SIGContainResource_7");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_print_2 = Module["_emscripten_bind_SIGContainResource_print_2"] = createExportWrapper("emscripten_bind_SIGContainResource_print_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_arrival_0 = Module["_emscripten_bind_SIGContainResource_arrival_0"] = createExportWrapper("emscripten_bind_SIGContainResource_arrival_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_hourCost_0 = Module["_emscripten_bind_SIGContainResource_hourCost_0"] = createExportWrapper("emscripten_bind_SIGContainResource_hourCost_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_duration_0 = Module["_emscripten_bind_SIGContainResource_duration_0"] = createExportWrapper("emscripten_bind_SIGContainResource_duration_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_production_0 = Module["_emscripten_bind_SIGContainResource_production_0"] = createExportWrapper("emscripten_bind_SIGContainResource_production_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_baseCost_0 = Module["_emscripten_bind_SIGContainResource_baseCost_0"] = createExportWrapper("emscripten_bind_SIGContainResource_baseCost_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_description_0 = Module["_emscripten_bind_SIGContainResource_description_0"] = createExportWrapper("emscripten_bind_SIGContainResource_description_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource_flank_0 = Module["_emscripten_bind_SIGContainResource_flank_0"] = createExportWrapper("emscripten_bind_SIGContainResource_flank_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainResource___destroy___0 = Module["_emscripten_bind_SIGContainResource___destroy___0"] = createExportWrapper("emscripten_bind_SIGContainResource___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_SIGContainForce_0 = Module["_emscripten_bind_SIGContainForce_SIGContainForce_0"] = createExportWrapper("emscripten_bind_SIGContainForce_SIGContainForce_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_addResource_1 = Module["_emscripten_bind_SIGContainForce_addResource_1"] = createExportWrapper("emscripten_bind_SIGContainForce_addResource_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_addResource_7 = Module["_emscripten_bind_SIGContainForce_addResource_7"] = createExportWrapper("emscripten_bind_SIGContainForce_addResource_7");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_exhausted_1 = Module["_emscripten_bind_SIGContainForce_exhausted_1"] = createExportWrapper("emscripten_bind_SIGContainForce_exhausted_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_firstArrival_1 = Module["_emscripten_bind_SIGContainForce_firstArrival_1"] = createExportWrapper("emscripten_bind_SIGContainForce_firstArrival_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_nextArrival_3 = Module["_emscripten_bind_SIGContainForce_nextArrival_3"] = createExportWrapper("emscripten_bind_SIGContainForce_nextArrival_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_productionRate_2 = Module["_emscripten_bind_SIGContainForce_productionRate_2"] = createExportWrapper("emscripten_bind_SIGContainForce_productionRate_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_resources_0 = Module["_emscripten_bind_SIGContainForce_resources_0"] = createExportWrapper("emscripten_bind_SIGContainForce_resources_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_resourceArrival_1 = Module["_emscripten_bind_SIGContainForce_resourceArrival_1"] = createExportWrapper("emscripten_bind_SIGContainForce_resourceArrival_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_resourceBaseCost_1 = Module["_emscripten_bind_SIGContainForce_resourceBaseCost_1"] = createExportWrapper("emscripten_bind_SIGContainForce_resourceBaseCost_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_resourceCost_2 = Module["_emscripten_bind_SIGContainForce_resourceCost_2"] = createExportWrapper("emscripten_bind_SIGContainForce_resourceCost_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_resourceDescription_1 = Module["_emscripten_bind_SIGContainForce_resourceDescription_1"] = createExportWrapper("emscripten_bind_SIGContainForce_resourceDescription_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_resourceDuration_1 = Module["_emscripten_bind_SIGContainForce_resourceDuration_1"] = createExportWrapper("emscripten_bind_SIGContainForce_resourceDuration_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_resourceFlank_1 = Module["_emscripten_bind_SIGContainForce_resourceFlank_1"] = createExportWrapper("emscripten_bind_SIGContainForce_resourceFlank_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_resourceHourCost_1 = Module["_emscripten_bind_SIGContainForce_resourceHourCost_1"] = createExportWrapper("emscripten_bind_SIGContainForce_resourceHourCost_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce_resourceProduction_1 = Module["_emscripten_bind_SIGContainForce_resourceProduction_1"] = createExportWrapper("emscripten_bind_SIGContainForce_resourceProduction_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForce___destroy___0 = Module["_emscripten_bind_SIGContainForce___destroy___0"] = createExportWrapper("emscripten_bind_SIGContainForce___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForceAdapter_SIGContainForceAdapter_0 = Module["_emscripten_bind_SIGContainForceAdapter_SIGContainForceAdapter_0"] = createExportWrapper("emscripten_bind_SIGContainForceAdapter_SIGContainForceAdapter_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForceAdapter_addResource_7 = Module["_emscripten_bind_SIGContainForceAdapter_addResource_7"] = createExportWrapper("emscripten_bind_SIGContainForceAdapter_addResource_7");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForceAdapter_firstArrival_1 = Module["_emscripten_bind_SIGContainForceAdapter_firstArrival_1"] = createExportWrapper("emscripten_bind_SIGContainForceAdapter_firstArrival_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForceAdapter_removeResourceAt_1 = Module["_emscripten_bind_SIGContainForceAdapter_removeResourceAt_1"] = createExportWrapper("emscripten_bind_SIGContainForceAdapter_removeResourceAt_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForceAdapter_removeResourceWithThisDesc_1 = Module["_emscripten_bind_SIGContainForceAdapter_removeResourceWithThisDesc_1"] = createExportWrapper("emscripten_bind_SIGContainForceAdapter_removeResourceWithThisDesc_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForceAdapter_removeAllResourcesWithThisDesc_1 = Module["_emscripten_bind_SIGContainForceAdapter_removeAllResourcesWithThisDesc_1"] = createExportWrapper("emscripten_bind_SIGContainForceAdapter_removeAllResourcesWithThisDesc_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainForceAdapter___destroy___0 = Module["_emscripten_bind_SIGContainForceAdapter___destroy___0"] = createExportWrapper("emscripten_bind_SIGContainForceAdapter___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_SIGContainSim_13 = Module["_emscripten_bind_SIGContainSim_SIGContainSim_13"] = createExportWrapper("emscripten_bind_SIGContainSim_SIGContainSim_13");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_attackDistance_0 = Module["_emscripten_bind_SIGContainSim_attackDistance_0"] = createExportWrapper("emscripten_bind_SIGContainSim_attackDistance_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_attackPointX_0 = Module["_emscripten_bind_SIGContainSim_attackPointX_0"] = createExportWrapper("emscripten_bind_SIGContainSim_attackPointX_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_attackPointY_0 = Module["_emscripten_bind_SIGContainSim_attackPointY_0"] = createExportWrapper("emscripten_bind_SIGContainSim_attackPointY_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_attackTime_0 = Module["_emscripten_bind_SIGContainSim_attackTime_0"] = createExportWrapper("emscripten_bind_SIGContainSim_attackTime_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_distanceStep_0 = Module["_emscripten_bind_SIGContainSim_distanceStep_0"] = createExportWrapper("emscripten_bind_SIGContainSim_distanceStep_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireBackAtAttack_0 = Module["_emscripten_bind_SIGContainSim_fireBackAtAttack_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireBackAtAttack_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireBackAtReport_0 = Module["_emscripten_bind_SIGContainSim_fireBackAtReport_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireBackAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireHeadAtAttack_0 = Module["_emscripten_bind_SIGContainSim_fireHeadAtAttack_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireHeadAtAttack_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireHeadAtReport_0 = Module["_emscripten_bind_SIGContainSim_fireHeadAtReport_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireHeadAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireLwRatioAtReport_0 = Module["_emscripten_bind_SIGContainSim_fireLwRatioAtReport_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireLwRatioAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireReportTime_0 = Module["_emscripten_bind_SIGContainSim_fireReportTime_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireReportTime_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireSizeAtReport_0 = Module["_emscripten_bind_SIGContainSim_fireSizeAtReport_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireSizeAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireSpreadRateAtBack_0 = Module["_emscripten_bind_SIGContainSim_fireSpreadRateAtBack_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireSpreadRateAtBack_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireSpreadRateAtReport_0 = Module["_emscripten_bind_SIGContainSim_fireSpreadRateAtReport_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireSpreadRateAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_force_0 = Module["_emscripten_bind_SIGContainSim_force_0"] = createExportWrapper("emscripten_bind_SIGContainSim_force_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_maximumSimulationSteps_0 = Module["_emscripten_bind_SIGContainSim_maximumSimulationSteps_0"] = createExportWrapper("emscripten_bind_SIGContainSim_maximumSimulationSteps_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_minimumSimulationSteps_0 = Module["_emscripten_bind_SIGContainSim_minimumSimulationSteps_0"] = createExportWrapper("emscripten_bind_SIGContainSim_minimumSimulationSteps_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_status_0 = Module["_emscripten_bind_SIGContainSim_status_0"] = createExportWrapper("emscripten_bind_SIGContainSim_status_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_tactic_0 = Module["_emscripten_bind_SIGContainSim_tactic_0"] = createExportWrapper("emscripten_bind_SIGContainSim_tactic_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_finalFireCost_0 = Module["_emscripten_bind_SIGContainSim_finalFireCost_0"] = createExportWrapper("emscripten_bind_SIGContainSim_finalFireCost_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_finalFireLine_0 = Module["_emscripten_bind_SIGContainSim_finalFireLine_0"] = createExportWrapper("emscripten_bind_SIGContainSim_finalFireLine_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_finalFirePerimeter_0 = Module["_emscripten_bind_SIGContainSim_finalFirePerimeter_0"] = createExportWrapper("emscripten_bind_SIGContainSim_finalFirePerimeter_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_finalFireSize_0 = Module["_emscripten_bind_SIGContainSim_finalFireSize_0"] = createExportWrapper("emscripten_bind_SIGContainSim_finalFireSize_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_finalFireSweep_0 = Module["_emscripten_bind_SIGContainSim_finalFireSweep_0"] = createExportWrapper("emscripten_bind_SIGContainSim_finalFireSweep_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_finalFireTime_0 = Module["_emscripten_bind_SIGContainSim_finalFireTime_0"] = createExportWrapper("emscripten_bind_SIGContainSim_finalFireTime_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_finalResourcesUsed_0 = Module["_emscripten_bind_SIGContainSim_finalResourcesUsed_0"] = createExportWrapper("emscripten_bind_SIGContainSim_finalResourcesUsed_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_fireHeadX_0 = Module["_emscripten_bind_SIGContainSim_fireHeadX_0"] = createExportWrapper("emscripten_bind_SIGContainSim_fireHeadX_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_firePerimeterY_0 = Module["_emscripten_bind_SIGContainSim_firePerimeterY_0"] = createExportWrapper("emscripten_bind_SIGContainSim_firePerimeterY_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_firePerimeterX_0 = Module["_emscripten_bind_SIGContainSim_firePerimeterX_0"] = createExportWrapper("emscripten_bind_SIGContainSim_firePerimeterX_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_firePoints_0 = Module["_emscripten_bind_SIGContainSim_firePoints_0"] = createExportWrapper("emscripten_bind_SIGContainSim_firePoints_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_run_0 = Module["_emscripten_bind_SIGContainSim_run_0"] = createExportWrapper("emscripten_bind_SIGContainSim_run_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim_UncontainedArea_5 = Module["_emscripten_bind_SIGContainSim_UncontainedArea_5"] = createExportWrapper("emscripten_bind_SIGContainSim_UncontainedArea_5");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainSim___destroy___0 = Module["_emscripten_bind_SIGContainSim___destroy___0"] = createExportWrapper("emscripten_bind_SIGContainSim___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGDiurnalROS_SIGDiurnalROS_0 = Module["_emscripten_bind_SIGDiurnalROS_SIGDiurnalROS_0"] = createExportWrapper("emscripten_bind_SIGDiurnalROS_SIGDiurnalROS_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGDiurnalROS_push_1 = Module["_emscripten_bind_SIGDiurnalROS_push_1"] = createExportWrapper("emscripten_bind_SIGDiurnalROS_push_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGDiurnalROS_at_1 = Module["_emscripten_bind_SIGDiurnalROS_at_1"] = createExportWrapper("emscripten_bind_SIGDiurnalROS_at_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGDiurnalROS_size_0 = Module["_emscripten_bind_SIGDiurnalROS_size_0"] = createExportWrapper("emscripten_bind_SIGDiurnalROS_size_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGDiurnalROS___destroy___0 = Module["_emscripten_bind_SIGDiurnalROS___destroy___0"] = createExportWrapper("emscripten_bind_SIGDiurnalROS___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_SIGContain_11 = Module["_emscripten_bind_SIGContain_SIGContain_11"] = createExportWrapper("emscripten_bind_SIGContain_SIGContain_11");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_simulationTime_0 = Module["_emscripten_bind_SIGContain_simulationTime_0"] = createExportWrapper("emscripten_bind_SIGContain_simulationTime_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_fireSpreadRateAtBack_0 = Module["_emscripten_bind_SIGContain_fireSpreadRateAtBack_0"] = createExportWrapper("emscripten_bind_SIGContain_fireSpreadRateAtBack_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_fireLwRatioAtReport_0 = Module["_emscripten_bind_SIGContain_fireLwRatioAtReport_0"] = createExportWrapper("emscripten_bind_SIGContain_fireLwRatioAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_force_0 = Module["_emscripten_bind_SIGContain_force_0"] = createExportWrapper("emscripten_bind_SIGContain_force_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_resourceHourCost_1 = Module["_emscripten_bind_SIGContain_resourceHourCost_1"] = createExportWrapper("emscripten_bind_SIGContain_resourceHourCost_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_attackDistance_0 = Module["_emscripten_bind_SIGContain_attackDistance_0"] = createExportWrapper("emscripten_bind_SIGContain_attackDistance_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_attackPointX_0 = Module["_emscripten_bind_SIGContain_attackPointX_0"] = createExportWrapper("emscripten_bind_SIGContain_attackPointX_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_fireHeadAtAttack_0 = Module["_emscripten_bind_SIGContain_fireHeadAtAttack_0"] = createExportWrapper("emscripten_bind_SIGContain_fireHeadAtAttack_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_attackPointY_0 = Module["_emscripten_bind_SIGContain_attackPointY_0"] = createExportWrapper("emscripten_bind_SIGContain_attackPointY_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_attackTime_0 = Module["_emscripten_bind_SIGContain_attackTime_0"] = createExportWrapper("emscripten_bind_SIGContain_attackTime_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_resourceBaseCost_1 = Module["_emscripten_bind_SIGContain_resourceBaseCost_1"] = createExportWrapper("emscripten_bind_SIGContain_resourceBaseCost_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_fireSpreadRateAtReport_0 = Module["_emscripten_bind_SIGContain_fireSpreadRateAtReport_0"] = createExportWrapper("emscripten_bind_SIGContain_fireSpreadRateAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_fireHeadAtReport_0 = Module["_emscripten_bind_SIGContain_fireHeadAtReport_0"] = createExportWrapper("emscripten_bind_SIGContain_fireHeadAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_fireReportTime_0 = Module["_emscripten_bind_SIGContain_fireReportTime_0"] = createExportWrapper("emscripten_bind_SIGContain_fireReportTime_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_resourceProduction_1 = Module["_emscripten_bind_SIGContain_resourceProduction_1"] = createExportWrapper("emscripten_bind_SIGContain_resourceProduction_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_fireBackAtAttack_0 = Module["_emscripten_bind_SIGContain_fireBackAtAttack_0"] = createExportWrapper("emscripten_bind_SIGContain_fireBackAtAttack_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_simulationStep_0 = Module["_emscripten_bind_SIGContain_simulationStep_0"] = createExportWrapper("emscripten_bind_SIGContain_simulationStep_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_tactic_0 = Module["_emscripten_bind_SIGContain_tactic_0"] = createExportWrapper("emscripten_bind_SIGContain_tactic_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_resourceDescription_1 = Module["_emscripten_bind_SIGContain_resourceDescription_1"] = createExportWrapper("emscripten_bind_SIGContain_resourceDescription_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_distanceStep_0 = Module["_emscripten_bind_SIGContain_distanceStep_0"] = createExportWrapper("emscripten_bind_SIGContain_distanceStep_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_status_0 = Module["_emscripten_bind_SIGContain_status_0"] = createExportWrapper("emscripten_bind_SIGContain_status_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_resourceArrival_1 = Module["_emscripten_bind_SIGContain_resourceArrival_1"] = createExportWrapper("emscripten_bind_SIGContain_resourceArrival_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_fireSizeAtReport_0 = Module["_emscripten_bind_SIGContain_fireSizeAtReport_0"] = createExportWrapper("emscripten_bind_SIGContain_fireSizeAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_setFireStartTimeMinutes_1 = Module["_emscripten_bind_SIGContain_setFireStartTimeMinutes_1"] = createExportWrapper("emscripten_bind_SIGContain_setFireStartTimeMinutes_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_fireBackAtReport_0 = Module["_emscripten_bind_SIGContain_fireBackAtReport_0"] = createExportWrapper("emscripten_bind_SIGContain_fireBackAtReport_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_resourceDuration_1 = Module["_emscripten_bind_SIGContain_resourceDuration_1"] = createExportWrapper("emscripten_bind_SIGContain_resourceDuration_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_resources_0 = Module["_emscripten_bind_SIGContain_resources_0"] = createExportWrapper("emscripten_bind_SIGContain_resources_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain_exhaustedTime_0 = Module["_emscripten_bind_SIGContain_exhaustedTime_0"] = createExportWrapper("emscripten_bind_SIGContain_exhaustedTime_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContain___destroy___0 = Module["_emscripten_bind_SIGContain___destroy___0"] = createExportWrapper("emscripten_bind_SIGContain___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_SIGContainAdapter_0 = Module["_emscripten_bind_SIGContainAdapter_SIGContainAdapter_0"] = createExportWrapper("emscripten_bind_SIGContainAdapter_SIGContainAdapter_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_getContainmentStatus_0 = Module["_emscripten_bind_SIGContainAdapter_getContainmentStatus_0"] = createExportWrapper("emscripten_bind_SIGContainAdapter_getContainmentStatus_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_getFinalContainmentArea_1 = Module["_emscripten_bind_SIGContainAdapter_getFinalContainmentArea_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_getFinalContainmentArea_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_getFinalCost_0 = Module["_emscripten_bind_SIGContainAdapter_getFinalCost_0"] = createExportWrapper("emscripten_bind_SIGContainAdapter_getFinalCost_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_getFinalFireLineLength_1 = Module["_emscripten_bind_SIGContainAdapter_getFinalFireLineLength_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_getFinalFireLineLength_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_getFinalFireSize_1 = Module["_emscripten_bind_SIGContainAdapter_getFinalFireSize_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_getFinalFireSize_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_getFinalTimeSinceReport_1 = Module["_emscripten_bind_SIGContainAdapter_getFinalTimeSinceReport_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_getFinalTimeSinceReport_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_getFireSizeAtInitialAttack_1 = Module["_emscripten_bind_SIGContainAdapter_getFireSizeAtInitialAttack_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_getFireSizeAtInitialAttack_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_getPerimeterAtContainment_1 = Module["_emscripten_bind_SIGContainAdapter_getPerimeterAtContainment_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_getPerimeterAtContainment_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_getPerimeterAtInitialAttack_1 = Module["_emscripten_bind_SIGContainAdapter_getPerimeterAtInitialAttack_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_getPerimeterAtInitialAttack_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_removeAllResourcesWithThisDesc_1 = Module["_emscripten_bind_SIGContainAdapter_removeAllResourcesWithThisDesc_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_removeAllResourcesWithThisDesc_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_removeResourceAt_1 = Module["_emscripten_bind_SIGContainAdapter_removeResourceAt_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_removeResourceAt_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_removeResourceWithThisDesc_1 = Module["_emscripten_bind_SIGContainAdapter_removeResourceWithThisDesc_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_removeResourceWithThisDesc_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_addResource_8 = Module["_emscripten_bind_SIGContainAdapter_addResource_8"] = createExportWrapper("emscripten_bind_SIGContainAdapter_addResource_8");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_doContainRun_0 = Module["_emscripten_bind_SIGContainAdapter_doContainRun_0"] = createExportWrapper("emscripten_bind_SIGContainAdapter_doContainRun_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_removeAllResources_0 = Module["_emscripten_bind_SIGContainAdapter_removeAllResources_0"] = createExportWrapper("emscripten_bind_SIGContainAdapter_removeAllResources_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setAttackDistance_2 = Module["_emscripten_bind_SIGContainAdapter_setAttackDistance_2"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setAttackDistance_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setFireStartTime_1 = Module["_emscripten_bind_SIGContainAdapter_setFireStartTime_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setFireStartTime_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setLwRatio_1 = Module["_emscripten_bind_SIGContainAdapter_setLwRatio_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setLwRatio_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setMaxFireSize_1 = Module["_emscripten_bind_SIGContainAdapter_setMaxFireSize_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setMaxFireSize_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setMaxFireTime_1 = Module["_emscripten_bind_SIGContainAdapter_setMaxFireTime_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setMaxFireTime_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setMaxSteps_1 = Module["_emscripten_bind_SIGContainAdapter_setMaxSteps_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setMaxSteps_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setMinSteps_1 = Module["_emscripten_bind_SIGContainAdapter_setMinSteps_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setMinSteps_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setReportRate_2 = Module["_emscripten_bind_SIGContainAdapter_setReportRate_2"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setReportRate_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setReportSize_2 = Module["_emscripten_bind_SIGContainAdapter_setReportSize_2"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setReportSize_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setRetry_1 = Module["_emscripten_bind_SIGContainAdapter_setRetry_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setRetry_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter_setTactic_1 = Module["_emscripten_bind_SIGContainAdapter_setTactic_1"] = createExportWrapper("emscripten_bind_SIGContainAdapter_setTactic_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGContainAdapter___destroy___0 = Module["_emscripten_bind_SIGContainAdapter___destroy___0"] = createExportWrapper("emscripten_bind_SIGContainAdapter___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_SIGIgniteInputs_0 = Module["_emscripten_bind_SIGIgniteInputs_SIGIgniteInputs_0"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_SIGIgniteInputs_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_initializeMembers_0 = Module["_emscripten_bind_SIGIgniteInputs_initializeMembers_0"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_initializeMembers_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_setAirTemperature_2 = Module["_emscripten_bind_SIGIgniteInputs_setAirTemperature_2"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_setAirTemperature_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_setDuffDepth_2 = Module["_emscripten_bind_SIGIgniteInputs_setDuffDepth_2"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_setDuffDepth_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_setIgnitionFuelBedType_1 = Module["_emscripten_bind_SIGIgniteInputs_setIgnitionFuelBedType_1"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_setIgnitionFuelBedType_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_setLightningChargeType_1 = Module["_emscripten_bind_SIGIgniteInputs_setLightningChargeType_1"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_setLightningChargeType_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_setMoistureHundredHour_2 = Module["_emscripten_bind_SIGIgniteInputs_setMoistureHundredHour_2"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_setMoistureHundredHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_setMoistureOneHour_2 = Module["_emscripten_bind_SIGIgniteInputs_setMoistureOneHour_2"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_setMoistureOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_setSunShade_2 = Module["_emscripten_bind_SIGIgniteInputs_setSunShade_2"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_setSunShade_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_updateIgniteInputs_11 = Module["_emscripten_bind_SIGIgniteInputs_updateIgniteInputs_11"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_updateIgniteInputs_11");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_getIgnitionFuelBedType_0 = Module["_emscripten_bind_SIGIgniteInputs_getIgnitionFuelBedType_0"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_getIgnitionFuelBedType_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_getLightningChargeType_0 = Module["_emscripten_bind_SIGIgniteInputs_getLightningChargeType_0"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_getLightningChargeType_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_getAirTemperature_1 = Module["_emscripten_bind_SIGIgniteInputs_getAirTemperature_1"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_getAirTemperature_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_getDuffDepth_1 = Module["_emscripten_bind_SIGIgniteInputs_getDuffDepth_1"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_getDuffDepth_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_getMoistureHundredHour_1 = Module["_emscripten_bind_SIGIgniteInputs_getMoistureHundredHour_1"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_getMoistureHundredHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_getMoistureOneHour_1 = Module["_emscripten_bind_SIGIgniteInputs_getMoistureOneHour_1"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_getMoistureOneHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs_getSunShade_1 = Module["_emscripten_bind_SIGIgniteInputs_getSunShade_1"] = createExportWrapper("emscripten_bind_SIGIgniteInputs_getSunShade_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgniteInputs___destroy___0 = Module["_emscripten_bind_SIGIgniteInputs___destroy___0"] = createExportWrapper("emscripten_bind_SIGIgniteInputs___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_SIGIgnite_0 = Module["_emscripten_bind_SIGIgnite_SIGIgnite_0"] = createExportWrapper("emscripten_bind_SIGIgnite_SIGIgnite_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_initializeMembers_0 = Module["_emscripten_bind_SIGIgnite_initializeMembers_0"] = createExportWrapper("emscripten_bind_SIGIgnite_initializeMembers_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_getFuelBedType_0 = Module["_emscripten_bind_SIGIgnite_getFuelBedType_0"] = createExportWrapper("emscripten_bind_SIGIgnite_getFuelBedType_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_getLightningChargeType_0 = Module["_emscripten_bind_SIGIgnite_getLightningChargeType_0"] = createExportWrapper("emscripten_bind_SIGIgnite_getLightningChargeType_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_calculateFirebrandIgnitionProbability_1 = Module["_emscripten_bind_SIGIgnite_calculateFirebrandIgnitionProbability_1"] = createExportWrapper("emscripten_bind_SIGIgnite_calculateFirebrandIgnitionProbability_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_calculateLightningIgnitionProbability_1 = Module["_emscripten_bind_SIGIgnite_calculateLightningIgnitionProbability_1"] = createExportWrapper("emscripten_bind_SIGIgnite_calculateLightningIgnitionProbability_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_setAirTemperature_2 = Module["_emscripten_bind_SIGIgnite_setAirTemperature_2"] = createExportWrapper("emscripten_bind_SIGIgnite_setAirTemperature_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_setDuffDepth_2 = Module["_emscripten_bind_SIGIgnite_setDuffDepth_2"] = createExportWrapper("emscripten_bind_SIGIgnite_setDuffDepth_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_setIgnitionFuelBedType_1 = Module["_emscripten_bind_SIGIgnite_setIgnitionFuelBedType_1"] = createExportWrapper("emscripten_bind_SIGIgnite_setIgnitionFuelBedType_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_setLightningChargeType_1 = Module["_emscripten_bind_SIGIgnite_setLightningChargeType_1"] = createExportWrapper("emscripten_bind_SIGIgnite_setLightningChargeType_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_setMoistureHundredHour_2 = Module["_emscripten_bind_SIGIgnite_setMoistureHundredHour_2"] = createExportWrapper("emscripten_bind_SIGIgnite_setMoistureHundredHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_setMoistureOneHour_2 = Module["_emscripten_bind_SIGIgnite_setMoistureOneHour_2"] = createExportWrapper("emscripten_bind_SIGIgnite_setMoistureOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_setSunShade_2 = Module["_emscripten_bind_SIGIgnite_setSunShade_2"] = createExportWrapper("emscripten_bind_SIGIgnite_setSunShade_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_updateIgniteInputs_11 = Module["_emscripten_bind_SIGIgnite_updateIgniteInputs_11"] = createExportWrapper("emscripten_bind_SIGIgnite_updateIgniteInputs_11");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_getAirTemperature_1 = Module["_emscripten_bind_SIGIgnite_getAirTemperature_1"] = createExportWrapper("emscripten_bind_SIGIgnite_getAirTemperature_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_getDuffDepth_1 = Module["_emscripten_bind_SIGIgnite_getDuffDepth_1"] = createExportWrapper("emscripten_bind_SIGIgnite_getDuffDepth_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_getFuelTemperature_1 = Module["_emscripten_bind_SIGIgnite_getFuelTemperature_1"] = createExportWrapper("emscripten_bind_SIGIgnite_getFuelTemperature_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_getMoistureHundredHour_1 = Module["_emscripten_bind_SIGIgnite_getMoistureHundredHour_1"] = createExportWrapper("emscripten_bind_SIGIgnite_getMoistureHundredHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_getMoistureOneHour_1 = Module["_emscripten_bind_SIGIgnite_getMoistureOneHour_1"] = createExportWrapper("emscripten_bind_SIGIgnite_getMoistureOneHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_getSunShade_1 = Module["_emscripten_bind_SIGIgnite_getSunShade_1"] = createExportWrapper("emscripten_bind_SIGIgnite_getSunShade_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite_isFuelDepthNeeded_0 = Module["_emscripten_bind_SIGIgnite_isFuelDepthNeeded_0"] = createExportWrapper("emscripten_bind_SIGIgnite_isFuelDepthNeeded_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGIgnite___destroy___0 = Module["_emscripten_bind_SIGIgnite___destroy___0"] = createExportWrapper("emscripten_bind_SIGIgnite___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_SIGSpotInputs_0 = Module["_emscripten_bind_SIGSpotInputs_SIGSpotInputs_0"] = createExportWrapper("emscripten_bind_SIGSpotInputs_SIGSpotInputs_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getLocation_0 = Module["_emscripten_bind_SIGSpotInputs_getLocation_0"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getLocation_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getTreeSpecies_0 = Module["_emscripten_bind_SIGSpotInputs_getTreeSpecies_0"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getTreeSpecies_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setBurningPileFlameHeight_2 = Module["_emscripten_bind_SIGSpotInputs_setBurningPileFlameHeight_2"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setBurningPileFlameHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setDBH_2 = Module["_emscripten_bind_SIGSpotInputs_setDBH_2"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setDBH_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setDownwindCoverHeight_2 = Module["_emscripten_bind_SIGSpotInputs_setDownwindCoverHeight_2"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setDownwindCoverHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setLocation_1 = Module["_emscripten_bind_SIGSpotInputs_setLocation_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setLocation_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setRidgeToValleyDistance_2 = Module["_emscripten_bind_SIGSpotInputs_setRidgeToValleyDistance_2"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setRidgeToValleyDistance_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setRidgeToValleyElevation_2 = Module["_emscripten_bind_SIGSpotInputs_setRidgeToValleyElevation_2"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setRidgeToValleyElevation_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setSurfaceFlameLength_2 = Module["_emscripten_bind_SIGSpotInputs_setSurfaceFlameLength_2"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setSurfaceFlameLength_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setTorchingTrees_1 = Module["_emscripten_bind_SIGSpotInputs_setTorchingTrees_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setTreeHeight_2 = Module["_emscripten_bind_SIGSpotInputs_setTreeHeight_2"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setTreeHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setTreeSpecies_1 = Module["_emscripten_bind_SIGSpotInputs_setTreeSpecies_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setTreeSpecies_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_setWindSpeedAtTwentyFeet_2 = Module["_emscripten_bind_SIGSpotInputs_setWindSpeedAtTwentyFeet_2"] = createExportWrapper("emscripten_bind_SIGSpotInputs_setWindSpeedAtTwentyFeet_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_updateSpotInputsForBurningPile_11 = Module["_emscripten_bind_SIGSpotInputs_updateSpotInputsForBurningPile_11"] = createExportWrapper("emscripten_bind_SIGSpotInputs_updateSpotInputsForBurningPile_11");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_updateSpotInputsForSurfaceFire_11 = Module["_emscripten_bind_SIGSpotInputs_updateSpotInputsForSurfaceFire_11"] = createExportWrapper("emscripten_bind_SIGSpotInputs_updateSpotInputsForSurfaceFire_11");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_updateSpotInputsForTorchingTrees_15 = Module["_emscripten_bind_SIGSpotInputs_updateSpotInputsForTorchingTrees_15"] = createExportWrapper("emscripten_bind_SIGSpotInputs_updateSpotInputsForTorchingTrees_15");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getBurningPileFlameHeight_1 = Module["_emscripten_bind_SIGSpotInputs_getBurningPileFlameHeight_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getBurningPileFlameHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getDBH_1 = Module["_emscripten_bind_SIGSpotInputs_getDBH_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getDBH_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getDownwindCoverHeight_1 = Module["_emscripten_bind_SIGSpotInputs_getDownwindCoverHeight_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getDownwindCoverHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getRidgeToValleyDistance_1 = Module["_emscripten_bind_SIGSpotInputs_getRidgeToValleyDistance_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getRidgeToValleyDistance_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getRidgeToValleyElevation_1 = Module["_emscripten_bind_SIGSpotInputs_getRidgeToValleyElevation_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getRidgeToValleyElevation_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getSurfaceFlameLength_1 = Module["_emscripten_bind_SIGSpotInputs_getSurfaceFlameLength_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getSurfaceFlameLength_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getTreeHeight_1 = Module["_emscripten_bind_SIGSpotInputs_getTreeHeight_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getTreeHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getWindSpeedAtTwentyFeet_1 = Module["_emscripten_bind_SIGSpotInputs_getWindSpeedAtTwentyFeet_1"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getWindSpeedAtTwentyFeet_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs_getTorchingTrees_0 = Module["_emscripten_bind_SIGSpotInputs_getTorchingTrees_0"] = createExportWrapper("emscripten_bind_SIGSpotInputs_getTorchingTrees_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpotInputs___destroy___0 = Module["_emscripten_bind_SIGSpotInputs___destroy___0"] = createExportWrapper("emscripten_bind_SIGSpotInputs___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_SIGSpot_0 = Module["_emscripten_bind_SIGSpot_SIGSpot_0"] = createExportWrapper("emscripten_bind_SIGSpot_SIGSpot_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_initializeMembers_0 = Module["_emscripten_bind_SIGSpot_initializeMembers_0"] = createExportWrapper("emscripten_bind_SIGSpot_initializeMembers_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_calculateSpottingDistanceFromBurningPile_0 = Module["_emscripten_bind_SIGSpot_calculateSpottingDistanceFromBurningPile_0"] = createExportWrapper("emscripten_bind_SIGSpot_calculateSpottingDistanceFromBurningPile_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_calculateSpottingDistanceFromSurfaceFire_0 = Module["_emscripten_bind_SIGSpot_calculateSpottingDistanceFromSurfaceFire_0"] = createExportWrapper("emscripten_bind_SIGSpot_calculateSpottingDistanceFromSurfaceFire_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_calculateSpottingDistanceFromTorchingTrees_0 = Module["_emscripten_bind_SIGSpot_calculateSpottingDistanceFromTorchingTrees_0"] = createExportWrapper("emscripten_bind_SIGSpot_calculateSpottingDistanceFromTorchingTrees_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setBurningPileFlameHeight_2 = Module["_emscripten_bind_SIGSpot_setBurningPileFlameHeight_2"] = createExportWrapper("emscripten_bind_SIGSpot_setBurningPileFlameHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setDBH_2 = Module["_emscripten_bind_SIGSpot_setDBH_2"] = createExportWrapper("emscripten_bind_SIGSpot_setDBH_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setDownwindCoverHeight_2 = Module["_emscripten_bind_SIGSpot_setDownwindCoverHeight_2"] = createExportWrapper("emscripten_bind_SIGSpot_setDownwindCoverHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setFlameLength_2 = Module["_emscripten_bind_SIGSpot_setFlameLength_2"] = createExportWrapper("emscripten_bind_SIGSpot_setFlameLength_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setLocation_1 = Module["_emscripten_bind_SIGSpot_setLocation_1"] = createExportWrapper("emscripten_bind_SIGSpot_setLocation_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setRidgeToValleyDistance_2 = Module["_emscripten_bind_SIGSpot_setRidgeToValleyDistance_2"] = createExportWrapper("emscripten_bind_SIGSpot_setRidgeToValleyDistance_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setRidgeToValleyElevation_2 = Module["_emscripten_bind_SIGSpot_setRidgeToValleyElevation_2"] = createExportWrapper("emscripten_bind_SIGSpot_setRidgeToValleyElevation_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setTorchingTrees_1 = Module["_emscripten_bind_SIGSpot_setTorchingTrees_1"] = createExportWrapper("emscripten_bind_SIGSpot_setTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setTreeHeight_2 = Module["_emscripten_bind_SIGSpot_setTreeHeight_2"] = createExportWrapper("emscripten_bind_SIGSpot_setTreeHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setTreeSpecies_1 = Module["_emscripten_bind_SIGSpot_setTreeSpecies_1"] = createExportWrapper("emscripten_bind_SIGSpot_setTreeSpecies_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_setWindSpeedAtTwentyFeet_2 = Module["_emscripten_bind_SIGSpot_setWindSpeedAtTwentyFeet_2"] = createExportWrapper("emscripten_bind_SIGSpot_setWindSpeedAtTwentyFeet_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_updateSpotInputsForBurningPile_11 = Module["_emscripten_bind_SIGSpot_updateSpotInputsForBurningPile_11"] = createExportWrapper("emscripten_bind_SIGSpot_updateSpotInputsForBurningPile_11");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_updateSpotInputsForSurfaceFire_11 = Module["_emscripten_bind_SIGSpot_updateSpotInputsForSurfaceFire_11"] = createExportWrapper("emscripten_bind_SIGSpot_updateSpotInputsForSurfaceFire_11");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_updateSpotInputsForTorchingTrees_15 = Module["_emscripten_bind_SIGSpot_updateSpotInputsForTorchingTrees_15"] = createExportWrapper("emscripten_bind_SIGSpot_updateSpotInputsForTorchingTrees_15");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getTorchingTrees_0 = Module["_emscripten_bind_SIGSpot_getTorchingTrees_0"] = createExportWrapper("emscripten_bind_SIGSpot_getTorchingTrees_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getLocation_0 = Module["_emscripten_bind_SIGSpot_getLocation_0"] = createExportWrapper("emscripten_bind_SIGSpot_getLocation_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getTreeSpecies_0 = Module["_emscripten_bind_SIGSpot_getTreeSpecies_0"] = createExportWrapper("emscripten_bind_SIGSpot_getTreeSpecies_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getBurningPileFlameHeight_1 = Module["_emscripten_bind_SIGSpot_getBurningPileFlameHeight_1"] = createExportWrapper("emscripten_bind_SIGSpot_getBurningPileFlameHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getCoverHeightUsedForBurningPile_1 = Module["_emscripten_bind_SIGSpot_getCoverHeightUsedForBurningPile_1"] = createExportWrapper("emscripten_bind_SIGSpot_getCoverHeightUsedForBurningPile_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getCoverHeightUsedForSurfaceFire_1 = Module["_emscripten_bind_SIGSpot_getCoverHeightUsedForSurfaceFire_1"] = createExportWrapper("emscripten_bind_SIGSpot_getCoverHeightUsedForSurfaceFire_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getCoverHeightUsedForTorchingTrees_1 = Module["_emscripten_bind_SIGSpot_getCoverHeightUsedForTorchingTrees_1"] = createExportWrapper("emscripten_bind_SIGSpot_getCoverHeightUsedForTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getDBH_1 = Module["_emscripten_bind_SIGSpot_getDBH_1"] = createExportWrapper("emscripten_bind_SIGSpot_getDBH_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getDownwindCoverHeight_1 = Module["_emscripten_bind_SIGSpot_getDownwindCoverHeight_1"] = createExportWrapper("emscripten_bind_SIGSpot_getDownwindCoverHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getFlameDurationForTorchingTrees_1 = Module["_emscripten_bind_SIGSpot_getFlameDurationForTorchingTrees_1"] = createExportWrapper("emscripten_bind_SIGSpot_getFlameDurationForTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getFlameHeightForTorchingTrees_1 = Module["_emscripten_bind_SIGSpot_getFlameHeightForTorchingTrees_1"] = createExportWrapper("emscripten_bind_SIGSpot_getFlameHeightForTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getFlameRatioForTorchingTrees_0 = Module["_emscripten_bind_SIGSpot_getFlameRatioForTorchingTrees_0"] = createExportWrapper("emscripten_bind_SIGSpot_getFlameRatioForTorchingTrees_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getMaxFirebrandHeightFromBurningPile_1 = Module["_emscripten_bind_SIGSpot_getMaxFirebrandHeightFromBurningPile_1"] = createExportWrapper("emscripten_bind_SIGSpot_getMaxFirebrandHeightFromBurningPile_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getMaxFirebrandHeightFromSurfaceFire_1 = Module["_emscripten_bind_SIGSpot_getMaxFirebrandHeightFromSurfaceFire_1"] = createExportWrapper("emscripten_bind_SIGSpot_getMaxFirebrandHeightFromSurfaceFire_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getMaxFirebrandHeightFromTorchingTrees_1 = Module["_emscripten_bind_SIGSpot_getMaxFirebrandHeightFromTorchingTrees_1"] = createExportWrapper("emscripten_bind_SIGSpot_getMaxFirebrandHeightFromTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1 = Module["_emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1"] = createExportWrapper("emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1 = Module["_emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1"] = createExportWrapper("emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1 = Module["_emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1"] = createExportWrapper("emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1 = Module["_emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1"] = createExportWrapper("emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1 = Module["_emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1"] = createExportWrapper("emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1 = Module["_emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1"] = createExportWrapper("emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getRidgeToValleyDistance_1 = Module["_emscripten_bind_SIGSpot_getRidgeToValleyDistance_1"] = createExportWrapper("emscripten_bind_SIGSpot_getRidgeToValleyDistance_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getRidgeToValleyElevation_1 = Module["_emscripten_bind_SIGSpot_getRidgeToValleyElevation_1"] = createExportWrapper("emscripten_bind_SIGSpot_getRidgeToValleyElevation_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getSurfaceFlameLength_1 = Module["_emscripten_bind_SIGSpot_getSurfaceFlameLength_1"] = createExportWrapper("emscripten_bind_SIGSpot_getSurfaceFlameLength_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getTreeHeight_1 = Module["_emscripten_bind_SIGSpot_getTreeHeight_1"] = createExportWrapper("emscripten_bind_SIGSpot_getTreeHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot_getWindSpeedAtTwentyFeet_1 = Module["_emscripten_bind_SIGSpot_getWindSpeedAtTwentyFeet_1"] = createExportWrapper("emscripten_bind_SIGSpot_getWindSpeedAtTwentyFeet_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSpot___destroy___0 = Module["_emscripten_bind_SIGSpot___destroy___0"] = createExportWrapper("emscripten_bind_SIGSpot___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_SIGFuelModels_0 = Module["_emscripten_bind_SIGFuelModels_SIGFuelModels_0"] = createExportWrapper("emscripten_bind_SIGFuelModels_SIGFuelModels_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_SIGFuelModels_1 = Module["_emscripten_bind_SIGFuelModels_SIGFuelModels_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_SIGFuelModels_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_equal_1 = Module["_emscripten_bind_SIGFuelModels_equal_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_equal_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_clearCustomFuelModel_1 = Module["_emscripten_bind_SIGFuelModels_clearCustomFuelModel_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_clearCustomFuelModel_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getIsDynamic_1 = Module["_emscripten_bind_SIGFuelModels_getIsDynamic_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_getIsDynamic_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_isFuelModelDefined_1 = Module["_emscripten_bind_SIGFuelModels_isFuelModelDefined_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_isFuelModelDefined_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_setCustomFuelModel_21 = Module["_emscripten_bind_SIGFuelModels_setCustomFuelModel_21"] = createExportWrapper("emscripten_bind_SIGFuelModels_setCustomFuelModel_21");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getFuelCode_1 = Module["_emscripten_bind_SIGFuelModels_getFuelCode_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_getFuelCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getFuelName_1 = Module["_emscripten_bind_SIGFuelModels_getFuelName_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_getFuelName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getFuelLoadHundredHour_2 = Module["_emscripten_bind_SIGFuelModels_getFuelLoadHundredHour_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getFuelLoadHundredHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getFuelLoadLiveHerbaceous_2 = Module["_emscripten_bind_SIGFuelModels_getFuelLoadLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getFuelLoadLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getFuelLoadLiveWoody_2 = Module["_emscripten_bind_SIGFuelModels_getFuelLoadLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getFuelLoadLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getFuelLoadOneHour_2 = Module["_emscripten_bind_SIGFuelModels_getFuelLoadOneHour_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getFuelLoadOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getFuelLoadTenHour_2 = Module["_emscripten_bind_SIGFuelModels_getFuelLoadTenHour_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getFuelLoadTenHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getFuelbedDepth_2 = Module["_emscripten_bind_SIGFuelModels_getFuelbedDepth_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getFuelbedDepth_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getHeatOfCombustionDead_2 = Module["_emscripten_bind_SIGFuelModels_getHeatOfCombustionDead_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getHeatOfCombustionDead_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2 = Module["_emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getMoistureOfExtinctionDead_2 = Module["_emscripten_bind_SIGFuelModels_getMoistureOfExtinctionDead_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getMoistureOfExtinctionDead_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getSavrLiveHerbaceous_2 = Module["_emscripten_bind_SIGFuelModels_getSavrLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getSavrLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getSavrLiveWoody_2 = Module["_emscripten_bind_SIGFuelModels_getSavrLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getSavrLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getSavrOneHour_2 = Module["_emscripten_bind_SIGFuelModels_getSavrOneHour_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getSavrOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels___destroy___0 = Module["_emscripten_bind_SIGFuelModels___destroy___0"] = createExportWrapper("emscripten_bind_SIGFuelModels___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_SIGSurface_1 = Module["_emscripten_bind_SIGSurface_SIGSurface_1"] = createExportWrapper("emscripten_bind_SIGSurface_SIGSurface_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_initializeMembers_0 = Module["_emscripten_bind_SIGSurface_initializeMembers_0"] = createExportWrapper("emscripten_bind_SIGSurface_initializeMembers_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_1 = Module["_emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_1"] = createExportWrapper("emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0 = Module["_emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0"] = createExportWrapper("emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindAdjustmentFactorCalculationMethod_0 = Module["_emscripten_bind_SIGSurface_getWindAdjustmentFactorCalculationMethod_0"] = createExportWrapper("emscripten_bind_SIGSurface_getWindAdjustmentFactorCalculationMethod_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindAndSpreadOrientationMode_0 = Module["_emscripten_bind_SIGSurface_getWindAndSpreadOrientationMode_0"] = createExportWrapper("emscripten_bind_SIGSurface_getWindAndSpreadOrientationMode_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindHeightInputMode_0 = Module["_emscripten_bind_SIGSurface_getWindHeightInputMode_0"] = createExportWrapper("emscripten_bind_SIGSurface_getWindHeightInputMode_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_isAllFuelLoadZero_1 = Module["_emscripten_bind_SIGSurface_isAllFuelLoadZero_1"] = createExportWrapper("emscripten_bind_SIGSurface_isAllFuelLoadZero_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_isUsingTwoFuelModels_0 = Module["_emscripten_bind_SIGSurface_isUsingTwoFuelModels_0"] = createExportWrapper("emscripten_bind_SIGSurface_isUsingTwoFuelModels_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_calculateFlameLength_1 = Module["_emscripten_bind_SIGSurface_calculateFlameLength_1"] = createExportWrapper("emscripten_bind_SIGSurface_calculateFlameLength_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspect_0 = Module["_emscripten_bind_SIGSurface_getAspect_0"] = createExportWrapper("emscripten_bind_SIGSurface_getAspect_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getBulkDensity_1 = Module["_emscripten_bind_SIGSurface_getBulkDensity_1"] = createExportWrapper("emscripten_bind_SIGSurface_getBulkDensity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getCanopyCover_1 = Module["_emscripten_bind_SIGSurface_getCanopyCover_1"] = createExportWrapper("emscripten_bind_SIGSurface_getCanopyCover_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getCanopyHeight_1 = Module["_emscripten_bind_SIGSurface_getCanopyHeight_1"] = createExportWrapper("emscripten_bind_SIGSurface_getCanopyHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getCrownRatio_0 = Module["_emscripten_bind_SIGSurface_getCrownRatio_0"] = createExportWrapper("emscripten_bind_SIGSurface_getCrownRatio_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getDirectionOfMaxSpread_0 = Module["_emscripten_bind_SIGSurface_getDirectionOfMaxSpread_0"] = createExportWrapper("emscripten_bind_SIGSurface_getDirectionOfMaxSpread_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getEllipticalA_3 = Module["_emscripten_bind_SIGSurface_getEllipticalA_3"] = createExportWrapper("emscripten_bind_SIGSurface_getEllipticalA_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getEllipticalB_3 = Module["_emscripten_bind_SIGSurface_getEllipticalB_3"] = createExportWrapper("emscripten_bind_SIGSurface_getEllipticalB_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getEllipticalC_3 = Module["_emscripten_bind_SIGSurface_getEllipticalC_3"] = createExportWrapper("emscripten_bind_SIGSurface_getEllipticalC_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFireArea_3 = Module["_emscripten_bind_SIGSurface_getFireArea_3"] = createExportWrapper("emscripten_bind_SIGSurface_getFireArea_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFireEccentricity_0 = Module["_emscripten_bind_SIGSurface_getFireEccentricity_0"] = createExportWrapper("emscripten_bind_SIGSurface_getFireEccentricity_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFireLengthToWidthRatio_0 = Module["_emscripten_bind_SIGSurface_getFireLengthToWidthRatio_0"] = createExportWrapper("emscripten_bind_SIGSurface_getFireLengthToWidthRatio_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFirePerimeter_3 = Module["_emscripten_bind_SIGSurface_getFirePerimeter_3"] = createExportWrapper("emscripten_bind_SIGSurface_getFirePerimeter_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFirelineIntensity_1 = Module["_emscripten_bind_SIGSurface_getFirelineIntensity_1"] = createExportWrapper("emscripten_bind_SIGSurface_getFirelineIntensity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFlameLength_1 = Module["_emscripten_bind_SIGSurface_getFlameLength_1"] = createExportWrapper("emscripten_bind_SIGSurface_getFlameLength_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getHeatPerUnitArea_1 = Module["_emscripten_bind_SIGSurface_getHeatPerUnitArea_1"] = createExportWrapper("emscripten_bind_SIGSurface_getHeatPerUnitArea_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getHeatSink_1 = Module["_emscripten_bind_SIGSurface_getHeatSink_1"] = createExportWrapper("emscripten_bind_SIGSurface_getHeatSink_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMidflameWindspeed_1 = Module["_emscripten_bind_SIGSurface_getMidflameWindspeed_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMidflameWindspeed_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureHundredHour_1 = Module["_emscripten_bind_SIGSurface_getMoistureHundredHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureHundredHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1 = Module["_emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureLiveWoody_1 = Module["_emscripten_bind_SIGSurface_getMoistureLiveWoody_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureLiveWoody_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureOneHour_1 = Module["_emscripten_bind_SIGSurface_getMoistureOneHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureOneHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureTenHour_1 = Module["_emscripten_bind_SIGSurface_getMoistureTenHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureTenHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getReactionIntensity_1 = Module["_emscripten_bind_SIGSurface_getReactionIntensity_1"] = createExportWrapper("emscripten_bind_SIGSurface_getReactionIntensity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getResidenceTime_1 = Module["_emscripten_bind_SIGSurface_getResidenceTime_1"] = createExportWrapper("emscripten_bind_SIGSurface_getResidenceTime_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSlope_1 = Module["_emscripten_bind_SIGSurface_getSlope_1"] = createExportWrapper("emscripten_bind_SIGSurface_getSlope_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSlopeFactor_0 = Module["_emscripten_bind_SIGSurface_getSlopeFactor_0"] = createExportWrapper("emscripten_bind_SIGSurface_getSlopeFactor_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSpreadRate_1 = Module["_emscripten_bind_SIGSurface_getSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGSurface_getSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1 = Module["_emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1"] = createExportWrapper("emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindDirection_0 = Module["_emscripten_bind_SIGSurface_getWindDirection_0"] = createExportWrapper("emscripten_bind_SIGSurface_getWindDirection_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindSpeed_2 = Module["_emscripten_bind_SIGSurface_getWindSpeed_2"] = createExportWrapper("emscripten_bind_SIGSurface_getWindSpeed_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelModelNumber_0 = Module["_emscripten_bind_SIGSurface_getFuelModelNumber_0"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelModelNumber_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setAspect_1 = Module["_emscripten_bind_SIGSurface_setAspect_1"] = createExportWrapper("emscripten_bind_SIGSurface_setAspect_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setCanopyCover_2 = Module["_emscripten_bind_SIGSurface_setCanopyCover_2"] = createExportWrapper("emscripten_bind_SIGSurface_setCanopyCover_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setCanopyHeight_2 = Module["_emscripten_bind_SIGSurface_setCanopyHeight_2"] = createExportWrapper("emscripten_bind_SIGSurface_setCanopyHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setCrownRatio_1 = Module["_emscripten_bind_SIGSurface_setCrownRatio_1"] = createExportWrapper("emscripten_bind_SIGSurface_setCrownRatio_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setFirstFuelModelNumber_1 = Module["_emscripten_bind_SIGSurface_setFirstFuelModelNumber_1"] = createExportWrapper("emscripten_bind_SIGSurface_setFirstFuelModelNumber_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setFuelModelNumber_1 = Module["_emscripten_bind_SIGSurface_setFuelModelNumber_1"] = createExportWrapper("emscripten_bind_SIGSurface_setFuelModelNumber_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setFuelModels_1 = Module["_emscripten_bind_SIGSurface_setFuelModels_1"] = createExportWrapper("emscripten_bind_SIGSurface_setFuelModels_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureHundredHour_2 = Module["_emscripten_bind_SIGSurface_setMoistureHundredHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureHundredHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2 = Module["_emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureLiveWoody_2 = Module["_emscripten_bind_SIGSurface_setMoistureLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureOneHour_2 = Module["_emscripten_bind_SIGSurface_setMoistureOneHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureTenHour_2 = Module["_emscripten_bind_SIGSurface_setMoistureTenHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureTenHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setSecondFuelModelNumber_1 = Module["_emscripten_bind_SIGSurface_setSecondFuelModelNumber_1"] = createExportWrapper("emscripten_bind_SIGSurface_setSecondFuelModelNumber_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setSlope_2 = Module["_emscripten_bind_SIGSurface_setSlope_2"] = createExportWrapper("emscripten_bind_SIGSurface_setSlope_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setTwoFuelModelsFirstFuelModelCoverage_2 = Module["_emscripten_bind_SIGSurface_setTwoFuelModelsFirstFuelModelCoverage_2"] = createExportWrapper("emscripten_bind_SIGSurface_setTwoFuelModelsFirstFuelModelCoverage_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setTwoFuelModelsMethod_1 = Module["_emscripten_bind_SIGSurface_setTwoFuelModelsMethod_1"] = createExportWrapper("emscripten_bind_SIGSurface_setTwoFuelModelsMethod_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setUserProvidedWindAdjustmentFactor_1 = Module["_emscripten_bind_SIGSurface_setUserProvidedWindAdjustmentFactor_1"] = createExportWrapper("emscripten_bind_SIGSurface_setUserProvidedWindAdjustmentFactor_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setWindAdjustmentFactorCalculationMethod_1 = Module["_emscripten_bind_SIGSurface_setWindAdjustmentFactorCalculationMethod_1"] = createExportWrapper("emscripten_bind_SIGSurface_setWindAdjustmentFactorCalculationMethod_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setWindAndSpreadOrientationMode_1 = Module["_emscripten_bind_SIGSurface_setWindAndSpreadOrientationMode_1"] = createExportWrapper("emscripten_bind_SIGSurface_setWindAndSpreadOrientationMode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setWindDirection_1 = Module["_emscripten_bind_SIGSurface_setWindDirection_1"] = createExportWrapper("emscripten_bind_SIGSurface_setWindDirection_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setWindHeightInputMode_1 = Module["_emscripten_bind_SIGSurface_setWindHeightInputMode_1"] = createExportWrapper("emscripten_bind_SIGSurface_setWindHeightInputMode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setWindSpeed_3 = Module["_emscripten_bind_SIGSurface_setWindSpeed_3"] = createExportWrapper("emscripten_bind_SIGSurface_setWindSpeed_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_updateSurfaceInputs_20 = Module["_emscripten_bind_SIGSurface_updateSurfaceInputs_20"] = createExportWrapper("emscripten_bind_SIGSurface_updateSurfaceInputs_20");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_23 = Module["_emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_23"] = createExportWrapper("emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_23");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_24 = Module["_emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_24"] = createExportWrapper("emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_24");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_23 = Module["_emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_23"] = createExportWrapper("emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_23");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface___destroy___0 = Module["_emscripten_bind_SIGSurface___destroy___0"] = createExportWrapper("emscripten_bind_SIGSurface___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_PalmettoGallberry_0 = Module["_emscripten_bind_PalmettoGallberry_PalmettoGallberry_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_PalmettoGallberry_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_initializeMembers_0 = Module["_emscripten_bind_PalmettoGallberry_initializeMembers_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_initializeMembers_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getHeatOfCombustionLive_0 = Module["_emscripten_bind_PalmettoGallberry_getHeatOfCombustionLive_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getHeatOfCombustionLive_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLitterLoad_2 = Module["_emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLitterLoad_2"] = createExportWrapper("emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLitterLoad_2");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveOneHourLoad_0 = Module["_emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveOneHourLoad_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveOneHourLoad_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFoliageLoad_0 = Module["_emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFoliageLoad_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFoliageLoad_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getHeatOfCombustionDead_0 = Module["_emscripten_bind_PalmettoGallberry_getHeatOfCombustionDead_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getHeatOfCombustionDead_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFoliageLoad_3 = Module["_emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFoliageLoad_3"] = createExportWrapper("emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFoliageLoad_3");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveTenHourLoad_2 = Module["_emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveTenHourLoad_2"] = createExportWrapper("emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveTenHourLoad_2");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadTenHourLoad_0 = Module["_emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadTenHourLoad_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadTenHourLoad_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getMoistureOfExtinctionDead_0 = Module["_emscripten_bind_PalmettoGallberry_getMoistureOfExtinctionDead_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getMoistureOfExtinctionDead_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFoliageLoad_0 = Module["_emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFoliageLoad_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFoliageLoad_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLitterLoad_0 = Module["_emscripten_bind_PalmettoGallberry_getPalmettoGallberyLitterLoad_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getPalmettoGallberyLitterLoad_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadTenHourLoad_2 = Module["_emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadTenHourLoad_2"] = createExportWrapper("emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadTenHourLoad_2");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveOneHourLoad_2 = Module["_emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveOneHourLoad_2"] = createExportWrapper("emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveOneHourLoad_2");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getPalmettoGallberyFuelBedDepth_0 = Module["_emscripten_bind_PalmettoGallberry_getPalmettoGallberyFuelBedDepth_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getPalmettoGallberyFuelBedDepth_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFoliageLoad_2 = Module["_emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFoliageLoad_2"] = createExportWrapper("emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFoliageLoad_2");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadOneHourLoad_2 = Module["_emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadOneHourLoad_2"] = createExportWrapper("emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadOneHourLoad_2");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveTenHourLoad_0 = Module["_emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveTenHourLoad_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveTenHourLoad_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadOneHourLoad_0 = Module["_emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadOneHourLoad_0"] = createExportWrapper("emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadOneHourLoad_0");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyFuelBedDepth_1 = Module["_emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyFuelBedDepth_1"] = createExportWrapper("emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyFuelBedDepth_1");
/** @type {function(...*):?} */
var _emscripten_bind_PalmettoGallberry___destroy___0 = Module["_emscripten_bind_PalmettoGallberry___destroy___0"] = createExportWrapper("emscripten_bind_PalmettoGallberry___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_WesternAspen_0 = Module["_emscripten_bind_WesternAspen_WesternAspen_0"] = createExportWrapper("emscripten_bind_WesternAspen_WesternAspen_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_initializeMembers_0 = Module["_emscripten_bind_WesternAspen_initializeMembers_0"] = createExportWrapper("emscripten_bind_WesternAspen_initializeMembers_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_calculateAspenMortality_3 = Module["_emscripten_bind_WesternAspen_calculateAspenMortality_3"] = createExportWrapper("emscripten_bind_WesternAspen_calculateAspenMortality_3");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenDBH_0 = Module["_emscripten_bind_WesternAspen_getAspenDBH_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenDBH_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenFuelBedDepth_1 = Module["_emscripten_bind_WesternAspen_getAspenFuelBedDepth_1"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenFuelBedDepth_1");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenHeatOfCombustionDead_0 = Module["_emscripten_bind_WesternAspen_getAspenHeatOfCombustionDead_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenHeatOfCombustionDead_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenHeatOfCombustionLive_0 = Module["_emscripten_bind_WesternAspen_getAspenHeatOfCombustionLive_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenHeatOfCombustionLive_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_2 = Module["_emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_2"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_1 = Module["_emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_1"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_2 = Module["_emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenLoadLiveWoody_2 = Module["_emscripten_bind_WesternAspen_getAspenLoadLiveWoody_2"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenLoadLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0 = Module["_emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenMortality_0 = Module["_emscripten_bind_WesternAspen_getAspenMortality_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenMortality_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_2 = Module["_emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_2"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0 = Module["_emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0 = Module["_emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenSavrLiveWoody_2 = Module["_emscripten_bind_WesternAspen_getAspenSavrLiveWoody_2"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenSavrLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen___destroy___0 = Module["_emscripten_bind_WesternAspen___destroy___0"] = createExportWrapper("emscripten_bind_WesternAspen___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_initializeMembers_0 = Module["_emscripten_bind_SIGCrown_initializeMembers_0"] = createExportWrapper("emscripten_bind_SIGCrown_initializeMembers_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_doCrownRunRothermel_0 = Module["_emscripten_bind_SIGCrown_doCrownRunRothermel_0"] = createExportWrapper("emscripten_bind_SIGCrown_doCrownRunRothermel_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0 = Module["_emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0"] = createExportWrapper("emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFireType_0 = Module["_emscripten_bind_SIGCrown_getFireType_0"] = createExportWrapper("emscripten_bind_SIGCrown_getFireType_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getAspect_0 = Module["_emscripten_bind_SIGCrown_getAspect_0"] = createExportWrapper("emscripten_bind_SIGCrown_getAspect_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCanopyBaseHeight_1 = Module["_emscripten_bind_SIGCrown_getCanopyBaseHeight_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCanopyBaseHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCanopyBulkDensity_1 = Module["_emscripten_bind_SIGCrown_getCanopyBulkDensity_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCanopyBulkDensity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCanopyCover_1 = Module["_emscripten_bind_SIGCrown_getCanopyCover_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCanopyCover_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCanopyHeight_1 = Module["_emscripten_bind_SIGCrown_getCanopyHeight_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCanopyHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCriticalOpenWindSpeed_1 = Module["_emscripten_bind_SIGCrown_getCriticalOpenWindSpeed_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCriticalOpenWindSpeed_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownFireLengthToWidthRatio_0 = Module["_emscripten_bind_SIGCrown_getCrownFireLengthToWidthRatio_0"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownFireLengthToWidthRatio_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownFireSpreadRate_1 = Module["_emscripten_bind_SIGCrown_getCrownFireSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownFireSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownFirelineIntensity_1 = Module["_emscripten_bind_SIGCrown_getCrownFirelineIntensity_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownFirelineIntensity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownFlameLength_1 = Module["_emscripten_bind_SIGCrown_getCrownFlameLength_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownFlameLength_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownRatio_0 = Module["_emscripten_bind_SIGCrown_getCrownRatio_0"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownRatio_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFinalFirelineIntesity_1 = Module["_emscripten_bind_SIGCrown_getFinalFirelineIntesity_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFinalFirelineIntesity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFinalFlameLength_1 = Module["_emscripten_bind_SIGCrown_getFinalFlameLength_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFinalFlameLength_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFinalHeatPerUnitArea_1 = Module["_emscripten_bind_SIGCrown_getFinalHeatPerUnitArea_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFinalHeatPerUnitArea_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFinalSpreadRate_1 = Module["_emscripten_bind_SIGCrown_getFinalSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFinalSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureFoliar_1 = Module["_emscripten_bind_SIGCrown_getMoistureFoliar_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureFoliar_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureHundredHour_1 = Module["_emscripten_bind_SIGCrown_getMoistureHundredHour_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureHundredHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureLiveHerbaceous_1 = Module["_emscripten_bind_SIGCrown_getMoistureLiveHerbaceous_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureLiveHerbaceous_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureLiveWoody_1 = Module["_emscripten_bind_SIGCrown_getMoistureLiveWoody_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureLiveWoody_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureOneHour_1 = Module["_emscripten_bind_SIGCrown_getMoistureOneHour_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureOneHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureTenHour_1 = Module["_emscripten_bind_SIGCrown_getMoistureTenHour_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureTenHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getSlope_1 = Module["_emscripten_bind_SIGCrown_getSlope_1"] = createExportWrapper("emscripten_bind_SIGCrown_getSlope_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getSurfaceFireSpreadRate_1 = Module["_emscripten_bind_SIGCrown_getSurfaceFireSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGCrown_getSurfaceFireSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getWindDirection_0 = Module["_emscripten_bind_SIGCrown_getWindDirection_0"] = createExportWrapper("emscripten_bind_SIGCrown_getWindDirection_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getWindSpeed_2 = Module["_emscripten_bind_SIGCrown_getWindSpeed_2"] = createExportWrapper("emscripten_bind_SIGCrown_getWindSpeed_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelModelNumber_0 = Module["_emscripten_bind_SIGCrown_getFuelModelNumber_0"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelModelNumber_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setAspect_1 = Module["_emscripten_bind_SIGCrown_setAspect_1"] = createExportWrapper("emscripten_bind_SIGCrown_setAspect_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setCanopyBaseHeight_2 = Module["_emscripten_bind_SIGCrown_setCanopyBaseHeight_2"] = createExportWrapper("emscripten_bind_SIGCrown_setCanopyBaseHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setCanopyBulkDensity_2 = Module["_emscripten_bind_SIGCrown_setCanopyBulkDensity_2"] = createExportWrapper("emscripten_bind_SIGCrown_setCanopyBulkDensity_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setCanopyCover_2 = Module["_emscripten_bind_SIGCrown_setCanopyCover_2"] = createExportWrapper("emscripten_bind_SIGCrown_setCanopyCover_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setCanopyHeight_2 = Module["_emscripten_bind_SIGCrown_setCanopyHeight_2"] = createExportWrapper("emscripten_bind_SIGCrown_setCanopyHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setCrownRatio_1 = Module["_emscripten_bind_SIGCrown_setCrownRatio_1"] = createExportWrapper("emscripten_bind_SIGCrown_setCrownRatio_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setFuelModelNumber_1 = Module["_emscripten_bind_SIGCrown_setFuelModelNumber_1"] = createExportWrapper("emscripten_bind_SIGCrown_setFuelModelNumber_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setFuelModels_1 = Module["_emscripten_bind_SIGCrown_setFuelModels_1"] = createExportWrapper("emscripten_bind_SIGCrown_setFuelModels_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureFoliar_2 = Module["_emscripten_bind_SIGCrown_setMoistureFoliar_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureFoliar_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureHundredHour_2 = Module["_emscripten_bind_SIGCrown_setMoistureHundredHour_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureHundredHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureLiveHerbaceous_2 = Module["_emscripten_bind_SIGCrown_setMoistureLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureLiveWoody_2 = Module["_emscripten_bind_SIGCrown_setMoistureLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureOneHour_2 = Module["_emscripten_bind_SIGCrown_setMoistureOneHour_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureTenHour_2 = Module["_emscripten_bind_SIGCrown_setMoistureTenHour_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureTenHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setSlope_2 = Module["_emscripten_bind_SIGCrown_setSlope_2"] = createExportWrapper("emscripten_bind_SIGCrown_setSlope_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setUserProvidedWindAdjustmentFactor_1 = Module["_emscripten_bind_SIGCrown_setUserProvidedWindAdjustmentFactor_1"] = createExportWrapper("emscripten_bind_SIGCrown_setUserProvidedWindAdjustmentFactor_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setWindAdjustmentFactorCalculationMethod_1 = Module["_emscripten_bind_SIGCrown_setWindAdjustmentFactorCalculationMethod_1"] = createExportWrapper("emscripten_bind_SIGCrown_setWindAdjustmentFactorCalculationMethod_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setWindAndSpreadOrientationMode_1 = Module["_emscripten_bind_SIGCrown_setWindAndSpreadOrientationMode_1"] = createExportWrapper("emscripten_bind_SIGCrown_setWindAndSpreadOrientationMode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setWindDirection_1 = Module["_emscripten_bind_SIGCrown_setWindDirection_1"] = createExportWrapper("emscripten_bind_SIGCrown_setWindDirection_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setWindHeightInputMode_1 = Module["_emscripten_bind_SIGCrown_setWindHeightInputMode_1"] = createExportWrapper("emscripten_bind_SIGCrown_setWindHeightInputMode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setWindSpeed_3 = Module["_emscripten_bind_SIGCrown_setWindSpeed_3"] = createExportWrapper("emscripten_bind_SIGCrown_setWindSpeed_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_updateCrownInputs_24 = Module["_emscripten_bind_SIGCrown_updateCrownInputs_24"] = createExportWrapper("emscripten_bind_SIGCrown_updateCrownInputs_24");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_updateCrownsSurfaceInputs_20 = Module["_emscripten_bind_SIGCrown_updateCrownsSurfaceInputs_20"] = createExportWrapper("emscripten_bind_SIGCrown_updateCrownsSurfaceInputs_20");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown___destroy___0 = Module["_emscripten_bind_SIGCrown___destroy___0"] = createExportWrapper("emscripten_bind_SIGCrown___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_0 = Module["_emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_0"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_0");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_1 = Module["_emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_1"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_1");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTableRecord___destroy___0 = Module["_emscripten_bind_SpeciesMasterTableRecord___destroy___0"] = createExportWrapper("emscripten_bind_SpeciesMasterTableRecord___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTable_SpeciesMasterTable_0 = Module["_emscripten_bind_SpeciesMasterTable_SpeciesMasterTable_0"] = createExportWrapper("emscripten_bind_SpeciesMasterTable_SpeciesMasterTable_0");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTable_initializeMasterTable_0 = Module["_emscripten_bind_SpeciesMasterTable_initializeMasterTable_0"] = createExportWrapper("emscripten_bind_SpeciesMasterTable_initializeMasterTable_0");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCode_1 = Module["_emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCode_1"] = createExportWrapper("emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2 = Module["_emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2"] = createExportWrapper("emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTable_insertRecord_12 = Module["_emscripten_bind_SpeciesMasterTable_insertRecord_12"] = createExportWrapper("emscripten_bind_SpeciesMasterTable_insertRecord_12");
/** @type {function(...*):?} */
var _emscripten_bind_SpeciesMasterTable___destroy___0 = Module["_emscripten_bind_SpeciesMasterTable___destroy___0"] = createExportWrapper("emscripten_bind_SpeciesMasterTable___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_SIGMortality_1 = Module["_emscripten_bind_SIGMortality_SIGMortality_1"] = createExportWrapper("emscripten_bind_SIGMortality_SIGMortality_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getBeetleDamage_0 = Module["_emscripten_bind_SIGMortality_getBeetleDamage_0"] = createExportWrapper("emscripten_bind_SIGMortality_getBeetleDamage_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCrownDamageEquationCode_0 = Module["_emscripten_bind_SIGMortality_getCrownDamageEquationCode_0"] = createExportWrapper("emscripten_bind_SIGMortality_getCrownDamageEquationCode_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCrownDamageEquationCodeAtSpeciesTableIndex_1 = Module["_emscripten_bind_SIGMortality_getCrownDamageEquationCodeAtSpeciesTableIndex_1"] = createExportWrapper("emscripten_bind_SIGMortality_getCrownDamageEquationCodeAtSpeciesTableIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCrownDamageEquationCodeFromSpeciesCode_1 = Module["_emscripten_bind_SIGMortality_getCrownDamageEquationCodeFromSpeciesCode_1"] = createExportWrapper("emscripten_bind_SIGMortality_getCrownDamageEquationCodeFromSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCrownDamageType_0 = Module["_emscripten_bind_SIGMortality_getCrownDamageType_0"] = createExportWrapper("emscripten_bind_SIGMortality_getCrownDamageType_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getEquationType_0 = Module["_emscripten_bind_SIGMortality_getEquationType_0"] = createExportWrapper("emscripten_bind_SIGMortality_getEquationType_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getEquationTypeAtSpeciesTableIndex_1 = Module["_emscripten_bind_SIGMortality_getEquationTypeAtSpeciesTableIndex_1"] = createExportWrapper("emscripten_bind_SIGMortality_getEquationTypeAtSpeciesTableIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getEquationTypeFromSpeciesCode_1 = Module["_emscripten_bind_SIGMortality_getEquationTypeFromSpeciesCode_1"] = createExportWrapper("emscripten_bind_SIGMortality_getEquationTypeFromSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getFireSeverity_0 = Module["_emscripten_bind_SIGMortality_getFireSeverity_0"] = createExportWrapper("emscripten_bind_SIGMortality_getFireSeverity_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightSwitch_0 = Module["_emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightSwitch_0"] = createExportWrapper("emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightSwitch_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getRegion_0 = Module["_emscripten_bind_SIGMortality_getRegion_0"] = createExportWrapper("emscripten_bind_SIGMortality_getRegion_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_checkIsInRegionAtSpeciesTableIndex_2 = Module["_emscripten_bind_SIGMortality_checkIsInRegionAtSpeciesTableIndex_2"] = createExportWrapper("emscripten_bind_SIGMortality_checkIsInRegionAtSpeciesTableIndex_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_checkIsInRegionFromSpeciesCode_2 = Module["_emscripten_bind_SIGMortality_checkIsInRegionFromSpeciesCode_2"] = createExportWrapper("emscripten_bind_SIGMortality_checkIsInRegionFromSpeciesCode_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getBarkThickness_1 = Module["_emscripten_bind_SIGMortality_getBarkThickness_1"] = createExportWrapper("emscripten_bind_SIGMortality_getBarkThickness_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getBasalAreaKillled_0 = Module["_emscripten_bind_SIGMortality_getBasalAreaKillled_0"] = createExportWrapper("emscripten_bind_SIGMortality_getBasalAreaKillled_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getBasalAreaPostfire_0 = Module["_emscripten_bind_SIGMortality_getBasalAreaPostfire_0"] = createExportWrapper("emscripten_bind_SIGMortality_getBasalAreaPostfire_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getBasalAreaPrefire_0 = Module["_emscripten_bind_SIGMortality_getBasalAreaPrefire_0"] = createExportWrapper("emscripten_bind_SIGMortality_getBasalAreaPrefire_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getBoleCharHeight_1 = Module["_emscripten_bind_SIGMortality_getBoleCharHeight_1"] = createExportWrapper("emscripten_bind_SIGMortality_getBoleCharHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCambiumKillRating_0 = Module["_emscripten_bind_SIGMortality_getCambiumKillRating_0"] = createExportWrapper("emscripten_bind_SIGMortality_getCambiumKillRating_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCrownDamage_0 = Module["_emscripten_bind_SIGMortality_getCrownDamage_0"] = createExportWrapper("emscripten_bind_SIGMortality_getCrownDamage_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCrownRatio_0 = Module["_emscripten_bind_SIGMortality_getCrownRatio_0"] = createExportWrapper("emscripten_bind_SIGMortality_getCrownRatio_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getDBH_1 = Module["_emscripten_bind_SIGMortality_getDBH_1"] = createExportWrapper("emscripten_bind_SIGMortality_getDBH_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightValue_1 = Module["_emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightValue_1"] = createExportWrapper("emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightValue_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getKilledTrees_0 = Module["_emscripten_bind_SIGMortality_getKilledTrees_0"] = createExportWrapper("emscripten_bind_SIGMortality_getKilledTrees_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getProbabilityOfMortality_1 = Module["_emscripten_bind_SIGMortality_getProbabilityOfMortality_1"] = createExportWrapper("emscripten_bind_SIGMortality_getProbabilityOfMortality_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getTotalPrefireTrees_0 = Module["_emscripten_bind_SIGMortality_getTotalPrefireTrees_0"] = createExportWrapper("emscripten_bind_SIGMortality_getTotalPrefireTrees_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getTreeDensityPerUnitArea_1 = Module["_emscripten_bind_SIGMortality_getTreeDensityPerUnitArea_1"] = createExportWrapper("emscripten_bind_SIGMortality_getTreeDensityPerUnitArea_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getTreeHeight_1 = Module["_emscripten_bind_SIGMortality_getTreeHeight_1"] = createExportWrapper("emscripten_bind_SIGMortality_getTreeHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_postfireCanopyCover_0 = Module["_emscripten_bind_SIGMortality_postfireCanopyCover_0"] = createExportWrapper("emscripten_bind_SIGMortality_postfireCanopyCover_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_prefireCanopyCover_0 = Module["_emscripten_bind_SIGMortality_prefireCanopyCover_0"] = createExportWrapper("emscripten_bind_SIGMortality_prefireCanopyCover_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getBarkEquationNumberAtSpeciesTableIndex_1 = Module["_emscripten_bind_SIGMortality_getBarkEquationNumberAtSpeciesTableIndex_1"] = createExportWrapper("emscripten_bind_SIGMortality_getBarkEquationNumberAtSpeciesTableIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getBarkEquationNumberFromSpeciesCode_1 = Module["_emscripten_bind_SIGMortality_getBarkEquationNumberFromSpeciesCode_1"] = createExportWrapper("emscripten_bind_SIGMortality_getBarkEquationNumberFromSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCrownCoefficientCodeAtSpeciesTableIndex_1 = Module["_emscripten_bind_SIGMortality_getCrownCoefficientCodeAtSpeciesTableIndex_1"] = createExportWrapper("emscripten_bind_SIGMortality_getCrownCoefficientCodeAtSpeciesTableIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCrownCoefficientCodeFromSpeciesCode_1 = Module["_emscripten_bind_SIGMortality_getCrownCoefficientCodeFromSpeciesCode_1"] = createExportWrapper("emscripten_bind_SIGMortality_getCrownCoefficientCodeFromSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCrownScorchOrBoleCharEquationNumber_0 = Module["_emscripten_bind_SIGMortality_getCrownScorchOrBoleCharEquationNumber_0"] = createExportWrapper("emscripten_bind_SIGMortality_getCrownScorchOrBoleCharEquationNumber_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getMortalityEquationNumberAtSpeciesTableIndex_1 = Module["_emscripten_bind_SIGMortality_getMortalityEquationNumberAtSpeciesTableIndex_1"] = createExportWrapper("emscripten_bind_SIGMortality_getMortalityEquationNumberAtSpeciesTableIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getMortalityEquationNumberFromSpeciesCode_1 = Module["_emscripten_bind_SIGMortality_getMortalityEquationNumberFromSpeciesCode_1"] = createExportWrapper("emscripten_bind_SIGMortality_getMortalityEquationNumberFromSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getNumberOfRecordsInSpeciesTable_0 = Module["_emscripten_bind_SIGMortality_getNumberOfRecordsInSpeciesTable_0"] = createExportWrapper("emscripten_bind_SIGMortality_getNumberOfRecordsInSpeciesTable_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCode_1 = Module["_emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCode_1"] = createExportWrapper("emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2 = Module["_emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2"] = createExportWrapper("emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getSpeciesCode_0 = Module["_emscripten_bind_SIGMortality_getSpeciesCode_0"] = createExportWrapper("emscripten_bind_SIGMortality_getSpeciesCode_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegion_1 = Module["_emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegion_1"] = createExportWrapper("emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegion_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegionAndEquationType_2 = Module["_emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegionAndEquationType_2"] = createExportWrapper("emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegionAndEquationType_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCommonNameAtSpeciesTableIndex_1 = Module["_emscripten_bind_SIGMortality_getCommonNameAtSpeciesTableIndex_1"] = createExportWrapper("emscripten_bind_SIGMortality_getCommonNameAtSpeciesTableIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getCommonNameFromSpeciesCode_1 = Module["_emscripten_bind_SIGMortality_getCommonNameFromSpeciesCode_1"] = createExportWrapper("emscripten_bind_SIGMortality_getCommonNameFromSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getScientificNameAtSpeciesTableIndex_1 = Module["_emscripten_bind_SIGMortality_getScientificNameAtSpeciesTableIndex_1"] = createExportWrapper("emscripten_bind_SIGMortality_getScientificNameAtSpeciesTableIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getScientificNameFromSpeciesCode_1 = Module["_emscripten_bind_SIGMortality_getScientificNameFromSpeciesCode_1"] = createExportWrapper("emscripten_bind_SIGMortality_getScientificNameFromSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getSpeciesCodeAtSpeciesTableIndex_1 = Module["_emscripten_bind_SIGMortality_getSpeciesCodeAtSpeciesTableIndex_1"] = createExportWrapper("emscripten_bind_SIGMortality_getSpeciesCodeAtSpeciesTableIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getRequiredFieldVector_0 = Module["_emscripten_bind_SIGMortality_getRequiredFieldVector_0"] = createExportWrapper("emscripten_bind_SIGMortality_getRequiredFieldVector_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_calculateMortality_1 = Module["_emscripten_bind_SIGMortality_calculateMortality_1"] = createExportWrapper("emscripten_bind_SIGMortality_calculateMortality_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setBeetleDamage_1 = Module["_emscripten_bind_SIGMortality_setBeetleDamage_1"] = createExportWrapper("emscripten_bind_SIGMortality_setBeetleDamage_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setBoleCharHeight_2 = Module["_emscripten_bind_SIGMortality_setBoleCharHeight_2"] = createExportWrapper("emscripten_bind_SIGMortality_setBoleCharHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setCambiumKillRating_1 = Module["_emscripten_bind_SIGMortality_setCambiumKillRating_1"] = createExportWrapper("emscripten_bind_SIGMortality_setCambiumKillRating_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setCrownDamage_1 = Module["_emscripten_bind_SIGMortality_setCrownDamage_1"] = createExportWrapper("emscripten_bind_SIGMortality_setCrownDamage_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setCrownRatio_1 = Module["_emscripten_bind_SIGMortality_setCrownRatio_1"] = createExportWrapper("emscripten_bind_SIGMortality_setCrownRatio_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setDBH_2 = Module["_emscripten_bind_SIGMortality_setDBH_2"] = createExportWrapper("emscripten_bind_SIGMortality_setDBH_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setEquationType_1 = Module["_emscripten_bind_SIGMortality_setEquationType_1"] = createExportWrapper("emscripten_bind_SIGMortality_setEquationType_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setFireSeverity_1 = Module["_emscripten_bind_SIGMortality_setFireSeverity_1"] = createExportWrapper("emscripten_bind_SIGMortality_setFireSeverity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightSwitch_1 = Module["_emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightSwitch_1"] = createExportWrapper("emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightSwitch_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightValue_2 = Module["_emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightValue_2"] = createExportWrapper("emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightValue_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setRegion_1 = Module["_emscripten_bind_SIGMortality_setRegion_1"] = createExportWrapper("emscripten_bind_SIGMortality_setRegion_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setSpeciesCode_1 = Module["_emscripten_bind_SIGMortality_setSpeciesCode_1"] = createExportWrapper("emscripten_bind_SIGMortality_setSpeciesCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setTreeDensityPerUnitArea_2 = Module["_emscripten_bind_SIGMortality_setTreeDensityPerUnitArea_2"] = createExportWrapper("emscripten_bind_SIGMortality_setTreeDensityPerUnitArea_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_setTreeHeight_2 = Module["_emscripten_bind_SIGMortality_setTreeHeight_2"] = createExportWrapper("emscripten_bind_SIGMortality_setTreeHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2 = Module["_emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2"] = createExportWrapper("emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality___destroy___0 = Module["_emscripten_bind_SIGMortality___destroy___0"] = createExportWrapper("emscripten_bind_SIGMortality___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_WindSpeedUtility_WindSpeedUtility_0 = Module["_emscripten_bind_WindSpeedUtility_WindSpeedUtility_0"] = createExportWrapper("emscripten_bind_WindSpeedUtility_WindSpeedUtility_0");
/** @type {function(...*):?} */
var _emscripten_bind_WindSpeedUtility_windSpeedAtMidflame_2 = Module["_emscripten_bind_WindSpeedUtility_windSpeedAtMidflame_2"] = createExportWrapper("emscripten_bind_WindSpeedUtility_windSpeedAtMidflame_2");
/** @type {function(...*):?} */
var _emscripten_bind_WindSpeedUtility_windSpeedAtTwentyFeetFromTenMeter_1 = Module["_emscripten_bind_WindSpeedUtility_windSpeedAtTwentyFeetFromTenMeter_1"] = createExportWrapper("emscripten_bind_WindSpeedUtility_windSpeedAtTwentyFeetFromTenMeter_1");
/** @type {function(...*):?} */
var _emscripten_bind_WindSpeedUtility___destroy___0 = Module["_emscripten_bind_WindSpeedUtility___destroy___0"] = createExportWrapper("emscripten_bind_WindSpeedUtility___destroy___0");
/** @type {function(...*):?} */
var _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareFeet = Module["_emscripten_enum_AreaUnits_AreaUnitsEnum_SquareFeet"] = createExportWrapper("emscripten_enum_AreaUnits_AreaUnitsEnum_SquareFeet");
/** @type {function(...*):?} */
var _emscripten_enum_AreaUnits_AreaUnitsEnum_Acres = Module["_emscripten_enum_AreaUnits_AreaUnitsEnum_Acres"] = createExportWrapper("emscripten_enum_AreaUnits_AreaUnitsEnum_Acres");
/** @type {function(...*):?} */
var _emscripten_enum_AreaUnits_AreaUnitsEnum_Hectares = Module["_emscripten_enum_AreaUnits_AreaUnitsEnum_Hectares"] = createExportWrapper("emscripten_enum_AreaUnits_AreaUnitsEnum_Hectares");
/** @type {function(...*):?} */
var _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMeters = Module["_emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMeters"] = createExportWrapper("emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMeters");
/** @type {function(...*):?} */
var _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMiles = Module["_emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMiles"] = createExportWrapper("emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMiles");
/** @type {function(...*):?} */
var _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareKilometers = Module["_emscripten_enum_AreaUnits_AreaUnitsEnum_SquareKilometers"] = createExportWrapper("emscripten_enum_AreaUnits_AreaUnitsEnum_SquareKilometers");
/** @type {function(...*):?} */
var _emscripten_enum_LengthUnits_LengthUnitsEnum_Feet = Module["_emscripten_enum_LengthUnits_LengthUnitsEnum_Feet"] = createExportWrapper("emscripten_enum_LengthUnits_LengthUnitsEnum_Feet");
/** @type {function(...*):?} */
var _emscripten_enum_LengthUnits_LengthUnitsEnum_Inches = Module["_emscripten_enum_LengthUnits_LengthUnitsEnum_Inches"] = createExportWrapper("emscripten_enum_LengthUnits_LengthUnitsEnum_Inches");
/** @type {function(...*):?} */
var _emscripten_enum_LengthUnits_LengthUnitsEnum_Centimeters = Module["_emscripten_enum_LengthUnits_LengthUnitsEnum_Centimeters"] = createExportWrapper("emscripten_enum_LengthUnits_LengthUnitsEnum_Centimeters");
/** @type {function(...*):?} */
var _emscripten_enum_LengthUnits_LengthUnitsEnum_Meters = Module["_emscripten_enum_LengthUnits_LengthUnitsEnum_Meters"] = createExportWrapper("emscripten_enum_LengthUnits_LengthUnitsEnum_Meters");
/** @type {function(...*):?} */
var _emscripten_enum_LengthUnits_LengthUnitsEnum_Chains = Module["_emscripten_enum_LengthUnits_LengthUnitsEnum_Chains"] = createExportWrapper("emscripten_enum_LengthUnits_LengthUnitsEnum_Chains");
/** @type {function(...*):?} */
var _emscripten_enum_LengthUnits_LengthUnitsEnum_Miles = Module["_emscripten_enum_LengthUnits_LengthUnitsEnum_Miles"] = createExportWrapper("emscripten_enum_LengthUnits_LengthUnitsEnum_Miles");
/** @type {function(...*):?} */
var _emscripten_enum_LengthUnits_LengthUnitsEnum_Kilometers = Module["_emscripten_enum_LengthUnits_LengthUnitsEnum_Kilometers"] = createExportWrapper("emscripten_enum_LengthUnits_LengthUnitsEnum_Kilometers");
/** @type {function(...*):?} */
var _emscripten_enum_LoadingUnits_LoadingUnitsEnum_PoundsPerSquareFoot = Module["_emscripten_enum_LoadingUnits_LoadingUnitsEnum_PoundsPerSquareFoot"] = createExportWrapper("emscripten_enum_LoadingUnits_LoadingUnitsEnum_PoundsPerSquareFoot");
/** @type {function(...*):?} */
var _emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonsPerAcre = Module["_emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonsPerAcre"] = createExportWrapper("emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonsPerAcre");
/** @type {function(...*):?} */
var _emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonnesPerHectare = Module["_emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonnesPerHectare"] = createExportWrapper("emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonnesPerHectare");
/** @type {function(...*):?} */
var _emscripten_enum_LoadingUnits_LoadingUnitsEnum_KilogramsPerSquareMeter = Module["_emscripten_enum_LoadingUnits_LoadingUnitsEnum_KilogramsPerSquareMeter"] = createExportWrapper("emscripten_enum_LoadingUnits_LoadingUnitsEnum_KilogramsPerSquareMeter");
/** @type {function(...*):?} */
var _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareFeetOverCubicFeet = Module["_emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareFeetOverCubicFeet"] = createExportWrapper("emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareFeetOverCubicFeet");
/** @type {function(...*):?} */
var _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareMetersOverCubicMeters = Module["_emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareMetersOverCubicMeters"] = createExportWrapper("emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareMetersOverCubicMeters");
/** @type {function(...*):?} */
var _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareInchesOverCubicInches = Module["_emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareInchesOverCubicInches"] = createExportWrapper("emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareInchesOverCubicInches");
/** @type {function(...*):?} */
var _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareCentimetersOverCubicCentimeters = Module["_emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareCentimetersOverCubicCentimeters"] = createExportWrapper("emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareCentimetersOverCubicCentimeters");
/** @type {function(...*):?} */
var _emscripten_enum_CoverUnits_CoverUnitsEnum_Fraction = Module["_emscripten_enum_CoverUnits_CoverUnitsEnum_Fraction"] = createExportWrapper("emscripten_enum_CoverUnits_CoverUnitsEnum_Fraction");
/** @type {function(...*):?} */
var _emscripten_enum_CoverUnits_CoverUnitsEnum_Percent = Module["_emscripten_enum_CoverUnits_CoverUnitsEnum_Percent"] = createExportWrapper("emscripten_enum_CoverUnits_CoverUnitsEnum_Percent");
/** @type {function(...*):?} */
var _emscripten_enum_SpeedUnits_SpeedUnitsEnum_FeetPerMinute = Module["_emscripten_enum_SpeedUnits_SpeedUnitsEnum_FeetPerMinute"] = createExportWrapper("emscripten_enum_SpeedUnits_SpeedUnitsEnum_FeetPerMinute");
/** @type {function(...*):?} */
var _emscripten_enum_SpeedUnits_SpeedUnitsEnum_ChainsPerHour = Module["_emscripten_enum_SpeedUnits_SpeedUnitsEnum_ChainsPerHour"] = createExportWrapper("emscripten_enum_SpeedUnits_SpeedUnitsEnum_ChainsPerHour");
/** @type {function(...*):?} */
var _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerSecond = Module["_emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerSecond"] = createExportWrapper("emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerSecond");
/** @type {function(...*):?} */
var _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerMinute = Module["_emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerMinute"] = createExportWrapper("emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerMinute");
/** @type {function(...*):?} */
var _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MilesPerHour = Module["_emscripten_enum_SpeedUnits_SpeedUnitsEnum_MilesPerHour"] = createExportWrapper("emscripten_enum_SpeedUnits_SpeedUnitsEnum_MilesPerHour");
/** @type {function(...*):?} */
var _emscripten_enum_SpeedUnits_SpeedUnitsEnum_KilometersPerHour = Module["_emscripten_enum_SpeedUnits_SpeedUnitsEnum_KilometersPerHour"] = createExportWrapper("emscripten_enum_SpeedUnits_SpeedUnitsEnum_KilometersPerHour");
/** @type {function(...*):?} */
var _emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Fraction = Module["_emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Fraction"] = createExportWrapper("emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Fraction");
/** @type {function(...*):?} */
var _emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Percent = Module["_emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Percent"] = createExportWrapper("emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Percent");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureUnits_MoistureUnitsEnum_Fraction = Module["_emscripten_enum_MoistureUnits_MoistureUnitsEnum_Fraction"] = createExportWrapper("emscripten_enum_MoistureUnits_MoistureUnitsEnum_Fraction");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureUnits_MoistureUnitsEnum_Percent = Module["_emscripten_enum_MoistureUnits_MoistureUnitsEnum_Percent"] = createExportWrapper("emscripten_enum_MoistureUnits_MoistureUnitsEnum_Percent");
/** @type {function(...*):?} */
var _emscripten_enum_SlopeUnits_SlopeUnitsEnum_Degrees = Module["_emscripten_enum_SlopeUnits_SlopeUnitsEnum_Degrees"] = createExportWrapper("emscripten_enum_SlopeUnits_SlopeUnitsEnum_Degrees");
/** @type {function(...*):?} */
var _emscripten_enum_SlopeUnits_SlopeUnitsEnum_Percent = Module["_emscripten_enum_SlopeUnits_SlopeUnitsEnum_Percent"] = createExportWrapper("emscripten_enum_SlopeUnits_SlopeUnitsEnum_Percent");
/** @type {function(...*):?} */
var _emscripten_enum_DensityUnits_DensityUnitsEnum_PoundsPerCubicFoot = Module["_emscripten_enum_DensityUnits_DensityUnitsEnum_PoundsPerCubicFoot"] = createExportWrapper("emscripten_enum_DensityUnits_DensityUnitsEnum_PoundsPerCubicFoot");
/** @type {function(...*):?} */
var _emscripten_enum_DensityUnits_DensityUnitsEnum_KilogramsPerCubicMeter = Module["_emscripten_enum_DensityUnits_DensityUnitsEnum_KilogramsPerCubicMeter"] = createExportWrapper("emscripten_enum_DensityUnits_DensityUnitsEnum_KilogramsPerCubicMeter");
/** @type {function(...*):?} */
var _emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_BtusPerPound = Module["_emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_BtusPerPound"] = createExportWrapper("emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_BtusPerPound");
/** @type {function(...*):?} */
var _emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_KilojoulesPerKilogram = Module["_emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_KilojoulesPerKilogram"] = createExportWrapper("emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_KilojoulesPerKilogram");
/** @type {function(...*):?} */
var _emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_BtusPerCubicFoot = Module["_emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_BtusPerCubicFoot"] = createExportWrapper("emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_BtusPerCubicFoot");
/** @type {function(...*):?} */
var _emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_KilojoulesPerCubicMeter = Module["_emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_KilojoulesPerCubicMeter"] = createExportWrapper("emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_KilojoulesPerCubicMeter");
/** @type {function(...*):?} */
var _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_BtusPerSquareFoot = Module["_emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_BtusPerSquareFoot"] = createExportWrapper("emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_BtusPerSquareFoot");
/** @type {function(...*):?} */
var _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilojoulesPerSquareMeter = Module["_emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilojoulesPerSquareMeter"] = createExportWrapper("emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilojoulesPerSquareMeter");
/** @type {function(...*):?} */
var _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilowattSecondsPerSquareMeter = Module["_emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilowattSecondsPerSquareMeter"] = createExportWrapper("emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilowattSecondsPerSquareMeter");
/** @type {function(...*):?} */
var _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerMinute = Module["_emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerMinute"] = createExportWrapper("emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerMinute");
/** @type {function(...*):?} */
var _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerSecond = Module["_emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerSecond"] = createExportWrapper("emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerSecond");
/** @type {function(...*):?} */
var _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerSecond = Module["_emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerSecond"] = createExportWrapper("emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerSecond");
/** @type {function(...*):?} */
var _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerMinute = Module["_emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerMinute"] = createExportWrapper("emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerMinute");
/** @type {function(...*):?} */
var _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilowattsPerSquareMeter = Module["_emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilowattsPerSquareMeter"] = createExportWrapper("emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilowattsPerSquareMeter");
/** @type {function(...*):?} */
var _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerSecond = Module["_emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerSecond"] = createExportWrapper("emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerSecond");
/** @type {function(...*):?} */
var _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerMinute = Module["_emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerMinute"] = createExportWrapper("emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerMinute");
/** @type {function(...*):?} */
var _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerSecond = Module["_emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerSecond"] = createExportWrapper("emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerSecond");
/** @type {function(...*):?} */
var _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerMinute = Module["_emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerMinute"] = createExportWrapper("emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerMinute");
/** @type {function(...*):?} */
var _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilowattsPerMeter = Module["_emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilowattsPerMeter"] = createExportWrapper("emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilowattsPerMeter");
/** @type {function(...*):?} */
var _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Fahrenheit = Module["_emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Fahrenheit"] = createExportWrapper("emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Fahrenheit");
/** @type {function(...*):?} */
var _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Celsius = Module["_emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Celsius"] = createExportWrapper("emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Celsius");
/** @type {function(...*):?} */
var _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Kelvin = Module["_emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Kelvin"] = createExportWrapper("emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Kelvin");
/** @type {function(...*):?} */
var _emscripten_enum_TimeUnits_TimeUnitsEnum_Minutes = Module["_emscripten_enum_TimeUnits_TimeUnitsEnum_Minutes"] = createExportWrapper("emscripten_enum_TimeUnits_TimeUnitsEnum_Minutes");
/** @type {function(...*):?} */
var _emscripten_enum_TimeUnits_TimeUnitsEnum_Seconds = Module["_emscripten_enum_TimeUnits_TimeUnitsEnum_Seconds"] = createExportWrapper("emscripten_enum_TimeUnits_TimeUnitsEnum_Seconds");
/** @type {function(...*):?} */
var _emscripten_enum_TimeUnits_TimeUnitsEnum_Hours = Module["_emscripten_enum_TimeUnits_TimeUnitsEnum_Hours"] = createExportWrapper("emscripten_enum_TimeUnits_TimeUnitsEnum_Hours");
/** @type {function(...*):?} */
var _emscripten_enum_ContainTactic_HeadAttack = Module["_emscripten_enum_ContainTactic_HeadAttack"] = createExportWrapper("emscripten_enum_ContainTactic_HeadAttack");
/** @type {function(...*):?} */
var _emscripten_enum_ContainTactic_RearAttack = Module["_emscripten_enum_ContainTactic_RearAttack"] = createExportWrapper("emscripten_enum_ContainTactic_RearAttack");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_Unreported = Module["_emscripten_enum_ContainStatus_Unreported"] = createExportWrapper("emscripten_enum_ContainStatus_Unreported");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_Reported = Module["_emscripten_enum_ContainStatus_Reported"] = createExportWrapper("emscripten_enum_ContainStatus_Reported");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_Attacked = Module["_emscripten_enum_ContainStatus_Attacked"] = createExportWrapper("emscripten_enum_ContainStatus_Attacked");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_Contained = Module["_emscripten_enum_ContainStatus_Contained"] = createExportWrapper("emscripten_enum_ContainStatus_Contained");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_Overrun = Module["_emscripten_enum_ContainStatus_Overrun"] = createExportWrapper("emscripten_enum_ContainStatus_Overrun");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_Exhausted = Module["_emscripten_enum_ContainStatus_Exhausted"] = createExportWrapper("emscripten_enum_ContainStatus_Exhausted");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_Overflow = Module["_emscripten_enum_ContainStatus_Overflow"] = createExportWrapper("emscripten_enum_ContainStatus_Overflow");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_SizeLimitExceeded = Module["_emscripten_enum_ContainStatus_SizeLimitExceeded"] = createExportWrapper("emscripten_enum_ContainStatus_SizeLimitExceeded");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_TimeLimitExceeded = Module["_emscripten_enum_ContainStatus_TimeLimitExceeded"] = createExportWrapper("emscripten_enum_ContainStatus_TimeLimitExceeded");
/** @type {function(...*):?} */
var _emscripten_enum_ContainFlank_LeftFlank = Module["_emscripten_enum_ContainFlank_LeftFlank"] = createExportWrapper("emscripten_enum_ContainFlank_LeftFlank");
/** @type {function(...*):?} */
var _emscripten_enum_ContainFlank_RightFlank = Module["_emscripten_enum_ContainFlank_RightFlank"] = createExportWrapper("emscripten_enum_ContainFlank_RightFlank");
/** @type {function(...*):?} */
var _emscripten_enum_ContainFlank_BothFlanks = Module["_emscripten_enum_ContainFlank_BothFlanks"] = createExportWrapper("emscripten_enum_ContainFlank_BothFlanks");
/** @type {function(...*):?} */
var _emscripten_enum_ContainFlank_NeitherFlank = Module["_emscripten_enum_ContainFlank_NeitherFlank"] = createExportWrapper("emscripten_enum_ContainFlank_NeitherFlank");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_PonderosaPineLitter = Module["_emscripten_enum_IgnitionFuelBedType_PonderosaPineLitter"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_PonderosaPineLitter");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_PunkyWoodRottenChunky = Module["_emscripten_enum_IgnitionFuelBedType_PunkyWoodRottenChunky"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_PunkyWoodRottenChunky");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_PunkyWoodPowderDeep = Module["_emscripten_enum_IgnitionFuelBedType_PunkyWoodPowderDeep"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_PunkyWoodPowderDeep");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_PunkWoodPowderShallow = Module["_emscripten_enum_IgnitionFuelBedType_PunkWoodPowderShallow"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_PunkWoodPowderShallow");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_LodgepolePineDuff = Module["_emscripten_enum_IgnitionFuelBedType_LodgepolePineDuff"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_LodgepolePineDuff");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_DouglasFirDuff = Module["_emscripten_enum_IgnitionFuelBedType_DouglasFirDuff"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_DouglasFirDuff");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_HighAltitudeMixed = Module["_emscripten_enum_IgnitionFuelBedType_HighAltitudeMixed"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_HighAltitudeMixed");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_PeatMoss = Module["_emscripten_enum_IgnitionFuelBedType_PeatMoss"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_PeatMoss");
/** @type {function(...*):?} */
var _emscripten_enum_LightningCharge_Negative = Module["_emscripten_enum_LightningCharge_Negative"] = createExportWrapper("emscripten_enum_LightningCharge_Negative");
/** @type {function(...*):?} */
var _emscripten_enum_LightningCharge_Positive = Module["_emscripten_enum_LightningCharge_Positive"] = createExportWrapper("emscripten_enum_LightningCharge_Positive");
/** @type {function(...*):?} */
var _emscripten_enum_LightningCharge_Unknown = Module["_emscripten_enum_LightningCharge_Unknown"] = createExportWrapper("emscripten_enum_LightningCharge_Unknown");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_ENGELMANN_SPRUCE = Module["_emscripten_enum_SpotTreeSpecies_ENGELMANN_SPRUCE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_ENGELMANN_SPRUCE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_DOUGLAS_FIR = Module["_emscripten_enum_SpotTreeSpecies_DOUGLAS_FIR"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_DOUGLAS_FIR");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SUBALPINE_FIR = Module["_emscripten_enum_SpotTreeSpecies_SUBALPINE_FIR"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SUBALPINE_FIR");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_WESTERN_HEMLOCK = Module["_emscripten_enum_SpotTreeSpecies_WESTERN_HEMLOCK"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_WESTERN_HEMLOCK");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_PONDEROSA_PINE = Module["_emscripten_enum_SpotTreeSpecies_PONDEROSA_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_PONDEROSA_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_LODGEPOLE_PINE = Module["_emscripten_enum_SpotTreeSpecies_LODGEPOLE_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_LODGEPOLE_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_WESTERN_WHITE_PINE = Module["_emscripten_enum_SpotTreeSpecies_WESTERN_WHITE_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_WESTERN_WHITE_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_GRAND_FIR = Module["_emscripten_enum_SpotTreeSpecies_GRAND_FIR"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_GRAND_FIR");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_BALSAM_FIR = Module["_emscripten_enum_SpotTreeSpecies_BALSAM_FIR"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_BALSAM_FIR");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SLASH_PINE = Module["_emscripten_enum_SpotTreeSpecies_SLASH_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SLASH_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_LONGLEAF_PINE = Module["_emscripten_enum_SpotTreeSpecies_LONGLEAF_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_LONGLEAF_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_POND_PINE = Module["_emscripten_enum_SpotTreeSpecies_POND_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_POND_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SHORTLEAF_PINE = Module["_emscripten_enum_SpotTreeSpecies_SHORTLEAF_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SHORTLEAF_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_LOBLOLLY_PINE = Module["_emscripten_enum_SpotTreeSpecies_LOBLOLLY_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_LOBLOLLY_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotFireLocation_MIDSLOPE_WINDWARD = Module["_emscripten_enum_SpotFireLocation_MIDSLOPE_WINDWARD"] = createExportWrapper("emscripten_enum_SpotFireLocation_MIDSLOPE_WINDWARD");
/** @type {function(...*):?} */
var _emscripten_enum_SpotFireLocation_VALLEY_BOTTOM = Module["_emscripten_enum_SpotFireLocation_VALLEY_BOTTOM"] = createExportWrapper("emscripten_enum_SpotFireLocation_VALLEY_BOTTOM");
/** @type {function(...*):?} */
var _emscripten_enum_SpotFireLocation_MIDSLOPE_LEEWARD = Module["_emscripten_enum_SpotFireLocation_MIDSLOPE_LEEWARD"] = createExportWrapper("emscripten_enum_SpotFireLocation_MIDSLOPE_LEEWARD");
/** @type {function(...*):?} */
var _emscripten_enum_SpotFireLocation_RIDGE_TOP = Module["_emscripten_enum_SpotFireLocation_RIDGE_TOP"] = createExportWrapper("emscripten_enum_SpotFireLocation_RIDGE_TOP");
/** @type {function(...*):?} */
var _emscripten_enum_SpotArrayConstants_NUM_COLS = Module["_emscripten_enum_SpotArrayConstants_NUM_COLS"] = createExportWrapper("emscripten_enum_SpotArrayConstants_NUM_COLS");
/** @type {function(...*):?} */
var _emscripten_enum_SpotArrayConstants_NUM_FIREBRAND_ROWS = Module["_emscripten_enum_SpotArrayConstants_NUM_FIREBRAND_ROWS"] = createExportWrapper("emscripten_enum_SpotArrayConstants_NUM_FIREBRAND_ROWS");
/** @type {function(...*):?} */
var _emscripten_enum_SpotArrayConstants_NUM_SPECIES = Module["_emscripten_enum_SpotArrayConstants_NUM_SPECIES"] = createExportWrapper("emscripten_enum_SpotArrayConstants_NUM_SPECIES");
/** @type {function(...*):?} */
var _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low = Module["_emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low"] = createExportWrapper("emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low");
/** @type {function(...*):?} */
var _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate = Module["_emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate"] = createExportWrapper("emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate");
/** @type {function(...*):?} */
var _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_NoMethod = Module["_emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_NoMethod"] = createExportWrapper("emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_NoMethod");
/** @type {function(...*):?} */
var _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Arithmetic = Module["_emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Arithmetic"] = createExportWrapper("emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Arithmetic");
/** @type {function(...*):?} */
var _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Harmonic = Module["_emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Harmonic"] = createExportWrapper("emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Harmonic");
/** @type {function(...*):?} */
var _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_TwoDimensional = Module["_emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_TwoDimensional"] = createExportWrapper("emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_TwoDimensional");
/** @type {function(...*):?} */
var _emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Unsheltered = Module["_emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Unsheltered"] = createExportWrapper("emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Unsheltered");
/** @type {function(...*):?} */
var _emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Sheltered = Module["_emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Sheltered"] = createExportWrapper("emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Sheltered");
/** @type {function(...*):?} */
var _emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UserInput = Module["_emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UserInput"] = createExportWrapper("emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UserInput");
/** @type {function(...*):?} */
var _emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UseCrownRatio = Module["_emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UseCrownRatio"] = createExportWrapper("emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UseCrownRatio");
/** @type {function(...*):?} */
var _emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_DontUseCrownRatio = Module["_emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_DontUseCrownRatio"] = createExportWrapper("emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_DontUseCrownRatio");
/** @type {function(...*):?} */
var _emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToUpslope = Module["_emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToUpslope"] = createExportWrapper("emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToUpslope");
/** @type {function(...*):?} */
var _emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToNorth = Module["_emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToNorth"] = createExportWrapper("emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToNorth");
/** @type {function(...*):?} */
var _emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_DirectMidflame = Module["_emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_DirectMidflame"] = createExportWrapper("emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_DirectMidflame");
/** @type {function(...*):?} */
var _emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TwentyFoot = Module["_emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TwentyFoot"] = createExportWrapper("emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TwentyFoot");
/** @type {function(...*):?} */
var _emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TenMeter = Module["_emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TenMeter"] = createExportWrapper("emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TenMeter");
/** @type {function(...*):?} */
var _emscripten_enum_FireType_FireTypeEnum_Surface = Module["_emscripten_enum_FireType_FireTypeEnum_Surface"] = createExportWrapper("emscripten_enum_FireType_FireTypeEnum_Surface");
/** @type {function(...*):?} */
var _emscripten_enum_FireType_FireTypeEnum_Torching = Module["_emscripten_enum_FireType_FireTypeEnum_Torching"] = createExportWrapper("emscripten_enum_FireType_FireTypeEnum_Torching");
/** @type {function(...*):?} */
var _emscripten_enum_FireType_FireTypeEnum_ConditionalCrownFire = Module["_emscripten_enum_FireType_FireTypeEnum_ConditionalCrownFire"] = createExportWrapper("emscripten_enum_FireType_FireTypeEnum_ConditionalCrownFire");
/** @type {function(...*):?} */
var _emscripten_enum_FireType_FireTypeEnum_Crowning = Module["_emscripten_enum_FireType_FireTypeEnum_Crowning"] = createExportWrapper("emscripten_enum_FireType_FireTypeEnum_Crowning");
/** @type {function(...*):?} */
var _emscripten_enum_BeetleDamage_not_set = Module["_emscripten_enum_BeetleDamage_not_set"] = createExportWrapper("emscripten_enum_BeetleDamage_not_set");
/** @type {function(...*):?} */
var _emscripten_enum_BeetleDamage_no = Module["_emscripten_enum_BeetleDamage_no"] = createExportWrapper("emscripten_enum_BeetleDamage_no");
/** @type {function(...*):?} */
var _emscripten_enum_BeetleDamage_yes = Module["_emscripten_enum_BeetleDamage_yes"] = createExportWrapper("emscripten_enum_BeetleDamage_yes");
/** @type {function(...*):?} */
var _emscripten_enum_FireSeverity_not_set = Module["_emscripten_enum_FireSeverity_not_set"] = createExportWrapper("emscripten_enum_FireSeverity_not_set");
/** @type {function(...*):?} */
var _emscripten_enum_FireSeverity_empty = Module["_emscripten_enum_FireSeverity_empty"] = createExportWrapper("emscripten_enum_FireSeverity_empty");
/** @type {function(...*):?} */
var _emscripten_enum_FireSeverity_low = Module["_emscripten_enum_FireSeverity_low"] = createExportWrapper("emscripten_enum_FireSeverity_low");
/** @type {function(...*):?} */
var _emscripten_enum_FlameLengthOrScorchHeightSwitch_flame_length = Module["_emscripten_enum_FlameLengthOrScorchHeightSwitch_flame_length"] = createExportWrapper("emscripten_enum_FlameLengthOrScorchHeightSwitch_flame_length");
/** @type {function(...*):?} */
var _emscripten_enum_FlameLengthOrScorchHeightSwitch_scorch_height = Module["_emscripten_enum_FlameLengthOrScorchHeightSwitch_scorch_height"] = createExportWrapper("emscripten_enum_FlameLengthOrScorchHeightSwitch_scorch_height");
/** @type {function(...*):?} */
var _emscripten_enum_RegionCode_interior_west = Module["_emscripten_enum_RegionCode_interior_west"] = createExportWrapper("emscripten_enum_RegionCode_interior_west");
/** @type {function(...*):?} */
var _emscripten_enum_RegionCode_pacific_west = Module["_emscripten_enum_RegionCode_pacific_west"] = createExportWrapper("emscripten_enum_RegionCode_pacific_west");
/** @type {function(...*):?} */
var _emscripten_enum_RegionCode_north_east = Module["_emscripten_enum_RegionCode_north_east"] = createExportWrapper("emscripten_enum_RegionCode_north_east");
/** @type {function(...*):?} */
var _emscripten_enum_RegionCode_south_east = Module["_emscripten_enum_RegionCode_south_east"] = createExportWrapper("emscripten_enum_RegionCode_south_east");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageType_not_set = Module["_emscripten_enum_CrownDamageType_not_set"] = createExportWrapper("emscripten_enum_CrownDamageType_not_set");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageType_crown_length = Module["_emscripten_enum_CrownDamageType_crown_length"] = createExportWrapper("emscripten_enum_CrownDamageType_crown_length");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageType_crown_volume = Module["_emscripten_enum_CrownDamageType_crown_volume"] = createExportWrapper("emscripten_enum_CrownDamageType_crown_volume");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageType_crown_kill = Module["_emscripten_enum_CrownDamageType_crown_kill"] = createExportWrapper("emscripten_enum_CrownDamageType_crown_kill");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_region = Module["_emscripten_enum_RequiredFieldNames_region"] = createExportWrapper("emscripten_enum_RequiredFieldNames_region");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_switch = Module["_emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_switch"] = createExportWrapper("emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_switch");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_value = Module["_emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_value"] = createExportWrapper("emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_value");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_equation_type = Module["_emscripten_enum_RequiredFieldNames_equation_type"] = createExportWrapper("emscripten_enum_RequiredFieldNames_equation_type");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_dbh = Module["_emscripten_enum_RequiredFieldNames_dbh"] = createExportWrapper("emscripten_enum_RequiredFieldNames_dbh");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_tree_height = Module["_emscripten_enum_RequiredFieldNames_tree_height"] = createExportWrapper("emscripten_enum_RequiredFieldNames_tree_height");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_crown_ratio = Module["_emscripten_enum_RequiredFieldNames_crown_ratio"] = createExportWrapper("emscripten_enum_RequiredFieldNames_crown_ratio");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_crown_damage = Module["_emscripten_enum_RequiredFieldNames_crown_damage"] = createExportWrapper("emscripten_enum_RequiredFieldNames_crown_damage");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_cambium_kill_rating = Module["_emscripten_enum_RequiredFieldNames_cambium_kill_rating"] = createExportWrapper("emscripten_enum_RequiredFieldNames_cambium_kill_rating");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_beetle_damage = Module["_emscripten_enum_RequiredFieldNames_beetle_damage"] = createExportWrapper("emscripten_enum_RequiredFieldNames_beetle_damage");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_bole_char_height = Module["_emscripten_enum_RequiredFieldNames_bole_char_height"] = createExportWrapper("emscripten_enum_RequiredFieldNames_bole_char_height");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_bark_thickness = Module["_emscripten_enum_RequiredFieldNames_bark_thickness"] = createExportWrapper("emscripten_enum_RequiredFieldNames_bark_thickness");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_fire_severity = Module["_emscripten_enum_RequiredFieldNames_fire_severity"] = createExportWrapper("emscripten_enum_RequiredFieldNames_fire_severity");
/** @type {function(...*):?} */
var _emscripten_enum_RequiredFieldNames_num_inputs = Module["_emscripten_enum_RequiredFieldNames_num_inputs"] = createExportWrapper("emscripten_enum_RequiredFieldNames_num_inputs");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_not_set = Module["_emscripten_enum_CrownDamageEquationCode_not_set"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_not_set");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_white_fir = Module["_emscripten_enum_CrownDamageEquationCode_white_fir"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_white_fir");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_subalpine_fir = Module["_emscripten_enum_CrownDamageEquationCode_subalpine_fir"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_subalpine_fir");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_incense_cedar = Module["_emscripten_enum_CrownDamageEquationCode_incense_cedar"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_incense_cedar");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_western_larch = Module["_emscripten_enum_CrownDamageEquationCode_western_larch"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_western_larch");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_whitebark_pine = Module["_emscripten_enum_CrownDamageEquationCode_whitebark_pine"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_whitebark_pine");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_engelmann_spruce = Module["_emscripten_enum_CrownDamageEquationCode_engelmann_spruce"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_engelmann_spruce");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_sugar_pine = Module["_emscripten_enum_CrownDamageEquationCode_sugar_pine"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_sugar_pine");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_red_fir = Module["_emscripten_enum_CrownDamageEquationCode_red_fir"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_red_fir");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_ponderosa_pine = Module["_emscripten_enum_CrownDamageEquationCode_ponderosa_pine"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_ponderosa_pine");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_ponderosa_kill = Module["_emscripten_enum_CrownDamageEquationCode_ponderosa_kill"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_ponderosa_kill");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageEquationCode_douglas_fir = Module["_emscripten_enum_CrownDamageEquationCode_douglas_fir"] = createExportWrapper("emscripten_enum_CrownDamageEquationCode_douglas_fir");
/** @type {function(...*):?} */
var _emscripten_enum_EquationType_not_set = Module["_emscripten_enum_EquationType_not_set"] = createExportWrapper("emscripten_enum_EquationType_not_set");
/** @type {function(...*):?} */
var _emscripten_enum_EquationType_crown_scorch = Module["_emscripten_enum_EquationType_crown_scorch"] = createExportWrapper("emscripten_enum_EquationType_crown_scorch");
/** @type {function(...*):?} */
var _emscripten_enum_EquationType_bole_char = Module["_emscripten_enum_EquationType_bole_char"] = createExportWrapper("emscripten_enum_EquationType_bole_char");
/** @type {function(...*):?} */
var _emscripten_enum_EquationType_crown_damage = Module["_emscripten_enum_EquationType_crown_damage"] = createExportWrapper("emscripten_enum_EquationType_crown_damage");
/** @type {function(...*):?} */
var ___cxa_free_exception = createExportWrapper("__cxa_free_exception");
/** @type {function(...*):?} */
var ___errno_location = createExportWrapper("__errno_location");
/** @type {function(...*):?} */
var _malloc = Module["_malloc"] = createExportWrapper("malloc");
/** @type {function(...*):?} */
var _free = Module["_free"] = createExportWrapper("free");
/** @type {function(...*):?} */
var _setThrew = createExportWrapper("setThrew");
/** @type {function(...*):?} */
var setTempRet0 = createExportWrapper("setTempRet0");
/** @type {function(...*):?} */
var _emscripten_stack_init = function() {
  return (_emscripten_stack_init = Module["asm"]["emscripten_stack_init"]).apply(null, arguments);
};

/** @type {function(...*):?} */
var _emscripten_stack_get_free = function() {
  return (_emscripten_stack_get_free = Module["asm"]["emscripten_stack_get_free"]).apply(null, arguments);
};

/** @type {function(...*):?} */
var _emscripten_stack_get_base = function() {
  return (_emscripten_stack_get_base = Module["asm"]["emscripten_stack_get_base"]).apply(null, arguments);
};

/** @type {function(...*):?} */
var _emscripten_stack_get_end = function() {
  return (_emscripten_stack_get_end = Module["asm"]["emscripten_stack_get_end"]).apply(null, arguments);
};

/** @type {function(...*):?} */
var stackSave = createExportWrapper("stackSave");
/** @type {function(...*):?} */
var stackRestore = createExportWrapper("stackRestore");
/** @type {function(...*):?} */
var stackAlloc = createExportWrapper("stackAlloc");
/** @type {function(...*):?} */
var _emscripten_stack_get_current = function() {
  return (_emscripten_stack_get_current = Module["asm"]["emscripten_stack_get_current"]).apply(null, arguments);
};

/** @type {function(...*):?} */
var ___get_exception_message = Module["___get_exception_message"] = createExportWrapper("__get_exception_message");
/** @type {function(...*):?} */
var ___cxa_can_catch = createExportWrapper("__cxa_can_catch");
/** @type {function(...*):?} */
var ___cxa_is_pointer_type = createExportWrapper("__cxa_is_pointer_type");
/** @type {function(...*):?} */
var dynCall_jiji = Module["dynCall_jiji"] = createExportWrapper("dynCall_jiji");
var ___start_em_js = Module['___start_em_js'] = 111960;
var ___stop_em_js = Module['___stop_em_js'] = 112058;
function invoke_vii(index,a1,a2) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_vi(index,a1) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_iiii(index,a1,a2,a3) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2,a3);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_iii(index,a1,a2) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_viiiiddddddddddddii(index,a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,a14,a15,a16,a17,a18) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,a14,a15,a16,a17,a18);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_ii(index,a1) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_viiii(index,a1,a2,a3,a4) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2,a3,a4);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_iiiii(index,a1,a2,a3,a4) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2,a3,a4);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_iiiiii(index,a1,a2,a3,a4,a5) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2,a3,a4,a5);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_viii(index,a1,a2,a3) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2,a3);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_viiiii(index,a1,a2,a3,a4,a5) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2,a3,a4,a5);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_diiidiiiii(index,a1,a2,a3,a4,a5,a6,a7,a8,a9) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2,a3,a4,a5,a6,a7,a8,a9);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_iiiiidididdidddddidddii(index,a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,a14,a15,a16,a17,a18,a19,a20,a21,a22) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,a14,a15,a16,a17,a18,a19,a20,a21,a22);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_iidddiidd(index,a1,a2,a3,a4,a5,a6,a7,a8) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2,a3,a4,a5,a6,a7,a8);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_iiddiiddiidid(index,a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_iiddiidiidiiiii(index,a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,a14) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,a14);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_vididi(index,a1,a2,a3,a4,a5) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2,a3,a4,a5);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_viiiiiiiiiiiii(index,a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13);
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}

function invoke_v(index) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)();
  } catch(e) {
    stackRestore(sp);
    if (!(e instanceof EmscriptenEH)) throw e;
    _setThrew(1, 0);
  }
}


// include: postamble.js
// === Auto-generated postamble setup entry stuff ===

Module["UTF8ToString"] = UTF8ToString;
Module["addFunction"] = addFunction;
Module["allocateUTF8"] = allocateUTF8;
var missingLibrarySymbols = [
  'zeroMemory',
  'stringToNewUTF8',
  'exitJS',
  'setErrNo',
  'inetPton4',
  'inetNtop4',
  'inetPton6',
  'inetNtop6',
  'readSockaddr',
  'writeSockaddr',
  'getHostByName',
  'getRandomDevice',
  'traverseStack',
  'convertPCtoSourceLocation',
  'readEmAsmArgs',
  'jstoi_q',
  'jstoi_s',
  'getExecutableName',
  'listenOnce',
  'autoResumeAudioContext',
  'dynCallLegacy',
  'getDynCaller',
  'dynCall',
  'handleException',
  'runtimeKeepalivePush',
  'runtimeKeepalivePop',
  'callUserCallback',
  'maybeExit',
  'safeSetTimeout',
  'asmjsMangle',
  'asyncLoad',
  'alignMemory',
  'mmapAlloc',
  'HandleAllocator',
  'getNativeTypeSize',
  'STACK_SIZE',
  'STACK_ALIGN',
  'POINTER_SIZE',
  'ASSERTIONS',
  'writeI53ToI64',
  'writeI53ToI64Clamped',
  'writeI53ToI64Signaling',
  'writeI53ToU64Clamped',
  'writeI53ToU64Signaling',
  'readI53FromI64',
  'readI53FromU64',
  'convertI32PairToI53',
  'convertU32PairToI53',
  'getCFunc',
  'ccall',
  'cwrap',
  'removeFunction',
  'reallyNegative',
  'unSign',
  'strLen',
  'reSign',
  'formatString',
  'intArrayToString',
  'AsciiToString',
  'stringToAscii',
  'UTF16ToString',
  'stringToUTF16',
  'lengthBytesUTF16',
  'UTF32ToString',
  'stringToUTF32',
  'lengthBytesUTF32',
  'allocateUTF8OnStack',
  'writeStringToMemory',
  'writeArrayToMemory',
  'writeAsciiToMemory',
  'getSocketFromFD',
  'getSocketAddress',
  'registerKeyEventCallback',
  'maybeCStringToJsString',
  'findEventTarget',
  'findCanvasEventTarget',
  'getBoundingClientRect',
  'fillMouseEventData',
  'registerMouseEventCallback',
  'registerWheelEventCallback',
  'registerUiEventCallback',
  'registerFocusEventCallback',
  'fillDeviceOrientationEventData',
  'registerDeviceOrientationEventCallback',
  'fillDeviceMotionEventData',
  'registerDeviceMotionEventCallback',
  'screenOrientation',
  'fillOrientationChangeEventData',
  'registerOrientationChangeEventCallback',
  'fillFullscreenChangeEventData',
  'registerFullscreenChangeEventCallback',
  'JSEvents_requestFullscreen',
  'JSEvents_resizeCanvasForFullscreen',
  'registerRestoreOldStyle',
  'hideEverythingExceptGivenElement',
  'restoreHiddenElements',
  'setLetterbox',
  'softFullscreenResizeWebGLRenderTarget',
  'doRequestFullscreen',
  'fillPointerlockChangeEventData',
  'registerPointerlockChangeEventCallback',
  'registerPointerlockErrorEventCallback',
  'requestPointerLock',
  'fillVisibilityChangeEventData',
  'registerVisibilityChangeEventCallback',
  'registerTouchEventCallback',
  'fillGamepadEventData',
  'registerGamepadEventCallback',
  'registerBeforeUnloadEventCallback',
  'fillBatteryEventData',
  'battery',
  'registerBatteryEventCallback',
  'setCanvasElementSize',
  'getCanvasElementSize',
  'demangle',
  'demangleAll',
  'jsStackTrace',
  'stackTrace',
  'getEnvStrings',
  'checkWasiClock',
  'createDyncallWrapper',
  'setImmediateWrapped',
  'clearImmediateWrapped',
  'polyfillSetImmediate',
  'getPromise',
  'makePromise',
  'makePromiseCallback',
  'setMainLoop',
  '_setNetworkCallback',
  'heapObjectForWebGLType',
  'heapAccessShiftForWebGLHeap',
  'emscriptenWebGLGet',
  'computeUnpackAlignedImageSize',
  'emscriptenWebGLGetTexPixelData',
  'emscriptenWebGLGetUniform',
  'webglGetUniformLocation',
  'webglPrepareUniformLocationsBeforeFirstUse',
  'webglGetLeftBracePos',
  'emscriptenWebGLGetVertexAttrib',
  'writeGLArray',
  'SDL_unicode',
  'SDL_ttfContext',
  'SDL_audio',
  'GLFW_Window',
  'runAndAbortIfError',
  'ALLOC_NORMAL',
  'ALLOC_STACK',
  'allocate',
];
missingLibrarySymbols.forEach(missingLibrarySymbol)

var unexportedSymbols = [
  'run',
  'UTF8ArrayToString',
  'stringToUTF8Array',
  'stringToUTF8',
  'lengthBytesUTF8',
  'addOnPreRun',
  'addOnInit',
  'addOnPreMain',
  'addOnExit',
  'addOnPostRun',
  'addRunDependency',
  'removeRunDependency',
  'FS_createFolder',
  'FS_createPath',
  'FS_createDataFile',
  'FS_createPreloadedFile',
  'FS_createLazyFile',
  'FS_createLink',
  'FS_createDevice',
  'FS_unlink',
  'out',
  'err',
  'callMain',
  'abort',
  'keepRuntimeAlive',
  'wasmMemory',
  'stackAlloc',
  'stackSave',
  'stackRestore',
  'getTempRet0',
  'setTempRet0',
  'writeStackCookie',
  'checkStackCookie',
  'ptrToString',
  'getHeapMax',
  'emscripten_realloc_buffer',
  'ENV',
  'ERRNO_CODES',
  'ERRNO_MESSAGES',
  'DNS',
  'Protocols',
  'Sockets',
  'timers',
  'warnOnce',
  'UNWIND_CACHE',
  'readEmAsmArgsArray',
  'convertI32PairToI53Checked',
  'uleb128Encode',
  'sigToWasmTypes',
  'generateFuncType',
  'convertJsFunctionToWasm',
  'freeTableIndexes',
  'functionsInTableMap',
  'getEmptyTableSlot',
  'updateTableMap',
  'getFunctionAddress',
  'setValue',
  'getValue',
  'PATH',
  'PATH_FS',
  'intArrayFromString',
  'UTF16Decoder',
  'SYSCALLS',
  'JSEvents',
  'specialHTMLTargets',
  'currentFullscreenStrategy',
  'restoreOldWindowedStyle',
  'ExitStatus',
  'flush_NO_FILESYSTEM',
  'dlopenMissingError',
  'promiseMap',
  'uncaughtExceptionCount',
  'exceptionLast',
  'exceptionCaught',
  'ExceptionInfo',
  'exception_addRef',
  'exception_decRef',
  'getExceptionMessageCommon',
  'incrementExceptionRefcount',
  'decrementExceptionRefcount',
  'getExceptionMessage',
  'Browser',
  'wget',
  'FS',
  'MEMFS',
  'TTY',
  'PIPEFS',
  'SOCKFS',
  'tempFixedLengthArray',
  'miniTempWebGLFloatBuffers',
  'GL',
  'AL',
  'SDL',
  'SDL_gfx',
  'GLUT',
  'EGL',
  'GLFW',
  'GLEW',
  'IDBStore',
];
unexportedSymbols.forEach(unexportedRuntimeSymbol);



var calledRun;

dependenciesFulfilled = function runCaller() {
  // If run has never been called, and we should call run (INVOKE_RUN is true, and Module.noInitialRun is not false)
  if (!calledRun) run();
  if (!calledRun) dependenciesFulfilled = runCaller; // try this again later, after new deps are fulfilled
};

function stackCheckInit() {
  // This is normally called automatically during __wasm_call_ctors but need to
  // get these values before even running any of the ctors so we call it redundantly
  // here.
  _emscripten_stack_init();
  // TODO(sbc): Move writeStackCookie to native to to avoid this.
  writeStackCookie();
}

function run() {

  if (runDependencies > 0) {
    return;
  }

    stackCheckInit();

  preRun();

  // a preRun added a dependency, run will be called later
  if (runDependencies > 0) {
    return;
  }

  function doRun() {
    // run may have just been called through dependencies being fulfilled just in this very frame,
    // or while the async setStatus time below was happening
    if (calledRun) return;
    calledRun = true;
    Module['calledRun'] = true;

    if (ABORT) return;

    initRuntime();

    if (Module['onRuntimeInitialized']) Module['onRuntimeInitialized']();

    assert(!Module['_main'], 'compiled without a main, but one is present. if you added it from JS, use Module["onRuntimeInitialized"]');

    postRun();
  }

  if (Module['setStatus']) {
    Module['setStatus']('Running...');
    setTimeout(function() {
      setTimeout(function() {
        Module['setStatus']('');
      }, 1);
      doRun();
    }, 1);
  } else
  {
    doRun();
  }
  checkStackCookie();
}

function checkUnflushedContent() {
  // Compiler settings do not allow exiting the runtime, so flushing
  // the streams is not possible. but in ASSERTIONS mode we check
  // if there was something to flush, and if so tell the user they
  // should request that the runtime be exitable.
  // Normally we would not even include flush() at all, but in ASSERTIONS
  // builds we do so just for this check, and here we see if there is any
  // content to flush, that is, we check if there would have been
  // something a non-ASSERTIONS build would have not seen.
  // How we flush the streams depends on whether we are in SYSCALLS_REQUIRE_FILESYSTEM=0
  // mode (which has its own special function for this; otherwise, all
  // the code is inside libc)
  var oldOut = out;
  var oldErr = err;
  var has = false;
  out = err = (x) => {
    has = true;
  }
  try { // it doesn't matter if it fails
    flush_NO_FILESYSTEM();
  } catch(e) {}
  out = oldOut;
  err = oldErr;
  if (has) {
    warnOnce('stdio streams had content in them that was not flushed. you should set EXIT_RUNTIME to 1 (see the FAQ), or make sure to emit a newline when you printf etc.');
    warnOnce('(this may also be due to not including full filesystem support - try building with -sFORCE_FILESYSTEM)');
  }
}

if (Module['preInit']) {
  if (typeof Module['preInit'] == 'function') Module['preInit'] = [Module['preInit']];
  while (Module['preInit'].length > 0) {
    Module['preInit'].pop()();
  }
}

run();


// end include: postamble.js
// include: /home/kcheung/work/code/behave-polylith/behave-lib/include/js/glue.js

// Bindings utilities

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function WrapperObject() {
}
WrapperObject.prototype = Object.create(WrapperObject.prototype);
WrapperObject.prototype.constructor = WrapperObject;
WrapperObject.prototype.__class__ = WrapperObject;
WrapperObject.__cache__ = {};
Module['WrapperObject'] = WrapperObject;

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant)
    @param {*=} __class__ */
function getCache(__class__) {
  return (__class__ || WrapperObject).__cache__;
}
Module['getCache'] = getCache;

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant)
    @param {*=} __class__ */
function wrapPointer(ptr, __class__) {
  var cache = getCache(__class__);
  var ret = cache[ptr];
  if (ret) return ret;
  ret = Object.create((__class__ || WrapperObject).prototype);
  ret.ptr = ptr;
  return cache[ptr] = ret;
}
Module['wrapPointer'] = wrapPointer;

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function castObject(obj, __class__) {
  return wrapPointer(obj.ptr, __class__);
}
Module['castObject'] = castObject;

Module['NULL'] = wrapPointer(0);

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function destroy(obj) {
  if (!obj['__destroy__']) throw 'Error: Cannot destroy object. (Did you create it yourself?)';
  obj['__destroy__']();
  // Remove from cache, so the object can be GC'd and refs added onto it released
  delete getCache(obj.__class__)[obj.ptr];
}
Module['destroy'] = destroy;

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function compare(obj1, obj2) {
  return obj1.ptr === obj2.ptr;
}
Module['compare'] = compare;

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function getPointer(obj) {
  return obj.ptr;
}
Module['getPointer'] = getPointer;

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function getClass(obj) {
  return obj.__class__;
}
Module['getClass'] = getClass;

// Converts big (string or array) values into a C-style storage, in temporary space

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
var ensureCache = {
  buffer: 0,  // the main buffer of temporary storage
  size: 0,   // the size of buffer
  pos: 0,    // the next free offset in buffer
  temps: [], // extra allocations
  needed: 0, // the total size we need next time

  prepare: function() {
    if (ensureCache.needed) {
      // clear the temps
      for (var i = 0; i < ensureCache.temps.length; i++) {
        Module['_free'](ensureCache.temps[i]);
      }
      ensureCache.temps.length = 0;
      // prepare to allocate a bigger buffer
      Module['_free'](ensureCache.buffer);
      ensureCache.buffer = 0;
      ensureCache.size += ensureCache.needed;
      // clean up
      ensureCache.needed = 0;
    }
    if (!ensureCache.buffer) { // happens first time, or when we need to grow
      ensureCache.size += 128; // heuristic, avoid many small grow events
      ensureCache.buffer = Module['_malloc'](ensureCache.size);
      assert(ensureCache.buffer);
    }
    ensureCache.pos = 0;
  },
  alloc: function(array, view) {
    assert(ensureCache.buffer);
    var bytes = view.BYTES_PER_ELEMENT;
    var len = array.length * bytes;
    len = (len + 7) & -8; // keep things aligned to 8 byte boundaries
    var ret;
    if (ensureCache.pos + len >= ensureCache.size) {
      // we failed to allocate in the buffer, ensureCache time around :(
      assert(len > 0); // null terminator, at least
      ensureCache.needed += len;
      ret = Module['_malloc'](len);
      ensureCache.temps.push(ret);
    } else {
      // we can allocate in the buffer
      ret = ensureCache.buffer + ensureCache.pos;
      ensureCache.pos += len;
    }
    return ret;
  },
  copy: function(array, view, offset) {
    offset >>>= 0;
    var bytes = view.BYTES_PER_ELEMENT;
    switch (bytes) {
      case 2: offset >>>= 1; break;
      case 4: offset >>>= 2; break;
      case 8: offset >>>= 3; break;
    }
    for (var i = 0; i < array.length; i++) {
      view[offset + i] = array[i];
    }
  },
};

/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function ensureString(value) {
  if (typeof value === 'string') {
    var intArray = intArrayFromString(value);
    var offset = ensureCache.alloc(intArray, HEAP8);
    ensureCache.copy(intArray, HEAP8, offset);
    return offset;
  }
  return value;
}
/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function ensureInt8(value) {
  if (typeof value === 'object') {
    var offset = ensureCache.alloc(value, HEAP8);
    ensureCache.copy(value, HEAP8, offset);
    return offset;
  }
  return value;
}
/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function ensureInt16(value) {
  if (typeof value === 'object') {
    var offset = ensureCache.alloc(value, HEAP16);
    ensureCache.copy(value, HEAP16, offset);
    return offset;
  }
  return value;
}
/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function ensureInt32(value) {
  if (typeof value === 'object') {
    var offset = ensureCache.alloc(value, HEAP32);
    ensureCache.copy(value, HEAP32, offset);
    return offset;
  }
  return value;
}
/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function ensureFloat32(value) {
  if (typeof value === 'object') {
    var offset = ensureCache.alloc(value, HEAPF32);
    ensureCache.copy(value, HEAPF32, offset);
    return offset;
  }
  return value;
}
/** @suppress {duplicate} (TODO: avoid emitting this multiple times, it is redundant) */
function ensureFloat64(value) {
  if (typeof value === 'object') {
    var offset = ensureCache.alloc(value, HEAPF64);
    ensureCache.copy(value, HEAPF64, offset);
    return offset;
  }
  return value;
}

// VoidPtr
/** @suppress {undefinedVars, duplicate} @this{Object} */function VoidPtr() { throw "cannot construct a VoidPtr, no constructor in IDL" }
VoidPtr.prototype = Object.create(WrapperObject.prototype);
VoidPtr.prototype.constructor = VoidPtr;
VoidPtr.prototype.__class__ = VoidPtr;
VoidPtr.__cache__ = {};
Module['VoidPtr'] = VoidPtr;

  VoidPtr.prototype['__destroy__'] = VoidPtr.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_VoidPtr___destroy___0(self);
};
// DoublePtr
/** @suppress {undefinedVars, duplicate} @this{Object} */function DoublePtr() { throw "cannot construct a DoublePtr, no constructor in IDL" }
DoublePtr.prototype = Object.create(WrapperObject.prototype);
DoublePtr.prototype.constructor = DoublePtr;
DoublePtr.prototype.__class__ = DoublePtr;
DoublePtr.__cache__ = {};
Module['DoublePtr'] = DoublePtr;

  DoublePtr.prototype['__destroy__'] = DoublePtr.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_DoublePtr___destroy___0(self);
};
// BoolVector
/** @suppress {undefinedVars, duplicate} @this{Object} */function BoolVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_BoolVector_BoolVector_0(); getCache(BoolVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_BoolVector_BoolVector_1(size);
  getCache(BoolVector)[this.ptr] = this;
};;
BoolVector.prototype = Object.create(WrapperObject.prototype);
BoolVector.prototype.constructor = BoolVector;
BoolVector.prototype.__class__ = BoolVector;
BoolVector.__cache__ = {};
Module['BoolVector'] = BoolVector;

BoolVector.prototype['resize'] = BoolVector.prototype.resize = /** @suppress {undefinedVars, duplicate} @this{Object} */function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_BoolVector_resize_1(self, size);
};;

BoolVector.prototype['get'] = BoolVector.prototype.get = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return !!(_emscripten_bind_BoolVector_get_1(self, i));
};;

BoolVector.prototype['set'] = BoolVector.prototype.set = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_BoolVector_set_2(self, i, val);
};;

BoolVector.prototype['size'] = BoolVector.prototype.size = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_BoolVector_size_0(self);
};;

  BoolVector.prototype['__destroy__'] = BoolVector.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_BoolVector___destroy___0(self);
};
// CharVector
/** @suppress {undefinedVars, duplicate} @this{Object} */function CharVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_CharVector_CharVector_0(); getCache(CharVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_CharVector_CharVector_1(size);
  getCache(CharVector)[this.ptr] = this;
};;
CharVector.prototype = Object.create(WrapperObject.prototype);
CharVector.prototype.constructor = CharVector;
CharVector.prototype.__class__ = CharVector;
CharVector.__cache__ = {};
Module['CharVector'] = CharVector;

CharVector.prototype['resize'] = CharVector.prototype.resize = /** @suppress {undefinedVars, duplicate} @this{Object} */function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_CharVector_resize_1(self, size);
};;

CharVector.prototype['get'] = CharVector.prototype.get = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return _emscripten_bind_CharVector_get_1(self, i);
};;

CharVector.prototype['set'] = CharVector.prototype.set = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_CharVector_set_2(self, i, val);
};;

CharVector.prototype['size'] = CharVector.prototype.size = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_CharVector_size_0(self);
};;

  CharVector.prototype['__destroy__'] = CharVector.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_CharVector___destroy___0(self);
};
// IntVector
/** @suppress {undefinedVars, duplicate} @this{Object} */function IntVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_IntVector_IntVector_0(); getCache(IntVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_IntVector_IntVector_1(size);
  getCache(IntVector)[this.ptr] = this;
};;
IntVector.prototype = Object.create(WrapperObject.prototype);
IntVector.prototype.constructor = IntVector;
IntVector.prototype.__class__ = IntVector;
IntVector.__cache__ = {};
Module['IntVector'] = IntVector;

IntVector.prototype['resize'] = IntVector.prototype.resize = /** @suppress {undefinedVars, duplicate} @this{Object} */function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_IntVector_resize_1(self, size);
};;

IntVector.prototype['get'] = IntVector.prototype.get = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return _emscripten_bind_IntVector_get_1(self, i);
};;

IntVector.prototype['set'] = IntVector.prototype.set = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_IntVector_set_2(self, i, val);
};;

IntVector.prototype['size'] = IntVector.prototype.size = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_IntVector_size_0(self);
};;

  IntVector.prototype['__destroy__'] = IntVector.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_IntVector___destroy___0(self);
};
// DoubleVector
/** @suppress {undefinedVars, duplicate} @this{Object} */function DoubleVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_DoubleVector_DoubleVector_0(); getCache(DoubleVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_DoubleVector_DoubleVector_1(size);
  getCache(DoubleVector)[this.ptr] = this;
};;
DoubleVector.prototype = Object.create(WrapperObject.prototype);
DoubleVector.prototype.constructor = DoubleVector;
DoubleVector.prototype.__class__ = DoubleVector;
DoubleVector.__cache__ = {};
Module['DoubleVector'] = DoubleVector;

DoubleVector.prototype['resize'] = DoubleVector.prototype.resize = /** @suppress {undefinedVars, duplicate} @this{Object} */function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_DoubleVector_resize_1(self, size);
};;

DoubleVector.prototype['get'] = DoubleVector.prototype.get = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return _emscripten_bind_DoubleVector_get_1(self, i);
};;

DoubleVector.prototype['set'] = DoubleVector.prototype.set = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_DoubleVector_set_2(self, i, val);
};;

DoubleVector.prototype['size'] = DoubleVector.prototype.size = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_DoubleVector_size_0(self);
};;

  DoubleVector.prototype['__destroy__'] = DoubleVector.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_DoubleVector___destroy___0(self);
};
// SpeciesMasterTableRecordVector
/** @suppress {undefinedVars, duplicate} @this{Object} */function SpeciesMasterTableRecordVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_0(); getCache(SpeciesMasterTableRecordVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_1(size);
  getCache(SpeciesMasterTableRecordVector)[this.ptr] = this;
};;
SpeciesMasterTableRecordVector.prototype = Object.create(WrapperObject.prototype);
SpeciesMasterTableRecordVector.prototype.constructor = SpeciesMasterTableRecordVector;
SpeciesMasterTableRecordVector.prototype.__class__ = SpeciesMasterTableRecordVector;
SpeciesMasterTableRecordVector.__cache__ = {};
Module['SpeciesMasterTableRecordVector'] = SpeciesMasterTableRecordVector;

SpeciesMasterTableRecordVector.prototype['resize'] = SpeciesMasterTableRecordVector.prototype.resize = /** @suppress {undefinedVars, duplicate} @this{Object} */function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_SpeciesMasterTableRecordVector_resize_1(self, size);
};;

SpeciesMasterTableRecordVector.prototype['get'] = SpeciesMasterTableRecordVector.prototype.get = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return wrapPointer(_emscripten_bind_SpeciesMasterTableRecordVector_get_1(self, i), SpeciesMasterTableRecord);
};;

SpeciesMasterTableRecordVector.prototype['set'] = SpeciesMasterTableRecordVector.prototype.set = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_SpeciesMasterTableRecordVector_set_2(self, i, val);
};;

SpeciesMasterTableRecordVector.prototype['size'] = SpeciesMasterTableRecordVector.prototype.size = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SpeciesMasterTableRecordVector_size_0(self);
};;

  SpeciesMasterTableRecordVector.prototype['__destroy__'] = SpeciesMasterTableRecordVector.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SpeciesMasterTableRecordVector___destroy___0(self);
};
// FireSize
/** @suppress {undefinedVars, duplicate} @this{Object} */function FireSize() {
  this.ptr = _emscripten_bind_FireSize_FireSize_0();
  getCache(FireSize)[this.ptr] = this;
};;
FireSize.prototype = Object.create(WrapperObject.prototype);
FireSize.prototype.constructor = FireSize;
FireSize.prototype.__class__ = FireSize;
FireSize.__cache__ = {};
Module['FireSize'] = FireSize;

FireSize.prototype['calculateFireBasicDimensions'] = FireSize.prototype.calculateFireBasicDimensions = /** @suppress {undefinedVars, duplicate} @this{Object} */function(effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits) {
  var self = this.ptr;
  if (effectiveWindSpeed && typeof effectiveWindSpeed === 'object') effectiveWindSpeed = effectiveWindSpeed.ptr;
  if (windSpeedRateUnits && typeof windSpeedRateUnits === 'object') windSpeedRateUnits = windSpeedRateUnits.ptr;
  if (forwardSpreadRate && typeof forwardSpreadRate === 'object') forwardSpreadRate = forwardSpreadRate.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  _emscripten_bind_FireSize_calculateFireBasicDimensions_4(self, effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits);
};;

FireSize.prototype['getFireLengthToWidthRatio'] = FireSize.prototype.getFireLengthToWidthRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_FireSize_getFireLengthToWidthRatio_0(self);
};;

FireSize.prototype['getEccentricity'] = FireSize.prototype.getEccentricity = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_FireSize_getEccentricity_0(self);
};;

FireSize.prototype['getBackingSpreadRate'] = FireSize.prototype.getBackingSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_FireSize_getBackingSpreadRate_1(self, spreadRateUnits);
};;

FireSize.prototype['getEllipticalA'] = FireSize.prototype.getEllipticalA = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getEllipticalA_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getEllipticalB'] = FireSize.prototype.getEllipticalB = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getEllipticalB_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getEllipticalC'] = FireSize.prototype.getEllipticalC = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getEllipticalC_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getFireLength'] = FireSize.prototype.getFireLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFireLength_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getMaxFireWidth'] = FireSize.prototype.getMaxFireWidth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getMaxFireWidth_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getFirePerimeter'] = FireSize.prototype.getFirePerimeter = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFirePerimeter_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getFireArea'] = FireSize.prototype.getFireArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFireArea_3(self, areaUnits, elapsedTime, timeUnits);
};;

  FireSize.prototype['__destroy__'] = FireSize.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_FireSize___destroy___0(self);
};
// SIGContainResource
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGContainResource(arrival, production, duration, flank, desc, baseCost, hourCost) {
  ensureCache.prepare();
  if (arrival && typeof arrival === 'object') arrival = arrival.ptr;
  if (production && typeof production === 'object') production = production.ptr;
  if (duration && typeof duration === 'object') duration = duration.ptr;
  if (flank && typeof flank === 'object') flank = flank.ptr;
  if (desc && typeof desc === 'object') desc = desc.ptr;
  else desc = ensureString(desc);
  if (baseCost && typeof baseCost === 'object') baseCost = baseCost.ptr;
  if (hourCost && typeof hourCost === 'object') hourCost = hourCost.ptr;
  if (arrival === undefined) { this.ptr = _emscripten_bind_SIGContainResource_SIGContainResource_0(); getCache(SIGContainResource)[this.ptr] = this;return }
  if (production === undefined) { this.ptr = _emscripten_bind_SIGContainResource_SIGContainResource_1(arrival); getCache(SIGContainResource)[this.ptr] = this;return }
  if (duration === undefined) { this.ptr = _emscripten_bind_SIGContainResource_SIGContainResource_2(arrival, production); getCache(SIGContainResource)[this.ptr] = this;return }
  if (flank === undefined) { this.ptr = _emscripten_bind_SIGContainResource_SIGContainResource_3(arrival, production, duration); getCache(SIGContainResource)[this.ptr] = this;return }
  if (desc === undefined) { this.ptr = _emscripten_bind_SIGContainResource_SIGContainResource_4(arrival, production, duration, flank); getCache(SIGContainResource)[this.ptr] = this;return }
  if (baseCost === undefined) { this.ptr = _emscripten_bind_SIGContainResource_SIGContainResource_5(arrival, production, duration, flank, desc); getCache(SIGContainResource)[this.ptr] = this;return }
  if (hourCost === undefined) { this.ptr = _emscripten_bind_SIGContainResource_SIGContainResource_6(arrival, production, duration, flank, desc, baseCost); getCache(SIGContainResource)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_SIGContainResource_SIGContainResource_7(arrival, production, duration, flank, desc, baseCost, hourCost);
  getCache(SIGContainResource)[this.ptr] = this;
};;
SIGContainResource.prototype = Object.create(WrapperObject.prototype);
SIGContainResource.prototype.constructor = SIGContainResource;
SIGContainResource.prototype.__class__ = SIGContainResource;
SIGContainResource.__cache__ = {};
Module['SIGContainResource'] = SIGContainResource;

SIGContainResource.prototype['print'] = SIGContainResource.prototype.print = /** @suppress {undefinedVars, duplicate} @this{Object} */function(buf, buflen) {
  var self = this.ptr;
  ensureCache.prepare();
  if (buf && typeof buf === 'object') buf = buf.ptr;
  else buf = ensureString(buf);
  if (buflen && typeof buflen === 'object') buflen = buflen.ptr;
  _emscripten_bind_SIGContainResource_print_2(self, buf, buflen);
};;

SIGContainResource.prototype['arrival'] = SIGContainResource.prototype.arrival = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainResource_arrival_0(self);
};;

SIGContainResource.prototype['hourCost'] = SIGContainResource.prototype.hourCost = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainResource_hourCost_0(self);
};;

SIGContainResource.prototype['duration'] = SIGContainResource.prototype.duration = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainResource_duration_0(self);
};;

SIGContainResource.prototype['production'] = SIGContainResource.prototype.production = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainResource_production_0(self);
};;

SIGContainResource.prototype['baseCost'] = SIGContainResource.prototype.baseCost = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainResource_baseCost_0(self);
};;

SIGContainResource.prototype['description'] = SIGContainResource.prototype.description = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return UTF8ToString(_emscripten_bind_SIGContainResource_description_0(self));
};;

SIGContainResource.prototype['flank'] = SIGContainResource.prototype.flank = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainResource_flank_0(self);
};;

  SIGContainResource.prototype['__destroy__'] = SIGContainResource.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainResource___destroy___0(self);
};
// SIGContainForce
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGContainForce() {
  this.ptr = _emscripten_bind_SIGContainForce_SIGContainForce_0();
  getCache(SIGContainForce)[this.ptr] = this;
};;
SIGContainForce.prototype = Object.create(WrapperObject.prototype);
SIGContainForce.prototype.constructor = SIGContainForce;
SIGContainForce.prototype.__class__ = SIGContainForce;
SIGContainForce.__cache__ = {};
Module['SIGContainForce'] = SIGContainForce;

SIGContainForce.prototype['addResource'] = SIGContainForce.prototype.addResource = /** @suppress {undefinedVars, duplicate} @this{Object} */function(arrival, production, duration, flank, desc, baseCost, hourCost) {
  var self = this.ptr;
  ensureCache.prepare();
  if (arrival && typeof arrival === 'object') arrival = arrival.ptr;
  if (production && typeof production === 'object') production = production.ptr;
  if (duration && typeof duration === 'object') duration = duration.ptr;
  if (flank && typeof flank === 'object') flank = flank.ptr;
  if (desc && typeof desc === 'object') desc = desc.ptr;
  else desc = ensureString(desc);
  if (baseCost && typeof baseCost === 'object') baseCost = baseCost.ptr;
  if (hourCost && typeof hourCost === 'object') hourCost = hourCost.ptr;
  if (production === undefined) { return wrapPointer(_emscripten_bind_SIGContainForce_addResource_1(self, arrival), SIGContainResource) }
  if (duration === undefined) { return wrapPointer(_emscripten_bind_SIGContainForce_addResource_2(self, arrival, production), SIGContainResource) }
  if (flank === undefined) { return wrapPointer(_emscripten_bind_SIGContainForce_addResource_3(self, arrival, production, duration), SIGContainResource) }
  if (desc === undefined) { return wrapPointer(_emscripten_bind_SIGContainForce_addResource_4(self, arrival, production, duration, flank), SIGContainResource) }
  if (baseCost === undefined) { return wrapPointer(_emscripten_bind_SIGContainForce_addResource_5(self, arrival, production, duration, flank, desc), SIGContainResource) }
  if (hourCost === undefined) { return wrapPointer(_emscripten_bind_SIGContainForce_addResource_6(self, arrival, production, duration, flank, desc, baseCost), SIGContainResource) }
  return wrapPointer(_emscripten_bind_SIGContainForce_addResource_7(self, arrival, production, duration, flank, desc, baseCost, hourCost), SIGContainResource);
};;

SIGContainForce.prototype['exhausted'] = SIGContainForce.prototype.exhausted = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flank) {
  var self = this.ptr;
  if (flank && typeof flank === 'object') flank = flank.ptr;
  return _emscripten_bind_SIGContainForce_exhausted_1(self, flank);
};;

SIGContainForce.prototype['firstArrival'] = SIGContainForce.prototype.firstArrival = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flank) {
  var self = this.ptr;
  if (flank && typeof flank === 'object') flank = flank.ptr;
  return _emscripten_bind_SIGContainForce_firstArrival_1(self, flank);
};;

SIGContainForce.prototype['nextArrival'] = SIGContainForce.prototype.nextArrival = /** @suppress {undefinedVars, duplicate} @this{Object} */function(after, until, flank) {
  var self = this.ptr;
  if (after && typeof after === 'object') after = after.ptr;
  if (until && typeof until === 'object') until = until.ptr;
  if (flank && typeof flank === 'object') flank = flank.ptr;
  return _emscripten_bind_SIGContainForce_nextArrival_3(self, after, until, flank);
};;

SIGContainForce.prototype['productionRate'] = SIGContainForce.prototype.productionRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(minutesSinceReport, flank) {
  var self = this.ptr;
  if (minutesSinceReport && typeof minutesSinceReport === 'object') minutesSinceReport = minutesSinceReport.ptr;
  if (flank && typeof flank === 'object') flank = flank.ptr;
  return _emscripten_bind_SIGContainForce_productionRate_2(self, minutesSinceReport, flank);
};;

SIGContainForce.prototype['resources'] = SIGContainForce.prototype.resources = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainForce_resources_0(self);
};;

SIGContainForce.prototype['resourceArrival'] = SIGContainForce.prototype.resourceArrival = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContainForce_resourceArrival_1(self, index);
};;

SIGContainForce.prototype['resourceBaseCost'] = SIGContainForce.prototype.resourceBaseCost = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContainForce_resourceBaseCost_1(self, index);
};;

SIGContainForce.prototype['resourceCost'] = SIGContainForce.prototype.resourceCost = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, finalTime) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (finalTime && typeof finalTime === 'object') finalTime = finalTime.ptr;
  return _emscripten_bind_SIGContainForce_resourceCost_2(self, index, finalTime);
};;

SIGContainForce.prototype['resourceDescription'] = SIGContainForce.prototype.resourceDescription = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGContainForce_resourceDescription_1(self, index));
};;

SIGContainForce.prototype['resourceDuration'] = SIGContainForce.prototype.resourceDuration = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContainForce_resourceDuration_1(self, index);
};;

SIGContainForce.prototype['resourceFlank'] = SIGContainForce.prototype.resourceFlank = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContainForce_resourceFlank_1(self, index);
};;

SIGContainForce.prototype['resourceHourCost'] = SIGContainForce.prototype.resourceHourCost = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContainForce_resourceHourCost_1(self, index);
};;

SIGContainForce.prototype['resourceProduction'] = SIGContainForce.prototype.resourceProduction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContainForce_resourceProduction_1(self, index);
};;

  SIGContainForce.prototype['__destroy__'] = SIGContainForce.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainForce___destroy___0(self);
};
// SIGContainForceAdapter
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGContainForceAdapter() {
  this.ptr = _emscripten_bind_SIGContainForceAdapter_SIGContainForceAdapter_0();
  getCache(SIGContainForceAdapter)[this.ptr] = this;
};;
SIGContainForceAdapter.prototype = Object.create(WrapperObject.prototype);
SIGContainForceAdapter.prototype.constructor = SIGContainForceAdapter;
SIGContainForceAdapter.prototype.__class__ = SIGContainForceAdapter;
SIGContainForceAdapter.__cache__ = {};
Module['SIGContainForceAdapter'] = SIGContainForceAdapter;

SIGContainForceAdapter.prototype['addResource'] = SIGContainForceAdapter.prototype.addResource = /** @suppress {undefinedVars, duplicate} @this{Object} */function(arrival, production, duration, flank, desc, baseCost, hourCost) {
  var self = this.ptr;
  ensureCache.prepare();
  if (arrival && typeof arrival === 'object') arrival = arrival.ptr;
  if (production && typeof production === 'object') production = production.ptr;
  if (duration && typeof duration === 'object') duration = duration.ptr;
  if (flank && typeof flank === 'object') flank = flank.ptr;
  if (desc && typeof desc === 'object') desc = desc.ptr;
  else desc = ensureString(desc);
  if (baseCost && typeof baseCost === 'object') baseCost = baseCost.ptr;
  if (hourCost && typeof hourCost === 'object') hourCost = hourCost.ptr;
  _emscripten_bind_SIGContainForceAdapter_addResource_7(self, arrival, production, duration, flank, desc, baseCost, hourCost);
};;

SIGContainForceAdapter.prototype['firstArrival'] = SIGContainForceAdapter.prototype.firstArrival = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flank) {
  var self = this.ptr;
  if (flank && typeof flank === 'object') flank = flank.ptr;
  return _emscripten_bind_SIGContainForceAdapter_firstArrival_1(self, flank);
};;

SIGContainForceAdapter.prototype['removeResourceAt'] = SIGContainForceAdapter.prototype.removeResourceAt = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContainForceAdapter_removeResourceAt_1(self, index);
};;

SIGContainForceAdapter.prototype['removeResourceWithThisDesc'] = SIGContainForceAdapter.prototype.removeResourceWithThisDesc = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desc) {
  var self = this.ptr;
  ensureCache.prepare();
  if (desc && typeof desc === 'object') desc = desc.ptr;
  else desc = ensureString(desc);
  return _emscripten_bind_SIGContainForceAdapter_removeResourceWithThisDesc_1(self, desc);
};;

SIGContainForceAdapter.prototype['removeAllResourcesWithThisDesc'] = SIGContainForceAdapter.prototype.removeAllResourcesWithThisDesc = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desc) {
  var self = this.ptr;
  ensureCache.prepare();
  if (desc && typeof desc === 'object') desc = desc.ptr;
  else desc = ensureString(desc);
  return _emscripten_bind_SIGContainForceAdapter_removeAllResourcesWithThisDesc_1(self, desc);
};;

  SIGContainForceAdapter.prototype['__destroy__'] = SIGContainForceAdapter.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainForceAdapter___destroy___0(self);
};
// SIGContainSim
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGContainSim(reportSize, reportRate, diurnalROS, fireStartMinutesStartTime, lwRatio, force, tactic, attackDist, retry, minSteps, maxSteps, maxFireSize, maxFireTime) {
  if (reportSize && typeof reportSize === 'object') reportSize = reportSize.ptr;
  if (reportRate && typeof reportRate === 'object') reportRate = reportRate.ptr;
  if (diurnalROS && typeof diurnalROS === 'object') diurnalROS = diurnalROS.ptr;
  if (fireStartMinutesStartTime && typeof fireStartMinutesStartTime === 'object') fireStartMinutesStartTime = fireStartMinutesStartTime.ptr;
  if (lwRatio && typeof lwRatio === 'object') lwRatio = lwRatio.ptr;
  if (force && typeof force === 'object') force = force.ptr;
  if (tactic && typeof tactic === 'object') tactic = tactic.ptr;
  if (attackDist && typeof attackDist === 'object') attackDist = attackDist.ptr;
  if (retry && typeof retry === 'object') retry = retry.ptr;
  if (minSteps && typeof minSteps === 'object') minSteps = minSteps.ptr;
  if (maxSteps && typeof maxSteps === 'object') maxSteps = maxSteps.ptr;
  if (maxFireSize && typeof maxFireSize === 'object') maxFireSize = maxFireSize.ptr;
  if (maxFireTime && typeof maxFireTime === 'object') maxFireTime = maxFireTime.ptr;
  this.ptr = _emscripten_bind_SIGContainSim_SIGContainSim_13(reportSize, reportRate, diurnalROS, fireStartMinutesStartTime, lwRatio, force, tactic, attackDist, retry, minSteps, maxSteps, maxFireSize, maxFireTime);
  getCache(SIGContainSim)[this.ptr] = this;
};;
SIGContainSim.prototype = Object.create(WrapperObject.prototype);
SIGContainSim.prototype.constructor = SIGContainSim;
SIGContainSim.prototype.__class__ = SIGContainSim;
SIGContainSim.__cache__ = {};
Module['SIGContainSim'] = SIGContainSim;

SIGContainSim.prototype['attackDistance'] = SIGContainSim.prototype.attackDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_attackDistance_0(self);
};;

SIGContainSim.prototype['attackPointX'] = SIGContainSim.prototype.attackPointX = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_attackPointX_0(self);
};;

SIGContainSim.prototype['attackPointY'] = SIGContainSim.prototype.attackPointY = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_attackPointY_0(self);
};;

SIGContainSim.prototype['attackTime'] = SIGContainSim.prototype.attackTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_attackTime_0(self);
};;

SIGContainSim.prototype['distanceStep'] = SIGContainSim.prototype.distanceStep = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_distanceStep_0(self);
};;

SIGContainSim.prototype['fireBackAtAttack'] = SIGContainSim.prototype.fireBackAtAttack = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_fireBackAtAttack_0(self);
};;

SIGContainSim.prototype['fireBackAtReport'] = SIGContainSim.prototype.fireBackAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_fireBackAtReport_0(self);
};;

SIGContainSim.prototype['fireHeadAtAttack'] = SIGContainSim.prototype.fireHeadAtAttack = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_fireHeadAtAttack_0(self);
};;

SIGContainSim.prototype['fireHeadAtReport'] = SIGContainSim.prototype.fireHeadAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_fireHeadAtReport_0(self);
};;

SIGContainSim.prototype['fireLwRatioAtReport'] = SIGContainSim.prototype.fireLwRatioAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_fireLwRatioAtReport_0(self);
};;

SIGContainSim.prototype['fireReportTime'] = SIGContainSim.prototype.fireReportTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_fireReportTime_0(self);
};;

SIGContainSim.prototype['fireSizeAtReport'] = SIGContainSim.prototype.fireSizeAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_fireSizeAtReport_0(self);
};;

SIGContainSim.prototype['fireSpreadRateAtBack'] = SIGContainSim.prototype.fireSpreadRateAtBack = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_fireSpreadRateAtBack_0(self);
};;

SIGContainSim.prototype['fireSpreadRateAtReport'] = SIGContainSim.prototype.fireSpreadRateAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_fireSpreadRateAtReport_0(self);
};;

SIGContainSim.prototype['force'] = SIGContainSim.prototype.force = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGContainSim_force_0(self), SIGContainForce);
};;

SIGContainSim.prototype['maximumSimulationSteps'] = SIGContainSim.prototype.maximumSimulationSteps = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_maximumSimulationSteps_0(self);
};;

SIGContainSim.prototype['minimumSimulationSteps'] = SIGContainSim.prototype.minimumSimulationSteps = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_minimumSimulationSteps_0(self);
};;

SIGContainSim.prototype['status'] = SIGContainSim.prototype.status = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_status_0(self);
};;

SIGContainSim.prototype['tactic'] = SIGContainSim.prototype.tactic = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_tactic_0(self);
};;

SIGContainSim.prototype['finalFireCost'] = SIGContainSim.prototype.finalFireCost = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_finalFireCost_0(self);
};;

SIGContainSim.prototype['finalFireLine'] = SIGContainSim.prototype.finalFireLine = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_finalFireLine_0(self);
};;

SIGContainSim.prototype['finalFirePerimeter'] = SIGContainSim.prototype.finalFirePerimeter = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_finalFirePerimeter_0(self);
};;

SIGContainSim.prototype['finalFireSize'] = SIGContainSim.prototype.finalFireSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_finalFireSize_0(self);
};;

SIGContainSim.prototype['finalFireSweep'] = SIGContainSim.prototype.finalFireSweep = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_finalFireSweep_0(self);
};;

SIGContainSim.prototype['finalFireTime'] = SIGContainSim.prototype.finalFireTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_finalFireTime_0(self);
};;

SIGContainSim.prototype['finalResourcesUsed'] = SIGContainSim.prototype.finalResourcesUsed = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_finalResourcesUsed_0(self);
};;

SIGContainSim.prototype['fireHeadX'] = SIGContainSim.prototype.fireHeadX = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGContainSim_fireHeadX_0(self), DoublePtr);
};;

SIGContainSim.prototype['firePerimeterY'] = SIGContainSim.prototype.firePerimeterY = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGContainSim_firePerimeterY_0(self), DoublePtr);
};;

SIGContainSim.prototype['firePerimeterX'] = SIGContainSim.prototype.firePerimeterX = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGContainSim_firePerimeterX_0(self), DoublePtr);
};;

SIGContainSim.prototype['firePoints'] = SIGContainSim.prototype.firePoints = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainSim_firePoints_0(self);
};;

SIGContainSim.prototype['run'] = SIGContainSim.prototype.run = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainSim_run_0(self);
};;

SIGContainSim.prototype['UncontainedArea'] = SIGContainSim.prototype.UncontainedArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(head, lwRatio, x, y, tactic) {
  var self = this.ptr;
  if (head && typeof head === 'object') head = head.ptr;
  if (lwRatio && typeof lwRatio === 'object') lwRatio = lwRatio.ptr;
  if (x && typeof x === 'object') x = x.ptr;
  if (y && typeof y === 'object') y = y.ptr;
  if (tactic && typeof tactic === 'object') tactic = tactic.ptr;
  return _emscripten_bind_SIGContainSim_UncontainedArea_5(self, head, lwRatio, x, y, tactic);
};;

  SIGContainSim.prototype['__destroy__'] = SIGContainSim.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainSim___destroy___0(self);
};
// SIGDiurnalROS
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGDiurnalROS() {
  this.ptr = _emscripten_bind_SIGDiurnalROS_SIGDiurnalROS_0();
  getCache(SIGDiurnalROS)[this.ptr] = this;
};;
SIGDiurnalROS.prototype = Object.create(WrapperObject.prototype);
SIGDiurnalROS.prototype.constructor = SIGDiurnalROS;
SIGDiurnalROS.prototype.__class__ = SIGDiurnalROS;
SIGDiurnalROS.__cache__ = {};
Module['SIGDiurnalROS'] = SIGDiurnalROS;

SIGDiurnalROS.prototype['push'] = SIGDiurnalROS.prototype.push = /** @suppress {undefinedVars, duplicate} @this{Object} */function(v) {
  var self = this.ptr;
  if (v && typeof v === 'object') v = v.ptr;
  _emscripten_bind_SIGDiurnalROS_push_1(self, v);
};;

SIGDiurnalROS.prototype['at'] = SIGDiurnalROS.prototype.at = /** @suppress {undefinedVars, duplicate} @this{Object} */function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return _emscripten_bind_SIGDiurnalROS_at_1(self, i);
};;

SIGDiurnalROS.prototype['size'] = SIGDiurnalROS.prototype.size = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGDiurnalROS_size_0(self);
};;

  SIGDiurnalROS.prototype['__destroy__'] = SIGDiurnalROS.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGDiurnalROS___destroy___0(self);
};
// SIGContain
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGContain(reportSize, reportRate, diurnalROS, fireStartMinutesStartTime, lwRatio, distStep, flank, force, attackTime, tactic, attackDist) {
  if (reportSize && typeof reportSize === 'object') reportSize = reportSize.ptr;
  if (reportRate && typeof reportRate === 'object') reportRate = reportRate.ptr;
  if (diurnalROS && typeof diurnalROS === 'object') diurnalROS = diurnalROS.ptr;
  if (fireStartMinutesStartTime && typeof fireStartMinutesStartTime === 'object') fireStartMinutesStartTime = fireStartMinutesStartTime.ptr;
  if (lwRatio && typeof lwRatio === 'object') lwRatio = lwRatio.ptr;
  if (distStep && typeof distStep === 'object') distStep = distStep.ptr;
  if (flank && typeof flank === 'object') flank = flank.ptr;
  if (force && typeof force === 'object') force = force.ptr;
  if (attackTime && typeof attackTime === 'object') attackTime = attackTime.ptr;
  if (tactic && typeof tactic === 'object') tactic = tactic.ptr;
  if (attackDist && typeof attackDist === 'object') attackDist = attackDist.ptr;
  this.ptr = _emscripten_bind_SIGContain_SIGContain_11(reportSize, reportRate, diurnalROS, fireStartMinutesStartTime, lwRatio, distStep, flank, force, attackTime, tactic, attackDist);
  getCache(SIGContain)[this.ptr] = this;
};;
SIGContain.prototype = Object.create(WrapperObject.prototype);
SIGContain.prototype.constructor = SIGContain;
SIGContain.prototype.__class__ = SIGContain;
SIGContain.__cache__ = {};
Module['SIGContain'] = SIGContain;

SIGContain.prototype['simulationTime'] = SIGContain.prototype.simulationTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_simulationTime_0(self);
};;

SIGContain.prototype['fireSpreadRateAtBack'] = SIGContain.prototype.fireSpreadRateAtBack = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_fireSpreadRateAtBack_0(self);
};;

SIGContain.prototype['fireLwRatioAtReport'] = SIGContain.prototype.fireLwRatioAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_fireLwRatioAtReport_0(self);
};;

SIGContain.prototype['force'] = SIGContain.prototype.force = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGContain_force_0(self), SIGContainForce);
};;

SIGContain.prototype['resourceHourCost'] = SIGContain.prototype.resourceHourCost = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContain_resourceHourCost_1(self, index);
};;

SIGContain.prototype['attackDistance'] = SIGContain.prototype.attackDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_attackDistance_0(self);
};;

SIGContain.prototype['attackPointX'] = SIGContain.prototype.attackPointX = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_attackPointX_0(self);
};;

SIGContain.prototype['fireHeadAtAttack'] = SIGContain.prototype.fireHeadAtAttack = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_fireHeadAtAttack_0(self);
};;

SIGContain.prototype['attackPointY'] = SIGContain.prototype.attackPointY = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_attackPointY_0(self);
};;

SIGContain.prototype['attackTime'] = SIGContain.prototype.attackTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_attackTime_0(self);
};;

SIGContain.prototype['resourceBaseCost'] = SIGContain.prototype.resourceBaseCost = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContain_resourceBaseCost_1(self, index);
};;

SIGContain.prototype['fireSpreadRateAtReport'] = SIGContain.prototype.fireSpreadRateAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_fireSpreadRateAtReport_0(self);
};;

SIGContain.prototype['fireHeadAtReport'] = SIGContain.prototype.fireHeadAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_fireHeadAtReport_0(self);
};;

SIGContain.prototype['fireReportTime'] = SIGContain.prototype.fireReportTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_fireReportTime_0(self);
};;

SIGContain.prototype['resourceProduction'] = SIGContain.prototype.resourceProduction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContain_resourceProduction_1(self, index);
};;

SIGContain.prototype['fireBackAtAttack'] = SIGContain.prototype.fireBackAtAttack = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_fireBackAtAttack_0(self);
};;

SIGContain.prototype['simulationStep'] = SIGContain.prototype.simulationStep = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_simulationStep_0(self);
};;

SIGContain.prototype['tactic'] = SIGContain.prototype.tactic = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_tactic_0(self);
};;

SIGContain.prototype['resourceDescription'] = SIGContain.prototype.resourceDescription = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGContain_resourceDescription_1(self, index));
};;

SIGContain.prototype['distanceStep'] = SIGContain.prototype.distanceStep = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_distanceStep_0(self);
};;

SIGContain.prototype['status'] = SIGContain.prototype.status = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_status_0(self);
};;

SIGContain.prototype['resourceArrival'] = SIGContain.prototype.resourceArrival = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContain_resourceArrival_1(self, index);
};;

SIGContain.prototype['fireSizeAtReport'] = SIGContain.prototype.fireSizeAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_fireSizeAtReport_0(self);
};;

SIGContain.prototype['setFireStartTimeMinutes'] = SIGContain.prototype.setFireStartTimeMinutes = /** @suppress {undefinedVars, duplicate} @this{Object} */function(starttime) {
  var self = this.ptr;
  if (starttime && typeof starttime === 'object') starttime = starttime.ptr;
  return _emscripten_bind_SIGContain_setFireStartTimeMinutes_1(self, starttime);
};;

SIGContain.prototype['fireBackAtReport'] = SIGContain.prototype.fireBackAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_fireBackAtReport_0(self);
};;

SIGContain.prototype['resourceDuration'] = SIGContain.prototype.resourceDuration = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContain_resourceDuration_1(self, index);
};;

SIGContain.prototype['resources'] = SIGContain.prototype.resources = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_resources_0(self);
};;

SIGContain.prototype['exhaustedTime'] = SIGContain.prototype.exhaustedTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContain_exhaustedTime_0(self);
};;

  SIGContain.prototype['__destroy__'] = SIGContain.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGContain___destroy___0(self);
};
// SIGContainAdapter
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGContainAdapter() {
  this.ptr = _emscripten_bind_SIGContainAdapter_SIGContainAdapter_0();
  getCache(SIGContainAdapter)[this.ptr] = this;
};;
SIGContainAdapter.prototype = Object.create(WrapperObject.prototype);
SIGContainAdapter.prototype.constructor = SIGContainAdapter;
SIGContainAdapter.prototype.__class__ = SIGContainAdapter;
SIGContainAdapter.__cache__ = {};
Module['SIGContainAdapter'] = SIGContainAdapter;

SIGContainAdapter.prototype['getContainmentStatus'] = SIGContainAdapter.prototype.getContainmentStatus = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getContainmentStatus_0(self);
};;

SIGContainAdapter.prototype['getFinalContainmentArea'] = SIGContainAdapter.prototype.getFinalContainmentArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalContainmentArea_1(self, areaUnits);
};;

SIGContainAdapter.prototype['getFinalCost'] = SIGContainAdapter.prototype.getFinalCost = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalCost_0(self);
};;

SIGContainAdapter.prototype['getFinalFireLineLength'] = SIGContainAdapter.prototype.getFinalFireLineLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalFireLineLength_1(self, lengthUnits);
};;

SIGContainAdapter.prototype['getFinalFireSize'] = SIGContainAdapter.prototype.getFinalFireSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalFireSize_1(self, areaUnits);
};;

SIGContainAdapter.prototype['getFinalTimeSinceReport'] = SIGContainAdapter.prototype.getFinalTimeSinceReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function(timeUnits) {
  var self = this.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalTimeSinceReport_1(self, timeUnits);
};;

SIGContainAdapter.prototype['getFireSizeAtInitialAttack'] = SIGContainAdapter.prototype.getFireSizeAtInitialAttack = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireSizeAtInitialAttack_1(self, areaUnits);
};;

SIGContainAdapter.prototype['getPerimeterAtContainment'] = SIGContainAdapter.prototype.getPerimeterAtContainment = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getPerimeterAtContainment_1(self, lengthUnits);
};;

SIGContainAdapter.prototype['getPerimeterAtInitialAttack'] = SIGContainAdapter.prototype.getPerimeterAtInitialAttack = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getPerimeterAtInitialAttack_1(self, lengthUnits);
};;

SIGContainAdapter.prototype['removeAllResourcesWithThisDesc'] = SIGContainAdapter.prototype.removeAllResourcesWithThisDesc = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desc) {
  var self = this.ptr;
  ensureCache.prepare();
  if (desc && typeof desc === 'object') desc = desc.ptr;
  else desc = ensureString(desc);
  return _emscripten_bind_SIGContainAdapter_removeAllResourcesWithThisDesc_1(self, desc);
};;

SIGContainAdapter.prototype['removeResourceAt'] = SIGContainAdapter.prototype.removeResourceAt = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContainAdapter_removeResourceAt_1(self, index);
};;

SIGContainAdapter.prototype['removeResourceWithThisDesc'] = SIGContainAdapter.prototype.removeResourceWithThisDesc = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desc) {
  var self = this.ptr;
  ensureCache.prepare();
  if (desc && typeof desc === 'object') desc = desc.ptr;
  else desc = ensureString(desc);
  return _emscripten_bind_SIGContainAdapter_removeResourceWithThisDesc_1(self, desc);
};;

SIGContainAdapter.prototype['addResource'] = SIGContainAdapter.prototype.addResource = /** @suppress {undefinedVars, duplicate} @this{Object} */function(arrival, duration, timeUnit, productionRate, productionRateUnits, description, baseCost, hourCost) {
  var self = this.ptr;
  ensureCache.prepare();
  if (arrival && typeof arrival === 'object') arrival = arrival.ptr;
  if (duration && typeof duration === 'object') duration = duration.ptr;
  if (timeUnit && typeof timeUnit === 'object') timeUnit = timeUnit.ptr;
  if (productionRate && typeof productionRate === 'object') productionRate = productionRate.ptr;
  if (productionRateUnits && typeof productionRateUnits === 'object') productionRateUnits = productionRateUnits.ptr;
  if (description && typeof description === 'object') description = description.ptr;
  else description = ensureString(description);
  if (baseCost && typeof baseCost === 'object') baseCost = baseCost.ptr;
  if (hourCost && typeof hourCost === 'object') hourCost = hourCost.ptr;
  _emscripten_bind_SIGContainAdapter_addResource_8(self, arrival, duration, timeUnit, productionRate, productionRateUnits, description, baseCost, hourCost);
};;

SIGContainAdapter.prototype['doContainRun'] = SIGContainAdapter.prototype.doContainRun = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainAdapter_doContainRun_0(self);
};;

SIGContainAdapter.prototype['removeAllResources'] = SIGContainAdapter.prototype.removeAllResources = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainAdapter_removeAllResources_0(self);
};;

SIGContainAdapter.prototype['setAttackDistance'] = SIGContainAdapter.prototype.setAttackDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(attackDistance, lengthUnits) {
  var self = this.ptr;
  if (attackDistance && typeof attackDistance === 'object') attackDistance = attackDistance.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGContainAdapter_setAttackDistance_2(self, attackDistance, lengthUnits);
};;

SIGContainAdapter.prototype['setFireStartTime'] = SIGContainAdapter.prototype.setFireStartTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fireStartTime) {
  var self = this.ptr;
  if (fireStartTime && typeof fireStartTime === 'object') fireStartTime = fireStartTime.ptr;
  _emscripten_bind_SIGContainAdapter_setFireStartTime_1(self, fireStartTime);
};;

SIGContainAdapter.prototype['setLwRatio'] = SIGContainAdapter.prototype.setLwRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lwRatio) {
  var self = this.ptr;
  if (lwRatio && typeof lwRatio === 'object') lwRatio = lwRatio.ptr;
  _emscripten_bind_SIGContainAdapter_setLwRatio_1(self, lwRatio);
};;

SIGContainAdapter.prototype['setMaxFireSize'] = SIGContainAdapter.prototype.setMaxFireSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function(maxFireSize) {
  var self = this.ptr;
  if (maxFireSize && typeof maxFireSize === 'object') maxFireSize = maxFireSize.ptr;
  _emscripten_bind_SIGContainAdapter_setMaxFireSize_1(self, maxFireSize);
};;

SIGContainAdapter.prototype['setMaxFireTime'] = SIGContainAdapter.prototype.setMaxFireTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function(maxFireTime) {
  var self = this.ptr;
  if (maxFireTime && typeof maxFireTime === 'object') maxFireTime = maxFireTime.ptr;
  _emscripten_bind_SIGContainAdapter_setMaxFireTime_1(self, maxFireTime);
};;

SIGContainAdapter.prototype['setMaxSteps'] = SIGContainAdapter.prototype.setMaxSteps = /** @suppress {undefinedVars, duplicate} @this{Object} */function(maxSteps) {
  var self = this.ptr;
  if (maxSteps && typeof maxSteps === 'object') maxSteps = maxSteps.ptr;
  _emscripten_bind_SIGContainAdapter_setMaxSteps_1(self, maxSteps);
};;

SIGContainAdapter.prototype['setMinSteps'] = SIGContainAdapter.prototype.setMinSteps = /** @suppress {undefinedVars, duplicate} @this{Object} */function(minSteps) {
  var self = this.ptr;
  if (minSteps && typeof minSteps === 'object') minSteps = minSteps.ptr;
  _emscripten_bind_SIGContainAdapter_setMinSteps_1(self, minSteps);
};;

SIGContainAdapter.prototype['setReportRate'] = SIGContainAdapter.prototype.setReportRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(reportRate, speedUnits) {
  var self = this.ptr;
  if (reportRate && typeof reportRate === 'object') reportRate = reportRate.ptr;
  if (speedUnits && typeof speedUnits === 'object') speedUnits = speedUnits.ptr;
  _emscripten_bind_SIGContainAdapter_setReportRate_2(self, reportRate, speedUnits);
};;

SIGContainAdapter.prototype['setReportSize'] = SIGContainAdapter.prototype.setReportSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function(reportSize, areaUnits) {
  var self = this.ptr;
  if (reportSize && typeof reportSize === 'object') reportSize = reportSize.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  _emscripten_bind_SIGContainAdapter_setReportSize_2(self, reportSize, areaUnits);
};;

SIGContainAdapter.prototype['setRetry'] = SIGContainAdapter.prototype.setRetry = /** @suppress {undefinedVars, duplicate} @this{Object} */function(retry) {
  var self = this.ptr;
  if (retry && typeof retry === 'object') retry = retry.ptr;
  _emscripten_bind_SIGContainAdapter_setRetry_1(self, retry);
};;

SIGContainAdapter.prototype['setTactic'] = SIGContainAdapter.prototype.setTactic = /** @suppress {undefinedVars, duplicate} @this{Object} */function(tactic) {
  var self = this.ptr;
  if (tactic && typeof tactic === 'object') tactic = tactic.ptr;
  _emscripten_bind_SIGContainAdapter_setTactic_1(self, tactic);
};;

  SIGContainAdapter.prototype['__destroy__'] = SIGContainAdapter.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainAdapter___destroy___0(self);
};
// SIGIgniteInputs
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGIgniteInputs() {
  this.ptr = _emscripten_bind_SIGIgniteInputs_SIGIgniteInputs_0();
  getCache(SIGIgniteInputs)[this.ptr] = this;
};;
SIGIgniteInputs.prototype = Object.create(WrapperObject.prototype);
SIGIgniteInputs.prototype.constructor = SIGIgniteInputs;
SIGIgniteInputs.prototype.__class__ = SIGIgniteInputs;
SIGIgniteInputs.__cache__ = {};
Module['SIGIgniteInputs'] = SIGIgniteInputs;

SIGIgniteInputs.prototype['initializeMembers'] = SIGIgniteInputs.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGIgniteInputs_initializeMembers_0(self);
};;

SIGIgniteInputs.prototype['setAirTemperature'] = SIGIgniteInputs.prototype.setAirTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(airTemperature, temperatureUnits) {
  var self = this.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  _emscripten_bind_SIGIgniteInputs_setAirTemperature_2(self, airTemperature, temperatureUnits);
};;

SIGIgniteInputs.prototype['setDuffDepth'] = SIGIgniteInputs.prototype.setDuffDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(duffDepth, lengthUnits) {
  var self = this.ptr;
  if (duffDepth && typeof duffDepth === 'object') duffDepth = duffDepth.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGIgniteInputs_setDuffDepth_2(self, duffDepth, lengthUnits);
};;

SIGIgniteInputs.prototype['setIgnitionFuelBedType'] = SIGIgniteInputs.prototype.setIgnitionFuelBedType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelBedType) {
  var self = this.ptr;
  if (fuelBedType && typeof fuelBedType === 'object') fuelBedType = fuelBedType.ptr;
  _emscripten_bind_SIGIgniteInputs_setIgnitionFuelBedType_1(self, fuelBedType);
};;

SIGIgniteInputs.prototype['setLightningChargeType'] = SIGIgniteInputs.prototype.setLightningChargeType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lightningChargeType) {
  var self = this.ptr;
  if (lightningChargeType && typeof lightningChargeType === 'object') lightningChargeType = lightningChargeType.ptr;
  _emscripten_bind_SIGIgniteInputs_setLightningChargeType_1(self, lightningChargeType);
};;

SIGIgniteInputs.prototype['setMoistureHundredHour'] = SIGIgniteInputs.prototype.setMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(hundredHourMoisture, moistureUnits) {
  var self = this.ptr;
  if (hundredHourMoisture && typeof hundredHourMoisture === 'object') hundredHourMoisture = hundredHourMoisture.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGIgniteInputs_setMoistureHundredHour_2(self, hundredHourMoisture, moistureUnits);
};;

SIGIgniteInputs.prototype['setMoistureOneHour'] = SIGIgniteInputs.prototype.setMoistureOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureOneHour, moistureUnits) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGIgniteInputs_setMoistureOneHour_2(self, moistureOneHour, moistureUnits);
};;

SIGIgniteInputs.prototype['setSunShade'] = SIGIgniteInputs.prototype.setSunShade = /** @suppress {undefinedVars, duplicate} @this{Object} */function(sunShade, sunShadeUnits) {
  var self = this.ptr;
  if (sunShade && typeof sunShade === 'object') sunShade = sunShade.ptr;
  if (sunShadeUnits && typeof sunShadeUnits === 'object') sunShadeUnits = sunShadeUnits.ptr;
  _emscripten_bind_SIGIgniteInputs_setSunShade_2(self, sunShade, sunShadeUnits);
};;

SIGIgniteInputs.prototype['updateIgniteInputs'] = SIGIgniteInputs.prototype.updateIgniteInputs = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureOneHour, moistureHundredHour, moistureUnits, airTemperature, temperatureUnits, sunShade, sunShadeUnits, fuelBedType, duffDepth, duffDepthUnits, lightningChargeType) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  if (sunShade && typeof sunShade === 'object') sunShade = sunShade.ptr;
  if (sunShadeUnits && typeof sunShadeUnits === 'object') sunShadeUnits = sunShadeUnits.ptr;
  if (fuelBedType && typeof fuelBedType === 'object') fuelBedType = fuelBedType.ptr;
  if (duffDepth && typeof duffDepth === 'object') duffDepth = duffDepth.ptr;
  if (duffDepthUnits && typeof duffDepthUnits === 'object') duffDepthUnits = duffDepthUnits.ptr;
  if (lightningChargeType && typeof lightningChargeType === 'object') lightningChargeType = lightningChargeType.ptr;
  _emscripten_bind_SIGIgniteInputs_updateIgniteInputs_11(self, moistureOneHour, moistureHundredHour, moistureUnits, airTemperature, temperatureUnits, sunShade, sunShadeUnits, fuelBedType, duffDepth, duffDepthUnits, lightningChargeType);
};;

SIGIgniteInputs.prototype['getIgnitionFuelBedType'] = SIGIgniteInputs.prototype.getIgnitionFuelBedType = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGIgniteInputs_getIgnitionFuelBedType_0(self);
};;

SIGIgniteInputs.prototype['getLightningChargeType'] = SIGIgniteInputs.prototype.getLightningChargeType = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGIgniteInputs_getLightningChargeType_0(self);
};;

SIGIgniteInputs.prototype['getAirTemperature'] = SIGIgniteInputs.prototype.getAirTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgniteInputs_getAirTemperature_1(self, desiredUnits);
};;

SIGIgniteInputs.prototype['getDuffDepth'] = SIGIgniteInputs.prototype.getDuffDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgniteInputs_getDuffDepth_1(self, desiredUnits);
};;

SIGIgniteInputs.prototype['getMoistureHundredHour'] = SIGIgniteInputs.prototype.getMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgniteInputs_getMoistureHundredHour_1(self, desiredUnits);
};;

SIGIgniteInputs.prototype['getMoistureOneHour'] = SIGIgniteInputs.prototype.getMoistureOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgniteInputs_getMoistureOneHour_1(self, desiredUnits);
};;

SIGIgniteInputs.prototype['getSunShade'] = SIGIgniteInputs.prototype.getSunShade = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgniteInputs_getSunShade_1(self, desiredUnits);
};;

  SIGIgniteInputs.prototype['__destroy__'] = SIGIgniteInputs.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGIgniteInputs___destroy___0(self);
};
// SIGIgnite
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGIgnite() {
  this.ptr = _emscripten_bind_SIGIgnite_SIGIgnite_0();
  getCache(SIGIgnite)[this.ptr] = this;
};;
SIGIgnite.prototype = Object.create(WrapperObject.prototype);
SIGIgnite.prototype.constructor = SIGIgnite;
SIGIgnite.prototype.__class__ = SIGIgnite;
SIGIgnite.__cache__ = {};
Module['SIGIgnite'] = SIGIgnite;

SIGIgnite.prototype['initializeMembers'] = SIGIgnite.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGIgnite_initializeMembers_0(self);
};;

SIGIgnite.prototype['getFuelBedType'] = SIGIgnite.prototype.getFuelBedType = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGIgnite_getFuelBedType_0(self);
};;

SIGIgnite.prototype['getLightningChargeType'] = SIGIgnite.prototype.getLightningChargeType = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGIgnite_getLightningChargeType_0(self);
};;

SIGIgnite.prototype['calculateFirebrandIgnitionProbability'] = SIGIgnite.prototype.calculateFirebrandIgnitionProbability = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_calculateFirebrandIgnitionProbability_1(self, desiredUnits);
};;

SIGIgnite.prototype['calculateLightningIgnitionProbability'] = SIGIgnite.prototype.calculateLightningIgnitionProbability = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_calculateLightningIgnitionProbability_1(self, desiredUnits);
};;

SIGIgnite.prototype['setAirTemperature'] = SIGIgnite.prototype.setAirTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(airTemperature, temperatureUnites) {
  var self = this.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnites && typeof temperatureUnites === 'object') temperatureUnites = temperatureUnites.ptr;
  _emscripten_bind_SIGIgnite_setAirTemperature_2(self, airTemperature, temperatureUnites);
};;

SIGIgnite.prototype['setDuffDepth'] = SIGIgnite.prototype.setDuffDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(duffDepth, lengthUnits) {
  var self = this.ptr;
  if (duffDepth && typeof duffDepth === 'object') duffDepth = duffDepth.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGIgnite_setDuffDepth_2(self, duffDepth, lengthUnits);
};;

SIGIgnite.prototype['setIgnitionFuelBedType'] = SIGIgnite.prototype.setIgnitionFuelBedType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelBedType_) {
  var self = this.ptr;
  if (fuelBedType_ && typeof fuelBedType_ === 'object') fuelBedType_ = fuelBedType_.ptr;
  _emscripten_bind_SIGIgnite_setIgnitionFuelBedType_1(self, fuelBedType_);
};;

SIGIgnite.prototype['setLightningChargeType'] = SIGIgnite.prototype.setLightningChargeType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lightningChargeType) {
  var self = this.ptr;
  if (lightningChargeType && typeof lightningChargeType === 'object') lightningChargeType = lightningChargeType.ptr;
  _emscripten_bind_SIGIgnite_setLightningChargeType_1(self, lightningChargeType);
};;

SIGIgnite.prototype['setMoistureHundredHour'] = SIGIgnite.prototype.setMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureHundredHour, moistureUnits) {
  var self = this.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGIgnite_setMoistureHundredHour_2(self, moistureHundredHour, moistureUnits);
};;

SIGIgnite.prototype['setMoistureOneHour'] = SIGIgnite.prototype.setMoistureOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureOneHour, moistureUnits) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGIgnite_setMoistureOneHour_2(self, moistureOneHour, moistureUnits);
};;

SIGIgnite.prototype['setSunShade'] = SIGIgnite.prototype.setSunShade = /** @suppress {undefinedVars, duplicate} @this{Object} */function(sunShade, sunShadeUnits) {
  var self = this.ptr;
  if (sunShade && typeof sunShade === 'object') sunShade = sunShade.ptr;
  if (sunShadeUnits && typeof sunShadeUnits === 'object') sunShadeUnits = sunShadeUnits.ptr;
  _emscripten_bind_SIGIgnite_setSunShade_2(self, sunShade, sunShadeUnits);
};;

SIGIgnite.prototype['updateIgniteInputs'] = SIGIgnite.prototype.updateIgniteInputs = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureOneHour, moistureHundredHour, moistureUnits, airTemperature, temperatureUnits, sunShade, sunShadeUnits, fuelBedType, duffDepth, duffDepthUnits, lightningChargeType) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  if (sunShade && typeof sunShade === 'object') sunShade = sunShade.ptr;
  if (sunShadeUnits && typeof sunShadeUnits === 'object') sunShadeUnits = sunShadeUnits.ptr;
  if (fuelBedType && typeof fuelBedType === 'object') fuelBedType = fuelBedType.ptr;
  if (duffDepth && typeof duffDepth === 'object') duffDepth = duffDepth.ptr;
  if (duffDepthUnits && typeof duffDepthUnits === 'object') duffDepthUnits = duffDepthUnits.ptr;
  if (lightningChargeType && typeof lightningChargeType === 'object') lightningChargeType = lightningChargeType.ptr;
  _emscripten_bind_SIGIgnite_updateIgniteInputs_11(self, moistureOneHour, moistureHundredHour, moistureUnits, airTemperature, temperatureUnits, sunShade, sunShadeUnits, fuelBedType, duffDepth, duffDepthUnits, lightningChargeType);
};;

SIGIgnite.prototype['getAirTemperature'] = SIGIgnite.prototype.getAirTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getAirTemperature_1(self, desiredUnits);
};;

SIGIgnite.prototype['getDuffDepth'] = SIGIgnite.prototype.getDuffDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getDuffDepth_1(self, desiredUnits);
};;

SIGIgnite.prototype['getFuelTemperature'] = SIGIgnite.prototype.getFuelTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getFuelTemperature_1(self, desiredUnits);
};;

SIGIgnite.prototype['getMoistureHundredHour'] = SIGIgnite.prototype.getMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getMoistureHundredHour_1(self, desiredUnits);
};;

SIGIgnite.prototype['getMoistureOneHour'] = SIGIgnite.prototype.getMoistureOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getMoistureOneHour_1(self, desiredUnits);
};;

SIGIgnite.prototype['getSunShade'] = SIGIgnite.prototype.getSunShade = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getSunShade_1(self, desiredUnits);
};;

SIGIgnite.prototype['isFuelDepthNeeded'] = SIGIgnite.prototype.isFuelDepthNeeded = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGIgnite_isFuelDepthNeeded_0(self));
};;

  SIGIgnite.prototype['__destroy__'] = SIGIgnite.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGIgnite___destroy___0(self);
};
// SIGSpotInputs
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGSpotInputs() {
  this.ptr = _emscripten_bind_SIGSpotInputs_SIGSpotInputs_0();
  getCache(SIGSpotInputs)[this.ptr] = this;
};;
SIGSpotInputs.prototype = Object.create(WrapperObject.prototype);
SIGSpotInputs.prototype.constructor = SIGSpotInputs;
SIGSpotInputs.prototype.__class__ = SIGSpotInputs;
SIGSpotInputs.__cache__ = {};
Module['SIGSpotInputs'] = SIGSpotInputs;

SIGSpotInputs.prototype['getLocation'] = SIGSpotInputs.prototype.getLocation = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpotInputs_getLocation_0(self);
};;

SIGSpotInputs.prototype['getTreeSpecies'] = SIGSpotInputs.prototype.getTreeSpecies = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpotInputs_getTreeSpecies_0(self);
};;

SIGSpotInputs.prototype['setBurningPileFlameHeight'] = SIGSpotInputs.prototype.setBurningPileFlameHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(buringPileFlameHeight, flameHeightUnits) {
  var self = this.ptr;
  if (buringPileFlameHeight && typeof buringPileFlameHeight === 'object') buringPileFlameHeight = buringPileFlameHeight.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  _emscripten_bind_SIGSpotInputs_setBurningPileFlameHeight_2(self, buringPileFlameHeight, flameHeightUnits);
};;

SIGSpotInputs.prototype['setDBH'] = SIGSpotInputs.prototype.setDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(DBH, DBHUnits) {
  var self = this.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  _emscripten_bind_SIGSpotInputs_setDBH_2(self, DBH, DBHUnits);
};;

SIGSpotInputs.prototype['setDownwindCoverHeight'] = SIGSpotInputs.prototype.setDownwindCoverHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(downwindCoverHeight, coverHeightUnits) {
  var self = this.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  _emscripten_bind_SIGSpotInputs_setDownwindCoverHeight_2(self, downwindCoverHeight, coverHeightUnits);
};;

SIGSpotInputs.prototype['setLocation'] = SIGSpotInputs.prototype.setLocation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  _emscripten_bind_SIGSpotInputs_setLocation_1(self, location);
};;

SIGSpotInputs.prototype['setRidgeToValleyDistance'] = SIGSpotInputs.prototype.setRidgeToValleyDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ridgeToValleyDistance, ridgeToValleyDistanceUnits) {
  var self = this.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  _emscripten_bind_SIGSpotInputs_setRidgeToValleyDistance_2(self, ridgeToValleyDistance, ridgeToValleyDistanceUnits);
};;

SIGSpotInputs.prototype['setRidgeToValleyElevation'] = SIGSpotInputs.prototype.setRidgeToValleyElevation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ridgeToValleyElevation, elevationUnits) {
  var self = this.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  _emscripten_bind_SIGSpotInputs_setRidgeToValleyElevation_2(self, ridgeToValleyElevation, elevationUnits);
};;

SIGSpotInputs.prototype['setSurfaceFlameLength'] = SIGSpotInputs.prototype.setSurfaceFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(surfaceFlameLength, flameLengthUnits) {
  var self = this.ptr;
  if (surfaceFlameLength && typeof surfaceFlameLength === 'object') surfaceFlameLength = surfaceFlameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGSpotInputs_setSurfaceFlameLength_2(self, surfaceFlameLength, flameLengthUnits);
};;

SIGSpotInputs.prototype['setTorchingTrees'] = SIGSpotInputs.prototype.setTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(torchingTrees) {
  var self = this.ptr;
  if (torchingTrees && typeof torchingTrees === 'object') torchingTrees = torchingTrees.ptr;
  _emscripten_bind_SIGSpotInputs_setTorchingTrees_1(self, torchingTrees);
};;

SIGSpotInputs.prototype['setTreeHeight'] = SIGSpotInputs.prototype.setTreeHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeHeight, treeHeightUnits) {
  var self = this.ptr;
  if (treeHeight && typeof treeHeight === 'object') treeHeight = treeHeight.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  _emscripten_bind_SIGSpotInputs_setTreeHeight_2(self, treeHeight, treeHeightUnits);
};;

SIGSpotInputs.prototype['setTreeSpecies'] = SIGSpotInputs.prototype.setTreeSpecies = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeSpecies) {
  var self = this.ptr;
  if (treeSpecies && typeof treeSpecies === 'object') treeSpecies = treeSpecies.ptr;
  _emscripten_bind_SIGSpotInputs_setTreeSpecies_1(self, treeSpecies);
};;

SIGSpotInputs.prototype['setWindSpeedAtTwentyFeet'] = SIGSpotInputs.prototype.setWindSpeedAtTwentyFeet = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSpotInputs_setWindSpeedAtTwentyFeet_2(self, windSpeedAtTwentyFeet, windSpeedUnits);
};;

SIGSpotInputs.prototype['updateSpotInputsForBurningPile'] = SIGSpotInputs.prototype.updateSpotInputsForBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  if (buringPileFlameHeight && typeof buringPileFlameHeight === 'object') buringPileFlameHeight = buringPileFlameHeight.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSpotInputs_updateSpotInputsForBurningPile_11(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits);
};;

SIGSpotInputs.prototype['updateSpotInputsForSurfaceFire'] = SIGSpotInputs.prototype.updateSpotInputsForSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits, surfaceFlameLength, flameLengthUnits) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (surfaceFlameLength && typeof surfaceFlameLength === 'object') surfaceFlameLength = surfaceFlameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGSpotInputs_updateSpotInputsForSurfaceFire_11(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits, surfaceFlameLength, flameLengthUnits);
};;

SIGSpotInputs.prototype['updateSpotInputsForTorchingTrees'] = SIGSpotInputs.prototype.updateSpotInputsForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  if (torchingTrees && typeof torchingTrees === 'object') torchingTrees = torchingTrees.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  if (treeHeight && typeof treeHeight === 'object') treeHeight = treeHeight.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  if (treeSpecies && typeof treeSpecies === 'object') treeSpecies = treeSpecies.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSpotInputs_updateSpotInputsForTorchingTrees_15(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits);
};;

SIGSpotInputs.prototype['getBurningPileFlameHeight'] = SIGSpotInputs.prototype.getBurningPileFlameHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameHeightUnits) {
  var self = this.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  return _emscripten_bind_SIGSpotInputs_getBurningPileFlameHeight_1(self, flameHeightUnits);
};;

SIGSpotInputs.prototype['getDBH'] = SIGSpotInputs.prototype.getDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(DBHUnits) {
  var self = this.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  return _emscripten_bind_SIGSpotInputs_getDBH_1(self, DBHUnits);
};;

SIGSpotInputs.prototype['getDownwindCoverHeight'] = SIGSpotInputs.prototype.getDownwindCoverHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_SIGSpotInputs_getDownwindCoverHeight_1(self, coverHeightUnits);
};;

SIGSpotInputs.prototype['getRidgeToValleyDistance'] = SIGSpotInputs.prototype.getRidgeToValleyDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ridgeToValleyDistanceUnits) {
  var self = this.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  return _emscripten_bind_SIGSpotInputs_getRidgeToValleyDistance_1(self, ridgeToValleyDistanceUnits);
};;

SIGSpotInputs.prototype['getRidgeToValleyElevation'] = SIGSpotInputs.prototype.getRidgeToValleyElevation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(elevationUnits) {
  var self = this.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  return _emscripten_bind_SIGSpotInputs_getRidgeToValleyElevation_1(self, elevationUnits);
};;

SIGSpotInputs.prototype['getSurfaceFlameLength'] = SIGSpotInputs.prototype.getSurfaceFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSpotInputs_getSurfaceFlameLength_1(self, flameLengthUnits);
};;

SIGSpotInputs.prototype['getTreeHeight'] = SIGSpotInputs.prototype.getTreeHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeHeightUnits) {
  var self = this.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  return _emscripten_bind_SIGSpotInputs_getTreeHeight_1(self, treeHeightUnits);
};;

SIGSpotInputs.prototype['getWindSpeedAtTwentyFeet'] = SIGSpotInputs.prototype.getWindSpeedAtTwentyFeet = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  return _emscripten_bind_SIGSpotInputs_getWindSpeedAtTwentyFeet_1(self, windSpeedUnits);
};;

SIGSpotInputs.prototype['getTorchingTrees'] = SIGSpotInputs.prototype.getTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpotInputs_getTorchingTrees_0(self);
};;

  SIGSpotInputs.prototype['__destroy__'] = SIGSpotInputs.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpotInputs___destroy___0(self);
};
// SIGSpot
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGSpot() {
  this.ptr = _emscripten_bind_SIGSpot_SIGSpot_0();
  getCache(SIGSpot)[this.ptr] = this;
};;
SIGSpot.prototype = Object.create(WrapperObject.prototype);
SIGSpot.prototype.constructor = SIGSpot;
SIGSpot.prototype.__class__ = SIGSpot;
SIGSpot.__cache__ = {};
Module['SIGSpot'] = SIGSpot;

SIGSpot.prototype['initializeMembers'] = SIGSpot.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_initializeMembers_0(self);
};;

SIGSpot.prototype['calculateSpottingDistanceFromBurningPile'] = SIGSpot.prototype.calculateSpottingDistanceFromBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_calculateSpottingDistanceFromBurningPile_0(self);
};;

SIGSpot.prototype['calculateSpottingDistanceFromSurfaceFire'] = SIGSpot.prototype.calculateSpottingDistanceFromSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_calculateSpottingDistanceFromSurfaceFire_0(self);
};;

SIGSpot.prototype['calculateSpottingDistanceFromTorchingTrees'] = SIGSpot.prototype.calculateSpottingDistanceFromTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_calculateSpottingDistanceFromTorchingTrees_0(self);
};;

SIGSpot.prototype['setBurningPileFlameHeight'] = SIGSpot.prototype.setBurningPileFlameHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(buringPileflameHeight, flameHeightUnits) {
  var self = this.ptr;
  if (buringPileflameHeight && typeof buringPileflameHeight === 'object') buringPileflameHeight = buringPileflameHeight.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  _emscripten_bind_SIGSpot_setBurningPileFlameHeight_2(self, buringPileflameHeight, flameHeightUnits);
};;

SIGSpot.prototype['setDBH'] = SIGSpot.prototype.setDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(DBH, DBHUnits) {
  var self = this.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  _emscripten_bind_SIGSpot_setDBH_2(self, DBH, DBHUnits);
};;

SIGSpot.prototype['setDownwindCoverHeight'] = SIGSpot.prototype.setDownwindCoverHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(downwindCoverHeight, coverHeightUnits) {
  var self = this.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  _emscripten_bind_SIGSpot_setDownwindCoverHeight_2(self, downwindCoverHeight, coverHeightUnits);
};;

SIGSpot.prototype['setFlameLength'] = SIGSpot.prototype.setFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGSpot_setFlameLength_2(self, flameLength, flameLengthUnits);
};;

SIGSpot.prototype['setLocation'] = SIGSpot.prototype.setLocation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  _emscripten_bind_SIGSpot_setLocation_1(self, location);
};;

SIGSpot.prototype['setRidgeToValleyDistance'] = SIGSpot.prototype.setRidgeToValleyDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ridgeToValleyDistance, ridgeToValleyDistanceUnits) {
  var self = this.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  _emscripten_bind_SIGSpot_setRidgeToValleyDistance_2(self, ridgeToValleyDistance, ridgeToValleyDistanceUnits);
};;

SIGSpot.prototype['setRidgeToValleyElevation'] = SIGSpot.prototype.setRidgeToValleyElevation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ridgeToValleyElevation, elevationUnits) {
  var self = this.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  _emscripten_bind_SIGSpot_setRidgeToValleyElevation_2(self, ridgeToValleyElevation, elevationUnits);
};;

SIGSpot.prototype['setTorchingTrees'] = SIGSpot.prototype.setTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(torchingTrees) {
  var self = this.ptr;
  if (torchingTrees && typeof torchingTrees === 'object') torchingTrees = torchingTrees.ptr;
  _emscripten_bind_SIGSpot_setTorchingTrees_1(self, torchingTrees);
};;

SIGSpot.prototype['setTreeHeight'] = SIGSpot.prototype.setTreeHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeHeight, treeHeightUnits) {
  var self = this.ptr;
  if (treeHeight && typeof treeHeight === 'object') treeHeight = treeHeight.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  _emscripten_bind_SIGSpot_setTreeHeight_2(self, treeHeight, treeHeightUnits);
};;

SIGSpot.prototype['setTreeSpecies'] = SIGSpot.prototype.setTreeSpecies = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeSpecies) {
  var self = this.ptr;
  if (treeSpecies && typeof treeSpecies === 'object') treeSpecies = treeSpecies.ptr;
  _emscripten_bind_SIGSpot_setTreeSpecies_1(self, treeSpecies);
};;

SIGSpot.prototype['setWindSpeedAtTwentyFeet'] = SIGSpot.prototype.setWindSpeedAtTwentyFeet = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSpot_setWindSpeedAtTwentyFeet_2(self, windSpeedAtTwentyFeet, windSpeedUnits);
};;

SIGSpot.prototype['updateSpotInputsForBurningPile'] = SIGSpot.prototype.updateSpotInputsForBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  if (buringPileFlameHeight && typeof buringPileFlameHeight === 'object') buringPileFlameHeight = buringPileFlameHeight.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSpot_updateSpotInputsForBurningPile_11(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits);
};;

SIGSpot.prototype['updateSpotInputsForSurfaceFire'] = SIGSpot.prototype.updateSpotInputsForSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits, flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGSpot_updateSpotInputsForSurfaceFire_11(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits, flameLength, flameLengthUnits);
};;

SIGSpot.prototype['updateSpotInputsForTorchingTrees'] = SIGSpot.prototype.updateSpotInputsForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  if (torchingTrees && typeof torchingTrees === 'object') torchingTrees = torchingTrees.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  if (treeHeight && typeof treeHeight === 'object') treeHeight = treeHeight.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  if (treeSpecies && typeof treeSpecies === 'object') treeSpecies = treeSpecies.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSpot_updateSpotInputsForTorchingTrees_15(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits);
};;

SIGSpot.prototype['getTorchingTrees'] = SIGSpot.prototype.getTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getTorchingTrees_0(self);
};;

SIGSpot.prototype['getLocation'] = SIGSpot.prototype.getLocation = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getLocation_0(self);
};;

SIGSpot.prototype['getTreeSpecies'] = SIGSpot.prototype.getTreeSpecies = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getTreeSpecies_0(self);
};;

SIGSpot.prototype['getBurningPileFlameHeight'] = SIGSpot.prototype.getBurningPileFlameHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameHeightUnits) {
  var self = this.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getBurningPileFlameHeight_1(self, flameHeightUnits);
};;

SIGSpot.prototype['getCoverHeightUsedForBurningPile'] = SIGSpot.prototype.getCoverHeightUsedForBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getCoverHeightUsedForBurningPile_1(self, coverHeightUnits);
};;

SIGSpot.prototype['getCoverHeightUsedForSurfaceFire'] = SIGSpot.prototype.getCoverHeightUsedForSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getCoverHeightUsedForSurfaceFire_1(self, coverHeightUnits);
};;

SIGSpot.prototype['getCoverHeightUsedForTorchingTrees'] = SIGSpot.prototype.getCoverHeightUsedForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getCoverHeightUsedForTorchingTrees_1(self, coverHeightUnits);
};;

SIGSpot.prototype['getDBH'] = SIGSpot.prototype.getDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(DBHUnits) {
  var self = this.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  return _emscripten_bind_SIGSpot_getDBH_1(self, DBHUnits);
};;

SIGSpot.prototype['getDownwindCoverHeight'] = SIGSpot.prototype.getDownwindCoverHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getDownwindCoverHeight_1(self, coverHeightUnits);
};;

SIGSpot.prototype['getFlameDurationForTorchingTrees'] = SIGSpot.prototype.getFlameDurationForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(durationUnits) {
  var self = this.ptr;
  if (durationUnits && typeof durationUnits === 'object') durationUnits = durationUnits.ptr;
  return _emscripten_bind_SIGSpot_getFlameDurationForTorchingTrees_1(self, durationUnits);
};;

SIGSpot.prototype['getFlameHeightForTorchingTrees'] = SIGSpot.prototype.getFlameHeightForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameHeightUnits) {
  var self = this.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getFlameHeightForTorchingTrees_1(self, flameHeightUnits);
};;

SIGSpot.prototype['getFlameRatioForTorchingTrees'] = SIGSpot.prototype.getFlameRatioForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getFlameRatioForTorchingTrees_0(self);
};;

SIGSpot.prototype['getMaxFirebrandHeightFromBurningPile'] = SIGSpot.prototype.getMaxFirebrandHeightFromBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firebrandHeightUnits) {
  var self = this.ptr;
  if (firebrandHeightUnits && typeof firebrandHeightUnits === 'object') firebrandHeightUnits = firebrandHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFirebrandHeightFromBurningPile_1(self, firebrandHeightUnits);
};;

SIGSpot.prototype['getMaxFirebrandHeightFromSurfaceFire'] = SIGSpot.prototype.getMaxFirebrandHeightFromSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firebrandHeightUnits) {
  var self = this.ptr;
  if (firebrandHeightUnits && typeof firebrandHeightUnits === 'object') firebrandHeightUnits = firebrandHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFirebrandHeightFromSurfaceFire_1(self, firebrandHeightUnits);
};;

SIGSpot.prototype['getMaxFirebrandHeightFromTorchingTrees'] = SIGSpot.prototype.getMaxFirebrandHeightFromTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firebrandHeightUnits) {
  var self = this.ptr;
  if (firebrandHeightUnits && typeof firebrandHeightUnits === 'object') firebrandHeightUnits = firebrandHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFirebrandHeightFromTorchingTrees_1(self, firebrandHeightUnits);
};;

SIGSpot.prototype['getMaxFlatTerrainSpottingDistanceFromBurningPile'] = SIGSpot.prototype.getMaxFlatTerrainSpottingDistanceFromBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1(self, spottingDistanceUnits);
};;

SIGSpot.prototype['getMaxFlatTerrainSpottingDistanceFromSurfaceFire'] = SIGSpot.prototype.getMaxFlatTerrainSpottingDistanceFromSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1(self, spottingDistanceUnits);
};;

SIGSpot.prototype['getMaxFlatTerrainSpottingDistanceFromTorchingTrees'] = SIGSpot.prototype.getMaxFlatTerrainSpottingDistanceFromTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1(self, spottingDistanceUnits);
};;

SIGSpot.prototype['getMaxMountainousTerrainSpottingDistanceFromBurningPile'] = SIGSpot.prototype.getMaxMountainousTerrainSpottingDistanceFromBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1(self, spottingDistanceUnits);
};;

SIGSpot.prototype['getMaxMountainousTerrainSpottingDistanceFromSurfaceFire'] = SIGSpot.prototype.getMaxMountainousTerrainSpottingDistanceFromSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1(self, spottingDistanceUnits);
};;

SIGSpot.prototype['getMaxMountainousTerrainSpottingDistanceFromTorchingTrees'] = SIGSpot.prototype.getMaxMountainousTerrainSpottingDistanceFromTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1(self, spottingDistanceUnits);
};;

SIGSpot.prototype['getRidgeToValleyDistance'] = SIGSpot.prototype.getRidgeToValleyDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ridgeToValleyDistanceUnits) {
  var self = this.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getRidgeToValleyDistance_1(self, ridgeToValleyDistanceUnits);
};;

SIGSpot.prototype['getRidgeToValleyElevation'] = SIGSpot.prototype.getRidgeToValleyElevation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(elevationUnits) {
  var self = this.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  return _emscripten_bind_SIGSpot_getRidgeToValleyElevation_1(self, elevationUnits);
};;

SIGSpot.prototype['getSurfaceFlameLength'] = SIGSpot.prototype.getSurfaceFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(surfaceFlameLengthUnits) {
  var self = this.ptr;
  if (surfaceFlameLengthUnits && typeof surfaceFlameLengthUnits === 'object') surfaceFlameLengthUnits = surfaceFlameLengthUnits.ptr;
  return _emscripten_bind_SIGSpot_getSurfaceFlameLength_1(self, surfaceFlameLengthUnits);
};;

SIGSpot.prototype['getTreeHeight'] = SIGSpot.prototype.getTreeHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeHeightUnits) {
  var self = this.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getTreeHeight_1(self, treeHeightUnits);
};;

SIGSpot.prototype['getWindSpeedAtTwentyFeet'] = SIGSpot.prototype.getWindSpeedAtTwentyFeet = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  return _emscripten_bind_SIGSpot_getWindSpeedAtTwentyFeet_1(self, windSpeedUnits);
};;

  SIGSpot.prototype['__destroy__'] = SIGSpot.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot___destroy___0(self);
};
// SIGFuelModels
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGFuelModels(rhs) {
  if (rhs && typeof rhs === 'object') rhs = rhs.ptr;
  if (rhs === undefined) { this.ptr = _emscripten_bind_SIGFuelModels_SIGFuelModels_0(); getCache(SIGFuelModels)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_SIGFuelModels_SIGFuelModels_1(rhs);
  getCache(SIGFuelModels)[this.ptr] = this;
};;
SIGFuelModels.prototype = Object.create(WrapperObject.prototype);
SIGFuelModels.prototype.constructor = SIGFuelModels;
SIGFuelModels.prototype.__class__ = SIGFuelModels;
SIGFuelModels.__cache__ = {};
Module['SIGFuelModels'] = SIGFuelModels;

SIGFuelModels.prototype['equal'] = SIGFuelModels.prototype.equal = /** @suppress {undefinedVars, duplicate} @this{Object} */function(rhs) {
  var self = this.ptr;
  if (rhs && typeof rhs === 'object') rhs = rhs.ptr;
  return wrapPointer(_emscripten_bind_SIGFuelModels_equal_1(self, rhs), SIGFuelModels);
};;

SIGFuelModels.prototype['clearCustomFuelModel'] = SIGFuelModels.prototype.clearCustomFuelModel = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_clearCustomFuelModel_1(self, fuelModelNumber));
};;

SIGFuelModels.prototype['getIsDynamic'] = SIGFuelModels.prototype.getIsDynamic = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_getIsDynamic_1(self, fuelModelNumber));
};;

SIGFuelModels.prototype['isFuelModelDefined'] = SIGFuelModels.prototype.isFuelModelDefined = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_isFuelModelDefined_1(self, fuelModelNumber));
};;

SIGFuelModels.prototype['setCustomFuelModel'] = SIGFuelModels.prototype.setCustomFuelModel = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumberIn, code, name, fuelBedDepth, lengthUnits, moistureOfExtinctionDead, moistureUnits, heatOfCombustionDead, heatOfCombustionLive, heatOfCombustionUnits, fuelLoadOneHour, fuelLoadTenHour, fuelLoadHundredHour, fuelLoadLiveHerbaceous, fuelLoadLiveWoody, loadingUnits, savrOneHour, savrLiveHerbaceous, savrLiveWoody, savrUnits, isDynamic) {
  var self = this.ptr;
  ensureCache.prepare();
  if (fuelModelNumberIn && typeof fuelModelNumberIn === 'object') fuelModelNumberIn = fuelModelNumberIn.ptr;
  if (code && typeof code === 'object') code = code.ptr;
  else code = ensureString(code);
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (fuelBedDepth && typeof fuelBedDepth === 'object') fuelBedDepth = fuelBedDepth.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (moistureOfExtinctionDead && typeof moistureOfExtinctionDead === 'object') moistureOfExtinctionDead = moistureOfExtinctionDead.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  if (heatOfCombustionDead && typeof heatOfCombustionDead === 'object') heatOfCombustionDead = heatOfCombustionDead.ptr;
  if (heatOfCombustionLive && typeof heatOfCombustionLive === 'object') heatOfCombustionLive = heatOfCombustionLive.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  if (fuelLoadOneHour && typeof fuelLoadOneHour === 'object') fuelLoadOneHour = fuelLoadOneHour.ptr;
  if (fuelLoadTenHour && typeof fuelLoadTenHour === 'object') fuelLoadTenHour = fuelLoadTenHour.ptr;
  if (fuelLoadHundredHour && typeof fuelLoadHundredHour === 'object') fuelLoadHundredHour = fuelLoadHundredHour.ptr;
  if (fuelLoadLiveHerbaceous && typeof fuelLoadLiveHerbaceous === 'object') fuelLoadLiveHerbaceous = fuelLoadLiveHerbaceous.ptr;
  if (fuelLoadLiveWoody && typeof fuelLoadLiveWoody === 'object') fuelLoadLiveWoody = fuelLoadLiveWoody.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  if (savrOneHour && typeof savrOneHour === 'object') savrOneHour = savrOneHour.ptr;
  if (savrLiveHerbaceous && typeof savrLiveHerbaceous === 'object') savrLiveHerbaceous = savrLiveHerbaceous.ptr;
  if (savrLiveWoody && typeof savrLiveWoody === 'object') savrLiveWoody = savrLiveWoody.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  if (isDynamic && typeof isDynamic === 'object') isDynamic = isDynamic.ptr;
  return !!(_emscripten_bind_SIGFuelModels_setCustomFuelModel_21(self, fuelModelNumberIn, code, name, fuelBedDepth, lengthUnits, moistureOfExtinctionDead, moistureUnits, heatOfCombustionDead, heatOfCombustionLive, heatOfCombustionUnits, fuelLoadOneHour, fuelLoadTenHour, fuelLoadHundredHour, fuelLoadLiveHerbaceous, fuelLoadLiveWoody, loadingUnits, savrOneHour, savrLiveHerbaceous, savrLiveWoody, savrUnits, isDynamic));
};;

SIGFuelModels.prototype['getFuelCode'] = SIGFuelModels.prototype.getFuelCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGFuelModels_getFuelCode_1(self, fuelModelNumber));
};;

SIGFuelModels.prototype['getFuelName'] = SIGFuelModels.prototype.getFuelName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGFuelModels_getFuelName_1(self, fuelModelNumber));
};;

SIGFuelModels.prototype['getFuelLoadHundredHour'] = SIGFuelModels.prototype.getFuelLoadHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadHundredHour_2(self, fuelModelNumber, loadingUnits);
};;

SIGFuelModels.prototype['getFuelLoadLiveHerbaceous'] = SIGFuelModels.prototype.getFuelLoadLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadLiveHerbaceous_2(self, fuelModelNumber, loadingUnits);
};;

SIGFuelModels.prototype['getFuelLoadLiveWoody'] = SIGFuelModels.prototype.getFuelLoadLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadLiveWoody_2(self, fuelModelNumber, loadingUnits);
};;

SIGFuelModels.prototype['getFuelLoadOneHour'] = SIGFuelModels.prototype.getFuelLoadOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadOneHour_2(self, fuelModelNumber, loadingUnits);
};;

SIGFuelModels.prototype['getFuelLoadTenHour'] = SIGFuelModels.prototype.getFuelLoadTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadTenHour_2(self, fuelModelNumber, loadingUnits);
};;

SIGFuelModels.prototype['getFuelbedDepth'] = SIGFuelModels.prototype.getFuelbedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, lengthUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelbedDepth_2(self, fuelModelNumber, lengthUnits);
};;

SIGFuelModels.prototype['getHeatOfCombustionDead'] = SIGFuelModels.prototype.getHeatOfCombustionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getHeatOfCombustionDead_2(self, fuelModelNumber, heatOfCombustionUnits);
};;

SIGFuelModels.prototype['getHeatOfCombustionLive'] = SIGFuelModels.prototype.getHeatOfCombustionLive = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2(self, fuelModelNumber, heatOfCombustionUnits);
};;

SIGFuelModels.prototype['getMoistureOfExtinctionDead'] = SIGFuelModels.prototype.getMoistureOfExtinctionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, moistureUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getMoistureOfExtinctionDead_2(self, fuelModelNumber, moistureUnits);
};;

SIGFuelModels.prototype['getSavrLiveHerbaceous'] = SIGFuelModels.prototype.getSavrLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getSavrLiveHerbaceous_2(self, fuelModelNumber, savrUnits);
};;

SIGFuelModels.prototype['getSavrLiveWoody'] = SIGFuelModels.prototype.getSavrLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getSavrLiveWoody_2(self, fuelModelNumber, savrUnits);
};;

SIGFuelModels.prototype['getSavrOneHour'] = SIGFuelModels.prototype.getSavrOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getSavrOneHour_2(self, fuelModelNumber, savrUnits);
};;

  SIGFuelModels.prototype['__destroy__'] = SIGFuelModels.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGFuelModels___destroy___0(self);
};
// SIGSurface
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGSurface(fuelModels) {
  if (fuelModels && typeof fuelModels === 'object') fuelModels = fuelModels.ptr;
  this.ptr = _emscripten_bind_SIGSurface_SIGSurface_1(fuelModels);
  getCache(SIGSurface)[this.ptr] = this;
};;
SIGSurface.prototype = Object.create(WrapperObject.prototype);
SIGSurface.prototype.constructor = SIGSurface;
SIGSurface.prototype.__class__ = SIGSurface;
SIGSurface.__cache__ = {};
Module['SIGSurface'] = SIGSurface;

SIGSurface.prototype['initializeMembers'] = SIGSurface.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface_initializeMembers_0(self);
};;

SIGSurface.prototype['doSurfaceRunInDirectionOfInterest'] = SIGSurface.prototype.doSurfaceRunInDirectionOfInterest = /** @suppress {undefinedVars, duplicate} @this{Object} */function(directionOfinterest) {
  var self = this.ptr;
  if (directionOfinterest && typeof directionOfinterest === 'object') directionOfinterest = directionOfinterest.ptr;
  _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_1(self, directionOfinterest);
};;

SIGSurface.prototype['doSurfaceRunInDirectionOfMaxSpread'] = SIGSurface.prototype.doSurfaceRunInDirectionOfMaxSpread = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0(self);
};;

SIGSurface.prototype['getWindAdjustmentFactorCalculationMethod'] = SIGSurface.prototype.getWindAdjustmentFactorCalculationMethod = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindAdjustmentFactorCalculationMethod_0(self);
};;

SIGSurface.prototype['getWindAndSpreadOrientationMode'] = SIGSurface.prototype.getWindAndSpreadOrientationMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindAndSpreadOrientationMode_0(self);
};;

SIGSurface.prototype['getWindHeightInputMode'] = SIGSurface.prototype.getWindHeightInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindHeightInputMode_0(self);
};;

SIGSurface.prototype['isAllFuelLoadZero'] = SIGSurface.prototype.isAllFuelLoadZero = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGSurface_isAllFuelLoadZero_1(self, fuelModelNumber));
};;

SIGSurface.prototype['isUsingTwoFuelModels'] = SIGSurface.prototype.isUsingTwoFuelModels = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGSurface_isUsingTwoFuelModels_0(self));
};;

SIGSurface.prototype['calculateFlameLength'] = SIGSurface.prototype.calculateFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensity) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  return _emscripten_bind_SIGSurface_calculateFlameLength_1(self, firelineIntensity);
};;

SIGSurface.prototype['getAspect'] = SIGSurface.prototype.getAspect = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getAspect_0(self);
};;

SIGSurface.prototype['getBulkDensity'] = SIGSurface.prototype.getBulkDensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(densityUnits) {
  var self = this.ptr;
  if (densityUnits && typeof densityUnits === 'object') densityUnits = densityUnits.ptr;
  return _emscripten_bind_SIGSurface_getBulkDensity_1(self, densityUnits);
};;

SIGSurface.prototype['getCanopyCover'] = SIGSurface.prototype.getCanopyCover = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverUnits) {
  var self = this.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  return _emscripten_bind_SIGSurface_getCanopyCover_1(self, coverUnits);
};;

SIGSurface.prototype['getCanopyHeight'] = SIGSurface.prototype.getCanopyHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyHeightUnits) {
  var self = this.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  return _emscripten_bind_SIGSurface_getCanopyHeight_1(self, canopyHeightUnits);
};;

SIGSurface.prototype['getCrownRatio'] = SIGSurface.prototype.getCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getCrownRatio_0(self);
};;

SIGSurface.prototype['getDirectionOfMaxSpread'] = SIGSurface.prototype.getDirectionOfMaxSpread = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getDirectionOfMaxSpread_0(self);
};;

SIGSurface.prototype['getEllipticalA'] = SIGSurface.prototype.getEllipticalA = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getEllipticalA_3(self, lengthUnits, elapsedTime, timeUnits);
};;

SIGSurface.prototype['getEllipticalB'] = SIGSurface.prototype.getEllipticalB = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getEllipticalB_3(self, lengthUnits, elapsedTime, timeUnits);
};;

SIGSurface.prototype['getEllipticalC'] = SIGSurface.prototype.getEllipticalC = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getEllipticalC_3(self, lengthUnits, elapsedTime, timeUnits);
};;

SIGSurface.prototype['getFireArea'] = SIGSurface.prototype.getFireArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getFireArea_3(self, areaUnits, elapsedTime, timeUnits);
};;

SIGSurface.prototype['getFireEccentricity'] = SIGSurface.prototype.getFireEccentricity = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getFireEccentricity_0(self);
};;

SIGSurface.prototype['getFireLengthToWidthRatio'] = SIGSurface.prototype.getFireLengthToWidthRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getFireLengthToWidthRatio_0(self);
};;

SIGSurface.prototype['getFirePerimeter'] = SIGSurface.prototype.getFirePerimeter = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getFirePerimeter_3(self, lengthUnits, elapsedTime, timeUnits);
};;

SIGSurface.prototype['getFirelineIntensity'] = SIGSurface.prototype.getFirelineIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getFirelineIntensity_1(self, firelineIntensityUnits);
};;

SIGSurface.prototype['getFlameLength'] = SIGSurface.prototype.getFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlameLength_1(self, flameLengthUnits);
};;

SIGSurface.prototype['getHeatPerUnitArea'] = SIGSurface.prototype.getHeatPerUnitArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heatPerUnitAreaUnits) {
  var self = this.ptr;
  if (heatPerUnitAreaUnits && typeof heatPerUnitAreaUnits === 'object') heatPerUnitAreaUnits = heatPerUnitAreaUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeatPerUnitArea_1(self, heatPerUnitAreaUnits);
};;

SIGSurface.prototype['getHeatSink'] = SIGSurface.prototype.getHeatSink = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heatSinkUnits) {
  var self = this.ptr;
  if (heatSinkUnits && typeof heatSinkUnits === 'object') heatSinkUnits = heatSinkUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeatSink_1(self, heatSinkUnits);
};;

SIGSurface.prototype['getMidflameWindspeed'] = SIGSurface.prototype.getMidflameWindspeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  return _emscripten_bind_SIGSurface_getMidflameWindspeed_1(self, windSpeedUnits);
};;

SIGSurface.prototype['getMoistureHundredHour'] = SIGSurface.prototype.getMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureHundredHour_1(self, moistureUnits);
};;

SIGSurface.prototype['getMoistureLiveHerbaceous'] = SIGSurface.prototype.getMoistureLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1(self, moistureUnits);
};;

SIGSurface.prototype['getMoistureLiveWoody'] = SIGSurface.prototype.getMoistureLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureLiveWoody_1(self, moistureUnits);
};;

SIGSurface.prototype['getMoistureOneHour'] = SIGSurface.prototype.getMoistureOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureOneHour_1(self, moistureUnits);
};;

SIGSurface.prototype['getMoistureTenHour'] = SIGSurface.prototype.getMoistureTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureTenHour_1(self, moistureUnits);
};;

SIGSurface.prototype['getReactionIntensity'] = SIGSurface.prototype.getReactionIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(reactiontionIntensityUnits) {
  var self = this.ptr;
  if (reactiontionIntensityUnits && typeof reactiontionIntensityUnits === 'object') reactiontionIntensityUnits = reactiontionIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getReactionIntensity_1(self, reactiontionIntensityUnits);
};;

SIGSurface.prototype['getResidenceTime'] = SIGSurface.prototype.getResidenceTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function(timeUnits) {
  var self = this.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getResidenceTime_1(self, timeUnits);
};;

SIGSurface.prototype['getSlope'] = SIGSurface.prototype.getSlope = /** @suppress {undefinedVars, duplicate} @this{Object} */function(slopeUnits) {
  var self = this.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  return _emscripten_bind_SIGSurface_getSlope_1(self, slopeUnits);
};;

SIGSurface.prototype['getSlopeFactor'] = SIGSurface.prototype.getSlopeFactor = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getSlopeFactor_0(self);
};;

SIGSurface.prototype['getSpreadRate'] = SIGSurface.prototype.getSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadRate_1(self, spreadRateUnits);
};;

SIGSurface.prototype['getSpreadRateInDirectionOfInterest'] = SIGSurface.prototype.getSpreadRateInDirectionOfInterest = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1(self, spreadRateUnits);
};;

SIGSurface.prototype['getWindDirection'] = SIGSurface.prototype.getWindDirection = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindDirection_0(self);
};;

SIGSurface.prototype['getWindSpeed'] = SIGSurface.prototype.getWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedUnits, windHeightInputMode) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  return _emscripten_bind_SIGSurface_getWindSpeed_2(self, windSpeedUnits, windHeightInputMode);
};;

SIGSurface.prototype['getFuelModelNumber'] = SIGSurface.prototype.getFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getFuelModelNumber_0(self);
};;

SIGSurface.prototype['setAspect'] = SIGSurface.prototype.setAspect = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspect) {
  var self = this.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  _emscripten_bind_SIGSurface_setAspect_1(self, aspect);
};;

SIGSurface.prototype['setCanopyCover'] = SIGSurface.prototype.setCanopyCover = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyCover, coverUnits) {
  var self = this.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  _emscripten_bind_SIGSurface_setCanopyCover_2(self, canopyCover, coverUnits);
};;

SIGSurface.prototype['setCanopyHeight'] = SIGSurface.prototype.setCanopyHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyHeight, canopyHeightUnits) {
  var self = this.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  _emscripten_bind_SIGSurface_setCanopyHeight_2(self, canopyHeight, canopyHeightUnits);
};;

SIGSurface.prototype['setCrownRatio'] = SIGSurface.prototype.setCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownRatio) {
  var self = this.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  _emscripten_bind_SIGSurface_setCrownRatio_1(self, crownRatio);
};;

SIGSurface.prototype['setFirstFuelModelNumber'] = SIGSurface.prototype.setFirstFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firstFuelModelNumber) {
  var self = this.ptr;
  if (firstFuelModelNumber && typeof firstFuelModelNumber === 'object') firstFuelModelNumber = firstFuelModelNumber.ptr;
  _emscripten_bind_SIGSurface_setFirstFuelModelNumber_1(self, firstFuelModelNumber);
};;

SIGSurface.prototype['setFuelModelNumber'] = SIGSurface.prototype.setFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  _emscripten_bind_SIGSurface_setFuelModelNumber_1(self, fuelModelNumber);
};;

SIGSurface.prototype['setFuelModels'] = SIGSurface.prototype.setFuelModels = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModels) {
  var self = this.ptr;
  if (fuelModels && typeof fuelModels === 'object') fuelModels = fuelModels.ptr;
  _emscripten_bind_SIGSurface_setFuelModels_1(self, fuelModels);
};;

SIGSurface.prototype['setMoistureHundredHour'] = SIGSurface.prototype.setMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureHundredHour, moistureUnits) {
  var self = this.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureHundredHour_2(self, moistureHundredHour, moistureUnits);
};;

SIGSurface.prototype['setMoistureLiveHerbaceous'] = SIGSurface.prototype.setMoistureLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureLiveHerbaceous, moistureUnits) {
  var self = this.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2(self, moistureLiveHerbaceous, moistureUnits);
};;

SIGSurface.prototype['setMoistureLiveWoody'] = SIGSurface.prototype.setMoistureLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureLiveWoody, moistureUnits) {
  var self = this.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureLiveWoody_2(self, moistureLiveWoody, moistureUnits);
};;

SIGSurface.prototype['setMoistureOneHour'] = SIGSurface.prototype.setMoistureOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureOneHour, moistureUnits) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureOneHour_2(self, moistureOneHour, moistureUnits);
};;

SIGSurface.prototype['setMoistureTenHour'] = SIGSurface.prototype.setMoistureTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureTenHour, moistureUnits) {
  var self = this.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureTenHour_2(self, moistureTenHour, moistureUnits);
};;

SIGSurface.prototype['setSecondFuelModelNumber'] = SIGSurface.prototype.setSecondFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function(secondFuelModelNumber) {
  var self = this.ptr;
  if (secondFuelModelNumber && typeof secondFuelModelNumber === 'object') secondFuelModelNumber = secondFuelModelNumber.ptr;
  _emscripten_bind_SIGSurface_setSecondFuelModelNumber_1(self, secondFuelModelNumber);
};;

SIGSurface.prototype['setSlope'] = SIGSurface.prototype.setSlope = /** @suppress {undefinedVars, duplicate} @this{Object} */function(slope, slopeUnits) {
  var self = this.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  _emscripten_bind_SIGSurface_setSlope_2(self, slope, slopeUnits);
};;

SIGSurface.prototype['setTwoFuelModelsFirstFuelModelCoverage'] = SIGSurface.prototype.setTwoFuelModelsFirstFuelModelCoverage = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firstFuelModelCoverage, coverUnits) {
  var self = this.ptr;
  if (firstFuelModelCoverage && typeof firstFuelModelCoverage === 'object') firstFuelModelCoverage = firstFuelModelCoverage.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  _emscripten_bind_SIGSurface_setTwoFuelModelsFirstFuelModelCoverage_2(self, firstFuelModelCoverage, coverUnits);
};;

SIGSurface.prototype['setTwoFuelModelsMethod'] = SIGSurface.prototype.setTwoFuelModelsMethod = /** @suppress {undefinedVars, duplicate} @this{Object} */function(twoFuelModelsMethod) {
  var self = this.ptr;
  if (twoFuelModelsMethod && typeof twoFuelModelsMethod === 'object') twoFuelModelsMethod = twoFuelModelsMethod.ptr;
  _emscripten_bind_SIGSurface_setTwoFuelModelsMethod_1(self, twoFuelModelsMethod);
};;

SIGSurface.prototype['setUserProvidedWindAdjustmentFactor'] = SIGSurface.prototype.setUserProvidedWindAdjustmentFactor = /** @suppress {undefinedVars, duplicate} @this{Object} */function(userProvidedWindAdjustmentFactor) {
  var self = this.ptr;
  if (userProvidedWindAdjustmentFactor && typeof userProvidedWindAdjustmentFactor === 'object') userProvidedWindAdjustmentFactor = userProvidedWindAdjustmentFactor.ptr;
  _emscripten_bind_SIGSurface_setUserProvidedWindAdjustmentFactor_1(self, userProvidedWindAdjustmentFactor);
};;

SIGSurface.prototype['setWindAdjustmentFactorCalculationMethod'] = SIGSurface.prototype.setWindAdjustmentFactorCalculationMethod = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windAdjustmentFactorCalculationMethod) {
  var self = this.ptr;
  if (windAdjustmentFactorCalculationMethod && typeof windAdjustmentFactorCalculationMethod === 'object') windAdjustmentFactorCalculationMethod = windAdjustmentFactorCalculationMethod.ptr;
  _emscripten_bind_SIGSurface_setWindAdjustmentFactorCalculationMethod_1(self, windAdjustmentFactorCalculationMethod);
};;

SIGSurface.prototype['setWindAndSpreadOrientationMode'] = SIGSurface.prototype.setWindAndSpreadOrientationMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windAndSpreadOrientationMode) {
  var self = this.ptr;
  if (windAndSpreadOrientationMode && typeof windAndSpreadOrientationMode === 'object') windAndSpreadOrientationMode = windAndSpreadOrientationMode.ptr;
  _emscripten_bind_SIGSurface_setWindAndSpreadOrientationMode_1(self, windAndSpreadOrientationMode);
};;

SIGSurface.prototype['setWindDirection'] = SIGSurface.prototype.setWindDirection = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windDirection) {
  var self = this.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  _emscripten_bind_SIGSurface_setWindDirection_1(self, windDirection);
};;

SIGSurface.prototype['setWindHeightInputMode'] = SIGSurface.prototype.setWindHeightInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windHeightInputMode) {
  var self = this.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGSurface_setWindHeightInputMode_1(self, windHeightInputMode);
};;

SIGSurface.prototype['setWindSpeed'] = SIGSurface.prototype.setWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeed, windSpeedUnits, windHeightInputMode) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGSurface_setWindSpeed_3(self, windSpeed, windSpeedUnits, windHeightInputMode);
};;

SIGSurface.prototype['updateSurfaceInputs'] = SIGSurface.prototype.updateSurfaceInputs = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  if (windAndSpreadOrientationMode && typeof windAndSpreadOrientationMode === 'object') windAndSpreadOrientationMode = windAndSpreadOrientationMode.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  _emscripten_bind_SIGSurface_updateSurfaceInputs_20(self, fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
};;

SIGSurface.prototype['updateSurfaceInputsForPalmettoGallbery'] = SIGSurface.prototype.updateSurfaceInputsForPalmettoGallbery = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  if (windAndSpreadOrientationMode && typeof windAndSpreadOrientationMode === 'object') windAndSpreadOrientationMode = windAndSpreadOrientationMode.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  if (overstoryBasalArea && typeof overstoryBasalArea === 'object') overstoryBasalArea = overstoryBasalArea.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  _emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_23(self, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
};;

SIGSurface.prototype['updateSurfaceInputsForTwoFuelModels'] = SIGSurface.prototype.updateSurfaceInputsForTwoFuelModels = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firstfuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope, slopeUnits, aspect, canopyCover, canopyCoverUnits, canopyHeight, canopyHeightUnits, crownRatio) {
  var self = this.ptr;
  if (firstfuelModelNumber && typeof firstfuelModelNumber === 'object') firstfuelModelNumber = firstfuelModelNumber.ptr;
  if (secondFuelModelNumber && typeof secondFuelModelNumber === 'object') secondFuelModelNumber = secondFuelModelNumber.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  if (windAndSpreadOrientationMode && typeof windAndSpreadOrientationMode === 'object') windAndSpreadOrientationMode = windAndSpreadOrientationMode.ptr;
  if (firstFuelModelCoverage && typeof firstFuelModelCoverage === 'object') firstFuelModelCoverage = firstFuelModelCoverage.ptr;
  if (firstFuelModelCoverageUnits && typeof firstFuelModelCoverageUnits === 'object') firstFuelModelCoverageUnits = firstFuelModelCoverageUnits.ptr;
  if (twoFuelModelsMethod && typeof twoFuelModelsMethod === 'object') twoFuelModelsMethod = twoFuelModelsMethod.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (canopyCoverUnits && typeof canopyCoverUnits === 'object') canopyCoverUnits = canopyCoverUnits.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  _emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_24(self, firstfuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope, slopeUnits, aspect, canopyCover, canopyCoverUnits, canopyHeight, canopyHeightUnits, crownRatio);
};;

SIGSurface.prototype['updateSurfaceInputsForWesternAspen'] = SIGSurface.prototype.updateSurfaceInputsForWesternAspen = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber, aspenCuringLevel, aspenFireSeverity, DBH, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  if (aspenCuringLevel && typeof aspenCuringLevel === 'object') aspenCuringLevel = aspenCuringLevel.ptr;
  if (aspenFireSeverity && typeof aspenFireSeverity === 'object') aspenFireSeverity = aspenFireSeverity.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  if (windAndSpreadOrientationMode && typeof windAndSpreadOrientationMode === 'object') windAndSpreadOrientationMode = windAndSpreadOrientationMode.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  _emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_23(self, aspenFuelModelNumber, aspenCuringLevel, aspenFireSeverity, DBH, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
};;

  SIGSurface.prototype['__destroy__'] = SIGSurface.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface___destroy___0(self);
};
// PalmettoGallberry
/** @suppress {undefinedVars, duplicate} @this{Object} */function PalmettoGallberry() {
  this.ptr = _emscripten_bind_PalmettoGallberry_PalmettoGallberry_0();
  getCache(PalmettoGallberry)[this.ptr] = this;
};;
PalmettoGallberry.prototype = Object.create(WrapperObject.prototype);
PalmettoGallberry.prototype.constructor = PalmettoGallberry;
PalmettoGallberry.prototype.__class__ = PalmettoGallberry;
PalmettoGallberry.__cache__ = {};
Module['PalmettoGallberry'] = PalmettoGallberry;

PalmettoGallberry.prototype['initializeMembers'] = PalmettoGallberry.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_PalmettoGallberry_initializeMembers_0(self);
};;

PalmettoGallberry.prototype['getHeatOfCombustionLive'] = PalmettoGallberry.prototype.getHeatOfCombustionLive = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getHeatOfCombustionLive_0(self);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyLitterLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLitterLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, overstoryBasalArea) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (overstoryBasalArea && typeof overstoryBasalArea === 'object') overstoryBasalArea = overstoryBasalArea.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLitterLoad_2(self, ageOfRough, overstoryBasalArea);
};;

PalmettoGallberry.prototype['getPalmettoGallberyLiveOneHourLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLiveOneHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveOneHourLoad_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyDeadFoliageLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyDeadFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFoliageLoad_0(self);
};;

PalmettoGallberry.prototype['getHeatOfCombustionDead'] = PalmettoGallberry.prototype.getHeatOfCombustionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getHeatOfCombustionDead_0(self);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyLiveFoliageLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLiveFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, palmettoCoverage, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFoliageLoad_3(self, ageOfRough, palmettoCoverage, heightOfUnderstory);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyLiveTenHourLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLiveTenHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveTenHourLoad_2(self, ageOfRough, heightOfUnderstory);
};;

PalmettoGallberry.prototype['getPalmettoGallberyDeadTenHourLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyDeadTenHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadTenHourLoad_0(self);
};;

PalmettoGallberry.prototype['getMoistureOfExtinctionDead'] = PalmettoGallberry.prototype.getMoistureOfExtinctionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getMoistureOfExtinctionDead_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyLiveFoliageLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLiveFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFoliageLoad_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyLitterLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLitterLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLitterLoad_0(self);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyDeadTenHourLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyDeadTenHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, palmettoCoverage) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadTenHourLoad_2(self, ageOfRough, palmettoCoverage);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyLiveOneHourLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLiveOneHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveOneHourLoad_2(self, ageOfRough, heightOfUnderstory);
};;

PalmettoGallberry.prototype['getPalmettoGallberyFuelBedDepth'] = PalmettoGallberry.prototype.getPalmettoGallberyFuelBedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyFuelBedDepth_0(self);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyDeadFoliageLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyDeadFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, palmettoCoverage) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFoliageLoad_2(self, ageOfRough, palmettoCoverage);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyDeadOneHourLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyDeadOneHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadOneHourLoad_2(self, ageOfRough, heightOfUnderstory);
};;

PalmettoGallberry.prototype['getPalmettoGallberyLiveTenHourLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLiveTenHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveTenHourLoad_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyDeadOneHourLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyDeadOneHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadOneHourLoad_0(self);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyFuelBedDepth'] = PalmettoGallberry.prototype.calculatePalmettoGallberyFuelBedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heightOfUnderstory) {
  var self = this.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyFuelBedDepth_1(self, heightOfUnderstory);
};;

  PalmettoGallberry.prototype['__destroy__'] = PalmettoGallberry.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_PalmettoGallberry___destroy___0(self);
};
// WesternAspen
/** @suppress {undefinedVars, duplicate} @this{Object} */function WesternAspen() {
  this.ptr = _emscripten_bind_WesternAspen_WesternAspen_0();
  getCache(WesternAspen)[this.ptr] = this;
};;
WesternAspen.prototype = Object.create(WrapperObject.prototype);
WesternAspen.prototype.constructor = WesternAspen;
WesternAspen.prototype.__class__ = WesternAspen;
WesternAspen.__cache__ = {};
Module['WesternAspen'] = WesternAspen;

WesternAspen.prototype['initializeMembers'] = WesternAspen.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_WesternAspen_initializeMembers_0(self);
};;

WesternAspen.prototype['calculateAspenMortality'] = WesternAspen.prototype.calculateAspenMortality = /** @suppress {undefinedVars, duplicate} @this{Object} */function(severity, flameLength, DBH) {
  var self = this.ptr;
  if (severity && typeof severity === 'object') severity = severity.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  return _emscripten_bind_WesternAspen_calculateAspenMortality_3(self, severity, flameLength, DBH);
};;

WesternAspen.prototype['getAspenDBH'] = WesternAspen.prototype.getAspenDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenDBH_0(self);
};;

WesternAspen.prototype['getAspenFuelBedDepth'] = WesternAspen.prototype.getAspenFuelBedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(typeIndex) {
  var self = this.ptr;
  if (typeIndex && typeof typeIndex === 'object') typeIndex = typeIndex.ptr;
  return _emscripten_bind_WesternAspen_getAspenFuelBedDepth_1(self, typeIndex);
};;

WesternAspen.prototype['getAspenHeatOfCombustionDead'] = WesternAspen.prototype.getAspenHeatOfCombustionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenHeatOfCombustionDead_0(self);
};;

WesternAspen.prototype['getAspenHeatOfCombustionLive'] = WesternAspen.prototype.getAspenHeatOfCombustionLive = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenHeatOfCombustionLive_0(self);
};;

WesternAspen.prototype['getAspenLoadDeadOneHour'] = WesternAspen.prototype.getAspenLoadDeadOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber, aspenCuringLevel) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  if (aspenCuringLevel && typeof aspenCuringLevel === 'object') aspenCuringLevel = aspenCuringLevel.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_2(self, aspenFuelModelNumber, aspenCuringLevel);
};;

WesternAspen.prototype['getAspenLoadDeadTenHour'] = WesternAspen.prototype.getAspenLoadDeadTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_1(self, aspenFuelModelNumber);
};;

WesternAspen.prototype['getAspenLoadLiveHerbaceous'] = WesternAspen.prototype.getAspenLoadLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber, aspenCuringLevel) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  if (aspenCuringLevel && typeof aspenCuringLevel === 'object') aspenCuringLevel = aspenCuringLevel.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_2(self, aspenFuelModelNumber, aspenCuringLevel);
};;

WesternAspen.prototype['getAspenLoadLiveWoody'] = WesternAspen.prototype.getAspenLoadLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber, aspenCuringLevel) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  if (aspenCuringLevel && typeof aspenCuringLevel === 'object') aspenCuringLevel = aspenCuringLevel.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadLiveWoody_2(self, aspenFuelModelNumber, aspenCuringLevel);
};;

WesternAspen.prototype['getAspenMoistureOfExtinctionDead'] = WesternAspen.prototype.getAspenMoistureOfExtinctionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0(self);
};;

WesternAspen.prototype['getAspenMortality'] = WesternAspen.prototype.getAspenMortality = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenMortality_0(self);
};;

WesternAspen.prototype['getAspenSavrDeadOneHour'] = WesternAspen.prototype.getAspenSavrDeadOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber, aspenCuringLevel) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  if (aspenCuringLevel && typeof aspenCuringLevel === 'object') aspenCuringLevel = aspenCuringLevel.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_2(self, aspenFuelModelNumber, aspenCuringLevel);
};;

WesternAspen.prototype['getAspenSavrDeadTenHour'] = WesternAspen.prototype.getAspenSavrDeadTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0(self);
};;

WesternAspen.prototype['getAspenSavrLiveHerbaceous'] = WesternAspen.prototype.getAspenSavrLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0(self);
};;

WesternAspen.prototype['getAspenSavrLiveWoody'] = WesternAspen.prototype.getAspenSavrLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber, aspenCuringLevel) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  if (aspenCuringLevel && typeof aspenCuringLevel === 'object') aspenCuringLevel = aspenCuringLevel.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrLiveWoody_2(self, aspenFuelModelNumber, aspenCuringLevel);
};;

  WesternAspen.prototype['__destroy__'] = WesternAspen.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_WesternAspen___destroy___0(self);
};
// SIGCrown
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGCrown() { throw "cannot construct a SIGCrown, no constructor in IDL" }
SIGCrown.prototype = Object.create(WrapperObject.prototype);
SIGCrown.prototype.constructor = SIGCrown;
SIGCrown.prototype.__class__ = SIGCrown;
SIGCrown.__cache__ = {};
Module['SIGCrown'] = SIGCrown;

SIGCrown.prototype['initializeMembers'] = SIGCrown.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_initializeMembers_0(self);
};;

SIGCrown.prototype['doCrownRunRothermel'] = SIGCrown.prototype.doCrownRunRothermel = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_doCrownRunRothermel_0(self);
};;

SIGCrown.prototype['doCrownRunScottAndReinhardt'] = SIGCrown.prototype.doCrownRunScottAndReinhardt = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0(self);
};;

SIGCrown.prototype['getFireType'] = SIGCrown.prototype.getFireType = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getFireType_0(self);
};;

SIGCrown.prototype['getAspect'] = SIGCrown.prototype.getAspect = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getAspect_0(self);
};;

SIGCrown.prototype['getCanopyBaseHeight'] = SIGCrown.prototype.getCanopyBaseHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyHeightUnits) {
  var self = this.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  return _emscripten_bind_SIGCrown_getCanopyBaseHeight_1(self, canopyHeightUnits);
};;

SIGCrown.prototype['getCanopyBulkDensity'] = SIGCrown.prototype.getCanopyBulkDensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyBulkDensityUnits) {
  var self = this.ptr;
  if (canopyBulkDensityUnits && typeof canopyBulkDensityUnits === 'object') canopyBulkDensityUnits = canopyBulkDensityUnits.ptr;
  return _emscripten_bind_SIGCrown_getCanopyBulkDensity_1(self, canopyBulkDensityUnits);
};;

SIGCrown.prototype['getCanopyCover'] = SIGCrown.prototype.getCanopyCover = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyCoverUnits) {
  var self = this.ptr;
  if (canopyCoverUnits && typeof canopyCoverUnits === 'object') canopyCoverUnits = canopyCoverUnits.ptr;
  return _emscripten_bind_SIGCrown_getCanopyCover_1(self, canopyCoverUnits);
};;

SIGCrown.prototype['getCanopyHeight'] = SIGCrown.prototype.getCanopyHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyHeighUnits) {
  var self = this.ptr;
  if (canopyHeighUnits && typeof canopyHeighUnits === 'object') canopyHeighUnits = canopyHeighUnits.ptr;
  return _emscripten_bind_SIGCrown_getCanopyHeight_1(self, canopyHeighUnits);
};;

SIGCrown.prototype['getCriticalOpenWindSpeed'] = SIGCrown.prototype.getCriticalOpenWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speedUnits) {
  var self = this.ptr;
  if (speedUnits && typeof speedUnits === 'object') speedUnits = speedUnits.ptr;
  return _emscripten_bind_SIGCrown_getCriticalOpenWindSpeed_1(self, speedUnits);
};;

SIGCrown.prototype['getCrownFireLengthToWidthRatio'] = SIGCrown.prototype.getCrownFireLengthToWidthRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireLengthToWidthRatio_0(self);
};;

SIGCrown.prototype['getCrownFireSpreadRate'] = SIGCrown.prototype.getCrownFireSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireSpreadRate_1(self, spreadRateUnits);
};;

SIGCrown.prototype['getCrownFirelineIntensity'] = SIGCrown.prototype.getCrownFirelineIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFirelineIntensity_1(self, firelineIntensityUnits);
};;

SIGCrown.prototype['getCrownFlameLength'] = SIGCrown.prototype.getCrownFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFlameLength_1(self, flameLengthUnits);
};;

SIGCrown.prototype['getCrownRatio'] = SIGCrown.prototype.getCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownRatio_0(self);
};;

SIGCrown.prototype['getFinalFirelineIntesity'] = SIGCrown.prototype.getFinalFirelineIntesity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalFirelineIntesity_1(self, firelineIntensityUnits);
};;

SIGCrown.prototype['getFinalFlameLength'] = SIGCrown.prototype.getFinalFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalFlameLength_1(self, flameLengthUnits);
};;

SIGCrown.prototype['getFinalHeatPerUnitArea'] = SIGCrown.prototype.getFinalHeatPerUnitArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heatPerUnitAreaUnits) {
  var self = this.ptr;
  if (heatPerUnitAreaUnits && typeof heatPerUnitAreaUnits === 'object') heatPerUnitAreaUnits = heatPerUnitAreaUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalHeatPerUnitArea_1(self, heatPerUnitAreaUnits);
};;

SIGCrown.prototype['getFinalSpreadRate'] = SIGCrown.prototype.getFinalSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalSpreadRate_1(self, spreadRateUnits);
};;

SIGCrown.prototype['getMoistureFoliar'] = SIGCrown.prototype.getMoistureFoliar = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureFoliar_1(self, moistureUnits);
};;

SIGCrown.prototype['getMoistureHundredHour'] = SIGCrown.prototype.getMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureHundredHour_1(self, moistureUnits);
};;

SIGCrown.prototype['getMoistureLiveHerbaceous'] = SIGCrown.prototype.getMoistureLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureLiveHerbaceous_1(self, moistureUnits);
};;

SIGCrown.prototype['getMoistureLiveWoody'] = SIGCrown.prototype.getMoistureLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureLiveWoody_1(self, moistureUnits);
};;

SIGCrown.prototype['getMoistureOneHour'] = SIGCrown.prototype.getMoistureOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureOneHour_1(self, moistureUnits);
};;

SIGCrown.prototype['getMoistureTenHour'] = SIGCrown.prototype.getMoistureTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureTenHour_1(self, moistureUnits);
};;

SIGCrown.prototype['getSlope'] = SIGCrown.prototype.getSlope = /** @suppress {undefinedVars, duplicate} @this{Object} */function(slopeUnits) {
  var self = this.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  return _emscripten_bind_SIGCrown_getSlope_1(self, slopeUnits);
};;

SIGCrown.prototype['getSurfaceFireSpreadRate'] = SIGCrown.prototype.getSurfaceFireSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGCrown_getSurfaceFireSpreadRate_1(self, spreadRateUnits);
};;

SIGCrown.prototype['getWindDirection'] = SIGCrown.prototype.getWindDirection = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getWindDirection_0(self);
};;

SIGCrown.prototype['getWindSpeed'] = SIGCrown.prototype.getWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedUnits, windHeightInputMode) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  return _emscripten_bind_SIGCrown_getWindSpeed_2(self, windSpeedUnits, windHeightInputMode);
};;

SIGCrown.prototype['getFuelModelNumber'] = SIGCrown.prototype.getFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getFuelModelNumber_0(self);
};;

SIGCrown.prototype['setAspect'] = SIGCrown.prototype.setAspect = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspect) {
  var self = this.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  _emscripten_bind_SIGCrown_setAspect_1(self, aspect);
};;

SIGCrown.prototype['setCanopyBaseHeight'] = SIGCrown.prototype.setCanopyBaseHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyBaseHeight, canopyHeightUnits) {
  var self = this.ptr;
  if (canopyBaseHeight && typeof canopyBaseHeight === 'object') canopyBaseHeight = canopyBaseHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  _emscripten_bind_SIGCrown_setCanopyBaseHeight_2(self, canopyBaseHeight, canopyHeightUnits);
};;

SIGCrown.prototype['setCanopyBulkDensity'] = SIGCrown.prototype.setCanopyBulkDensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyBulkDensity, densityUnits) {
  var self = this.ptr;
  if (canopyBulkDensity && typeof canopyBulkDensity === 'object') canopyBulkDensity = canopyBulkDensity.ptr;
  if (densityUnits && typeof densityUnits === 'object') densityUnits = densityUnits.ptr;
  _emscripten_bind_SIGCrown_setCanopyBulkDensity_2(self, canopyBulkDensity, densityUnits);
};;

SIGCrown.prototype['setCanopyCover'] = SIGCrown.prototype.setCanopyCover = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyCover, coverUnits) {
  var self = this.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  _emscripten_bind_SIGCrown_setCanopyCover_2(self, canopyCover, coverUnits);
};;

SIGCrown.prototype['setCanopyHeight'] = SIGCrown.prototype.setCanopyHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyHeight, canopyHeightUnits) {
  var self = this.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  _emscripten_bind_SIGCrown_setCanopyHeight_2(self, canopyHeight, canopyHeightUnits);
};;

SIGCrown.prototype['setCrownRatio'] = SIGCrown.prototype.setCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownRatio) {
  var self = this.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  _emscripten_bind_SIGCrown_setCrownRatio_1(self, crownRatio);
};;

SIGCrown.prototype['setFuelModelNumber'] = SIGCrown.prototype.setFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  _emscripten_bind_SIGCrown_setFuelModelNumber_1(self, fuelModelNumber);
};;

SIGCrown.prototype['setFuelModels'] = SIGCrown.prototype.setFuelModels = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModels) {
  var self = this.ptr;
  if (fuelModels && typeof fuelModels === 'object') fuelModels = fuelModels.ptr;
  _emscripten_bind_SIGCrown_setFuelModels_1(self, fuelModels);
};;

SIGCrown.prototype['setMoistureFoliar'] = SIGCrown.prototype.setMoistureFoliar = /** @suppress {undefinedVars, duplicate} @this{Object} */function(foliarMoisture, moistureUnits) {
  var self = this.ptr;
  if (foliarMoisture && typeof foliarMoisture === 'object') foliarMoisture = foliarMoisture.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureFoliar_2(self, foliarMoisture, moistureUnits);
};;

SIGCrown.prototype['setMoistureHundredHour'] = SIGCrown.prototype.setMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureHundredHour, moistureUnits) {
  var self = this.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureHundredHour_2(self, moistureHundredHour, moistureUnits);
};;

SIGCrown.prototype['setMoistureLiveHerbaceous'] = SIGCrown.prototype.setMoistureLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureLiveHerbaceous, moistureUnits) {
  var self = this.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureLiveHerbaceous_2(self, moistureLiveHerbaceous, moistureUnits);
};;

SIGCrown.prototype['setMoistureLiveWoody'] = SIGCrown.prototype.setMoistureLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureLiveWoody, moistureUnits) {
  var self = this.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureLiveWoody_2(self, moistureLiveWoody, moistureUnits);
};;

SIGCrown.prototype['setMoistureOneHour'] = SIGCrown.prototype.setMoistureOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureOneHour, moistureUnits) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureOneHour_2(self, moistureOneHour, moistureUnits);
};;

SIGCrown.prototype['setMoistureTenHour'] = SIGCrown.prototype.setMoistureTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureTenHour, moistureUnits) {
  var self = this.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureTenHour_2(self, moistureTenHour, moistureUnits);
};;

SIGCrown.prototype['setSlope'] = SIGCrown.prototype.setSlope = /** @suppress {undefinedVars, duplicate} @this{Object} */function(slope, slopeUnits) {
  var self = this.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  _emscripten_bind_SIGCrown_setSlope_2(self, slope, slopeUnits);
};;

SIGCrown.prototype['setUserProvidedWindAdjustmentFactor'] = SIGCrown.prototype.setUserProvidedWindAdjustmentFactor = /** @suppress {undefinedVars, duplicate} @this{Object} */function(userProvidedWindAdjustmentFactor) {
  var self = this.ptr;
  if (userProvidedWindAdjustmentFactor && typeof userProvidedWindAdjustmentFactor === 'object') userProvidedWindAdjustmentFactor = userProvidedWindAdjustmentFactor.ptr;
  _emscripten_bind_SIGCrown_setUserProvidedWindAdjustmentFactor_1(self, userProvidedWindAdjustmentFactor);
};;

SIGCrown.prototype['setWindAdjustmentFactorCalculationMethod'] = SIGCrown.prototype.setWindAdjustmentFactorCalculationMethod = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windAdjustmentFactorCalculationMethod) {
  var self = this.ptr;
  if (windAdjustmentFactorCalculationMethod && typeof windAdjustmentFactorCalculationMethod === 'object') windAdjustmentFactorCalculationMethod = windAdjustmentFactorCalculationMethod.ptr;
  _emscripten_bind_SIGCrown_setWindAdjustmentFactorCalculationMethod_1(self, windAdjustmentFactorCalculationMethod);
};;

SIGCrown.prototype['setWindAndSpreadOrientationMode'] = SIGCrown.prototype.setWindAndSpreadOrientationMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windAndSpreadAngleMode) {
  var self = this.ptr;
  if (windAndSpreadAngleMode && typeof windAndSpreadAngleMode === 'object') windAndSpreadAngleMode = windAndSpreadAngleMode.ptr;
  _emscripten_bind_SIGCrown_setWindAndSpreadOrientationMode_1(self, windAndSpreadAngleMode);
};;

SIGCrown.prototype['setWindDirection'] = SIGCrown.prototype.setWindDirection = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windDirection) {
  var self = this.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  _emscripten_bind_SIGCrown_setWindDirection_1(self, windDirection);
};;

SIGCrown.prototype['setWindHeightInputMode'] = SIGCrown.prototype.setWindHeightInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windHeightInputMode) {
  var self = this.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGCrown_setWindHeightInputMode_1(self, windHeightInputMode);
};;

SIGCrown.prototype['setWindSpeed'] = SIGCrown.prototype.setWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeed, windSpeedUnits, windHeightInputMode) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGCrown_setWindSpeed_3(self, windSpeed, windSpeedUnits, windHeightInputMode);
};;

SIGCrown.prototype['updateCrownInputs'] = SIGCrown.prototype.updateCrownInputs = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureFoliar, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyBaseHeight, canopyHeightUnits, crownRatio, canopyBulkDensity, densityUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureFoliar && typeof moistureFoliar === 'object') moistureFoliar = moistureFoliar.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  if (windAndSpreadOrientationMode && typeof windAndSpreadOrientationMode === 'object') windAndSpreadOrientationMode = windAndSpreadOrientationMode.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyBaseHeight && typeof canopyBaseHeight === 'object') canopyBaseHeight = canopyBaseHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  if (canopyBulkDensity && typeof canopyBulkDensity === 'object') canopyBulkDensity = canopyBulkDensity.ptr;
  if (densityUnits && typeof densityUnits === 'object') densityUnits = densityUnits.ptr;
  _emscripten_bind_SIGCrown_updateCrownInputs_24(self, fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureFoliar, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyBaseHeight, canopyHeightUnits, crownRatio, canopyBulkDensity, densityUnits);
};;

SIGCrown.prototype['updateCrownsSurfaceInputs'] = SIGCrown.prototype.updateCrownsSurfaceInputs = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  if (windAndSpreadOrientationMode && typeof windAndSpreadOrientationMode === 'object') windAndSpreadOrientationMode = windAndSpreadOrientationMode.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  _emscripten_bind_SIGCrown_updateCrownsSurfaceInputs_20(self, fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
};;

  SIGCrown.prototype['__destroy__'] = SIGCrown.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown___destroy___0(self);
};
// SpeciesMasterTableRecord
/** @suppress {undefinedVars, duplicate} @this{Object} */function SpeciesMasterTableRecord(rhs) {
  if (rhs && typeof rhs === 'object') rhs = rhs.ptr;
  if (rhs === undefined) { this.ptr = _emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_0(); getCache(SpeciesMasterTableRecord)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_1(rhs);
  getCache(SpeciesMasterTableRecord)[this.ptr] = this;
};;
SpeciesMasterTableRecord.prototype = Object.create(WrapperObject.prototype);
SpeciesMasterTableRecord.prototype.constructor = SpeciesMasterTableRecord;
SpeciesMasterTableRecord.prototype.__class__ = SpeciesMasterTableRecord;
SpeciesMasterTableRecord.__cache__ = {};
Module['SpeciesMasterTableRecord'] = SpeciesMasterTableRecord;

  SpeciesMasterTableRecord.prototype['__destroy__'] = SpeciesMasterTableRecord.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SpeciesMasterTableRecord___destroy___0(self);
};
// SpeciesMasterTable
/** @suppress {undefinedVars, duplicate} @this{Object} */function SpeciesMasterTable() {
  this.ptr = _emscripten_bind_SpeciesMasterTable_SpeciesMasterTable_0();
  getCache(SpeciesMasterTable)[this.ptr] = this;
};;
SpeciesMasterTable.prototype = Object.create(WrapperObject.prototype);
SpeciesMasterTable.prototype.constructor = SpeciesMasterTable;
SpeciesMasterTable.prototype.__class__ = SpeciesMasterTable;
SpeciesMasterTable.__cache__ = {};
Module['SpeciesMasterTable'] = SpeciesMasterTable;

SpeciesMasterTable.prototype['initializeMasterTable'] = SpeciesMasterTable.prototype.initializeMasterTable = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SpeciesMasterTable_initializeMasterTable_0(self);
};;

SpeciesMasterTable.prototype['getSpeciesTableIndexFromSpeciesCode'] = SpeciesMasterTable.prototype.getSpeciesTableIndexFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCode_1(self, speciesCode);
};;

SpeciesMasterTable.prototype['getSpeciesTableIndexFromSpeciesCodeAndEquationType'] = SpeciesMasterTable.prototype.getSpeciesTableIndexFromSpeciesCodeAndEquationType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode, equationType) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return _emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2(self, speciesCode, equationType);
};;

SpeciesMasterTable.prototype['insertRecord'] = SpeciesMasterTable.prototype.insertRecord = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode, scientificName, commonName, mortalityEquation, brkEqu, crownCoefficientCode, region1, region2, region3, region4, equationType, crownDamageEquationCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  if (scientificName && typeof scientificName === 'object') scientificName = scientificName.ptr;
  else scientificName = ensureString(scientificName);
  if (commonName && typeof commonName === 'object') commonName = commonName.ptr;
  else commonName = ensureString(commonName);
  if (mortalityEquation && typeof mortalityEquation === 'object') mortalityEquation = mortalityEquation.ptr;
  if (brkEqu && typeof brkEqu === 'object') brkEqu = brkEqu.ptr;
  if (crownCoefficientCode && typeof crownCoefficientCode === 'object') crownCoefficientCode = crownCoefficientCode.ptr;
  if (region1 && typeof region1 === 'object') region1 = region1.ptr;
  if (region2 && typeof region2 === 'object') region2 = region2.ptr;
  if (region3 && typeof region3 === 'object') region3 = region3.ptr;
  if (region4 && typeof region4 === 'object') region4 = region4.ptr;
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  if (crownDamageEquationCode && typeof crownDamageEquationCode === 'object') crownDamageEquationCode = crownDamageEquationCode.ptr;
  _emscripten_bind_SpeciesMasterTable_insertRecord_12(self, speciesCode, scientificName, commonName, mortalityEquation, brkEqu, crownCoefficientCode, region1, region2, region3, region4, equationType, crownDamageEquationCode);
};;

  SpeciesMasterTable.prototype['__destroy__'] = SpeciesMasterTable.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SpeciesMasterTable___destroy___0(self);
};
// SIGMortality
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGMortality(speciesMasterTable) {
  if (speciesMasterTable && typeof speciesMasterTable === 'object') speciesMasterTable = speciesMasterTable.ptr;
  this.ptr = _emscripten_bind_SIGMortality_SIGMortality_1(speciesMasterTable);
  getCache(SIGMortality)[this.ptr] = this;
};;
SIGMortality.prototype = Object.create(WrapperObject.prototype);
SIGMortality.prototype.constructor = SIGMortality;
SIGMortality.prototype.__class__ = SIGMortality;
SIGMortality.__cache__ = {};
Module['SIGMortality'] = SIGMortality;

SIGMortality.prototype['getBeetleDamage'] = SIGMortality.prototype.getBeetleDamage = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getBeetleDamage_0(self);
};;

SIGMortality.prototype['getCrownDamageEquationCode'] = SIGMortality.prototype.getCrownDamageEquationCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownDamageEquationCode_0(self);
};;

SIGMortality.prototype['getCrownDamageEquationCodeAtSpeciesTableIndex'] = SIGMortality.prototype.getCrownDamageEquationCodeAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getCrownDamageEquationCodeAtSpeciesTableIndex_1(self, index);
};;

SIGMortality.prototype['getCrownDamageEquationCodeFromSpeciesCode'] = SIGMortality.prototype.getCrownDamageEquationCodeFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getCrownDamageEquationCodeFromSpeciesCode_1(self, speciesCode);
};;

SIGMortality.prototype['getCrownDamageType'] = SIGMortality.prototype.getCrownDamageType = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownDamageType_0(self);
};;

SIGMortality.prototype['getEquationType'] = SIGMortality.prototype.getEquationType = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getEquationType_0(self);
};;

SIGMortality.prototype['getEquationTypeAtSpeciesTableIndex'] = SIGMortality.prototype.getEquationTypeAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getEquationTypeAtSpeciesTableIndex_1(self, index);
};;

SIGMortality.prototype['getEquationTypeFromSpeciesCode'] = SIGMortality.prototype.getEquationTypeFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getEquationTypeFromSpeciesCode_1(self, speciesCode);
};;

SIGMortality.prototype['getFireSeverity'] = SIGMortality.prototype.getFireSeverity = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getFireSeverity_0(self);
};;

SIGMortality.prototype['getFlameLengthOrScorchHeightSwitch'] = SIGMortality.prototype.getFlameLengthOrScorchHeightSwitch = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightSwitch_0(self);
};;

SIGMortality.prototype['getRegion'] = SIGMortality.prototype.getRegion = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getRegion_0(self);
};;

SIGMortality.prototype['checkIsInRegionAtSpeciesTableIndex'] = SIGMortality.prototype.checkIsInRegionAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, region) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  return !!(_emscripten_bind_SIGMortality_checkIsInRegionAtSpeciesTableIndex_2(self, index, region));
};;

SIGMortality.prototype['checkIsInRegionFromSpeciesCode'] = SIGMortality.prototype.checkIsInRegionFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode, region) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  if (region && typeof region === 'object') region = region.ptr;
  return !!(_emscripten_bind_SIGMortality_checkIsInRegionFromSpeciesCode_2(self, speciesCode, region));
};;

SIGMortality.prototype['getBarkThickness'] = SIGMortality.prototype.getBarkThickness = /** @suppress {undefinedVars, duplicate} @this{Object} */function(barkThicknessUnits) {
  var self = this.ptr;
  if (barkThicknessUnits && typeof barkThicknessUnits === 'object') barkThicknessUnits = barkThicknessUnits.ptr;
  return _emscripten_bind_SIGMortality_getBarkThickness_1(self, barkThicknessUnits);
};;

SIGMortality.prototype['getBasalAreaKillled'] = SIGMortality.prototype.getBasalAreaKillled = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getBasalAreaKillled_0(self);
};;

SIGMortality.prototype['getBasalAreaPostfire'] = SIGMortality.prototype.getBasalAreaPostfire = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getBasalAreaPostfire_0(self);
};;

SIGMortality.prototype['getBasalAreaPrefire'] = SIGMortality.prototype.getBasalAreaPrefire = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getBasalAreaPrefire_0(self);
};;

SIGMortality.prototype['getBoleCharHeight'] = SIGMortality.prototype.getBoleCharHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(boleCharHeightUnits) {
  var self = this.ptr;
  if (boleCharHeightUnits && typeof boleCharHeightUnits === 'object') boleCharHeightUnits = boleCharHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getBoleCharHeight_1(self, boleCharHeightUnits);
};;

SIGMortality.prototype['getCambiumKillRating'] = SIGMortality.prototype.getCambiumKillRating = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCambiumKillRating_0(self);
};;

SIGMortality.prototype['getCrownDamage'] = SIGMortality.prototype.getCrownDamage = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownDamage_0(self);
};;

SIGMortality.prototype['getCrownRatio'] = SIGMortality.prototype.getCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownRatio_0(self);
};;

SIGMortality.prototype['getDBH'] = SIGMortality.prototype.getDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(diameterUnits) {
  var self = this.ptr;
  if (diameterUnits && typeof diameterUnits === 'object') diameterUnits = diameterUnits.ptr;
  return _emscripten_bind_SIGMortality_getDBH_1(self, diameterUnits);
};;

SIGMortality.prototype['getFlameLengthOrScorchHeightValue'] = SIGMortality.prototype.getFlameLengthOrScorchHeightValue = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthOrScorchHeightUnits) {
  var self = this.ptr;
  if (flameLengthOrScorchHeightUnits && typeof flameLengthOrScorchHeightUnits === 'object') flameLengthOrScorchHeightUnits = flameLengthOrScorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightValue_1(self, flameLengthOrScorchHeightUnits);
};;

SIGMortality.prototype['getKilledTrees'] = SIGMortality.prototype.getKilledTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getKilledTrees_0(self);
};;

SIGMortality.prototype['getProbabilityOfMortality'] = SIGMortality.prototype.getProbabilityOfMortality = /** @suppress {undefinedVars, duplicate} @this{Object} */function(probabilityUnits) {
  var self = this.ptr;
  if (probabilityUnits && typeof probabilityUnits === 'object') probabilityUnits = probabilityUnits.ptr;
  return _emscripten_bind_SIGMortality_getProbabilityOfMortality_1(self, probabilityUnits);
};;

SIGMortality.prototype['getTotalPrefireTrees'] = SIGMortality.prototype.getTotalPrefireTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getTotalPrefireTrees_0(self);
};;

SIGMortality.prototype['getTreeDensityPerUnitArea'] = SIGMortality.prototype.getTreeDensityPerUnitArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeDensityPerUnitArea_1(self, areaUnits);
};;

SIGMortality.prototype['getTreeHeight'] = SIGMortality.prototype.getTreeHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeHeightUnits) {
  var self = this.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeHeight_1(self, treeHeightUnits);
};;

SIGMortality.prototype['postfireCanopyCover'] = SIGMortality.prototype.postfireCanopyCover = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_postfireCanopyCover_0(self);
};;

SIGMortality.prototype['prefireCanopyCover'] = SIGMortality.prototype.prefireCanopyCover = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_prefireCanopyCover_0(self);
};;

SIGMortality.prototype['getBarkEquationNumberAtSpeciesTableIndex'] = SIGMortality.prototype.getBarkEquationNumberAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getBarkEquationNumberAtSpeciesTableIndex_1(self, index);
};;

SIGMortality.prototype['getBarkEquationNumberFromSpeciesCode'] = SIGMortality.prototype.getBarkEquationNumberFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getBarkEquationNumberFromSpeciesCode_1(self, speciesCode);
};;

SIGMortality.prototype['getCrownCoefficientCodeAtSpeciesTableIndex'] = SIGMortality.prototype.getCrownCoefficientCodeAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getCrownCoefficientCodeAtSpeciesTableIndex_1(self, index);
};;

SIGMortality.prototype['getCrownCoefficientCodeFromSpeciesCode'] = SIGMortality.prototype.getCrownCoefficientCodeFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getCrownCoefficientCodeFromSpeciesCode_1(self, speciesCode);
};;

SIGMortality.prototype['getCrownScorchOrBoleCharEquationNumber'] = SIGMortality.prototype.getCrownScorchOrBoleCharEquationNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownScorchOrBoleCharEquationNumber_0(self);
};;

SIGMortality.prototype['getMortalityEquationNumberAtSpeciesTableIndex'] = SIGMortality.prototype.getMortalityEquationNumberAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getMortalityEquationNumberAtSpeciesTableIndex_1(self, index);
};;

SIGMortality.prototype['getMortalityEquationNumberFromSpeciesCode'] = SIGMortality.prototype.getMortalityEquationNumberFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getMortalityEquationNumberFromSpeciesCode_1(self, speciesCode);
};;

SIGMortality.prototype['getNumberOfRecordsInSpeciesTable'] = SIGMortality.prototype.getNumberOfRecordsInSpeciesTable = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getNumberOfRecordsInSpeciesTable_0(self);
};;

SIGMortality.prototype['getSpeciesTableIndexFromSpeciesCode'] = SIGMortality.prototype.getSpeciesTableIndexFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesNameCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesNameCode && typeof speciesNameCode === 'object') speciesNameCode = speciesNameCode.ptr;
  else speciesNameCode = ensureString(speciesNameCode);
  return _emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCode_1(self, speciesNameCode);
};;

SIGMortality.prototype['getSpeciesTableIndexFromSpeciesCodeAndEquationType'] = SIGMortality.prototype.getSpeciesTableIndexFromSpeciesCodeAndEquationType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesNameCode, equationType) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesNameCode && typeof speciesNameCode === 'object') speciesNameCode = speciesNameCode.ptr;
  else speciesNameCode = ensureString(speciesNameCode);
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return _emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2(self, speciesNameCode, equationType);
};;

SIGMortality.prototype['getSpeciesCode'] = SIGMortality.prototype.getSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getSpeciesCode_0(self));
};;

SIGMortality.prototype['getSpeciesRecordVectorForRegion'] = SIGMortality.prototype.getSpeciesRecordVectorForRegion = /** @suppress {undefinedVars, duplicate} @this{Object} */function(region) {
  var self = this.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  return wrapPointer(_emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegion_1(self, region), SpeciesMasterTableRecordVector);
};;

SIGMortality.prototype['getSpeciesRecordVectorForRegionAndEquationType'] = SIGMortality.prototype.getSpeciesRecordVectorForRegionAndEquationType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(region, equationType) {
  var self = this.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return wrapPointer(_emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegionAndEquationType_2(self, region, equationType), SpeciesMasterTableRecordVector);
};;

SIGMortality.prototype['getCommonNameAtSpeciesTableIndex'] = SIGMortality.prototype.getCommonNameAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getCommonNameAtSpeciesTableIndex_1(self, index));
};;

SIGMortality.prototype['getCommonNameFromSpeciesCode'] = SIGMortality.prototype.getCommonNameFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return UTF8ToString(_emscripten_bind_SIGMortality_getCommonNameFromSpeciesCode_1(self, speciesCode));
};;

SIGMortality.prototype['getScientificNameAtSpeciesTableIndex'] = SIGMortality.prototype.getScientificNameAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getScientificNameAtSpeciesTableIndex_1(self, index));
};;

SIGMortality.prototype['getScientificNameFromSpeciesCode'] = SIGMortality.prototype.getScientificNameFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return UTF8ToString(_emscripten_bind_SIGMortality_getScientificNameFromSpeciesCode_1(self, speciesCode));
};;

SIGMortality.prototype['getSpeciesCodeAtSpeciesTableIndex'] = SIGMortality.prototype.getSpeciesCodeAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getSpeciesCodeAtSpeciesTableIndex_1(self, index));
};;

SIGMortality.prototype['getRequiredFieldVector'] = SIGMortality.prototype.getRequiredFieldVector = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGMortality_getRequiredFieldVector_0(self), BoolVector);
};;

SIGMortality.prototype['calculateMortality'] = SIGMortality.prototype.calculateMortality = /** @suppress {undefinedVars, duplicate} @this{Object} */function(probablityUnits) {
  var self = this.ptr;
  if (probablityUnits && typeof probablityUnits === 'object') probablityUnits = probablityUnits.ptr;
  return _emscripten_bind_SIGMortality_calculateMortality_1(self, probablityUnits);
};;

SIGMortality.prototype['setBeetleDamage'] = SIGMortality.prototype.setBeetleDamage = /** @suppress {undefinedVars, duplicate} @this{Object} */function(beetleDamage) {
  var self = this.ptr;
  if (beetleDamage && typeof beetleDamage === 'object') beetleDamage = beetleDamage.ptr;
  _emscripten_bind_SIGMortality_setBeetleDamage_1(self, beetleDamage);
};;

SIGMortality.prototype['setBoleCharHeight'] = SIGMortality.prototype.setBoleCharHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(boleCharHeight, boleCharHeightUnits) {
  var self = this.ptr;
  if (boleCharHeight && typeof boleCharHeight === 'object') boleCharHeight = boleCharHeight.ptr;
  if (boleCharHeightUnits && typeof boleCharHeightUnits === 'object') boleCharHeightUnits = boleCharHeightUnits.ptr;
  _emscripten_bind_SIGMortality_setBoleCharHeight_2(self, boleCharHeight, boleCharHeightUnits);
};;

SIGMortality.prototype['setCambiumKillRating'] = SIGMortality.prototype.setCambiumKillRating = /** @suppress {undefinedVars, duplicate} @this{Object} */function(cambiumKillRating) {
  var self = this.ptr;
  if (cambiumKillRating && typeof cambiumKillRating === 'object') cambiumKillRating = cambiumKillRating.ptr;
  _emscripten_bind_SIGMortality_setCambiumKillRating_1(self, cambiumKillRating);
};;

SIGMortality.prototype['setCrownDamage'] = SIGMortality.prototype.setCrownDamage = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownDamage) {
  var self = this.ptr;
  if (crownDamage && typeof crownDamage === 'object') crownDamage = crownDamage.ptr;
  _emscripten_bind_SIGMortality_setCrownDamage_1(self, crownDamage);
};;

SIGMortality.prototype['setCrownRatio'] = SIGMortality.prototype.setCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownRatio) {
  var self = this.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  _emscripten_bind_SIGMortality_setCrownRatio_1(self, crownRatio);
};;

SIGMortality.prototype['setDBH'] = SIGMortality.prototype.setDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(dbh, diameterUnits) {
  var self = this.ptr;
  if (dbh && typeof dbh === 'object') dbh = dbh.ptr;
  if (diameterUnits && typeof diameterUnits === 'object') diameterUnits = diameterUnits.ptr;
  _emscripten_bind_SIGMortality_setDBH_2(self, dbh, diameterUnits);
};;

SIGMortality.prototype['setEquationType'] = SIGMortality.prototype.setEquationType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(equationType) {
  var self = this.ptr;
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  _emscripten_bind_SIGMortality_setEquationType_1(self, equationType);
};;

SIGMortality.prototype['setFireSeverity'] = SIGMortality.prototype.setFireSeverity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fireSeverity) {
  var self = this.ptr;
  if (fireSeverity && typeof fireSeverity === 'object') fireSeverity = fireSeverity.ptr;
  _emscripten_bind_SIGMortality_setFireSeverity_1(self, fireSeverity);
};;

SIGMortality.prototype['setFlameLengthOrScorchHeightSwitch'] = SIGMortality.prototype.setFlameLengthOrScorchHeightSwitch = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthOrScorchHeightSwitch) {
  var self = this.ptr;
  if (flameLengthOrScorchHeightSwitch && typeof flameLengthOrScorchHeightSwitch === 'object') flameLengthOrScorchHeightSwitch = flameLengthOrScorchHeightSwitch.ptr;
  _emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightSwitch_1(self, flameLengthOrScorchHeightSwitch);
};;

SIGMortality.prototype['setFlameLengthOrScorchHeightValue'] = SIGMortality.prototype.setFlameLengthOrScorchHeightValue = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthOrScorchHeightValue, flameLengthOrScorchHeightUnits) {
  var self = this.ptr;
  if (flameLengthOrScorchHeightValue && typeof flameLengthOrScorchHeightValue === 'object') flameLengthOrScorchHeightValue = flameLengthOrScorchHeightValue.ptr;
  if (flameLengthOrScorchHeightUnits && typeof flameLengthOrScorchHeightUnits === 'object') flameLengthOrScorchHeightUnits = flameLengthOrScorchHeightUnits.ptr;
  _emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightValue_2(self, flameLengthOrScorchHeightValue, flameLengthOrScorchHeightUnits);
};;

SIGMortality.prototype['setRegion'] = SIGMortality.prototype.setRegion = /** @suppress {undefinedVars, duplicate} @this{Object} */function(region) {
  var self = this.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  _emscripten_bind_SIGMortality_setRegion_1(self, region);
};;

SIGMortality.prototype['setSpeciesCode'] = SIGMortality.prototype.setSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  _emscripten_bind_SIGMortality_setSpeciesCode_1(self, speciesCode);
};;

SIGMortality.prototype['setTreeDensityPerUnitArea'] = SIGMortality.prototype.setTreeDensityPerUnitArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(numberOfTrees, areaUnits) {
  var self = this.ptr;
  if (numberOfTrees && typeof numberOfTrees === 'object') numberOfTrees = numberOfTrees.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  _emscripten_bind_SIGMortality_setTreeDensityPerUnitArea_2(self, numberOfTrees, areaUnits);
};;

SIGMortality.prototype['setTreeHeight'] = SIGMortality.prototype.setTreeHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeHeight, treeHeightUnits) {
  var self = this.ptr;
  if (treeHeight && typeof treeHeight === 'object') treeHeight = treeHeight.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  _emscripten_bind_SIGMortality_setTreeHeight_2(self, treeHeight, treeHeightUnits);
};;

SIGMortality.prototype['updateInputsForSpeciesCodeAndEquationType'] = SIGMortality.prototype.updateInputsForSpeciesCodeAndEquationType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode, equationType) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return !!(_emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2(self, speciesCode, equationType));
};;

  SIGMortality.prototype['__destroy__'] = SIGMortality.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGMortality___destroy___0(self);
};
// WindSpeedUtility
/** @suppress {undefinedVars, duplicate} @this{Object} */function WindSpeedUtility() {
  this.ptr = _emscripten_bind_WindSpeedUtility_WindSpeedUtility_0();
  getCache(WindSpeedUtility)[this.ptr] = this;
};;
WindSpeedUtility.prototype = Object.create(WrapperObject.prototype);
WindSpeedUtility.prototype.constructor = WindSpeedUtility;
WindSpeedUtility.prototype.__class__ = WindSpeedUtility;
WindSpeedUtility.__cache__ = {};
Module['WindSpeedUtility'] = WindSpeedUtility;

WindSpeedUtility.prototype['windSpeedAtMidflame'] = WindSpeedUtility.prototype.windSpeedAtMidflame = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedAtTwentyFeet, windAdjustmentFactor) {
  var self = this.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windAdjustmentFactor && typeof windAdjustmentFactor === 'object') windAdjustmentFactor = windAdjustmentFactor.ptr;
  return _emscripten_bind_WindSpeedUtility_windSpeedAtMidflame_2(self, windSpeedAtTwentyFeet, windAdjustmentFactor);
};;

WindSpeedUtility.prototype['windSpeedAtTwentyFeetFromTenMeter'] = WindSpeedUtility.prototype.windSpeedAtTwentyFeetFromTenMeter = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedAtTenMeters) {
  var self = this.ptr;
  if (windSpeedAtTenMeters && typeof windSpeedAtTenMeters === 'object') windSpeedAtTenMeters = windSpeedAtTenMeters.ptr;
  return _emscripten_bind_WindSpeedUtility_windSpeedAtTwentyFeetFromTenMeter_1(self, windSpeedAtTenMeters);
};;

  WindSpeedUtility.prototype['__destroy__'] = WindSpeedUtility.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_WindSpeedUtility___destroy___0(self);
};
(function() {
  function setupEnums() {
    

    // AreaUnits_AreaUnitsEnum

    Module['SquareFeet'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareFeet();

    Module['Acres'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_Acres();

    Module['Hectares'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_Hectares();

    Module['SquareMeters'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMeters();

    Module['SquareMiles'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMiles();

    Module['SquareKilometers'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareKilometers();

    

    // LengthUnits_LengthUnitsEnum

    Module['Feet'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Feet();

    Module['Inches'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Inches();

    Module['Centimeters'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Centimeters();

    Module['Meters'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Meters();

    Module['Chains'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Chains();

    Module['Miles'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Miles();

    Module['Kilometers'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Kilometers();

    

    // LoadingUnits_LoadingUnitsEnum

    Module['PoundsPerSquareFoot'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_PoundsPerSquareFoot();

    Module['TonsPerAcre'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonsPerAcre();

    Module['TonnesPerHectare'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonnesPerHectare();

    Module['KilogramsPerSquareMeter'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_KilogramsPerSquareMeter();

    

    // SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum

    Module['SquareFeetOverCubicFeet'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareFeetOverCubicFeet();

    Module['SquareMetersOverCubicMeters'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareMetersOverCubicMeters();

    Module['SquareInchesOverCubicInches'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareInchesOverCubicInches();

    Module['SquareCentimetersOverCubicCentimeters'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareCentimetersOverCubicCentimeters();

    

    // CoverUnits_CoverUnitsEnum

    Module['Fraction'] = _emscripten_enum_CoverUnits_CoverUnitsEnum_Fraction();

    Module['Percent'] = _emscripten_enum_CoverUnits_CoverUnitsEnum_Percent();

    

    // SpeedUnits_SpeedUnitsEnum

    Module['FeetPerMinute'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_FeetPerMinute();

    Module['ChainsPerHour'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_ChainsPerHour();

    Module['MetersPerSecond'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerSecond();

    Module['MetersPerMinute'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerMinute();

    Module['MilesPerHour'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MilesPerHour();

    Module['KilometersPerHour'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_KilometersPerHour();

    

    // ProbabilityUnits_ProbabilityUnitsEnum

    Module['Fraction'] = _emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Fraction();

    Module['Percent'] = _emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Percent();

    

    // MoistureUnits_MoistureUnitsEnum

    Module['Fraction'] = _emscripten_enum_MoistureUnits_MoistureUnitsEnum_Fraction();

    Module['Percent'] = _emscripten_enum_MoistureUnits_MoistureUnitsEnum_Percent();

    

    // SlopeUnits_SlopeUnitsEnum

    Module['Degrees'] = _emscripten_enum_SlopeUnits_SlopeUnitsEnum_Degrees();

    Module['Percent'] = _emscripten_enum_SlopeUnits_SlopeUnitsEnum_Percent();

    

    // DensityUnits_DensityUnitsEnum

    Module['PoundsPerCubicFoot'] = _emscripten_enum_DensityUnits_DensityUnitsEnum_PoundsPerCubicFoot();

    Module['KilogramsPerCubicMeter'] = _emscripten_enum_DensityUnits_DensityUnitsEnum_KilogramsPerCubicMeter();

    

    // HeatOfCombustionUnits_HeatOfCombustionUnitsEnum

    Module['BtusPerPound'] = _emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_BtusPerPound();

    Module['KilojoulesPerKilogram'] = _emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_KilojoulesPerKilogram();

    

    // HeatSinkUnits_HeatSinkUnitsEnum

    Module['BtusPerCubicFoot'] = _emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_BtusPerCubicFoot();

    Module['KilojoulesPerCubicMeter'] = _emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_KilojoulesPerCubicMeter();

    

    // HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum

    Module['BtusPerSquareFoot'] = _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_BtusPerSquareFoot();

    Module['KilojoulesPerSquareMeter'] = _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilojoulesPerSquareMeter();

    Module['KilowattSecondsPerSquareMeter'] = _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilowattSecondsPerSquareMeter();

    

    // HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum

    Module['BtusPerSquareFootPerMinute'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerMinute();

    Module['BtusPerSquareFootPerSecond'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerSecond();

    Module['KilojoulesPerSquareMeterPerSecond'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerSecond();

    Module['KilojoulesPerSquareMeterPerMinute'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerMinute();

    Module['KilowattsPerSquareMeter'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilowattsPerSquareMeter();

    

    // FirelineIntensityUnits_FirelineIntensityUnitsEnum

    Module['BtusPerFootPerSecond'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerSecond();

    Module['BtusPerFootPerMinute'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerMinute();

    Module['KilojoulesPerMeterPerSecond'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerSecond();

    Module['KilojoulesPerMeterPerMinute'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerMinute();

    Module['KilowattsPerMeter'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilowattsPerMeter();

    

    // TemperatureUnits_TemperatureUnitsEnum

    Module['Fahrenheit'] = _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Fahrenheit();

    Module['Celsius'] = _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Celsius();

    Module['Kelvin'] = _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Kelvin();

    

    // TimeUnits_TimeUnitsEnum

    Module['Minutes'] = _emscripten_enum_TimeUnits_TimeUnitsEnum_Minutes();

    Module['Seconds'] = _emscripten_enum_TimeUnits_TimeUnitsEnum_Seconds();

    Module['Hours'] = _emscripten_enum_TimeUnits_TimeUnitsEnum_Hours();

    

    // ContainTactic

    Module['HeadAttack'] = _emscripten_enum_ContainTactic_HeadAttack();

    Module['RearAttack'] = _emscripten_enum_ContainTactic_RearAttack();

    

    // ContainStatus

    Module['Unreported'] = _emscripten_enum_ContainStatus_Unreported();

    Module['Reported'] = _emscripten_enum_ContainStatus_Reported();

    Module['Attacked'] = _emscripten_enum_ContainStatus_Attacked();

    Module['Contained'] = _emscripten_enum_ContainStatus_Contained();

    Module['Overrun'] = _emscripten_enum_ContainStatus_Overrun();

    Module['Exhausted'] = _emscripten_enum_ContainStatus_Exhausted();

    Module['Overflow'] = _emscripten_enum_ContainStatus_Overflow();

    Module['SizeLimitExceeded'] = _emscripten_enum_ContainStatus_SizeLimitExceeded();

    Module['TimeLimitExceeded'] = _emscripten_enum_ContainStatus_TimeLimitExceeded();

    

    // ContainFlank

    Module['LeftFlank'] = _emscripten_enum_ContainFlank_LeftFlank();

    Module['RightFlank'] = _emscripten_enum_ContainFlank_RightFlank();

    Module['BothFlanks'] = _emscripten_enum_ContainFlank_BothFlanks();

    Module['NeitherFlank'] = _emscripten_enum_ContainFlank_NeitherFlank();

    

    // IgnitionFuelBedType

    Module['PonderosaPineLitter'] = _emscripten_enum_IgnitionFuelBedType_PonderosaPineLitter();

    Module['PunkyWoodRottenChunky'] = _emscripten_enum_IgnitionFuelBedType_PunkyWoodRottenChunky();

    Module['PunkyWoodPowderDeep'] = _emscripten_enum_IgnitionFuelBedType_PunkyWoodPowderDeep();

    Module['PunkWoodPowderShallow'] = _emscripten_enum_IgnitionFuelBedType_PunkWoodPowderShallow();

    Module['LodgepolePineDuff'] = _emscripten_enum_IgnitionFuelBedType_LodgepolePineDuff();

    Module['DouglasFirDuff'] = _emscripten_enum_IgnitionFuelBedType_DouglasFirDuff();

    Module['HighAltitudeMixed'] = _emscripten_enum_IgnitionFuelBedType_HighAltitudeMixed();

    Module['PeatMoss'] = _emscripten_enum_IgnitionFuelBedType_PeatMoss();

    

    // LightningCharge

    Module['Negative'] = _emscripten_enum_LightningCharge_Negative();

    Module['Positive'] = _emscripten_enum_LightningCharge_Positive();

    Module['Unknown'] = _emscripten_enum_LightningCharge_Unknown();

    

    // SpotTreeSpecies

    Module['ENGELMANN_SPRUCE'] = _emscripten_enum_SpotTreeSpecies_ENGELMANN_SPRUCE();

    Module['DOUGLAS_FIR'] = _emscripten_enum_SpotTreeSpecies_DOUGLAS_FIR();

    Module['SUBALPINE_FIR'] = _emscripten_enum_SpotTreeSpecies_SUBALPINE_FIR();

    Module['WESTERN_HEMLOCK'] = _emscripten_enum_SpotTreeSpecies_WESTERN_HEMLOCK();

    Module['PONDEROSA_PINE'] = _emscripten_enum_SpotTreeSpecies_PONDEROSA_PINE();

    Module['LODGEPOLE_PINE'] = _emscripten_enum_SpotTreeSpecies_LODGEPOLE_PINE();

    Module['WESTERN_WHITE_PINE'] = _emscripten_enum_SpotTreeSpecies_WESTERN_WHITE_PINE();

    Module['GRAND_FIR'] = _emscripten_enum_SpotTreeSpecies_GRAND_FIR();

    Module['BALSAM_FIR'] = _emscripten_enum_SpotTreeSpecies_BALSAM_FIR();

    Module['SLASH_PINE'] = _emscripten_enum_SpotTreeSpecies_SLASH_PINE();

    Module['LONGLEAF_PINE'] = _emscripten_enum_SpotTreeSpecies_LONGLEAF_PINE();

    Module['POND_PINE'] = _emscripten_enum_SpotTreeSpecies_POND_PINE();

    Module['SHORTLEAF_PINE'] = _emscripten_enum_SpotTreeSpecies_SHORTLEAF_PINE();

    Module['LOBLOLLY_PINE'] = _emscripten_enum_SpotTreeSpecies_LOBLOLLY_PINE();

    

    // SpotFireLocation

    Module['MIDSLOPE_WINDWARD'] = _emscripten_enum_SpotFireLocation_MIDSLOPE_WINDWARD();

    Module['VALLEY_BOTTOM'] = _emscripten_enum_SpotFireLocation_VALLEY_BOTTOM();

    Module['MIDSLOPE_LEEWARD'] = _emscripten_enum_SpotFireLocation_MIDSLOPE_LEEWARD();

    Module['RIDGE_TOP'] = _emscripten_enum_SpotFireLocation_RIDGE_TOP();

    

    // SpotArrayConstants

    Module['NUM_COLS'] = _emscripten_enum_SpotArrayConstants_NUM_COLS();

    Module['NUM_FIREBRAND_ROWS'] = _emscripten_enum_SpotArrayConstants_NUM_FIREBRAND_ROWS();

    Module['NUM_SPECIES'] = _emscripten_enum_SpotArrayConstants_NUM_SPECIES();

    

    // AspenFireSeverity_AspenFireSeverityEnum

    Module['Low'] = _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low();

    Module['Moderate'] = _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate();

    

    // TwoFuelModelsMethod_TwoFuelModelsMethodEnum

    Module['NoMethod'] = _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_NoMethod();

    Module['Arithmetic'] = _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Arithmetic();

    Module['Harmonic'] = _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Harmonic();

    Module['TwoDimensional'] = _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_TwoDimensional();

    

    // WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum

    Module['Unsheltered'] = _emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Unsheltered();

    Module['Sheltered'] = _emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Sheltered();

    

    // WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum

    Module['UserInput'] = _emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UserInput();

    Module['UseCrownRatio'] = _emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UseCrownRatio();

    Module['DontUseCrownRatio'] = _emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_DontUseCrownRatio();

    

    // WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum

    Module['RelativeToUpslope'] = _emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToUpslope();

    Module['RelativeToNorth'] = _emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToNorth();

    

    // WindHeightInputMode_WindHeightInputModeEnum

    Module['DirectMidflame'] = _emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_DirectMidflame();

    Module['TwentyFoot'] = _emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TwentyFoot();

    Module['TenMeter'] = _emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TenMeter();

    

    // FireType_FireTypeEnum

    Module['Surface'] = _emscripten_enum_FireType_FireTypeEnum_Surface();

    Module['Torching'] = _emscripten_enum_FireType_FireTypeEnum_Torching();

    Module['ConditionalCrownFire'] = _emscripten_enum_FireType_FireTypeEnum_ConditionalCrownFire();

    Module['Crowning'] = _emscripten_enum_FireType_FireTypeEnum_Crowning();

    

    // BeetleDamage

    Module['not_set'] = _emscripten_enum_BeetleDamage_not_set();

    Module['no'] = _emscripten_enum_BeetleDamage_no();

    Module['yes'] = _emscripten_enum_BeetleDamage_yes();

    

    // FireSeverity

    Module['not_set'] = _emscripten_enum_FireSeverity_not_set();

    Module['empty'] = _emscripten_enum_FireSeverity_empty();

    Module['low'] = _emscripten_enum_FireSeverity_low();

    

    // FlameLengthOrScorchHeightSwitch

    Module['flame_length'] = _emscripten_enum_FlameLengthOrScorchHeightSwitch_flame_length();

    Module['scorch_height'] = _emscripten_enum_FlameLengthOrScorchHeightSwitch_scorch_height();

    

    // RegionCode

    Module['interior_west'] = _emscripten_enum_RegionCode_interior_west();

    Module['pacific_west'] = _emscripten_enum_RegionCode_pacific_west();

    Module['north_east'] = _emscripten_enum_RegionCode_north_east();

    Module['south_east'] = _emscripten_enum_RegionCode_south_east();

    

    // CrownDamageType

    Module['not_set'] = _emscripten_enum_CrownDamageType_not_set();

    Module['crown_length'] = _emscripten_enum_CrownDamageType_crown_length();

    Module['crown_volume'] = _emscripten_enum_CrownDamageType_crown_volume();

    Module['crown_kill'] = _emscripten_enum_CrownDamageType_crown_kill();

    

    // RequiredFieldNames

    Module['region'] = _emscripten_enum_RequiredFieldNames_region();

    Module['flame_length_or_scorch_height_switch'] = _emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_switch();

    Module['flame_length_or_scorch_height_value'] = _emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_value();

    Module['equation_type'] = _emscripten_enum_RequiredFieldNames_equation_type();

    Module['dbh'] = _emscripten_enum_RequiredFieldNames_dbh();

    Module['tree_height'] = _emscripten_enum_RequiredFieldNames_tree_height();

    Module['crown_ratio'] = _emscripten_enum_RequiredFieldNames_crown_ratio();

    Module['crown_damage'] = _emscripten_enum_RequiredFieldNames_crown_damage();

    Module['cambium_kill_rating'] = _emscripten_enum_RequiredFieldNames_cambium_kill_rating();

    Module['beetle_damage'] = _emscripten_enum_RequiredFieldNames_beetle_damage();

    Module['bole_char_height'] = _emscripten_enum_RequiredFieldNames_bole_char_height();

    Module['bark_thickness'] = _emscripten_enum_RequiredFieldNames_bark_thickness();

    Module['fire_severity'] = _emscripten_enum_RequiredFieldNames_fire_severity();

    Module['num_inputs'] = _emscripten_enum_RequiredFieldNames_num_inputs();

    

    // CrownDamageEquationCode

    Module['not_set'] = _emscripten_enum_CrownDamageEquationCode_not_set();

    Module['white_fir'] = _emscripten_enum_CrownDamageEquationCode_white_fir();

    Module['subalpine_fir'] = _emscripten_enum_CrownDamageEquationCode_subalpine_fir();

    Module['incense_cedar'] = _emscripten_enum_CrownDamageEquationCode_incense_cedar();

    Module['western_larch'] = _emscripten_enum_CrownDamageEquationCode_western_larch();

    Module['whitebark_pine'] = _emscripten_enum_CrownDamageEquationCode_whitebark_pine();

    Module['engelmann_spruce'] = _emscripten_enum_CrownDamageEquationCode_engelmann_spruce();

    Module['sugar_pine'] = _emscripten_enum_CrownDamageEquationCode_sugar_pine();

    Module['red_fir'] = _emscripten_enum_CrownDamageEquationCode_red_fir();

    Module['ponderosa_pine'] = _emscripten_enum_CrownDamageEquationCode_ponderosa_pine();

    Module['ponderosa_kill'] = _emscripten_enum_CrownDamageEquationCode_ponderosa_kill();

    Module['douglas_fir'] = _emscripten_enum_CrownDamageEquationCode_douglas_fir();

    

    // EquationType

    Module['not_set'] = _emscripten_enum_EquationType_not_set();

    Module['crown_scorch'] = _emscripten_enum_EquationType_crown_scorch();

    Module['bole_char'] = _emscripten_enum_EquationType_bole_char();

    Module['crown_damage'] = _emscripten_enum_EquationType_crown_damage();

  }
  if (runtimeInitialized) setupEnums();
  else addOnInit(setupEnums);
})();


// end include: /home/kcheung/work/code/behave-polylith/behave-lib/include/js/glue.js
