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
    if (this._node instanceof Branch) {
      return this._node.child(this._set._storage, this._idx);
    }
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
    const d = this._cmp(this.first(), this._keyTo);
    return this._asc ? d > 0 : d < 0;
  }

  /**
   * Advance iterator to next position
   * @returns {boolean} True if advanced successfully, false if at end
   */
  advance() {
    this.checkVersion();

    if (this._asc) {
      if (this._idx < this._node._len - 1) {
        this._idx++;
        return !this.over();
      } else if (this._parent != null) {
        const parent = this._parent;
        this._parent = parent.next();
        if (this._parent != null) {
          this._node = this._parent.child();
          this._idx = 0;
          return !this.over();
        }
      }
    } else { // !_asc
      if (this._idx > 0) {
        this._idx--;
        return !this.over();
      } else if (this._parent != null) {
        const parent = _parent;
        this._parent = parent.next();
        if (_parent != null) {
          this._node = this._parent.child();
          this._idx = this._node._len - 1;
          return !this.over();
        }
      }
    }
    return false;
  }

  /**
   * Get next sequence
   * @returns {Seq|null} Next sequence or null if at end
   */
  next() {
    const seq = this.clone();

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
  seek(to, cmp = null) {
    if (to == null) throw new Error("seek can't be called with a nil key!");

    cmp = cmp || this._cmp;
    let seq = this._parent;
    let node = this._node;

    if (this._asc) {

      while (node != null && cmp(node.maxKey(), to) < 0) {
        if (seq == null) {
          return null;
        } else {
          node = seq._node;
          seq = seq._parent;
        }
      }

      while (true) {
        let idx = node.searchFirst(to, cmp);
        if (idx < 0)
          idx = -idx - 1;
        if (idx == node._len)
          return null;
        if (node instanceof Branch) {
          seq = new Seq(null, this._set, seq, node, idx, null, null, true, _version);
          node = seq.child();
        } else { // Leaf
          seq = new Seq(null, this._set, seq, node, idx, this._keyTo, cmp, true, _version);
          return seq.over() ? null : seq;
        }
      }

    } else {

      // NOTE: We can't shortcircuit here as we don't know the minKey. Might go up one level too high.
      while (cmp.compare(to, node.minKey()) < 0 && seq != null){
        node = seq._node;
        seq = seq._parent;
      }

      while (true) {
        if (node instanceof Branch) {
          let idx = node.searchLast(to, cmp) + 1;
          if (idx == node._len) --idx; // last or beyond, clamp to last
          seq = new Seq(null, this._set, seq, node, idx, null, null, false, _version);
          node = seq.child();
        } else { // Leaf
          let idx = node.searchLast(to, cmp);
          if (idx == -1) { // not in this, so definitely in prev
            seq = new Seq(null, this._set, seq, node, 0, this._keyTo, cmp, false, _version);
            return seq.advance() ? seq : null;
          } else { // exact match
            seq = new Seq(null, this._set, seq, node, idx, this._keyTo, cmp, false, _version);
            return seq.over() ? null : seq;
          }
        }
      }
    }
  }

  clone() {
    return new Seq(
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
  }

  /**
   * Reduce over sequence
   * @param {Function} f - Reducer function
   * @param {*} start - Initial value
   * @returns {*} Final value
   */
  reduce(f, start = null) {
    this.checkVersion();

    let clone = this.clone()
    let ret = start || clone.first();
    while (clone.advance()) {
      ret = f(ret, clone.first());
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
