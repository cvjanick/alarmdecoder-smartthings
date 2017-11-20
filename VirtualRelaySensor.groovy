/* Virtual Contact Sensor for alarm panel zones */
// 
// cjanick updates: added attributes for relayAddress, relayChannel, relayValue

metadata {
    definition (name: "VirtualRelaySensor", namespace: "alarmdecoder", author: "cjanick") {
        capability "Contact Sensor"
        capability "Refresh"
    }
    
    attribute "relayAddress",    "number"
    attribute "relayChannel",    "number"
    attribute "relayValue",      "number"
    
    command "refresh"

    // tile definitions 
    tiles (scale: 2)  {
        standardTile("sensor", "device.contact", width: 2, height: 2, canChangeIcon: true) {
            state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
            state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ff0000"
        }

        standardTile("relay_address_tile", "device.relayAddress", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: 'Address ${currentValue}'
        }
        standardTile("relay_channel_tile", "device.relayChannel", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
            state "default", label: 'Channel ${currentValue}'
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'Refresh', action:"device.refresh", icon: "st.secondary.refresh-icon"
        }

        main "sensor"
        details(["sensor", "relay_address_tile",   "relay_channel_tile",
                 "refresh",
                ])
    }
}

/** commands **/
def refresh() {
   parent.refresh()
   }
   
