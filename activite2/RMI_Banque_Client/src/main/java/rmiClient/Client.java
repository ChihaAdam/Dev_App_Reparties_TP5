package rmiClient;
import metier.Compte;
import rmiService.IBanque;

import javax.naming.Context;
import javax.naming.InitialContext;

public class Client {
    public static void main(String[] args) {
        try {
            // Recherche de l'objet distant via l'annuaire JNDI
            Context ctx = new InitialContext();
            IBanque banque = (IBanque) ctx.lookup("banque");
            Compte compte = new Compte(1, 1000.0, java.sql.Date.valueOf("2005-07-13"));
            compte.retirer(200);
            compte.verser(300);
            banque.creerCompte(compte);
            Compte cpte = banque.getInfoCompte(1);
            System.out.println(cpte.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
