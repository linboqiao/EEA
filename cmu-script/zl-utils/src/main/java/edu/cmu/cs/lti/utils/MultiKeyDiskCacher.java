package edu.cmu.cs.lti.utils;

import com.google.common.base.Joiner;
import com.google.common.cache.*;
import edu.cmu.cs.lti.exception.CacheException;
import edu.cmu.cs.lti.exception.CacheMissException;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A disk-backed cacher based on Guava's {@link com.google.common.cache.LoadingCache}.
 * <p>
 * Each key is a list of strings, which jointly identify the object (you can also use one string). Values are any
 * valid objects. Cache objects are discarded based on the weigher (how much a key, value pair weight). When the
 * object is discarded, it will be written to the cache directory, the keys will form the subfolder hierarchy.
 * <p>
 * Cache lookup will be two level, first level is the in memory lookup, then disk lookup. Null value will be return
 * if the result is not found. Users are responsible to insert key, value pair into the cache.
 * <p>
 * Upon close, the cacher will decide whether to write down all caches to disk or discard the whole cache (including
 * the directory) entirely based on discardAfter flag during construction.
 *
 * @author Zhengzhong Liu
 */
public class MultiKeyDiskCacher<T extends Serializable> {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private File cachingDirectory;

    private LoadingCache<List<String>, T> cache;

    private final boolean discardAfter;

    private final boolean saveCaches;

    private AtomicInteger writeOutCounter;

    /**
     * @param cachePath      The main directory to store the disk based copy of the cache.
     * @param weigher        The weighting method of a key, object pair.
     * @param weightCapacity The maximum weight capacity this cache can hold.
     * @param discardAfter   Whether to discard the whole cache at {@link MultiKeyDiskCacher#close()}.
     *                       Note that this cannot be true when the specified cachingDirectory is not empty to
     *                       avoid accidental deletion of non-caching files.
     * @throws IOException
     * @throws CacheException Throw when cannot start the cache.
     */
    public MultiKeyDiskCacher(String cachePath, Weigher<List<String>, T> weigher, long weightCapacity,
                              boolean discardAfter) throws IOException, CacheException {
        this(cachePath, weigher, weightCapacity, discardAfter, "cache");
    }

    /**
     * @param cachePath      The main directory to store the disk based copy of the cache.
     * @param weigher        The weighting method of a key, object pair.
     * @param weightCapacity The maximum weight capacity this cache can hold.
     * @param discardAfter   Whether to discard the whole cache at {@link MultiKeyDiskCacher#close()}.
     *                       Note that this cannot be true when the specified cachingDirectory is not empty to
     *                       avoid accidental deletion of non-caching files.
     * @param cacheSubfolder We will always create a sub-folder to store to the cached files.
     * @throws IOException
     * @throws CacheException Throw when cannot start the cache.
     */
    public MultiKeyDiskCacher(String cachePath, Weigher<List<String>, T> weigher, long weightCapacity,
                              boolean discardAfter, String cacheSubfolder) throws IOException, CacheException {
        this.cachingDirectory = FileUtils.joinPathsAsFile(cachePath, cacheSubfolder);
        FileUtils.ensureDirectory(cachingDirectory);

        logger.info("Cacher initialized at " + cachePath);

        this.saveCaches = !discardAfter;
        if (discardAfter) {
            logger.info("Cache configured to be deleted after the process.");
        } else {
            logger.info("Cache will be retained after the process.");
        }

        if (cachingDirectory.list().length > 0) {
            if (discardAfter) {
                logger.warn("Cache directory is not empty, not safe to discard after use, will not delete the dir.");
                discardAfter = false;
            }
            logger.warn("Starting the cache with existing cache directory.");
        }

        CacheLoader<List<String>, T> diskBackedLoader = new CacheLoader<List<String>, T>() {
            @Override
            public T load(List<String> key) throws FileNotFoundException, CacheMissException {
                T v = loadCachedObjectFromDisk(key);
                if (v != null) {
                    return v;
                } else {
                    throw new CacheMissException("Cannot find key: " + key);
                }
            }
        };

        RemovalListener<List<String>, T> removalListener = notification -> {
            writeOutCounter.incrementAndGet();
            try {
                writeCacheObject(notification.getValue(), notification.getKey());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        };

        this.cache = CacheBuilder.newBuilder().maximumWeight(weightCapacity)
                .weigher(weigher)
                .removalListener(removalListener)
                .build(diskBackedLoader);

        this.discardAfter = discardAfter;

        this.writeOutCounter = new AtomicInteger();
    }

    /**
     * Add key value to this method.
     *
     * @param cacheValue The cache object/value.
     * @param keys       The keys (ordered), you can specify multiple ones here and they will jointly specify the key.
     */
    public void addWithMultiKey(T cacheValue, String... keys) {
        cache.put(Arrays.asList(keys), cacheValue);
    }

    /**
     * Get the value based on the keys.
     *
     * @param keys The keys used to retrieve the value. The ordering of the keys matters.
     * @return
     */
    public T get(String... keys) {
        try {
            return cache.get(Arrays.asList(keys));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof CacheMissException)) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private T loadCachedObjectFromDisk(List<String> keys) throws FileNotFoundException {
        File cachedFile = getCacheFile(keys);
        if (cachedFile.exists()) {
            return (T) SerializationUtils.deserialize(new FileInputStream(cachedFile));
        } else {
            return null;
        }
    }

    private void writeCacheObject(T object, List<String> keys) throws FileNotFoundException {
        File cacheFile = getCacheFile(keys);
        if (!cacheFile.exists()) {
            SerializationUtils.serialize(object, new FileOutputStream(cacheFile));
        }
    }

    private File getCacheFile(List<String> keys) {
        return new File(cachingDirectory, Joiner.on("_").join(keys));
    }

    /**
     * Actions to take when you don't need the cacher. It will discard the cache directory entirely if discardAfter
     * flag is true. Otherwise all objects will be written to disk.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (discardAfter) {
            logger.info("Invalidating cache at " + cachingDirectory.getAbsolutePath());
            org.apache.commons.io.FileUtils.deleteDirectory(cachingDirectory);
        }

        if (saveCaches) {
            logger.info("Writing caches to disk.");
            for (Map.Entry<List<String>, T> entry : cache.asMap().entrySet()) {
                writeCacheObject(entry.getValue(), entry.getKey());
            }
        }

        logger.info(String.format("The Cacher write %d times to the following path [%s]", writeOutCounter.get(),
                cachingDirectory.getPath()));
    }

    /**
     * A small test example
     *
     * @param argv The command line argument, not used;
     * @throws IOException
     * @throws CacheException
     */
    public static void main(String[] argv) throws IOException, CacheException {
        MultiKeyDiskCacher<String> testCacher = new MultiKeyDiskCacher<>("data/temp",
                (k, v) -> 1, 10, false);
        String value = testCacher.get("k1");
        if (value == null) {
            testCacher.addWithMultiKey("v1", "k1");
        }

        value = testCacher.get("k2");
        if (value == null) {
            testCacher.addWithMultiKey("v2", "k2");
        }

        value = testCacher.get("k1");
        if (value == null) {
            testCacher.addWithMultiKey("v1", "k1");
        }

        testCacher.close();
    }
}
