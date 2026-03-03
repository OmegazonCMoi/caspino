import { randomUUID } from "crypto"
import type WebSocket from "ws"
import { buildSidePots, calculatePokerGains } from "./calculatePokerGains.ts"
import {
  DEFAULT_TABLE_CONFIG,
  PlayerStatus,
  PokerAction,
  PokerPhase,
  RANKS,
  SUITS,
  type Card,
  type HandResultPayload,
  type Player,
  type PokerTableConfig,
  type Pot,
  type ServerMessage,
  type TableStatePayload,
} from "./utils/poker.model.ts"

export class PokerTable {
  private players: Map<string, Player> = new Map()
  private wsSeatMap: Map<WebSocket, string> = new Map()
  private phase: PokerPhase = PokerPhase.WAITING
  private communityCards: Card[] = []
  private deck: Card[] = []
  private pots: Pot[] = []
  private currentBet = 0
  private minRaise = 0
  private dealerSeatIndex = -1
  private currentPlayerIndex = -1
  private config: PokerTableConfig
  private handScheduled = false
  private disconnectTimers: Map<string, ReturnType<typeof setTimeout>> =
    new Map()

  constructor(
    private broadcast: (msg: ServerMessage) => void,
    private message: (ws: WebSocket, msg: ServerMessage) => void,
    config?: Partial<PokerTableConfig>,
  ) {
    this.config = { ...DEFAULT_TABLE_CONFIG, ...config }
  }

  // ── Public API ──────────────────────────────────────────

  joinTable(ws: WebSocket, name: string, buyIn: number): void {
    if (buyIn < this.config.minBuyIn || buyIn > this.config.maxBuyIn) {
      this.message(ws, {
        type: "ERROR",
        payload: {
          message: `Buy-in must be between ${this.config.minBuyIn} and ${this.config.maxBuyIn}`,
        },
      })
      return
    }

    // Check if reconnecting
    for (const [playerId, player] of this.players) {
      if (player.name === name && this.disconnectTimers.has(playerId)) {
        const timer = this.disconnectTimers.get(playerId)
        if (timer) clearTimeout(timer)
        this.disconnectTimers.delete(playerId)
        player.ws = ws
        this.wsSeatMap.set(ws, playerId)
        this.broadcastTableState()
        return
      }
    }

    const seatIndex = this.findFreeSeat()
    if (seatIndex === -1) {
      this.message(ws, {
        type: "ERROR",
        payload: { message: "Table is full" },
      })
      return
    }

    const playerId = randomUUID()
    const player: Player = {
      id: playerId,
      ws,
      seatIndex,
      name,
      chips: buyIn,
      holeCards: [],
      status: PlayerStatus.SITTING_OUT,
      currentBet: 0,
      totalBetThisHand: 0,
      hasActedThisRound: false,
    }

    this.players.set(playerId, player)
    this.wsSeatMap.set(ws, playerId)

    this.broadcast({
      type: "PLAYER_JOINED",
      payload: { name, seatIndex },
    })

    this.broadcastTableState()
    this.maybeScheduleHand()
  }

  leaveTable(ws: WebSocket): void {
    const playerId = this.wsSeatMap.get(ws)
    if (!playerId) return

    const player = this.players.get(playerId)
    if (!player) return

    // Auto-fold if in a hand
    if (
      player.status === PlayerStatus.ACTIVE ||
      player.status === PlayerStatus.ALL_IN
    ) {
      player.status = PlayerStatus.FOLDED
      if (this.isCurrentPlayer(playerId)) {
        this.advanceToNextPlayer()
      }
    }

    const { name, seatIndex } = player
    this.players.delete(playerId)
    this.wsSeatMap.delete(ws)

    this.broadcast({
      type: "PLAYER_LEFT",
      payload: { name, seatIndex },
    })

    this.checkSinglePlayerWin()
    this.broadcastTableState()
  }

