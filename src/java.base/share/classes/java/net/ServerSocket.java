/*
 * Copyright (c) 1995, 2024, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ServerSocketChannel;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;

import sun.net.PlatformSocketImpl;

/**
 * This class implements server sockets. A server socket waits for
 * requests to come in over the network. It performs some operation
 * based on that request, and then possibly returns a result to the requester.
 * <p>
 * The actual work of the server socket is performed by an instance
 * of the {@code SocketImpl} class.
 *
 * <p> The {@code ServerSocket} class defines convenience
 * methods to set and get several socket options. This class also
 * defines the {@link #setOption(SocketOption, Object) setOption}
 * and {@link #getOption(SocketOption) getOption} methods to set
 * and query socket options.
 * A {@code ServerSocket} supports the following options:
 * <blockquote>
 * <table class="striped">
 * <caption style="display:none">Socket options</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Option Name</th>
 *     <th scope="col">Description</th>
 *   </tr>
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_RCVBUF SO_RCVBUF} </th>
 *     <td> The size of the socket receive buffer </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR} </th>
 *     <td> Re-use address </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 * Additional (implementation specific) options may also be supported.
 *
 * @see     java.net.SocketImpl
 * @see     java.nio.channels.ServerSocketChannel
 * @since   1.0
 */
public class ServerSocket implements java.io.Closeable {
    // the underlying SocketImpl
    private final SocketImpl impl;

    // various states
    private volatile boolean created;   // impl.create(boolean) called
    private volatile boolean bound;
    private volatile boolean closed;

    // used to coordinate creating and closing underlying socket
    private final Object socketLock = new Object();

    /**
     * Creates a server socket with a user-specified {@code SocketImpl}.
     *
     * @param      impl an instance of a SocketImpl to use on the ServerSocket.
     *
     * @throws     NullPointerException if impl is {@code null}.
     *
     * @since 12
     */
    protected ServerSocket(SocketImpl impl) {
        this.impl = Objects.requireNonNull(impl);
    }

    /**
     * Creates an unbound server socket.
     *
     * @throws    IOException IO error when opening the socket.
     */
    public ServerSocket() throws IOException {
        this.impl = createImpl();
    }

    /**
     * Creates a server socket, bound to the specified port. A port number
     * of {@code 0} means that the port number is automatically
     * allocated, typically from an ephemeral port range. This port
     * number can then be retrieved by calling {@link #getLocalPort getLocalPort}.
     * <p>
     * The maximum queue length for incoming connection indications (a
     * request to connect) is set to {@code 50}. If a connection
     * indication arrives when the queue is full, the connection is refused.
     * <p>
     * If the application has specified a {@linkplain SocketImplFactory server
     * socket implementation factory}, that factory's
     * {@linkplain SocketImplFactory#createSocketImpl() createSocketImpl} method
     * is called to create the actual socket implementation. Otherwise a system-default
     * socket implementation is created.
     *
     * @param      port  the port number, or {@code 0} to use a port
     *                   number that is automatically allocated.
     *
     * @throws     IOException  if an I/O error occurs when opening the socket.
     * @throws     IllegalArgumentException if the port parameter is outside
     *             the specified range of valid port values, which is between
     *             0 and 65535, inclusive.
     */
    public ServerSocket(int port) throws IOException {
        this(port, 50, null);
    }

