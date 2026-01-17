package com.markolukarami.copilotclone.adapters.presentation

class ChatPresenter {
    fun presentUser(text: String) = ChatViewModel(displayText = "You: $text\n", ChatType.USER)
    fun presentAssistant(text: String) = ChatViewModel(displayText = "AI: $text", ChatType.ASSISTANT)
    fun presentError(text: String)=  ChatViewModel(displayText = "Error: $text", ChatType.SYSTEM)
    fun loading() = ChatViewModel(displayText = "LLM is thinking...", ChatType.SYSTEM)
}