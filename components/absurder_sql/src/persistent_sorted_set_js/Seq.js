import { Chunk } from './Chunk.js';
import { Branch } from './Branch.js';
import { Leaf } from './Leaf.js';

/**
 * Sequence iterator for PersistentSortedSet
 * Supports forward and reverse iteration with range limits
 */
export class Seq {
  /**
   * @param {Seq|null} prev - Previous sequence in parent stack
   * @param {PersistentSortedSet} set - The set being iterated
   * @param {Seq|null} parent - Parent sequence (for branch nodes)
   * @param {ANode} node - Current node
   * @param {number} idx - Current index in node
   * @param {*} keyTo - Upper/lower bound key (inclusive)
   * @param {Function} cmp - Comparator function
   * @param {boolean} asc - Ascending direction
   * @param {number} version - Version for concurrent modification check
   */
  constructor(prev, set, parent, node, idx, keyTo, cmp, asc, version) {
    this._prev = prev;
    this._set = set;
    this._parent = parent;
    this._node = node;
    this._idx = idx;
    this._keyTo = keyTo;
    this._cmp = cmp;
    this._asc = asc;
    this._version = version;
  }

  checkVersion() {
    if (this._version !== this._set._version) {
      throw new Error('Tovarisch, you are iterating and mutating a transient set at the same time!');
    }
  }

  /**
   * Get current child node (for branch iteration)
   * @returns {ANode} Child node
   */
  child() {
    return this._parent.node.child(this._set._storage, this._idx);
  }

  /**
   * Get current key
   * @returns {*} Current key
   */
  first() {
    this.checkVersion();
    return this._node._keys[this._idx];
  }

  /**
   * Check if iterator is past the end
   * @returns {boolean} True if past the end
   */
  over() {
    if (this._keyTo == null) {
      return false;
    }
    const c = this._cmp(this._node._keys[this._idx], this._keyTo);
    return this._asc ? c > 0 : c < 0;
  }

  /**
   * Advance iterator to next position
   * @returns {boolean} True if advanced successfully, false if at end
   */
  advance() {
    this.checkVersion();

    if (this._node instanceof Leaf) {
      // Advance in leaf
      if (this._asc) {
        this._idx++;
        if (this._idx < this._node._len) {
          return !this.over();
        }
      } else {
        this._idx--;
        if (this._idx >= 0) {
          return !this.over();
        }
      }

      // Move up to parent
      if (this._parent == null) {
        return false;
      }

      const parent = this._parent;
      this._node = parent._node;
      this._idx = parent._idx;
      this._parent = parent._parent;

      return this.advance();
    } else {
      // Branch node - descend to next child
      if (this._asc) {
        this._idx++;
      } else {
        this._idx--;
      }

      if (this._asc && this._idx >= this._node._len) {
        // Past end of branch
        if (this._parent == null) {
          return false;
        }

        const parent = this._parent;
        this._node = parent._node;
        this._idx = parent._idx;
        this._parent = parent._parent;

        return this.advance();
      }

      if (!this._asc && this._idx < 0) {
        // Before start of branch
        if (this._parent == null) {
          return false;
        }

        const parent = this._parent;
        this._node = parent._node;
        this._idx = parent._idx;
        this._parent = parent._parent;

        return this.advance();
      }

      // Descend to leaf
      let node = this.child();
      const parent = new Seq(null, this._set, this._parent, this._node, this._idx, null, null, this._asc, this._version);

      while (node instanceof Branch) {
        const idx = this._asc ? 0 : node._len - 1;
        const seq = new Seq(null, this._set, parent, node, idx, null, null, this._asc, this._version);
        node = seq.child();
        parent._node = seq._node;
        parent._idx = seq._idx;
        parent._parent = seq._parent;
      }

      this._node = node;
      this._idx = this._asc ? 0 : node._len - 1;
      this._parent = parent;

      return !this.over();
    }
  }

  /**
   * Get next sequence
   * @returns {Seq|null} Next sequence or null if at end
   */
  next() {
    const seq = new Seq(
      this._prev,
      this._set,
      this._parent,
      this._node,
      this._idx,
      this._keyTo,
      this._cmp,
      this._asc,
      this._version
    );

    if (seq.advance()) {
      return seq;
    }
    return null;
  }

  /**
   * Seek to a specific key
   * @param {*} to - Key to seek to
   * @param {Function} cmp - Comparator function
   * @returns {Seq|null} Sequence at key or null if not found
   */
  seek(to, cmp) {
    // Implementation would be similar to slice() in PersistentSortedSet
    // For simplicity, we'll iterate until we find it
    let seq = this;
    while (seq != null) {
      const c = cmp(seq.first(), to);
      if (c === 0) {
        return seq;
      }
      if (this._asc && c > 0) {
        return null;
      }
      if (!this._asc && c < 0) {
        return null;
      }
      seq = seq.next();
    }
    return null;
  }

  /**
   * Reduce over sequence
   * @param {Function} f - Reducer function
   * @param {*} start - Initial value
   * @returns {*} Final value
   */
  reduce(f, start) {
    let ret = start;
    let seq = this;
    while (seq != null) {
      ret = f(ret, seq.first());
      seq = seq.next();
    }
    return ret;
  }

  /**
   * Get chunked iterator
   * @returns {Chunk} Chunk for efficient iteration
   */
  chunkedSeq() {
    if (this._node instanceof Leaf) {
      return Chunk.fromSeq(this);
    }
    return null;
  }

  /**
   * Convert to array
   * @returns {Array} Array of all elements
   */
  toArray() {
    const arr = [];
    let seq = this;
    while (seq != null) {
      arr.push(seq.first());
      seq = seq.next();
    }
    return arr;
  }

  /**
   * Create iterator
   * @returns {Iterator} JavaScript iterator
   */
  [Symbol.iterator]() {
    let seq = this;
    return {
      next() {
        if (seq == null) {
          return { done: true };
        }
        const value = seq.first();
        seq = seq.next();
        return { value, done: false };
      }
    };
  }
}
