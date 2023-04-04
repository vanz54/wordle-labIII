// Libreria java
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Reti e Laboratorio III - A.A. 2022/2023
 * Wordle
 * 
 * Classe Java che rappresenta il client del gioco Wordle.
 * Prende in input i comandi da tastiera (uno alla volta) e riceverà sempre risposta dal server
 * anch'esse una alla volta, impostatto sulla falsa riga dell'assignment "Dungeon Adventures" per la
 * comunicazione client-server, botta e risposta tra essi per permettere di giocare a Wordle.
 * 
 * @author Tommaso Vanz
 */

public class ClientMain {
  // Percorso del file di configurazione del client.
	public static final String configFile = "client.properties";
  // Nome host e porta del server.
	public static String hostname;
	public static int port;
  // Porta e schema di indirizzamento per identificare il multicast [224.0.0.0-239.255.255.255] 
  public static int multicastPort;
  public static String multicastHost;
  // Array di stringhe di notifiche
  private static List<String> arrayNotifiche = new ArrayList<String>(); 
  // Variabile per uscire dall'ascolto sulla multicastSocket dopo una logout
  private static boolean exitms = false;
      public static void main(String[] args) throws Exception {
          try { 
          // Leggo il file di configurazione prendendo le info che mi servono da client.properties
			    readConfig();

          // Try with resources sulle socket e sugli input da tastiera e dal server
          try (Socket clientSocket = new Socket(hostname, port); // Client crea socket verso server, richiesta connessione
               Scanner inputTastiera = new Scanner(System.in); // Scannerizza input da tastiera
               Scanner inputFromServer = new Scanner(clientSocket.getInputStream()); // Input dal server
               MulticastSocket multicastSocket = new MulticastSocket(multicastPort); // Socket per gruppo sociale dei client
              ) {
              
          // Connessione avvenuta, stampo le azioni possibili
          System.out.println("[CLIENT] Connessione all' host " + hostname + " sulla porta: " + port);
          System.out.println("[CLIENT] L'utente puo' eseguire le seguenti azioni: \n|-> register \n|-> login \n|-> logout \n|-> play wordle | play\n|-> send word | send\n|-> send me statistics | send stats\n|-> share \n|-> show me sharing | show shares\n|-> remove user \n|-> exit");
          
          // Thread che inizializzo non appena il client fa la join
          Thread threadListenerUDPs = new Thread () {
            public void run () {
              System.out.println("[CLIENT] Il client si mette in attesa di share dagli altri utenti.");
              while(!exitms){
                // Se ricevo condivisioni tramite UDP le stampo nel client che ha mandato show me sharing
                DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                try {
                  multicastSocket.receive(response);
                } catch (IOException e) {
                  System.out.println("[CLIENT] Il client non riceve notifiche al momento.");
                }
                String responseUDPfromServer = new String(response.getData(), 0, response.getLength());
                arrayNotifiche.add(responseUDPfromServer);
                // Decommenta se vuoi vedere ogni volta il client che notifiche riceve
                // System.out.println("[CLIENT] Ricevuta condivisione risultati dal server: " + responseUDPfromServer);
              }
            }
          };

          // Inizializzo l'output verso il server, verso il quale manderò i comandi
          PrintWriter outputToServer = new PrintWriter(clientSocket.getOutputStream(), true);
           
            boolean end=false;
            while (!end) { // Ciclo per invio di comandi e ricezione risposte client-server
              String lineUser = inputTastiera.nextLine(); // Prendo input da tastiera
                if (lineUser.contentEquals("exit")) { // Se inserisco exit esce dopo averlo mandato al server
                  outputToServer.println(lineUser);
                  end = true; 
                  exitms = true;
                  continue;
                }             

              outputToServer.println(lineUser); // Mando l'input inserito (!="exit") al server
              String inputString = inputFromServer.nextLine(); // Risposta **SEMPRE SINGOLA** del server
              System.out.println(inputString);

              // Se ho loggato un utente con successo lato server allora riceverò una unica risposta, quindi
              // unisco il client al gruppo di multicast di cui fa parte il server stesso
                if(inputString.equals("[LOGIN] Utente loggato con successo.")){
                  arrayNotifiche.clear();
                  exitms = false; // Risetto eventualmente a false per riavviare l'ascolto sul gruppo
                  InetAddress group = InetAddress.getByName(multicastHost); // Stesso gruppo del server
                  InetSocketAddress address = new InetSocketAddress(group, multicastPort);
                  NetworkInterface networkInterface = NetworkInterface.getByInetAddress(group);
                  try { // Una volta loggato con successo, se il client non ha ancora joinato, joina il gruppo
                    multicastSocket.joinGroup(address, networkInterface);
                    System.out.println("[CLIENT JOIN] Il client joina il gruppo sociale " + multicastHost);
                    // E si mette in ascolto di info sharate dagli altri da mettere nell'array di notifiche
                    threadListenerUDPs.start();
                  } catch (SocketException e) { 
                    // Se il client aveva già precedentemente joinato allora "joinGroup"
                    // darà errore, che verrà catchato con SocketException
                    System.out.println("[CLIENT WARNING] Il client ha joinato il gruppo sociale precedentemente.");
                    continue;
                  }
                }

              // Se inserisco show me sharing e ricevo la inputString dal ServerWordle senza errore, il che mi ha 
              // confermato la buona riuscita della richiesta di condivisione, stampo l'array di notifiche che mi son man
              // mano salvato con tutte le notifiche sharate dagli altri utenti
                if(inputString.equals("[SHOW ME SHARING] Richiesta condivisione risultati avvenuta con successo.")) { 
                  // Attivo un thread che sta in attesa di ricevere messaggi UDP dal server e che li stampa sul client
                  System.out.println("[SHOW ME SHARING] Notifiche ricevute -> " + arrayNotifiche.toString());
                  
                }

              // Se ho fatto la logout sull'utente devo chiudere la multicast socket per questo client,
              // interrompo il thread che era in ascolto delle notifiche e svuoto l'array di notifiche
                if(inputString.equals("[LOGOUT] Logout effettuato sull'utente.")){
                  multicastSocket.close();
                  exitms = true;
                  System.out.println("[CLIENT WARNING] Client uscito dal gruppo sociale.");
                  continue;
                }

            }

          } catch (IllegalArgumentException e) {
            System.out.println("[CLIENT ERROR] Errore client socket.");
            System.exit(1);
          }
          } catch (NullPointerException | ConnectException e) {
            System.out.println("[CLIENT ERROR] Prima avviare il server!");
            System.exit(1);
          }
    }

  /**
	 * Metodo per leggere il file di configurazione del client.
	 * @throws FileNotFoundException se il file non e' presente
	 * @throws IOException se qualcosa non va in fase di lettura
	 */
	public static void readConfig() throws FileNotFoundException, IOException {
		InputStream input = new FileInputStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        hostname = prop.getProperty("hostname");
        port = Integer.parseInt(prop.getProperty("port"));
        multicastPort = Integer.parseInt(prop.getProperty("multicastPort"));
        multicastHost = prop.getProperty("multicastHost");
        input.close();
	}
}