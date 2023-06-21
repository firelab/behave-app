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

  
if (!Module["noFSInit"] && !FS.init.initialized)
  FS.init();
FS.ignorePermissions = false;

TTY.init();
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


  function setErrNo(value) {
      HEAP32[((___errno_location())>>2)] = value;
      return value;
    }
  
  var PATH = {isAbs:(path) => path.charAt(0) === '/',splitPath:(filename) => {
        var splitPathRe = /^(\/?|)([\s\S]*?)((?:\.{1,2}|[^\/]+?|)(\.[^.\/]*|))(?:[\/]*)$/;
        return splitPathRe.exec(filename).slice(1);
      },normalizeArray:(parts, allowAboveRoot) => {
        // if the path tries to go above the root, `up` ends up > 0
        var up = 0;
        for (var i = parts.length - 1; i >= 0; i--) {
          var last = parts[i];
          if (last === '.') {
            parts.splice(i, 1);
          } else if (last === '..') {
            parts.splice(i, 1);
            up++;
          } else if (up) {
            parts.splice(i, 1);
            up--;
          }
        }
        // if the path is allowed to go above the root, restore leading ..s
        if (allowAboveRoot) {
          for (; up; up--) {
            parts.unshift('..');
          }
        }
        return parts;
      },normalize:(path) => {
        var isAbsolute = PATH.isAbs(path),
            trailingSlash = path.substr(-1) === '/';
        // Normalize the path
        path = PATH.normalizeArray(path.split('/').filter((p) => !!p), !isAbsolute).join('/');
        if (!path && !isAbsolute) {
          path = '.';
        }
        if (path && trailingSlash) {
          path += '/';
        }
        return (isAbsolute ? '/' : '') + path;
      },dirname:(path) => {
        var result = PATH.splitPath(path),
            root = result[0],
            dir = result[1];
        if (!root && !dir) {
          // No dirname whatsoever
          return '.';
        }
        if (dir) {
          // It has a dirname, strip trailing slash
          dir = dir.substr(0, dir.length - 1);
        }
        return root + dir;
      },basename:(path) => {
        // EMSCRIPTEN return '/'' for '/', not an empty string
        if (path === '/') return '/';
        path = PATH.normalize(path);
        path = path.replace(/\/$/, "");
        var lastSlash = path.lastIndexOf('/');
        if (lastSlash === -1) return path;
        return path.substr(lastSlash+1);
      },join:function() {
        var paths = Array.prototype.slice.call(arguments);
        return PATH.normalize(paths.join('/'));
      },join2:(l, r) => {
        return PATH.normalize(l + '/' + r);
      }};
  
  function getRandomDevice() {
      if (typeof crypto == 'object' && typeof crypto['getRandomValues'] == 'function') {
        // for modern web browsers
        var randomBuffer = new Uint8Array(1);
        return () => { crypto.getRandomValues(randomBuffer); return randomBuffer[0]; };
      } else
      if (ENVIRONMENT_IS_NODE) {
        // for nodejs with or without crypto support included
        try {
          var crypto_module = require('crypto');
          // nodejs has crypto support
          return () => crypto_module['randomBytes'](1)[0];
        } catch (e) {
          // nodejs doesn't have crypto support
        }
      }
      // we couldn't find a proper implementation, as Math.random() is not suitable for /dev/random, see emscripten-core/emscripten/pull/7096
      return () => abort("no cryptographic support found for randomDevice. consider polyfilling it if you want to use something insecure like Math.random(), e.g. put this in a --pre-js: var crypto = { getRandomValues: function(array) { for (var i = 0; i < array.length; i++) array[i] = (Math.random()*256)|0 } };");
    }
  
  
  
  var PATH_FS = {resolve:function() {
        var resolvedPath = '',
          resolvedAbsolute = false;
        for (var i = arguments.length - 1; i >= -1 && !resolvedAbsolute; i--) {
          var path = (i >= 0) ? arguments[i] : FS.cwd();
          // Skip empty and invalid entries
          if (typeof path != 'string') {
            throw new TypeError('Arguments to path.resolve must be strings');
          } else if (!path) {
            return ''; // an invalid portion invalidates the whole thing
          }
          resolvedPath = path + '/' + resolvedPath;
          resolvedAbsolute = PATH.isAbs(path);
        }
        // At this point the path should be resolved to a full absolute path, but
        // handle relative paths to be safe (might happen when process.cwd() fails)
        resolvedPath = PATH.normalizeArray(resolvedPath.split('/').filter((p) => !!p), !resolvedAbsolute).join('/');
        return ((resolvedAbsolute ? '/' : '') + resolvedPath) || '.';
      },relative:(from, to) => {
        from = PATH_FS.resolve(from).substr(1);
        to = PATH_FS.resolve(to).substr(1);
        function trim(arr) {
          var start = 0;
          for (; start < arr.length; start++) {
            if (arr[start] !== '') break;
          }
          var end = arr.length - 1;
          for (; end >= 0; end--) {
            if (arr[end] !== '') break;
          }
          if (start > end) return [];
          return arr.slice(start, end - start + 1);
        }
        var fromParts = trim(from.split('/'));
        var toParts = trim(to.split('/'));
        var length = Math.min(fromParts.length, toParts.length);
        var samePartsLength = length;
        for (var i = 0; i < length; i++) {
          if (fromParts[i] !== toParts[i]) {
            samePartsLength = i;
            break;
          }
        }
        var outputParts = [];
        for (var i = samePartsLength; i < fromParts.length; i++) {
          outputParts.push('..');
        }
        outputParts = outputParts.concat(toParts.slice(samePartsLength));
        return outputParts.join('/');
      }};
  
  
  /** @type {function(string, boolean=, number=)} */
  function intArrayFromString(stringy, dontAddNull, length) {
    var len = length > 0 ? length : lengthBytesUTF8(stringy)+1;
    var u8array = new Array(len);
    var numBytesWritten = stringToUTF8Array(stringy, u8array, 0, u8array.length);
    if (dontAddNull) u8array.length = numBytesWritten;
    return u8array;
  }
  var TTY = {ttys:[],init:function () {
        // https://github.com/emscripten-core/emscripten/pull/1555
        // if (ENVIRONMENT_IS_NODE) {
        //   // currently, FS.init does not distinguish if process.stdin is a file or TTY
        //   // device, it always assumes it's a TTY device. because of this, we're forcing
        //   // process.stdin to UTF8 encoding to at least make stdin reading compatible
        //   // with text files until FS.init can be refactored.
        //   process.stdin.setEncoding('utf8');
        // }
      },shutdown:function() {
        // https://github.com/emscripten-core/emscripten/pull/1555
        // if (ENVIRONMENT_IS_NODE) {
        //   // inolen: any idea as to why node -e 'process.stdin.read()' wouldn't exit immediately (with process.stdin being a tty)?
        //   // isaacs: because now it's reading from the stream, you've expressed interest in it, so that read() kicks off a _read() which creates a ReadReq operation
        //   // inolen: I thought read() in that case was a synchronous operation that just grabbed some amount of buffered data if it exists?
        //   // isaacs: it is. but it also triggers a _read() call, which calls readStart() on the handle
        //   // isaacs: do process.stdin.pause() and i'd think it'd probably close the pending call
        //   process.stdin.pause();
        // }
      },register:function(dev, ops) {
        TTY.ttys[dev] = { input: [], output: [], ops: ops };
        FS.registerDevice(dev, TTY.stream_ops);
      },stream_ops:{open:function(stream) {
          var tty = TTY.ttys[stream.node.rdev];
          if (!tty) {
            throw new FS.ErrnoError(43);
          }
          stream.tty = tty;
          stream.seekable = false;
        },close:function(stream) {
          // flush any pending line data
          stream.tty.ops.fsync(stream.tty);
        },fsync:function(stream) {
          stream.tty.ops.fsync(stream.tty);
        },read:function(stream, buffer, offset, length, pos /* ignored */) {
          if (!stream.tty || !stream.tty.ops.get_char) {
            throw new FS.ErrnoError(60);
          }
          var bytesRead = 0;
          for (var i = 0; i < length; i++) {
            var result;
            try {
              result = stream.tty.ops.get_char(stream.tty);
            } catch (e) {
              throw new FS.ErrnoError(29);
            }
            if (result === undefined && bytesRead === 0) {
              throw new FS.ErrnoError(6);
            }
            if (result === null || result === undefined) break;
            bytesRead++;
            buffer[offset+i] = result;
          }
          if (bytesRead) {
            stream.node.timestamp = Date.now();
          }
          return bytesRead;
        },write:function(stream, buffer, offset, length, pos) {
          if (!stream.tty || !stream.tty.ops.put_char) {
            throw new FS.ErrnoError(60);
          }
          try {
            for (var i = 0; i < length; i++) {
              stream.tty.ops.put_char(stream.tty, buffer[offset+i]);
            }
          } catch (e) {
            throw new FS.ErrnoError(29);
          }
          if (length) {
            stream.node.timestamp = Date.now();
          }
          return i;
        }},default_tty_ops:{get_char:function(tty) {
          if (!tty.input.length) {
            var result = null;
            if (ENVIRONMENT_IS_NODE) {
              // we will read data by chunks of BUFSIZE
              var BUFSIZE = 256;
              var buf = Buffer.alloc(BUFSIZE);
              var bytesRead = 0;
  
              try {
                bytesRead = fs.readSync(process.stdin.fd, buf, 0, BUFSIZE, -1);
              } catch(e) {
                // Cross-platform differences: on Windows, reading EOF throws an exception, but on other OSes,
                // reading EOF returns 0. Uniformize behavior by treating the EOF exception to return 0.
                if (e.toString().includes('EOF')) bytesRead = 0;
                else throw e;
              }
  
              if (bytesRead > 0) {
                result = buf.slice(0, bytesRead).toString('utf-8');
              } else {
                result = null;
              }
            } else
            if (typeof window != 'undefined' &&
              typeof window.prompt == 'function') {
              // Browser.
              result = window.prompt('Input: ');  // returns null on cancel
              if (result !== null) {
                result += '\n';
              }
            } else if (typeof readline == 'function') {
              // Command line.
              result = readline();
              if (result !== null) {
                result += '\n';
              }
            }
            if (!result) {
              return null;
            }
            tty.input = intArrayFromString(result, true);
          }
          return tty.input.shift();
        },put_char:function(tty, val) {
          if (val === null || val === 10) {
            out(UTF8ArrayToString(tty.output, 0));
            tty.output = [];
          } else {
            if (val != 0) tty.output.push(val); // val == 0 would cut text output off in the middle.
          }
        },fsync:function(tty) {
          if (tty.output && tty.output.length > 0) {
            out(UTF8ArrayToString(tty.output, 0));
            tty.output = [];
          }
        }},default_tty1_ops:{put_char:function(tty, val) {
          if (val === null || val === 10) {
            err(UTF8ArrayToString(tty.output, 0));
            tty.output = [];
          } else {
            if (val != 0) tty.output.push(val);
          }
        },fsync:function(tty) {
          if (tty.output && tty.output.length > 0) {
            err(UTF8ArrayToString(tty.output, 0));
            tty.output = [];
          }
        }}};
  
  
  function zeroMemory(address, size) {
      HEAPU8.fill(0, address, address + size);
      return address;
    }
  
  function alignMemory(size, alignment) {
      assert(alignment, "alignment argument is required");
      return Math.ceil(size / alignment) * alignment;
    }
  function mmapAlloc(size) {
      abort('internal error: mmapAlloc called but `emscripten_builtin_memalign` native symbol not exported');
    }
  var MEMFS = {ops_table:null,mount:function(mount) {
        return MEMFS.createNode(null, '/', 16384 | 511 /* 0777 */, 0);
      },createNode:function(parent, name, mode, dev) {
        if (FS.isBlkdev(mode) || FS.isFIFO(mode)) {
          // no supported
          throw new FS.ErrnoError(63);
        }
        if (!MEMFS.ops_table) {
          MEMFS.ops_table = {
            dir: {
              node: {
                getattr: MEMFS.node_ops.getattr,
                setattr: MEMFS.node_ops.setattr,
                lookup: MEMFS.node_ops.lookup,
                mknod: MEMFS.node_ops.mknod,
                rename: MEMFS.node_ops.rename,
                unlink: MEMFS.node_ops.unlink,
                rmdir: MEMFS.node_ops.rmdir,
                readdir: MEMFS.node_ops.readdir,
                symlink: MEMFS.node_ops.symlink
              },
              stream: {
                llseek: MEMFS.stream_ops.llseek
              }
            },
            file: {
              node: {
                getattr: MEMFS.node_ops.getattr,
                setattr: MEMFS.node_ops.setattr
              },
              stream: {
                llseek: MEMFS.stream_ops.llseek,
                read: MEMFS.stream_ops.read,
                write: MEMFS.stream_ops.write,
                allocate: MEMFS.stream_ops.allocate,
                mmap: MEMFS.stream_ops.mmap,
                msync: MEMFS.stream_ops.msync
              }
            },
            link: {
              node: {
                getattr: MEMFS.node_ops.getattr,
                setattr: MEMFS.node_ops.setattr,
                readlink: MEMFS.node_ops.readlink
              },
              stream: {}
            },
            chrdev: {
              node: {
                getattr: MEMFS.node_ops.getattr,
                setattr: MEMFS.node_ops.setattr
              },
              stream: FS.chrdev_stream_ops
            }
          };
        }
        var node = FS.createNode(parent, name, mode, dev);
        if (FS.isDir(node.mode)) {
          node.node_ops = MEMFS.ops_table.dir.node;
          node.stream_ops = MEMFS.ops_table.dir.stream;
          node.contents = {};
        } else if (FS.isFile(node.mode)) {
          node.node_ops = MEMFS.ops_table.file.node;
          node.stream_ops = MEMFS.ops_table.file.stream;
          node.usedBytes = 0; // The actual number of bytes used in the typed array, as opposed to contents.length which gives the whole capacity.
          // When the byte data of the file is populated, this will point to either a typed array, or a normal JS array. Typed arrays are preferred
          // for performance, and used by default. However, typed arrays are not resizable like normal JS arrays are, so there is a small disk size
          // penalty involved for appending file writes that continuously grow a file similar to std::vector capacity vs used -scheme.
          node.contents = null; 
        } else if (FS.isLink(node.mode)) {
          node.node_ops = MEMFS.ops_table.link.node;
          node.stream_ops = MEMFS.ops_table.link.stream;
        } else if (FS.isChrdev(node.mode)) {
          node.node_ops = MEMFS.ops_table.chrdev.node;
          node.stream_ops = MEMFS.ops_table.chrdev.stream;
        }
        node.timestamp = Date.now();
        // add the new node to the parent
        if (parent) {
          parent.contents[name] = node;
          parent.timestamp = node.timestamp;
        }
        return node;
      },getFileDataAsTypedArray:function(node) {
        if (!node.contents) return new Uint8Array(0);
        if (node.contents.subarray) return node.contents.subarray(0, node.usedBytes); // Make sure to not return excess unused bytes.
        return new Uint8Array(node.contents);
      },expandFileStorage:function(node, newCapacity) {
        var prevCapacity = node.contents ? node.contents.length : 0;
        if (prevCapacity >= newCapacity) return; // No need to expand, the storage was already large enough.
        // Don't expand strictly to the given requested limit if it's only a very small increase, but instead geometrically grow capacity.
        // For small filesizes (<1MB), perform size*2 geometric increase, but for large sizes, do a much more conservative size*1.125 increase to
        // avoid overshooting the allocation cap by a very large margin.
        var CAPACITY_DOUBLING_MAX = 1024 * 1024;
        newCapacity = Math.max(newCapacity, (prevCapacity * (prevCapacity < CAPACITY_DOUBLING_MAX ? 2.0 : 1.125)) >>> 0);
        if (prevCapacity != 0) newCapacity = Math.max(newCapacity, 256); // At minimum allocate 256b for each file when expanding.
        var oldContents = node.contents;
        node.contents = new Uint8Array(newCapacity); // Allocate new storage.
        if (node.usedBytes > 0) node.contents.set(oldContents.subarray(0, node.usedBytes), 0); // Copy old data over to the new storage.
      },resizeFileStorage:function(node, newSize) {
        if (node.usedBytes == newSize) return;
        if (newSize == 0) {
          node.contents = null; // Fully decommit when requesting a resize to zero.
          node.usedBytes = 0;
        } else {
          var oldContents = node.contents;
          node.contents = new Uint8Array(newSize); // Allocate new storage.
          if (oldContents) {
            node.contents.set(oldContents.subarray(0, Math.min(newSize, node.usedBytes))); // Copy old data over to the new storage.
          }
          node.usedBytes = newSize;
        }
      },node_ops:{getattr:function(node) {
          var attr = {};
          // device numbers reuse inode numbers.
          attr.dev = FS.isChrdev(node.mode) ? node.id : 1;
          attr.ino = node.id;
          attr.mode = node.mode;
          attr.nlink = 1;
          attr.uid = 0;
          attr.gid = 0;
          attr.rdev = node.rdev;
          if (FS.isDir(node.mode)) {
            attr.size = 4096;
          } else if (FS.isFile(node.mode)) {
            attr.size = node.usedBytes;
          } else if (FS.isLink(node.mode)) {
            attr.size = node.link.length;
          } else {
            attr.size = 0;
          }
          attr.atime = new Date(node.timestamp);
          attr.mtime = new Date(node.timestamp);
          attr.ctime = new Date(node.timestamp);
          // NOTE: In our implementation, st_blocks = Math.ceil(st_size/st_blksize),
          //       but this is not required by the standard.
          attr.blksize = 4096;
          attr.blocks = Math.ceil(attr.size / attr.blksize);
          return attr;
        },setattr:function(node, attr) {
          if (attr.mode !== undefined) {
            node.mode = attr.mode;
          }
          if (attr.timestamp !== undefined) {
            node.timestamp = attr.timestamp;
          }
          if (attr.size !== undefined) {
            MEMFS.resizeFileStorage(node, attr.size);
          }
        },lookup:function(parent, name) {
          throw FS.genericErrors[44];
        },mknod:function(parent, name, mode, dev) {
          return MEMFS.createNode(parent, name, mode, dev);
        },rename:function(old_node, new_dir, new_name) {
          // if we're overwriting a directory at new_name, make sure it's empty.
          if (FS.isDir(old_node.mode)) {
            var new_node;
            try {
              new_node = FS.lookupNode(new_dir, new_name);
            } catch (e) {
            }
            if (new_node) {
              for (var i in new_node.contents) {
                throw new FS.ErrnoError(55);
              }
            }
          }
          // do the internal rewiring
          delete old_node.parent.contents[old_node.name];
          old_node.parent.timestamp = Date.now()
          old_node.name = new_name;
          new_dir.contents[new_name] = old_node;
          new_dir.timestamp = old_node.parent.timestamp;
          old_node.parent = new_dir;
        },unlink:function(parent, name) {
          delete parent.contents[name];
          parent.timestamp = Date.now();
        },rmdir:function(parent, name) {
          var node = FS.lookupNode(parent, name);
          for (var i in node.contents) {
            throw new FS.ErrnoError(55);
          }
          delete parent.contents[name];
          parent.timestamp = Date.now();
        },readdir:function(node) {
          var entries = ['.', '..'];
          for (var key in node.contents) {
            if (!node.contents.hasOwnProperty(key)) {
              continue;
            }
            entries.push(key);
          }
          return entries;
        },symlink:function(parent, newname, oldpath) {
          var node = MEMFS.createNode(parent, newname, 511 /* 0777 */ | 40960, 0);
          node.link = oldpath;
          return node;
        },readlink:function(node) {
          if (!FS.isLink(node.mode)) {
            throw new FS.ErrnoError(28);
          }
          return node.link;
        }},stream_ops:{read:function(stream, buffer, offset, length, position) {
          var contents = stream.node.contents;
          if (position >= stream.node.usedBytes) return 0;
          var size = Math.min(stream.node.usedBytes - position, length);
          assert(size >= 0);
          if (size > 8 && contents.subarray) { // non-trivial, and typed array
            buffer.set(contents.subarray(position, position + size), offset);
          } else {
            for (var i = 0; i < size; i++) buffer[offset + i] = contents[position + i];
          }
          return size;
        },write:function(stream, buffer, offset, length, position, canOwn) {
          // The data buffer should be a typed array view
          assert(!(buffer instanceof ArrayBuffer));
          // If the buffer is located in main memory (HEAP), and if
          // memory can grow, we can't hold on to references of the
          // memory buffer, as they may get invalidated. That means we
          // need to do copy its contents.
          if (buffer.buffer === HEAP8.buffer) {
            canOwn = false;
          }
  
          if (!length) return 0;
          var node = stream.node;
          node.timestamp = Date.now();
  
          if (buffer.subarray && (!node.contents || node.contents.subarray)) { // This write is from a typed array to a typed array?
            if (canOwn) {
              assert(position === 0, 'canOwn must imply no weird position inside the file');
              node.contents = buffer.subarray(offset, offset + length);
              node.usedBytes = length;
              return length;
            } else if (node.usedBytes === 0 && position === 0) { // If this is a simple first write to an empty file, do a fast set since we don't need to care about old data.
              node.contents = buffer.slice(offset, offset + length);
              node.usedBytes = length;
              return length;
            } else if (position + length <= node.usedBytes) { // Writing to an already allocated and used subrange of the file?
              node.contents.set(buffer.subarray(offset, offset + length), position);
              return length;
            }
          }
  
          // Appending to an existing file and we need to reallocate, or source data did not come as a typed array.
          MEMFS.expandFileStorage(node, position+length);
          if (node.contents.subarray && buffer.subarray) {
            // Use typed array write which is available.
            node.contents.set(buffer.subarray(offset, offset + length), position);
          } else {
            for (var i = 0; i < length; i++) {
             node.contents[position + i] = buffer[offset + i]; // Or fall back to manual write if not.
            }
          }
          node.usedBytes = Math.max(node.usedBytes, position + length);
          return length;
        },llseek:function(stream, offset, whence) {
          var position = offset;
          if (whence === 1) {
            position += stream.position;
          } else if (whence === 2) {
            if (FS.isFile(stream.node.mode)) {
              position += stream.node.usedBytes;
            }
          }
          if (position < 0) {
            throw new FS.ErrnoError(28);
          }
          return position;
        },allocate:function(stream, offset, length) {
          MEMFS.expandFileStorage(stream.node, offset + length);
          stream.node.usedBytes = Math.max(stream.node.usedBytes, offset + length);
        },mmap:function(stream, length, position, prot, flags) {
          if (!FS.isFile(stream.node.mode)) {
            throw new FS.ErrnoError(43);
          }
          var ptr;
          var allocated;
          var contents = stream.node.contents;
          // Only make a new copy when MAP_PRIVATE is specified.
          if (!(flags & 2) && contents.buffer === HEAP8.buffer) {
            // We can't emulate MAP_SHARED when the file is not backed by the
            // buffer we're mapping to (e.g. the HEAP buffer).
            allocated = false;
            ptr = contents.byteOffset;
          } else {
            // Try to avoid unnecessary slices.
            if (position > 0 || position + length < contents.length) {
              if (contents.subarray) {
                contents = contents.subarray(position, position + length);
              } else {
                contents = Array.prototype.slice.call(contents, position, position + length);
              }
            }
            allocated = true;
            ptr = mmapAlloc(length);
            if (!ptr) {
              throw new FS.ErrnoError(48);
            }
            HEAP8.set(contents, ptr);
          }
          return { ptr: ptr, allocated: allocated };
        },msync:function(stream, buffer, offset, length, mmapFlags) {
          MEMFS.stream_ops.write(stream, buffer, 0, length, offset, false);
          // should we check if bytesWritten and length are the same?
          return 0;
        }}};
  
  /** @param {boolean=} noRunDep */
  function asyncLoad(url, onload, onerror, noRunDep) {
      var dep = !noRunDep ? getUniqueRunDependency('al ' + url) : '';
      readAsync(url, (arrayBuffer) => {
        assert(arrayBuffer, 'Loading data file "' + url + '" failed (no arrayBuffer).');
        onload(new Uint8Array(arrayBuffer));
        if (dep) removeRunDependency(dep);
      }, (event) => {
        if (onerror) {
          onerror();
        } else {
          throw 'Loading data file "' + url + '" failed.';
        }
      });
      if (dep) addRunDependency(dep);
    }
  
  
  var ERRNO_MESSAGES = {0:"Success",1:"Arg list too long",2:"Permission denied",3:"Address already in use",4:"Address not available",5:"Address family not supported by protocol family",6:"No more processes",7:"Socket already connected",8:"Bad file number",9:"Trying to read unreadable message",10:"Mount device busy",11:"Operation canceled",12:"No children",13:"Connection aborted",14:"Connection refused",15:"Connection reset by peer",16:"File locking deadlock error",17:"Destination address required",18:"Math arg out of domain of func",19:"Quota exceeded",20:"File exists",21:"Bad address",22:"File too large",23:"Host is unreachable",24:"Identifier removed",25:"Illegal byte sequence",26:"Connection already in progress",27:"Interrupted system call",28:"Invalid argument",29:"I/O error",30:"Socket is already connected",31:"Is a directory",32:"Too many symbolic links",33:"Too many open files",34:"Too many links",35:"Message too long",36:"Multihop attempted",37:"File or path name too long",38:"Network interface is not configured",39:"Connection reset by network",40:"Network is unreachable",41:"Too many open files in system",42:"No buffer space available",43:"No such device",44:"No such file or directory",45:"Exec format error",46:"No record locks available",47:"The link has been severed",48:"Not enough core",49:"No message of desired type",50:"Protocol not available",51:"No space left on device",52:"Function not implemented",53:"Socket is not connected",54:"Not a directory",55:"Directory not empty",56:"State not recoverable",57:"Socket operation on non-socket",59:"Not a typewriter",60:"No such device or address",61:"Value too large for defined data type",62:"Previous owner died",63:"Not super-user",64:"Broken pipe",65:"Protocol error",66:"Unknown protocol",67:"Protocol wrong type for socket",68:"Math result not representable",69:"Read only file system",70:"Illegal seek",71:"No such process",72:"Stale file handle",73:"Connection timed out",74:"Text file busy",75:"Cross-device link",100:"Device not a stream",101:"Bad font file fmt",102:"Invalid slot",103:"Invalid request code",104:"No anode",105:"Block device required",106:"Channel number out of range",107:"Level 3 halted",108:"Level 3 reset",109:"Link number out of range",110:"Protocol driver not attached",111:"No CSI structure available",112:"Level 2 halted",113:"Invalid exchange",114:"Invalid request descriptor",115:"Exchange full",116:"No data (for no delay io)",117:"Timer expired",118:"Out of streams resources",119:"Machine is not on the network",120:"Package not installed",121:"The object is remote",122:"Advertise error",123:"Srmount error",124:"Communication error on send",125:"Cross mount point (not really error)",126:"Given log. name not unique",127:"f.d. invalid for this operation",128:"Remote address changed",129:"Can   access a needed shared lib",130:"Accessing a corrupted shared lib",131:".lib section in a.out corrupted",132:"Attempting to link in too many libs",133:"Attempting to exec a shared library",135:"Streams pipe error",136:"Too many users",137:"Socket type not supported",138:"Not supported",139:"Protocol family not supported",140:"Can't send after socket shutdown",141:"Too many references",142:"Host is down",148:"No medium (in tape drive)",156:"Level 2 not synchronized"};
  
  var ERRNO_CODES = {};
  
  function demangle(func) {
      warnOnce('warning: build with -sDEMANGLE_SUPPORT to link in libcxxabi demangling');
      return func;
    }
  function demangleAll(text) {
      var regex =
        /\b_Z[\w\d_]+/g;
      return text.replace(regex,
        function(x) {
          var y = demangle(x);
          return x === y ? x : (y + ' [' + x + ']');
        });
    }
  var FS = {root:null,mounts:[],devices:{},streams:[],nextInode:1,nameTable:null,currentPath:"/",initialized:false,ignorePermissions:true,ErrnoError:null,genericErrors:{},filesystems:null,syncFSRequests:0,lookupPath:(path, opts = {}) => {
        path = PATH_FS.resolve(path);
  
        if (!path) return { path: '', node: null };
  
        var defaults = {
          follow_mount: true,
          recurse_count: 0
        };
        opts = Object.assign(defaults, opts)
  
        if (opts.recurse_count > 8) {  // max recursive lookup of 8
          throw new FS.ErrnoError(32);
        }
  
        // split the absolute path
        var parts = path.split('/').filter((p) => !!p);
  
        // start at the root
        var current = FS.root;
        var current_path = '/';
  
        for (var i = 0; i < parts.length; i++) {
          var islast = (i === parts.length-1);
          if (islast && opts.parent) {
            // stop resolving
            break;
          }
  
          current = FS.lookupNode(current, parts[i]);
          current_path = PATH.join2(current_path, parts[i]);
  
          // jump to the mount's root node if this is a mountpoint
          if (FS.isMountpoint(current)) {
            if (!islast || (islast && opts.follow_mount)) {
              current = current.mounted.root;
            }
          }
  
          // by default, lookupPath will not follow a symlink if it is the final path component.
          // setting opts.follow = true will override this behavior.
          if (!islast || opts.follow) {
            var count = 0;
            while (FS.isLink(current.mode)) {
              var link = FS.readlink(current_path);
              current_path = PATH_FS.resolve(PATH.dirname(current_path), link);
  
              var lookup = FS.lookupPath(current_path, { recurse_count: opts.recurse_count + 1 });
              current = lookup.node;
  
              if (count++ > 40) {  // limit max consecutive symlinks to 40 (SYMLOOP_MAX).
                throw new FS.ErrnoError(32);
              }
            }
          }
        }
  
        return { path: current_path, node: current };
      },getPath:(node) => {
        var path;
        while (true) {
          if (FS.isRoot(node)) {
            var mount = node.mount.mountpoint;
            if (!path) return mount;
            return mount[mount.length-1] !== '/' ? mount + '/' + path : mount + path;
          }
          path = path ? node.name + '/' + path : node.name;
          node = node.parent;
        }
      },hashName:(parentid, name) => {
        var hash = 0;
  
        for (var i = 0; i < name.length; i++) {
          hash = ((hash << 5) - hash + name.charCodeAt(i)) | 0;
        }
        return ((parentid + hash) >>> 0) % FS.nameTable.length;
      },hashAddNode:(node) => {
        var hash = FS.hashName(node.parent.id, node.name);
        node.name_next = FS.nameTable[hash];
        FS.nameTable[hash] = node;
      },hashRemoveNode:(node) => {
        var hash = FS.hashName(node.parent.id, node.name);
        if (FS.nameTable[hash] === node) {
          FS.nameTable[hash] = node.name_next;
        } else {
          var current = FS.nameTable[hash];
          while (current) {
            if (current.name_next === node) {
              current.name_next = node.name_next;
              break;
            }
            current = current.name_next;
          }
        }
      },lookupNode:(parent, name) => {
        var errCode = FS.mayLookup(parent);
        if (errCode) {
          throw new FS.ErrnoError(errCode, parent);
        }
        var hash = FS.hashName(parent.id, name);
        for (var node = FS.nameTable[hash]; node; node = node.name_next) {
          var nodeName = node.name;
          if (node.parent.id === parent.id && nodeName === name) {
            return node;
          }
        }
        // if we failed to find it in the cache, call into the VFS
        return FS.lookup(parent, name);
      },createNode:(parent, name, mode, rdev) => {
        assert(typeof parent == 'object')
        var node = new FS.FSNode(parent, name, mode, rdev);
  
        FS.hashAddNode(node);
  
        return node;
      },destroyNode:(node) => {
        FS.hashRemoveNode(node);
      },isRoot:(node) => {
        return node === node.parent;
      },isMountpoint:(node) => {
        return !!node.mounted;
      },isFile:(mode) => {
        return (mode & 61440) === 32768;
      },isDir:(mode) => {
        return (mode & 61440) === 16384;
      },isLink:(mode) => {
        return (mode & 61440) === 40960;
      },isChrdev:(mode) => {
        return (mode & 61440) === 8192;
      },isBlkdev:(mode) => {
        return (mode & 61440) === 24576;
      },isFIFO:(mode) => {
        return (mode & 61440) === 4096;
      },isSocket:(mode) => {
        return (mode & 49152) === 49152;
      },flagModes:{"r":0,"r+":2,"w":577,"w+":578,"a":1089,"a+":1090},modeStringToFlags:(str) => {
        var flags = FS.flagModes[str];
        if (typeof flags == 'undefined') {
          throw new Error('Unknown file open mode: ' + str);
        }
        return flags;
      },flagsToPermissionString:(flag) => {
        var perms = ['r', 'w', 'rw'][flag & 3];
        if ((flag & 512)) {
          perms += 'w';
        }
        return perms;
      },nodePermissions:(node, perms) => {
        if (FS.ignorePermissions) {
          return 0;
        }
        // return 0 if any user, group or owner bits are set.
        if (perms.includes('r') && !(node.mode & 292)) {
          return 2;
        } else if (perms.includes('w') && !(node.mode & 146)) {
          return 2;
        } else if (perms.includes('x') && !(node.mode & 73)) {
          return 2;
        }
        return 0;
      },mayLookup:(dir) => {
        var errCode = FS.nodePermissions(dir, 'x');
        if (errCode) return errCode;
        if (!dir.node_ops.lookup) return 2;
        return 0;
      },mayCreate:(dir, name) => {
        try {
          var node = FS.lookupNode(dir, name);
          return 20;
        } catch (e) {
        }
        return FS.nodePermissions(dir, 'wx');
      },mayDelete:(dir, name, isdir) => {
        var node;
        try {
          node = FS.lookupNode(dir, name);
        } catch (e) {
          return e.errno;
        }
        var errCode = FS.nodePermissions(dir, 'wx');
        if (errCode) {
          return errCode;
        }
        if (isdir) {
          if (!FS.isDir(node.mode)) {
            return 54;
          }
          if (FS.isRoot(node) || FS.getPath(node) === FS.cwd()) {
            return 10;
          }
        } else {
          if (FS.isDir(node.mode)) {
            return 31;
          }
        }
        return 0;
      },mayOpen:(node, flags) => {
        if (!node) {
          return 44;
        }
        if (FS.isLink(node.mode)) {
          return 32;
        } else if (FS.isDir(node.mode)) {
          if (FS.flagsToPermissionString(flags) !== 'r' || // opening for write
              (flags & 512)) { // TODO: check for O_SEARCH? (== search for dir only)
            return 31;
          }
        }
        return FS.nodePermissions(node, FS.flagsToPermissionString(flags));
      },MAX_OPEN_FDS:4096,nextfd:(fd_start = 0, fd_end = FS.MAX_OPEN_FDS) => {
        for (var fd = fd_start; fd <= fd_end; fd++) {
          if (!FS.streams[fd]) {
            return fd;
          }
        }
        throw new FS.ErrnoError(33);
      },getStream:(fd) => FS.streams[fd],createStream:(stream, fd_start, fd_end) => {
        if (!FS.FSStream) {
          FS.FSStream = /** @constructor */ function() {
            this.shared = { };
          };
          FS.FSStream.prototype = {};
          Object.defineProperties(FS.FSStream.prototype, {
            object: {
              /** @this {FS.FSStream} */
              get: function() { return this.node; },
              /** @this {FS.FSStream} */
              set: function(val) { this.node = val; }
            },
            isRead: {
              /** @this {FS.FSStream} */
              get: function() { return (this.flags & 2097155) !== 1; }
            },
            isWrite: {
              /** @this {FS.FSStream} */
              get: function() { return (this.flags & 2097155) !== 0; }
            },
            isAppend: {
              /** @this {FS.FSStream} */
              get: function() { return (this.flags & 1024); }
            },
            flags: {
              /** @this {FS.FSStream} */
              get: function() { return this.shared.flags; },
              /** @this {FS.FSStream} */
              set: function(val) { this.shared.flags = val; },
            },
            position : {
              /** @this {FS.FSStream} */
              get: function() { return this.shared.position; },
              /** @this {FS.FSStream} */
              set: function(val) { this.shared.position = val; },
            },
          });
        }
        // clone it, so we can return an instance of FSStream
        stream = Object.assign(new FS.FSStream(), stream);
        var fd = FS.nextfd(fd_start, fd_end);
        stream.fd = fd;
        FS.streams[fd] = stream;
        return stream;
      },closeStream:(fd) => {
        FS.streams[fd] = null;
      },chrdev_stream_ops:{open:(stream) => {
          var device = FS.getDevice(stream.node.rdev);
          // override node's stream ops with the device's
          stream.stream_ops = device.stream_ops;
          // forward the open call
          if (stream.stream_ops.open) {
            stream.stream_ops.open(stream);
          }
        },llseek:() => {
          throw new FS.ErrnoError(70);
        }},major:(dev) => ((dev) >> 8),minor:(dev) => ((dev) & 0xff),makedev:(ma, mi) => ((ma) << 8 | (mi)),registerDevice:(dev, ops) => {
        FS.devices[dev] = { stream_ops: ops };
      },getDevice:(dev) => FS.devices[dev],getMounts:(mount) => {
        var mounts = [];
        var check = [mount];
  
        while (check.length) {
          var m = check.pop();
  
          mounts.push(m);
  
          check.push.apply(check, m.mounts);
        }
  
        return mounts;
      },syncfs:(populate, callback) => {
        if (typeof populate == 'function') {
          callback = populate;
          populate = false;
        }
  
        FS.syncFSRequests++;
  
        if (FS.syncFSRequests > 1) {
          err('warning: ' + FS.syncFSRequests + ' FS.syncfs operations in flight at once, probably just doing extra work');
        }
  
        var mounts = FS.getMounts(FS.root.mount);
        var completed = 0;
  
        function doCallback(errCode) {
          assert(FS.syncFSRequests > 0);
          FS.syncFSRequests--;
          return callback(errCode);
        }
  
        function done(errCode) {
          if (errCode) {
            if (!done.errored) {
              done.errored = true;
              return doCallback(errCode);
            }
            return;
          }
          if (++completed >= mounts.length) {
            doCallback(null);
          }
        };
  
        // sync all mounts
        mounts.forEach((mount) => {
          if (!mount.type.syncfs) {
            return done(null);
          }
          mount.type.syncfs(mount, populate, done);
        });
      },mount:(type, opts, mountpoint) => {
        if (typeof type == 'string') {
          // The filesystem was not included, and instead we have an error
          // message stored in the variable.
          throw type;
        }
        var root = mountpoint === '/';
        var pseudo = !mountpoint;
        var node;
  
        if (root && FS.root) {
          throw new FS.ErrnoError(10);
        } else if (!root && !pseudo) {
          var lookup = FS.lookupPath(mountpoint, { follow_mount: false });
  
          mountpoint = lookup.path;  // use the absolute path
          node = lookup.node;
  
          if (FS.isMountpoint(node)) {
            throw new FS.ErrnoError(10);
          }
  
          if (!FS.isDir(node.mode)) {
            throw new FS.ErrnoError(54);
          }
        }
  
        var mount = {
          type: type,
          opts: opts,
          mountpoint: mountpoint,
          mounts: []
        };
  
        // create a root node for the fs
        var mountRoot = type.mount(mount);
        mountRoot.mount = mount;
        mount.root = mountRoot;
  
        if (root) {
          FS.root = mountRoot;
        } else if (node) {
          // set as a mountpoint
          node.mounted = mount;
  
          // add the new mount to the current mount's children
          if (node.mount) {
            node.mount.mounts.push(mount);
          }
        }
  
        return mountRoot;
      },unmount:(mountpoint) => {
        var lookup = FS.lookupPath(mountpoint, { follow_mount: false });
  
        if (!FS.isMountpoint(lookup.node)) {
          throw new FS.ErrnoError(28);
        }
  
        // destroy the nodes for this mount, and all its child mounts
        var node = lookup.node;
        var mount = node.mounted;
        var mounts = FS.getMounts(mount);
  
        Object.keys(FS.nameTable).forEach((hash) => {
          var current = FS.nameTable[hash];
  
          while (current) {
            var next = current.name_next;
  
            if (mounts.includes(current.mount)) {
              FS.destroyNode(current);
            }
  
            current = next;
          }
        });
  
        // no longer a mountpoint
        node.mounted = null;
  
        // remove this mount from the child mounts
        var idx = node.mount.mounts.indexOf(mount);
        assert(idx !== -1);
        node.mount.mounts.splice(idx, 1);
      },lookup:(parent, name) => {
        return parent.node_ops.lookup(parent, name);
      },mknod:(path, mode, dev) => {
        var lookup = FS.lookupPath(path, { parent: true });
        var parent = lookup.node;
        var name = PATH.basename(path);
        if (!name || name === '.' || name === '..') {
          throw new FS.ErrnoError(28);
        }
        var errCode = FS.mayCreate(parent, name);
        if (errCode) {
          throw new FS.ErrnoError(errCode);
        }
        if (!parent.node_ops.mknod) {
          throw new FS.ErrnoError(63);
        }
        return parent.node_ops.mknod(parent, name, mode, dev);
      },create:(path, mode) => {
        mode = mode !== undefined ? mode : 438 /* 0666 */;
        mode &= 4095;
        mode |= 32768;
        return FS.mknod(path, mode, 0);
      },mkdir:(path, mode) => {
        mode = mode !== undefined ? mode : 511 /* 0777 */;
        mode &= 511 | 512;
        mode |= 16384;
        return FS.mknod(path, mode, 0);
      },mkdirTree:(path, mode) => {
        var dirs = path.split('/');
        var d = '';
        for (var i = 0; i < dirs.length; ++i) {
          if (!dirs[i]) continue;
          d += '/' + dirs[i];
          try {
            FS.mkdir(d, mode);
          } catch(e) {
            if (e.errno != 20) throw e;
          }
        }
      },mkdev:(path, mode, dev) => {
        if (typeof dev == 'undefined') {
          dev = mode;
          mode = 438 /* 0666 */;
        }
        mode |= 8192;
        return FS.mknod(path, mode, dev);
      },symlink:(oldpath, newpath) => {
        if (!PATH_FS.resolve(oldpath)) {
          throw new FS.ErrnoError(44);
        }
        var lookup = FS.lookupPath(newpath, { parent: true });
        var parent = lookup.node;
        if (!parent) {
          throw new FS.ErrnoError(44);
        }
        var newname = PATH.basename(newpath);
        var errCode = FS.mayCreate(parent, newname);
        if (errCode) {
          throw new FS.ErrnoError(errCode);
        }
        if (!parent.node_ops.symlink) {
          throw new FS.ErrnoError(63);
        }
        return parent.node_ops.symlink(parent, newname, oldpath);
      },rename:(old_path, new_path) => {
        var old_dirname = PATH.dirname(old_path);
        var new_dirname = PATH.dirname(new_path);
        var old_name = PATH.basename(old_path);
        var new_name = PATH.basename(new_path);
        // parents must exist
        var lookup, old_dir, new_dir;
  
        // let the errors from non existant directories percolate up
        lookup = FS.lookupPath(old_path, { parent: true });
        old_dir = lookup.node;
        lookup = FS.lookupPath(new_path, { parent: true });
        new_dir = lookup.node;
  
        if (!old_dir || !new_dir) throw new FS.ErrnoError(44);
        // need to be part of the same mount
        if (old_dir.mount !== new_dir.mount) {
          throw new FS.ErrnoError(75);
        }
        // source must exist
        var old_node = FS.lookupNode(old_dir, old_name);
        // old path should not be an ancestor of the new path
        var relative = PATH_FS.relative(old_path, new_dirname);
        if (relative.charAt(0) !== '.') {
          throw new FS.ErrnoError(28);
        }
        // new path should not be an ancestor of the old path
        relative = PATH_FS.relative(new_path, old_dirname);
        if (relative.charAt(0) !== '.') {
          throw new FS.ErrnoError(55);
        }
        // see if the new path already exists
        var new_node;
        try {
          new_node = FS.lookupNode(new_dir, new_name);
        } catch (e) {
          // not fatal
        }
        // early out if nothing needs to change
        if (old_node === new_node) {
          return;
        }
        // we'll need to delete the old entry
        var isdir = FS.isDir(old_node.mode);
        var errCode = FS.mayDelete(old_dir, old_name, isdir);
        if (errCode) {
          throw new FS.ErrnoError(errCode);
        }
        // need delete permissions if we'll be overwriting.
        // need create permissions if new doesn't already exist.
        errCode = new_node ?
          FS.mayDelete(new_dir, new_name, isdir) :
          FS.mayCreate(new_dir, new_name);
        if (errCode) {
          throw new FS.ErrnoError(errCode);
        }
        if (!old_dir.node_ops.rename) {
          throw new FS.ErrnoError(63);
        }
        if (FS.isMountpoint(old_node) || (new_node && FS.isMountpoint(new_node))) {
          throw new FS.ErrnoError(10);
        }
        // if we are going to change the parent, check write permissions
        if (new_dir !== old_dir) {
          errCode = FS.nodePermissions(old_dir, 'w');
          if (errCode) {
            throw new FS.ErrnoError(errCode);
          }
        }
        // remove the node from the lookup hash
        FS.hashRemoveNode(old_node);
        // do the underlying fs rename
        try {
          old_dir.node_ops.rename(old_node, new_dir, new_name);
        } catch (e) {
          throw e;
        } finally {
          // add the node back to the hash (in case node_ops.rename
          // changed its name)
          FS.hashAddNode(old_node);
        }
      },rmdir:(path) => {
        var lookup = FS.lookupPath(path, { parent: true });
        var parent = lookup.node;
        var name = PATH.basename(path);
        var node = FS.lookupNode(parent, name);
        var errCode = FS.mayDelete(parent, name, true);
        if (errCode) {
          throw new FS.ErrnoError(errCode);
        }
        if (!parent.node_ops.rmdir) {
          throw new FS.ErrnoError(63);
        }
        if (FS.isMountpoint(node)) {
          throw new FS.ErrnoError(10);
        }
        parent.node_ops.rmdir(parent, name);
        FS.destroyNode(node);
      },readdir:(path) => {
        var lookup = FS.lookupPath(path, { follow: true });
        var node = lookup.node;
        if (!node.node_ops.readdir) {
          throw new FS.ErrnoError(54);
        }
        return node.node_ops.readdir(node);
      },unlink:(path) => {
        var lookup = FS.lookupPath(path, { parent: true });
        var parent = lookup.node;
        if (!parent) {
          throw new FS.ErrnoError(44);
        }
        var name = PATH.basename(path);
        var node = FS.lookupNode(parent, name);
        var errCode = FS.mayDelete(parent, name, false);
        if (errCode) {
          // According to POSIX, we should map EISDIR to EPERM, but
          // we instead do what Linux does (and we must, as we use
          // the musl linux libc).
          throw new FS.ErrnoError(errCode);
        }
        if (!parent.node_ops.unlink) {
          throw new FS.ErrnoError(63);
        }
        if (FS.isMountpoint(node)) {
          throw new FS.ErrnoError(10);
        }
        parent.node_ops.unlink(parent, name);
        FS.destroyNode(node);
      },readlink:(path) => {
        var lookup = FS.lookupPath(path);
        var link = lookup.node;
        if (!link) {
          throw new FS.ErrnoError(44);
        }
        if (!link.node_ops.readlink) {
          throw new FS.ErrnoError(28);
        }
        return PATH_FS.resolve(FS.getPath(link.parent), link.node_ops.readlink(link));
      },stat:(path, dontFollow) => {
        var lookup = FS.lookupPath(path, { follow: !dontFollow });
        var node = lookup.node;
        if (!node) {
          throw new FS.ErrnoError(44);
        }
        if (!node.node_ops.getattr) {
          throw new FS.ErrnoError(63);
        }
        return node.node_ops.getattr(node);
      },lstat:(path) => {
        return FS.stat(path, true);
      },chmod:(path, mode, dontFollow) => {
        var node;
        if (typeof path == 'string') {
          var lookup = FS.lookupPath(path, { follow: !dontFollow });
          node = lookup.node;
        } else {
          node = path;
        }
        if (!node.node_ops.setattr) {
          throw new FS.ErrnoError(63);
        }
        node.node_ops.setattr(node, {
          mode: (mode & 4095) | (node.mode & ~4095),
          timestamp: Date.now()
        });
      },lchmod:(path, mode) => {
        FS.chmod(path, mode, true);
      },fchmod:(fd, mode) => {
        var stream = FS.getStream(fd);
        if (!stream) {
          throw new FS.ErrnoError(8);
        }
        FS.chmod(stream.node, mode);
      },chown:(path, uid, gid, dontFollow) => {
        var node;
        if (typeof path == 'string') {
          var lookup = FS.lookupPath(path, { follow: !dontFollow });
          node = lookup.node;
        } else {
          node = path;
        }
        if (!node.node_ops.setattr) {
          throw new FS.ErrnoError(63);
        }
        node.node_ops.setattr(node, {
          timestamp: Date.now()
          // we ignore the uid / gid for now
        });
      },lchown:(path, uid, gid) => {
        FS.chown(path, uid, gid, true);
      },fchown:(fd, uid, gid) => {
        var stream = FS.getStream(fd);
        if (!stream) {
          throw new FS.ErrnoError(8);
        }
        FS.chown(stream.node, uid, gid);
      },truncate:(path, len) => {
        if (len < 0) {
          throw new FS.ErrnoError(28);
        }
        var node;
        if (typeof path == 'string') {
          var lookup = FS.lookupPath(path, { follow: true });
          node = lookup.node;
        } else {
          node = path;
        }
        if (!node.node_ops.setattr) {
          throw new FS.ErrnoError(63);
        }
        if (FS.isDir(node.mode)) {
          throw new FS.ErrnoError(31);
        }
        if (!FS.isFile(node.mode)) {
          throw new FS.ErrnoError(28);
        }
        var errCode = FS.nodePermissions(node, 'w');
        if (errCode) {
          throw new FS.ErrnoError(errCode);
        }
        node.node_ops.setattr(node, {
          size: len,
          timestamp: Date.now()
        });
      },ftruncate:(fd, len) => {
        var stream = FS.getStream(fd);
        if (!stream) {
          throw new FS.ErrnoError(8);
        }
        if ((stream.flags & 2097155) === 0) {
          throw new FS.ErrnoError(28);
        }
        FS.truncate(stream.node, len);
      },utime:(path, atime, mtime) => {
        var lookup = FS.lookupPath(path, { follow: true });
        var node = lookup.node;
        node.node_ops.setattr(node, {
          timestamp: Math.max(atime, mtime)
        });
      },open:(path, flags, mode) => {
        if (path === "") {
          throw new FS.ErrnoError(44);
        }
        flags = typeof flags == 'string' ? FS.modeStringToFlags(flags) : flags;
        mode = typeof mode == 'undefined' ? 438 /* 0666 */ : mode;
        if ((flags & 64)) {
          mode = (mode & 4095) | 32768;
        } else {
          mode = 0;
        }
        var node;
        if (typeof path == 'object') {
          node = path;
        } else {
          path = PATH.normalize(path);
          try {
            var lookup = FS.lookupPath(path, {
              follow: !(flags & 131072)
            });
            node = lookup.node;
          } catch (e) {
            // ignore
          }
        }
        // perhaps we need to create the node
        var created = false;
        if ((flags & 64)) {
          if (node) {
            // if O_CREAT and O_EXCL are set, error out if the node already exists
            if ((flags & 128)) {
              throw new FS.ErrnoError(20);
            }
          } else {
            // node doesn't exist, try to create it
            node = FS.mknod(path, mode, 0);
            created = true;
          }
        }
        if (!node) {
          throw new FS.ErrnoError(44);
        }
        // can't truncate a device
        if (FS.isChrdev(node.mode)) {
          flags &= ~512;
        }
        // if asked only for a directory, then this must be one
        if ((flags & 65536) && !FS.isDir(node.mode)) {
          throw new FS.ErrnoError(54);
        }
        // check permissions, if this is not a file we just created now (it is ok to
        // create and write to a file with read-only permissions; it is read-only
        // for later use)
        if (!created) {
          var errCode = FS.mayOpen(node, flags);
          if (errCode) {
            throw new FS.ErrnoError(errCode);
          }
        }
        // do truncation if necessary
        if ((flags & 512) && !created) {
          FS.truncate(node, 0);
        }
        // we've already handled these, don't pass down to the underlying vfs
        flags &= ~(128 | 512 | 131072);
  
        // register the stream with the filesystem
        var stream = FS.createStream({
          node: node,
          path: FS.getPath(node),  // we want the absolute path to the node
          flags: flags,
          seekable: true,
          position: 0,
          stream_ops: node.stream_ops,
          // used by the file family libc calls (fopen, fwrite, ferror, etc.)
          ungotten: [],
          error: false
        });
        // call the new stream's open function
        if (stream.stream_ops.open) {
          stream.stream_ops.open(stream);
        }
        if (Module['logReadFiles'] && !(flags & 1)) {
          if (!FS.readFiles) FS.readFiles = {};
          if (!(path in FS.readFiles)) {
            FS.readFiles[path] = 1;
          }
        }
        return stream;
      },close:(stream) => {
        if (FS.isClosed(stream)) {
          throw new FS.ErrnoError(8);
        }
        if (stream.getdents) stream.getdents = null; // free readdir state
        try {
          if (stream.stream_ops.close) {
            stream.stream_ops.close(stream);
          }
        } catch (e) {
          throw e;
        } finally {
          FS.closeStream(stream.fd);
        }
        stream.fd = null;
      },isClosed:(stream) => {
        return stream.fd === null;
      },llseek:(stream, offset, whence) => {
        if (FS.isClosed(stream)) {
          throw new FS.ErrnoError(8);
        }
        if (!stream.seekable || !stream.stream_ops.llseek) {
          throw new FS.ErrnoError(70);
        }
        if (whence != 0 && whence != 1 && whence != 2) {
          throw new FS.ErrnoError(28);
        }
        stream.position = stream.stream_ops.llseek(stream, offset, whence);
        stream.ungotten = [];
        return stream.position;
      },read:(stream, buffer, offset, length, position) => {
        if (length < 0 || position < 0) {
          throw new FS.ErrnoError(28);
        }
        if (FS.isClosed(stream)) {
          throw new FS.ErrnoError(8);
        }
        if ((stream.flags & 2097155) === 1) {
          throw new FS.ErrnoError(8);
        }
        if (FS.isDir(stream.node.mode)) {
          throw new FS.ErrnoError(31);
        }
        if (!stream.stream_ops.read) {
          throw new FS.ErrnoError(28);
        }
        var seeking = typeof position != 'undefined';
        if (!seeking) {
          position = stream.position;
        } else if (!stream.seekable) {
          throw new FS.ErrnoError(70);
        }
        var bytesRead = stream.stream_ops.read(stream, buffer, offset, length, position);
        if (!seeking) stream.position += bytesRead;
        return bytesRead;
      },write:(stream, buffer, offset, length, position, canOwn) => {
        if (length < 0 || position < 0) {
          throw new FS.ErrnoError(28);
        }
        if (FS.isClosed(stream)) {
          throw new FS.ErrnoError(8);
        }
        if ((stream.flags & 2097155) === 0) {
          throw new FS.ErrnoError(8);
        }
        if (FS.isDir(stream.node.mode)) {
          throw new FS.ErrnoError(31);
        }
        if (!stream.stream_ops.write) {
          throw new FS.ErrnoError(28);
        }
        if (stream.seekable && stream.flags & 1024) {
          // seek to the end before writing in append mode
          FS.llseek(stream, 0, 2);
        }
        var seeking = typeof position != 'undefined';
        if (!seeking) {
          position = stream.position;
        } else if (!stream.seekable) {
          throw new FS.ErrnoError(70);
        }
        var bytesWritten = stream.stream_ops.write(stream, buffer, offset, length, position, canOwn);
        if (!seeking) stream.position += bytesWritten;
        return bytesWritten;
      },allocate:(stream, offset, length) => {
        if (FS.isClosed(stream)) {
          throw new FS.ErrnoError(8);
        }
        if (offset < 0 || length <= 0) {
          throw new FS.ErrnoError(28);
        }
        if ((stream.flags & 2097155) === 0) {
          throw new FS.ErrnoError(8);
        }
        if (!FS.isFile(stream.node.mode) && !FS.isDir(stream.node.mode)) {
          throw new FS.ErrnoError(43);
        }
        if (!stream.stream_ops.allocate) {
          throw new FS.ErrnoError(138);
        }
        stream.stream_ops.allocate(stream, offset, length);
      },mmap:(stream, length, position, prot, flags) => {
        // User requests writing to file (prot & PROT_WRITE != 0).
        // Checking if we have permissions to write to the file unless
        // MAP_PRIVATE flag is set. According to POSIX spec it is possible
        // to write to file opened in read-only mode with MAP_PRIVATE flag,
        // as all modifications will be visible only in the memory of
        // the current process.
        if ((prot & 2) !== 0
            && (flags & 2) === 0
            && (stream.flags & 2097155) !== 2) {
          throw new FS.ErrnoError(2);
        }
        if ((stream.flags & 2097155) === 1) {
          throw new FS.ErrnoError(2);
        }
        if (!stream.stream_ops.mmap) {
          throw new FS.ErrnoError(43);
        }
        return stream.stream_ops.mmap(stream, length, position, prot, flags);
      },msync:(stream, buffer, offset, length, mmapFlags) => {
        if (!stream.stream_ops.msync) {
          return 0;
        }
        return stream.stream_ops.msync(stream, buffer, offset, length, mmapFlags);
      },munmap:(stream) => 0,ioctl:(stream, cmd, arg) => {
        if (!stream.stream_ops.ioctl) {
          throw new FS.ErrnoError(59);
        }
        return stream.stream_ops.ioctl(stream, cmd, arg);
      },readFile:(path, opts = {}) => {
        opts.flags = opts.flags || 0;
        opts.encoding = opts.encoding || 'binary';
        if (opts.encoding !== 'utf8' && opts.encoding !== 'binary') {
          throw new Error('Invalid encoding type "' + opts.encoding + '"');
        }
        var ret;
        var stream = FS.open(path, opts.flags);
        var stat = FS.stat(path);
        var length = stat.size;
        var buf = new Uint8Array(length);
        FS.read(stream, buf, 0, length, 0);
        if (opts.encoding === 'utf8') {
          ret = UTF8ArrayToString(buf, 0);
        } else if (opts.encoding === 'binary') {
          ret = buf;
        }
        FS.close(stream);
        return ret;
      },writeFile:(path, data, opts = {}) => {
        opts.flags = opts.flags || 577;
        var stream = FS.open(path, opts.flags, opts.mode);
        if (typeof data == 'string') {
          var buf = new Uint8Array(lengthBytesUTF8(data)+1);
          var actualNumBytes = stringToUTF8Array(data, buf, 0, buf.length);
          FS.write(stream, buf, 0, actualNumBytes, undefined, opts.canOwn);
        } else if (ArrayBuffer.isView(data)) {
          FS.write(stream, data, 0, data.byteLength, undefined, opts.canOwn);
        } else {
          throw new Error('Unsupported data type');
        }
        FS.close(stream);
      },cwd:() => FS.currentPath,chdir:(path) => {
        var lookup = FS.lookupPath(path, { follow: true });
        if (lookup.node === null) {
          throw new FS.ErrnoError(44);
        }
        if (!FS.isDir(lookup.node.mode)) {
          throw new FS.ErrnoError(54);
        }
        var errCode = FS.nodePermissions(lookup.node, 'x');
        if (errCode) {
          throw new FS.ErrnoError(errCode);
        }
        FS.currentPath = lookup.path;
      },createDefaultDirectories:() => {
        FS.mkdir('/tmp');
        FS.mkdir('/home');
        FS.mkdir('/home/web_user');
      },createDefaultDevices:() => {
        // create /dev
        FS.mkdir('/dev');
        // setup /dev/null
        FS.registerDevice(FS.makedev(1, 3), {
          read: () => 0,
          write: (stream, buffer, offset, length, pos) => length,
        });
        FS.mkdev('/dev/null', FS.makedev(1, 3));
        // setup /dev/tty and /dev/tty1
        // stderr needs to print output using err() rather than out()
        // so we register a second tty just for it.
        TTY.register(FS.makedev(5, 0), TTY.default_tty_ops);
        TTY.register(FS.makedev(6, 0), TTY.default_tty1_ops);
        FS.mkdev('/dev/tty', FS.makedev(5, 0));
        FS.mkdev('/dev/tty1', FS.makedev(6, 0));
        // setup /dev/[u]random
        var random_device = getRandomDevice();
        FS.createDevice('/dev', 'random', random_device);
        FS.createDevice('/dev', 'urandom', random_device);
        // we're not going to emulate the actual shm device,
        // just create the tmp dirs that reside in it commonly
        FS.mkdir('/dev/shm');
        FS.mkdir('/dev/shm/tmp');
      },createSpecialDirectories:() => {
        // create /proc/self/fd which allows /proc/self/fd/6 => readlink gives the
        // name of the stream for fd 6 (see test_unistd_ttyname)
        FS.mkdir('/proc');
        var proc_self = FS.mkdir('/proc/self');
        FS.mkdir('/proc/self/fd');
        FS.mount({
          mount: () => {
            var node = FS.createNode(proc_self, 'fd', 16384 | 511 /* 0777 */, 73);
            node.node_ops = {
              lookup: (parent, name) => {
                var fd = +name;
                var stream = FS.getStream(fd);
                if (!stream) throw new FS.ErrnoError(8);
                var ret = {
                  parent: null,
                  mount: { mountpoint: 'fake' },
                  node_ops: { readlink: () => stream.path },
                };
                ret.parent = ret; // make it look like a simple root node
                return ret;
              }
            };
            return node;
          }
        }, {}, '/proc/self/fd');
      },createStandardStreams:() => {
        // TODO deprecate the old functionality of a single
        // input / output callback and that utilizes FS.createDevice
        // and instead require a unique set of stream ops
  
        // by default, we symlink the standard streams to the
        // default tty devices. however, if the standard streams
        // have been overwritten we create a unique device for
        // them instead.
        if (Module['stdin']) {
          FS.createDevice('/dev', 'stdin', Module['stdin']);
        } else {
          FS.symlink('/dev/tty', '/dev/stdin');
        }
        if (Module['stdout']) {
          FS.createDevice('/dev', 'stdout', null, Module['stdout']);
        } else {
          FS.symlink('/dev/tty', '/dev/stdout');
        }
        if (Module['stderr']) {
          FS.createDevice('/dev', 'stderr', null, Module['stderr']);
        } else {
          FS.symlink('/dev/tty1', '/dev/stderr');
        }
  
        // open default streams for the stdin, stdout and stderr devices
        var stdin = FS.open('/dev/stdin', 0);
        var stdout = FS.open('/dev/stdout', 1);
        var stderr = FS.open('/dev/stderr', 1);
        assert(stdin.fd === 0, 'invalid handle for stdin (' + stdin.fd + ')');
        assert(stdout.fd === 1, 'invalid handle for stdout (' + stdout.fd + ')');
        assert(stderr.fd === 2, 'invalid handle for stderr (' + stderr.fd + ')');
      },ensureErrnoError:() => {
        if (FS.ErrnoError) return;
        FS.ErrnoError = /** @this{Object} */ function ErrnoError(errno, node) {
          // We set the `name` property to be able to identify `FS.ErrnoError`
          // - the `name` is a standard ECMA-262 property of error objects. Kind of good to have it anyway.
          // - when using PROXYFS, an error can come from an underlying FS
          // as different FS objects have their own FS.ErrnoError each,
          // the test `err instanceof FS.ErrnoError` won't detect an error coming from another filesystem, causing bugs.
          // we'll use the reliable test `err.name == "ErrnoError"` instead
          this.name = 'ErrnoError';
          this.node = node;
          this.setErrno = /** @this{Object} */ function(errno) {
            this.errno = errno;
            for (var key in ERRNO_CODES) {
              if (ERRNO_CODES[key] === errno) {
                this.code = key;
                break;
              }
            }
          };
          this.setErrno(errno);
          this.message = ERRNO_MESSAGES[errno];
  
          // Try to get a maximally helpful stack trace. On Node.js, getting Error.stack
          // now ensures it shows what we want.
          if (this.stack) {
            // Define the stack property for Node.js 4, which otherwise errors on the next line.
            Object.defineProperty(this, "stack", { value: (new Error).stack, writable: true });
            this.stack = demangleAll(this.stack);
          }
        };
        FS.ErrnoError.prototype = new Error();
        FS.ErrnoError.prototype.constructor = FS.ErrnoError;
        // Some errors may happen quite a bit, to avoid overhead we reuse them (and suffer a lack of stack info)
        [44].forEach((code) => {
          FS.genericErrors[code] = new FS.ErrnoError(code);
          FS.genericErrors[code].stack = '<generic error, no stack>';
        });
      },staticInit:() => {
        FS.ensureErrnoError();
  
        FS.nameTable = new Array(4096);
  
        FS.mount(MEMFS, {}, '/');
  
        FS.createDefaultDirectories();
        FS.createDefaultDevices();
        FS.createSpecialDirectories();
  
        FS.filesystems = {
          'MEMFS': MEMFS,
        };
      },init:(input, output, error) => {
        assert(!FS.init.initialized, 'FS.init was previously called. If you want to initialize later with custom parameters, remove any earlier calls (note that one is automatically added to the generated code)');
        FS.init.initialized = true;
  
        FS.ensureErrnoError();
  
        // Allow Module.stdin etc. to provide defaults, if none explicitly passed to us here
        Module['stdin'] = input || Module['stdin'];
        Module['stdout'] = output || Module['stdout'];
        Module['stderr'] = error || Module['stderr'];
  
        FS.createStandardStreams();
      },quit:() => {
        FS.init.initialized = false;
        // force-flush all streams, so we get musl std streams printed out
        _fflush(0);
        // close all of our streams
        for (var i = 0; i < FS.streams.length; i++) {
          var stream = FS.streams[i];
          if (!stream) {
            continue;
          }
          FS.close(stream);
        }
      },getMode:(canRead, canWrite) => {
        var mode = 0;
        if (canRead) mode |= 292 | 73;
        if (canWrite) mode |= 146;
        return mode;
      },findObject:(path, dontResolveLastLink) => {
        var ret = FS.analyzePath(path, dontResolveLastLink);
        if (!ret.exists) {
          return null;
        }
        return ret.object;
      },analyzePath:(path, dontResolveLastLink) => {
        // operate from within the context of the symlink's target
        try {
          var lookup = FS.lookupPath(path, { follow: !dontResolveLastLink });
          path = lookup.path;
        } catch (e) {
        }
        var ret = {
          isRoot: false, exists: false, error: 0, name: null, path: null, object: null,
          parentExists: false, parentPath: null, parentObject: null
        };
        try {
          var lookup = FS.lookupPath(path, { parent: true });
          ret.parentExists = true;
          ret.parentPath = lookup.path;
          ret.parentObject = lookup.node;
          ret.name = PATH.basename(path);
          lookup = FS.lookupPath(path, { follow: !dontResolveLastLink });
          ret.exists = true;
          ret.path = lookup.path;
          ret.object = lookup.node;
          ret.name = lookup.node.name;
          ret.isRoot = lookup.path === '/';
        } catch (e) {
          ret.error = e.errno;
        };
        return ret;
      },createPath:(parent, path, canRead, canWrite) => {
        parent = typeof parent == 'string' ? parent : FS.getPath(parent);
        var parts = path.split('/').reverse();
        while (parts.length) {
          var part = parts.pop();
          if (!part) continue;
          var current = PATH.join2(parent, part);
          try {
            FS.mkdir(current);
          } catch (e) {
            // ignore EEXIST
          }
          parent = current;
        }
        return current;
      },createFile:(parent, name, properties, canRead, canWrite) => {
        var path = PATH.join2(typeof parent == 'string' ? parent : FS.getPath(parent), name);
        var mode = FS.getMode(canRead, canWrite);
        return FS.create(path, mode);
      },createDataFile:(parent, name, data, canRead, canWrite, canOwn) => {
        var path = name;
        if (parent) {
          parent = typeof parent == 'string' ? parent : FS.getPath(parent);
          path = name ? PATH.join2(parent, name) : parent;
        }
        var mode = FS.getMode(canRead, canWrite);
        var node = FS.create(path, mode);
        if (data) {
          if (typeof data == 'string') {
            var arr = new Array(data.length);
            for (var i = 0, len = data.length; i < len; ++i) arr[i] = data.charCodeAt(i);
            data = arr;
          }
          // make sure we can write to the file
          FS.chmod(node, mode | 146);
          var stream = FS.open(node, 577);
          FS.write(stream, data, 0, data.length, 0, canOwn);
          FS.close(stream);
          FS.chmod(node, mode);
        }
        return node;
      },createDevice:(parent, name, input, output) => {
        var path = PATH.join2(typeof parent == 'string' ? parent : FS.getPath(parent), name);
        var mode = FS.getMode(!!input, !!output);
        if (!FS.createDevice.major) FS.createDevice.major = 64;
        var dev = FS.makedev(FS.createDevice.major++, 0);
        // Create a fake device that a set of stream ops to emulate
        // the old behavior.
        FS.registerDevice(dev, {
          open: (stream) => {
            stream.seekable = false;
          },
          close: (stream) => {
            // flush any pending line data
            if (output && output.buffer && output.buffer.length) {
              output(10);
            }
          },
          read: (stream, buffer, offset, length, pos /* ignored */) => {
            var bytesRead = 0;
            for (var i = 0; i < length; i++) {
              var result;
              try {
                result = input();
              } catch (e) {
                throw new FS.ErrnoError(29);
              }
              if (result === undefined && bytesRead === 0) {
                throw new FS.ErrnoError(6);
              }
              if (result === null || result === undefined) break;
              bytesRead++;
              buffer[offset+i] = result;
            }
            if (bytesRead) {
              stream.node.timestamp = Date.now();
            }
            return bytesRead;
          },
          write: (stream, buffer, offset, length, pos) => {
            for (var i = 0; i < length; i++) {
              try {
                output(buffer[offset+i]);
              } catch (e) {
                throw new FS.ErrnoError(29);
              }
            }
            if (length) {
              stream.node.timestamp = Date.now();
            }
            return i;
          }
        });
        return FS.mkdev(path, mode, dev);
      },forceLoadFile:(obj) => {
        if (obj.isDevice || obj.isFolder || obj.link || obj.contents) return true;
        if (typeof XMLHttpRequest != 'undefined') {
          throw new Error("Lazy loading should have been performed (contents set) in createLazyFile, but it was not. Lazy loading only works in web workers. Use --embed-file or --preload-file in emcc on the main thread.");
        } else if (read_) {
          // Command-line.
          try {
            // WARNING: Can't read binary files in V8's d8 or tracemonkey's js, as
            //          read() will try to parse UTF8.
            obj.contents = intArrayFromString(read_(obj.url), true);
            obj.usedBytes = obj.contents.length;
          } catch (e) {
            throw new FS.ErrnoError(29);
          }
        } else {
          throw new Error('Cannot load without read() or XMLHttpRequest.');
        }
      },createLazyFile:(parent, name, url, canRead, canWrite) => {
        // Lazy chunked Uint8Array (implements get and length from Uint8Array). Actual getting is abstracted away for eventual reuse.
        /** @constructor */
        function LazyUint8Array() {
          this.lengthKnown = false;
          this.chunks = []; // Loaded chunks. Index is the chunk number
        }
        LazyUint8Array.prototype.get = /** @this{Object} */ function LazyUint8Array_get(idx) {
          if (idx > this.length-1 || idx < 0) {
            return undefined;
          }
          var chunkOffset = idx % this.chunkSize;
          var chunkNum = (idx / this.chunkSize)|0;
          return this.getter(chunkNum)[chunkOffset];
        };
        LazyUint8Array.prototype.setDataGetter = function LazyUint8Array_setDataGetter(getter) {
          this.getter = getter;
        };
        LazyUint8Array.prototype.cacheLength = function LazyUint8Array_cacheLength() {
          // Find length
          var xhr = new XMLHttpRequest();
          xhr.open('HEAD', url, false);
          xhr.send(null);
          if (!(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304)) throw new Error("Couldn't load " + url + ". Status: " + xhr.status);
          var datalength = Number(xhr.getResponseHeader("Content-length"));
          var header;
          var hasByteServing = (header = xhr.getResponseHeader("Accept-Ranges")) && header === "bytes";
          var usesGzip = (header = xhr.getResponseHeader("Content-Encoding")) && header === "gzip";
  
          var chunkSize = 1024*1024; // Chunk size in bytes
  
          if (!hasByteServing) chunkSize = datalength;
  
          // Function to get a range from the remote URL.
          var doXHR = (from, to) => {
            if (from > to) throw new Error("invalid range (" + from + ", " + to + ") or no bytes requested!");
            if (to > datalength-1) throw new Error("only " + datalength + " bytes available! programmer error!");
  
            // TODO: Use mozResponseArrayBuffer, responseStream, etc. if available.
            var xhr = new XMLHttpRequest();
            xhr.open('GET', url, false);
            if (datalength !== chunkSize) xhr.setRequestHeader("Range", "bytes=" + from + "-" + to);
  
            // Some hints to the browser that we want binary data.
            xhr.responseType = 'arraybuffer';
            if (xhr.overrideMimeType) {
              xhr.overrideMimeType('text/plain; charset=x-user-defined');
            }
  
            xhr.send(null);
            if (!(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304)) throw new Error("Couldn't load " + url + ". Status: " + xhr.status);
            if (xhr.response !== undefined) {
              return new Uint8Array(/** @type{Array<number>} */(xhr.response || []));
            }
            return intArrayFromString(xhr.responseText || '', true);
          };
          var lazyArray = this;
          lazyArray.setDataGetter((chunkNum) => {
            var start = chunkNum * chunkSize;
            var end = (chunkNum+1) * chunkSize - 1; // including this byte
            end = Math.min(end, datalength-1); // if datalength-1 is selected, this is the last block
            if (typeof lazyArray.chunks[chunkNum] == 'undefined') {
              lazyArray.chunks[chunkNum] = doXHR(start, end);
            }
            if (typeof lazyArray.chunks[chunkNum] == 'undefined') throw new Error('doXHR failed!');
            return lazyArray.chunks[chunkNum];
          });
  
          if (usesGzip || !datalength) {
            // if the server uses gzip or doesn't supply the length, we have to download the whole file to get the (uncompressed) length
            chunkSize = datalength = 1; // this will force getter(0)/doXHR do download the whole file
            datalength = this.getter(0).length;
            chunkSize = datalength;
            out("LazyFiles on gzip forces download of the whole file when length is accessed");
          }
  
          this._length = datalength;
          this._chunkSize = chunkSize;
          this.lengthKnown = true;
        };
        if (typeof XMLHttpRequest != 'undefined') {
          if (!ENVIRONMENT_IS_WORKER) throw 'Cannot do synchronous binary XHRs outside webworkers in modern browsers. Use --embed-file or --preload-file in emcc';
          var lazyArray = new LazyUint8Array();
          Object.defineProperties(lazyArray, {
            length: {
              get: /** @this{Object} */ function() {
                if (!this.lengthKnown) {
                  this.cacheLength();
                }
                return this._length;
              }
            },
            chunkSize: {
              get: /** @this{Object} */ function() {
                if (!this.lengthKnown) {
                  this.cacheLength();
                }
                return this._chunkSize;
              }
            }
          });
  
          var properties = { isDevice: false, contents: lazyArray };
        } else {
          var properties = { isDevice: false, url: url };
        }
  
        var node = FS.createFile(parent, name, properties, canRead, canWrite);
        // This is a total hack, but I want to get this lazy file code out of the
        // core of MEMFS. If we want to keep this lazy file concept I feel it should
        // be its own thin LAZYFS proxying calls to MEMFS.
        if (properties.contents) {
          node.contents = properties.contents;
        } else if (properties.url) {
          node.contents = null;
          node.url = properties.url;
        }
        // Add a function that defers querying the file size until it is asked the first time.
        Object.defineProperties(node, {
          usedBytes: {
            get: /** @this {FSNode} */ function() { return this.contents.length; }
          }
        });
        // override each stream op with one that tries to force load the lazy file first
        var stream_ops = {};
        var keys = Object.keys(node.stream_ops);
        keys.forEach((key) => {
          var fn = node.stream_ops[key];
          stream_ops[key] = function forceLoadLazyFile() {
            FS.forceLoadFile(node);
            return fn.apply(null, arguments);
          };
        });
        function writeChunks(stream, buffer, offset, length, position) {
          var contents = stream.node.contents;
          if (position >= contents.length)
            return 0;
          var size = Math.min(contents.length - position, length);
          assert(size >= 0);
          if (contents.slice) { // normal array
            for (var i = 0; i < size; i++) {
              buffer[offset + i] = contents[position + i];
            }
          } else {
            for (var i = 0; i < size; i++) { // LazyUint8Array from sync binary XHR
              buffer[offset + i] = contents.get(position + i);
            }
          }
          return size;
        }
        // use a custom read function
        stream_ops.read = (stream, buffer, offset, length, position) => {
          FS.forceLoadFile(node);
          return writeChunks(stream, buffer, offset, length, position)
        };
        // use a custom mmap function
        stream_ops.mmap = (stream, length, position, prot, flags) => {
          FS.forceLoadFile(node);
          var ptr = mmapAlloc(length);
          if (!ptr) {
            throw new FS.ErrnoError(48);
          }
          writeChunks(stream, HEAP8, ptr, length, position);
          return { ptr: ptr, allocated: true };
        };
        node.stream_ops = stream_ops;
        return node;
      },createPreloadedFile:(parent, name, url, canRead, canWrite, onload, onerror, dontCreateFile, canOwn, preFinish) => {
        // TODO we should allow people to just pass in a complete filename instead
        // of parent and name being that we just join them anyways
        var fullname = name ? PATH_FS.resolve(PATH.join2(parent, name)) : parent;
        var dep = getUniqueRunDependency('cp ' + fullname); // might have several active requests for the same fullname
        function processData(byteArray) {
          function finish(byteArray) {
            if (preFinish) preFinish();
            if (!dontCreateFile) {
              FS.createDataFile(parent, name, byteArray, canRead, canWrite, canOwn);
            }
            if (onload) onload();
            removeRunDependency(dep);
          }
          if (Browser.handledByPreloadPlugin(byteArray, fullname, finish, () => {
            if (onerror) onerror();
            removeRunDependency(dep);
          })) {
            return;
          }
          finish(byteArray);
        }
        addRunDependency(dep);
        if (typeof url == 'string') {
          asyncLoad(url, (byteArray) => processData(byteArray), onerror);
        } else {
          processData(url);
        }
      },indexedDB:() => {
        return window.indexedDB || window.mozIndexedDB || window.webkitIndexedDB || window.msIndexedDB;
      },DB_NAME:() => {
        return 'EM_FS_' + window.location.pathname;
      },DB_VERSION:20,DB_STORE_NAME:"FILE_DATA",saveFilesToDB:(paths, onload = (() => {}), onerror = (() => {})) => {
        var indexedDB = FS.indexedDB();
        try {
          var openRequest = indexedDB.open(FS.DB_NAME(), FS.DB_VERSION);
        } catch (e) {
          return onerror(e);
        }
        openRequest.onupgradeneeded = () => {
          out('creating db');
          var db = openRequest.result;
          db.createObjectStore(FS.DB_STORE_NAME);
        };
        openRequest.onsuccess = () => {
          var db = openRequest.result;
          var transaction = db.transaction([FS.DB_STORE_NAME], 'readwrite');
          var files = transaction.objectStore(FS.DB_STORE_NAME);
          var ok = 0, fail = 0, total = paths.length;
          function finish() {
            if (fail == 0) onload(); else onerror();
          }
          paths.forEach((path) => {
            var putRequest = files.put(FS.analyzePath(path).object.contents, path);
            putRequest.onsuccess = () => { ok++; if (ok + fail == total) finish() };
            putRequest.onerror = () => { fail++; if (ok + fail == total) finish() };
          });
          transaction.onerror = onerror;
        };
        openRequest.onerror = onerror;
      },loadFilesFromDB:(paths, onload = (() => {}), onerror = (() => {})) => {
        var indexedDB = FS.indexedDB();
        try {
          var openRequest = indexedDB.open(FS.DB_NAME(), FS.DB_VERSION);
        } catch (e) {
          return onerror(e);
        }
        openRequest.onupgradeneeded = onerror; // no database to load from
        openRequest.onsuccess = () => {
          var db = openRequest.result;
          try {
            var transaction = db.transaction([FS.DB_STORE_NAME], 'readonly');
          } catch(e) {
            onerror(e);
            return;
          }
          var files = transaction.objectStore(FS.DB_STORE_NAME);
          var ok = 0, fail = 0, total = paths.length;
          function finish() {
            if (fail == 0) onload(); else onerror();
          }
          paths.forEach((path) => {
            var getRequest = files.get(path);
            getRequest.onsuccess = () => {
              if (FS.analyzePath(path).exists) {
                FS.unlink(path);
              }
              FS.createDataFile(PATH.dirname(path), PATH.basename(path), getRequest.result, true, true, true);
              ok++;
              if (ok + fail == total) finish();
            };
            getRequest.onerror = () => { fail++; if (ok + fail == total) finish() };
          });
          transaction.onerror = onerror;
        };
        openRequest.onerror = onerror;
      },absolutePath:() => {
        abort('FS.absolutePath has been removed; use PATH_FS.resolve instead');
      },createFolder:() => {
        abort('FS.createFolder has been removed; use FS.mkdir instead');
      },createLink:() => {
        abort('FS.createLink has been removed; use FS.symlink instead');
      },joinPath:() => {
        abort('FS.joinPath has been removed; use PATH.join instead');
      },mmapAlloc:() => {
        abort('FS.mmapAlloc has been replaced by the top level function mmapAlloc');
      },standardizePath:() => {
        abort('FS.standardizePath has been removed; use PATH.normalize instead');
      }};
  var SYSCALLS = {DEFAULT_POLLMASK:5,calculateAt:function(dirfd, path, allowEmpty) {
        if (PATH.isAbs(path)) {
          return path;
        }
        // relative path
        var dir;
        if (dirfd === -100) {
          dir = FS.cwd();
        } else {
          var dirstream = SYSCALLS.getStreamFromFD(dirfd);
          dir = dirstream.path;
        }
        if (path.length == 0) {
          if (!allowEmpty) {
            throw new FS.ErrnoError(44);;
          }
          return dir;
        }
        return PATH.join2(dir, path);
      },doStat:function(func, path, buf) {
        try {
          var stat = func(path);
        } catch (e) {
          if (e && e.node && PATH.normalize(path) !== PATH.normalize(FS.getPath(e.node))) {
            // an error occurred while trying to look up the path; we should just report ENOTDIR
            return -54;
          }
          throw e;
        }
        HEAP32[((buf)>>2)] = stat.dev;
        HEAP32[(((buf)+(8))>>2)] = stat.ino;
        HEAP32[(((buf)+(12))>>2)] = stat.mode;
        HEAPU32[(((buf)+(16))>>2)] = stat.nlink;
        HEAP32[(((buf)+(20))>>2)] = stat.uid;
        HEAP32[(((buf)+(24))>>2)] = stat.gid;
        HEAP32[(((buf)+(28))>>2)] = stat.rdev;
        (tempI64 = [stat.size>>>0,(tempDouble=stat.size,(+(Math.abs(tempDouble))) >= 1.0 ? (tempDouble > 0.0 ? ((Math.min((+(Math.floor((tempDouble)/4294967296.0))), 4294967295.0))|0)>>>0 : (~~((+(Math.ceil((tempDouble - +(((~~(tempDouble)))>>>0))/4294967296.0)))))>>>0) : 0)],HEAP32[(((buf)+(40))>>2)] = tempI64[0],HEAP32[(((buf)+(44))>>2)] = tempI64[1]);
        HEAP32[(((buf)+(48))>>2)] = 4096;
        HEAP32[(((buf)+(52))>>2)] = stat.blocks;
        var atime = stat.atime.getTime();
        var mtime = stat.mtime.getTime();
        var ctime = stat.ctime.getTime();
        (tempI64 = [Math.floor(atime / 1000)>>>0,(tempDouble=Math.floor(atime / 1000),(+(Math.abs(tempDouble))) >= 1.0 ? (tempDouble > 0.0 ? ((Math.min((+(Math.floor((tempDouble)/4294967296.0))), 4294967295.0))|0)>>>0 : (~~((+(Math.ceil((tempDouble - +(((~~(tempDouble)))>>>0))/4294967296.0)))))>>>0) : 0)],HEAP32[(((buf)+(56))>>2)] = tempI64[0],HEAP32[(((buf)+(60))>>2)] = tempI64[1]);
        HEAPU32[(((buf)+(64))>>2)] = (atime % 1000) * 1000;
        (tempI64 = [Math.floor(mtime / 1000)>>>0,(tempDouble=Math.floor(mtime / 1000),(+(Math.abs(tempDouble))) >= 1.0 ? (tempDouble > 0.0 ? ((Math.min((+(Math.floor((tempDouble)/4294967296.0))), 4294967295.0))|0)>>>0 : (~~((+(Math.ceil((tempDouble - +(((~~(tempDouble)))>>>0))/4294967296.0)))))>>>0) : 0)],HEAP32[(((buf)+(72))>>2)] = tempI64[0],HEAP32[(((buf)+(76))>>2)] = tempI64[1]);
        HEAPU32[(((buf)+(80))>>2)] = (mtime % 1000) * 1000;
        (tempI64 = [Math.floor(ctime / 1000)>>>0,(tempDouble=Math.floor(ctime / 1000),(+(Math.abs(tempDouble))) >= 1.0 ? (tempDouble > 0.0 ? ((Math.min((+(Math.floor((tempDouble)/4294967296.0))), 4294967295.0))|0)>>>0 : (~~((+(Math.ceil((tempDouble - +(((~~(tempDouble)))>>>0))/4294967296.0)))))>>>0) : 0)],HEAP32[(((buf)+(88))>>2)] = tempI64[0],HEAP32[(((buf)+(92))>>2)] = tempI64[1]);
        HEAPU32[(((buf)+(96))>>2)] = (ctime % 1000) * 1000;
        (tempI64 = [stat.ino>>>0,(tempDouble=stat.ino,(+(Math.abs(tempDouble))) >= 1.0 ? (tempDouble > 0.0 ? ((Math.min((+(Math.floor((tempDouble)/4294967296.0))), 4294967295.0))|0)>>>0 : (~~((+(Math.ceil((tempDouble - +(((~~(tempDouble)))>>>0))/4294967296.0)))))>>>0) : 0)],HEAP32[(((buf)+(104))>>2)] = tempI64[0],HEAP32[(((buf)+(108))>>2)] = tempI64[1]);
        return 0;
      },doMsync:function(addr, stream, len, flags, offset) {
        if (!FS.isFile(stream.node.mode)) {
          throw new FS.ErrnoError(43);
        }
        if (flags & 2) {
          // MAP_PRIVATE calls need not to be synced back to underlying fs
          return 0;
        }
        var buffer = HEAPU8.slice(addr, addr + len);
        FS.msync(stream, buffer, offset, len, flags);
      },varargs:undefined,get:function() {
        assert(SYSCALLS.varargs != undefined);
        SYSCALLS.varargs += 4;
        var ret = HEAP32[(((SYSCALLS.varargs)-(4))>>2)];
        return ret;
      },getStr:function(ptr) {
        var ret = UTF8ToString(ptr);
        return ret;
      },getStreamFromFD:function(fd) {
        var stream = FS.getStream(fd);
        if (!stream) throw new FS.ErrnoError(8);
        return stream;
      }};
  function ___syscall_fcntl64(fd, cmd, varargs) {
  SYSCALLS.varargs = varargs;
  try {
  
      var stream = SYSCALLS.getStreamFromFD(fd);
      switch (cmd) {
        case 0: {
          var arg = SYSCALLS.get();
          if (arg < 0) {
            return -28;
          }
          var newStream;
          newStream = FS.createStream(stream, arg);
          return newStream.fd;
        }
        case 1:
        case 2:
          return 0;  // FD_CLOEXEC makes no sense for a single process.
        case 3:
          return stream.flags;
        case 4: {
          var arg = SYSCALLS.get();
          stream.flags |= arg;
          return 0;
        }
        case 5:
        /* case 5: Currently in musl F_GETLK64 has same value as F_GETLK, so omitted to avoid duplicate case blocks. If that changes, uncomment this */ {
          
          var arg = SYSCALLS.get();
          var offset = 0;
          // We're always unlocked.
          HEAP16[(((arg)+(offset))>>1)] = 2;
          return 0;
        }
        case 6:
        case 7:
        /* case 6: Currently in musl F_SETLK64 has same value as F_SETLK, so omitted to avoid duplicate case blocks. If that changes, uncomment this */
        /* case 7: Currently in musl F_SETLKW64 has same value as F_SETLKW, so omitted to avoid duplicate case blocks. If that changes, uncomment this */
          
          
          return 0; // Pretend that the locking is successful.
        case 16:
        case 8:
          return -28; // These are for sockets. We don't have them fully implemented yet.
        case 9:
          // musl trusts getown return values, due to a bug where they must be, as they overlap with errors. just return -1 here, so fcntl() returns that, and we set errno ourselves.
          setErrNo(28);
          return -1;
        default: {
          return -28;
        }
      }
    } catch (e) {
    if (typeof FS == 'undefined' || !(e.name === 'ErrnoError')) throw e;
    return -e.errno;
  }
  }

  function ___syscall_ioctl(fd, op, varargs) {
  SYSCALLS.varargs = varargs;
  try {
  
      var stream = SYSCALLS.getStreamFromFD(fd);
      switch (op) {
        case 21509:
        case 21505: {
          if (!stream.tty) return -59;
          return 0;
        }
        case 21510:
        case 21511:
        case 21512:
        case 21506:
        case 21507:
        case 21508: {
          if (!stream.tty) return -59;
          return 0; // no-op, not actually adjusting terminal settings
        }
        case 21519: {
          if (!stream.tty) return -59;
          var argp = SYSCALLS.get();
          HEAP32[((argp)>>2)] = 0;
          return 0;
        }
        case 21520: {
          if (!stream.tty) return -59;
          return -28; // not supported
        }
        case 21531: {
          var argp = SYSCALLS.get();
          return FS.ioctl(stream, op, argp);
        }
        case 21523: {
          // TODO: in theory we should write to the winsize struct that gets
          // passed in, but for now musl doesn't read anything on it
          if (!stream.tty) return -59;
          return 0;
        }
        case 21524: {
          // TODO: technically, this ioctl call should change the window size.
          // but, since emscripten doesn't have any concept of a terminal window
          // yet, we'll just silently throw it away as we do TIOCGWINSZ
          if (!stream.tty) return -59;
          return 0;
        }
        default: return -28; // not supported
      }
    } catch (e) {
    if (typeof FS == 'undefined' || !(e.name === 'ErrnoError')) throw e;
    return -e.errno;
  }
  }

  function ___syscall_openat(dirfd, path, flags, varargs) {
  SYSCALLS.varargs = varargs;
  try {
  
      path = SYSCALLS.getStr(path);
      path = SYSCALLS.calculateAt(dirfd, path);
      var mode = varargs ? SYSCALLS.get() : 0;
      return FS.open(path, flags, mode).fd;
    } catch (e) {
    if (typeof FS == 'undefined' || !(e.name === 'ErrnoError')) throw e;
    return -e.errno;
  }
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

  function _fd_close(fd) {
  try {
  
      var stream = SYSCALLS.getStreamFromFD(fd);
      FS.close(stream);
      return 0;
    } catch (e) {
    if (typeof FS == 'undefined' || !(e.name === 'ErrnoError')) throw e;
    return e.errno;
  }
  }

  /** @param {number=} offset */
  function doReadv(stream, iov, iovcnt, offset) {
      var ret = 0;
      for (var i = 0; i < iovcnt; i++) {
        var ptr = HEAPU32[((iov)>>2)];
        var len = HEAPU32[(((iov)+(4))>>2)];
        iov += 8;
        var curr = FS.read(stream, HEAP8,ptr, len, offset);
        if (curr < 0) return -1;
        ret += curr;
        if (curr < len) break; // nothing more to read
        if (typeof offset !== 'undefined') {
          offset += curr;
        }
      }
      return ret;
    }
  
  function _fd_read(fd, iov, iovcnt, pnum) {
  try {
  
      var stream = SYSCALLS.getStreamFromFD(fd);
      var num = doReadv(stream, iov, iovcnt);
      HEAPU32[((pnum)>>2)] = num;
      return 0;
    } catch (e) {
    if (typeof FS == 'undefined' || !(e.name === 'ErrnoError')) throw e;
    return e.errno;
  }
  }

  function convertI32PairToI53Checked(lo, hi) {
      assert(lo == (lo >>> 0) || lo == (lo|0)); // lo should either be a i32 or a u32
      assert(hi === (hi|0));                    // hi should be a i32
      return ((hi + 0x200000) >>> 0 < 0x400001 - !!lo) ? (lo >>> 0) + hi * 4294967296 : NaN;
    }
  
  
  
  
  function _fd_seek(fd, offset_low, offset_high, whence, newOffset) {
  try {
  
      var offset = convertI32PairToI53Checked(offset_low, offset_high); if (isNaN(offset)) return 61;
      var stream = SYSCALLS.getStreamFromFD(fd);
      FS.llseek(stream, offset, whence);
      (tempI64 = [stream.position>>>0,(tempDouble=stream.position,(+(Math.abs(tempDouble))) >= 1.0 ? (tempDouble > 0.0 ? ((Math.min((+(Math.floor((tempDouble)/4294967296.0))), 4294967295.0))|0)>>>0 : (~~((+(Math.ceil((tempDouble - +(((~~(tempDouble)))>>>0))/4294967296.0)))))>>>0) : 0)],HEAP32[((newOffset)>>2)] = tempI64[0],HEAP32[(((newOffset)+(4))>>2)] = tempI64[1]);
      if (stream.getdents && offset === 0 && whence === 0) stream.getdents = null; // reset readdir state
      return 0;
    } catch (e) {
    if (typeof FS == 'undefined' || !(e.name === 'ErrnoError')) throw e;
    return e.errno;
  }
  }

  /** @param {number=} offset */
  function doWritev(stream, iov, iovcnt, offset) {
      var ret = 0;
      for (var i = 0; i < iovcnt; i++) {
        var ptr = HEAPU32[((iov)>>2)];
        var len = HEAPU32[(((iov)+(4))>>2)];
        iov += 8;
        var curr = FS.write(stream, HEAP8,ptr, len, offset);
        if (curr < 0) return -1;
        ret += curr;
        if (typeof offset !== 'undefined') {
          offset += curr;
        }
      }
      return ret;
    }
  
  function _fd_write(fd, iov, iovcnt, pnum) {
  try {
  
      var stream = SYSCALLS.getStreamFromFD(fd);
      var num = doWritev(stream, iov, iovcnt);
      HEAPU32[((pnum)>>2)] = num;
      return 0;
    } catch (e) {
    if (typeof FS == 'undefined' || !(e.name === 'ErrnoError')) throw e;
    return e.errno;
  }
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

  var FSNode = /** @constructor */ function(parent, name, mode, rdev) {
    if (!parent) {
      parent = this;  // root node sets parent to itself
    }
    this.parent = parent;
    this.mount = parent.mount;
    this.mounted = null;
    this.id = FS.nextInode++;
    this.name = name;
    this.mode = mode;
    this.node_ops = {};
    this.stream_ops = {};
    this.rdev = rdev;
  };
  var readMode = 292/*292*/ | 73/*73*/;
  var writeMode = 146/*146*/;
  Object.defineProperties(FSNode.prototype, {
   read: {
    get: /** @this{FSNode} */function() {
     return (this.mode & readMode) === readMode;
    },
    set: /** @this{FSNode} */function(val) {
     val ? this.mode |= readMode : this.mode &= ~readMode;
    }
   },
   write: {
    get: /** @this{FSNode} */function() {
     return (this.mode & writeMode) === writeMode;
    },
    set: /** @this{FSNode} */function(val) {
     val ? this.mode |= writeMode : this.mode &= ~writeMode;
    }
   },
   isFolder: {
    get: /** @this{FSNode} */function() {
     return FS.isDir(this.mode);
    }
   },
   isDevice: {
    get: /** @this{FSNode} */function() {
     return FS.isChrdev(this.mode);
    }
   }
  });
  FS.FSNode = FSNode;
  FS.staticInit();;
ERRNO_CODES = {
      'EPERM': 63,
      'ENOENT': 44,
      'ESRCH': 71,
      'EINTR': 27,
      'EIO': 29,
      'ENXIO': 60,
      'E2BIG': 1,
      'ENOEXEC': 45,
      'EBADF': 8,
      'ECHILD': 12,
      'EAGAIN': 6,
      'EWOULDBLOCK': 6,
      'ENOMEM': 48,
      'EACCES': 2,
      'EFAULT': 21,
      'ENOTBLK': 105,
      'EBUSY': 10,
      'EEXIST': 20,
      'EXDEV': 75,
      'ENODEV': 43,
      'ENOTDIR': 54,
      'EISDIR': 31,
      'EINVAL': 28,
      'ENFILE': 41,
      'EMFILE': 33,
      'ENOTTY': 59,
      'ETXTBSY': 74,
      'EFBIG': 22,
      'ENOSPC': 51,
      'ESPIPE': 70,
      'EROFS': 69,
      'EMLINK': 34,
      'EPIPE': 64,
      'EDOM': 18,
      'ERANGE': 68,
      'ENOMSG': 49,
      'EIDRM': 24,
      'ECHRNG': 106,
      'EL2NSYNC': 156,
      'EL3HLT': 107,
      'EL3RST': 108,
      'ELNRNG': 109,
      'EUNATCH': 110,
      'ENOCSI': 111,
      'EL2HLT': 112,
      'EDEADLK': 16,
      'ENOLCK': 46,
      'EBADE': 113,
      'EBADR': 114,
      'EXFULL': 115,
      'ENOANO': 104,
      'EBADRQC': 103,
      'EBADSLT': 102,
      'EDEADLOCK': 16,
      'EBFONT': 101,
      'ENOSTR': 100,
      'ENODATA': 116,
      'ETIME': 117,
      'ENOSR': 118,
      'ENONET': 119,
      'ENOPKG': 120,
      'EREMOTE': 121,
      'ENOLINK': 47,
      'EADV': 122,
      'ESRMNT': 123,
      'ECOMM': 124,
      'EPROTO': 65,
      'EMULTIHOP': 36,
      'EDOTDOT': 125,
      'EBADMSG': 9,
      'ENOTUNIQ': 126,
      'EBADFD': 127,
      'EREMCHG': 128,
      'ELIBACC': 129,
      'ELIBBAD': 130,
      'ELIBSCN': 131,
      'ELIBMAX': 132,
      'ELIBEXEC': 133,
      'ENOSYS': 52,
      'ENOTEMPTY': 55,
      'ENAMETOOLONG': 37,
      'ELOOP': 32,
      'EOPNOTSUPP': 138,
      'EPFNOSUPPORT': 139,
      'ECONNRESET': 15,
      'ENOBUFS': 42,
      'EAFNOSUPPORT': 5,
      'EPROTOTYPE': 67,
      'ENOTSOCK': 57,
      'ENOPROTOOPT': 50,
      'ESHUTDOWN': 140,
      'ECONNREFUSED': 14,
      'EADDRINUSE': 3,
      'ECONNABORTED': 13,
      'ENETUNREACH': 40,
      'ENETDOWN': 38,
      'ETIMEDOUT': 73,
      'EHOSTDOWN': 142,
      'EHOSTUNREACH': 23,
      'EINPROGRESS': 26,
      'EALREADY': 7,
      'EDESTADDRREQ': 17,
      'EMSGSIZE': 35,
      'EPROTONOSUPPORT': 66,
      'ESOCKTNOSUPPORT': 137,
      'EADDRNOTAVAIL': 4,
      'ENETRESET': 39,
      'EISCONN': 30,
      'ENOTCONN': 53,
      'ETOOMANYREFS': 141,
      'EUSERS': 136,
      'EDQUOT': 19,
      'ESTALE': 72,
      'ENOTSUP': 138,
      'ENOMEDIUM': 148,
      'EILSEQ': 25,
      'EOVERFLOW': 61,
      'ECANCELED': 11,
      'ENOTRECOVERABLE': 56,
      'EOWNERDEAD': 62,
      'ESTRPIPE': 135,
    };;
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
  "__syscall_fcntl64": ___syscall_fcntl64,
  "__syscall_ioctl": ___syscall_ioctl,
  "__syscall_openat": ___syscall_openat,
  "abort": _abort,
  "emscripten_memcpy_big": _emscripten_memcpy_big,
  "emscripten_resize_heap": _emscripten_resize_heap,
  "fd_close": _fd_close,
  "fd_read": _fd_read,
  "fd_seek": _fd_seek,
  "fd_write": _fd_write,
  "invoke_dii": invoke_dii,
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
  "invoke_viddidiidd": invoke_viddidiidd,
  "invoke_vididi": invoke_vididi,
  "invoke_vidii": invoke_vidii,
  "invoke_vii": invoke_vii,
  "invoke_viii": invoke_viii,
  "invoke_viiiddddd": invoke_viiiddddd,
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
var _emscripten_bind_FireSize_getBackingSpreadRate_1 = Module["_emscripten_bind_FireSize_getBackingSpreadRate_1"] = createExportWrapper("emscripten_bind_FireSize_getBackingSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getEccentricity_0 = Module["_emscripten_bind_FireSize_getEccentricity_0"] = createExportWrapper("emscripten_bind_FireSize_getEccentricity_0");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getEllipticalA_3 = Module["_emscripten_bind_FireSize_getEllipticalA_3"] = createExportWrapper("emscripten_bind_FireSize_getEllipticalA_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getEllipticalB_3 = Module["_emscripten_bind_FireSize_getEllipticalB_3"] = createExportWrapper("emscripten_bind_FireSize_getEllipticalB_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getEllipticalC_3 = Module["_emscripten_bind_FireSize_getEllipticalC_3"] = createExportWrapper("emscripten_bind_FireSize_getEllipticalC_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getFireArea_3 = Module["_emscripten_bind_FireSize_getFireArea_3"] = createExportWrapper("emscripten_bind_FireSize_getFireArea_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getFireLength_3 = Module["_emscripten_bind_FireSize_getFireLength_3"] = createExportWrapper("emscripten_bind_FireSize_getFireLength_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getFireLengthToWidthRatio_0 = Module["_emscripten_bind_FireSize_getFireLengthToWidthRatio_0"] = createExportWrapper("emscripten_bind_FireSize_getFireLengthToWidthRatio_0");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getFirePerimeter_3 = Module["_emscripten_bind_FireSize_getFirePerimeter_3"] = createExportWrapper("emscripten_bind_FireSize_getFirePerimeter_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_getMaxFireWidth_3 = Module["_emscripten_bind_FireSize_getMaxFireWidth_3"] = createExportWrapper("emscripten_bind_FireSize_getMaxFireWidth_3");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize_calculateFireBasicDimensions_4 = Module["_emscripten_bind_FireSize_calculateFireBasicDimensions_4"] = createExportWrapper("emscripten_bind_FireSize_calculateFireBasicDimensions_4");
/** @type {function(...*):?} */
var _emscripten_bind_FireSize___destroy___0 = Module["_emscripten_bind_FireSize___destroy___0"] = createExportWrapper("emscripten_bind_FireSize___destroy___0");
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
var _emscripten_bind_SIGMoistureScenarios_SIGMoistureScenarios_0 = Module["_emscripten_bind_SIGMoistureScenarios_SIGMoistureScenarios_0"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_SIGMoistureScenarios_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByIndex_1 = Module["_emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByIndex_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByName_1 = Module["_emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByName_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByIndex_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByIndex_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByName_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByName_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByIndex_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByIndex_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByName_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByName_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByIndex_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByIndex_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByName_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByName_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByIndex_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByIndex_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByName_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByName_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByIndex_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByIndex_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByName_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByName_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioIndexByName_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioIndexByName_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioIndexByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getNumberOfMoistureScenarios_0 = Module["_emscripten_bind_SIGMoistureScenarios_getNumberOfMoistureScenarios_0"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getNumberOfMoistureScenarios_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByIndex_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByIndex_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByName_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByName_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioNameByIndex_1 = Module["_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioNameByIndex_1"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios_getMoistureScenarioNameByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMoistureScenarios___destroy___0 = Module["_emscripten_bind_SIGMoistureScenarios___destroy___0"] = createExportWrapper("emscripten_bind_SIGMoistureScenarios___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_Spot_0 = Module["_emscripten_bind_Spot_Spot_0"] = createExportWrapper("emscripten_bind_Spot_Spot_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getDownwindCanopyMode_0 = Module["_emscripten_bind_Spot_getDownwindCanopyMode_0"] = createExportWrapper("emscripten_bind_Spot_getDownwindCanopyMode_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getLocation_0 = Module["_emscripten_bind_Spot_getLocation_0"] = createExportWrapper("emscripten_bind_Spot_getLocation_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getTreeSpecies_0 = Module["_emscripten_bind_Spot_getTreeSpecies_0"] = createExportWrapper("emscripten_bind_Spot_getTreeSpecies_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getBurningPileFlameHeight_1 = Module["_emscripten_bind_Spot_getBurningPileFlameHeight_1"] = createExportWrapper("emscripten_bind_Spot_getBurningPileFlameHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getCoverHeightUsedForBurningPile_1 = Module["_emscripten_bind_Spot_getCoverHeightUsedForBurningPile_1"] = createExportWrapper("emscripten_bind_Spot_getCoverHeightUsedForBurningPile_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getCoverHeightUsedForSurfaceFire_1 = Module["_emscripten_bind_Spot_getCoverHeightUsedForSurfaceFire_1"] = createExportWrapper("emscripten_bind_Spot_getCoverHeightUsedForSurfaceFire_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getCoverHeightUsedForTorchingTrees_1 = Module["_emscripten_bind_Spot_getCoverHeightUsedForTorchingTrees_1"] = createExportWrapper("emscripten_bind_Spot_getCoverHeightUsedForTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getDBH_1 = Module["_emscripten_bind_Spot_getDBH_1"] = createExportWrapper("emscripten_bind_Spot_getDBH_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getDownwindCoverHeight_1 = Module["_emscripten_bind_Spot_getDownwindCoverHeight_1"] = createExportWrapper("emscripten_bind_Spot_getDownwindCoverHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getFlameDurationForTorchingTrees_1 = Module["_emscripten_bind_Spot_getFlameDurationForTorchingTrees_1"] = createExportWrapper("emscripten_bind_Spot_getFlameDurationForTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getFlameHeightForTorchingTrees_1 = Module["_emscripten_bind_Spot_getFlameHeightForTorchingTrees_1"] = createExportWrapper("emscripten_bind_Spot_getFlameHeightForTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getFlameRatioForTorchingTrees_0 = Module["_emscripten_bind_Spot_getFlameRatioForTorchingTrees_0"] = createExportWrapper("emscripten_bind_Spot_getFlameRatioForTorchingTrees_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getMaxFirebrandHeightFromBurningPile_1 = Module["_emscripten_bind_Spot_getMaxFirebrandHeightFromBurningPile_1"] = createExportWrapper("emscripten_bind_Spot_getMaxFirebrandHeightFromBurningPile_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getMaxFirebrandHeightFromSurfaceFire_1 = Module["_emscripten_bind_Spot_getMaxFirebrandHeightFromSurfaceFire_1"] = createExportWrapper("emscripten_bind_Spot_getMaxFirebrandHeightFromSurfaceFire_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getMaxFirebrandHeightFromTorchingTrees_1 = Module["_emscripten_bind_Spot_getMaxFirebrandHeightFromTorchingTrees_1"] = createExportWrapper("emscripten_bind_Spot_getMaxFirebrandHeightFromTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1 = Module["_emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1"] = createExportWrapper("emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1 = Module["_emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1"] = createExportWrapper("emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1 = Module["_emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1"] = createExportWrapper("emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1 = Module["_emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1"] = createExportWrapper("emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1 = Module["_emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1"] = createExportWrapper("emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1 = Module["_emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1"] = createExportWrapper("emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getRidgeToValleyDistance_1 = Module["_emscripten_bind_Spot_getRidgeToValleyDistance_1"] = createExportWrapper("emscripten_bind_Spot_getRidgeToValleyDistance_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getRidgeToValleyElevation_1 = Module["_emscripten_bind_Spot_getRidgeToValleyElevation_1"] = createExportWrapper("emscripten_bind_Spot_getRidgeToValleyElevation_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getSurfaceFlameLength_1 = Module["_emscripten_bind_Spot_getSurfaceFlameLength_1"] = createExportWrapper("emscripten_bind_Spot_getSurfaceFlameLength_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getTreeHeight_1 = Module["_emscripten_bind_Spot_getTreeHeight_1"] = createExportWrapper("emscripten_bind_Spot_getTreeHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getWindSpeedAtTwentyFeet_1 = Module["_emscripten_bind_Spot_getWindSpeedAtTwentyFeet_1"] = createExportWrapper("emscripten_bind_Spot_getWindSpeedAtTwentyFeet_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_getTorchingTrees_0 = Module["_emscripten_bind_Spot_getTorchingTrees_0"] = createExportWrapper("emscripten_bind_Spot_getTorchingTrees_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_calculateSpottingDistanceFromBurningPile_0 = Module["_emscripten_bind_Spot_calculateSpottingDistanceFromBurningPile_0"] = createExportWrapper("emscripten_bind_Spot_calculateSpottingDistanceFromBurningPile_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_calculateSpottingDistanceFromSurfaceFire_0 = Module["_emscripten_bind_Spot_calculateSpottingDistanceFromSurfaceFire_0"] = createExportWrapper("emscripten_bind_Spot_calculateSpottingDistanceFromSurfaceFire_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_calculateSpottingDistanceFromTorchingTrees_0 = Module["_emscripten_bind_Spot_calculateSpottingDistanceFromTorchingTrees_0"] = createExportWrapper("emscripten_bind_Spot_calculateSpottingDistanceFromTorchingTrees_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_initializeMembers_0 = Module["_emscripten_bind_Spot_initializeMembers_0"] = createExportWrapper("emscripten_bind_Spot_initializeMembers_0");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setBurningPileFlameHeight_2 = Module["_emscripten_bind_Spot_setBurningPileFlameHeight_2"] = createExportWrapper("emscripten_bind_Spot_setBurningPileFlameHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setDBH_2 = Module["_emscripten_bind_Spot_setDBH_2"] = createExportWrapper("emscripten_bind_Spot_setDBH_2");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setDownwindCanopyMode_1 = Module["_emscripten_bind_Spot_setDownwindCanopyMode_1"] = createExportWrapper("emscripten_bind_Spot_setDownwindCanopyMode_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setDownwindCoverHeight_2 = Module["_emscripten_bind_Spot_setDownwindCoverHeight_2"] = createExportWrapper("emscripten_bind_Spot_setDownwindCoverHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setFlameLength_2 = Module["_emscripten_bind_Spot_setFlameLength_2"] = createExportWrapper("emscripten_bind_Spot_setFlameLength_2");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setLocation_1 = Module["_emscripten_bind_Spot_setLocation_1"] = createExportWrapper("emscripten_bind_Spot_setLocation_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setRidgeToValleyDistance_2 = Module["_emscripten_bind_Spot_setRidgeToValleyDistance_2"] = createExportWrapper("emscripten_bind_Spot_setRidgeToValleyDistance_2");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setRidgeToValleyElevation_2 = Module["_emscripten_bind_Spot_setRidgeToValleyElevation_2"] = createExportWrapper("emscripten_bind_Spot_setRidgeToValleyElevation_2");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setTorchingTrees_1 = Module["_emscripten_bind_Spot_setTorchingTrees_1"] = createExportWrapper("emscripten_bind_Spot_setTorchingTrees_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setTreeHeight_2 = Module["_emscripten_bind_Spot_setTreeHeight_2"] = createExportWrapper("emscripten_bind_Spot_setTreeHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setTreeSpecies_1 = Module["_emscripten_bind_Spot_setTreeSpecies_1"] = createExportWrapper("emscripten_bind_Spot_setTreeSpecies_1");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_setWindSpeedAtTwentyFeet_2 = Module["_emscripten_bind_Spot_setWindSpeedAtTwentyFeet_2"] = createExportWrapper("emscripten_bind_Spot_setWindSpeedAtTwentyFeet_2");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_updateSpotInputsForBurningPile_12 = Module["_emscripten_bind_Spot_updateSpotInputsForBurningPile_12"] = createExportWrapper("emscripten_bind_Spot_updateSpotInputsForBurningPile_12");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_updateSpotInputsForSurfaceFire_12 = Module["_emscripten_bind_Spot_updateSpotInputsForSurfaceFire_12"] = createExportWrapper("emscripten_bind_Spot_updateSpotInputsForSurfaceFire_12");
/** @type {function(...*):?} */
var _emscripten_bind_Spot_updateSpotInputsForTorchingTrees_16 = Module["_emscripten_bind_Spot_updateSpotInputsForTorchingTrees_16"] = createExportWrapper("emscripten_bind_Spot_updateSpotInputsForTorchingTrees_16");
/** @type {function(...*):?} */
var _emscripten_bind_Spot___destroy___0 = Module["_emscripten_bind_Spot___destroy___0"] = createExportWrapper("emscripten_bind_Spot___destroy___0");
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
var _emscripten_bind_SIGFuelModels_isAllFuelLoadZero_1 = Module["_emscripten_bind_SIGFuelModels_isAllFuelLoadZero_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_isAllFuelLoadZero_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_isFuelModelDefined_1 = Module["_emscripten_bind_SIGFuelModels_isFuelModelDefined_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_isFuelModelDefined_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_isFuelModelReserved_1 = Module["_emscripten_bind_SIGFuelModels_isFuelModelReserved_1"] = createExportWrapper("emscripten_bind_SIGFuelModels_isFuelModelReserved_1");
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
var _emscripten_bind_SIGFuelModels_getMoistureOfExtinctionDead_2 = Module["_emscripten_bind_SIGFuelModels_getMoistureOfExtinctionDead_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getMoistureOfExtinctionDead_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getSavrLiveHerbaceous_2 = Module["_emscripten_bind_SIGFuelModels_getSavrLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getSavrLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getSavrLiveWoody_2 = Module["_emscripten_bind_SIGFuelModels_getSavrLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getSavrLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getSavrOneHour_2 = Module["_emscripten_bind_SIGFuelModels_getSavrOneHour_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getSavrOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2 = Module["_emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2"] = createExportWrapper("emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGFuelModels___destroy___0 = Module["_emscripten_bind_SIGFuelModels___destroy___0"] = createExportWrapper("emscripten_bind_SIGFuelModels___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_SIGSurface_1 = Module["_emscripten_bind_SIGSurface_SIGSurface_1"] = createExportWrapper("emscripten_bind_SIGSurface_SIGSurface_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenFireSeverity_0 = Module["_emscripten_bind_SIGSurface_getAspenFireSeverity_0"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenFireSeverity_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralFuelType_0 = Module["_emscripten_bind_SIGSurface_getChaparralFuelType_0"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralFuelType_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureInputMode_0 = Module["_emscripten_bind_SIGSurface_getMoistureInputMode_0"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureInputMode_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindAdjustmentFactorCalculationMethod_0 = Module["_emscripten_bind_SIGSurface_getWindAdjustmentFactorCalculationMethod_0"] = createExportWrapper("emscripten_bind_SIGSurface_getWindAdjustmentFactorCalculationMethod_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindAndSpreadOrientationMode_0 = Module["_emscripten_bind_SIGSurface_getWindAndSpreadOrientationMode_0"] = createExportWrapper("emscripten_bind_SIGSurface_getWindAndSpreadOrientationMode_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindHeightInputMode_0 = Module["_emscripten_bind_SIGSurface_getWindHeightInputMode_0"] = createExportWrapper("emscripten_bind_SIGSurface_getWindHeightInputMode_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByIndex_1 = Module["_emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByIndex_1"] = createExportWrapper("emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByName_1 = Module["_emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByName_1"] = createExportWrapper("emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getIsUsingChaparral_0 = Module["_emscripten_bind_SIGSurface_getIsUsingChaparral_0"] = createExportWrapper("emscripten_bind_SIGSurface_getIsUsingChaparral_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getIsUsingPalmettoGallberry_0 = Module["_emscripten_bind_SIGSurface_getIsUsingPalmettoGallberry_0"] = createExportWrapper("emscripten_bind_SIGSurface_getIsUsingPalmettoGallberry_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getIsUsingWesternAspen_0 = Module["_emscripten_bind_SIGSurface_getIsUsingWesternAspen_0"] = createExportWrapper("emscripten_bind_SIGSurface_getIsUsingWesternAspen_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_isAllFuelLoadZero_1 = Module["_emscripten_bind_SIGSurface_isAllFuelLoadZero_1"] = createExportWrapper("emscripten_bind_SIGSurface_isAllFuelLoadZero_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_isFuelDynamic_1 = Module["_emscripten_bind_SIGSurface_isFuelDynamic_1"] = createExportWrapper("emscripten_bind_SIGSurface_isFuelDynamic_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_isFuelModelDefined_1 = Module["_emscripten_bind_SIGSurface_isFuelModelDefined_1"] = createExportWrapper("emscripten_bind_SIGSurface_isFuelModelDefined_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_isFuelModelReserved_1 = Module["_emscripten_bind_SIGSurface_isFuelModelReserved_1"] = createExportWrapper("emscripten_bind_SIGSurface_isFuelModelReserved_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_isMoistureClassInputNeededForCurrentFuelModel_1 = Module["_emscripten_bind_SIGSurface_isMoistureClassInputNeededForCurrentFuelModel_1"] = createExportWrapper("emscripten_bind_SIGSurface_isMoistureClassInputNeededForCurrentFuelModel_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_isUsingTwoFuelModels_0 = Module["_emscripten_bind_SIGSurface_isUsingTwoFuelModels_0"] = createExportWrapper("emscripten_bind_SIGSurface_isUsingTwoFuelModels_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureScenarioByIndex_1 = Module["_emscripten_bind_SIGSurface_setMoistureScenarioByIndex_1"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureScenarioByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureScenarioByName_1 = Module["_emscripten_bind_SIGSurface_setMoistureScenarioByName_1"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureScenarioByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_calculateFlameLength_3 = Module["_emscripten_bind_SIGSurface_calculateFlameLength_3"] = createExportWrapper("emscripten_bind_SIGSurface_calculateFlameLength_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_calculateScorchHeight_7 = Module["_emscripten_bind_SIGSurface_calculateScorchHeight_7"] = createExportWrapper("emscripten_bind_SIGSurface_calculateScorchHeight_7");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAgeOfRough_0 = Module["_emscripten_bind_SIGSurface_getAgeOfRough_0"] = createExportWrapper("emscripten_bind_SIGSurface_getAgeOfRough_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspect_0 = Module["_emscripten_bind_SIGSurface_getAspect_0"] = createExportWrapper("emscripten_bind_SIGSurface_getAspect_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenCuringLevel_1 = Module["_emscripten_bind_SIGSurface_getAspenCuringLevel_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenCuringLevel_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenDBH_1 = Module["_emscripten_bind_SIGSurface_getAspenDBH_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenDBH_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenLoadDeadOneHour_1 = Module["_emscripten_bind_SIGSurface_getAspenLoadDeadOneHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenLoadDeadOneHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenLoadDeadTenHour_1 = Module["_emscripten_bind_SIGSurface_getAspenLoadDeadTenHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenLoadDeadTenHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenLoadLiveHerbaceous_1 = Module["_emscripten_bind_SIGSurface_getAspenLoadLiveHerbaceous_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenLoadLiveHerbaceous_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenLoadLiveWoody_1 = Module["_emscripten_bind_SIGSurface_getAspenLoadLiveWoody_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenLoadLiveWoody_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenSavrDeadOneHour_1 = Module["_emscripten_bind_SIGSurface_getAspenSavrDeadOneHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenSavrDeadOneHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenSavrDeadTenHour_1 = Module["_emscripten_bind_SIGSurface_getAspenSavrDeadTenHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenSavrDeadTenHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenSavrLiveHerbaceous_1 = Module["_emscripten_bind_SIGSurface_getAspenSavrLiveHerbaceous_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenSavrLiveHerbaceous_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenSavrLiveWoody_1 = Module["_emscripten_bind_SIGSurface_getAspenSavrLiveWoody_1"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenSavrLiveWoody_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getBackingSpreadDistance_3 = Module["_emscripten_bind_SIGSurface_getBackingSpreadDistance_3"] = createExportWrapper("emscripten_bind_SIGSurface_getBackingSpreadDistance_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getBackingSpreadRate_1 = Module["_emscripten_bind_SIGSurface_getBackingSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGSurface_getBackingSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getBulkDensity_1 = Module["_emscripten_bind_SIGSurface_getBulkDensity_1"] = createExportWrapper("emscripten_bind_SIGSurface_getBulkDensity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getCanopyCover_1 = Module["_emscripten_bind_SIGSurface_getCanopyCover_1"] = createExportWrapper("emscripten_bind_SIGSurface_getCanopyCover_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getCanopyHeight_1 = Module["_emscripten_bind_SIGSurface_getCanopyHeight_1"] = createExportWrapper("emscripten_bind_SIGSurface_getCanopyHeight_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralAge_1 = Module["_emscripten_bind_SIGSurface_getChaparralAge_1"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralAge_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralDaysSinceMayFirst_0 = Module["_emscripten_bind_SIGSurface_getChaparralDaysSinceMayFirst_0"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralDaysSinceMayFirst_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralDeadFuelFraction_0 = Module["_emscripten_bind_SIGSurface_getChaparralDeadFuelFraction_0"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralDeadFuelFraction_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralDeadMoistureOfExtinction_1 = Module["_emscripten_bind_SIGSurface_getChaparralDeadMoistureOfExtinction_1"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralDeadMoistureOfExtinction_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralDensity_3 = Module["_emscripten_bind_SIGSurface_getChaparralDensity_3"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralDensity_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralEffectiveSilicaContent_2 = Module["_emscripten_bind_SIGSurface_getChaparralEffectiveSilicaContent_2"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralEffectiveSilicaContent_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralFuelBedDepth_1 = Module["_emscripten_bind_SIGSurface_getChaparralFuelBedDepth_1"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralFuelBedDepth_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralFuelDeadLoadFraction_0 = Module["_emscripten_bind_SIGSurface_getChaparralFuelDeadLoadFraction_0"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralFuelDeadLoadFraction_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralHeatOfCombustion_3 = Module["_emscripten_bind_SIGSurface_getChaparralHeatOfCombustion_3"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralHeatOfCombustion_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralLiveMoistureOfExtinction_1 = Module["_emscripten_bind_SIGSurface_getChaparralLiveMoistureOfExtinction_1"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralLiveMoistureOfExtinction_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralLoad_3 = Module["_emscripten_bind_SIGSurface_getChaparralLoad_3"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralLoad_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralMoisture_3 = Module["_emscripten_bind_SIGSurface_getChaparralMoisture_3"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralMoisture_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralSavr_3 = Module["_emscripten_bind_SIGSurface_getChaparralSavr_3"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralSavr_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralTotalDeadFuelLoad_1 = Module["_emscripten_bind_SIGSurface_getChaparralTotalDeadFuelLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralTotalDeadFuelLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralTotalFuelLoad_1 = Module["_emscripten_bind_SIGSurface_getChaparralTotalFuelLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralTotalFuelLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralTotalLiveFuelLoad_1 = Module["_emscripten_bind_SIGSurface_getChaparralTotalLiveFuelLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralTotalLiveFuelLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getChaparralTotalSilicaContent_2 = Module["_emscripten_bind_SIGSurface_getChaparralTotalSilicaContent_2"] = createExportWrapper("emscripten_bind_SIGSurface_getChaparralTotalSilicaContent_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getCharacteristicMoistureByLifeState_2 = Module["_emscripten_bind_SIGSurface_getCharacteristicMoistureByLifeState_2"] = createExportWrapper("emscripten_bind_SIGSurface_getCharacteristicMoistureByLifeState_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getCharacteristicSAVR_1 = Module["_emscripten_bind_SIGSurface_getCharacteristicSAVR_1"] = createExportWrapper("emscripten_bind_SIGSurface_getCharacteristicSAVR_1");
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
var _emscripten_bind_SIGSurface_getFlankingSpreadDistance_3 = Module["_emscripten_bind_SIGSurface_getFlankingSpreadDistance_3"] = createExportWrapper("emscripten_bind_SIGSurface_getFlankingSpreadDistance_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFlankingSpreadRate_1 = Module["_emscripten_bind_SIGSurface_getFlankingSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGSurface_getFlankingSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelHeatOfCombustionDead_2 = Module["_emscripten_bind_SIGSurface_getFuelHeatOfCombustionDead_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelHeatOfCombustionDead_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelHeatOfCombustionLive_2 = Module["_emscripten_bind_SIGSurface_getFuelHeatOfCombustionLive_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelHeatOfCombustionLive_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelLoadHundredHour_2 = Module["_emscripten_bind_SIGSurface_getFuelLoadHundredHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelLoadHundredHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelLoadLiveHerbaceous_2 = Module["_emscripten_bind_SIGSurface_getFuelLoadLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelLoadLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelLoadLiveWoody_2 = Module["_emscripten_bind_SIGSurface_getFuelLoadLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelLoadLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelLoadOneHour_2 = Module["_emscripten_bind_SIGSurface_getFuelLoadOneHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelLoadOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelLoadTenHour_2 = Module["_emscripten_bind_SIGSurface_getFuelLoadTenHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelLoadTenHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelMoistureOfExtinctionDead_2 = Module["_emscripten_bind_SIGSurface_getFuelMoistureOfExtinctionDead_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelMoistureOfExtinctionDead_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelSavrLiveHerbaceous_2 = Module["_emscripten_bind_SIGSurface_getFuelSavrLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelSavrLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelSavrLiveWoody_2 = Module["_emscripten_bind_SIGSurface_getFuelSavrLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelSavrLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelSavrOneHour_2 = Module["_emscripten_bind_SIGSurface_getFuelSavrOneHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelSavrOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelbedDepth_2 = Module["_emscripten_bind_SIGSurface_getFuelbedDepth_2"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelbedDepth_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getHeadingToBackingRatio_0 = Module["_emscripten_bind_SIGSurface_getHeadingToBackingRatio_0"] = createExportWrapper("emscripten_bind_SIGSurface_getHeadingToBackingRatio_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getHeatPerUnitArea_1 = Module["_emscripten_bind_SIGSurface_getHeatPerUnitArea_1"] = createExportWrapper("emscripten_bind_SIGSurface_getHeatPerUnitArea_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getHeatSink_1 = Module["_emscripten_bind_SIGSurface_getHeatSink_1"] = createExportWrapper("emscripten_bind_SIGSurface_getHeatSink_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getHeatSource_1 = Module["_emscripten_bind_SIGSurface_getHeatSource_1"] = createExportWrapper("emscripten_bind_SIGSurface_getHeatSource_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getHeightOfUnderstory_1 = Module["_emscripten_bind_SIGSurface_getHeightOfUnderstory_1"] = createExportWrapper("emscripten_bind_SIGSurface_getHeightOfUnderstory_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getLiveFuelMoistureOfExtinction_1 = Module["_emscripten_bind_SIGSurface_getLiveFuelMoistureOfExtinction_1"] = createExportWrapper("emscripten_bind_SIGSurface_getLiveFuelMoistureOfExtinction_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMidflameWindspeed_1 = Module["_emscripten_bind_SIGSurface_getMidflameWindspeed_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMidflameWindspeed_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureDeadAggregateValue_1 = Module["_emscripten_bind_SIGSurface_getMoistureDeadAggregateValue_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureDeadAggregateValue_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureHundredHour_1 = Module["_emscripten_bind_SIGSurface_getMoistureHundredHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureHundredHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureLiveAggregateValue_1 = Module["_emscripten_bind_SIGSurface_getMoistureLiveAggregateValue_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureLiveAggregateValue_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1 = Module["_emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureLiveWoody_1 = Module["_emscripten_bind_SIGSurface_getMoistureLiveWoody_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureLiveWoody_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureOneHour_1 = Module["_emscripten_bind_SIGSurface_getMoistureOneHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureOneHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByIndex_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByIndex_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByName_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByName_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByIndex_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByIndex_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByName_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByName_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByIndex_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByIndex_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByName_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByName_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioOneHourByIndex_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioOneHourByIndex_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioOneHourByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioOneHourByName_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioOneHourByName_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioOneHourByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioTenHourByIndex_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioTenHourByIndex_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioTenHourByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioTenHourByName_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioTenHourByName_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioTenHourByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureTenHour_1 = Module["_emscripten_bind_SIGSurface_getMoistureTenHour_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureTenHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getOverstoryBasalArea_1 = Module["_emscripten_bind_SIGSurface_getOverstoryBasalArea_1"] = createExportWrapper("emscripten_bind_SIGSurface_getOverstoryBasalArea_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberryCoverage_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberryCoverage_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberryCoverage_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionDead_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionDead_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionDead_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionLive_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionLive_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionLive_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberryMoistureOfExtinctionDead_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberryMoistureOfExtinctionDead_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberryMoistureOfExtinctionDead_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberyDeadFoliageLoad_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberyDeadFoliageLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberyDeadFoliageLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberyDeadOneHourLoad_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberyDeadOneHourLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberyDeadOneHourLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberyDeadTenHourLoad_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberyDeadTenHourLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberyDeadTenHourLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberyFuelBedDepth_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberyFuelBedDepth_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberyFuelBedDepth_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberyLitterLoad_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberyLitterLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberyLitterLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberyLiveFoliageLoad_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberyLiveFoliageLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberyLiveFoliageLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberyLiveOneHourLoad_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberyLiveOneHourLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberyLiveOneHourLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getPalmettoGallberyLiveTenHourLoad_1 = Module["_emscripten_bind_SIGSurface_getPalmettoGallberyLiveTenHourLoad_1"] = createExportWrapper("emscripten_bind_SIGSurface_getPalmettoGallberyLiveTenHourLoad_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getReactionIntensity_1 = Module["_emscripten_bind_SIGSurface_getReactionIntensity_1"] = createExportWrapper("emscripten_bind_SIGSurface_getReactionIntensity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getResidenceTime_1 = Module["_emscripten_bind_SIGSurface_getResidenceTime_1"] = createExportWrapper("emscripten_bind_SIGSurface_getResidenceTime_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSlope_1 = Module["_emscripten_bind_SIGSurface_getSlope_1"] = createExportWrapper("emscripten_bind_SIGSurface_getSlope_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSlopeFactor_0 = Module["_emscripten_bind_SIGSurface_getSlopeFactor_0"] = createExportWrapper("emscripten_bind_SIGSurface_getSlopeFactor_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSpreadDistance_3 = Module["_emscripten_bind_SIGSurface_getSpreadDistance_3"] = createExportWrapper("emscripten_bind_SIGSurface_getSpreadDistance_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSpreadDistanceInDirectionOfInterest_3 = Module["_emscripten_bind_SIGSurface_getSpreadDistanceInDirectionOfInterest_3"] = createExportWrapper("emscripten_bind_SIGSurface_getSpreadDistanceInDirectionOfInterest_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSpreadRate_1 = Module["_emscripten_bind_SIGSurface_getSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGSurface_getSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1 = Module["_emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1"] = createExportWrapper("emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getSurfaceFireReactionIntensityForLifeState_1 = Module["_emscripten_bind_SIGSurface_getSurfaceFireReactionIntensityForLifeState_1"] = createExportWrapper("emscripten_bind_SIGSurface_getSurfaceFireReactionIntensityForLifeState_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindDirection_0 = Module["_emscripten_bind_SIGSurface_getWindDirection_0"] = createExportWrapper("emscripten_bind_SIGSurface_getWindDirection_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getWindSpeed_2 = Module["_emscripten_bind_SIGSurface_getWindSpeed_2"] = createExportWrapper("emscripten_bind_SIGSurface_getWindSpeed_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getAspenFuelModelNumber_0 = Module["_emscripten_bind_SIGSurface_getAspenFuelModelNumber_0"] = createExportWrapper("emscripten_bind_SIGSurface_getAspenFuelModelNumber_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelModelNumber_0 = Module["_emscripten_bind_SIGSurface_getFuelModelNumber_0"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelModelNumber_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioIndexByName_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioIndexByName_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioIndexByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getNumberOfMoistureScenarios_0 = Module["_emscripten_bind_SIGSurface_getNumberOfMoistureScenarios_0"] = createExportWrapper("emscripten_bind_SIGSurface_getNumberOfMoistureScenarios_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelCode_1 = Module["_emscripten_bind_SIGSurface_getFuelCode_1"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getFuelName_1 = Module["_emscripten_bind_SIGSurface_getFuelName_1"] = createExportWrapper("emscripten_bind_SIGSurface_getFuelName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByIndex_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByIndex_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByName_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByName_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_getMoistureScenarioNameByIndex_1 = Module["_emscripten_bind_SIGSurface_getMoistureScenarioNameByIndex_1"] = createExportWrapper("emscripten_bind_SIGSurface_getMoistureScenarioNameByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_2 = Module["_emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_2"] = createExportWrapper("emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0 = Module["_emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0"] = createExportWrapper("emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_initializeMembers_0 = Module["_emscripten_bind_SIGSurface_initializeMembers_0"] = createExportWrapper("emscripten_bind_SIGSurface_initializeMembers_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setAgeOfRough_1 = Module["_emscripten_bind_SIGSurface_setAgeOfRough_1"] = createExportWrapper("emscripten_bind_SIGSurface_setAgeOfRough_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setAspect_1 = Module["_emscripten_bind_SIGSurface_setAspect_1"] = createExportWrapper("emscripten_bind_SIGSurface_setAspect_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setAspenCuringLevel_2 = Module["_emscripten_bind_SIGSurface_setAspenCuringLevel_2"] = createExportWrapper("emscripten_bind_SIGSurface_setAspenCuringLevel_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setAspenDBH_2 = Module["_emscripten_bind_SIGSurface_setAspenDBH_2"] = createExportWrapper("emscripten_bind_SIGSurface_setAspenDBH_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setAspenFireSeverity_1 = Module["_emscripten_bind_SIGSurface_setAspenFireSeverity_1"] = createExportWrapper("emscripten_bind_SIGSurface_setAspenFireSeverity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setAspenFuelModelNumber_1 = Module["_emscripten_bind_SIGSurface_setAspenFuelModelNumber_1"] = createExportWrapper("emscripten_bind_SIGSurface_setAspenFuelModelNumber_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setCanopyCover_2 = Module["_emscripten_bind_SIGSurface_setCanopyCover_2"] = createExportWrapper("emscripten_bind_SIGSurface_setCanopyCover_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setCanopyHeight_2 = Module["_emscripten_bind_SIGSurface_setCanopyHeight_2"] = createExportWrapper("emscripten_bind_SIGSurface_setCanopyHeight_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setChaparralFuelBedDepth_2 = Module["_emscripten_bind_SIGSurface_setChaparralFuelBedDepth_2"] = createExportWrapper("emscripten_bind_SIGSurface_setChaparralFuelBedDepth_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setChaparralFuelDeadLoadFraction_1 = Module["_emscripten_bind_SIGSurface_setChaparralFuelDeadLoadFraction_1"] = createExportWrapper("emscripten_bind_SIGSurface_setChaparralFuelDeadLoadFraction_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setChaparralFuelLoadInputMode_1 = Module["_emscripten_bind_SIGSurface_setChaparralFuelLoadInputMode_1"] = createExportWrapper("emscripten_bind_SIGSurface_setChaparralFuelLoadInputMode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setChaparralFuelType_1 = Module["_emscripten_bind_SIGSurface_setChaparralFuelType_1"] = createExportWrapper("emscripten_bind_SIGSurface_setChaparralFuelType_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setChaparralTotalFuelLoad_2 = Module["_emscripten_bind_SIGSurface_setChaparralTotalFuelLoad_2"] = createExportWrapper("emscripten_bind_SIGSurface_setChaparralTotalFuelLoad_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setCrownRatio_1 = Module["_emscripten_bind_SIGSurface_setCrownRatio_1"] = createExportWrapper("emscripten_bind_SIGSurface_setCrownRatio_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setFirstFuelModelNumber_1 = Module["_emscripten_bind_SIGSurface_setFirstFuelModelNumber_1"] = createExportWrapper("emscripten_bind_SIGSurface_setFirstFuelModelNumber_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setFuelModels_1 = Module["_emscripten_bind_SIGSurface_setFuelModels_1"] = createExportWrapper("emscripten_bind_SIGSurface_setFuelModels_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setHeightOfUnderstory_2 = Module["_emscripten_bind_SIGSurface_setHeightOfUnderstory_2"] = createExportWrapper("emscripten_bind_SIGSurface_setHeightOfUnderstory_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setIsUsingChaparral_1 = Module["_emscripten_bind_SIGSurface_setIsUsingChaparral_1"] = createExportWrapper("emscripten_bind_SIGSurface_setIsUsingChaparral_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setIsUsingPalmettoGallberry_1 = Module["_emscripten_bind_SIGSurface_setIsUsingPalmettoGallberry_1"] = createExportWrapper("emscripten_bind_SIGSurface_setIsUsingPalmettoGallberry_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setIsUsingWesternAspen_1 = Module["_emscripten_bind_SIGSurface_setIsUsingWesternAspen_1"] = createExportWrapper("emscripten_bind_SIGSurface_setIsUsingWesternAspen_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureDeadAggregate_2 = Module["_emscripten_bind_SIGSurface_setMoistureDeadAggregate_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureDeadAggregate_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureHundredHour_2 = Module["_emscripten_bind_SIGSurface_setMoistureHundredHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureHundredHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureInputMode_1 = Module["_emscripten_bind_SIGSurface_setMoistureInputMode_1"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureInputMode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureLiveAggregate_2 = Module["_emscripten_bind_SIGSurface_setMoistureLiveAggregate_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureLiveAggregate_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2 = Module["_emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureLiveWoody_2 = Module["_emscripten_bind_SIGSurface_setMoistureLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureOneHour_2 = Module["_emscripten_bind_SIGSurface_setMoistureOneHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureScenarios_1 = Module["_emscripten_bind_SIGSurface_setMoistureScenarios_1"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureScenarios_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setMoistureTenHour_2 = Module["_emscripten_bind_SIGSurface_setMoistureTenHour_2"] = createExportWrapper("emscripten_bind_SIGSurface_setMoistureTenHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setOverstoryBasalArea_2 = Module["_emscripten_bind_SIGSurface_setOverstoryBasalArea_2"] = createExportWrapper("emscripten_bind_SIGSurface_setOverstoryBasalArea_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setPalmettoCoverage_2 = Module["_emscripten_bind_SIGSurface_setPalmettoCoverage_2"] = createExportWrapper("emscripten_bind_SIGSurface_setPalmettoCoverage_2");
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
var _emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_24 = Module["_emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_24"] = createExportWrapper("emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_24");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_24 = Module["_emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_24"] = createExportWrapper("emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_24");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_25 = Module["_emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_25"] = createExportWrapper("emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_25");
/** @type {function(...*):?} */
var _emscripten_bind_SIGSurface_setFuelModelNumber_1 = Module["_emscripten_bind_SIGSurface_setFuelModelNumber_1"] = createExportWrapper("emscripten_bind_SIGSurface_setFuelModelNumber_1");
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
var _emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_0 = Module["_emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_0 = Module["_emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_0 = Module["_emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenLoadLiveWoody_0 = Module["_emscripten_bind_WesternAspen_getAspenLoadLiveWoody_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenLoadLiveWoody_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0 = Module["_emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenMortality_0 = Module["_emscripten_bind_WesternAspen_getAspenMortality_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenMortality_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_0 = Module["_emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0 = Module["_emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0 = Module["_emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen_getAspenSavrLiveWoody_0 = Module["_emscripten_bind_WesternAspen_getAspenSavrLiveWoody_0"] = createExportWrapper("emscripten_bind_WesternAspen_getAspenSavrLiveWoody_0");
/** @type {function(...*):?} */
var _emscripten_bind_WesternAspen___destroy___0 = Module["_emscripten_bind_WesternAspen___destroy___0"] = createExportWrapper("emscripten_bind_WesternAspen___destroy___0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_SIGCrown_1 = Module["_emscripten_bind_SIGCrown_SIGCrown_1"] = createExportWrapper("emscripten_bind_SIGCrown_SIGCrown_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFireType_0 = Module["_emscripten_bind_SIGCrown_getFireType_0"] = createExportWrapper("emscripten_bind_SIGCrown_getFireType_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByIndex_1 = Module["_emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByIndex_1"] = createExportWrapper("emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByName_1 = Module["_emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByName_1"] = createExportWrapper("emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_isAllFuelLoadZero_1 = Module["_emscripten_bind_SIGCrown_isAllFuelLoadZero_1"] = createExportWrapper("emscripten_bind_SIGCrown_isAllFuelLoadZero_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_isFuelDynamic_1 = Module["_emscripten_bind_SIGCrown_isFuelDynamic_1"] = createExportWrapper("emscripten_bind_SIGCrown_isFuelDynamic_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_isFuelModelDefined_1 = Module["_emscripten_bind_SIGCrown_isFuelModelDefined_1"] = createExportWrapper("emscripten_bind_SIGCrown_isFuelModelDefined_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_isFuelModelReserved_1 = Module["_emscripten_bind_SIGCrown_isFuelModelReserved_1"] = createExportWrapper("emscripten_bind_SIGCrown_isFuelModelReserved_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureScenarioByIndex_1 = Module["_emscripten_bind_SIGCrown_setMoistureScenarioByIndex_1"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureScenarioByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureScenarioByName_1 = Module["_emscripten_bind_SIGCrown_setMoistureScenarioByName_1"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureScenarioByName_1");
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
var _emscripten_bind_SIGCrown_getCrownFireSpreadDistance_3 = Module["_emscripten_bind_SIGCrown_getCrownFireSpreadDistance_3"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownFireSpreadDistance_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownFireSpreadRate_1 = Module["_emscripten_bind_SIGCrown_getCrownFireSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownFireSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownFirelineIntensity_1 = Module["_emscripten_bind_SIGCrown_getCrownFirelineIntensity_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownFirelineIntensity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownFlameLength_1 = Module["_emscripten_bind_SIGCrown_getCrownFlameLength_1"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownFlameLength_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownFractionBurned_0 = Module["_emscripten_bind_SIGCrown_getCrownFractionBurned_0"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownFractionBurned_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getCrownRatio_0 = Module["_emscripten_bind_SIGCrown_getCrownRatio_0"] = createExportWrapper("emscripten_bind_SIGCrown_getCrownRatio_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFinalFirelineIntesity_1 = Module["_emscripten_bind_SIGCrown_getFinalFirelineIntesity_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFinalFirelineIntesity_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFinalHeatPerUnitArea_1 = Module["_emscripten_bind_SIGCrown_getFinalHeatPerUnitArea_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFinalHeatPerUnitArea_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFinalSpreadRate_1 = Module["_emscripten_bind_SIGCrown_getFinalSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFinalSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelHeatOfCombustionDead_2 = Module["_emscripten_bind_SIGCrown_getFuelHeatOfCombustionDead_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelHeatOfCombustionDead_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelHeatOfCombustionLive_2 = Module["_emscripten_bind_SIGCrown_getFuelHeatOfCombustionLive_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelHeatOfCombustionLive_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelLoadHundredHour_2 = Module["_emscripten_bind_SIGCrown_getFuelLoadHundredHour_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelLoadHundredHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelLoadLiveHerbaceous_2 = Module["_emscripten_bind_SIGCrown_getFuelLoadLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelLoadLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelLoadLiveWoody_2 = Module["_emscripten_bind_SIGCrown_getFuelLoadLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelLoadLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelLoadOneHour_2 = Module["_emscripten_bind_SIGCrown_getFuelLoadOneHour_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelLoadOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelLoadTenHour_2 = Module["_emscripten_bind_SIGCrown_getFuelLoadTenHour_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelLoadTenHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelMoistureOfExtinctionDead_2 = Module["_emscripten_bind_SIGCrown_getFuelMoistureOfExtinctionDead_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelMoistureOfExtinctionDead_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelSavrLiveHerbaceous_2 = Module["_emscripten_bind_SIGCrown_getFuelSavrLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelSavrLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelSavrLiveWoody_2 = Module["_emscripten_bind_SIGCrown_getFuelSavrLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelSavrLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelSavrOneHour_2 = Module["_emscripten_bind_SIGCrown_getFuelSavrOneHour_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelSavrOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelbedDepth_2 = Module["_emscripten_bind_SIGCrown_getFuelbedDepth_2"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelbedDepth_2");
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
var _emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByIndex_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByIndex_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByName_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByName_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByIndex_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByIndex_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByName_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByName_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByIndex_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByIndex_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByName_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByName_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioOneHourByIndex_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioOneHourByIndex_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioOneHourByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioOneHourByName_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioOneHourByName_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioOneHourByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioTenHourByIndex_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioTenHourByIndex_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioTenHourByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioTenHourByName_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioTenHourByName_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioTenHourByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureTenHour_1 = Module["_emscripten_bind_SIGCrown_getMoistureTenHour_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureTenHour_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getSlope_1 = Module["_emscripten_bind_SIGCrown_getSlope_1"] = createExportWrapper("emscripten_bind_SIGCrown_getSlope_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getSurfaceFireSpreadDistance_3 = Module["_emscripten_bind_SIGCrown_getSurfaceFireSpreadDistance_3"] = createExportWrapper("emscripten_bind_SIGCrown_getSurfaceFireSpreadDistance_3");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getSurfaceFireSpreadRate_1 = Module["_emscripten_bind_SIGCrown_getSurfaceFireSpreadRate_1"] = createExportWrapper("emscripten_bind_SIGCrown_getSurfaceFireSpreadRate_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getWindDirection_0 = Module["_emscripten_bind_SIGCrown_getWindDirection_0"] = createExportWrapper("emscripten_bind_SIGCrown_getWindDirection_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getWindSpeed_2 = Module["_emscripten_bind_SIGCrown_getWindSpeed_2"] = createExportWrapper("emscripten_bind_SIGCrown_getWindSpeed_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelModelNumber_0 = Module["_emscripten_bind_SIGCrown_getFuelModelNumber_0"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelModelNumber_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioIndexByName_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioIndexByName_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioIndexByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getNumberOfMoistureScenarios_0 = Module["_emscripten_bind_SIGCrown_getNumberOfMoistureScenarios_0"] = createExportWrapper("emscripten_bind_SIGCrown_getNumberOfMoistureScenarios_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelCode_1 = Module["_emscripten_bind_SIGCrown_getFuelCode_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelCode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getFuelName_1 = Module["_emscripten_bind_SIGCrown_getFuelName_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFuelName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByIndex_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByIndex_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByName_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByName_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByName_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_getMoistureScenarioNameByIndex_1 = Module["_emscripten_bind_SIGCrown_getMoistureScenarioNameByIndex_1"] = createExportWrapper("emscripten_bind_SIGCrown_getMoistureScenarioNameByIndex_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_doCrownRunRothermel_0 = Module["_emscripten_bind_SIGCrown_doCrownRunRothermel_0"] = createExportWrapper("emscripten_bind_SIGCrown_doCrownRunRothermel_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0 = Module["_emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0"] = createExportWrapper("emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_initializeMembers_0 = Module["_emscripten_bind_SIGCrown_initializeMembers_0"] = createExportWrapper("emscripten_bind_SIGCrown_initializeMembers_0");
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
var _emscripten_bind_SIGCrown_setMoistureDeadAggregate_2 = Module["_emscripten_bind_SIGCrown_setMoistureDeadAggregate_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureDeadAggregate_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureFoliar_2 = Module["_emscripten_bind_SIGCrown_setMoistureFoliar_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureFoliar_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureHundredHour_2 = Module["_emscripten_bind_SIGCrown_setMoistureHundredHour_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureHundredHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureInputMode_1 = Module["_emscripten_bind_SIGCrown_setMoistureInputMode_1"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureInputMode_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureLiveAggregate_2 = Module["_emscripten_bind_SIGCrown_setMoistureLiveAggregate_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureLiveAggregate_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureLiveHerbaceous_2 = Module["_emscripten_bind_SIGCrown_setMoistureLiveHerbaceous_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureLiveHerbaceous_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureLiveWoody_2 = Module["_emscripten_bind_SIGCrown_setMoistureLiveWoody_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureLiveWoody_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureOneHour_2 = Module["_emscripten_bind_SIGCrown_setMoistureOneHour_2"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureOneHour_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGCrown_setMoistureScenarios_1 = Module["_emscripten_bind_SIGCrown_setMoistureScenarios_1"] = createExportWrapper("emscripten_bind_SIGCrown_setMoistureScenarios_1");
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
var _emscripten_bind_SIGCrown_getFinalFlameLength_1 = Module["_emscripten_bind_SIGCrown_getFinalFlameLength_1"] = createExportWrapper("emscripten_bind_SIGCrown_getFinalFlameLength_1");
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
var _emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2 = Module["_emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2"] = createExportWrapper("emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_calculateMortality_1 = Module["_emscripten_bind_SIGMortality_calculateMortality_1"] = createExportWrapper("emscripten_bind_SIGMortality_calculateMortality_1");
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
var _emscripten_bind_SIGMortality_getTreeCrownLengthScorched_1 = Module["_emscripten_bind_SIGMortality_getTreeCrownLengthScorched_1"] = createExportWrapper("emscripten_bind_SIGMortality_getTreeCrownLengthScorched_1");
/** @type {function(...*):?} */
var _emscripten_bind_SIGMortality_getTreeCrownVolumeScorched_1 = Module["_emscripten_bind_SIGMortality_getTreeCrownVolumeScorched_1"] = createExportWrapper("emscripten_bind_SIGMortality_getTreeCrownVolumeScorched_1");
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
var _emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareFeetPerAcre = Module["_emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareFeetPerAcre"] = createExportWrapper("emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareFeetPerAcre");
/** @type {function(...*):?} */
var _emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareMetersPerHectare = Module["_emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareMetersPerHectare"] = createExportWrapper("emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareMetersPerHectare");
/** @type {function(...*):?} */
var _emscripten_enum_CuringLevelUnits_CuringLevelEnum_Fraction = Module["_emscripten_enum_CuringLevelUnits_CuringLevelEnum_Fraction"] = createExportWrapper("emscripten_enum_CuringLevelUnits_CuringLevelEnum_Fraction");
/** @type {function(...*):?} */
var _emscripten_enum_CuringLevelUnits_CuringLevelEnum_Percent = Module["_emscripten_enum_CuringLevelUnits_CuringLevelEnum_Percent"] = createExportWrapper("emscripten_enum_CuringLevelUnits_CuringLevelEnum_Percent");
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
var _emscripten_enum_ContainTactic_ContainTacticEnum_HeadAttack = Module["_emscripten_enum_ContainTactic_ContainTacticEnum_HeadAttack"] = createExportWrapper("emscripten_enum_ContainTactic_ContainTacticEnum_HeadAttack");
/** @type {function(...*):?} */
var _emscripten_enum_ContainTactic_ContainTacticEnum_RearAttack = Module["_emscripten_enum_ContainTactic_ContainTacticEnum_RearAttack"] = createExportWrapper("emscripten_enum_ContainTactic_ContainTacticEnum_RearAttack");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_ContainStatusEnum_Unreported = Module["_emscripten_enum_ContainStatus_ContainStatusEnum_Unreported"] = createExportWrapper("emscripten_enum_ContainStatus_ContainStatusEnum_Unreported");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_ContainStatusEnum_Reported = Module["_emscripten_enum_ContainStatus_ContainStatusEnum_Reported"] = createExportWrapper("emscripten_enum_ContainStatus_ContainStatusEnum_Reported");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_ContainStatusEnum_Attacked = Module["_emscripten_enum_ContainStatus_ContainStatusEnum_Attacked"] = createExportWrapper("emscripten_enum_ContainStatus_ContainStatusEnum_Attacked");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_ContainStatusEnum_Contained = Module["_emscripten_enum_ContainStatus_ContainStatusEnum_Contained"] = createExportWrapper("emscripten_enum_ContainStatus_ContainStatusEnum_Contained");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_ContainStatusEnum_Overrun = Module["_emscripten_enum_ContainStatus_ContainStatusEnum_Overrun"] = createExportWrapper("emscripten_enum_ContainStatus_ContainStatusEnum_Overrun");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_ContainStatusEnum_Exhausted = Module["_emscripten_enum_ContainStatus_ContainStatusEnum_Exhausted"] = createExportWrapper("emscripten_enum_ContainStatus_ContainStatusEnum_Exhausted");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_ContainStatusEnum_Overflow = Module["_emscripten_enum_ContainStatus_ContainStatusEnum_Overflow"] = createExportWrapper("emscripten_enum_ContainStatus_ContainStatusEnum_Overflow");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_ContainStatusEnum_SizeLimitExceeded = Module["_emscripten_enum_ContainStatus_ContainStatusEnum_SizeLimitExceeded"] = createExportWrapper("emscripten_enum_ContainStatus_ContainStatusEnum_SizeLimitExceeded");
/** @type {function(...*):?} */
var _emscripten_enum_ContainStatus_ContainStatusEnum_TimeLimitExceeded = Module["_emscripten_enum_ContainStatus_ContainStatusEnum_TimeLimitExceeded"] = createExportWrapper("emscripten_enum_ContainStatus_ContainStatusEnum_TimeLimitExceeded");
/** @type {function(...*):?} */
var _emscripten_enum_ContainFlank_ContainFlankEnum_LeftFlank = Module["_emscripten_enum_ContainFlank_ContainFlankEnum_LeftFlank"] = createExportWrapper("emscripten_enum_ContainFlank_ContainFlankEnum_LeftFlank");
/** @type {function(...*):?} */
var _emscripten_enum_ContainFlank_ContainFlankEnum_RightFlank = Module["_emscripten_enum_ContainFlank_ContainFlankEnum_RightFlank"] = createExportWrapper("emscripten_enum_ContainFlank_ContainFlankEnum_RightFlank");
/** @type {function(...*):?} */
var _emscripten_enum_ContainFlank_ContainFlankEnum_BothFlanks = Module["_emscripten_enum_ContainFlank_ContainFlankEnum_BothFlanks"] = createExportWrapper("emscripten_enum_ContainFlank_ContainFlankEnum_BothFlanks");
/** @type {function(...*):?} */
var _emscripten_enum_ContainFlank_ContainFlankEnum_NeitherFlank = Module["_emscripten_enum_ContainFlank_ContainFlankEnum_NeitherFlank"] = createExportWrapper("emscripten_enum_ContainFlank_ContainFlankEnum_NeitherFlank");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PonderosaPineLitter = Module["_emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PonderosaPineLitter"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PonderosaPineLitter");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodRottenChunky = Module["_emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodRottenChunky"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodRottenChunky");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodPowderDeep = Module["_emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodPowderDeep"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodPowderDeep");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkWoodPowderShallow = Module["_emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkWoodPowderShallow"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkWoodPowderShallow");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_LodgepolePineDuff = Module["_emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_LodgepolePineDuff"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_LodgepolePineDuff");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_DouglasFirDuff = Module["_emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_DouglasFirDuff"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_DouglasFirDuff");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_HighAltitudeMixed = Module["_emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_HighAltitudeMixed"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_HighAltitudeMixed");
/** @type {function(...*):?} */
var _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PeatMoss = Module["_emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PeatMoss"] = createExportWrapper("emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PeatMoss");
/** @type {function(...*):?} */
var _emscripten_enum_LightningCharge_LightningChargeEnum_Negative = Module["_emscripten_enum_LightningCharge_LightningChargeEnum_Negative"] = createExportWrapper("emscripten_enum_LightningCharge_LightningChargeEnum_Negative");
/** @type {function(...*):?} */
var _emscripten_enum_LightningCharge_LightningChargeEnum_Positive = Module["_emscripten_enum_LightningCharge_LightningChargeEnum_Positive"] = createExportWrapper("emscripten_enum_LightningCharge_LightningChargeEnum_Positive");
/** @type {function(...*):?} */
var _emscripten_enum_LightningCharge_LightningChargeEnum_Unknown = Module["_emscripten_enum_LightningCharge_LightningChargeEnum_Unknown"] = createExportWrapper("emscripten_enum_LightningCharge_LightningChargeEnum_Unknown");
/** @type {function(...*):?} */
var _emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_CLOSED = Module["_emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_CLOSED"] = createExportWrapper("emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_CLOSED");
/** @type {function(...*):?} */
var _emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_OPEN = Module["_emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_OPEN"] = createExportWrapper("emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_OPEN");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_ENGELMANN_SPRUCE = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_ENGELMANN_SPRUCE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_ENGELMANN_SPRUCE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_DOUGLAS_FIR = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_DOUGLAS_FIR"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_DOUGLAS_FIR");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SUBALPINE_FIR = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SUBALPINE_FIR"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SUBALPINE_FIR");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_HEMLOCK = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_HEMLOCK"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_HEMLOCK");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_PONDEROSA_PINE = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_PONDEROSA_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_PONDEROSA_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LODGEPOLE_PINE = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LODGEPOLE_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LODGEPOLE_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_WHITE_PINE = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_WHITE_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_WHITE_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_GRAND_FIR = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_GRAND_FIR"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_GRAND_FIR");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_BALSAM_FIR = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_BALSAM_FIR"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_BALSAM_FIR");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SLASH_PINE = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SLASH_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SLASH_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LONGLEAF_PINE = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LONGLEAF_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LONGLEAF_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_POND_PINE = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_POND_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_POND_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SHORTLEAF_PINE = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SHORTLEAF_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SHORTLEAF_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LOBLOLLY_PINE = Module["_emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LOBLOLLY_PINE"] = createExportWrapper("emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LOBLOLLY_PINE");
/** @type {function(...*):?} */
var _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_WINDWARD = Module["_emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_WINDWARD"] = createExportWrapper("emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_WINDWARD");
/** @type {function(...*):?} */
var _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_VALLEY_BOTTOM = Module["_emscripten_enum_SpotFireLocation_SpotFireLocationEnum_VALLEY_BOTTOM"] = createExportWrapper("emscripten_enum_SpotFireLocation_SpotFireLocationEnum_VALLEY_BOTTOM");
/** @type {function(...*):?} */
var _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_LEEWARD = Module["_emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_LEEWARD"] = createExportWrapper("emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_LEEWARD");
/** @type {function(...*):?} */
var _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_RIDGE_TOP = Module["_emscripten_enum_SpotFireLocation_SpotFireLocationEnum_RIDGE_TOP"] = createExportWrapper("emscripten_enum_SpotFireLocation_SpotFireLocationEnum_RIDGE_TOP");
/** @type {function(...*):?} */
var _emscripten_enum_FuelLifeState_FuelLifeStateEnum_Dead = Module["_emscripten_enum_FuelLifeState_FuelLifeStateEnum_Dead"] = createExportWrapper("emscripten_enum_FuelLifeState_FuelLifeStateEnum_Dead");
/** @type {function(...*):?} */
var _emscripten_enum_FuelLifeState_FuelLifeStateEnum_Live = Module["_emscripten_enum_FuelLifeState_FuelLifeStateEnum_Live"] = createExportWrapper("emscripten_enum_FuelLifeState_FuelLifeStateEnum_Live");
/** @type {function(...*):?} */
var _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLifeStates = Module["_emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLifeStates"] = createExportWrapper("emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLifeStates");
/** @type {function(...*):?} */
var _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLiveSizeClasses = Module["_emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLiveSizeClasses"] = createExportWrapper("emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLiveSizeClasses");
/** @type {function(...*):?} */
var _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxDeadSizeClasses = Module["_emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxDeadSizeClasses"] = createExportWrapper("emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxDeadSizeClasses");
/** @type {function(...*):?} */
var _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxParticles = Module["_emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxParticles"] = createExportWrapper("emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxParticles");
/** @type {function(...*):?} */
var _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxSavrSizeClasses = Module["_emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxSavrSizeClasses"] = createExportWrapper("emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxSavrSizeClasses");
/** @type {function(...*):?} */
var _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxFuelModels = Module["_emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxFuelModels"] = createExportWrapper("emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxFuelModels");
/** @type {function(...*):?} */
var _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low = Module["_emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low"] = createExportWrapper("emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low");
/** @type {function(...*):?} */
var _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate = Module["_emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate"] = createExportWrapper("emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate");
/** @type {function(...*):?} */
var _emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_NotSet = Module["_emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_NotSet"] = createExportWrapper("emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_NotSet");
/** @type {function(...*):?} */
var _emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_Chamise = Module["_emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_Chamise"] = createExportWrapper("emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_Chamise");
/** @type {function(...*):?} */
var _emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_MixedBrush = Module["_emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_MixedBrush"] = createExportWrapper("emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_MixedBrush");
/** @type {function(...*):?} */
var _emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_DirectFuelLoad = Module["_emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_DirectFuelLoad"] = createExportWrapper("emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_DirectFuelLoad");
/** @type {function(...*):?} */
var _emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_FuelLoadFromDepthAndChaparralType = Module["_emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_FuelLoadFromDepthAndChaparralType"] = createExportWrapper("emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_FuelLoadFromDepthAndChaparralType");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_BySizeClass = Module["_emscripten_enum_MoistureInputMode_MoistureInputModeEnum_BySizeClass"] = createExportWrapper("emscripten_enum_MoistureInputMode_MoistureInputModeEnum_BySizeClass");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_AllAggregate = Module["_emscripten_enum_MoistureInputMode_MoistureInputModeEnum_AllAggregate"] = createExportWrapper("emscripten_enum_MoistureInputMode_MoistureInputModeEnum_AllAggregate");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_DeadAggregateAndLiveSizeClass = Module["_emscripten_enum_MoistureInputMode_MoistureInputModeEnum_DeadAggregateAndLiveSizeClass"] = createExportWrapper("emscripten_enum_MoistureInputMode_MoistureInputModeEnum_DeadAggregateAndLiveSizeClass");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_LiveAggregateAndDeadSizeClass = Module["_emscripten_enum_MoistureInputMode_MoistureInputModeEnum_LiveAggregateAndDeadSizeClass"] = createExportWrapper("emscripten_enum_MoistureInputMode_MoistureInputModeEnum_LiveAggregateAndDeadSizeClass");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_MoistureScenario = Module["_emscripten_enum_MoistureInputMode_MoistureInputModeEnum_MoistureScenario"] = createExportWrapper("emscripten_enum_MoistureInputMode_MoistureInputModeEnum_MoistureScenario");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_OneHour = Module["_emscripten_enum_MoistureClassInput_MoistureClassInputEnum_OneHour"] = createExportWrapper("emscripten_enum_MoistureClassInput_MoistureClassInputEnum_OneHour");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_TenHour = Module["_emscripten_enum_MoistureClassInput_MoistureClassInputEnum_TenHour"] = createExportWrapper("emscripten_enum_MoistureClassInput_MoistureClassInputEnum_TenHour");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_HundredHour = Module["_emscripten_enum_MoistureClassInput_MoistureClassInputEnum_HundredHour"] = createExportWrapper("emscripten_enum_MoistureClassInput_MoistureClassInputEnum_HundredHour");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveHerbaceous = Module["_emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveHerbaceous"] = createExportWrapper("emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveHerbaceous");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveWoody = Module["_emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveWoody"] = createExportWrapper("emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveWoody");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_DeadAggregate = Module["_emscripten_enum_MoistureClassInput_MoistureClassInputEnum_DeadAggregate"] = createExportWrapper("emscripten_enum_MoistureClassInput_MoistureClassInputEnum_DeadAggregate");
/** @type {function(...*):?} */
var _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveAggregate = Module["_emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveAggregate"] = createExportWrapper("emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveAggregate");
/** @type {function(...*):?} */
var _emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromIgnitionPoint = Module["_emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromIgnitionPoint"] = createExportWrapper("emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromIgnitionPoint");
/** @type {function(...*):?} */
var _emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromPerimeter = Module["_emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromPerimeter"] = createExportWrapper("emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromPerimeter");
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
var _emscripten_enum_CrownDamageType_not_set = Module["_emscripten_enum_CrownDamageType_not_set"] = createExportWrapper("emscripten_enum_CrownDamageType_not_set");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageType_crown_length = Module["_emscripten_enum_CrownDamageType_crown_length"] = createExportWrapper("emscripten_enum_CrownDamageType_crown_length");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageType_crown_volume = Module["_emscripten_enum_CrownDamageType_crown_volume"] = createExportWrapper("emscripten_enum_CrownDamageType_crown_volume");
/** @type {function(...*):?} */
var _emscripten_enum_CrownDamageType_crown_kill = Module["_emscripten_enum_CrownDamageType_crown_kill"] = createExportWrapper("emscripten_enum_CrownDamageType_crown_kill");
/** @type {function(...*):?} */
var _emscripten_enum_EquationType_not_set = Module["_emscripten_enum_EquationType_not_set"] = createExportWrapper("emscripten_enum_EquationType_not_set");
/** @type {function(...*):?} */
var _emscripten_enum_EquationType_crown_scorch = Module["_emscripten_enum_EquationType_crown_scorch"] = createExportWrapper("emscripten_enum_EquationType_crown_scorch");
/** @type {function(...*):?} */
var _emscripten_enum_EquationType_bole_char = Module["_emscripten_enum_EquationType_bole_char"] = createExportWrapper("emscripten_enum_EquationType_bole_char");
/** @type {function(...*):?} */
var _emscripten_enum_EquationType_crown_damage = Module["_emscripten_enum_EquationType_crown_damage"] = createExportWrapper("emscripten_enum_EquationType_crown_damage");
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
var _emscripten_enum_MortalityRateUnits_MortalityRateUnitsEnum_Fraction = Module["_emscripten_enum_MortalityRateUnits_MortalityRateUnitsEnum_Fraction"] = createExportWrapper("emscripten_enum_MortalityRateUnits_MortalityRateUnitsEnum_Fraction");
/** @type {function(...*):?} */
var _emscripten_enum_MortalityRateUnits_MortalityRateUnitsEnum_Percent = Module["_emscripten_enum_MortalityRateUnits_MortalityRateUnitsEnum_Percent"] = createExportWrapper("emscripten_enum_MortalityRateUnits_MortalityRateUnitsEnum_Percent");
/** @type {function(...*):?} */
var _emscripten_enum_RegionCode_interior_west = Module["_emscripten_enum_RegionCode_interior_west"] = createExportWrapper("emscripten_enum_RegionCode_interior_west");
/** @type {function(...*):?} */
var _emscripten_enum_RegionCode_pacific_west = Module["_emscripten_enum_RegionCode_pacific_west"] = createExportWrapper("emscripten_enum_RegionCode_pacific_west");
/** @type {function(...*):?} */
var _emscripten_enum_RegionCode_north_east = Module["_emscripten_enum_RegionCode_north_east"] = createExportWrapper("emscripten_enum_RegionCode_north_east");
/** @type {function(...*):?} */
var _emscripten_enum_RegionCode_south_east = Module["_emscripten_enum_RegionCode_south_east"] = createExportWrapper("emscripten_enum_RegionCode_south_east");
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
var ___start_em_js = Module['___start_em_js'] = 113880;
var ___stop_em_js = Module['___stop_em_js'] = 113978;
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

function invoke_vidii(index,a1,a2,a3,a4) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2,a3,a4);
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

function invoke_dii(index,a1,a2) {
  var sp = stackSave();
  try {
    return getWasmTableEntry(index)(a1,a2);
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

function invoke_viiiddddd(index,a1,a2,a3,a4,a5,a6,a7,a8) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2,a3,a4,a5,a6,a7,a8);
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

function invoke_viddidiidd(index,a1,a2,a3,a4,a5,a6,a7,a8,a9) {
  var sp = stackSave();
  try {
    getWasmTableEntry(index)(a1,a2,a3,a4,a5,a6,a7,a8,a9);
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
  'stringToNewUTF8',
  'exitJS',
  'inetPton4',
  'inetNtop4',
  'inetPton6',
  'inetNtop6',
  'readSockaddr',
  'writeSockaddr',
  'getHostByName',
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
  'zeroMemory',
  'getHeapMax',
  'emscripten_realloc_buffer',
  'ENV',
  'ERRNO_CODES',
  'ERRNO_MESSAGES',
  'setErrNo',
  'DNS',
  'Protocols',
  'Sockets',
  'getRandomDevice',
  'timers',
  'warnOnce',
  'UNWIND_CACHE',
  'readEmAsmArgsArray',
  'asyncLoad',
  'alignMemory',
  'mmapAlloc',
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
  'demangle',
  'demangleAll',
  'ExitStatus',
  'doReadv',
  'doWritev',
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
    _fflush(0);
    // also flush in the JS FS layer
    ['stdout', 'stderr'].forEach(function(name) {
      var info = FS.analyzePath('/dev/' + name);
      if (!info) return;
      var stream = info.object;
      var rdev = stream.rdev;
      var tty = TTY.ttys[rdev];
      if (tty && tty.output && tty.output.length) {
        has = true;
      }
    });
  } catch(e) {}
  out = oldOut;
  err = oldErr;
  if (has) {
    warnOnce('stdio streams had content in them that was not flushed. you should set EXIT_RUNTIME to 1 (see the FAQ), or make sure to emit a newline when you printf etc.');
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

FireSize.prototype['getBackingSpreadRate'] = FireSize.prototype.getBackingSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_FireSize_getBackingSpreadRate_1(self, spreadRateUnits);
};;

FireSize.prototype['getEccentricity'] = FireSize.prototype.getEccentricity = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_FireSize_getEccentricity_0(self);
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

FireSize.prototype['getFireArea'] = FireSize.prototype.getFireArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFireArea_3(self, areaUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getFireLength'] = FireSize.prototype.getFireLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFireLength_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getFireLengthToWidthRatio'] = FireSize.prototype.getFireLengthToWidthRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_FireSize_getFireLengthToWidthRatio_0(self);
};;

FireSize.prototype['getFirePerimeter'] = FireSize.prototype.getFirePerimeter = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFirePerimeter_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getMaxFireWidth'] = FireSize.prototype.getMaxFireWidth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getMaxFireWidth_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['calculateFireBasicDimensions'] = FireSize.prototype.calculateFireBasicDimensions = /** @suppress {undefinedVars, duplicate} @this{Object} */function(effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits) {
  var self = this.ptr;
  if (effectiveWindSpeed && typeof effectiveWindSpeed === 'object') effectiveWindSpeed = effectiveWindSpeed.ptr;
  if (windSpeedRateUnits && typeof windSpeedRateUnits === 'object') windSpeedRateUnits = windSpeedRateUnits.ptr;
  if (forwardSpreadRate && typeof forwardSpreadRate === 'object') forwardSpreadRate = forwardSpreadRate.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  _emscripten_bind_FireSize_calculateFireBasicDimensions_4(self, effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits);
};;

  FireSize.prototype['__destroy__'] = FireSize.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_FireSize___destroy___0(self);
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
// SIGMoistureScenarios
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGMoistureScenarios() {
  this.ptr = _emscripten_bind_SIGMoistureScenarios_SIGMoistureScenarios_0();
  getCache(SIGMoistureScenarios)[this.ptr] = this;
};;
SIGMoistureScenarios.prototype = Object.create(WrapperObject.prototype);
SIGMoistureScenarios.prototype.constructor = SIGMoistureScenarios;
SIGMoistureScenarios.prototype.__class__ = SIGMoistureScenarios;
SIGMoistureScenarios.__cache__ = {};
Module['SIGMoistureScenarios'] = SIGMoistureScenarios;

SIGMoistureScenarios.prototype['getIsMoistureScenarioDefinedByIndex'] = SIGMoistureScenarios.prototype.getIsMoistureScenarioDefinedByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return !!(_emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByIndex_1(self, index));
};;

SIGMoistureScenarios.prototype['getIsMoistureScenarioDefinedByName'] = SIGMoistureScenarios.prototype.getIsMoistureScenarioDefinedByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return !!(_emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByName_1(self, name));
};;

SIGMoistureScenarios.prototype['getMoistureScenarioHundredHourByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioHundredHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByIndex_1(self, index);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioHundredHourByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioHundredHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByName_1(self, name);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioLiveHerbaceousByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveHerbaceousByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByIndex_1(self, index);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioLiveHerbaceousByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveHerbaceousByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByName_1(self, name);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioLiveWoodyByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveWoodyByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByIndex_1(self, index);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioLiveWoodyByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveWoodyByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByName_1(self, name);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioOneHourByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioOneHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByIndex_1(self, index);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioOneHourByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioOneHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByName_1(self, name);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioTenHourByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioTenHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByIndex_1(self, index);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioTenHourByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioTenHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByName_1(self, name);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioIndexByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioIndexByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioIndexByName_1(self, name);
};;

SIGMoistureScenarios.prototype['getNumberOfMoistureScenarios'] = SIGMoistureScenarios.prototype.getNumberOfMoistureScenarios = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getNumberOfMoistureScenarios_0(self);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioDescriptionByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioDescriptionByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByIndex_1(self, index));
};;

SIGMoistureScenarios.prototype['getMoistureScenarioDescriptionByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioDescriptionByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return UTF8ToString(_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByName_1(self, name));
};;

SIGMoistureScenarios.prototype['getMoistureScenarioNameByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioNameByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioNameByIndex_1(self, index));
};;

  SIGMoistureScenarios.prototype['__destroy__'] = SIGMoistureScenarios.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGMoistureScenarios___destroy___0(self);
};
// Spot
/** @suppress {undefinedVars, duplicate} @this{Object} */function Spot() {
  this.ptr = _emscripten_bind_Spot_Spot_0();
  getCache(Spot)[this.ptr] = this;
};;
Spot.prototype = Object.create(WrapperObject.prototype);
Spot.prototype.constructor = Spot;
Spot.prototype.__class__ = Spot;
Spot.__cache__ = {};
Module['Spot'] = Spot;

Spot.prototype['getDownwindCanopyMode'] = Spot.prototype.getDownwindCanopyMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_Spot_getDownwindCanopyMode_0(self);
};;

Spot.prototype['getLocation'] = Spot.prototype.getLocation = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_Spot_getLocation_0(self);
};;

Spot.prototype['getTreeSpecies'] = Spot.prototype.getTreeSpecies = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_Spot_getTreeSpecies_0(self);
};;

Spot.prototype['getBurningPileFlameHeight'] = Spot.prototype.getBurningPileFlameHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameHeightUnits) {
  var self = this.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  return _emscripten_bind_Spot_getBurningPileFlameHeight_1(self, flameHeightUnits);
};;

Spot.prototype['getCoverHeightUsedForBurningPile'] = Spot.prototype.getCoverHeightUsedForBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_Spot_getCoverHeightUsedForBurningPile_1(self, coverHeightUnits);
};;

Spot.prototype['getCoverHeightUsedForSurfaceFire'] = Spot.prototype.getCoverHeightUsedForSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_Spot_getCoverHeightUsedForSurfaceFire_1(self, coverHeightUnits);
};;

Spot.prototype['getCoverHeightUsedForTorchingTrees'] = Spot.prototype.getCoverHeightUsedForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_Spot_getCoverHeightUsedForTorchingTrees_1(self, coverHeightUnits);
};;

Spot.prototype['getDBH'] = Spot.prototype.getDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(DBHUnits) {
  var self = this.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  return _emscripten_bind_Spot_getDBH_1(self, DBHUnits);
};;

Spot.prototype['getDownwindCoverHeight'] = Spot.prototype.getDownwindCoverHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_Spot_getDownwindCoverHeight_1(self, coverHeightUnits);
};;

Spot.prototype['getFlameDurationForTorchingTrees'] = Spot.prototype.getFlameDurationForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(durationUnits) {
  var self = this.ptr;
  if (durationUnits && typeof durationUnits === 'object') durationUnits = durationUnits.ptr;
  return _emscripten_bind_Spot_getFlameDurationForTorchingTrees_1(self, durationUnits);
};;

Spot.prototype['getFlameHeightForTorchingTrees'] = Spot.prototype.getFlameHeightForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameHeightUnits) {
  var self = this.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  return _emscripten_bind_Spot_getFlameHeightForTorchingTrees_1(self, flameHeightUnits);
};;

Spot.prototype['getFlameRatioForTorchingTrees'] = Spot.prototype.getFlameRatioForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_Spot_getFlameRatioForTorchingTrees_0(self);
};;

Spot.prototype['getMaxFirebrandHeightFromBurningPile'] = Spot.prototype.getMaxFirebrandHeightFromBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firebrandHeightUnits) {
  var self = this.ptr;
  if (firebrandHeightUnits && typeof firebrandHeightUnits === 'object') firebrandHeightUnits = firebrandHeightUnits.ptr;
  return _emscripten_bind_Spot_getMaxFirebrandHeightFromBurningPile_1(self, firebrandHeightUnits);
};;

Spot.prototype['getMaxFirebrandHeightFromSurfaceFire'] = Spot.prototype.getMaxFirebrandHeightFromSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firebrandHeightUnits) {
  var self = this.ptr;
  if (firebrandHeightUnits && typeof firebrandHeightUnits === 'object') firebrandHeightUnits = firebrandHeightUnits.ptr;
  return _emscripten_bind_Spot_getMaxFirebrandHeightFromSurfaceFire_1(self, firebrandHeightUnits);
};;

Spot.prototype['getMaxFirebrandHeightFromTorchingTrees'] = Spot.prototype.getMaxFirebrandHeightFromTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firebrandHeightUnits) {
  var self = this.ptr;
  if (firebrandHeightUnits && typeof firebrandHeightUnits === 'object') firebrandHeightUnits = firebrandHeightUnits.ptr;
  return _emscripten_bind_Spot_getMaxFirebrandHeightFromTorchingTrees_1(self, firebrandHeightUnits);
};;

Spot.prototype['getMaxFlatTerrainSpottingDistanceFromBurningPile'] = Spot.prototype.getMaxFlatTerrainSpottingDistanceFromBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1(self, spottingDistanceUnits);
};;

Spot.prototype['getMaxFlatTerrainSpottingDistanceFromSurfaceFire'] = Spot.prototype.getMaxFlatTerrainSpottingDistanceFromSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1(self, spottingDistanceUnits);
};;

Spot.prototype['getMaxFlatTerrainSpottingDistanceFromTorchingTrees'] = Spot.prototype.getMaxFlatTerrainSpottingDistanceFromTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_Spot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1(self, spottingDistanceUnits);
};;

Spot.prototype['getMaxMountainousTerrainSpottingDistanceFromBurningPile'] = Spot.prototype.getMaxMountainousTerrainSpottingDistanceFromBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1(self, spottingDistanceUnits);
};;

Spot.prototype['getMaxMountainousTerrainSpottingDistanceFromSurfaceFire'] = Spot.prototype.getMaxMountainousTerrainSpottingDistanceFromSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1(self, spottingDistanceUnits);
};;

Spot.prototype['getMaxMountainousTerrainSpottingDistanceFromTorchingTrees'] = Spot.prototype.getMaxMountainousTerrainSpottingDistanceFromTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_Spot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1(self, spottingDistanceUnits);
};;

Spot.prototype['getRidgeToValleyDistance'] = Spot.prototype.getRidgeToValleyDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ridgeToValleyDistanceUnits) {
  var self = this.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  return _emscripten_bind_Spot_getRidgeToValleyDistance_1(self, ridgeToValleyDistanceUnits);
};;

Spot.prototype['getRidgeToValleyElevation'] = Spot.prototype.getRidgeToValleyElevation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(elevationUnits) {
  var self = this.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  return _emscripten_bind_Spot_getRidgeToValleyElevation_1(self, elevationUnits);
};;

Spot.prototype['getSurfaceFlameLength'] = Spot.prototype.getSurfaceFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(surfaceFlameLengthUnits) {
  var self = this.ptr;
  if (surfaceFlameLengthUnits && typeof surfaceFlameLengthUnits === 'object') surfaceFlameLengthUnits = surfaceFlameLengthUnits.ptr;
  return _emscripten_bind_Spot_getSurfaceFlameLength_1(self, surfaceFlameLengthUnits);
};;

Spot.prototype['getTreeHeight'] = Spot.prototype.getTreeHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeHeightUnits) {
  var self = this.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  return _emscripten_bind_Spot_getTreeHeight_1(self, treeHeightUnits);
};;

Spot.prototype['getWindSpeedAtTwentyFeet'] = Spot.prototype.getWindSpeedAtTwentyFeet = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  return _emscripten_bind_Spot_getWindSpeedAtTwentyFeet_1(self, windSpeedUnits);
};;

Spot.prototype['getTorchingTrees'] = Spot.prototype.getTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_Spot_getTorchingTrees_0(self);
};;

Spot.prototype['calculateSpottingDistanceFromBurningPile'] = Spot.prototype.calculateSpottingDistanceFromBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_Spot_calculateSpottingDistanceFromBurningPile_0(self);
};;

Spot.prototype['calculateSpottingDistanceFromSurfaceFire'] = Spot.prototype.calculateSpottingDistanceFromSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_Spot_calculateSpottingDistanceFromSurfaceFire_0(self);
};;

Spot.prototype['calculateSpottingDistanceFromTorchingTrees'] = Spot.prototype.calculateSpottingDistanceFromTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_Spot_calculateSpottingDistanceFromTorchingTrees_0(self);
};;

Spot.prototype['initializeMembers'] = Spot.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_Spot_initializeMembers_0(self);
};;

Spot.prototype['setBurningPileFlameHeight'] = Spot.prototype.setBurningPileFlameHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(buringPileflameHeight, flameHeightUnits) {
  var self = this.ptr;
  if (buringPileflameHeight && typeof buringPileflameHeight === 'object') buringPileflameHeight = buringPileflameHeight.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  _emscripten_bind_Spot_setBurningPileFlameHeight_2(self, buringPileflameHeight, flameHeightUnits);
};;

Spot.prototype['setDBH'] = Spot.prototype.setDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(DBH, DBHUnits) {
  var self = this.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  _emscripten_bind_Spot_setDBH_2(self, DBH, DBHUnits);
};;

Spot.prototype['setDownwindCanopyMode'] = Spot.prototype.setDownwindCanopyMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(downwindCanopyMode) {
  var self = this.ptr;
  if (downwindCanopyMode && typeof downwindCanopyMode === 'object') downwindCanopyMode = downwindCanopyMode.ptr;
  _emscripten_bind_Spot_setDownwindCanopyMode_1(self, downwindCanopyMode);
};;

Spot.prototype['setDownwindCoverHeight'] = Spot.prototype.setDownwindCoverHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(downwindCoverHeight, coverHeightUnits) {
  var self = this.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  _emscripten_bind_Spot_setDownwindCoverHeight_2(self, downwindCoverHeight, coverHeightUnits);
};;

Spot.prototype['setFlameLength'] = Spot.prototype.setFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_Spot_setFlameLength_2(self, flameLength, flameLengthUnits);
};;

Spot.prototype['setLocation'] = Spot.prototype.setLocation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  _emscripten_bind_Spot_setLocation_1(self, location);
};;

Spot.prototype['setRidgeToValleyDistance'] = Spot.prototype.setRidgeToValleyDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ridgeToValleyDistance, ridgeToValleyDistanceUnits) {
  var self = this.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  _emscripten_bind_Spot_setRidgeToValleyDistance_2(self, ridgeToValleyDistance, ridgeToValleyDistanceUnits);
};;

Spot.prototype['setRidgeToValleyElevation'] = Spot.prototype.setRidgeToValleyElevation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ridgeToValleyElevation, elevationUnits) {
  var self = this.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  _emscripten_bind_Spot_setRidgeToValleyElevation_2(self, ridgeToValleyElevation, elevationUnits);
};;

Spot.prototype['setTorchingTrees'] = Spot.prototype.setTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(torchingTrees) {
  var self = this.ptr;
  if (torchingTrees && typeof torchingTrees === 'object') torchingTrees = torchingTrees.ptr;
  _emscripten_bind_Spot_setTorchingTrees_1(self, torchingTrees);
};;

Spot.prototype['setTreeHeight'] = Spot.prototype.setTreeHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeHeight, treeHeightUnits) {
  var self = this.ptr;
  if (treeHeight && typeof treeHeight === 'object') treeHeight = treeHeight.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  _emscripten_bind_Spot_setTreeHeight_2(self, treeHeight, treeHeightUnits);
};;

Spot.prototype['setTreeSpecies'] = Spot.prototype.setTreeSpecies = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeSpecies) {
  var self = this.ptr;
  if (treeSpecies && typeof treeSpecies === 'object') treeSpecies = treeSpecies.ptr;
  _emscripten_bind_Spot_setTreeSpecies_1(self, treeSpecies);
};;

Spot.prototype['setWindSpeedAtTwentyFeet'] = Spot.prototype.setWindSpeedAtTwentyFeet = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_Spot_setWindSpeedAtTwentyFeet_2(self, windSpeedAtTwentyFeet, windSpeedUnits);
};;

Spot.prototype['updateSpotInputsForBurningPile'] = Spot.prototype.updateSpotInputsForBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  if (downwindCanopyMode && typeof downwindCanopyMode === 'object') downwindCanopyMode = downwindCanopyMode.ptr;
  if (buringPileFlameHeight && typeof buringPileFlameHeight === 'object') buringPileFlameHeight = buringPileFlameHeight.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_Spot_updateSpotInputsForBurningPile_12(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits);
};;

Spot.prototype['updateSpotInputsForSurfaceFire'] = Spot.prototype.updateSpotInputsForSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, windSpeedAtTwentyFeet, windSpeedUnits, flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  if (downwindCanopyMode && typeof downwindCanopyMode === 'object') downwindCanopyMode = downwindCanopyMode.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_Spot_updateSpotInputsForSurfaceFire_12(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, windSpeedAtTwentyFeet, windSpeedUnits, flameLength, flameLengthUnits);
};;

Spot.prototype['updateSpotInputsForTorchingTrees'] = Spot.prototype.updateSpotInputsForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  if (downwindCanopyMode && typeof downwindCanopyMode === 'object') downwindCanopyMode = downwindCanopyMode.ptr;
  if (torchingTrees && typeof torchingTrees === 'object') torchingTrees = torchingTrees.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  if (treeHeight && typeof treeHeight === 'object') treeHeight = treeHeight.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  if (treeSpecies && typeof treeSpecies === 'object') treeSpecies = treeSpecies.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_Spot_updateSpotInputsForTorchingTrees_16(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits);
};;

  Spot.prototype['__destroy__'] = Spot.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_Spot___destroy___0(self);
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

SIGFuelModels.prototype['isAllFuelLoadZero'] = SIGFuelModels.prototype.isAllFuelLoadZero = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_isAllFuelLoadZero_1(self, fuelModelNumber));
};;

SIGFuelModels.prototype['isFuelModelDefined'] = SIGFuelModels.prototype.isFuelModelDefined = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_isFuelModelDefined_1(self, fuelModelNumber));
};;

SIGFuelModels.prototype['isFuelModelReserved'] = SIGFuelModels.prototype.isFuelModelReserved = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_isFuelModelReserved_1(self, fuelModelNumber));
};;

SIGFuelModels.prototype['setCustomFuelModel'] = SIGFuelModels.prototype.setCustomFuelModel = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, code, name, fuelBedDepth, lengthUnits, moistureOfExtinctionDead, moistureUnits, heatOfCombustionDead, heatOfCombustionLive, heatOfCombustionUnits, fuelLoadOneHour, fuelLoadTenHour, fuelLoadHundredHour, fuelLoadLiveHerbaceous, fuelLoadLiveWoody, loadingUnits, savrOneHour, savrLiveHerbaceous, savrLiveWoody, savrUnits, isDynamic) {
  var self = this.ptr;
  ensureCache.prepare();
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
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
  return !!(_emscripten_bind_SIGFuelModels_setCustomFuelModel_21(self, fuelModelNumber, code, name, fuelBedDepth, lengthUnits, moistureOfExtinctionDead, moistureUnits, heatOfCombustionDead, heatOfCombustionLive, heatOfCombustionUnits, fuelLoadOneHour, fuelLoadTenHour, fuelLoadHundredHour, fuelLoadLiveHerbaceous, fuelLoadLiveWoody, loadingUnits, savrOneHour, savrLiveHerbaceous, savrLiveWoody, savrUnits, isDynamic));
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

SIGFuelModels.prototype['getHeatOfCombustionLive'] = SIGFuelModels.prototype.getHeatOfCombustionLive = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2(self, fuelModelNumber, heatOfCombustionUnits);
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

SIGSurface.prototype['getAspenFireSeverity'] = SIGSurface.prototype.getAspenFireSeverity = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getAspenFireSeverity_0(self);
};;

SIGSurface.prototype['getChaparralFuelType'] = SIGSurface.prototype.getChaparralFuelType = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getChaparralFuelType_0(self);
};;

SIGSurface.prototype['getMoistureInputMode'] = SIGSurface.prototype.getMoistureInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getMoistureInputMode_0(self);
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

SIGSurface.prototype['getIsMoistureScenarioDefinedByIndex'] = SIGSurface.prototype.getIsMoistureScenarioDefinedByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return !!(_emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByIndex_1(self, index));
};;

SIGSurface.prototype['getIsMoistureScenarioDefinedByName'] = SIGSurface.prototype.getIsMoistureScenarioDefinedByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return !!(_emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByName_1(self, name));
};;

SIGSurface.prototype['getIsUsingChaparral'] = SIGSurface.prototype.getIsUsingChaparral = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGSurface_getIsUsingChaparral_0(self));
};;

SIGSurface.prototype['getIsUsingPalmettoGallberry'] = SIGSurface.prototype.getIsUsingPalmettoGallberry = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGSurface_getIsUsingPalmettoGallberry_0(self));
};;

SIGSurface.prototype['getIsUsingWesternAspen'] = SIGSurface.prototype.getIsUsingWesternAspen = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGSurface_getIsUsingWesternAspen_0(self));
};;

SIGSurface.prototype['isAllFuelLoadZero'] = SIGSurface.prototype.isAllFuelLoadZero = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGSurface_isAllFuelLoadZero_1(self, fuelModelNumber));
};;

SIGSurface.prototype['isFuelDynamic'] = SIGSurface.prototype.isFuelDynamic = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGSurface_isFuelDynamic_1(self, fuelModelNumber));
};;

SIGSurface.prototype['isFuelModelDefined'] = SIGSurface.prototype.isFuelModelDefined = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGSurface_isFuelModelDefined_1(self, fuelModelNumber));
};;

SIGSurface.prototype['isFuelModelReserved'] = SIGSurface.prototype.isFuelModelReserved = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGSurface_isFuelModelReserved_1(self, fuelModelNumber));
};;

SIGSurface.prototype['isMoistureClassInputNeededForCurrentFuelModel'] = SIGSurface.prototype.isMoistureClassInputNeededForCurrentFuelModel = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureClass) {
  var self = this.ptr;
  if (moistureClass && typeof moistureClass === 'object') moistureClass = moistureClass.ptr;
  return !!(_emscripten_bind_SIGSurface_isMoistureClassInputNeededForCurrentFuelModel_1(self, moistureClass));
};;

SIGSurface.prototype['isUsingTwoFuelModels'] = SIGSurface.prototype.isUsingTwoFuelModels = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGSurface_isUsingTwoFuelModels_0(self));
};;

SIGSurface.prototype['setMoistureScenarioByIndex'] = SIGSurface.prototype.setMoistureScenarioByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarioIndex) {
  var self = this.ptr;
  if (moistureScenarioIndex && typeof moistureScenarioIndex === 'object') moistureScenarioIndex = moistureScenarioIndex.ptr;
  return !!(_emscripten_bind_SIGSurface_setMoistureScenarioByIndex_1(self, moistureScenarioIndex));
};;

SIGSurface.prototype['setMoistureScenarioByName'] = SIGSurface.prototype.setMoistureScenarioByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarioName) {
  var self = this.ptr;
  ensureCache.prepare();
  if (moistureScenarioName && typeof moistureScenarioName === 'object') moistureScenarioName = moistureScenarioName.ptr;
  else moistureScenarioName = ensureString(moistureScenarioName);
  return !!(_emscripten_bind_SIGSurface_setMoistureScenarioByName_1(self, moistureScenarioName));
};;

SIGSurface.prototype['calculateFlameLength'] = SIGSurface.prototype.calculateFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensity, firelineIntensityUnits, flameLengthUnits) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_calculateFlameLength_3(self, firelineIntensity, firelineIntensityUnits, flameLengthUnits);
};;

SIGSurface.prototype['calculateScorchHeight'] = SIGSurface.prototype.calculateScorchHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensity, firelineIntensityUnits, midFlameWindSpeed, windSpeedUnits, airTemperature, temperatureUnits, scorchHeightUnits) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  if (midFlameWindSpeed && typeof midFlameWindSpeed === 'object') midFlameWindSpeed = midFlameWindSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  return _emscripten_bind_SIGSurface_calculateScorchHeight_7(self, firelineIntensity, firelineIntensityUnits, midFlameWindSpeed, windSpeedUnits, airTemperature, temperatureUnits, scorchHeightUnits);
};;

SIGSurface.prototype['getAgeOfRough'] = SIGSurface.prototype.getAgeOfRough = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getAgeOfRough_0(self);
};;

SIGSurface.prototype['getAspect'] = SIGSurface.prototype.getAspect = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getAspect_0(self);
};;

SIGSurface.prototype['getAspenCuringLevel'] = SIGSurface.prototype.getAspenCuringLevel = /** @suppress {undefinedVars, duplicate} @this{Object} */function(curingLevelUnits) {
  var self = this.ptr;
  if (curingLevelUnits && typeof curingLevelUnits === 'object') curingLevelUnits = curingLevelUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenCuringLevel_1(self, curingLevelUnits);
};;

SIGSurface.prototype['getAspenDBH'] = SIGSurface.prototype.getAspenDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(dbhUnits) {
  var self = this.ptr;
  if (dbhUnits && typeof dbhUnits === 'object') dbhUnits = dbhUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenDBH_1(self, dbhUnits);
};;

SIGSurface.prototype['getAspenLoadDeadOneHour'] = SIGSurface.prototype.getAspenLoadDeadOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenLoadDeadOneHour_1(self, loadingUnits);
};;

SIGSurface.prototype['getAspenLoadDeadTenHour'] = SIGSurface.prototype.getAspenLoadDeadTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenLoadDeadTenHour_1(self, loadingUnits);
};;

SIGSurface.prototype['getAspenLoadLiveHerbaceous'] = SIGSurface.prototype.getAspenLoadLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenLoadLiveHerbaceous_1(self, loadingUnits);
};;

SIGSurface.prototype['getAspenLoadLiveWoody'] = SIGSurface.prototype.getAspenLoadLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenLoadLiveWoody_1(self, loadingUnits);
};;

SIGSurface.prototype['getAspenSavrDeadOneHour'] = SIGSurface.prototype.getAspenSavrDeadOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenSavrDeadOneHour_1(self, savrUnits);
};;

SIGSurface.prototype['getAspenSavrDeadTenHour'] = SIGSurface.prototype.getAspenSavrDeadTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenSavrDeadTenHour_1(self, savrUnits);
};;

SIGSurface.prototype['getAspenSavrLiveHerbaceous'] = SIGSurface.prototype.getAspenSavrLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenSavrLiveHerbaceous_1(self, savrUnits);
};;

SIGSurface.prototype['getAspenSavrLiveWoody'] = SIGSurface.prototype.getAspenSavrLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenSavrLiveWoody_1(self, savrUnits);
};;

SIGSurface.prototype['getBackingSpreadDistance'] = SIGSurface.prototype.getBackingSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getBackingSpreadDistance_3(self, lengthUnits, elapsedTime, timeUnits);
};;

SIGSurface.prototype['getBackingSpreadRate'] = SIGSurface.prototype.getBackingSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getBackingSpreadRate_1(self, spreadRateUnits);
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

SIGSurface.prototype['getChaparralAge'] = SIGSurface.prototype.getChaparralAge = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageUnits) {
  var self = this.ptr;
  if (ageUnits && typeof ageUnits === 'object') ageUnits = ageUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralAge_1(self, ageUnits);
};;

SIGSurface.prototype['getChaparralDaysSinceMayFirst'] = SIGSurface.prototype.getChaparralDaysSinceMayFirst = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getChaparralDaysSinceMayFirst_0(self);
};;

SIGSurface.prototype['getChaparralDeadFuelFraction'] = SIGSurface.prototype.getChaparralDeadFuelFraction = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getChaparralDeadFuelFraction_0(self);
};;

SIGSurface.prototype['getChaparralDeadMoistureOfExtinction'] = SIGSurface.prototype.getChaparralDeadMoistureOfExtinction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralDeadMoistureOfExtinction_1(self, moistureUnits);
};;

SIGSurface.prototype['getChaparralDensity'] = SIGSurface.prototype.getChaparralDensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, sizeClass, densityUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  if (densityUnits && typeof densityUnits === 'object') densityUnits = densityUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralDensity_3(self, lifeState, sizeClass, densityUnits);
};;

SIGSurface.prototype['getChaparralEffectiveSilicaContent'] = SIGSurface.prototype.getChaparralEffectiveSilicaContent = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, sizeClass) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  return _emscripten_bind_SIGSurface_getChaparralEffectiveSilicaContent_2(self, lifeState, sizeClass);
};;

SIGSurface.prototype['getChaparralFuelBedDepth'] = SIGSurface.prototype.getChaparralFuelBedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(depthUnits) {
  var self = this.ptr;
  if (depthUnits && typeof depthUnits === 'object') depthUnits = depthUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralFuelBedDepth_1(self, depthUnits);
};;

SIGSurface.prototype['getChaparralFuelDeadLoadFraction'] = SIGSurface.prototype.getChaparralFuelDeadLoadFraction = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getChaparralFuelDeadLoadFraction_0(self);
};;

SIGSurface.prototype['getChaparralHeatOfCombustion'] = SIGSurface.prototype.getChaparralHeatOfCombustion = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, sizeClass, heatOfCombustionUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralHeatOfCombustion_3(self, lifeState, sizeClass, heatOfCombustionUnits);
};;

SIGSurface.prototype['getChaparralLiveMoistureOfExtinction'] = SIGSurface.prototype.getChaparralLiveMoistureOfExtinction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLiveMoistureOfExtinction_1(self, moistureUnits);
};;

SIGSurface.prototype['getChaparralLoad'] = SIGSurface.prototype.getChaparralLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, sizeClass, loadingUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoad_3(self, lifeState, sizeClass, loadingUnits);
};;

SIGSurface.prototype['getChaparralMoisture'] = SIGSurface.prototype.getChaparralMoisture = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, sizeClass, moistureUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralMoisture_3(self, lifeState, sizeClass, moistureUnits);
};;

SIGSurface.prototype['getChaparralSavr'] = SIGSurface.prototype.getChaparralSavr = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, sizeClass, savrUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralSavr_3(self, lifeState, sizeClass, savrUnits);
};;

SIGSurface.prototype['getChaparralTotalDeadFuelLoad'] = SIGSurface.prototype.getChaparralTotalDeadFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralTotalDeadFuelLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralTotalFuelLoad'] = SIGSurface.prototype.getChaparralTotalFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralTotalFuelLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralTotalLiveFuelLoad'] = SIGSurface.prototype.getChaparralTotalLiveFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralTotalLiveFuelLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralTotalSilicaContent'] = SIGSurface.prototype.getChaparralTotalSilicaContent = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, sizeClass) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  return _emscripten_bind_SIGSurface_getChaparralTotalSilicaContent_2(self, lifeState, sizeClass);
};;

SIGSurface.prototype['getCharacteristicMoistureByLifeState'] = SIGSurface.prototype.getCharacteristicMoistureByLifeState = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, moistureUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicMoistureByLifeState_2(self, lifeState, moistureUnits);
};;

SIGSurface.prototype['getCharacteristicSAVR'] = SIGSurface.prototype.getCharacteristicSAVR = /** @suppress {undefinedVars, duplicate} @this{Object} */function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicSAVR_1(self, savrUnits);
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

SIGSurface.prototype['getFlankingSpreadDistance'] = SIGSurface.prototype.getFlankingSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingSpreadDistance_3(self, lengthUnits, elapsedTime, timeUnits);
};;

SIGSurface.prototype['getFlankingSpreadRate'] = SIGSurface.prototype.getFlankingSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingSpreadRate_1(self, spreadRateUnits);
};;

SIGSurface.prototype['getFuelHeatOfCombustionDead'] = SIGSurface.prototype.getFuelHeatOfCombustionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelHeatOfCombustionDead_2(self, fuelModelNumber, heatOfCombustionUnits);
};;

SIGSurface.prototype['getFuelHeatOfCombustionLive'] = SIGSurface.prototype.getFuelHeatOfCombustionLive = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelHeatOfCombustionLive_2(self, fuelModelNumber, heatOfCombustionUnits);
};;

SIGSurface.prototype['getFuelLoadHundredHour'] = SIGSurface.prototype.getFuelLoadHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadHundredHour_2(self, fuelModelNumber, loadingUnits);
};;

SIGSurface.prototype['getFuelLoadLiveHerbaceous'] = SIGSurface.prototype.getFuelLoadLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadLiveHerbaceous_2(self, fuelModelNumber, loadingUnits);
};;

SIGSurface.prototype['getFuelLoadLiveWoody'] = SIGSurface.prototype.getFuelLoadLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadLiveWoody_2(self, fuelModelNumber, loadingUnits);
};;

SIGSurface.prototype['getFuelLoadOneHour'] = SIGSurface.prototype.getFuelLoadOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadOneHour_2(self, fuelModelNumber, loadingUnits);
};;

SIGSurface.prototype['getFuelLoadTenHour'] = SIGSurface.prototype.getFuelLoadTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadTenHour_2(self, fuelModelNumber, loadingUnits);
};;

SIGSurface.prototype['getFuelMoistureOfExtinctionDead'] = SIGSurface.prototype.getFuelMoistureOfExtinctionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, moistureUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelMoistureOfExtinctionDead_2(self, fuelModelNumber, moistureUnits);
};;

SIGSurface.prototype['getFuelSavrLiveHerbaceous'] = SIGSurface.prototype.getFuelSavrLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelSavrLiveHerbaceous_2(self, fuelModelNumber, savrUnits);
};;

SIGSurface.prototype['getFuelSavrLiveWoody'] = SIGSurface.prototype.getFuelSavrLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelSavrLiveWoody_2(self, fuelModelNumber, savrUnits);
};;

SIGSurface.prototype['getFuelSavrOneHour'] = SIGSurface.prototype.getFuelSavrOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelSavrOneHour_2(self, fuelModelNumber, savrUnits);
};;

SIGSurface.prototype['getFuelbedDepth'] = SIGSurface.prototype.getFuelbedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, lengthUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelbedDepth_2(self, fuelModelNumber, lengthUnits);
};;

SIGSurface.prototype['getHeadingToBackingRatio'] = SIGSurface.prototype.getHeadingToBackingRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getHeadingToBackingRatio_0(self);
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

SIGSurface.prototype['getHeatSource'] = SIGSurface.prototype.getHeatSource = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heatSourceUnits) {
  var self = this.ptr;
  if (heatSourceUnits && typeof heatSourceUnits === 'object') heatSourceUnits = heatSourceUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeatSource_1(self, heatSourceUnits);
};;

SIGSurface.prototype['getHeightOfUnderstory'] = SIGSurface.prototype.getHeightOfUnderstory = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heightUnits) {
  var self = this.ptr;
  if (heightUnits && typeof heightUnits === 'object') heightUnits = heightUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeightOfUnderstory_1(self, heightUnits);
};;

SIGSurface.prototype['getLiveFuelMoistureOfExtinction'] = SIGSurface.prototype.getLiveFuelMoistureOfExtinction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getLiveFuelMoistureOfExtinction_1(self, moistureUnits);
};;

SIGSurface.prototype['getMidflameWindspeed'] = SIGSurface.prototype.getMidflameWindspeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  return _emscripten_bind_SIGSurface_getMidflameWindspeed_1(self, windSpeedUnits);
};;

SIGSurface.prototype['getMoistureDeadAggregateValue'] = SIGSurface.prototype.getMoistureDeadAggregateValue = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureDeadAggregateValue_1(self, moistureUnits);
};;

SIGSurface.prototype['getMoistureHundredHour'] = SIGSurface.prototype.getMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureHundredHour_1(self, moistureUnits);
};;

SIGSurface.prototype['getMoistureLiveAggregateValue'] = SIGSurface.prototype.getMoistureLiveAggregateValue = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureLiveAggregateValue_1(self, moistureUnits);
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

SIGSurface.prototype['getMoistureScenarioHundredHourByIndex'] = SIGSurface.prototype.getMoistureScenarioHundredHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByIndex_1(self, index);
};;

SIGSurface.prototype['getMoistureScenarioHundredHourByName'] = SIGSurface.prototype.getMoistureScenarioHundredHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByName_1(self, name);
};;

SIGSurface.prototype['getMoistureScenarioLiveHerbaceousByIndex'] = SIGSurface.prototype.getMoistureScenarioLiveHerbaceousByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByIndex_1(self, index);
};;

SIGSurface.prototype['getMoistureScenarioLiveHerbaceousByName'] = SIGSurface.prototype.getMoistureScenarioLiveHerbaceousByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByName_1(self, name);
};;

SIGSurface.prototype['getMoistureScenarioLiveWoodyByIndex'] = SIGSurface.prototype.getMoistureScenarioLiveWoodyByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByIndex_1(self, index);
};;

SIGSurface.prototype['getMoistureScenarioLiveWoodyByName'] = SIGSurface.prototype.getMoistureScenarioLiveWoodyByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByName_1(self, name);
};;

SIGSurface.prototype['getMoistureScenarioOneHourByIndex'] = SIGSurface.prototype.getMoistureScenarioOneHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioOneHourByIndex_1(self, index);
};;

SIGSurface.prototype['getMoistureScenarioOneHourByName'] = SIGSurface.prototype.getMoistureScenarioOneHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGSurface_getMoistureScenarioOneHourByName_1(self, name);
};;

