import {
  HandRank,
  RANK_VALUE_MAP,
  type Card,
  type HandEvaluation,
} from "./utils/poker.model.ts"

function combinations(cards: Card[], size: number): Card[][] {
  const result: Card[][] = []
  const currentCombo: Card[] = []

  function backtrack(start: number) {
    if (currentCombo.length === size) {
      result.push([...currentCombo])
      return
    }
    for (let index = start; index < cards.length; index++) {
      const card = cards[index]
      if (card) {
        currentCombo.push(card)
        backtrack(index + 1)
        currentCombo.pop()
      }
    }
  }

  backtrack(0)
  return result
}

function classifyFive(cards: Card[]): HandEvaluation {
  const values = cards
    .map((card) => RANK_VALUE_MAP[card.rank])
    .sort((left, right) => right - left)
  const suits = cards.map((card) => card.suit)

  const isFlush = suits.every((suit) => suit === suits[0])

  // Check straight (including wheel A-2-3-4-5)
  let isStraight = false
  let straightHigh = 0

  const uniqueValues = [...new Set(values)].sort((left, right) => right - left)
  if (uniqueValues.length === 5) {
    const high = uniqueValues[0] ?? 0
    const low = uniqueValues[4] ?? 0
    if (high - low === 4) {
      isStraight = true
      straightHigh = high
    }
    // Wheel: A-2-3-4-5
    if (
      uniqueValues[0] === 14 &&
      uniqueValues[1] === 5 &&
      uniqueValues[2] === 4 &&
      uniqueValues[3] === 3 &&
      uniqueValues[4] === 2
    ) {
      isStraight = true
      straightHigh = 5
    }
  }

  // Count ranks
  const counts = new Map<number, number>()
  for (const value of values) {
    counts.set(value, (counts.get(value) || 0) + 1)
  }

  const groups = [...counts.entries()].sort((left, right) => {
    if (right[1] !== left[1]) return right[1] - left[1]
    return right[0] - left[0]
  })

  const primaryGroup = groups[0] ?? [0, 0]
  const secondaryGroup = groups[1] ?? [0, 0]

  let handRank: HandRank
  let rankValues: number[]

  if (isFlush && isStraight && straightHigh === 14) {
    handRank = HandRank.ROYAL_FLUSH
    rankValues = [14, 13, 12, 11, 10]
  } else if (isFlush && isStraight) {
    handRank = HandRank.STRAIGHT_FLUSH
    rankValues = [straightHigh, 0, 0, 0, 0]
  } else if (primaryGroup[1] === 4) {
    handRank = HandRank.FOUR_OF_A_KIND
    rankValues = [primaryGroup[0], secondaryGroup[0], 0, 0, 0]
  } else if (primaryGroup[1] === 3 && secondaryGroup[1] === 2) {
    handRank = HandRank.FULL_HOUSE
    rankValues = [primaryGroup[0], secondaryGroup[0], 0, 0, 0]
  } else if (isFlush) {
    handRank = HandRank.FLUSH
    rankValues = values
  } else if (isStraight) {
    handRank = HandRank.STRAIGHT
    rankValues = [straightHigh, 0, 0, 0, 0]
  } else if (primaryGroup[1] === 3) {
    handRank = HandRank.THREE_OF_A_KIND
    const kickers = groups
      .filter((group) => group[1] === 1)
      .map((group) => group[0])
      .sort((left, right) => right - left)
    rankValues = [primaryGroup[0], kickers[0] ?? 0, kickers[1] ?? 0, 0, 0]
  } else if (primaryGroup[1] === 2 && secondaryGroup[1] === 2) {
    const highPair = Math.max(primaryGroup[0], secondaryGroup[0])
    const lowPair = Math.min(primaryGroup[0], secondaryGroup[0])
    const kicker = groups.find((group) => group[1] === 1)?.[0] ?? 0
    handRank = HandRank.TWO_PAIR
    rankValues = [highPair, lowPair, kicker, 0, 0]
  } else if (primaryGroup[1] === 2) {
    const kickers = groups
      .filter((group) => group[1] === 1)
      .map((group) => group[0])
      .sort((left, right) => right - left)
    handRank = HandRank.ONE_PAIR
    rankValues = [primaryGroup[0], kickers[0] ?? 0, kickers[1] ?? 0, kickers[2] ?? 0, 0]
  } else {
    handRank = HandRank.HIGH_CARD
    rankValues = values
  }

  const score =
    handRank * 15 ** 5 +
    (rankValues[0] ?? 0) * 15 ** 4 +
    (rankValues[1] ?? 0) * 15 ** 3 +
    (rankValues[2] ?? 0) * 15 ** 2 +
    (rankValues[3] ?? 0) * 15 +
    (rankValues[4] ?? 0)

  return { handRank, rankValues, score, bestCards: cards }
}

export function evaluateHand(cards: Card[]): HandEvaluation {
  if (cards.length < 5) {
    throw new Error(`Need at least 5 cards, got ${cards.length}`)
  }

  if (cards.length === 5) {
    return classifyFive(cards)
  }

  const combos = combinations(cards, 5)
  let bestEvaluation: HandEvaluation | null = null

  for (const combo of combos) {
    const evaluation = classifyFive(combo)
    if (!bestEvaluation || evaluation.score > bestEvaluation.score) {
      bestEvaluation = evaluation
    }
  }

  if (!bestEvaluation) {
    throw new Error("No valid hand combination found")
  }

  return bestEvaluation
}