  handleDisconnect(ws: WebSocket): void {
    const playerId = this.wsSeatMap.get(ws)
    if (!playerId) return

    const player = this.players.get(playerId)
    if (!player) return

    this.wsSeatMap.delete(ws)

    // If it's their turn, auto-fold or auto-check
    if (this.isCurrentPlayer(playerId)) {
      if (player.currentBet >= this.currentBet) {
        player.hasActedThisRound = true
      } else {
        player.status = PlayerStatus.FOLDED
      }
      this.advanceToNextPlayer()
    }

    // Reserve seat for 60s
    const disconnectTimer = setTimeout(() => {
      this.disconnectTimers.delete(playerId)
      if (
        player.status === PlayerStatus.ACTIVE ||
        player.status === PlayerStatus.ALL_IN
      ) {
        player.status = PlayerStatus.FOLDED
        if (this.isCurrentPlayer(playerId)) {
          this.advanceToNextPlayer()
        }
        this.checkSinglePlayerWin()
      }
      this.players.delete(playerId)
      this.broadcast({
        type: "PLAYER_LEFT",
        payload: { name: player.name, seatIndex: player.seatIndex },
      })
      this.broadcastTableState()
    }, 60_000)

    this.disconnectTimers.set(playerId, disconnectTimer)
  }

  handleAction(ws: WebSocket, action: PokerAction, amount?: number): void {
    const playerId = this.wsSeatMap.get(ws)
    if (!playerId) return

    if (!this.isCurrentPlayer(playerId)) {
      this.message(ws, {
        type: "ERROR",
        payload: { message: "Not your turn" },
      })
      return
    }

    const player = this.players.get(playerId)
    if (!player) return

    switch (action) {
      case PokerAction.FOLD:
        this.handleFold(player)
        break
      case PokerAction.CHECK:
        this.handleCheck(player, ws)
        break
      case PokerAction.CALL:
        this.handleCall(player)
        break
      case PokerAction.BET:
        this.handleBet(player, amount ?? 0, ws)
        break
      case PokerAction.RAISE:
        this.handleRaise(player, amount ?? 0, ws)
        break
    }
  }

  // ── Hand lifecycle ──────────────────────────────────────

  private maybeScheduleHand(): void {
    if (this.handScheduled) return
    if (this.phase !== PokerPhase.WAITING) return

    const eligiblePlayers = this.getSeatedPlayers().filter(
      (player) => player.chips > 0,
    )
    if (eligiblePlayers.length >= 2) {
      this.handScheduled = true
      setTimeout(() => {
        this.handScheduled = false
        this.startHand()
      }, 3_000)
    }
  }

  private startHand(): void {
    const eligiblePlayers = this.getSeatedPlayers().filter(
      (player) => player.chips > 0,
    )
    if (eligiblePlayers.length < 2) {
      this.phase = PokerPhase.WAITING
      this.broadcastTableState()
      return
    }

    // Reset hand state
    this.communityCards = []
    this.pots = []
    this.currentBet = 0

    // Activate eligible players
    for (const player of eligiblePlayers) {
      player.status = PlayerStatus.ACTIVE
      player.holeCards = []
      player.currentBet = 0
      player.totalBetThisHand = 0
      player.hasActedThisRound = false
    }

    // Mark others as sitting out
    for (const player of this.players.values()) {
      if (!eligiblePlayers.includes(player)) {
        player.status = PlayerStatus.SITTING_OUT
        player.holeCards = []
        player.currentBet = 0
        player.totalBetThisHand = 0
        player.hasActedThisRound = false
      }
    }

    // Shuffle deck
    this.deck = this.createShuffledDeck()

    // Rotate dealer
    this.rotateDealerButton(eligiblePlayers)

    // Post blinds
    this.postBlinds(eligiblePlayers)

    // Deal hole cards
    for (const player of eligiblePlayers) {
      player.holeCards = [this.drawCard(), this.drawCard()]
    }

    // Start pre-flop
    this.phase = PokerPhase.PRE_FLOP
    this.minRaise = this.config.bigBlind

    // First to act: after BB (heads-up: dealer/SB acts first)
    const activePlayers = this.getActivePlayers()
    if (activePlayers.length === 2) {
      // Heads-up: dealer (who is SB) acts first pre-flop
      this.currentPlayerIndex = this.findPlayerIndexBySeat(
        this.dealerSeatIndex,
        activePlayers,
      )
    } else {
      const bbSeat = this.getBBSeatIndex(eligiblePlayers)
      this.currentPlayerIndex = this.nextActivePlayerIndex(
        this.findPlayerIndexBySeat(bbSeat, activePlayers),
        activePlayers,
      )
    }

    this.broadcastTableState()
  }

