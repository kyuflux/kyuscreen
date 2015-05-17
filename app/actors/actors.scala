package kyu.actors
import scala.concurrent.Future
import play.api.libs.concurrent.Akka
import scala.util._
import play.api._
import play.api.Play.current
import akka.actor._
import play.api.libs.json._
import akka.event.Logging
import kyu.tools.TextToSpeech._
import scala.concurrent.ExecutionContext.Implicits.global

class CorporateActor extends Actor{
	def receive = {
		case (Child(branch),Child(screen)) => Try{ 
			context.actorOf(Props[BranchActor], branch) ! Child(screen) 
			} match{
				case Success(_) => ()
				case Failure(err) => context.actorSelection(s"$branch") ! Child(screen) 
			}

				case Child(name) => Try( context.actorOf(Props[BranchActor], name) ! Child(name) )
	}
}

class BranchActor extends Actor{
	val log = Logging(context.system,this)
	def receive = {
		case Child(name) => 
			log.info(name)
			Try(context.actorOf(Props[ScreenActor], name))
	}
}

class ScreenActor extends Actor{
	val log = Logging(context.system,this)
	var wsClients = Set[ActorRef]()
	override def preStart = log.info(s"starting ... ${self.path.name}")
	def receive = {
		case Subscribe => 
			wsClients += sender()
			context watch sender()
			log.info("subscribing")
		case msg:JsValue => 
			log.info(msg.toString)
			 val name = (msg \ "name").as[String]
			 b64Audio(name) map{ w => 
				 wsClients foreach {_ ! JsObject(Seq("name"-> JsString(name),"audio" -> JsString(w)))}
			}		
		case Terminated(wsclt) => wsClients -= wsclt
	}
}

object ScreenActor{
	def join(corporate:String,branch:String,screen:String) = 
		Try{Akka.system.actorOf(Props[CorporateActor],corporate)}
	match{
		case Success(ar) => 
			ar ! (Child(branch),Child(screen))
		case Failure(err) => 
			Akka.system.actorSelection(s"/user/$corporate") ! (Child(branch),Child(screen))
	}

}

		case class Child(name:String)
		object Subscribe
		object WebSocketActor{
			def props(uid:String,screen:ActorSelection)(out:ActorRef) = Props(new WebSocketActor(uid,out,screen))
		}
		class WebSocketActor(uid:String, out:ActorRef, screen:ActorSelection) extends Actor{
			override def preStart() = screen ! Subscribe 
			val log = Logging(context.system,this)
			def receive = {
				case msg:JsValue => 
					log.info( msg toString ) 
					out ! msg 
			}
		}

