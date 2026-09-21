import {createApp} from "vue";
import ChatApp from "./ChatApp.vue";
import "../css/app.css";

document.body.classList.add("chat-body");
createApp(ChatApp).mount("#app");
