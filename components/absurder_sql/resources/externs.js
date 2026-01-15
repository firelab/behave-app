var sqlite = {

  "init_logger": function() {},
  "initSync" : function() {},

  "Database": {
    "newDatabase": function() {},
    "getAllDatabases": function() {},
    "deleteDatabase": function() {},

    "prototype": {
      "allowNonLeaderWrites": function() {},
      "free": function() {},
      "name": function() {},
      "execute": function() {},
      "executeWithParams": function() {},
      "close": function() {},
      "forceCloseConnection": function() {},
      "sync": function() {},
      "exportToFile": function() {},
      "testLock": function() {},
      "importFromFile": function() {},
      "waitForLeadership": function() {},
      "requestLeadership": function() {},
      "getLeaderInfo": function() {},
      "queueWrite": function() {},
      "queueWriteWithTimeout": function() {},
      "isLeader": function() {},
      "is_leader": function() {},
      "onDataChange": function() {},
      "enableOptimisticUpdates": function() {},
      "isOptimisticMode": function() {},
      "trackOptimisticWrite": function() {},
      "getPendingWritesCount": function() {},
      "clearOptimisticWrites": function() {},
      "enableCoordinationMetrics": function() {},
      "isCoordinationMetricsEnabled": function() {},
      "recordLeadershipChange": function() {},
      "recordNotificationLatency": function() {},
      "recordWriteConflict": function() {},
      "recordFollowerRefresh": function() {},
      "getCoordinationMetrics": function() {},
      "resetCoordinationMetrics": function() {},
    },
  },

  "WasmColumnValue": {
    "createNull": function() {},
    "createInteger": function() {},
    "createReal": function() {},
    "createText": function() {},
    "createBlob": function() {},
    "createBigInt": function() {},
    "createDate": function() {},
    "fromJsValue": function() {},
    "null": function() {},
    "integer": function() {},
    "real": function() {},
    "text": function() {},
    "blob": function() {},
    "big_int": function() {},
    "date": function() {},

    "prototype": {
      "free": function() {},
      "__destroy_into_raw": function() {},
    }
  }
}

// Persistent Sorted Set externs
var PersistentSortedSet = function() {};
PersistentSortedSet.empty = function() {};
PersistentSortedSet.withComparator = function() {};
PersistentSortedSet.withComparatorAndStorage = function() {};
PersistentSortedSet.from = function() {};
PersistentSortedSet.EARLY_EXIT = [];
PersistentSortedSet.UNCHANGED = [];
PersistentSortedSet.prototype = {
  "_cmp": {},
  "_storage": {},
  "_settings": {},
  "_address": {},
  "_root": {},
  "_count": 0,
  "_version": 0,
  "_hash": 0,
  "root": function() {},
  "editable": function() {},
  "comparator": function() {},
  "count": function() {},
  "isEmpty": function() {},
  "alterCount": function() {},
  "contains": function() {},
  "has": function() {},
  "get": function() {},
  "conj": function() {},
  "disj": function() {},
  "slice": function() {},
  "rslice": function() {},
  "seq": function() {},
  "rseq": function() {},
  "toArray": function() {},
  "forEach": function() {},
  "map": function() {},
  "filter": function() {},
  "reduce": function() {},
  "asTransient": function() {},
  "persistent": function() {},
  "store": function() {},
  "walkAddresses": function() {},
  "str": function() {},
  "toString": function() {},
  "hashCode": function() {},
  "equals": function() {}
};

var Settings = function() {};
Settings.prototype = {
  "_branchingFactor": 0,
  "_refType": "",
  "_edit": {},
  "minBranchingFactor": function() {},
  "branchingFactor": function() {},
  "expandLen": function() {},
  "refType": function() {},
  "editable": function() {},
  "editableSettings": function() {},
  "persistent": function() {},
  "makeReference": function() {},
  "readReference": function() {}
};

var RefType = {
  "STRONG": "",
  "SOFT": "",
  "WEAK": ""
};

var ANode = function() {};
ANode.newLen = function() {};
ANode.prototype = {
  "_len": 0,
  "_keys": [],
  "_settings": {},
  "len": function() {},
  "minKey": function() {},
  "maxKey": function() {},
  "keys": function() {},
  "search": function() {},
  "searchFirst": function() {},
  "searchLast": function() {},
  "editable": function() {},
  "level": function() {},
  "count": function() {},
  "contains": function() {},
  "add": function() {},
  "remove": function() {},
  "walkAddresses": function() {},
  "store": function() {},
  "str": function() {},
  "toString": function() {}
};

var Leaf = function() {};
Leaf.EARLY_EXIT = [];
Leaf.UNCHANGED = [];
Leaf.prototype = {
  "_len": 0,
  "_keys": [],
  "_settings": {},
  "level": function() {},
  "count": function() {},
  "contains": function() {},
  "add": function() {},
  "remove": function() {},
  "walkAddresses": function() {},
  "store": function() {},
  "str": function() {},
  "toString": function() {}
};

var Branch = function() {};
Branch.EARLY_EXIT = [];
Branch.UNCHANGED = [];
Branch.prototype = {
  "_len": 0,
  "_level": 0,
  "_keys": [],
  "_addresses": [],
  "_children": [],
  "_settings": {},
  "level": function() {},
  "count": function() {},
  "child": function() {},
  "address": function() {},
  "contains": function() {},
  "add": function() {},
  "remove": function() {},
  "walkAddresses": function() {},
  "store": function() {},
  "str": function() {},
  "toString": function() {}
};

var Seq = function() {};
Seq.prototype = {
  "_val": {},
  "_set": {},
  "_prev": {},
  "_node": {},
  "_idx": 0,
  "_to": {},
  "_cmp": {},
  "_forward": true,
  "_version": 0,
  "first": function() {},
  "next": function() {},
  "over": function() {},
  "advance": function() {},
  "seek": function() {},
  "toArray": function() {},
  "reduce": function() {}
};

var Chunk = function() {};
Chunk.prototype = {
  "copyAll": function() {},
  "copyOne": function() {}
};

var Stitch = function() {};
Stitch.prototype = {
  "_target": [],
  "_targetIdx": 0,
  "copyAll": function() {},
  "copyOne": function() {}
};

var ArrayUtil = {
  "copy": function() {},
  "fill": function() {}
};

var NodeFactory = function() {};
NodeFactory.prototype = {
  "createLeaf": function() {},
  "createBranch": function() {}
};

var IStorage = function() {};
IStorage.prototype = {
  "restore": function() {},
  "store": function() {}
};

function defaultComparator() {}
