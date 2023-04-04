// Libreria java
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
// Libreria gson
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Reti e Laboratorio III - A.A. 2022/2023
 * Wordle
 * 
 * Classe Java che rappresenta il server del gioco Wordle.
 * Interagisce con i client tramite un pool di thread chiamando un task che svolge la maggior parte
 * del lavoro di interazione col client, ad ogni richiesta corrisponde una risposta e delle conseguenze gestite
 * all'interno del ServerWordle, gestisce, salva e ripristina gli utenti che hanno giocato, giocano e giocheranno
 * tramite il file archive.json e una mappa.
 * 
 * @author Tommaso Vanz
 */

public class ServerMain {
    // Percorso del file di configurazione del server.
	public static final String configFile = "server.properties";
	// Porta di ascolto del server.
	public static int port;
    // Numero di thread utilizzati
	public static int nThread;
    // Tempo massimo di attesa (in ms) per la terminazione del server.
	public static int maxDelay;
    //  keepAliveTime T, se nessun task viene sottomesso entro T, il thread termina la sua esecuzione, riducendo così il numero di threads del pool
	public static int terminationDelay;
    // Distanza tra la generazione di due parole da cercare a 5 minuti (300000 ms)
	public static int timeBetweenWords;
    // Mappa
    public static ConcurrentHashMap<String, Utente> map;
    // Parola chiave da indovinare, si aggiorna ogni tot grazie ad atomic reference, la inizializzo sennò da errore causa null
    public static AtomicReference<String> keyWord = new AtomicReference<String>("initialKeyWord");

    public static void main(String[] args) throws Exception {
        try {
        // Leggo il file di configurazione.
		readConfig();
        
        // Faccio una try with resources su 3 elementi:
        // Welcome socket del server, apro listening socket su 1609 con una try with resources e resto in attesa di richieste
        // RandomAccessFile in modalita' lettura per scegliere randomicamente una stringa che sarà la parola chiave
        // Socket relativa al multicast per implementazione del gruppo sociale con i client
        try (ServerSocket serverSocket = new ServerSocket(port);
            RandomAccessFile f = new RandomAccessFile("words.txt", "r");
            MulticastSocket multicastSocket = new MulticastSocket();
            ) 
            { 
            
            System.out.println("[SERVER] Server online.");
            System.out.printf("[SERVER] In ascolto sulla porta: %d\n", port);
            System.out.printf("[SERVER] Inizializzazione gruppo sociale sull'host 226.226.226.226\n");

            // Creazione threadpool con "coda" per la gestione dei thread """infinita"""
            BlockingQueue<Runnable> coda = new LinkedBlockingQueue<Runnable>();
            ExecutorService pool = new ThreadPoolExecutor( 
            nThread, 
            nThread, 
            terminationDelay, 
            TimeUnit.MILLISECONDS,
            coda, 
            new ThreadPoolExecutor.AbortPolicy() 
            );

            // Creazione ConcurrentHashMap per la gestione degli utenti tramite
            // ripristino della mappa salvata negli utilizzi del server precedenti all'interno del file archive.json
            // Prendo quindi il json file che ha tutti i dati degli utenti e lo porto in un formato mappa
            String file = "archive.json";
            String jsonString = readFileAsString(file);
            if(jsonString.equals("") || jsonString.equals(null)){ // Inizializzo la mappa al primo avvio del server
                System.out.println("[SERVER] Inizializzo la mappa -> " + map);
                map = new ConcurrentHashMap<String, Utente>();
            } else { // Altrimenti se ho una mappa già ben formata, l'avevo salvata al precedente spegnimento e ora la ripristino
                map = new Gson().fromJson(jsonString, new TypeToken<ConcurrentHashMap<String, Utente>>() {}.getType());
                System.out.println("[SERVER] Mappa ripristinata dal MainServer -> " + map);
            }

            // Utilizzo handler di terminazione, ad esempio se terminassi il server con CTRL+C mi salva una mappa
            // con tutti gli utenti e le caratteristiche nel file json "archive.json"
            Runtime.getRuntime().addShutdownHook(new TerminationHandler(maxDelay, pool, serverSocket, map));

            // Timer che ogni tot secondi reimposta la possibilità di giocare e aggiorna la parola da indovinare
            // tempo tra una parola e l'altra reperito da server.properties
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                public void run(){
                    // Scelta stringa da usare come parola da indovinare.
                    byte[] bufferString = new byte[10];
                    int randomNum = ThreadLocalRandom.current().nextInt(0, 30824 + 1);
                    try{
                        // Il file parte dal byte 0, la seconda parola si trova all'11° bit e così via (parole di 10 lettere)
                        f.seek(randomNum * 11); 
                        f.read(bufferString);
                    } catch (IOException e) { 
                        System.out.println("[SERVER ERROR] Errore lettura parola chiave.");
                    }
                    // Stampo e setto la nuova stringa da cercare
                    String newString = new String(bufferString);
                    keyWord.set(newString);
                    System.out.println("[SERVER] Chiave da indovinare ---> " + keyWord.toString());	
                    
                    // Setto tutte le variabili degli utenti che magari han già partecipato ad una parola precedente
                    // a false, ripristino delle cose basilari, in modo che possano rigiocare con la nuova parola 
                    for(Map.Entry<String,Utente> entry : map.entrySet()) {
                        Utente utente = entry.getValue();
                        utente.tentativi = 0;
                        utente.myWinStreak = 0;
                        utente.haPartecipato = false;
                        utente.partitaTerminata = false;
                        utente.puoGiocare = false;
                        utente.logged = false;
                        utente.haVinto = false;
                        utente.haPerso = false;
                    }
                }
            }; // Schedula l'aggiornamento delle parole partendo subito e aspettando 2 minuti tra le parole
            timer.scheduleAtFixedRate(task, 0, timeBetweenWords);


            // Ciclo gestione dei client con un pool di thread
            while (true) {
                Socket socket = null;
                // Accetto le richieste provenienti dai client
                // Quando il TerminationHandler chiude la ServerSocket, si solleva una SocketException ed esco dal ciclo
                try {
                    socket = serverSocket.accept();
                } catch (SocketException e) {
                    break;
                }
                // Avvio un ServerWordle per interagire con il client.
                pool.execute(new ServerWordle(socket, multicastSocket, map, keyWord));  
            } // Gli passo anche  la mcSocket almeno combacia con tutti i client, la mappa con tutti gli utenti e la parola da indovinare

            } catch (IllegalArgumentException e) {
                System.err.printf("[SERVER ERROR] Errore: Porta errata");
                e.printStackTrace();
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.printf("[SERVER ERROR] Errore ServerMain");
            System.exit(1);
        }
    }

