package com.sffteam.openmax

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Toast

class CodeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code)

        val input = findViewById<EditText>(R.id.editTextCode)
        val button = findViewById<Button>(R.id.buttonjoin)

        val phone = intent.getStringExtra("number")

        Toast.makeText(this, phone, Toast.LENGTH_SHORT).show()
    }
}