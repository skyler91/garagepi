import os
import gpiozero
#import RPi.GPIO as gpio
import pigpio
from time import sleep
from signal import pause

piname = 'garagepi'
RELAY_PIN = 2
CLOSE_SENSOR_PIN = 4

relay = gpiozero.OutputDevice(RELAY_PIN)
door_sensor = gpiozero.LineSensor(CLOSE_SENSOR_PIN)
#gpio.setmode(gpio.BCM)
#gpio.setwarnings(False)
#gpio.setup(CLOSE_SENSOR_PIN, gpio.IN)

def is_door_open():
    if door_sensor.value == 0 :
        # Door is open!
        print("Door is open")
        return True
    else :
        # Door is closed!
        print("Door is closed")
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

def print_open():
    print("Opened!")

def print_close():
    print("Closed!")


if __name__ == "__main__":
    door_sensor.when_line = print_open
    door_sensor.when_no_line = print_close
    while(True):
        print(str(door_sensor.value))
        sleep(0.1)
    pause()
