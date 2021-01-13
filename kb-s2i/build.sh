#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/like-a-look-*.war "$TOMCAT_APPS/like-a-look.war"
cp -- /tmp/src/conf/ocp/like-a-look.xml "$TOMCAT_APPS/like-a-look.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/like-a-look.war")
