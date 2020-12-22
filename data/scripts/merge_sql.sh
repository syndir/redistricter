#!/bin/bash
#
# Merges the three sql files into data.sql and moves the resulting file to the
# correct location

if ! [[ -f fl.sql ]]
then
    echo fl.sql does not exist
    exit 0
fi

if ! [[ -f al.sql ]]
then
    echo al.sql does not exist
    exit 0
fi

if ! [[ -f pa.sql ]]
then
    echo pa.sql does not exist
    exit 0
fi

cat al.sql fl.sql pa.sql > data.sql
cp data.sql ../../server/src/main/resources/META-INF/sql/data.sql
