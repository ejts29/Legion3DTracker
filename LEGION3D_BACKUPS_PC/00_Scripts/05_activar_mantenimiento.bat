@echo off
color 2F
title LEGION 3D - BOTON DE ENCENDIDO (REACTIVADOR)

echo ===============================================================
echo      [+] INICIANDO MOTORES: AUTOMATIZACION LEGION 3D [+]
echo ===============================================================
echo.
echo Este programa volvera a encender los robots de mantenimiento.
echo Se programara en Windows para ejecutarse TODOS LOS DIAS a las 13:00 horas.
echo.

:: Ruta exacta de tu Orquestador Maestro
set "SCRIPT_MAESTRO=C:\LEGION3D_BACKUPS_PC\00_Scripts\00_ejecutar_mantenimiento_legion3d.bat"

:: Validación de seguridad: Comprueba que el archivo maestro exista antes de programarlo
if not exist "%SCRIPT_MAESTRO%" (
    echo [ERROR CRITICO] No se encuentra el Orquestador Maestro en:
    echo %SCRIPT_MAESTRO%
    echo El sistema no puede encenderse si falta el archivo principal.
    echo.
    pause
    exit /b 1
)

echo [X] Configurando Programador de Tareas de Windows...
:: El comando schtasks crea la tarea. 
:: /TN = Nombre de la tarea
:: /TR = Ruta del archivo a ejecutar
:: /SC DAILY = Frecuencia diaria
:: /ST 13:00 = Hora de ejecucion (1 PM)
:: /F = Fuerza la sobreescritura si la tarea ya existia
schtasks /Create /TN "Legion3D_Mantenimiento" /TR "\"%SCRIPT_MAESTRO%\"" /SC DAILY /ST 13:00 /F >nul 2>&1

:: Verificamos si Windows acepto la orden
if errorlevel 1 (
    echo.
    echo [ERROR] Windows bloqueo la configuracion por falta de permisos.
    echo Por favor, haz clic derecho sobre este archivo y selecciona:
    echo "Ejecutar como administrador".
) else (
    echo.
    echo [OK] EXITOSAMENTE REACTIVADO.
    echo Todo el ecosistema de Legion 3D (Descargas, Limpieza y Boveda)
    echo volvera a operar de forma automatica y silenciosa.
)

echo.
pause
exit /b 0