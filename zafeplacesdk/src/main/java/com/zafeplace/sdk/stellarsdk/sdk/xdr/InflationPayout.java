// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.zafeplace.sdk.stellarsdk.sdk.xdr;


import java.io.IOException;

// === xdr source ============================================================

//  struct InflationPayout // or use PaymentResultAtom to limit types?
//  {
//      AccountID destination;
//      int64 amount;
//  };

//  ===========================================================================
public class InflationPayout  {
  public InflationPayout () {}
  private AccountID destination;
  public AccountID getDestination() {
    return this.destination;
  }
  public void setDestination(AccountID value) {
    this.destination = value;
  }
  private Int64 amount;
  public Int64 getAmount() {
    return this.amount;
  }
  public void setAmount(Int64 value) {
    this.amount = value;
  }
  public static void encode(XdrDataOutputStream stream, InflationPayout encodedInflationPayout) throws IOException{
    AccountID.encode(stream, encodedInflationPayout.destination);
    Int64.encode(stream, encodedInflationPayout.amount);
  }
  public static InflationPayout decode(XdrDataInputStream stream) throws IOException {
    InflationPayout decodedInflationPayout = new InflationPayout();
    decodedInflationPayout.destination = AccountID.decode(stream);
    decodedInflationPayout.amount = Int64.decode(stream);
    return decodedInflationPayout;
  }
}