  private postBlinds(eligiblePlayers: Player[]): void {
    const isHeadsUp = eligiblePlayers.length === 2

    let sbPlayer: Player | undefined
    let bbPlayer: Player | undefined

    if (isHeadsUp) {
      sbPlayer = eligiblePlayers.find(
        (player) => player.seatIndex === this.dealerSeatIndex,
      )
      bbPlayer = eligiblePlayers.find(
        (player) => player.seatIndex !== this.dealerSeatIndex,
      )
    } else {
      const sbSeat = this.nextOccupiedSeat(
        this.dealerSeatIndex,
        eligiblePlayers,
      )
      const bbSeat = this.nextOccupiedSeat(sbSeat, eligiblePlayers)
      sbPlayer = eligiblePlayers.find(
        (player) => player.seatIndex === sbSeat,
      )
      bbPlayer = eligiblePlayers.find(
        (player) => player.seatIndex === bbSeat,
      )
    }

    if (sbPlayer) this.postBlind(sbPlayer, this.config.smallBlind)
    if (bbPlayer) this.postBlind(bbPlayer, this.config.bigBlind)
    this.currentBet = this.config.bigBlind
  }

  private postBlind(player: Player, amount: number): void {
    const actual = Math.min(amount, player.chips)
    player.chips -= actual
    player.currentBet = actual
    player.totalBetThisHand = actual
    if (player.chips === 0) {
      player.status = PlayerStatus.ALL_IN
    }
  }

  private getBBSeatIndex(eligiblePlayers: Player[]): number {
    if (eligiblePlayers.length === 2) {
      const bbPlayer = eligiblePlayers.find(
        (player) => player.seatIndex !== this.dealerSeatIndex,
      )
      return bbPlayer?.seatIndex ?? 0
    }
    const sbSeat = this.nextOccupiedSeat(
      this.dealerSeatIndex,
      eligiblePlayers,
    )
    return this.nextOccupiedSeat(sbSeat, eligiblePlayers)
  }

  // ── Action handlers ─────────────────────────────────────

  private handleFold(player: Player): void {
    player.status = PlayerStatus.FOLDED
    this.advanceToNextPlayer()
  }

  private handleCheck(player: Player, ws: WebSocket): void {
    if (player.currentBet < this.currentBet) {
      this.message(ws, {
        type: "ERROR",
        payload: { message: "Cannot check, there is a bet to match" },
      })
      return
    }
    player.hasActedThisRound = true
    this.advanceToNextPlayer()
  }

  private handleCall(player: Player): void {
    const toCall = this.currentBet - player.currentBet
    const actual = Math.min(toCall, player.chips)
    player.chips -= actual
    player.currentBet += actual
    player.totalBetThisHand += actual
    player.hasActedThisRound = true

    if (player.chips === 0) {
      player.status = PlayerStatus.ALL_IN
    }

    this.advanceToNextPlayer()
  }

  private handleBet(player: Player, amount: number, ws: WebSocket): void {
    if (this.currentBet > 0) {
      this.message(ws, {
        type: "ERROR",
        payload: { message: "Cannot bet, use raise instead" },
      })
      return
    }

    if (amount < this.config.bigBlind && amount < player.chips) {
      this.message(ws, {
        type: "ERROR",
        payload: { message: `Minimum bet is ${this.config.bigBlind}` },
      })
      return
    }

    const actual = Math.min(amount, player.chips)
    player.chips -= actual
    player.currentBet += actual
    player.totalBetThisHand += actual
    this.currentBet = player.currentBet
    this.minRaise = actual
    player.hasActedThisRound = true

    if (player.chips === 0) {
      player.status = PlayerStatus.ALL_IN
    }

    this.resetOthersActed(player.id)
    this.advanceToNextPlayer()
  }

  private handleRaise(player: Player, amount: number, ws: WebSocket): void {
    const raiseAbove = amount - this.currentBet

    if (
      raiseAbove < this.minRaise &&
      amount < player.chips + player.currentBet
    ) {
      this.message(ws, {
        type: "ERROR",
        payload: {
          message: `Minimum raise is ${this.minRaise} above current bet of ${this.currentBet}`,
        },
      })
      return
    }

    const toPay = amount - player.currentBet
    const actual = Math.min(toPay, player.chips)
    player.chips -= actual
    player.currentBet += actual
    player.totalBetThisHand += actual

    if (player.currentBet > this.currentBet) {
      this.minRaise = player.currentBet - this.currentBet
      this.currentBet = player.currentBet
    }

    player.hasActedThisRound = true

    if (player.chips === 0) {
      player.status = PlayerStatus.ALL_IN
    }

    this.resetOthersActed(player.id)
    this.advanceToNextPlayer()
  }

