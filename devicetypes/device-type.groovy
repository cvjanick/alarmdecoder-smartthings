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
 *  AlarmDecoder is a great product with the WebApp but to help make it easier for SmartThings users to use and integrate suggest the following changes
 *  reflected in the changes made here:
 *  1. Changed the SmartApp and Device Handler to use the WebApp API to configure VirtualZoneControls, VirtualZoneSensors and VirtualRelaySensors
 *     from settings and configuration in the AlarmDecoder WebApp.
 *  2. Added child automation apps for linking ST device to VirtualZoneControl - i.e. link Garage Door Sensor to fault VirtualZoneControl 25 when open
 *     and an app to trigger ST event when wired zone state or relay changes - i.e. if Front Door Zone #3 faults (opens), Turn On ST light Switch
 *  3. Added keypad message to the UI
    4. Added state monitors for powerSource, Chime, Ready/Not Ready, Smoke, Low Battery, System Trouble
 *  5. Added buttons to change chime mode, show configuration, Add/update child devices
 *  6. Added relay state tiles for open/closed relays
 *  7. Changed armed states to use Smart Home Monitor standards for away, stay, off - plus added arming, disarming
 *  8. Changed state handling for Arm Away, Arm Stay, and Disarm to act more like radio buttons reflecting current state of the panel and SmartThings Home Monitor
 *  9. Added fourth Panic/Function button and allow custom labels to send <S1>,<S2>,<S3>,<S4> keys and message panel instructing to press 3x.
 *  10. Added web services endpoints for future AlarmDecoder network appliance to push notifications to the SmartThings app, or to update configuration.
 *  11. Attempted to make some changes to ensure a site with multiple alarm decoders would function properly from SmartApp to Device Handlers, this still needs work.
 *
 *  TODO LIST:
 *  1. Needs to be tested for DSC panels, I dont have one so can't do that
 *  2. Needs to be tested with wired relay expanders.
 *  3. Needs to be tested for hubs connected to networks with more than one AlarmDecoder (if that is a supported use case).
 *     One example might be one LAN with house and guest house, two panels, two alarm decoders but one SmartThings hub, or an office complex campus.
 */
import groovy.json.JsonSlurper;

preferences {
     section(name: "DevicePrefs", title: "Device Settings") {
        icon(title: "Select AlarmDecoder icon", required: false)
        input("api_key",    "password", title: "API Key",    label: "The key to access the REST API", description: "The key to access the REST API", required: false)
        input("user_code",  "number",   title: "Alarm Code", description: "The user code for the panel", required: false)
        input("panel_type", "enum",     title: "Panel Type", description: "Type of panel", options: ["ADEMCO", "DSC"], defaultValue: "ADEMCO", required: true)
     }
     /*** Future - perhaps API update to authenticate user/password to get API Key from WebApp
     section(name: "ADWebAppInfo", title: "WebApp Info") {
        input("username",  "text",     title: "WebApp User Name", label: "Enter User Name", 
                                       description: "Enter WebApp User Name", required: false)
        input("password",  "password", title: "WebApp Password",  description: "Enter Password", required: false)
     }
     ***/
     section(name: "ChildDevicePrefs", title: "Child Device & Automations Preferences") {
        input("create_keypad",   "bool", title: "Create a separate companion AlarmDecoder Keypad Device in Smartthings", 
              description: "Create a child keypad device that looks similar to the AlarmDecoder WebApp keypad",
              required: false, defaultValue: false)
        input("use_virtual_zones",   "bool", title: "Create Virtual Zone Controls (ADEMCO ONLY) to expose emulated zones for ST automations", 
              description: "Create VirtualZoneControls for zones IDs on an emulated expander enabled on AlarmDecoder that can also be set by Smartthings",
              required: false, defaultValue: false)
        input("use_physical_zones",  "bool", title: "Create Virtual Zone Sensors to expose physical & RF zones for ST automations", 
               description: "Create VirtualContactSensors for zone IDs for physical/RF zones that can not be changed by Smarttings",
               required: false, defaultValue: false)
        input(name: "defaultSensorToClosed", type: "bool", required: false, defaultValue: true, title: "Default Zone Controls & Sensors to closed?")
        input("use_relays",  "bool", title: "Create Virtual Relay Contact Sensors to expose pnysical/emulated relays for ST automations", 
               description: "Create Virtual Relays for device numbers on an emulated relay expander enabled on AlarmDecoder that can be changed by Smartthings",
               required: false, defaultValue: false)    
     }
     section(name: "LayoutPrefs", title: "Device UI Layout Preferences") {
            input("show_keypad",   "bool", title: "Show Keypad Numbers on the AlarmDecoder details page", 
              description: "Show the keypad keys device that looks similar to the AlarmDecoder WebApp keypad",
              required: false, defaultValue: false)
            input("show_fkeys",   "bool", title: "Show Special Function Keys (F1-F4) on the AlarmDecoder details page", 
              description: "Show the F1-F4 function keys, See label and delay settings for Function Keys below",
              required: false, defaultValue: false)
            input("panic1_label", "string", title: "Panic/Function Key #1 Label", description: "Label for special function key #1", defaultValue: "F1 - FIRE")
            input("panic2_label", "string", title: "Panic/Function Key #2 Label", description: "Label for special function key #2", defaultValue: "F2 - POLICE")
            input("panic3_label", "string", title: "Panic/Function Key #3 Label", description: "Label for special function key #3", defaultValue: "F3 - MEDICAL")
            input("panic4_label", "string", title: "Panic/Function Key #4 Label", description: "Label for special function key #4", defaultValue: "F4 - PROG/STAY")   
            input("panic1_delay", "number", title: "Panic/Function Key #1 Delay", description: "Delay for sending function key #1 (0-60)", defaultValue: 15 )
            input("panic2_delay", "number", title: "Panic/Function Key #2 Delay", description: "Delay for sending function key #2 (0-60)", defaultValue: 15 )
            input("panic3_delay", "number", title: "Panic/Function Key #4 Delay", description: "Delay for sending function key #3 (0-60)", defaultValue: 15) 
            input("panic4_delay", "number", title: "Panic/Function Key #4 Delay", description: "Delay for sending function key #4 (0-60)", defaultValue: 0) 
     }
    section (name: "Audio Notifications", title: "Select a device or Audio Notifications of Panel and Zone Events" ) {
            input("audioDevices", "capability.audioNotification", title: "Select Audio Notification Devices for Audio Notifications", multiple: true, required: false )
            input("musicPlayers", "capability.musicPlayer", title: "Select Music Player Devices for Audio Notifications", multiple: true, required: false )
            input("volume", title: "Set volume for playback of notifications", range: "0..100", multiple: false, required: false)
     }
} // prefs

