package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Akka
import scala.Left
import scala.Right
import scala.util._
import play.api._
import play.api.Play.current
import play.api.mvc._
import akka.actor._
import play.api.libs.json._
import akka.event.Logging

object Application extends Controller {
	val UID = "uid"
	var counter = 0


	def index = Action { 
		Ok("API Kyû Screen")
	}

	def join(corporate:String, branch:String, screen:String) = Action {
		implicit request =>
			val uid: String = request.session.get(UID).getOrElse {
				counter += 1
				counter.toString
			}
			ScreenActor.join(corporate,branch,screen)
			val url = s"ws://${request.host}/ws/$corporate/$branch/$screen"
			Ok(views.html.index(url))
				.withSession(request.session + (UID -> uid))
	}

	def send(corporate:String, branch:String, screen:String,msg:String) = Action {
		Akka.system.actorSelection(s"/user/$corporate/$branch/$screen") ! JsObject(Seq(
			"mensaje" -> JsString(msg)))
		Ok(s"sent $msg!")
	} 

	def ws(corporate:String, branch:String, screen:String) = 
		WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>

			val scrn = Akka.system.actorSelection(s"/user/$corporate/$branch/$screen")
			Future.successful(request.session.get(UID) match {
				case None => Left(Forbidden)
				case Some(uid) => Right(WebSocketActor.props(uid,scrn))
			})
		}
}



class CorporateActor extends Actor{
	def receive = {
		case (Child(branch),Child(screen)) => Try{ 
			context.actorOf(Props[BranchActor], branch) ! Child(screen) 
		} match{
			case Success(_) => ()
			case Failure(err) => context.actorSelection(s"/$branch") ! Child(screen) 
		}

		case Child(name) => Try(context.actorOf(Props[BranchActor], name))
	}
}

class BranchActor extends Actor{
	def receive = {
		case Child(name) => Try(context.actorOf(Props[ScreenActor], name))
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
			wsClients foreach {_ ! msg }
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

