import { ANode } from './ANode.js';
import { ArrayUtil } from './ArrayUtil.js';
import { Stitch } from './Stitch.js';
import { Leaf } from './Leaf.js';

// Sentinel values
const EARLY_EXIT = [];
const UNCHANGED = [];

/**
 * Branch node in the B-tree (level >= 1)
 */
export class Branch extends ANode {
  /**
   * @param {number} level - Level of this branch (>= 1)
   * @param {number} len - Number of keys
   * @param {Array} keys - Keys array
   * @param {Array|null} addresses - Addresses array (nullable)
   * @param {Array|null} children - Children array (nullable)
   * @param {Settings} settings - Settings
   */
  constructor(level, len, keys, addresses, children, settings) {
    // Handle overload: Branch(level, len, settings)
    if (arguments.length === 3 && typeof keys === 'object' && keys._branchingFactor) {
      const actualSettings = keys;
      const newLen = ANode.newLen(len, actualSettings);
      super(len, new Array(newLen), actualSettings);
      this._level = level;
      this._addresses = null;
      this._children = null;
      return;
    }

    super(len, keys, settings);
    this._level = level;
    this._addresses = addresses;
    this._children = children;
  }

  level() {
    return this._level;
  }

  ensureAddresses() {
    if (this._addresses == null) {
      this._addresses = new Array(this._keys.length);
    }
    return this._addresses;
  }

  ensureChildren() {
    if (this._children == null) {
      this._children = new Array(this._keys.length);
    }
    return this._children;
  }

  /**
   * Get addresses as array
   * @returns {Array} Addresses array
   */
  addresses() {
    if (this._addresses == null) {
      return new Array(this._len);
    } else if (this._addresses.length === this._len) {
      return Array.from(this._addresses);
    } else {
      return this._addresses.slice(0, this._len);
    }
  }

  /**
   * Get address at index
   * @param {number} idx - Index
   * @returns {*} Address or null
   */
  address(idx) {
    if (this._addresses == null) {
      return null;
    }
    return this._addresses[idx];
  }

  /**
   * Set address at index
   * @param {number} idx - Index
   * @param {*} address - Address to set
   * @returns {*} The address
   */
  setAddress(idx, address) {
    if (this._addresses != null || address != null) {
      this.ensureAddresses();
      this._addresses[idx] = address;

      if (address != null && this._children != null && this._children[idx] instanceof ANode) {
        this._children[idx] = this._settings.makeReference(this._children[idx]);
      }
    }
    return address;
  }

  /**
   * Get child node at index
   * @param {IStorage} storage - Storage backend
   * @param {number} idx - Index
   * @returns {ANode} Child node
   */
  child(storage, idx) {
    let child = null;

    if (this._children != null) {
      const ref = this._children[idx];
      child = this._settings.readReference(ref);
    }

    if (child == null) {
      child = storage.restore(this._addresses[idx]);
      this.ensureChildren()[idx] = this._settings.makeReference(child);
    } else {
      if (this._addresses != null && this._addresses[idx] != null) {
        storage.accessed(this._addresses[idx]);
      }
    }

    return child;
  }

  /**
   * Set child node at index
   * @param {number} idx - Index
   * @param {ANode} child - Child node
   * @returns {ANode} The child
   */
  setChild(idx, child) {
    this.setAddress(idx, null);
    if (this._children != null || child != null) {
      this.ensureChildren();
      this._children[idx] = child;
    }
    return child;
  }

  count(storage) {
    let count = 0;
    for (let i = 0; i < this._len; i++) {
      count += this.child(storage, i).count(storage);
    }
    return count;
  }

  contains(storage, key, cmp) {
    let idx = this.search(key, cmp);
    if (idx >= 0) return true;

    let ins = -(idx + 1);
    if (ins === this._len) return false;

    return this.child(storage, ins).contains(storage, key, cmp);
  }

