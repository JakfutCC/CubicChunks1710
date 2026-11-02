/*
 * This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 * Copyright (c) 2015-2021 OpenCubicChunks
 * Copyright (c) 2015-2021 contributors
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cardinalstar.cubicchunks.api.util;

import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.joml.Vector3i;
import org.joml.Vector3ic;

import com.google.common.collect.AbstractIterator;

@ParametersAreNonnullByDefault
public class Box implements Iterable<Vector3ic> {

    protected int x1, y1, z1;
    protected int x2, y2, z2;

    public Box(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
    }

    public Box(int x, int y, int z, int radius) {
        this(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getZ1() {
        return z1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getZ2() {
        return z2;
    }

    @Override
    public @Nonnull Iterator<Vector3ic> iterator() {
        return new AbstractIterator<>() {

            private final Vector3i v = new Vector3i();

            private int x = x1;
            private int y = y1;
            private int z = z1;

            @Override
            protected Vector3ic computeNext() {
                if (x > x2) {
                    x = x1;
                    y++;
                }
                if (y > y2) {
                    y = y1;
                    z++;
                }
                if (z > z2) {
                    this.endOfData();
                    return null;
                }
                v.set(x, y, z);
                x++;
                return v;
            }
        };
    }

    public static Box horizontalChunkSlice(int startY, int heightY) {
        return new Box(0, startY, 0, 15, startY + heightY, 15);
    }
}