metadata {  
    definition (name:        "Alarm Decoder Network Appliance", 
                namespace:   "alarmdecoder", 
                description: "Alarm Decoder Network Appliance Device Handler",
                author:      "Scott Petersen") 
        {
        capability "Polling"
        capability "Refresh"
        capability "Switch"             // Used to set Arm Stay mode for AlarmDecoder
        capability "Lock"               // Used to set Arm Away mode for AlarmDecoder
        capability "Alarm"              // 
        capability "smokeDetector"      // AlarmDecoder will show as smoke detector reporting panel status on smoke/fire detected
        capability "powerSource"        // CJ MOD - Power
        capability "Health Check"
        capability "Configuration"

        attribute "panel_state", "enum", ["loading", "armed_away", "armed_stay", "disarmed", "panicked", "alarming", "fire"]
        attribute "armed", "enum", ["away", "stay", "off", "arming", "disarming"]
        // attribute "armed_away", "enum", ["armed", "disarmed", "arming", "disarming", "unknown"] - Changed to only 1 armed state "armed"
        // attribute "armed_stay", "enum", ["armed", "disarmed", "arming", "disarming", "unknown"]

        attribute "zoneStatus1", "number"
        attribute "zoneStatus2", "number"
        attribute "zoneStatus3", "number"
        attribute "zoneStatus4", "number"
        attribute "zoneStatus5", "number"
        attribute "zoneStatus6", "number"
        attribute "zoneStatus7", "number"
        attribute "zoneStatus8", "number"
        attribute "zoneStatus9", "number"
        attribute "zoneStatus10", "number"
        attribute "zoneStatus11", "number"
        attribute "zoneStatus12", "number"
        attribute "relayOpenStatus1", "number"
        attribute "relayOpenStatus2", "number"
        attribute "relayOpenStatus3", "number"
        attribute "relayOpenStatus4", "number"
        attribute "relayOpenStatus5", "number"
        attribute "relayOpenStatus6", "number"
        attribute "relayClosedStatus1", "number"
        attribute "relayClosedStatus2", "number"
        attribute "relayClosedStatus3", "number"
        attribute "relayClosedStatus4", "number"
        attribute "relayClosedStatus5", "number"
        attribute "relayClosedStatus6", "number"
        
        // CJ MODS
        // attribute "smoke",       "enum", ["clear", "detected", "tested"]            // defined with smokeDetector Capability, use panel_fire_detected to populate
        // attribute "alarm",       "enum", ["both", "off", "siren", "strobe"]         // For ST Alarm Capability
        attribute "powerSource", "enum",       ["battery", "dc", "mains", "unknown"]   // For powerSource Capability
        attribute "alarmSystemStatus", "enum", ["away", "stay", "off", "unconfigured"] // Mirrors status of Smart Home Monitor location.alarmSystemStatus       
        attribute "chime",        "enum",      ["off", "on"]                           // For Chime on/off icon
        attribute "ready",        "enum",      ["ready", "not_ready"]                  // For Ready on/off icon
        attribute "batteryState", "enum",      ["ok", "low"]                           // Track Battery State
        attribute "systemState",  "enum",      ["ok", "trouble"]                       // System issue/trouble indicator
        attribute "keypadMsg",    "string"                                             // Tracks panel keypad message
        attribute "totalZoneFaults",  "number"
        attribute "totalOpenRelays",  "number"
        attribute "totalClosedRelays","number"
        
        attribute "accessToken", "string"                                               // SmartThings Web API Access Token
        attribute "endpointURL", "string"                                               // SmartThings Web API Endpoint URL for Svc Mgr
        attribute "alarmdecoder_keypad_address", "string"
        attribute "zone_list",   "string"
        attribute "zone_id_list", "enum", []
        attribute "zone_nbr_list", "enum", []
        attribute "zone_count",  "string"
        attribute "emulate_lrr", "boolean"
        attribute "zone_expander_1", "boolean"
        attribute "zone_expander_2", "boolean"
        attribute "zone_expander_3", "boolean"
        attribute "zone_expander_4", "boolean"
        attribute "zone_expander_5", "boolean"
        attribute "relay_expander_1", "boolean"
        attribute "relay_expander_2", "boolean"
        attribute "relay_expander_3", "boolean"
        attribute "relay_expander_4", "boolean"
        attribute "readyTileLabel",   "string"
        attribute "ad_layout", "enum", [ "device", "keypad" ]
        
        // END CJ MOD

        command "arm_away"
        command "arm_stay"
        command "disarm"
        command "refresh"
        command "poll"
        command "ping"
        command "chime_cmd"  
        command "ready_cmd"
        command "test_cmd"
        command "update_devices_cmd"
        command "show_config_cmd"
        command "set_access_token"
        command "layout_toggle"
         
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {
   
        multiAttributeTile(name: "status", type: "generic", width: 6, height: 4) {
            tileAttribute("device.panel_state", key: "PRIMARY_CONTROL") {
                attributeState "away",     label: 'Armed (Away)', icon: "st.security.alarm.on", backgroundColor: "#00a0dc" // "#ffa81e"
                attributeState "stay",     label: 'Armed (Stay)', icon: "st.security.alarm.on", backgroundColor: "#00a0dc" // "#ff4000"
                attributeState "disarmed", label: 'Disarmed',     icon: "st.security.alarm.off", backgroundColor: "#79b821", defaultState: true
                attributeState "off",      label: 'Disarmed',     icon: "st.security.alarm.off", backgroundColor: "#79b821" 
                attributeState "panicked", label: 'Panicked!',    icon: "st.security.alarm.on",  backgroundColor: "#e86d13"
                attributeState "alarming", label: 'Alarming!',    icon: "st.security.alarm.on",      backgroundColor: "#ff4000"
                attributeState "fire",     label: 'Fire!',        icon: "st.contact.contact.closed", backgroundColor: "#ff0000"
                attributeState "loading",  label: 'Please Wait, Loading...',     icon: "st.security.alarm.off", backgroundColor: "#e86d13"
            }
            tileAttribute("device.keypadMsg", key: "SECONDARY_CONTROL" ) {
                attributeState "keypadMsg", label: '${currentValue}', defaultState: true
            }
        }

   
        standardTile("refresh1", "device.refresh", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        valueTile("powerSource", "device.powerSource", decoration: "flat", width: 1, height: 1) {
                state "mains",   icon: "st.switches.switch.on", label: "AC",      defaultState: true
                state "dc",      icon: "st.switches.switch.on", label: "DC"
                state "battery", icon: "st.switches.switch.on", label: "BATT"
                state "unknown", icon: "st.switches.switch.on", label: "Unknown"
        } 
        valueTile("ready", "device.ready", decoration: "flat", width:1, height:1 ) {
                state "ready",      label: "READY",        defaultState: true  // backgroundColor: "#79b821",
                state "not_ready",  label: "NOT READY",  backgroundColor: "#FF0000"
        } 
        valueTile("chime", "device.chime", decoration: "flat", width:1, height:1 ) {
                state "on",    label: "Chime ON",      backgroundColor: "#00a0dc"  // icon: "st.custom.sonos.unmuted", 
                state "off",   label: "Chime OFF",     backgroundColor: "#ffffff", defaultState: true // icon: "st.custom.sonos.muted",
        } 
        valueTile("smoke", "device.smoke", decoration: "flat", width:1, height:1) {
                state "clear",    icon: "st.alarm.smoke.clear",    label: "CLEAR",   backgroundColor: "#ffffff", defaultState: true
                state "detected",                                  label: "SMOKE",   backgroundColor: "#ff0000"
                state "tested",   icon: "st.alarm.smoke.tested",   label: "Tested",  backgroundColor: "#ff4000"
        } 
        valueTile("battery", "device.batteryState", decoration: "flat", width: 1, height: 1) {
                state "ok",  label: 'BATT OK',  backgroundColor: "#ffffff", defaultState: true
                state "low", label: 'BATT LOW', backgroundColor: "#ff0000", defaultState: true
        }
        valueTile("system", "device.systemState", decoration: "flat", width: 1, height: 1) {
                state "ok",      label: 'PANEL OK',    backgroundColor: "#ffffff", defaultState: true
                state "trouble", label: 'PANEL ERR', backgroundColor: "#ff0000", defaultState: true
        }
        
        standardTile("arm_away", "device.armed", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "away",       action:"lock.off",  icon:"st.security.alarm.on",      label: "ARMED (AWAY)", nextState: "disarming", backgroundColor:  "#00a0dc"
            state "stay",                           icon:"st.security.alarm.off",     label: "ARM (AWAY)", nextState: "arming", backgroundColor: "#ffffff"
            state "off",        action:"lock.on",   icon:"st.security.alarm.off",     label: "ARM (AWAY)", nextState: "arming", backgroundColor: "#ffffff"
            state "arming",                         icon:"st.security.alarm.partial", label: "ARMING (AWAY)", nextState: "armed", backgroundColor:  "#f0f0f0"
            state "disarming",                      icon:"st.security.alarm.partial", label: "DISARMING", nextState: "disarmed", backgroundColor:  "#f0f0f0"
        }

        standardTile("arm_stay", "device.armed", inactiveLabel:  false, decoration: "flat", width: 2, height: 2) {
            state "stay",       action:"switch.off", icon:"st.security.alarm.on",  label: "ARMED (STAY)", nextState: "disarming", backgroundColor: "#00a0dc"
            state "away",                            icon:"st.Home.home4",         label: "ARM (STAY)",   nextState: "arming",    backgroundColor: "#ffffff"
            state "off",        action:"switch.on",  icon:"st.Home.home4",         label: "ARM (STAY)",   nextState: "arming",    backgroundColor: "#ffffff"
            state "arming",                          icon:"st.security.alarm.off", label: "ARMING (STAY)",nextState: "armed",     backgroundColor: "#f0f0f0"
            state "disarming",                       icon:"st.Home.home4",         label: "DISARMING",    nextState: "disarmed",  backgroundColor: "#f0f0f0"
        }
        standardTile("disarm", "device.armed", inactiveLabel: false, decoration: "flat", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true ) {
            state "away",        action:"disarm", icon:"st.security.alarm.off", label: "DISARM",    nextState: "off", backgroundColor: "#ffffff"
            state "stay",        action:"disarm", icon:"st.security.alarm.off", label: "DISARM",    nextState: "off", backgroundColor: "#ffffff"
            state "off",         action:"disarm", icon:"st.security.alarm.off", label: "DISARMED",  backgroundColor: "#00a0dc"
        }
 
 /**
        standardTile("autorefresh", "device.autoRefresh", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "on",      action:"stopAutoRefresh", label:"AUTO", icon:"st.secondary.refresh", backgroundColor: "#00a0dc"
            state "off",     action:"stopAutoRefresh",  label:"AUTO", icon:"st.secondary.refresh", backgroundColor: "#ffffff", defaultState: "true"
            state "unknown",                            label:"AUTO", icon:"st.secondary.refresh", backgroundColor: "#cccccc"
        }
 ***/
 
        standardTile("chime_tile", "device.chime", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {  // icon: "st.custom.sonos.unmuted",
            state "on",         action: "chime_cmd", label: "Set Chime Off", backgroundColor: "#ffffff" // icon: "st.custom.sonos.unmuted",  "#00a0dc" nextState: "send_off",
            state "off",        action: "chime_cmd", label: "Set Chime On",  backgroundColor: "#ffffff" // icon: "st.custom.sonos.muted", nextState: "send_on", 
            //state "send_off",   action: "chime_cmd", label: "Sending..",     nextState: "off",        backgroundColor: "#ffffff" // icon: "st.custom.sonos.muted", "#a0a0a0"
            //state "send_on",    action: "chime_cmd", label: "Sending..",     nextState: "on",         backgroundColor: "#ffffff" // icon: "st.custom.sonos.muted", 
        }  
        standardTile("test_tile", "device.test", inactiveLabel: false, decoration: "flat",width: 2, height: 2) {
            state "on",         action: "check_panel_status",   nextState: "off",   icon: "st.secondary.test",   backgroundColor: "#e86d13" // icon: "st.custom.sonos.unmuted",
            state "off",        action: "check_panel_status",   nextState: "on",  icon: "st.secondary.test",   backgroundColor: "#ffffff" // icon: "st.custom.sonos.muted", 
        } 
        standardTile("status_tile", "device.readyTileLabel", inactiveLabel: false, decoration: "flat",width: 2, height: 1) { // icon: "st.secondary.test", // "#e86d13" 
            state "on",         action: "check_panel_status",   label: '${currentValue}', nextState: "off",    backgroundColor: "#ffffff"  // icon: "st.custom.sonos.unmuted",
            state "off",        action: "check_panel_status",   label: '${currentValue}', nextState: "on",     backgroundColor: "#ffffff" // icon: "st.custom.sonos.muted", 
        }
        standardTile("layout_tile", "device.ad_layout", inactiveLabel: false, decoration: "flat",width: 2, height: 1) { // icon: "st.secondary.test", // "#e86d13" 
            state "device",  action: "layout_toggle",   label: "Show Keypad",   backgroundColor: "#ffffff", defaultState: true // icon: "st.custom.sonos.unmuted",
            state "keypad",  action: "layout_toggle",   label: "Show Device",   backgroundColor: "#ffffff"                     // icon: "st.custom.sonos.muted", 
        } 
        standardTile("ready_tile", "device.ready", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "on",         action: "ready_cmd", label: "READY",     nextState: "off",  icon: "st.home.home7", backgroundColor:     "#00a0dc" // icon: "st.custom.sonos.unmuted",
            state "off",        action: "ready_cmd", label: "READY",     nextState: "on",   icon: "st.home.home7",   backgroundColor:   "#ffffff" // icon: "st.custom.sonos.muted", 
        } 
        standardTile("config_tile", "device.configMsg", inactiveLabel: false, decoration: "flat",width: 2, height: 2) {
            state "default",         action: "show_config_cmd",   label: "Show Config", nextState: "off",     backgroundColor: "#ffffff" // icon: "st.custom.sonos.unmuted",
        } 
        standardTile("update_devices_tile", "device.configMsg", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default",  action: "update_devices_cmd", label: "Update Devices",     backgroundColor:     "#ffffff" // icon: "st.custom.sonos.unmuted",
        }
        standardTile("help_tile", "device.configMsg", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default",    action: "help_cmd", label: "Help",  backgroundColor:   "#ffffff" // icon: "st.custom.sonos.unmuted",
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", action:"refresh.refresh", label: "Refresh", icon:"st.secondary.refresh"
        }

        standardTile("big_refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", action:"refresh.refresh", label: "Refresh", icon:"st.secondary.refresh"
        }
        
        valueTile("zone_fault_label", "device.refresh", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
            state "default", label: "Zone Faults", defaultState: true
        }
        
        valueTile("faulted_zone_count", "device.totalZoneFaults", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label: 'Total: ${currentValue}', defaultState: true
        }
        
       valueTile("relays_closed_label", "device.refresh", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
            state "default", label: "Closed Relays", defaultState: true
        }
        
       valueTile("closed_relay_count", "device.totalClosedRelays", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label: 'Total: ${currentValue}', defaultState: true
        }
        
        valueTile("relays_open_label", "device.refresh", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
            state "default", label: "Open Relays", defaultState: true
        }
        
        valueTile("open_relay_count", "device.totalOpenRelays", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label: 'Total: ${currentValue}', defaultState: true
        }
               
        valueTile("zoneStatus1", "device.zoneStatus1", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus2", "device.zoneStatus2", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus3", "device.zoneStatus3", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus4", "device.zoneStatus4", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus5", "device.zoneStatus5", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus6", "device.zoneStatus6", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus7", "device.zoneStatus7", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus8", "device.zoneStatus8", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus9", "device.zoneStatus9", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus10", "device.zoneStatus10", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus11", "device.zoneStatus11", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("zoneStatus12", "device.zoneStatus12", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }
        
        valueTile("relayOpenStatus1", "device.relayOpenStatus1", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("relayOpenStatus2", "device.relayOpenStatus2", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }
        valueTile("relayOpenStatus3", "device.relayOpenStatus3", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("relayOpenStatus4", "device.relayOpenStatus4", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }
        valueTile("relayOpenStatus5", "device.relayOpenStatus5", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("relayOpenStatus6", "device.relayOpenStatus6", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }    
        
           valueTile("relayClosedStatus1", "device.relayClosedStatus1", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("relayClosedStatus2", "device.relayClosedStatus2", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }
        valueTile("relayClosedStatus3", "device.relayClosedStatus3", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("relayClosedStatus4", "device.relayClosedStatus4", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }
        valueTile("relayClosedStatus5", "device.relayClosedStatus5", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        valueTile("relayClosedStatus6", "device.relayClosedStatus6", inactiveLabel: false, width: 1, height: 1) {
            state "default", icon:"", label: '${currentValue}', backgroundColors: [
                [value: 0, color: "#ffffff"],
                [value: 1, color: "#ff0000"],
                [value: 99, color: "#ff0000"]
            ]
        }

        // childDeviceTile( tilename, componentName (must match dev), width: xx, height: xx, childTileName: string (must match child dev)
        //**
        childDeviceTile( "KPM", "KeypadLayout", width: 6, height: 2, childTileName: "keypadMsg" )
        childDeviceTile( "K1",  "KeypadLayout", width: 2, height: 1, childTileName: "K1" )
        childDeviceTile( "K2",  "KeypadLayout", width: 2, height: 1, childTileName: "K2" )
        childDeviceTile( "K3",  "KeypadLayout", width: 2, height: 1, childTileName: "K3" )
        childDeviceTile( "K4",  "KeypadLayout", width: 2, height: 1, childTileName: "K4" )
        childDeviceTile( "K5",  "KeypadLayout", width: 2, height: 1, childTileName: "K5" )
        childDeviceTile( "K6",  "KeypadLayout", width: 2, height: 1, childTileName: "K6" )
        childDeviceTile( "K7",  "KeypadLayout", width: 2, height: 1, childTileName: "K7" )
        childDeviceTile( "K8",  "KeypadLayout", width: 2, height: 1, childTileName: "K8" )
        childDeviceTile( "K9",  "KeypadLayout", width: 2, height: 1, childTileName: "K9" )
        childDeviceTile( "K*",  "KeypadLayout", width: 2, height: 1, childTileName: "K*" )
        childDeviceTile( "K0",  "KeypadLayout", width: 2, height: 1, childTileName: "K0" )
        childDeviceTile( "K#",  "KeypadLayout", width: 2, height: 1, childTileName: "K#" )
       
        childDeviceTile( "F1",    "F1", width: 3, height: 1, childTileName: "key" )
        childDeviceTile( "F2",    "F2", width: 3, height: 1, childTileName: "key" )
        childDeviceTile( "F3",    "F3", width: 3, height: 1, childTileName: "key" )
        childDeviceTile( "F4",    "F4", width: 3, height: 1, childTileName: "key" )
        /***
        childDeviceTile( "C1",    "F1", width: 1, height: 1, childTileName: "counter" )
        childDeviceTile( "C2",    "F2", width: 1, height: 1, childTileName: "counter" )
        childDeviceTile( "C3",    "F3", width: 1, height: 1, childTileName: "counter" )
        childDeviceTile( "C4",    "F4", width: 1, height: 1, childTileName: "counter" )
        ***/
        
        main(["status"] )
        details(["status", 
                 "ready", "chime",  "powerSource", "battery", "smoke", "system",
                 "chime_tile",  "layout_tile", "refresh",  // "status_tile",
                 "arm_away",    "arm_stay",   "disarm", 
                 //"F1", "C1", "F2", "C2",
                 //"F3", "C3", "F4", "C4",
                 /**
                 "KPM",
                 "K1", "K2", "K3",
                 "K4", "K5", "K6",
                 "K7", "K8", "K9",
                 "K*", "K0", "K#",
                 "F1", "F2",
                 "F3", "F4",
                 ***/
                 "zone_fault_label", "faulted_zone_count", 
                 "zoneStatus1", "zoneStatus2", "zoneStatus3", "zoneStatus4", "zoneStatus5", "zoneStatus6", 
                 "zoneStatus7", "zoneStatus8", "zoneStatus9", "zoneStatus10", "zoneStatus11", "zoneStatus12", 
                 "relays_open_label", "open_relay_count",
                 "relayOpenStatus1","relayOpenStatus2", "relayOpenStatus3", "relayOpenStatus4","relayOpenStatus5", "relayOpenStatus6",
                 "relays_closed_label", "closed_relay_count",
                 "relayClosedStatus1","relayClosedStatus2", "relayClosedStatus3", "relayClosedStatus4","relayClosedStatus5", "relayClosedStatus6",
                 "config_tile", "update_devices_tile", "big_refresh"
                 ])
    }
}

/*** Standard Device Callbacks ***/


def initialize() {
    log.debug("--- handler.initialize")
    
    state.max_zone_status_tiles  = 12
    state.max_open_relay_tiles   = 6
    state.max_closed_relay_tiles = 6  
    
    state.faulted_zones  = []    
    
    state.zone_expander_address_list_1 = [ 9,10,11,12,13,14,15,16  ] 
    state.zone_expander_address_list_2 = [ 17,18,19,20,21,22,23,24 ] 
    state.zone_expander_address_list_3 = [ 25,26,27,28,29,30,31,32 ] 
    state.zone_expander_address_list_4 = [ 33,34,35,36,37,38,39,40 ] 
    state.zone_expander_address_list_5 = [ 41,42,43,44,45,46,47,48 ] 
       
    // set Ready (Ademco) or Status (DSC) button label
    if(settings.panel_type.equals("ADEMCO"))
       sendEvent(name: "readyTileLabel", value: "Ready Key (#)" )
    else if (settings.panel_type.equals("DSC"))
        sendEvent(name: "readyTileLabel", value: "Status (*1)" )
        else 
          sendEvent(name: "readyTileLabel",  value: "Ready/Status")
          
        
    // Set default message for keypad and function key prompts
    if(settings.api_key==null)
       sendEvent(name: "keypadMsg",  value: "Enter APIKey, User Code and Panel Type in Settings!", isStateChange: true, displayed: false)   
   
        
    log.debug("--- handler.initialize END")
}


def installed() {
    log.trace("--- handler.installed")
    
    // Device was created by parent.installed()
    //parent.installed()
    
    initialize()
    
    state.layout = "device"
    
    // blank out the zone status tiles
    for (def i = 1; i <= state.max_zone_status_tiles; i++)
        sendEvent(name: "zoneStatus${i}", value: "", displayed: false)
        
    // blank out the open relay status tiles
    for (def i = 1; i <= state.max_open_relay_tiles; i++)
        sendEvent(name: "relayOpenStatus${i}", value: "", displayed: false)
    
    // blank out the closed relay status tiles
    for (def i = 1; i <= state.max_open_relay_tiles; i++)
        sendEvent(name: "relayOpenStatus${i}", value: "", displayed: false)
    
    // Dont update child devices until settings are updated
    state.updateDevices=false
    
    log.debug("--- handler.installed END")
}

def configure() {
    // Called when device is created, assigned device handler
    // Normally this would be to send special z-wave configuration commands, etc.
    // Here we will just grab the AD configuration from the web  API assuming the key has been entered.
    // updated()
}


def updated() {
    log.trace ("--- handler.updated")
    
    // initialize key labels and state variables
    initialize()
  
    def t  = device.type
    log.trace("AlarmDecoder device type=${t}")
    
    // Once the user has entered the API key we will now try to create the child devices from the list 
    // of zones provided by the Web API by setting updateDevices to true.
    //if(settings.use_virtual_zones || settings.use_physical_zones  || settings.use_relays)
        state.updateDevices=true
    
    // gets the AlarmDecoder config info then the zone lists and relay lists
    // will then update the child devices depending on settings.
      
    if(settings.api_key!=null) {

      // ApiKey is present so now send ST Access Key and Path to ad2web
      //sendHubCommand( send_st_settings_request() )
      request=update_ad2web()
    //  sendHubCommand( request )
      
      def dni   = device.deviceNetworkId
      state.dni = device.deviceNetworkId
      
      //if(settings.create_keypad)
      if(state.layout=="keypad")
         addKeypadDevice()
      else
         deleteKeypadDevice()
         
      if(settings.show_fkeys)
         addFunctionKeys()
      else
         deleteFunctionKeys()
         
      // We have an AlarmDecoder API Key so get the config and zone list
      //sendHubCommand( get_ad_config() )
         
    } else
        parent.Alert("The AlarmDecoder WebApp API Key must be entered in settings for the SmartThings Integration to function!")
    log.debug "parent.selectedDevices=${parent.selectedDevices}, parent.devices=${parent.devices}"
    
} // updated

def uninstalled() {
    log.trace "--- handler.uninstalled for ${device.displayName}"
    
    // Explicitly remove child devices from the device handler...
    /***
    def devices = getChildDevices()
    log.debug "--handler deleting child devices ${devices}"
        
    devices.each{ d ->
       log.debug "--handler deleting child device ${d}"
       if(d!=null) {
         log.debug "--handler deleting child device networkid ${d.deviceNetworkId}"
         log.debug "--handler deleting child device id ${d.getId()}"
         //deleteChildDevice(d.deviceNetworkId)
         }
    }
    ****/
    
    // Make sure the service manager removes this device from its child device list
    // and removes any sibling devices for keypad and zone handling...
    // parent.uninstallAlarmDecoder(device.deviceNetworkId)
}

/*********** Parse & Callback Handlers **********/

// parse events into attributes
def parse(String description) {
    //log.trace("-- alarmdecoder.parse description=${description}")
    
    String body = description.decodeBase64()
    log.trace("-- alarmdecoder.body=${body}")
        
    def events = []
    def event = parseEventMessage(description)
    
    // HTTP
    if (event?.body && event?.headers) {
        def slurper = new JsonSlurper()
        String bodyText = new String(event.body.decodeBase64())
        
        //log.debug("-- alarmdecoder.parse.bodyText=${bodyText}")
        def result = slurper.parseText(bodyText)

        // Debug: Log the http response body
        log.info("--- handler.parse: http result=${result}")
        
        // Update alarm system state.
        if (result.error != null) {
            log.error("Error accessing AlarmDecoder API: ${result.error.code} - ${result.error.message}")
            if(result.error.code==7100)
               sendEvent(name: "keypadMsg", value: "Error ${result.error.code}: Not Authorized, Enter API Key, User Code and Panel Type in Settings!" )
            else
               sendEvent(name: "keypadMsg", value: "Error ${result.error.code}: ${result.error.message}" )
        } else if (result.panel_armed != null)
                  update_state(result).each { e-> events << e }
             else if(result.zones != null)
                      events=update_zone_lists(result)
                  else if(result.address != null)
                              events=update_config(result)                                     
    }

    // Log events for debug before returning them
    //log.debug("--- handler.parse: resulting events=${events}")

    return events
}

void ad2WebHandler(physicalgraph.device.HubResponse hubResponse) {
    log.debug "--handler ad2webHandler called."
    def headers = hubResponse.headers
    def status  = hubResponse.status
    def body    = hubResponse.body
    def xml     = hubResponse.xml
    def json    = hubResponse.json
    log.debug "ad2WebHandler: headers=${headers}"
    log.debug "ad2WebHandler: headers=${status}"
    log.debug "ad2WebHandler: body=${body}"
    log.debug "ad2WebHandler: body=${xml}"
    log.debug "ad2WebHandler: json=${json}"
    log.debug "ad2WEbHandler: state.networkAddr=${state.networkAddr}"
}

void configHandler(physicalgraph.device.HubResponse hubResponse) {
    log.debug "--handler configHandler called."
    def headers = hubResponse.headers
    def status  = hubResponse.status
    def body    = hubResponse.body
    def xml     = hubResponse.xml
    def json    = hubResponse.json
    log.debug "configHandler: headers=${headers}"
    log.debug "configHandler: headers=${status}"
    log.debug "configHandler: body=${body}"
    log.debug "configHandler: body=${xml}"
    log.debug "configHandler: json=${json}"
    log.debug "configHandler: state.networkAddr=${state.networkAddr}"
}

/*** Capabilities ***/

    
def on() {
    log.trace("--- switch.on (arm stay)")

    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )
    
    sendEvent(name: "armed", value: "stay")    // NOTE: Not sure if it's the best way to accomplish it,
                                                //       but solves the weird tile state issues I was having.                                          
    return delayBetween([
        arm_stay(),
        refresh()
    ], 2000)
}

def off() {
    log.trace("--- switch.off (disarm)")
    
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )

    sendEvent(name: "armed", value: "off") // NOTE: Not sure if it's the best way to accomplish it,
                                           //       but solves the weird tile state issues I was having.
                                           
    // NOTE: Need to send disarm keys for this and for unlock!!
    def code = _get_user_code()
    
    def cmd = send_keys("${code}1")
                                            
    return delayBetween([
        disarm(),
        refresh()
    ], 2000)
}

def strobe() {
    log.trace("--- alarm.strobe, do nothing")
}

def siren() {
    log.trace("--- alarm.siren, do nothing")
}

def both() {
    log.trace("--- alarm.both (panic)")

    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )
    
    state.panic_started = null;

    return delayBetween([
        panic(),
        refresh()
    ], 1000)
}
    
def lock() {
    
    log.trace("--- lock.lock (arm away)")
    
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )

    sendEvent(name: "armed", value: "away")    // NOTE: Not sure if it's the best way to accomplish it,
                                               //       but solves the weird tile state issues I was having.
                                         
    return delayBetween([
        arm_away(),
        refresh()
    ], 2000)
}

def unlock() {
    log.trace("--- lock.unlock (disarm)")
    
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )
    
    sendEvent(name: "armed", value: "off") // NOTE: Not sure if it's the best way to accomplish it,
                                                //       but solves the weird tile state issues I was having.
    return delayBetween([
        disarm(),
        refresh()
    ], 2000)
}

def chime_cmd() {
    log.trace("--- chime_cmd")
    
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )
    
    return delayBetween([
        send_chime_keys(),
        refresh()
    ], 2000)
}

def poll() {
    log.trace("--- handler.poll")
    // Should polling be handled in the service manager?
    refresh() //parent.refreshAllAlarmDecoders()
}

def ping() {
    log.trace("--- handler.ping")
    refresh()
    }

/********* Commands ************/

def layout_toggle() {
    if(state.layout==null)
       state.layout="device"
    if(state.layout=="keypad") {
       state.layout="device"
    } else 
       state.layout="keypad"
    log.debug "state.layout=${layout}"
    updated()
}

def show_config_cmd() {
    log.trace("--- show_config_cmd")
    
    def events = []
    
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )
    
    def msg1 = "Address: ${state.address}\nAddress Mask: ${state.address_mask}\nConfig Bits: ${state.config_bits}\nEmulate LRR: ${state.emulate_lrr}\n\n"
    def msg2 = "Emulate Relay1: ${state.emulate_relay[0]}\nEmulate Relay2: ${state.emulate_relay[1]}\nEmulate Relay3: ${state.emulate_relay[2]}\nEmulate Relay4: ${state.emulate_relay[3]}\n\n"
    def msg3 =
    "Emulate Zone1: ${state.emulate_zone[0]}\nEmulate Zone2: ${state.emulate_zone[1]}\nEmulate Zone3: ${state.emulate_zone[2]}\nEmulate Zone4: ${state.emulate_zone[3]}\nEmulate Zone5: ${state.emulate_zone[4]}\n\n"

    // def msg3 = "${state.zone_list}"
    
    def config_msg = msg1 + msg2 + msg3 
    
    def v_msg = ""
    state.virtual_zone_list.each { z->
       v_msg += "Zone: ${z.zone_id} Name: ${z.name}\n"
       }     
    if(v_msg.equals(""))
       v_msg = "No Virtual Zones Found.\n\n" 
    else {
           v_msg = "Virtual Zones\n" + v_msg
           v_msg += "\n"
           }
       
    def p_msg = ""
    state.physical_zone_list.each { z->
       p_msg += "Zone: ${z.zone_id} Name: ${z.name}\n"
       }
    if(p_msg.equals(""))
       p_msg = "No Physical Zones Found.\n\n"    
    else {
           p_msg = "Physical Zones\n" + p_msg
           p_msg += "\n"
           }
       
    def zone_msg = v_msg + p_msg
    
    // show relay info
    
    def relay_msg = ""
    state.relay_list.each { r->
       def state="unknown"
       if(r.value==1||r.value=="1")
          state="closed"
       else
          state="open"
       relay_msg += "Relay address: ${r.address} channel: ${r.channel} state: ${state}\n"
       }
    if(relay_msg.equals(""))
       relay_msg = "No Relays Found.\n\n"    
    else {
           relay_msg = "Relays\n" + relay_msg
           relay_msg += "\n"
           }
       
    //log.trace(config_msg)
    
    // Or send-message event can be used
    parent.Notify(config_msg)
    parent.Notify(zone_msg)
    parent.Notify(relay_msg)

}

def update_devices_cmd() {
    log.trace("--- update_devices_cmd")
    
    state.updateDevices=true
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )
    updated()
}
   

def refresh() {
    log.trace("--- handler.refresh")
    
    def urn = getDataValue("urn")
    def apikey = _get_api_key()
      
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false)
       
    // Request update to AD state - it will also update the relay info in update_state handler
    return hub_http_get(urn, "/api/v1/alarmdecoder?apikey=${apikey}") 
}

def disarm() {
    log.trace("--- disarm")
    
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )
    // off()
    // unlock()
    return send_disarm_keys()
}

