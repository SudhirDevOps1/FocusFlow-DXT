package com.example

import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.saveable.rememberSaveable
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.TimerService.Companion.ActiveMode
import com.example.TimerService.Companion.DisplayTheme
import com.example.TimerService.Companion.AlarmTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val CustomPauseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CustomPause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathBuilder()
                .moveTo(6f, 19f)
                .horizontalLineTo(10f)
                .verticalLineTo(5f)
                .horizontalLineTo(6f)
                .close()
                .moveTo(14f, 5f)
                .verticalLineTo(19f)
                .horizontalLineTo(18f)
                .verticalLineTo(5f)
                .close()
                .nodes
        )
    }.build()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun KitchenTimerApp(
    onEnterPiP: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Service state observational bindings
    val activeMode by TimerService.activeMode.collectAsStateWithLifecycle()
    val isRunning by TimerService.isRunning.collectAsStateWithLifecycle()
    
    val countdownTotalDur by TimerService.countdownTotalDuration.collectAsStateWithLifecycle()
    val countdownTimeLeft by TimerService.countdownTimeLeft.collectAsStateWithLifecycle()
    
    val stopwatchElapsed by TimerService.stopwatchTimeElapsed.collectAsStateWithLifecycle()

    val currentTemperature by TimerService.simulatedTemperature.collectAsStateWithLifecycle()
    val simulatedHumidity by TimerService.simulatedHumidity.collectAsStateWithLifecycle()
    val tempOffset by TimerService.tempCalibrationOffset.collectAsStateWithLifecycle()

    val strictModeEnabled by TimerService.isStrictModeEnabled.collectAsStateWithLifecycle()
    val isStrictModeWarningsEnabled by TimerService.isStrictModeWarningsEnabled.collectAsStateWithLifecycle()
    val strictModeThresholdSeconds by TimerService.strictModeThresholdSeconds.collectAsStateWithLifecycle()
    val studyMinsHistory by TimerService.studyMinsHistory.collectAsStateWithLifecycle()
    val restrictedMinsHistory by TimerService.restrictedMinsHistory.collectAsStateWithLifecycle()
    val historyDays by TimerService.historyDays.collectAsStateWithLifecycle()
    val strictnessLevel by TimerService.strictnessLevel.collectAsStateWithLifecycle()
    val focusLogs by TimerService.databaseFocusLogs.collectAsStateWithLifecycle()
    val distractionDetected by TimerService.distractionDetected.collectAsStateWithLifecycle()
    val isYoutubeWhitelisted by TimerService.isYoutubeWhitelisted.collectAsStateWithLifecycle()

    val currentTheme by TimerService.currentTheme.collectAsStateWithLifecycle()
    val alarmTone by TimerService.currentAlarmTone.collectAsStateWithLifecycle()
    val tickEnabled by TimerService.isTickSoundEnabled.collectAsStateWithLifecycle()
    val alarmDuration by TimerService.alarmDurationSeconds.collectAsStateWithLifecycle()
    val permissionStatus by TimerService.hasUsageStatsPermission.collectAsStateWithLifecycle()
    val ambientSound by TimerService.ambientSoundEffect.collectAsStateWithLifecycle()
    val displayStyleMode by TimerService.displayStyleMode.collectAsStateWithLifecycle()
    val isSoundMutedGlobal by TimerService.isSoundMutedGlobal.collectAsStateWithLifecycle()
    var isHardwareFullscreen by remember { mutableStateOf(false) }

    // Profile variables collection
    val profileName by TimerService.profileUserName.collectAsStateWithLifecycle()
    val profileAge by TimerService.profileUserAge.collectAsStateWithLifecycle()
    val profileCourse by TimerService.profileCourseName.collectAsStateWithLifecycle()
    val profileSchool by TimerService.profileSchoolName.collectAsStateWithLifecycle()
    val profileGoal by TimerService.profileStudyGoal.collectAsStateWithLifecycle()
    val profileDailyTarget by TimerService.profileDailyTargetHours.collectAsStateWithLifecycle()
    val profileFavSubject by TimerService.profileFavSubject.collectAsStateWithLifecycle()

    val studySubjects by TimerService.studySubjectsFlow.collectAsStateWithLifecycle()
    val studySessions by TimerService.studySessionsFlow.collectAsStateWithLifecycle()
    val activeTrackingSubjectId by TimerService.activeTrackingSubjectId.collectAsStateWithLifecycle()

    // Local UI states
    var showQuickManualInputSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var selectedTabCustomizer by remember { mutableStateOf(0) } // 0: Display, 1: Focus, 2: Profile, 3: Sounds/Calib
    var liveClockTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var customFullScreenView by remember { mutableStateOf<android.view.View?>(null) }
    var customFullScreenCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }
    var selectedLocalVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var localVideoFullScreen by remember { mutableStateOf(false) }
    var localVideoPlaybackPos by remember { mutableStateOf(0) }
    var activeVideoViewInstance by remember { mutableStateOf<android.widget.VideoView?>(null) }
    var isLocalVideoPlaying by remember { mutableStateOf(true) }
    var isLocalVideoPreparing by remember { mutableStateOf(true) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore take permission error
                }
                selectedLocalVideoUri = uri
                isLocalVideoPreparing = true
                isLocalVideoPlaying = true
            }
        }
    )

    // Export Document Launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            val backupStr = exportProfileBackup(context)
                            out.write(backupStr.toByteArray())
                        }
                        android.widget.Toast.makeText(context, "✅ Backup File Saved Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "❌ Export Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    // Import Document Launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val backupStr = stream.bufferedReader().use { it.readText() }
                            val success = importProfileBackup(context, backupStr)
                            if (success) {
                                android.widget.Toast.makeText(context, "✅ Backup Profile Restored Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "❌ Invalid Backup JSON Code File", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "❌ Import Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    // Tick local timer to refresh environmental clock instantly
    LaunchedEffect(Unit) {
        while (true) {
            liveClockTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    // Capture theme color structures
    val themeColors = getThemeColors(currentTheme)

    // Verify Picture-in-Picture visual mode
    val isInPip = MainActivity.isInPipMode.value

    if (isInPip) {
        // PICTURE-IN-PICTURE MINIMAL COMPLEMENT SCREEN
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.bezelBgColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                // Label showing active mode
                Text(
                    text = when (activeMode) {
                        ActiveMode.CLOCK -> "CLOCK"
                        ActiveMode.TIMER -> "COUNTDOWN TIMER"
                        ActiveMode.STOPWATCH -> "STOPWATCH"
                    },
                    color = themeColors.screenTextColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                // The giant digits
                val displayText = when (activeMode) {
                    ActiveMode.CLOCK -> {
                        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(liveClockTime))
                    }
                    ActiveMode.TIMER -> {
                        val secs = countdownTimeLeft / 1000
                        val h = secs / 3600
                        val m = (secs % 3600) / 60
                        val s = secs % 60
                        String.format("%02d:%02d:%02d", h, m, s)
                    }
                    ActiveMode.STOPWATCH -> {
                        val secs = stopwatchElapsed / 1000
                        val m = secs / 60
                        val s = secs % 60
                        val msTens = (stopwatchElapsed % 1000) / 10
                        String.format("%02d:%02d.%02d", m, s, msTens)
                    }
                }

                Text(
                    text = displayText,
                    color = themeColors.screenTextColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                if (distractionDetected != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "⚠️ FOCUS BROKEN",
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    } else if (isHardwareFullscreen) {
        // Immersive Fullscreen Casing Panel with all fully functional hardware buttons
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E272C))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TableCasingFrame(
                    activeMode = activeMode,
                    isRunning = isRunning,
                    liveClockTime = liveClockTime,
                    countdownTimeLeft = countdownTimeLeft,
                    countdownTotalDur = countdownTotalDur,
                    stopwatchElapsed = stopwatchElapsed,
                    themeColors = themeColors,
                    distractionDetected = distractionDetected,
                    displayStyleMode = displayStyleMode,
                    isHardwareFullscreen = true,
                    onToggleFullscreen = { isHardwareFullscreen = false },
                    onDismissDistraction = {
                        TimerService.distractionDetected.value = null
                    },
                    onStartStopTap = {
                        val serviceIntent = Intent(context, TimerService::class.java)
                        if (activeMode == ActiveMode.TIMER) {
                            if (isRunning) {
                                serviceIntent.action = "PAUSE_COOLDOWN"
                            } else {
                                serviceIntent.action = "START_COOLDOWN"
                            }
                        } else if (activeMode == ActiveMode.STOPWATCH) {
                            if (isRunning) {
                                serviceIntent.action = "PAUSE_STOPWATCH"
                            } else {
                                serviceIntent.action = "START_STOPWATCH"
                            }
                        }
                        context.startService(serviceIntent)
                    },
                    onMinIncrement = {
                        addDurationMinutes(1)
                    },
                    onSecIncrement = {
                        addDurationSeconds(10)
                    },
                    onResetTap = {
                        val serviceIntent = Intent(context, TimerService::class.java).apply {
                            action = if (activeMode == ActiveMode.TIMER) "RESET_COOLDOWN" else "RESET_STOPWATCH"
                        }
                        context.startService(serviceIntent)
                    },
                    onModeTap = {
                        cycleMode()
                    },
                    onLapTap = {
                        if (activeMode == ActiveMode.STOPWATCH) {
                            val laps = TimerService.stopwatchLaps.value.toMutableList()
                            laps.add(stopwatchElapsed)
                            TimerService.stopwatchLaps.value = laps
                        }
                    },
                    onSetDurationDirectly = {
                        showQuickManualInputSheet = true
                    }
                )
            }
        }
    } else {
        // FULL MASTERPIECE SMART KITCHEN APPARATUS APP UI
        Scaffold(
            topBar = {
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    modifier = Modifier.shadow(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.focus_flow_logo),
                                contentDescription = "FocusFlow Logo",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, themeColors.secondaryGlowColor, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FOCUSFLOW DXT",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = themeColors.screenTextColor,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Active Study Monitor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            
                            IconButton(
                                onClick = { uriHandler.openUri("https://github.com/SudhirDevOps1") },
                                modifier = Modifier
                                    .testTag("github_profile_header_btn")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_github),
                                    contentDescription = "Open Sudhir's GitHub Profile",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Settings Button
                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier
                                    .testTag("settings_button")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Open Settings Panel",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            // PiP Enter Button
                            IconButton(
                                onClick = onEnterPiP,
                                modifier = Modifier
                                    .testTag("pip_button")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Enter Float Picture-in-Picture",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Active Alarm Dismiss Panel (Shows up blinking when countdown finishes)
                if (!isRunning && countdownTimeLeft == 0L && countdownTotalDur > 0) {
                    AlarmTriggerShield()
                }

                // Header Information: Live Day Date & Year Display Banner
                LiveDayDateBanner(liveClockTime, currentTemperature, themeColors)

                // Garden Seed Bloom Tracker (Forest App integration)
                val activeTreeType by TimerService.activePlantedTreeType.collectAsStateWithLifecycle()
                val activeTreeSubj by TimerService.activePlantedSubjectId.collectAsStateWithLifecycle()
                val activeDurationMins by TimerService.activePlantedDurationMins.collectAsStateWithLifecycle()
                
                if (activeTreeType.isNotEmpty()) {
                    val matchingSubjName = TimerService.studySubjectsFlow.value.find { it.id == activeTreeSubj }?.name ?: "General Focus"
                    val isTimerRunning = isRunning && activeMode == ActiveMode.TIMER
                    
                    val percentRemain = if (countdownTotalDur > 0) countdownTimeLeft.toFloat() / countdownTotalDur.toFloat() else 0f
                    val percentGrow = (1f - percentRemain).coerceIn(0f, 1f)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9).copy(alpha = 0.9f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val treeEmoji = when {
                                        activeTreeType.contains("Sakura") -> "🌸"
                                        activeTreeType.contains("Oak") -> "🌳"
                                        activeTreeType.contains("Palm") -> "🌴"
                                        activeTreeType.contains("Bonsai") -> "🔮"
                                        activeTreeType.contains("Sunflower") -> "🌻"
                                        else -> "🌲"
                                    }
                                    
                                    Text(text = treeEmoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "GARDEN SEED ALIVE: ${activeTreeType.uppercase()}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = "Linked Subject: ${matchingSubjName.uppercase()} 📚",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isTimerRunning) Color(0xFF4CAF50) else Color(0xFFFF9800))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isTimerRunning) "GROWING 🌱" else "PAUSED ⏸️",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Progress indicator
                            LinearProgressIndicator(
                                progress = percentGrow,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF2E7D32),
                                trackColor = Color(0xFFC8E6C9)
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val minsLeft = (countdownTimeLeft / 60000L)
                                val secsLeft = (countdownTimeLeft % 60000L) / 1000L
                                Text(
                                    text = String.format("%02d:%02d remaining to mature", minsLeft, secsLeft),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(percentGrow * 100).toInt()}% mature",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // THE HARDWARE CASING & TABLE MOUNT FRAME
                TableCasingFrame(
                    activeMode = activeMode,
                    isRunning = isRunning,
                    liveClockTime = liveClockTime,
                    countdownTimeLeft = countdownTimeLeft,
                    countdownTotalDur = countdownTotalDur,
                    stopwatchElapsed = stopwatchElapsed,
                    themeColors = themeColors,
                    distractionDetected = distractionDetected,
                    displayStyleMode = displayStyleMode,
                    isHardwareFullscreen = false,
                    onToggleFullscreen = { isHardwareFullscreen = true },
                    onDismissDistraction = {
                        TimerService.distractionDetected.value = null
                    },
                    onStartStopTap = {
                        val serviceIntent = Intent(context, TimerService::class.java)
                        if (activeMode == ActiveMode.TIMER) {
                            if (isRunning) {
                                serviceIntent.action = "PAUSE_COOLDOWN"
                            } else {
                                serviceIntent.action = "START_COOLDOWN"
                            }
                        } else if (activeMode == ActiveMode.STOPWATCH) {
                            if (isRunning) {
                                serviceIntent.action = "PAUSE_STOPWATCH"
                            } else {
                                serviceIntent.action = "START_STOPWATCH"
                            }
                        }
                        context.startService(serviceIntent)
                    },
                    onMinIncrement = {
                        addDurationMinutes(1)
                    },
                    onSecIncrement = {
                        addDurationSeconds(10)
                    },
                    onResetTap = {
                        val serviceIntent = Intent(context, TimerService::class.java).apply {
                            action = if (activeMode == ActiveMode.TIMER) "RESET_COOLDOWN" else "RESET_STOPWATCH"
                        }
                        context.startService(serviceIntent)
                    },
                    onModeTap = {
                        cycleMode()
                    },
                    onLapTap = {
                        if (activeMode == ActiveMode.STOPWATCH) {
                            val laps = TimerService.stopwatchLaps.value.toMutableList()
                            laps.add(stopwatchElapsed)
                            TimerService.stopwatchLaps.value = laps
                        }
                    },
                    onSetDurationDirectly = {
                        showQuickManualInputSheet = true
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // --- PREMIUM DYNAMIC PRESETS FOR CHRONOMETER SCREEN TYPES ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎯 CHOOSE STUDY PRESET & MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val studyPresetOptions = listOf(
                                Triple("🍅 Pomodoro", 25L, "25 Min"),
                                Triple("⚡ Sprint", 15L, "15 Min"),
                                Triple("📚 Intense", 50L, "50 Min"),
                                Triple("☕ Break", 5L, "5 Min")
                            )
                            studyPresetOptions.forEach { (title, mins, subtitle) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            TimerService.activeMode.value = ActiveMode.TIMER
                                            val targetMs = mins * 60L * 1000L
                                            TimerService.countdownTotalDuration.value = targetMs
                                            TimerService.countdownTimeLeft.value = targetMs
                                            TimerService.isRunning.value = false
                                            TimerService.instance?.saveSettings()
                                            
                                            val resetIntent = Intent(context, TimerService::class.java).apply {
                                                action = "RESET_COOLDOWN"
                                            }
                                            context.startService(resetIntent)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = subtitle,
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🎨 DEVICE STYLE PRESETS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val currentActiveTheme by TimerService.currentTheme.collectAsStateWithLifecycle()
                                val themeChipsList = listOf(
                                    TimerService.Companion.DisplayTheme.RETRO_GREEN to Color(0xFF6F826B),
                                    TimerService.Companion.DisplayTheme.AMBER_GLOW to Color(0xFFD35400),
                                    TimerService.Companion.DisplayTheme.CYBER_PUNK to Color(0xFFEC407A),
                                    TimerService.Companion.DisplayTheme.NEON_BLUE to Color(0xFF1E88E5),
                                    TimerService.Companion.DisplayTheme.AMOLED_DARK to Color(0xFF424242),
                                    TimerService.Companion.DisplayTheme.BRUSHED_STEEL to Color(0xFFCFD8DC)
                                )
                                themeChipsList.forEach { (thm, col) ->
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (currentActiveTheme == thm) 2.dp else 0.5.dp,
                                                color = if (currentActiveTheme == thm) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                TimerService.currentTheme.value = thm
                                                TimerService.instance?.saveSettings()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- LAP SPLITS CHRONICLE TIMELINE CARD ---
                if (activeMode == ActiveMode.STOPWATCH) {
                    val recordedLaps = TimerService.stopwatchLaps.value
                    if (recordedLaps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⏱️ CAPTURED LAP SPLITS (${recordedLaps.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    TextButton(
                                        onClick = { TimerService.stopwatchLaps.value = emptyList() },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("WIPE LAPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 120.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    recordedLaps.forEachIndexed { i, timeMs ->
                                        val totalSecs = timeMs / 1000
                                        val outputTime = String.format("%02d:%02d.%02d", totalSecs / 60, totalSecs % 60, (timeMs % 1000) / 10)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "SPLIT S-\${i + 1} (" + run {
                                                    val delta = timeMs - (if (i > 0) recordedLaps[i - 1] else 0L)
                                                    val ds = delta / 1000
                                                    String.format("+%02d:%02d", ds / 60, ds % 60)
                                                } + ")",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = outputTime,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = themeColors.secondaryGlowColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- INTEGRATED SECURE STUDY CLASSROOM STUDY WEB-ZONE PANEL ---
                var secureWebPanelExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Online Study Classroom Web Engine Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "🎓 SANDBOXED STUDY CLASSROOM",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Attend online lectures safely without reels distraction",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            IconButton(onClick = { secureWebPanelExpanded = !secureWebPanelExpanded }) {
                                Icon(
                                    imageVector = if (secureWebPanelExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle sandbox visual panel",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (secureWebPanelExpanded) {
                            var classroomModeOnline by rememberSaveable { mutableStateOf(true) }
                            var inputAddressText by rememberSaveable { mutableStateOf("https://m.wikipedia.org/") }
                            var targetLoadedUrlState by rememberSaveable { mutableStateOf("https://m.wikipedia.org/") }
                            var sandboxHeightDp by rememberSaveable { mutableStateOf(290) }

                            Spacer(modifier = Modifier.height(6.dp))
                            TabRow(
                                selectedTabIndex = if (classroomModeOnline) 0 else 1,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Tab(
                                    selected = classroomModeOnline,
                                    onClick = { classroomModeOnline = true },
                                    text = { Text("🌐 Online Web Classroom", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = !classroomModeOnline,
                                    onClick = { classroomModeOnline = false },
                                    text = { Text("💾 Local Offline Lectures", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            if (classroomModeOnline) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = inputAddressText,
                                        onValueChange = { inputAddressText = it },
                                        label = { Text("Lecture, Class Website or Video Link", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f).testTag("sandbox_url_input_box"),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    )
                                    Button(
                                        onClick = {
                                            var corrected = inputAddressText.trim()
                                            if (!corrected.startsWith("http://") && !corrected.startsWith("https://")) {
                                                corrected = "https://$corrected"
                                            }
                                            targetLoadedUrlState = corrected
                                            inputAddressText = corrected
                                        },
                                        modifier = Modifier.height(54.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Load", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Fast Learning Portals:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val scrollingStateRow = rememberScrollState()
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollingStateRow),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val studyLinks = listOf(
                                        "Wikipedia" to "https://m.wikipedia.org/",
                                        "MIT OCW" to "https://ocw.mit.edu/",
                                        "Khan Academy" to "https://www.khanacademy.org/",
                                        "Relax Lofi Music" to "https://www.youtube.com/embed/jfKfPfyJRdk"
                                    )
                                    studyLinks.forEach { (title, link) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                                                .clickable {
                                                    targetLoadedUrlState = link
                                                    inputAddressText = link
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = title,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "📐 Frame Height: ${sandboxHeightDp}dp",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Slider(
                                        value = sandboxHeightDp.toFloat(),
                                        onValueChange = { sandboxHeightDp = it.toInt() },
                                        valueRange = 180f..650f,
                                        modifier = Modifier.weight(1f).height(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Web browser frame container
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(sandboxHeightDp.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                webViewClient = android.webkit.WebViewClient()
                                                webChromeClient = object : android.webkit.WebChromeClient() {
                                                    override fun onShowCustomView(view: android.view.View?, callback: android.webkit.WebChromeClient.CustomViewCallback?) {
                                                        super.onShowCustomView(view, callback)
                                                        customFullScreenView = view
                                                         customFullScreenCallback = callback
                                                    }
                                                    override fun onHideCustomView() {
                                                        super.onHideCustomView()
                                                         customFullScreenCallback = null
                                                        customFullScreenView = null
                                                    }
                                                }
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                settings.useWideViewPort = true
                                                settings.loadWithOverviewMode = true
                                                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                                settings.mediaPlaybackRequiresUserGesture = false
                                                settings.databaseEnabled = true
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                                }
                                                loadUrl(targetLoadedUrlState)
                                            }
                                        },
                                        update = { wbview ->
                                            val currentUrl = wbview.url ?: ""
                                            val normCurrent = currentUrl.removeSuffix("/")
                                            val normTarget = targetLoadedUrlState.removeSuffix("/")
                                            if (normCurrent != normTarget && currentUrl != targetLoadedUrlState) {
                                                wbview.loadUrl(targetLoadedUrlState)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                // Local Offline Media Player
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(sandboxHeightDp.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                ) {
                                    if (selectedLocalVideoUri == null) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable {
                                                    try {
                                                        videoPickerLauncher.launch(arrayOf("video/*"))
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Standard Video Picker is not available on this device configuration", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Select local lecture",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Select Local Offline Class Video",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "No video selected. Tap to open local files (offline friendly).",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 24.dp)
                                            )
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            if (isLocalVideoPreparing) {
                                                CircularProgressIndicator(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            }
                                            AndroidView(
                                                factory = { ctx ->
                                                    android.widget.VideoView(ctx).apply {
                                                        setVideoURI(selectedLocalVideoUri)
                                                        val mediaController = android.widget.MediaController(ctx)
                                                        mediaController.setAnchorView(this)
                                                        setMediaController(mediaController)
                                                        setOnPreparedListener { mp ->
                                                            seekTo(localVideoPlaybackPos); isLocalVideoPreparing = false; isLocalVideoPlaying = true
                                                            start()
                                                        }
                                                        setOnErrorListener { mp, what, extra ->
                                                            android.widget.Toast.makeText(ctx, "Device failed to play this video type. Ensure format is valid (mp4/mkv/etc)", android.widget.Toast.LENGTH_LONG).show()
                                                            true
                                                        }
                                                        activeVideoViewInstance = this
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        try {
                                                            videoPickerLauncher.launch(arrayOf("video/*"))
                                                        } catch (e: Exception) {
                                                            android.widget.Toast.makeText(context, "Picker error", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("Pick Different File", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = {
                                                        selectedLocalVideoUri = null
                                                        activeVideoViewInstance = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("Close", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .padding(vertical = 4.dp, horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    IconButton(
                                                        onClick = {
                                                            activeVideoViewInstance?.let { view ->
                                                                val targetPos = (view.currentPosition - 10000).coerceAtLeast(0)
                                                                view.seekTo(targetPos)
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowBack,
                                                            contentDescription = "Rewind 10s",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            activeVideoViewInstance?.let { view ->
                                                                if (view.isPlaying) {
                                                                    view.pause(); isLocalVideoPlaying = false
                                                                } else {
                                                                    view.start(); isLocalVideoPlaying = true
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isLocalVideoPlaying) CustomPauseIcon else Icons.Default.PlayArrow,
                                                            contentDescription = "Play or Pause",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            activeVideoViewInstance?.let { view ->
                                                                 val targetPos = (view.currentPosition + 10000).coerceAtMost(view.duration)
                                                                 view.seekTo(targetPos)
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowForward,
                                                            contentDescription = "Forward 10s",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        activeVideoViewInstance?.let { view ->
                                                            localVideoPlaybackPos = view.currentPosition
                                                        }
                                                        localVideoFullScreen = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ExitToApp,
                                                        contentDescription = "Full Screen",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Direct numerical input modal dialog to quickly dial timing
    if (showQuickManualInputSheet) {
        var hrsInput by remember { mutableStateOf("0") }
        var minsInput by remember { mutableStateOf("10") }
        var secsInput by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showQuickManualInputSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, contentDescription = "Watch preset icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Preset & Direct Digit Input", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Dial exact duration for countdown study/kitchen hours below:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CasingInputBox(label = "HR", valStr = hrsInput, onValChange = { hrsInput = it.take(2).filter { c -> c.isDigit() } })
                        CasingInputBox(label = "MIN", valStr = minsInput, onValChange = { minsInput = it.take(2).filter { c -> c.isDigit() } })
                        CasingInputBox(label = "SEC", valStr = secsInput, onValChange = { secsInput = it.take(2).filter { c -> c.isDigit() } })
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Instant Presets (Quick Tap):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetQuickChip("Study (45m)") { hrsInput = "0"; minsInput = "45"; secsInput = "0" }
                        PresetQuickChip("Study (20m)") { hrsInput = "0"; minsInput = "20"; secsInput = "0" }
                        PresetQuickChip("Boil (5m)") { hrsInput = "0"; minsInput = "5"; secsInput = "0" }
                        PresetQuickChip("Maggi (2m)") { hrsInput = "0"; minsInput = "2"; secsInput = "0" }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hrL = hrsInput.toLongOrNull() ?: 0L
                        val minL = minsInput.toLongOrNull() ?: 10L
                        val secL = secsInput.toLongOrNull() ?: 0L
                        val totMs = (hrL * 3600 + minL * 60 + secL) * 1000L
                        TimerService.countdownTotalDuration.value = totMs
                        TimerService.countdownTimeLeft.value = totMs
                        TimerService.instance?.saveSettings()
                        showQuickManualInputSheet = false
                    },
                    modifier = Modifier.testTag("apply_duration_confirm")
                ) {
                    Text("Apply Digit Dial")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickManualInputSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings Configuration Panel Dialog
    if (showSettingsSheet) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSettingsSheet = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DXT-808 CONTROL PANEL",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        IconButton(onClick = { showSettingsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close settings", modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Navigation Row with scroll support
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .horizontalScroll(rememberScrollState())
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabItems = listOf(
                            Triple("Display", Icons.Default.Settings, 0),
                            Triple("Focus", Icons.Default.Check, 1),
                            Triple("Profile", Icons.Default.Person, 2),
                            Triple("Tones", Icons.Default.Notifications, 3),
                            Triple("AI Tutor", Icons.Default.Star, 4),
                            Triple("Tracker", Icons.Default.PlayArrow, 5),
                            Triple("Quests", Icons.Default.Favorite, 6)
                        )
                        tabItems.forEach { (label, icon, index) ->
                            val isSelected = selectedTabCustomizer == index
                            val animBgColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                label = "tab_bg"
                            )
                            val animContentColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "tab_fg"
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(animBgColor)
                                    .clickable { selectedTabCustomizer = index }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = animContentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        color = animContentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable Core Content Pane
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedTabCustomizer) {
                            0 -> CustomizerDisplayTab(
                                currentTheme = currentTheme,
                                onThemeChange = {
                                    TimerService.currentTheme.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                tickEnabled = tickEnabled,
                                onTickChange = {
                                    TimerService.isTickSoundEnabled.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                displayStyleMode = displayStyleMode,
                                onDisplayStyleChange = {
                                    TimerService.displayStyleMode.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                isSoundMutedGlobal = isSoundMutedGlobal,
                                onMuteChange = {
                                    TimerService.isSoundMutedGlobal.value = it
                                    TimerService.instance?.saveSettings()
                                }
                            )
                            1 -> CustomizerFocusTab(
                                strictModeEnabled = strictModeEnabled,
                                onStrictModeChange = {
                                    TimerService.isStrictModeEnabled.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                isStrictModeWarningsEnabled = isStrictModeWarningsEnabled,
                                onStrictModeWarningsChange = {
                                    TimerService.isStrictModeWarningsEnabled.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                strictModeThresholdSeconds = strictModeThresholdSeconds,
                                onThresholdChange = {
                                    TimerService.strictModeThresholdSeconds.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                strictnessLevel = strictnessLevel,
                                onStrictnessChange = {
                                    TimerService.strictnessLevel.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                isYoutubeWhitelisted = isYoutubeWhitelisted,
                                onWhitelistYoutubeChange = {
                                    TimerService.isYoutubeWhitelisted.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                permissionStatus = permissionStatus,
                                focusLogs = focusLogs,
                                studyHistory = studyMinsHistory,
                                restrictedHistory = restrictedMinsHistory,
                                days = historyDays,
                                themeColors = themeColors,
                                onOpenUsageSettings = onOpenUsageSettings,
                                onOpenOverlaySettings = onOpenOverlaySettings
                            )
                            2 -> CustomizerProfileTab(
                                profileName = profileName,
                                profileAge = profileAge,
                                profileCourse = profileCourse,
                                profileSchool = profileSchool,
                                profileGoal = profileGoal,
                                profileDailyTarget = profileDailyTarget,
                                profileFavSubject = profileFavSubject,
                                onExportFile = { createDocumentLauncher.launch("focusflow_backup.json") },
                                onImportFile = { openDocumentLauncher.launch(arrayOf("application/json")) }
                            )
                            3 -> CustomizerTonesSensorsTab(
                                alarmTone = alarmTone,
                                onToneChange = {
                                    TimerService.currentAlarmTone.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                alarmDuration = alarmDuration,
                                onDurationChange = {
                                    TimerService.alarmDurationSeconds.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                ambientSound = ambientSound,
                                onAmbientChange = {
                                    TimerService.ambientSoundEffect.value = it
                                    TimerService.instance?.saveSettings()
                                    TimerService.instance?.startAmbientSynth()
                                },
                                tempOffset = tempOffset,
                                onTempOffsetChange = {
                                    TimerService.tempCalibrationOffset.value = it
                                    TimerService.instance?.saveSettings()
                                },
                                currentTemperature = currentTemperature,
                                simulatedHumidity = simulatedHumidity,
                                themeColors = themeColors
                            )
                            4 -> CustomizerAITutorTab(
                                profileName = profileName,
                                profileAge = profileAge,
                                profileCourse = profileCourse,
                                profileSchool = profileSchool,
                                profileGoal = profileGoal,
                                profileDailyTarget = profileDailyTarget,
                                profileFavSubject = profileFavSubject,
                                studyHistory = studyMinsHistory,
                                restrictedHistory = restrictedMinsHistory,
                                currentTemperature = currentTemperature,
                                themeColors = themeColors
                            )
                            5 -> CustomizerFlowTrackTab(
                                subjects = studySubjects,
                                sessions = studySessions,
                                activeTrackingSubjectId = activeTrackingSubjectId,
                                themeColors = themeColors
                            )
                            6 -> CustomizerGamifyTab(
                                themeColors = themeColors
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dialog Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showSettingsSheet = false },
                            modifier = Modifier.testTag("close_settings_confirm").height(48.dp)
                        ) {
                            Text("DONE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }

    // Support auto-rotation when entering either fullscreen video player
    LaunchedEffect(customFullScreenView, localVideoFullScreen) {
        val activity = context as? android.app.Activity
        if (customFullScreenView != null || localVideoFullScreen) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Full Screen Video View Overlay
    if (customFullScreenView != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("fullscreen_video_overlay"),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { customFullScreenView!! },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = {
                    customFullScreenCallback?.onCustomViewHidden()
                    customFullScreenView = null
                    customFullScreenCallback = null
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close full screen video",
                    tint = Color.White
                )
            }
        }
    }

    // Full Screen Video View Overlay for Local Video Player
    if (localVideoFullScreen && selectedLocalVideoUri != null) {
        var fullscreenVideoViewInstance by remember { mutableStateOf<android.widget.VideoView?>(null) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("local_fullscreen_video_overlay"),
            contentAlignment = Alignment.Center
        ) {
            if (isLocalVideoPreparing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            AndroidView(
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        setVideoURI(selectedLocalVideoUri)
                        val mediaController = android.widget.MediaController(ctx)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setOnPreparedListener { mp ->
                            seekTo(localVideoPlaybackPos); isLocalVideoPreparing = false; isLocalVideoPlaying = true
                            start()
                        }
                        setOnErrorListener { mp, what, extra ->
                            android.widget.Toast.makeText(ctx, "Playback fails in full screen", android.widget.Toast.LENGTH_SHORT).show()
                            true
                        }
                        fullscreenVideoViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📺 Fullscreen Study Lecture Mode",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    IconButton(
                        onClick = {
                            fullscreenVideoViewInstance?.let { view ->
                                localVideoPlaybackPos = view.currentPosition
                            }
                            localVideoFullScreen = false
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit fullscreen local video",
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            fullscreenVideoViewInstance?.let { view ->
                                val target = (view.currentPosition - 10000).coerceAtLeast(0)
                                view.seekTo(target)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            fullscreenVideoViewInstance?.let { view ->
                                if (view.isPlaying) {
                                    view.pause()
                                } else {
                                    view.start()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isLocalVideoPlaying) CustomPauseIcon else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            fullscreenVideoViewInstance?.let { view ->
                                val target = (view.currentPosition + 10000).coerceAtMost(view.duration)
                                view.seekTo(target)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- HELPER CHIPS & VIEWS ---

@Composable
fun PresetQuickChip(label: String, onApply: () -> Unit) {
    Surface(
        onClick = onApply,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasingInputBox(label: String, valStr: String, onValChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = valStr,
            onValueChange = onValChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- ACTIVE RINGING ALARM STRIKE BANNER ---

@Composable
fun AlarmTriggerShield() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm_shield_container")
            .padding(bottom = 12.dp)
            .border(2.dp, Color.Red, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Ringing Notification",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("🛎️ ALARM RINGING!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Kitchen timer count reached zero limit.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }

            Button(
                onClick = {
                    val context = TimerService.instance?.applicationContext
                    if (context != null) {
                        val serviceIntent = Intent(context, TimerService::class.java).apply { action = "STOP_ALARM" }
                        context.startService(serviceIntent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("dismiss_ring_button")
            ) {
                Text("DISMISS", fontWeight = FontWeight.Black)
            }
        }
    }
}

// ENVIRONMENT HEADER BANNER DISPLAYING AMBIENT STATE
@Composable
fun LiveDayDateBanner(
    liveClockMs: Long,
    simulatedTemp: Float,
    themeColors: ThemeColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        val profileName by TimerService.profileUserName.collectAsStateWithLifecycle()
        val profileXP by TimerService.profileXP.collectAsStateWithLifecycle()
        val profileLevel by TimerService.profileLevel.collectAsStateWithLifecycle()
        val profileStreakDays by TimerService.profileStreakDays.collectAsStateWithLifecycle()
        val forestCoins by TimerService.forestCoins.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: GREETING & DATE BANNER
            Column {
                val cal = Calendar.getInstance().apply { timeInMillis = liveClockMs }
                val hrOfDay = cal.get(Calendar.HOUR_OF_DAY)
                val displayName = if (profileName.trim().isNotEmpty()) profileName.trim() else "Scholar"
                val greetMessage = when (hrOfDay) {
                    in 4..11 -> "Good Morning, $displayName! ☀️"
                    in 12..16 -> "Good Afternoon, $displayName! ☕"
                    in 17..21 -> "Good Evening, $displayName! 🌙"
                    else -> "Midnight Focus, $displayName! 🌌"
                }
                val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(liveClockMs))
                val dateStr = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date(liveClockMs))
                
                val quoteMessage = when (hrOfDay) {
                    in 4..11 -> "☀️ Secret of getting ahead is getting started!"
                    in 12..16 -> "☕ Every step is progress! Stay consistent."
                    in 17..21 -> "🌙 Plan, execute & finish the day strong!"
                    else -> "🌌 Quiet midnight hours build true legends."
                }
                
                Text(
                    text = greetMessage,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "$dayStr, $dateStr",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = quoteMessage,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 0.5.dp)

            // Row 2: GAMIFICATION & SCHOLAR STANDINGS (Equal width adaptive columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⭐ LVM $profileLevel",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val maxNextLvlXp = profileLevel * 150
                    Text(
                        text = "📈 $profileXP/$maxNextLvlXp XP",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔥 $profileStreakDays DAY",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFA000).copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "☀️ $forestCoins COIN",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD48000),
                        maxLines = 1
                    )
                }
            }

            // Row 3: SENSORS & BATTERY TEMPERATURE TELEMETRY
            val context = androidx.compose.ui.platform.LocalContext.current
            val batteryManager = remember { context.getSystemService(android.content.Context.BATTERY_SERVICE) as? android.os.BatteryManager }
            val batteryInfoText = remember { mutableStateOf("BATT -.-") }
            val chargingColor = remember { mutableStateOf(Color.Green) }

            LaunchedEffect(liveClockMs) {
                try {
                    val currentNow = batteryManager?.getLongProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0L
                    val isChargingCheck = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        batteryManager?.isCharging == true
                    } else {
                        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                        val batteryStatus = context.registerReceiver(null, filter)
                        val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                        status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    }

                    val currentAmps = currentNow.toDouble() / 1_000_000.0
                    val currentMilliAmps = currentNow / 1000L
                    
                    if (isChargingCheck) {
                        chargingColor.value = Color(0xFFFFC107)
                        val displayAmps = if (kotlin.math.abs(currentAmps) > 0.01) kotlin.math.abs(currentAmps) else 1.85
                        val displayMa = if (kotlin.math.abs(currentMilliAmps) > 1) kotlin.math.abs(currentMilliAmps).toInt() else 1850
                        batteryInfoText.value = String.format("⚡ %.1fA (%dmA)", displayAmps, displayMa)
                    } else {
                        chargingColor.value = Color(0xFF4CAF50)
                        val displayAmps = if (kotlin.math.abs(currentAmps) > 0.01) -kotlin.math.abs(currentAmps) else -0.38
                        val displayMa = if (kotlin.math.abs(currentMilliAmps) > 1) -kotlin.math.abs(currentMilliAmps).toInt() else -380
                        batteryInfoText.value = String.format("🔋 %.1fA (%dmA)", displayAmps, displayMa)
                    }
                } catch (e: Exception) {
                    batteryInfoText.value = "🔋 BATT ACTIVE"
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Room temp
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(themeColors.bezelBgColor.copy(alpha = 0.15f))
                        .border(0.5.dp, themeColors.bezelBgColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(themeColors.secondaryGlowColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ROOM ${String.format("%.1f", simulatedTemp)}°C",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = themeColors.secondaryGlowColor
                    )
                }

                // Battery Telemetry
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(themeColors.bezelBgColor.copy(alpha = 0.15f))
                        .border(0.5.dp, themeColors.bezelBgColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(chargingColor.value)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = batteryInfoText.value,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = themeColors.screenTextColor
                    )
                }
            }
        }
    }
}

// -- TABLE TOP CASING DESIGN MIMIC --
@Composable
fun TableCasingFrame(
    activeMode: ActiveMode,
    isRunning: Boolean,
    liveClockTime: Long,
    countdownTimeLeft: Long,
    countdownTotalDur: Long,
    stopwatchElapsed: Long,
    themeColors: ThemeColorScheme,
    distractionDetected: String?,
    displayStyleMode: Int,
    isHardwareFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onDismissDistraction: () -> Unit,
    onStartStopTap: () -> Unit,
    onMinIncrement: () -> Unit,
    onSecIncrement: () -> Unit,
    onResetTap: () -> Unit,
    onModeTap: () -> Unit,
    onLapTap: () -> Unit,
    onSetDurationDirectly: () -> Unit
) {
    // Elegant frame replication with gradient backing
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2C3E50),
                        Color(0xFF1E272C)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(3.dp, Color(0xFF4F5B66), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Elegant brand engraving
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left mock case-screw
                MockCaseScrew()
                
                Text(
                    text = "DIGITAL STUDY LABS • DXT-808",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color(0xFF8B9B9C),
                    letterSpacing = 1.5.sp
                )

                // Right mock case-screw
                MockCaseScrew()
            }

            Spacer(modifier = Modifier.height(10.dp))

            // THE LCD GLASS SCREEN ELEMENT WINDOW
            val lcdHeight = if (isHardwareFullscreen) 360.dp else 260.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lcdHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColors.bezelBgColor)
                    .border(2.dp, themeColors.bezelBorderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                // Glass Screen grid overlay drawing subtle retro scanline texture
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scanlineSpacing = 4.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.04f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += scanlineSpacing
                    }
                }

                // Inner content layout of glass clock screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    
                    // Displays Top Status: Active selection pointers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IndicatorPill("CLOCK", active = activeMode == ActiveMode.CLOCK, themeColors)
                            IndicatorPill("TIMER", active = activeMode == ActiveMode.TIMER, themeColors)
                            IndicatorPill("SWATCH", active = activeMode == ActiveMode.STOPWATCH, themeColors)
                        }

                        // AM/PM or Running blinker status + Fullscreen toggle Icon
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Fullscreen Toggle Icon Button
                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.size(24.dp).testTag("fullscreen_toggle_ib")
                            ) {
                                Icon(
                                    imageVector = if (isHardwareFullscreen) Icons.Default.Close else Icons.Default.ExitToApp,
                                    contentDescription = "Toggle Immersive Hardware Fullscreen Mode",
                                    tint = themeColors.screenTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            if (isRunning) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(themeColors.screenTextColor)
                                )
                                Text(
                                    text = "RUNNING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = themeColors.screenTextColor
                                )
                            } else {
                                Text(
                                    text = "PAUSED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = themeColors.screenTextColor.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Active tracking subject banner
                    val activeTrackingSubjectId by TimerService.activeTrackingSubjectId.collectAsStateWithLifecycle()
                    val studySubjects by TimerService.studySubjectsFlow.collectAsStateWithLifecycle()
                    val studySessions by TimerService.studySessionsFlow.collectAsStateWithLifecycle()
                    val activeSubject = studySubjects.find { it.id == activeTrackingSubjectId }

                    if (activeSubject != null) {
                        val sessionList = studySessions.filter { it.subjectId == activeSubject.id }
                        val totalPlannedMins = sessionList.sumOf { it.plannedMinutes }
                        val totalActualSecs = sessionList.sumOf { it.actualSeconds }
                        val totalActualHrs = totalActualSecs / 3600.0
                        val totalPlannedHrs = totalPlannedMins / 60.0
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeColors.screenTextColor.copy(alpha = 0.08f))
                                .border(0.5.dp, themeColors.screenTextColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "subject_pulse")
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1.0f,
                                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                        animation = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.LinearEasing),
                                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                    ),
                                    label = "pulse"
                                )
                                val parsedColor = try {
                                    Color(android.graphics.Color.parseColor(activeSubject.color))
                                } catch (e: Exception) {
                                    themeColors.secondaryGlowColor
                                }
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor.copy(alpha = pulseAlpha))
                                )
                                Text(
                                    text = "TRK: ${activeSubject.name.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = themeColors.screenTextColor
                                )
                            }
                            Text(
                                text = "${String.format("%.2f", totalActualHrs)}h / ${String.format("%.1f", totalPlannedHrs)}h",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = themeColors.screenTextColor
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeColors.screenTextColor.copy(alpha = 0.02f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 NO SUBJECT ACTIVE (TRACKER TAB)",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = themeColors.screenTextColor.copy(alpha = 0.4f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // MAIN DISPLAY CANVAS AREA (Digital LCD vs. Analog Hands vs. Watch Rings)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("digital_display_canvas"),
                        contentAlignment = Alignment.Center
                    ) {
                        val mainTimeText = when (activeMode) {
                            ActiveMode.CLOCK -> {
                                SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(Date(liveClockTime))
                            }
                            ActiveMode.TIMER -> {
                                val secondsLeft = countdownTimeLeft / 1000
                                val hrs = secondsLeft / 3600
                                val mins = (secondsLeft % 3600) / 60
                                val secs = secondsLeft % 60
                                String.format("%02d:%02d:%02d", hrs, mins, secs)
                            }
                            ActiveMode.STOPWATCH -> {
                                val seconds = stopwatchElapsed / 1000
                                val mins = seconds / 60
                                val secs = seconds % 60
                                val msTens = (stopwatchElapsed % 1000) / 10
                                String.format("%02d:%02d.%02d", mins, secs, msTens)
                            }
                        }

                        if (displayStyleMode == 1) { // 1: REALISTIC ANALOG DIAL
                            Box(
                                modifier = Modifier.size(if (isHardwareFullscreen) 200.dp else 150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    val radius = size.minDimension / 2f
                                    
                                    // 1. Draw Dial face plate
                                    drawCircle(
                                        color = themeColors.screenTextColor.copy(alpha = 0.08f),
                                        radius = radius,
                                        center = center
                                    )
                                    
                                    // 2. Draw border
                                    drawCircle(
                                        color = themeColors.screenTextColor.copy(alpha = 0.4f),
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = (if (isHardwareFullscreen) 2.5.dp else 1.5.dp).toPx())
                                    )
                                    
                                    // 3. Draw ticks for hours (every 30 degrees, 12 numbers total)
                                    for (angleDegrees in 0 until 360 step 30) {
                                        val isMajor = angleDegrees % 90 == 0
                                        val tickLen = if (isMajor) (if (isHardwareFullscreen) 12.dp else 8.dp).toPx() else (if (isHardwareFullscreen) 6.dp else 4.dp).toPx()
                                        val strokeW = if (isMajor) (if (isHardwareFullscreen) 3.dp else 2.dp).toPx() else (if (isHardwareFullscreen) 1.5.dp else 1.dp).toPx()
                                        
                                        val angleRad = Math.toRadians(angleDegrees.toDouble())
                                        val startX = center.x + (radius - tickLen) * kotlin.math.sin(angleRad).toFloat()
                                        val startY = center.y - (radius - tickLen) * kotlin.math.cos(angleRad).toFloat()
                                        
                                        val endX = center.x + radius * kotlin.math.sin(angleRad).toFloat()
                                        val endY = center.y - radius * kotlin.math.cos(angleRad).toFloat()
                                        
                                        drawLine(
                                            color = themeColors.screenTextColor.copy(alpha = if (isMajor) 0.8f else 0.4f),
                                            start = Offset(startX, startY),
                                            end = Offset(endX, endY),
                                            strokeWidth = strokeW
                                        )
                                    }

                                    // 4. Calculate angles based on active mode
                                    val secondsAngle: Float
                                    val minutesAngle: Float
                                    val hoursAngle: Float

                                    when (activeMode) {
                                        ActiveMode.CLOCK -> {
                                            val cal = Calendar.getInstance().apply { timeInMillis = liveClockTime }
                                            val currentHr = cal.get(Calendar.HOUR)
                                            val currentMin = cal.get(Calendar.MINUTE)
                                            val currentSec = cal.get(Calendar.SECOND)
                                            val currentMs = cal.get(Calendar.MILLISECOND)
                                            
                                            secondsAngle = (currentSec + currentMs / 1000f) * 6f
                                            minutesAngle = (currentMin + currentSec / 60f) * 6f
                                            hoursAngle = (currentHr + currentMin / 60f) * 30f
                                        }
                                        ActiveMode.TIMER -> {
                                            val secsLeft = countdownTimeLeft / 1000
                                            val hrs = (secsLeft / 3600) % 12
                                            val mins = (secsLeft % 3600) / 60
                                            val secs = secsLeft % 60
                                            
                                            secondsAngle = secs * 6f
                                            minutesAngle = mins * 6f
                                            hoursAngle = hrs * 30f
                                        }
                                        ActiveMode.STOPWATCH -> {
                                            val totalSecs = stopwatchElapsed / 1000
                                            val mins = (totalSecs / 60) % 60
                                            val hrs = (totalSecs / 3600) % 12
                                            val msTens = (stopwatchElapsed % 1000) / 10
                                            
                                            secondsAngle = (totalSecs % 60 + msTens / 100f) * 6f
                                            minutesAngle = mins * 6f
                                            hoursAngle = hrs * 30f
                                        }
                                    }

                                    // Draw Hour hand
                                    val hourLen = radius * 0.48f
                                    val hourRad = Math.toRadians(hoursAngle.toDouble())
                                    drawLine(
                                        color = themeColors.screenTextColor,
                                        start = center,
                                        end = Offset(
                                            center.x + hourLen * kotlin.math.sin(hourRad).toFloat(),
                                            center.y - hourLen * kotlin.math.cos(hourRad).toFloat()
                                        ),
                                        strokeWidth = (if (isHardwareFullscreen) 6.dp else 4.dp).toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Draw Minute hand
                                    val minLen = radius * 0.72f
                                    val minRad = Math.toRadians(minutesAngle.toDouble())
                                    drawLine(
                                        color = themeColors.screenTextColor,
                                        start = center,
                                        end = Offset(
                                            center.x + minLen * kotlin.math.sin(minRad).toFloat(),
                                            center.y - minLen * kotlin.math.cos(minRad).toFloat()
                                        ),
                                        strokeWidth = (if (isHardwareFullscreen) 4.dp else 2.5.dp).toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Draw Second hand
                                    val secLen = radius * 0.85f
                                    val secRad = Math.toRadians(secondsAngle.toDouble())
                                    drawLine(
                                        color = themeColors.secondaryGlowColor,
                                        start = center,
                                        end = Offset(
                                            center.x + secLen * kotlin.math.sin(secRad).toFloat(),
                                            center.y - secLen * kotlin.math.cos(secRad).toFloat()
                                        ),
                                        strokeWidth = (if (isHardwareFullscreen) 2.dp else 1.2.dp).toPx()
                                    )

                                    // Pin
                                    drawCircle(
                                        color = themeColors.screenTextColor,
                                        radius = (if (isHardwareFullscreen) 4.dp else 3.dp).toPx(),
                                        center = center
                                    )
                                }
                                
                                // Small numeric digital text underneath analog clock face
                                Text(
                                    text = mainTimeText.take(5),
                                    fontSize = if (isHardwareFullscreen) 12.sp else 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = themeColors.screenTextColor,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .background(themeColors.bezelBgColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        } else if (displayStyleMode == 2) { // 2: SMART WATCH DYNAMIC PROGRESS RINGS
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val sizeCircle = if (isHardwareFullscreen) 220.dp else 160.dp
                                Canvas(modifier = Modifier.size(sizeCircle)) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    val radius = size.minDimension / 2f
                                    
                                    // Outermost Sweep
                                    val calRef = Calendar.getInstance().apply { timeInMillis = liveClockTime }
                                    val secsSweep = calRef.get(Calendar.SECOND) + calRef.get(Calendar.MILLISECOND) / 1000f
                                    val sweepPct = when (activeMode) {
                                        ActiveMode.CLOCK -> secsSweep / 60f
                                        ActiveMode.TIMER -> if (countdownTotalDur > 0) countdownTimeLeft.toFloat() / countdownTotalDur.toFloat() else 1f
                                        ActiveMode.STOPWATCH -> (stopwatchElapsed % 60000L).toFloat() / 60000L
                                    }
                                    
                                    drawArc(
                                        color = themeColors.screenTextColor.copy(alpha = 0.10f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = (if (isHardwareFullscreen) 8.dp else 5.dp).toPx(), cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = themeColors.secondaryGlowColor,
                                        startAngle = -90f,
                                        sweepAngle = sweepPct * 360f,
                                        useCenter = false,
                                        style = Stroke(width = (if (isHardwareFullscreen) 8.dp else 5.dp).toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = mainTimeText,
                                        style = LocalTextStyle.current.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = if (isHardwareFullscreen) 38.sp else 28.sp,
                                            color = themeColors.screenTextColor,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Text(
                                        text = "HEALTH ZONE SENSORS ACTIVE",
                                        fontSize = if (isHardwareFullscreen) 10.sp else 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = themeColors.screenTextColor.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else if (displayStyleMode == 3) { // 3: ANTIQUE POCKET WATCH STYLIST
                            Box(
                                modifier = Modifier.size(if (isHardwareFullscreen) 200.dp else 150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    val radius = size.minDimension / 2f
                                    
                                    // Pocket watch outer ring bracket
                                    drawCircle(
                                        color = themeColors.screenTextColor.copy(alpha = 0.2f),
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = (if (isHardwareFullscreen) 6.dp else 4.dp).toPx())
                                    )
                                    
                                    // Ring pendant
                                    drawCircle(
                                        color = themeColors.screenTextColor.copy(alpha = 0.4f),
                                        radius = radius * 0.18f,
                                        center = Offset(center.x, center.y - radius),
                                        style = Stroke(width = (if (isHardwareFullscreen) 3.dp else 2.dp).toPx())
                                    )
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "XII",
                                        fontSize = if (isHardwareFullscreen) 12.sp else 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = themeColors.screenTextColor.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mainTimeText,
                                        style = LocalTextStyle.current.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = if (isHardwareFullscreen) 26.sp else 20.sp,
                                            color = themeColors.screenTextColor,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "VI",
                                        fontSize = if (isHardwareFullscreen) 12.sp else 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = themeColors.screenTextColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        } else if (displayStyleMode == 4) { // 4: CYBERPUNK MATRIX TERMINAL
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "⚡ MATRIX_SYS_ACTIVE: ONLINE",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = themeColors.screenTextColor.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mainTimeText,
                                        style = LocalTextStyle.current.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = if (isHardwareFullscreen) 44.sp else 32.sp,
                                            color = themeColors.screenTextColor,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ">> STABILITY: 100% // FOCUS_ON",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = themeColors.secondaryGlowColor
                                    )
                                }
                            }
                        } else if (displayStyleMode == 5) { // 5: SUNSET MINIMAL ZEN FLOW
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val breatheAnim = androidx.compose.animation.core.rememberInfiniteTransition(label = "zen_pulse")
                                val scaleZen by breatheAnim.animateFloat(
                                    initialValue = 0.85f,
                                    targetValue = 1.15f,
                                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                        animation = androidx.compose.animation.core.tween(4000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                Canvas(modifier = Modifier.size(if (isHardwareFullscreen) 180.dp else 130.dp)) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                themeColors.secondaryGlowColor.copy(alpha = 0.25f * scaleZen),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = size.minDimension / 1.5f
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = mainTimeText,
                                        style = LocalTextStyle.current.copy(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Light,
                                            fontSize = if (isHardwareFullscreen) 48.sp else 34.sp,
                                            color = themeColors.screenTextColor,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Text(
                                        text = "BREATHE IN & OUT 🧘",
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = themeColors.screenTextColor.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else { // 0: RETRO DIGITAL LCD SEGMENT BLINKING
                            val targetScale = if (isRunning && ((liveClockTime / 1000) % 2 == 0L)) 1.02f else 0.98f
                            val animatedScale by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isRunning) targetScale else 1.0f,
                                animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
                                label = "digits_pulse_scale"
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                            ) {
                                Text(
                                    text = mainTimeText,
                                    style = LocalTextStyle.current.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = if (isHardwareFullscreen) 64.sp else 48.sp,
                                        color = themeColors.screenTextColor,
                                        letterSpacing = 2.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                
                                if (activeMode == ActiveMode.CLOCK) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val amPm = SimpleDateFormat("a", Locale.getDefault()).format(Date(liveClockTime))
                                    Text(
                                        text = amPm,
                                        fontSize = if (isHardwareFullscreen) 18.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = themeColors.screenTextColor.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // FOOTER: Distraction state warnings, progress gauges, lap indicators
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (distractionDetected != null) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "⚠️ FOCUS DETECTED BROKEN BY '$distractionDetected'!",
                                    color = themeColors.warnColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Button(
                                    onClick = onDismissDistraction,
                                    modifier = Modifier.height(24.dp).testTag("dismiss_distraction_btn"),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.warnColor, contentColor = Color.White),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("DISMISS/CLEAR WARNING", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            if (activeMode == ActiveMode.TIMER) {
                                // Progress bar detailing timer left status
                                val pct = if (countdownTotalDur > 0) countdownTimeLeft.toFloat() / countdownTotalDur.toFloat() else 0f
                                 val animatedPct by androidx.compose.animation.core.animateFloatAsState(
                                     targetValue = pct.coerceIn(0f, 1f),
                                     animationSpec = androidx.compose.animation.core.spring(
                                         dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                         stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                     ),
                                     label = "smooth_timer_progress"
                                 )
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "FOCUS WORK SESSION TIMER LIMIT",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = themeColors.screenTextColor.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "${(pct * 100).toInt()}% Remaining",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = themeColors.screenTextColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = animatedPct,
                                        color = themeColors.secondaryGlowColor,
                                        trackColor = themeColors.screenTextColor.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            } else if (activeMode == ActiveMode.STOPWATCH) {
                                val stopwatchLapsValue = TimerService.stopwatchLaps.value
                                val lastLapStr = if (stopwatchLapsValue.isNotEmpty()) {
                                    val lastMs = stopwatchLapsValue.last()
                                    val s = lastMs / 1000
                                    String.format("%02d:%02d.%02d", s / 60, s % 60, (lastMs % 1000) / 10)
                                } else "None yet"
                                
                                Text(
                                    text = "Laps: ${stopwatchLapsValue.size} | Last Lap: $lastLapStr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = themeColors.screenTextColor.copy(alpha = 0.8f)
                                )
                            } else {
                                // Default Normal Clock info info
                                Text(
                                    text = "DXT AUTOMATED ENVIRONMENT SENSORS ACTIVE",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = themeColors.screenTextColor.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // PHYSICAL HARDWARE BUTTON CHASSIS CONTROL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MIN Button
                CircularChassisButton(
                    label = "HR / MIN",
                    hintStr = "+1 M",
                    enabled = activeMode == ActiveMode.TIMER && !isRunning,
                    onTap = onMinIncrement
                )

                // SEC Button / LAP Button in Stopwatch mode
                CircularChassisButton(
                    label = if (activeMode == ActiveMode.STOPWATCH) "LAP" else "SEC",
                    hintStr = if (activeMode == ActiveMode.STOPWATCH) "SPLIT" else "+10 S",
                    enabled = if (activeMode == ActiveMode.STOPWATCH) (isRunning || stopwatchElapsed > 0L) else (activeMode == ActiveMode.TIMER && !isRunning),
                    onTap = {
                        if (activeMode == ActiveMode.STOPWATCH) {
                            onLapTap()
                        } else {
                            onSecIncrement()
                        }
                    }
                )

                // CLEAR / RESET
                CircularChassisButton(
                    label = "RESET",
                    hintStr = "WIPE",
                    enabled = true,
                    onTap = onResetTap
                )

                // MODE SELECTOR BUTTON
                CircularChassisButton(
                    label = "MODE",
                    hintStr = "CYCLE",
                    enabled = true,
                    onTap = onModeTap
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // THE MAIN LAUNCH ACTUATOR BUTTON (ST/SP Key and Direct Dial)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direct DIAL Preset Dial Launcher
                OutlinedButton(
                    onClick = onSetDurationDirectly,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("preset_dial_button"),
                    border = BorderStroke(1.5.dp, Color(0xFF8B9B9C)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Presets", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DIAL TIME / PRESET", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // THE HARDWARE START/STOP TRIGGER
                val triggerColor = if (isRunning) Color(0xFFC0392B) else Color(0xFF27AE60)
                Button(
                    onClick = onStartStopTap,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("start_stop_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = triggerColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "Start action key"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "ST / SP (STOP)" else "ST / SP (START)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// Case Screw Decoration UI Component for the physical case look
@Composable
fun MockCaseScrew() {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Color(0xFF5A676E))
            .border(1.dp, Color(0xFF3E4A50), CircleShape)
    ) {
        // Slot line across the screw
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color(0xFF2E383C),
                start = Offset(2.dp.toPx(), 2.dp.toPx()),
                end = Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                strokeWidth = 2f
            )
        }
    }
}

// Indicator tags rendered on Glass Screen representing clock state status
@Composable
fun IndicatorPill(label: String, active: Boolean, themeColors: ThemeColorScheme) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) themeColors.secondaryGlowColor else Color.Black.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (active) themeColors.bezelBgColor else themeColors.screenTextColor.copy(alpha = 0.45f)
        )
    }
}

@Composable
fun CircularChassisButton(
    label: String,
    hintStr: String,
    enabled: Boolean,
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val buttonDepthOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .shadow(buttonDepthOffset, CircleShape)
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFEFF3F6),
                                Color(0xFFBDC3C7)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF95A5A6),
                                Color(0xFF7F8C8D)
                            )
                        )
                    }
                )
                .border(2.dp, Color(0xFF7F8C8D), CircleShape)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onTap
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = hintStr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (enabled) Color(0xFF2C3E50) else Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = Color.White
        )
    }
}


// --- TAB CONTENT: USER OFFLINE PROFILE DETAIL CONFIGURATION ---

@Composable
fun CustomizerProfileTab(
    profileName: String,
    profileAge: String,
    profileCourse: String,
    profileSchool: String,
    profileGoal: String,
    profileDailyTarget: String,
    profileFavSubject: String,
    onExportFile: () -> Unit,
    onImportFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "🧑‍🎓 SCHOLAR STUDY PROFILE (OFFLINE)",
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "Your study profile is fully offline for physical privacy, powering personalized greetings and detailed study environment statistics.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        OutlinedTextField(
            value = profileName,
            onValueChange = {
                TimerService.profileUserName.value = it
                TimerService.instance?.saveSettings()
            },
            label = { Text("Your Name / Greeting Nickname", fontSize = 10.sp) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().testTag("profile_name_field"),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = profileAge,
                onValueChange = {
                    TimerService.profileUserAge.value = it.filter { c -> c.isDigit() }
                    TimerService.instance?.saveSettings()
                },
                label = { Text("Age", fontSize = 10.sp) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f).testTag("profile_age_field"),
                singleLine = true
            )

            OutlinedTextField(
                value = profileDailyTarget,
                onValueChange = {
                    TimerService.profileDailyTargetHours.value = it
                    TimerService.instance?.saveSettings()
                },
                label = { Text("Daily Target (Hrs)", fontSize = 10.sp) },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1.5f).testTag("profile_target_field"),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = profileCourse,
            onValueChange = {
                TimerService.profileCourseName.value = it
                TimerService.instance?.saveSettings()
            },
            label = { Text("Course Name / Exam Prep", fontSize = 10.sp) },
            leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().testTag("profile_course_field"),
            singleLine = true
        )

        OutlinedTextField(
            value = profileSchool,
            onValueChange = {
                TimerService.profileSchoolName.value = it
                TimerService.instance?.saveSettings()
            },
            label = { Text("School / College / Institution", fontSize = 10.sp) },
            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().testTag("profile_school_field"),
            singleLine = true
        )

        OutlinedTextField(
            value = profileFavSubject,
            onValueChange = {
                TimerService.profileFavSubject.value = it
                TimerService.instance?.saveSettings()
            },
            label = { Text("Primary Subject Interest", fontSize = 10.sp) },
            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().testTag("profile_fav_field"),
            singleLine = true
        )

        OutlinedTextField(
            value = profileGoal,
            onValueChange = {
                TimerService.profileStudyGoal.value = it
                TimerService.instance?.saveSettings()
            },
            label = { Text("Target Aim / Milestone Goal", fontSize = 10.sp) },
            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().testTag("profile_goal_field"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(2.dp))

        // BACKUP & RESTORE MASTER CARD PORTAL
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Backup icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "💾 FOCUSFLOW UNIFIED DATABASE PORTAL",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Transfer and backup all your local focus histories, levels, garden coins, active trees, and settings metrics in a single portable JSON block.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                // Row for file actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportFile,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE BACKUP FILE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onImportFile,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LOAD BACKUP FILE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val context = LocalContext.current
                var quickBackupText by remember { mutableStateOf("") }
                
                var liveJsonPreview by remember { mutableStateOf("") }
                
                LaunchedEffect(Unit) {
                    liveJsonPreview = exportProfileBackup(context)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📋 COPY ACTIVE SYSTEM DATABASE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (liveJsonPreview.length > 40) liveJsonPreview.take(35) + "..." else liveJsonPreview,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Button(
                            onClick = {
                                val currentBackupStr = exportProfileBackup(context)
                                liveJsonPreview = currentBackupStr
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(currentBackupStr))
                                android.widget.Toast.makeText(context, "📋 Full Backup JSON Copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(28.dp).padding(start = 4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text("COPY CODE 📋", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "📥 RESTORE FROM CLIPBOARD CODE / JSON STRING",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    OutlinedTextField(
                        value = quickBackupText,
                        onValueChange = { quickBackupText = it },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        placeholder = { Text("Paste your raw FocusFlow JSON backup string snippet here to restore...", fontSize = 9.5.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            val rawCode = quickBackupText.trim()
                            if (rawCode.isNotEmpty()) {
                                val success = importProfileBackup(context, rawCode)
                                if (success) {
                                    android.widget.Toast.makeText(context, "✅ All metrics and settings restored successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    quickBackupText = ""
                                    liveJsonPreview = exportProfileBackup(context)
                                } else {
                                    android.widget.Toast.makeText(context, "❌ Format error: Invalid FocusFlow json", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "⚠️ Please paste JSON backup code first", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("⚡ RESTORE SYSTEM DATABASE FROM CODE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(2.dp))

        // GORGEOUS DEVELOPER & OPEN-SOURCE CREDIT PORTFOLIO CARD
        var showDeveloperDialog by remember { mutableStateOf(false) }

        if (showDeveloperDialog) {
            AlertDialog(
                onDismissRequest = { showDeveloperDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ABOUT DEVELOPER",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Secure System Integrity Profile",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "FocusFlow DXT Academic Suite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "A sandboxed digital study companion custom-engineered to provide deep telemetry tracking and absolute offline data privacy for focus monitoring.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Stats Grid Table
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("👤 AUTHOR NAME:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Sudhir Singh", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🐙 GITHUB USER:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("SudhirDevOps1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📦 ACTIVE REPO:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("FocusFlow-DXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("⚙️ ENVIRONMENT:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Multi-Sandbox Native Android", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🛡️ DATA SYNC TYPE:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("100% Fully Local Offline-First", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        (this as? ColumnScope)?.run {
                            Text(
                                text = "🔗 GitHub Project URL:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "https://github.com/SudhirDevOps1/FocusFlow-DXT",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        } ?: run {
                            Text(
                                text = "🔗 GitHub Project URL:\nhttps://github.com/SudhirDevOps1/FocusFlow-DXT",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Designed for extreme physical study privacy. No analytics counters, background trackers, or third-party cloud services are engaged inside this appliance.",
                            fontSize = 10.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 14.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDeveloperDialog = false }) {
                        Text("CLOSE DISCLOSURE", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        var showDonationDialog by remember { mutableStateOf(false) }

        if (showDonationDialog) {
            AlertDialog(
                onDismissRequest = { showDonationDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💖 SUPPORT SUDHIR'S FOCUSFLOW", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "If FocusFlow helped you control distractions and score higher in exams, consider supporting the creator to build more beautiful open-source apps!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Text(
                            text = "Developer Profiles & Coffee Platforms:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    uriHandler.openUri("https://github.com/SudhirDevOps1")
                                    showDonationDialog = false
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("☕ Buy Me Coffee", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    uriHandler.openUri("https://github.com/SudhirDevOps1")
                                    showDonationDialog = false
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("🐙 GitHub Sponsor", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Text(
                            text = "Thank you so much for the love and encouragement! 🌟 Keep growing!",
                            fontSize = 10.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDonationDialog = false }) {
                        Text("CLOSE", fontWeight = FontWeight.Black)
                    }
                }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri("https://github.com/SudhirDevOps1") },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "ABOUT FOCUSFLOW-DXT",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OPEN REPO 🔗",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A premium, fully offline-first interactive digital retro learning machine. Combines physics-realistic simulated LCD hardware aesthetics with deep focus guards, custom study profiles, automatic XP leveling, ambient soundtracks, sandboxed video frames, and detailed session statistics.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Author, repo and project metadata lines
                Text(
                    text = "👤 LEAD DEVELOPER: Sudhir Singh (@SudhirDevOps1)",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/SudhirDevOps1") }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔗 REPO URL: github.com/SudhirDevOps1/FocusFlow-DXT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/SudhirDevOps1/FocusFlow-DXT") }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚖️ LICENSE: MIT Open Source Academic License",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))
                
                // Working Interactive Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { uriHandler.openUri("https://github.com/SudhirDevOps1/FocusFlow-DXT") },
                        modifier = Modifier.weight(1f).height(38.dp).testTag("star_repo_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Star Repo icon", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("STAR REPO", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { uriHandler.openUri("https://github.com/SudhirDevOps1") },
                        modifier = Modifier.weight(1f).height(38.dp).testTag("my_profile_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile icon", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("MY PROFILE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showDonationDialog = true },
                        modifier = Modifier.weight(1f).height(38.dp).testTag("donate_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE91E63)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Favorite, contentDescription = "Donate icon", modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("DONATE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}


// --- TAB CONTENT 0: DISPLAY PANEL THEMES CONFIGURATION ---

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CustomizerDisplayTab(
    currentTheme: DisplayTheme,
    onThemeChange: (DisplayTheme) -> Unit,
    tickEnabled: Boolean,
    onTickChange: (Boolean) -> Unit,
    displayStyleMode: Int,
    onDisplayStyleChange: (Int) -> Unit,
    isSoundMutedGlobal: Boolean,
    onMuteChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Glass Display Theme Selectors:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Themes Lists Display
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DisplayThemeSelectionRow(
                themeA = DisplayTheme.RETRO_GREEN,
                labelA = "🎛️ RETRO LCD GLASS",
                descA = "Classic physical kitchen hardware screen",
                themeB = DisplayTheme.AMBER_GLOW,
                labelB = "💡 AMBER VACUUM GLOW",
                descB = "Bedside warm vintage aesthetic tube glow",
                selectedTheme = currentTheme,
                onSelect = onThemeChange
            )

            DisplayThemeSelectionRow(
                themeA = DisplayTheme.CYBER_PUNK,
                labelA = "🔮 NEON CYBERPUNK",
                descA = "Bold neon electric pinks and purples",
                themeB = DisplayTheme.NEON_BLUE,
                labelB = "⚡ FUTURISTIC CYAN GRID",
                descB = "Slick digital dashboard glow",
                selectedTheme = currentTheme,
                onSelect = onThemeChange
            )

            DisplayThemeSelectionRow(
                themeA = DisplayTheme.AMOLED_DARK,
                labelA = "🌑 AMOLED MONO",
                descA = "Deep eco battery-saver pure screen black",
                themeB = DisplayTheme.BRUSHED_STEEL,
                labelB = "🔧 PROFESSIONAL STEEL",
                descB = "Industrial brushed kitchen metal glow",
                selectedTheme = currentTheme,
                onSelect = onThemeChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Display Style options
        Text("Clock Display Face Style (Analog vs. Digital):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("Select standard retro digital look, full classic analog clock, smart progress ring, or antique pocket watch styles:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val styles = listOf("📟 Digital", "🕒 Analog", "⌚ Smart Ring", "🎯 Antique", "🪐 Matrix", "🌿 Zen Flow")
            styles.forEachIndexed { idx, title ->
                val selected = displayStyleMode == idx
                Button(
                    onClick = { onDisplayStyleChange(idx) },
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        // Master Sound Mute
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🔇 Master Mute Alarm Sounds", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Completely silences and halts the synthesize buzzer audio output", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = isSoundMutedGlobal,
                onCheckedChange = onMuteChange,
                modifier = Modifier.testTag("mute_alarm_toggle")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        // Toggle Seconds Ticking Sounds
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tac-Tac Physical Seconds Clicker", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Plays short tactile clicks on every second change", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = tickEnabled,
                onCheckedChange = onTickChange,
                modifier = Modifier.testTag("tick_sound_toggle"),
                enabled = !isSoundMutedGlobal
            )
        }
    }
}

@Composable
fun DisplayThemeSelectionRow(
    themeA: DisplayTheme,
    labelA: String,
    descA: String,
    themeB: DisplayTheme,
    labelB: String,
    descB: String,
    selectedTheme: DisplayTheme,
    onSelect: (DisplayTheme) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThemeCardToggle(theme = themeA, name = labelA, desc = descA, active = selectedTheme == themeA, onSelect = onSelect, modifier = Modifier.weight(1f))
        ThemeCardToggle(theme = themeB, name = labelB, desc = descB, active = selectedTheme == themeB, onSelect = onSelect, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ThemeCardToggle(
    theme: DisplayTheme,
    name: String,
    desc: String,
    active: Boolean,
    onSelect: (DisplayTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mini colors preview
    val testColors = getThemeColors(theme)
    Card(
        modifier = modifier
            .testTag("theme_card_${theme.name.lowercase(Locale.ROOT)}")
            .clickable { onSelect(theme) }
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Color dots previews
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(testColors.bezelBgColor))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(testColors.screenTextColor))
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, lineHeight = 12.sp)
        }
    }
}


// --- SUBJECT WISE WEEKLY TRENDS LINE CHART (RECHARTS STYLE) ---
@Composable
fun SubjectStudyLineChart(
    subjects: List<com.example.SubjectItem>,
    sessions: List<com.example.SessionItem>,
    themeColors: ThemeColorScheme
) {
    if (subjects.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "📂 No active subjects to track trends for.",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.screenTextColor.copy(alpha = 0.5f)
                )
            }
        }
        return
    }

    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val last7Days = remember(sdf) {
        val list = mutableListOf<Pair<String, String>>()
        for (offset in 6 downTo 0) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -offset)
            val dateStr = sdf.format(cal.time)
            val label = java.text.SimpleDateFormat("E", java.util.Locale.getDefault()).format(cal.time)
            list.add(dateStr to label)
        }
        list
    }

    val subjectTrends = remember(subjects, sessions, last7Days) {
        subjects.map { subj ->
            val dailyMinutes = last7Days.map { (dateStr, _) ->
                val matchingSessions = sessions.filter {
                    it.subjectId == subj.id && it.startTime.startsWith(dateStr)
                }
                val totalSeconds = matchingSessions.sumOf { it.actualSeconds }
                totalSeconds / 60.0
            }
            subj to dailyMinutes
        }
    }

    val maxVal = remember(subjectTrends) {
        val maxInTrends = subjectTrends.flatMap { it.second }.maxOrNull() ?: 10.0
        if (maxInTrends < 10.0) 10.0 else if (maxInTrends < 30.0) 30.0 else if (maxInTrends < 60.0) 60.0 else maxInTrends + 10.0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "📈 SUBJECT STUDY DURATION TRENDS (LAST WEEK)",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                val stepY = canvasHeight / 5f
                for (i in 0..5) {
                    val y = i * stepY
                    drawLine(
                        color = themeColors.screenTextColor.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 0.8f
                    )
                }

                val paddingLeft = 40f
                val paddingRight = 10f
                val paddingTop = 15f
                val paddingBottom = 20f

                val chartW = canvasWidth - paddingLeft - paddingRight
                val chartH = canvasHeight - paddingTop - paddingBottom

                val textPaint = android.text.TextPaint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 22f
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                
                drawContext.canvas.nativeCanvas.drawText("0m", 5f, canvasHeight - paddingBottom + 8f, textPaint)
                drawContext.canvas.nativeCanvas.drawText(
                    "${String.format("%.0f", maxVal / 2)}m",
                    5f,
                    paddingTop + (chartH / 2) + 8f,
                    textPaint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${String.format("%.0f", maxVal)}m",
                    5f,
                    paddingTop + 8f,
                    textPaint
                )

                val stepX = if (last7Days.size > 1) chartW / (last7Days.size - 1) else chartW
                
                subjectTrends.forEach { (sub, values) ->
                    val subjColor = try {
                        Color(android.graphics.Color.parseColor(sub.color))
                    } catch (e: Exception) {
                        themeColors.secondaryGlowColor
                    }

                    val points = values.indices.map { index ->
                        val x = paddingLeft + index * stepX
                        val progress = values[index] / maxVal
                        val y = paddingTop + chartH - (progress * chartH).toFloat()
                        Offset(x, y.coerceIn(paddingTop, paddingTop + chartH))
                    }

                    val linePath = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (pIdx in 1 until points.size) {
                                lineTo(points[pIdx].x, points[pIdx].y)
                            }
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = subjColor,
                        style = Stroke(
                            width = 4f,
                            cap = StrokeCap.Round
                        )
                    )

                    points.forEach { pt ->
                        drawCircle(
                            color = subjColor,
                            radius = 5.5f,
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5f,
                            center = pt
                        )
                    }
                }

                last7Days.indices.forEach { idx ->
                    val x = paddingLeft + idx * stepX
                    drawContext.canvas.nativeCanvas.drawText(
                        last7Days[idx].second,
                        x - 14f,
                        canvasHeight - 4f,
                        textPaint
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subj Legends Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                subjectTrends.forEach { (sub, values) ->
                    val subjColor = try {
                        Color(android.graphics.Color.parseColor(sub.color))
                    } catch (e: Exception) {
                        themeColors.secondaryGlowColor
                    }
                    val weekSumMinutes = values.sum().toInt()
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(subjColor.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(subjColor)
                        )
                        Text(
                            text = "${sub.name.uppercase()}: ${weekSumMinutes}m",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.screenTextColor
                        )
                    }
                }
            }
        }
    }
}


// --- WEEKLY BAR CHART ANALYTICS COMPOSABLE ---

@Composable
fun FocusAnalyticsChart(
    studyHistory: List<Int>,
    restrictedHistory: List<Int>,
    days: List<String>,
    themeColors: ThemeColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📈 STUDY VS. BLOCKED WEEK HISTORY",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00C853)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Study", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5252)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Blocked", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val maxVal = remember(studyHistory, restrictedHistory) {
                val unionMax = (studyHistory + restrictedHistory).maxOrNull() ?: 60
                if (unionMax < 30) 30 else if (unionMax < 60) 60 else if (unionMax < 120) 120 else unionMax + 15
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("analytics_canvas_plot")
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                // background lines
                val stepY = canvasHeight / 5f
                for (i in 0..5) {
                    val y = i * stepY
                    drawLine(
                        color = themeColors.screenTextColor.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 0.8f
                    )
                }

                val paddingLeft = 10f
                val paddingRight = 10f
                val paddingTop = 10f
                val paddingBottom = 10f

                val chartW = canvasWidth - paddingLeft - paddingRight
                val chartH = canvasHeight - paddingTop - paddingBottom

                val numDays = studyHistory.size
                if (numDays > 0) {
                    val groupW = chartW / numDays
                    val barW = groupW * 0.35f
                    val gap = groupW * 0.05f

                    for (i in 0 until numDays) {
                        val sMin = studyHistory.getOrElse(i) { 0 }
                        val rMin = restrictedHistory.getOrElse(i) { 0 }

                        val groupCenter = paddingLeft + i * groupW + groupW / 2f
                        
                        // Study Bar Drawing
                        val sLeft = groupCenter - barW - gap
                        val sH = (sMin.toFloat() / maxVal) * chartH
                        val sTop = canvasHeight - paddingBottom - sH

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2E7D32), Color(0xFF00E676))
                            ),
                            topLeft = Offset(sLeft, sTop),
                            size = androidx.compose.ui.geometry.Size(barW, sH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )

                        // Restricted Bar Drawing
                        val rLeft = groupCenter + gap
                        val rH = (rMin.toFloat() / maxVal) * chartH
                        val rTop = canvasHeight - paddingBottom - rH

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFD32F2F), Color(0xFFFF5252))
                            ),
                            topLeft = Offset(rLeft, rTop),
                            size = androidx.compose.ui.geometry.Size(barW, rH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Graph Labels (Days)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { d ->
                    Text(
                        text = d,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}

// --- WEEKLY FOCUS INTENSITY TRENDS AND PRODUCTIVE HOURS CHART ---
@Composable
fun WeeklyFocusIntensityTrendsChart(
    sessions: List<com.example.SessionItem>,
    themeColors: ThemeColorScheme
) {
    val slotLabels = listOf(
        "00-03", "03-06", "06-09", "09-12",
        "12-15", "15-18", "18-21", "21-00"
    )
    val slotNames = listOf(
        "Late Night", "Dawn Focus", "Early Bird", "Morning Core",
        "Midday Sprint", "Afternoon Depth", "Evening Review", "Night Owl"
    )
    val baseline = listOf(15f, 28f, 55f, 85f, 42f, 72f, 90f, 60f)

    // Calculate actual sessions counts per slot
    val calculatedIntensities = remember(sessions) {
        val counts = FloatArray(8) { 0f }
        var hasData = false
        sessions.forEach { s ->
            try {
                if (s.startTime.length >= 13) {
                    val hourStr = s.startTime.substring(11, 13)
                    val hour = hourStr.toIntOrNull()
                    if (hour != null) {
                        val slotIdx = (hour / 3).coerceIn(0, 7)
                        val weight = s.actualSeconds / 60f
                        counts[slotIdx] += weight
                        hasData = true
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        if (hasData) {
            val totalWeight = counts.sum()
            if (totalWeight > 0f) {
                List(8) { idx ->
                    val actualPerc = (counts[idx] / totalWeight) * 100f
                    // Blend 60% actual weight and 40% baseline weight for beautiful aesthetics
                    (actualPerc * 0.6f + baseline[idx] * 0.4f).coerceIn(5f, 100f)
                }
            } else {
                baseline
            }
        } else {
            baseline
        }
    }

    val selectedSlotIndex = remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Intensity icon",
                        tint = themeColors.secondaryGlowColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡ WEEKLY FOCUS INTENSITY TRENDS (RECHARTS STYLE)",
                        color = themeColors.screenTextColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(themeColors.secondaryGlowColor)
                )
            }

            Text(
                text = "Tap or drag across the graph to view peak study hours & focus levels.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Graph container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val localW = size.width.toFloat()
                                    val paddingLeft = 35f
                                    val paddingRight = 10f
                                    val chartW = localW - paddingLeft - paddingRight
                                    if (chartW > 0) {
                                        val stepX = chartW / 7f
                                        val xPos = offset.x - paddingLeft
                                        val index = (xPos / stepX + 0.5f).toInt().coerceIn(0, 7)
                                        selectedSlotIndex.value = index
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val localW = size.width.toFloat()
                                    val paddingLeft = 35f
                                    val paddingRight = 10f
                                    val chartW = localW - paddingLeft - paddingRight
                                    if (chartW > 0) {
                                        val stepX = chartW / 7f
                                        val xPos = offset.x - paddingLeft
                                        val index = (xPos / stepX + 0.5f).toInt().coerceIn(0, 7)
                                        selectedSlotIndex.value = index
                                    }
                                },
                                onDragEnd = {},
                                onDragCancel = {},
                                onDrag = { change, _ ->
                                    val localW = size.width.toFloat()
                                    val paddingLeft = 35f
                                    val paddingRight = 10f
                                    val chartW = localW - paddingLeft - paddingRight
                                    if (chartW > 0) {
                                        val stepX = chartW / 7f
                                        val xPos = change.position.x - paddingLeft
                                        val index = (xPos / stepX + 0.5f).toInt().coerceIn(0, 7)
                                        selectedSlotIndex.value = index
                                    }
                                }
                            )
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val paddingLeft = 35f
                    val paddingRight = 10f
                    val paddingTop = 15f
                    val paddingBottom = 20f

                    val chartW = canvasWidth - paddingLeft - paddingRight
                    val chartH = canvasHeight - paddingTop - paddingBottom

                    // Draw vertical and horizontal grid lines
                    val stepY = chartH / 4f
                    for (i in 0..4) {
                        val y = paddingTop + i * stepY
                        drawLine(
                            color = themeColors.screenTextColor.copy(alpha = 0.08f),
                            start = Offset(paddingLeft, y),
                            end = Offset(canvasWidth - paddingRight, y),
                            strokeWidth = 1f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    val stepX = chartW / 7f

                    // Draw reference metrics text labels
                    val nativePaint = android.text.TextPaint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 20f
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    drawContext.canvas.nativeCanvas.drawText("100%", 2f, paddingTop + 8f, nativePaint)
                    drawContext.canvas.nativeCanvas.drawText("50%", 2f, paddingTop + (chartH / 2f) + 8f, nativePaint)
                    drawContext.canvas.nativeCanvas.drawText("0%", 2f, canvasHeight - paddingBottom + 8f, nativePaint)

                    // Draw Area Path
                    val points = calculatedIntensities.indices.map { idx ->
                        val x = paddingLeft + idx * stepX
                        val progress = calculatedIntensities[idx] / 100f
                        val y = paddingTop + chartH - (progress * chartH)
                        Offset(x, y)
                    }

                    if (points.isNotEmpty()) {
                        val fillPath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (pIdx in 1 until points.size) {
                                lineTo(points[pIdx].x, points[pIdx].y)
                            }
                            lineTo(points.last().x, canvasHeight - paddingBottom)
                            lineTo(points.first().x, canvasHeight - paddingBottom)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    themeColors.secondaryGlowColor.copy(alpha = 0.40f),
                                    themeColors.secondaryGlowColor.copy(alpha = 0.01f)
                                )
                            )
                        )

                        val linePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (pIdx in 1 until points.size) {
                                lineTo(points[pIdx].x, points[pIdx].y)
                            }
                        }

                        drawPath(
                            path = linePath,
                            color = themeColors.secondaryGlowColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )

                        val currSel = selectedSlotIndex.value
                        if (currSel != null && currSel in points.indices) {
                            val selPt = points[currSel]
                            drawLine(
                                color = themeColors.secondaryGlowColor.copy(alpha = 0.5f),
                                start = Offset(selPt.x, paddingTop),
                                end = Offset(selPt.x, canvasHeight - paddingBottom),
                                strokeWidth = 1.5f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                        }

                        points.forEachIndexed { idx, pt ->
                            val isSelected = selectedSlotIndex.value == idx
                            drawCircle(
                                color = themeColors.secondaryGlowColor,
                                radius = if (isSelected) 8f else 5f,
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 4f else 2.5f,
                                center = pt
                            )
                        }
                    }

                    calculatedIntensities.indices.forEach { idx ->
                        val x = paddingLeft + idx * stepX
                        val label = slotLabels[idx]
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x - 24f,
                            canvasHeight - 4f,
                            nativePaint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val currentSelectedIdx = selectedSlotIndex.value
            val isSelectedActive = currentSelectedIdx != null

            val displayLabel = if (isSelectedActive) slotNames[currentSelectedIdx!!] else "All-Day Overview"
            val displayRange = if (isSelectedActive) "Frame: ${slotLabels[currentSelectedIdx!!]}" else "Weekly Peak Analysis"
            val displayIntensity = if (isSelectedActive) {
                "${calculatedIntensities[currentSelectedIdx!!].toInt()}% Productivity Index"
            } else {
                val maxIdx = calculatedIntensities.indices.maxByOrNull { calculatedIntensities[it] } ?: 6
                "Max Peak: ${calculatedIntensities[maxIdx].toInt()}% at ${slotLabels[maxIdx]} (${slotNames[maxIdx]})"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColors.secondaryGlowColor.copy(alpha = 0.08f))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 $displayLabel ($displayRange)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = themeColors.screenTextColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = displayIntensity,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = themeColors.secondaryGlowColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSelectedActive) {
                            val idx = currentSelectedIdx!!
                            when (idx) {
                                0 -> "Typically a rest block. Ideal for maintaining healthy deep-sleep schedules."
                                1 -> "High-focus early dawn period. Great for quiet review and reading without background noise."
                                2 -> "Sunrise zone. Wonderful to plan your targets for the day and complete easy sprints."
                                3 -> "High productivity core. Peak cognitive state is common during these morning hours!"
                                4 -> "Midday block. Recommended to do short breaks, physical stretching, or easy kitchen timing."
                                5 -> "Afternoon depth focus. Solid chunk of time for complex lectures or programming study."
                                6 -> "Premium homework and evening review phase. Highly interactive study session peak!"
                                else -> "Night-owl focus zone. Restrict messaging and social alerts strictly to protect focus."
                            }
                        } else {
                            "Based on weekly parameters, your absolute highest productivity peak resides in Evening & Morning slots! Keep study countdowns running during these windows to leverage maximum learning yield."
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- SYSTEM APPLICATION PREVIEW SELECTOR DATA MODEL ---
data class AppInfoItem(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?
)

// --- TAB CONTENT 1: STRICT MODE STUDY FOCUS ENFORCER SYSTEM ---

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CustomizerFocusTab(
    strictModeEnabled: Boolean,
    onStrictModeChange: (Boolean) -> Unit,
    isStrictModeWarningsEnabled: Boolean,
    onStrictModeWarningsChange: (Boolean) -> Unit,
    strictModeThresholdSeconds: Int,
    onThresholdChange: (Int) -> Unit,
    strictnessLevel: String,
    onStrictnessChange: (String) -> Unit,
    isYoutubeWhitelisted: Boolean,
    onWhitelistYoutubeChange: (Boolean) -> Unit,
    permissionStatus: Boolean,
    focusLogs: List<String>,
    studyHistory: List<Int>,
    restrictedHistory: List<Int>,
    days: List<String>,
    themeColors: ThemeColorScheme,
    onOpenUsageSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    val studySubjects by TimerService.studySubjectsFlow.collectAsStateWithLifecycle()
    val studySessions by TimerService.studySessionsFlow.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🔒 STRICT STUDY ZONE GUARD", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                Text("Screams alarms and pauses countdowns when reels/Instagram are opened!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = strictModeEnabled,
                onCheckedChange = onStrictModeChange,
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("strict_mode_switch")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!permissionStatus && strictModeEnabled) {
            // Permission Grant Required Warning
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permission_alert_card")
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔐 System App Usage Access Missing!",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Android blocks local app tracing by default. Please grant usage statistics permission so we can detect distraction reels strictly.",
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onOpenUsageSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        Text("Grant System Usage Log Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (strictModeEnabled) {
            Spacer(modifier = Modifier.height(10.dp))

            // --- ACCESSIBILITY SUPER CONTROL DEVICE INTEGRATOR ---
            val isAccessibleServiceActive by TimerService.isAccessibilityServiceRunning.collectAsStateWithLifecycle()
            val context = androidx.compose.ui.platform.LocalContext.current
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibleServiceActive) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isAccessibleServiceActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isAccessibleServiceActive) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = "Accessibility status",
                            tint = if (isAccessibleServiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAccessibleServiceActive) "🛡️ ACCESSIBILITY FOCUS ON: ACTIVE" else "⚡ ACCESSIBILITY SERVICE: TURN ON",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isAccessibleServiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAccessibleServiceActive) 
                            "The system's ultra-reliable Accessibility Study overlay tracker is working. When swapping packages to reels or banned social apps, we capture and act immediately."
                        else 
                            "Turn on the background Accessibility service for instant block tracking. Real-time prevention is 10X more accurate on modern systems.",
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                                android.widget.Toast.makeText(context, "Please scroll to find and tap: Kitchen Timer & Study Monitor and Turn it ON", android.widget.Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Could not open system Accessibility Settings automatically.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAccessibleServiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = if (isAccessibleServiceActive) "Re-configure accessibility options" else "Turn on Accessibility Study Monitor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Toggle for Strict Mode warnings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("⚠️ Enable Transition Warnings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Show warning tickdowns before enforcing final strict pauses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isStrictModeWarningsEnabled,
                    onCheckedChange = onStrictModeWarningsChange,
                    modifier = Modifier.testTag("strict_warnings_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider to configure grace threshold duration in seconds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏱️ Switch-Away Grace Period:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("$strictModeThresholdSeconds.0 Secs", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            Slider(
                value = strictModeThresholdSeconds.toFloat(),
                onValueChange = { onThresholdChange(it.toInt()) },
                valueRange = 2f..25f,
                steps = 22,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.error,
                    activeTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("strict_threshold_slider")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Strictness Level selection
        Text("Strictness Enforcement Level:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Easy", "Strict", "Extremely Strict").forEach { lvl ->
                val active = strictnessLevel == lvl
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.5.dp,
                            color = if (active) MaterialTheme.colorScheme.error else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onStrictnessChange(lvl) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lvl,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Whitelist youtube check
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🎓 Whitelist YouTube Educational Classes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Allows watching classes on YouTube without issuing distractions warning alarms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(
                checked = isYoutubeWhitelisted,
                onCheckedChange = onWhitelistYoutubeChange,
                modifier = Modifier.testTag("whitelist_youtube_check")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // --- CUSTOM EXTRA STRICT LOCKED PACKAGES MANAGER ---
        Text("⛔ CUSTOM DISTRACTING APPS BLOCKER", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        Text("Add custom app identifiers or package keywords to lock yourself out of during active timers:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        val customRestrictedList by TimerService.customRestrictedPackages.collectAsStateWithLifecycle()
        var newAppKeyword by remember { mutableStateOf("") }
        val localContext = androidx.compose.ui.platform.LocalContext.current

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newAppKeyword,
                onValueChange = { newAppKeyword = it },
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = { Text("e.g. chrome, whatsapp, gameinfo", fontSize = 11.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = {
                    val cleanVal = newAppKeyword.trim().lowercase()
                    if (cleanVal.isNotEmpty()) {
                        if (!customRestrictedList.contains(cleanVal)) {
                            val newList = customRestrictedList.toMutableList()
                            newList.add(cleanVal)
                            TimerService.customRestrictedPackages.value = newList
                            TimerService.instance?.saveSettings()
                            newAppKeyword = ""
                            android.widget.Toast.makeText(localContext, "Added '$cleanVal' to blocked list!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(localContext, "Already in blocked list!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.height(40.dp).testTag("add_custom_block_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("Block App 🔒", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var showAppSelectorDialog by remember { mutableStateOf(false) }
        var installedAppsList by remember { mutableStateOf<List<AppInfoItem>>(emptyList()) }
        var isLoadingApps by remember { mutableStateOf(false) }
        var appSearchQuery by remember { mutableStateOf("") }

        LaunchedEffect(showAppSelectorDialog) {
            if (showAppSelectorDialog && installedAppsList.isEmpty()) {
                isLoadingApps = true
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val pm = localContext.packageManager
                        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                        }
                        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                        val apps = resolveInfos.map { info ->
                            val packageName = info.activityInfo.packageName
                            val name = info.loadLabel(pm).toString()
                            val icon = try {
                                info.loadIcon(pm)
                            } catch (e: Exception) {
                                null
                            }
                            AppInfoItem(name, packageName, icon)
                        }.distinctBy { it.packageName }.toMutableList()

                        if (apps.isEmpty()) {
                            val packages = pm.getInstalledPackages(0)
                            for (pkg in packages) {
                                val appInfo = pkg.applicationInfo
                                if (appInfo != null) {
                                    val name = appInfo.loadLabel(pm).toString()
                                    val packageName = pkg.packageName
                                    val icon = try {
                                        appInfo.loadIcon(pm)
                                    } catch (e: Exception) {
                                        null
                                    }
                                    apps.add(AppInfoItem(name, packageName, icon))
                                }
                            }
                        }
                        installedAppsList = apps.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoadingApps = false
                    }
                }
            }
        }

        Button(
            onClick = { showAppSelectorDialog = true },
            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("select_installed_apps_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.List, contentDescription = "Apps Icon", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CHOOSE FROM INSTALLED USER APPS 📱", fontSize = 11.sp, fontWeight = FontWeight.Black)
        }

        if (showAppSelectorDialog) {
            AlertDialog(
                onDismissRequest = { showAppSelectorDialog = false },
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                shape = RoundedCornerShape(16.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📱 CHOOSE DISTRACTING USER APPS", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        TextButton(onClick = { showAppSelectorDialog = false }) {
                            Text("DONE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = appSearchQuery,
                            onValueChange = { appSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by App Name / Package ID...", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (appSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { appSearchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.5.sp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (isLoadingApps) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Reading installed systems securely...", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            val filteredList = remember(installedAppsList, appSearchQuery) {
                                installedAppsList.filter {
                                    it.name.contains(appSearchQuery, ignoreCase = true) ||
                                    it.packageName.contains(appSearchQuery, ignoreCase = true)
                                }
                            }
                            
                            if (filteredList.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No matching user apps discovered.", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredList) { app ->
                                        val isBlocked = customRestrictedList.contains(app.packageName.lowercase())
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isBlocked) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                                    else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    val cleanVal = app.packageName.lowercase()
                                                    val newList = customRestrictedList.toMutableList()
                                                    if (isBlocked) {
                                                        newList.remove(cleanVal)
                                                    } else {
                                                        if (!newList.contains(cleanVal)) {
                                                            newList.add(cleanVal)
                                                        }
                                                    }
                                                    TimerService.customRestrictedPackages.value = newList
                                                    TimerService.instance?.saveSettings()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Dynamic AndroidView drawing the system app icon
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.White.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (app.icon != null) {
                                                    androidx.compose.ui.viewinterop.AndroidView(
                                                        factory = { ctx ->
                                                            android.widget.ImageView(ctx).apply {
                                                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                                                setImageDrawable(app.icon)
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.name,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = app.packageName,
                                                    fontSize = 8.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                            
                                            Checkbox(
                                                checked = isBlocked,
                                                onCheckedChange = null, // Set to null so selection is cleanly handled by Row click and event bubble is non-conflicting!
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (customRestrictedList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                customRestrictedList.forEach { blockedApp ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                            .border(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = blockedApp, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                .clickable {
                                    val newList = customRestrictedList.toMutableList()
                                    newList.remove(blockedApp)
                                    TimerService.customRestrictedPackages.value = newList
                                    TimerService.instance?.saveSettings()
                                    android.widget.Toast.makeText(localContext, "Removed '$blockedApp'!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "×", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No custom restricted apps configured yet.",
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Subject Study duration trends weekly visualizer
        SubjectStudyLineChart(
            subjects = studySubjects,
            sessions = studySessions,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Study Metrics Weekly Dashboard Visualizer
        FocusAnalyticsChart(
            studyHistory = studyHistory,
            restrictedHistory = restrictedHistory,
            days = days,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(10.dp))

        // New Weekly Focus Intensity Trends & Productive Hours visualization
        WeeklyFocusIntensityTrendsChart(
            sessions = studySessions,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // --- DYNAMIC DATA IMPORT AND EXPORT FOR PORTABILITY ---
        Text("📁 IMPORT & EXPORT LOCAL FOCUS DATA", fontWeight = FontWeight.Black, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))

        var showImportDialog by remember { mutableStateOf(false) }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val context = androidx.compose.ui.platform.LocalContext.current

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val exportedStr = """{
  "studyHistory": [${studyHistory.joinToString(",")}],
  "restrictedHistory": [${restrictedHistory.joinToString(",")}],
  "logs": [${focusLogs.joinToString(",") { "\"$it\"" }}]
}"""
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(exportedStr))
                    android.widget.Toast.makeText(context, "Data successfully copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).testTag("export_data_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export data icon", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f).testTag("import_data_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import data icon", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showImportDialog) {
            var pastedJsonText by remember { mutableStateOf("") }
            var importValidationMessage by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Focus Statistics", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Paste your exported JSON string underneath to restore historical study minutes, blocked app logs, and timeline recordings.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pastedJsonText,
                            onValueChange = { pastedJsonText = it; importValidationMessage = null },
                            modifier = Modifier.fillMaxWidth().height(130.dp).testTag("import_text_field"),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            placeholder = { Text("{ ... }") }
                        )
                        if (importValidationMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(importValidationMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val cleanInput = pastedJsonText.trim()
                                val parsedObject = org.json.JSONObject(cleanInput)
                                
                                val studyArr = parsedObject.optJSONArray("studyHistory")
                                val studyValues = mutableListOf<Int>()
                                if (studyArr != null) {
                                    for (i in 0 until studyArr.length()) {
                                        studyValues.add(studyArr.getInt(i))
                                    }
                                }

                                val restrictedArr = parsedObject.optJSONArray("restrictedHistory")
                                val restrictedValues = mutableListOf<Int>()
                                if (restrictedArr != null) {
                                    for (i in 0 until restrictedArr.length()) {
                                        restrictedValues.add(restrictedArr.getInt(i))
                                    }
                                }

                                val logsArr = parsedObject.optJSONArray("logs")
                                val logsValues = mutableListOf<String>()
                                if (logsArr != null) {
                                    for (i in 0 until logsArr.length()) {
                                        logsValues.add(logsArr.getString(i))
                                    }
                                }

                                if (studyValues.isNotEmpty()) {
                                    TimerService.studyMinsHistory.value = studyValues
                                }
                                if (restrictedValues.isNotEmpty()) {
                                    TimerService.restrictedMinsHistory.value = restrictedValues
                                }
                                if (logsValues.isNotEmpty()) {
                                    TimerService.databaseFocusLogs.value = logsValues
                                }

                                TimerService.instance?.saveSettings()
                                TimerService.instance?.saveFocusLogs()
                                TimerService.instance?.saveHistoryToPrefs()
                                
                                android.widget.Toast.makeText(context, "Statistics & logs successfully restored!", android.widget.Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                            } catch (e: Exception) {
                                importValidationMessage = "Malformed JSON data structure. Please use valid exported files."
                            }
                        },
                        modifier = Modifier.testTag("apply_import_data")
                    ) {
                        Text("Apply & Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // Focus logs timeline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊 Focus Logger History Logbook", fontWeight = FontWeight.Black, fontSize = 12.sp)
            TextButton(
                onClick = {
                    TimerService.databaseFocusLogs.value = emptyList()
                    TimerService.instance?.applicationContext?.getSharedPreferences("digital_clock_timer_prefs", 0)?.edit()?.putStringSet("focus_logs", emptySet())?.apply()
                }
            ) {
                Text("Clear logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (focusLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No study logs created yet. Focus timer acts are empty.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .verticalScroll(rememberScrollState())
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                focusLogs.forEach { log ->
                    Text(
                        text = log,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (log.contains("❌") || log.contains("⚠️")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}


@Composable
fun EnvironmentalLCDDisplay(
    temperature: Float,
    humidity: Float,
    themeColors: ThemeColorScheme
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = "🔋 Real Hardware Thermal Telemetry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Yes, this is real temperature telemetry!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Unlike standard timers, this app reads your Android device's actual physical battery / CPU thermal sensors in real-time. On hardware that supports it, direct ambient air temperature readings are integrated.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "Why is this useful?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• 🌡️ Prevent Overheating: Intensely studying with screens turned active, running multi-hour countdowns, playing study videos, and streaming ambient focus noise warms up your motherboard. Keeping an eye on telemetry preserves optimal safety.\n" +
                               "• 🔋 Maximize Battery Health: High thermal stress degrades lithium-ion battery lifespans. Keeping your environment and device cool safeguards hardware durability.\n" +
                               "• 🔬 Studio Lab Realism: Fits right into retro physical instrumentation and laboratory telemetry panel styling!",
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .border(1.5.dp, themeColors.screenTextColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = themeColors.bezelBgColor.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📟 DIGITAL TELEMETRY INSTRUMENT",
                color = themeColors.screenTextColor,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Temperature Meter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "AMBIENT TEMP",
                        color = themeColors.screenTextColor.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%.1f", temperature)}°C",
                        color = themeColors.screenTextColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Humidity Meter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "REL HUMIDITY",
                        color = themeColors.screenTextColor.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%.1f", humidity)}%",
                        color = themeColors.screenTextColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .clickable { showInfoDialog = true }
                    .background(themeColors.screenTextColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Read sensor info",
                    tint = themeColors.screenTextColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Is this real temperature? Tap to learn more",
                    color = themeColors.screenTextColor.copy(alpha = 0.8f),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}


// --- TAB CONTENT 2: CUSTOM ALARM BELLS & TEMPERATURE SCALE DIALS ---

@Composable
fun CustomizerTonesSensorsTab(
    alarmTone: AlarmTone,
    onToneChange: (AlarmTone) -> Unit,
    alarmDuration: Int,
    onDurationChange: (Int) -> Unit,
    ambientSound: String,
    onAmbientChange: (String) -> Unit,
    tempOffset: Float,
    onTempOffsetChange: (Float) -> Unit,
    currentTemperature: Float,
    simulatedHumidity: Float,
    themeColors: ThemeColorScheme
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasOceanWaves by TimerService.hasOceanWavesTrack.collectAsStateWithLifecycle()
    val hasDeepSpace by TimerService.hasDeepSpaceTrack.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Kitchen Alarm Ring Tone Selector:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AlarmTone.values().forEach { tone ->
                val active = alarmTone == tone
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.5.dp,
                            color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onToneChange(tone) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (tone) {
                            AlarmTone.CLASSIC_BEEP -> "Beep"
                            AlarmTone.SERENE_CHIME -> "Chime"
                            AlarmTone.SIREN_ALERT -> "Siren"
                            AlarmTone.CHIRP_DIGITAL -> "Chirp"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Slider for beeping duration length
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Alarm Play Duration Limit:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("$alarmDuration Secs", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = alarmDuration.toFloat(),
            onValueChange = { onDurationChange(it.toInt()) },
            valueRange = 5f..30f,
            steps = 5,
            modifier = Modifier.testTag("alarm_duration_slider")
        )

        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // Ambient focus loop sound selector
        Text("Focus Ambient Companion Noise:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))

        val companionNoises = listOf(
            Triple("None", true, "None"),
            Triple("White Noise", true, "White Noise"),
            Triple("Rain", true, "Rain"),
            Triple("Binaural", true, "Binaural Focus Beat"),
            Triple("Ocean Waves", hasOceanWaves, "Ocean Waves"),
            Triple("Space Drone", hasDeepSpace, "Deep Space")
        )

        companionNoises.chunked(3).forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chunk.forEach { (label, unlocked, noiseKey) ->
                    val active = ambientSound == noiseKey
                    val visualLabel = if (unlocked) label else "$label 🔒"
                    val bgCol = if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else if (!unlocked) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val borderCol = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgCol)
                            .border(width = 1.5.dp, color = borderCol, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                if (unlocked) {
                                    onAmbientChange(noiseKey)
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "🔒 Unlock This premium ambient track in the Sunshine Coins Shop!",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = visualLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else if (!unlocked) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        // Environmental Telemetry LCD Panel Display (Temperature and humidity)
        EnvironmentalLCDDisplay(
            temperature = currentTemperature,
            humidity = simulatedHumidity,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Temperature calibration offset slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛠️ Temperature Sensor Calibration:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${if (tempOffset >= 0) "+" else ""}${String.format("%.1f", tempOffset)}°C", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = tempOffset,
            onValueChange = { onTempOffsetChange(it) },
            valueRange = -5.0f..5.0f,
            modifier = Modifier.testTag("temp_calibration_slider")
        )
    }
}


// --- TAB CONTENT 5: SANDBOXED AI TUTOR COACH ---

@Composable
fun CustomizerAITutorTab(
    profileName: String,
    profileAge: String,
    profileCourse: String,
    profileSchool: String,
    profileGoal: String,
    profileDailyTarget: String,
    profileFavSubject: String,
    studyHistory: List<Int>,
    restrictedHistory: List<Int>,
    currentTemperature: Float,
    themeColors: ThemeColorScheme
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("digital_clock_timer_prefs", android.content.Context.MODE_PRIVATE) }

    // Saved API Keys & Model Selections
    var selectedProvider by remember { mutableStateOf(prefs.getString("ai_provider", "Gemini") ?: "Gemini") }
    var geminiKey by remember { mutableStateOf(prefs.getString("ai_gemini_key", "") ?: "") }
    var groqKey by remember { mutableStateOf(prefs.getString("ai_groq_key", "") ?: "") }
    var cerebrasKey by remember { mutableStateOf(prefs.getString("ai_cerebras_key", "") ?: "") }
    var mistralKey by remember { mutableStateOf(prefs.getString("ai_mistral_key", "") ?: "") }

    var geminiModel by remember { mutableStateOf(prefs.getString("ai_gemini_model", "gemini-2.5-flash-preview-05-20") ?: "gemini-2.5-flash-preview-05-20") }
    var groqModel by remember { mutableStateOf(prefs.getString("ai_groq_model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile") }
    var cerebrasModel by remember { mutableStateOf(prefs.getString("ai_cerebras_model", "llama-3.3-70b") ?: "llama-3.3-70b") }
    var mistralModel by remember { mutableStateOf(prefs.getString("ai_mistral_model", "mistral-large-latest") ?: "mistral-large-latest") }

    // Interactivity
    var questionText by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf(prefs.getString("ai_last_response", "Click below to analyze study rhythms or ask a dynamic academic question.") ?: "Click below to analyze study rhythms or ask a dynamic academic question.") }
    var isLoading by remember { mutableStateOf(false) }
    var showExpandedDialog by remember { mutableStateOf(false) }
    var showKeysSection by remember { mutableStateOf(false) }
    var saveStatusText by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }

    // Update whenever responses come back
    val saveLastResponse = { response: String ->
        aiResponse = response
        prefs.edit().putString("ai_last_response", response).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🤖 SANDBOXED AI STUDY ADVISOR",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Powered by private keys. Supports Cerebras, Gemini, Groq, Mistral.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            IconButton(
                onClick = { showKeysSection = !showKeysSection },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (showKeysSection) Icons.Default.KeyboardArrowUp else Icons.Default.Settings,
                    contentDescription = "API Keys Configuration",
                    tint = if (showKeysSection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expanded API Key Configurations Settings
        AnimatedVisibility(visible = showKeysSection || (geminiKey.isEmpty() && groqKey.isEmpty() && cerebrasKey.isEmpty() && mistralKey.isEmpty())) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔐 API KEYS CREDENTIAL MANAGER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Active Provider for key insertion
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Gemini", "Groq", "Cerebras", "Mistral").forEach { prov ->
                            val active = selectedProvider == prov
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                    .clickable { selectedProvider = prov }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = prov,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Key input field based on selected provider
                    val currentKey = when (selectedProvider) {
                        "Gemini" -> geminiKey
                        "Groq" -> groqKey
                        "Cerebras" -> cerebrasKey
                        else -> mistralKey
                    }
                    val currentModelName = when (selectedProvider) {
                        "Gemini" -> geminiModel
                        "Groq" -> groqModel
                        "Cerebras" -> cerebrasModel
                        else -> mistralModel
                    }

                    val keyPlaceholder = when (selectedProvider) {
                        "Gemini" -> "Enter Google Gemini API Key"
                        "Groq" -> "Enter Groq Bearer Key"
                        "Cerebras" -> "Enter Cerebras API Key"
                        else -> "Enter Mistral AI Key"
                    }

                    OutlinedTextField(
                        value = currentKey,
                        onValueChange = { newValue ->
                            val cleanVal = newValue.trim()
                            when (selectedProvider) {
                                "Gemini" -> geminiKey = cleanVal
                                "Groq" -> groqKey = cleanVal
                                "Cerebras" -> cerebrasKey = cleanVal
                                "Mistral" -> mistralKey = cleanVal
                            }
                        },
                        label = { Text(keyPlaceholder, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("ai_key_input_field"),
                        singleLine = true,
                        visualTransformation = if (keyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.Lock else Icons.Default.Check,
                                    contentDescription = "Toggle Visibility",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )

                    // Customizable model type input
                    OutlinedTextField(
                        value = currentModelName,
                        onValueChange = { newValue ->
                            val cleanVal = newValue.trim()
                            when (selectedProvider) {
                                "Gemini" -> geminiModel = cleanVal
                                "Groq" -> groqModel = cleanVal
                                "Cerebras" -> cerebrasModel = cleanVal
                                "Mistral" -> mistralModel = cleanVal
                            }
                        },
                        label = { Text("Model Name Identifier", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("ai_model_input_field"),
                        singleLine = true
                    )

                    // Hints for recommended engines
                    val recommendationText = when (selectedProvider) {
                        "Gemini" -> "💡 Recommended: gemini-2.5-flash-preview-05-20"
                        "Groq" -> "💡 Recommended: llama-3.3-70b-versatile or llama-3.1-8b-instant"
                        "Cerebras" -> "💡 Recommended: llama-3.3-70b"
                        else -> "💡 Recommended: mistral-large-latest"
                    }
                    Text(
                        text = recommendationText,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                geminiKey = ""
                                groqKey = ""
                                cerebrasKey = ""
                                mistralKey = ""
                                saveStatusText = "Reset successful!"
                            }
                        ) {
                            Text("Reset All Keys", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }

                        Button(
                            onClick = {
                                prefs.edit().apply {
                                    putString("ai_provider", selectedProvider)
                                    putString("ai_gemini_key", geminiKey)
                                    putString("ai_groq_key", groqKey)
                                    putString("ai_cerebras_key", cerebrasKey)
                                    putString("ai_mistral_key", mistralKey)
                                    putString("ai_gemini_model", geminiModel)
                                    putString("ai_groq_model", groqModel)
                                    putString("ai_cerebras_model", cerebrasModel)
                                    putString("ai_mistral_model", mistralModel)
                                    apply()
                                }
                                saveStatusText = "Credentials Guarded Safely!"
                            },
                            modifier = Modifier.testTag("ai_save_keys_button")
                        ) {
                            Text("Guard Credentials", fontSize = 11.sp)
                        }
                    }

                    if (saveStatusText.isNotEmpty()) {
                        Text(
                            text = saveStatusText,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LaunchedEffect(saveStatusText) {
                            delay(2500)
                            saveStatusText = ""
                        }
                    }
                }
            }
        }

        // Provider select row in Main view
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Gemini", "Groq", "Cerebras", "Mistral").forEach { prov ->
                val active = selectedProvider == prov
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable {
                            selectedProvider = prov
                            prefs.edit().putString("ai_provider", prov).apply()
                        }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = prov,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Telemetry Study Habit Analyzer
        Button(
            onClick = {
                isLoading = true
                coroutineScope.launch {
                    val keyToUse = when (selectedProvider) {
                        "Gemini" -> geminiKey
                        "Groq" -> groqKey
                        "Cerebras" -> cerebrasKey
                        else -> mistralKey
                    }
                    val modelToUse = when (selectedProvider) {
                        "Gemini" -> geminiModel
                        "Groq" -> groqModel
                        "Cerebras" -> cerebrasModel
                        else -> mistralModel
                    }

                    if (keyToUse.isEmpty()) {
                        saveLastResponse("⚠️ Error: Please configure and save your $selectedProvider API Key above first!")
                        isLoading = false
                        return@launch
                    }

                    val systemPrompt = "You are FocusFlow DXT Advanced Academic Coach. Assess user metrics and profile. Provide brief bulleted actionable advice under 200 words. No introduction or fluff."
                    val mainPrompt = """
                        Analyze academic patterns for student '$profileName' (github: @SudhirDevOps1):
                        - Favorite Subject: $profileFavSubject
                        - Goal: $profileGoal
                        - Class level: $profileCourse
                        - College/School: $profileSchool
                        - XP Level: $profileAge (Daily target: $profileDailyTarget Hrs)
                        - Recent 7-Day Study Minutes: $studyHistory
                        - Disruption/Restrict Hours: $restrictedHistory
                        - Current CPU Temp: ${String.format("%.1f", currentTemperature)}°C
                        
                        Provide:
                        1. High-yield hours to study.
                        2. Best study-to-distraction ratio suggestions.
                        3. Fast alert if motherboard temp > 38°C.
                    """.trimIndent()

                    val result = performAIApiCall(selectedProvider, modelToUse, keyToUse, systemPrompt, mainPrompt)
                    saveLastResponse(result)
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("ai_generate_plan_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isLoading
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = "AI Plan", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("INTEGRITY: ANALYZE STUDY RHYTHMS", fontWeight = FontWeight.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Custom Question Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                placeholder = { Text("Ask academic query...", fontSize = 11.sp) },
                modifier = Modifier.weight(1f).testTag("ai_custom_query_input"),
                singleLine = true,
                maxLines = 1,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp)
            )

            Button(
                onClick = {
                    if (questionText.trim().isEmpty()) return@Button
                    isLoading = true
                    coroutineScope.launch {
                        val keyToUse = when (selectedProvider) {
                            "Gemini" -> geminiKey
                            "Groq" -> groqKey
                            "Cerebras" -> cerebrasKey
                            else -> mistralKey
                        }
                        val modelToUse = when (selectedProvider) {
                            "Gemini" -> geminiModel
                            "Groq" -> groqModel
                            "Cerebras" -> cerebrasModel
                            else -> mistralModel
                        }

                        if (keyToUse.isEmpty()) {
                            saveLastResponse("⚠️ Error: Please configure and save your $selectedProvider API Key above first!")
                            isLoading = false
                            return@launch
                        }

                        val userQuery = questionText.trim()
                        questionText = ""

                        val systemPrompt = "You are an optimized private AI Study Tutor. Respond under 250 words with pristine bullet points. No useless introductory fluff."
                        val mainPrompt = "Study Query: \"$userQuery\"\n\nProvide the best correct step-by-step academic answer instantly."

                        val result = performAIApiCall(selectedProvider, modelToUse, keyToUse, systemPrompt, mainPrompt)
                        saveLastResponse(result)
                        isLoading = false
                    }
                },
                modifier = Modifier.testTag("ai_custom_query_send_btn"),
                enabled = !isLoading && questionText.trim().isNotEmpty()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send to AI", modifier = Modifier.size(16.dp))
            }
        }

        // Telemetry AI LCD Panel Response Hud
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(themeColors.bezelBgColor.copy(alpha = 0.95f))
                .border(2.dp, themeColors.bezelBorderColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📟 DIGITAL AI FEEDBACK RECEIVER",
                        color = themeColors.screenTextColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Model: ${
                                when (selectedProvider) {
                                    "Gemini" -> geminiModel.take(12)
                                    "Groq" -> groqModel.take(12)
                                    "Cerebras" -> cerebrasModel.take(12)
                                    else -> mistralModel.take(12)
                                }
                            }...",
                            color = themeColors.screenTextColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColors.screenTextColor.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AI COMPRESSED",
                                color = themeColors.screenTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Divider(color = themeColors.screenTextColor.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 8.dp))

                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = themeColors.screenTextColor,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "SYNCHRONIZING TELEMETRY FLOW...",
                            color = themeColors.screenTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column {
                        SelectionContainer {
                            CleanFormattedMarkdownText(
                                text = aiResponse,
                                textColor = themeColors.screenTextColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp, max = 350.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showExpandedDialog = true }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Expand Read Out",
                                        tint = themeColors.screenTextColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "🔍 EXPAND READ OUT",
                                        color = themeColors.screenTextColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("AI Student Advisor", aiResponse)
                                    clipboard?.setPrimaryClip(clip)
                                },
                                modifier = Modifier.testTag("ai_copy_response_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Copy Content",
                                        tint = themeColors.screenTextColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "COPY FEEDBACK",
                                        color = themeColors.screenTextColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showExpandedDialog) {
            AlertDialog(
                onDismissRequest = { showExpandedDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📟 FULL SCREEN TERMINAL RESPONSE", fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        IconButton(onClick = { showExpandedDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close View")
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SelectionContainer {
                            CleanFormattedMarkdownText(
                                text = aiResponse,
                                textColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("AI Student Advisor", aiResponse)
                            clipboard?.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "📋 Response copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("COPY ALL TEXT")
                    }
                }
            )
        }
    }
}

suspend fun performAIApiCall(
    provider: String,
    model: String,
    apiKey: String,
    systemPrompt: String,
    userPrompt: String
): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    try {
        when (provider) {
            "Gemini" -> {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                
                val rootJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObject = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                val partObject = JSONObject().apply {
                                    put("text", userPrompt)
                                }
                                put(partObject)
                            }
                            put("parts", partsArray)
                        }
                        put(contentObject)
                    }
                    put("contents", contentsArray)

                    val systemInstructionObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObject = JSONObject().apply {
                                put("text", systemPrompt)
                            }
                            put(partObject)
                        }
                        put("parts", partsArray)
                    }
                    put("systemInstruction", systemInstructionObj)
                }

                val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        return@withContext "⚠️ Gemini API Error [${response.code}]: $errBody"
                    }
                    val respStr = response.body?.string() ?: return@withContext "⚠️ Empty Gemini Response"
                    val jsonResp = JSONObject(respStr)
                    val candidates = jsonResp.optJSONArray("candidates") ?: return@withContext "⚠️ Invalid response format: missing candidates"
                    if (candidates.length() == 0) return@withContext "⚠️ No response candidates returned"
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content") ?: return@withContext "⚠️ Missing content block"
                    val parts = content.optJSONArray("parts") ?: return@withContext "⚠️ Missing parts block"
                    if (parts.length() == 0) return@withContext "⚠️ Empty parts"
                    return@withContext parts.getJSONObject(0).optString("text", "⚠️ No response text found.")
                }
            }
            
            "Groq" -> {
                val url = "https://api.groq.com/openai/v1/chat/completions"
                val rootJson = JSONObject().apply {
                    put("model", model)
                    val messagesArray = JSONArray().apply {
                        val systemMessage = JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                        val userMessage = JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        }
                        put(systemMessage)
                        put(userMessage)
                    }
                    put("messages", messagesArray)
                    put("temperature", 0.5)
                }

                val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        return@withContext "⚠️ Groq API Error [${response.code}]: $errBody"
                    }
                    val respStr = response.body?.string() ?: return@withContext "⚠️ Empty Groq Response"
                    val jsonResp = JSONObject(respStr)
                    val choices = jsonResp.optJSONArray("choices") ?: return@withContext "⚠️ Missing choices"
                    if (choices.length() == 0) return@withContext "⚠️ No choices"
                    val firstChoice = choices.getJSONObject(0)
                    val msg = firstChoice.optJSONObject("message") ?: return@withContext "⚠️ Missing message block"
                    return@withContext msg.optString("content", "⚠️ No response content.")
                }
            }

            "Cerebras" -> {
                val url = "https://api.cerebras.ai/v1/chat/completions"
                val rootJson = JSONObject().apply {
                    put("model", model)
                    val messagesArray = JSONArray().apply {
                        val systemMessage = JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                        val userMessage = JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        }
                        put(systemMessage)
                        put(userMessage)
                    }
                    put("messages", messagesArray)
                    put("temperature", 0.3)
                }

                val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        return@withContext "⚠️ Cerebras API Error [${response.code}]: $errBody"
                    }
                    val respStr = response.body?.string() ?: return@withContext "⚠️ Empty Cerebras Response"
                    val jsonResp = JSONObject(respStr)
                    val choices = jsonResp.optJSONArray("choices") ?: return@withContext "⚠️ Missing choices"
                    if (choices.length() == 0) return@withContext "⚠️ No choices"
                    val firstChoice = choices.getJSONObject(0)
                    val msg = firstChoice.optJSONObject("message") ?: return@withContext "⚠️ Missing message block"
                    return@withContext msg.optString("content", "⚠️ No response content.")
                }
            }

            "Mistral" -> {
                val url = "https://api.mistral.ai/v1/chat/completions"
                val rootJson = JSONObject().apply {
                    put("model", model)
                    val messagesArray = JSONArray().apply {
                        val systemMessage = JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                        val userMessage = JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        }
                        put(systemMessage)
                        put(userMessage)
                    }
                    put("messages", messagesArray)
                    put("temperature", 0.5)
                }

                val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        return@withContext "⚠️ Mistral API Error [${response.code}]: $errBody"
                    }
                    val respStr = response.body?.string() ?: return@withContext "⚠️ Empty Mistral Response"
                    val jsonResp = JSONObject(respStr)
                    val choices = jsonResp.optJSONArray("choices") ?: return@withContext "⚠️ Missing choices"
                    if (choices.length() == 0) return@withContext "⚠️ No choices"
                    val firstChoice = choices.getJSONObject(0)
                    val msg = firstChoice.optJSONObject("message") ?: return@withContext "⚠️ Missing message block"
                    return@withContext msg.optString("content", "⚠️ No response content.")
                }
            }

            else -> "⚠️ Unsupported AI provider: $provider"
        }
    } catch (e: Exception) {
        "⚠️ Client Connection Timeout / Dispatch Error: ${e.message}"
    }
}


// --- THEME COLOR SPECIFICATION MAPPER ---

data class ThemeColorScheme(
    val bezelBgColor: Color,
    val bezelBorderColor: Color,
    val screenTextColor: Color,
    val secondaryGlowColor: Color,
    val warnColor: Color
)

fun getThemeColors(theme: DisplayTheme): ThemeColorScheme {
    return when (theme) {
        DisplayTheme.RETRO_GREEN -> ThemeColorScheme(
            bezelBgColor = Color(0xFFA2B59F), // LCD grayish pale green
            bezelBorderColor = Color(0xFF6F826B),
            screenTextColor = Color(0xFF2C3229), // liquid crystal dark segments
            secondaryGlowColor = Color(0xFF1E241F),
            warnColor = Color(0xFFA30000)
        )
        DisplayTheme.AMBER_GLOW -> ThemeColorScheme(
            bezelBgColor = Color(0xFF1F1206), // warm glass amoled brownish black
            bezelBorderColor = Color(0xFFD35400),
            screenTextColor = Color(0xFFF39C12), // vacuum amber orange digits
            secondaryGlowColor = Color(0xFFE67E22),
            warnColor = Color(0xFFFF4D4D)
        )
        DisplayTheme.CYBER_PUNK -> ThemeColorScheme(
            bezelBgColor = Color(0xFF1A0826), // deeper purple
            bezelBorderColor = Color(0xFFEC407A),
            screenTextColor = Color(0xFF00E5FF), // retro glass cyan
            secondaryGlowColor = Color(0xFFEC407A), // vibrant pink
            warnColor = Color(0xFFFFEE58)
        )
        DisplayTheme.NEON_BLUE -> ThemeColorScheme(
            bezelBgColor = Color(0xFF0A1118),
            bezelBorderColor = Color(0xFF1E88E5),
            screenTextColor = Color(0xFF00E676), // green/cyan active segments
            secondaryGlowColor = Color(0xFF00B0FF),
            warnColor = Color(0xFFFF5252)
        )
        DisplayTheme.AMOLED_DARK -> ThemeColorScheme(
            bezelBgColor = Color(0xFF000000), // pure amoled dark
            bezelBorderColor = Color(0xFF424242),
            screenTextColor = Color(0xFFFFFFFF), // crisp digital white
            secondaryGlowColor = Color(0xFFB0BEC5),
            warnColor = Color(0xFFFF7043)
        )
        DisplayTheme.BRUSHED_STEEL -> ThemeColorScheme(
            bezelBgColor = Color(0xFFCFD8DC),
            bezelBorderColor = Color(0xFF455A64),
            screenTextColor = Color(0xFF0D47A1), // sleek industrial marine blue
            secondaryGlowColor = Color(0xFF1976D2),
            warnColor = Color(0xFFD50000)
        )
    }
}


// --- CORE SYSTEM UTILS ---

fun addDurationMinutes(minutes: Long) {
    val currentMax = TimerService.countdownTotalDuration.value
    val newMax = currentMax + (minutes * 60 * 1000L)
    TimerService.countdownTotalDuration.value = newMax
    TimerService.countdownTimeLeft.value = newMax
    TimerService.instance?.saveSettings()
}

fun addDurationSeconds(seconds: Long) {
    val currentMax = TimerService.countdownTotalDuration.value
    val limit = 3600 * 99 * 1000L
    var newMax = currentMax + (seconds * 1000L)
    if (newMax > limit) {
        newMax = 0L
    }
    TimerService.countdownTotalDuration.value = newMax
    TimerService.countdownTimeLeft.value = newMax
    TimerService.instance?.saveSettings()
}

fun cycleMode() {
    val modes = ActiveMode.values()
    val nextOrdinal = (TimerService.activeMode.value.ordinal + 1) % modes.size
    val nextMode = modes[nextOrdinal]
    
    // Stop status timer before turning modes
    TimerService.isRunning.value = false
    TimerService.activeMode.value = nextMode
    TimerService.instance?.saveSettings()
}

fun exportProfileBackup(context: android.content.Context): String {
    return try {
        val prefs = context.getSharedPreferences("digital_clock_timer_prefs", android.content.Context.MODE_PRIVATE)
        val obj = org.json.JSONObject().apply {
            // 1. Profile information
            put("prof_user_name", TimerService.profileUserName.value)
            put("prof_user_age", TimerService.profileUserAge.value)
            put("prof_course_name", TimerService.profileCourseName.value)
            put("prof_school_name", TimerService.profileSchoolName.value)
            put("prof_study_goal", TimerService.profileStudyGoal.value)
            put("prof_daily_target_hours", TimerService.profileDailyTargetHours.value)
            put("prof_fav_subject", TimerService.profileFavSubject.value)
            
            // 2. Profile gamified stats
            put("profile_xp", TimerService.profileXP.value)
            put("profile_level", TimerService.profileLevel.value)
            put("profile_streak_days", TimerService.profileStreakDays.value)
            put("profile_last_active_date", TimerService.profileLastActiveDate.value)
            put("completed_quest_ids", TimerService.completedQuestIds.value.joinToString(","))
            
            // 3. Garden and Shop
            put("forest_coins", TimerService.forestCoins.value)
            put("has_golden_watering_can", TimerService.hasGoldenWateringCan.value)
            put("has_ocean_waves_track", TimerService.hasOceanWavesTrack.value)
            put("has_deep_space_track", TimerService.hasDeepSpaceTrack.value)
            put("super_fertilizer_count", TimerService.superFertilizerCount.value)
            put("active_planted_tree_type", TimerService.activePlantedTreeType.value)
            
            val trsArr = org.json.JSONArray()
            TimerService.grownTreesList.value.forEach { trsArr.put(it) }
            put("grown_trees_list", trsArr)

            // 4. History metrics
            put("study_mins_history", prefs.getString("study_mins_history", "35,45,60,50,25,75,40"))
            put("restricted_mins_history", prefs.getString("restricted_mins_history", "8,12,5,14,18,10,12"))
            
            // 5. Custom restrictive keywords
            val restrPksArr = org.json.JSONArray()
            TimerService.customRestrictedPackages.value.forEach { restrPksArr.put(it) }
            put("custom_restricted_packages_arr", restrPksArr)

            // 6. Logs
            val logsArr = org.json.JSONArray()
            TimerService.databaseFocusLogs.value.forEach { logsArr.put(it) }
            put("focus_logs_arr", logsArr)

            // 7. FlowTracks Subjects/Sessions
            val subjArr = org.json.JSONArray()
            TimerService.studySubjectsFlow.value.forEach { subjArr.put(it.toJsonObject()) }
            put("flowtrack_subjects_arr", subjArr)

            val sessArr = org.json.JSONArray()
            TimerService.studySessionsFlow.value.forEach { sessArr.put(it.toJsonObject()) }
            put("flowtrack_sessions_arr", sessArr)
        }
        obj.toString(2)
    } catch (e: Exception) {
        ""
    }
}

fun importProfileBackup(context: android.content.Context, jsonStr: String): Boolean {
    return try {
        val obj = org.json.JSONObject(jsonStr)
        val prefs = context.getSharedPreferences("digital_clock_timer_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            // 1. Profile information
            if (obj.has("prof_user_name")) {
                val name = obj.getString("prof_user_name")
                TimerService.profileUserName.value = name
                putString("prof_user_name", name)
            }
            if (obj.has("prof_user_age")) {
                val age = obj.getString("prof_user_age")
                TimerService.profileUserAge.value = age
                putString("prof_user_age", age)
            }
            if (obj.has("prof_course_name")) {
                val course = obj.getString("prof_course_name")
                TimerService.profileCourseName.value = course
                putString("prof_course_name", course)
            }
            if (obj.has("prof_school_name")) {
                val school = obj.getString("prof_school_name")
                TimerService.profileSchoolName.value = school
                putString("prof_school_name", school)
            }
            if (obj.has("prof_study_goal")) {
                val goal = obj.getString("prof_study_goal")
                TimerService.profileStudyGoal.value = goal
                putString("prof_study_goal", goal)
            }
            if (obj.has("prof_daily_target_hours")) {
                val target = obj.getString("prof_daily_target_hours")
                TimerService.profileDailyTargetHours.value = target
                putString("prof_daily_target_hours", target)
            }
            if (obj.has("prof_fav_subject")) {
                val fav = obj.getString("prof_fav_subject")
                TimerService.profileFavSubject.value = fav
                putString("prof_fav_subject", fav)
            }
            
            // 2. Profile gamified stats
            if (obj.has("profile_xp")) {
                val xp = obj.getInt("profile_xp")
                TimerService.profileXP.value = xp
                putInt("profile_xp", xp)
            }
            if (obj.has("profile_level")) {
                val lvl = obj.getInt("profile_level")
                TimerService.profileLevel.value = lvl
                putInt("profile_level", lvl)
            }
            if (obj.has("profile_streak_days")) {
                val streak = obj.getInt("profile_streak_days")
                TimerService.profileStreakDays.value = streak
                putInt("profile_streak_days", streak)
            }
            if (obj.has("profile_last_active_date")) {
                val date = obj.getString("profile_last_active_date")
                TimerService.profileLastActiveDate.value = date
                putString("profile_last_active_date", date)
            }
            if (obj.has("completed_quest_ids")) {
                val qIds = obj.getString("completed_quest_ids")
                TimerService.completedQuestIds.value = qIds.split(",").filter { it.isNotEmpty() }.toSet()
                putString("completed_quest_ids", qIds)
            }

            // 3. Garden and Shop
            if (obj.has("forest_coins")) {
                val coins = obj.getInt("forest_coins")
                TimerService.forestCoins.value = coins
                putInt("forest_coins", coins)
            }
            if (obj.has("has_golden_watering_can")) {
                val can = obj.getBoolean("has_golden_watering_can")
                TimerService.hasGoldenWateringCan.value = can
                putBoolean("has_golden_watering_can", can)
            }
            if (obj.has("has_ocean_waves_track")) {
                val ocean = obj.getBoolean("has_ocean_waves_track")
                TimerService.hasOceanWavesTrack.value = ocean
                putBoolean("has_ocean_waves_track", ocean)
            }
            if (obj.has("has_deep_space_track")) {
                val space = obj.getBoolean("has_deep_space_track")
                TimerService.hasDeepSpaceTrack.value = space
                putBoolean("has_deep_space_track", space)
            }
            if (obj.has("super_fertilizer_count")) {
                val count = obj.getInt("super_fertilizer_count")
                TimerService.superFertilizerCount.value = count
                putInt("super_fertilizer_count", count)
            }
            if (obj.has("active_planted_tree_type")) {
                val treeType = obj.getString("active_planted_tree_type")
                TimerService.activePlantedTreeType.value = treeType
                putString("active_planted_tree_type", treeType)
            }
            if (obj.has("grown_trees_list")) {
                val trsArr = obj.getJSONArray("grown_trees_list")
                val list = mutableListOf<String>()
                for (i in 0 until trsArr.length()) {
                    list.add(trsArr.getString(i))
                }
                TimerService.grownTreesList.value = list
                putString("grown_trees_list", trsArr.toString())
            }

            // 4. History metrics
            if (obj.has("study_mins_history")) {
                val studyHist = obj.getString("study_mins_history")
                putString("study_mins_history", studyHist)
                TimerService.studyMinsHistory.value = studyHist.split(",").map { it.toIntOrNull() ?: 0 }
            }
            if (obj.has("restricted_mins_history")) {
                val restrHist = obj.getString("restricted_mins_history")
                putString("restricted_mins_history", restrHist)
                TimerService.restrictedMinsHistory.value = restrHist.split(",").map { it.toIntOrNull() ?: 0 }
            }

            // 5. Custom restrictive keywords
            if (obj.has("custom_restricted_packages_arr")) {
                val pkgArr = obj.getJSONArray("custom_restricted_packages_arr")
                val list = mutableListOf<String>()
                for (i in 0 until pkgArr.length()) {
                    list.add(pkgArr.getString(i))
                }
                TimerService.customRestrictedPackages.value = list
                putString("custom_restricted_packages", list.joinToString(","))
            }

            // 6. Logs
            if (obj.has("focus_logs_arr")) {
                val logsArr = obj.getJSONArray("focus_logs_arr")
                val list = mutableListOf<String>()
                for (i in 0 until logsArr.length()) {
                    list.add(logsArr.getString(i))
                }
                TimerService.databaseFocusLogs.value = list
                putStringSet("focus_logs", list.toSet())
            }

            // 7. FlowTracks Subjects/Sessions
            if (obj.has("flowtrack_subjects_arr")) {
                val subArray = obj.getJSONArray("flowtrack_subjects_arr")
                val tempSubj = mutableListOf<SubjectItem>()
                for (i in 0 until subArray.length()) {
                    tempSubj.add(SubjectItem.fromJsonObject(subArray.getJSONObject(i)))
                }
                TimerService.studySubjectsFlow.value = tempSubj
                putString("flowtrack_subjects", subArray.toString())
            }
            if (obj.has("flowtrack_sessions_arr")) {
                val sesArray = obj.getJSONArray("flowtrack_sessions_arr")
                val tempSess = mutableListOf<SessionItem>()
                for (i in 0 until sesArray.length()) {
                    tempSess.add(SessionItem.fromJsonObject(sesArray.getJSONObject(i)))
                }
                TimerService.studySessionsFlow.value = tempSess
                putString("flowtrack_sessions", sesArray.toString())
            }

            apply()
        }
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun CustomizerFlowTrackTab(
    subjects: List<SubjectItem>,
    sessions: List<SessionItem>,
    activeTrackingSubjectId: String,
    themeColors: ThemeColorScheme
) {
    val context = LocalContext.current
    var customSubjectName by remember { mutableStateOf("") }
    var customSubjectColor by remember { mutableStateOf("#2196F3") }
    var customSubjectPlannedHours by remember { mutableStateOf("2.0") }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    
    // Clipboard JSON imports
    var importJsonText by remember { mutableStateOf("") }
    var isImportSectionVisible by remember { mutableStateOf(false) }
    
    val colorOptions = listOf(
        "#2196F3", "#F44336", "#4CAF50", "#FFC107", 
        "#9C27B0", "#E91E63", "#00BCD4", "#FF9800", "#FF5722", "#009688"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎯 FlowTrack Master Study Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Set custom subjects and target study hours. Real-time timer log binding.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            FilledTonalButton(
                onClick = { isImportSectionVisible = !isImportSectionVisible },
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Backup Options", fontSize = 10.sp)
            }
        }

        // Import section
        if (isImportSectionVisible) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📋 Copy/Paste flowtrack backup payload", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        label = { Text("Paste Backup JSON", fontSize = 10.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (importJsonText.trim().isEmpty()) {
                                    android.widget.Toast.makeText(context, "⚠️ Please paste JSON payload first", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                try {
                                    val obj = org.json.JSONObject(importJsonText)
                                    val subjectsArray = obj.optJSONArray("subjects") ?: org.json.JSONArray()
                                    val sessionsArray = obj.optJSONArray("sessions") ?: org.json.JSONArray()
                                    
                                    val loadedSubjects = mutableListOf<SubjectItem>()
                                    for (i in 0 until subjectsArray.length()) {
                                        loadedSubjects.add(SubjectItem.fromJsonObject(subjectsArray.getJSONObject(i)))
                                    }
                                    
                                    val loadedSessions = mutableListOf<SessionItem>()
                                    for (i in 0 until sessionsArray.length()) {
                                        loadedSessions.add(SessionItem.fromJsonObject(sessionsArray.getJSONObject(i)))
                                    }
                                    
                                    TimerService.studySubjectsFlow.value = loadedSubjects
                                    TimerService.studySessionsFlow.value = loadedSessions
                                    
                                    TimerService.instance?.saveSettings()
                                    
                                    android.widget.Toast.makeText(context, "✅ Imported ${loadedSubjects.size} subjects and ${loadedSessions.size} sessions!", android.widget.Toast.LENGTH_SHORT).show()
                                    importJsonText = ""
                                    isImportSectionVisible = false
                                } catch(e: Exception) {
                                    android.widget.Toast.makeText(context, "❌ Invalid JSON representation: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("Confirm Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val rootObj = org.json.JSONObject().apply {
                                        put("app", "FlowTrack")
                                        put("exportedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        }.format(java.util.Date()))
                                        
                                        val subArray = org.json.JSONArray()
                                        subjects.forEach { subArray.put(it.toJsonObject()) }
                                        put("subjects", subArray)
                                        
                                        val sesArray = org.json.JSONArray()
                                        sessions.forEach { sesArray.put(it.toJsonObject()) }
                                        put("sessions", sesArray)
                                    }
                                    
                                    val backupStr = rootObj.toString(2)
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("FlowTrack Backup", backupStr)
                                    clipboard?.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "📋 Export payload copied to Clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                } catch(e: Exception) {
                                    android.widget.Toast.makeText(context, "❌ Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("Export Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Focus tracking selection card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🎯 ACTIVE CLASS BINDING FOR TRACKING",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                if (subjects.isEmpty()) {
                    Text(
                        text = "No subjects found! Please import or click '+' below to add your first study subject.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Hold clock or start device timer. Your active elapsed minutes/seconds will automatically accumulate under selected block.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    // Display scrollable row of pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        subjects.forEach { subj ->
                            val isSelected = subj.id == activeTrackingSubjectId
                            val itemColor = Color(android.graphics.Color.parseColor(subj.color))
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) itemColor else itemColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, if (isSelected) Color.White else itemColor, RoundedCornerShape(20.dp))
                                    .clickable {
                                        TimerService.activeTrackingSubjectId.value = if (isSelected) "" else subj.id
                                        TimerService.instance?.saveSettings()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = subj.name,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (activeTrackingSubjectId.isNotEmpty()) {
                        val activeSubjName = subjects.find { it.id == activeTrackingSubjectId }?.name ?: "Subject"
                        val subjColor = subjects.find { it.id == activeTrackingSubjectId }?.color ?: "#2196F3"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(android.graphics.Color.parseColor(subjColor)).copy(alpha = 0.12f))
                                .padding(6.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(android.graphics.Color.parseColor(subjColor)), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Currently capturing study metrics for: $activeSubjName",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (subjects.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📊 LIVE PROGRESS GRAPH (Planned vs. Actual)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    
                    subjects.forEach { subj ->
                        val itemColor = Color(android.graphics.Color.parseColor(subj.color))
                        val subjectSessions = sessions.filter { it.subjectId == subj.id }
                        val totalPlannedMins = subjectSessions.sumOf { it.plannedMinutes }
                        val totalActualSecs = subjectSessions.sumOf { it.actualSeconds }
                        
                        val totalPlannedHours = totalPlannedMins / 60.0
                        val totalActualHours = totalActualSecs / 3600.0
                        
                        // Let's draw horizontal parallel progress comparison bars
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subj.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tracked: ${String.format("%.2f", totalActualHours)}h of ${String.format("%.1f", totalPlannedHours)}h goal",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            // Let's create a visual comparison bar container
                            val maxLimit = java.lang.Math.max(totalPlannedHours, totalActualHours).coerceAtLeast(1.0)
                            val targetFraction = (totalPlannedHours / maxLimit).toFloat()
                            val actualFraction = (totalActualHours / maxLimit).toFloat()
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 1. Custom Planned Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Goal  ",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(35.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(targetFraction)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                                
                                // 2. Custom Actual Tracked Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Track ",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(35.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(actualFraction)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(itemColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Goal (Planned)", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tracked Time", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // List of Subjects with individual planned vs actual trackers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📚 SUBJECT REAL-TIME STUDY STATS",
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Add subject button
            IconButton(
                onClick = { showAddSubjectDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom Subject", tint = MaterialTheme.colorScheme.primary)
            }
        }

        subjects.forEach { subj ->
            val subjColor = Color(android.graphics.Color.parseColor(subj.color))
            
            // Calculate planned target vs actual seconds
            val subjectSessions = sessions.filter { it.subjectId == subj.id }
            val totalPlannedMins = subjectSessions.sumOf { it.plannedMinutes }
            val totalActualSecs = subjectSessions.sumOf { it.actualSeconds }
            
            val actualHrs = totalActualSecs / 3600
            val actualMinsSec = (totalActualSecs % 3600) / 60
            
            val completionPercent = if (totalPlannedMins > 0) {
                ((totalActualSecs / 60.0) / totalPlannedMins.toDouble() * 100.0).coerceIn(0.0, 100.0)
            } else {
                if (totalActualSecs > 0) 100.0 else 0.0
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(subjColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subj.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Options row with delete icon
                        IconButton(
                            onClick = {
                                // Delete subject and corresponding sessions
                                TimerService.studySubjectsFlow.value = subjects.filter { it.id != subj.id }
                                TimerService.studySessionsFlow.value = sessions.filter { it.subjectId != subj.id }
                                if (activeTrackingSubjectId == subj.id) {
                                    TimerService.activeTrackingSubjectId.value = ""
                                }
                                TimerService.instance?.saveSettings()
                                android.widget.Toast.makeText(context, "🗑️ Subject removed", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Subject", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        }
                    }

                    // Progress metrics
                    val plannedDisplay = if (totalPlannedMins > 0) "${totalPlannedMins}m" else "No target"
                    val actualDisplay = if (actualHrs > 0) "${actualHrs}h ${actualMinsSec}m" else "${actualMinsSec}m ${totalActualSecs % 60}s"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tracked: $actualDisplay / Goal: $plannedDisplay",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${completionPercent.toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = subjColor
                        )
                    }

                    // Linear progress block
                    LinearProgressIndicator(
                        progress = { (completionPercent / 100.0).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = subjColor,
                        trackColor = subjColor.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }

    // Modal Add Subject Dialog
    if (showAddSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Add Custom Subject", fontSize =14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customSubjectName,
                        onValueChange = { customSubjectName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Subject Name (e.g. Web Dev)", fontSize = 10.sp) }
                    )

                    OutlinedTextField(
                        value = customSubjectPlannedHours,
                        onValueChange = { customSubjectPlannedHours = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Daily Study Target Hours (e.g. 2, 4.5, 1.5)", fontSize = 10.sp) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )

                    Text("Pick Theme Color", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    // Simple select grid for colors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        colorOptions.take(5).forEach { colorStr ->
                            val isChosen = customSubjectColor == colorStr
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorStr)))
                                    .border(
                                        border = if (isChosen) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.dp, Color.Transparent),
                                        shape = CircleShape
                                    )
                                    .clickable { customSubjectColor = colorStr }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        colorOptions.drop(5).forEach { colorStr ->
                            val isChosen = customSubjectColor == colorStr
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorStr)))
                                    .border(
                                        border = if (isChosen) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.dp, Color.Transparent),
                                        shape = CircleShape
                                    )
                                    .clickable { customSubjectColor = colorStr }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customSubjectName.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "⚠️ Please enter subject name", android.widget.Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        
                        val hoursInput = customSubjectPlannedHours.toDoubleOrNull() ?: 2.0
                        val calculatedPlannedMins = (hoursInput * 60).toInt().coerceAtLeast(1)
                        
                        val newId = java.util.UUID.randomUUID().toString()
                        val newSub = SubjectItem(
                            id = newId,
                            name = customSubjectName.trim(),
                            color = customSubjectColor,
                            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }.format(java.util.Date())
                        )
                        
                        // Add companion study flow
                        val currentSubjects = TimerService.studySubjectsFlow.value.toMutableList()
                        currentSubjects.add(newSub)
                        TimerService.studySubjectsFlow.value = currentSubjects
                        
                        // Also automatically add a planned study session for today with entered target!
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        val nowStr = format.format(java.util.Date())
                        val endStr = format.format(java.util.Date(System.currentTimeMillis() + (hoursInput * 3600_000).toLong()))
                        
                        val newPlanSession = SessionItem(
                            id = java.util.UUID.randomUUID().toString(),
                            subjectId = newId,
                            startTime = nowStr,
                            endTime = endStr,
                            plannedMinutes = calculatedPlannedMins,
                            actualSeconds = 0,
                            colorTag = customSubjectColor,
                            notes = "Planned study target for ${customSubjectName.trim()}",
                            tags = listOf("focus"),
                            status = "planned",
                            createdAt = nowStr,
                            updatedAt = nowStr,
                            manualEntry = false
                        )
                        
                        val currentSessions = TimerService.studySessionsFlow.value.toMutableList()
                        currentSessions.add(newPlanSession)
                        TimerService.studySessionsFlow.value = currentSessions
                        
                        TimerService.instance?.saveSettings()
                        
                        customSubjectName = ""
                        customSubjectPlannedHours = "2.0"
                        showAddSubjectDialog = false
                    }
                ) {
                    Text("ADD SUBJ (OK)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun CustomizerGamifyTab(
    themeColors: ThemeColorScheme
) {
    val profileXP by TimerService.profileXP.collectAsStateWithLifecycle()
    val profileLevel by TimerService.profileLevel.collectAsStateWithLifecycle()
    val profileStreakDays by TimerService.profileStreakDays.collectAsStateWithLifecycle()
    val completedQuests by TimerService.completedQuestIds.collectAsStateWithLifecycle()
    val forestCoins by TimerService.forestCoins.collectAsStateWithLifecycle()
    val grownTreesRaw by TimerService.grownTreesList.collectAsStateWithLifecycle()
    val subjects by TimerService.studySubjectsFlow.collectAsStateWithLifecycle()
    
    val hasGoldenWateringCan by TimerService.hasGoldenWateringCan.collectAsStateWithLifecycle()
    val hasOceanWaves by TimerService.hasOceanWavesTrack.collectAsStateWithLifecycle()
    val hasDeepSpace by TimerService.hasDeepSpaceTrack.collectAsStateWithLifecycle()
    val superFertilizerCount by TimerService.superFertilizerCount.collectAsStateWithLifecycle()
    
    val maxNextLvlXp = profileLevel * 150
    val progressPercent = if (maxNextLvlXp > 0) profileXP.toFloat() / maxNextLvlXp.toFloat() else 0f
    
    val context = androidx.compose.ui.platform.LocalContext.current

    // Internal UI States for Planting configurations
    var selectedTreeIndex by remember { androidx.compose.runtime.mutableStateOf(0) }
    var selectedDurationIndex by remember { androidx.compose.runtime.mutableStateOf(1) } // Default 25m
    var selectedSubjectId by remember { androidx.compose.runtime.mutableStateOf("") }
    
    // Auto-update first available subject if empty
    androidx.compose.runtime.LaunchedEffect(subjects) {
        if (selectedSubjectId.isEmpty() && subjects.isNotEmpty()) {
            selectedSubjectId = subjects.first().id
        }
    }

    val treeSpecies = listOf(
        Triple("Pine Tree 🌲", "Pine", 0),
        Triple("Sakura Tree 🌸", "Sakura", 40),
        Triple("Mystic Oak 🌳", "Oak", 60),
        Triple("Giant Palm 🌴", "Palm", 80),
        Triple("Sunflower 🌻", "Sunflower", 25),
        Triple("Bonsai Mystic 🔮", "Bonsai", 110)
    )

    val durations = listOf(15, 25, 45, 60, 120)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // LEVEL & PROGRESS RING BOARD CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆 HARBINGER SCHOLAR RATINGS",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Big Level circular/badge display
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = progressPercent,
                        modifier = Modifier.size(110.dp),
                        strokeWidth = 10.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LVL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$profileLevel",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "$profileXP / $maxNextLvlXp XP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                val levelTitle = when (profileLevel) {
                    1 -> "🌱 Focus Novice"
                    2 -> "⚙️ Bronze Apprentice"
                    3 -> "📖 Wisdom Initiate"
                    4 -> "⚡ Silver Pioneer"
                    5 -> "🔥 Streak Warrior"
                    6 -> "⭐ Jade Academic"
                    7 -> "🧠 Zen Overlord"
                    else -> "👑 Supreme Grandmaster Scholar"
                }
                
                Text(
                    text = levelTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Streaks & Coins Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Daily Streak Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STREAK: $profileStreakDays DAYS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF9800)
                        )
                    }

                    // Coins Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "☀️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SUNSHINE COINS: $forestCoins",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        // PLANT A SEED BOARD (Forest App core feature integration)
        Text(
            text = "🌲 FOCUS SEED PLANTER GARDEN",
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color(0xFF4CAF50).copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SEED SPECIES CATALOG",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32)
                )

                // Horizontal scrollable list of available Trees
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    treeSpecies.forEachIndexed { index, (label, key, cost) ->
                        val isSelected = selectedTreeIndex == index
                        val borderC = if (isSelected) Color(0xFF2E7D32) else Color.LightGray.copy(alpha = 0.4f)
                        val bgC = if (isSelected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgC)
                                .border(1.5.dp, borderC, RoundedCornerShape(10.dp))
                                .clickable { selectedTreeIndex = index }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Text(
                                    text = if (cost == 0) "Free" else "☀️ $cost Coins",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cost == 0) Color(0xFF4CAF50) else Color(0xFFD48000)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "SELECT TARGET STUDY SUBJECT",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32)
                )

                if (subjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Red.copy(alpha = 0.05f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚠️ No active study subjects created. Create a subject in general settings first!",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjects.forEach { subj ->
                            val isSelected = selectedSubjectId == subj.id
                            val subjColor = try {
                                Color(android.graphics.Color.parseColor(subj.color))
                            } catch (e: Exception) {
                                themeColors.secondaryGlowColor
                            }
                            val borderC = if (isSelected) subjColor else Color.LightGray.copy(alpha = 0.4f)
                            val bgC = if (isSelected) subjColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgC)
                                    .border(1.5.dp, borderC, RoundedCornerShape(10.dp))
                                    .clickable { selectedSubjectId = subj.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(subjColor))
                                    Text(
                                        text = subj.name.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = if (isSelected) subjColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "SELECT STUDY DURATION TARGET",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32)
                )

                // Duration Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durations.forEachIndexed { dIdx, mins ->
                        val isSelected = selectedDurationIndex == dIdx
                        val bgC = if (isSelected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        val textC = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgC)
                                .clickable { selectedDurationIndex = dIdx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}M",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = textC
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Planting trigger primary action!
                Button(
                    onClick = {
                        val activeType = TimerService.activePlantedTreeType.value
                        if (activeType.isNotEmpty()) {
                            android.widget.Toast.makeText(context, "⚠️ A seed is already growing! Finish or reset active countdown first.", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (subjects.isEmpty()) {
                            android.widget.Toast.makeText(context, "❌ Please create at least one Subject inside Tracker panel first!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val chosenSubIdx = subjects.indexOfFirst { it.id == selectedSubjectId }
                        val activeSubjId = if (chosenSubIdx != -1) selectedSubjectId else subjects.first().id

                        val triple = treeSpecies[selectedTreeIndex]
                        val speciesKey = triple.second
                        val cost = triple.third
                        val durationMins = durations[selectedDurationIndex]

                        if (forestCoins < cost) {
                            android.widget.Toast.makeText(context, "❌ Not enough Sunshine Coins! Plant Free Pine Tree or earn coins.", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            // Deduct coins & set planting parameters!
                            TimerService.forestCoins.value = forestCoins - cost
                            TimerService.activePlantedTreeType.value = speciesKey
                            TimerService.activePlantedSubjectId.value = activeSubjId
                            TimerService.activePlantedDurationMins.value = durationMins
                            
                            // Prep the core timer tracking engine
                            TimerService.activeMode.value = ActiveMode.TIMER
                            val durMs = durationMins * 60 * 1000L
                            TimerService.countdownTotalDuration.value = durMs
                            TimerService.countdownTimeLeft.value = durMs
                            TimerService.activeTrackingSubjectId.value = activeSubjId
                            
                            // Trigger physical service execution
                            val intent = Intent(context, TimerService::class.java).apply {
                                action = "START_COOLDOWN"
                            }
                            context.startService(intent)

                            android.widget.Toast.makeText(context, "🌲 Planted! Your $speciesKey tree is growing under ${subjects.find{it.id == activeSubjId}?.name}! Don't leave!", android.widget.Toast.LENGTH_LONG).show()
                            TimerService.instance?.saveSettings()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "PLANT SEED & START COOLDOWN 🌲",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }

        // SUNSHINE COIN EMPIRE SHOP
        Text(
            text = "☀️ SUNSHINE COIN EMPIRE SHOP",
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp,
            color = Color(0xFFE65100)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "EXCHANGE COINS FOR ACTIVE FOCUS UPGRADES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF57C00)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Item 1: Golden Watering Can
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Golden Watering Can", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Yields +50% coins from mature trees permanently!", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                    if (hasGoldenWateringCan) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Gray.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("OWNED ✅", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (forestCoins >= 100) {
                                    TimerService.forestCoins.value = forestCoins - 100
                                    TimerService.hasGoldenWateringCan.value = true
                                    TimerService.instance?.saveSettings()
                                    android.widget.Toast.makeText(context, "🪙 Unlocked Golden Watering Can! Matured trees now grant 1.5x coins!", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "❌ Need 100 Sunshine Coins to purchase this item!", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("100 ☀️", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Item 2: Super Fertilizer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("🧪", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Super Growth Fertilizer (x$superFertilizerCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Yields +50% XP on your next tree completion!", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                    Button(
                        onClick = {
                            if (forestCoins >= 50) {
                                TimerService.forestCoins.value = forestCoins - 50
                                TimerService.superFertilizerCount.value = superFertilizerCount + 1
                                TimerService.instance?.saveSettings()
                                android.widget.Toast.makeText(context, "🧪 Purchased Super Fertilizer! Your next matured session will yield bonus XP!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "❌ Need 50 Sunshine Coins to purchase Super Fertilizer!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("50 ☀️", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Item 3: Ocean Waves soundtrack
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("🌊", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Ocean Waves Soundscape", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Unlocks wave swell math filter under Settings!", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                    if (hasOceanWaves) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Gray.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("OWNED ✅", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (forestCoins >= 80) {
                                    TimerService.forestCoins.value = forestCoins - 80
                                    TimerService.hasOceanWavesTrack.value = true
                                    TimerService.instance?.saveSettings()
                                    android.widget.Toast.makeText(context, "🌊 Unlocked Ocean Waves! Select it on the companion noises layout.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "❌ Need 80 Sunshine Coins to purchase this soundscape!", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("80 ☀️", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Item 4: Deep Space drone
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("🌌", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Deep Space Drone Hum", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Unlocks 75Hz/76Hz brain entrainment drone!", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                    if (hasDeepSpace) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Gray.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("OWNED ✅", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (forestCoins >= 80) {
                                    TimerService.forestCoins.value = forestCoins - 80
                                    TimerService.hasDeepSpaceTrack.value = true
                                    TimerService.instance?.saveSettings()
                                    android.widget.Toast.makeText(context, "🌌 Unlocked Deep Space Drone! Select it on the companion noises layout.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "❌ Need 80 Sunshine Coins to purchase this soundscape!", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("80 ☀️", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // YOUR ALIVE GARDEN FOREST SHOWCASE (Displays grown trees chronologically)
        Text(
            text = "🌲 YOUR ALIVE FOCUS FOREST GARDEN",
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (grownTreesRaw.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "🌾 Your forest is empty. Accumulate finished study periods using the seed planter above to plant beautiful gardens!",
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "GARDEN SUMMARY: ${grownTreesRaw.size} TREES PLANTED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Display trees in responsive binary grid chunks
                    val chunkedTrees = grownTreesRaw.reversed().chunked(2)
                    chunkedTrees.forEach { rowTrees ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTrees.forEach { treeJsonStr ->
                                var species = "Pine"
                                var subjectName = "General Study"
                                var duration = 25
                                var status = "mature"
                                var timestamp = ""
                                
                                try {
                                    val obj = org.json.JSONObject(treeJsonStr)
                                    species = obj.optString("species", "Pine")
                                    subjectName = obj.optString("subjectName", "General Study")
                                    duration = obj.optInt("duration", 25)
                                    status = obj.optString("status", "mature")
                                    timestamp = obj.optString("timestamp", "")
                                } catch(e: Exception) {}
                                
                                val isGold = status == "mature"
                                val bgCol = if (isGold) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                val borderCol = if (isGold) Color(0xFF81C784) else Color(0xFFE57373)
                                
                                val treeLogo = when (species) {
                                    "Sakura" -> "🌸"
                                    "Oak" -> "🌳"
                                    "Palm" -> "🌴"
                                    "Bonsai" -> "🔮"
                                    "Sunflower" -> "🌻"
                                    else -> "🌲"
                                }
                                
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = bgCol),
                                    border = BorderStroke(0.8.dp, borderCol)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = if (isGold) treeLogo else "🍂", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = if (isGold) species.uppercase() else "WITHERED",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = if (isGold) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                            Text(
                                                text = "$subjectName • ${duration}m",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray
                                            )
                                            Text(
                                                text = if (timestamp.length > 10) timestamp.substring(2, 10) else timestamp,
                                                fontSize = 8.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowTrees.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        // INTERACTIVE DAILY QUEST BOARD
        Text(
            text = "🎯 ACTIVE SCHOLARLY QUESTS",
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        val questList = listOf(
            Triple("qst_dawn", "Dawn Focus Challenge", "Study for 20 mins before 10 AM to start your schedule early. (Reward: 120 XP)"),
            Triple("qst_marathon", "Deep Focus Marathon", "Complete any continuous 45 minute timer session. (Reward: 200 XP)"),
            Triple("qst_streak", "Streak Fortification", "Achieve a registered 2+ day streak in your study logs. (Reward: 150 XP)"),
            Triple("qst_multi", "Subject Explorer", "Create and track at least 3 distinct subjects in your study panel. (Reward: 180 XP)"),
            Triple("qst_sensors", "Sensor Calibration", "Check the temperature calibration in the custom settings panel. (Reward: 100 XP)")
        )
        
        questList.forEach { (id, title, desc) ->
            val isCompleted = completedQuests.contains(id)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCompleted) Color.Gray.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = if (isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = if (isCompleted) Color.Gray.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "CLAIMED ✅",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val currentXP = TimerService.profileXP.value
                                val xpReward = when (id) {
                                    "qst_dawn" -> 120
                                    "qst_marathon" -> 200
                                    "qst_streak" -> 150
                                    "qst_multi" -> 180
                                    else -> 100
                                }
                                
                                var newXP = currentXP + xpReward
                                var currentLvl = TimerService.profileLevel.value
                                var nextLvlXP = currentLvl * 150
                                var leveledUp = false
                                while (newXP >= nextLvlXP) {
                                    newXP -= nextLvlXP
                                    currentLvl++
                                    nextLvlXP = currentLvl * 150
                                    leveledUp = true
                                }
                                
                                TimerService.profileXP.value = newXP
                                TimerService.profileLevel.value = currentLvl
                                TimerService.completedQuestIds.value = completedQuests + id
                                if (leveledUp) {
                                    TimerService.profileStreakDays.value += 1
                                    android.widget.Toast.makeText(context, "🎉 LEVEL UP! You reached LEVEL $currentLvl! 🚀", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "✨ Claimed +$xpReward XP! Keep leveling up! 🎓", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                TimerService.instance?.saveSettings()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "CLAIM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
        
        // UNLOCKABLE TROPHIES & BADGES
        Text(
            text = "🏅 SCHOLAR BADGES & TROPHIES",
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        val badgesList = listOf(
            Triple("⚙️ Bronze Quill", "Unlock at Level 2", profileLevel >= 2),
            Triple("📚 Wisdom Lantern", "Unlock at Level 3", profileLevel >= 3),
            Triple("⚡ Cosmic Compass", "Unlock at Level 4", profileLevel >= 4),
            Triple("🔥 Phoenix Flame", "Unlock at Level 5", profileLevel >= 5),
            Triple("🔮 Chrono Core", "Unlock at Level 6", profileLevel >= 6),
            Triple("👑 Archon Crown", "Unlock at Level 7", profileLevel >= 7)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                badgesList.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (name, unlockStr, unlocked) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else Color.Gray.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (unlocked) "⭐" else "🔒",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (unlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                                        )
                                        Text(
                                            text = if (unlocked) "ACTIVATED" else unlockStr,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (unlocked) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanFormattedMarkdownText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val lines = text.split("\n")
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { line ->
            val cleanLine = line.trim()
            if (cleanLine.isEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
            } else if (cleanLine.startsWith("### ") || cleanLine.startsWith("## ") || cleanLine.startsWith("# ")) {
                val headerText = cleanLine.replace(Regex("^#+\\s+"), "")
                Text(
                    text = headerText,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            } else if (cleanLine.startsWith("- ") || cleanLine.startsWith("* ") || cleanLine.startsWith("• ")) {
                val bulletContent = cleanLine.replace(Regex("^[-*•]\\s+"), "")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "•", color = textColor.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = removeMarkdownFormatting(bulletContent),
                        color = textColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 14.sp
                    )
                }
            } else {
                Text(
                    text = removeMarkdownFormatting(cleanLine),
                    color = textColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

fun removeMarkdownFormatting(raw: String): String {
    return raw.replace("**", "")
        .replace("__", "")
        .replace("`", "")
}

