package com.sunildhiman90.geminifirebaseai

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.database.getStringOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.TextPart
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.random.Random
import kotlin.random.nextInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseAiLogicChatScreen(
    chatViewModel: FirebaseAiLogicChatViewModel = viewModel<FirebaseAiLogicChatViewModel>()
) {

    val messages: List<Content> by chatViewModel.messages.collectAsStateWithLifecycle()
    val isLoading: Boolean by chatViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage: String? by chatViewModel.errorMessage.collectAsStateWithLifecycle()
    val attachments: List<Attachment> by chatViewModel.attachments.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Firebase AI Gemini Chatbot",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            //Main Content
            ChatList(
                messages, listState, modifier = Modifier
                    .fillMaxSize()
                    .weight(0.5f)
            )

            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer
                        )
                ) {
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    errorMessage?.let {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {

                            Text(text = it, modifier = Modifier.padding(16.dp))
                        }
                    }


                    //Attachment list

                    AttachmentList(
                        attachments
                    )

                    val context = LocalContext.current
                    val contentResolver = context.contentResolver

                    val scope = rememberCoroutineScope()
                    BottomInput(
                        initialMessage = "",
                        onSendMessage = {
                            chatViewModel.sendMessage(it)
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        onFileAttached = { uri ->
                            val mimeType = contentResolver.getType(uri).orEmpty()
                            var fileName: String? = null
                            contentResolver.query(
                                uri, null, null, null, null
                            )?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                                cursor.moveToLast()

                                val humanReadableSize =
                                    Formatter.formatShortFileSize(
                                        context,
                                        cursor.getLong(sizeIndex)
                                    )
                                fileName = "${cursor.getString(nameIndex)} ($humanReadableSize)"
                            }

                            contentResolver.openInputStream(uri)?.use { stream ->
                                val bytes = stream.readBytes()
                                chatViewModel.addAttachment(
                                    bytes, mimeType, fileName
                                )
                            }
                        },
                        isLoading = isLoading
                    )
                }

            }

        }
    }
}


@Composable
fun BottomInput(
    initialMessage: String = "",
    onSendMessage: (String) -> Unit,
    onFileAttached: (Uri) -> Unit,
    isLoading: Boolean = false
) {

    var userMessage by rememberSaveable {
        mutableStateOf(initialMessage)
    }

    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value = userMessage,
            label = {
                Text("Enter Message")
            },
            onValueChange = {
                userMessage = it
            },
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 4.dp)
                .fillMaxWidth()
                .weight(1f)
        )
        //Add attachment
        AttachmentMenu(
            modifier = Modifier.align(Alignment.CenterVertically),
            onFileAttached = onFileAttached
        )

        IconButton(
            onClick = {
                if (userMessage.isNotBlank()) {
                    onSendMessage(userMessage)
                    userMessage = ""
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clip(CircleShape)
                .background(
                    color = if (isLoading) {
                        IconButtonDefaults.iconButtonColors().disabledContentColor
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
        ) {

            Icon(
                Icons.AutoMirrored.Default.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )

        }

    }


}


@Composable
fun AttachmentList(
    attachments: List<Attachment>
) {

    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(attachments) { attachment ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = 8.dp, vertical = 4.dp
                )
            ) {
                Icon(
                    Icons.Default.Attachment, contentDescription = null,
                    modifier = Modifier.padding(4.dp).align(
                        Alignment.CenterVertically
                    )
                )
                attachment.image?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = attachment.fileName,
                        modifier = Modifier.align(
                            Alignment.CenterVertically
                        )
                    )
                }

                Text(
                    text = attachment.fileName ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 4.dp)
                )
            }
        }
    }


}

@Composable
fun AttachmentMenu(
    modifier: Modifier = Modifier,
    onFileAttached: (Uri) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            onFileAttached(it)
        }
    }

    Box(modifier = modifier.padding(end = 4.dp)) {

        IconButton(onClick = {
            expanded = !expanded
        }) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }

        DropdownMenu(
            expanded = expanded, onDismissRequest = {
                expanded = false
            }
        ) {
            Text(
                "Attach",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            DropdownMenuItem(
                text = {
                    Text("Image")
                },
                onClick = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    expanded = !expanded
                }
            )
        }
    }

}

@Composable
fun ChatList(
    messages: List<Content>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        reverseLayout = true,
        state = listState,
        modifier = modifier
    ) {
        items(messages.reversed(), key = { Random.nextInt() }) { chatMessage ->
            ChatItem(chatMessage)
        }

    }

}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ChatItem(
    chatMessage: Content
) {

    val isModelMessage = chatMessage.role == "model"

    val horizontalAlignment = if (
        isModelMessage
    ) Alignment.Start else Alignment.End


    val bubbleShape = if (isModelMessage) {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    }

    val bgColor = if (isModelMessage) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = if (isModelMessage) {
        Color.Unspecified
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }


    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            )
            .fillMaxWidth()
    ) {

        Row {

            BoxWithConstraints {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = bgColor
                    ),
                    shape = bubbleShape,
                    modifier = Modifier.requiredWidthIn(0.dp, maxWidth * 0.9f)
                ) {

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        chatMessage.parts.forEach { part ->
                            when (part) {
                                is TextPart -> {
                                    Text(
                                        text = part.text,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = textColor
                                    )
                                }

                                is ImagePart -> {

                                    Image(
                                        bitmap = part.image.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                }
            }

        }

    }

}