import { evaluateHand } from "./evaluateHand.ts"
import {
  PlayerStatus,
  type Card,
  type Player,
  type Pot,
  type PotWinner,
} from "./utils/poker.model.ts"

export function buildSidePots(players: Player[]): Pot[] {
  const betEntries = players
    .filter((player) => player.totalBetThisHand > 0)
    .map((player) => ({ id: player.id, bet: player.totalBetThisHand, status: player.status }))

  if (betEntries.length === 0) return []

  const nonFoldedBets = betEntries
    .filter((entry) => entry.status !== PlayerStatus.FOLDED)
    .map((entry) => entry.bet)
  const uniqueLevels = [...new Set(nonFoldedBets)].sort((left, right) => left - right)

  const pots: Pot[] = []
  let previousLevel = 0

  for (const level of uniqueLevels) {
    const contribution = level - previousLevel
    if (contribution <= 0) continue

    let potAmount = 0
    const eligibleIds: string[] = []

    for (const entry of betEntries) {
      const availableBet = entry.bet - previousLevel
      if (availableBet > 0) {
        potAmount += Math.min(availableBet, contribution)
      }
      if (entry.status !== PlayerStatus.FOLDED && entry.bet >= level) {
        eligibleIds.push(entry.id)
      }
    }

    if (potAmount > 0) {
      pots.push({ amount: potAmount, eligiblePlayerIds: eligibleIds })
    }

    previousLevel = level
  }

  return pots
}

export function calculatePokerGains(
  pots: Pot[],
  players: Player[],
  communityCards: Card[],
  dealerSeatIndex: number,
): PotWinner[] {
  const playerMap = new Map(players.map((player) => [player.id, player]))
  const potWinners: PotWinner[] = []

  for (let potIndex = 0; potIndex < pots.length; potIndex++) {
    const pot = pots[potIndex]
    if (!pot) continue

    const eligiblePlayers = pot.eligiblePlayerIds
      .map((playerId) => playerMap.get(playerId))
      .filter((player): player is Player => player !== undefined && player.status !== PlayerStatus.FOLDED)

    if (eligiblePlayers.length === 0) continue

    // Single eligible player — wins without showdown
    if (eligiblePlayers.length === 1) {
      const winner = eligiblePlayers[0]
      if (!winner) continue
      potWinners.push({
        potIndex,
        potAmount: pot.amount,
        winners: [
          {
            playerId: winner.id,
            playerName: winner.name,
            amount: pot.amount,
          },
        ],
      })
      continue
    }

    // Evaluate hands
    const evaluations = eligiblePlayers.map((player) => ({
      player,
      eval: evaluateHand([...player.holeCards, ...communityCards]),
    }))

    const bestScore = Math.max(...evaluations.map((evaluation) => evaluation.eval.score))
    const winners = evaluations.filter((evaluation) => evaluation.eval.score === bestScore)

    // Split pot evenly, odd chip goes to player closest left of dealer
    const shareBase = Math.floor(pot.amount / winners.length)
    const remainder = pot.amount - shareBase * winners.length

    // Sort winners by seat position relative to dealer (clockwise)
    const sortedWinners = winners.sort((left, right) => {
      const distLeft =
        (left.player.seatIndex - dealerSeatIndex + players.length) %
        players.length
      const distRight =
        (right.player.seatIndex - dealerSeatIndex + players.length) %
        players.length
      return distLeft - distRight
    })

    const winnerResults = sortedWinners.map((winner, winnerIndex) => ({
      playerId: winner.player.id,
      playerName: winner.player.name,
      amount: shareBase + (winnerIndex === 0 && remainder > 0 ? remainder : 0),
      hand: winner.eval,
    }))

    potWinners.push({
      potIndex,
      potAmount: pot.amount,
      winners: winnerResults,
    })
  }

  return potWinners
}
