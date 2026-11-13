package metier;

import java.io.Serializable;
import java.util.Date;

public class Compte implements Serializable {
    private int code;
    private double solde;
    private Date date_creation;

    public Compte(int code, double solde, Date date_creation) {
        this.code = code;
        this.solde = solde;
        this.date_creation = date_creation;
    }

    public int getCode() {
        return code;
    }

    public double getSolde() {
        return solde;
    }


    public void verser(double montant) {
        solde += montant;
    }

    public void retirer(double montant) {
        solde -= montant;
    }

    public Date getDate_creation() {
        return date_creation;
    }

    @Override
    public String toString() {
        return "Compte{" +
                "code=" + code +
                ", solde=" + solde +
                ", date_creation=" + date_creation +
                '}';
    }
}
