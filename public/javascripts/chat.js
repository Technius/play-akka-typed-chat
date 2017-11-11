var chatlog = document.getElementById("chat-log");
var chatbox = document.getElementById("chat-box");

var ws = null;

var sendMessage = function(type, data) {
    var payload = {};
    payload[type] = data;
    ws.send(JSON.stringify(payload));
};

var handleChatEvent = function(event) {
    var atBottom = chatlog.scrollTop == chatlog.scrollTopMax;

    if (event.type === "Message") {
        var msg = event.message;
        console.log("Received message from server:", event);
        var child = document.createElement("li");
        child.textContent = msg;
        chatlog.appendChild(child);

        // auto scroll if at bottom of chat log
        if (atBottom) {
            chatlog.scrollTop = chatlog.scrollHeight;
        }
    } else if (event.type === "Disconnected") {
        var reason = event.reason;
        alert("Disconnected from server: " + reason);
    }
};

var connect = function(name) {
    ws = new WebSocket("ws://" + location.host + "/chat");
    ws.onmessage = function(e) {
        handleChatEvent(JSON.parse(e.data));
    };
    ws.onclose = function() {
        chatlog.classList.add("hidden");
        ws = null;
    };
    ws.onerror = function(e) {
        console.log("Error:", e);
        alert("A connection error occurred.");
    };
    ws.onopen = function() {
        chatlog.classList.remove("hidden");
        sendMessage("Connect", { name: name });
    };
};

document.getElementById("chat-submit").addEventListener("submit", function(e) {
    e.preventDefault();
    var msg = chatbox.value.trim();
    if (msg !== "") {
        if (ws === null) {
            connect(msg);
        } else {
            sendMessage("SendMessage", { message: msg });
        }
        chatbox.value = "";
    }
});