SIGSurface.prototype['getMoistureScenarioTenHourByIndex'] = SIGSurface.prototype.getMoistureScenarioTenHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioTenHourByIndex_1(self, index);
};;

SIGSurface.prototype['getMoistureScenarioTenHourByName'] = SIGSurface.prototype.getMoistureScenarioTenHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGSurface_getMoistureScenarioTenHourByName_1(self, name);
};;

SIGSurface.prototype['getMoistureTenHour'] = SIGSurface.prototype.getMoistureTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureTenHour_1(self, moistureUnits);
};;

SIGSurface.prototype['getOverstoryBasalArea'] = SIGSurface.prototype.getOverstoryBasalArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(basalAreaUnits) {
  var self = this.ptr;
  if (basalAreaUnits && typeof basalAreaUnits === 'object') basalAreaUnits = basalAreaUnits.ptr;
  return _emscripten_bind_SIGSurface_getOverstoryBasalArea_1(self, basalAreaUnits);
};;

SIGSurface.prototype['getPalmettoGallberryCoverage'] = SIGSurface.prototype.getPalmettoGallberryCoverage = /** @suppress {undefinedVars, duplicate} @this{Object} */function(coverUnits) {
  var self = this.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberryCoverage_1(self, coverUnits);
};;

SIGSurface.prototype['getPalmettoGallberryHeatOfCombustionDead'] = SIGSurface.prototype.getPalmettoGallberryHeatOfCombustionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heatOfCombustionUnits) {
  var self = this.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionDead_1(self, heatOfCombustionUnits);
};;

