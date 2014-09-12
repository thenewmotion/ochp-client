package com.thenewmotion.chargenetwork.eclearing

import com.thenewmotion.chargenetwork.eclearing.api.{CDR, EclearingApi, Card}
import com.thenewmotion.chargenetwork.eclearing.client.{Result, EclearingClient}
import com.thenewmotion.time.Imports._
import eu.ochp._1.RoamingAuthorisationInfo


/**
 * @author Yaroslav Klymko
 */
trait EclearingService extends EclearingApi {
  def client: EclearingClient

  def sendAllCards(cards: List[Card]): Result = client.setRoamingAuthorisationList(cards)
  def recvAllCards():List[Card] = client.roamingAuthorisationList()
  def sendNewCards(cards: List[Card]): Result = client.setRoamingAuthorisationListUpdate(cards)
  def recvNewCards(lastUpdate: DateTime):List[Card] = client.roamingAuthorisationListUpdate()

  def sendCdrs(cdrs: List[CDR]): Result = client.addCdrs(cdrs)
  def recvCdrs():List[CDR] = client.getCdrs()
}