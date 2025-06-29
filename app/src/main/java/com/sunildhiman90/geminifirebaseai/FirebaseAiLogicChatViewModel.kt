package com.sunildhiman90.geminifirebaseai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class Attachment(
    val fileName: String?,
    val image: Bitmap? = null
)

class FirebaseAiLogicChatViewModel() : ViewModel() {

    val systemInstructions = content {
        text(
            "You are a chatbot who can answer questions asked by users."
        )
    }

    val generationConfig = generationConfig {
        //Represents the type of content present in a response, we are using gemini-2.5-flash which generates text
        responseModalities = listOf(ResponseModality.TEXT)

        //we can use gemini-2.0-flash-preview-image-generation for generating images and text both
        //responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
    }

    val modelName = "gemini-2.5-flash"

    val chatHistory: List<Content> = listOf()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _messages = MutableStateFlow<List<Content>>(emptyList())
    val messages: StateFlow<List<Content>> = _messages.asStateFlow()

    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments: StateFlow<List<Attachment>> = _attachments.asStateFlow()

    private var contentBuilder = Content.Builder()
    lateinit var chat: Chat

    init {
        val generativeModel = Firebase.ai(
            backend = GenerativeBackend.googleAI(),
        ).generativeModel(
            modelName = modelName,
            systemInstruction = systemInstructions,
            generationConfig = generationConfig
        )

        chat = generativeModel.startChat()
    }

    fun sendMessage(userMessage: String) {
        val prompt = contentBuilder.text(userMessage).build()

        _messages.update {
            it.toMutableList().apply {
                add(prompt)
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = chat.sendMessage(prompt)
                _messages.update {
                    it.toMutableList().apply {
                        add(response.candidates.first().content)
                    }
                }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
                contentBuilder = Content.Builder()
                _attachments.value = listOf()
            }
        }

    }

    fun addAttachment(
        fileInBytes: ByteArray,
        mimeType: String?,
        fileName: String? = "Unnamed file"
    ) {
        if (mimeType?.contains("image") == true) {
            contentBuilder.image(generateBitmapFromByteArray(fileInBytes))
        } else {

        }

        _attachments.update {
            it.toMutableList().apply {
                add(Attachment(fileName))
            }
        }
    }

    fun generateBitmapFromByteArray(fileInBytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(fileInBytes, 0, fileInBytes.size)
    }
}
