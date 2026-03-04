import crypto from "crypto"
import { db } from "../../db/index.ts"

export const saveSlotResult = (
  userId: string,
  partyId: string,
  result: string[],
  gain: number,
) => {
  return db
    .insertInto("slot_results")
    .values({
      id: crypto.randomUUID(),
      user_id: userId,
      party_id: partyId,
      result: JSON.stringify(result), // fallen symbols
      gain,
      created_at: new Date(),
    })
    .execute()
}
