// Conexión con el proyecto de Firebase de Cósmica (el mismo que usa la app del local).
// Si el pasaporte muestra un error de conexión, en la consola de Firebase andá a
// Configuración del proyecto > Tus apps > Agregar app > Web (</>), y pegá acá los datos que te da.
export const firebaseConfig = {
  apiKey: "AIzaSyACSRcBaCxXHwdijVKerlIqAY91lHLM98U",
  authDomain: "cosmica-cafe.firebaseapp.com",
  projectId: "cosmica-cafe",
  storageBucket: "cosmica-cafe.firebasestorage.app",
  messagingSenderId: "255888566458",
};

// Cuántos sellos hacen falta para el café gratis (el 7mo va por la casa).
export const SELLOS_PARA_PREMIO = 6;
