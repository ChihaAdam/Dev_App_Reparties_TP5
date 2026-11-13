package rmiService;

import metier.Compte;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class BanqueImpl extends UnicastRemoteObject implements IBanque{
    List<Compte> comptes = new ArrayList<>();

    public BanqueImpl() throws RemoteException {
    }

    @Override
    public String creerCompte(Compte c) throws RemoteException{
        for (Compte compte : comptes) {
            if (compte.getCode() == c.getCode()) {
                return "il existe déja un compte avec ce code";
            }
        }
        comptes.add(c);
        return "le compte est crée avec succes";
    }

    @Override
    public Compte getInfoCompte(int code) throws RemoteException{
        for (Compte compte : comptes) {
            if (compte.getCode() == code) {
                return compte;
            }
        }
        return null;
    }
}
