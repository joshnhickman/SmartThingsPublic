/**
 *  Motion Room Lights
 *
 *  Copyright 2015 Josh Hickman
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
    name: "Motion Room Lights",
    namespace: "joshnhickman",
    author: "Josh Hickman",
    description: "Intelligently turns on and off room lights based on motion sensor in the room.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Control which light(s)?") {
    	input "lights", "capability.switchLevel", required: true, multiple: true, title: "light(s)"
    }
    section("Using which motion sensor?") {
    	input "room", "capability.motionSensor", required: true, title: "room"
    }
    section("Turn off after how many minutes of no motion?") {
    	input "minutes", "number", required: true, default: 2, title: "minutes"
    }
}

def installed() {
	log.debug "installed with settings: $settings"
	initialize()
}

def updated() {
	log.debug "updated with settings: $settings"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(room, "motion", roomMotionHandler)
    subscribe(lights, "level", roomLightHandler)
}

def roomMotionHandler(evt) {
    if (evt.value == "active") {
        if (!state.lightsOn) {
        	lightsOn()
        }
    } else {
    	if (!state.physical) {
            runIn(60 * minutes, lightsOff())
        }
    }
}

def roomLightHandler(evt) {
	log.debug "roomLightHandler called: $evt"
    state.lightsOn = evt.doubleValue > 0
    state.physical = evt.isPhysical()
    if (state.physical) {
	    unschedule()
    }
    if (!state.lightsOn) {
    	state.physical = false
    }
}

def lightsOn() {
	lights*.setLevel(30)
}

def lightsOff() {
	lights*.setLevel(0)
}