@echo off
setlocal
cd /d "%~dp0"

if not exist "caja-0.0.1-SNAPSHOT.jar" (
    echo No se encontro caja-0.0.1-SNAPSHOT.jar en esta carpeta.
    goto error
)

where java >nul 2>nul
if errorlevel 1 (
    echo No se encontro Java. Instale un entorno Java 17 compatible.
    goto error
)

if "%~1"=="" (
    echo Iniciando Caja. Mantenga esta ventana abierta mientras usa la aplicacion.
    echo Direccion: http://localhost:8080
)
java -jar "caja-0.0.1-SNAPSHOT.jar" %*
if errorlevel 1 goto error
exit /b 0

:error
echo No se pudo completar la operacion.
pause
exit /b 1