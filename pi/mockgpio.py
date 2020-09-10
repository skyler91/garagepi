from os import path

def is_door_open() :
    return readStatusFromFile().startswith('1')

def open_door() :
    if not is_door_open() :
        toggle_door()

def close_door():
    if is_door_open() :
        toggle_door()

def toggle_door():
    if is_door_open():
        writeStatusToFile('0')
    else :
        writeStatusToFile('1')

def readStatusFromFile():
    status = '0'
    if path.exists('mockstatus') :
        statusfile = open('mockstatus')
        status = statusfile.readline()
        statusfile.close()
    else :
        statusfile = open('mockstatus', 'w+')
        statusfile.write('0')
        statusfile.close()
    return status

def writeStatusToFile(status) :
    statusfile = open('mockstatus', 'w+')
    statusfile.write(status)
    statusfile.close()

statusfile = open('mockstatus', 'w+')