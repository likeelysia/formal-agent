/* ---------------------------------------------------------------------
   Formal Agent 前端逻辑(原生 JS,无框架)

   它只干两件事:
     1. 把用户输入塞进 HTTP 请求(问答 / 图片问答 / 入库)
     2. 把 JSON 响应渲染成气泡
   业务全在后端 —— 前端就是个"遥控器"。
   --------------------------------------------------------------------- */

const messagesEl = document.getElementById('messages');
const formEl = document.getElementById('ask-form');
const questionEl = document.getElementById('question');
const imageEl = document.getElementById('image');
const previewEl = document.getElementById('preview');
const sendBtn = document.getElementById('send');

let pendingImage = null;          // 待发送的图片(用户选了但还没发)

/* ---------------- 小工具 ---------------- */

function escapeHtml(s) {
    return (s ?? '').replace(/[&<>"']/g,
        c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function addBubble(className, html) {
    const div = document.createElement('div');
    div.className = 'bubble ' + className;
    div.innerHTML = html;
    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;
    return div;
}

async function readError(res) {
    try {
        const j = await res.json();
        return j.error || JSON.stringify(j);
    } catch {
        return res.status + ' ' + res.statusText;
    }
}

/* ---------------- 渲染回答(含工具轨迹) ---------------- */

function renderAnswer(res) {
    const answer = escapeHtml(res.answer || '(空回答)').replace(/\n/g, '<br>');

    let trace = '<div class="muted small">(这题不需要查资料,直接回答)</div>';
    if (res.trace && res.trace.length) {
        const items = res.trace.map(t => `<li><code>${escapeHtml(t)}</code></li>`).join('');
        trace = `<details class="trace"><summary>它调用了 ${res.trace.length} 次工具(共 ${res.steps} 步)</summary>
                     <ol>${items}</ol></details>`;
    }
    return `<div class="answer">${answer}</div>${trace}`;
}

/* ---------------- 图片选择预览 ---------------- */

imageEl.addEventListener('change', () => {
    const file = imageEl.files[0];
    pendingImage = file || null;
    if (file) {
        previewEl.hidden = false;
        previewEl.innerHTML = `<img src="${URL.createObjectURL(file)}" alt="待提问图片">
                               <span class="muted small">${escapeHtml(file.name)}</span>`;
    } else {
        previewEl.hidden = true;
    }
});

/* ---------------- 提问 ---------------- */

formEl.addEventListener('submit', async (e) => {
    e.preventDefault();
    const question = questionEl.value.trim();
    if (!question && !pendingImage) return;

    const thumb = pendingImage
        ? `<img class="thumb" src="${URL.createObjectURL(pendingImage)}" alt="图片">` : '';
    addBubble('user', thumb + (question ? `<div>${escapeHtml(question)}</div>` : '<div>(图片提问)</div>'));

    const imageFile = pendingImage;
    questionEl.value = '';
    pendingImage = null;
    imageEl.value = '';
    previewEl.hidden = true;

    const bubble = addBubble('bot thinking', '正在思考…');
    sendBtn.disabled = true;
    try {
        const result = imageFile ? await askImage(imageFile, question) : await askText(question);
        bubble.className = 'bubble bot';
        bubble.innerHTML = renderAnswer(result);
    } catch (err) {
        bubble.className = 'bubble bot error';
        bubble.textContent = '出错了:' + err.message;
    } finally {
        sendBtn.disabled = false;
    }
});

/* ---------------- 三个接口调用 ---------------- */

async function askText(question) {
    const res = await fetch('/api/agent', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question })
    });
    if (!res.ok) throw new Error(await readError(res));
    return res.json();
}

async function askImage(file, question) {
    const fd = new FormData();
    fd.append('file', file);
    if (question) fd.append('question', question);
    // 注意:用 FormData 时千万不要自己设 Content-Type,
    //       浏览器会自动带上 multipart 的 boundary
    const res = await fetch('/api/agent', { method: 'POST', body: fd });
    if (!res.ok) throw new Error(await readError(res));
    return res.json();
}

/* ---------------- 知识库概览 ---------------- */

async function loadKnowledge() {
    const box = document.getElementById('kb');
    try {
        const data = await (await fetch('/api/knowledge?sample=5')).json();
        const sources = (data.sources || []).map(escapeHtml).join('、') || '(空)';
        const sample = (data.sample || []).map(s => `<li>${escapeHtml(s)}</li>`).join('');
        box.innerHTML = `<div class="stat"><b>${data.count}</b> 条知识点</div>
                         <div class="muted small">来源:${sources}</div>
                         <ul class="sample">${sample}</ul>`;
    } catch (e) {
        box.textContent = '加载失败:' + e.message;
    }
}

/* ---------------- 管理:上传入库 ---------------- */

document.getElementById('ingest').addEventListener('click', async () => {
    const file = document.getElementById('book').files[0];
    const key = document.getElementById('adminKey').value.trim();
    const msg = document.getElementById('ingestMsg');

    if (!file) { msg.textContent = '先选一个文件'; return; }
    msg.textContent = '上传中…(整本教材可能要几分钟)';
    try {
        const fd = new FormData();
        fd.append('file', file);
        const res = await fetch('/api/ingest', { method: 'POST', headers: { 'X-Admin-Key': key }, body: fd });
        if (!res.ok) throw new Error(await readError(res));
        const data = await res.json();
        msg.textContent = `入库成功:新增 ${data.added} 条,现有 ${data.total} 条`;
        loadKnowledge();
    } catch (e) {
        msg.textContent = '失败:' + e.message;
    }
});

loadKnowledge();