  add(storage, key, cmp, settings) {
    let idx = this.search(key, cmp);

    if (idx >= 0) {
      // Already in set
      return UNCHANGED;
    }

    let ins = -(idx + 1);
    if (ins === this._len) ins = this._len - 1;

    const nodes = this.child(storage, ins).add(storage, key, cmp, settings);

    if (nodes === UNCHANGED) {
      return UNCHANGED;
    }

    if (nodes === EARLY_EXIT) {
      return EARLY_EXIT;
    }

    // Same len, editable
    if (nodes.length === 1 && this.editable()) {
      const node = nodes[0];
      this._keys[ins] = node.maxKey();
      this.setChild(ins, node);

      if (ins === this._len - 1 && node.maxKey() === this.maxKey()) {
        return [this];
      } else {
        return EARLY_EXIT;
      }
    }

    // Same len, not editable
    if (nodes.length === 1) {
      const node = nodes[0];
      let newKeys;

      if (cmp(node.maxKey(), this._keys[ins]) === 0) {
        newKeys = this._keys;
      } else {
        newKeys = this._keys.slice(0, this._len);
        newKeys[ins] = node.maxKey();
      }

      let newAddresses = null;
      let newChildren = null;

      if (node === this.child(storage, ins)) {
        newAddresses = this._addresses;
        newChildren = this._children;
      } else {
        if (this._addresses != null) {
          newAddresses = this._addresses.slice(0, this._len);
          newAddresses[ins] = null;
        }

        newChildren = this._children == null ? new Array(this._keys.length) : this._children.slice(0, this._len);
        newChildren[ins] = node;
      }

      return [new Branch(this._level, this._len, newKeys, newAddresses, newChildren, settings)];
    }

    // len + 1
    if (this._len < this._settings.branchingFactor()) {
      const n = new Branch(this._level, this._len + 1, settings);

      new Stitch(n._keys, 0)
        .copyAll(this._keys, 0, ins)
        .copyOne(nodes[0].maxKey())
        .copyOne(nodes[1].maxKey())
        .copyAll(this._keys, ins + 1, this._len);

      if (this._addresses != null) {
        n.ensureAddresses();
        new Stitch(n._addresses, 0)
          .copyAll(this._addresses, 0, ins)
          .copyOne(null)
          .copyOne(null)
          .copyAll(this._addresses, ins + 1, this._len);
      }

      n.ensureChildren();
      new Stitch(n._children, 0)
        .copyAll(this._children, 0, ins)
        .copyOne(nodes[0])
        .copyOne(nodes[1])
        .copyAll(this._children, ins + 1, this._len);

      return [n];
    }

    // Split
    let half1 = (this._len + 1) >>> 1;
    if (ins + 1 === half1) half1++;
    const half2 = this._len + 1 - half1;

    // Add to first half
    if (ins < half1) {
      const keys1 = new Array(half1);
      new Stitch(keys1, 0)
        .copyAll(this._keys, 0, ins)
        .copyOne(nodes[0].maxKey())
        .copyOne(nodes[1].maxKey())
        .copyAll(this._keys, ins + 1, half1 - 1);

      const keys2 = new Array(half2);
      ArrayUtil.copy(this._keys, half1 - 1, this._len, keys2, 0);

      let addresses1 = null;
      let addresses2 = null;
      if (this._addresses != null) {
        addresses1 = new Array(half1);
        new Stitch(addresses1, 0)
          .copyAll(this._addresses, 0, ins)
          .copyOne(null)
          .copyOne(null)
          .copyAll(this._addresses, ins + 1, half1 - 1);
        addresses2 = new Array(half2);
        ArrayUtil.copy(this._addresses, half1 - 1, this._len, addresses2, 0);
      }

      const children1 = new Array(half1);
      new Stitch(children1, 0)
        .copyAll(this._children, 0, ins)
        .copyOne(nodes[0])
        .copyOne(nodes[1])
        .copyAll(this._children, ins + 1, half1 - 1);

      let children2 = null;
      if (this._children != null) {
        children2 = new Array(half2);
        ArrayUtil.copy(this._children, half1 - 1, this._len, children2, 0);
      }

      return [
        new Branch(this._level, half1, keys1, addresses1, children1, settings),
        new Branch(this._level, half2, keys2, addresses2, children2, settings)
      ];
    }

    // Add to second half
    const keys1 = new Array(half1);
    const keys2 = new Array(half2);
    ArrayUtil.copy(this._keys, 0, half1, keys1, 0);

    new Stitch(keys2, 0)
      .copyAll(this._keys, half1, ins)
      .copyOne(nodes[0].maxKey())
      .copyOne(nodes[1].maxKey())
      .copyAll(this._keys, ins + 1, this._len);

    let addresses1 = null;
    let addresses2 = null;
    if (this._addresses != null) {
      addresses1 = new Array(half1);
      ArrayUtil.copy(this._addresses, 0, half1, addresses1, 0);
      addresses2 = new Array(half2);
      new Stitch(addresses2, 0)
        .copyAll(this._addresses, half1, ins)
        .copyOne(null)
        .copyOne(null)
        .copyAll(this._addresses, ins + 1, this._len);
    }

    let children1 = null;
    const children2 = new Array(half2);
    if (this._children != null) {
      children1 = new Array(half1);
      ArrayUtil.copy(this._children, 0, half1, children1, 0);
    }
    new Stitch(children2, 0)
      .copyAll(this._children, half1, ins)
      .copyOne(nodes[0])
      .copyOne(nodes[1])
      .copyAll(this._children, ins + 1, this._len);

    return [
      new Branch(this._level, half1, keys1, addresses1, children1, settings),
      new Branch(this._level, half2, keys2, addresses2, children2, settings)
    ];
  }

