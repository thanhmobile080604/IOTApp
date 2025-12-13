package com.example.iotapp.ui.chatbot

import android.Manifest
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentChatbotBinding
import com.example.iotapp.model.ChatMessage
import com.example.iotapp.model.PlantInformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

class ChatbotFragment : BaseFragment<FragmentChatbotBinding>(FragmentChatbotBinding::inflate) {

    private val messageAdapter = ChatMessageAdapter { message -> onFileClick(message) }
    private val messages = mutableListOf<ChatMessage>()

    // Ollama URL - For Android emulator use "http://10.0.2.2:11434"
    // For physical device, use your computer's IP address: "http://YOUR_COMPUTER_IP:11434"
    // Make sure Ollama is running and exposed to network: ollama serve

    private val ollamaBaseUrl = "https://ollama.com"
    private val ollamaModel = "gpt-oss:120b"


    private val OLLAMA_API_KEY = "0313a3b2d2dd4f219c263389a1f8185a.QEgeNBS01Ksiapjn1stb4VXK"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()





    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImageUri != null) {
            currentImageUri?.let { uri ->
                sendImageMessage(uri)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            sendImageMessage(it)
        }
    }

    private val fileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            sendFileMessage(it)
        }
    }

    private var currentImageUri: Uri? = null

    override fun FragmentChatbotBinding.initView() {
        setupRecyclerView()
        setupStatusBar()
        setupKeyboardListener()
    }

    override fun FragmentChatbotBinding.initListener() {
        icBack.setSingleClick { onBack() }

        icSend.setSingleClick {
            val message = etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendTextMessage(message)
                etMessage.text?.clear()
            }
        }

        icCamera.setSingleClick {
            requestCameraPermission()
        }

        icGallery.setSingleClick {
            requestStoragePermission { galleryLauncher.launch("image/*") }
        }

        icFile.setSingleClick {
            requestStoragePermission { fileLauncher.launch("application/pdf") }
        }

        // Send on Enter key (handled by imeOptions="actionSend")
        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                val message = etMessage.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendTextMessage(message)
                    etMessage.text?.clear()
                }
                true
            } else {
                false
            }
        }
    }

    override fun initObserver() = Unit

    private fun setupRecyclerView() {
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupStatusBar() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarSpacer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.layoutParams.height = systemBars.top
            insets
        }
    }

    private fun setupKeyboardListener() {
        // Make input bar follow keyboard using WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(binding.inputBar) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply bottom margin/padding when keyboard is visible
            // This makes the input bar stick to the top of the keyboard
            val bottomPadding = if (imeInsets.bottom > 0) {
                imeInsets.bottom - systemBars.bottom
            } else {
                0
            }

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                bottomPadding
            )

            // Scroll to bottom when keyboard appears
            if (imeInsets.bottom > 0 && messages.isNotEmpty()) {
                binding.rvMessages.postDelayed({
                    binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                }, 100)
            }

            insets
        }
    }