def send_disarm_keys() {
    log.trace("--- send_disarm_keys")

    def user_code = _get_user_code()
    def keys = ""

    if (settings.panel_type == "ADEMCO")
        keys = "${user_code}1"
    else if (settings.panel_type == "DSC")
        keys = "${user_code}"
    else
        log.warn("--- disarm: unknown panel_type.")
    
    return send_keys(keys)
}

def arm_away() {
    log.trace("--- arm_away")

    def user_code = _get_user_code()
    def keys = ""
    
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )

    if (settings.panel_type == "ADEMCO")
        keys = "${user_code}2"
    else if (settings.panel_type == "DSC")
        keys = "<S5>"
    else
        log.warn("--- arm_away: unknown panel_type.")

    return send_keys(keys)
}

def arm_stay() {
    log.trace("--- arm_stay")

    def user_code = _get_user_code()
    def keys = ""
    
    sendEvent(name: "keypadMsg",  value: "Sending...", isStateChange: true, displayed: false )
    
    if (settings.panel_type == "ADEMCO")
        keys = "${user_code}3"
    else if (settings.panel_type == "DSC")
        keys = "<S4>"
    else
        log.warn("--- arm_stay: unknown panel_type.")

    return send_keys(keys)
}


def send_chime_keys() {
    log.trace("--- chime")

    def user_code = _get_user_code()
    def keys = ""

    if (settings.panel_type == "ADEMCO")
        keys = "${user_code}9"
    else if (settings.panel_type == "DSC")
        keys = "<S6>"
    else
        log.warn("--- send_chime_keys: unknown panel_type.")

    return send_keys(keys)

}


