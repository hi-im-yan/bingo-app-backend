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
        let drawnNumbers = JSON.parse(message.body).drawnNumbers
        let drawnNumbersList = drawnNumbers.split(",");
        $("#drawn-number").html(`Drawn Number: ${drawnNumbersList[drawnNumbersList.length - 1]}`)
        $("#all-drawn-numbers").html(`History: ${drawnNumbers}`)
        console.log(parsedMessage)
    });
    localStorage.setItem("sessionCode", roomSessionCode)
}

function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function addNumber() {
   stompClient.publish({
       destination: "/app/add-number",
       body:                JSON.stringify({
                                "session-code":  localStorage.getItem('sessionCode'),
                                "creator-hash": localStorage.getItem('creatorHash'),
                                "number": localStorage.getItem('selectedNumber'),
                            })
   });
   $("#btn-confirmar-numero").prop('disabled', true)
   $("#btn-confirmar-numero").html(`Confirm`)
   $("#last-number").html(`Last number: ${localStorage.getItem('selectedNumber')}`)
}

function selectNumber(number) {
    localStorage.setItem('selectedNumber', number);
    $("#btn-confirmar-numero").prop('disabled', false)
    $("#btn-confirmar-numero").html(`Confirm: ${number}`)
}

connect()
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
            localStorage.setItem("creatorHash", data.creatorHash)
            localStorage.setItem("sessionCode", data.sessionCode)
            $("#GM-interface").prop("style", "display:true");
            $("#room-creator-section").prop("style", "display:none");
            $("#inviteCode").html(`Invite Code: ${data.sessionCode}`)

            // Handle the response data as needed
        })
        .catch(error => {
            console.error("Error:", error);
            alert("Error creating room, try again with another room name.")
            // Handle errors
        })
    });


    $("connectForm").on('submit', (e) => e.preventDefault());
    $( "#connect" ).click(() => connect());
    $( "#subscribeToRoomButton" ).click(() => {
        subscribeToRoom()
        $("#room-player-section").prop("style", "display:none");
        $("#player-interface").prop("style", "display:true");
    });
    $( "#mockSendDataButton" ).click(() => sendMockData());


    $( "#create-room-section-button" ).click(() => {
        $("#connect-to-room-section-button").prop("disabled", true);
        $("#room-creator-section").prop("style", "display:true");
    });

    $( "#connect-to-room-section-button" ).click(() => {
        $("#create-room-section-button").prop("disabled", true);
        $("#room-player-section").prop("style", "display:true");
    });

    // Button click event for B
    $("#bingo-btn-b").click(() => {
        $("#bingo-btn-b").prop("class", "btn btn-info");
        $("#bingo-btn-i").prop("class", "btn btn-primary");
        $("#bingo-btn-n").prop("class", "btn btn-primary");
        $("#bingo-btn-g").prop("class", "btn btn-primary");
        $("#bingo-btn-o").prop("class", "btn btn-primary");

        $("#bingo-numbers-b").prop("style", "display:block");
        $("#bingo-numbers-i").prop("style", "display:none");
        $("#bingo-numbers-n").prop("style", "display:none");
        $("#bingo-numbers-g").prop("style", "display:none");
        $("#bingo-numbers-o").prop("style", "display:none");
    });

    // Button click event for I
    $("#bingo-btn-i").click(() => {
        $("#bingo-btn-b").prop("class", "btn btn-primary");
        $("#bingo-btn-i").prop("class", "btn btn-info");
        $("#bingo-btn-n").prop("class", "btn btn-primary");
        $("#bingo-btn-g").prop("class", "btn btn-primary");
        $("#bingo-btn-o").prop("class", "btn btn-primary");

        $("#bingo-numbers-b").prop("style", "display:none");
        $("#bingo-numbers-i").prop("style", "display:block");
        $("#bingo-numbers-n").prop("style", "display:none");
        $("#bingo-numbers-g").prop("style", "display:none");
        $("#bingo-numbers-o").prop("style", "display:none");
    });

    // Button click event for N
    $("#bingo-btn-n").click(() => {
        $("#bingo-btn-b").prop("class", "btn btn-primary");
        $("#bingo-btn-i").prop("class", "btn btn-primary");
        $("#bingo-btn-n").prop("class", "btn btn-info");
        $("#bingo-btn-g").prop("class", "btn btn-primary");
        $("#bingo-btn-o").prop("class", "btn btn-primary");

        $("#bingo-numbers-b").prop("style", "display:none");
        $("#bingo-numbers-i").prop("style", "display:none");
        $("#bingo-numbers-n").prop("style", "display:block");
        $("#bingo-numbers-g").prop("style", "display:none");
        $("#bingo-numbers-o").prop("style", "display:none");
    });

    // Button click event for G
    $("#bingo-btn-g").click(() => {
        $("#bingo-btn-b").prop("class", "btn btn-primary");
        $("#bingo-btn-i").prop("class", "btn btn-primary");
        $("#bingo-btn-n").prop("class", "btn btn-primary");
        $("#bingo-btn-g").prop("class", "btn btn-info");
        $("#bingo-btn-o").prop("class", "btn btn-primary");

        $("#bingo-numbers-b").prop("style", "display:none");
        $("#bingo-numbers-i").prop("style", "display:none");
        $("#bingo-numbers-n").prop("style", "display:none");
        $("#bingo-numbers-g").prop("style", "display:block");
        $("#bingo-numbers-o").prop("style", "display:none");
    });

    // Button click event for O
    $("#bingo-btn-o").click(() => {
        $("#bingo-btn-b").prop("class", "btn btn-primary");
        $("#bingo-btn-i").prop("class", "btn btn-primary");
        $("#bingo-btn-n").prop("class", "btn btn-primary");
        $("#bingo-btn-g").prop("class", "btn btn-primary");
        $("#bingo-btn-o").prop("class", "btn btn-info");

        $("#bingo-numbers-b").prop("style", "display:none");
        $("#bingo-numbers-i").prop("style", "display:none");
        $("#bingo-numbers-n").prop("style", "display:none");
        $("#bingo-numbers-g").prop("style", "display:none");
        $("#bingo-numbers-o").prop("style", "display:block");
    });
});