/**
 *  AlarmDecoder Service Manager
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
 *  cvjanick - Recommended changes in this version:
 *  1. Use updated() method in device handler to load zone list from the webapp.
 *  2. Use Door Control capability for VirtualZoneControl for open/close tracking and to enable SmartApps to link ST Device status to Zone Status on panel for emulated zones.
 *  3. Provide setting to enable user to automatically create VirtualZoneControl child devices (or not), this will also enable reconfiguration when zones change in webapp (for now).
 *  3. Reduce calls to parent where possible (ST guidelines)
 *
 *  For future version:
 *  Ensure Service manager and device handlers work with multiple AlarmDecoders configured at a location.
 *  Perhaps add a zone type field to the web app (panel, expander, RF, emulated) to help with automatic zone configuration
 */
import groovy.json.JsonSlurper;

definition(
    name: "Alarm Decoder (Service Manager)",
    namespace: "alarmdecoder",
    author: "Nu Tech Software Solutions, Inc.",
    description: "AlarmDecoder (Service Manager v2)",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true,
    oauth: true
    ) 

preferences {

    page(name: "main_page", title: "AlarmDecoder Service Manager Setup", content: "main_page", install: true, uninstall: true) 
    
    page(name: "main", title: "AlarmDecoder Service Manager Setup", install: true, uninstall: true) {
        section("AlarmDecoders") {
            //href( name: "discover", title: "Find and Select/Install Alarm Decoders", required:false, page: "discover_devices", 
            //                        description: "Tap to Find or Select AlarmDecoder(s)" )
            href(name: "discover", title: "Discover", required: false, page: "discover_devices", description: "Tap to discover")
        }
        section("General Settings & Automations (All Devices)") {
            href( name: "settings", title: "Update Settings and Automations", description: "Tap to Show Settings...", page: "settings_page" )
        }
        section("Help") {
            href( name: "help", title: "Help with Installation/Setup", label: "Tap for Help Content", page: "help_page" )
        }
      }            
    page(name: "discover_devices", title: "Discovery started..", content: "discover_devices", refreshTimeout: 5)
    page(name: "stauth_page",        title: "Smartthings Authorization Token", description: "Tap to view or update access token")
    page(name: "stauth_update_page", title: "Smartthings Authorization Token")  
    page(name: "settings_page",      title: "AlarmDecoder Service Manager - General Settings & Automations", ) {  
        section(title: "Smart Home Monitor Settings") {
           input(name: "shmIntegration", type: "bool", required: false, defaultValue: true, title: "Integrate with Smart Home Monitor?")
           input(name: "shmChangeSHMStatus", type: "bool", required: false, defaultValue: true, title: "Automatically change Smart Home Monitor status when armed or disarmed?")
           input(name: "defaultSensorToClosed", type: "bool", required: false, defaultValue: true, title: "Default Zone Sensors to closed?")
        }
        section ("Automations - Configure Device Settings First") {
            app(name: "VZCAutomations", appName: "VirtualZoneControlApp", namespace: "alarmdecoder", title: "Add New Zone Control Automation >", multiple: true, required: false)
            app(name: "VZSAutomations", appName: "VirtualZoneControlApp", namespace: "alarmdecoder", title: "Add New Zone Sensor Automation >", multiple: true, required: false)
            app(name: "VRSAutomations", appName: "VirtualZoneControlApp", namespace: "alarmdecoder", title: "Add New Relay Sensor Automation >", multiple: true, required: false)
        } 
        section("SmartThings Authorization Settings") {
            href(name: "stauth_ref", title: "SmartThings Auth Token & URL", required: false, page: "stauth_page", description: "Tap to View/Update")
        }
        section (name: "Audio Notifications", title: "Select a device or Audio Notifications of Panel and Zone Events" ) {
            input("audioDevices", "capability.audioNotification", title: "Select Audio Notification Devices for Audio Notifications", multiple: true, required: false )
            input("musicPlayers", "capability.musicPlayer", title: "Select Music Player Devices for Audio Notifications", multiple: true, required: false )
            input("volume", title: "Set volume for playback of notifications", range: "0..100", multiple: false, required: false)
        }
        section("Help") {
            href(name: "help", title: "Help", required: false, page: "help_page", description: "How To Install & Configure The SmartApp & Device Handler >")
        }
        /***
        section("Send Notifications?") {
            input("recipients", "contact", title: "Send AlarmDecoder Notifications To", required: false)
        }
        ****/
    }
    page(name: "help_page",          title: "Help - Installation & Configuration", ) {
         section("Prior to SmartThings Installation") {
           paragraph ("1. Make sure your AlarmDecoder is installed and working properly with the webapp. The SmartThings app will not work if the AlarmDecoder webapp and API are not working. - See helpful links below")
           paragraph ("2. Make sure you have your zone schedule (wired/RF) zones entered into the AlarmDecoder webapp on the Settings/Zones page.")
           paragraph ("3. If you enabled an emulated zone expander enter any zone numbers you want to use on that expander in the web app using the appropriate addresses for that expander.")
           paragraph ("Note: Expander #1 zone addresses (9-16), Expander #2 zones (17-24), Expander #3 zones (25-32), Expander #4 zones (#33-40), Expander #5 zones (41-48)")
           paragraph ("Hint: Vista 20p/21ip panels appear to prefer expander 3, 4 and 5 due to address conflicts. For Vista panels use zone device/connection type 2 Aux Wire when programming the panel.")
         }
         section("AlarmDecoder SmartApp") {
           paragraph ("1. Tap on Discover Devices and wait for the SmartApp to find AlarmDecoder devices on your local network.")
           paragraph ("2. Select your device(s) and tap on Save to create the AlarmDecoder device(s) in SmartThings.")
           paragraph ("Note: If your device does not appear make sure it is powered on and connected to your network. If necessary login to the Raspberry Pi and make sure it has a valid IP address or ping it from another device on your local network.") 
         }
         section("AlarmDecoder Device Handler Setup") {
           paragraph ("1. <color=#ff0000>Exit the SmartApp and navigate to MyHome/Things to find your AlarmDecoder device.")
           paragraph ("2. Tap to open the device and then tap on settings (gear icon in the upper right) to open the device settings.")
           paragraph ("3. Enter or copy the API key that can be found in the AlarmDecoder webapp at https://alarmdecoder.local(or IP address)/api/keys")
           paragraph ("4. Enter the user or master security code for your alarm panel.")       
           paragraph ("5. Select your alarm panel type (ADEMCO or DSC)")  
           paragraph ("6. Set the Virtual Zone Controls switch to on if you want to create virtual zone controls for zones in the address range of an emulated zone expander.")   
           paragraph ("Note: With a VirtualZoneControl you can use a SmartApp to fault or restore a virtual zone whenever a SmartThings device such as a motion or door sensor changes state." )   
           paragraph ("7. Set the Virtual Contact Sensor switch to on if you want to create virtual contact sensors for wired/RF zones on your panel that you entered in the webapp.")          
           paragraph ("Note: With a VirtualContactSensor you can use a SmartApp to monitor a zone and trigger a SmartThings device to change state (ex. lights on) or run a routine (change scene)." )   
          /*****
           paragraph ("8. Set the Virtual Relay switch to on if you want to create virtual relays for all device addresses on the relay expander.")   
           paragraph ("Note: With a VirtualRelay you can use a SmartApp to trigger SmartThings devices or routines depending on the state of the relay." ) 
           *****/
           }
           section("Other Help Links") {
           href(name: "InstallHref", title: "AlarmDecoder Installation Documentation",
                 description: "Go to AlarmDecoder Installation Web Page",
                 required: false,
                 image: "https://github.com/cvjanick/alarmdecoder-smartthings/blob/master/README.md",
                 url: "https://github.com/cvjanick/alarmdecoder-smartthings/blob/master/README.md")
             href(name: "DocumentsHref", title: "AlarmDecoder Documentation",
                 description: "Go to AlarmDecoder Documentation Website",
                 required: false,
                 image: "https://www.alarmdecoder.com/wiki/index.php/Main_Page",
                 url: "https://www.alarmdecoder.com/wiki/index.php/Main_Page")
             href(name: "ForumHref", title: "AlarmDecoder Forums",
                 description: "Go to AlarmDecoder Forums Website",
                 required: false,
                 image: "https://www.alarmdecoder.com/forums/index.php",
                 url: "https://www.alarmdecoder.com/forums/index.php")
              }
        } // help page
}  // preferences

