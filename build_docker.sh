#!/bin/bash

# 
# I may make this an ant task but for the moment this is just easier to manage

if [ $# -lt 2 ]; then
    echo "Usage: $0 <tag> <version>"
    exit 1
fi

images=("lrgs"  "routingscheduler"  "compproc" "compdepends")
tag=$1
version=$2

for image in ${images[@]}; do
    echo "Building $tag/$image:$version"
    docker build --target $image -t $tag/$image:$version .
done;