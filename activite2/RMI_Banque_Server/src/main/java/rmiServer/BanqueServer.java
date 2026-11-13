package rmiServer;

import rmiService.BanqueImpl;
import rmiService.IBanque;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.rmi.registry.LocateRegistry;

public class BanqueServer {
    public static void main(String[] args){
        try{
            LocateRegistry.createRegistry(1099);
            IBanque banque = new BanqueImpl();
            Context ctx = new InitialContext();
            ctx.rebind("banque", banque);
            System.out.println("service banque publié dans l'annuaire JNDI (rmi://localhost:1099)");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
