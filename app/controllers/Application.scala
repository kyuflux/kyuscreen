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
import kyu.actors._

object Application extends Controller {
	val UID = "uid"
	var counter = 0


	def index = Action { 
		Ok("API Kyû Screen")
	}

	def join(corporate:String, branch:String, screen:String) = Action {
		implicit request =>
			val url = s"ws://${request.host}/ws/$corporate/$branch/$screen"
			val uid: String = request.session.get(UID).getOrElse {
				counter += 1
				counter.toString
			}
			ScreenActor.join(corporate,branch,screen)
			Ok(views.html.index(url))
				.withSession(request.session + (UID -> uid))
	}

	def send(corporate:String, branch:String, screen:String,msg:String) = Action {
		Akka.system.actorSelection(
			s"/user/$corporate/$branch/$screen") ! JsObject(Seq(
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




