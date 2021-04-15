import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteConverterInterface extends Remote {
    /**
     * Rejestracja uĹźytkownika.
     *
     * @return unikalny identyfikator uĹźytkownika systemu
     * @throws RemoteException
     */
    public int registerUser() throws RemoteException;

    /**
     * Zlecenie dodania wartoĹci value do listy danych dla uĹźytkownika userID.
     *
     * @param userID numer uĹźytkownika, ktĂłry dodaje danÄ
     * @param value  dana do dodania
     * @throws RemoteException
     */
    public void addDataToList(int userID, int value) throws RemoteException;

    /**
     * Metoda pozwala na przekazanie informacji o URL, pod ktĂłrym moĹźna zlokalizowaÄ
     * serwis dokonujÄcy konwersji.
     *
     * @param url adres serwisu dokonujÄcego konwersji
     * @throws RemoteException
     */
    public void setConverterURL(String url) throws RemoteException;

    /**
     * Koniec danych przekazywanych przez uĹźytkownika o podanym userID. Przekazane
     * dane naleĹźy przekazaÄ do konwersji.
     *
     * @param userID identyfikator uĹźytkownika
     * @throws RemoteException
     */
    public void endOfData(int userID) throws RemoteException;

    /**
     * Metoda pozwalajÄca na sprawdzenie czy proces obsĹugi danych dla uĹźytkownika
     * userID zostaĹ zakoĹczony. Przez proces obsĹugi rozumie siÄ przekazanie przez
     * uĹźytkownika danych, zakoĹcznie przekazywania danych poprzez wywoĹanie
     * <code>endOfData</code> i wykonanie konwersji.
     *
     * @param userID identyfikator uĹźytkownika
     * @return true - zakoĹczono przetwarzanie danych dla userID, false - dane w
     *         trakcie przetwarzania.
     * @throws RemoteException
     */
    public boolean resultReady(int userID) throws RemoteException;

    /**
     * Wynik przetwarzania danych dla uĹźytkownika userID. Zwracana lista to dane
     * przekazane przez uĹźytkownika i poddane konwersji.
     *
     * @param userID identyfikator uĹźytkownika
     * @return wynik przetwarzania danych
     * @throws RemoteException
     */
    public List<Integer> getResult(int userID) throws RemoteException;
}