SIGSurface.prototype['getPalmettoGallberryHeatOfCombustionLive'] = SIGSurface.prototype.getPalmettoGallberryHeatOfCombustionLive = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heatOfCombustionUnits) {
  var self = this.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionLive_1(self, heatOfCombustionUnits);
};;

SIGSurface.prototype['getPalmettoGallberryMoistureOfExtinctionDead'] = SIGSurface.prototype.getPalmettoGallberryMoistureOfExtinctionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberryMoistureOfExtinctionDead_1(self, moistureUnits);
};;

SIGSurface.prototype['getPalmettoGallberyDeadFoliageLoad'] = SIGSurface.prototype.getPalmettoGallberyDeadFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyDeadFoliageLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyDeadOneHourLoad'] = SIGSurface.prototype.getPalmettoGallberyDeadOneHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyDeadOneHourLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyDeadTenHourLoad'] = SIGSurface.prototype.getPalmettoGallberyDeadTenHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyDeadTenHourLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyFuelBedDepth'] = SIGSurface.prototype.getPalmettoGallberyFuelBedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(depthUnits) {
  var self = this.ptr;
  if (depthUnits && typeof depthUnits === 'object') depthUnits = depthUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyFuelBedDepth_1(self, depthUnits);
};;

SIGSurface.prototype['getPalmettoGallberyLitterLoad'] = SIGSurface.prototype.getPalmettoGallberyLitterLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLitterLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyLiveFoliageLoad'] = SIGSurface.prototype.getPalmettoGallberyLiveFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLiveFoliageLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyLiveOneHourLoad'] = SIGSurface.prototype.getPalmettoGallberyLiveOneHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLiveOneHourLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyLiveTenHourLoad'] = SIGSurface.prototype.getPalmettoGallberyLiveTenHourLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLiveTenHourLoad_1(self, loadingUnits);
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