//******* Dynamic Pages

def main_page() {
   log.debug " --manager main_page()"
   def devs = getChildDevices()
   def cnt  = 0
   log.trace "devs=${devs}"
   
   def names = "No Devices Installed"
   
   if(devs!=null) 
       cnt=devs.size()
       
   if(cnt>0) {
       def i=1
       names=""
       devs.each { d ->
          def name = d.getLabel() + "-" + d.getName()
          names += name
          if(i<cnt)
            names += ", "
          i++
       }
    }    
       
   return dynamicPage(name: "main_page", title: "AlarmDecoder Service Manager Setup", install: true, uninstall: true) {
        section("AlarmDecoders ${cnt} Devices Installed") {
            paragraph "${names}"
        }
        section("AlarmDecoders") {
            href( name: "devices", title: "Find and Select Alarm Decoders to Install", label: "Tap to Find & Select AlarmDecoder(s)", page: "discover_devices" )
        }
        section("General Settings & Automations (All Devices)") {
            href( name: "settings", title: "Update Settings and Automations", description: "Tap to Show Settings...", page: "settings_page" )
        }
        section("Help") {
            href( name: "help", title: "Help with Installation/Setup", label: "Tap for Help Content", page: "help_page" )
        }
      }

}

def discover_devices() {
  // Dynamic page for AlarmDecoder Device Discovery and Settings
         
    int refreshInterval = 10
       
    int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
    state.refreshCount = refreshCount += 1

    def found_devices = [:]
    def options = state.devices.each { k, v ->
        log.trace "discover_devices: ${v}"
        def ip = convertHexToIP(v.ip)
        found_devices["${v.ip}:${v.port}"] = "AlarmDecoder @ ${ip}"
    }

    def numFound = found_devices.size() ?: 0
    
    log.debug "found_devices=${found_devices}"

    if (!state.subscribed) {
        log.trace "discover_devices: subscribe to location"
        subscribe(location, null, locationHandler, [filterEvents: false])
        state.subscribed = true
    }

    discover_alarmdecoder()


    return dynamicPage(name: "discover_devices", title: "Setup", nextPage: "", refreshInterval: refreshInterval, install: false, uninstall: true ) {
        section() {
            input "selectedDevices", "enum", required: false, title: "Select Device(s) to Install (${numFound} found)", multiple: true, options: found_devices
            //TODO: REMOVE THIS? YEP
           //href(name: "refreshDevices", title: "Refresh", required: false, page: "discover_alarmdecoder")
        }
    }
}

