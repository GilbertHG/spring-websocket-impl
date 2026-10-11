const loginPanel = document.getElementById("login-panel");
const chatPanel = document.getElementById("chat-panel");
const loginForm = document.getElementById("login-form");
const loginError = document.getElementById("login-error");
const chatError = document.getElementById("chat-error");
const whoami = document.getElementById("whoami");
const connStatus = document.getElementById("conn-status");
const roomsEl = document.getElementById("rooms");
const presenceEl = document.getElementById("presence");
const messagesEl = document.getElementById("messages");
const typingEl = document.getElementById("typing");
const sendForm = document.getElementById("send-form");
const messageInput = document.getElementById("message");

let token = null;
let username = null;
let client = null;
let currentRoom = null;
const subscriptions = [];
let typingTimer = null;
let typingSent = false;

function showError(el, text) {
  el.hidden = !text;
  el.textContent = text || "";
}

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  showError(loginError, "");
  const body = {
    username: document.getElementById("username").value,
    password: document.getElementById("password").value,
  };
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    showError(loginError, "Sign-in failed");
    return;
  }
  const data = await response.json();
  token = data.token;
  username = data.username;
  whoami.textContent = username;
  loginPanel.hidden = true;
  chatPanel.hidden = false;
  await connect();
  await loadRooms();
});

document.getElementById("logout").addEventListener("click", () => {
  teardown();
  loginPanel.hidden = false;
  chatPanel.hidden = true;
});

async function api(path) {
  const response = await fetch(path, {
    headers: { Authorization: "Bearer " + token },
  });
  if (!response.ok) {
    throw new Error("Request failed: " + path);
  }
  return response.json();
}

async function connect() {
  return new Promise((resolve, reject) => {
    client = new StompJs.Client({
      brokerURL: (location.protocol === "https:" ? "wss://" : "ws://") + location.host + "/ws",
      connectHeaders: { Authorization: "Bearer " + token },
      reconnectDelay: 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connStatus.textContent = "connected";
        client.subscribe("/user/queue/errors", (frame) => {
          const body = JSON.parse(frame.body);
          showError(chatError, body.message || "Error");
        });
        resolve();
      },
      onStompError: (frame) => {
        connStatus.textContent = "error";
        reject(new Error(frame.headers.message || "STOMP error"));
      },
      onWebSocketClose: () => {
        connStatus.textContent = "disconnected";
      },
    });
    client.activate();
  });
}

async function loadRooms() {
  const rooms = await api("/api/rooms");
  roomsEl.innerHTML = "";
  rooms.forEach((room, index) => {
    const item = document.createElement("li");
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = room.name;
    button.addEventListener("click", () => joinRoom(room.id, button));
    item.appendChild(button);
    roomsEl.appendChild(item);
    if (index === 0) {
      joinRoom(room.id, button);
    }
  });
}

async function joinRoom(roomId, button) {
  currentRoom = roomId;
  roomsEl.querySelectorAll("button").forEach((el) => el.classList.remove("active"));
  button.classList.add("active");
  subscriptions.splice(0).forEach((sub) => sub.unsubscribe());
  messagesEl.innerHTML = "";
  typingEl.textContent = "";
  showError(chatError, "");

  subscriptions.push(client.subscribe("/topic/rooms." + roomId, (frame) => {
    appendMessage(JSON.parse(frame.body));
  }));
  subscriptions.push(client.subscribe("/topic/rooms." + roomId + ".typing", (frame) => {
    const event = JSON.parse(frame.body);
    if (event.username === username) {
      return;
    }
    typingEl.textContent = event.typing ? event.username + " is typing…" : "";
  }));
  subscriptions.push(client.subscribe("/topic/rooms." + roomId + ".presence", (frame) => {
    renderPresence(JSON.parse(frame.body).users || []);
  }));

  const history = await api("/api/rooms/" + roomId + "/messages");
  history.forEach(appendMessage);
  const presence = await api("/api/rooms/" + roomId + "/presence");
  renderPresence(presence.users || []);
}

function appendMessage(message) {
  const item = document.createElement("li");
  const meta = document.createElement("div");
  meta.className = "meta";
  meta.textContent = message.sender + " · " + new Date(message.sentAt).toLocaleTimeString();
  const body = document.createElement("div");
  body.textContent = message.content;
  item.appendChild(meta);
  item.appendChild(body);
  messagesEl.appendChild(item);
  messagesEl.scrollTop = messagesEl.scrollHeight;
}

function renderPresence(users) {
  presenceEl.innerHTML = "";
  users.forEach((user) => {
    const item = document.createElement("li");
    item.textContent = user;
    presenceEl.appendChild(item);
  });
}

sendForm.addEventListener("submit", (event) => {
  event.preventDefault();
  if (!currentRoom || !client || !client.connected) {
    return;
  }
  client.publish({
    destination: "/app/rooms." + currentRoom + ".chat",
    body: JSON.stringify({ content: messageInput.value }),
  });
  messageInput.value = "";
  sendTyping(false);
});

messageInput.addEventListener("input", () => {
  if (!typingSent) {
    sendTyping(true);
    typingSent = true;
  }
  clearTimeout(typingTimer);
  typingTimer = setTimeout(() => {
    sendTyping(false);
    typingSent = false;
  }, 800);
});

function sendTyping(typing) {
  if (!currentRoom || !client || !client.connected) {
    return;
  }
  client.publish({
    destination: "/app/rooms." + currentRoom + ".typing",
    body: JSON.stringify({ typing }),
  });
}

function teardown() {
  subscriptions.splice(0).forEach((sub) => sub.unsubscribe());
  if (client) {
    client.deactivate();
    client = null;
  }
  token = null;
  username = null;
  currentRoom = null;
  connStatus.textContent = "disconnected";
}
