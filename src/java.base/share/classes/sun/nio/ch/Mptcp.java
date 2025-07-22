/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.nio.ch;

import sun.nio.ch.Net;
import java.io.FileDescriptor;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.JavaIOFileDescriptorAccess;
import jdk.internal.util.OperatingSystem;

/**
 * This class defines methods for MPTCP sockets.
 */

public final class Mptcp {
    private static final boolean isSupported = OperatingSystem.isLinux();
    private static final JavaIOFileDescriptorAccess fdAccess =
        SharedSecrets.getJavaIOFileDescriptorAccess();

    private Mptcp() { }

    /**
     * Enable MPTCP
     */
    public static boolean mptcpify(FileDescriptor fd)
    {
        if (!isSupported)
            throw new UnsupportedOperationException("MPTCP not supported");
        return mptcpify0(fd);
    }

    private static native boolean mptcpify0(FileDescriptor fd);

    static {
        jdk.internal.loader.BootLoader.loadLibrary("net");
    }
}
