import { db } from "../../db/index.ts"
import { placeBet } from "../../globalRepository.ts"
import type { BetKind } from "../../db/database.ts"
import type {
  Bet,
  NumberBet,
  TwoTimesBet,
  ThreeTimesBet,
} from "./utils/roulette.model.ts"

export const saveRouletteResult = (
  userId: string,
  partyId: string,
  number: number,
  color: "red" | "black" | "green",
  gain: number,
) => {
  return db
    .insertInto("roulette_results")
    .values({
      party_id: partyId,
      user_id: userId,
      number,
      color,
      gain,
    })
    .execute()
}

const isNumberBet = (bet: Bet): bet is NumberBet =>
  Array.isArray((bet as NumberBet).numbers)

const isTwoTimesBet = (bet: Bet): bet is TwoTimesBet =>
  typeof (bet as TwoTimesBet).choice === "string"

const isThreeTimesBet = (bet: Bet): bet is ThreeTimesBet =>
  typeof (bet as ThreeTimesBet).dozen === "string"

const getBetKind = (bet: Bet): BetKind => {
  if (isNumberBet(bet)) return "roulette_number"
  if (isTwoTimesBet(bet)) {
    switch (bet.choice) {
      case "red":
      case "black":
        return "roulette_color"
      case "even":
      case "odd":
        return "roulette_parity"
      case "1-18":
      case "19-36":
        return "roulette_low_high"
    }
  }
  if (isThreeTimesBet(bet)) return "roulette_dozen"
  return "roulette_number"
}

const getBetSelection = (bet: Bet): unknown => {
  if (isNumberBet(bet)) return bet.numbers
  if (isTwoTimesBet(bet)) return bet.choice
  if (isThreeTimesBet(bet)) return bet.dozen
  return null
}

export const placeRouletteBets = (
  partyId: string,
  userId: string,
  bets: Bet[],
) => {
  return Promise.all(
    bets.map((bet) =>
      placeBet(
        partyId,
        userId,
        bet.amount,
        getBetKind(bet),
        getBetSelection(bet),
      ),
    ),
  )
}
