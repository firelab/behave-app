/**
 * Storage interface for persisting and restoring nodes
 *
 * Implement this interface to provide custom storage for the sorted set.
 * This allows integration with databases, files, or other storage systems.
 */
export class IStorage {
  /**
   * Given an address, reconstruct and (optionally) cache the node.
   * The set itself does not store strong references to nodes and
   * might request them by address during its operation many times.
   *
   * @param {*} address - Address to restore from
   * @returns {ANode} The restored node (Leaf or Branch)
   */
  restore(address) {
    throw new Error('restore() must be implemented');
  }

  /**
   * Tell the storage layer that an address was accessed.
   * Useful for implementing LRU cache in storage.
   *
   * @param {*} address - Address that was accessed
   */
  accessed(address) {
    // Optional: override to track accesses
  }

  /**
   * Store a node and return its address.
   * Will be called after all children of the node have been stored.
   *
   * For Leaf nodes: store keys
   * For Branch nodes: store level, keys, and addresses
   *
   * @param {ANode} node - Node to store
   * @returns {*} Address where node was stored, or null if not needed
   */
  store(node) {
    throw new Error('store() must be implemented');
  }
}