/***********  Parse Handlers for State, Config, Zones - Business Logic ***********/

def set_access_token(token, url) {
    log.debug "--handler.set_access_token"
    state.access_token = token
    state.endpoint_url = url
    sendEvent(name: "accessToken",  value: token)
    sendEvent(name: "endpointURL",  value: url)
    updateDataValue("access_token", token)
    updateDataValue("endpoint_url", url)
    
    // Must update ad2web with new access token & url also
    // update_ad2web
}

def update_config( response ) {
      
    //parse configuration rest response
    if(response.address==null)
       return
      
    log.trace(" --update_config response=${response}")
      
    def events        = []
    def address       = response.address
    def address_mask  = response.address_mask
    def config_bits   = response.config_bits
    def deduplicate   = response.deduplicate
    def emulate_lrr   = response.emulate_lrr
    def emulate_relay = response.emulate_relay
    def emulate_zone  = response.emulate_zone
    
    state.address       = address
    state.address_mask  = address_mask
    state.config_bits   = config_bits
    state.emulate_lrr   = emulate_lrr
    state.emulate_relay = emulate_relay
    state.emulate_zone  = emulate_zone
    
    events << createEvent(name: "alarmdecoder_keypad_address",  value: address, isStateChange: true, displayed: true)
    events << createEvent(name: "emulate_lrr",  value: emulate_lrr, isStateChange: true, displayed: true)
    
    def int i = 0
    emulate_zone.each { expander ->
       i++
       events << createEvent(name: "zone_expander_${i}",  value: expander, isStateChange: true, displayed: true)
    }
    
    i = 0
    emulate_relay.each { expander ->
       i++
       events << createEvent(name: "relay_expander_${i}",  value: expander, isStateChange: true, displayed: true)
    }
    
    sendHubCommand( get_zone_list() )
    
    return events
    
} // update_config

def update_zone_lists( response ) {
      
    //response.zones is an arrayList
    if(response.zones==null)
      return
      
    log.trace(" --update_zone_lists")
      
    def urn = getDataValue("urn")
    
    def zone_list      = response.zones
    def zone_nbr_list  = []
    def events         = []   
    def zone_count     = 0
    
    // Sort the Zone List
    zone_list.sort( { m1, m2 -> m1.zone_id <=> m2.zone_id } )
    
    // Update the zone state lists
    if(state.zone_list==null) {
       state.zones_changed=true
       }
    else if(zone_list.equals(state.zone_list))
               state.zones_changed = false
         else
               state.zones_changed = true   

    // Verbose debug zone list content
    // log.debug("update_zone_lists zoneList=${zone_list}")
   
    // Determine which zones are new adds or deleted zones
    if(state.zone_list==null)
       state.zone_list=[]
    if(state.old_zone_list==null)
       state.old_zone_list=[]
    //log.debug "MINUS state.zone_list=${state.zone_list}"
    //log.debug "MINUS state.old_zone_list=${state.old_zone_list}"
    if(state.zone_list != null) {
      state.old_zone_list     = state.zone_list                       // move current to old  ex. old 5, 6, 7   new 5,6,8  
      state.deleted_zone_list = state.old_zone_list.minus(zone_list)  // old list minus current ex. old 5, 6, 7 new 5,6,8 deleted is 7
      state.new_zone_list     = zone_list.minus(state.old_zone_list)  // current 5,6,8 minus old 5,6,7 new is 8
      state.zone_list         = zone_list
      } else {
                state.zone_list         = []
                state.deleted_zone_list = []
                state.new_zone_list     = []
                }

    def tmp_nbr_list = []
    zone_count          = 0
    
    zone_list.each { z ->
        tmp_nbr_list.add(z.zone_id)
        zone_count++
    }
    
    state.zone_nbr_list = tmp_nbr_list
    state.zone_count    = zone_count  
    
    def physical_zone_list = []
    def virtual_zone_list  = []
    
    // Determine physical_zone_list and virtual_zone_list  
    zone_list.each { zone -> 
       if(state.emulate_zone[0] && state.zone_expander_address_list_1.contains(zone.zone_id) )
             virtual_zone_list.add(zone)
       if(state.emulate_zone[1] && state.zone_expander_address_list_2.contains(zone.zone_id) )
             virtual_zone_list.add(zone)
       if(state.emulate_zone[2] && state.zone_expander_address_list_3.contains(zone.zone_id) )
             virtual_zone_list.add(zone)
       if(state.emulate_zone[3] && state.zone_expander_address_list_4.contains(zone.zone_id) )
             virtual_zone_list.add(zone)             
       if(state.emulate_zone[4] && state.zone_expander_address_list_5.contains(zone.zone_id) )
             virtual_zone_list.add(zone)  
    } // each
    
    physical_zone_list = zone_list.minus(virtual_zone_list)

    // Determine new adds that are virtual/emulated zones
    if(state.old_virtual_zone_list != null) {
       state.new_virtual_zone_list     = virtual_zone_list.minus(state.old_virtual_zone_list)
       state.deleted_virtual_zone_list = state.old_virtual_zone_list.minus(virtual_zone_list)
       }
    if(state.old_physical_zone_list != null) {
       state.new_physical_zone_list  = physical_zone_list.minus(state.old_phyisical_zone_list)
       state.deleted_virtual_zone_list = state.old_physical_zone_list.minus(physical_zone_list)
      }
      
    // Persist virtual and physical zone lists
    state.virtual_zone_list      = virtual_zone_list
    state.old_virtual_zone_list  = state.virtual_zone_list   
    state.old_physical_zone_list = state.physical_zone_list  
    state.new_virtual_zone_list  = state.virtual_zone_list.minus(state.old_virtual_zone_list)
    // Persist physical zone list
    state.physical_zone_list  = physical_zone_list
    state.virtual_zone_count  = virtual_zone_list.size()
    state.physical_zone_count = virtual_zone_list.size()
    
    state.virtual_zone_nbr_list = tmp_nbr_list
    log.trace(" -- physical_zone_list = ${state.physical_zone_list}")
    log.trace(" -- virtual_zone_list = ${state.virtual_zone_list}")
        
    log.trace(" -- zone_nbr_list = ${state.zone_nbr_list}")
    // log.trace(" -- zone_list = ${state.zone_list}")
    //log.trace(" -- old_zone_list = ${state.old_zone_list}")
    log.trace(" -- deleted_zone_list = ${state.deleted_zone_list}")
    //log.trace(" -- new_zone_list = ${state.new_zone_list}")
    //log.trace(" -- zone_list state.zones_changed = ${state.zones_changed} ")  
    
    events << createEvent(name: "zone_count", value: state.zone_count, isStateChange: true, displayed: true)
    
    sendEvent(events)
    
    if(state.updateDevices)
       update_zone_devices()

    return events
}


