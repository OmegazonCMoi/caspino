import { BlackjackGame } from "./blackjackGame.ts"
import {
  HandStatus,
  MULTIPLIERS,
  type BlackjackResult,
  type BlackjackSession,
  type HandResult,
} from "./utils/blackjack.model.ts"

export const calculateBlackjackGains = (
  session: BlackjackSession,
): BlackjackResult => {
  const dealerValue = BlackjackGame.handValue(session.dealerHand)
  const dealerBlackjack =
    session.dealerHand.length === 2 && dealerValue === 21

  const handResults: HandResult[] = session.hands.map((hand) => {
    const playerValue = BlackjackGame.handValue(hand.cards)

    if (hand.status === HandStatus.BUSTED) {
      return {
        bet: hand.bet,
        gains: hand.bet * MULTIPLIERS.LOSS,
        playerValue,
        status: hand.status,
      }
    }

    if (
      hand.status === HandStatus.BLACKJACK &&
      hand.cards.length === 2 &&
      !dealerBlackjack
    ) {
      return {
        bet: hand.bet,
        gains: hand.bet * MULTIPLIERS.BLACKJACK,
        playerValue,
        status: hand.status,
      }
    }

    if (dealerValue > 21) {
      return {
        bet: hand.bet,
        gains: hand.bet * MULTIPLIERS.WIN,
        playerValue,
        status: hand.status,
      }
    }

    if (playerValue > dealerValue) {
      return {
        bet: hand.bet,
        gains: hand.bet * MULTIPLIERS.WIN,
        playerValue,
        status: hand.status,
      }
    }

    if (playerValue === dealerValue) {
      return {
        bet: hand.bet,
        gains: hand.bet * MULTIPLIERS.PUSH,
        playerValue,
        status: hand.status,
      }
    }

    return {
      bet: hand.bet,
      gains: hand.bet * MULTIPLIERS.LOSS,
      playerValue,
      status: hand.status,
    }
  })

  const insuranceGain =
    session.insuranceBet > 0 && dealerBlackjack
      ? session.insuranceBet * MULTIPLIERS.INSURANCE
      : 0

  const totalGains =
    handResults.reduce((sum, r) => sum + r.gains, 0) + insuranceGain

  return { totalGains, handResults, insuranceGain }
}
