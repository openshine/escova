package com.openshine.escova.exceptions

import org.elasticsearch.ElasticsearchParseException

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
class NoSuchDateFieldException(msg: String)
    extends ElasticsearchParseException(msg, Array())

object NoSuchDateFieldException {
  def fromFieldName(fieldName: String) =
    new NoSuchDateFieldException(s"No such date field $fieldName exists")
}
