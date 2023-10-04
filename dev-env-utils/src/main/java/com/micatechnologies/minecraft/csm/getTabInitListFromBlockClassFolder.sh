#!/bin/bash

# Specify the directory where you want to process the files
target_folder="E:\\source\\repos\\minecraft-city-super-mod\\src\\main\\java\\com\\micatechnologies\\minecraft\\csm\\trafficaccessories"

# Extract the folder name from the target path
folder_name=$(basename "$target_folder")

# Create a filename based on the folder name
output_file="${folder_name}_blocks.txt"

# Use ls command to list files in the specified directory
ls_output=$(ls -l "$target_folder")

# Process each line of the ls output and write to the output file
while IFS= read -r line; do
    file_name=$(echo "$line" | awk '{print $NF}')
    if [[ "$file_name" =~ ^Block([A-Za-z0-9_]+)\.java$ ]]; then
        block_name="${BASH_REMATCH[1]}"
        formatted_line="initTabBlock( Block${block_name}.class, fmlPreInitializationEvent ); // ${block_name}"
        echo "$formatted_line" >> "$output_file"
    elif [[ "$file_name" =~ ^Item([A-Za-z0-9_]+)\.java$ ]]; then
        item_name="${BASH_REMATCH[1]}"
        formatted_line="initTabItem( Item${item_name}.class, fmlPreInitializationEvent ); // ${item_name}"
        echo "$formatted_line" >> "$output_file"
    fi
done <<< "$ls_output"
