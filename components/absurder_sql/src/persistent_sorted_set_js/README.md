# Persistent Sorted Set - JavaScript

A fast B-tree based persistent sorted set for JavaScript, ported from [tonsky/persistent-sorted-set](https://github.com/tonsky/persistent-sorted-set).

## Features

- **Persistent (Immutable)**: All operations return new sets, original remains unchanged
- **Transient Support**: Opt-in mutable mode for efficient batch operations
- **Custom Comparators**: Sort by any criteria
- **Fast Iteration**: Efficient forward and reverse traversal
- **Slicing**: Get subsets by range
- **Optional Persistence**: Integrate with storage backends (perfect for AbsurderSQL/SQLite)
- **Memory Efficient**: Uses WeakRef for automatic memory management

## Installation

```javascript
import { PersistentSortedSet } from './persistent-sorted-set-js/index.js';
```

## Quick Start

```javascript
// Create an empty set
let set = PersistentSortedSet.empty();

// Add elements (returns new set)
set = set.conj(3).conj(1).conj(2);

console.log(set.toArray()); // [1, 2, 3]

// Check membership
console.log(set.has(2)); // true

// Remove elements
set = set.disj(2);

console.log(set.toArray()); // [1, 3]

// Iterate
for (const item of set) {
  console.log(item);
}
```

## Custom Comparators

```javascript
// Reverse order
const reverseSet = PersistentSortedSet.empty((a, b) => {
  if (a < b) return 1;
  if (a > b) return -1;
  return 0;
});

// Create from array
const set = PersistentSortedSet.from([5, 2, 8, 1], reverseSet.comparator());
console.log(set.toArray()); // [8, 5, 2, 1]
```

## Range Queries (Slicing)

```javascript
const set = PersistentSortedSet.from([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);

// Get elements from 3 to 7 (inclusive)
const slice = set.slice(3, 7);
console.log(slice.toArray()); // [3, 4, 5, 6, 7]

// Reverse iteration from 8 to 4
const rslice = set.rslice(8, 4);
console.log(rslice.toArray()); // [8, 7, 6, 5, 4]
```

## Transient (Mutable) Mode

For efficient batch operations:

```javascript
let set = PersistentSortedSet.empty();

// Convert to transient
const transient = set.asTransient();

// Fast batch operations (modifies in place)
for (let i = 0; i < 1000; i++) {
  transient.conj(i);
}

// Convert back to persistent
set = transient.persistent();
```

## Storage Integration

Perfect for use with AbsurderSQL or other storage backends:

```javascript
import { IStorage } from './persistent-sorted-set-js/index.js';

class SQLiteStorage extends IStorage {
  restore(address) {
    // Load node from SQLite by address
    const row = db.query('SELECT * FROM nodes WHERE id = ?', [address]);
    // Reconstruct node from row data
    return ANode.restore(row.level, row.keys, row.addresses, this.settings);
  }

  store(node) {
    // Store node to SQLite
    if (node instanceof Leaf) {
      const id = db.insert('nodes', {
        level: 0,
        keys: JSON.stringify(node.keys())
      });
      return id;
    } else if (node instanceof Branch) {
      const id = db.insert('nodes', {
        level: node.level(),
        keys: JSON.stringify(node.keys()),
        addresses: JSON.stringify(node.addresses())
      });
      return id;
    }
  }
}

const storage = new SQLiteStorage();
let set = new PersistentSortedSet(null, storage);

set = set.conj(1).conj(2).conj(3);

// Store to database
const address = set.store();

// Later, restore from address
const restored = new PersistentSortedSet(null, storage, null, address);
```

## API Reference

### Construction

- `PersistentSortedSet.empty(comparator?)` - Create empty set
- `PersistentSortedSet.from(array, comparator?)` - Create from array

### Query

- `count()` - Number of elements
- `isEmpty()` - Check if empty
- `contains(key)` / `has(key)` - Check membership
- `get(key, notFound?)` - Get key or default

### Modification

- `conj(key)` - Add element (returns new set)
- `disj(key)` - Remove element (returns new set)

### Iteration

- `seq()` - Forward iterator
- `rseq()` - Reverse iterator
- `slice(from?, to?)` - Range iterator (forward)
- `rslice(from?, to?)` - Range iterator (reverse)
- `toArray()` - Convert to array
- `forEach(fn)` - Iterate with function
- `map(fn)` - Map to array
- `filter(pred)` - Filter to new set
- `reduce(fn, init)` - Reduce elements

### Transients

- `asTransient()` - Convert to mutable mode
- `persistent()` - Convert back to immutable

### Storage

- `store(storage?)` - Persist to storage
- `walkAddresses(fn)` - Walk all stored addresses

### Comparison

- `equals(other)` - Deep equality check
- `hashCode()` - Hash code
- `comparator()` - Get comparator function

## Configuration

```javascript
import { Settings, RefType } from './persistent-sorted-set-js/index.js';

const settings = new Settings(
  512,              // branching factor (default: 512)
  RefType.WEAK      // reference type (STRONG, SOFT, or WEAK)
);

const set = new PersistentSortedSet(null, null, settings);
```

## Performance

- Add: O(log n)
- Remove: O(log n)
- Lookup: O(log n)
- Iteration: O(n) with small constant factor
- Slice: O(log n) to find start, then O(k) for k elements

The default branching factor of 512 provides excellent performance for most use cases.

## Differences from Original Java Implementation

1. **No Clojure Dependencies**: Pure JavaScript implementation
2. **WeakRef Only**: JavaScript only has WeakRef, not SoftReference
3. **ES6 Iterators**: Implements JavaScript's iterator protocol
4. **No Metadata**: Removed Clojure's metadata support
5. **Simplified API**: Focused on core functionality

## Use Cases

- **Database Indices**: Perfect for implementing sorted indices in AbsurderSQL
- **Range Queries**: Efficient range scans
- **Ordered Collections**: Any time you need sorted data with fast updates
- **Version Control**: Persistent structure is great for undo/redo
- **Immutable Data Structures**: Building functional JavaScript applications

## License

Original Java implementation by Nikita Prokopov (tonsky).
JavaScript port maintains the same spirit and algorithms.

## Credits

Ported from [tonsky/persistent-sorted-set](https://github.com/tonsky/persistent-sorted-set)