  // ── Turn management ─────────────────────────────────────

  private advanceToNextPlayer(): void {
    if (this.checkSinglePlayerWin()) return

    const activePlayers = this.getActivePlayers()

    // If all non-folded are ALL_IN (or only one ACTIVE left with matched bet), run out
    const nonFoldedPlayers = this.getNonFoldedPlayers()
    const playersWhoCanAct = nonFoldedPlayers.filter(
      (player) => player.status === PlayerStatus.ACTIVE,
    )

    if (
      playersWhoCanAct.length <= 1 &&
      this.isBettingComplete(nonFoldedPlayers)
    ) {
      this.runOutCommunityCards()
      return
    }

    if (activePlayers.length === 0) {
      this.runOutCommunityCards()
      return
    }

    // Find next active player who needs to act
    const startIdx = this.currentPlayerIndex
    let nextIdx = this.nextActivePlayerIndex(startIdx, activePlayers)
    let checkedCount = 0

    while (checkedCount < activePlayers.length) {
      const candidate = activePlayers[nextIdx]
      if (
        candidate &&
        candidate.status === PlayerStatus.ACTIVE &&
        (!candidate.hasActedThisRound ||
          candidate.currentBet < this.currentBet)
      ) {
        this.currentPlayerIndex = nextIdx
        this.broadcastTableState()
        return
      }
      nextIdx = this.nextActivePlayerIndex(nextIdx, activePlayers)
      checkedCount++
    }

    // Everyone has acted and matched — advance phase
    this.advancePhase()
  }

  private isBettingComplete(nonFoldedPlayers: Player[]): boolean {
    for (const player of nonFoldedPlayers) {
      if (player.status === PlayerStatus.ACTIVE) {
        if (
          !player.hasActedThisRound ||
          player.currentBet < this.currentBet
        ) {
          return false
        }
      }
    }
    return true
  }

  private advancePhase(): void {
    // Reset round state
    for (const player of this.players.values()) {
      if (
        player.status === PlayerStatus.ACTIVE ||
        player.status === PlayerStatus.ALL_IN
      ) {
        player.currentBet = 0
        player.hasActedThisRound = false
      }
    }
    this.currentBet = 0

    switch (this.phase) {
      case PokerPhase.PRE_FLOP:
        this.deck.pop() // burn
        this.communityCards.push(
          this.drawCard(),
          this.drawCard(),
          this.drawCard(),
        )
        this.phase = PokerPhase.FLOP
        break
      case PokerPhase.FLOP:
        this.deck.pop() // burn
        this.communityCards.push(this.drawCard())
        this.phase = PokerPhase.TURN
        break
      case PokerPhase.TURN:
        this.deck.pop() // burn
        this.communityCards.push(this.drawCard())
        this.phase = PokerPhase.RIVER
        break
      case PokerPhase.RIVER:
        this.resolveShowdown()
        return
    }

    // Set first to act: first active player after dealer
    const activePlayers = this.getActivePlayers()
    if (activePlayers.length === 0) {
      this.runOutCommunityCards()
      return
    }

    const dealerIdx = this.findClosestPlayerIndex(
      this.dealerSeatIndex,
      activePlayers,
    )
    this.currentPlayerIndex = this.nextActivePlayerIndex(
      dealerIdx,
      activePlayers,
    )
    this.minRaise = this.config.bigBlind

    this.broadcastTableState()
  }

  private runOutCommunityCards(): void {
    while (this.communityCards.length < 5) {
      this.deck.pop() // burn
      this.communityCards.push(this.drawCard())
    }
    this.resolveShowdown()
  }

