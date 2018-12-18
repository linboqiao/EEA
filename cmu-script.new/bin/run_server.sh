#!/usr/bin/env bash
if [ "$#" -lt 1 ]; then
    echo "Command not provided"
    exit 1
fi

ssh $cairo 'cd "projects/cmu-script"; '$@''
