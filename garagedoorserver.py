from flask import Flask
import garagedoorgpio
app = Flask(__name__)

@app.route('/')
def door_status():
    if garagedoorgpio.is_door_open() :
        return "Door is open"
    else :
        return "Door is closed"

@app.route('/open')
def door_open():
    if garagedoorgpio.is_door_open() :
        return "Door already open"
    else :
        garagedoorgpio.toggle_door()
        return "Opening door"

@app.route('/close')
def door_close():
    if garagedoorgpio.is_door_open() :
        garagedoorgpio.toggle_door()
        return "Closing door"
    else :
        return "Door already closed"
