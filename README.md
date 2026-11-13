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
   - [Étape 1 : espace de travail](#étape-1--espace-de-travail)
   - [Étape 2 : développement du serveur](#étape-2--développement-du-serveur)
   - [Étape 3 : développement du client](#étape-3--développement-du-client)
   - [Étape 4 : exécution de l'application](#étape-4--exécution-de-lapplication)
   - [Synthèse de l'activité 5-1](#synthèse-de-lactivité-5-1)
5. [Activité 5-2 — Service de banque en RMI et JNDI](#5-activité-5-2--service-de-banque-en-rmi-et-jndi)
   - [Modèle métier : la classe `Compte`](#modèle-métier--la-classe-compte)
   - [Interface distante `IBanque`](#interface-distante-ibanque)
   - [Implémentation `BanqueImpl`](#implémentation-banqueimpl)
   - [Serveur `BanqueServer`](#serveur-banqueserver)
   - [Client `Client`](#client-client)
   - [Exécution de l'application](#exécution-de-lapplication-1)
   - [Étape 5 : enregistrement et recherche via JNDI](#étape-5--enregistrement-et-recherche-via-jndi)
   - [Synthèse de l'activité 5-2](#synthèse-de-lactivité-5-2)
6. [RMI « direct » (`Naming`) vs JNDI](#6-rmi-direct-naming-vs-jndi)
7. [Bilan et difficultés rencontrées](#7-bilan-et-difficultés-rencontrées)

---

## 1. Présentation générale

Ce TP a pour objectif la mise en œuvre d'une communication **client / serveur** en Java au moyen
des **RMI** (*Remote Method Invocation*). Il est découpé en deux activités complémentaires :

| Activité | Thème | Points techniques abordés |
|---|---|---|
| **5-1** | Service de conversion de devise (€ → DT) | `Remote`, `UnicastRemoteObject`, `LocateRegistry`, `Naming.rebind`, `Naming.lookup`, export d'un JAR d'interface |
| **5-2** | Gestion de comptes bancaires | objets **sérialisables** transportés par RMI (`Compte`), collection de comptes côté serveur, **JNDI** (`InitialContext`, `bind`/`lookup`, `jndi.properties`) |

Les compétences visées sont les suivantes :

- comprendre le **cycle de vie** d'un service distant : *définition de l'interface → implémentation →
  enregistrement dans l'annuaire → invocation par le client* ;
- maîtriser la notion de **stub / skeleton** et la séparation interface / implémentation ;
- savoir **exporter** les seules classes nécessaires (interface + objets métier) vers le client ;
- savoir enregistrer et retrouver un objet distant, d'abord avec le registre RMI, puis via **JNDI**.

> **Principe de base.** En RMI, le client ne manipule jamais directement l'objet distant : il
> manipule un **stub** (proxy) qui sérialise les arguments, envoie la requête au serveur, attend le
> résultat et le désérialise. Le serveur, lui, possède le **skeleton** qui décode l'appel et invoque
> la méthode réelle sur l'objet distant.

---

## 2. Environnement de travail

| Élément | Version / outil |
|---|---|
| JDK | 17 (`maven.compiler.release = 17`) |
| IDE | IntelliJ IDEA |
| Build | Apache Maven (projets `jar`) |
| Communication | Java RMI (JDK), annuaire `rmiregistry` |
| Port utilisé | **1234** (registre créé par le serveur) |
| Nommage des services | `conversion` (activité 5-1), `banque` (activité 5-2) |
| Versionnage | Git / GitHub |

---

## 3. Architecture du dépôt

```
TP5_DAR2026/
├── activite1/
│   ├── RMIConversion_Server/          # serveur RMI : interface + implémentation + publication
│   │   └── src/main/java/
│   │       ├── rmiService/IConversion.java
│   │       ├── rmiService/ConversionImpl.java
│   │       └── rmiServer/ConversionServer.java
│   └── RMIConversion_Client/          # client RMI : recherche du stub + invocation
│       └── src/main/java/
│           └── rmiClient/ConversionClient.java
└── activite2/
    ├── RMI_Banque_Server/             # serveur RMI : métier + service + publication
    │   └── src/main/java/
    │       ├── metier/Compte.java
    │       ├── rmiService/IBanque.java
    │       ├── rmiService/BanqueImpl.java
    │       └── rmiServer/BanqueServer.java
    └── RMI_Banque_Client/             # client RMI : création et consultation de comptes
        └── src/main/java/
            └── rmiClient/Client.java
```

Chaque module est un projet Maven indépendant ; le module client dépend uniquement des
**contrats** exposés par le serveur (interface distante et objets métier sérialisables), jamais des
classes d'implémentation.

> **Capture d'écran —** arborescence des projets dans l'IDE (insérer `captures/00-arborescence.png`).

---

## 4. Activité 5-1 — Service de conversion de devise

Le serveur offre une méthode distante `convertirMontant(double)` qui convertit un montant en euros
vers le dinar tunisien (taux appliqué : **1 € = 3 DT**). Le client recherche la référence du service
dans l'annuaire RMI, l'invoque, puis affiche le résultat.

### Étape 1 : espace de travail

Le dépôt `TP5_DAR2026` a été créé et contient un sous-répertoire par activité (`activite1`,
`activite2`), lui-même composé d'un projet **serveur** et d'un projet **client**.

> **Capture d'écran —** répertoire de travail (insérer `captures/01-espace-travail.png`).

### Étape 2 : développement du serveur

#### 2.1. L'interface distante `IConversion`

Une interface distante doit **étendre `java.rmi.Remote`** et **déclarer `RemoteException`** dans
chaque méthode, car tout appel distant peut échouer pour des raisons réseau.

```java
package rmiService;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IConversion extends Remote {

    double convertirMontant(double mt) throws RemoteException;
}
```

Points importants :

- `IConversion` ne contient **aucun code** : c'est uniquement un **contrat** partagé entre le
  serveur et le client ;
- tous les types manipulés (`double`, ici un type primitif) doivent être sérialisables ou primitifs.

#### 2.2. L'implémentation `ConversionImpl`

La classe d'implémentation étend `UnicastRemoteObject` : le constructeur appelle implicitement
`super()`, ce qui **exporte l'objet** et génère le stub permettant les appels distants.

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

Le constructeur est déclaré `throws RemoteException` car l'export de l'objet distant peut échouer.
La logique métier reste volontairement triviale : elle illustre le **transport** de l'appel, pas le
calcul.

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

Le `main` réalise exactement les trois opérations demandées par l'énoncé :

1. **activation de l'annuaire** : `LocateRegistry.createRegistry(1234)` démarre un registre RMI
   sur le port `1234` (équivalent programmatique de la commande `rmiregistry 1234`) ;
2. **instanciation du service** : `new ConversionImpl()` crée et exporte l'objet distant ;
3. **publication de la référence** : `Naming.rebind(URL, conversion)` associe le nom
   `conversion` à l'objet dans l'annuaire. `rebind` est préféré à `bind` car il **remplace** une
   éventuelle entrée existante, ce qui rend le serveur redémarrable sans nettoyage préalable.

### Étape 3 : développement du client

#### 3.1. Export du contrat sous forme de JAR

Le client n'a besoin que de l'interface `IConversion` (jamais de `ConversionImpl`). Le projet
serveur est configuré pour produire, en plus du JAR applicatif, un **JAR d'API** ne contenant que
l'interface :

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

Après `mvn package`, on obtient `target/RMIConversion_Server-1.0-SNAPSHOT-api.jar`, qui joue le rôle
du fichier `conversionClient.jar` demandé. Ce JAR est ensuite ajouté au *classpath* du projet
`RMIConversion_Client` (**Build Path → Add External JARs**).

> **Capture d'écran —** génération et ajout du JAR d'interface au classpath
> (insérer `captures/02-jar-interface.png`).

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

Le client effectue deux opérations :

- `Naming.lookup(...)` : résolution du nom `conversion` dans le registre ; le résultat est un
  **stub** que l'on transtype vers l'interface `IConversion` ;
- `stub.convertirMontant(200)` : appel **synchrone** — le thread du client est bloqué jusqu'à la
  réponse du serveur (comportement RMI par défaut).

L'appel est encapsulé dans un `try/catch` couvrant `NotBoundException`, `MalformedURLException` et
`RemoteException`, toutes héritées de `Exception`.

### Étape 4 : exécution de l'application

L'ordre de démarrage est important : **le serveur d'abord**, le client ensuite.

**Console du serveur :**

```text
service publiée sur: rmi://localhost:1234/conversion
```

**Console du client :**

```text
Résultat de la conversion : 600.0
```

Le résultat `600.0` correspond bien à `200 € × 3 = 600 DT` : l'appel a été exécuté **dans la JVM du
serveur**, le client n'ayant reçu que la valeur de retour sérialisée.

> **Capture d'écran —** exécution du serveur puis du client (insérer
> `captures/03-execution-conversion.png`).

### Synthèse de l'activité 5-1

- Une interface distante hérite de `Remote` et déclare `RemoteException` ; elle constitue le
  **contrat** partagé par les deux parties.
- `UnicastRemoteObject` exporte automatiquement l'objet : le stub est généré et communiqué au client
  lors du `lookup`.
- `LocateRegistry.createRegistry(port)` permet d'embarquer l'annuaire dans le processus serveur, ce
  qui évite de lancer `rmiregistry` manuellement.
- `Naming.rebind` publie le service, `Naming.lookup` le consomme : le couplage repose uniquement sur
  une **URL logique** (`rmi://hôte:port/nom`).
- L'export sélectif du contrat dans un JAR garantit que le client ne dépend pas de l'implémentation
  (principe de **séparation interface / implémentation**).
- L'appel distant est **transparent** du point de vue du code client : la syntaxe est identique à un
  appel local, seules les exceptions et la latence trahissent le caractère distribué.

---

## 5. Activité 5-2 — Service de banque en RMI et JNDI

Le serveur centralise la gestion de comptes bancaires et expose deux services :

- `creerCompte(Compte c)` : crée un compte si son code n'existe pas déjà ;
- `getInfoCompte(int code)` : retourne le compte correspondant au code (ou `null`).

### Modèle métier : la classe `Compte`

`Compte` doit être **`Serializable`** : les objets métier transmis en argument ou en retour sont
**copiés par valeur** (sérialisation), contrairement aux objets distants qui sont passés par
référence.

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

La présence de `toString()` est importante : c'est lui qui produit l'affichage lisible du compte
côté client après désérialisation.

> **Capture d'écran —** package `metier` et classe `Compte` (insérer `captures/04-compte.png`).

### Interface distante `IBanque`

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

`getInfoCompte` retourne un `Compte` : c'est un objet **sérialisable** qui traverse le réseau **par
valeur**. Si `Compte` n'implémentait pas `Serializable`, l'appel lèverait une
`MarshalException`.

### Implémentation `BanqueImpl`

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

Observations :

- l'état (`comptes`) est conservé **côté serveur**, dans la JVM qui héberge l'objet distant. C'est ce
  qui rend le service *stateful* : deux clients successifs voient les mêmes comptes ;
- `creerCompte` applique la règle métier « **unicité du code** » et retourne un message explicite
  dans les deux cas (succès / doublon) ;
- `getInfoCompte` effectue une recherche linéaire et retourne `null` si le code est inconnu — la
  gestion de ce cas est laissée au client. Une `HashMap<Integer, Compte>` serait plus efficace,
  mais la liste met bien en évidence le stockage serveur.

### Serveur `BanqueServer`

```java
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
```

Le service est publié sous le nom **`banque`**, sur le même registre `1234`. On remarque que la
variable est typée `IBanque` (l'interface) et non `BanqueImpl` : c'est le contrat qui circule, pas
l'implémentation.

### Client `Client`

```java
package rmiClient;
import metier.Compte;
import rmiService.IBanque;

public class Client {
    public static void main(String[] args) {
        try {

            IBanque banque = (IBanque) java.rmi.Naming.lookup("rmi://localhost:1234/banque");
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

Scénario exécuté par le client :

1. `lookup` du service `banque` → obtention du stub `IBanque` ;
2. construction d'un compte local `(code=1, solde=1000.0, 13/07/2005)` ;
3. opérations locales `retirer(200)` puis `verser(300)` → solde **1100.0** ;
4. `creerCompte(compte)` → le compte est **envoyé par valeur** au serveur qui le stocke ;
5. `getInfoCompte(1)` → le serveur renvoie une **copie** du compte stocké ;
6. affichage via `toString()`.

> **Capture d'écran —** package `rmiClient` et classe `Client` (insérer `captures/05-client-banque.png`).

### Exécution de l'application

**Console du serveur :**

```text
service publiée sur: rmi://localhost:1234/banque
```

**Console du client :**

```text
Compte{code=1, solde=1100.0, date_creation=2005-07-13}
```

**Second appel du client (test du doublon) :** la création d'un compte de code `1` déjà existant
retourne :

```text
il existe déja un compte avec ce code
```

Ce test confirme que l'état est bien conservé **dans le serveur** : le client a beau créer un
nouvel objet `Compte`, c'est la liste serveur qui fait foi.

> **Capture d'écran —** exécution du serveur banque et du client (insérer
> `captures/06-execution-banque.png`).

### Étape 5 : enregistrement et recherche via JNDI

JNDI (*Java Naming and Directory Interface*) ajoute une **couche d'abstraction** au-dessus du
registre RMI : le code client/serveur ne connaît plus l'URL `rmi://…` directement, mais un **nom
logique** résolu par un *contexte de nommage*. Le fournisseur JNDI pour RMI est
`com.sun.jndi.rmi.registry.RegistryContextFactory`.

#### 5.1. Configuration : `jndi.properties`

Le fichier est placé dans `src/main/resources` (équivalent du répertoire `src` demandé), de sorte
qu'il soit présent dans le *classpath* à l'exécution :

```properties
java.naming.factory.initial=com.sun.jndi.rmi.registry.RegistryContextFactory
java.naming.provider.url=rmi://localhost:1099
```

- `java.naming.factory.initial` : fabrique de contexte JNDI à utiliser (ici, le registre RMI) ;
- `java.naming.provider.url` : URL du registre contacté.

L'énoncé impose le port **1099** (port par défaut de `rmiregistry`). Il faut donc soit lancer
`rmiregistry 1099`, soit créer le registre sur ce port au démarrage du serveur
(`LocateRegistry.createRegistry(1099)`).

#### 5.2. Enregistrement de l'objet distant (côté serveur)

```java
Context ctx = new InitialContext();
ctx.rebind("banque", banque);   // binding JNDI : plus d'URL rmi://...
```

`InitialContext` lit automatiquement `jndi.properties` et délègue l'opération au registre RMI. Le
nom `banque` est alors résolu relativement à `rmi://localhost:1099`.

#### 5.3. Recherche de l'objet distant (côté client)

```java
Context ctx = new InitialContext();
IBanque banque = (IBanque) ctx.lookup("banque");
```

Le client n'a plus besoin de connaître le protocole ni l'hôte : il demande le nom `banque` au
contexte, qui retourne le stub. Le reste du code (invocation des méthodes distantes) est **inchangé**.

#### 5.4. Vérification

Après relance du serveur puis du client, la sortie console reste identique à celle de l'étape 4 :

```text
Compte{code=1, solde=1100.0, date_creation=2005-07-13}
```

La communication RMI fonctionne donc de la même manière ; seul le **mode de résolution du nom** a
changé (JNDI au lieu de `Naming.lookup`).

> **Capture d'écran —** `jndi.properties` et exécution avec JNDI (insérer
> `captures/07-jndi.png`).

> **Remarque.** La version présente dans ce dépôt publie et recherche encore les services avec
> l'API `Naming` (registre RMI direct, port `1234`). La configuration et les extraits ci-dessus
> décrivent la variante JNDI demandée à l'étape 5 ; le passage de l'une à l'autre se limite aux
> deux lignes de `bind`/`lookup` et au fichier `jndi.properties`.

### Synthèse de l'activité 5-2

- Les **objets métier** échangés (`Compte`) doivent implémenter `Serializable` : RMI les transporte
  **par copie**, alors que le service (`BanqueImpl`) est manipulé **par référence** via son stub.
- L'état métier (la liste des comptes) vit dans le **serveur** ; le client ne détient que des copies.
  Toute modification locale d'un `Compte` reçu n'affecte donc pas la version serveur.
- La règle d'unicité du code est implémentée côté serveur : c'est le point naturel de **contrôle
  centralisé** dans une architecture répartie.
- `creerCompte` et `getInfoCompte` illustrent les deux sens du transfert : paramètre **client →
  serveur** et valeur de retour **serveur → client**.
- **JNDI** découple le code de l'URL du registre : le nom logique (`banque`) remplace l'URL physique,
  ce qui facilite le changement d'annuaire ou d'hôte sans recompilation.
- Le registre doit être démarré **avant** le client, et le port doit être cohérent entre le serveur,
  `jndi.properties` et le client — source d'erreur classique (`ConnectException`).

---

## 6. RMI « direct » (`Naming`) vs JNDI

| Critère | `Naming` (registre RMI direct) | JNDI (`InitialContext`) |
|---|---|---|
| Résolution | `Naming.lookup("rmi://host:port/nom")` | `ctx.lookup("nom")` |
| Publication | `Naming.rebind(url, obj)` | `ctx.rebind("nom", obj)` |
| Dépendance au protocole | URL RMI explicite dans le code | Aucune : configuration externalisée |
| Configuration | Aucune (ou en dur) | `jndi.properties` |
| Changement d'annuaire | Modification + recompilation | Modification du fichier de configuration |
| Usage typique | Prototypage, TP d'introduction | Applications d'entreprise, annuaires LDAP/RMI |

Le mécanisme sous-jacent reste **identique** : JNDI pour RMI n'est qu'une façade au-dessus du
registre RMI.

---

## 7. Bilan et difficultés rencontrées

**Ce qui a été acquis**

- mise en place complète d'une chaîne RMI : interface → implémentation → publication → invocation ;
- compréhension de la différence **objet distant (par référence)** / **objet métier (par valeur)** ;
- export sélectif du contrat dans un JAR pour éviter toute dépendance du client à l'implémentation ;
- découverte de JNDI comme couche d'abstraction du nommage.

**Difficultés rencontrées et solutions**

| Difficulté | Cause | Solution |
|---|---|---|
| `ConnectException: Connection refused` | registre non démarré ou mauvais port | démarrer le serveur avant le client, vérifier la cohérence des ports (`1234`, `1099`) |
| `NotBoundException` | nom absent de l'annuaire (faute de frappe, `bind` non exécuté) | vérifier le nom (`conversion`, `banque`) et l'ordre de démarrage |
| `ClassNotFoundException: rmiService.IConversion` | JAR d'interface absent du *classpath* client | ajouter le JAR d'API au Build Path |
| `MarshalException` / `NotSerializableException` | objet métier non `Serializable` | faire implémenter `Serializable` à `Compte` |
| `AlreadyBoundException` au redémarrage | utilisation de `bind` | utiliser `rebind` |

**Conclusion**

Ce TP montre que RMI permet d'invoquer une méthode Java distante avec une syntaxe quasi identique à
celle d'un appel local, au prix d'un contrat d'interface strict (`Remote`, `RemoteException`,
`Serializable`) et d'un annuaire de publication. L'activité 5-2 met en évidence le rôle central du
serveur comme **autorité sur l'état métier**, tandis que l'étape JNDI illustre la séparation entre
**nom logique** et **localisation physique** du service — un principe fondamental des architectures
réparties.

---

## Références

- Cours « Développement d'applications réparties : RMI », A. ABID.
- Énoncés `TP5_Activité5-1_DevAppRep_LSI3.pdf` et `TP5_Activité5-2_DevAppRep_LSI3.pdf`.
- Documentation Oracle : *Java Remote Method Invocation* et *Java Naming and Directory Interface*.
