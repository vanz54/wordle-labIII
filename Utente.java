// Librerie java
import java.util.ArrayList;
import java.util.List;

/**
 * Reti e Laboratorio III - A.A. 2022/2023
 * Wordle
 * 
 * Utente è la classe che si occupa della gestione dell'utente che sta giocando.
 * Contiene tutte le strutture dati e informazioni utili per l'applicazione dei comandi mandati dal
 * client, ogni giocatore avrà un suo utente con i relativi dati e informazioni.
 * 
 * @author Tommaso Vanz
 */

public class Utente {
public String username; // Username utente
public String password; // Password utente
public boolean haPartecipato = false; // Per vedere se ho già partecipato per quella parola, se ho già partecipato aspetto esca la nuova parola, altrimenti se è false inizia a giocare
public boolean puoGiocare = false; // Dopo che l'utente partecipa (play) , allora potrà giocare (send), non prima
public boolean logged = false; // Per vedere se ho un utente ancora connesso
public boolean partitaTerminata = false; // Per vedere se l'utente ha terminato la partita
public boolean haVinto = false; // Per vedere se l'utente ha vinto o perso
public boolean haPerso = false;
public int tentativi; // Numero tentativi correnti
public int partiteGiocate; // Numero partite giocate
public int percentualeVittorie; // (Vittorie/Partite giocate)%
public int lengthLastWinstreak; // Ultima winstreak
public int lengthMaxWinstreak; // Massima Winstreak
public int guessdistribution; // Guess Distribution che mi calcolo grazie all'array di tentativi
public int vittorie; // Numero vittorie
public int myWinStreak; // Winstreak corrente
List<Integer> arrayTentativi = new ArrayList<Integer>(); // Array che contiene tutti i tentativi fatti dall'user
    // Costruttore che mi serve per la registrazione e ripristino dei parametri
    Utente(String username, String password, int partiteGiocate, int percentualeVittorie, int lengthLastWinstreak, int lengthMaxWinstreak, int guessdistribution){
        this.username = username;
        this.password = password;
        this.tentativi = 0;
        this.partiteGiocate = 0;
        this.percentualeVittorie = 0;
        this.lengthLastWinstreak = 0;
        this.lengthMaxWinstreak = 0;
        this.guessdistribution = 0;
        this.vittorie = 0;
        this.myWinStreak = 0;
        this.arrayTentativi = new ArrayList<Integer>();
    }    

    // Metodi getter
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public int getTentativiFatti() {
        return tentativi;
    }
    public int getPartiteGiocate() {
        return partiteGiocate;
    }
    public int getpercentualeVittorie() {
        return percentualeVittorie;
    }
    public int getlengthLastWinstreak() {
        return lengthLastWinstreak;
    }
    public int getMaxWinstreak() {
        return lengthMaxWinstreak;
    }
    public int getGuessDistribution() {
        return guessdistribution;
    }
    public int getWinstreak() {
        return myWinStreak;
    }
    public boolean getHaGiaPartecipato() {
        return haPartecipato;
    }
    public List<Integer> getArrayTentativi() {
        return arrayTentativi;
    }

    // Metodo per vedere se un player ha partecipato, se non ancora potrà partecipare
    public void partecipa() {
        if(!haPartecipato) {
            partiteGiocate++;
            haPartecipato = true;
            puoGiocare = true;
            haVinto = false;
            haPerso = false;
        }
    }

    // Una volta terminata una partita, metto i tentativi fatti nell'array
    public void addTentativoToArray(Integer tentativo) {
        this.arrayTentativi.add(tentativo);
    }

    // Calcolo la winstreak per aggiornarla eventualmente sull'utente (myWinstreak=Winstreak corrente)
    public void calcolaWinstreak(){
        this.lengthLastWinstreak = this.myWinStreak;
        // Se la winstreak è anche maggiore della sua winstreak totale, aggiorno anche la winstreak massima
            if(this.getMaxWinstreak()<this.getWinstreak()){
                this.lengthMaxWinstreak = this.myWinStreak;    
            }
    }

    // Calcolo la guessdistribution facendo la media dei tentativi dell'array tentativi
    public Integer calcolaDistribution(){
        Integer sum = 0;
        for(int i = 0; i < arrayTentativi.size(); i++)
            sum += arrayTentativi.get(i);
        return sum/arrayTentativi.size();
    }

    // Da oggetto Utente a Stringa
    public String toString() {
        return " {" + username + "," + password + "," + partiteGiocate + "," + percentualeVittorie + "," + lengthLastWinstreak + "," + lengthMaxWinstreak + "," + guessdistribution + "} " ;
    }

}
