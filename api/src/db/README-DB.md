# Fonctionnement des Triggers

Les **triggers (déclencheurs)** permettent d’exécuter automatiquement du code lorsqu’un événement se produit dans la base de données.

Dans ce projet, ils servent à **gérer automatiquement le portefeuille des joueurs** afin de garantir la cohérence des soldes.

Les triggers sont exécutés **après certaines opérations (`INSERT`) sur des tables spécifiques**.

---

# 1. Trigger : Mise à jour du solde du portefeuille

## Objectif

Mettre à jour automatiquement le **solde (`balance`) de l’utilisateur** lorsqu’une transaction est ajoutée dans la table :

```
wallet_transactions
```

## Fonction utilisée

```sql
apply_wallet_transaction()
```

## Fonctionnement

Lorsqu’une nouvelle transaction est créée :

1. Le trigger récupère l’utilisateur concerné (`NEW.user_id`).
2. Il met à jour son solde en ajoutant le montant de la transaction.
3. Il vérifie que le solde ne devient pas négatif.

### Logique appliquée

```
balance = balance + amount
```

* Si `amount` est positif → crédit
* Si `amount` est négatif → débit

### Sécurité

Si le solde devient négatif :

```
ERROR: Insufficient funds
```

La transaction est alors **annulée**.

## Trigger associé

```sql
CREATE TRIGGER trg_apply_wallet_transaction
AFTER INSERT ON wallet_transactions
FOR EACH ROW
EXECUTE FUNCTION apply_wallet_transaction();
```

Cela signifie :

* le trigger se déclenche **après chaque insertion**
* il s’exécute **pour chaque ligne ajoutée**

---

# 2. Trigger : Débit automatique lors d’un pari

## Objectif

Lorsqu’un joueur place un pari, son portefeuille doit être **débité automatiquement**.

## Fonction utilisée

```sql
create_wallet_debit_for_bet()
```

## Fonctionnement

Quand un pari est ajouté dans la table :

```
bets
```

le trigger :

1. récupère l’utilisateur (`NEW.user_id`)
2. crée une transaction dans `wallet_transactions`
3. ajoute un montant **négatif**

### Transaction créée

```
amount = -NEW.amount
reason = 'bet_debit'
reference_id = bet.id
```

Exemple :

```
pari = 10€
wallet_transaction = -10€
```

Le trigger précédent met ensuite à jour le solde.

## Trigger associé

```sql
CREATE TRIGGER trg_bet_wallet_debit
AFTER INSERT ON bets
FOR EACH ROW
EXECUTE FUNCTION create_wallet_debit_for_bet();
```

---

# 3. Trigger : Crédit automatique lors d’un gain

## Objectif

Lorsqu’un résultat de jeu est enregistré et qu’un joueur gagne, son portefeuille doit être **crédité automatiquement**.

## Fonction utilisée

```
create_wallet_credit_for_result()
```

## Fonctionnement

Quand un résultat est ajouté dans :

* `slot_results`
* `roulette_results`
* `blackjack_results`

le trigger :

1. récupère l’utilisateur associé à la partie
2. vérifie si le gain est supérieur à 0
3. crée une transaction dans `wallet_transactions`

### Transaction créée

```
amount = gain
reason = 'game_win'
reference_id = game_id
```

Exemple :

```
gain = 50€
wallet_transaction = +50€
```

Le trigger de mise à jour du solde est ensuite exécuté.

## Triggers associés

```
trg_slot_wallet_credit
trg_roulette_wallet_credit
trg_blackjack_wallet_credit
```

Tous utilisent la même fonction.

---

# Enchaînement des triggers

Les triggers fonctionnent en **cascade**.

## Cas d’un pari

```
Insertion dans bets
        ↓
Trigger : create_wallet_debit_for_bet
        ↓
Insertion dans wallet_transactions
        ↓
Trigger : apply_wallet_transaction
        ↓
Mise à jour du solde utilisateur
```

---

## Cas d’un gain

```
Insertion dans roulette_results
        ↓
Trigger : create_wallet_credit_for_result
        ↓
Insertion dans wallet_transactions
        ↓
Trigger : apply_wallet_transaction
        ↓
Mise à jour du solde utilisateur
```