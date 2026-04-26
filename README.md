# ⚡ dCore
> **El Framework Definitivo para Servidores Minecraft.** > Potente, asíncrono y diseñado para un rendimiento crítico con personalización absoluta.

[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.18.2%20--%201.20.x-green?style=for-the-badge&logo=minecraft)](https://papermc.io/)
[![JitPack](https://img.shields.io/badge/JitPack-v1.0.0-blue?style=for-the-badge)](https://jitpack.io/)

---
## Información más detallada del plugin:
* **🗄️ dCore es mucho más que un plugin; es un framework de alto rendimiento diseñado para centralizar y simplificar la lógica de tu servidor. A nivel de plugin, integra un motor infinito de economías custom con balances iniciales, sistemas de baltop configurables (en chat o GUI interactiva) y un poderoso constructor de menús dinámicos 100% editables desde YAML, capaz de registrar sus propios comandos de apertura, cobrar accesos con monedas internas, soportar cabezas de HeadDatabase y ejecutar cadenas lógicas de acciones (como dar/quitar dinero, ejecutar comandos de consola/jugador, reproducir sonidos y enviar mensajes enriquecidos con MiniMessage). A nivel de API, expone una arquitectura reactiva basada en CompletableFuture y el patrón Repository, permitiendo a desarrolladores externos interactuar de forma 100% asíncrona y segura con múltiples motores de persistencia (MySQL, MongoDB, H2) sin arriesgar el hilo principal (Tick Thread), además de proveer métodos nativos para manipular balances, formatear monedas al instante y sincronizarse fluidamente con PlaceholderAPI para expandir las posibilidades de cualquier otro complemento.
* 
## 🌟 Características Principales

* **🗄️ Persistencia Multi-DB:** Soporte nativo y 100% asíncrono para **H2, MySQL y MongoDB**. Cero bloqueos en el hilo principal (*Main Thread*).
* **💰 Economías Custom Infinitas:** Crea múltiples monedas desde `config.yml` con iconos únicos, balances iniciales y sistema de Baltop integrado.
* **🖥️ Menús Dinámicos (GUI):** Configuración avanzada en `menus.yml` con soporte para paginación, comandos dinámicos y requisitos de apertura (permisos/costos).
* **⚡ Sistema de Acciones:** Sintaxis simple para ejecutar comandos, enviar mensajes, sonidos y manipular economías directamente desde los items.
* **📦 Carga en Runtime (Libby):** dCore gestiona sus propias dependencias. Descarga e aísla drivers JDBC, MongoDB y HikariCP automáticamente.
* **🛡️ Compatibilidad Total:** Basado en **XSeries** y **MiniMessage**. Funciona desde la 1.18.2 hasta las versiones más recientes de Paper/Spigot.
* **💀 HeadDatabase (HDB):** Integración nativa. Usa `hdb-<id>` y el plugin se encarga del resto con un sistema de fallback seguro.

---

## 🚀 Instalación

1. Descarga el archivo `dCore.jar` desde los lanzamientos.
2. Sube el archivo a la carpeta `/plugins/` de tu servidor.
3. Inicia el servidor para generar los archivos de configuración. 
   > *Nota: Las librerías internas se descargarán automáticamente en el primer inicio.*
4. Configura tus economías, mensajes y menús a tu gusto.
5. ¡Listo! Reinicia o usa el comando de recarga.

---

## 👨‍💻 API para Desarrolladores

Integrar **dCore** es extremadamente sencillo. Todas las operaciones de base de datos devuelven `CompletableFuture`, permitiendo un flujo de trabajo asíncrono y eficiente.

### 📦 Dependencia (Maven)
Añade el repositorio y la dependencia a tu `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>[https://jitpack.io](https://jitpack.io)</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.TuUsuarioGitHub</groupId>
        <artifactId>dCore</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
💻 Ejemplo de UsoRecuerda: Nunca bloquees el hilo principal llamando a .join() o .get().Javapublic void darRecompensa(UUID uuid) {
    String moneda = "gems";
    double cantidad = 50.0;
    
    DCoreAPI.getBalance(uuid.toString(), moneda).thenAccept(balanceActual -> {
        double nuevoBalance = balanceActual + cantidad;
        
        DCoreAPI.setBalance(uuid.toString(), moneda, nuevoBalance).thenRun(() -> {
            String formateado = DCoreAPI.formatBalance(moneda, nuevoBalance);
            getLogger().info("¡Jugador recompensado! Nuevo balance: " + formateado);
        });
    });
}
🪄 Integración con PlaceholderAPISi tienes PlaceholderAPI instalado, dCore registra automáticamente los siguientes placeholders:PlaceholderDescripciónEjemplo de Salida%dCore_balance_<moneda>_formatted%Balance con prefijo configurado.<gold>Coins</gold> 1,250.50%dCore_balance_<moneda>_raw%Valor numérico exacto.1250.5🖱️ Sistema de Acciones (Menús)Configura acciones potentes en tus items de menú con una sintaxis intuitiva: [tipo] datos.SintaxisDescripciónEjemplo[console_cmd]Ejecuta comando desde consola.[console_cmd] give %player% diamond 1[player_cmd]El jugador ejecuta el comando.[player_cmd] spawn[message]Envía mensaje (MiniMessage).[message] <green>¡Premio reclamado![sound]Reproduce un sonido (XSeries).[sound] ENTITY_PLAYER_LEVELUP 1.0 1.0[close]Cierra el menú actual.[close][give_economy]Suma dinero a una economía.[give_economy] coins 100[take_economy]Resta dinero de una economía.[take_economy] gems 50[next_page]Salta a la siguiente página.[next_page]💀 Soporte HeadDatabasePara usar cabezas personalizadas, define el material con el prefijo hdb-.YAML# Ejemplo en menus.yml
item_cabeza:
  material: "hdb-7129"
  name: "<aqua>Cabeza Custom"
  slots: [ 13 ]
  actions:
    - "[message] <gray>¡Hiciste clic en una cabeza!"
Si el ID es inválido o el plugin no está presente, dCore colocará un bloque de BARRIER para evitar errores visuales.
