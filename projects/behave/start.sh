#!/usr/bin/env bash

# JVM flags must come before -jar; anything after the jar is passed to the app.
java --add-exports=java.base/java.lang=ALL-UNNAMED \
     --add-exports=java.desktop/sun.awt=ALL-UNNAMED \
     --add-exports=java.desktop/sun.java2d=ALL-UNNAMED \
     --add-exports=java.desktop/sun.awt.X11=ALL-UNNAMED \
     --add-opens=java.desktop/sun.awt=ALL-UNNAMED \
     --add-opens=java.desktop/sun.lwawt=ALL-UNNAMED \
     --add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED \
     -Xss4M \
     -Xmx1024m \
     -jar target/behave7.jar
