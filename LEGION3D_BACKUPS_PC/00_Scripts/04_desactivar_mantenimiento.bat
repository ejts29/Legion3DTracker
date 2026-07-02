@echo off
color 4F
title LEGION 3D - BOTON DE PANICO (DESACTIVADOR)

echo ===============================================================
echo     [!] ALERTA: MODO DE EMERGENCIA ACTIVADO (KILL SWITCH) [!]
echo ===============================================================
echo.
echo ATENCION: Estas a punto de apagar los robots de mantenimiento de Legion 3D.
echo Si procedes con esta accion:
echo  1. Las descargas ya no se ordenaran automaticamente.
echo  2. La computadora dejara de borrar archivos viejos (riesgo de disco lleno).
echo  3. La Boveda de Historial dejara de respaldarse.
echo.

:: VALIDACIÓN DE SEGURIDAD (Confirmación estricta)
set /p "CONFIRMACION=Escribe la palabra 'ACEPTO' (sin comillas) para apagar el sistema, o presiona ENTER para cancelar: "

if /I "%CONFIRMACION%"=="ACEPTO" (
    echo.
    echo [X] INICIANDO APAGADO DE MOTORES...
    
    :: Esto elimina la tarea programada de Windows que crearemos al hacer el despliegue
    schtasks /Delete /TN "Legion3D_Mantenimiento" /F >nul 2>&1
    
    echo.
    echo [OK] El sistema de automatizacion ha sido APAGADO.
    echo Para volver a encenderlo, deberas configurar la Tarea de Windows nuevamente.
) else (
    echo.
    echo [-] Abortado. El sistema de automatizacion sigue funcionando con normalidad.
)

echo.
pause
exit /b 0