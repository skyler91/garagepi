import os
import gpiozero
#import RPi.GPIO as gpio
import pigpio
from time import sleep

piname = 'garagepi'
RELAY_PIN = 2
CLOSE_SENSOR_PIN = 5

relay = gpiozero.OutputDevice(RELAY_PIN)
door_sensor = gpiozero.LineSensor(CLOSE_SENSOR_PIN)
door_sensor.when_line(door_opened)
door_sensor.when_no_line(door_closed)
#gpio.setmode(gpio.BCM)
#gpio.setwarnings(False)
#gpio.setup(CLOSE_SENSOR_PIN, gpio.IN)

def door_closed():
    # Send update to gcloud
    print("Door closed")

def door_opened():
    # Send update to gcloud
    print("Door opened")

def is_door_open():
    if door_sensor.value < 0.5 :
        # Door is closed!
        print("Door is closed")
        return True
    else :
        # Door is open!
        print("Door is open")
        return False

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