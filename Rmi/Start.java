import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Start extends UnicastRemoteObject implements RemoteConverterInterface {
    private ConverterInterface converterItf;

    public static void main(String[] args) throws RemoteException, AlreadyBoundException {
        try{
            Start s = new Start();
            Naming.bind("REMOTE_CONVERTER", s);
        } catch (RemoteException | MalformedURLException e){
            e.printStackTrace();
        }
    }

    public static String URL;
    private AtomicInteger userCounter;
    private ConcurrentHashMap<Integer, ArrayList<Integer>> inputArray;
    private ConcurrentHashMap<Integer, List<Integer>> convertedInputMap;
    private final Object objectGlob = new Object();

    public Start() throws RemoteException {
        userCounter = new AtomicInteger(1);
        inputArray = new ConcurrentHashMap<Integer, ArrayList<Integer>>();
        convertedInputMap = new ConcurrentHashMap<Integer, List<Integer>>();
    }

    @Override
    public int registerUser() throws RemoteException {
        int userTemp = userCounter.getAndIncrement();
        inputArray.put(userTemp, new ArrayList<Integer>());
        return userTemp;
    }

    @Override
    public void addDataToList(int userID, int value) throws RemoteException {
        ArrayList<Integer> tempList = inputArray.get(userID);
        tempList.add(value);
        inputArray.put(userID, tempList);
    }

    @Override
    public void setConverterURL(String url) throws RemoteException {
        URL = url;
    }

    @Override
    public void endOfData(int userID) throws RemoteException {
        synchronized ( objectGlob )
        {
            try
            {
                converterItf = (ConverterInterface) Naming.lookup(URL);
            }
            catch (NotBoundException | MalformedURLException e)
            {
                e.printStackTrace();
            }
            List<Integer> userData = inputArray.get(userID);
            List<Integer> result = converterItf.convert(userData);
            convertedInputMap.put(userID, result);
        }
    }

    @Override
    public boolean resultReady(int userID) throws RemoteException {
        return convertedInputMap.containsKey(userID);
    }

    @Override
    public List<Integer> getResult(int userID) throws RemoteException {
        if (!convertedInputMap.containsKey(userID))
            return null;
        return convertedInputMap.get(userID);
    }
}

