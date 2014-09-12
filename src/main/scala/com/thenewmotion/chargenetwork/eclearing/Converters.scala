package com.thenewmotion.chargenetwork.eclearing

import api._
import com.thenewmotion.chargenetwork.eclearing.api.BillingItem
import com.thenewmotion.chargenetwork.eclearing.api.CdrPeriod
import com.thenewmotion.chargenetwork.eclearing.api.ChargePointStatus.ChargePointStatus
import com.thenewmotion.chargenetwork.eclearing.api.ConnectorFormat
import com.thenewmotion.chargenetwork.eclearing.api.ConnectorStandard
import eu.ochp._1.{ConnectorType => GenConnectorType, EvseImageUrlType => GenEvseImageUrlType, EmtId => GenEmtId, CdrStatusType => GenCdrStatusType, ConnectorFormat => GenConnectorFormat, ConnectorStandard => GenConnectorStandard, CdrPeriodType => GenCdrPeriodType, BillingItemType => GenBillingItemType, _}
import org.joda.time.DateTime


/**
 * @author Yaroslav Klymko
 */
object Converters{
  import scala.collection.JavaConverters._

  implicit def roamingAuthorisationInfoToCard(rai: RoamingAuthorisationInfo): Card = {
    Card(
      contractId = rai.getContractId,
      emtId = EmtId(
        tokenId = rai.getEmtId.getInstance,
        tokenType = TokenType.withName(rai.getEmtId.getTokenType),
        tokenSubType = Some(TokenSubType.withName(rai.getEmtId.getTokenSubType))),
      printedNumber = Some(rai.getPrintedNumber),
      expiryDate = DateTimeNoMillis(rai.getExpiryDate.getDateTime)
    )
  }

  implicit def cardToRoamingAuthorisationInfo(card: Card): RoamingAuthorisationInfo = {
    import card._

    val rai = new RoamingAuthorisationInfo()
    val emtId = new GenEmtId()
    rai.setContractId(contractId)
    emtId.setInstance(card.emtId.tokenId)
    emtId.setTokenType(card.emtId.tokenType.toString)
    card.emtId.tokenSubType map {st => emtId.setTokenSubType(st.toString)}
    emtId.setRepresentation("plain")
    rai.setEmtId(emtId)
    val expDate = new DateTimeType()
    expDate.setDateTime(expiryDate.toString)
    rai.setExpiryDate(expDate)
    rai
  }

  private def toOption (value: String):Option[String] = {
    value match {case null => None; case s if !s.isEmpty => Some(s); case _ => None}
  }

  private def toDateTimeOption (value: DateTimeType):Option[DateTime] = {
    value match {case null => None; case s if !s.getDateTime.isEmpty => Some(DateTimeNoMillis(s.getDateTime)); case _ => None}
  }

  private def toGeoPointOption (value: GeoPointType):Option[GeoPoint] = {
    value match {
      case null => None
      case gp: GeoPointType if (gp.getLat != null) && (gp.getLon != null) => Some(GeoPoint(gp.getLat, gp.getLon))
    }
  }

  private def toChargePointStatusOption(value: ChargePointStatusType): Option[ChargePointStatus] = {
    value match {
      case null => None
      case cps: ChargePointStatusType if !cps.getChargePointStatusType.isEmpty =>
        Some(ChargePointStatus.withName(cps.getChargePointStatusType))
      case _ => None
    }
  }

  private def toHoursOption (value: HoursType):Option[Hours] = {
    value match {
      case null => None;
      case hrs: HoursType => Some(Hours(
        regularHours = hrs.getRegularHours.asScala.toList map {rh =>
          RegularHours(
            rh.getWeekday,
            TimeNoSecs(rh.getPeriodBegin),
            TimeNoSecs(rh.getPeriodEnd))},
        exceptionalOpenings = hrs.getExceptionalOpenings.asScala.toList map {eo =>
          ExceptionalPeriod(
            periodBegin = TimeNoSecs(eo.getPeriodBegin.getDateTime),
            periodEnd = TimeNoSecs(eo.getPeriodEnd.getDateTime)
          )},
        exceptionalClosings = hrs.getExceptionalClosings.asScala.toList map {ec =>
          ExceptionalPeriod(
            periodBegin = TimeNoSecs(ec.getPeriodBegin.getDateTime),
            periodEnd = TimeNoSecs(ec.getPeriodEnd.getDateTime)
          )}))
    }
  }