  remove(storage, key, _left, _right, cmp, settings) {
    const left = _left;
    const right = _right;

    let idx = this.search(key, cmp);
    if (idx < 0) idx = -(idx + 1);

    if (idx === this._len) {
      // Not in set
      return UNCHANGED;
    }

    const leftChild = idx > 0 ? this.child(storage, idx - 1) : null;
    const rightChild = idx < this._len - 1 ? this.child(storage, idx + 1) : null;
    const leftChildLen = ANode.safeLen(leftChild);
    const rightChildLen = ANode.safeLen(rightChild);

    const nodes = this.child(storage, idx).remove(storage, key, leftChild, rightChild, cmp, settings);

    if (nodes === UNCHANGED) {
      return UNCHANGED;
    }

    if (nodes === EARLY_EXIT) {
      return EARLY_EXIT;
    }

    const leftChanged = leftChild !== nodes[0] || leftChildLen !== ANode.safeLen(nodes[0]);
    const rightChanged = rightChild !== nodes[2] || rightChildLen !== ANode.safeLen(nodes[2]);

    let newLen = this._len - 1
      - (leftChild != null ? 1 : 0)
      - (rightChild != null ? 1 : 0)
      + (nodes[0] != null ? 1 : 0)
      + 1
      + (nodes[2] != null ? 1 : 0);

    // No rebalance needed
    if (newLen >= this._settings.minBranchingFactor() || (left == null && right == null)) {
      // Can update in place
      if (this.editable() && idx < this._len - 2) {
        const ks = new Stitch(this._keys, Math.max(idx - 1, 0));
        if (nodes[0] != null) ks.copyOne(nodes[0].maxKey());
        ks.copyOne(nodes[1].maxKey());
        if (nodes[2] != null) ks.copyOne(nodes[2].maxKey());
        if (newLen !== this._len) {
          ks.copyAll(this._keys, idx + 2, this._len);
        }

        if (this._addresses != null) {
          const as = new Stitch(this._addresses, Math.max(idx - 1, 0));
          if (nodes[0] != null) as.copyOne(leftChanged ? null : this.address(idx - 1));
          as.copyOne(null);
          if (nodes[2] != null) as.copyOne(rightChanged ? null : this.address(idx + 1));
          if (newLen !== this._len) {
            as.copyAll(this._addresses, idx + 2, this._len);
          }
        }

        this.ensureChildren();
        const cs = new Stitch(this._children, Math.max(idx - 1, 0));
        if (nodes[0] != null) cs.copyOne(nodes[0]);
        cs.copyOne(nodes[1]);
        if (nodes[2] != null) cs.copyOne(nodes[2]);
        if (newLen !== this._len) {
          cs.copyAll(this._children, idx + 2, this._len);
        }

        this._len = newLen;
        return EARLY_EXIT;
      }

      const newCenter = new Branch(this._level, newLen, settings);

      const ks = new Stitch(newCenter._keys, 0);
      ks.copyAll(this._keys, 0, idx - 1);
      if (nodes[0] != null) ks.copyOne(nodes[0].maxKey());
      ks.copyOne(nodes[1].maxKey());
      if (nodes[2] != null) ks.copyOne(nodes[2].maxKey());
      ks.copyAll(this._keys, idx + 2, this._len);

      if (this._addresses != null) {
        const as = new Stitch(newCenter.ensureAddresses(), 0);
        as.copyAll(this._addresses, 0, idx - 1);
        if (nodes[0] != null) as.copyOne(leftChanged ? null : this.address(idx - 1));
        as.copyOne(null);
        if (nodes[2] != null) as.copyOne(rightChanged ? null : this.address(idx + 1));
        as.copyAll(this._addresses, idx + 2, this._len);
      }

      newCenter.ensureChildren();
      const cs = new Stitch(newCenter._children, 0);
      cs.copyAll(this._children, 0, idx - 1);
      if (nodes[0] != null) cs.copyOne(nodes[0]);
      cs.copyOne(nodes[1]);
      if (nodes[2] != null) cs.copyOne(nodes[2]);
      cs.copyAll(this._children, idx + 2, this._len);

      return [left, newCenter, right];
    }

    // Can join with left
    if (left != null && left._len + newLen <= this._settings.branchingFactor()) {
      const join = new Branch(this._level, left._len + newLen, settings);

      const ks = new Stitch(join._keys, 0);
      ks.copyAll(left._keys, 0, left._len);
      ks.copyAll(this._keys, 0, idx - 1);
      if (nodes[0] != null) ks.copyOne(nodes[0].maxKey());
      ks.copyOne(nodes[1].maxKey());
      if (nodes[2] != null) ks.copyOne(nodes[2].maxKey());
      ks.copyAll(this._keys, idx + 2, this._len);

      if (left._addresses != null || this._addresses != null) {
        const as = new Stitch(join.ensureAddresses(), 0);
        as.copyAll(left._addresses, 0, left._len);
        as.copyAll(this._addresses, 0, idx - 1);
        if (nodes[0] != null) as.copyOne(leftChanged ? null : this.address(idx - 1));
        as.copyOne(null);
        if (nodes[2] != null) as.copyOne(rightChanged ? null : this.address(idx + 1));
        as.copyAll(this._addresses, idx + 2, this._len);
      }

      join.ensureChildren();
      const cs = new Stitch(join._children, 0);
      cs.copyAll(left._children, 0, left._len);
      cs.copyAll(this._children, 0, idx - 1);
      if (nodes[0] != null) cs.copyOne(nodes[0]);
      cs.copyOne(nodes[1]);
      if (nodes[2] != null) cs.copyOne(nodes[2]);
      cs.copyAll(this._children, idx + 2, this._len);

      return [null, join, right];
    }

    // Can join with right
    if (right != null && newLen + right._len <= this._settings.branchingFactor()) {
      const join = new Branch(this._level, newLen + right._len, settings);

      const ks = new Stitch(join._keys, 0);
      ks.copyAll(this._keys, 0, idx - 1);
      if (nodes[0] != null) ks.copyOne(nodes[0].maxKey());
      ks.copyOne(nodes[1].maxKey());
      if (nodes[2] != null) ks.copyOne(nodes[2].maxKey());
      ks.copyAll(this._keys, idx + 2, this._len);
      ks.copyAll(right._keys, 0, right._len);

      if (this._addresses != null || right._addresses != null) {
        const as = new Stitch(join.ensureAddresses(), 0);
        as.copyAll(this._addresses, 0, idx - 1);
        if (nodes[0] != null) as.copyOne(leftChanged ? null : this.address(idx - 1));
        as.copyOne(null);
        if (nodes[2] != null) as.copyOne(rightChanged ? null : this.address(idx + 1));
        as.copyAll(this._addresses, idx + 2, this._len);
        as.copyAll(right._addresses, 0, right._len);
      }

      join.ensureChildren();
      const cs = new Stitch(join._children, 0);
      cs.copyAll(this._children, 0, idx - 1);
      if (nodes[0] != null) cs.copyOne(nodes[0]);
      cs.copyOne(nodes[1]);
      if (nodes[2] != null) cs.copyOne(nodes[2]);
      cs.copyAll(this._children, idx + 2, this._len);
      cs.copyAll(right._children, 0, right._len);

      return [left, join, null];
    }

    // Borrow from left
    if (left != null && (right == null || left._len >= right._len)) {
      const totalLen = left._len + newLen;
      const newLeftLen = totalLen >>> 1;
      const newCenterLen = totalLen - newLeftLen;

      const newLeft = new Branch(this._level, newLeftLen, settings);
      const newCenter = new Branch(this._level, newCenterLen, settings);

      ArrayUtil.copy(left._keys, 0, newLeftLen, newLeft._keys, 0);

      const ks = new Stitch(newCenter._keys, 0);
      ks.copyAll(left._keys, newLeftLen, left._len);
      ks.copyAll(this._keys, 0, idx - 1);
      if (nodes[0] != null) ks.copyOne(nodes[0].maxKey());
      ks.copyOne(nodes[1].maxKey());
      if (nodes[2] != null) ks.copyOne(nodes[2].maxKey());
      ks.copyAll(this._keys, idx + 2, this._len);

      if (left._addresses != null) {
        ArrayUtil.copy(left._addresses, 0, newLeftLen, newLeft.ensureAddresses(), 0);
      }
      if (left._children != null) {
        ArrayUtil.copy(left._children, 0, newLeftLen, newLeft.ensureChildren(), 0);
      }

      if (left._addresses != null || this._addresses != null) {
        const as = new Stitch(newCenter.ensureAddresses(), 0);
        as.copyAll(left._addresses, newLeftLen, left._len);
        as.copyAll(this._addresses, 0, idx - 1);
        if (nodes[0] != null) as.copyOne(leftChanged ? null : this.address(idx - 1));
        as.copyOne(null);
        if (nodes[2] != null) as.copyOne(rightChanged ? null : this.address(idx + 1));
        as.copyAll(this._addresses, idx + 2, this._len);
      }

      newCenter.ensureChildren();
      const cs = new Stitch(newCenter._children, 0);
      cs.copyAll(left._children, newLeftLen, left._len);
      cs.copyAll(this._children, 0, idx - 1);
      if (nodes[0] != null) cs.copyOne(nodes[0]);
      cs.copyOne(nodes[1]);
      if (nodes[2] != null) cs.copyOne(nodes[2]);
      cs.copyAll(this._children, idx + 2, this._len);

      return [newLeft, newCenter, right];
    }

    // Borrow from right
    if (right != null) {
      const totalLen = newLen + right._len;
      const newCenterLen = totalLen >>> 1;
      const newRightLen = totalLen - newCenterLen;
      const rightHead = right._len - newRightLen;

      const newCenter = new Branch(this._level, newCenterLen, settings);
      const newRight = new Branch(this._level, newRightLen, settings);

      const ks = new Stitch(newCenter._keys, 0);
      ks.copyAll(this._keys, 0, idx - 1);
      if (nodes[0] != null) ks.copyOne(nodes[0].maxKey());
      ks.copyOne(nodes[1].maxKey());
      if (nodes[2] != null) ks.copyOne(nodes[2].maxKey());
      ks.copyAll(this._keys, idx + 2, this._len);
      ks.copyAll(right._keys, 0, rightHead);

      ArrayUtil.copy(right._keys, rightHead, right._len, newRight._keys, 0);

      if (this._addresses != null || right._addresses != null) {
        const as = new Stitch(newCenter.ensureAddresses(), 0);
        as.copyAll(this._addresses, 0, idx - 1);
        if (nodes[0] != null) as.copyOne(leftChanged ? null : this.address(idx - 1));
        as.copyOne(null);
        if (nodes[2] != null) as.copyOne(rightChanged ? null : this.address(idx + 1));
        as.copyAll(this._addresses, idx + 2, this._len);
        as.copyAll(right._addresses, 0, rightHead);
      }

      newCenter.ensureChildren();
      const cs = new Stitch(newCenter._children, 0);
      cs.copyAll(this._children, 0, idx - 1);
      if (nodes[0] != null) cs.copyOne(nodes[0]);
      cs.copyOne(nodes[1]);
      if (nodes[2] != null) cs.copyOne(nodes[2]);
      cs.copyAll(this._children, idx + 2, this._len);
      cs.copyAll(right._children, 0, rightHead);

      if (right._addresses != null) {
        ArrayUtil.copy(right._addresses, rightHead, right._len, newRight.ensureAddresses(), 0);
      }
      if (right._children != null) {
        ArrayUtil.copy(right._children, rightHead, right._len, newRight.ensureChildren(), 0);
      }

      return [left, newCenter, newRight];
    }

    throw new Error('Unreachable');
  }

