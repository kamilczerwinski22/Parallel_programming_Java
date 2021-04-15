import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Client {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws IOException, NotBoundException {


        System.out.println("enter server address: ");
        String server = "localhost";

        Remote r = java.rmi.Naming.lookup("//" + server + ":1099/REMOTE_CONVERTER");
        RemoteConverterInterface rci = (RemoteConverterInterface) r;
        System.out.println("oto referencja do proxy: " + rci);

        int id = rci.registerUser();

        Thread th = new Thread(() -> {
            System.out.println("Enter ur data here: ");
            while (true) {
                int data = sc.nextInt();
                try {
                    rci.setConverterURL(server);
                    if (data == 0) {
                        rci.endOfData(id);
                        break;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                try {
                    rci.addDataToList(id, data);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });

        th.start();
    }

}





