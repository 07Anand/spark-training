import spark.SparkContext
import spark.SparkContext._
import spark.util.Vector

import scala.util.Random
import scala.io.Source

object WikipediaKMeans {
  def parseVector(line: String): Vector = {
      return new Vector(line.split(',').map(_.toDouble))
  }

  // Add any new functions you need here
  
  def main(args: Array[String]) {
    val sparkHome = "/root/spark"
    val jarFile = "target/scala-2.9.2/wikipedia-kmeans_2.9.2-0.0.jar"
    val master = Source.fromFile("/root/spark-ec2/cluster-url").mkString.trim
    val masterHostname = Source.fromFile("/root/spark-ec2/masters").mkString.trim

    val sc = new SparkContext(master, "WikipediaKMeans", sparkHome, Seq(jarFile))

    val K = 10
    val convergeDist = 1e-6

    val data = sc.sequenceFile[String, String](
        "hdfs://" + masterHostname + ":9000/wikistats_featurized").map(
            t => (t._1,  parseVector(t._2))).cache()

    // Your code goes here

    sc.stop()
    System.exit(0)
  }
}
