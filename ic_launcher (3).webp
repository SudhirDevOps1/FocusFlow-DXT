package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.os.BatteryManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var usageCheckJob: Job? = null

    private var audioTrack: AudioTrack? = null
    private var isPlayingAlarm = false

    private var ambientSensorListener: SensorEventListener? = null
    private var ambientTempRawValue: Float? = null
    private var ambientJob: Job? = null

    companion object {
        const val CHANNEL_ID = "KitchenTimerFocusChannel"
        const val NOTIFICATION_ID = 24680

        // Modes
        enum class ActiveMode {
            CLOCK, TIMER, STOPWATCH
        }

        // Display themes
        enum class DisplayTheme {
            RETRO_GREEN, AMBER_GLOW, CYBER_PUNK, NEON_BLUE, AMOLED_DARK, BRUSHED_STEEL
        }

        // Tones
        enum class AlarmTone {
            CLASSIC_BEEP, SERENE_CHIME, SIREN_ALERT, CHIRP_DIGITAL
        }

        // Service States (Single source of truth binding with Compose)
        val activeMode = MutableStateFlow(ActiveMode.CLOCK)
        val isRunning = MutableStateFlow(false)
        
        // Countdown values
        val countdownTotalDuration = MutableStateFlow(0L) // in milliseconds
        val countdownTimeLeft = MutableStateFlow(0L) // in milliseconds
        
        // Stopwatch values
        val stopwatchTimeElapsed = MutableStateFlow(0L) // in milliseconds
        val stopwatchLaps = MutableStateFlow<List<Long>>(emptyList())

        // Sensors & Environmental Simulation
        val simulatedTemperature = MutableStateFlow(24.5f) // in Celsius
        val simulatedHumidity = MutableStateFlow(55.0f) // in percent
        val tempCalibrationOffset = MutableStateFlow(0.0f) // user calibration offset

        // Focus & Strict mode settings
        val isStrictModeEnabled = MutableStateFlow(false)
        val isStrictModeWarningsEnabled = MutableStateFlow(true)
        val strictModeThresholdSeconds = MutableStateFlow(5) // grace threshold duration before pausing
        val strictnessLevel = MutableStateFlow("Strict") // Strict, Extremely Strict, Easy
        val databaseFocusLogs = MutableStateFlow<List<String>>(emptyList()) // Simple persistence in-memory/prefs
        val distractionDetected = MutableStateFlow<String?>(null) // Name of distracted package if detected
        val isYoutubeWhitelisted = MutableStateFlow(true) // Allow watching study lectures or not
        val customRestrictedPackages = MutableStateFlow<List<String>>(emptyList()) // Custom package names to block

        // Study vs. Distraction Stats History (Past 7 days)
        val studyMinsHistory = MutableStateFlow(listOf(35, 45, 60, 50, 25, 75, 40))
        val restrictedMinsHistory = MutableStateFlow(listOf(8, 12, 5, 14, 18, 10, 12))
        val historyDays = MutableStateFlow(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))

        // Visual and audio customization settings
        val currentTheme = MutableStateFlow(DisplayTheme.RETRO_GREEN)
        val currentAlarmTone = MutableStateFlow(AlarmTone.CLASSIC_BEEP)
        val isTickSoundEnabled = MutableStateFlow(true)
        val alarmDurationSeconds = MutableStateFlow(10) // duration to beep
        val hasUsageStatsPermission = MutableStateFlow(false)
        val isAccessibilityServiceRunning = MutableStateFlow(false)
        val ambientSoundEffect = MutableStateFlow("None") // None, White Noise, Ocean, Forest (Simulated)

        // User Profile Custom Fields (Offline-first persistence)
        val profileUserName = MutableStateFlow("")
        val profileUserAge = MutableStateFlow("")
        val profileCourseName = MutableStateFlow("")
        val profileSchoolName = MutableStateFlow("")
        val profileStudyGoal = MutableStateFlow("")
        val profileDailyTargetHours = MutableStateFlow("4")
        val profileFavSubject = MutableStateFlow("")

        // FlowTrack Subjects and Sessions state flows
        val studySubjectsFlow = MutableStateFlow<List<SubjectItem>>(emptyList())
        val studySessionsFlow = MutableStateFlow<List<SessionItem>>(emptyList())
        val activeTrackingSubjectId = MutableStateFlow<String>("")

        // Gamification States
        val profileXP = MutableStateFlow(0)
        val profileLevel = MutableStateFlow(1)
        val profileStreakDays = MutableStateFlow(1)
        val profileLastActiveDate = MutableStateFlow("")
        val completedQuestIds = MutableStateFlow<Set<String>>(emptySet())
        val activePlantedTreeType = MutableStateFlow<String>("")
        val activePlantedSubjectId = MutableStateFlow<String>("")
        val activePlantedDurationMins = MutableStateFlow<Int>(0)
        val grownTreesList = MutableStateFlow<List<String>>(emptyList())
        val forestCoins = MutableStateFlow<Int>(150)

        // Sunshine Shop items states
        val hasGoldenWateringCan = MutableStateFlow(false)
        val hasOceanWavesTrack = MutableStateFlow(false)
        val hasDeepSpaceTrack = MutableStateFlow(false)
        val superFertilizerCount = MutableStateFlow(0)

        val displayStyleMode = MutableStateFlow(0) // 0: Digital LCD, 1: Analog Clock, 2: Smart Watch, 3: Classic Pocket Watch style
        val isSoundMutedGlobal = MutableStateFlow(false) // Master sound mute toggle

        // Singleton reference for simple Binder-less checking
        var instance: TimerService? = null
            private set

        fun onAccessibilityAppChanged(packageName: String) {
            val sInstance = instance ?: return
            if (!isStrictModeEnabled.value || !isRunning.value || activeMode.value != ActiveMode.TIMER) {
                return
            }
            sInstance.handleAppDetected(packageName)
        }
    }

    private var accumulatedStudyMs = 0L
    private var accumulatedRestrictedMs = 0L
    private var firstTimeAwayMs = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        loadSettings()
        startSensorSimulation()
        startForeground(NOTIFICATION_ID, buildLiveNotification("System Initialized"))
        updateTrackingEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "START_COOLDOWN" -> startCountdown()
            "PAUSE_COOLDOWN" -> pauseTimer()
            "RESET_COOLDOWN" -> resetCountdown()
            "START_STOPWATCH" -> startStopwatch()
            "PAUSE_STOPWATCH" -> pauseStopwatch()
            "RESET_STOPWATCH" -> resetStopwatch()
            "STOP_ALARM" -> stopRingingAlarm()
        }
        updateTrackingEngine()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        usageCheckJob?.cancel()
        serviceScope.cancel()
        audioTrack?.release()
        ambientSensorListener?.let { listener ->
            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            sensorManager?.unregisterListener(listener)
        }
        instance = null
        super.onDestroy()
    }

    // --- TIMING ENGINE & ACCURATE COROUTINE TICKERS ---

    private fun updateTrackingEngine() {
        timerJob?.cancel()
        
        if (isRunning.value || isPlayingAlarm) {
            val label = when {
                isPlayingAlarm -> "Alarm Ringing!"
                activeMode.value == ActiveMode.TIMER -> "Countdown Timer Active"
                else -> "Stopwatch Active"
            }
            startForeground(NOTIFICATION_ID, buildLiveNotification(label))

            if (isRunning.value) {
                timerJob = serviceScope.launch {
                    val tickInterval = if (activeMode.value == ActiveMode.STOPWATCH) 50L else 500L
                    while (isActive) {
                        delay(tickInterval)
                        withContext(Dispatchers.Main) {
                            if (activeMode.value == ActiveMode.TIMER) {
                                val newLeft = countdownTimeLeft.value - tickInterval
                                accumulateActiveSubjectStudyTime(tickInterval)
                                if (newLeft <= 0L) {
                                    countdownTimeLeft.value = 0L
                                    isRunning.value = false
                                    timerJob?.cancel()
                                    triggerAlarmMelody()

                                    // Grant completed session XP bonus!
                                    var superApplied = false
                                    val bonusXP = if (superFertilizerCount.value > 0) {
                                        superFertilizerCount.value -= 1
                                        superApplied = true
                                        150 // +50% XP boost
                                    } else {
                                        100
                                    }
                                    profileXP.value += bonusXP
                                    if (superApplied) {
                                        addFocusLog("🏆 Earned +$bonusXP XP completion bonus (Super Fertilizer Applied!)")
                                    } else {
                                        addFocusLog("🏆 Earned +100 XP study session completion bonus!")
                                    }
                                    
                                    // Process Forest Plant growth if active
                                    if (activePlantedTreeType.value.isNotEmpty()) {
                                        val species = activePlantedTreeType.value
                                        val subjId = activePlantedSubjectId.value
                                        val durationMins = activePlantedDurationMins.value
                                        val matchingSubjName = studySubjectsFlow.value.find { it.id == subjId }?.name ?: "General Focus"
                                        val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                        
                                        val treeObj = org.json.JSONObject().apply {
                                            put("species", species)
                                            put("subjectName", matchingSubjName)
                                            put("duration", durationMins)
                                            put("status", "mature")
                                            put("timestamp", timestampStr)
                                        }
                                        val currentTrees = grownTreesList.value.toMutableList()
                                        currentTrees.add(treeObj.toString())
                                        grownTreesList.value = currentTrees
                                        
                                        val baseCoins = if (durationMins > 0) (durationMins * 2).coerceIn(10, 100) else 25
                                        val coinReward = if (hasGoldenWateringCan.value) (baseCoins * 1.5).toInt() else baseCoins
                                        forestCoins.value += coinReward
                                        
                                        val coinMsg = if (hasGoldenWateringCan.value) {
                                            "Earned +$coinReward Sunshine Coins (Golden Watering Can Active 1.5x!)"
                                        } else {
                                            "Earned +$coinReward Sunshine Coins!"
                                        }
                                        addFocusLog("🌲 Garden: Your planted $species has matured! $coinMsg")
                                        activePlantedTreeType.value = ""
                                    }

                                    checkLevelUp()
                                    saveSettings()
                                    updateTrackingEngine()
                                } else {
                                    countdownTimeLeft.value = newLeft
                                    if (isTickSoundEnabled.value && (newLeft % 1000 == 0L)) {
                                        playShortTick()
                                    }
                                }
                            } else if (activeMode.value == ActiveMode.STOPWATCH) {
                                val currentElapsed = stopwatchTimeElapsed.value + tickInterval
                                accumulateActiveSubjectStudyTime(tickInterval)
                                stopwatchTimeElapsed.value = currentElapsed
                                if (isTickSoundEnabled.value && (currentElapsed % 1000 == 0L)) {
                                    playShortTick()
                                }
                            }
                        }
                        // Update live notification
                        updateNotification()
                    }
                }
            }
        } else {
            stopForeground(true)
        }

        // Track focus in custom thread loop if strict mode is active and we are counting down
        updateFocusMonitor()
        startAmbientSynth()
    }

    private fun updateFocusMonitor() {
        usageCheckJob?.cancel()
        if (isStrictModeEnabled.value && isRunning.value && activeMode.value == ActiveMode.TIMER) {
            usageCheckJob = serviceScope.launch {
                while (isActive) {
                    delay(1500L) // check every 1.5s
                    checkForegroundPackageUsage()
                }
            }
        } else {
            distractionDetected.value = null
        }
    }

    private var subMsCollector = 0L

    private fun accumulateActiveSubjectStudyTime(ms: Long) {
        val subjId = activeTrackingSubjectId.value
        if (subjId.isEmpty()) return
        
        subMsCollector += ms
        if (subMsCollector >= 1000L) {
            val secondsToAdd = (subMsCollector / 1000L).toInt()
            subMsCollector %= 1000L
            
            val currentList = studySessionsFlow.value.toMutableList()
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            val todaySessionIndex = currentList.indexOfFirst { 
                it.subjectId == subjId && it.startTime.startsWith(todayStr) 
            }
            
            if (todaySessionIndex != -1) {
                val existing = currentList[todaySessionIndex]
                currentList[todaySessionIndex] = existing.copy(
                    actualSeconds = existing.actualSeconds + secondsToAdd,
                    updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date())
                )
            } else {
                val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val nowStr = isoFormat.format(java.util.Date())
                val endStr = isoFormat.format(java.util.Date(System.currentTimeMillis() + 3600_000))
                
                val matchingSubj = studySubjectsFlow.value.firstOrNull { it.id == subjId }
                val targetColor = matchingSubj?.color ?: "#2196F3"
                val subjName = matchingSubj?.name ?: "Subject study"
                
                val newSess = SessionItem(
                    id = java.util.UUID.randomUUID().toString(),
                    subjectId = subjId,
                    startTime = nowStr,
                    endTime = endStr,
                    plannedMinutes = 60,
                    actualSeconds = secondsToAdd,
                    colorTag = targetColor,
                    notes = "Live tracked session for $subjName",
                    tags = listOf("focus"),
                    status = "completed",
                    createdAt = nowStr,
                    updatedAt = nowStr,
                    manualEntry = false
                )
                currentList.add(newSess)
            }
            
            studySessionsFlow.value = currentList
            val nowSeconds = System.currentTimeMillis() / 1000
            if (nowSeconds % 5 == 0L) {
                saveSettings()
            }
        }
    }

    private fun accumulateStudyTime(ms: Long) {
        accumulatedStudyMs += ms
        if (accumulatedStudyMs >= 60000L) { // 1 minute
            val currentMins = accumulatedStudyMs / 60000L
            accumulatedStudyMs %= 60000L
            
            val currentList = studyMinsHistory.value.toMutableList()
            if (currentList.isNotEmpty()) {
                val index = currentList.size - 1
                currentList[index] = currentList[index] + currentMins.toInt()
                studyMinsHistory.value = currentList
                saveHistoryToPrefs()
            }

            // Award 15 XP points per minute focused!
            val gainedXP = (15 * currentMins).toInt()
            profileXP.value += gainedXP
            addFocusLog("✨ Acquired +$gainedXP XP studying...")
            checkLevelUp()
            checkStreakUpdate()
            saveSettings()
        }
    }

    private fun checkLevelUp() {
        var xp = profileXP.value
        var lvl = profileLevel.value
        var xpNeeded = lvl * 150
        var leveledUp = false
        while (xp >= xpNeeded) {
            xp -= xpNeeded
            lvl++
            xpNeeded = lvl * 150
            leveledUp = true
        }
        if (leveledUp) {
            profileLevel.value = lvl
            addFocusLog("🎉 LEVEL UP! You reached Level $lvl!")
        }
        profileXP.value = xp
    }

    private fun checkStreakUpdate() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val lastActive = profileLastActiveDate.value
        
        if (lastActive.isEmpty()) {
            profileLastActiveDate.value = todayStr
            profileStreakDays.value = 1
        } else if (lastActive != todayStr) {
            try {
                val lastDate = sdf.parse(lastActive)
                val todayDate = sdf.parse(todayStr)
                if (lastDate != null && todayDate != null) {
                    val diff = todayDate.time - lastDate.time
                    val diffDays = diff / (24 * 60 * 60 * 1000)
                    if (diffDays == 1L) {
                        profileStreakDays.value += 1
                        addFocusLog("🔥 Study Streak maintained! Day ${profileStreakDays.value}")
                    } else if (diffDays > 1L) {
                        profileStreakDays.value = 1
                        addFocusLog("❄️ Streak reset. Welcome back!")
                    }
                }
            } catch (e: Exception) {
                // Ignore parse error
            }
            profileLastActiveDate.value = todayStr
        }
    }

    private fun accumulateDistractionTime(ms: Long) {
        accumulatedRestrictedMs += ms
        if (accumulatedRestrictedMs >= 60000L) { // 1 minute
            val currentMins = accumulatedRestrictedMs / 60000L
            accumulatedRestrictedMs %= 60000L
            
            val currentList = restrictedMinsHistory.value.toMutableList()
            if (currentList.isNotEmpty()) {
                val index = currentList.size - 1
                currentList[index] = currentList[index] + currentMins.toInt()
                restrictedMinsHistory.value = currentList
                saveHistoryToPrefs()
            }
        }
    }

    // Checks background package usage permissions & active app to warn user
    private suspend fun checkForegroundPackageUsage() {
        val context = applicationContext
        val statsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (statsManager == null) {
            withContext(Dispatchers.Main) {
                hasUsageStatsPermission.value = false
            }
            return
        }

        val time = System.currentTimeMillis()
        val stats = statsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 4000, time)
        val hasPermission = stats != null && stats.isNotEmpty()
        
        withContext(Dispatchers.Main) {
            hasUsageStatsPermission.value = hasPermission
        }

        if (hasPermission && stats != null) {
            val sorted = stats.sortedByDescending { it.lastTimeUsed }
            val foregroundApp = sorted.firstOrNull()?.packageName ?: ""
            handleAppDetected(foregroundApp)
        }
    }

    // Handles app detection from both accessibility service and usage stats loop
    fun handleAppDetected(foregroundApp: String) {
        val context = applicationContext
        if (foregroundApp.isNotEmpty() && foregroundApp != context.packageName) {
            val isCustomRestricted = customRestrictedPackages.value.any { customApp ->
                customApp.isNotEmpty() && foregroundApp.contains(customApp.trim(), ignoreCase = true)
            }
            // Determine if this is a restricted app distraction
            val isDistraction = isCustomRestricted || when {
                foregroundApp.contains("instagram", ignoreCase = true) -> true
                foregroundApp.contains("tiktok", ignoreCase = true) -> true
                foregroundApp.contains("facebook", ignoreCase = true) -> true
                foregroundApp.contains("snapchat", ignoreCase = true) -> true
                foregroundApp.contains("twitter", ignoreCase = true) -> true
                foregroundApp.contains("pinterest", ignoreCase = true) -> true
                foregroundApp.contains("netflix", ignoreCase = true) -> true
                foregroundApp.contains("reels", ignoreCase = true) -> true
                foregroundApp.contains("twitch", ignoreCase = true) -> true
                foregroundApp.contains("gaming", ignoreCase = true) -> true
                foregroundApp.contains("youtube", ignoreCase = true) -> !isYoutubeWhitelisted.value
                else -> false
            }

            if (isDistraction) {
                accumulateDistractionTime(1500L) // check loop check length unit
                // Enforce direct gamified coin penalty during active study focus sessions
                if (isRunning.value && activeMode.value == ActiveMode.TIMER && forestCoins.value > 0) {
                    forestCoins.value = (forestCoins.value - 1).coerceAtLeast(0)
                    if (System.currentTimeMillis() % 15000L < 2000L) { // Limit log flood to approx once every 15s
                        addFocusLog("⚠️ Penalty: Deducted -1 Sunshine Coin for running background distraction!")
                    }
                }
            } else {
                // Just an external app (not explicitly predefined distraction, but still away)
                accumulateStudyTime(1500L / 3) // Give partial study credit for external references (dictionaries/calculators etc.)
            }

            if (firstTimeAwayMs == 0L) {
                firstTimeAwayMs = System.currentTimeMillis()
            }

            val secondsAway = (System.currentTimeMillis() - firstTimeAwayMs) / 1000L
            val threshold = strictModeThresholdSeconds.value.toLong()
            val appLabel = foregroundApp.split(".").lastOrNull()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } ?: "External App"

            serviceScope.launch(Dispatchers.Main) {
                if (secondsAway >= threshold) {
                    if (isRunning.value) {
                        isRunning.value = false
                        updateTrackingEngine()
                        addFocusLog("❌ Paused: Switched away to '$appLabel' for more than $threshold seconds!")
                        distractionDetected.value = "$appLabel (Study Paused)"
                        if (strictnessLevel.value == "Extremely Strict") {
                            triggerAlarmMelody()
                        }
                    }
                } else {
                    val secondsRemaining = threshold - secondsAway
                    if (isStrictModeWarningsEnabled.value) {
                        distractionDetected.value = "$appLabel ($secondsRemaining s left)"
                        if (secondsAway % 3 == 0L) {
                            addFocusLog("⚠️ Distraction Warning: Returned in $secondsRemaining s to keep timer running!")
                            sendSirenWarningNotification("$appLabel ($secondsRemaining s left!)")
                        }
                    }
                }
            }
        } else {
            // User is in the Study application
            firstTimeAwayMs = 0L
            if (isRunning.value && activeMode.value == ActiveMode.TIMER) {
                accumulateStudyTime(1500L)
            }
            serviceScope.launch(Dispatchers.Main) {
                distractionDetected.value = null
            }
        }
    }

    private fun addFocusLog(logMessage: String) {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val formattedLog = "[$timestamp] $logMessage"
        val curr = databaseFocusLogs.value.toMutableList()
        curr.add(0, formattedLog)
        databaseFocusLogs.value = curr.take(30) // limit to latest 30 logs
        saveFocusLogs()
    }

    // --- SOUND GENERATOR USING AUDIO_TRACK (Real hardware synthesis) ---

    private fun playShortTick() {
        if (isSoundMutedGlobal.value) return
        serviceScope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 15
                val numSamples = sampleRate * durationMs / 1000
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)
                
                // Pure tick peak clicking at 800 Hz, sliding down instantly
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    sample[i] = sin(2.0 * Math.PI * 800.0 * t) * (1.0 - (i.toDouble() / numSamples)) // decaying amplitude
                }
                
                var idx = 0
                for (dVal in sample) {
                    val valShort = (dVal * 32767).toInt().toShort()
                    generatedSnd[idx++] = (valShort.toInt() and 0x00FF).toByte()
                    generatedSnd[idx++] = ((valShort.toInt() and 0xFF00) ushr 8).toByte()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(generatedSnd, 0, generatedSnd.size)
                track.play()
                delay(durationMs.toLong() + 20)
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun triggerAlarmMelody() {
        if (isSoundMutedGlobal.value) return
        if (isPlayingAlarm) return
        isPlayingAlarm = true
        addFocusLog("🔔 Alarm triggered!")
        updateTrackingEngine()

        serviceScope.launch {
            val durationLimitMs = alarmDurationSeconds.value * 1000L
            val startTime = System.currentTimeMillis()
            
            while (isPlayingAlarm && (System.currentTimeMillis() - startTime) < durationLimitMs) {
                // Synthesize active beep melodies according to chosen alarm tone
                val patternPair = when (currentAlarmTone.value) {
                    AlarmTone.CLASSIC_BEEP -> listOf(1000.0 to 180, 0.0 to 100, 1000.0 to 180, 0.0 to 400)
                    AlarmTone.SERENE_CHIME -> listOf(440.0 to 300, 554.37 to 300, 659.25 to 400, 0.0 to 600)
                    AlarmTone.SIREN_ALERT -> listOf(880.0 to 200, 1200.0 to 200, 880.0 to 200, 1200.0 to 200)
                    AlarmTone.CHIRP_DIGITAL -> listOf(2000.0 to 50, 2500.0 to 50, 3000.0 to 50, 0.0 to 200)
                }

                for (pair in patternPair) {
                    if (!isPlayingAlarm) break
                    val frequency = pair.first
                    val duration = pair.second

                    if (frequency > 0.1) {
                        playSynthesizedTone(frequency, duration)
                    } else {
                        delay(duration.toLong())
                    }
                }
            }
            isPlayingAlarm = false
            withContext(Dispatchers.Main) {
                updateTrackingEngine()
            }
        }
    }

    private fun playSynthesizedTone(freqHz: Double, durationMs: Int) {
        if (isSoundMutedGlobal.value) return
        try {
            val sampleRate = 44100
            val numSamples = sampleRate * durationMs / 1000
            val sample = DoubleArray(numSamples)
            val generatedSnd = ByteArray(2 * numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // apply subtle fade out window to prevent pops
                val envelope = if (i > numSamples - 100) (numSamples - i).toDouble() / 100.0 else 1.0
                sample[i] = sin(2.0 * Math.PI * freqHz * t) * envelope
            }

            var idx = 0
            for (dVal in sample) {
                val valShort = (dVal * 25000).toInt().toShort() // slightly softer than maximum
                generatedSnd[idx++] = (valShort.toInt() and 0x00FF).toByte()
                generatedSnd[idx++] = ((valShort.toInt() and 0xFF00) ushr 8).toByte()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(generatedSnd, 0, generatedSnd.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 10)
            track.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRingingAlarm() {
        isPlayingAlarm = false
        updateTrackingEngine()
    }

    fun startAmbientSynth() {
        ambientJob?.cancel()
        val soundMode = ambientSoundEffect.value
        if (soundMode == "None" || isSoundMutedGlobal.value || !isRunning.value) return

        ambientJob = serviceScope.launch {
            try {
                val sampleRate = 44100
                // We create a stereo stream to support 3D/ambient sensations and Binaural Beats
                val bufferSize = sampleRate * 1 // 0.5s stereo buffer is fine, let's use 10000 samples for responsiveness
                val chunkSamples = 16000 // 16-bit stereo shorts (even number)
                val generatedSnd = ShortArray(chunkSamples)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(chunkSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                var phaseLeft = 0.0
                var phaseRight = 0.0
                val r = java.util.Random()
                
                // Low-frequency filter states for rain synthesis
                var lastNoiseVal = 0f

                while (isActive && isRunning.value && ambientSoundEffect.value == soundMode && !isSoundMutedGlobal.value) {
                    val samplesToWrite = chunkSamples / 2 // left + right pairs
                    for (i in 0 until samplesToWrite) {
                        when (soundMode) {
                            "White Noise" -> {
                                val noise = (r.nextFloat() * 2f - 1f) * 0.04f
                                generatedSnd[2 * i] = (noise * 32767).toInt().toShort()
                                generatedSnd[2 * i + 1] = (noise * 32767).toInt().toShort()
                            }
                            "Binaural Focus Beat", "Binaural Beats", "Binaural" -> {
                                // Left frequency = 120 Hz, Right frequency = 130 Hz (Creates 10 Hz Alpha wave, ideal for focus!)
                                val valueL = sin(phaseLeft) * 0.12
                                val valueR = sin(phaseRight) * 0.12
                                generatedSnd[2 * i] = (valueL * 32767).toInt().toShort()
                                generatedSnd[2 * i + 1] = (valueR * 32767).toInt().toShort()

                                phaseLeft += 2.0 * Math.PI * 120.0 / sampleRate
                                phaseRight += 2.0 * Math.PI * 130.0 / sampleRate

                                if (phaseLeft > 2.0 * Math.PI) phaseLeft -= 2.0 * Math.PI
                                if (phaseRight > 2.0 * Math.PI) phaseRight -= 2.0 * Math.PI
                            }
                            "Ocean Waves" -> {
                                // Filtered low-pass noise combined with a 6-second periodic breathing swell
                                val waveSwell = sin(System.currentTimeMillis() / 6000.0 * 2.0 * Math.PI) * 0.45 + 0.55
                                val rawNoise = (r.nextFloat() * 2f - 1f)
                                lastNoiseVal = 0.94f * lastNoiseVal + 0.06f * rawNoise
                                val volume = 0.07f * waveSwell.toFloat()
                                val oceanSample = lastNoiseVal * volume
                                generatedSnd[2 * i] = (oceanSample * 32767).toInt().toShort()
                                generatedSnd[2 * i + 1] = (oceanSample * 32767).toInt().toShort()
                            }
                            "Deep Space" -> {
                                // Low detuned sub-bass space shuttle humming drone (75 Hz Left, 76.5 Hz Right)
                                val valueL = sin(phaseLeft) * 0.13
                                val valueR = sin(phaseRight) * 0.13
                                generatedSnd[2 * i] = (valueL * 32767).toInt().toShort()
                                generatedSnd[2 * i + 1] = (valueR * 32767).toInt().toShort()

                                phaseLeft += 2.0 * Math.PI * 75.0 / sampleRate
                                phaseRight += 2.0 * Math.PI * 76.5 / sampleRate

                                if (phaseLeft > 2.0 * Math.PI) phaseLeft -= 2.0 * Math.PI
                                if (phaseRight > 2.0 * Math.PI) phaseRight -= 2.0 * Math.PI
                            }
                            else -> { // "Rain"
                                // Filtered brown/pink noise emulation for soft rainfall/ocean rustling
                                val rawNoise = (r.nextFloat() * 2f - 1f)
                                // Standard low-pass coefficient makes noise softer and brownian (rain-like)
                                lastNoiseVal = 0.92f * lastNoiseVal + 0.08f * rawNoise
                                val volume = 0.07f
                                val rainSample = lastNoiseVal * volume
                                generatedSnd[2 * i] = (rainSample * 32767).toInt().toShort()
                                generatedSnd[2 * i + 1] = (rainSample * 32767).toInt().toShort()
                            }
                        }
                    }
                    track.write(generatedSnd, 0, chunkSamples)
                }
                try {
                    track.stop()
                } catch (e: Exception) {}
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- SENSOR ENVIRONMENT SIMULATION & ACCURATE HARDWARE READERS ---
    // Reads real-time device battery thermal telemetry or ambient temperature sensor where available,
    // and implements an intelligent physical simulation fallback to deliver extreme environmental realism.
    private fun startSensorSimulation() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val ambientSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (ambientSensor != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.values?.getOrNull(0)?.let { value ->
                        ambientTempRawValue = value
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, ambientSensor, SensorManager.SENSOR_DELAY_NORMAL)
            ambientSensorListener = listener
        }

        serviceScope.launch {
            val r = Random()
            while (isActive) {
                delay(4000)
                var currentBase: Float? = ambientTempRawValue
                
                // If no ambient air temperature sensor, fallback to live hardware battery temperature
                if (currentBase == null) {
                    try {
                        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                        val batteryIntent = registerReceiver(null, filter)
                        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                        if (tempTenths > 0) {
                            currentBase = tempTenths / 10.0f
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Fallback to high-realism simulation
                if (currentBase == null) {
                    currentBase = 24.5f + (r.nextFloat() * 1.5f)
                }
                
                val delta = (r.nextFloat() - 0.5f) * 0.12f // minor natural fluctuations
                val deltaH = (r.nextFloat() - 0.5f) * 1.5f
                var finalTemp = currentBase.toFloat() + delta + tempCalibrationOffset.value
                if (finalTemp.isNaN() || finalTemp.isInfinite() || finalTemp < -10f || finalTemp > 50f) {
                    finalTemp = 24.5f + (r.nextFloat() * 1.5f)
                }
                withContext(Dispatchers.Main) {
                    simulatedTemperature.value = finalTemp
                    simulatedHumidity.value = (55.0f + deltaH).coerceIn(30.0f, 95.0f)
                }
            }
        }
    }

    // --- FOREGROUND SERVICE CONTROLS & DYNAMIC NOTIFICATIONS ---

    private fun startCountdown() {
        if (countdownTimeLeft.value <= 0L && countdownTotalDuration.value > 0L) {
            countdownTimeLeft.value = countdownTotalDuration.value
        }
        if (countdownTimeLeft.value > 0L) {
            isRunning.value = true
            addFocusLog("▶️ Timer Kitchen Countdown started")
        }
    }

    private fun pauseTimer() {
        isRunning.value = false
        addFocusLog("⏸️ Timer paused")
    }

    private fun resetCountdown() {
        isRunning.value = false
        countdownTimeLeft.value = countdownTotalDuration.value
        stopRingingAlarm()
        addFocusLog("🔄 Timer reset")
        
        if (activePlantedTreeType.value.isNotEmpty()) {
            val species = activePlantedTreeType.value
            val subjId = activePlantedSubjectId.value
            val durationMins = activePlantedDurationMins.value
            val matchingSubjName = studySubjectsFlow.value.find { it.id == subjId }?.name ?: "General Focus"
            val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            
            val treeObj = org.json.JSONObject().apply {
                put("species", species)
                put("subjectName", matchingSubjName)
                put("duration", durationMins)
                put("status", "withered")
                put("timestamp", timestampStr)
            }
            val currentTrees = grownTreesList.value.toMutableList()
            currentTrees.add(treeObj.toString())
            grownTreesList.value = currentTrees
            
            addFocusLog("🍂 Garden: Focus interrupted! Your planted $species withered and died.")
            activePlantedTreeType.value = ""
            saveSettings()
        }
    }

    private fun startStopwatch() {
        isRunning.value = true
        addFocusLog("▶️ Stopwatch lap started")
    }

    private fun pauseStopwatch() {
        isRunning.value = false
        addFocusLog("⏸️ Stopwatch paused")
    }

    private fun resetStopwatch() {
        isRunning.value = false
        stopwatchTimeElapsed.value = 0L
        stopwatchLaps.value = emptyList()
        addFocusLog("🔄 Stopwatch reset")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Kitchen Timer & Focus Engine Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildLiveNotification(contentText: String): Notification {
        val clickIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 100, clickIntent, pendingIntentFlags)

        val modeLabel = when (activeMode.value) {
            ActiveMode.CLOCK -> "Table Clock Mode"
            ActiveMode.TIMER -> "Countdown Timer Active"
            ActiveMode.STOPWATCH -> "Stopwatch Mode Active"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Digital Table Clock - $modeLabel")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true) // prevent annoying alert ringtones every second
            .build()
    }

    private fun updateNotification() {
        val content = when (activeMode.value) {
            ActiveMode.CLOCK -> {
                val clockStr = SimpleDateFormat("hh:mm:ss a dd MMM yyyy", Locale.getDefault()).format(Date())
                "Current Clock: $clockStr | Temp: ${String.format("%.1f", simulatedTemperature.value)}°C"
            }
            ActiveMode.TIMER -> {
                val seconds = countdownTimeLeft.value / 1000
                val h = seconds / 3600
                val m = (seconds % 3600) / 60
                val s = seconds % 60
                val timeStr = String.format("%02d:%02d:%02d", h, m, s)
                "Countdown left: $timeStr | Strict: ${if (isStrictModeEnabled.value) "ON" else "OFF"}"
            }
            ActiveMode.STOPWATCH -> {
                val ms = stopwatchTimeElapsed.value
                val s = ms / 1000
                val m = s / 60
                val formattedStr = String.format("%02d:%02d.%02d", m, s % 60, (ms % 1000) / 10)
                "Stopwatch: $formattedStr | Laps captured: ${stopwatchLaps.value.size}"
            }
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, buildLiveNotification(content))
    }

    private fun sendSirenWarningNotification(appName: String) {
        val clickIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 102, clickIntent, pendingIntentFlags)

        val warningNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⛔ DISTRACTION ENFORCED!")
            .setContentText("You opened '$appName'! Get back to study immediately.")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID + 1, warningNotification)
    }

    // --- SETTINGS LOCAL PERSISTENCE (Lightweight & 100% Reliable SharedPreferences) ---

    private fun loadSettings() {
        val prefs = getSharedPreferences("digital_clock_timer_prefs", Context.MODE_PRIVATE)
        
        val modeOrdinal = prefs.getInt("active_mode", ActiveMode.CLOCK.ordinal)
        activeMode.value = ActiveMode.values().getOrElse(modeOrdinal) { ActiveMode.CLOCK }
        
        currentTheme.value = DisplayTheme.values().getOrElse(prefs.getInt("current_theme", DisplayTheme.RETRO_GREEN.ordinal)) { DisplayTheme.RETRO_GREEN }
        currentAlarmTone.value = AlarmTone.values().getOrElse(prefs.getInt("alarm_tone", AlarmTone.CLASSIC_BEEP.ordinal)) { AlarmTone.CLASSIC_BEEP }
        
        isTickSoundEnabled.value = prefs.getBoolean("tick_sound", true)
        isStrictModeEnabled.value = prefs.getBoolean("strict_mode", false)
        isStrictModeWarningsEnabled.value = prefs.getBoolean("strict_mode_warnings", true)
        strictModeThresholdSeconds.value = prefs.getInt("strict_mode_threshold", 5)
        strictnessLevel.value = prefs.getString("strictness_level", "Strict") ?: "Strict"
        countdownTotalDuration.value = prefs.getLong("countdown_total_dur", 600000L) // default 10 minutes
        countdownTimeLeft.value = countdownTotalDuration.value
        isYoutubeWhitelisted.value = prefs.getBoolean("youtube_whitelisted", true)
        val customRestrictedStr = prefs.getString("custom_restricted_packages", "") ?: ""
        customRestrictedPackages.value = if (customRestrictedStr.isEmpty()) emptyList() else customRestrictedStr.split(",")
        tempCalibrationOffset.value = prefs.getFloat("temp_offset", 0.0f)
        alarmDurationSeconds.value = prefs.getInt("alarm_duration", 10)
        ambientSoundEffect.value = prefs.getString("ambient_sound", "None") ?: "None"
        displayStyleMode.value = prefs.getInt("display_style_mode", 0)
        isSoundMutedGlobal.value = prefs.getBoolean("is_sound_muted_global", false)

        // Load User Profile details
        profileUserName.value = prefs.getString("prof_user_name", "") ?: ""
        profileUserAge.value = prefs.getString("prof_user_age", "") ?: ""
        profileCourseName.value = prefs.getString("prof_course_name", "") ?: ""
        profileSchoolName.value = prefs.getString("prof_school_name", "") ?: ""
        profileStudyGoal.value = prefs.getString("prof_study_goal", "") ?: ""
        profileDailyTargetHours.value = prefs.getString("prof_daily_target_hours", "4") ?: "4"
        profileFavSubject.value = prefs.getString("prof_fav_subject", "") ?: ""

        // Load Gamification details
        profileXP.value = prefs.getInt("profile_xp", 0)
        profileLevel.value = prefs.getInt("profile_level", 1)
        profileStreakDays.value = prefs.getInt("profile_streak_days", 1)
        profileLastActiveDate.value = prefs.getString("profile_last_active_date", "") ?: ""
        val completedQuestsStr = prefs.getString("completed_quest_ids", "") ?: ""
        completedQuestIds.value = if (completedQuestsStr.isEmpty()) emptySet() else completedQuestsStr.split(",").toSet()
        activePlantedTreeType.value = prefs.getString("active_planted_tree_type", "") ?: ""
        activePlantedSubjectId.value = prefs.getString("active_planted_subject_id", "") ?: ""
        activePlantedDurationMins.value = prefs.getInt("active_planted_duration_mins", 0)
        forestCoins.value = prefs.getInt("forest_coins", 150)
        hasGoldenWateringCan.value = prefs.getBoolean("has_golden_watering_can", false)
        hasOceanWavesTrack.value = prefs.getBoolean("has_ocean_waves_track", false)
        hasDeepSpaceTrack.value = prefs.getBoolean("has_deep_space_track", false)
        superFertilizerCount.value = prefs.getInt("super_fertilizer_count", 0)
        
        val grownTreesStr = prefs.getString("grown_trees_list", "[]") ?: "[]"
        try {
            val arr = org.json.JSONArray(grownTreesStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            grownTreesList.value = list
        } catch(e: Exception) {
            grownTreesList.value = emptyList()
        }

        val studyHistoryStr = prefs.getString("study_mins_history", "35,45,60,50,25,75,40") ?: "35,45,60,50,25,75,40"
        studyMinsHistory.value = studyHistoryStr.split(",").map { it.toIntOrNull() ?: 0 }

        val restrictedHistoryStr = prefs.getString("restricted_mins_history", "8,12,5,14,18,10,12") ?: "8,12,5,14,18,10,12"
        restrictedMinsHistory.value = restrictedHistoryStr.split(",").map { it.toIntOrNull() ?: 0 }

        // Load logs
        val logSet = prefs.getStringSet("focus_logs", emptySet()) ?: emptySet()
        databaseFocusLogs.value = logSet.toList().sortedByDescending { it }

        // Load FlowTrack lists
        val subjectsJsonStr = prefs.getString("flowtrack_subjects", "[]") ?: "[]"
        val sessionsJsonStr = prefs.getString("flowtrack_sessions", "[]") ?: "[]"
        activeTrackingSubjectId.value = prefs.getString("flowtrack_active_subj_id", "") ?: ""
        
        try {
            val subArray = org.json.JSONArray(subjectsJsonStr)
            val tempSubj = mutableListOf<SubjectItem>()
            for (i in 0 until subArray.length()) {
                tempSubj.add(SubjectItem.fromJsonObject(subArray.getJSONObject(i)))
            }
            studySubjectsFlow.value = tempSubj
        } catch(e: Exception) { e.printStackTrace() }
        
        try {
            val sesArray = org.json.JSONArray(sessionsJsonStr)
            val tempSess = mutableListOf<SessionItem>()
            for (i in 0 until sesArray.length()) {
                tempSess.add(SessionItem.fromJsonObject(sesArray.getJSONObject(i)))
            }
            studySessionsFlow.value = tempSess
        } catch(e: Exception) { e.printStackTrace() }
    }

    fun saveSettings() {
        val prefs = getSharedPreferences("digital_clock_timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("active_mode", activeMode.value.ordinal)
            putInt("current_theme", currentTheme.value.ordinal)
            putInt("alarm_tone", currentAlarmTone.value.ordinal)
            putBoolean("tick_sound", isTickSoundEnabled.value)
            putBoolean("strict_mode", isStrictModeEnabled.value)
            putBoolean("strict_mode_warnings", isStrictModeWarningsEnabled.value)
            putInt("strict_mode_threshold", strictModeThresholdSeconds.value)
            putString("strictness_level", strictnessLevel.value)
            putLong("countdown_total_dur", countdownTotalDuration.value)
            putBoolean("youtube_whitelisted", isYoutubeWhitelisted.value)
            putString("custom_restricted_packages", customRestrictedPackages.value.joinToString(","))
            putFloat("temp_offset", tempCalibrationOffset.value)
            putInt("alarm_duration", alarmDurationSeconds.value)
            putString("ambient_sound", ambientSoundEffect.value)
            putInt("display_style_mode", displayStyleMode.value)
            putBoolean("is_sound_muted_global", isSoundMutedGlobal.value)

            // Save User Profile details
            putString("prof_user_name", profileUserName.value)
            putString("prof_user_age", profileUserAge.value)
            putString("prof_course_name", profileCourseName.value)
            putString("prof_school_name", profileSchoolName.value)
            putString("prof_study_goal", profileStudyGoal.value)
            putString("prof_daily_target_hours", profileDailyTargetHours.value)
            putString("prof_fav_subject", profileFavSubject.value)

            // Save Gamification details
            putInt("profile_xp", profileXP.value)
            putInt("profile_level", profileLevel.value)
            putInt("profile_streak_days", profileStreakDays.value)
            putString("profile_last_active_date", profileLastActiveDate.value)
            putString("completed_quest_ids", completedQuestIds.value.joinToString(","))
            putString("active_planted_tree_type", activePlantedTreeType.value)
            putString("active_planted_subject_id", activePlantedSubjectId.value)
            putInt("active_planted_duration_mins", activePlantedDurationMins.value)
            putInt("forest_coins", forestCoins.value)
            putBoolean("has_golden_watering_can", hasGoldenWateringCan.value)
            putBoolean("has_ocean_waves_track", hasOceanWavesTrack.value)
            putBoolean("has_deep_space_track", hasDeepSpaceTrack.value)
            putInt("super_fertilizer_count", superFertilizerCount.value)
            
            val trsArr = org.json.JSONArray()
            grownTreesList.value.forEach { trsArr.put(it) }
            putString("grown_trees_list", trsArr.toString())

            // Save FlowTrack lists
            val subjectsArr = org.json.JSONArray()
            studySubjectsFlow.value.forEach { subjectsArr.put(it.toJsonObject()) }
            putString("flowtrack_subjects", subjectsArr.toString())

            val sessionsArr = org.json.JSONArray()
            studySessionsFlow.value.forEach { sessionsArr.put(it.toJsonObject()) }
            putString("flowtrack_sessions", sessionsArr.toString())
            
            putString("flowtrack_active_subj_id", activeTrackingSubjectId.value)

            apply()
        }
    }

    fun saveHistoryToPrefs() {
        val prefs = getSharedPreferences("digital_clock_timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("study_mins_history", studyMinsHistory.value.joinToString(","))
            putString("restricted_mins_history", restrictedMinsHistory.value.joinToString(","))
            apply()
        }
    }

    fun saveFocusLogs() {
        val prefs = getSharedPreferences("digital_clock_timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("focus_logs", databaseFocusLogs.value.toSet()).apply()
    }
}

data class SubjectItem(
    val id: String,
    val name: String,
    val color: String,
    val createdAt: String
) {
    fun toJsonObject(): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put("id", id)
            put("name", name)
            put("color", color)
            put("createdAt", createdAt)
        }
    }
    companion object {
        fun fromJsonObject(obj: org.json.JSONObject): SubjectItem {
            return SubjectItem(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                name = obj.optString("name", "Untitled"),
                color = obj.optString("color", "#2196F3"),
                createdAt = obj.optString("createdAt", "")
            )
        }
    }
}

data class SessionItem(
    val id: String,
    val subjectId: String,
    val startTime: String,
    val endTime: String,
    val plannedMinutes: Int,
    val actualSeconds: Int,
    val colorTag: String,
    val notes: String,
    val tags: List<String>,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val manualEntry: Boolean
) {
    fun toJsonObject(): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put("id", id)
            put("subjectId", subjectId)
            put("startTime", startTime)
            put("endTime", endTime)
            put("plannedMinutes", plannedMinutes)
            put("actualSeconds", actualSeconds)
            put("colorTag", colorTag)
            put("notes", notes)
            val tagsArr = org.json.JSONArray()
            tags.forEach { tagsArr.put(it) }
            put("tags", tagsArr)
            put("status", status)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
            put("manualEntry", manualEntry)
        }
    }
    companion object {
        fun fromJsonObject(obj: org.json.JSONObject): SessionItem {
            val tagsList = mutableListOf<String>()
            val tagsArr = obj.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    tagsList.add(tagsArr.optString(i))
                }
            }
            return SessionItem(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                subjectId = obj.optString("subjectId", ""),
                startTime = obj.optString("startTime", ""),
                endTime = obj.optString("endTime", ""),
                plannedMinutes = obj.optInt("plannedMinutes", 0),
                actualSeconds = obj.optInt("actualSeconds", 0),
                colorTag = obj.optString("colorTag", ""),
                notes = obj.optString("notes", ""),
                tags = tagsList,
                status = obj.optString("status", "planned"),
                createdAt = obj.optString("createdAt", ""),
                updatedAt = obj.optString("updatedAt", ""),
                manualEntry = obj.optBoolean("manualEntry", false)
            )
        }
    }
}
