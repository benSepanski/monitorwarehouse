#!/usr/bin/env bash
 
declare -A dir2args=(
    ["rwlock"]="25 0 100|0 25 100|25 25 1000|5 200 250"
    ["boundedbuffer"]="10 3 5 50")
# Use | as delimiter
IFS='|' ;

for dir in rwlock ; do 
    echo "Compiling java in $dir/";
    find $dir -type f -name "*.java" | xargs javac ;
    echo "Building $dir/$dir.jar" ;
    find $dir -type f -name "*.class" | xargs jar cmf "$dir/manifest.mf" "$dir/$dir.jar" ;
    echo "Cleaning out .class files from $dir/" ;
    find $dir -type f -name "*.class" -delete ;
    # Get args as array and put in arglist
    read -a arglist <<< ${dir2args[$dir]} ;
    # Make log directory
    if [ ! -d "$dir/logs" ]; then 
        echo "Making dir $dir/logs";
        mkdir "$dir"/logs;
    fi
    # iterate through args
    for args in ${arglist[@]} ; do 
        log_name=$(echo "$args.log" | sed -e 's/ /_/g');
        echo "Running $dir/$dir.jar with args $args with timeout of 1 min to '$dir/logs/$log_name'";
        timeout 60 echo $args | xargs java -jar "$dir/$dir.jar" 2> "$dir/logs/$log_name" ;
    done
done ;