  private resolveShowdown(): void {
    this.phase = PokerPhase.SHOWDOWN

    const allPlayers = [...this.players.values()]
    const pots = buildSidePots(allPlayers)

    const potWinners = calculatePokerGains(
      pots,
      allPlayers,
      this.communityCards,
      this.dealerSeatIndex,
    )

    // Award chips
    for (const potWinner of potWinners) {
      for (const winner of potWinner.winners) {
        const player = this.players.get(winner.playerId)
        if (player) {
          player.chips += winner.amount
        }
      }
    }

    const handResult: HandResultPayload = {
      potWinners,
      communityCards: this.communityCards,
      players: allPlayers.map((player) => ({
        seatIndex: player.seatIndex,
        name: player.name,
        cards:
          player.status !== PlayerStatus.FOLDED &&
          player.status !== PlayerStatus.SITTING_OUT
            ? player.holeCards
            : null,
        status: player.status,
      })),
    }

    for (const player of this.players.values()) {
      if (this.wsSeatMap.has(player.ws)) {
        this.message(player.ws, {
          type: "HAND_RESULT",
          payload: handResult,
        })
      }
    }

    this.phase = PokerPhase.WAITING
    this.currentPlayerIndex = -1

    for (const player of this.players.values()) {
      if (player.chips <= 0 && player.status !== PlayerStatus.SITTING_OUT) {
        player.status = PlayerStatus.SITTING_OUT
      }
    }

    this.broadcastTableState()
    this.maybeScheduleHand()
  }

  private checkSinglePlayerWin(): boolean {
    if (
      this.phase === PokerPhase.WAITING ||
      this.phase === PokerPhase.SHOWDOWN
    )
      return false

    const nonFoldedPlayers = this.getNonFoldedPlayers()
    if (nonFoldedPlayers.length !== 1) return false

    const winner = nonFoldedPlayers[0]
    if (!winner) return false

    // Collect all bets
    let totalPot = 0
    for (const player of this.players.values()) {
      totalPot += player.totalBetThisHand
    }
    winner.chips += totalPot

    const handResult: HandResultPayload = {
      potWinners: [
        {
          potIndex: 0,
          potAmount: totalPot,
          winners: [
            {
              playerId: winner.id,
              playerName: winner.name,
              amount: totalPot,
            },
          ],
        },
      ],
      communityCards: this.communityCards,
      players: [...this.players.values()].map((player) => ({
        seatIndex: player.seatIndex,
        name: player.name,
        cards: null,
        status: player.status,
      })),
    }

    for (const player of this.players.values()) {
      if (this.wsSeatMap.has(player.ws)) {
        this.message(player.ws, {
          type: "HAND_RESULT",
          payload: handResult,
        })
      }
    }

    this.phase = PokerPhase.WAITING
    this.currentPlayerIndex = -1
    this.broadcastTableState()
    this.maybeScheduleHand()
    return true
  }

  // ── Communication ───────────────────────────────────────

  private broadcastTableState(): void {
    for (const player of this.players.values()) {
      if (!this.wsSeatMap.has(player.ws)) continue

      const state = this.buildTableStateForPlayer(player)
      this.message(player.ws, { type: "TABLE_STATE", payload: state })
    }
  }

  private buildTableStateForPlayer(viewer: Player): TableStatePayload {
    const activePlayers = this.getActivePlayers()
    const currentPlayer =
      this.currentPlayerIndex >= 0 && activePlayers.length > 0
        ? activePlayers[this.currentPlayerIndex % activePlayers.length]
        : null

    const isMyTurn = currentPlayer?.id === viewer.id
    const availableActions = isMyTurn
      ? this.getAvailableActions(viewer)
      : []

    const allPlayers = [...this.players.values()]

    return {
      phase: this.phase,
      communityCards: this.communityCards,
      pots:
        this.phase !== PokerPhase.WAITING ? buildSidePots(allPlayers) : [],
      currentBet: this.currentBet,
      dealerSeatIndex: this.dealerSeatIndex,
      currentPlayerSeatIndex: currentPlayer?.seatIndex ?? null,
      yourCards: viewer.holeCards,
      yourChips: viewer.chips,
      yourSeatIndex: viewer.seatIndex,
      yourCurrentBet: viewer.currentBet,
      availableActions,
      minRaise: this.currentBet + this.minRaise,
      players: allPlayers.map((player) => ({
        seatIndex: player.seatIndex,
        name: player.name,
        chips: player.chips,
        status: player.status,
        currentBet: player.currentBet,
        cardCount: player.holeCards.length,
        cards:
          this.phase === PokerPhase.SHOWDOWN &&
          player.status !== PlayerStatus.FOLDED &&
          player.status !== PlayerStatus.SITTING_OUT
            ? player.holeCards
            : null,
      })),
    }
  }

