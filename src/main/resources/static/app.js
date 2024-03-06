const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/bingo-connect'
});

//console.log("Hello world");

stompClient.onConnect = (frame) => {
    console.log('Connected: ' + frame);
};


stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function subscribeToRoom() {
    var roomSessionCode = $("#roomSessionCode").val();
    setTimeout(() => {}, 5000)
    stompClient.subscribe('/room/' + roomSessionCode, (message) => {
        console.log(message.body)
        let parsedMessage = JSON.parse(message.body)
        console.log(parsedMessage)
    });

}

function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function sendMockData() {

                   stompClient.publish({
                       destination: "/app/add-number",
                       body:                JSON.stringify({
                                                "session-code":  "IEEj2H",
                                                "creator-hash": "03fe0f50-81a9-4965-b918-6e1eab5ce3d2",
                                                "number": 22
                                            })
                   });
}
$(function () {
    $("#form-room-submit").click( async () => {
        let reqBody = JSON.stringify({
                            'name': $("#form-room-name").val(),
                            'description': $("#form-room-description").val()
                        })
//        console.log(reqBody)

        await fetch("http://localhost:8080/api/v1/room", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: reqBody,
        })
        .then(response => {
            console.log("Response Status:", response.status);
            return response.json();
        })
        .then(data => {
            console.log("Success:", data);
            localstorage.set("creatorHash", data.creatorHash)
            // Handle the response data as needed
        })
        .catch(error => {
            console.error("Error:", error);
            // Handle errors
        })
        .finally(() => {
            console.log("Fetch done");
        });

        console.log("fetch done??")
    });

    $( "#disconnect" ).click(() => disconnect());
    $( "#send" ).click(() => connect());

    $("connectForm").on('submit', (e) => e.preventDefault());
    $( "#connect" ).click(() => connect());
    $( "#subscribeToRoomButton" ).click(() => subscribeToRoom());
    $( "#mockSendDataButton" ).click(() => sendMockData());


    $( "#create-room-section-button" ).click(() => $("#connect-to-room-section-button").prop("disabled", true));
    $( "#connect-to-room-section-button" ).click(() => $("#create-room-section-button").prop("disabled", true));
});