SIGSurface.prototype['getSpreadDistance'] = SIGSurface.prototype.getSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadDistance_3(self, lengthUnits, elapsedTime, timeUnits);
};;

SIGSurface.prototype['getSpreadDistanceInDirectionOfInterest'] = SIGSurface.prototype.getSpreadDistanceInDirectionOfInterest = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadDistanceInDirectionOfInterest_3(self, lengthUnits, elapsedTime, timeUnits);
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

SIGSurface.prototype['getSurfaceFireReactionIntensityForLifeState'] = SIGSurface.prototype.getSurfaceFireReactionIntensityForLifeState = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  return _emscripten_bind_SIGSurface_getSurfaceFireReactionIntensityForLifeState_1(self, lifeState);
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

SIGSurface.prototype['getAspenFuelModelNumber'] = SIGSurface.prototype.getAspenFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getAspenFuelModelNumber_0(self);
};;

SIGSurface.prototype['getFuelModelNumber'] = SIGSurface.prototype.getFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getFuelModelNumber_0(self);
};;

SIGSurface.prototype['getMoistureScenarioIndexByName'] = SIGSurface.prototype.getMoistureScenarioIndexByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGSurface_getMoistureScenarioIndexByName_1(self, name);
};;

SIGSurface.prototype['getNumberOfMoistureScenarios'] = SIGSurface.prototype.getNumberOfMoistureScenarios = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getNumberOfMoistureScenarios_0(self);
};;

