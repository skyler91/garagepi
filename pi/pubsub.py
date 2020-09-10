import json
from datetime import datetime


class DoorMessage :
    def __init__(self, status, message) :
        self.status = status
        self.message = message
        self.timestamp = datetime.utcnow().timestamp()
    
    def to_json(self) :
        return json.dumps({'status': self.status, 'message': self.message, 'pub_timestamp': str(self.timestamp)})

def publish_message(project, topic, doorMessage) :
    from google.cloud import pubsub_v1

    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(project, topic)
    print("Topic path: {}".format(topic_path))

    data = str(doorMessage.to_json())
    print("Publishing status update: {}".format(data))
    data = data.encode("utf-8")
    publisher.publish(topic_path, data)

    print("published messages")

def watch_door_status() :
    import garagedoorgpio
    garagedoorgpio.monitor_door(door_opened, door_closed)

def door_opened() :
    publish_message(PROJECT, TOPIC, DoorMessage("open", "Open detected by watcher"))

def door_closed() :
    publish_message(PROJECT, TOPIC, DoorMessage("closed", "Close detected by watcher"))


PROJECT = "garagepi-2389102"
TOPIC = "garagecommand"

if __name__ == '__main__' :
    watch_door_status()