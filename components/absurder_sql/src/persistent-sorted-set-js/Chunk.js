/**
 * Chunked iteration for efficient traversal
 */
export class Chunk {
  /**
   * @param {PersistentSortedSet} set - The set being iterated
   * @param {Array} keys - Keys array from leaf node
   * @param {number} idx - Current index
   * @param {number} end - End index
   * @param {boolean} asc - Ascending direction
   * @param {number} version - Version for concurrent modification check
   */
  constructor(set, keys, idx, end, asc, version) {
    this._set = set;
    this._keys = keys;
    this._idx = idx;
    this._end = end;
    this._asc = asc;
    this._version = version;
  }

  /**
   * Create chunk from Seq
   * @param {Seq} seq - Sequence iterator
   * @returns {Chunk} New chunk
   */
  static fromSeq(seq) {
    const idx = seq._idx;
    const keys = seq._node._keys;
    const asc = seq._asc;
    let end;

    if (asc) {
      end = seq._node._len - 1;
      if (seq._keyTo != null) {
        while (end > idx && seq._cmp(keys[end], seq._keyTo) > 0) {
          end--;
        }
      }
    } else {
      end = 0;
      if (seq._keyTo != null) {
        while (end < idx && seq._cmp(keys[end], seq._keyTo) < 0) {
          end++;
        }
      }
    }

    return new Chunk(seq._set, keys, idx, end, asc, seq._version);
  }

  checkVersion() {
    if (this._version !== this._set._version) {
      throw new Error('Tovarisch, you are iterating and mutating a transient set at the same time!');
    }
  }

  dropFirst() {
    this.checkVersion();
    if (this._idx === this._end) {
      throw new Error('dropFirst of empty chunk');
    }
    return new Chunk(
      this._set,
      this._keys,
      this._asc ? this._idx + 1 : this._idx - 1,
      this._end,
      this._asc,
      this._version
    );
  }

  reduce(f, start) {
    this.checkVersion();
    let ret = f(start, this._keys[this._idx]);

    if (this._asc) {
      for (let x = this._idx + 1; x <= this._end; x++) {
        ret = f(ret, this._keys[x]);
      }
    } else {
      for (let x = this._idx - 1; x >= this._end; x--) {
        ret = f(ret, this._keys[x]);
      }
    }

    return ret;
  }

  nth(i, notFound = null) {
    this.checkVersion();
    if (i >= 0 && i < this.count()) {
      return this._asc ? this._keys[this._idx + i] : this._keys[this._idx - i];
    }
    return notFound;
  }

  count() {
    this.checkVersion();
    if (this._asc) {
      return this._end - this._idx + 1;
    } else {
      return this._idx - this._end + 1;
    }
  }

  /**
   * Get chunk as array
   * @returns {Array} Array of keys in chunk
   */
  toArray() {
    this.checkVersion();
    const arr = [];
    if (this._asc) {
      for (let i = this._idx; i <= this._end; i++) {
        arr.push(this._keys[i]);
      }
    } else {
      for (let i = this._idx; i >= this._end; i--) {
        arr.push(this._keys[i]);
      }
    }
    return arr;
  }
}
