export const playeEffect = (gains: number, bet: number): void => {
  const factor = gains / bet
  console.log("jouer sons")
  if (factor < 1) {
    fetch("http://10.109.150.245:5701/siren/small", {
      method: "POST",
    }).catch(() => {})
  } else if (factor < 5) {
    fetch("http://10.109.150.245:5701/siren/medium", {
      method: "POST",
    }).catch(() => {})
  } else if (factor < 10) {
    fetch("http://10.109.150.245:5701/plug/blink", {
      method: "POST",
    }).catch(() => {})
    fetch("http://10.109.150.245:5701/siren/big", {
      method: "POST",
    }).catch(() => {})
  } else {
    fetch("http://10.109.150.245:5701/casino/jackpot", {
      method: "POST",
    }).catch(() => {})
  }
}
