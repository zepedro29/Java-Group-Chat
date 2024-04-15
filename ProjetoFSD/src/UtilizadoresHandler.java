import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TimerTask;

public class UtilizadoresHandler extends Thread {
    static ArrayList<UtilizadoresHandler> listaUtilizadores = new ArrayList<>();
    Presences presencesClients;
    static ArrayList<String> listaMensagens = new ArrayList<>();
    Socket socket;
    BufferedReader entrada;
    PrintWriter saida;
    String username;
    String msgPriv;
    String publicKey;

    public UtilizadoresHandler(Socket socket, Presences presencesClients) {
        this.socket = socket;
        try {
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.saida = new PrintWriter(socket.getOutputStream());
            this.presencesClients = presencesClients;
        } catch (IOException e) {
            fecharTudo(socket, entrada, saida);
            System.out.println("Erro ao comunicar com o servidor: " + e);
            System.exit(1);

        }
    }

    public void sessionUpdate(UtilizadoresHandler user) {
        user.saida.println("SESSION_UPDATE: ");
        user.saida.println("SESSION_UPDATE: Utilizadores Online: ");
        for (UtilizadoresHandler u : listaUtilizadores) {
            user.saida.println("SESSION_UPDATE: /*cliente*/" + u.username + "-"
                    + u.socket.getRemoteSocketAddress().toString() + "-" + String.valueOf(u.msgPriv) + "-" + publicKey);
        }

        String mensagens = "";
        int i = listaMensagens.size();

        if (i == 0) {
            user.saida.println("SESSION_UPDATE: Nao ha historico de mensagens!");
            user.saida.flush();

        } else if (i <= 9) {
            for (String s : listaMensagens) {
                mensagens = mensagens.concat(s + ", ");
            }

            user.saida.println("SESSION_UPDATE: Ultimas Mensagens:  ");
            user.saida.println("SESSION_UPDATE: " + mensagens);
            user.saida.flush();

        } else {
            for (int j = i - 10; j < i; j++) {
                mensagens = mensagens.concat(listaMensagens.get(j) + ", ");
            }

            user.saida.println("SESSION_UPDATE: Ultimas Mensagens:  ");
            user.saida.println("SESSION_UPDATE: " + mensagens);
            user.saida.flush();
        }
    }

    public void atualizarPresenca() {
        presencesClients.getPresences(socket.getRemoteSocketAddress().toString(), this.username);
    }

    public void updateAgentPost(String newMessage, String msgClient) {
        for (UtilizadoresHandler u : listaUtilizadores) {
            if (!u.username.equals(msgClient)) {
                u.saida.println("SESSION_UPDATE: " + newMessage);
                u.saida.flush();
            }
        }

    }

    public void lerProtocolo(String msg) {
        // System.out.println(mensagem); //AGENT_POST: jose:ola
        String[] split = msg.split(":");
        String protocolo = split[0];
        System.out.println(protocolo); // AGENT_POST

        int pos = msg.indexOf(':');
        String mensagem = msg.substring(pos + 2); // jose:ola
        String msgClient = msg.split(":")[0]; // jose
        // String xd = mensagem.split(":")[1]; //ola

        if (protocolo.equalsIgnoreCase("AGENT_POST")) {
            atualizarPresenca();
            listaMensagens.add(mensagem);

            updateAgentPost(mensagem, msgClient);

            for (UtilizadoresHandler u : listaUtilizadores) {
                sessionUpdate(u);
            }

        } else if (protocolo.equalsIgnoreCase("SESSION_UPDATE_REQUEST")) {
            if (mensagem.length() > 1) {
                String[] s = mensagem.split(":");
                String username = s[0];
                String msgPriv = s[1];
                this.publicKey = s[2];
                this.username = username;
                if (msgPriv.equalsIgnoreCase("true")) {
                    this.msgPriv = "pode receber pm";
                } else {
                    this.msgPriv = "nao pode recerber pm";
                }
                System.out.println("Cliente " + username + " conectou-se");
                for (UtilizadoresHandler u : listaUtilizadores) {
                    if (!u.username.equals(username)) {
                        u.saida.println("SESSION_UPDATE: Cliente " + username + " conectou-se!");
                        u.saida.flush();
                    }
                }

                if (!listaUtilizadores.contains(this)) {
                    listaUtilizadores.add(this);
                }
                atualizarPresenca();
                for (UtilizadoresHandler u : listaUtilizadores) {
                    sessionUpdate(u);
                }
            } else {
                atualizarPresenca();
                sessionUpdate(this);
            }
        }
    }

    public void removerUtilizadorHandler() {
        listaUtilizadores.remove(this);

        for (UtilizadoresHandler u : listaUtilizadores) {
            if (!u.username.equals(this.username)) {
                u.saida.println("SESSION_UPDATE: SERVER: " + username + " saiu do chat!");
                u.saida.flush();
            }
        }

    }

    public void fecharTudo(Socket socket, BufferedReader bufferedReader, PrintWriter printWriter) {
        removerUtilizadorHandler();

        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (printWriter != null) {
                printWriter.close();
            }
            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // sessionTimeout();
            }
        }, 1000 * 5, 1000 * 5);
        while (socket.isConnected()) {
            try {
                String msg = entrada.readLine();
                lerProtocolo(msg);

            } catch (IOException e) {
                System.out.println("Erro ao comunicar com o servidor: " + e);

            }
        }
    }

}