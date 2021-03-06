#!/usr/bin/env bash

# You've got to run this from inside the src directory

declare -A dir2args=(
    ["boundedbuffer"]="10 3 2 40|25 5 1 250|10 25 8 500"
    ["h2o"]="1 1|2 2|4 4|10 1|1 10"
    ["roundrobin"]="1 2|2 1|2 2|4 4|8 8|16 16|32 32|2 8|4 16|8 32|8 2|16 4|32 8"
    ["rwlock"]="25 0 100|0 25 100|25 25 10|5 200 25"
    ["sleepingbarber"]="2 1|4 1|8 1|16 1|32 1|64 1|8 8|16 8|32 8|64 8|128 8|256 8"
)

# Use | as delimiter
IFS='|' ;

for dir in */ ; do 
    # Remove trailing slash because I always forget about it
    dir=${dir%"/"};
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
        nsecs=30;
        echo "Running $dir/$dir.jar with args $args with timeout of $nsecs seconds to '$dir/logs/$log_name'";
        timeout $nsecs bash -c "echo $args | xargs java -jar \"$dir/$dir.jar\" 2> \"$dir/logs/$log_name\" ";
        if [ $? == 124 ]; then 
            echo "Killed after $nsecs seconds by timeout" | tee -a "$dir/logs/$log_name";
        else
            echo "run complete";
        fi;
    done
done ;
