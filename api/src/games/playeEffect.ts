export const playeEffect = (gains: number, bet: number): void => {

    const factor = gains / bet
    if (factor < 1) {
        fetch("http://localhost:5701/siren/small", {
            method: "POST"
        });
    }
    else if (factor < 5) {
        fetch("http://localhost:5701/siren/medium", {
            method: "POST"
        });
    }
    else if (factor < 10) {
        fetch("http://localhost:5701/plug/blink")
        fetch("http://localhost:5701/siren/big")
    }
    else {
        fetch("http://localhost:5701/casino/jackpot")
    }
}