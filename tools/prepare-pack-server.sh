#!/usr/bin/env bash
# Copies a Bubie Pack server pack (mods, config, kubejs) into runs/pack-server for ./gradlew runPackServer.
# Mods the dev run already loads from Gradle (JEI, EMI, Jade, Mekanism, Mekanism Generators) and any
# Voltaic Dynamics jar are left out, so nothing is loaded twice. Integrated Dynamics and its addons are left out
# too: a dev run always registers every mod's game tests, and theirs reference Refined Storage, which the pack
# does not have, so the dev server would crash on start (a real server never loads game tests).
# usage: tools/prepare-pack-server.sh [path/to/server-pack]   (default: newest ../minecraft-modpack/dist/server-*)
set -euo pipefail
cd "$(dirname "$0")/.."
src="${1:-$(ls -d ../minecraft-modpack/dist/server-* | sort -V | tail -1)}"
dst=runs/pack-server
rm -rf "$dst/mods" "$dst/config" "$dst/defaultconfigs" "$dst/kubejs"
mkdir -p "$dst/mods"
for dir in config defaultconfigs kubejs; do
  [ -d "$src/$dir" ] && cp -r "$src/$dir" "$dst/"
done
skipped=0
for jar in "$src"/mods/*.jar; do
  name=$(basename "$jar")
  case "$name" in
    jei-*|emi-*|Jade-*|Mekanism-1*|MekanismGenerators-*|voltaicdynamics-*|integrated*) skipped=$((skipped + 1)) ;;
    *) cp "$jar" "$dst/mods/" ;;
  esac
done
echo "Server pack: $src"
echo "Copied $(ls "$dst/mods" | wc -l) mods into $dst/mods (skipped $skipped: already in the dev run, or Integrated Dynamics)."
