# wordle-labIII
### Leggere "Relazione progetto Wordle - Tommaso Vanz.pdf" per una visione completa del progetto in formato pdf
Progetto universitario che ricrea il gioco 'Wordle' valido per l'esame di Laboratorio III. 

Il repository contiene le classi principali che hanno permesso la realizzazione del progetto copnsegnato all'esame, qui sotto nel `READ.me` è spiegata la struttura e il funzionamento del progetto.

# Introduzione
Wordle è un progetto basato su un programma Client-Server che fornisce la possibilità a diversi utenti di giocare al giochino di
parole omonimo divenuto celebre qualche anno fa. Gli utenti potranno, dopo essersi connessi al server di gioco, giocare tentando
di indovinare la parola fornita ciclicamente dal server stesso, l’obbiettivo degli utenti sarà cercare di indovinarla anche grazie agli
aiuti forniti dal gioco, per scalare le classifiche e condividere i propri risultati con altri giocatori.

## Struttura del progetto
L’architettura generale del sistema è basata sul paradigma Client-Server: gli utenti del servizio possono accedere al sistema
mediante un Client, che invia le richieste ad un Server (in rete locale) che le elabora e restituisce le informazioni richieste dal
Client. Il progetto ricalca ampiamente l’idea dietro al compito Dungeon Adventures, dove il client, mandando richieste tramite linea
di comando, riceveva relative risposte che permettevano il proseguimento del gioco in varie direzioni, stessa cosa in questo
progetto: il Client può eseguire una serie di comandi che lo interfacceranno ad una sequenza di botta e risposta col server stesso,
dando così ‘vita’ al gioco in sé.
Il progetto è stato suddiviso in 5 classi principali che si trovano all’interno di `src`:
- `ServerMain.java` → classe alla base del server, reperisce i parametri di input dal file di
configurazione `server.properties` , ripristina la mappa da `archive.json` che contiene e mantiene i vari
giocatori e gestisce un pool di thread per l’interazione con i client.
- `ClientMain.java` → classe che si occupa del client, permette l’interazione uomo-client grazie ai
comandi passati da linea di comando e interfaccia questa comunicazione col server per la ricezione
di risposte che verranno stampate per far capire all’utente come sta proseguendo il gioco e il
risultato delle azione che sta compiendo.
- `ServerWordle.java` → classe task lanciata ogni qual volta la pool di thread esegue, è la vera e propria
“interfaccia” tra server e client in quanto riceve il comando da quest’ultimo, esegue una determinata
azione di conseguenza e riferisce la risposta al client stesso.
- `TerminationHandler.java` → classe gestore della terminazione corretta lato server, interrompendo il
server con CTRL+C entrerà in esecuzione permettendo di salvare la struttura dati che contiene gli
utenti, rendendo quindi di fatto questa struttura persistente e riutilizzabile in un avviamento del
server successivo.
- `Utente.java` → classe che tiene traccia di tutte le statistiche e informazione dell’utente in gioco, è
utilizzata come base di appoggio per la gestione degli utenti dal server.

Librerie in `lib` :
Utilizzo la libreria `gson-2.8.2.jar` per la serializzazione e deserializzazione della mappa che contiene gli utenti, all’interno del file
`archive.json` , garantendo così persistenza delle informazioni.

