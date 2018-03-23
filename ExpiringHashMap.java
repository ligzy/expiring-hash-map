import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;


public class ExpiringHashMap<K, V> implements Map<K, V> {

    private final HashMap<K, V> expired = new HashMap<>();
    private final HashMap<K, V> fingerTable = new HashMap<>();
    private final HashMap<K, Long> expirationMap = new HashMap<>();
    private final long timeToLive;

    public ExpiringHashMap(TimeUnit ttlUnit, long timeToLive) {
        this.timeToLive = ttlUnit.toNanos(timeToLive);
    }

    /********************************************************************************
     * Implementation of Map interface
     ********************************************************************************/

    @Override
    public int size() {
        this.removeAllExpired(now());
        return this.fingerTable.size();
    }

    @Override
    public boolean isEmpty() {
        removeAllExpired(now());
        return fingerTable.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        removeIfExpired(key, now());
        return fingerTable.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        removeAllExpired(now());
        return fingerTable.containsValue(value);
    }

    @Override
    public V get(Object key) {
        this.removeIfExpired(key, now());
        return this.fingerTable.get(key);
    }

    @Override
    public V put(K key, V value) {
        this.expirationMap.put(key, now());
        return this.fingerTable.put(key, value);
    }

    @Override
    public V remove(Object key) {
        expirationMap.remove(key);
        return fingerTable.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            this.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        expirationMap.clear();
        fingerTable.clear();
    }

    @Override
    public Set<K> keySet() {
        removeAllExpired(now());
        return unmodifiableSet(fingerTable.keySet());
    }

    @Override
    public Collection<V> values() {
        removeAllExpired(now());
        return unmodifiableCollection(fingerTable.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        removeAllExpired(now());
        return unmodifiableSet(fingerTable.entrySet());
    }
    
    /********************************************************************************
     * Methods for manipulating expired entries
     ********************************************************************************/

    public Set<K> expiredKeySet() {
        removeAllExpired(now());
        return this.expired.keySet();
    }

    public V expiredGet(final Object index) {
        return this.expired.get(index);
    }

    public int expiredSize() {
        removeAllExpired(now());
        return this.expired.size();
    }

    public boolean expiredIsEmpty() {
        removeAllExpired(now());
        return !this.expired.isEmpty();
    }

    public V expiredRemove(Integer index) {
        return this.expired.remove(index);
    }

    public void expiredClear() {
        this.expired.clear();
    }

    public Collection<V> expiredValues() {
        removeAllExpired(now());
        return this.expired.values();
    }

    public Set<java.util.Map.Entry<K, V>> expiredEntrySet() {
        removeAllExpired(now());
        return unmodifiableSet(this.expired.entrySet());
    }

    /********************************************************************************
     * Methods for internal use.
     ********************************************************************************/

    private boolean isExpired(final long now, final Long expirationTimeObject) {
        if (expirationTimeObject != null) {
            return now - expirationTimeObject.longValue() >= this.timeToLive;
        }
        return false;
    }

    private void removeAllExpired(final long now) {
        final Iterator<Entry<K, Long>> iterator = expirationMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<K, Long> expirationEntry = iterator.next();
            if (isExpired(now, expirationEntry.getValue())) {
                V value = this.fingerTable.remove(expirationEntry.getKey());
                this.expired.put(expirationEntry.getKey(), value);
                iterator.remove();
            }
        }
    }

    private void removeIfExpired(final Object key, final long now) {
        final Long expirationTime = this.expirationMap.get(key);
        if (isExpired(now, expirationTime)) {
            this.expired.put((K) key, this.fingerTable.get(key));
            this.fingerTable.remove(key);
        }
    }

    private long now() {
        return System.nanoTime();
    }
    
}
