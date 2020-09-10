import gpiozero
import RPi.GPIO as gpio
from time import sleep

RELAY_PIN = 2
CLOSE_SENSOR_PIN = 5

relay = gpiozero.OutputDevice(RELAY_PIN)
gpio.setmode(gpio.BCM)
gpio.setwarnings(False)
gpio.setup(CLOSE_SENSOR_PIN, gpio.IN)

doorClosed = True

def is_door_open():
    #return gpio.input(CLOSE_SENSOR_PIN)
    
    if gpio.input(CLOSE_SENSOR_PIN) == False :
        # Door is closed!
        if not doorClosed :
            print("Door closed")
            doorClosed = True
    else :
        # Door is open!
        if doorClosed :
            print("Door open")
            doorClosed = False
    return not doorClosed

def toggle_door():
    relay.on()
    sleep(1)
    relay.off()
    doorClosed = not doorClosed