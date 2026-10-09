package com.golfgod.phone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.golfgod.core.model.*
import com.golfgod.core.physics.BallFlightSimulator
import com.golfgod.phone.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GolfGodPhoneApp()
        }
    }
}

enum class PhoneTab(val label: String, val emoji: String) {
    RANGE("Range", "🏌️"),
    COACH("AI Coach", "📹"),
    RADAR("Ball Flight", "📊"),
    BAG("Bag & Hand", "🎒"),
    HISTORY("History", "📜")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GolfGodPhoneApp() {
    var selectedTab by remember { mutableStateOf(PhoneTab.RANGE) }
    var selectedClub by remember { mutableStateOf(StandardBag.CLUBS[0]) }
    var swingsList by remember { mutableStateOf(generateSampleSwings()) }
    var is240FpsEnabled by remember { mutableStateOf(true) }

    // Handedness State (Right-Handed default, with full user toggle)
    var handedness by remember { mutableStateOf(GolferHandedness.RIGHT_HANDED) }
    var watchWrist by remember { mutableStateOf(WatchWrist.LEAD_WRIST) }

    val lastSwing = swingsList.firstOrNull()
    val ballFlight = remember(lastSwing, selectedClub) {
        if (lastSwing != null) {
            BallFlightSimulator.simulateFlight(lastSwing.estimatedClubSpeedMph, selectedClub)
        } else {
            BallFlightSimulator.simulateFlight(98.0f, selectedClub)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⛳", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "GolfGod",
                                fontWeight = FontWeight.Black,
                                color = GolfGold,
                                fontSize = 20.sp
                            )
                        }

                        // Handedness, High-Speed Camera & Watch Pairing Badges
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Handedness Pill (clickable)
                            Text(
                                text = "🏌️ ${handedness.shortCode}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22295E))
                                    .clickable {
                                        handedness = if (handedness == GolferHandedness.RIGHT_HANDED) {
                                            GolferHandedness.LEFT_HANDED
                                        } else {
                                            GolferHandedness.RIGHT_HANDED
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                            Text(
                                text = if (is240FpsEnabled) "📹 240fps" else "📹 30fps",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GolfGold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2A200B))
                                    .clickable { is240FpsEnabled = !is240FpsEnabled }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                            Text(
                                text = "⌚ Watch (IMU)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GolfGreen,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1B3D2B))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkNavyBg,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F1434),
                contentColor = TextPrimary
            ) {
                PhoneTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.emoji, fontSize = 18.sp) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GolfGold,
                            selectedTextColor = GolfGold,
                            indicatorColor = Color(0xFF22295E),
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        },
        containerColor = DarkNavyBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkNavyBg)
        ) {
            when (selectedTab) {
                PhoneTab.RANGE -> RangeScreen(
                    selectedClub = selectedClub,
                    handedness = handedness,
                    wrist = watchWrist,
                    onToggleHandedness = {
                        handedness = if (handedness == GolferHandedness.RIGHT_HANDED) GolferHandedness.LEFT_HANDED else GolferHandedness.RIGHT_HANDED
                    },
                    onClubSelected = { selectedClub = it },
                    lastSwing = lastSwing,
                    ballFlight = ballFlight,
                    swingsList = swingsList,
                    onSimulateSwing = {
                        val newSwing = simulateNewSwing(selectedClub, handedness)
                        swingsList = listOf(newSwing) + swingsList
                    },
                    onNavigateToCoach = { selectedTab = PhoneTab.COACH }
                )
                PhoneTab.COACH -> VideoCoachScreen(
                    selectedClub = selectedClub,
                    handedness = handedness,
                    is240Fps = is240FpsEnabled,
                    onToggle240Fps = { is240FpsEnabled = !is240FpsEnabled },
                    onSwingAnalyzed = { newSwing ->
                        swingsList = listOf(newSwing) + swingsList
                    }
                )
                PhoneTab.RADAR -> RadarFlightScreen(
                    club = selectedClub,
                    swing = lastSwing,
                    flight = ballFlight
                )
                PhoneTab.BAG -> BagScreen(
                    selectedClub = selectedClub,
                    handedness = handedness,
                    wrist = watchWrist,
                    onHandednessChanged = { handedness = it },
                    onWristChanged = { watchWrist = it },
                    onClubSelected = { selectedClub = it }
                )
                PhoneTab.HISTORY -> HistoryScreen(swingsList = swingsList)
            }
        }
    }
}

