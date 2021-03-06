/*
 * The MIT License
 *
 * Copyright (c) 2015 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland,
 * Group Fellay
 * Modified work, Copyright (c) 2016 Istvan Bartha

 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tasks.util.config

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import java.io.File
import tasks._
import scala.util.Try

class TasksConfig(val raw: Config) {

  val asString = raw.root.render

  val cacheEnabled = raw.getBoolean("tasks.cache.enabled")

  val cachePath: String = raw.getString("tasks.cache.path")

  val cacheType = raw.getString("tasks.cache.store")

  val askInterval: FD = raw.getDuration("tasks.askInterval")

  val proxyTaskGetBackResult: FD =
    raw.getDuration("tasks.proxytaskGetBackResultTimeout")

  val launcherActorHeartBeatInterval: FD =
    raw.getDuration("tasks.failuredetector.heartbeat-interval")

  val fileSendChunkSize = raw.getBytes("tasks.fileSendChunkSize").toInt

  val resubmitFailedTask = raw.getBoolean("tasks.resubmitFailedTask")

  val logToStandardOutput = raw.getBoolean("tasks.stdout")

  val verifySharedFileInCache = raw.getBoolean("tasks.verifySharedFileInCache")

  val disableRemoting = raw.getBoolean("tasks.disableRemoting")

  val nonLocalFileSystems = raw
    .getStringList("tasks.nonLocalFileSystems")
    .map(f => new java.io.File(f))

  val skipContentHashVerificationAfterCache =
    raw.getBoolean("tasks.skipContentHashVerificationAfterCache")

  val acceptableHeartbeatPause: FD =
    raw.getDuration("tasks.failuredetector.acceptable-heartbeat-pause")

  val remoteCacheAddress = raw.getString("hosts.remoteCacheAddress")

  val masterAddress = raw.getString("hosts.master")

  val hostNumCPU = raw.getInt("hosts.numCPU")

  val hostRAM = raw.getInt("hosts.RAM")

  val hostName = raw.getString("hosts.hostname")

  val hostReservedCPU = raw.getInt("hosts.reservedCPU")

  val hostPort = raw.getInt("hosts.port")

  val auxThreads = raw.getInt("tasks.auxThreads")

  val storageURI =
    new java.net.URI(raw.getString("tasks.fileservice.storageURI"))

  val fileServiceExtendedFolders = raw
    .getStringList("tasks.fileservice.extendedFolders")
    .map(x => new File(x))
    .toList
    .filter(_.isDirectory)

  val fileServiceBaseFolderIsShared =
    raw.getBoolean("tasks.fileservice.baseFolderIsShared")

  val fileServiceThreadPoolSize =
    raw.getInt("tasks.fileservice.threadPoolSize")

  val sshHosts = raw.getObject("tasks.elastic.ssh.hosts")

  val gridEngine = raw.getString("tasks.elastic.engine")

  val idleNodeTimeout: FD = raw.getDuration("tasks.elastic.idleNodeTimeout")

  val maxNodes = raw.getInt("tasks.elastic.maxNodes")

  val maxPendingNodes = raw.getInt("tasks.elastic.maxPending")

  val maxNodesCumulative = raw.getInt("tasks.elastic.maxNodesCumulative")

  val queueCheckInterval: FD =
    raw.getDuration("tasks.elastic.queueCheckInterval")

  val queueCheckInitialDelay: FD =
    raw.getDuration("tasks.elastic.queueCheckInitialDelay")

  val nodeKillerMonitorInterval: FD =
    raw.getDuration("tasks.elastic.nodeKillerMonitorInterval")

  val jvmMaxHeapFactor = raw.getDouble("tasks.elastic.jvmMaxHeapFactor")

  val logQueueStatus = raw.getBoolean("tasks.elastic.logQueueStatus")

  val endpoint: String = raw.getString("tasks.elastic.aws.endpoint")

  val spotPrice: Double = raw.getDouble("tasks.elastic.aws.spotPrice")

  val amiID: String = raw.getString("tasks.elastic.aws.ami")

  val slaveInstanceType = raw.getString("tasks.elastic.aws.instanceType")

  val securityGroup: String = raw.getString("tasks.elastic.aws.securityGroup")

  val securityGroups: List[String] =
    raw.getStringList("tasks.elastic.aws.securityGroups").toList

  val subnetId = raw.getString("tasks.elastic.aws.subnetId")

  val keyName = raw.getString("tasks.elastic.aws.keyName")

  val awsNumCPUOverride =
    Try(raw.getInt("tasks.elastic.aws.numCPUOverride")).toOption

  val awsRamOverride =
    Try(raw.getInt("tasks.elastic.aws.RAMOverride")).toOption

  val additionalJavaCommandline =
    raw.getString("tasks.elastic.javaCommandline")

  val iamRole = {
    val s = raw.getString("tasks.elastic.aws.iamRole")
    if (s == "" || s == "-") None
    else Some(s)
  }

  val placementGroup: Option[String] =
    raw.getString("tasks.elastic.aws.placementGroup") match {
      case x if x == "" => None
      case x => Some(x)
    }

  val s3Region = raw.getString("tasks.s3.region")

  val s3ServerSideEncryption = raw.getBoolean("tasks.s3.serverSideEncryption")

  val s3CannedAcl = raw.getStringList("tasks.s3.cannedAcls").toList

  val s3GrantFullControl = raw
    .getStringList("tasks.s3.grantFullControl")
    .grouped(2)
    .map(x => x(0) -> x(1))
    .toList

  val instanceTags = raw
    .getStringList("tasks.elastic.aws.tags")
    .grouped(2)
    .map(x => x(0) -> x(1))
    .toList

  val terminateMaster = raw.getBoolean("tasks.elastic.aws.terminateMaster")

}
