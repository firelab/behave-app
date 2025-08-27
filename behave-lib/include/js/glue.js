
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
// AreaUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function AreaUnits() { throw "cannot construct a AreaUnits, no constructor in IDL" }
AreaUnits.prototype = Object.create(WrapperObject.prototype);
AreaUnits.prototype.constructor = AreaUnits;
AreaUnits.prototype.__class__ = AreaUnits;
AreaUnits.__cache__ = {};
Module['AreaUnits'] = AreaUnits;

AreaUnits.prototype['toBaseUnits'] = AreaUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_AreaUnits_toBaseUnits_2(self, value, units);
};;

AreaUnits.prototype['fromBaseUnits'] = AreaUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_AreaUnits_fromBaseUnits_2(self, value, units);
};;

  AreaUnits.prototype['__destroy__'] = AreaUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_AreaUnits___destroy___0(self);
};
// BasalAreaUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function BasalAreaUnits() { throw "cannot construct a BasalAreaUnits, no constructor in IDL" }
BasalAreaUnits.prototype = Object.create(WrapperObject.prototype);
BasalAreaUnits.prototype.constructor = BasalAreaUnits;
BasalAreaUnits.prototype.__class__ = BasalAreaUnits;
BasalAreaUnits.__cache__ = {};
Module['BasalAreaUnits'] = BasalAreaUnits;

BasalAreaUnits.prototype['toBaseUnits'] = BasalAreaUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_BasalAreaUnits_toBaseUnits_2(self, value, units);
};;

BasalAreaUnits.prototype['fromBaseUnits'] = BasalAreaUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_BasalAreaUnits_fromBaseUnits_2(self, value, units);
};;

  BasalAreaUnits.prototype['__destroy__'] = BasalAreaUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_BasalAreaUnits___destroy___0(self);
};
// FractionUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function FractionUnits() { throw "cannot construct a FractionUnits, no constructor in IDL" }
FractionUnits.prototype = Object.create(WrapperObject.prototype);
FractionUnits.prototype.constructor = FractionUnits;
FractionUnits.prototype.__class__ = FractionUnits;
FractionUnits.__cache__ = {};
Module['FractionUnits'] = FractionUnits;

FractionUnits.prototype['toBaseUnits'] = FractionUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_FractionUnits_toBaseUnits_2(self, value, units);
};;

FractionUnits.prototype['fromBaseUnits'] = FractionUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_FractionUnits_fromBaseUnits_2(self, value, units);
};;

  FractionUnits.prototype['__destroy__'] = FractionUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_FractionUnits___destroy___0(self);
};
// LengthUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function LengthUnits() { throw "cannot construct a LengthUnits, no constructor in IDL" }
LengthUnits.prototype = Object.create(WrapperObject.prototype);
LengthUnits.prototype.constructor = LengthUnits;
LengthUnits.prototype.__class__ = LengthUnits;
LengthUnits.__cache__ = {};
Module['LengthUnits'] = LengthUnits;

LengthUnits.prototype['toBaseUnits'] = LengthUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_LengthUnits_toBaseUnits_2(self, value, units);
};;

LengthUnits.prototype['fromBaseUnits'] = LengthUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_LengthUnits_fromBaseUnits_2(self, value, units);
};;

  LengthUnits.prototype['__destroy__'] = LengthUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_LengthUnits___destroy___0(self);
};
// LoadingUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function LoadingUnits() { throw "cannot construct a LoadingUnits, no constructor in IDL" }
LoadingUnits.prototype = Object.create(WrapperObject.prototype);
LoadingUnits.prototype.constructor = LoadingUnits;
LoadingUnits.prototype.__class__ = LoadingUnits;
LoadingUnits.__cache__ = {};
Module['LoadingUnits'] = LoadingUnits;

LoadingUnits.prototype['toBaseUnits'] = LoadingUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_LoadingUnits_toBaseUnits_2(self, value, units);
};;

LoadingUnits.prototype['fromBaseUnits'] = LoadingUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_LoadingUnits_fromBaseUnits_2(self, value, units);
};;

  LoadingUnits.prototype['__destroy__'] = LoadingUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_LoadingUnits___destroy___0(self);
};
// SurfaceAreaToVolumeUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function SurfaceAreaToVolumeUnits() { throw "cannot construct a SurfaceAreaToVolumeUnits, no constructor in IDL" }
SurfaceAreaToVolumeUnits.prototype = Object.create(WrapperObject.prototype);
SurfaceAreaToVolumeUnits.prototype.constructor = SurfaceAreaToVolumeUnits;
SurfaceAreaToVolumeUnits.prototype.__class__ = SurfaceAreaToVolumeUnits;
SurfaceAreaToVolumeUnits.__cache__ = {};
Module['SurfaceAreaToVolumeUnits'] = SurfaceAreaToVolumeUnits;

SurfaceAreaToVolumeUnits.prototype['toBaseUnits'] = SurfaceAreaToVolumeUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SurfaceAreaToVolumeUnits_toBaseUnits_2(self, value, units);
};;

SurfaceAreaToVolumeUnits.prototype['fromBaseUnits'] = SurfaceAreaToVolumeUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SurfaceAreaToVolumeUnits_fromBaseUnits_2(self, value, units);
};;

  SurfaceAreaToVolumeUnits.prototype['__destroy__'] = SurfaceAreaToVolumeUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SurfaceAreaToVolumeUnits___destroy___0(self);
};
// SpeedUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function SpeedUnits() { throw "cannot construct a SpeedUnits, no constructor in IDL" }
SpeedUnits.prototype = Object.create(WrapperObject.prototype);
SpeedUnits.prototype.constructor = SpeedUnits;
SpeedUnits.prototype.__class__ = SpeedUnits;
SpeedUnits.__cache__ = {};
Module['SpeedUnits'] = SpeedUnits;

SpeedUnits.prototype['toBaseUnits'] = SpeedUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SpeedUnits_toBaseUnits_2(self, value, units);
};;

SpeedUnits.prototype['fromBaseUnits'] = SpeedUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SpeedUnits_fromBaseUnits_2(self, value, units);
};;

  SpeedUnits.prototype['__destroy__'] = SpeedUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SpeedUnits___destroy___0(self);
};
// PressureUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function PressureUnits() { throw "cannot construct a PressureUnits, no constructor in IDL" }
PressureUnits.prototype = Object.create(WrapperObject.prototype);
PressureUnits.prototype.constructor = PressureUnits;
PressureUnits.prototype.__class__ = PressureUnits;
PressureUnits.__cache__ = {};
Module['PressureUnits'] = PressureUnits;

PressureUnits.prototype['toBaseUnits'] = PressureUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_PressureUnits_toBaseUnits_2(self, value, units);
};;

PressureUnits.prototype['fromBaseUnits'] = PressureUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_PressureUnits_fromBaseUnits_2(self, value, units);
};;

  PressureUnits.prototype['__destroy__'] = PressureUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_PressureUnits___destroy___0(self);
};
// SlopeUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function SlopeUnits() { throw "cannot construct a SlopeUnits, no constructor in IDL" }
SlopeUnits.prototype = Object.create(WrapperObject.prototype);
SlopeUnits.prototype.constructor = SlopeUnits;
SlopeUnits.prototype.__class__ = SlopeUnits;
SlopeUnits.__cache__ = {};
Module['SlopeUnits'] = SlopeUnits;

SlopeUnits.prototype['toBaseUnits'] = SlopeUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SlopeUnits_toBaseUnits_2(self, value, units);
};;

SlopeUnits.prototype['fromBaseUnits'] = SlopeUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_SlopeUnits_fromBaseUnits_2(self, value, units);
};;

  SlopeUnits.prototype['__destroy__'] = SlopeUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SlopeUnits___destroy___0(self);
};
// DensityUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function DensityUnits() { throw "cannot construct a DensityUnits, no constructor in IDL" }
DensityUnits.prototype = Object.create(WrapperObject.prototype);
DensityUnits.prototype.constructor = DensityUnits;
DensityUnits.prototype.__class__ = DensityUnits;
DensityUnits.__cache__ = {};
Module['DensityUnits'] = DensityUnits;

DensityUnits.prototype['toBaseUnits'] = DensityUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_DensityUnits_toBaseUnits_2(self, value, units);
};;

DensityUnits.prototype['fromBaseUnits'] = DensityUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_DensityUnits_fromBaseUnits_2(self, value, units);
};;

  DensityUnits.prototype['__destroy__'] = DensityUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_DensityUnits___destroy___0(self);
};
// HeatOfCombustionUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function HeatOfCombustionUnits() { throw "cannot construct a HeatOfCombustionUnits, no constructor in IDL" }
HeatOfCombustionUnits.prototype = Object.create(WrapperObject.prototype);
HeatOfCombustionUnits.prototype.constructor = HeatOfCombustionUnits;
HeatOfCombustionUnits.prototype.__class__ = HeatOfCombustionUnits;
HeatOfCombustionUnits.__cache__ = {};
Module['HeatOfCombustionUnits'] = HeatOfCombustionUnits;

HeatOfCombustionUnits.prototype['toBaseUnits'] = HeatOfCombustionUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatOfCombustionUnits_toBaseUnits_2(self, value, units);
};;

HeatOfCombustionUnits.prototype['fromBaseUnits'] = HeatOfCombustionUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatOfCombustionUnits_fromBaseUnits_2(self, value, units);
};;

  HeatOfCombustionUnits.prototype['__destroy__'] = HeatOfCombustionUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_HeatOfCombustionUnits___destroy___0(self);
};
// HeatSinkUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function HeatSinkUnits() { throw "cannot construct a HeatSinkUnits, no constructor in IDL" }
HeatSinkUnits.prototype = Object.create(WrapperObject.prototype);
HeatSinkUnits.prototype.constructor = HeatSinkUnits;
HeatSinkUnits.prototype.__class__ = HeatSinkUnits;
HeatSinkUnits.__cache__ = {};
Module['HeatSinkUnits'] = HeatSinkUnits;

HeatSinkUnits.prototype['toBaseUnits'] = HeatSinkUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatSinkUnits_toBaseUnits_2(self, value, units);
};;

HeatSinkUnits.prototype['fromBaseUnits'] = HeatSinkUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatSinkUnits_fromBaseUnits_2(self, value, units);
};;

  HeatSinkUnits.prototype['__destroy__'] = HeatSinkUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_HeatSinkUnits___destroy___0(self);
};
// HeatPerUnitAreaUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function HeatPerUnitAreaUnits() { throw "cannot construct a HeatPerUnitAreaUnits, no constructor in IDL" }
HeatPerUnitAreaUnits.prototype = Object.create(WrapperObject.prototype);
HeatPerUnitAreaUnits.prototype.constructor = HeatPerUnitAreaUnits;
HeatPerUnitAreaUnits.prototype.__class__ = HeatPerUnitAreaUnits;
HeatPerUnitAreaUnits.__cache__ = {};
Module['HeatPerUnitAreaUnits'] = HeatPerUnitAreaUnits;

HeatPerUnitAreaUnits.prototype['toBaseUnits'] = HeatPerUnitAreaUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatPerUnitAreaUnits_toBaseUnits_2(self, value, units);
};;

HeatPerUnitAreaUnits.prototype['fromBaseUnits'] = HeatPerUnitAreaUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatPerUnitAreaUnits_fromBaseUnits_2(self, value, units);
};;

  HeatPerUnitAreaUnits.prototype['__destroy__'] = HeatPerUnitAreaUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_HeatPerUnitAreaUnits___destroy___0(self);
};
// HeatSourceAndReactionIntensityUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function HeatSourceAndReactionIntensityUnits() { throw "cannot construct a HeatSourceAndReactionIntensityUnits, no constructor in IDL" }
HeatSourceAndReactionIntensityUnits.prototype = Object.create(WrapperObject.prototype);
HeatSourceAndReactionIntensityUnits.prototype.constructor = HeatSourceAndReactionIntensityUnits;
HeatSourceAndReactionIntensityUnits.prototype.__class__ = HeatSourceAndReactionIntensityUnits;
HeatSourceAndReactionIntensityUnits.__cache__ = {};
Module['HeatSourceAndReactionIntensityUnits'] = HeatSourceAndReactionIntensityUnits;

HeatSourceAndReactionIntensityUnits.prototype['toBaseUnits'] = HeatSourceAndReactionIntensityUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatSourceAndReactionIntensityUnits_toBaseUnits_2(self, value, units);
};;

HeatSourceAndReactionIntensityUnits.prototype['fromBaseUnits'] = HeatSourceAndReactionIntensityUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_HeatSourceAndReactionIntensityUnits_fromBaseUnits_2(self, value, units);
};;

  HeatSourceAndReactionIntensityUnits.prototype['__destroy__'] = HeatSourceAndReactionIntensityUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_HeatSourceAndReactionIntensityUnits___destroy___0(self);
};
// FirelineIntensityUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function FirelineIntensityUnits() { throw "cannot construct a FirelineIntensityUnits, no constructor in IDL" }
FirelineIntensityUnits.prototype = Object.create(WrapperObject.prototype);
FirelineIntensityUnits.prototype.constructor = FirelineIntensityUnits;
FirelineIntensityUnits.prototype.__class__ = FirelineIntensityUnits;
FirelineIntensityUnits.__cache__ = {};
Module['FirelineIntensityUnits'] = FirelineIntensityUnits;

FirelineIntensityUnits.prototype['toBaseUnits'] = FirelineIntensityUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_FirelineIntensityUnits_toBaseUnits_2(self, value, units);
};;

FirelineIntensityUnits.prototype['fromBaseUnits'] = FirelineIntensityUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_FirelineIntensityUnits_fromBaseUnits_2(self, value, units);
};;

  FirelineIntensityUnits.prototype['__destroy__'] = FirelineIntensityUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_FirelineIntensityUnits___destroy___0(self);
};
// TemperatureUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function TemperatureUnits() { throw "cannot construct a TemperatureUnits, no constructor in IDL" }
TemperatureUnits.prototype = Object.create(WrapperObject.prototype);
TemperatureUnits.prototype.constructor = TemperatureUnits;
TemperatureUnits.prototype.__class__ = TemperatureUnits;
TemperatureUnits.__cache__ = {};
Module['TemperatureUnits'] = TemperatureUnits;

TemperatureUnits.prototype['toBaseUnits'] = TemperatureUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_TemperatureUnits_toBaseUnits_2(self, value, units);
};;

TemperatureUnits.prototype['fromBaseUnits'] = TemperatureUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_TemperatureUnits_fromBaseUnits_2(self, value, units);
};;

  TemperatureUnits.prototype['__destroy__'] = TemperatureUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_TemperatureUnits___destroy___0(self);
};
// TimeUnits
/** @suppress {undefinedVars, duplicate} @this{Object} */function TimeUnits() { throw "cannot construct a TimeUnits, no constructor in IDL" }
TimeUnits.prototype = Object.create(WrapperObject.prototype);
TimeUnits.prototype.constructor = TimeUnits;
TimeUnits.prototype.__class__ = TimeUnits;
TimeUnits.__cache__ = {};
Module['TimeUnits'] = TimeUnits;

