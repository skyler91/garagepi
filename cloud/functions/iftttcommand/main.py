from google.cloud import pubsub_v1
import json

# TODO: Get token from cloud store
valid_token = "<AUTH_TOKEN_GOES_HERE>"

def process_door_command(request):
    """Responds to any HTTP request.
    Args:
        request (flask.Request): HTTP request object.
    Returns:
        The response text or any set of values that can be turned into a
        Response object using
        `make_response <http://flask.pocoo.org/docs/1.0/api/#flask.Flask.make_response>`.
    """
    valid_commands = ["open", "close"]
    request_json = request.get_json()
    token = request_json.get("token")
    if token != valid_token :
        return "Invalid token", 401
    command = request_json.get("command")
    if command in valid_commands :
        publisher = pubsub_v1.PublisherClient()
        topic_path = publisher.topic_path("garagepi-289102", "garagecommand")
        print(f"Publishing {command} command")
        future = publisher.publish(topic_path, json.dumps(request_json).encode('utf-8'))
        return str(future.result()), 200
    else :
        return f"Invalid command: {command}", 400