# WebSocket API – Jeux (Mobile Client)

Cette documentation décrit comment communiquer avec les serveurs WebSocket des jeux côté mobile.

---

## 📡 Connexion WebSocket

Chaque jeu possède son **propre serveur WebSocket** et son **port dédié** :

| Jeu       | Port |
| --------- | ---- |
| Roulette  | 5600 |
| Slots     | 5700 |
| Blackjack | 5800 |
| Poker     | 5900 |

### Exemple de connexion (roulette)

ws://<host>:5600

---

## 🧱 Format des messages (OBLIGATOIRE)

👉 **Tous les messages échangés (client ↔ serveur) respectent ce format** :

```json
{
  "type": "STRING",
  "payload": {}
}
```

type : identifiant du message (string)

payload : données associées au message (objet ou tableau)

❌ Aucun message ne sera traité sans ces deux champs.

🎮 Roulette – Logique générale
La roulette fonctionne par phases successives, envoyées par le serveur.

Phases possibles
BETTING : le joueur peut miser

SPINNING : les mises sont fermées

RESULT : résultat et gains envoyés

Le serveur envoie automatiquement les changements de phase.

## 🔁 Message serveur → client

PHASE_UPDATE
Envoyé à tous les clients à chaque changement de phase.

```
{
  "type": "PHASE_UPDATE",
  "payload": {
    "phase": "BETTING",
    "endsAt": 1700000000000
  }
}
```

phase : phase actuelle

endsAt : timestamp (ms) de fin de la phase

👉 Le client doit utiliser cette info pour activer/désactiver l’UI de mise.

BET_RESULT
Envoyé uniquement au joueur concerné après un tour.

```
{
  "type": "BET_RESULT",
  "payload": {
    "gains": 300,
    "roulettteRandomResult": 17
  }
}
```

gains : gains totaux du joueur pour ce tour

roulettteRandomResult : numéro sorti (0–36)

## 📤 Message client → serveur

PLACE_BET
⚠️ Autorisé uniquement pendant la phase BETTING
Sinon, la mise sera refusée.

```
{
  "type": "PLACE_BET",
  "payload": [
    {
      "choice": "red",
      "amount": 100
    },
    {
      "numbers": [1, 2, 4, 5],
      "amount": 100
    },
    {
      "dozen": "13-24",
      "amount": 100
    }
  ]
}
```

👉 Le payload est un tableau de mises.

Types de mises supportées
Couleur (red, black)

Numéros (numbers: number[])

Douzaines (1-12, 13-24, 25-36)

Montant (amount)

🧪 Exemple client (Node.js – debug)
Un fichier de référence existe pour comprendre le flux complet :

## 📄 client.ts

```
import WebSocket from "ws"

// attendre que l’API soit prête
await new Promise((resolve) => setTimeout(resolve, 2000))

const ws = new WebSocket("ws://localhost:5600")

ws.on("open", () => {
  console.log("connected")
})

ws.on("message", (raw) => {
  const msg = JSON.parse(raw.toString())

  if (msg.type === "PHASE_UPDATE" && msg.payload.phase === "BETTING") {
    ws.send(
      JSON.stringify({
        type: "PLACE_BET",
        payload: [
          { choice: "red", amount: 100 },
          { numbers: [1, 2, 4, 5], amount: 100 },
          { dozen: "13-24", amount: 100 }
        ]
      })
    )
  }

  if (msg.type === "BET_RESULT") {
    console.log(msg.payload)
  }
})
```

👉 Ce fichier montre :

la réception des phases

l’envoi automatique des mises

la réception du résultat

## ⚠️ Règles importantes côté client

❌ Ne jamais envoyer de mise hors phase BETTING

❌ Ne jamais supposer une phase sans PHASE_UPDATE

✅ Toujours parser les messages via type

✅ Gérer la reconnexion WebSocket si besoin

✅ Le serveur est autoritaire (le client ne calcule rien)

## 🔮 À venir (non implémenté encore)

Gestion du solde joueur

Erreurs structurées (ERROR message)

Identité joueur persistante

Reconnexion avec récupération d’état

Si besoin, se référer au serveur pour la logique exacte :

`games/roulette/RouletteGame.ts`

`calculateRouletteGains.usecase.ts`
