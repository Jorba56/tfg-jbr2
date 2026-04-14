export const AppState = {
    currentUser: null,
    game: {
        currentPhrase: null,
        phraseBuffer: [],
        score: 0,
        timeLeft: 60,
        timerInterval: null,
        gameHistory: [],
        isFetchingPhrases: false,
        modoPersonalizado: false,
        temaElegido: ""
    },
    powerUps: {
        tiempoCongelado: false,
        multiplicadorActivo: 1,
        inventario: { freeze: 0, multiplier: 0 },
        escudoActivo: false,
        tieneRebufo: false,
        tieneRadar: false,
        tieneSegundaOportunidad: false,
        comboAcertadas: 0,
        chanceUsada: false
    }
};