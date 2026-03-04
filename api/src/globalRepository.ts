import crypto from "crypto"
import { db } from "./db/index.ts"
import type { GameType, BetKind } from "./db/database.ts"

export const getUserByUsername = (username: string) => {
  return db
    .selectFrom("users")
    .selectAll()
    .where("username", "=", username)
    .executeTakeFirst()
}

export const getUserById = (userId: string) => {
  return db
    .selectFrom("users")
    .selectAll()
    .where("id", "=", userId)
    .executeTakeFirst()
}

export const getUserByUsernameOrEmail = (username: string, email: string) => {
  return db
    .selectFrom("users")
    .select(["username", "email"])
    .where((eb) =>
      eb.or([eb("email", "=", email), eb("username", "=", username)]),
    )
    .executeTakeFirst()
}

export const createUser = (
  userId: string,
  username: string,
  hashedPassword: string,
  email: string,
) => {
  return db
    .insertInto("users")
    .values({
      id: userId,
      username,
      password: hashedPassword,
      email,
      is_active: true,
      last_login: new Date(),
      created_at: new Date(),
      balance: 0,
    })
    .execute()
}


export const createParty = (gameType: GameType) => {
  const partyId = crypto.randomUUID()
  return db
    .insertInto("parties")
    .values({
      id: partyId,
      game_type: gameType,
      created_at: new Date(),
      finished_at: null,
    })
    .returning("id")
    .executeTakeFirstOrThrow()
}

export const finishParty = (partyId: string) => {
  return db
    .updateTable("parties")
    .set({ finished_at: new Date() })
    .where("id", "=", partyId)
    .execute()
}

export const placeBet = (
  partyId: string,
  userId: string,
  amount: number,
  betType: BetKind,
  selection: unknown,
) => {
  return db
    .insertInto("bets")
    .values({
      id: crypto.randomUUID(),
      party_id: partyId,
      user_id: userId,
      amount,
      kind: betType,
      selection: JSON.stringify(selection),
      created_at: new Date(),
    })
    .execute()
}
