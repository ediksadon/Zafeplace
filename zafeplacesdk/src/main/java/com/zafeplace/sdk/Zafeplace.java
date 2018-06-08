package com.zafeplace.sdk;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.zafeplace.sdk.callbacks.OnAccessTokenListener;
import com.zafeplace.sdk.callbacks.OnGetTokenBalance;
import com.zafeplace.sdk.callbacks.OnGetWalletBalance;
import com.zafeplace.sdk.callbacks.OnMakeTransaction;
import com.zafeplace.sdk.callbacks.OnSmartContractRawList;
import com.zafeplace.sdk.callbacks.OnWalletGenerateListener;
import com.zafeplace.sdk.managers.PreferencesManager;
import com.zafeplace.sdk.models.EthWallet;
import com.zafeplace.sdk.models.Wallet;
import com.zafeplace.sdk.server.ZafeplaceApi;
import com.zafeplace.sdk.server.models.Abi;
import com.zafeplace.sdk.server.models.BalanceModel;
import com.zafeplace.sdk.server.models.LoginResponse;
import com.zafeplace.sdk.server.models.SmartContractTransactionRaw;
import com.zafeplace.sdk.server.models.TransactionRaw;
import com.zafeplace.sdk.utils.FingerPrintLogin;
import com.zafeplace.sdk.utils.FingerprintHandler;
import com.zafeplace.sdk.utils.ParseUtils;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.zafeplace.sdk.Constants.AuthType.FINGERPRINT_AUTH;
import static com.zafeplace.sdk.Constants.AuthType.PIN_AUTH;
import static com.zafeplace.sdk.Constants.ETH_SERVICE_URL;
import static com.zafeplace.sdk.Constants.WalletType.ETH;
import static com.zafeplace.sdk.Constants.ZAFEPLACE_PASSWORD;
import static com.zafeplace.sdk.utils.AppUtils.isNull;
import static com.zafeplace.sdk.utils.EncryptionUtils.decryption;
import static com.zafeplace.sdk.utils.EncryptionUtils.encryption;
import static com.zafeplace.sdk.utils.StorageUtils.deleteFile;
import static com.zafeplace.sdk.utils.WalletUtils.getWalletName;

public class Zafeplace {

    private Activity mActivity;
    private EditText mInput;
    private PreferencesManager mManager;
    private ExecutorService mExecutor;
    private AlertDialog mAlert = null;
    private FingerPrintLogin mFingerPrintLogin;
    private static String smart_contract = "smart_contract";
    private boolean mIsCancelClicked;

    public enum WalletTypes {
        ETH_WALLET;
    }

    public enum AuthType {
        FINGERPRINT_AUTH,
        PIN_AUTH;
    }

    private static Zafeplace instance;

    public static Zafeplace getInstance(Activity context) {
        if (instance == null) {
            instance = new Zafeplace(context);
        } else {
            instance.mActivity = context;
        }
        return instance;
    }

