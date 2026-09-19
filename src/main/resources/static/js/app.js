const API = "/api/documents";

const TYPE_INFO = {
    txt: ["TXT", "#287f96"],
    md: ["MARKDOWN", "#765aa3"],
    markdown: ["MARKDOWN", "#765aa3"],
    pdf: ["PDF", "#c45151"],
    docx: ["DOCX", "#3f6fa8"],
    note: ["笔记", "#b27828"],
    web: ["网页", "#258269"]
};

const STATUS_TEXT = {
    PENDING: "等待处理",
    PROCESSING: "正在处理",
    READY: "已完成索引",
    FAILED: "处理失败"
};

const documents = [];
let selectedDocument;
let refreshTimer;

bindTabs();
bindCreateForms();
bindDialogs();
document.querySelector("#refresh-button").addEventListener("click", loadDocuments);
document.querySelector("#file-input").addEventListener("change", event => {
    document.querySelector("#file-label").textContent = event.target.files[0]?.name || "拖放文件到这里，或点击选择";
});
loadHealth();
loadDocuments();

function bindTabs() {
    document.querySelectorAll(".composer-tab").forEach(tab => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".composer-tab").forEach(item => {
                item.classList.toggle("active", item === tab);
                item.setAttribute("aria-selected", String(item === tab));
            });
            document.querySelectorAll(".tab-panel").forEach(panel => {
                const active = panel.id === `panel-${tab.dataset.tab}`;
                panel.classList.toggle("active", active);
                panel.hidden = !active;
            });
        });
    });
}

function bindCreateForms() {
    document.querySelector("#file-form").addEventListener("submit", event => {
        submit(event, () => request(API, {
            method: "POST",
            body: new FormData(event.currentTarget)
        }), "文件已提交，正在后台处理");
    });

    document.querySelector("#note-form").addEventListener("submit", event => {
        const data = new FormData(event.currentTarget);
        submit(event, () => request(`${API}/notes`, jsonRequest("POST", {
            title: data.get("title"),
            content: data.get("content")
        })), "笔记已提交，正在后台处理");
    });

    document.querySelector("#link-form").addEventListener("submit", event => {
        const data = new FormData(event.currentTarget);
        submit(event, () => request(`${API}/links`, jsonRequest("POST", {
            url: data.get("url"),
            title: data.get("title")
        })), "网页已提交，正在后台处理");
    });
}

async function submit(event, action, message) {
    event.preventDefault();
    const form = event.currentTarget;
    const button = form.querySelector("button[type=submit]");
    button.disabled = true;
    try {
        await action();
        form.reset();
        document.querySelector("#file-label").textContent = "拖放文件到这里，或点击选择";
        showToast(message);
        await loadDocuments();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        button.disabled = false;
    }
}

function bindDialogs() {
    document.querySelector("#delete-dialog").addEventListener("close", async event => {
        if (event.target.returnValue !== "confirm") return;
        try {
            await request(`${API}/${selectedDocument.id}`, {method: "DELETE"});
            showToast("资料已删除");
            await loadDocuments();
        } catch (error) {
            showToast(error.message, true);
        }
    });

    document.querySelector("#cancel-note-edit").addEventListener("click", () => {
        document.querySelector("#edit-note-dialog").close();
    });
    document.querySelector("#edit-note-form").addEventListener("submit", async event => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        try {
            await request(`${API}/${selectedDocument.id}`, jsonRequest("PUT", {
                name: data.get("name"),
                content: data.get("content")
            }));
            document.querySelector("#edit-note-dialog").close();
            showToast("修改已提交，正在后台处理");
            await loadDocuments();
        } catch (error) {
            showToast(error.message, true);
        }
    });

    document.querySelector("#cancel-file-replacement").addEventListener("click", () => {
        document.querySelector("#replace-file-dialog").close();
    });
    document.querySelector("#replace-file-form").addEventListener("submit", async event => {
        event.preventDefault();
        try {
            await request(`${API}/${selectedDocument.id}`, {
                method: "PUT",
                body: new FormData(event.currentTarget)
            });
            document.querySelector("#replace-file-dialog").close();
            showToast("替换已提交，正在后台处理");
            await loadDocuments();
        } catch (error) {
            showToast(error.message, true);
        }
    });
}

