import java.net.*;
import java.io.*;

public class Servidor {
	static int porta=9999;

	public static void main(String[] args) {

		
		ServerSocket socketServidor = null;
        Presences listaUtilizadores = new Presences();
	
        try {
            socketServidor = new ServerSocket(porta);
            System.out.println("Servidor iniciado com sucesso na porta " + porta);
        } catch (IOException e) {
            System.out.println("Erro ao iniciar o servidor: " + e);
            System.exit(1);
        } 

        while(true){
            
            try {
                Socket conexao = socketServidor.accept();
                UtilizadoresHandler utilizadores = new UtilizadoresHandler(conexao, listaUtilizadores); 
                utilizadores.start(); 
            } catch (IOException e) {
                
                System.out.println("Erro na execução do servidor: " + e);
            } 
        }
        
    }
}
