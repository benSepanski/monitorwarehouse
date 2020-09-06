# Delete all .log and .jar files
find . -type f -regextype egrep -regex ".*\.(log|jar)" -delete
# Delete any logs/ directoreis
find . -type d -name "logs" | xargs rmdir
