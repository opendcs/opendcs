#!/bin/bash

# 
# I may make this an ant task for for the moment this is just easier to manage

images=("lrgs"  "routingscheduler"  "compproc" "compdepends")
tag=$1
version=$2

for image in ${images[@]}; do
    echo "Building $tag/$image:$version"
    docker build --target $image -t $tag/$image:$version .
done;