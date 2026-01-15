import { ANode, defaultComparator } from './ANode.js';
import { Leaf } from './Leaf.js';
import { Branch } from './Branch.js';
import { Seq } from './Seq.js';
import { Settings } from './Settings.js';

// Sentinel values
const EARLY_EXIT = [];
const UNCHANGED = [];

/**
 * A fast B-tree based persistent sorted set
 *
 * Features:
 * - Persistent (immutable) by default with transient support
 * - Custom comparators
 * - Fast iteration and slicing
 * - Optional storage backend for persistence
 */
export class PersistentSortedSet {
  /**
   * @param {Function} cmp - Comparator function (default: natural ordering)
   * @param {IStorage} storage - Optional storage backend
   * @param {Settings} settings - Configuration settings
   * @param {*} address - Optional stored address
   * @param {ANode} root - Root node
   * @param {number} count - Cached count (-1 if unknown)
   * @param {number} version - Version for transient tracking
   */
  constructor(cmp = null, storage = null, settings = null, address = null, root = null, count = 0, version = 0) {
    this._cmp = cmp || defaultComparator;
    this._storage = storage;
    this._settings = settings || new Settings();
    this._address = address;
    this._root = root;
    this._count = count;
    this._version = version;
    this._hash = 0;
  }


  /**
   * Create an sorted set with comparator and storage
   * @param {Function} cmp - Optional comparator function
   * @param {IStorage} storage - Optional storage backend
   * @param {Settings} settings - Configuration settings
   * @returns {PersistentSortedSet} Empty set
   */
  static withComparatorAndStorage(cmp, storage, settings = null) {
    let theSettings = settings || new Settings();
    return new PersistentSortedSet(cmp, null, theSettings, null, new Leaf(0, theSettings));
  }

  /**
   * Create an sorted set with comparator
   * @param {Function} cmp - Optional comparator function
   * @returns {PersistentSortedSet} Empty set
   */
  static withComparator(cmp) {
    return this.withComparatorAndStorage(cmp, null, new Settings())
  }

  /**
   * Create an empty sorted set
   * @param {Function} cmp - Optional comparator function
   * @returns {PersistentSortedSet} Empty set
   */
  static empty(cmp = null) {
    return this.withComparator(cmp);
  }

  /**
   * Create a sorted set from an array
   * @param {Array} arr - Array of elements
   * @param {Function} cmp - Optional comparator function
   * @returns {PersistentSortedSet} New sorted set
   */
  static from(arr, cmp = null, storage = null, settings = null) {
    let set = this.withComparatorAndStorage(cmp, storage, settings);
    for (const item of arr) {
      set = set.conj(item);
    }
    return set;
  }

  /**
   * Get root node
   * @returns {ANode} Root node
   */
  root() {
    let root = this._settings.readReference(this._root);
    if (root == null && this._address != null) {
      root = this._storage.restore(this._address);
      this._root = this._settings.makeReference(root);
    }
    return root;
  }

  /**
   * Check if editable (transient)
   * @returns {boolean} True if editable
   */
  editable() {
    return this._settings.editable();
  }

  /**
   * Get comparator function
   * @returns {Function} Comparator
   */
  comparator() {
    return this._cmp;
  }

  /**
   * Get number of elements
   * @returns {number} Count
   */
  count() {
    if (this._count < 0) {
      this._count = this.root().count(this._storage);
    }
    return this._count;
  }

  /**
   * Check if empty
   * @returns {boolean} True if empty
   */
  isEmpty() {
    return this.count() === 0;
  }

  /**
   * Alter count by delta
   * @private
   * @param {number} delta - Change in count
   * @returns {number} New count
   */
  alterCount(delta) {
    return this._count < 0 ? this._count : this._count + delta;
  }

  /**
   * Check if key exists
   * @param {*} key - Key to check
   * @returns {boolean} True if exists
   */
  contains(key) {
    return this.root().contains(this._storage, key, this._cmp);
  }

  /**
   * Check if key exists (alias)
   * @param {*} key - Key to check
   * @returns {boolean} True if exists
   */
  has(key) {
    return this.contains(key);
  }

  /**
   * Get key if exists
   * @param {*} key - Key to get
   * @param {*} notFound - Value to return if not found
   * @returns {*} Key if found, notFound otherwise
   */
  get(key, notFound = null) {
    return this.contains(key) ? key : notFound;
  }

