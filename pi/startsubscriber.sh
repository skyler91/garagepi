#!/bin/bash

export GOOGLE_APPLICATION_CREDENTIALS=garagepi-pub.json
python3 -c 'import gcloud; gcloud.subscribe_to_garagecommand()'