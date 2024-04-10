#!/bin/bash
UUID=$1
NAME=$2
SRVPATH=$3
MX=$4
MS=$5
JARFILE=$6
FULLPATH=$3/$1
cd $FULLPATH

screen -dmS $NAME java -Xmx$MX -Xms$MS -Dcom.mojang.eula.agree=true -jar $JARFILE