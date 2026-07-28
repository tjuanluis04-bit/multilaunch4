package com.mtl.launcher4;

interface IUserService {

    // Ejecuta un comando de shell y devuelve stdout + stderr combinados.
    String exec(String command) = 1;

    // Requerido por Shizuku para poder terminar el proceso de forma limpia.
    void destroy() = 16777114;
}
