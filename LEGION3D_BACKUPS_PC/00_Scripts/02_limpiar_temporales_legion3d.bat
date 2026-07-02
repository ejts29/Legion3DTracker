@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM ============================================================
REM LEGION 3D TRACKER - EXTERMINADOR DE ARCHIVOS OBSOLETOS
REM Regla de negocio: Todo archivo fisico se borra a los 90 dias
REM ============================================================
color 0C
title Legion 3D - Robot Exterminador (90 Dias)

set "ROOT=C:\LEGION3D_BACKUPS_PC"
set "LOG_DIR=%ROOT%\00_Scripts\logs"

REM Obtenemos la fecha exacta usando PowerShell (Metodo infalible)
for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd"') do set "HOY=%%A"

REM Creamos la carpeta de bitacoras (Logs) si no existe
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
set "LOG=%LOG_DIR%\limpieza_%HOY%.log"

echo ============================================================ >> "%LOG%"
echo LIMPIEZA INICIADA: %DATE% %TIME% >> "%LOG%"
echo ============================================================ >> "%LOG%"

REM === EJECUCION DE LIMPIEZA (90 DIAS PARA TODO) ===
REM Al usar la carpeta raiz con la funcion /S, el script entra a todas las subcarpetas automaticamente
call :limpiar "%ROOT%\Archivos_Temporales_PC" 90
call :limpiar "%ROOT%\Envios_Starken_PC" 90
call :limpiar "%ROOT%\Pagos_PC" 90

REM === LIMPIEZA DE CARPETAS VACIAS ===
REM Elimina la cascada de carpetas (Ano/Mes/Dia) que queden vacias tras el borrado
echo. >> "%LOG%"
echo Revisando y eliminando carpetas vacias... >> "%LOG%"
for /f "delims=" %%d in ('dir "%ROOT%" /s /b /ad ^| sort /r') do rd "%%d" 2>nul

echo ============================================================ >> "%LOG%"
echo LIMPIEZA FINALIZADA: %DATE% %TIME% >> "%LOG%"
echo ============================================================ >> "%LOG%"

exit /b 0


REM ============================================================
REM FUNCION: limpiar
REM %1 = carpeta raiz a evaluar
REM %2 = dias de antiguedad limite
REM ============================================================
:limpiar
set "CARPETA=%~1"
set "DIAS=%~2"

echo. >> "%LOG%"
echo Analizando directorio: %CARPETA% >> "%LOG%"
echo Regla activa: Destruir archivos mayores a %DIAS% dias >> "%LOG%"

if not exist "%CARPETA%" (
    echo [OMITIDO] La carpeta no existe actualmente. >> "%LOG%"
    exit /b 0
)

REM Ejecuta el borrado silencioso y escribe en el Log el nombre exacto de lo que destruyo.
REM ESCUDO DE SEGURIDAD APLICADO: Solo borrara archivos que empiecen con "LEG-"
forfiles /P "%CARPETA%" /S /M LEG-* /D -%DIAS% /C "cmd /c echo [ELIMINADO] @path >> \"%LOG%\" & del /q @path" 2>nul

exit /b 0