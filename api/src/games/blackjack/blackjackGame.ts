import type WebSocket from "ws"
import { calculateBlackjackGains } from "./calculateBlackjackGains.ts"
import {
  BlackjackAction,
  HandStatus,
  SessionPhase,
  SUITS,
  RANKS,
  DECK_COUNT,
  RESHUFFLE_THRESHOLD,
  type Card,
  type Hand,
  type BlackjackSession,
} from "./utils/blackjack.model.ts"

export class BlackjackGame {
  private deck: Card[] = []
  private sessions: Map<WebSocket, BlackjackSession> = new Map()

  constructor(private message: (ws: WebSocket, msg: any) => void) {
    this.initDeck()
  }

  // --- Deck management ---

  private initDeck() {
    this.deck = []
    for (let deckIndex = 0; deckIndex < DECK_COUNT; deckIndex++) {
      for (const suit of SUITS) {
        for (const rank of RANKS) {
          this.deck.push({ suit, rank })
        }
      }
    }
    this.shuffleDeck()
  }

  private shuffleDeck() {
    for (let cardIndex = this.deck.length - 1; cardIndex > 0; cardIndex--) {
      const swapIndex = Math.floor(Math.random() * (cardIndex + 1))
      const currentCard = this.deck[cardIndex]
      const swapCard = this.deck[swapIndex]
      if (currentCard && swapCard) {
        this.deck[cardIndex] = swapCard
        this.deck[swapIndex] = currentCard
      }
    }
  }

  private drawCard(): Card {
    const totalCards = DECK_COUNT * SUITS.length * RANKS.length
    if (this.deck.length < totalCards * RESHUFFLE_THRESHOLD) {
      this.initDeck()
    }
    const card = this.deck.pop()
    if (!card) throw new Error("Deck is empty")
    return card
  }

  // --- Hand value ---

  static handValue(cards: Card[]): number {
    let value = 0
    let aces = 0

    for (const card of cards) {
      if (card.rank === "A") {
        value += 11
        aces++
      } else if (["K", "Q", "J"].includes(card.rank)) {
        value += 10
      } else {
        value += parseInt(card.rank)
      }
    }

    while (value > 21 && aces > 0) {
      value -= 10
      aces--
    }

    return value
  }

  static isBlackjack(cards: Card[]): boolean {
    return cards.length === 2 && BlackjackGame.handValue(cards) === 21
  }

  // --- Game flow ---

  private getActiveHand(session: BlackjackSession): Hand {
    const hand = session.hands[session.activeHandIndex]
    if (!hand) throw new Error("No active hand")
    return hand
  }

  startGame(bet: number, ws: WebSocket) {
    if (bet <= 0) throw new Error("Bet must be positive")

    const playerCards = [this.drawCard(), this.drawCard()]
    const dealerCards = [this.drawCard(), this.drawCard()]

    const hand: Hand = {
      cards: playerCards,
      bet,
      status: BlackjackGame.isBlackjack(playerCards)
        ? HandStatus.BLACKJACK
        : HandStatus.PLAYING,
      isDoubled: false,
    }

    const session: BlackjackSession = {
      hands: [hand],
      activeHandIndex: 0,
      dealerHand: dealerCards,
      phase: SessionPhase.PLAYER_TURN,
      insuranceBet: 0,
    }

    this.sessions.set(ws, session)

    // Natural blackjack — resolve immediately
    if (hand.status === HandStatus.BLACKJACK) {
      this.resolveGame(ws, session)
      return
    }

    this.sendGameState(ws, session)
  }

  handleAction(action: BlackjackAction, ws: WebSocket) {
    const session = this.sessions.get(ws)
    if (!session) throw new Error("No active session")
    if (session.phase !== SessionPhase.PLAYER_TURN)
      throw new Error("Not your turn")

    switch (action) {
      case BlackjackAction.HIT:
        this.hit(ws, session)
        break
      case BlackjackAction.STAND:
        this.stand(ws, session)
        break
      case BlackjackAction.DOUBLE_DOWN:
        this.doubleDown(ws, session)
        break
      case BlackjackAction.SPLIT:
        this.split(ws, session)
        break
      case BlackjackAction.INSURANCE:
        this.insurance(ws, session)
        break
    }
  }

  // --- Player actions ---

  private hit(ws: WebSocket, session: BlackjackSession) {
    const hand = this.getActiveHand(session)
    if (hand.status !== HandStatus.PLAYING)
      throw new Error("Cannot hit on this hand")

    hand.cards.push(this.drawCard())
    const value = BlackjackGame.handValue(hand.cards)

    if (value > 21) {
      hand.status = HandStatus.BUSTED
      this.advanceHand(ws, session)
    } else if (value === 21) {
      hand.status = HandStatus.STOOD
      this.advanceHand(ws, session)
    } else {
      this.sendGameState(ws, session)
    }
  }

  private stand(ws: WebSocket, session: BlackjackSession) {
    const hand = this.getActiveHand(session)
    if (hand.status !== HandStatus.PLAYING)
      throw new Error("Cannot stand on this hand")

    hand.status = HandStatus.STOOD
    this.advanceHand(ws, session)
  }

