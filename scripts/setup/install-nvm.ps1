# Funktion zum Anzeigen von Fortschrittsmeldungen
function Write-ProgressMessage {
    param([string]$Message)
    Write-Host $Message -ForegroundColor Cyan
}

# Funktion zum Abrufen der neuesten NVM-Version
function Get-LatestNvmVersion {
    $releases = Invoke-RestMethod -Uri "https://api.github.com/repos/coreybutler/nvm-windows/releases/latest"
    return $releases.tag_name
}

# Aktuellen NVM-Installationspfad ermitteln
$nvmPath = "$env:APPDATA\nvm"

# Neueste NVM-Version abrufen
$latestVersion = Get-LatestNvmVersion

# Überprüfen der aktuell installierten Version
$currentVersion = if (Test-Path "$nvmPath\nvm.exe") {
    & "$nvmPath\nvm.exe" version
} else {
    "0.0.0"
}

if ($currentVersion -eq $latestVersion) {
    Write-ProgressMessage "Die neueste NVM-Version ($latestVersion) ist bereits installiert."
} else {
    Write-ProgressMessage "Neue NVM-Version verfügbar. Aktualisiere von $currentVersion auf $latestVersion..."

    # URL für den NVM-Installer
    $nvmUrl = "https://github.com/coreybutler/nvm-windows/releases/download/$latestVersion/nvm-setup.exe"

    # Temporärer Pfad für den Installer
    $installerPath = "$env:TEMP\nvm-setup.exe"

    # NVM-Installer herunterladen
    Write-ProgressMessage "Lade NVM-Installer herunter..."
    Invoke-WebRequest -Uri $nvmUrl -OutFile $installerPath

    # Installer ausführen
    Write-ProgressMessage "Führe NVM-Installer aus..."
    Start-Process -FilePath $installerPath -ArgumentList "/SILENT /NORESTART" -Wait

    # Temporären Installer löschen
    Remove-Item $installerPath

    # Umgebungsvariablen aktualisieren
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

    Write-ProgressMessage "NVM wurde erfolgreich auf Version $latestVersion aktualisiert."
}

Write-ProgressMessage "Bitte starten Sie Ihre PowerShell-Sitzung neu, um die Änderungen zu übernehmen."

# Pfad zum Root-Ordner (wo .nvmrc liegt)
$rootPath = Get-Location

# .nvmrc Datei auslesen und Node.js-Version installieren
if (Test-Path (Join-Path $rootPath ".nvmrc")) {
    $nodeVersion = Get-Content (Join-Path $rootPath ".nvmrc") -Raw
    $nodeVersion = $nodeVersion.Trim()
    Write-ProgressMessage "Installiere und aktiviere Node.js Version $nodeVersion aus .nvmrc..."
    & $nvmPath\nvm.exe install $nodeVersion
    & $nvmPath\nvm.exe use $nodeVersion
    Write-ProgressMessage "Node.js Version $nodeVersion wurde installiert und aktiviert."

    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

    Write-ProgressMessage "Aktiviere Corepack..."
    & corepack enable

    # Lese die Yarn-Version aus der package.json
    $packageJsonPath = Join-Path $rootPath "package.json"
    if (Test-Path $packageJsonPath) {
        $packageJson = Get-Content $packageJsonPath -Raw | ConvertFrom-Json
        $yarnVersion = $packageJson.packageManager -replace "yarn@", ""

        Write-ProgressMessage "Installiere Yarn Version $yarnVersion..."
        yarn set version $yarnVersion

        Write-ProgressMessage "Yarn Version $yarnVersion wurde installiert."
    } else {
        Write-ProgressMessage "Keine package.json Datei gefunden. Yarn-Version wurde nicht installiert."
    }
} else {
    Write-ProgressMessage "Keine .nvmrc Datei gefunden. Node.js-Version wurde nicht automatisch installiert."
}

# Warte auf Benutzereingabe vor dem Beenden
Read-Host "Drücken Sie Enter zum Beenden"