    /**
     * Creates a server socket and binds it to the specified local port
     * number, with the specified backlog.
     * A port number of {@code 0} means that the port number is
     * automatically allocated, typically from an ephemeral port range.
     * This port number can then be retrieved by calling
     * {@link #getLocalPort getLocalPort}.
     * <p>
     * The maximum queue length for incoming connection indications (a
     * request to connect) is set to the {@code backlog} parameter. If
     * a connection indication arrives when the queue is full, the
     * connection is refused.
     * <p>
     * If the application has specified a {@linkplain SocketImplFactory server
     * socket implementation factory}, that factory's
     * {@linkplain SocketImplFactory#createSocketImpl() createSocketImpl} method
     * is called to create the actual socket implementation. Otherwise a system-default
     * socket implementation is created.
     *
     * The {@code backlog} argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogether. The value provided
     * should be greater than {@code 0}. If it is less than or equal to
     * {@code 0}, then an implementation specific default will be used.
     *
     * @param      port     the port number, or {@code 0} to use a port
     *                      number that is automatically allocated.
     * @param      backlog  requested maximum length of the queue of incoming
     *                      connections.
     *
     * @throws     IOException  if an I/O error occurs when opening the socket.
     * @throws     IllegalArgumentException if the port parameter is outside
     *             the specified range of valid port values, which is between
     *             0 and 65535, inclusive.
     */
    public ServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, null);
    }

    /**
     * Create a server with the specified port, listen backlog, and
     * local IP address to bind to.  The <i>bindAddr</i> argument
     * can be used on a multi-homed host for a ServerSocket that
     * will only accept connect requests to one of its addresses.
     * If <i>bindAddr</i> is null, it will default accepting
     * connections on any/all local addresses.
     * The port must be between 0 and 65535, inclusive.
     * A port number of {@code 0} means that the port number is
     * automatically allocated, typically from an ephemeral port range.
     * This port number can then be retrieved by calling
     * {@link #getLocalPort getLocalPort}.
     *
     * The {@code backlog} argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogether. The value provided
     * should be greater than {@code 0}. If it is less than or equal to
     * {@code 0}, then an implementation specific default will be used.
     *
     * @param port  the port number, or {@code 0} to use a port
     *              number that is automatically allocated.
     * @param backlog requested maximum length of the queue of incoming
     *                connections.
     * @param bindAddr the local InetAddress the server will bind to
     *
     * @throws  IOException if an I/O error occurs when opening the socket.
     * @throws     IllegalArgumentException if the port parameter is outside
     *             the specified range of valid port values, which is between
     *             0 and 65535, inclusive.
     * @since   1.1
     */
    @SuppressWarnings("this-escape")
    public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("Port value out of range: " + port);
        if (backlog < 1)
            backlog = 50;

        this.impl = createImpl();
        try {
            bind(new InetSocketAddress(bindAddr, port), backlog);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * Create a server using <i>mptcp</i> (Multipath TCP) or TCP protocol with
     * the specified port, listen backlog, and local IP address to bind to.
     * The <i>bindAddr</i> argument can be used on a multi-homed host for a
     * ServerSocket that will only accept connect requests to one of its addresses.
     * If <i>bindAddr</i> is null, it will default accepting
     * connections on any/all local addresses.
     * The port must be between 0 and 65535, inclusive.
     * A port number of {@code 0} means that the port number is
     * automatically allocated, typically from an ephemeral port range.
     * This port number can then be retrieved by calling
     * {@link #getLocalPort getLocalPort}.
     *
     * The {@code backlog} argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogether. The value provided
     * should be greater than {@code 0}. If it is less than or equal to
     * {@code 0}, then an implementation specific default will be used.
     *
     * The {@code mptcp} argument is used to control whether to create a socket
     * with MPTCP or TCP protocol.
     *
     * @param port  the port number, or {@code 0} to use a port
     *              number that is automatically allocated.
     * @param backlog requested maximum length of the queue of incoming
     *                connections.
     * @param bindAddr the local InetAddress the server will bind to
     * @param mptcp create a socket with MPTCP or TCP protocol.
     *
     * @throws  IOException if an I/O error occurs when opening the socket.
     * @throws     IllegalArgumentException if the port parameter is outside
     *             the specified range of valid port values, which is between
     *             0 and 65535, inclusive.
     */
    @SuppressWarnings("this-escape")
    public ServerSocket(int port, int backlog, InetAddress bindAddr, boolean mptcp) throws IOException {
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("Port value out of range: " + port);
        if (backlog < 1)
            backlog = 50;

        this.impl = createImpl(mptcp);
        try {
            bind(new InetSocketAddress(bindAddr, port), backlog);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * Create a SocketImpl for a server socket. The SocketImpl is created
     * without an underlying socket.
     */
    private static SocketImpl createImpl() {
        SocketImplFactory factory = ServerSocket.factory;
        if (factory != null) {
            return factory.createSocketImpl();
        } else {
            return SocketImpl.createPlatformSocketImpl(true);
        }
    }

    /**
     * Create a SocketImpl for a server socket. The SocketImpl is created
     * without an underlying socket.
     *
     * @param mptcp create a socket with MPTCP or TCP protocol.
     */
    private static SocketImpl createImpl(boolean mptcp) {
        SocketImplFactory factory = ServerSocket.factory;
        if (factory != null) {
            return factory.createSocketImpl();
        } else {
            return SocketImpl.createPlatformSocketImpl(true, mptcp);
        }
    }

    /**
     * Returns the {@code SocketImpl} for this ServerSocket, creating the
     * underlying socket if required.
     * @throws SocketException if creating the underlying socket fails
     */
    private SocketImpl getImpl() throws SocketException {
        if (!created) {
            synchronized (socketLock) {
                if (!created) {
                    if (closed) {
                        throw new SocketException("Socket is closed");
                    }
                    try {
                        impl.create(true);
                    } catch (SocketException e) {
                        throw e;
                    } catch (IOException e) {
                        throw new SocketException(e.getMessage(), e);
                    }
                    created = true;
                }
            }
        }
        return impl;
    }

    /**
     *
     * Binds the {@code ServerSocket} to a specific address
     * (IP address and port number).
     * <p>
     * If the address is {@code null}, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     *
     * @param   endpoint        The IP address and port number to bind to.
     * @throws  IOException if the bind operation fails, the socket
     *                      is already bound or the socket is closed.
     * @throws  IllegalArgumentException if endpoint is a
     *          SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    public void bind(SocketAddress endpoint) throws IOException {
        bind(endpoint, 50);
    }

    /**
     *
     * Binds the {@code ServerSocket} to a specific address
     * (IP address and port number).
     * <p>
     * If the address is {@code null}, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     * <P>
     * The {@code backlog} argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogether. The value provided
     * should be greater than {@code 0}. If it is less than or equal to
     * {@code 0}, then an implementation specific default will be used.
     * @param   endpoint        The IP address and port number to bind to.
     * @param   backlog         requested maximum length of the queue of
     *                          incoming connections.
     * @throws  IOException if the bind operation fails, the socket
     *                      is already bound or the socket is closed.
     * @throws  IllegalArgumentException if endpoint is a
     *          SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (isBound())
            throw new SocketException("Already bound");
        if (endpoint == null)
            endpoint = new InetSocketAddress(0);
        if (!(endpoint instanceof InetSocketAddress epoint))
            throw new IllegalArgumentException("Unsupported address type");
        if (epoint.isUnresolved())
            throw new SocketException("Unresolved address");
        if (backlog < 1)
            backlog = 50;

        // SocketImpl bind+listen throw if already bound or closed
        SocketImpl impl = getImpl();
        impl.bind(epoint.getAddress(), epoint.getPort());
        impl.listen(backlog);
        bound = true;
    }

    /**
     * Returns the local address of this server socket.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the local address
     * after the socket is closed.
     *
     * @return  the address to which this socket is bound,
     *          or {@code null} if the socket is unbound.
     */
    public InetAddress getInetAddress() {
        if (!isBound())
            return null;
        try {
            return getImpl().getInetAddress();
        } catch (SocketException e) {
            // nothing
            // If we're bound, the impl has been created
            // so we shouldn't get here
        }
        return null;
    }

    /**
     * Returns the port number on which this socket is listening.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the port number
     * after the socket is closed.
     *
     * @return  the port number to which this socket is listening or
     *          -1 if the socket is not bound yet.
     */
    public int getLocalPort() {
        if (!isBound())
            return -1;
        try {
            return getImpl().getLocalPort();
        } catch (SocketException e) {
            // nothing
            // If we're bound, the impl has been created
            // so we shouldn't get here
        }
        return -1;
    }

    /**
     * Returns the address of the endpoint this socket is bound to.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the address of the endpoint
     * after the socket is closed.
     *
     * @return a {@code SocketAddress} representing the local endpoint of
     *         this socket, or {@code null} if the socket is not bound yet.
     *
     * @see #getInetAddress()
     * @see #getLocalPort()
     * @see #bind(SocketAddress)
     * @since 1.4
     */
    public SocketAddress getLocalSocketAddress() {
        if (!isBound())
            return null;
        return new InetSocketAddress(getInetAddress(), getLocalPort());
    }

    /**
     * Listens for a connection to be made to this socket and accepts
     * it. The method blocks until a connection is made.
     *
     * <p> This method is {@linkplain Thread#interrupt() interruptible} in the
     * following circumstances:
     * <ol>
     *   <li> The socket is {@linkplain ServerSocketChannel#socket() associated}
     *        with a {@link ServerSocketChannel ServerSocketChannel}. In that
     *        case, interrupting a thread accepting a connection will close the
     *        underlying channel and cause this method to throw {@link
     *        java.nio.channels.ClosedByInterruptException} with the interrupt
     *        status set.
     *   <li> The socket uses the system-default socket implementation and a
     *        {@linkplain Thread#isVirtual() virtual thread} is accepting a
     *        connection. In that case, interrupting the virtual thread will
     *        cause it to wakeup and close the socket. This method will then throw
     *        {@code SocketException} with the interrupt status set.
     * </ol>
     *
     * @implNote
     * An instance of this class using a system-default {@code SocketImpl}
     * accepts sockets with a {@code SocketImpl} of the same type, regardless
     * of the {@linkplain Socket#setSocketImplFactory(SocketImplFactory)
     * client socket implementation factory}, if one has been set.
     *
     * @throws     IOException  if an I/O error occurs when waiting for a
     *               connection, the socket is not bound or the socket is closed.
     * @throws     SocketTimeoutException if a timeout was previously set with setSoTimeout and
     *             the timeout has been reached.
     * @throws     java.nio.channels.IllegalBlockingModeException
     *             if this socket has an associated channel, the channel is in
     *             non-blocking mode, and there is no connection ready to be
     *             accepted
     *
     * @return the new Socket
     */
    public Socket accept() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (!isBound())
            throw new SocketException("Socket is not bound yet");
        Socket s = new Socket((SocketImpl) null);
        implAccept(s);
        return s;
    }

    /**
     * Subclasses of ServerSocket use this method to override accept()
     * to return their own subclass of socket.  So a FooServerSocket
     * will typically hand this method a newly created, unbound, FooSocket.
     * On return from implAccept the FooSocket will be connected to a client.
     *
     * <p> The behavior of this method is unspecified when invoked with a
     * socket that is not newly created and unbound. Any socket options set
     * on the given socket prior to invoking this method may or may not be
     * preserved when the connection is accepted. It may not be possible to
     * accept a connection when this socket has a {@code SocketImpl} of one
     * type and the given socket has a {@code SocketImpl} of a completely
     * different type.
     *
     * @implNote
     * An instance of this class using a system-default {@code SocketImpl}
     * can accept a connection with a Socket using a {@code SocketImpl} of
     * the same type: {@code IOException} is thrown if the Socket is using
     * a custom {@code SocketImpl}. An instance of this class using a
     * custom {@code SocketImpl} cannot accept a connection with a Socket
     * using a system-default {@code SocketImpl}.
     *
     * @param s the Socket
     * @throws java.nio.channels.IllegalBlockingModeException
     *         if this socket has an associated channel,
     *         and the channel is in non-blocking mode
     * @throws IOException if an I/O error occurs when waiting
     *         for a connection, or if it is not possible for this socket
     *         to accept a connection with the given socket
     *
     * @since   1.1
     */
    protected final void implAccept(Socket s) throws IOException {
        SocketImpl si = s.impl();

        // Socket has no SocketImpl
        if (si == null) {
            si = implAccept();
            try {
                s.setConnectedImpl(si);
            } catch (SocketException e) {
                // s has been closed so newly accepted connection needs to be closed
                si.closeQuietly();
                throw e;
            }
            return;
        }

        // Socket has a SOCKS or HTTP SocketImpl, need delegate
        if (si instanceof DelegatingSocketImpl dsi) {
            si = dsi.delegate();
            assert si instanceof PlatformSocketImpl;
        }

        // Accept connection with a platform or custom SocketImpl.
        // For the platform SocketImpl case:
        // - the connection is accepted with a new SocketImpl
        // - the SO_TIMEOUT socket option is copied to the new SocketImpl
        // - the Socket is connected to the new SocketImpl
        // - the existing/old SocketImpl is closed
        // For the custom SocketImpl case, the connection is accepted with the
        // existing custom SocketImpl.
        ensureCompatible(si);
        if (impl instanceof PlatformSocketImpl) {
            SocketImpl psi = platformImplAccept();
            si.copyOptionsTo(psi);
            try {
                s.setConnectedImpl(psi);
            } catch (SocketException e) {
                // s has been closed so newly accepted connection needs to be closed
                psi.closeQuietly();
                throw e;
            }
        } else {
            s.setImpl(null);    // temporarily break connection to impl
            try {
                customImplAccept(si);
            } finally {
                s.setImpl(si);  // restore connection to impl
            }
            s.setConnected();
        }

    }

    /**
     * Accepts a connection with a new SocketImpl.
     * @return the new SocketImpl
     */
    private SocketImpl implAccept() throws IOException {
        if (impl instanceof PlatformSocketImpl) {
            return platformImplAccept();
        } else {
            // custom server SocketImpl, client SocketImplFactory must be set
            SocketImplFactory factory = Socket.socketImplFactory();
            if (factory == null) {
                throw new IOException("An instance of " + impl.getClass() +
                    " cannot accept connection with 'null' SocketImpl:" +
                    " client socket implementation factory not set");
            }
            SocketImpl si = factory.createSocketImpl();
            customImplAccept(si);
            return si;
        }
    }

    /**
     * Accepts a connection with a new platform SocketImpl.
     * @return the new platform SocketImpl
     */
    private SocketImpl platformImplAccept() throws IOException {
        assert impl instanceof PlatformSocketImpl;

        // create a new platform SocketImpl and accept the connection
        SocketImpl psi = SocketImpl.createPlatformSocketImpl(false);
        implAccept(psi);
        return psi;
    }

    /**
     * Accepts a new connection with the given custom SocketImpl.
     */
    private void customImplAccept(SocketImpl si) throws IOException {
        assert !(impl instanceof PlatformSocketImpl)
                && !(si instanceof PlatformSocketImpl);

        si.reset();
        try {
            // custom SocketImpl may expect fd/address objects to be created
            si.fd = new FileDescriptor();
            si.address = new InetAddress();
            implAccept(si);
        } catch (Exception e) {
            si.reset();
            throw e;
        }
    }

    /**
     * Accepts a new connection so that the given SocketImpl is connected to
     * the peer.
     * @throws IOException if an I/O error occurs
     */
    private void implAccept(SocketImpl si) throws IOException {
        assert !(si instanceof DelegatingSocketImpl);

        // accept a connection
        try {
            impl.accept(si);
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (InterruptedIOException e) {
            Thread thread = Thread.currentThread();
            if (thread.isVirtual() && thread.isInterrupted()) {
                close();
                throw new SocketException("Closed by interrupt");
            }
            throw e;
        }

    }

    /**
     * Throws IOException if the server SocketImpl and the given client
     * SocketImpl are not both platform or custom SocketImpls.
     */
    private void ensureCompatible(SocketImpl si) throws IOException {
        if ((impl instanceof PlatformSocketImpl) != (si instanceof PlatformSocketImpl)) {
            throw new IOException("An instance of " + impl.getClass() +
                " cannot accept a connection with an instance of " + si.getClass());
        }
    }

    /**
     * Closes this socket.
     *
     * Any thread currently blocked in {@link #accept()} will throw
     * a {@link SocketException}.
     *
     * <p> If this socket has an associated channel then the channel is closed
     * as well.
     *
     * <p> Once closed, several of the methods defined by this class will throw
     * an exception if invoked on the closed socket.
     *
     * @throws     IOException  if an I/O error occurs when closing the socket.
     */
    public void close() throws IOException {
        synchronized (socketLock) {
            if (!closed) {
                closed = true;

                // close underlying socket if created
                if (created) {
                    impl.close();
                }
            }
        }
    }

    /**
     * Returns the unique {@link java.nio.channels.ServerSocketChannel} object
     * associated with this socket, if any.
     *
     * <p> A server socket will have a channel if, and only if, the channel
     * itself was created via the {@link
     * java.nio.channels.ServerSocketChannel#open ServerSocketChannel.open}
     * method.
     *
     * @return  the server-socket channel associated with this socket,
     *          or {@code null} if this socket was not created
     *          for a channel
     *
     * @since 1.4
     */
    public ServerSocketChannel getChannel() {
        return null;
    }

    /**
     * Returns the binding state of the ServerSocket.
     * <p>
     * If the socket was bound prior to being {@linkplain #close closed},
     * then this method will continue to return {@code true}
     * after the socket is closed.
     *
     * @return true if the ServerSocket successfully bound to an address
     * @since 1.4
     */
    public boolean isBound() {
        return bound;
    }

    /**
     * Returns the closed state of the ServerSocket.
     *
     * @return true if the socket has been closed
     * @since 1.4
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Enable/disable {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT} with the
     * specified timeout, in milliseconds.  With this option set to a positive
     * timeout value, a call to accept() for this ServerSocket
     * will block for only this amount of time.  If the timeout expires,
     * a {@link java.net.SocketTimeoutException} is raised, though the
     * ServerSocket is still valid. A timeout of zero is interpreted as an
     * infinite timeout.
     * The option <B>must</B> be enabled prior to entering the blocking
     * operation to have effect.
     *
     * @param timeout the specified timeout, in milliseconds
     * @throws SocketException if there is an error in the underlying protocol,
     *         such as a TCP error, or the socket is closed.
     * @throws  IllegalArgumentException  if {@code timeout} is negative
     * @since   1.1
     * @see #getSoTimeout()
     */
    public void setSoTimeout(int timeout) throws SocketException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (timeout < 0)
            throw new IllegalArgumentException("timeout < 0");
        getImpl().setOption(SocketOptions.SO_TIMEOUT, timeout);
    }

    /**
     * Retrieve setting for {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT}.
     * 0 returns implies that the option is disabled (i.e., timeout of infinity).
     * @return the {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT} value
     * @throws IOException if an I/O error occurs or the socket is closed.
     * @since   1.1
     * @see #setSoTimeout(int)
     */
    public int getSoTimeout() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        Object o = getImpl().getOption(SocketOptions.SO_TIMEOUT);
        /* extra type safety */
        if (o instanceof Integer i) {
            return i.intValue();
        } else {
            return 0;
        }
    }

    /**
     * Enable/disable the {@link StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR}
     * socket option.
     * <p>
     * When a TCP connection is closed the connection may remain
     * in a timeout state for a period of time after the connection
     * is closed (typically known as the {@code TIME_WAIT} state
     * or {@code 2MSL} wait state).
     * For applications using a well known socket address or port
     * it may not be possible to bind a socket to the required
     * {@code SocketAddress} if there is a connection in the
     * timeout state involving the socket address or port.
     * <p>
     * Enabling {@code SO_REUSEADDR} prior to binding the socket using
     * {@link #bind(SocketAddress)} allows the socket to be bound even
     * though a previous connection is in a timeout state.
     * <p>
     * When a {@code ServerSocket} is created the initial setting
     * of {@code SO_REUSEADDR} is not defined. Applications can use
     * {@link #getReuseAddress()} to determine the initial
     * setting of {@code SO_REUSEADDR}.
     * <p>
     * The behaviour when {@code SO_REUSEADDR} is enabled or disabled
     * after a socket is bound (See {@link #isBound()})
     * is not defined.
     *
     * @param on  whether to enable or disable the socket option
     * @throws    SocketException if an error occurs enabling or
     *            disabling the {@code SO_REUSEADDR}
     *            socket option, or the socket is closed.
     * @since 1.4
     * @see #getReuseAddress()
     * @see #bind(SocketAddress)
     * @see #isBound()
     * @see #isClosed()
     */
    public void setReuseAddress(boolean on) throws SocketException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        getImpl().setOption(SocketOptions.SO_REUSEADDR, Boolean.valueOf(on));
    }

    /**
     * Tests if {@link StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR} is enabled.
     *
     * @return a {@code boolean} indicating whether or not
     *         {@code SO_REUSEADDR} is enabled.
     * @throws SocketException if there is an error in the underlying protocol, such as a TCP error,
     *         or the socket is closed.
     * @since   1.4
     * @see #setReuseAddress(boolean)
     */
    public boolean getReuseAddress() throws SocketException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        return ((Boolean) (getImpl().getOption(SocketOptions.SO_REUSEADDR))).booleanValue();
    }

    /**
     * Returns the implementation address and implementation port of
     * this socket as a {@code String}.
     *
     * @return  a string representation of this socket.
     */
    public String toString() {
        if (!isBound())
            return "ServerSocket[unbound]";
        InetAddress in = impl.getInetAddress();
        return "ServerSocket[addr=" + in +
                ",localport=" + impl.getLocalPort()  + "]";
    }

    /**
     * The factory for all server sockets.
     */
    private static volatile SocketImplFactory factory;

    /**
     * Sets the server socket implementation factory for the
     * application. The factory can be specified only once.
     * <p>
     * When an application creates a new server socket, the socket
     * implementation factory's {@code createSocketImpl} method is
     * called to create the actual socket implementation.
     * <p>
     * Passing {@code null} to the method is a no-op unless the factory
     * was already set.
     *
     * @param      fac   the desired factory.
     * @throws     IOException  if an I/O error occurs when setting the
     *               socket factory.
     * @throws     SocketException  if the factory has already been defined.
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @deprecated Use a {@link javax.net.ServerSocketFactory} and subclass {@code ServerSocket}
     *    directly.
     *    <br> This method provided a way in early JDK releases to replace the
     *    system wide implementation of {@code ServerSocket}. It has been mostly
     *    obsolete since Java 1.4. If required, a {@code ServerSocket} can be
     *    created to use a custom implementation by extending {@code ServerSocket}
     *    and using the {@linkplain #ServerSocket(SocketImpl) protected
     *    constructor} that takes an {@linkplain SocketImpl implementation}
     *    as a parameter.
     */
    @Deprecated(since = "17")
    public static synchronized void setSocketFactory(SocketImplFactory fac) throws IOException {
        if (factory != null) {
            throw new SocketException("factory already defined");
        }
        factory = fac;
    }

    /**
     * Sets a default proposed value for the
     * {@link StandardSocketOptions#SO_RCVBUF SO_RCVBUF} option for sockets
     * accepted from this {@code ServerSocket}. The value actually set
     * in the accepted socket must be determined by calling
     * {@link Socket#getReceiveBufferSize()} after the socket
     * is returned by {@link #accept()}.
     * <p>
     * The value of {@code SO_RCVBUF} is used both to set the size of
     * the internal socket receive buffer, and to set the size
     * of the TCP receive window that is advertised to the remote peer.
     * <p>
     * It is possible to change the value subsequently, by calling
     * {@link Socket#setReceiveBufferSize(int)}. However, if the application
     * wishes to allow a receive window larger than 64K bytes, as defined by RFC1323
     * then the proposed value must be set in the ServerSocket <B>before</B>
     * it is bound to a local address. This implies, that the ServerSocket must be
     * created with the no-argument constructor, then setReceiveBufferSize() must
     * be called and lastly the ServerSocket is bound to an address by calling bind().
     * <p>
     * Failure to do this will not cause an error, and the buffer size may be set to the
     * requested value but the TCP receive window in sockets accepted from
     * this ServerSocket will be no larger than 64K bytes.
     *
     * @throws SocketException if there is an error in the underlying protocol,
     *         such as a TCP error, or the socket is closed.
     *
     * @param size the size to which to set the receive buffer
     * size. This value must be greater than 0.
     *
     * @throws    IllegalArgumentException if the
     * value is 0 or is negative.
     *
     * @since 1.4
     * @see #getReceiveBufferSize
     */
    public void setReceiveBufferSize(int size) throws SocketException {
        if (size <= 0)
            throw new IllegalArgumentException("negative receive size");
        if (isClosed())
            throw new SocketException("Socket is closed");
        getImpl().setOption(SocketOptions.SO_RCVBUF, size);
    }

    /**
     * Gets the value of the {@link StandardSocketOptions#SO_RCVBUF SO_RCVBUF} option
     * for this {@code ServerSocket}, that is the proposed buffer size that
     * will be used for Sockets accepted from this {@code ServerSocket}.
     *
     * <p>Note, the value actually set in the accepted socket is determined by
     * calling {@link Socket#getReceiveBufferSize()}.
     * @return the value of the {@code SO_RCVBUF} option for this {@code Socket}.
     * @throws SocketException if there is an error in the underlying protocol,
     *         such as a TCP error, or the socket is closed.
     * @see #setReceiveBufferSize(int)
     * @since 1.4
     */
    public int getReceiveBufferSize() throws SocketException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        int result = 0;
        Object o = getImpl().getOption(SocketOptions.SO_RCVBUF);
        if (o instanceof Integer) {
            result = ((Integer)o).intValue();
        }
        return result;
    }

    /**
     * Sets performance preferences for this ServerSocket.
     *
     * <p> Sockets use the TCP/IP protocol by default.  Some implementations
     * may offer alternative protocols which have different performance
     * characteristics than TCP/IP.  This method allows the application to
     * express its own preferences as to how these tradeoffs should be made
     * when the implementation chooses from the available protocols.
     *
     * <p> Performance preferences are described by three integers
     * whose values indicate the relative importance of short connection time,
     * low latency, and high bandwidth.  The absolute values of the integers
     * are irrelevant; in order to choose a protocol the values are simply
     * compared, with larger values indicating stronger preferences.  If the
     * application prefers short connection time over both low latency and high
     * bandwidth, for example, then it could invoke this method with the values
     * {@code (1, 0, 0)}.  If the application prefers high bandwidth above low
     * latency, and low latency above short connection time, then it could
     * invoke this method with the values {@code (0, 1, 2)}.
     *
     * <p> Invoking this method after this socket has been bound
     * will have no effect. This implies that in order to use this capability
     * requires the socket to be created with the no-argument constructor.
     *
     * @param  connectionTime
     *         An {@code int} expressing the relative importance of a short
     *         connection time
     *
     * @param  latency
     *         An {@code int} expressing the relative importance of low
     *         latency
     *
     * @param  bandwidth
     *         An {@code int} expressing the relative importance of high
     *         bandwidth
     *
     * @since 1.5
     */
    public void setPerformancePreferences(int connectionTime,
                                          int latency,
                                          int bandwidth)
    {
        /* Not implemented yet */
    }

    /**
     * Sets the value of a socket option.
     *
     * @param <T> The type of the socket option value
     * @param name The socket option
     * @param value The value of the socket option. A value of {@code null}
     *              may be valid for some options.
     * @return this ServerSocket
     *
     * @throws UnsupportedOperationException if the server socket does not
     *         support the option.
     *
     * @throws IllegalArgumentException if the value is not valid for
     *         the option.
     *
     * @throws IOException if an I/O error occurs, or if the socket is closed.
     *
     * @throws NullPointerException if name is {@code null}
     *
     * @since 9
     */
    public <T> ServerSocket setOption(SocketOption<T> name, T value)
        throws IOException
    {
        Objects.requireNonNull(name);
        if (isClosed())
            throw new SocketException("Socket is closed");
        getImpl().setOption(name, value);
        return this;
    }

    /**
     * Returns the value of a socket option.
     *
     * @param <T> The type of the socket option value
     * @param name The socket option
     *
     * @return The value of the socket option.
     *
     * @throws UnsupportedOperationException if the server socket does not
     *         support the option.
     *
     * @throws IOException if an I/O error occurs, or if the socket is closed.
     *
     * @throws NullPointerException if name is {@code null}
     *
     * @since 9
     */
    public <T> T getOption(SocketOption<T> name) throws IOException {
        Objects.requireNonNull(name);
        if (isClosed())
            throw new SocketException("Socket is closed");
        return getImpl().getOption(name);
    }

    // cache of unmodifiable impl options. Possibly set racy, in impl we trust
    private volatile Set<SocketOption<?>> options;

    /**
     * Returns a set of the socket options supported by this server socket.
     *
     * This method will continue to return the set of options even after
     * the socket has been closed.
     *
     * @return A set of the socket options supported by this socket. This set
     *         may be empty if the socket's SocketImpl cannot be created.
     *
     * @since 9
     */
    public Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> so = options;
        if (so != null)
            return so;

        try {
            SocketImpl impl = getImpl();
            options = Collections.unmodifiableSet(impl.supportedOptions());
        } catch (IOException e) {
            options = Collections.emptySet();
        }
        return options;
    }
}
