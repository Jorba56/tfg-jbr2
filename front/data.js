/**
 * CLASE MODELO DE DATOS - FRASES
 */
export class FraseJuego {
    constructor(id, texto, dificultad) {
        this.id = id;
        this.texto = texto;
        this.dificultad = dificultad;
    }
}

/**
 * CLASE USUARIO (Nueva)
 */
export class Usuario {
    constructor(username, password, creditos = 0, isAdmin = false) {
        this.username = username;
        this.password = password;
        this.creditos = creditos;
        this.isAdmin = isAdmin;
        this.inventario = []; // IDs de productos comprados
        this.colorTema = null; // Personalización equipada
    }
}

/**
 * CATÁLOGO DE COSMÉTICOS (Nuevo)
 * Tipo: 'tema', 'efecto', etc.
 */
export const catalogoCosmeticos = [
    { id: 'c1', nombre: "Tema Fuego", tipo: 'tema', precio: 500, cssClass: 'theme-fire', desc: "Fondo animado de fuego" },
    { id: 'c2', nombre: "Tema Matrix", tipo: 'tema', precio: 800, cssClass: 'theme-matrix', desc: "Lluvia de código verde" },
    { id: 'c3', nombre: "Borde Dorado", tipo: 'borde', precio: 200, cssClass: 'border-gold', desc: "Marco brillante para el juego" },
    { id: 'c4', nombre: "Modo Hacker", tipo: 'tema', precio: 1000, cssClass: 'theme-hacker', desc: "Estilo terminal retro" }
];

/**
 * REPOSITORIO DE FRASES
 */
export const frasesRepository = [
    new FraseJuego(1, "El veloz murciélago hindú comía feliz cardillo y kiwi.", "Fácil"),
    new FraseJuego(2, "La cigüeña tocaba el saxofón detrás del palenque de paja.", "Media"),
    new FraseJuego(3, "Programar en Java requiere paciencia y mucha cafeína.", "Fácil"),
    new FraseJuego(4, "La inteligencia artificial transformará el desarrollo web.", "Media"),
    new FraseJuego(5, "FastFingers dominará el mercado de los juegos de mecanografía.", "Difícil")
];

/**
 * BASE DE DATOS DE USUARIOS (Simulada)
 */
export const usuariosRepository = [
    new Usuario("admin", "1234", 99999, true), // Admin con muchos créditos
    new Usuario("alumno", "agora", 50, false)  // Usuario normal
];