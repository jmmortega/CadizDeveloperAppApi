package controllers

import play.api._
import play.api.mvc._

import com.mongodb.casbah._
import play.api.libs.json.{JsArray, JsValue}
import com.mongodb.casbah.commons.MongoDBObject
import play.api.libs.json._
import org.bson.types.ObjectId
import com.mongodb.casbah.gridfs.GridFS
import java.io.{FileInputStream, ByteArrayOutputStream, InputStream}


object Application extends Controller {


  def GetUsers() = Action {

    val client = MongoClient()
    var jsonList = List[JsValue]()

    for(user <- client("CadizDevelopersAPI")("Users").find())
    {
       val obj = MongoDBObject.newBuilder

      obj += "Name" -> user.get("Name").asInstanceOf[String]
      obj += "Mail" -> user.get("Mail").asInstanceOf[String]

      jsonList ::= Json.parse(obj.result().toString())
    }

    client.close()
    Ok(JsArray(jsonList)).as("application/json")
  }

  def GetUser(id : String) = Action {

    val client = MongoClient()

    val user = client("CadizDevelopersAPI")("Users").findOneByID(id).get

    val obj = MongoDBObject.newBuilder

    obj += "Name" -> user.get("Name").asInstanceOf[String]
    obj += "Mail" -> user.get("Mail").asInstanceOf[String]

    client.close()

    Ok(Json.parse(obj.result().toString())).as("application/json")
  }

  def GetImage(id : String) = Action {

    val mongo = MongoConnection()("CadizDevelopersAPI")
    val gridFS = GridFS(mongo)

    val array = fileToBytes(gridFS.find(new ObjectId(id)).inputStream)


    Ok(array).as("image/jpeg")
  }

  def Add(name : String , mail : String) = Action {

    val client = MongoClient()

    val users = client("CadizDevelopersAPI")("Users")

    val user = MongoDBObject.newBuilder

    user += "Name" -> name
    user += "Mail" -> mail

    val result = users.insert(user.result()).getError()

    Ok(result).as("application/json")
  }

  def AddImage(id : String) = Action(parse.anyContent) {

    request =>
      request.body.asRaw match{
        case Some(bytes) => {
          val srcImage = bytes.asFile

          try
          {
            val mongo = MongoConnection()("CadizDevelopersAPI")

            val gridFs = GridFS(mongo)

            val client = MongoClient()
            val user = client("CadizDevelopersAPI")("Users").findOneByID(id).get

            if(user != null)
            {
              var gsFile = gridFs.createFile(srcImage)
              gsFile.filename = user.get("Name").asInstanceOf[String]
              gsFile.save()

              Ok("Image Stored").as("application/json")
            }
            else
            {
              BadRequest("No user")
            }
          }
        }
      }
  }

  def fileToBytes(inStream : InputStream) : Array[Byte] = {
    val outStream = new ByteArrayOutputStream
    try {
      var reading = true
      while ( reading ) {
        inStream.read() match {
          case -1 => reading = false
          case c => outStream.write(c)
        }
      }
      outStream.flush()
    }
    finally
    {
      inStream.close()
    }

    return outStream.toByteArray
  }
  
}