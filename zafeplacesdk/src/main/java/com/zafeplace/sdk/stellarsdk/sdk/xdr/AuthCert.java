// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten

package com.zafeplace.sdk.stellarsdk.sdk.xdr;


import java.io.IOException;

// === xdr source ============================================================

//  struct AuthCert
//  {
//      Curve25519Public pubkey;
//      uint64 expiration;
//      Signature sig;
//  };

//  ===========================================================================
public class AuthCert  {
  public AuthCert () {}
  private Curve25519Public pubkey;
  public Curve25519Public getPubkey() {
    return this.pubkey;
  }
  public void setPubkey(Curve25519Public value) {
    this.pubkey = value;
  }
  private Uint64 expiration;
  public Uint64 getExpiration() {
    return this.expiration;
  }
  public void setExpiration(Uint64 value) {
    this.expiration = value;
  }
  private Signature sig;
  public Signature getSig() {
    return this.sig;
  }
  public void setSig(Signature value) {
    this.sig = value;
  }
  public static void encode(XdrDataOutputStream stream, AuthCert encodedAuthCert) throws IOException{
    Curve25519Public.encode(stream, encodedAuthCert.pubkey);
    Uint64.encode(stream, encodedAuthCert.expiration);
    Signature.encode(stream, encodedAuthCert.sig);
  }
  public static AuthCert decode(XdrDataInputStream stream) throws IOException {
    AuthCert decodedAuthCert = new AuthCert();
    decodedAuthCert.pubkey = Curve25519Public.decode(stream);
    decodedAuthCert.expiration = Uint64.decode(stream);
    decodedAuthCert.sig = Signature.decode(stream);
    return decodedAuthCert;
  }
}
