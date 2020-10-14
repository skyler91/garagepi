import json
import google.cloud.logging
import logging
from datetime import datetime
from firebase_admin import credentials, firestore, initialize_app
from google.cloud import pubsub_v1
from messages import DoorMessage

# TODO: Initialize when needed?
default_app = initialize_app()

db = firestore.client()
statusCollection = db.collection("status")

# TODO: Get project/topic from ENV
PROJECT = "garagepi-289102"
TOPIC = "garagecommand"
SUBSCRIPTION = "garagecommand"

# TODO: Initialize when needed?
subscriber = pubsub_v1.SubscriberClient()
subscription_path = subscriber.subscription_path(PROJECT, SUBSCRIPTION)

def setup_logging() :
    logclient = google.cloud.logging.Client()
    logclient.get_default_handler()
    logclient.setup_logging()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(filename)s %(levelname)s: %(message)s")

def publish_message(status, message) :
    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(PROJECT, TOPIC)
    #print(f"Topic path: {topic_path}")
    msg = DoorMessage(status, message)

    data = str(msg.to_json())
    logging.info(f"Publishing status update: {data}")
    data = data.encode("utf-8")
    publisher.publish(topic_path, data)
    #print("published messages")

def garagecommand_callback(message) :
    logging.info(f"Received command: {message.data}")
    try :
        msgDict = json.loads(message.data)
    except :
        logging.warning("Bad command, ignoring...")
        return
    cmd = msgDict.get('command')
    if cmd :
        if cmd == "open" :
            import garagedoorgpio
            garagedoorgpio.open_door()
        elif cmd == "close" :
            import garagedoorgpio
            garagedoorgpio.close_door()
        else :
            logging.warning(f"Unknown command: {msgDict['command']}")
        message.ack()

def subscribe_to_garagecommand() :
    streaming_pull_future = subscriber.subscribe(subscription_path, callback=garagecommand_callback)
    logging.info(f"Listening for messages on {subscription_path}")
    with subscriber :
        try :
            streaming_pull_future.result()
        except TimeoutError :
            streaming_pull_future.cancel()

def get_door_status() :
    return statusCollection.document("latest").get().to_dict()

def update_door_status(doorMessage) :
    logging.info(f"Publishing door status update {doorMessage.status.value} at {str(doorMessage.timestamp)}")
    statusCollection.document(str(doorMessage.timestamp)).set(doorMessage.to_json())
    statusCollection.document("latest").set(doorMessage.to_json())

setup_logging()