def update_state(data) {
    log.trace("--- update_state ") // data=${data}")

    def events = []
    // We are using SHM states now for armed.
    // def armed = data.panel_armed || (data.panel_armed_stay != null && data.panel_armed_stay == true)
    // def panel_state = armed ? "armed" : "disarmed"
        
    // CJ MODS
    def msg = "NONE"
    def powerSource = "unknown"
    def bitfield = ""
    def numcode  = ""
    def rawdata  = ""
    def keypad_msg = "NONE"
    
    // CJ MOD
    // Get/Set the Panel Alpha Message
    //
    msg = data.last_message_received
 
    // log.trace(">> MSG=${msg}" )
    
    def admsg = msg.split(',')
    
    admsg.eachWithIndex { item, index -> 
        // log.trace("indexed admsg ${index}: ${item}" )
        if(index==0) {
          def i = item.indexOf('[')
          if(i>0)
            item = item.substring(i)
          bitfield = item
          }
        if(index==1)
          numcode = item
        if(index==2)
          rawdata = item
        if(index==3) {
          item=item.replaceAll("\"", "")
          item=item.replaceAll("\r", "")
          item=item.replaceAll("\n", "")
          keypad_msg=item
          }
        }
    //log.trace("bitfield: ${bitfield}" )
    log.trace("keypad_msg: ${keypad_msg}" )
    state.keypadMsg = keypad_msg
        
    // CJ MOD
    // Set the powerSource attribute of ST Power Source Capability
    if(data.panel_powered) {
       state.powerSource = "ac"
       } else  if(data.panel_on_battery) {
                  state.powerSource = "battery"
               } else
                  state.powerSource = "unknown"
                         
    // CJ MOD
    // Get the chime indicator
    //
    def chime_flag = bitfield.substring(9,10)
    log.trace("chime_flag=${chime_flag}" )
    if(chime_flag == "0")
       state.chime = "off"
    else
       state.chime = "on"
       
    log.trace("state.chime=${state.chime}" )
    // log.trace("device.chime=${device.chime}" )
    
    // CJ MOD
    // Get the ready indicator
    //
    def ready_flag = bitfield.substring(1,2)
    log.trace("ready_flag=${ready_flag}" )
    if(ready_flag == "0")
       state.ready = "not_ready"
    else
       state.ready = "ready";
    
    // CJ MOD
    // Get the battery indicator
    //
    def battery_flag = bitfield.substring(12,13)
    log.trace("battery_flag=${battery_flag}" )
    if(battery_flag == "0") {
       state.batteryState = "ok"
       //state.battery       = 0;
       }
    else {
       state.batteryState = "low"
       //state.battery       = 100;
       }
       
    // CJ MOD
    // Get the systemState
    //
    def system_state = bitfield.substring(15,16)
    log.trace("system_state=${system_state}" )
    if(system_state == "0") {
       state.systemState= "ok"
       }
    else {
       state.systemState = "trouble"
       }
    
       
    // CJ MOD
    // armed is now using SHM states away, stay, off (and more)
    //
    state.armed="off"
    def shmStatus = "off"
    def perimeter_flag = bitfield.substring(16,17)
    log.trace("perimeter_flag=${perimeter_flag}" )
    if(data.panel_armed || perimeter_flag == "1") {
       state.armed="away"
       shmStatus="away"
       }
    if(data.panel_armed_stay) {
       state.armed = "stay"
       shmStatus = "stay"
       }
    
    // Track the panel state in state variables
    if(state.armed=="off")
      state.panel_state = "off" 
    if(state.armed == "away")
      state.panel_state = "away"
    if(state.armed == "stay" )
      state.panel_state = "stay"
    if(data.panel_panicked)
      state.panel_state = "panicked"
    if(data.panel_alarming)
      state.panel_state = "alarming"     
    if(data.panel_fire_detected)
      state.panel_state = "fire"
    
    
    // Send events to update attributes and the UI
    events << createEvent(name: "keypadMsg", value: state.keypadMsg, isStateChange: true, displayed: true)
    events << createEvent(name: "ready", value: state.ready, isStateChange: true, displayed: true)
    events << createEvent(name: "powerSource", value: state.powerSource, isStateChange: true, displayed: true)
    events << createEvent(name: "chime", value: state.chime, isStateChange: true, displayed: true)
    events << createEvent(name: "ready", value: state.ready, isStateChange: true, displayed: true)
    events << createEvent(name: "ready", value: state.ready, isStateChange: true, displayed: true)
    events << createEvent(name: "systemState", value: state.systemState, isStateChange: true, displayed: true)
    events << createEvent(name: "batteryState", value: state.batteryState, isStateChange: true, displayed: true)
        
    events << createEvent(name: "lock", value: data.panel_armed_stay ? "locked" : "unlocked", isStateChange: true, displayed: false)
    events << createEvent(name: "armed", value: state.armed )  // armed ? "armed" : "disarmed", isStateChange: true, displayed: false)
    events << createEvent(name: "alarm", value: data.panel_alarming ? "both" : "off", isStateChange: true, displayed: true)
    events << createEvent(name: "smoke", value: data.panel_fire_detected ? "detected" : "clear", isStateChange: true, displayed: true)
    events << createEvent(name: "panel_state", value: state.panel_state, isStateChange: true, displayed: true)
      
    
    // IMPORTANT
    // Create an event to notify Smart Home Monitor (SHM).
        
    events << createEvent(name: "alarmStatus", value: shmStatus, isStateChange: true, displayed: true)
    events << createEvent(name: "alarmSystemStatus", value: shmStatus, isStateChange: true, displayed: true)  // CJ MOD - Need this instead to sync state with SHM.

    // Build or clear faulted zone events
    //
    log.trace("---executing build_zone_events") //data=${data}")
    def zone_events = build_zone_events(data)
    log.trace("---executed build_zone_events zone events=${zone_events}")
    events += zone_events

    // Build Relay Status Events
    //
    log.trace("---executing build_relay_events ") //data=${data}")
    def relay_events = build_relay_events(data)
    log.trace("---executed build_relay_events relay events=${relay_events}")
    events += relay_events
    
    // Set new states.
    state.alarm=alarm
    state.alarm_status=alarm_status
    state.panel_state = panel_state
    state.fire = data.panel_fire_detected
    state.alarming = data.panel_alarming
    state.armed = data.panel_armed
    state.armed_away = armed_away
    state.armed_stay = armed_stay
    state.panel_panicked= data.panel_panicked
    state.fire_detected = data.panel_fire_detected
    
    /***
    log.debug "--update_state finding keypad device..."
    def kpd = findKeypadDevice()
    if(kpd!=null) {
         log.debug "--update_state updating keypad device..."
         kpd.sendEvent(name: "keypadMsg", value: state.keypadMsg, isStateChange: true, displayed: false)
         kpd.sendEvent(name: "chime", value: state.chime, isStateChange: true, displayed: false)
         }
     ***/
       
    return events
}  // update_state


private def build_zone_events(data) {

    // cvjanick MOD - Updated update_zone_switches to update_zone_status to remove reference to implementation
    // 
    def events = []

    // TODO: probably remove this.
    if (state.faulted_zones == null)
        state.faulted_zones = []

    //log.trace("Previous faulted zones: ${state.faulted_zones}")
    //log.trace("--- build_zone_events: data=${data}  zones_faulted=${data.panel_zones_faulted} current_faults=${state.faulted_zones}")

    // data from Alarmdecoder
    def current_faults = data.panel_zones_faulted
    def number_of_zones_faulted = current_faults.size()
    
    // Make sure we send this to the attribute for the UI tile
    sendEvent(name: "totalZoneFaults", value: number_of_zones_faulted)

    // new faults are current fault list minus last fault list stored in state
    def new_faults = current_faults.minus(state.faulted_zones)
    
    // cleared faults are previous fault list from state less the faults on the new list from alarm decoder
    def cleared_faults = state.faulted_zones.minus(current_faults)

    log.trace("Current faulted zones: ${current_faults}")
    log.trace("New faults: ${new_faults}")
    log.trace("Cleared faults: ${cleared_faults}")

    // Trigger switches for newly faulted zones.
    log.trace(" --- build_zone_events new_faults=${new_faults}")
    for (def i = 0; i < new_faults.size(); i++)
    {
        //log.trace("Setting switch ${new_faults[i]}")
        def zone_events = update_zone_status(new_faults[i], true)
        events = events.plus(zone_events)
    }

    // Reset switches for cleared zones.
    log.trace(" --- build_zone_events cleared_faults=${new_faults}")
    for (def i = 0; i < cleared_faults.size(); i++)
    {
        //log.trace("Clearing switch ${cleared_faults[i]}")
        def zone_events = update_zone_status(cleared_faults[i], false)
        events = events.plus(zone_events)
    }

    log.trace("Filling zone tiles..")

    // Fill zone tiles
    for (def i = 1; i <= state.max_zone_status_tiles; i++) {
        if (number_of_zones_faulted > 0 && i <= number_of_zones_faulted) {
            if (device.currentValue("zoneStatus${i}") != current_faults[i-1])
                events << createEvent(name: "zoneStatus${i}", value: current_faults[i-1], displayed: false)
        }
        else {
            if (device.currentValue("zoneStatus${i}") != null)
                events << createEvent(name: "zoneStatus${i}", value: "", displayed: false)  // HACK: Setting this to an empty string has less issues
                                                                                            //       than setting it to null.  Setting it to null
                                                                                            //       results in a NumberFormatException after installing
                                                                                            //       a new version of the device type.
        }
    }

    state.faulted_zones = current_faults

    return events
}

private def update_zone_status(zone, faulted) {
    // cvjanick MOD - Updated update_zone_switches to update_zone_status for automatically configured VirtualZoneControls
    // 
    def events = []
    
    if(state.zone_nbr_list==null) {
       log.error("-- update_zone_status AlarmDecoder state.zone_nbr_list does not exist!!")
       return
       }
   
    //log.trace(" -- update_zone_settings zone = ${zone}")   
    //log.trace(" -- update_zone_settings state.zone_list = ${state.zone_list}")  
    log.trace(" -- update_zone_status state.zone_nbr_list = ${state.zone_nbr_list}")  
    
    def found = false
    if (state.zone_nbr_list.contains(zone))
        found = true
    
    if ( found ) 
       log.trace( "---update_zone_status ZONE FOUND IN ZONE LIST")
    else
       log.trace( "---update_zone_status ZONE NOT FOUND IN ZONE LIST!!!")
    
    if ( found ) 
        if (faulted)
                events << createEvent(name: "zone-open", value: zone, isStateChange: true, displayed: true)
            else
                events << createEvent(name: "zone-closed", value: zone, isStateChange: true, displayed: true)
    
       log.debug( "---update_zone_status events=${events}")
       
    return events
}


private def build_relay_events(data) {

    // cvjanick MOD - Parse relay messages from alarmdecoder data panel_relay_status - a list of maps
    // 
    def events = []
    
    def relay_status = data.panel_relay_status
    if(relay_status==null) {
       log.trace("---build_relay_events - no relay data found")
       return
    }  //else
       //    log.trace("---build_relay_events relay_status=${relay_status}")
    
    if(state.old_relay_config==null)
        state.old_relay_config = []

    // Relay config will be map of address, channel
    if (state.relay_config == null)
        state.relay_config = []
    else
        state.old_relay_config = state.relay_config
        
    // Relay list contains the address, value, channel maps from the parsed data
    if (state.relay_list == null)
        state.relay_list = []

    state.old_relay_list = state.relay_list
    
    //  Process new relay data if there is any.
    def rcnt = 0
    def relay_config = []
    def relay_list   = []
    def open_relay_list   = []
    def closed_relay_list = []
    relay_status.each { relay ->
      log.trace("relay.channel=${relay.channel}")
      rcnt++
      def r = [ name: "${relay.address}", channel: relay.channel ]
      log.trace("r=${r}")
      relay_config.add(r)
      relay_list.add(relay)
      if(relay.value==1||relay.value=="1")
         closed_relay_list.add(relay)
      else 
         open_relay_list.add(relay)
      // create relay events 1=closed, 0=open
      log.trace("relay=${relay}")
      if(relay.value==1||relay.value=="1")
          events << createEvent(name: "relay-closed", value: "${relay.address}-${relay.channel}", isStateChange: true, displayed: false)
      else
          events << createEvent(name: "relay-open",   value: "${relay.address}-${relay.channel}", isStateChange: true, displayed: false)

    } // each
    
    state.relay_count  = rcnt
    state.relay_config = relay_config
    state.relay_list   = relay_list
    state.open_relay_list   = open_relay_list
    state.closed_relay_list = closed_relay_list
    
    log.debug("build_relay_events state.relay_list=${state.relay_list}")
    log.debug("build_relay_events state.old_relay_list=${state.old_relay_list}")
    state.new_relay_list     = state.relay_list.minus(state.old_relay_list)
    state.deleted_relay_list = state.old_relay_list.minus(state.relay_list)
    
    log.trace("relay_count=${state.relay_count}")
    //log.trace("relay_config=${state.relay_config}")
    log.trace("relay_list=${state.relay_list}")
    log.trace("deleted_relay_list=${state.deleted_relay_list}")
    //log.trace("new_relay_list=${state.new_relay_list}")
    //log.trace("old_relay_list=${state.old_relay_list}")
    //log.trace("open_relay_list=${state.open_relay_list}")
    //log.trace("closed_relay_list=${state.closed_relay_list}")  
    
    // Set attributes for total open/closed relays
    sendEvent(name: "totalOpenRelays", value: state.open_relay_list.size())
    sendEvent(name: "totalClosedRelays", value: state.closed_relay_list.size())
    
    // Fill relay tiles
    for (def i = 1; i <= state.max_open_relay_tiles; i++) {   
         sendEvent(name: "relayOpenStatus${i}", value: "", displayed: false)
         }
    for (def i = 1; i <= state.max_closed_relay_tiles; i++) {   
         sendEvent(name: "relayClosedStatus${i}", value: "", displayed: false)
         }
         
    for (def i = 1; i <= state.max_closed_relay_tiles; i++) {
        if (state.closed_relay_list.size() > 0 && i <= state.closed_relay_list.size ) {
            def relay = state.closed_relay_list.get(i-1)
            events << createEvent(name: "relayClosedStatus${i}", value: "${relay.address}-${relay.channel}", displayed: false)
        }
        }
    for (def i = 1; i <= state.max_open_relay_tiles; i++) {
        if (state.open_relay_list.size() > 0 && i <= state.open_relay_list.size ) {
            def relay = state.open_relay_list.get(i-1)
            events << createEvent(name: "relayOpenStatus${i}", value: "${relay.address}-${relay.channel}", displayed: false)
        }
        }
    
    return events    
}

/******************** Zone/Relay Child Update Devices Add/Delete *********************/