def stauth_page() {
	dynamicPage(name: "stauth_page", refreshInterval:2 )
    {
       section("SmartThings Authorization Token (used to configure WebService push notifications from AlarmDecoder to SmartThings App")
       {
                href(name: "generate", title: "Update Access Token & URL", required: false,  page: "stauth_update_page",  
                                       description: "Tap to Update the Access Token & URL")
                paragraph  title: "Authorization/Access Token", "${state.access_token}"
                paragraph  title: "Web Services Endpoint Root URL", "${state.endpoint_url}"                   
	   }
  }
}

def stauth_update_page() {
    generateAccessToken()
	dynamicPage(name: "stauth_update_page")
    {
       section("SmartThings Authorization Token (used to configure push notifications from AlarmDecoder to SmartThings")
       {
            	paragraph("SmartThings Authorization Access Token Updated.")
                paragraph  title: "Authorization/Access Token", "${state.access_token}"
                paragraph  title: "Web Services Endpoint Root URL", "${state.endpoint_url}"   
	   }
   }
}

//******* Preference/Settings Functions ****/

def discover_alarmdecoder() {
    log.trace "discover_alarmdecoder"

    if (!state.subscribed) {
        log.trace "discover_alarmdecoder: subscribing!"
        subscribe(location, null, locationHandler, [filterEvents: false])
        state.subscribed = true
    }
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:AlarmDecoder:1", physicalgraph.device.Protocol.LAN))
}

def generateAccessToken() {
    state.access_token = createAccessToken()
    state.endpoint_url = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}"  // apiUrl()
    log.debug "generateAccessToken token=${state.access_token}, url=${state.endpoint_url}"
    def dni = "${state.ip}:${state.port}"
    def ad  = getChildDevice("${state.ip}:${state.port}")
    def ad2 = findAlarmDecoder()
    log.debug "generateAccessToken ad=${ad}"
    log.debug "generateAccessToken ad2=${ad2}"
    log.debug "generateAccessToken state.ip=${state.ip}"
    log.debug "generateAccessToken state.port=${state.port}"
    log.debug "generateAccessToken state.devices=${state.devices}"
    
    def token=state.access_token
    def url=state.endpoint_url
    //log.debug "generateAccessToken token=${token}"
    //log.debug "generateAccessToken url=${url}"

    if(ad!=null) {
        ad.set_access_token(token, url)
        }
}

//******* Web Services Endpoint Mappings

mappings {
    path("/refresh") {
        action: [
            GET: "refreshWebService"
        ]
    }
    path("/setapikey") {
        action: [
            GET:  "setAPIKeyWebService",
            PUT:  "setAPIKeyWebService",
            POST: "setAPIKeyWebService",
        ]
    }
    path("/update_state") {
        action: [
            POST: "updateStateWebService"
        ]
    }
    path("/update_config") {
        action: [
            POST: "updateConfigWebService"
        ]
    }
}


//********** Predefined Callbacks **********/

def initialize() {
    log.trace "--manager initialize"

    unsubscribe()
    state.subscribed = false
    state.lastSHMStatus = null
    state.lastAlarmDecoderStatus = null

    subscribe(location, "alarmSystemStatus", shmAlarmHandler)

    unschedule()
    
    log.debug "selectedDevices=${selectedDevices}"

    // Tapping on Save in the SmartApp 
    if (selectedDevices) {
        addAlarmDecoderDevices()
    }

    scheduleRefresh()
    
    log.trace "--manager initialize END"
}

def installed() {
    log.debug "-- manager Installed with settings: ${settings}"

    initialize()
    
    log.trace "--manager generateAccessToken()"
    generateAccessToken()
    
    log.trace "--manager installed END"
}

