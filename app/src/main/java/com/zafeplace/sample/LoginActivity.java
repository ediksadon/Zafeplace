package com.zafeplace.sample;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zafeplace.sdk.Zafeplace;
import com.zafeplace.sdk.callbacks.OnAccessTokenListener;
import com.zafeplace.sdk.utils.FingerprintHandler;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.zafeplace.sample.Constants.Extras.AUTH_TYPE;
import static com.zafeplace.sample.Constants.Extras.IS_REGISTRATION;
import static com.zafeplace.sdk.Constants.AuthType.FINGERPRINT_AUTH;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.editText)
    EditText pinCodeEditText;
    @BindView(R.id.button)
    Button button;
    @BindView(R.id.textView)
    TextView textView;

    private Zafeplace mZafeplace;
    private int mAuthType;
    private boolean mIsRegistrtation;
    private Unbinder mUnbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mUnbinder = ButterKnife.bind(this);
        mZafeplace = Zafeplace.getInstance(this);
        fetchExtras();
        initUI();
    }

    @Override
    protected void onDestroy() {
        if (mUnbinder != null) {
            mUnbinder.unbind();
        }
        super.onDestroy();
    }

    public void inputPin(View view) {
        if (!pinCodeEditText.getText().toString().isEmpty()) {
            if (mIsRegistrtation) {
                mZafeplace.pinCodeLogin(pinCodeEditText.getText().toString());
                checkAccessToken();
            } else {
                if (pinCodeEditText.getText().toString().equals(mZafeplace.getPinCode())) {
                    checkAccessToken();
                } else {
                    Toast.makeText(this, "Incorrect Pin Code", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }


    public static void start(Context context, int authType, boolean isRegistration) {
        Intent starter = new Intent(context, LoginActivity.class);
        starter.putExtra(AUTH_TYPE, authType);
        starter.putExtra(IS_REGISTRATION, isRegistration);
        context.startActivity(starter);
    }

    private void fetchExtras() {
        mAuthType = getIntent().getIntExtra(AUTH_TYPE, 1);
        mIsRegistrtation = getIntent().getBooleanExtra(IS_REGISTRATION, false);
    }

    private void initUI() {
        if (mAuthType == FINGERPRINT_AUTH) {
            textView.setVisibility(View.VISIBLE);
            button.setVisibility(View.GONE);
            pinCodeEditText.setVisibility(View.GONE);
            mZafeplace.fingerprintLogin(new FingerprintHandler.FingerprintAuthenticationCallback() {
                @Override
                public void onResponse(String message, boolean isSuccess) {
                    if (isSuccess) {
                        MainActivity.start(LoginActivity.this);
                    } else {
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            textView.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
            pinCodeEditText.setVisibility(View.VISIBLE);
        }
    }

    private void checkAccessToken() {
        if (mZafeplace.getAccessToken() == null) {
            final LoadingDialogFragment loadingDialogFragment = LoadingDialogFragment.newInstance();
            loadingDialogFragment.show(getSupportFragmentManager(), LoadingDialogFragment.TAG);
            mZafeplace.generateAccessToken("com.zafeplace.sample",
                    "756496e0a7d900ade56913cc098749ee",
                    new OnAccessTokenListener() {
                        @Override
                        public void onGetToken(String response) {
                            loadingDialogFragment.dismiss();
                            MainActivity.start(LoginActivity.this);
                        }

                        @Override
                        public void onErrorToken(String error) {
                            loadingDialogFragment.dismiss();
                            Toast.makeText(LoginActivity.this, "Error take token " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            MainActivity.start(LoginActivity.this);
        }
    }
}