  private doubleDown(ws: WebSocket, session: BlackjackSession) {
    const hand = this.getActiveHand(session)
    if (hand.status !== HandStatus.PLAYING)
      throw new Error("Cannot double down on this hand")
    if (hand.cards.length !== 2)
      throw new Error("Can only double down on initial hand")

    hand.bet *= 2
    hand.isDoubled = true
    hand.cards.push(this.drawCard())

    const value = BlackjackGame.handValue(hand.cards)
    hand.status = value > 21 ? HandStatus.BUSTED : HandStatus.STOOD

    this.advanceHand(ws, session)
  }

  private split(ws: WebSocket, session: BlackjackSession) {
    const hand = this.getActiveHand(session)
    if (hand.status !== HandStatus.PLAYING)
      throw new Error("Cannot split this hand")
    if (hand.cards.length !== 2) throw new Error("Can only split initial hand")

    const cardValueOf = (card: Card) =>
      ["K", "Q", "J"].includes(card.rank)
        ? 10
        : card.rank === "A"
          ? 11
          : parseInt(card.rank)

    const firstCard = hand.cards[0]
    const secondCardFromHand = hand.cards[1]
    if (!firstCard || !secondCardFromHand)
      throw new Error("Hand must have 2 cards")

    if (cardValueOf(firstCard) !== cardValueOf(secondCardFromHand))
      throw new Error("Can only split cards of equal value")

    hand.cards.pop()
    hand.cards.push(this.drawCard())

    const newHand: Hand = {
      cards: [secondCardFromHand, this.drawCard()],
      bet: hand.bet,
      status: HandStatus.PLAYING,
      isDoubled: false,
    }

    session.hands.splice(session.activeHandIndex + 1, 0, newHand)

    // Check if current hand hit 21
    if (BlackjackGame.handValue(hand.cards) === 21) {
      hand.status = HandStatus.STOOD
      this.advanceHand(ws, session)
    } else {
      this.sendGameState(ws, session)
    }
  }

  private insurance(ws: WebSocket, session: BlackjackSession) {
    if (session.insuranceBet > 0)
      throw new Error("Insurance already placed")

    const dealerUpCard = session.dealerHand[0]
    if (!dealerUpCard || dealerUpCard.rank !== "A")
      throw new Error("Insurance only available when dealer shows an Ace")

    const firstHand = session.hands[0]
    if (!firstHand) throw new Error("No hand to insure")
    session.insuranceBet = Math.floor(firstHand.bet / 2)

    this.sendGameState(ws, session)
  }

  // --- Game progression ---

  private advanceHand(ws: WebSocket, session: BlackjackSession) {
    const nextIndex = session.activeHandIndex + 1

    if (nextIndex < session.hands.length) {
      session.activeHandIndex = nextIndex
      const nextHand = session.hands[nextIndex]
      if (!nextHand) throw new Error("Invalid hand index")

      // Check if next hand also needs to auto-resolve (e.g. 21 from split)
      if (BlackjackGame.handValue(nextHand.cards) === 21) {
        nextHand.status = HandStatus.STOOD
        this.advanceHand(ws, session)
      } else {
        this.sendGameState(ws, session)
      }
    } else {
      this.dealerPlay(session)
      this.resolveGame(ws, session)
    }
  }

  private dealerPlay(session: BlackjackSession) {
    session.phase = SessionPhase.DEALER_TURN

    // Only play if at least one hand is not busted
    const hasActiveHand = session.hands.some(
      (h) => h.status !== HandStatus.BUSTED,
    )
    if (!hasActiveHand) return

    while (BlackjackGame.handValue(session.dealerHand) < 17) {
      session.dealerHand.push(this.drawCard())
    }
  }

  private resolveGame(ws: WebSocket, session: BlackjackSession) {
    session.phase = SessionPhase.RESOLVED
    const result = calculateBlackjackGains(session)

    this.message(ws, {
      type: "BET_RESULT",
      payload: {
        gains: result.totalGains,
        hands: result.handResults,
        dealerHand: session.dealerHand,
        dealerValue: BlackjackGame.handValue(session.dealerHand),
        insuranceGain: result.insuranceGain,
      },
    })

    // Reset session to waiting
    session.phase = SessionPhase.WAITING
  }

  // --- Communication ---

  private sendGameState(ws: WebSocket, session: BlackjackSession) {
    const dealerUpCard = session.dealerHand[0]
    if (!dealerUpCard) throw new Error("Dealer has no cards")

    const firstHand = session.hands[0]
    const canInsure =
      session.insuranceBet === 0 &&
      dealerUpCard.rank === "A" &&
      firstHand !== undefined &&
      firstHand.cards.length === 2 &&
      session.activeHandIndex === 0

    this.message(ws, {
      type: "GAME_STATE",
      payload: {
        hands: session.hands.map((hand) => ({
          cards: hand.cards,
          bet: hand.bet,
          status: hand.status,
          isDoubled: hand.isDoubled,
          value: BlackjackGame.handValue(hand.cards),
        })),
        activeHandIndex: session.activeHandIndex,
        dealerUpCard,
        phase: session.phase,
        insuranceBet: session.insuranceBet,
        canInsure,
      },
    })
  }

  // --- Cleanup ---

  removeSession(ws: WebSocket) {
    this.sessions.delete(ws)
  }
}
