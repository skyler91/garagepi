import json
from datetime import datetime
from firebase_admin import credentials, firestore, initialize_app
from google.cloud import pubsub_v1

default_app = initialize_app()
db = firestore.client()
statusCollection = db.collection("status")

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

def get_door_status() :
    return statusCollection.document("latest").get().to_dict()

# TODO: Get project/topic from ENV
PROJECT = "garagepi-289102"
TOPIC = "garagecommand"
