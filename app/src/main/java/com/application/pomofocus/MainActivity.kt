package com.application.pomofocus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.application.pomofocus.ui.theme.BackBreak
import com.application.pomofocus.ui.theme.BackPomodoro
import com.application.pomofocus.ui.theme.FrontBreak
import com.application.pomofocus.ui.theme.FrontPomodoro
import com.application.pomofocus.ui.theme.ShadowBreak
import com.application.pomofocus.ui.theme.ShadowPomodoro
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material3.Checkbox // Baris baru
import androidx.compose.material3.CheckboxDefaults // Baris baru
import androidx.compose.material3.OutlinedTextField // Baris baru
import androidx.compose.material3.OutlinedTextFieldDefaults // Baris baru
import androidx.compose.ui.text.style.TextDecoration // Baris baru
import androidx.compose.foundation.lazy.LazyColumn // Baris baru
import androidx.compose.foundation.lazy.items // Baris baru
import androidx.compose.ui.text.input.ImeAction // Baris baru
import androidx.compose.foundation.text.KeyboardActions // Baris baru
import androidx.compose.foundation.text.KeyboardOptions // Baris baru
import androidx.compose.material3.Card // Baris baru
import androidx.compose.material3.CardDefaults // Baris baru
import androidx.compose.material3.Icon // Baris baru
import androidx.compose.material3.IconButton // Baris baru
import androidx.compose.foundation.rememberScrollState // <<< BARIS BARU INI
import androidx.compose.foundation.verticalScroll // <<< BARIS BARU INI

import java.util.UUID // Baris baru

import androidx.compose.runtime.mutableStateListOf // Baris baru: Pastikan ini ada

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Pomodoro Notification"
        val descriptionText = "Channel for Pomodoro Timer"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("pomodoro_channel_id", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun showNotification(context: Context, title: String, message: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent =
        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val builder = NotificationCompat.Builder(context, "pomodoro_channel_id")
        .setSmallIcon(R.drawable.app_icon)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notify(1, builder.build())
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        setContent {
            val navController = rememberNavController()
            val systemUiController = rememberSystemUiController()
            NavHost(navController = navController, startDestination = "pomodoro") {
                composable("pomodoro") {
                    systemUiController.setStatusBarColor(BackPomodoro)
                    PomodoroScreen(navController)
                }
                composable("break") {
                    systemUiController.setStatusBarColor(BackBreak)
                    BreakScreen(navController)
                }
            }
        }
    }
}

val font = FontFamily(Font(R.font.arialroundedmt))
const val DEFAULT_POMODORO_TIME = 25
const val DEFAULT_BREAK_TIME = 5

