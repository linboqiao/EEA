#!/usr/bin/env bash
args=$@
mvn exec:java -pl event-coref -Dexec.mainClass="edu.cmu.cs.lti.event_coref.pipeline.DescriptorGenerator" -Dexec.args="$args"
