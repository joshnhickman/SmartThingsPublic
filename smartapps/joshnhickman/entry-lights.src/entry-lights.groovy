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
    input "lights", "capability.switchLevel", multiple: true, title: "light(s)"
  }
  section("Monitor which motion sensor??") {
    input "motion", "capability.motionSensor", title: "motion"
  }
  section("Monitor which contact") {
    input "contact", "capability.contactSensor", title: "contact"
  }
  section("Set light(s) to what level?") {
    input "level", "number", title: "percent"
  }
  section("Stay on for how long with no motion?") {
    input "minutes", "number", title: "minutes"
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
  state.arriving = false
  state.pending = false
  subscribe(contact, "contact.open", contactOpenHandler)
  subscribe(motion, "motion", motionHandler)
}

def contactOpenHandler(evt) {
  log.debug "contact open event: '${evt.descriptionText}' at ${evt.date}"
  if ("inactive" == motion.currentState("motion").value && !lights.currentState("switch").value.contains("on")) {
    state.arriving = true
    lights*.setLevel(level)
    state.pending = true
    runIn(60 * 30, lightsOff)
  }
}

def motionHandler(evt) {
  if (state.arriving) {
    if ("inactive" == evt.value) {
      state.pending = true
      runIn(60 * minutes, lightsOff)
    } else {
      state.pending = false
    }
  }
}

def lightsOff() {
  if (state.pending && state.arriving) {
    lights*.setLevel(0)
    state.pending = false
    state.arriving = false
  }
}
