
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

  prepare() {
    if (ensureCache.needed) {
      // clear the temps
      for (var i = 0; i < ensureCache.temps.length; i++) {
        Module['_webidl_free'](ensureCache.temps[i]);
      }
      ensureCache.temps.length = 0;
      // prepare to allocate a bigger buffer
      Module['_webidl_free'](ensureCache.buffer);
      ensureCache.buffer = 0;
      ensureCache.size += ensureCache.needed;
      // clean up
      ensureCache.needed = 0;
    }
    if (!ensureCache.buffer) { // happens first time, or when we need to grow
      ensureCache.size += 128; // heuristic, avoid many small grow events
      ensureCache.buffer = Module['_webidl_malloc'](ensureCache.size);
      assert(ensureCache.buffer);
    }
    ensureCache.pos = 0;
  },
  alloc(array, view) {
    assert(ensureCache.buffer);
    var bytes = view.BYTES_PER_ELEMENT;
    var len = array.length * bytes;
    len = alignMemory(len, 8); // keep things aligned to 8 byte boundaries
    var ret;
    if (ensureCache.pos + len >= ensureCache.size) {
      // we failed to allocate in the buffer, ensureCache time around :(
      assert(len > 0); // null terminator, at least
      ensureCache.needed += len;
      ret = Module['_webidl_malloc'](len);
      ensureCache.temps.push(ret);
    } else {
      // we can allocate in the buffer
      ret = ensureCache.buffer + ensureCache.pos;
      ensureCache.pos += len;
    }
    return ret;
  },
  copy(array, view, offset) {
    offset /= view.BYTES_PER_ELEMENT;
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

// Interface: VoidPtr

/** @suppress {undefinedVars, duplicate} @this{Object} */
function VoidPtr() { throw "cannot construct a VoidPtr, no constructor in IDL" }
VoidPtr.prototype = Object.create(WrapperObject.prototype);
VoidPtr.prototype.constructor = VoidPtr;
VoidPtr.prototype.__class__ = VoidPtr;
VoidPtr.__cache__ = {};
Module['VoidPtr'] = VoidPtr;

/** @suppress {undefinedVars, duplicate} @this{Object} */
VoidPtr.prototype['__destroy__'] = VoidPtr.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_VoidPtr___destroy___0(self);
};

// Interface: DoublePtr

/** @suppress {undefinedVars, duplicate} @this{Object} */
function DoublePtr() { throw "cannot construct a DoublePtr, no constructor in IDL" }
DoublePtr.prototype = Object.create(WrapperObject.prototype);
DoublePtr.prototype.constructor = DoublePtr;
DoublePtr.prototype.__class__ = DoublePtr;
DoublePtr.__cache__ = {};
Module['DoublePtr'] = DoublePtr;

/** @suppress {undefinedVars, duplicate} @this{Object} */
DoublePtr.prototype['__destroy__'] = DoublePtr.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_DoublePtr___destroy___0(self);
};

// Interface: BoolVector

/** @suppress {undefinedVars, duplicate} @this{Object} */
function BoolVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_BoolVector_BoolVector_0(); getCache(BoolVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_BoolVector_BoolVector_1(size);
  getCache(BoolVector)[this.ptr] = this;
};

BoolVector.prototype = Object.create(WrapperObject.prototype);
BoolVector.prototype.constructor = BoolVector;
BoolVector.prototype.__class__ = BoolVector;
BoolVector.__cache__ = {};
Module['BoolVector'] = BoolVector;
/** @suppress {undefinedVars, duplicate} @this{Object} */
BoolVector.prototype['resize'] = BoolVector.prototype.resize = function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_BoolVector_resize_1(self, size);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
BoolVector.prototype['get'] = BoolVector.prototype.get = function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return !!(_emscripten_bind_BoolVector_get_1(self, i));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
BoolVector.prototype['set'] = BoolVector.prototype.set = function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_BoolVector_set_2(self, i, val);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
BoolVector.prototype['size'] = BoolVector.prototype.size = function() {
  var self = this.ptr;
  return _emscripten_bind_BoolVector_size_0(self);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
BoolVector.prototype['__destroy__'] = BoolVector.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_BoolVector___destroy___0(self);
};

// Interface: CharVector

/** @suppress {undefinedVars, duplicate} @this{Object} */
function CharVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_CharVector_CharVector_0(); getCache(CharVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_CharVector_CharVector_1(size);
  getCache(CharVector)[this.ptr] = this;
};

CharVector.prototype = Object.create(WrapperObject.prototype);
CharVector.prototype.constructor = CharVector;
CharVector.prototype.__class__ = CharVector;
CharVector.__cache__ = {};
Module['CharVector'] = CharVector;
/** @suppress {undefinedVars, duplicate} @this{Object} */
CharVector.prototype['resize'] = CharVector.prototype.resize = function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_CharVector_resize_1(self, size);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
CharVector.prototype['get'] = CharVector.prototype.get = function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return _emscripten_bind_CharVector_get_1(self, i);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
CharVector.prototype['set'] = CharVector.prototype.set = function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_CharVector_set_2(self, i, val);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
CharVector.prototype['size'] = CharVector.prototype.size = function() {
  var self = this.ptr;
  return _emscripten_bind_CharVector_size_0(self);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
CharVector.prototype['__destroy__'] = CharVector.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_CharVector___destroy___0(self);
};

// Interface: IntVector

/** @suppress {undefinedVars, duplicate} @this{Object} */
function IntVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_IntVector_IntVector_0(); getCache(IntVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_IntVector_IntVector_1(size);
  getCache(IntVector)[this.ptr] = this;
};

IntVector.prototype = Object.create(WrapperObject.prototype);
IntVector.prototype.constructor = IntVector;
IntVector.prototype.__class__ = IntVector;
IntVector.__cache__ = {};
Module['IntVector'] = IntVector;
/** @suppress {undefinedVars, duplicate} @this{Object} */
IntVector.prototype['resize'] = IntVector.prototype.resize = function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_IntVector_resize_1(self, size);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
IntVector.prototype['get'] = IntVector.prototype.get = function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return _emscripten_bind_IntVector_get_1(self, i);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
IntVector.prototype['set'] = IntVector.prototype.set = function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_IntVector_set_2(self, i, val);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
IntVector.prototype['size'] = IntVector.prototype.size = function() {
  var self = this.ptr;
  return _emscripten_bind_IntVector_size_0(self);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
IntVector.prototype['__destroy__'] = IntVector.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_IntVector___destroy___0(self);
};

// Interface: DoubleVector

/** @suppress {undefinedVars, duplicate} @this{Object} */
function DoubleVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_DoubleVector_DoubleVector_0(); getCache(DoubleVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_DoubleVector_DoubleVector_1(size);
  getCache(DoubleVector)[this.ptr] = this;
};

DoubleVector.prototype = Object.create(WrapperObject.prototype);
DoubleVector.prototype.constructor = DoubleVector;
DoubleVector.prototype.__class__ = DoubleVector;
DoubleVector.__cache__ = {};
Module['DoubleVector'] = DoubleVector;
/** @suppress {undefinedVars, duplicate} @this{Object} */
DoubleVector.prototype['resize'] = DoubleVector.prototype.resize = function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_DoubleVector_resize_1(self, size);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
DoubleVector.prototype['get'] = DoubleVector.prototype.get = function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return _emscripten_bind_DoubleVector_get_1(self, i);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
DoubleVector.prototype['set'] = DoubleVector.prototype.set = function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_DoubleVector_set_2(self, i, val);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
DoubleVector.prototype['size'] = DoubleVector.prototype.size = function() {
  var self = this.ptr;
  return _emscripten_bind_DoubleVector_size_0(self);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
DoubleVector.prototype['__destroy__'] = DoubleVector.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_DoubleVector___destroy___0(self);
};

// Interface: SpeciesMasterTableRecordVector

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SpeciesMasterTableRecordVector(size) {
  if (size && typeof size === 'object') size = size.ptr;
  if (size === undefined) { this.ptr = _emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_0(); getCache(SpeciesMasterTableRecordVector)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_1(size);
  getCache(SpeciesMasterTableRecordVector)[this.ptr] = this;
};

SpeciesMasterTableRecordVector.prototype = Object.create(WrapperObject.prototype);
SpeciesMasterTableRecordVector.prototype.constructor = SpeciesMasterTableRecordVector;
SpeciesMasterTableRecordVector.prototype.__class__ = SpeciesMasterTableRecordVector;
SpeciesMasterTableRecordVector.__cache__ = {};
Module['SpeciesMasterTableRecordVector'] = SpeciesMasterTableRecordVector;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTableRecordVector.prototype['resize'] = SpeciesMasterTableRecordVector.prototype.resize = function(size) {
  var self = this.ptr;
  if (size && typeof size === 'object') size = size.ptr;
  _emscripten_bind_SpeciesMasterTableRecordVector_resize_1(self, size);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTableRecordVector.prototype['get'] = SpeciesMasterTableRecordVector.prototype.get = function(i) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  return wrapPointer(_emscripten_bind_SpeciesMasterTableRecordVector_get_1(self, i), SpeciesMasterTableRecord);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTableRecordVector.prototype['set'] = SpeciesMasterTableRecordVector.prototype.set = function(i, val) {
  var self = this.ptr;
  if (i && typeof i === 'object') i = i.ptr;
  if (val && typeof val === 'object') val = val.ptr;
  _emscripten_bind_SpeciesMasterTableRecordVector_set_2(self, i, val);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTableRecordVector.prototype['size'] = SpeciesMasterTableRecordVector.prototype.size = function() {
  var self = this.ptr;
  return _emscripten_bind_SpeciesMasterTableRecordVector_size_0(self);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTableRecordVector.prototype['__destroy__'] = SpeciesMasterTableRecordVector.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SpeciesMasterTableRecordVector___destroy___0(self);
};

// Interface: AreaUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function AreaUnits() { throw "cannot construct a AreaUnits, no constructor in IDL" }
AreaUnits.prototype = Object.create(WrapperObject.prototype);
AreaUnits.prototype.constructor = AreaUnits;
AreaUnits.prototype.__class__ = AreaUnits;
AreaUnits.__cache__ = {};
Module['AreaUnits'] = AreaUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
AreaUnits.prototype['toBaseUnits'] = AreaUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_AreaUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
AreaUnits.prototype['fromBaseUnits'] = AreaUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_AreaUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
AreaUnits.prototype['__destroy__'] = AreaUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_AreaUnits___destroy___0(self);
};

// Interface: BasalAreaUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function BasalAreaUnits() { throw "cannot construct a BasalAreaUnits, no constructor in IDL" }
BasalAreaUnits.prototype = Object.create(WrapperObject.prototype);
BasalAreaUnits.prototype.constructor = BasalAreaUnits;
BasalAreaUnits.prototype.__class__ = BasalAreaUnits;
BasalAreaUnits.__cache__ = {};
Module['BasalAreaUnits'] = BasalAreaUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
BasalAreaUnits.prototype['toBaseUnits'] = BasalAreaUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_BasalAreaUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
BasalAreaUnits.prototype['fromBaseUnits'] = BasalAreaUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_BasalAreaUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
BasalAreaUnits.prototype['__destroy__'] = BasalAreaUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_BasalAreaUnits___destroy___0(self);
};

// Interface: FractionUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function FractionUnits() { throw "cannot construct a FractionUnits, no constructor in IDL" }
FractionUnits.prototype = Object.create(WrapperObject.prototype);
FractionUnits.prototype.constructor = FractionUnits;
FractionUnits.prototype.__class__ = FractionUnits;
FractionUnits.__cache__ = {};
Module['FractionUnits'] = FractionUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
FractionUnits.prototype['toBaseUnits'] = FractionUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_FractionUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FractionUnits.prototype['fromBaseUnits'] = FractionUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_FractionUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
FractionUnits.prototype['__destroy__'] = FractionUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_FractionUnits___destroy___0(self);
};

// Interface: LengthUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function LengthUnits() { throw "cannot construct a LengthUnits, no constructor in IDL" }
LengthUnits.prototype = Object.create(WrapperObject.prototype);
LengthUnits.prototype.constructor = LengthUnits;
LengthUnits.prototype.__class__ = LengthUnits;
LengthUnits.__cache__ = {};
Module['LengthUnits'] = LengthUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
LengthUnits.prototype['toBaseUnits'] = LengthUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_LengthUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
LengthUnits.prototype['fromBaseUnits'] = LengthUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_LengthUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
LengthUnits.prototype['__destroy__'] = LengthUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_LengthUnits___destroy___0(self);
};

// Interface: LoadingUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function LoadingUnits() { throw "cannot construct a LoadingUnits, no constructor in IDL" }
LoadingUnits.prototype = Object.create(WrapperObject.prototype);
LoadingUnits.prototype.constructor = LoadingUnits;
LoadingUnits.prototype.__class__ = LoadingUnits;
LoadingUnits.__cache__ = {};
Module['LoadingUnits'] = LoadingUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
LoadingUnits.prototype['toBaseUnits'] = LoadingUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_LoadingUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
LoadingUnits.prototype['fromBaseUnits'] = LoadingUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_LoadingUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
LoadingUnits.prototype['__destroy__'] = LoadingUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_LoadingUnits___destroy___0(self);
};

// Interface: SurfaceAreaToVolumeUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SurfaceAreaToVolumeUnits() { throw "cannot construct a SurfaceAreaToVolumeUnits, no constructor in IDL" }
SurfaceAreaToVolumeUnits.prototype = Object.create(WrapperObject.prototype);
SurfaceAreaToVolumeUnits.prototype.constructor = SurfaceAreaToVolumeUnits;
SurfaceAreaToVolumeUnits.prototype.__class__ = SurfaceAreaToVolumeUnits;
SurfaceAreaToVolumeUnits.__cache__ = {};
Module['SurfaceAreaToVolumeUnits'] = SurfaceAreaToVolumeUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SurfaceAreaToVolumeUnits.prototype['toBaseUnits'] = SurfaceAreaToVolumeUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SurfaceAreaToVolumeUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SurfaceAreaToVolumeUnits.prototype['fromBaseUnits'] = SurfaceAreaToVolumeUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SurfaceAreaToVolumeUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SurfaceAreaToVolumeUnits.prototype['__destroy__'] = SurfaceAreaToVolumeUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SurfaceAreaToVolumeUnits___destroy___0(self);
};

// Interface: SpeedUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SpeedUnits() { throw "cannot construct a SpeedUnits, no constructor in IDL" }
SpeedUnits.prototype = Object.create(WrapperObject.prototype);
SpeedUnits.prototype.constructor = SpeedUnits;
SpeedUnits.prototype.__class__ = SpeedUnits;
SpeedUnits.__cache__ = {};
Module['SpeedUnits'] = SpeedUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeedUnits.prototype['toBaseUnits'] = SpeedUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SpeedUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeedUnits.prototype['fromBaseUnits'] = SpeedUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SpeedUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeedUnits.prototype['__destroy__'] = SpeedUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SpeedUnits___destroy___0(self);
};

// Interface: PressureUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function PressureUnits() { throw "cannot construct a PressureUnits, no constructor in IDL" }
PressureUnits.prototype = Object.create(WrapperObject.prototype);
PressureUnits.prototype.constructor = PressureUnits;
PressureUnits.prototype.__class__ = PressureUnits;
PressureUnits.__cache__ = {};
Module['PressureUnits'] = PressureUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
PressureUnits.prototype['toBaseUnits'] = PressureUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_PressureUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PressureUnits.prototype['fromBaseUnits'] = PressureUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_PressureUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
PressureUnits.prototype['__destroy__'] = PressureUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_PressureUnits___destroy___0(self);
};

// Interface: SlopeUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SlopeUnits() { throw "cannot construct a SlopeUnits, no constructor in IDL" }
SlopeUnits.prototype = Object.create(WrapperObject.prototype);
SlopeUnits.prototype.constructor = SlopeUnits;
SlopeUnits.prototype.__class__ = SlopeUnits;
SlopeUnits.__cache__ = {};
Module['SlopeUnits'] = SlopeUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SlopeUnits.prototype['toBaseUnits'] = SlopeUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SlopeUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SlopeUnits.prototype['fromBaseUnits'] = SlopeUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SlopeUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SlopeUnits.prototype['__destroy__'] = SlopeUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SlopeUnits___destroy___0(self);
};

// Interface: DensityUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function DensityUnits() { throw "cannot construct a DensityUnits, no constructor in IDL" }
DensityUnits.prototype = Object.create(WrapperObject.prototype);
DensityUnits.prototype.constructor = DensityUnits;
DensityUnits.prototype.__class__ = DensityUnits;
DensityUnits.__cache__ = {};
Module['DensityUnits'] = DensityUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
DensityUnits.prototype['toBaseUnits'] = DensityUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_DensityUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
DensityUnits.prototype['fromBaseUnits'] = DensityUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_DensityUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
DensityUnits.prototype['__destroy__'] = DensityUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_DensityUnits___destroy___0(self);
};

// Interface: HeatOfCombustionUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function HeatOfCombustionUnits() { throw "cannot construct a HeatOfCombustionUnits, no constructor in IDL" }
HeatOfCombustionUnits.prototype = Object.create(WrapperObject.prototype);
HeatOfCombustionUnits.prototype.constructor = HeatOfCombustionUnits;
HeatOfCombustionUnits.prototype.__class__ = HeatOfCombustionUnits;
HeatOfCombustionUnits.__cache__ = {};
Module['HeatOfCombustionUnits'] = HeatOfCombustionUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatOfCombustionUnits.prototype['toBaseUnits'] = HeatOfCombustionUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatOfCombustionUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatOfCombustionUnits.prototype['fromBaseUnits'] = HeatOfCombustionUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatOfCombustionUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatOfCombustionUnits.prototype['__destroy__'] = HeatOfCombustionUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_HeatOfCombustionUnits___destroy___0(self);
};

// Interface: HeatSinkUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function HeatSinkUnits() { throw "cannot construct a HeatSinkUnits, no constructor in IDL" }
HeatSinkUnits.prototype = Object.create(WrapperObject.prototype);
HeatSinkUnits.prototype.constructor = HeatSinkUnits;
HeatSinkUnits.prototype.__class__ = HeatSinkUnits;
HeatSinkUnits.__cache__ = {};
Module['HeatSinkUnits'] = HeatSinkUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatSinkUnits.prototype['toBaseUnits'] = HeatSinkUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatSinkUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatSinkUnits.prototype['fromBaseUnits'] = HeatSinkUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatSinkUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatSinkUnits.prototype['__destroy__'] = HeatSinkUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_HeatSinkUnits___destroy___0(self);
};

// Interface: HeatPerUnitAreaUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function HeatPerUnitAreaUnits() { throw "cannot construct a HeatPerUnitAreaUnits, no constructor in IDL" }
HeatPerUnitAreaUnits.prototype = Object.create(WrapperObject.prototype);
HeatPerUnitAreaUnits.prototype.constructor = HeatPerUnitAreaUnits;
HeatPerUnitAreaUnits.prototype.__class__ = HeatPerUnitAreaUnits;
HeatPerUnitAreaUnits.__cache__ = {};
Module['HeatPerUnitAreaUnits'] = HeatPerUnitAreaUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatPerUnitAreaUnits.prototype['toBaseUnits'] = HeatPerUnitAreaUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatPerUnitAreaUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatPerUnitAreaUnits.prototype['fromBaseUnits'] = HeatPerUnitAreaUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatPerUnitAreaUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatPerUnitAreaUnits.prototype['__destroy__'] = HeatPerUnitAreaUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_HeatPerUnitAreaUnits___destroy___0(self);
};

// Interface: HeatSourceAndReactionIntensityUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function HeatSourceAndReactionIntensityUnits() { throw "cannot construct a HeatSourceAndReactionIntensityUnits, no constructor in IDL" }
HeatSourceAndReactionIntensityUnits.prototype = Object.create(WrapperObject.prototype);
HeatSourceAndReactionIntensityUnits.prototype.constructor = HeatSourceAndReactionIntensityUnits;
HeatSourceAndReactionIntensityUnits.prototype.__class__ = HeatSourceAndReactionIntensityUnits;
HeatSourceAndReactionIntensityUnits.__cache__ = {};
Module['HeatSourceAndReactionIntensityUnits'] = HeatSourceAndReactionIntensityUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatSourceAndReactionIntensityUnits.prototype['toBaseUnits'] = HeatSourceAndReactionIntensityUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatSourceAndReactionIntensityUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatSourceAndReactionIntensityUnits.prototype['fromBaseUnits'] = HeatSourceAndReactionIntensityUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatSourceAndReactionIntensityUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
HeatSourceAndReactionIntensityUnits.prototype['__destroy__'] = HeatSourceAndReactionIntensityUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_HeatSourceAndReactionIntensityUnits___destroy___0(self);
};

// Interface: FirelineIntensityUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function FirelineIntensityUnits() { throw "cannot construct a FirelineIntensityUnits, no constructor in IDL" }
FirelineIntensityUnits.prototype = Object.create(WrapperObject.prototype);
FirelineIntensityUnits.prototype.constructor = FirelineIntensityUnits;
FirelineIntensityUnits.prototype.__class__ = FirelineIntensityUnits;
FirelineIntensityUnits.__cache__ = {};
Module['FirelineIntensityUnits'] = FirelineIntensityUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
FirelineIntensityUnits.prototype['toBaseUnits'] = FirelineIntensityUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_FirelineIntensityUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FirelineIntensityUnits.prototype['fromBaseUnits'] = FirelineIntensityUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_FirelineIntensityUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
FirelineIntensityUnits.prototype['__destroy__'] = FirelineIntensityUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_FirelineIntensityUnits___destroy___0(self);
};

// Interface: TemperatureUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function TemperatureUnits() { throw "cannot construct a TemperatureUnits, no constructor in IDL" }
TemperatureUnits.prototype = Object.create(WrapperObject.prototype);
TemperatureUnits.prototype.constructor = TemperatureUnits;
TemperatureUnits.prototype.__class__ = TemperatureUnits;
TemperatureUnits.__cache__ = {};
Module['TemperatureUnits'] = TemperatureUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
TemperatureUnits.prototype['toBaseUnits'] = TemperatureUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_TemperatureUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
TemperatureUnits.prototype['fromBaseUnits'] = TemperatureUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_TemperatureUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
TemperatureUnits.prototype['__destroy__'] = TemperatureUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_TemperatureUnits___destroy___0(self);
};

// Interface: TimeUnits