Cartella `bin` è la cartella contenente le classi compilate e i file fondamentali per la buona riuscita del programma:
- `words.txt` → file contenente tutte le parole possibili scrivibili nel gioco, sono 30824 parole lunghe 10 caratteri, una di esse verrà ciclicamente estratta dal server.
- `server.properties` → file di configurazione del server, contiene
la porta per la socket lato server, il numero di thread gestiti
dalla threadpool, il tempo massimo prima della shutdown
forzata della threadpool nel TerminationHandler, il
keepAliveTime della threadpool e il tempo tra una parola da
indovinare e l’altra, il server legge tutto ciò dal file grazie alla
readConfig().
- `files.class` → file ottenuti dalla compilazione dei file sorgenti Java,
sono l’eseguibile delle classi che formano il programma.
- `server.properties` → file di configurazione del server, contiene
la porta per la socket lato server, il numero di thread gestiti
dalla threadpool, il tempo massimo prima della shutdown
forzata della threadpool nel TerminationHandler, il
keepAliveTime della threadpool e il tempo tra una parola da
indovinare e l’altra, il server legge tutto ciò dal file grazie alla
readConfig().
- `client.properties` → file di configurazione del server,
contiene nome dell’host, porta uguale a quella del
server, porta ed host per la multicastSocket per unirsi
al gruppo sociale. Il client leggerà tutte queste info
invocando una readConfig().
- `taskwordle.properties` → file di configurazione del task del
server, che contiene solo le informazioni per
l’implementazione multicast lato server in modo da inviare
messaggi ai partecipanti al gruppo sociale.
- `archive.json` → file json contenente la stringa json che
rappresenta la mappa degli utenti che hanno giocato o
sono solo registrati al gioco Wordle.

## Scelte effettuate nel progetto
Il mio progetto ha una struttura Client-Server: per prima cosa va avviato il server che si metterà in ascolto sulla porta passata con i
parametri di configurazione, sucessivamente posso avviare uno o più client che saranno l’interfaccia tra il server e l’utente
giocante.
L’idea è che il client stia in ascolto delle richieste passate da linea di comando dall’utente e il ServerWordle task mandi le risposte
in base a che richiesta ha ricevuto.
Fondamentalmente tutto il lavoro di botta-risposta tra client e server è gestito dal task ServerWordle e ClientMain.
Di seguito le classi “core” del progetto spiegate:
- Un’altra azione molte importante, cruciale per
l’immagazzinamento e la buona riuscita di più partite
consecutive è il ripristino della mappa contenente i vari
utenti, essa viene ripristinata dal file archive.json letto
come stringa json. Se tale stringa è vuota, vuol dire
che non ci sono utenti ancora registrati al servizio di
gioco e quindi inizializzo una ConcurrentHashMap
vuota, altrimenti la ripristino utilizzando le funzionalità
di gson.
- Invece il salvataggio della struttura dati degli utenti è affidato al
TerminationHandler, esso è un thread che viene inizializzato subito
dopo aver ripristinato la mappa nel ServerMain MA va in esecuzione
solamente allo spegnimento del server e\o alla sua brusca interruzione
con CTRL+C, il TerminationHandler, è praticamente lo stesso visto a
lezione, in aggiunta ha solo il meccanismo di salvataggio della mappa,
che viene serializzata e messa nel file json di archivio degli utenti.
- Prima del ciclo di ascolto di richieste affidato alla
threadpool (inizializzata con un ThreadPoolExecutor),
viene avviato un TimerTask, che ogni 5 minuti mi
scansiona il file di parole da indovinare scegliendone
una in particolare, aggiornandola rispetto alla
precedente e resettando alcune variabili-Utente per
poter permettergli di rigiocare con la nuova parola (la
chiave da indovinare è una Atomic Reference, quindi
anche se un client era già in esecuzione, la parola da
indovinare viene aggiornata istantaneamente)
Una volta che il server ha le socket, la mappa di utenti
e la parola da indovinare, il server entra in un loop
dove accetta le richieste di connessione provenienti
dal client, utilizzando un pool di thread, passandogli il
task ServerWordle che si occuperà della interazione
effettiva.

