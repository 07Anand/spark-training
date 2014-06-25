import java.util.Random

import org.apache.log4j.Logger
import org.apache.log4j.Level

import scala.io.Source

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}

object MovieLensALS {

  def main(args: Array[String]) {

    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)

    if (args.length != 1) {
      println("Usage: /root/spark/bin/spark-submit --master `cat ~/spark-ec2/cluster-url` "+
        "--class MovieLensALS target/scala-2.10/movielens-als-assembly-0.0.jar movieLensHomeDir")
      sys.exit(1)
    }

    // set up environment

    val masterHostname = Source.fromFile("/root/spark-ec2/masters").mkString.trim
    val conf = new SparkConf()
      .setAppName("MovieLensALS")
      .set("spark.executor.memory", "2g")
    val sc = new SparkContext(conf)

    // load ratings and movie titles

    val movieLensHomeDir = "hdfs://" + masterHostname + ":9000" + args(0)

    val ratings = sc.textFile(movieLensHomeDir + "/ratings.dat").map { line =>
      val fields = line.split("::")
      // format: (timestamp % 10, Rating(userId, movieId, rating))
      (fields(3).toLong % 10, Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble))
    }

    val movies = sc.textFile(movieLensHomeDir + "/movies.dat").map { line =>
      val fields = line.split("::")
      // format: (movieId, movieName)
      (fields(0).toInt, fields(1))
    }.collect.toMap

    // your code here

    // clean up

    sc.stop()
  }

  /** Compute RMSE (Root Mean Squared Error). */
  def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating], n: Long) = {
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
                                           .join(data.map(x => ((x.user, x.product), x.rating)))
                                           .values
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }
  
  /** Load personal ratings from file. */
  def loadPersonalRatings(): Seq[Rating] = {
    var ratings = List[Rating]()
    for (line <- Source.fromFile("../userRatings/userRatings.txt").getLines()) {
      val ls = line.split(",")
      if (ls.size == 3) {
        val rating = new Rating(0, line.split(",")(0).toInt, line.split(",")(2).toDouble)
        ratings ::= rating
      }
    }
    if (ratings.size == 0) {
      sys.error("No ratings provided.")
    } else {
      ratings.toSeq
    }
  }
}
