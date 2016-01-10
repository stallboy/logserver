package logserver;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class LogFile {
    static String Home = "clientlog";
    static int RotateHour = 4;
    static String Encoding = "UTF-8";

    private final File home;
    private PrintStream ps;
    private LocalDateTime rotateTime;

    public LogFile(String identification) {
        home = new File(Home, identification);
        home.mkdirs();
        File file = new File(home, "logger.log");
        LocalDateTime lastModified = toLocale(file.lastModified());
        if (file.exists() && lastModified.isBefore(nextRotateTime().minusDays(1))) {
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

        if (null != exception)
            exception.printStackTrace(ps);
    }

    public void close() {
        ps.close();
    }

    private void rotate(LocalDateTime time) {
        File file = new File(home, "logger.log");
        String dst = "logger." + time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".log";
        File dstFile = new File(home, dst);
        if (!dstFile.exists()) {
            file.renameTo(dstFile);
            if (ps != null) {
                ps.close();
                open(new File(home, "logger.log"));
            }
        }
    }

    private void open(File file) {
        try {
            ps = new PrintStream(new FileOutputStream(file, true), false, Encoding);
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
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