import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PrivateMessagingInterface extends Remote
{
    String sendMessage(String name, String message) throws RemoteException;
    String sendMessageSecure(String name, String message, String signature) throws RemoteException;
}

