import crypto from "crypto"
import { db } from "../../db/index.ts"

export const saveBlackjackResult = (
  userId: string,
  partyId: string,
  won: boolean,
  gain: number,
) => {
  return db
    .insertInto("blackjack_results")
    .values({
      id: crypto.randomUUID(),
      user_id: userId,
      party_id: partyId,
      won,
      gain,
      created_at: new Date(),
    })
    .execute()
}
