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

import java.io.*;
import java.net.*;

public class MPTCPClient {
    public static void main(String[] args) {
        final String HOST = "localhost";
        final int PORT = 12345;
        
        try (Socket socket = new Socket(HOST, PORT, true, true)) {
            System.out.println("Connected to server");
            
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true);
            
            BufferedReader stdIn = new BufferedReader(
                new InputStreamReader(System.in));
            
            String userInput;
            System.out.println("Enter message (type 'exit' to quit):");
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                
                System.out.println("Server response: " + in.readLine());
                
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
            }
            
            System.out.println("Connection closed");
        } catch (UnknownHostException e) {
            System.err.println("Host not found: " + HOST);
        } catch (IOException e) {
            System.err.println("Client I/O error: " + e.getMessage());
        }
    }
}
