
package com.floraai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FloraApp()
        }
    }
}

@Composable
fun FloraApp() {
    val context = LocalContext.current

    var showSettings by remember { mutableStateOf(false) }
    var geminiKey by remember { mutableStateOf("") }
    var groqKey by remember { mutableStateOf("") }
    var openRouterKey by remember { mutableStateOf("") }

    var geminiSaved by remember {
        mutableStateOf(KeyVault.get(context, "gemini") != null)
    }
    var groqSaved by remember {
        mutableStateOf(KeyVault.get(context, "groq") != null)
    }
    var openRouterSaved by remember {
        mutableStateOf(KeyVault.get(context, "openrouter") != null)
    }

    var status by remember { mutableStateOf("") }

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
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "FLORA AI",
                        color = Color.White,
                        fontSize = 26.sp
                    )
                    Text(
                        "Your personal AI assistant",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Button(onClick = {
                    showSettings = !showSettings
                    status = ""
                }) {
                    Text(if (showSettings) "Chat" else "Settings ⚙")
                }
            }

            Spacer(Modifier.height(24.dp))

            if (showSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "API Settings",
                        color = Color.White,
                        fontSize = 22.sp
                    )

                    Text(
                        "Add your own API keys to connect FLORA to AI providers.",
                        color = Color.Gray
                    )

                    ApiKeyField(
                        title = "Gemini API Key",
                        value = geminiKey,
                        saved = geminiSaved,
                        onValueChange = { geminiKey = it },
                        onSave = {
                            if (geminiKey.isNotBlank()) {
                                KeyVault.save(
                                    context, "gemini", geminiKey.trim()
                                )
                                geminiKey = ""
                                geminiSaved = true
                                status = "Gemini key saved securely!"
                            } else {
                                status = "Enter a Gemini key first."
                            }
                        },
                        onDelete = {
                            KeyVault.delete(context, "gemini")
                            geminiKey = ""
                            geminiSaved = false
                            status = "Gemini key deleted."
                        }
                    )

                    ApiKeyField(
                        title = "Groq API Key",
                        value = groqKey,
                        saved = groqSaved,
                        onValueChange = { groqKey = it },
                        onSave = {
                            if (groqKey.isNotBlank()) {
                                KeyVault.save(
                                    context, "groq", groqKey.trim()
                                )
                                groqKey = ""
                                groqSaved = true
                                status = "Groq key saved securely!"
                            } else {
                                status = "Enter a Groq key first."
                            }
                        },
                        onDelete = {
                            KeyVault.delete(context, "groq")
                            groqKey = ""
                            groqSaved = false
                            status = "Groq key deleted."
                        }
                    )

                    ApiKeyField(
                        title = "OpenRouter API Key",
                        value = openRouterKey,
                        saved = openRouterSaved,
                        onValueChange = { openRouterKey = it },
                        onSave = {
                            if (openRouterKey.isNotBlank()) {
                                KeyVault.save(
                                    context, "openrouter",
                                    openRouterKey.trim()
                                )
                                openRouterKey = ""
                                openRouterSaved = true
                                status = "OpenRouter key saved securely!"
                            } else {
                                status = "Enter an OpenRouter key first."
                            }
                        },
                        onDelete = {
                            KeyVault.delete(context, "openrouter")
                            openRouterKey = ""
                            openRouterSaved = false
                            status = "OpenRouter key deleted."
                        }
                    )

                    if (status.isNotBlank()) {
                        Text(status, color = Color(0xFF9B8CFF))
                    }

                    Text(
                        "Your API keys are encrypted and stored locally on this device.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFF151A25),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Welcome to FLORA AI 🌿",
                            color = Color.White,
                            fontSize = 21.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Open Settings and add your API key to get started.",
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = {
                            showSettings = true
                        }) {
                            Text("Open Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApiKeyField(
    title: String,
    value: String,
    saved: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF151A25),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Color.White, fontSize = 17.sp)

        Text(
            if (saved) "Key saved on this device"
            else "No key saved",
            color = if (saved) Color(0xFF7BE0A0) else Color.Gray,
            fontSize = 12.sp
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste your API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                enabled = saved
            ) {
                Text("Delete")
            }
        }
    }
}
