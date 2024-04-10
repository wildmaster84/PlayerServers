#!/bin/bash
SNAME=$1

screen -S $SNAME -p 0 -X stuff "stop $(printf \\r)"

