# Prüfen, ob das Skript als Administrator ausgeführt wird
if (-NOT ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator"))
{
    # Skript neu starten mit erhöhten Rechten
    Start-Process PowerShell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    Exit
}

# Speichern des aktuellen Verzeichnisses (Root-Verzeichnis)
$rootDir = Get-Location

# Ermitteln des Skriptpfads
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition

# PowerShell-Skript ausführen
Write-Host "Führe PowerShell-Skript aus..."
try {
    & "$scriptPath\install-nvm.ps1"
}
catch {
    Write-Host "Fehler beim Ausführen des PowerShell-Skripts: $_"
}

# Shell-Skript über GitBash ausführen
Write-Host "Führe Shell-Skript über GitBash aus..."
try {
    $rootDir = Get-Location
    $gitBashOutput = & "C:\Program Files\Git\bin\bash.exe" -c "START_DIR='$rootDir' '$scriptPath/install-sdkman.sh'"
    $gitBashOutput | ForEach-Object { Write-Host $_ }
}
catch {
    Write-Host "Fehler beim Ausführen des Shell-Skripts: $_"
}

Write-Host "Skriptausführung abgeschlossen. Drücken Sie eine beliebige Taste zum Beenden..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
