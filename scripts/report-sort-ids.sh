#!/usr/bin/env bash

# Colors!!
YELLOW=$(tput setaf 3)
RED=$(tput setaf 1)
NC=$(tput sgr0)

# Add hash map source
source libs/shell_map.sh

# Name of output report
reportname="SORT_ID_REPORT.txt"

# Clear old output report and map
rm -r ../$reportname

# Create Sort ID Map
shell_map new sids
sids clear_all

# Get Sort IDs from Java Files

found_conflict=false
for FILE in `find ../src -name "*.java" -type f`; do
    SORTID=$(cat "$FILE" | grep "super(\s*instance,.*" | grep -o -E '[0-9]+')
    SHORTFILENAME=$(basename $FILE)
    if [ -z "$SORTID" ] 
    then
        echo "Skipping Java file without a Sort ID: $SHORTFILENAME"
    else
        # Flag for conflict warning if more than 1 instance of Sort ID
        if $(sids contains_key "$SORTID")
        then
            found_conflict=true
            echo "${YELLOW} A block sort ID conflict was found for ID $SORTID${NC}"
        fi
        
        # Add file name to map under Sort ID key
        sids put_append "$SORTID" "$SHORTFILENAME\n"
    fi
done

# Parse Sort IDs and Create Report
for sid in `sids keys`; do
    # Output to report
    echo -e "SORT ID $sid\n-------------" >> ../$reportname
    echo -e `sids get "$sid"`  >> ../$reportname
    echo -e "-------------\n"  >> ../$reportname
done

if $found_conflict
then
    echo "${RED}BLOCK SORT ID CONFLICTS WERE FOUND! View the report for further details: $reportname${NC}"
fi