package com.golfgod.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfgod.core.model.*
import com.golfgod.core.physics.BallFlightSimulator
import com.golfgod.phone.theme.*
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

                        // S24, Handedness & Galaxy Watch 5 Pairing Badges
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
                                text = "S24 240fps",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GolfGold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2A200B))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                            Text(
                                text = "Watch 5",
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
                    }
                )
                PhoneTab.COACH -> VideoCoachScreen(
                    selectedClub = selectedClub,
                    handedness = handedness,
                    is240Fps = is240FpsEnabled,
                    onToggle240Fps = { is240FpsEnabled = !is240FpsEnabled }
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
    onSimulateSwing: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricColumn(
                            title = "TEMPO",
                            value = "${lastSwing?.tempoRatio ?: 3.0} : 1",
                            subtitle = "${lastSwing?.backswingTimeMs ?: 720}ms / ${lastSwing?.downswingTimeMs ?: 240}ms",
                            color = GolfGreen
                        )
                        MetricColumn(
                            title = "CLUB SPEED",
                            value = "${lastSwing?.estimatedClubSpeedMph?.toInt() ?: 102} MPH",
                            subtitle = "Hand: ${lastSwing?.peakHandSpeedMph?.toInt() ?: 24} MPH",
                            color = GolfGold
                        )
                        MetricColumn(
                            title = "EST. CARRY",
                            value = "${ballFlight.estimatedCarryYards} YDS",
                            subtitle = "Total: ${ballFlight.estimatedTotalYards} yds",
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = CardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ball Speed: ${ballFlight.estimatedBallSpeedMph} mph", fontSize = 12.sp, color = TextSecondary)
                        Text("Launch: ${ballFlight.estimatedLaunchAngleDeg}°", fontSize = 12.sp, color = TextSecondary)
                        Text("Spin: ${ballFlight.estimatedBackspinRpm} rpm", fontSize = 12.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSimulateSwing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22295E)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simulate ${handedness.shortCode} Swing ⛳", color = GolfGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("SESSION LOG (${swingsList.size} SWINGS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }

        items(swingsList) { swing ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${swing.clubCode} · ${swing.tempoRatio} : 1", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${swing.backswingTimeMs}ms back / ${swing.downswingTimeMs}ms down", fontSize = 11.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${swing.estimatedClubSpeedMph.toInt()} MPH", fontWeight = FontWeight.Bold, color = GolfGold)
                        Text("Score: ${swing.qualityScore}/100", fontSize = 11.sp, color = GolfGreen)
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
fun VideoCoachScreen(selectedClub: Club, handedness: GolferHandedness, is240Fps: Boolean, onToggle240Fps: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AI VIDEO SWING COACH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Text("${handedness.label} Alignment Model", fontSize = 14.sp, color = TextSecondary)
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

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1434))
                    .border(1.dp, GolfGold, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📹", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Samsung S24 Ultra HD Viewfinder Ready", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${handedness.label} Down-The-Line · 240fps Active", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1B3D2B))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("33 POSE JOINTS", color = GolfGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2A200B))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("BALL TRACKING ACTIVE", color = GolfGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                    Text("${handedness.shortCode} BIOMECHANICAL ANGLES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GolfGold)
                    Text("At 240fps, your downswing is captured across 57 distinct frames.", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    AngleRow("Spine Angle Retention", "38° setup → 37° impact", "STABLE ✓", GolfGreen)
                    AngleRow("Shoulder Turn at Top", "92°", "EXCELLENT ✓", GolfGreen)
                    AngleRow("Hip Turn at Top", "44°", "IDEAL (X-Factor: 48°)", GolfGreen)
                    AngleRow("Early Extension (Pelvis Drift)", "2.8 inches forward", "NEEDS WORK ⚠️", WarningOrange)
                    AngleRow("Club Shaft Angle at P6", "48° on-plane", "ON PLANE ✓", GolfGreen)
                    AngleRow("Head Movement (Sway)", "0.6 inches", "SOLID ✓", GolfGreen)
                }
            }
        }

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
                        Text("Grade: B+", fontSize = 13.sp, fontWeight = FontWeight.Black, color = GolfGold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Primary Fault: Early Extension at Impact", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your hips moved 2.8 inches closer to the ball during the downswing transition. This stands your torso up, forces you to flip your hands at the ball, and causes thin shots or push blocks.",
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
