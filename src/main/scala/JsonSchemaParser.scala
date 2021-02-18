package io.carrera.jsontoavroschema
import io.lemonlabs.uri.Uri
import ujson.Value

import scala.util.Try

case class RootJsonSchema(schemaUri: Option[Uri], schema: JsonSchema)

case class JsonSchema(id: Option[Uri])

object JsonSchemaParser {

  def parse(value: ujson.Value): Either[Throwable, RootJsonSchema] =
    for {
      root <- value.objOpt.toRight(ParserError("schema must be an object"))
      schemaUri <- parseSchemaUri(root)
      // The schema *should* be used to determine how to parse the rest of the document
      // For now, we are just assuming it's a draft 6 document
      schema <- parseSubSchema(root)
    } yield RootJsonSchema(schemaUri, schema)

  /**
   * $schema uri MUST ONLY be in the root schema,
   * so this is our recursive descent function
   * that ignores it.
   * */
  def parseSubSchema(obj: ujson.Obj): Either[ParserError, JsonSchema] =
    for {
      id <- parseId(obj)
    } yield JsonSchema(id)

  def parseId(obj: ujson.Obj): Either[ParserError, Option[Uri]] = {
    parseUri(obj, "$id")
  }

  def parseSchemaUri(obj: ujson.Obj): Either[ParserError, Option[Uri]] = {
    //TODO: The spec says the schema uri must include a scheme. Validate it does.
    // https://tools.ietf.org/html/draft-wright-json-schema-01#section-7
    parseUri(obj, "$schema")
  }

  private def parseUri(obj: ujson.Obj, elemName: String): Either[ParserError, Option[Uri]] = {
    Try(obj(elemName)).toOption match {
      case Some(node) => {
        for {
          uriStr <- node.strOpt.toRight(ParserError(s"$elemName must be a URI string"))
          uri <- Uri.parseOption(uriStr).toRight(ParserError(s"Invalid $elemName URI"))
        } yield Some(uri)
      }
      case None => Right(None)
    }
  }
}

final case class ParserError(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)
