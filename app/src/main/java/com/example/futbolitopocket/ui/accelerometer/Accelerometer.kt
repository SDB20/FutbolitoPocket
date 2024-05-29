package com.example.futbolitopocket.ui.accelerometer

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ricknout.composesensors.accelerometer.isAccelerometerSensorAvailable
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun AccelerometerDemo() {
    var scoreLeft by remember { mutableStateOf(0) }
    var scoreRight by remember { mutableStateOf(0) }

    val context = LocalContext.current

    if (isAccelerometerSensorAvailable()) {
        val sensorValue by rememberAccelerometerSensorValueAsState()
        val (x, y, z) = sensorValue.value

        val orientation = LocalConfiguration.current.orientation
        val contentColor = LocalContentColor.current
        val radius = with(LocalDensity.current) { 10.dp.toPx() }
        val goalHeight = with(LocalDensity.current) { 50.dp.toPx() }
        val goalWidth = with(LocalDensity.current) { 100.dp.toPx() }

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00008B))) {
            var center by remember { mutableStateOf(Offset.Zero) }

            val screenSize = getScreenSize(context)
            val screenWidth = screenSize.first.toFloat()
            val screenHeight = screenSize.second.toFloat()

            FootballFieldCanvas(goalWidth = screenWidth, goalHeight = screenHeight)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            Log.d("TouchEvent", "${offset.x}f,${offset.y}f")
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                if (center == Offset.Zero) {
                    center = Offset(width / 2, height / 2)
                }

                var newCenter = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Offset(
                        x = (center.x - x).coerceIn(radius, width - radius),
                        y = (center.y + y).coerceIn(radius, height - radius)
                    )
                } else {

                    Offset(
                        x = (center.x + y).coerceIn(radius, width - radius),
                        y = (center.y + x).coerceIn(radius, height - radius)
                    )
                }

                val topGoalTopLeft = Offset((width - goalWidth) / 2, 0f)
                val bottomGoalTopLeft = Offset((width - goalWidth) / 2, height - goalHeight)

                // Detect goal scoring
                if (newCenter.y - radius <= goalHeight && newCenter.x in topGoalTopLeft.x..(topGoalTopLeft.x + goalWidth)) {
                    scoreRight++
                    newCenter = Offset(width / 2, height / 2) // Reset position after scoring
                } else if (newCenter.y + radius >= height - goalHeight && newCenter.x in bottomGoalTopLeft.x..(bottomGoalTopLeft.x + goalWidth)) {
                    scoreLeft++
                    newCenter = Offset(width / 2, height / 2) // Reset position after scoring
                }

                // Detect collisions with lines
                var collisionDetected = false
                val lines = getLines(width, height, goalWidth, goalHeight)
                for (line in lines) {
                    val (start, end) = line
                    val intersectionPoint = lineIntersectsCircle(start, end, newCenter, radius)
                    if (intersectionPoint != null) {
                        collisionDetected = true

                        // Calculate the reflection vector
                        val dx = end.x - start.x
                        val dy = end.y - start.y
                        val distance = sqrt(dx * dx + dy * dy)
                        val normalX = dy / distance
                        val normalY = -dx / distance
                        val normal = Offset(normalX, normalY)
                        val incidentVector = Offset(x, y)
                        val dotProduct = incidentVector.x * normal.x + incidentVector.y * normal.y
                        val reflectionVector = Offset(
                            x = incidentVector.x - 2 * dotProduct * normal.x,
                            y = incidentVector.y - 2 * dotProduct * normal.y
                        )

                        // Update the ball's position to avoid it being stuck in the line
                        val timeFactor = 100f
                        newCenter = Offset(
                            x = newCenter.x + (reflectionVector.x * 2 * timeFactor), // Aumentamos el factor de multiplicación
                            y = newCenter.y + (reflectionVector.y * 10 * timeFactor)  // para un rebote más rápido
                        )

                        break
                    }
                }

                if (!collisionDetected) {
                    center = newCenter
                }

                // Draw goals
                val goalStrokeWidth = 8.dp.toPx()
                drawGoal(
                    topLeft = topGoalTopLeft,
                    width = goalWidth,
                    height = goalHeight,
                    strokeWidth = goalStrokeWidth,
                    color = Color.White,
                    isTopGoal = true
                )
                drawGoal(
                    topLeft = bottomGoalTopLeft,
                    width = goalWidth,
                    height = goalHeight,
                    strokeWidth = goalStrokeWidth,
                    color = Color.White,
                    isTopGoal = false
                )

                // Draw ball
                drawCircle(
                    color = contentColor,
                    radius = radius,
                    center = center
                )
            }

            // Display scores
            Text(
                text = "Goles: $scoreLeft",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                fontSize = 19.sp
            )
            Text(
                text = "Goles: $scoreRight",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                fontSize = 19.sp
            )
        }
    } else {
        NotAvailableDemo()
    }
}