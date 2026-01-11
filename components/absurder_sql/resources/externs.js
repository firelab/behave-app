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
