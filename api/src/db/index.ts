import { Kysely, PostgresDialect } from "kysely"
import { Pool } from "pg"
import type { Database } from "./database.ts"
import "dotenv/config"

declare global {
  var __db__: Kysely<Database> | undefined
}

export const db: Kysely<Database> =
  global.__db__ ??
  new Kysely<Database>({
    dialect: new PostgresDialect({
      pool: new Pool({
        host: process.env.DB_HOST,
        port: Number(process.env.DB_PORT ?? 5432),
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
      }),
    }),
  })