SIGSurface.prototype['getFuelCode'] = SIGSurface.prototype.getFuelCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGSurface_getFuelCode_1(self, fuelModelNumber));
};;

SIGSurface.prototype['getFuelName'] = SIGSurface.prototype.getFuelName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGSurface_getFuelName_1(self, fuelModelNumber));
};;

SIGSurface.prototype['getMoistureScenarioDescriptionByIndex'] = SIGSurface.prototype.getMoistureScenarioDescriptionByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByIndex_1(self, index));
};;

SIGSurface.prototype['getMoistureScenarioDescriptionByName'] = SIGSurface.prototype.getMoistureScenarioDescriptionByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return UTF8ToString(_emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByName_1(self, name));
};;

SIGSurface.prototype['getMoistureScenarioNameByIndex'] = SIGSurface.prototype.getMoistureScenarioNameByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGSurface_getMoistureScenarioNameByIndex_1(self, index));
};;

SIGSurface.prototype['doSurfaceRunInDirectionOfInterest'] = SIGSurface.prototype.doSurfaceRunInDirectionOfInterest = /** @suppress {undefinedVars, duplicate} @this{Object} */function(directionOfInterest, directionMode) {
  var self = this.ptr;
  if (directionOfInterest && typeof directionOfInterest === 'object') directionOfInterest = directionOfInterest.ptr;
  if (directionMode && typeof directionMode === 'object') directionMode = directionMode.ptr;
  _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_2(self, directionOfInterest, directionMode);
};;

