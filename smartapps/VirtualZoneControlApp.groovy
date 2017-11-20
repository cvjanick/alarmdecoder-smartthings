/**
 *  VirtualZoneControlApp
 *
 *  Copyright 2017 Cory Janick
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  AlarmDecoder community contrib app for AlarmDecoder SmartThings automations
 *  This SmartApp will allow a user to close (restore) or open (fault) an AlarmDecoder emulated zone depending
 *  on the state of the selected SmartThings devices. 
 *
 *  Example would be SmartThings garage door sensor open faults VirtualZoneControl representing zone nbr NN, closed restores the emulated zone.
 *
 */
definition(
    name: "VirtualZoneControlApp",
    namespace: "alarmdecoder",
    parent: "alarmdecoder:AlarmDecoder (Service Manager)",
    author: "C. Janick",
    description: "Select a SmartThings Device that will be linked to an emulated zone on AlarmDecoder. Changes in device state will open/close a fault for that zone using a VirtualZoneControl",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

    page(name: "main", title: "VirtualZoneControl App - ST Device Faults/Restores Emulated Zone", uninstall: true, install: true ) {
  /***
    section("Select from ONE of the Device Types below that will open/clear a fault on an Emulated Zone") {
        input( name: "dtype", type: "enum",  title: "Device Type",  
               options: [ "Contact Sensor", "Motion Sensor", "Smoke Detector", "Lock", "Water Sensor", "Shock Sensor", "Switch", "Relay", "Door", "Garage Door"] )
    }
   ***/
    section("Automation Name") {
        label title: "Assign a Name", required: false
    }
    section("Select the AlarmDecoder") {
        input "decoder", "device.AlarmDecoderNetworkAppliance", title: "AlarmDecoder", multiple: false                                                                                                                                                    
	} // section
    section("Select the AlarmDecoder Emulated Zone Control that will be faulted or restored by the SmartThing Device") {
        input "zoneControl",   title: "AlarmDecoder Virtual Zone Control",  type: "device.VirtualZoneControl"   // "capability.doorControl",                                                                                                                                                  
	} // section
	section("Select a Device Type below that will fault or restore the Virtual Zone Control") {
		// TODO: put inputs here
        input "contactSensors",  title: "Contact (Open/Closed) Sensor?", 
                                                               "capability.contactSensor",     hideWhenEmpty: true, required: false//, multiple: true
        input "doorControls",    title: "Door Control?",       "capability.doorControl",       hideWhenEmpty: true, required: false//, multiple: true
        input "garageDoors",     title: "Garage Door Control?","capability.garageDoorControl", hideWhenEmpty: true, required: false//, multiple: true
        input "motionSensors",   title: "Motion Sensor?",      "capability.motionSensor",      hideWhenEmpty: true, required: false//, multiple: true
        input "smokeDetectors",  title: "Smoke Detector?",     "capability.smokeDetector",     hideWhenEmpty: true, required: false//, multiple: true
        input "carbonMonoxideDetectors", title: "Carbon Monoxide Detector?", 
                                                        "capability.carbonMonoxideDetector",   hideWhenEmpty: true, required: false//, multiple: true
        input "locks",          title: "Locks?",               "capability.lock",              hideWhenEmpty: true, required: false//, multiple: true
        input "switches",       title: "Switches?",            "capability.switch",            hideWhenEmpty: true, required: false, multiple: true
        input "relays",         title: "Relays?",              "capability.relaySwitch",       hideWhenEmpty: true, required: false//, multiple: true
        input "touchSensors",   title: "Touch Sensors?",       "capability.touchSensor",       hideWhenEmpty: true, required: false//, multiple: true
        input "waterSensors",   title: "Water Sensor?",        "capability.waterSensor",       hideWhenEmpty: true, required: false//, multiple: true
        input "shockSensors",   title: "Shock Sensors?",       "capability.shockSensor",       hideWhenEmpty: true, required: false//, multiple: true
        input "soundSensors",   title: "Sound Sensors?",       "capability.soundSensor",       hideWhenEmpty: true, required: false//, multiple: true
        input "temperatureSensors", title: "Temperature Measurement Devices?", 
                                                        "capability.temperatureMeasurement",   hideWhenEmpty: true, required: false//, multiple: true
	} // section
    
    section("If true fault the zone when the device is open, if false fault zone when device is closed.") {
        input(name: "faultWhenOpen",   title: "Fault When Open",  type: "bool", defaultValue: true  )                                                                                                                                               
	} // section
    section("If true fault the zone when the device is ON, if false fault zone when device is OFF.") {
        input(name: "faultWhenOn",   title: "Fault When ON",  type: "bool", defaultValue: true  )                                                                                                                                               
	} // section
   } // page

    
} // preferences



