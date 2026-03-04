export type GameType = 'slot' | 'roulette' | 'blackjack'

export type BetKind =
  | 'roulette_color'
  | 'roulette_number'
  | 'roulette_parity'
  | 'roulette_dozen'
  | 'roulette_column'
  | 'roulette_low_high'
  | 'blackjack_win'
  | 'slot_spin'

export interface UserTable {
  id: string
  username: string
  email: string
  password: string
  is_active: boolean
  created_at: Date
  last_login: Date | null
  balance: number
}

export interface PartyTable {
  id: string
  user_id: string
  game_type: GameType
  created_at: Date
  finished_at: Date | null
}

export interface BetTable {
  id: string
  party_id: string
  user_id: string
  amount: number
  kind: BetKind
  selection: Record<string, unknown>
  created_at: Date
}

export interface SlotResultTable {
  party_id: string
  result: string
  gain: number
  created_at: Date
}

export interface RouletteResultTable {
  party_id: string
  number: number
  color: 'red' | 'black' | 'green'
  gain: number
  created_at: Date
}

export interface BlackjackResultTable {
  party_id: string
  won: boolean
  gain: number
  created_at: Date
}

export interface WalletTransactionTable {
  id: string
  user_id: string
  amount: number
  reason: string
  reference_id: string | null
  created_at: Date
}

export interface Database {
  users: UserTable
  parties: PartyTable
  bets: BetTable
  slot_results: SlotResultTable
  roulette_results: RouletteResultTable
  blackjack_results: BlackjackResultTable
  wallet_transactions: WalletTransactionTable
}