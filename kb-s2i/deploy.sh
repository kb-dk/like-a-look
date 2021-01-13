#!/usr/bin/env bash

cp -- /tmp/src/conf/ocp/logback.xml "$CONF_DIR/logback.xml"
cp -- /tmp/src/conf/like-a-look.yaml "$CONF_DIR/like-a-look.yaml"
 
ln -s -- "$TOMCAT_APPS/like-a-look.xml" "$DEPLOYMENT_DESC_DIR/like-a-look.xml"