private attributeValues(attributeName) {
    switch(attributeName) {
        case "switch":
        case "relaySwitch" :
            return ["on","off"]
        case "lock":      
            return ["locked","unknown","unlocked","unlocked with timeout"]
        case "contactSensor":
        case "door" :
        case "garageDoor":
            return ["open","closed"]
        case "motionSensor":
            return ["active","inactive"]
        case "waterSensor":
            return ["wet","dry"]
        case "smoke":
            return ["clear", "detected", "tested"]
        case "carbonMonoxide":
            return ["clear", "detected", "tested"]
        case "tamper":
            return ["clear", "detected"] 
        default:
            return ["UNDEFINED"]
    }
}

private actions(attributeName) {
    switch(attributeName) {
        case "switch":
            return ["on","off"]
        case "lock":
            return ["lock","unlock"]
        case "contact":
        case "door":
            return ["open","close"]
        default:
            return ["UNDEFINED"]
    }
  }

def installed() {
	log.debug "VirtualZoneControl Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "VirtualZoneControl Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    log.debug "VirtualZoneControl initializing subscriptions..."
    if(location!=null)
      subscribe(location,       changedLocationMode)
    if(contactSensors!=null)
      subscribe(contactSensors,  "contact",       contactHandler)
    if(doorControls!=null)
      subscribe(doorControls,    "door",          doorHandler)
    if(garageDoorControl!=null)
      subscribe(garageDoors,     "door",          garageDoorHandler)
    if(motionSensors!=null)
      subscribe(motionSensors,   "motion",        motionSensorHandler)
    if(smokeDetectors!=null)
      subscribe(smokeDetectors,  "smoke",         smokeDetectorHandler)
    if(carbonMonoxideDetectors!=null)
      subscribe(carbonMonoxideDetectors, "carbonMonoxide", carbonMonoxideDetectorHandler)
    if(locks!=null)
      subscribe(locks,           "lock",          lockHandler)
    if(switches!=null)
      subscribe(switches,        "switch",        switchHandler)
    if(relays!=null)
      subscribe(relays,          "switch",        switchHandler)
    if(touchSensors!=null)
      subscribe(touchSensors,    "touch",        touchSensorHandler)
    if(waterSensors!=null)
      subscribe(waterSensors,    "water",        waterSensorHandler)
    if(shockSensors!=null)
      subscribe(shockSensors,    "shock",        shockSensorHandler)
    if(soundSensors!=null)
      subscribe(soundSensors,    "sound",        soundSensorHandler)
      
    // Check current device states
    if(contactSensors!=null && zoneControl!=null)
      checkContactSensors()
    if(doorSensors!=null && zoneControl!=null)
      checkDoorSensors
    if(garageDoorSensors!=null && zoneControl!=null)
      checkGarageDoorSensors()
    if(motionSensors!=null && zoneControl!=null)
      checkMotionSensors()
    if(smokeDetectors!=null && zoneControl!=null)
      checkSmokeDetectors()
    if(carbonMonoxideDetectors!=null && zoneControl!=null)
      checkCarbonMonoxideDetectors()
    if(locks!=null && zoneControl!=null)
      checkLocks()
    if(switches!=null && zoneControl!=null)
      checkSwitches()
    if(relays!=null && zoneControl!=null)
      checkRelays()
    if(touchSensors!=null && zoneControl!=null)
      checkTouchSensors()
     if(waterSensors!=null && zoneControl!=null)
      checkWaterSensors()
     if(shockSensors!=null && zoneControl!=null)
      checkShockSensors()
     if(soundSensors!=null && zoneControl!=null)
      checkSoundSensors()

}