  /**
   * Add key to set
   * @param {*} key - Key to add
   * @returns {PersistentSortedSet} New set with key added
   */
  conj(key) {
    const nodes = this.root().add(this._storage, key, this._cmp, this._settings);

    if (nodes === UNCHANGED) {
      return this;
    }

    if (this.editable()) {
      if (nodes.length === 1) {
        this._address = null;
        this._root = nodes[0];
      } else if (nodes.length === 2) {
        const keys = [nodes[0].maxKey(), nodes[1].maxKey()];
        this._address = null;
        this._root = new Branch(nodes[0].level() + 1, 2, keys, null, nodes, this._settings);
      }
      this._count = this.alterCount(1);
      this._version += 1;
      return this;
    }

    if (nodes.length === 1) {
      return new PersistentSortedSet(
        this._cmp,
        this._storage,
        this._settings,
        null,
        nodes[0],
        this.alterCount(1),
        this._version + 1
      );
    }

    const keys = [nodes[0].maxKey(), nodes[1].maxKey()];
    const newRoot = new Branch(nodes[0].level() + 1, 2, keys, null, nodes, this._settings);
    return new PersistentSortedSet(
      this._cmp,
      this._storage,
      this._settings,
      null,
      newRoot,
      this.alterCount(1),
      this._version + 1
    );
  }

  /**
   * Remove key from set
   * @param {*} key - Key to remove
   * @returns {PersistentSortedSet} New set with key removed
   */
  disj(key) {
    const nodes = this.root().remove(this._storage, key, null, null, this._cmp, this._settings);

    if (nodes === UNCHANGED) {
      return this;
    }

    if (nodes === EARLY_EXIT) {
      this._address = null;
      this._count = this.alterCount(-1);
      this._version += 1;
      return this;
    }

    let newRoot = nodes[1];

    if (this.editable()) {
      if (newRoot instanceof Branch && newRoot._len === 1) {
        newRoot = newRoot.child(this._storage, 0);
      }
      this._address = null;
      this._root = newRoot;
      this._count = this.alterCount(-1);
      this._version += 1;
      return this;
    }

    if (newRoot instanceof Branch && newRoot._len === 1) {
      newRoot = newRoot.child(this._storage, 0);
      return new PersistentSortedSet(
        this._cmp,
        this._storage,
        this._settings,
        null,
        newRoot,
        this.alterCount(-1),
        this._version + 1
      );
    }

    return new PersistentSortedSet(
      this._cmp,
      this._storage,
      this._settings,
      null,
      newRoot,
      this.alterCount(-1),
      this._version + 1
    );
  }

  /**
   * Create forward iterator
   * @param {*} from - Optional start key (inclusive)
   * @param {*} to - Optional end key (inclusive)
   * @returns {Seq|null} Sequence iterator or null if empty
   */
  slice(from = null, to = null) {
    let seq = null;
    let node = this.root();

    if (node.len() === 0) {
      return null;
    }

    if (from == null) {
      // Start from beginning
      while (true) {
        if (node instanceof Branch) {
          seq = new Seq(null, this, seq, node, 0, null, null, true, this._version);
          node = node.child(this._storage, 0);
        } else {
          seq = new Seq(null, this, seq, node, 0, to, this._cmp, true, this._version);
          return seq.over() ? null : seq;
        }
      }
    }

    // Seek to 'from'
    while (true) {
      let idx = node.searchFirst(from, this._cmp);
      if (idx < 0) idx = -(idx + 1);
      if (idx === node._len) return null;

      if (node instanceof Branch) {
        seq = new Seq(null, this, seq, node, idx, null, null, true, this._version);
        node = node.child(this._storage, idx);
      } else {
        seq = new Seq(null, this, seq, node, idx, to, this._cmp, true, this._version);
        return seq.over() ? null : seq;
      }
    }
  }

  /**
   * Create reverse iterator
   * @param {*} from - Optional start key (inclusive)
   * @param {*} to - Optional end key (inclusive)
   * @returns {Seq|null} Sequence iterator or null if empty
   */
  rslice(from = null, to = null) {
    let seq = null;
    let node = this.root();

    if (node.len() === 0) {
      return null;
    }

    if (from == null) {
      // Start from end
      while (true) {
        const idx = node._len - 1;
        if (node instanceof Branch) {
          seq = new Seq(null, this, seq, node, idx, null, null, false, this._version);
          node = node.child(this._storage, idx);
        } else {
          seq = new Seq(null, this, seq, node, idx, to, this._cmp, false, this._version);
          return seq.over() ? null : seq;
        }
      }
    }

    // Seek to 'from'
    while (true) {
      if (node instanceof Branch) {
        let idx = node.searchLast(from, this._cmp) + 1;
        if (idx === node._len) idx--;
        seq = new Seq(null, this, seq, node, idx, null, null, false, this._version);
        node = node.child(this._storage, idx);
      } else {
        const idx = node.searchLast(from, this._cmp);
        if (idx === -1) {
          seq = new Seq(null, this, seq, node, 0, to, this._cmp, false, this._version);
          return seq.advance() ? seq : null;
        } else {
          seq = new Seq(null, this, seq, node, idx, to, this._cmp, false, this._version);
          return seq.over() ? null : seq;
        }
      }
    }
  }

  /**
   * Create forward iterator (alias)
   * @returns {Seq|null} Sequence iterator
   */
  seq() {
    return this.slice();
  }

