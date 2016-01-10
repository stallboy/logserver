package logserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class Main {

    private static void usage(String reason) {
        System.err.println(reason);
        System.out.println("Usage: java -jar logserver.jar [options]");
        System.out.println("    -home           home directory, no default");
        System.out.println("    -port           default 10010");
        System.out.println("    -encoding       default UTF-8");
        System.out.println("    -fileopencache  default 4096");
        System.out.println("    -rotatehour     default 4");
        System.out.println("    -integerid      true if set, default false");
        Runtime.getRuntime().exit(1);
    }

    private static final String SYSTEM = "system";
    private static final String NULL = "null";
    private static final String EXCEPTION = "exception";

    private static LogFileCache cache;
    private static ExecutorService executor;
    private static int port = 10010;
    private static volatile boolean stop = false;
    private static volatile BlockingQueue<Object> stopped = new LinkedBlockingDeque<>();

    public static void main(String args[]) throws SocketException {
        String home = null;
        String encoding = "UTF-8";
        int fileopencache = 4196;
        int rotatehour = 4;
        boolean integerid = false;

        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "-home":
                    home = args[++i];
                    break;
                case "-port":
                    port = Integer.parseInt(args[++i]);
                    break;
                case "-encoding":
                    encoding = args[++i];
                    break;
                case "-fileopencache":
                    fileopencache = Integer.parseInt(args[++i]);
                    break;
                case "-rotatehour":
                    rotatehour = Integer.parseInt(args[++i]);
                    break;
                case "-integerid":
                    integerid = true;
                    break;
                default:
                    usage("unknown args " + args[i]);
                    break;
            }
        }

        if (home == null) {
            usage("-home miss");
            return;
        }

        LogFile.Home = home;
        LogFile.RotateHour = rotatehour;
        LogFile.Encoding = encoding;

        cache = new LogFileCache(fileopencache);
        executor = Executors.newSingleThreadExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(Main::stop, "logserver.ShutdownHook"));
        enqueue(LocalDateTime.now(), InetAddress.getLoopbackAddress(), SYSTEM, "logger started", false);
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[4096];
            while (!stop) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String packetStr = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    String[] pack = packetStr.split("@", 2);
                    boolean idOk = (pack.length == 2 && !pack[0].isEmpty() && checkIntegerIf(pack[0], integerid));
                    String id = idOk ? pack[0] : NULL;
                    String message = idOk ? pack[1] : packetStr;
                    if (message.endsWith("\n"))
                        message = message.substring(0, message.length() - 1);
                    if (message.endsWith("\r"))
                        message = message.substring(0, message.length() - 1);
                    LocalDateTime now = LocalDateTime.now();
                    InetAddress address = packet.getAddress();
                    enqueue(now, address, id, message, true);

                    boolean guessException = message.contains("\n") || message.contains("\r");
                    if (guessException)
                        enqueue(now, address, EXCEPTION, packetStr, false);
                } catch (IOException ignored) {
                }
            }
        }
        enqueue(LocalDateTime.now(), InetAddress.getLoopbackAddress(), SYSTEM, "logger stop", false);

        for (executor.shutdown(); true; ) {
            try {
                if (executor.awaitTermination(1, TimeUnit.SECONDS))
                    break;
            } catch (InterruptedException ignored) {
            }
        }
        cache.close();

        stopped.offer(new Object());

        System.out.println("main thread end");
    }

    private static void stop() {
        stop = true;
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        byte[] bytes = "quit".getBytes();
        DatagramPacket dp = new DatagramPacket(bytes, 0, bytes.length, InetAddress.getLoopbackAddress(), port);
        try {
            socket.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket.close();

        while (true) {
            try {
                stopped.take();
                break;
            } catch (InterruptedException ignored) {
            }
        }
        System.out.println("shutdown hook end");
    }

    private static void enqueue(LocalDateTime now, InetAddress address, String id, String message, boolean logException) {
        executor.execute(() -> {
            String msg = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + address + " " + message;
            try {
                cache.get(id).log(msg, null);
            } catch (Throwable e) {
                if (logException) {
                    try {
                        cache.get(SYSTEM).log(msg, e);
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    private static boolean checkIntegerIf(String id, boolean integerid) {
        if (integerid) {
            try {
                Long.parseLong(id);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

}
