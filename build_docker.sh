#!/bin/bash

images=("lrgs"  "routingscheduler"  "compproc")
tag=$1

for image in ${images[@]}; do
    echo "Building tag"
    docker build --target $image -t $tag .
done;