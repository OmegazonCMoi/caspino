import type WebSocket from "ws"

// ── Card types ──────────────────────────────────────────────

export type Suit = "hearts" | "diamonds" | "clubs" | "spades"
export type Rank =
  | "2"
  | "3"
  | "4"
  | "5"
  | "6"
  | "7"
  | "8"
  | "9"
  | "10"
  | "J"
  | "Q"
  | "K"
  | "A"

export interface Card {
  suit: Suit
  rank: Rank
}

export const SUITS: Suit[] = ["hearts", "diamonds", "clubs", "spades"]
export const RANKS: Rank[] = [
  "2",
  "3",
  "4",
  "5",
  "6",
  "7",
  "8",
  "9",
  "10",
  "J",
  "Q",
  "K",
  "A",
]

export const RANK_VALUE_MAP: Record<Rank, number> = {
  "2": 2,
  "3": 3,
  "4": 4,
  "5": 5,
  "6": 6,
  "7": 7,
  "8": 8,
  "9": 9,
  "10": 10,
  J: 11,
  Q: 12,
  K: 13,
  A: 14,
}

// ── Enums ───────────────────────────────────────────────────

export enum HandRank {
  HIGH_CARD = 0,
  ONE_PAIR = 1,
  TWO_PAIR = 2,
  THREE_OF_A_KIND = 3,
  STRAIGHT = 4,
  FLUSH = 5,
  FULL_HOUSE = 6,
  FOUR_OF_A_KIND = 7,
  STRAIGHT_FLUSH = 8,
  ROYAL_FLUSH = 9,
}

export enum PokerPhase {
  WAITING = "WAITING",
  PRE_FLOP = "PRE_FLOP",
  FLOP = "FLOP",
  TURN = "TURN",
  RIVER = "RIVER",
  SHOWDOWN = "SHOWDOWN",
}

export enum PlayerStatus {
  SITTING_OUT = "SITTING_OUT",
  ACTIVE = "ACTIVE",
  FOLDED = "FOLDED",
  ALL_IN = "ALL_IN",
}

export enum PokerAction {
  FOLD = "FOLD",
  CHECK = "CHECK",
  CALL = "CALL",
  BET = "BET",
  RAISE = "RAISE",
}

// ── Interfaces ──────────────────────────────────────────────

export interface HandEvaluation {
  handRank: HandRank
  rankValues: number[]
  score: number
  bestCards: Card[]
}

export interface Player {
  id: string
  ws: WebSocket
  seatIndex: number
  name: string
  chips: number
  holeCards: Card[]
  status: PlayerStatus
  currentBet: number
  totalBetThisHand: number
  hasActedThisRound: boolean
}

export interface Pot {
  amount: number
  eligiblePlayerIds: string[]
}

export interface PokerTableConfig {
  maxSeats: number
  smallBlind: number
  bigBlind: number
  minBuyIn: number
  maxBuyIn: number
}

export const DEFAULT_TABLE_CONFIG: PokerTableConfig = {
  maxSeats: 6,
  smallBlind: 5,
  bigBlind: 10,
  minBuyIn: 200,
  maxBuyIn: 1000,
}

// ── Messages client → server ────────────────────────────────

export interface JoinTableMessage {
  type: "JOIN_TABLE"
  payload: { name: string; buyIn: number }
}

export interface LeaveTableMessage {
  type: "LEAVE_TABLE"
}

export interface PlayerActionMessage {
  type: "PLAYER_ACTION"
  payload: { action: PokerAction; amount?: number }
}

export type ClientMessage =
  | JoinTableMessage
  | LeaveTableMessage
  | PlayerActionMessage

// ── Messages server → client ────────────────────────────────

export interface TableStatePayload {
  phase: PokerPhase
  communityCards: Card[]
  pots: Pot[]
  currentBet: number
  dealerSeatIndex: number
  currentPlayerSeatIndex: number | null
  yourCards: Card[]
  yourChips: number
  yourSeatIndex: number
  yourCurrentBet: number
  availableActions: PokerAction[]
  minRaise: number
  players: {
    seatIndex: number
    name: string
    chips: number
    status: PlayerStatus
    currentBet: number
    cardCount: number
    cards: Card[] | null
  }[]
}

export interface PotWinner {
  potIndex: number
  potAmount: number
  winners: {
    playerId: string
    playerName: string
    amount: number
    hand?: HandEvaluation
  }[]
}

export interface HandResultPayload {
  potWinners: PotWinner[]
  communityCards: Card[]
  players: {
    seatIndex: number
    name: string
    cards: Card[] | null
    status: PlayerStatus
  }[]
}

export type ServerMessage =
  | { type: "TABLE_STATE"; payload: TableStatePayload }
  | { type: "HAND_RESULT"; payload: HandResultPayload }
  | { type: "PLAYER_JOINED"; payload: { name: string; seatIndex: number } }
  | { type: "PLAYER_LEFT"; payload: { name: string; seatIndex: number } }
  | { type: "ERROR"; payload: { message: string } }
