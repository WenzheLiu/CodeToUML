package wenzhe.umlgen.config

import wenzhe.umlgen.ExporterConfig
import wenzhe.umlgen.FilterConfig
import wenzhe.umlgen.ClassDiagramConfig

trait AppConfig {
  val filterConfig: FilterConfig
  val exporterConfig: ExporterConfig
  val classDiagramConfig: ClassDiagramConfig
}
