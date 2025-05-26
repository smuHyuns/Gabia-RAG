// src/stores/chatStore.js
import { defineStore } from 'pinia'

export const useChatStore = defineStore('chat', {
  state: () => ({
    chatId: '',
  }),
  actions: {
    setChatId(id) {
      this.chatId = id
    },
    clearChatId() {
      this.chatId = ''
    },
  },
  getters: {
    hasChatId: (state) => !!state.chatId,
    getChatId: (state) => state.chatId
  },
})
