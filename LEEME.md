# Cósmica Coffee

App Android para manejar una cafetería al paso entre varias personas. Cada uno entra con su correo y
contraseña, y todos ven los mismos datos al instante (se guardan en Firebase, de Google).
Si se corta internet se puede seguir vendiendo: todo se sincroniza solo cuando vuelve la conexión.
Para entrar la primera vez en un teléfono sí hace falta internet.

## Qué hace

- **Vender**: tocás los productos para armar el pedido, elegís efectivo, tarjeta o transferencia y tocás *Cobrar*.
- **Ventas**: lo vendido hoy y en el mes, con el detalle de cada venta y quién la hizo.
- **Gastos**: compras de insumos, alquiler, servicios, sueldos, etc., con quién lo cargó.
- **Menú**: agregás, editás o quitás productos con su precio y su costo. Muestra la ganancia y el margen de cada uno.
- **Balance**: por mes, ventas, costo de lo vendido, ganancia bruta, gastos y **ganancia neta**; además lo cobrado por
  medio de pago, lo vendido por cada persona, gastos por categoría y los productos más vendidos.

- **Pasaporte de sellos** (para clientes): ver abajo.

Cada venta guarda el precio y el costo del momento, así si después cambiás un precio el balance de meses anteriores no cambia.

## Configurar Firebase (una sola vez, gratis)

1. Entrá a <https://console.firebase.google.com> con tu cuenta de Google y tocá **Crear un proyecto**
   (nombre: *Cosmica Cafe*; Google Analytics no hace falta).
2. En la página del proyecto tocá el ícono de **Android** para agregar una app.
   En *Nombre del paquete* poné exactamente `com.cosmica.cafeteria`. Seguí y **descargá `google-services.json`**.
   Ese archivo es el que conecta la app con tu proyecto.
3. Menú **Authentication** → *Comenzar* → *Correo electrónico/contraseña* → activarlo y guardar.
   En la pestaña **Usuarios** → *Agregar usuario*: cargá el correo y una contraseña para cada uno de ustedes dos.
4. Menú **Firestore Database** → *Crear base de datos* → ubicación cercana (por ejemplo `southamerica-east1`)
   → modo **producción**.
5. En Firestore, pestaña **Reglas**: borrá lo que hay, pegá el contenido de `firestore.rules`, cambiá los dos
   correos de ejemplo por los de ustedes y tocá **Publicar**. Así nadie más puede ver ni tocar los datos.

Para sumar a alguien más: agregalo como usuario (paso 3) y sumá su correo en las reglas (paso 5).

## Pasaporte de sellos para clientes

Los clientes no instalan nada: escanean el QR del mostrador o del vaso y se abre su **pasaporte cósmico** en el
navegador (página `docs/index.html`). Ponen su nombre y les aparece su propio QR y un código de 6 letras.
Cada café es un planeta sellado: **con 6 sellos, el 7mo café es gratis**.

Al cobrar en *Vender*, en la hoja del pedido tocá **Escanear** y apuntá al QR del cliente (o escribí su código).
La app sugiere un sello por cada café del pedido (productos de una categoría que diga "Café"); se puede cambiar
con − y +. Cuando el cliente tiene 6 sellos aparece **Canjear café gratis**: descuenta del total el café más caro
del pedido y le resta 6 sellos. La venta queda anotada con el nombre del cliente.

Puesta en marcha (una sola vez):

1. Firebase → **Authentication** → *Método de acceso*: activar **Anónimo** (cada cliente arranca sin crear cuenta)
   y **Google** (para guardar los sellos en su cuenta). En *Configuración* → *Dominios autorizados*, agregar
   `walybrollo.github.io`.
2. Firebase → **Firestore** → *Reglas*: pegar de nuevo `firestore.rules` y **Publicar** (suma la parte de `clientes`).
3. GitHub → *Settings* → **Pages** → *Branch*: `main`, carpeta `/docs` → *Save*. En un par de minutos el pasaporte
   queda en <https://walybrollo.github.io/cosmica-cafe/>.
4. Abrir <https://walybrollo.github.io/cosmica-cafe/qr.html> e **imprimir**: hoja 1, cartel para el mostrador;
   hoja 2, 12 stickers para los vasos.

Si el pasaporte dice que no se pudo conectar, en Firebase andá a *Configuración del proyecto* → *Tus apps* →
*Agregar app* → **Web**, y copiá esos datos en `docs/config.js`.
Para ver cómo se ve sin conexión: `index.html?demo=4` (el número es la cantidad de sellos).

**App para el cliente:** la misma página se instala como app (ícono propio, se abre sin barra del navegador). En
Android aparece el botón *Instalar la app*; en iPhone, *Compartir → Agregar a inicio*.
Con **Guardar mis sellos con Google** el pasaporte queda en su cuenta: si cambia de teléfono o borra el navegador,
toca *Ya tengo pasaporte: entrar con Google* y recupera sus sellos. Sin eso, el pasaporte vive solo en ese navegador.

## Cómo obtener el APK

El proyecto está preparado para que GitHub arme el APK solo cada vez que se sube un cambio
(`.github/workflows/apk.yml`). El APK aparece en la pestaña **Actions** del repositorio, dentro de la última
ejecución, como `cosmica-cafe-apk`. Necesita el archivo `app/google-services.json` en el repositorio
(o guardado como secreto `GOOGLE_SERVICES_JSON`).

También se puede compilar con Android Studio (*File > Open* y *Build > Build APK(s)*), con
`google-services.json` copiado dentro de la carpeta `app/`.

Para instalarlo, pasá el APK al teléfono y abrilo (Android pide permitir "instalar apps desconocidas").

## Técnico

Kotlin, Jetpack Compose (Material 3), Firebase Authentication y Cloud Firestore. minSdk 26 (Android 8.0), targetSdk 35.
Código en `app/src/main/java/com/cosmica/cafeteria/`: `data/` (Firestore y sesión), `CafeViewModel.kt`
(lógica y balance), `ui/screens/` (pantallas). Colecciones de Firestore: `productos`, `ventas`, `gastos`, `cierres`, `insumos`, `movimientos` y `clientes`
(pasaporte de sellos). La página de clientes está en `docs/` (HTML + Firebase web, publicada con GitHub Pages).
