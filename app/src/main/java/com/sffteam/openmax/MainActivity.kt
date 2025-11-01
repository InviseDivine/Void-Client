package com.sffteam.openmax

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity
import android.widget.AdapterView
import android.content.Intent
import android.util.Log
import com.sffteam.openmax.WebsocketManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WebsocketManager.Connect()

        val label = findViewById<Button>(R.id.buttonjoin)
        val input = findViewById<EditText>(R.id.edittext)
        val spinner = findViewById<Spinner>(R.id.spinner)
        val phonecodes = arrayOf("+7", "+375")
        var currentcode = "+7"

        val arrayAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, phonecodes)

        spinner.adapter = arrayAdapter

        label.setOnClickListener {
            val intent = Intent(this, CodeActivity::class.java)
            val phone = currentcode + input.text
            intent.putExtra("number", phone)

            WebsocketManager.SendPacket(
                OPCode.START_AUTH,
                JsonObject(
                    mapOf(
                        "phone" to JsonPrimitive(phone),
                        "type" to JsonPrimitive("START_AUTH"),
                        "language" to JsonPrimitive("ru")
                    )
                )
            )

            startActivity(intent)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                currentcode = phonecodes[p2]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }
    }
}