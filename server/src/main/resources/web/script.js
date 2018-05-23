var socket;

var msg = "<MSG>";

$(document).ready(
    function () {
        socket = new WebSocket("ws://" + window.location.host + "/ws");
        socket.onopen = function() {
            console.log("Соединение установлено.");
        };
        socket.onmessage = onMessage;

        $("#loginRegisterButton").click(
            function () {
                socket.send("<CRED_RESP>" + $("#loginInput").val() + " " + $("#passwordInput").val());
            }
        )

        $("#sendMessageButton").click(
            function () {
                socket.send(msg + $("#messageInput").val())
            }
        );
    }
);

function onMessage(event) {
    var message = event.data;
    if (!message.startsWith(msg)){
        alert(message)
    }
    if(message.startsWith(msg)) {
        message = message.replace(msg, "")
        $("#messagesArea").append(message + "\n")
    }
    if(message.startsWith("<AUTH_SUCCESS>")) {
        $("#authorizationDiv").hide()
        $("#messagingDiv").show()
    }
}