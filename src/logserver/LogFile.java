package logserver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class LogFile {
    static String Home = "clientlog";
    static int RotateHour = 4;
    static String Encoding = "UTF-8";

    private final Path home;
    private PrintStream ps;
    private LocalDateTime rotateTime;

    public LogFile(String identification) {
        home = Paths.get(Home, identification);
        home.toFile().mkdirs();
        Path file = home.resolve("logger.log");
        LocalDateTime lastModified = toLocale(file.toFile().lastModified());
        if (file.toFile().exists() && lastModified.isBefore(nextRotateTime().minusDays(1))) {
            rotate(lastModified);
        } else {
            open(file);
        }
        rotateTime = nextRotateTime();
    }

    public void log(String message, Throwable exception) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(rotateTime)) {
            rotateTime = nextRotateTime();
            rotate(now);
        }

        if (message != null)
            ps.println(message);
        if (exception != null)
            exception.printStackTrace(ps);
    }

    public void close() {
        ps.close();
    }

    private void rotate(LocalDateTime time) {
        Path file = home.resolve("logger.log");
        Path hist = home.resolve("logger." + time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".log");

        if (!hist.toFile().exists()) {
            file.toFile().renameTo(hist.toFile());
            if (ps != null) {
                ps.close();
                open(file);
            }
        }
    }

    private void open(Path path) {
        try {
            ps = new PrintStream(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND), false, Encoding);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static LocalDateTime nextRotateTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rotateTime = now.withHour(RotateHour);
        return rotateTime.isAfter(now) ? rotateTime : rotateTime.plusDays(1);
    }

    private static LocalDateTime toLocale(long time) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC);
    }
}