#!/bin/bash
# Get names and versions of applications


#=======================================================
#
# Three ways to run script:
#
# 1 './version.sh'
#
# 2 './version.sh <path-to-directory-to-iterate>'
#
# 3 './version.sh <path-to-directory-to-iterate> <output-file-name>'
#
# Run without argument and script runs in the current
# working directory
#
#=======================================================

#Argument example (absolute path to symlink target):
# '/www/inera/releases/tak/tak-services-2.4.0.war'
#
#Returns:
# 'tak-services-2.4.0'
extractFileFromPathAndExtension(){
  echo "$(basename $1 ."${1##*.}")" #>> "./users.txt"
}

#Prints application information in the format 'NAME=VERSION'
#to a file
#
# $1 absolute path to aa symlink pointing to a target application
# (example '/www/inera/war/tak/tak-services.war')
#
#Prints to file:
# 'tak-services=2.4.0'
processFile () {
  #If file is an .sh-file, ignore it
  if [ ${1: -3} == ".sh" ]; then
    return
  fi

  #Absolute path to symlink target
  LINKS_TO="$(readlink -f $1)"

  #Remove spaces in path
  LINKS_TO_NO_SPACE="${LINKS_TO// /}"

  #Get target name full name only, including version, excludig path
  TARGET_FULL_NAME=$(extractFileFromPathAndExtension $LINKS_TO_NO_SPACE)

  #Extracts only the name from full name
  TARGET_NAME=$(extractName $TARGET_FULL_NAME)

  #Extracts only the version from full name
  TARGET_VERSION=$(extractVersion $TARGET_FULL_NAME $TARGET_NAME)

  #Path to current file. Needed to separate files with the same name
	DIR_PATH=$(pwd)

  #Write name of target to output file
  printf "$TARGET_NAME=$TARGET_VERSION=$DIR_PATH\n" >> "$OUTPUT_FILENAME"


}

#Argument example (target full name, both name and version):
# $1 full name, both name and version
#
# 'GetAggregatedCareContacts-v2-teststub-2.2.1'
#
#Returns (name):
# 'GetAggregatedCareContacts-v2-teststub'
extractName(){
  FULL_NAME=$1

  #From string FULL_NAME, remove the substring matching the pattern
  #of: "dash, followed by any number, followed by a period, then anything (*)"
  echo "${FULL_NAME%%-[0-9].*}"
}

#Argument example (target full name, both name and version):
# $1 full name, both name and version
# $2 only name, without version
#
# 'GetAggregatedCareContacts-v2-teststub-2.2.1'
#
#Returns (version):
# '2.2.1'
extractVersion(){
  FULL_NAME=$1
  NAME=$2

  #From string FULL_NAME, remove the substring matching the pattern
  #of: "anything before the name, followed by a dash", leaving only
  #the version left
  echo "${FULL_NAME##*$NAME-}"
}

#Recursively iterates over the file structure in a depth-first
#manner while printig file names to a file
traverse () {

  #Change directory to argument
  cd "$1" || return

  #Iterate over all files and directories in current directory
  for element in *; do
    #If element is a directory, process that directory
    if [[ -d "$element" ]]; then
      traverse "$element" "$FILE"

    #If element is a file
    elif [[ -f "$element" ]]; then
      processFile "$element"

    fi

  done
  cd ..
}


#Get current date and time
DATE=$(date)

#Get directory of this shell script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

#Absolute path to output file
OUTPUT_FILENAME="${DIR}/app_versions"

echo $DIR

#If no arguments was passed to the script, run in current directory
if [ $# -eq 0 ]; then
    traverse .

  elif [ $# -eq 1 ]; then
    #1 argument was passed to the script. Run script in specified directory

    #Write present date and time to the output file
    echo "${DATE}" > "$OUTPUT_FILENAME"

    traverse "$1"

  elif [ $# -eq 2 ]; then
    #2 arguments was passed to the script. Run script in specified directory ($1)
    #and give the output file the name of the second argument ($2)
    OUTPUT_FILENAME="${2}"
    #OUTPUT_FILENAME="${DIR}/$2"

    #Write present date and time to the output file
    echo "${DATE}" > "$OUTPUT_FILENAME"

    traverse "$1"

  else
    #An invalid amount of arguments was passed to the script
    echo "Invalid argument count"
fi
