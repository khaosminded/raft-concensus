package raft;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class kvstore {

    private volatile Map<String, String> map;

    public kvstore() {
        this.map = new ConcurrentHashMap<>();
    }

    public synchronized void put(String key, String val) {
        map.put(key, val);
    }

    public String get(String key) {
        return map.get(key);
    }

    public synchronized void del(String key) {
        if (map.get(key) != null) {
            map.remove(key);
        }
    }

    public String list() {
        String list = "";
        Iterator<String> that = map.keySet().iterator();
        while (that.hasNext()) {
            String key = that.next();
            String val = map.get(key);
            list += "\nkey:" + key + ":value:" + val + ":";
        }
        return list;
    }

}