def update_zone_devices() {
    log.debug("--handler update_zone_devices")
    log.debug("--handler update_zone_devices deleted=${state.deleted_zone_list}")

    // Get this AlarmDecoders deviceNetworkId
    def dni = device.deviceNetworkId
       
    // Add VirtualZoneControl and VirtualContactSensor devices depending on setttings.
    //
    
    // First, delete any old child zone devices for zones that have been removed from the webapp
    // 
    if(settings.use_virtual_zones || settings.use_physical_zones)
      if(state.deleted_zone_list.size()>0 ) {
           log.trace(" -- update_zone_devices - deleting deleted_zone_list=${state.deleted_zone_list}")
           parent.uninstallDeletedZoneDevices(dni, state.deleted_zone_list)
           // uninstallDeletedZoneDevices(dni, state.deleted_zone_list)
      }
     
    // If setting is on to add virtual zones add the VirtualZoneControldevices
    // otherwise delete them if they exist
    def vcnt=parent.getZoneControlDeviceCount()
    //def vcnt=getZoneControlDeviceCount()
    log.debug "- handler VZC vcnt=${vcnt}"
    
    // Only ADEMCO panels support the emulated zones..
    if(settings.panel_type=="ADEMCO") {
      if(settings.use_virtual_zones) {
            log.trace("--- update_zone_devices adding/updating VirtualZoneControl devices=${state.virtual_zone_list}")
            parent.addZoneControlDevices(dni, settings.defaultSensorToClosed, state.virtual_zone_list)
            //addZoneControlDevices(dni, state.virtual_zone_list)
      } else if( vcnt>0)
           { // Setting is off, delete all VirtualZoneControls
            log.trace("--- update_zone_devices deleting ALL VirtualZoneControl devices=${state.virtual_zone_list}")
            parent.uninstallZoneControlDevices(dni, state.virtual_zone_list)
            //uninstallZoneControlDevices(dni, state.virtual_zone_list)
           }
     }
    
    // If setting is on to add physical zones add the VirtualContactSensors  
    // otherwise delete them if they exist
    def pcnt=parent.getZoneSensorDeviceCount()
    //def pcnt=getZoneSensorDeviceCount()
    
    //if(settings.panel_type=="ADEMCO")
    if(settings.use_physical_zones) {
          log.trace("--- update_zone_devices adding ALL VirtualContactSensor devices=${state.physical_zone_list}")
          parent.addZoneSensorDevices(dni, settings.defaultSensorToClosed, state.physical_zone_list)
          //addZoneSensorDevices(dni, state.physical_zone_list)
    } else if(pcnt>0) {
            log.trace("--- update_zone_devices deleting ALL VirtualContactSensor devices=${state.physical_zone_list}")
            parent.uninstallZoneSensorDevices(dni, state.physical_zone_list) 
            //uninstallZoneSensorDevices(dni, state.physical_zone_list) 
         }
       
    // Create, Update or Delete VirtualRelaySensors depending on settings 
    //
    if(state.updateDevices)
       update_relay_devices()
       
} // update_zone_devices()

def update_relay_devices() {

    def dni = device.deviceNetworkId
    
    // Create, Update or Delete VirtualRelaySensors depending on settings 
    //
    // delete removed relays - relays no longer showing in panel_relay_status
    //
    if(state.relay_list!=null)
      log.trace("--- update_relay_devices state.deleted_relay_list=${state.deleted_relay_list}")
    else {
      log.trace("--- update_relay_devices state.deleted_relay_list IS NULL.")
      state.relay_list=[]
      }
      
    if(state.deleted_relay_list!=null)
      log.trace("--- update_relay_devices state.deleted_relay_list=${state.deleted_relay_list}")
    else {
      log.trace("--- update_relay_devices state.deleted_relay_list IS NULL.")
      state.deleted_relay_list=[]
      }
      
    if(settings.use_relays)
      if( state.deleted_relay_list?.size()>0)
          parent.uninstallRelaySensorDevices(dni, state.deleted_relay_list)
          //uninstallRelaySensorDevices(dni, state.deleted_relay_list)
          
    // Create, Update or Delete VirtualRelaySensors depending on settings 
    //
    def rcnt=parent.getRelaySensorDeviceCount()
    if(settings.use_relays) {
        if(state.relay_list.size()>0) {
           log.trace("--- update_relay_devices adding or updating VirtualRelaySensor devices=${state.relay_list}")
           parent.addRelaySensorDevices(dni, state.relay_list)
           // addRelaySensorDevices(dni, state.relay_list)
         }
    } else if(rcnt>0) {
             log.trace("--- update_relay_devices deleting ALL VirtualRelaySensor devices=${state.relay_list}")
             parent.uninstallRelaySensorDevices(dni, state.relay_list) 
             // uninstallRelaySensorDevices(dni, state.relay_list) 
             }
             
     state.updateDevices=false
             
} // update_relay_devices

/*********** Zone & Relay Open/Close Handlers ******************/

def subscribeAll() {
    log.trace("--handler.subscribeAll()")

    def devices = getChildDevices()

    log.trace("--handler.subscribeAll() devices=${devices}")
    
    devices.each { d-> 
       // Need to listen to these events so that if a virtual zone control is open or close we send Lxx0 or Lxx1 keys
       // through alarm decoder. They will have no effect on physical or RF zones, just emulated virtual zones.
           subscribe(d, "zone-open",         zoneOpenHandler,                 [filterEvents: false])
           subscribe(d, "zone-closed",       zoneClosedHandler,               [filterEvents: false])
           subscribe(d, "relay-open",        relayOpenHandler,                [filterEvents: false])
           subscribe(d, "relay-closed",      relayClosedHandler,              [filterEvents: false])         
           //subscribe(d, "alarmStatus",       parent.alarmdecoderAlarmHandler, [filterEvents: false])
           //subscribe(d, "refresh",           parent.refreshHandler,           [filterEvents: false])
           //subscribe(d, "send-message",      parent.sendMessageHandler,       [filterEvents: false])
    } // devices
}

def zoneOpenHandler(evt) {
    log.debug("--handler zoneOpenHandler: value=${evt.value}")
    log.debug("--handler zoneOpenHandler: parent.settings.defaultSensorToClosed=${parent.settings.defaultSensorToClosed}")

    //def d = getAllChildDevices().find { it.deviceNetworkId.contains("${state.ip}:${state.port}:switch${evt.value}") }
    d = getChildDevices().find { it.deviceNetworkId.contains("${state.ip}:${state.port}:switch${evt.value}") }
    log.trace(" -- zoneOpenHandler d.state.zoneID=${evt.value}")
    
    if (d!=null)
    {
        log.trace(" -- zoneOpenHandler device ${evt.value} found.")
        def sensorValue = "closed"
        //if (settings.defaultSensorToClosed == true) 
        if (parent.settings.defaultSensorToClosed == true) 
            sensorValue = "open"

        // Send event to the appropriate virtual device (d) for this zone event
        //d.open()
        // If its a VirtualZoneControl (Door Control)
        d.sendEvent(name: "door", value: sensorValue, isStateChange: true, filtered: true)
        // If its a VirtualContactSensor
        d.sendEvent(name: "contact", value: sensorValue, isStateChange: true, filtered: true) 
    }
}

def zoneClosedHandler(evt) {
    log.debug("-- handler zoneCloseHandler: desc=${evt.value}")
    log.debug("--handler zoneCloseHandler: parent.settings.defaultSensorToClosed=${parent.settings.defaultSensorToClosed}")

    def d = getChildDevices().find { it.deviceNetworkId.contains("${state.ip}:${state.port}:switch${evt.value}") }

    log.trace(" --handler zoneClosedHandler d.state.zoneID=${evt.value}")

    log.debug "parent.settings.defaultSensorToClosed=${parent.settings.defaultSensorToClosed}"
    
    if (d!=null)
    {
        log.trace(" -- zoneOpenHandler device ${evt.value} found.")
        def sensorValue = "open"
        //if (settings.defaultSensorToClosed == true)
        if (parent.settings.defaultSensorToClosed == true)
            sensorValue = "closed"
        //d.close()
        // If its a VirtualZoneControl (Door Control)
        d.sendEvent(name: "door", value: sensorValue, isStateChange: true, filtered: true)
        // If its a VirtualContactSensor
        d.sendEvent(name: "contact", value: sensorValue, isStateChange: true, filtered: true) 
    }
}

def relayOpenHandler(evt) {
    log.debug("-- handler relayOpenHandler: desc=${evt.value}")

    def d = getChildDevices().find { it.deviceNetworkId.contains("${state.ip}:${state.port}:relay${evt.value}") }
    
    if (d)
    {
        relayValue = "open"
        // Send event to the appropriate virtual device (d) for this relay event
        //d.open()
        d.sendEvent(name: "contact", value: relayValue, isStateChange: true, filtered: true) 
    }
}

def relayClosedHandler(evt) {
    log.debug("--handler relayCloseHandler: desc=${evt.value}")

    def d = getChildDevices().find { it.deviceNetworkId.contains("${state.ip}:${state.port}:relay${evt.value}") }

    if (d)
    {
        relayValue = "closed"
        //d.close()
        d.sendEvent(name: "contact", value: relayValue, isStateChange: true, filtered: true) 
    }
}

/******* Child Zone/Relay Device Management ************/

def getZoneControlDeviceCount() {
    def cnt=findDeviceTypeCount("Virtual Zone Control")
    log.trace("getZoneControlDeviceCount=${cnt}")
    return cnt
}

def getZoneSensorDeviceCount() {
    def cnt=findDeviceTypeCount("Virtual Zone Sensor")
    log.trace("getZoneSensorDeviceCount=${cnt}")
    return cnt
}
def getRelaySensorDeviceCount() {
    def cnt=findDeviceTypeCount("Virtual Relay Sensor")
    log.trace("getRelaySensorDeviceCount=${cnt}")
    return cnt
}

def findDeviceTypeCount(type) {
    def c=0
    getChildDevices().each { d ->
        log.trace("Child device type=${d.getTypeName()}")
        if(d.getTypeName()=="${type}")
          c++
    }
    return c
}

def addZoneControlDevices(dni, zone_list) {

    if(zone_list==null) {
        Alert( "The AlarmDecoder zone_list is empty, no devices created.") 
        return
      }
      
    log.debug(" -- addZoneControlDevices AD deviceNetworkId=${dni}, state.hub=${state.hub}, zone_list=${zone_list}")
    
    def ecnt=0
    def created=false
    
    zone_list.each { zone -> 
        def id              = zone.zone_id
        def name            = zone.name
        def description     = zone.description
        
        log.trace("-- addZoneControlDevices: checking existence of zonecontrol child device for zone=${id}, name=${name}")
        
        // CJ MOD - Create the zone control devices
        //
        // def newSwitch = state.devices.find { k, v -> k == "${state.ip}:${state.port}:switch${i}" }
        //def urn = getDataValue("urn")
        def child_dni = "${dni}:switch${id}"
        def d         = getChildDevices().find { it.deviceNetworkId=="${child_dni}" }
        
        if(d!=null)
            log.trace("-- addZoneControlDevices: UPDATING d=${d}")
        else
             log.trace("-- addZoneControlDevices: ADDING DEVICE...")     
             
        def zone_control = null
        
        if (d==null)
        {  
            log.trace("Adding VirtualZoneControl device ${child_dni}")
            try{
                  zone_control = addChildDevice("alarmdecoder",         // namespace
                                                "Virtual Zone Control",   // type 
                                                "${child_dni}",         // device.networkId
                                                state.hub,              // hub id
                                                [name:          "${child_dni}", 
                                                label:          "Alarm Zone #${id}", 
                                                completedSetup: true, 
                                                isComponent:    false,
                                                componentName:  "${child_dni}", 
                                                componentLabel: "Alarm Zone #${id}", 
                                                data:[ad_dni: dni, dni: child_dni ]]
                                                )
                  created=true
                  log.trace("-- addZoneControl addChildDevice ok.")
                } catch(e){
                     ecnt++
                     created=false
                     //Alert("Error adding VirtualZoneControl device ${child_dni}, error: ${e}")
                     log.error("Error adding VirtualZoneControl device ${child_dni}, error: ${e}")
                     //log.error("Error cause: ${e.getCause()}")
                }
            // Error this is showing null...
            log.trace("-- addZoneControlDevices: zone_control=${zone_control}")
            
            def controlValue = "open"
            if (settings.defaultSensorToClosed == true)
                controlValue = "closed"

            // Set default contact state.
            if(zone_control!=null) {
              zone_control.sendEvent(name: "door", value: controlValue, isStateChange: true, displayed: false)
              zone_control.sendEvent(name: "zoneID", value: id, isStateChange: true, displayed: true)
              zone_control.sendEvent(name: "zoneName", value: name, isStateChange: true, displayed: true)
              zone_control.sendEvent(name: "zoneDescription", value: description, isStateChange: true, displayed: true)
              }
        } else {
            log.trace("addZoneControlDevices - device ${child_dni} already exists, updating...")
            //d.sendEvent(name: "zoneID", value: id, isStateChange: true, displayed: true) // id is same as switch nbr..
            d.sendEvent(name: "zoneName", value: name, isStateChange: true, displayed: true)
            d.sendEvent(name: "zoneDescription", value: description, isStateChange: true, displayed: true)
            created=false
        }
        
    } // zone_list.each
    
    def cnt = getZoneControlDeviceCount()
    if(created==true)
       parent.Notify("Created ${cnt} VirtualZoneControls for AlarmDecoder from webapp settings with ${ecnt} errors.")
    else
        parent.Notify("Updated ${cnt} VirtualZoneControls for AlarmDecoder from webapp settings with ${ecnt} errors.")   
    
    //else
    //   Alert("Warning: error adding one or more VirtualZoneControl devices (device not created).")
       
    // Make sure we are subscribing to all events from the new devices.
    //
    if(zone_control!=null)
    try { 
          //subscribeAll()
           subscribe(zone_control, "zone-open",         zoneOpenHandler,                 [filterEvents: false])
           subscribe(zone_control, "zone-closed",       zoneClosedHandler,               [filterEvents: false])
       } catch(e) {
          log.error("--- handler - error during subscribe() error: ${e}" )
       }
    
    log.trace(" --- createChildDevices count=${cnt}")
     
}

