export async function fetchChatStream(question, onDelta, category) {
    const response = await fetch(
      `http://localhost:8080/api/ask?question=${encodeURIComponent(question)}&questionType=${encodeURIComponent(category)}`
    )
    console.log(response.formData)
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let partial = ''
  
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
  
      partial += decoder.decode(value, { stream: true })
  
      const lines = partial.split('\n')
      partial = lines.pop() || ''
  
      for (const line of lines) {
        if (!line.trim()) continue
  
        try {
          // ✅ SSE 형식 처리: "data: ..." 제거
          const clean = line.startsWith('data:') ? line.slice(5).trim() : line.trim()
  
          // ✅ 스트림 종료 처리
          if (clean === '[DONE]') {
            console.log('✅ 스트리밍 종료')
            return
          }
  
          // ✅ JSON 파싱 + 델타 추출
          const parsed = JSON.parse(clean)
          const delta = parsed.choices?.[0]?.delta?.content || ''
          onDelta(delta)
        } catch (e) {
          console.warn('❌ JSON 파싱 오류:', e, '\n👉 원본 line:', line)
        }
      }
    }
  }
  
// chat.js
export function connectWebSocket(chatId, socketRef, deltaQueue, currentBotMessageRef, processQueue) {
  const url = `ws://localhost:8080/ws/chat?userId=${chatId}`
  const socket = new WebSocket(url)
  socketRef.value = socket

  socket.onopen = () => {
    console.log('[WebSocket] 연결 성공')
  }

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if(data != null) {
        const formatted = data.replace(/\n/g, '<br>') // 줄바꿈 → <br>
        deltaQueue.push(formatted) // 글자 단위 push
        processQueue(currentBotMessageRef.value) // ref 객체의 value 넘김
      }
    } catch (err) {
      console.error('[WebSocket] 메시지 파싱 오류:', err)
    }
  }

  socket.onclose = () => {
    console.log('[WebSocket] 연결 종료')
  }

  socket.onerror = (error) => {
    console.error('[WebSocket] 오류 발생:', error)
  }
}



// WebSocket 메시지 전송 함수
export function sendWebSocketMessage(question, questionType, socket) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    const payload = {
      question,
      questionType,
    }
    socket.send(JSON.stringify(payload))
  } else {
    console.warn('[WebSocket] 아직 연결되지 않았습니다.')
  }
}
