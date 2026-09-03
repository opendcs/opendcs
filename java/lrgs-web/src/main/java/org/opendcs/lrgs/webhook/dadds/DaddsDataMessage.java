package org.opendcs.lrgs.webhook.dadds;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A record to hold the extracted Message from the Dadds WebHook notification
 * DaddsDataMessage
 * @param id WebHook Message ID
 * @param address DCP Address
 * @param time time recieved.
 * @param infoCode
 * @param groupCode
 * @param data
 * @param dataHex
 * @param armCodes
 * @param lockTime
 * @param baud
 * @param signalStrength
 * @param frequencyDeviationStart
 * @param frequencyDeviationEnd
 * @param phaseNoise
 * @param goodPhase
 * @param channelId
 * @param satelliteLocation
 * @param source
 * @param noEot
 * @param parity
 * @param nwsDescriptor
 * @param isNws
 * @param nwsCenter
 * @param pdtId
 * @param groupId
 * @param AddressReceived
 * @param syncTime
 * @param signalToNoiseRatio
 * @param length
 * @param quality
 * @param satId
 * @param priority
 * @param duration
 * @param frameSync
 * @param addressCode
 * @param bitErrorRate
 */
public record DaddsDataMessage(
    @JsonProperty("Id")
    UUID id,
    @JsonProperty("Address")
    String address,
    @JsonProperty("Time")
    LocalDateTime time,
    @JsonProperty("InfoCd")
    String infoCode,
    @JsonProperty("GroupCd")
    String groupCode,
    @JsonProperty("Data")
    String data,
    @JsonProperty("DataHex")
    String dataHex,
    @JsonProperty("ArmCodes")
    List<String> armCodes,
    @JsonProperty("LockTime")
    LocalDateTime lockTime,
    @JsonProperty("Baud")
    int baud,
    @JsonProperty("SigStrength")
    float signalStrength,
    @JsonProperty("FreqDevStart")
    float frequencyDeviationStart,
    @JsonProperty("FreqDevEnd")
    float frequencyDeviationEnd,
    @JsonProperty("PhaseNoise")
    float phaseNoise,
    @JsonProperty("GoodPhase")
    int goodPhase,
    @JsonProperty("ChannelId")
    int channelId,
    @JsonProperty("SatLocation")
    String satelliteLocation,
    @JsonProperty("Source")
    String source,
    @JsonProperty("NoEot")
    boolean noEot,
    @JsonProperty("Par")
    int parity,
    @JsonProperty("NwsDescriptor")
    String nwsDescriptor,
    @JsonProperty("IsNws")
    boolean isNws, 
    @JsonProperty("NwsCenter")
    String nwsCenter,
    @JsonProperty("PdtId")
    int pdtId,
    @JsonProperty("GroupId")
    int groupId,
    @JsonProperty("AddressRecv")
    String AddressReceived,
    @JsonProperty("SyncTime")
    LocalDateTime syncTime, 
    @JsonProperty("Snr")
    float signalToNoiseRatio,
    @JsonProperty("Length")
    int length,
    @JsonProperty("Quality")
    String quality,
    @JsonProperty("SatId")
    int satId,
    @JsonProperty("Priority")
    int priority,
    @JsonProperty("Duration")
    float duration,
    @JsonProperty("FrameSync")
    String frameSync,
    @JsonProperty("AddressCode")
    String addressCode,
    @JsonProperty("Ber")
    int bitErrorRate)
{
}