TimeUnits.prototype['toBaseUnits'] = TimeUnits.prototype.toBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_TimeUnits_toBaseUnits_2(self, value, units);
};;

TimeUnits.prototype['fromBaseUnits'] = TimeUnits.prototype.fromBaseUnits = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, units) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_TimeUnits_fromBaseUnits_2(self, value, units);
};;

  TimeUnits.prototype['__destroy__'] = TimeUnits.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_TimeUnits___destroy___0(self);
};
// FireSize
/** @suppress {undefinedVars, duplicate} @this{Object} */function FireSize() { throw "cannot construct a FireSize, no constructor in IDL" }
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

FireSize.prototype['getFireArea'] = FireSize.prototype.getFireArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(isCrown, areaUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (isCrown && typeof isCrown === 'object') isCrown = isCrown.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFireArea_4(self, isCrown, areaUnits, elapsedTime, timeUnits);
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

FireSize.prototype['getFirePerimeter'] = FireSize.prototype.getFirePerimeter = /** @suppress {undefinedVars, duplicate} @this{Object} */function(isCrown, lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (isCrown && typeof isCrown === 'object') isCrown = isCrown.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getFirePerimeter_4(self, isCrown, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['getFlankingSpreadRate'] = FireSize.prototype.getFlankingSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_FireSize_getFlankingSpreadRate_1(self, spreadRateUnits);
};;

FireSize.prototype['getHeadingToBackingRatio'] = FireSize.prototype.getHeadingToBackingRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_FireSize_getHeadingToBackingRatio_0(self);
};;

FireSize.prototype['getMaxFireWidth'] = FireSize.prototype.getMaxFireWidth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits, elapsedTime, timeUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_FireSize_getMaxFireWidth_3(self, lengthUnits, elapsedTime, timeUnits);
};;

FireSize.prototype['calculateFireBasicDimensions'] = FireSize.prototype.calculateFireBasicDimensions = /** @suppress {undefinedVars, duplicate} @this{Object} */function(isCrown, effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits) {
  var self = this.ptr;
  if (isCrown && typeof isCrown === 'object') isCrown = isCrown.ptr;
  if (effectiveWindSpeed && typeof effectiveWindSpeed === 'object') effectiveWindSpeed = effectiveWindSpeed.ptr;
  if (windSpeedRateUnits && typeof windSpeedRateUnits === 'object') windSpeedRateUnits = windSpeedRateUnits.ptr;
  if (forwardSpreadRate && typeof forwardSpreadRate === 'object') forwardSpreadRate = forwardSpreadRate.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  _emscripten_bind_FireSize_calculateFireBasicDimensions_5(self, isCrown, effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits);
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

SIGContainAdapter.prototype['getFirePerimeterX'] = SIGContainAdapter.prototype.getFirePerimeterX = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGContainAdapter_getFirePerimeterX_0(self), DoubleVector);
};;

SIGContainAdapter.prototype['getFirePerimeterY'] = SIGContainAdapter.prototype.getFirePerimeterY = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGContainAdapter_getFirePerimeterY_0(self), DoubleVector);
};;

SIGContainAdapter.prototype['getAttackDistance'] = SIGContainAdapter.prototype.getAttackDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getAttackDistance_1(self, lengthUnits);
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

SIGContainAdapter.prototype['getFinalProductionRate'] = SIGContainAdapter.prototype.getFinalProductionRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speedUnits) {
  var self = this.ptr;
  if (speedUnits && typeof speedUnits === 'object') speedUnits = speedUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFinalProductionRate_1(self, speedUnits);
};;

SIGContainAdapter.prototype['getFireBackAtAttack'] = SIGContainAdapter.prototype.getFireBackAtAttack = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireBackAtAttack_0(self);
};;

SIGContainAdapter.prototype['getFireBackAtReport'] = SIGContainAdapter.prototype.getFireBackAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireBackAtReport_0(self);
};;

SIGContainAdapter.prototype['getFireHeadAtAttack'] = SIGContainAdapter.prototype.getFireHeadAtAttack = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireHeadAtAttack_0(self);
};;

SIGContainAdapter.prototype['getFireHeadAtReport'] = SIGContainAdapter.prototype.getFireHeadAtReport = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireHeadAtReport_0(self);
};;

SIGContainAdapter.prototype['getFireSizeAtInitialAttack'] = SIGContainAdapter.prototype.getFireSizeAtInitialAttack = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getFireSizeAtInitialAttack_1(self, areaUnits);
};;

SIGContainAdapter.prototype['getLengthToWidthRatio'] = SIGContainAdapter.prototype.getLengthToWidthRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getLengthToWidthRatio_0(self);
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

SIGContainAdapter.prototype['getReportSize'] = SIGContainAdapter.prototype.getReportSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getReportSize_1(self, areaUnits);
};;

SIGContainAdapter.prototype['getReportRate'] = SIGContainAdapter.prototype.getReportRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speedUnits) {
  var self = this.ptr;
  if (speedUnits && typeof speedUnits === 'object') speedUnits = speedUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getReportRate_1(self, speedUnits);
};;

SIGContainAdapter.prototype['getAutoComputedResourceProductionRate'] = SIGContainAdapter.prototype.getAutoComputedResourceProductionRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speedUnits) {
  var self = this.ptr;
  if (speedUnits && typeof speedUnits === 'object') speedUnits = speedUnits.ptr;
  return _emscripten_bind_SIGContainAdapter_getAutoComputedResourceProductionRate_1(self, speedUnits);
};;

SIGContainAdapter.prototype['getTactic'] = SIGContainAdapter.prototype.getTactic = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getTactic_0(self);
};;

SIGContainAdapter.prototype['getFirePerimeterPointCount'] = SIGContainAdapter.prototype.getFirePerimeterPointCount = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGContainAdapter_getFirePerimeterPointCount_0(self);
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

SIGContainAdapter.prototype['addResource'] = SIGContainAdapter.prototype.addResource = /** @suppress {undefinedVars, duplicate} @this{Object} */function(arrival, arrivalTimeUnit, duration, durationTimeUnit, productionRate, productionRateUnits, description, baseCost, hourCost) {
  var self = this.ptr;
  ensureCache.prepare();
  if (arrival && typeof arrival === 'object') arrival = arrival.ptr;
  if (arrivalTimeUnit && typeof arrivalTimeUnit === 'object') arrivalTimeUnit = arrivalTimeUnit.ptr;
  if (duration && typeof duration === 'object') duration = duration.ptr;
  if (durationTimeUnit && typeof durationTimeUnit === 'object') durationTimeUnit = durationTimeUnit.ptr;
  if (productionRate && typeof productionRate === 'object') productionRate = productionRate.ptr;
  if (productionRateUnits && typeof productionRateUnits === 'object') productionRateUnits = productionRateUnits.ptr;
  if (description && typeof description === 'object') description = description.ptr;
  else description = ensureString(description);
  if (baseCost && typeof baseCost === 'object') baseCost = baseCost.ptr;
  if (hourCost && typeof hourCost === 'object') hourCost = hourCost.ptr;
  _emscripten_bind_SIGContainAdapter_addResource_9(self, arrival, arrivalTimeUnit, duration, durationTimeUnit, productionRate, productionRateUnits, description, baseCost, hourCost);
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

SIGContainAdapter.prototype['setContainMode'] = SIGContainAdapter.prototype.setContainMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(containmode) {
  var self = this.ptr;
  if (containmode && typeof containmode === 'object') containmode = containmode.ptr;
  _emscripten_bind_SIGContainAdapter_setContainMode_1(self, containmode);
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

SIGContainAdapter.prototype['setResourceArrivalTime'] = SIGContainAdapter.prototype.setResourceArrivalTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function(arrivalTime, timeUnits) {
  var self = this.ptr;
  if (arrivalTime && typeof arrivalTime === 'object') arrivalTime = arrivalTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  _emscripten_bind_SIGContainAdapter_setResourceArrivalTime_2(self, arrivalTime, timeUnits);
};;

SIGContainAdapter.prototype['setResourceDuration'] = SIGContainAdapter.prototype.setResourceDuration = /** @suppress {undefinedVars, duplicate} @this{Object} */function(duration, timeUnits) {
  var self = this.ptr;
  if (duration && typeof duration === 'object') duration = duration.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  _emscripten_bind_SIGContainAdapter_setResourceDuration_2(self, duration, timeUnits);
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

SIGIgnite.prototype['calculateFirebrandIgnitionProbability'] = SIGIgnite.prototype.calculateFirebrandIgnitionProbability = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGIgnite_calculateFirebrandIgnitionProbability_0(self);
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

SIGIgnite.prototype['getFirebrandIgnitionProbability'] = SIGIgnite.prototype.getFirebrandIgnitionProbability = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGIgnite_getFirebrandIgnitionProbability_1(self, desiredUnits);
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

SIGMoistureScenarios.prototype['getMoistureScenarioHundredHourByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioHundredHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByIndex_2(self, index, moistureUnits);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioHundredHourByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioHundredHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByName_2(self, name, moistureUnits);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioLiveHerbaceousByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveHerbaceousByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByIndex_2(self, index, moistureUnits);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioLiveHerbaceousByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveHerbaceousByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByName_2(self, name, moistureUnits);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioLiveWoodyByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveWoodyByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByIndex_2(self, index, moistureUnits);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioLiveWoodyByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioLiveWoodyByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByName_2(self, name, moistureUnits);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioOneHourByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioOneHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByIndex_2(self, index, moistureUnits);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioOneHourByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioOneHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByName_2(self, name, moistureUnits);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioTenHourByIndex'] = SIGMoistureScenarios.prototype.getMoistureScenarioTenHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByIndex_2(self, index, moistureUnits);
};;

SIGMoistureScenarios.prototype['getMoistureScenarioTenHourByName'] = SIGMoistureScenarios.prototype.getMoistureScenarioTenHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByName_2(self, name, moistureUnits);
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

SIGSpot.prototype['getDownwindCanopyMode'] = SIGSpot.prototype.getDownwindCanopyMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getDownwindCanopyMode_0(self);
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

SIGSpot.prototype['getMaxMountainousTerrainSpottingDistanceFromActiveCrown'] = SIGSpot.prototype.getMaxMountainousTerrainSpottingDistanceFromActiveCrown = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spottingDistanceUnits) {
  var self = this.ptr;
  if (spottingDistanceUnits && typeof spottingDistanceUnits === 'object') spottingDistanceUnits = spottingDistanceUnits.ptr;
  return _emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromActiveCrown_1(self, spottingDistanceUnits);
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

SIGSpot.prototype['getTorchingTrees'] = SIGSpot.prototype.getTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSpot_getTorchingTrees_0(self);
};;

SIGSpot.prototype['calculateAll'] = SIGSpot.prototype.calculateAll = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_calculateAll_0(self);
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

SIGSpot.prototype['initializeMembers'] = SIGSpot.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSpot_initializeMembers_0(self);
};;

SIGSpot.prototype['setActiveCrownFlameLength'] = SIGSpot.prototype.setActiveCrownFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGSpot_setActiveCrownFlameLength_2(self, flameLength, flameLengthUnits);
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

SIGSpot.prototype['setDownwindCanopyMode'] = SIGSpot.prototype.setDownwindCanopyMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(downwindCanopyMode) {
  var self = this.ptr;
  if (downwindCanopyMode && typeof downwindCanopyMode === 'object') downwindCanopyMode = downwindCanopyMode.ptr;
  _emscripten_bind_SIGSpot_setDownwindCanopyMode_1(self, downwindCanopyMode);
};;

SIGSpot.prototype['setDownwindCoverHeight'] = SIGSpot.prototype.setDownwindCoverHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(downwindCoverHeight, coverHeightUnits) {
  var self = this.ptr;
  if (downwindCoverHeight && typeof downwindCoverHeight === 'object') downwindCoverHeight = downwindCoverHeight.ptr;
  if (coverHeightUnits && typeof coverHeightUnits === 'object') coverHeightUnits = coverHeightUnits.ptr;
  _emscripten_bind_SIGSpot_setDownwindCoverHeight_2(self, downwindCoverHeight, coverHeightUnits);
};;

SIGSpot.prototype['setFireType'] = SIGSpot.prototype.setFireType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fireType) {
  var self = this.ptr;
  if (fireType && typeof fireType === 'object') fireType = fireType.ptr;
  _emscripten_bind_SIGSpot_setFireType_1(self, fireType);
};;

SIGSpot.prototype['setFlameLength'] = SIGSpot.prototype.setFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGSpot_setFlameLength_2(self, flameLength, flameLengthUnits);
};;

SIGSpot.prototype['setFirelineIntensity'] = SIGSpot.prototype.setFirelineIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensity, firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  _emscripten_bind_SIGSpot_setFirelineIntensity_2(self, firelineIntensity, firelineIntensityUnits);
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

SIGSpot.prototype['setWindSpeed'] = SIGSpot.prototype.setWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSpot_setWindSpeed_2(self, windSpeed, windSpeedUnits);
};;

SIGSpot.prototype['setWindSpeedAndWindHeightInputMode'] = SIGSpot.prototype.setWindSpeedAndWindHeightInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeed, windSpeedUnits, windHeightInputMode) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGSpot_setWindSpeedAndWindHeightInputMode_3(self, windSpeed, windSpeedUnits, windHeightInputMode);
};;

SIGSpot.prototype['setWindHeightInputMode'] = SIGSpot.prototype.setWindHeightInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windHeightInputMode) {
  var self = this.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGSpot_setWindHeightInputMode_1(self, windHeightInputMode);
};;

SIGSpot.prototype['updateSpotInputsForBurningPile'] = SIGSpot.prototype.updateSpotInputsForBurningPile = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits) {
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
};;

SIGSpot.prototype['updateSpotInputsForSurfaceFire'] = SIGSpot.prototype.updateSpotInputsForSurfaceFire = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, windSpeedAtTwentyFeet, windSpeedUnits, flameLength, flameLengthUnits) {
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
};;

SIGSpot.prototype['updateSpotInputsForTorchingTrees'] = SIGSpot.prototype.updateSpotInputsForTorchingTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits) {
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

SIGSurface.prototype['getWindUpslopeAlignmentMode'] = SIGSurface.prototype.getWindUpslopeAlignmentMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getWindUpslopeAlignmentMode_0(self);
};;

SIGSurface.prototype['getSurfaceRunInDirectionOf'] = SIGSurface.prototype.getSurfaceRunInDirectionOf = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getSurfaceRunInDirectionOf_0(self);
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

SIGSurface.prototype['setCurrentMoistureScenarioByIndex'] = SIGSurface.prototype.setCurrentMoistureScenarioByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarioIndex) {
  var self = this.ptr;
  if (moistureScenarioIndex && typeof moistureScenarioIndex === 'object') moistureScenarioIndex = moistureScenarioIndex.ptr;
  return !!(_emscripten_bind_SIGSurface_setCurrentMoistureScenarioByIndex_1(self, moistureScenarioIndex));
};;

