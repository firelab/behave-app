/**
 * Array utility functions
 */
export class ArrayUtil {
  /**
   * Copy elements from source array to target array
   * @param {Array} source - Source array
   * @param {number} srcFrom - Start index in source
   * @param {number} srcTo - End index in source (exclusive)
   * @param {Array} target - Target array
   * @param {number} tgtFrom - Start index in target
   */
  static copy(source, srcFrom, srcTo, target, tgtFrom) {
    const len = srcTo - srcFrom;
    for (let i = 0; i < len; i++) {
      target[tgtFrom + i] = source[srcFrom + i];
    }
  }

  /**
   * Convert indexed collection to array
   * @param {*} indexed - Collection with nth() method
   * @param {Function} arrayType - Constructor for array type
   * @param {number} len - Length of collection
   * @returns {Array} New array with elements
   */
  static indexedToArray(indexed, arrayType, len) {
    const arr = new Array(len);
    for (let i = 0; i < len; i++) {
      arr[i] = indexed.nth(i);
    }
    return arr;
  }

  /**
   * Filter array to keep only distinct consecutive elements
   * @param {Array} arr - Array to filter
   * @param {number} len - Number of elements to process
   * @param {Function} cmp - Comparator function
   * @returns {number} Number of distinct elements
   */
  static distinct(arr, len, cmp) {
    if (len <= 1) return len;

    let writeIdx = 1;
    for (let readIdx = 1; readIdx < len; readIdx++) {
      if (cmp(arr[readIdx - 1], arr[readIdx]) !== 0) {
        arr[writeIdx++] = arr[readIdx];
      }
    }
    return writeIdx;
  }
}
