/**
 *  Entry Lights
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
    name: "Entry Lights",
    namespace: "joshnhickman",
    author: "Josh Hickman",
    description: "Fades in the hall lights when the door is opened and there is no motion in the hall.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Control which light(s)?") {
		input "lights", "capability.switchLevel", required: true, multiple: true, title: "light(s)"
	}
    section("When which door opens?") {
    	input "door", "capability.contactSensor", required: true, title: "door"
    }
    section("Monitor which sensor for movement?") {
    	input "hall", "capability.motionSensor", required:true, title: "hall"
    }
    section("Stay on for how long?") {
    	input "minutes", "number", required: true, default: 2, title: "minutes"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
    state.automatic = false
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(door, "contact", doorContactHandler)
    subscribe(hall, "motion", hallMotionHandler)
    subscribe(lights, "switch", lightSwitchHandler)
}

def doorContactHandler(evt) {
	if ("open" == evt.value) {
        def hallState = hall.currentState("motion")
        if (hallState.value == "inactive") {
        	lightsOn()
        }
    }
}

def hallMotionHandler(evt) {
	if (state.automatic && evt.value == "inactive") {
    	runIn(60 * minutes, lightsOff)
    } else if (evt.value == "active") {
    	unschedule()
    }
}

def lightSwitchHandler(evt) {
	if (evt.isPhysical()) {
    	unschedule()
        state.automatic = false
    }
}

def lightsOn() {
	state.automatic = true
    lights*.setLevel(20)
}

def lightsOff() {
	state.automatic = false
	lights*.setLevel(0)
}