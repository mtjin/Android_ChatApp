package com.mtjin.mtjinchat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

//구글로그인 인증 참고 : https://firebase.google.com/docs/auth/android/google-signin?hl=ko
public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final int CODE_SIGN_IN = 1000;
    private FirebaseAuth mFirebaseAuth; //인증객체
    private GoogleApiClient mGoogleApiClient; //구글인증에필요

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mFirebaseAuth = FirebaseAuth.getInstance(); //인증은 구글로 할 것이다. (페이스북이나 다른걸로도 가능)
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this) //기본으로 세팅해줌
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        findViewById(R.id.sign_btn_signin).setOnClickListener(this);
    }


    //구글인증연결 실패시
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //구글로그인버튼 눌렀을 때 처리
    @Override
    public void onClick(View v) {
        Intent signInintent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInintent, CODE_SIGN_IN);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CODE_SIGN_IN) { //구글로그인버튼 누르고 응답결과
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) { //로그인 성공시
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                Toast.makeText(this, "구글 로그인을 실패하였습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) { //task에서 다양한 정보를 담고있기 때문에 잘 사용하면된다.
                        if (!task.isSuccessful()) { //실패했다면
                            Toast.makeText(SignInActivity.this, "인증 실패하였습니다", Toast.LENGTH_LONG).show();
                        } else { //성공했으면 다시 메인액티비티로 가게해주면된다. 그리고 메인액티비티에서 인증이 이제 되었으므로 인증됬을 경우의 분기를 실행할 거다.
                            startActivity(new Intent(SignInActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                });
    }
}