def addZoneSensorDevices(dni, zone_list) {

    if(zone_list==null) {
        Alert( "The AlarmDecoder zone_list is empty, no devices created.") 
        return
      }
      
    log.trace(" -- addZoneSensorDevices AD deviceNetworkId=${dni}")
    
    def created=false
    def ecnt=0;
    def size=zone_list.size()
    
    zone_list.each { zone -> 
        def id              = zone.zone_id
        def name            = zone.name
        def description     = zone.description
        
        log.trace("-- addZoneSensorDevices: checking existence of zonecontrol child device for zone=${id}, name=${name}")
        

        def child_dni = "${dni}:switch${id}"
        def d         = getChildDevices().find { it.deviceNetworkId=="${child_dni}" }
        log.trace("-- addZoneSensorDevices: d=${d}")
        
        def sensor = null
        
        // If device not found add it
        if (d==null)
        {  
            log.trace("Adding VirtualZoneSensor device ${child_dni}") 
            try{
                  sensor = addChildDevice("alarmdecoder", 
                                          "Virtual Zone Sensor", 
                                          "${child_dni}", 
                                           state.hub, 
                                           [name: "${child_dni}", 
                                           label: "Alarm Zone #${id}", 
                                           completedSetup: true, 
                                           data:[ad_dni: dni, dni: child_dni ]])
                   created=true
                } catch(e){
                     ecnt++
                     created=false
                     Alert("Error adding VirtualZoneSensor device ${child_dni}, error: ${e}")
                     log.error("Error adding VirtualZoneSensor device ${child_dni}, error: ${e}")
                }
            log.trace("-- addZoneSensorDevices: sensor=${sensor}")
            
            def controlValue = "open"
            if (settings.defaultSensorToClosed == true)
                controlValue = "closed"

            // Set default contact state.
            if(sensor!=null) {
              sensor.sendEvent(name: "contact", value: controlValue, isStateChange: true, displayed: false)
              sensor.sendEvent(name: "zoneID", value: id, isStateChange: true, displayed: true)
              sensor.sendEvent(name: "zoneName", value: name, isStateChange: true, displayed: true)
              sensor.sendEvent(name: "zoneDescription", value: description, isStateChange: true, displayed: true)
            }
        } else {
            // Otherwise update the device name, description
            log.trace("addZoneSensorDevices - device ${child_dni} already exists, updating...")
            d.sendEvent(name: "zoneName", value: name, isStateChange: true, displayed: true)
            d.sendEvent(name: "zoneDescription", value: description, isStateChange: true, displayed: true)
        }
        
    } // zone_list.each
    
    def cnt=getZoneSensorDeviceCount()
    if(created==true)
       parent.Notify("Created ${cnt} VirtualZoneSensors for AlarmDecoder from webapp settings with ${ecnt} errors.")
    else
       parent.Notify("Updated ${cnt} VirtualZoneSensors for AlarmDecoder from webapp settings with ${ecnt} errors.")

    try { 
          subscribeAll()
       } catch(e) {
          log.error("--- handler - error during subscribeAll() error: ${e}" )
       }
    
    log.trace(" --- addVirtualContactSensorDevices count=${cnt}")
     
}

def addRelaySensorDevices(dni, relay_list) {

    if(relay_list==null) {
        Alert( "The AlarmDecoder relay_list is empty, no devices created.") 
        return
      }
      
    log.trace(" -- addRelayContactSensorDevices AD deviceNetworkId=${dni}")
    
    def created=false
    def ecnt=0;
    def size=relay_list.size()
    
    relay_list.each { relay -> 
        def address        = relay.address
        def value          = relay.value
        def channel        = relay.channel
        
        log.trace("-- addRelayContactSensorDevices: checking existence of VirtualRelaySensor address=${address}, channel=${channel}")
        

        def child_dni = "${dni}:relay${address}-${channel}"
        def d         = getChildDevices().find { it.deviceNetworkId=="${child_dni}" }
        log.trace("-- addRelaySensorDevices: d=${d}")
        
        def sensor = null
        
        // If device not found add it
        if (d==null)
        {  
            log.trace("Adding VirtualRelaySensor device ${child_dni}") 
            try{
                  sensor = addChildDevice("alarmdecoder", 
                                          "Virtual Relay Sensor", 
                                          "${child_dni}", 
                                           state.hub, 
                                           [name: "${child_dni}", 
                                            label: "Alarm Relay ${address}-${channel}", 
                                            completedSetup: true, 
                                            data:[ad_dni: dni, dni: child_dni ]])
                   created=true
                } catch(e){
                     ecnt++
                     created=false
                     Alert("Error adding VirtualRelaySensor device ${child_dni}, error: ${e}")
                     log.error("Error adding VirtualRelaySensor device ${child_dni}, error: ${e}")
                }
            log.trace("-- addRelaySensorDevices: sensor=${sensor}")
            
            def controlValue = "open"
            if (settings.defaultSensorToClosed == true)
                controlValue = "closed"

            // Set default contact state.
            sensor.sendEvent(name: "contact", value: controlValue, isStateChange: true, displayed: false)
            sensor.sendEvent(name: "relayAddress", value: id, isStateChange: true, displayed: true)
            sensor.sendEvent(name: "relayChannel", value: name, isStateChange: true, displayed: true)
        } else {
            // Otherwise update the device name, description
            log.trace("addRelaySensorDevices - device ${child_dni} already exists, updating...")
            d.sendEvent(name: "relayAddress", value: name, isStateChange: true, displayed: true)
            d.sendEvent(name: "relayChannel", value: description, isStateChange: true, displayed: true)
        }
        
    } // relay_list.each
    
    def cnt=getRelaySensorDeviceCount()
    if(created==true)
       Notify("Created ${cnt} VirtualRelaySensors for AlarmDecoder from panel status with ${ecnt} errors.")
    else
       Notify("Updated ${cnt} VirtualReaySensors for AlarmDecoder from panel status with ${ecnt} errors.")

    try { 
          subscribeAll()
       } catch(e) {
          log.error("--- manager - error during subscribeAll() error: ${e}" )
       }
    
    log.trace(" --- addRelaySensorDevices count=${cnt}")
     
}

def uninstallDeletedZoneDevices(dni, zone_list) {
    return uninstallChildZoneDevices(dni, zone_list, "deleted")
}
def uninstallZoneControlDevices(dni, zone_list) {
    return uninstallChildZoneDevices(dni, zone_list, "control")
}
def uninstallZoneSensorDevices(dni, zone_list) {
    return uninstallChildZoneDevices(dni, zone_list, "sensor")
}

def uninstallChildZoneDevices(dni, zone_list, type) {
    log.trace( " --handler uninstallChildZoneDevices dni=${dni} zones=${zone_list}")
    
    def urn = getDataValue("urn")
    
    if(zone_list==null)
      switch(type) {
      case "control":
           parent.Notify("No VirtualZoneControol child devices found to delete. No devices removed.")
           return
      case "sensor":
           parent.Notify("No VirtualContactSensor child devices found to delete. No devices removed.")
           return 
      default:
           parent.Notify("No deleted zone child devices found to delete. No devices removed.")
           return 
           }
      
    def cnt     = 0
    def success = false
           
    zone_list.each { z ->  
        log.debug "zone z=${z}"
    
        //def child_dni = "${state.ip}:${state.port}:switch${z.zone_id}"
        def child_dni = "${urn}:switch${z.zone_id}"
        
        def d = getChildDevices().find { it.deviceNetworkId=="${child_dni}" }
        log.debug("-- uninstallChildZoneDevices: d=${d}, child_dni=${child_dni}")
        log.debug("d.deviceNetworkId=${d.deviceNetworkId}")
        
        if ( d !=null )
          {   
            log.trace("uninstallChildlDevices: ${d}" )
            
            try {
                 // d.unsubscribe()
                 //runIn(300,deleteChildDevice(d.deviceNetworkId))
                 deleteChildDevice(d.deviceNetworkId)
                 success=true
                 cnt++
             }
             catch(Exception e) {
                 def msg = "NotFoundException while uninstalling child device ${child_dni} for alarmdecoder ${urn}: ${e}"
                 log.error(msg)
                 //log.error(e.getCause())
                 success=false
              }
           } else {
                 log.debug("--uninstallChildDevice: Cant find device for ${child_dni}")
           }// if

    } // each
    
    def msg = "Uninstall Child Devices called with no valid type parameter"
    log.trace("--uninstallChildZoneDevices cnt=${cnt}")
    switch(type) {
      case "control":
            msg="Deleted ${cnt} VirtualZoneControl devices."
            break
      case "sensor":
            msg="Deleted ${cnt} VirtualContactSensor devices."
            break
      case "deleted":
            msg="Deleted ${cnt} child zone devices based on configuration change in the webapp."
            break
      }
      
      if(success)
          parent.Notify(msg)
      else
          parent.Notify("Error deleting zone device ${d}")
          
      return success 
}

def uninstallRelaySensorDevices(dni, relay_list) {
    log.trace( " --- uninstallChildRelayDevices dni=${dni} relays=${relay_list}")
    
    if(relay_list==null) {
           parent.Notify("No VirtualRelaySensor child devices found to delete. No devices removed.")
           return 
           }
           
     if(dni==null) {
           parent.Notify("No dni provided, dni is null for uninstallRelaySensors.")
           return 
           }
      
    def cnt     = 0
    def success = false
           
    relay_list.each { r ->            
    
        def child_dni = "${dni}:relay${r.address}-${r.channel}"

        def d = getChildDevices().find { it.deviceNetworkId=="${child_dni}" }
        log.trace("-- uninstallChildRelayDevices: d=${d}, child_dni=${child_dni}")
        
        if ( d !=null )
          {   
            log.trace("uninstallChildlRelayDevices: ${d}" )
            
            try {
                 unsubscribe(d)
                 runIn(300,deleteChildDevice(d.deviceNetworkId))
                 success=true
                 cnt++
             }
             catch(Exception e) {
                 def msg = "NotFoundException while uninstalling child device ${child_dni} for alarmdecoder ${urn}: ${e}"
                 log.error(msg)
                 success=false
              }
           } else {
                 log.debug("--uninstallChildDevice: Cant find device for ${child_dni}")
           }// if

    } // each
    
    def msg="Deleted ${cnt} VirtualRelaySensor devices."
      
    if(success)
          parent.Notify(msg)
          
    return success 
}

//******* Child Layout Devices Management *************/
//*******    Keypad companion device     ************/

def getAlarmDecoderDevice() {
  def ad = device
  return ad
}
def getAlarmDecoder() {
  def ad = parent.getChildDevice(device.deviceNetworkId)
 // def ad = parent.getAlarmDecoder()
  return ad
}

def findKeypadDevice() {
    log.debug "findKeypadDevice"
    def ad  = getAlarmDecoder()
    def dni = ad.getDeviceNetworkId()
    dni    += ":keypad"
    log.debug "findKeypadDevice ad=${ad}  dni=${dni}"
    def devices = getChildDevices()
    def kpd = null
    devices.each{ d ->
       def d_dni = d.deviceNetworkId
       log.debug "findKeypadDevice d_dni=${d_dni}  dni=${dni}"
       if(d_dni==dni)
         kpd=d
    }
    log.debug "findKeypadDevice() kpd=${kpd}"
    return kpd
}

def keypadMsgHandler(evt) {
   log.debug "--handler not child keypadMsgHandler..."
}

def keypadChimeHandler(evt) {
   log.debug "--handler not child keypadChimeHandler..."
}

def addKeypadDevice() {
    // Note: change isComponent: true for debugging/to show child device in things list
    //
    log.trace "--manager addKeypadDevice()"

    // Add or Update Keypad Child Device
    def ad      = getAlarmDecoder()
    def ad_dni  = ad.getDeviceNetworkId()
    def d       = findKeypadDevice()
    def kp_dni   = "${ad_dni}:keypad"
    if(d!=null) {
      // remove current device first... 
      kp_dni = d.getDeviceNetworkId()
      log.debug"removing keypad device=${d}, dni=${kpdni}, keypad attributes=${alist}"
      try {
         deleteChildDevice(kp_dni)
         } catch(Exception e) {
             log.trace "addKeypadDevice cant delete keypad device"
         }
    }
    // Now add Device
    def cd = null
    try {
        cd = addChildDevice(
                "alarmdecoder",
				"Alarm Decoder Keypad Layout",
				kp_dni,
				state.hub, 
				[completedSetup: true, 
                 isComponent: false, 
                 componentName: "KeypadLayout", 
                 componentLabel: "KeypadLayout",
                 completedSetup: true, 
                 label: "Alarm Decoder Keypad Layout",
                 data: [ ad_dni: ad_dni, dni: kp_dni, isKeypad: true ]
                 ])
     } catch(Exception e) {
            log.debug "addKeypadDevice error=${e.message}"
     }
     
     log.debug "addKeypadDevice added cd=${cd}"
     
     // cd is devicewrapper
     if(cd!=null) {
       cd.subscribe(ad, "keypadMsg" , cd.keypadMsgHandler)
       sendEvent(name: "ad_layout", value: "keypad")
       }
     
} // addKeypadDevice

