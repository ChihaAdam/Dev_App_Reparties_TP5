package rmiServer;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import rmiService.ConversionImpl;
public class ConversionServer {
    private static String URL = "rmi://localhost:1234/conversion";
    public static void main(String[] args){
        try{
            LocateRegistry.createRegistry(1234);
            ConversionImpl conversion = new ConversionImpl();
            Naming.rebind(URL,conversion);
            System.out.println("service publiée sur: "+URL);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
