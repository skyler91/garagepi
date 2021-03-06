# Monitors garage door status for changes and sends updates to the cloud
import logging
import gcloud
import garagedoorgpio
from messages import DoorMessage, DoorStatus

def start_monitor() :
    door_status = garagedoorgpio.get_door_status()
    logging.info(f"Monitoring garage door status (currently {door_status.value})")
    garagedoorgpio.monitor_door(door_opened, door_closed)

def set_initial_status() :
    statusFromCloud = DoorStatus.from_str(gcloud.get_door_status()['status'])
    statusFromDoor = garagedoorgpio.get_door_status()
    logging.info(f"Initial cloud status: {statusFromCloud}")
    logging.info(f"Initial door status: {statusFromDoor.value}")
    if statusFromCloud != statusFromDoor :
        logging.info(f"Updating initial door state to {statusFromDoor.value}")
        gcloud.update_door_status(DoorMessage(statusFromDoor, "Initial status from monitor"))
        #gcloud.publish_message(statusFromDoor, "Initial status from monitor")

def door_opened() :
    logging.info("Detected door opened")
    gcloud.update_door_status(DoorMessage(DoorStatus.OPEN, "Open detected by watcher"))
    #gcloud.publish_message("open", "Open detected by watcher")

def door_closed() :
    logging.info("Detected door closed")
    gcloud.update_door_status(DoorMessage(DoorStatus.CLOSED, "Close detected by watcher"))
    #gcloud.publish_message("closed", "Close detected by watcher")

if __name__ == '__main__' :
    set_initial_status()
    start_monitor()
