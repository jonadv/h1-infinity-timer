package com.bobindustriesbv.halo1infinitytimer

var boDbgTxt = false
var boDbg = true

const val iDbg_AddedAtStart_Seconds: Long = 0//45
const val iDbg_AddedAtStart_Minutes: Long = 0//13
const val iDbg_AddedAtStart_Hours: Long = 0
const val iDbg_AddedAtStart_Days: Long = 0//247
const val iDbg_AddedAtStart_Years: Long = 0//532
//2552, sept 19. "John-117 is awoken from cryo sleep and tasked with preventing Cortana's capture by the Covenant." https://halo.fandom.com/wiki/2552

//defaults for saving in sharedPrefFile
const val sharedPrefFile = "h1_infinity_timer_settings"
const val boSettingsAvailable_default = false
const val boFirstActivation_default = false
const val boTimerRunning_default = false
const val boBigTimeIsTotalTime_default = true
const val millisTimeStamp_StartToInfinity_default: Long = 0
const val secsToCountdownFrom_default: Long = 120 //was timSetting
const val secsAddedToCountdownStart_default: Long = 0
var boTutorialFinished = false
var boTutorialHideToasts = false
var boSettingsAvailable = boSettingsAvailable_default
var boFirstActivation = boFirstActivation_default
var boTimerRunning = boTimerRunning_default
var boBigTimeIsTotalTime = boBigTimeIsTotalTime_default
var millisTimeStamp_StartToInfinity: Long = millisTimeStamp_StartToInfinity_default
var secsToCountdownFrom: Long = secsToCountdownFrom_default
var secsAddedToCountdownStart: Long = secsAddedToCountdownStart_default
var boBackbuttonPressedOnceRecently = false

const val secsMIN: Double = 60.0
const val secsHOUR: Double = secsMIN * 60
const val secsDAY: Double = secsHOUR * 24
const val secsYEAR : Double = secsDAY * 365.25
//Julian astronomical year https://www.rapidtables.com/calc/time/seconds-in-year 365.25
//https://www.volkskrant.nl/wetenschap/afgesproken-jaar-duurt-31-556-925-445-seconden~ba74b7f2/ 31556925.445
const val secsMONTH: Double = secsYEAR

//not saved
var secsSinceStart_total: Long = 0
var secsSinceStart_down: Long = secsToCountdownFrom
var millisOneSecond: Long = 1000
var secsOneMinuteState = 60L
var secsAdding_steps = 15L
var secsMaxAddedToCount_Reset: Long = 3L