  /**
   * Create reverse iterator (alias)
   * @returns {Seq|null} Reverse sequence iterator
   */
  rseq() {
    return this.rslice();
  }

  /**
   * Convert to array
   * @returns {Array} Array of all elements
   */
  toArray() {
    const arr = [];
    const seq = this.seq();
    if (seq != null) {
      return seq.toArray();
    }
    return arr;
  }

  /**
   * Create iterator
   * @returns {Iterator} JavaScript iterator
   */
  [Symbol.iterator]() {
    const seq = this.seq();
    if (seq == null) {
      return {
        next() {
          return { done: true };
        }
      };
    }
    return seq[Symbol.iterator]();
  }

  /**
   * For each element
   * @param {Function} fn - Function to call for each element
   */
  forEach(fn) {
    let i = 0;
    for (const item of this) {
      fn(item, i++, this);
    }
  }

  /**
   * Map over elements
   * @param {Function} fn - Mapping function
   * @returns {Array} Mapped array
   */
  map(fn) {
    const result = [];
    let i = 0;
    for (const item of this) {
      result.push(fn(item, i++, this));
    }
    return result;
  }

  /**
   * Filter elements
   * @param {Function} pred - Predicate function
   * @returns {PersistentSortedSet} Filtered set
   */
  filter(pred) {
    let result = PersistentSortedSet.empty(this._cmp);
    let i = 0;
    for (const item of this) {
      if (pred(item, i++, this)) {
        result = result.conj(item);
      }
    }
    return result;
  }

  /**
   * Reduce over elements
   * @param {Function} fn - Reducer function
   * @param {*} init - Initial value
   * @returns {*} Reduced value
   */
  reduce(fn, init) {
    const seq = this.seq();
    if (seq == null) {
      return init;
    }
    return seq.reduce(fn, init);
  }

  /**
   * Convert to transient (editable) set
   * @returns {PersistentSortedSet} Transient set
   */
  asTransient() {
    if (this.editable()) {
      throw new Error('Already transient');
    }
    return new PersistentSortedSet(
      this._cmp,
      this._storage,
      this._settings.editableSettings(true),
      this._address,
      this._root,
      this._count,
      this._version
    );
  }

  /**
   * Convert transient set back to persistent
   * @returns {PersistentSortedSet} Persistent set
   */
  persistent() {
    if (!this.editable()) {
      throw new Error('Already persistent');
    }
    this._settings.persistent();
    return this;
  }

  /**
   * @param {IStorage} storage - Optional storage (uses existing if not provided)
   * @returns {*} Address where stored
   */
  store(storage = null) {
    if (storage != null) {
      this._storage = storage;
    }

    if (this._storage == null) {
      throw new Error('No storage backend provided');
    }

    if (this._address == null) {
      const root = this._settings.readReference(this._root);
      this._address = root.store(this._storage);
      this._root = this._settings.makeReference(root);
    }

    return this._address;
  }

  /**
   * Walk all addresses in tree
   * @param {Function} onAddress - Callback for each address
   */
  walkAddresses(onAddress) {
    if (this._address != null) {
      if (!onAddress(this._address)) {
        return;
      }
    }
    this.root().walkAddresses(this._storage, onAddress);
  }

  /**
   * Debug string representation
   * @returns {string} String representation
   */
  str() {
    return this.root().str(this._storage, 0);
  }

  /**
   * String representation
   * @returns {string} String representation
   */
  toString() {
    return '#{' + this.toArray().join(' ') + '}';
  }

  /**
   * Hash code
   * @returns {number} Hash code
   */
  hashCode() {
    if (this._hash === 0) {
      let hash = 0;
      for (const item of this) {
        hash += (typeof item === 'object' && item !== null && typeof item.hashCode === 'function')
          ? item.hashCode()
          : JSON.stringify(item).split('').reduce((a, b) => {
              a = ((a << 5) - a) + b.charCodeAt(0);
              return a & a;
            }, 0);
      }
      this._hash = hash;
    }
    return this._hash;
  }

  /**
   * Equality check
   * @param {*} other - Other value
   * @returns {boolean} True if equal
   */
  equals(other) {
    if (this === other) return true;
    if (!(other instanceof PersistentSortedSet)) return false;
    if (this.count() !== other.count()) return false;

    const seq1 = this.seq();
    const seq2 = other.seq();

    let s1 = seq1;
    let s2 = seq2;

    while (s1 != null && s2 != null) {
      if (this._cmp(s1.first(), s2.first()) !== 0) {
        return false;
      }
      s1 = s1.next();
      s2 = s2.next();
    }

    return s1 == null && s2 == null;
  }
}

// Export sentinels
PersistentSortedSet.EARLY_EXIT = EARLY_EXIT;
PersistentSortedSet.UNCHANGED = UNCHANGED;
