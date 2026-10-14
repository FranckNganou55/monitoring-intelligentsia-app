#Requires -Version 5.1
<#
.SYNOPSIS
    Build complet (frontend + backend JAR) pour le deploiement en production.
.PARAMETER MavenPath
    Chemin explicite vers mvn.cmd ou mvn. Optionnel - le script cherche automatiquement.
.EXAMPLE
    .\build-prod.ps1
    .\build-prod.ps1 -MavenPath "C:\apache-maven-3.9.9\bin\mvn.cmd"
#>
param(
    [string]$MavenPath = ""
)

Set-StrictMode -Off
$ErrorActionPreference = "Stop"

$ROOT     = Split-Path -Parent $MyInvocation.MyCommand.Path
$FRONTEND = Join-Path $ROOT "frontend"
$BACKEND  = Join-Path $ROOT "backend"
$STATIC   = Join-Path $BACKEND "src\main\resources\static"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BUILD PRODUCTION - Monitoring App     " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# --- Resolution de Maven ---
function Find-Maven {
    # 1. Parametre explicite
    if ($MavenPath -ne "" -and (Test-Path $MavenPath)) {
        return $MavenPath
    }

    # 2. mvn sur le PATH
    $onPath = Get-Command mvn -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }

    # 3. mvnw dans le repertoire backend (Maven Wrapper)
    $wrapper = Join-Path $BACKEND "mvnw.cmd"
    if (Test-Path $wrapper) { return $wrapper }
    $wrapperSh = Join-Path $BACKEND "mvnw"
    if (Test-Path $wrapperSh) { return $wrapperSh }

    # 4. Recherche dans MAVEN_HOME / M2_HOME
    foreach ($envVar in @($env:MAVEN_HOME, $env:M2_HOME)) {
        if ($envVar) {
            $candidate = Join-Path $envVar "bin\mvn.cmd"
            if (Test-Path $candidate) { return $candidate }
        }
    }

    # 5. Emplacements courants sur Windows
    $commonPaths = @(
        "C:\Program Files\Apache Software Foundation",
        "C:\Program Files\Maven",
        "C:\maven",
        "C:\tools\maven",
        "$env:LOCALAPPDATA\Programs\Maven",
        "$env:USERPROFILE\.m2\wrapper\dists"
    )
    foreach ($base in $commonPaths) {
        if (Test-Path $base) {
            $found = Get-ChildItem $base -Recurse -Filter "mvn.cmd" -ErrorAction SilentlyContinue |
                     Select-Object -First 1 -ExpandProperty FullName
            if ($found) { return $found }
        }
    }

    # 6. IntelliJ (toutes versions)
    foreach ($base in @("C:\Program Files\JetBrains", "$env:LOCALAPPDATA\JetBrains")) {
        if (Test-Path $base) {
            $found = Get-ChildItem $base -Recurse -Filter "mvn.cmd" -ErrorAction SilentlyContinue |
                     Select-Object -First 1 -ExpandProperty FullName
            if ($found) { return $found }
        }
    }

    return $null
}

$MVN = Find-Maven

