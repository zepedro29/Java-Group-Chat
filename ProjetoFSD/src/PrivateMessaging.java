import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class PrivateMessaging extends UnicastRemoteObject implements PrivateMessagingInterface {

    private String nome;
    private boolean receberPM;
    private HashMap<String, PublicKey> chavesPublicas;

    public PrivateMessaging(String nome, boolean receberPM, HashMap<String, PublicKey> chavesPublicas) throws RemoteException {
        super();
        this.nome = nome;
        this.receberPM = receberPM;
        this.chavesPublicas = chavesPublicas;
    }

    public String sendMessage(String enviou, String msg) throws RemoteException {
        if(this.receberPM == true){
            System.out.println("Mensagem privada de " + enviou + ": " + msg); //E ESTA MENSAGEM
            return this.nome;
        }else{
            return null;
        }
    }

    public String sendMessageSecure(String enviou, String msg, String assinatura){
        if(this.receberPM == true){
            try{
                //Converter de Base 64 para bytes e decifrar com a chave publica para obter o sumario original
                byte[] decodedBytes = Base64.getDecoder().decode(assinatura);
                Cipher cipher = Cipher.getInstance("RSA");

                //Obter a chave publica do cliente pelo seu nome
                PublicKey pk = chavesPublicas.get(enviou);
                //System.out.println("PK DE QUEM ENVIOU: \nl" + pk);

                //Decifrar o sumario com a chaave publica do cliente
                cipher.init(Cipher.DECRYPT_MODE, pk);
                byte[] decipheredDigest = cipher.doFinal(decodedBytes);

                //Gerar um novo sumario da mensagem recebida
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(msg.getBytes()); //passamos os bytes do que escrevemos para o messagedigest
                byte[] digest = md.digest();

                //Comparar os sumarios para verificar autenticidade
                if(Arrays.equals(decipheredDigest, digest)){
                    System.out.println("Mensagem privada segura de " + enviou + ": " + msg);
                }else{
                    System.out.println(enviou + " tentou enviar uma mensagem que sofreu alteracoes");
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }
            return this.nome;
        }else{
            return null;
        }
    }

}