    implicit def cdrInfoToCdr(cdrinfo: CDRInfo): CDR = {
    CDR(
      cdrId = cdrinfo.getCdrId,
      evseId = cdrinfo.getEvseId,
      emtId = EmtId(
        tokenId = cdrinfo.getEmtId.getInstance,
        tokenType = TokenType.withName(cdrinfo.getEmtId.getTokenType),
        tokenSubType = Some(TokenSubType.withName(cdrinfo.getEmtId.getTokenSubType))
      ),
      contractId = cdrinfo.getContractId,
      liveAuthId = toOption(cdrinfo.getLiveAuthId),
      status = CdrStatusType.withName(cdrinfo.getStatus.getCdrStatusType),
      startDateTime = DateTimeNoMillis(cdrinfo.getStartDateTime.getLocalDateTime),
      endDateTime = DateTimeNoMillis(cdrinfo.getEndDateTime.getLocalDateTime),
      duration = toOption(cdrinfo.getDuration),
      houseNumber = toOption(cdrinfo.getHouseNumber),
      address = toOption(cdrinfo.getAddress),
      zipCode = toOption(cdrinfo.getZipCode),
      city = toOption(cdrinfo.getCity),
      country = cdrinfo.getCountry,
      chargePointType = cdrinfo.getChargePointType,
      connectorType = ConnectorType(
        connectorStandard = ConnectorStandard.withName(
          cdrinfo.getConnectorType.getConnectorStandard.getConnectorStandard),
        connectorFormat = ConnectorFormat.withName(
          cdrinfo.getConnectorType.getConnectorFormat.getConnectorFormat)),
      maxSocketPower = cdrinfo.getMaxSocketPower,
      productType = toOption(cdrinfo.getProductType),
      meterId = toOption(cdrinfo.getMeterId),
      chargingPeriods = cdrinfo.getChargingPeriods.asScala.toList.map( cdrPeriod=>
        CdrPeriod(
          startDateTime = DateTimeNoMillis(cdrPeriod.getStartDateTime.getLocalDateTime),
          endDateTime = DateTimeNoMillis(cdrPeriod.getEndDateTime.getLocalDateTime),
          billingItem = BillingItem.withName(cdrPeriod.getBillingItem.getBillingItemType) ,
          billingValue = cdrPeriod.getBillingValue,
          currency = cdrPeriod.getCurrency,
          itemPrice = cdrPeriod.getItemPrice,
          periodCost = Some(cdrPeriod.getPeriodCost)
        )

      )
    )
  }



  implicit def cdrToCdrInfo(cdr: CDR): CDRInfo = {
    import cdr._
    val cdrInfo = new CDRInfo
    cdr.address match {case Some(s) if !s.isEmpty => cdrInfo.setAddress(s)}
    cdrInfo.setCdrId(cdr.cdrId)
    cdrInfo.setChargePointType(cdr.chargePointType)

    val cType = new GenConnectorType()
    val cFormat = new GenConnectorFormat()
    cFormat.setConnectorFormat(cdr.connectorType.connectorFormat.toString)
    cType.setConnectorFormat(cFormat)
    val cStandard = new GenConnectorStandard()
    cStandard.setConnectorStandard(cdr.connectorType.connectorStandard.toString)
    cType.setConnectorStandard(cStandard)
    cdrInfo.setConnectorType(cType)
    cdrInfo.setContractId(cdr.contractId)
    cdr.houseNumber match {case Some(s) if !s.isEmpty => cdrInfo.setHouseNumber(s)}
    cdr.zipCode match {case Some(s) if !s.isEmpty => cdrInfo.setZipCode(s)}
    cdr.city match {case Some(s) if !s.isEmpty => cdrInfo.setCity(s)}
    cdrInfo.setCountry(cdr.country)
    cdr.duration  match {case Some(s) if !s.isEmpty => cdrInfo.setDuration(s)}
    val eid = new GenEmtId()
    eid.setInstance(cdr.emtId.tokenId)
    eid.setTokenType(cdr.emtId.tokenType.toString)
    eid.setTokenSubType(cdr.emtId.tokenSubType.toString)
    cdrInfo.setEmtId(eid)
    val start = new LocalDateTimeType()
    start.setLocalDateTime(startDateTime.toString)
    cdrInfo.setStartDateTime(start)
    val end = new LocalDateTimeType()
    end.setLocalDateTime(endDateTime.toString)
    cdrInfo.setEndDateTime(end)
    cdrInfo.setEvseId(cdr.evseId)

    cdr.liveAuthId match {case Some(s) if !s.isEmpty => cdrInfo.setLiveAuthId(s)}
    cdrInfo.setMaxSocketPower(cdr.maxSocketPower)
    cdr.meterId match {case Some(s) if !s.isEmpty => cdrInfo.setMeterId(s)}
    cdr.productType match {case Some(s) if !s.isEmpty => cdrInfo.setProductType(s)}

    val cdrStatus = new GenCdrStatusType()
    cdrStatus.setCdrStatusType(cdr.status.toString)
    cdrInfo.setStatus(cdrStatus)
    cdrInfo.getChargingPeriods.addAll(
      cdr.chargingPeriods.map {chargePeriodToGenCp} asJavaCollection)
    cdrInfo
  }