  walkAddresses(storage, onAddress) {
    for (let i = 0; i < this._len; i++) {
      const address = this.address(i);
      if (address != null) {
        if (!onAddress(address)) {
          continue;
        }
      }
      if (this._level > 1) {
        this.child(storage, i).walkAddresses(storage, onAddress);
      }
    }
  }

  store(storage) {
    this.ensureAddresses();
    for (let i = 0; i < this._len; i++) {
      if (this._addresses[i] == null) {
        const child = this._settings.readReference(this._children[i]);
        this.setAddress(i, child.store(storage));
      }
    }
    return storage.store(this);
  }

  str(storage, lvl) {
    const lines = [];
    for (let i = 0; i < this._len; i++) {
      lines.push('\n');
      for (let j = 0; j < lvl; j++) {
        lines.push('| ');
      }
      lines.push(this._keys[i] + ': ' + this.child(storage, i).str(storage, lvl + 1));
    }
    return lines.join('');
  }

  toString(sb, address, indent) {
    sb.push(indent);
    sb.push('Branch addr: ' + address + ' len: ' + this._len + ' ');
    for (let i = 0; i < this._len; i++) {
      sb.push('\n');
      let child = null;
      if (this._children != null) {
        const ref = this._children[i];
        if (ref != null) {
          child = this._settings.readReference(ref);
        }
      }
      if (child != null) {
        child.toString(sb, this.address(i), indent + '  ');
      } else {
        sb.push(indent + '  ' + this.address(i) + ': <lazy> ');
      }
    }
  }
}

// Export sentinels
Branch.EARLY_EXIT = EARLY_EXIT;
Branch.UNCHANGED = UNCHANGED;