//    private fun sendTextMessage(text: String) {
//        val userMessage = ChatMessage(
//            id = UUID.randomUUID().toString(),
//            text = text,
//            isUser = true
//        )
//        val plantInfo = mainViewModel.fireBaseInformation.value
//        val prompt = buildPlantPrompt(plantInfo, text)
//        addMessage(userMessage)
//        sendToOllama(prompt)
//    }
//
    private fun sendImageMessage(imageUri: Uri) {
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = "",
            isUser = true,
            imageUri = imageUri
        )
        addMessage(userMessage)
        // For now, just send a text message about the image
        // In the future, you can implement vision model support
        //sendToOllama("I've shared an image with you.")
    }

    private fun sendTextMessage(text: String) {

        // 1️⃣ User message (luôn add trước)
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isUser = true
        )
        addMessage(userMessage)

        // 2️⃣ Thinking message (giữ id để remove)
        val thinkingMessageId = UUID.randomUUID().toString()
        val thinkingMessage = ChatMessage(
            id = thinkingMessageId,
            text = getString(R.string.thinking),
            isUser = false
        )
        addMessage(thinkingMessage)

        val plantInfo = mainViewModel.fireBaseInformation.value

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reply = callOllamaCloud(plantInfo, text)

                withContext(Dispatchers.Main) {
                    removeMessageById(thinkingMessageId)

                    addMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = reply,
                            isUser = false
                        )
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    removeMessageById(thinkingMessageId)

                    addMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = "Error: ${e.message}",
                            isUser = false
                        )
                    )
                }
            }
        }
    }


    private fun sendFileMessage(fileUri: Uri) {
        try {
            val fileName = getFileName(fileUri)
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = fileName ?: "file.pdf",
                isUser = true,
                fileUri = fileUri,
                fileName = fileName
            )
            addMessage(userMessage)
            //sendToOllama("I've shared a PDF file: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file message", e)
            Toast.makeText(requireContext(), "Error selecting file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex =
                        cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) {
                    it.substring(cut + 1)
                } else {
                    it
                }
            }
        }
        return result
    }

    private fun onFileClick(message: ChatMessage) {
        message.fileUri?.let { uri ->
            downloadFile(uri, message.fileName ?: "file.pdf")
        }
    }

    private fun downloadFile(uri: Uri, fileName: String) {
        try {
            requestStoragePermission {
                try {
                    val contentResolver = requireContext().contentResolver
                    val inputStream = contentResolver.openInputStream(uri)

                    if (inputStream != null) {
                        val downloadsDir =
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                // Use app-specific directory for Android 10+
                                requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                            } else {
                                android.os.Environment.getExternalStoragePublicDirectory(
                                    android.os.Environment.DIRECTORY_DOWNLOADS
                                )
                            }

                        if (downloadsDir != null && !downloadsDir.exists()) {
                            downloadsDir.mkdirs()
                        }

                        if (downloadsDir != null) {
                            val file = java.io.File(downloadsDir, fileName)
                            file.outputStream().use { output ->
                                inputStream.copyTo(output)
                            }

                            Toast.makeText(
                                requireContext(),
                                "File saved: $fileName",
                                Toast.LENGTH_LONG
                            ).show()

                            Log.d(TAG, "File downloaded to: ${file.absolutePath}")
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Cannot access Downloads folder",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error reading file", Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading file", e)
                    Toast.makeText(
                        requireContext(),
                        "Error downloading file: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting permission for download", e)
            Toast.makeText(
                requireContext(),
                "Permission required to download file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        messageAdapter.submitList(messages.toList()) {
            binding.rvMessages.scrollToPosition(messages.size - 1)
        }
    }

//    private fun sendToOllama(prompt: String) {
//        // Show loading message
//        val loadingMessage = ChatMessage(
//            id = UUID.randomUUID().toString(),
//            text = getString(R.string.thinking),
//            isUser = false
//        )
//        val loadingIndex = messages.size
//        messages.add(loadingMessage)
//        messageAdapter.submitList(messages.toList())
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val response = callOllamaCloud(plantInfo, text)
//                withContext(Dispatchers.Main) {
//                    // Remove loading message
//                    messages.removeAt(loadingIndex)
//                    // Add bot response
//                    val botMessage = ChatMessage(
//                        id = UUID.randomUUID().toString(),
//                        text = response,
//                        isUser = false
//                    )
//                    messages.add(botMessage)
//                    messageAdapter.submitList(messages.toList()) {
//                        binding.rvMessages.scrollToPosition(messages.size - 1)
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error calling Ollama API", e)
//                withContext(Dispatchers.Main) {
//                    // Remove loading message
//                    messages.removeAt(loadingIndex)
//                    // Add error message
//                    val errorMessage = ChatMessage(
//                        id = UUID.randomUUID().toString(),
//                        text = "Error: ${e.message ?: getString(R.string.error_sending_message)}",
//                        isUser = false
//                    )
//                    messages.add(errorMessage)
//                    messageAdapter.submitList(messages.toList()) {
//                        binding.rvMessages.scrollToPosition(messages.size - 1)
//                    }
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.error_sending_message),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }

//    private suspend fun callOllamaAPI(prompt: String): String = withContext(Dispatchers.IO) {
//        try {
//            // Build context from conversation history
//            val context = buildContextFromHistory(prompt)
//
//            val jsonBody = JSONObject().apply {
//                put("model", ollamaModel)
//                put("prompt", context)
//                put("stream", false)
//            }
//
//            Log.d(TAG, "Calling Ollama API: $ollamaBaseUrl/api/generate with model: $ollamaModel")
//
//            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
//            val request = Request.Builder()
//                .url("$ollamaBaseUrl/api/generate")
//                .post(requestBody)
//                .addHeader("Content-Type", "application/json")
//                .build()
//
//            Log.d(TAG, "Request URL: ${request.url}")
//            Log.d(TAG, "Request body: ${jsonBody.toString()}")
//
//            val response = okHttpClient.newCall(request).execute()
//            val responseBody = response.body?.string() ?: ""
//
//            Log.d(
//                TAG,
//                "Ollama API response code: ${response.code}, body length: ${responseBody.length}"
//            )
//
//            if (response.isSuccessful) {
//                val jsonResponse = JSONObject(responseBody)
//                val botResponse = jsonResponse.optString("response", "No response")
//                Log.d(TAG, "Bot response: $botResponse")
//                botResponse
//            } else {
//                val errorMsg = "API call failed: ${response.code} - $responseBody"
//                Log.e(TAG, errorMsg)
//                throw Exception(errorMsg)
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Ollama API error", e)
//            throw e
//        }
//    }

    private suspend fun callOllamaCloud(
        plantInfo: PlantInformation?,
        userText: String
    ): String = withContext(Dispatchers.IO) {
        val messages = buildPlantMessages(plantInfo, userText)

        val jsonBody = JSONObject().apply {
            put("model", ollamaModel)
            put("messages", messages)
            put("stream", false)
        }

        val requestBody =
            jsonBody.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$ollamaBaseUrl/api/chat")
            .addHeader("Authorization", "Bearer $OLLAMA_API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("Cloud Ollama error ${response.code}: $body")
        }

        val json = JSONObject(body)
        json
            .getJSONObject("message")
            .getString("content")
    }


    private fun buildContextFromHistory(currentPrompt: String): String {
        // Build context from recent messages (last 5 user-bot pairs)
        val recentMessages = messages.takeLast(10) // Last 10 messages (5 pairs)
        val contextBuilder = StringBuilder()

        recentMessages.forEach { message ->
            if (message.isUser) {
                contextBuilder.append("User: ${message.text}\n")
            } else {
                contextBuilder.append("Assistant: ${message.text}\n")
            }
        }

        contextBuilder.append("User: $currentPrompt\n")
        contextBuilder.append("Assistant:")

        return contextBuilder.toString()
    }

    private fun requestCameraPermission() {
        doRequestPermission(
            arrayOf(Manifest.permission.CAMERA),
            object : IPermissionListener {
                override fun onAllow() {
                    openCamera()
                }

                override fun onDenied() {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onNeverAskAgain(permission: String) {
                    Toast.makeText(
                        requireContext(),
                        "Please enable camera permission in settings",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun requestStoragePermission(onGranted: () -> Unit) {
        val permissions =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        doRequestPermission(
            permissions,
            object : IPermissionListener {
                override fun onAllow() {
                    onGranted()
                }

                override fun onDenied() {
                    Toast.makeText(
                        requireContext(),
                        "Storage permission denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onNeverAskAgain(permission: String) {
                    Toast.makeText(
                        requireContext(),
                        "Please enable storage permission in settings",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun openCamera() {
        val imageUri = createImageUri()
        currentImageUri = imageUri
        cameraLauncher.launch(imageUri)
    }

    private fun createImageUri(): Uri {
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "chatbot_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Failed to create image URI")
    }

    companion object {
        private const val TAG = "ChatbotFragment"
    }

    fun buildPlantPrompt(
        plantInfo: PlantInformation?,
        userText: String
    ): String {
        if (plantInfo == null) return userText

        return """
You are a smart plant care assistant.
Use ONLY the information below to answer.

Plant status:
- Temperature: ${plantInfo.temperature}
- Humidity: ${plantInfo.humidity}
- Rain: ${plantInfo.rainStatus}
- Pump: ${plantInfo.connectStatus}
- Schedule: ${plantInfo.schedule}

User question:
$userText
""".trimIndent()
    }

    fun buildPlantMessages(
        plantInfo: PlantInformation?,
        userText: String
    ): org.json.JSONArray {
        val messages = org.json.JSONArray()

        // SYSTEM
        val systemContent = if (plantInfo != null) {
            """
You are a smart plant care assistant.
Use ONLY the following plant data to answer.

Plant status:
- Temperature: ${plantInfo.temperature}
- Humidity: ${plantInfo.humidity}
- Rain: ${plantInfo.rainStatus}
- Pump: ${plantInfo.connectStatus}
- Schedule: ${plantInfo.schedule}
""".trimIndent()
        } else {
            "You are a helpful assistant."
        }

        messages.put(
            JSONObject()
                .put("role", "system")
                .put("content", systemContent)
        )

        // USER
        messages.put(
            JSONObject()
                .put("role", "user")
                .put("content", userText)
        )

        return messages
    }

    private fun removeMessageById(messageId: String) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messages.removeAt(index)
            messageAdapter.submitList(messages.toList()) {
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

}