SIGSurface.prototype['doSurfaceRunInDirectionOfMaxSpread'] = SIGSurface.prototype.doSurfaceRunInDirectionOfMaxSpread = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0(self);
};;

SIGSurface.prototype['initializeMembers'] = SIGSurface.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface_initializeMembers_0(self);
};;

SIGSurface.prototype['setAgeOfRough'] = SIGSurface.prototype.setAgeOfRough = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  _emscripten_bind_SIGSurface_setAgeOfRough_1(self, ageOfRough);
};;

SIGSurface.prototype['setAspect'] = SIGSurface.prototype.setAspect = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspect) {
  var self = this.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  _emscripten_bind_SIGSurface_setAspect_1(self, aspect);
};;

SIGSurface.prototype['setAspenCuringLevel'] = SIGSurface.prototype.setAspenCuringLevel = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenCuringLevel, curingLevelUnits) {
  var self = this.ptr;
  if (aspenCuringLevel && typeof aspenCuringLevel === 'object') aspenCuringLevel = aspenCuringLevel.ptr;
  if (curingLevelUnits && typeof curingLevelUnits === 'object') curingLevelUnits = curingLevelUnits.ptr;
  _emscripten_bind_SIGSurface_setAspenCuringLevel_2(self, aspenCuringLevel, curingLevelUnits);
};;

