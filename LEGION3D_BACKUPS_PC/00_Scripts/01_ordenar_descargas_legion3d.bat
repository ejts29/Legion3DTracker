@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM ============================================================
REM LEGION 3D TRACKER - ORDENADOR LOCAL DE DESCARGAS (PULIDO)
REM ============================================================
color 0A
title Legion 3D - Robot Ordenador de Descargas

set "DOWNLOADS=%USERPROFILE%\Downloads"
set "ROOT=C:\LEGION3D_BACKUPS_PC"
set "LOG_DIR=%ROOT%\00_Scripts\logs"

echo ============================================================
echo   LEGION 3D - ROBOT ORDENADOR DE DESCARGAS
echo ============================================================
echo Escaneando la carpeta de Descargas de Luis...
echo.

REM --- 1. EXTRACCION SEGURA DE FECHA Y HORA ---
REM Utilizamos PowerShell porque es inmune a los cambios de idioma o formato regional de Windows.
for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format yyyy"') do set "YYYY=%%A"
for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format MM"') do set "MM=%%A"
for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format dd"') do set "DD=%%A"
for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "STAMP=%%A"

REM --- 2. PREPARACION DE BITACORA (LOGS) ---
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
set "LOG=%LOG_DIR%\orden_descargas_%YYYY%-%MM%-%DD%.log"

echo ============================================================ >> "%LOG%"
echo EJECUCION: %DATE% %TIME% >> "%LOG%"
echo ============================================================ >> "%LOG%"

REM Validamos que la carpeta de descargas de Windows realmente exista.
if not exist "%DOWNLOADS%" (
    echo [ERROR] No existe la carpeta de descargas: %DOWNLOADS%
    echo ERROR: No existe la carpeta de descargas: %DOWNLOADS% >> "%LOG%"
    timeout /t 5 >nul
    exit /b 1
)

REM --- 3. PROCESAMIENTO MODULAR POR TIPO DE ARCHIVO ---
REM Llamamos a la funcion inferior pasandole 2 datos: 
REM 1) La etiqueta a buscar (Ej: -pago-)
REM 2) La ruta exacta donde debe guardarse.
call :procesar "-pago-"  "%ROOT%\Pagos_PC\%YYYY%\%MM%\%DD%"
call :procesar "-envio-" "%ROOT%\Envios_Starken_PC\%YYYY%\%MM%\%DD%"
call :procesar "-tec-"   "%ROOT%\Archivos_Temporales_PC\F_Tecnicos_Temporales_Finales_PC\%YYYY%\%MM%\%DD%"
call :procesar "-wps-"   "%ROOT%\Archivos_Temporales_PC\WPS_Temporales_Inicial_PC\%YYYY%\%MM%\%DD%"
call :procesar "-links-" "%ROOT%\Archivos_Temporales_PC\F_Tecnicos_Temporales_Links_Url_PC\%YYYY%\%MM%\%DD%"

echo.
echo -----------------------------------------------
echo   [OK] ORDENAMIENTO COMPLETADO EXITOSAMENTE
echo -----------------------------------------------
echo Operaciones guardadas en el log.
timeout /t 5 >nul
exit /b 0


REM ----------------------------------------------
REM MOTOR DE PROCESAMIENTO (FUNCION: procesar)
REM -------------------------------------------
:procesar
set "TOKEN=%~1"
set "DEST=%~2"

REM Primero verificamos SI HAY ARCHIVOS para no crear carpetas vacias inutiles
dir /b /a-d "%DOWNLOADS%\LEG-*%TOKEN%*" >nul 2>&1
if errorlevel 1 (
    REM Si no hay archivos con ese token, salimos de la funcion en silencio
    exit /b 0
)

REM Si llegamos aqui, es porque SI hay archivos. Creamos la carpeta del dia.
if not exist "%DEST%" mkdir "%DEST%"

echo. >> "%LOG%"
echo Moviendo archivos con etiqueta: %TOKEN% >> "%LOG%"

REM Hacemos un bucle para mover cada archivo uno por uno
for /f "delims=" %%F in ('dir /b /a-d "%DOWNLOADS%\LEG-*%TOKEN%*" 2^>nul') do (
    set "SRC=%DOWNLOADS%\%%F"
    set "TARGET=%DEST%\%%F"

    REM SISTEMA ANTICOLISIÓN: Si Luis descarga el mismo archivo 2 veces, 
    REM el sistema le agrega la hora exacta al nombre para no sobrescribir el anterior.
    if exist "!TARGET!" (
        set "TARGET=%DEST%\%%~nF_%STAMP%%%~xF"
    )

    echo Moviendo: %%F
    echo Moviendo: "!SRC!" -> "!TARGET!" >> "%LOG%"

    REM Ejecutamos el movimiento real
    move /Y "!SRC!" "!TARGET!" >nul

    if errorlevel 1 (
        echo   [ERROR] %%F
        echo ERROR moviendo archivo: %%F >> "%LOG%"
    ) else (
        echo   [EXITO] %%F
        echo OK: %%F >> "%LOG%"
    )
)
exit /b 0