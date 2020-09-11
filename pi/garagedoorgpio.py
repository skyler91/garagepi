import os
import gpiozero
from time import sleep
from signal import pause
from enum import Enum

RELAY_PIN = 2
CLOSE_SENSOR_PIN = 14

relay = gpiozero.OutputDevice(RELAY_PIN)
door_sensor = gpiozero.LineSensor(CLOSE_SENSOR_PIN, sample_rate=10)

class DoorStatus(str, Enum) :
    OPEN = "open"
    CLOSED = "closed"

def monitor_door(openCallback, closeCallback) :
    door_sensor.when_line = openCallback
    door_sensor.when_no_line = closeCallback
    pause()

def get_door_status():
    return DoorStatus.OPEN if is_door_open() else DoorStatus.CLOSED

def is_door_open():
    return door_sensor.value == 0

def open_door():
    if not is_door_open():
        toggle_door()

def close_door():
    if is_door_open():
        toggle_door()

def toggle_door():
    relay.on()
    sleep(1)
    relay.off()

def print_open():
    print("Opened!")

def print_close():
    print("Closed!")


if __name__ == "__main__":
    print("Monitoring garage door status")
    door_sensor.when_line = print_open
    door_sensor.when_no_line = print_close
    pause()
