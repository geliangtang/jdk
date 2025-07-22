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

#include <sys/types.h>
#include <sys/socket.h>
#include <errno.h>

#include "jni.h"
#include "jni_util.h"
#include "net_util.h"
#include "nio_util.h"

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Mptcp_mptcpify0(JNIEnv *env, jclass cl, jobject fdo)
{
    int ret = JNI_TRUE;
#if defined(__linux__) && defined(IPPROTO_MPTCP)
    int domain, type, protocol;
    int v6only, reuse;
    int fd = fdval(env, fdo);
    socklen_t len;
    int fdm;

    len = sizeof(domain);
    if (getsockopt(fd, SOL_SOCKET, SO_DOMAIN, &domain, &len) == -1) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "mptcpify",
                                     "getsockopt(SO_DOMAIN)");
        return JNI_FALSE;
    }

    if (domain != AF_INET && domain != AF_INET6) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "mptcpify",
                                     "unsupported socket domain");
        return JNI_FALSE;
    }

    len = sizeof(type);
    if (getsockopt(fd, SOL_SOCKET, SO_TYPE, &type, &len) == -1) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "mptcpify",
                                     "getsockopt(SO_TYPE)");
        return JNI_FALSE;
    }

    if (type != SOCK_STREAM) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "mptcpify",
                                     "unsupported socket type");
        return JNI_FALSE;
    }

    len = sizeof(protocol);
    if (getsockopt(fd, SOL_SOCKET, SO_PROTOCOL, &protocol, &len) == -1) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "mptcpify",
                                     "getsockopt(SO_PROTOCOL)");
        return JNI_FALSE;
    }

    if (protocol != 0 && protocol != IPPROTO_TCP) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "mptcpify",
                                     "unsupported socket protocol");
        return JNI_FALSE;
    }

    fdm = socket(domain, type, IPPROTO_MPTCP);
    if (fdm < 0) {
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "mptcpify",
                                     "socket(MPTCP)");
        return JNI_FALSE;
    }

    if (domain == AF_INET6 && ipv4_available()) {
        len = sizeof(v6only);
        if (getsockopt(fd, IPPROTO_IPV6, IPV6_V6ONLY, (char*)&v6only, &len) == 0)
            setsockopt(fdm, IPPROTO_IPV6, IPV6_V6ONLY, (char*)&v6only, len);
    }

    len = sizeof(reuse);
    if (getsockopt(fd, SOL_SOCKET, SO_REUSEPORT, (char*)&reuse, &len) == 0)
        setsockopt(fdm, SOL_SOCKET, SO_REUSEPORT, (char*)&reuse, len);

    if (dup2(fdm, fd) < 0) {
        ret = JNI_FALSE;
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "mptcpify",
                                     "dup2");
    }

    if (close(fdm) < 0) {
        ret = JNI_FALSE;
        JNU_ThrowByNameWithLastError(env, JNU_JAVANETPKG "mptcpify",
                                     "close");
    }
#endif

    return ret;
}
