//Libreria java
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reti e Laboratorio III - A.A. 2022/2023
 * Wordle
 * 
 * Il task ServerWordle si occupa di interagire con un utente durante le partite.
 * 
 * Durante una partita, il thread ServerWordle:
 * (1) riceve un comando dall'utente;
 * (2) esegue l'azione richiesta, interagendo opportunamente con le strutture dati;
 * (3) comunica al client l'esito dell'operazione che aveva mandato.
 * 
 * @author Tommaso Vanz
 */

public class ServerWordle implements Runnable {
    // Percorso del file di configurazione del task wordle
	public static final String configFile = "taskwordle.properties";
    // Porta e schema di indirizzamento per identificare il multicast [224.0.0.0-239.255.255.255] 
    public static int multicastPort;
    public static String multicastHost;
    // Socket connessione client-server
    private Socket socket;
    // Multicast socket per creazione gruppo sociale
    public MulticastSocket multicastSocket;
    // Gruppo sociale in comune con i client che ci si collegano dopo la login
    InetAddress group;
    // Mappa per tenere traccia di tutti gli utenti che hanno giocato
    public ConcurrentHashMap<String, Utente> map;
    // Parola da indovinare che si aggiorna ogni 2 minuti nel ServerMain
    private AtomicReference<String> parolaDaIndovinare;
    // Il mio utente sul quale lavoro una volta che ha effettuato la login
    public Utente myActualUser = null;
    // Variabile per vedere se sono ancora loggato con un utente o se ho effettuato la logout
    public boolean stillLogged = false;
    // Risultato della partita del singolo utente da usare per la share
    public String risultatoPartita;

