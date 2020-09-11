from firebase_admin import firestore, initialize_app
import json

default_app = initialize_app()
db = firestore.client()
statusCollection = db.collection('status')

def get_status(request):
    """Responds to any HTTP request.
    Args:
        request (flask.Request): HTTP request object.
    Returns:
        The response text or any set of values that can be turned into a
        Response object using
        `make_response <http://flask.pocoo.org/docs/1.0/api/#flask.Flask.make_response>`.
    """
    request_json = request.get_json()
    if request.args and 'timestamp' in request.args:
        print("Getting door status for timestamp {}".format(request.args.get('timestamp')))
        return json.dumps(statusCollection.document(request.args.get('timestamp')).get().to_dict())
    elif request_json and 'message' in request_json:
        return request_json['message']
    else:
        print("Getting latest door status")
        return json.dumps(statusCollection.document('latest').get().to_dict())
