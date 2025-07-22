/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/* @test
 * @summary ServerSocket(mptcp=true) creation semantics with MPTCP enabled/disabled
 * @requires os.family == "linux"
 * @run main/othervm TestMPTCP
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import jdk.net.ExtendedSocketOptions;

public class TestMPTCP {

    public static void main(String[] args) throws Exception {
        final InetAddress loop = InetAddress.getLoopbackAddress();

        // Positive path: enabled => can create and accept once.
        try (ServerSocket srv = new ServerSocket()) {
            srv.setOption(ExtendedSocketOptions.TCP_MPTCPIFY, true);
            srv.bind(new InetSocketAddress(loop, 0), 50);

            final int port = srv.getLocalPort();

            Thread client = new Thread(() -> {
                try (Socket c = new Socket()) {
                    c.setOption(ExtendedSocketOptions.TCP_MPTCPIFY, true);
                    c.connect(new InetSocketAddress(loop.getHostAddress(), port));
                    c.setSoTimeout(10_000);
                    c.getOutputStream().write(new byte[]{'h','e','l','l','o'});
                    c.getOutputStream().flush();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            client.start();

            try (Socket cli = srv.accept()) {
                cli.setSoTimeout(10_000);
                byte[] buf = cli.getInputStream().readNBytes(5);

                if (!"hello".equals(new String(buf))) {
                    throw new AssertionError("Echo mismatch");
                }
            }

            client.join(10_000);
            if(client.isAlive()) {
                throw new AssertionError("Client thread did not finish in time");
            }
        }
    }
}
