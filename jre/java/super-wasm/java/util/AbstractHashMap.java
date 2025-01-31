/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package java.util;

import java.io.Serializable;

abstract class AbstractHashMap<K, V> extends AbstractMap<K, V> implements Cloneable, Serializable {

  /**
   * Min capacity (other than zero) for a HashMap. Must be a power of two greater than 1 (and less
   * than 1 << 30).
   */
  private static final int MINIMUM_CAPACITY = 4;

  /** Max capacity for a HashMap. Must be a power of two >= MINIMUM_CAPACITY. */
  private static final int MAXIMUM_CAPACITY = 1 << 30;

  /**
   * An empty table shared by all zero-capacity maps (typically from default constructor). It is
   * never written to, and replaced on first put. Its size is set to half the minimum, so that the
   * first resize will create a minimum-sized table.
   */
  private static final Entry[] EMPTY_TABLE = new HashMapEntry[MINIMUM_CAPACITY >>> 1];

  /**
   * The default load factor. Note that this implementation ignores the load factor, but cannot do
   * away with it entirely because it's mentioned in the API.
   *
   * <p>Note that this constant has no impact on the behavior of the program, but it is emitted as
   * part of the serialized form. The load factor of .75 is hardwired into the program, which uses
   * cheap shifts in place of expensive division.
   */
  static final float DEFAULT_LOAD_FACTOR = .75F;

  /**
   * The hash table. If this hash map contains a mapping for null, it is not represented this hash
   * table.
   */
  transient HashMapEntry<K, V>[] table;

  /** The entry representing the null key, or null if there's no such mapping. */
  transient HashMapEntry<K, V> entryForNullKey;

  /** The number of mappings in this hash map. */
  transient int size;

  /**
   * Incremented by "structural modifications" to allow (best effort) detection of concurrent
   * modification.
   */
  transient int modCount;

  /**
   * The table is rehashed when its size exceeds this threshold. The value of this field is
   * generally .75 * capacity, except when the capacity is zero, as described in the EMPTY_TABLE
   * declaration above.
   */
  private transient int threshold;

  private transient Set<Entry<K, V>> entrySet;

  @SuppressWarnings("unchecked")
  public AbstractHashMap() {
    table = (HashMapEntry<K, V>[]) EMPTY_TABLE;
    threshold = -1; // Forces first put invocation to replace EMPTY_TABLE
  }

  public AbstractHashMap(int capacity) {
    if (capacity < 0) {
      throw new IllegalArgumentException("Capacity: " + capacity);
    }
    if (capacity == 0) {
      @SuppressWarnings("unchecked")
      HashMapEntry<K, V>[] tab = (HashMapEntry<K, V>[]) EMPTY_TABLE;
      table = tab;
      threshold = -1; // Forces first put() to replace EMPTY_TABLE
      return;
    }
    if (capacity < MINIMUM_CAPACITY) {
      capacity = MINIMUM_CAPACITY;
    } else if (capacity > MAXIMUM_CAPACITY) {
      capacity = MAXIMUM_CAPACITY;
    } else {
      capacity = roundUpToPowerOfTwo(capacity);
    }
    makeTable(capacity);
  }

