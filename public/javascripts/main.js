var ws = new WebSocket(url);

var messages = Rx.Observable.fromEvent(ws,'message')
.map(function(e){return e.data});

var observer = Rx.Observer.create(
		function(n){ console.log(n) },
		function(er){ console.log("Error")},
		function(c){ console.log("Completed")});

messages.subscribe(observer);