if (-not $MVN) {
    Write-Host ""
    Write-Host "ERREUR : Maven (mvn) est introuvable." -ForegroundColor Red
    Write-Host ""
    Write-Host "Solutions :" -ForegroundColor Yellow
    Write-Host "  A) Ajoutez Maven a votre PATH, puis relancez le script."
    Write-Host "  B) Passez le chemin en parametre :"
    Write-Host "       .\build-prod.ps1 -MavenPath `"C:\apache-maven-3.9.9\bin\mvn.cmd`""
    Write-Host "  C) Telechargez Maven : https://maven.apache.org/download.cgi"
    Write-Host "     Extrayez dans C:\apache-maven-X.X.X et ajoutez bin\ au PATH."
    Write-Host ""
    Write-Host "  Si vous utilisez IntelliJ, Maven est dans :"
    Write-Host "    C:\Users\<vous>\.m2\wrapper\dists\  (Maven Wrapper)"
    Write-Host "    ou  C:\Program Files\JetBrains\IntelliJ IDEA <version>\plugins\maven\lib\maven3\bin\"
    Write-Host ""
    exit 1
}

Write-Host "  Maven trouve : $MVN" -ForegroundColor Gray

# --- Resolution du JDK ---
function Find-Jdk {
    # 1. JAVA_HOME deja positionne et contient javac
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\javac.exe"))) {
        return $env:JAVA_HOME
    }
    # 2. Chercher javac.exe dans des emplacements courants
    $candidates = @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Semeru",
        "C:\Program Files\Zulu",
        "$env:LOCALAPPDATA\Programs"
    )
    foreach ($base in $candidates) {
        if (Test-Path $base) {
            $found = Get-ChildItem $base -Recurse -Filter "javac.exe" -ErrorAction SilentlyContinue |
                     Select-Object -First 1 -ExpandProperty FullName
            if ($found) { return (Split-Path (Split-Path $found -Parent) -Parent) }
        }
    }
    # 3. JBR embarque dans IntelliJ (fallback)
    foreach ($base in @("C:\Program Files\JetBrains", "$env:LOCALAPPDATA\JetBrains")) {
        if (Test-Path $base) {
            $found = Get-ChildItem $base -Recurse -Filter "javac.exe" -ErrorAction SilentlyContinue |
                     Select-Object -First 1 -ExpandProperty FullName
            if ($found) { return (Split-Path (Split-Path $found -Parent) -Parent) }
        }
    }
    return $null
}

$JDK = Find-Jdk
if (-not $JDK) {
    Write-Host ""
    Write-Host "ERREUR : Aucun JDK trouve (javac introuvable)." -ForegroundColor Red
    Write-Host ""
    Write-Host "Installez un JDK 17+ :" -ForegroundColor Yellow
    Write-Host "  https://adoptium.net/  (Eclipse Temurin 17 ou 21)"
    Write-Host "  Extrayez et ajoutez bin\ au PATH, ou definissez JAVA_HOME."
    Write-Host ""
    exit 1
}

$env:JAVA_HOME = $JDK
Write-Host "  JDK utilise  : $JDK" -ForegroundColor Gray
Write-Host ""

# --- Etape 1 : Build frontend ---
Write-Host "[ 1/3 ] Build frontend React..." -ForegroundColor Cyan

if (-not (Test-Path $FRONTEND)) {
    Write-Host "ERREUR : Dossier frontend introuvable : $FRONTEND" -ForegroundColor Red
    exit 1
}

Set-Location $FRONTEND

Write-Host "        npm ci --prefer-offline ..."
npm ci --prefer-offline
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERREUR : npm ci a echoue (code $LASTEXITCODE)." -ForegroundColor Red
    exit 1
}

Write-Host "        npm run build ..."
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERREUR : npm run build a echoue (code $LASTEXITCODE)." -ForegroundColor Red
    exit 1
}

if (-not (Test-Path (Join-Path $FRONTEND "dist\index.html"))) {
    Write-Host "ERREUR : dist/index.html introuvable apres le build." -ForegroundColor Red
    exit 1
}

Write-Host "        OK - dist/ genere." -ForegroundColor Green

# --- Etape 2 : Copie vers backend/static ---
Write-Host ""
Write-Host "[ 2/3 ] Copie vers backend/static..." -ForegroundColor Cyan

if (Test-Path $STATIC) {
    Remove-Item -Path $STATIC -Recurse -Force
}
New-Item -ItemType Directory -Path $STATIC -Force | Out-Null
Copy-Item -Path (Join-Path $FRONTEND "dist\*") -Destination $STATIC -Recurse -Force
Write-Host "        OK - fichiers copies dans : $STATIC" -ForegroundColor Green

# --- Etape 3 : Build backend JAR ---
Write-Host ""
Write-Host "[ 3/3 ] Build backend JAR..." -ForegroundColor Cyan

Set-Location $BACKEND

& $MVN clean package -DskipTests "-Dspring.profiles.active=prod"
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERREUR : Maven clean package a echoue (code $LASTEXITCODE)." -ForegroundColor Red
    exit 1
}

$JAR = Get-ChildItem (Join-Path $BACKEND "target") -Filter "monitoring-backend*.jar" -ErrorAction SilentlyContinue |
       Where-Object { $_.Name -notmatch "sources|javadoc" } |
       Select-Object -First 1

if (-not $JAR) {
    Write-Host "ERREUR : JAR introuvable dans target/ apres le build." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " Build termine avec succes !" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host " JAR : $($JAR.FullName)" -ForegroundColor White
Write-Host ""
Write-Host " Deploiement sur 172.25.2.53 :" -ForegroundColor Yellow
Write-Host "   1. Copier le JAR vers le serveur"
Write-Host "   2. PostgreSQL accessible (port 5432)"
Write-Host "   3. Oracle SVFE accessible (port 1521, base cmstest)"
Write-Host "   4. Lancer :"
Write-Host "      java -jar $($JAR.Name) --spring.profiles.active=prod"
Write-Host ""
Write-Host " Variables d'env optionnelles :"
Write-Host "   DB_URL, DB_USER, DB_PASS"
Write-Host "   SVFE_DB_URL, SVFE_DB_USER, SVFE_DB_PASS"
Write-Host "   JWT_SECRET"
Write-Host ""