SIGSurface.prototype['setCurrentMoistureScenarioByName'] = SIGSurface.prototype.setCurrentMoistureScenarioByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarioName) {
  var self = this.ptr;
  ensureCache.prepare();
  if (moistureScenarioName && typeof moistureScenarioName === 'object') moistureScenarioName = moistureScenarioName.ptr;
  else moistureScenarioName = ensureString(moistureScenarioName);
  return !!(_emscripten_bind_SIGSurface_setCurrentMoistureScenarioByName_1(self, moistureScenarioName));
};;

SIGSurface.prototype['calculateFlameLength'] = SIGSurface.prototype.calculateFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensity, firelineIntensityUnits, flameLengthUnits) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_calculateFlameLength_3(self, firelineIntensity, firelineIntensityUnits, flameLengthUnits);
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

SIGSurface.prototype['getBackingFirelineIntensity'] = SIGSurface.prototype.getBackingFirelineIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getBackingFirelineIntensity_1(self, firelineIntensityUnits);
};;

SIGSurface.prototype['getBackingFlameLength'] = SIGSurface.prototype.getBackingFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getBackingFlameLength_1(self, flameLengthUnits);
};;

SIGSurface.prototype['getBackingSpreadDistance'] = SIGSurface.prototype.getBackingSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getBackingSpreadDistance_1(self, lengthUnits);
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

SIGSurface.prototype['getChaparralLoadDeadHalfInchToLessThanOneInch'] = SIGSurface.prototype.getChaparralLoadDeadHalfInchToLessThanOneInch = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadDeadHalfInchToLessThanOneInch_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralLoadDeadLessThanQuarterInch'] = SIGSurface.prototype.getChaparralLoadDeadLessThanQuarterInch = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadDeadLessThanQuarterInch_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralLoadDeadOneInchToThreeInch'] = SIGSurface.prototype.getChaparralLoadDeadOneInchToThreeInch = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadDeadOneInchToThreeInch_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralLoadDeadQuarterInchToLessThanHalfInch'] = SIGSurface.prototype.getChaparralLoadDeadQuarterInchToLessThanHalfInch = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadDeadQuarterInchToLessThanHalfInch_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralLoadLiveHalfInchToLessThanOneInch'] = SIGSurface.prototype.getChaparralLoadLiveHalfInchToLessThanOneInch = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveHalfInchToLessThanOneInch_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralLoadLiveLeaves'] = SIGSurface.prototype.getChaparralLoadLiveLeaves = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveLeaves_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralLoadLiveOneInchToThreeInch'] = SIGSurface.prototype.getChaparralLoadLiveOneInchToThreeInch = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveOneInchToThreeInch_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralLoadLiveQuarterInchToLessThanHalfInch'] = SIGSurface.prototype.getChaparralLoadLiveQuarterInchToLessThanHalfInch = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveQuarterInchToLessThanHalfInch_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralLoadLiveStemsLessThanQuaterInch'] = SIGSurface.prototype.getChaparralLoadLiveStemsLessThanQuaterInch = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralLoadLiveStemsLessThanQuaterInch_1(self, loadingUnits);
};;

SIGSurface.prototype['getChaparralMoisture'] = SIGSurface.prototype.getChaparralMoisture = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, sizeClass, moistureUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (sizeClass && typeof sizeClass === 'object') sizeClass = sizeClass.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getChaparralMoisture_3(self, lifeState, sizeClass, moistureUnits);
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

SIGSurface.prototype['getCharacteristicMoistureByLifeState'] = SIGSurface.prototype.getCharacteristicMoistureByLifeState = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lifeState, moistureUnits) {
  var self = this.ptr;
  if (lifeState && typeof lifeState === 'object') lifeState = lifeState.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicMoistureByLifeState_2(self, lifeState, moistureUnits);
};;

SIGSurface.prototype['getCharacteristicMoistureDead'] = SIGSurface.prototype.getCharacteristicMoistureDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicMoistureDead_1(self, moistureUnits);
};;

SIGSurface.prototype['getCharacteristicMoistureLive'] = SIGSurface.prototype.getCharacteristicMoistureLive = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureUnits) {
  var self = this.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicMoistureLive_1(self, moistureUnits);
};;

SIGSurface.prototype['getCharacteristicSAVR'] = SIGSurface.prototype.getCharacteristicSAVR = /** @suppress {undefinedVars, duplicate} @this{Object} */function(savrUnits) {
  var self = this.ptr;
  if (savrUnits && typeof savrUnits === 'object') savrUnits = savrUnits.ptr;
  return _emscripten_bind_SIGSurface_getCharacteristicSAVR_1(self, savrUnits);
};;

SIGSurface.prototype['getCrownRatio'] = SIGSurface.prototype.getCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownRatioUnits) {
  var self = this.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  return _emscripten_bind_SIGSurface_getCrownRatio_1(self, crownRatioUnits);
};;

SIGSurface.prototype['getDirectionOfMaxSpread'] = SIGSurface.prototype.getDirectionOfMaxSpread = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getDirectionOfMaxSpread_0(self);
};;

SIGSurface.prototype['getDirectionOfInterest'] = SIGSurface.prototype.getDirectionOfInterest = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getDirectionOfInterest_0(self);
};;

SIGSurface.prototype['getDirectionOfBacking'] = SIGSurface.prototype.getDirectionOfBacking = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getDirectionOfBacking_0(self);
};;

SIGSurface.prototype['getDirectionOfFlanking'] = SIGSurface.prototype.getDirectionOfFlanking = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getDirectionOfFlanking_0(self);
};;

SIGSurface.prototype['getElapsedTime'] = SIGSurface.prototype.getElapsedTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function(timeUnits) {
  var self = this.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  return _emscripten_bind_SIGSurface_getElapsedTime_1(self, timeUnits);
};;

SIGSurface.prototype['getEllipticalA'] = SIGSurface.prototype.getEllipticalA = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getEllipticalA_1(self, lengthUnits);
};;

SIGSurface.prototype['getEllipticalB'] = SIGSurface.prototype.getEllipticalB = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getEllipticalB_1(self, lengthUnits);
};;

SIGSurface.prototype['getEllipticalC'] = SIGSurface.prototype.getEllipticalC = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getEllipticalC_1(self, lengthUnits);
};;

SIGSurface.prototype['getFireLength'] = SIGSurface.prototype.getFireLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFireLength_1(self, lengthUnits);
};;

SIGSurface.prototype['getMaxFireWidth'] = SIGSurface.prototype.getMaxFireWidth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getMaxFireWidth_1(self, lengthUnits);
};;

SIGSurface.prototype['getFireArea'] = SIGSurface.prototype.getFireArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGSurface_getFireArea_1(self, areaUnits);
};;

SIGSurface.prototype['getFireEccentricity'] = SIGSurface.prototype.getFireEccentricity = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getFireEccentricity_0(self);
};;

SIGSurface.prototype['getFireLengthToWidthRatio'] = SIGSurface.prototype.getFireLengthToWidthRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSurface_getFireLengthToWidthRatio_0(self);
};;

SIGSurface.prototype['getFirePerimeter'] = SIGSurface.prototype.getFirePerimeter = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFirePerimeter_1(self, lengthUnits);
};;

SIGSurface.prototype['getFirelineIntensity'] = SIGSurface.prototype.getFirelineIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getFirelineIntensity_1(self, firelineIntensityUnits);
};;

SIGSurface.prototype['getFirelineIntensityInDirectionOfInterest'] = SIGSurface.prototype.getFirelineIntensityInDirectionOfInterest = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getFirelineIntensityInDirectionOfInterest_1(self, firelineIntensityUnits);
};;

SIGSurface.prototype['getFlameLength'] = SIGSurface.prototype.getFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlameLength_1(self, flameLengthUnits);
};;

SIGSurface.prototype['getFlameLengthInDirectionOfInterest'] = SIGSurface.prototype.getFlameLengthInDirectionOfInterest = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlameLengthInDirectionOfInterest_1(self, flameLengthUnits);
};;

SIGSurface.prototype['getFlankingFirelineIntensity'] = SIGSurface.prototype.getFlankingFirelineIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingFirelineIntensity_1(self, firelineIntensityUnits);
};;

SIGSurface.prototype['getFlankingFlameLength'] = SIGSurface.prototype.getFlankingFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingFlameLength_1(self, flameLengthUnits);
};;

SIGSurface.prototype['getFlankingSpreadRate'] = SIGSurface.prototype.getFlankingSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingSpreadRate_1(self, spreadRateUnits);
};;

SIGSurface.prototype['getFlankingSpreadDistance'] = SIGSurface.prototype.getFlankingSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getFlankingSpreadDistance_1(self, lengthUnits);
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

SIGSurface.prototype['getHeadingSpreadRate'] = SIGSurface.prototype.getHeadingSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGSurface_getHeadingSpreadRate_1(self, spreadRateUnits);
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

SIGSurface.prototype['getMoistureScenarioHundredHourByIndex'] = SIGSurface.prototype.getMoistureScenarioHundredHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByIndex_2(self, index, moistureUnits);
};;

SIGSurface.prototype['getMoistureScenarioHundredHourByName'] = SIGSurface.prototype.getMoistureScenarioHundredHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByName_2(self, name, moistureUnits);
};;

SIGSurface.prototype['getMoistureScenarioLiveHerbaceousByIndex'] = SIGSurface.prototype.getMoistureScenarioLiveHerbaceousByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByIndex_2(self, index, moistureUnits);
};;

SIGSurface.prototype['getMoistureScenarioLiveHerbaceousByName'] = SIGSurface.prototype.getMoistureScenarioLiveHerbaceousByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByName_2(self, name, moistureUnits);
};;

SIGSurface.prototype['getMoistureScenarioLiveWoodyByIndex'] = SIGSurface.prototype.getMoistureScenarioLiveWoodyByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByIndex_2(self, index, moistureUnits);
};;

SIGSurface.prototype['getMoistureScenarioLiveWoodyByName'] = SIGSurface.prototype.getMoistureScenarioLiveWoodyByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByName_2(self, name, moistureUnits);
};;

SIGSurface.prototype['getMoistureScenarioOneHourByIndex'] = SIGSurface.prototype.getMoistureScenarioOneHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioOneHourByIndex_2(self, index, moistureUnits);
};;

SIGSurface.prototype['getMoistureScenarioOneHourByName'] = SIGSurface.prototype.getMoistureScenarioOneHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioOneHourByName_2(self, name, moistureUnits);
};;

SIGSurface.prototype['getMoistureScenarioTenHourByIndex'] = SIGSurface.prototype.getMoistureScenarioTenHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioTenHourByIndex_2(self, index, moistureUnits);
};;

SIGSurface.prototype['getMoistureScenarioTenHourByName'] = SIGSurface.prototype.getMoistureScenarioTenHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGSurface_getMoistureScenarioTenHourByName_2(self, name, moistureUnits);
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

SIGSurface.prototype['getPalmettoGallberyDeadFineFuelLoad'] = SIGSurface.prototype.getPalmettoGallberyDeadFineFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyDeadFineFuelLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyDeadFoliageLoad'] = SIGSurface.prototype.getPalmettoGallberyDeadFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyDeadFoliageLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyDeadMediumFuelLoad'] = SIGSurface.prototype.getPalmettoGallberyDeadMediumFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyDeadMediumFuelLoad_1(self, loadingUnits);
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

SIGSurface.prototype['getPalmettoGallberyLiveFineFuelLoad'] = SIGSurface.prototype.getPalmettoGallberyLiveFineFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLiveFineFuelLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyLiveFoliageLoad'] = SIGSurface.prototype.getPalmettoGallberyLiveFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLiveFoliageLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getPalmettoGallberyLiveMediumFuelLoad'] = SIGSurface.prototype.getPalmettoGallberyLiveMediumFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getPalmettoGallberyLiveMediumFuelLoad_1(self, loadingUnits);
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

SIGSurface.prototype['getSpreadDistance'] = SIGSurface.prototype.getSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadDistance_1(self, lengthUnits);
};;

SIGSurface.prototype['getSpreadDistanceInDirectionOfInterest'] = SIGSurface.prototype.getSpreadDistanceInDirectionOfInterest = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGSurface_getSpreadDistanceInDirectionOfInterest_1(self, lengthUnits);
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

SIGSurface.prototype['getTotalLiveFuelLoad'] = SIGSurface.prototype.getTotalLiveFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getTotalLiveFuelLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getTotalDeadFuelLoad'] = SIGSurface.prototype.getTotalDeadFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getTotalDeadFuelLoad_1(self, loadingUnits);
};;

SIGSurface.prototype['getTotalDeadHerbaceousFuelLoad'] = SIGSurface.prototype.getTotalDeadHerbaceousFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(loadingUnits) {
  var self = this.ptr;
  if (loadingUnits && typeof loadingUnits === 'object') loadingUnits = loadingUnits.ptr;
  return _emscripten_bind_SIGSurface_getTotalDeadHerbaceousFuelLoad_1(self, loadingUnits);
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

SIGSurface.prototype['doSurfaceRun'] = SIGSurface.prototype.doSurfaceRun = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSurface_doSurfaceRun_0(self);
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

SIGSurface.prototype['setCrownRatio'] = SIGSurface.prototype.setCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownRatio, crownRatioUnits) {
  var self = this.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGSurface_setCrownRatio_2(self, crownRatio, crownRatioUnits);
};;

SIGSurface.prototype['setDirectionOfInterest'] = SIGSurface.prototype.setDirectionOfInterest = /** @suppress {undefinedVars, duplicate} @this{Object} */function(directionOfInterest) {
  var self = this.ptr;
  if (directionOfInterest && typeof directionOfInterest === 'object') directionOfInterest = directionOfInterest.ptr;
  _emscripten_bind_SIGSurface_setDirectionOfInterest_1(self, directionOfInterest);
};;

SIGSurface.prototype['setElapsedTime'] = SIGSurface.prototype.setElapsedTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function(elapsedTime, timeUnits) {
  var self = this.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  _emscripten_bind_SIGSurface_setElapsedTime_2(self, elapsedTime, timeUnits);
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

SIGSurface.prototype['setSurfaceFireSpreadDirectionMode'] = SIGSurface.prototype.setSurfaceFireSpreadDirectionMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(directionMode) {
  var self = this.ptr;
  if (directionMode && typeof directionMode === 'object') directionMode = directionMode.ptr;
  _emscripten_bind_SIGSurface_setSurfaceFireSpreadDirectionMode_1(self, directionMode);
};;

SIGSurface.prototype['setSurfaceRunInDirectionOf'] = SIGSurface.prototype.setSurfaceRunInDirectionOf = /** @suppress {undefinedVars, duplicate} @this{Object} */function(surfaceRunInDirectionOf) {
  var self = this.ptr;
  if (surfaceRunInDirectionOf && typeof surfaceRunInDirectionOf === 'object') surfaceRunInDirectionOf = surfaceRunInDirectionOf.ptr;
  _emscripten_bind_SIGSurface_setSurfaceRunInDirectionOf_1(self, surfaceRunInDirectionOf);
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

SIGSurface.prototype['setWindSpeed'] = SIGSurface.prototype.setWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGSurface_setWindSpeed_2(self, windSpeed, windSpeedUnits);
};;

SIGSurface.prototype['updateSurfaceInputs'] = SIGSurface.prototype.updateSurfaceInputs = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits) {
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
};;

