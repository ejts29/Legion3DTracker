@echo off
setlocal EnableExtensions EnableDelayedExpansion
color 0D
title Legion 3D - Boveda de Historial (Respaldo SQL)

echo ============================================================
echo   LEGION 3D - RESPALDO DE BASE DE DATOS (BOVEDA)
echo ============================================================
echo Conectando con el servidor MySQL...
echo.

:: =========================================================================
:: [ZONA CRITICA PARA EL DESPLIEGUE] 
:: NOTA PARA EFRÉN: Modifica estos datos cuando instales el sistema final.
:: =========================================================================
set "DB_USER=root"
set "DB_PASS=root"
set "DB_NAME=dblegion3dtracker"

:: Ruta del ejecutable de MySQL Dump. 
:: Si instalaste MySQL Workbench o XAMPP, debes buscar donde esta este archivo.
:: Por lo general esta en: C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe
set "MYSQL_DUMP=mysqldump" 
:: =========================================================================


set "ROOT=C:\LEGION3D_BACKUPS_PC"
set "LOG_DIR=%ROOT%\00_Scripts\logs"

:: Extraccion de Fecha y Hora exacta usando PowerShell
for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format yyyy"') do set "YYYY=%%A"
for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format MM"') do set "MM=%%A"
for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format dd"') do set "DD=%%A"
for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format HH-mm"') do set "HORA=%%A"

set "DESTINO=%ROOT%\Boveda de Historial_PC\%YYYY%\%MM%\%DD%"
if not exist "%DESTINO%" mkdir "%DESTINO%"

set "ARCHIVO_SQL=%DESTINO%\Boveda_de_Historial_backup_Legion3D_%YYYY%-%MM%-%DD%_%HORA%.sql"
set "LOG=%LOG_DIR%\respaldo_sql_%YYYY%-%MM%-%DD%.log"

echo Intentando exportar esquema: %DB_NAME%...
echo Guardando en: %ARCHIVO_SQL%

:: EJECUCIÓN DEL RESPALDO
:: Genera un archivo de texto gigante con todos los comandos SQL para reconstruir la base de datos
"%MYSQL_DUMP%" -u "%DB_USER%" -p"%DB_PASS%" "%DB_NAME%" > "%ARCHIVO_SQL%" 2>> "%LOG%"

if errorlevel 1 (
    echo.
    echo [ERROR] No se pudo crear el respaldo. Revisa el log o las contraseñas.
    echo ERROR al crear Boveda: %DATE% %TIME% >> "%LOG%"
) else (
    echo.
    echo [EXITO] La Boveda de Historial ha sido blindada y actualizada.
    echo EXITO Boveda creada: %ARCHIVO_SQL% >> "%LOG%"
)

timeout /t 5 >nul
exit /b 0