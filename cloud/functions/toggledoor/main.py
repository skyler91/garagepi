from google.cloud import pubsub_v1

publisher = pubsub_v1.PublisherClient()
topic_path = publisher.topic_path("garagepi-289102", "garagecommand")

def hello_world(request):
    """Responds to any HTTP request.
    Args:
        request (flask.Request): HTTP request object.
    Returns:
        The response text or any set of values that can be turned into a
        Response object using
        `make_response <http://flask.pocoo.org/docs/1.0/api/#flask.Flask.make_response>`.
    """
    request_json = request.get_json()

    print(f"Request: {request_json}")
    if request_json and 'command' in request_json:
        data = data.encode(request_json)
        future = publisher.publish(topic_path, data)
        print(future.result())
    else:
        return f'Hello World!'
