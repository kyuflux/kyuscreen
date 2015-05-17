/*global Rx */

(function () {
	var AUDIO_URI_CODEC = 'data:audio/mpeg;base64,';
	var createObserver = function(el){
		return Rx.Observer.create(
		function(n){
		       	var o = JSON.parse(n);	
			console.log(o);
			el.data = o.name;
		       	el.play(o.audio);	
		},
		function(){console.log('Error');},
		function(){console.log('Completed');}); 
	};

	Polymer({
		ready: function(){
			this.audio = new Audio();			
			this.ws = new WebSocket(this.wsurl);
			this.wsStreams = Rx.Observable.fromEvent(this.ws,'message')
		.map(function(x){return x.data;});
		this.wsStreams.subscribe(createObserver(this));
		},
		play: function(b64){
			this.audio.src = AUDIO_URI_CODEC+b64;
			this.audio.play();
		}
	});
})();