@Composable
fun PomodoroScreen(navController: NavController) {
    var isStartPressed by remember { mutableStateOf(false) }
    var totalTime by remember { mutableIntStateOf(DEFAULT_POMODORO_TIME) }
    var remainingTime by remember { mutableIntStateOf(totalTime) }
    var showDialog by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(1f) }
    val context = LocalContext.current

    // --- MULAI TAMBAHAN UNTUK TASK ---
    val tasks = remember { mutableStateListOf<Task>() }
    var newTaskText by remember { mutableStateOf("") }
    // --- AKHIR TAMBAHAN ---

    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.alarm)
    }

    LaunchedEffect(isStartPressed) {
        if (isStartPressed) {
            while (remainingTime > 0 && isStartPressed) {
                progress = remainingTime.toFloat() / totalTime.toFloat()
                delay(1000L)
                remainingTime -= 1
            }
            if (remainingTime <= 0) {
                isStartPressed = false
                showNotification(context, "Pomodoro Complete!", "Take a well-deserved break!")
                CoroutineScope(Dispatchers.Main).launch {
                    mediaPlayer.start()
                }
                navController.navigate("break")
            }
        }
    }

    if (showDialog) {
        TimePicker(
            currentTime = remainingTime,
            backColor = BackPomodoro,
            maxTime = 60,
            onTimeSelected = { newTime ->
                totalTime = newTime
                remainingTime = newTime
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackPomodoro)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(37.dp)
                    .padding(end = 5.dp),
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "",
            )
            Text(
                text = "Bisafokus",
                fontSize = 35.sp,
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Box( // Ini adalah Box dengan background FrontPomodoro
            modifier = Modifier
                .fillMaxSize() // Ini tetap fillMaxSize untuk Box utama
                .padding(start = 20.dp, end = 20.dp, top = 90.dp, bottom = 10.dp) // padding Box utama
                .background(FrontPomodoro, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ){
            val scrollState = rememberScrollState() // <<< BARIS BARU: Untuk mengelola scroll state

            Column( // Column utama yang akan bisa di-scroll
                modifier = Modifier
                    .fillMaxSize() // <<< UBAH INI: Agar Column ini mengisi Box dan bisa di-scroll
                    .padding(horizontal = 20.dp, vertical = 20.dp) // padding internal Column
                    .verticalScroll(scrollState), // <<< INI KUNCI SCROLL LAYAR PENUH
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top // Tetap Top, kita kelola spasi manual
            ){
                // Tombol Pomodoro / Break (TETAP DI ATAS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(color = ShadowPomodoro, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pomodoro",
                            fontFamily = font,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                navController.navigate("break")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Break",
                            fontSize = 20.sp,
                            fontFamily = font,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Bagian Timer (TETAP DI TENGAH ATAS)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                )
                {
                    TimerClock(progress = progress, backColor = BackPomodoro)
                    Text(
                        text = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60),
                        fontSize = 100.sp,
                        fontFamily = font,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable {
                            showDialog = true
                            isStartPressed = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp)) // Spasi antara timer dan tombol START

                // Tombol START/PAUSE (INI POSISI BARUNYA: DI BAWAH TIMER)
                TimerButton(
                    context = context,
                    backColor = BackPomodoro,
                    text = if (isStartPressed) "PAUSE" else "START",
                    isPressed = isStartPressed,
                    onClick = { isStartPressed = !isStartPressed }
                )

                Spacer(modifier = Modifier.height(32.dp)) // Spasi antara Tombol START dan Bagian Task

                // --- AWAL BAGIAN INPUT DAN DAFTAR TUGAS (INI POSISI BARUNYA: DI BAWAH TOMBOL START) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp), // <<< PENTING: Batasi tinggi Column ini,
//                        .weight(1f), // <<< Ini akan mengambil sisa ruang vertikal di bawah Tombol START
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        label = { Text("Add a new task", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.LightGray,
                            cursorColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newTaskText.isNotBlank()) {
                                tasks.add(Task(UUID.randomUUID().toString(), newTaskText))
                                newTaskText = ""
                            }
                        })
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newTaskText.isNotBlank()) {
                                tasks.add(Task(UUID.randomUUID().toString(), newTaskText))
                                newTaskText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Task", color = Color.White, fontFamily = font, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daftar Tugas (Ini akan bisa di-scroll jika melebihi tinggi maksimum)
                    if (tasks.isEmpty()) {
                        Text(
                            text = "No tasks yet! Add one above.",
                            color = Color.Gray,
                            fontFamily = font,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth() // Tidak ada weight di sini, tinggi diatur oleh LazyColumn induk
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
//                                .heightIn(max = 120.dp) // <<< TINGGI MAKSIMUM UNTUK SCROLL, SESUAIKAN JIKA PERLU
                        ) {
                            items(tasks.size) { index ->
                                val task = tasks[index]
                                TaskItem(
                                    task = task,
                                    onToggleComplete = { toggledTask ->
                                        val updatedList = tasks.toMutableList()
                                        val taskIndex = updatedList.indexOfFirst { it.id == toggledTask.id }
                                        if (taskIndex != -1) {
                                            updatedList[taskIndex] = toggledTask.copy(isCompleted = !toggledTask.isCompleted)
                                            tasks.clear()
                                            tasks.addAll(updatedList)
                                        }
                                    },
                                    onDelete = { deletedTask ->
                                        tasks.remove(deletedTask)
                                    }
                                )
                            }
                        }
                    }
                }
                // --- AKHIR BAGIAN INPUT DAN DAFTAR TUGAS ---
             // Penutup untuk Column utama di dalam Box FrontPomodoro

                // --- BAGIAN BAWAH (Tombol START/PAUSE) ---

                // --- AKHIR BAGIAN BAWAH ---
            }
        }
    }
}

