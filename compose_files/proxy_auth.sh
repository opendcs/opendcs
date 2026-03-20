#!/bin/bash

mkfifo backpipe
#while true; do   nc -lk -p 7100  0<backpipe | nc auth 7100 1>backpipe; done
nc -lk -p 7100 -e nc auth 7100