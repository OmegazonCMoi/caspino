# **Caspino — Casino Virtuel Connecté**

## **1. Objectif du projet**

Créer un **casino en ligne virtuel et connecté**, combinant des jeux classiques (BlackJack, Poker, Roulette, Machine à sous…) avec des fonctionnalités ludiques et décalées, accessibles via une application mobile et synchronisées avec un serveur central.

L’objectif est de proposer une expérience interactive, humoristique et technologique respectant les contraintes du projet (serveur, Raspberry Pi, mobile, IA).

---

## **2. Fonctionnalités principales**

### Jeux disponibles

- **BlackJack** : jeu de cartes contre la banque géré par le serveur.
- **Machine à sous** : tirage aléatoire avec effets visuels sur le Raspberry Pi (LED, sons, sirène).
- **Poker** : partie multijoueur connectée via le serveur.
- **Roulette** : simulation de roulette avec affichage des résultats sur mobile et LEDs sur le Pi.

### Gestion de compte

- **Ajout d’argent virtuel** : chaque joueur dispose d’une monnaie interne (*PiCoins*).
- **Paris sportifs** : possibilité de parier sur des événements générés aléatoirement (résultats absurdes et drôles).

### Fonctionnalités bonus

- **Achat d’accès à ChatGPT (acoustic)** : permet aux joueurs d’interagir avec une IA sarcastique (“le croupier IA”) hébergée sur le serveur Ollama.
- **Achat d’accès à actionneurs** :
    - *Sirène* : déclenchée en cas de gros gain.
    - *Lampe connectée* : s’allume selon la chance du joueur (couleur selon résultat).
- **Statistiques casino** : suivi des gains, pertes, taux de réussite, classement des joueurs.

---

## **3. Architecture technique**

### **Serveur**

- Centralise les comptes, les parties, et la base de données des joueurs.
- Fournit une API REST pour l’application mobile.
- Héberge l’IA Ollama (croupier intelligent).
- Gère la logique des jeux et des transactions virtuelles.

### **Raspberry Pi**

- Connecté au serveur pour exécuter les effets physiques : LEDs, sirène, lumière.
- Peut servir de “poste de croupier” physique (roulette mini, machine à sous).
- Héberge éventuellement une petite base de données locale ou un cache.

### **Application mobile (NextJS)**

- Interface de connexion et de gestion du compte joueur.
- Accès aux jeux du casino.
- Historique des gains, classements, et stats.
- Interaction directe avec le serveur (mises, résultats, IA).

### **Intelligence Artificielle**

- **Classification** : analyse du profil du joueur → type de joueur (“chanceux”, “prudent”, “gambler fou”).
- **Régression** : prédiction du taux de gain potentiel en fonction de l’historique.
- **IA Ollama** : simulation d’un croupier qui commente les parties et trolle les joueurs.

---

## **4. Technologies utilisées**

| Composant | Technologie / Langage |
| --- | --- |
| Serveur | Node.js |
| Base de données | PostgreSQL |
| Mobile | NextJS |
| Embarqué | Raspberry Pi (Python, GPIO, MQTT ou HTTP) |
| IA | Ollama (modèle local) |
| Communication | REST API + WebSocket pour jeux en temps réel |

---

## **5. Scénario d’utilisation**

1. Le joueur ouvre l’application et se connecte à son compte.
2. Il choisit un jeu (ex. Machine à sous).
3. Lors d’un gros gain, le Raspberry Pi déclenche la **sirène** et **change la couleur de la lampe**.
4. L’IA commente la partie : “Tu viens de perdre 3 fois d’affilée, faut peut-être aller boire un verre…”
5. Les statistiques s’actualisent en temps réel sur le tableau global du serveur.

---

## **6. Objectifs techniques atteints**

- Serveur distribué
- Raspberry Pi intégré
- Base de données
- Application mobile NextJS
- IA (classification / régression)
- Actionneurs physiques (sirène, lampe

---

## **7. Répartition des charges de travail**

- Fabian : Interface mobile + Dashboard
- Jules : Chef de projet & responsable serveur
- Aina : Développeur embarqué / actionneurs
- Ethann : Développeur embarqué / actionneurs
