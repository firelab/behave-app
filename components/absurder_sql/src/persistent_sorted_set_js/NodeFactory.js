import { Leaf } from './Leaf.js';
import { Branch } from './Branch.js';

/**
 * Factory for creating nodes (avoids circular dependencies)
 */
export class NodeFactory {
  /**
   * Restore node from keys/addresses
   * @param {number} level - Node level (0 for leaf)
   * @param {Array} keys - Keys array
   * @param {Array} addresses - Addresses array (for branch nodes)
   * @param {Settings} settings - Settings
   * @returns {ANode} Restored node
   */
  static restore(level, keys, addresses, settings) {
    if (level === 0) {
      return new Leaf(keys.length, keys, settings);
    } else {
      return new Branch(level, keys.length, keys, addresses, null, settings);
    }
  }
}
