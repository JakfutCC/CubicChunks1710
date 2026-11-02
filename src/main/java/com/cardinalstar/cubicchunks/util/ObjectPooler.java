package com.cardinalstar.cubicchunks.util;

import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/** Copied from gtnhlib, with some modifications */
public class ObjectPooler<T> {

    private final Supplier<T> instanceSupplier;
    private final ObjectArrayList<T> availableInstances;

    public ObjectPooler(Supplier<T> instanceSupplier) {
        this.instanceSupplier = instanceSupplier;
        this.availableInstances = new ObjectArrayList<>();
    }

    public final void clear() {
        this.availableInstances.clear();
    }

    public final T getInstance() {
        if (this.availableInstances.isEmpty()) {
            return this.instanceSupplier.get();
        }
        return this.availableInstances.pop();
    }
}
