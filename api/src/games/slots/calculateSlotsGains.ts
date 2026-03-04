export const calculateGains = (
  bet: number,
  result: string[],
  symbols: { value: string; weight: number }[],
): number => {
  if (result.length === 0) return 0

  const counts: { [key: string]: number } = {}
  for (const symbol of result) counts[symbol] = (counts[symbol] || 0) + 1

  let score = 0
  const totalWeight = symbols.reduce((sum, entry) => sum + entry.weight, 0)
  const reels = result.length

  for (const [sym, count] of Object.entries(counts)) {
    const symbolWeight = symbols.find((entry) => entry.value === sym)?.weight || 1
    const probability = symbolWeight / totalWeight

    // 3 fois le meme symbole
    if (count === reels) {
      score += 1 / probability
    }

    // 2 fois le meme symbole
    else if (count === reels - 1) {
      const fractionTwoSame = 0.225
      score += (1 / probability) * fractionTwoSame
    }

    // Sinon si y'a un 7
    else if (count === 1 && sym === "7") {
      score += 1
    }
  }

  return Math.round(bet * score)
}
