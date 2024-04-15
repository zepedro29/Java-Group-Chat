import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class Presences {

    private static Hashtable<String, IPInfo> listaUtilizadores = new Hashtable<String, IPInfo>();
    //private static int cont = 0;

    public Vector<String> getPresences(String IPAddress, String username) {

        long actualTime = new Date().getTime();

        synchronized(this) {
            if (listaUtilizadores.containsKey(IPAddress)) {
                IPInfo newIp = listaUtilizadores.get(IPAddress);
                newIp.setLastSeen(actualTime);
            }
            else {
                IPInfo newIP = new IPInfo(IPAddress, actualTime, username);
                listaUtilizadores.put(IPAddress,newIP);
            }
        }
        return getIPList();
    }


    public Vector<String> getIPList(){
        Vector<String> result = new Vector<String>();
        for (Enumeration<IPInfo> e = listaUtilizadores.elements(); e.hasMoreElements(); ) {
            IPInfo element = e.nextElement();
            if (!element.timeOutPassed(120*1000)) {
                result.add(element.getUsername());
            }
        }
        return result;
    }
}

class IPInfo {

    private String ip;
    private long lastSeen;
    private String username;

    public IPInfo(String ip, long lastSeen, String username) {
        this.ip = ip;
        this.lastSeen = lastSeen;
        this.username = username;
    }


    public String getUsername(){
        return this.username;
    }

    public void setLastSeen(long time){
        this.lastSeen = time;
    }


    public boolean timeOutPassed(int timeout){
        boolean result = false;
        long timePassedSinceLastSeen = new Date().getTime() - this.lastSeen;
        if (timePassedSinceLastSeen >= timeout)
            result = true;
        return result;
    }
}