package com.fasttaxi.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF1264E8)
private val Navy = Color(0xFF10234D)
private val Pale = Color(0xFFF2F6FF)
private val Green = Color(0xFF16A34A)

data class Trip(
    val date: String,
    val km: Double,
    val fare: Int,
    val duration: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FastTaxiApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastTaxiApp() {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    var role by remember { mutableStateOf("Driver") }
    var tracking by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var distanceMeters by remember { mutableDoubleStateOf(0.0) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var startTime by remember { mutableLongStateOf(0L) }
    var lastGps by remember { mutableStateOf("GPS မချိတ်ဆက်ရသေးပါ") }
    var baseFare by remember { mutableIntStateOf(5000) }
    var perKm by remember { mutableIntStateOf(1000) }
    var demand by remember { mutableIntStateOf(0) }
    var trips by remember { mutableStateOf(listOf<Trip>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val callback = remember(tracking) {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val current = result.lastLocation ?: return
                lastGps = "GPS ရရှိနေပါသည် • ±${current.accuracy.toInt()} m"
                if (current.accuracy <= 35f) {
                    val previous = lastLocation
                    if (tracking && previous != null) {
                        val delta = previous.distanceTo(current)
                        if (delta in 2f..250f) distanceMeters += delta.toDouble()
                    }
                    lastLocation = current
                }
            }
        }
    }

    DisposableEffect(tracking, permissionGranted) {
        if (tracking && permissionGranted) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateDistanceMeters(3f)
                .build()
            try { fused.requestLocationUpdates(request, callback, context.mainLooper) }
            catch (_: SecurityException) { lastGps = "Location permission လိုအပ်ပါသည်" }
        }
        onDispose { fused.removeLocationUpdates(callback) }
    }

    val km = distanceMeters / 1000.0
    val fare = baseFare + (km * perKm).toInt() + demand
    val money = "%,d MMK".format(Locale.US, fare)
    val dateFormat = remember { SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()) }

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Blue, background = Pale, surface = Color.White)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("FAST TAXI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text("Your Ride, Our Priority", color = Color(0xFFDCE8FF), fontSize = 12.sp)
                        }
                    },
                    actions = { Text("● LIVE ", color = Color(0xFF9AF3B1), modifier = Modifier.padding(16.dp)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue)
                )
            },
            containerColor = Pale
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("အသုံးပြုသူ အမျိုးအစား", color = Navy, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = role == "Driver", onClick = { role = "Driver" }, label = { Text("🚕 Driver") })
                                FilterChip(selected = role == "Admin", onClick = { role = "Admin" }, label = { Text("🛠 Admin") })
                            }
                            Text(
                                if (role == "Driver") "ယာဉ်မောင်း Dashboard" else "Admin Dashboard",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy
                            )
                            Text(if (role == "Driver") "ယာဉ်မောင်း • 4D 9208" else "စီမံခန့်ခွဲသူ • ခရီးစဉ်အကျဉ်းချုပ်", color = Color.Gray)
                        }
                    }
                }

                if (role == "Driver") {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("GPS ခရီးစဉ်", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                                Box(
                                    Modifier.fillMaxWidth().height(130.dp).background(
                                        Color(0xFFE1F4E9), RoundedCornerShape(14.dp)
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📍", fontSize = 30.sp)
                                        Text(if (tracking) "ခရီးစဉ်ကို တိုင်းတာနေသည်" else "GPS ခရီးစဉ် အဆင်သင့်", color = Navy, fontWeight = FontWeight.SemiBold)
                                        Text(lastGps, color = Color.DarkGray, fontSize = 12.sp)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Metric("ခရီးမိုင်", "%.2f km".format(Locale.US, km))
                                    Metric("အချိန်", if (tracking) elapsed(startTime) else "00:00")
                                }
                                if (!tracking) {
                                    Button(
                                        onClick = {
                                            if (!permissionGranted) {
                                                permissionLauncher.launch(arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                ))
                                            } else {
                                                distanceMeters = 0.0
                                                lastLocation = null
                                                startTime = System.currentTimeMillis()
                                                tracking = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("ခရီးစဉ် စတင်မည်") }
                                } else {
                                    Button(
                                        onClick = {
                                            tracking = false
                                            val duration = elapsed(startTime)
                                            trips = listOf(Trip(dateFormat.format(Date()), km, fare, duration)) + trips
                                            lastLocation = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("ခရီးစဉ် ပြီးဆုံးမည်") }
                                }
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("ယာဉ်ခ တွက်ချက်မှု", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                                FareLine("အခြေခံခ", "%,d MMK".format(Locale.US, baseFare))
                                FareLine("ခရီးမိုင် (${ "%.2f".format(Locale.US, km) } km)", "%,d MMK".format(Locale.US, (km * perKm).toInt()))
                                FareLine("Demand", "%,d MMK".format(Locale.US, demand))
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("စုစုပေါင်း", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                                    Text(money, fontWeight = FontWeight.Bold, color = Blue, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("ယာဉ်ခနှုန်းထား သတ်မှတ်ရန်", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                                NumberSetting("အခြေခံခ (MMK)", baseFare) { baseFare = it }
                                NumberSetting("တစ်ကီလိုမီတာခ (MMK)", perKm) { perKm = it }
                                NumberSetting("Demand (MMK)", demand) { demand = it }
                                Text("ဤနှုန်းထားများသည် စမ်းသပ်ရန် Default တန်ဖိုးများဖြစ်ပြီး ပြင်ဆင်နိုင်ပါသည်။", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ခရီးစဉ် အကျဉ်းချုပ်", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Metric("ခရီးစဉ်", "${trips.size}")
                                    Metric("စုစုပေါင်းဝင်ငွေ", "%,d MMK".format(Locale.US, trips.sumOf { it.fare }))
                                }
                            }
                        }
                    }
                }

                item { Text("နောက်ဆုံး ခရီးစဉ်များ", color = Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                if (trips.isEmpty()) {
                    item { Text("ခရီးစဉ်မှတ်တမ်း မရှိသေးပါ။", color = Color.Gray) }
                } else {
                    items(trips.take(20)) { trip ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(trip.date, color = Navy, fontWeight = FontWeight.SemiBold)
                                    Text("${"%.2f".format(Locale.US, trip.km)} km • ${trip.duration}", color = Color.Gray, fontSize = 12.sp)
                                }
                                Text("%,d MMK".format(Locale.US, trip.fare), color = Blue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Prototype version • Data သည် ယခုဖုန်းထဲတွင်သာ ယာယီသိမ်းထားပါသည်။",
                        color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Metric(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Navy, fontWeight = FontWeight.Bold, fontSize = 19.sp)
    }
}

@Composable
fun FareLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.DarkGray)
        Text(value, color = Navy, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun NumberSetting(label: String, value: Int, onValue: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter(Char::isDigit).take(9)
            onValue(text.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

fun elapsed(start: Long): String {
    if (start == 0L) return "00:00"
    val seconds = ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