def deleteKeypadDevice() {
    log.trace "--handler deleteKeypadDevice()"
    def d = findKeypadDevice()
    log.trace "--handler d=${d}"
    if(d!=null) {
      deleteChildDevice(d.deviceNetworkId)  
      sendEvent(name: "ad_layout", value: "device")
    }
    
}

def sendKeypadEvent(dni, evt) {
   def d = findKeypadDevice()
   if(d!=null)
      d.sendEvent(evt)
}

/*********** Function Keys **************/

def addKeypadKeys() {
      log.debug("addKeypadKeys..")
      addKeypadKeyDevice("K1", "1","1")
      addKeypadKeyDevice("K2", "2","2")
      addKeypadKeyDevice("K3", "3","3")
      addKeypadKeyDevice("K4", "4","4")
      addKeypadKeyDevice("K5", "5","5")
      addKeypadKeyDevice("K6", "6","6")
      addKeypadKeyDevice("K7", "7","7")
      addKeypadKeyDevice("K8", "8","8")
      addKeypadKeyDevice("K9", "9","9")
      addKeypadKeyDevice("K*", "*","*")
      addKeypadKeyDevice("K0", "0","0")
      addKeypadKeyDevice("K#", "#","#")
}
def deleteKeypadKeys() {
      log.debug("deleteKeypadKeys..")
      deleteKeypadKeyDevice( "K1" )
      deleteKeypadKeyDevice( "K2" )
      deleteKeypadKeyDevice( "K3" )
      deleteKeypadKeyDevice( "K4" )
      deleteKeypadKeyDevice( "K5" )
      deleteKeypadKeyDevice( "K6" )
      deleteKeypadKeyDevice( "K7" )
      deleteKeypadKeyDevice( "K8" )
      deleteKeypadKeyDevice( "K9" )
      deleteKeypadKeyDevice( "K*" )
      deleteKeypadKeyDevice( "K0" )
      deleteKeypadKeyDevice( "K#" )
}
def deleteKeypadKeyDevice(name) {
      def d = ""
      d = findKeypadKeyDevice( name )
      if(d!=null)
        deleteChildDevice(d.deviceNetworkId)
}

def addFunctionKeys() {
      addFunctionKeyDevice("F1", settings.panic1_label,settings.panic1_delay, "<s1>")
      addFunctionKeyDevice("F2", settings.panic2_label,settings.panic2_delay, "<s2>")
      addFunctionKeyDevice("F3", settings.panic3_label,settings.panic3_delay, "<s3>")
      addFunctionKeyDevice("F4", settings.panic4_label,settings.panic4_delay, "<s4>")
}
def deleteFunctionKeys() {
      deleteFunctionKeyDevice( "F1" )
      deleteFunctionKeyDevice( "F2" )
      deleteFunctionKeyDevice( "F3" )
      deleteFunctionKeyDevice( "F4" )
}

def deleteFunctionKeyDevice(name) {
      def d = ""
      d = findFunctionKeyDevice( name )
      if(d!=null)
        deleteChildDevice(d.deviceNetworkId)
}

def findKeypadKeyDevice(component_name) {
  def urn = getDataValue("urn")
  def dni = "${urn}:key:${component_name}"
  def kpd=null
  def devices=getChildDevices()
  devices.each{ d ->
     if( d.getDeviceNetworkId() == dni)
        kpd=d
   }
   return kpd
}
def addKeypadKeyDevice(component_name, label, val) {
    // Add or Update Keypad Child Device
    def visible = ! settings.show_keypad
    def d       = findKeypadKeyDevice(component_name)
    def urn     = getDataValue("urn")
    def kpdni   = "${urn}:key:${label}"
    if(d!=null) {
      // Update the device...
      d.sendEvent(name: "label", value: label)
      d.sendEvent(name: "value", value: val)
      return
    }
    // Now add Device
    def cd = null
    try {
        cd = addChildDevice(
                "alarmdecoder",
				"Alarm Decoder Keypad Key",
				"${urn}:key:${component_name}",
				null, // hubid
				[completedSetup: true, 
                 isComponent: true, 
                 componentName: "${component_name}", 
                 componentLabel: "${label}",
                 label: "Alarm Decoder Key ${label}",
                 data: [value: val, label: "${label}", dni: "${urn}:key:${component_name}" ]
                 ])
     } catch(Exception e) {
            log.debug "addKeypadKeyDevice error=${e.message}"
     }
     log.debug "addKeypadKeyDevice added cd=${cd}"

} // addKeypadKeyDevice

def findFunctionKeyDevice(name) {
  def urn = getDataValue("urn")
  def dni = "${urn}:function:${name}"
  def kpd=null
  def devices=getChildDevices()
  devices.each{ d ->
     if( d.getDeviceNetworkId() == dni)
        kpd=d
   }
   return kpd
}
def addFunctionKeyDevice(name, label, delay, val) {
    // Add or Update Keypad Child Device
    def visible = ! settings.show_keypad
    def d       = findFunctionKeyDevice(name)
    def urn     = getDataValue("urn")
    if(d!=null) {
      // remove current device first... 
      // Update the device...
      d.sendEvent(name: "label", value: label)
      d.sendEvent(name: "delay", value: delay)
      d.sendEvent(name: "value", value: val)
      return
    }
    // Now add Device
    def cd = null
    try {
        cd = addChildDevice(
                "alarmdecoder",
				"Alarm Decoder Function Key",
				"${urn}:function:${name}",
				null, // hubid
				[completedSetup: true, 
                 isComponent: true, 
                 componentName: "${name}", 
                 componentLabel: "${label}",
                 label: "Alarm Decoder Function Key ${label}",
                 data: [value: val, label: "${label}", delay: delay, dni: "${urn}:function:${name}"]
                 ])
     } catch(Exception e) {
            log.debug "addFunctionKeyDevice error=${e.message}"
     }
     log.debug "addFunctionKeyDevice added cd=${cd}"

} // addFunctionKeyDevice



/***** Utility *****/

private def parseEventMessage(String description) {
    log.trace "handler parseEventMessage" 
    def event = [:]
    def parts = description.split(',')
    log.trace "handler parseEventMessage description=${description}"  //, parts=${parts}" 
    
    parts.each { part ->
        part = part.trim()
        //log.trace "handler parseEventMessage, part=${part}" 
        if (part==null)
           return
        if (part.startsWith('devicetype:')) {
            def valueString = part.split(":")[1].trim()
            event.devicetype = valueString
        }
        else if (part.startsWith('mac:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.mac = valueString
            }
        }
        else if (part.startsWith('networkAddress:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.ip = valueString
            }
        }
        else if (part.startsWith('deviceAddress:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.port = valueString
            }
        }
        else if (part.startsWith('ssdpPath:')) {
            part -= "ssdpPath:"
            def valueString = part.trim()
            if (valueString) {
                event.ssdpPath = valueString
            }
        }
        else if (part.startsWith('ssdpUSN:')) {
            part -= "ssdpUSN:"
            def valueString = part.trim()
            if (valueString) {
                event.ssdpUSN = valueString
            }
        }
        else if (part.startsWith('ssdpTerm:')) {
            part -= "ssdpTerm:"
            def valueString = part.trim()
            if (valueString) {
                event.ssdpTerm = valueString
            }
        }
        else if (part.startsWith('headers')) {
            part -= "headers:"
            def valueString = part.trim()
            if (valueString) {
                event.headers = valueString
            }
        }
        else if (part.startsWith('body')) {
            part -= "body:"
            def valueString = part.trim()
            if (valueString) {
                event.body = valueString
            }
        }
    }

    event
}


def get_ad_config() {
    log.trace("--- get_ad_config: ")
    def urn    = getDataValue("urn")
    def apikey = _get_api_key()
    

    return hub_http_get_callback(urn, "/api/v1/alarmdecoder/configuration?apikey=${apikey}", configHandler )
  }
  
def get_zone_list() {
    log.trace("--- get_zone_list: ")
    def urn    = getDataValue("urn")
    def host   = urn.substring(0, urn.indexOf(":"))
    def apikey = _get_api_key()

    return hub_http_get(urn, "/api/v1/zones?apikey=${apikey}")
  }
  
def send_keys(keys) {
    log.trace("--- send_keys: keys=${keys}")

    def urn = getDataValue("urn")
    def apikey = _get_api_key()

    return hub_http_post(urn, "/api/v1/alarmdecoder/send?apikey=${apikey}", """{ "keys": "${keys}" }""")
}
  
def update_ad2web() {
    log.trace("--- update_ad2web ")
    def urn    = getDataValue("urn")
    
    def host   = urn.substring(0, urn.indexOf(":"))
    def apikey = _get_api_key()
    def path   = "/api/v1/users?apikey=${apikey}" 
    //def path   = "/api/v1/alarmdecoder/send?apikey=${apikey}" 
    //def body = """{  'access_token' : \"${state.access_token}\" 'endpoint_url' : \"${state.endpoint_url}\" }"""
    
    def body = """{ "email": "email@example.com", "name": "testuser", "password": "password",  "role": 1, "status": 1 }"""
    
    // return hub_http_post(urn, "/api/v1/alarmdecoder/send?apikey=${apikey}", """{ "keys": "${keys}" }""")
    def request = hub_http_post_callback(urn, path, body, ad2WebHandler )
    log.trace("--- update_ad2web request=${request}")    
    
    return request
}

def hub_http_get(host, path) {
    log.trace "--- hub_http_get: host=${host}, path=${path}"

    def httpRequest = [
        method:     "GET",
        path:       path,
        headers:    [ HOST: host ]
    ]

    return new physicalgraph.device.HubAction(httpRequest, "${host}")
}


def hub_http_get_callback(host, path, cb) {
    log.trace "--- hub_http_get: host=${host}, path=${path}"
              
    def httpRequest = [
        method:     "GET",
        path:       path,
        headers:    [ HOST: host ]
    ]

    // hubAction( request, protocol, dni/host, [callback: callbackHandler] )
    return new physicalgraph.device.HubAction(httpRequest, "${host}", cb)
}

def hub_http_post(host, path, body) {
    log.trace "--- hub_http_post: host=${host}, path=${path}"

    //def ebody = uuencode(body)
    def len   = body.length()
    def httpRequest = [
        method:     "POST",
        path:       path,
        headers:    [ HOST: host, "Content-Type": "application/json", "Content-Length": len , 'Accept': '*/*' ],
        body:       body
    ]

    return new physicalgraph.device.HubAction(httpRequest, "${host}")
}

def hub_http_post_callback(host, path, body, cb) {
    log.trace "--- hub_http_post: host=${host}, path=${path}"

    //def ebody = uuencode(body)
    def len   = body.length()
    def httpRequest = [
        method:     "POST",
        path:       path,
        headers:    [ HOST: host, "Content-Type": "application/json", "Content-Length": len ,'Accept': 'application/json' ],
        body:       body
    ]

    return new physicalgraph.device.HubAction(httpRequest, "${host}", [callback: cb])
}

def hub_https_post(host, path, body) {
    log.trace "--- hub_https_post: host=${host}, path=${path}"

    def uri  = "https://${host}"
    def len = body.length()
    
    def httpRequest = [
              method:     "POST",
              uri:        uri,
              path:       path,
              headers: [
              'HOST' : host,
              "Content-Type": "application/json", 
              "Content-Length" : len,
              'Accept': '*/*',
              'DNT': '1',
              'Accept-Encoding': 'plain',
              'Cache-Control': 'max-age=0',
              'Accept-Language': 'en-US,en,q=0.8',
              'Connection': 'keep-alive',
              'Referer': 'https://mytotalconnectcomfort.com/portal',
               // 'X-Requested-With': 'XMLHttpRequest',
              'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
              'Cookie': data.cookies
              ],
              Body: body
              ]
    return new physicalgraph.device.HubAction(httpRequest, null, [callback: ad2WebHandler])
    //new physicalgraph.device.HubAction(httpRequest, "${host}", [callback: ad2WebHandler])
}

def _get_user_code() {
    def user_code = settings.user_code

    return user_code
}

def _get_api_key() {
    def api_key = settings.api_key

    return api_key
}
