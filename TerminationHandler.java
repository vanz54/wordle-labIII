// Libreria java
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
// Libreria gson
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

/**
 *	Reti e Laboratorio III - A.A. 2022/2023
 *	Wordle
 *
 *	Classe che implementa l'handler di terminazione per il server.
 *	Questo thread viene avviato al momento della pressione dei tasti CTRL+C.
 *	Lo scopo e' quello di far terminare il main del server bloccato sulla accept()
 *	in attesa di nuove connessioni e chiudere il pool di thread e infine salvare la mappa
 *  per la gestione degli utenti del gioco nel file archive.json.
 *	
 *	@author Tommaso Vanz
 */
public class TerminationHandler extends Thread {
	private int maxDelay; // Settato a 15 sec, aspetterà la pool per 15 sec prima di interrompere bruscamente
	private ExecutorService pool;
	private ServerSocket serverSocket;
	public ConcurrentHashMap<String, Utente> map;
	
	public TerminationHandler(int maxDelay, ExecutorService pool, ServerSocket serverSocket, ConcurrentHashMap<String, Utente> map) {
		this.maxDelay = maxDelay;
		this.pool = pool;
		this.serverSocket = serverSocket;
		this.map = map;
	}
	
	public void run() {
		// Avvio la procedura di terminazione del server.
        System.out.println("[SERVER TERMINATION] Avvio terminazione...");

		// Chiudo la ServerSocket in modo tale da non accettare piu' nuove richieste.
		try {
			serverSocket.close();
			}
		catch (IOException e) {
			System.err.printf("[SERVER ERROR] Errore: %s\n", e.getMessage());
		}
		// Faccio terminare il pool di thread.
		pool.shutdown();
		try {
			if (!pool.awaitTermination(maxDelay, TimeUnit.MILLISECONDS)) 
				pool.shutdownNow();
		} 
		catch (InterruptedException e) {pool.shutdownNow();}
		System.out.println("[SERVER TERMINATION] Terminato.");
		
		// Trasformo la mappa in un json e lo metto nel file archive.json
		System.out.println("[SERVER TERMINATION] Mappa salvata dal TerminationHandler -> " + map);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (FileWriter fileWriter = new FileWriter("archive.json")) {
			gson.toJson(map, fileWriter);
			try {
				fileWriter.close();
			} catch (IOException e1) {
				System.err.printf("[SERVER ERROR] Errore chiusura FileWriter.");
			}
			} catch (JsonIOException | IOException e2) {
				System.err.printf("[SERVER ERROR] Errore trasformazione mappa in json.");
			}			
	}
	
}
