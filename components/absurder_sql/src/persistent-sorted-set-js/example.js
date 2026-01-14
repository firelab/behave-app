/**
 * Example usage of PersistentSortedSet
 */

import { PersistentSortedSet, IStorage, ANode } from './index.js';
import { Leaf } from './Leaf.js';
import { Branch } from './Branch.js';

console.log('=== Persistent Sorted Set Examples ===\n');

// Example 1: Basic usage
console.log('1. Basic Usage:');
let set = PersistentSortedSet.empty();
set = set.conj(3).conj(1).conj(4).conj(1).conj(5).conj(9).conj(2).conj(6);
console.log('Set:', set.toArray());
console.log('Count:', set.count());
console.log('Has 5?', set.has(5));
console.log('Has 7?', set.has(7));
console.log();

// Example 2: Remove elements
console.log('2. Remove Elements:');
set = set.disj(4).disj(6);
console.log('After removing 4 and 6:', set.toArray());
console.log();

// Example 3: Custom comparator (reverse order)
console.log('3. Custom Comparator (Reverse):');
const reverseSet = PersistentSortedSet.from(
  [1, 2, 3, 4, 5],
  (a, b) => {
    if (a < b) return 1;
    if (a > b) return -1;
    return 0;
  }
);
console.log('Reverse sorted:', reverseSet.toArray());
console.log();

// Example 4: Range queries
console.log('4. Range Queries:');
const largeSet = PersistentSortedSet.from(
  Array.from({ length: 20 }, (_, i) => i + 1)
);
console.log('Full set:', largeSet.toArray());

const slice = largeSet.slice(5, 10);
if (slice) {
  console.log('Slice [5, 10]:', slice.toArray());
}

const rslice = largeSet.rslice(15, 10);
if (rslice) {
  console.log('Reverse slice [15, 10]:', rslice.toArray());
}
console.log();

// Example 5: Iteration
console.log('5. Iteration Methods:');
const smallSet = PersistentSortedSet.from([1, 2, 3, 4, 5]);

console.log('forEach:');
smallSet.forEach((item, i) => console.log(`  [${i}] = ${item}`));

console.log('map:', smallSet.map(x => x * 2));
console.log('filter (even):', smallSet.filter(x => x % 2 === 0).toArray());
console.log('reduce (sum):', smallSet.reduce((acc, x) => acc + x, 0));
console.log();

// Example 6: Transient operations
console.log('6. Transient (Mutable) Mode:');
let transSet = PersistentSortedSet.empty().asTransient();

console.time('Transient add 1000');
for (let i = 0; i < 1000; i++) {
  transSet.conj(i);
}
console.timeEnd('Transient add 1000');

transSet = transSet.persistent();
console.log('Final count:', transSet.count());
console.log('First 10:', transSet.slice(0, 9).toArray());
console.log();

// Example 7: Storage backend (mock implementation)
console.log('7. Storage Backend:');

class MockStorage extends IStorage {
  constructor() {
    super();
    this.store = new Map();
    this.nextId = 1;
  }

  restore(address) {
    const data = this.store.get(address);
    if (!data) {
      throw new Error('Address not found: ' + address);
    }
    return ANode.restore(data.level, data.keys, data.addresses, data.settings);
  }

  store(node) {
    const id = this.nextId++;
    const data = {
      level: node.level(),
      keys: node.keys(),
      addresses: node instanceof Branch ? node.addresses() : null,
      settings: node._settings
    };
    this.store.set(id, data);
    return id;
  }
}

const storage = new MockStorage();
let storedSet = new PersistentSortedSet(null, storage);
storedSet = storedSet.conj(1).conj(2).conj(3).conj(4).conj(5);

console.log('Before store:', storedSet.toArray());
const address = storedSet.store();
console.log('Stored at address:', address);
console.log('Storage has', storage.store.size, 'nodes');
console.log();

// Example 8: Set operations
console.log('8. Set Equality:');
const set1 = PersistentSortedSet.from([1, 2, 3]);
const set2 = PersistentSortedSet.from([3, 2, 1]);
const set3 = PersistentSortedSet.from([1, 2, 4]);

console.log('set1:', set1.toArray());
console.log('set2:', set2.toArray());
console.log('set3:', set3.toArray());
console.log('set1 equals set2?', set1.equals(set2));
console.log('set1 equals set3?', set1.equals(set3));
console.log();

console.log('=== All Examples Complete ===');
