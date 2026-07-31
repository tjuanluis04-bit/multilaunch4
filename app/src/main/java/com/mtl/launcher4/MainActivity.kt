package com.mtl.launcher4

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private var userService: IUserService? = null
    private var freeformEnabled = false

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var slotButtons: List<Button>

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            userService = IUserService.Stub.asInterface(binder)
            log("Servicio Shizuku conectado.")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            log("Servicio Shizuku desconectado.")
        }
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                statusText.text = "Shizuku: permiso concedido."
                bindService()
            } else {
                log("Permiso de Shizuku denegado.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)

        slotButtons = listOf(
            findViewById(R.id.slot0Button),
            findViewById(R.id.slot1Button),
            findViewById(R.id.slot2Button),
            findViewById(R.id.slot3Button)
        )

        slotButtons.forEachIndexed { index, button ->
            updateSlotLabel(index)
            button.setOnClickListener {
                AppPickerDialog.show(this) { app ->
                    Prefs.save(this, index, app)
                    updateSlotLabel(index)
                }
            }
        }

        findViewById<Button>(R.id.launchButton).setOnClickListener {
            if (ensureShizukuReady()) launchAllApps()
        }

        findViewById<Button>(R.id.diagButton).setOnClickListener {
            if (ensureShizukuReady()) runDiagnostic()
        }

        Shizuku.addRequestPermissionResultListener(permissionListener)
        checkShizukuStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        try {
            Shizuku.unbindUserService(buildUserServiceArgs(), serviceConnection, true)
        } catch (_: Exception) {
        }
    }

    private fun updateSlotLabel(index: Int) {
        val app = Prefs.load(this, index)
        val prefix = listOf("Arriba-izquierda", "Arriba-derecha", "Abajo-izquierda", "Abajo-derecha")[index]
        slotButtons[index].text = "$prefix: ${app?.label ?: "(elegir app)"}"
    }

    private fun checkShizukuStatus() {
        if (!Shizuku.pingBinder()) {
            statusText.text = "Shizuku: no está corriendo. Abre la app Shizuku e inicia el servicio."
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            statusText.text = "Shizuku: permiso concedido."
            bindService()
        } else {
            statusText.text = "Shizuku: falta permiso. Pulsa 'Lanzar las 4 apps' para solicitarlo."
        }
    }

    private fun buildUserServiceArgs() = Shizuku.UserServiceArgs(
        ComponentName(packageName, UserServiceImpl::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(false)
        .version(1)

    private fun bindService() {
        if (userService != null) return
        try {
            Shizuku.bindUserService(buildUserServiceArgs(), serviceConnection)
        } catch (e: Exception) {
            log("Error al vincular servicio: ${e.message}")
        }
    }

    /**
     * Comprueba que Shizuku esté corriendo, con permiso concedido y con el
     * servicio ya conectado. Si falta algo, lo soluciona (pide permiso,
     * conecta el servicio) y devuelve false para que quien llama reintente
     * en el siguiente click.
     */
    private fun ensureShizukuReady(): Boolean {
        if (!Shizuku.pingBinder()) {
            log("Shizuku no está corriendo. Ábrelo primero e inicia el servicio (por ADB/wireless debugging).")
            return false
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            return false
        }
        if (userService == null) {
            bindService()
            log("Conectando con el servicio... pulsa el botón de nuevo en un segundo.")
            return false
        }
        return true
    }

    private fun runDiagnostic() {
        val app = Prefs.load(this, 0)
        if (app == null) {
            log("Elige primero una app en el espacio 'Arriba-izquierda' para diagnosticar con ella.")
            return
        }
        val service = userService ?: return
        val component = "${app.packageName}/${app.activityName}"

        log("--- DIAGNÓSTICO con ${app.label} ---")
        thread {
            try {
                val startOut = service.exec("am start -n $component --windowingMode 5")
                runOnUiThread { log("[start] -> $startOut") }

                Thread.sleep(1000)

                val dumpCmd = "dumpsys activity activities | grep -n -i -A 3 -B 3 '${app.packageName}'"
                val dumpOut = service.exec(dumpCmd)
                runOnUiThread {
                    log("--- Salida de dumpsys (busca dónde aparece el ID de tarea) ---")
                    log(dumpOut.ifBlank { "(vacío: no encontró el paquete en el dump; puede que la app no haya arrancado)" })
                    log("--- FIN DIAGNÓSTICO ---")
                }
            } catch (e: Exception) {
                runOnUiThread { log("Error en diagnóstico: ${e.message}") }
            }
        }
    }

    private fun launchAllApps() {
        val apps = (0..3).map { Prefs.load(this, it) }
        if (apps.any { it == null }) {
            log("Elige una app para cada uno de los 4 espacios primero.")
            return
        }

        val dm = resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val bounds = listOf(
            intArrayOf(0, 0, w / 2, h / 2),
            intArrayOf(w / 2, 0, w, h / 2),
            intArrayOf(0, h / 2, w / 2, h),
            intArrayOf(w / 2, h / 2, w, h)
        )

        val service = userService ?: return

        thread {
            try {
                if (!freeformEnabled) {
                    val out = service.exec(
                        "settings put global development_settings_enabled 1; " +
                            "settings put global force_resizable_activities 1; " +
                            "settings put global enable_freeform_support 1"
                    )
                    runOnUiThread { log("Freeform habilitado: $out") }
                    freeformEnabled = true
                }

                apps.forEachIndexed { index, app ->
                    if (app == null) return@forEachIndexed
                    val b = bounds[index]
                    val script = buildScript(app.packageName, app.activityName, b)
                    val out = service.exec(script)
                    runOnUiThread { log("[${app.label}] -> $out") }
                }

                runOnUiThread {
                    log("Listo. Si alguna ventana no quedó en su sitio, arrástrala/redimensiónala a mano (depende del fabricante y versión de Android).")
                }
            } catch (e: Exception) {
                runOnUiThread { log("Error: ${e.message}") }
            }
        }
    }

    /**
     * Lanza la app en modo freeform y, si el dispositivo lo permite, intenta
     * reposicionarla en el cuadrante indicado. "am task resize" es una
     * característica interna que varía según el fabricante y la versión de
     * Android: si falla, la app igualmente queda abierta en una ventana
     * flotante que el usuario puede mover a mano.
     */
    private fun buildScript(pkg: String, act: String, b: IntArray): String {
        val component = "$pkg/$act"
        return "am start -n $component --windowingMode 5\n" +
            "sleep 0.7\n" +
            "tid=\$(dumpsys activity activities | grep 'Task{' | grep '$pkg' | tail -1 | sed -E 's/.*#([0-9]+).*/\\1/')\n" +
            "if [ -n \"\$tid\" ]; then am task resize \$tid ${b[0]} ${b[1]} ${b[2]} ${b[3]}; fi"
    }

    private fun log(msg: String) {
        logText.append("$msg\n")
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 9001
    }
}