## ClientMain
Il client è il mezzo tra utente umano e server. Anch’esso riceve i parametri di configurazione da un file di proprietà nella cartella bin
leggendoli tramite la readConfig(); anche il client con una try with resources (utile per chiudere autonomamente le risorse) si
inizializza la socket verso il server, la multicastSocket tramite la quale si legherà al gruppo sociale e due importantissimi Scanner,
uno per prender l’input dall’umano tramite linea di comando, l’altro come input dal ServerWordle tramite uno stream di input per
ricevere le risposte del server.
Il client può inserire un insieme ben definito di comandi da mandare al server (da ora in poi mi riferirò al server non come
ServerMain bensì come struttura che riceve richieste e manda risposte grazie al ServerWordle task), inserire comandi diversi
comporta al ricevere un messaggio di risposta di default che indica di cambiare comando.
Lista di comandi :
- `register`
- `login`
- `logout`
- `play wordle` (abbreviazione `play`)
- `send word` (abbreviazione `send`)
- `send me statistics` (abbreviazione `send stats`)
- `share`
- `show me sharing` (abbreviazione `show shares`)
- `remove user`
- `exit`
Tutto ciò avviene sempre all’interno del ciclo while di ascolto di risposte. Il client manda *UNA STRINGA* come comando e riceve
*UNA STRINGA* come risposta, ho deciso di riutilizzare il paradigma usato nel Dungeon Adventure poiché non era necessaria la
ricezione o invio di più di una stringa per volta.

In base a determinati tipi di risposte, il client effettuerà ulteriori azioni:
- Se il client richiede ed effettua un `login` ed essa avviene
con successo, allora esso si legherà al gruppo sociale sul
quale sono presenti il server ed eventuali altri client,
joinandolo. Non solo, ma avvierà anche un thread
(threadListenerUDPs) per stare in ascolto di eventuali
notifiche UDP mandate da altri client che hanno effettuato
una `share`.
- Il thread immagazzina le eventuali notifiche ricevute in un array
di notifiche e solamente se il client stesso effettuasse una `show
me sharing` per voler vedere le notifiche degli altri utenti, l’array
di notifiche verrebbe restituito
- Se un utente, al contrario, effettuasse una logout, allora il client
chiuderebbe la comunicazione col gruppo sociale ed uscirebbe dal
thread che era in attesa di notifiche UDP.

## ServerWordle
Il grosso del lavoro del gioco è svolto in questa classe, ne viene creata un’istanza ogni qual volta un client si connette al server,
tutto ciò grazie al pool di thread e alla socket. Il task riceve dal server la socket di connessione col client (fondamentale per riceve i
comandi in input e mandare le risposte in output), la multicast socket per istanziare il gruppo sociale in quanto farà le veci di server
“inviatore di notifiche di gioco”, la mappa aggiornata con i vari utenti e la parola da indovinare che si aggiorna atomicamente
essendo una AtomicReference.
Tutto ciò che fa è contenuto in un grosso metodo run(): essendo un task, per prima cosa come il client e il server, si legge dal file di
configurazione la porta e l’host multicast, quest’ultimo lo utilizza subito per istanziare il gruppo sociale corretto (vi manderà solo
messaggi UDP, non lo joina);
Poi in una try with resources inizializza l’input per ricevere i
SINGOLI comandi dal client e l’output per mandare SINGOLE
stringhe di risposta al client e inizia a svolgere il compito
principale: un while dove reperisce continuamente i
comandi che gli arrivano dal client e tramite uno switch, in
base a che comando è stato inviato dal client, effettuerà
una data azione.
Lato server naturalmente i comandi sono gli stessi del lato client, in aggiunta sono nascosti 3 comandi “speciale” che può ricevere
lo stesso che mi hanno aiutato durante la scrittura del programma: `CHEAT` , `UNLOG USERS` e `reset my stats` (questi non sono ‘visibili’
client-side).
I restanti comandi visualizzati anche all’accensione del client, mi permettono l’implementazione del gioco:
- `register` : mi permette di registrare un nuovo utente alla ConcurrentHashMap chiedendo prima username e poi password uno
dietro l’altro, controllando che sia un utente “nuovo” e non nullo, se va tutto a buon fine inserisce il nuovo utente nella mappa

- `login` : consente all’umano di loggarsi con un dato utente, esso deve essere presente nella mappa e la password deve essere
corretta, una volta loggato, l’utente lato cliente joinerà il gruppo sociale, l’utente appena loggato diventerà l’utente a cui farà
riferimento il ServerWordle su cui è stata fatta la login.

- `logout` : scollega l’utente attualmente loggato dal gioco, lato client chiuderà la socket relativa al multicast ed esce dal thread di
ricezione delle notifiche UDP.

