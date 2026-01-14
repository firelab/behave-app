import { ArrayUtil } from './ArrayUtil.js';

/**
 * Default comparator function
 * @param {*} a - First value
 * @param {*} b - Second value
 * @returns {number} -1 if a < b, 0 if a === b, 1 if a > b
 */
export function defaultComparator(a, b) {
  if (a < b) return -1;
  if (a > b) return 1;
  return 0;
}

/**
 * Abstract base class for nodes in the B-tree
 * @abstract
 */
export class ANode {
  /**
   * @param {number} len - Number of keys in this node
   * @param {Array} keys - Array of keys
   * @param {Settings} settings - Configuration settings
   */
  constructor(len, keys, settings) {
    this._len = len;
    this._keys = keys;
    this._settings = settings;
  }

  /**
   * @returns {number} Number of keys in this node
   */
  len() {
    return this._len;
  }

  /**
   * @returns {*} Minimum key in this node
   */
  minKey() {
    return this._keys[0];
  }

  /**
   * @returns {*} Maximum key in this node
   */
  maxKey() {
    return this._keys[this._len - 1];
  }

  /**
   * Get keys as array
   * @returns {Array} Array of keys
   */
  keys() {
    if (this._keys.length === this._len) {
      return Array.from(this._keys);
    } else {
      return this._keys.slice(0, this._len);
    }
  }

  /**
   * Binary search for key
   * @param {*} key - Key to search for
   * @param {Function} cmp - Comparator function
   * @returns {number} Index if found (>= 0), or -(insertion point) - 1 if not found
   */
  search(key, cmp) {
    let l = 0;
    let r = this._len - 1;

    while (l <= r) {
      const m = (l + r) >>> 1;
      const c = cmp(this._keys[m], key);

      if (c < 0) {
        l = m + 1;
      } else if (c > 0) {
        r = m - 1;
      } else {
        return m;
      }
    }
    return -(l + 1);
  }

  /**
   * Find first index where key >= search key
   * @param {*} key - Key to search for
   * @param {Function} cmp - Comparator function
   * @returns {number} Index of first key >= search key
   */
  searchFirst(key, cmp) {
    let l = 0;
    let r = this._len - 1;
    let found = -1;

    while (l <= r) {
      const m = (l + r) >>> 1;
      const c = cmp(this._keys[m], key);

      if (c < 0) {
        l = m + 1;
      } else if (c > 0) {
        found = m;
        r = m - 1;
      } else {
        return m;
      }
    }
    return found >= 0 ? found : -(l + 1);
  }

  /**
   * Find last index where key <= search key
   * @param {*} key - Key to search for
   * @param {Function} cmp - Comparator function
   * @returns {number} Index of last key <= search key
   */
  searchLast(key, cmp) {
    let l = 0;
    let r = this._len - 1;
    let found = -1;

    while (l <= r) {
      const m = (l + r) >>> 1;
      const c = cmp(this._keys[m], key);

      if (c < 0) {
        found = m;
        l = m + 1;
      } else if (c > 0) {
        r = m - 1;
      } else {
        return m;
      }
    }
    return found;
  }

  /**
   * Whether this node is editable (part of a transient collection)
   * @returns {boolean} True if editable
   */
  editable() {
    return this._settings.editable();
  }

  /**
   * Calculate new length with expansion
   * @param {number} len - Current length
   * @param {Settings} settings - Settings
   * @returns {number} New length
   */
  static newLen(len, settings) {
    const expandLen = settings.expandLen();
    return len + expandLen - (len % expandLen);
  }

  /**
   * Safe length accessor for nullable nodes
   * @param {ANode|null} node - Node or null
   * @returns {number} Length or 0 if null
   */
  static safeLen(node) {
    return node == null ? 0 : node._len;
  }

  /**
   * Restore node from keys/addresses
   * @param {number} level - Node level (0 for leaf)
   * @param {Array} keys - Keys array
   * @param {Array} addresses - Addresses array (for branch nodes)
   * @param {Settings} settings - Settings
   * @returns {ANode} Restored node
   */
  static restore(level, keys, addresses, settings) {
    // Import dynamically to avoid circular dependency
    const { Leaf } = require('./Leaf.js');
    const { Branch } = require('./Branch.js');

    if (level === 0) {
      return new Leaf(keys.length, keys, settings);
    } else {
      return new Branch(level, keys.length, keys, addresses, null, settings);
    }
  }

  // Abstract methods to be implemented by subclasses

  /**
   * Count total elements in subtree
   * @abstract
   * @param {IStorage} storage - Storage backend
   * @returns {number} Total count
   */
  count(storage) {
    throw new Error('count() must be implemented');
  }

  /**
   * Level of this node (0 for leaf)
   * @abstract
   * @returns {number} Node level
   */
  level() {
    throw new Error('level() must be implemented');
  }

  /**
   * Check if key is in subtree
   * @abstract
   * @param {IStorage} storage - Storage backend
   * @param {*} key - Key to search for
   * @param {Function} cmp - Comparator function
   * @returns {boolean} True if key exists
   */
  contains(storage, key, cmp) {
    throw new Error('contains() must be implemented');
  }

  /**
   * Add key to subtree
   * @abstract
   * @param {IStorage} storage - Storage backend
   * @param {*} key - Key to add
   * @param {Function} cmp - Comparator function
   * @param {Settings} settings - Settings
   * @returns {Array<ANode>} Result nodes (1 or 2 after possible split)
   */
  add(storage, key, cmp, settings) {
    throw new Error('add() must be implemented');
  }

  /**
   * Remove key from subtree
   * @abstract
   * @param {IStorage} storage - Storage backend
   * @param {*} key - Key to remove
   * @param {ANode} left - Left sibling
   * @param {ANode} right - Right sibling
   * @param {Function} cmp - Comparator function
   * @param {Settings} settings - Settings
   * @returns {Array<ANode>} Result nodes [left, center, right]
   */
  remove(storage, key, left, right, cmp, settings) {
    throw new Error('remove() must be implemented');
  }

  /**
   * Walk all addresses in subtree
   * @abstract
   * @param {IStorage} storage - Storage backend
   * @param {Function} onAddress - Callback for each address
   */
  walkAddresses(storage, onAddress) {
    throw new Error('walkAddresses() must be implemented');
  }

  /**
   * Store this node and return address
   * @abstract
   * @param {IStorage} storage - Storage backend
   * @returns {*} Address where stored
   */
  store(storage) {
    throw new Error('store() must be implemented');
  }

  /**
   * Debug string representation
   * @abstract
   * @param {IStorage} storage - Storage backend
   * @param {number} lvl - Indentation level
   * @returns {string} String representation
   */
  str(storage, lvl) {
    throw new Error('str() must be implemented');
  }

  /**
   * String representation
   * @abstract
   * @param {StringBuilder} sb - String builder
   * @param {*} address - Node address
   * @param {string} indent - Indentation string
   */
  toString(sb, address, indent) {
    throw new Error('toString() must be implemented');
  }
}