  public AbstractHashMap(int capacity, float loadFactor) {
    this(capacity);
    if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
      throw new IllegalArgumentException("Load factor: " + loadFactor);
    }
    /*
     * Note that this implementation ignores loadFactor; it always uses
     * a load factor of 3/4. This simplifies the code and generally
     * improves performance.
     */
  }

  public AbstractHashMap(Map<? extends K, ? extends V> map) {
    this(capacityForInitSize(map.size()));
    constructorPutAll(map);
  }

  private static int roundUpToPowerOfTwo(int i) {
    i--; // If input is a power of two, shift its high-order bit right.
    // "Smear" the high-order bit all the way to the right.
    i |= i >>> 1;
    i |= i >>> 2;
    i |= i >>> 4;
    i |= i >>> 8;
    i |= i >>> 16;
    return i + 1;
  }

  abstract int getHashCode(Object key);

  abstract boolean equals(Object value1, Object value2);

  private int secondaryHash(Object o) {
    int h = getHashCode(o);

    // Spread bits to regularize both segment and index locations,
    // using variant of single-word Wang/Jenkins hash.
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  /**
   * Inserts all of the elements of map into this HashMap in a manner suitable for use by
   * constructors and pseudo-constructors (i.e., clone, readObject). Also used by LinkedHashMap.
   */
  final void constructorPutAll(Map<? extends K, ? extends V> map) {
    if (table == EMPTY_TABLE) {
      doubleCapacity(); // Don't do unchecked puts to a shared table.
    }
    for (Entry<? extends K, ? extends V> e : map.entrySet()) {
      constructorPut(e.getKey(), e.getValue());
    }
  }

  /**
   * Returns an appropriate capacity for the specified initial size. Does not round the result up to
   * a power of two; the caller must do this! The returned value will be between 0 and
   * MAXIMUM_CAPACITY (inclusive).
   */
  static int capacityForInitSize(int size) {
    int result = (size >> 1) + size; // Multiply by 3/2 to allow for growth
    // boolean expr is equivalent to result >= 0 && result<MAXIMUM_CAPACITY
    return (result & ~(MAXIMUM_CAPACITY - 1)) == 0 ? result : MAXIMUM_CAPACITY;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  public V get(Object key) {
    if (key == null) {
      HashMapEntry<K, V> e = entryForNullKey;
      return e == null ? null : e.value;
    }
    int hash = secondaryHash(key);
    HashMapEntry<K, V>[] tab = table;
    for (HashMapEntry<K, V> e = tab[hash & (tab.length - 1)]; e != null; e = e.next) {
      K eKey = e.key;
      if (eKey == key || (e.hash == hash && equals(key, eKey))) {
        return e.value;
      }
    }
    return null;
  }

  @Override
  public boolean containsKey(Object key) {
    if (key == null) {
      return entryForNullKey != null;
    }
    int hash = secondaryHash(key);
    HashMapEntry<K, V>[] tab = table;
    for (HashMapEntry<K, V> e = tab[hash & (tab.length - 1)]; e != null; e = e.next) {
      K eKey = e.key;
      if (eKey == key || (e.hash == hash && equals(key, eKey))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    HashMapEntry[] tab = table;
    int len = tab.length;
    if (value == null) {
      for (int i = 0; i < len; i++) {
        for (HashMapEntry e = tab[i]; e != null; e = e.next) {
          if (e.value == null) {
            return true;
          }
        }
      }
      return entryForNullKey != null && entryForNullKey.value == null;
    }
    // value is non-null
    for (int i = 0; i < len; i++) {
      for (HashMapEntry e = tab[i]; e != null; e = e.next) {
        if (value.equals(e.value)) {
          return true;
        }
      }
    }
    return entryForNullKey != null && value.equals(entryForNullKey.value);
  }

  @Override
  public V put(K key, V value) {
    if (key == null) {
      return putValueForNullKey(value);
    }
    int hash = secondaryHash(key);
    HashMapEntry<K, V>[] tab = table;
    int index = hash & (tab.length - 1);
    for (HashMapEntry<K, V> e = tab[index]; e != null; e = e.next) {
      if (e.hash == hash && equals(key, e.key)) {
        V oldValue = e.value;
        e.value = value;
        return oldValue;
      }
    }
    // No entry for (non-null) key is present; create one
    modCount++;
    if (size++ > threshold) {
      tab = doubleCapacity();
      index = hash & (tab.length - 1);
    }
    addNewEntry(key, value, hash, index);
    return null;
  }

  private V putValueForNullKey(V value) {
    HashMapEntry<K, V> entry = entryForNullKey;
    if (entry == null) {
      addNewEntryForNullKey(value);
      size++;
      modCount++;
      return null;
    } else {
      V oldValue = entry.value;
      entry.value = value;
      return oldValue;
    }
  }

  private void constructorPut(K key, V value) {
    if (key == null) {
      HashMapEntry<K, V> entry = entryForNullKey;
      if (entry == null) {
        entryForNullKey = constructorNewEntry(null, value, 0, null);
        size++;
      } else {
        entry.value = value;
      }
      return;
    }
    int hash = secondaryHash(key);
    HashMapEntry<K, V>[] tab = table;
    int index = hash & (tab.length - 1);
    HashMapEntry<K, V> first = tab[index];
    for (HashMapEntry<K, V> e = first; e != null; e = e.next) {
      if (e.hash == hash && equals(key, e.key)) {
        e.value = value;
        return;
      }
    }
    // No entry for (non-null) key is present; create one
    tab[index] = constructorNewEntry(key, value, hash, first);
    size++;
  }

  /**
   * Creates a new entry for the given key, value, hash, and index and inserts it into the hash
   * table. This method is called by put (and indirectly, putAll), and overridden by LinkedHashMap.
   * The hash must incorporate the secondary hash function.
   */
  void addNewEntry(K key, V value, int hash, int index) {
    table[index] = new HashMapEntry<K, V>(key, value, hash, table[index]);
  }

  /**
   * Creates a new entry for the null key, and the given value and inserts it into the hash table.
   * This method is called by put (and indirectly, putAll), and overridden by LinkedHashMap.
   */
  void addNewEntryForNullKey(V value) {
    entryForNullKey = new HashMapEntry<K, V>(null, value, 0, null);
  }

  /**
   * Like newEntry, but does not perform any activity that would be unnecessary or inappropriate for
   * constructors. In this class, the two methods behave identically; in LinkedHashMap, they differ.
   */
  HashMapEntry<K, V> constructorNewEntry(K key, V value, int hash, HashMapEntry<K, V> first) {
    return new HashMapEntry<K, V>(key, value, hash, first);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    ensureCapacity(map.size());
    super.putAll(map);
  }

  /**
   * Ensures that the hash table has sufficient capacity to store the specified number of mappings,
   * with room to grow. If not, it increases the capacity as appropriate. Like doubleCapacity, this
   * method moves existing entries to new buckets as appropriate. Unlike doubleCapacity, this method
   * can grow the table by factors of 2^n for n > 1. Hopefully, a single call to this method will be
   * faster than multiple calls to doubleCapacity.
   *
   * <p>This method is called only by putAll.
   */
  private void ensureCapacity(int numMappings) {
    int newCapacity = roundUpToPowerOfTwo(capacityForInitSize(numMappings));
    HashMapEntry<K, V>[] oldTable = table;
    int oldCapacity = oldTable.length;
    if (newCapacity <= oldCapacity) {
      return;
    }
    if (newCapacity == oldCapacity * 2) {
      doubleCapacity();
      return;
    }
    // We're growing by at least 4x, rehash in the obvious way
    HashMapEntry<K, V>[] newTable = makeTable(newCapacity);
    if (size != 0) {
      int newMask = newCapacity - 1;
      for (int i = 0; i < oldCapacity; i++) {
        for (HashMapEntry<K, V> e = oldTable[i]; e != null; ) {
          HashMapEntry<K, V> oldNext = e.next;
          int newIndex = e.hash & newMask;
          HashMapEntry<K, V> newNext = newTable[newIndex];
          newTable[newIndex] = e;
          e.next = newNext;
          e = oldNext;
        }
      }
    }
  }

  /**
   * Allocate a table of the given capacity and set the threshold accordingly.
   *
   * @param newCapacity must be a power of two
   */
  private HashMapEntry<K, V>[] makeTable(int newCapacity) {
    @SuppressWarnings("unchecked")
    HashMapEntry<K, V>[] newTable = (HashMapEntry<K, V>[]) new HashMapEntry[newCapacity];
    table = newTable;
    threshold = (newCapacity >> 1) + (newCapacity >> 2); // 3/4 capacity
    return newTable;
  }

  /**
   * Doubles the capacity of the hash table. Existing entries are placed in the correct bucket on
   * the enlarged table. If the current capacity is, MAXIMUM_CAPACITY, this method is a no-op.
   * Returns the table, which will be new unless we were already at MAXIMUM_CAPACITY.
   */
  private HashMapEntry<K, V>[] doubleCapacity() {
    HashMapEntry<K, V>[] oldTable = table;
    int oldCapacity = oldTable.length;
    if (oldCapacity == MAXIMUM_CAPACITY) {
      return oldTable;
    }
    int newCapacity = oldCapacity * 2;
    HashMapEntry<K, V>[] newTable = makeTable(newCapacity);
    if (size == 0) {
      return newTable;
    }
    for (int j = 0; j < oldCapacity; j++) {
      /*
       * Rehash the bucket using the minimum number of field writes.
       * This is the most subtle and delicate code in the class.
       */
      HashMapEntry<K, V> e = oldTable[j];
      if (e == null) {
        continue;
      }
      int highBit = e.hash & oldCapacity;
      HashMapEntry<K, V> broken = null;
      newTable[j | highBit] = e;
      for (HashMapEntry<K, V> n = e.next; n != null; e = n, n = n.next) {
        int nextHighBit = n.hash & oldCapacity;
        if (nextHighBit != highBit) {
          if (broken == null) {
            newTable[j | nextHighBit] = n;
          } else {
            broken.next = n;
          }
          broken = e;
          highBit = nextHighBit;
        }
      }
      if (broken != null) {
        broken.next = null;
      }
    }
    return newTable;
  }

  @Override
  public V remove(Object key) {
    if (key == null) {
      return removeNullKey();
    }
    int hash = secondaryHash(key);
    HashMapEntry<K, V>[] tab = table;
    int index = hash & (tab.length - 1);
    for (HashMapEntry<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
      if (e.hash == hash && equals(key, e.key)) {
        if (prev == null) {
          tab[index] = e.next;
        } else {
          prev.next = e.next;
        }
        modCount++;
        size--;
        return e.value;
      }
    }
    return null;
  }

  private V removeNullKey() {
    HashMapEntry<K, V> e = entryForNullKey;
    if (e == null) {
      return null;
    }
    entryForNullKey = null;
    modCount++;
    size--;
    return e.value;
  }

  @Override
  public void clear() {
    if (size != 0) {
      Arrays.fill(table, null);
      entryForNullKey = null;
      modCount++;
      size = 0;
    }
  }


  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> es = entrySet;
    return (es != null) ? es : (entrySet = new EntrySet());
  }

  static class HashMapEntry<K, V> implements Entry<K, V> {
    final K key;
    V value;
    final int hash;
    HashMapEntry<K, V> next;

    HashMapEntry(K key, V value, int hash, HashMapEntry<K, V> next) {
      this.key = key;
      this.value = value;
      this.hash = hash;
      this.next = next;
    }

    public final K getKey() {
      return key;
    }

    public final V getValue() {
      return value;
    }

    public final V setValue(V value) {
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    @Override
    public final boolean equals(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      return Objects.equals(e.getKey(), key) && Objects.equals(e.getValue(), value);
    }

    @Override
    public final int hashCode() {
      return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }

    @Override
    public final String toString() {
      return key + "=" + value;
    }
  }

  private abstract class HashIterator {
    int nextIndex;
    HashMapEntry<K, V> nextEntry = entryForNullKey;
    HashMapEntry<K, V> lastEntryReturned;
    int expectedModCount = modCount;

    HashIterator() {
      if (nextEntry == null) {
        HashMapEntry<K, V>[] tab = table;
        HashMapEntry<K, V> next = null;
        while (next == null && nextIndex < tab.length) {
          next = tab[nextIndex++];
        }
        nextEntry = next;
      }
    }

    public boolean hasNext() {
      return nextEntry != null;
    }

    HashMapEntry<K, V> nextEntry() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      if (nextEntry == null) {
        throw new NoSuchElementException();
      }
      HashMapEntry<K, V> entryToReturn = nextEntry;
      HashMapEntry<K, V>[] tab = table;
      HashMapEntry<K, V> next = entryToReturn.next;
      while (next == null && nextIndex < tab.length) {
        next = tab[nextIndex++];
      }
      nextEntry = next;
      return lastEntryReturned = entryToReturn;
    }

    public void remove() {
      if (lastEntryReturned == null) {
        throw new IllegalStateException();
      }
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      AbstractHashMap.this.remove(lastEntryReturned.key);
      lastEntryReturned = null;
      expectedModCount = modCount;
    }
  }

  private final class KeyIterator extends HashIterator implements Iterator<K> {
    public K next() {
      return nextEntry().key;
    }
  }

  private final class ValueIterator extends HashIterator implements Iterator<V> {
    public V next() {
      return nextEntry().value;
    }
  }

  private final class EntryIterator extends HashIterator implements Iterator<Entry<K, V>> {
    public Entry<K, V> next() {
      return nextEntry();
    }
  }
  /** Returns true if this map contains the specified mapping. */
  private boolean containsMapping(Object key, Object value) {
    if (key == null) {
      HashMapEntry<K, V> e = entryForNullKey;
      return e != null && Objects.equals(value, e.value);
    }
    int hash = secondaryHash(key);
    HashMapEntry<K, V>[] tab = table;
    int index = hash & (tab.length - 1);
    for (HashMapEntry<K, V> e = tab[index]; e != null; e = e.next) {
      if (e.hash == hash && equals(key, e.key)) {
        return Objects.equals(value, e.value);
      }
    }
    return false; // No entry for key
  }

  /**
   * Removes the mapping from key to value and returns true if this mapping exists; otherwise,
   * returns does nothing and returns false.
   */
  private boolean removeMapping(Object key, Object value) {
    if (key == null) {
      HashMapEntry<K, V> e = entryForNullKey;
      if (e == null || !Objects.equals(value, e.value)) {
        return false;
      }
      entryForNullKey = null;
      modCount++;
      size--;
      return true;
    }
    int hash = secondaryHash(key);
    HashMapEntry<K, V>[] tab = table;
    int index = hash & (tab.length - 1);
    for (HashMapEntry<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
      if (e.hash == hash && equals(key, e.key)) {
        if (!Objects.equals(value, e.value)) {
          return false; // Map has wrong value for key
        }
        if (prev == null) {
          tab[index] = e.next;
        } else {
          prev.next = e.next;
        }
        modCount++;
        size--;
        return true;
      }
    }
    return false; // No entry for key
  }

  private final class EntrySet extends AbstractSet<Entry<K, V>> {
    public Iterator<Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    public boolean contains(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      return containsMapping(e.getKey(), e.getValue());
    }

    public boolean remove(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      return removeMapping(e.getKey(), e.getValue());
    }

    public int size() {
      return size;
    }

    public boolean isEmpty() {
      return size == 0;
    }

    public void clear() {
      AbstractHashMap.this.clear();
    }
  }
}
