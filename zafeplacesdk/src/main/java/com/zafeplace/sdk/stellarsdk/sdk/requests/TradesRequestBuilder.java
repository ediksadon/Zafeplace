package com.zafeplace.sdk.stellarsdk.sdk.requests;

import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.zafeplace.sdk.stellarsdk.sdk.Asset;
import com.zafeplace.sdk.stellarsdk.sdk.AssetTypeCreditAlphaNum;
import com.zafeplace.sdk.stellarsdk.sdk.responses.Page;
import com.zafeplace.sdk.stellarsdk.sdk.responses.TradeResponse;
import com.zafeplace.sdk.stellarsdk.sdk.responses.TransactionResponse;

/**
 * Builds requests connected to trades.
 */
public class TradesRequestBuilder extends RequestBuilder {
    public TradesRequestBuilder(OkHttpClient httpClient, HttpUrl serverURI) {
        super(httpClient, serverURI, "trades");
    }

    public TradesRequestBuilder baseAsset(Asset asset) {
        uriBuilder.setQueryParameter("base_asset_type", asset.getType());
        if (asset instanceof AssetTypeCreditAlphaNum) {
            AssetTypeCreditAlphaNum creditAlphaNumAsset = (AssetTypeCreditAlphaNum) asset;
            uriBuilder.setQueryParameter("base_asset_code", creditAlphaNumAsset.getCode());
            uriBuilder.setQueryParameter("base_asset_issuer", creditAlphaNumAsset.getIssuer().getAccountId());
        }
        return this;
    }

    public TradesRequestBuilder counterAsset(Asset asset) {
        uriBuilder.setQueryParameter("counter_asset_type", asset.getType());
        if (asset instanceof AssetTypeCreditAlphaNum) {
            AssetTypeCreditAlphaNum creditAlphaNumAsset = (AssetTypeCreditAlphaNum) asset;
            uriBuilder.setQueryParameter("counter_asset_code", creditAlphaNumAsset.getCode());
            uriBuilder.setQueryParameter("counter_asset_issuer", creditAlphaNumAsset.getIssuer().getAccountId());
        }
        return this;
    }

    public static Page<TradeResponse> execute(OkHttpClient httpClient, HttpUrl uri)
        throws IOException, TooManyRequestsException {
        TypeToken type = new TypeToken<Page<TradeResponse>>() {};
        ResponseHandler<Page<TradeResponse>> responseHandler = new ResponseHandler<Page<TradeResponse>>(
            type);

        Request request = new Request.Builder().get().url(uri).build();
        Response response = httpClient.newCall(request).execute();
        return responseHandler.handleResponse(response);
    }

    public Page<TradeResponse> execute() throws IOException, TooManyRequestsException {
        return this.execute(this.httpClient, this.buildUri());
    }

    public TradesRequestBuilder offerId(String offerId) {
        uriBuilder.setQueryParameter("offer_id", offerId);
        return this;
    }
}
