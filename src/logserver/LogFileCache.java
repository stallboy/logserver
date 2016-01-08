package logserver;

import java.util.LinkedHashMap;
import java.util.Map;

public class LogFileCache {
    private final Map<String, LogFile> cache;

    public LogFileCache(int cacheCapacity) {
        cache = new LinkedHashMap<String, LogFile>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, LogFile> eldest) {
                if (size() > cacheCapacity) {
                    eldest.getValue().close();
                    return true;
                }
                return false;
            }
        };
    }

    public LogFile get(String identification) {
        LogFile lf = cache.get(identification);
        if (lf == null){
            lf = new LogFile(identification);
            cache.put(identification, lf);
        }
        return lf;
    }

    public void close() {
        cache.values().forEach(LogFile::close);
    }
}