  def chargePeriodToGenCp(gcp: CdrPeriod): GenCdrPeriodType = {
    val period1 = new GenCdrPeriodType()
    val start = new LocalDateTimeType()
    start.setLocalDateTime(gcp.startDateTime.toString)
    period1.setStartDateTime(start)
    val end = new LocalDateTimeType()
    end.setLocalDateTime(gcp.endDateTime.toString)
    period1.setEndDateTime(end)
    val billingItem = new GenBillingItemType()
    billingItem.setBillingItemType(gcp.billingItem.toString)
    period1.setBillingItem(billingItem)
    period1.setBillingValue(gcp.billingValue)
    period1.setCurrency(gcp.currency)
    period1.setItemPrice(gcp.itemPrice)
    gcp.periodCost.foreach {period1.setPeriodCost(_)}
    period1
  }

  implicit def cpInfoToChargePoint(genCp: ChargePointInfo): ChargePoint = {
    ChargePoint(
      evseId = genCp.getEvseId,
      locationId = genCp.getLocationId,
      timestamp = toDateTimeOption(genCp.getTimestamp),
      locationName = genCp.getLocationName,
      locationNameLang = genCp.getLocationNameLang,
      images = genCp.getImages.asScala.toList map {genImage => EvseImageUrl(
        uri = genImage.getUri,
        thumbUri = toOption(genImage.getThumbUri),
        clazz = genImage.getClazz,
        `type` = genImage.getType,
        width = Some(genImage.getWidth),
        height = Some(genImage.getHeight)
      )},
      address = CpAddress(
        houseNumber = toOption(genCp.getHouseNumber),
        address =  genCp.getAddress,
        city = genCp.getCity,
        zipCode = genCp.getZipCode,
        country = genCp.getCountry
      ),
      geoLocation = GeoPoint(
        genCp.getGeoLocation.getLat,
        genCp.getGeoLocation.getLon),
      geoUserInterface = toGeoPointOption(genCp.getGeoUserInterface),
      geoSiteEntrance = genCp.getGeoSiteEntrance.asScala.toList map {gp =>
        GeoPoint(gp.getLat, gp.getLon)},
      geoSiteExit = genCp.getGeoSiteExit.asScala.toList map {gp =>
        GeoPoint(gp.getLat, gp.getLon)},
      operatingTimes = toHoursOption(genCp.getOperatingTimes),
      accessTimes = toHoursOption(genCp.getAccessTimes),
      status = toChargePointStatusOption(genCp.getStatus),
      statusSchedule = genCp.getStatusSchedule.asScala.toList map {cps =>
        ChargePointSchedule(DateTimeNoMillis(cps.getStartDate.getDateTime),
          DateTimeNoMillis(cps.getEndDate.getDateTime),
          ChargePointStatus.withName(cps.getStatus.getChargePointStatusType))},
      telephoneNumber = toOption(genCp.getTelephoneNumber),
      floorLevel = toOption(genCp.getFloorLevel),
      parkingSlotNumber = toOption(genCp.getParkingSlotNumber),
      parkingRestriction = genCp.getParkingRestriction.asScala.toList map {pr =>
        ParkingRestriction.withName(pr.getParkingRestrictionType)},
      authMethods = genCp.getAuthMethods.asScala.toList map {am =>
        AuthMethod.withName(am.getAuthMethodType)},
      connectors = genCp.getConnectors.asScala.toList map {con =>
        ConnectorType(
        connectorStandard = ConnectorStandard.withName(
          con.getConnectorStandard.getConnectorStandard),
        connectorFormat = ConnectorFormat.withName(
          con.getConnectorFormat.getConnectorFormat))},
      userInterfaceLang = genCp.getUserInterfaceLang.asScala.toList
    )
  }

  def imagesToGenImages(image: EvseImageUrl): GenEvseImageUrlType  = {
    val iut = new GenEvseImageUrlType()
    iut.setClazz(image.clazz)
    image.height  match {case Some(s) => iut.setHeight(s)}
    image.width  match {case Some(s) => iut.setWidth(s)}
    image.thumbUri  match {case Some(s) => iut.setThumbUri(s)}
    iut.setType(image.`type`)
    iut.setUri(image.uri)
    iut
  }

  implicit def chargePointToGenCp(cp: ChargePoint): ChargePointInfo = {
    val cpi = new ChargePointInfo()
    cpi.setEvseId(cp.evseId)
    cpi.setLocationId(cp.locationId)
    cpi.timestamp match {case Some(t) =>
      val ts = new DateTimeType()
      ts.setDateTime(t.toString)
      cpi.setTimestamp(ts)}
    cpi.setLocationName(cp.locationName)
    cpi.setLocationNameLang(cp.locationNameLang)
    cpi.getImages.addAll(cp.images.map {imagesToGenImages} asJavaCollection)
    cpi
  }
}