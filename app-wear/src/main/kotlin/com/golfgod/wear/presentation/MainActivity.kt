package com.golfgod.wear.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.golfgod.core.model.*
import com.golfgod.core.swing.DetectorState
import com.golfgod.core.swing.ImuSample
import com.golfgod.core.swing.SwingDetectionStateMachine
import com.golfgod.wear.presentation.theme.GolfGold
import com.golfgod.wear.presentation.theme.GolfGreen
import com.golfgod.wear.presentation.theme.WearColorPalette

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null
    private var vibrator: Vibrator? = null

    private var currentClubIndex by mutableStateOf(0)
    private var detectorStateText by mutableStateOf("READY")
    private var swingCount by mutableStateOf(0)
    private var lastSwing by mutableStateOf<SwingRecord?>(null)
    private var isSessionActive by mutableStateOf(true)

    // Handedness setting (Defaults to Right-Handed, Lead Wrist)
    private var handedness by mutableStateOf(GolferHandedness.RIGHT_HANDED)
    private var watchWrist by mutableStateOf(WatchWrist.LEAD_WRIST)

    private val detector = SwingDetectionStateMachine(
        currentClub = StandardBag.CLUBS[0],
        profile = GolferProfile(handedness = GolferHandedness.RIGHT_HANDED, watchWrist = WatchWrist.LEAD_WRIST)
    ) { record ->
        handleSwingDetected(record)
    }

    private var lastAx = 0f
    private var lastAy = 0f
    private var lastAz = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        setContent {
            MaterialTheme(colors = WearColorPalette) {
                WearGolfScreen(
                    club = StandardBag.CLUBS[currentClubIndex],
                    handedness = handedness,
                    wrist = watchWrist,
                    swingCount = swingCount,
                    stateText = detectorStateText,
                    lastSwing = lastSwing,
                    onNextClub = {
                        currentClubIndex = (currentClubIndex + 1) % StandardBag.CLUBS.size
                        detector.setClub(StandardBag.CLUBS[currentClubIndex])
                        vibrateClick()
                    },
                    onPrevClub = {
                        currentClubIndex = (currentClubIndex - 1 + StandardBag.CLUBS.size) % StandardBag.CLUBS.size
                        detector.setClub(StandardBag.CLUBS[currentClubIndex])
                        vibrateClick()
                    },
                    onToggleHandedness = {
                        handedness = if (handedness == GolferHandedness.RIGHT_HANDED) {
                            GolferHandedness.LEFT_HANDED
                        } else {
                            GolferHandedness.RIGHT_HANDED
                        }
                        detector.updateHandedness(handedness, watchWrist)
                        vibrateClick()
                    },
                    onResetSession = {
                        swingCount = 0
                        lastSwing = null
                        detector.resetState()
                        vibrateClick()
                    },
                    onSimulateSwing = {
                        simulateSwing()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isSessionActive) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER || event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            lastAx = event.values[0]
            lastAy = event.values[1]
            lastAz = event.values[2]
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val sample = ImuSample(
                timestampNs = event.timestamp,
                ax = lastAx,
                ay = lastAy,
                az = lastAz,
                gx = event.values[0],
                gy = event.values[1],
                gz = event.values[2]
            )
            detector.processSample(sample)
            updateStateLabel(detector.state)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateStateLabel(state: DetectorState) {
        detectorStateText = when (state) {
            DetectorState.WAITING_FOR_ADDRESS -> "HOLD STILL"
            DetectorState.ADDRESS_LOCKED -> "READY"
            DetectorState.TAKEAWAY, DetectorState.BACKSWING -> "BACKSWING"
            DetectorState.TOP_OF_SWING -> "TOP"
            DetectorState.DOWNSWING -> "DOWNSWING"
            DetectorState.IMPACT_CONFIRMED -> "IMPACT!"
            DetectorState.FOLLOW_THROUGH -> "FINISH"
        }
    }

    private fun handleSwingDetected(record: SwingRecord) {
        runOnUiThread {
            lastSwing = record
            swingCount++
            vibrateImpact(record.tempoRating)
        }
    }

    private fun vibrateClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibrateImpact(rating: TempoRating) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (rating == TempoRating.PERFECT) {
                val timings = longArrayOf(0, 80, 50, 80, 50, 120)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                val timings = longArrayOf(0, 100, 60, 120)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            }
        }
    }

    private fun simulateSwing() {
        val club = StandardBag.CLUBS[currentClubIndex]
        val backMs = (700..750).random().toLong()
        val downMs = (230..250).random().toLong()
        val tempo = (backMs.toFloat() / downMs.toFloat() * 10f).toInt() / 10f

        val record = SwingRecord(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            clubCode = club.code,
            backswingTimeMs = backMs,
            downswingTimeMs = downMs,
            tempoRatio = tempo,
            tempoRating = TempoRating.PERFECT,
            peakHandSpeedMph = 23.5f,
            estimatedClubSpeedMph = if (club.code == "DR") 103.2f else 84.5f,
            peakImpactAccelG = 16.4f,
            qualityScore = 92,
            detectedFaults = emptyList(),
            notes = "${handedness.shortCode} Simulated"
        )
        handleSwingDetected(record)
    }
}

@Composable
fun WearGolfScreen(
    club: Club,
    handedness: GolferHandedness,
    wrist: WatchWrist,
    swingCount: Int,
    stateText: String,
    lastSwing: SwingRecord?,
    onNextClub: () -> Unit,
    onPrevClub: () -> Unit,
    onToggleHandedness: () -> Unit,
    onResetSession: () -> Unit,
    onSimulateSwing: () -> Unit
) {
    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E27)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Club Selector Pill + Handedness Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "◀",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { onPrevClub() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161B40))
                                .clickable { onNextClub() }
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${club.emoji} ${club.name}",
                                color = GolfGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "▶",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { onNextClub() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // 1-Tap Handedness Switcher Pill: [🧤 RH (Left Wrist)]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF22295E))
                            .clickable { onToggleHandedness() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🧤 ${handedness.shortCode} · ${if (handedness == GolferHandedness.RIGHT_HANDED) "L-Wrist" else "R-Wrist"}",
                            color = GolfGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center: Live Metrics or Ready State
                if (lastSwing != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSimulateSwing() }
                    ) {
                        Text(
                            text = "${lastSwing.tempoRatio} : 1",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = if (lastSwing.tempoRating == TempoRating.PERFECT) GolfGreen else GolfGold
                        )
                        Text(
                            text = "${lastSwing.backswingTimeMs}ms / ${lastSwing.downswingTimeMs}ms",
                            fontSize = 10.sp,
                            color = Color(0xFFB0B8D1)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${lastSwing.estimatedClubSpeedMph.toInt()} MPH",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSimulateSwing() }
                    ) {
                        Text(
                            text = stateText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = GolfGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Take your stance",
                            fontSize = 10.sp,
                            color = Color(0xFFB0B8D1)
                        )
                    }
                }

                // Bottom: Ball Counter & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⛳ Balls: $swingCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "RESET",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E234A))
                            .clickable { onResetSession() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
