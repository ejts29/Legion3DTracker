@echo off
setlocal
color 0B
title Legion 3D - Mantenimiento General

echo -------------------------------------------
echo   LEGION 3D TRACKER - MANTENIMIENTO LOCAL COMPLETO
echo -----------------------------------------------------
echo Iniciando orquestador de scripts...
echo.

set "SCRIPTS=C:\LEGION3D_BACKUPS_PC\00_Scripts"

echo [1/3] Ordenando descargas del equipo...
call "%SCRIPTS%\01_ordenar_descargas_legion3d.bat"

echo.
echo [2/3] Ejecutando limpieza de 90 dias...
call "%SCRIPTS%\02_limpiar_temporales_legion3d.bat"

echo.
echo [3/3] Respaldando Base de Datos (Boveda de Historial)...
REM Descomentaremos esta linea cuando creemos el script 03
REM call "%SCRIPTS%\03_respaldo_boveda_legion3d.bat"
echo (Configuracion SQL pendiente)

echo.
echo -------------------------------------------------
echo Mantenimiento local terminado con exito.
echo ---------------------------------------------------
timeout /t 5 >nul
exit /b 0