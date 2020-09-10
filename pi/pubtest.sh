#!/bin/bash

export GOOGLE_APPLICATION_CREDENTIALS=garagepi-pub.json
python3 -c 'import pubsub; pubsub.publish_message("garagepi-289102", "garagecommand", pubsub.DoorMessage("open", "Manual entry from pubtest script"))'