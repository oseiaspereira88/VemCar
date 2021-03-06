package com.example.simplificar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.simplificar.models.User;
import com.example.simplificar.util.CryptWithMD5;
import com.example.simplificar.util.FirebaseConnection;
import com.example.simplificar.util.LibraryClass;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class LoginActivity extends AppCompatActivity implements OnConnectionFailedListener {
    private static final int RC_SIGN_IN_GOOGLE = 777;

    private FirebaseDatabase firebaseBD;
    private FirebaseAuth mAuth;
    private User user;

    private EditText editNome, editEmail, editSenha;
    private ImageView imgFB, imgGoogle, imgBack;
    private Button bLogar;
    private TextView tvCadastrar, tvRedefinir, tvInfo;
    private LinearLayout llInfo01;
    private String nome, email, senha;
    private boolean isSavedUser = false;
    private boolean isLogin = true, isPasswordReset = false;

    private CallbackManager callbackManager;
    public GoogleSignInClient mGoogleSignInClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        formSlideRL();
        setAllListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initFirebase();
        initFacebook();
        initGoogle();
        updateUI(mAuth.getCurrentUser());
    }

    private void initFirebase() {
        firebaseBD = LibraryClass.getFirebaseDB();
        mAuth = FirebaseConnection.getFirebaseAuth();
    }

    private void initFacebook() {
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                alert("Sucesso no Login!", 0);
                //accessFacebookLoginData(loginResult.getAccessToken());
                saveFacebookCredentialsInFirebase(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                LoginManager.getInstance().logOut();
            }

            @Override
            public void onError(FacebookException error) {
                alert("Falha no Login!\nError:" + error, 0);
                LoginManager.getInstance().logOut();
            }
        });

        //Para anuncios com valores (code de moeda sgundo modelo ISO 4217)
        //logger.logPurchase(BigDecimal.valueOf(4.32), Currency.getInstance("USD"));

        // If using in a fragment
        //loginButton.setFragment(LoginActivity.this);

        //Verifique o status de login

        //O aplicativo pode ter apenas uma pessoa conectada de cada vez, e o LoginManager define
        // o AccessToken e o Profile atuais dessa pessoa. O SDK do Facebook salva os dados nas
        // preferências compartilhadas e os define no início da sessão. Você pode ver se uma pessoa
        // já está conectada consultando AccessToken.getCurrentAccessToken() e Profile.getCurrentProfile();

    }

    private void initGoogle() {
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN_GOOGLE && resultCode == RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("TAG", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("TAG", "Google sign in failed", e);
                // ...
            }
        } else {
            //para o FacebookApi
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            User mUser = new User();
                            mUser.setId(user.getUid());
                            mUser.setNome(user.getDisplayName());
                            mUser.setEmail(user.getEmail());
                            mUser.saveInFirebase();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                            mAuth.signOut();
                            updateUI(null);
                        }
                        // ...
                    }
                });
    }

    private void saveFacebookCredentialsInFirebase(final AccessToken accessToken) {
        //se já existir as credenciais no BD, sinaliza apenas fazer login
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());

        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    //Salvando user no RealtimeDB
                    user = new User();
                    user.setId(accessToken.getUserId());
                    user.setNome(mAuth.getCurrentUser().getDisplayName());
                    user.setEmail(mAuth.getCurrentUser().getEmail());
                    user.saveInFirebase();
                    Toast.makeText(getApplicationContext(), "Usuário foi salvo no BD", Toast.LENGTH_LONG).show();
                    Intent it = new Intent(getBaseContext(), MainActivity.class);
                    startActivity(it);
                } else {
                    Toast.makeText(getApplicationContext(), "Login is Fail", Toast.LENGTH_LONG).show();
                    mAuth.getInstance().signOut();
                    LoginManager.getInstance().logOut();
                }
            }
        });
    }

    public void loadUserProfile(AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                try {
                    String first_name = object.getString("first_name");
                    String last_name = object.getString("last_name");
                    String email = object.getString("email");
                    String id = object.getString("id");

                    String image_url = "https://graph.facebook.com/" + id + "/picture?type=normal";

                    editNome.setText(first_name + " " + last_name);
                    editEmail.setText(email);
                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.dontAnimate();

                    Glide.with(LoginActivity.this).load(image_url).into(imgFB);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name,last_name,email,id");
        request.setParameters(parameters);
        request.executeAsync();
    }

    public boolean isUserLogged() {
        boolean isLogged = false;
        if (mAuth.getCurrentUser() != null) {
            isLogged = true;
        }
        if (AccessToken.getCurrentAccessToken() != null) {
            isLogged = true;
        }
        return isLogged;
    }

    private void updateUI(final FirebaseUser user) {
        Intent it = new Intent(getBaseContext(), MainActivity.class);

        if ((user != null && user.isEmailVerified())) {
            if (User.haveNameAndEmailEqualSP(getApplicationContext(), user.getEmail())) {
                User newUser = new User();
                newUser.restaureNameSP(getApplicationContext());
                newUser.setEmail(user.getEmail());
                newUser.setId(user.getUid());
                newUser.saveInFirebase();
                newUser.deleteNameSP(getApplicationContext());
                startActivity(it);
            } else {
                if(isLogin){
                    startActivity(it);
                }
            }
        } else if (isUserLogged()) {
            startActivity(it);
        }
    }

    private void initViews() {
        editNome = (EditText) findViewById(R.id.editNome);
        editEmail = (EditText) findViewById(R.id.editEmail);
        editSenha = (EditText) findViewById(R.id.editSenha);
        imgFB = (ImageView) findViewById(R.id.imgFB);
        imgGoogle = (ImageView) findViewById(R.id.imgGoogle);
        imgBack = (ImageView) findViewById(R.id.imgBack);
        bLogar = (Button) findViewById(R.id.bLogar);
        tvCadastrar = (TextView) findViewById(R.id.tvCadastrar);
        tvRedefinir = (TextView) findViewById(R.id.tvRedefinir);
        tvInfo = (TextView) findViewById(R.id.tvInfo);
        llInfo01 = (LinearLayout) findViewById(R.id.llInfo01);
    }

    private void initEditListeners() {
        editNome.setSelectAllOnFocus(true);
        editEmail.setSelectAllOnFocus(true);
        editSenha.setSelectAllOnFocus(true);
        View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    if (editNome.getText().length() == 0) {
                        editNome.setText("Nome");
                    }
                    if (editEmail.getText().length() == 0) {
                        editEmail.setText("Email");
                    }
                    if (editSenha.getText().length() == 0) {
                        editSenha.setText("Senha");
                    }
                }
            }
        };
        editNome.setOnFocusChangeListener(onFocusChangeListener);
        editEmail.setOnFocusChangeListener(onFocusChangeListener);
        editSenha.setOnFocusChangeListener(onFocusChangeListener);
    }

    private void setAllListeners() {
        initEditListeners();
        bLogar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickAnimation(view);
                if (isPasswordReset) {
                    if (isValidedEmail()) {
                        mAuth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            alert("Foi enviado um e-mail foi enviado para alterar sua senha.", 1);
                                        } else {
                                            alert("Email não registrado!", 1);
                                        }
                                    }
                                });
                    }
                } else if (verificarCampos()) {
                    if (isLogin) {
                        try {
                            signInWithEmail(email, senha);
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            newUser(email, senha);
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    editEmail.setError("O email deve ser válido!");
                    editSenha.setError("A senha deve ter ao menos 8 caracteres!");
                    if (!isLogin) editNome.setError("O nome deve ter ao menos 8 caracteres!");
                }
            }
        });

        tvCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alternarLoginCadastro();
            }
        });

        tvRedefinir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //anima o form
                isLogin = true;
                formSlideRL();

                //reset o form
                editSenha.setVisibility(View.GONE);
                llInfo01.setVisibility(View.GONE);
                tvCadastrar.setVisibility(View.GONE);
                imgBack.setVisibility(View.VISIBLE);
                tvInfo.setText("Deseja voltar a tela de login?");
                bLogar.setText("Redefinir Senha");
                isPasswordReset = true;
            }
        });

        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPasswordReset) {
                    alternarLoginCadastro();
                    clickAnimation(view);
                } else {
                    //anima o form
                    isLogin = true;
                    formSlideRL();
                    limparCampos();

                    //reset o form
                    editSenha.setVisibility(View.VISIBLE);
                    llInfo01.setVisibility(View.VISIBLE);
                    tvCadastrar.setVisibility(View.VISIBLE);
                    imgBack.setVisibility(View.GONE);
                    tvInfo.setText("Não tem uma conta?");
                    bLogar.setText("Logar");
                    isPasswordReset = false;
                }
            }
        });

        imgFB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickAnimation(view);
                LoginManager.getInstance()
                        .logInWithReadPermissions(LoginActivity.this, Arrays.asList("email", "public_profile"));
            }
        });

        imgGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickAnimation(view);
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN_GOOGLE);
            }
        });

    }

    private boolean isValidedEmail() {
        boolean isValided = true;
        email = editEmail.getText().toString().trim();
        if (!email.contains("@") || !email.contains(".") || email.length() < 14 || email.contains(" ")) {
            isValided = false;
        }
        return isValided;
    }

    private boolean verificarCampos() {
        boolean isOK = true;

        //recuperando as strings dos edits
        nome = editNome.getText().toString().trim();
        email = editEmail.getText().toString().trim();
        senha = editSenha.getText().toString().trim();

        if (!email.contains("@") || !email.contains(".") || email.length() < 14 || email.contains(" ")) {
            isOK = false;
        }
        if (senha.length() < 6 || (!isLogin && nome.length() < 6)) {
            isOK = false;
        }
        return isOK;
    }

    public void signInWithEmail(String nome, String senha) throws NoSuchAlgorithmException {
        mAuth.signInWithEmailAndPassword(email, CryptWithMD5.gerarMD5Hast(senha))
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (!user.isEmailVerified()) {
                                alert("Verifique o email de confirmação!", 0);
                            }
                            updateUI(mAuth.getCurrentUser());
                        } else {
                            // If sign in fails, display a message to the user.
                            alert("Usuário não registrado!", 0);
                        }
                    }
                });
    }

    private void initUser() throws NoSuchAlgorithmException {
        user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setSenha(senha);
        user.gerarCryptSenha();
        user.saveNameAndEmailSP(getApplicationContext());
    }

    public void newUser(String nome, String senha) throws NoSuchAlgorithmException {
        initUser();
        mAuth.createUserWithEmailAndPassword(user.getEmail(), user.getSenha()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    //só animação
                    alternarLoginCadastro();

                    FirebaseUser user = mAuth.getCurrentUser();
                    user.sendEmailVerification()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        alert("Um email foi enviado para confirmar seu cadastro.", 1);
                                    } else {
                                        alert("Houve um erro ao soliciar a confirmação do cadastro!", 1);
                                    }
                                }
                            });

                } else {
                    alert("Houve um erro ao cadastrar!", 0);
                }
            }
        });
    }

    private void limparCampos() {
        editNome.setText("Nome");
        editEmail.setText("Email");
        editSenha.setText("Senha");
    }

    private void alternarLoginCadastro() {
        limparCampos();
        if (isLogin) {
            isLogin = false;
            tvCadastrar.setVisibility(View.GONE);
            llInfo01.setVisibility(View.GONE);
            bLogar.setText("Cadastrar");
            tvInfo.setText("Deseja voltar a tela de login?");
            editNome.setVisibility(View.VISIBLE);
            imgBack.setVisibility(View.VISIBLE);
            formSlideRL();
        } else {
            isLogin = true;
            tvCadastrar.setVisibility(View.VISIBLE);
            llInfo01.setVisibility(View.VISIBLE);
            bLogar.setText("Logar");
            tvInfo.setText("Não tem uma conta?");
            editNome.setVisibility(View.GONE);
            imgBack.setVisibility(View.GONE);
            formSlideRL();
        }
    }

    private void clickAnimation(View view) {
        YoYo.with(Techniques.Pulse).duration(300).repeat(0).playOn(view);
    }

    private void formSlideRL() {
        clickAnimation(imgBack);

        Techniques t;
        if (isLogin) {
            t = Techniques.SlideInLeft;
        } else {
            t = Techniques.SlideInRight;
        }

        YoYo.with(t).duration(700).repeat(0).playOn(editNome);
        YoYo.with(t).duration(600).repeat(0).playOn(editEmail);
        YoYo.with(t).duration(400).repeat(0).playOn(editSenha);
        YoYo.with(t).duration(820).repeat(0).playOn(bLogar);
        YoYo.with(t).duration(600).repeat(0).playOn(llInfo01);
        YoYo.with(t).duration(600).repeat(0).playOn(findViewById(R.id.tvOu));
        YoYo.with(Techniques.FadeIn).duration(600).repeat(0).playOn(findViewById(R.id.imgDiv));
        YoYo.with(t).duration(1000).repeat(0).playOn(tvCadastrar);
        YoYo.with(t).duration(600).repeat(0).playOn(imgFB);
        YoYo.with(t).duration(600).repeat(0).playOn(findViewById(R.id.tvFB));
        YoYo.with(t).duration(600).repeat(0).playOn(findViewById(R.id.f_icon_fb));
        YoYo.with(t).duration(800).repeat(0).playOn(imgGoogle);
        YoYo.with(t).duration(800).repeat(0).playOn(findViewById(R.id.tvGoogle));
        YoYo.with(t).duration(800).repeat(0).playOn(findViewById(R.id.g_icon_google));
        YoYo.with(Techniques.Tada).duration(600).repeat(0).playOn(findViewById(R.id.llTitulo));
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d("Connection:", "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Connection:", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Connection:", "signInWithCredential:failure", task.getException());
                            alert("Falha na autenticação 02!", 0);
                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

    private void alert(String txtAlert, int delay) {
        if (delay == 0) {
            Toast.makeText(getApplicationContext(), txtAlert, Toast.LENGTH_SHORT).show();
        } else if (delay == 1) {
            Toast.makeText(getApplicationContext(), txtAlert, Toast.LENGTH_LONG).show();
        }
    }

    public void printHashKey(Context pContext) {
        try {
            PackageInfo info = pContext.getPackageManager().getPackageInfo(pContext.getPackageName(), PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                final String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i("AppLog", "printHashKey() Hash Key: " + hashKey + "=");
                alert("printHashKey() Hash Key: " + hashKey + "=", 1);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e("TAG", "printHashKey()", e);
            alert("printHashKey() Error: " + e, 1);
        } catch (Exception e) {
            Log.e("TAG", "printHashKey()", e);
            alert("printHashKey() Error: " + e, 1);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        alert("Error: " + connectionResult, 1);
    }
}