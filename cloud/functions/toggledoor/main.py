from google.cloud import pubsub_v1
import json
from datetime import datetime

publisher = pubsub_v1.PublisherClient()
topic_path = publisher.topic_path("garagepi-289102", "garagecommand")

def toggle_door(request):
    """Responds to any HTTP request.
    Args:
        request (flask.Request): HTTP request object.
    Returns:
        The response text or any set of values that can be turned into a
        Response object using
        `make_response <http://flask.pocoo.org/docs/1.0/api/#flask.Flask.make_response>`.
    """
    print("In toggle_door")
    request_json = request.get_json()

    print(f"Request: {request_json}")
    if request_json and 'command' in request_json:
        request_json['pub_timestamp'] = str(datetime.utcnow().timestamp())
        data = json.dumps(request_json).encode("utf-8")
        
        future = publisher.publish(topic_path, data)
        print(future.result())
    else:
        return f'Hello World!'
