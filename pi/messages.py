import json
from datetime import datetime
from enum import Enum

class DoorStatus(str, Enum) :
    OPEN = "open"
    CLOSED = "closed"

    @staticmethod
    def from_str(label) :
        if label == "open" :
            return DoorStatus.OPEN
        if label == "closed" :
            return DoorStatus.CLOSED

class DoorMessage :
    def __init__(self, status, message) :
        self.status = status
        self.message = message
        self.timestamp = datetime.utcnow().timestamp()
    
    def to_json(self) :
        return {'status': self.status.value, 'message': self.message, 'pub_timestamp': str(self.timestamp)}
