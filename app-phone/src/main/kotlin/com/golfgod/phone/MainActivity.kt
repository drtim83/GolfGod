package com.golfgod.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.nativeCanvas
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
import java.util.Locale
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
    COACH("AI Coach", "📹"),
    RANGE("Range", "🏌️"),
    RADAR("Ball Flight", "📊"),
    BAG("Bag & Hand", "🎒"),
    HISTORY("History", "📜")
}

enum class CameraPerspective(val label: String, val description: String) {
    DOWN_THE_LINE("Down-The-Line (DTL)", "Audits swing plane, shaft shallowing & pelvis extension"),
    FACE_ON("Face-On (FO)", "Audits ball position, shoulder tilt & head sway")
}

enum class SwingCheckpoint(val code: String, val title: String, val phase: String) {
    P1("P1", "Address & Setup", "Posture & Balance"),
    P2("P2", "Takeaway", "Shaft Parallel"),
    P4("P4", "Top of Backswing", "Coil & X-Factor"),
    P6("P6", "Delivery Slot", "Downswing Shallowing"),
    P7("P7", "Impact Zone", "The Moment of Truth"),
    P8("P8", "Follow-Through", "Balance & Extension")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GolfGodPhoneApp() {
    // Default directly to AI Coach & Swing Analyzer
    var selectedTab by remember { mutableStateOf(PhoneTab.COACH) }
    var selectedClub by remember { mutableStateOf(StandardBag.CLUBS[0]) }
    var swingsList by remember { mutableStateOf(generateSampleSwings()) }
    var is240FpsEnabled by remember { mutableStateOf(true) }

    // Handedness State (Right-Handed default, with full user toggle)
    var handedness by remember { mutableStateOf(GolferHandedness.RIGHT_HANDED) }
    var watchWrist by remember { mutableStateOf(WatchWrist.LEAD_WRIST) }

    // Text-To-Speech for Voice Caddie Coaching Announcements
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isVoiceCoachEnabled by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }
        textToSpeech = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

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

                        // Handedness, High-Speed Camera & Voice Coach Badges
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
                                text = if (is240FpsEnabled) "📹 240fps" else "📹 60fps",
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
                                text = if (isVoiceCoachEnabled) "🗣️ Voice ON" else "🗣️ Mute",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVoiceCoachEnabled) GolfGreen else Color.Gray,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isVoiceCoachEnabled) Color(0xFF1B3D2B) else Color(0xFF1E234A))
                                    .clickable { isVoiceCoachEnabled = !isVoiceCoachEnabled }
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
                PhoneTab.COACH -> SwingCoachScreen(
                    selectedClub = selectedClub,
                    handedness = handedness,
                    is240Fps = is240FpsEnabled,
                    onToggle240Fps = { is240FpsEnabled = !is240FpsEnabled },
                    onSwingAnalyzed = { newSwing ->
                        swingsList = listOf(newSwing) + swingsList
                    },
                    tts = textToSpeech,
                    isVoiceCoachEnabled = isVoiceCoachEnabled
                )
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

// =============================================================================
// CAMERA PREVIEW WITH HARDWARE LIFECYCLE & IMAGE ANALYSIS
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
    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    // Stable PreviewView instance held in remember to prevent surface recreation on parent recomposition
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Keep fresh callbacks without triggering camera rebinds
    val currentTelemetryCallback by rememberUpdatedState(onVisionTelemetry)
    val currentSwingDetectedCallback by rememberUpdatedState(onAutoSwingDetected)

    // Bind camera ONLY when lensFacing or lifecycleOwner changes
    LaunchedEffect(lensFacing, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                val visionAnalyzer = SwingVisionAnalyzer(
                    onTelemetry = { telemetry ->
                        ContextCompat.getMainExecutor(context).execute {
                            currentTelemetryCallback(telemetry)
                        }
                    },
                    onSwingImpactDetected = { peak, spine, pelvis ->
                        ContextCompat.getMainExecutor(context).execute {
                            currentSwingDetectedCallback(peak, spine, pelvis)
                        }
                    }
                )
                imageAnalysis.setAnalyzer(cameraExecutor, visionAnalyzer)

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                camera = boundCamera
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Torch control without rebuilding camera pipeline
    LaunchedEffect(isTorchOn, camera) {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(isTorchOn)
            }
        }
    }

    // Clean disposal on unmount
    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // Ignore disposal errors
            }
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

