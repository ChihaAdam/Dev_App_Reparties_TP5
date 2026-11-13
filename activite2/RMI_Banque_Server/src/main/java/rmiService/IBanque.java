package rmiService;

import metier.Compte;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IBanque extends Remote {
    String creerCompte(Compte c) throws RemoteException;

    Compte getInfoCompte(int code) throws RemoteException;
}
