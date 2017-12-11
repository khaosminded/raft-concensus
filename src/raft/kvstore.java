package raft;




import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static raft.Protocol.Operation.*;

public class kvstore {

    private static volatile Map<String, String> map = new ConcurrentHashMap<>();
    private Protocol.TYPE type;

    public kvstore() {

    }

    public kvstore(Protocol.TYPE type) {
        this.type = type;
    }

    public synchronized void put(String key, String val) {
        map.put(key, val);
    }

    public String get(String key) {
        return map.get(key);
    }

    public synchronized void del(String key) {
        if(map.get(key)!=null)
            map.remove(key);
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
