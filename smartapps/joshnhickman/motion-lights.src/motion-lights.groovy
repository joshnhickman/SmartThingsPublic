/**
 *  Motion Lights
 *
 *  Copyright 2016 Josh Hickman
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
 */
definition(
    name: "Motion Lights",
    namespace: "joshnhickman",
    author: "Josh Hickman",
    description: "does stuff",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Control which light(s)?") {
		input "lights", "capability.switchLevel", required: true, multiple: true, title: "light(s)"
	}
    section("Monitor which motion sensor?") {
    	input "room", "capability.motionSensor", required: true, title: "room"
    }
    section("Stay on for how long?") {
    	input "minutes", "number", default: 2, title: "minutes"
    }
    section("Set to what level?") {
    	input "level", "number", default: 50, title: "percent"
    }
}

def installed() {
	log.debug "installed with settings: ${settings}"
	initialize()
    state.manual = false
    state.automatic = false
    state.expectSwitchEvent = false
}

def updated() {
	log.debug "updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(lights, "switch.off", lightSwitchHandler)
    subscribe(lights, "level", lightSwitchHandler)
    subscribe(room, "motion", roomMotionHandler)
}

def lightSwitchHandler(evt) {
	if (state.expectSwitchEvent) {
    	log.debug "digital switch event: ${evt.descriptionText}"
    	state.expectSwitchEvent = false
   	} else {
    	log.debug "physical switch event: ${evt.descriptionText}"
        log.debug "unscheduling"
        unschedule()
        if ("on" == evt.value || ("off" != evt.value && "0" != evt.value)) {
        	log.debug "manual control ON"
        	state.manual = true
            state.automatic = false
        }
    }
    if (("off" == evt.value || "0" == evt.value) && state.manual) {
    	log.debug "manual control OFF"
        state.manual = false
    }
    log.debug "value: ${evt.value}"
	log.debug "physical: ${evt.isPhysical()}"
}

def roomMotionHandler(evt) {
	log.debug "motion event: ${evt.descriptionText}"
	if (state.manual) {
    	log.debug "manual control on; ignoring automation"
    } else {
        if ("active" == evt.value) {
  			log.debug "unscheduling"
            unschedule()
            if (!state.automatic) {
            	lightsOn()
            }
        } else if ("inactive" == evt.value) {
        	log.debug "scheduling lights off in ${minutes} minutes"
            runIn(60 * minutes, lightsOff)
        }
    }
}

def lightsOn() {
    log.debug "setting lights to ${level}%"
	state.expectSwitchEvent = true
    state.automatic = true
    lights*.setLevel(level)
}

def lightsOff() {
    log.debug "turning lights off"
    state.expectSwitchEvent = true
    state.automatic = false
    lights*.off()
}