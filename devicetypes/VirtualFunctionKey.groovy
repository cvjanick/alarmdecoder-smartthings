/**
 *  AlarmDecoder Network Appliance
 *
 *  Copyright 2016 Nu Tech Software Solutions, Inc.
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
 *  cvjanick Recommended Modifications for Version 2
 *  This is a child device for Alarm Decoder Network Appliance device hander that will render
 *  a Function Key with a configured label and countdown delay before sending the keys to AlarmDecoder.
 *  
 *  To render in the device handler add the child device in device handler updated() and use a child device tile:
 *  // childDeviceTile( tilename, componentName (must match dev), width: xx, height: xx, childTileName: string (must match child dev tile name)
 *         childDeviceTile( "F1",    "F1", width: 3, height: 1, childTileName: "key" )
 *
 *  For debug set isComponent: false when creating child devices so they will show as separate devices
 *  in SmartThings Things list.
 *
 */
 
import groovy.json.JsonSlurper;

preferences {
     section(name: "DevicePrefs", title: "Device Settings", install: true, uninstall: true) {
        icon(title: "Select AlarmDecoder icon", required: false)
        paragraph "The Keypad Component is a child device of Alarm Decoder"
     }
} // prefs

metadata {  
    definition ( name:        "Alarm Decoder Function Key", 
                 namespace:   "alarmdecoder", 
                 description: "Alarm Decoder Function Key Child Device Handler",
                 author:      "C. Janick" ) 
                 {
                 capability "Actuator"
                 capability "Switch"
                 
                 attribute "key_label",    "string"
                 attribute "key_in_label", "string"
                 attribute "counter",      "number"
                 
                 command "refresh"
                 command "sendKey"
                 command "panic1"
                 command "panic2"
                 command "panic3"
                 command "reset"
                 command "on"
                 command "off"
                 command "toggle"
                 }
                 
       tiles(scale:2) {
          valueTile("switch", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
               state "on",   label: '${currentValue}', action: "toggle",  backgroundColor: "#ff0000"
               state "off",  label: '${name}',         action: "toggle",  backgroundColor: "#2222aa"
               /***
              
               state "off", label: '${currentValue}', action: "switch.on",
                  icon: "st.switches.switch.off", backgroundColor: "#ffffff"
               state "on", label: '${currentValue}', action: "switch.off",
                  icon: "st.switches.switch.on", backgroundColor: "#00a0dc"
                  ***/
          }
          standardTile("key", "device.key_label", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "off",   label: '${currentValue}', action:"toggle",  backgroundColor: "#ffffff", nextState: "on"
               state "on",    label: '${currentValue}', action:"toggle",  backgroundColor: "#ff0000", nextState: "on"
          }
          standardTile("keyIn", "device.key_in_label", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
               state "off",   label: '${currentValue}', action:"toggle",  backgroundColor: "#ffffff", nextState: "on"
               state "on",    label: '${currentValue}', action:"toggle",  backgroundColor: "#ff0000", nextState: "on"
          }
          valueTile("counter", "device.counter", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
                 state "counter", label:'${currentValue}', defaultState: true, backgroundColors: [
                       [value: 5,  color: "#ff0000"],
                       [value: 10, color: "#ffa81e"],
                       [value: 15, color: "#ffffff"]
                       ]
          }

          main ([ "key" ])
          details([ 
                    "key", "counter", "switch"
                  ])
       }
}

/**** standard callbacks *****/

def initialize() {
   state.label = getDataValue("label")
   state.value = getDataValue("value")
   state.delay = getDataValue("delay")
   sendEvent(name: "switch",       value: "off",       isStateChange: true) 
   sendEvent(name: "key_label",    value: state.label, isStateChange: true)
   sendEvent(name: "key_in_label", value: state.label, isStateChange: true)
   
   state.label2 = state.label + " Tap 2x"
   state.label2 = state.label + " Tap 1x"
   //subscribe(button, "button.default",  buttonEvent)
   //subscribe(button, "button.pushed",   buttonEvent)
   //subscribe(button, "button.held",     buttonEvent)
   //subscribe(button, "button.released", buttonEvent)
 }

def installed() {
   initialize()
}

def updated() {
   initialize()
}

/****** commands ******/

def toggle() {
    def currstate = device.currentValue("switch").toString()
    log.trace("Special Function Key Toggle - Switch is ${currstate}" )
    if(currstate=="on") 
        off()
    else
        on()
}
def on() {
    log.trace("Special Function Key - Switch is ON")
    sendEvent(name: "switch", value: "on")
    def currstate = device.currentValue("switch").toString()
    state.delay   = getDataValue("delay").toInteger()
    log.trace("Special Function Key - label=${state.label}, delay=${state.delay}, switch is ${currstate}" )
    if(state.delay!=null)
       state.panic_counter=state.delay+1
    if(state.panic_counter==null)
       state.panic_counter=16
    else
       state.panic_counter=state.delay+1
    if(parent!=null)
      parent.sendEvent(name: "keypadMsg", value: "Countdown started for ${state.label} Alarm, Tap Again to Cancel...", isStateChange: true, displayed: false)
    checkPanicCounter()
}

def off() {
    log.trace("Special Function Key - Switch is OFF")
    sendEvent(name: "switch", value: "off")
    def currstate = device.currentValue("switch").toString()
    log.trace("Special Function Key - Switch is ${currstate}" )
}


/**** utilities *****/

def checkPanicCounter() { 
    if(state.panic_counter==null)
       state.panic_counter=16
    state.panic_counter=state.panic_counter-1
    //log.trace("checkPanicCounter - Panic countdown ${state.panic_counter}")
    def currstate = device.currentValue("switch").toString()
    if(currstate=="on")  {
         def tmplabel = state.label + " IN ${state.panic_counter}"
         sendEvent(name: "key_label", value: tmplabel)
         sendEvent(name: "counter", value: "${state.panic_counter}")
         if(state.panic_counter>0)
              runIn(1, checkPanicCounter)
         else 
              sendKeysAndReset()
     } else
          cancel()    
}

def sendKeysAndReset() {
    log.trace("Special Function Key ${state.label} - Sending Function Key...")    
    sendKey()
    reset()
}

def cancel() {
    if(parent!=null)
       parent.sendEvent(name: "keypadMsg", value: "Send ${state.label} command cancelled", isStateChange: true, displayed: false)
    reset()
}

def reset() {
    sendEvent(name: "switch",    value: "off")
    sendEvent(name: "key_label", value: state.label)
    sendEvent(name: "counter",   value: "")
    refresh()
}

def getDelay() {
    def delay=device.getDataValue("delay")
    return delay
}

def setDelay(seconds) {
    device.updateDataValue("delay",seconds)
}

def refresh() {
    parent.refresh()
}

def sendKey() {
    log.trace("--- keypad key sendKey ${state.value}")
    sendEvent(name: "button", value: "default", isStateChange: true)
    parent.sendHubCommand( parent.send_keys(state.value) )
}
