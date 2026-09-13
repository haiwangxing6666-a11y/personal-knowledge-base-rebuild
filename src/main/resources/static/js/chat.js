const input = document.querySelector("#question-input");
const form = document.querySelector("#chat-form");
const messages = document.querySelector("#messages");
const sendButton = document.querySelector("#send-button");

document.querySelectorAll("[data-question]").forEach(button => {
    button.addEventListener("click", () => {
        input.value = button.dataset.question;
        updateCount();
        input.focus();
    });
});

input.addEventListener("input", updateCount);
input.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        form.requestSubmit();
    }
});
document.querySelector("#clear-chat").addEventListener("click", () => {
    messages.replaceChildren(createEmptyState());
});
form.addEventListener("submit", askQuestion);
checkHealth();

async function askQuestion(event) {
    event.preventDefault();
    const question = input.value.trim();
    if (!question) return;

    document.querySelector("#chat-empty")?.remove();
    appendUserMessage(question);
    input.value = "";
    updateCount();
    sendButton.disabled = true;
    const loading = appendAssistantMessage("正在检索知识库……");

    try {
        const result = await request("/api/chat", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({question})
        });
        loading.remove();
        appendAnswer(result);
    } catch (error) {
        loading.querySelector(".message-bubble").textContent = `请求失败：${error.message}`;
    } finally {
        sendButton.disabled = false;
        input.focus();
    }
}

function appendUserMessage(question) {
    const message = document.createElement("article");
    message.className = "message user";
    const content = document.createElement("div");
    content.className = "message-content";
    const bubble = document.createElement("div");
    bubble.className = "message-bubble";
    bubble.textContent = question;
    content.append(bubble);
    message.append(content);
    messages.append(message);
}

function appendAssistantMessage(text, markdown = false) {
    const message = document.createElement("article");
    message.className = "message assistant";
    message.innerHTML = '<span class="message-icon">知</span><div class="message-content"><div class="message-bubble"></div></div>';
    const bubble = message.querySelector(".message-bubble");
    if (markdown) {
        bubble.classList.add("markdown-answer");
        bubble.innerHTML = DOMPurify.sanitize(marked.parse(text), {USE_PROFILES: {html: true}});
    } else {
        bubble.textContent = text;
    }
    messages.append(message);
    messages.scrollTop = messages.scrollHeight;
    return message;
}

function appendAnswer(result) {
    const message = appendAssistantMessage(result.answer, true);
    if (result.refused) message.classList.add("refused");
    const content = message.querySelector(".message-content");

    if (result.secondSearchExecuted) {
        const trace = document.createElement("div");
        trace.className = "retrieval-trace";
        trace.textContent = `已执行二次检索：${result.rewrittenQuestion}`;
        content.append(trace);
    }

    if (result.sources.length > 0) {
        const block = document.createElement("section");
        block.className = "sources-block";
        const title = document.createElement("div");
        title.className = "sources-title";
        title.textContent = `◎ 回答来源 · ${result.sources.length}`;
        const list = document.createElement("div");
        list.className = "source-list";
        result.sources.forEach(source => list.append(createSource(source)));
        block.append(title, list);
        content.append(block);
    }
}

function createSource(source) {
    const card = document.createElement(source.sourceUrl ? "a" : "div");
    card.className = "source-card";
    if (source.sourceUrl) {
        card.href = source.sourceUrl;
        card.target = "_blank";
    }
    const name = document.createElement("strong");
    name.textContent = source.documentName;
    const detail = document.createElement("span");
    detail.textContent = `${source.sourceType} · 片段 ${source.chunkIndexes.join(", ")}`;
    card.append(name, detail);
    return card;
}

function createEmptyState() {
    const empty = document.createElement("div");
    empty.className = "chat-empty";
    empty.id = "chat-empty";
    empty.innerHTML = '<span class="empty-orbit"><i>知</i></span><h2>向你的知识岛提问</h2><p>回答只来自已经录入的资料。</p>';
    return empty;
}

function updateCount() {
    document.querySelector("#question-count").textContent = input.value.length;
}

async function checkHealth() {
    const state = document.querySelector("#service-state");
    const text = document.querySelector("#service-state-text");
    try {
        await request("/api/health");
        state.classList.add("online");
        text.textContent = "服务正常";
    } catch {
        text.textContent = "服务未连接";
    }
}

async function request(url, options) {
    const response = await fetch(url, options);
    const body = await response.json();
    if (!response.ok) throw new Error(body.message);
    return body;
}
