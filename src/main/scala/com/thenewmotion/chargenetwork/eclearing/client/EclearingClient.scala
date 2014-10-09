package com.thenewmotion.chargenetwork.eclearing.client

import java.util
import javax.xml.namespace.QName
import javax.xml.ws.Service
import javax.xml.ws.soap.SOAPBinding

import com.thenewmotion.chargenetwork.eclearing.EclearingConfig
import com.thenewmotion.chargenetwork.eclearing.api.{CDR, ChargeToken, ChargePoint, EvseStatus}
import com.thenewmotion.time.Imports._
import eu.ochp._1._
import eu.ochp._1_2.{OCHP12, OCHP12Live}
import org.apache.cxf.endpoint.{Client, Endpoint}
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.joda.time.DateTime

/**
 * @param cxfClient The SOAP client generated by CXF
 */
class EclearingClient(cxfClient: OCHP12) {

  import com.thenewmotion.chargenetwork.eclearing.Converters._
  import scala.collection.JavaConverters._

  def setRoamingAuthorisationList(info: Seq[ChargeToken]): Result[ChargeToken] = {
    val req = new SetRoamingAuthorisationListRequest()
    req.getRoamingAuthorisationInfoArray.addAll(info.map(implicitly[RoamingAuthorisationInfo](_)).asJava)
    val resp = cxfClient.setRoamingAuthorisationList(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
      resp.getRefusedRoamingAuthorisationInfo.asScala.toList.map(implicitly[ChargeToken](_)))
  }

  def roamingAuthorisationList() = {
    val resp = cxfClient.getRoamingAuthorisationList(
      new GetRoamingAuthorisationListRequest)
    resp.getRoamingAuthorisationInfoArray.asScala.toList.map(implicitly[ChargeToken](_))
  }

  def setRoamingAuthorisationListUpdate(info: Seq[ChargeToken]): Result[ChargeToken] = {
    val req = new UpdateRoamingAuthorisationListRequest()
    require(info.nonEmpty, "need at least one ChargeToken to send!")
    req.getRoamingAuthorisationInfoArray.addAll(info.map(implicitly[RoamingAuthorisationInfo](_)).asJava)
    val resp = cxfClient.updateRoamingAuthorisationList(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
        resp.getRefusedRoamingAuthorisationInfo.asScala.toList.map(implicitly[ChargeToken](_)))
  }

  def roamingAuthorisationListUpdate(lastUpdate: DateTime) = {
    val req = new GetRoamingAuthorisationListUpdatesRequest
    req.setLastUpdate(toDateTimeType(lastUpdate))
    val resp = cxfClient.getRoamingAuthorisationListUpdates( req )
    resp.getRoamingAuthorisationInfo.asScala.toList.map(implicitly[ChargeToken](_))
  }

  def getCdrs() = {
    val resp: GetCDRsResponse = cxfClient.getCDRs(
      new GetCDRsRequest)
    resp.getCdrInfoArray.asScala.toList.map(implicitly[CDR](_))
  }

  def addCdrs(cdrs: Seq[CDR]) = {
    val req: AddCDRsRequest = new AddCDRsRequest()
    req.getCdrInfoArray.addAll(cdrs.map(implicitly[CDRInfo](_)).asJava)
    val resp = cxfClient.addCDRs(req)
    Result[CDR](resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
      resp.getImplausibleCdrsArray.asScala.toList.map(implicitly[CDR](_)))
  }

  def confirmCdrs(approvedCdrs: Seq[CDR], declinedCdrs: Seq[CDR]) = {
    val req = new ConfirmCDRsRequest()
    req.getApproved.addAll(approvedCdrs.map(implicitly[CDRInfo](_)).asJava)
    req.getDeclined.addAll(declinedCdrs.map(implicitly[CDRInfo](_)).asJava)
    val resp = cxfClient.confirmCDRs(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription, List())
  }

  def setChargePointList(info: Seq[ChargePoint]): Result[ChargePoint] = {
    val req = new SetChargePointListRequest()
    req.getChargepointInfoArray.addAll(info.map(implicitly[ChargePointInfo](_)).asJava)
    val resp = cxfClient.setChargepointList(req)
    Result[ChargePoint](resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
        resp.getRefusedChargePointInfo.asScala.toList.map(implicitly[ChargePoint](_)))
  }

  def chargePointList() = {
    val resp = cxfClient.getChargePointList(
      new GetChargePointListRequest)
    resp.getChargePointInfoArray.asScala.toList.map(implicitly[ChargePoint](_))
  }

