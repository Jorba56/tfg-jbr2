// js/core/state.js

// Exportamos un objeto reactivo que guardará el estado de toda la aplicación
export const app = {
    currentUser: null,

    // Variables del juego de mecanografía
    game: {
        currentPhrase: null,
        phraseBuffer: [],
        score: 0,
        timeLeft: 60,
        timerInterval: null,
        gameHistory: [],
        isFetchingPhrases: false,
        modoPersonalizado: false,
        temaElegido: "",
    },

    // Variables de Power-Ups y Habilidades
    powerUps: {
        inventario: { freeze: 0, multiplier: 0 },
        tiempoCongelado: false,
        multiplicadorActivo: 1,
        escudoActivo: false,
        tieneRebufo: false,
        tieneRadar: false,
        tieneSegundaOportunidad: false,
        comboAcertadas: 0,
        chanceUsada: false
    }
};