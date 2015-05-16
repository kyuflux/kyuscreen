package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Akka
import scala.Left
import scala.Right
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
			Ok(views.html.index("Your new application is ready."))
				.withSession(request.session + (UID -> uid))
	}

	def ws(corporate:String, branch:String, screen:String) = 
		WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
		Future.successful(request.session.get(UID) match {
			case None => Left(Forbidden)
			case Some(uid) => Right(WebSocketActor.props(uid))
		})
	}
}

object WebSocketActor{
	def props(uid:String)(out:ActorRef) = Props(new WebSocketActor(uid,out))
}
class WebSocketActor(uid:String, out:ActorRef) extends Actor{
	val log = Logging(context.system,this)
	def receive = {
		case msg:JsValue => 
			log.info( (msg \ "name").as[String] ) 
			out ! msg 
	}
}

