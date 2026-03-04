export interface UserTable {
  id: string
  username: string
  password_hash: string
  email: string
  is_active: boolean
  created_at: Date
  last_login: Date | null
  balance: number
}

export type GameType = "slot" | "roulette" | "blackjack"

export type BetKind =
  | "roulette_color"
  | "roulette_number"
  | "roulette_parity"
  | "roulette_dozen"
  | "roulette_column"
  | "roulette_low_high"
  | "blackjack_win"
  | "slot_spin"

export interface PartiesTable {
  id: string
  user_id: string
  game_type: GameType
  created_at: Date
  finished_at: Date | null
}

export interface BetsTable {
  id: string
  party_id: string
  user_id: string
  amount: number
  kind: BetKind
  selection: unknown
  created_at: Date
}

export interface SlotResultsTable {
  party_id: string
  result: string
  gain: number
  created_at: Date
}

export interface RouletteResultsTable {
  party_id: string
  number: number
  color: string
  gain: number
  created_at: Date
}

export interface BlackjackResultsTable {
  party_id: string
  won: boolean
  gain: number
  created_at: Date
}

export interface WalletTransactionsTable {
  id: string
  user_id: string
  amount: number
  reason: string
  reference_id: string | null
  created_at: Date
  bonus_day: Date | null
}

export interface Database {
  users: UserTable
  parties: PartiesTable
  bets: BetsTable
  slot_results: SlotResultsTable
  roulette_results: RouletteResultsTable
  blackjack_results: BlackjackResultsTable
  wallet_transactions: WalletTransactionsTable
}