@Composable
fun BreakScreen(navController: NavController) {
    var isStartPressed by remember { mutableStateOf(false) }
    var totalTime by remember { mutableIntStateOf(DEFAULT_BREAK_TIME) }
    var remainingTime by remember { mutableIntStateOf(totalTime) }
    var showDialog by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(1f) }
    val context = LocalContext.current

    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.alarm)
    }

    LaunchedEffect(isStartPressed) {
        if (isStartPressed) {
            while (remainingTime > 0 && isStartPressed) {
                progress = remainingTime.toFloat() / totalTime.toFloat()
                delay(1000L)
                remainingTime -= 1
            }
            if (remainingTime <= 0) {
                isStartPressed = false
                showNotification(context, "Break time is over!", "Let's return to work!")
                CoroutineScope(Dispatchers.Main).launch {
                    mediaPlayer.start()
                }
                navController.navigate("pomodoro")
            }
        }
    }

    if (showDialog) {
        TimePicker(
            currentTime = remainingTime,
            backColor = BackBreak,
            maxTime = 30,
            onTimeSelected = { newTime ->
                totalTime = newTime
                remainingTime = newTime
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackBreak)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(37.dp)
                    .padding(end = 5.dp),
                painter = painterResource(id = R.drawable.logo),
                contentDescription = ""
            )
            Text(
                text = "Bisafokus",
                fontSize = 35.sp,
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 175.dp)
                .background(FrontBreak, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                navController.navigate("pomodoro")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pomodoro",
                            fontSize = 20.sp,
                            fontFamily = font,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(ShadowBreak, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Break",
                            fontSize = 20.sp,
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                )
                {
                    TimerClock(progress = progress, backColor = BackBreak)
                    Text(
                        text = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60),
                        fontSize = 100.sp,
                        fontFamily = font,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable {
                            showDialog = true
                            isStartPressed = false
                        }
                    )
                }

                TimerButton(
                    context = context,
                    backColor = BackBreak,
                    text = if (isStartPressed) "PAUSE" else "START",
                    isPressed = isStartPressed,
                    onClick = { isStartPressed = !isStartPressed }
                )
            }
        }
    }
}


@Composable
fun TimerButton(
    context: Context,
    text: String,
    backColor: Color,
    isPressed: Boolean,
    onClick: () -> Unit
) {
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.click_sound)
    }

    Box(
        modifier = Modifier
            .padding(7.dp)
            .height(57.dp)
            .width(180.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isPressed) {
            Box(
                modifier = Modifier
                    .height(14.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Color(0xFFebebeb),
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
            )
        }

        Button(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (!isPressed) (-7).dp else 0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = backColor
            ),
            onClick = {
                CoroutineScope(Dispatchers.Main).launch {
                    mediaPlayer.start()
                }
                onClick()
            }
        ) {
            Text(
                text = text,
                fontFamily = font,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TimePicker(
    currentTime: Int,
    backColor: Color,
    maxTime: Int,
    onTimeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTime by remember { mutableIntStateOf(currentTime / 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(120.dp, 44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF363636)
                    ),
                    onClick = {
                        onTimeSelected(selectedTime * 60)
                    }
                ) {
                    Text(
                        text = "OK",
                        fontFamily = font,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = "Time (minutes)",
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFaaaaaa)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Divider(
                    color = Color(0xFFdcdcdc),
                    thickness = 2.dp,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "$selectedTime",
                    fontSize = 120.sp,
                    fontFamily = font,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF555555)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Slider(
                    value = selectedTime.toFloat(),
                    onValueChange = { selectedTime = it.toInt() },
                    valueRange = 1f..maxTime.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = backColor,
                        activeTrackColor = backColor,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }
        }
    )
}

@Composable
fun TimerClock(progress: Float, backColor: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = ""
    )

    Canvas(
        modifier = Modifier.size(300.dp)
    ) {
        val strokeWidth = 20f
        val radius = size.width / 2
        val centerX = size.width / 2
        val centerY = size.height / 2

        drawArc(
            color = backColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(strokeWidth)
        )

        drawArc(
            color = Color.White,
            startAngle = -90f,
            sweepAngle = 360 * animatedProgress,
            useCenter = false,
            style = Stroke(strokeWidth)
        )

        val indicatorRadius = radius * 1f
        val startAngle = -90f
        val sweepAngle = 360 * animatedProgress
        val endAngle = startAngle + sweepAngle
        val indicatorX =
            centerX + indicatorRadius * cos(Math.toRadians(endAngle.toDouble())).toFloat()
        val indicatorY =
            centerY + indicatorRadius * sin(Math.toRadians(endAngle.toDouble())).toFloat()

        drawCircle(
            color = Color.White,
            radius = strokeWidth / 2f, // UBAH INI: Membuat radius indikator lebih kecil (setengah dari strokeWidth)
            // Atau Anda bisa coba nilai tetap seperti: radius = 10f
            center = Offset(indicatorX, indicatorY)
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete(task) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.Green,
                        uncheckedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.text,
                    fontSize = 18.sp,
                    fontFamily = font,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    )
                )
            }
            IconButton(onClick = { onDelete(task) }) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_delete),
                    contentDescription = "Delete Task",
                    tint = Color.Red
                )
            }
        }
    }
}