  private getAvailableActions(player: Player): PokerAction[] {
    if (player.status !== PlayerStatus.ACTIVE) return []

    const actions: PokerAction[] = [PokerAction.FOLD]

    if (player.currentBet >= this.currentBet) {
      actions.push(PokerAction.CHECK)
    }

    if (player.currentBet < this.currentBet) {
      actions.push(PokerAction.CALL)
    }

    if (player.chips > 0) {
      if (this.currentBet === 0) {
        actions.push(PokerAction.BET)
      } else if (player.chips + player.currentBet > this.currentBet) {
        actions.push(PokerAction.RAISE)
      }
    }

    return actions
  }

  // ── Helpers ─────────────────────────────────────────────

  private drawCard(): Card {
    const card = this.deck.pop()
    if (!card) throw new Error("Deck is empty")
    return card
  }

  private getSeatedPlayers(): Player[] {
    return [...this.players.values()].sort(
      (left, right) => left.seatIndex - right.seatIndex,
    )
  }

  private getActivePlayers(): Player[] {
    return this.getSeatedPlayers().filter(
      (player) => player.status === PlayerStatus.ACTIVE,
    )
  }

  private getNonFoldedPlayers(): Player[] {
    return this.getSeatedPlayers().filter(
      (player) =>
        player.status === PlayerStatus.ACTIVE ||
        player.status === PlayerStatus.ALL_IN,
    )
  }

  private isCurrentPlayer(playerId: string): boolean {
    const activePlayers = this.getActivePlayers()
    if (this.currentPlayerIndex < 0 || activePlayers.length === 0)
      return false
    return (
      activePlayers[this.currentPlayerIndex % activePlayers.length]?.id ===
      playerId
    )
  }

  private findFreeSeat(): number {
    const takenSeats = new Set(
      [...this.players.values()].map((player) => player.seatIndex),
    )
    for (let seatIndex = 0; seatIndex < this.config.maxSeats; seatIndex++) {
      if (!takenSeats.has(seatIndex)) return seatIndex
    }
    return -1
  }

  private rotateDealerButton(eligiblePlayers: Player[]): void {
    if (this.dealerSeatIndex === -1) {
      const randomPlayer =
        eligiblePlayers[
          Math.floor(Math.random() * eligiblePlayers.length)
        ]
      this.dealerSeatIndex = randomPlayer?.seatIndex ?? 0
    } else {
      this.dealerSeatIndex = this.nextOccupiedSeat(
        this.dealerSeatIndex,
        eligiblePlayers,
      )
    }
  }

  private nextOccupiedSeat(
    fromSeat: number,
    players: Player[],
  ): number {
    const seats = players
      .map((player) => player.seatIndex)
      .sort((left, right) => left - right)
    for (const seat of seats) {
      if (seat > fromSeat) return seat
    }
    return seats[0] ?? 0
  }

  private findPlayerIndexBySeat(
    seatIndex: number,
    players: Player[],
  ): number {
    return players.findIndex((player) => player.seatIndex === seatIndex)
  }

  private findClosestPlayerIndex(
    seatIndex: number,
    players: Player[],
  ): number {
    let bestIndex = 0
    let bestDistance = Infinity
    for (let playerIndex = 0; playerIndex < players.length; playerIndex++) {
      const candidate = players[playerIndex]
      if (!candidate) continue
      const distance =
        (candidate.seatIndex - seatIndex + this.config.maxSeats) %
        this.config.maxSeats
      if (distance < bestDistance) {
        bestDistance = distance
        bestIndex = playerIndex
      }
    }
    return bestIndex
  }

  private nextActivePlayerIndex(
    currentIdx: number,
    players: Player[],
  ): number {
    if (players.length === 0) return -1
    return (currentIdx + 1) % players.length
  }

  private resetOthersActed(exceptId: string): void {
    for (const player of this.players.values()) {
      if (player.id !== exceptId && player.status === PlayerStatus.ACTIVE) {
        player.hasActedThisRound = false
      }
    }
  }

  private createShuffledDeck(): Card[] {
    const deck: Card[] = []
    for (const suit of SUITS) {
      for (const rank of RANKS) {
        deck.push({ suit, rank })
      }
    }
    // Fisher-Yates shuffle
    for (let cardIndex = deck.length - 1; cardIndex > 0; cardIndex--) {
      const swapIndex = Math.floor(Math.random() * (cardIndex + 1))
      const cardA = deck[cardIndex]
      const cardB = deck[swapIndex]
      if (cardA && cardB) {
        deck[cardIndex] = cardB
        deck[swapIndex] = cardA
      }
    }
    return deck
  }
}