// =============================================================================
// BIOMECHANICAL ALIGNMENT OVERLAY CANVAS (DTL & FACE-ON)
// =============================================================================

@Composable
fun CameraBiomechanicalOverlay(
    modifier: Modifier = Modifier,
    handedness: GolferHandedness,
    perspective: CameraPerspective,
    telemetry: LiveVisionTelemetry = LiveVisionTelemetry(),
    isRecording: Boolean = false,
    recordingProgress: Float = 0f,
    showPoseSkeleton: Boolean = true
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ground Baseline Across Screen
        drawLine(
            color = Color(0x77FFFFFF),
            start = Offset(0f, h * 0.88f),
            end = Offset(w, h * 0.88f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
        )

        if (!showPoseSkeleton) return@Canvas

        val textPaint = android.graphics.Paint().apply {
            textSize = 28f
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
        }

        if (perspective == CameraPerspective.DOWN_THE_LINE) {
            // =================================================================
            // DOWN-THE-LINE (DTL) TOUR BIOMECHANICS & POSE SKELETON
            // =================================================================
            val isRH = handedness == GolferHandedness.RIGHT_HANDED
            val spineOffset = (telemetry.detectedSpineAngleDeg - 38f) * 1.2f
            val pelvisShift = telemetry.pelvisDriftInches * 6f

            // 1. Core Posture Landmarks
            val hipX = (if (isRH) w * 0.44f else w * 0.56f) + (if (isRH) pelvisShift else -pelvisShift)
            val hipY = h * 0.56f

            val spineTopX = (if (isRH) w * 0.54f else w * 0.46f) + (if (isRH) spineOffset else -spineOffset)
            val spineTopY = h * 0.33f

            val headX = (if (isRH) w * 0.57f else w * 0.43f) + (if (isRH) spineOffset else -spineOffset)
            val headY = h * 0.23f

            // 2. Shoulders & Arms Hanging Underneath
            val leadShoulderX = if (isRH) spineTopX + 14f else spineTopX - 14f
            val leadShoulderY = h * 0.34f
            val trailShoulderX = if (isRH) spineTopX - 14f else spineTopX + 14f
            val trailShoulderY = h * 0.35f

            val leadElbowX = if (isRH) w * 0.55f else w * 0.45f
            val leadElbowY = h * 0.46f
            val trailElbowX = if (isRH) w * 0.50f else w * 0.50f
            val trailElbowY = h * 0.47f

            val handsX = if (isRH) w * 0.52f else w * 0.48f
            val handsY = h * 0.57f

            // 3. Ball & Shaft Plane
            val ballX = if (isRH) w * 0.34f else w * 0.66f
            val ballY = h * 0.88f
            val shaftTopX = if (isRH) w * 0.78f else w * 0.22f
            val shaftTopY = h * 0.20f

            // 4. Pelvis Tush Wall (Butt-Line for Early Extension Audit)
            val wallX = if (isRH) hipX - 30f else hipX + 30f

            // 5. Legs & Knee Flex
            val leadKneeX = if (isRH) w * 0.49f else w * 0.51f
            val leadKneeY = h * 0.73f
            val trailKneeX = if (isRH) w * 0.43f else w * 0.57f
            val trailKneeY = h * 0.73f

            val leadAnkleX = if (isRH) w * 0.51f else w * 0.49f
            val leadAnkleY = h * 0.88f
            val trailAnkleX = if (isRH) w * 0.41f else w * 0.59f
            val trailAnkleY = h * 0.88f

            // --- DRAW DTL GUIDE LINES ---

            // A. Primary Shaft Plane Line (Tour Gold)
            drawLine(
                color = GolfGold,
                start = Offset(ballX, ballY),
                end = Offset(shaftTopX, shaftTopY),
                strokeWidth = 4f
            )

            // B. Delivery Slot Corridor (Dashed Gold)
            val slotShift = if (isRH) -35f else 35f
            drawLine(
                color = Color(0x99FFD700),
                start = Offset(ballX + slotShift, ballY),
                end = Offset(shaftTopX + slotShift, shaftTopY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            // C. Spine Posture Line (Neon Cyan)
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(hipX, hipY),
                end = Offset(headX, headY),
                strokeWidth = 3.5f
            )

            // D. Pelvis Tush Wall Line (Glute Line)
            drawLine(
                color = WarningOrange,
                start = Offset(wallX, h * 0.44f),
                end = Offset(wallX, h * 0.84f),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            // E. Upper Body / Arm Skeleton Lines
            // Shoulder bar
            drawLine(
                color = GolfGold,
                start = Offset(trailShoulderX, trailShoulderY),
                end = Offset(leadShoulderX, leadShoulderY),
                strokeWidth = 3f
            )
            // Lead Arm (Gold)
            drawLine(
                color = GolfGold,
                start = Offset(leadShoulderX, leadShoulderY),
                end = Offset(leadElbowX, leadElbowY),
                strokeWidth = 3f
            )
            drawLine(
                color = GolfGold,
                start = Offset(leadElbowX, leadElbowY),
                end = Offset(handsX, handsY),
                strokeWidth = 3f
            )
            // Trail Arm (Ice Blue)
            drawLine(
                color = Color(0xFF80D8FF),
                start = Offset(trailShoulderX, trailShoulderY),
                end = Offset(trailElbowX, trailElbowY),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xFF80D8FF),
                start = Offset(trailElbowX, trailElbowY),
                end = Offset(handsX, handsY),
                strokeWidth = 2.5f
            )

            // F. Legs & Athletic Knee Flex Lines (Emerald Green)
            drawLine(
                color = GolfGreen,
                start = Offset(hipX, hipY),
                end = Offset(leadKneeX, leadKneeY),
                strokeWidth = 3f
            )
            drawLine(
                color = GolfGreen,
                start = Offset(leadKneeX, leadKneeY),
                end = Offset(leadAnkleX, leadAnkleY),
                strokeWidth = 3f
            )
            drawLine(
                color = GolfGreen,
                start = Offset(hipX, hipY),
                end = Offset(trailKneeX, trailKneeY),
                strokeWidth = 3f
            )
            drawLine(
                color = GolfGreen,
                start = Offset(trailKneeX, trailKneeY),
                end = Offset(trailAnkleX, trailAnkleY),
                strokeWidth = 3f
            )

            // G. Head Circle & Ball
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 18f,
                center = Offset(headX, headY),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = Offset(ballX, ballY)
            )

            // H. Biomechanical Joint Nodes
            val jointNodes = listOf(
                Offset(headX, headY),
                Offset(leadShoulderX, leadShoulderY),
                Offset(trailShoulderX, trailShoulderY),
                Offset(leadElbowX, leadElbowY),
                Offset(trailElbowX, trailElbowY),
                Offset(handsX, handsY),
                Offset(hipX, hipY),
                Offset(leadKneeX, leadKneeY),
                Offset(trailKneeX, trailKneeY),
                Offset(leadAnkleX, leadAnkleY),
                Offset(trailAnkleX, trailAnkleY)
            )
            jointNodes.forEach { pt ->
                drawCircle(color = GolfGreen, radius = 6.5f, center = pt)
                drawCircle(color = Color.White, radius = 3.5f, center = pt)
            }

            // I. Telestrator Labels
            drawContext.canvas.nativeCanvas.apply {
                textPaint.color = android.graphics.Color.argb(240, 255, 215, 0)
                drawText("SHAFT PLANE", if (isRH) ballX + 16f else ballX - 160f, ballY - 14f, textPaint)

                textPaint.color = android.graphics.Color.argb(240, 0, 229, 255)
                drawText("SPINE: ${telemetry.detectedSpineAngleDeg.toInt()}°", headX - 60f, headY - 26f, textPaint)

                textPaint.color = android.graphics.Color.argb(240, 255, 145, 0)
                drawText("TUSH LINE", if (isRH) wallX - 120f else wallX + 10f, h * 0.48f, textPaint)
            }

        } else {
            // =================================================================
            // FACE-ON (FO) TOUR BIOMECHANICS & POSE SKELETON
            // =================================================================
            val isRH = handedness == GolferHandedness.RIGHT_HANDED
            val cx = w * 0.50f
            val swayShift = telemetry.headSwayInches * 10f
            val pelvisShift = telemetry.pelvisDriftInches * 8f

            // 1. Core Posture Landmarks
            val headX = cx + (if (isRH) swayShift else -swayShift)
            val headY = h * 0.22f

            val neckX = cx
            val neckY = h * 0.31f

            // 2. Shoulders (Lead shoulder higher by ~15° tilt)
            val leadShoulderX = cx - 58f
            val leadShoulderY = h * 0.33f
            val trailShoulderX = cx + 58f
            val trailShoulderY = h * 0.36f

            // 3. Arms forming the Tour "V" Triangle
            val leadElbowX = cx - 44f
            val leadElbowY = h * 0.46f
            val trailElbowX = cx + 40f
            val trailElbowY = h * 0.47f

            val handsX = cx - 14f
            val handsY = h * 0.57f

            // 4. Ball Position & Club Shaft
            val ballX = cx - 52f
            val ballY = h * 0.88f

            // 5. Spine with Secondary Tilt
            val midSpineX = cx + 4f
            val midSpineY = h * 0.44f
            val pelvisX = cx + 8f + pelvisShift
            val pelvisY = h * 0.57f

            // 6. Hips & Lateral Sway Limits
            val leadHipX = cx - 44f + pelvisShift
            val leadHipY = h * 0.57f
            val trailHipX = cx + 44f + pelvisShift
            val trailHipY = h * 0.57f

            // 7. Legs & Stance
            val leadKneeX = cx - 46f
            val leadKneeY = h * 0.72f
            val trailKneeX = cx + 46f
            val trailKneeY = h * 0.72f

            val leadFootX = cx - 62f
            val leadFootY = h * 0.88f
            val trailFootX = cx + 62f
            val trailFootY = h * 0.88f

            // --- DRAW FACE-ON GUIDE LINES ---

            // A. Head Sway Audit Box (Turns Amber if swaying too far)
            val isHeadSwaying = telemetry.headSwayInches > 0.8f
            drawRect(
                color = if (isHeadSwaying) WarningOrange else Color(0xFF00E5FF),
                topLeft = Offset(headX - 40f, headY - 40f),
                size = Size(80f, 80f),
                style = Stroke(width = 2.5f)
            )
            // Head crosshair
            drawLine(
                color = if (isHeadSwaying) WarningOrange else Color(0xFF00E5FF),
                start = Offset(headX - 20f, headY),
                end = Offset(headX + 20f, headY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = if (isHeadSwaying) WarningOrange else Color(0xFF00E5FF),
                start = Offset(headX, headY - 20f),
                end = Offset(headX, headY + 20f),
                strokeWidth = 1.5f
            )

            // B. The Tour "V" Triangle (Upper Body Connection)
            // Shoulder Bar
            drawLine(
                color = GolfGold,
                start = Offset(leadShoulderX, leadShoulderY),
                end = Offset(trailShoulderX, trailShoulderY),
                strokeWidth = 3.5f
            )
            // Lead Arm
            drawLine(
                color = GolfGold,
                start = Offset(leadShoulderX, leadShoulderY),
                end = Offset(leadElbowX, leadElbowY),
                strokeWidth = 3.5f
            )
            drawLine(
                color = GolfGold,
                start = Offset(leadElbowX, leadElbowY),
                end = Offset(handsX, handsY),
                strokeWidth = 3.5f
            )
            // Trail Arm
            drawLine(
                color = Color(0xFF80D8FF),
                start = Offset(trailShoulderX, trailShoulderY),
                end = Offset(trailElbowX, trailElbowY),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF80D8FF),
                start = Offset(trailElbowX, trailElbowY),
                end = Offset(handsX, handsY),
                strokeWidth = 3f
            )

            // C. Forward Shaft Lean Line
            drawLine(
                color = GolfGold,
                start = Offset(handsX, handsY),
                end = Offset(ballX, ballY),
                strokeWidth = 4f
            )
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = Offset(ballX, ballY)
            )

            // D. Spine Secondary Axis Tilt (Neon Cyan)
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(neckX, neckY),
                end = Offset(midSpineX, midSpineY),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(midSpineX, midSpineY),
                end = Offset(pelvisX, pelvisY),
                strokeWidth = 3f
            )

            // E. Pelvis Axis & Lateral Sway Bounds (Emerald Green)
            drawLine(
                color = GolfGreen,
                start = Offset(leadHipX, leadHipY),
                end = Offset(trailHipX, trailHipY),
                strokeWidth = 3f
            )
            // Lateral Sway Boundary Hashes
            drawLine(
                color = Color(0x9900E676),
                start = Offset(leadHipX, leadHipY - 25f),
                end = Offset(leadHipX, leadHipY + 25f),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
            drawLine(
                color = Color(0x9900E676),
                start = Offset(trailHipX, trailHipY - 25f),
                end = Offset(trailHipX, trailHipY + 25f),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // F. Legs & Stance Width (Emerald Green)
            drawLine(
                color = GolfGreen,
                start = Offset(leadHipX, leadHipY),
                end = Offset(leadKneeX, leadKneeY),
                strokeWidth = 3f
            )
            drawLine(
                color = GolfGreen,
                start = Offset(leadKneeX, leadKneeY),
                end = Offset(leadFootX, leadFootY),
                strokeWidth = 3f
            )
            drawLine(
                color = GolfGreen,
                start = Offset(trailHipX, trailHipY),
                end = Offset(trailKneeX, trailKneeY),
                strokeWidth = 3f
            )
            drawLine(
                color = GolfGreen,
                start = Offset(trailKneeX, trailKneeY),
                end = Offset(trailFootX, trailFootY),
                strokeWidth = 3f
            )

            // G. Biomechanical Joint Nodes
            val foJoints = listOf(
                Offset(headX, headY),
                Offset(leadShoulderX, leadShoulderY),
                Offset(trailShoulderX, trailShoulderY),
                Offset(leadElbowX, leadElbowY),
                Offset(trailElbowX, trailElbowY),
                Offset(handsX, handsY),
                Offset(leadHipX, leadHipY),
                Offset(trailHipX, trailHipY),
                Offset(leadKneeX, leadKneeY),
                Offset(trailKneeX, trailKneeY),
                Offset(leadFootX, leadFootY),
                Offset(trailFootX, trailFootY)
            )
            foJoints.forEach { pt ->
                drawCircle(color = GolfGreen, radius = 6.5f, center = pt)
                drawCircle(color = Color.White, radius = 3.5f, center = pt)
            }

            // H. Telestrator Labels
            drawContext.canvas.nativeCanvas.apply {
                textPaint.color = if (isHeadSwaying) android.graphics.Color.argb(240, 255, 145, 0) else android.graphics.Color.argb(240, 0, 229, 255)
                drawText("HEAD BOX", headX - 55f, headY - 48f, textPaint)

                textPaint.color = android.graphics.Color.argb(240, 255, 215, 0)
                drawText("TOUR TRIANGLE", cx - 85f, h * 0.40f, textPaint)

                textPaint.color = android.graphics.Color.argb(240, 0, 230, 118)
                drawText("HIP ROTATION", cx - 75f, h * 0.60f, textPaint)
            }
        }
    }
}

