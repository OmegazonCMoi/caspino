import crypto from "crypto"
import { db } from "../../db/index.ts"
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

const getBetSelection = (bet: Bet): Record<string, unknown> => {
  if (isNumberBet(bet)) return { number: bet.numbers[0] }
  if (isTwoTimesBet(bet)) {
    switch (bet.choice) {
      case "red":
      case "black":
        return { color: bet.choice }
      case "even":
      case "odd":
        return { parity: bet.choice }
      case "1-18":
        return { range: "low" }
      case "19-36":
        return { range: "high" }
    }
  }
  if (isThreeTimesBet(bet)) {
    const dozenMap: Record<string, number> = { "1-12": 1, "13-24": 2, "25-36": 3 }
    return { dozen: dozenMap[bet.dozen] }
  }
  return {}
}

export const placeRouletteBets = (
  partyId: string,
  userId: string,
  bets: Bet[],
) => {
  return db.transaction().execute(async (trx) => {
    const totalBet = bets.reduce((sum, bet) => sum + bet.amount, 0)

    const user = await trx
      .selectFrom("users")
      .select("balance")
      .where("id", "=", userId)
      .forUpdate()
      .executeTakeFirstOrThrow()

    if (Number(user.balance) < totalBet) {
      throw new Error("Solde insuffisant")
    }

    for (const bet of bets) {
      if (isNumberBet(bet) && bet.numbers.length > 1) {
        const amountPerNumber = bet.amount / bet.numbers.length
        for (const number of bet.numbers) {
          await trx
            .insertInto("bets")
            .values({
              id: crypto.randomUUID(),
              party_id: partyId,
              user_id: userId,
              amount: amountPerNumber,
              kind: "roulette_number",
              selection: JSON.stringify({ number }),
              created_at: new Date(),
            })
            .execute()
        }
      } else {
        await trx
          .insertInto("bets")
          .values({
            id: crypto.randomUUID(),
            party_id: partyId,
            user_id: userId,
            amount: bet.amount,
            kind: getBetKind(bet),
            selection: JSON.stringify(getBetSelection(bet)),
            created_at: new Date(),
          })
          .execute()
      }
    }
  })
}
