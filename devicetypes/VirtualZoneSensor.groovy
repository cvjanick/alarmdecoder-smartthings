/* Virtual Contact Sensor for alarm panel zones */
// 
// cjanick updates: added attributes for zoneID, zoneName, ZoneDescription

metadata {
    definition (name: "Virtual Zone Sensor", namespace: "alarmdecoder", author: "scott@nutech.com") {
        capability "Contact Sensor"
        capability "Refresh"
    }
    
    attribute "zoneID",          "number"
    attribute "zoneName",        "string"
    attribute "zoneDescription", "string"
    
    command "refresh"

    // tile definitions 
    tiles (scale: 2)  {
        standardTile("sensor", "device.contact", width: 2, height: 2, canChangeIcon: true) {
            state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
            state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ff0000"
        }

        standardTile("zone_id_tile", "device.zoneID", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: 'Zone ${currentValue}'
        }
        standardTile("zone_name_tile", "device.zoneName", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
            state "default", label: '${currentValue}'
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'Refresh', action:"device.refresh", icon: "st.secondary.refresh-icon"
        }

        main "sensor"
        details(["sensor", "zone_id_tile",   "refresh",
                 "zone_name_tile"])
    }
}

/** commands **/
def refresh() {
   parent.refresh()
   }
