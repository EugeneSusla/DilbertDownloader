DilbertDownloader
=================

A simple utility to enjoy Dilbert comic strips offline.
Downloads comics onto your hard drive as jpeg images.
- - -

While originally created using java standard libs only, after about a year this project was revisited 
and tweaked significantly, mainly for self-educational purposes, involving maven, git, log4j, etc.

- - -

Usage:
 1. Specify a folder in res/dilbertdownloader/config.properties, where the strips should be downloaded ('targetFolder' entry)
 2. mvn clean package
 3. Launch launcher.bat
 4. If interrupted, it will resume from where it stopped the next launch