import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Cliente {
    static int porta = 9999;
    static String host = "192.168.96.237";
    private static HashMap<String, String> clientes = new HashMap<>(); 
    private static PublicKey publicKey; 
    private static PrivateKey privateKey; 
    private static HashMap<String, PublicKey> chavesPublicas = new HashMap<>();
    private static String SERVICE_NAME = "/PrivateMessaging";

    public static void main(String[] args) {

        Scanner scanner3 = new Scanner(System.in);
        System.out.print("Insira o seu username: ");
        String username = scanner3.nextLine();

        boolean msgPriv = true;

        Scanner scanner5 = new Scanner(System.in);
        System.out.println("Receber mensagens privadas (sim/nao)");
        String var = scanner5.nextLine();

        if(var.equals("sim")){
            msgPriv = true;
            System.out.println("verdadeiro");
        }else{
            System.out.println("falso");
            msgPriv = false;
        }


        try {

            Socket socket = new Socket(host, porta);
            System.out.println("Cliente iniciado");

            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

            try {
                LocateRegistry.createRegistry(1099);
                //Enviamos o hashmap das chaves publicas para a interface
                PrivateMessagingInterface ref = new PrivateMessaging(username, msgPriv, chavesPublicas);
                LocateRegistry.getRegistry("127.0.0.1", 1099).rebind(SERVICE_NAME, ref);
            } catch (RemoteException e) {
                
            }

            //Geramos as chaves publicas e privadas 
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(1024);
            KeyPair pair = keyPairGen.generateKeyPair();
            publicKey = pair.getPublic();
            privateKey = pair.getPrivate();

            //Convertemos a chave publica para Base64 para enviar para o servidor 
            String encodedString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            saida.println("SESSION_UPDATE_REQUEST: " + username + ":" + String.valueOf(msgPriv) + ":" + encodedString);

            new Thread(new Runnable() {
                public void run() {
                    while (socket.isConnected()) {
                        try {
                            String msg = entrada.readLine();
  
                            String[] split = msg.split(":");
                            String protocolo = split[0];

                            int pos = msg.indexOf(':');
                            String mensagem = msg.substring(pos + 2);
                            if (protocolo.equalsIgnoreCase("SESSION_UPDATE")) {
                                if(mensagem.startsWith("/*cliente*/")){
                                    String info = mensagem.substring(11); 
                                    String[] split_info = info.split("-"); /*USERNAME-IP-RECEBERPM-PUBLIC_KEY */
                                    //Se o cliente nao estiver na lista adicionamos para ter o seu IP
                                    if(!clientes.keySet().contains(split_info[0])){
                                        int p = split_info[1].indexOf(":"); 
                                        clientes.put(split_info[0], split_info[1].substring(1, p)); 
                                    }
                                    //Se o cliente nao estiver na lista de chaves publicas, adicionamos
                                    if(!chavesPublicas.keySet().contains(split_info[0])){
                                        //Temos que converter de Base64 para PublicKey
                                        byte[] decodedBytes = Base64.getDecoder().decode(split_info[3]);
                                        KeyFactory factory = KeyFactory.getInstance("RSA","SunRsaSign");
                                        PublicKey public_key = (PublicKey) factory.generatePublic(new X509EncodedKeySpec(decodedBytes));
                                        chavesPublicas.put(split_info[0], public_key);
                                        
                                    }
                                    System.out.println(split_info[0] + " - " + split_info[1] + " - "+ split_info[2]);
                                }else{
                                    System.out.println(mensagem);
                                }
                            } else if (protocolo.equalsIgnoreCase("SESSION_TIMEOUT")) {
                                System.out.println(mensagem);
                                entrada.close();
                                saida.flush();
                                saida.close();
                                socket.close();
                            }
                        } catch (IOException e) {
                            System.out.println("Erro ao comunicar com o servidor: " + e);
                            System.exit(-1);
                        } catch (InvalidKeySpecException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (NoSuchProviderException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }).start();


            new java.util.Timer().schedule(new TimerTask(){
                @Override
                public void run() {
                    if(socket.isConnected()){
                        saida.println("SESSION_UPDATE_REQUEST: ");
                        saida.flush();
                    }
                }
            },1000*40, 1000*40);

            //
            while (socket.isConnected()) {
                Scanner input = new Scanner(System.in);
                System.out.print("");
                String msg = input.nextLine();

                if(msg.equalsIgnoreCase("/pm")){

                    System.out.println("Insira o destinatario: ");
                    Scanner scanner2 = new Scanner(System.in);
                    String ipDestino = clientes.get(scanner2.nextLine());
                    Scanner scanner4 = new Scanner(System.in);
                    System.out.println("Insira a mensagem: ");
                    String msgPrivada = scanner4.nextLine();

                    PrivateMessagingInterface ref;

                    try {
                        ref = (PrivateMessagingInterface) LocateRegistry.getRegistry(ipDestino).lookup(SERVICE_NAME);
                        String recebeu = ref.sendMessage(username, msgPrivada);
                        if(recebeu == null){
                            System.out.println("Este utilizador não quer receber mensagens");
                        }else{
                            System.out.println("Mensagem enviada a " + recebeu);
                        }
                    } catch (NotBoundException e) {
                        e.printStackTrace();
                    }        
                }else if(msg.equalsIgnoreCase("/pmSegura")){ //mensagem privada segura

                    System.out.println("Insira o destinatario: ");
                    Scanner scanner2 = new Scanner(System.in);
                    String ipDestino = clientes.get(scanner2.nextLine());
                    Scanner scanner4 = new Scanner(System.in);
                    System.out.println("Insira a mensagem: ");
                    String msgPrivada = scanner4.nextLine();
                    //CRIAR O MESSAGE DIGEST
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(msgPrivada.getBytes()); //passamos os bytes do que escrevemos para o messagedigest
                    byte[] digest = md.digest(); //Temos o sumario
            
                    //CRIAR O ALGORIMTO QUE VAI CIFRAR OS DADOS
                    Cipher cipher = Cipher.getInstance("RSA"); //Usamos o RSA 
                    cipher.init(Cipher.ENCRYPT_MODE, privateKey); //Encriptamos com a privateKey deste cliente 
                    cipher.update(digest); //Adicionamos a assinatura ao resumo 
                    
                    byte[] cipherText = cipher.doFinal(); // juntamos tudo 

                    //Temos que converter os bytes para String (Base64) para poder enviar
                    String msgBase = Base64.getEncoder().encodeToString(cipherText);

                    //Obter a referencia do cliente destino
                    PrivateMessagingInterface ref = (PrivateMessagingInterface) LocateRegistry.getRegistry(ipDestino).lookup(SERVICE_NAME);

                    //Enviar a mensagem
                    String recebeu = ref.sendMessageSecure(username, msgPrivada, msgBase);
                    if(recebeu == null){
                        System.out.println("Este utilizador não quer receber mensagens");
                    }else{
                        System.out.println("Mensagem segura enviada a " + recebeu);
                    }
                }
                else{
                    saida.println("AGENT_POST: " + username + ": " + msg);
                }
            }
//
        } catch (IOException e) {
            System.out.println("Erro ao comunicar com o servidor: " + e);
        } catch (NoSuchAlgorithmException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NotBoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
