import json
import garagedoorgpio
from datetime import datetime
from firebase_admin import credentials, firestore, initialize_app
from google.cloud import pubsub_v1

default_app = initialize_app()
db = firestore.client()
statusCollection = db.collection("status")

# TODO: Get project/topic from ENV
PROJECT = "garagepi-289102"
TOPIC = "garagecommand"
SUBSCRIPTION = "garagestatus"

subscriber = pubsub_v1.SubscriberClient()
subscription_path = subscriber.subscription_path(PROJECT, SUBSCRIPTION)

class DoorMessage :
    def __init__(self, status, message) :
        self.status = status
        self.message = message
        self.timestamp = datetime.utcnow().timestamp()
    
    def to_json(self) :
        return json.dumps({'status': self.status, 'message': self.message, 'pub_timestamp': str(self.timestamp)})

def publish_message(status, message) :
    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(PROJECT, TOPIC)
    #print(f"Topic path: {topic_path}")
    msg = DoorMessage(status, message)

    data = str(msg.to_json())
    print(f"Publishing status update: {data}")
    data = data.encode("utf-8")
    publisher.publish(topic_path, data)
    print("published messages")

def garagecommand_callback(message) :
    print(f"Received command: {message}")
    msgDict = json.loads(message)
    if msgDict['command'] == "open" :
        garagedoorgpio.open_door()
    elif msgDict['command'] == "close" :
        garagedoorgpio.close_door()
    else :
        print(f"Unknown command: {msgDict['command']}")
    message.ack()

def subscribe_to_garagecommand() :
    streaming_pull_future = subscriber.subscribe(subscription_path, callback=garagecommand_callback)
    print(f"Listening for messages on {subscription_path}")
    with subscriber :
        try :
            streaming_pull_future.result()
        except TimeoutError :
            streaming_pull_future.cancel()

def get_door_status() :
    return statusCollection.document("latest").get().to_dict()
