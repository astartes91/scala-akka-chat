var socket;
$(document).ready(
    function () {
        socket = new WebSocket("ws://" + window.location.host + "/ws");
        socket.onopen = function() {
            console.log("Соединение установлено.");
        };
        socket.onmessage = onMessage;

        $("#loginRegisterButton").click(
            function () {
                socket.send("<CRED_RESP>" + $("#loginRegisterButton").val() + " " + $("#passwordInput").val());
            }
        )
    }
);

function onMessage(event) {
    alert(event.data)
}