SIGSurface.prototype['updateSurfaceInputsForPalmettoGallbery'] = SIGSurface.prototype.updateSurfaceInputsForPalmettoGallbery = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, basalAreaUnits, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits) {
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
};;

SIGSurface.prototype['updateSurfaceInputsForTwoFuelModels'] = SIGSurface.prototype.updateSurfaceInputsForTwoFuelModels = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firstFuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope, slopeUnits, aspect, canopyCover, canopyFractionUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnitso) {
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
};;

SIGSurface.prototype['updateSurfaceInputsForWesternAspen'] = SIGSurface.prototype.updateSurfaceInputsForWesternAspen = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspenFuelModelNumber, aspenCuringLevel, curingLevelUnits, aspenFireSeverity, dbh, dbhUnits, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits) {
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

PalmettoGallberry.prototype['calculatePalmettoGallberyDeadFineFuelLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyDeadFineFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFineFuelLoad_2(self, ageOfRough, heightOfUnderstory);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyDeadFoliageLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyDeadFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, palmettoCoverage) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFoliageLoad_2(self, ageOfRough, palmettoCoverage);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyDeadMediumFuelLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyDeadMediumFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, palmettoCoverage) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadMediumFuelLoad_2(self, ageOfRough, palmettoCoverage);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyFuelBedDepth'] = PalmettoGallberry.prototype.calculatePalmettoGallberyFuelBedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function(heightOfUnderstory) {
  var self = this.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyFuelBedDepth_1(self, heightOfUnderstory);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyLitterLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLitterLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, overstoryBasalArea) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (overstoryBasalArea && typeof overstoryBasalArea === 'object') overstoryBasalArea = overstoryBasalArea.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLitterLoad_2(self, ageOfRough, overstoryBasalArea);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyLiveFineFuelLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLiveFineFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFineFuelLoad_2(self, ageOfRough, heightOfUnderstory);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyLiveFoliageLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLiveFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, palmettoCoverage, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (palmettoCoverage && typeof palmettoCoverage === 'object') palmettoCoverage = palmettoCoverage.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFoliageLoad_3(self, ageOfRough, palmettoCoverage, heightOfUnderstory);
};;

PalmettoGallberry.prototype['calculatePalmettoGallberyLiveMediumFuelLoad'] = PalmettoGallberry.prototype.calculatePalmettoGallberyLiveMediumFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function(ageOfRough, heightOfUnderstory) {
  var self = this.ptr;
  if (ageOfRough && typeof ageOfRough === 'object') ageOfRough = ageOfRough.ptr;
  if (heightOfUnderstory && typeof heightOfUnderstory === 'object') heightOfUnderstory = heightOfUnderstory.ptr;
  return _emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveMediumFuelLoad_2(self, ageOfRough, heightOfUnderstory);
};;

PalmettoGallberry.prototype['getHeatOfCombustionDead'] = PalmettoGallberry.prototype.getHeatOfCombustionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getHeatOfCombustionDead_0(self);
};;

PalmettoGallberry.prototype['getHeatOfCombustionLive'] = PalmettoGallberry.prototype.getHeatOfCombustionLive = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getHeatOfCombustionLive_0(self);
};;

PalmettoGallberry.prototype['getMoistureOfExtinctionDead'] = PalmettoGallberry.prototype.getMoistureOfExtinctionDead = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getMoistureOfExtinctionDead_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyDeadFineFuelLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyDeadFineFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFineFuelLoad_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyDeadFoliageLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyDeadFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFoliageLoad_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyDeadMediumFuelLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyDeadMediumFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadMediumFuelLoad_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyFuelBedDepth'] = PalmettoGallberry.prototype.getPalmettoGallberyFuelBedDepth = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyFuelBedDepth_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyLitterLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLitterLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLitterLoad_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyLiveFineFuelLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLiveFineFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFineFuelLoad_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyLiveFoliageLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLiveFoliageLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFoliageLoad_0(self);
};;

PalmettoGallberry.prototype['getPalmettoGallberyLiveMediumFuelLoad'] = PalmettoGallberry.prototype.getPalmettoGallberyLiveMediumFuelLoad = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveMediumFuelLoad_0(self);
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

SIGCrown.prototype['setCurrentMoistureScenarioByIndex'] = SIGCrown.prototype.setCurrentMoistureScenarioByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarioIndex) {
  var self = this.ptr;
  if (moistureScenarioIndex && typeof moistureScenarioIndex === 'object') moistureScenarioIndex = moistureScenarioIndex.ptr;
  return !!(_emscripten_bind_SIGCrown_setCurrentMoistureScenarioByIndex_1(self, moistureScenarioIndex));
};;

SIGCrown.prototype['setCurrentMoistureScenarioByName'] = SIGCrown.prototype.setCurrentMoistureScenarioByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(moistureScenarioName) {
  var self = this.ptr;
  ensureCache.prepare();
  if (moistureScenarioName && typeof moistureScenarioName === 'object') moistureScenarioName = moistureScenarioName.ptr;
  else moistureScenarioName = ensureString(moistureScenarioName);
  return !!(_emscripten_bind_SIGCrown_setCurrentMoistureScenarioByName_1(self, moistureScenarioName));
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

SIGCrown.prototype['getCanopyCover'] = SIGCrown.prototype.getCanopyCover = /** @suppress {undefinedVars, duplicate} @this{Object} */function(canopyFractionUnits) {
  var self = this.ptr;
  if (canopyFractionUnits && typeof canopyFractionUnits === 'object') canopyFractionUnits = canopyFractionUnits.ptr;
  return _emscripten_bind_SIGCrown_getCanopyCover_1(self, canopyFractionUnits);
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

SIGCrown.prototype['getCrownCriticalFireSpreadRate'] = SIGCrown.prototype.getCrownCriticalFireSpreadRate = /** @suppress {undefinedVars, duplicate} @this{Object} */function(spreadRateUnits) {
  var self = this.ptr;
  if (spreadRateUnits && typeof spreadRateUnits === 'object') spreadRateUnits = spreadRateUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownCriticalFireSpreadRate_1(self, spreadRateUnits);
};;

SIGCrown.prototype['getCrownCriticalSurfaceFirelineIntensity'] = SIGCrown.prototype.getCrownCriticalSurfaceFirelineIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownCriticalSurfaceFirelineIntensity_1(self, firelineIntensityUnits);
};;

SIGCrown.prototype['getCrownCriticalSurfaceFlameLength'] = SIGCrown.prototype.getCrownCriticalSurfaceFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownCriticalSurfaceFlameLength_1(self, flameLengthUnits);
};;

SIGCrown.prototype['getCrownFireActiveRatio'] = SIGCrown.prototype.getCrownFireActiveRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireActiveRatio_0(self);
};;

SIGCrown.prototype['getCrownFireArea'] = SIGCrown.prototype.getCrownFireArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireArea_1(self, areaUnits);
};;

SIGCrown.prototype['getCrownFirePerimeter'] = SIGCrown.prototype.getCrownFirePerimeter = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFirePerimeter_1(self, lengthUnits);
};;

SIGCrown.prototype['getCrownTransitionRatio'] = SIGCrown.prototype.getCrownTransitionRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownTransitionRatio_0(self);
};;

SIGCrown.prototype['getCrownFireLengthToWidthRatio'] = SIGCrown.prototype.getCrownFireLengthToWidthRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireLengthToWidthRatio_0(self);
};;

SIGCrown.prototype['getCrownFireSpreadDistance'] = SIGCrown.prototype.getCrownFireSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownFireSpreadDistance_1(self, lengthUnits);
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

SIGCrown.prototype['getCrownRatio'] = SIGCrown.prototype.getCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownRatioUnits) {
  var self = this.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  return _emscripten_bind_SIGCrown_getCrownRatio_1(self, crownRatioUnits);
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

SIGCrown.prototype['getFinalSpreadDistance'] = SIGCrown.prototype.getFinalSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalSpreadDistance_1(self, lengthUnits);
};;

SIGCrown.prototype['getFinalFireArea'] = SIGCrown.prototype.getFinalFireArea = /** @suppress {undefinedVars, duplicate} @this{Object} */function(areaUnits) {
  var self = this.ptr;
  if (areaUnits && typeof areaUnits === 'object') areaUnits = areaUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalFireArea_1(self, areaUnits);
};;

SIGCrown.prototype['getFinalFirePerimeter'] = SIGCrown.prototype.getFinalFirePerimeter = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getFinalFirePerimeter_1(self, lengthUnits);
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

SIGCrown.prototype['getMoistureScenarioHundredHourByIndex'] = SIGCrown.prototype.getMoistureScenarioHundredHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByIndex_2(self, index, moistureUnits);
};;

SIGCrown.prototype['getMoistureScenarioHundredHourByName'] = SIGCrown.prototype.getMoistureScenarioHundredHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByName_2(self, name, moistureUnits);
};;

SIGCrown.prototype['getMoistureScenarioLiveHerbaceousByIndex'] = SIGCrown.prototype.getMoistureScenarioLiveHerbaceousByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByIndex_2(self, index, moistureUnits);
};;

SIGCrown.prototype['getMoistureScenarioLiveHerbaceousByName'] = SIGCrown.prototype.getMoistureScenarioLiveHerbaceousByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByName_2(self, name, moistureUnits);
};;

SIGCrown.prototype['getMoistureScenarioLiveWoodyByIndex'] = SIGCrown.prototype.getMoistureScenarioLiveWoodyByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByIndex_2(self, index, moistureUnits);
};;

SIGCrown.prototype['getMoistureScenarioLiveWoodyByName'] = SIGCrown.prototype.getMoistureScenarioLiveWoodyByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByName_2(self, name, moistureUnits);
};;

SIGCrown.prototype['getMoistureScenarioOneHourByIndex'] = SIGCrown.prototype.getMoistureScenarioOneHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioOneHourByIndex_2(self, index, moistureUnits);
};;

SIGCrown.prototype['getMoistureScenarioOneHourByName'] = SIGCrown.prototype.getMoistureScenarioOneHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioOneHourByName_2(self, name, moistureUnits);
};;

SIGCrown.prototype['getMoistureScenarioTenHourByIndex'] = SIGCrown.prototype.getMoistureScenarioTenHourByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, moistureUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioTenHourByIndex_2(self, index, moistureUnits);
};;

SIGCrown.prototype['getMoistureScenarioTenHourByName'] = SIGCrown.prototype.getMoistureScenarioTenHourByName = /** @suppress {undefinedVars, duplicate} @this{Object} */function(name, moistureUnits) {
  var self = this.ptr;
  ensureCache.prepare();
  if (name && typeof name === 'object') name = name.ptr;
  else name = ensureString(name);
  if (moistureUnits && typeof moistureUnits === 'object') moistureUnits = moistureUnits.ptr;
  return _emscripten_bind_SIGCrown_getMoistureScenarioTenHourByName_2(self, name, moistureUnits);
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

SIGCrown.prototype['getSurfaceFireSpreadDistance'] = SIGCrown.prototype.getSurfaceFireSpreadDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_SIGCrown_getSurfaceFireSpreadDistance_1(self, lengthUnits);
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

SIGCrown.prototype['doCrownRun'] = SIGCrown.prototype.doCrownRun = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGCrown_doCrownRun_0(self);
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

SIGCrown.prototype['setCrownRatio'] = SIGCrown.prototype.setCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownRatio, crownRatioUnits) {
  var self = this.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGCrown_setCrownRatio_2(self, crownRatio, crownRatioUnits);
};;

SIGCrown.prototype['setFuelModelNumber'] = SIGCrown.prototype.setFuelModelNumber = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber) {
  var self = this.ptr;
  if (fuelModelNumber && typeof fuelModelNumber === 'object') fuelModelNumber = fuelModelNumber.ptr;
  _emscripten_bind_SIGCrown_setFuelModelNumber_1(self, fuelModelNumber);
};;

SIGCrown.prototype['setCrownFireCalculationMethod'] = SIGCrown.prototype.setCrownFireCalculationMethod = /** @suppress {undefinedVars, duplicate} @this{Object} */function(CrownFireCalculationMethod) {
  var self = this.ptr;
  if (CrownFireCalculationMethod && typeof CrownFireCalculationMethod === 'object') CrownFireCalculationMethod = CrownFireCalculationMethod.ptr;
  _emscripten_bind_SIGCrown_setCrownFireCalculationMethod_1(self, CrownFireCalculationMethod);
};;

SIGCrown.prototype['setElapsedTime'] = SIGCrown.prototype.setElapsedTime = /** @suppress {undefinedVars, duplicate} @this{Object} */function(elapsedTime, timeUnits) {
  var self = this.ptr;
  if (elapsedTime && typeof elapsedTime === 'object') elapsedTime = elapsedTime.ptr;
  if (timeUnits && typeof timeUnits === 'object') timeUnits = timeUnits.ptr;
  _emscripten_bind_SIGCrown_setElapsedTime_2(self, elapsedTime, timeUnits);
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

SIGCrown.prototype['setWindSpeed'] = SIGCrown.prototype.setWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGCrown_setWindSpeed_2(self, windSpeed, windSpeedUnits);
};;

SIGCrown.prototype['updateCrownInputs'] = SIGCrown.prototype.updateCrownInputs = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureFoliar, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyBaseHeight, canopyHeightUnits, crownRatio, crownRatioUnits, canopyBulkDensity, densityUnits) {
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
};;

SIGCrown.prototype['updateCrownsSurfaceInputs'] = SIGCrown.prototype.updateCrownsSurfaceInputs = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits) {
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

SpeciesMasterTable.prototype['insertRecord'] = SpeciesMasterTable.prototype.insertRecord = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode, scientificName, commonName, mortalityEquation, brkEqu, crownCoefficientCode, Alaska, California, EasternArea, GreatBasin, NorthernRockies, Northwest, RocketyMountain, SouthernArea, SouthWest, equationType, crownDamageEquationCode) {
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
  if (Alaska && typeof Alaska === 'object') Alaska = Alaska.ptr;
  if (California && typeof California === 'object') California = California.ptr;
  if (EasternArea && typeof EasternArea === 'object') EasternArea = EasternArea.ptr;
  if (GreatBasin && typeof GreatBasin === 'object') GreatBasin = GreatBasin.ptr;
  if (NorthernRockies && typeof NorthernRockies === 'object') NorthernRockies = NorthernRockies.ptr;
  if (Northwest && typeof Northwest === 'object') Northwest = Northwest.ptr;
  if (RocketyMountain && typeof RocketyMountain === 'object') RocketyMountain = RocketyMountain.ptr;
  if (SouthernArea && typeof SouthernArea === 'object') SouthernArea = SouthernArea.ptr;
  if (SouthWest && typeof SouthWest === 'object') SouthWest = SouthWest.ptr;
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  if (crownDamageEquationCode && typeof crownDamageEquationCode === 'object') crownDamageEquationCode = crownDamageEquationCode.ptr;
  _emscripten_bind_SpeciesMasterTable_insertRecord_17(self, speciesCode, scientificName, commonName, mortalityEquation, brkEqu, crownCoefficientCode, Alaska, California, EasternArea, GreatBasin, NorthernRockies, Northwest, RocketyMountain, SouthernArea, SouthWest, equationType, crownDamageEquationCode);
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

SIGMortality.prototype['initializeMembers'] = SIGMortality.prototype.initializeMembers = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGMortality_initializeMembers_0(self);
};;