SIGSurface.prototype['setAspenDBH'] = SIGSurface.prototype.setAspenDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(dbh, dbhUnits) {
  var self = this.ptr;
  if (dbh && typeof dbh === 'object') dbh = dbh.ptr;
  if (dbhUnits && typeof dbhUnits === 'object') dbhUnits = dbhUnits.ptr;
  _emscripten_bind_SIGSurface_setAspenDBH_2(self, dbh, dbhUnits);
};;

SIGSurface.prototype['setAspenFireSeverity'] = SIGSurface.prototype.setAspenFireSeverity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFireSeverity) {
  var self = this.ptr;
  if (aspenFireSeverity && typeof aspenFireSeverity === 'object') aspenFireSeverity = aspenFireSeverity.ptr;
  _emscripten_bind_SIGSurface_setAspenFireSeverity_1(self, aspenFireSeverity);
};;

SIGSurface.prototype['setAspenFuelModelNumber'] = SIGSurface.prototype.setAspenFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  _emscripten_bind_SIGSurface_setAspenFuelModelNumber_1(self, aspenFuelModelNumber);
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

SIGSurface.prototype['setChaparralFuelBedDepth'] = SIGSurface.prototype.setChaparralFuelBedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(chaparralFuelBedDepth, depthUnts) {
  var self = this.ptr;
  if (chaparralFuelBedDepth && typeof chaparralFuelBedDepth === 'object') chaparralFuelBedDepth = chaparralFuelBedDepth.ptr;
  if (depthUnts && typeof depthUnts === 'object') depthUnts = depthUnts.ptr;
  _emscripten_bind_SIGSurface_setChaparralFuelBedDepth_2(self, chaparralFuelBedDepth, depthUnts);
};;

SIGSurface.prototype['setChaparralFuelDeadLoadFraction'] = SIGSurface.prototype.setChaparralFuelDeadLoadFraction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(chaparralFuelDeadLoadFraction) {
  var self = this.ptr;
  if (chaparralFuelDeadLoadFraction && typeof chaparralFuelDeadLoadFraction === 'object') chaparralFuelDeadLoadFraction = chaparralFuelDeadLoadFraction.ptr;
  _emscripten_bind_SIGSurface_setChaparralFuelDeadLoadFraction_1(self, chaparralFuelDeadLoadFraction);
};;

SIGSurface.prototype['setChaparralFuelLoadInputMode'] = SIGSurface.prototype.setChaparralFuelLoadInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelLoadInputMode) {
  var self = this.ptr;
  if (fuelLoadInputMode && typeof fuelLoadInputMode === 'object') fuelLoadInputMode = fuelLoadInputMode.ptr;
  _emscripten_bind_SIGSurface_setChaparralFuelLoadInputMode_1(self, fuelLoadInputMode);
};;

