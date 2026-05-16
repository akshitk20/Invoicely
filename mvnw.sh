#!/bin/sh
# Build invoicely using project-local Maven settings (bypasses corporate settings.xml)
mvn "$@" -s .mvn/local-settings.xml
