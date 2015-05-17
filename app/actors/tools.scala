package kyu.tools

import play.api.Play.current
import play.api.libs.ws._
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import scala.concurrent.Future
import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.commons.codec.binary._
import play.api.libs.iteratee._


object TextToSpeech{
	val consume = Iteratee.consume[Array[Byte]]()
	def b64Audio(w:String):Future[String] = {
	WS.url("http://translate.google.com/translate_tts?tl=es")
		.withQueryString("q" -> w).getStream().flatMap{
			case (headers,body) =>{
				body(consume) flatMap {
					i => i.run map ( ba =>	Base64.encodeBase64String(ba))
				}
			}
		}
	}

}
