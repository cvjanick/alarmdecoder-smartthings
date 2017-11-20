/* Virtual Zone Door Control for alarm panel zones */

// cvjanick Mod - Instead of using virtual contact sensor using virtual door control to enable fault/restore of 
//                alarmdecoder virtual zones. This will enable a smartthings sensor to be bound to a virtual expander zone
//                in a Smart App automation.

import groovy.json.JsonSlurper;

metadata {
    definition (name: "VirtualZoneControl", namespace: "alarmdecoder", author: "cvjanick") {
        capability "Door Control"
        capability "Contact Sensor"
        capability "Refresh"
    }
    
    //attribute "door",            "enum", ["closed", "closing", "open", "opening", "unknown"]    
    attribute "zoneID",          "number"
    attribute "zoneName",        "string"
    attribute "zoneDescription", "string"
    attribute "zoneContactType", "enum",    ["normally_closed", "normally_open"]
    attribute "expanderID",      "number"
    attribute "uiMessage",       "string"
    
    command "toggle"
    command "open"
    command "close"
    command "refresh"
    command "fault"
    command "clear"
    
    // tile definitions
    // Was a contact sensor previously where open meant faulted
    // Assuming normally closed devices, Here we will use open for faulted, closed for ready

    tiles (scale: 2)  {
           /******
            multiAttributeTile(name: "door", type: "generic", width: 6, height: 4 ) {
            tileAttribute("device.door", key: "PRIMARY_CONTROL") {
                 state "closed",  label: '${currentValue}', icon: "st.contact.contact.closed",  backgroundColor: "#79b821", action: "toggle"
                 state "closing", label: '${currentValue}', icon: "st.contact.contact.closed",  backgroundColor: "#c0c0c0", action: "toggle"
                 state "open",    label: '${currentValue}', icon: "st.contact.contact.open",    backgroundColor: "#ff4000", action: "toggle"
                 state "opening", label: '${currentValue}', icon: "st.contact.contact.open",    backgroundColor: "#c0c0c0", action: "toggle"
                 state "unknown", label: '${currentValue}', icon: "st.unknown.unknown.unknown", backgroundColor: "#ffffff", action: "toggle"
            }
            tileAttribute("device.uiMsg", key: "SECONDARY_CONTROL" ) {
                attributeState "default", label: 'Virtual Zone: ${currentValue}', defaultState: true
            }
        }
        *********/
        standardTile("zone_id_tile", "device.zoneID", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: 'Zone ${currentValue}'
        }
        standardTile("zone_name_tile", "device.zoneName", inactiveLabel: false, decoration: "flat", width: 4, height: 2) {
            state "default", label: '${currentValue}'
        }
        standardTile("toggle_tile", "device.door",  width: 2, height: 2, canChangeIcon: true) {
            state "closed",  action: "toggle", label: "Closed",  icon: "st.contact.contact.closed",  backgroundColor: "#79b821", nextState: "opening"
           # state "closing", action: "toggle", label: "Closing", icon: "st.contact.contact.closed",  backgroundColor: "#cccccc"
            state "open",    action: "toggle", label: "Open",    icon: "st.contact.contact.open",    backgroundColor: "#ff4000", nextState: "closing" // "#e86d13"
           # state "opening", action: "toggle", label: "Opening", icon: "st.contact.contact.open",    backgroundColor: "#cccccc"
            state "unknown",                   label: "Unknown", icon: "st.unknown.unknown.unknown", backgroundColor: "#cccccc"
        }
        standardTile("open", "device.door", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'Open', action:"open", icon: "st.secondary.refresh-icon"
        }
        standardTile("close", "device.door", inactiveLabel: false, decoration: "flat",  width: 2, height: 2) {
            state "default", label:'Close', action:"close", icon: "st.secondary.refresh-icon"
        }
        standardTile("fault", "device.door", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'Fault', action:"fault", icon: "st.secondary.refresh-icon"
        }
        standardTile("restore", "device.door", inactiveLabel: false, decoration: "flat",  width: 2, height: 2) {
            state "default", label:'Clear', action:"clear", icon: "st.secondary.refresh-icon"
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat",  width: 6, height: 2) {
            state "default", label:'Refresh', action:"refresh", icon: "st.secondary.refresh-icon"
        }

        main "toggle_tile"
        details(["door",
                 "zone_id_tile", "zone_name_tile",
                 "toggle_tile",  "open", "close", 
                 "refresh"
                 ])

    }
    
         
}

// Handlers

def installed() {
        log.trace("--VirtualZoneControl installed()")
        //subscribe( door, "door", zoneHandler)
}

def updated() {
        log.trace("--VirtualZoneControl updated()")
        unsubscribe()
        subscribeAll()      
}

def uninstalled() {
         log.trace("--VirtualZoneControl uninstall()")
}



// commands

// Note: let alarmdecoder refresh send the current status of the zone back to update the 
//       status of the door and contact attributes to show actual status.

def open() {
   def id   = device.currentValue("zoneID").toString()   
   def ad   = find_alarm_decoder()
   def zone = id.padLeft(2,"0")

   log.trace(" -- VirtualZoneControl opening fault on alarmdecoder=${ad}, zone=${zone}") 
   
   sendEvent(name: "door", value: "opening", isStateChange: true )

   // API for Fault/Restore not working so send key commands..
   def keys = "L${zone}1\\r"
   
   def result = ad.send_keys("${keys}")
   log.trace("result=${result}")

   //sendEvent(name: "delayed-refresh", value: 2 )
   //sendEvent(name: "delayed-refresh", value: 4 )
   
   return result
}

def close() {
   def id   = device.currentValue("zoneID").toString()   
   def ad   = find_alarm_decoder()
   def zone = id.padLeft(2,"0")
   
   log.trace(" -- VirtualZoneControl close/clear fault on alarmdecoder=${ad}, zone=${zone}") 

   sendEvent(name: "door", value: "closing", isStateChange: true )
   
   // API for Fault/Restore not working so send key commands..    
   def keys = "L${zone}0\\r"

   def result =  ad.send_keys("${keys}")
   log.trace("result=${result}")

   sendEvent(name: "delayed-refresh", value: 2 )
   sendEvent(name: "delayed-refresh", value: 4 )
   
   return result
}

def fault() {
   return delayBetween( [
        open(),
        refresh()
        ],
        1000 )
}

def clear() {
   return delayBetwen( [
        close(),
        refresh()
        ],
        1000 )
}

def toggle() {
   def status = device.currentValue("door").toString()
   log.trace(" -- VirtualZoneControl toggle status=${status}") 
   def result = ""
   if(status=="closed" || status=="closing")
      result=open()
   else
      result=close()
      
   return result
}

def refresh() {
   return parent.refresh()
}
// utility


def find_alarm_decoder() {
   def ad  = null
   def dni = getDataValue("ad_dni")
   parent.getAllChildDevices().each { d ->
       if(d.deviceNetworkId.equals(dni))
          ad=d
          }
   if(ad==null)
     log.error(" --VirtualZoneControl.find_alarm_decoder(): Can't find primary device!")
   return ad
}