async function loadHealth() {
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

async function loadDocuments() {
    const loading = document.querySelector("#loading-state");
    loading.hidden = false;
    clearTimeout(refreshTimer);
    try {
        const result = await request(API);
        documents.splice(0, documents.length, ...result);
        renderDocuments();
        if (documents.some(item => ["PENDING", "PROCESSING"].includes(item.status))) {
            refreshTimer = setTimeout(loadDocuments, 2000);
        }
    } catch (error) {
        showToast(error.message, true);
    } finally {
        loading.hidden = true;
    }
}

function renderDocuments() {
    const list = document.querySelector("#document-list");
    list.replaceChildren();
    document.querySelector("#empty-state").hidden = documents.length > 0;
    document.querySelector("#total-count").textContent = documents.length;
    document.querySelector("#ready-count").textContent = documents.filter(item => item.status === "READY").length;
    document.querySelector("#chunk-count").textContent = documents.reduce((sum, item) => sum + item.chunkCount, 0);

    documents.forEach(item => {
        const type = TYPE_INFO[item.fileType] || [item.fileType, "#60758a"];
        const card = document.createElement("article");
        card.className = "document-card";
        card.style.setProperty("--type-color", type[1]);
        card.innerHTML = `
            <div class="document-card-head">
                <span class="type-badge">${escapeHtml(type[0])}</span>
                <div class="document-actions"></div>
            </div>
            <h3>${escapeHtml(item.name)}</h3>
            ${item.sourceUrl ? `<p class="document-url">${escapeHtml(item.sourceUrl)}</p>` : ""}
            ${item.failureReason ? `<p class="document-error">${escapeHtml(item.failureReason)}</p>` : ""}
            <div class="document-meta">
                <span class="ready-pill">${STATUS_TEXT[item.status] || escapeHtml(item.status)}</span>
                <span>${item.chunkCount} 个片段</span>
                <time>${new Date(item.uploadTime).toLocaleDateString("zh-CN")}</time>
            </div>`;

        const actions = card.querySelector(".document-actions");
        if (item.status === "FAILED") addButton(actions, "↻", "重新处理", () => retryDocument(item));
        if (item.fileType === "note") addButton(actions, "✎", "修改笔记", () => openNoteEditor(item));
        if (["txt", "md", "markdown", "pdf", "docx"].includes(item.fileType)) {
            addButton(actions, "↥", "替换文件", () => openFileDialog(item));
        }
        addButton(actions, "×", "删除资料", () => openDeleteDialog(item), "delete-button");
        list.append(card);
    });
}

function addButton(parent, text, title, action, className = "") {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = text;
    button.title = title;
    button.className = className;
    button.addEventListener("click", action);
    parent.append(button);
}

async function retryDocument(item) {
    try {
        await request(`${API}/${item.id}/retry`, {method: "POST"});
        showToast("已重新提交处理");
        await loadDocuments();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function openNoteEditor(item) {
    try {
        selectedDocument = await request(`${API}/${item.id}`);
        document.querySelector("#edit-note-title").value = selectedDocument.name;
        document.querySelector("#edit-note-content").value = selectedDocument.content;
        document.querySelector("#edit-note-dialog").showModal();
    } catch (error) {
        showToast(error.message, true);
    }
}

function openFileDialog(item) {
    selectedDocument = item;
    document.querySelector("#replace-document-name").textContent = item.name;
    document.querySelector("#replace-file-form").reset();
    document.querySelector("#replace-file-dialog").showModal();
}

function openDeleteDialog(item) {
    selectedDocument = item;
    document.querySelector("#delete-name").textContent = item.name;
    document.querySelector("#delete-dialog").showModal();
}

function jsonRequest(method, body) {
    return {
        method,
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(body)
    };
}

async function request(url, options) {
    const response = await fetch(url, options);
    if (response.status === 204) return;
    const body = await response.json();
    if (!response.ok) throw new Error(body.message);
    return body;
}

function escapeHtml(value) {
    const element = document.createElement("div");
    element.textContent = value;
    return element.innerHTML;
}

function showToast(message, error = false) {
    const toast = document.querySelector("#toast");
    toast.textContent = message;
    toast.className = error ? "toast error visible" : "toast success visible";
    setTimeout(() => toast.classList.remove("visible"), 3000);
}