def updated() {
    log.debug "-- manager Updated with settings: ${settings}"

    unschedule()
    initialize()
}

def uninstalled() {
    log.trace "--manager uninstalled"

    // HACK: Work around SmartThings wonky uninstall.  They claim unsubscribe is uncessary,
    //       but it is, as is the runIn() since everything is asynchronous.  Otherwise events
    //       don't get correctly unbound and the devices can't be deleted because they're in use.
    unschedule()
    unsubscribe()
    old_uninstall()
}

def childUninstalled() {
    log.trace "--manager childUninstalled (ChildApp)"
}

def old_uninstall() {
    def devices = getChildDevices()

    devices.each {
        try {
            log.trace "deleting child device: ${it.deviceNetworkId}"
            deleteChildDevice(it.deviceNetworkId)
        }
        catch(Exception e) {
            log.trace("exception while uninstalling: ${e}")
            Notify("Error deleting child device ${it.displayName}, make sure to remove all SmartApp Automations from this Device in the Device Settings and try again.")
        }
    }
    
    // UNFORTUNATELY NO WAY TO EXPLICITLY DELETE CHILD SMART APPS (Automations) FOR NOW
    /***
    def apps = getChildApps()
    def childApps = getChildApps()
    // Update the label for all child apps
    childApps.each {
    if (!it.label?.startsWith(app.name)) {
        it.updateLabel("$app.name/$it.label")
        // delete child app?
       }  
    }
    ****/
}

//********* Web Service Endpoint Handlers

def refreshWebService()
{
    log.trace "refreshWebService"
    def rsp = [name: "status", value: "ok"]
    refresh_alarmdecoders()
    return  rsp
}

def updateStateWebService() {
    log.debug "--ServiceManager.updateStateWebService()"
    def rsp  = [name: "status", value: "ok"]
    def data = request.JSON
    log.debug "updateStateWebService() data=$data"
    //log.debug "updateStateWebService() request=$request"
    //log.debug "updateStateWebservice() params=$params
    //log.debug "updateStateWebService() data: $data"
    log.debug "updateStateWebService() state.ip=${state.ip} state.port=${state.port}"  // ok
    
    def dni = "${state.ip}:${state.port}"
    def ad = findAlarmDecoder(dni)
    def events=[]
    log.debug "ad=${ad}"
    if(ad!=null)
       events=ad.update_state(data)
    else
       log.error("updateStateWebService ERROR, AlarmDecoder Device Not Found!")
       
    log.debug("updateState events.size=${events.size}")
    //log.debug("updateState events=${events}")
    
    events.each { e ->
        //log.debug("updateStateWebService event=${e}")
        if(e.name=="last_message_received" || e.name=="keypadMsg") {
          log.debug "e.name=${e.name}"
          log.debug "${e.name}.value=${e.value}" 
          ad.sendEvent(name: e.name, value: e.value, isStateChange: true, displayed: false )
        } else
            ad.sendEvent(name: e.name, value: e.value, isStateChange: true, displayed: false )
    }
    
    return
}

def updateConfigWebService() {
    log.debug "updateConfigWebService() called, updating configuration for AlarmDecoder, Zones and Relays..."
    def p = params
    def r = request
    def d = findAlarmDecoder()
    
    log.debug "params=${params}"
    log.debug "request=${request}"
    log.debug "d=${d}"
    
    d.updated()   
    def rsp  =  [name: "status", value: "ok"]
    return rsp
}

def updateDevicesWebService() {
    log.debug "updateConfigWebService() called, updating configuration for AlarmDecoder, Zones and Relays..."
    def data = response.JSON
    def d = findAlarmDecoder()
    d.update_devices_cmd() 
    def rsp  = [name: "status", value: "ok"]
    return rsp
}


/**** Handlers ****/

def locationHandler(evt) {
    // Add devices returned by discover_alarmdecoder, update based on location events

    def description = evt.description
    def hub = evt?.hubId

    log.trace "locationHandler: description=${description}"

    // Getting dni updated event description...
    def parsedEvent = parseEventMessage(description)
    parsedEvent << ["hub":hub]
    
    //log.trace "locationHandler: parsedEvent=${parsedEvent}"

    // LAN EVENTS
    //if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:MediaServer:1")) {
    if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:AlarmDecoder:1")) {
        getDevices()

        // Add device to list
        if (!(state.devices."${parsedEvent.ssdpUSN.toString()}")) {
            log.trace "locationHandler: Adding device: ${parsedEvent.ssdpUSN}"

            devices << ["${parsedEvent.ssdpUSN.toString()}": parsedEvent]
        }
        else {
            // update device info
            def d = state.devices."${parsedEvent.ssdpUSN.toString()}"
            boolean deviceChangedValues = false

            log.trace "locationHandler: device already exists.. checking for changed values"

            if (d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
                d.ip = parsedEvent.ip
                d.port = parsedEvent.port
                deviceChangedValues = true

                log.trace "locationHandler: device changed values!"
            }

            if (deviceChangedValues) {
                def children = getChildDevices()
                children.each {
                    if (it.getDeviceDataByName("mac") == parsedEvent.mac) {
                        it.setDeviceNetworkId((parsedEvent.ip + ":" + parsedEvent.port))
                        log.trace "Set new network id: " + parsedEvent.ip + ":" + parsedEvent.port
                    }
                }
            }
        }
    }

    // HTTP EVENTS
    if (parsedEvent?.body && parsedEvent?.headers) {
        log.trace "locationHandler: headers=${new String(parsedEvent.headers.decodeBase64())}"
        log.trace "locationHandler: body=${new String(parsedEvent.body.decodeBase64())}"
    }
    
} // locationHandler

