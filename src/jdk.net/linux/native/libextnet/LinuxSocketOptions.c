/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>

#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>

#include <jni.h>
#include <netinet/tcp.h>
#include <netinet/in.h>
#include "jni_util.h"
#include "jdk_net_LinuxSocketOptions.h"

#ifndef SO_INCOMING_NAPI_ID
#define SO_INCOMING_NAPI_ID    56
#endif

static void handleError(JNIEnv *env, jint rv, const char *errmsg) {
    if (rv < 0) {
        if (errno == ENOPROTOOPT) {
            JNU_ThrowByName(env, "java/lang/UnsupportedOperationException",
                    "unsupported socket option");
        } else {
            JNU_ThrowByNameWithLastError(env, "java/net/SocketException", errmsg);
        }
    }
}

static jint socketOptionSupported(jint level, jint optname) {
    jint one = 1;
    jint rv, s;
    socklen_t sz = sizeof (one);
    /* First try IPv6; fall back to IPv4. */
    s = socket(PF_INET6, SOCK_STREAM, IPPROTO_TCP);
    if (s < 0) {
        if (errno == EPFNOSUPPORT || errno == EAFNOSUPPORT) {
            s = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
        }
        if (s < 0) {
            return 0;
        }
    }
    rv = getsockopt(s, level, optname, (void *) &one, &sz);
    if (rv != 0 && errno == ENOPROTOOPT) {
        rv = 0;
    } else {
        rv = 1;
    }
    close(s);
    return rv;
}

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    setQuickAck
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdk_net_LinuxSocketOptions_setQuickAck0
(JNIEnv *env, jobject unused, jint fd, jboolean on) {
    int optval;
    int rv;
    optval = (on ? 1 : 0);
    rv = setsockopt(fd, IPPROTO_TCP, TCP_QUICKACK, &optval, sizeof (optval));
    handleError(env, rv, "set option TCP_QUICKACK failed");
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getQuickAck
 * Signature: (I)Z;
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_getQuickAck0
(JNIEnv *env, jobject unused, jint fd) {
    int on;
    socklen_t sz = sizeof (on);
    int rv = getsockopt(fd, IPPROTO_TCP, TCP_QUICKACK, &on, &sz);
    handleError(env, rv, "get option TCP_QUICKACK failed");
    return on != 0;
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    quickAckSupported
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_quickAckSupported0
(JNIEnv *env, jobject unused) {
    return socketOptionSupported(IPPROTO_TCP, TCP_QUICKACK);
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getSoPeerCred0
 * Signature: (I)L
 */
JNIEXPORT jlong JNICALL Java_jdk_net_LinuxSocketOptions_getSoPeerCred0
  (JNIEnv *env, jclass clazz, jint fd) {

    int rv;
    struct ucred cred;
    socklen_t len = sizeof(cred);

    if ((rv=getsockopt(fd, SOL_SOCKET, SO_PEERCRED, &cred, &len)) < 0) {
        handleError(env, rv, "get SO_PEERCRED failed");
    } else {
        if ((int)cred.uid == -1) {
            handleError(env, -1, "get SO_PEERCRED failed");
            cred.uid = cred.gid = -1;
        }
    }
    return (((jlong)cred.uid) << 32) | (cred.gid & 0xffffffffL);
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    keepAliveOptionsSupported0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_keepAliveOptionsSupported0
(JNIEnv *env, jobject unused) {
    return socketOptionSupported(SOL_TCP, TCP_KEEPIDLE) && socketOptionSupported(SOL_TCP, TCP_KEEPCNT)
            && socketOptionSupported(SOL_TCP, TCP_KEEPINTVL);
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    setTcpKeepAliveProbes0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdk_net_LinuxSocketOptions_setTcpKeepAliveProbes0
(JNIEnv *env, jobject unused, jint fd, jint optval) {
    jint rv = setsockopt(fd, SOL_TCP, TCP_KEEPCNT, &optval, sizeof (optval));
    handleError(env, rv, "set option TCP_KEEPCNT failed");
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    setTcpKeepAliveTime0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdk_net_LinuxSocketOptions_setTcpKeepAliveTime0
(JNIEnv *env, jobject unused, jint fd, jint optval) {
    jint rv = setsockopt(fd, SOL_TCP, TCP_KEEPIDLE, &optval, sizeof (optval));
    handleError(env, rv, "set option TCP_KEEPIDLE failed");
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    setTcpKeepAliveIntvl0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdk_net_LinuxSocketOptions_setTcpKeepAliveIntvl0
(JNIEnv *env, jobject unused, jint fd, jint optval) {
    jint rv = setsockopt(fd, SOL_TCP, TCP_KEEPINTVL, &optval, sizeof (optval));
    handleError(env, rv, "set option TCP_KEEPINTVL failed");
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getTcpKeepAliveProbes0
 * Signature: (I)I;
 */
JNIEXPORT jint JNICALL Java_jdk_net_LinuxSocketOptions_getTcpKeepAliveProbes0
(JNIEnv *env, jobject unused, jint fd) {
    jint optval, rv;
    socklen_t sz = sizeof (optval);
    rv = getsockopt(fd, SOL_TCP, TCP_KEEPCNT, &optval, &sz);
    handleError(env, rv, "get option TCP_KEEPCNT failed");
    return optval;
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getTcpKeepAliveTime0
 * Signature: (I)I;
 */
JNIEXPORT jint JNICALL Java_jdk_net_LinuxSocketOptions_getTcpKeepAliveTime0
(JNIEnv *env, jobject unused, jint fd) {
    jint optval, rv;
    socklen_t sz = sizeof (optval);
    rv = getsockopt(fd, SOL_TCP, TCP_KEEPIDLE, &optval, &sz);
    handleError(env, rv, "get option TCP_KEEPIDLE failed");
    return optval;
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getTcpKeepAliveIntvl0
 * Signature: (I)I;
 */
JNIEXPORT jint JNICALL Java_jdk_net_LinuxSocketOptions_getTcpKeepAliveIntvl0
(JNIEnv *env, jobject unused, jint fd) {
    jint optval, rv;
    socklen_t sz = sizeof (optval);
    rv = getsockopt(fd, SOL_TCP, TCP_KEEPINTVL, &optval, &sz);
    handleError(env, rv, "get option TCP_KEEPINTVL failed");
    return optval;
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    incomingNapiIdSupported0
 * Signature: ()Z;
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_incomingNapiIdSupported0
(JNIEnv *env, jobject unused) {
    return socketOptionSupported(SOL_SOCKET, SO_INCOMING_NAPI_ID);
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getIncomingNapiId0
 * Signature: (I)I;
 */
JNIEXPORT jint JNICALL Java_jdk_net_LinuxSocketOptions_getIncomingNapiId0
(JNIEnv *env, jobject unused, jint fd) {
    jint optval, rv;
    socklen_t sz = sizeof (optval);
    rv = getsockopt(fd, SOL_SOCKET, SO_INCOMING_NAPI_ID, &optval, &sz);
    handleError(env, rv, "get option SO_INCOMING_NAPI_ID failed");
    return optval;
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    setIpDontFragment0
 * Signature: (IZZ)V
 */
JNIEXPORT void JNICALL Java_jdk_net_LinuxSocketOptions_setIpDontFragment0
(JNIEnv *env, jobject unused, jint fd, jboolean optval, jboolean isIPv6) {
    jint rv, optsetting;

    optsetting = optval ? IP_PMTUDISC_DO : IP_PMTUDISC_DONT;

    if (!isIPv6) {
        rv = setsockopt(fd, IPPROTO_IP, IP_MTU_DISCOVER, &optsetting, sizeof (optsetting));
    } else {
        rv = setsockopt(fd, IPPROTO_IPV6, IPV6_MTU_DISCOVER, &optsetting, sizeof (optsetting));
    }
    handleError(env, rv, "set option IP_DONTFRAGMENT failed");
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getIpDontFragment0
 * Signature: (IZ)Z;
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_getIpDontFragment0
(JNIEnv *env, jobject unused, jint fd, jboolean isIPv6) {
    jint optlevel, optname, optval, rv;

    if (!isIPv6) {
        optlevel = IPPROTO_IP;
        optname = IP_MTU_DISCOVER;
    } else {
        optlevel = IPPROTO_IPV6;
        optname = IPV6_MTU_DISCOVER;
    }
    socklen_t sz = sizeof(optval);
    rv = getsockopt(fd, optlevel, optname, &optval, &sz);
    handleError(env, rv, "get option IP_DONTFRAGMENT failed");
    return optval == IP_PMTUDISC_DO ? JNI_TRUE : JNI_FALSE;
}

#ifndef SO_PROTOCOL
#define SO_PROTOCOL 38
#endif

static int read_int_file(const char* path, int* out) {
    char buf[32];
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return -1;
    ssize_t n = read(fd, buf, sizeof(buf) - 1);
    close(fd);
    if (n <= 0) return -1;
    buf[n] = '\0';
    *out = atoi(buf);
    return 0;
}

/* Class:     jdk_net_LinuxSocketOptions
 * Method:    isMptcpSupported0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_isMptcpSupported0
(JNIEnv *env, jclass clazz) {
    int v;
    if (read_int_file("/proc/sys/net/mptcp/enabled", &v) == 0) {
        return JNI_TRUE;
    }

    return JNI_FALSE;
}

/*
 * Class:    jdk_net_LinuxSocketOptions
 * Method:   setMptcpEnabled0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
Java_jdk_net_LinuxSocketOptions_setMptcpEnabled0(JNIEnv *env, jclass clazz,
                                                 jint fd, jboolean on) {
#ifdef IPPROTO_MPTCP
    if (!on) return;

    int domain, type, proto;
    int v6only, reuse;
    socklen_t len = sizeof(int);

    if (getsockopt(fd, SOL_SOCKET, SO_DOMAIN, &domain, &len) == -1) {
        JNU_ThrowByNameWithLastError(env, "java/net/SocketException", "getsockopt(SO_DOMAIN) failed");
        return;
    }

    if (domain != AF_INET && domain != AF_INET6) {
        JNU_ThrowByName(env, "java/net/SocketException", "unsupported socket domain");
        return;
    }

    if (getsockopt(fd, SOL_SOCKET, SO_TYPE, &type, &len) == -1) {
        JNU_ThrowByNameWithLastError(env, "java/net/SocketException", "getsockopt(SO_TYPE) failed");
        return;
    }

    if (type != SOCK_STREAM) {
        JNU_ThrowByName(env, "java/net/SocketException", "unsupported socket type");
        return;
    }

    if (getsockopt(fd, SOL_SOCKET, SO_PROTOCOL, &proto, &len) == -1) {
        JNU_ThrowByNameWithLastError(env, "java/net/SocketException", "getsockopt(SO_PROTOCOL) failed");
        return;
    }

    if (proto !=0 && proto != IPPROTO_TCP) {
        JNU_ThrowByName(env, "java/net/SocketException", "unsupported socket protocol");
        return;
    }

    int fdm = socket(domain, type, IPPROTO_MPTCP);
    if (fdm < 0) {
        JNU_ThrowByNameWithLastError(env, "java/net/SocketException", "socket(IPPROTO_MPTCP) failed");
        return;
    }

    if (domain == AF_INET6) {
        if (getsockopt(fd, IPPROTO_IPV6, IPV6_V6ONLY, &v6only, &len) == 0) {
            setsockopt(fdm, IPPROTO_IPV6, IPV6_V6ONLY, &v6only, len);
        }
    }

    if (getsockopt(fd, SOL_SOCKET, SO_REUSEPORT, &reuse, &len) == 0) {
        setsockopt(fdm, SOL_SOCKET, SO_REUSEPORT, &reuse, len);
    }

    if (dup2(fdm, fd) < 0) {
        JNU_ThrowByNameWithLastError(env, "java/net/SocketException", "dup2 failed");
        close(fdm);
        return;
    }
    if (close(fdm) < 0) {
        JNU_ThrowByNameWithLastError(env, "java/net/SocketException", "close failed");
        return;
    }
#else
    JNU_ThrowByName(env, "java/net/SocketException", "MPTCP not supported on this platform");
#endif
}

/*
 * Class:    jdk_net_LinuxSocketOptions
 * Method:   getMptcpEnabled0
 * Signature: (I)I
 */
JNIEXPORT jboolean JNICALL
Java_jdk_net_LinuxSocketOptions_getMptcpEnabled0(JNIEnv *env, jclass clazz, jint fd) {
#ifdef SO_PROTOCOL
    int proto = 0;
    socklen_t len = sizeof(int);
    if (getsockopt(fd, SOL_SOCKET, SO_PROTOCOL, &proto, &len) == -1) {
        JNU_ThrowByNameWithLastError(env, "java/net/SocketException", "getsockopt(SO_PROTOCOL) failed");
        return JNI_FALSE;
    }
#ifdef IPPROTO_MPTCP
    return (proto == IPPROTO_MPTCP) ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
#else
    return JNI_FALSE;
#endif
}
