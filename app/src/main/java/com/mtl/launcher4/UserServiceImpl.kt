package com.mtl.launcher4

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Este objeto corre en un proceso separado con privilegios de shell (ADB) u
 * opcionalmente root, otorgados por Shizuku. No es un proceso de Android
 * normal, así que solo debe ejecutar comandos de shell, sin depender de
 * Context ni de otras APIs de Android.
 */
class UserServiceImpl : IUserService.Stub() {

    override fun exec(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor()
            output.trim()
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    override fun destroy() {
        System.exit(0)
    }
}