def refresh() {
    refreshHandler()
    }

def refreshHandler() {
    log.trace "refreshHandler"

    refresh_alarmdecoders()
}

def delayedRefreshHandler(evt) {
    def t = evt.value.toInteger()
    log.trace "--- mmanager.delayedRefreshHandler t=${t}"
    runIn( t, refreshHandler )
}

def shmAlarmHandler(evt) {
    if (settings.shmIntegration == false)
        return

    log.trace("shmAlarmHandler -- ${evt.value}")

    if (state.lastSHMStatus != evt.value && evt.value != state.lastAlarmDecoderStatus)
    {
        getAllChildDevices().each { device ->
            if (!device.deviceNetworkId.contains(":switch"))
            {
                if (evt.value == "away")
                    device.lock()
                else if (evt.value == "stay")
                    device.on()
                else if (evt.value == "off")
                    device.off()
                else
                    log.debug "Unknown SHM alarm value: ${evt.value}"
            }
        }
    }

    state.lastSHMStatus = evt.value
}

def alarmdecoderAlarmHandler(evt) {
    if (settings.shmIntegration == false || settings.shmChangeSHMStatus == false)
        return

    log.trace("alarmdecoderAlarmHandler: ${evt.value}")

    if (state.lastAlarmDecoderStatus != evt.value && evt.value != state.lastSHMStatus)
        sendLocationEvent(name: "alarmSystemStatus", value: evt.value)

    state.lastAlarmDecoderStatus = evt.value
}


