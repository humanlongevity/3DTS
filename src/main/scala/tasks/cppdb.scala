import java.io.File
import collection.JavaConversions._
import scala.sys.process._
import scala.concurrent._
import scala.concurrent.duration._
import tasks._
import tasks.queue.NodeLocalCache
import tasks.util.TempFile
import java.io._

import fileutils._
import stringsplit._

import IOHelpers._
import MathHelpers._
import Model._

import akka.stream.ActorMaterializer

import index2._
import SharedTypes._

case class JoinCPWithPdbInput(
    gencodeUniprot: JsDump[Ensembl2Uniprot.MapResult],
    pdbUniprot: List[JsDump[UniProtPdb.T1]])

case class CpPdbIndex(fs: Set[SharedFile])
    extends ResultWithSharedFiles(fs.toSeq: _*)

object JoinCPWithPdb {

  val CpPdbTable = Table(name = "CPPDBxTerms",
                         uniqueDocuments = true,
                         compressedDocuments = true)

  val indexCpPdb =
    AsyncTask[JsDump[PdbUniGencodeRow], CpPdbIndex]("indexcppdb", 2) {
      pdbunigencode => implicit ctx =>
        log.info("start indexing " + pdbunigencode)
        implicit val mat = ctx.components.actorMaterializer
        val tmpFolder = TempFile.createTempFile("index")
        tmpFolder.delete
        tmpFolder.mkdirs
        val tableManager = TableManager(tmpFolder)

        val writer = tableManager.writer(CpPdbTable)

        pdbunigencode.source.runForeach { pdbunigencode =>
          val pdbId = pdbunigencode._1.s
          val enst = pdbunigencode._8.s
          val uniid = pdbunigencode._5.s
          val chrpos = pdbunigencode._9.s.split1('\t')
          val pdbchain = pdbunigencode._2.s
          val pdbres = pdbunigencode._3.s

          val cp = chrpos(0) + "_" + chrpos(2)
          val js = upickle.default.write(pdbunigencode)
          writer.add(Doc(js),
                     List(enst,
                          uniid,
                          pdbId,
                          cp,
                          pdbId + "_" + pdbchain,
                          pdbId + "_" + pdbchain + "_" + pdbres))
        }.map { done =>
          writer.makeIndex(1000000, 50)
        }.flatMap { _ =>
          Future
            .sequence(tmpFolder.listFiles.toList.map(f =>
              SharedFile(f, name = f.getName)))
            .map(x => CpPdbIndex(x.toSet))
        }
    }

  val task =
    AsyncTask[JoinCPWithPdbInput, JsDump[PdbUniGencodeRow]]("cppdb-1", 3) {

      case JoinCPWithPdbInput(
          gencodeUniprot,
          pdbMaps
          ) =>
        implicit ctx =>
          // implicit val mat = ActorMaterializer()

          val uniprot2Genome: Future[
            collection.mutable.Map[String, List[String]]] =
            gencodeUniprot.sf.file.map { localFile =>
              log.info(
                "Reading UniProt -> genome map .." + gencodeUniprot.sf.name)

              val mmap =
                scala.collection.mutable.AnyRefMap[String, List[String]]()

              gencodeUniprot.iterator(localFile)(
                i =>
                  i.filter(_.isInstanceOf[Ensembl2Uniprot.Success])
                    .map(_.asInstanceOf[Ensembl2Uniprot.Success])
                    .flatMap(_.v)
                    .map { line =>
                      val uni: UniId = line._2
                      val uninum: Option[UniNumber] = line._3
                      (uni, uninum) -> upickle.default.write(line)
                    }
                    .filter(_._1._2.isDefined)
                    .map(x => (x._1._1.s + "_" + x._1._2.get.i) -> x._2)
                    .foreach {
                      case (k, line) =>
                        mmap.get(k) match {
                          case None => mmap.update(k, List(line))
                          case Some(l) => mmap.update(k, line :: l)
                        }
                  })

              log.info(
                "grouping done " + mmap.size + " " + mmap.foldLeft(0)(
                  _ + _._2.size))
              mmap

            }

          uniprot2Genome.flatMap { uniprot2Genome =>
            Future
              .sequence(pdbMaps.map {
                case jsondump =>
                  jsondump.sf.file.map(f => jsondump -> f)
              })
              .flatMap { files =>
                val iters = files.map(x => x._1.createIterator(x._2))
                val iter: Iterator[PdbUniGencodeRow] =
                  iters.map(_._1).iterator.flatMap { iter =>
                    iter.map {
                      case unipdbline =>
                        val uni: UniId = unipdbline._1
                        val uninum: UniNumber = unipdbline._7
                        val genomeLines =
                          uniprot2Genome.get(uni.s + "_" + uninum.i)
                        (genomeLines, unipdbline)
                    }.filter(_._1.isDefined)
                      .map(x => (x._1.get -> x._2))
                      .flatMap {
                        case (genomeLines,
                              (uniid,
                               pdbid,
                               pdbch,
                               pdbres,
                               pdbnum,
                               pdbaa,
                               unin,
                               uniaa,
                               unipdbmatch)) =>
                          genomeLines.iterator.map {
                            case genomeLine =>
                              val t1: Ensembl2Uniprot.T1 = upickle.default
                                .read[Ensembl2Uniprot.T1](genomeLine)
                              (pdbid,
                               pdbch,
                               pdbres,
                               pdbaa,
                               uniid,
                               unin,
                               uniaa,
                               t1._1,
                               t1._4,
                               t1._5,
                               t1._6,
                               t1._7,
                               t1._8,
                               t1._9,
                               t1._10,
                               t1._11)
                          }

                      }

                  }

                JsDump
                  .fromIterator[PdbUniGencodeRow](
                    iter,
                    gencodeUniprot.sf.name + "." + pdbMaps.hashCode)
                  .andThen { case _ => iters.foreach(_._2.close) }

              }

        }
    }

}
