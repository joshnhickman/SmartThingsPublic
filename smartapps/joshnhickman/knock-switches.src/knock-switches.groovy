/**
*  Knock switches
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
  name: "Knock switches",
  namespace: "joshnhickman",
  author: "Josh Hickman",
  description: "Blink switches when someone knocks on a door",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section("When there's a knock at the") {
    input "knock", "capability.accelerationSensor", title: "door"
  }
  section("Blink which light(s)?") {
    input "switches", "capability.switch", title: "light(s)"
    input "numFlashes", "number", title: "this number of times (default 3)", required: false
    // input "onLights", "capability.switch", title: "light(s) when on"
  }
  section("Time settings in milliseconds (optional)") {
    input "onFor", "number", title: "On for (default 1000)", required: false
    input "offFor", "number", title: "Off for (default 1000)", required: false
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"

  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"

  unsubscribe()
  initialize()
}

def initialize() {
	subscribe(knock, "acceleration.active", knockHandler)
}

def knockHandler(evt) {
  log.debug "acceleration $evt.value"
  flashLights()
}

def flashLights() {
  def doFlash = true
  def onFor = onFor ?: 1000
  def offFor = offFor ?: 1000
  def numFlashes = numFlashes ?: 3

  log.debug "LAST ACTIVATED IS: ${state.lastActivated}"
  if (state.lastActivated) {
    def elapsed = now() - state.lastActivated
    def sequenceTime = (numFlashes + 1) * (onFor + offFor)
    doFlash = elasped > sequenceTime
    log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
  }

  if (doFlash) {
    log.debug "FLASHING $numFlashes times"
    state.lastActivated = now()
    log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
    def initialActionOn = switches.collect{it.currentSwitch != "on"}
    def delay = 0L
    numFlashes.times {
      log.trace "Switch on after $delay msec"
      switches.eachWithIndex {s, i ->
        if (initialActionOn[i]) {
          s.on(delay: delay)
        }
        else {
          s.off(delay: delay)
        }
      }
      delay += onFor
      log.trace "Switch off after $delay msec"
      switches.eachWithIndex {s, i ->
        if (initialActionOn[i]) {
          s.off(delay: delay)
        } else {
          s.on(delay: delay)
        }
      }
      delay += offFor
    }
  }
}
