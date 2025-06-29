package com.sunildhiman90.geminifirebaseai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sunildhiman90.geminifirebaseai.ui.theme.GeminiFirebaseAiChatbotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeminiFirebaseAiChatbotTheme {
                FirebaseAiLogicChatScreen()
            }
        }
    }
}
