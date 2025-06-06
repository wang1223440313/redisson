## Map
Java implementation of Valkey or Redis based [Map](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMap.html) object for Java implements [ConcurrentMap](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentMap.html) interface. This object is thread-safe. Consider to use [Live Object service](services.md/#live-object-service) to store POJO object as Valkey or Redis Map. Valkey or Redis uses serialized state to check key uniqueness instead of key's `hashCode()`/`equals()` methods.

If Map used mostly for read operations and/or network roundtrips are undesirable use Map with [Local cache](#eviction-local-cache-and-data-partitioning) support.

Code examples:

```java
RMap<String, SomeObject> map = redisson.getMap("anyMap");
SomeObject prevObject = map.put("123", new SomeObject());
SomeObject currentObject = map.putIfAbsent("323", new SomeObject());
SomeObject obj = map.remove("123");

// use fast* methods when previous value is not required
map.fastPut("a", new SomeObject());
map.fastPutIfAbsent("d", new SomeObject());
map.fastRemove("b");

RFuture<SomeObject> putAsyncFuture = map.putAsync("321");
RFuture<Void> fastPutAsyncFuture = map.fastPutAsync("321");

map.fastPutAsync("321", new SomeObject());
map.fastRemoveAsync("321");
```
RMap object allows to bind a [Lock](locks-and-synchronizers.md/#lock)/[ReadWriteLock](locks-and-synchronizers.md/#readwritelock)/[Semaphore](locks-and-synchronizers.md/#semaphore)/[CountDownLatch](locks-and-synchronizers.md/#countdownlatch) object per key:
```java
RMap<MyKey, MyValue> map = redisson.getMap("anyMap");
MyKey k = new MyKey();
RLock keyLock = map.getLock(k);
keyLock.lock();
try {
   MyValue v = map.get(k);
   // process value ...
} finally {
   keyLock.unlock();
}

RReadWriteLock rwLock = map.getReadWriteLock(k);
rwLock.readLock().lock();
try {
   MyValue v = map.get(k);
   // process value ...
} finally {
   keyLock.readLock().unlock();
}
```

### Eviction, local cache and data partitioning

Redisson provides various Map structure implementations with multiple important features:  

**local cache** - so called `near cache` used to speed up read operations and avoid network roundtrips. It caches Map entries on Redisson side and executes read operations up to **45x faster** in comparison with common implementation. Local cache instances with the same name connected to the same pub/sub channel. This channel is used for exchanging of update/invalidate events between all instances. Local cache store doesn't use `hashCode()`/`equals()` methods of key object, instead it uses hash of serialized state. It's recommended to use each local cached instance as a singleton per unique name since it has own state for local cache.

**data partitioning** - although any Map object is cluster compatible its content isn't scaled/partitioned across multiple master nodes in cluster. Data partitioning allows to scale available memory, read/write operations and entry eviction process for individual Map instance in cluster.  

**1. No eviction** 

Each object implements [RMap](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMap.html), [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapAsync.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapReactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapRx.html) interfaces.

Available implementations:

|RedissonClient<br/>method name | Local<br/>cache | Data<br/>partitioning | Ultra-fast<br/>read/write |
| ------------- | :-----------: | :-----------:| :---------:|
|getMap()<br/><sub><i>open-source version</i></sub> | ❌ | ❌ | ❌ |
|getLocalCachedMap()<br/><sub><i>open-source version</i></sub> | ✔️ | ❌ | ❌ |
|getMap()<br/><sub><i>[Redisson PRO](https://redisson.pro/feature-comparison.html) version</i></sub> | ❌ | ❌ | ✔️ |
|getLocalCachedMap()<br/><sub><i>[Redisson PRO](https://redisson.pro/feature-comparison.html) version</i></sub> | ✔️ | ❌ | ✔️ |
|getClusteredMap()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ❌ | ✔️ | ✔️ |
|getClusteredLocalCachedMap()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ✔️ | ✔️ | ✔️ |
<br/>

**2. Scripted eviction** 

Allows to define `time to live` or `max idle time` parameters per map entry. Eviction is done on Redisson side through a custom scheduled task which removes expired entries using Lua script. Eviction task is started once per unique object name at the moment of getting Map instance. If instance isn't used and has expired entries it should be get again to start the eviction process. This leads to extra Valkey or Redis calls and eviction task per unique map object name. 

Entries are cleaned time to time by `org.redisson.eviction.EvictionScheduler`. By default, it removes 100 expired entries at a time. This can be changed through [cleanUpKeysAmount](../configuration.md) setting. Task launch time tuned automatically and depends on expired entries amount deleted in previous time and varies between 5 second to 30 minutes by default. This time interval can be changed through [minCleanUpDelay](../configuration.md) and [maxCleanUpDelay](../configuration.md). For example, if clean task deletes 100 entries each time it will be executed every 5 seconds (minimum execution delay). But if current expired entries amount is lower than previous one then execution delay will be increased by 1.5 times and decreased otherwise.

Each object implements [RMapCache](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCache.html), [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheAsync.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheReactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheRx.html) interfaces.

Available implementations:

|RedissonClient<br/>method name | Local<br/>cache | Data<br/>partitioning | Ultra-fast<br/>read/write |
| ------------- | :-----------: | :-----------:| :---------:|
|getMapCache()<br/><sub><i>open-source version</i></sub> | ❌ | ❌ | ❌ |
|getMapCache()<br/><sub><i>[Redisson PRO](https://redisson.pro/feature-comparison.html) version</i></sub> | ❌ | ❌ | ✔️ |
|getLocalCachedMapCache()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ✔️ | ❌ | ✔️ |
|getClusteredMapCache()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ❌ | ✔️ | ✔️ |
|getClusteredLocalCachedMapCache()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ✔️ | ✔️ | ✔️ |
<br/>

**3. Advanced eviction**

Allows to define `time to live` parameter per map entry. Doesn't use an entry eviction task, entries are cleaned on Valkey or Redis side.

Each object implements [RMapCacheV2](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheV2.html), [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheV2Async.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheV2Reactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheV2Rx.html) interfaces.

Available implementations:

|RedissonClient<br/>method name | Local<br/>cache | Data<br/>partitioning | Ultra-fast<br/>read/write |
| ------------- | :-----------: | :-----------:| :---------:|
|getMapCacheV2()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ❌ | ✔️ | ✔️ |
|getLocalCachedMapCacheV2()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ✔️ | ✔️ | ✔️ |
<br/>

**4. Native eviction**

Allows to define `time to live` parameter per map entry. Doesn't use an entry eviction task, entries are cleaned on Redis side. Requires **Redis 7.4+**.

Each object implements [RMapCacheNative](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheNative.html), [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheNativeAsync.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheNativeReactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMapCacheNativeRx.html) interfaces.

Available implementations:

|RedissonClient<br/>method name | Local<br/>cache | Data<br/>partitioning | Ultra-fast<br/>read/write |
| ------------- | :-----------: | :-----------:| :---------:|
|getMapCacheNative()<br/><sub><i>open-source version</i></sub> | ❌ | ❌ | ❌ |
|getMapCacheNative()<br/><sub><i>[Redisson PRO](https://redisson.pro/feature-comparison.html) version</i></sub> | ❌ | ❌ | ✔️ |
|getLocalCachedMapCacheNative()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ✔️ | ❌ | ✔️ |
|getClusteredMapCacheNative()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ❌ | ✔️ | ✔️ |
<br/>

Redisson also provides various [Cache API](../cache-API-implementations.md) implementations.

Code example:
```java
RMapCache<String, SomeObject> map = redisson.getMapCache("anyMap");
// or
RMapCache<String, SomeObject> map = redisson.getMapCache("anyMap", MapCacheOptions.defaults());
// or
RMapCacheV2<String, SomeObject> map = redisson.getMapCacheV2("anyMap");
// or
RMapCacheV2<String, SomeObject> map = redisson.getMapCacheV2("anyMap", MapOptions.defaults());
// or
RMapCache<String, SomeObject> map = redisson.getClusteredMapCache("anyMap");
// or
RMapCache<String, SomeObject> map = redisson.getClusteredMapCache("anyMap", MapCacheOptions.defaults());


// ttl = 10 minutes, 
map.put("key1", new SomeObject(), 10, TimeUnit.MINUTES);
// ttl = 10 minutes, maxIdleTime = 10 seconds
map.put("key1", new SomeObject(), 10, TimeUnit.MINUTES, 10, TimeUnit.SECONDS);

// ttl = 3 seconds
map.putIfAbsent("key2", new SomeObject(), 3, TimeUnit.SECONDS);
// ttl = 40 seconds, maxIdleTime = 10 seconds
map.putIfAbsent("key2", new SomeObject(), 40, TimeUnit.SECONDS, 10, TimeUnit.SECONDS);

// if object is not used anymore
map.destroy();
```

**Local cache**  

Map object with local cache support implements [RLocalCachedMap](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RLocalCachedMap.html) or [RLocalCachedMapCache](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RLocalCachedMapCache.html) which extends [ConcurrentMap](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentMap.html) interface. This object is thread-safe.

Follow options can be supplied during object creation:
```java
      LocalCachedMapOptions options = LocalCachedMapOptions.defaults()

      // Defines whether to store a cache miss into the local cache.
      // Default value is false.
      .storeCacheMiss(false);

      // Defines store mode of cache data.
      // Follow options are available:
      // LOCALCACHE - store data in local cache only and use Valkey or Redis only for data update/invalidation.
      // LOCALCACHE_REDIS - store data in both Valkey or Redis and local cache.
      .storeMode(StoreMode.LOCALCACHE_REDIS)

      // Defines Cache provider used as local cache store.
      // Follow options are available:
      // REDISSON - uses Redisson own implementation
      // CAFFEINE - uses Caffeine implementation
      .cacheProvider(CacheProvider.REDISSON)

      // Defines local cache eviction policy.
      // Follow options are available:
      // LFU - Counts how often an item was requested. Those that are used least often are discarded first.
      // LRU - Discards the least recently used items first
      // SOFT - Uses soft references, entries are removed by GC
      // WEAK - Uses weak references, entries are removed by GC
      // NONE - No eviction
     .evictionPolicy(EvictionPolicy.NONE)

      // If cache size is 0 then local cache is unbounded.
     .cacheSize(1000)

      // Defines strategy for load missed local cache updates after connection failure.
      //
      // Follow reconnection strategies are available:
      // CLEAR - Clear local cache if map instance has been disconnected for a while.
      // LOAD - Store invalidated entry hash in invalidation log for 10 minutes
      //        Cache keys for stored invalidated entry hashes will be removed 
      //        if LocalCachedMap instance has been disconnected less than 10 minutes
      //        or whole cache will be cleaned otherwise.
      // NONE - Default. No reconnection handling
     .reconnectionStrategy(ReconnectionStrategy.NONE)

      // Defines local cache synchronization strategy.
      //
      // Follow sync strategies are available:
      // INVALIDATE - Default. Invalidate cache entry across all LocalCachedMap instances on map entry change
      // UPDATE - Insert/update cache entry across all LocalCachedMap instances on map entry change
      // NONE - No synchronizations on map changes
     .syncStrategy(SyncStrategy.INVALIDATE)

      // time to live for each entry in local cache
     .timeToLive(Duration.ofSeconds(10))

      // max idle time for each map entry in local cache
     .maxIdle(Duration.ofSeconds(10))

     // Defines how to listen expired event sent by Valkey or Redis upon this instance deletion
     //
     // Follow expiration policies are available:
     // DONT_SUBSCRIBE - Don't subscribe on expire event
     // SUBSCRIBE_WITH_KEYEVENT_PATTERN - Subscribe on expire event using `__keyevent@*:expired` pattern
     // SUBSCRIBE_WITH_KEYSPACE_CHANNEL - Subscribe on expire event using `__keyspace@N__:name` channel
     .expirationEventPolicy(ExpirationEventPolicy.SUBSCRIBE_WITH_KEYEVENT_PATTERN);
```

!!! warning

    It's recommended to use a single instance of local cached Map instance per unique name for each Redisson instance. Same `LocalCachedMapOptions` object should be used across all instances with the same name.

Code example:

```java
RLocalCachedMap<String, Integer> map = redisson.getLocalCachedMap("test", LocalCachedMapOptions.defaults());
// or
RLocalCachedMap<String, SomeObject> map = redisson.getLocalCachedMapCache("anyMap", LocalCachedMapCacheOptions.defaults());
// or
RLocalCachedMap<String, SomeObject> map = redisson.getClusteredLocalCachedMapCache("anyMap", LocalCachedMapCacheOptions.defaults());
// or
RLocalCachedMap<String, SomeObject> map = redisson.getClusteredLocalCachedMap("anyMap", LocalCachedMapOptions.defaults());

        
String prevObject = map.put("123", 1);
String currentObject = map.putIfAbsent("323", 2);
String obj = map.remove("123");

// use fast* methods when previous value is not required
map.fastPut("a", 1);
map.fastPutIfAbsent("d", 32);
map.fastRemove("b");

RFuture<String> putAsyncFuture = map.putAsync("321");
RFuture<Void> fastPutAsyncFuture = map.fastPutAsync("321");

map.fastPutAsync("321", new SomeObject());
map.fastRemoveAsync("321");
```

Object should be destroyed if it not used anymore, but it's not necessary to call destroy method if Redisson goes shutdown.
```java
RLocalCachedMap<String, Integer> map = ...
map.destroy();
```


**How to load data and avoid invalidation messages traffic.**

Code example:

```java
    public void loadData(String cacheName, Map<String, String> data) {
        RLocalCachedMap<String, String> clearMap = redisson.getLocalCachedMap(cacheName, 
                LocalCachedMapOptions.defaults().cacheSize(1).syncStrategy(SyncStrategy.INVALIDATE));
        RLocalCachedMap<String, String> loadMap = redisson.getLocalCachedMap(cacheName, 
                LocalCachedMapOptions.defaults().cacheSize(1).syncStrategy(SyncStrategy.NONE));
        
        loadMap.putAll(data);
        clearMap.clearLocalCache();
    }
```

**Data partitioning**

Map object with data partitioning support implements `org.redisson.api.RClusteredMap` which extends `java.util.concurrent.ConcurrentMap` interface. Read more details about data partitioning [here](data-partitioning.md).

Code example:

```java
RClusteredMap<String, SomeObject> map = redisson.getClusteredMap("anyMap");
// or
RClusteredMap<String, SomeObject> map = redisson.getClusteredLocalCachedMapCache("anyMap", LocalCachedMapCacheOptions.defaults());
// or
RClusteredMap<String, SomeObject> map = redisson.getClusteredLocalCachedMap("anyMap", LocalCachedMapOptions.defaults());
// or
RClusteredMap<String, SomeObject> map = redisson.getClusteredMapCache("anyMap");

SomeObject prevObject = map.put("123", new SomeObject());
SomeObject currentObject = map.putIfAbsent("323", new SomeObject());
SomeObject obj = map.remove("123");

map.fastPut("321", new SomeObject());
map.fastRemove("321");
```

### Persistence

Redisson allows to store Map data in external storage along with Valkey or Redis store.  
Use cases:

1. Redisson Map object as a cache between an application and external storage.
2. Increase durability of Redisson Map data and life-span of evicted entries.
3. Caching for databases, web services or any other data source.

**Read-through strategy**

If requested entry doesn't exist in the Redisson Map object 
when it will be loaded using provided [MapLoader](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/map/MapLoader.html) object. Code example:

```java
        MapLoader<String, String> mapLoader = new MapLoader<String, String>() {
            
            @Override
            public Iterable<String> loadAllKeys() {
                List<String> list = new ArrayList<String>();
                Statement statement = conn.createStatement();
                try {
                    ResultSet result = statement.executeQuery("SELECT id FROM student");
                    while (result.next()) {
                        list.add(result.getString(1));
                    }
                } finally {
                    statement.close();
                }

                return list;
            }
            
            @Override
            public String load(String key) {
                PreparedStatement preparedStatement = conn.prepareStatement("SELECT name FROM student where id = ?");
                try {
                    preparedStatement.setString(1, key);
                    ResultSet result = preparedStatement.executeQuery();
                    if (result.next()) {
                        return result.getString(1);
                    }
                    return null;
                } finally {
                    preparedStatement.close();
                }
            }
        };
```
Configuration example:
```java
MapOptions<K, V> options = MapOptions.<K, V>defaults()
                              .loader(mapLoader);

MapCacheOptions<K, V> mcoptions = MapCacheOptions.<K, V>defaults()
                              .loader(mapLoader);


RMap<K, V> map = redisson.getMap("test", options);
// or
RMapCache<K, V> map = redisson.getMapCache("test", mcoptions);
// or with performance boost up to 45x times 
RLocalCachedMap<K, V> map = redisson.getLocalCachedMap("test", options);
// or with performance boost up to 45x times 
RLocalCachedMapCache<K, V> map = redisson.getLocalCachedMapCache("test", mcoptions);
```

**Write-through (synchronous) strategy**

When the Map entry is being updated method won't return until 
Redisson update it in an external storage using [MapWriter](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/map/MapWriter.html) object. Code example:

```java
        MapWriter<String, String> mapWriter = new MapWriter<String, String>() {
            
            @Override
            public void write(Map<String, String> map) {
                PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO student (id, name) values (?, ?)");
                try {
                    for (Entry<String, String> entry : map.entrySet()) {
                        preparedStatement.setString(1, entry.getKey());
                        preparedStatement.setString(2, entry.getValue());
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                } finally {
                    preparedStatement.close();
                }
            }
            
            @Override
            public void delete(Collection<String> keys) {
                PreparedStatement preparedStatement = conn.prepareStatement("DELETE FROM student where id = ?");
                try {
                    for (String key : keys) {
                        preparedStatement.setString(1, key);
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                } finally {
                    preparedStatement.close();
                }
            }
        };
```
Configuration example:
```java
MapOptions<K, V> options = MapOptions.<K, V>defaults()
                              .writer(mapWriter)
                              .writeMode(WriteMode.WRITE_THROUGH);

MapCacheOptions<K, V> mcoptions = MapCacheOptions.<K, V>defaults()
                              .writer(mapWriter)
                              .writeMode(WriteMode.WRITE_THROUGH);


RMap<K, V> map = redisson.getMap("test", options);
// or
RMapCache<K, V> map = redisson.getMapCache("test", mcoptions);
// or with performance boost up to 45x times 
RLocalCachedMap<K, V> map = redisson.getLocalCachedMap("test", options);
// or with performance boost up to 45x times 
RLocalCachedMapCache<K, V> map = redisson.getLocalCachedMapCache("test", mcoptions);
```

**Write-behind (asynchronous) strategy**

Updates of Map object are accumulated in batches and asynchronously written with defined delay to external storage through [MapWriter](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/map/MapWriter.html) object.  
`writeBehindDelay` - delay of batched write or delete operation. Default value is 1000 milliseconds.
`writeBehindBatchSize` - size of batch. Each batch contains Map Entry write or delete commands. Default value is 50.

Configuration example:

```java
MapOptions<K, V> options = MapOptions.<K, V>defaults()
                              .writer(mapWriter)
                              .writeMode(WriteMode.WRITE_BEHIND)
                              .writeBehindDelay(5000)
                              .writeBehindBatchSize(100);

MapCacheOptions<K, V> mcoptions = MapCacheOptions.<K, V>defaults()
                              .writer(mapWriter)
                              .writeMode(WriteMode.WRITE_BEHIND)
                              .writeBehindDelay(5000)
                              .writeBehindBatchSize(100);


RMap<K, V> map = redisson.getMap("test", options);
// or
RMapCache<K, V> map = redisson.getMapCache("test", mcoptions);
// or with performance boost up to 45x times 
RLocalCachedMap<K, V> map = redisson.getLocalCachedMap("test", options);
// or with performance boost up to 45x times 
RLocalCachedMapCache<K, V> map = redisson.getLocalCachedMapCache("test", mcoptions);
```

This feature available for `RMap`, `RMapCache`, `RLocalCachedMap` and `RLocalCachedMapCache` objects.

Usage of `RLocalCachedMap` and `RLocalCachedMapCache` objects boost Valkey or Redis read-operations up to **45x times** and give almost instant speed for database, web service or any other data source.

### Listeners

Redisson allows binding listeners per `RMap` object. This requires the `notify-keyspace-events` setting to be enabled on Valkey or Redis side.

`RMap` object allows to track follow events over the data.

|Listener class name|Event description | Valkey or Redis<br/>`notify-keyspace-events` value|
|:--:|:--:|:--:|
|org.redisson.api.listener.TrackingListener|Entry created/removed/updated after read operation| - |
|org.redisson.api.listener.MapPutListener|Entry created/updated|Eh|
|org.redisson.api.listener.MapRemoveListener|Entry removed|Eh|
|org.redisson.api.ExpiredObjectListener|`RMap` object expired|Ex|
|org.redisson.api.DeletedObjectListener|`RMap` object deleted|Eg|

Usage examples:
```java
RMap<String, SomeObject> map = redisson.getMap("anyMap");

int listenerId = map.addListener(new DeletedObjectListener() {
     @Override
     public void onDeleted(String name) {
        // ...
     }
});

int listenerId = map.addListener(new ExpiredObjectListener() {
     @Override
     public void onExpired(String name) {
        // ...
     }
});

int listenerId = map.addListener(new MapPutListener() {
     @Override
     public void onPut(String name) {
        // ...
     }
});

int listenerId = map.addListener(new MapRemoveListener() {
     @Override
     public void onRemove(String name) {
        // ...
     }
});

map.removeListener(listenerId);
```

`RMapCache` object allows to track additional events over the data.

|Listener class name|Event description |
|:--:|:--:|
|org.redisson.api.map.event.EntryCreatedListener|Entry created|
|org.redisson.api.map.event.EntryExpiredListener|Entry expired|
|org.redisson.api.map.event.EntryRemovedListener|Entry removed|
|org.redisson.api.map.event.EntryUpdatedListener|Entry updated|

!!! note "Important" 
    For optimization purposes, RMapCache entry events are emitted only when there are registered listeners. This means that listener registration affects the internal map state.

Usage examples:

```java
RMapCache<String, SomeObject> map = redisson.getMapCache("anyMap");
// or
RMapCache<String, SomeObject> map = redisson.getLocalCachedMapCache(LocalCachedMapCacheOptions.name("anyMap"));
// or
RMapCache<String, SomeObject> map = redisson.getClusteredLocalCachedMapCache("anyMap", LocalCachedMapOptions.defaults());
// or
RMapCache<String, SomeObject> map = redisson.getClusteredMapCache("anyMap");


int listenerId = map.addListener(new EntryUpdatedListener<Integer, Integer>() {
     @Override
     public void onUpdated(EntryEvent<Integer, Integer> event) {
          event.getKey(); // key
          event.getValue() // new value
          event.getOldValue() // old value
          // ...
     }
});

int listenerId = map.addListener(new EntryCreatedListener<Integer, Integer>() {
     @Override
     public void onCreated(EntryEvent<Integer, Integer> event) {
          event.getKey(); // key
          event.getValue() // value
          // ...
     }
});

int listenerId = map.addListener(new EntryExpiredListener<Integer, Integer>() {
     @Override
     public void onExpired(EntryEvent<Integer, Integer> event) {
          event.getKey(); // key
          event.getValue() // value
          // ...
     }
});

int listenerId = map.addListener(new EntryRemovedListener<Integer, Integer>() {
     @Override
     public void onRemoved(EntryEvent<Integer, Integer> event) {
          event.getKey(); // key
          event.getValue() // value
          // ...
     }
});

map.removeListener(listenerId);
```

### LRU/LFU bounded Map
Map object which implements `RMapCache` interface could be bounded using [Least Recently Used (LRU)](https://en.wikipedia.org/wiki/Cache_replacement_policies#LRU) or [Least Frequently Used (LFU)](https://en.wikipedia.org/wiki/Least_frequently_used) order. Bounded Map allows to store map entries within defined limit and retire entries in defined order. 

Use cases: limited Valkey or Redis memory.

```java
RMapCache<String, SomeObject> map = redisson.getMapCache("anyMap");
// or
RMapCache<String, SomeObject> map = redisson.getMapCache("anyMap", MapCacheOptions.defaults());
// or
RMapCache<String, SomeObject> map = redisson.getLocalCachedMapCache("anyMap", LocalCachedMapOptions.defaults());
// or
RMapCache<String, SomeObject> map = redisson.getClusteredLocalCachedMapCache("anyMap", LocalCachedMapOptions.defaults());
// or
RMapCache<String, SomeObject> map = redisson.getClusteredMapCache("anyMap");
// or
RMapCache<String, SomeObject> map = redisson.getClusteredMapCache("anyMap", MapCacheOptions.defaults());


// tries to set limit map to 10 entries using LRU eviction algorithm
map.trySetMaxSize(10);
// ... using LFU eviction algorithm
map.trySetMaxSize(10, EvictionMode.LFU);

// set or change limit map to 10 entries using LRU eviction algorithm
map.setMaxSize(10);
// ... using LFU eviction algorithm
map.setMaxSize(10, EvictionMode.LFU);

map.put("1", "2");
map.put("3", "3", 1, TimeUnit.SECONDS);
```

## Multimap
Java implementation of Valkey or Redis based [Multimap](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMultimap.html) object for  allows to store multiple values per key. Keys amount limited to `4 294 967 295` elements. Valkey and Redis use serialized key state to its uniqueness instead of key's `hashCode()`/`equals()` methods. This object is thread-safe.

It has [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMultimapAsync.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMultimapReactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMultimapRx.html) interfaces.

### Set based Multimap
Set based Multimap doesn't allow duplications for values per key.
```java
RSetMultimap<SimpleKey, SimpleValue> map = redisson.getSetMultimap("myMultimap");
map.put(new SimpleKey("0"), new SimpleValue("1"));
map.put(new SimpleKey("0"), new SimpleValue("2"));
map.put(new SimpleKey("3"), new SimpleValue("4"));

Set<SimpleValue> allValues = map.get(new SimpleKey("0"));

List<SimpleValue> newValues = Arrays.asList(new SimpleValue("7"), new SimpleValue("6"), new SimpleValue("5"));
Set<SimpleValue> oldValues = map.replaceValues(new SimpleKey("0"), newValues);

Set<SimpleValue> removedValues = map.removeAll(new SimpleKey("0"));
```
### List based Multimap
List based [Multimap](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RMultimap.html) object for Java stores entries in insertion order and allows duplicates for values mapped to key.
```java
RListMultimap<SimpleKey, SimpleValue> map = redisson.getListMultimap("test1");
map.put(new SimpleKey("0"), new SimpleValue("1"));
map.put(new SimpleKey("0"), new SimpleValue("2"));
map.put(new SimpleKey("0"), new SimpleValue("1"));
map.put(new SimpleKey("3"), new SimpleValue("4"));

List<SimpleValue> allValues = map.get(new SimpleKey("0"));

Collection<SimpleValue> newValues = Arrays.asList(new SimpleValue("7"), new SimpleValue("6"), new SimpleValue("5"));
List<SimpleValue> oldValues = map.replaceValues(new SimpleKey("0"), newValues);

List<SimpleValue> removedValues = map.removeAll(new SimpleKey("0"));
```

### Eviction
Multimap entries eviction implemented by a separate MultimapCache object. There are [RSetMultimapCache](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSetMultimapCache.html) and [RListMultimapCache](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RListMultimapCache.html) objects for Set and List based Multimaps respectively.  

Eviction task is started once per unique object name at the moment of getting Multimap instance. If instance isn't used and has expired entries it should be get again to start the eviction process. This leads to extra Valkey or Redis calls and eviction task per unique map object name. 

Entries are cleaned time to time by `org.redisson.eviction.EvictionScheduler`. By default, it removes 100 expired entries at a time. This can be changed through [cleanUpKeysAmount](../configuration.md) setting. Task launch time tuned automatically and depends on expired entries amount deleted in previous time and varies between 5 second to 30 minutes by default. This time interval can be changed through [minCleanUpDelay](../configuration.md) and [maxCleanUpDelay](../configuration.md). For example, if clean task deletes 100 entries each time it will be executed every 5 seconds (minimum execution delay). But if current expired entries amount is lower than previous one then execution delay will be increased by 1.5 times and decreased otherwise.

Redis 7.4.0 and higher version implements native eviction. It's supported by [RSetMultimapCacheNative](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RSetMultimapCacheNative.html) and [RListMultimapCacheNative](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RListMultimapCacheNative.html) objects.

Code examples:

=== "Sync"
	```java
	// scripted eviction implementation
	RSetMultimapCache<String, String> multimap = redisson.getSetMultimapCache("myMultimap");

	// native eviction implementation
	RSetMultimapCacheNative<String, String> multimap = redisson.getSetMultimapCacheNative("myMultimap");

	multimap.put("1", "a");
	multimap.put("1", "b");
	multimap.put("1", "c");

	multimap.put("2", "e");
	multimap.put("2", "f");

	multimap.expireKey("2", 10, TimeUnit.MINUTES);

	// if object is not used anymore
	multimap.destroy();
	```
=== "Async"
	```java
	// scripted eviction implementation
	RSetMultimapCacheAsync<String, String> multimap = redisson.getSetMultimapCache("myMultimap");

	// native eviction implementation
	RSetMultimapCacheNativeAsync<String, String> multimap = redisson.getSetMultimapCacheNative("myMultimap");

	RFuture<Boolean> f1 = multimap.putAsync("1", "a");
	RFuture<Boolean> f2 = multimap.putAsync("1", "b");
	RFuture<Boolean> f3 = multimap.putAsync("1", "c");

	RFuture<Boolean> f4 = multimap.putAsync("2", "e");
	RFuture<Boolean> f5 = multimap.putAsync("2", "f");

	RFuture<Boolean> exfeature = multimap.expireKeyAsync("2", 10, TimeUnit.MINUTES);

	// if object is not used anymore
	multimap.destroy();
	```
=== "Reactive"
    ```java
	RedissonReactiveClient redissonReactive = redisson.reactive();
	
	// scripted eviction implementation
	RSetMultimapCacheReactive<String, String> multimap = redissonReactive.getSetMultimapCache("myMultimap");

	// native eviction implementation
	RSetMultimapCacheNativeReactive<String, String> multimap = redissonReactive.getSetMultimapCacheNative("myMultimap");
	
	Mono<Boolean> f1 = multimap.put("1", "a");
	Mono<Boolean> f2 = multimap.put("1", "b");
	Mono<Boolean> f3 = multimap.put("1", "c");

	Mono<Boolean> f4 = multimap.put("2", "e");
	Mono<Boolean> f5 = multimap.put("2", "f");

	Mono<Boolean> exfeature = multimap.expireKey("2", 10, TimeUnit.MINUTES);

	// if object is not used anymore
	multimap.destroy();
    ```
=== "RxJava3"
    ```java
	RedissonRxClient redissonRx = redisson.rxJava();
	
	// scripted eviction implementation
	RSetMultimapCacheRx<String, String> multimap = redissonReactive.getSetMultimapCache("myMultimap");

	// native eviction implementation
	RSetMultimapCacheNativeRx<String, String> multimap = redissonReactive.getSetMultimapCacheNative("myMultimap");
	
	Single<Boolean> f1 = multimap.put("1", "a");
	Single<Boolean> f2 = multimap.put("1", "b");
	Single<Boolean> f3 = multimap.put("1", "c");

	Single<Boolean> f4 = multimap.put("2", "e");
	Single<Boolean> f5 = multimap.put("2", "f");

	Single<Boolean> exfeature = multimap.expireKey("2", 10, TimeUnit.MINUTES);

	// if object is not used anymore
	multimap.destroy();
    ```


### Listeners

Redisson allows binding listeners per `RSetMultimap` or `RListMultimap` object. This requires the `notify-keyspace-events` setting to be enabled on Valkey or Redis side.

`RSetMultimap` listeners:

|Listener class name|Event description | Valkey or Redis<br/>`notify-keyspace-events` value|
|:--:|:--:|:--:|
|org.redisson.api.ExpiredObjectListener|`RSetMultimap` object expired| Ex|
|org.redisson.api.DeletedObjectListener|`RSetMultimap` object deleted| Eg|
|org.redisson.api.listener.SetAddListener|Element added to entry| Es|
|org.redisson.api.listener.SetRemoveListener|Element removed from entry| Es|
|org.redisson.api.listener.MapPutListener|Entry created|Eh|
|org.redisson.api.listener.MapRemoveListener|Entry removed|Eh|

`RListMultimap` listeners:

|Listener class name|Event description | Valkey or Redis<br/>`notify-keyspace-events` value|
|:--:|:--:|:--:|
|org.redisson.api.ExpiredObjectListener|`RListMultimap` object expired| Ex|
|org.redisson.api.DeletedObjectListener|`RListMultimap` object deleted| Eg|
|org.redisson.api.listener.ListAddListener|Element added to entry| Es|
|org.redisson.api.listener.ListRemoveListener|Element removed from entry| Es|
|org.redisson.api.listener.MapPutListener|Entry created|Eh|
|org.redisson.api.listener.MapRemoveListener|Entry removed|Eh|

Usage example:

```java
RListMultimap<Integer, Integer> lmap = redisson.getListMultimap("mymap");

int listenerId = lmap.addListener(new MapPutListener() {
     @Override
     public void onPut(String name) {
        // ...
     }
});

// ...

lmap.removeListener(listenerId);
```


## JSON Store

_This feature is available only in [Redisson PRO](https://redisson.pro/feature-comparison.html) edition._

[RJsonStore](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RJsonStore.html) is a distributed Key Value store for JSON objects. Compatible with Valkey and Redis. This object is thread-safe. Allows to store JSON value mapped by key. Operations can be executed per key or group of keys. Value is stored/retrieved using `JSON.*` commands. Both key and value are POJO objects. 

Allows to define `time to live` parameter per entry. Doesn't use an entry eviction task, entries are cleaned on Valkey or Redis side.

Code example of **[Async](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RJsonStoreAsync.html) interface** usage:

```java
RJsonStoreAsync<String, MyObject> store = redisson.getJsonStore("test", new JacksonCodec(MyObject.class));
```

Code example of **[Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RJsonStoreReactive.html) interface** usage:

```java
RedissonReactiveClient redisson = redissonClient.reactive();
RJsonStoreReactive<AnyObject> bucket = redisson.getJsonStore("anyObject", new JacksonCodec<>(AnyObject.class));
```

Code example of **[RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RJsonStoreRx.html) interface** usage:
```java
RedissonRxClient redisson = redissonClient.rxJava();
RJsonStoreRx<AnyObject> bucket = redisson.getJsonStore("anyObject", new JacksonCodec<>(AnyObject.class));
```

Data write code example:
```java
RJsonStore<String, MyObject> store = redisson.getJsonStore("test", new JacksonCodec(MyObject.class));

MyObject t1 = new MyObject();
t1.setName("name1");
MyObject t2 = new MyObject();
t2.setName("name2");

Map<String, MyObject> entries = new HashMap<>();
entries.put("1", t1);
entries.put("2", t2);

// multiple entries at once
store.set(entries);

// or set entry per call
store.set("1", t1);
store.set("2", t2);

// with ttl
store.set("1", t1, Duration.ofSeconds(100));

// set if not set previously
store.setIfAbsent("1", t1);

// set if entry already exists
store.setIfExists("1", t1);
```

Data read code example:
```java
RJsonStore<String, MyObject> store = redisson.getJsonStore("test", new JacksonCodec(MyObject.class));

// multiple entries at once
Map<String, MyObject> entries = store.get(Set.of("1", "2"));

// or read entry per call
MyObject value1 = store.get("1");
MyObject value2 = store.get("2");
```

Data deletion code example:
```java
RJsonStore<String, MyObject> store = redisson.getJsonStore("test", new JacksonCodec(MyObject.class));

// multiple entries at once
long deleted = store.delete(Set.of("1", "2"));

// or delete entry per call
boolean status = store.delete("1");
boolean status = store.delete("2");
```

Keys access code examples:
```java
RJsonStore<String, MyObject> store = redisson.getJsonStore("test", new JacksonCodec(MyObject.class));

// iterate keys
Set<String> keys = store.keySet();

// read all keys at once
Set<String> keys = store.readAllKeySet();
```

### Search by Object properties

For data searching, index prefix should be defined in `<object_name>:` format. For example for object name "test" prefix is "test:".

`StringCodec` should be used as object codec to enable searching by field.

Data search code example:
```java
RSearch s = redisson.getSearch();
s.createIndex("idx", IndexOptions.defaults()
                        .on(IndexType.JSON)
                        .prefix(Arrays.asList("test:")),
                    FieldIndex.text("name"));

RJsonStore<String, MyObject> store = redisson.getJsonStore("test", StringCodec.INSTANCE, new JacksonCodec(MyObject.class));

MyObject t1 = new MyObject();
t1.setName("name1");
MyObject t2 = new MyObject();
t2.setName("name2");

Map<String, MyObject> entries = new HashMap<>();
entries.put("1", t1);
entries.put("2", t2);
store.set(entries);

// search
SearchResult r = s.search("idx", "*", QueryOptions.defaults()
                                                  .returnAttributes(new ReturnAttribute("name")));

// aggregation
AggregationResult ar = s.aggregate("idx", "*", AggregationOptions.defaults()
                                                                 .withCursor().load("name"));
```

### Local Cache

Redisson provides [JSON Store](#json-store) implementation with local cache.

**local cache** - so called near cache used to speed up read operations and avoid network roundtrips. It caches JSON Store entries on Redisson side and executes read operations up to **45x faster** in comparison with regular implementation. Local cached instances with the same name are connected to the same pub/sub channel. This channel is used for exchanging of update/invalidate events between all instances. Local cache store doesn't use `hashCode()`/`equals()` methods of key object, instead it uses hash of serialized state. It's recommended to use each local cached instance as a singleton per unique name since it has own state for local cache.

!!! warning

    It's recommended to use a single instance of local cached JsonStore instance per unique name for each Redisson instance. Same `LocalCachedJsonStoreOptions` object should be used across all instances with the same name.

Follow options can be supplied during object creation:
```java
      LocalCachedJsonStoreOptions options = LocalCachedJsonStoreOptions.name("object_name_example")

      // Defines codec used for key
      .keyCodec(codec)

      // Defines codec used for JSON value
      .valueCodec(codec)

      // Defines whether to store a cache miss into the local cache.
      // Default value is false.
      .storeCacheMiss(false);

      // Defines store mode of cache data.
      // Follow options are available:
      // LOCALCACHE - store data in local cache only and use Valkey or Redis only for data update/invalidation.
      // LOCALCACHE_REDIS - store data in both Valkey or Redis and local cache.
      .storeMode(StoreMode.LOCALCACHE_REDIS)

      // Defines Cache provider used as local cache store.
      // Follow options are available:
      // REDISSON - uses Redisson own implementation
      // CAFFEINE - uses Caffeine implementation
      .cacheProvider(CacheProvider.REDISSON)

      // Defines local cache eviction policy.
      // Follow options are available:
      // LFU - Counts how often an item was requested. Those that are used least often are discarded first.
      // LRU - Discards the least recently used items first
      // SOFT - Uses soft references, entries are removed by GC
      // WEAK - Uses weak references, entries are removed by GC
      // NONE - No eviction
     .evictionPolicy(EvictionPolicy.NONE)

      // If cache size is 0 then local cache is unbounded.
     .cacheSize(1000)

      // Defines strategy for load missed local cache updates after connection failure.
      //
      // Follow reconnection strategies are available:
      // CLEAR - Clear local cache if map instance has been disconnected for a while.
      // NONE - Default. No reconnection handling
     .reconnectionStrategy(ReconnectionStrategy.NONE)

      // Defines local cache synchronization strategy.
      //
      // Follow sync strategies are available:
      // INVALIDATE - Default. Invalidate cache entry across all RLocalCachedJsonStore instances on map entry change
      // UPDATE - Insert/update cache entry across all RLocalCachedJsonStore instances on map entry change
      // NONE - No synchronizations on map changes
     .syncStrategy(SyncStrategy.INVALIDATE)

      // time to live for each entry in local cache
     .timeToLive(Duration.ofSeconds(10))

      // max idle time for each entry in local cache
     .maxIdle(Duration.ofSeconds(10));

     // Defines how to listen expired event sent by Valkey or Redis upon this instance deletion
     //
     // Follow expiration policies are available:
     // DONT_SUBSCRIBE - Don't subscribe on expire event
     // SUBSCRIBE_WITH_KEYEVENT_PATTERN - Subscribe on expire event using `__keyevent@*:expired` pattern
     // SUBSCRIBE_WITH_KEYSPACE_CHANNEL - Subscribe on expire event using `__keyspace@N__:name` channel
     .expirationEventPolicy(ExpirationEventPolicy.SUBSCRIBE_WITH_KEYEVENT_PATTERN)
```

Data write code example:
```java
LocalCachedJsonStoreOptions ops = LocalCachedJsonStoreOptions.name("test")
                .keyCodec(StringCodec.INSTANCE)
                .valueCodec(new JacksonCodec<>(MyObject.class));
RLocalCachedJsonStore<String, MyObject> store = redisson.getLocalCachedJsonStore(ops);

MyObject t1 = new MyObject();
t1.setName("name1");
MyObject t2 = new MyObject();
t2.setName("name2");

Map<String, MyObject> entries = new HashMap<>();
entries.put("1", t1);
entries.put("2", t2);

Map<String, MyObject> entries = new HashMap<>();
entries.put("1", t1);
entries.put("2", t2);

// multiple entries at once
store.set(entries);

// or set entry per call
store.set("1", t1);
store.set("2", t2);

// with ttl
store.set("1", t1, Duration.ofSeconds(100));

// set if not set previously
store.setIfAbsent("1", t1);

// set if entry already exists
store.setIfExists("1", t1);
```

Data read code example:
```java
LocalCachedJsonStoreOptions ops = LocalCachedJsonStoreOptions.name("test")
                .keyCodec(StringCodec.INSTANCE)
                .valueCodec(new JacksonCodec<>(MyObject.class));
RLocalCachedJsonStore<String, MyObject> store = redisson.getLocalCachedJsonStore(ops);

// multiple entries at once
Map<String, MyObject> entries = store.get(Set.of("1", "2"));

// or read entry per call
MyObject value1 = store.get("1");
MyObject value2 = store.get("2");
```

Data deletion code example:
```java
LocalCachedJsonStoreOptions ops = LocalCachedJsonStoreOptions.name("test")
                .keyCodec(StringCodec.INSTANCE)
                .valueCodec(new JacksonCodec<>(MyObject.class));
RLocalCachedJsonStore<String, MyObject> store = redisson.getLocalCachedJsonStore(ops);

// multiple entries at once
long deleted = store.delete(Set.of("1", "2"));

// or delete entry per call
boolean status = store.delete("1");
boolean status = store.delete("2");
```

## Set
Valkey or Redis based [Set](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSet.html) object for Java implements [Set](https://docs.oracle.com/javase/8/docs/api/java/util/Set.html) interface. This object is thread-safe. Keeps elements uniqueness via element state comparison. Set size limited to `4 294 967 295` elements. Valkey or Redis uses serialized state to check value uniqueness instead of value's `hashCode()`/`equals()` methods.

It has [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSetAsync.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSetReactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSetRx.html) interfaces.

```java
RSet<SomeObject> set = redisson.getSet("anySet");
set.add(new SomeObject());
set.remove(new SomeObject());
```
RSet object allows to bind a [Lock](locks-and-synchronizers.md/#lock)/[ReadWriteLock](locks-and-synchronizers.md/#readwritelock)/[Semaphore](locks-and-synchronizers.md/#semaphore)/[CountDownLatch](locks-and-synchronizers.md/#countdownlatch) object per value:
```
RSet<MyObject> set = redisson.getSet("anySet");
MyObject value = new MyObject();
RLock lock = map.getLock(value);
lock.lock();
try {
   // process value ...
} finally {
   lock.unlock();
}
```

### Eviction and data partitioning

Redisson provides various Set structure implementations with a few important features:  

**data partitioning** - although any Set object is cluster compatible its content isn't scaled/partitioned across multiple master nodes in cluster. Data partitioning allows to scale available memory, read/write operations and entry eviction process for individual Set instance in cluster.  

**entry eviction** - allows to define `time to live` parameter per SetCache entry. Valkey or Redis set structure doesn't support eviction thus it's done on Redisson side through a custom scheduled task which removes expired entries using Lua script. Eviction task is started once per unique object name at the moment of getting SetCache instance. If instance isn't used and has expired entries it should be get again to start the eviction process. This leads to extra Valkey or Redis calls and eviction task per unique SetCache object name. 

Entries are cleaned time to time by `org.redisson.eviction.EvictionScheduler`. By default, it removes 100 expired entries at a time. This can be changed through [cleanUpKeysAmount](../configuration.md) setting. Task launch time tuned automatically and depends on expired entries amount deleted in previous time and varies between 5 second to 30 minutes by default. This time interval can be changed through [minCleanUpDelay](../configuration.md) and [maxCleanUpDelay](../configuration.md). For example, if clean task deletes 100 entries each time it will be executed every 5 seconds (minimum execution delay). But if current expired entries amount is lower than previous one then execution delay will be increased by 1.5 times and decreased otherwise.

**advanced entry eviction** - improved version of the **entry eviction** process. Doesn't use an entry eviction task.

**Eviction**

Set object with eviction support implements [RSetCache](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSetCache.html),  [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSetCacheAsync.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSetCacheReactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSetCacheRx.html) interfaces.

Code example:
```java
RSetCache<SomeObject> set = redisson.getSetCache("mySet");
// or
RMapCache<SomeObject> set = redisson.getClusteredSetCache("mySet");

// ttl = 10 minutes, 
set.add(new SomeObject(), 10, TimeUnit.MINUTES);

// if object is not used anymore
map.destroy();
```

**Data partitioning**
Map object with data partitioning support implements `org.redisson.api.RClusteredSet`. Read more details about data partitioning [here](data-partitioning.md).

Code example:

```java
RClusteredSet<SomeObject> set = redisson.getClusteredSet("mySet");
// or
RClusteredSet<SomeObject> set = redisson.getClusteredSetCache("mySet");

// ttl = 10 minutes, 
map.add(new SomeObject(), 10, TimeUnit.MINUTES);
```

Below is the list of all available Set implementations:  

|RedissonClient <br/> method name | Data<br/>partitioning | Entry<br/>eviction | Advanced<br/>entry eviction | Ultra-fast<br/>read/write |
| ------------- | :----------:| :----------:| :----------:| :----------:|
|getSet()<br/><sub><i>open-source version</i></sub> | ❌ | ❌ | ❌ | ❌ |
|getSetCache()<br/><sub><i>open-source version</i></sub> | ❌ | ✔️ | ❌ | ❌ |
|getSet()<br/><sub><i>[Redisson PRO](https://redisson.pro/feature-comparison.html) version</i></sub> | ❌ | ❌ | ❌ | ✔️ |
|getSetCache()<br/><sub><i>[Redisson PRO](https://redisson.pro/feature-comparison.html) version</i></sub> | ❌ | ✔️ | ❌ | ✔️ |
|getSetCacheV2()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ✔️ | ❌ | ✔️ | ✔️ |
|getClusteredSet()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ✔️ | ❌ | ❌ | ✔️ |
|getClusteredSetCache()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ✔️ | ✔️ | ❌ | ✔️ |

### Listeners

Redisson allows binding listeners per `RSet` object. This requires the `notify-keyspace-events` setting to be enabled on Valkey or Redis side.

|Listener class name|Event description | Valkey or Redis<br/>`notify-keyspace-events` value|
|:--:|:--:|:--:|
|org.redisson.api.listener.TrackingListener|Element added/removed/updated after read operation| -|
|org.redisson.api.ExpiredObjectListener|`RSet` object expired| Ex|
|org.redisson.api.DeletedObjectListener|`RSet` object deleted| Eg|
|org.redisson.api.listener.SetAddListener|Element added| Es|
|org.redisson.api.listener.SetRemoveListener|Element removed| Es|
|org.redisson.api.listener.SetRemoveRandomListener|Element randomly removed|Es|

Usage example:

```java
RSet<String> set = redisson.getSet("anySet");

int listenerId = set.addListener(new DeletedObjectListener() {
     @Override
     public void onDeleted(String name) {
        // ...
     }
});

// ...

set.removeListener(listenerId);
```

## SortedSet
Valkey or Redis based distributed [SortedSet](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RSortedSet.html) for Java implements [SortedSet](https://docs.oracle.com/javase/8/docs/api/java/util/SortedSet.html) interface. This object is thread-safe. It uses comparator to sort elements and keep uniqueness. For String data type it's recommended to use [LexSortedSet](#lexsortedset) object due to performance gain.
```java
RSortedSet<Integer> set = redisson.getSortedSet("anySet");
set.trySetComparator(new MyComparator()); // set object comparator
set.add(3);
set.add(1);
set.add(2);

set.removeAsync(0);
set.addAsync(5);
```
## ScoredSortedSet
Valkey or Redis based distributed [ScoredSortedSet](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RScoredSortedSet.html) object. Sorts elements by score defined during element insertion. Keeps elements uniqueness via element state comparison. 

It has [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RScoredSortedSetAsync.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RScoredSortedSetReactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RScoredSortedSetRx.html) interfaces. Set size is limited to `4 294 967 295` elements.
```java
RScoredSortedSet<SomeObject> set = redisson.getScoredSortedSet("simple");

set.add(0.13, new SomeObject(a, b));
set.addAsync(0.251, new SomeObject(c, d));
set.add(0.302, new SomeObject(g, d));

set.pollFirst();
set.pollLast();

int index = set.rank(new SomeObject(g, d)); // get element index
Double score = set.getScore(new SomeObject(g, d)); // get element score
```

### Data partitioning

Although 'RScoredSortedSet' object is cluster compatible its content isn't scaled across multiple master nodes. `RScoredSortedSet` data partitioning available only in cluster mode and implemented by separate `RClusteredScoredSortedSet` object. Size is limited by whole Cluster memory. More about partitioning [here](data-partitioning.md).

Below is the list of all available `RScoredSortedSet` implementations:  

|RedissonClient <br/> method name | Data partitioning <br/> support | Ultra-fast read/write |
| ------------- | :----------:| :----------:|
|getScoredSortedSet()<br/><sub><i>open-source version</i></sub> | ❌ | ❌ |
|getScoredSortedSet()<br/><sub><i>[Redisson PRO](https://redisson.pro/feature-comparison.html) version</i></sub> | ❌ | ✔️ |
|getClusteredScoredSortedSet()<br/><sub><i>available only in [Redisson PRO](https://redisson.pro/feature-comparison.html)</i></sub> | ✔️ | ✔️ |

Code example:
```java
RClusteredScoredSortedSet set = redisson.getClusteredScoredSortedSet("simpleBitset");
set.add(1.1, "v1");
set.add(1.2, "v2");
set.add(1.3, "v3");

ScoredEntry<String> s = set.firstEntry();
ScoredEntry<String> e = set.pollFirstEntry();
```

### Listeners

Redisson allows binding listeners per `RScoredSortedSet` object. This requires the `notify-keyspace-events` setting to be enabled on Valkey or Redis side.

|Listener class name|Event description | Valkey or Redis<br/>`notify-keyspace-events` value|
|:--:|:--:|:--:|
|org.redisson.api.listener.TrackingListener|Element created/removed/updated after read operation| - |
|org.redisson.api.listener.ScoredSortedSetAddListener|Element created/updated|Ez|
|org.redisson.api.listener.ScoredSortedSetRemoveListener|Element removed|Ez|
|org.redisson.api.ExpiredObjectListener|`RScoredSortedSet` object expired|Ex|
|org.redisson.api.DeletedObjectListener|`RScoredSortedSet` object deleted|Eg|

Usage example:

```java
RScoredSortedSet<String> set = redisson.getScoredSortedSet("anySet");

int listenerId = set.addListener(new DeletedObjectListener() {
     @Override
     public void onDeleted(String name) {
        // ...
     }
});

// ...

set.removeListener(listenerId);
```

## LexSortedSet
Valkey or Redis based distributed [Set](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RLexSortedSet.html) object for Java allows String objects only and implements `java.util.Set<String>` interface. It keeps elements in lexicographical order and maintain elements uniqueness via element state comparison. 

It has [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RLexSortedSetAsync.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RLexSortedSetReactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RLexSortedSetRx.html) interfaces.
```java
RLexSortedSet set = redisson.getLexSortedSet("simple");
set.add("d");
set.addAsync("e");
set.add("f");

set.rangeTail("d", false);
set.countHead("e");
set.range("d", true, "z", false);
```

### Listeners

Redisson allows binding listeners per `RLexSortedSet` object. This requires the `notify-keyspace-events` setting to be enabled on Valkey or Redis side.

|Listener class name|Event description | Valkey or Redis<br/>`notify-keyspace-events` value|
|:--:|:--:|:--:|
|org.redisson.api.listener.TrackingListener|Element created/removed/updated after read operation|-|
|org.redisson.api.listener.ScoredSortedSetAddListener|Element created/updated|Ez|
|org.redisson.api.listener.ScoredSortedSetRemoveListener|Element removed|Ez|
|org.redisson.api.ExpiredObjectListener|`RScoredSortedSet` object expired|Ex|
|org.redisson.api.DeletedObjectListener|`RScoredSortedSet` object deleted|Eg|

Usage example:

```java
RLexSortedSet<String> set = redisson.getLexSortedSet("anySet");

int listenerId = set.addListener(new DeletedObjectListener() {
     @Override
     public void onDeleted(String name) {
        // ...
     }
});

// ...

set.removeListener(listenerId);
```

## List
Valkey or Redis based distributed [List](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RList.html) object for Java implements `java.util.List` interface. It keeps elements in insertion order. 

It has [Async](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RListAsync.html), [Reactive](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RListReactive.html) and [RxJava3](https://static.javadoc.io/org.redisson/redisson/latest/org/redisson/api/RListRx.html) interfaces. List size is limited to `4 294 967 295` elements.
```java
RList<SomeObject> list = redisson.getList("anyList");
list.add(new SomeObject());
list.get(0);
list.remove(new SomeObject());
```

### Listeners

Redisson allows binding listeners per `RList` object. This requires the `notify-keyspace-events` setting to be enabled on Valkey or Redis side.

|Listener class name|Event description | Valkey or Redis<br/>`notify-keyspace-events` value|
|:--:|:--:|:--:|
|org.redisson.api.listener.TrackingListener|Element created/removed/updated after read operation|-|
|org.redisson.api.listener.ListAddListener|Element created|El|
|org.redisson.api.listener.ListInsertListener|Element inserted|El|
|org.redisson.api.listener.ListSetListener|Element set/updated|El|
|org.redisson.api.listener.ListRemoveListener|Element removed|El|
|org.redisson.api.listener.ListTrimListener|List trimmed|El|
|org.redisson.api.ExpiredObjectListener|`RList` object expired|Ex|
|org.redisson.api.DeletedObjectListener|`RList` object deleted|Eg|

Usage example:

```java
RList<String> list = redisson.getList("anyList");

int listenerId = list.addListener(new DeletedObjectListener() {
     @Override
     public void onDeleted(String name) {
        // ...
     }
});

// ...

list.removeListener(listenerId);
```


## Time Series
Java implementation of Valkey or Redis based [TimeSeries](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RTimeSeries.html) object allows to store value by timestamp and define TTL(time-to-live) per entry. Values are ordered by timestamp. This object is thread-safe.  

Code example:
```java
RTimeSeries<String> ts = redisson.getTimeSeries("myTimeSeries");

ts.add(201908110501, "10%");
ts.add(201908110502, "30%");
ts.add(201908110504, "10%");
ts.add(201908110508, "75%");

// entry time-to-live is 10 hours
ts.add(201908110510, "85%", 10, TimeUnit.HOURS);
ts.add(201908110510, "95%", 10, TimeUnit.HOURS);

String value = ts.get(201908110508);
ts.remove(201908110508);

Collection<String> values = ts.pollFirst(2);
Collection<String> range = ts.range(201908110501, 201908110508);
```

Code example of **[Async interface](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RTimeSeriesAsync.html)** usage:
```java
RTimeSeries<String> ts = redisson.getTimeSeries("myTimeSeries");

RFuture<Void> future = ts.addAsync(201908110501, "10%");
RFuture<Void> future = ts.addAsync(201908110502, "30%");
RFuture<Void> future = ts.addAsync(201908110504, "10%");
RFuture<Void> future = ts.addAsync(201908110508, "75%");

// entry time-to-live is 10 hours
RFuture<Void> future = ts.addAsync(201908110510, "85%", 10, TimeUnit.HOURS);
RFuture<Void> future = ts.addAsync(201908110510, "95%", 10, TimeUnit.HOURS);

RFuture<String> future = ts.getAsync(201908110508);
RFuture<Boolean> future = ts.removeAsync(201908110508);

RFuture<Collection<String>> future = t.pollFirstAsync(2);
RFuture<Collection<String>> future = t.rangeAsync(201908110501, 201908110508);

future.whenComplete((res, exception) -> {
    // ...
});
```

Code example of **[Reactive interface](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RTimeSeriesReactive.html)** usage:
```java
RedissonReactiveClient redisson = redissonClient.reactive();
RTimeSeriesReactive<String> ts = redisson.getTimeSeries("myTimeSeries");

Mono<Void> mono = ts.add(201908110501, "10%");
Mono<Void> mono = ts.add(201908110502, "30%");
Mono<Void> mono = ts.add(201908110504, "10%");
Mono<Void> mono = ts.add(201908110508, "75%");

// entry time-to-live is 10 hours
Mono<Void> mono = ts.add(201908110510, "85%", 10, TimeUnit.HOURS);
Mono<Void> mono = ts.add(201908110510, "95%", 10, TimeUnit.HOURS);

Mono<String> mono = ts.get(201908110508);
Mono<Boolean> mono = ts.remove(201908110508);

Mono<Collection<String>> mono = ts.pollFirst(2);
Mono<Collection<String>> mono = ts.range(201908110501, 201908110508);

mono.doOnNext(res -> {
   // ...
}).subscribe();
```

Code example of **[RxJava3 interface](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RTimeSeriesRx.html)** usage:
```java
RedissonRxClient redisson = redissonClient.rxJava();
RTimeSeriesRx<String> ts = redisson.getTimeSeries("myTimeSeries");

Completable rx = ts.add(201908110501, "10%");
Completable rx = ts.add(201908110502, "30%");
Completable rx = ts.add(201908110504, "10%");
Completable rx = ts.add(201908110508, "75%");

// entry time-to-live is 10 hours
Completable rx = ts.add(201908110510, "85%", 10, TimeUnit.HOURS);
Completable rx = ts.add(201908110510, "95%", 10, TimeUnit.HOURS);

Maybe<String> rx = ts.get(201908110508);
Single<Boolean> rx = ts.remove(201908110508);

Single<Collection<String>> rx = ts.pollFirst(2);
Single<Collection<String>> rx = ts.range(201908110501, 201908110508);

rx.doOnSuccess(res -> {
   // ...
}).subscribe();
```

### Listeners

Redisson allows binding listeners per `RTimeSeries` object. This requires the `notify-keyspace-events` setting to be enabled on Valkey or Redis side.

|Listener class name|Event description | Valkey or Redis<br/>`notify-keyspace-events` value|
|:--:|:--:|:--:|
|org.redisson.api.listener.TrackingListener|Element created/removed/updated after read operation| - |
|org.redisson.api.listener.ScoredSortedSetAddListener|Element created/updated|Ez|
|org.redisson.api.listener.ScoredSortedSetRemoveListener|Element removed|Ez|
|org.redisson.api.ExpiredObjectListener|`RTimeSeries` object expired|Ex|
|org.redisson.api.DeletedObjectListener|`RTimeSeries` object deleted|Eg|

Usage example:

```java
RTimeSeries<String> set = redisson.getTimeSeries("obj");

int listenerId = set.addListener(new DeletedObjectListener() {
     @Override
     public void onDeleted(String name) {
        // ...
     }
});

// ...

set.removeListener(listenerId);
```

## Vector Set 

Java implementation of Valkey or Redis based [Vector Set](https://www.javadoc.io/doc/org.redisson/redisson/latest/org/redisson/api/RVectorSet.html) object is a specialized data type designed for managing high-dimensional vector data and enabling fast vector similarity search. Vector sets are similar to sorted sets but instead of a score, each element has a string representation of a vector, making them ideal for AI applications, machine learning models, and semantic search use cases.

Vector sets support the HNSW algorithm and use cosine similarity metrics for efficient vector similarity search. The data type is optimized for storing text embeddings and other high-dimensional vector representations commonly used in modern AI applications.

**Features**

The RVectorSet interfaces provide access to essential vector set operations including:

- Vector Addition: Adding elements with their associated vectors to the set

- Similarity Search: Retrieving elements most similar to a specified vector or existing element

- Attribute Management: Setting and retrieving JSON attributes associated with vector elements

- Filtered Search: Performing similarity searches with mathematical filters on element attributes

- Cardinality Operations: Getting the number of elements and vector dimensions

Usage examples:

=== "Sync"
    ```java
	RVectorSet vectorSet = redisson.getVectorSet("my-vectors");
	
	vectorSet.add(VectorAddArgs.element("element1").vector(1.0, 1.0));
	vectorSet.add(VectorAddArgs.element("element2").vector(-1.0, -1.0));

    List<Double> vector1 = vectorSet.getVector("element1");
	List<Double> vector2 = vectorSet.getVector("element2");

    List<String> similarElements = vectorSet.getSimilar(VectorSimilarArgs.vector(1.0, 1.0));

	vectorSet.remove("element1");
	vectorSet.remove("element2");

	```
=== "Async"
    ```java
	RVectorSetAsync vectorSet = redisson.getVectorSet("my-vectors").async();

	RFuture<Boolean> f1 = vectorSet.addAsync(VectorAddArgs.element("element1").vector(1.0, 1.0));
	RFuture<Boolean> f2 = vectorSet.addAsync(VectorAddArgs.element("element2").vector(-1.0, -1.0));

    RFuture<List<Double>> vector1 = vectorSet.getVectorAsync("element1");
	RFuture<List<Double>> vector2 = vectorSet.getVectorAsync("element2");

    RFuture<List<String>> similarElements = vectorSet.getSimilarAsync(VectorSimilarArgs.vector(1.0, 1.0));

	RFuture<Boolean> r1 = vectorSet.removeAsync("element1");
	RFuture<Boolean> r2 = vectorSet.removeAsync("element2");

	```
=== "Reactive"
    ```java
	RedissonReactiveClient redissonReactive = redisson.reactive();
	RVectorSetReactive vectorSet = redissonReactive.getVectorSet("my-vectors");

	Mono<Boolean> f1 = vectorSet.add(VectorAddArgs.element("element1").vector(1.0, 1.0));
	Mono<Boolean> f2 = vectorSet.add(VectorAddArgs.element("element2").vector(-1.0, -1.0));

    Mono<List<Double>> vector1 = vectorSet.getVector("element1");
	Mono<List<Double>> vector2 = vectorSet.getVector("element2");

    Mono<List<String>> similarElements = vectorSet.getSimilar(VectorSimilarArgs.vector(1.0, 1.0));
	
	Mono<Boolean> r1 = vectorSet.remove("element1");
	Mono<Boolean> r2 = vectorSet.remove("element2");
    ```
=== "RxJava3"
    ```java
	RedissonRxClient redissonRx = redisson.rxJava();
	RVectorSetRx<float[]> vectorSetRx = redissonRx.getVectorSet("myVectorSetRx");

	Single<Boolean> f1 = vectorSet.add(VectorAddArgs.element("element1").vector(1.0, 1.0));
	Single<Boolean> f2 = vectorSet.add(VectorAddArgs.element("element2").vector(-1.0, -1.0));

    Single<List<Double>> vector1 = vectorSet.getVector("element1");
	Single<List<Double>> vector2 = vectorSet.getVector("element2");

    Single<List<String>> similarElements = vectorSet.getSimilar(VectorSimilarArgs.vector(1.0, 1.0));
	
	Single<Boolean> r1 = vectorSet.remove("element1");
	Single<Boolean> r2 = vectorSet.remove("element2");
    ```
