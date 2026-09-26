import {createApp} from "vue";
import PracticeApp from "./PracticeApp.vue";
import "../css/app.css";

document.body.classList.add("practice-body");
createApp(PracticeApp).mount("#app");