@Composable
fun RangeScreen(
    selectedClub: Club,
    handedness: GolferHandedness,
    wrist: WatchWrist,
    onToggleHandedness: () -> Unit,
    onClubSelected: (Club) -> Unit,
    lastSwing: SwingRecord?,
    ballFlight: BallFlightData,
    swingsList: List<SwingRecord>,
    onSimulateSwing: () -> Unit,
    onNavigateToCoach: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera Coach Hero Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E234A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GolfGold, RoundedCornerShape(14.dp))
                    .clickable { onNavigateToCoach() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A200B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📹", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("CAMERA SWING COACH & ANALYZER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                            Text("Live 240 FPS Camera + Smartwatch IMU", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Down-the-line posture, spine & swing plane", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    Text("OPEN ➔", color = GolfGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        // Handedness Quick Toggle Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GOLFER HANDEDNESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (handedness == GolferHandedness.RIGHT_HANDED) GolfGold else CardNavyBg)
                            .clickable { onToggleHandedness() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Right-Handed (RH)",
                            color = if (handedness == GolferHandedness.RIGHT_HANDED) Color.Black else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (handedness == GolferHandedness.LEFT_HANDED) GolfGold else CardNavyBg)
                            .clickable { onToggleHandedness() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Left-Handed (LH)",
                            color = if (handedness == GolferHandedness.LEFT_HANDED) Color.Black else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Club Selector Bar
        item {
            Text("ACTIVE CLUB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StandardBag.CLUBS) { club ->
                    val isSelected = club.code == selectedClub.code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GolfGold else CardNavyBg)
                            .border(1.dp, if (isSelected) GolfGold else CardBorder, RoundedCornerShape(10.dp))
                            .clickable { onClubSelected(club) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${club.emoji} ${club.code}",
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Hero Swing & Flight Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LATEST SWING METRICS (${handedness.shortCode})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                            Text("${selectedClub.name} (${selectedClub.loftDegrees}°)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (lastSwing?.tempoRating == TempoRating.PERFECT) Color(0xFF1B3D2B) else Color(0xFF3E2813))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = lastSwing?.tempoRating?.label ?: "Ready",
                                color = if (lastSwing?.tempoRating == TempoRating.PERFECT) GolfGreen else WarningOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricColumn(
                            title = "EST. CARRY",
                            value = "${ballFlight.estimatedCarryYards}y",
                            subtitle = "Total: ${ballFlight.estimatedTotalYards}y",
                            color = GolfGold
                        )
                        MetricColumn(
                            title = "CLUB SPEED",
                            value = "${lastSwing?.estimatedClubSpeedMph?.toInt() ?: 102}",
                            subtitle = "MPH",
                            color = TextPrimary
                        )
                        MetricColumn(
                            title = "BALL SPEED",
                            value = "${ballFlight.estimatedBallSpeedMph}",
                            subtitle = "Smash ${ballFlight.estimatedSmashFactor}",
                            color = TextPrimary
                        )
                        MetricColumn(
                            title = "TEMPO RATIO",
                            value = "${lastSwing?.tempoRatio ?: 3.0f}:1",
                            subtitle = "Ideal 3.0:1",
                            color = if (lastSwing?.tempoRating == TempoRating.PERFECT) GolfGreen else WarningOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSimulateSwing,
                        colors = ButtonDefaults.buttonColors(containerColor = GolfGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SIMULATE RANGE SWING ⚡", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricColumn(title: String, value: String, subtitle: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, fontSize = 10.sp, color = TextSecondary)
    }
}

// =============================================================================
// CAMERA PREVIEW & AI SWING COACH SCREEN
// =============================================================================

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    isTorchOn: Boolean = false,
    onVisionTelemetry: (LiveVisionTelemetry) -> Unit = {},
    onAutoSwingDetected: (peakMotion: Float, spineDelta: Float, pelvisDrift: Float) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier,
        update = { view ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(view.surfaceProvider)
                    }

                    val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                        .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    val visionAnalyzer = SwingVisionAnalyzer(
                        onTelemetry = { telemetry ->
                            ContextCompat.getMainExecutor(context).execute {
                                onVisionTelemetry(telemetry)
                            }
                        },
                        onSwingImpactDetected = { peak, spine, pelvis ->
                            ContextCompat.getMainExecutor(context).execute {
                                onAutoSwingDetected(peak, spine, pelvis)
                            }
                        }
                    )
                    imageAnalysis.setAnalyzer(cameraExecutor, visionAnalyzer)

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    if (camera.cameraInfo.hasFlashUnit()) {
                        camera.cameraControl.enableTorch(isTorchOn)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

enum class AlignmentOverlayMode(val label: String) {
    DTL_PLANE("DTL Plane"),
    POSTURE_GRID("Posture Grid"),
    FULL_POSE("33-Pose"),
    OFF("Off")
}

@Composable
fun CameraBiomechanicalOverlay(
    modifier: Modifier = Modifier,
    handedness: GolferHandedness,
    overlayMode: AlignmentOverlayMode,
    isRecording: Boolean,
    recordingProgress: Float
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (overlayMode == AlignmentOverlayMode.OFF) return@Canvas

        // 1. Ground & Address Reference Line
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(0f, h * 0.88f),
            end = Offset(w, h * 0.88f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
        )

        // 2. Down-The-Line (DTL) Swing Plane Angle Line (Gold)
        if (overlayMode == AlignmentOverlayMode.DTL_PLANE || overlayMode == AlignmentOverlayMode.FULL_POSE) {
            val startX = if (handedness == GolferHandedness.RIGHT_HANDED) w * 0.35f else w * 0.65f
            val endX = if (handedness == GolferHandedness.RIGHT_HANDED) w * 0.78f else w * 0.22f

            // Shaft Plane
            drawLine(
                color = GolfGold,
                start = Offset(startX, h * 0.88f),
                end = Offset(endX, h * 0.22f),
                strokeWidth = 4f
            )

            // Shoulder Corridor Line
            drawLine(
                color = Color(0x88FFD700),
                start = Offset(startX - 30f, h * 0.85f),
                end = Offset(endX - 30f, h * 0.18f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }

        // 3. Setup Spine Angle Guide Line (Cyan)
        if (overlayMode == AlignmentOverlayMode.POSTURE_GRID || overlayMode == AlignmentOverlayMode.FULL_POSE) {
            val hipX = if (handedness == GolferHandedness.RIGHT_HANDED) w * 0.42f else w * 0.58f
            val headX = if (handedness == GolferHandedness.RIGHT_HANDED) w * 0.52f else w * 0.48f

            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(hipX, h * 0.65f),
                end = Offset(headX, h * 0.30f),
                strokeWidth = 3f
            )

            // Head Sway Box
            drawRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(headX - 35f, h * 0.22f),
                size = Size(70f, 70f),
                style = Stroke(width = 2f)
            )

            // Pelvis Early Extension Wall
            val wallX = if (handedness == GolferHandedness.RIGHT_HANDED) hipX - 25f else hipX + 25f
            drawLine(
                color = WarningOrange,
                start = Offset(wallX, h * 0.52f),
                end = Offset(wallX, h * 0.80f),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        // 4. Active Recording 33-Joint Keypoints Animation
        if (isRecording || overlayMode == AlignmentOverlayMode.FULL_POSE) {
            val keypoints = listOf(
                Offset(w * 0.50f, h * 0.25f), // Head
                Offset(w * 0.48f, h * 0.36f), // Shoulder L
                Offset(w * 0.54f, h * 0.36f), // Shoulder R
                Offset(w * 0.42f, h * 0.48f), // Elbow
                Offset(w * 0.38f, h * 0.58f), // Hands
                Offset(w * 0.46f, h * 0.58f), // Hip L
                Offset(w * 0.52f, h * 0.58f), // Hip R
                Offset(w * 0.45f, h * 0.72f), // Knee L
                Offset(w * 0.53f, h * 0.72f), // Knee R
                Offset(w * 0.43f, h * 0.88f), // Ankle L
                Offset(w * 0.55f, h * 0.88f)  // Ankle R
            )

            keypoints.forEach { pt ->
                drawCircle(
                    color = GolfGreen,
                    radius = 5f,
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = pt
                )
            }
        }
    }
}

@Composable
fun VideoCoachScreen(
    selectedClub: Club,
    handedness: GolferHandedness,
    is240Fps: Boolean,
    onToggle240Fps: () -> Unit,
    onSwingAnalyzed: (SwingRecord) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Camera Permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Camera Configuration & States
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var overlayMode by remember { mutableStateOf(AlignmentOverlayMode.DTL_PLANE) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingProgress by remember { mutableStateOf(0f) }
    var analysisStatus by remember { mutableStateOf("READY AT ADDRESS · DTL VIEW") }
    var useSimulatedFallback by remember { mutableStateOf(false) }

    // Dynamic Biomechanical Metrics (Calculated on each swing)
    var spineAngleSetup by remember { mutableStateOf(38) }
    var spineAngleImpact by remember { mutableStateOf(37) }
    var shoulderTurn by remember { mutableStateOf(92) }
    var hipTurn by remember { mutableStateOf(44) }
    var earlyExtensionInches by remember { mutableStateOf(2.8f) }
    var clubShaftP6Angle by remember { mutableStateOf(48) }
    var coachGrade by remember { mutableStateOf("B+") }
    var frameCaptureCount by remember { mutableStateOf(57) }
    var visionTelemetry by remember {
        mutableStateOf(
            LiveVisionTelemetry(
                processedFps = 0f,
                motionEnergyPct = 0f,
                isMotionDetected = false,
                detectedSpineAngleDeg = 38.0f,
                pelvisDriftInches = 0.5f,
                headSwayInches = 0.3f,
                swingPhase = SwingPhase.ADDRESS,
                averageLuminance = 128f,
                frameCount = 0L,
                upperBodyMotionPct = 50f,
                lowerBodyMotionPct = 50f
            )
        )
    }

    // Swing Capture Function (Phone Camera + Smartwatch Trigger)
    fun triggerSwingCapture(triggeredByWatch: Boolean) {
        if (isRecording) return
        coroutineScope.launch {
            isRecording = true
            analysisStatus = if (triggeredByWatch) {
                "⌚ WATCH TRIGGERED! RECORDING 240 FPS DOWNSWING..."
            } else {
                "🔴 RECORDING DOWNSWING (FRAME 1/$frameCaptureCount)..."
            }

            // High-Speed Frame Capture Simulation
            for (f in 1..frameCaptureCount) {
                recordingProgress = f.toFloat() / frameCaptureCount.toFloat()
                if (f % 10 == 0) {
                    analysisStatus = "🔴 CAPTURING SWING (FRAME $f/$frameCaptureCount @ 240 FPS)..."
                }
                delay(16) // ~60fps UI refresh during 240fps buffer capture
            }

            analysisStatus = "⚙️ PROCESSING 33-POINT BIOMECHANICAL POSTURE..."
            delay(400)

            // Randomize slight variations to simulate active real-time AI coach feedback
            spineAngleSetup = (36..40).random()
            spineAngleImpact = spineAngleSetup - (0..2).random()
            shoulderTurn = (89..95).random()
            hipTurn = (42..47).random()
            earlyExtensionInches = if (triggeredByWatch) (12..24).random() / 10.0f else 2.8f
            clubShaftP6Angle = (46..50).random()
            coachGrade = if (earlyExtensionInches <= 1.8f) "A" else "B+"

            val simulatedRecord = simulateNewSwing(selectedClub, handedness)
            onSwingAnalyzed(simulatedRecord)

            isRecording = false
            recordingProgress = 0f
            analysisStatus = if (triggeredByWatch) {
                "✓ WATCH-SYNCED SWING ANALYZED (${simulatedRecord.estimatedClubSpeedMph.toInt()} MPH · GRADE: $coachGrade)"
            } else {
                "✓ SWING ANALYZED (${simulatedRecord.estimatedClubSpeedMph.toInt()} MPH · GRADE: $coachGrade)"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AI VIDEO SWING COACH & ANALYZER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Text("${handedness.label} DTL Vision Model", fontSize = 14.sp, color = TextSecondary)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (is240Fps) Color(0xFF2A200B) else Color(0xFF1E234A))
                        .clickable { onToggle240Fps() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (is240Fps) "⚡ 240 FPS (ON)" else "30 FPS",
                        color = if (is240Fps) GolfGold else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live CameraX Viewfinder / Permission Request Area
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F2C)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, if (isRecording) Color.Red else GolfGold, RoundedCornerShape(16.dp))
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!hasCameraPermission && !useSimulatedFallback) {
                            // Permission Request Card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("📷", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Camera Access Required",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "GolfGod needs live camera access to capture your swing at high speed (240 FPS), audit down-the-line posture, and analyze 33 joint angles. Your smartwatch acts as a wrist supplement.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GolfGold),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("GRANT CAMERA ACCESS 📹", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = { useSimulatedFallback = true }
                                ) {
                                    Text("Test with Simulated Camera Grid", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        } else {
                            // Active Live CameraX Viewfinder
                            if (hasCameraPermission) {
                                CameraPreviewView(
                                    modifier = Modifier.fillMaxSize(),
                                    lensFacing = lensFacing,
                                    isTorchOn = isTorchOn,
                                    onVisionTelemetry = { telemetry ->
                                        visionTelemetry = telemetry
                                        if (!isRecording) {
                                            analysisStatus = "LIVE CV · ${telemetry.swingPhase.name} · ${telemetry.detectedSpineAngleDeg}°"
                                        }
                                    },
                                    onAutoSwingDetected = { peakMotion, spineDelta, pelvisDrift ->
                                        earlyExtensionInches = pelvisDrift
                                        spineAngleSetup = 38
                                        spineAngleImpact = (38 - spineDelta).toInt()
                                        coachGrade = if (pelvisDrift <= 1.8f) "A" else "B+"
                                        val simRecord = simulateNewSwing(selectedClub, handedness)
                                        onSwingAnalyzed(simRecord)
                                        analysisStatus = "✓ AUTO-CAPTURED SWING! (${simRecord.estimatedClubSpeedMph.toInt()} MPH · GRADE: $coachGrade)"
                                    }
                                )
                            } else {
                                // Simulated Viewfinder Backdrop
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F1A30)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⛳ SIMULATED DTL CAMERA FEED", color = Color(0x44FFFFFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            // Biomechanical HUD & Alignment Overlay
                            CameraBiomechanicalOverlay(
                                modifier = Modifier.fillMaxSize(),
                                handedness = handedness,
                                overlayMode = overlayMode,
                                isRecording = isRecording,
                                recordingProgress = recordingProgress
                            )

                            // Top Viewfinder Controls Overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .background(Color(0x990A0F2C))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Switch Lens
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E234A))
                                            .clickable {
                                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                                    CameraSelector.LENS_FACING_FRONT
                                                } else {
                                                    CameraSelector.LENS_FACING_BACK
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (lensFacing == CameraSelector.LENS_FACING_BACK) "🔄 Back" else "🔄 Front",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    // Torch Toggle
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isTorchOn) GolfGold else Color(0xFF1E234A))
                                            .clickable { isTorchOn = !isTorchOn }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isTorchOn) "💡 Light ON" else "💡 Light",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTorchOn) Color.Black else TextPrimary
                                        )
                                    }

                                    // Overlay Grid Mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E234A))
                                            .clickable {
                                                overlayMode = when (overlayMode) {
                                                    AlignmentOverlayMode.DTL_PLANE -> AlignmentOverlayMode.POSTURE_GRID
                                                    AlignmentOverlayMode.POSTURE_GRID -> AlignmentOverlayMode.FULL_POSE
                                                    AlignmentOverlayMode.FULL_POSE -> AlignmentOverlayMode.OFF
                                                    AlignmentOverlayMode.OFF -> AlignmentOverlayMode.DTL_PLANE
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "📐 ${overlayMode.label}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GolfGold
                                        )
                                    }
                                }

                                Text(
                                    text = if (is240Fps) "240 FPS HD" else "60 FPS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GolfGreen
                                )
                            }

                            // Bottom Viewfinder Status & Live Progress
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color(0xB3000000))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (isRecording) {
                                    LinearProgressIndicator(
                                        progress = recordingProgress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Color.Red,
                                        trackColor = Color(0x33FFFFFF)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = analysisStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRecording) Color.Red else GolfGold
                                    )
                                    Text(
                                        text = "${selectedClub.name} (${selectedClub.loftDegrees}°)",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📹 CV SENSOR: ${visionTelemetry.processedFps.toInt()} FPS · Motion ${visionTelemetry.motionEnergyPct.toInt()}% · Spine ${visionTelemetry.detectedSpineAngleDeg}°",
                                        fontSize = 9.sp,
                                        color = if (visionTelemetry.isMotionDetected) GolfGreen else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0x44FFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = (visionTelemetry.motionEnergyPct / 100f).coerceIn(0f, 1f))
                                                .background(if (visionTelemetry.motionEnergyPct > 40f) Color.Red else GolfGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Viewfinder Control Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { triggerSwingCapture(triggeredByWatch = false) },
                            enabled = !isRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color.Red else GolfGold
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = if (isRecording) "RECORDING..." else "🔴 RECORD & ANALYZE SWING",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { triggerSwingCapture(triggeredByWatch = true) },
                            enabled = !isRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3D2B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "⌚ WATCH TRIGGER",
                                color = GolfGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Smartwatch (Wear OS) as Supplement Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF22295E))),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⌚", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SMARTWATCH IMU (WRIST SUPPLEMENT)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGreen)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1B3D2B))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("SYNCED (200Hz)", color = GolfGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The smartphone acts as your primary visual swing coach (capturing high-speed posture, spine retention, and swing plane). Your smartwatch serves as the essential wrist supplement: polling linear acceleration & gyroscope at 200 Hz to detect takeaway, provide haptic top-of-swing timing, and automatically trigger the phone camera at impact.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Record on Wrist Impact: ACTIVE", fontSize = 10.sp, color = GolfGold, fontWeight = FontWeight.Bold)
                        Text("Lead Wrist (Left)", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }

        // Biomechanical Angles Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${handedness.shortCode} BIOMECHANICAL ANGLES (ANALYZED)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Text("Captured across $frameCaptureCount high-speed video frames during downswing.", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    AngleRow("Spine Angle Retention", "${spineAngleSetup}° setup → ${spineAngleImpact}° impact", "STABLE ✓", GolfGreen)
                    AngleRow("Shoulder Turn at Top", "${shoulderTurn}°", "EXCELLENT ✓", GolfGreen)
                    AngleRow("Hip Turn at Top", "${hipTurn}°", "IDEAL (X-Factor: ${shoulderTurn - hipTurn}°)", GolfGreen)
                    AngleRow(
                        label = "Early Extension (Pelvis Drift)",
                        value = "${earlyExtensionInches} in. forward",
                        rating = if (earlyExtensionInches <= 1.8f) "IDEAL ✓" else "NEEDS WORK ⚠️",
                        color = if (earlyExtensionInches <= 1.8f) GolfGreen else WarningOrange
                    )
                    AngleRow("Club Shaft Angle at P6", "${clubShaftP6Angle}° on-plane", "ON PLANE ✓", GolfGreen)
                    AngleRow("Head Movement (Sway)", "0.5 inches", "SOLID ✓", GolfGreen)
                }
            }
        }

        // Coach Recommendation & Prescribed Drill
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarningOrange)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("COACH RECOMMENDATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                        Text("Grade: $coachGrade", fontSize = 13.sp, fontWeight = FontWeight.Black, color = GolfGold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Primary Fault: Pelvis Early Extension", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your hips moved ${earlyExtensionInches} inches closer to the ball during the transition. When hips push forward, the torso stands up, forcing you to flip your hands through impact.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E234A))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("🏌️ Prescribed Fix: The Glute Wall / Chair Drill", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GolfGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Place your golf bag or chair touching your trail glute at setup.\n2. On backswing, push your trail hip into it.\n3. On downswing, roll your lead glute back against the wall BEFORE impact.\n4. Do 10 slow reps holding finish balance.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AngleRow(label: String, value: String, rating: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Row {
            Text(value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(rating, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RadarFlightScreen(club: Club, swing: SwingRecord?, flight: BallFlightData) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("BALL FLIGHT & RADAR TRAJECTORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GolfGold)
            Text("${club.name} Aerodynamic Physics Simulation", fontSize = 14.sp, color = TextSecondary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("DRIVING RANGE TRACER (ESTIMATED)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0C2417)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("--- 250 YDS ---", color = Color(0xFF388E3C), fontSize = 10.sp)
                            Text("--- 200 YDS ---", color = Color(0xFF388E3C), fontSize = 10.sp)
                            Text("--- 150 YDS ---", color = Color(0xFF388E3C), fontSize = 10.sp)
                            Text("--- 100 YDS ---", color = Color(0xFF388E3C), fontSize = 10.sp)
                            Text("--- 50 YDS ---", color = Color(0xFF388E3C), fontSize = 10.sp)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = (flight.estimatedCarryYards * 0.55f).coerceIn(20f, 160f).dp)
                        ) {
                            Text("🎯", fontSize = 20.sp)
                            Text("${flight.estimatedCarryYards}y", color = GolfGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LAUNCH MONITOR METRICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Spacer(modifier = Modifier.height(10.dp))
                    LaunchRow("Ball Speed (Est.)", "${flight.estimatedBallSpeedMph} MPH")
                    LaunchRow("Clubhead Speed", "${swing?.estimatedClubSpeedMph?.toInt() ?: 102} MPH")
                    LaunchRow("Smash Factor", "${flight.estimatedSmashFactor}")
                    LaunchRow("Launch Angle", "${flight.estimatedLaunchAngleDeg}°")
                    LaunchRow("Backspin Rate (Est.)", "${flight.estimatedBackspinRpm} RPM")
                    LaunchRow("Apex Height", "${flight.estimatedApexFeet} FT")
                    LaunchRow("Carry Distance", "${flight.estimatedCarryYards} YDS")
                    LaunchRow("Total Distance", "${flight.estimatedTotalYards} YDS")
                    LaunchRow("Hang Time", "${flight.flightTimeSeconds}s")
                }
            }
        }
    }
}

@Composable
fun LaunchRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BagScreen(
    selectedClub: Club,
    handedness: GolferHandedness,
    wrist: WatchWrist,
    onHandednessChanged: (GolferHandedness) -> Unit,
    onWristChanged: (WatchWrist) -> Unit,
    onClubSelected: (Club) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Handedness and Watch Wrist Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GOLFER HANDEDNESS & WRIST POSITION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Dominant Hand:", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onHandednessChanged(GolferHandedness.RIGHT_HANDED) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (handedness == GolferHandedness.RIGHT_HANDED) GolfGold else Color(0xFF22295E)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Right-Handed (RH)", color = if (handedness == GolferHandedness.RIGHT_HANDED) Color.Black else TextPrimary, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onHandednessChanged(GolferHandedness.LEFT_HANDED) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (handedness == GolferHandedness.LEFT_HANDED) GolfGold else Color(0xFF22295E)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Left-Handed (LH)", color = if (handedness == GolferHandedness.LEFT_HANDED) Color.Black else TextPrimary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Watch Wrist Location:", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val leadLabel = if (handedness == GolferHandedness.RIGHT_HANDED) "Left Wrist (Lead)" else "Right Wrist (Lead)"
                        val trailLabel = if (handedness == GolferHandedness.RIGHT_HANDED) "Right Wrist (Trail)" else "Left Wrist (Trail)"

                        Button(
                            onClick = { onWristChanged(WatchWrist.LEAD_WRIST) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (wrist == WatchWrist.LEAD_WRIST) GolfGreen else Color(0xFF22295E)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(leadLabel, color = if (wrist == WatchWrist.LEAD_WRIST) Color.Black else TextPrimary, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onWristChanged(WatchWrist.TRAIL_WRIST) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (wrist == WatchWrist.TRAIL_WRIST) GolfGreen else Color(0xFF22295E)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(trailLabel, color = if (wrist == WatchWrist.TRAIL_WRIST) Color.Black else TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("14-CLUB BAG & DISTANCE GAPPING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GolfGold)
            Text("Tap a club to set as active tracking target", fontSize = 13.sp, color = TextSecondary)
        }

        items(StandardBag.CLUBS) { club ->
            val isSelected = club.code == selectedClub.code
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1E234A) else CardNavyBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClubSelected(club) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(club.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(club.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Loft: ${club.loftDegrees}° · Shaft: ${club.shaftLengthInches}\"", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("~${club.typicalCarryYards} Yds", fontWeight = FontWeight.Black, color = GolfGold, fontSize = 15.sp)
                        Text("Tempo: ${club.defaultTargetTempo}:1", fontSize = 10.sp, color = GolfGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(swingsList: List<SwingRecord>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("SESSION ANALYTICS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GolfGold)
            Text("Driving Range Consistency Breakdown", fontSize = 14.sp, color = TextSecondary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TODAY'S RANGE SESSION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Spacer(modifier = Modifier.height(10.dp))
                    LaunchRow("Total Balls Hit", "${swingsList.size}")
                    LaunchRow("Tour Ideal Tempo (3:1)", "${swingsList.count { it.tempoRating == TempoRating.PERFECT }} swings")
                    val avgScore = if (swingsList.isNotEmpty()) swingsList.map { it.qualityScore }.average().toInt() else 0
                    LaunchRow("Avg Quality Score", "$avgScore/100")
                }
            }
        }
    }
}

fun generateSampleSwings(): List<SwingRecord> {
    return listOf(
        SwingRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis() - 60000,
            clubCode = "DR",
            backswingTimeMs = 730,
            downswingTimeMs = 242,
            tempoRatio = 3.0f,
            tempoRating = TempoRating.PERFECT,
            peakHandSpeedMph = 24.2f,
            estimatedClubSpeedMph = 103.5f,
            peakImpactAccelG = 16.8f,
            qualityScore = 94,
            detectedFaults = emptyList()
        ),
        SwingRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis() - 180000,
            clubCode = "7I",
            backswingTimeMs = 690,
            downswingTimeMs = 230,
            tempoRatio = 3.0f,
            tempoRating = TempoRating.PERFECT,
            peakHandSpeedMph = 20.1f,
            estimatedClubSpeedMph = 85.0f,
            peakImpactAccelG = 14.2f,
            qualityScore = 91,
            detectedFaults = emptyList()
        )
    )
}

fun simulateNewSwing(club: Club, handedness: GolferHandedness): SwingRecord {
    val backMs = (680..760).random().toLong()
    val downMs = (220..260).random().toLong()
    val tempo = ((backMs.toFloat() / downMs.toFloat()) * 10f).toInt() / 10f
    val baseSpeed = when (club.code) {
        "DR" -> (98..107).random().toFloat()
        "3W", "5W" -> (90..98).random().toFloat()
        "3H", "4I", "5I" -> (85..92).random().toFloat()
        "6I", "7I", "8I" -> (78..86).random().toFloat()
        else -> (65..75).random().toFloat()
    }
    return SwingRecord(
        id = UUID.randomUUID().toString(),
        timestamp = System.currentTimeMillis(),
        clubCode = club.code,
        backswingTimeMs = backMs,
        downswingTimeMs = downMs,
        tempoRatio = tempo,
        tempoRating = if (tempo in 2.8f..3.2f) TempoRating.PERFECT else TempoRating.FAST_DOWNSWING,
        peakHandSpeedMph = (baseSpeed * 0.24f),
        estimatedClubSpeedMph = baseSpeed,
        peakImpactAccelG = (12..18).random().toFloat(),
        qualityScore = (85..96).random(),
        detectedFaults = if (tempo !in 2.8f..3.2f) listOf("Slightly rushed downswing transition") else emptyList(),
        notes = "${handedness.shortCode} Simulated Swing"
    )
}
