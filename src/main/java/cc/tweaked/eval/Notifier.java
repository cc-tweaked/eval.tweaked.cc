package cc.tweaked.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.OptionalLong;

/// Support for the systemd notifier and watchdog.
///
/// See [`sd_notify`](https://www.freedesktop.org/software/systemd/man/latest/sd_notify.html)
public final class Notifier {
    private static final Logger LOG = LoggerFactory.getLogger(Notifier.class);
    private final OptionalLong watchdogTimeout;
    private long lastWatchdog = -1;

    public Notifier() {
        var watchdogPid = System.getenv("WATCHDOG_PID");
        var watchdogTimeout = System.getenv("WATCHDOG_USEC");
        this.watchdogTimeout =
            watchdogPid != null && watchdogPid.equals(Long.toString(ProcessHandle.current().pid())) && watchdogTimeout != null
                ? OptionalLong.of(Long.parseLong(watchdogTimeout))
                : OptionalLong.empty();
    }

    private static void notify(String message) {
        var notifyPath = System.getenv("NOTIFY_SOCKET");
        if (notifyPath == null) return;

        if (notifyPath.startsWith("@")) {
            notifyPath = "\0" + notifyPath.substring(1);
        } else if (!notifyPath.startsWith("/")) {
            throw new IllegalStateException("Unsupported socket type");
        }

        try (Arena arena = Arena.ofConfined()) {
            var errnoState = arena.allocate(SocketFfi.errnoLayout);

            int socketFd = -1;
            try {
                socketFd = (int) SocketFfi.socket.invokeExact(errnoState, SocketFfi.AF_UNIX, SocketFfi.SOCK_DGRAM | SocketFfi.SOCK_CLOEXEC, 0);
                if (socketFd < 0) SocketFfi.throwErrno("socket", errnoState);

                var bytes = notifyPath.getBytes(StandardCharsets.UTF_8);
                var socketAddr = arena.allocate(SocketFfi.socketAddrLayout);
                SocketFfi.socketAddrFamily.set(socketAddr, 0, (short) SocketFfi.AF_UNIX);
                socketAddr.asSlice(SocketFfi.socketAddrPathOffset).copyFrom(MemorySegment.ofArray(bytes));

                int result = (int) SocketFfi.connect.invokeExact(
                    errnoState, socketFd, socketAddr, (int) (SocketFfi.socketAddrPathOffset + bytes.length)
                );
                if (result != 0) SocketFfi.throwErrno("connect", errnoState);

                var messageAddr = arena.allocateFrom(message);
                long written = (long) SocketFfi.write.invokeExact(errnoState, socketFd, messageAddr, messageAddr.byteSize());
                if (written != messageAddr.byteSize()) SocketFfi.throwErrno("write", errnoState);
            } finally {
                if (socketFd >= 0) {
                    var _ = (int) SocketFfi.close.invokeExact(socketFd);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Error in FFI", t);
        }
    }

    /**
     * Signal that the service is ready.
     */
    public void ready() {
        notify("READY=1");
    }

    /**
     * The time to wait until we should signal the watchdog.
     *
     * @return The time to wait until we should signal the watchdog, in microseconds. Will return a negative value, if
     * the watchdog should be triggered now.
     */
    public long watchdogDelay() {
        if (watchdogTimeout.isEmpty()) return Long.MAX_VALUE;
        if (lastWatchdog == -1) return -1;

        var timeout = lastWatchdog + watchdogTimeout.getAsLong() / 2;
        return timeout - System.nanoTime();
    }

    public void signalWatchdog() {
        var watchdogDelay = watchdogDelay();
        if (watchdogDelay <= 0) {
            lastWatchdog = System.nanoTime();
            notify("WATCHDOG=1");
        }
    }

    private static final class SocketFfi {
        static final int AF_UNIX = 1;
        static final int SOCK_DGRAM = 2;
        static final int SOCK_CLOEXEC = 0x80000;

        static final MethodHandle socket;
        static final MethodHandle connect;
        static final MethodHandle write;
        static final MethodHandle close;
        static final MethodHandle strerr;

        static final StructLayout errnoLayout;
        static final VarHandle errnoHandle;

        static final StructLayout socketAddrLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_SHORT.withName("family"),
            MemoryLayout.sequenceLayout(108, ValueLayout.JAVA_CHAR).withName("path")
        );
        static final VarHandle socketAddrFamily = socketAddrLayout.varHandle(MemoryLayout.PathElement.groupElement("family"));
        static final long socketAddrPathOffset = socketAddrLayout.byteOffset(MemoryLayout.PathElement.groupElement("path"));

        static {
            Linker linker = Linker.nativeLinker();
            SymbolLookup stdlib = linker.defaultLookup();

            Linker.Option errnoCallState = Linker.Option.captureCallState("errno");
            errnoLayout = Linker.Option.captureStateLayout();
            errnoHandle = errnoLayout.varHandle(MemoryLayout.PathElement.groupElement("errno"));

            var cInt = (ValueLayout.OfInt) Linker.nativeLinker().canonicalLayouts().get("int");

            socket = linker.downcallHandle(
                stdlib.findOrThrow("socket"),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
                errnoCallState
            );
            connect = linker.downcallHandle(
                stdlib.findOrThrow("connect"),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
                errnoCallState
            );
            write = linker.downcallHandle(
                stdlib.findOrThrow("write"),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
                errnoCallState
            );
            close = linker.downcallHandle(
                stdlib.findOrThrow("close"),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
            );
            strerr = linker.downcallHandle(
                stdlib.find("strerror").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
            );
        }

        private static void throwErrno(String message, MemorySegment errnoState) throws Throwable {
            var errno = (int) errnoHandle.get(errnoState, 0);
            var errrorString = ((MemorySegment) strerr.invokeExact(errno)).reinterpret(Long.MAX_VALUE).getString(0);
            throw new IllegalArgumentException("Error in " + message + ": " + errrorString);
        }
    }
}
