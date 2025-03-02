#!/bin/bash

# Funktion zur Anzeige von Fortschrittsmeldungen
print_message() {
    echo -e "\033[0;36m$1\033[0m"
}

# Funktion zum Herunterladen und Installieren von zip und unzip unter Windows
install_zip_unzip() {
    print_message "Installiere zip und unzip..."

    # Erstelle temporäres Verzeichnis
    temp_dir=$(mktemp -d)
    cd "$temp_dir"

    # Lade zip und unzip herunter
    curl -LO "https://sourceforge.net/projects/gnuwin32/files/zip/3.0/zip-3.0-bin.zip"
    curl -LO "https://sourceforge.net/projects/gnuwin32/files/unzip/5.51-1/unzip-5.51-1-bin.zip"

    # Entpacke die heruntergeladenen Dateien
    unzip -q zip-3.0-bin.zip
    unzip -q unzip-5.51-1-bin.zip

    # Kopiere die Executables in das Git-Verzeichnis
    cp bin/zip.exe /c/Program\ Files/Git/mingw64/bin/
    cp bin/unzip.exe /c/Program\ Files/Git/mingw64/bin/

    # Aufräumen
    cd -
    rm -rf "$temp_dir"

    print_message "zip und unzip wurden erfolgreich installiert."
}

# Funktion zum Überprüfen von zip und unzip unter Linux/Mac
check_zip_unzip() {
    if ! command -v zip &> /dev/null || ! command -v unzip &> /dev/null; then
        print_message "Fehler: zip und/oder unzip sind nicht installiert."
        print_message "Bitte installieren Sie diese Programme und versuchen Sie es erneut."
        exit 1
    fi
}

START_DIR=${START_DIR:-$(pwd)}

# Wechsle in das Startverzeichnis
cd "$START_DIR"
echo "Arbeitsverzeichnis geändert zu: $(pwd)"

# Prüfe das Betriebssystem
if [[ "$OSTYPE" == "msys"* || "$OSTYPE" == "cygwin"* ]]; then
    # Windows-spezifischer Code
    if ! command -v zip &> /dev/null || ! command -v unzip &> /dev/null; then
        install_zip_unzip
    fi
else
    # Linux/Mac-spezifischer Code
    check_zip_unzip
fi

# Prüfen, ob SDKMAN bereits installiert ist
if [ -d "$HOME/.sdkman" ]; then
    print_message "SDKMAN ist bereits installiert. Prüfe auf Updates..."
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk selfupdate
else
    print_message "SDKMAN wird installiert..."
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    print_message "SDKMAN wurde erfolgreich installiert."
fi

sdk env install

# Hole die Java-Version aus der .sdkmanrc-Datei
java_version=$(grep "java=" .sdkmanrc | cut -d'=' -f2)

# Aktiviere die Version mit sdk use
if [ -n "$java_version" ]; then
  sdk use java "$java_version"
else
  echo "Keine Java-Version in .sdkmanrc gefunden."
fi

# Hole die Maven-Version aus der .sdkmanrc-Datei
maven_version=$(grep "maven=" .sdkmanrc | cut -d'=' -f2)

# Aktiviere die Version mit sdk use
if [ -n "$maven_version" ]; then
  sdk use maven "$maven_version"
else
  echo "Keine Maven-Version in .sdkmanrc gefunden."
fi

JAVA_HOME=$HOME/.sdkman/candidates/java/current
echo "export JAVA_HOME=$JAVA_HOME" >> "$HOME/.bashrc"

print_message "Installation/Aktualisierung abgeschlossen. Bitte starten Sie Ihre Shell-Sitzung neu, um die Änderungen zu übernehmen."
