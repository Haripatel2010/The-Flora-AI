package com.floraai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FloraApp() }
    }
}

@Composable
fun FloraApp() {
    var message by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf("Hello! I'm FLORA AI. Add your API key in Settings to begin.")) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF080B12),
            surface = Color(0xFF151A25),
            primary = Color(0xFF9B8CFF)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080B12))
                .padding(16.dp)
        ) {
            Text("FLORA AI", color = Color.White, fontSize = 25.sp)
            Text("Your personal AI assistant", color = Color.Gray, fontSize = 13.sp)

            Spacer(Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { item ->
                    Text(
                        item,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF151A25), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF151A25), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    decorationBox = { inner ->
                        if (message.isEmpty()) {
                            Text("Message FLORA...", color = Color.Gray)
                        }
                        inner()
                    }
                )

                Spacer(Modifier.width(8.dp))

                Button(onClick = {
                    if (message.isNotBlank()) {
                        messages = messages + "You: $message"
                        message = ""
                    }
                }) {
                    Text("Send")
                }
            }
        }
    }
}
