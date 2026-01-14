/**
 * Utility class for efficient array copying with offset tracking
 */
export class Stitch {
  /**
   * @param {Array} target - Target array
   * @param {number} offset - Starting offset in target array
   */
  constructor(target, offset) {
    this.target = target;
    this.offset = offset;
  }

  /**
   * Copy a range of elements from source to target
   * @param {Array} source - Source array
   * @param {number} from - Start index (inclusive)
   * @param {number} to - End index (exclusive)
   * @returns {Stitch} this for chaining
   */
  copyAll(source, from, to) {
    if (to >= from) {
      if (source != null) {
        for (let i = from; i < to; i++) {
          this.target[this.offset++] = source[i];
        }
      } else {
        this.offset += (to - from);
      }
    }
    return this;
  }

  /**
   * Copy a single value to target
   * @param {*} val - Value to copy
   * @returns {Stitch} this for chaining
   */
  copyOne(val) {
    this.target[this.offset++] = val;
    return this;
  }
}