SIGMortality.prototype['checkIsInGACCRegionAtSpeciesTableIndex'] = SIGMortality.prototype.checkIsInGACCRegionAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, region) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  return !!(_emscripten_bind_SIGMortality_checkIsInGACCRegionAtSpeciesTableIndex_2(self, index, region));
};;

SIGMortality.prototype['checkIsInGACCRegionFromSpeciesCode'] = SIGMortality.prototype.checkIsInGACCRegionFromSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode, region) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  if (region && typeof region === 'object') region = region.ptr;
  return !!(_emscripten_bind_SIGMortality_checkIsInGACCRegionFromSpeciesCode_2(self, speciesCode, region));
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

SIGMortality.prototype['calculateScorchHeight'] = SIGMortality.prototype.calculateScorchHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensity, firelineIntensityUnits, midFlameWindSpeed, windSpeedUnits, airTemperature, temperatureUnits, scorchHeightUnits) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  if (midFlameWindSpeed && typeof midFlameWindSpeed === 'object') midFlameWindSpeed = midFlameWindSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_calculateScorchHeight_7(self, firelineIntensity, firelineIntensityUnits, midFlameWindSpeed, windSpeedUnits, airTemperature, temperatureUnits, scorchHeightUnits);
};;

SIGMortality.prototype['calculateMortalityAllDirections'] = SIGMortality.prototype.calculateMortalityAllDirections = /** @suppress {undefinedVars, duplicate} @this{Object} */function(probablityUnits) {
  var self = this.ptr;
  if (probablityUnits && typeof probablityUnits === 'object') probablityUnits = probablityUnits.ptr;
  _emscripten_bind_SIGMortality_calculateMortalityAllDirections_1(self, probablityUnits);
};;

SIGMortality.prototype['getRequiredFieldVector'] = SIGMortality.prototype.getRequiredFieldVector = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return wrapPointer(_emscripten_bind_SIGMortality_getRequiredFieldVector_0(self), BoolVector);
};;

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

SIGMortality.prototype['getSpeciesCode'] = SIGMortality.prototype.getSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getSpeciesCode_0(self));
};;

SIGMortality.prototype['getSpeciesCodeAtSpeciesTableIndex'] = SIGMortality.prototype.getSpeciesCodeAtSpeciesTableIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getSpeciesCodeAtSpeciesTableIndex_1(self, index));
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

SIGMortality.prototype['getGACCRegion'] = SIGMortality.prototype.getGACCRegion = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getGACCRegion_0(self);
};;

SIGMortality.prototype['getSpeciesRecordVectorForGACCRegion'] = SIGMortality.prototype.getSpeciesRecordVectorForGACCRegion = /** @suppress {undefinedVars, duplicate} @this{Object} */function(region) {
  var self = this.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  return wrapPointer(_emscripten_bind_SIGMortality_getSpeciesRecordVectorForGACCRegion_1(self, region), SpeciesMasterTableRecordVector);
};;

SIGMortality.prototype['getSpeciesRecordVectorForGACCRegionAndEquationType'] = SIGMortality.prototype.getSpeciesRecordVectorForGACCRegionAndEquationType = /** @suppress {undefinedVars, duplicate} @this{Object} */function(region, equationType) {
  var self = this.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  if (equationType && typeof equationType === 'object') equationType = equationType.ptr;
  return wrapPointer(_emscripten_bind_SIGMortality_getSpeciesRecordVectorForGACCRegionAndEquationType_2(self, region, equationType), SpeciesMasterTableRecordVector);
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

SIGMortality.prototype['getBoleCharHeightBacking'] = SIGMortality.prototype.getBoleCharHeightBacking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(boleCharHeightUnits) {
  var self = this.ptr;
  if (boleCharHeightUnits && typeof boleCharHeightUnits === 'object') boleCharHeightUnits = boleCharHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getBoleCharHeightBacking_1(self, boleCharHeightUnits);
};;

SIGMortality.prototype['getBoleCharHeightFlanking'] = SIGMortality.prototype.getBoleCharHeightFlanking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(boleCharHeightUnits) {
  var self = this.ptr;
  if (boleCharHeightUnits && typeof boleCharHeightUnits === 'object') boleCharHeightUnits = boleCharHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getBoleCharHeightFlanking_1(self, boleCharHeightUnits);
};;

SIGMortality.prototype['getCambiumKillRating'] = SIGMortality.prototype.getCambiumKillRating = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCambiumKillRating_0(self);
};;

SIGMortality.prototype['getCrownDamage'] = SIGMortality.prototype.getCrownDamage = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getCrownDamage_0(self);
};;

SIGMortality.prototype['getCrownRatio'] = SIGMortality.prototype.getCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownRatioUnits) {
  var self = this.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  return _emscripten_bind_SIGMortality_getCrownRatio_1(self, crownRatioUnits);
};;

SIGMortality.prototype['getCVSorCLS'] = SIGMortality.prototype.getCVSorCLS = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return UTF8ToString(_emscripten_bind_SIGMortality_getCVSorCLS_0(self));
};;

SIGMortality.prototype['getDBH'] = SIGMortality.prototype.getDBH = /** @suppress {undefinedVars, duplicate} @this{Object} */function(diameterUnits) {
  var self = this.ptr;
  if (diameterUnits && typeof diameterUnits === 'object') diameterUnits = diameterUnits.ptr;
  return _emscripten_bind_SIGMortality_getDBH_1(self, diameterUnits);
};;

SIGMortality.prototype['getFlameLength'] = SIGMortality.prototype.getFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLengthUnits) {
  var self = this.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  return _emscripten_bind_SIGMortality_getFlameLength_1(self, flameLengthUnits);
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

SIGMortality.prototype['getProbabilityOfMortalityBacking'] = SIGMortality.prototype.getProbabilityOfMortalityBacking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(probabilityUnits) {
  var self = this.ptr;
  if (probabilityUnits && typeof probabilityUnits === 'object') probabilityUnits = probabilityUnits.ptr;
  return _emscripten_bind_SIGMortality_getProbabilityOfMortalityBacking_1(self, probabilityUnits);
};;

SIGMortality.prototype['getProbabilityOfMortalityFlanking'] = SIGMortality.prototype.getProbabilityOfMortalityFlanking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(probabilityUnits) {
  var self = this.ptr;
  if (probabilityUnits && typeof probabilityUnits === 'object') probabilityUnits = probabilityUnits.ptr;
  return _emscripten_bind_SIGMortality_getProbabilityOfMortalityFlanking_1(self, probabilityUnits);
};;

SIGMortality.prototype['getScorchHeight'] = SIGMortality.prototype.getScorchHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(scorchHeightUnits) {
  var self = this.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getScorchHeight_1(self, scorchHeightUnits);
};;

SIGMortality.prototype['getScorchHeightBacking'] = SIGMortality.prototype.getScorchHeightBacking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(scorchHeightUnits) {
  var self = this.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getScorchHeightBacking_1(self, scorchHeightUnits);
};;

SIGMortality.prototype['getScorchHeightFlanking'] = SIGMortality.prototype.getScorchHeightFlanking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(scorchHeightUnits) {
  var self = this.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  return _emscripten_bind_SIGMortality_getScorchHeightFlanking_1(self, scorchHeightUnits);
};;

SIGMortality.prototype['getTotalPrefireTrees'] = SIGMortality.prototype.getTotalPrefireTrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGMortality_getTotalPrefireTrees_0(self);
};;

SIGMortality.prototype['getTreeCrownLengthScorched'] = SIGMortality.prototype.getTreeCrownLengthScorched = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeCrownLengthScorchedUnits) {
  var self = this.ptr;
  if (treeCrownLengthScorchedUnits && typeof treeCrownLengthScorchedUnits === 'object') treeCrownLengthScorchedUnits = treeCrownLengthScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownLengthScorched_1(self, treeCrownLengthScorchedUnits);
};;

SIGMortality.prototype['getTreeCrownLengthScorchedBacking'] = SIGMortality.prototype.getTreeCrownLengthScorchedBacking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeCrownLengthScorchedUnits) {
  var self = this.ptr;
  if (treeCrownLengthScorchedUnits && typeof treeCrownLengthScorchedUnits === 'object') treeCrownLengthScorchedUnits = treeCrownLengthScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownLengthScorchedBacking_1(self, treeCrownLengthScorchedUnits);
};;

SIGMortality.prototype['getTreeCrownLengthScorchedFlanking'] = SIGMortality.prototype.getTreeCrownLengthScorchedFlanking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(treeCrownLengthScorchedUnits) {
  var self = this.ptr;
  if (treeCrownLengthScorchedUnits && typeof treeCrownLengthScorchedUnits === 'object') treeCrownLengthScorchedUnits = treeCrownLengthScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownLengthScorchedFlanking_1(self, treeCrownLengthScorchedUnits);
};;

SIGMortality.prototype['getTreeCrownVolumeScorched'] = SIGMortality.prototype.getTreeCrownVolumeScorched = /** @suppress {undefinedVars, duplicate} @this{Object} */function(getTreeCrownVolumeScorchedUnits) {
  var self = this.ptr;
  if (getTreeCrownVolumeScorchedUnits && typeof getTreeCrownVolumeScorchedUnits === 'object') getTreeCrownVolumeScorchedUnits = getTreeCrownVolumeScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownVolumeScorched_1(self, getTreeCrownVolumeScorchedUnits);
};;

SIGMortality.prototype['getTreeCrownVolumeScorchedBacking'] = SIGMortality.prototype.getTreeCrownVolumeScorchedBacking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(getTreeCrownVolumeScorchedUnits) {
  var self = this.ptr;
  if (getTreeCrownVolumeScorchedUnits && typeof getTreeCrownVolumeScorchedUnits === 'object') getTreeCrownVolumeScorchedUnits = getTreeCrownVolumeScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownVolumeScorchedBacking_1(self, getTreeCrownVolumeScorchedUnits);
};;

SIGMortality.prototype['getTreeCrownVolumeScorchedFlanking'] = SIGMortality.prototype.getTreeCrownVolumeScorchedFlanking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(getTreeCrownVolumeScorchedUnits) {
  var self = this.ptr;
  if (getTreeCrownVolumeScorchedUnits && typeof getTreeCrownVolumeScorchedUnits === 'object') getTreeCrownVolumeScorchedUnits = getTreeCrownVolumeScorchedUnits.ptr;
  return _emscripten_bind_SIGMortality_getTreeCrownVolumeScorchedFlanking_1(self, getTreeCrownVolumeScorchedUnits);
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

SIGMortality.prototype['setAirTemperature'] = SIGMortality.prototype.setAirTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(airTemperature, temperatureUnits) {
  var self = this.ptr;
  if (airTemperature && typeof airTemperature === 'object') airTemperature = airTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  _emscripten_bind_SIGMortality_setAirTemperature_2(self, airTemperature, temperatureUnits);
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

SIGMortality.prototype['setCrownRatio'] = SIGMortality.prototype.setCrownRatio = /** @suppress {undefinedVars, duplicate} @this{Object} */function(crownRatio, crownRatioUnits) {
  var self = this.ptr;
  if (crownRatio && typeof crownRatio === 'object') crownRatio = crownRatio.ptr;
  if (crownRatioUnits && typeof crownRatioUnits === 'object') crownRatioUnits = crownRatioUnits.ptr;
  _emscripten_bind_SIGMortality_setCrownRatio_2(self, crownRatio, crownRatioUnits);
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

SIGMortality.prototype['setFirelineIntensity'] = SIGMortality.prototype.setFirelineIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(firelineIntensity, firelineIntensityUnits) {
  var self = this.ptr;
  if (firelineIntensity && typeof firelineIntensity === 'object') firelineIntensity = firelineIntensity.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  _emscripten_bind_SIGMortality_setFirelineIntensity_2(self, firelineIntensity, firelineIntensityUnits);
};;

SIGMortality.prototype['setFlameLength'] = SIGMortality.prototype.setFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(flameLength, flameLengthUnits) {
  var self = this.ptr;
  if (flameLength && typeof flameLength === 'object') flameLength = flameLength.ptr;
  if (flameLengthUnits && typeof flameLengthUnits === 'object') flameLengthUnits = flameLengthUnits.ptr;
  _emscripten_bind_SIGMortality_setFlameLength_2(self, flameLength, flameLengthUnits);
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

SIGMortality.prototype['setMidFlameWindSpeed'] = SIGMortality.prototype.setMidFlameWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(midFlameWindSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (midFlameWindSpeed && typeof midFlameWindSpeed === 'object') midFlameWindSpeed = midFlameWindSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGMortality_setMidFlameWindSpeed_2(self, midFlameWindSpeed, windSpeedUnits);
};;

SIGMortality.prototype['setGACCRegion'] = SIGMortality.prototype.setGACCRegion = /** @suppress {undefinedVars, duplicate} @this{Object} */function(region) {
  var self = this.ptr;
  if (region && typeof region === 'object') region = region.ptr;
  _emscripten_bind_SIGMortality_setGACCRegion_1(self, region);
};;

SIGMortality.prototype['setScorchHeight'] = SIGMortality.prototype.setScorchHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(scorchHeight, scorchHeightUnits) {
  var self = this.ptr;
  if (scorchHeight && typeof scorchHeight === 'object') scorchHeight = scorchHeight.ptr;
  if (scorchHeightUnits && typeof scorchHeightUnits === 'object') scorchHeightUnits = scorchHeightUnits.ptr;
  _emscripten_bind_SIGMortality_setScorchHeight_2(self, scorchHeight, scorchHeightUnits);
};;

SIGMortality.prototype['setSpeciesCode'] = SIGMortality.prototype.setSpeciesCode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(speciesCode) {
  var self = this.ptr;
  ensureCache.prepare();
  if (speciesCode && typeof speciesCode === 'object') speciesCode = speciesCode.ptr;
  else speciesCode = ensureString(speciesCode);
  _emscripten_bind_SIGMortality_setSpeciesCode_1(self, speciesCode);
};;

SIGMortality.prototype['setSurfaceFireFirelineIntensity'] = SIGMortality.prototype.setSurfaceFireFirelineIntensity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, firelineIntensityUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFirelineIntensity_2(self, value, firelineIntensityUnits);
};;

SIGMortality.prototype['setSurfaceFireFirelineIntensityBacking'] = SIGMortality.prototype.setSurfaceFireFirelineIntensityBacking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, firelineIntensityUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFirelineIntensityBacking_2(self, value, firelineIntensityUnits);
};;

SIGMortality.prototype['setSurfaceFireFirelineIntensityFlanking'] = SIGMortality.prototype.setSurfaceFireFirelineIntensityFlanking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, firelineIntensityUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (firelineIntensityUnits && typeof firelineIntensityUnits === 'object') firelineIntensityUnits = firelineIntensityUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFirelineIntensityFlanking_2(self, value, firelineIntensityUnits);
};;

SIGMortality.prototype['setSurfaceFireFlameLength'] = SIGMortality.prototype.setSurfaceFireFlameLength = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, lengthUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFlameLength_2(self, value, lengthUnits);
};;

