# MultiLaunch4

App Android para elegir 4 aplicaciones y abrirlas a la vez, cada una en un
cuadrante de la pantalla, usando permisos de Shizuku.

## Cómo compilarla

1. Sube esta carpeta a un repositorio de GitHub (rama `main`).
2. Ve a la pestaña **Actions** del repo — el workflow `Build APK` corre solo
   con cada push, o puedes lanzarlo a mano ("Run workflow").
3. Al terminar, descarga el artefacto `MultiLaunch4-debug`, que contiene el
   `.apk`. Instálalo en el móvil (activa "orígenes desconocidos" si hace falta).

## Uso

1. Abre Shizuku e inicia el servicio (por ADB inalámbrico o root, según tengas).
2. Abre MultiLaunch4, toca cada uno de los 4 botones y elige la app para ese
   cuadrante.
3. Toca **"Lanzar las 4 apps"**. La primera vez te pedirá el permiso de
   Shizuku — acéptalo y vuelve a tocar el botón.

## Cómo funciona por dentro

- Usa un **UserService de Shizuku** (no el método `newProcess`, que está
  descontinuado) para ejecutar comandos de shell con privilegios de ADB/root.
- Para cada app: la abre en modo *freeform* (`am start --windowingMode 5`) y
  luego intenta reposicionarla en su cuadrante con `am task resize`.

## Limitación importante (léela)

`am task resize` y el modo *freeform* forzado dependen mucho del fabricante
y de la versión de Android. En algunos móviles (sobre todo con soporte de
modo escritorio/DeX, o tablets) funciona y las 4 apps quedan encajadas solas.
En otros, las 4 apps se abrirán igualmente como ventanas flotantes, pero
puede que tengas que arrastrarlas/redimensionarlas a mano la primera vez.
Eso ya cumple el objetivo de "tener las 4 abiertas a la vez", solo que el
encaje automático no está garantizado en todos los dispositivos.

Si tu móvil no soporta freeform en absoluto, las apps se abrirán en pantalla
completa una tras otra en vez de en cuadrantes.
