# Monitors garage door status for changes and sends updates to the cloud
import gcloud
import garagedoorgpio

def startMonitor() :
    door_status = garagedoorgpio.get_door_status()
    print(f"Monitoring garage door status (currently {door_status})")
    garagedoorgpio.monitor_door(door_opened, door_closed)

def setInitialStatus() :
    statusFromCloud = gcloud.get_door_status()['status']
    statusFromDoor = f"{garagedoorgpio.get_door_status()}"
    print(f"Initial cloud status: {statusFromCloud}")
    print(f"Initial door status: {statusFromDoor}")
    if statusFromCloud != statusFromDoor :
        print(f"Updating initial door state to {statusFromDoor}")
        gcloud.publish_message(statusFromDoor, "Initial status from monitor")

def door_opened() :
    print("Detected door opened")
    gcloud.publish_message("open", "Open detected by watcher")

def door_closed() :
    print("Detected door closed")
    gcloud.publish_message("closed", "Close detected by watcher")

if __name__ == '__main__' :
    setInitialStatus()
    startMonitor()
