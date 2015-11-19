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
    description: "Intelligently turns on and off room lights based on motion sensors in the room and the hallway leading to the room.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Control which light(s)?") {
    	input "lights", "capability.switch", required: true, multiple: true, title: "light(s)"
    }
    section("Using which motion sensors?") {
    	input "room", "capability.motionSensor", required: true, title: "room"
        input "hall", "capability.motionSensor", required: true, title: "hall"
    }
    section("With up to how many seconds between motion events?") {
    	input "seconds", "number", required: true, default: 10, title: "seconds"
    }
    section("Force off after how many minutes of no motion?") {
    	input "minutes", "number", required: true, default: 10, title: "minutes"
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
    subscribe(hall, "motion", hallMotionHandler)
	subscribe(room, "motion", roomMotionHandler)
}

def hallMotionHandler(evt) {
	log.debug "hallMotionHandler called: $evt"
    state.lastHallMotionActive = evt.date.time
}

def roomMotionHandler(evt) {
	log.debug "roomMotionHandler called: $evt"
    if (evt.value == "active") {
    	log.debug "motion active in room"
        enterRoom()
    } else if (evt.value == "inactive") {
    	log.debug "motion inactive in room"
    	if (hall.currentState("motion").value == "active" || evt.date.time - state.lastHallMotionActive <= 1000 * seconds) {
        	log.debug "motion active in hall within $seconds seconds; lights off"
            exitRoom()
        } else {
        	log.debug "motion inactive in hall; begin $minutes minute off timer"
        	runIn(60 * minutes, checkRoomMotion)
        }
    }
}

def checkRoomMotion() {
	log.debug "checkRoomMotion called"
    def roomState = room.currentState("motion")
    if (roomState.value == "inactive") {
        if (now() - roomState.date.time >= 1000 * 60 * minutes) {
        	log.debug "no motion within $minutes minutes"
        	exitRoom()
        } else {
        	log.debug "motion within $minutes minutes; leaving lights"
        }
    } else {
    	log.debug "motion active in room; leaving lights"
    }
}

def enterRoom() {
	unschedule()
	if (!state.occupied) {
    	log.debug "not already occupied; lights on"
		state.occupied = true
        turnOnLights()
    } else {
    	log.debug "already occupied; leaving lights"
    }
}

def turnOnLights() {
	log.debug now() - getSunriseAndSunset().sunrise.time 
    if (now() - getSunriseAndSunset().sunrise.time < 0 || now() - getSunriseAndSunset().sunset.time > 0) {
        log.debug "sun is down; set color to dim red"
        lights*.setColor([hue: 100, saturation: 100, level: 40])
        lights*.on()
    } else {
        log.debug "sun is up; set color to sunlight"
        lights*.setColor([hue: 53, saturation: 91, level: 100])
        lights*.on()
    }
}

def exitRoom() {
	if (state.occupied) {
    	log.debug "previously occupied; lights off"
		state.occupied = false
    	lights*.off()
    } else {
    	log.debug "not previously occupied; leaving lights"
    }
}