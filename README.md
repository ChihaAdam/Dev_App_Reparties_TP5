# TP 5 — Invocation des méthodes sur des objets distants avec Java RMI

**Matière :** Développement d'Applications Réparties
**Auditoire :** LSI 3 — Année universitaire 2026 / 2027
**Responsable du cours :** Amal ABID — **Enseignante des TP :** Zeineb DAOUD
**Étudiant :** *— à compléter —*
**Dépôt :** `TP5_DAR2026`

---

## Sommaire

1. [Présentation générale](#1-présentation-générale)
2. [Environnement de travail](#2-environnement-de-travail)
3. [Architecture du dépôt](#3-architecture-du-dépôt)
4. [Activité 5-1 — Service de conversion de devise](#4-activité-5-1--service-de-conversion-de-devise)
5. [Activité 5-2 — Service de banque en RMI et JNDI](#5-activité-5-2--service-de-banque-en-rmi-et-jndi)
6. [Bilan et difficultés rencontrées](#6-bilan-et-difficultés-rencontrées)

---

## 1. Présentation générale

Ce TP met en œuvre une communication **client / serveur** en Java avec les **RMI** (*Remote Method
Invocation*). Il comporte deux activités :

| Activité | Thème | Points techniques |
|---|---|---|
| **5-1** | Conversion de devise (€ → DT) | `Remote`, `UnicastRemoteObject`, `LocateRegistry`, `Naming.rebind`, `Naming.lookup`, export d'un JAR d'interface |
| **5-2** | Gestion de comptes bancaires | objets **sérialisables** (`Compte`), état conservé côté serveur, **JNDI** (`InitialContext`, `rebind`/`lookup`, `jndi.properties`) |

Compétences visées :

- comprendre le **cycle de vie** d'un service distant : *interface → implémentation → enregistrement
  dans l'annuaire → invocation* ;
- distinguer **objet distant** (manipulé par référence via un stub) et **objet métier** (transmis par
  valeur grâce à la sérialisation) ;
- savoir **exporter** uniquement le contrat nécessaire au client ;
- enregistrer et retrouver un service via **JNDI**.

---

## 2. Environnement de travail

| Élément | Version / outil |
|---|---|
| JDK | 17 (build configuré en `release 17`) |
| IDE | IntelliJ IDEA |
| Build | Apache Maven (projets `jar`) |
| Communication | Java RMI |
| Port — activité 5-1 | **1234** (registre RMI créé par le serveur) |
| Port — activité 5-2 | **1099** (registre RMI utilisé par JNDI) |
| Noms des services | `conversion` (5-1), `banque` (5-2) |
| Versionnage | Git / GitHub |

---

## 3. Architecture du dépôt

```
TP5_DAR2026/
├── activite1/
│   ├── RMIConversion_Server/
│   │   └── src/main/java/
│   │       ├── rmiService/IConversion.java
│   │       ├── rmiService/ConversionImpl.java
│   │       └── rmiServer/ConversionServer.java
│   └── RMIConversion_Client/
│       └── src/main/java/
│           └── rmiClient/ConversionClient.java
└── activite2/
    ├── RMI_Banque_Server/
    │   └── src/main/
    │       ├── java/
    │       │   ├── metier/Compte.java
    │       │   ├── rmiService/IBanque.java
    │       │   ├── rmiService/BanqueImpl.java
    │       │   └── rmiServer/BanqueServer.java
    │       └── resources/jndi.properties
    └── RMI_Banque_Client/
        └── src/main/
            ├── java/rmiClient/Client.java
            └── resources/jndi.properties
```

Chaque activité contient un projet **serveur** et un projet **client**. Le client ne dépend que du
**contrat** exposé par le serveur (interface distante + objets métier sérialisables).

---

## 4. Activité 5-1 — Service de conversion de devise

Le serveur expose une méthode distante `convertirMontant(double)` qui convertit un montant en euros
vers le dinar tunisien (taux appliqué : **1 € = 3 DT**). Le client recherche la référence du service
dans l'annuaire RMI, l'invoque, puis affiche le résultat.

### Étape 1 : espace de travail

Le dépôt `TP5_DAR2026` regroupe les deux activités, chacune avec son projet serveur et son projet
client.

### Étape 2 : développement du serveur

#### 2.1. L'interface distante `IConversion`

Une interface distante **étend `java.rmi.Remote`** et **déclare `RemoteException`** sur chaque
méthode, car tout appel distant peut échouer (réseau, serveur indisponible).

```java
package rmiService;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IConversion extends Remote {

    double convertirMontant(double mt) throws RemoteException;
}
```

`IConversion` ne contient aucun code : c'est un **contrat** partagé entre le serveur et le client.

#### 2.2. L'implémentation `ConversionImpl`

```java
package rmiService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ConversionImpl extends UnicastRemoteObject implements IConversion {

    public ConversionImpl() throws RemoteException {
        super();
    }

    @Override
    public double convertirMontant(double mt) throws RemoteException {
        return mt * 3;
    }
}
```

L'héritage de `UnicastRemoteObject` **exporte l'objet distant** : le stub est généré et pourra être
transmis au client. Le constructeur déclare `throws RemoteException` car l'export peut échouer.

#### 2.3. Le serveur `ConversionServer`

```java
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
```

Le `main` réalise les trois opérations demandées :

1. **activation de l'annuaire** : `LocateRegistry.createRegistry(1234)` démarre un registre RMI sur
   le port `1234` (équivalent de la commande `rmiregistry 1234`) ;
2. **instanciation du service** : `new ConversionImpl()` crée et exporte l'objet distant ;
3. **publication** : `Naming.rebind(URL, conversion)` associe le nom `conversion` à l'objet.
   `rebind` est préféré à `bind` car il remplace une entrée existante, ce qui permet de redémarrer
   le serveur sans nettoyage.

### Étape 3 : développement du client

#### 3.1. Export du contrat dans un JAR

Le client n'a besoin que de l'interface `IConversion` (jamais de `ConversionImpl`). Le `pom.xml` du
serveur produit donc, en plus du JAR applicatif, un **JAR d'API** limité à l'interface :

```xml
<execution>
  <id>interface-jar</id>
  <phase>package</phase>
  <goals>
    <goal>jar</goal>
  </goals>
  <configuration>
    <classifier>api</classifier>
    <includes>
      <include>rmiService/IConversion.class</include>
    </includes>
  </configuration>
</execution>
```

`mvn package` génère `target/RMIConversion_Server-1.0-SNAPSHOT-api.jar`, qui joue le rôle du
`conversionClient.jar` demandé. Ce JAR est ajouté au *classpath* du projet client
(**Build Path → Add External JARs**).

#### 3.2. Le client `ConversionClient`

```java

package rmiClient;
import rmiService.IConversion;

import javax.naming.InitialContext;
import java.rmi.Naming;

public class ConversionClient {

    public static void main(String[] args) {
        try{
            // Chercher la référence de l'objet distant dans le RMIRegistry
            IConversion stub = (IConversion) Naming.lookup("rmi://localhost:1234/conversion");
            // Appel de la méthode distante
            double result = stub.convertirMontant(200);
            System.out.println("Résultat de la conversion : "+ result);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
```

- `Naming.lookup(...)` résout le nom `conversion` et retourne un **stub**, transtypé vers
  `IConversion` ;
- `stub.convertirMontant(200)` est un appel **synchrone** : le client attend la réponse du serveur.

### Étape 4 : exécution de l'application

Ordre imposé : **serveur d'abord**, client ensuite.

**Console du serveur :**

```text
service publiée sur: rmi://localhost:1234/conversion
```

**Console du client :**

```text
Résultat de la conversion : 600.0
```

`600.0 = 200 € × 3` : le calcul a été exécuté **dans la JVM du serveur**, le client n'ayant reçu que
la valeur de retour.

### Synthèse de l'activité 5-1

- L'interface distante (`Remote` + `RemoteException`) est le **contrat** commun aux deux programmes.
- `UnicastRemoteObject` exporte l'objet et permet la génération du stub.
- `LocateRegistry.createRegistry` embarque l'annuaire dans le processus serveur.
- `Naming.rebind` publie, `Naming.lookup` consomme : tout le couplage tient dans une **URL logique**.
- L'export sélectif du contrat garantit que le client **ignore l'implémentation**.
- L'appel distant garde la **syntaxe d'un appel local** ; seules les exceptions et la latence
  rappellent le caractère distribué.

---

## 5. Activité 5-2 — Service de banque en RMI et JNDI

Le serveur centralise les comptes bancaires et expose deux services :

- `creerCompte(Compte c)` : crée un compte si son code n'existe pas déjà ;
- `getInfoCompte(int code)` : retourne le compte correspondant au code (ou `null`).

L'enregistrement et la recherche du service se font via **JNDI**, avec un nom logique (`banque`) et
non une URL RMI codée en dur.

### 5.1. Modèle métier : la classe `Compte`

`Compte` doit être **`Serializable`** : les objets métier échangés avec le serveur sont **copiés par
valeur**, contrairement au service lui-même qui est manipulé par référence.

```java
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
```

`toString()` produit l'affichage lisible du compte côté client après désérialisation.

### 5.2. Interface distante `IBanque`

```java
package rmiService;

import metier.Compte;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IBanque extends Remote {
    String creerCompte(Compte c) throws RemoteException;

    Compte getInfoCompte(int code) throws RemoteException;
}
```

`getInfoCompte` retourne un `Compte` **par valeur** ; sans `Serializable`, l'appel lèverait une
`MarshalException`.

### 5.3. Implémentation `BanqueImpl`

```java
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
```

- l'état (`comptes`) est conservé **dans la JVM du serveur** : le service est *stateful*, tous les
  clients voient les mêmes comptes ;
- `creerCompte` applique la règle d'**unicité du code** et retourne un message explicite ;
- `getInfoCompte` retourne `null` si le code est inconnu.

### 5.4. Configuration JNDI : `jndi.properties`

Le fichier est placé dans `src/main/resources` des deux projets (serveur **et** client) afin d'être
présent dans le *classpath* à l'exécution :

```properties
java.naming.factory.initial=com.sun.jndi.rmi.registry.RegistryContextFactory
java.naming.provider.url=rmi://localhost:1099
```

- `java.naming.factory.initial` : fabrique de contexte JNDI (ici, le fournisseur RMI) ;
- `java.naming.provider.url` : URL du registre contacté.

`InitialContext` lit ce fichier automatiquement : aucun paramètre de connexion n'apparaît dans le
code Java.


### 5.5. Serveur `BanqueServer`

```java
package rmiServer;

import rmiService.BanqueImpl;
import rmiService.IBanque;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.rmi.registry.LocateRegistry;

public class BanqueServer {
    public static void main(String[] args){
        try{
            // Annuaire RMI sur le port déclaré dans jndi.properties
            LocateRegistry.createRegistry(1099);
            IBanque banque = new BanqueImpl();
            // Publication de l'objet distant dans l'annuaire JNDI
            Context ctx = new InitialContext();
            ctx.rebind("banque", banque);
            System.out.println("service banque publié dans l'annuaire JNDI (rmi://localhost:1099)");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
```

Le registre est créé sur le port **1099** pour correspondre au `provider.url`. L'objet est ensuite
publié sous le nom logique `banque` avec `ctx.rebind(...)` — plus aucune URL `rmi://` dans le code
d'enregistrement. La variable est typée `IBanque` (le contrat), pas `BanqueImpl`.

### 5.6. Client `Client`

```java
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
```

Scénario exécuté :

1. `ctx.lookup("banque")` retourne le stub `IBanque` ;
2. construction d'un compte local `(code=1, solde=1000.0, 13/07/2005)` ;
3. `retirer(200)` puis `verser(300)` → solde **1100.0** (opérations locales, avant envoi) ;
4. `creerCompte(compte)` envoie le compte **par valeur** au serveur qui le stocke ;
5. `getInfoCompte(1)` renvoie une **copie** du compte stocké ;
6. affichage via `toString()`.

> **Capture d'écran —** package `rmiClient` et classe `Client` (insérer `captures/06-client-banque.png`).

### 5.7. Exécution de l'application

**Console du serveur :**

```text
service banque publié dans l'annuaire JNDI (rmi://localhost:1099)
```

**Console du client :**

```text
Compte{code=1, solde=1100.0, date_creation=2005-07-13}
```

**Test du doublon :** une seconde création avec le code `1` retourne :

```text
il existe déja un compte avec ce code
```

L'état est bien conservé côté serveur : c'est la liste serveur qui fait foi, pas l'objet local du
client.

### Synthèse de l'activité 5-2

- `Compte` est **sérialisable** : il traverse le réseau par **copie** ; le service, lui, est
  manipulé **par référence** via son stub.
- L'état métier vit dans le **serveur** : modifier un `Compte` reçu côté client ne change pas la
  version stockée.
- L'unicité du code est contrôlée **côté serveur**, seul point centralisé de l'architecture.
- `creerCompte` illustre le transfert **client → serveur**, `getInfoCompte` le transfert
  **serveur → client**.
- **JNDI** remplace l'URL physique par un **nom logique** (`banque`) : l'hôte et le port sont
  externalisés dans `jndi.properties`, donc modifiables sans recompilation.
- Le registre doit être démarré **avant** le client, et le port doit être identique dans le serveur,
  les deux `jndi.properties` et le client.

---
**Conclusion**

RMI permet d'invoquer une méthode Java distante avec une syntaxe quasi identique à un appel local,
au prix d'un contrat strict (`Remote`, `RemoteException`, `Serializable`) et d'un annuaire de
publication. L'activité 5-2 montre le serveur comme **autorité sur l'état métier**, et l'usage de
**JNDI** illustre la séparation entre **nom logique** et **localisation physique** du service.

