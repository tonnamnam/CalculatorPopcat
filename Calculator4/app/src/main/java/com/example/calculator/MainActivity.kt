package com.example.calculator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.lang.ArithmeticException

class MainActivity : AppCompatActivity() {

    private lateinit var workingsTV: TextView
    private lateinit var resultsTV: TextView
    private var workings = ""
    private var Operator = false
    private var Decimal = true

    // Variable to track the number of times equals is pressed
    private var equalsPressCount = 0

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        workingsTV = findViewById(R.id.working)
        resultsTV = findViewById(R.id.results)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE)
            } else {
                createNotificationChannelInternal()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelInternal() {
        val name = "My Notification Channel"
        val descriptionText = "This is a description for the channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("MY_CHANNEL_ID", name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                createNotificationChannelInternal()
            }
        }
    }

    fun toggleSign(view: View) {
        if (workings.isEmpty()) return

        val regex = Regex("(?<=[+\\-x/])(-?\\d+\\.?\\d*)\$|^-?\\d+\\.?\\d*\$")
        val matchResult = regex.find(workings)

        if (matchResult != null) {
            val lastNumber = matchResult.value

            if (lastNumber.startsWith("-")) {
                workings = workings.replaceRange(matchResult.range, lastNumber.substring(1))
            } else {
                workings = workings.replaceRange(matchResult.range, "-$lastNumber")
            }
            workingsTV.text = workings
        }
    }

    fun operatorAction(view: View) {
        if (view is Button) {
            val operatorText = view.text.toString()

            if (workings.isEmpty() && operatorText == "-") {
                workings += operatorText
                workingsTV.text = workings
                Operator = false
            } else if (Operator || (operatorText == "-" && workings.last() in "x/")) {
                workings += operatorText
                workingsTV.text = workings
                Operator = false
                Decimal = true
            } else if (workings.isNotEmpty() && workings.last().isDigit()) {
                workings += operatorText
                workingsTV.text = workings
                Operator = false
                Decimal = true
            } else if (operatorText != "-" && workings.isNotEmpty() && workings.last() in "+x/") {
                workings = workings.dropLast(1) + operatorText
                workingsTV.text = workings
                Operator = false
                Decimal = true
            }
        }
    }

    fun numberAction(view: View) {
        if (view is Button) {
            if (view.text == ".") {
                if (Decimal) {
                    workings += view.text
                    Decimal = false
                }
            } else {
                workings += view.text
            }
            workingsTV.text = workings
            Operator = true
        }
    }

    fun allClearAction(view: View) {
        workingsTV.text = ""
        resultsTV.text = ""
        workings = ""
        Operator = false
        Decimal = true
    }

    fun backSpaceAction(view: View) {
        if (workings.isNotEmpty()) {
            val lastChar = workings.last()
            if (lastChar == '.') {
                Decimal = true
            }
            workings = workings.dropLast(1)
            workingsTV.text = workings
            Operator = workings.isNotEmpty() && workings.last().isDigit()
        }
    }

    fun equalsAction(view: View) {
        try {
            val result = evaluateExpression()
            val resultText = if (result % 1 == 0.0) {
                result.toInt().toString()
            } else {
                result.toString()
            }
            resultsTV.text = resultText
            workings = resultText
            Operator = true
            Decimal = !workings.contains(".")

            // Increment equals press count and show notification
            equalsPressCount++
            showNotification(equalsPressCount)

        } catch (e: ArithmeticException) {
            resultsTV.text = "Error"
        } catch (e: Exception) {
            resultsTV.text = "Error"
            e.printStackTrace()
        }
    }

    private fun evaluateExpression(): Double {
        var expression = workings.replace("x", "*")

        expression = expression.replace("+-", "-")
        expression = expression.replace("-+", "-")
        expression = expression.replace("--", "+")
        expression = expression.replace("++", "+")

        if (expression.startsWith("-")) {
            expression = "0$expression"
        }

        val parts = expression.split(Regex("(?<=[*/])|(?=[*/])"))
        var result = evaluateSimpleExpression(parts[0])

        for (i in 1 until parts.size step 2) {
            if (i + 1 >= parts.size) break

            val operator = parts[i]
            val operand = evaluateSimpleExpression(parts[i + 1])

            when (operator) {
                "*" -> result *= operand
                "/" -> {
                    if (operand == 0.0) throw ArithmeticException("Division by zero")
                    result /= operand
                }
            }
        }

        return result
    }

    private fun evaluateSimpleExpression(expr: String): Double {
        var expression = expr
        expression = expression.replace("+-", "-")
        expression = expression.replace("-+", "-")
        expression = expression.replace("--", "+")
        expression = expression.replace("++", "+")

        val parts = expression.split(Regex("(?<=[+-])|(?=[+-])"))
        var result = parts[0].toDoubleOrNull() ?: 0.0

        for (i in 1 until parts.size step 2) {
            if (i + 1 >= parts.size) break

            val operator = parts[i]
            val operand = parts[i + 1].toDoubleOrNull() ?: continue

            when (operator) {
                "+" -> result += operand
                "-" -> result -= operand
            }
        }

        return result
    }

    private fun showNotification(equalsCount: Int) {
        val channelId = "countdown_channel"
        val channelName = "Equals Press Notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notification channel for equals press count"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Equals Press Count")
            .setContentText("The equals button has been pressed $equalsCount times")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(0, notificationBuilder.build())
    }

    fun gameAction(view: View) {
        val intent = Intent(this, PopcatGameActivity::class.java)
        startActivity(intent)
    }
}