SIGMortality.prototype['setSurfaceFireFlameLengthBacking'] = SIGMortality.prototype.setSurfaceFireFlameLengthBacking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, lengthUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFlameLengthBacking_2(self, value, lengthUnits);
};;

SIGMortality.prototype['setSurfaceFireFlameLengthFlanking'] = SIGMortality.prototype.setSurfaceFireFlameLengthFlanking = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, lengthUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireFlameLengthFlanking_2(self, value, lengthUnits);
};;

SIGMortality.prototype['setSurfaceFireScorchHeight'] = SIGMortality.prototype.setSurfaceFireScorchHeight = /** @suppress {undefinedVars, duplicate} @this{Object} */function(value, lengthUnits) {
  var self = this.ptr;
  if (value && typeof value === 'object') value = value.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_SIGMortality_setSurfaceFireScorchHeight_2(self, value, lengthUnits);
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

SIGMortality.prototype['setUserProvidedWindAdjustmentFactor'] = SIGMortality.prototype.setUserProvidedWindAdjustmentFactor = /** @suppress {undefinedVars, duplicate} @this{Object} */function(userProvidedWindAdjustmentFactor) {
  var self = this.ptr;
  if (userProvidedWindAdjustmentFactor && typeof userProvidedWindAdjustmentFactor === 'object') userProvidedWindAdjustmentFactor = userProvidedWindAdjustmentFactor.ptr;
  _emscripten_bind_SIGMortality_setUserProvidedWindAdjustmentFactor_1(self, userProvidedWindAdjustmentFactor);
};;

SIGMortality.prototype['setWindHeightInputMode'] = SIGMortality.prototype.setWindHeightInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windHeightInputMode) {
  var self = this.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  _emscripten_bind_SIGMortality_setWindHeightInputMode_1(self, windHeightInputMode);
};;

SIGMortality.prototype['setWindSpeed'] = SIGMortality.prototype.setWindSpeed = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windSpeed, windSpeedUnits) {
  var self = this.ptr;
  if (windSpeed && typeof windSpeed === 'object') windSpeed = windSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  _emscripten_bind_SIGMortality_setWindSpeed_2(self, windSpeed, windSpeedUnits);
};;

SIGMortality.prototype['setWindSpeedAndWindHeightInputMode'] = SIGMortality.prototype.setWindSpeedAndWindHeightInputMode = /** @suppress {undefinedVars, duplicate} @this{Object} */function(windwindSpeed, windSpeedUnits, windHeightInputMode, userProvidedWindAdjustmentFactor) {
  var self = this.ptr;
  if (windwindSpeed && typeof windwindSpeed === 'object') windwindSpeed = windwindSpeed.ptr;
  if (windSpeedUnits && typeof windSpeedUnits === 'object') windSpeedUnits = windSpeedUnits.ptr;
  if (windHeightInputMode && typeof windHeightInputMode === 'object') windHeightInputMode = windHeightInputMode.ptr;
  if (userProvidedWindAdjustmentFactor && typeof userProvidedWindAdjustmentFactor === 'object') userProvidedWindAdjustmentFactor = userProvidedWindAdjustmentFactor.ptr;
  _emscripten_bind_SIGMortality_setWindSpeedAndWindHeightInputMode_4(self, windwindSpeed, windSpeedUnits, windHeightInputMode, userProvidedWindAdjustmentFactor);
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
// SIGFineDeadFuelMoistureTool
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGFineDeadFuelMoistureTool() {
  this.ptr = _emscripten_bind_SIGFineDeadFuelMoistureTool_SIGFineDeadFuelMoistureTool_0();
  getCache(SIGFineDeadFuelMoistureTool)[this.ptr] = this;
};;
SIGFineDeadFuelMoistureTool.prototype = Object.create(WrapperObject.prototype);
SIGFineDeadFuelMoistureTool.prototype.constructor = SIGFineDeadFuelMoistureTool;
SIGFineDeadFuelMoistureTool.prototype.__class__ = SIGFineDeadFuelMoistureTool;
SIGFineDeadFuelMoistureTool.__cache__ = {};
Module['SIGFineDeadFuelMoistureTool'] = SIGFineDeadFuelMoistureTool;

SIGFineDeadFuelMoistureTool.prototype['calculate'] = SIGFineDeadFuelMoistureTool.prototype.calculate = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_calculate_0(self);
};;

SIGFineDeadFuelMoistureTool.prototype['setTimeOfDayIndex'] = SIGFineDeadFuelMoistureTool.prototype.setTimeOfDayIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(timeOfDayIndex) {
  var self = this.ptr;
  if (timeOfDayIndex && typeof timeOfDayIndex === 'object') timeOfDayIndex = timeOfDayIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setTimeOfDayIndex_1(self, timeOfDayIndex);
};;

SIGFineDeadFuelMoistureTool.prototype['setSlopeIndex'] = SIGFineDeadFuelMoistureTool.prototype.setSlopeIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(slopeIndex) {
  var self = this.ptr;
  if (slopeIndex && typeof slopeIndex === 'object') slopeIndex = slopeIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setSlopeIndex_1(self, slopeIndex);
};;

SIGFineDeadFuelMoistureTool.prototype['setShadingIndex'] = SIGFineDeadFuelMoistureTool.prototype.setShadingIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(shadingIndex) {
  var self = this.ptr;
  if (shadingIndex && typeof shadingIndex === 'object') shadingIndex = shadingIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setShadingIndex_1(self, shadingIndex);
};;

SIGFineDeadFuelMoistureTool.prototype['setAspectIndex'] = SIGFineDeadFuelMoistureTool.prototype.setAspectIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspectIndex) {
  var self = this.ptr;
  if (aspectIndex && typeof aspectIndex === 'object') aspectIndex = aspectIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setAspectIndex_1(self, aspectIndex);
};;

SIGFineDeadFuelMoistureTool.prototype['setRHIndex'] = SIGFineDeadFuelMoistureTool.prototype.setRHIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(relativeHumidityIndex) {
  var self = this.ptr;
  if (relativeHumidityIndex && typeof relativeHumidityIndex === 'object') relativeHumidityIndex = relativeHumidityIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setRHIndex_1(self, relativeHumidityIndex);
};;

SIGFineDeadFuelMoistureTool.prototype['setElevationIndex'] = SIGFineDeadFuelMoistureTool.prototype.setElevationIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(elevationIndex) {
  var self = this.ptr;
  if (elevationIndex && typeof elevationIndex === 'object') elevationIndex = elevationIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setElevationIndex_1(self, elevationIndex);
};;

SIGFineDeadFuelMoistureTool.prototype['setDryBulbIndex'] = SIGFineDeadFuelMoistureTool.prototype.setDryBulbIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(dryBulbIndex) {
  var self = this.ptr;
  if (dryBulbIndex && typeof dryBulbIndex === 'object') dryBulbIndex = dryBulbIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setDryBulbIndex_1(self, dryBulbIndex);
};;

SIGFineDeadFuelMoistureTool.prototype['setMonthIndex'] = SIGFineDeadFuelMoistureTool.prototype.setMonthIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(monthIndex) {
  var self = this.ptr;
  if (monthIndex && typeof monthIndex === 'object') monthIndex = monthIndex.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool_setMonthIndex_1(self, monthIndex);
};;

SIGFineDeadFuelMoistureTool.prototype['getFineDeadFuelMoisture'] = SIGFineDeadFuelMoistureTool.prototype.getFineDeadFuelMoisture = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getFineDeadFuelMoisture_1(self, desiredUnits);
};;

SIGFineDeadFuelMoistureTool.prototype['getSlopeIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getSlopeIndexSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getSlopeIndexSize_0(self);
};;

SIGFineDeadFuelMoistureTool.prototype['getElevationIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getElevationIndexSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getElevationIndexSize_0(self);
};;

SIGFineDeadFuelMoistureTool.prototype['getMonthIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getMonthIndexSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getMonthIndexSize_0(self);
};;

SIGFineDeadFuelMoistureTool.prototype['getDryBulbTemperatureIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getDryBulbTemperatureIndexSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getDryBulbTemperatureIndexSize_0(self);
};;

SIGFineDeadFuelMoistureTool.prototype['getReferenceMoisture'] = SIGFineDeadFuelMoistureTool.prototype.getReferenceMoisture = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getReferenceMoisture_1(self, desiredUnits);
};;

SIGFineDeadFuelMoistureTool.prototype['calculateByIndex'] = SIGFineDeadFuelMoistureTool.prototype.calculateByIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(aspectIndex, dryBulbIndex, elevationIndex, monthIndex, relativeHumidityIndex, shadingIndex, slopeIndex, timeOfDayIndex) {
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
};;

SIGFineDeadFuelMoistureTool.prototype['getTimeOfDayIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getTimeOfDayIndexSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getTimeOfDayIndexSize_0(self);
};;

SIGFineDeadFuelMoistureTool.prototype['getCorrectionMoisture'] = SIGFineDeadFuelMoistureTool.prototype.getCorrectionMoisture = /** @suppress {undefinedVars, duplicate} @this{Object} */function(desiredUnits) {
  var self = this.ptr;
  if (desiredUnits && typeof desiredUnits === 'object') desiredUnits = desiredUnits.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getCorrectionMoisture_1(self, desiredUnits);
};;

SIGFineDeadFuelMoistureTool.prototype['getAspectIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getAspectIndexSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getAspectIndexSize_0(self);
};;

SIGFineDeadFuelMoistureTool.prototype['getShadingIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getShadingIndexSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getShadingIndexSize_0(self);
};;

SIGFineDeadFuelMoistureTool.prototype['getRelativeHumidityIndexSize'] = SIGFineDeadFuelMoistureTool.prototype.getRelativeHumidityIndexSize = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGFineDeadFuelMoistureTool_getRelativeHumidityIndexSize_0(self);
};;

  SIGFineDeadFuelMoistureTool.prototype['__destroy__'] = SIGFineDeadFuelMoistureTool.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGFineDeadFuelMoistureTool___destroy___0(self);
};
// SIGSlopeTool
/** @suppress {undefinedVars, duplicate} @this{Object} */function SIGSlopeTool() {
  this.ptr = _emscripten_bind_SIGSlopeTool_SIGSlopeTool_0();
  getCache(SIGSlopeTool)[this.ptr] = this;
};;
SIGSlopeTool.prototype = Object.create(WrapperObject.prototype);
SIGSlopeTool.prototype.constructor = SIGSlopeTool;
SIGSlopeTool.prototype.__class__ = SIGSlopeTool;
SIGSlopeTool.__cache__ = {};
Module['SIGSlopeTool'] = SIGSlopeTool;

SIGSlopeTool.prototype['getCentimetersPerKilometerAtIndex'] = SIGSlopeTool.prototype.getCentimetersPerKilometerAtIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getCentimetersPerKilometerAtIndex_1(self, index);
};;

SIGSlopeTool.prototype['getCentimetersPerKilometerAtRepresentativeFraction'] = SIGSlopeTool.prototype.getCentimetersPerKilometerAtRepresentativeFraction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getCentimetersPerKilometerAtRepresentativeFraction_1(self, representativeFraction);
};;

SIGSlopeTool.prototype['getHorizontalDistance'] = SIGSlopeTool.prototype.getHorizontalDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(horizontalDistanceIndex, mapDistanceUnits) {
  var self = this.ptr;
  if (horizontalDistanceIndex && typeof horizontalDistanceIndex === 'object') horizontalDistanceIndex = horizontalDistanceIndex.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistance_2(self, horizontalDistanceIndex, mapDistanceUnits);
};;

SIGSlopeTool.prototype['getHorizontalDistanceAtIndex'] = SIGSlopeTool.prototype.getHorizontalDistanceAtIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index, mapDistanceUnits) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceAtIndex_2(self, index, mapDistanceUnits);
};;

SIGSlopeTool.prototype['getHorizontalDistanceFifteen'] = SIGSlopeTool.prototype.getHorizontalDistanceFifteen = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceFifteen_1(self, mapDistanceUnits);
};;

SIGSlopeTool.prototype['getHorizontalDistanceFourtyFive'] = SIGSlopeTool.prototype.getHorizontalDistanceFourtyFive = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceFourtyFive_1(self, mapDistanceUnits);
};;

SIGSlopeTool.prototype['getHorizontalDistanceMaxSlope'] = SIGSlopeTool.prototype.getHorizontalDistanceMaxSlope = /** @suppress {undefinedVars, duplicate} @this{Object} */function(slopeUnits) {
  var self = this.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceMaxSlope_1(self, slopeUnits);
};;

SIGSlopeTool.prototype['getHorizontalDistanceNinety'] = SIGSlopeTool.prototype.getHorizontalDistanceNinety = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceNinety_1(self, mapDistanceUnits);
};;

SIGSlopeTool.prototype['getHorizontalDistanceSeventy'] = SIGSlopeTool.prototype.getHorizontalDistanceSeventy = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceSeventy_1(self, mapDistanceUnits);
};;

SIGSlopeTool.prototype['getHorizontalDistanceSixty'] = SIGSlopeTool.prototype.getHorizontalDistanceSixty = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceSixty_1(self, mapDistanceUnits);
};;

SIGSlopeTool.prototype['getHorizontalDistanceThirty'] = SIGSlopeTool.prototype.getHorizontalDistanceThirty = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceThirty_1(self, mapDistanceUnits);
};;

SIGSlopeTool.prototype['getHorizontalDistanceZero'] = SIGSlopeTool.prototype.getHorizontalDistanceZero = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mapDistanceUnits) {
  var self = this.ptr;
  if (mapDistanceUnits && typeof mapDistanceUnits === 'object') mapDistanceUnits = mapDistanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getHorizontalDistanceZero_1(self, mapDistanceUnits);
};;

SIGSlopeTool.prototype['getInchesPerMileAtIndex'] = SIGSlopeTool.prototype.getInchesPerMileAtIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getInchesPerMileAtIndex_1(self, index);
};;

SIGSlopeTool.prototype['getInchesPerMileAtRepresentativeFraction'] = SIGSlopeTool.prototype.getInchesPerMileAtRepresentativeFraction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getInchesPerMileAtRepresentativeFraction_1(self, representativeFraction);
};;

SIGSlopeTool.prototype['getKilometersPerCentimeterAtIndex'] = SIGSlopeTool.prototype.getKilometersPerCentimeterAtIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getKilometersPerCentimeterAtIndex_1(self, index);
};;

