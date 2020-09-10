import os
import gpiozero
import RPi.GPIO as gpio
import mockgpio
from time import sleep

piname = 'garagepi'
RELAY_PIN = 2
CLOSE_SENSOR_PIN = 5

relay = gpiozero.OutputDevice(RELAY_PIN)
gpio.setmode(gpio.BCM)
gpio.setwarnings(False)
gpio.setup(CLOSE_SENSOR_PIN, gpio.IN)

def is_door_open():
    if not isPi :
        return mockgpio.is_door_open()
    if gpio.input(CLOSE_SENSOR_PIN) == False :
        # Door is closed!
        print("Door is closed")
        return True
    else :
        # Door is open!
        print("Door is open")
        return False

def open_door():
    if not isPi:
        return mockgpio.open_door()
    if not is_door_open():
        toggle_door()

def close_door():
    if not isPi :
        return mockgpio.close_door()
    if is_door_open():
        toggle_door()

def toggle_door():
    if not isPi :
        return mockgpio.toggle_door()
    relay.on()
    sleep(1)
    relay.off()

isPi = os.uname()[1] == piname