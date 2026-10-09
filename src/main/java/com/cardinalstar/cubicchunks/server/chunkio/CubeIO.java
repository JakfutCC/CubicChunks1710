package com.cardinalstar.cubicchunks.server.chunkio;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;

import org.jetbrains.annotations.Nullable;

import com.cardinalstar.cubicchunks.CubicChunks;
import com.cardinalstar.cubicchunks.api.event.ColumnEvent;
import com.cardinalstar.cubicchunks.api.event.CubeEvent;
import com.cardinalstar.cubicchunks.api.world.storage.ICubicStorage;
import com.cardinalstar.cubicchunks.api.world.storage.ICubicStorage.PosBatch;
import com.cardinalstar.cubicchunks.async.TaskPool;
import com.cardinalstar.cubicchunks.async.TaskPool.ITaskExecutor;
import com.cardinalstar.cubicchunks.async.TaskPool.ITaskFuture;
import com.cardinalstar.cubicchunks.util.CubePos;
import com.cardinalstar.cubicchunks.util.DataUtils;
import com.cardinalstar.cubicchunks.world.cube.Cube;
import com.github.bsideup.jabel.Desugar;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public class CubeIO implements ICubeIO {

    private static final long EXPIRY = Duration.ofSeconds(120)
        .toMillis();

    private final ICubicStorage storage;
    private volatile IOException saveFailure;
    private final IPreloadFailureDelegate preloadFailures;
    private final ITaskExecutor<ChunkCoordIntPair, Optional<NBTTagCompound>> columnLoadExecutor;
    private final ITaskExecutor<CubePos, Optional<NBTTagCompound>> cubeLoadExecutor;
    private final ITaskExecutor<Savable, Void> saveExecutor;

    private final Object2ObjectLinkedOpenHashMap<ChunkCoordIntPair, SaveData> columnCache = new Object2ObjectLinkedOpenHashMap<>();

    private final Object2ObjectLinkedOpenHashMap<CubePos, SaveData> cubeCache = new Object2ObjectLinkedOpenHashMap<>();

    interface Savable {
    }

    private static final Savable RETRY_SAVES = new Savable() {};

    @Desugar
    private record SaveColumn(ChunkCoordIntPair pos, NBTTagCompound tag) implements Savable {}

    @Desugar
    private record SaveCube(CubePos pos, NBTTagCompound tag) implements Savable {}

    @NoArgsConstructor
    @AllArgsConstructor
    private static class SaveData {

        @Nullable
        public NBTTagCompound tag;
        @Nullable
        public Future<?> task;
        public long lastAccess;
    }

    public CubeIO(ICubicStorage storage, IPreloadFailureDelegate preloadFailures) {
        this.storage = storage;
        this.preloadFailures = preloadFailures;

        columnLoadExecutor = tasks -> {
            try {
                var result = storage
                    .readBatch(new PosBatch(DataUtils.mapToList(tasks, ITaskFuture::getTask), Collections.emptyList()));

                for (var task : tasks) {
                    NBTTagCompound tag = result.columns.get(task.getTask());

                    task.finish(Optional.ofNullable(tag));
                }
            } catch (IOException e) {
                for (var task : tasks) {
                    task.fail(e);
                }
            }
        };

        cubeLoadExecutor = tasks -> {
            try {
                var result = storage
                    .readBatch(new PosBatch(Collections.emptyList(), DataUtils.mapToList(tasks, ITaskFuture::getTask)));

                for (var task : tasks) {
                    NBTTagCompound tag = result.cubes.get(task.getTask());

                    task.finish(Optional.ofNullable(tag));
                }
            } catch (IOException e) {
                for (var task : tasks) {
                    task.fail(e);
                }
            }
        };

        saveExecutor = new ITaskExecutor<>() {

            // Only failed records are retained. Ordinary saves never rewrite an unrelated failed backlog.
            private final Map<ChunkCoordIntPair, NBTTagCompound> failedColumns = new Object2ObjectOpenHashMap<>();
            private final Map<CubePos, NBTTagCompound> failedCubes = new Object2ObjectOpenHashMap<>();

            @Override
            public boolean requiresSerialExecution() {
                return true;
            }

            private void savedColumn(ChunkCoordIntPair pos, NBTTagCompound tag) {
                failedColumns.remove(pos);
                synchronized (columnCache) {
                    SaveData cached = columnCache.get(pos);
                    if (cached != null && cached.tag == tag) cached.task = null;
                }
            }

            private void savedCube(CubePos pos, NBTTagCompound tag) {
                failedCubes.remove(pos);
                synchronized (cubeCache) {
                    SaveData cached = cubeCache.get(pos);
                    if (cached != null && cached.tag == tag) cached.task = null;
                }
            }

            private void writeRecords(Map<ChunkCoordIntPair, NBTTagCompound> columns,
                Map<CubePos, NBTTagCompound> cubes, boolean logFailure) {
                int count = columns.size() + cubes.size();
                if (count == 0) return;
                try {
                    storage.writeBatch(new ICubicStorage.NBTBatch(columns, cubes));
                    columns.forEach(this::savedColumn);
                    cubes.forEach(this::savedCube);
                } catch (IOException | RuntimeException e) {
                    if (logFailure)
                        CubicChunks.LOGGER.error("Could not save columns or cubes; isolating failed records", e);
                    if (count == 1) {
                        failedColumns.putAll(columns);
                        failedCubes.putAll(cubes);
                        saveFailure = new IOException("Could not save a cube or column", e);
                        return;
                    }
                    // Preserve batching for healthy regions rather than forcing each record separately.
                    Map<ChunkCoordIntPair, NBTTagCompound> firstColumns = new Object2ObjectOpenHashMap<>();
                    Map<ChunkCoordIntPair, NBTTagCompound> lastColumns = new Object2ObjectOpenHashMap<>();
                    Map<CubePos, NBTTagCompound> firstCubes = new Object2ObjectOpenHashMap<>();
                    Map<CubePos, NBTTagCompound> lastCubes = new Object2ObjectOpenHashMap<>();
                    int index = 0;
                    for (var entry : columns.entrySet()) {
                        (index++ < count / 2 ? firstColumns : lastColumns).put(entry.getKey(), entry.getValue());
                    }
                    for (var entry : cubes.entrySet()) {
                        (index++ < count / 2 ? firstCubes : lastCubes).put(entry.getKey(), entry.getValue());
                    }
                    writeRecords(firstColumns, firstCubes, false);
                    writeRecords(lastColumns, lastCubes, false);
                }
            }

            @Override
            public void execute(List<ITaskFuture<Savable, Void>> tasks) {
                Map<ChunkCoordIntPair, NBTTagCompound> columns = new Object2ObjectOpenHashMap<>();
                Map<CubePos, NBTTagCompound> cubes = new Object2ObjectOpenHashMap<>();
                boolean retry = false;
                for (ITaskFuture<Savable, Void> task : tasks) {
                    Savable savable = task.getTask();
                    if (savable instanceof SaveColumn column) columns.put(column.pos, column.tag);
                    if (savable instanceof SaveCube cube) cubes.put(cube.pos, cube.tag);
                    if (savable == RETRY_SAVES) retry = true;
                }
                Error fatal = null;
                try {
                    writeRecords(columns, cubes, true);
                    if (retry) {
                        // Only explicit flush retries an unrelated backlog, retaining batching where possible.
                        writeRecords(
                            new Object2ObjectOpenHashMap<>(failedColumns),
                            new Object2ObjectOpenHashMap<>(failedCubes),
                            true);
                    }
                } catch (Error e) {
                    failedColumns.putAll(columns);
                    failedCubes.putAll(cubes);
                    saveFailure = new IOException("Fatal error while saving columns or cubes", e);
                    fatal = e;
                }
                if (failedColumns.isEmpty() && failedCubes.isEmpty()) saveFailure = null;
                IOException flushFailure = null;
                if (retry) {
                    try {
                        // Force healthy records even if another record failed. Keep live region I/O on this
                        // worker: cancelling a backup must only interrupt its waiter, never a shared FileChannel.
                        storage.flush();
                    } catch (IOException | RuntimeException e) {
                        flushFailure = new IOException("Could not flush cube storage", e);
                    } catch (Error e) {
                        flushFailure = new IOException("Fatal error while flushing cube storage", e);
                        fatal = e;
                    }
                }
                for (ITaskFuture<Savable, Void> task : tasks) {
                    Savable savable = task.getTask();
                    boolean failed = (savable instanceof SaveColumn column && failedColumns.containsKey(column.pos))
                        || (savable instanceof SaveCube cube && failedCubes.containsKey(cube.pos))
                        || (savable == RETRY_SAVES && saveFailure != null);
                    if (failed) task.fail(saveFailure);
                    else if (savable == RETRY_SAVES && flushFailure != null) task.fail(flushFailure);
                    else task.finish(null);
                }
                if (fatal != null) throw fatal;
            }

            @Override
            public boolean canMerge(List<ITaskFuture<Savable, Void>> tasks, Savable savable) {
                return true;
            }
        };
    }

    @Override
    public boolean columnExists(ChunkCoordIntPair pos) {
        synchronized (columnCache) {
            SaveData data = columnCache.get(pos);

            if (data != null) return data.tag != null;
        }

        try {
            if (storage.columnExists(pos)) return true;
        } catch (IOException e) {
            CubicChunks.LOGGER.error("Could not check if column {} exists", pos, e);
        }

        return false;
    }

    @Override
    public boolean cubeExists(CubePos pos) {
        synchronized (cubeCache) {
            SaveData data = cubeCache.get(pos);

            if (data != null) return data.tag != null;
        }

        try {
            if (storage.cubeExists(pos)) return true;
        } catch (IOException e) {
            CubicChunks.LOGGER.error("Could not check if cube {} exists", pos, e);
        }

        return false;
    }

    @Override
    public NBTTagCompound loadColumn(ChunkCoordIntPair pos) throws LoadFailureException {
        synchronized (columnCache) {
            SaveData data = columnCache.get(pos);

            if (data != null) {
                return data.tag == null ? null : (NBTTagCompound) data.tag.copy();
            }
        }

        try {
            NBTTagCompound tag = storage.readColumn(pos);

            synchronized (columnCache) {
                SaveData cached = columnCache.get(pos);
                if (cached != null) {
                    return cached.tag == null ? null : (NBTTagCompound) cached.tag.copy();
                }

                if (tag == null) {
                    columnCache.put(pos, new SaveData(null, null, System.currentTimeMillis()));
                }
            }

            return tag;
        } catch (IOException e) {
            CubicChunks.LOGGER.error("Could not read column {}", pos, e);
            throw new LoadFailureException("Could not read column", e);
        }
    }

    @Override
    public NBTTagCompound loadCube(CubePos pos) throws LoadFailureException {
        synchronized (cubeCache) {
            SaveData data = cubeCache.get(pos);

            if (data != null) {
                return data.tag == null ? null : (NBTTagCompound) data.tag.copy();
            }
        }

        try {
            NBTTagCompound tag = storage.readCube(pos);

            synchronized (cubeCache) {
                SaveData cached = cubeCache.get(pos);
                if (cached != null) {
                    return cached.tag == null ? null : (NBTTagCompound) cached.tag.copy();
                }

                if (tag == null) {
                    cubeCache.put(pos, new SaveData(null, null, System.currentTimeMillis()));
                }
            }

            return tag;
        } catch (IOException e) {
            CubicChunks.LOGGER.error("Could not read cube {}", pos, e);
            throw new LoadFailureException("Could not read cube", e);
        }
    }

    public void saveColumn(ChunkCoordIntPair pos, Chunk column) {
        // NOTE: this function blocks the world thread
        // make it as fast as possible by offloading processing to the IO thread
        // except we have to write the NBT in this thread to avoid problems
        // with concurrent access to world data structures

        // add the column to the save queue
        NBTTagCompound tag = IONbtWriter.write(column);

        MinecraftForge.EVENT_BUS.post(new ColumnEvent.SaveNBT(column.worldObj, column, tag));

        column.isModified = false;

        Future<?> task = TaskPool.submit(saveExecutor, new SaveColumn(pos, tag));

        long now = System.currentTimeMillis();

        synchronized (columnCache) {
            columnCache.put(pos, new SaveData(tag, task, now));

            trimCache(columnCache, 5000, now);
        }
    }

    public void saveCube(CubePos pos, Cube cube) {
        cube.markSaved();

        NBTTagCompound tag = IONbtWriter.write(cube);

        MinecraftForge.EVENT_BUS.post(new CubeEvent.SaveNBT(cube.getWorld(), cube, tag));

        Future<?> task = TaskPool.submit(saveExecutor, new SaveCube(pos, tag));

        long now = System.currentTimeMillis();

        synchronized (cubeCache) {
            cubeCache.put(pos, new SaveData(tag, task, now));

            trimCache(cubeCache, 30000, now);
        }
    }

    private static <K> void trimCache(Object2ObjectLinkedOpenHashMap<K, SaveData> cache, int limit, long now) {
        // Rotate examined entries so failed or recently reused records cannot pin every healthy entry behind them.
        for (int checked = 0; cache.size() > limit && checked < 100; checked++) {
            K key = cache.firstKey();
            SaveData data = cache.getAndMoveToLast(key);
            if (data.task != null && completedSuccessfully(data.task)) data.task = null;
            if (data.task == null && data.lastAccess + EXPIRY < now) cache.remove(key);
        }
    }

    private static boolean completedSuccessfully(Future<?> task) {
        if (!task.isDone() || task.isCancelled()) return false;
        try {
            task.get();
            return true;
        } catch (ExecutionException e) {
            // Keep unsaved NBT available for a reload until a later save replaces this cache entry.
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            return false;
        }
    }

    @Override
    public void flush() throws IOException {
        // A serial marker drains this dimension's prior writes, without waiting for ongoing gameplay preloads.
        Future<?> barrier = TaskPool.submit(saveExecutor, RETRY_SAVES);
        try {
            barrier.get();
        } catch (ExecutionException e) {
            throw new IOException("A previous cube or column write or storage flush failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            InterruptedIOException interrupted = new InterruptedIOException("Interrupted while flushing cube saves");
            interrupted.initCause(e);
            throw interrupted;
        }
    }

    @Override
    public void close() throws IOException {
        // World unload runs on the server thread: finish outstanding reads before closing their storage too.
        TaskPool.flush();
        try {
            flush();
        } finally {
            this.storage.close();
        }
    }

    @Override
    public void preloadColumn(ChunkCoordIntPair pos) {
        synchronized (columnCache) {
            if (columnCache.containsKey(pos)) return;
        }

        TaskPool.submit(columnLoadExecutor, pos, tag -> {
            synchronized (columnCache) {
                if (columnCache.containsKey(pos)) {
                    return;
                }

                columnCache.put(pos, new SaveData(tag.orElse(null), null, System.currentTimeMillis()));
            }

            if (!tag.isPresent()) {
                if (preloadFailures != null) preloadFailures.onColumnPreloadFailed(pos);
            }
        });
    }

    @Override
    public void preloadCube(CubePos pos, CubeInitLevel wanted) {
        synchronized (cubeCache) {
            if (cubeCache.containsKey(pos)) return;
        }

        TaskPool.submit(cubeLoadExecutor, pos, tag -> {
            synchronized (cubeCache) {
                if (cubeCache.containsKey(pos)) {
                    return;
                }

                cubeCache.put(pos, new SaveData(tag.orElse(null), null, System.currentTimeMillis()));
            }

            CubeInitLevel actual = !tag.isPresent() ? CubeInitLevel.None : IONbtReader.getCubeInitLevel(tag.get());

            if (actual.ordinal() < wanted.ordinal()) {
                if (preloadFailures != null) {
                    preloadFailures.onCubePreloadFailed(pos, CubeInitLevel.None, wanted);
                }
            }
        });
    }
}