    // Costruttore a cui passo le socket, la mappa già ripristinata e la parola in continuo aggiornamento
    public ServerWordle (Socket socket, MulticastSocket multicastSocket, ConcurrentHashMap<String, Utente> map, AtomicReference<String> parolaDaIndovinare) {
        this.socket = socket;
        this.multicastSocket = multicastSocket;
        this.map = map; 
        this.parolaDaIndovinare = parolaDaIndovinare;
    }
    // InputStream dal client e OutputStream verso client 
    public void run () {
        System.out.println("[SERVER] Apertura client " + socket.getPort());
        // Leggo il file di configurazione.
		try {
            readConfig();
        } catch (IOException e2) {
            System.out.println("[SERVER ERROR] Errore lettura properties.");
        }
        
        // Schema di indirizzamento per identificare il multicast, reperito dalle properties
        try {
            group = InetAddress.getByName(multicastHost);
        } catch (UnknownHostException e1) {
            System.out.println("[SERVER ERROR] Il server non è connesso al gruppo di multicast corretto.");
        }

        // Stampo la mappa letta dal file json, che mi ha memorizzato tutto 
        System.out.println("[SERVER] Mappa all'inizio del ThreadServer -> " + map);

        try (
            Scanner in = new Scanner(socket.getInputStream()); // Recepisce le linee dal client tramite socket
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true) // Restituisce il risultato sull'OutputStream
            ) 
            {   
            while (in.hasNextLine()) {
                end:
                switch (in.nextLine()) {
               
                    
//--------> REGISTER : Registra un nuovo utente all'interno della mappa che contiene tutti i giocatori e le statistiche.
/*                     Controlla che l'utente non sia già esistente e la mappa si aggiorna per ogni client
*/                  case "register":   
                        out.println("[REGISTER] Username:");
                        String myUser_register = in.nextLine();
                        boolean utenteNonesiste = true;

                        if(myUser_register.equals("")){ // Non posso inserire il nulla
                            out.println("[REGISTER ERROR] Inserire username.");
                            break end; 
                        }
                        for(Map.Entry<String,Utente> entry : map.entrySet()) {
                            Utente utente = entry.getValue(); 
                            if(utente.getUsername().equalsIgnoreCase(myUser_register)){
                                out.println("[REGISTER ERROR] Utente esistente, devi effettuarci il login con tale utente.");
                                utenteNonesiste = false;
                                break end;
                            }
                            continue;
                        }
                        // A questo punto sono sicuro che il mio utente che registro sia "nuovo"
                        if (utenteNonesiste) {
                            out.println("[REGISTER] Password:");
                            String myPass_register = in.nextLine();

                            Utente myUtente = new Utente(myUser_register, myPass_register, 0, 0, 0, 0, 0);
                            map.put(myUser_register + " ", myUtente); // Inserisco il nuovo utente nella mappa
                        }
                        System.out.println("[SERVER] Mappa aggiornata dopo una register -> " + map);
                        out.println("[CLIENT] Inserisci prossima azione.");
                        break end;


//--------> LOGIN : Login dell'utente con il quale giocherò, l'utente che decido di loggare deve essere
/*                  presente all'interno della mappa degli utenti e nel client attuale nessun altro
                    utente deve essere momentaneamente loggato, dopo la logout posso fare altre login
*/                  case "login": 
                        // Al primo login il mio user sarà null, se faccio dei login successivamente mi basta non sia nessun altro loggato al momento
                        if(myActualUser==null || (myActualUser!=null && !myActualUser.logged)){
                            out.println("[LOGIN] Username:");
                            String myUser_login = in.nextLine();
                            boolean utenteEsiste = false;

                            for(Map.Entry<String,Utente> entry : map.entrySet()) {
                                Utente utente = entry.getValue();
                                if(utente.getUsername().equalsIgnoreCase(myUser_login) && !utenteEsiste){ 
                                    // Se un utente nella mappa è uguale all'username che ho inserito, controllo la password
                                    utenteEsiste = true;

                                    out.println("[LOGIN] Password:");
                                    String myPass_login = in.nextLine();
                                    // Controllo la password, se combacia il login è effettuato con successo
                                    if(utente.getPassword().equalsIgnoreCase(myPass_login)){
                                        out.println("[LOGIN] Utente loggato con successo.");
                                        // Inizializzo l'utente che userò in quel momento
                                        myActualUser = utente;
                                        myActualUser.logged = true;
                                        // Se il login ha avuto successo il client si unisce al gruppo di multicast uguale al server (lato client)
                                        break end;
                                    } else {
                                    out.println("[LOGIN ERROR] Password errata, rieffettuare il login.");
                                    break end;
                                    }
                                }
                            }
                            if(!utenteEsiste){
                                out.println("[LOGIN ERROR] Utente non esistente, devi effettuare la register.");
                                break end;
                            }
                            out.println("[CLIENT] Inserisci prossima azione.");
                            break end;
                        } else { // Se dopo una logout volessi loggarmi con un altro utente
                            out.println("[LOGIN ERROR] Sei loggato con un altro account.");
                            break end;
                        }


//--------> LOGOUT : Disconetto l'utente attuale dal gioco, non potrà più giocare a meno che non rilogghi e siano 
/*                   sempre attive le condizioni per giocare   
*/                  case "logout":
                        try {
                            if(myActualUser!=null && myActualUser.logged) {
                                // Prima di eseguire il logout setto l'ultima winstreak ed eventualmente aggiorno la massima winstreak
                                myActualUser.calcolaWinstreak();
                                // E aggiungo i tentativi effettuati
                                Integer tentativiEffettuati = myActualUser.getTentativiFatti(); 
                                // Mi salvo i tentativi fatti in questa partita e li metto nell'array
                                myActualUser.addTentativoToArray(tentativiEffettuati);
                                // Setto che non sono più loggato con nessun utente
                                myActualUser.logged = false;
                                // Ripristino la verifica se le partite son terminate
                                myActualUser.partitaTerminata = false;
                                out.println("[LOGOUT] Logout effettuato sull'utente.");
                                break end;
                            } else { 
                                out.println("[LOGOUT ERROR] Non sei loggato con nessun utente.");
                                break end;
                            }
                        } catch (NullPointerException e) {
                            break end;
                        }


//--------> PLAY WORDLE : richiesta d'iniziare il gioco indovinando l’ultima parola estratta dal 
/*                        server, controlla se l’utente ha già partecipato al gioco per quella parola
*/                  case "play wordle":
                    case "play":
                        if(myActualUser!=null && myActualUser.logged){
                            if(!(myActualUser.getHaGiaPartecipato())) {
                                myActualUser.partecipa();
                                out.println("[PLAY] L'utente " + myActualUser.getUsername() + " inizia a giocare.");
                                break end;
                            } else {
                                out.println("[PLAY ERROR] L'utente " + myActualUser.getUsername() + " ha partecipato per la parola corrente.");
                                break end;
                            }
                        } else {
                            out.println("[PLAY ERROR] Prima devi loggarti.");
                            break end;
                        }


//--------> SEND WORD : Invio della parola da parte dell'utente, con un massimo di 12 tentativi, la parola si
/*                      aggiorna ogni 5 minuti, il client riceverà dal server degli indizi per riuscire a 
                        far indovinare all'utente la parola più facilmente, nel mentre aggiorna le opportune
                        statistiche dell'utente che prosegue col gioco
*/                  case "send word":
                    case "send":
                        if(myActualUser!=null && myActualUser.logged){
                            if(myActualUser.puoGiocare == true && myActualUser.getTentativiFatti()<12) {
                                out.println("[SEND] Inserisci la parola che pensi sia corretta:");
                                String myGuessedWord = in.nextLine();
                                // Se l'utente indovina la parola
                                if(myGuessedWord.equals(parolaDaIndovinare.toString())) {
                                    out.println("[USER WIN] HAI VINTO! La parola da indovinare era '" + parolaDaIndovinare.toString() + "'.");
                                    myActualUser.vittorie++; // Incremento il numero di vittorie
                                    myActualUser.tentativi++; // Incremento il numero di tentativi (questo sarà l'incremento finale)
                                    myActualUser.myWinStreak++; // Incremento la winstreak attuale
                                    Integer tentativiEffettuati = myActualUser.getTentativiFatti(); // Mi salvo i tentativi fatti in questa partita per guessare la parola 
                                    myActualUser.addTentativoToArray(tentativiEffettuati); // Li metto nell'array
                                    myActualUser.calcolaWinstreak(); // Calcolo le 2 winstreak delle stats e vedo se vanno aggiornate
                                    myActualUser.puoGiocare = false; // L'utente che ha vinto non può giocare fino alla prossima parola
                                    // Se ha vinto la partita è terminata con successo
                                    myActualUser.haVinto = true;
                                    myActualUser.partitaTerminata = true;
                                    break end;
                                } else if(!(checkWord(myGuessedWord)) || (myGuessedWord.length()!=10) ) {
                                    // Se l'utente inserisce una parola che non è presente nel vocabolario / inesistente in generale (<10 lettere)
                                    // do un warning senza incrementare i tentativi
                                    out.println("[SEND WARNING] La parola '" + myGuessedWord + "' non esiste nella lista di parole possibili. (Tentativi effettuati: " + myActualUser.tentativi + ")");
                                    break end; 
                                } else { // Se la parola esiste nel vocabolario ma non è quella da indovinare
                                    myActualUser.tentativi++;
                                    // La parola guessata è presente nel vocabolario, fornisco indizi al client
                                    char[] a = myGuessedWord.toCharArray();  // Parola guessata in array di chars
                                    char[] b = parolaDaIndovinare.toString().toCharArray(); // Parola da indovinare in array di chars

                                    // Costruzione dell' indizio comparando i due chars array e fornendone un terzo come indizio
                                    char[] hint = new char[10]; 
                                    for (int i = 0; i < a.length; i++) {
                                        if(a[i] == b[i]){
                                            hint[i] = '+';
                                            continue;
                                        } else {
                                            forloop: 
                                            for (int j = 0; j < b.length; j++) {
                                                if (a[i] == b[j]) {
                                                    hint[i] = '?';
                                                    break forloop;                                    
                                                }
                                            }
                                        }
                                    }
                                    for (int k = 0; k < hint.length; k++) {
                                        if(hint[k] != '+' && hint[k] != '?'){
                                            hint[k] = 'X';
                                        }
                                    } 
                                    // + vuol dire che la lettera è al posto giusto
                                    // X vuol dire che la lettera non c'è
                                    // ? vuol dire che la lettera è in un altra posizione
                                    System.out.println("[SERVER] Indizio al client -->" + Arrays.toString(hint));
                                    out.println("[SEND HINT] '" + myGuessedWord + "' parola ERRATA : (Tentativi effettuati: " + myActualUser.tentativi + "). " + "[INDIZIO] --> " + Arrays.toString(hint));
                                    break end;   
                                }
                            } else if(myActualUser.getTentativiFatti()==12) {
                                    out.println("[SEND ERROR] Numero massimo di tentativi raggiunto.");
                                    // La partita è terminata senza che l'utente abbia indovinato
                                    myActualUser.addTentativoToArray(12);
                                    myActualUser.partitaTerminata = true;
                                    myActualUser.haPerso = true;
                                    break end;  
                            } else { // Se non faccio prima play di send
                                out.println("[SEND ERROR] L'utente deve mostrare prima l'intenzione di voler giocare.");
                                break end; 
                            }
                        } else {
                            out.println("[SEND ERROR] Prima devi loggarti.");
                            break end;
                        }


//--------> SEND ME STATISTICS : Mi manda le statistiche aggiornate sull'utente corrente
/*                               Partite giocate, Percentuale vittorie, Ultima winstreak, 
                                 Massima winstreak, Guess Distribution
*/                  case "send stats":             
                    case "send me statistics":
                        if(myActualUser!=null && myActualUser.partiteGiocate!=0 && myActualUser.logged) {
                            try {
                                out.println("[STATS] Statistiche " + myActualUser.getUsername() + ": |Partite giocate->[" + myActualUser.partiteGiocate + "]| |Percentuale vittorie->[" + ((float)myActualUser.vittorie/(float)myActualUser.partiteGiocate)*100 + "%]| |Ultima winstreak->[" + myActualUser.lengthLastWinstreak  + "]| |Massima winstreak->[" + myActualUser.lengthMaxWinstreak  + "]| |Guess distribution->[array tentativi]=" + myActualUser.arrayTentativi.toString() + "--[media tentativi]=[" + myActualUser.calcolaDistribution() + "]|");
                                break end;
                            } catch (NullPointerException e) {
                                out.println("[STATS ERROR] Errore caricamento statistiche per l'utente selezionato.");
                                break end; 
                            }
                        } else {
                            out.println("[STATS ERROR] Statistiche non disponibili per chi non gioca.");
                            break end; 
                        }
             

//--------> SHARE : Condivide il risultato della partita appena effettuata e il numero di tentativi impiegato per 
/*                  indovinare la parola sul multicast ai client che vogliono riceverlo avendo effettuato 
                    "show me sharing"
*/                  case "share":
                        if(myActualUser!=null && myActualUser.partiteGiocate!=0 && myActualUser.logged && myActualUser.partitaTerminata) {
                            System.out.println("[SERVER] Condivido risultati ultima partita di " + myActualUser.getUsername());
                            if(myActualUser.haVinto) { // Vedo il risultato della partita
                                risultatoPartita = "vinto";
                            } else if(!myActualUser.haVinto && myActualUser.haPerso) {
                                risultatoPartita = "perso";
                            } 
                            // Creo la stringa UDP da mandare ai client che han joinato il multicast
                            String stringSendUDP = "|L'utente " + myActualUser.getUsername() + " ha " + risultatoPartita + " con " + myActualUser.tentativi + " tentativi|";
                            // Mando il risultato della partita ai client che hanno joinato la multicast (solo quei client che hanno un utente loggato)
                            byte[] buf = stringSendUDP.getBytes();
                            DatagramPacket shareRes = new DatagramPacket(buf, buf.length, group, multicastPort);
                            multicastSocket.send(shareRes);
                            out.println("[SHARE] Risultato condiviso con successo.");
                            System.out.println("[SERVER] Risultati condivisi ai client: " + stringSendUDP);
                            break end;
                        } else {
                            out.println("[SHARE ERROR] Condivisione risultati non riuscita.");
                            break end; 
                        }

//--------> SHOW ME SHARING :  Mostra sul terminale le notifiche di altri client con altri utenti sopra che stanno 
/*                             giocando e hanno condiviso le loro partite tramite "share", una volta mandata la 
                               risposta al client attuale, si avvierà un thread lato client che riceverà gli UDP
                               dal multicast su cui sono tutti i client che condividono, fondamentalmente 
                               questo metodo serve solo per mandare un feedback, avviene quasi tutto lato client.  
*/                  case "show me sharing":  
                    case "show shares":
                        if(myActualUser!=null && myActualUser.logged){
                            out.println("[SHOW ME SHARING] Richiesta condivisione risultati avvenuta con successo.");
                            break end;
                        } else {
                            out.println("[SHOW ME SHARING ERROR] Devi prima loggarti per permettere al client di unirsi al multicast.");
                            break end;
                        }
                        
//--------> REMOVE USER : Rimuove un utente facendo tutti gli appropriati controlli, 
//                        solo se loggato e sull'utente attuale
                    case "remove user":
                        if(myActualUser!=null && myActualUser.logged){
                            out.println("[REMOVE] Username:");
                            String myUser_remove = in.nextLine();
                            boolean utenteEsistente = false;


                            for(Map.Entry<String,Utente> entry : map.entrySet()) {
                                Utente utente = entry.getValue(); 
                                // Se è uguale all'username che ho inserito e a quello su cui sono loggato, controllo la password
                                if(utente.getUsername().equalsIgnoreCase(myUser_remove) && utente.getUsername().equalsIgnoreCase(myActualUser.getUsername()) && !utenteEsistente){ 
                                    utenteEsistente = true;

                                    out.println("[REMOVE] Password:");
                                    String myPass_remove = in.nextLine();

                                    if(utente.getPassword().equalsIgnoreCase(myPass_remove)){
                                        map.remove(entry.getKey());
                                        out.println("[REMOVE] Utente rimosso con successo.");
                                        System.out.println(map);
                                        myActualUser.logged = false;
                                        break end;
                                    } else {
                                    out.println("[REMOVE ERROR] Password errata.");
                                    break end;
                                    }
                                }
                            }
                            if(!utenteEsistente){
                                out.println("[REMOVE ERROR] Utente non esistente.");
                                break end;
                            }
                            break end;
                        } else {
                            out.println("[REMOVE ERROR] Prima devi loggarti.");
                            break end;
                        }

//--------> EXIT : Chiude il client.
                    case "exit":
                        System.out.println("[SERVER] Chiusura client " + socket.getPort());
                        break end;

// ****CASISTICHE SEGRETE CHE MI AIUTANO IN ALCUNE CASISTICHE DURANTE LA PROGRAMMAZIONE****

            //CHEAT
                    case "CHEAT":
                        out.println("[CLIENT] Sei un cheater, la parola da indovinare è: " + parolaDaIndovinare.toString());
                        break end;
            
            //UNLOG USER
                    case "UNLOG USERS":
                        if(myActualUser!=null && myActualUser.logged)
                            myActualUser.logged = false;
                        out.println("[CLIENT] Chiunque fosse loggato è stato sloggato.");
                        break end;
                            
            //RESET MY STATS
                    case "reset my stats":
                        if(myActualUser!=null && myActualUser.logged){
                            myActualUser.haPartecipato = false;
                            myActualUser.puoGiocare = false;
                            myActualUser.logged = false;
                            myActualUser.partitaTerminata = false;
                            myActualUser.tentativi = 0;
                            myActualUser.partiteGiocate = 0;
                            myActualUser.percentualeVittorie = 0;
                            myActualUser.lengthLastWinstreak = 0;
                            myActualUser.lengthMaxWinstreak = 0;
                            myActualUser.guessdistribution = 0;
                            myActualUser.vittorie = 0;
                            myActualUser.myWinStreak = 0;
                            myActualUser.arrayTentativi = new ArrayList<Integer>();
                            out.println("[CLIENT] Statistiche resettate.");
                            break end;
                        } else {
                            out.println("[RESET ERROR] Prima devi loggarti.");
                            break end;
                        }

                    default:
                        out.println("[CLIENT WARNING] Comando non esistente, inserisci una delle azioni possibili.");
                        break;
                    
                }
            }

            
            } catch (NullPointerException e) {
                System.err.printf("[SERVER ERROR] Nessun utente in gioco.");
            }
            catch (Exception e) {
                System.err.printf("[SERVER ERROR] Errore ServerWordle");
                e.printStackTrace();
                System.exit(1);
            }
    }

    /**
	* Metodo che controlla se una data parola che gli passo è nel file words.txt .
	*/
    public boolean checkWord(String myWord) throws FileNotFoundException {
        if (new Scanner(new File("words.txt")).useDelimiter("\\Z").next().contains(myWord)) {
            return true;
        } else {
            return false;
        }
    }

    /**
	* Metodo che legge il file e lo trasforma in una stringa.
	*/
    public String readFileAsString(String file)throws Exception{
        return new String(Files.readAllBytes(Paths.get(file)));
    }

    /**
	 * Metodo che legge il file di configurazione del task wordle.
	 * @throws FileNotFoundException se il file non esiste
	 * @throws IOException se si verifica un errore durante la lettura
	 */
	public static void readConfig() throws FileNotFoundException, IOException {
		InputStream input = new FileInputStream(configFile);
		Properties prop = new Properties();
		prop.load(input);
        multicastPort = Integer.parseInt(prop.getProperty("multicastPort"));
        multicastHost = prop.getProperty("multicastHost");
		input.close();
	}


}