    private Zafeplace(Activity context) {
        this.mActivity = context;
        mManager = new PreferencesManager();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    public void generateAccessToken(String packageName, String appSecret, final OnAccessTokenListener onAccessTokenListener) {

        ZafeplaceApi.getInstance(mActivity).generateAccessToken(packageName, appSecret).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    onAccessTokenListener.onGetToken(response.body().accessToken);
                    String tok = response.body().accessToken;
                    mManager.setAuthToken(tok, mActivity);
                } else {
                    String err = response.message();
                    onAccessTokenListener.onErrorToken(new Throwable(err));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                onAccessTokenListener.onErrorToken(t);
            }
        });
    }

    public String getAccessToken() {
        return mManager.getAuthToken(mActivity);
    }


    public void getWalletBalance(WalletTypes walletType, String address, final OnGetWalletBalance onGetWalletBalance) {
        ZafeplaceApi.getInstance(mActivity).getWalletBalance(getWalletName(walletType), address).enqueue(new Callback<BalanceModel>() {
            @Override
            public void onResponse(Call<BalanceModel> call, Response<BalanceModel> response) {
                onGetWalletBalance.onWalletBalance(response.body().result);
            }

            @Override
            public void onFailure(Call<BalanceModel> call, Throwable t) {
                onGetWalletBalance.onErrorWalletBalance(t);
            }
        });
    }

    public void getTokenBalance(WalletTypes walletType, String address, final OnGetTokenBalance onGetTokenBalance) {
        ZafeplaceApi.getInstance(mActivity).getTokenBalance(getWalletName(walletType), address).enqueue(new Callback<BalanceModel>() {
            @Override
            public void onResponse(Call<BalanceModel> call, Response<BalanceModel> response) {
                onGetTokenBalance.onTokenBalance(response.body().result);
            }

            @Override
            public void onFailure(Call<BalanceModel> call, Throwable t) {
                onGetTokenBalance.onErrorTokenBalance(t);
            }
        });
    }

    public void getSmartContractTransactionRaw(final WalletTypes walletType, final OnSmartContractRawList onSmartContractRaw) {
        ZafeplaceApi.getInstance(mActivity).getSmartContractRaw(getWalletName(walletType)).enqueue(new Callback<SmartContractTransactionRaw>() {
            @Override
            public void onResponse(Call<SmartContractTransactionRaw> call, Response<SmartContractTransactionRaw> response) {
                try {
                    SmartContractTransactionRaw mes = response.body();
                    List<Abi> abis = mes.result.abi;
                    onSmartContractRaw.onGetSmartContractAbiList(abis);
                } catch (Exception e) {
                    onSmartContractRaw.onErrorSmartRaw(e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<SmartContractTransactionRaw> call, Throwable t) {
                onSmartContractRaw.onErrorSmartRaw(t);
            }
        });
    }

    public void createTransaction(WalletTypes walletType, String addressSender, String addressRecipient, double amount,
                                  final OnMakeTransaction onMakeTransaction) {
        checkLoginCoinTransaction(walletType, addressSender, addressRecipient, amount, onMakeTransaction);
    }

    private void makeTransaction(WalletTypes walletType, String addressSender, String addressRecipient, double amount,
                                 final OnMakeTransaction onMakeTransaction) {
        ZafeplaceApi.getInstance(mActivity).getRawTransaction(getWalletName(walletType), addressSender, addressRecipient, amount).enqueue(new Callback<TransactionRaw>() {
            @Override
            public void onResponse(Call<TransactionRaw> call, Response<TransactionRaw> response) {
                TransactionRaw raw = response.body();
                Credentials credentials = Credentials.create(mManager.getEthWallet(mActivity).getPrivateKey());
                RawTransaction rawTransaction = RawTransaction.createEtherTransaction(raw.result.rawTx.result.nonce, new BigInteger(raw.result.rawTx.result.gasPrice),
                        new BigInteger(raw.result.rawTx.result.gasLimit), raw.result.rawTx.result.to, raw.result.rawTx.result.value);
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);
                showDialog(hexValue, onMakeTransaction);
            }

            @Override
            public void onFailure(Call<TransactionRaw> call, Throwable t) {
                onMakeTransaction.onBreakTransaction();
                showErrorDialog(t.getMessage());
            }
        });
    }

    private void showDialog(final String message, final OnMakeTransaction onMakeTransaction) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mActivity,
                R.style.SimpleDialogTheme)).setTitle("Do you really want to do this transaction?").setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onMakeTransaction.onBreakTransaction();
                dialog.dismiss();
            }
        }).setPositiveButton("Accept", (dialog, which) -> doTransaction(WalletTypes.ETH_WALLET, message, onMakeTransaction));
        try {
            builder.show();
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
    }

    private void showErrorDialog(final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mActivity,
                R.style.SimpleDialogTheme)).setTitle("Error transaction " + message).setNeutralButton("Ok", (dialog, which) -> dialog.dismiss());
        builder.setMessage(message);
        try {
            builder.show();
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
    }

    public void createTransactionToken(WalletTypes walletType, String addressSender, String addressRecipient, int amount,
                                       final OnMakeTransaction onMakeTransaction) {
        checkPinTokenTransaction(walletType, addressSender, addressRecipient, amount, onMakeTransaction);
    }

    public void makeTransactionToken(WalletTypes walletType, String addressSender, String addressRecipient, int amount,
                                     final OnMakeTransaction onMakeTransaction) {
        ZafeplaceApi.getInstance(mActivity).getTokenTransactionRaw(getWalletName(walletType), addressSender, addressRecipient, amount).enqueue(new Callback<TransactionRaw>() {
            @Override
            public void onResponse(Call<TransactionRaw> call, Response<TransactionRaw> response) {
                TransactionRaw raw = response.body();
                Credentials credentials = Credentials.create(mManager.getEthWallet(mActivity).getPrivateKey());
                RawTransaction rawTransaction = RawTransaction.createEtherTransaction(raw.result.rawTx.result.nonce, new BigInteger(raw.result.rawTx.result.gasPrice),
                        new BigInteger(raw.result.rawTx.result.gasLimit), raw.result.rawTx.result.to, raw.result.rawTx.result.value);
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);
                showDialog(hexValue, onMakeTransaction);
            }

            @Override
            public void onFailure(Call<TransactionRaw> call, Throwable t) {
                onMakeTransaction.onBreakTransaction();
                showErrorDialog(t.getMessage());
            }
        });
    }

    private void doTransaction(WalletTypes walletType, String signTx, final OnMakeTransaction onMakeTransaction) {
        ZafeplaceApi.getInstance(mActivity).doTransaction(signTx, getWalletName(walletType)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                try {
                    onMakeTransaction.onSuccessTransaction(ParseUtils.transactionMessage(response.body()));
                } catch (Exception e) {
                    e.printStackTrace();
                    onMakeTransaction.onErrorTransaction(e);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                onMakeTransaction.onErrorTransaction(t);
            }
        });
    }

    public void fingerprintLogin(FingerprintHandler.FingerprintAuthenticationCallback fingerprintAuthenticationCallback) {
        mFingerPrintLogin = new FingerPrintLogin(mActivity, fingerprintAuthenticationCallback);
        mFingerPrintLogin.initAuth();
    }

    public void cancelFingerprintLogin() {
        if (mFingerPrintLogin != null) {
            mFingerPrintLogin.stopAuth();
        }
    }


    public void pinCodeLogin(String code) {
        mManager.setIsLoggedIn(true, mActivity);
        mManager.setAuthType(PIN_AUTH, mActivity);
        mManager.setPinCode(encryption(ZAFEPLACE_PASSWORD, code), mActivity);
    }


    public void generateWallet(WalletTypes walletType, OnWalletGenerateListener onWalletGenerateListener) {
        switch (walletType) {
            case ETH_WALLET:
                checkLoginGenerateEthWallet(onWalletGenerateListener);
        }
    }


    private void generateEthWallet(final OnWalletGenerateListener onWalletGenerateListener) {
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            onWalletGenerateListener.onErrorGenerate(mActivity.getString(R.string.write_external_storage_permission_not_enabled));
        } else if (!isLoggedIn()) {
            onWalletGenerateListener.onErrorGenerate(mActivity.getString(R.string.you_need_auth_to_generate_wallet));
        } else {
            mExecutor.execute(() -> {
                try {
                    Web3jFactory.build(new HttpService(ETH_SERVICE_URL));
                    String wallet = WalletUtils.generateLightNewWalletFile(ZAFEPLACE_PASSWORD, Environment.getExternalStorageDirectory());
                    Credentials credentials = WalletUtils.loadCredentials(ZAFEPLACE_PASSWORD, Environment.getExternalStorageDirectory() + "/" + wallet);
                    String privateKey = String.format("%x", credentials.getEcKeyPair().getPrivateKey());
                    final String address = credentials.getAddress();
                    mManager.setEthWallet(privateKey, address, mActivity);
                    deleteFile(Environment.getExternalStorageDirectory() + "/" + wallet);
                    mActivity.runOnUiThread(() -> onWalletGenerateListener.onSuccessGenerate(address));
                } catch (final Throwable e) {
                    mActivity.runOnUiThread(() -> onWalletGenerateListener.onErrorGenerate(e.getMessage()));
                }
            });

        }
    }

    public void executeSmartContractMethod(boolean isTransaction, String nameFunk) {

    }

    public void saveUserData(String firstName, String secondName, String email, String additionalData) {
        mManager.setUserData(firstName, secondName, email, additionalData, mActivity);
    }

    public Wallet getWallet(WalletTypes walletType) {
        switch (walletType) {
            case ETH_WALLET:
                return mManager.getEthWallet(mActivity);
            default:
                return null;
        }
    }

    public boolean isIdentityExist(WalletTypes walletType) {
        switch (walletType) {
            case ETH_WALLET:
                return !isNull(mManager.getEthWallet(mActivity));
            default:
                return false;
        }
    }

    public String getPinCode() {
        return mManager.isLoggedIn(mActivity) ? decryption(ZAFEPLACE_PASSWORD, mManager.getPinCode(mActivity)) :
                mActivity.getString(R.string.you_need_auth_to_get_pin_code);
    }

    public List<Wallet> getSemiPublicData() {
        List<Wallet> walletList = new ArrayList<>();
        EthWallet ethWallet = new EthWallet();
        ethWallet.setCurrencyName(ETH);
        ethWallet.setAddress(mManager.getEthWallet(mActivity).getAddress());
        walletList.add(ethWallet);
        return walletList;
    }

    public boolean isLoggedIn() {
        return mManager.isLoggedIn(mActivity);
    }

    public int getAuthType() {
        return mManager.getAuthType(mActivity);
    }

    public void logout() {
        mManager.setIsLoggedIn(false, mActivity);
    }

    private void checkLoginGenerateEthWallet(final OnWalletGenerateListener onWalletGenerateListener) {
        String title = "";
        String titleButton = "";
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mActivity,
                R.style.SimpleDialogTheme));
        builder.setCancelable(true);
        switch (getAuthType()) {
            case FINGERPRINT_AUTH:
                title = "Please use fingerprint for authorization";
                titleButton = "Cancel";
                fingerprintLogin((message, isSuccess) -> {
                    if (isSuccess) {
                        closeDialog();
                        generateEthWallet(onWalletGenerateListener);
                        onWalletGenerateListener.onStartGenerate();
                    } else {
                        if (!mIsCancelClicked) {
                            Toast.makeText(mActivity, "Wrong Fingerprint!", Toast.LENGTH_SHORT).show();
                        } else {
                            mIsCancelClicked = false;
                        }
                    }
                });
                break;
            case PIN_AUTH:
                title = "Input Pin Code";
                titleButton = "OK";
                mInput = new EditText(mActivity);
                mInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                mInput.setLayoutParams(lp);
                builder.setView(mInput);
                break;
        }
        builder.setTitle(title).setNeutralButton(titleButton, (dialog, which) -> {
            switch (getAuthType()) {
                case PIN_AUTH: {
                    if (mInput.getText().toString().equals(getPinCode())) {
                        generateEthWallet(onWalletGenerateListener);
                        onWalletGenerateListener.onStartGenerate();
                    } else {
                        Toast.makeText(mActivity, "Wrong Pin!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
                case FINGERPRINT_AUTH:
                    mIsCancelClicked = true;
                    cancelFingerprintLogin();
                    break;
            }
        });

        mAlert = builder.show();
        mAlert.setOnDismissListener((dialog) -> {
            mIsCancelClicked = true;
        });

    }

    private void closeDialog() {
        mAlert.dismiss();
    }

    private void checkLoginCoinTransaction(final WalletTypes walletType, final String addressSender, final String addressRecipient, final double amount,
                                           final OnMakeTransaction onMakeTransaction) {
        String title = "";
        String titleButton = "";
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mActivity,
                R.style.SimpleDialogTheme));
        switch (getAuthType()) {
            case FINGERPRINT_AUTH:
                title = "Please use fingerprint for authorization";
                titleButton = "Cancel";
                fingerprintLogin((message, isSuccess) -> {
                    if (isSuccess) {
                        closeDialog();
                        onMakeTransaction.onStartTransaction();
                        makeTransaction(walletType, addressSender, addressRecipient, amount, onMakeTransaction);
                    } else {
                        if (!mIsCancelClicked) {
                            Toast.makeText(mActivity, "Wrong Fingerprint!", Toast.LENGTH_SHORT).show();
                        } else {
                            mIsCancelClicked = false;
                        }
                    }
                });
                break;
            case PIN_AUTH:
                title = "Input Pin Code";
                titleButton = "OK";
                mInput = new EditText(mActivity);
                mInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                mInput.setLayoutParams(lp);
                builder.setView(mInput);
                break;
        }
        builder.setTitle(title).setNeutralButton(titleButton, (dialog, which) -> {
            switch (getAuthType()) {
                case PIN_AUTH: {
                    if (mInput.getText().toString().equals(getPinCode())) {
                        onMakeTransaction.onStartTransaction();
                        makeTransaction(walletType, addressSender, addressRecipient, amount, onMakeTransaction);
                    } else {
                        Toast.makeText(mActivity, "Wrong Pin!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
                case FINGERPRINT_AUTH:
                    mIsCancelClicked = true;
                    cancelFingerprintLogin();
                    break;
            }
        });
        builder.setView(mInput);
        mAlert = builder.show();
        mAlert.setOnDismissListener((dialog) -> {
            mIsCancelClicked = true;
        });
    }

    private void checkPinTokenTransaction(final WalletTypes walletType, final String addressSender, final String addressRecipient, final int amount,
                                          final OnMakeTransaction onMakeTransaction) {
        String title = "";
        String titleButton = "";
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mActivity,
                R.style.SimpleDialogTheme));
        switch (getAuthType()) {
            case FINGERPRINT_AUTH:
                title = "Please use fingerprint for authorization";
                titleButton = "Cancel";
                fingerprintLogin((message, isSuccess) -> {
                    if (isSuccess) {
                        closeDialog();
                        onMakeTransaction.onStartTransaction();
                        makeTransactionToken(walletType, addressSender, addressRecipient, amount, onMakeTransaction);
                    } else {
                        if (!mIsCancelClicked) {
                            Toast.makeText(mActivity, "Wrong Fingerprint!", Toast.LENGTH_SHORT).show();
                        } else {
                            mIsCancelClicked = false;
                        }
                    }
                });
                break;
            case PIN_AUTH:
                title = "Input Pin Code";
                titleButton = "OK";
                mInput = new EditText(mActivity);
                mInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                mInput.setLayoutParams(lp);
                builder.setView(mInput);
                break;
        }
        builder.setTitle(title).setNeutralButton(titleButton, (dialog, which) -> {
            switch (getAuthType()) {
                case PIN_AUTH: {
                    if (mInput.getText().toString().equals(getPinCode())) {
                        onMakeTransaction.onStartTransaction();
                        makeTransactionToken(walletType, addressSender, addressRecipient, amount, onMakeTransaction);
                    } else {
                        Toast.makeText(mActivity, "Wrong Pin!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
                case FINGERPRINT_AUTH:
                    mIsCancelClicked = true;
                    cancelFingerprintLogin();
                    break;
            }
        });
        builder.setView(mInput);
        mAlert = builder.show();
        mAlert.setOnDismissListener((dialog) -> {
            mIsCancelClicked = true;
        });
    }
}