- `play wordle` : permette all’utente loggato di iniziare la sessione di gioco, non deve aver già giocato oppure la parola non deve
essersi ancora aggiornata per effettuare correttamente questo comando.

- `send` : permette all’utente di mandare una parola che pensa sia quella corretta, una volta mandato il comando, se tutto va a
buon fine in base ai vincoli imposti dal gioco, l’utente può inviare una parola esistente nel vocabolario, se la parola fosse troppo
corta o inesistente, avverte l’utente senza consumare tentativi, in caso contrario la parola sarà semplicemente inesatta e viene
mandato un indizio tramite array di caratteri con formato indicato nella consegna del progetto (+,?,X). Se l’utente indovina, ha
vinto e termina di giocare fino a che non esce la prossima parola allo scattare dei 5 minuti.

- `send me statistics` : manda le statistiche all’utente relative all’utente attualmente loggato, le statistiche che gli manda sono tutte
presenti all’interno della classe Utente, le cinque statistiche principali descritte dal punto 1 del progetto vengono, sia passate al
costruttore per inizializzare un Utente, che salvate dal server e ripristinate al riavvio in modo da poter effettuare diverse partite
con i salvataggi effettuati sulle giocate precedenti. La guess distribution l’ho rappresentata sia come array dei tentativi
effettuati nelle varie partite, che come media degli stessi, per dare un’idea di quanto un utente ci abbia messo a indovinare una
data parola.

- `share` : solo dopo aver effettuato una partita, con esito positivo o negativo, l’utente potrà condividere sul gruppo sociale l’esito
della partita stessa, la notifica arriverà solo agli altri client già con utente loggato e potranno vederla solamente dopo aver
effettuato una `show me sharing` , tale notifica verrà immagazzinata nei vari array di notifiche dei client in ascolto grazie al
threadListener attivato dopo la join.

- `show me sharing` : lato server non succede praticamente nulla mandando questo comando dal client, infatti semplicemente il
server risponde con una stringa di successo o fallimento, il grosso avviene tutto lato client, perché se il client stesso riceve un
responso positivo alla richiesta di mostrare le condivisioni, allora mostrerà l’array di notifiche salvate man mano che gli altri
utenti effettuavano le `share` .

- `exit` : lato server non fa nulla, manda solo una stampa sul terminale del server, invece lato client chiude tutte le socket ed
esce dal while di ricezione comandi.

# Strutture utilizzate
## Lato Server :
- ConcorrentHashMap<String,Utente> : ho utilizzato una
concurrenthashmap per immagazzinare i vari utenti, mi era tornata utile
nell’assignment sulle occorrenze, anche in quello si era utilizzata una
concurrenthashmap in combo con una threadpool, infatti ho pensato di
riutilizzarla associando questa volta username-oggetto Utente come
chiave-valore, in modo che poi potessi riutilizzare questa struttura nelle
operazioni effettuate all’interno del task server. Avere una stringa
rappresentante l’username in associazione con l’utente dello stesso
username mi è parso utile da subito.
- ThreadPoolExecutor : ideale per la gestione di più
richieste e per interagire con più client, in quanto si ha una
gestione automatica dei task dall'ExecutorService. Inoltre
con la LinkedBlockingQueue ho una coda di lunghezza
“infinita”.
- RandomAccessFile : utilizzo un RandomAccessFile per la lettura
della parola segreta, all’interno del thread TimerTask che ‘pesca’ la
parola randomicamente in quando mi torna utile il meccanismo di
puntatore all’interno del file ad accesso randomico, infatti settando
il file pointer con una seek che prende un offset random ma con
parametri corretti (0≤offset≤30824 * 11byte) , riesco a leggere
sempre una parola di 10 lettere randomica nel file words.txt
## Lato Client :
- ArrayList<String> : mi serve un array di stringhe dove
poter immagazzinare le notifiche che ricevo lato client
(tramite una .add) una volta joinato il gruppo multicast.
- Stream : InputStream e OutputStream rispettivamente per
ricevere la risposta dal server task e per mandarla al
medesimo.