    /**
	 * Metodo che legge il file di configurazione del server.
	 * @throws FileNotFoundException se il file non esiste
	 * @throws IOException se si verifica un errore durante la lettura
	 */
	public static void readConfig() throws FileNotFoundException, IOException {
		InputStream input = new FileInputStream(configFile);
		Properties prop = new Properties();
		prop.load(input);
		port = Integer.parseInt(prop.getProperty("port"));
        nThread = Integer.parseInt(prop.getProperty("nThread"));
		maxDelay = Integer.parseInt(prop.getProperty("maxDelay"));
        terminationDelay = Integer.parseInt(prop.getProperty("terminationDelay"));
        timeBetweenWords = Integer.parseInt(prop.getProperty("timeBetweenWords"));
		input.close();
	}

    /*
     * Metodo per leggere il file passato come fosse una stringa.
     */
    public static String readFileAsString(String file)throws Exception{
        return new String(Files.readAllBytes(Paths.get(file)));
    }
    
}


// Backup mappa utile se si vuole iniziare già con qualche utente
/*
{
  "tommy ": {
    "username": "tommy",
    "password": "vanz",
    "haPartecipato": false,
    "puoGiocare": false,
    "logged": false,
    "partitaTerminata": false,
    "haVinto": false,
    "haPerso": false,
    "tentativi": 0,
    "partiteGiocate": 0,
    "percentualeVittorie": 0,
    "lengthLastWinstreak": 0,
    "lengthMaxWinstreak": 0,
    "guessdistribution": 0,
    "vittorie": 0,
    "myWinStreak": 0,
    "arrayTentativi": [
      1,
      2,
      3,
      1
    ]
  },
  "sonia ": {
    "username": "sonia",
    "password": "delgiu",
    "haPartecipato": false,
    "puoGiocare": false,
    "logged": false,
    "partitaTerminata": false,
    "haVinto": false,
    "haPerso": false,
    "tentativi": 0,
    "partiteGiocate": 0,
    "percentualeVittorie": 0,
    "lengthLastWinstreak": 0,
    "lengthMaxWinstreak": 0,
    "guessdistribution": 0,
    "vittorie": 0,
    "myWinStreak": 0,
    "arrayTentativi": [
      10,
      8,
      5,
      1
    ]
  }
}
*/

//Utente utente1 = new Utente("lala","lolo", 0, 0, 0, 0, 0);
//map.put("lala ",utente1);
//Utente utente0 = new Utente("tommy","vanz", 0, 0, 0, 0, 0);
//map.put("tommy ",utente0);