SIGSlopeTool.prototype['getKilometersPerCentimeterAtRepresentativeFraction'] = SIGSlopeTool.prototype.getKilometersPerCentimeterAtRepresentativeFraction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getKilometersPerCentimeterAtRepresentativeFraction_1(self, representativeFraction);
};;

SIGSlopeTool.prototype['getMilesPerInchAtIndex'] = SIGSlopeTool.prototype.getMilesPerInchAtIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getMilesPerInchAtIndex_1(self, index);
};;

SIGSlopeTool.prototype['getMilesPerInchAtRepresentativeFraction'] = SIGSlopeTool.prototype.getMilesPerInchAtRepresentativeFraction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getMilesPerInchAtRepresentativeFraction_1(self, representativeFraction);
};;

SIGSlopeTool.prototype['getSlopeElevationChangeFromMapMeasurements'] = SIGSlopeTool.prototype.getSlopeElevationChangeFromMapMeasurements = /** @suppress {undefinedVars, duplicate} @this{Object} */function(elevationUnits) {
  var self = this.ptr;
  if (elevationUnits && typeof elevationUnits === 'object') elevationUnits = elevationUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeElevationChangeFromMapMeasurements_1(self, elevationUnits);
};;

SIGSlopeTool.prototype['getSlopeFromMapMeasurements'] = SIGSlopeTool.prototype.getSlopeFromMapMeasurements = /** @suppress {undefinedVars, duplicate} @this{Object} */function(slopeUnits) {
  var self = this.ptr;
  if (slopeUnits && typeof slopeUnits === 'object') slopeUnits = slopeUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeFromMapMeasurements_1(self, slopeUnits);
};;

SIGSlopeTool.prototype['getSlopeHorizontalDistanceFromMapMeasurements'] = SIGSlopeTool.prototype.getSlopeHorizontalDistanceFromMapMeasurements = /** @suppress {undefinedVars, duplicate} @this{Object} */function(distanceUnits) {
  var self = this.ptr;
  if (distanceUnits && typeof distanceUnits === 'object') distanceUnits = distanceUnits.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeHorizontalDistanceFromMapMeasurements_1(self, distanceUnits);
};;

SIGSlopeTool.prototype['getSlopeFromMapMeasurementsInDegrees'] = SIGSlopeTool.prototype.getSlopeFromMapMeasurementsInDegrees = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeFromMapMeasurementsInDegrees_0(self);
};;

SIGSlopeTool.prototype['getSlopeFromMapMeasurementsInPercent'] = SIGSlopeTool.prototype.getSlopeFromMapMeasurementsInPercent = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSlopeTool_getSlopeFromMapMeasurementsInPercent_0(self);
};;

SIGSlopeTool.prototype['getNumberOfHorizontalDistances'] = SIGSlopeTool.prototype.getNumberOfHorizontalDistances = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSlopeTool_getNumberOfHorizontalDistances_0(self);
};;

SIGSlopeTool.prototype['getNumberOfRepresentativeFractions'] = SIGSlopeTool.prototype.getNumberOfRepresentativeFractions = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  return _emscripten_bind_SIGSlopeTool_getNumberOfRepresentativeFractions_0(self);
};;

SIGSlopeTool.prototype['getRepresentativeFractionAtIndex'] = SIGSlopeTool.prototype.getRepresentativeFractionAtIndex = /** @suppress {undefinedVars, duplicate} @this{Object} */function(index) {
  var self = this.ptr;
  if (index && typeof index === 'object') index = index.ptr;
  return _emscripten_bind_SIGSlopeTool_getRepresentativeFractionAtIndex_1(self, index);
};;

SIGSlopeTool.prototype['getRepresentativeFractionAtRepresentativeFraction'] = SIGSlopeTool.prototype.getRepresentativeFractionAtRepresentativeFraction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(representativeFraction) {
  var self = this.ptr;
  if (representativeFraction && typeof representativeFraction === 'object') representativeFraction = representativeFraction.ptr;
  return _emscripten_bind_SIGSlopeTool_getRepresentativeFractionAtRepresentativeFraction_1(self, representativeFraction);
};;

SIGSlopeTool.prototype['calculateHorizontalDistance'] = SIGSlopeTool.prototype.calculateHorizontalDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSlopeTool_calculateHorizontalDistance_0(self);
};;

SIGSlopeTool.prototype['calculateSlopeFromMapMeasurements'] = SIGSlopeTool.prototype.calculateSlopeFromMapMeasurements = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSlopeTool_calculateSlopeFromMapMeasurements_0(self);
};;

SIGSlopeTool.prototype['setCalculatedMapDistance'] = SIGSlopeTool.prototype.setCalculatedMapDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(calculatedMapDistance, distanceUnits) {
  var self = this.ptr;
  if (calculatedMapDistance && typeof calculatedMapDistance === 'object') calculatedMapDistance = calculatedMapDistance.ptr;
  if (distanceUnits && typeof distanceUnits === 'object') distanceUnits = distanceUnits.ptr;
  _emscripten_bind_SIGSlopeTool_setCalculatedMapDistance_2(self, calculatedMapDistance, distanceUnits);
};;

SIGSlopeTool.prototype['setContourInterval'] = SIGSlopeTool.prototype.setContourInterval = /** @suppress {undefinedVars, duplicate} @this{Object} */function(contourInterval, contourUnits) {
  var self = this.ptr;
  if (contourInterval && typeof contourInterval === 'object') contourInterval = contourInterval.ptr;
  if (contourUnits && typeof contourUnits === 'object') contourUnits = contourUnits.ptr;
  _emscripten_bind_SIGSlopeTool_setContourInterval_2(self, contourInterval, contourUnits);
};;

SIGSlopeTool.prototype['setMapDistance'] = SIGSlopeTool.prototype.setMapDistance = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mapDistance, distanceUnits) {
  var self = this.ptr;
  if (mapDistance && typeof mapDistance === 'object') mapDistance = mapDistance.ptr;
  if (distanceUnits && typeof distanceUnits === 'object') distanceUnits = distanceUnits.ptr;
  _emscripten_bind_SIGSlopeTool_setMapDistance_2(self, mapDistance, distanceUnits);
};;

SIGSlopeTool.prototype['setMapRepresentativeFraction'] = SIGSlopeTool.prototype.setMapRepresentativeFraction = /** @suppress {undefinedVars, duplicate} @this{Object} */function(mapRepresentativeFraction) {
  var self = this.ptr;
  if (mapRepresentativeFraction && typeof mapRepresentativeFraction === 'object') mapRepresentativeFraction = mapRepresentativeFraction.ptr;
  _emscripten_bind_SIGSlopeTool_setMapRepresentativeFraction_1(self, mapRepresentativeFraction);
};;

SIGSlopeTool.prototype['setMaxSlopeSteepness'] = SIGSlopeTool.prototype.setMaxSlopeSteepness = /** @suppress {undefinedVars, duplicate} @this{Object} */function(maxSlopeSteepness) {
  var self = this.ptr;
  if (maxSlopeSteepness && typeof maxSlopeSteepness === 'object') maxSlopeSteepness = maxSlopeSteepness.ptr;
  _emscripten_bind_SIGSlopeTool_setMaxSlopeSteepness_1(self, maxSlopeSteepness);
};;

SIGSlopeTool.prototype['setNumberOfContours'] = SIGSlopeTool.prototype.setNumberOfContours = /** @suppress {undefinedVars, duplicate} @this{Object} */function(numberOfContours) {
  var self = this.ptr;
  if (numberOfContours && typeof numberOfContours === 'object') numberOfContours = numberOfContours.ptr;
  _emscripten_bind_SIGSlopeTool_setNumberOfContours_1(self, numberOfContours);
};;

  SIGSlopeTool.prototype['__destroy__'] = SIGSlopeTool.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_SIGSlopeTool___destroy___0(self);
};
// VaporPressureDeficitCalculator
/** @suppress {undefinedVars, duplicate} @this{Object} */function VaporPressureDeficitCalculator() {
  this.ptr = _emscripten_bind_VaporPressureDeficitCalculator_VaporPressureDeficitCalculator_0();
  getCache(VaporPressureDeficitCalculator)[this.ptr] = this;
};;
VaporPressureDeficitCalculator.prototype = Object.create(WrapperObject.prototype);
VaporPressureDeficitCalculator.prototype.constructor = VaporPressureDeficitCalculator;
VaporPressureDeficitCalculator.prototype.__class__ = VaporPressureDeficitCalculator;
VaporPressureDeficitCalculator.__cache__ = {};
Module['VaporPressureDeficitCalculator'] = VaporPressureDeficitCalculator;

VaporPressureDeficitCalculator.prototype['runCalculation'] = VaporPressureDeficitCalculator.prototype.runCalculation = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_VaporPressureDeficitCalculator_runCalculation_0(self);
};;

VaporPressureDeficitCalculator.prototype['setTemperature'] = VaporPressureDeficitCalculator.prototype.setTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(temperature, units) {
  var self = this.ptr;
  if (temperature && typeof temperature === 'object') temperature = temperature.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  _emscripten_bind_VaporPressureDeficitCalculator_setTemperature_2(self, temperature, units);
};;

VaporPressureDeficitCalculator.prototype['setRelativeHumidity'] = VaporPressureDeficitCalculator.prototype.setRelativeHumidity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(relativeHumidity, units) {
  var self = this.ptr;
  if (relativeHumidity && typeof relativeHumidity === 'object') relativeHumidity = relativeHumidity.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  _emscripten_bind_VaporPressureDeficitCalculator_setRelativeHumidity_2(self, relativeHumidity, units);
};;

VaporPressureDeficitCalculator.prototype['getVaporPressureDeficit'] = VaporPressureDeficitCalculator.prototype.getVaporPressureDeficit = /** @suppress {undefinedVars, duplicate} @this{Object} */function(units) {
  var self = this.ptr;
  if (units && typeof units === 'object') units = units.ptr;
  return _emscripten_bind_VaporPressureDeficitCalculator_getVaporPressureDeficit_1(self, units);
};;

  VaporPressureDeficitCalculator.prototype['__destroy__'] = VaporPressureDeficitCalculator.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_VaporPressureDeficitCalculator___destroy___0(self);
};
// RelativeHumidityTool
/** @suppress {undefinedVars, duplicate} @this{Object} */function RelativeHumidityTool() {
  this.ptr = _emscripten_bind_RelativeHumidityTool_RelativeHumidityTool_0();
  getCache(RelativeHumidityTool)[this.ptr] = this;
};;
RelativeHumidityTool.prototype = Object.create(WrapperObject.prototype);
RelativeHumidityTool.prototype.constructor = RelativeHumidityTool;
RelativeHumidityTool.prototype.__class__ = RelativeHumidityTool;
RelativeHumidityTool.__cache__ = {};
Module['RelativeHumidityTool'] = RelativeHumidityTool;

RelativeHumidityTool.prototype['calculate'] = RelativeHumidityTool.prototype.calculate = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_RelativeHumidityTool_calculate_0(self);
};;

RelativeHumidityTool.prototype['getDryBulbTemperature'] = RelativeHumidityTool.prototype.getDryBulbTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(temperatureUnits) {
  var self = this.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  return _emscripten_bind_RelativeHumidityTool_getDryBulbTemperature_1(self, temperatureUnits);
};;

RelativeHumidityTool.prototype['getSiteElevation'] = RelativeHumidityTool.prototype.getSiteElevation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(lengthUnits) {
  var self = this.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  return _emscripten_bind_RelativeHumidityTool_getSiteElevation_1(self, lengthUnits);
};;

RelativeHumidityTool.prototype['getWetBulbTemperature'] = RelativeHumidityTool.prototype.getWetBulbTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(temperatureUnits) {
  var self = this.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  return _emscripten_bind_RelativeHumidityTool_getWetBulbTemperature_1(self, temperatureUnits);
};;

RelativeHumidityTool.prototype['getDewPointTemperature'] = RelativeHumidityTool.prototype.getDewPointTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(temperatureUnits) {
  var self = this.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  return _emscripten_bind_RelativeHumidityTool_getDewPointTemperature_1(self, temperatureUnits);
};;

RelativeHumidityTool.prototype['getRelativeHumidity'] = RelativeHumidityTool.prototype.getRelativeHumidity = /** @suppress {undefinedVars, duplicate} @this{Object} */function(fractionUnits) {
  var self = this.ptr;
  if (fractionUnits && typeof fractionUnits === 'object') fractionUnits = fractionUnits.ptr;
  return _emscripten_bind_RelativeHumidityTool_getRelativeHumidity_1(self, fractionUnits);
};;

RelativeHumidityTool.prototype['getWetBulbDepression'] = RelativeHumidityTool.prototype.getWetBulbDepression = /** @suppress {undefinedVars, duplicate} @this{Object} */function(temperatureUnits) {
  var self = this.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  return _emscripten_bind_RelativeHumidityTool_getWetBulbDepression_1(self, temperatureUnits);
};;

RelativeHumidityTool.prototype['setDryBulbTemperature'] = RelativeHumidityTool.prototype.setDryBulbTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(dryBulbTemperature, temperatureUnits) {
  var self = this.ptr;
  if (dryBulbTemperature && typeof dryBulbTemperature === 'object') dryBulbTemperature = dryBulbTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  _emscripten_bind_RelativeHumidityTool_setDryBulbTemperature_2(self, dryBulbTemperature, temperatureUnits);
};;

RelativeHumidityTool.prototype['setSiteElevation'] = RelativeHumidityTool.prototype.setSiteElevation = /** @suppress {undefinedVars, duplicate} @this{Object} */function(siteElevation, lengthUnits) {
  var self = this.ptr;
  if (siteElevation && typeof siteElevation === 'object') siteElevation = siteElevation.ptr;
  if (lengthUnits && typeof lengthUnits === 'object') lengthUnits = lengthUnits.ptr;
  _emscripten_bind_RelativeHumidityTool_setSiteElevation_2(self, siteElevation, lengthUnits);
};;

