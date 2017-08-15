/**
*  Occupied Lights
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
  name: "Occupied Lights",
  namespace: "joshnhickman",
  author: "Josh Hickman",
  description: "Determines if a room is occupied and turns on and off the lights accordingly",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  section("Control which light(s)?") {
    input "lights", "capability.switchLevel", multiple: true, title: "light(s)"
  }
  section("Monitor which motion sensor?") {
    input "motion", "capability.motionSensor", title: "motion"
  }
  section("Monitor which contact?") {
    input "contact", "capability.contactSensor", title: "contact"
  }
  section("Set light(s) to what level?") {
    input "level", "number", title: "percent"
  }
  section("Leave lights on for how long?") {
    input "onMinutes", "number", title: "minutes"
  }
  section("Turn lights off if no motion within how many seconds? (recommend 13s)") {
    input "offSeconds", "number", title: "seconds"
  }
}

def installed() {
  log.debug "installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "updated with settings: ${settings}"
  unsubscribe()
  initialize()
}

def initialize() {
  state.closedTime = now()
  state.contactState = "NOT_SET"
  subscribe(motion, "motion", motionHandler)
  subscribe(contact, "contact", contactHandler)
}

def motionHandler(evt) {
  log.debug "motion event: '${evt.descriptionText}' at ${evt.date}"

  if ("active" == evt.value) {
    if ("open" == state.contactState) {
      lightsOn()
    } else {
      log.debug "unscheduling"
      unschedule()
    }
  } else {
    if ("closed" == state.contactState && now() - state.closedTime <= offSeconds * 1000) {
      log.debug "no motion and contact closed within $offSeconds seconds; turning off lights"
      lightsOff()
    }
  }
}

def contactHandler(evt) {
  log.debug "contact event: '${evt.descriptionText}' at ${evt.date}"
  state.contactState = evt.value

  if ("open" == evt.value) {
    lightsOn()
  } else {
    state.closedTime = now()
  }
}

def lightsOn() {
  if (!lights.currentState("switch").value.contains("on")) {
    log.debug "turning lights on to $level%"
    lights*.setLevel(level)

    log.debug "scheduling lightsOff in $onMinutes minutes"
  } else {
    log.debug "lights already on; not turning on lights"
  }
  runIn(60 * onMinutes, lightsOff)
}

def lightsOff() {
  if ("inactive" == motion.currentState("motion").value) {
    log.debug "turning lights off"
    lights*.setLevel(0)
  } else {
    log.debug "motion active; rescheduling lightsOff in $onMinutes minutes"
    runIn(60 * onMinutes, lightsOff)
  }
}