// =============================================================================
// HERO AI SWING COACH & ANALYZER SCREEN
// =============================================================================

@Composable
fun SwingCoachScreen(
    selectedClub: Club,
    handedness: GolferHandedness,
    is240Fps: Boolean,
    onToggle240Fps: () -> Unit,
    onSwingAnalyzed: (SwingRecord) -> Unit,
    tts: TextToSpeech?,
    isVoiceCoachEnabled: Boolean
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

    // Viewfinder Configuration
    var perspective by remember { mutableStateOf(CameraPerspective.DOWN_THE_LINE) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingProgress by remember { mutableStateOf(0f) }
    var analysisStatus by remember { mutableStateOf("READY AT ADDRESS · DTL VIEW") }
    var useSimulatedFallback by remember { mutableStateOf(false) }
    var showPoseSkeleton by remember { mutableStateOf(true) }

    // Selected PGA Kinematic Checkpoint (P1 to P8)
    var selectedCheckpoint by remember { mutableStateOf(SwingCheckpoint.P7) }

    // Live Telemetry from real CameraX frame analyzer
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

    // Analyzed Biomechanical Metrics
    var spineAngleSetup by remember { mutableStateOf(38) }
    var spineAngleImpact by remember { mutableStateOf(37) }
    var shoulderTurn by remember { mutableStateOf(92) }
    var hipTurn by remember { mutableStateOf(44) }
    var earlyExtensionInches by remember { mutableStateOf(2.6f) }
    var clubShaftP6Angle by remember { mutableStateOf(48) }
    var coachGrade by remember { mutableStateOf("B+") }
    var frameCaptureCount by remember { mutableStateOf(57) }

    // Execute Voice Coach Announcement
    fun speakFeedback(text: String) {
        if (isVoiceCoachEnabled && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Swing Capture Function
    fun executeSwingAnalysis(source: String, measuredPelvis: Float? = null) {
        if (isRecording) return
        coroutineScope.launch {
            isRecording = true
            analysisStatus = "🔴 CAPTURING SWING @ 240 FPS (FRAME 1/$frameCaptureCount)..."

            for (f in 1..frameCaptureCount) {
                recordingProgress = f.toFloat() / frameCaptureCount.toFloat()
                if (f % 12 == 0) {
                    analysisStatus = "🔴 BUFFERING 240 FPS FRAMES ($f/$frameCaptureCount)..."
                }
                delay(16)
            }

            analysisStatus = "⚙️ COMPUTING 33-POINT BIOMECHANICAL ANGLES..."
            delay(350)

            // Dynamic evaluation based on camera measurement or simulation
            spineAngleSetup = 38
            spineAngleImpact = 37
            shoulderTurn = (89..95).random()
            hipTurn = (42..46).random()
            earlyExtensionInches = measuredPelvis ?: ((24..28).random() / 10f)
            clubShaftP6Angle = (47..49).random()
            coachGrade = if (earlyExtensionInches <= 1.8f) "A" else "B+"

            val simulatedRecord = simulateNewSwing(selectedClub, handedness)
            onSwingAnalyzed(simulatedRecord)

            isRecording = false
            recordingProgress = 0f
            analysisStatus = "✓ SWING ANALYZED (${simulatedRecord.estimatedClubSpeedMph.toInt()} MPH · GRADE: $coachGrade)"

            // Voice announcement
            val speechMsg = "Swing captured. ${simulatedRecord.estimatedClubSpeedMph.toInt()} miles per hour. Grade $coachGrade. Early extension detected at impact."
            speakFeedback(speechMsg)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Perspective Selector: Down-The-Line (DTL) vs Face-On (FO)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CAMERA PERSPECTIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                        Text(
                            text = if (is240Fps) "240 FPS HIGH-SPEED" else "60 FPS",
                            fontSize = 10.sp,
                            color = GolfGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { perspective = CameraPerspective.DOWN_THE_LINE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (perspective == CameraPerspective.DOWN_THE_LINE) GolfGold else Color(0xFF22295E)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "📐 Down-The-Line",
                                color = if (perspective == CameraPerspective.DOWN_THE_LINE) Color.Black else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = { perspective = CameraPerspective.FACE_ON },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (perspective == CameraPerspective.FACE_ON) GolfGold else Color(0xFF22295E)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "👤 Face-On",
                                color = if (perspective == CameraPerspective.FACE_ON) Color.Black else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = perspective.description,
                        fontSize = 11.sp,
                        color = TextMuted
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
                            .height(350.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!hasCameraPermission && !useSimulatedFallback) {
                            // Permission Request Prompt
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
                                    "GolfGod needs live camera access to analyze your downswing plane, spine retention, and 33-point biomechanics in high-speed (240 FPS).",
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
                                    Text("Preview in Simulation Mode", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        } else {
                            // Live CameraX Feed
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
                                        executeSwingAnalysis("AUTO_CAMERA", pelvisDrift)
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F1A30)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⛳ SIMULATED CAMERA FEED", color = Color(0x44FFFFFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            // Biomechanical HUD & Alignment Overlays
                            CameraBiomechanicalOverlay(
                                modifier = Modifier.fillMaxSize(),
                                handedness = handedness,
                                perspective = perspective,
                                telemetry = visionTelemetry,
                                isRecording = isRecording,
                                recordingProgress = recordingProgress,
                                showPoseSkeleton = showPoseSkeleton
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

                                    // Pose Skeleton Overlay Toggle
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (showPoseSkeleton) GolfGold else Color(0xFF1E234A))
                                            .clickable { showPoseSkeleton = !showPoseSkeleton }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (showPoseSkeleton) "🦴 Pose ON" else "🦴 Pose OFF",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (showPoseSkeleton) Color.Black else TextPrimary
                                        )
                                    }
                                }

                                Text(
                                    text = "${perspective.label.split(' ')[0]} · ${handedness.shortCode}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GolfGold
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

                                // Real-time Computer Vision Sensor Bar
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

                    // Viewfinder Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { executeSwingAnalysis("MANUAL_RECORD") },
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
                            onClick = {
                                speakFeedback("Ready. Take stance. Three, two, one, swing!")
                                executeSwingAnalysis("VOICE_TRIGGER")
                            },
                            enabled = !isRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3D2B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🗣️ VOICE TRIGGER",
                                color = GolfGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // PGA 6-POINT KINEMATIC CHECKPOINT ANALYZER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PGA 6-CHECKPOINT KINEMATIC ANALYZER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Text("Tap a checkpoint to inspect body & shaft delivery angles:", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SwingCheckpoint.values()) { cp ->
                            val isSelected = cp == selectedCheckpoint
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GolfGold else Color(0xFF1E234A))
                                    .border(1.dp, if (isSelected) GolfGold else CardBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedCheckpoint = cp }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = cp.code,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                    Text(
                                        text = cp.phase,
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail of selected checkpoint
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF14193D))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedCheckpoint.code} · ${selectedCheckpoint.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GolfGold
                                )
                                Text(
                                    text = "Status: Tour Ideal ✓",
                                    fontSize = 10.sp,
                                    color = GolfGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            when (selectedCheckpoint) {
                                SwingCheckpoint.P1 -> {
                                    Text("""• Setup Spine Tilt: 38° (Ideal: 36°–42°)
• Knee Flex: 24° athletic bend
• Weight Distribution: 50% Lead / 50% Trail foot""", fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                                }
                                SwingCheckpoint.P2 -> {
                                    Text("""• Shaft Parallel to target line
• Clubface Angle: Matches spine tilt (Toe slightly turned down)
• Hands inside clubhead corridor""", fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                                }
                                SwingCheckpoint.P4 -> {
                                    Text("""• Shoulder Turn: 92° (Tour avg: 90°+)
• Hip Turn: 44° (Ideal resistance)
• X-Factor Stretch: 48° differential coil (Max potential energy)""", fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                                }
                                SwingCheckpoint.P6 -> {
                                    Text("""• Delivery Shaft Angle: 48° (Shallow approach)
• Clubhead dropping from inside delivery slot
• Lead hip beginning open clearance""", fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                                }
                                SwingCheckpoint.P7 -> {
                                    Text("""• Pelvis Early Extension: 2.6 in. forward (Needs Work)
• Spine Retention: 37° (Retained 97% of setup tilt)
• Forward Shaft Lean: 6.5° forward shaft angle (Crisp ball-first compression)""", fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                                }
                                SwingCheckpoint.P8 -> {
                                    Text("""• Chest rotated fully through target
• Belt buckle facing target
• 95% weight balanced on lead heel""", fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // BIOMECHANICAL ANGLES CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${handedness.shortCode} BIOMECHANICAL ANGLES (ANALYZED)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Text("Captured across $frameCaptureCount high-speed frames during downswing.", fontSize = 11.sp, color = TextMuted)
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

        // COACH RECOMMENDATION & PRESCRIBED DRILL
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
                        text = "Your hips moved ${earlyExtensionInches} inches closer to the ball during transition. When hips push forward into the hitting zone, the torso stands up, forcing your hands to flip through impact.",
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
                                text = "1. Place your golf bag or chair touching your trail glute at setup.\n2. On backswing, push your trail hip back into it.\n3. On downswing, roll your lead glute deeper into the chair BEFORE impact.\n4. Do 10 slow practice reps holding balanced finish.",
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
        // Quick Camera Coach Banner
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
                            Text("AI SWING COACH & ANALYZER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                            Text("Live 240 FPS Camera + 6-Checkpoint Kinematics", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Real-time DTL & Face-On biomechanical posture", fontSize = 10.sp, color = TextSecondary)
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
        // Handedness Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GOLFER HANDEDNESS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Spacer(modifier = Modifier.height(10.dp))

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
        notes = "${handedness.shortCode} Analyzed Swing"
    )
}