// TODO: implement event handlers

def checkContactSensors() {
    def status="closed"
    contactSensors.each { s ->
       def state=s.currentState("contact").getValue()
       log.debug "contactSensor ${s} state is ${state}"
       if(state=="open")
          status="open"
    }
    if(status=="open") {
      log.debug "a contactSensor is open, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "all contactSensors closed, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}

def contactHandler(evt) {
    log.debug "one of the configured contactSensors changed states"
    checkContactSensors()
    /***
    if (evt.value == "open") {
        log.debug "contactSensor ${evt.device} opened, opening VirtualZoneControl ${settings.zoneControl}"
        zoneControl.open()
    } else if (evt.value == "closed") {
        log.debug "contactSensor ${evt.device} closed, closing VirtualZoneControl ${settings.zoneControl}"
        zoneControl.close()
    }
    ***/
}

def checkDoorSensors() {
    def status="closed"
    doorSensors.each { s ->
       def state=s.currentState("door").getValue()
       log.debug "doorSensor ${s} state is ${state}"
       if(state=="open")
          status="open"
    }
    if(status=="open") {
      log.debug "A doorSensor is open, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All doorSensors closed, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}

def doorHandler(evt) {
    log.debug "one of the configured doorSensors changed states"
    checkDoorSensors()
}

def checkGarageDoorSensors() {
    def status="closed"
    garageDoorSensors.each { s ->
       def state=s.currentState("door").getValue()
       log.debug "garageDoorSensor ${s} state is ${state}"
       if(state=="open")
          status="open"
    }
    if(status=="open") {
      log.debug "A garageDoorSensor is open, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All garageDoorSensors closed, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def garageDoorHandler(evt) {
    log.debug "one of the configured garageDoorSensors changed states"
    checkGarageDoorSensors()
}

def checkMotionSensors() {
    def status="inactive"
    motionSensors.each { m ->
       def state=m.currentState("motion").getValue()
       log.debug "motionSensor ${m} state is ${state}"
       if(state=="active")
          status="active"
    }
    if(status=="active") {
      log.debug "A motionSensor is active, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All motionSensors are inactive, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def motionSensorHandler(evt) {
    log.debug "one of the motion sensors changed states"
    checkMotionSensors()
    /***
    if (evt.value == "inactive") {
        log.debug "motionDetector ${evt.device} changed to inactive, closing VirtualZoneControl ${settings.zoneControl}"
        zoneControl.close()
    } else if (evt.value == "active") {
        log.debug "motionDetector ${evt.device} changed to active, opening VirtualZoneControl ${settings.zoneControl}"
        zoneControl.open()
    }
    ****/
}

def checkSmokeDetectors() {
    def status="clear"
    smokeDetectors.each { s ->
       def state=s.currentState("smoke").getValue()
       log.debug "smokeDetector ${s} state is ${state}"
       if(state=="detected")
          status="detected"
    }
    if(status=="detected") {
      log.debug "A smokeDetector is detecting smoke,  opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All smokeDetectors are clear, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def smokeDetectorHandler(evt) {
    log.debug "one of the smoke detectors changed states"
    checkSmokes()
    /***
    if (evt.value == "detected") {
        log.debug "SMOKE DETECTED!!..."
        zoneControl.open()
    } else if (evt.value == "clear") {
        log.debug "smoke is clear..."
        zoneControl.close()
    }
    ****/
}

def checkCarbonMonoxideDetectors() {
    def status="clear"
    carbonMonoxideDetectors.each { s ->
       def state=s.currentState("carbonMonoxide").getValue()
       log.debug "CO Detector ${s} state is ${state}"
       if(state=="detected")
          status="detected"
    }
    if(status=="detected") {
      log.debug "A CO Detector is detecting CARBON MONOXIDE!!,  opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All CO Detectors are clear, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def carbonMonoxideDetectorHandler(evt) {
    log.debug "one of the Carbon Monoxide detectors changed states"
    checkCarbonMonoxideDetectors()
    /****
    if (evt.value == "detected") {
        log.debug "SMOKE DETECTED!!..."
        zoneControl.open()
    } else if (evt.value == "clear") {
        log.debug "smoke is clear..."
        zoneControl.close()
    }
    ****/
}

def checkLocks() {
    def status="unlocked"
    locks.each { l ->
       def state=l.currentState("lock").getValue()
       log.debug "lock ${l} state is ${state}"
       if(state=="locked")
          status="locked"
    }
    if(status=="locked") {
      log.debug "A lock is unlocked, unlocked with timeout or unknown, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All locks are locked, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def lockHandler(evt) {
    log.debug "one of the locks changed states"
    checkLocks()
    /****
    if (evt.value == "unlocked") {
        log.debug "lock is unlocked..."
        zoneControl.open()
    } else if (evt.value == "locked") {
        log.debug "lock in locked..."
        zoneControl.close()
    }
    ****/
}

def checkSwitches() {
    def status="off"
    switches.each { s ->
       def state=s.currentState("switch").getValue()
       log.debug "switch ${s} state is ${state}"
       if(state=="on")
          status="on"
    }
    if(status=="on") {
      log.debug "a switch is on, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "all switches off, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def switchHandler(evt) {
    log.debug "one of the configured switches changed states"
    checkSwitches()
      
    /***
    if (evt.value == "on") {
        log.debug "switch ${evt.device} turned on, opening VirtualZoneControl ${settings.zoneControl}"
        zoneControl.open()
    } else if (evt.value == "off") {
        log.debug "switch ${evt.device} turned off, closing VirtualZoneControl ${settings.zoneControl}"
        zoneControl.close()
    }
    ****/
   
}

def checkRelays() {
    def status="off"
    relays.each { s ->
       def state=s.currentState("switch").getValue()
       log.debug "relay ${s} state is ${state}"
       if(state=="on")
          status="on"
    }
    if(status=="on") {
      log.debug "A relay is on, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All relays off, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def relayHandler(evt) {
    log.debug "one of the configured relays changed states"
    checkRelays()
}

def checkTouchSensors() {
    def status="clear"
    touchSensors.each { s ->
       def state=s.currentState("touch").getValue()
       log.debug "touchSensor ${s} state is ${state}"
       if(state=="touched")
          status="touched"
    }
    if(status=="touched") {
      log.debug "A touchSensor is touched, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All touchSensors clear, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def touchSensorHandler(evt) {
    log.debug "one of the configured touchSensors changed states"
    checkTouchSensors()
}

def checkWaterSensors() {
    def status="dry"
    waterSensors.each { s ->
       def state=s.currentState("water").getValue()
       log.debug "waterSensor ${s} state is ${state}"
       if(state=="wet")
          status="wet"
    }
    if(status=="wet") {
      log.debug "A waterSensor indicates water is detected, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All waterSensors are dry, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def waterSensorHandler(evt) {
    log.debug "One of the configured waterSensors changed states"
    checkWaterSensors()
}

def checkShockSensors() {
    def status="clear"
    shockSensors.each { s ->
       def state=s.currentState("shock").getValue()
       log.debug "shockSensor ${s} state is ${state}"
       if(state=="detected")
          status="detected"
    }
    if(status=="detected") {
      log.debug "A shockSensor shock is detected, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All touchSensors clear, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def shockSensorHandler(evt) {
    log.debug "one of the configured shockSensors changed states"
    checkShockSensors()
}

def checkSoundSensors() {
    def status="not detected"
    soundSensors.each { s ->
       def state=s.currentState("sound").getValue()
       log.debug "soundSensor ${s} state is ${state}"
       if(state=="detected")
          status="detected"
    }
    if(status=="detected") {
      log.debug "A soundSensor sound is detected, opening VirtualZoneControl ${settings.zoneControl}"
      zoneControl.open()
    } else {
      log.debug "All soundSensors clear, closing VirtualZoneControl ${settings.zoneControl}"
      zoneControl.close()
      }
}
def checkSoundSensorHandler(evt) {
    log.debug "one of the configured shockSensors changed states"
    checkSoundSensors()
}
