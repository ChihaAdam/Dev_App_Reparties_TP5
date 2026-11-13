package rmiServer;

import rmiService.BanqueImpl;
import rmiService.IBanque;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class BanqueServer {
    private static String URL = "rmi://localhost:1234/banque";
    public static void main(String[] args){
        try{
            LocateRegistry.createRegistry(1234);
            IBanque banque = new BanqueImpl();
            Naming.rebind(URL,banque);
            System.out.println("service publiée sur: "+URL);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
