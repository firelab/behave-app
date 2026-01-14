import { RefType } from './RefType.js';

/**
 * Configuration settings for PersistentSortedSet
 */
export class Settings {
  /**
   * @param {number} branchingFactor - Maximum children per branch node (default: 512)
   * @param {string} refType - Reference type (STRONG, SOFT, or WEAK)
   * @param {{value: boolean}} edit - Editable flag (for transient collections)
   */
  constructor(branchingFactor = 0, refType = null, edit = null) {
    if (branchingFactor <= 0) {
      branchingFactor = 512;
    }
    if (refType == null) {
      refType = RefType.SOFT;
    }

    this._branchingFactor = branchingFactor;
    this._refType = refType;
    this._edit = edit;
  }

  /**
   * @returns {number} Minimum branching factor (half of max)
   */
  minBranchingFactor() {
    return this._branchingFactor >>> 1;
  }

  /**
   * @returns {number} Maximum branching factor
   */
  branchingFactor() {
    return this._branchingFactor;
  }

  /**
   * @returns {number} Initial array expansion length
   */
  expandLen() {
    return 8;
  }

  /**
   * @returns {string} Reference type
   */
  refType() {
    return this._refType;
  }

  /**
   * @returns {boolean} Whether this collection is editable (transient)
   */
  editable() {
    return this._edit != null && this._edit.value;
  }

  /**
   * Make settings editable
   * @param {boolean} value - Must be true
   * @returns {Settings} New editable settings
   */
  editableSettings(value) {
    if (this.editable()) {
      throw new Error('Already editable');
    }
    if (value !== true) {
      throw new Error('Value must be true');
    }
    return new Settings(this._branchingFactor, this._refType, { value });
  }

  /**
   * Make collection persistent (no longer editable)
   */
  persistent() {
    if (this._edit == null) {
      throw new Error('Not editable');
    }
    this._edit.value = false;
  }

  /**
   * Create a reference based on reference type
   * @param {*} value - Value to wrap
   * @returns {*} Reference to value
   */
  makeReference(value) {
    switch (this._refType) {
      case RefType.STRONG:
        return value;
      case RefType.SOFT:
      case RefType.WEAK:
        // JavaScript only has WeakRef, no SoftReference equivalent
        // WeakRef is close enough for our purposes
        return new WeakRef(value);
      default:
        throw new Error('Unexpected refType: ' + this._refType);
    }
  }

  /**
   * Read value from reference
   * @param {*} ref - Reference or direct value
   * @returns {*} The underlying value
   */
  readReference(ref) {
    return ref instanceof WeakRef ? ref.deref() : ref;
  }
}