RelativeHumidityTool.prototype['setWetBulbTemperature'] = RelativeHumidityTool.prototype.setWetBulbTemperature = /** @suppress {undefinedVars, duplicate} @this{Object} */function(wetBulbTemperature, temperatureUnits) {
  var self = this.ptr;
  if (wetBulbTemperature && typeof wetBulbTemperature === 'object') wetBulbTemperature = wetBulbTemperature.ptr;
  if (temperatureUnits && typeof temperatureUnits === 'object') temperatureUnits = temperatureUnits.ptr;
  _emscripten_bind_RelativeHumidityTool_setWetBulbTemperature_2(self, wetBulbTemperature, temperatureUnits);
};;

  RelativeHumidityTool.prototype['__destroy__'] = RelativeHumidityTool.prototype.__destroy__ = /** @suppress {undefinedVars, duplicate} @this{Object} */function() {
  var self = this.ptr;
  _emscripten_bind_RelativeHumidityTool___destroy___0(self);
};
(function() {
  function setupEnums() {
    

    // AreaUnits_AreaUnitsEnum

    Module['AreaUnits']['SquareFeet'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareFeet();

    Module['AreaUnits']['Acres'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_Acres();

    Module['AreaUnits']['Hectares'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_Hectares();

    Module['AreaUnits']['SquareMeters'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMeters();

    Module['AreaUnits']['SquareMiles'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMiles();

    Module['AreaUnits']['SquareKilometers'] = _emscripten_enum_AreaUnits_AreaUnitsEnum_SquareKilometers();

    

    // BasalAreaUnits_BasalAreaUnitsEnum

    Module['BasalAreaUnits']['SquareFeetPerAcre'] = _emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareFeetPerAcre();

    Module['BasalAreaUnits']['SquareMetersPerHectare'] = _emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareMetersPerHectare();

    

    // FractionUnits_FractionUnitsEnum

    Module['FractionUnits']['Fraction'] = _emscripten_enum_FractionUnits_FractionUnitsEnum_Fraction();

    Module['FractionUnits']['Percent'] = _emscripten_enum_FractionUnits_FractionUnitsEnum_Percent();

    

    // LengthUnits_LengthUnitsEnum

    Module['LengthUnits']['Feet'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Feet();

    Module['LengthUnits']['Inches'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Inches();

    Module['LengthUnits']['Millimeters'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Millimeters();

    Module['LengthUnits']['Centimeters'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Centimeters();

    Module['LengthUnits']['Meters'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Meters();

    Module['LengthUnits']['Chains'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Chains();

    Module['LengthUnits']['Miles'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Miles();

    Module['LengthUnits']['Kilometers'] = _emscripten_enum_LengthUnits_LengthUnitsEnum_Kilometers();

    

    // LoadingUnits_LoadingUnitsEnum

    Module['LoadingUnits']['PoundsPerSquareFoot'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_PoundsPerSquareFoot();

    Module['LoadingUnits']['TonsPerAcre'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonsPerAcre();

    Module['LoadingUnits']['TonnesPerHectare'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonnesPerHectare();

    Module['LoadingUnits']['KilogramsPerSquareMeter'] = _emscripten_enum_LoadingUnits_LoadingUnitsEnum_KilogramsPerSquareMeter();

    

    // SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum

    Module['SurfaceAreaToVolumeUnits']['SquareFeetOverCubicFeet'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareFeetOverCubicFeet();

    Module['SurfaceAreaToVolumeUnits']['SquareMetersOverCubicMeters'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareMetersOverCubicMeters();

    Module['SurfaceAreaToVolumeUnits']['SquareInchesOverCubicInches'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareInchesOverCubicInches();

    Module['SurfaceAreaToVolumeUnits']['SquareCentimetersOverCubicCentimeters'] = _emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareCentimetersOverCubicCentimeters();

    

    // SpeedUnits_SpeedUnitsEnum

    Module['SpeedUnits']['FeetPerMinute'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_FeetPerMinute();

    Module['SpeedUnits']['ChainsPerHour'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_ChainsPerHour();

    Module['SpeedUnits']['MetersPerSecond'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerSecond();

    Module['SpeedUnits']['MetersPerMinute'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerMinute();

    Module['SpeedUnits']['MilesPerHour'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_MilesPerHour();

    Module['SpeedUnits']['KilometersPerHour'] = _emscripten_enum_SpeedUnits_SpeedUnitsEnum_KilometersPerHour();

    

    // PressureUnits_PressureUnitsEnum

    Module['PressureUnits']['Pascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_Pascal();

    Module['PressureUnits']['HectoPascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_HectoPascal();

    Module['PressureUnits']['KiloPascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_KiloPascal();

    Module['PressureUnits']['MegaPascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_MegaPascal();

    Module['PressureUnits']['GigaPascal'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_GigaPascal();

    Module['PressureUnits']['Bar'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_Bar();

    Module['PressureUnits']['Atmosphere'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_Atmosphere();

    Module['PressureUnits']['TechnicalAtmosphere'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_TechnicalAtmosphere();

    Module['PressureUnits']['PoundPerSquareInch'] = _emscripten_enum_PressureUnits_PressureUnitsEnum_PoundPerSquareInch();

    

    // SlopeUnits_SlopeUnitsEnum

    Module['SlopeUnits']['Degrees'] = _emscripten_enum_SlopeUnits_SlopeUnitsEnum_Degrees();

    Module['SlopeUnits']['Percent'] = _emscripten_enum_SlopeUnits_SlopeUnitsEnum_Percent();

    

    // DensityUnits_DensityUnitsEnum

    Module['DensityUnits']['PoundsPerCubicFoot'] = _emscripten_enum_DensityUnits_DensityUnitsEnum_PoundsPerCubicFoot();

    Module['DensityUnits']['KilogramsPerCubicMeter'] = _emscripten_enum_DensityUnits_DensityUnitsEnum_KilogramsPerCubicMeter();

    

    // HeatOfCombustionUnits_HeatOfCombustionUnitsEnum

    Module['HeatOfCombustionUnits']['BtusPerPound'] = _emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_BtusPerPound();

    Module['HeatOfCombustionUnits']['KilojoulesPerKilogram'] = _emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_KilojoulesPerKilogram();

    

    // HeatSinkUnits_HeatSinkUnitsEnum

    Module['HeatSinkUnits']['BtusPerCubicFoot'] = _emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_BtusPerCubicFoot();

    Module['HeatSinkUnits']['KilojoulesPerCubicMeter'] = _emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_KilojoulesPerCubicMeter();

    

    // HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum

    Module['HeatPerUnitAreaUnits']['BtusPerSquareFoot'] = _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_BtusPerSquareFoot();

    Module['HeatPerUnitAreaUnits']['KilojoulesPerSquareMeter'] = _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilojoulesPerSquareMeter();

    Module['HeatPerUnitAreaUnits']['KilowattSecondsPerSquareMeter'] = _emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilowattSecondsPerSquareMeter();

    

    // HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum

    Module['HeatSourceAndReactionIntensityUnits']['BtusPerSquareFootPerMinute'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerMinute();

    Module['HeatSourceAndReactionIntensityUnits']['BtusPerSquareFootPerSecond'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerSecond();

    Module['HeatSourceAndReactionIntensityUnits']['KilojoulesPerSquareMeterPerSecond'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerSecond();

    Module['HeatSourceAndReactionIntensityUnits']['KilojoulesPerSquareMeterPerMinute'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerMinute();

    Module['HeatSourceAndReactionIntensityUnits']['KilowattsPerSquareMeter'] = _emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilowattsPerSquareMeter();

    

    // FirelineIntensityUnits_FirelineIntensityUnitsEnum

    Module['FirelineIntensityUnits']['BtusPerFootPerSecond'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerSecond();

    Module['FirelineIntensityUnits']['BtusPerFootPerMinute'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerMinute();

    Module['FirelineIntensityUnits']['KilojoulesPerMeterPerSecond'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerSecond();

    Module['FirelineIntensityUnits']['KilojoulesPerMeterPerMinute'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerMinute();

    Module['FirelineIntensityUnits']['KilowattsPerMeter'] = _emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilowattsPerMeter();

    

    // TemperatureUnits_TemperatureUnitsEnum

    Module['TemperatureUnits']['Fahrenheit'] = _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Fahrenheit();

    Module['TemperatureUnits']['Celsius'] = _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Celsius();

    Module['TemperatureUnits']['Kelvin'] = _emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Kelvin();

    

    // TimeUnits_TimeUnitsEnum

    Module['TimeUnits']['Minutes'] = _emscripten_enum_TimeUnits_TimeUnitsEnum_Minutes();

    Module['TimeUnits']['Seconds'] = _emscripten_enum_TimeUnits_TimeUnitsEnum_Seconds();

    Module['TimeUnits']['Hours'] = _emscripten_enum_TimeUnits_TimeUnitsEnum_Hours();

    

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

    

    // ContainMode

    Module['Default'] = _emscripten_enum_ContainMode_Default();

    Module['ComputeWithOptimalResource'] = _emscripten_enum_ContainMode_ComputeWithOptimalResource();

    

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

    

    // WindUpslopeAlignmentMode

    Module['NotAligned'] = _emscripten_enum_WindUpslopeAlignmentMode_NotAligned();

    Module['Aligned'] = _emscripten_enum_WindUpslopeAlignmentMode_Aligned();

    

    // SurfaceRunInDirectionOf

    Module['MaxSpread'] = _emscripten_enum_SurfaceRunInDirectionOf_MaxSpread();

    Module['DirectionOfInterest'] = _emscripten_enum_SurfaceRunInDirectionOf_DirectionOfInterest();

    Module['HeadingBackingFlanking'] = _emscripten_enum_SurfaceRunInDirectionOf_HeadingBackingFlanking();

    

    // FireType_FireTypeEnum

    Module['Surface'] = _emscripten_enum_FireType_FireTypeEnum_Surface();

    Module['Torching'] = _emscripten_enum_FireType_FireTypeEnum_Torching();

    Module['ConditionalCrownFire'] = _emscripten_enum_FireType_FireTypeEnum_ConditionalCrownFire();

    Module['Crowning'] = _emscripten_enum_FireType_FireTypeEnum_Crowning();

    

    // BeetleDamage

    Module['not_set'] = _emscripten_enum_BeetleDamage_not_set();

    Module['no'] = _emscripten_enum_BeetleDamage_no();

    Module['yes'] = _emscripten_enum_BeetleDamage_yes();

    

    // CrownFireCalculationMethod

    Module['rothermel'] = _emscripten_enum_CrownFireCalculationMethod_rothermel();

    Module['scott_and_reinhardt'] = _emscripten_enum_CrownFireCalculationMethod_scott_and_reinhardt();

    

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

    

    // GACC

    Module['NotSet'] = _emscripten_enum_GACC_NotSet();

    Module['Alaska'] = _emscripten_enum_GACC_Alaska();

    Module['California'] = _emscripten_enum_GACC_California();

    Module['EasternArea'] = _emscripten_enum_GACC_EasternArea();

    Module['GreatBasin'] = _emscripten_enum_GACC_GreatBasin();

    Module['NorthernRockies'] = _emscripten_enum_GACC_NorthernRockies();

    Module['Northwest'] = _emscripten_enum_GACC_Northwest();

    Module['RockeyMountain'] = _emscripten_enum_GACC_RockeyMountain();

    Module['SouthernArea'] = _emscripten_enum_GACC_SouthernArea();

    Module['Southwest'] = _emscripten_enum_GACC_Southwest();

    

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

    

    // FDFMToolAspectIndex_AspectIndexEnum

    Module['NORTH'] = _emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_NORTH();

    Module['EAST'] = _emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_EAST();

    Module['SOUTH'] = _emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_SOUTH();

    Module['WEST'] = _emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_WEST();

    

    // FDFMToolDryBulbIndex_DryBulbIndexEnum

    Module['TEN_TO_TWENTY_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_TEN_TO_TWENTY_NINE_DEGREES_F();

    Module['THRITY_TO_FOURTY_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_THRITY_TO_FOURTY_NINE_DEGREES_F();

    Module['FIFTY_TO_SIXTY_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_FIFTY_TO_SIXTY_NINE_DEGREES_F();

    Module['SEVENTY_TO_EIGHTY_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_SEVENTY_TO_EIGHTY_NINE_DEGREES_F();

    Module['NINETY_TO_ONE_HUNDRED_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_NINETY_TO_ONE_HUNDRED_NINE_DEGREES_F();

    Module['GREATER_THAN_ONE_HUNDRED_NINE_DEGREES_F'] = _emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_GREATER_THAN_ONE_HUNDRED_NINE_DEGREES_F();

    

    // FDFMToolElevationIndex_ElevationIndexEnum

    Module['BELOW_1000_TO_2000_FT'] = _emscripten_enum_FDFMToolElevationIndex_ElevationIndexEnum_BELOW_1000_TO_2000_FT();

    Module['LEVEL_WITHIN_1000_FT'] = _emscripten_enum_FDFMToolElevationIndex_ElevationIndexEnum_LEVEL_WITHIN_1000_FT();

    Module['ABOVE_1000_TO_2000_FT'] = _emscripten_enum_FDFMToolElevationIndex_ElevationIndexEnum_ABOVE_1000_TO_2000_FT();

    

    // FDFMToolMonthIndex_MonthIndexEnum

    Module['MAY_JUNE_JULY'] = _emscripten_enum_FDFMToolMonthIndex_MonthIndexEnum_MAY_JUNE_JULY();

    Module['FEB_MAR_APR_AUG_SEP_OCT'] = _emscripten_enum_FDFMToolMonthIndex_MonthIndexEnum_FEB_MAR_APR_AUG_SEP_OCT();

    Module['NOV_DEC_JAN'] = _emscripten_enum_FDFMToolMonthIndex_MonthIndexEnum_NOV_DEC_JAN();

    

    // FDFMToolRHIndex_RHIndexEnum

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

    

    // FDFMToolShadingIndex_ShadingIndexEnum

    Module['EXPOSED'] = _emscripten_enum_FDFMToolShadingIndex_ShadingIndexEnum_EXPOSED();

    Module['SHADED'] = _emscripten_enum_FDFMToolShadingIndex_ShadingIndexEnum_SHADED();

    

    // FDFMToolSlopeIndex_SlopeIndexEnum

    Module['ZERO_TO_THIRTY_PERCENT'] = _emscripten_enum_FDFMToolSlopeIndex_SlopeIndexEnum_ZERO_TO_THIRTY_PERCENT();

    Module['GREATER_THAN_OR_EQUAL_TO_THIRTY_ONE_PERCENT'] = _emscripten_enum_FDFMToolSlopeIndex_SlopeIndexEnum_GREATER_THAN_OR_EQUAL_TO_THIRTY_ONE_PERCENT();

    

    // FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum

    Module['EIGHT_HUNDRED_HOURS_TO_NINE_HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_EIGHT_HUNDRED_HOURS_TO_NINE_HUNDRED_FIFTY_NINE();

    Module['TEN_HUNDRED_HOURS_TO_ELEVEN__HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_TEN_HUNDRED_HOURS_TO_ELEVEN__HUNDRED_FIFTY_NINE();

    Module['TWELVE_HUNDRED_HOURS_TO_THIRTEEN_HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_TWELVE_HUNDRED_HOURS_TO_THIRTEEN_HUNDRED_FIFTY_NINE();

    Module['FOURTEEN_HUNDRED_HOURS_TO_FIFTEEN_HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_FOURTEEN_HUNDRED_HOURS_TO_FIFTEEN_HUNDRED_FIFTY_NINE();

    Module['SIXTEEN_HUNDRED_HOURS_TO_SIXTEEN_HUNDRED_FIFTY_NINE'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_SIXTEEN_HUNDRED_HOURS_TO_SIXTEEN_HUNDRED_FIFTY_NINE();

    Module['EIGHTTEEN_HUNDRED_HOURS_TO_SUNSET'] = _emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_EIGHTTEEN_HUNDRED_HOURS_TO_SUNSET();

    

    // RepresentativeFraction_RepresentativeFractionEnum

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

    

    // HorizontalDistanceIndex_HorizontalDistanceIndexEnum

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
