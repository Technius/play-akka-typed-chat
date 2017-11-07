var chatlog = document.getElementById("chat-log");
var chatbox = document.getElementById("chat-box");

var ws = null;

var sendMessage = function(type, data) {
    var payload = {};
    payload[type] = data;
    ws.send(JSON.stringify(payload));
};

document.getElementById("chat-submit").addEventListener("submit", function(e) {
    e.preventDefault();
    var msg = chatbox.value.trim();
    if (msg != "") {
        if (ws == null) {
            ws = new WebSocket("ws://" + location.host + "/chat");
            ws.onmessage = function(e) {
                var data = JSON.parse(e.data);
                var msg = data.message;
                console.log("Received message from server:", data);
                var child = document.createElement("li");
                child.textContent = msg;
                chatlog.appendChild(child);
            };
            ws.onclose = function() {
                ws = null;
            };
            ws.onerror = function(e) {
                console.log("Error:", e);
            };
            ws.onopen = function() {
                sendMessage("Connect", { name: msg });
            };
        } else {
            sendMessage("SendMessage", { message: msg });
        }
        chatbox.value = "";
    }
});