/** @suppress {undefinedVars, duplicate} @this{Object} */
function TimeUnits() { throw "cannot construct a TimeUnits, no constructor in IDL" }
TimeUnits.prototype = Object.create(WrapperObject.prototype);
TimeUnits.prototype.constructor = TimeUnits;
TimeUnits.prototype.__class__ = TimeUnits;
TimeUnits.__cache__ = {};
Module['TimeUnits'] = TimeUnits;
/** @suppress {undefinedVars, duplicate} @this{Object} */
TimeUnits.prototype['toBaseUnits'] = TimeUnits.prototype.toBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_TimeUnits_toBaseUnits_2(value, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
TimeUnits.prototype['fromBaseUnits'] = TimeUnits.prototype.fromBaseUnits = function(value, units) {
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_TimeUnits_fromBaseUnits_2(value, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
TimeUnits.prototype['__destroy__'] = TimeUnits.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_TimeUnits___destroy___0(self);
};

// Interface: FireSize

/** @suppress {undefinedVars, duplicate} @this{Object} */
function FireSize() { throw "cannot construct a FireSize, no constructor in IDL" }
FireSize.prototype = Object.create(WrapperObject.prototype);
FireSize.prototype.constructor = FireSize;
FireSize.prototype.__class__ = FireSize;
FireSize.__cache__ = {};
Module['FireSize'] = FireSize;
/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getBackingSpreadRate'] = FireSize.prototype.getBackingSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_FireSize_getBackingSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getEccentricity'] = FireSize.prototype.getEccentricity = function() {
  var self = this.ptr;
  return _emscripten_bind_FireSize_getEccentricity_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getEllipticalA'] = FireSize.prototype.getEllipticalA = function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getEllipticalA_3(self, lengthUnits, elapsedTime, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getEllipticalB'] = FireSize.prototype.getEllipticalB = function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getEllipticalB_3(self, lengthUnits, elapsedTime, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getEllipticalC'] = FireSize.prototype.getEllipticalC = function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getEllipticalC_3(self, lengthUnits, elapsedTime, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getFireArea'] = FireSize.prototype.getFireArea = function(isCrown, areaUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (isCrown && typeof isCrown === 'object') isCrown = isCrown.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFireArea_4(self, isCrown, areaUnits, elapsedTime, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getFireLength'] = FireSize.prototype.getFireLength = function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFireLength_3(self, lengthUnits, elapsedTime, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getFireLengthToWidthRatio'] = FireSize.prototype.getFireLengthToWidthRatio = function() {
  var self = this.ptr;
  return _emscripten_bind_FireSize_getFireLengthToWidthRatio_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getFirePerimeter'] = FireSize.prototype.getFirePerimeter = function(isCrown, lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (isCrown && typeof isCrown === 'object') isCrown = isCrown.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFirePerimeter_4(self, isCrown, lengthUnits, elapsedTime, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getFlankingSpreadRate'] = FireSize.prototype.getFlankingSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_FireSize_getFlankingSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getHeadingToBackingRatio'] = FireSize.prototype.getHeadingToBackingRatio = function() {
  var self = this.ptr;
  return _emscripten_bind_FireSize_getHeadingToBackingRatio_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['getMaxFireWidth'] = FireSize.prototype.getMaxFireWidth = function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getMaxFireWidth_3(self, lengthUnits, elapsedTime, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['calculateFireBasicDimensions'] = FireSize.prototype.calculateFireBasicDimensions = function(isCrown, effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits) {
  var self = this.ptr;
  if (isCrown && typeof isCrown === 'object') isCrown = isCrown.ptr;
  if (effectiveWindSpeed && typeof effectiveWindSpeed === 'object') effectiveWindSpeed = effectiveWindSpeed.ptr;
  if (windSpeedRateUnits && typeof windSpeedRateUnits === 'object') windSpeedRateUnits = windSpeedRateUnits.ptr;
  if (forwardSpreadRate && typeof forwardSpreadRate === 'object') forwardSpreadRate = forwardSpreadRate.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  _emscripten_bind_FireSize_calculateFireBasicDimensions_5(self, isCrown, effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
FireSize.prototype['__destroy__'] = FireSize.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_FireSize___destroy___0(self);
};

// Interface: SIGContainAdapter

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGContainAdapter() {
  this.ptr = _emscripten_bind_SIGContainAdapter_SIGContainAdapter_0();
  getCache(SIGContainAdapter)[this.ptr] = this;
};

SIGContainAdapter.prototype = Object.create(WrapperObject.prototype);
SIGContainAdapter.prototype.constructor = SIGContainAdapter;
SIGContainAdapter.prototype.__class__ = SIGContainAdapter;
SIGContainAdapter.__cache__ = {};
Module['SIGContainAdapter'] = SIGContainAdapter;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getContainmentStatus'] = SIGContainAdapter.prototype.getContainmentStatus = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getContainmentStatus_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFirePerimeterX'] = SIGContainAdapter.prototype.getFirePerimeterX = function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGContainAdapter_getFirePerimeterX_0(self), DoubleVector);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFirePerimeterY'] = SIGContainAdapter.prototype.getFirePerimeterY = function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGContainAdapter_getFirePerimeterY_0(self), DoubleVector);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getAttackDistance'] = SIGContainAdapter.prototype.getAttackDistance = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getAttackDistance_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFinalContainmentArea'] = SIGContainAdapter.prototype.getFinalContainmentArea = function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalContainmentArea_1(self, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFinalCost'] = SIGContainAdapter.prototype.getFinalCost = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalCost_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFinalFireLineLength'] = SIGContainAdapter.prototype.getFinalFireLineLength = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalFireLineLength_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFinalFireSize'] = SIGContainAdapter.prototype.getFinalFireSize = function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalFireSize_1(self, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFinalTimeSinceReport'] = SIGContainAdapter.prototype.getFinalTimeSinceReport = function(timeUnits) {
  var self = this.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalTimeSinceReport_1(self, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFireBackAtAttack'] = SIGContainAdapter.prototype.getFireBackAtAttack = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireBackAtAttack_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFireBackAtReport'] = SIGContainAdapter.prototype.getFireBackAtReport = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireBackAtReport_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFireHeadAtAttack'] = SIGContainAdapter.prototype.getFireHeadAtAttack = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireHeadAtAttack_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFireHeadAtReport'] = SIGContainAdapter.prototype.getFireHeadAtReport = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireHeadAtReport_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFireSizeAtInitialAttack'] = SIGContainAdapter.prototype.getFireSizeAtInitialAttack = function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireSizeAtInitialAttack_1(self, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getLengthToWidthRatio'] = SIGContainAdapter.prototype.getLengthToWidthRatio = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getLengthToWidthRatio_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getPerimeterAtContainment'] = SIGContainAdapter.prototype.getPerimeterAtContainment = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getPerimeterAtContainment_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getPerimeterAtInitialAttack'] = SIGContainAdapter.prototype.getPerimeterAtInitialAttack = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getPerimeterAtInitialAttack_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getReportSize'] = SIGContainAdapter.prototype.getReportSize = function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getReportSize_1(self, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getReportRate'] = SIGContainAdapter.prototype.getReportRate = function(speedUnits) {
  var self = this.ptr;
  if (speedUnits && typeof speedUnits === 'object') speedUnits = speedUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getReportRate_1(self, speedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getTactic'] = SIGContainAdapter.prototype.getTactic = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getTactic_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['getFirePerimeterPointCount'] = SIGContainAdapter.prototype.getFirePerimeterPointCount = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFirePerimeterPointCount_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['removeAllResourcesWithThisDesc'] = SIGContainAdapter.prototype.removeAllResourcesWithThisDesc = function(desc) {
  var self = this.ptr;
  ensureCache.prepare();
  if (desc && typeof desc === 'object') desc = desc.ptr;
  else desc = ensureString(desc);
  return _emscripten_bind_SIGContainAdapter_removeAllResourcesWithThisDesc_1(self, desc);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['removeResourceAt'] = SIGContainAdapter.prototype.removeResourceAt = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGContainAdapter_removeResourceAt_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['removeResourceWithThisDesc'] = SIGContainAdapter.prototype.removeResourceWithThisDesc = function(desc) {
  var self = this.ptr;
  ensureCache.prepare();
  if (desc && typeof desc === 'object') desc = desc.ptr;
  else desc = ensureString(desc);
  return _emscripten_bind_SIGContainAdapter_removeResourceWithThisDesc_1(self, desc);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['addResource'] = SIGContainAdapter.prototype.addResource = function(arrival, duration, timeUnit, productionRate, productionRateUnits, description, baseCost, hourCost) {
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
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['doContainRun'] = SIGContainAdapter.prototype.doContainRun = function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainAdapter_doContainRun_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['removeAllResources'] = SIGContainAdapter.prototype.removeAllResources = function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainAdapter_removeAllResources_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setAttackDistance'] = SIGContainAdapter.prototype.setAttackDistance = function(attackDistance, lengthUnits) {
  var self = this.ptr;
  if (attackDistance && typeof attackDistance === 'object') attackDistance = attackDistance.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGContainAdapter_setAttackDistance_2(self, attackDistance, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setFireStartTime'] = SIGContainAdapter.prototype.setFireStartTime = function(fireStartTime) {
  var self = this.ptr;
  if (fireStartTime && typeof fireStartTime === 'object') fireStartTime = fireStartTime.ptr;
  _emscripten_bind_SIGContainAdapter_setFireStartTime_1(self, fireStartTime);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setLwRatio'] = SIGContainAdapter.prototype.setLwRatio = function(lwRatio) {
  var self = this.ptr;
  if (lwRatio && typeof lwRatio === 'object') lwRatio = lwRatio.ptr;
  _emscripten_bind_SIGContainAdapter_setLwRatio_1(self, lwRatio);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setMaxFireSize'] = SIGContainAdapter.prototype.setMaxFireSize = function(maxFireSize) {
  var self = this.ptr;
  if (maxFireSize && typeof maxFireSize === 'object') maxFireSize = maxFireSize.ptr;
  _emscripten_bind_SIGContainAdapter_setMaxFireSize_1(self, maxFireSize);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setMaxFireTime'] = SIGContainAdapter.prototype.setMaxFireTime = function(maxFireTime) {
  var self = this.ptr;
  if (maxFireTime && typeof maxFireTime === 'object') maxFireTime = maxFireTime.ptr;
  _emscripten_bind_SIGContainAdapter_setMaxFireTime_1(self, maxFireTime);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setMaxSteps'] = SIGContainAdapter.prototype.setMaxSteps = function(maxSteps) {
  var self = this.ptr;
  if (maxSteps && typeof maxSteps === 'object') maxSteps = maxSteps.ptr;
  _emscripten_bind_SIGContainAdapter_setMaxSteps_1(self, maxSteps);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setMinSteps'] = SIGContainAdapter.prototype.setMinSteps = function(minSteps) {
  var self = this.ptr;
  if (minSteps && typeof minSteps === 'object') minSteps = minSteps.ptr;
  _emscripten_bind_SIGContainAdapter_setMinSteps_1(self, minSteps);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setReportRate'] = SIGContainAdapter.prototype.setReportRate = function(reportRate, speedUnits) {
  var self = this.ptr;
  if (reportRate && typeof reportRate === 'object') reportRate = reportRate.ptr;
  if (speedUnits && typeof speedUnits === 'object') speedUnits = speedUnits.ptr;
  _emscripten_bind_SIGContainAdapter_setReportRate_2(self, reportRate, speedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setReportSize'] = SIGContainAdapter.prototype.setReportSize = function(reportSize, areaUnits) {
  var self = this.ptr;
  if (reportSize && typeof reportSize === 'object') reportSize = reportSize.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  _emscripten_bind_SIGContainAdapter_setReportSize_2(self, reportSize, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setRetry'] = SIGContainAdapter.prototype.setRetry = function(retry) {
  var self = this.ptr;
  if (retry && typeof retry === 'object') retry = retry.ptr;
  _emscripten_bind_SIGContainAdapter_setRetry_1(self, retry);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['setTactic'] = SIGContainAdapter.prototype.setTactic = function(tactic) {
  var self = this.ptr;
  if (tactic && typeof tactic === 'object') tactic = tactic.ptr;
  _emscripten_bind_SIGContainAdapter_setTactic_1(self, tactic);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGContainAdapter.prototype['__destroy__'] = SIGContainAdapter.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGContainAdapter___destroy___0(self);
};

// Interface: SIGIgnite

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGIgnite() {
  this.ptr = _emscripten_bind_SIGIgnite_SIGIgnite_0();
  getCache(SIGIgnite)[this.ptr] = this;
};

SIGIgnite.prototype = Object.create(WrapperObject.prototype);
SIGIgnite.prototype.constructor = SIGIgnite;
SIGIgnite.prototype.__class__ = SIGIgnite;
SIGIgnite.__cache__ = {};
Module['SIGIgnite'] = SIGIgnite;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['initializeMembers'] = SIGIgnite.prototype.initializeMembers = function() {
  var self = this.ptr;
  _emscripten_bind_SIGIgnite_initializeMembers_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['getFuelBedType'] = SIGIgnite.prototype.getFuelBedType = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGIgnite_getFuelBedType_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['getLightningChargeType'] = SIGIgnite.prototype.getLightningChargeType = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGIgnite_getLightningChargeType_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['calculateFirebrandIgnitionProbability'] = SIGIgnite.prototype.calculateFirebrandIgnitionProbability = function() {
  var self = this.ptr;
  _emscripten_bind_SIGIgnite_calculateFirebrandIgnitionProbability_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['calculateLightningIgnitionProbability'] = SIGIgnite.prototype.calculateLightningIgnitionProbability = function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_calculateLightningIgnitionProbability_1(self, desiredUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['setAirTemperature'] = SIGIgnite.prototype.setAirTemperature = function(airTemperature, temperatureUnites) {
  var self = this.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnites && typeof temperatureUnites === 'object') temperatureUnites = temperatureUnites.ptr;
  _emscripten_bind_SIGIgnite_setAirTemperature_2(self, airTemperature, temperatureUnites);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['setDuffDepth'] = SIGIgnite.prototype.setDuffDepth = function(duffDepth, lengthUnits) {
  var self = this.ptr;
  if (duffDepth && typeof duffDepth === 'object') duffDepth = duffDepth.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGIgnite_setDuffDepth_2(self, duffDepth, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['setIgnitionFuelBedType'] = SIGIgnite.prototype.setIgnitionFuelBedType = function(fuelBedType_) {
  var self = this.ptr;
  if (fuelBedType_ && typeof fuelBedType_ === 'object') fuelBedType_ = fuelBedType_.ptr;
  _emscripten_bind_SIGIgnite_setIgnitionFuelBedType_1(self, fuelBedType_);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['setLightningChargeType'] = SIGIgnite.prototype.setLightningChargeType = function(lightningChargeType) {
  var self = this.ptr;
  if (lightningChargeType && typeof lightningChargeType === 'object') lightningChargeType = lightningChargeType.ptr;
  _emscripten_bind_SIGIgnite_setLightningChargeType_1(self, lightningChargeType);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['setMoistureHundredHour'] = SIGIgnite.prototype.setMoistureHundredHour = function(moistureHundredHour, moistureUnits) {
  var self = this.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGIgnite_setMoistureHundredHour_2(self, moistureHundredHour, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['setMoistureOneHour'] = SIGIgnite.prototype.setMoistureOneHour = function(moistureOneHour, moistureUnits) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGIgnite_setMoistureOneHour_2(self, moistureOneHour, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['setSunShade'] = SIGIgnite.prototype.setSunShade = function(sunShade, sunShadeUnits) {
  var self = this.ptr;
  if (sunShade && typeof sunShade === 'object') sunShade = sunShade.ptr;
  if (sunShadeUnits && typeof sunShadeUnits === 'object') sunShadeUnits = sunShadeUnits.ptr;
  _emscripten_bind_SIGIgnite_setSunShade_2(self, sunShade, sunShadeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['updateIgniteInputs'] = SIGIgnite.prototype.updateIgniteInputs = function(moistureOneHour, moistureHundredHour, moistureUnits, airTemperature, temperatureUnits, sunShade, sunShadeUnits, fuelBedType, duffDepth, duffDepthUnits, lightningChargeType) {
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
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['getAirTemperature'] = SIGIgnite.prototype.getAirTemperature = function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getAirTemperature_1(self, desiredUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['getDuffDepth'] = SIGIgnite.prototype.getDuffDepth = function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getDuffDepth_1(self, desiredUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['getFirebrandIgnitionProbability'] = SIGIgnite.prototype.getFirebrandIgnitionProbability = function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getFirebrandIgnitionProbability_1(self, desiredUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['getFuelTemperature'] = SIGIgnite.prototype.getFuelTemperature = function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getFuelTemperature_1(self, desiredUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['getMoistureHundredHour'] = SIGIgnite.prototype.getMoistureHundredHour = function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getMoistureHundredHour_1(self, desiredUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['getMoistureOneHour'] = SIGIgnite.prototype.getMoistureOneHour = function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getMoistureOneHour_1(self, desiredUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['getSunShade'] = SIGIgnite.prototype.getSunShade = function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getSunShade_1(self, desiredUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['isFuelDepthNeeded'] = SIGIgnite.prototype.isFuelDepthNeeded = function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGIgnite_isFuelDepthNeeded_0(self));
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGIgnite.prototype['__destroy__'] = SIGIgnite.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGIgnite___destroy___0(self);
};

// Interface: SIGMoistureScenarios

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGMoistureScenarios() {
  this.ptr = _emscripten_bind_SIGMoistureScenarios_SIGMoistureScenarios_0();
  getCache(SIGMoistureScenarios)[this.ptr] = this;
};

SIGMoistureScenarios.prototype = Object.create(WrapperObject.prototype);
SIGMoistureScenarios.prototype.constructor = SIGMoistureScenarios;
SIGMoistureScenarios.prototype.__class__ = SIGMoistureScenarios;
SIGMoistureScenarios.__cache__ = {};
Module['SIGMoistureScenarios'] = SIGMoistureScenarios;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getIsMoistureScenarioDefinedByIndex'] = SIGMoistureScenarios.prototype.getIsMoistureScenarioDefinedByIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return !!(_emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getIsMoistureScenarioDefinedByName'] = SIGMoistureScenarios.prototype.getIsMoistureScenarioDefinedByName = function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return !!(_emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByName_1(self, name));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioHundredHourByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioHundredHourByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioHundredHourByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioHundredHourByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioLiveHerbaceousByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveHerbaceousByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioLiveHerbaceousByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveHerbaceousByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioLiveWoodyByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveWoodyByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioLiveWoodyByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveWoodyByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioOneHourByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioOneHourByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioOneHourByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioOneHourByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioTenHourByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioTenHourByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioTenHourByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioTenHourByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioIndexByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioIndexByName = function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioIndexByName_1(self, name);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getNumberOfMoistureScenarios'] = SIGMoistureScenarios.prototype.getNumberOfMoistureScenarios = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getNumberOfMoistureScenarios_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioDescriptionByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioDescriptionByIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioDescriptionByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioDescriptionByName = function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return UTF8ToString(_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByName_1(self, name));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['getMoistureScenarioNameByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioNameByIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMoistureScenarios_getMoistureScenarioNameByIndex_1(self, index));
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMoistureScenarios.prototype['__destroy__'] = SIGMoistureScenarios.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGMoistureScenarios___destroy___0(self);
};

// Interface: SIGSpot

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGSpot() {
  this.ptr = _emscripten_bind_SIGSpot_SIGSpot_0();
  getCache(SIGSpot)[this.ptr] = this;
};

SIGSpot.prototype = Object.create(WrapperObject.prototype);
SIGSpot.prototype.constructor = SIGSpot;
SIGSpot.prototype.__class__ = SIGSpot;
SIGSpot.__cache__ = {};
Module['SIGSpot'] = SIGSpot;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getDownwindCanopyMode'] = SIGSpot.prototype.getDownwindCanopyMode = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getDownwindCanopyMode_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getLocation'] = SIGSpot.prototype.getLocation = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getLocation_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getTreeSpecies'] = SIGSpot.prototype.getTreeSpecies = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getTreeSpecies_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getBurningPileFlameHeight'] = SIGSpot.prototype.getBurningPileFlameHeight = function(flameHeightUnits) {
  var self = this.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getBurningPileFlameHeight_1(self, flameHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getCoverHeightUsedForBurningPile'] = SIGSpot.prototype.getCoverHeightUsedForBurningPile = function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getCoverHeightUsedForBurningPile_1(self, coverHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getCoverHeightUsedForSurfaceFire'] = SIGSpot.prototype.getCoverHeightUsedForSurfaceFire = function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getCoverHeightUsedForSurfaceFire_1(self, coverHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getCoverHeightUsedForTorchingTrees'] = SIGSpot.prototype.getCoverHeightUsedForTorchingTrees = function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getCoverHeightUsedForTorchingTrees_1(self, coverHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getDBH'] = SIGSpot.prototype.getDBH = function(DBHUnits) {
  var self = this.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  return _emscripten_bind_SIGSpot_getDBH_1(self, DBHUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getDownwindCoverHeight'] = SIGSpot.prototype.getDownwindCoverHeight = function(coverHeightUnits) {
  var self = this.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getDownwindCoverHeight_1(self, coverHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getFlameDurationForTorchingTrees'] = SIGSpot.prototype.getFlameDurationForTorchingTrees = function(durationUnits) {
  var self = this.ptr;
  if (durationUnits && typeof durationUnits === 'object') durationUnits = durationUnits.ptr;
  return _emscripten_bind_SIGSpot_getFlameDurationForTorchingTrees_1(self, durationUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getFlameHeightForTorchingTrees'] = SIGSpot.prototype.getFlameHeightForTorchingTrees = function(flameHeightUnits) {
  var self = this.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getFlameHeightForTorchingTrees_1(self, flameHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getFlameRatioForTorchingTrees'] = SIGSpot.prototype.getFlameRatioForTorchingTrees = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getFlameRatioForTorchingTrees_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxFirebrandHeightFromBurningPile'] = SIGSpot.prototype.getMaxFirebrandHeightFromBurningPile = function(firebrandHeightUnits) {
  var self = this.ptr;
  if (firebrandHeightUnits && typeof firebrandHeightUnits === 'object') firebrandHeightUnits = firebrandHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFirebrandHeightFromBurningPile_1(self, firebrandHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxFirebrandHeightFromSurfaceFire'] = SIGSpot.prototype.getMaxFirebrandHeightFromSurfaceFire = function(firebrandHeightUnits) {
  var self = this.ptr;
  if (firebrandHeightUnits && typeof firebrandHeightUnits === 'object') firebrandHeightUnits = firebrandHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFirebrandHeightFromSurfaceFire_1(self, firebrandHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxFirebrandHeightFromTorchingTrees'] = SIGSpot.prototype.getMaxFirebrandHeightFromTorchingTrees = function(firebrandHeightUnits) {
  var self = this.ptr;
  if (firebrandHeightUnits && typeof firebrandHeightUnits === 'object') firebrandHeightUnits = firebrandHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFirebrandHeightFromTorchingTrees_1(self, firebrandHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxFlatTerrainSpottingDistanceFromBurningPile'] = SIGSpot.prototype.getMaxFlatTerrainSpottingDistanceFromBurningPile = function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1(self, spottingDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxFlatTerrainSpottingDistanceFromSurfaceFire'] = SIGSpot.prototype.getMaxFlatTerrainSpottingDistanceFromSurfaceFire = function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1(self, spottingDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxFlatTerrainSpottingDistanceFromTorchingTrees'] = SIGSpot.prototype.getMaxFlatTerrainSpottingDistanceFromTorchingTrees = function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1(self, spottingDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxMountainousTerrainSpottingDistanceFromBurningPile'] = SIGSpot.prototype.getMaxMountainousTerrainSpottingDistanceFromBurningPile = function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1(self, spottingDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxMountainousTerrainSpottingDistanceFromSurfaceFire'] = SIGSpot.prototype.getMaxMountainousTerrainSpottingDistanceFromSurfaceFire = function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1(self, spottingDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxMountainousTerrainSpottingDistanceFromTorchingTrees'] = SIGSpot.prototype.getMaxMountainousTerrainSpottingDistanceFromTorchingTrees = function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1(self, spottingDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getMaxMountainousTerrainSpottingDistanceFromActiveCrown'] = SIGSpot.prototype.getMaxMountainousTerrainSpottingDistanceFromActiveCrown = function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromActiveCrown_1(self, spottingDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getRidgeToValleyDistance'] = SIGSpot.prototype.getRidgeToValleyDistance = function(ridgeToValleyDistanceUnits) {
  var self = this.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getRidgeToValleyDistance_1(self, ridgeToValleyDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getRidgeToValleyElevation'] = SIGSpot.prototype.getRidgeToValleyElevation = function(elevationUnits) {
  var self = this.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  return _emscripten_bind_SIGSpot_getRidgeToValleyElevation_1(self, elevationUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getSurfaceFlameLength'] = SIGSpot.prototype.getSurfaceFlameLength = function(surfaceFlameLengthUnits) {
  var self = this.ptr;
  if (surfaceFlameLengthUnits && typeof surfaceFlameLengthUnits === 'object') surfaceFlameLengthUnits = surfaceFlameLengthUnits.ptr;
  return _emscripten_bind_SIGSpot_getSurfaceFlameLength_1(self, surfaceFlameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getTreeHeight'] = SIGSpot.prototype.getTreeHeight = function(treeHeightUnits) {
  var self = this.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  return _emscripten_bind_SIGSpot_getTreeHeight_1(self, treeHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getWindSpeedAtTwentyFeet'] = SIGSpot.prototype.getWindSpeedAtTwentyFeet = function(windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  return _emscripten_bind_SIGSpot_getWindSpeedAtTwentyFeet_1(self, windSpeedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['getTorchingTrees'] = SIGSpot.prototype.getTorchingTrees = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getTorchingTrees_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['calculateAll'] = SIGSpot.prototype.calculateAll = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_calculateAll_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['calculateSpottingDistanceFromBurningPile'] = SIGSpot.prototype.calculateSpottingDistanceFromBurningPile = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_calculateSpottingDistanceFromBurningPile_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['calculateSpottingDistanceFromSurfaceFire'] = SIGSpot.prototype.calculateSpottingDistanceFromSurfaceFire = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_calculateSpottingDistanceFromSurfaceFire_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['calculateSpottingDistanceFromTorchingTrees'] = SIGSpot.prototype.calculateSpottingDistanceFromTorchingTrees = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_calculateSpottingDistanceFromTorchingTrees_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['initializeMembers'] = SIGSpot.prototype.initializeMembers = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_initializeMembers_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setActiveCrownFlameLength'] = SIGSpot.prototype.setActiveCrownFlameLength = function(flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGSpot_setActiveCrownFlameLength_2(self, flameLength, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setBurningPileFlameHeight'] = SIGSpot.prototype.setBurningPileFlameHeight = function(buringPileflameHeight, flameHeightUnits) {
  var self = this.ptr;
  if (buringPileflameHeight && typeof buringPileflameHeight === 'object') buringPileflameHeight = buringPileflameHeight.ptr;
  if (flameHeightUnits && typeof flameHeightUnits === 'object') flameHeightUnits = flameHeightUnits.ptr;
  _emscripten_bind_SIGSpot_setBurningPileFlameHeight_2(self, buringPileflameHeight, flameHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setDBH'] = SIGSpot.prototype.setDBH = function(DBH, DBHUnits) {
  var self = this.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  if (DBHUnits && typeof DBHUnits === 'object') DBHUnits = DBHUnits.ptr;
  _emscripten_bind_SIGSpot_setDBH_2(self, DBH, DBHUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setDownwindCanopyMode'] = SIGSpot.prototype.setDownwindCanopyMode = function(downwindCanopyMode) {
  var self = this.ptr;
  if (downwindCanopyMode && typeof downwindCanopyMode === 'object') downwindCanopyMode = downwindCanopyMode.ptr;
  _emscripten_bind_SIGSpot_setDownwindCanopyMode_1(self, downwindCanopyMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setDownwindCoverHeight'] = SIGSpot.prototype.setDownwindCoverHeight = function(downwindCoverHeight, coverHeightUnits) {
  var self = this.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  _emscripten_bind_SIGSpot_setDownwindCoverHeight_2(self, downwindCoverHeight, coverHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setFireType'] = SIGSpot.prototype.setFireType = function(fireType) {
  var self = this.ptr;
  if (fireType && typeof fireType === 'object') fireType = fireType.ptr;
  _emscripten_bind_SIGSpot_setFireType_1(self, fireType);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setFlameLength'] = SIGSpot.prototype.setFlameLength = function(flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGSpot_setFlameLength_2(self, flameLength, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setLocation'] = SIGSpot.prototype.setLocation = function(location) {
  var self = this.ptr;
  if (location && typeof location === 'object') location = location.ptr;
  _emscripten_bind_SIGSpot_setLocation_1(self, location);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setRidgeToValleyDistance'] = SIGSpot.prototype.setRidgeToValleyDistance = function(ridgeToValleyDistance, ridgeToValleyDistanceUnits) {
  var self = this.ptr;
  if (ridgeToValleyDistance && typeof ridgeToValleyDistance === 'object') ridgeToValleyDistance = ridgeToValleyDistance.ptr;
  if (ridgeToValleyDistanceUnits && typeof ridgeToValleyDistanceUnits === 'object') ridgeToValleyDistanceUnits = ridgeToValleyDistanceUnits.ptr;
  _emscripten_bind_SIGSpot_setRidgeToValleyDistance_2(self, ridgeToValleyDistance, ridgeToValleyDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setRidgeToValleyElevation'] = SIGSpot.prototype.setRidgeToValleyElevation = function(ridgeToValleyElevation, elevationUnits) {
  var self = this.ptr;
  if (ridgeToValleyElevation && typeof ridgeToValleyElevation === 'object') ridgeToValleyElevation = ridgeToValleyElevation.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  _emscripten_bind_SIGSpot_setRidgeToValleyElevation_2(self, ridgeToValleyElevation, elevationUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setTorchingTrees'] = SIGSpot.prototype.setTorchingTrees = function(torchingTrees) {
  var self = this.ptr;
  if (torchingTrees && typeof torchingTrees === 'object') torchingTrees = torchingTrees.ptr;
  _emscripten_bind_SIGSpot_setTorchingTrees_1(self, torchingTrees);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setTreeHeight'] = SIGSpot.prototype.setTreeHeight = function(treeHeight, treeHeightUnits) {
  var self = this.ptr;
  if (treeHeight && typeof treeHeight === 'object') treeHeight = treeHeight.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  _emscripten_bind_SIGSpot_setTreeHeight_2(self, treeHeight, treeHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setTreeSpecies'] = SIGSpot.prototype.setTreeSpecies = function(treeSpecies) {
  var self = this.ptr;
  if (treeSpecies && typeof treeSpecies === 'object') treeSpecies = treeSpecies.ptr;
  _emscripten_bind_SIGSpot_setTreeSpecies_1(self, treeSpecies);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setWindSpeedAtTwentyFeet'] = SIGSpot.prototype.setWindSpeedAtTwentyFeet = function(windSpeedAtTwentyFeet, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSpot_setWindSpeedAtTwentyFeet_2(self, windSpeedAtTwentyFeet, windSpeedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setWindSpeed'] = SIGSpot.prototype.setWindSpeed = function(windSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSpot_setWindSpeed_2(self, windSpeed, windSpeedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setWindSpeedAndWindHeightInputMode'] = SIGSpot.prototype.setWindSpeedAndWindHeightInputMode = function(windSpeed, windSpeedUnits, windHeightInputMode) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGSpot_setWindSpeedAndWindHeightInputMode_3(self, windSpeed, windSpeedUnits, windHeightInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['setWindHeightInputMode'] = SIGSpot.prototype.setWindHeightInputMode = function(windHeightInputMode) {
  var self = this.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGSpot_setWindHeightInputMode_1(self, windHeightInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['updateSpotInputsForBurningPile'] = SIGSpot.prototype.updateSpotInputsForBurningPile = function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits) {
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
  _emscripten_bind_SIGSpot_updateSpotInputsForBurningPile_12(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['updateSpotInputsForSurfaceFire'] = SIGSpot.prototype.updateSpotInputsForSurfaceFire = function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, windSpeedAtTwentyFeet, windSpeedUnits, flameLength, flameLengthUnits) {
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
  _emscripten_bind_SIGSpot_updateSpotInputsForSurfaceFire_12(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, windSpeedAtTwentyFeet, windSpeedUnits, flameLength, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['updateSpotInputsForTorchingTrees'] = SIGSpot.prototype.updateSpotInputsForTorchingTrees = function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits) {
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
  _emscripten_bind_SIGSpot_updateSpotInputsForTorchingTrees_16(self, location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSpot.prototype['__destroy__'] = SIGSpot.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot___destroy___0(self);
};

// Interface: SIGFuelModels

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGFuelModels(rhs) {
  if (rhs && typeof rhs === 'object') rhs = rhs.ptr;
  if (rhs === undefined) { this.ptr = _emscripten_bind_SIGFuelModels_SIGFuelModels_0(); getCache(SIGFuelModels)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_SIGFuelModels_SIGFuelModels_1(rhs);
  getCache(SIGFuelModels)[this.ptr] = this;
};

SIGFuelModels.prototype = Object.create(WrapperObject.prototype);
SIGFuelModels.prototype.constructor = SIGFuelModels;
SIGFuelModels.prototype.__class__ = SIGFuelModels;
SIGFuelModels.__cache__ = {};
Module['SIGFuelModels'] = SIGFuelModels;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['equal'] = SIGFuelModels.prototype.equal = function(rhs) {
  var self = this.ptr;
  if (rhs && typeof rhs === 'object') rhs = rhs.ptr;
  return wrapPointer(_emscripten_bind_SIGFuelModels_equal_1(self, rhs), SIGFuelModels);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['clearCustomFuelModel'] = SIGFuelModels.prototype.clearCustomFuelModel = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_clearCustomFuelModel_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getIsDynamic'] = SIGFuelModels.prototype.getIsDynamic = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_getIsDynamic_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['isAllFuelLoadZero'] = SIGFuelModels.prototype.isAllFuelLoadZero = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_isAllFuelLoadZero_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['isFuelModelDefined'] = SIGFuelModels.prototype.isFuelModelDefined = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_isFuelModelDefined_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['isFuelModelReserved'] = SIGFuelModels.prototype.isFuelModelReserved = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGFuelModels_isFuelModelReserved_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['setCustomFuelModel'] = SIGFuelModels.prototype.setCustomFuelModel = function(fuelModelNumber, code, name, fuelBedDepth, lengthUnits, moistureOfExtinctionDead, moistureUnits, heatOfCombustionDead, heatOfCombustionLive, heatOfCombustionUnits, fuelLoadOneHour, fuelLoadTenHour, fuelLoadHundredHour, fuelLoadLiveHerbaceous, fuelLoadLiveWoody, loadingUnits, savrOneHour, savrLiveHerbaceous, savrLiveWoody, savrUnits, isDynamic) {
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
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getFuelCode'] = SIGFuelModels.prototype.getFuelCode = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGFuelModels_getFuelCode_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getFuelName'] = SIGFuelModels.prototype.getFuelName = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGFuelModels_getFuelName_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getFuelLoadHundredHour'] = SIGFuelModels.prototype.getFuelLoadHundredHour = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadHundredHour_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getFuelLoadLiveHerbaceous'] = SIGFuelModels.prototype.getFuelLoadLiveHerbaceous = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadLiveHerbaceous_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getFuelLoadLiveWoody'] = SIGFuelModels.prototype.getFuelLoadLiveWoody = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadLiveWoody_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getFuelLoadOneHour'] = SIGFuelModels.prototype.getFuelLoadOneHour = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadOneHour_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getFuelLoadTenHour'] = SIGFuelModels.prototype.getFuelLoadTenHour = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelLoadTenHour_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getFuelbedDepth'] = SIGFuelModels.prototype.getFuelbedDepth = function(fuelModelNumber, lengthUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getFuelbedDepth_2(self, fuelModelNumber, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getHeatOfCombustionDead'] = SIGFuelModels.prototype.getHeatOfCombustionDead = function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getHeatOfCombustionDead_2(self, fuelModelNumber, heatOfCombustionUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getMoistureOfExtinctionDead'] = SIGFuelModels.prototype.getMoistureOfExtinctionDead = function(fuelModelNumber, moistureUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getMoistureOfExtinctionDead_2(self, fuelModelNumber, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getSavrLiveHerbaceous'] = SIGFuelModels.prototype.getSavrLiveHerbaceous = function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getSavrLiveHerbaceous_2(self, fuelModelNumber, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getSavrLiveWoody'] = SIGFuelModels.prototype.getSavrLiveWoody = function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getSavrLiveWoody_2(self, fuelModelNumber, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getSavrOneHour'] = SIGFuelModels.prototype.getSavrOneHour = function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getSavrOneHour_2(self, fuelModelNumber, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['getHeatOfCombustionLive'] = SIGFuelModels.prototype.getHeatOfCombustionLive = function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2(self, fuelModelNumber, heatOfCombustionUnits);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFuelModels.prototype['__destroy__'] = SIGFuelModels.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGFuelModels___destroy___0(self);
};

// Interface: SIGSurface

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGSurface(fuelModels) {
  if (fuelModels && typeof fuelModels === 'object') fuelModels = fuelModels.ptr;
  this.ptr = _emscripten_bind_SIGSurface_SIGSurface_1(fuelModels);
  getCache(SIGSurface)[this.ptr] = this;
};

SIGSurface.prototype = Object.create(WrapperObject.prototype);
SIGSurface.prototype.constructor = SIGSurface;
SIGSurface.prototype.__class__ = SIGSurface;
SIGSurface.__cache__ = {};
Module['SIGSurface'] = SIGSurface;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenFireSeverity'] = SIGSurface.prototype.getAspenFireSeverity = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getAspenFireSeverity_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralFuelType'] = SIGSurface.prototype.getChaparralFuelType = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getChaparralFuelType_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureInputMode'] = SIGSurface.prototype.getMoistureInputMode = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getMoistureInputMode_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getWindAdjustmentFactorCalculationMethod'] = SIGSurface.prototype.getWindAdjustmentFactorCalculationMethod = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindAdjustmentFactorCalculationMethod_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getWindAndSpreadOrientationMode'] = SIGSurface.prototype.getWindAndSpreadOrientationMode = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindAndSpreadOrientationMode_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getWindHeightInputMode'] = SIGSurface.prototype.getWindHeightInputMode = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindHeightInputMode_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getWindUpslopeAlignmentMode'] = SIGSurface.prototype.getWindUpslopeAlignmentMode = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindUpslopeAlignmentMode_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getSurfaceRunInDirectionOf'] = SIGSurface.prototype.getSurfaceRunInDirectionOf = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getSurfaceRunInDirectionOf_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getIsMoistureScenarioDefinedByIndex'] = SIGSurface.prototype.getIsMoistureScenarioDefinedByIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return !!(_emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getIsMoistureScenarioDefinedByName'] = SIGSurface.prototype.getIsMoistureScenarioDefinedByName = function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return !!(_emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByName_1(self, name));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getIsUsingChaparral'] = SIGSurface.prototype.getIsUsingChaparral = function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGSurface_getIsUsingChaparral_0(self));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getIsUsingPalmettoGallberry'] = SIGSurface.prototype.getIsUsingPalmettoGallberry = function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGSurface_getIsUsingPalmettoGallberry_0(self));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getIsUsingWesternAspen'] = SIGSurface.prototype.getIsUsingWesternAspen = function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGSurface_getIsUsingWesternAspen_0(self));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['isAllFuelLoadZero'] = SIGSurface.prototype.isAllFuelLoadZero = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGSurface_isAllFuelLoadZero_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['isFuelDynamic'] = SIGSurface.prototype.isFuelDynamic = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGSurface_isFuelDynamic_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['isFuelModelDefined'] = SIGSurface.prototype.isFuelModelDefined = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGSurface_isFuelModelDefined_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['isFuelModelReserved'] = SIGSurface.prototype.isFuelModelReserved = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGSurface_isFuelModelReserved_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['isMoistureClassInputNeededForCurrentFuelModel'] = SIGSurface.prototype.isMoistureClassInputNeededForCurrentFuelModel = function(moistureClass) {
  var self = this.ptr;
  if (moistureClass && typeof moistureClass === 'object') moistureClass = moistureClass.ptr;
  return !!(_emscripten_bind_SIGSurface_isMoistureClassInputNeededForCurrentFuelModel_1(self, moistureClass));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['isUsingTwoFuelModels'] = SIGSurface.prototype.isUsingTwoFuelModels = function() {
  var self = this.ptr;
  return !!(_emscripten_bind_SIGSurface_isUsingTwoFuelModels_0(self));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setCurrentMoistureScenarioByIndex'] = SIGSurface.prototype.setCurrentMoistureScenarioByIndex = function(moistureScenarioIndex) {
  var self = this.ptr;
  if (moistureScenarioIndex && typeof moistureScenarioIndex === 'object') moistureScenarioIndex = moistureScenarioIndex.ptr;
  return !!(_emscripten_bind_SIGSurface_setCurrentMoistureScenarioByIndex_1(self, moistureScenarioIndex));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setCurrentMoistureScenarioByName'] = SIGSurface.prototype.setCurrentMoistureScenarioByName = function(moistureScenarioName) {
  var self = this.ptr;
  ensureCache.prepare();
  if (moistureScenarioName && typeof moistureScenarioName === 'object') moistureScenarioName = moistureScenarioName.ptr;
  else moistureScenarioName = ensureString(moistureScenarioName);
  return !!(_emscripten_bind_SIGSurface_setCurrentMoistureScenarioByName_1(self, moistureScenarioName));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['calculateFlameLength'] = SIGSurface.prototype.calculateFlameLength = function(firelineIntensity, firelineIntensityUnits, flameLengthUnits) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_calculateFlameLength_3(self, firelineIntensity, firelineIntensityUnits, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAgeOfRough'] = SIGSurface.prototype.getAgeOfRough = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getAgeOfRough_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspect'] = SIGSurface.prototype.getAspect = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getAspect_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenCuringLevel'] = SIGSurface.prototype.getAspenCuringLevel = function(curingLevelUnits) {
  var self = this.ptr;
  if (curingLevelUnits && typeof curingLevelUnits === 'object') curingLevelUnits = curingLevelUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenCuringLevel_1(self, curingLevelUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenDBH'] = SIGSurface.prototype.getAspenDBH = function(dbhUnits) {
  var self = this.ptr;
  if (dbhUnits && typeof dbhUnits === 'object') dbhUnits = dbhUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenDBH_1(self, dbhUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenLoadDeadOneHour'] = SIGSurface.prototype.getAspenLoadDeadOneHour = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenLoadDeadOneHour_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenLoadDeadTenHour'] = SIGSurface.prototype.getAspenLoadDeadTenHour = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenLoadDeadTenHour_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenLoadLiveHerbaceous'] = SIGSurface.prototype.getAspenLoadLiveHerbaceous = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenLoadLiveHerbaceous_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenLoadLiveWoody'] = SIGSurface.prototype.getAspenLoadLiveWoody = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenLoadLiveWoody_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenSavrDeadOneHour'] = SIGSurface.prototype.getAspenSavrDeadOneHour = function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenSavrDeadOneHour_1(self, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenSavrDeadTenHour'] = SIGSurface.prototype.getAspenSavrDeadTenHour = function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenSavrDeadTenHour_1(self, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenSavrLiveHerbaceous'] = SIGSurface.prototype.getAspenSavrLiveHerbaceous = function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenSavrLiveHerbaceous_1(self, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenSavrLiveWoody'] = SIGSurface.prototype.getAspenSavrLiveWoody = function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getAspenSavrLiveWoody_1(self, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getBackingFirelineIntensity'] = SIGSurface.prototype.getBackingFirelineIntensity = function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getBackingFirelineIntensity_1(self, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getBackingFlameLength'] = SIGSurface.prototype.getBackingFlameLength = function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getBackingFlameLength_1(self, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getBackingSpreadDistance'] = SIGSurface.prototype.getBackingSpreadDistance = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getBackingSpreadDistance_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getBackingSpreadRate'] = SIGSurface.prototype.getBackingSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getBackingSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getBulkDensity'] = SIGSurface.prototype.getBulkDensity = function(densityUnits) {
  var self = this.ptr;
  if (densityUnits && typeof densityUnits === 'object') densityUnits = densityUnits.ptr;
  return _emscripten_bind_SIGSurface_getBulkDensity_1(self, densityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getCanopyCover'] = SIGSurface.prototype.getCanopyCover = function(coverUnits) {
  var self = this.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  return _emscripten_bind_SIGSurface_getCanopyCover_1(self, coverUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getCanopyHeight'] = SIGSurface.prototype.getCanopyHeight = function(canopyHeightUnits) {
  var self = this.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  return _emscripten_bind_SIGSurface_getCanopyHeight_1(self, canopyHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralAge'] = SIGSurface.prototype.getChaparralAge = function(ageUnits) {
  var self = this.ptr;
  if (ageUnits && typeof ageUnits === 'object') ageUnits = ageUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralAge_1(self, ageUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralDaysSinceMayFirst'] = SIGSurface.prototype.getChaparralDaysSinceMayFirst = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getChaparralDaysSinceMayFirst_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralDeadFuelFraction'] = SIGSurface.prototype.getChaparralDeadFuelFraction = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getChaparralDeadFuelFraction_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralDeadMoistureOfExtinction'] = SIGSurface.prototype.getChaparralDeadMoistureOfExtinction = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralDeadMoistureOfExtinction_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralDensity'] = SIGSurface.prototype.getChaparralDensity = function(lifeState, sizeClass, densityUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  if (densityUnits && typeof densityUnits === 'object') densityUnits = densityUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralDensity_3(self, lifeState, sizeClass, densityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralFuelBedDepth'] = SIGSurface.prototype.getChaparralFuelBedDepth = function(depthUnits) {
  var self = this.ptr;
  if (depthUnits && typeof depthUnits === 'object') depthUnits = depthUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralFuelBedDepth_1(self, depthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralFuelDeadLoadFraction'] = SIGSurface.prototype.getChaparralFuelDeadLoadFraction = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getChaparralFuelDeadLoadFraction_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralHeatOfCombustion'] = SIGSurface.prototype.getChaparralHeatOfCombustion = function(lifeState, sizeClass, heatOfCombustionUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralHeatOfCombustion_3(self, lifeState, sizeClass, heatOfCombustionUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLiveMoistureOfExtinction'] = SIGSurface.prototype.getChaparralLiveMoistureOfExtinction = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLiveMoistureOfExtinction_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLoadDeadHalfInchToLessThanOneInch'] = SIGSurface.prototype.getChaparralLoadDeadHalfInchToLessThanOneInch = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadDeadHalfInchToLessThanOneInch_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLoadDeadLessThanQuarterInch'] = SIGSurface.prototype.getChaparralLoadDeadLessThanQuarterInch = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadDeadLessThanQuarterInch_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLoadDeadOneInchToThreeInch'] = SIGSurface.prototype.getChaparralLoadDeadOneInchToThreeInch = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadDeadOneInchToThreeInch_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLoadDeadQuarterInchToLessThanHalfInch'] = SIGSurface.prototype.getChaparralLoadDeadQuarterInchToLessThanHalfInch = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadDeadQuarterInchToLessThanHalfInch_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLoadLiveHalfInchToLessThanOneInch'] = SIGSurface.prototype.getChaparralLoadLiveHalfInchToLessThanOneInch = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveHalfInchToLessThanOneInch_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLoadLiveLeaves'] = SIGSurface.prototype.getChaparralLoadLiveLeaves = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveLeaves_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLoadLiveOneInchToThreeInch'] = SIGSurface.prototype.getChaparralLoadLiveOneInchToThreeInch = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveOneInchToThreeInch_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLoadLiveQuarterInchToLessThanHalfInch'] = SIGSurface.prototype.getChaparralLoadLiveQuarterInchToLessThanHalfInch = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveQuarterInchToLessThanHalfInch_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralLoadLiveStemsLessThanQuaterInch'] = SIGSurface.prototype.getChaparralLoadLiveStemsLessThanQuaterInch = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveStemsLessThanQuaterInch_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralMoisture'] = SIGSurface.prototype.getChaparralMoisture = function(lifeState, sizeClass, moistureUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralMoisture_3(self, lifeState, sizeClass, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralTotalDeadFuelLoad'] = SIGSurface.prototype.getChaparralTotalDeadFuelLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralTotalDeadFuelLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralTotalFuelLoad'] = SIGSurface.prototype.getChaparralTotalFuelLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralTotalFuelLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getChaparralTotalLiveFuelLoad'] = SIGSurface.prototype.getChaparralTotalLiveFuelLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralTotalLiveFuelLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getCharacteristicMoistureByLifeState'] = SIGSurface.prototype.getCharacteristicMoistureByLifeState = function(lifeState, moistureUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicMoistureByLifeState_2(self, lifeState, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getCharacteristicMoistureDead'] = SIGSurface.prototype.getCharacteristicMoistureDead = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicMoistureDead_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getCharacteristicMoistureLive'] = SIGSurface.prototype.getCharacteristicMoistureLive = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicMoistureLive_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getCharacteristicSAVR'] = SIGSurface.prototype.getCharacteristicSAVR = function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicSAVR_1(self, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getCrownRatio'] = SIGSurface.prototype.getCrownRatio = function(crownRatioUnits) {
  var self = this.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  return _emscripten_bind_SIGSurface_getCrownRatio_1(self, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getDirectionOfMaxSpread'] = SIGSurface.prototype.getDirectionOfMaxSpread = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getDirectionOfMaxSpread_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getDirectionOfInterest'] = SIGSurface.prototype.getDirectionOfInterest = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getDirectionOfInterest_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getDirectionOfBacking'] = SIGSurface.prototype.getDirectionOfBacking = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getDirectionOfBacking_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getDirectionOfFlanking'] = SIGSurface.prototype.getDirectionOfFlanking = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getDirectionOfFlanking_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getElapsedTime'] = SIGSurface.prototype.getElapsedTime = function(timeUnits) {
  var self = this.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getElapsedTime_1(self, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getEllipticalA'] = SIGSurface.prototype.getEllipticalA = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getEllipticalA_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getEllipticalB'] = SIGSurface.prototype.getEllipticalB = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getEllipticalB_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getEllipticalC'] = SIGSurface.prototype.getEllipticalC = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getEllipticalC_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFireLength'] = SIGSurface.prototype.getFireLength = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFireLength_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMaxFireWidth'] = SIGSurface.prototype.getMaxFireWidth = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getMaxFireWidth_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFireArea'] = SIGSurface.prototype.getFireArea = function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGSurface_getFireArea_1(self, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFireEccentricity'] = SIGSurface.prototype.getFireEccentricity = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getFireEccentricity_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFireLengthToWidthRatio'] = SIGSurface.prototype.getFireLengthToWidthRatio = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getFireLengthToWidthRatio_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFirePerimeter'] = SIGSurface.prototype.getFirePerimeter = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFirePerimeter_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFirelineIntensity'] = SIGSurface.prototype.getFirelineIntensity = function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getFirelineIntensity_1(self, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFirelineIntensityInDirectionOfInterest'] = SIGSurface.prototype.getFirelineIntensityInDirectionOfInterest = function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getFirelineIntensityInDirectionOfInterest_1(self, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFlameLength'] = SIGSurface.prototype.getFlameLength = function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlameLength_1(self, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFlameLengthInDirectionOfInterest'] = SIGSurface.prototype.getFlameLengthInDirectionOfInterest = function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlameLengthInDirectionOfInterest_1(self, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFlankingFirelineIntensity'] = SIGSurface.prototype.getFlankingFirelineIntensity = function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingFirelineIntensity_1(self, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFlankingFlameLength'] = SIGSurface.prototype.getFlankingFlameLength = function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingFlameLength_1(self, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFlankingSpreadRate'] = SIGSurface.prototype.getFlankingSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFlankingSpreadDistance'] = SIGSurface.prototype.getFlankingSpreadDistance = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingSpreadDistance_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelHeatOfCombustionDead'] = SIGSurface.prototype.getFuelHeatOfCombustionDead = function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelHeatOfCombustionDead_2(self, fuelModelNumber, heatOfCombustionUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelHeatOfCombustionLive'] = SIGSurface.prototype.getFuelHeatOfCombustionLive = function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelHeatOfCombustionLive_2(self, fuelModelNumber, heatOfCombustionUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelLoadHundredHour'] = SIGSurface.prototype.getFuelLoadHundredHour = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadHundredHour_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelLoadLiveHerbaceous'] = SIGSurface.prototype.getFuelLoadLiveHerbaceous = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadLiveHerbaceous_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelLoadLiveWoody'] = SIGSurface.prototype.getFuelLoadLiveWoody = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadLiveWoody_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelLoadOneHour'] = SIGSurface.prototype.getFuelLoadOneHour = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadOneHour_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelLoadTenHour'] = SIGSurface.prototype.getFuelLoadTenHour = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelLoadTenHour_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelMoistureOfExtinctionDead'] = SIGSurface.prototype.getFuelMoistureOfExtinctionDead = function(fuelModelNumber, moistureUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelMoistureOfExtinctionDead_2(self, fuelModelNumber, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelSavrLiveHerbaceous'] = SIGSurface.prototype.getFuelSavrLiveHerbaceous = function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelSavrLiveHerbaceous_2(self, fuelModelNumber, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelSavrLiveWoody'] = SIGSurface.prototype.getFuelSavrLiveWoody = function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelSavrLiveWoody_2(self, fuelModelNumber, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelSavrOneHour'] = SIGSurface.prototype.getFuelSavrOneHour = function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelSavrOneHour_2(self, fuelModelNumber, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelbedDepth'] = SIGSurface.prototype.getFuelbedDepth = function(fuelModelNumber, lengthUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFuelbedDepth_2(self, fuelModelNumber, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getHeadingSpreadRate'] = SIGSurface.prototype.getHeadingSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeadingSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getHeadingToBackingRatio'] = SIGSurface.prototype.getHeadingToBackingRatio = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getHeadingToBackingRatio_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getHeatPerUnitArea'] = SIGSurface.prototype.getHeatPerUnitArea = function(heatPerUnitAreaUnits) {
  var self = this.ptr;
  if (heatPerUnitAreaUnits && typeof heatPerUnitAreaUnits === 'object') heatPerUnitAreaUnits = heatPerUnitAreaUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeatPerUnitArea_1(self, heatPerUnitAreaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getHeatSink'] = SIGSurface.prototype.getHeatSink = function(heatSinkUnits) {
  var self = this.ptr;
  if (heatSinkUnits && typeof heatSinkUnits === 'object') heatSinkUnits = heatSinkUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeatSink_1(self, heatSinkUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getHeatSource'] = SIGSurface.prototype.getHeatSource = function(heatSourceUnits) {
  var self = this.ptr;
  if (heatSourceUnits && typeof heatSourceUnits === 'object') heatSourceUnits = heatSourceUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeatSource_1(self, heatSourceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getHeightOfUnderstory'] = SIGSurface.prototype.getHeightOfUnderstory = function(heightUnits) {
  var self = this.ptr;
  if (heightUnits && typeof heightUnits === 'object') heightUnits = heightUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeightOfUnderstory_1(self, heightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getLiveFuelMoistureOfExtinction'] = SIGSurface.prototype.getLiveFuelMoistureOfExtinction = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getLiveFuelMoistureOfExtinction_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMidflameWindspeed'] = SIGSurface.prototype.getMidflameWindspeed = function(windSpeedUnits) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  return _emscripten_bind_SIGSurface_getMidflameWindspeed_1(self, windSpeedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureDeadAggregateValue'] = SIGSurface.prototype.getMoistureDeadAggregateValue = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureDeadAggregateValue_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureHundredHour'] = SIGSurface.prototype.getMoistureHundredHour = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureHundredHour_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureLiveAggregateValue'] = SIGSurface.prototype.getMoistureLiveAggregateValue = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureLiveAggregateValue_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureLiveHerbaceous'] = SIGSurface.prototype.getMoistureLiveHerbaceous = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureLiveWoody'] = SIGSurface.prototype.getMoistureLiveWoody = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureLiveWoody_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureOneHour'] = SIGSurface.prototype.getMoistureOneHour = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureOneHour_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioHundredHourByIndex'] = SIGSurface.prototype.getMoistureScenarioHundredHourByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioHundredHourByName'] = SIGSurface.prototype.getMoistureScenarioHundredHourByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioLiveHerbaceousByIndex'] = SIGSurface.prototype.getMoistureScenarioLiveHerbaceousByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioLiveHerbaceousByName'] = SIGSurface.prototype.getMoistureScenarioLiveHerbaceousByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioLiveWoodyByIndex'] = SIGSurface.prototype.getMoistureScenarioLiveWoodyByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioLiveWoodyByName'] = SIGSurface.prototype.getMoistureScenarioLiveWoodyByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioOneHourByIndex'] = SIGSurface.prototype.getMoistureScenarioOneHourByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioOneHourByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioOneHourByName'] = SIGSurface.prototype.getMoistureScenarioOneHourByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioOneHourByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioTenHourByIndex'] = SIGSurface.prototype.getMoistureScenarioTenHourByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioTenHourByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioTenHourByName'] = SIGSurface.prototype.getMoistureScenarioTenHourByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioTenHourByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureTenHour'] = SIGSurface.prototype.getMoistureTenHour = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureTenHour_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getOverstoryBasalArea'] = SIGSurface.prototype.getOverstoryBasalArea = function(basalAreaUnits) {
  var self = this.ptr;
  if (basalAreaUnits && typeof basalAreaUnits === 'object') basalAreaUnits = basalAreaUnits.ptr;
  return _emscripten_bind_SIGSurface_getOverstoryBasalArea_1(self, basalAreaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberryCoverage'] = SIGSurface.prototype.getPalmettoGallberryCoverage = function(coverUnits) {
  var self = this.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberryCoverage_1(self, coverUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberryHeatOfCombustionDead'] = SIGSurface.prototype.getPalmettoGallberryHeatOfCombustionDead = function(heatOfCombustionUnits) {
  var self = this.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionDead_1(self, heatOfCombustionUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberryHeatOfCombustionLive'] = SIGSurface.prototype.getPalmettoGallberryHeatOfCombustionLive = function(heatOfCombustionUnits) {
  var self = this.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionLive_1(self, heatOfCombustionUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberryMoistureOfExtinctionDead'] = SIGSurface.prototype.getPalmettoGallberryMoistureOfExtinctionDead = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberryMoistureOfExtinctionDead_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberyDeadFineFuelLoad'] = SIGSurface.prototype.getPalmettoGallberyDeadFineFuelLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyDeadFineFuelLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberyDeadFoliageLoad'] = SIGSurface.prototype.getPalmettoGallberyDeadFoliageLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyDeadFoliageLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberyDeadMediumFuelLoad'] = SIGSurface.prototype.getPalmettoGallberyDeadMediumFuelLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyDeadMediumFuelLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberyFuelBedDepth'] = SIGSurface.prototype.getPalmettoGallberyFuelBedDepth = function(depthUnits) {
  var self = this.ptr;
  if (depthUnits && typeof depthUnits === 'object') depthUnits = depthUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyFuelBedDepth_1(self, depthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberyLitterLoad'] = SIGSurface.prototype.getPalmettoGallberyLitterLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLitterLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberyLiveFineFuelLoad'] = SIGSurface.prototype.getPalmettoGallberyLiveFineFuelLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLiveFineFuelLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberyLiveFoliageLoad'] = SIGSurface.prototype.getPalmettoGallberyLiveFoliageLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLiveFoliageLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getPalmettoGallberyLiveMediumFuelLoad'] = SIGSurface.prototype.getPalmettoGallberyLiveMediumFuelLoad = function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLiveMediumFuelLoad_1(self, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getReactionIntensity'] = SIGSurface.prototype.getReactionIntensity = function(reactiontionIntensityUnits) {
  var self = this.ptr;
  if (reactiontionIntensityUnits && typeof reactiontionIntensityUnits === 'object') reactiontionIntensityUnits = reactiontionIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getReactionIntensity_1(self, reactiontionIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getResidenceTime'] = SIGSurface.prototype.getResidenceTime = function(timeUnits) {
  var self = this.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getResidenceTime_1(self, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getSlope'] = SIGSurface.prototype.getSlope = function(slopeUnits) {
  var self = this.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  return _emscripten_bind_SIGSurface_getSlope_1(self, slopeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getSlopeFactor'] = SIGSurface.prototype.getSlopeFactor = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getSlopeFactor_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getSpreadDistance'] = SIGSurface.prototype.getSpreadDistance = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadDistance_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getSpreadDistanceInDirectionOfInterest'] = SIGSurface.prototype.getSpreadDistanceInDirectionOfInterest = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadDistanceInDirectionOfInterest_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getSpreadRate'] = SIGSurface.prototype.getSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getSpreadRateInDirectionOfInterest'] = SIGSurface.prototype.getSpreadRateInDirectionOfInterest = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getSurfaceFireReactionIntensityForLifeState'] = SIGSurface.prototype.getSurfaceFireReactionIntensityForLifeState = function(lifeState) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  return _emscripten_bind_SIGSurface_getSurfaceFireReactionIntensityForLifeState_1(self, lifeState);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getWindDirection'] = SIGSurface.prototype.getWindDirection = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindDirection_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getWindSpeed'] = SIGSurface.prototype.getWindSpeed = function(windSpeedUnits, windHeightInputMode) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  return _emscripten_bind_SIGSurface_getWindSpeed_2(self, windSpeedUnits, windHeightInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getAspenFuelModelNumber'] = SIGSurface.prototype.getAspenFuelModelNumber = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getAspenFuelModelNumber_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelModelNumber'] = SIGSurface.prototype.getFuelModelNumber = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getFuelModelNumber_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioIndexByName'] = SIGSurface.prototype.getMoistureScenarioIndexByName = function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGSurface_getMoistureScenarioIndexByName_1(self, name);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getNumberOfMoistureScenarios'] = SIGSurface.prototype.getNumberOfMoistureScenarios = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getNumberOfMoistureScenarios_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelCode'] = SIGSurface.prototype.getFuelCode = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGSurface_getFuelCode_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getFuelName'] = SIGSurface.prototype.getFuelName = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGSurface_getFuelName_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioDescriptionByIndex'] = SIGSurface.prototype.getMoistureScenarioDescriptionByIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioDescriptionByName'] = SIGSurface.prototype.getMoistureScenarioDescriptionByName = function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return UTF8ToString(_emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByName_1(self, name));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['getMoistureScenarioNameByIndex'] = SIGSurface.prototype.getMoistureScenarioNameByIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGSurface_getMoistureScenarioNameByIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['doSurfaceRun'] = SIGSurface.prototype.doSurfaceRun = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface_doSurfaceRun_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['doSurfaceRunInDirectionOfInterest'] = SIGSurface.prototype.doSurfaceRunInDirectionOfInterest = function(directionOfInterest, directionMode) {
  var self = this.ptr;
  if (directionOfInterest && typeof directionOfInterest === 'object') directionOfInterest = directionOfInterest.ptr;
  if (directionMode && typeof directionMode === 'object') directionMode = directionMode.ptr;
  _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_2(self, directionOfInterest, directionMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['doSurfaceRunInDirectionOfMaxSpread'] = SIGSurface.prototype.doSurfaceRunInDirectionOfMaxSpread = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['initializeMembers'] = SIGSurface.prototype.initializeMembers = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface_initializeMembers_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setAgeOfRough'] = SIGSurface.prototype.setAgeOfRough = function(ageOfRough) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  _emscripten_bind_SIGSurface_setAgeOfRough_1(self, ageOfRough);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setAspect'] = SIGSurface.prototype.setAspect = function(aspect) {
  var self = this.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  _emscripten_bind_SIGSurface_setAspect_1(self, aspect);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setAspenCuringLevel'] = SIGSurface.prototype.setAspenCuringLevel = function(aspenCuringLevel, curingLevelUnits) {
  var self = this.ptr;
  if (aspenCuringLevel && typeof aspenCuringLevel === 'object') aspenCuringLevel = aspenCuringLevel.ptr;
  if (curingLevelUnits && typeof curingLevelUnits === 'object') curingLevelUnits = curingLevelUnits.ptr;
  _emscripten_bind_SIGSurface_setAspenCuringLevel_2(self, aspenCuringLevel, curingLevelUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setAspenDBH'] = SIGSurface.prototype.setAspenDBH = function(dbh, dbhUnits) {
  var self = this.ptr;
  if (dbh && typeof dbh === 'object') dbh = dbh.ptr;
  if (dbhUnits && typeof dbhUnits === 'object') dbhUnits = dbhUnits.ptr;
  _emscripten_bind_SIGSurface_setAspenDBH_2(self, dbh, dbhUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setAspenFireSeverity'] = SIGSurface.prototype.setAspenFireSeverity = function(aspenFireSeverity) {
  var self = this.ptr;
  if (aspenFireSeverity && typeof aspenFireSeverity === 'object') aspenFireSeverity = aspenFireSeverity.ptr;
  _emscripten_bind_SIGSurface_setAspenFireSeverity_1(self, aspenFireSeverity);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setAspenFuelModelNumber'] = SIGSurface.prototype.setAspenFuelModelNumber = function(aspenFuelModelNumber) {
  var self = this.ptr;
  if (aspenFuelModelNumber && typeof aspenFuelModelNumber === 'object') aspenFuelModelNumber = aspenFuelModelNumber.ptr;
  _emscripten_bind_SIGSurface_setAspenFuelModelNumber_1(self, aspenFuelModelNumber);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setCanopyCover'] = SIGSurface.prototype.setCanopyCover = function(canopyCover, coverUnits) {
  var self = this.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  _emscripten_bind_SIGSurface_setCanopyCover_2(self, canopyCover, coverUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setCanopyHeight'] = SIGSurface.prototype.setCanopyHeight = function(canopyHeight, canopyHeightUnits) {
  var self = this.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  _emscripten_bind_SIGSurface_setCanopyHeight_2(self, canopyHeight, canopyHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setChaparralFuelBedDepth'] = SIGSurface.prototype.setChaparralFuelBedDepth = function(chaparralFuelBedDepth, depthUnts) {
  var self = this.ptr;
  if (chaparralFuelBedDepth && typeof chaparralFuelBedDepth === 'object') chaparralFuelBedDepth = chaparralFuelBedDepth.ptr;
  if (depthUnts && typeof depthUnts === 'object') depthUnts = depthUnts.ptr;
  _emscripten_bind_SIGSurface_setChaparralFuelBedDepth_2(self, chaparralFuelBedDepth, depthUnts);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setChaparralFuelDeadLoadFraction'] = SIGSurface.prototype.setChaparralFuelDeadLoadFraction = function(chaparralFuelDeadLoadFraction) {
  var self = this.ptr;
  if (chaparralFuelDeadLoadFraction && typeof chaparralFuelDeadLoadFraction === 'object') chaparralFuelDeadLoadFraction = chaparralFuelDeadLoadFraction.ptr;
  _emscripten_bind_SIGSurface_setChaparralFuelDeadLoadFraction_1(self, chaparralFuelDeadLoadFraction);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setChaparralFuelLoadInputMode'] = SIGSurface.prototype.setChaparralFuelLoadInputMode = function(fuelLoadInputMode) {
  var self = this.ptr;
  if (fuelLoadInputMode && typeof fuelLoadInputMode === 'object') fuelLoadInputMode = fuelLoadInputMode.ptr;
  _emscripten_bind_SIGSurface_setChaparralFuelLoadInputMode_1(self, fuelLoadInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setChaparralFuelType'] = SIGSurface.prototype.setChaparralFuelType = function(chaparralFuelType) {
  var self = this.ptr;
  if (chaparralFuelType && typeof chaparralFuelType === 'object') chaparralFuelType = chaparralFuelType.ptr;
  _emscripten_bind_SIGSurface_setChaparralFuelType_1(self, chaparralFuelType);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setChaparralTotalFuelLoad'] = SIGSurface.prototype.setChaparralTotalFuelLoad = function(chaparralTotalFuelLoad, fuelLoadUnits) {
  var self = this.ptr;
  if (chaparralTotalFuelLoad && typeof chaparralTotalFuelLoad === 'object') chaparralTotalFuelLoad = chaparralTotalFuelLoad.ptr;
  if (fuelLoadUnits && typeof fuelLoadUnits === 'object') fuelLoadUnits = fuelLoadUnits.ptr;
  _emscripten_bind_SIGSurface_setChaparralTotalFuelLoad_2(self, chaparralTotalFuelLoad, fuelLoadUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setCrownRatio'] = SIGSurface.prototype.setCrownRatio = function(crownRatio, crownRatioUnits) {
  var self = this.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGSurface_setCrownRatio_2(self, crownRatio, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setDirectionOfInterest'] = SIGSurface.prototype.setDirectionOfInterest = function(directionOfInterest) {
  var self = this.ptr;
  if (directionOfInterest && typeof directionOfInterest === 'object') directionOfInterest = directionOfInterest.ptr;
  _emscripten_bind_SIGSurface_setDirectionOfInterest_1(self, directionOfInterest);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setElapsedTime'] = SIGSurface.prototype.setElapsedTime = function(elapsedTime, timeUnits) {
  var self = this.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  _emscripten_bind_SIGSurface_setElapsedTime_2(self, elapsedTime, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setFirstFuelModelNumber'] = SIGSurface.prototype.setFirstFuelModelNumber = function(firstFuelModelNumber) {
  var self = this.ptr;
  if (firstFuelModelNumber && typeof firstFuelModelNumber === 'object') firstFuelModelNumber = firstFuelModelNumber.ptr;
  _emscripten_bind_SIGSurface_setFirstFuelModelNumber_1(self, firstFuelModelNumber);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setFuelModels'] = SIGSurface.prototype.setFuelModels = function(fuelModels) {
  var self = this.ptr;
  if (fuelModels && typeof fuelModels === 'object') fuelModels = fuelModels.ptr;
  _emscripten_bind_SIGSurface_setFuelModels_1(self, fuelModels);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setHeightOfUnderstory'] = SIGSurface.prototype.setHeightOfUnderstory = function(heightOfUnderstory, heightUnits) {
  var self = this.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  if (heightUnits && typeof heightUnits === 'object') heightUnits = heightUnits.ptr;
  _emscripten_bind_SIGSurface_setHeightOfUnderstory_2(self, heightOfUnderstory, heightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setIsUsingChaparral'] = SIGSurface.prototype.setIsUsingChaparral = function(isUsingChaparral) {
  var self = this.ptr;
  if (isUsingChaparral && typeof isUsingChaparral === 'object') isUsingChaparral = isUsingChaparral.ptr;
  _emscripten_bind_SIGSurface_setIsUsingChaparral_1(self, isUsingChaparral);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setIsUsingPalmettoGallberry'] = SIGSurface.prototype.setIsUsingPalmettoGallberry = function(isUsingPalmettoGallberry) {
  var self = this.ptr;
  if (isUsingPalmettoGallberry && typeof isUsingPalmettoGallberry === 'object') isUsingPalmettoGallberry = isUsingPalmettoGallberry.ptr;
  _emscripten_bind_SIGSurface_setIsUsingPalmettoGallberry_1(self, isUsingPalmettoGallberry);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setIsUsingWesternAspen'] = SIGSurface.prototype.setIsUsingWesternAspen = function(isUsingWesternAspen) {
  var self = this.ptr;
  if (isUsingWesternAspen && typeof isUsingWesternAspen === 'object') isUsingWesternAspen = isUsingWesternAspen.ptr;
  _emscripten_bind_SIGSurface_setIsUsingWesternAspen_1(self, isUsingWesternAspen);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setMoistureDeadAggregate'] = SIGSurface.prototype.setMoistureDeadAggregate = function(moistureDead, moistureUnits) {
  var self = this.ptr;
  if (moistureDead && typeof moistureDead === 'object') moistureDead = moistureDead.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureDeadAggregate_2(self, moistureDead, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setMoistureHundredHour'] = SIGSurface.prototype.setMoistureHundredHour = function(moistureHundredHour, moistureUnits) {
  var self = this.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureHundredHour_2(self, moistureHundredHour, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setMoistureInputMode'] = SIGSurface.prototype.setMoistureInputMode = function(moistureInputMode) {
  var self = this.ptr;
  if (moistureInputMode && typeof moistureInputMode === 'object') moistureInputMode = moistureInputMode.ptr;
  _emscripten_bind_SIGSurface_setMoistureInputMode_1(self, moistureInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setMoistureLiveAggregate'] = SIGSurface.prototype.setMoistureLiveAggregate = function(moistureLive, moistureUnits) {
  var self = this.ptr;
  if (moistureLive && typeof moistureLive === 'object') moistureLive = moistureLive.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureLiveAggregate_2(self, moistureLive, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setMoistureLiveHerbaceous'] = SIGSurface.prototype.setMoistureLiveHerbaceous = function(moistureLiveHerbaceous, moistureUnits) {
  var self = this.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2(self, moistureLiveHerbaceous, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setMoistureLiveWoody'] = SIGSurface.prototype.setMoistureLiveWoody = function(moistureLiveWoody, moistureUnits) {
  var self = this.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureLiveWoody_2(self, moistureLiveWoody, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setMoistureOneHour'] = SIGSurface.prototype.setMoistureOneHour = function(moistureOneHour, moistureUnits) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureOneHour_2(self, moistureOneHour, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setMoistureScenarios'] = SIGSurface.prototype.setMoistureScenarios = function(moistureScenarios) {
  var self = this.ptr;
  if (moistureScenarios && typeof moistureScenarios === 'object') moistureScenarios = moistureScenarios.ptr;
  _emscripten_bind_SIGSurface_setMoistureScenarios_1(self, moistureScenarios);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setMoistureTenHour'] = SIGSurface.prototype.setMoistureTenHour = function(moistureTenHour, moistureUnits) {
  var self = this.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGSurface_setMoistureTenHour_2(self, moistureTenHour, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setOverstoryBasalArea'] = SIGSurface.prototype.setOverstoryBasalArea = function(overstoryBasalArea, basalAreaUnits) {
  var self = this.ptr;
  if (overstoryBasalArea && typeof overstoryBasalArea === 'object') overstoryBasalArea = overstoryBasalArea.ptr;
  if (basalAreaUnits && typeof basalAreaUnits === 'object') basalAreaUnits = basalAreaUnits.ptr;
  _emscripten_bind_SIGSurface_setOverstoryBasalArea_2(self, overstoryBasalArea, basalAreaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setPalmettoCoverage'] = SIGSurface.prototype.setPalmettoCoverage = function(palmettoCoverage, coverUnits) {
  var self = this.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  _emscripten_bind_SIGSurface_setPalmettoCoverage_2(self, palmettoCoverage, coverUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setSecondFuelModelNumber'] = SIGSurface.prototype.setSecondFuelModelNumber = function(secondFuelModelNumber) {
  var self = this.ptr;
  if (secondFuelModelNumber && typeof secondFuelModelNumber === 'object') secondFuelModelNumber = secondFuelModelNumber.ptr;
  _emscripten_bind_SIGSurface_setSecondFuelModelNumber_1(self, secondFuelModelNumber);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setSlope'] = SIGSurface.prototype.setSlope = function(slope, slopeUnits) {
  var self = this.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  _emscripten_bind_SIGSurface_setSlope_2(self, slope, slopeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setSurfaceFireSpreadDirectionMode'] = SIGSurface.prototype.setSurfaceFireSpreadDirectionMode = function(directionMode) {
  var self = this.ptr;
  if (directionMode && typeof directionMode === 'object') directionMode = directionMode.ptr;
  _emscripten_bind_SIGSurface_setSurfaceFireSpreadDirectionMode_1(self, directionMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setSurfaceRunInDirectionOf'] = SIGSurface.prototype.setSurfaceRunInDirectionOf = function(surfaceRunInDirectionOf) {
  var self = this.ptr;
  if (surfaceRunInDirectionOf && typeof surfaceRunInDirectionOf === 'object') surfaceRunInDirectionOf = surfaceRunInDirectionOf.ptr;
  _emscripten_bind_SIGSurface_setSurfaceRunInDirectionOf_1(self, surfaceRunInDirectionOf);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setTwoFuelModelsFirstFuelModelCoverage'] = SIGSurface.prototype.setTwoFuelModelsFirstFuelModelCoverage = function(firstFuelModelCoverage, coverUnits) {
  var self = this.ptr;
  if (firstFuelModelCoverage && typeof firstFuelModelCoverage === 'object') firstFuelModelCoverage = firstFuelModelCoverage.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  _emscripten_bind_SIGSurface_setTwoFuelModelsFirstFuelModelCoverage_2(self, firstFuelModelCoverage, coverUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setTwoFuelModelsMethod'] = SIGSurface.prototype.setTwoFuelModelsMethod = function(twoFuelModelsMethod) {
  var self = this.ptr;
  if (twoFuelModelsMethod && typeof twoFuelModelsMethod === 'object') twoFuelModelsMethod = twoFuelModelsMethod.ptr;
  _emscripten_bind_SIGSurface_setTwoFuelModelsMethod_1(self, twoFuelModelsMethod);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setUserProvidedWindAdjustmentFactor'] = SIGSurface.prototype.setUserProvidedWindAdjustmentFactor = function(userProvidedWindAdjustmentFactor) {
  var self = this.ptr;
  if (userProvidedWindAdjustmentFactor && typeof userProvidedWindAdjustmentFactor === 'object') userProvidedWindAdjustmentFactor = userProvidedWindAdjustmentFactor.ptr;
  _emscripten_bind_SIGSurface_setUserProvidedWindAdjustmentFactor_1(self, userProvidedWindAdjustmentFactor);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setWindAdjustmentFactorCalculationMethod'] = SIGSurface.prototype.setWindAdjustmentFactorCalculationMethod = function(windAdjustmentFactorCalculationMethod) {
  var self = this.ptr;
  if (windAdjustmentFactorCalculationMethod && typeof windAdjustmentFactorCalculationMethod === 'object') windAdjustmentFactorCalculationMethod = windAdjustmentFactorCalculationMethod.ptr;
  _emscripten_bind_SIGSurface_setWindAdjustmentFactorCalculationMethod_1(self, windAdjustmentFactorCalculationMethod);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setWindAndSpreadOrientationMode'] = SIGSurface.prototype.setWindAndSpreadOrientationMode = function(windAndSpreadOrientationMode) {
  var self = this.ptr;
  if (windAndSpreadOrientationMode && typeof windAndSpreadOrientationMode === 'object') windAndSpreadOrientationMode = windAndSpreadOrientationMode.ptr;
  _emscripten_bind_SIGSurface_setWindAndSpreadOrientationMode_1(self, windAndSpreadOrientationMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setWindDirection'] = SIGSurface.prototype.setWindDirection = function(windDirection) {
  var self = this.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  _emscripten_bind_SIGSurface_setWindDirection_1(self, windDirection);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setWindHeightInputMode'] = SIGSurface.prototype.setWindHeightInputMode = function(windHeightInputMode) {
  var self = this.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGSurface_setWindHeightInputMode_1(self, windHeightInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setWindSpeed'] = SIGSurface.prototype.setWindSpeed = function(windSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSurface_setWindSpeed_2(self, windSpeed, windSpeedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['updateSurfaceInputs'] = SIGSurface.prototype.updateSurfaceInputs = function(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits) {
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
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGSurface_updateSurfaceInputs_21(self, fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['updateSurfaceInputsForPalmettoGallbery'] = SIGSurface.prototype.updateSurfaceInputsForPalmettoGallbery = function(moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, basalAreaUnits, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits) {
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
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_25(self, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, basalAreaUnits, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['updateSurfaceInputsForTwoFuelModels'] = SIGSurface.prototype.updateSurfaceInputsForTwoFuelModels = function(firstFuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope, slopeUnits, aspect, canopyCover, canopyFractionUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnitso) {
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
  if (canopyFractionUnits && typeof canopyFractionUnits === 'object') canopyFractionUnits = canopyFractionUnits.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  if (crownRatioUnitso && typeof crownRatioUnitso === 'object') crownRatioUnitso = crownRatioUnitso.ptr;
  _emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_25(self, firstFuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope, slopeUnits, aspect, canopyCover, canopyFractionUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnitso);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['updateSurfaceInputsForWesternAspen'] = SIGSurface.prototype.updateSurfaceInputsForWesternAspen = function(aspenFuelModelNumber, aspenCuringLevel, curingLevelUnits, aspenFireSeverity, dbh, dbhUnits, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits) {
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
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_26(self, aspenFuelModelNumber, aspenCuringLevel, curingLevelUnits, aspenFireSeverity, dbh, dbhUnits, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['setFuelModelNumber'] = SIGSurface.prototype.setFuelModelNumber = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  _emscripten_bind_SIGSurface_setFuelModelNumber_1(self, fuelModelNumber);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSurface.prototype['__destroy__'] = SIGSurface.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface___destroy___0(self);
};

// Interface: PalmettoGallberry

/** @suppress {undefinedVars, duplicate} @this{Object} */
function PalmettoGallberry() {
  this.ptr = _emscripten_bind_PalmettoGallberry_PalmettoGallberry_0();
  getCache(PalmettoGallberry)[this.ptr] = this;
};

PalmettoGallberry.prototype = Object.create(WrapperObject.prototype);
PalmettoGallberry.prototype.constructor = PalmettoGallberry;
PalmettoGallberry.prototype.__class__ = PalmettoGallberry;
PalmettoGallberry.__cache__ = {};
Module['PalmettoGallberry'] = PalmettoGallberry;
/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['initializeMembers'] = PalmettoGallberry.prototype.initializeMembers = function() {
  var self = this.ptr;
  _emscripten_bind_PalmettoGallberry_initializeMembers_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['calculatePalmettoGallberyDeadFineFuelLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyDeadFineFuelLoad = function(ageOfRough, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFineFuelLoad_2(self, ageOfRough, heightOfUnderstory);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['calculatePalmettoGallberyDeadFoliageLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyDeadFoliageLoad = function(ageOfRough, palmettoCoverage) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFoliageLoad_2(self, ageOfRough, palmettoCoverage);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['calculatePalmettoGallberyDeadMediumFuelLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyDeadMediumFuelLoad = function(ageOfRough, palmettoCoverage) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadMediumFuelLoad_2(self, ageOfRough, palmettoCoverage);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['calculatePalmettoGallberyFuelBedDepth'] = PalmettoGallberry.prototype.calculatePalmettoGallberyFuelBedDepth = function(heightOfUnderstory) {
  var self = this.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyFuelBedDepth_1(self, heightOfUnderstory);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['calculatePalmettoGallberyLitterLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLitterLoad = function(ageOfRough, overstoryBasalArea) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (overstoryBasalArea && typeof overstoryBasalArea === 'object') overstoryBasalArea = overstoryBasalArea.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLitterLoad_2(self, ageOfRough, overstoryBasalArea);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['calculatePalmettoGallberyLiveFineFuelLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLiveFineFuelLoad = function(ageOfRough, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFineFuelLoad_2(self, ageOfRough, heightOfUnderstory);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['calculatePalmettoGallberyLiveFoliageLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLiveFoliageLoad = function(ageOfRough, palmettoCoverage, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFoliageLoad_3(self, ageOfRough, palmettoCoverage, heightOfUnderstory);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['calculatePalmettoGallberyLiveMediumFuelLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLiveMediumFuelLoad = function(ageOfRough, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveMediumFuelLoad_2(self, ageOfRough, heightOfUnderstory);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getHeatOfCombustionDead'] = PalmettoGallberry.prototype.getHeatOfCombustionDead = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getHeatOfCombustionDead_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getHeatOfCombustionLive'] = PalmettoGallberry.prototype.getHeatOfCombustionLive = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getHeatOfCombustionLive_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getMoistureOfExtinctionDead'] = PalmettoGallberry.prototype.getMoistureOfExtinctionDead = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getMoistureOfExtinctionDead_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getPalmettoGallberyDeadFineFuelLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyDeadFineFuelLoad = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFineFuelLoad_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getPalmettoGallberyDeadFoliageLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyDeadFoliageLoad = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFoliageLoad_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getPalmettoGallberyDeadMediumFuelLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyDeadMediumFuelLoad = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadMediumFuelLoad_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getPalmettoGallberyFuelBedDepth'] = PalmettoGallberry.prototype.getPalmettoGallberyFuelBedDepth = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyFuelBedDepth_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getPalmettoGallberyLitterLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLitterLoad = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLitterLoad_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getPalmettoGallberyLiveFineFuelLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLiveFineFuelLoad = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFineFuelLoad_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getPalmettoGallberyLiveFoliageLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLiveFoliageLoad = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFoliageLoad_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['getPalmettoGallberyLiveMediumFuelLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLiveMediumFuelLoad = function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveMediumFuelLoad_0(self);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
PalmettoGallberry.prototype['__destroy__'] = PalmettoGallberry.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_PalmettoGallberry___destroy___0(self);
};

// Interface: WesternAspen

/** @suppress {undefinedVars, duplicate} @this{Object} */
function WesternAspen() {
  this.ptr = _emscripten_bind_WesternAspen_WesternAspen_0();
  getCache(WesternAspen)[this.ptr] = this;
};

WesternAspen.prototype = Object.create(WrapperObject.prototype);
WesternAspen.prototype.constructor = WesternAspen;
WesternAspen.prototype.__class__ = WesternAspen;
WesternAspen.__cache__ = {};
Module['WesternAspen'] = WesternAspen;
/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['initializeMembers'] = WesternAspen.prototype.initializeMembers = function() {
  var self = this.ptr;
  _emscripten_bind_WesternAspen_initializeMembers_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['calculateAspenMortality'] = WesternAspen.prototype.calculateAspenMortality = function(severity, flameLength, DBH) {
  var self = this.ptr;
  if (severity && typeof severity === 'object') severity = severity.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (DBH && typeof DBH === 'object') DBH = DBH.ptr;
  return _emscripten_bind_WesternAspen_calculateAspenMortality_3(self, severity, flameLength, DBH);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenFuelBedDepth'] = WesternAspen.prototype.getAspenFuelBedDepth = function(typeIndex) {
  var self = this.ptr;
  if (typeIndex && typeof typeIndex === 'object') typeIndex = typeIndex.ptr;
  return _emscripten_bind_WesternAspen_getAspenFuelBedDepth_1(self, typeIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenHeatOfCombustionDead'] = WesternAspen.prototype.getAspenHeatOfCombustionDead = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenHeatOfCombustionDead_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenHeatOfCombustionLive'] = WesternAspen.prototype.getAspenHeatOfCombustionLive = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenHeatOfCombustionLive_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenLoadDeadOneHour'] = WesternAspen.prototype.getAspenLoadDeadOneHour = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenLoadDeadTenHour'] = WesternAspen.prototype.getAspenLoadDeadTenHour = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenLoadLiveHerbaceous'] = WesternAspen.prototype.getAspenLoadLiveHerbaceous = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenLoadLiveWoody'] = WesternAspen.prototype.getAspenLoadLiveWoody = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenLoadLiveWoody_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenMoistureOfExtinctionDead'] = WesternAspen.prototype.getAspenMoistureOfExtinctionDead = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenMortality'] = WesternAspen.prototype.getAspenMortality = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenMortality_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenSavrDeadOneHour'] = WesternAspen.prototype.getAspenSavrDeadOneHour = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenSavrDeadTenHour'] = WesternAspen.prototype.getAspenSavrDeadTenHour = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenSavrLiveHerbaceous'] = WesternAspen.prototype.getAspenSavrLiveHerbaceous = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['getAspenSavrLiveWoody'] = WesternAspen.prototype.getAspenSavrLiveWoody = function() {
  var self = this.ptr;
  return _emscripten_bind_WesternAspen_getAspenSavrLiveWoody_0(self);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
WesternAspen.prototype['__destroy__'] = WesternAspen.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_WesternAspen___destroy___0(self);
};

// Interface: SIGCrown

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGCrown(fuelModels) {
  if (fuelModels && typeof fuelModels === 'object') fuelModels = fuelModels.ptr;
  this.ptr = _emscripten_bind_SIGCrown_SIGCrown_1(fuelModels);
  getCache(SIGCrown)[this.ptr] = this;
};

SIGCrown.prototype = Object.create(WrapperObject.prototype);
SIGCrown.prototype.constructor = SIGCrown;
SIGCrown.prototype.__class__ = SIGCrown;
SIGCrown.__cache__ = {};
Module['SIGCrown'] = SIGCrown;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFireType'] = SIGCrown.prototype.getFireType = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getFireType_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getIsMoistureScenarioDefinedByIndex'] = SIGCrown.prototype.getIsMoistureScenarioDefinedByIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return !!(_emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getIsMoistureScenarioDefinedByName'] = SIGCrown.prototype.getIsMoistureScenarioDefinedByName = function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return !!(_emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByName_1(self, name));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['isAllFuelLoadZero'] = SIGCrown.prototype.isAllFuelLoadZero = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGCrown_isAllFuelLoadZero_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['isFuelDynamic'] = SIGCrown.prototype.isFuelDynamic = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGCrown_isFuelDynamic_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['isFuelModelDefined'] = SIGCrown.prototype.isFuelModelDefined = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGCrown_isFuelModelDefined_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['isFuelModelReserved'] = SIGCrown.prototype.isFuelModelReserved = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return !!(_emscripten_bind_SIGCrown_isFuelModelReserved_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setCurrentMoistureScenarioByIndex'] = SIGCrown.prototype.setCurrentMoistureScenarioByIndex = function(moistureScenarioIndex) {
  var self = this.ptr;
  if (moistureScenarioIndex && typeof moistureScenarioIndex === 'object') moistureScenarioIndex = moistureScenarioIndex.ptr;
  return !!(_emscripten_bind_SIGCrown_setCurrentMoistureScenarioByIndex_1(self, moistureScenarioIndex));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setCurrentMoistureScenarioByName'] = SIGCrown.prototype.setCurrentMoistureScenarioByName = function(moistureScenarioName) {
  var self = this.ptr;
  ensureCache.prepare();
  if (moistureScenarioName && typeof moistureScenarioName === 'object') moistureScenarioName = moistureScenarioName.ptr;
  else moistureScenarioName = ensureString(moistureScenarioName);
  return !!(_emscripten_bind_SIGCrown_setCurrentMoistureScenarioByName_1(self, moistureScenarioName));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getAspect'] = SIGCrown.prototype.getAspect = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getAspect_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCanopyBaseHeight'] = SIGCrown.prototype.getCanopyBaseHeight = function(canopyHeightUnits) {
  var self = this.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  return _emscripten_bind_SIGCrown_getCanopyBaseHeight_1(self, canopyHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCanopyBulkDensity'] = SIGCrown.prototype.getCanopyBulkDensity = function(canopyBulkDensityUnits) {
  var self = this.ptr;
  if (canopyBulkDensityUnits && typeof canopyBulkDensityUnits === 'object') canopyBulkDensityUnits = canopyBulkDensityUnits.ptr;
  return _emscripten_bind_SIGCrown_getCanopyBulkDensity_1(self, canopyBulkDensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCanopyCover'] = SIGCrown.prototype.getCanopyCover = function(canopyFractionUnits) {
  var self = this.ptr;
  if (canopyFractionUnits && typeof canopyFractionUnits === 'object') canopyFractionUnits = canopyFractionUnits.ptr;
  return _emscripten_bind_SIGCrown_getCanopyCover_1(self, canopyFractionUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCanopyHeight'] = SIGCrown.prototype.getCanopyHeight = function(canopyHeighUnits) {
  var self = this.ptr;
  if (canopyHeighUnits && typeof canopyHeighUnits === 'object') canopyHeighUnits = canopyHeighUnits.ptr;
  return _emscripten_bind_SIGCrown_getCanopyHeight_1(self, canopyHeighUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCriticalOpenWindSpeed'] = SIGCrown.prototype.getCriticalOpenWindSpeed = function(speedUnits) {
  var self = this.ptr;
  if (speedUnits && typeof speedUnits === 'object') speedUnits = speedUnits.ptr;
  return _emscripten_bind_SIGCrown_getCriticalOpenWindSpeed_1(self, speedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownCriticalFireSpreadRate'] = SIGCrown.prototype.getCrownCriticalFireSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownCriticalFireSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownCriticalSurfaceFirelineIntensity'] = SIGCrown.prototype.getCrownCriticalSurfaceFirelineIntensity = function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownCriticalSurfaceFirelineIntensity_1(self, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownCriticalSurfaceFlameLength'] = SIGCrown.prototype.getCrownCriticalSurfaceFlameLength = function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownCriticalSurfaceFlameLength_1(self, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownFireActiveRatio'] = SIGCrown.prototype.getCrownFireActiveRatio = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireActiveRatio_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownFireArea'] = SIGCrown.prototype.getCrownFireArea = function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireArea_1(self, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownFirePerimeter'] = SIGCrown.prototype.getCrownFirePerimeter = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFirePerimeter_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownTransitionRatio'] = SIGCrown.prototype.getCrownTransitionRatio = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownTransitionRatio_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownFireLengthToWidthRatio'] = SIGCrown.prototype.getCrownFireLengthToWidthRatio = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireLengthToWidthRatio_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownFireSpreadDistance'] = SIGCrown.prototype.getCrownFireSpreadDistance = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireSpreadDistance_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownFireSpreadRate'] = SIGCrown.prototype.getCrownFireSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownFirelineIntensity'] = SIGCrown.prototype.getCrownFirelineIntensity = function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFirelineIntensity_1(self, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownFlameLength'] = SIGCrown.prototype.getCrownFlameLength = function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFlameLength_1(self, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownFractionBurned'] = SIGCrown.prototype.getCrownFractionBurned = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownFractionBurned_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getCrownRatio'] = SIGCrown.prototype.getCrownRatio = function(crownRatioUnits) {
  var self = this.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownRatio_1(self, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFinalFirelineIntesity'] = SIGCrown.prototype.getFinalFirelineIntesity = function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalFirelineIntesity_1(self, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFinalHeatPerUnitArea'] = SIGCrown.prototype.getFinalHeatPerUnitArea = function(heatPerUnitAreaUnits) {
  var self = this.ptr;
  if (heatPerUnitAreaUnits && typeof heatPerUnitAreaUnits === 'object') heatPerUnitAreaUnits = heatPerUnitAreaUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalHeatPerUnitArea_1(self, heatPerUnitAreaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFinalSpreadRate'] = SIGCrown.prototype.getFinalSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFinalSpreadDistance'] = SIGCrown.prototype.getFinalSpreadDistance = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalSpreadDistance_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFinalFireArea'] = SIGCrown.prototype.getFinalFireArea = function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalFireArea_1(self, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFinalFirePerimeter'] = SIGCrown.prototype.getFinalFirePerimeter = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalFirePerimeter_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelHeatOfCombustionDead'] = SIGCrown.prototype.getFuelHeatOfCombustionDead = function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelHeatOfCombustionDead_2(self, fuelModelNumber, heatOfCombustionUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelHeatOfCombustionLive'] = SIGCrown.prototype.getFuelHeatOfCombustionLive = function(fuelModelNumber, heatOfCombustionUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (heatOfCombustionUnits && typeof heatOfCombustionUnits === 'object') heatOfCombustionUnits = heatOfCombustionUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelHeatOfCombustionLive_2(self, fuelModelNumber, heatOfCombustionUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelLoadHundredHour'] = SIGCrown.prototype.getFuelLoadHundredHour = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadHundredHour_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelLoadLiveHerbaceous'] = SIGCrown.prototype.getFuelLoadLiveHerbaceous = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadLiveHerbaceous_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelLoadLiveWoody'] = SIGCrown.prototype.getFuelLoadLiveWoody = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadLiveWoody_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelLoadOneHour'] = SIGCrown.prototype.getFuelLoadOneHour = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadOneHour_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelLoadTenHour'] = SIGCrown.prototype.getFuelLoadTenHour = function(fuelModelNumber, loadingUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelLoadTenHour_2(self, fuelModelNumber, loadingUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelMoistureOfExtinctionDead'] = SIGCrown.prototype.getFuelMoistureOfExtinctionDead = function(fuelModelNumber, moistureUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelMoistureOfExtinctionDead_2(self, fuelModelNumber, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelSavrLiveHerbaceous'] = SIGCrown.prototype.getFuelSavrLiveHerbaceous = function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelSavrLiveHerbaceous_2(self, fuelModelNumber, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelSavrLiveWoody'] = SIGCrown.prototype.getFuelSavrLiveWoody = function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelSavrLiveWoody_2(self, fuelModelNumber, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelSavrOneHour'] = SIGCrown.prototype.getFuelSavrOneHour = function(fuelModelNumber, savrUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelSavrOneHour_2(self, fuelModelNumber, savrUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelbedDepth'] = SIGCrown.prototype.getFuelbedDepth = function(fuelModelNumber, lengthUnits) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getFuelbedDepth_2(self, fuelModelNumber, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureFoliar'] = SIGCrown.prototype.getMoistureFoliar = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureFoliar_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureHundredHour'] = SIGCrown.prototype.getMoistureHundredHour = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureHundredHour_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureLiveHerbaceous'] = SIGCrown.prototype.getMoistureLiveHerbaceous = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureLiveHerbaceous_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureLiveWoody'] = SIGCrown.prototype.getMoistureLiveWoody = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureLiveWoody_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureOneHour'] = SIGCrown.prototype.getMoistureOneHour = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureOneHour_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioHundredHourByIndex'] = SIGCrown.prototype.getMoistureScenarioHundredHourByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioHundredHourByName'] = SIGCrown.prototype.getMoistureScenarioHundredHourByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioLiveHerbaceousByIndex'] = SIGCrown.prototype.getMoistureScenarioLiveHerbaceousByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioLiveHerbaceousByName'] = SIGCrown.prototype.getMoistureScenarioLiveHerbaceousByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioLiveWoodyByIndex'] = SIGCrown.prototype.getMoistureScenarioLiveWoodyByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioLiveWoodyByName'] = SIGCrown.prototype.getMoistureScenarioLiveWoodyByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioOneHourByIndex'] = SIGCrown.prototype.getMoistureScenarioOneHourByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioOneHourByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioOneHourByName'] = SIGCrown.prototype.getMoistureScenarioOneHourByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioOneHourByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioTenHourByIndex'] = SIGCrown.prototype.getMoistureScenarioTenHourByIndex = function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioTenHourByIndex_2(self, index, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioTenHourByName'] = SIGCrown.prototype.getMoistureScenarioTenHourByName = function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioTenHourByName_2(self, name, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureTenHour'] = SIGCrown.prototype.getMoistureTenHour = function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureTenHour_1(self, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getSlope'] = SIGCrown.prototype.getSlope = function(slopeUnits) {
  var self = this.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  return _emscripten_bind_SIGCrown_getSlope_1(self, slopeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getSurfaceFireSpreadDistance'] = SIGCrown.prototype.getSurfaceFireSpreadDistance = function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getSurfaceFireSpreadDistance_1(self, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getSurfaceFireSpreadRate'] = SIGCrown.prototype.getSurfaceFireSpreadRate = function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGCrown_getSurfaceFireSpreadRate_1(self, spreadRateUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getWindDirection'] = SIGCrown.prototype.getWindDirection = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getWindDirection_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getWindSpeed'] = SIGCrown.prototype.getWindSpeed = function(windSpeedUnits, windHeightInputMode) {
  var self = this.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  return _emscripten_bind_SIGCrown_getWindSpeed_2(self, windSpeedUnits, windHeightInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelModelNumber'] = SIGCrown.prototype.getFuelModelNumber = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getFuelModelNumber_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioIndexByName'] = SIGCrown.prototype.getMoistureScenarioIndexByName = function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return _emscripten_bind_SIGCrown_getMoistureScenarioIndexByName_1(self, name);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getNumberOfMoistureScenarios'] = SIGCrown.prototype.getNumberOfMoistureScenarios = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getNumberOfMoistureScenarios_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelCode'] = SIGCrown.prototype.getFuelCode = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGCrown_getFuelCode_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFuelName'] = SIGCrown.prototype.getFuelName = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  return UTF8ToString(_emscripten_bind_SIGCrown_getFuelName_1(self, fuelModelNumber));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioDescriptionByIndex'] = SIGCrown.prototype.getMoistureScenarioDescriptionByIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioDescriptionByName'] = SIGCrown.prototype.getMoistureScenarioDescriptionByName = function(name) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  return UTF8ToString(_emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByName_1(self, name));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getMoistureScenarioNameByIndex'] = SIGCrown.prototype.getMoistureScenarioNameByIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGCrown_getMoistureScenarioNameByIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['doCrownRun'] = SIGCrown.prototype.doCrownRun = function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_doCrownRun_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['doCrownRunRothermel'] = SIGCrown.prototype.doCrownRunRothermel = function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_doCrownRunRothermel_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['doCrownRunScottAndReinhardt'] = SIGCrown.prototype.doCrownRunScottAndReinhardt = function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['initializeMembers'] = SIGCrown.prototype.initializeMembers = function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_initializeMembers_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setAspect'] = SIGCrown.prototype.setAspect = function(aspect) {
  var self = this.ptr;
  if (aspect && typeof aspect === 'object') aspect = aspect.ptr;
  _emscripten_bind_SIGCrown_setAspect_1(self, aspect);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setCanopyBaseHeight'] = SIGCrown.prototype.setCanopyBaseHeight = function(canopyBaseHeight, canopyHeightUnits) {
  var self = this.ptr;
  if (canopyBaseHeight && typeof canopyBaseHeight === 'object') canopyBaseHeight = canopyBaseHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  _emscripten_bind_SIGCrown_setCanopyBaseHeight_2(self, canopyBaseHeight, canopyHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setCanopyBulkDensity'] = SIGCrown.prototype.setCanopyBulkDensity = function(canopyBulkDensity, densityUnits) {
  var self = this.ptr;
  if (canopyBulkDensity && typeof canopyBulkDensity === 'object') canopyBulkDensity = canopyBulkDensity.ptr;
  if (densityUnits && typeof densityUnits === 'object') densityUnits = densityUnits.ptr;
  _emscripten_bind_SIGCrown_setCanopyBulkDensity_2(self, canopyBulkDensity, densityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setCanopyCover'] = SIGCrown.prototype.setCanopyCover = function(canopyCover, coverUnits) {
  var self = this.ptr;
  if (canopyCover && typeof canopyCover === 'object') canopyCover = canopyCover.ptr;
  if (coverUnits && typeof coverUnits === 'object') coverUnits = coverUnits.ptr;
  _emscripten_bind_SIGCrown_setCanopyCover_2(self, canopyCover, coverUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setCanopyHeight'] = SIGCrown.prototype.setCanopyHeight = function(canopyHeight, canopyHeightUnits) {
  var self = this.ptr;
  if (canopyHeight && typeof canopyHeight === 'object') canopyHeight = canopyHeight.ptr;
  if (canopyHeightUnits && typeof canopyHeightUnits === 'object') canopyHeightUnits = canopyHeightUnits.ptr;
  _emscripten_bind_SIGCrown_setCanopyHeight_2(self, canopyHeight, canopyHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setCrownRatio'] = SIGCrown.prototype.setCrownRatio = function(crownRatio, crownRatioUnits) {
  var self = this.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGCrown_setCrownRatio_2(self, crownRatio, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setFuelModelNumber'] = SIGCrown.prototype.setFuelModelNumber = function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  _emscripten_bind_SIGCrown_setFuelModelNumber_1(self, fuelModelNumber);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setCrownFireCalculationMethod'] = SIGCrown.prototype.setCrownFireCalculationMethod = function(CrownFireCalculationMethod) {
  var self = this.ptr;
  if (CrownFireCalculationMethod && typeof CrownFireCalculationMethod === 'object') CrownFireCalculationMethod = CrownFireCalculationMethod.ptr;
  _emscripten_bind_SIGCrown_setCrownFireCalculationMethod_1(self, CrownFireCalculationMethod);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setElapsedTime'] = SIGCrown.prototype.setElapsedTime = function(elapsedTime, timeUnits) {
  var self = this.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  _emscripten_bind_SIGCrown_setElapsedTime_2(self, elapsedTime, timeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setFuelModels'] = SIGCrown.prototype.setFuelModels = function(fuelModels) {
  var self = this.ptr;
  if (fuelModels && typeof fuelModels === 'object') fuelModels = fuelModels.ptr;
  _emscripten_bind_SIGCrown_setFuelModels_1(self, fuelModels);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureDeadAggregate'] = SIGCrown.prototype.setMoistureDeadAggregate = function(moistureDead, moistureUnits) {
  var self = this.ptr;
  if (moistureDead && typeof moistureDead === 'object') moistureDead = moistureDead.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureDeadAggregate_2(self, moistureDead, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureFoliar'] = SIGCrown.prototype.setMoistureFoliar = function(foliarMoisture, moistureUnits) {
  var self = this.ptr;
  if (foliarMoisture && typeof foliarMoisture === 'object') foliarMoisture = foliarMoisture.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureFoliar_2(self, foliarMoisture, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureHundredHour'] = SIGCrown.prototype.setMoistureHundredHour = function(moistureHundredHour, moistureUnits) {
  var self = this.ptr;
  if (moistureHundredHour && typeof moistureHundredHour === 'object') moistureHundredHour = moistureHundredHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureHundredHour_2(self, moistureHundredHour, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureInputMode'] = SIGCrown.prototype.setMoistureInputMode = function(moistureInputMode) {
  var self = this.ptr;
  if (moistureInputMode && typeof moistureInputMode === 'object') moistureInputMode = moistureInputMode.ptr;
  _emscripten_bind_SIGCrown_setMoistureInputMode_1(self, moistureInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureLiveAggregate'] = SIGCrown.prototype.setMoistureLiveAggregate = function(moistureLive, moistureUnits) {
  var self = this.ptr;
  if (moistureLive && typeof moistureLive === 'object') moistureLive = moistureLive.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureLiveAggregate_2(self, moistureLive, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureLiveHerbaceous'] = SIGCrown.prototype.setMoistureLiveHerbaceous = function(moistureLiveHerbaceous, moistureUnits) {
  var self = this.ptr;
  if (moistureLiveHerbaceous && typeof moistureLiveHerbaceous === 'object') moistureLiveHerbaceous = moistureLiveHerbaceous.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureLiveHerbaceous_2(self, moistureLiveHerbaceous, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureLiveWoody'] = SIGCrown.prototype.setMoistureLiveWoody = function(moistureLiveWoody, moistureUnits) {
  var self = this.ptr;
  if (moistureLiveWoody && typeof moistureLiveWoody === 'object') moistureLiveWoody = moistureLiveWoody.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureLiveWoody_2(self, moistureLiveWoody, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureOneHour'] = SIGCrown.prototype.setMoistureOneHour = function(moistureOneHour, moistureUnits) {
  var self = this.ptr;
  if (moistureOneHour && typeof moistureOneHour === 'object') moistureOneHour = moistureOneHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureOneHour_2(self, moistureOneHour, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureScenarios'] = SIGCrown.prototype.setMoistureScenarios = function(moistureScenarios) {
  var self = this.ptr;
  if (moistureScenarios && typeof moistureScenarios === 'object') moistureScenarios = moistureScenarios.ptr;
  _emscripten_bind_SIGCrown_setMoistureScenarios_1(self, moistureScenarios);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setMoistureTenHour'] = SIGCrown.prototype.setMoistureTenHour = function(moistureTenHour, moistureUnits) {
  var self = this.ptr;
  if (moistureTenHour && typeof moistureTenHour === 'object') moistureTenHour = moistureTenHour.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  _emscripten_bind_SIGCrown_setMoistureTenHour_2(self, moistureTenHour, moistureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setSlope'] = SIGCrown.prototype.setSlope = function(slope, slopeUnits) {
  var self = this.ptr;
  if (slope && typeof slope === 'object') slope = slope.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  _emscripten_bind_SIGCrown_setSlope_2(self, slope, slopeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setUserProvidedWindAdjustmentFactor'] = SIGCrown.prototype.setUserProvidedWindAdjustmentFactor = function(userProvidedWindAdjustmentFactor) {
  var self = this.ptr;
  if (userProvidedWindAdjustmentFactor && typeof userProvidedWindAdjustmentFactor === 'object') userProvidedWindAdjustmentFactor = userProvidedWindAdjustmentFactor.ptr;
  _emscripten_bind_SIGCrown_setUserProvidedWindAdjustmentFactor_1(self, userProvidedWindAdjustmentFactor);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setWindAdjustmentFactorCalculationMethod'] = SIGCrown.prototype.setWindAdjustmentFactorCalculationMethod = function(windAdjustmentFactorCalculationMethod) {
  var self = this.ptr;
  if (windAdjustmentFactorCalculationMethod && typeof windAdjustmentFactorCalculationMethod === 'object') windAdjustmentFactorCalculationMethod = windAdjustmentFactorCalculationMethod.ptr;
  _emscripten_bind_SIGCrown_setWindAdjustmentFactorCalculationMethod_1(self, windAdjustmentFactorCalculationMethod);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setWindAndSpreadOrientationMode'] = SIGCrown.prototype.setWindAndSpreadOrientationMode = function(windAndSpreadAngleMode) {
  var self = this.ptr;
  if (windAndSpreadAngleMode && typeof windAndSpreadAngleMode === 'object') windAndSpreadAngleMode = windAndSpreadAngleMode.ptr;
  _emscripten_bind_SIGCrown_setWindAndSpreadOrientationMode_1(self, windAndSpreadAngleMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setWindDirection'] = SIGCrown.prototype.setWindDirection = function(windDirection) {
  var self = this.ptr;
  if (windDirection && typeof windDirection === 'object') windDirection = windDirection.ptr;
  _emscripten_bind_SIGCrown_setWindDirection_1(self, windDirection);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setWindHeightInputMode'] = SIGCrown.prototype.setWindHeightInputMode = function(windHeightInputMode) {
  var self = this.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGCrown_setWindHeightInputMode_1(self, windHeightInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['setWindSpeed'] = SIGCrown.prototype.setWindSpeed = function(windSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGCrown_setWindSpeed_2(self, windSpeed, windSpeedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['updateCrownInputs'] = SIGCrown.prototype.updateCrownInputs = function(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureFoliar, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyBaseHeight, canopyHeightUnits, crownRatio, crownRatioUnits, canopyBulkDensity, densityUnits) {
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
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  if (canopyBulkDensity && typeof canopyBulkDensity === 'object') canopyBulkDensity = canopyBulkDensity.ptr;
  if (densityUnits && typeof densityUnits === 'object') densityUnits = densityUnits.ptr;
  _emscripten_bind_SIGCrown_updateCrownInputs_25(self, fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureFoliar, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyBaseHeight, canopyHeightUnits, crownRatio, crownRatioUnits, canopyBulkDensity, densityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['updateCrownsSurfaceInputs'] = SIGCrown.prototype.updateCrownsSurfaceInputs = function(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits) {
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
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGCrown_updateCrownsSurfaceInputs_21(self, fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['getFinalFlameLength'] = SIGCrown.prototype.getFinalFlameLength = function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalFlameLength_1(self, flameLengthUnits);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGCrown.prototype['__destroy__'] = SIGCrown.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown___destroy___0(self);
};

// Interface: SpeciesMasterTableRecord

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SpeciesMasterTableRecord(rhs) {
  if (rhs && typeof rhs === 'object') rhs = rhs.ptr;
  if (rhs === undefined) { this.ptr = _emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_0(); getCache(SpeciesMasterTableRecord)[this.ptr] = this;return }
  this.ptr = _emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_1(rhs);
  getCache(SpeciesMasterTableRecord)[this.ptr] = this;
};

SpeciesMasterTableRecord.prototype = Object.create(WrapperObject.prototype);
SpeciesMasterTableRecord.prototype.constructor = SpeciesMasterTableRecord;
SpeciesMasterTableRecord.prototype.__class__ = SpeciesMasterTableRecord;
SpeciesMasterTableRecord.__cache__ = {};
Module['SpeciesMasterTableRecord'] = SpeciesMasterTableRecord;

/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTableRecord.prototype['__destroy__'] = SpeciesMasterTableRecord.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SpeciesMasterTableRecord___destroy___0(self);
};

// Interface: SpeciesMasterTable

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SpeciesMasterTable() {
  this.ptr = _emscripten_bind_SpeciesMasterTable_SpeciesMasterTable_0();
  getCache(SpeciesMasterTable)[this.ptr] = this;
};

SpeciesMasterTable.prototype = Object.create(WrapperObject.prototype);
SpeciesMasterTable.prototype.constructor = SpeciesMasterTable;
SpeciesMasterTable.prototype.__class__ = SpeciesMasterTable;
SpeciesMasterTable.__cache__ = {};
Module['SpeciesMasterTable'] = SpeciesMasterTable;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTable.prototype['initializeMasterTable'] = SpeciesMasterTable.prototype.initializeMasterTable = function() {
  var self = this.ptr;
  _emscripten_bind_SpeciesMasterTable_initializeMasterTable_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTable.prototype['getSpeciesTableIndexFromSpeciesCode'] = SpeciesMasterTable.prototype.getSpeciesTableIndexFromSpeciesCode = function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCode_1(self, speciesCode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTable.prototype['getSpeciesTableIndexFromSpeciesCodeAndEquationType'] = SpeciesMasterTable.prototype.getSpeciesTableIndexFromSpeciesCodeAndEquationType = function(speciesCode, equationType) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return _emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2(self, speciesCode, equationType);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTable.prototype['insertRecord'] = SpeciesMasterTable.prototype.insertRecord = function(speciesCode, scientificName, commonName, mortalityEquation, brkEqu, crownCoefficientCode, region1, region2, region3, region4, equationType, crownDamageEquationCode) {
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
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SpeciesMasterTable.prototype['__destroy__'] = SpeciesMasterTable.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SpeciesMasterTable___destroy___0(self);
};

// Interface: SIGMortality

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGMortality(speciesMasterTable) {
  if (speciesMasterTable && typeof speciesMasterTable === 'object') speciesMasterTable = speciesMasterTable.ptr;
  this.ptr = _emscripten_bind_SIGMortality_SIGMortality_1(speciesMasterTable);
  getCache(SIGMortality)[this.ptr] = this;
};

SIGMortality.prototype = Object.create(WrapperObject.prototype);
SIGMortality.prototype.constructor = SIGMortality;
SIGMortality.prototype.__class__ = SIGMortality;
SIGMortality.__cache__ = {};
Module['SIGMortality'] = SIGMortality;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['initializeMembers'] = SIGMortality.prototype.initializeMembers = function() {
  var self = this.ptr;
  _emscripten_bind_SIGMortality_initializeMembers_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['checkIsInRegionAtSpeciesTableIndex'] = SIGMortality.prototype.checkIsInRegionAtSpeciesTableIndex = function(index, region) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  return !!(_emscripten_bind_SIGMortality_checkIsInRegionAtSpeciesTableIndex_2(self, index, region));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['checkIsInRegionFromSpeciesCode'] = SIGMortality.prototype.checkIsInRegionFromSpeciesCode = function(speciesCode, region) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  if (region && typeof region === 'object') region = region.ptr;
  return !!(_emscripten_bind_SIGMortality_checkIsInRegionFromSpeciesCode_2(self, speciesCode, region));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['updateInputsForSpeciesCodeAndEquationType'] = SIGMortality.prototype.updateInputsForSpeciesCodeAndEquationType = function(speciesCode, equationType) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return !!(_emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2(self, speciesCode, equationType));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['calculateMortality'] = SIGMortality.prototype.calculateMortality = function(probablityUnits) {
  var self = this.ptr;
  if (probablityUnits && typeof probablityUnits === 'object') probablityUnits = probablityUnits.ptr;
  return _emscripten_bind_SIGMortality_calculateMortality_1(self, probablityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['calculateScorchHeight'] = SIGMortality.prototype.calculateScorchHeight = function(firelineIntensity, firelineIntensityUnits, midFlameWindSpeed, windSpeedUnits, airTemperature, temperatureUnits, scorchHeightUnits) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  if (midFlameWindSpeed && typeof midFlameWindSpeed === 'object') midFlameWindSpeed = midFlameWindSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_calculateScorchHeight_7(self, firelineIntensity, firelineIntensityUnits, midFlameWindSpeed, windSpeedUnits, airTemperature, temperatureUnits, scorchHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['calculateMortalityAllDirections'] = SIGMortality.prototype.calculateMortalityAllDirections = function(probablityUnits) {
  var self = this.ptr;
  if (probablityUnits && typeof probablityUnits === 'object') probablityUnits = probablityUnits.ptr;
  _emscripten_bind_SIGMortality_calculateMortalityAllDirections_1(self, probablityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getRequiredFieldVector'] = SIGMortality.prototype.getRequiredFieldVector = function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGMortality_getRequiredFieldVector_0(self), BoolVector);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBeetleDamage'] = SIGMortality.prototype.getBeetleDamage = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getBeetleDamage_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCrownDamageEquationCode'] = SIGMortality.prototype.getCrownDamageEquationCode = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownDamageEquationCode_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCrownDamageEquationCodeAtSpeciesTableIndex'] = SIGMortality.prototype.getCrownDamageEquationCodeAtSpeciesTableIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getCrownDamageEquationCodeAtSpeciesTableIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCrownDamageEquationCodeFromSpeciesCode'] = SIGMortality.prototype.getCrownDamageEquationCodeFromSpeciesCode = function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getCrownDamageEquationCodeFromSpeciesCode_1(self, speciesCode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCrownDamageType'] = SIGMortality.prototype.getCrownDamageType = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownDamageType_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCommonNameAtSpeciesTableIndex'] = SIGMortality.prototype.getCommonNameAtSpeciesTableIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getCommonNameAtSpeciesTableIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCommonNameFromSpeciesCode'] = SIGMortality.prototype.getCommonNameFromSpeciesCode = function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return UTF8ToString(_emscripten_bind_SIGMortality_getCommonNameFromSpeciesCode_1(self, speciesCode));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getScientificNameAtSpeciesTableIndex'] = SIGMortality.prototype.getScientificNameAtSpeciesTableIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getScientificNameAtSpeciesTableIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getScientificNameFromSpeciesCode'] = SIGMortality.prototype.getScientificNameFromSpeciesCode = function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return UTF8ToString(_emscripten_bind_SIGMortality_getScientificNameFromSpeciesCode_1(self, speciesCode));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getSpeciesCode'] = SIGMortality.prototype.getSpeciesCode = function() {
  var self = this.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getSpeciesCode_0(self));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getSpeciesCodeAtSpeciesTableIndex'] = SIGMortality.prototype.getSpeciesCodeAtSpeciesTableIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getSpeciesCodeAtSpeciesTableIndex_1(self, index));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getEquationType'] = SIGMortality.prototype.getEquationType = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getEquationType_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getEquationTypeAtSpeciesTableIndex'] = SIGMortality.prototype.getEquationTypeAtSpeciesTableIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getEquationTypeAtSpeciesTableIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getEquationTypeFromSpeciesCode'] = SIGMortality.prototype.getEquationTypeFromSpeciesCode = function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getEquationTypeFromSpeciesCode_1(self, speciesCode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getFireSeverity'] = SIGMortality.prototype.getFireSeverity = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getFireSeverity_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getFlameLengthOrScorchHeightSwitch'] = SIGMortality.prototype.getFlameLengthOrScorchHeightSwitch = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightSwitch_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getRegion'] = SIGMortality.prototype.getRegion = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getRegion_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getSpeciesRecordVectorForRegion'] = SIGMortality.prototype.getSpeciesRecordVectorForRegion = function(region) {
  var self = this.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  return wrapPointer(_emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegion_1(self, region), SpeciesMasterTableRecordVector);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getSpeciesRecordVectorForRegionAndEquationType'] = SIGMortality.prototype.getSpeciesRecordVectorForRegionAndEquationType = function(region, equationType) {
  var self = this.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return wrapPointer(_emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegionAndEquationType_2(self, region, equationType), SpeciesMasterTableRecordVector);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBarkThickness'] = SIGMortality.prototype.getBarkThickness = function(barkThicknessUnits) {
  var self = this.ptr;
  if (barkThicknessUnits && typeof barkThicknessUnits === 'object') barkThicknessUnits = barkThicknessUnits.ptr;
  return _emscripten_bind_SIGMortality_getBarkThickness_1(self, barkThicknessUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBasalAreaKillled'] = SIGMortality.prototype.getBasalAreaKillled = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getBasalAreaKillled_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBasalAreaPostfire'] = SIGMortality.prototype.getBasalAreaPostfire = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getBasalAreaPostfire_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBasalAreaPrefire'] = SIGMortality.prototype.getBasalAreaPrefire = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getBasalAreaPrefire_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBoleCharHeight'] = SIGMortality.prototype.getBoleCharHeight = function(boleCharHeightUnits) {
  var self = this.ptr;
  if (boleCharHeightUnits && typeof boleCharHeightUnits === 'object') boleCharHeightUnits = boleCharHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getBoleCharHeight_1(self, boleCharHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBoleCharHeightBacking'] = SIGMortality.prototype.getBoleCharHeightBacking = function(boleCharHeightUnits) {
  var self = this.ptr;
  if (boleCharHeightUnits && typeof boleCharHeightUnits === 'object') boleCharHeightUnits = boleCharHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getBoleCharHeightBacking_1(self, boleCharHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBoleCharHeightFlanking'] = SIGMortality.prototype.getBoleCharHeightFlanking = function(boleCharHeightUnits) {
  var self = this.ptr;
  if (boleCharHeightUnits && typeof boleCharHeightUnits === 'object') boleCharHeightUnits = boleCharHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getBoleCharHeightFlanking_1(self, boleCharHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCambiumKillRating'] = SIGMortality.prototype.getCambiumKillRating = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCambiumKillRating_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCrownDamage'] = SIGMortality.prototype.getCrownDamage = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownDamage_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCrownRatio'] = SIGMortality.prototype.getCrownRatio = function(crownRatioUnits) {
  var self = this.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  return _emscripten_bind_SIGMortality_getCrownRatio_1(self, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCVSorCLS'] = SIGMortality.prototype.getCVSorCLS = function() {
  var self = this.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getCVSorCLS_0(self));
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getDBH'] = SIGMortality.prototype.getDBH = function(diameterUnits) {
  var self = this.ptr;
  if (diameterUnits && typeof diameterUnits === 'object') diameterUnits = diameterUnits.ptr;
  return _emscripten_bind_SIGMortality_getDBH_1(self, diameterUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getFlameLength'] = SIGMortality.prototype.getFlameLength = function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGMortality_getFlameLength_1(self, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getFlameLengthOrScorchHeightValue'] = SIGMortality.prototype.getFlameLengthOrScorchHeightValue = function(flameLengthOrScorchHeightUnits) {
  var self = this.ptr;
  if (flameLengthOrScorchHeightUnits && typeof flameLengthOrScorchHeightUnits === 'object') flameLengthOrScorchHeightUnits = flameLengthOrScorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightValue_1(self, flameLengthOrScorchHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getKilledTrees'] = SIGMortality.prototype.getKilledTrees = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getKilledTrees_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getProbabilityOfMortality'] = SIGMortality.prototype.getProbabilityOfMortality = function(probabilityUnits) {
  var self = this.ptr;
  if (probabilityUnits && typeof probabilityUnits === 'object') probabilityUnits = probabilityUnits.ptr;
  return _emscripten_bind_SIGMortality_getProbabilityOfMortality_1(self, probabilityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getProbabilityOfMortalityBacking'] = SIGMortality.prototype.getProbabilityOfMortalityBacking = function(probabilityUnits) {
  var self = this.ptr;
  if (probabilityUnits && typeof probabilityUnits === 'object') probabilityUnits = probabilityUnits.ptr;
  return _emscripten_bind_SIGMortality_getProbabilityOfMortalityBacking_1(self, probabilityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getProbabilityOfMortalityFlanking'] = SIGMortality.prototype.getProbabilityOfMortalityFlanking = function(probabilityUnits) {
  var self = this.ptr;
  if (probabilityUnits && typeof probabilityUnits === 'object') probabilityUnits = probabilityUnits.ptr;
  return _emscripten_bind_SIGMortality_getProbabilityOfMortalityFlanking_1(self, probabilityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getScorchHeight'] = SIGMortality.prototype.getScorchHeight = function(scorchHeightUnits) {
  var self = this.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getScorchHeight_1(self, scorchHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getScorchHeightBacking'] = SIGMortality.prototype.getScorchHeightBacking = function(scorchHeightUnits) {
  var self = this.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getScorchHeightBacking_1(self, scorchHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getScorchHeightFlanking'] = SIGMortality.prototype.getScorchHeightFlanking = function(scorchHeightUnits) {
  var self = this.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getScorchHeightFlanking_1(self, scorchHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getTotalPrefireTrees'] = SIGMortality.prototype.getTotalPrefireTrees = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getTotalPrefireTrees_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getTreeCrownLengthScorched'] = SIGMortality.prototype.getTreeCrownLengthScorched = function(treeCrownLengthScorchedUnits) {
  var self = this.ptr;
  if (treeCrownLengthScorchedUnits && typeof treeCrownLengthScorchedUnits === 'object') treeCrownLengthScorchedUnits = treeCrownLengthScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownLengthScorched_1(self, treeCrownLengthScorchedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getTreeCrownLengthScorchedBacking'] = SIGMortality.prototype.getTreeCrownLengthScorchedBacking = function(treeCrownLengthScorchedUnits) {
  var self = this.ptr;
  if (treeCrownLengthScorchedUnits && typeof treeCrownLengthScorchedUnits === 'object') treeCrownLengthScorchedUnits = treeCrownLengthScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownLengthScorchedBacking_1(self, treeCrownLengthScorchedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getTreeCrownLengthScorchedFlanking'] = SIGMortality.prototype.getTreeCrownLengthScorchedFlanking = function(treeCrownLengthScorchedUnits) {
  var self = this.ptr;
  if (treeCrownLengthScorchedUnits && typeof treeCrownLengthScorchedUnits === 'object') treeCrownLengthScorchedUnits = treeCrownLengthScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownLengthScorchedFlanking_1(self, treeCrownLengthScorchedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getTreeCrownVolumeScorched'] = SIGMortality.prototype.getTreeCrownVolumeScorched = function(getTreeCrownVolumeScorchedUnits) {
  var self = this.ptr;
  if (getTreeCrownVolumeScorchedUnits && typeof getTreeCrownVolumeScorchedUnits === 'object') getTreeCrownVolumeScorchedUnits = getTreeCrownVolumeScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownVolumeScorched_1(self, getTreeCrownVolumeScorchedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getTreeCrownVolumeScorchedBacking'] = SIGMortality.prototype.getTreeCrownVolumeScorchedBacking = function(getTreeCrownVolumeScorchedUnits) {
  var self = this.ptr;
  if (getTreeCrownVolumeScorchedUnits && typeof getTreeCrownVolumeScorchedUnits === 'object') getTreeCrownVolumeScorchedUnits = getTreeCrownVolumeScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownVolumeScorchedBacking_1(self, getTreeCrownVolumeScorchedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getTreeCrownVolumeScorchedFlanking'] = SIGMortality.prototype.getTreeCrownVolumeScorchedFlanking = function(getTreeCrownVolumeScorchedUnits) {
  var self = this.ptr;
  if (getTreeCrownVolumeScorchedUnits && typeof getTreeCrownVolumeScorchedUnits === 'object') getTreeCrownVolumeScorchedUnits = getTreeCrownVolumeScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownVolumeScorchedFlanking_1(self, getTreeCrownVolumeScorchedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getTreeDensityPerUnitArea'] = SIGMortality.prototype.getTreeDensityPerUnitArea = function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeDensityPerUnitArea_1(self, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getTreeHeight'] = SIGMortality.prototype.getTreeHeight = function(treeHeightUnits) {
  var self = this.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeHeight_1(self, treeHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['postfireCanopyCover'] = SIGMortality.prototype.postfireCanopyCover = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_postfireCanopyCover_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['prefireCanopyCover'] = SIGMortality.prototype.prefireCanopyCover = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_prefireCanopyCover_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBarkEquationNumberAtSpeciesTableIndex'] = SIGMortality.prototype.getBarkEquationNumberAtSpeciesTableIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getBarkEquationNumberAtSpeciesTableIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getBarkEquationNumberFromSpeciesCode'] = SIGMortality.prototype.getBarkEquationNumberFromSpeciesCode = function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getBarkEquationNumberFromSpeciesCode_1(self, speciesCode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCrownCoefficientCodeAtSpeciesTableIndex'] = SIGMortality.prototype.getCrownCoefficientCodeAtSpeciesTableIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getCrownCoefficientCodeAtSpeciesTableIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCrownCoefficientCodeFromSpeciesCode'] = SIGMortality.prototype.getCrownCoefficientCodeFromSpeciesCode = function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getCrownCoefficientCodeFromSpeciesCode_1(self, speciesCode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getCrownScorchOrBoleCharEquationNumber'] = SIGMortality.prototype.getCrownScorchOrBoleCharEquationNumber = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownScorchOrBoleCharEquationNumber_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getMortalityEquationNumberAtSpeciesTableIndex'] = SIGMortality.prototype.getMortalityEquationNumberAtSpeciesTableIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGMortality_getMortalityEquationNumberAtSpeciesTableIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getMortalityEquationNumberFromSpeciesCode'] = SIGMortality.prototype.getMortalityEquationNumberFromSpeciesCode = function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  return _emscripten_bind_SIGMortality_getMortalityEquationNumberFromSpeciesCode_1(self, speciesCode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getNumberOfRecordsInSpeciesTable'] = SIGMortality.prototype.getNumberOfRecordsInSpeciesTable = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getNumberOfRecordsInSpeciesTable_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getSpeciesTableIndexFromSpeciesCode'] = SIGMortality.prototype.getSpeciesTableIndexFromSpeciesCode = function(speciesNameCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesNameCode && typeof speciesNameCode === 'object') speciesNameCode = speciesNameCode.ptr;
  else speciesNameCode = ensureString(speciesNameCode);
  return _emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCode_1(self, speciesNameCode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['getSpeciesTableIndexFromSpeciesCodeAndEquationType'] = SIGMortality.prototype.getSpeciesTableIndexFromSpeciesCodeAndEquationType = function(speciesNameCode, equationType) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesNameCode && typeof speciesNameCode === 'object') speciesNameCode = speciesNameCode.ptr;
  else speciesNameCode = ensureString(speciesNameCode);
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return _emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2(self, speciesNameCode, equationType);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setAirTemperature'] = SIGMortality.prototype.setAirTemperature = function(airTemperature, temperatureUnits) {
  var self = this.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  _emscripten_bind_SIGMortality_setAirTemperature_2(self, airTemperature, temperatureUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setBeetleDamage'] = SIGMortality.prototype.setBeetleDamage = function(beetleDamage) {
  var self = this.ptr;
  if (beetleDamage && typeof beetleDamage === 'object') beetleDamage = beetleDamage.ptr;
  _emscripten_bind_SIGMortality_setBeetleDamage_1(self, beetleDamage);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setBoleCharHeight'] = SIGMortality.prototype.setBoleCharHeight = function(boleCharHeight, boleCharHeightUnits) {
  var self = this.ptr;
  if (boleCharHeight && typeof boleCharHeight === 'object') boleCharHeight = boleCharHeight.ptr;
  if (boleCharHeightUnits && typeof boleCharHeightUnits === 'object') boleCharHeightUnits = boleCharHeightUnits.ptr;
  _emscripten_bind_SIGMortality_setBoleCharHeight_2(self, boleCharHeight, boleCharHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setCambiumKillRating'] = SIGMortality.prototype.setCambiumKillRating = function(cambiumKillRating) {
  var self = this.ptr;
  if (cambiumKillRating && typeof cambiumKillRating === 'object') cambiumKillRating = cambiumKillRating.ptr;
  _emscripten_bind_SIGMortality_setCambiumKillRating_1(self, cambiumKillRating);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setCrownDamage'] = SIGMortality.prototype.setCrownDamage = function(crownDamage) {
  var self = this.ptr;
  if (crownDamage && typeof crownDamage === 'object') crownDamage = crownDamage.ptr;
  _emscripten_bind_SIGMortality_setCrownDamage_1(self, crownDamage);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setCrownRatio'] = SIGMortality.prototype.setCrownRatio = function(crownRatio, crownRatioUnits) {
  var self = this.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGMortality_setCrownRatio_2(self, crownRatio, crownRatioUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setDBH'] = SIGMortality.prototype.setDBH = function(dbh, diameterUnits) {
  var self = this.ptr;
  if (dbh && typeof dbh === 'object') dbh = dbh.ptr;
  if (diameterUnits && typeof diameterUnits === 'object') diameterUnits = diameterUnits.ptr;
  _emscripten_bind_SIGMortality_setDBH_2(self, dbh, diameterUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setEquationType'] = SIGMortality.prototype.setEquationType = function(equationType) {
  var self = this.ptr;
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  _emscripten_bind_SIGMortality_setEquationType_1(self, equationType);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setFireSeverity'] = SIGMortality.prototype.setFireSeverity = function(fireSeverity) {
  var self = this.ptr;
  if (fireSeverity && typeof fireSeverity === 'object') fireSeverity = fireSeverity.ptr;
  _emscripten_bind_SIGMortality_setFireSeverity_1(self, fireSeverity);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setFirelineIntensity'] = SIGMortality.prototype.setFirelineIntensity = function(firelineIntensity, firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  _emscripten_bind_SIGMortality_setFirelineIntensity_2(self, firelineIntensity, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setFlameLength'] = SIGMortality.prototype.setFlameLength = function(flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGMortality_setFlameLength_2(self, flameLength, flameLengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setFlameLengthOrScorchHeightSwitch'] = SIGMortality.prototype.setFlameLengthOrScorchHeightSwitch = function(flameLengthOrScorchHeightSwitch) {
  var self = this.ptr;
  if (flameLengthOrScorchHeightSwitch && typeof flameLengthOrScorchHeightSwitch === 'object') flameLengthOrScorchHeightSwitch = flameLengthOrScorchHeightSwitch.ptr;
  _emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightSwitch_1(self, flameLengthOrScorchHeightSwitch);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setFlameLengthOrScorchHeightValue'] = SIGMortality.prototype.setFlameLengthOrScorchHeightValue = function(flameLengthOrScorchHeightValue, flameLengthOrScorchHeightUnits) {
  var self = this.ptr;
  if (flameLengthOrScorchHeightValue && typeof flameLengthOrScorchHeightValue === 'object') flameLengthOrScorchHeightValue = flameLengthOrScorchHeightValue.ptr;
  if (flameLengthOrScorchHeightUnits && typeof flameLengthOrScorchHeightUnits === 'object') flameLengthOrScorchHeightUnits = flameLengthOrScorchHeightUnits.ptr;
  _emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightValue_2(self, flameLengthOrScorchHeightValue, flameLengthOrScorchHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setMidFlameWindSpeed'] = SIGMortality.prototype.setMidFlameWindSpeed = function(midFlameWindSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (midFlameWindSpeed && typeof midFlameWindSpeed === 'object') midFlameWindSpeed = midFlameWindSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGMortality_setMidFlameWindSpeed_2(self, midFlameWindSpeed, windSpeedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setRegion'] = SIGMortality.prototype.setRegion = function(region) {
  var self = this.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  _emscripten_bind_SIGMortality_setRegion_1(self, region);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setScorchHeight'] = SIGMortality.prototype.setScorchHeight = function(scorchHeight, scorchHeightUnits) {
  var self = this.ptr;
  if (scorchHeight && typeof scorchHeight === 'object') scorchHeight = scorchHeight.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  _emscripten_bind_SIGMortality_setScorchHeight_2(self, scorchHeight, scorchHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setSpeciesCode'] = SIGMortality.prototype.setSpeciesCode = function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  _emscripten_bind_SIGMortality_setSpeciesCode_1(self, speciesCode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setSurfaceFireFirelineIntensity'] = SIGMortality.prototype.setSurfaceFireFirelineIntensity = function(value, firelineIntensityUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFirelineIntensity_2(self, value, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setSurfaceFireFirelineIntensityBacking'] = SIGMortality.prototype.setSurfaceFireFirelineIntensityBacking = function(value, firelineIntensityUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFirelineIntensityBacking_2(self, value, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setSurfaceFireFirelineIntensityFlanking'] = SIGMortality.prototype.setSurfaceFireFirelineIntensityFlanking = function(value, firelineIntensityUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFirelineIntensityFlanking_2(self, value, firelineIntensityUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setSurfaceFireFlameLength'] = SIGMortality.prototype.setSurfaceFireFlameLength = function(value, lengthUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFlameLength_2(self, value, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setSurfaceFireFlameLengthBacking'] = SIGMortality.prototype.setSurfaceFireFlameLengthBacking = function(value, lengthUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFlameLengthBacking_2(self, value, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setSurfaceFireFlameLengthFlanking'] = SIGMortality.prototype.setSurfaceFireFlameLengthFlanking = function(value, lengthUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFlameLengthFlanking_2(self, value, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setSurfaceFireScorchHeight'] = SIGMortality.prototype.setSurfaceFireScorchHeight = function(value, lengthUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireScorchHeight_2(self, value, lengthUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setTreeDensityPerUnitArea'] = SIGMortality.prototype.setTreeDensityPerUnitArea = function(numberOfTrees, areaUnits) {
  var self = this.ptr;
  if (numberOfTrees && typeof numberOfTrees === 'object') numberOfTrees = numberOfTrees.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  _emscripten_bind_SIGMortality_setTreeDensityPerUnitArea_2(self, numberOfTrees, areaUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setTreeHeight'] = SIGMortality.prototype.setTreeHeight = function(treeHeight, treeHeightUnits) {
  var self = this.ptr;
  if (treeHeight && typeof treeHeight === 'object') treeHeight = treeHeight.ptr;
  if (treeHeightUnits && typeof treeHeightUnits === 'object') treeHeightUnits = treeHeightUnits.ptr;
  _emscripten_bind_SIGMortality_setTreeHeight_2(self, treeHeight, treeHeightUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setUserProvidedWindAdjustmentFactor'] = SIGMortality.prototype.setUserProvidedWindAdjustmentFactor = function(userProvidedWindAdjustmentFactor) {
  var self = this.ptr;
  if (userProvidedWindAdjustmentFactor && typeof userProvidedWindAdjustmentFactor === 'object') userProvidedWindAdjustmentFactor = userProvidedWindAdjustmentFactor.ptr;
  _emscripten_bind_SIGMortality_setUserProvidedWindAdjustmentFactor_1(self, userProvidedWindAdjustmentFactor);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setWindHeightInputMode'] = SIGMortality.prototype.setWindHeightInputMode = function(windHeightInputMode) {
  var self = this.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGMortality_setWindHeightInputMode_1(self, windHeightInputMode);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setWindSpeed'] = SIGMortality.prototype.setWindSpeed = function(windSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGMortality_setWindSpeed_2(self, windSpeed, windSpeedUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['setWindSpeedAndWindHeightInputMode'] = SIGMortality.prototype.setWindSpeedAndWindHeightInputMode = function(windwindSpeed, windSpeedUnits, windHeightInputMode, userProvidedWindAdjustmentFactor) {
  var self = this.ptr;
  if (windwindSpeed && typeof windwindSpeed === 'object') windwindSpeed = windwindSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  if (userProvidedWindAdjustmentFactor && typeof userProvidedWindAdjustmentFactor === 'object') userProvidedWindAdjustmentFactor = userProvidedWindAdjustmentFactor.ptr;
  _emscripten_bind_SIGMortality_setWindSpeedAndWindHeightInputMode_4(self, windwindSpeed, windSpeedUnits, windHeightInputMode, userProvidedWindAdjustmentFactor);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGMortality.prototype['__destroy__'] = SIGMortality.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGMortality___destroy___0(self);
};

// Interface: WindSpeedUtility

/** @suppress {undefinedVars, duplicate} @this{Object} */
function WindSpeedUtility() {
  this.ptr = _emscripten_bind_WindSpeedUtility_WindSpeedUtility_0();
  getCache(WindSpeedUtility)[this.ptr] = this;
};

WindSpeedUtility.prototype = Object.create(WrapperObject.prototype);
WindSpeedUtility.prototype.constructor = WindSpeedUtility;
WindSpeedUtility.prototype.__class__ = WindSpeedUtility;
WindSpeedUtility.__cache__ = {};
Module['WindSpeedUtility'] = WindSpeedUtility;
/** @suppress {undefinedVars, duplicate} @this{Object} */
WindSpeedUtility.prototype['windSpeedAtMidflame'] = WindSpeedUtility.prototype.windSpeedAtMidflame = function(windSpeedAtTwentyFeet, windAdjustmentFactor) {
  var self = this.ptr;
  if (windSpeedAtTwentyFeet && typeof windSpeedAtTwentyFeet === 'object') windSpeedAtTwentyFeet = windSpeedAtTwentyFeet.ptr;
  if (windAdjustmentFactor && typeof windAdjustmentFactor === 'object') windAdjustmentFactor = windAdjustmentFactor.ptr;
  return _emscripten_bind_WindSpeedUtility_windSpeedAtMidflame_2(self, windSpeedAtTwentyFeet, windAdjustmentFactor);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
WindSpeedUtility.prototype['windSpeedAtTwentyFeetFromTenMeter'] = WindSpeedUtility.prototype.windSpeedAtTwentyFeetFromTenMeter = function(windSpeedAtTenMeters) {
  var self = this.ptr;
  if (windSpeedAtTenMeters && typeof windSpeedAtTenMeters === 'object') windSpeedAtTenMeters = windSpeedAtTenMeters.ptr;
  return _emscripten_bind_WindSpeedUtility_windSpeedAtTwentyFeetFromTenMeter_1(self, windSpeedAtTenMeters);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
WindSpeedUtility.prototype['__destroy__'] = WindSpeedUtility.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_WindSpeedUtility___destroy___0(self);
};

// Interface: SIGFineDeadFuelMoistureTool

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGFineDeadFuelMoistureTool() {
  this.ptr = _emscripten_bind_SIGFineDeadFuelMoistureTool_SIGFineDeadFuelMoistureTool_0();
  getCache(SIGFineDeadFuelMoistureTool)[this.ptr] = this;
};

SIGFineDeadFuelMoistureTool.prototype = Object.create(WrapperObject.prototype);
SIGFineDeadFuelMoistureTool.prototype.constructor = SIGFineDeadFuelMoistureTool;
SIGFineDeadFuelMoistureTool.prototype.__class__ = SIGFineDeadFuelMoistureTool;
SIGFineDeadFuelMoistureTool.__cache__ = {};
Module['SIGFineDeadFuelMoistureTool'] = SIGFineDeadFuelMoistureTool;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['calculate'] = SIGFineDeadFuelMoistureTool.prototype.calculate = function() {
  var self = this.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_calculate_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['setTimeOfDayIndex'] = SIGFineDeadFuelMoistureTool.prototype.setTimeOfDayIndex = function(timeOfDayIndex) {
  var self = this.ptr;
  if (timeOfDayIndex && typeof timeOfDayIndex === 'object') timeOfDayIndex = timeOfDayIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setTimeOfDayIndex_1(self, timeOfDayIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['setSlopeIndex'] = SIGFineDeadFuelMoistureTool.prototype.setSlopeIndex = function(slopeIndex) {
  var self = this.ptr;
  if (slopeIndex && typeof slopeIndex === 'object') slopeIndex = slopeIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setSlopeIndex_1(self, slopeIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['setShadingIndex'] = SIGFineDeadFuelMoistureTool.prototype.setShadingIndex = function(shadingIndex) {
  var self = this.ptr;
  if (shadingIndex && typeof shadingIndex === 'object') shadingIndex = shadingIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setShadingIndex_1(self, shadingIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['setAspectIndex'] = SIGFineDeadFuelMoistureTool.prototype.setAspectIndex = function(aspectIndex) {
  var self = this.ptr;
  if (aspectIndex && typeof aspectIndex === 'object') aspectIndex = aspectIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setAspectIndex_1(self, aspectIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['setRHIndex'] = SIGFineDeadFuelMoistureTool.prototype.setRHIndex = function(relativeHumidityIndex) {
  var self = this.ptr;
  if (relativeHumidityIndex && typeof relativeHumidityIndex === 'object') relativeHumidityIndex = relativeHumidityIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setRHIndex_1(self, relativeHumidityIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['setElevationIndex'] = SIGFineDeadFuelMoistureTool.prototype.setElevationIndex = function(elevationIndex) {
  var self = this.ptr;
  if (elevationIndex && typeof elevationIndex === 'object') elevationIndex = elevationIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setElevationIndex_1(self, elevationIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['setDryBulbIndex'] = SIGFineDeadFuelMoistureTool.prototype.setDryBulbIndex = function(dryBulbIndex) {
  var self = this.ptr;
  if (dryBulbIndex && typeof dryBulbIndex === 'object') dryBulbIndex = dryBulbIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setDryBulbIndex_1(self, dryBulbIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['setMonthIndex'] = SIGFineDeadFuelMoistureTool.prototype.setMonthIndex = function(monthIndex) {
  var self = this.ptr;
  if (monthIndex && typeof monthIndex === 'object') monthIndex = monthIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setMonthIndex_1(self, monthIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getFineDeadFuelMoisture'] = SIGFineDeadFuelMoistureTool.prototype.getFineDeadFuelMoisture = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getFineDeadFuelMoisture_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getSlopeIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getSlopeIndexSize = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getSlopeIndexSize_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getElevationIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getElevationIndexSize = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getElevationIndexSize_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getMonthIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getMonthIndexSize = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getMonthIndexSize_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getDryBulbTemperatureIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getDryBulbTemperatureIndexSize = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getDryBulbTemperatureIndexSize_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getReferenceMoisture'] = SIGFineDeadFuelMoistureTool.prototype.getReferenceMoisture = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getReferenceMoisture_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['calculateByIndex'] = SIGFineDeadFuelMoistureTool.prototype.calculateByIndex = function(aspectIndex, dryBulbIndex, elevationIndex, monthIndex, relativeHumidityIndex, shadingIndex, slopeIndex, timeOfDayIndex) {
  var self = this.ptr;
  if (aspectIndex && typeof aspectIndex === 'object') aspectIndex = aspectIndex.ptr;
  if (dryBulbIndex && typeof dryBulbIndex === 'object') dryBulbIndex = dryBulbIndex.ptr;
  if (elevationIndex && typeof elevationIndex === 'object') elevationIndex = elevationIndex.ptr;
  if (monthIndex && typeof monthIndex === 'object') monthIndex = monthIndex.ptr;
  if (relativeHumidityIndex && typeof relativeHumidityIndex === 'object') relativeHumidityIndex = relativeHumidityIndex.ptr;
  if (shadingIndex && typeof shadingIndex === 'object') shadingIndex = shadingIndex.ptr;
  if (slopeIndex && typeof slopeIndex === 'object') slopeIndex = slopeIndex.ptr;
  if (timeOfDayIndex && typeof timeOfDayIndex === 'object') timeOfDayIndex = timeOfDayIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_calculateByIndex_8(self, aspectIndex, dryBulbIndex, elevationIndex, monthIndex, relativeHumidityIndex, shadingIndex, slopeIndex, timeOfDayIndex);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getTimeOfDayIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getTimeOfDayIndexSize = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getTimeOfDayIndexSize_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getCorrectionMoisture'] = SIGFineDeadFuelMoistureTool.prototype.getCorrectionMoisture = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getCorrectionMoisture_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getAspectIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getAspectIndexSize = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getAspectIndexSize_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getShadingIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getShadingIndexSize = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getShadingIndexSize_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['getRelativeHumidityIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getRelativeHumidityIndexSize = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getRelativeHumidityIndexSize_0(self);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGFineDeadFuelMoistureTool.prototype['__destroy__'] = SIGFineDeadFuelMoistureTool.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool___destroy___0(self);
};

// Interface: SIGSlopeTool

/** @suppress {undefinedVars, duplicate} @this{Object} */
function SIGSlopeTool() {
  this.ptr = _emscripten_bind_SIGSlopeTool_SIGSlopeTool_0();
  getCache(SIGSlopeTool)[this.ptr] = this;
};

SIGSlopeTool.prototype = Object.create(WrapperObject.prototype);
SIGSlopeTool.prototype.constructor = SIGSlopeTool;
SIGSlopeTool.prototype.__class__ = SIGSlopeTool;
SIGSlopeTool.__cache__ = {};
Module['SIGSlopeTool'] = SIGSlopeTool;
/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getCentimetersPerKilometerAtIndex'] = SIGSlopeTool.prototype.getCentimetersPerKilometerAtIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getCentimetersPerKilometerAtIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getCentimetersPerKilometerAtRepresentativeFraction'] = SIGSlopeTool.prototype.getCentimetersPerKilometerAtRepresentativeFraction = function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getCentimetersPerKilometerAtRepresentativeFraction_1(self, representativeFraction);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistance'] = SIGSlopeTool.prototype.getHorizontalDistance = function(horizontalDistanceIndex, mapDistanceUnits) {
  var self = this.ptr;
  if (horizontalDistanceIndex && typeof horizontalDistanceIndex === 'object') horizontalDistanceIndex = horizontalDistanceIndex.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistance_2(self, horizontalDistanceIndex, mapDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistanceAtIndex'] = SIGSlopeTool.prototype.getHorizontalDistanceAtIndex = function(index, mapDistanceUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceAtIndex_2(self, index, mapDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistanceFifteen'] = SIGSlopeTool.prototype.getHorizontalDistanceFifteen = function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceFifteen_1(self, mapDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistanceFourtyFive'] = SIGSlopeTool.prototype.getHorizontalDistanceFourtyFive = function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceFourtyFive_1(self, mapDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistanceMaxSlope'] = SIGSlopeTool.prototype.getHorizontalDistanceMaxSlope = function(slopeUnits) {
  var self = this.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceMaxSlope_1(self, slopeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistanceNinety'] = SIGSlopeTool.prototype.getHorizontalDistanceNinety = function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceNinety_1(self, mapDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistanceSeventy'] = SIGSlopeTool.prototype.getHorizontalDistanceSeventy = function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceSeventy_1(self, mapDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistanceSixty'] = SIGSlopeTool.prototype.getHorizontalDistanceSixty = function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceSixty_1(self, mapDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistanceThirty'] = SIGSlopeTool.prototype.getHorizontalDistanceThirty = function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceThirty_1(self, mapDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getHorizontalDistanceZero'] = SIGSlopeTool.prototype.getHorizontalDistanceZero = function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceZero_1(self, mapDistanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getInchesPerMileAtIndex'] = SIGSlopeTool.prototype.getInchesPerMileAtIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getInchesPerMileAtIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getInchesPerMileAtRepresentativeFraction'] = SIGSlopeTool.prototype.getInchesPerMileAtRepresentativeFraction = function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getInchesPerMileAtRepresentativeFraction_1(self, representativeFraction);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getKilometersPerCentimeterAtIndex'] = SIGSlopeTool.prototype.getKilometersPerCentimeterAtIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getKilometersPerCentimeterAtIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getKilometersPerCentimeterAtRepresentativeFraction'] = SIGSlopeTool.prototype.getKilometersPerCentimeterAtRepresentativeFraction = function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getKilometersPerCentimeterAtRepresentativeFraction_1(self, representativeFraction);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getMilesPerInchAtIndex'] = SIGSlopeTool.prototype.getMilesPerInchAtIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getMilesPerInchAtIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getMilesPerInchAtRepresentativeFraction'] = SIGSlopeTool.prototype.getMilesPerInchAtRepresentativeFraction = function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getMilesPerInchAtRepresentativeFraction_1(self, representativeFraction);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getSlopeElevationChangeFromMapMeasurements'] = SIGSlopeTool.prototype.getSlopeElevationChangeFromMapMeasurements = function(elevationUnits) {
  var self = this.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeElevationChangeFromMapMeasurements_1(self, elevationUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getSlopeFromMapMeasurements'] = SIGSlopeTool.prototype.getSlopeFromMapMeasurements = function(slopeUnits) {
  var self = this.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeFromMapMeasurements_1(self, slopeUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getSlopeHorizontalDistanceFromMapMeasurements'] = SIGSlopeTool.prototype.getSlopeHorizontalDistanceFromMapMeasurements = function(distanceUnits) {
  var self = this.ptr;
  if (distanceUnits && typeof distanceUnits === 'object') distanceUnits = distanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeHorizontalDistanceFromMapMeasurements_1(self, distanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getSlopeFromMapMeasurementsInDegrees'] = SIGSlopeTool.prototype.getSlopeFromMapMeasurementsInDegrees = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeFromMapMeasurementsInDegrees_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getSlopeFromMapMeasurementsInPercent'] = SIGSlopeTool.prototype.getSlopeFromMapMeasurementsInPercent = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeFromMapMeasurementsInPercent_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getNumberOfHorizontalDistances'] = SIGSlopeTool.prototype.getNumberOfHorizontalDistances = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSlopeTool_getNumberOfHorizontalDistances_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getNumberOfRepresentativeFractions'] = SIGSlopeTool.prototype.getNumberOfRepresentativeFractions = function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSlopeTool_getNumberOfRepresentativeFractions_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getRepresentativeFractionAtIndex'] = SIGSlopeTool.prototype.getRepresentativeFractionAtIndex = function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getRepresentativeFractionAtIndex_1(self, index);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['getRepresentativeFractionAtRepresentativeFraction'] = SIGSlopeTool.prototype.getRepresentativeFractionAtRepresentativeFraction = function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getRepresentativeFractionAtRepresentativeFraction_1(self, representativeFraction);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['calculateHorizontalDistance'] = SIGSlopeTool.prototype.calculateHorizontalDistance = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSlopeTool_calculateHorizontalDistance_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['calculateSlopeFromMapMeasurements'] = SIGSlopeTool.prototype.calculateSlopeFromMapMeasurements = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSlopeTool_calculateSlopeFromMapMeasurements_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['setCalculatedMapDistance'] = SIGSlopeTool.prototype.setCalculatedMapDistance = function(calculatedMapDistance, distanceUnits) {
  var self = this.ptr;
  if (calculatedMapDistance && typeof calculatedMapDistance === 'object') calculatedMapDistance = calculatedMapDistance.ptr;
  if (distanceUnits && typeof distanceUnits === 'object') distanceUnits = distanceUnits.ptr;
  _emscripten_bind_SIGSlopeTool_setCalculatedMapDistance_2(self, calculatedMapDistance, distanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['setContourInterval'] = SIGSlopeTool.prototype.setContourInterval = function(contourInterval, contourUnits) {
  var self = this.ptr;
  if (contourInterval && typeof contourInterval === 'object') contourInterval = contourInterval.ptr;
  if (contourUnits && typeof contourUnits === 'object') contourUnits = contourUnits.ptr;
  _emscripten_bind_SIGSlopeTool_setContourInterval_2(self, contourInterval, contourUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['setMapDistance'] = SIGSlopeTool.prototype.setMapDistance = function(mapDistance, distanceUnits) {
  var self = this.ptr;
  if (mapDistance && typeof mapDistance === 'object') mapDistance = mapDistance.ptr;
  if (distanceUnits && typeof distanceUnits === 'object') distanceUnits = distanceUnits.ptr;
  _emscripten_bind_SIGSlopeTool_setMapDistance_2(self, mapDistance, distanceUnits);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['setMapRepresentativeFraction'] = SIGSlopeTool.prototype.setMapRepresentativeFraction = function(mapRepresentativeFraction) {
  var self = this.ptr;
  if (mapRepresentativeFraction && typeof mapRepresentativeFraction === 'object') mapRepresentativeFraction = mapRepresentativeFraction.ptr;
  _emscripten_bind_SIGSlopeTool_setMapRepresentativeFraction_1(self, mapRepresentativeFraction);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['setMaxSlopeSteepness'] = SIGSlopeTool.prototype.setMaxSlopeSteepness = function(maxSlopeSteepness) {
  var self = this.ptr;
  if (maxSlopeSteepness && typeof maxSlopeSteepness === 'object') maxSlopeSteepness = maxSlopeSteepness.ptr;
  _emscripten_bind_SIGSlopeTool_setMaxSlopeSteepness_1(self, maxSlopeSteepness);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['setNumberOfContours'] = SIGSlopeTool.prototype.setNumberOfContours = function(numberOfContours) {
  var self = this.ptr;
  if (numberOfContours && typeof numberOfContours === 'object') numberOfContours = numberOfContours.ptr;
  _emscripten_bind_SIGSlopeTool_setNumberOfContours_1(self, numberOfContours);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
SIGSlopeTool.prototype['__destroy__'] = SIGSlopeTool.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_SIGSlopeTool___destroy___0(self);
};

// Interface: VaporPressureDeficitCalculator

/** @suppress {undefinedVars, duplicate} @this{Object} */
function VaporPressureDeficitCalculator() {
  this.ptr = _emscripten_bind_VaporPressureDeficitCalculator_VaporPressureDeficitCalculator_0();
  getCache(VaporPressureDeficitCalculator)[this.ptr] = this;
};

VaporPressureDeficitCalculator.prototype = Object.create(WrapperObject.prototype);
VaporPressureDeficitCalculator.prototype.constructor = VaporPressureDeficitCalculator;
VaporPressureDeficitCalculator.prototype.__class__ = VaporPressureDeficitCalculator;
VaporPressureDeficitCalculator.__cache__ = {};
Module['VaporPressureDeficitCalculator'] = VaporPressureDeficitCalculator;
/** @suppress {undefinedVars, duplicate} @this{Object} */
VaporPressureDeficitCalculator.prototype['runCalculation'] = VaporPressureDeficitCalculator.prototype.runCalculation = function() {
  var self = this.ptr;
  _emscripten_bind_VaporPressureDeficitCalculator_runCalculation_0(self);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
VaporPressureDeficitCalculator.prototype['setTemperature'] = VaporPressureDeficitCalculator.prototype.setTemperature = function(temperature, units) {
  var self = this.ptr;
  if (temperature && typeof temperature === 'object') temperature = temperature.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  _emscripten_bind_VaporPressureDeficitCalculator_setTemperature_2(self, temperature, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
VaporPressureDeficitCalculator.prototype['setRelativeHumidity'] = VaporPressureDeficitCalculator.prototype.setRelativeHumidity = function(relativeHumidity, units) {
  var self = this.ptr;
  if (relativeHumidity && typeof relativeHumidity === 'object') relativeHumidity = relativeHumidity.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  _emscripten_bind_VaporPressureDeficitCalculator_setRelativeHumidity_2(self, relativeHumidity, units);
};

/** @suppress {undefinedVars, duplicate} @this{Object} */
VaporPressureDeficitCalculator.prototype['getVaporPressureDeficit'] = VaporPressureDeficitCalculator.prototype.getVaporPressureDeficit = function(units) {
  var self = this.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_VaporPressureDeficitCalculator_getVaporPressureDeficit_1(self, units);
};


/** @suppress {undefinedVars, duplicate} @this{Object} */
VaporPressureDeficitCalculator.prototype['__destroy__'] = VaporPressureDeficitCalculator.prototype.__destroy__ = function() {
  var self = this.ptr;
  _emscripten_bind_VaporPressureDeficitCalculator___destroy___0(self);
};

(function() {
  function setupEnums() {
    
// $AreaUnits_AreaUnitsEnum

    Module['AreaUnits']['SquareFeet'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareFeet();

    Module['AreaUnits']['Acres'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_Acres();

    Module['AreaUnits']['Hectares'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_Hectares();

    Module['AreaUnits']['SquareMeters'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMeters();

    Module['AreaUnits']['SquareMiles'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMiles();

    Module['AreaUnits']['SquareKilometers'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareKilometers();

    
// $BasalAreaUnits_BasalAreaUnitsEnum

    Module['BasalAreaUnits']['SquareFeetPerAcre'] = _emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareFeetPerAcre();

    Module['BasalAreaUnits']['SquareMetersPerHectare'] = _emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareMetersPerHectare();

    
// $FractionUnits_FractionUnitsEnum

    Module['FractionUnits']['Fraction'] = _emscripten_enum_FractionUnits_FractionUnitsEnum_Fraction();

    Module['FractionUnits']['Percent'] = _emscripten_enum_FractionUnits_FractionUnitsEnum_Percent();

    
// $LengthUnits_LengthUnitsEnum

    Module['LengthUnits']['Feet'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Feet();

    Module['LengthUnits']['Inches'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Inches();

    Module['LengthUnits']['Millimeters'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Millimeters();

    Module['LengthUnits']['Centimeters'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Centimeters();

    Module['LengthUnits']['Meters'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Meters();

    Module['LengthUnits']['Chains'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Chains();

    Module['LengthUnits']['Miles'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Miles();

    Module['LengthUnits']['Kilometers'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Kilometers();

    
// $LoadingUnits_LoadingUnitsEnum

    Module['LoadingUnits']['PoundsPerSquareFoot'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_PoundsPerSquareFoot();

    Module['LoadingUnits']['TonsPerAcre'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonsPerAcre();

    Module['LoadingUnits']['TonnesPerHectare'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonnesPerHectare();

    Module['LoadingUnits']['KilogramsPerSquareMeter'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_KilogramsPerSquareMeter();

    
// $SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum

    Module['SurfaceAreaToVolumeUnits']['SquareFeetOverCubicFeet'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareFeetOverCubicFeet();

    Module['SurfaceAreaToVolumeUnits']['SquareMetersOverCubicMeters'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareMetersOverCubicMeters();

    Module['SurfaceAreaToVolumeUnits']['SquareInchesOverCubicInches'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareInchesOverCubicInches();

    Module['SurfaceAreaToVolumeUnits']['SquareCentimetersOverCubicCentimeters'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareCentimetersOverCubicCentimeters();

    
// $SpeedUnits_SpeedUnitsEnum

    Module['SpeedUnits']['FeetPerMinute'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_FeetPerMinute();

    Module['SpeedUnits']['ChainsPerHour'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_ChainsPerHour();

    Module['SpeedUnits']['MetersPerSecond'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerSecond();

    Module['SpeedUnits']['MetersPerMinute'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerMinute();

    Module['SpeedUnits']['MilesPerHour'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MilesPerHour();

    Module['SpeedUnits']['KilometersPerHour'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_KilometersPerHour();

    
// $PressureUnits_PressureUnitsEnum

    Module['PressureUnits']['Pascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_Pascal();

    Module['PressureUnits']['HectoPascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_HectoPascal();

    Module['PressureUnits']['KiloPascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_KiloPascal();

    Module['PressureUnits']['MegaPascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_MegaPascal();

    Module['PressureUnits']['GigaPascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_GigaPascal();

    Module['PressureUnits']['Bar'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_Bar();

    Module['PressureUnits']['Atmosphere'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_Atmosphere();

    Module['PressureUnits']['TechnicalAtmosphere'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_TechnicalAtmosphere();

    Module['PressureUnits']['PoundPerSquareInch'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_PoundPerSquareInch();

    
// $SlopeUnits_SlopeUnitsEnum

    Module['SlopeUnits']['Degrees'] = _emscripten_enum_SlopeUnits_SlopeUnitsEnum_Degrees();

    Module['SlopeUnits']['Percent'] = _emscripten_enum_SlopeUnits_SlopeUnitsEnum_Percent();

    
// $DensityUnits_DensityUnitsEnum

    Module['DensityUnits']['PoundsPerCubicFoot'] = _emscripten_enum_DensityUnits_DensityUnitsEnum_PoundsPerCubicFoot();

    Module['DensityUnits']['KilogramsPerCubicMeter'] = _emscripten_enum_DensityUnits_DensityUnitsEnum_KilogramsPerCubicMeter();

    
// $HeatOfCombustionUnits_HeatOfCombustionUnitsEnum

    Module['HeatOfCombustionUnits']['BtusPerPound'] = _emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_BtusPerPound();

    Module['HeatOfCombustionUnits']['KilojoulesPerKilogram'] = _emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_KilojoulesPerKilogram();

    
// $HeatSinkUnits_HeatSinkUnitsEnum

    Module['HeatSinkUnits']['BtusPerCubicFoot'] = _emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_BtusPerCubicFoot();

    Module['HeatSinkUnits']['KilojoulesPerCubicMeter'] = _emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_KilojoulesPerCubicMeter();

    
// $HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum

    Module['HeatPerUnitAreaUnits']['BtusPerSquareFoot'] = _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_BtusPerSquareFoot();

    Module['HeatPerUnitAreaUnits']['KilojoulesPerSquareMeter'] = _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilojoulesPerSquareMeter();

    Module['HeatPerUnitAreaUnits']['KilowattSecondsPerSquareMeter'] = _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilowattSecondsPerSquareMeter();

    
// $HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum

    Module['HeatSourceAndReactionIntensityUnits']['BtusPerSquareFootPerMinute'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerMinute();

    Module['HeatSourceAndReactionIntensityUnits']['BtusPerSquareFootPerSecond'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerSecond();

    Module['HeatSourceAndReactionIntensityUnits']['KilojoulesPerSquareMeterPerSecond'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerSecond();

    Module['HeatSourceAndReactionIntensityUnits']['KilojoulesPerSquareMeterPerMinute'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerMinute();

    Module['HeatSourceAndReactionIntensityUnits']['KilowattsPerSquareMeter'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilowattsPerSquareMeter();

    
// $FirelineIntensityUnits_FirelineIntensityUnitsEnum

    Module['FirelineIntensityUnits']['BtusPerFootPerSecond'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerSecond();

    Module['FirelineIntensityUnits']['BtusPerFootPerMinute'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerMinute();

    Module['FirelineIntensityUnits']['KilojoulesPerMeterPerSecond'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerSecond();

    Module['FirelineIntensityUnits']['KilojoulesPerMeterPerMinute'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerMinute();

    Module['FirelineIntensityUnits']['KilowattsPerMeter'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilowattsPerMeter();

    
// $TemperatureUnits_TemperatureUnitsEnum

    Module['TemperatureUnits']['Fahrenheit'] = _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Fahrenheit();

    Module['TemperatureUnits']['Celsius'] = _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Celsius();

    Module['TemperatureUnits']['Kelvin'] = _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Kelvin();

    
// $TimeUnits_TimeUnitsEnum

    Module['TimeUnits']['Minutes'] = _emscripten_enum_TimeUnits_TimeUnitsEnum_Minutes();

    Module['TimeUnits']['Seconds'] = _emscripten_enum_TimeUnits_TimeUnitsEnum_Seconds();

    Module['TimeUnits']['Hours'] = _emscripten_enum_TimeUnits_TimeUnitsEnum_Hours();

    
// $ContainTactic_ContainTacticEnum

    Module['HeadAttack'] = _emscripten_enum_ContainTactic_ContainTacticEnum_HeadAttack();

    Module['RearAttack'] = _emscripten_enum_ContainTactic_ContainTacticEnum_RearAttack();

    
// $ContainStatus_ContainStatusEnum

    Module['Unreported'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Unreported();

    Module['Reported'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Reported();

    Module['Attacked'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Attacked();

    Module['Contained'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Contained();

    Module['Overrun'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Overrun();

    Module['Exhausted'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Exhausted();

    Module['Overflow'] = _emscripten_enum_ContainStatus_ContainStatusEnum_Overflow();

    Module['SizeLimitExceeded'] = _emscripten_enum_ContainStatus_ContainStatusEnum_SizeLimitExceeded();

    Module['TimeLimitExceeded'] = _emscripten_enum_ContainStatus_ContainStatusEnum_TimeLimitExceeded();

    
// $ContainFlank_ContainFlankEnum

    Module['LeftFlank'] = _emscripten_enum_ContainFlank_ContainFlankEnum_LeftFlank();

    Module['RightFlank'] = _emscripten_enum_ContainFlank_ContainFlankEnum_RightFlank();

    Module['BothFlanks'] = _emscripten_enum_ContainFlank_ContainFlankEnum_BothFlanks();

    Module['NeitherFlank'] = _emscripten_enum_ContainFlank_ContainFlankEnum_NeitherFlank();

    
// $IgnitionFuelBedType_IgnitionFuelBedTypeEnum

    Module['PonderosaPineLitter'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PonderosaPineLitter();

    Module['PunkyWoodRottenChunky'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodRottenChunky();

    Module['PunkyWoodPowderDeep'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodPowderDeep();

    Module['PunkWoodPowderShallow'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkWoodPowderShallow();

    Module['LodgepolePineDuff'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_LodgepolePineDuff();

    Module['DouglasFirDuff'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_DouglasFirDuff();

    Module['HighAltitudeMixed'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_HighAltitudeMixed();

    Module['PeatMoss'] = _emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PeatMoss();

    
// $LightningCharge_LightningChargeEnum

    Module['Negative'] = _emscripten_enum_LightningCharge_LightningChargeEnum_Negative();

    Module['Positive'] = _emscripten_enum_LightningCharge_LightningChargeEnum_Positive();

    Module['Unknown'] = _emscripten_enum_LightningCharge_LightningChargeEnum_Unknown();

    
// $SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum

    Module['CLOSED'] = _emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_CLOSED();

    Module['OPEN'] = _emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_OPEN();

    
// $SpotTreeSpecies_SpotTreeSpeciesEnum

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

    
// $SpotFireLocation_SpotFireLocationEnum

    Module['MIDSLOPE_WINDWARD'] = _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_WINDWARD();

    Module['VALLEY_BOTTOM'] = _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_VALLEY_BOTTOM();

    Module['MIDSLOPE_LEEWARD'] = _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_LEEWARD();

    Module['RIDGE_TOP'] = _emscripten_enum_SpotFireLocation_SpotFireLocationEnum_RIDGE_TOP();

    
// $FuelLifeState_FuelLifeStateEnum

    Module['Dead'] = _emscripten_enum_FuelLifeState_FuelLifeStateEnum_Dead();

    Module['Live'] = _emscripten_enum_FuelLifeState_FuelLifeStateEnum_Live();

    
// $FuelConstantsEnum_FuelConstantsEnum

    Module['MaxLifeStates'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLifeStates();

    Module['MaxLiveSizeClasses'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLiveSizeClasses();

    Module['MaxDeadSizeClasses'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxDeadSizeClasses();

    Module['MaxParticles'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxParticles();

    Module['MaxSavrSizeClasses'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxSavrSizeClasses();

    Module['MaxFuelModels'] = _emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxFuelModels();

    
// $AspenFireSeverity_AspenFireSeverityEnum

    Module['Low'] = _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low();

    Module['Moderate'] = _emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate();

    
// $ChaparralFuelType_ChaparralFuelTypeEnum

    Module['NotSet'] = _emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_NotSet();

    Module['Chamise'] = _emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_Chamise();

    Module['MixedBrush'] = _emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_MixedBrush();

    
// $ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum

    Module['DirectFuelLoad'] = _emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_DirectFuelLoad();

    Module['FuelLoadFromDepthAndChaparralType'] = _emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_FuelLoadFromDepthAndChaparralType();

    
// $MoistureInputMode_MoistureInputModeEnum

    Module['BySizeClass'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_BySizeClass();

    Module['AllAggregate'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_AllAggregate();

    Module['DeadAggregateAndLiveSizeClass'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_DeadAggregateAndLiveSizeClass();

    Module['LiveAggregateAndDeadSizeClass'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_LiveAggregateAndDeadSizeClass();

    Module['MoistureScenario'] = _emscripten_enum_MoistureInputMode_MoistureInputModeEnum_MoistureScenario();

    
// $MoistureClassInput_MoistureClassInputEnum

    Module['OneHour'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_OneHour();

    Module['TenHour'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_TenHour();

    Module['HundredHour'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_HundredHour();

    Module['LiveHerbaceous'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveHerbaceous();

    Module['LiveWoody'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveWoody();

    Module['DeadAggregate'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_DeadAggregate();

    Module['LiveAggregate'] = _emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveAggregate();

    
// $SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum

    Module['FromIgnitionPoint'] = _emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromIgnitionPoint();

    Module['FromPerimeter'] = _emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromPerimeter();

    
// $TwoFuelModelsMethod_TwoFuelModelsMethodEnum

    Module['NoMethod'] = _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_NoMethod();

    Module['Arithmetic'] = _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Arithmetic();

    Module['Harmonic'] = _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Harmonic();

    Module['TwoDimensional'] = _emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_TwoDimensional();

    
// $WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum

    Module['Unsheltered'] = _emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Unsheltered();

    Module['Sheltered'] = _emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Sheltered();

    
// $WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum

    Module['UserInput'] = _emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UserInput();

    Module['UseCrownRatio'] = _emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UseCrownRatio();

    Module['DontUseCrownRatio'] = _emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_DontUseCrownRatio();

    
// $WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum

    Module['RelativeToUpslope'] = _emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToUpslope();

    Module['RelativeToNorth'] = _emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToNorth();

    
// $WindHeightInputMode_WindHeightInputModeEnum

    Module['DirectMidflame'] = _emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_DirectMidflame();

    Module['TwentyFoot'] = _emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TwentyFoot();

    Module['TenMeter'] = _emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TenMeter();

    
// $WindUpslopeAlignmentMode

    Module['NotAligned'] = _emscripten_enum_WindUpslopeAlignmentMode_NotAligned();

    Module['Aligned'] = _emscripten_enum_WindUpslopeAlignmentMode_Aligned();

    
// $SurfaceRunInDirectionOf

    Module['MaxSpread'] = _emscripten_enum_SurfaceRunInDirectionOf_MaxSpread();

    Module['DirectionOfInterest'] = _emscripten_enum_SurfaceRunInDirectionOf_DirectionOfInterest();

    
// $FireType_FireTypeEnum

    Module['Surface'] = _emscripten_enum_FireType_FireTypeEnum_Surface();

    Module['Torching'] = _emscripten_enum_FireType_FireTypeEnum_Torching();

    Module['ConditionalCrownFire'] = _emscripten_enum_FireType_FireTypeEnum_ConditionalCrownFire();

    Module['Crowning'] = _emscripten_enum_FireType_FireTypeEnum_Crowning();

    
// $BeetleDamage

    Module['not_set'] = _emscripten_enum_BeetleDamage_not_set();

    Module['no'] = _emscripten_enum_BeetleDamage_no();

    Module['yes'] = _emscripten_enum_BeetleDamage_yes();

    
// $CrownFireCalculationMethod

    Module['rothermel'] = _emscripten_enum_CrownFireCalculationMethod_rothermel();

    Module['scott_and_reinhardt'] = _emscripten_enum_CrownFireCalculationMethod_scott_and_reinhardt();

    
// $CrownDamageEquationCode

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

    
// $CrownDamageType

    Module['not_set'] = _emscripten_enum_CrownDamageType_not_set();

    Module['crown_length'] = _emscripten_enum_CrownDamageType_crown_length();

    Module['crown_volume'] = _emscripten_enum_CrownDamageType_crown_volume();

    Module['crown_kill'] = _emscripten_enum_CrownDamageType_crown_kill();

    
// $EquationType

    Module['not_set'] = _emscripten_enum_EquationType_not_set();

    Module['crown_scorch'] = _emscripten_enum_EquationType_crown_scorch();

    Module['bole_char'] = _emscripten_enum_EquationType_bole_char();

    Module['crown_damage'] = _emscripten_enum_EquationType_crown_damage();

    
// $FireSeverity

    Module['not_set'] = _emscripten_enum_FireSeverity_not_set();

    Module['empty'] = _emscripten_enum_FireSeverity_empty();

    Module['low'] = _emscripten_enum_FireSeverity_low();

    
// $FlameLengthOrScorchHeightSwitch

    Module['flame_length'] = _emscripten_enum_FlameLengthOrScorchHeightSwitch_flame_length();

    Module['scorch_height'] = _emscripten_enum_FlameLengthOrScorchHeightSwitch_scorch_height();

    
// $RegionCode

    Module['interior_west'] = _emscripten_enum_RegionCode_interior_west();

    Module['pacific_west'] = _emscripten_enum_RegionCode_pacific_west();

    Module['north_east'] = _emscripten_enum_RegionCode_north_east();

    Module['south_east'] = _emscripten_enum_RegionCode_south_east();

    
// $RequiredFieldNames

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

    
// $FDFMToolAspectIndex_AspectIndexEnum

    Module['NORTH'] = _emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_NORTH();

    Module['EAST'] = _emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_EAST();

    Module['SOUTH'] = _emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_SOUTH();

    Module['WEST'] = _emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_WEST();

    
// $FDFMToolDryBulbIndex_DryBulbIndexEnum

    Module['TEN_TO_TWENTY_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_TEN_TO_TWENTY_NINE_DEGREES_F();

    Module['THRITY_TO_FOURTY_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_THRITY_TO_FOURTY_NINE_DEGREES_F();

    Module['FIFTY_TO_SIXTY_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_FIFTY_TO_SIXTY_NINE_DEGREES_F();

    Module['SEVENTY_TO_EIGHTY_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_SEVENTY_TO_EIGHTY_NINE_DEGREES_F();

    Module['NINETY_TO_ONE_HUNDRED_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_NINETY_TO_ONE_HUNDRED_NINE_DEGREES_F();

    Module['GREATER_THAN_ONE_HUNDRED_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_GREATER_THAN_ONE_HUNDRED_NINE_DEGREES_F();

    
// $FDFMToolElevationIndex_ElevationIndexEnum

    Module['BELOW_1000_TO_2000_FT'] = _emscripten_enum_FDFMToolElevationIndex_ElevationIndexEnum_BELOW_1000_TO_2000_FT();

    Module['LEVEL_WITHIN_1000_FT'] = _emscripten_enum_FDFMToolElevationIndex_ElevationIndexEnum_LEVEL_WITHIN_1000_FT();

    Module['ABOVE_1000_TO_2000_FT'] = _emscripten_enum_FDFMToolElevationIndex_ElevationIndexEnum_ABOVE_1000_TO_2000_FT();

    
// $FDFMToolMonthIndex_MonthIndexEnum

    Module['MAY_JUNE_JULY'] = _emscripten_enum_FDFMToolMonthIndex_MonthIndexEnum_MAY_JUNE_JULY();

    Module['FEB_MAR_APR_AUG_SEP_OCT'] = _emscripten_enum_FDFMToolMonthIndex_MonthIndexEnum_FEB_MAR_APR_AUG_SEP_OCT();

    Module['NOV_DEC_JAN'] = _emscripten_enum_FDFMToolMonthIndex_MonthIndexEnum_NOV_DEC_JAN();

    
// $FDFMToolRHIndex_RHIndexEnum

    Module['ZERO_TO_FOUR_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_ZERO_TO_FOUR_PERCENT();

    Module['FIVE_TO_NINE_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FIVE_TO_NINE_PERCENT();

    Module['TEN_TO_FOURTEEN_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_TEN_TO_FOURTEEN_PERCENT();

    Module['FIFTEEN_TO_NINETEEN_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FIFTEEN_TO_NINETEEN_PERCENT();

    Module['TWENTY_TO_TWENTY_FOUR_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_TWENTY_TO_TWENTY_FOUR_PERCENT();

    Module['TWENTY_FIVE_TO_TWENTY_NINE_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_TWENTY_FIVE_TO_TWENTY_NINE_PERCENT();

    Module['THIRTY_TO_THIRTY_FOUR_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_THIRTY_TO_THIRTY_FOUR_PERCENT();

    Module['THIRTY_FIVE_TO_THIRTY_NINE_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_THIRTY_FIVE_TO_THIRTY_NINE_PERCENT();

    Module['FORTY_TO_FORTY_FOUR_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FORTY_TO_FORTY_FOUR_PERCENT();

    Module['FORTY_FIVE_TO_FORTY_NINE_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FORTY_FIVE_TO_FORTY_NINE_PERCENT();

    Module['FIFTY_TO_FIFTY_FOUR_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FIFTY_TO_FIFTY_FOUR_PERCENT();

    Module['FIFTY_FIVE_TO_FIFTY_NINE_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FIFTY_FIVE_TO_FIFTY_NINE_PERCENT();

    Module['SIXTY_TO_SIXTY_FOUR_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_SIXTY_TO_SIXTY_FOUR_PERCENT();

    Module['SIXTY_FIVE_TO_SIXTY_NINE_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_SIXTY_FIVE_TO_SIXTY_NINE_PERCENT();

    Module['SEVENTY_TO_SEVENTY_FOUR_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_SEVENTY_TO_SEVENTY_FOUR_PERCENT();

    Module['SEVENTY_FIVE_TO_SEVENTY_NINE_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_SEVENTY_FIVE_TO_SEVENTY_NINE_PERCENT();

    Module['EIGHTY_TO_EIGHTY_FOUR_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_EIGHTY_TO_EIGHTY_FOUR_PERCENT();

    Module['EIGHTY_FIVE_TO_EIGHTY_NINE_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_EIGHTY_FIVE_TO_EIGHTY_NINE_PERCENT();

    Module['NINETY_TO_NINETY_FOUR_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_NINETY_TO_NINETY_FOUR_PERCENT();

    Module['NINETY_FIVE_TO_NINETY_NINE_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_NINETY_FIVE_TO_NINETY_NINE_PERCENT();

    Module['ONE_HUNDRED_PERCENT'] = _emscripten_enum_FDFMToolRHIndex_RHIndexEnum_ONE_HUNDRED_PERCENT();

    
// $FDFMToolShadingIndex_ShadingIndexEnum

    Module['EXPOSED'] = _emscripten_enum_FDFMToolShadingIndex_ShadingIndexEnum_EXPOSED();

    Module['SHADED'] = _emscripten_enum_FDFMToolShadingIndex_ShadingIndexEnum_SHADED();

    
// $FDFMToolSlopeIndex_SlopeIndexEnum

    Module['ZERO_TO_THIRTY_PERCENT'] = _emscripten_enum_FDFMToolSlopeIndex_SlopeIndexEnum_ZERO_TO_THIRTY_PERCENT();

    Module['GREATER_THAN_OR_EQUAL_TO_THIRTY_ONE_PERCENT'] = _emscripten_enum_FDFMToolSlopeIndex_SlopeIndexEnum_GREATER_THAN_OR_EQUAL_TO_THIRTY_ONE_PERCENT();

    
// $FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum

    Module['EIGHT_HUNDRED_HOURS_TO_NINE_HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_EIGHT_HUNDRED_HOURS_TO_NINE_HUNDRED_FIFTY_NINE();

    Module['TEN_HUNDRED_HOURS_TO_ELEVEN__HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_TEN_HUNDRED_HOURS_TO_ELEVEN__HUNDRED_FIFTY_NINE();

    Module['TWELVE_HUNDRED_HOURS_TO_THIRTEEN_HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_TWELVE_HUNDRED_HOURS_TO_THIRTEEN_HUNDRED_FIFTY_NINE();

    Module['FOURTEEN_HUNDRED_HOURS_TO_FIFTEEN_HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_FOURTEEN_HUNDRED_HOURS_TO_FIFTEEN_HUNDRED_FIFTY_NINE();

    Module['SIXTEEN_HUNDRED_HOURS_TO_SIXTEEN_HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_SIXTEEN_HUNDRED_HOURS_TO_SIXTEEN_HUNDRED_FIFTY_NINE();

    Module['EIGHTTEEN_HUNDRED_HOURS_TO_SUNSET'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_EIGHTTEEN_HUNDRED_HOURS_TO_SUNSET();

    
// $RepresentativeFraction_RepresentativeFractionEnum

    Module['NINTEEN_HUNDRED_EIGHTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_NINTEEN_HUNDRED_EIGHTY();

    Module['THREE_THOUSAND_NINEHUNDRED_SIXTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_THREE_THOUSAND_NINEHUNDRED_SIXTY();

    Module['SEVEN_THOUSAND_NINEHUNDRED_TWENTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_SEVEN_THOUSAND_NINEHUNDRED_TWENTY();

    Module['TEN_THOUSAND'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TEN_THOUSAND();

    Module['FIFTEEN_THOUSAND_EIGHT_HUNDRED_FORTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_FIFTEEN_THOUSAND_EIGHT_HUNDRED_FORTY();

    Module['TWENTY_ONE_THOUSAND_ONE_HUNDRED_TWENTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TWENTY_ONE_THOUSAND_ONE_HUNDRED_TWENTY();

    Module['TWENTY_FOUR_THOUSAND'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TWENTY_FOUR_THOUSAND();

    Module['THRITY_ONE_THOUSAND_SIX_HUNDRED_EIGHTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_THRITY_ONE_THOUSAND_SIX_HUNDRED_EIGHTY();

    Module['FIFTY_THOUSAND'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_FIFTY_THOUSAND();

    Module['SIXTY_TWO_THOUSAND_FIVE_HUNDRED'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_SIXTY_TWO_THOUSAND_FIVE_HUNDRED();

    Module['SIXTY_THREE_THOUSAND_THREE_HUNDRED_SIXTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_SIXTY_THREE_THOUSAND_THREE_HUNDRED_SIXTY();

    Module['ONE_HUNDRED_THOUSAND'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_ONE_HUNDRED_THOUSAND();

    Module['ONE_HUNDRED_TWENTY_SIX_THOUSAND_SEVEN_HUNDRED_TWENTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_ONE_HUNDRED_TWENTY_SIX_THOUSAND_SEVEN_HUNDRED_TWENTY();

    Module['TWO_HUNDRED_FIFTY_THOUSAND'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TWO_HUNDRED_FIFTY_THOUSAND();

    Module['TWO_HUNDRED_FIFTY_THREE_THOUSAND_FOUR_HUNDRED_FORTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TWO_HUNDRED_FIFTY_THREE_THOUSAND_FOUR_HUNDRED_FORTY();

    Module['FIVE_HUNDRED_SIX_THOUSAND_EIGHT_HUNDRED_EIGHTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_FIVE_HUNDRED_SIX_THOUSAND_EIGHT_HUNDRED_EIGHTY();

    Module['ONE_MILLION'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_ONE_MILLION();

    Module['ONE_MILLION_THIRTEEN_THOUSAND_SEVEN_HUNDRED_SIXTY'] = _emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_ONE_MILLION_THIRTEEN_THOUSAND_SEVEN_HUNDRED_SIXTY();

    
// $HorizontalDistanceIndex_HorizontalDistanceIndexEnum

    Module['UPSLOPE_ZERO_DEGREES'] = _emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_UPSLOPE_ZERO_DEGREES();

    Module['FIFTEEN_DEGREES_FROM_UPSLOPE'] = _emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_FIFTEEN_DEGREES_FROM_UPSLOPE();

    Module['THIRTY_DEGREES_FROM_UPSLOPE'] = _emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_THIRTY_DEGREES_FROM_UPSLOPE();

    Module['FORTY_FIVE_DEGREES_FROM_UPSLOPE'] = _emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_FORTY_FIVE_DEGREES_FROM_UPSLOPE();

    Module['SIXTY_DEGREES_FROM_UPSLOPE'] = _emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_SIXTY_DEGREES_FROM_UPSLOPE();

    Module['SEVENTY_FIVE_DEGREES_FROM_UPSLOPE'] = _emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_SEVENTY_FIVE_DEGREES_FROM_UPSLOPE();

    Module['CROSS_SLOPE_NINETY_DEGREES'] = _emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_CROSS_SLOPE_NINETY_DEGREES();

  }
  if (runtimeInitialized) setupEnums();
  else addOnInit(setupEnums);
})();
