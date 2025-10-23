(() => {
  var __defProp = Object.defineProperty;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __moduleCache = /* @__PURE__ */ new WeakMap;
  var __toCommonJS = (from) => {
    var entry = __moduleCache.get(from), desc;
    if (entry)
      return entry;
    entry = __defProp({}, "__esModule", { value: true });
    if (from && typeof from === "object" || typeof from === "function")
      __getOwnPropNames(from).map((key) => !__hasOwnProp.call(entry, key) && __defProp(entry, key, {
        get: () => from[key],
        enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable
      }));
    __moduleCache.set(from, entry);
    return entry;
  };
  var __export = (target, all) => {
    for (var name in all)
      __defProp(target, name, {
        get: all[name],
        enumerable: true,
        configurable: true,
        set: (newValue) => all[name] = () => newValue
      });
  };

  // sqlite.js
  var exports_sqlite = {};
  __export(exports_sqlite, {
    init_logger: () => init_logger,
    initSync: () => initSync,
    default: () => sqlite_default,
    WasmColumnValue: () => WasmColumnValue,
    Database: () => Database
  });
  var wasm;
  var cachedUint8ArrayMemory0 = null;
  function getUint8ArrayMemory0() {
    if (cachedUint8ArrayMemory0 === null || cachedUint8ArrayMemory0.byteLength === 0) {
      cachedUint8ArrayMemory0 = new Uint8Array(wasm.memory.buffer);
    }
    return cachedUint8ArrayMemory0;
  }
  var cachedTextDecoder = new TextDecoder("utf-8", { ignoreBOM: true, fatal: true });
  cachedTextDecoder.decode();
  var MAX_SAFARI_DECODE_BYTES = 2146435072;
  var numBytesDecoded = 0;
  function decodeText(ptr, len) {
    numBytesDecoded += len;
    if (numBytesDecoded >= MAX_SAFARI_DECODE_BYTES) {
      cachedTextDecoder = new TextDecoder("utf-8", { ignoreBOM: true, fatal: true });
      cachedTextDecoder.decode();
      numBytesDecoded = len;
    }
    return cachedTextDecoder.decode(getUint8ArrayMemory0().subarray(ptr, ptr + len));
  }
  function getStringFromWasm0(ptr, len) {
    ptr = ptr >>> 0;
    return decodeText(ptr, len);
  }
  var WASM_VECTOR_LEN = 0;
  var cachedTextEncoder = new TextEncoder;
  if (!("encodeInto" in cachedTextEncoder)) {
    cachedTextEncoder.encodeInto = function(arg, view) {
      const buf = cachedTextEncoder.encode(arg);
      view.set(buf);
      return {
        read: arg.length,
        written: buf.length
      };
    };
  }
  function passStringToWasm0(arg, malloc, realloc) {
    if (realloc === undefined) {
      const buf = cachedTextEncoder.encode(arg);
      const ptr2 = malloc(buf.length, 1) >>> 0;
      getUint8ArrayMemory0().subarray(ptr2, ptr2 + buf.length).set(buf);
      WASM_VECTOR_LEN = buf.length;
      return ptr2;
    }
    let len = arg.length;
    let ptr = malloc(len, 1) >>> 0;
    const mem = getUint8ArrayMemory0();
    let offset = 0;
    for (;offset < len; offset++) {
      const code = arg.charCodeAt(offset);
      if (code > 127)
        break;
      mem[ptr + offset] = code;
    }
    if (offset !== len) {
      if (offset !== 0) {
        arg = arg.slice(offset);
      }
      ptr = realloc(ptr, len, len = offset + arg.length * 3, 1) >>> 0;
      const view = getUint8ArrayMemory0().subarray(ptr + offset, ptr + len);
      const ret = cachedTextEncoder.encodeInto(arg, view);
      offset += ret.written;
      ptr = realloc(ptr, len, offset, 1) >>> 0;
    }
    WASM_VECTOR_LEN = offset;
    return ptr;
  }
  var cachedDataViewMemory0 = null;
  function getDataViewMemory0() {
    if (cachedDataViewMemory0 === null || cachedDataViewMemory0.buffer.detached === true || cachedDataViewMemory0.buffer.detached === undefined && cachedDataViewMemory0.buffer !== wasm.memory.buffer) {
      cachedDataViewMemory0 = new DataView(wasm.memory.buffer);
    }
    return cachedDataViewMemory0;
  }
  function addToExternrefTable0(obj) {
    const idx = wasm.__externref_table_alloc();
    wasm.__wbindgen_export_4.set(idx, obj);
    return idx;
  }
  function handleError(f, args) {
    try {
      return f.apply(this, args);
    } catch (e) {
      const idx = addToExternrefTable0(e);
      wasm.__wbindgen_exn_store(idx);
    }
  }
  function isLikeNone(x) {
    return x === undefined || x === null;
  }
  function getArrayU8FromWasm0(ptr, len) {
    ptr = ptr >>> 0;
    return getUint8ArrayMemory0().subarray(ptr / 1, ptr / 1 + len);
  }
  function debugString(val) {
    const type = typeof val;
    if (type == "number" || type == "boolean" || val == null) {
      return `${val}`;
    }
    if (type == "string") {
      return `"${val}"`;
    }
    if (type == "symbol") {
      const description = val.description;
      if (description == null) {
        return "Symbol";
      } else {
        return `Symbol(${description})`;
      }
    }
    if (type == "function") {
      const name = val.name;
      if (typeof name == "string" && name.length > 0) {
        return `Function(${name})`;
      } else {
        return "Function";
      }
    }
    if (Array.isArray(val)) {
      const length = val.length;
      let debug = "[";
      if (length > 0) {
        debug += debugString(val[0]);
      }
      for (let i = 1;i < length; i++) {
        debug += ", " + debugString(val[i]);
      }
      debug += "]";
      return debug;
    }
    const builtInMatches = /\[object ([^\]]+)\]/.exec(toString.call(val));
    let className;
    if (builtInMatches && builtInMatches.length > 1) {
      className = builtInMatches[1];
    } else {
      return toString.call(val);
    }
    if (className == "Object") {
      try {
        return "Object(" + JSON.stringify(val) + ")";
      } catch (_) {
        return "Object";
      }
    }
    if (val instanceof Error) {
      return `${val.name}: ${val.message}
${val.stack}`;
    }
    return className;
  }
  var CLOSURE_DTORS = typeof FinalizationRegistry === "undefined" ? { register: () => {}, unregister: () => {} } : new FinalizationRegistry((state) => {
    wasm.__wbindgen_export_5.get(state.dtor)(state.a, state.b);
  });
  function makeMutClosure(arg0, arg1, dtor, f) {
    const state = { a: arg0, b: arg1, cnt: 1, dtor };
    const real = (...args) => {
      state.cnt++;
      const a = state.a;
      state.a = 0;
      try {
        return f(a, state.b, ...args);
      } finally {
        if (--state.cnt === 0) {
          wasm.__wbindgen_export_5.get(state.dtor)(a, state.b);
          CLOSURE_DTORS.unregister(state);
        } else {
          state.a = a;
        }
      }
    };
    real.original = state;
    CLOSURE_DTORS.register(real, state, state);
    return real;
  }
  function init_logger() {
    wasm.init_logger();
  }
  function takeFromExternrefTable0(idx) {
    const value = wasm.__wbindgen_export_4.get(idx);
    wasm.__externref_table_dealloc(idx);
    return value;
  }
  function passArray8ToWasm0(arg, malloc) {
    const ptr = malloc(arg.length * 1, 1) >>> 0;
    getUint8ArrayMemory0().set(arg, ptr / 1);
    WASM_VECTOR_LEN = arg.length;
    return ptr;
  }
  function __wbg_adapter_6(arg0, arg1, arg2) {
    wasm.closure310_externref_shim(arg0, arg1, arg2);
  }
  function __wbg_adapter_9(arg0, arg1, arg2) {
    wasm.closure933_externref_shim(arg0, arg1, arg2);
  }
  function __wbg_adapter_16(arg0, arg1) {
    wasm.wasm_bindgen__convert__closures_____invoke__hb8bb58060fde6c51(arg0, arg1);
  }
  function __wbg_adapter_240(arg0, arg1, arg2, arg3) {
    wasm.closure961_externref_shim(arg0, arg1, arg2, arg3);
  }
  var __wbindgen_enum_IdbTransactionMode = ["readonly", "readwrite", "versionchange", "readwriteflush", "cleanup"];
  var DatabaseFinalization = typeof FinalizationRegistry === "undefined" ? { register: () => {}, unregister: () => {} } : new FinalizationRegistry((ptr) => wasm.__wbg_database_free(ptr >>> 0, 1));

  class Database {
    static __wrap(ptr) {
      ptr = ptr >>> 0;
      const obj = Object.create(Database.prototype);
      obj.__wbg_ptr = ptr;
      DatabaseFinalization.register(obj, obj.__wbg_ptr, obj);
      return obj;
    }
    __destroy_into_raw() {
      const ptr = this.__wbg_ptr;
      this.__wbg_ptr = 0;
      DatabaseFinalization.unregister(this);
      return ptr;
    }
    free() {
      const ptr = this.__destroy_into_raw();
      wasm.__wbg_database_free(ptr, 0);
    }
    static newDatabase(name) {
      const ptr0 = passStringToWasm0(name, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.database_newDatabase(ptr0, len0);
      return ret;
    }
    execute(sql) {
      const ptr0 = passStringToWasm0(sql, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.database_execute(this.__wbg_ptr, ptr0, len0);
      return ret;
    }
    executeWithParams(sql, params) {
      const ptr0 = passStringToWasm0(sql, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.database_executeWithParams(this.__wbg_ptr, ptr0, len0, params);
      return ret;
    }
    close() {
      const ret = wasm.database_close(this.__wbg_ptr);
      return ret;
    }
    sync() {
      const ret = wasm.database_sync(this.__wbg_ptr);
      return ret;
    }
    allowNonLeaderWrites(allow) {
      const ret = wasm.database_allowNonLeaderWrites(this.__wbg_ptr, allow);
      return ret;
    }
    exportToFile() {
      const ret = wasm.database_exportToFile(this.__wbg_ptr);
      return ret;
    }
    importFromFile(file_data) {
      const ret = wasm.database_importFromFile(this.__wbg_ptr, file_data);
      return ret;
    }
    waitForLeadership() {
      const ret = wasm.database_waitForLeadership(this.__wbg_ptr);
      return ret;
    }
    requestLeadership() {
      const ret = wasm.database_requestLeadership(this.__wbg_ptr);
      return ret;
    }
    getLeaderInfo() {
      const ret = wasm.database_getLeaderInfo(this.__wbg_ptr);
      return ret;
    }
    queueWrite(sql) {
      const ptr0 = passStringToWasm0(sql, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.database_queueWrite(this.__wbg_ptr, ptr0, len0);
      return ret;
    }
    queueWriteWithTimeout(sql, timeout_ms) {
      const ptr0 = passStringToWasm0(sql, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.database_queueWriteWithTimeout(this.__wbg_ptr, ptr0, len0, timeout_ms);
      return ret;
    }
    isLeader() {
      const ret = wasm.database_isLeader(this.__wbg_ptr);
      return ret;
    }
    is_leader() {
      const ret = wasm.database_is_leader(this.__wbg_ptr);
      return ret;
    }
    onDataChange(callback) {
      const ret = wasm.database_onDataChange(this.__wbg_ptr, callback);
      if (ret[1]) {
        throw takeFromExternrefTable0(ret[0]);
      }
    }
    enableOptimisticUpdates(enabled) {
      const ret = wasm.database_enableOptimisticUpdates(this.__wbg_ptr, enabled);
      return ret;
    }
    isOptimisticMode() {
      const ret = wasm.database_isOptimisticMode(this.__wbg_ptr);
      return ret;
    }
    trackOptimisticWrite(sql) {
      const ptr0 = passStringToWasm0(sql, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.database_trackOptimisticWrite(this.__wbg_ptr, ptr0, len0);
      return ret;
    }
    getPendingWritesCount() {
      const ret = wasm.database_getPendingWritesCount(this.__wbg_ptr);
      return ret;
    }
    clearOptimisticWrites() {
      const ret = wasm.database_clearOptimisticWrites(this.__wbg_ptr);
      return ret;
    }
    enableCoordinationMetrics(enabled) {
      const ret = wasm.database_enableCoordinationMetrics(this.__wbg_ptr, enabled);
      return ret;
    }
    isCoordinationMetricsEnabled() {
      const ret = wasm.database_isCoordinationMetricsEnabled(this.__wbg_ptr);
      return ret;
    }
    recordLeadershipChange(became_leader) {
      const ret = wasm.database_recordLeadershipChange(this.__wbg_ptr, became_leader);
      return ret;
    }
    recordNotificationLatency(latency_ms) {
      const ret = wasm.database_recordNotificationLatency(this.__wbg_ptr, latency_ms);
      return ret;
    }
    recordWriteConflict() {
      const ret = wasm.database_recordWriteConflict(this.__wbg_ptr);
      return ret;
    }
    recordFollowerRefresh() {
      const ret = wasm.database_recordFollowerRefresh(this.__wbg_ptr);
      return ret;
    }
    getCoordinationMetrics() {
      const ret = wasm.database_getCoordinationMetrics(this.__wbg_ptr);
      return ret;
    }
    resetCoordinationMetrics() {
      const ret = wasm.database_resetCoordinationMetrics(this.__wbg_ptr);
      return ret;
    }
  }
  if (Symbol.dispose)
    Database.prototype[Symbol.dispose] = Database.prototype.free;
  var WasmColumnValueFinalization = typeof FinalizationRegistry === "undefined" ? { register: () => {}, unregister: () => {} } : new FinalizationRegistry((ptr) => wasm.__wbg_wasmcolumnvalue_free(ptr >>> 0, 1));

  class WasmColumnValue {
    static __wrap(ptr) {
      ptr = ptr >>> 0;
      const obj = Object.create(WasmColumnValue.prototype);
      obj.__wbg_ptr = ptr;
      WasmColumnValueFinalization.register(obj, obj.__wbg_ptr, obj);
      return obj;
    }
    __destroy_into_raw() {
      const ptr = this.__wbg_ptr;
      this.__wbg_ptr = 0;
      WasmColumnValueFinalization.unregister(this);
      return ptr;
    }
    free() {
      const ptr = this.__destroy_into_raw();
      wasm.__wbg_wasmcolumnvalue_free(ptr, 0);
    }
    static createNull() {
      const ret = wasm.wasmcolumnvalue_createNull();
      return WasmColumnValue.__wrap(ret);
    }
    static createInteger(value) {
      const ret = wasm.wasmcolumnvalue_createInteger(value);
      return WasmColumnValue.__wrap(ret);
    }
    static createReal(value) {
      const ret = wasm.wasmcolumnvalue_createReal(value);
      return WasmColumnValue.__wrap(ret);
    }
    static createText(value) {
      const ptr0 = passStringToWasm0(value, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.wasmcolumnvalue_createText(ptr0, len0);
      return WasmColumnValue.__wrap(ret);
    }
    static createBlob(value) {
      const ptr0 = passArray8ToWasm0(value, wasm.__wbindgen_malloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.wasmcolumnvalue_createBlob(ptr0, len0);
      return WasmColumnValue.__wrap(ret);
    }
    static createBigInt(value) {
      const ptr0 = passStringToWasm0(value, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.wasmcolumnvalue_createBigInt(ptr0, len0);
      return WasmColumnValue.__wrap(ret);
    }
    static createDate(timestamp) {
      const ret = wasm.wasmcolumnvalue_createDate(timestamp);
      return WasmColumnValue.__wrap(ret);
    }
    static fromJsValue(value) {
      const ret = wasm.wasmcolumnvalue_fromJsValue(value);
      return WasmColumnValue.__wrap(ret);
    }
    static null() {
      const ret = wasm.wasmcolumnvalue_createNull();
      return WasmColumnValue.__wrap(ret);
    }
    static integer(value) {
      const ret = wasm.wasmcolumnvalue_integer(value);
      return WasmColumnValue.__wrap(ret);
    }
    static real(value) {
      const ret = wasm.wasmcolumnvalue_createReal(value);
      return WasmColumnValue.__wrap(ret);
    }
    static text(value) {
      const ptr0 = passStringToWasm0(value, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.wasmcolumnvalue_createText(ptr0, len0);
      return WasmColumnValue.__wrap(ret);
    }
    static blob(value) {
      const ptr0 = passArray8ToWasm0(value, wasm.__wbindgen_malloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.wasmcolumnvalue_blob(ptr0, len0);
      return WasmColumnValue.__wrap(ret);
    }
    static big_int(value) {
      const ptr0 = passStringToWasm0(value, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len0 = WASM_VECTOR_LEN;
      const ret = wasm.wasmcolumnvalue_big_int(ptr0, len0);
      return WasmColumnValue.__wrap(ret);
    }
    static date(timestamp_ms) {
      const ret = wasm.wasmcolumnvalue_createDate(timestamp_ms);
      return WasmColumnValue.__wrap(ret);
    }
  }
  if (Symbol.dispose)
    WasmColumnValue.prototype[Symbol.dispose] = WasmColumnValue.prototype.free;
  var EXPECTED_RESPONSE_TYPES = new Set(["basic", "cors", "default"]);
  async function __wbg_load(module, imports) {
    if (typeof Response === "function" && module instanceof Response) {
      if (typeof WebAssembly.instantiateStreaming === "function") {
        try {
          return await WebAssembly.instantiateStreaming(module, imports);
        } catch (e) {
          const validResponse = module.ok && EXPECTED_RESPONSE_TYPES.has(module.type);
          if (validResponse && module.headers.get("Content-Type") !== "application/wasm") {
            console.warn("`WebAssembly.instantiateStreaming` failed because your server does not serve Wasm with `application/wasm` MIME type. Falling back to `WebAssembly.instantiate` which is slower. Original error:\n", e);
          } else {
            throw e;
          }
        }
      }
      const bytes = await module.arrayBuffer();
      return await WebAssembly.instantiate(bytes, imports);
    } else {
      const instance = await WebAssembly.instantiate(module, imports);
      if (instance instanceof WebAssembly.Instance) {
        return { instance, module };
      } else {
        return instance;
      }
    }
  }
  function __wbg_get_imports() {
    const imports = {};
    imports.wbg = {};
    imports.wbg.__wbg_Error_e17e777aac105295 = function(arg0, arg1) {
      const ret = Error(getStringFromWasm0(arg0, arg1));
      return ret;
    };
    imports.wbg.__wbg_Number_998bea33bd87c3e0 = function(arg0) {
      const ret = Number(arg0);
      return ret;
    };
    imports.wbg.__wbg_String_8f0eb39a4a4c2f66 = function(arg0, arg1) {
      const ret = String(arg1);
      const ptr1 = passStringToWasm0(ret, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len1 = WASM_VECTOR_LEN;
      getDataViewMemory0().setInt32(arg0 + 4 * 1, len1, true);
      getDataViewMemory0().setInt32(arg0 + 4 * 0, ptr1, true);
    };
    imports.wbg.__wbg_bound_791a2b3ae6f3b269 = function() {
      return handleError(function(arg0, arg1) {
        const ret = IDBKeyRange.bound(arg0, arg1);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_call_13410aac570ffff7 = function() {
      return handleError(function(arg0, arg1) {
        const ret = arg0.call(arg1);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_call_a5400b25a865cfd8 = function() {
      return handleError(function(arg0, arg1, arg2) {
        const ret = arg0.call(arg1, arg2);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_clearInterval_f2d83bf5893f2fa6 = function(arg0, arg1) {
      arg0.clearInterval(arg1);
    };
    imports.wbg.__wbg_contains_ba81bb246a03c507 = function(arg0, arg1, arg2) {
      const ret = arg0.contains(getStringFromWasm0(arg1, arg2));
      return ret;
    };
    imports.wbg.__wbg_continue_f3937b9af363e05d = function() {
      return handleError(function(arg0) {
        arg0.continue();
      }, arguments);
    };
    imports.wbg.__wbg_createObjectStore_2bc52da689ca2130 = function() {
      return handleError(function(arg0, arg1, arg2) {
        const ret = arg0.createObjectStore(getStringFromWasm0(arg1, arg2));
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_data_9ab529722bcc4e6c = function(arg0) {
      const ret = arg0.data;
      return ret;
    };
    imports.wbg.__wbg_database_new = function(arg0) {
      const ret = Database.__wrap(arg0);
      return ret;
    };
    imports.wbg.__wbg_debug_c906769d2f88c17b = function(arg0) {
      console.debug(arg0);
    };
    imports.wbg.__wbg_done_75ed0ee6dd243d9d = function(arg0) {
      const ret = arg0.done;
      return ret;
    };
    imports.wbg.__wbg_entries_2be2f15bd5554996 = function(arg0) {
      const ret = Object.entries(arg0);
      return ret;
    };
    imports.wbg.__wbg_error_99981e16d476aa5c = function(arg0) {
      console.error(arg0);
    };
    imports.wbg.__wbg_getDate_9615e288fc892247 = function(arg0) {
      const ret = arg0.getDate();
      return ret;
    };
    imports.wbg.__wbg_getDay_c9c4f57fb4ef6fef = function(arg0) {
      const ret = arg0.getDay();
      return ret;
    };
    imports.wbg.__wbg_getFullYear_e351a9fa7d2fab83 = function(arg0) {
      const ret = arg0.getFullYear();
      return ret;
    };
    imports.wbg.__wbg_getHours_4cc14de357c9e723 = function(arg0) {
      const ret = arg0.getHours();
      return ret;
    };
    imports.wbg.__wbg_getItem_9fc74b31b896f95a = function() {
      return handleError(function(arg0, arg1, arg2, arg3) {
        const ret = arg1.getItem(getStringFromWasm0(arg2, arg3));
        var ptr1 = isLikeNone(ret) ? 0 : passStringToWasm0(ret, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
        var len1 = WASM_VECTOR_LEN;
        getDataViewMemory0().setInt32(arg0 + 4 * 1, len1, true);
        getDataViewMemory0().setInt32(arg0 + 4 * 0, ptr1, true);
      }, arguments);
    };
    imports.wbg.__wbg_getMinutes_6cde8fdd08b0c2ec = function(arg0) {
      const ret = arg0.getMinutes();
      return ret;
    };
    imports.wbg.__wbg_getMonth_8cc234bce5c8bcac = function(arg0) {
      const ret = arg0.getMonth();
      return ret;
    };
    imports.wbg.__wbg_getSeconds_c2f02452d804ece0 = function(arg0) {
      const ret = arg0.getSeconds();
      return ret;
    };
    imports.wbg.__wbg_getTime_6bb3f64e0f18f817 = function(arg0) {
      const ret = arg0.getTime();
      return ret;
    };
    imports.wbg.__wbg_getTimezoneOffset_1e3ddc1382e7c8b0 = function(arg0) {
      const ret = arg0.getTimezoneOffset();
      return ret;
    };
    imports.wbg.__wbg_get_0da715ceaecea5c8 = function(arg0, arg1) {
      const ret = arg0[arg1 >>> 0];
      return ret;
    };
    imports.wbg.__wbg_get_1167dc45047c17fe = function(arg0, arg1, arg2) {
      const ret = arg1[arg2 >>> 0];
      var ptr1 = isLikeNone(ret) ? 0 : passStringToWasm0(ret, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      var len1 = WASM_VECTOR_LEN;
      getDataViewMemory0().setInt32(arg0 + 4 * 1, len1, true);
      getDataViewMemory0().setInt32(arg0 + 4 * 0, ptr1, true);
    };
    imports.wbg.__wbg_get_1b2c33a63c4be73f = function() {
      return handleError(function(arg0, arg1) {
        const ret = arg0.get(arg1);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_get_458e874b43b18b25 = function() {
      return handleError(function(arg0, arg1) {
        const ret = Reflect.get(arg0, arg1);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_getwithrefkey_1dc361bd10053bfe = function(arg0, arg1) {
      const ret = arg0[arg1];
      return ret;
    };
    imports.wbg.__wbg_indexedDB_1956995e4297311c = function() {
      return handleError(function(arg0) {
        const ret = arg0.indexedDB;
        return isLikeNone(ret) ? 0 : addToExternrefTable0(ret);
      }, arguments);
    };
    imports.wbg.__wbg_info_6cf68c1a86a92f6a = function(arg0) {
      console.info(arg0);
    };
    imports.wbg.__wbg_instanceof_ArrayBuffer_67f3012529f6a2dd = function(arg0) {
      let result;
      try {
        result = arg0 instanceof ArrayBuffer;
      } catch (_) {
        result = false;
      }
      const ret = result;
      return ret;
    };
    imports.wbg.__wbg_instanceof_IdbDatabase_6e6efef94c4a355d = function(arg0) {
      let result;
      try {
        result = arg0 instanceof IDBDatabase;
      } catch (_) {
        result = false;
      }
      const ret = result;
      return ret;
    };
    imports.wbg.__wbg_instanceof_IdbOpenDbRequest_2be27facb05c6739 = function(arg0) {
      let result;
      try {
        result = arg0 instanceof IDBOpenDBRequest;
      } catch (_) {
        result = false;
      }
      const ret = result;
      return ret;
    };
    imports.wbg.__wbg_instanceof_Map_ebb01a5b6b5ffd0b = function(arg0) {
      let result;
      try {
        result = arg0 instanceof Map;
      } catch (_) {
        result = false;
      }
      const ret = result;
      return ret;
    };
    imports.wbg.__wbg_instanceof_Uint8Array_9a8378d955933db7 = function(arg0) {
      let result;
      try {
        result = arg0 instanceof Uint8Array;
      } catch (_) {
        result = false;
      }
      const ret = result;
      return ret;
    };
    imports.wbg.__wbg_instanceof_Window_12d20d558ef92592 = function(arg0) {
      let result;
      try {
        result = arg0 instanceof Window;
      } catch (_) {
        result = false;
      }
      const ret = result;
      return ret;
    };
    imports.wbg.__wbg_isArray_030cce220591fb41 = function(arg0) {
      const ret = Array.isArray(arg0);
      return ret;
    };
    imports.wbg.__wbg_isSafeInteger_1c0d1af5542e102a = function(arg0) {
      const ret = Number.isSafeInteger(arg0);
      return ret;
    };
    imports.wbg.__wbg_iterator_f370b34483c71a1c = function() {
      const ret = Symbol.iterator;
      return ret;
    };
    imports.wbg.__wbg_key_4c46cfeacd2dbc18 = function() {
      return handleError(function(arg0) {
        const ret = arg0.key;
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_length_186546c51cd61acd = function(arg0) {
      const ret = arg0.length;
      return ret;
    };
    imports.wbg.__wbg_length_6bb7e81f9d7713e4 = function(arg0) {
      const ret = arg0.length;
      return ret;
    };
    imports.wbg.__wbg_length_9d771c54845e987f = function(arg0) {
      const ret = arg0.length;
      return ret;
    };
    imports.wbg.__wbg_length_afebfa9c2d66f7e4 = function(arg0) {
      const ret = arg0.length;
      return ret;
    };
    imports.wbg.__wbg_localStorage_9330af8bf39365ba = function() {
      return handleError(function(arg0) {
        const ret = arg0.localStorage;
        return isLikeNone(ret) ? 0 : addToExternrefTable0(ret);
      }, arguments);
    };
    imports.wbg.__wbg_log_6c7b5f4f00b8ce3f = function(arg0) {
      console.log(arg0);
    };
    imports.wbg.__wbg_new0_b0a0a38c201e6df5 = function() {
      const ret = new Date;
      return ret;
    };
    imports.wbg.__wbg_new_19c25a3f2fa63a02 = function() {
      const ret = new Object;
      return ret;
    };
    imports.wbg.__wbg_new_1f3a344cf3123716 = function() {
      const ret = new Array;
      return ret;
    };
    imports.wbg.__wbg_new_2e3c58a15f39f5f9 = function(arg0, arg1) {
      try {
        var state0 = { a: arg0, b: arg1 };
        var cb0 = (arg02, arg12) => {
          const a = state0.a;
          state0.a = 0;
          try {
            return __wbg_adapter_240(a, state0.b, arg02, arg12);
          } finally {
            state0.a = a;
          }
        };
        const ret = new Promise(cb0);
        return ret;
      } finally {
        state0.a = state0.b = 0;
      }
    };
    imports.wbg.__wbg_new_5a2ae4557f92b50e = function(arg0) {
      const ret = new Date(arg0);
      return ret;
    };
    imports.wbg.__wbg_new_638ebfaedbf32a5e = function(arg0) {
      const ret = new Uint8Array(arg0);
      return ret;
    };
    imports.wbg.__wbg_new_d44a894c2d64757c = function() {
      return handleError(function(arg0, arg1) {
        const ret = new BroadcastChannel(getStringFromWasm0(arg0, arg1));
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_newfromslice_074c56947bd43469 = function(arg0, arg1) {
      const ret = new Uint8Array(getArrayU8FromWasm0(arg0, arg1));
      return ret;
    };
    imports.wbg.__wbg_newnoargs_254190557c45b4ec = function(arg0, arg1) {
      const ret = new Function(getStringFromWasm0(arg0, arg1));
      return ret;
    };
    imports.wbg.__wbg_newwithlength_a167dcc7aaa3ba77 = function(arg0) {
      const ret = new Uint8Array(arg0 >>> 0);
      return ret;
    };
    imports.wbg.__wbg_newwithyearmonthday_9d5466a369f2521d = function(arg0, arg1, arg2) {
      const ret = new Date(arg0 >>> 0, arg1, arg2);
      return ret;
    };
    imports.wbg.__wbg_next_5b3530e612fde77d = function(arg0) {
      const ret = arg0.next;
      return ret;
    };
    imports.wbg.__wbg_next_692e82279131b03c = function() {
      return handleError(function(arg0) {
        const ret = arg0.next();
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_now_1e80617bcee43265 = function() {
      const ret = Date.now();
      return ret;
    };
    imports.wbg.__wbg_objectStoreNames_31ac72154caf5a01 = function(arg0) {
      const ret = arg0.objectStoreNames;
      return ret;
    };
    imports.wbg.__wbg_objectStore_b2a5b80b2e5c5f8b = function() {
      return handleError(function(arg0, arg1, arg2) {
        const ret = arg0.objectStore(getStringFromWasm0(arg1, arg2));
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_openCursor_4fa351acf02ef5fc = function() {
      return handleError(function(arg0, arg1) {
        const ret = arg0.openCursor(arg1);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_open_7281831ed8ff7bd2 = function() {
      return handleError(function(arg0, arg1, arg2, arg3) {
        const ret = arg0.open(getStringFromWasm0(arg1, arg2), arg3 >>> 0);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_parse_442f5ba02e5eaf8b = function() {
      return handleError(function(arg0, arg1) {
        const ret = JSON.parse(getStringFromWasm0(arg0, arg1));
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_postMessage_17937fcb20586318 = function() {
      return handleError(function(arg0, arg1) {
        arg0.postMessage(arg1);
      }, arguments);
    };
    imports.wbg.__wbg_prototypesetcall_3d4a26c1ed734349 = function(arg0, arg1, arg2) {
      Uint8Array.prototype.set.call(getArrayU8FromWasm0(arg0, arg1), arg2);
    };
    imports.wbg.__wbg_push_330b2eb93e4e1212 = function(arg0, arg1) {
      const ret = arg0.push(arg1);
      return ret;
    };
    imports.wbg.__wbg_put_cdfadd5d7f714201 = function() {
      return handleError(function(arg0, arg1, arg2) {
        const ret = arg0.put(arg1, arg2);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_queueMicrotask_25d0739ac89e8c88 = function(arg0) {
      queueMicrotask(arg0);
    };
    imports.wbg.__wbg_queueMicrotask_4488407636f5bf24 = function(arg0) {
      const ret = arg0.queueMicrotask;
      return ret;
    };
    imports.wbg.__wbg_random_7ed63a0b38ee3b75 = function() {
      const ret = Math.random();
      return ret;
    };
    imports.wbg.__wbg_removeItem_487c5a070c7adaf7 = function() {
      return handleError(function(arg0, arg1, arg2) {
        arg0.removeItem(getStringFromWasm0(arg1, arg2));
      }, arguments);
    };
    imports.wbg.__wbg_resolve_4055c623acdd6a1b = function(arg0) {
      const ret = Promise.resolve(arg0);
      return ret;
    };
    imports.wbg.__wbg_result_825a6aeeb31189d2 = function() {
      return handleError(function(arg0) {
        const ret = arg0.result;
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_setInterval_3ca6a76801aa276c = function() {
      return handleError(function(arg0, arg1, arg2) {
        const ret = arg0.setInterval(arg1, arg2);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_setItem_7add5eb06a28b38f = function() {
      return handleError(function(arg0, arg1, arg2, arg3, arg4) {
        arg0.setItem(getStringFromWasm0(arg1, arg2), getStringFromWasm0(arg3, arg4));
      }, arguments);
    };
    imports.wbg.__wbg_setTimeout_2966518f28aef92e = function() {
      return handleError(function(arg0, arg1, arg2) {
        const ret = arg0.setTimeout(arg1, arg2);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_set_1353b2a5e96bc48c = function(arg0, arg1, arg2) {
      arg0.set(getArrayU8FromWasm0(arg1, arg2));
    };
    imports.wbg.__wbg_set_3f1d0b984ed272ed = function(arg0, arg1, arg2) {
      arg0[arg1] = arg2;
    };
    imports.wbg.__wbg_set_453345bcda80b89a = function() {
      return handleError(function(arg0, arg1, arg2) {
        const ret = Reflect.set(arg0, arg1, arg2);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_set_90f6c0f7bd8c0415 = function(arg0, arg1, arg2) {
      arg0[arg1 >>> 0] = arg2;
    };
    imports.wbg.__wbg_setoncomplete_8a32ad2d1ca4f49b = function(arg0, arg1) {
      arg0.oncomplete = arg1;
    };
    imports.wbg.__wbg_setonerror_4b0c685c365f600d = function(arg0, arg1) {
      arg0.onerror = arg1;
    };
    imports.wbg.__wbg_setonerror_bcdbd7f3921ffb1f = function(arg0, arg1) {
      arg0.onerror = arg1;
    };
    imports.wbg.__wbg_setonmessage_d8ad82e8d230ecc8 = function(arg0, arg1) {
      arg0.onmessage = arg1;
    };
    imports.wbg.__wbg_setonsuccess_ffb2ddb27ce681d8 = function(arg0, arg1) {
      arg0.onsuccess = arg1;
    };
    imports.wbg.__wbg_setonupgradeneeded_4e32d1c6a08c4257 = function(arg0, arg1) {
      arg0.onupgradeneeded = arg1;
    };
    imports.wbg.__wbg_slice_974daea329f5c01d = function(arg0, arg1, arg2) {
      const ret = arg0.slice(arg1 >>> 0, arg2 >>> 0);
      return ret;
    };
    imports.wbg.__wbg_static_accessor_GLOBAL_8921f820c2ce3f12 = function() {
      const ret = typeof global === "undefined" ? null : global;
      return isLikeNone(ret) ? 0 : addToExternrefTable0(ret);
    };
    imports.wbg.__wbg_static_accessor_GLOBAL_THIS_f0a4409105898184 = function() {
      const ret = typeof globalThis === "undefined" ? null : globalThis;
      return isLikeNone(ret) ? 0 : addToExternrefTable0(ret);
    };
    imports.wbg.__wbg_static_accessor_SELF_995b214ae681ff99 = function() {
      const ret = typeof self === "undefined" ? null : self;
      return isLikeNone(ret) ? 0 : addToExternrefTable0(ret);
    };
    imports.wbg.__wbg_static_accessor_WINDOW_cde3890479c675ea = function() {
      const ret = typeof window === "undefined" ? null : window;
      return isLikeNone(ret) ? 0 : addToExternrefTable0(ret);
    };
    imports.wbg.__wbg_stringify_b98c93d0a190446a = function() {
      return handleError(function(arg0) {
        const ret = JSON.stringify(arg0);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_target_f2c963b447be6283 = function(arg0) {
      const ret = arg0.target;
      return isLikeNone(ret) ? 0 : addToExternrefTable0(ret);
    };
    imports.wbg.__wbg_then_b33a773d723afa3e = function(arg0, arg1, arg2) {
      const ret = arg0.then(arg1, arg2);
      return ret;
    };
    imports.wbg.__wbg_then_e22500defe16819f = function(arg0, arg1) {
      const ret = arg0.then(arg1);
      return ret;
    };
    imports.wbg.__wbg_toString_2ca967683e5874bc = function() {
      return handleError(function(arg0, arg1) {
        const ret = arg0.toString(arg1);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_transaction_938247b9138748bd = function() {
      return handleError(function(arg0, arg1) {
        const ret = arg0.transaction(arg1);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_transaction_e94a54f60797ce82 = function() {
      return handleError(function(arg0, arg1, arg2) {
        const ret = arg0.transaction(arg1, __wbindgen_enum_IdbTransactionMode[arg2]);
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_transaction_fc84f03ee76124ed = function() {
      return handleError(function(arg0, arg1, arg2) {
        const ret = arg0.transaction(getStringFromWasm0(arg1, arg2));
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_value_809430714c127bb5 = function() {
      return handleError(function(arg0) {
        const ret = arg0.value;
        return ret;
      }, arguments);
    };
    imports.wbg.__wbg_value_dd9372230531eade = function(arg0) {
      const ret = arg0.value;
      return ret;
    };
    imports.wbg.__wbg_warn_e2ada06313f92f09 = function(arg0) {
      console.warn(arg0);
    };
    imports.wbg.__wbg_wbindgenbigintgetasi64_ac743ece6ab9bba1 = function(arg0, arg1) {
      const v = arg1;
      const ret = typeof v === "bigint" ? v : undefined;
      getDataViewMemory0().setBigInt64(arg0 + 8 * 1, isLikeNone(ret) ? BigInt(0) : ret, true);
      getDataViewMemory0().setInt32(arg0 + 4 * 0, !isLikeNone(ret), true);
    };
    imports.wbg.__wbg_wbindgenbooleanget_3fe6f642c7d97746 = function(arg0) {
      const v = arg0;
      const ret = typeof v === "boolean" ? v : undefined;
      return isLikeNone(ret) ? 16777215 : ret ? 1 : 0;
    };
    imports.wbg.__wbg_wbindgencbdrop_eb10308566512b88 = function(arg0) {
      const obj = arg0.original;
      if (obj.cnt-- == 1) {
        obj.a = 0;
        return true;
      }
      const ret = false;
      return ret;
    };
    imports.wbg.__wbg_wbindgendebugstring_99ef257a3ddda34d = function(arg0, arg1) {
      const ret = debugString(arg1);
      const ptr1 = passStringToWasm0(ret, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      const len1 = WASM_VECTOR_LEN;
      getDataViewMemory0().setInt32(arg0 + 4 * 1, len1, true);
      getDataViewMemory0().setInt32(arg0 + 4 * 0, ptr1, true);
    };
    imports.wbg.__wbg_wbindgenin_d7a1ee10933d2d55 = function(arg0, arg1) {
      const ret = arg0 in arg1;
      return ret;
    };
    imports.wbg.__wbg_wbindgenisbigint_ecb90cc08a5a9154 = function(arg0) {
      const ret = typeof arg0 === "bigint";
      return ret;
    };
    imports.wbg.__wbg_wbindgenisfunction_8cee7dce3725ae74 = function(arg0) {
      const ret = typeof arg0 === "function";
      return ret;
    };
    imports.wbg.__wbg_wbindgenisnull_f3037694abe4d97a = function(arg0) {
      const ret = arg0 === null;
      return ret;
    };
    imports.wbg.__wbg_wbindgenisobject_307a53c6bd97fbf8 = function(arg0) {
      const val = arg0;
      const ret = typeof val === "object" && val !== null;
      return ret;
    };
    imports.wbg.__wbg_wbindgenisstring_d4fa939789f003b0 = function(arg0) {
      const ret = typeof arg0 === "string";
      return ret;
    };
    imports.wbg.__wbg_wbindgenisundefined_c4b71d073b92f3c5 = function(arg0) {
      const ret = arg0 === undefined;
      return ret;
    };
    imports.wbg.__wbg_wbindgenjsvaleq_e6f2ad59ccae1b58 = function(arg0, arg1) {
      const ret = arg0 === arg1;
      return ret;
    };
    imports.wbg.__wbg_wbindgenjsvallooseeq_9bec8c9be826bed1 = function(arg0, arg1) {
      const ret = arg0 == arg1;
      return ret;
    };
    imports.wbg.__wbg_wbindgennumberget_f74b4c7525ac05cb = function(arg0, arg1) {
      const obj = arg1;
      const ret = typeof obj === "number" ? obj : undefined;
      getDataViewMemory0().setFloat64(arg0 + 8 * 1, isLikeNone(ret) ? 0 : ret, true);
      getDataViewMemory0().setInt32(arg0 + 4 * 0, !isLikeNone(ret), true);
    };
    imports.wbg.__wbg_wbindgenstringget_0f16a6ddddef376f = function(arg0, arg1) {
      const obj = arg1;
      const ret = typeof obj === "string" ? obj : undefined;
      var ptr1 = isLikeNone(ret) ? 0 : passStringToWasm0(ret, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
      var len1 = WASM_VECTOR_LEN;
      getDataViewMemory0().setInt32(arg0 + 4 * 1, len1, true);
      getDataViewMemory0().setInt32(arg0 + 4 * 0, ptr1, true);
    };
    imports.wbg.__wbg_wbindgenthrow_451ec1a8469d7eb6 = function(arg0, arg1) {
      throw new Error(getStringFromWasm0(arg0, arg1));
    };
    imports.wbg.__wbindgen_cast_2241b6af4c4b2941 = function(arg0, arg1) {
      const ret = getStringFromWasm0(arg0, arg1);
      return ret;
    };
    imports.wbg.__wbindgen_cast_460d377fe32622f8 = function(arg0, arg1) {
      const ret = makeMutClosure(arg0, arg1, 932, __wbg_adapter_9);
      return ret;
    };
    imports.wbg.__wbindgen_cast_4625c577ab2ec9ee = function(arg0) {
      const ret = BigInt.asUintN(64, arg0);
      return ret;
    };
    imports.wbg.__wbindgen_cast_9ae0607507abb057 = function(arg0) {
      const ret = arg0;
      return ret;
    };
    imports.wbg.__wbindgen_cast_d1666c0c2a12290c = function(arg0, arg1) {
      const ret = makeMutClosure(arg0, arg1, 309, __wbg_adapter_6);
      return ret;
    };
    imports.wbg.__wbindgen_cast_d6cd19b81560fd6e = function(arg0) {
      const ret = arg0;
      return ret;
    };
    imports.wbg.__wbindgen_cast_e8c425bb8db43554 = function(arg0, arg1) {
      const ret = makeMutClosure(arg0, arg1, 309, __wbg_adapter_6);
      return ret;
    };
    imports.wbg.__wbindgen_cast_fed53a618bf0ae35 = function(arg0, arg1) {
      const ret = makeMutClosure(arg0, arg1, 309, __wbg_adapter_16);
      return ret;
    };
    imports.wbg.__wbindgen_init_externref_table = function() {
      const table = wasm.__wbindgen_export_4;
      const offset = table.grow(4);
      table.set(0, undefined);
      table.set(offset + 0, undefined);
      table.set(offset + 1, null);
      table.set(offset + 2, true);
      table.set(offset + 3, false);
    };
    return imports;
  }
  function __wbg_init_memory(imports, memory) {}
  function __wbg_finalize_init(instance, module) {
    wasm = instance.exports;
    __wbg_init.__wbindgen_wasm_module = module;
    cachedDataViewMemory0 = null;
    cachedUint8ArrayMemory0 = null;
    wasm.__wbindgen_start();
    return wasm;
  }
  function initSync(module) {
    if (wasm !== undefined)
      return wasm;
    if (typeof module !== "undefined") {
      if (Object.getPrototypeOf(module) === Object.prototype) {
        ({ module } = module);
      } else {
        console.warn("using deprecated parameters for `initSync()`; pass a single object instead");
      }
    }
    const imports = __wbg_get_imports();
    __wbg_init_memory(imports);
    if (!(module instanceof WebAssembly.Module)) {
      module = new WebAssembly.Module(module);
    }
    const instance = new WebAssembly.Instance(module, imports);
    return __wbg_finalize_init(instance, module);
  }
  async function __wbg_init(module_or_path) {
    if (wasm !== undefined)
      return wasm;
    if (typeof module_or_path !== "undefined") {
      if (Object.getPrototypeOf(module_or_path) === Object.prototype) {
        ({ module_or_path } = module_or_path);
      } else {
        console.warn("using deprecated parameters for the initialization function; pass a single object instead");
      }
    }
    if (typeof module_or_path === "undefined") {
      module_or_path = new URL("/js/sqlite.wasm");
    }
    const imports = __wbg_get_imports();
    if (typeof module_or_path === "string" || typeof Request === "function" && module_or_path instanceof Request || typeof URL === "function" && module_or_path instanceof URL) {
      module_or_path = fetch(module_or_path);
    }
    __wbg_init_memory(imports);
    const { instance, module } = await __wbg_load(await module_or_path, imports);
    return __wbg_finalize_init(instance, module);
  }
  var sqlite_default = __wbg_init;
  window.sqlite = exports_sqlite;
})();
