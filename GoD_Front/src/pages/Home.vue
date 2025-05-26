<template>
  <div class="chat-container">
    <!-- 헤더 -->
    <header class="chat-header">
      <img
        src="https://image.aladin.co.kr/product/17639/29/cover500/f402534217_1.jpg"
        alt="Profile"
        class="profile-img"
      />
      <div>
        <h2>Genie Of Document</h2>
        <p class="status">가비아 RAG 봇</p>
      </div>
    </header>

    <!-- 채팅 영역 -->
    <main class="chat-main" ref="chatMain">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="chat-bubble"
        :class="msg.type"
      >
        <!-- 버튼 타입일 경우 버튼 렌더링 -->
        <template v-if="msg.type === 'category-buttons'">
          <button
            class="category-button"
            v-for="(btn, idx) in msg.options"
            :key="idx"
            @click="selectCategory(btn.value, btn.label)"
          >
            {{ btn.label }}
          </button>
        </template>
        <!-- 일반 메시지일 경우 텍스트 출력 -->
        <template v-else>
          <p v-html="msg.text"></p>
        </template>
      </div>
    </main>

    <!-- 입력창 -->
    <div v-if="showInput" class="input-area">
      <input
        v-model="userInput"
        @keydown.enter="submitQuestion"
        type="text"
        placeholder="질문을 입력하세요..."
      />
      <button
    @click="submitQuestion"
    :disabled="isSubmitting"
    :class="{ submitting: isSubmitting }"
  >
     {{ isSubmitting ? '응답 중' : '전송' }}
  </button>    </div>
  </div>
</template>
<script setup>
import { ref, nextTick, reactive, onMounted } from 'vue'
import { connectWebSocket, sendWebSocketMessage } from '../api/chat.js'
import { useChatStore } from '../stores/store.js'
import { generateUUID } from '../api/util.js'

const messages = ref([
  {
    text: `가비아의 RAG 챗봇에 오신것을 환영합니다! 😺<br>가비아의 지니가 여러분이 <strong>원하는 정보</strong>를 찾아드릴게요!<br>그럼 시작해볼까요? 😄`,
    type: 'bot',
  },
])

const socket = ref(null)
const chatMain = ref(null)
const chatStore = useChatStore()
const showInput = ref(false)
const selectedCategory = ref('')
const userInput = ref('')
const deltaQueue = []
const isSubmitting = ref(false)

let processing = false
let currentBotMessage = null
const currentBotMessageRef = ref(null)

function processQueue(botMsg) {
  if (processing) return
  processing = true

  const run = async () => {
    while (deltaQueue.length > 0) {
      const char = deltaQueue.shift()
      botMsg.text += char // ✅ text 업데이트

      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 30))
    }

    processing = false

    if (deltaQueue.length > 0) {
      processQueue(botMsg)
    }

    if (deltaQueue.length ===0){
      isSubmitting.value = false
    }
  }

  run()
}

// WebSocket 연결
onMounted(() => {
  chatStore.setChatId(generateUUID())

  connectWebSocket(
    chatStore.getChatId,
    socket,
    deltaQueue,
    currentBotMessageRef,
    processQueue
  )

  messages.value.push({
    type: 'category-buttons',
    options: [
      { label: '문의사항', value: 'INQUIRY' },
      { label: '고객의 소리', value: 'VOC' },
    ],
  })
})

const submitQuestion = async () => {
  if (!userInput.value.trim() || isSubmitting.value) return
  isSubmitting.value = true

  const question = userInput.value
  userInput.value = ''

  messages.value.push({ text: question, type: 'user' })

  const botMsg = reactive({ text: '', type: 'bot' })
  messages.value.push(botMsg)
  currentBotMessage = botMsg
  currentBotMessageRef.value = botMsg

  await nextTick()

  try {
    await sendWebSocketMessage(question, selectedCategory.value, socket.value)

    // 🟡 processQueue가 끝나면 다시 enable
    const checkProcessing = async () => {
      while (processing) {
        await new Promise(resolve => setTimeout(resolve, 100))
      }
    }

    checkProcessing()
  } catch (error) {
    console.error('❌ 스트리밍 실패:', error)
    messages.value.push({ text: '❌ 응답 중 오류가 발생했어요.', type: 'bot' })
    isSubmitting.value = false
    await nextTick()
  }
}


const selectCategory = (value, label) => {
  selectedCategory.value = value
  messages.value.push({ text: `${label}을 선택하셨군요 😊`, type: 'bot' })
  messages.value.push({ text: '궁금하신 질문을 입력해주세요!', type: 'bot' })
  showInput.value = true
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatMain.value) {
      chatMain.value.scrollTop = chatMain.value.scrollHeight
    }
  })
}
</script>


<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Noto Sans KR', sans-serif;
}

.chat-container {
  width: 750px;
  height: 80vh;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f4f4f4;
  border-bottom: 1px solid #ddd;
}

.profile-img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  margin-right: 12px;
}

.status {
  font-size: 12px;
  color: gray;
  margin: 0;
}

.chat-main {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-bubble.bot {
  align-self: flex-start;
  background: #e8e8e8;
}

.chat-bubble.user {
  align-self: flex-end;
  background: #d4e8ff;
}

.chat-bubble.category-buttons {
  margin: 0px;
  padding: 0px;
  align-self: flex-end;
}

.category-button {
  margin-left: 10px;
  align-self: flex-end;
  border: none;
  background: #d4e8ff;
  color: black;
  padding: 12px 16px;
  cursor: pointer;
  max-width: 80%;
  font-size: 16px;
  border-radius: 16px;
  line-height: 1.6;
}

.category-button:hover{
  background-color: #e8e8e8;
  color : black;
  transform: 0.7 delay;
}

.input-area {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #eee;
  background-color: white;
}

.input-area input {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #ccc;
  font-size: 14px;
}

.input-area button {
  background: black;
  color: white;
  border: none;
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.input-area button.submitting {
  background: #888;
  color: #f5f5f5;
  cursor: not-allowed;
}
</style>
