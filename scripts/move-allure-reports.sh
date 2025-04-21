#!/bin/bash

# Zielverzeichnis
MERGED_DIR="target/merged-allure-results"

# Vorheriges Verzeichnis löschen, falls vorhanden
echo "Lösche vorhandenes Verzeichnis: $MERGED_DIR"
rm -rf "$MERGED_DIR"

# Neu anlegen
echo "Erstelle neues Verzeichnis: $MERGED_DIR"
mkdir -p "$MERGED_DIR"

# Alle allure-results Verzeichnisse finden und Inhalte kopieren
echo "Suche nach allure-results Verzeichnissen..."
find . -type d -name "allure-results" | while read dir; do
    echo "Kopiere Inhalte aus: $dir"
    cp -r "$dir"/* "$MERGED_DIR"/
done

echo "Fertig! Alle Ergebnisse wurden zusammengeführt nach: $MERGED_DIR"