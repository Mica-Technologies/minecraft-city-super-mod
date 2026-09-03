#!/bin/bash
#
# Prints the initTabBlock/initTabItem lines for every Block*/Item* class in one subsystem package,
# ready to paste into that subsystem's CsmTab class.
#
# The mod is built from several source trees: Core at src/main plus one per optional module under
# modules/<name>/src/main (see modules.gradle). A subsystem package lives in exactly one of them,
# so a package name is enough -- the script finds the tree that has it.
#
# Usage:
#   dev-env-utils/src/main/java/com/micatechnologies/minecraft/csm/getTabInitListFromBlockClassFolder.sh <package> [repo-root]
#
#   <package>    the package below com/micatechnologies/minecraft/csm, e.g. trafficaccessories
#                or trafficsignals/logic. A full path to the folder also works.
#   [repo-root]  defaults to the repository this script lives in.
#
# Writes <package-name>_blocks.txt in the current directory.

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <package> [repo-root]" >&2
    echo "  e.g. $0 trafficaccessories" >&2
    exit 1
fi

package="$1"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../dev-env-utils/src/main/java/com/micatechnologies/minecraft/csm -> repository root
repo_root="${2:-$(cd "$script_dir/../../../../../../../.." && pwd)}"

java_package="com/micatechnologies/minecraft/csm"

# An absolute or already-resolved folder is taken as-is; otherwise search Core then each module.
if [ -d "$package" ]; then
    target_folder="$package"
else
    target_folder=""
    for tree in "$repo_root" "$repo_root"/modules/*; do
        candidate="$tree/src/main/java/$java_package/$package"
        if [ -d "$candidate" ]; then
            target_folder="$candidate"
            break
        fi
    done
fi

if [ -z "$target_folder" ]; then
    echo "No source tree has package '$package'." >&2
    echo "Looked in $repo_root/src/main and $repo_root/modules/*/src/main." >&2
    exit 1
fi

echo "Reading $target_folder" >&2

folder_name=$(basename "$target_folder")
output_file="${folder_name}_blocks.txt"
: > "$output_file"

for path in "$target_folder"/*.java; do
    [ -e "$path" ] || continue
    file_name=$(basename "$path")
    if [[ "$file_name" =~ ^Block([A-Za-z0-9_]+)\.java$ ]]; then
        block_name="${BASH_REMATCH[1]}"
        echo "initTabBlock( Block${block_name}.class, fmlPreInitializationEvent ); // ${block_name}" >> "$output_file"
    elif [[ "$file_name" =~ ^Item([A-Za-z0-9_]+)\.java$ ]]; then
        item_name="${BASH_REMATCH[1]}"
        echo "initTabItem( Item${item_name}.class, fmlPreInitializationEvent ); // ${item_name}" >> "$output_file"
    fi
done

echo "Wrote $(wc -l < "$output_file") line(s) to $output_file" >&2
