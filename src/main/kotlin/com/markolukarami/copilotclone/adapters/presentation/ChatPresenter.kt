package com.markolukarami.copilotclone.adapters.presentation

class ChatPresenter {
    fun presentUser(text: String): ChatViewModel = ChatViewModel(displayText = "You: $text\n")
    fun presentAssistant(text: String): ChatViewModel = ChatViewModel(displayText = "AI: $text")
    fun presentError(text: String): ChatViewModel = ChatViewModel(displayText = "Error: $text")
}