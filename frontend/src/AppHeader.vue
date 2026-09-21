<template>
    <header class="topbar">
        <a class="brand" href="/" aria-label="知屿首页">
            <span class="brand-mark" aria-hidden="true">知</span>
            <span><strong>知屿</strong><small>PERSONAL KNOWLEDGE BASE</small></span>
        </a>
        <nav class="nav-tabs" aria-label="主要导航">
            <a class="nav-link" :class="{active: currentPage === 'library'}" href="/">资料管理</a>
            <a class="nav-link" :class="{active: currentPage === 'chat'}" href="/chat.html">知识问答</a>
        </nav>
        <div class="service-state" :class="{online: healthOnline}" title="后端服务状态">
            <span class="status-dot"></span>
            <span>{{ healthOnline ? "服务正常" : "服务未连接" }}</span>
        </div>
    </header>
</template>

<script>
export default {
    props: {
        currentPage: {
            type: String,
            required: true
        },
        healthOnline: {
            type: Boolean,
            required: true
        }
    }
};
</script>