SIGSurface.prototype['setChaparralFuelType'] = SIGSurface.prototype.setChaparralFuelType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(chaparralFuelType) {
  var self = this.ptr;
  if (chaparralFuelType && typeof chaparralFuelType === 'object') chaparralFuelType = chaparralFuelType.ptr;
  _emscripten_bind_SIGSurface_setChaparralFuelType_1(self, chaparralFuelType);
};;

SIGSurface.prototype['setChaparralTotalFuelLoad'] = SIGSurface.prototype.setChaparralTotalFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(chaparralTotalFuelLoad, fuelLoadUnits) {
  var self = this.ptr;
  if (chaparralTotalFuelLoad && typeof chaparralTotalFuelLoad === 'object') chaparralTotalFuelLoad = chaparralTotalFuelLoad.ptr;
  if (fuelLoadUnits && typeof fuelLoadUnits === 'object') fuelLoadUnits = fuelLoadUnits.ptr;
  _emscripten_bind_SIGSurface_setChaparralTotalFuelLoad_2(self, chaparralTotalFuelLoad, fuelLoadUnits);
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

SIGSurface.prototype['setFuelModels'] = SIGSurface.prototype.setFuelModels = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModels) {
  var self = this.ptr;
  if (fuelModels && typeof fuelModels === 'object') fuelModels = fuelModels.ptr;
  _emscripten_bind_SIGSurface_setFuelModels_1(self, fuelModels);
};;

SIGSurface.prototype['setHeightOfUnderstory'] = SIGSurface.prototype.setHeightOfUnderstory = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heightOfUnderstory, heightUnits) {
  var self = this.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  if (heightUnits && typeof heightUnits === 'object') heightUnits = heightUnits.ptr;
  _emscripten_bind_SIGSurface_setHeightOfUnderstory_2(self, heightOfUnderstory, heightUnits);
};;

SIGSurface.prototype['setIsUsingChaparral'] = SIGSurface.prototype.setIsUsingChaparral = /** @suppress {undefinedVars, duplicate} @this{Object} */function(isUsingChaparral) {
  var self = this.ptr;
  if (isUsingChaparral && typeof isUsingChaparral === 'object') isUsingChaparral = isUsingChaparral.ptr;
  _emscripten_bind_SIGSurface_setIsUsingChaparral_1(self, isUsingChaparral);
};;

SIGSurface.prototype['setIsUsingPalmettoGallberry'] = SIGSurface.prototype.setIsUsingPalmettoGallberry = /** @suppress {undefinedVars, duplicate} @this{Object} */function(isUsingPalmettoGallberry) {
  var self = this.ptr;
  if (isUsingPalmettoGallberry && typeof isUsingPalmettoGallberry === 'object') isUsingPalmettoGallberry = isUsingPalmettoGallberry.ptr;
  _emscripten_bind_SIGSurface_setIsUsingPalmettoGallberry_1(self, isUsingPalmettoGallberry);
};;

SIGSurface.prototype['setIsUsingWesternAspen'] = SIGSurface.prototype.setIsUsingWesternAspen = /** @suppress {undefinedVars, duplicate} @this{Object} */function(isUsingWesternAspen) {
  var self = this.ptr;
  if (isUsingWesternAspen && typeof isUsingWesternAspen === 'object') isUsingWesternAspen = isUsingWesternAspen.ptr;
  _emscripten_bind_SIGSurface_setIsUsingWesternAspen_1(self, isUsingWesternAspen);
};;

SIGSurface.prototype['setMoistureDeadAggregate'] = SIGSurface.prototype.setMoistureDeadAggregate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureDead, moistureUnits) {
  var self = this.ptr;
  if (moistureDead && typeof moistureDead === 'object') moistureDead = moistureDead.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureDeadAggregate_2(self, moistureDead, moistureUnits);
};;

SIGSurface.prototype['setMoistureHundredHour'] = SIGSurface.prototype.setMoistureHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureHundredHour, moistureUnits) {
  var self = this.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureHundredHour_2(self, moistureHundredHour, moistureUnits);
};;

SIGSurface.prototype['setMoistureInputMode'] = SIGSurface.prototype.setMoistureInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureInputMode) {
  var self = this.ptr;
  if (moistureInputMode && typeof moistureInputMode === 'object') moistureInputMode = moistureInputMode.ptr;
  _emscripten_bind_SIGSurface_setMoistureInputMode_1(self, moistureInputMode);
};;

SIGSurface.prototype['setMoistureLiveAggregate'] = SIGSurface.prototype.setMoistureLiveAggregate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureLive, moistureUnits) {
  var self = this.ptr;
  if (moistureLive && typeof moistureLive === 'object') moistureLive = moistureLive.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureLiveAggregate_2(self, moistureLive, moistureUnits);
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

SIGSurface.prototype['setMoistureScenarios'] = SIGSurface.prototype.setMoistureScenarios = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarios) {
  var self = this.ptr;
  if (moistureScenarios && typeof moistureScenarios === 'object') moistureScenarios = moistureScenarios.ptr;
  _emscripten_bind_SIGSurface_setMoistureScenarios_1(self, moistureScenarios);
};;

SIGSurface.prototype['setMoistureTenHour'] = SIGSurface.prototype.setMoistureTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureTenHour, moistureUnits) {
  var self = this.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureTenHour_2(self, moistureTenHour, moistureUnits);
};;

SIGSurface.prototype['setOverstoryBasalArea'] = SIGSurface.prototype.setOverstoryBasalArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(overstoryBasalArea, basalAreaUnits) {
  var self = this.ptr;
  if (overstoryBasalArea && typeof overstoryBasalArea === 'object') overstoryBasalArea = overstoryBasalArea.ptr;
  if (basalAreaUnits && typeof basalAreaUnits === 'object') basalAreaUnits = basalAreaUnits.ptr;
  _emscripten_bind_SIGSurface_setOverstoryBasalArea_2(self, overstoryBasalArea, basalAreaUnits);
};;

SIGSurface.prototype['setPalmettoCoverage'] = SIGSurface.prototype.setPalmettoCoverage = /** @suppress {undefinedVars, duplicate} @this{Object} */function(palmettoCoverage, coverUnits) {
  var self = this.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  _emscripten_bind_SIGSurface_setPalmettoCoverage_2(self, palmettoCoverage, coverUnits);
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

SIGSurface.prototype['updateSurfaceInputsForPalmettoGallbery'] = SIGSurface.prototype.updateSurfaceInputsForPalmettoGallbery = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, basalAreaUnits, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio) {
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
  if (basalAreaUnits && typeof basalAreaUnits === 'object') basalAreaUnits = basalAreaUnits.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  _emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_24(self, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, basalAreaUnits, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
};;

SIGSurface.prototype['updateSurfaceInputsForTwoFuelModels'] = SIGSurface.prototype.updateSurfaceInputsForTwoFuelModels = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firstFuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope, slopeUnits, aspect, canopyCover, canopyCoverUnits, canopyHeight, canopyHeightUnits, crownRatio) {
  var self = this.ptr;
  if (firstFuelModelNumber && typeof firstFuelModelNumber === 'object') firstFuelModelNumber = firstFuelModelNumber.ptr;
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
  _emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_24(self, firstFuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope, slopeUnits, aspect, canopyCover, canopyCoverUnits, canopyHeight, canopyHeightUnits, crownRatio);
};;

SIGSurface.prototype['updateSurfaceInputsForWesternAspen'] = SIGSurface.prototype.updateSurfaceInputsForWesternAspen = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber, aspenCuringLevel, curingLevelUnits, aspenFireSeverity, dbh, dbhUnits, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  if (aspenCuringLevel && typeof aspenCuringLevel === 'object') aspenCuringLevel = aspenCuringLevel.ptr;
  if (curingLevelUnits && typeof curingLevelUnits === 'object') curingLevelUnits = curingLevelUnits.ptr;
  if (aspenFireSeverity && typeof aspenFireSeverity === 'object') aspenFireSeverity = aspenFireSeverity.ptr;
  if (dbh && typeof dbh === 'object') dbh = dbh.ptr;
  if (dbhUnits && typeof dbhUnits === 'object') dbhUnits = dbhUnits.ptr;
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
  _emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_25(self, aspenFuelModelNumber, aspenCuringLevel, curingLevelUnits, aspenFireSeverity, dbh, dbhUnits, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
};;

SIGSurface.prototype['setFuelModelNumber'] = SIGSurface.prototype.setFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  _emscripten_bind_SIGSurface_setFuelModelNumber_1(self, fuelModelNumber);
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

WesternAspen.prototype['getAspenLoadDeadOneHour'] = WesternAspen.prototype.getAspenLoadDeadOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_0(self);
};;

WesternAspen.prototype['getAspenLoadDeadTenHour'] = WesternAspen.prototype.getAspenLoadDeadTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_0(self);
};;

WesternAspen.prototype['getAspenLoadLiveHerbaceous'] = WesternAspen.prototype.getAspenLoadLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_0(self);
};;

WesternAspen.prototype['getAspenLoadLiveWoody'] = WesternAspen.prototype.getAspenLoadLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadLiveWoody_0(self);
};;

WesternAspen.prototype['getAspenMoistureOfExtinctionDead'] = WesternAspen.prototype.getAspenMoistureOfExtinctionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0(self);
};;

WesternAspen.prototype['getAspenMortality'] = WesternAspen.prototype.getAspenMortality = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenMortality_0(self);
};;

WesternAspen.prototype['getAspenSavrDeadOneHour'] = WesternAspen.prototype.getAspenSavrDeadOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_0(self);
};;

WesternAspen.prototype['getAspenSavrDeadTenHour'] = WesternAspen.prototype.getAspenSavrDeadTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0(self);
};;

WesternAspen.prototype['getAspenSavrLiveHerbaceous'] = WesternAspen.prototype.getAspenSavrLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0(self);
};;

WesternAspen.prototype['getAspenSavrLiveWoody'] = WesternAspen.prototype.getAspenSavrLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrLiveWoody_0(self);
};;

  WesternAspen.prototype['__destroy__'] = WesternAspen.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_WesternAspen___destroy___0(self);
};
// SIGCrown
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGCrown(fuelModels) {
  if (fuelModels && typeof fuelModels === 'object') fuelModels = fuelModels.ptr;
  this.ptr = _emscripten_bind_SIGCrown_SIGCrown_1(fuelModels);
  getCache(SIGCrown)[this.ptr] = this;
};;
SIGCrown.prototype = Object.create(WrapperObject.prototype);
SIGCrown.prototype.constructor = SIGCrown;
SIGCrown.prototype.__class__ = SIGCrown;
SIGCrown.__cache__ = {};
Module['SIGCrown'] = SIGCrown;

SIGCrown.prototype['getFireType'] = SIGCrown.prototype.getFireType = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getFireType_0(self);
};;

SIGCrown.prototype['getIsMoistureScenarioDefinedByIndex'] = SIGCrown.prototype.getIsMoistureScenarioDefinedByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return !!(_emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByIndex_1(self, index));
};;

SIGCrown.prototype['getIsMoistureScenarioDefinedByName'] = SIGCrown.prototype.getIsMoistureScenarioDefinedByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return !!(_emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByName_1(self, name));
};;

SIGCrown.prototype['isAllFuelLoadZero'] = SIGCrown.prototype.isAllFuelLoadZero = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGCrown_isAllFuelLoadZero_1(self, fuelModelNumber));
};;

SIGCrown.prototype['isFuelDynamic'] = SIGCrown.prototype.isFuelDynamic = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGCrown_isFuelDynamic_1(self, fuelModelNumber));
};;

SIGCrown.prototype['isFuelModelDefined'] = SIGCrown.prototype.isFuelModelDefined = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGCrown_isFuelModelDefined_1(self, fuelModelNumber));
};;

SIGCrown.prototype['isFuelModelReserved'] = SIGCrown.prototype.isFuelModelReserved = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGCrown_isFuelModelReserved_1(self, fuelModelNumber));
};;

SIGCrown.prototype['setMoistureScenarioByIndex'] = SIGCrown.prototype.setMoistureScenarioByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarioIndex) {
  var self = this.ptr;
  if (moistureScenarioIndex && typeof moistureScenarioIndex === 'object') moistureScenarioIndex = moistureScenarioIndex.ptr;
  return !!(_emscripten_bind_SIGCrown_setMoistureScenarioByIndex_1(self, moistureScenarioIndex));
};;

SIGCrown.prototype['setMoistureScenarioByName'] = SIGCrown.prototype.setMoistureScenarioByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarioName) {
  var self = this.ptr;
  ensureCache.prepare();
  if (moistureScenarioName && typeof moistureScenarioName === 'object') moistureScenarioName = moistureScenarioName.ptr;
  else moistureScenarioName = ensureString(moistureScenarioName);
  return !!(_emscripten_bind_SIGCrown_setMoistureScenarioByName_1(self, moistureScenarioName));
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

SIGCrown.prototype['getCrownFireSpreadDistance'] = SIGCrown.prototype.getCrownFireSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireSpreadDistance_3(self, lengthUnits, elapsedTime, timeUnits);
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

SIGCrown.prototype['getCrownFractionBurned'] = SIGCrown.prototype.getCrownFractionBurned = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownFractionBurned_0(self);
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

SIGCrown.prototype['getFuelHeatOfCombustionDead'] = SIGCrown.prototype.getFuelHeatOfCombustionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelHeatOfCombustionDead_2(self, fuelModelNumber, heatOfCombustionUnits);
};;

SIGCrown.prototype['getFuelHeatOfCombustionLive'] = SIGCrown.prototype.getFuelHeatOfCombustionLive = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelHeatOfCombustionLive_2(self, fuelModelNumber, heatOfCombustionUnits);
};;

SIGCrown.prototype['getFuelLoadHundredHour'] = SIGCrown.prototype.getFuelLoadHundredHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadHundredHour_2(self, fuelModelNumber, loadingUnits);
};;

SIGCrown.prototype['getFuelLoadLiveHerbaceous'] = SIGCrown.prototype.getFuelLoadLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadLiveHerbaceous_2(self, fuelModelNumber, loadingUnits);
};;

SIGCrown.prototype['getFuelLoadLiveWoody'] = SIGCrown.prototype.getFuelLoadLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadLiveWoody_2(self, fuelModelNumber, loadingUnits);
};;

SIGCrown.prototype['getFuelLoadOneHour'] = SIGCrown.prototype.getFuelLoadOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadOneHour_2(self, fuelModelNumber, loadingUnits);
};;

SIGCrown.prototype['getFuelLoadTenHour'] = SIGCrown.prototype.getFuelLoadTenHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadTenHour_2(self, fuelModelNumber, loadingUnits);
};;

SIGCrown.prototype['getFuelMoistureOfExtinctionDead'] = SIGCrown.prototype.getFuelMoistureOfExtinctionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, moistureUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelMoistureOfExtinctionDead_2(self, fuelModelNumber, moistureUnits);
};;

SIGCrown.prototype['getFuelSavrLiveHerbaceous'] = SIGCrown.prototype.getFuelSavrLiveHerbaceous = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelSavrLiveHerbaceous_2(self, fuelModelNumber, savrUnits);
};;

SIGCrown.prototype['getFuelSavrLiveWoody'] = SIGCrown.prototype.getFuelSavrLiveWoody = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelSavrLiveWoody_2(self, fuelModelNumber, savrUnits);
};;

SIGCrown.prototype['getFuelSavrOneHour'] = SIGCrown.prototype.getFuelSavrOneHour = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelSavrOneHour_2(self, fuelModelNumber, savrUnits);
};;

SIGCrown.prototype['getFuelbedDepth'] = SIGCrown.prototype.getFuelbedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, lengthUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelbedDepth_2(self, fuelModelNumber, lengthUnits);
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

SIGCrown.prototype['getMoistureScenarioHundredHourByIndex'] = SIGCrown.prototype.getMoistureScenarioHundredHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByIndex_1(self, index);
};;

SIGCrown.prototype['getMoistureScenarioHundredHourByName'] = SIGCrown.prototype.getMoistureScenarioHundredHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByName_1(self, name);
};;

SIGCrown.prototype['getMoistureScenarioLiveHerbaceousByIndex'] = SIGCrown.prototype.getMoistureScenarioLiveHerbaceousByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByIndex_1(self, index);
};;

SIGCrown.prototype['getMoistureScenarioLiveHerbaceousByName'] = SIGCrown.prototype.getMoistureScenarioLiveHerbaceousByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByName_1(self, name);
};;

SIGCrown.prototype['getMoistureScenarioLiveWoodyByIndex'] = SIGCrown.prototype.getMoistureScenarioLiveWoodyByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByIndex_1(self, index);
};;

SIGCrown.prototype['getMoistureScenarioLiveWoodyByName'] = SIGCrown.prototype.getMoistureScenarioLiveWoodyByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByName_1(self, name);
};;

SIGCrown.prototype['getMoistureScenarioOneHourByIndex'] = SIGCrown.prototype.getMoistureScenarioOneHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioOneHourByIndex_1(self, index);
};;

SIGCrown.prototype['getMoistureScenarioOneHourByName'] = SIGCrown.prototype.getMoistureScenarioOneHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGCrown_getMoistureScenarioOneHourByName_1(self, name);
};;

SIGCrown.prototype['getMoistureScenarioTenHourByIndex'] = SIGCrown.prototype.getMoistureScenarioTenHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioTenHourByIndex_1(self, index);
};;

SIGCrown.prototype['getMoistureScenarioTenHourByName'] = SIGCrown.prototype.getMoistureScenarioTenHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGCrown_getMoistureScenarioTenHourByName_1(self, name);
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

SIGCrown.prototype['getSurfaceFireSpreadDistance'] = SIGCrown.prototype.getSurfaceFireSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGCrown_getSurfaceFireSpreadDistance_3(self, lengthUnits, elapsedTime, timeUnits);
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

SIGCrown.prototype['getMoistureScenarioIndexByName'] = SIGCrown.prototype.getMoistureScenarioIndexByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGCrown_getMoistureScenarioIndexByName_1(self, name);
};;

SIGCrown.prototype['getNumberOfMoistureScenarios'] = SIGCrown.prototype.getNumberOfMoistureScenarios = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getNumberOfMoistureScenarios_0(self);
};;

SIGCrown.prototype['getFuelCode'] = SIGCrown.prototype.getFuelCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGCrown_getFuelCode_1(self, fuelModelNumber));
};;

SIGCrown.prototype['getFuelName'] = SIGCrown.prototype.getFuelName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGCrown_getFuelName_1(self, fuelModelNumber));
};;

SIGCrown.prototype['getMoistureScenarioDescriptionByIndex'] = SIGCrown.prototype.getMoistureScenarioDescriptionByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByIndex_1(self, index));
};;

SIGCrown.prototype['getMoistureScenarioDescriptionByName'] = SIGCrown.prototype.getMoistureScenarioDescriptionByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return UTF8ToString(_emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByName_1(self, name));
};;

SIGCrown.prototype['getMoistureScenarioNameByIndex'] = SIGCrown.prototype.getMoistureScenarioNameByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGCrown_getMoistureScenarioNameByIndex_1(self, index));
};;

SIGCrown.prototype['doCrownRunRothermel'] = SIGCrown.prototype.doCrownRunRothermel = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_doCrownRunRothermel_0(self);
};;

SIGCrown.prototype['doCrownRunScottAndReinhardt'] = SIGCrown.prototype.doCrownRunScottAndReinhardt = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0(self);
};;

SIGCrown.prototype['initializeMembers'] = SIGCrown.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_initializeMembers_0(self);
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

SIGCrown.prototype['setMoistureDeadAggregate'] = SIGCrown.prototype.setMoistureDeadAggregate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureDead, moistureUnits) {
  var self = this.ptr;
  if (moistureDead && typeof moistureDead === 'object') moistureDead = moistureDead.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureDeadAggregate_2(self, moistureDead, moistureUnits);
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

SIGCrown.prototype['setMoistureInputMode'] = SIGCrown.prototype.setMoistureInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureInputMode) {
  var self = this.ptr;
  if (moistureInputMode && typeof moistureInputMode === 'object') moistureInputMode = moistureInputMode.ptr;
  _emscripten_bind_SIGCrown_setMoistureInputMode_1(self, moistureInputMode);
};;

SIGCrown.prototype['setMoistureLiveAggregate'] = SIGCrown.prototype.setMoistureLiveAggregate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureLive, moistureUnits) {
  var self = this.ptr;
  if (moistureLive && typeof moistureLive === 'object') moistureLive = moistureLive.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureLiveAggregate_2(self, moistureLive, moistureUnits);
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

SIGCrown.prototype['setMoistureScenarios'] = SIGCrown.prototype.setMoistureScenarios = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarios) {
  var self = this.ptr;
  if (moistureScenarios && typeof moistureScenarios === 'object') moistureScenarios = moistureScenarios.ptr;
  _emscripten_bind_SIGCrown_setMoistureScenarios_1(self, moistureScenarios);
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

SIGCrown.prototype['getFinalFlameLength'] = SIGCrown.prototype.getFinalFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalFlameLength_1(self, flameLengthUnits);
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

SIGMortality.prototype['updateInputsForSpeciesCodeAndEquationType'] = SIGMortality.prototype.updateInputsForSpeciesCodeAndEquationType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode, equationType) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return !!(_emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2(self, speciesCode, equationType));
};;

SIGMortality.prototype['calculateMortality'] = SIGMortality.prototype.calculateMortality = /** @suppress {undefinedVars, duplicate} @this{Object} */function(probablityUnits) {
  var self = this.ptr;
  if (probablityUnits && typeof probablityUnits === 'object') probablityUnits = probablityUnits.ptr;
  return _emscripten_bind_SIGMortality_calculateMortality_1(self, probablityUnits);
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

SIGMortality.prototype['getTreeCrownLengthScorched'] = SIGMortality.prototype.getTreeCrownLengthScorched = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mortalityRateUnits) {
  var self = this.ptr;
  if (mortalityRateUnits && typeof mortalityRateUnits === 'object') mortalityRateUnits = mortalityRateUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownLengthScorched_1(self, mortalityRateUnits);
};;

SIGMortality.prototype['getTreeCrownVolumeScorched'] = SIGMortality.prototype.getTreeCrownVolumeScorched = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mortalityRateUnits) {
  var self = this.ptr;
  if (mortalityRateUnits && typeof mortalityRateUnits === 'object') mortalityRateUnits = mortalityRateUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownVolumeScorched_1(self, mortalityRateUnits);
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

    

    // BasalAreaUnits_BasalAreaUnitsEnum

    Module['SquareFeetPerAcre'] = _emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareFeetPerAcre();

    Module['SquareMetersPerHectare'] = _emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareMetersPerHectare();

    

    // CuringLevelUnits_CuringLevelEnum

    Module['Fraction'] = _emscripten_enum_CuringLevelUnits_CuringLevelEnum_Fraction();

    Module['Percent'] = _emscripten_enum_CuringLevelUnits_CuringLevelEnum_Percent();

    

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

    

    // ContainTactic_ContainTacticEnum

    Module['HeadAttack'] = _emscripten_enum_ContainTactic_ContainTacticEnum_HeadAttack();

    Module['RearAttack'] = _emscripten_enum_ContainTactic_ContainTacticEnum_RearAttack();

    

    // ContainStatus_ContainStatusEnum

    Module['Unreported'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Unreported();

    Module['Reported'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Reported();

    Module['Attacked'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Attacked();

    Module['Contained'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Contained();

    Module['Overrun'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Overrun();

    Module['Exhausted'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Exhausted();

    Module['Overflow'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Overflow();

    Module['SizeLimitExceeded'] = _emscripten_enum_ContainStatus_ContainStatusEnum_SizeLimitExceeded();

    Module['TimeLimitExceeded'] = _emscripten_enum_ContainStatus_ContainStatusEnum_TimeLimitExceeded();

    

    // ContainFlank_ContainFlankEnum

    Module['LeftFlank'] = _emscripten_enum_ContainFlank_ContainFlankEnum_LeftFlank();

    Module['RightFlank'] = _emscripten_enum_ContainFlank_ContainFlankEnum_RightFlank();

    Module['BothFlanks'] = _emscripten_enum_ContainFlank_ContainFlankEnum_BothFlanks();

    Module['NeitherFlank'] = _emscripten_enum_ContainFlank_ContainFlankEnum_NeitherFlank();

    

    // IgnitionFuelBedType_IgnitionFuelBedTypeEnum

    Module['PonderosaPineLitter'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PonderosaPineLitter();

    Module['PunkyWoodRottenChunky'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodRottenChunky();

    Module['PunkyWoodPowderDeep'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodPowderDeep();

    Module['PunkWoodPowderShallow'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkWoodPowderShallow();

    Module['LodgepolePineDuff'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_LodgepolePineDuff();

    Module['DouglasFirDuff'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_DouglasFirDuff();

    Module['HighAltitudeMixed'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_HighAltitudeMixed();

    Module['PeatMoss'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PeatMoss();

    

    // LightningCharge_LightningChargeEnum

    Module['Negative'] = _emscripten_enum_LightningCharge_LightningChargeEnum_Negative();

    Module['Positive'] = _emscripten_enum_LightningCharge_LightningChargeEnum_Positive();

    Module['Unknown'] = _emscripten_enum_LightningCharge_LightningChargeEnum_Unknown();

    

    // SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum

    Module['CLOSED'] = _emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_CLOSED();

    Module['OPEN'] = _emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_OPEN();

    

    // SpotTreeSpecies_SpotTreeSpeciesEnum

    Module['ENGELMANN_SPRUCE'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_ENGELMANN_SPRUCE();

    Module['DOUGLAS_FIR'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_DOUGLAS_FIR();

    Module['SUBALPINE_FIR'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SUBALPINE_FIR();

    Module['WESTERN_HEMLOCK'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_HEMLOCK();

    Module['PONDEROSA_PINE'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_PONDEROSA_PINE();

    Module['LODGEPOLE_PINE'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LODGEPOLE_PINE();

    Module['WESTERN_WHITE_PINE'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_WHITE_PINE();

    Module['GRAND_FIR'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_GRAND_FIR();

    Module['BALSAM_FIR'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_BALSAM_FIR();

    Module['SLASH_PINE'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SLASH_PINE();

    Module['LONGLEAF_PINE'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LONGLEAF_PINE();

    Module['POND_PINE'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_POND_PINE();

    Module['SHORTLEAF_PINE'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SHORTLEAF_PINE();

    Module['LOBLOLLY_PINE'] = _emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LOBLOLLY_PINE();

    

    // SpotFireLocation_SpotFireLocationEnum

    Module['MIDSLOPE_WINDWARD'] = _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_WINDWARD();

    Module['VALLEY_BOTTOM'] = _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_VALLEY_BOTTOM();

    Module['MIDSLOPE_LEEWARD'] = _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_LEEWARD();

    Module['RIDGE_TOP'] = _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_RIDGE_TOP();

    

    // FuelLifeState_FuelLifeStateEnum

    Module['Dead'] = _emscripten_enum_FuelLifeState_FuelLifeStateEnum_Dead();

    Module['Live'] = _emscripten_enum_FuelLifeState_FuelLifeStateEnum_Live();

    

    // FuelConstantsEnum_FuelConstantsEnum

    Module['MaxLifeStates'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLifeStates();

    Module['MaxLiveSizeClasses'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLiveSizeClasses();

    Module['MaxDeadSizeClasses'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxDeadSizeClasses();

    Module['MaxParticles'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxParticles();

    Module['MaxSavrSizeClasses'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxSavrSizeClasses();

    Module['MaxFuelModels'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxFuelModels();

    

    // AspenFireSeverity_AspenFireSeverityEnum

    Module['Low'] = _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low();

    Module['Moderate'] = _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate();

    

    // ChaparralFuelType_ChaparralFuelTypeEnum

    Module['NotSet'] = _emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_NotSet();

    Module['Chamise'] = _emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_Chamise();

    Module['MixedBrush'] = _emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_MixedBrush();

    

    // ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum

    Module['DirectFuelLoad'] = _emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_DirectFuelLoad();

    Module['FuelLoadFromDepthAndChaparralType'] = _emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_FuelLoadFromDepthAndChaparralType();

    

    // MoistureInputMode_MoistureInputModeEnum

    Module['BySizeClass'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_BySizeClass();

    Module['AllAggregate'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_AllAggregate();

    Module['DeadAggregateAndLiveSizeClass'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_DeadAggregateAndLiveSizeClass();

    Module['LiveAggregateAndDeadSizeClass'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_LiveAggregateAndDeadSizeClass();

    Module['MoistureScenario'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_MoistureScenario();

    

    // MoistureClassInput_MoistureClassInputEnum

    Module['OneHour'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_OneHour();

    Module['TenHour'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_TenHour();

    Module['HundredHour'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_HundredHour();

    Module['LiveHerbaceous'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveHerbaceous();

    Module['LiveWoody'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveWoody();

    Module['DeadAggregate'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_DeadAggregate();

    Module['LiveAggregate'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveAggregate();

    

    // SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum

    Module['FromIgnitionPoint'] = _emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromIgnitionPoint();

    Module['FromPerimeter'] = _emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromPerimeter();

    

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

    

    // CrownDamageType

    Module['not_set'] = _emscripten_enum_CrownDamageType_not_set();

    Module['crown_length'] = _emscripten_enum_CrownDamageType_crown_length();

    Module['crown_volume'] = _emscripten_enum_CrownDamageType_crown_volume();

    Module['crown_kill'] = _emscripten_enum_CrownDamageType_crown_kill();

    

    // EquationType

    Module['not_set'] = _emscripten_enum_EquationType_not_set();

    Module['crown_scorch'] = _emscripten_enum_EquationType_crown_scorch();

    Module['bole_char'] = _emscripten_enum_EquationType_bole_char();

    Module['crown_damage'] = _emscripten_enum_EquationType_crown_damage();

    

    // FireSeverity

    Module['not_set'] = _emscripten_enum_FireSeverity_not_set();

    Module['empty'] = _emscripten_enum_FireSeverity_empty();

    Module['low'] = _emscripten_enum_FireSeverity_low();

    

    // FlameLengthOrScorchHeightSwitch

    Module['flame_length'] = _emscripten_enum_FlameLengthOrScorchHeightSwitch_flame_length();

    Module['scorch_height'] = _emscripten_enum_FlameLengthOrScorchHeightSwitch_scorch_height();

    

    // MortalityRateUnits_MortalityRateUnitsEnum

    Module['Fraction'] = _emscripten_enum_MortalityRateUnits_MortalityRateUnitsEnum_Fraction();

    Module['Percent'] = _emscripten_enum_MortalityRateUnits_MortalityRateUnitsEnum_Percent();

    

    // RegionCode

    Module['interior_west'] = _emscripten_enum_RegionCode_interior_west();

    Module['pacific_west'] = _emscripten_enum_RegionCode_pacific_west();

    Module['north_east'] = _emscripten_enum_RegionCode_north_east();

    Module['south_east'] = _emscripten_enum_RegionCode_south_east();

    

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

  }
  if (runtimeInitialized) setupEnums();
  else addOnInit(setupEnums);
})();


// end include: /home/kcheung/work/code/behave-polylith/behave-lib/include/js/glue.js
