import base64
import json
from firebase_admin import firestore, initialize_app
from datetime import datetime
import sys

default_app = initialize_app()
db = firestore.client()
statusCollection = db.collection('status')


def hello_pubsub(event, context):
    """Triggered from a message on a Cloud Pub/Sub topic.
    Args:
         event (dict): Event payload.
         context (google.cloud.functions.Context): Metadata for the event.
    """
    pubsub_message = base64.b64decode(event['data']).decode('utf-8')
    jsonMsg = json.loads(pubsub_message)
    jsonMsg['sub_timestamp'] = str(datetime.utcnow().timestamp())
    print("JSON message: {}".format(jsonMsg))

    if not jsonMsg.get('pub_timestamp') :
         print("No pub_timestamp in message. Ignoring it!", file=sys.stderr)
         return
    if statusCollection.document(jsonMsg['pub_timestamp']).get().exists :
         print("Status update with timestamp {} already exists and will be overwritten".format(jsonMsg['pub_timestamp']))

    statusCollection.document(jsonMsg['pub_timestamp']).set(jsonMsg)
    statusCollection.document("latest").set(jsonMsg)