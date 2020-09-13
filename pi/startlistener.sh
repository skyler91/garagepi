#!/bin/bash

cd "$(dirname ${0%\*})"
export GOOGLE_APPLICATION_CREDENTIALS=garagepi-pub.json
/usr/bin/python3 garagedoormonitor.py
