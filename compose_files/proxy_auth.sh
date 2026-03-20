#!/bin/bash

mkfifo backpipe
while true; do   nc -l -p 7100  0<backpipe | nc auth 7100 1>backpipe; done