// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.zafeplace.sdk.stellarsdk.sdk.xdr;


import java.io.IOException;

// === xdr source ============================================================

//  struct LedgerSCPMessages
//  {
//      uint32 ledgerSeq;
//      SCPEnvelope messages<>;
//  };

//  ===========================================================================
public class LedgerSCPMessages  {
  public LedgerSCPMessages () {}
  private Uint32 ledgerSeq;
  public Uint32 getLedgerSeq() {
    return this.ledgerSeq;
  }
  public void setLedgerSeq(Uint32 value) {
    this.ledgerSeq = value;
  }
  private SCPEnvelope[] messages;
  public SCPEnvelope[] getMessages() {
    return this.messages;
  }
  public void setMessages(SCPEnvelope[] value) {
    this.messages = value;
  }
  public static void encode(XdrDataOutputStream stream, LedgerSCPMessages encodedLedgerSCPMessages) throws IOException{
    Uint32.encode(stream, encodedLedgerSCPMessages.ledgerSeq);
    int messagessize = encodedLedgerSCPMessages.getMessages().length;
    stream.writeInt(messagessize);
    for (int i = 0; i < messagessize; i++) {
      SCPEnvelope.encode(stream, encodedLedgerSCPMessages.messages[i]);
    }
  }
  public static LedgerSCPMessages decode(XdrDataInputStream stream) throws IOException {
    LedgerSCPMessages decodedLedgerSCPMessages = new LedgerSCPMessages();
    decodedLedgerSCPMessages.ledgerSeq = Uint32.decode(stream);
    int messagessize = stream.readInt();
    decodedLedgerSCPMessages.messages = new SCPEnvelope[messagessize];
    for (int i = 0; i < messagessize; i++) {
      decodedLedgerSCPMessages.messages[i] = SCPEnvelope.decode(stream);
    }
    return decodedLedgerSCPMessages;
  }
}
