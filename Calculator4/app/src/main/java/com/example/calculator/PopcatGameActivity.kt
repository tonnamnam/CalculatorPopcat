package com.example.calculator

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class PopcatGameActivity : AppCompatActivity() {

    private lateinit var popcatImageView: ImageView
    private var score = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popcat_game)

        popcatImageView = findViewById(R.id.popcatImageView)
        val backButton: Button = findViewById(R.id.backButton)

        // โหลดคะแนนที่บันทึกไว้
        score = loadScore()
        updateScoreDisplay()

        popcatImageView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    popcatImageView.setImageResource(R.drawable.popcat_open)
                    score++
                    updateScoreDisplay()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    popcatImageView.setImageResource(R.drawable.popcat_closed)
                    true
                }
                else -> false
            }
        }

        backButton.setOnClickListener {
            saveScore()
            finish()
        }
    }

    private fun updateScoreDisplay() {
        title = "Popcat Score: $score"
    }

    private fun saveScore() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putInt("popcat_score", score)
            apply()
        }
    }

    private fun loadScore(): Int {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        return sharedPref.getInt("popcat_score", 0)
    }

    override fun onPause() {
        super.onPause()
        saveScore()
    }
}