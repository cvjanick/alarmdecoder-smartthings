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
 *  cvjanick Recommended Modifications for this Version
 *  Alarm Decoder Keypad Layout is a child device that enables a WebApp like keypad display in SmartThings.
 *  Intended for toggling between normal device layout view and keypad layout.
 *
 */
physicalgraph.app.DeviceWrapper;

preferences {
     section(name: "DevicePrefs", title: "Device Settings", install: true, uninstall: true) {
        icon(title: "Select AlarmDecoder icon", required: false)
        paragraph "The Keypad Component is a child device of Alarm Decoder"
     }
} // prefs

metadata {  
    definition ( name:        "Alarm Decoder Keypad Layout", 
                 namespace:   "alarmdecoder", 
                 description: "Alarm Decoder Network Appliance Device Handler",
                 author:      "Scott Petersen" ) 
                 {
                 capability "Actuator"

                 attribute "keypadMsg", "string"
                 attribute "chime",     "string"
                 attribute "key_1",     "string"
                 attribute "key_2",     "string"
                 attribute "key_3",     "string"
                 
                 command "refresh"
                 command "sendKey"
                 command "chime_toggle"
                 command "key_1"
                 command "key_2"
                 command "key_3"
                 command "key_4"
                 command "key_5"
                 command "key_6"                 
                 command "key_7"
                 command "key_8"
                 command "key_9" 
                 command "key_Asterisk"
                 command "key_0"
                 command "key_Pound" 
                 command "find"
                 command "findByType"
                 }
       tiles(scale:2) {
          valueTile("keypadMsg", "device.keypadMsg", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
               state "default", label:'${currentValue}'
          }
          standardTile("K1", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'1', action:"key_1"
          }
          standardTile("K2", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'2', action:"key_2"
          }
          standardTile("K3", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'3', action:"key_3"
          }  
          standardTile("K4", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'4', action:"key_4"
          }
          standardTile("K5", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'5', action:"key_5"
          }
          standardTile("K6", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'6', action:"key_6"
          }  
          standardTile("K7", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'7', action:"key_7"
          }
          standardTile("K8", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'8', action:"key_8"
          }
          standardTile("K9", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'9', action:"key_9"
          }
          standardTile("K*", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'*', action:"key_Asterisk"
          }
          standardTile("K0", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'0', action:"key_0"
          }
          standardTile("K#", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'#', action:"key_Pound"
          }
          standardTile("find", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'Find', action:"find"
          }
          standardTile("find2", "device.actuator", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
               state "default", label:'Find By Type', action:"findByType"
          }          
          
          main "keypadMsg"
          details([ 
                  "keypadMsg", 
                  "K1", "K2", "K3",
                  "K4", "K5", "K6",
                  "K7", "K8", "K9",
                  "K*", "K0", "K#",
                  "find", "find2",
                                    "keypadMsg", 
                ])
       }
}

def initialize() {
  //def ad = (physicalgraph.app.DeviceWrapper)parent.getAlarmDecoder()
 // log.debug "keypad initialize ad=${ad} type=${ad.getTypeName()}"
  // subscribe(ad, keypadMsgHandler)
   
}

def installed() {
  sendEvent(name: "keypadMsg", value: "Loading, Please Wait...")
  initialize()
  
  parent.refresh()
}

def updated() {
  //initialize()
  addFunctionKeys()
}

def uninstalled() {
  // unsubscribe()
}

def parse(description) {
   log.debug("description=${description}")
   }

def keypadMsgHandler(evt) {
   log.debug "--keypad keypadMsgHandler evt=${evt}"
   sendEvent(name: evt.name, value: evt.value )
}
def chimeHandler(evt) {
   log.debug "--keypad chimeHandler evt=${evt}"
   sendEvent(name: evt.name, value: evt.value )
}

def refresh() {
  parent.refresh()
}

def chime_toggle() {
   request=parent.send_chime_keys()
   parent.sendHubCommand(request)
}
def key_1() {
   sendKey("1")
}
def key_2() {
   sendKey("2")
}
def key_3() {
   sendKey("3")
}
def key_4() {
   sendKey("4")
}
def key_5() {
   sendKey("5")
}
def key_6() {
   sendKey("6")
}
def key_7() {
   sendKey("7")
}
def key_8() {
   sendKey("8")
}
def key_9() {
   sendKey("9")
}
def key_Asterisk() {
   sendKey("*")
}
def key_0() {
   sendKey("0")
}
def key_Pound() {
   sendKey("#")
}

def sendKey(key) {
    log.trace("--- keypad key sendKey ${state.value}")
    parent.sendHubCommand( parent.send_keys(key) )
}

def find() {
    def kpd = parent.findKeypadDevice()
    sendEvent(name: "keypadMsg", value: "keypad device=${kpd}")
}

def findByType() {
    initialize()
}