  def setChargePointListUpdate(info: Seq[ChargePoint]): Result[ChargePoint] = {
    val req = new UpdateChargePointListRequest()
    req.getChargePointInfoArray.addAll(info.map(implicitly[ChargePointInfo](_)).asJava)
    val resp = cxfClient.updateChargePointList(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
        resp.getRefusedChargePointInfo.asScala.toList.map(implicitly[ChargePoint](_)))
  }

  def chargePointListUpdate(lastUpdate: DateTime) = {
    val req = new GetChargePointListUpdatesRequest
    req.setLastUpdate(toDateTimeType(lastUpdate))
    val resp = cxfClient.getChargePointListUpdates(req)
    resp.getChargePointInfoArray.asScala.toList.map(implicitly[ChargePoint](_))
  }


}



class EclearingLiveClient(cxfLiveClient: OCHP12Live) {
  import scala.collection.JavaConverters.asJavaCollectionConverter
  import com.thenewmotion.chargenetwork.eclearing.Converters.toDateTimeType

  /**
   * Only implements setting the timeToLive for the whole list,
   * not individually.
   *
   * @param evseStats
   * @param timeToLive
   */
  def updateStatus(evseStats: List[EvseStatus], timeToLive: Option[DateTime] = None) = {
    def toStatusType(evseStat: EvseStatus): EvseStatusType = {
      val est = new EvseStatusType
      est.setEvseId(evseStat.evseId)
      est.setMajor(evseStat.majorStatus.toString)
      evseStat.minorStatus foreach {minStat=> est.setMinor(minStat.toString)}
      est
    }
    val req  = new UpdateStatusRequest
    req.getEvse.addAll(evseStats map toStatusType asJavaCollection)
    timeToLive foreach {ttl=>req.setTtl(toDateTimeType(ttl))}

    val resp = cxfLiveClient.updateStatus(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription, List())
  }
}

case class Result[A](
  success: Boolean,
  description: String,
  refusedItems: List[A])

object Result  {
  def apply[A](code: String, desc: String, ref: List[A]) = {
    new Result(code == "ok", desc, ref)
  }
}



object EclearingClient {

  // need to pass the pw to the PwCallbackHandler somehow,
  // but can't pass it to the constructor, else wss4j won't be
  // able to instantiate it
  var password = ""

//  def apply(conf: EclearingConfig):EclearingClient = {
//    password = conf.password
//
//    lazy val cxfClient = EclearingClient.createCxfClient(conf)
//    new EclearingClient(cxfClient)
//  }

  def createCxfClient(conf: EclearingConfig): EclearingClient = {
    require(conf.wsUri != "", "need endpoint uri!")
    val (servicePort: QName, service: Service) = createClient(conf, conf.wsUri)
    val cxfClient = addWSSHeaders(conf, service.getPort(servicePort, classOf[OCHP12]))
    new EclearingClient(cxfClient)
  }

  def createCxfLiveClient(conf: EclearingConfig): EclearingLiveClient = {
    require(conf.liveWsUri != "", "need live endpoint uri!")
    val (servicePort: QName, service: Service) = createClient(conf, conf.liveWsUri)
    val cxfLiveClient = addWSSHeaders(conf, service.getPort(servicePort, classOf[OCHP12Live]))
    new EclearingLiveClient(cxfLiveClient)
  }

  private def createClient(conf: EclearingConfig, endpoint_address: String): (QName, Service) = {
    password = conf.password
    val servicePort: QName = new QName(endpoint_address, "service port")
    val service: Service = Service.create(null, servicePort)
    service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endpoint_address)
    (servicePort, service)
  }

  private def addWSSHeaders(conf: EclearingConfig, port: OCHP12): OCHP12 = {
    val client = ClientProxy.getClient(port)
    doAddWssHeaders(conf, client)
    port
  }

  private def addWSSHeaders(conf: EclearingConfig, port: OCHP12Live): OCHP12Live = {
    val client = ClientProxy.getClient(port)
    doAddWssHeaders(conf, client)
    port
  }

  private def doAddWssHeaders(conf: EclearingConfig, client: Client) {
    val cxfEndpoint: Endpoint = client.getEndpoint
    val outProps = new util.HashMap[String, Object]()
    outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN)
    outProps.put(WSHandlerConstants.USER, conf.user)
    outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT)
    outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS,
      new PwCallbackHandler().getClass.getName)
    val wssOut = new WSS4JOutInterceptor(outProps)
    cxfEndpoint.getOutInterceptors.add(wssOut)
  }

  import javax.security.auth.callback.{Callback, CallbackHandler}

import org.apache.wss4j.common.ext.WSPasswordCallback

  private class PwCallbackHandler  extends CallbackHandler {

    def handle( callbacks: Array[Callback]) = {
      val pc: WSPasswordCallback  = callbacks(0).asInstanceOf[WSPasswordCallback]
      pc.setPassword(password)
    }
  }

}
