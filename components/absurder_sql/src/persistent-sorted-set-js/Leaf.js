import { ANode } from './ANode.js';
import { ArrayUtil } from './ArrayUtil.js';
import { Stitch } from './Stitch.js';

// Sentinel values
const EARLY_EXIT = [];
const UNCHANGED = [];

/**
 * Leaf node in the B-tree (level 0)
 */
export class Leaf extends ANode {
  /**
   * @param {number} len - Number of keys
   * @param {Array} keys - Keys array
   * @param {Settings} settings - Settings
   */
  constructor(len, keys, settings) {
    if (Array.isArray(keys) && keys.length >= len) {
      super(len, keys, settings);
    } else if (typeof keys === 'number') {
      // Constructor overload: Leaf(len, settings)
      const actualSettings = settings || keys;
      const newLen = ANode.newLen(len, actualSettings);
      super(len, new Array(newLen), actualSettings);
    } else if (Array.isArray(keys)) {
      // Constructor overload: Leaf(keys[], settings)
      const actualSettings = settings;
      super(keys.length, Array.from(keys), actualSettings);
    } else {
      throw new Error('Invalid Leaf constructor arguments');
    }
  }

  level() {
    return 0;
  }

  count(storage) {
    return this._len;
  }

  contains(storage, key, cmp) {
    return this.search(key, cmp) >= 0;
  }

  add(storage, key, cmp, settings) {
    let idx = this.search(key, cmp);

    if (idx >= 0) {
      // Key already exists
      return UNCHANGED;
    }

    const ins = -(idx + 1);

    // Can fit in current node
    if (this._len < this._settings.branchingFactor()) {
      // Editable - modify in place
      if (this.editable()) {
        for (let i = this._len; i > ins; i--) {
          this._keys[i] = this._keys[i - 1];
        }
        this._keys[ins] = key;
        this._len++;
        return EARLY_EXIT;
      }

      // Not editable - create new node
      const newKeys = new Array(this._keys.length);
      new Stitch(newKeys, 0)
        .copyAll(this._keys, 0, ins)
        .copyOne(key)
        .copyAll(this._keys, ins, this._len);

      return [new Leaf(this._len + 1, newKeys, settings)];
    }

    // Need to split
    const half1 = (this._len + 1) >>> 1;
    const half2 = this._len + 1 - half1;

    // Insert in first half
    if (ins < half1) {
      const keys1 = new Array(half1);
      const keys2 = new Array(half2);

      new Stitch(keys1, 0)
        .copyAll(this._keys, 0, ins)
        .copyOne(key)
        .copyAll(this._keys, ins, half1 - 1);

      ArrayUtil.copy(this._keys, half1 - 1, this._len, keys2, 0);

      return [
        new Leaf(half1, keys1, settings),
        new Leaf(half2, keys2, settings)
      ];
    }

    // Insert in second half
    const keys1 = new Array(half1);
    const keys2 = new Array(half2);

    ArrayUtil.copy(this._keys, 0, half1, keys1, 0);

    new Stitch(keys2, 0)
      .copyAll(this._keys, half1, ins)
      .copyOne(key)
      .copyAll(this._keys, ins, this._len);

    return [
      new Leaf(half1, keys1, settings),
      new Leaf(half2, keys2, settings)
    ];
  }

  remove(storage, key, _left, _right, cmp, settings) {
    const left = _left;
    const right = _right;

    const idx = this.search(key, cmp);

    if (idx < 0) {
      // Key not found
      return UNCHANGED;
    }

    const newLen = this._len - 1;

    // No rebalancing needed
    if (newLen >= this._settings.minBranchingFactor() || (left == null && right == null)) {
      // Can edit in place
      if (this.editable()) {
        for (let i = idx; i < newLen; i++) {
          this._keys[i] = this._keys[i + 1];
        }
        this._len = newLen;
        return EARLY_EXIT;
      }

      // Create new node
      const newKeys = new Array(this._keys.length);
      new Stitch(newKeys, 0)
        .copyAll(this._keys, 0, idx)
        .copyAll(this._keys, idx + 1, this._len);

      return [left, new Leaf(newLen, newKeys, settings), right];
    }

    // Can join with left
    if (left != null && left._len + newLen <= this._settings.branchingFactor()) {
      const joinKeys = new Array(left._len + newLen);
      new Stitch(joinKeys, 0)
        .copyAll(left._keys, 0, left._len)
        .copyAll(this._keys, 0, idx)
        .copyAll(this._keys, idx + 1, this._len);

      return [null, new Leaf(left._len + newLen, joinKeys, settings), right];
    }

    // Can join with right
    if (right != null && newLen + right._len <= this._settings.branchingFactor()) {
      const joinKeys = new Array(newLen + right._len);
      new Stitch(joinKeys, 0)
        .copyAll(this._keys, 0, idx)
        .copyAll(this._keys, idx + 1, this._len)
        .copyAll(right._keys, 0, right._len);

      return [left, new Leaf(newLen + right._len, joinKeys, settings), null];
    }

    // Borrow from left
    if (left != null && (right == null || left._len >= right._len)) {
      const totalLen = left._len + newLen;
      const newLeftLen = totalLen >>> 1;
      const newCenterLen = totalLen - newLeftLen;

      const newLeftKeys = new Array(newLeftLen);
      const newCenterKeys = new Array(newCenterLen);

      ArrayUtil.copy(left._keys, 0, newLeftLen, newLeftKeys, 0);

      new Stitch(newCenterKeys, 0)
        .copyAll(left._keys, newLeftLen, left._len)
        .copyAll(this._keys, 0, idx)
        .copyAll(this._keys, idx + 1, this._len);

      return [
        new Leaf(newLeftLen, newLeftKeys, settings),
        new Leaf(newCenterLen, newCenterKeys, settings),
        right
      ];
    }

    // Borrow from right
    if (right != null) {
      const totalLen = newLen + right._len;
      const newCenterLen = totalLen >>> 1;
      const newRightLen = totalLen - newCenterLen;
      const rightHead = right._len - newRightLen;

      const newCenterKeys = new Array(newCenterLen);
      const newRightKeys = new Array(newRightLen);

      new Stitch(newCenterKeys, 0)
        .copyAll(this._keys, 0, idx)
        .copyAll(this._keys, idx + 1, this._len)
        .copyAll(right._keys, 0, rightHead);

      ArrayUtil.copy(right._keys, rightHead, right._len, newRightKeys, 0);

      return [
        left,
        new Leaf(newCenterLen, newCenterKeys, settings),
        new Leaf(newRightLen, newRightKeys, settings)
      ];
    }

    throw new Error('Unreachable');
  }

  walkAddresses(storage, onAddress) {
    // Leaf nodes have no addresses
  }

  store(storage) {
    return storage.store(this);
  }

  str(storage, lvl) {
    return this._keys.slice(0, this._len).join(' ');
  }

  toString(sb, address, indent) {
    sb.push(indent);
    sb.push('Leaf addr: ' + address + ' len: ' + this._len + ' keys: ');
    sb.push(this._keys.slice(0, this._len).join(' '));
  }
}

// Export sentinels
Leaf.EARLY_EXIT = EARLY_EXIT;
Leaf.UNCHANGED = UNCHANGED;
