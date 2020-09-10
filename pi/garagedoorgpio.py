import gpiozero
import RPi.GPIO as gpio
from time import sleep

RELAY_PIN = 2
CLOSE_SENSOR_PIN = 5

relay = gpiozero.OutputDevice(RELAY_PIN)
gpio.setmode(gpio.BCM)
gpio.setwarnings(False)
gpio.setup(CLOSE_SENSOR_PIN, gpio.IN)

def is_door_open():
    if gpio.input(CLOSE_SENSOR_PIN) == False :
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