def zoneOpenHandler(evt) {
    log.debug("zoneOpenHandler: desc=${evt.value}")


    def d = getAllChildDevices().find { it.deviceNetworkId.contains("${state.ip}:${state.port}:switch${evt.value}") }
    log.trace(" -- zoneOpenHandler d.state.zoneID=${evt.value}")
    
    if (d!=null)
    {
        log.trace(" -- zoneOpenHandler device ${evt.value} found.")
        def sensorValue = "closed"
        if (settings.defaultSensorToClosed == true) 
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
    log.debug("zoneCloseHandler: desc=${evt.value}")

    def d = getChildDevices().find { it.deviceNetworkId.contains("${state.ip}:${state.port}:switch${evt.value}") }
    log.trace(" -- zoneClosedHandler d.state.zoneID=${evt.value}")

    if (d!=null)
    {
        log.trace(" -- zoneOpenHandler device ${evt.value} found.")
        def sensorValue = "open"
        if (settings.defaultSensorToClosed == true)
            sensorValue = "closed"
        //d.close()
        // If its a VirtualZoneControl (Door Control)
        d.sendEvent(name: "door", value: sensorValue, isStateChange: true, filtered: true)
        // If its a VirtualContactSensor
        d.sendEvent(name: "contact", value: sensorValue, isStateChange: true, filtered: true) 
    }
}

def relayOpenHandler(evt) {
    log.debug("relayOpenHandler: desc=${evt.value}")

    def d = getAllChildDevices().find { it.deviceNetworkId.contains("${state.ip}:${state.port}:relay${evt.value}") }
    
    if (d)
    {
        relayValue = "open"
        // Send event to the appropriate virtual device (d) for this relay event
        //d.open()
        d.sendEvent(name: "contact", value: relayValue, isStateChange: true, filtered: true) 
    }
}

def relayClosedHandler(evt) {
    log.debug("relayCloseHandler: desc=${evt.value}")

    def d = getChildDevices().find { it.deviceNetworkId.contains("${state.ip}:${state.port}:relay${evt.value}") }

    if (d)
    {
        relayValue = "closed"
        //d.close()
        d.sendEvent(name: "contact", value: relayValue, isStateChange: true, filtered: true) 
    }
}

def sendMessageHandler(evt) {
    log.trace("--- manager.sendMessageHandler")
    def type = evt.value
    def msg  = evt.description
    if(type=="alert")
      Alert(msg)
    else
      Notify(msg)
}


/*** Utility ***/

def Notify(msg) {
      sendNotification(msg)
}

def Alert(msg) {
   sendEvent(
      descriptionText: msg,
	  eventType: "ALERT",
	  name: "AlarmDecoder",
	  value: "failure",
	  displayed: true,
   )
}

def getUrn() {
    def urn = "${state.ip}:${state.port}"
}

def getDevices() {
    if(!state.devices) {
        state.devices = [:]
    }

    state.devices
}

def scheduleRefresh() {
    def minutes = 1

    def cron = "0 0/${minutes} * * * ?"
    schedule(cron, refreshHandler)
}

def refresh_alarmdecoders() {
    log.trace("refresh_alarmdecoders-")
    getAllChildDevices().each { device ->
        // Only refresh the main device.
        if (!device.deviceNetworkId.contains(":switch"))
        {
            log.trace("refresh_alarmdecoders: ${device}")
            device.refresh()
        }
    }
}

def findAlarmDecoder() {
    def ad  = getChildDevice("${state.ip}:${state.port}")
    return ad
    }
    
def findAlarmDecoder(dni) {

    def ad  = getChildDevice("${dni}")
    return ad
} 

def getZoneControlDeviceCount() {
    def cnt=findDeviceTypeCount("VirtualZoneControl")
    log.trace("getZoneControlDeviceCount=${cnt}")
    return cnt
}

def getZoneSensorDeviceCount() {
    def cnt=findDeviceTypeCount("VirtualZoneSensor")
    log.trace("getZoneSensorDeviceCount=${cnt}")
    return cnt
}
def getRelaySensorDeviceCount() {
    def cnt=findDeviceTypeCount("VirtualRelaySensor")
    log.trace("getRelaySensorDeviceCount=${cnt}")
    return cnt
}

def findDeviceTypeCount(type) {
    def c=0
    getAllChildDevices().each { d ->
        log.trace("Child device type=${d.getTypeName()}")
        if(d.getTypeName()=="${type}")
          c++
    }
    return c
}

//***** Device adds/removals


def addAlarmDecoderDevices() {
    log.trace "addAlarmDecoderDevices: ${selectedDevices}"
    
    def selected_devices = selectedDevices
    if (selected_devices instanceof java.lang.String) {
        selected_devices = [selected_devices]
    }

    selected_devices.each { dni ->
        def d = getChildDevice(dni)
        log.trace("addAlarmDecoderDevices, getChildDevice(${dni}), d=${d}")
        if (!d) {
            log.trace("devices=${devices}")
            def newDevice = state.devices.find { /*k, v -> k == dni*/ k, v -> dni == "${v.ip}:${v.port}" }
            log.trace("addAlarmDecoderDevices, devices.find=${newDevice}")

            if (newDevice!=null) {
                log.trace("addAlarmDecoderDevices, newDevice=${newDevice}")
                // Set the device network ID so that hubactions get sent to the device parser.
                state.ip   = newDevice.value.ip
                state.port = newDevice.value.port
                state.hub  = newDevice.value.hub
                state.mac  = newDevice.value.mac

                // HUB ISSUE: Set URN for the child device
                // This has shown up as null with the last hub update 19.0017
                def urn = "unknown"
                if(newDevice.value.ssdpPath!=null) {
                   urn  = newDevice.value.ssdpPath
                   urn -= "http://"
                 } else 
                     urn = "${state.ip}" + ":" + "${state.port}"

                state.urn = urn
                log.debug"addAlarmDecoderDevices, state.urn=${state.urn}"     
                
                // Generate a unique label in case there is more than one to 
                /**
                def child_cnt = 0
                def cd        = getChildDevices()
                if(cd!=null)
                   child_cnt=cd.size()
                   
                child_cnt++
                //def ad_label="AlarmDecoder #${child_cnt}"   
                **/
                def ad_label="AlarmDecoder"
                
                
                // Create device and subscribe to it's zone-on/off events.
                log.debug("addAlarmDecoderDevices, trying to add child device ${ad_label}...")
                try{
                     d = addChildDevice("alarmdecoder", 
                                        "Alarm Decoder Network Appliance", 
                                        "${state.ip}:${state.port}", 
                                        newDevice?.value.hub, 
                                        [name: "${state.ip}:${state.port}", 
                                         label: ad_label, 
                                         completedSetup: true, 
                                         data:[urn: state.urn, mac: state.mac, access_token: state.access_token, endpoint_url: state.endpoint_url]
                                        ])
                    } catch(e) {
                         Alert("Error adding AlarmDecoder Device! Error: ${e}")
                         }
                
                subscribe(d, "alarmStatus",       alarmdecoderAlarmHandler, [filterEvents: false])

            } // if 
        } // if !d
    }  // each
                // Need to listen to these events so that if a virtual zone control is open or close we send Lxx0 or Lxx1 keys
                // through alarm decoder. They will have no effect on physical or RF zones, just emulated virtual zones.
                subscribeAll()
}

def subscribeAll() {
    log.trace("--manager.subscribeAll()")

    def devices = getAllChildDevices()
    
    devices.each { d-> 
       // Need to listen to these events so that if a virtual zone control is open or close we send Lxx0 or Lxx1 keys
       // through alarm decoder. They will have no effect on physical or RF zones, just emulated virtual zones.
           subscribe(d, "send-keys",         sendKeysHandler,          [filterEvents: false])
           subscribe(d, "zone-open",         zoneOpenHandler,          [filterEvents: false])
           subscribe(d, "zone-closed",       zoneClosedHandler,        [filterEvents: false])
           subscribe(d, "relay-open",        relayOpenHandler,         [filterEvents: false])
           subscribe(d, "relay-closed",      relayClosedHandler,       [filterEvents: false])
           subscribe(d, "alarmStatus",       alarmdecoderAlarmHandler, [filterEvents: false])
           subscribe(d, "refresh",           refreshHandler,           [filterEvents: false])
           subscribe(d, "delayed-refresh",   delayedRefreshHandler,    [filterEvents: false])
           subscribe(d, "send-message",      sendMessageHandler,       [filterEvents: false])
    } // devices
}
                


def do_uninstall() {
    log.trace("-- uninstallAlarmDecoders-")
    getAllChildDevices().each { device ->
        // Only refresh the main device.

        deleteChildDevice(device.deviceNetworkId)
        /***
        if ( !device.deviceNetworkId.contains(":switch") && !device.deviceNetworkId.contains(":relay") )
        {
            log.trace("uninstallAlarmDecoders: ${device}")
            uninstallAlarmDecoder(device.deviceNetworkId)
        }
        ***/
    }
}

def uninstallAlarmDecoder(dni) {

    log.trace("---uninstallAlarmDecoder: ${dni}")
    
    def d = getChildDevice(dni)
    
    if(d) {
          try {
             log.trace " -- uninstallAlarmDecoder deleting alarmdecoder device: ${dni}"
             deleteChildDevice(d.deviceNetworkId)
        }
        catch(Exception e) {
            log.trace("exception while uninstalling AlarmDecoder: ${e}")
        }
          // uninstall the child devices for this AlarmDecoder 
          uninstallChildDevices(dni)
          }
    else
            log.trace("AlarmDecoder Device Not Found!!")
}

def addZoneControlDevices(dni, zone_list) {

    if(zone_list==null) {
        Alert( "The AlarmDecoder zone_list is empty, no devices created.") 
        return
      }
      
    log.trace(" -- addZoneControlDevices AD deviceNetworkId=${dni}")
    
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
        log.trace("-- addZoneControlDevices: d=${d}")
        
        def zone_control = null
        
        if (d==null)
        {  
            log.trace("Adding VirtualZoneControl device ${child_dni}")
            try{
                  zone_control = addChildDevice("alarmdecoder",         // namespace
                                                "VirtualZoneControl",   // type 
                                                "${child_dni}"          // device.networkId
                                                , state.hub,            // hub id
                                                [name:          "${child_dni}", 
                                                label:          "Alarm Zone #${id}", 
                                                completedSetup: true, 
                                                isComponent:    false,
                                                componentName:  "${child_dni}", 
                                                componentLabel: "Alarm Zone #${id}", 
                                                data:[ad_dni: dni, mac: state.mac]]
                                                )
                  created=true
                  log.trace("-- addZoneControl addChildDevice ok.")
                } catch(e){
                     ecnt++
                     created=false
                     //Alert("Error adding VirtualZoneControl device ${child_dni}, error: ${e}")
                     log.error("Error adding VirtualZoneControl device ${child_dni}, error: ${e}")
                }
            // Error this is showing null...
            log.trace("-- addZoneControlDevices: zone_control=${zone_control}")
            
            def controlValue = "open"
            if (settings.defaultSensorToClosed == true)
                controlValue = "closed"

            // Set default contact state.
            zone_control.sendEvent(name: "door", value: controlValue, isStateChange: true, displayed: false)
            zone_control.sendEvent(name: "zoneID", value: id, isStateChange: true, displayed: true)
            zone_control.sendEvent(name: "zoneName", value: name, isStateChange: true, displayed: true)
            zone_control.sendEvent(name: "zoneDescription", value: description, isStateChange: true, displayed: true)
        } else {
            log.trace("addZoneControlDevices - device ${child_dni} already exists, updating...")
            d.sendEvent(name: "zoneName", value: name, isStateChange: true, displayed: true)
            d.sendEvent(name: "zoneDescription", value: description, isStateChange: true, displayed: true)
            created=false
        }
        
    } // zone_list.each
    
    def cnt = getZoneControlDeviceCount()
    if(created==true)
       Notify("Created ${cnt} VirtualZoneControls for AlarmDecoder from webapp settings with ${ecnt} errors.")
    else
        Notify("Updated ${cnt} VirtualZoneControls for AlarmDecoder from webapp settings with ${ecnt} errors.")   
    
    //else
    //   Alert("Warning: error adding one or more VirtualZoneControl devices (device not created).")
       
    // Make sure we are subscribing to all events from the new devices in the service manager.
    //
    try { 
          subscribeAll()
       } catch(e) {
          log.error("--- manager - error during subscribeAll() error: ${e}" )
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
            log.trace("Adding VirtualContactSensor device ${child_dni}") 
            try{
                  sensor = addChildDevice("alarmdecoder", "VirtualZoneSensor", 
                               "${child_dni}", state.hub, [name: "${child_dni}", 
                               label: "Alarm Zone #${id}", completedSetup: true, data:[ad_dni: dni, mac: state.mac]])
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
            sensor.sendEvent(name: "contact", value: controlValue, isStateChange: true, displayed: false)
            sensor.sendEvent(name: "zoneID", value: id, isStateChange: true, displayed: true)
            sensor.sendEvent(name: "zoneName", value: name, isStateChange: true, displayed: true)
            sensor.sendEvent(name: "zoneDescription", value: description, isStateChange: true, displayed: true)
        } else {
            // Otherwise update the device name, description
            log.trace("addContactSensorDevices - device ${child_dni} already exists, updating...")
            d.sendEvent(name: "zoneName", value: name, isStateChange: true, displayed: true)
            d.sendEvent(name: "zoneDescription", value: description, isStateChange: true, displayed: true)
        }
        
    } // zone_list.each
    
    def cnt=getZoneSensorDeviceCount()
    if(created==true)
       Notify("Created ${cnt} VirtualZoneSensors for AlarmDecoder from webapp settings with ${ecnt} errors.")
    else
       Notify("Updated ${cnt} VirtualZoneSensors for AlarmDecoder from webapp settings with ${ecnt} errors.")

    try { 
          subscribeAll()
       } catch(e) {
          log.error("--- manager - error during subscribeAll() error: ${e}" )
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
                  sensor = addChildDevice("alarmdecoder", "VirtualRelaySensor", 
                               "${child_dni}", state.hub, [name: "${child_dni}", 
                               label: "Alarm Relay ${address}-${channel}", completedSetup: true, data:[ad_dni: dni, mac: state.mac]])
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
    log.trace( " --- uninstallChildDevices dni=${dni} zones=${zone_list}")
    
    if(zone_list==null)
      switch(type) {
      case "control":
           notify("No VirtualZoneControol child devices found to delete. No devices removed.")
           return
      case "sensor":
           notify("No VirtualContactSensor child devices found to delete. No devices removed.")
           return 
      default:
           notify("No deleted zone child devices found to delete. No devices removed.")
           return 
           }
      
    def cnt     = 0
    def success = false
           
    zone_list.each { z ->            
    
        def child_dni = "${state.ip}:${state.port}:switch${z.zone_id}"

        def d = getAllChildDevices().find { it.deviceNetworkId=="${child_dni}" }
        log.trace("-- uninstallChildDevices: d=${d}, child_dni=${child_dni}")
        
        if ( d !=null )
          {   
            log.trace("uninstallChildlDevices: ${d}" )
            
            try {
                 // device.unsubscribe()
                 //runIn(300, deleteChildDevice(d.deviceNetworkId))
                 // state.remove(d.id)
                 unsubscribe(d)
                 runIn(300,deleteChildDevice(d.deviceNetworkId))
                 success=true
                 //state.remove(d.id)
                 //state.devices.remove(d)
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
    
    def msg = "Uninstall Child Devices called with no valid type parameter"
    log.trace("--uninstallChildDevices cnt=${cnt}")
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
          Notify(msg)
          
      return success 
}

def uninstallRelaySensorDevices(dni, relay_list) {
    log.trace( " --- uninstallChildRelayDevices dni=${dni} relays=${relay_list}")
    
    if(relay_list==null) {
           notify("No VirtualRelaySensor child devices found to delete. No devices removed.")
           return 
           }
           
     if(dni==null) {
           notify("No dni provided, dni is null for uninstallRelaySensors.")
           return 
           }
      
    def cnt     = 0
    def success = false
           
    relay_list.each { r ->            
    
        def child_dni = "${dni}:relay${r.address}-${r.channel}"

        def d = getAllChildDevices().find { it.deviceNetworkId=="${child_dni}" }
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
          Notify(msg)
          
    return success 
}


private def parseEventMessage(String description) {
    //log.trace "parseEventMessage"
    log.trace "manager parseEventMessage, description=${description}"
    def event = [:]
    def parts = description.split(',')
    
    log.trace "manager parseEventMessage, parts=${parts}"    
    // Now getting "C0A80110:1388 updated" events so..
    if(parts==null)
      return
    
    parts.each { part ->
        part = part.trim()
        log.trace "manager parseEventMessage, part=${part}" 
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

    log.trace("manager event=${event}")
    event
}


private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}